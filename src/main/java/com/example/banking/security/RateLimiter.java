package com.example.banking.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Simple in-memory token-bucket rate limiter. Sufficient for a single-instance dev app;
 * swap for Bucket4j/Redis in a clustered deployment.
 */
@Service
@RequiredArgsConstructor
public class RateLimiter {

    private final ConcurrentHashMap<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    public boolean allow(String key, int maxRequests, int windowSeconds) {
        Instant cutoff = Instant.now().minusSeconds(windowSeconds);
        Deque<Instant> log = hits.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        synchronized (log) {
            while (!log.isEmpty() && log.peekFirst().isBefore(cutoff)) {
                log.pollFirst();
            }
            if (log.size() >= maxRequests) return false;
            log.addLast(Instant.now());
            return true;
        }
    }

    public void reset(String key) { hits.remove(key); }
}
