package com.transcendance.backend.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transcendance.backend.config.SessionHintCookie;
import com.transcendance.backend.dto.LoginResponseTokenDto;
import com.transcendance.backend.dto.TwoFactorCodeRequestDto;
import com.transcendance.backend.model.User;
import com.transcendance.backend.services.JWTService;
import com.transcendance.backend.services.RefreshTokenService;
import com.transcendance.backend.services.TwoFactorLockedException;
import com.transcendance.backend.services.TwoFactorService;
import com.transcendance.backend.services.UserService;

import dev.samstevens.totp.exceptions.QrGenerationException;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth/2fa")
public class TwoFactorController {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(TwoFactorController.class);

    private final TwoFactorService twoFactorService;
    private final UserService userService;
    private final JWTService jwtService;
    private final RefreshTokenService refreshTokenService;

    public TwoFactorController(TwoFactorService twoFactorService, UserService userService,
            JWTService jwtService, RefreshTokenService refreshTokenService) {
        this.twoFactorService = twoFactorService;
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Starts the 2FA setup: generates a TOTP secret and its QR code.
     *
     * @param user the authenticated user
     * @return 200 with the QR code data URI and the secret, 409 if 2FA is
     *         already enabled, 500 if the QR code cannot be generated
     */
    @PostMapping("/setup")
    public ResponseEntity<?> setup(@AuthenticationPrincipal User user) {
        log.info("2FA setup requested by user id: {}", user.getIdUser());

        try {
            return ResponseEntity.ok(twoFactorService.generateSetup(user));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (QrGenerationException e) {
            log.error("QR code generation failed for user id: {}", user.getIdUser(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Impossible de générer le QR code"));
        }
    }

    /**
     * Enables 2FA after checking a first code from the authenticator app.
     *
     * @param user the authenticated user
     * @param request the code to verify
     * @param bindingResult the result of validation checks on the request
     * @return 200 if enabled, 400 if the code is malformed or invalid, 409 if
     *         2FA is already enabled or setup was not done
     */
    @PostMapping("/enable")
    public ResponseEntity<?> enable(@AuthenticationPrincipal User user,
            @Valid @RequestBody TwoFactorCodeRequestDto request, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error ->
                    errors.put(error.getField(), error.getDefaultMessage())
            );
            log.warn("2FA enable failed due to validation error: {}", errors);
            return ResponseEntity.badRequest().body(errors);
        }

        try {
            twoFactorService.enable(user, request.getCode());
            return ResponseEntity.ok(Map.of("message", "Double authentification activée"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Disables 2FA after checking a code from the authenticator app.
     *
     * @param user the authenticated user
     * @param request the code to verify
     * @param bindingResult the result of validation checks on the request
     * @return 200 if disabled, 400 if the code is malformed or invalid, 409 if
     *         2FA is not enabled, 429 if too many wrong codes were sent
     */
    @PostMapping("/disable")
    public ResponseEntity<?> disable(@AuthenticationPrincipal User user,
            @Valid @RequestBody TwoFactorCodeRequestDto request, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error ->
                    errors.put(error.getField(), error.getDefaultMessage())
            );
            log.warn("2FA disable failed due to validation error: {}", errors);
            return ResponseEntity.badRequest().body(errors);
        }

        try {
            twoFactorService.disable(user, request.getCode());
            return ResponseEntity.ok(Map.of("message", "Double authentification désactivée"));
        } catch (TwoFactorLockedException e) {
            return ResponseEntity.status(429).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Second step of an OAuth 42 login on a 2FA account: checks the code for
     * the user held by the {@code two_factor_token} cookie, then opens the session.
     *
     * @param twoFactorToken the pending login token set by the 42 callback
     * @param request the code to verify
     * @param bindingResult the result of validation checks on the request
     * @return 200 with the session cookies, 400 if the code is malformed,
     *         401 if the token is missing/expired or the code is invalid,
     *         429 if too many wrong codes were sent
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@CookieValue(name = "two_factor_token", required = false) String twoFactorToken,
            @Valid @RequestBody TwoFactorCodeRequestDto request, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error ->
                    errors.put(error.getField(), error.getDefaultMessage())
            );
            log.warn("2FA login failed due to validation error: {}", errors);
            return ResponseEntity.badRequest().body(errors);
        }

        Optional<User> pendingUser = twoFactorService.findPendingLogin(twoFactorToken)
                .flatMap(userService::getUserById)
                .filter(user -> user.getDeletedAt() == null && user.isTwoFactorEnabled());
        if (pendingUser.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Session de double authentification expirée"));
        }
        User user = pendingUser.get();

        try {
            twoFactorService.verifyCode(user, request.getCode());
        } catch (TwoFactorLockedException e) {
            return ResponseEntity.status(429).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }

        twoFactorService.removePendingLogin(twoFactorToken);
        log.info("2FA login successful for user id: {}", user.getIdUser());

        LoginResponseTokenDto tokens = refreshTokenService.openSession(user);

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

        ResponseCookie clearTwoFactorCookie = ResponseCookie.from("two_factor_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("strict")
                .path("/api/auth/2fa")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearTwoFactorCookie.toString())
                .header(HttpHeaders.SET_COOKIE, SessionHintCookie.set().toString())
                .body(Map.of("message", "Connexion réussie"));
    }
}
