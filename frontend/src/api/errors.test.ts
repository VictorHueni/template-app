import { describe, it, expect } from "vitest";

import {
    parseApiError,
    isProblemDetail,
    isUnauthorizedError,
    isForbiddenError,
    isNotFoundError,
    isValidationError,
    getErrorMessage,
} from "./errors";

describe("isProblemDetail", () => {
    it("returns true for valid ProblemDetail object", () => {
        const problemDetail = {
            type: "https://api.example.com/problems/unauthorized",
            title: "Unauthorized",
            status: 401,
        };
        expect(isProblemDetail(problemDetail)).toBe(true);
    });

    it("returns true for ProblemDetail with optional fields", () => {
        const problemDetail = {
            type: "https://api.example.com/problems/validation-error",
            title: "Validation Error",
            status: 400,
            detail: "Request validation failed",
            instance: "/api/v1/greetings",
            timestamp: "2026-01-15T10:30:00Z",
            traceId: "abc-123",
            errors: { message: "must not be blank" },
        };
        expect(isProblemDetail(problemDetail)).toBe(true);
    });

    it("returns false for null", () => {
        expect(isProblemDetail(null)).toBe(false);
    });

    it("returns false for undefined", () => {
        expect(isProblemDetail(undefined)).toBe(false);
    });

    it("returns false for primitive values", () => {
        expect(isProblemDetail("error")).toBe(false);
        expect(isProblemDetail(123)).toBe(false);
        expect(isProblemDetail(true)).toBe(false);
    });

    it("returns false when missing required fields", () => {
        expect(isProblemDetail({ type: "test", title: "Test" })).toBe(false);
        expect(isProblemDetail({ type: "test", status: 400 })).toBe(false);
        expect(isProblemDetail({ title: "Test", status: 400 })).toBe(false);
    });

    it("returns false when fields have wrong types", () => {
        expect(isProblemDetail({ type: 123, title: "Test", status: 400 })).toBe(false);
        expect(isProblemDetail({ type: "test", title: 123, status: 400 })).toBe(false);
        expect(isProblemDetail({ type: "test", title: "Test", status: "400" })).toBe(false);
    });
});

describe("parseApiError", () => {
    describe("Response object handling (gateway 401 without body)", () => {
        it("extracts 401 status from Response without body", async () => {
            const response = new Response(null, { status: 401, statusText: "Unauthorized" });
            const apiError = await parseApiError(response);

            expect(apiError.status).toBe(401);
            expect(apiError.title).toBe("Unauthorized");
            expect(isUnauthorizedError(apiError)).toBe(true);
        });

        it("extracts 403 status from Response without body", async () => {
            const response = new Response(null, { status: 403, statusText: "Forbidden" });
            const apiError = await parseApiError(response);

            expect(apiError.status).toBe(403);
            expect(apiError.title).toBe("Forbidden");
            expect(isForbiddenError(apiError)).toBe(true);
        });

        it("extracts 404 status from Response without body", async () => {
            const response = new Response(null, { status: 404, statusText: "Not Found" });
            const apiError = await parseApiError(response);

            expect(apiError.status).toBe(404);
            expect(apiError.title).toBe("Not Found");
            expect(isNotFoundError(apiError)).toBe(true);
        });

        it("handles Response with empty statusText", async () => {
            const response = new Response(null, { status: 500, statusText: "" });
            const apiError = await parseApiError(response);

            expect(apiError.status).toBe(500);
            expect(apiError.title).toBe("Internal Server Error");
        });

        it("provides default detail message for common status codes", async () => {
            const response401 = new Response(null, { status: 401 });
            const error401 = await parseApiError(response401);
            expect(error401.detail).toBe("Authentication is required to access this resource");

            const response403 = new Response(null, { status: 403 });
            const error403 = await parseApiError(response403);
            expect(error403.detail).toBe("You do not have permission to access this resource");

            const response404 = new Response(null, { status: 404 });
            const error404 = await parseApiError(response404);
            expect(error404.detail).toBe("The requested resource was not found");
        });

        it("provides generic detail message for unknown status codes", async () => {
            const response = new Response(null, { status: 418 });
            const apiError = await parseApiError(response);
            expect(apiError.detail).toBe("HTTP error 418");
        });
    });

    describe("Response with ProblemDetail body", () => {
        it("parses ProblemDetail from Response body when present", async () => {
            const problemDetail = {
                type: "https://api.example.com/problems/unauthorized",
                title: "Unauthorized",
                status: 401,
                detail: "Authentication is required",
                instance: "/api/v1/me",
                timestamp: "2026-01-15T10:30:00Z",
                traceId: "abc-123",
            };
            const response = new Response(JSON.stringify(problemDetail), {
                status: 401,
                headers: { "Content-Type": "application/problem+json" },
            });

            const apiError = await parseApiError(response);

            expect(apiError.status).toBe(401);
            expect(apiError.title).toBe("Unauthorized");
            expect(apiError.detail).toBe("Authentication is required");
            expect(apiError.problemDetail).toBeDefined();
            expect(apiError.problemDetail?.traceId).toBe("abc-123");
        });

        it("extracts field errors from ProblemDetail", async () => {
            const problemDetail = {
                type: "https://api.example.com/problems/validation-error",
                title: "Validation Error",
                status: 400,
                detail: "Request validation failed",
                errors: {
                    message: "must not be blank",
                    recipient: "must not be null",
                },
            };
            const response = new Response(JSON.stringify(problemDetail), {
                status: 400,
                headers: { "Content-Type": "application/problem+json" },
            });

            const apiError = await parseApiError(response);

            expect(apiError.status).toBe(400);
            expect(apiError.fieldErrors).toEqual({
                message: "must not be blank",
                recipient: "must not be null",
            });
            expect(isValidationError(apiError)).toBe(true);
        });

        it("falls back to Response status when body is not valid ProblemDetail", async () => {
            const response = new Response(JSON.stringify({ error: "something went wrong" }), {
                status: 500,
                headers: { "Content-Type": "application/json" },
            });

            const apiError = await parseApiError(response);

            expect(apiError.status).toBe(500);
        });

        it("falls back to Response status when body is not JSON", async () => {
            const response = new Response("Internal Server Error", {
                status: 500,
                headers: { "Content-Type": "text/plain" },
            });

            const apiError = await parseApiError(response);

            expect(apiError.status).toBe(500);
        });
    });

    describe("ProblemDetail object handling (direct from hey-api)", () => {
        it("handles ProblemDetail objects directly", async () => {
            const problemDetail = {
                type: "https://api.example.com/problems/forbidden",
                title: "Forbidden",
                status: 403,
                detail: "You do not have permission to access this resource",
            };

            const apiError = await parseApiError(problemDetail);

            expect(apiError.status).toBe(403);
            expect(apiError.title).toBe("Forbidden");
            expect(apiError.detail).toBe("You do not have permission to access this resource");
            expect(isForbiddenError(apiError)).toBe(true);
        });
    });

    describe("Error object handling", () => {
        it("handles standard JavaScript Error", async () => {
            const error = new Error("Network request failed");

            const apiError = await parseApiError(error);

            expect(apiError.status).toBe(0);
            expect(apiError.title).toBe("Network Error");
            expect(apiError.detail).toBe("Network request failed");
        });
    });

    describe("plain object handling", () => {
        it("handles plain objects with status and message", async () => {
            const error = { status: 502, message: "Bad Gateway" };

            const apiError = await parseApiError(error);

            expect(apiError.status).toBe(502);
            expect(apiError.detail).toBe("Bad Gateway");
        });

        it("handles plain objects with detail property", async () => {
            const error = { status: 400, detail: "Invalid request" };

            const apiError = await parseApiError(error);

            expect(apiError.status).toBe(400);
            expect(apiError.detail).toBe("Invalid request");
        });
    });

    describe("unknown error handling", () => {
        it("handles unknown error types", async () => {
            const apiError = await parseApiError("Something went wrong");

            expect(apiError.status).toBe(0);
            expect(apiError.title).toBe("Unknown Error");
            expect(apiError.detail).toBe("Something went wrong");
        });
    });
});

