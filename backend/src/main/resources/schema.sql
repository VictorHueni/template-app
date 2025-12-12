CREATE SEQUENCE IF NOT EXISTS greeting_sequence START WITH 1 INCREMENT BY 1;

-- ============================================================
-- BUSINESS ACTIVITY LOG INDEXES
-- ============================================================
-- Note: The business_activity_log table is created by Hibernate DDL
-- (via BusinessActivityLog entity). These indexes are added separately
-- for query performance. They use IF NOT EXISTS for idempotency.
--
-- Purpose: Permanent business record of domain events for compliance,
-- debugging, and audit trail. This is the "Archive" - never deleted.
-- The transactional EVENT_PUBLICATION table (outbox) is auto-managed
-- by Spring Modulith and cleaned up after successful delivery.
-- ============================================================

-- Index for trace correlation (find all events in a request)
-- Deferred: created by Hibernate or migration scripts
-- CREATE INDEX IF NOT EXISTS idx_business_activity_log_trace_id
--     ON business_activity_log (trace_id);

-- Index for aggregate lookups (find all events for an entity)
-- Deferred: created by Hibernate or migration scripts
-- CREATE INDEX IF NOT EXISTS idx_business_activity_log_aggregate
--     ON business_activity_log (aggregate_type, aggregate_id);