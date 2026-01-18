/**
 * API Configuration Module
 *
 * This module provides centralized configuration for all API clients.
 * It handles:
 * - Base path configuration (environment-aware)
 * - Authentication token management
 * - Configuring the hey-api client instance
 *
 * WHY THIS PATTERN?
 * -----------------
 * 1. Single source of truth: All API configuration in one place
 * 2. Environment flexibility: Easy to switch between local/dev/prod
 * 3. Token management: Centralized authentication logic
 * 4. Testability: Easy to mock the entire API layer
 */

import { client } from "./generated/client.gen";

/**
 * Get the API base path from environment variables.
 * Falls back to empty string for relative URLs (works with Vite proxy).
 *
 * Environment variable: VITE_API_URL
 * - Development: Usually empty (uses Vite proxy to avoid CORS)
 * - Production: Full URL like "https://api.example.com"
 *
 * Note: The generated API client already includes "/v1" in the paths,
 * so we only add "/api" here to match the server URL from the OpenAPI spec.
 */
export function getApiBasePath(): string {
    const configured = import.meta.env.VITE_API_URL;

    if (configured) {
        return configured.endsWith("/api") ? configured : `${configured}/api`;
    }

    // Ensure absolute URL for environments where `Request` requires it (e.g., Vitest/node).
    // In the browser, same-origin absolute URLs work with Vite proxy and production nginx.
    const origin =
        typeof globalThis !== "undefined" && globalThis.location
            ? globalThis.location.origin
            : "http://localhost";
    return new URL("/api", origin).toString();
}

/**
 * Computed API base path for display purposes.
 * Shows the full path prefix for UI information.
 */
export const API_BASE_PATH = "/api/v1";

function getCookieValue(name: string): string | undefined {
    if (typeof document === "undefined") {
        return undefined;
    }

    const cookie = document.cookie
        .split(";")
        .map((c) => c.trim())
        .find((c) => c.startsWith(`${name}=`));

    if (!cookie) {
        return undefined;
    }

    return decodeURIComponent(cookie.substring(name.length + 1));
}

function isStateChangingMethod(method: string): boolean {
    return ["POST", "PUT", "PATCH", "DELETE"].includes(method.toUpperCase());
}

let interceptorsRegistered = false;

/**
 * Internal use only: resets the interceptors flag for testing
 */
export function resetInterceptors() {
    interceptorsRegistered = false;
}

export interface InitApiClientOptions {
    fetch?: typeof globalThis.fetch;
}

/**
 * Initialize the generated API client.
 *
 * For the BFF architecture, the browser authenticates via HttpOnly session cookies.
 * Therefore we must:
 * - send requests with `credentials: "include"`
 * - send the CSRF header for state-changing requests (Spring Security defaults)
 */
export function initApiClient(options: InitApiClientOptions = {}): void {
    client.setConfig({
        baseUrl: getApiBasePath(),
        credentials: "include",
        ...(options.fetch ? { fetch: options.fetch } : {}),
    });

    if (interceptorsRegistered) {
        return;
    }
    interceptorsRegistered = true;

    client.interceptors.request.use((request) => {
        if (!isStateChangingMethod(request.method)) {
            return request;
        }

        const csrfToken = getCookieValue("XSRF-TOKEN");
        if (csrfToken) {
            request.headers.set("X-XSRF-TOKEN", csrfToken);
        }

        return request;
    });

    client.interceptors.response.use((response) => {
        if (
            response.status === 401 &&
            typeof globalThis !== "undefined" &&
            globalThis.dispatchEvent
        ) {
            globalThis.dispatchEvent(new CustomEvent("auth:session-expired"));
        }
        return response;
    });
}

/**
 * Export the configured client for direct access if needed
 */
export { client };

/**
 * Re-export types for convenience
 */
export type {
    GreetingResponse,
    GreetingPage,
    PageMeta,
    CreateGreetingRequest,
    UpdateGreetingRequest,
    PatchGreetingRequest,
    ProblemDetail,
    UserInfoResponse,
} from "./generated";

/**
 * Re-export SDK functions for convenience
 */
export {
    listGreetings,
    getGreeting,
    createGreeting,
    updateGreeting,
    patchGreeting,
    deleteGreeting,
    getCurrentUser,
} from "./generated";
