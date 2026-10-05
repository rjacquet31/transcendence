package com.transcendance.backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

import com.transcendance.backend.config.AvatarProperties;
import com.transcendance.backend.config.SessionHintCookie;
import com.transcendance.backend.dto.UpdateProfileRequestDto;
import com.transcendance.backend.dto.UpdateUserRequestDto;
import com.transcendance.backend.dto.UserPublicProfileDto;
import com.transcendance.backend.model.User;
import com.transcendance.backend.services.AvatarStorageService;
import com.transcendance.backend.services.DeleteUserService;
import com.transcendance.backend.services.UpdateUserService;
import com.transcendance.backend.services.UserRoleService;
import com.transcendance.backend.services.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(UserController.class);

    private final UserService userService;
    private final UserRoleService userRoleService;
    private final AvatarStorageService avatarStorageService;
    private final AvatarProperties avatarProperties;
    private final UpdateUserService updateUserService;
    private final DeleteUserService deleteUserService;

    public UserController(UserService userService, UserRoleService userRoleService,
            AvatarStorageService avatarStorageService, AvatarProperties avatarProperties,
            UpdateUserService updateUserService, DeleteUserService deleteUserService) {
        this.userService = userService;
        this.userRoleService = userRoleService;
        this.avatarStorageService = avatarStorageService;
        this.avatarProperties = avatarProperties;
        this.updateUserService = updateUserService;
        this.deleteUserService = deleteUserService;
    }

    /** Maps a user to what other users may see: no email, no role. */
    private UserPublicProfileDto toPublicDto(User user) {
        UserPublicProfileDto dto = new UserPublicProfileDto();
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setId(user.getIdUser());
        dto.setOnline(user.getOnline());
        dto.setUserName(user.getUserName());
        return dto;
    }

    /** Maps a user to its full profile, for its owner and for administrators. */
    private UserPublicProfileDto toDto(User user) {
        UserPublicProfileDto dto = toPublicDto(user);
        dto.setEmail(user.getEmail());
        dto.setRole(userRoleService.getRoleName(user));
        return dto;
    }

    /**
     * Retrieves the public profile of every user stored in the database.
     * <p>
     * Public route: no JWT is read here, the data comes straight from the
     * database.
     *
     * @return an HTTP 200 response containing one {@link UserPublicProfileDto}
     *         per user, or an empty list if the table is empty
     */
    @GetMapping
    public ResponseEntity<List<UserPublicProfileDto>> getAllUsers() {
        log.info("Fetching public profiles of all users");

        List<UserPublicProfileDto> profiles = userService.getAllUsers()
                .stream()
                .map(this::toDto)
                .toList();

        log.info("{} public profiles fetched", profiles.size());
        return ResponseEntity.ok(profiles);
    }

    /**
     * Retrieves the authenticated user's public profile.
     *
     * @param user the authenticated user
     * @return an HTTP 200 response containing the {@link UserPublicProfileDto}
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMeProfile(@AuthenticationPrincipal User user) {
        log.info("Fetching public profile for user id: {}", user.getIdUser());

        UserPublicProfileDto dto = toDto(user);
        dto.setTwoFactorEnabled(user.isTwoFactorEnabled());
        dto.setOauth42Linked(user.getOauth42Id() != null);

        log.info("Public profile fetched for user: {}", user.getIdUser());
        return ResponseEntity.ok(dto);
    }

    /**
     * Retrieves a user's public profile by their identifier.
     *
     * @param idUser the UUID of the user to look up
     * @return an HTTP 200 response containing the {@link UserPublicProfileDto}
     *         if the user exists, or an HTTP 404 response otherwise
     */
    @GetMapping("/{idUser}")
    public ResponseEntity<?> getUserProfile(@PathVariable UUID idUser) {
        log.info("Fetching public profile for user id: {}", idUser);

        Optional<User> userOpt = userService.getUserById(idUser);

        if (userOpt.isEmpty()) {
            log.warn("User with id {} not found", idUser);
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        UserPublicProfileDto dto = toPublicDto(user);

        return ResponseEntity.ok(dto);
    }

    /**
     * Uploads (and replaces) the authenticated user's avatar.
     *
     * @param user the authenticated user
     * @param file the uploaded image file
     * @return an HTTP 200 response containing the updated {@link UserPublicProfileDto},
     *         or an HTTP 400 response if the file is invalid
     */
    @PostMapping("/me/avatar")
    public ResponseEntity<?> uploadAvatar(@AuthenticationPrincipal User user, @RequestParam("file") MultipartFile file) {
        log.info("Uploading avatar for user id: {}", user.getIdUser());

        String newFilename;
        try {
            newFilename = avatarStorageService.store(file);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid avatar file for user id: {}", user.getIdUser(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        String oldAvatarUrl = user.getAvatarUrl();

        String newAvatarUrl = avatarProperties.getPublicPath() + "/" + newFilename;

        userService.updateAvatarUrl(user, newAvatarUrl);

        if (oldAvatarUrl != null && !oldAvatarUrl.isBlank()) {
            String oldFilename = oldAvatarUrl.substring(oldAvatarUrl.lastIndexOf('/') + 1);
            avatarStorageService.delete(oldFilename);
        }

        log.info("Avatar uploaded for user id: {}", user.getIdUser());
        return ResponseEntity.ok(toDto(user));
    }

    /**
     * @RequestParam("file") is resolved before uploadAvatar's body runs, so
     * a request with no "file" part at all throws MultipartException before
     * any try/catch inside that method could ever catch it.
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<?> handleMissingFilePart(MultipartException e) {
        log.warn("No file part in the request", e);
        return ResponseEntity.badRequest().body(e.getMessage());
    }

     /*
     * Self-service update of a user's username and email.
     * <p>
     * A regular user may only target their own account; an admin may target
     * any account. No role change happens here (see {@link #updateUser}).
     *
     * @param idUser             the UUID of the user to update
     * @param authenticatedUser  the authenticated caller
     * @param updateProfileDto   the new values
     * @param bindingResult      validation result for the request
     * @return an HTTP 200 response containing the updated
     *         {@link UserPublicProfileDto}, an HTTP 403 response if the caller
     *         is not allowed to edit this account, an HTTP 404 response if the
     *         user does not exist, or an HTTP 400 response if the payload is
     *         rejected
     */
    @PutMapping("/{idUser}")
    public ResponseEntity<?> updateProfile(
            @PathVariable UUID idUser,
            @AuthenticationPrincipal User authenticatedUser,
            @Valid @RequestBody UpdateProfileRequestDto updateProfileDto,
            BindingResult bindingResult) {

        log.info("Updating profile for user id: {}", idUser);

        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error ->
                    errors.put(error.getField(), error.getDefaultMessage())
            );
            log.warn("Update failed due to validation error: {}", errors);

            return ResponseEntity.badRequest().body(errors);
        }

        if (!authenticatedUser.getIdUser().equals(idUser) &&
                !userRoleService.getRoleName(authenticatedUser).equals("ADMIN")) {
            log.warn("Unauthorized profile update attempt by user id: {}", authenticatedUser.getIdUser());
            return ResponseEntity.status(403).body(Map.of("error", "Tu n'es pas autorisé à modifier ce profil."));
        }

        Optional<User> userOpt = userService.getUserById(idUser);
        if (userOpt.isEmpty()) {
            log.warn("User with id {} not found", idUser);
            return ResponseEntity.notFound().build();
        }

        try {
            User updatedUser = updateUserService.updateProfile(userOpt.get(), updateProfileDto);
            
            log.info("User {} updated", idUser);
            return ResponseEntity.ok(toDto(updatedUser));
        } catch (IllegalArgumentException e) {
            log.warn("Update failed: {}", e.getMessage());

            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Overwrites a user's username, email and role.
     * <p>
     * Administration route: the security chain already restricts it to
     * ROLE_ADMIN. Every field of the body is written back, even when unchanged.
     *
     * @param idUser        the UUID of the user to update
     * @param updateUserDto the new values
     * @param bindingResult validation result for the request
     * @return an HTTP 200 response containing the updated
     *         {@link UserPublicProfileDto}, an HTTP 404 response if the user does
     *         not exist, or an HTTP 400 response if the payload is rejected
     */
    @PostMapping("/{idUser}")
    public ResponseEntity<?> updateUser(
            @PathVariable UUID idUser,
            @Valid @RequestBody UpdateUserRequestDto updateUserDto,
            BindingResult bindingResult) {

        log.info("Updating user id: {}", idUser);

        // Check whether validation errors were detected by @Valid.
        if (bindingResult.hasErrors()) {
            log.warn("Update failed due to validation error: {}",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());

            return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
        }

        Optional<User> userOpt = userService.getUserById(idUser);

        if (userOpt.isEmpty()) {
            log.warn("User with id {} not found", idUser);
            return ResponseEntity.notFound().build();
        }

        try {
            User updatedUser = updateUserService.updateUser(userOpt.get(), updateUserDto);

            log.info("User {} updated", idUser);
            return ResponseEntity.ok(toDto(updatedUser));

        } catch (IllegalArgumentException ex) {

            // Business error (e.g. email already taken by another account).
            log.warn("Update failed: {}", ex.getMessage());

            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * Deletes a user, along with their roles, refresh tokens and friend links.
     * <p>
     * Administration route: the security chain already restricts it to
     * ROLE_ADMIN. An administrator cannot delete their own account, which would
     * revoke the session making the call.
     *
     * @param idUser the UUID of the user to delete
     * @param admin  the authenticated administrator
     * @return an HTTP 204 response, an HTTP 404 response if the user does not
     *         exist, or an HTTP 400 response on self-deletion
     */
    @PostMapping("/delete/{idUser}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID idUser, @AuthenticationPrincipal User admin) {
        log.info("Deleting user id: {}", idUser);

        if (admin != null && admin.getIdUser().equals(idUser)) {
            log.warn("User {} tried to delete their own account", idUser);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Tu ne peux pas supprimer ton propre compte."));
        }

        Optional<User> userOpt = userService.getUserById(idUser);

        if (userOpt.isEmpty()) {
            log.warn("User with id {} not found", idUser);
            return ResponseEntity.notFound().build();
        }

        deleteUserService.deleteUser(userOpt.get());

        log.info("User {} deleted", idUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lets the authenticated caller delete their own account.
     * <p>
     * Reuses the same soft-delete as the admin route above, then clears
     * the session cookies on the way out — they're worthless afterward
     * anyway, since deleteUserService already revoked every refresh token
     * for this user.
     *
     * @param user the authenticated caller
     * @return an HTTP 200 response with the access/refresh cookies expired
     */
    @PostMapping("/me/delete")
    public ResponseEntity<?> deleteMyAccount(@AuthenticationPrincipal User user) {
        log.info("User {} deleting their own account", user.getIdUser());

        deleteUserService.deleteUser(user);

        ResponseCookie expiredAccessCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("strict")
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie expiredRefreshCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("strict")
                .path("/api/auth")
                .maxAge(0)
                .build();

        log.info("User {} deleted their own account", user.getIdUser());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredAccessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, SessionHintCookie.clear().toString())
                .body("Compte supprimé");
    }
}
