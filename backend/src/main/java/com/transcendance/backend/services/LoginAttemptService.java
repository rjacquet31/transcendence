package com.transcendance.backend.services;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Brute-force protection of the password login: after
 * {@value #MAX_FAILED_ATTEMPTS} failed attempts, the same client is locked
 * out of the account for {@code app.login.lock-minutes}.
 */
@Service
public class LoginAttemptService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(LoginAttemptService.class);

    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Value("${app.login.lock-minutes:15}")
    private long lockMinutes = 15;

    private final Map<String, FailedAttempts> failedAttempts = new ConcurrentHashMap<>();

    private record FailedAttempts(int count, Instant lastFailure, Instant lockedUntil) {}

    /** Key attempts are counted under: email and client address. */
    public String keyOf(String email, String clientAddress) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return normalizedEmail + "|" + clientAddress;
    }

    /** @return true if the login must be refused without being checked */
    public boolean isLocked(String key) {
        FailedAttempts attempts = failedAttempts.get(key);
        return attempts != null && attempts.lockedUntil() != null
                && Instant.now().isBefore(attempts.lockedUntil());
    }

    /** Counts one more failed login. */
    public void recordFailure(String key) {
        FailedAttempts attempts = failedAttempts.compute(key, (k, previous) -> {
            Instant now = Instant.now();
            int count = (previous == null || previous.lockedUntil() != null) ? 1 : previous.count() + 1;
            Instant lockedUntil = count >= MAX_FAILED_ATTEMPTS ? now.plus(Duration.ofMinutes(lockMinutes)) : null;
            return new FailedAttempts(count, now, lockedUntil);
        });

        if (attempts.lockedUntil() != null) {
            log.warn("Login locked for {} minute(s) after {} failed attempts", lockMinutes, attempts.count());
        }
    }

    /** Forgets the failed attempts once a login succeeds. */
    public void reset(String key) {
        failedAttempts.remove(key);
    }

    /** Drops expired locks and old attempts, so the map cannot grow forever. */
    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void purgeStaleAttempts() {
        Instant now = Instant.now();
        Instant oldest = now.minus(Duration.ofMinutes(lockMinutes));
        failedAttempts.values().removeIf(attempts -> attempts.lockedUntil() != null
                ? now.isAfter(attempts.lockedUntil())
                : attempts.lastFailure().isBefore(oldest));
    }
}
