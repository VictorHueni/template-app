import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { AuthProvider } from "./AuthProvider";
import { useAuth } from "./hooks";

vi.mock("../../api/config", () => ({
    getCurrentUser: vi.fn(),
}));

import { getCurrentUser, type UserInfoResponse, type ProblemDetail } from "../../api/config";

const TEST_USER: UserInfoResponse = {
    id: "u-1",
    username: "johndoe",
    roles: ["USER"],
};

const TEST_ERROR: ProblemDetail = {
    type: "https://api.example.com/problems/unauthorized",
    title: "Unauthorized",
    status: 401,
    timestamp: "2025-01-15T10:30:00Z",
    traceId: "550e8400-e29b-41d4-a716-446655440000",
    detail: "Authentication is required",
};

/**
 * Helper to mock the getCurrentUser API response.
 */
function mockUserResponse(user: UserInfoResponse | null, error?: ProblemDetail) {
    vi.mocked(getCurrentUser).mockResolvedValue({
        data: user ?? undefined,
        error: error,
        response: new Response(null, { status: error?.status ?? 200 }),
        request: new Request("http://localhost"),
    });
}

/**
 * Helper to wait for the AuthProvider to reach a specific status.
 */
async function waitForStatus(status: "authenticated" | "anonymous" | "loading") {
    await waitFor(() => expect(screen.getByTestId("status").textContent).toBe(status));
}

function Viewer() {
    const { status, user } = useAuth();
    return (
        <div>
            <span data-testid="status">{status}</span>
            <span data-testid="username">{user?.username ?? ""}</span>
        </div>
    );
}

function ViewerWithLogout() {
    const { status, user, logout } = useAuth();
    return (
        <div>
            <span data-testid="status">{status}</span>
            <span data-testid="username">{user?.username ?? ""}</span>
            <button data-testid="logout-btn" onClick={() => { logout(); }}>
                Logout
            </button>
        </div>
    );
}

describe("AuthProvider", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("authenticates when /v1/me returns user", async () => {
        mockUserResponse(TEST_USER);

        render(
            <AuthProvider mode="real">
                <Viewer />
            </AuthProvider>,
        );

        await waitForStatus("authenticated");
        expect(screen.getByTestId("username").textContent).toBe(TEST_USER.username);
    });

    it("becomes anonymous when /v1/me returns 401", async () => {
        mockUserResponse(null, TEST_ERROR);

        render(
            <AuthProvider mode="real">
                <Viewer />
            </AuthProvider>,
        );

        await waitForStatus("anonymous");
        expect(screen.getByTestId("username").textContent).toBe("");
    });

    it("uses mock user in mock mode without calling API", async () => {
        mockUserResponse({ ...TEST_USER, username: "should-not-be-used" });

        render(
            <AuthProvider mode="mock" mockUser={{ ...TEST_USER, username: "mocky" }}>
                <Viewer />
            </AuthProvider>,
        );

        await waitForStatus("authenticated");
        expect(screen.getByTestId("username").textContent).toBe("mocky");
        expect(vi.mocked(getCurrentUser)).not.toHaveBeenCalled();
    });

    it("clears state when auth:session-expired is dispatched", async () => {
        mockUserResponse(TEST_USER);

        render(
            <AuthProvider mode="real">
                <Viewer />
            </AuthProvider>,
        );

        await waitForStatus("authenticated");

        globalThis.dispatchEvent(new CustomEvent("auth:session-expired"));

        await waitForStatus("anonymous");
        expect(screen.getByTestId("username").textContent).toBe("");
    });
});

