package com.nexus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the rate limiter in isolation: with a tiny capacity, requests up to capacity
 * are processed normally (invalid credentials -> 401) and the next is rejected with 429
 * by the filter before it reaches the controller. Standalone (does not extend
 * AbstractIntegrationTest) so it can enable limiting without affecting the rest of the suite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "nexus.ratelimit.enabled=true",
        "nexus.ratelimit.capacity=3",
        "nexus.ratelimit.refill-tokens=3",
        "nexus.ratelimit.refill-period=1m"
})
class RateLimitingTest {

    @Autowired
    private MockMvc mvc;

    private static final String BAD_LOGIN =
            "{\"username\":\"nobody_rl\",\"password\":\"wrongpassword\"}";

    @Test
    void blocksAuthRequestsAfterCapacityIsExhausted() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(BAD_LOGIN))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BAD_LOGIN))
                .andExpect(status().isTooManyRequests());
    }
}
