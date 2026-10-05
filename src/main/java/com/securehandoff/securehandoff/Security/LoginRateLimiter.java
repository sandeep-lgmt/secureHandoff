package com.securehandoff.securehandoff.Security;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.common.errors.ApiException;
import org.springframework.http.HttpStatus;
import com.securehandoff.securehandoff.exception.ApiException;
import org.springframework.stereotype.Component;
@Component
public class LoginRateLimiter {
     private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000; // 15 minutes

    private final ConcurrentHashMap<String, Window> attempts = new ConcurrentHashMap<>();

    public void checkAllowed(String email) {
        Window window = attempts.computeIfAbsent(email, k -> new Window());

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

    private static class Window {
        volatile long windowStartMs = Instant.now().toEpochMilli();
        AtomicInteger count = new AtomicInteger(0);
    }

}
