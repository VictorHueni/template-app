/**
 * MSW Request Handlers
 *
 * This module defines mock API handlers using MSW (Mock Service Worker).
 * These handlers intercept HTTP requests during tests, providing:
 * 1. Realistic API responses without a running backend
 * 2. Consistent test data
 * 3. Easy simulation of error scenarios
 *
 * WHY MSW?
 * --------
 * - Intercepts at the network level (works with any HTTP client)
 * - No need to mock fetch or the generated API client directly
 * - Tests exercise the full code path including serialization
 * - Same handlers can be used for browser development
 */

import { http, HttpResponse, delay } from "msw";
import { mockGreetings, createMockGreeting, createMockGreetingPage, mockErrors } from "./data";
import type { GreetingResponse, UserInfoResponse } from "../../api/generated";

/**
 * Mock authentication state for BFF testing.
 *
 * The BFF pattern uses HttpOnly session cookies, NOT Bearer tokens.
 * This configurable auth state simulates whether a user has a valid session.
 */
let mockAuthenticatedUser: UserInfoResponse | null = null;

/**
 * Set the authenticated user for mock requests.
 * Pass null to simulate an anonymous/logged-out user.
 */
export function setMockAuthenticatedUser(user: UserInfoResponse | null): void {
    mockAuthenticatedUser = user;
}

/**
 * Get the current mock authenticated user.
 */
export function getMockAuthenticatedUser(): UserInfoResponse | null {
    return mockAuthenticatedUser;
}

/**
 * Reset mock authentication state to anonymous.
 * Call this in beforeEach() along with resetGreetingsStore().
 */
export function resetMockAuth(): void {
    mockAuthenticatedUser = null;
}

/**
 * Base URL for API endpoints.
 * Using relative URL so it works with Vite's proxy in development.
 */
const API_BASE = "/api/v1";

/**
 * In-memory store for greetings during tests.
 * This allows testing CRUD operations with state persistence within a test.
 */
let greetingsStore: GreetingResponse[] = [...mockGreetings];

/**
 * Reset the greetings store to initial state.
 * Call this in beforeEach() to ensure test isolation.
 */
export function resetGreetingsStore(): void {
    greetingsStore = [...mockGreetings];
}

/**
 * Get the current state of the greetings store.
 * Useful for assertions in tests.
 */
export function getGreetingsStore(): GreetingResponse[] {
    return [...greetingsStore];
}

/**
 * Counter for generating unique IDs in tests.
 */
let nextId = 506979954615550000;

/**
 * Default request handlers for the Greetings API.
 * These simulate the backend behavior as defined in the OpenAPI spec.
 */
