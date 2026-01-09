# 004. Integration Test Strategy

## Context and Problem Statement

We want integration tests for the `backend` service that:

1. **Exercise the real runtime**: Spring Boot web stack, real HTTP boundary (`RANDOM_PORT`), real PostgreSQL, and real Flyway migrations.
2. **Remain fast and reliable**: Avoid flaky cleanup, enable safe parallelism, and keep feedback loops short in CI.
3. **Scale with the schema**: Adding tables/constraints must not silently break isolation.
4. **Work with async processing**: Spring Modulith listeners and other `@Async` flows must still use the correct database isolation.
5. **Avoid unnecessary infrastructure**: Security-focused tests should not require a full Keycloak container just to validate authorization behavior.

## Considered Options

### A. Database isolation strategy

* **Schema-per-test (single shared PostgreSQL container)**
* **Database-per-test / database-per-class (single PostgreSQL instance)**
* **Container-per-test / container-per-class (multiple PostgreSQL containers)**
* **Manual cleanup (truncate tables / delete rows) in a shared schema**

### B. Security for integration tests

* **Run Keycloak in Testcontainers** and use real JWTs
* **Mock JWT validation** with locally minted tokens (HS256) but keep the resource-server behavior
* **Disable security entirely** for most integration tests

### C. Parallel execution model

* **Single JVM process + JUnit 5 parallel execution**
* **Multiple JVM forks (Surefire/Failsafe forkCount > 1)**

## Decision Outcome

Chosen strategy:

1. **Single shared PostgreSQL Testcontainer + schema-per-test-method isolation**
   - Each integration test method gets its own schema.
   - Flyway migrations are executed programmatically for that schema.
   - The schema is dropped `CASCADE` after the test.

2. **Schema routing via PostgreSQL `search_path` controlled by a ThreadLocal context**
   - The active schema is stored in a test-only `SchemaContext` (ThreadLocal).
   - A test-only `SmartRoutingDataSource` applies `SET search_path` on checkout and resets it on connection close to prevent connection-pool leakage.

3. **Explicit context propagation across thread boundaries**
   - **JUnit thread → Tomcat thread:** propagate via an HTTP header (`X-Test-Schema`) and a servlet filter.
   - **Submitting thread → async worker threads:** propagate via a `TaskDecorator` wired into the `applicationTaskExecutor` used by Spring Modulith.

4. **Security approach for tests**
   - Most tests run with the permissive `test` profile (security disabled for speed and simplicity).
   - Auth-focused integration tests use the `integration` profile and a mock resource-server configuration that validates locally minted HS256 JWTs while preserving claim/role mapping.

5. **Parallelization approach**
   - Run integration tests in a **single JVM** (Failsafe `forkCount=1`).
   - Enable JUnit 5 parallel execution for **test classes** while keeping **methods within a class sequential**.

## Consequences

* **Good**:
  * **Deterministic cleanup**: `DROP SCHEMA ... CASCADE` avoids "forgot to truncate a new table" drift.
  * **Realistic behavior**: tests cross the real HTTP boundary and exercise the same schema migrations used in production.
  * **Parallel-safe design**: isolation is per schema and pooled connections are reset on return to the pool.
  * **Async-safe**: schema context reaches Spring Modulith async listeners via the configured executor.
  * **No Keycloak container required**: security tests remain stable and fast while still validating JWT claim shape and role mapping.

* **Bad / Trade-offs**:
  * **More moving parts**: schema routing + propagation must be maintained carefully.
  * **Flyway cost per test**: migrating per schema adds overhead that grows with the migration set.
  * **Mock token drift risk**: locally minted JWTs can drift from production token shape unless kept aligned.

## References

1. Test persistence infrastructure (schema routing, filter, executor): `backend/src/test/java/com/example/demo/testsupport/persistence/`
2. Integration-test base classes: `backend/src/test/java/com/example/demo/testsupport/`
3. JUnit parallel execution config: `backend/src/test/resources/junit-platform.properties`
4. Maven integration-test configuration (Failsafe `forkCount=1`): `backend/pom.xml`
5. OpenAPI contract validation helper: `backend/src/test/java/com/example/demo/contract/OpenApiValidator.java`
