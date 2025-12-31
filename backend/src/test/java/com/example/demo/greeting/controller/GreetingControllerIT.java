package com.example.demo.greeting.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.example.demo.greeting.service.GreetingService;
import com.example.demo.testsupport.AbstractRestAssuredIntegrationTest;
import com.example.demo.testsupport.DatabaseCleanupHelper;

import static com.example.demo.contract.OpenApiValidator.validationFilter;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

/**
 * API-level integration tests for Greeting HTTP endpoints (REST CRUD operations).
 *
 * <p><strong>Test Scope:</strong></p>
 * <ul>
 *   <li>Tests real Spring Boot application server (random port via Testcontainers)</li>
 *   <li>Tests real PostgreSQL database (via Testcontainers singleton)</li>
 *   <li>Tests full HTTP request/response cycle via RestAssured</li>
 *   <li>Uses "test" profile for simplified security configuration</li>
 * </ul>
 *
 * <p><strong>Test Isolation Strategy:</strong></p>
 * <ul>
 *   <li>@SpringBootTest(webEnvironment = RANDOM_PORT) - Real application instance</li>
 *   <li>@ResourceLock(value = "DB", mode = READ_WRITE) - Exclusive database access</li>
 *   <li>@BeforeEach cleanup - Prepares clean database state before test</li>
 *   <li>NO @AfterEach cleanup - REST tests don't have transaction scope across HTTP calls</li>
 * </ul>
 *
 * <p><strong>Why NO @Transactional and NO @AfterEach:</strong></p>
 * <ul>
 *   <li>REST controller tests use HTTP requests which execute in separate server threads</li>
 *   <li>@Transactional only rolls back changes in the test thread, not HTTP handler threads</li>
 *   <li>Each HTTP request commits data directly to database (outside test transaction)</li>
 *   <li>@BeforeEach is sufficient: cleans state before test starts</li>
 *   <li>@AfterEach cleanup would be redundant (data already committed by HTTP layer)</li>
 * </ul>
 *
 * <p><strong>Contract Validation:</strong></p>
 * <ul>
 *   <li>Uses {@link #validationFilter()} to validate responses against OpenAPI spec</li>
 *   <li>Ensures API responses conform to contract (important for client compatibility)</li>
 * </ul>
 *
 * @see com.example.demo.contract.OpenApiValidator#validationFilter()
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "integration"})
@ResourceLock(value = "DB", mode = READ_WRITE)
class GreetingControllerIT extends AbstractRestAssuredIntegrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    GreetingService greetingService;

    @Autowired
    DatabaseCleanupHelper cleanupHelper;

    /**
     * Prepare database before test by cleaning the greeting table.
     *
     * <p><strong>Purpose:</strong> Ensures each test starts with clean, isolated data.
     * All previous test data is removed before test execution begins.</p>
     *
     * <p><strong>Why @BeforeEach is Sufficient for REST Tests:</strong></p>
     * <ul>
     *   <li>REST controller tests use HTTP requests with separate server threads</li>
     *   <li>Each HTTP request commits directly to database (outside test transaction scope)</li>
     *   <li>Data created by test A is permanent in database</li>
     *   <li>Test B's @BeforeEach cleanup removes all of Test A's data</li>
     *   <li>@AfterEach would be redundant (data already committed by HTTP layer)</li>
     * </ul>
     *
     * <p><strong>Why NOT @AfterEach for this test:</strong></p>
     * Unlike async event tests (BusinessActivityIT) which have pending background processing,
     * REST controller tests have no async work that leaks to next test. The committed data
     * is cleaned at the START of each test via @BeforeEach.
     */
    @BeforeEach
    void cleanupDatabase() {
        cleanupHelper.truncateTables("greeting");
    }

    /**
     * INTENTIONALLY NOT IMPLEMENTED FOR REST CONTROLLER TESTS
     *
     * <p><strong>Why we don't use @AfterEach for this test class:</strong></p>
     * <ul>
     *   <li>REST tests don't have async work that needs cleanup after test completes</li>
     *   <li>Data committed by HTTP requests is permanent and cleaned at START of next test</li>
     *   <li>@BeforeEach cleanup is sufficient and more efficient (one cleanup per test)</li>
     *   <li>@AfterEach would add unnecessary cleanup overhead</li>
     * </ul>
     *
     * <p><strong>CONTRAST: BusinessActivityIT uses @AfterEach because:</strong></p>
     * <ul>
     *   <li>Spring Modulith event listeners run @Async in background thread pool</li>
     *   <li>Test completes but async event processing continues in background</li>
     *   <li>Pending events in event_publication table leak to next test without @AfterEach</li>
     *   <li>@AfterEach cleanup is CRITICAL to prevent flaky tests with async events</li>
     * </ul>
     *
     * <p>If you observe tests interfering with each other (failing only when run in parallel),
     * you can uncomment @AfterEach below as an extra safety measure.</p>
     */
    // @AfterEach
    // void cleanupAfterTest() {
    //     cleanupHelper.truncateTables("greeting");
    // }

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
                .body("reference", matchesPattern("^GRE-\\d{4}-\\d{6}$"))
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
                .body("data[0].reference", matchesPattern("^GRE-\\d{4}-\\d{6}$"))
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
                .body("reference", matchesPattern("^GRE-\\d{4}-\\d{6}$"))
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
                .statusCode(404)
                .contentType("application/problem+json")
                .body("type", equalTo("https://api.example.com/problems/greetings/resource-not-found"))
                .body("title", equalTo("Greeting Not Found"))
                .body("status", equalTo(404))
                .body("traceId", notNullValue());
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
                .body("type", equalTo("https://api.example.com/problems/greetings/resource-not-found"))
                .body("title", equalTo("Greeting Not Found"))
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

    // ============================================================
    // Validation Error Tests (RFC 7807 Problem Detail format)
    // ============================================================

    @Test
    void returns400WhenCreatingGreetingWithMissingRequiredField() {
        // Missing 'message' field which is required
        given()
                .filter(validationFilter())
                .contentType("application/json")
                .body("""
                       {"recipient": "Test"}
                       """)
                .when()
                .post("/api/v1/greetings")
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("type", notNullValue())
                .body("title", notNullValue())
                .body("status", equalTo(400));
    }
}
