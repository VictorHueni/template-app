import { defineConfig, devices } from "@playwright/test";

/**
 * Playwright configuration for running E2E tests against Prism mock server.
 *
 * This configuration enables fast, isolated E2E testing without requiring:
 * - Backend server running
 * - Database setup
 * - Test data seeding
 *
 * Benefits:
 * - Faster test execution (~30s vs ~60s with real backend)
 * - Consistent, predictable test data (from OpenAPI examples)
 * - No backend startup time
 * - Perfect for CI pipelines
 *
 * Usage:
 *   npm run test:e2e:mock
 *
 * vs regular E2E tests (with real backend):
 *   npm run test:e2e
 */

const DEV_PORT = 5173; // Vite dev server default
const baseURL = `http://localhost:${DEV_PORT}`;

export default defineConfig({
    testDir: "./e2e",
    timeout: 30_000,
    expect: {
        timeout: 5_000,
    },
    fullyParallel: true,
    retries: process.env.CI ? 2 : 0,
    reporter: [["list"]],

    use: {
        baseURL: baseURL,
        trace: "on-first-retry",
    },

    /**
     * Start Prism mock server + Vite dev server before running tests.
     *
     * This automatically:
    * 1. Starts Prism on port 4010 (serving OpenAPI examples)
     * 2. Starts Vite on port 5173 (with proxy to Prism)
     * 3. Waits for both to be ready
     * 4. Runs tests
     * 5. Stops both servers when done
     */
    webServer: {
        command: "npm run dev:mock",
        url: baseURL,
        reuseExistingServer: !process.env.CI,
        timeout: 60_000, // Allow time for both Prism and Vite to start
    },

    projects: [
        {
            name: "chromium",
            use: { ...devices["Desktop Chrome"] },
        },
        // Uncomment for cross-browser testing against mocks
        // {
        //     name: "firefox",
        //     use: { ...devices["Desktop Firefox"] },
        // },
        // {
        //     name: "webkit",
        //     use: { ...devices["Desktop Safari"] },
        // },
    ],
});
