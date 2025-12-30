package com.example.demo.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Testcontainers configuration providing a singleton PostgreSQL container with reuse enabled.
 * <p>
 * This configuration follows Spring Boot best practices for Testcontainers:
 * - Single PostgreSQL container shared across ALL test classes
 * - Automatic datasource configuration via @ServiceConnection
 * - Container reuse enabled to persist container between test runs (requires testcontainers.reuse.enable=true)
 * - Container is started once and reused, significantly improving test performance
 * <p>
 * Container Reuse Benefits:
 * - First test run: ~15s container startup
 * - Subsequent runs: ~0s container startup (reuses existing container)
 * - Total time savings: ~15s per test run
 * <p>
 * Setup Required:
 * 1. Create ~/.testcontainers.properties with: testcontainers.reuse.enable=true
 * 2. Or set environment variable: TESTCONTAINERS_REUSE_ENABLE=true
 * <p>
 * Thread-Safety:
 * - PostgreSQL handles concurrent connections (default max_connections=100)
 * - Each test uses @Transactional with rollback for data isolation
 * - Safe for parallel test execution
 * <p>
 * Usage: Add @Import(TestcontainersConfiguration.class) to your test classes
 * or use @ContextConfiguration to load this configuration automatically.
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/testing/testcontainers.html">Spring Boot Testcontainers Documentation</a>
 * @see <a href="https://java.testcontainers.org/features/reuse/">Testcontainers Reuse Feature</a>
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /**
     * Creates a singleton PostgreSQL container for all integration tests with reuse enabled.
     * <p>
     * The @ServiceConnection annotation automatically configures Spring Boot's datasource properties:
     * - spring.datasource.url
     * - spring.datasource.username
     * - spring.datasource.password
     * <p>
     * Container Reuse:
     * - withReuse(true) enables container persistence between test runs
     * - withLabel() provides unique identification for the reusable container
     * - Container is stopped only when Docker daemon stops or manual cleanup
     * <p>
     * Thread-Safety:
     * - PostgreSQL container handles concurrent connections safely
     * - Each parallel test gets its own database transaction
     * - @Transactional ensures proper isolation and rollback
     *
     * @return PostgreSQL container configured for testing with reuse enabled
     */
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("demo")
                .withUsername("demo")
                .withPassword("demo")
                // Enable container reuse between test runs (requires testcontainers.reuse.enable=true)
                .withReuse(true)
                // Label for identifying this reusable container
                .withLabel("com.example.demo.test", "postgres-integration-tests");
    }
}

