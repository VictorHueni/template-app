# Implementation Plan - Database Migration (Flyway)

## 1. Analysis & Approach

**Current State:**
*   The project relies on `spring.jpa.hibernate.ddl-auto` (`update` in dev, `validate` in prod) and a `schema.sql` file for initialization.
*   This approach is non-deterministic and unsafe for production schema evolution.
*   **Spring Boot Version:** 4.0.1
*   **Database:** PostgreSQL
*   **ID Generation:**
    *   TSID (in-memory via hypersistence-utils) for entity primary keys
    *   `greeting_sequence` for functional IDs via `FunctionalIdGenerator`
*   **Audit:** Hibernate Envers with ValidityAuditStrategy
*   **Event Outbox:** Spring Modulith

**Target State:**
*   **Tool:** Flyway Community Edition.
*   **Workflow:** SQL-based migrations in `src/main/resources/db/migration`.
*   **Strictness:** `ddl-auto` will be set to `validate` to ensure JPA entities match the Flyway-managed schema.
*   **Test Strategy:** Flyway disabled for H2 unit tests (uses `create-drop`), enabled for PostgreSQL integration tests.

---

## 2. Implementation Steps

### Step 1: Add Dependencies

**File:** `backend/pom.xml`
**Action:** Add dependencies to `<dependencies>` section.

```xml
<!-- Flyway for Database Migrations -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

---

### Step 2: Configure Flyway & JPA (All Profiles)

#### 2.1 Base Configuration
**File:** `backend/src/main/resources/application.properties`
**Action:** Add to existing configuration.

```properties
# ========================================
# 9. FLYWAY CONFIGURATION
# ========================================
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=0

# Disable legacy script initialization (replaced by Flyway)
spring.sql.init.mode=never

# Disable Spring Modulith auto-schema (Flyway manages event_publication table)
spring.modulith.events.jdbc-schema-initialization.enabled=false
```

#### 2.2 Local Profile
**File:** `backend/src/main/resources/application-local.properties`
**Action:** Update JPA and remove legacy SQL init.

```properties
# Change from 'update' to 'validate'
spring.jpa.hibernate.ddl-auto=validate

# Remove these lines:
# spring.sql.init.mode=always
# spring.sql.init.continue-on-error=true
```

#### 2.3 Dev Profile
**File:** `backend/src/main/resources/application-dev.properties`
**Action:** Update JPA and remove legacy SQL init.

```properties
# Change from 'update' to 'validate'
spring.jpa.hibernate.ddl-auto=validate

# Remove these lines:
# spring.sql.init.mode=always
# spring.sql.init.continue-on-error=true
```

#### 2.4 Prod Profile
**File:** `backend/src/main/resources/application-prod.properties`
**Action:** Already has `spring.jpa.hibernate.ddl-auto=validate` and `spring.sql.init.mode=never`. No changes needed.

#### 2.5 Test Profile
**File:** `backend/src/main/resources/application-test.properties`
**Action:** Disable Flyway for H2 unit tests.

```properties
# ========================================
# 9. FLYWAY CONFIGURATION (TEST)
# ========================================
# Disable Flyway for H2 unit tests (uses Hibernate DDL instead)
spring.flyway.enabled=false

# Keep create-drop for H2 unit tests
spring.jpa.hibernate.ddl-auto=create-drop

# Re-enable Spring Modulith auto-schema for H2
spring.modulith.events.jdbc-schema-initialization.enabled=true

# Keep existing SQL init for H2 compatibility
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:schema.sql
spring.jpa.defer-datasource-initialization=true
```

#### 2.6 Integration Test Profile (NEW FILE)
**File:** `backend/src/test/resources/application-integration.properties`

```properties
# ========================================
# INTEGRATION TEST PROFILE
# ========================================
# Enable Flyway for PostgreSQL Testcontainers
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=0

# Validate schema after Flyway migration
spring.jpa.hibernate.ddl-auto=validate

# Disable legacy SQL init
spring.sql.init.mode=never

# Disable Spring Modulith auto-schema (Flyway manages it)
spring.modulith.events.jdbc-schema-initialization.enabled=false
```

---

### Step 3: Create Baseline Migration (V1)

**File:** `backend/src/main/resources/db/migration/V1__Init_Schema.sql`

```sql
-- ============================================================
-- V1__Init_Schema.sql
-- Baseline migration for Spring Boot 4.0.1 application
-- Database: PostgreSQL
-- ============================================================

-- ============================================================
-- 1. SEQUENCES
-- ============================================================
-- Functional ID sequence (used by FunctionalIdGenerator for business-readable IDs)
CREATE SEQUENCE IF NOT EXISTS greeting_sequence START WITH 1 INCREMENT BY 1;

-- Hibernate Envers revision sequence (pooled allocation size = 50)
CREATE SEQUENCE IF NOT EXISTS revinfo_seq START WITH 1 INCREMENT BY 50;

-- ============================================================
-- 2. CORE TABLES
-- ============================================================

-- User table (Spring Security)
CREATE TABLE app_user (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    username VARCHAR(255) UNIQUE,
    password VARCHAR(255)
);

