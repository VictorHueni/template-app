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

Additional Spring TestContext cache hygiene rules (non-negotiable):
*   **Profiles must be consistent** across all integration tests in this suite. Avoid mixing profile sets like `{"test", "integration"}` vs `{"test", "keycloak-test"}` unless you accept the cost of separate cached contexts.
*   **Do not vary the web environment** for suite members (this plan assumes `@SpringBootTest(webEnvironment = RANDOM_PORT)` for RestAssured-based ITs). Mixing non-web and web tests in the same “IT suite” creates separate contexts.
*   **Avoid `@MockBean` / `@SpyBean`** in `@SpringBootTest` integration tests. They change the context cache key and force new ApplicationContexts.
*   **Avoid per-class property overrides** like `@SpringBootTest(properties = ...)` or `@TestPropertySource` unless the override is identical for a whole tier of tests.
*   **Prefer shared test configurations** (imported consistently from a single base class) over ad-hoc `@Import(...)` per test class.
*   **No test should introduce new global mutable state** (static singletons, static caches) unless it is explicitly reset and proven parallel-safe.

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

## Core Stack 
- Spring Boot: 4.0.1
- Spring Security: 7.0.2 (transitive via starter)
- Java: 25

## 2. Phase 0: Prerequisites & Configuration

### Step 2.1: Verify Dependencies
**Why:** Ensure we have the libraries to generate JWTs locally.
**Action:** Verify `backend/pom.xml` has `spring-security-oauth2-jose` (usually included in `spring-boot-starter-oauth2-resource-server`).

**Test:**
1. Open `backend/pom.xml` and search for `spring-boot-starter-oauth2-resource-server`.
2. Verify the dependency is present in the `<dependencies>` section.
3. Run `./mvnw dependency:tree | grep oauth2` to confirm transitive dependencies include `spring-security-oauth2-jose`.
4. Expected output: The tree should list `com.nimbusds:nimbus-jose-jwt` as a transitive dependency.


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

**Test:**
1. Verify the file `backend/src/test/resources/application-integration.properties` exists and contains both properties.
2. Run a quick test with profile `integration`: `./mvnw test -Dspring.profiles.active=integration`.
3. Check the startup logs—you should **NOT** see "Flyway migrating" messages for the public schema on startup.
4. Verify pool configuration is loaded by searching logs for `maximum-pool-size=20`.

### Step 2.3: Remove Legacy Keycloak Config
**Why:** We are replacing the containerized Keycloak with a mock.
**Action:** Delete:
*   `backend/src/test/java/com/example/demo/testsupport/KeycloakTestcontainerConfiguration.java`
*   `backend/src/test/java/com/example/demo/testsupport/KeycloakTokenProvider.java`
*   `backend/src/test/java/com/example/demo/testsupport/KeycloakTestSecurityConfig.java`
*   `backend/src/test/resources/application-keycloak-test.properties`

**Test:**
1. Execute the delete commands (or use IDE to remove files).
2. Run `./mvnw clean compile` to verify no compilation errors referring to these classes.
3. Search codebase for imports of the deleted classes: `grep -r "KeycloakTestcontainerConfiguration" backend/src/`.
4. Expected result: No search results (all references have been removed).

### Step 2.4: Final Steps (Commit Prerequisites & Configuration)

After completing all Phase 0 steps and validating each one, execute these final steps to commit your changes:

**Final Validation:**
1. Run `./mvnw clean compile` one final time to ensure no lingering compilation errors.
2. Verify all configuration files exist and are valid:
   ```bash
   ./mvnw test -Dspring.profiles.active=integration -DfailIfNoTests=false
   ```
3. Confirm all legacy authentication container files have been removed:
   ```bash
   find backend/src/test -name "*Keycloak*" -type f
   # Expected: No results
   ```

**Commit Changes:**
```bash
# Stage configuration changes and removed files
git status  # Review what has changed
git add -A  # Or selectively: git add backend/pom.xml backend/src/test/resources/
git rm <legacy_files>  # Remove any old auth testcontainer files

git commit -m "<commit message>"
```
---

## 3. Phase 1: Authentication Layer (The "Virtual Gateway")

### Step 3.1: Create JWT Generation Utility
**Why:** Mint valid JWTs locally that match the Keycloak structure (same claims, issuer).
**Implementation:** `backend/src/test/java/com/example/demo/testsupport/auth/TestJwtUtils.java`
*   Use `nimbus-jose-jwt` to sign tokens with a fixed HS256 secret.
*   Token Contract: Generated JWTs must conform to ADR-003 (issuer + claim names + role structure), so production claim mapping is exercised and drift is detectable.
*   Claims: Ensure the generator includes the same “shape” the backend expects in production (e.g., `preferred_username`, roles in `realm_access.roles`, etc.).
*   Methods: `createToken(username, roles)`, `getUserToken()`, `getAdminToken()`.
**Test:**
1. Create a simple unit test in `backend/src/test/java/com/example/demo/testsupport/auth/TestJwtUtilsTest.java`.
2. Verify `TestJwtUtils.getUserToken()` generates a valid JWT string (not null, not empty).
3. Decode the JWT manually: `JWT.parse(token)` and verify it contains the expected claims:
   - `preferred_username` claim is present and equals expected value
   - `realm_access.roles` array contains `["USER"]`
   - `iss` (issuer) matches the configured test issuer
