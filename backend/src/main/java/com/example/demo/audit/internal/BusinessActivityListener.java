package com.example.demo.audit.internal;

import com.example.demo.audit.BusinessActivityLog;
import com.example.demo.audit.BusinessActivityLogRepository;
import com.example.demo.greeting.event.GreetingCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AuditorAware;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Event listener that records business activities in the audit log.
 *
 * <p>This is an internal component of the audit module. It listens to domain
 * events and persists them to the {@code BUSINESS_ACTIVITY_LOG} table.</p>
 *
 * <p>Uses {@link ApplicationModuleListener} for transactional event handling
 * with at-least-once delivery semantics.</p>
 */
@Component
class BusinessActivityListener {

    private static final Logger LOG = LoggerFactory.getLogger(BusinessActivityListener.class);

    private final BusinessActivityLogRepository repository;
    private final ObjectMapper objectMapper;
    private final AuditorAware<String> auditorAware;
    private final Tracer tracer;

    BusinessActivityListener(
            BusinessActivityLogRepository repository,
            ObjectMapper objectMapper,
            AuditorAware<String> auditorAware,
            Tracer tracer) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.auditorAware = auditorAware;
        this.tracer = tracer;
    }

    /**
     * Handles GreetingCreatedEvent by recording it in the audit log.
     *
     * @param event the greeting created event
     */
    @ApplicationModuleListener
    void onGreetingCreated(GreetingCreatedEvent event) {
        LOG.debug("Recording GreetingCreatedEvent for greeting {}", event.greetingId());

        BusinessActivityLog log = new BusinessActivityLog(
                event.createdAt(),
                resolveActorUserId(event),
                "GreetingCreatedEvent",
                event.aggregateType(),
                event.aggregateIdAsString(),
                resolveTraceId(),
                serializeEventData(event)
        );

        repository.save(log);

        LOG.info("Recorded business activity: {} for {} {}",
                log.getEventType(), log.getAggregateType(), log.getAggregateId());
    }

    /**
     * Resolves the actor user ID from the event or falls back to current auditor.
     */
    private String resolveActorUserId(GreetingCreatedEvent event) {
        // Prefer the user from the event (set at creation time)
        if (event.createdBy() != null && !event.createdBy().isBlank()) {
            return event.createdBy();
        }
        // Fallback to current auditor (may differ if event is processed async)
        return auditorAware.getCurrentAuditor().orElse("system");
    }

    /**
     * Resolves the current trace ID from Micrometer Tracer.
     */
    private String resolveTraceId() {
        if (tracer != null && tracer.currentSpan() != null) {
            var context = tracer.currentSpan().context();
            if (context != null) {
                return context.traceId();
            }
        }
        return null;
    }

    /**
     * Serializes the event to a Map for JSONB storage.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> serializeEventData(Object event) {
        try {
            return objectMapper.convertValue(event, Map.class);
        } catch (IllegalArgumentException e) {
            LOG.warn("Failed to serialize event data for {}: {}", event.getClass().getSimpleName(), e.getMessage());
            return Map.of("error", "serialization_failed", "eventType", event.getClass().getSimpleName());
        }
    }
}
