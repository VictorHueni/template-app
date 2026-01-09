package com.example.demo.user.controller;

import static com.example.demo.contract.OpenApiValidator.validationFilter;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.demo.testsupport.AbstractSecuredRestAssuredIT;

/**
 * Security integration tests for UserController using locally minted JWT tokens.
 *
 * <p><strong>Test Scope:</strong></p>
 * <ul>
 *   <li>Tests OAuth2 Resource Server behavior (JWT validation + claim mapping)</li>
 *   <li>Tests full HTTP request/response cycle via RestAssured</li>
 *   <li>Tests JWT token validation and claim extraction</li>
 *   <li>Validates responses against OpenAPI spec</li>
 * </ul>
 *
 * <p><strong>Why locally minted tokens?</strong></p>
 * <p>
 * Spring's {@code @WithJwt} annotation sets SecurityContext in ThreadLocal,
 * which doesn't propagate to server threads during RestAssured HTTP calls.
 * This test uses locally minted JWTs (signed with a fixed HS256 secret) to emulate
 * the Gateway relaying a JWT to the backend.
 * </p>
 *
 * <p><strong>Test Users:</strong></p>
 * <ul>
 *   <li>testuser / test123 - USER role</li>
 *   <li>adminuser / admin123 - USER + ADMIN roles</li>
 * </ul>
 *
 * @see UserController
 * @see AbstractSecuredRestAssuredIT
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test"})
@DisplayName("UserController Security Integration Tests (Mock JWT)")
class UserControllerSecuredIT extends AbstractSecuredRestAssuredIT {

    @Nested
    @DisplayName("GET /api/v1/me - Authentication Tests")
    class AuthenticationTests {

        // Note: 401 responses from Spring Security use application/json by default,
        // not application/problem+json as defined in OpenAPI spec.
        // OpenAPI validation is skipped for security-layer error responses.

        @Test
        @DisplayName("should return 401 Unauthorized without token")
        void shouldReturn401WithoutToken() {
            givenUnauthenticated()
                // Don't validate OpenAPI - Spring Security returns application/json for 401
                .contentType("application/json")
                .when()
                .get("/api/v1/me")
                .then()
                .statusCode(401);
        }

        @Test
        @DisplayName("should return 401 with invalid token")
        void shouldReturn401WithInvalidToken() {
            given()
                // Don't validate OpenAPI - Spring Security returns application/json for 401
                .header("Authorization", "Bearer invalid.jwt.token")
                .contentType("application/json")
                .when()
                .get("/api/v1/me")
                .then()
                .statusCode(401);
        }

        @Test
        @DisplayName("should return 401 with malformed Authorization header")
        void shouldReturn401WithMalformedHeader() {
            given()
                // Don't validate OpenAPI - Spring Security returns application/json for 401
                .header("Authorization", "NotBearer " + getUserToken())
                .contentType("application/json")
                .when()
                .get("/api/v1/me")
                .then()
                .statusCode(401);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/me - User Info Response Tests")
    class UserInfoTests {

        @Test
        @DisplayName("should return user info for authenticated user with USER role")
        void shouldReturnUserInfoForAuthenticatedUser() {
            givenAuthenticatedUser()
                .filter(validationFilter())
                .contentType("application/json")
                .when()
                .get("/api/v1/me")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("username", equalTo("testuser"))
                .body("email", equalTo("test@example.com"))
                .body("roles", hasSize(1))
                .body("roles[0]", equalTo("USER"));
        }

        @Test
        @DisplayName("should return user info with admin roles for admin user")
        void shouldReturnUserInfoWithAdminRoles() {
            givenAuthenticatedAdmin()
                .filter(validationFilter())
                .contentType("application/json")
                .when()
                .get("/api/v1/me")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("username", equalTo("adminuser"))
                .body("email", equalTo("admin@example.com"))
                .body("roles", hasSize(2))
                .body("roles", containsInAnyOrder("USER", "ADMIN"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/me - OpenAPI Contract Tests")
    class ContractTests {

        @Test
        @DisplayName("should return response conforming to OpenAPI spec")
        void shouldReturnValidOpenApiResponse() {
            givenAuthenticatedUser()
                .filter(validationFilter())
                .accept("application/json")
                .when()
                .get("/api/v1/me")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("username", notNullValue())
                .body("roles", notNullValue());
        }
    }
}
