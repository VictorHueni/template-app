package com.example.demo;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HelloApiIT {

    @LocalServerPort
    int port;

    @BeforeEach
    void configureRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @Test
    void healthEndpointIsOk() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("ok"));
    }

    @Test
    void helloDefault() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/api/hello")
                .then()
                .statusCode(200)
                .body("message", equalTo("Hello world"));
    }

    @Test
    void helloCustomName() {
        given()
                .accept(ContentType.JSON)
                .queryParam("name", "Alice")
                .when()
                .get("/api/hello")
                .then()
                .statusCode(200)
                .body("message", equalTo("Hello Alice"));
    }
}