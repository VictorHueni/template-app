import { configDefaults, defineConfig } from "vitest/config";
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
            exclude: [
                ...(configDefaults.coverage.exclude || []), // Keep default excludes (node_modules, etc.)
                "src/api/generated/**",             // Exclude generated API client
                "src/vite-env.d.ts",                // Exclude type definitions
                "src/main.tsx",                     // Exclude entry point (hard to unit test)
                "e2e/**"                            // Exclude e2e tests from unit coverage
            ]
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
