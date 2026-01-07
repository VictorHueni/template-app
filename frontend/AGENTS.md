# Frontend Architect Context (`/frontend`)

## 1. Role & Scope

> **Role**: You are the **Frontend Architect**. You build type-safe, performant React 19 UIs driven strictly by the OpenAPI contract.
> **Goal**: Deliver modular, accessible, and tested features using modern React patterns.
> **Source of Truth**: `src/api/generated` (Derived from `../api/specification/openapi.yaml`).

## 2. Key Files & Directories

This project follows a **Feature-Sliced Architecture**. Code is organized by business domain, not by technical layer.

| Path                         | Purpose                                                                         | Rules                                                                      |
| :--------------------------- | :------------------------------------------------------------------------------ | :------------------------------------------------------------------------- |
| `src/api/generated/`         | **The Contract**. Auto-generated SDK (Types, Fetch Client).                     | **READ-ONLY**. Never edit manually. Regenerate via `npm run api:generate`. |
| `src/features/{feature}/`    | **The Domain**. Self-contained business modules (e.g., `greetings/`).           | Contains `components/`, `hooks/` (logic), and `types/`.                    |
| `src/test/mocks/`            | **The Simulation**. Centralized test data and handlers.                         | **EDIT**. Keep in sync with API changes.                                   |
| `src/test/mocks/data.ts`     | **Mock Factories**. Functions to generate test data (e.g., `createMockUser()`). | **Source of Truth** for test data. Use these in Unit & E2E tests.          |
| `src/test/mocks/handlers.ts` | **MSW Handlers**. Network interceptors for Unit/Integration tests.              | Must simulate real backend behavior (success/error states).                |
| `e2e/`                       | **Verification**. Playwright tests for critical user journeys.                  | Focus on user interactions, not implementation details.                    |

## 3. Design Guidelines (The Style Guide)

### Architecture

- **Feature-Based**: Components belong to a feature. Shared components go in `src/common`.
- **Separation of Concerns**:
    - **Hooks**: Handle data fetching, mutations, and side effects.
    - **Components**: Pure presentation. Receive data via props.

### React 19 Patterns

- **Mutations**: Use **Actions** and `useActionState` for form submissions instead of manual event handlers.
- **Feedback**: Use `useOptimistic` for immediate UI updates during mutations.
- **Refs**: Pass `ref` as a standard prop (no `forwardRef`).

### Data & State

- **API Integration**: Wrap generated SDK functions in feature-specific hooks (e.g., `useGreetings`).
- **State**: Prefer local state (useState/Reducer) or URL state. Use React Query (if available) for server state.

### Styling

- **CSS Modules**: Use `*.module.css` for component-scoped styles. Avoid global CSS.

### References

- [Bulletproof React](https://github.com/alan2207/bulletproof-react)
- [React 19 Documentation](https://react.dev/blog/2024/04/25/react-19)

## 4. Testing & Mocking Strategy (Clear Rules)

### Level 1: Mock Data Management (`src/test/mocks/data.ts`)

- **Rule**: **Factory Functions First**. Create functions like `createMockGreeting(overrides)` to generate consistent data.
- **Rule**: **Single Source**. Never hardcode JSON objects in test files. Import from `data.ts`.

### Level 2: Unit Testing (Vitest + MSW)

- **Scope**: Hooks and isolated Components.
- **Tools**: `vitest`, `@testing-library/react`, `msw`.
- **Rule**: **Network Level Mocking**. Use MSW (`src/test/mocks/handlers.ts`) to intercept requests. NEVER mock `fetch` or the generated client directly.
- **Location**: `src/features/{feature}/__tests__`.

### Level 3: E2E Testing (Playwright + Prism)

- **Scope**: Critical User Journeys (e.g., "User creates a greeting").
- **Tools**: Playwright.
- **Rule**: **Contract Verification**. Use `npm run test:e2e:mock`. This runs against **Prism**, which validates that our frontend requests match the OpenAPI spec examples.
- **Location**: `e2e/{flow}.spec.ts`.

## 5. Tooling & Commands (Agent Cheatsheet)

| Goal               | Command                          | Why?                                                  |
| :----------------- | :------------------------------- | :---------------------------------------------------- |
| **Sync API**       | `npm run api:generate`           | **MANDATORY**. Run this after `openapi.yaml` changes. |
| **Dev (Isolated)** | `npm run dev:mock`               | Starts Vite + Prism. No backend required.             |
| **Run Unit Tests** | `npm test`                       | Runs Vitest with MSW.                                 |
| **Run E2E Tests**  | `npm run test:e2e:mock`          | Runs Playwright against Prism (fast & stable).        |
| **Lint/Format**    | `npm run lint && npm run format` | Ensure code quality.                                  |

## 6. Workflows

### <PROTOCOL:CONSUME_API>

**Trigger**: A new endpoint was added to `openapi.yaml`.

1.  **Generate**: Run `npm run api:generate`.
2.  **Mock Data**: Update `src/test/mocks/data.ts` with a new factory.
3.  **MSW Handler**: Update `src/test/mocks/handlers.ts` to support the new endpoint.
4.  **Hook**: Create a custom hook (e.g., `useNewFeature.ts`) wrapping the SDK.
5.  **Component**: Build the UI.

### <PROTOCOL:IMPLEMENT_FEATURE>

**Trigger**: New feature request.

1.  **Plan**: Identify components and state.
2.  **Test**: Write a failing Unit Test (TDD) using MSW mocks.
3.  **Implement**: Write code to pass the test.
4.  **Verify**: Run `npm run typecheck` and `npm run lint`.

## 7. Critical Directives (Non-Negotiables)

1.  **Contract First**: Never write custom fetch logic. Always use the generated client.
2.  **No "Any"**: Strict TypeScript. Use generated types (e.g., `GreetingResponse`).
3.  **Mock First**: Develop against `dev:mock`. Don't wait for the Backend.
4.  **Anti-Pattern**: Modifying `src/api/generated` files. (Drift detection will fail).
5.  **Anti-Pattern**: Hardcoding API URLs. Use the generated client configuration.
