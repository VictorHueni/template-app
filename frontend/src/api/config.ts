/**
 * API Configuration Module
 *
 * This module provides centralized configuration for all API clients.
 * It handles:
 * - Base path configuration (environment-aware)
 * - Authentication token management
 * - Creating pre-configured API instances
 *
 * WHY THIS PATTERN?
 * -----------------
 * 1. Single source of truth: All API configuration in one place
 * 2. Environment flexibility: Easy to switch between local/dev/prod
 * 3. Token management: Centralized authentication logic
 * 4. Testability: Easy to mock the entire API layer
 */

import { Configuration, GreetingsApi } from "./generated";

/**
 * Get the demo authentication token for development.
 * This should NEVER be used in production.
 *
 * Priority:
 * 1. VITE_DEMO_TOKEN from .env (allows per-developer customization)
 * 2. Hardcoded fallback for quick setup
 *
 * In a real application, tokens would come from:
 * - OAuth/OIDC flow
 * - Session storage after login
 * - Auth context provider
 *
 * SECURITY NOTE: This is a mock JWT with obvious fake values.
 * Real production tokens come from OAuth/OIDC providers.
 */
function getDemoToken(): string {
    // Check environment variable first
    const envToken = import.meta.env.VITE_DEMO_TOKEN;
    if (envToken) {
        return envToken;
    }

    // Fallback to hardcoded demo token
    // This is a mock JWT with "demo-signature" suffix and impossible expiration (year 2030)
    // NOT a real secret - see .gitleaks.toml and .gitleaksignore for allowlist rules
    // nosemgrep: generic.secrets.security.detected-jwt-token, generic.secrets.security.detected-generic-secret
    return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJkZW1vLXVzZXIiLCJuYW1lIjoiRGVtbyBVc2VyIiwiaWF0IjoxNzMzNjY1NjAwLCJleHAiOjE4OTk5OTk5OTl9.demo-signature";
}

const DEMO_AUTH_TOKEN = getDemoToken();

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
    const baseUrl = import.meta.env.VITE_API_URL ?? "";
    return `${baseUrl}/api`;
}

/**
 * Computed API base path for display purposes.
 * Shows the full path prefix for UI information.
 */
export const API_BASE_PATH = "/api/v1";

/**
 * Check if authentication is enabled.
 * Can be toggled via environment variable for testing unauthenticated flows.
 */
export function isAuthEnabled(): boolean {
    return import.meta.env.VITE_AUTH_ENABLED !== "false";
}

/**
 * Get the current authentication token.
 * In this demo, returns a hardcoded token.
 *
 * TODO: Replace with real auth implementation:
 * - Read from auth context
 * - Refresh token if expired
 * - Redirect to login if no token
 */
export function getAuthToken(): string | null {
    if (!isAuthEnabled()) {
        return null;
    }
    // In production, this would fetch from auth state/storage
    return DEMO_AUTH_TOKEN;
}

/**
 * Create the API configuration object.
 * This is used by all generated API clients.
 *
 * @param includeAuth - Whether to include authentication headers
 *                      Set to false for public endpoints to avoid sending unnecessary headers
 */
export function createApiConfiguration(includeAuth = true): Configuration {
    return new Configuration({
        basePath: getApiBasePath(),
        // accessToken is called for each request, allowing dynamic token refresh
        accessToken: includeAuth
            ? async () => {
                  const token = getAuthToken();
                  return token ?? "";
              }
            : undefined,
    });
}

/**
 * Pre-configured API client instances.
 * Use these throughout the application for consistency.
 *
 * WHY SINGLETON INSTANCES?
 * - Consistent configuration across the app
 * - Easier to mock in tests
 * - No accidental misconfiguration
 */

// API instance for public endpoints (no auth header sent)
const publicConfig = createApiConfiguration(false);

// API instance for authenticated endpoints (includes Bearer token)
const authenticatedConfig = createApiConfiguration(true);

/**
 * Greetings API client for public operations (GET list, GET by ID)
 */
export const greetingsApiPublic = new GreetingsApi(publicConfig);

/**
 * Greetings API client for authenticated operations (POST, PUT, PATCH, DELETE)
 */
export const greetingsApiAuth = new GreetingsApi(authenticatedConfig);

/**
 * Default export: authenticated API for convenience
 * Most operations in a real app require authentication
 */
export const greetingsApi = greetingsApiAuth;

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
} from "./generated";
