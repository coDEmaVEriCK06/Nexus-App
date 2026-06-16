package com.nexus;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Verifies that throttled requests increment the nexus.ratelimit.rejected counter.
 * Uses a distinct capacity so it runs in its own application context (fresh token bucket),
 * independent of RateLimitingTest's bucket state.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "nexus.ratelimit.enabled=true",
        "nexus.ratelimit.capacity=2",
        "nexus.ratelimit.refill-tokens=2",
        "nexus.ratelimit.refill-period=1m"
})
class RateLimitMetricTest {

    @Autowired private MockMvc mvc;
    @Autowired private MeterRegistry meterRegistry;

    private static final String BAD_LOGIN =
            "{\"username\":\"nobody_rlm\",\"password\":\"wrongpassword\"}";

    @Test
    void rejectedRequestsIncrementTheCounter() throws Exception {
        double before = rejectedCount();
        // Capacity is 2, so firing 5 guarantees several rejections regardless of bucket state.
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(BAD_LOGIN));
        }
        assertThat(rejectedCount())
                .as("nexus.ratelimit.rejected should increase when requests are throttled")
                .isGreaterThan(before);
    }

    private double rejectedCount() {
        Counter c = meterRegistry.find("nexus.ratelimit.rejected").counter();
        return c == null ? 0.0 : c.count();
    }
}
