# 📄 Integration Test Strategy: Hybrid Parallel Architecture

## 1. Executive Summary
This strategy defines the approach for validating the **Spring Boot Backend** within the **BFF (Backend for Frontend)** architecture.

The primary objective is to transition from a **"Serialized/Heavy"** test suite (currently relying on `@ResourceLock`) to a **"Parallel/Hybrid"** suite. We aim to reduce feedback time by **70-80%** by removing bottlenecks such as the Keycloak container startup and database locking, while increasing reliability through strict data isolation.

## 2. Architectural Context
The application operates as an **OAuth2 Resource Server** behind a **Spring Cloud Gateway (BFF)**.

* **Production Flow:** `Browser` $\rightarrow$ `Gateway (Cookie)` $\rightarrow$ `Backend (JWT)`
* **Test Context:** The Integration Tests must validate the **Backend** in isolation.
* **Boundary:** We will **not** test Keycloak or the Gateway in this suite. We assume the Gateway correctly translates Cookies to JWTs. We test that the Backend correctly consumes those JWTs and interacts with the Database.

## 3. Pillar I: Authentication Strategy (The "Virtual Gateway")
**Goal:** Verify authorization rules and identity extraction without the overhead of a real Identity Provider.

### 3.1. The "Gateway Emulation" Pattern
Since the backend relies on the Gateway to handle the OAuth2 handshake, our tests will emulate the Gateway's behavior. We will **not** use a real Keycloak container.

* **Mocking Layer:** Replace the current `dasniko/testcontainers-keycloak` implementation with a local **JWT Factory**.
* **Token Generation:** Tests will self-sign JWTs using a test-specific secret key configured in a new `MockSecurityConfig`.
* **Payload Accuracy:** Generated tokens will strictly match the structure defined in **ADR-003** (claims, roles, issuer) to ensure `AbstractSecuredRestAssuredIT` validates them exactly as it would production tokens.

### 3.2. Authorization Testing Scope
* **Role Enforcement:** Verify that endpoints reject valid tokens lacking required roles (e.g., `ROLE_USER` trying to access Admin APIs).
* **Identity Propagation:** Verify that the `Authentication` principal is correctly mapped to the domain model (e.g., "Created By" auditing fields).
* **Error Handling:** Validate `401`/`403` responses for invalid, expired, or missing tokens.

## 4. Pillar II: Persistence Strategy (Logical Isolation)
**Goal:** Enable `forkCount=1` (Single JVM) parallel execution where multiple threads share one PostgreSQL container without data collisions.

### 4.1. The "Schema-per-Thread" Pattern
Instead of locking the database via `@ResourceLock`, we will logically partition it.

* **Shared Resource:** A single `PostgreSQLContainer` (Singleton) starts once for the entire suite.
* **Dynamic Isolation:**
    1.  **Setup (`@BeforeEach`):** A JUnit extension intercepts the test execution, generates a unique schema name (e.g., `test_schema_thread_12`), and creates it.
    2.  **Migration:** Programmatic invocation of **Flyway** (overriding the default boot behavior) to migrate *only* that specific schema.
    3.  **Routing:** A "Smart DataSource Proxy" intercepts JDBC calls and routes them to the thread-local schema using `SET search_path`.
    4.  **Teardown (`@AfterEach`):** The schema is dropped immediately, ensuring zero residue.

### 4.2. Transaction Management
* **RestAssured Constraints:** Since HTTP tests (Tomcat thread) and Test logic (JUnit thread) are separate, standard `@Transactional` rollback does **not** work.
* **Strategy:** We rely entirely on **Schema Dropping** for cleanup. Commits are allowed because they are confined to a disposable schema that ceases to exist after the test.

### 4.3. Cross-Thread Context Propagation (Critical)
**Challenge:** Using `RANDOM_PORT` creates a disconnect between the **Test Thread** (JUnit) and the **Server Thread** (Tomcat). The `ThreadLocal` holding the current schema is **not** automatically visible to the application code running in Tomcat.

**Strategy:** Header-Based Context Propagation.
1.  **Client-Side (Test):** The Base Integration Test automatically injects a custom HTTP Header (`X-Test-Schema: [schema_name]`) into every RestAssured request.
2.  **Server-Side (App):** A `OncePerRequestFilter` (registered only in the `test` profile) intercepts incoming requests, reads the `X-Test-Schema` header, and sets the `ThreadLocal` for the Tomcat worker thread.
3.  **Cleanup:** The filter ensures the `ThreadLocal` is cleared in a `finally` block to prevent thread pollution.

## 5. Execution Strategy
**Goal:** Maximize CPU utilization and minimize context reloading.

### 5.1. Parallel Configuration
* **JUnit 5 Mode:** `concurrent` execution for classes, `same_thread` for methods (to maintain state consistency within a scenario).
* **Thread Count:** Fixed parallelism based on available CPU cores (e.g., `Processors x 1` or `Fixed = 4`).

### 5.2. Resource Optimization
**Challenge:** Parallel tests consume significantly more database connections (JUnit setup + Tomcat request).
* **Connection Pool:** The HikariCP `maximum-pool-size` must be increased to accommodate concurrency.
* **Formula:** `(Parallel Threads * 2) + Buffer`. For 4 parallel classes, `maximum-pool-size` should be set to at least **20**.

