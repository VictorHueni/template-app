# Implementation Plan: Hybrid Parallel Integration Test Strategy

## 1. Understanding the Problem

### The Current State ("Big Bang" Testing)
Currently, our integration tests rely on heavy resources:
1.  **Keycloak Container:** Starts a real Keycloak instance (~15-20s startup).
2.  **Shared Database with Locking:** We use `@ResourceLock("DB")`, forcing tests to run one by one (serialized) to avoid data pollution.
3.  **Data Cleanup:** We rely on `DatabaseCleanupHelper` to truncate tables between tests, which is slow and error-prone if new tables are added.

### The Consequences
*   **Slow Feedback Loop:** Running the full suite takes too long due to serialized execution and cleanup overhead.
*   **Resource Heavy:** Running Keycloak requires significant memory.
*   **Bottleneck:** We cannot utilize the full power of our CPU cores because tests are waiting in line for the database.

### The Solution ("Hybrid Parallel" Strategy)
We will refactor to a hybrid approach:
1.  **Mock Authentication:** Replace Keycloak with a local "Token Factory". We trust that the Gateway handles the OAuth2 handshake correctly. The backend only needs to validate the JWT signature.
2.  **Parallel Persistence:** Instead of locking the DB, we will give each test thread its own **private database schema**.
    *   Thread 1 -> `test_schema_01`
    *   Thread 2 -> `test_schema_02`
3.  **Context Propagation:** Ensure the schema context flows from the Test Thread (JUnit) to the Server Thread (Tomcat) via HTTP Headers.

### 1.1 Design Constraints
The implementation must strictly adhere to the following constraints to ensure efficiency and reliability:
*   **Single JVM:** `forkCount=1` is required for memory efficiency.
*   **Context Caching:** The Spring `ApplicationContext` **must not** reload between test classes. This means avoiding `@DirtiesContext` and ensuring `@DynamicPropertySource` values remain static.
*   **Database runtime constraint:** Use a single shared PostgreSQL Testcontainer for the whole suite; isolation is logical (schema-per-test), not physical (multiple containers).
*   **Thread Disconnect:** Tests use **RestAssured** (`@SpringBootTest(webEnvironment = RANDOM_PORT)`). The Test Logic runs in the **JUnit Thread**, while the Controller Logic runs in a separate **Tomcat Thread**. This necessitates explicit context propagation.

Context caching guardrails (hard rules):
*   Do not use `@DirtiesContext`.
*   Do not use per-class/per-test `@DynamicPropertySource` values that vary between tests.
*   All per-test variability must be internal (schema routing), not via external config changes.

### 1.2 Scope & Boundary (Non-Goals)
This integration test suite validates the Backend in isolation as an OAuth2 Resource Server:
*   JWT signature validation and claim mapping
*   Authorization rules (roles/authorities) and authenticated identity extraction
*   Persistence behavior against PostgreSQL using real schema migrations

Non-goals (explicitly out of scope for this suite):
*   Gateway behavior (cookie handling, OAuth2 handshake, Token Relay correctness)
*   Keycloak/IdP availability and configuration correctness

Rationale: In production the Gateway performs the OAuth2 handshake and relays a JWT. These tests emulate the Gateway by injecting JWTs directly and focus on how the Backend consumes them.

---

## 2. Phase 0: Prerequisites & Configuration

### Step 2.1: Verify Dependencies
**Why:** Ensure we have the libraries to generate JWTs locally.
**Action:** Verify `backend/pom.xml` has `spring-security-oauth2-jose` (usually included in `spring-boot-starter-oauth2-resource-server`).

### Step 2.2: Disable Auto-Migration
**Why:** We want to control Flyway programmatically to migrate specific schemas, not the default `public` schema on startup.
**Action:** Update `backend/src/test/resources/application-integration.properties`.
**Changes:**
```properties
# Disable auto-migration on startup
spring.flyway.enabled=false
# Tune pool for parallel threads: (Parallel Threads * 2) + Buffer
spring.datasource.hikari.maximum-pool-size=20
```

