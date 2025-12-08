/**
 * Tests for Greeting Components
 *
 * Tests for the presentational components in the greetings feature.
 */

import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { GreetingCard } from "./GreetingCard";
import { GreetingList } from "./GreetingList";
import { GreetingForm } from "./GreetingForm";
import { ErrorMessage } from "./ErrorMessage";
import { LoadingSpinner } from "./LoadingSpinner";
import { mockGreetings, createMockGreeting } from "../../../test/mocks/data";
import type { ApiError } from "../../../api/errors";

describe("GreetingCard", () => {
    const greeting = mockGreetings[0];

    it("renders greeting message", () => {
        render(<GreetingCard greeting={greeting} />);

        expect(screen.getByText(greeting.message)).toBeInTheDocument();
    });

    it("renders recipient when provided", () => {
        render(<GreetingCard greeting={greeting} />);

        expect(screen.getByText(/To:/)).toBeInTheDocument();
        expect(screen.getByText(greeting.recipient!)).toBeInTheDocument();
    });

    it("renders reference number", () => {
        render(<GreetingCard greeting={greeting} />);

        expect(screen.getByText(greeting.reference)).toBeInTheDocument();
    });

    it("does not render recipient when not provided", () => {
        const greetingWithoutRecipient = createMockGreeting({ recipient: undefined });
        render(<GreetingCard greeting={greetingWithoutRecipient} />);

        expect(screen.queryByText(/To:/)).not.toBeInTheDocument();
    });

    it("calls onEdit when edit button is clicked", async () => {
        const onEdit = vi.fn();
        const user = userEvent.setup();

        render(<GreetingCard greeting={greeting} onEdit={onEdit} />);

        await user.click(screen.getByRole("button", { name: /edit/i }));

        expect(onEdit).toHaveBeenCalledWith(greeting);
    });

    it("calls onDelete when delete button is clicked", async () => {
        const onDelete = vi.fn();
        const user = userEvent.setup();

        render(<GreetingCard greeting={greeting} onDelete={onDelete} />);

        await user.click(screen.getByRole("button", { name: /delete/i }));

        expect(onDelete).toHaveBeenCalledWith(greeting.id);
    });

    it("does not render edit button when onEdit is not provided", () => {
        render(<GreetingCard greeting={greeting} />);

        expect(screen.queryByRole("button", { name: /edit/i })).not.toBeInTheDocument();
    });

    it("does not render delete button when onDelete is not provided", () => {
        render(<GreetingCard greeting={greeting} />);

        expect(screen.queryByRole("button", { name: /delete/i })).not.toBeInTheDocument();
    });
});

describe("GreetingList", () => {
    it("renders list of greetings", () => {
        render(<GreetingList greetings={mockGreetings} />);

        for (const greeting of mockGreetings) {
            expect(screen.getByText(greeting.message)).toBeInTheDocument();
        }
    });

    it("renders empty state when no greetings", () => {
        render(<GreetingList greetings={[]} />);

        expect(screen.getByText(/no greetings/i)).toBeInTheDocument();
    });

    it("renders pagination info when meta is provided", () => {
        const meta = {
            pageNumber: 0,
            pageSize: 10,
            totalElements: 25,
            totalPages: 3,
        };

        render(<GreetingList greetings={mockGreetings} meta={meta} />);

        expect(screen.getByText(/page 1 of 3/i)).toBeInTheDocument();
        expect(screen.getByText(/25 total/i)).toBeInTheDocument();
    });

    it("calls onPageChange when pagination buttons are clicked", async () => {
        const onPageChange = vi.fn();
        const user = userEvent.setup();
        const meta = {
            pageNumber: 1,
            pageSize: 10,
            totalElements: 25,
            totalPages: 3,
        };

        render(<GreetingList greetings={mockGreetings} meta={meta} onPageChange={onPageChange} />);

        // Click next
        await user.click(screen.getByRole("button", { name: /next/i }));
        expect(onPageChange).toHaveBeenCalledWith(2);

        // Click previous
        await user.click(screen.getByRole("button", { name: /previous/i }));
        expect(onPageChange).toHaveBeenCalledWith(0);
    });

    it("disables previous button on first page", () => {
        const meta = {
            pageNumber: 0,
            pageSize: 10,
            totalElements: 25,
            totalPages: 3,
        };

        render(<GreetingList greetings={mockGreetings} meta={meta} onPageChange={vi.fn()} />);

        expect(screen.getByRole("button", { name: /previous/i })).toBeDisabled();
    });

    it("disables next button on last page", () => {
        const meta = {
            pageNumber: 2,
            pageSize: 10,
            totalElements: 25,
            totalPages: 3,
        };

        render(<GreetingList greetings={mockGreetings} meta={meta} onPageChange={vi.fn()} />);

        expect(screen.getByRole("button", { name: /next/i })).toBeDisabled();
    });
});

