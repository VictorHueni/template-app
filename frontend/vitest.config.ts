import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react-swc";

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
        include: ["src/**/*.{test,spec}.{ts,tsx}"],
        exclude: ["e2e/**", "node_modules/**", "dist/**", ".playwright/**"],
        // Required for MSW with jsdom - enables localStorage
        environmentOptions: {
            jsdom: {
                url: "http://localhost:3000",
            },
        },
    },
});