### Step 2.3: Remove Legacy Keycloak Config
**Why:** We are replacing the containerized Keycloak with a mock.
**Action:** Delete:
*   `backend/src/test/java/com/example/demo/testsupport/KeycloakTestcontainerConfiguration.java`
*   `backend/src/test/java/com/example/demo/testsupport/KeycloakTokenProvider.java`
*   `backend/src/test/java/com/example/demo/testsupport/KeycloakTestSecurityConfig.java`
*   `backend/src/test/resources/application-keycloak-test.properties`

---

## 3. Phase 1: Authentication Layer (The "Virtual Gateway")

### Step 3.1: Create JWT Generation Utility
**Why:** Mint valid JWTs locally that match the Keycloak structure (same claims, issuer).
**Implementation:** `backend/src/test/java/com/example/demo/testsupport/auth/TestJwtUtils.java`
*   Use `nimbus-jose-jwt` to sign tokens with a fixed HS256 secret.
*   Token Contract: Generated JWTs must conform to ADR-003 (issuer + claim names + role structure), so production claim mapping is exercised and drift is detectable.
*   Claims: Ensure the generator includes the same “shape” the backend expects in production (e.g., `preferred_username`, roles in `realm_access.roles`, etc.).
*   Methods: `createToken(username, roles)`, `getUserToken()`, `getAdminToken()`.

### Step 3.2: Configure Mock Security
**Why:** Configure the backend to validate tokens using our test secret instead of calling Keycloak.
**Implementation:** `backend/src/test/java/com/example/demo/testsupport/auth/MockSecurityConfig.java`
*   Define a `JwtDecoder` bean configured with the same HS256 secret used in `TestJwtUtils`.
*   Annotate with `@TestConfiguration` and `@Profile("integration")`.

All test-only security wiring must be enabled only for the `integration` test profile to avoid leaking test behavior into other runs.

### Step 3.3: Refactor Secured Base Test
**Why:** `AbstractSecuredRestAssuredIT` currently relies on Keycloak classes we just deleted.
**Action:** Update `backend/src/test/java/com/example/demo/testsupport/AbstractSecuredRestAssuredIT.java`.
*   Remove `@Import(KeycloakTestcontainerConfiguration.class)`.
*   Remove `DynamicPropertySource` for Keycloak properties.
*   Update `getUserToken()` and `getAdminToken()` to use `TestJwtUtils`.
*   Import `MockSecurityConfig.class`.

### 3.4 Authorization Coverage Checklist (Must-Have Assertions)
The refactor is only complete if the suite still proves:
*   Role enforcement: a valid JWT without required roles is rejected (403).
*   Missing/invalid token handling: no token or invalid signature is rejected (401/403 as applicable to the endpoint).
*   Identity propagation: authenticated identity/claims are correctly mapped (e.g., auditing fields like “createdBy” or equivalent domain attribution).

---

## 4. Phase 2: Persistence Layer (Smart Schema)

### 4.0 Cleanup Model (Why Schema Dropping, Not Rollback)
Because tests use RestAssured with `RANDOM_PORT`, the test logic runs on the JUnit thread while request handling and DB interactions run on Tomcat worker threads. A transaction started in the test method does not wrap server-side work, so `@Transactional` rollback is not a reliable cleanup mechanism.
Isolation and cleanup are guaranteed only by per-test schema creation + `DROP SCHEMA … CASCADE`.

### 4.A Decision Options (Isolation Strategies)
This template prioritizes quality (real DB + real migrations + realistic Spring runtime behavior) and fast feedback (parallel execution without fragile cleanup). There are multiple viable isolation strategies:

#### Option 1 (Chosen): Single shared PostgreSQL container + schema-per-test
**How it works:** Start one `PostgreSQLContainer` once for the suite, create a unique schema per test, run migrations on that schema, route DB access to it, and `DROP SCHEMA … CASCADE` after the test.
**Why we choose it (for this template):**
*   Strong isolation with deterministic cleanup (no truncation drift when new tables are added).
*   Fast startup (container starts once), enabling parallelism within a single JVM (`forkCount=1`).
*   Compatible with Spring context caching constraints (no per-class datasource URL churn).
*   Good CI stability: parallelism uses threads, not many heavyweight containers.

