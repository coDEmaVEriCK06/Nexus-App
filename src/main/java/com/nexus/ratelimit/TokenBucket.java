package com.nexus.ratelimit;

import java.time.Duration;
import java.util.function.LongSupplier;

/**
 * A thread-safe token bucket with lazy refill.
 *
 * <p>The bucket starts full ({@code capacity} tokens). Each {@link #tryConsume()}
 * removes one token if available. Tokens are replenished continuously at a rate of
 * {@code refillTokens} per {@code refillPeriod}; rather than a background thread, the
 * refill is computed lazily from elapsed time on each call, which keeps the bucket
 * allocation-free and cheap.
 *
 * <p>The clock is injectable so the algorithm can be tested deterministically without
 * sleeping; production code uses {@link System#nanoTime()}.
 */
public class TokenBucket {

    private final long capacity;
    private final double refillTokensPerNano;
    private final LongSupplier clock;

    private double availableTokens;
    private long lastRefillNanos;

    public TokenBucket(long capacity, long refillTokens, Duration refillPeriod) {
        this(capacity, refillTokens, refillPeriod, System::nanoTime);
    }

    TokenBucket(long capacity, long refillTokens, Duration refillPeriod, LongSupplier clock) {
        this.capacity = capacity;
        this.refillTokensPerNano = (double) refillTokens / refillPeriod.toNanos();
        this.clock = clock;
        this.availableTokens = capacity;
        this.lastRefillNanos = clock.getAsLong();
    }

    public synchronized boolean tryConsume() {
        refill();
        if (availableTokens >= 1.0) {
            availableTokens -= 1.0;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = clock.getAsLong();
        long elapsedNanos = now - lastRefillNanos;
        if (elapsedNanos > 0) {
            availableTokens = Math.min(capacity, availableTokens + elapsedNanos * refillTokensPerNano);
            lastRefillNanos = now;
        }
    }
}
