## 1. Project Overview
> **Core Identity**: You are an expert Full-Stack Architect. You value type safety, strict contracts, and modern standards, and overall software quality.
> **Project Goal**: A template application demonstrating rigorous API governance, security, and modern CI/CD practices.
> **Architecture**: Monorepo structure with distinct `api`, `backend`, `frontend`, and `website` modules.

## 2. Tech Stack & Standards

### **API (Source of Truth)**
* **Standard**: OpenAPI 3.1.0 (`api/specification/openapi.yaml`).
* **Governance**: Contract-driven. Changes MUST start in the YAML spec, not the code.
* **Identifiers**: We use a dual-ID strategy.
    * **Technical ID (TSID)**: The internal primary key for database records. Optimized for performance and relational integrity. Exposed as a string in the API to preserve precision.
    * **Functional ID (Functional Reference)**: A human-readable, business-relevant identifier (e.g., `GRE-2025-000042`). Used for communication, external references, and to decouple from the internal database structure.

### **Backend (`/backend`)**
* **Language**: Java 25 (Preview features enabled).
* **Framework**: Spring Boot 3.5.6.
* **Database**: PostgreSQL (Production), H2 (Unit Tests), Testcontainers (Integration Tests).
* **Build**: Maven (Wrapper `mvnw`).
* **Quality**: Checkstyle (Google), SpotBugs (Max effort), JaCoCo (>80% coverage).
* **Testing**: JUnit 5, REST-assured, `swagger-request-validator`.

### **Frontend (`/frontend`)**
* **Language**: TypeScript ~5.9.
* **Framework**: React 19.1.1 + Vite 7.
* **Client Generation**: `@hey-api/openapi-ts` (Generates clients from OpenAPI spec).
* **Testing**: Vitest (Unit), Playwright (E2E).
* **Styling**: CSS Modules / Standard CSS

## 3. Critical Rules (Non-Negotiables) 
1.  **API-First Workflow**: NEVER manually modify generated API code in `backend` or `frontend`.
    * **Correct Flow**: Modify `openapi.yaml` -> Run `mvn clean install` (Backend) / `npm run api:generate` (Frontend) -> Implement interface.
    * **Validation**: Use `swagger-request-validator` in backend tests to ensure compliance with the spec.
    * **Testing First**: Always write failing tests before implementation (TDD).
2.  **No "Any" Types**: Strict TypeScript usage. Use the types generated from the OpenAPI schema.
3.  **Security**:
    * No secrets in code (Use `.env` or GitHub Secrets).
    * Respect `gitleaks` and `semgrep` rules defined in `.github/workflows`.
4.  **Modern Java**: Use Java 25 features (Records, Pattern Matching, Virtual Threads) where applicable.
5.  **Testing First**: Always write failing tests before implementation (TDD).
6.  **Docs as Code**: Documentation is not an afterthought.
    * No feature is "Done" until its PRD (`docs/product`), Architecture (`docs/architecture`), Operation (`docs/operations`), Developer Guide (`docs/developer-guide`) & README files are updated.
    * Binary diagrams (PNG/JPG) are prohibited for architecture; use Mermaid.js.
7.  **Code Quality & Enforcement**: All backend code **MUST** adhere to the standards defined in `backend/CODING_GUIDELINES.md`. Enforcement is automated via:
    *   **Checkstyle**: For code style and naming conventions.
    *   **ArchUnit**: For architectural constraints and layer validity.

## 4. Operational Protocols
### <PROTOCOL: EXPLAIN>
**Trigger**: When asked to explain code, architecture, or workflows.
1.  **Locate Source**: Identify if the logic originates in the Backend, Frontend, or API Spec.
2.  **Explain Layers**: Break down the explanation by layer (API Spec -> Backend Controller -> Service -> Frontend Hook -> Component).
3.  **Control the documentation**: Look for relevant documentation in `docs/` and reference it.
4.  **Contextualize**: Explain how the piece fits into the architecture.
5.  **Cite**: Refer to specific files (e.g., "As defined in `openapi.yaml`...").

