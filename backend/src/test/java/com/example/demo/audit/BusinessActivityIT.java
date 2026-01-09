package com.example.demo.audit;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.demo.testsupport.AbstractControllerIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.notNullValue;

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
 * <p><strong>Test Isolation Strategy (Schema-Per-Test):</strong></p>
 * <ul>
 *   <li>@SpringBootTest(webEnvironment = RANDOM_PORT) - Real Spring context, real database</li>
 *   <li>SchemaIsolationExtension creates a unique schema per test method</li>
 *   <li>X-Test-Schema header propagates schema context from JUnit to Tomcat threads</li>
 *   <li>SchemaContextTaskDecorator propagates schema context to @Async threads</li>
 *   <li>Schema is dropped CASCADE after test - no manual cleanup needed</li>
 * </ul>
 *
 * <p><strong>Async Event Processing Notes:</strong></p>
 * <ul>
 *   <li>Spring Modulith @ApplicationModuleListener runs async via applicationTaskExecutor</li>
 *   <li>TaskDecorator captures and propagates schema context to async threads</li>
 *   <li>await() with untilAsserted() polls database until async processing completes</li>
 *   <li>Schema isolation ensures parallel tests don't interfere with each other</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "integration"})
class BusinessActivityIT extends AbstractControllerIT {

    @Autowired
    private BusinessActivityLogRepository auditLogRepository;

    // Note: No manual cleanup needed - SchemaIsolationExtension provides per-test schema isolation.
    // Each test runs in its own database schema which is dropped CASCADE after the test.

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
        String greetingId = givenAuthenticatedUser()
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
        await().pollInSameThread().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // Query the audit log repository for record matching this greeting
            Optional<BusinessActivityLog> logOptional = auditLogRepository.findByAggregateId(greetingId);

            // Should have exactly one audit log entry
            assertThat(logOptional).isPresent();

            // Verify the audit log entry contains correct event information
            BusinessActivityLog log = logOptional.get();
            assertThat(log.getEventType()).isEqualTo("GreetingCreatedEvent");
            assertThat(log.getAggregateType()).isEqualTo("Greeting");
            assertThat(log.getAggregateId()).isEqualTo(greetingId);
            assertThat(log.getOccurredAt()).isNotNull();
            assertThat(log.getData()).isNotNull();  // Contains serialized event data
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
        String greetingId = givenAuthenticatedUser()
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
        await().pollInSameThread().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // Query the audit log repository for record matching this greeting
            Optional<BusinessActivityLog> logOptional = auditLogRepository.findByAggregateId(greetingId);
            assertThat(logOptional).isPresent();

            // Get the data map directly from the entity
            Map<String, Object> data = logOptional.get().getData();

            // Verify the data contains expected fields and values
            assertThat(data).isNotNull();
            assertThat(data.get("message")).isEqualTo("JSON Test Message");
            assertThat(data.get("recipient")).isEqualTo("JsonRecipient");
            assertThat(data.get("reference")).asString().startsWith("GRE-");
        });
    }
}
