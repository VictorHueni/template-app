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
import { mockGreetings, createMockGreeting, mockErrors } from "../../../test/mocks/data";

// Mock the API config module
vi.mock("../../../api/config", () => ({
    createGreeting: vi.fn(),
    updateGreeting: vi.fn(),
    patchGreeting: vi.fn(),
    deleteGreeting: vi.fn(),
}));

import { createGreeting, updateGreeting, patchGreeting, deleteGreeting } from "../../../api/config";

const mockCreateGreeting = createGreeting as Mock;
const mockUpdateGreeting = updateGreeting as Mock;
const mockPatchGreeting = patchGreeting as Mock;
const mockDeleteGreeting = deleteGreeting as Mock;

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
        mockCreateGreeting.mockResolvedValue({
            data: newGreeting,
            error: undefined,
        });

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
            body: {
                message: "New greeting",
                recipient: "Alice",
            },
        });
    });

    it("should set loading state during mutation", async () => {
        mockCreateGreeting.mockImplementation(
            () =>
                new Promise((resolve) =>
                    setTimeout(() => resolve({ data: mockGreetings[0], error: undefined }), 100),
                ),
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
        mockCreateGreeting.mockResolvedValue({
            data: undefined,
            error: mockErrors.validationError,
        });

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
        mockCreateGreeting.mockResolvedValue({
            data: undefined,
            error: mockErrors.unauthorized,
        });

        const { result } = renderHook(() => useCreateGreeting());

        await act(async () => {
            await result.current.mutate({ message: "Test" });
        });

        expect(result.current.error?.status).toBe(401);
    });

    it("should reset state when reset is called", async () => {
        mockCreateGreeting.mockResolvedValue({
            data: mockGreetings[0],
            error: undefined,
        });

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
        mockUpdateGreeting.mockResolvedValue({
            data: updatedGreeting,
            error: undefined,
        });

        const { result } = renderHook(() => useUpdateGreeting());

        await act(async () => {
            await result.current.mutate(mockGreetings[0].id, {
                message: "Updated message",
                recipient: "World",
            });
        });

        expect(result.current.data).toEqual(updatedGreeting);
        expect(mockUpdateGreeting).toHaveBeenCalledWith({
            path: { id: mockGreetings[0].id },
            body: {
                message: "Updated message",
                recipient: "World",
            },
        });
    });

    it("should handle not found errors", async () => {
        mockUpdateGreeting.mockResolvedValue({
            data: undefined,
            error: mockErrors.notFound,
        });

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
        mockPatchGreeting.mockResolvedValue({
            data: patchedGreeting,
            error: undefined,
        });

        const { result } = renderHook(() => usePatchGreeting());

        await act(async () => {
            await result.current.mutate(mockGreetings[0].id, { message: "Patched message" });
        });

        expect(result.current.data).toEqual(patchedGreeting);
        expect(mockPatchGreeting).toHaveBeenCalledWith({
            path: { id: mockGreetings[0].id },
            body: { message: "Patched message" },
        });
    });

    it("should allow partial updates", async () => {
        mockPatchGreeting.mockResolvedValue({
            data: mockGreetings[0],
            error: undefined,
        });

        const { result } = renderHook(() => usePatchGreeting());

        // Only update recipient
        await act(async () => {
            await result.current.mutate(mockGreetings[0].id, { recipient: "New Recipient" });
        });

        expect(mockPatchGreeting).toHaveBeenCalledWith({
            path: { id: mockGreetings[0].id },
            body: { recipient: "New Recipient" },
        });
    });
});

describe("useDeleteGreeting", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("should delete a greeting successfully", async () => {
        mockDeleteGreeting.mockResolvedValue({
            data: undefined,
            error: undefined,
        });

        const { result } = renderHook(() => useDeleteGreeting());

        await act(async () => {
            await result.current.mutate(mockGreetings[0].id);
        });

        expect(result.current.loading).toBe(false);
        expect(result.current.error).toBeNull();
        expect(result.current.isSuccess).toBe(true);
        expect(mockDeleteGreeting).toHaveBeenCalledWith({ path: { id: mockGreetings[0].id } });
    });

    it("should handle not found errors", async () => {
        mockDeleteGreeting.mockResolvedValue({
            data: undefined,
            error: mockErrors.notFound,
        });

        const { result } = renderHook(() => useDeleteGreeting());

        await act(async () => {
            await result.current.mutate(999999);
        });

        expect(result.current.error?.status).toBe(404);
        expect(result.current.isSuccess).toBe(false);
    });

    it("should reset success state when reset is called", async () => {
        mockDeleteGreeting.mockResolvedValue({
            data: undefined,
            error: undefined,
        });

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
