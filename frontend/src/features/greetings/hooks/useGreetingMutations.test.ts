/**
 * Tests for Greeting Mutation Hooks
 *
 * Tests for create, update, patch, and delete operations.
 * These hooks handle authenticated operations that modify data.
 */

import { describe, it, expect, vi, beforeEach, type Mock } from "vitest";
import { renderHook, act } from "@testing-library/react";
import {
    useCreateGreeting,
    useUpdateGreeting,
    usePatchGreeting,
    useDeleteGreeting,
} from "./useGreetingMutations";
import { mockGreetings, createMockGreeting } from "../../../test/mocks/data";
import { ResponseError } from "../../../api/generated";

// Mock the API config module
vi.mock("../../../api/config", () => ({
    greetingsApi: {
        createGreeting: vi.fn(),
        updateGreeting: vi.fn(),
        patchGreeting: vi.fn(),
        deleteGreeting: vi.fn(),
    },
}));

import { greetingsApi } from "../../../api/config";

const mockCreateGreeting = greetingsApi.createGreeting as Mock;
const mockUpdateGreeting = greetingsApi.updateGreeting as Mock;
const mockPatchGreeting = greetingsApi.patchGreeting as Mock;
const mockDeleteGreeting = greetingsApi.deleteGreeting as Mock;

describe("useCreateGreeting", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("should start in idle state", () => {
        const { result } = renderHook(() => useCreateGreeting());

        expect(result.current.loading).toBe(false);
        expect(result.current.error).toBeNull();
        expect(result.current.data).toBeNull();
    });

    it("should create a greeting successfully", async () => {
        const newGreeting = createMockGreeting({
            message: "New greeting",
            recipient: "Alice",
        });
        mockCreateGreeting.mockResolvedValue(newGreeting);

        const { result } = renderHook(() => useCreateGreeting());

        await act(async () => {
            await result.current.mutate({
                message: "New greeting",
                recipient: "Alice",
            });
        });

        expect(result.current.loading).toBe(false);
        expect(result.current.data).toEqual(newGreeting);
        expect(result.current.error).toBeNull();
        expect(mockCreateGreeting).toHaveBeenCalledWith({
            createGreetingRequest: {
                message: "New greeting",
                recipient: "Alice",
            },
        });
    });

    it("should set loading state during mutation", async () => {
        mockCreateGreeting.mockImplementation(
            () => new Promise((resolve) => setTimeout(() => resolve(mockGreetings[0]), 100)),
        );

        const { result } = renderHook(() => useCreateGreeting());

        let mutatePromise: Promise<unknown>;
        act(() => {
            mutatePromise = result.current.mutate({ message: "Test" });
        });

        expect(result.current.loading).toBe(true);

        await act(async () => {
            await mutatePromise;
        });

        expect(result.current.loading).toBe(false);
    });

    it("should handle validation errors", async () => {
        const mockResponse = new Response(
            JSON.stringify({
                type: "https://api.example.com/problems/validation-error",
                title: "Validation Error",
                status: 400,
                detail: "Request validation failed",
                errors: { message: "must not be blank" },
            }),
            { status: 400 },
        );
        mockCreateGreeting.mockRejectedValue(new ResponseError(mockResponse, "Bad Request"));

        const { result } = renderHook(() => useCreateGreeting());

        await act(async () => {
            await result.current.mutate({ message: "" });
        });

        expect(result.current.error).not.toBeNull();
        expect(result.current.error?.status).toBe(400);
        expect(result.current.error?.fieldErrors).toEqual({ message: "must not be blank" });
        expect(result.current.data).toBeNull();
    });

    it("should handle unauthorized errors", async () => {
        const mockResponse = new Response(
            JSON.stringify({
                type: "https://api.example.com/problems/unauthorized",
                title: "Unauthorized",
                status: 401,
                detail: "Authentication required",
            }),
            { status: 401 },
        );
        mockCreateGreeting.mockRejectedValue(new ResponseError(mockResponse, "Unauthorized"));

        const { result } = renderHook(() => useCreateGreeting());

        await act(async () => {
            await result.current.mutate({ message: "Test" });
        });

        expect(result.current.error?.status).toBe(401);
    });

    it("should reset state when reset is called", async () => {
        mockCreateGreeting.mockResolvedValue(mockGreetings[0]);

        const { result } = renderHook(() => useCreateGreeting());

        await act(async () => {
            await result.current.mutate({ message: "Test" });
        });

        expect(result.current.data).not.toBeNull();

        act(() => {
            result.current.reset();
        });

        expect(result.current.data).toBeNull();
        expect(result.current.error).toBeNull();
    });
});

