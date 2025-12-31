package com.example.demo.audit;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.demo.greeting.event.GreetingCreatedEvent;
import com.example.demo.testsupport.AbstractIntegrationTest;

import io.hypersistence.tsid.TSID;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module test for the Audit module.
 *
 * <p>This test verifies that the audit module correctly listens to domain events
 * and records them in the {@code BUSINESS_ACTIVITY_LOG} table.</p>
 *
 * <p>Uses Spring Modulith's {@link Scenario} API to publish events and verify
 * the resulting state changes.</p>
 */
@ApplicationModuleTest
@ActiveProfiles({"test", "integration"})
class AuditModuleTest extends AbstractIntegrationTest {

    @Autowired
    private BusinessActivityLogRepository repository;

    @MockitoBean
    private org.springframework.data.domain.AuditorAware<String> auditorAware;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    @DisplayName("records GreetingCreatedEvent in business activity log")
    void recordsGreetingCreatedEventInBusinessActivityLog(Scenario scenario) {
        // Given: a GreetingCreatedEvent with dynamic IDs to avoid collisions in shared DB
        long greetingId = TSID.Factory.getTsid().toLong();
        String reference = "GRE-" + greetingId;

        var event = new GreetingCreatedEvent(
                greetingId,
                reference,
                "Alice",
                "Hello, World!",
                "testuser",
                Instant.parse("2025-01-15T10:00:00Z")
        );

        // When: the event is published
        // Then: a record appears in the business activity log
        scenario.publish(event)
                .andWaitForStateChange(() -> repository.findByAggregateId(String.valueOf(greetingId)))
                .andVerify(result -> {
                    assertThat(result).isPresent();

                    BusinessActivityLog log = result.get();
                    assertThat(log.getEventType()).isEqualTo("GreetingCreatedEvent");
                    assertThat(log.getAggregateType()).isEqualTo("Greeting");
                    assertThat(log.getAggregateId()).isEqualTo(String.valueOf(greetingId));
                    assertThat(log.getActorUserId()).isEqualTo("testuser");
                    assertThat(log.getOccurredAt()).isEqualTo(Instant.parse("2025-01-15T10:00:00Z"));

                    // Verify the event data was serialized
                    assertThat(log.getData()).isNotNull();
                    assertThat(log.getData()).containsEntry("recipient", "Alice");
                    assertThat(log.getData()).containsEntry("message", "Hello, World!");
                    assertThat(log.getData()).containsEntry("reference", reference);
                });
    }
}