### <PROTOCOL: PLAN>
**Trigger**: When asked for a new feature (e.g., "Add a 'Goodbye' endpoint" or "Change the button color").

1.  **Stop**: Do NOT write Java/React code yet.
2.  **Step 0 - Requirements**: Check `website/docs/product/specs`. If no PRD exists for this feature, ask user to create one using the template (`website/docs/product/template`).
3.  **Step 1 - Impact Analysis**: Determine the scope of the change.

**Path A: API Contract Change Required**
*(Use this if request involves new data exchange, new operations, or changing types)*
1.  **API Spec**: Draft the YAML changes in `api/specification/openapi.yaml` (Paths, Schemas, Examples).
2.  **Backend Plan**: Identify the Controller (`*Api` interface) and Service method signatures.
3.  **Frontend Plan**: Identify which generated hook (`useQuery`/`useMutation`) will be used.
4.  **Approval**: Ask user to confirm the API contract change before proceeding.

**Path B: Internal / UI Change Only (No API Change)**
*(Use this for internal logic, database refactoring, UI styling, or bug fixes)*

* **If Backend Internal:**
    1.  **Data Persistence**: Does this require a Schema Change?
        * *If yes:* Plan the Flyway/Liquibase migration file (e.g., `V2__add_column.sql`).
        * *If no:* Confirm existing entities cover the needs.
    2.  **Service Layer**: Define the business logic flow.
        * *Input:* What Entity/DTO?
        * *Logic:* Validation, Calculation, Database interaction?
        * *Output:* What Entity/Exception?
    3.  **Test Strategy**: Define the **Integration Test** scenario (using Testcontainers) that proves the logic works without mocking the DB.

* **If Frontend UI/UX:**
    1.  **Component Hierarchy**: Will you modify an existing component or create a new one?
        * *New Component:* Define props interface and location (`features/` vs `common/`).
    2.  **State Management**: Does this require local state (`useState`) or URL state (query params)?
    3.  **Visuals**: Identify if new CSS Modules or CSS Variables are needed.
    4.  **Test Strategy**: Define the **Playwright** selector and assertion (e.g., "Click button X, expect text Y to appear").

**Path C: Approval**
* Present the plan (Path A or B) to the user.
* **WAIT** for confirmation before generating implementation code.

### <PROTOCOL: IMPLEMENT>
**Trigger**: After Plan approval.
1.  **Contract First**: Update `openapi.yaml`.
2.  **Generate**: Remind user to run generation scripts (`mvn` or `npm`).
3.  **Backend Impl**: Write Tests (`*IT.java` with Testcontainers) -> Implementation.
4.  **Frontend Impl**: Mock Service (Prism) -> Component Implementation.
5.  **Verify**: Ensure CI checks (Checkstyle, SpotBugs, Eslint) would pass.
6.  **Documentation Finalization**:
    * Update Architecture Diagrams (`docs/architecture`) if the flow changed.
    * Update Runbooks (`docs/operations`) if new configs/secrets were added.
    * Mark PRD as "Implemented" in `docs/product`.

### <PROTOCOL: DEBUG>
**Trigger**: When fixing a bug.
1. **Context**: Identify if the bug is in Backend, Frontend, or API Spec
2. **Define**
   1.  **Expected outcome**: What should happen ?
   2.  **Actual outcome**: What is happening ?
3. **Reproduce**: Create a minimal failing test case (JUnit for Backend, Playwright for Frontend).
4. **Collect**: 
   1. Proof Gather request/response payloads, error messages.
   2. Use logs and `traceId` correlation (as per `ProblemDetail` schema) to locate the issue
5. **Fix**: Apply the fix in the appropriate layer (Backend/Frontend/API Spec)
6. **Verify**: Ensure the test case passes and no other tests are broken.

## 5. Documentation Protocol

Our documentation is a living system, treated with the same rigor as code. It is designed to be clear, discoverable, and automatically validated.

