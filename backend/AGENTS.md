# Backend Architect Context (`/backend`)

## 1. Role & Scope
> **Role**: You are the **Backend Architect**. You build robust, scalable, and secure Spring Boot applications.
> **Goal**: Implement business logic with strict adherence to the API contract and architectural patterns.
> **Source of Truth**: `src/main/java` (Implementation) and `src/test/java` (Verification).

## 2. Key Files & Directories
| Path | Purpose | Interaction |
| :--- | :--- | :--- |
| `src/main/java/com/example/demo/` | **The Code**. Feature-based package structure. | **EDIT**. Implement Controllers, Services, and Repositories here. |
| `src/test/java/` | **The Tests**. Unit, Slice, and Integration tests. | **EDIT**. Write tests *before* implementation. |
| `src/main/resources/db/migration/` | **The Database**. Flyway SQL migration scripts. | **ADD**. Create new files for schema changes. |
| `pom.xml` | **The Build**. Dependencies and plugin configuration. | **READ/EDIT**. Manage libraries and build lifecycle. |
| `CODING_GUIDELINES.md` | **The Rules**. Detailed coding standards. | **READ**. Adhere strictly to these rules. |

## 3. Design Guidelines (The Style Guide)

### Tech Stack
*   **Language**: Java 25 (Preview features enabled).
*   **Framework**: Spring Boot 4.0.0 / Spring Framework 7.0.0.
*   **Build**: Maven (Wrapper `mvnw`).
*   **Database**: PostgreSQL (Prod), Testcontainers (Integration Tests).

### Technology Guides
This project follows strict architectural rules. Consult these guides for implementation details:

