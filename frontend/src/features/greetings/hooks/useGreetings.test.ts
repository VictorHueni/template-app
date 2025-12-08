/**
 * Tests for useGreetings Hook
 *
 * These tests verify that the useGreetings hook correctly:
 * 1. Fetches paginated greetings from the API
 * 2. Manages loading state
 * 3. Handles errors properly
 * 4. Supports pagination
 * 5. Supports manual refresh
 *
 * TESTING STRATEGY:
 * -----------------
 * We mock the API config module to return a mocked listGreetings function.
 * This approach:
 * - Is faster than network-level mocking (MSW)
 * - Works reliably with jsdom
 * - Tests the hook logic without network complexity
 * - Still exercises the real hook code
 */

import { describe, it, expect, vi, beforeEach, type Mock } from "vitest";
import { renderHook, waitFor, act } from "@testing-library/react";
import { useGreetings } from "./useGreetings";
import {
    mockGreetings,
    createMockGreetingPage,
    mockErrors,
    mockApiSuccess,
    mockApiError,
} from "../../../test/mocks/data";

// Mock the API config module
vi.mock("../../../api/config", () => ({
    listGreetings: vi.fn(),
}));

// Import the mocked module to control it in tests
import { listGreetings } from "../../../api/config";

// Type helper for mocked function
const mockListGreetings = listGreetings as Mock;