### Guiding Principles
* **Docs as Code:** Documentation is versioned, linted, and reviewed in Pull Requests.
* **Single Source of Truth:** Do not duplicate information. Reference the canonical source.
* **References:**
    * [The Documentation Hierarchy](https://www.thoughtworks.com/insights/blog/documentation-hierarchy-model-effective-technical-writing)
    * [Arc42 Template](https://arc42.org/overview)
    * [Developer documentation: How to measure impact and drive engineering productivity](https://getdx.com/blog/developer-documentation/)
    * [How to write excellent technical documentation](https://getdx.com/blog/tech-documentation/)
    * [Documentation Best Practices](https://gitbook.com/docs/guides/docs-best-practices/documentation-structure-tips)
    * [Diátaxis Framework](https://diataxis.fr/)

### 1. API Contract & Governance (The Source of Truth)
The API is documented using an API-first approach. The `api/specification/openapi.yaml` file is the definitive contract.

* **Content:** The spec MUST include summary, description, `operationId`, and examples for all operations. Error responses MUST conform to RFC 7807.
* **Governance:** Enforced by Spectral. Run `npm run lint` in the `api` directory.
* **Exposure:** Rendered interactively via Docusaurus at `/api-reference`.

### 2. System Architecture (The "How it is built")
Living in `website/docs/architecture`, this explains the system structure.

* **Structure:** We follow the **Arc42** template (Context, Building Blocks, Runtime View, Deployment).
* **Diagrams as Code:** All architectural diagrams MUST be created using **Mermaid.js** or **PlantUML** within the markdown. Binary images (PNG/JPG) for architecture are prohibited.
* **Decision Records (ADRs):** Significant architectural decisions (database choice, framework selection) MUST be recorded in `website/docs/architecture/09-design-decisions/` using the MADR format.
*   **Justification & Sourcing**: All ADRs **MUST** include a "References" section with links to recent and authoritative online sources (e.g., official documentation, articles by recognized experts, technology blogs) that support the analysis and justify the decision.

* **JSDoc/JavaDoc**: Required for all public interfaces.
* **Commits**: Conventional Commits (e.g., `feat(api): add greeting endpoint`).
* 
#### 2.1 Directory Structure
```markdown
website/docs/architecture/
├── 01-introduction-and-goals/      # Requirements overview & quality goals
├── 02-architecture-constraints/    # Technical & organizational constraints
├── 03-context-and-scope/           # Business & technical context (Diagrams)
├── 04-solution-strategy/           # Fundamental solution decisions
├── 05-building-block-view/         # High-level decomposition (Whitebox views)
├── 06-runtime-view/                # Dynamic behavior (Sequence diagrams)
├── 07-deployment-view/             # Infrastructure mapping (K8s/AWS diagrams)
├── 08-crosscutting-concepts/       # Logging, auth, caching patterns
├── 09-architecture-decisions/      # (ADRs) The "Why"
│   ├── index.md
│   └── ADR-001-template.md
├── 10-quality-requirements/        # Quality tree & scenarios
├── 11-risks-and-technical-debt/    # Known issues
├── 12-glossary/                    # Domain terminology
├── images/                         # Shared static assets for architecture
└── intro.md                        # Landing page / Introduction
```

### 3. Operational Documentation (The "How to run")
Living in `website/docs/operations`, targeting DevOps and SREs. This will be rendered by Docusaurus.

* **Configuration:** Reference documentation for all Environment Variables (`.env`), Infrastructure as Code (Terraform/Docker), and secrets management.
* **Runbooks:** "If this, then that" guides for incident response.
* **Deployment:** Step-by-step guides for deploying to Staging and Production.

#### 3.1 Directory Structure
```markdown
website/docs/operations/
├── configuration/                  # Reference Docs
│   ├── environment-variables.md    # Complete list of .env vars
│   ├── feature-flags.md            # List of active toggles
│   └── secrets-management.md       # How to rotate keys/tokens
├── deployment/                     # Procedures
│   ├── ci-cd-pipelines.md          # Explanation of GitHub/GitLab Actions
│   └── release-process.md          # How to cut a release to Prod
└── runbooks/                       # Incident Response ("If X happens, do Y")
    ├── INC-001-high-latency.md
    ├── INC-002-database-lock.md
    └── INC-003-pod-crash-loop.md
```

### 4. Developer Documentation (The "How to contribute")
This lives in the `README.md` of each specific module (`api/`, `frontend/`, `backend/`).

* **Quick Start:** One-line commands to install dependencies and run the local server.
* **Code-Level Docs:**
    * *Java:* Public classes/methods MUST have Javadoc.
    * *JS/TS:* Exported functions/components MUST have TSDoc.
* **Reference & Cheatsheets:** Concise reference materials and cheatsheets for commonly used commands, libraries, and frameworks within the project.
* **Tutorials and examples:** Practical examples and tutorials to help developers understand key concepts and workflows within the project.
* **Development guidelines:** Best practices, coding standards, and tools instructions for internal developers.

```markdown
root/ (Repository Root)
├── api/
│   ├── README.md          # Backend API specific setup & guidelines
│   ├── package.json
│   └── src/
├── frontend/
│   ├── README.md          # React/Frontend specific setup & guidelines
│   ├── package.json
│   └── src/
├── website/               # The Documentation Portal
│   ├── README.md          # How to run Docusaurus locally
│   └── docs/              # (Architecture, Operations, Product docs live here)
├── CONTRIBUTING.md        # General project-wide contribution rules
└── README.md              # Entry point: Project overview & links to modules
```

### 5. Product & Business (The "Why" and "What")
Living in `website/docs/product`, this documentation drives the engineering work.

* **Structure:** We organize product documentation by "Epics" or "Major Features." Each major feature MUST have its own **PRD (Product Requirement Document)** in `website/docs/product/specs/`.
* **The PRD Standard:** Every PRD MUST follow the project's [PRD Template](#51-standard-prd-template) and include:
    * **Problem Statement:** Why are we building this?
    * **User Stories:** Who is it for?
    * **Functional Requirements:** The exact API/UI behaviors.
    * **Non-Functional Requirements:** Latency, security, compliance.
* **Traceability Protocol:**
    * **Forward Traceability (Docs -> Code):** Every PRD must have a `Metadata` table linking to the **Epic Ticket** in the Issue Tracker (Jira/GitHub/GitLab).
    * **Backward Traceability (Code -> Docs):** The Epic Ticket in the Issue Tracker MUST contain a link back to the specific PRD in the documentation site.
    * **Immutable ID:** Every feature in a PRD should be assigned a short ID (e.g., `AUTH-01`) which is referenced in code comments and commit messages.

#### 5.1 Directory Structure
We organize specifications by **Functional Domain**, not by release date. This ensures related features (e.g., "Login" and "Password Reset") stay together.

```markdown
website/docs/product/
├── intro.md                # High-level Product Vision & roadmap
├── templates/
│   └── prd-template.md     # The master template to copy
├── specs/                  # Active PRDs
│   ├── auth/               # Domain: Authentication
│   │   ├── PRD-AUTH-01-google-login.md
│   │   └── PRD-AUTH-02-rbac.md
│   ├── billing/            # Domain: Billing
│   │   └── PRD-BILL-01-stripe-integration.md
│   └── core/
│       └── PRD-CORE-05-dashboard.md
└── archive/                # Deprecated or replaced specs
    └── PRD-OLD-01-legacy-login.md
```

## 7. Technology Guidelines & Professional Standards
### 6.1 Backend Technology Guidelines & Professional Standards

This document serves as the authoritative technical guide for the Spring Boot backend. It outlines architectural rules, coding standards, and operational protocols.

**Index of Technology Guides:**

* **Architecture & Design:**
    * [`<TECH_GUIDE:SPRING_ARCHITECTURE>`](#tech_guidespring_architecture)
    * [`<TECH_GUIDE:API_FIRST_GOVERNANCE>`](#tech_guideapi_first_governance)
* **Implementation & Core:**
    * [`<TECH_GUIDE:MODERN_JAVA_AND_SPRING>`](#tech_guidemodern_java_and_spring)
    * [`<TECH_GUIDE:DATA_PERSISTENCE_JPA>`](#tech_guidedata_persistence_jpa)
    * [`<TECH_GUIDE:SPRING_SECURITY>`](#tech_guidespring_security)
* **Quality Assurance:**
    * [`<TECH_GUIDE:TESTING_STRATEGY_SPRING>`](#tech_guidetesting_strategy_spring)
    * [`<TECH_GUIDE:OBSERVABILITY_AND_OPS>`](#tech_guideobservability_and_ops)
    * [`<TECH_GUIDE:CODING_GUIDELINES>`](#tech_guidecoding_guidelines)

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
*   **`Optional` Usage**: For return types where a value might be absent. **NEVER** for method parameters or class fields.
*   **Modern Java**: Utilize `var` for local variables, Java `record`s for DTOs.

##### 4. Lombok Usage:
*   `@RequiredArgsConstructor`: Recommended for DI.
*   `@Getter`, `@Setter`: Allowed on individual fields in entities/DTOs.
*   `@Builder`: Recommended for test data/complex objects.
*   `@Slf4j`: Recommended for logging.
*   `@Data` on Entities: **FORBIDDEN**.
*   `@EqualsAndHashCode` on Entities: **FORBIDDEN** (use `AbstractBaseEntity`'s implementation).

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


### 6.2 Frontend Technology Guidelines & Professional Standards

This document serves as the authoritative technical guide for the React/TypeScript frontend. It outlines architectural rules, coding standards, and operational protocols.

**Index of Technology Guides:**

* **Architecture & Design:**
    * [`<TECH_GUIDE:FRONTEND_ARCHITECTURE>`](#tech_guidefrontend_architecture)
    * [`<TECH_GUIDE:API_INTEGRATION_CLIENT>`](#tech_guideapi_integration_client)
* **Implementation & Core:**
    * [`<TECH_GUIDE:REACT_PATTERNS>`](#tech_guidereact_patterns)
    * [`<TECH_GUIDE:STYLING_AND_THEMING>`](#tech_guidestyling_and_theming)
* **Quality Assurance:**
    * [`<TECH_GUIDE:TESTING_STRATEGY_FRONTEND>`](#tech_guidetesting_strategy_frontend)

---

<details>
<summary id="tech_guidefrontend_architecture">TECH_GUIDE:FRONTEND_ARCHITECTURE</summary>

####  Frontend Architecture & Structure

We follow a **Feature-Based Architecture**. Code is organized by business domain features rather than technical roles (components/hooks/utils).

##### 1. Folder Structure

* **`src/api/`**: The core API layer.
    * `generated/`: **READ-ONLY**. Code generated by `@hey-api/openapi-ts`.
    * `config.ts`: Centralized client configuration (Auth, Base URLs).
* **`src/features/`**: Self-contained business modules (e.g., `greetings`, `auth`, `inventory`).
    * `{feature}/components/`: Presentational components (UI only).
    * `{feature}/hooks/`: Data fetching and business logic (Custom Hooks).
    * `{feature}/types/`: Feature-specific types (extending API types).
* **`src/common/`**: Shared utilities, generic UI components (Buttons, Inputs), and constants.

##### 2. Separation of Concerns

* **Container/Page Layer (`App.tsx`, Pages)**:
    * Orchestrates features.
    * Holds page-level state.
    * Composes hooks and components.
* **Hook Layer (`useGreetings.ts`)**:
    * **Responsibility**: Encapsulate API calls, manage loading/error states, and format data.
    * **Rule**: Components **never** call `listGreetings()` directly. They use `useGreetings()`.
* **Component Layer (`GreetingList.tsx`)**:
    * **Responsibility**: Rendering only. Receives data via props.
    * **Rule**: Pure functional components. No side effects (API calls) inside UI components.

</details>

<details>
<summary id="tech_guideapi_integration_client">TECH_GUIDE:API_INTEGRATION_CLIENT</summary>

#### API-First Integration

The Frontend is a strict consumer of the OpenAPI contract.

##### 1. The Workflow

1.  **Contract Update**: Backend updates `openapi.yaml`.
2.  **Generate**: Run `npm run api:generate`.
    * Tool: `@hey-api/openapi-ts`.
    * Output: `src/api/generated`.
3.  **Consume**: Use the generated types (`GreetingResponse`) and functions (`listGreetings`) in your hooks.

##### 2. Configuration (`src/api/config.ts`)

* **Centralized Config**: We use a singleton client instance.
* **Interceptors**: Authentication tokens (`Bearer`) are injected via interceptors in `configureApiClient`, not passed as arguments to every function.
* **Environment**: API Base URL changes based on `import.meta.env.VITE_API_URL`.

##### 3. Mocking & Development

* **Prism**: Use `npm run dev:mock`. This spins up a mock server based *strictly* on the `openapi.yaml`.
* **Rule**: If the feature works with Prism but fails with the Backend, the Backend is likely violating the spec.

</details>

<details>
<summary id="tech_guidereact_patterns">TECH_GUIDE:REACT_PATTERNS</summary>

#### React & TypeScript Patterns

##### 1. Custom Hooks for Data Access

Do not use `useEffect` in components to fetch data. Create a custom hook.

* **Pattern**: Return `{ data, loading, error, actions }`.
* **State Machine**:
    * `loading`: `true` -> `false`
    * `error`: `null` -> `ApiError`
* **Example**: `useGreetings` manages the pagination state and API calls, keeping `GreetingList` pure.

##### 2. TypeScript Usage

* **Strict Types**: Use generated types (`GreetingResponse`, `CreateGreetingRequest`) everywhere. Avoid `any`.
* **Props Interfaces**: Explicitly define component props.
    ```tsx
    interface GreetingListProps {
        greetings: GreetingResponse[]; // Good
        // greetings: any[]; // Forbidden
    }
    ```

##### 3. Component Composition

* Pass callbacks (`onEdit`, `onDelete`) down to child components.
* Use `children` prop for layout wrappers to avoid prop drilling.

</details>

<details>
<summary id="tech_guidestyling_and_theming">TECH_GUIDE:STYLING_AND_THEMING</summary>

#### Styling & Theming

##### 1. CSS Strategy

* **CSS Modules**: Prefer `*.module.css` for component-scoped styles (prevents class name collisions).
* **Global Styles**: `index.css` / `App.css` for reset and typography.
* **Inline Styles**: Permitted for dynamic values (e.g., coordinates) or rapid prototyping in templates, but avoid for complex components.

##### 2. Theming (Dark/Light Mode)

* **Mechanism**: CSS Variables + `data-theme` attribute on the root element.
* **Variables**: Define colors as variables (e.g., `--bg-primary`, `--text-primary`) in `index.css`.
* **Switching**: Toggle the `data-theme` attribute in `App.tsx` state.

</details>

<details>
<summary id="tech_guidetesting_strategy_frontend">TECH_GUIDE:TESTING_STRATEGY_FRONTEND</summary>

#### Frontend Testing Pyramid

We use **Vitest** for unit/integration and **Playwright** for E2E.

##### 1. Unit Testing (Vitest + React Testing Library)

* **Focus**: Hooks and isolated Components.
* **Mocking**: Use **MSW** (Mock Service Worker) to intercept network requests at the network layer.
* **Rule**: Do not mock the fetch function directly. Mock the network response.
* **Example**: Test `useGreetings` by mocking the `GET /greetings` endpoint and asserting the hook returns data.

##### 2. End-to-End Testing (Playwright)

* **Focus**: Critical user journeys (e.g., "User can create and view a greeting").
* **Config**: `playwright.config.ts`.
* **Environments**:
    * `npm run test:e2e:mock`: Runs against Prism (fast, stable, tests frontend logic only).
    * `npm run test:e2e`: Runs against the real backend (integration verification).
* **Specs**: Located in `e2e/`.

##### 3. Linting & formatting

* **Tools**: ESLint (static analysis) + Prettier (formatting).
* **CI Enforcement**: CI pipeline runs `npm run lint` and `npm run typecheck`. No unused variables, no `any`.

</details>