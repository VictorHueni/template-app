package com.example.demo.common.exception;

/**
 * Constants for RFC 7807 Problem Details type URIs.
 * These URIs should be documented in the OpenAPI specification.
 *
 * Type URIs help clients identify the specific error type programmatically.
 */
public final class ProblemType {

    private ProblemType() {
        // Utility class
    }

    /**
     * Base URI for all problem types in this API
     */
    private static final String BASE_URI = "https://api.example.com/problems";

    /**
     * Resource not found (HTTP 404)
     */
    public static final String RESOURCE_NOT_FOUND = BASE_URI + "/resource-not-found";

    /**
     * Business validation failed (HTTP 400)
     */
    public static final String VALIDATION_ERROR = BASE_URI + "/validation-error";

    /**
     * Request validation failed (HTTP 400)
     */
    public static final String BAD_REQUEST = BASE_URI + "/bad-request";

    /**
     * Resource conflict (HTTP 409)
     */
    public static final String CONFLICT = BASE_URI + "/conflict";

    /**
     * Authentication required (HTTP 401)
     */
    public static final String UNAUTHORIZED = BASE_URI + "/unauthorized";

    /**
     * Insufficient permissions (HTTP 403)
     */
    public static final String FORBIDDEN = BASE_URI + "/forbidden";

    /**
     * Method not allowed (HTTP 405)
     */
    public static final String METHOD_NOT_ALLOWED = BASE_URI + "/method-not-allowed";

    /**
     * Unsupported media type (HTTP 415)
     */
    public static final String UNSUPPORTED_MEDIA_TYPE = BASE_URI + "/unsupported-media-type";

    /**
     * Internal server error (HTTP 500)
     */
    public static final String INTERNAL_SERVER_ERROR = BASE_URI + "/internal-server-error";
}

