package com.example.demo.audit;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.AuditorAware;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.demo.testsupport.AbstractRestAssuredIntegrationTest;
import com.example.demo.testsupport.DatabaseCleanupHelper;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;
import static org.mockito.Mockito.when;

/**
 * End-to-end integration test for the Business Activity Audit system.
 *
 * <p><strong>Test Scope:</strong> Verifies the complete event-driven audit flow:</p>
 * <ol>
 *   <li>REST API call creates a Greeting entity</li>
 *   <li>GreetingCreatedEvent is published via Spring Modulith events</li>
 *   <li>Audit listener asynchronously persists record to BUSINESS_ACTIVITY_LOG</li>
 *   <li>Spring Modulith outbox pattern cleans up EVENT_PUBLICATION table (completion-mode=DELETE)</li>
 * </ol>
 *
 * <p><strong>Test Isolation Strategy:</strong></p>
 * <ul>
 *   <li>@SpringBootTest(webEnvironment = RANDOM_PORT) - Real Spring context, real database</li>
 *   <li>@ResourceLock(value = "DB", mode = READ_WRITE) - Exclusive database access during test</li>
 *   <li>@BeforeEach cleanup - Prepares clean database state before test runs</li>
 *   <li>@AfterEach cleanup - CRITICAL for async events: prevents event processing from
 *       previous tests leaking into subsequent tests via the event_publication table</li>
 * </ul>
 *
 * <p><strong>Async Event Processing Important Notes:</strong></p>
 * <ul>
 *   <li>Spring Modulith uses @Async for event listeners (runs in separate thread pool)</li>
 *   <li>Test finishes before async processing completes</li>
 *   <li>await() with untilAsserted() waits for async completion (max 5 seconds)</li>
 *   <li>Without @AfterEach cleanup: pending events leak to next test → race conditions → flakiness</li>
 *   <li>With @AfterEach cleanup: Each test starts and ends with clean event state</li>
 * </ul>
 *
 * <p><strong>Why NOT to use @Transactional:</strong></p>
 * <ul>
 *   <li>@Transactional only rolls back changes in the test thread</li>
 *   <li>Spring Modulith event listeners run in separate @Async threads (not rolled back)</li>
 *   <li>Async processing happens outside the transaction scope</li>
 *   <li>Manual cleanup via @BeforeEach/@AfterEach is the only reliable approach</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "integration"})
@ResourceLock(value = "DB", mode = READ_WRITE)
class BusinessActivityIT extends AbstractRestAssuredIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DatabaseCleanupHelper cleanupHelper;

    /**
     * Prepare database before test by cleaning all related tables.
     *
     * <p>Called before EVERY test to ensure clean, isolated starting state.</p>
     *
     * <p>Tables cleaned:
     * <ul>
     *   <li>business_activity_log - audit records from GreetingCreatedEvent listener</li>
     *   <li>greeting - the Greeting entities being created and tested</li>
     *   <li>event_publication - Spring Modulith outbox table for event delivery</li>
     * </ul>
     */
    @BeforeEach
    void cleanupDatabase() {
        cleanupHelper.truncateTables("business_activity_log", "greeting", "event_publication");
    }

    /**
     * Clean up after test to prevent dirty data from leaking to next test.
     *
     * <p><strong>CRITICAL for async event processing:</strong></p>
     * <ul>
     *   <li>Spring Modulith event listeners are @Async (run in separate thread pool)</li>
     *   <li>Test completes before async event processing finishes</li>
     *   <li>If not cleaned, pending events in event_publication table leak to next test</li>
     *   <li>Next test sees events from previous test → race conditions → flaky tests</li>
     *   <li>@AfterEach ensures each test leaves database in clean state</li>
     * </ul>
     *
     * <p><strong>Example of what happens without @AfterEach cleanup:</strong></p>
     * <pre>
     * Test A: Creates greeting → GreetingCreatedEvent published → @AfterEach NOT called
     * Test A finishes (but async processing still pending in background)
     * Test B: @BeforeEach starts, but event_publication still has Test A's unprocessed events
     * Test B: Async processing from Test A completes in background
     * Test B: Verifies audit log, but finds BOTH Test A's AND Test B's events
     * Test B: FLAKY FAILURE - found unexpected audit log entries
     * </pre>
     *
     * <p><strong>With @AfterEach cleanup:</strong></p>
     * <pre>
     * Test A: Creates greeting → event processing begins
     * Test A: await() waits for async processing to complete (max 5 seconds)
     * Test A: @AfterEach cleanup removes all event_publication entries
     * Test B: Starts with clean event_publication table
     * Test B: No leftover events from Test A
     * Test B: RELIABLE - only sees its own events
     * </pre>
     */
    @AfterEach
    void cleanupAfterTest() {
        // Critical: cleanup even if test fails or times out waiting for async events
        cleanupHelper.truncateTables("business_activity_log", "greeting", "event_publication");
    }

    /**
     * Test the complete audit event flow: create greeting → async event processing → audit log.
     *
     * <p><strong>What this test verifies:</strong></p>
     * <ol>
     *   <li>REST POST /api/v1/greetings creates a greeting and returns 201</li>
     *   <li>GreetingCreatedEvent is published via Spring event mechanism</li>
     *   <li>Spring Modulith event listener picks up the event asynchronously</li>
     *   <li>Audit listener persists BusinessActivityLog record to database</li>
     *   <li>Spring Modulith outbox pattern deletes event from event_publication (completion-mode=DELETE)</li>
     * </ol>
     *
     * <p><strong>Async Event Handling Notes:</strong></p>
     * <ul>
     *   <li>Event listener is marked @Async, so it runs in separate thread pool</li>
     *   <li>REST response returns immediately (event processing continues in background)</li>
     *   <li>await().untilAsserted() polls the database waiting for event processing to complete</li>
     *   <li>Max wait time is 5 seconds; if event not processed by then, test fails</li>
     * </ul>
     */
    @Test
    @DisplayName("records business activity when greeting is created via REST API")
    void recordsBusinessActivityWhenGreetingCreatedViaRestApi() {
        // GIVEN: No data in database (cleaned by @BeforeEach)

        // WHEN: Create a greeting via REST API
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
        // Note: REST response returns immediately, but async event processing continues in background

        // THEN: Verify GreetingCreatedEvent was processed and audit log record was created
        // This uses await().untilAsserted() to poll the database until async processing completes
        // (or timeout after 5 seconds)
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // Query the audit log table for records matching this greeting
            List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                    "SELECT * FROM business_activity_log WHERE aggregate_id = ?",
                    greetingId
            );

            // Should have exactly one audit log entry (one for the create event)
            assertThat(logs).hasSize(1);

            // Verify the audit log entry contains correct event information
            Map<String, Object> log = logs.get(0);
            assertThat(log.get("event_type")).isEqualTo("GreetingCreatedEvent");
            assertThat(log.get("aggregate_type")).isEqualTo("Greeting");
            assertThat(log.get("aggregate_id")).isEqualTo(greetingId);
            assertThat(log.get("occurred_at")).isNotNull();
            assertThat(log.get("data")).isNotNull();  // Contains serialized event data
        });

        // AND: The EVENT_PUBLICATION (outbox) table is empty
        // This verifies Spring Modulith's completion-mode=DELETE successfully deleted the event
        // after it was processed
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM event_publication",
                    Integer.class
            );
            assertThat(count).isZero();  // Event publication table should be empty
        });
    }

    /**
     * Test that audit log correctly serializes and stores event payload as JSON.
     *
     * <p><strong>What this test verifies:</strong></p>
     * <ul>
     *   <li>Event data is correctly serialized to JSON format</li>
     *   <li>JSON contains all expected fields from GreetingCreatedEvent</li>
     *   <li>Audit log data column properly stores the serialized JSON</li>
     * </ul>
     */
    @Test
    @DisplayName("audit log data contains event payload as JSON")
    void auditLogDataContainsEventPayloadAsJson() {
        // WHEN: Create a greeting via REST API with specific values
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

        // THEN: Verify the audit log entry contains serialized JSON data
        // Wait for async event processing to complete
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // Query the data column (JSONB type, cast to text for comparison)
            String jsonData = jdbcTemplate.queryForObject(
                    "SELECT data::text FROM business_activity_log WHERE aggregate_id = ?",
                    String.class,
                    greetingId
            );

            // Verify JSON contains expected fields and values
            assertThat(jsonData)
                    .contains("\"message\"")
                    .contains("JSON Test Message")
                    .contains("\"recipient\"")
                    .contains("JsonRecipient")
                    .contains("\"reference\"")  // Greeting reference like "GRE-0001-123456"
                    .contains("GRE-");
        });
    }
}