describe("getErrorMessage", () => {
    it("returns detail for simple errors", () => {
        const apiError = {
            status: 404,
            title: "Not Found",
            detail: "Greeting with id '123' not found",
            originalError: null,
        };

        expect(getErrorMessage(apiError)).toBe("Greeting with id '123' not found");
    });

    it("combines field errors for validation errors", () => {
        const apiError = {
            status: 400,
            title: "Validation Error",
            detail: "Request validation failed",
            fieldErrors: {
                message: "must not be blank",
                recipient: "must not be null",
            },
            originalError: null,
        };

        const message = getErrorMessage(apiError);
        expect(message).toContain("Validation failed:");
        expect(message).toContain("message: must not be blank");
        expect(message).toContain("recipient: must not be null");
    });
});

describe("error type guards", () => {
    it("isUnauthorizedError returns true for 401", () => {
        expect(
            isUnauthorizedError({ status: 401, title: "", detail: "", originalError: null }),
        ).toBe(true);
        expect(
            isUnauthorizedError({ status: 403, title: "", detail: "", originalError: null }),
        ).toBe(false);
    });

    it("isForbiddenError returns true for 403", () => {
        expect(isForbiddenError({ status: 403, title: "", detail: "", originalError: null })).toBe(
            true,
        );
        expect(isForbiddenError({ status: 401, title: "", detail: "", originalError: null })).toBe(
            false,
        );
    });

    it("isNotFoundError returns true for 404", () => {
        expect(isNotFoundError({ status: 404, title: "", detail: "", originalError: null })).toBe(
            true,
        );
        expect(isNotFoundError({ status: 500, title: "", detail: "", originalError: null })).toBe(
            false,
        );
    });

    it("isValidationError returns true for 400 with field errors", () => {
        expect(
            isValidationError({
                status: 400,
                title: "",
                detail: "",
                fieldErrors: { field: "error" },
                originalError: null,
            }),
        ).toBe(true);
        expect(
            isValidationError({
                status: 400,
                title: "",
                detail: "",
                originalError: null,
            }),
        ).toBe(false);
        expect(
            isValidationError({
                status: 422,
                title: "",
                detail: "",
                fieldErrors: { field: "error" },
                originalError: null,
            }),
        ).toBe(false);
    });
});
