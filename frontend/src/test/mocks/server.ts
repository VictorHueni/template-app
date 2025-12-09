/**
 * MSW Server Setup for Tests
 *
 * This module configures the MSW server for use in Vitest.
 * It provides the server instance and utility functions for tests.
 */

import { setupServer } from "msw/node";
import { handlers, resetGreetingsStore } from "./handlers";

/**
 * Create the MSW server with default handlers.
 * The server intercepts all HTTP requests matching our handlers.
 */
export const server = setupServer(...handlers);

/**
 * Reset all handlers and data store.
 * Call this in beforeEach() to ensure test isolation.
 */
export function resetServer(): void {
    server.resetHandlers();
    resetGreetingsStore();
}

// Re-export utilities for convenience
export { resetGreetingsStore, getGreetingsStore } from "./handlers";
export * from "./data";