4. Verify `TestJwtUtils.getAdminToken()` includes `["USER", "ADMIN"]` in roles.
5. Expected outcome: All three token types are generated and claims are verifiable without external dependencies.

### Step 3.2: Configure Mock Security
**Why:** Configure the backend to validate tokens using our test secret instead of calling Keycloak.
**Implementation:**
*   `backend/src/test/java/com/example/demo/testsupport/auth/MockJwtBeansConfig.java`
   *   Defines the `JwtDecoder` + `JwtAuthenticationConverter` using the same HS256 secret as `TestJwtUtils`.
*   `backend/src/test/java/com/example/demo/testsupport/auth/MockSecurityConfig.java`
   *   Defines the `SecurityFilterChain` for the `integration` profile and imports `MockJwtBeansConfig`.
   *   Annotate with `@TestConfiguration` and `@Profile("integration")`.

All test-only security wiring must be enabled only for the `integration` test profile to avoid leaking test behavior into other runs.

**Timing note (important):** In this strategy, Flyway migrations will be run **programmatically** later (Phase 2) for per-test schemas. Until that is implemented, do not rely on `@SpringBootTest` with the `integration` profile if it triggers DB schema validation.

**Test:**
1. Add a lightweight context test that does **not** boot JPA/DB:
   *   Use `ApplicationContextRunner` (or similar) to load `MockJwtBeansConfig` with `spring.profiles.active=integration`.
2. Retrieve the `JwtDecoder` bean from the context and decode a token from `TestJwtUtils.getUserToken()`.
3. Verify no exceptions are thrown and the decoded token contains the expected claims (issuer + `preferred_username`).
4. Expected outcome: The `JwtDecoder` validates locally signed JWTs with the exact production claim shape.

### Step 3.3: Refactor Secured Base Test
**Why:** `AbstractSecuredRestAssuredIT` currently relies on Keycloak classes we just deleted.
**Action:** Update `backend/src/test/java/com/example/demo/testsupport/AbstractSecuredRestAssuredIT.java`.
*   Remove `@Import(KeycloakTestcontainerConfiguration.class)`.
*   Remove `DynamicPropertySource` for Keycloak properties.
*   Update `getUserToken()` and `getAdminToken()` to use `TestJwtUtils`.
*   Remove any leftover Keycloak-related imports/helpers.

**Best-practice note (test tiering):** Avoid permanently splitting tests into “SecuredIT vs IT”. Prefer one integration-test tier where security is always available and each test simply decides whether to send an `Authorization` header.

**When to activate full web-security tests:** Only after Phase 2 (schema-per-test + programmatic Flyway migrations) is implemented, so `@SpringBootTest(webEnvironment = RANDOM_PORT)` can start reliably with the `integration` profile.

**Test:**
1. Compile/test the JWT token plumbing without starting the full web context:
   *   Run unit/context-level tests (e.g., `*JwtUtils*`, mock security context tests).
2. Verify secured IT code compiles without references to removed Keycloak classes.
3. After Phase 2 is done, run a secured integration test that extends `AbstractSecuredRestAssuredIT` (e.g., `GreetingControllerSecuredIT`) with `@ActiveProfiles({"test", "integration"})`.
4. Execute a protected endpoint with `RestAssured.given().header("Authorization", "Bearer " + getUserToken())`.
5. Expected outcome: Secured tests pass using the new JWT utility and no longer depend on Keycloak containers.

### Steps 3.4 : Authorization Coverage Checklist (Must-Have Assertions)
The refactor is only complete if the suite still proves:
*   Role enforcement: a valid JWT without required roles is rejected (403).
*   Missing/invalid token handling:
   *   Missing/invalid token -> 401 (authentication failure)
   *   Valid token but insufficient role -> 403 (authorization failure)
*   Identity propagation: authenticated identity/claims are correctly mapped (e.g., auditing fields like “createdBy” or equivalent domain attribution).

**Best-practice alignment (confirmed):** Enforce authentication at the filter-chain level (so missing/invalid token yields 401) and enforce roles/authorities at method security (403).

**Role-enforcement anchor (Option A):** Introduce at least one admin-only operation to make the “insufficient role -> 403” scenario real. For this template we use:
- `DELETE /api/v1/greetings/{id}` -> ADMIN only

**Test:**
1. For each secured endpoint, write three scenarios:
   - **Authorized:** Request with valid token of required role → Expect 200.
   - **Insufficient Role:** Request with valid token but missing required role (e.g., `getUserToken()` on admin-only endpoint) → Expect 403.
   - **Missing/Invalid Token:** Request with no token or invalid signature → Expect 401.
2. Validate claim mapping without DB (Phase 1 friendly):
   - Decode `TestJwtUtils.getUserToken()` and ensure `preferred_username` exists.
   - Convert JWT to Authentication and ensure:
     - principal name is `preferred_username`
     - `realm_access.roles` becomes `ROLE_*` authorities.
