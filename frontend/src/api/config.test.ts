import { describe, it, expect, beforeEach, vi, afterEach, type Mock } from "vitest";

import { initApiClient, client, createGreeting, listGreetings, resetInterceptors } from "./config";

const MOCK_GREETING = {
    id: "1004",
    reference: "GRE-2025-000004",
    message: "Hello",
    recipient: "World",
    createdAt: new Date().toISOString(),
};

function clearCookie(name: string) {
    document.cookie = `${name}=; Max-Age=0; path=/`;
}

/**
 * Helper to setup API client with a mocked fetch.
 */
async function setupApiTest(body: unknown = {}, status = 200) {
    const fetchStub = vi.fn(async () => {
        return new Response(JSON.stringify(body), {
            status,
            headers: { "Content-Type": "application/json" },
        });
    });

    // We use globalThis.fetch because initApiClient defaults to it
    globalThis.fetch = fetchStub as unknown as typeof fetch;
    initApiClient({ fetch: globalThis.fetch });

    return { fetchStub };
}

/**
 * Helper to get the last request sent through the client.
 */
function getLastRequest(fetchStub: Mock): Request {
    const lastCall = fetchStub.mock.lastCall;
    if (!lastCall) {
        throw new Error("No fetch calls recorded");
    }
    const [urlOrRequest, init] = lastCall;
    return urlOrRequest instanceof Request ? urlOrRequest : new Request(urlOrRequest, init);
}

describe("api/config initApiClient", () => {
    const originalFetch = globalThis.fetch;

    beforeEach(() => {
        vi.restoreAllMocks();
        clearCookie("XSRF-TOKEN");

        // Reset the singleton client state for each test
        resetInterceptors();
        client.interceptors.request.clear();
        client.interceptors.response.clear();
        client.interceptors.error.clear();
    });

    afterEach(() => {
        globalThis.fetch = originalFetch;
    });

    it("sets baseUrl to /api and includes credentials", async () => {
        const { fetchStub } = await setupApiTest();

        await listGreetings({ query: { page: 0, size: 5 } });

        const config = client.getConfig();
        expect(config.baseUrl).toBe("http://localhost:3000/api");
        expect(config.credentials).toBe("include");

        const lastRequest = getLastRequest(fetchStub);
        expect(lastRequest.credentials).toBe("include");
    });

    it("does not add Authorization header (BFF cookies)", async () => {
        const { fetchStub } = await setupApiTest(MOCK_GREETING, 201);

        await createGreeting({
            body: {
                message: "Hello",
                recipient: "World",
            },
        });

        const lastRequest = getLastRequest(fetchStub);
        expect(lastRequest.headers.get("Authorization")).toBeNull();
    });

    it.each([
        {
            name: "adds CSRF header for state-changing requests when cookie exists",
            method: "POST",
            cookieValue: "test-csrf-token",
            expectedHeader: "test-csrf-token",
        },
        {
            name: "handles URL-encoded CSRF cookie values",
            method: "POST",
            cookieValue: "abc%3D123%26test",
            expectedHeader: "abc=123&test",
        },
        {
            name: "does not add CSRF header for GET requests",
            method: "GET",
            cookieValue: "test-csrf-token",
            expectedHeader: null,
        },
        {
            name: "does not add CSRF header when cookie is missing",
            method: "POST",
            cookieValue: null,
            expectedHeader: null,
        },
    ])("$name", async ({ method, cookieValue, expectedHeader }) => {
        if (cookieValue) {
            document.cookie = `XSRF-TOKEN=${cookieValue}; path=/`;
        } else {
            clearCookie("XSRF-TOKEN");
        }

        const { fetchStub } = await setupApiTest(
            method === "POST" ? MOCK_GREETING : { data: [] },
            method === "POST" ? 201 : 200,
        );

        if (method === "POST") {
            await createGreeting({
                body: {
                    message: "Hello",
                    recipient: "World",
                },
            });
        } else {
            await listGreetings({ query: { page: 0, size: 5 } });
        }

        const lastRequest = getLastRequest(fetchStub);
        expect(lastRequest.method).toBe(method);
        expect(lastRequest.headers.get("X-XSRF-TOKEN")).toBe(expectedHeader);
    });

    it("dispatches session-expired event on 401 response", async () => {
        const eventListener = vi.fn();
        globalThis.addEventListener("auth:session-expired", eventListener);

        await setupApiTest({ error: "Unauthorized" }, 401);

        try {
            await listGreetings({ query: { page: 0, size: 5 } });
        } catch {
            // Expected to throw
        }

        expect(eventListener).toHaveBeenCalled();
        globalThis.removeEventListener("auth:session-expired", eventListener);
    });
});
