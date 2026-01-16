import { defineConfig } from "vite";
import react from "@vitejs/plugin-react-swc";

// https://vite.dev/config/
// https://vite.dev/config/
export default defineConfig(({ mode }) => ({
    plugins: [react()],
    server: {
        proxy:
            mode === "development"
                ? {
                      "/login-options": {
                          target:
                              process.env.VITE_AUTH_PROXY_TARGET ||
                              process.env.VITE_PROXY_TARGET ||
                              "http://localhost:8080",
                          changeOrigin: true,
                      },
                      "/oauth2": {
                          target:
                              process.env.VITE_AUTH_PROXY_TARGET ||
                              process.env.VITE_PROXY_TARGET ||
                              "http://localhost:8080",
                          changeOrigin: true,
                      },
                      "/login": {
                          target:
                              process.env.VITE_AUTH_PROXY_TARGET ||
                              process.env.VITE_PROXY_TARGET ||
                              "http://localhost:8080",
                          changeOrigin: true,
                      },
                      "/logout": {
                          target:
                              process.env.VITE_AUTH_PROXY_TARGET ||
                              process.env.VITE_PROXY_TARGET ||
                              "http://localhost:8080",
                          changeOrigin: true,
                      },
                      "/api": {
                          target: process.env.VITE_PROXY_TARGET || "http://localhost:8080",
                          changeOrigin: true,
                          rewrite:
                              process.env.VITE_USE_PRISM === "true"
                                  ? (path) => path.replace(/^\/api/, "")
                                  : undefined,
                      },
                  }
                : undefined,
    },
    build: { outDir: "dist" },
    base: "/",
    define: {
        __API_URL__: JSON.stringify(process.env.VITE_API_URL || "/api"),
    },
}));
