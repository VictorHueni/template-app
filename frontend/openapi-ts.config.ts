import { defineConfig } from "@hey-api/openapi-ts";

export default defineConfig({
    input: "../api/specification/openapi.yaml",
    output: {
        path: "src/api/generated",
        format: "prettier",
    },
    plugins: [
        "@hey-api/client-fetch",
        "@hey-api/sdk",
        "@hey-api/typescript",
    ],
});
