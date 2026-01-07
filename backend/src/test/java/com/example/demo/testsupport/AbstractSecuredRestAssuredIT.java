package com.example.demo.testsupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/**
 * Base class for secured REST API integration tests using RestAssured with real Keycloak JWT tokens.
 * <p>
 * This class extends {@link AbstractRestAssuredIntegrationTest} and adds:
 * <ul>
 *   <li>Keycloak Testcontainer integration for real JWT token validation</li>
 *   <li>Helper methods to obtain user and admin tokens</li>
 *   <li>Pre-configured request specifications with Authorization headers</li>
 *   <li>Dynamic property injection for Spring Security OAuth2 configuration</li>
 * </ul>
 *
 * <p><strong>Why Real Keycloak Tokens?</strong></p>
 * <p>
 * Spring Security's {@code @WithJwt} annotation sets SecurityContext in ThreadLocal,
 * which doesn't propagate to server threads during RestAssured HTTP calls. This base class
 * solves this by obtaining real JWT tokens from Keycloak and passing them via HTTP headers.
 * </p>
 *
 * <p><strong>Test Profiles:</strong></p>
 * <ul>
 *   <li>{@code test} - Base test profile</li>
 *   <li>{@code keycloak-test} - Enables real OAuth2 security (disables TestSecurityConfig)</li>
 * </ul>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * @SpringBootTest(webEnvironment = RANDOM_PORT)
 * @ActiveProfiles({"test", "keycloak-test"})
 * class MySecuredControllerIT extends AbstractSecuredRestAssuredIT {
 *
 *     @Test
 *     void shouldAllowAuthenticatedUser() {
 *         givenAuthenticatedUser()
 *             .when()
 *             .get("/api/v1/me")
 *             .then()
 *             .statusCode(200);
 *     }
 *
 *     @Test
 *     void shouldRejectUnauthenticated() {
 *         given()
 *             .when()
 *             .get("/api/v1/me")
 *             .then()
 *             .statusCode(401);
 *     }
 * }
 * }</pre>
 *
 * @see KeycloakTestcontainerConfiguration
 * @see KeycloakTokenProvider
 * @see AbstractRestAssuredIntegrationTest
 */
@Import(KeycloakTestcontainerConfiguration.class)
public abstract class AbstractSecuredRestAssuredIT extends AbstractIntegrationTest {

    @LocalServerPort
    protected int port;

    /**
     * Registers the Keycloak issuer URL for Spring Security OAuth2 Resource Server.
     * <p>
     * This dynamically injects the Keycloak container's issuer URL into Spring's
     * configuration, enabling real JWT validation against the test Keycloak instance.
     *
     * @param registry the dynamic property registry
     */
    @DynamicPropertySource
    static void keycloakProperties(DynamicPropertyRegistry registry) {
        registry.add("com.c4-soft.springaddons.oidc.ops[0].iss",
                KeycloakTestcontainerConfiguration::getIssuerUrl);
        // Must also set authorities path dynamically - properties file entries can be overridden
        // by array index when iss is set dynamically
        registry.add("com.c4-soft.springaddons.oidc.ops[0].authorities[0].path",
                () -> "$.roles");
        registry.add("com.c4-soft.springaddons.oidc.ops[0].username-claim",
                () -> "$.preferred_username");
    }

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @AfterEach
    void tearDownRestAssured() {
        RestAssured.reset();
    }

    /**
     * Returns a JWT access token for the test user (USER role).
     *
     * @return the access token string
     */
    protected String getUserToken() {
        return KeycloakTokenProvider.getUserToken();
    }

    /**
     * Returns a JWT access token for the admin user (USER + ADMIN roles).
     *
     * @return the access token string
     */
    protected String getAdminToken() {
        return KeycloakTokenProvider.getAdminToken();
    }

    /**
     * Returns a RestAssured request specification pre-configured with the test user's token.
     * <p>
     * Usage:
     * <pre>{@code
     * givenAuthenticatedUser()
     *     .when()
     *     .get("/api/v1/me")
     *     .then()
     *     .statusCode(200);
     * }</pre>
     *
     * @return a request specification with Bearer token authentication
     */
    protected RequestSpecification givenAuthenticatedUser() {
        return given()
                .header("Authorization", "Bearer " + getUserToken());
    }

    /**
     * Returns a RestAssured request specification pre-configured with the admin user's token.
     * <p>
     * Usage:
     * <pre>{@code
     * givenAuthenticatedAdmin()
     *     .when()
     *     .delete("/api/v1/admin/users/123")
     *     .then()
     *     .statusCode(204);
     * }</pre>
     *
     * @return a request specification with Bearer token authentication (admin)
     */
    protected RequestSpecification givenAuthenticatedAdmin() {
        return given()
                .header("Authorization", "Bearer " + getAdminToken());
    }

    /**
     * Returns a RestAssured request specification without authentication.
     * <p>
     * Usage:
     * <pre>{@code
     * givenUnauthenticated()
     *     .when()
     *     .get("/api/v1/me")
     *     .then()
     *     .statusCode(401);
     * }</pre>
     *
     * @return a request specification without authentication
     */
    protected RequestSpecification givenUnauthenticated() {
        return given();
    }
}
