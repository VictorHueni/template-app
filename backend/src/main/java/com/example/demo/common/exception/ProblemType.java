package com.example.demo.common.exception;

/**
 * Constants for RFC 7807 Problem Details type URIs.
 * These URIs should be documented in the OpenAPI specification.
 *
 * <p>Framework and infrastructure problem types are defined here.
 * Modules should define their own domain-specific types using
 * {@link #buildModuleProblemType(String, String)}.</p>
 *
 * <p>Type URIs help clients identify the specific error type programmatically.</p>
 *
 * @since 1.0.0
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

    /**
     * Builds a module-specific problem type URI.
     *
     * <p>This helper method creates consistent problem type URIs for module-specific exceptions.
     * Modules should use this method to define their own problem types rather than adding
     * constants to this class.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * {@code
     * // In GreetingNotFoundException:
     * public String getProblemTypeUri() {
     *     return ProblemType.buildModuleProblemType("greeting", "not-found");
     * }
     * // Result: "https://api.example.com/problems/greeting/not-found"
     * }
     * </pre>
     *
     * @param module the module name (e.g., "greeting", "user", "audit")
     * @param problemType the problem type within the module (e.g., "not-found", "conflict", "invalid-state")
     * @return the complete problem type URI
     * @since 1.1.0
     */
    public static String buildModuleProblemType(String module, String problemType) {
        return BASE_URI + "/" + module + "/" + problemType;
    }
}

