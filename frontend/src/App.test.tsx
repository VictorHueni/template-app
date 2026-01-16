/**
 * App Component Tests
 *
 * Tests for the main App component which displays a list of greetings
 * with CRUD operations (create, read, update, delete).
 */

import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import App from "./App";
import { AuthProvider } from "./features/auth";
import {
    createMockGreetingPage,
    mockGreetings,
    mockApiSuccess,
    mockApiError,
    mockErrors,
} from "./test/mocks/data";
import type { GreetingResponse } from "./api/generated";

// Mock the API config module
vi.mock("./api/config", async () => {
    const actual = await vi.importActual<typeof import("./api/config")>("./api/config");
    return {
        ...actual,
        API_BASE_PATH: "/api/v1",
        listGreetings: vi.fn(),
        getGreeting: vi.fn(),
        createGreeting: vi.fn(),
        updateGreeting: vi.fn(),
        patchGreeting: vi.fn(),
        deleteGreeting: vi.fn(),
    };
});

// Get the mocked modules
import { listGreetings, createGreeting } from "./api/config";

const mockListGreetings = vi.mocked(listGreetings);
const mockCreateGreeting = vi.mocked(createGreeting);

describe("App", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        // Default: return empty page (hey-api returns { data, error, request, response })
        mockListGreetings.mockResolvedValue(
            mockApiSuccess(createMockGreetingPage([])) as Awaited<ReturnType<typeof listGreetings>>,
        );
    });

    it("loading state: shows loading indicator initially", () => {
        // Make the API hang - use type assertion for never-resolving promise
        mockListGreetings.mockImplementation(
            () => new Promise(() => {}) as Promise<Awaited<ReturnType<typeof listGreetings>>>,
        );

        render(
            <AuthProvider mode="mock">
                <App />
            </AuthProvider>,
        );

        expect(screen.getByRole("status", { name: /loading/i })).toBeInTheDocument();
    });

    it("loading state: hides loading after data loads", async () => {
        mockListGreetings.mockResolvedValue(
            mockApiSuccess(createMockGreetingPage(mockGreetings)) as Awaited<
                ReturnType<typeof listGreetings>
            >,
        );

        render(
            <AuthProvider mode="mock">
                <App />
            </AuthProvider>,
        );

        await waitFor(() => {
            expect(screen.queryByRole("status", { name: /loading/i })).not.toBeInTheDocument();
        });
    });

    it("displaying greetings: shows list of greetings", async () => {
        mockListGreetings.mockResolvedValue(
            mockApiSuccess(createMockGreetingPage(mockGreetings)) as Awaited<
                ReturnType<typeof listGreetings>
            >,
        );

        render(
            <AuthProvider mode="mock">
                <App />
            </AuthProvider>,
        );

        await waitFor(() => {
            expect(screen.getByText("Hello, World!")).toBeInTheDocument();
            expect(screen.getByText("Welcome to the app!")).toBeInTheDocument();
        });
    });

    it("displaying greetings: shows empty state when no greetings", async () => {
        mockListGreetings.mockResolvedValue(
            mockApiSuccess(createMockGreetingPage([])) as Awaited<ReturnType<typeof listGreetings>>,
        );

        render(
            <AuthProvider mode="mock">
                <App />
            </AuthProvider>,
        );

        await waitFor(() => {
            expect(screen.getByText(/no greetings found/i)).toBeInTheDocument();
        });
    });

    it("displaying greetings: shows error state on API failure", async () => {
        mockListGreetings.mockResolvedValue(
            mockApiError(mockErrors.serverError) as Awaited<ReturnType<typeof listGreetings>>,
        );

        render(
            <AuthProvider mode="mock">
                <App />
            </AuthProvider>,
        );

        await waitFor(() => {
            expect(screen.getByRole("alert")).toBeInTheDocument();
        });
    });

    it("theme toggle: toggles between light and dark theme", async () => {
        mockListGreetings.mockResolvedValue(
            mockApiSuccess(createMockGreetingPage([])) as Awaited<ReturnType<typeof listGreetings>>,
        );
        const { container } = render(
            <AuthProvider mode="mock">
                <App />
            </AuthProvider>,
        );

        const main = container.querySelector("main");
        expect(main).toHaveAttribute("data-theme", "light");

        // Button should show dark option when in light mode
        const toggleButton = screen.getByRole("button", { name: /dark/i });
        const user = userEvent.setup();

        await user.click(toggleButton);

        expect(main).toHaveAttribute("data-theme", "dark");

        // Now should show light option
        expect(screen.getByRole("button", { name: /light/i })).toBeInTheDocument();
    });

    it("creating greetings: shows create form when clicking New Greeting button", async () => {
        mockListGreetings.mockResolvedValue(
            mockApiSuccess(createMockGreetingPage([])) as Awaited<ReturnType<typeof listGreetings>>,
        );

        render(
            <AuthProvider mode="mock">
                <App />
            </AuthProvider>,
        );
        const user = userEvent.setup();

        await waitFor(() => {
            expect(screen.getByRole("button", { name: /new greeting/i })).toBeInTheDocument();
        });

        await user.click(screen.getByRole("button", { name: /new greeting/i }));

        expect(screen.getByRole("heading", { name: /create new greeting/i })).toBeInTheDocument();
    });

    it("creating greetings: creates a new greeting and refreshes list", async () => {
        const newGreeting: GreetingResponse = {
            id: "1004",
            reference: "REF-1004",
            message: "New greeting!",
            recipient: "Test",
        };

        mockListGreetings.mockResolvedValue(
            mockApiSuccess(createMockGreetingPage([])) as Awaited<ReturnType<typeof listGreetings>>,
        );
        mockCreateGreeting.mockResolvedValue(
            mockApiSuccess(newGreeting) as Awaited<ReturnType<typeof createGreeting>>,
        );

        render(
            <AuthProvider mode="mock">
                <App />
            </AuthProvider>,
        );
        const user = userEvent.setup();

        // Wait for initial load
        await waitFor(() => {
            expect(screen.getByRole("button", { name: /new greeting/i })).toBeInTheDocument();
        });

        // Open form
        await user.click(screen.getByRole("button", { name: /new greeting/i }));

        // Fill form
        await user.type(screen.getByLabelText(/message/i), "New greeting!");
        await user.type(screen.getByLabelText(/recipient/i), "Test");

        // Update mock for refresh
        mockListGreetings.mockResolvedValue(
            mockApiSuccess(createMockGreetingPage([newGreeting])) as Awaited<
                ReturnType<typeof listGreetings>
            >,
        );

        // Submit
        await user.click(screen.getByRole("button", { name: /create greeting/i }));

        // Verify API was called with hey-api format
        await waitFor(() => {
            expect(mockCreateGreeting).toHaveBeenCalledWith({
                body: {
                    message: "New greeting!",
                    recipient: "Test",
                },
            });
        });

        // Verify list was refreshed
        await waitFor(() => {
            expect(mockListGreetings).toHaveBeenCalledTimes(2);
        });
    });

    it("refresh functionality: refreshes list when clicking refresh button", async () => {
        mockListGreetings.mockResolvedValue(
            mockApiSuccess(createMockGreetingPage(mockGreetings)) as Awaited<
                ReturnType<typeof listGreetings>
            >,
        );

        render(
            <AuthProvider mode="mock">
                <App />
            </AuthProvider>,
        );
        const user = userEvent.setup();

        // Wait for initial load
        await waitFor(() => {
            expect(screen.getByText("Hello, World!")).toBeInTheDocument();
        });

        // Click refresh
        await user.click(screen.getByRole("button", { name: /refresh/i }));

        // Verify API called again
        await waitFor(() => {
            expect(mockListGreetings).toHaveBeenCalledTimes(2);
        });
    });
});
