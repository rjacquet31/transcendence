package com.transcendance.backend.controller;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import com.transcendance.backend.config.Oauth42Properties;
import com.transcendance.backend.config.SessionHintCookie;
import com.transcendance.backend.dto.LoginResponseTokenDto;
import com.transcendance.backend.dto.Oauth42UserInfoDto;
import com.transcendance.backend.model.User;
import com.transcendance.backend.services.JWTService;
import com.transcendance.backend.services.Oauth42Service;
import com.transcendance.backend.services.UserService;
import com.transcendance.backend.services.RefreshTokenService;
import com.transcendance.backend.services.TwoFactorService;

@RestController
@RequestMapping("/api/auth/42")
public class Oauth42Controller {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(Oauth42Controller.class);

    private static final String STATE_COOKIE = "oauth42_state";
    private static final String LOGIN_STATE_PREFIX = "login-";
    private static final int STATE_TTL_SECONDS = 5 * 60;

    private final Oauth42Service oauth42Service;
    private final Oauth42Properties properties;
    private final JWTService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TwoFactorService twoFactorService;
    private final UserService userService;

    private final SecureRandom random = new SecureRandom();

    public Oauth42Controller(Oauth42Service oauth42Service, Oauth42Properties properties,
            JWTService jwtService, RefreshTokenService refreshTokenService, TwoFactorService twoFactorService,
            UserService userService) {
        this.oauth42Service = oauth42Service;
        this.properties = properties;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.twoFactorService = twoFactorService;
        this.userService = userService;
    }

    /**
     * Builds a frontend URL, whatever the configured base looks like.
     *
     * @param suffix the path and query to append, may be empty
     * @return the absolute frontend URL to redirect the browser to
     */
    private String frontendUrl(String suffix) {
        String base = properties.getFrontendRedirectUri();
        if (!base.endsWith("/")) {
            base += "/";
        }
        return base + suffix;
    }

