package com.example.demo.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Testcontainers configuration providing a singleton PostgreSQL container.
 * <p>
 * This configuration follows Spring Boot best practices for Testcontainers:
 * - Single PostgreSQL container shared across ALL test classes
 * - Automatic datasource configuration via @ServiceConnection
 * - Container is started once and reused, significantly improving test performance
 * <p>
 * Usage: Add @Import(TestcontainersConfiguration.class) to your test classes
 * or use @ContextConfiguration to load this configuration automatically.
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/testing/testcontainers.html">Spring Boot Testcontainers Documentation</a>
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /**
     * Creates a singleton PostgreSQL container for all integration tests.
     * <p>
     * The @ServiceConnection annotation automatically configures Spring Boot's datasource properties:
     * - spring.datasource.url
     * - spring.datasource.username
     * - spring.datasource.password
     * <p>
     * The container is started once when first needed and reused across all test classes,
     * dramatically reducing test execution time.
     *
     * @return PostgreSQL container configured for testing
     */
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("demo")
                .withUsername("demo")
                .withPassword("demo");
    }
}

