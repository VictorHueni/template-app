package com.example.demo.testsupport;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for REST API integration tests.
 * <p>
 * - Starts Spring Boot on a RANDOM_PORT.
 * - Configures RestAssured to talk to that port on localhost.
 * - Inherits PostgreSQL Testcontainers setup from {@link AbstractIntegrationTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractRestAssuredIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    protected int port;

    @BeforeEach
    void configureRestAssuredBaseUri() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }
}