**Costs / complexity:** Requires schema routing and schema context propagation across threads (JUnit → Tomcat), plus strict ThreadLocal hygiene.

#### Option 2: One PostgreSQL container per test class
**How it works:** Each test class starts its own Postgres container (and typically runs migrations once per class).
**Pros:**
*   Simpler mental model (no schema routing/filter/ThreadLocal complexity).
*   Very strong isolation; failures are easier to attribute to a class.
**Cons (why not chosen here):**
*   Container startup time and resource usage scale with the number of classes; parallel execution becomes memory/IO bound and can get flaky on typical CI agents.
*   Often pressures you into per-class dynamic datasource configuration, which can conflict with the “single cached ApplicationContext” constraint.

#### Option 3: One PostgreSQL container per test (method)
**How it works:** Every test method starts a fresh container.
**Pros:** Maximum isolation.
**Cons:** Usually too slow and too resource-intensive for an integration test suite beyond a handful of tests.

#### Option 4: Database-per-test-class (single Postgres instance, multiple databases)
**How it works:** One Postgres container, but create a new database per class (instead of a new schema), migrate it, drop it after.
**Pros:** Avoids schema routing while still using a single container.
**Cons:** Still requires some dynamic routing/config; database create/drop can be slower than schema create/drop depending on environment.

#### Guideline (when to reconsider)
If this template is used in a context with very few IT classes and maximum simplicity is preferred over runtime (or CI hardware is extremely strong), Option 2 can be a reasonable trade.
If Flyway-per-test becomes a bottleneck, keep Option 1 but introduce a faster migration strategy (see Risk 7.3).

### Step 4.1: Schema Context Holder
**Why:** Store the current schema name for the running thread.
**Implementation:** `backend/src/test/java/com/example/demo/testsupport/persistence/SchemaContext.java`
*   Simple `ThreadLocal<String>`.

### Step 4.2: Smart Routing DataSource
**Why:** Intercept JDBC calls and switch the PostgreSQL `search_path` to the current thread's schema.
**Implementation:** `backend/src/test/java/com/example/demo/testsupport/persistence/SmartRoutingDataSource.java`
*   Extend `DelegatingDataSource`.
*   Override `getConnection()`:
    ```java
    Connection conn = super.getConnection();
    String schema = SchemaContext.getSchema();
    if (schema != null) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET search_path TO " + schema);
        }
    }
    return conn;
    ```

### Step 4.3: DataSource Configuration
**Why:** Register the Smart Proxy as the primary DataSource, wrapping the auto-configured one.
**Implementation:** `backend/src/test/java/com/example/demo/testsupport/persistence/TestPersistenceConfig.java`
*   Annotate with `@TestConfiguration`.
*   Inject `DataSourceProperties` (populated by `@ServiceConnection`).
*   Define `@Bean @Primary DataSource`:
    ```java
    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        HikariDataSource hikari = properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
        return new SmartRoutingDataSource(hikari);
    }
    ```

### Step 4.4: JUnit Extension (The Orchestrator)
**Why:** Manage the schema lifecycle (Create -> Migrate -> Test -> Drop).
**Implementation:** `backend/src/test/java/com/example/demo/testsupport/persistence/SchemaIsolationExtension.java`
*   Implement `BeforeEachCallback`, `AfterEachCallback`.
*   **BeforeEach:**
    1.  Generate random schema name (`test_req_...`).
    2.  `CREATE SCHEMA` using `dataSource.getConnection()`.
    3.  `SchemaContext.setSchema()`.
    4.  Programmatically run `Flyway.configure().dataSource(ds).schemas(schemaName).locations(...).migrate()`.
*   **AfterEach:**
    1.  `DROP SCHEMA ... CASCADE`.
    2.  `SchemaContext.clear()`.

---

## 5. Phase 3: Cross-Thread Context Propagation

