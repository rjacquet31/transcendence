package com.transcendance.backend.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transcendance.backend.config.SessionHintCookie;
import com.transcendance.backend.dto.LoginRequestDto;
import com.transcendance.backend.dto.LoginResponseTokenDto;
import com.transcendance.backend.services.LoginAttemptService;
import com.transcendance.backend.services.LoginService;
import com.transcendance.backend.services.TwoFactorLockedException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(LoginController.class);

    private final LoginService loginService;
    private final LoginAttemptService loginAttemptService;

    public LoginController(LoginService loginService, LoginAttemptService loginAttemptService) {
        this.loginService = loginService;
        this.loginAttemptService = loginAttemptService;
    }

    /**
     * Handles user login requests.
     *
     * @param loginRequest the login request containing email, password and the optional 2FA code
     * @param bindingResult the result of validation checks on the request
     * @param request the HTTP request, read for the address of the client
     * @return a ResponseEntity containing the JWT token in a cookie if successful,
     *         {"twoFactorRequired": true} if a 2FA code is missing, 429 after too
     *         many failed attempts, or an error message if failed
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto loginRequest,
                                    BindingResult bindingResult, HttpServletRequest request) {

        // A leftover refresh_token cookie is simply overwritten by the new one below
        log.info("Processing login for email: {}", loginRequest.getEmail());
        
        if (bindingResult.hasErrors()) {
        Map<String, String> errors = new HashMap<>();
        bindingResult.getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        log.warn("Login failed due to validation error: {}", errors);
        return ResponseEntity.badRequest().body(errors);
    }

        String attemptKey = loginAttemptService.keyOf(loginRequest.getEmail(), clientAddress(request));
        if (loginAttemptService.isLocked(attemptKey)) {
            log.warn("Login refused for email: {}. Too many failed attempts", loginRequest.getEmail());
            return ResponseEntity.status(429)
                    .body(Map.of("error", "Trop de tentatives de connexion échouées. Réessaie plus tard."));
        }

        try {
            Optional<LoginResponseTokenDto> tokens = loginService.authenticate(
                    loginRequest.getEmail(), loginRequest.getPassword(), loginRequest.getCode());

            // 2FA account without code: no cookies yet, the front asks for the code and retries
            if (tokens.isEmpty()) {
                return ResponseEntity.ok(Map.of("twoFactorRequired", true));
            }
            LoginResponseTokenDto loginResponse = tokens.get();
            loginAttemptService.reset(attemptKey);
            log.info("Login successful for email: {}", loginRequest.getEmail());
            
            ResponseCookie accessCookie = ResponseCookie.from("access_token", loginResponse.getAccessToken())
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("strict")
                    .path("/")
                    .maxAge(15 * 60)
                    .build();
            
            ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", loginResponse.getRefreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("strict")
                    .path("/api/auth")
                    .maxAge(7 * 24 * 60 * 60)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, SessionHintCookie.set().toString())
                    .body("Connexion réussie");
        
        } catch (TwoFactorLockedException e) {
            return ResponseEntity.status(429).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
        log.error("Login failed for email: {}. Error: {}", loginRequest.getEmail(), e.getMessage());
        loginAttemptService.recordFailure(attemptKey);
        return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
    }
    }

    /** The client address: nginx passes the real one in {@code X-Real-IP}. */
    private String clientAddress(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        return realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp;
    }
}
