package com.securehandoff.securehandoff.security;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.securehandoff.securehandoff.exception.ApiException;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000L; // 15 minutes

    private final ConcurrentHashMap<String, Window> attempts = new ConcurrentHashMap<>();

    public void checkAllowed(String email) {
        Window window = attempts.get(email);
        if (window == null) {
            return;
        }

        synchronized (window) {
            long now = Instant.now().toEpochMilli();
            if (now - window.windowStartMs > WINDOW_MS) {
                window.windowStartMs = now;
                window.count.set(0);
            }

            if (window.count.get() >= MAX_ATTEMPTS) {
                throw new ApiException(
                        "Too many login attempts. Please try again in a few minutes.",
                        HttpStatus.TOO_MANY_REQUESTS
                );
            }
        }
    }

    public void recordFailedAttempt(String email) {
        Window window = attempts.computeIfAbsent(email, k -> new Window());
        window.count.incrementAndGet();
    }

    public void recordSuccess(String email) {
        attempts.remove(email);
    }

    /** Prevents the map from growing forever with emails that were tried once. */
    @Scheduled(fixedDelay = 600_000)
    void purgeExpired() {
        long now = Instant.now().toEpochMilli();
        attempts.entrySet().removeIf(e -> now - e.getValue().windowStartMs > WINDOW_MS);
    }

    private static class Window {
        volatile long windowStartMs = Instant.now().toEpochMilli();
        final AtomicInteger count = new AtomicInteger(0);
    }
}
