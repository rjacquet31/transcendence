package com.transcendance.backend.services;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.transcendance.backend.dto.LoginResponseTokenDto;
import com.transcendance.backend.model.User;

import jakarta.transaction.Transactional;

@Service
public class LoginService {

    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(LoginService.class);

    private final JWTService jwtService;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final TwoFactorService twoFactorService;

    public LoginService(JWTService jwtService, UserService userService, RefreshTokenService refreshTokenService,
                        TwoFactorService twoFactorService) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.twoFactorService = twoFactorService;
    }

    /**
     * Authenticates a user based on the provided email, password and, when
     * 2FA is enabled, the code from their authenticator app.
     * @param email the user's email
     * @param password the user's password
     * @param code the 6-digit 2FA code, null on the first attempt
     * @return the tokens, or an empty Optional if the account needs a 2FA code
     * @throws IllegalArgumentException if the email, password or code is invalid
     * @throws TwoFactorLockedException if too many wrong 2FA codes were sent
     */
    @Transactional
    public Optional<LoginResponseTokenDto> authenticate(String email, String password, String code) {

        // Fetch the user by email
        User user = userService.getUserByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email ou mot de passe invalide"));

        // Check if the provided password matches the stored password
        if (!userService.checkPassword(user, password)) {
            log.warn("Invalid password for email: {}", email);
            throw new IllegalArgumentException("Email ou mot de passe invalide");
        }

        if (user.getDeletedAt() != null) {
            log.warn("Can't access a deleted account");
            throw new IllegalArgumentException("Email ou mot de passe invalide");
        }

        // Password is right: a 2FA account still has to prove it holds the authenticator app
        if (user.isTwoFactorEnabled()) {
            if (code == null || code.isBlank()) {
                log.info("2FA code required for email: {}", email);
                return Optional.empty();
            }
            twoFactorService.verifyCode(user, code);
        }

        // Generate the access and refresh tokens
        LoginResponseTokenDto loginResponse = refreshTokenService.openSession(user);

        return Optional.of(loginResponse);
    }
}
