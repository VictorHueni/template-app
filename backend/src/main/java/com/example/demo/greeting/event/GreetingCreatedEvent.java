package com.example.demo.greeting.event;

import java.time.Instant;

/**
 * Domain event published when a new Greeting is created.
 *
 * <p>This event is published transactionally within the same transaction
 * as the entity save. Spring Modulith's event externalization ensures
 * at-least-once delivery to all listeners.</p>
 *
 * <p>Consumers should handle this event idempotently as it may be
 * delivered more than once in failure recovery scenarios.</p>
 *
 * @param greetingId the TSID of the created greeting
 * @param reference  the human-readable functional ID (e.g., GRE-2025-000042)
 * @param recipient  the greeting recipient
 * @param message    the greeting message
 * @param createdBy  the user who created the greeting
 * @param createdAt  when the greeting was created
 */
public record GreetingCreatedEvent(
        Long greetingId,
        String reference,
        String recipient,
        String message,
        String createdBy,
        Instant createdAt
) {

    /**
     * Returns the aggregate type for this event.
     * Used by the audit module to categorize events.
     */
    public String aggregateType() {
        return "Greeting";
    }

    /**
     * Returns the aggregate ID as a string for this event.
     * Used by the audit module for correlation.
     */
    public String aggregateIdAsString() {
        return String.valueOf(greetingId);
    }
}
