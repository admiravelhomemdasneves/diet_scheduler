package com.dietscheduler.backend.common;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A minimal in-process, per-key fixed-window rate limiter -- no new dependency (no bucket4j/
 * resilience4j), no external store. Good enough for a single-instance deployment; if this backend
 * is ever horizontally scaled, each instance would enforce its own independent limit, which is a
 * real gap worth revisiting then (a shared store like Redis would be the fix), but not before.
 *
 * Keys are caller-composed (e.g. {@code userId + ":external-recipe"}) so the same bucket type can
 * be reused across multiple call sites/limits without collisions.
 */
@Component
public class RateLimiterService {

    private static final long WINDOW_MILLIS = 60_000;

    private record Window(AtomicInteger count, AtomicLong windowStart) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /** @throws TooManyRequestsException if {@code key} has already been used {@code limitPerMinute} times in the current window */
    public void requireWithinLimit(String key, int limitPerMinute) {
        long now = System.currentTimeMillis();
        Window window = windows.computeIfAbsent(key, k -> new Window(new AtomicInteger(0), new AtomicLong(now)));
        synchronized (window) {
            if (now - window.windowStart().get() >= WINDOW_MILLIS) {
                window.windowStart().set(now);
                window.count().set(0);
            }
            if (window.count().incrementAndGet() > limitPerMinute) {
                throw new TooManyRequestsException("Too many requests -- please slow down and try again shortly");
            }
        }
    }
}