3. For endpoints that audit createdBy/modifiedBy (after Phase 2), verify identity propagation:
   - Create a resource with `getUserToken()` and verify the audit field equals the user from the token (`preferred_username`).
4. After Phase 2 is implemented (programmatic Flyway per test schema), run the complete test suite: `./mvnw verify -Dspring.profiles.active=integration`.
5. Expected outcome: All three scenarios pass for each secured endpoint, proving role enforcement and claim mapping work correctly.

### Steps 3.5 : Final Steps (Commit Authentication Layer)

After completing all Phase 1 steps and validating each one, execute these final steps to commit your changes:

**Final Validation:**
1. Test JWT utilities work correctly:
   - Linux/Mac: `./mvnw test -Dtest='*JwtUtils*' -pl backend`
   - Windows: `mvn.cmd test "-Dtest=*JwtUtils*"` (run from `backend/`)
2. Test the no-DB mock security plumbing:
   - Windows: `mvn.cmd test "-Dtest=TestJwtUtilsTest,MockSecurityConfigContextTest"` (run from `backend/`)
3. (After Phase 2) Test secured endpoints work with mock tokens:
   - `./mvnw test -Dtest='*Secured*' -pl backend`
4. Verify no Keycloak references remain in test codebase:
   - Linux/Mac: `grep -r "Keycloak" backend/src/test/ || echo "✓ Clean"`
   - Windows PowerShell: `Get-ChildItem -Recurse backend\src\test | Select-String -Pattern "Keycloak"`
5. Verify mock security config is profile-gated:
   - Linux/Mac: `grep -r "@Profile(\"integration\"" backend/src/test/ | grep -i "security\|config"`
   - Windows PowerShell: `Get-ChildItem -Recurse backend\src\test\java | Select-String -Pattern "@Profile\(\"integration\""`

**Commit Changes:**
```bash
git status  # Review authentication layer changes
git add -A  # Or selectively add auth test infrastructure files

git commit -m "refactor(test): <commit message>
"
```

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
*   Simple `ThreadLocal<String>` (intentionally **not** `InheritableThreadLocal`).
*   Utility class (`final`, private constructor) with:
   *   `setSchema(String)` (rejects null/blank)
   *   `getSchema()` (nullable)
   *   `requireSchema()` (throws if unset, useful for fail-fast in later steps)
   *   `clear()` uses `ThreadLocal.remove()` to avoid pooled-thread pollution under parallel test execution.

**Test:**
1. Create a unit test: `backend/src/test/java/com/example/demo/testsupport/persistence/SchemaContextTest.java`.
2. Verify `SchemaContext.setSchema("test_schema_1")` and `SchemaContext.getSchema()` returns the same value.
3. In a new thread, verify `SchemaContext.getSchema()` returns null (proving ThreadLocal isolation).
4. Verify `SchemaContext.clear()` clears the value and subsequent `getSchema()` returns null.
5. Verify `SchemaContext.requireSchema()` throws when unset.
6. Verify `SchemaContext.setSchema("  ")` throws (blank schema is rejected).
7. Add `@AfterEach SchemaContext.clear()` to enforce ThreadLocal hygiene (JUnit reuses threads when running classes in parallel).
8. Expected outcome: ThreadLocal correctly isolates schema per thread and supports set/get/clear operations without cross-test leakage.

### Step 4.2: Smart Routing DataSource
**Why:** Intercept JDBC calls and switch the PostgreSQL `search_path` to the current thread's schema.
**Implementation:** `backend/src/test/java/com/example/demo/testsupport/persistence/SmartRoutingDataSource.java`
*   Extend `DelegatingDataSource`.
*   Override `getConnection()` and `getConnection(username, password)`.
*   Important: `search_path` is **session state** in PostgreSQL. With pooling, connections are reused, so we must prevent schema leakage.
   *   On checkout, apply `SET search_path TO "<schema>", public` when `SchemaContext` is set.
   *   When `SchemaContext` is unset, defensively `RESET search_path`.
   *   Return a proxied `Connection` that runs `RESET search_path` on `close()` before returning to the pool.
   *   Quote (or strictly validate) schema identifiers to avoid SQL injection / invalid identifiers.
*   Example (simplified):
    ```java
    Connection conn = super.getConnection();
    String schema = SchemaContext.getSchema();
   if (schema == null) {
      try (Statement stmt = conn.createStatement()) {
         stmt.execute("RESET search_path");
      }
      return conn;
   }
   try (Statement stmt = conn.createStatement()) {
      stmt.execute("SET search_path TO \"" + schema + "\", public");
   }
   return proxiedConnThatResetsOnClose(conn);
    ```

**Test:**
1. Create a Testcontainers-based integration test: `backend/src/test/java/com/example/demo/testsupport/persistence/SmartRoutingDataSourceIT.java`.
2. Configure the underlying pool with `maximumPoolSize=1` to force physical connection reuse (this detects leakage reliably).
3. Create two schemas (`test_schema_1`, `test_schema_2`).
4. Set `SchemaContext.setSchema("test_schema_1")` and verify `SELECT current_schema();` returns `test_schema_1`.
5. Set `SchemaContext.setSchema("test_schema_2")` and verify `SELECT current_schema();` returns `test_schema_2`.
6. Clear the context and verify a new borrow returns to the default schema (typically `public`).
7. Expected outcome: Connections are routed to the correct schema based on ThreadLocal context and do not leak schema state across pooled connections.

