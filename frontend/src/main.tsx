import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import App from "./App.tsx";
import { initApiClient } from "./api/config";
import { AuthProvider } from "./features/auth";

initApiClient();

createRoot(document.getElementById("root")!).render(
    <StrictMode>
        <AuthProvider>
            <App />
        </AuthProvider>
    </StrictMode>,
);