### Step 5.1: The Server-Side Filter
**Why:** RestAssured requests hit Tomcat on a different thread; in the `integration` test profile we must propagate schema context via an HTTP header.
**Implementation:** `backend/src/test/java/com/example/demo/testsupport/persistence/TestSchemaFilter.java`
*   Extend `OncePerRequestFilter`.
*   Read header `X-Test-Schema`.
*   If present, `SchemaContext.setSchema()`.
*   `finally { SchemaContext.clear() }`.

ThreadLocal hygiene is a non-negotiable invariant under parallel execution: schema context must be set for the duration of request handling and always cleared in a `finally` block to prevent pooled-thread pollution across concurrent tests.

### Step 5.2: Register the Filter
**Why:** Ensure the filter runs for every request in the `integration` test profile.
**Action:** Add to `TestPersistenceConfig.java`:
    ```java
    @Bean
    public FilterRegistrationBean<TestSchemaFilter> testSchemaFilter() {
        FilterRegistrationBean<TestSchemaFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TestSchemaFilter());
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }
    ```

### Step 5.3: Configure RestAssured (Client-Side)
**Why:** Automatically inject the header into all requests.
**Action:** Update `backend/src/test/java/com/example/demo/testsupport/AbstractRestAssuredIntegrationTest.java`.
*   In `@BeforeEach`, check `SchemaContext.getSchema()`.
*   If set, add `RequestSpecBuilder().addHeader("X-Test-Schema", schema)`.

### Step 5.4: Async Context Propagation (TaskDecorator)
**Why:** Spring Modulith event listeners run asynchronously (`@Async`). These background threads do not inherit the `ThreadLocal` schema context, causing database writes (like Audit Logs) to fail or leak into the default schema.
**Implementation:** `backend/src/test/java/com/example/demo/testsupport/persistence/SchemaContextTaskDecorator.java`
*   Implement `org.springframework.core.task.TaskDecorator`.
*   Capture `SchemaContext.getSchema()` in the `decorate` method.
*   Return a wrapper `Runnable` that sets the schema before execution and clears it after.
**Action:** Register in `TestPersistenceConfig.java`:
    ```java
    @Bean
    public TaskDecorator schemaContextTaskDecorator() {
        return new SchemaContextTaskDecorator();
    }

    // Override default TaskExecutor to use the decorator
    @Bean(name = "applicationTaskExecutor")
    public ThreadPoolTaskExecutor applicationTaskExecutor(TaskDecorator decorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(decorator);
        executor.setCorePoolSize(5); // Tune for test concurrency
        return executor;
    }
    ```

---

## 6. Phase 4: Final Integration & Cleanup

### Step 6.1: Update Base Integration Test
**Action:** Update `backend/src/test/java/com/example/demo/testsupport/AbstractIntegrationTest.java`.
*   Add `@ExtendWith(SchemaIsolationExtension.class)`.
*   Add `@Import({TestPersistenceConfig.class, MockSecurityConfig.class})`.

### Step 6.2: Enable Parallel Execution
**Action:** Verify `backend/src/test/resources/junit-platform.properties`.
*   `junit.jupiter.execution.parallel.enabled=true`
*   `junit.jupiter.execution.parallel.mode.classes.default=concurrent`
*   Keep methods in the same thread by default to preserve multi-step scenario consistency within a class.
*   Set a deterministic parallelism strategy (fixed number or CPU-based) to avoid environment-dependent behavior across CI agents.

### Step 6.3: Refactor Existing Tests (Remove Locking & Manual Cleanup)
**Why:** `SchemaIsolationExtension` handles isolation and cleanup now.
**Action:**
1.  **Remove** `@ResourceLock(value = "DB", mode = READ_WRITE)`
2.  **Remove** `DatabaseCleanupHelper` injection and usage (`@BeforeEach`/`@AfterEach`).

**Files to Update:**
*   `backend/src/test/java/com/example/demo/audit/BusinessActivityIT.java`
*   `backend/src/test/java/com/example/demo/greeting/controller/GreetingControllerIT.java`
*   `backend/src/test/java/com/example/demo/greeting/controller/GreetingControllerSecuredIT.java`
*   `backend/src/test/java/com/example/demo/greeting/repository/GreetingRepositoryIT.java`
*   `backend/src/test/java/com/example/demo/greeting/service/GreetingServiceIT.java`
*   `backend/src/test/java/com/example/demo/user/controller/UserControllerSecuredIT.java`

