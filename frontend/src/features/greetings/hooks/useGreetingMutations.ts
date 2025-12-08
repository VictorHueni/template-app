/**
 * Greeting Mutation Hooks
 *
 * Custom hooks for creating, updating, patching, and deleting greetings.
 * These operations require authentication and modify server state.
 *
 * WHY SEPARATE HOOKS FOR MUTATIONS?
 * ---------------------------------
 * 1. Clear separation of concerns: Read vs. Write operations
 * 2. Different state needs: Mutations track success/loading differently
 * 3. Simpler components: Each hook has a single purpose
 * 4. Reusability: Can be used in forms, modals, etc.
 */

import { useState, useCallback } from "react";
import { greetingsApi } from "../../../api/config";
import { parseApiError, type ApiError } from "../../../api/errors";
import type {
    GreetingResponse,
    CreateGreetingRequest,
    UpdateGreetingRequest,
    PatchGreetingRequest,
} from "../../../api/generated";

// ============================================================================
// Common Types
// ============================================================================

/**
 * Base result type for mutation hooks.
 */
interface MutationResult<TData> {
    /** The result data from a successful mutation */
    data: TData | null;
    /** Whether a mutation is in progress */
    loading: boolean;
    /** Error information if the mutation failed */
    error: ApiError | null;
    /** Reset the hook state to initial values */
    reset: () => void;
}

// ============================================================================
// useCreateGreeting
// ============================================================================

export interface UseCreateGreetingResult extends MutationResult<GreetingResponse> {
    /** Execute the create mutation */
    mutate: (data: CreateGreetingRequest) => Promise<GreetingResponse | null>;
}

/**
 * Hook for creating a new greeting.
 *
 * @example
 * ```tsx
 * function CreateForm() {
 *   const { mutate, loading, error } = useCreateGreeting();
 *
 *   const handleSubmit = async (data: CreateGreetingRequest) => {
 *     const result = await mutate(data);
 *     if (result) {
 *       // Success - navigate or show message
 *     }
 *   };
 *
 *   return <form onSubmit={handleSubmit}>...</form>;
 * }
 * ```
 */
export function useCreateGreeting(): UseCreateGreetingResult {
    const [data, setData] = useState<GreetingResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<ApiError | null>(null);

    const mutate = useCallback(
        async (input: CreateGreetingRequest): Promise<GreetingResponse | null> => {
            setLoading(true);
            setError(null);

            try {
                const response = await greetingsApi.createGreeting({
                    createGreetingRequest: input,
                });
                setData(response);
                return response;
            } catch (e) {
                const apiError = await parseApiError(e);
                setError(apiError);
                setData(null);
                return null;
            } finally {
                setLoading(false);
            }
        },
        [],
    );

    const reset = useCallback(() => {
        setData(null);
        setError(null);
        setLoading(false);
    }, []);

    return { data, loading, error, mutate, reset };
}

// ============================================================================
// useUpdateGreeting
// ============================================================================

export interface UseUpdateGreetingResult extends MutationResult<GreetingResponse> {
    /** Execute the update mutation (full replacement) */
    mutate: (id: number, data: UpdateGreetingRequest) => Promise<GreetingResponse | null>;
}

/**
 * Hook for updating a greeting (full replacement - PUT).
 * All fields must be provided.
 *
 * @example
 * ```tsx
 * function EditForm({ greeting }: { greeting: GreetingResponse }) {
 *   const { mutate, loading, error } = useUpdateGreeting();
 *
 *   const handleSubmit = async (data: UpdateGreetingRequest) => {
 *     const result = await mutate(greeting.id, data);
 *     if (result) {
 *       // Success
 *     }
 *   };
 *
 *   return <form>...</form>;
 * }
 * ```
 */
