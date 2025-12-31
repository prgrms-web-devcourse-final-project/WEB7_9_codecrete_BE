package com.back.web7_9_codecrete_be.domain.artists.service.seed;

// 모든 Spotify API 호출 전에 최소 간격을 보장하여 Rate Limit 위반 방지

public class SimpleRateLimiter {
    private final long minIntervalMs;
    private long lastCallAt = 0;

    public SimpleRateLimiter(long minIntervalMs) {
        this.minIntervalMs = minIntervalMs;
    }

    public synchronized void acquire() {
        long now = System.currentTimeMillis();
        long wait = (lastCallAt + minIntervalMs) - now;
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastCallAt = System.currentTimeMillis();
    }
}

