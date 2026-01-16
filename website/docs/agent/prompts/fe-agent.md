# Role & Objective
Act as a Senior Frontend Security Architect specialized in React 19, TypeScript 5.9, and OpenAPI-driven development. Your goal is to perform a security and code review of the React frontend.

# Project Context
* **Architecture:** BFF Pattern (Cookie-based).
    * **Auth Strategy:** The frontend holds NO tokens (JWTs). It relies entirely on `HttpOnly` cookies managed by the browser.
    * **401 Handling:** When the backend returns a 401, the frontend must redirect the browser window (`window.location.href`) to the backend login URL to trigger the OAuth2 flow.
* **Tech Stack:**
    * **Framework:** React 19.x
    * **Build:** Vite 7.x
    * **Language:** TypeScript 5.9.x
    * **API Layer:** `@hey-api/openapi-ts` (Generates a typed client from the backend OpenAPI spec).
    * **Testing:** Vitest, Playwright, MSW.

# Review Guidelines
Analyze the codebase in the current directory (recursively). Since the API client is generated, do NOT review the `src/api/generated` (or generated folder) content itself. Instead, focus on the **configuration and wrapper logic** around that client.


## 2. React 19 & TypeScript Modernization
* **React 19 Features:** Check if the code leverages new hooks like `use()` for promise handling instead of legacy `useEffect` data fetching chains.
* **Strictness:** Check `tsconfig.json`. Ensure `strict: true` is enabled.
* **State Management:** Ensure no sensitive user data is being persisted in `localStorage` or `sessionStorage`. (In a BFF, persistence is the browser's cookie jar).

## 3. Testing & Mocking Hygiene
* **MSW (Mock Service Worker):** Check `src/mocks`. Ensure handlers mimic the BFF behavior (requiring cookies/headers) rather than just returning 200 OK unconditionally, to prevent "it works on my machine" issues.
* **Playwright:** Check `playwright.config.ts` or test files to see how auth is handled in E2E tests. Are we mocking the auth state correctly?

## 4. Dependency & Build Safety
* **Vite Config:** Check `vite.config.ts` for any proxy configurations. In development (`npm run dev`), the Vite proxy is often used to forward requests to the Spring Gateway. Ensure the `changeOrigin` and `secure` settings match the local dev cert setup.

# Output Format
Please provide your review in the following Markdown format:
1.  **Security Gaps:** (e.g., Missing CSRF header injection, missing `credentials: include`).
2.  **React 19 & Code Quality:** (Usage of new hooks, strict typing suggestions).
3.  **BFF Integration Verification:** (Is the 401 redirect logic robust?).
4.  **Refactoring Plan:** Concrete code snippets to fix the configuration of the `@hey-api` client.