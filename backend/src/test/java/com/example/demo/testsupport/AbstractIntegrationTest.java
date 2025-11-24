package com.example.demo.testsupport;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for Spring integration tests that need a real PostgreSQL.
 * <p>
 * Starts a shared Testcontainers PostgreSQL instance once per test run and
 * exposes its JDBC URL, username and password via Spring Boot properties.
 */
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("demo")
                    .withUsername("demo")
                    .withPassword("demo");

    @DynamicPropertySource
    static void overrideDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("SPRING_SERVER_PORT", () -> "0");

    }

    @BeforeAll
    static void checkRunning() {
        // forces container startup early
        POSTGRES.isRunning();
    }
}