describe("GreetingForm", () => {
    it("renders form with message input", () => {
        render(<GreetingForm onSubmit={vi.fn()} />);

        expect(screen.getByLabelText(/message/i)).toBeInTheDocument();
    });

    it("renders form with recipient input", () => {
        render(<GreetingForm onSubmit={vi.fn()} />);

        expect(screen.getByLabelText(/recipient/i)).toBeInTheDocument();
    });

    it("submits form with entered values", async () => {
        const onSubmit = vi.fn();
        const user = userEvent.setup();

        render(<GreetingForm onSubmit={onSubmit} />);

        await user.type(screen.getByLabelText(/message/i), "Hello, World!");
        await user.type(screen.getByLabelText(/recipient/i), "Everyone");
        await user.click(screen.getByRole("button", { name: /submit|create|save/i }));

        expect(onSubmit).toHaveBeenCalledWith({
            message: "Hello, World!",
            recipient: "Everyone",
        });
    });

    it("shows validation error when message is empty", async () => {
        const onSubmit = vi.fn();
        const user = userEvent.setup();

        render(<GreetingForm onSubmit={onSubmit} />);

        await user.click(screen.getByRole("button", { name: /submit|create|save/i }));

        expect(screen.getByText(/message is required/i)).toBeInTheDocument();
        expect(onSubmit).not.toHaveBeenCalled();
    });

    it("pre-fills form with initial values", () => {
        render(
            <GreetingForm
                onSubmit={vi.fn()}
                initialValues={{ message: "Hello", recipient: "World" }}
            />,
        );

        expect(screen.getByLabelText(/message/i)).toHaveValue("Hello");
        expect(screen.getByLabelText(/recipient/i)).toHaveValue("World");
    });

    it("displays field errors from API", () => {
        const fieldErrors = { message: "must not be blank" };

        render(<GreetingForm onSubmit={vi.fn()} fieldErrors={fieldErrors} />);

        expect(screen.getByText(/must not be blank/i)).toBeInTheDocument();
    });

    it("disables submit button when loading", () => {
        render(<GreetingForm onSubmit={vi.fn()} loading />);

        expect(screen.getByRole("button", { name: /submit|create|save|saving/i })).toBeDisabled();
    });
});

describe("ErrorMessage", () => {
    const mockApiError: ApiError = {
        status: 500,
        title: "Server Error",
        detail: "Something went wrong on the server",
        originalError: new Error("test"),
    };

    it("renders error message", () => {
        render(<ErrorMessage error={mockApiError} />);

        expect(screen.getByText(/something went wrong/i)).toBeInTheDocument();
    });

    it("renders error title", () => {
        render(<ErrorMessage error={mockApiError} />);

        expect(screen.getByText(/server error/i)).toBeInTheDocument();
    });

    it("calls onDismiss when dismiss button is clicked", async () => {
        const onDismiss = vi.fn();
        const user = userEvent.setup();

        render(<ErrorMessage error={mockApiError} onDismiss={onDismiss} />);

        await user.click(screen.getByRole("button", { name: /dismiss|close/i }));

        expect(onDismiss).toHaveBeenCalled();
    });

    it("calls onRetry when retry button is clicked", async () => {
        const onRetry = vi.fn();
        const user = userEvent.setup();

        render(<ErrorMessage error={mockApiError} onRetry={onRetry} />);

        await user.click(screen.getByRole("button", { name: /retry/i }));

        expect(onRetry).toHaveBeenCalled();
    });

    it("renders field errors when present", () => {
        const errorWithFields: ApiError = {
            ...mockApiError,
            status: 400,
            title: "Validation Error",
            fieldErrors: {
                message: "must not be blank",
                recipient: "invalid format",
            },
        };

        render(<ErrorMessage error={errorWithFields} />);

        expect(screen.getByText(/must not be blank/i)).toBeInTheDocument();
        expect(screen.getByText(/invalid format/i)).toBeInTheDocument();
    });
});

describe("LoadingSpinner", () => {
    it("renders loading indicator", () => {
        render(<LoadingSpinner />);

        expect(screen.getByRole("status")).toBeInTheDocument();
    });

    it("renders custom message", () => {
        render(<LoadingSpinner message="Loading greetings..." />);

        expect(screen.getByText(/loading greetings/i)).toBeInTheDocument();
    });

    it("has accessible label", () => {
        render(<LoadingSpinner />);

        expect(screen.getByLabelText(/loading/i)).toBeInTheDocument();
    });
});
