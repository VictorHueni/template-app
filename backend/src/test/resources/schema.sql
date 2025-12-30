-- ============================================================
-- schema.sql (Test Environment)
-- Complements Hibernate ddl-auto=create-drop
-- Used for objects Hibernate doesn't manage automatically
-- ============================================================

-- 1. SEQUENCES
-- Functional ID sequence for Greeting (used by FunctionalIdGenerator via native query)
-- Hibernate doesn't create this because it's not declared as a JPA @SequenceGenerator in any entity
CREATE SEQUENCE IF NOT EXISTS seq_greeting_reference START WITH 1 INCREMENT BY 1;

-- Note: seq_revinfo_id is managed by Hibernate (@SequenceGenerator in CustomRevisionEntity)
-- and doesn't strictly need to be here, but we could add it for completeness if needed.
