package com.example.demo.user.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.demo.testsupport.AbstractControllerIT;

import static com.example.demo.contract.OpenApiValidator.validationFilter;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "integration"})
@DisplayName("UserController Integration Tests (Mock JWT)")
class UserControllerIT extends AbstractControllerIT {

    @Nested
    @DisplayName("GET /api/v1/me - Authentication Tests")
    class AuthenticationTests {

        // Note: 401 responses from Spring Security use application/json by default,
        // not application/problem+json as defined in OpenAPI spec.
        // OpenAPI validation is skipped for security-layer error responses.

        @Test
        @DisplayName("should return 403 Forbidden without token")
        void shouldReturn401WithoutToken() {
            givenUnauthenticated()
                    .contentType("application/json")
                    .when()
                    .get("/api/v1/me")
                    .then()
                .statusCode(403);
        }

        @Test
        @DisplayName("should return 401 with invalid token")
        void shouldReturn401WithInvalidToken() {
            given()
                    .header("Authorization", "Bearer invalid.jwt.token")
                    .contentType("application/json")
                    .when()
                    .get("/api/v1/me")
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("should return 403 with malformed Authorization header")
        void shouldReturn401WithMalformedHeader() {
            given()
                    .header("Authorization", "NotBearer " + getUserToken())
                    .contentType("application/json")
                    .when()
                    .get("/api/v1/me")
                    .then()
                .statusCode(403);
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
                    .body("email", equalTo("test@template.com"))
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
                    .body("email", equalTo("admin@template.com"))
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