### 6.X Test Data Lifecycle (Expected Pattern)
*   Seeding: Create test data via HTTP calls (RestAssured) or repositories within the test method as needed.
*   Migration: Flyway runs for each schema, continuously validating migrations.
*   Cleanup: Cleanup is implicit via `DROP SCHEMA … CASCADE`; table truncation helpers are retired.

### Step 6.4: Remove Legacy Cleanup Helper
**Action:** Delete `backend/src/test/java/com/example/demo/testsupport/DatabaseCleanupHelper.java`.

### Step 6.5: Verification
*   Run `mvn verify` to ensure all tests pass.
*   Check logs to ensure Schemas are being created and dropped.
*   Verify execution time reduction.

---

## 7. Risks & Mitigations

### 7.1 Database Objects Definition
**Risk:** The `SchemaIsolationExtension` runs Flyway migrations on the new schema. If any database objects (like `seq_greeting_reference` in `src/test/resources/schema.sql`) are defined outside of Flyway (e.g., in `schema.sql`), they will **not** be created in the test schemas, causing tests to fail.
**Mitigation:** Ensure all required database objects (tables, sequences, views) are defined in Flyway migration scripts. If `schema.sql` is required, update `SchemaIsolationExtension` to execute it manually after Flyway migration.

### 7.2 Mock Token Drift
Risk: Locally generated JWTs can drift from the real IdP/Gateway token shape over time (claims, issuer, roles), producing false confidence.
Mitigation: Add a small nightly contract test suite that runs with the real Keycloak container to validate the JWT factory remains compatible with production token expectations.

### 7.3 Flyway Overhead per Test Schema
Risk: Running Flyway migrations for every test schema can become a dominant cost as migrations grow.
Mitigation: If per-test migration time becomes a bottleneck, consider a template-based approach (pre-migrated template database/schema snapshot) to amortize migration cost while preserving isolation.

### 7.4 Async Execution and Schema Context
Risk: Async work (`@Async`, CompletableFutures, modulith events) does not automatically inherit ThreadLocal schema context, causing writes to fail or leak into the default schema under parallel tests.
Mitigation: Use a TaskDecorator (or executor decoration) to propagate schema context for any async DB access.

---

## 8. References

1.  **Spring Cloud Gateway Token Relay:**
    *   [Spring Cloud Gateway: The Token Relay GatewayFilter Factory](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway/developer-guide.html#the-tokenrelay-gatewayfilter-factory)
    *   Justification for mocking: The backend relies on the Gateway for OAuth2 handshakes; mimicking this via direct token injection is a valid testing strategy.

2.  **Spring Context Caching:**
    *   [Spring Framework: Parallel Test Execution](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/parallel-test-execution.html)
    *   Rationale for avoiding `@DirtiesContext`: Maintaining a singleton context is crucial for performance.

3.  **Multi-Tenancy / Schema Switching:**
    *   [AbstractRoutingDataSource API](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/datasource/lookup/AbstractRoutingDataSource.html)
    *   [Multi-Tenancy in Spring Boot](https://spring.io/guides/gs/multi-tenancy/) (General concept adaptation)
    *   This supports the "Smart Routing DataSource" design for schema-per-thread isolation.

4.  **Spring Security Testing:**
    *   [Spring Security Reference: Testing JWTs](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/oauth2.html#testing-jwt)
    *   Standardizes the approach of using `JwtEncoder` or similar libraries to mint test tokens.

5.  **Context Propagation:**
    *   [Spring Framework: TaskDecorator](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/task/TaskDecorator.html)
    *   Essential for propagating `ThreadLocal` context to async threads in Spring Modulith.

6.  **HikariCP Pool Sizing:**
    *   [HikariCP: About Pool Sizing](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)
    *   Mathematical basis for the connection pool tuning formula used in the plan.