describe("AuthProvider logout", () => {
    const originalFetch = global.fetch;
    const originalLocation = globalThis.location;
    let cookieValue = "";
    let fetchSpy = vi.fn();

    beforeEach(() => {
        vi.clearAllMocks();
        cookieValue = "";
        fetchSpy = vi.fn().mockResolvedValue({
            headers: new Headers({ Location: "http://keycloak/logout" }),
        });
        global.fetch = fetchSpy;

        // Mock document.cookie
        Object.defineProperty(document, "cookie", {
            get: () => cookieValue,
            set: (v: string) => {
                cookieValue = v;
            },
            configurable: true,
        });
        // Mock window.location for redirect testing
        Object.defineProperty(globalThis, "location", {
            value: {
                href: "http://localhost:3000",
                origin: "http://localhost:3000",
                reload: vi.fn(),
                assign: vi.fn(),
            },
            writable: true,
        });

        // Most logout tests start with an authenticated user
        mockUserResponse(TEST_USER);
    });

    afterEach(() => {
        global.fetch = originalFetch;
        Object.defineProperty(globalThis, "location", {
            value: originalLocation,
            writable: true,
        });
    });

    it("clears state in mock mode without network call", async () => {
        const user = userEvent.setup();
        fetchSpy.mockReset().mockResolvedValue({}); // Reset default fetch mock

        render(
            <AuthProvider mode="mock" mockUser={{ ...TEST_USER, username: "mocky" }}>
                <ViewerWithLogout />
            </AuthProvider>,
        );

        await waitForStatus("authenticated");
        expect(screen.getByTestId("username").textContent).toBe("mocky");

        await user.click(screen.getByTestId("logout-btn"));

        await waitForStatus("anonymous");
        expect(screen.getByTestId("username").textContent).toBe("");
        expect(fetchSpy).not.toHaveBeenCalled();
    });

    it("POSTs to /logout with X-POST-LOGOUT-SUCCESS-URI header in real mode", async () => {
        const user = userEvent.setup();

        render(
            <AuthProvider mode="real">
                <ViewerWithLogout />
            </AuthProvider>,
        );

        await waitForStatus("authenticated");
        await user.click(screen.getByTestId("logout-btn"));

        await waitFor(() => {
            expect(fetchSpy).toHaveBeenCalledWith("/logout", {
                method: "POST",
                credentials: "include",
                headers: {
                    "X-POST-LOGOUT-SUCCESS-URI": "http://localhost:3000",
                },
            });
        });
    });

    it("includes X-XSRF-TOKEN header when CSRF cookie exists", async () => {
        const user = userEvent.setup();
        cookieValue = "XSRF-TOKEN=test-csrf-token-123; other-cookie=value";

        render(
            <AuthProvider mode="real">
                <ViewerWithLogout />
            </AuthProvider>,
        );

        await waitForStatus("authenticated");
        await user.click(screen.getByTestId("logout-btn"));

        await waitFor(() => {
            expect(fetchSpy).toHaveBeenCalledWith("/logout", {
                method: "POST",
                credentials: "include",
                headers: {
                    "X-POST-LOGOUT-SUCCESS-URI": "http://localhost:3000",
                    "X-XSRF-TOKEN": "test-csrf-token-123",
                },
            });
        });
    });

    it("redirects to Location header URL on successful logout", async () => {
        const user = userEvent.setup();
        const keycloakLogoutUrl = "http://keycloak/logout-redirect";
        fetchSpy.mockResolvedValue({
            headers: new Headers({ Location: keycloakLogoutUrl }),
        });

        render(
            <AuthProvider mode="real">
                <ViewerWithLogout />
            </AuthProvider>,
        );

        await waitForStatus("authenticated");
        await user.click(screen.getByTestId("logout-btn"));

        await waitFor(() => {
            expect(globalThis.location.href).toBe(keycloakLogoutUrl);
        });
    });

    it("falls back to reload when no Location header", async () => {
        const user = userEvent.setup();
        fetchSpy.mockResolvedValue({
            headers: new Headers({}), // No Location header
        });

        render(
            <AuthProvider mode="real">
                <ViewerWithLogout />
            </AuthProvider>,
        );

        await waitForStatus("authenticated");
        await user.click(screen.getByTestId("logout-btn"));

        await waitFor(() => {
            expect(globalThis.location.reload).toHaveBeenCalled();
        });
    });

    it("handles logout errors gracefully by redirecting to home", async () => {
        const user = userEvent.setup();
        const consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
        fetchSpy.mockRejectedValue(new Error("Network error"));

        render(
            <AuthProvider mode="real">
                <ViewerWithLogout />
            </AuthProvider>,
        );

        await waitForStatus("authenticated");
        await user.click(screen.getByTestId("logout-btn"));

        await waitFor(() => {
            expect(consoleErrorSpy).toHaveBeenCalledWith("Logout failed:", expect.any(Error));
            expect(globalThis.location.href).toBe("/");
        });

        consoleErrorSpy.mockRestore();
    });
});