-- Greeting entity (extends AbstractBaseEntity, uses TSID for id)
CREATE TABLE greeting (
    id BIGINT NOT NULL PRIMARY KEY,
    version INTEGER,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE,
    created_by_id VARCHAR(255) NOT NULL,
    updated_by_id VARCHAR(255),
    reference VARCHAR(32) NOT NULL UNIQUE,
    recipient VARCHAR(255) NOT NULL,
    message VARCHAR(255) NOT NULL
);

-- ============================================================
-- 3. HIBERNATE ENVERS AUDIT TABLES (ValidityAuditStrategy)
-- ============================================================
-- Configuration:
--   audit_table_suffix=_AUD (PostgreSQL folds to lowercase: _aud)
--   revision_field_name=REV (folds to: rev)
--   revision_type_field_name=REVTYPE (folds to: revtype)
--   ValidityAuditStrategy adds REVEND column

-- Revision info table (custom entity with username)
CREATE TABLE revinfo (
    rev INTEGER NOT NULL PRIMARY KEY,
    revtstmp BIGINT,
    username VARCHAR(255)
);

-- Greeting audit history table
CREATE TABLE greeting_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL,
    revtype SMALLINT,
    revend INTEGER,
    reference VARCHAR(32),
    recipient VARCHAR(255),
    message VARCHAR(255),
    PRIMARY KEY (rev, id),
    CONSTRAINT fk_greeting_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev),
    CONSTRAINT fk_greeting_aud_revend FOREIGN KEY (revend) REFERENCES revinfo (rev)
);

-- ============================================================
-- 4. BUSINESS ACTIVITY LOG
-- ============================================================
CREATE TABLE business_activity_log (
    id BIGINT NOT NULL PRIMARY KEY,
    occurred_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    actor_user_id VARCHAR(255),
    event_type VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255),
    aggregate_id VARCHAR(255),
    trace_id VARCHAR(255),
    data JSONB
);

-- Indexes for query performance
CREATE INDEX idx_business_activity_log_trace_id ON business_activity_log (trace_id);
CREATE INDEX idx_business_activity_log_aggregate ON business_activity_log (aggregate_type, aggregate_id);
CREATE INDEX idx_business_activity_log_event_type ON business_activity_log (event_type);
CREATE INDEX idx_business_activity_log_occurred_at ON business_activity_log (occurred_at);

-- ============================================================
-- 5. SPRING MODULITH EVENT PUBLICATION (Transactional Outbox)
-- ============================================================
-- Schema based on Spring Modulith 2.0.x for PostgreSQL
CREATE TABLE event_publication (
    id UUID NOT NULL PRIMARY KEY,
    listener_id VARCHAR(512) NOT NULL,
    event_type VARCHAR(512) NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    completion_date TIMESTAMP(6) WITH TIME ZONE
);

-- Index for efficient incomplete event queries
CREATE INDEX idx_event_publication_incomplete ON event_publication (publication_date)
    WHERE completion_date IS NULL;
```

---

### Step 4: Update Integration Test Base Class

**File:** `backend/src/test/java/.../AbstractIntegrationTest.java` (or similar)
**Action:** Add `integration` profile to load Flyway configuration.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "integration"})
@Testcontainers
public abstract class AbstractIntegrationTest {
    // ... existing code
}
```

---

### Step 5: Cleanup

1. **Delete:** `backend/src/main/resources/schema.sql` (now obsolete for PostgreSQL)
2. **Keep:** `schema.sql` only if needed for H2 unit tests (optional - H2 uses `create-drop`)

---

## 3. Verification Strategy

### 3.1 Unit Tests (H2)
```bash
mvn clean test
# Uses H2 with create-drop, Flyway disabled
# Verifies entity mappings work with auto-generated schema
```

### 3.2 Integration Tests (PostgreSQL + Testcontainers)
```bash
mvn clean verify -Pintegration-tests
# Uses PostgreSQL Testcontainers
# Flyway runs V1 migration
# Hibernate validates schema matches entities
```

### 3.3 Local Development
```bash
# Start PostgreSQL container
docker run --name spring-postgres -p 5432:5432 \
  -e POSTGRES_DB=spring_db \
  -e POSTGRES_USER=spring_user \
  -e POSTGRES_PASSWORD=secure_password \
  -d postgres:16-alpine

# Run application
mvn spring-boot:run
# Flyway migrates, Hibernate validates
```

---

## 4. Future Migration Example

**File:** `backend/src/main/resources/db/migration/V2__Add_Greeting_Category.sql`
```sql
-- Add category column to greeting table
ALTER TABLE greeting ADD COLUMN category VARCHAR(50);

-- Add index for category queries
CREATE INDEX idx_greeting_category ON greeting (category);

-- Update audit table to include new column
ALTER TABLE greeting_aud ADD COLUMN category VARCHAR(50);
```

---

## 5. Next Steps

1. [ ] Approve this plan
2. [ ] Add Flyway dependencies to `pom.xml`
3. [ ] Update `application.properties` (base config)
4. [ ] Update `application-local.properties`
5. [ ] Update `application-dev.properties`
6. [ ] Update `application-test.properties`
7. [ ] Create `application-integration.properties` (test resources)
8. [ ] Create `V1__Init_Schema.sql`
9. [ ] Update integration test base class with `@ActiveProfiles`
10. [ ] Delete obsolete `schema.sql`
11. [ ] Run verification tests
