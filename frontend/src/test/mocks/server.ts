/**
 * MSW Server Setup for Tests
 *
 * This module configures the MSW server for use in Vitest.
 * It provides the server instance and utility functions for tests.
 */

import { setupServer } from "msw/node";
import {
    handlers,
    resetGreetingsStore,
    resetMockAuth,
    setMockAuthenticatedUser,
    getMockAuthenticatedUser,
} from "./handlers";

/**
 * Create the MSW server with default handlers.
 * The server intercepts all HTTP requests matching our handlers.
 */
export const server = setupServer(...handlers);

/**
 * Reset all handlers, data store, and authentication state.
 * Call this in beforeEach() to ensure test isolation.
 */
export function resetServer(): void {
    server.resetHandlers();
    resetGreetingsStore();
    resetMockAuth();
}

// Re-export utilities for convenience
export {
    resetGreetingsStore,
    getGreetingsStore,
    setMockAuthenticatedUser,
    getMockAuthenticatedUser,
    resetMockAuth,
} from "./handlers";
export * from "./data";