### Step 4.3: DataSource Configuration
**Why:** Register the Smart Proxy as the primary DataSource, wrapping the auto-configured one.
**Implementation:** `backend/src/test/java/com/example/demo/testsupport/persistence/TestPersistenceConfig.java`
*   Annotate with `@TestConfiguration`.
*   Use a `BeanPostProcessor` to wrap the auto-configured DataSource (instead of defining a new bean):
    ```java
    @Bean
    static BeanPostProcessor dataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource ds && !(bean instanceof SmartRoutingDataSource)) {
                    return new SmartRoutingDataSource(ds);
                }
                return bean;
            }
        };
    }
    ```
*   **Rationale for BeanPostProcessor approach:**
    *   Preserves the auto-configured DataSource (including `@ServiceConnection` from Testcontainers).
    *   Avoids duplicate bean conflicts with Spring Boot's DataSource auto-configuration.
    *   Works seamlessly with HikariCP pooling and any Spring Boot DataSource properties.

**Test:** `backend/src/test/java/com/example/demo/testsupport/persistence/TestPersistenceConfigIT.java`

> ⚠️ **Critical: Spring Modulith Exclusions Required**
>
> When testing `TestPersistenceConfig` in isolation (minimal context with only DataSource/JDBC),
> Spring Modulith's auto-configurations must be excluded. These classes expect a `@SpringBootApplication`-annotated
> class for module discovery. Since minimal test contexts use `@SpringBootConfiguration` instead,
> exclude them via `spring.autoconfigure.exclude` property (required because some classes are package-private):
>
> ```java
> @SpringBootTest(
>     classes = { TestPersistenceConfigIT.TestApp.class, TestPersistenceConfig.class },
>     properties = {
>         "spring.main.web-application-type=none",
>         "spring.autoconfigure.exclude=" +
>             "com.c4_soft.springaddons.security.oidc.starter.synchronised.resourceserver.SpringAddonsOidcResourceServerBeans," +
>             "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
>             "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration," +
>             "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration," +
>             "org.springframework.modulith.runtime.autoconfigure.SpringModulithRuntimeAutoConfiguration," +
>             "org.springframework.modulith.observability.autoconfigure.ModuleObservabilityAutoConfiguration," +
>             "org.springframework.modulith.events.jpa.autoconfigure.JpaEventPublicationAutoConfiguration," +
>             "org.springframework.modulith.events.autoconfigure.EventPublicationAutoConfiguration," +
>             "org.springframework.modulith.actuator.autoconfigure.ApplicationModulesEndpointConfiguration," +
>             "org.springframework.modulith.runtime.autoconfigure.ApplicationModuleInitializerRuntimeVerification"
>     }
> )
> ```

1. Create `TestPersistenceConfigIT.java` with an inner `@SpringBootConfiguration` class (not `@SpringBootApplication`).
2. Exclude security and Spring Modulith auto-configurations as shown above.
3. Configure `@Testcontainers` with `PostgreSQLContainer` and `@ServiceConnection`.
4. Inject `DataSource` in the test: `@Autowired DataSource dataSource`.
5. Verify the bean is an instance of `SmartRoutingDataSource`: `assertThat(dataSource).isInstanceOf(SmartRoutingDataSource.class)`.
6. Test schema routing:
   *   Set `SchemaContext.setSchema("test_schema_demo")`.
   *   Get a connection and execute `SELECT current_schema()`.
   *   Verify it returns `test_schema_demo`.
7. Expected outcome: DataSource bean is correctly wrapped by BeanPostProcessor and routes connections based on SchemaContext.

### Step 4.4: JUnit Extension (The Orchestrator)
**Why:** Manage the schema lifecycle (Create -> Migrate -> Test -> Drop).
**Implementation:** `backend/src/test/java/com/example/demo/testsupport/persistence/SchemaIsolationExtension.java`
*   Implement `BeforeEachCallback`, `AfterEachCallback`.
*   **BeforeEach:**
   1.  Clear `SchemaContext` (defensive hygiene).
   2.  Generate random schema name (`test_req_...`) using a UUID (lowercase, no hyphens).
   3.  Store schema name in `ExtensionContext.Store` (so `AfterEach` can drop it).
   4.  Obtain `DataSource` from the Spring test context via `SpringExtension.getApplicationContext(ctx)`.
   5.  `CREATE SCHEMA "<schema>"` using `dataSource.getConnection()` (with `SchemaContext` still unset).
   6.  `SchemaContext.setSchema(schemaName)`.
   7.  Programmatically run Flyway for that schema:
      * `Flyway.configure().dataSource(ds)`
      * `.schemas(schemaName).defaultSchema(schemaName)`
      * `.locations("classpath:db/migration")`
      * `.migrate()`
