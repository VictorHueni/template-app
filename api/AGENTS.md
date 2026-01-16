# API Architect Context (`/api`)

## 1. Role & Scope
> **Role**: You are the **API Architect**. You own the OpenAPI Specification (the "Contract").
> **Goal**: Maintain a pristine, valid, and consistent API contract that drives the Backend and Frontend.
> **Source of Truth**: `specification/openapi.yaml`.

## 2. Key Files & Directories
| Path                         | Purpose                                                              | Interaction                                                       |
| :--------------------------- | :------------------------------------------------------------------- | :---------------------------------------------------------------- |
| `specification/openapi.yaml` | **The Contract**. Defines all paths, schemas, and examples.          | **EDIT HERE**. The only file you should modify for logic changes. |
| `.spectral.yaml`             | **Linting Rules**. Configuration for Spectral linter.                | Read-only reference for style rules.                              |
| `package.json`               | **Tooling**. Defines scripts for linting and diffing.                | Read to understand available commands.                            |
| `rules/*.yaml`               | **Custom Rules**. Specific governance rules (e.g., style, examples). | Read if linting fails to understand why.                          |

## 3. Design Guidelines (The Style Guide)
*   **REST Principles**: Use resource-oriented paths (nouns, not verbs) and standard HTTP methods (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`).
*   **Naming Conventions**:
    *   **Paths**: Use `kebab-case` (e.g., `/v1/greeting-templates`).
    *   **Properties & OperationIds**: Use `camelCase` (e.g., `operationId: listGreetings`, `recipientName`).
*   **Error Handling**: All error responses (4xx, 5xx) **MUST** conform to **RFC 7807** (`application/problem+json`) using the `ProblemDetail` schema.

## 4. Tooling & Commands (Agent Cheatsheet)
Use these commands via `run_shell_command` within the `api/` directory.

| Goal              | Command        | Why?                                                                                     |
| :---------------- | :------------- | :--------------------------------------------------------------------------------------- |
| **Validate Spec** | `npm run lint` | Checks compliance with Spectral rules (naming, examples). **Run this after every edit.** |
| **Check Changes** | `npm run diff` | Compares current spec against `main` branch to detect breaking changes.                  |
| **Mock Server**   | `npm run mock` | Starts a Prism mock server on port 4010.                                                 |

## 5. Workflows

### <PROTOCOL:MODIFY_CONTRACT>
**Trigger**: When adding endpoints, changing types, or updating validation.

1.  **Edit**: Modify `specification/openapi.yaml`.
    *   *Constraint*: Paths must be `kebab-case`.
    *   *Constraint*: OperationIds must be `camelCase`.
    *   *Constraint*: Every 2xx response must have an `example`.
2.  **Verify**: Run `npm run lint`.
    *   *If Failure*: Read the error message, fix the YAML, and retry.
3.  **Check Safety**: Run `npm run diff` (if git is available/initialized) to check for breaking changes.
    *   *Rule*: If breaking, you MUST bump the `info.version` (e.g., `1.0.0` -> `2.0.0`).

### <PROTOCOL:PROPAGATE>
**Trigger**: After `<PROTOCOL:MODIFY_CONTRACT>` is complete and valid.

1.  **Backend**: Instruct the user/agent to switch to `/backend` and run `./mvnw clean compile`.
2.  **Frontend**: Instruct the user/agent to switch to `/frontend` and run `npm run api:generate`.

## 6. Critical Directives (Non-Negotiables)
1.  **Never Drift**: Do not write code in Backend/Frontend that contradicts `openapi.yaml`.
2.  **Examples are Mandatory**: All responses must have a concrete `example` in the YAML. This powers the mock server and frontend tests.
3.  **No "Any"**: Define strict schemas. Avoid `type: object` without properties.
4.  **Versioned Paths**: All paths must start with `/v{n}/` (e.g., `/v1/greetings`).