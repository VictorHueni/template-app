package com.example.demo.audit;

import io.hypersistence.utils.hibernate.id.Tsid;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;

/**
 * Permanent business activity log entry.
 *
 * <p>This entity represents the "Archive" - an immutable record of business events
 * for compliance, debugging, and audit trail purposes. Records are never deleted.</p>
 *
 * <p>Contrasts with the transactional {@code EVENT_PUBLICATION} table (the "Mailman")
 * which is auto-managed by Spring Modulith and cleaned up after successful delivery.</p>
 */
@Entity
@Table(name = "business_activity_log")
@NoArgsConstructor
@Getter
public class BusinessActivityLog {

    @Id
    @Tsid
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "actor_user_id", updatable = false)
    private String actorUserId;

    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType;

    @Column(name = "aggregate_type", updatable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", updatable = false)
    private String aggregateId;

    @Column(name = "trace_id", updatable = false)
    private String traceId;

    @Type(JsonType.class)
    @Column(name = "data", columnDefinition = "json", updatable = false)
    private Map<String, Object> data;

    /**
     * Creates a new business activity log entry.
     *
     * @param occurredAt    when the event occurred
     * @param actorUserId   the user who triggered the event (nullable for system events)
     * @param eventType     the event class name
     * @param aggregateType the type of aggregate affected (e.g., "Greeting")
     * @param aggregateId   the ID of the aggregate affected
     * @param traceId       the distributed trace ID for correlation
     * @param data          the full event payload as a map
     */
    public BusinessActivityLog(
            Instant occurredAt,
            String actorUserId,
            String eventType,
            String aggregateType,
            String aggregateId,
            String traceId,
            Map<String, Object> data) {
        this.occurredAt = occurredAt;
        this.actorUserId = actorUserId;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.traceId = traceId;
        this.data = data;
    }
}
