package com.example.demo.testsupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.example.demo.testsupport.auth.TestJwtUtils;
import com.example.demo.testsupport.persistence.SchemaContext;
import com.example.demo.testsupport.persistence.TestSchemaFilter;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/**
 * Base class for REST API controller integration tests.
 *
 * <p>Extends {@link AbstractIntegrationTest} with RestAssured configuration and
 * JWT authentication helpers for testing secured endpoints.</p>
 *
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li>Automatic RestAssured configuration with random port</li>
 *   <li>Schema context propagation via X-Test-Schema header (per-request, thread-safe)</li>
 *   <li>JWT token generation helpers for USER and ADMIN roles</li>
 *   <li>Convenience methods: {@code givenAuthenticatedUser()}, {@code givenAuthenticatedAdmin()}</li>
 * </ul>
 *
 * <p><strong>Thread-Safety for Parallel Execution:</strong></p>
 * <ul>
 *   <li>Schema header is captured at request-time from {@link SchemaContext} ThreadLocal</li>
 *   <li>Avoids static {@code RestAssured.requestSpecification} which causes race conditions</li>
 *   <li>Each HTTP request gets the correct schema header for its test's isolated schema</li>
 * </ul>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * @SpringBootTest(webEnvironment = RANDOM_PORT)
 * @ActiveProfiles({"test", "integration"})
 * class MyControllerIT extends AbstractControllerIT {
 *
 *     @Test
 *     void shouldReturnOkForAuthenticatedUser() {
 *         givenAuthenticatedUser()
 *             .when()
 *             .get("/api/v1/resource")
 *             .then()
 *             .statusCode(200);
 *     }
 * }
 * }</pre>
 *
 * @see AbstractIntegrationTest
 */
public abstract class AbstractControllerIT extends AbstractIntegrationTest {

    @LocalServerPort
    protected int port;

    /**
     * Configure RestAssured base URI and port.
     *
     * <p><strong>Note:</strong> We intentionally do NOT set {@code RestAssured.requestSpecification}
     * here because it's a static field that gets overwritten by parallel tests. Instead, the schema
     * header is added per-request in {@link #givenWithSchemaHeader()}.</p>
     */
    @BeforeEach
    void setUpRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        // DO NOT set RestAssured.requestSpecification - it's static and causes race conditions
        // in parallel test execution. Schema header is added per-request instead.
    }

    @AfterEach
    void tearDownRestAssured() {
        RestAssured.reset();
    }

    /**
     * Creates a base request specification with the current test's schema header.
     *
     * <p>This method captures the schema from {@link SchemaContext} at call-time,
     * ensuring each HTTP request uses the correct isolated schema even during
     * parallel test execution.</p>
     *
     * @return a request specification with X-Test-Schema header set
     */
    private RequestSpecification givenWithSchemaHeader() {
        String schema = SchemaContext.getSchema();
        RequestSpecification spec = given();
        if (schema != null && !schema.isBlank()) {
            spec = spec.header(TestSchemaFilter.TEST_SCHEMA_HEADER, schema);
        }
        return spec;
    }

    /**
     * Returns a JWT access token for the test user (USER role).
     *
     * @return the access token string
     */
    protected String getUserToken() {
        return TestJwtUtils.getUserToken();
    }

    /**
     * Returns a JWT access token for the admin user (USER + ADMIN roles).
     *
     * @return the access token string
     */
    protected String getAdminToken() {
        return TestJwtUtils.getAdminToken();
    }

    /**
     * Returns a RestAssured request specification pre-configured with the test user's token.
     *
     * <p>The schema header is automatically included for proper test isolation.</p>
     *
     * <p>Usage:</p>
     * <pre>{@code
     * givenAuthenticatedUser()
     *     .when()
     *     .get("/api/v1/me")
     *     .then()
     *     .statusCode(200);
     * }</pre>
     *
     * @return a request specification with Bearer token authentication and schema header
     */
    protected RequestSpecification givenAuthenticatedUser() {
        return givenWithSchemaHeader()
                .header("Authorization", "Bearer " + getUserToken());
    }

    /**
     * Returns a RestAssured request specification pre-configured with the admin user's token.
     *
     * <p>The schema header is automatically included for proper test isolation.</p>
     *
     * <p>Usage:</p>
     * <pre>{@code
     * givenAuthenticatedAdmin()
     *     .when()
     *     .delete("/api/v1/admin/users/123")
     *     .then()
     *     .statusCode(204);
     * }</pre>
     *
     * @return a request specification with Bearer token authentication (admin) and schema header
     */
    protected RequestSpecification givenAuthenticatedAdmin() {
        return givenWithSchemaHeader()
                .header("Authorization", "Bearer " + getAdminToken());
    }

    /**
     * Returns a RestAssured request specification without authentication.
     *
     * <p>The schema header is automatically included for proper test isolation.</p>
     *
     * <p>Usage:</p>
     * <pre>{@code
     * givenUnauthenticated()
     *     .when()
     *     .get("/api/v1/greetings")
     *     .then()
     *     .statusCode(200);
     * }</pre>
     *
     * @return a request specification without authentication but with schema header
     */
    protected RequestSpecification givenUnauthenticated() {
        return givenWithSchemaHeader();
    }
}
