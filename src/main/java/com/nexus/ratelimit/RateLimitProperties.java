package com.nexus.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Externalized rate-limit configuration, bound from {@code nexus.ratelimit.*}.
 * Defaults: 5 requests, refilling 5 tokens per minute, per client IP, enabled.
 */
@Component
@ConfigurationProperties(prefix = "nexus.ratelimit")
public class RateLimitProperties {

    private boolean enabled = true;
    private int capacity = 5;
    private int refillTokens = 5;
    private Duration refillPeriod = Duration.ofMinutes(1);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getRefillTokens() {
        return refillTokens;
    }

    public void setRefillTokens(int refillTokens) {
        this.refillTokens = refillTokens;
    }

    public Duration getRefillPeriod() {
        return refillPeriod;
    }

    public void setRefillPeriod(Duration refillPeriod) {
        this.refillPeriod = refillPeriod;
    }
}