*   **AfterEach:**
   1.  Read schema name from `ExtensionContext.Store`.
   2.  `SchemaContext.clear()`.
   3.  `DROP SCHEMA "<schema>" CASCADE`.

Implementation note: the extension logs `Creating test schema ...`, `Running Flyway migrations ...`, and `Dropping test schema ...` so schema lifecycle can be verified in test output.

**Test:**
1. Create an integration test: `backend/src/test/java/com/example/demo/testsupport/persistence/SchemaIsolationExtensionIT.java`.
2. Use `@SpringBootTest` + `@ExtendWith(SchemaIsolationExtension.class)`.
3. In the test method, verify `SchemaContext.getSchema()` is NOT blank and starts with `test_req_`.
4. Query the database: `select current_schema()` and verify it equals `SchemaContext.getSchema()`.
5. Verify Flyway ran by checking `flyway_schema_history` exists in the current schema.
6. Verify schema drop via logs (look for `Dropping test schema ... CASCADE`).
7. Expected outcome: Extension creates a unique schema, runs migrations in it, and cleans up after each test.

### Steps 4.5: Final Steps (Commit Persistence Layer)

After completing all Phase 2 steps and validating each one, execute these final steps to commit your changes:

**Final Validation:**
1. Test SchemaContext isolation: `./mvnw test -Dtest=SchemaContextTest`.
2. Test SmartRoutingDataSource: `./mvnw test -Dtest='*DataSourceTest'`.
3. Test SchemaIsolationExtension: `./mvnw test -Dtest=SchemaIsolationExtensionIT` to see schema creation/migrations/drops in logs.
4. Verify all components compile without errors:
   ```bash
   ./mvnw clean compile -pl backend -DskipTests
   ```
5. Verify no schema pollution across test runs:
   ```bash
   ./mvnw test -Dtest=SampleIT -DfailIfNoTests=false
   ./mvnw test -Dtest=SampleIT -DfailIfNoTests=false
   # Run twice; second run should show different schema names
   ```

---

## 5. Phase 3: Cross-Thread Context Propagation

**Convergence (do later, after Phase 2):** Once `@SpringBootTest` can reliably run with `@ActiveProfiles({"test", "integration"})`, converge the test tiers so there is only one IT per component:
1. Rename `*SecuredIT.java` classes to `*IT.java` (or merge secured scenarios into the existing `*IT` for that component).
2. Keep security enabled for the whole IT tier and vary behavior per request:
   * Unauthenticated requests -> no `Authorization` header
   * Authenticated requests -> `Authorization: Bearer <token>`
3. Make profiles consistent across all RestAssured ITs in the suite:
   * Use the same `@ActiveProfiles({"test", "integration"})` everywhere (avoid mixed profile sets that split the Spring TestContext cache).
4. Keep helper methods (`givenAuthenticatedUser()`, `givenAuthenticatedAdmin()`, `givenUnauthenticated()`) in a single shared base class.
5. Run a suite spot-check after convergence:
   * `./mvnw test -Dtest='*ControllerIT' -pl backend`
   * `./mvnw verify -Dspring.profiles.active=integration -pl backend`


### Step 5.1: The Server-Side Filter
**Why:** RestAssured requests hit Tomcat on a different thread; in the `integration` test profile we must propagate schema context via an HTTP header.
**Implementation:** `backend/src/test/java/com/example/demo/testsupport/persistence/TestSchemaFilter.java`
*   Extend `OncePerRequestFilter`.
*   Read header `X-Test-Schema`.
*   If present, `SchemaContext.setSchema()`.
*   `finally { SchemaContext.clear() }`.

ThreadLocal hygiene is a non-negotiable invariant under parallel execution: schema context must be set for the duration of request handling and always cleared in a `finally` block to prevent pooled-thread pollution across concurrent tests.

**Test:**
1. Run an integration test using RestAssured with a schema-specific header: `RestAssured.given().header("X-Test-Schema", "test_schema_abc").get("/api/v1/greetings")`.
2. Inside your test, also inject the repository or a query endpoint that verifies data is stored in the correct schema.
3. Verify the request succeeds (200 status).
4. In a second test in the same class with a different schema, repeat the process with a different `X-Test-Schema` value.
5. Verify data isolation: each schema only sees data written in its own schema.
6. Expected outcome: Filter correctly reads the header and routes each request to its respective schema without cross-contamination.

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

**Test:**
1. Run an integration test and inject `FilterRegistrationBean` or verify via Spring's filter registry that `TestSchemaFilter` is registered.
2. Make a RestAssured request without the `X-Test-Schema` header and verify it succeeds (filter handles missing header gracefully).
3. Make another request with a valid header and verify it succeeds.
4. Verify filter is invoked by adding a breakpoint or logging statement and confirming it appears in test logs.
5. Expected outcome: Filter is successfully registered with highest precedence and executes for all requests.

### Step 5.3: Configure RestAssured (Client-Side)
**Why:** Automatically inject the header into all requests.
**Action:** Update `backend/src/test/java/com/example/demo/testsupport/AbstractRestAssuredIntegrationTest.java`.
*   In `@BeforeEach`, check `SchemaContext.getSchema()`.
*   If set, add `RequestSpecBuilder().addHeader("X-Test-Schema", schema)`.

