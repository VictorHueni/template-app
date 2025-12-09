import js from "@eslint/js";
import globals from "globals";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";
import tseslint from "typescript-eslint";
import { defineConfig, globalIgnores } from "eslint/config";

export default defineConfig([
    // Global ignores for linting
    // Follow ESLint flat config best-practice: use `globalIgnores` for ignored paths
    // (replaces legacy `.eslintignore`). Also ignore generated OpenAPI client.
    globalIgnores(["dist", "node_modules", "src/api/generated", "coverage"]),

    {
        files: ["**/*.{ts,tsx}"],

        extends: [
            js.configs.recommended,
            ...tseslint.configs.recommended,
            reactHooks.configs["recommended-latest"],
            reactRefresh.configs.vite,
        ],

        languageOptions: {
            ecmaVersion: 2020,
            sourceType: "module",
            globals: {
                ...globals.browser,
                ...globals.es2021,
            },
        },

        rules: {
            "no-console": "warn",
            "no-debugger": "error",
        },
    },
]);
