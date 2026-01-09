# Implementation Plan: Add a JPA Mapping Validation CI Job (Dedicated Schema + Dedicated Maven Profile)

## 1. Understanding the Problem

### Why are we doing this?
Our current PostgreSQL integration-test strategy uses **schema-per-test** and runs Flyway **programmatically** after the Spring `ApplicationContext` is already started.

That makes the suite fast and isolated, but it has a blind spot:
- Hibernate’s mapping validation (`spring.jpa.hibernate.ddl-auto=validate`) happens **during context startup**.
- In schema-per-test mode, the schema doesn’t exist yet at startup, so we intentionally set `ddl-auto=none` for the `integration` profile.

**Value of this work:**
- Catch “entity ↔ migration drift” early (missing columns, wrong nullability, missing tables, wrong types).
- Make schema correctness a **quality gate** in CI, independent of the schema-per-test IT suite.

### What is the current bottleneck / risk?
- The schema-per-test integration tests can pass even if JPA mappings drift from the Flyway migrations, because JPA validation is disabled under the `integration` profile.
- If drift reaches production, it tends to fail at runtime (e.g., on a query), which is slower to debug.

### Before vs After

| Aspect              | Before                                               | After                                             |
| ------------------- | ---------------------------------------------------- | ------------------------------------------------- |
| Schema-per-test ITs | Work (fast + isolated), but run with `ddl-auto=none` | Unchanged                                         |
| Mapping validation  | Not enforced for PostgreSQL + Flyway migrations      | New CI job fails fast on drift                    |
| Where it runs       | Nowhere consistently                                 | Dedicated Maven profile + dedicated CI job        |
| Schema used         | Per-test random schema                               | One dedicated schema (e.g., `mapping_validation`) |

**Diagram description (mental model):**
1) New job starts PostgreSQL Testcontainer.
2) Spring Boot starts with profile `mapping-validation`.
3) Flyway migrates schema `mapping_validation` automatically.
4) Hibernate validates mappings against `mapping_validation` during startup.
5) If anything mismatches → context fails → job fails.

---

## 2. Phase 0: Prerequisites & Configuration

### Step 0.1: Pick a dedicated schema name and freeze it
- **Why**: We want one stable schema target that never collides with schema-per-test, and is easy to spot in logs.
- **What**: Use a fixed schema name, e.g. `mapping_validation`.
- **Implementation Options & Design Decisions**:
  - Option A (preferred): `mapping_validation` (explicit, purpose-driven)
  - Option B: `public` (simpler but riskier; can hide schema-qualification issues)
- **Changes**:
  - Decide and document the schema name in this plan.
- **Verification**:
  - Manual: Schema name appears in Flyway logs once implemented.

### Step 0.2: Confirm the migrations are compatible with “single-schema Flyway at startup”
- **Why**: This job will execute Flyway at Spring startup (standard Boot lifecycle). If migrations depend on schema-per-test behavior, validation will become flaky.
- **What**: Ensure migrations don’t rely on test-only search_path tricks and create objects in the configured schema.
- **Implementation Options & Design Decisions**:
  - Option A: Keep migrations as-is; configure Flyway schema explicitly in the profile.
  - Option B: If needed, refactor migrations to be schema-explicit (heavier; do only if drift occurs).
- **Changes**:
  - No code change yet—this is a pre-check.
- **Verification**:
  - Manual: Run the new job once locally (Phase 3) and confirm Flyway runs cleanly.

---

## 3. Phase 1: Add a dedicated Spring profile for mapping validation

### Step 1.1: Add `application-mapping-validation.properties`
- **Why**: We must enable Flyway + `ddl-auto=validate` in a mode compatible with context startup, without changing existing IT behavior.
- **What**: Create a test profile overlay that:
  - Enables Flyway
  - Targets the dedicated schema
  - Enables Hibernate validation
  - Avoids extra initialization that could mask issues
- **Implementation Options & Design Decisions**:
  - Option A (preferred): `backend/src/test/resources/application-mapping-validation.properties`
    - Keeps it clearly “test-only”
    - Doesn’t affect runtime profiles
  - Option B: Add to an existing profile (not recommended: increases accidental coupling)
- **Changes**:
  - Create: `backend/src/test/resources/application-mapping-validation.properties`

  Suggested contents (example; keep minimal):

  ```properties
  # Dedicated profile for entity↔migration validation

  # Flyway MUST run at startup in this tier
  spring.flyway.enabled=true
  spring.flyway.create-schemas=true
  spring.flyway.schemas=mapping_validation
  spring.flyway.default-schema=mapping_validation

  # Hibernate validation MUST run at startup
  spring.jpa.hibernate.ddl-auto=validate
  spring.jpa.properties.hibernate.default_schema=mapping_validation

  # Avoid parallel/duplicate schema initialization mechanisms
  spring.sql.init.mode=never
  spring.jpa.defer-datasource-initialization=false

  # Optional: speed up the job (no web server needed)
  spring.main.web-application-type=none

  # Keep modulith table init under Flyway control
  spring.modulith.events.jdbc.schema-initialization.enabled=false
  ```

- **Verification**:
  - Manual checklist:
    - Startup logs show Flyway migrating `mapping_validation`.
    - No references to schema-per-test headers like `X-Test-Schema`.
  - Local run (once implemented):
    - `cd backend; ./mvnw -B verify -Pmapping-validation`
    - Expected: `BUILD SUCCESS`

---

## 4. Phase 2: Add a dedicated “context boots + validate” integration test

