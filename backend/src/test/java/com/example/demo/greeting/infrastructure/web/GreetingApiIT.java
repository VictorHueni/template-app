package com.example.demo.greeting.infrastructure.web;


import com.example.demo.testsupport.AbstractRestAssuredIntegrationTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * API-level integration tests for Greeting HTTP endpoints.
 * Hits the real Spring Boot app and real PostgreSQL (via Testcontainers).
 */
class GreetingApiIT extends AbstractRestAssuredIntegrationTest {

    @Test
    void createsGreetingAndReturnsContract() {
        given()
                .contentType("application/json")
                .body("""
                       {"name": "Charlie"}
                       """)
                .when()
                .post("/api/greetings")
                .then()
                .statusCode(200)
                .body("name", equalTo("Charlie"))
                .body("message", equalTo("Hello Charlie"))
                .body("id", notNullValue())
                .body("createdAt", notNullValue());
    }
}
