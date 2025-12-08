/**
 * Tests for useGreeting Hook (single item)
 *
 * Tests for fetching a single greeting by ID.
 */

import { describe, it, expect, vi, beforeEach, type Mock } from "vitest";
import { renderHook, waitFor, act } from "@testing-library/react";
import { useGreeting } from "./useGreeting";
import {
    mockGreetings,
    createMockGreeting,
    mockErrors,
    mockApiSuccess,
    mockApiError,
} from "../../../test/mocks/data";

// Mock the API config module
vi.mock("../../../api/config", () => ({
    getGreeting: vi.fn(),
}));

import { getGreeting } from "../../../api/config";

const mockGetGreeting = getGreeting as Mock;

describe("useGreeting", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        // hey-api returns { data, error, request, response }
        mockGetGreeting.mockResolvedValue(mockApiSuccess(mockGreetings[0]));
    });

    describe("fetching a single greeting", () => {
        it("should start in loading state when id is provided", () => {
            const { result } = renderHook(() => useGreeting(mockGreetings[0].id));

            expect(result.current.loading).toBe(true);
            expect(result.current.greeting).toBeNull();
            expect(result.current.error).toBeNull();
        });

        it("should fetch greeting on mount", async () => {
            const { result } = renderHook(() => useGreeting(mockGreetings[0].id));

            await waitFor(() => {
                expect(result.current.loading).toBe(false);
            });

            expect(result.current.greeting).toEqual(mockGreetings[0]);
            expect(result.current.error).toBeNull();
        });

        it("should call API with correct id", async () => {
            const testId = 123456789;
            mockGetGreeting.mockResolvedValue(mockApiSuccess(createMockGreeting({ id: testId })));

            renderHook(() => useGreeting(testId));

            await waitFor(() => {
                expect(mockGetGreeting).toHaveBeenCalledWith({ path: { id: testId } });
            });
        });

        it("should not fetch when id is null", async () => {
            const { result } = renderHook(() => useGreeting(null));

            // Wait a bit to ensure no fetch happened
            await new Promise((resolve) => setTimeout(resolve, 50));

            expect(mockGetGreeting).not.toHaveBeenCalled();
            expect(result.current.loading).toBe(false);
            expect(result.current.greeting).toBeNull();
        });

        it("should not fetch when id is undefined", async () => {
            const { result } = renderHook(() => useGreeting(undefined));

            await new Promise((resolve) => setTimeout(resolve, 50));

            expect(mockGetGreeting).not.toHaveBeenCalled();
            expect(result.current.loading).toBe(false);
            expect(result.current.greeting).toBeNull();
        });
    });

    describe("refetching when id changes", () => {
        it("should refetch when id changes", async () => {
            // Setup mock to return different data based on call order
            mockGetGreeting
                .mockResolvedValueOnce(mockApiSuccess(mockGreetings[0]))
                .mockResolvedValueOnce(mockApiSuccess(mockGreetings[1]));

            const { result, rerender } = renderHook(({ id }) => useGreeting(id), {
                initialProps: { id: mockGreetings[0].id },
            });

            await waitFor(() => {
                expect(result.current.loading).toBe(false);
            });

            expect(result.current.greeting?.id).toBe(mockGreetings[0].id);
            expect(mockGetGreeting).toHaveBeenCalledTimes(1);

            // Change to different id - this should trigger a new fetch
            rerender({ id: mockGreetings[1].id });

            // Wait for loading to start and then finish
            await waitFor(() => {
                expect(result.current.greeting?.id).toBe(mockGreetings[1].id);
            });

            expect(mockGetGreeting).toHaveBeenCalledTimes(2);
            expect(mockGetGreeting).toHaveBeenLastCalledWith({ path: { id: mockGreetings[1].id } });
        });
    });

    describe("refresh", () => {
        it("should refetch data when refresh is called", async () => {
            const { result } = renderHook(() => useGreeting(mockGreetings[0].id));

            await waitFor(() => {
                expect(result.current.loading).toBe(false);
            });

            expect(mockGetGreeting).toHaveBeenCalledTimes(1);

            await act(async () => {
                await result.current.refresh();
            });

            expect(mockGetGreeting).toHaveBeenCalledTimes(2);
        });
    });

    describe("error handling", () => {
        it("should handle not found errors (404)", async () => {
            mockGetGreeting.mockResolvedValue(mockApiError(mockErrors.notFound));

            const { result } = renderHook(() => useGreeting(999999));

            await waitFor(() => {
                expect(result.current.loading).toBe(false);
            });

            expect(result.current.error).not.toBeNull();
            expect(result.current.error?.status).toBe(404);
            expect(result.current.greeting).toBeNull();
        });

        it("should handle network errors", async () => {
            mockGetGreeting.mockRejectedValue(new Error("Network error"));

            const { result } = renderHook(() => useGreeting(mockGreetings[0].id));

            await waitFor(() => {
                expect(result.current.loading).toBe(false);
            });

            expect(result.current.error).not.toBeNull();
            expect(result.current.error?.title).toBe("Network Error");
        });
    });
});
