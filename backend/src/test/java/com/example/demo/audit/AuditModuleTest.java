package com.example.demo.audit;

import com.example.demo.greeting.event.GreetingCreatedEvent;
import com.example.demo.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

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
@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
@ContextConfiguration(classes = TestcontainersConfiguration.class)
class AuditModuleTest {

    @Autowired
    private BusinessActivityLogRepository repository;

    @MockitoBean
    private org.springframework.data.domain.AuditorAware<String> auditorAware;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    @DisplayName("records GreetingCreatedEvent in business activity log")
    void recordsGreetingCreatedEventInBusinessActivityLog(Scenario scenario) {
        // Given: a GreetingCreatedEvent
        var event = new GreetingCreatedEvent(
                506979954615549952L,
                "GRE-2025-000042",
                "Alice",
                "Hello, World!",
                "testuser",
                Instant.parse("2025-01-15T10:00:00Z")
        );

        // When: the event is published
        // Then: a record appears in the business activity log
        scenario.publish(event)
                .andWaitForStateChange(() -> repository.findByAggregateId("506979954615549952"))
                .andVerify(result -> {
                    assertThat(result).isPresent();

                    BusinessActivityLog log = result.get();
                    assertThat(log.getEventType()).isEqualTo("GreetingCreatedEvent");
                    assertThat(log.getAggregateType()).isEqualTo("Greeting");
                    assertThat(log.getAggregateId()).isEqualTo("506979954615549952");
                    assertThat(log.getActorUserId()).isEqualTo("testuser");
                    assertThat(log.getOccurredAt()).isEqualTo(Instant.parse("2025-01-15T10:00:00Z"));

                    // Verify the event data was serialized
                    assertThat(log.getData()).isNotNull();
                    assertThat(log.getData()).containsEntry("recipient", "Alice");
                    assertThat(log.getData()).containsEntry("message", "Hello, World!");
                    assertThat(log.getData()).containsEntry("reference", "GRE-2025-000042");
                });
    }
}
