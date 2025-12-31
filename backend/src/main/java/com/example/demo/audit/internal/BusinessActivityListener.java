package com.example.demo.audit.internal;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.example.demo.audit.BusinessActivityLog;
import com.example.demo.audit.BusinessActivityLogRepository;
import com.example.demo.greeting.event.GreetingCreatedEvent;

import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

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
@Slf4j
class BusinessActivityListener {

    private final BusinessActivityLogRepository repository;
    private final JsonMapper jsonMapper;
    private final AuditorAware<String> auditorAware;
    private final Tracer tracer;

    BusinessActivityListener(
            BusinessActivityLogRepository repository,
            JsonMapper jsonMapper,
            AuditorAware<String> auditorAware,
            ObjectProvider<Tracer> tracerProvider) {

        this.repository = repository;
        this.jsonMapper = jsonMapper;
        this.auditorAware = auditorAware;
        this.tracer = tracerProvider.getIfAvailable();
    }

    /**
     * Handles GreetingCreatedEvent by recording it in the audit log.
     *
     * @param event the greeting created event
     */
    @ApplicationModuleListener
    void onGreetingCreated(GreetingCreatedEvent event) {
        log.debug("Recording GreetingCreatedEvent for greeting {}", event.greetingId());

        BusinessActivityLog activityLog = new BusinessActivityLog(
                event.createdAt(),
                resolveActorUserId(event),
                "GreetingCreatedEvent",
                event.aggregateType(),
                event.aggregateIdAsString(),
                resolveTraceId(),
                serializeEventData(event)
        );

        repository.save(activityLog);

        log.info("Recorded business activity: {} for {} {}",
                activityLog.getEventType(), activityLog.getAggregateType(), activityLog.getAggregateId());
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
    private Map<String, Object> serializeEventData(Object event) {
        try {
            return jsonMapper.convertValue(event, new TypeReference<Map<String, Object>>() {});
        }
        catch (JacksonException e) {
            log.warn("Failed to serialize event data for {}:{}", event.getClass().getSimpleName(), e.getMessage());
            return Map.of("error", "serialization_failed", "eventType", event.getClass().getSimpleName());
        }
    }
}
