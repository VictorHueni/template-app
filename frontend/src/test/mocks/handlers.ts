import { http, HttpResponse, delay, type HttpResponseResolver } from "msw";
import { mockGreetings, createMockGreeting, createMockGreetingPage, mockErrors } from "./data";
import type { GreetingResponse, UserInfoResponse } from "../../api/generated";

/**
 * Mock authentication state for BFF testing.
 */
let mockAuthenticatedUser: UserInfoResponse | null = null;

export function setMockAuthenticatedUser(user: UserInfoResponse | null): void {
    mockAuthenticatedUser = user;
}

export function getMockAuthenticatedUser(): UserInfoResponse | null {
    return mockAuthenticatedUser;
}

export function resetMockAuth(): void {
    mockAuthenticatedUser = null;
}

const API_BASE = "/api/v1";

/**
 * In-memory store for greetings during tests.
 */
let greetingsStore: GreetingResponse[] = [...mockGreetings];

export function resetGreetingsStore(): void {
    greetingsStore = [...mockGreetings];
}

export function getGreetingsStore(): GreetingResponse[] {
    return [...greetingsStore];
}

let nextId = 506979954615550000;

// --- Helpers for DRY handlers ---

/**
 * Adds a standard delay to any resolver.
 */
function withDelay(resolver: HttpResponseResolver): HttpResponseResolver {
    return async (input) => {
        await delay(50);
        return resolver(input);
    };
}

/**
 * Enforces authentication for a resolver.
 * Returns 401 if no mock user is set.
 */
function withAuth(resolver: HttpResponseResolver): HttpResponseResolver {
    return async (input) => {
        if (!mockAuthenticatedUser) {
            return HttpResponse.json(mockErrors.unauthorized, { status: 401 });
        }
        return resolver(input);
    };
}

/**
 * Helper to find a greeting by ID in the store.
 * Returns 404 if not found, otherwise executes the callback with the greeting and its index.
 */
function withGreeting(
    resolver: (args: {
        greeting: GreetingResponse;
        index: number;
        params: Record<string, string | readonly string[]>;
        request: Request;
    }) => HttpResponse | Promise<HttpResponse>,
): HttpResponseResolver {
    return async ({ params, request }) => {
        const id = Number(params.id);
        const index = greetingsStore.findIndex((g) => g.id === id);

        if (index === -1) {
            return HttpResponse.json(mockErrors.notFound, { status: 404 });
        }

        return resolver({ greeting: greetingsStore[index], index, params, request });
    };
}

/**
 * Common validation for greeting messages.
 */
function validateGreeting(body: { message?: string }): HttpResponse | null {
    if (!body.message || body.message.trim() === "") {
        return HttpResponse.json(
            {
                ...mockErrors.validationError,
                errors: { message: "must not be blank" },
            },
            { status: 400 },
        );
    }
    return null;
}

// --- Request Handlers ---

export const handlers = [
    /**
     * GET /api/v1/me
     */
    http.get(
        `${API_BASE}/me`,
        withDelay(async () => {
            if (!mockAuthenticatedUser) {
                return HttpResponse.json(mockErrors.unauthorized, { status: 401 });
            }
            return HttpResponse.json(mockAuthenticatedUser);
        }),
    ),

    /**
     * GET /api/v1/greetings - List greetings (paginated)
     */
    http.get(
        `${API_BASE}/greetings`,
        withDelay(async ({ request }) => {
            const url = new URL(request.url);
            const page = parseInt(url.searchParams.get("page") ?? "0", 10);
            const size = parseInt(url.searchParams.get("size") ?? "20", 10);

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
    ),

    /**
     * GET /api/v1/greetings/:id
     */
    http.get(
        `${API_BASE}/greetings/:id`,
        withDelay(
            withGreeting(({ greeting }) => {
                return HttpResponse.json(greeting);
            }),
        ),
    ),

    /**
     * POST /api/v1/greetings
     */
    http.post(
        `${API_BASE}/greetings`,
        withDelay(
            withAuth(async ({ request }) => {
                const body = (await request.json()) as { message?: string; recipient?: string };
                const validationError = validateGreeting(body);
                if (validationError) return validationError;

                const newGreeting = createMockGreeting({
                    id: nextId++,
                    message: body.message!,
                    recipient: body.recipient,
                    createdAt: new Date().toISOString(),
                });

                greetingsStore.push(newGreeting);
                return HttpResponse.json(newGreeting, { status: 201 });
            }),
        ),
    ),

    /**
     * PUT /api/v1/greetings/:id
     */
    http.put(
        `${API_BASE}/greetings/:id`,
        withDelay(
            withAuth(
                withGreeting(async ({ index, request }) => {
                    const body = (await request.json()) as { message?: string; recipient?: string };
                    const validationError = validateGreeting(body);
                    if (validationError) return validationError;

                    greetingsStore[index] = {
                        ...greetingsStore[index],
                        message: body.message!,
                        recipient: body.recipient,
                    };

                    return HttpResponse.json(greetingsStore[index]);
                }),
            ),
        ),
    ),

    /**
     * PATCH /api/v1/greetings/:id
     */
    http.patch(
        `${API_BASE}/greetings/:id`,
        withDelay(
            withAuth(
                withGreeting(async ({ index, request }) => {
                    const body = (await request.json()) as { message?: string; recipient?: string };

                    if (body.message !== undefined) {
                        greetingsStore[index].message = body.message;
                    }
                    if (body.recipient !== undefined) {
                        greetingsStore[index].recipient = body.recipient;
                    }

                    return HttpResponse.json(greetingsStore[index]);
                }),
            ),
        ),
    ),

    /**
     * DELETE /api/v1/greetings/:id
     */
    http.delete(
        `${API_BASE}/greetings/:id`,
        withDelay(
            withAuth(
                withGreeting(({ index }) => {
                    greetingsStore.splice(index, 1);
                    return new HttpResponse(null, { status: 204 });
                }),
            ),
        ),
    ),
];