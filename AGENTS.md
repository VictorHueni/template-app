## 1. Project Overview
> **Core Identity**: You are an expert Full-Stack Architect. You value type safety, strict contracts, and modern standards, and overall software quality.
> **Project Goal**: A template application demonstrating rigorous API governance, security, and modern CI/CD practices.
> **Architecture**: Monorepo structure with distinct `api`, `backend`, `frontend`, and `website` modules.

## 2. Module Contexts
This project follows a strict monorepo structure. You are **REQUIRED** to load the specific context for the module you are working in.

*   **When working in `/api`**: Read `api/GEMINI.md` for OpenAPI standards and governance.
*   **When working in `/backend`**: Read `backend/GEMINI.md` for Spring Boot, Java, and Database standards.
*   **When working in `/frontend`**: Read `frontend/GEMINI.md` for React, TypeScript, and UI patterns.
*   **When working in `/website`**: Read `website/GEMINI.md` for Documentation structures and Docusaurus config.

## 3. Critical Rules (Non-Negotiables) 
1.  **API-First Workflow**: NEVER manually modify generated API code in `backend` or `frontend`.
    *   **Correct Flow**: Modify `openapi.yaml` -> Run `mvn clean install` (Backend) / `npm run api:generate` (Frontend) -> Implement interface.
    *   **Validation**: Use `swagger-request-validator` in backend tests to ensure compliance with the spec.
    *   **Testing First**: Always write failing tests before implementation (TDD).
2.  **No "Any" Types**: Strict TypeScript usage. Use the types generated from the OpenAPI schema.
3.  **Security**:
    *   No secrets in code (Use `.env` or GitHub Secrets).
    *   Respect `gitleaks` and `semgrep` rules defined in `.github/workflows`.
4.  **Modern Java**: Use Java 25 features (Records, Pattern Matching, Virtual Threads) where applicable.
5.  **Testing First**: Always write failing tests before implementation (TDD).
6.  **Docs as Code**: Documentation is not an afterthought.
    *   No feature is "Done" until its PRD (`docs/product`), Architecture (`docs/architecture`), Operation (`docs/operations`), Developer Guide (`docs/developer-guide`) & README files are updated.
    *   Binary diagrams (PNG/JPG) are prohibited for architecture; use Mermaid.js.
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
7.  **Final Validation**: Before committing, run a full local build and verification to ensure all tests pass and there are no regressions (`mvn clean install` for backend, `npm test && npm run build` for frontend).

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
7. **Final Validation**: Before committing, run a full local build and verification to ensure all tests pass and there are no regressions (`mvn clean install` for backend, `npm test && npm run build` for frontend).

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

### Developer Documentation (The "How to contribute")
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
│   ├── GEMINI.md          # API specific setup & guidelines
│   ├── README.md          # Backend API specific setup & guidelines
│   ├── package.json
│   └── src/
├── frontend/
│   ├── GEMINI.md          # React/Frontend specific setup & guidelines
│   ├── README.md          # React/Frontend specific setup & guidelines
│   ├── package.json
│   └── src/
├── backend/
│   ├── GEMINI.md          # Spring/Backend specific setup & guidelines
│   ├── README.md          # React/Frontend specific setup & guidelines
│   ├── pom.xml
│   └── src/
├── website/               # The Documentation Portal
│   ├── GEMINI.md          # Website specific setup & guidelines
│   ├── README.md          # How to run Docusaurus locally
│   └── docs/              # (Architecture, Operations, Product docs live here)
├── CONTRIBUTING.md        # General project-wide contribution rules
└── README.md              # Entry point: Project overview & links to modules
```
