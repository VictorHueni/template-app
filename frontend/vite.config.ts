import { defineConfig } from "vite";
import react from "@vitejs/plugin-react-swc";

// https://vite.dev/config/
// https://vite.dev/config/
export default defineConfig(({ mode }) => ({
  plugins: [react()],
  server: {
    proxy:
      mode === "development"
        ? { "/api": "http://localhost:8080" } // local backend
        : undefined,
  },
  build: { outDir: "dist" },
  base: "/",
  define: {
    __API_URL__: JSON.stringify(process.env.VITE_API_URL || "/api"),
  },
}));