export const handlers = [
    /**
     * GET /api/v1/me - Get current user info
     * Returns authenticated user or 401 if not authenticated.
     * This endpoint is used by AuthProvider to check session state.
     */
    http.get(`${API_BASE}/me`, async () => {
        await delay(50);

        if (!mockAuthenticatedUser) {
            return HttpResponse.json(mockErrors.unauthorized, { status: 401 });
        }

        return HttpResponse.json(mockAuthenticatedUser);
    }),

    /**
     * GET /api/v1/greetings - List greetings (paginated)
     * Public endpoint - no authentication required
     */
    http.get(`${API_BASE}/greetings`, async ({ request }) => {
        // Add small delay to simulate network latency
        await delay(50);

        const url = new URL(request.url);
        const page = parseInt(url.searchParams.get("page") ?? "0", 10);
        const size = parseInt(url.searchParams.get("size") ?? "20", 10);

        // Calculate pagination
        const startIndex = page * size;
        const endIndex = startIndex + size;
        const paginatedData = greetingsStore.slice(startIndex, endIndex);

        return HttpResponse.json(
            createMockGreetingPage(paginatedData, {
                pageNumber: page,
                pageSize: size,
                totalElements: greetingsStore.length,
                totalPages: Math.ceil(greetingsStore.length / size),
            }),
        );
    }),

    /**
     * GET /api/v1/greetings/:id - Get a single greeting
     * Public endpoint - no authentication required
     */
    http.get(`${API_BASE}/greetings/:id`, async ({ params }) => {
        await delay(50);

        const id = Number(params.id);
        const greeting = greetingsStore.find((g) => g.id === id);

        if (!greeting) {
            return HttpResponse.json(mockErrors.notFound, { status: 404 });
        }

        return HttpResponse.json(greeting);
    }),

    /**
     * POST /api/v1/greetings - Create a new greeting
     * Requires authentication (session cookie in BFF pattern)
     */
    http.post(`${API_BASE}/greetings`, async ({ request }) => {
        await delay(50);

        // Check for authenticated session (BFF uses cookies, not Bearer tokens)
        if (!mockAuthenticatedUser) {
            return HttpResponse.json(mockErrors.unauthorized, { status: 401 });
        }

        // Parse request body
        const body = (await request.json()) as { message?: string; recipient?: string };

        // Validate required fields
        if (!body.message || body.message.trim() === "") {
            return HttpResponse.json(
                {
                    ...mockErrors.validationError,
                    errors: { message: "must not be blank" },
                },
                { status: 400 },
            );
        }

        // Create new greeting
        const newGreeting = createMockGreeting({
            id: nextId++,
            message: body.message,
            recipient: body.recipient,
            createdAt: new Date().toISOString(),
        });

        greetingsStore.push(newGreeting);

        return HttpResponse.json(newGreeting, { status: 201 });
    }),

    /**
     * PUT /api/v1/greetings/:id - Full update of a greeting
     * Requires authentication (session cookie in BFF pattern)
     */
    http.put(`${API_BASE}/greetings/:id`, async ({ params, request }) => {
        await delay(50);

        // Check for authenticated session (BFF uses cookies, not Bearer tokens)
        if (!mockAuthenticatedUser) {
            return HttpResponse.json(mockErrors.unauthorized, { status: 401 });
        }

        const id = Number(params.id);
        const index = greetingsStore.findIndex((g) => g.id === id);

        if (index === -1) {
            return HttpResponse.json(mockErrors.notFound, { status: 404 });
        }

        const body = (await request.json()) as { message?: string; recipient?: string };

        // Validate required fields for PUT (full replacement)
        if (!body.message || body.message.trim() === "") {
            return HttpResponse.json(
                {
                    ...mockErrors.validationError,
                    errors: { message: "must not be blank" },
                },
                { status: 400 },
            );
        }

        // Update greeting (full replacement, keeping id and createdAt)
        greetingsStore[index] = {
            ...greetingsStore[index],
            message: body.message,
            recipient: body.recipient,
        };

        return HttpResponse.json(greetingsStore[index]);
    }),

    /**
     * PATCH /api/v1/greetings/:id - Partial update of a greeting
     * Requires authentication (session cookie in BFF pattern)
     */
    http.patch(`${API_BASE}/greetings/:id`, async ({ params, request }) => {
        await delay(50);

        // Check for authenticated session (BFF uses cookies, not Bearer tokens)
        if (!mockAuthenticatedUser) {
            return HttpResponse.json(mockErrors.unauthorized, { status: 401 });
        }

        const id = Number(params.id);
        const index = greetingsStore.findIndex((g) => g.id === id);

        if (index === -1) {
            return HttpResponse.json(mockErrors.notFound, { status: 404 });
        }

        const body = (await request.json()) as { message?: string; recipient?: string };

        // Partial update - only update provided fields
        if (body.message !== undefined) {
            greetingsStore[index].message = body.message;
        }
        if (body.recipient !== undefined) {
            greetingsStore[index].recipient = body.recipient;
        }

        return HttpResponse.json(greetingsStore[index]);
    }),

    /**
     * DELETE /api/v1/greetings/:id - Delete a greeting
     * Requires authentication (session cookie in BFF pattern)
     */
    http.delete(`${API_BASE}/greetings/:id`, async ({ params }) => {
        await delay(50);

        // Check for authenticated session (BFF uses cookies, not Bearer tokens)
        if (!mockAuthenticatedUser) {
            return HttpResponse.json(mockErrors.unauthorized, { status: 401 });
        }

        const id = Number(params.id);
        const index = greetingsStore.findIndex((g) => g.id === id);

        if (index === -1) {
            return HttpResponse.json(mockErrors.notFound, { status: 404 });
        }

        greetingsStore.splice(index, 1);

        return new HttpResponse(null, { status: 204 });
    }),
];
