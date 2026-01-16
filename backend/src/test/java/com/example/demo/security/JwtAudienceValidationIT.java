package com.example.demo.security;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.demo.testsupport.AbstractControllerIT;
import com.example.demo.testsupport.auth.TestJwtUtils;

import static io.restassured.RestAssured.given;

/**
 * Integration tests for JWT audience (aud) claim validation.
 *
 * <p>These tests verify the Resource Server rejects JWTs with invalid
 * audience claims, preventing cross-client token reuse attacks.
 *
 * <p><b>Security Context:</b> Without audience validation, an attacker with
 * a valid JWT from a different Keycloak client (e.g., billing-service) could
 * use it to access this API. The {@code aud} claim ensures tokens are only
 * accepted when intended for this specific resource server.
 *
 * <p><b>Gap Filled:</b> Existing tests (UserControllerIT, GreetingControllerIT)
 * verify authentication presence but NOT audience validation. A token could be
 * valid for another Keycloak client and incorrectly accepted without this check.
 *
 * <p>Uses AbstractControllerIT for RestAssured setup and schema isolation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "integration"})
@DisplayName("JWT Audience Validation")
class JwtAudienceValidationIT extends AbstractControllerIT {

    @Nested
    @DisplayName("When audience claim is configured")
    class WhenAudienceConfigured {

        @Test
        @DisplayName("should accept JWT with correct audience")
        void shouldAcceptJwtWithCorrectAudience() {
            // Given: JWT with aud=["template-gateway"] (matches config)
            String token = TestJwtUtils.createTokenWithAudience(
                    "testuser",
                    List.of("USER"),
                    List.of("template-gateway")
            );

            // When/Then: Request succeeds
            given()
                    .header("Authorization", "Bearer " + token)
                    .contentType("application/json")
                    .when()
                    .get("/api/v1/me")
                    .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("should reject JWT with wrong audience (cross-client attack)")
        void shouldRejectJwtWithWrongAudience() {
            // Given: Valid JWT but for different client (billing-service)
            // This simulates a cross-client token reuse attack
            String token = TestJwtUtils.createTokenWithAudience(
                    "attacker",
                    List.of("USER"),
                    List.of("billing-service")
            );

            // When/Then: Request rejected with 401 - token not intended for us
            given()
                    .header("Authorization", "Bearer " + token)
                    .contentType("application/json")
                    .when()
                    .get("/api/v1/me")
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("should reject JWT with missing audience")
        void shouldRejectJwtWithMissingAudience() {
            // Given: JWT without any audience claim
            String token = TestJwtUtils.createTokenWithoutAudience(
                    "user",
                    List.of("USER")
            );

            // When/Then: Request rejected with 401
            given()
                    .header("Authorization", "Bearer " + token)
                    .contentType("application/json")
                    .when()
                    .get("/api/v1/me")
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("should accept JWT with multiple audiences including ours")
        void shouldAcceptJwtWithMultipleAudiencesIncludingOurs() {
            // Given: JWT with multiple audiences (common in multi-service scenarios)
            // Our expected audience is included in the list
            String token = TestJwtUtils.createTokenWithAudience(
                    "testuser",
                    List.of("USER"),
                    List.of("template-gateway", "audit-service")
            );

            // When/Then: Request succeeds (our audience is in the list)
            given()
                    .header("Authorization", "Bearer " + token)
                    .contentType("application/json")
                    .when()
                    .get("/api/v1/me")
                    .then()
                    .statusCode(200);
        }
    }
}
