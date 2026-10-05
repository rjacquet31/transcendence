package com.transcendance.backend.services;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.transcendance.backend.dto.TwoFactorSetupResponseDto;
import com.transcendance.backend.model.User;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.util.Utils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(TwoFactorService.class);

    private static final String ISSUER = "ft_transcendence";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration PENDING_LOGIN_TTL = Duration.ofMinutes(5);

    private final UserService userService;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(
            new DefaultCodeGenerator(), new SystemTimeProvider());
    private final SecureRandom random = new SecureRandom();

    @Value("${app.2fa.lock-minutes:15}")
    private long lockMinutes = 15;

    // Kept in memory on purpose: an exception inside LoginService's transaction
    // would roll back a counter stored in the database. Lost on restart.
    private final Map<UUID, FailedAttempts> failedAttempts = new ConcurrentHashMap<>();

    // OAuth 42 logins waiting for their 2FA code: opaque token -> user.
    private final Map<String, PendingLogin> pendingLogins = new ConcurrentHashMap<>();

    private record FailedAttempts(int count, Instant lockedUntil) {}

    private record PendingLogin(UUID userId, Instant expiresAt) {}

    /**
     * Generates a new TOTP secret for the user and the matching QR code.
     * <p>
     * The secret is stored right away but 2FA stays disabled until the user
     * proves their app is configured by sending a first valid code to
     * {@link #enable(User, String)}. Calling setup again before that simply
     * replaces the pending secret.
     *
     * @param user the authenticated user
     * @return the QR code as a PNG data URI and the raw secret
     * @throws IllegalStateException if 2FA is already enabled for this user
     * @throws QrGenerationException if the QR code image cannot be generated
     */
    public TwoFactorSetupResponseDto generateSetup(User user) throws QrGenerationException {
        if (user.isTwoFactorEnabled()) {
            log.warn("2FA setup refused, already enabled for user id: {}", user.getIdUser());
            throw new IllegalStateException("La double authentification est déjà activée");
        }

        String secret = secretGenerator.generate();

        QrData data = new QrData.Builder()
                .label(user.getEmail())
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        String qrCodeDataUri = Utils.getDataUriForImage(qrGenerator.generate(data), qrGenerator.getImageMimeType());

        user.setTotpSecret(secret);
        userService.saveUser(user);

        log.info("2FA secret generated for user id: {}", user.getIdUser());
        return new TwoFactorSetupResponseDto(qrCodeDataUri, secret);
    }

    /**
     * Checks a code against the user's active 2FA secret, with brute-force
     * protection: after {@value #MAX_FAILED_ATTEMPTS} wrong codes the user is
     * locked out for {@code app.2fa.lock-minutes}. A right code resets the count.
     *
     * @param user the user with 2FA enabled
     * @param code the 6-digit code from the authenticator app
     * @throws TwoFactorLockedException if the user is currently locked out
     * @throws IllegalArgumentException if the code is invalid
     */
    public void verifyCode(User user, String code) {
        UUID userId = user.getIdUser();

        // The attempt is counted atomically before checking the code, so parallel
        // requests cannot squeeze in more than MAX_FAILED_ATTEMPTS guesses.
        FailedAttempts attempts = failedAttempts.compute(userId, (id, previous) -> {
            Instant now = Instant.now();
            boolean locked = previous != null && previous.lockedUntil() != null
                    && now.isBefore(previous.lockedUntil());
            if (locked) {
                return new FailedAttempts(previous.count() + 1, previous.lockedUntil());
            }
            // No history, or a lock that has expired: start a fresh count.
            int count = (previous == null || previous.lockedUntil() != null) ? 1 : previous.count() + 1;
            Instant lockedUntil = count >= MAX_FAILED_ATTEMPTS ? now.plus(Duration.ofMinutes(lockMinutes)) : null;
            return new FailedAttempts(count, lockedUntil);
        });

        if (attempts.count() > MAX_FAILED_ATTEMPTS) {
            log.warn("2FA code refused, user id {} is locked out", userId);
            throw new TwoFactorLockedException();
        }

        if (codeVerifier.isValidCode(user.getTotpSecret(), code)) {
            failedAttempts.remove(userId);
            return;
        }

        log.warn("Invalid 2FA code for user id {} ({} failed attempt(s))", userId, attempts.count());
        throw new IllegalArgumentException("Code invalide");
    }

    /**
     * Starts an OAuth 42 login that still needs a 2FA code.
     *
     * @param user the user who passed the 42 authentication
     * @return a random single-use token, valid 5 minutes
     */
    public String createPendingLogin(User user) {
        Instant now = Instant.now();
        pendingLogins.values().removeIf(pending -> now.isAfter(pending.expiresAt()));

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        pendingLogins.put(token, new PendingLogin(user.getIdUser(), now.plus(PENDING_LOGIN_TTL)));
        return token;
    }

    /**
     * Finds the user behind a pending login token. The token stays valid so
     * the user can retry a wrong code; brute force is capped by {@link #verifyCode}.
     *
     * @param token the token from the two_factor_token cookie, may be null
     * @return the user id, or empty if the token is unknown or expired
     */
    public Optional<UUID> findPendingLogin(String token) {
        if (token == null) {
            return Optional.empty();
        }
        PendingLogin pending = pendingLogins.get(token);
        if (pending == null || Instant.now().isAfter(pending.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(pending.userId());
    }

    /**
     * Invalidates a pending login token once the login is complete.
     *
     * @param token the token to remove
     */
    public void removePendingLogin(String token) {
        pendingLogins.remove(token);
    }

    /**
     * Enables 2FA once the user has sent a valid code generated from the
     * pending secret.
     *
     * @param user the authenticated user
     * @param code the 6-digit code from the authenticator app
     * @throws IllegalStateException if 2FA is already enabled or no setup was done
     * @throws IllegalArgumentException if the code is invalid
     */
    public void enable(User user, String code) {
        if (user.isTwoFactorEnabled()) {
            throw new IllegalStateException("La double authentification est déjà activée");
        }
        if (user.getTotpSecret() == null) {
            throw new IllegalStateException("Configure d'abord la double authentification");
        }
        if (!codeVerifier.isValidCode(user.getTotpSecret(), code)) {
            log.warn("Invalid 2FA code for user id: {}", user.getIdUser());
            throw new IllegalArgumentException("Code invalide");
        }

        user.setTwoFactorEnabled(true);
        userService.saveUser(user);

        log.info("2FA enabled for user id: {}", user.getIdUser());
    }

    /**
     * Disables 2FA after checking a code from the authenticator app, so a
     * hijacked session alone is not enough to turn it off. The secret is
     * cleared: enabling again requires a fresh setup and a new QR code.
     *
     * @param user the authenticated user
     * @param code the 6-digit code from the authenticator app
     * @throws IllegalStateException if 2FA is not enabled
     * @throws TwoFactorLockedException if the user is locked out
     * @throws IllegalArgumentException if the code is invalid
     */
    public void disable(User user, String code) {
        if (!user.isTwoFactorEnabled()) {
            throw new IllegalStateException("La double authentification n'est pas activée");
        }
        verifyCode(user, code);

        user.setTwoFactorEnabled(false);
        user.setTotpSecret(null);
        userService.saveUser(user);

        log.info("2FA disabled for user id: {}", user.getIdUser());
    }
}
