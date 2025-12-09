/**
 * useGreetings Hook
 *
 * Custom hook for fetching and managing a paginated list of greetings.
 *
 * WHY A CUSTOM HOOK?
 * ------------------
 * 1. Encapsulation: API logic is separated from component rendering
 * 2. Reusability: Can be used in any component that needs greeting data
 * 3. Testability: Hook can be tested independently of UI
 * 4. Clean components: Components focus on rendering, not data fetching
 *
 * This hook follows the "state machine" pattern:
 * - loading: true → false (on success or error)
 * - greetings: [] → data (on success)
 * - error: null → ApiError (on failure)
 */

import { useState, useEffect, useCallback } from "react";
import { listGreetings } from "../../../api/config";
import { parseApiError, type ApiError } from "../../../api/errors";
import type { GreetingResponse, PageMeta } from "../../../api/generated";

/**
 * Options for the useGreetings hook.
 */
export interface UseGreetingsOptions {
    /** Page number to fetch (0-based). Default: 0 */
    page?: number;
    /** Number of items per page. Default: 20 */
    size?: number;
    /** Whether to fetch automatically on mount. Default: true */
    autoFetch?: boolean;
}

/**
 * Return type of the useGreetings hook.
 */
export interface UseGreetingsResult {
    /** Array of greeting objects */
    greetings: GreetingResponse[];
    /** Pagination metadata */
    meta: PageMeta | null;
    /** Whether a fetch is in progress */
    loading: boolean;
    /** Error information if the fetch failed */
    error: ApiError | null;
    /** Function to manually trigger a refetch */
    refresh: () => Promise<void>;
    /** Function to change the current page */
    setPage: (page: number) => void;
    /** Current page number */
    currentPage: number;
}

/**
 * Hook for fetching a paginated list of greetings.
 *
 * @param options - Configuration options for pagination and behavior
 * @returns Object containing greetings data, loading state, error, and refresh function
 *
 * @example
 * ```tsx
 * function GreetingsList() {
 *   const { greetings, loading, error, meta, refresh } = useGreetings({ page: 0, size: 10 });
 *
 *   if (loading) return <p>Loading...</p>;
 *   if (error) return <p>Error: {error.detail}</p>;
 *
 *   return (
 *     <>
 *       <ul>
 *         {greetings.map(g => <li key={g.id}>{g.message}</li>)}
 *       </ul>
 *       <p>Page {meta.pageNumber + 1} of {meta.totalPages}</p>
 *       <button onClick={refresh}>Refresh</button>
 *     </>
 *   );
 * }
 * ```
 */
export function useGreetings(options: UseGreetingsOptions = {}): UseGreetingsResult {
    const { page: initialPage = 0, size = 20, autoFetch = true } = options;

    // State management using individual useState calls
    // This is simpler than useReducer for this use case
    const [greetings, setGreetings] = useState<GreetingResponse[]>([]);
    const [meta, setMeta] = useState<PageMeta | null>(null);
    const [loading, setLoading] = useState(autoFetch);
    const [error, setError] = useState<ApiError | null>(null);
    const [currentPage, setCurrentPage] = useState(initialPage);

    /**
     * Fetch greetings from the API.
     * Wrapped in useCallback to maintain stable reference for useEffect dependency.
     */
    const fetchGreetings = useCallback(async () => {
        setLoading(true);
        setError(null);

        try {
            // Call the generated SDK function
            const { data, error: responseError } = await listGreetings({
                query: { page: currentPage, size },
            });

            if (responseError) {
                // Parse the error into our structured ApiError format
                const apiError = await parseApiError(responseError);
                setError(apiError);
                setGreetings([]);
                setMeta(null);
            } else if (data) {
                setGreetings(data.data);
                setMeta(data.meta);
            }
        } catch (e) {
            // Handle unexpected errors (network errors, etc.)
            const apiError = await parseApiError(e);
            setError(apiError);
            setGreetings([]);
            setMeta(null);
        } finally {
            setLoading(false);
        }
    }, [currentPage, size]);

    /**
     * Effect to fetch data on mount and when pagination changes.
     * Only runs if autoFetch is true.
     */
    useEffect(() => {
        if (autoFetch) {
            void fetchGreetings();
        }
    }, [fetchGreetings, autoFetch]);

    /**
     * Manual refresh function.
     * Exposed so components can trigger refetch (e.g., after mutations).
     */
    const refresh = useCallback(async () => {
        await fetchGreetings();
    }, [fetchGreetings]);

    /**
     * Function to change the current page.
     * Triggers a new fetch automatically via the useEffect.
     */
    const setPage = useCallback((page: number) => {
        setCurrentPage(page);
    }, []);

    return {
        greetings,
        meta,
        loading,
        error,
        refresh,
        setPage,
        currentPage,
    };
}