**Test:**
1. Create a test that extends `AbstractRestAssuredIntegrationTest`.
2. Verify that `SchemaContext.getSchema()` is set (from `SchemaIsolationExtension`).
3. Make a RestAssured request: `RestAssured.get("/api/v1/greetings")`.
4. Verify the request succeeds (no 4xx errors indicating missing header).
5. Optionally, add logging to intercept the HTTP request and verify the `X-Test-Schema` header is automatically included.
6. Expected outcome: Header is automatically injected into all RestAssured requests without explicit code in each test.

### Step 5.4: Async Context Propagation (TaskDecorator)
**Why:** Spring Modulith event listeners run asynchronously (`@Async`). These background threads do not inherit the `ThreadLocal` schema context, causing database writes (like Audit Logs) to fail or leak into the default schema.
**Implementation:** `backend/src/test/java/com/example/demo/testsupport/persistence/SchemaContextTaskDecorator.java`
*   Implement `org.springframework.core.task.TaskDecorator`.
*   Capture `SchemaContext.getSchema()` in the `decorate` method.
*   Return a wrapper `Runnable` that sets the schema before execution and clears it after.
*   Add DEBUG-level logging to trace context propagation for troubleshooting.

**Action:** Register in `TestPersistenceConfig.java`:
    ```java
    @Bean
    public TaskDecorator schemaContextTaskDecorator() {
        return new SchemaContextTaskDecorator();
    }

    // Override default TaskExecutor to use the decorator
    // CRITICAL: @Primary is REQUIRED for Spring Modulith integration
    @Bean(name = "applicationTaskExecutor")
    @Primary
    public ThreadPoolTaskExecutor applicationTaskExecutor(TaskDecorator decorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(decorator);
        executor.setCorePoolSize(5); // Tune for test concurrency
        return executor;
    }
    ```

> ⚠️ **Critical: Spring Modulith 2.x Integration**
>
> Spring Modulith 2.0+ uses `@ApplicationModuleListener` which is a meta-annotation combining:
> - `@Async` (uses bean named `applicationTaskExecutor` by default)
> - `@Transactional(propagation = REQUIRES_NEW)`
> - `@TransactionalEventListener`
>
> For the `TaskDecorator` to apply to Spring Modulith event listeners, two requirements must be met:
>
> 1. **`@EnableAsync` on TestPersistenceConfig**: Required to activate async processing and ensure
>    Spring picks up our custom executor configuration.
>
> 2. **`@Primary` on applicationTaskExecutor bean**: Without `@Primary`, Spring Modulith may use
>    its own auto-configured executor that doesn't have our `TaskDecorator`, causing schema context
>    to be lost in async event handlers (resulting in writes to wrong schema or test failures).
>
> **Verification**: Add DEBUG logging to `SchemaContextTaskDecorator` to confirm schema context
> is being captured and propagated:
> ```java
> log.debug("Capturing schema context for async propagation: {}", schema);
> log.debug("Executing async task with schema context: {}", capturedSchema);
> log.debug("Cleared schema context after async task completion");
> ```

**Test:**
1. Create an integration test that triggers an async operation (e.g., a `@Async` method or modulith event listener that writes to the database).
2. Verify `SchemaContext.getSchema()` is set before the async call.
3. Call the async method and allow it to complete (use `CompletableFuture` or `latch.await()`).
4. Query the database to verify the async work was executed in the correct schema (not the default schema).
5. Run multiple tests in parallel to ensure schema context is correctly isolated across threads.
6. Expected outcome: Async operations inherit and execute within the correct schema context without leaking into other tests' schemas.

### Steps 5.5 Final Steps (Commit Cross-Thread Context Propagation)

After completing all Phase 3 steps and validating each one, execute these final steps to commit your changes:

**Final Validation:**
1. Test HTTP header filtering: `./mvnw test -Dtest='*Filter*' -pl backend`.
2. Test async context propagation: `./mvnw test -Dtest='*Async*,*Event*' -pl backend`.
3. Verify ThreadLocal hygiene across all tests:
   ```bash
   ./mvnw test -Dtest='*IT' -pl backend
   # No context leaks or schema mismatches
   ```
4. Test concurrent requests to same endpoint (different schemas):
   ```bash
   ./mvnw test -Dtest='*Parallel*' -pl backend
   ```

**Commit Changes:**
```bash
git status  # Review context propagation changes
git add -A  # Or selectively add filter and decorator files

git commit -m "refactor(test): <commit message>
"
```


1. Run full suite with parallel execution to confirm no cross-test data pollution:
   ```bash
   ./mvnw verify -Dspring.profiles.active=integration -pl backend
   ```
2. Document in ticket/PR: "Phase 3 context propagation layer complete. Async operations now respect schema isolation."

---

## 6. Phase 4: Final Integration & Cleanup