describe("useGreetings", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        // Default: return mock data successfully (hey-api returns { data, error, request, response })
        mockListGreetings.mockResolvedValue(mockApiSuccess(createMockGreetingPage(mockGreetings)));
    });

    /**
     * Basic Functionality Tests
     */
    describe("fetching greetings", () => {
        it("should start in loading state", () => {
            const { result } = renderHook(() => useGreetings());

            expect(result.current.loading).toBe(true);
            expect(result.current.greetings).toEqual([]);
            expect(result.current.error).toBeNull();
        });

        it("should fetch greetings on mount", async () => {
            const { result } = renderHook(() => useGreetings());

            await waitFor(() => {
                expect(result.current.loading).toBe(false);
            });

            expect(result.current.greetings).toHaveLength(mockGreetings.length);
            expect(result.current.greetings[0].message).toBe(mockGreetings[0].message);
            expect(result.current.error).toBeNull();
        });

        it("should include pagination metadata", async () => {
            const { result } = renderHook(() => useGreetings());

            await waitFor(() => {
                expect(result.current.loading).toBe(false);
            });

            expect(result.current.meta).toBeDefined();
            expect(result.current.meta?.pageNumber).toBe(0);
            expect(result.current.meta?.totalElements).toBe(mockGreetings.length);
        });

        it("should call API with correct parameters", async () => {
            renderHook(() => useGreetings({ page: 2, size: 10 }));

            await waitFor(() => {
                expect(mockListGreetings).toHaveBeenCalledWith({
                    query: { page: 2, size: 10 },
                });
            });
        });
    });

    /**
     * Pagination Tests
     */
    describe("pagination", () => {
        it("should fetch specific page when setPage is called", async () => {
            mockListGreetings
                .mockResolvedValueOnce(
                    mockApiSuccess(createMockGreetingPage(mockGreetings, { pageNumber: 0 })),
                )
                .mockResolvedValueOnce(
                    mockApiSuccess(createMockGreetingPage([], { pageNumber: 1 })),
                );

            const { result } = renderHook(() => useGreetings());

            await waitFor(() => {
                expect(result.current.loading).toBe(false);
            });

            expect(result.current.meta?.pageNumber).toBe(0);
            expect(result.current.currentPage).toBe(0);

            // Change to page 1 using setPage
            act(() => {
                result.current.setPage(1);
            });

            await waitFor(() => {
                expect(result.current.currentPage).toBe(1);
                expect(result.current.meta?.pageNumber).toBe(1);
            });
        });

        it("should respect custom page size", async () => {
            const singleItemPage = createMockGreetingPage([mockGreetings[0]], {
                pageSize: 1,
                totalPages: 3,
            });
            mockListGreetings.mockResolvedValue(mockApiSuccess(singleItemPage));

            const { result } = renderHook(() => useGreetings({ size: 1 }));

            await waitFor(() => {
                expect(result.current.loading).toBe(false);
            });

            expect(result.current.greetings).toHaveLength(1);
            expect(result.current.meta?.pageSize).toBe(1);
            expect(result.current.meta?.totalPages).toBe(3);
        });
    });

    /**
     * Refresh Functionality Tests
     */
    describe("refresh", () => {
        it("should refetch data when refresh is called", async () => {
            const { result } = renderHook(() => useGreetings());

            await waitFor(() => {
                expect(result.current.loading).toBe(false);
            });

            expect(mockListGreetings).toHaveBeenCalledTimes(1);

            // Trigger refresh
            await act(async () => {
                await result.current.refresh();
            });

            expect(mockListGreetings).toHaveBeenCalledTimes(2);
            expect(result.current.error).toBeNull();
        });

        it("should set loading state during refresh", async () => {
            const { result } = renderHook(() => useGreetings());

            await waitFor(() => {
                expect(result.current.loading).toBe(false);
            });

            // Start refresh but don't await
            let refreshPromise: Promise<void>;
            act(() => {
                refreshPromise = result.current.refresh();
            });

            // Should be loading now
            expect(result.current.loading).toBe(true);

            await act(async () => {
                await refreshPromise;
            });

            expect(result.current.loading).toBe(false);
        });
    });

    /**
     * Error Handling Tests
     */
    describe("error handling", () => {
        it("should handle network errors", async () => {
            mockListGreetings.mockRejectedValue(new Error("Network error"));

            const { result } = renderHook(() => useGreetings());

            await waitFor(() => {
                expect(result.current.loading).toBe(false);
            });

            expect(result.current.error).not.toBeNull();
            expect(result.current.error?.title).toBe("Network Error");
            expect(result.current.greetings).toEqual([]);
        });

        it("should handle server errors (500)", async () => {
            // hey-api returns errors as { data: undefined, error: ProblemDetail }
            mockListGreetings.mockResolvedValue(mockApiError(mockErrors.serverError));

            const { result } = renderHook(() => useGreetings());

            await waitFor(() => {
                expect(result.current.loading).toBe(false);
            });

            expect(result.current.error).not.toBeNull();
            expect(result.current.error?.status).toBe(500);
        });

        it("should clear error on successful refetch", async () => {
            // First call fails
            mockListGreetings.mockResolvedValueOnce(mockApiError(mockErrors.serverError));

            const { result } = renderHook(() => useGreetings());

            await waitFor(() => {
                expect(result.current.error).not.toBeNull();
            });

            // Setup successful response for refresh
            mockListGreetings.mockResolvedValue(
                mockApiSuccess(createMockGreetingPage(mockGreetings)),
            );

            // Refresh should succeed
            await act(async () => {
                await result.current.refresh();
            });

            expect(result.current.error).toBeNull();
            expect(result.current.greetings).toHaveLength(mockGreetings.length);
        });
    });

    /**
     * Auto-fetch Control Tests
     */
    describe("auto-fetch control", () => {
        it("should not fetch automatically when autoFetch is false", async () => {
            const { result } = renderHook(() => useGreetings({ autoFetch: false }));

            // Wait a bit to ensure no fetch happened
            await new Promise((resolve) => setTimeout(resolve, 50));

            expect(mockListGreetings).not.toHaveBeenCalled();
            expect(result.current.loading).toBe(false);
            expect(result.current.greetings).toEqual([]);
        });

        it("should fetch when refresh is called with autoFetch false", async () => {
            const { result } = renderHook(() => useGreetings({ autoFetch: false }));

            expect(result.current.greetings).toEqual([]);
            expect(mockListGreetings).not.toHaveBeenCalled();

            await act(async () => {
                await result.current.refresh();
            });

            expect(mockListGreetings).toHaveBeenCalledTimes(1);
            expect(result.current.greetings).toHaveLength(mockGreetings.length);
        });
    });
});
