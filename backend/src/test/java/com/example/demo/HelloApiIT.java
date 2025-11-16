package com.example.demo;

import com.example.demo.testsupport.AbstractRestAssuredIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * API-level integration tests for the legacy /api/hello endpoints.
 * <p>
 * Uses:
 * - Spring Boot @SpringBootTest (RANDOM_PORT)
 * - RestAssured for HTTP calls
 * - PostgreSQL via Testcontainers (from AbstractIntegrationTest)
 */
class HelloApiIT extends AbstractRestAssuredIntegrationTest {

    @Test
    void healthEndpointIsOk() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("ok"));
    }

    @Test
    void helloDefault() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/api/hello")
                .then()
                .statusCode(200)
                .body("message", equalTo("Hello world"));
    }

    @Test
    void helloCustomName() {
        given()
                .accept(ContentType.JSON)
                .queryParam("name", "Alice")
                .when()
                .get("/api/hello")
                .then()
                .statusCode(200)
                .body("message", equalTo("Hello Alice"));
    }
}
