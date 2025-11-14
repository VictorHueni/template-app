import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react-swc";
import { playwright } from "@vitest/browser-playwright";

export default defineConfig({
    plugins: [react()],
    test: {
        environment: "jsdom",
        globals: true,
        setupFiles: "./src/test/setupTests.ts",
        coverage: {
            reporter: ["text", "lcov", "html"],
            reportsDirectory: "./coverage",
        },
        browser: {
            enabled: true,
            provider: playwright(),
            instances: [{ browser: "chromium" }],
        },
    },
});
