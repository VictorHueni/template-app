package com.example.demo.audit;

import com.example.demo.testsupport.AbstractRestAssuredIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.notNullValue;

/**
 * End-to-end integration test for the Business Activity Audit system.
 *
 * <p>This test verifies the full flow:</p>
 * <ol>
 *   <li>REST API call creates a Greeting</li>
 *   <li>GreetingCreatedEvent is published</li>
 *   <li>Audit listener persists record to BUSINESS_ACTIVITY_LOG</li>
 *   <li>EVENT_PUBLICATION (outbox) table is cleaned up</li>
 * </ol>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Execution(ExecutionMode.SAME_THREAD)
class BusinessActivityIT extends AbstractRestAssuredIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanupDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE business_activity_log CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE greeting CASCADE");
        // Also clean up the event publication table if it exists
        try {
            jdbcTemplate.execute("DELETE FROM event_publication");
        } catch (Exception e) {
            // Table may not exist yet on first run
        }
    }

    @Test
    @DisplayName("records business activity when greeting is created via REST API")
    void recordsBusinessActivityWhenGreetingCreatedViaRestApi() {
        // When: Create a greeting via REST API
        String greetingId = given()
                .contentType("application/json")
                .body("""
                       {"message": "Hello, Audit!", "recipient": "AuditTest"}
                       """)
                .when()
                .post("/api/v1/greetings")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract()
                .path("id");

        // Then: A record appears in BUSINESS_ACTIVITY_LOG
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                    "SELECT * FROM business_activity_log WHERE aggregate_id = ?",
                    greetingId
            );

            assertThat(logs).hasSize(1);

            Map<String, Object> log = logs.get(0);
            assertThat(log.get("event_type")).isEqualTo("GreetingCreatedEvent");
            assertThat(log.get("aggregate_type")).isEqualTo("Greeting");
            assertThat(log.get("aggregate_id")).isEqualTo(greetingId);
            assertThat(log.get("occurred_at")).isNotNull();
            assertThat(log.get("data")).isNotNull();
        });

        // And: The EVENT_PUBLICATION table is empty (completion-mode=DELETE worked)
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM event_publication",
                    Integer.class
            );
            assertThat(count).isZero();
        });
    }

    @Test
    @DisplayName("audit log data contains event payload as JSON")
    void auditLogDataContainsEventPayloadAsJson() {
        // When: Create a greeting via REST API
        String greetingId = given()
                .contentType("application/json")
                .body("""
                       {"message": "JSON Test Message", "recipient": "JsonRecipient"}
                       """)
                .when()
                .post("/api/v1/greetings")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // Then: The data column contains the serialized event
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            String jsonData = jdbcTemplate.queryForObject(
                    "SELECT data::text FROM business_activity_log WHERE aggregate_id = ?",
                    String.class,
                    greetingId
            );

            assertThat(jsonData)
                    .contains("\"message\"")
                    .contains("JSON Test Message")
                    .contains("\"recipient\"")
                    .contains("JsonRecipient")
                    .contains("\"reference\"")
                    .contains("GRE-");
        });
    }
}
