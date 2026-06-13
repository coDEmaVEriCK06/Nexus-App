package com.nexus;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "nexus.ratelimit.enabled=false")
public abstract class AbstractIntegrationTest {
    // Local + CI tests run against the Postgres from docker-compose (localhost:5432),
    // the same DB used since Phase 1. Testcontainers stays wired in pom.xml and is
    // activated in CI (Phase 6), where the Docker environment is clean.
    //
    // Rate limiting is disabled here so the auth-heavy integration tests (which issue
    // many register/login calls from the same loopback address) aren't throttled.
    // The 429 behavior is covered in isolation by RateLimitingTest.
}
