package com.transcendance.backend.services;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transcendance.backend.model.PasswordResetToken;
import com.transcendance.backend.model.User;
import com.transcendance.backend.repository.PasswordResetTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(PasswordResetService.class);

    private static final Duration RESEND_COOLDOWN = Duration.ofMinutes(2);

    private final SecureRandom secureRandom = new SecureRandom();

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final MailService mailService;

    @Value("${app.password-reset.validity-minutes}")
    private long validityMinutes;

    @Transactional
    public void requestReset(String email) {
        User user = userService.getUserByEmail(email)
                .filter(u -> u.getDeletedAt() == null)
                .orElse(null);
        if (user == null) {
            log.info("Password reset requested for unknown email");
            return;
        }

        if (wasSentRecently(user)) {
            log.info("Password reset email already sent recently to user: {}", user.getIdUser());
            return;
        }

        String rawToken = createToken(user);
        log.info("Password reset token created for user: {}", user.getIdUser());
        mailService.sendPasswordResetEmail(user, rawToken);
    }

    @Transactional
    public void resetPassword(String rawToken, String password, String confirmedPassword) {
        if (!password.equals(confirmedPassword)) {
            log.warn("Passwords do not match on password reset");
            throw new IllegalArgumentException("Les mots de passe ne correspondent pas");
        }

        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHash(refreshTokenService.hashToken(rawToken))
                .filter(t -> !t.isExpired(Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException("Lien de réinitialisation invalide ou expiré"));

        User user = token.getUser();
        user.setPassword(userService.encodePassword(password));
        user.setPasswordSet(true);
        userService.saveUser(user);

        passwordResetTokenRepository.deleteByUser(user);
        refreshTokenService.revokeActiveSessions(user);
        log.info("Password reset for user: {}", user.getIdUser());
    }

    @Scheduled(fixedRate = 60 * 60 * 1000)
    @Transactional
    public void purgeExpiredTokens() {
        passwordResetTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }

    private boolean wasSentRecently(User user) {
        Instant limit = Instant.now().minus(RESEND_COOLDOWN);
        return passwordResetTokenRepository.findByUser(user)
                .map(t -> t.getCreatedAt().isAfter(limit))
                .orElse(false);
    }

    private String createToken(User user) {
        passwordResetTokenRepository.deleteByUser(user);

        String rawToken = generateUrlSafeToken();
        Instant now = Instant.now();

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(refreshTokenService.hashToken(rawToken));
        token.setExpiresAt(now.plus(validityMinutes, ChronoUnit.MINUTES));
        token.setCreatedAt(now);
        passwordResetTokenRepository.save(token);

        return rawToken;
    }

    private String generateUrlSafeToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}
