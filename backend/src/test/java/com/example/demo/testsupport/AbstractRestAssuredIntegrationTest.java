package com.example.demo.testsupport;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Base class for REST API integration tests using RestAssured.
 * <p>
 * - Configures RestAssured to communicate with the Spring Boot application on a random port
 * - Inherits singleton PostgreSQL Testcontainers setup from {@link AbstractIntegrationTest}
 * - Resets RestAssured configuration after each test to avoid test pollution
 * <p>
 * Subclasses must add:
 * - @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
 * - @ActiveProfiles if needed (e.g., "test")
 * <p>
 * Example:
 * <pre>
 * {@code
 * @SpringBootTest(webEnvironment = RANDOM_PORT)
 * @ActiveProfiles("test")
 * class MyApiIT extends AbstractRestAssuredIntegrationTest {
 *     // Test methods using RestAssured
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
    }

    @AfterEach
    void resetRestAssured() {
        RestAssured.reset();
    }
}
