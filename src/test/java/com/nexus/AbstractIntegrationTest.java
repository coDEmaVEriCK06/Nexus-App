package com.nexus;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public abstract class AbstractIntegrationTest {
    // Local + CI tests run against the Postgres from docker-compose (localhost:5432),
    // the same DB used since Phase 1. Testcontainers stays wired in pom.xml and is
    // activated in CI (Phase 6), where the Docker environment is clean.
}