describe("useUpdateGreeting", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("should update a greeting successfully", async () => {
        const updatedGreeting = { ...mockGreetings[0], message: "Updated message" };
        mockUpdateGreeting.mockResolvedValue(updatedGreeting);

        const { result } = renderHook(() => useUpdateGreeting());

        await act(async () => {
            await result.current.mutate(mockGreetings[0].id, {
                message: "Updated message",
                recipient: "World",
            });
        });

        expect(result.current.data).toEqual(updatedGreeting);
        expect(mockUpdateGreeting).toHaveBeenCalledWith({
            id: mockGreetings[0].id,
            updateGreetingRequest: {
                message: "Updated message",
                recipient: "World",
            },
        });
    });

    it("should handle not found errors", async () => {
        const mockResponse = new Response(
            JSON.stringify({
                type: "https://api.example.com/problems/not-found",
                title: "Not Found",
                status: 404,
                detail: "Greeting not found",
            }),
            { status: 404 },
        );
        mockUpdateGreeting.mockRejectedValue(new ResponseError(mockResponse, "Not Found"));

        const { result } = renderHook(() => useUpdateGreeting());

        await act(async () => {
            await result.current.mutate(999999, {
                message: "Test",
                recipient: "Test",
            });
        });

        expect(result.current.error?.status).toBe(404);
    });
});

describe("usePatchGreeting", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("should patch a greeting successfully", async () => {
        const patchedGreeting = { ...mockGreetings[0], message: "Patched message" };
        mockPatchGreeting.mockResolvedValue(patchedGreeting);

        const { result } = renderHook(() => usePatchGreeting());

        await act(async () => {
            await result.current.mutate(mockGreetings[0].id, { message: "Patched message" });
        });

        expect(result.current.data).toEqual(patchedGreeting);
        expect(mockPatchGreeting).toHaveBeenCalledWith({
            id: mockGreetings[0].id,
            patchGreetingRequest: { message: "Patched message" },
        });
    });

    it("should allow partial updates", async () => {
        mockPatchGreeting.mockResolvedValue(mockGreetings[0]);

        const { result } = renderHook(() => usePatchGreeting());

        // Only update recipient
        await act(async () => {
            await result.current.mutate(mockGreetings[0].id, { recipient: "New Recipient" });
        });

        expect(mockPatchGreeting).toHaveBeenCalledWith({
            id: mockGreetings[0].id,
            patchGreetingRequest: { recipient: "New Recipient" },
        });
    });
});

describe("useDeleteGreeting", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("should delete a greeting successfully", async () => {
        mockDeleteGreeting.mockResolvedValue(undefined);

        const { result } = renderHook(() => useDeleteGreeting());

        await act(async () => {
            await result.current.mutate(mockGreetings[0].id);
        });

        expect(result.current.loading).toBe(false);
        expect(result.current.error).toBeNull();
        expect(result.current.isSuccess).toBe(true);
        expect(mockDeleteGreeting).toHaveBeenCalledWith({ id: mockGreetings[0].id });
    });

    it("should handle not found errors", async () => {
        const mockResponse = new Response(
            JSON.stringify({
                type: "https://api.example.com/problems/not-found",
                title: "Not Found",
                status: 404,
                detail: "Greeting not found",
            }),
            { status: 404 },
        );
        mockDeleteGreeting.mockRejectedValue(new ResponseError(mockResponse, "Not Found"));

        const { result } = renderHook(() => useDeleteGreeting());

        await act(async () => {
            await result.current.mutate(999999);
        });

        expect(result.current.error?.status).toBe(404);
        expect(result.current.isSuccess).toBe(false);
    });

    it("should reset success state when reset is called", async () => {
        mockDeleteGreeting.mockResolvedValue(undefined);

        const { result } = renderHook(() => useDeleteGreeting());

        await act(async () => {
            await result.current.mutate(mockGreetings[0].id);
        });

        expect(result.current.isSuccess).toBe(true);

        act(() => {
            result.current.reset();
        });

        expect(result.current.isSuccess).toBe(false);
    });
});
