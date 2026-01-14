import { describe, it, expect, beforeEach, vi } from "vitest";

import { initApiClient, client, createGreeting, listGreetings } from "./config";

function clearCookie(name: string) {
    document.cookie = `${name}=; Max-Age=0; path=/`;
}

describe("api/config initApiClient", () => {
    beforeEach(() => {
        vi.restoreAllMocks();
        clearCookie("XSRF-TOKEN");
    });

    it("sets baseUrl to /api and includes credentials", async () => {
        const fetchStub = vi.fn(async () =>
            new Response("{}", { status: 200, headers: { "Content-Type": "application/json" } }),
        );

        initApiClient({ fetch: fetchStub as unknown as typeof fetch });

        const config = client.getConfig();
        expect(config.baseUrl).toBe("http://localhost:3000/api");
        expect(config.credentials).toBe("include");
    });

    it("does not add Authorization header (BFF cookies)", async () => {
        let lastRequest: Request | undefined;

        const fetchStub = vi.fn(async (request: Request) => {
            lastRequest = request;
            return new Response(
                JSON.stringify({
                    id: "1004",
                    reference: "GRE-2025-000004",
                    message: "Hello",
                    recipient: "World",
                    createdAt: new Date().toISOString(),
                }),
                { status: 201, headers: { "Content-Type": "application/json" } },
            );
        });

        initApiClient({ fetch: fetchStub as unknown as typeof fetch });

        await createGreeting({
            body: {
                message: "Hello",
                recipient: "World",
            },
        });

        expect(fetchStub).toHaveBeenCalled();
        expect(lastRequest).toBeDefined();
        expect(lastRequest?.headers.get("Authorization")).toBeNull();
        expect(lastRequest?.credentials).toBe("include");
    });

    it("adds CSRF header for state-changing requests when cookie exists", async () => {
        document.cookie = "XSRF-TOKEN=test-csrf-token; path=/";

        let lastRequest: Request | undefined;

        const fetchStub = vi.fn(async (request: Request) => {
            lastRequest = request;
            return new Response(
                JSON.stringify({
                    id: "1004",
                    reference: "GRE-2025-000004",
                    message: "Hello",
                    recipient: "World",
                    createdAt: new Date().toISOString(),
                }),
                { status: 201, headers: { "Content-Type": "application/json" } },
            );
        });

        initApiClient({ fetch: fetchStub as unknown as typeof fetch });

        await createGreeting({
            body: {
                message: "Hello",
                recipient: "World",
            },
        });

        expect(lastRequest?.headers.get("X-XSRF-TOKEN")).toBe("test-csrf-token");
    });

    it("does not add CSRF header for GET requests", async () => {
        document.cookie = "XSRF-TOKEN=test-csrf-token; path=/";

        let lastRequest: Request | undefined;

        const fetchStub = vi.fn(async (request: Request) => {
            lastRequest = request;
            return new Response(
                JSON.stringify({
                    data: [],
                    meta: { pageNumber: 0, pageSize: 5, totalElements: 0, totalPages: 0 },
                }),
                { status: 200, headers: { "Content-Type": "application/json" } },
            );
        });

        initApiClient({ fetch: fetchStub as unknown as typeof fetch });

        await listGreetings({ query: { page: 0, size: 5 } });

        expect(lastRequest?.method).toBe("GET");
        expect(lastRequest?.headers.get("X-XSRF-TOKEN")).toBeNull();
    });

    it("handles URL-encoded CSRF cookie values", async () => {
        // Cookie values with special characters are URL-encoded
        document.cookie = "XSRF-TOKEN=abc%3D123%26test; path=/";

        let lastRequest: Request | undefined;

        const fetchStub = vi.fn(async (request: Request) => {
            lastRequest = request;
            return new Response(
                JSON.stringify({
                    id: "1004",
                    reference: "GRE-2025-000004",
                    message: "Hello",
                    recipient: "World",
                    createdAt: new Date().toISOString(),
                }),
                { status: 201, headers: { "Content-Type": "application/json" } },
            );
        });

        initApiClient({ fetch: fetchStub as unknown as typeof fetch });

        await createGreeting({
            body: {
                message: "Hello",
                recipient: "World",
            },
        });

        // Should decode the URL-encoded value
        expect(lastRequest?.headers.get("X-XSRF-TOKEN")).toBe("abc=123&test");
    });

    it("does not add CSRF header when cookie is missing", async () => {
        // Ensure no XSRF-TOKEN cookie exists
        clearCookie("XSRF-TOKEN");

        let lastRequest: Request | undefined;

        const fetchStub = vi.fn(async (request: Request) => {
            lastRequest = request;
            return new Response(
                JSON.stringify({
                    id: "1004",
                    reference: "GRE-2025-000004",
                    message: "Hello",
                    recipient: "World",
                    createdAt: new Date().toISOString(),
                }),
                { status: 201, headers: { "Content-Type": "application/json" } },
            );
        });

        initApiClient({ fetch: fetchStub as unknown as typeof fetch });

        await createGreeting({
            body: {
                message: "Hello",
                recipient: "World",
            },
        });

        // No CSRF token should be added when cookie doesn't exist
        expect(lastRequest?.headers.get("X-XSRF-TOKEN")).toBeNull();
    });

    it("dispatches session-expired event on 401 response", async () => {
        const eventListener = vi.fn();
        window.addEventListener("auth:session-expired", eventListener);

        const fetchStub = vi.fn(async () =>
            new Response(
                JSON.stringify({ error: "Unauthorized" }),
                { status: 401, headers: { "Content-Type": "application/json" } },
            ),
        );

        initApiClient({ fetch: fetchStub as unknown as typeof fetch });

        try {
            await listGreetings({ query: { page: 0, size: 5 } });
        } catch {
            // Expected to throw due to 401
        }

        expect(eventListener).toHaveBeenCalled();

        window.removeEventListener("auth:session-expired", eventListener);
    });
});
