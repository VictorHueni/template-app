import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";

import { AuthProvider, useAuth } from "./AuthProvider";

vi.mock("../../api/config", () => ({
    getCurrentUser: vi.fn(),
}));

import { getCurrentUser } from "../../api/config";

function Viewer() {
    const { status, user } = useAuth();
    return (
        <div>
            <span data-testid="status">{status}</span>
            <span data-testid="username">{user?.username ?? ""}</span>
        </div>
    );
}

describe("AuthProvider", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("authenticates when /v1/me returns user", async () => {
        vi.mocked(getCurrentUser).mockResolvedValue({
            data: {
                id: "u-1",
                username: "johndoe",
                roles: ["USER"],
            },
        } as any);

        render(
            <AuthProvider mode="real">
                <Viewer />
            </AuthProvider>,
        );

        await waitFor(() => expect(screen.getByTestId("status").textContent).toBe("authenticated"));
        expect(screen.getByTestId("username").textContent).toBe("johndoe");
    });

    it("becomes anonymous when /v1/me returns 401", async () => {
        vi.mocked(getCurrentUser).mockResolvedValue({
            error: {
                type: "https://api.example.com/problems/unauthorized",
                title: "Unauthorized",
                status: 401,
                timestamp: "2025-01-15T10:30:00Z",
                traceId: "550e8400-e29b-41d4-a716-446655440000",
                detail: "Authentication is required",
            },
        } as any);

        render(
            <AuthProvider mode="real">
                <Viewer />
            </AuthProvider>,
        );

        await waitFor(() => expect(screen.getByTestId("status").textContent).toBe("anonymous"));
        expect(screen.getByTestId("username").textContent).toBe("");
    });

    it("uses mock user in mock mode without calling API", async () => {
        vi.mocked(getCurrentUser).mockResolvedValue({
            data: {
                id: "u-should-not-be-used",
                username: "should-not-be-used",
                roles: ["USER"],
            },
        } as any);

        render(
            <AuthProvider mode="mock" mockUser={{ id: "m-1", username: "mocky", roles: ["ADMIN"] }}>
                <Viewer />
            </AuthProvider>,
        );

        await waitFor(() => expect(screen.getByTestId("status").textContent).toBe("authenticated"));
        expect(screen.getByTestId("username").textContent).toBe("mocky");
        expect(vi.mocked(getCurrentUser)).not.toHaveBeenCalled();
    });

    it("clears state when auth:session-expired is dispatched", async () => {
        vi.mocked(getCurrentUser).mockResolvedValue({
            data: {
                id: "u-1",
                username: "johndoe",
                roles: ["USER"],
            },
        } as any);

        render(
            <AuthProvider mode="real">
                <Viewer />
            </AuthProvider>,
        );

        await waitFor(() => expect(screen.getByTestId("status").textContent).toBe("authenticated"));

        window.dispatchEvent(new CustomEvent("auth:session-expired"));

        await waitFor(() => expect(screen.getByTestId("status").textContent).toBe("anonymous"));
        expect(screen.getByTestId("username").textContent).toBe("");
    });
});
