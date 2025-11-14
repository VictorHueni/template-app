import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import App from "./App";

const API_BASE = import.meta.env.VITE_API_URL ?? "";

type FetchMock = ReturnType<typeof vi.fn>;

// Helper to mock fetch per test
function mockFetchOnce(response: unknown, ok = true, status = 200) {
    const json = async () => response;
    const res = { ok, status, json } as Response;
    (globalThis.fetch as FetchMock).mockResolvedValueOnce(res);
}

describe("App", () => {
    beforeEach(() => {
        vi.resetAllMocks();
        globalThis.fetch = vi.fn() as unknown as typeof fetch;
    });

    it("shows loading and then the greeting message on success", async () => {
        mockFetchOnce({ message: "Hello world" });

        render(<App />);

        // Initial loading state
        expect(screen.getByLabelText(/loading/i)).toBeInTheDocument();

        // Wait for the message to appear
        await waitFor(() =>
            expect(screen.getByLabelText("greeting-message")).toHaveTextContent("Hello world"),
        );

        // Loading indicator should be gone
        expect(screen.queryByLabelText(/loading/i)).not.toBeInTheDocument();

        // Fetch called once, with default URL (default param is world)
        expect(globalThis.fetch).toHaveBeenCalledWith(`${API_BASE}/api/hello?name=world`);
    });

    it("shows an error when the fetch fails", async () => {
        (globalThis.fetch as FetchMock).mockRejectedValueOnce(new Error("Network down"));

        render(<App />);

        await waitFor(() =>
            expect(screen.getByLabelText("error-message")).toHaveTextContent("Network down"),
        );

        expect(screen.queryByLabelText("greeting-message")).not.toBeInTheDocument();
    });

    it("submits a custom name and updates the greeting", async () => {
        // initial load
        mockFetchOnce({ message: "Hello world" });
        render(<App />);
        await waitFor(() =>
            expect(screen.getByLabelText("greeting-message")).toHaveTextContent("Hello world"),
        );

        // Next call for custom name
        mockFetchOnce({ message: "Hello Alice" });

        const user = userEvent.setup();
        const input = screen.getByLabelText(/name/i);
        const button = screen.getByRole("button", { name: /update greeting/i });

        await user.clear(input);
        await user.type(input, "Alice");
        await user.click(button);

        await waitFor(() =>
            expect(screen.getByLabelText("greeting-message")).toHaveTextContent("Hello Alice"),
        );

        expect(globalThis.fetch).toHaveBeenLastCalledWith(`${API_BASE}/api/hello?name=Alice`);
    });

    it("clears the message when clicking Clear", async () => {
        mockFetchOnce({ message: "Hello world" });
        render(<App />);
        await waitFor(() =>
            expect(screen.getByLabelText("greeting-message")).toHaveTextContent("Hello world"),
        );

        const clearButton = screen.getByRole("button", { name: /clear/i });
        const user = userEvent.setup();
        await user.click(clearButton);

        expect(screen.queryByLabelText("greeting-message")).not.toBeInTheDocument();
        expect(screen.queryByLabelText("error-message")).not.toBeInTheDocument();
    });

    it("toggles theme between light and dark", async () => {
        mockFetchOnce({ message: "Hello world" });
        const { container } = render(<App />);

        const main = container.querySelector("main");
        expect(main).toHaveAttribute("data-theme", "light");

        const toggleButton = screen.getByRole("button", { name: /switch to dark theme/i });
        const user = userEvent.setup();

        await user.click(toggleButton);

        expect(main).toHaveAttribute("data-theme", "dark");
    });
});
