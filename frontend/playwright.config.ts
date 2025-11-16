import { defineConfig, devices } from "@playwright/test";

const PORT = 4173; // Vite preview default

const baseURL = process.env.BASE_URL ?? `http://localhost:${PORT}`;

export default defineConfig({
    testDir: "./e2e",
    timeout: 30_000,
    expect: {
        timeout: 5_000,
    },
    fullyParallel: true,
    retries: 0,
    reporter: [["list"]],

    use: {
        baseURL: baseURL,
        trace: "on-first-retry",
    },

    webServer: {
        // Locally: this will build then start preview
        // In CI: you can still pre-build in a separate step for speed, but this is fine too
        command: "npm run preview -- --port 4173",
        url: baseURL,
        reuseExistingServer: !process.env.CI,
        timeout: 60_000,
    },

    projects: [
        {
            name: "chromium",
            use: { ...devices["Desktop Chrome"] },
        },
        // you can add firefox/webkit later if you want
        // {
        //   name: "firefox",
        //   use: { ...devices["Desktop Firefox"] },
        // },
        // {
        //   name: "webkit",
        //   use: { ...devices["Desktop Safari"] },
        // },
    ],
});
