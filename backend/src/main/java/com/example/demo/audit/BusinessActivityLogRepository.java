package com.example.demo.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for querying business activity logs.
 *
 * <p>This is part of the module's public API and can be used by other modules
 * or services to query audit history.</p>
 */
public interface BusinessActivityLogRepository extends JpaRepository<BusinessActivityLog, Long> {

    /**
     * Find a log entry by aggregate ID.
     *
     * @param aggregateId the aggregate ID to search for
     * @return the log entry if found
     */
    Optional<BusinessActivityLog> findByAggregateId(String aggregateId);

    /**
     * Find all log entries for a specific aggregate.
     *
     * @param aggregateType the type of aggregate (e.g., "Greeting")
     * @param aggregateId   the aggregate ID
     * @return all log entries for this aggregate, ordered by occurrence time
     */
    List<BusinessActivityLog> findByAggregateTypeAndAggregateIdOrderByOccurredAtAsc(
            String aggregateType, String aggregateId);

    /**
     * Find all log entries with a specific trace ID.
     *
     * @param traceId the distributed trace ID
     * @return all log entries in this trace
     */
    List<BusinessActivityLog> findByTraceIdOrderByOccurredAtAsc(String traceId);

    /**
     * Find all log entries of a specific event type.
     *
     * @param eventType the event type (class simple name)
     * @return all log entries of this type
     */
    List<BusinessActivityLog> findByEventType(String eventType);
}
