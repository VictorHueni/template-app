package com.example.demo.testsupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.example.demo.testsupport.persistence.SchemaContext;
import com.example.demo.testsupport.persistence.TestSchemaFilter;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;

/**
 * Base class for REST API integration tests using RestAssured.
 * <p>
 * Features:
 * - Configures RestAssured to communicate with the Spring Boot application on a random port
 * - Inherits singleton PostgreSQL Testcontainers setup from {@link AbstractIntegrationTest}
 * - Resets RestAssured configuration after each test to avoid test pollution
 * - Thread-safe for parallel test execution
 * <p>
 * Thread-Safety Guarantees:
 * - ✅ @BeforeEach/@AfterEach are executed per test method (not shared between threads)
 * - ✅ RestAssured.reset() clears all static configuration after each test
 * - ✅ Each test class gets its own random port (@LocalServerPort is thread-safe)
 * - ✅ No shared mutable state between test methods
 * <p>
 * Best Practices:
 * - Use @Transactional if your tests modify database state (not required for read-only tests)
 * - Use @ActiveProfiles("test") for consistent Spring context caching
 * - Avoid @DirtiesContext unless absolutely necessary (kills context caching)
 * <p>
 * Subclasses must add:
 * - @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
 * - @ActiveProfiles("test") for simplified security and consistent context
 * <p>
 * Example:
 * <pre>
 * {@code
 * @SpringBootTest(webEnvironment = RANDOM_PORT)
 * @ActiveProfiles("test")
 * class MyApiIT extends AbstractRestAssuredIntegrationTest {
 *     // Test methods using RestAssured
 *     // Each test is isolated and thread-safe
 * }
 * }
 * </pre>
 */
public abstract class AbstractRestAssuredIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    protected int port;

    @BeforeEach
    void configureRestAssuredBaseUri() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        String schema = SchemaContext.getSchema();
        RequestSpecBuilder builder = new RequestSpecBuilder();
        if (schema != null && !schema.isBlank()) {
            builder.addHeader(TestSchemaFilter.TEST_SCHEMA_HEADER, schema);
        }
        RestAssured.requestSpecification = builder.build();
    }

    @AfterEach
    void resetRestAssured() {
        RestAssured.reset();
    }
}
