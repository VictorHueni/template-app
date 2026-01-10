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
});
