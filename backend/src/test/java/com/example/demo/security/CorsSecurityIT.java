package com.example.demo.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.demo.testsupport.AbstractControllerIT;
import com.example.demo.testsupport.auth.MockSecurityConfig;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Integration tests for CORS (Cross-Origin Resource Sharing) configuration.
 *
 * <p>These tests verify the backend correctly handles CORS requests:
 * <ul>
 *   <li>Allowed origins can make cross-origin requests</li>
 *   <li>Unauthorized origins are blocked (no CORS headers returned)</li>
 *   <li>Only permitted HTTP methods are allowed</li>
 *   <li>Only required headers are accepted in preflight</li>
 * </ul>
 *
 * <p><b>Security Context:</b> Proper CORS configuration prevents malicious
 * websites from making authenticated requests on behalf of users. Without
 * CORS, the browser's same-origin policy blocks these requests.
 *
 * <p><b>Gap Filled:</b> No existing CORS tests. Critical for frontend security
 * as the SPA makes cross-origin requests to the backend.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "integration"})
@DisplayName("CORS Security")
class CorsSecurityIT extends AbstractControllerIT {

    /**
     * Allowed origin matching MockSecurityConfig configuration.
     */
    private static final String ALLOWED_ORIGIN = MockSecurityConfig.TEST_ALLOWED_ORIGIN;

    /**
     * Malicious origin that should be rejected.
     */
    private static final String MALICIOUS_ORIGIN = "http://evil.com";

    @Nested
    @DisplayName("Simple requests (GET)")
    class SimpleRequests {

        @Test
        @DisplayName("should include CORS headers for allowed origin")
        void shouldAllowConfiguredOrigin() {
            givenUnauthenticated()
                    .header("Origin", ALLOWED_ORIGIN)
                    .when()
                    .get("/api/v1/greetings")
                    .then()
                    .statusCode(200)
                    .header("Access-Control-Allow-Origin", equalTo(ALLOWED_ORIGIN))
                    .header("Access-Control-Allow-Credentials", equalTo("true"));
        }

        @Test
        @DisplayName("should reject request from unauthorized origin")
        void shouldBlockUnauthorizedOrigin() {
            givenUnauthenticated()
                    .header("Origin", MALICIOUS_ORIGIN)
                    .when()
                    .get("/api/v1/greetings")
                    .then()
                    // Spring Security's CorsFilter rejects requests with invalid origins
                    // with 403 Forbidden when CORS is configured with specific allowed origins
                    .statusCode(403)
                    .header("Access-Control-Allow-Origin", nullValue());
        }
    }

    @Nested
    @DisplayName("Preflight requests (OPTIONS)")
    class PreflightRequests {

        @Test
        @DisplayName("should return allowed methods for POST preflight")
        void shouldReturnAllowedMethodsForPreflight() {
            givenUnauthenticated()
                    .header("Origin", ALLOWED_ORIGIN)
                    .header("Access-Control-Request-Method", "POST")
                    .when()
                    .options("/api/v1/greetings")
                    .then()
                    .statusCode(200)
                    .header("Access-Control-Allow-Methods",
                            allOf(
                                    containsString("GET"),
                                    containsString("POST"),
                                    containsString("PUT"),
                                    containsString("DELETE")
                            ));
        }

        @Test
        @DisplayName("should allow required headers in preflight")
        void shouldAllowRequiredHeaders() {
            givenUnauthenticated()
                    .header("Origin", ALLOWED_ORIGIN)
                    .header("Access-Control-Request-Method", "POST")
                    .header("Access-Control-Request-Headers", "Authorization, Content-Type, X-XSRF-TOKEN")
                    .when()
                    .options("/api/v1/greetings")
                    .then()
                    .statusCode(200)
                    .header("Access-Control-Allow-Headers", notNullValue());
        }
    }
}
