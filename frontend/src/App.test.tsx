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
import { createMockGreetingPage, mockGreetings } from "./test/mocks/data";
import type { GreetingResponse } from "./api/generated";

// Mock the API config module
vi.mock("./api/config", async () => {
    const actual = await vi.importActual<typeof import("./api/config")>("./api/config");
    return {
        ...actual,
        API_BASE_PATH: "/api/v1",
        greetingsApiPublic: {
            listGreetings: vi.fn(),
        },
        greetingsApiAuth: {
            createGreeting: vi.fn(),
            updateGreeting: vi.fn(),
            deleteGreeting: vi.fn(),
        },
        // greetingsApi is what the mutation hooks actually use
        greetingsApi: {
            createGreeting: vi.fn(),
            updateGreeting: vi.fn(),
            deleteGreeting: vi.fn(),
        },
    };
});

// Get the mocked modules
import { greetingsApiPublic, greetingsApi } from "./api/config";

const mockListGreetings = vi.mocked(greetingsApiPublic.listGreetings);
const mockCreateGreeting = vi.mocked(greetingsApi.createGreeting);
// These are available for future tests but not currently used
// const mockUpdateGreeting = vi.mocked(greetingsApi.updateGreeting);
// const mockDeleteGreeting = vi.mocked(greetingsApi.deleteGreeting);

describe("App", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        // Default: return empty page
        mockListGreetings.mockResolvedValue(createMockGreetingPage([]));
    });

    describe("loading state", () => {
        it("shows loading indicator initially", () => {
            // Make the API hang
            mockListGreetings.mockImplementation(() => new Promise(() => {}));

            render(<App />);

            expect(screen.getByRole("status", { name: /loading/i })).toBeInTheDocument();
        });

        it("hides loading after data loads", async () => {
            mockListGreetings.mockResolvedValue(createMockGreetingPage(mockGreetings));

            render(<App />);

            await waitFor(() => {
                expect(screen.queryByRole("status", { name: /loading/i })).not.toBeInTheDocument();
            });
        });
    });

    describe("displaying greetings", () => {
        it("shows list of greetings", async () => {
            mockListGreetings.mockResolvedValue(createMockGreetingPage(mockGreetings));

            render(<App />);

            await waitFor(() => {
                expect(screen.getByText("Hello, World!")).toBeInTheDocument();
                expect(screen.getByText("Welcome to the app!")).toBeInTheDocument();
            });
        });

        it("shows empty state when no greetings", async () => {
            mockListGreetings.mockResolvedValue(createMockGreetingPage([]));

            render(<App />);

            await waitFor(() => {
                expect(screen.getByText(/no greetings found/i)).toBeInTheDocument();
            });
        });

        it("shows error state on API failure", async () => {
            mockListGreetings.mockRejectedValue(new Error("Network error"));

            render(<App />);

            await waitFor(() => {
                expect(screen.getByRole("alert")).toBeInTheDocument();
            });
        });
    });

    describe("theme toggle", () => {
        it("toggles between light and dark theme", async () => {
            mockListGreetings.mockResolvedValue(createMockGreetingPage([]));
            const { container } = render(<App />);

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
    });

    describe("creating greetings", () => {
        it("shows create form when clicking New Greeting button", async () => {
            mockListGreetings.mockResolvedValue(createMockGreetingPage([]));

            render(<App />);
            const user = userEvent.setup();

            await waitFor(() => {
                expect(screen.getByRole("button", { name: /new greeting/i })).toBeInTheDocument();
            });

            await user.click(screen.getByRole("button", { name: /new greeting/i }));

            expect(
                screen.getByRole("heading", { name: /create new greeting/i }),
            ).toBeInTheDocument();
        });

        it("creates a new greeting and refreshes list", async () => {
            const newGreeting: GreetingResponse = {
                id: 1004,
                reference: "REF-1004",
                message: "New greeting!",
                recipient: "Test",
            };

            mockListGreetings.mockResolvedValue(createMockGreetingPage([]));
            mockCreateGreeting.mockResolvedValue(newGreeting);

            render(<App />);
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
            mockListGreetings.mockResolvedValue(createMockGreetingPage([newGreeting]));

            // Submit
            await user.click(screen.getByRole("button", { name: /create greeting/i }));

            // Verify API was called
            await waitFor(() => {
                expect(mockCreateGreeting).toHaveBeenCalledWith({
                    createGreetingRequest: {
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
    });

    describe("refresh functionality", () => {
        it("refreshes list when clicking refresh button", async () => {
            mockListGreetings.mockResolvedValue(createMockGreetingPage(mockGreetings));

            render(<App />);
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
});