export function useUpdateGreeting(): UseUpdateGreetingResult {
    const [data, setData] = useState<GreetingResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<ApiError | null>(null);

    const mutate = useCallback(
        async (id: number, input: UpdateGreetingRequest): Promise<GreetingResponse | null> => {
            setLoading(true);
            setError(null);

            try {
                const response = await greetingsApi.updateGreeting({
                    id,
                    updateGreetingRequest: input,
                });
                setData(response);
                return response;
            } catch (e) {
                const apiError = await parseApiError(e);
                setError(apiError);
                setData(null);
                return null;
            } finally {
                setLoading(false);
            }
        },
        [],
    );

    const reset = useCallback(() => {
        setData(null);
        setError(null);
        setLoading(false);
    }, []);

    return { data, loading, error, mutate, reset };
}

// ============================================================================
// usePatchGreeting
// ============================================================================

export interface UsePatchGreetingResult extends MutationResult<GreetingResponse> {
    /** Execute the patch mutation (partial update) */
    mutate: (id: number, data: PatchGreetingRequest) => Promise<GreetingResponse | null>;
}

/**
 * Hook for partially updating a greeting (PATCH).
 * Only provided fields are updated.
 *
 * @example
 * ```tsx
 * function QuickEdit({ greeting }: { greeting: GreetingResponse }) {
 *   const { mutate, loading } = usePatchGreeting();
 *
 *   const updateMessage = async (newMessage: string) => {
 *     await mutate(greeting.id, { message: newMessage });
 *   };
 *
 *   return <button onClick={() => updateMessage("Updated!")}>Update</button>;
 * }
 * ```
 */
export function usePatchGreeting(): UsePatchGreetingResult {
    const [data, setData] = useState<GreetingResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<ApiError | null>(null);

    const mutate = useCallback(
        async (id: number, input: PatchGreetingRequest): Promise<GreetingResponse | null> => {
            setLoading(true);
            setError(null);

            try {
                const response = await greetingsApi.patchGreeting({
                    id,
                    patchGreetingRequest: input,
                });
                setData(response);
                return response;
            } catch (e) {
                const apiError = await parseApiError(e);
                setError(apiError);
                setData(null);
                return null;
            } finally {
                setLoading(false);
            }
        },
        [],
    );

    const reset = useCallback(() => {
        setData(null);
        setError(null);
        setLoading(false);
    }, []);

    return { data, loading, error, mutate, reset };
}

// ============================================================================
// useDeleteGreeting
// ============================================================================

export interface UseDeleteGreetingResult {
    /** Whether a delete is in progress */
    loading: boolean;
    /** Error information if the delete failed */
    error: ApiError | null;
    /** Whether the delete was successful */
    isSuccess: boolean;
    /** Execute the delete mutation */
    mutate: (id: number) => Promise<boolean>;
    /** Reset the hook state */
    reset: () => void;
}

/**
 * Hook for deleting a greeting.
 *
 * @example
 * ```tsx
 * function DeleteButton({ id, onDeleted }: { id: number; onDeleted: () => void }) {
 *   const { mutate, loading, error, isSuccess } = useDeleteGreeting();
 *
 *   const handleDelete = async () => {
 *     if (confirm("Are you sure?")) {
 *       const success = await mutate(id);
 *       if (success) onDeleted();
 *     }
 *   };
 *
 *   return (
 *     <>
 *       <button onClick={handleDelete} disabled={loading}>
 *         {loading ? "Deleting..." : "Delete"}
 *       </button>
 *       {error && <p>Error: {error.detail}</p>}
 *     </>
 *   );
 * }
 * ```
 */
export function useDeleteGreeting(): UseDeleteGreetingResult {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<ApiError | null>(null);
    const [isSuccess, setIsSuccess] = useState(false);

    const mutate = useCallback(async (id: number): Promise<boolean> => {
        setLoading(true);
        setError(null);
        setIsSuccess(false);

        try {
            await greetingsApi.deleteGreeting({ id });
            setIsSuccess(true);
            return true;
        } catch (e) {
            const apiError = await parseApiError(e);
            setError(apiError);
            return false;
        } finally {
            setLoading(false);
        }
    }, []);

    const reset = useCallback(() => {
        setError(null);
        setLoading(false);
        setIsSuccess(false);
    }, []);

    return { loading, error, isSuccess, mutate, reset };
}