### Step 2.1: Add a single IT that only verifies startup succeeds
- **Why**: The simplest and most reliable mapping validation is: “does the Spring context boot when `ddl-auto=validate` is enabled after Flyway migrations?”. If validation fails, the context fails → the test fails.
- **What**: Add a new `*IT` class executed by Failsafe that:
  - Uses PostgreSQL Testcontainers (`TestcontainersConfiguration`)
  - Activates profiles `test` + `mapping-validation`
  - Does NOT use schema-per-test extension (`SchemaIsolationExtension`) or the routing datasource (`TestPersistenceConfig`)
- **Implementation Options & Design Decisions**:
  - Option A (preferred): `@SpringBootTest(webEnvironment = NONE)` + `@Import(TestcontainersConfiguration.class)` + `@ActiveProfiles({"test", "mapping-validation"})`
    - Very small surface area
    - Uses the same container wiring as other ITs
  - Option B: `ApplicationContextRunner`
    - Faster, but more “frameworky” and less junior-friendly
- **Changes**:
  - Create: `backend/src/test/java/com/example/demo/testsupport/validation/JpaMappingValidationIT.java`

  Example skeleton:

  ```java
  package com.example.demo.testsupport.validation;

  import com.example.demo.testsupport.TestcontainersConfiguration;
  import org.junit.jupiter.api.Test;
  import org.springframework.boot.test.context.SpringBootTest;
  import org.springframework.context.annotation.Import;
  import org.springframework.test.context.ActiveProfiles;

  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
  @Import(TestcontainersConfiguration.class)
  @ActiveProfiles({"test", "mapping-validation"})
  class JpaMappingValidationIT {

      @Test
      void contextStartsAndMappingsValidate() {
          // If mappings don’t match Flyway schema, the context will fail to start
          // and this test will fail before it reaches this line.
      }
  }
  ```

- **Verification**:
  - Manual checks:
    - Logs show `Flyway Community Edition` migration executed.
    - Logs show Hibernate startup without schema errors.
  - Local:
    - `cd backend; ./mvnw -B verify -Pintegration-tests -Dit.test=*JpaMappingValidationIT`
    - Expected: `BUILD SUCCESS`

---

## 5. Phase 3: Add a dedicated Maven profile that runs ONLY the validation test

### Step 3.1: Add Maven profile `mapping-validation`
- **Why**: CI should have a stable, memorable command that runs exactly one purpose-built test tier. Juniors should not need to remember `-Dit.test=...`.
- **What**: Add a Maven profile that:
  - Skips unit tests (`skipUTs=true`)
  - Configures Failsafe selection to run only `JpaMappingValidationIT`
- **Implementation Options & Design Decisions**:
  - Option A (preferred): Profile sets the standard `it.test` property
    - Works with `maven-failsafe-plugin` without plugin rewiring
  - Option B: Override Failsafe `<includes>` in the profile
    - More explicit, but more verbose config
- **Changes**:
  - Update: `backend/pom.xml`

  Add a new profile near existing IT profiles:

  ```xml
  <profile>
    <id>mapping-validation</id>
    <properties>
      <skipUTs>true</skipUTs>
      <it.test>*JpaMappingValidationIT</it.test>
    </properties>
  </profile>
  ```

  Then, in CI and locally, run:
  - `./mvnw -B verify -Pmapping-validation`

- **Verification**:
  - Local:
    - `cd backend; ./mvnw -B verify -Pmapping-validation`
    - Expected: `BUILD SUCCESS`
  - Regression:
    - `cd backend; ./mvnw -B verify -Pintegration-tests`
    - Expected: `BUILD SUCCESS` (existing suite unchanged)

---

## 6. Phase 4: Add a dedicated CI job (artifact reuse, parallel gate)

### Step 4.1: Add `backend_mapping_validation` job in GitHub Actions
- **Why**: This must be a first-class quality gate, not an occasional local check.
- **What**: Add a new job to the backend workflow that:
  - Depends on the compiled artifact reuse pipeline (same as `backend_integration_test`)
  - Runs only the mapping-validation profile
  - Uploads failsafe reports
- **Implementation Options & Design Decisions**:
  - Option A (preferred): Add a new job alongside `backend_integration_test`
    - Clear separation: “IT suite” vs “mapping validation”
  - Option B: Fold into the existing integration test job
    - Less parallelism and harder triage
- **Changes**:
  - Update: `.github/workflows/backend-ci.yml`

  Pattern to follow (same artifact downloads as other jobs):
  - Command: `./mvnw -B verify -Pmapping-validation`
  - Env: keep `TESTCONTAINERS_RYUK_DISABLED: true` (consistent with existing IT job)
  - Upload artifact: `backend-mapping-validation-test-results` → `backend/target/failsafe-reports`

- **Verification**:
  - Manual:
    - PR run shows an additional job named like “Mapping Validation”.
    - If you introduce a deliberate mismatch (e.g., remove a column in a migration), the job fails with a Hibernate validation error.
  - Expected output on success: `BUILD SUCCESS`

---

## 7. Phase 5 (Optional): Document the new test tier for juniors

### Step 5.1: Add a short section to the testing docs
- **Why**: Juniors need to know *which* suite catches *which* failures.
- **What**: Add a short section “Mapping Validation Tier” to the integration testing docs.
- **Implementation Options & Design Decisions**:
  - Option A (preferred): Update the existing crosscutting integration testing doc
  - Option B: Put it in a developer guide (fine if you prefer)
- **Changes**:
  - Update: `website/docs/architecture/08-crosscutting-concepts/integration-testing.md`
  - Add commands:
    - `./mvnw -B verify -Pmapping-validation`
- **Verification**:
  - Manual: docs are linked and explain when to use the tier.
