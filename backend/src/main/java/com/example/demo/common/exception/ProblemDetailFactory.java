package com.example.demo.common.exception;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/**
 * Factory for creating RFC 7807 Problem Details with consistent structure.
 *
 * <p>This utility ensures all exception handlers (module-specific and global)
 * create Problem Details responses with consistent metadata including timestamps
 * and trace IDs for debugging.</p>
 *
 * <p>Used by:</p>
 * <ul>
 *   <li>Module-specific exception handlers (e.g., GreetingExceptionHandler)</li>
 *   <li>GlobalExceptionHandler for framework exceptions</li>
 * </ul>
 *
 * @since 1.1.0
 */
@Component
public class ProblemDetailFactory {

    private final String problemBaseUri;

    public ProblemDetailFactory(@Value("${app.api.problem-base-uri:https://api.template.com/problems}") String problemBaseUri) {
        this.problemBaseUri = problemBaseUri;
    }

    /**
     * Creates a Problem Detail from a DomainException.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Creates a ProblemDetail with the exception's HTTP status and message</li>
     *   <li>Sets the type URI from the exception (resolved against base URI)</li>
     *   <li>Sets the title from the exception</li>
     *   <li>Sets the instance URI to the request path</li>
     *   <li>Adds standard properties (timestamp, traceId)</li>
     *   <li>Allows the exception to add domain-specific properties</li>
     * </ol>
     *
     * @param exception the domain exception
     * @param requestUri the request URI that triggered the exception
     * @return the RFC 7807 Problem Detail
     */
    public ProblemDetail createFromDomainException(
            DomainException exception,
            String requestUri) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                exception.getHttpStatus(),
                exception.getMessage()
        );

        problemDetail.setType(resolveTypeUri(exception.getProblemTypeUri()));
        problemDetail.setTitle(exception.getProblemTitle());
        problemDetail.setInstance(URI.create(requestUri));

        // Add standard metadata
        String traceId = UUID.randomUUID().toString();
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", traceId);

        // Allow exception to add domain-specific properties
        exception.enrichProblemDetail(problemDetail);

        return problemDetail;
    }

    /**
     * Creates a generic Problem Detail.
     *
     * <p>Used for framework exceptions or when a DomainException is not available.</p>
     *
     * @param status the HTTP status code
     * @param type the problem type slug or URI
     * @param title the problem title
     * @param detail the problem detail message
     * @param requestUri the request URI
     * @return the RFC 7807 Problem Detail
     */
    public ProblemDetail createGenericProblem(
            HttpStatus status,
            String type,
            String title,
            String detail,
            String requestUri) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                status,
                detail
        );

        problemDetail.setType(resolveTypeUri(type));
        problemDetail.setTitle(title);
        problemDetail.setInstance(URI.create(requestUri));

        // Add standard metadata
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", UUID.randomUUID().toString());

        return problemDetail;
    }

    /**
     * Resolves the problem type URI.
     * If the type is already an absolute URI, it returns it as is.
     * Otherwise, it appends it to the configured base URI.
     */
    public URI resolveTypeUri(String type) {
        if (type == null) {
            return URI.create("about:blank");
        }
        if (type.startsWith("http")) {
            return URI.create(type);
        }
        return URI.create(problemBaseUri + "/" + type);
    }

    /**
     * Creates a generic Problem Detail with status code.
     *
     * <p>Convenience overload that accepts int status code instead of HttpStatus.</p>
     *
     * @param statusCode the HTTP status code as int
     * @param type the problem type URI
     * @param title the problem title
     * @param detail the problem detail message
     * @param requestUri the request URI
     * @return the RFC 7807 Problem Detail
     */
    public ProblemDetail createGenericProblem(
            int statusCode,
            String type,
            String title,
            String detail,
            String requestUri) {

        return createGenericProblem(
                HttpStatus.valueOf(statusCode),
                type,
                title,
                detail,
                requestUri
        );
    }
}
