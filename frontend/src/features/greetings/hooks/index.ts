/**
 * Greeting Hooks - Public API
 *
 * This file exports all greeting-related hooks for easy importing.
 */

// Query hooks (read operations)
export { useGreetings, type UseGreetingsOptions, type UseGreetingsResult } from "./useGreetings";
export { useGreeting, type UseGreetingResult } from "./useGreeting";

// Mutation hooks (write operations)
export {
    useCreateGreeting,
    useUpdateGreeting,
    usePatchGreeting,
    useDeleteGreeting,
    type UseCreateGreetingResult,
    type UseUpdateGreetingResult,
    type UsePatchGreetingResult,
    type UseDeleteGreetingResult,
} from "./useGreetingMutations";
