package com.transcendance.backend.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.transcendance.backend.dto.LoginResponseTokenDto;
import com.transcendance.backend.model.RefreshToken;
import com.transcendance.backend.model.User;
import com.transcendance.backend.repository.RefreshTokenRepository;

import jakarta.transaction.Transactional;

@Service
public class RefreshTokenService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenRotationService rotationService;
    private final JWTService jwtService;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                                RefreshTokenRotationService rotationService,
                                JWTService jwtService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.rotationService = rotationService;
        this.jwtService = jwtService;
    }

    @Value("${jwt.refresh-token-validity-days}")
    private int refreshTokenValidityDays;

    /**
     * Opens a new session: revokes the previous ones, then issues both tokens.
     *
     * @param user the user for whom to open a session
     * @return the access token and the raw refresh token
     */
    @Transactional
    public LoginResponseTokenDto openSession(User user) {

        log.info("Generating refresh token for user: {}", user.getEmail());

        revokeActiveSessions(user);

        String rawToken = generateSecureRandomToken();
        String hashedToken = hashToken(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashedToken);
        refreshToken.setExpiresAt(Instant.now().plus(refreshTokenValidityDays, ChronoUnit.DAYS));
        refreshToken.setRevoked(false);
        refreshToken.setCreatedAt(Instant.now());

        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);

        log.info("Refresh token generated for user: {}", user.getEmail());
        return new LoginResponseTokenDto(
                jwtService.generateAccessToken(user, savedToken.getIdToken()), rawToken);
    }

    /**
     * @param sessionId the session id read from the access token, may be null
     * @return true if the matching refresh token is not revoked
     */
    public boolean isSessionActive(String sessionId) {
        if (sessionId == null) {
            return false;
        }
        try {
            return refreshTokenRepository.existsByIdTokenAndRevokedFalse(Long.parseLong(sessionId));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * revokes every refresh token still active for this user
     *
     * @param user the user whose other active sessions must be revoked
     */
    public void revokeActiveSessions(User user) {
        List<RefreshToken> refreshTokenList = refreshTokenRepository.findByUserAndRevokedFalse(user);
        for (RefreshToken refreshToken : refreshTokenList) {
            refreshToken.setRevoked(true);
        }
        refreshTokenRepository.saveAll(refreshTokenList);
    }

    /**
     * Generates a secure random token.
     *
     * @return the generated token
     */
    private String generateSecureRandomToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[64];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Hashes the given raw token.
     *
     * @param rawToken the raw token to hash
     * @return the hashed token
     */
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Refreshes the access token using the provided raw refresh token.
     * Validation + revocation of the old token happens in a separate
     * bean (RefreshTokenRotationService) so the transaction is real.
     *
     * @param rawRefreshToken the raw refresh token from the cookie
     * @return the new access token and new refresh token
     */
    public LoginResponseTokenDto refreshAccessToken(String rawRefreshToken) {

        String hashedToken = hashToken(rawRefreshToken);

        User user = rotationService.validateAndRevoke(hashedToken);

        return openSession(user);
    }
}