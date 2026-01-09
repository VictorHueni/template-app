package com.example.demo.testsupport;

import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.example.demo.testsupport.auth.MockSecurityConfig;

/**
 * Base class for Spring integration tests that need a real PostgreSQL database.
 * <p>
 * This class uses Spring Boot's best practice approach for Testcontainers:
 * - Imports {@link TestcontainersConfiguration} which provides a singleton PostgreSQL container
 * - The container is shared across ALL test classes for optimal performance
 * - Automatic datasource configuration via @ServiceConnection
 * - Container reuse enabled to persist between test runs (requires testcontainers.reuse.enable=true)
 * <p>
 * Thread-Safety for Parallel Execution:
 * - ✅ PostgreSQL container handles concurrent connections (max_connections=100)
 * - ✅ Each test should use @Transactional to ensure data isolation
 * - ✅ Spring TestContext framework is thread-safe
 * - ✅ No shared mutable state between test classes
 * - ⚠️ ️ REQUIRED: Use @ActiveProfiles({"test", "integration"}) for all integration tests
 * <p>
 * Subclasses should add:
 * - @SpringBootTest with appropriate webEnvironment
 * - @Transactional for automatic rollback (ensures test isolation in parallel execution)
 * - @ActiveProfiles("test") for consistent Spring context (optional but recommended)
 * <p>
 * Example:
 * <pre>
 * {@code
 * @SpringBootTest
 * @Transactional
 * class MyRepositoryIT extends AbstractIntegrationTest {
 *     // Test methods using real PostgreSQL
 *     // Each test runs in its own transaction (rolled back after test)
 *     // Safe for parallel execution
 * }
 * }
 * </pre>
 *
 * @see TestcontainersConfiguration
 */
@SpringJUnitConfig(classes = {TestcontainersConfiguration.class, MockSecurityConfig.class})
public abstract class AbstractIntegrationTest {
    // No fields needed - container is managed by TestcontainersConfiguration
}
