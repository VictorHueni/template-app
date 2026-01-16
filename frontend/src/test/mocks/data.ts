/**
 * Mock Data for Testing
 *
 * This module provides consistent test data used across all tests.
 * Having mock data in one place ensures:
 * 1. Consistency across tests
 * 2. Easy updates when API schema changes
 * 3. Realistic data matching the actual API responses
 *
 * NOTE: IDs are strings to match the API schema (TSID serialized as string
 * to preserve precision for large 64-bit integers in JavaScript).
 */

import type {
    GreetingResponse,
    GreetingPage,
    PageMeta,
    ProblemDetail,
    UserInfoResponse,
} from "../../api/generated";

/**
 * Sample greetings for testing list views and CRUD operations.
 */
export const mockGreetings: GreetingResponse[] = [
    {
        id: "1001",
        reference: "GRE-2025-000001",
        message: "Hello, World!",
        recipient: "World",
        createdAt: "2025-01-15T10:30:00Z",
    },
    {
        id: "1002",
        reference: "GRE-2025-000002",
        message: "Welcome to the app!",
        recipient: "User",
        createdAt: "2025-01-15T11:00:00Z",
    },
    {
        id: "1003",
        reference: "GRE-2025-000003",
        message: "Greetings from the API!",
        recipient: "Developer",
        createdAt: "2025-01-15T11:30:00Z",
    },
];

/**
 * Factory function to create a single mock greeting.
 * Useful for creating custom test scenarios.
 */
export function createMockGreeting(overrides: Partial<GreetingResponse> = {}): GreetingResponse {
    return {
        id: "9999",
        reference: "GRE-2025-000099",
        message: "Test greeting",
        recipient: "Test User",
        createdAt: "2025-01-15T12:00:00Z",
        ...overrides,
    };
}

/**
 * Factory function to create page metadata.
 */
export function createMockPageMeta(overrides: Partial<PageMeta> = {}): PageMeta {
    return {
        pageNumber: 0,
        pageSize: 20,
        totalElements: mockGreetings.length,
        totalPages: 1,
        ...overrides,
    };
}

/**
 * Factory function to create a paginated greeting response.
 */
export function createMockGreetingPage(
    greetings: GreetingResponse[] = mockGreetings,
    metaOverrides: Partial<PageMeta> = {},
): GreetingPage {
    return {
        data: greetings,
        meta: createMockPageMeta({
            totalElements: greetings.length,
            totalPages: Math.ceil(greetings.length / 20),
            ...metaOverrides,
        }),
    };
}

/**
 * Factory function to create a ProblemDetail error response.
 * Follows RFC 7807 format used by the backend.
 */
export function createMockProblemDetail(
    overrides: Partial<ProblemDetail> & { errors?: Record<string, string> } = {},
): ProblemDetail & { errors?: Record<string, string> } {
    return {
        type: "https://api.example.com/problems/error",
        title: "Error",
        status: 400,
        detail: "Something went wrong",
        instance: "/api/v1/greetings",
        timestamp: "2025-01-15T12:00:00Z",
        traceId: "test-trace-id-00000000",
        ...overrides,
    };
}

/**
 * Common error scenarios for testing error handling.
 */
export const mockErrors = {
    notFound: createMockProblemDetail({
        type: "https://api.example.com/problems/not-found",
        title: "Not Found",
        status: 404,
        detail: "Greeting not found",
    }),

    validationError: createMockProblemDetail({
        type: "https://api.example.com/problems/validation-error",
        title: "Validation Error",
        status: 400,
        detail: "Request validation failed",
        errors: {
            message: "must not be blank",
        },
    }),

    unauthorized: createMockProblemDetail({
        type: "https://api.example.com/problems/unauthorized",
        title: "Unauthorized",
        status: 401,
        detail: "Authentication required",
    }),

    forbidden: createMockProblemDetail({
        type: "https://api.example.com/problems/forbidden",
        title: "Forbidden",
        status: 403,
        detail: "You do not have permission to perform this action",
    }),

    conflict: createMockProblemDetail({
        type: "https://api.example.com/problems/conflict",
        title: "Conflict",
        status: 409,
        detail: "Resource already exists",
    }),

    serverError: createMockProblemDetail({
        type: "https://api.example.com/problems/internal-error",
        title: "Internal Server Error",
        status: 500,
        detail: "An unexpected error occurred",
    }),
};

/**
 * Helper to create a mock successful API response.
 * Properly typed for hey-api SDK functions.
 */
export function mockApiSuccess<T>(data: T) {
    return {
        data,
        error: undefined as undefined,
        request: new Request("http://localhost/test"),
        response: new Response(),
    };
}

/**
 * Helper to create a mock error API response.
 * Properly typed for hey-api SDK functions.
 */
export function mockApiError(error: ProblemDetail) {
    return {
        data: undefined as undefined,
        error,
        request: new Request("http://localhost/test"),
        response: new Response(null, { status: error.status }),
    };
}

/**
 * Mock users for authentication testing.
 * These match the Keycloak realm users defined in template-realm.json.
 */
export const mockUsers: Record<string, UserInfoResponse> = {
    user: {
        id: "mock-user-1",
        username: "testuser",
        email: "test@example.com",
        roles: ["USER"],
    },
    admin: {
        id: "mock-admin-1",
        username: "testadmin",
        email: "admin@example.com",
        roles: ["USER", "ADMIN"],
    },
};
