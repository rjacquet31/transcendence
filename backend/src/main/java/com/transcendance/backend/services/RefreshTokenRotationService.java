package com.transcendance.backend.services;

import java.time.Instant;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.transcendance.backend.model.RefreshToken;
import com.transcendance.backend.model.User;
import com.transcendance.backend.repository.RefreshTokenRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenRotationService {

    private final RefreshTokenRepository refreshTokenRepository;

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(RefreshTokenRotationService.class);

    /**
     * Validates an existing refresh token entity and revokes it as part of rotation.
     * Wrapped in a transaction since this class is injected as a Spring bean,
     * so calls to this method go through the Spring proxy correctly.
     *
     * @param hashedToken the hashed value of the raw refresh token
     * @return the user associated with the now-revoked token
     */
    @Transactional
    public User validateAndRevoke(String hashedToken) {

        log.info("Validating and revoking refresh token");

        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> {
                    log.warn("Invalid refresh token");
                    throw new IllegalArgumentException("Jeton de rafraîchissement invalide");
                });

        if (existingToken.isRevoked()) {
            log.warn("Attempted to use a revoked refresh token");
            throw new IllegalArgumentException("Le jeton de rafraîchissement a été révoqué");
        }

        if (existingToken.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Attempted to use an expired refresh token");
            throw new IllegalArgumentException("Le jeton de rafraîchissement a expiré");
        }

        existingToken.setRevoked(true);
        refreshTokenRepository.save(existingToken);

        log.info("Refresh token successfully validated and revoked");
        return existingToken.getUser();
    }
}