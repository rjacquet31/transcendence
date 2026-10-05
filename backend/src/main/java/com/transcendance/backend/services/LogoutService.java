package com.transcendance.backend.services;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.transcendance.backend.repository.RefreshTokenRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class LogoutService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(LogoutService.class);

    private RefreshTokenService refreshTokenService;
    private RefreshTokenRepository refreshTokenRepository;

    /**
     * Revokes the specified refresh token.
     *
     * @param refreshToken the refresh token to revoke
     */
    public void revokeRefreshToken(String refreshToken) {

        String hashedToken = refreshTokenService.hashToken(refreshToken);

        refreshTokenRepository.findByTokenHash(hashedToken)
            .ifPresentOrElse(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
                log.info("Refresh token revoked for user: {}", token.getUser().getEmail());
            }, () -> {
                log.warn("Refresh token not found: {}", hashedToken);
                throw new IllegalArgumentException("Jeton de rafraîchissement invalide");
            });
    }
}
