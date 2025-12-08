/**
 * useGreeting Hook
 *
 * Custom hook for fetching a single greeting by ID.
 *
 * WHY A SEPARATE HOOK?
 * --------------------
 * - Single Responsibility: This hook handles one thing - fetching one greeting
 * - Simpler state: No pagination complexity
 * - Different use cases: Detail pages, edit forms, etc.
 */

import { useState, useEffect, useCallback } from "react";
import { greetingsApiPublic } from "../../../api/config";
import { parseApiError, type ApiError } from "../../../api/errors";
import type { GreetingResponse } from "../../../api/generated";

/**
 * Return type of the useGreeting hook.
 */
export interface UseGreetingResult {
    /** The greeting object, or null if not loaded */
    greeting: GreetingResponse | null;
    /** Whether a fetch is in progress */
    loading: boolean;
    /** Error information if the fetch failed */
    error: ApiError | null;
    /** Function to manually trigger a refetch */
    refresh: () => Promise<void>;
}

/**
 * Hook for fetching a single greeting by ID.
 *
 * @param id - The greeting ID to fetch, or null/undefined to skip fetching
 * @returns Object containing greeting data, loading state, error, and refresh function
 *
 * @example
 * ```tsx
 * function GreetingDetail({ id }: { id: number }) {
 *   const { greeting, loading, error } = useGreeting(id);
 *
 *   if (loading) return <p>Loading...</p>;
 *   if (error) return <p>Error: {error.detail}</p>;
 *   if (!greeting) return <p>Not found</p>;
 *
 *   return (
 *     <article>
 *       <h2>{greeting.message}</h2>
 *       <p>To: {greeting.recipient}</p>
 *       <small>Ref: {greeting.reference}</small>
 *     </article>
 *   );
 * }
 * ```
 */
export function useGreeting(id: number | null | undefined): UseGreetingResult {
    const [greeting, setGreeting] = useState<GreetingResponse | null>(null);
    const [loading, setLoading] = useState(id != null);
    const [error, setError] = useState<ApiError | null>(null);

    const fetchGreeting = useCallback(async () => {
        if (id == null) {
            return;
        }

        setLoading(true);
        setError(null);

        try {
            const response = await greetingsApiPublic.getGreeting({ id });
            setGreeting(response);
        } catch (e) {
            const apiError = await parseApiError(e);
            setError(apiError);
            setGreeting(null);
        } finally {
            setLoading(false);
        }
    }, [id]);

    useEffect(() => {
        if (id != null) {
            void fetchGreeting();
        } else {
            // Reset state when id becomes null/undefined
            setGreeting(null);
            setLoading(false);
            setError(null);
        }
    }, [id, fetchGreeting]);

    const refresh = useCallback(async () => {
        await fetchGreeting();
    }, [fetchGreeting]);

    return {
        greeting,
        loading,
        error,
        refresh,
    };
}
