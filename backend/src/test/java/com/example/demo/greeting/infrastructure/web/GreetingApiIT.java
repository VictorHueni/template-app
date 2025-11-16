package com.example.demo.greeting.infrastructure.web;

import com.example.demo.DemoApplication;
import com.example.demo.greeting.infrastructure.db.AbstractPostgresIT;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(
        classes = DemoApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class GreetingApiIT extends AbstractPostgresIT {

    @LocalServerPort
    int port;

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @Test
    void createsGreetingAndReturnsContract() {
        given()
                .contentType("application/json")
                .body("""
                       {"name": "Charlie"}
                       """)
                .when()
                .post("/api/greetings")
                .then()
                .statusCode(200)
                .body("name", equalTo("Charlie"))
                .body("message", equalTo("Hello Charlie"))
                .body("id", notNullValue())
                .body("createdAt", notNullValue());
    }
}