    /**
     * Builds the state cookie. Lax, not strict: the callback comes from 42's site.
     *
     * @param value the state, or an empty string to clear the cookie
     * @param maxAgeSeconds the lifetime of the cookie, 0 to clear it
     */
    private ResponseCookie stateCookie(String value, int maxAgeSeconds) {
        return ResponseCookie.from(STATE_COOKIE, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("lax")
                .path("/api/auth/42")
                .maxAge(maxAgeSeconds)
                .build();
    }

    /** Redirects to 42's authorization page and stores the state in a cookie. */
    private ResponseEntity<Void> redirectTo42(String state) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(oauth42Service.buildAuthorizationUrl(state)))
                .header(HttpHeaders.SET_COOKIE, stateCookie(state, STATE_TTL_SECONDS).toString())
                .build();
    }

    /** Redirects to the frontend and clears the state cookie. */
    private ResponseEntity<Void> backToFrontend(String suffix, ResponseCookie... cookies) {
        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl(suffix)))
                .header(HttpHeaders.SET_COOKIE, stateCookie("", 0).toString());
        for (ResponseCookie cookie : cookies) {
            response.header(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return response.build();
    }

    /** Constant-time comparison of the two states. */
    private boolean sameState(String state, String expectedState) {
        return MessageDigest.isEqual(
                state.getBytes(StandardCharsets.UTF_8),
                expectedState.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Entry point of the OAuth flow: sends the browser to 42's own login
     * and authorization page.
     *
     * @return a redirect to 42's authorization page
     */
    @GetMapping("/login")
    public ResponseEntity<Void> redirectTo42() {
        log.info("Redirecting to 42 OAuth authorization page");

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String state = LOGIN_STATE_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        return redirectTo42(state);
    }

    /**
     * Entry point of the account linking flow, for someone already logged in
     * locally: same authorization page, but with a signed state that says who
     * asked. Called through a real browser navigation, so the sameSite=strict
     * session cookie is sent (this request is same-site, unlike the callback).
     *
     * @param user the authenticated local account
     * @return a redirect to 42's authorization page, or to the profile page
     *         with an error if this account is already linked
     */
    @GetMapping("/link")
    public ResponseEntity<Void> linkAccount(@AuthenticationPrincipal User user) {
        log.info("42 account linking requested by user id: {}", user.getIdUser());

        if (user.getOauth42Id() != null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl("profile?error=already_linked")))
                .build();
        }

        return redirectTo42(jwtService.generateLink42State(user));
    }

    /**
     * Drops the link between this account and its 42 one.
     *
     * @param user the authenticated local account
     * @return 200 once unlinked, 409 if the account was created through 42
     *         and has no password its owner could log in with
     */
    @PostMapping("/unlink")
    public ResponseEntity<?> unlinkAccount(@AuthenticationPrincipal User user) {
        log.info("42 account unlinking requested by user id: {}", user.getIdUser());

        try {
            oauth42Service.unlinkAccount(user);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            log.warn("42 unlink refused for user {}: {}", user.getIdUser(), e.getMessage());
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 42 redirects here once the user has answered. Logs the user in locally
     * and redirects to the frontend with the session cookies set.
     * <p>
     * If the account has 2FA enabled, a short-lived {@code two_factor_token}
     * cookie is set instead and the frontend asks for the code.
     * <p>
     * The state must match this browser's cookie. A linking state attaches the
     * 42 identity to the account it names, without opening a session.
     *
     * @param code the authorization code, absent if the user refused
     * @param state the state 42 sends back unchanged
     * @param expectedState the state stored in this browser's cookie
     * @return a redirect to the frontend
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @CookieValue(name = STATE_COOKIE, required = false) String expectedState) {
        log.info("Handling 42 OAuth callback");

        if (state == null || expectedState == null || !sameState(state, expectedState)) {
            log.warn("42 OAuth callback refused: state missing or not issued to this browser");
            return backToFrontend("?error=oauth_failed");
        }

        boolean linking = !state.startsWith(LOGIN_STATE_PREFIX);

        if (code == null) {
            log.info("42 OAuth callback without a code: the user refused the authorization");
            return backToFrontend(linking ? "profile?error=oauth_denied" : "?error=oauth_denied");
        }

        try {

            String access_token_42 = oauth42Service.exchangeCodeForAccessToken(code);

            Oauth42UserInfoDto userDto = oauth42Service.fetchUserInfo(access_token_42);

            if (linking) {
                return handleLinkCallback(state, userDto);
            }

            User localUser = oauth42Service.findOrCreateUser(userDto);

            if (localUser.isTwoFactorEnabled()) {
                ResponseCookie twoFactorCookie = ResponseCookie.from("two_factor_token",
                        twoFactorService.createPendingLogin(localUser))
                  .httpOnly(true)
                  .secure(true)
                  .sameSite("strict")
                  .path("/api/auth/2fa")
                  .maxAge(5 * 60)
                  .build();

                log.info("42 OAuth login waiting for 2FA code, user id: {}", localUser.getIdUser());
                return backToFrontend("?twoFactor=required", twoFactorCookie);
            }

            LoginResponseTokenDto tokens = refreshTokenService.openSession(localUser);

            ResponseCookie accessCookie = ResponseCookie.from("access_token", tokens.getAccessToken())
              .httpOnly(true)
              .secure(true)
              .sameSite("strict")
              .path("/")
              .maxAge(15 * 60)
              .build();

            ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokens.getRefreshToken())
              .httpOnly(true)
              .secure(true)
              .sameSite("strict")
              .path("/api/auth")
              .maxAge(7 * 24 * 60 * 60)
              .build();

            return backToFrontend("", accessCookie, refreshCookie, SessionHintCookie.set());

        } catch (IllegalStateException e) {
            log.warn("42 OAuth callback refused: {}", e.getMessage());
            return backToFrontend("?error=email_already_registered");
        } catch (RestClientException e) {
            log.warn("42 OAuth callback failed: {}", e.getMessage());
            return backToFrontend(linking ? "profile?error=oauth_failed" : "?error=oauth_failed");
        }
    }

    /**
     * Linking branch of the callback: the state says who is linking. Nothing
     * here opens a session.
     *
     * @param state the signed token issued by {@code /link}
     * @param info the 42 profile that just came back
     * @return a redirect to the profile page, with the outcome in the query
     */
    private ResponseEntity<Void> handleLinkCallback(String state, Oauth42UserInfoDto info) {
        String idUser = jwtService.extractUserIdFromLink42State(state);
        if (idUser == null) {
            log.warn("42 linking callback got an invalid state");
            return backToFrontend("profile?error=link_failed");
        }

        Optional<User> userOpt = userService.getUserById(UUID.fromString(idUser));
        if (userOpt.isEmpty()) {
            log.warn("42 linking callback names an unknown user: {}", idUser);
            return backToFrontend("profile?error=link_failed");
        }

        try {
            oauth42Service.linkAccount(userOpt.get(), info);
        } catch (IllegalStateException e) {
            log.warn("42 linking refused for user {}: {}", idUser, e.getMessage());
            return backToFrontend("profile?error=already_linked");
        }

        log.info("42 account linked to user {}", idUser);
        return backToFrontend("profile?linked=42");
    }
}
