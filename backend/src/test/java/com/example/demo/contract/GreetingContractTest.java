package com.example.demo.contract;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static com.example.demo.contract.OpenApiValidator.validationFilter;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Contract tests that validate API responses match the OpenAPI specification.
 *
 * <p>These tests ensure the backend implementation stays aligned with the contract
 * defined in {@code api/specification/openapi.yaml}. Every test uses the OpenAPI
 * validator filter which automatically validates:
 * <ul>
 *   <li>Request paths and methods exist in the spec</li>
 *   <li>Request bodies conform to schemas</li>
 *   <li>Response status codes are defined</li>
 *   <li>Response bodies match schemas (types, required fields, patterns)</li>
 *   <li>Response headers match specifications</li>
 * </ul>
 *
 * <p><strong>Purpose:</strong> These tests catch contract drift between:
 * <ul>
 *   <li>Backend implementation and OpenAPI spec</li>
 *   <li>Frontend expectations (based on generated SDK) and actual API behavior</li>
 * </ul>
 *
 * <p><strong>Why not just integration tests?</strong>
 * Traditional integration tests validate business logic but may miss schema violations like:
 * <ul>
 *   <li>Missing required fields in responses</li>
 *   <li>Incorrect field types (string instead of int)</li>
 *   <li>Extra fields not in the spec</li>
 *   <li>Pattern violations (e.g., reference format)</li>
 * </ul>
 *
 * @see OpenApiValidator
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GreetingContractTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    /**
     * Validates the complete CRUD workflow matches the OpenAPI contract.
     *
     * <p>This test exercises all greeting endpoints in sequence:
     * <ol>
     *   <li>GET /v1/greetings - List (initial state)</li>
     *   <li>POST /v1/greetings - Create new greeting</li>
     *   <li>GET /v1/greetings/{id} - Retrieve created greeting</li>
     *   <li>PUT /v1/greetings/{id} - Full update</li>
     *   <li>PATCH /v1/greetings/{id} - Partial update</li>
     *   <li>DELETE /v1/greetings/{id} - Delete greeting</li>
     *   <li>GET /v1/greetings/{id} - Verify deletion (404)</li>
     * </ol>
     *
     * <p>Each step is validated against the OpenAPI schema.
     */
    @Test
    void allGreetingEndpoints_matchOpenApiContract() {
        // Step 1: List greetings (initial state)
        given()
                .filter(validationFilter())
                .when()
                .get("/api/v1/greetings")
                .then()
                .statusCode(200)
                .body("data", is(notNullValue()))
                .body("meta", is(notNullValue()))
                .body("meta.pageNumber", is(notNullValue()))
                .body("meta.pageSize", is(notNullValue()))
                .body("meta.totalElements", is(notNullValue()))
                .body("meta.totalPages", is(notNullValue()));

        // Step 2: Create a new greeting
        String createRequestBody = """
                {
                    "message": "Contract test greeting",
                    "recipient": "Contract"
                }
                """;

        var response = given()
                .filter(validationFilter())
                .contentType(ContentType.JSON)
                .body(createRequestBody)
                .when()
                .post("/api/v1/greetings")
                .then()
                .log().all()  // Log the full response for debugging
                .statusCode(201)
                .body("id", is(notNullValue()))
                .body("reference", matchesRegex("^GRE-\\d{4}-\\d{6}$"))
                .body("message", equalTo("Contract test greeting"))
                .body("recipient", equalTo("Contract"))
                .body("createdAt", is(notNullValue()))
                .extract();

        String location = response.header("Location");
        if (location == null || location.isEmpty()) {
            // Fallback: construct location from response body
            var id = response.path("id").toString();
            location = "/api/v1/greetings/" + id;
        }

        // Extract ID from Location header
        String id = location.substring(location.lastIndexOf("/") + 1);

        // Step 3: Retrieve the created greeting
        given()
                .filter(validationFilter())
                .when()
                .get("/api/v1/greetings/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id)) // ID is now a string to preserve JS precision
                .body("reference", matchesRegex("^GRE-\\d{4}-\\d{6}$"))
                .body("message", equalTo("Contract test greeting"))
                .body("recipient", equalTo("Contract"))
                .body("createdAt", is(notNullValue()));

        // Step 4: Full update (PUT) - all fields required
        String updateRequestBody = """
                {
                    "message": "Updated contract test message",
                    "recipient": "Updated recipient"
                }
                """;

        given()
                .filter(validationFilter())
                .contentType(ContentType.JSON)
                .body(updateRequestBody)
                .when()
                .put("/api/v1/greetings/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id)) // ID is now a string
                .body("message", equalTo("Updated contract test message"))
                .body("recipient", equalTo("Updated recipient"));

        // Step 5: Partial update (PATCH) - only message field
        String patchRequestBody = """
                {
                    "message": "Patched message only"
                }
                """;

        given()
                .filter(validationFilter())
                .contentType(ContentType.JSON)
                .body(patchRequestBody)
                .when()
                .patch("/api/v1/greetings/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id)) // ID is now a string
                .body("message", equalTo("Patched message only"))
                .body("recipient", equalTo("Updated recipient")); // Unchanged from PUT

        // Step 6: Delete the greeting
        given()
                .filter(validationFilter())
                .when()
                .delete("/api/v1/greetings/" + id)
                .then()
                .statusCode(204);

        // Step 7: Verify deletion - should return 404
        given()
                .filter(validationFilter())
                .when()
                .get("/api/v1/greetings/" + id)
                .then()
                .statusCode(404);
    }

    /**
     * Validates pagination parameters match the OpenAPI contract.
     *
     * <p>Tests:
     * <ul>
     *   <li>Default pagination (no parameters)</li>
     *   <li>Custom page and size parameters</li>
     *   <li>Response meta fields match schema</li>
     * </ul>
     */
    @Test
    void listGreetings_withPagination_matchesContract() {
        // Default pagination
        given()
                .filter(validationFilter())
                .when()
                .get("/api/v1/greetings")
                .then()
                .statusCode(200)
                .body("meta.pageNumber", is(notNullValue()))
                .body("meta.pageSize", is(notNullValue()));

        // Custom pagination
        given()
                .filter(validationFilter())
                .queryParam("page", 0)
                .queryParam("size", 5)
                .when()
                .get("/api/v1/greetings")
                .then()
                .statusCode(200)
                .body("meta.pageNumber", equalTo(0))
                .body("meta.pageSize", lessThanOrEqualTo(5));
    }

    /**
     * Validates error responses match the RFC 7807 Problem Detail schema.
     *
     * <p>Tests error scenarios:
     * <ul>
     *   <li>404 Not Found - non-existent resource</li>
     *   <li>400 Bad Request - invalid request body</li>
     * </ul>
     */
    @Test
    void errorResponses_matchProblemDetailContract() {
        // Test 404 - Not Found
        // Note: 404 may not have response body due to Spring configuration
        given()
                .filter(validationFilter())
                .when()
                .get("/api/v1/greetings/999999999")
                .then()
                .statusCode(404);

        // Test 400 - Validation Error (missing required field)
        String invalidRequestBody = """
                {
                    "recipient": "Test"
                }
                """;

        given()
                .filter(validationFilter())
                .contentType(ContentType.JSON)
                .body(invalidRequestBody)
                .when()
                .post("/api/v1/greetings")
                .then()
                .statusCode(400)
                .body("type", is(notNullValue()))
                .body("title", is(notNullValue()))
                .body("status", equalTo(400));
    }

    /**
     * Validates that required fields match the schema.
     *
     * <p>This test catches regressions where:
     * <ul>
     *   <li>Required fields are accidentally made optional</li>
     *   <li>New required fields are added to the spec but not implemented</li>
     *   <li>Field names are changed without updating the spec</li>
     * </ul>
     */
    @Test
    void createdGreeting_containsAllRequiredFields() {
        String requestBody = """
                {
                    "message": "Testing required fields",
                    "recipient": "Test Recipient"
                }
                """;

        given()
                .filter(validationFilter())
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/greetings")
                .then()
                .statusCode(201)
                // Schema validation ensures all required fields are present:
                // id, reference, message, createdAt (from GreetingResponse schema)
                .body("id", is(notNullValue()))
                .body("reference", is(notNullValue()))
                .body("message", is(notNullValue()))
                .body("createdAt", is(notNullValue()));
    }
}
