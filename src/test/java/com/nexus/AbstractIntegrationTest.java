package com.nexus;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "nexus.ratelimit.enabled=false")
public abstract class AbstractIntegrationTest {
    // Integration tests run against the dev Postgres from docker-compose (localhost:5432),
    // configured via application.yml. Start it with `docker compose up -d` before testing.
    //
    // Rate limiting is disabled here so the auth-heavy integration tests (many register/login
    // calls from the same loopback address) aren't throttled; the 429 path is covered in
    // isolation by RateLimitingTest.
    //
    // Note: these tests share the dev database, so test fixtures must not collide with data
    // created by manual API calls. A fresh `docker compose down -v && up -d` gives a clean slate.
}