### 5.3. Context Caching
* **Constraint:** The Spring `ApplicationContext` must remain **Singleton**.
* **Implementation:**
    * No usage of `@DirtiesContext`.
    * No `@DynamicPropertySource` usage that changes distinct values per test class.
    * The "Smart DataSource" handles connection variance internally, keeping the external configuration static.

## 6. Test Data Lifecycle
* **Seeding:** Data required for a test is inserted via HTTP calls (RestAssured) or Repositories *within* the test method.
* **Migration:** Validates that Flyway scripts work correctly on every run (since they run for every schema).
* **Cleanup:** Implicit via `DROP SCHEMA`. The manual truncation code in `DatabaseCleanupHelper` is no longer required.

## 7. Tooling & Libraries

| Component       | Implementation                                    |
| :-------------- | :------------------------------------------------ |
| **Runner**      | JUnit 5 (Jupiter)                                 |
| **Http Client** | RestAssured                                       |
| **Database**    | Testcontainers (PostgreSQL)                       |
| **Auth Mock**   | `spring-security-oauth2-jose` + `nimbus-jose-jwt` |
| **Migration**   | Flyway Core (Programmatic API)                    |
| **Assertions**  | AssertJ                                           |

## 8. Risk Assessment & Mitigation
* **Risk:** Mocked tokens might drift from real Keycloak tokens.
    * **Mitigation:** A single "Contract Test" (running nightly) should use the real Keycloak container to validate that our Mock Generator produces tokens compatible with the real Identity Provider.
* **Risk:** Async tasks (`@Async`, `CompletableFuture`) losing schema context.
    * **Mitigation:** The `ThreadLocal` context is not automatically propagated to `TaskExecutor` threads. If async logic touches the DB, we must decorate the `TaskExecutor` to propagate the schema context.
* **Risk:** Flyway overhead per test.
    * **Mitigation:** If schema creation becomes slow (>300ms), we can switch to "Template Database" cloning (Postgres `CREATE DATABASE FROM TEMPLATE`), which is faster than running migrations from scratch.

---

### 📚 References for Implementation

Please ensure your solution aligns with these architectural patterns:

1.  **BFF Authentication Pattern (Token Relay):**
    * *Reference:* [Spring Cloud Gateway: The Token Relay GatewayFilter Factory](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway/developer-guide.html#the-tokenrelay-gatewayfilter-factory)
    * *Why:* Justifies why we are **mocking** the JWTs. The Backend trusts the Gateway to handle the handshake; our tests mimic this "Relay" by injecting the token directly.

2.  **Spring Context Caching Rules:**
    * *Reference:* [Spring Framework: Parallel Test Execution](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/parallel-test-execution.html)
    * *Why:* We must strictly follow the "do not dirty context" rule (e.g., no `@DirtiesContext`, no changing `@DynamicPropertySource` per class) to keep performance high.

3.  **Smart Routing (Schema-per-Thread):**
    * *Reference:* [Multi-Tenancy in Spring Boot (AbstractRoutingDataSource)](https://blog.captainfresh.in/multi-tenancy-in-spring-boot-a-practical-guide-c5602aeef3c8)
    * *Why:* This is the exact pattern for switching schemas dynamically (via `ThreadLocal`) without switching the physical `DataSource`, enabling parallel execution on a single container.
    * *Javadoc:* [AbstractRoutingDataSource API](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/datasource/lookup/AbstractRoutingDataSource.html)

4.  **Spring Security Testing (Mocking JWTs):**
    * *Reference:* [Spring Security Test: Mocking JWTs](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/oauth2.html#testing-jwt)
    * *Why:* The standard way to test Resource Servers. We will use `JwtEncoder` with a local secret to mint tokens that match the structure defined in `ADR-003`.

5.  **Context Propagation (Fixing the "Dead Angle"):**
    * *Reference:* [Leveraging ThreadLocal for Context Propagation](https://medium.com/@27.rahul.k/leveraging-threadlocal-for-context-propagation-in-spring-boot-microservices-c261d9dc535a)
    * *Why:* Addresses the critical issue where `RestAssured` (Tomcat thread) cannot see the `ThreadLocal` schema set by the JUnit thread. Validates the need for a `OncePerRequestFilter`.

6.  **Optimizing Integration Tests at Scale:**
    * *Reference:* [Optimizing Spring Integration Tests at Scale (JavaPro)](https://javapro.io/2025/12/17/optimizing-spring-integration-tests-at-scale/)
    * *Why:* Reinforces the "Smart Context" concept and provides strategies for managing test data without heavy overhead.

7.  **HikariCP Pool Sizing for Parallelism:**
    * *Reference:* [HikariCP: About Pool Sizing](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)
    * *Why:* Justifies increasing `spring.datasource.hikari.maximum-pool-size` to prevent deadlocks when running multiple tests in parallel (JUnit Thread + Tomcat Thread x Parallelism Factor).

8.  **Optimizing Spring Contexts:**
    * *Reference:* [Optimizing Spring Integration Tests at Scale (JavaPro)](https://javapro.io/2025/12/17/optimizing-spring-integration-tests-at-scale/)
    * *Why:* detailed concepts on "Smart Context" and avoiding reload penalties.
