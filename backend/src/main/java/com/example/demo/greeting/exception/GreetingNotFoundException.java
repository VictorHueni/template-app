package com.example.demo.greeting.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import com.example.demo.common.exception.DomainException;
import com.example.demo.common.exception.ProblemType;

/**
 * Exception thrown when a requested greeting cannot be found.
 *
 * <p><strong>This is an example of Pattern 2: Module-Specific Exceptions.</strong></p>
 *
 * <p>This module-specific exception demonstrates how to extend {@link DomainException}
 * to provide domain-specific error context. It includes:</p>
 * <ul>
 *   <li>Custom properties: {@code greetingId}, {@code greetingReference}</li>
 *   <li>Module-specific problem type URI: {@code /problems/greeting/not-found}</li>
 *   <li>Domain-specific error title: "Greeting Not Found"</li>
 *   <li>Dedicated exception handler: {@link GreetingExceptionHandler}</li>
 * </ul>
 *
 * <p><strong>When to use this pattern:</strong></p>
 * <p>Use module-specific exceptions like this when your domain has:</p>
 * <ul>
 *   <li>Special error properties (like functional references)</li>
 *   <li>Domain-specific error scenarios</li>
 *   <li>Need for specialized error handling or logging</li>
 * </ul>
 *
 * <p><strong>Alternative:</strong> For simple not-found scenarios, you could also use
 * {@code ResourceNotFoundException("Greeting", id)} from the common package.
 * Both approaches are valid Spring Modulith patterns.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // By ID
 * throw new GreetingNotFoundException(123L);
 *
 * // By reference (greeting-specific feature)
 * throw new GreetingNotFoundException("GRE-2025-000042");
 * }</pre>
 *
 * @see com.example.demo.common.exception.DomainException
 * @see com.example.demo.greeting.exception.GreetingExceptionHandler
 * @see com.example.demo.common.exception.ResourceNotFoundException
 * @since 1.1.0
 */
public class GreetingNotFoundException extends DomainException {

    private final Long greetingId;
    private final String greetingReference;

    /**
     * Constructs a new GreetingNotFoundException with the specified greeting ID.
     *
     * @param greetingId the ID of the greeting that was not found
     */
    public GreetingNotFoundException(Long greetingId) {
        super(String.format("Greeting with id '%s' not found", greetingId));
        this.greetingId = greetingId;
        this.greetingReference = null;
    }

    /**
     * Constructs a new GreetingNotFoundException with the specified greeting reference.
     *
     * @param greetingReference the functional reference of the greeting that was not found
     */
    public GreetingNotFoundException(String greetingReference) {
        super(String.format("Greeting with reference '%s' not found", greetingReference));
        this.greetingId = null;
        this.greetingReference = greetingReference;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }

    @Override
    public String getProblemTypeUri() {
        return ProblemType.buildModuleProblemType("greeting", "not-found");
    }

    @Override
    public String getProblemTitle() {
        return "Greeting Not Found";
    }

    @Override
    public void enrichProblemDetail(ProblemDetail problemDetail) {
        problemDetail.setProperty("resourceType", "Greeting");

        if (greetingId != null) {
            problemDetail.setProperty("greetingId", greetingId);
        }

        if (greetingReference != null) {
            problemDetail.setProperty("greetingReference", greetingReference);
        }
    }

    /**
     * Returns the greeting ID that was not found, or null if constructed with a reference.
     *
     * @return the greeting ID or null
     */
    public Long getGreetingId() {
        return greetingId;
    }

    /**
     * Returns the greeting reference that was not found, or null if constructed with an ID.
     *
     * @return the greeting reference or null
     */
    public String getGreetingReference() {
        return greetingReference;
    }
}
