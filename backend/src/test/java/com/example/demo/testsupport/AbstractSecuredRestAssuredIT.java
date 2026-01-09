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

public abstract class AbstractSecuredRestAssuredIT extends AbstractIntegrationTest {

    @LocalServerPort
    protected int port;

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
        return null;
    }

    /**
     * Returns a JWT access token for the admin user (USER + ADMIN roles).
     *
     * @return the access token string
     */
    protected String getAdminToken() {
        return null;
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
