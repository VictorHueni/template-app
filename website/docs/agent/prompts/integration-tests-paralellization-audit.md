# Refactoring Request: Hybrid Parallel Integration Test Strategy

**Role:** Act as a Principal Java Architect and Testcontainers Expert specializing in High-Performance CI/CD.

**Context:**
I am refactoring a Spring Boot 4 (or 3.x) integration test suite for an **OAuth2 Resource Server** running behind a **BFF Gateway**.
* **Current Architecture:** The suite currently uses "Big Bang" testing (spinning up full Keycloak + Postgres containers for every test), which forces serialized execution (`@ResourceLock`) and is extremely slow.
* **Target Architecture:** I want to move to a **Hybrid Strategy**:
    1.  **Auth:** **Mock the Gateway/Keycloak**. Since the Gateway handles the actual OAuth2 handshake, the Backend only needs to validate JWTs. We will generate signed JWTs locally in the test to simulate the Gateway (BFF pattern).
    2.  **Persistence:** **Isolate via Schemas**. We will keep the real `PostgreSQLContainer` (Singleton) but use a "Schema-per-Thread" strategy to allow parallel execution without data collisions.

**The Constraints:**
* **Single JVM:** `forkCount=1` (Memory efficiency).
* **Context Caching:** I **cannot** allow the Spring `ApplicationContext` to reload between test classes (no `@DirtiesContext` or changing `@DynamicPropertySource` values).
* **Thread Disconnect:** I am using **RestAssured** (`@SpringBootTest(webEnvironment = RANDOM_PORT)`). This means the Test Logic runs in the **JUnit Thread**, but the Controller Logic runs in a separate **Tomcat Thread**.

---

### 🛠️ Required Refactoring Plan

Please provide a comprehensive implementation plan and the necessary Java code for the following pillars:

#### 1. Authentication Layer: The "Virtual Gateway" (Mock Strategy)
* **The Goal:** Remove the `KeycloakContainer` entirely to save ~15s of startup time.
* **The Solution:** Implement a **Local JWT Factory** that mints tokens signed with a test secret, mimicking the Spring Cloud Gateway Token Relay.
* **Requirements:**
    1.  **Remove** `KeycloakTestcontainerConfiguration` and `KeycloakTokenProvider`.
    2.  **Create** `MockSecurityConfig`: A test configuration that configures a `JwtDecoder` to trust a fixed, local secret key (HS256).
    3.  **Create** `TestJwtUtils`: A utility to generate raw JWT strings (Bearer tokens) using the same secret. The claims must match our production profile (e.g., `preferred_username`, `realm_access.roles`).
    4.  **Update** `AbstractSecuredRestAssuredIT`: Replace the real token fetcher with `TestJwtUtils.generateToken()`.

#### 2. Persistence Layer: The "Smart Schema" Pattern
* **The Goal:** Run parallel tests on a shared Postgres container without data collisions.
* **Requirements:**
    1.  **Create** `SchemaIsolationExtension`: A JUnit 5 extension that:
        * `BeforeEach`: Generates a random schema name (e.g., `test_schema_thread_1`), creates it, and runs **Flyway** programmatically on *just* that schema.
        * `AfterEach`: Drops the schema immediately.
    2.  **Create** `SmartRoutingDataSource`: A proxy (wrapping the Testcontainer DataSource) that intercepts `getConnection()` and executes `SET search_path TO [current_schema]` based on a `ThreadLocal`.

#### 3. Critical Fix: Cross-Thread Context Propagation
* **The Problem:** Because of `RANDOM_PORT`, the `ThreadLocal` set by JUnit is **invisible** to the Tomcat thread handling the HTTP request.
* **The Solution:** Implement Header-Based Propagation.
* **Requirements:**
    1.  **Create** `TestSchemaFilter`: A `OncePerRequestFilter` (active only in test profile) that:
        * Intercepts every request.
        * Reads a custom header: `X-Test-Schema`.
        * Sets the `SchemaContext` (ThreadLocal) for the duration of the request.
        * Clears it in a `finally` block.
    2.  **Update** `AbstractIntegrationTest`: Configure RestAssured to automatically inject the `X-Test-Schema` header into every outgoing request using the value from the JUnit thread.

#### 4. Execution & Configuration
* **Cleanup:** Identify which classes/properties to delete (e.g., `DatabaseCleanupHelper`, `application-keycloak-test.properties`).
* **Connection Pool Tuning:** Since we are running parallel tests with blocked threads (JUnit waiting on Tomcat), the default pool size is insufficient.
    * **Action:** Configure `junit-platform.properties` for parallel execution.
    * **Action:** Update `application-integration.properties` to increase `spring.datasource.hikari.maximum-pool-size` using the formula `(Parallel Threads * 2) + Buffer` (e.g., set to 20 for 4 parallel tests).

---

### 📚 References for Implementation

Please ensure your solution aligns with these architectural patterns:

1.  **BFF Authentication Pattern (Token Relay):**
    * *Reference:* [Spring Cloud Gateway: The Token Relay GatewayFilter Factory](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway/developer-guide.html#the-tokenrelay-gatewayfilter-factory)
    * *Why:* Justifies why we are **mocking** the JWTs. The Backend trusts the Gateway to handle the handshake; our tests mimic this "Relay" by injecting the token directly.

2.  **Spring Context Caching Rules:**
    * *Reference:* [Spring Framework: Parallel Test Execution](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/parallel-test-execution.html)
    * *Why:* We must strictly follow the "do not dirty context" rule to keep performance high.

3.  **Smart Routing (Schema-per-Thread):**
    * *Reference:* [Multi-Tenancy in Spring Boot (AbstractRoutingDataSource)](https://blog.captainfresh.in/multi-tenancy-in-spring-boot-a-practical-guide-c5602aeef3c8)
    * *Why:* This is the exact pattern for switching schemas dynamically (via `ThreadLocal`) without switching the physical `DataSource`.

4.  **Spring Security Testing (Mocking JWTs):**
    * *Reference:* [Spring Security Test: Mocking JWTs](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/oauth2.html#testing-jwt)
    * *Why:* The standard way to test Resource Servers without external IdPs.

5.  **Context Propagation (Fixing the "Dead Angle"):**
    * *Reference:* [Leveraging ThreadLocal for Context Propagation](https://medium.com/@27.rahul.k/leveraging-threadlocal-for-context-propagation-in-spring-boot-microservices-c261d9dc535a)
    * *Why:* Addresses the critical issue where `RestAssured` (Tomcat thread) cannot see the `ThreadLocal` schema set by the JUnit thread.

6.  **HikariCP Pool Sizing for Parallelism:**
    * *Reference:* [HikariCP: About Pool Sizing](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)
    * *Why:* Justifies increasing `spring.datasource.hikari.maximum-pool-size` to prevent deadlocks when running multiple tests in parallel.