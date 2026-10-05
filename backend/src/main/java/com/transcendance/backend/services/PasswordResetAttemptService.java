package com.transcendance.backend.services;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetAttemptService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(PasswordResetAttemptService.class);

    private static final int MAX_REQUESTS_PER_WINDOW = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Requests> requests = new ConcurrentHashMap<>();

    private record Requests(int count, Instant windowStart) {}

    public boolean tryAcquire(String clientAddress) {
        Requests current = requests.compute(clientAddress, (k, previous) -> {
            Instant now = Instant.now();
            if (previous == null || now.isAfter(previous.windowStart().plus(WINDOW))) {
                return new Requests(1, now);
            }
            return new Requests(previous.count() + 1, previous.windowStart());
        });

        if (current.count() > MAX_REQUESTS_PER_WINDOW) {
            log.warn("Too many password reset requests from the same client");
            return false;
        }
        return true;
    }

    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void purgeStaleRequests() {
        Instant oldest = Instant.now().minus(WINDOW);
        requests.values().removeIf(r -> r.windowStart().isBefore(oldest));
    }
}