### Step 6.1: Update Base Integration Test
**Action:** Update `backend/src/test/java/com/example/demo/testsupport/AbstractIntegrationTest.java`.
*   Add `@ExtendWith(SchemaIsolationExtension.class)`.
*   Add `@Import({TestPersistenceConfig.class, MockSecurityConfig.class})`.

**Test:**
1. Verify the base class has both annotations in place.
2. Create a simple test that extends `AbstractIntegrationTest`.
3. Run `./mvnw test -Dtest=YourNewTest` and confirm it compiles and runs.
4. Verify in logs that a test schema was created (look for `CREATE SCHEMA test_req_...`).
5. Verify after the test, the schema was cleaned up (look for `DROP SCHEMA test_req_...`).
6. Expected outcome: All integration tests automatically get schema isolation and security mocking without explicit setup.

### Step 6.2: Enable Parallel Execution
**Action:** Verify `backend/src/test/resources/junit-platform.properties`.
*   `junit.jupiter.execution.parallel.enabled=true`
*   `junit.jupiter.execution.parallel.mode.classes.default=concurrent`
*   Keep methods in the same thread by default to preserve multi-step scenario consistency within a class.
*   Set a deterministic parallelism strategy (fixed number or CPU-based) to avoid environment-dependent behavior across CI agents.

> ⚠️ **Known Limitation: Parallel Class Execution (2026-01)**
>
> **Status**: Parallel class execution is currently **disabled** (`mode.classes.default=same_thread`).
>
> **Symptom**: When `mode.classes.default=concurrent`, tests pass individually but fail intermittently
> when run together in the suite. Specifically:
> - `BusinessActivityIT.recordsBusinessActivityWhenGreetingCreatedViaRestApi` - expected 1 audit record, got 0
> - `GreetingControllerIT.returns404WhenDeletingNonExistentGreeting` - returns 500 instead of 404
>
> **Root Cause (suspected)**: HikariCP connection pool behavior with schema routing under parallel
> test class execution. Even with per-test schema isolation, connections borrowed by concurrent
> test classes may interfere with each other's `SET search_path` commands.
>
> **Current Workaround**: Sequential class execution (`same_thread`) ensures all 54 tests pass reliably.
> Test execution time is still acceptable (~45s for the full suite).
>
> **TODO**: Investigate HikariCP behavior with `SmartRoutingDataSource` in parallel scenarios:
> 1. Add connection ID logging to trace pool borrowing/returning
> 2. Consider connection-per-schema pooling or separate connection pools
> 3. Evaluate if `getConnection()` needs synchronization or connection affinity

**Test:**
1. Verify the properties file exists at `backend/src/test/resources/junit-platform.properties`.
2. Run `./mvnw test -X | grep "junit.jupiter.execution.parallel"` to confirm properties are loaded.
3. Execute the full test suite: `./mvnw verify`.
4. Observe test output: tests should run sequentially by class (until parallel execution issue is resolved).
5. Verify no test failures due to data pollution (schema isolation ensures clean separation).
6. Expected outcome: All tests pass reliably in sequential mode.

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

**Test:**
1. For each file updated, run the specific test: `./mvnw test -Dtest=GreetingControllerIT`.
2. Verify all tests pass.
3. Grep for remaining `@ResourceLock` in test files: `grep -r "@ResourceLock" backend/src/test/`.
4. Expected result: No `@ResourceLock` annotations should remain in IT classes.
5. Grep for remaining `DatabaseCleanupHelper` usage: `grep -r "DatabaseCleanupHelper" backend/src/test/`.
6. Expected result: No references to the cleanup helper should remain.

### 6.4 Test Data Lifecycle (Expected Pattern)
*   Seeding: Create test data via HTTP calls (RestAssured) or repositories within the test method as needed.
*   Migration: Flyway runs for each schema, continuously validating migrations.
*   Cleanup: Cleanup is implicit via `DROP SCHEMA … CASCADE`; table truncation helpers are retired.

**Test:**
1. Create a simple integration test that seeds test data via a POST request.
2. Query the database to verify the data exists.
3. Run a second integration test in the same suite with a different seed.
4. Verify each test's schema contains only its own data (cross-test data pollution check).
5. Expected outcome: Tests can seed data cleanly, and cleanup happens implicitly without manual effort.

### Step 6.5: Remove Legacy Cleanup Helper
**Action:** Delete `backend/src/test/java/com/example/demo/testsupport/DatabaseCleanupHelper.java`.

**Test:**
1. Verify the file exists before deletion.
2. Delete the file using the IDE or command line: `rm backend/src/test/java/com/example/demo/testsupport/DatabaseCleanupHelper.java`.
3. Run `./mvnw clean compile` to ensure no compilation errors.
4. Search for any remaining imports of this class: `grep -r "DatabaseCleanupHelper" backend/src/`.
5. Expected result: No compilation errors and no remaining references.

### Step 6.6: Verification
*   Run `mvn verify` to ensure all tests pass.
*   Check logs to ensure Schemas are being created and dropped.
*   Verify execution time reduction.

