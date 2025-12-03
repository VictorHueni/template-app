package com.example.demo.greeting.infrastructure.web;

import com.example.demo.greeting.model.Greeting;
import com.example.demo.greeting.repository.GreetingRepository;
import com.example.demo.testsupport.AbstractRestAssuredIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

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

    @Autowired
    GreetingRepository greetingRepository;

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

    @Test
    void listsGreetingsWithPagination() {
        // 1. Arrange: Create some data to list
        greetingRepository.save(new Greeting(UUID.randomUUID(), "A", "Message A", Instant.now()));
        greetingRepository.save(new Greeting(UUID.randomUUID(), "B", "Message B", Instant.now()));

        // 2. Act & Assert
        given()
                .contentType("application/json")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/greetings")
                .then()
                .statusCode(200)
                // Validate Data Array
                .body("data", hasSize(greaterThanOrEqualTo(2)))
                .body("data[0].id", notNullValue())
                .body("data[0].message", notNullValue())
                // Validate Page Metadata
                .body("meta.pageNumber", equalTo(0))
                .body("meta.pageSize", equalTo(10))
                .body("meta.totalElements", greaterThanOrEqualTo(2));
    }
}
