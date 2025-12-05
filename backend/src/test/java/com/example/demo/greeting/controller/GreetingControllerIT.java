package com.example.demo.greeting.controller;

import com.example.demo.greeting.service.GreetingService;
import com.example.demo.testsupport.AbstractRestAssuredIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
class GreetingControllerIT extends AbstractRestAssuredIntegrationTest {

    @Autowired
    GreetingService greetingService;

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
        greetingService.createGreeting("Message A", "A");
        greetingService.createGreeting("Message B", "B");

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
                .body("data[0].reference", notNullValue())
                // Validate Page Metadata
                .body("meta.pageNumber", equalTo(0))
                .body("meta.pageSize", equalTo(10))
                .body("meta.totalElements", greaterThanOrEqualTo(2));
    }

    // ============================================================
    // Integration Tests for CRUD operations (get, update, patch, delete)
    // ============================================================

    @Test
    void getsGreetingById() {
        // Arrange: Create a greeting
        var created = greetingService.createGreeting("Hello GET", "GetTest");

        // Act & Assert
        given()
                .contentType("application/json")
                .when()
                .get("/api/v1/greetings/{id}", created.getId())
                .then()
                .statusCode(200)
                .body("id", equalTo(created.getId()))
                .body("reference", equalTo(created.getReference()))
                .body("message", equalTo("Hello GET"))
                .body("recipient", equalTo("GetTest"))
                .body("createdAt", notNullValue());
    }

    @Test
    void returns404WhenGreetingNotFound() {
        given()
                .contentType("application/json")
                .when()
                .get("/api/v1/greetings/{id}", 999999999L)
                .then()
                .statusCode(404);
    }

    @Test
    void updatesGreetingWithPut() {
        // Arrange: Create a greeting
        var created = greetingService.createGreeting("Original Message", "Original");

        // Act & Assert
        given()
                .contentType("application/json")
                .body("""
                       {"message": "Updated Message", "recipient": "Updated"}
                       """)
                .when()
                .put("/api/v1/greetings/{id}", created.getId())
                .then()
                .statusCode(200)
                .body("id", equalTo(created.getId()))
                .body("reference", equalTo(created.getReference())) // reference unchanged
                .body("message", equalTo("Updated Message"))
                .body("recipient", equalTo("Updated"));
    }

    @Test
    void returns404WhenUpdatingNonExistentGreeting() {
        given()
                .contentType("application/json")
                .body("""
                       {"message": "Updated Message", "recipient": "Updated"}
                       """)
                .when()
                .put("/api/v1/greetings/{id}", 999999999L)
                .then()
                .statusCode(404);
    }

    @Test
    void patchesGreetingMessageOnly() {
        // Arrange: Create a greeting
        var created = greetingService.createGreeting("Original Message", "OriginalRecipient");

        // Act & Assert: Patch only the message
        given()
                .contentType("application/json")
                .body("""
                       {"message": "Patched Message"}
                       """)
                .when()
                .patch("/api/v1/greetings/{id}", created.getId())
                .then()
                .statusCode(200)
                .body("id", equalTo(created.getId()))
                .body("message", equalTo("Patched Message"))
                .body("recipient", equalTo("OriginalRecipient")); // unchanged
    }

    @Test
    void patchesGreetingRecipientOnly() {
        // Arrange: Create a greeting
        var created = greetingService.createGreeting("Original Message", "OriginalRecipient");

        // Act & Assert: Patch only the recipient
        given()
                .contentType("application/json")
                .body("""
                       {"recipient": "PatchedRecipient"}
                       """)
                .when()
                .patch("/api/v1/greetings/{id}", created.getId())
                .then()
                .statusCode(200)
                .body("id", equalTo(created.getId()))
                .body("message", equalTo("Original Message")) // unchanged
                .body("recipient", equalTo("PatchedRecipient"));
    }

    @Test
    void returns404WhenPatchingNonExistentGreeting() {
        given()
                .contentType("application/json")
                .body("""
                       {"message": "Patched Message"}
                       """)
                .when()
                .patch("/api/v1/greetings/{id}", 999999999L)
                .then()
                .statusCode(404);
    }

    @Test
    void deletesGreeting() {
        // Arrange: Create a greeting
        var created = greetingService.createGreeting("To Delete", "DeleteTest");

        // Act: Delete the greeting
        given()
                .when()
                .delete("/api/v1/greetings/{id}", created.getId())
                .then()
                .statusCode(204);

        // Assert: Verify it's gone
        given()
                .contentType("application/json")
                .when()
                .get("/api/v1/greetings/{id}", created.getId())
                .then()
                .statusCode(404);
    }

    @Test
    void returns404WhenDeletingNonExistentGreeting() {
        given()
                .when()
                .delete("/api/v1/greetings/{id}", 999999999L)
                .then()
                .statusCode(404);
    }
}
