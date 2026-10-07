package com.draughts.adapter.in.web;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Boots the full application against PostgreSQL. Uses Testcontainers by default; set
 * {@code DRAUGHTS_TEST_DATABASE_URL} (plus optional {@code _USERNAME}/{@code _PASSWORD}) to use an
 * existing database instead.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "draughts.ai.sweep-interval=PT1S")
abstract class IntegrationTest {

    private static PostgreSQLContainer postgres;

    @LocalServerPort
    protected int port;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        var url = System.getenv("DRAUGHTS_TEST_DATABASE_URL");
        if (url != null) {
            registry.add("spring.datasource.url", () -> url);
            registry.add("spring.datasource.username", () -> env("DRAUGHTS_TEST_DATABASE_USERNAME", "draughts"));
            registry.add("spring.datasource.password", () -> env("DRAUGHTS_TEST_DATABASE_PASSWORD", "draughts"));
            return;
        }
        synchronized (IntegrationTest.class) {
            if (postgres == null) {
                postgres = new PostgreSQLContainer("postgres:17-alpine");
                postgres.start();
            }
        }
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static String env(String name, String fallback) {
        var value = System.getenv(name);
        return value == null ? fallback : value;
    }

    protected ApiClient newClient() {
        return new ApiClient("http://localhost:" + port);
    }
}
