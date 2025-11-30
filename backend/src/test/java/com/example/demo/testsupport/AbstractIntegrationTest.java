package com.example.demo.testsupport;

import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Base class for Spring integration tests that need a real PostgreSQL database.
 * <p>
 * This class uses Spring Boot's best practice approach for Testcontainers:
 * - Imports {@link TestcontainersConfiguration} which provides a singleton PostgreSQL container
 * - The container is shared across ALL test classes for optimal performance
 * - Automatic datasource configuration via @ServiceConnection
 * <p>
 * Subclasses should add:
 * - @SpringBootTest with appropriate webEnvironment
 * - @ActiveProfiles if needed
 * <p>
 * Example:
 * <pre>
 * {@code
 * @SpringBootTest
 * class MyRepositoryIT extends AbstractIntegrationTest {
 *     // Test methods using real PostgreSQL
 * }
 * }
 * </pre>
 *
 * @see TestcontainersConfiguration
 */
@SpringJUnitConfig(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {
    // No fields needed - container is managed by TestcontainersConfiguration
}
