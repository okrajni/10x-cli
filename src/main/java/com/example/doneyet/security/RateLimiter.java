package com.example.doneyet.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiter {
    private static final long CLEANUP_INTERVAL_MS = 60000; // 1 minute
    private static final long RATE_LIMIT_WINDOW_MS = 900000; // 15 minutes

    private final Map<String, RateLimitEntry> loginAttempts = new ConcurrentHashMap<>();
    private final Map<String, RateLimitEntry> registrationAttempts = new ConcurrentHashMap<>();

    public RateLimiter() {
        startCleanupThread();
    }

    public boolean isLoginAllowed(String email, String ipAddress) {
        String key = email + ":" + ipAddress;
        RateLimitEntry entry = loginAttempts.get(key);

        if (entry == null || isWindowExpired(entry.firstAttemptTime)) {
            return true;
        }

        // Allow 5 attempts per window
        return entry.attemptCount < 5;
    }

    public void recordLoginAttempt(String email, String ipAddress) {
        String key = email + ":" + ipAddress;
        long now = System.currentTimeMillis();

        loginAttempts.compute(key, (k, v) -> {
            if (v == null || isWindowExpired(v.firstAttemptTime)) {
                return new RateLimitEntry(now, 1);
            }
            v.attemptCount++;
            return v;
        });
    }

    public boolean isRegistrationAllowed(String ipAddress) {
        String key = "reg:" + ipAddress;
        RateLimitEntry entry = registrationAttempts.get(key);

        if (entry == null || isWindowExpired(entry.firstAttemptTime)) {
            return true;
        }

        // Allow 3 registrations per window
        return entry.attemptCount < 3;
    }

    public void recordRegistrationAttempt(String ipAddress) {
        String key = "reg:" + ipAddress;
        long now = System.currentTimeMillis();

        registrationAttempts.compute(key, (k, v) -> {
            if (v == null || isWindowExpired(v.firstAttemptTime)) {
                return new RateLimitEntry(now, 1);
            }
            v.attemptCount++;
            return v;
        });
    }

    public long getLoginRetryAfterSeconds(String email, String ipAddress) {
        String key = email + ":" + ipAddress;
        RateLimitEntry entry = loginAttempts.get(key);

        if (entry == null) {
            return 0;
        }

        long elapsedMs = System.currentTimeMillis() - entry.firstAttemptTime;
        long remainingMs = RATE_LIMIT_WINDOW_MS - elapsedMs;

        return Math.max(0, remainingMs / 1000);
    }

    private boolean isWindowExpired(long firstAttemptTime) {
        return System.currentTimeMillis() - firstAttemptTime > RATE_LIMIT_WINDOW_MS;
    }

    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(CLEANUP_INTERVAL_MS);
                    cleanupExpiredEntries();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "RateLimiter-Cleanup");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        loginAttempts.entrySet().removeIf(entry -> now - entry.getValue().firstAttemptTime > RATE_LIMIT_WINDOW_MS);
        registrationAttempts.entrySet().removeIf(entry -> now - entry.getValue().firstAttemptTime > RATE_LIMIT_WINDOW_MS);
    }

    private static class RateLimitEntry {
        long firstAttemptTime;
        int attemptCount;

        RateLimitEntry(long firstAttemptTime, int attemptCount) {
            this.firstAttemptTime = firstAttemptTime;
            this.attemptCount = attemptCount;
        }
    }
}
