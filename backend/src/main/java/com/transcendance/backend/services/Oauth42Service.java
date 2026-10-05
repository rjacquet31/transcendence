package com.transcendance.backend.services;

import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.transcendance.backend.config.Oauth42Properties;
import com.transcendance.backend.dto.Oauth42TokenResponseDto;
import com.transcendance.backend.dto.Oauth42UserInfoDto;
import com.transcendance.backend.model.User;

@Service
public class Oauth42Service {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(Oauth42Service.class);

    private static final String DEFAULT_AVATAR_URL = "/api/default-avatar.png";

    private final Oauth42Properties properties;
    private final UserService userService;
    private final UserRoleService userRoleService;
    private final RestClient restClient = RestClient.create();

    public Oauth42Service(Oauth42Properties properties, UserService userService, UserRoleService userRoleService) {
        this.properties = properties;
        this.userService = userService;
        this.userRoleService = userRoleService;
    }


    /**
     * Same URL, with an opaque state carried through the whole round trip.
     * 42 hands it back to the callback untouched, which is the only way to
     * know who asked for a link: the session cookie is sameSite=strict, so
     * the browser does not send it on the way back from api.intra.42.fr.
     *
     * @param state the signed token to carry, or null for a plain login
     * @return 42's authorization URL
     */
    public String buildAuthorizationUrl(String state) {
        log.info("Building 42 OAuth authorization URL");

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.getAuthorizeUri())
            .queryParam("client_id", properties.getClientId())
            .queryParam("redirect_uri", properties.getRedirectUri())
            .queryParam("response_type", "code")
            .queryParam("scope", "public");

        if (state != null) {
            builder.queryParam("state", state);
        }

        return builder.build().toUriString();
    }

    /**
     * Server-to-server call: never exposed to the frontend, since the
     * client_secret is used right here.
     *
     * @param code the temporary authorization code 42 gave the browser
     * @return a real access token for the 42 API
     */
    public String exchangeCodeForAccessToken(String code) {
        log.info("Exchanging 42 OAuth code for an access token");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", properties.getClientId());
        formData.add("client_secret", properties.getClientSecret());
        formData.add("code", code);
        formData.add("redirect_uri", properties.getRedirectUri());

        Oauth42TokenResponseDto response = restClient.post()
            .uri(properties.getTokenUri())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .retrieve()
            .body(Oauth42TokenResponseDto.class);

        if (response == null || response.getAccessToken() == null) {
            throw new RestClientException("42 answered the token exchange without an access token");
        }
        return response.getAccessToken();
    }

    public Oauth42UserInfoDto fetchUserInfo(String accessToken) {
        log.info("Fetching 42 profile info");

        Oauth42UserInfoDto response = restClient.get()
            .uri(properties.getUserInfoUri())
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .body(Oauth42UserInfoDto.class);

        if (response == null) {
            throw new RestClientException("42 answered the profile request with an empty body");
        }
        return response;
    }

    /**
     * Creates the local account on the fly if this is the user's first
     * login via 42.
     *
     * @param info the 42 profile to match or create a local account from
     * @return the matching or newly created local {@link User}
     */
    public User findOrCreateUser(Oauth42UserInfoDto info) {
        log.info("Looking up local account for 42 login: {}", info.getLogin());

        Optional<User> userOpt = userService.getUserByOauth42Id(info.getId());
        if (userOpt.isPresent()) {
            log.info("Existing local account found for 42 id: {}", info.getId());
            User user = userOpt.get();
            return user;
        }

        // Un compte local porte deja cet email sans etre lie a ce compte 42 : le
        // rattacher ici reviendrait a ouvrir une session sur un compte dont on
        // n'a pas prouve la possession, les emails du register n'etant pas verifies.
        if (userService.getUserByEmail(info.getEmail()).isPresent()) {
            log.warn("42 login refused: email {} already belongs to a local account", info.getEmail());
            throw new IllegalStateException("email already registered");
        }

        String username = info.getLogin();
        if (userService.isUserNameTaken(username)) {
            log.warn("Username '{}' already taken locally, appending suffix", username);
            username += "_42";
        }

        String passwd = UUID.randomUUID().toString();
        User newUser = new User(username, info.getEmail(), passwd);

        newUser.setAvatarUrl(DEFAULT_AVATAR_URL);

        newUser.setOauth42Id(info.getId());
        newUser.setPasswordSet(false);

        User user = userService.createUser(newUser);
        userRoleService.assignRoleUserToNewUser(user);

        log.info("Created new local account for 42 login: {}", info.getLogin());
        return user;
    }

    /**
     * Links a 42 account to an already authenticated local account. This is
     * the only supported way to reconcile a local account with a 42 one: the
     * callback itself refuses to merge on the email, which is never verified.
     *
     * @param user the authenticated local account
     * @param info the 42 profile that just came back from the callback
     * @return the updated local user
     * @throws IllegalStateException if either side is already linked
     */
    public User linkAccount(User user, Oauth42UserInfoDto info) {
        log.info("Linking 42 id {} to local user {}", info.getId(), user.getIdUser());

        if (user.getOauth42Id() != null) {
            throw new IllegalStateException("account already linked");
        }

        Optional<User> holder = userService.getUserByOauth42Id(info.getId());
        if (holder.isPresent()) {
            log.warn("42 id {} is already linked to another local account", info.getId());
            throw new IllegalStateException("42 account already linked");
        }

        user.setOauth42Id(info.getId());
        return userService.saveUser(user);
    }

    /**
     * Unlinks the 42 account. Refused on accounts born from the 42 flow,
     * whose password is a random UUID nobody ever received: unlinking would
     * lock them out for good.
     *
     * @param user the authenticated local account
     * @return the updated local user
     * @throws IllegalStateException if the account has no usable password
     */
    public User unlinkAccount(User user) {
        log.info("Unlinking 42 account from local user {}", user.getIdUser());

        if (!user.isPasswordSet()) {
            throw new IllegalStateException("Compte créé via 42 sans mot de passe : impossible de le délier");
        }

        user.setOauth42Id(null);
        return userService.saveUser(user);
    }
}
