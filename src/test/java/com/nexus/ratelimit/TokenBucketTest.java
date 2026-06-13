package com.nexus.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBucketTest {

    @Test
    void allowsUpToCapacityThenBlocks() {
        long[] now = {0L};
        TokenBucket bucket = new TokenBucket(3, 3, Duration.ofMinutes(1), () -> now[0]);

        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).as("fourth request with empty bucket").isFalse();
    }

    @Test
    void refillsTokensAsTimePasses() {
        long[] now = {0L};
        TokenBucket bucket = new TokenBucket(3, 3, Duration.ofMinutes(1), () -> now[0]);

        bucket.tryConsume();
        bucket.tryConsume();
        bucket.tryConsume();
        assertThat(bucket.tryConsume()).isFalse();

        now[0] += Duration.ofMinutes(1).toNanos();
        assertThat(bucket.tryConsume()).isTrue();
    }

    @Test
    void neverExceedsCapacityOnRefill() {
        long[] now = {0L};
        TokenBucket bucket = new TokenBucket(3, 3, Duration.ofMinutes(1), () -> now[0]);

        now[0] += Duration.ofHours(1).toNanos();
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isFalse();
    }
}
