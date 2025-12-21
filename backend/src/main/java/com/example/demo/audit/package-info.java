/**
 * Audit module - Business activity logging for compliance and debugging.
 *
 * <p>This module listens to domain events and records them in a permanent
 * {@code BUSINESS_ACTIVITY_LOG} table for audit trail purposes.</p>
 *
 * <p>The module is intentionally isolated and communicates via events only,
 * making it a good candidate for extraction into a separate microservice.</p>
 *
 * <h2>Module API</h2>
 * <ul>
 *   <li>{@link com.example.demo.audit.BusinessActivityLog} - The audit record entity</li>
 *   <li>{@link com.example.demo.audit.BusinessActivityLogRepository} - Query access to audit logs</li>
 * </ul>
 *
 * <h2>Internal Components</h2>
 * <ul>
 *   <li>{@link com.example.demo.audit.internal.BusinessActivityListener} - Event consumer</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"common", "greeting :: events"}
)
package com.example.demo.audit;