*   **Architecture & Design:**
    *   [`<TECH_GUIDE:SPRING_ARCHITECTURE>`](#tech_guidespring_architecture)
    *   [`<TECH_GUIDE:API_FIRST_GOVERNANCE>`](#tech_guideapi_first_governance)
*   **Implementation & Core:**
    *   [`<TECH_GUIDE:MODERN_JAVA_AND_SPRING>`](#tech_guidemodern_java_and_spring)
    *   [`<TECH_GUIDE:DATA_PERSISTENCE_JPA>`](#tech_guidedata_persistence_jpa)
    *   [`<TECH_GUIDE:SPRING_SECURITY>`](#tech_guidespring_security)
*   **Quality Assurance:**
    *   [`<TECH_GUIDE:TESTING_STRATEGY_SPRING>`](#tech_guidetesting_strategy_spring)
    *   [`<TECH_GUIDE:OBSERVABILITY_AND_OPS>`](#tech_guideobservability_and_ops)
    *   [`<TECH_GUIDE:CODING_GUIDELINES>`](#tech_guidecoding_guidelines)

---

<details>
<summary id="tech_guidecoding_guidelines">TECH_GUIDE:CODING_GUIDELINES</summary>

#### Backend Coding Guidelines Summary

This section summarizes the key coding standards for backend development, enforced by static analysis tools. For full details, refer to `backend/CODING_GUIDELINES.md`.

##### 1. Naming Conventions:
*   **Packages**: Feature-based (`com.example.demo.feature`).
*   **Classes**: `PascalCase` with appropriate suffixes (`Controller`, `Service`, `Repository`, `Mapper`, `Config`, `Exception`). Entities are simple nouns. DTOs are `*Request`, `*Response`, `*DTO`.
*   **Methods**: `camelCase`, verb-first.
*   **Variables**: `camelCase` for fields/locals, `UPPER_SNAKE_CASE` for `static final` constants.

##### 2. Formatting:
*   **Indentation**: 4 spaces.
*   **Braces**: K&R style (opening brace on same line).
*   **Line Length**: Max 120 characters.
*   **Imports**: No wildcards, specific ordering (java, jakarta, org, com, static).

##### 3. Programming Practices & Patterns:
*   **Constructor Injection**: **Mandatory** with `private final` fields and `@RequiredArgsConstructor`. Field injection is forbidden.
*   **Immutability**: Favor Java `record`s for DTOs.
*   **API Layer (Controllers)**: Implement OpenAPI-generated interfaces. Handle HTTP concerns, DTO conversion, delegate business logic to services. **MUST NOT** access repositories directly.
*   **Service Layer**: Business logic, transactional boundaries. Operates on **Entities only**. **MUST NOT** accept/return DTOs directly.
*   **DTO & Entity Mapping**: **MapStruct is MANDATORY** for all DTO-to-Entity conversions.
*   **`Optional` Usage**:
    *   **Return Types**: Services/Repos SHOULD return `Optional<T>` if a result might be missing.
    *   **Controller**: MUST handle `Optional` (e.g., return 404).
    *   **Forbidden**: NEVER use `Optional` for method parameters or class fields.
*   **Modern Java**: Utilize `var` for local variables, Java `record`s for DTOs.

##### 4. Lombok Usage:
*   `@RequiredArgsConstructor`: Recommended for DI.
*   `@Getter`, `@Setter`: Allowed on individual fields in entities/DTOs.
*   `@Builder`: Recommended for test data/complex objects.
*   `@Slf4j`: Recommended for logging.
*   `@Data` on Entities: **FORBIDDEN**.
*   `@EqualsAndHashCode` on Entities: **FORBIDDEN** (use `AbstractBaseEntity`'s implementation).

##### 5. Database Naming Conventions

*   **Core Principles**: `snake_case` everywhere, no redundancy (use `email`, not `user_email`), full words (no abbreviations), module scoping (`auth_user`).
*   **Tables**: `{module}_{entity}` (e.g., `auth_user`, `sales_order`).
*   **Columns**: `snake_case` (e.g., `first_name`, `created_at`). PK is always `id` (TSID). FK is `{target}_id`.
*   **Constraints**:
    *   **PK**: `pk_{table}`
    *   **FK**: `fk_{source}_{source_column}_{target}` (prevents collisions)
    *   **UK**: `uk_{table}_{column}`
    *   **Check**: `chk_{table}_{condition}`
    *   **Index**: `ix_{table}_{column}`
*   **Sequences**: `seq_{table}_{column}`. Must use `@SequenceGenerator`.
*   **Reserved Words**: NEVER use Postgres reserved words (e.g., `user` -> `app_user`, `order` -> `sales_order`).
*   **Audit**: Suffix `_aud`. Columns `rev`, `revtype`, `revend`.

</details>

---

<details>
<summary id="tech_guidespring_architecture">TECH_GUIDE:SPRING_ARCHITECTURE</summary>

#### Spring Boot Architecture & Patterns

We follow a strict, vertical slice architecture organized by domain features, then technical layers.

##### 1. Layered Architecture (The Standard)

* **Controller Layer (`*.controller`)**:
    * **Responsibility**: Handle HTTP requests, validate inputs, and use the mapper to convert between DTOs and internal models for the service layer.
    * **Rule**: Controllers **must** implement the interface generated from the OpenAPI spec. They should contain *no* business logic.

* **Service Layer (`*.service`)**:
    * **Responsibility**: Business logic, transaction boundaries (`@Transactional`), and orchestration. Operates exclusively on JPA entities (Models).
    * **Rule**: Services accept and return JPA entities or primitive types. They never accept or return DTOs directly, nor do they return `ResponseEntity`.

* **Repository Layer (`*.repository`)**:
    * **Responsibility**: Data access.
    * **Rule**: Use Spring Data JPA interfaces. Custom queries should use JPQL or `Specification`.

##### 2. Data Flow: DTOs, Entities, and Mappers

To ensure a strict separation between the API contract and the internal data model, we follow this pattern:

*   **DTOs (Data Transfer Objects)**: These objects define the "shape" of the API.
    *   **Source**: **Automatically generated** from the `openapi.yaml` specification by the `openapi-generator-maven-plugin`.
    *   **Usage**: Used exclusively in the Controller layer and for API responses.

*   **Entities (`*.model`)**: These are the JPA entities that map to the database schema.
    *   **Source**: **Manually created** by developers.
    *   **Usage**: Used by the Service and Repository layers for all business logic and data persistence.

*   **Mappers (`*.mapper`)**: This layer is responsible for the conversion between DTOs and Entities.
    *   **Standard**: **MapStruct is the mandatory library** for this purpose.
    *   **Implementation**: Developers create a Java interface annotated with `@Mapper` and define the mapping methods. MapStruct generates the implementation at compile time.

The data flow is: `Request -> Controller (receives DTO) -> Mapper (DTO to Entity) -> Service (operates on Entity) -> Mapper (Entity to DTO) -> Controller (returns DTO) -> Response`.

##### 3. Package Structure

Organize code by **feature**, not just by layer (Screaming Architecture).

* **Bad**: `com.example.demo.controllers`, `com.example.demo.services`
* **Good**: `com.example.demo.greeting.controller`, `com.example.demo.greeting.service`

##### 4. Error Handling

* **Global Handling**: Use `@ControllerAdvice` / `GlobalExceptionHandler`.
* **Standard**: All errors must return RFC 7807 `ProblemDetail` JSON objects.
* **Pattern**: Map custom exceptions (e.g., `ResourceNotFoundException`) to HTTP 404 in the handler.

##### 5. Shared Kernel (`common` package)

The `com.example.demo.common` package serves as a shared kernel, containing cross-cutting concerns and reusable components available to all features.

*   **`common.audit`**: Implements entity auditing and revision history using Hibernate Envers.
*   **`common.config`**: Centralizes application configuration, including JPA auditing, security settings, and web configurations.
*   **`common.domain`**: Provides base entities, like `AbstractBaseEntity`, from which all domain models should inherit.
*   **`common.exception`**: Contains the global exception handler (`GlobalExceptionHandler`) and a set of standardized custom exceptions (`ResourceNotFoundException`, etc.) used throughout the application.
*   **`common.repository`**: Holds shared data-access components, such as custom ID generators.
</details>

<details>
<summary id="tech_guideapi_first_governance">TECH_GUIDE:API_FIRST_GOVERNANCE</summary>

#### API-First Workflow & Governance

The OpenAPI specification is the **single source of truth**. Code is a derivative of the spec.

##### 1. The Workflow

1.  **Design**: Modify `api/specification/openapi.yaml`.
2.  **Generate**: Run `mvn clean install`. The `openapi-generator-maven-plugin` generates the Java interfaces.
3.  **Implement**: The Controller implements the generated `*Api` interface.
    * **Warning**: Never manually modify generated files in `target/`.

##### 2. Contract Enforcement

* **Strict Types**: The build process generates specific enums and model classes. Use them instead of generic `Map` or `Object`.
* **Validation**:
    * **Request**: `swagger-request-validator` intercepts requests in tests to ensure they match the spec.
    * **Response**: The same validator ensures our backend does not violate the contract it promised to consumers.
</details>

<details>
<summary id="tech_guidemodern_java_and_spring">TECH_GUIDE:MODERN_JAVA_AND_SPRING</summary>

#### Java 25 & Spring Boot 3.5 Standards

Leverage the modern capabilities of the stack. Do not write legacy Java.

##### 1. Java 25 Features

* **`var`**: Use `var` for local variables where the type is obvious.
* **Records**: Use `record` for DTOs and immutable data carriers.
* **Pattern Matching**: Use `instanceof` pattern matching and switch expressions to reduce boilerplate.
* **Virtual Threads**: Enabled by default in Spring Boot 3.2+. Blocking I/O is no longer a performance bottleneck.

##### 2. Dependency Injection

* **Constructor Injection**: **Mandatory**. Do not use `@Autowired` on fields.
* **Lombok**: Use `@RequiredArgsConstructor` to generate constructors for `final` fields. This is the **mandatory** approach for dependency injection.

##### 3. Bean Validation

* Use `jakarta.validation` annotations (`@NotNull`, `@Size`) on DTOs.
* Validation is triggered automatically by `@Valid` in the Controller.
</details>

<details>
<summary id="tech_guidedata_persistence_jpa">TECH_GUIDE:DATA_PERSISTENCE_JPA</summary>

#### Data Access with Spring Data JPA

##### 1. Entity Design & Identifier Strategy

Our entities follow a dual-identifier strategy to ensure both database performance and business-level clarity.

*   **Technical ID (Surrogate Key)**:
    *   **Field**: `private Tsid id;` (The Primary Key)
    *   **Strategy**: Use **TSID** (Time-Sorted Unique Identifier). It is more performant than UUIDs for DB indexing and provides a loosely chronological sort order. This ID is for internal use (e.g., foreign keys, API calls between microservices). It should not be the primary identifier exposed to end-users for business processes.

*   **Functional ID (Business Key)**:
    *   **Field**: `private String functionalId;`
    *   **Strategy**: A human-readable, domain-meaningful identifier (e.g., `GRE-2025-000042`). Generated by application logic (e.g., `FunctionalIdGenerator`). This ID is used for external communication, customer support, and in user-facing URLs. It should have a unique constraint in the database and be indexed for lookups.

> **Best Practice Rationale**: This separation decouples the application's external-facing identifiers from the internal database implementation. It allows the database schema to be refactored (e.g., changing the PK data type) without impacting external clients, while providing a stable, readable reference for business operations.

*   **Base Entity**: Extend `AbstractBaseEntity` for standard auditing (`createdAt`, `updatedAt`, `version`).
*   **Hypersistence Utils**: Use `hypersistence-utils` for advanced types (e.g., JSONB mapping) if needed.

##### 2. Entity Auditing with Hibernate Envers

For entities requiring a full history of changes, we use Hibernate Envers. This is managed in the `common.audit` package.

*   **Implementation**: We use a `CustomRevisionEntity` and `CustomRevisionListener` to enrich the audit trail with application-specific data, such as the user who performed the change.
*   **Usage**: Annotate entities with `@Audited` to enable history tracking.

##### 3. Repository Best Practices

* **Pagination**: Always support `Pageable` for list endpoints.
* **Projections**: Use Java Records as projections for read-only data to avoid fetching entire entities.
* **N+1 Problem**: Be vigilant. Use `@EntityGraph` or `JOIN FETCH` when loading related collections.

##### 4. Transactions

* Place `@Transactional` at the **Service** level, not the Controller or Repository.
* **Optimization**: Use `@Transactional(readOnly = true)` for read operations.
</details>

<details>
<summary id="tech_guidespring_security">TECH_GUIDE:SPRING_SECURITY</summary>

#### Security & Authentication

##### 1. Configuration

* **Stateless**: The API is stateless. Session creation policy is `STATELESS`.
* **CSRF**: Disabled (standard for stateless REST APIs).
* **CORS**: Explicitly configured to allow specific frontend origins.

##### 2. Authorization

* **Method Level**: Use `@PreAuthorize("hasRole('ADMIN')")` or `@PreAuthorize("hasAuthority('SCOPE_read')")` to secure service methods.
* **User Context**: Access the current user via `SecurityContextHolder` or strictly typed `@AuthenticationPrincipal` arguments.

##### 3. Implementation

*   **User Service**: The core user authentication logic is implemented in the `com.example.demo.user` package.
*   **`UserDetailsServiceImpl`**: This class implements Spring Security's `UserDetailsService` to load user-specific data from the database.
</details>

<details>
<summary id="tech_guidetesting_strategy_spring">TECH_GUIDE:TESTING_STRATEGY_SPRING</summary>

#### The Spring Testing Pyramid

We prioritize execution speed and reliability.

##### 1. Unit Tests (`*Test.java`)
* **Scope**: Single class (usually Service or Utility).
* **Mocking**: Use `Mockito` (`@ExtendWith(MockitoExtension.class)`).
* **Speed**: Must run in milliseconds. **No Context loading.**

##### 2. Slice Tests (Integration)
* **Scope**: A specific layer of the application.
* **Web Layer**: `@WebMvcTest`. Mocks the Service layer. Verifies HTTP status, serialization, and validation logic.
* **Data Layer**: `@DataJpaTest`. Uses H2 or Testcontainers. Verifies queries and mappings.

##### 3. Full Integration Tests (`*IT.java`)
* **Scope**: Full context load (`@SpringBootTest`).
* **Environment**: Uses **Testcontainers** to spin up a real PostgreSQL instance.
* **Tooling**: Use **RestAssured** for fluent API testing.
* **Base Class**: Extend `AbstractIntegrationTest` to inherit the container setup.

##### 4. Contract Tests
* **Tool**: `swagger-request-validator-restassured`.
* **Goal**: Ensure every integration test request/response complies with `openapi.yaml`.
</details>

<details>
<summary id="tech_guideobservability_and_ops">TECH_GUIDE:OBSERVABILITY_AND_OPS</summary>

#### Observability & Operations

##### 1. Health & Metrics (Actuator)
* **Endpoints**: Expose `/actuator/health` and `/actuator/info`.
* **Build Info**: The `git-commit-id-maven-plugin` populates version and commit info.

##### 2. Logging
* **Format**: Structured JSON logging in production.
* **Levels**: `INFO` by default. `DEBUG` for application packages (`com.example.demo`) in `dev` profile.
* **Correlation**: Ensure `traceId` and `spanId` are propagated in logs for distributed tracing.
</details>

## 4. Tooling & Commands (Agent Cheatsheet)
Use these commands via `run_shell_command` within the `backend/` directory.

| Goal | Command | Why? |
| :--- | :--- | :--- |
| **Sync API** | `./mvnw clean compile` | Regenerates API interfaces from `openapi.yaml`. **Run this first.** |
| **Run Tests** | `./mvnw verify` | Runs Unit, Integration, and Contract tests. |
| **Start Local** | `./mvnw spring-boot:run` | Starts the backend on `localhost:8080`. |
| **Lint/Format** | `./mvnw checkstyle:check` | Verifies code style compliance. |
| **Security** | `./mvnw spotbugs:check` | Runs static security analysis. |

## 5. Workflows

### <PROTOCOL:IMPLEMENT_API>
**Trigger**: A new endpoint is defined in `openapi.yaml`.
1.  **Sync**: Run `./mvnw clean compile` to generate the new interface.
2.  **Controller**: Implement the generated `*Api` interface.
3.  **Service**: Delegate business logic to a `@Service`.
4.  **Mapper**: Use MapStruct to convert DTOs to Entities.
5.  **Test**: Write an Integration Test (`*IT.java`) verifying the flow.

### <PROTOCOL:DB_MIGRATION>
**Trigger**: Data model changes.
1.  **Script**: Create `V{N}__{Description}.sql` in `src/main/resources/db/migration`.
2.  **Entity**: Update the JPA Entity (`@Entity`) to match.
3.  **Verify**: Run tests. Flyway will automatically apply the script in the test container.

## 6. Critical Directives (Non-Negotiables)
1.  **API First**: Do not create Controllers manually. Implement the generated interfaces.
2.  **Strict Layering**: Controllers speak DTOs; Services speak Entities. Never mix them.
3.  **Testing**: No feature is done without an Integration Test.
4.  **Security**: Never commit secrets. Use `application-local.properties` for local dev only.