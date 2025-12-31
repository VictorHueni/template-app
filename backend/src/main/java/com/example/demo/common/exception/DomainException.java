package com.example.demo.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Base class for all domain-specific exceptions.
 *
 * <p>Modules extend this to create their own exception hierarchies while maintaining
 * consistent RFC 7807 Problem Details responses.</p>
 *
 * <p>Subclasses must implement:</p>
 * <ul>
 *   <li>{@link #getHttpStatus()} - The HTTP status code for this exception</li>
 *   <li>{@link #getProblemTypeUri()} - The RFC 7807 problem type URI</li>
 * </ul>
 *
 * <p>Subclasses may override:</p>
 * <ul>
 *   <li>{@link #getProblemTitle()} - Custom title (defaults to HTTP status reason phrase)</li>
 *   <li>{@link #enrichProblemDetail(ProblemDetail)} - Add domain-specific properties</li>
 * </ul>
 *
 * @since 1.1.0
 */
public abstract class DomainException extends RuntimeException {

    /**
     * Constructs a new domain exception with the specified detail message.
     *
     * @param message the detail message
     */
    protected DomainException(String message) {
        super(message);
    }

    /**
     * Constructs a new domain exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Returns the HTTP status code this exception maps to.
     *
     * @return the HTTP status code
     */
    public abstract HttpStatus getHttpStatus();

    /**
     * Returns the RFC 7807 problem type URI.
     *
     * <p>Example: {@code https://api.example.com/problems/greeting/not-found}</p>
     *
     * <p>Use {@link ProblemType#buildModuleProblemType(String, String)} for consistent URIs.</p>
     *
     * @return the problem type URI
     */
    public abstract String getProblemTypeUri();

    /**
     * Returns the title for RFC 7807 Problem Details.
     *
     * <p>Defaults to the HTTP status reason phrase. Subclasses can override
     * for more specific titles.</p>
     *
     * @return the problem title
     */
    public String getProblemTitle() {
        return getHttpStatus().getReasonPhrase();
    }

    /**
     * Enriches the ProblemDetail with exception-specific properties.
     *
     * <p>Override this method to add custom fields to the RFC 7807 response.
     * For example, add resource identifiers, validation errors, or other
     * domain-specific metadata.</p>
     *
     * <p>Example:</p>
     * <pre>
     * {@code
     * @Override
     * public void enrichProblemDetail(ProblemDetail problemDetail) {
     *     problemDetail.setProperty("resourceType", "Greeting");
     *     problemDetail.setProperty("greetingId", this.greetingId);
     * }
     * }
     * </pre>
     *
     * @param problemDetail the problem detail to enrich
     */
    public void enrichProblemDetail(ProblemDetail problemDetail) {
        // Default: no additional properties
        // Subclasses override to add domain-specific fields
    }
}
