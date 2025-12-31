package com.example.demo.greeting.exception;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.demo.common.exception.ProblemDetailFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Exception handler for greeting module-specific exceptions.
 *
 * <p>This handler has precedence over {@code GlobalExceptionHandler} for exceptions
 * thrown from greeting module controllers due to Spring's {@code @RestControllerAdvice}
 * ordering and specificity rules (basePackages restriction makes it more specific).</p>
 *
 * <p>This allows the greeting module to:</p>
 * <ul>
 *   <li>Define domain-specific exception handling logic</li>
 *   <li>Customize error responses for greeting operations</li>
 *   <li>Add greeting-specific logging or monitoring</li>
 *   <li>Maintain full control over error presentation</li>
 * </ul>
 *
 * <p>All responses follow RFC 7807 Problem Details standard via {@link ProblemDetailFactory}.</p>
 *
 * @since 1.1.0
 */
@RestControllerAdvice(basePackages = "com.example.demo.greeting.controller")
@RequiredArgsConstructor
@Slf4j
public class GreetingExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    /**
     * Handles GreetingNotFoundException (HTTP 404).
     *
     * <p>This handler is invoked when a greeting cannot be found by ID or reference.
     * It creates a module-specific problem detail with greeting-specific properties.</p>
     *
     * @param ex the greeting not found exception
     * @param request the HTTP request that triggered the exception
     * @return RFC 7807 Problem Detail response with HTTP 404 status
     */
    @ExceptionHandler(GreetingNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleGreetingNotFound(
            GreetingNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Greeting not found: {}", ex.getMessage());

        ProblemDetail problemDetail = problemDetailFactory.createFromDomainException(
                ex,
                request.getRequestURI()
        );

        return ResponseEntity.status(ex.getHttpStatus()).body(problemDetail);
    }
}
