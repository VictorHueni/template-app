package com.example.demo.greeting.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;

import com.example.demo.greeting.service.GreetingService;
import com.example.demo.testsupport.AbstractRestAssuredIntegrationTest;

import static com.example.demo.contract.OpenApiValidator.validationFilter;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

/**
 * API-level integration tests for Greeting HTTP endpoints.
 * Hits the real Spring Boot app and real PostgreSQL (via Testcontainers singleton).
 * Uses the "test" profile for simplified security configuration.
 *
 * IMPORTANT: This test class uses @Execution(ExecutionMode.SAME_THREAD) to ensure
 * it runs in isolation from other parallel tests. This prevents transaction
 * management conflicts when running alongside @Transactional repository/service tests.
 *
 * REST controller tests should NOT use @Transactional because the HTTP requests
 * execute in separate threads from the test thread, making transaction rollback ineffective.
 * Instead, we clean up test data manually in @BeforeEach.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Execution(ExecutionMode.SAME_THREAD)
@TestExecutionListeners(
    listeners = {},
    mergeMode = MERGE_WITH_DEFAULTS
)
class GreetingControllerIT extends AbstractRestAssuredIntegrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    GreetingService greetingService;

    /**
     * Clean up database before each test to ensure test isolation.
     * REST controller tests cannot use @Transactional rollback because HTTP requests
     * run in separate threads, so we manually clean up data instead.
     */
    @BeforeEach
    void cleanupDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE greeting CASCADE");
    }

    @Test
    void createsGreetingAndReturnsContract() {
        given()
                .filter(validationFilter())
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
                .filter(validationFilter())
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
                .filter(validationFilter())
                .contentType("application/json")
                .when()
                .get("/api/v1/greetings/{id}", created.getId())
                .then()
                .statusCode(200)
                .body("id", equalTo(String.valueOf(created.getId())))
                .body("reference", equalTo(created.getReference()))
                .body("message", equalTo("Hello GET"))
                .body("recipient", equalTo("GetTest"))
                .body("createdAt", notNullValue());
    }

    @Test
    void returns404WhenGreetingNotFound() {
        given()
                .filter(validationFilter())
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
                .filter(validationFilter())
                .contentType("application/json")
                .body("""
                       {"message": "Updated Message", "recipient": "Updated"}
                       """)
                .when()
                .put("/api/v1/greetings/{id}", created.getId())
                .then()
                .statusCode(200)
                .body("id", equalTo(String.valueOf(created.getId())))
                .body("reference", equalTo(created.getReference())) // reference unchanged
                .body("message", equalTo("Updated Message"))
                .body("recipient", equalTo("Updated"));
    }

    @Test
    void returns404WhenUpdatingNonExistentGreeting() {
        given()
                .filter(validationFilter())
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
                .filter(validationFilter())
                .contentType("application/json")
                .body("""
                       {"message": "Patched Message"}
                       """)
                .when()
                .patch("/api/v1/greetings/{id}", created.getId())
                .then()
                .statusCode(200)
                .body("id", equalTo(String.valueOf(created.getId())))
                .body("message", equalTo("Patched Message"))
                .body("recipient", equalTo("OriginalRecipient")); // unchanged
    }

    @Test
    void patchesGreetingRecipientOnly() {
        // Arrange: Create a greeting
        var created = greetingService.createGreeting("Original Message", "OriginalRecipient");

        // Act & Assert: Patch only the recipient
        given()
                .filter(validationFilter())
                .contentType("application/json")
                .body("""
                       {"recipient": "PatchedRecipient"}
                       """)
                .when()
                .patch("/api/v1/greetings/{id}", created.getId())
                .then()
                .statusCode(200)
                .body("id", equalTo(String.valueOf(created.getId())))
                .body("message", equalTo("Original Message")) // unchanged
                .body("recipient", equalTo("PatchedRecipient"));
    }

    @Test
    void returns404WhenPatchingNonExistentGreeting() {
        given()
                .filter(validationFilter())
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
                .filter(validationFilter())
                .when()
                .delete("/api/v1/greetings/{id}", created.getId())
                .then()
                .statusCode(204);

        // Assert: Verify it's gone
        given()
                .filter(validationFilter())
                .contentType("application/json")
                .when()
                .get("/api/v1/greetings/{id}", created.getId())
                .then()
                .statusCode(404);
    }

    @Test
    void returns404WhenDeletingNonExistentGreeting() {
        given()
                .filter(validationFilter())
                .when()
                .delete("/api/v1/greetings/{id}", 999999999L)
                .then()
                .statusCode(404)
                .contentType("application/problem+json")
                .body("type", equalTo("https://api.example.com/problems/resource-not-found"))
                .body("title", equalTo("Resource Not Found"))
                .body("status", equalTo(404))
                .body("traceId", notNullValue());
    }

    // ============================================================
    // Regression test for JavaScript number precision issue
    // ============================================================

    /**
     * Verifies that greeting IDs are serialized as JSON strings, not numbers.
     *
     * This prevents JavaScript precision loss for large TSID values.
     * JavaScript's Number.MAX_SAFE_INTEGER is 2^53-1 (9007199254740991),
     * but TSIDs can exceed this, causing precision loss when parsed as numbers.
     *
     * Example of the bug this prevents:
     * - Backend returns: {"id": 785961326020039346}
     * - JavaScript parses as: 785961326020039300 (precision lost!)
     * - DELETE request fails with 404
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Number/MAX_SAFE_INTEGER">MDN: Number.MAX_SAFE_INTEGER</a>
     */
    @Test
    void idIsSerializedAsStringToPreserveJavaScriptPrecision() {
        // Arrange: Create a greeting (TSID will be a large 64-bit number)
        var created = greetingService.createGreeting("Precision Test", "Test");

        // Act & Assert: Verify ID is returned as a string in JSON
        given()
                .filter(validationFilter())
                .contentType("application/json")
                .when()
                .get("/api/v1/greetings/{id}", created.getId())
                .then()
                .statusCode(200)
                // The ID must be a string (quoted in JSON), not a raw number
                .body("id", isA(String.class))
                // The string value must match the actual ID exactly
                .body("id", equalTo(String.valueOf(created.getId())))
                // Verify it's a numeric string (digits only)
                .body("id", matchesPattern("^\\d+$"));
    }
}
