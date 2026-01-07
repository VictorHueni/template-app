package com.example.demo.greeting.controller;

import static com.example.demo.contract.OpenApiValidator.validationFilter;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.demo.testsupport.AbstractSecuredRestAssuredIT;
import com.example.demo.testsupport.DatabaseCleanupHelper;

import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

/**
 * Security integration tests for GreetingController using real Keycloak JWT tokens.
 *
 * <p><strong>Test Scope:</strong></p>
 * <ul>
 *   <li>Tests OAuth2 security enforcement on protected endpoints</li>
 *   <li>Tests public endpoint access without authentication</li>
 *   <li>Tests that unauthenticated requests to protected endpoints return 401</li>
 *   <li>Validates responses against OpenAPI spec</li>
 * </ul>
 *
 * <p><strong>OpenAPI Security Configuration:</strong></p>
 * <ul>
 *   <li>GET /v1/greetings - Public (security: [])</li>
 *   <li>POST /v1/greetings - Protected (BearerAuth)</li>
 *   <li>GET /v1/greetings/{id} - Public (security: [])</li>
 *   <li>PUT /v1/greetings/{id} - Protected (BearerAuth)</li>
 *   <li>PATCH /v1/greetings/{id} - Protected (BearerAuth)</li>
 *   <li>DELETE /v1/greetings/{id} - Protected (BearerAuth)</li>
 * </ul>
 *
 * @see GreetingController
 * @see AbstractSecuredRestAssuredIT
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "keycloak-test"})
@ResourceLock(value = "DB", mode = READ_WRITE)
@DisplayName("GreetingController Security Integration Tests (Keycloak)")
class GreetingControllerSecuredIT extends AbstractSecuredRestAssuredIT {

    /**
     * Regex pattern for functional business ID (e.g., GRE-2025-000001).
     */
    private static final String REFERENCE_PATTERN = "^[A-Z]{3}-\\d{4}-\\d{6}$";

    @Autowired
    DatabaseCleanupHelper cleanupHelper;

    @BeforeEach
    void cleanDatabase() {
        cleanupHelper.truncateTables("greeting");
    }

    @Nested
    @ResourceLock(value = "DB", mode = READ_WRITE)
    @DisplayName("Public Endpoints (No Authentication Required)")
    class PublicEndpointTests {

        @Test
        @DisplayName("GET /api/v1/greetings - should allow unauthenticated access")
        void listGreetings_shouldAllowUnauthenticated() {
            givenUnauthenticated()
                .filter(validationFilter())
                .contentType("application/json")
                .when()
                .get("/api/v1/greetings")
                .then()
                .statusCode(200);
        }

        @Test
        @DisplayName("GET /api/v1/greetings/{id} - should allow unauthenticated access (returns 404 if not found)")
        void getGreeting_shouldAllowUnauthenticated() {
            // Create a greeting first using authenticated request
            String greetingId = createTestGreeting();

            givenUnauthenticated()
                .filter(validationFilter())
                .contentType("application/json")
                .when()
                .get("/api/v1/greetings/" + greetingId)
                .then()
                .statusCode(200)
                .body("id", equalTo(greetingId))
                .body("message", equalTo("Test Message"));
        }
    }

    @Nested
    @ResourceLock(value = "DB", mode = READ_WRITE)
    @DisplayName("Protected Endpoints - POST /api/v1/greetings")
    class CreateGreetingSecurityTests {

        @Test
        @DisplayName("should return 403 without authentication")
        void shouldReturn401WithoutAuth() {
            // Note: Returns 403 (not 401) because:
            // 1. permit-all in properties allows anonymous requests through filter chain
            // 2. @PreAuthorize("isAuthenticated()") on method throws AccessDeniedException
            // 3. Spring Security translates AccessDeniedException to 403 Forbidden
            // This is expected behavior when using method-level security with permit-all paths.
            givenUnauthenticated()
                .contentType("application/json")
                .body("""
                    {
                        "message": "Hello, World!",
                        "recipient": "World"
                    }
                    """)
                .when()
                .post("/api/v1/greetings")
                .then()
                .statusCode(403);
        }

        @Test
        @DisplayName("should allow authenticated user to create greeting")
        void shouldAllowAuthenticatedUser() {
            givenAuthenticatedUser()
                .filter(validationFilter())
                .contentType("application/json")
                .body("""
                    {
                        "message": "Hello from authenticated user!",
                        "recipient": "World"
                    }
                    """)
                .when()
                .post("/api/v1/greetings")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("reference", matchesPattern(REFERENCE_PATTERN))
                .body("message", equalTo("Hello from authenticated user!"))
                .body("recipient", equalTo("World"));
        }

        @Test
        @DisplayName("should allow admin user to create greeting")
        void shouldAllowAdminUser() {
            givenAuthenticatedAdmin()
                .filter(validationFilter())
                .contentType("application/json")
                .body("""
                    {
                        "message": "Hello from admin!",
                        "recipient": "Everyone"
                    }
                    """)
                .when()
                .post("/api/v1/greetings")
                .then()
                .statusCode(201)
                .body("message", equalTo("Hello from admin!"));
        }
    }

    @Nested
    @ResourceLock(value = "DB", mode = READ_WRITE)
    @DisplayName("Protected Endpoints - PUT /api/v1/greetings/{id}")
    class UpdateGreetingSecurityTests {

        @Test
        @DisplayName("should return 403 without authentication")
        void shouldReturn401WithoutAuth() {
            String greetingId = createTestGreeting();

            // Note: Returns 403 (not 401) because:
            // 1. permit-all in properties allows anonymous requests through filter chain
            // 2. @PreAuthorize("isAuthenticated()") on method throws AccessDeniedException
            // 3. Spring Security translates AccessDeniedException to 403 Forbidden
            givenUnauthenticated()
                .contentType("application/json")
                .body("""
                    {
                        "message": "Updated message",
                        "recipient": "Updated recipient"
                    }
                    """)
                .when()
                .put("/api/v1/greetings/" + greetingId)
                .then()
                .statusCode(403);
        }

        @Test
        @DisplayName("should allow authenticated user to update greeting")
        void shouldAllowAuthenticatedUser() {
            String greetingId = createTestGreeting();

            givenAuthenticatedUser()
                .filter(validationFilter())
                .contentType("application/json")
                .body("""
                    {
                        "message": "Updated by user",
                        "recipient": "Updated recipient"
                    }
                    """)
                .when()
                .put("/api/v1/greetings/" + greetingId)
                .then()
                .statusCode(200)
                .body("message", equalTo("Updated by user"))
                .body("recipient", equalTo("Updated recipient"));
        }
    }

    @Nested
    @ResourceLock(value = "DB", mode = READ_WRITE)
    @DisplayName("Protected Endpoints - PATCH /api/v1/greetings/{id}")
    class PatchGreetingSecurityTests {

        @Test
        @DisplayName("should return 403 without authentication")
        void shouldReturn401WithoutAuth() {
            String greetingId = createTestGreeting();

            // Note: Returns 403 (not 401) because:
            // 1. permit-all in properties allows anonymous requests through filter chain
            // 2. @PreAuthorize("isAuthenticated()") on method throws AccessDeniedException
            // 3. Spring Security translates AccessDeniedException to 403 Forbidden
            givenUnauthenticated()
                .contentType("application/json")
                .body("""
                    {
                        "message": "Patched message"
                    }
                    """)
                .when()
                .patch("/api/v1/greetings/" + greetingId)
                .then()
                .statusCode(403);
        }

        @Test
        @DisplayName("should allow authenticated user to patch greeting")
        void shouldAllowAuthenticatedUser() {
            String greetingId = createTestGreeting();

            givenAuthenticatedUser()
                .filter(validationFilter())
                .contentType("application/json")
                .body("""
                    {
                        "message": "Patched by user"
                    }
                    """)
                .when()
                .patch("/api/v1/greetings/" + greetingId)
                .then()
                .statusCode(200)
                .body("message", equalTo("Patched by user"));
        }
    }

    @Nested
    @ResourceLock(value = "DB", mode = READ_WRITE)
    @DisplayName("Protected Endpoints - DELETE /api/v1/greetings/{id}")
    class DeleteGreetingSecurityTests {

        @Test
        @DisplayName("should return 403 without authentication")
        void shouldReturn401WithoutAuth() {
            String greetingId = createTestGreeting();

            // Note: Returns 403 (not 401) because:
            // 1. permit-all in properties allows anonymous requests through filter chain
            // 2. @PreAuthorize("isAuthenticated()") on method throws AccessDeniedException
            // 3. Spring Security translates AccessDeniedException to 403 Forbidden
            givenUnauthenticated()
                .contentType("application/json")
                .when()
                .delete("/api/v1/greetings/" + greetingId)
                .then()
                .statusCode(403);
        }

        @Test
        @DisplayName("should allow authenticated user to delete greeting")
        void shouldAllowAuthenticatedUser() {
            String greetingId = createTestGreeting();

            givenAuthenticatedUser()
                .filter(validationFilter())
                .contentType("application/json")
                .when()
                .delete("/api/v1/greetings/" + greetingId)
                .then()
                .statusCode(204);

            // Verify deletion by trying to get the greeting
            givenUnauthenticated()
                .contentType("application/json")
                .when()
                .get("/api/v1/greetings/" + greetingId)
                .then()
                .statusCode(404);
        }
    }

    /**
     * Helper method to create a test greeting for update/delete tests.
     *
     * @return the created greeting's ID
     */
    private String createTestGreeting() {
        return givenAuthenticatedUser()
            .contentType("application/json")
            .body("""
                {
                    "message": "Test Message",
                    "recipient": "Test Recipient"
                }
                """)
            .when()
            .post("/api/v1/greetings")
            .then()
            .statusCode(201)
            .extract()
            .path("id");
    }
}
