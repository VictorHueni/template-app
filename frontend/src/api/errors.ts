/**
 * Error Handling Utilities
 *
 * This module provides type-safe error handling for API responses.
 * It works with the RFC 7807 Problem Detail format returned by the backend.
 *
 * WHY THIS PATTERN?
 * -----------------
 * 1. Type safety: Properly typed error objects from API responses
 * 2. User-friendly messages: Transform technical errors into readable text
 * 3. Consistent handling: Same error handling logic everywhere
 * 4. Validation errors: Special handling for field-level validation
 */

import type { ProblemDetail } from "./generated";

/**
 * Extended error type that includes parsed ProblemDetail from the API.
 * This gives us structured access to error information.
 */
export interface ApiError {
    /** HTTP status code (e.g., 400, 404, 500) */
    status: number;
    /** Short error title (e.g., "Validation Error", "Not Found") */
    title: string;
    /** Detailed error message */
    detail: string;
    /** Field-level validation errors (field name -> error message) */
    fieldErrors?: Record<string, string>;
    /** Original ProblemDetail if parsing succeeded */
    problemDetail?: ProblemDetail;
    /** Original error for debugging */
    originalError: unknown;
}

/**
 * Type guard to check if a value is a ProblemDetail object.
 * Uses duck typing to validate the required fields.
 */
export function isProblemDetail(value: unknown): value is ProblemDetail {
    if (typeof value !== "object" || value === null) {
        return false;
    }
    const obj = value as Record<string, unknown>;
    return (
        typeof obj.type === "string" &&
        typeof obj.title === "string" &&
        typeof obj.status === "number"
    );
}

/**
 * Get a default user-friendly message for common HTTP status codes.
 * These messages match the backend's RFC 7807 ProblemDetail responses.
 */
function getDefaultMessageForStatus(status: number): string {
    switch (status) {
        case 401:
            return "Authentication is required to access this resource";
        case 403:
            return "You do not have permission to access this resource";
        case 404:
            return "The requested resource was not found";
        default:
            return `HTTP error ${status}`;
    }
}

/**
 * Get a default title for common HTTP status codes.
 */
function getDefaultTitleForStatus(status: number, statusText: string): string {
    if (statusText) {
        return statusText;
    }
    switch (status) {
        case 400:
            return "Bad Request";
        case 401:
            return "Unauthorized";
        case 403:
            return "Forbidden";
        case 404:
            return "Not Found";
        case 409:
            return "Conflict";
        case 500:
            return "Internal Server Error";
        default:
            return `Error ${status}`;
    }
}

/**
 * Parse an error into a structured ApiError object.
 *
 * This function handles:
 * 1. Response objects (from gateway 401 without JSON body)
 * 2. hey-api response objects with ProblemDetail
 * 3. Standard Error objects (network errors, etc.)
 * 4. Unknown error types (fallback)
 *
 * @param error - The caught error (unknown type for safety)
 * @returns Structured ApiError with all available information
 *
 * @example
 * ```typescript
 * const result = await createGreeting({ body: { ... } });
 * if (result.error) {
 *   const apiError = await parseApiError(result.error);
 *   if (apiError.fieldErrors) {
 *     // Show validation errors on form fields
 *   } else {
 *     // Show general error message
 *   }
 * }
 * ```
 */
export async function parseApiError(error: unknown): Promise<ApiError> {
    // Handle Response objects (from hey-api when gateway returns 401 without JSON body)
    if (error instanceof Response) {
        const status = error.status;
        const title = getDefaultTitleForStatus(status, error.statusText);

        // Try to parse body as ProblemDetail (if the backend returned JSON)
        try {
            const body = await error.json();
            if (isProblemDetail(body)) {
                return {
                    status: body.status,
                    title: body.title,
                    detail: body.detail ?? body.title,
                    fieldErrors: body.errors,
                    problemDetail: body,
                    originalError: error,
                };
            }
        } catch {
            // No JSON body or parse error - use status from Response
        }

        return {
            status,
            title,
            detail: getDefaultMessageForStatus(status),
            originalError: error,
        };
    }

    // Handle hey-api error responses (ProblemDetail objects)
    if (isProblemDetail(error)) {
        return {
            status: error.status,
            title: error.title,
            detail: error.detail ?? error.title,
            fieldErrors: error.errors,
            problemDetail: error,
            originalError: error,
        };
    }

    // Handle standard JavaScript Error
    if (error instanceof Error) {
        return {
            status: 0, // 0 indicates a client-side error (no HTTP response)
            title: "Network Error",
            detail: error.message,
            originalError: error,
        };
    }

    // Handle plain objects that might have status/message
    if (typeof error === "object" && error !== null) {
        const obj = error as Record<string, unknown>;
        const status = typeof obj.status === "number" ? obj.status : 0;

        let message: string;
        if (typeof obj.message === "string") {
            message = obj.message;
        } else if (typeof obj.detail === "string") {
            message = obj.detail;
        } else {
            message = String(error);
        }

        return {
            status,
            title: `Error ${status || "Unknown"}`,
            detail: message,
            originalError: error,
        };
    }

    // Fallback for unknown error types
    return {
        status: 0,
        title: "Unknown Error",
        detail: String(error),
        originalError: error,
    };
}

/**
 * Get a user-friendly error message from an ApiError.
 *
 * This provides a single string suitable for displaying in a toast/alert.
 * For validation errors, it combines all field errors into one message.
 */
export function getErrorMessage(apiError: ApiError): string {
    // If there are field-level validation errors, combine them
    if (apiError.fieldErrors) {
        const fieldMessages = Object.entries(apiError.fieldErrors)
            .map(([field, message]) => `${field}: ${message}`)
            .join("; ");
        return `Validation failed: ${fieldMessages}`;
    }

    // Otherwise return the detail message
    return apiError.detail;
}

/**
 * Check if an error is specifically a "not found" error (404).
 * Useful for showing "item doesn't exist" UI vs. generic error.
 */
export function isNotFoundError(apiError: ApiError): boolean {
    return apiError.status === 404;
}

/**
 * Check if an error is an authentication error (401).
 * Useful for redirecting to login.
 */
export function isUnauthorizedError(apiError: ApiError): boolean {
    return apiError.status === 401;
}

/**
 * Check if an error is a forbidden error (403).
 * User is authenticated but lacks permission.
 */
export function isForbiddenError(apiError: ApiError): boolean {
    return apiError.status === 403;
}

/**
 * Check if an error is a validation error (400 with field errors).
 * Useful for showing inline form validation.
 */
export function isValidationError(apiError: ApiError): boolean {
    return apiError.status === 400 && apiError.fieldErrors !== undefined;
}
