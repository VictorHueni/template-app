package com.example.demo.greeting.infrastructure.web;


import com.example.demo.testsupport.AbstractRestAssuredIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * API-level integration tests for Greeting HTTP endpoints.
 * Hits the real Spring Boot app and real PostgreSQL (via Testcontainers singleton).
 * Uses the "test" profile for simplified security configuration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GreetingApiIT extends AbstractRestAssuredIntegrationTest {

    @Test
    void createsGreetingAndReturnsContract() {
        given()
                .contentType("application/json")
                .body("""
                       {"message": "Hello, World!", "recipient": "Charlie"}
                       """)
                .when()
                .post("/api/v1/greetings")
                .then()
                .statusCode(201)
                .body("recipient", equalTo("Charlie"))
                .body("message", equalTo("Hello, World!"))
                .body("id", notNullValue())
                .body("createdAt", notNullValue());
    }
}
