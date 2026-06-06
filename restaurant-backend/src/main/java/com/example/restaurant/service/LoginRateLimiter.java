package com.example.restaurant.service;

import com.example.restaurant.common.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimiter {
    private final int maxFailures;
    private final Duration window;
    private final Duration lockDuration;
    private final Clock clock;
    private final Map<String, LoginAttempt> attempts = new ConcurrentHashMap<>();

    @Autowired
    public LoginRateLimiter(
            @Value("${app.security.login-rate-limit.max-failures:5}") int maxFailures,
            @Value("${app.security.login-rate-limit.window-minutes:10}") long windowMinutes,
            @Value("${app.security.login-rate-limit.lock-minutes:10}") long lockMinutes) {
        this(maxFailures, Duration.ofMinutes(windowMinutes), Duration.ofMinutes(lockMinutes), Clock.systemUTC());
    }

    LoginRateLimiter(int maxFailures, Duration window, Duration lockDuration, Clock clock) {
        this.maxFailures = Math.max(1, maxFailures);
        this.window = window;
        this.lockDuration = lockDuration;
        this.clock = clock;
    }

    public synchronized void assertAllowed(String key) {
        LoginAttempt attempt = attempts.get(key);
        if (attempt == null) {
            return;
        }
        Instant now = Instant.now(clock);
        if (attempt.lockedUntil != null) {
            if (now.isBefore(attempt.lockedUntil)) {
                throw new BusinessException("登录失败次数过多，请稍后再试");
            }
            attempts.remove(key);
            return;
        }
        if (attempt.firstFailureAt != null && now.isAfter(attempt.firstFailureAt.plus(window))) {
            attempts.remove(key);
        }
    }

    public synchronized void recordFailure(String key) {
        Instant now = Instant.now(clock);
        LoginAttempt attempt = attempts.computeIfAbsent(key, ignored -> new LoginAttempt());
        if (attempt.firstFailureAt == null || now.isAfter(attempt.firstFailureAt.plus(window))) {
            attempt.firstFailureAt = now;
            attempt.failures = 0;
            attempt.lockedUntil = null;
        }
        attempt.failures++;
        if (attempt.failures >= maxFailures) {
            attempt.lockedUntil = now.plus(lockDuration);
        }
    }

    public synchronized void clear(String key) {
        attempts.remove(key);
    }

    private static class LoginAttempt {
        private Instant firstFailureAt;
        private Instant lockedUntil;
        private int failures;
    }
}