**Test:**
1. Run the full integration test suite: `./mvnw verify -Dspring.profiles.active=integration`.
2. Verify all tests pass with exit code 0.
3. Measure total execution time and compare to before refactoring (should show significant improvement due to parallelism).
4. Check logs for evidence of schema lifecycle:
   - Search for `CREATE SCHEMA test_req_` (should see multiple, not sequential).
   - Search for `DROP SCHEMA test_req_` (should see cleanup for each test).
   - Search for `Flyway migrating` (should appear multiple times, once per schema).
5. Run tests multiple times to ensure consistency (no flaky tests due to schema pollution).
6. Run a subset: `./mvnw test -Dtest=GreetingControllerIT,UserControllerSecuredIT` and verify isolation is working.
7. Expected outcome: Full suite passes, schemas are created and destroyed correctly, and execution time is significantly reduced.

### Step 6.7: Final Steps (Commit Final Integration & Cleanup)

After completing all Phase 4 steps and validating each one, execute these final steps to commit your changes:

**Final Validation:**
1. Check base class configuration:
   ```bash
   grep -E "@ExtendWith|@Import" backend/src/test/java/com/example/demo/testsupport/Abstract*.java
   # Should show both annotations present
   ```
2. Verify refactored test classes:
   ```bash
   grep -r "@ResourceLock" backend/src/test/ || echo "✓ No @ResourceLock found"
   grep -r "DatabaseCleanupHelper" backend/src/test/ || echo "✓ No cleanup helper references"
   ```
3. Run full test suite with parallelism:
   ```bash
   ./mvnw clean verify -pl backend -DfailIfNoTests=false
   ```
4. Measure execution time improvement (document baseline vs. actual).
5. Run twice to verify no flaky tests:
   ```bash
   ./mvnw verify -pl backend && ./mvnw verify -pl backend
   ```

**Commit Changes:**
```bash
git status  # Review all phase integration changes
git add -A  # Or selectively add test class updates

git commit -m "<commit message>
```

---
   Should show parallel=true in logs
   Generate performance report (before/after):
   ```bash
   echo "Execution time: $(./mvnw verify -pl backend -q 2>&1 | tail -1)"
   ```
1. Document in ticket/PR: "Phase 4 complete. Full hybrid parallel integration test strategy implemented. Tests now run 70%+ faster with improved isolation."

### Post-Implementation: Maintenance & Monitoring

After all 4 phases are complete and committed, establish these ongoing practices:

**Weekly Checks:**
- Monitor CI execution time trends (should remain consistently fast).
- Watch for any test flakiness or data pollution issues (schema context leaks).
- Review test logs for anomalies in schema creation/cleanup.

**Quarterly Reviews:**
- Measure actual test execution time improvement vs. baseline.
- Assess if connection pool size needs tuning based on concurrency patterns.
- Evaluate if any new tests are breaking isolation assumptions.

**When Adding New Tests:**
1. Ensure tests extend `AbstractIntegrationTest` or `AbstractRestAssuredIntegrationTest`.
2. Do NOT add `@ResourceLock` annotations.
3. Do NOT use `DatabaseCleanupHelper`.
4. Run tests locally with `./mvnw test -Dtest=YourNewTest` to verify schema isolation.

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
Mitigation: Use a TaskDecorator (or executor decoration) to propagate schema context for any async DB access. Ensure the custom executor bean has `@Primary` annotation and the config class has `@EnableAsync`.

### 7.5 HikariCP Connection Pool and Parallel Schema Routing
Risk: Under parallel test class execution, HikariCP's connection pooling can cause interference between concurrent tests' `SET search_path` commands, even with per-test schema isolation.
Symptom: Tests pass individually but fail intermittently when run together (wrong schema, missing data, unexpected errors).
Current Status (2026-01): Parallel class execution is disabled (`mode.classes.default=same_thread`) as a workaround.
Mitigation Options (future investigation):
1. Connection-per-schema pooling: Maintain separate connection pools per active test schema.
2. Connection affinity: Ensure a test class always gets the same physical connections throughout its lifecycle.
3. Synchronization: Add locking around `getConnection()` to prevent concurrent `SET search_path` race conditions.
4. Alternative isolation: Consider database-per-test-class (Option 2 in Section 4.A) if schema routing proves too complex.

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
    *   [Multi-Tenancy in Spring Boot](https://blog.captainfresh.in/multi-tenancy-in-spring-boot-a-practical-guide-c5602aeef3c8) (General concept adaptation)
    *   This supports the "Smart Routing DataSource" design for schema-per-thread isolation.

4.  **Spring Security Testing:**
    *   [Spring Security Reference: Testing JWTs](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/oauth2.html#testing-jwt)
    *   Standardizes the approach of using `JwtEncoder` or similar libraries to mint test tokens.

5.  **Context Propagation:**
    *   [Spring Framework: TaskDecorator](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/task/TaskDecorator.html)
    *   Essential for propagating `ThreadLocal` context to async threads in Spring Modulith.
    *   [Practical security context propagation](https://ankurm.com/spring-security-context-propagation-complete-guide/)

6.  **HikariCP Pool Sizing:**
    *   [HikariCP: About Pool Sizing](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)
    *   Mathematical basis for the connection pool tuning formula used in the plan.
