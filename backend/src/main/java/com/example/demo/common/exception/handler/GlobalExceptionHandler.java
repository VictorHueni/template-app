package com.example.demo.common.exception.handler;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.example.demo.common.exception.BusinessValidationException;
import com.example.demo.common.exception.ConflictException;
import com.example.demo.common.exception.DomainException;
import com.example.demo.common.exception.ProblemDetailFactory;
import com.example.demo.common.exception.ProblemType;
import com.example.demo.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for the application.
 * Converts exceptions to RFC 7807 Problem Details responses.
 *
 * <p><strong>Responsibilities:</strong></p>
 * <ul>
 *   <li>Framework exceptions (Spring, Bean Validation, Security)</li>
 *   <li>Infrastructure exceptions (HTTP, method not allowed, media type)</li>
 *   <li>Fallback handler for unhandled module-specific {@link DomainException}s</li>
 *   <li>Common domain exceptions (Pattern 1): {@link ResourceNotFoundException},
 *       {@link BusinessValidationException}, {@link ConflictException}</li>
 * </ul>
 *
 * <p><strong>Exception Handling Patterns:</strong></p>
 * <p>This application supports two complementary exception handling approaches:</p>
 * <ul>
 *   <li><strong>Pattern 1 (Simple):</strong> Use common exceptions (ResourceNotFoundException, etc.)
 *       handled by this global handler for straightforward CRUD operations</li>
 *   <li><strong>Pattern 2 (Domain-specific):</strong> Create module-specific exceptions extending
 *       {@link DomainException} with dedicated {@code @RestControllerAdvice} handlers
 *       (e.g., {@code GreetingExceptionHandler}). Module handlers take precedence due to
 *       {@code basePackages} scoping.</li>
 * </ul>
 *
 * <p>Both patterns are valid and respect Spring Modulith principles. Choose based on your module's needs.
 * See {@code common.exception} package documentation for detailed guidance.</p>
 *
 * <p><strong>RFC 7807 Response Format:</strong></p>
 * <p>All responses include:</p>
 * <ul>
 *   <li><code>type</code>: URI identifying the problem type</li>
 *   <li><code>title</code>: Short, human-readable summary</li>
 *   <li><code>status</code>: HTTP status code</li>
 *   <li><code>detail</code>: Human-readable explanation</li>
 *   <li><code>instance</code>: URI identifying the specific occurrence</li>
 *   <li><code>timestamp</code>: When the error occurred (ISO-8601)</li>
 *   <li><code>traceId</code>: Unique identifier for debugging</li>
 * </ul>
 *
 * @since 1.0.0
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    /**
     * Handles any DomainException not caught by module-specific handlers.
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomainException(
            DomainException ex,
            HttpServletRequest request) {

        log.warn("Domain exception [{}]: {}", ex.getClass().getSimpleName(), ex.getMessage());

        ProblemDetail problemDetail = problemDetailFactory.createFromDomainException(
                ex,
                request.getRequestURI()
        );

        return ResponseEntity.status(ex.getHttpStatus()).body(problemDetail);
    }

    /**
     * Handles ResourceNotFoundException (HTTP 404)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Resource not found [traceId={}]: {}", traceId, ex.getMessage());

        ProblemDetail problemDetail = problemDetailFactory.createGenericProblem(
                HttpStatus.NOT_FOUND,
                ProblemType.RESOURCE_NOT_FOUND,
                "Resource Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
        problemDetail.setProperty("traceId", traceId);

        if (ex.getResourceType() != null) {
            problemDetail.setProperty("resourceType", ex.getResourceType());
            problemDetail.setProperty("resourceId", ex.getResourceId());
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    /**
     * Handles BusinessValidationException (HTTP 400)
     */
    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ProblemDetail> handleBusinessValidation(
            BusinessValidationException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Business validation failed [traceId={}]: {}", traceId, ex.getMessage());

        ProblemDetail problemDetail = problemDetailFactory.createGenericProblem(
                HttpStatus.BAD_REQUEST,
                ProblemType.VALIDATION_ERROR,
                "Validation Error",
                ex.getMessage(),
                request.getRequestURI()
        );
        problemDetail.setProperty("traceId", traceId);

        if (!ex.getValidationErrors().isEmpty()) {
            problemDetail.setProperty("errors", ex.getValidationErrors());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /**
     * Handles ConflictException (HTTP 409)
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(
            ConflictException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Conflict [traceId={}]: {}", traceId, ex.getMessage());

        ProblemDetail problemDetail = problemDetailFactory.createGenericProblem(
                HttpStatus.CONFLICT,
                ProblemType.CONFLICT,
                "Conflict",
                ex.getMessage(),
                request.getRequestURI()
        );
        problemDetail.setProperty("traceId", traceId);

        if (ex.getConflictReason() != null) {
            problemDetail.setProperty("reason", ex.getConflictReason());
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    /**
     * Handles Bean Validation errors from @Valid annotations (HTTP 400)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Validation failed [traceId={}]: {} error(s)", traceId, ex.getErrorCount());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError
                    ? ((FieldError) error).getField()
                    : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ProblemDetail problemDetail = problemDetailFactory.createGenericProblem(
                HttpStatus.BAD_REQUEST,
                ProblemType.VALIDATION_ERROR,
                "Validation Error",
                "Request validation failed",
                request.getRequestURI()
        );
        problemDetail.setProperty("traceId", traceId);
        problemDetail.setProperty("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /**
     * Handles constraint violations (HTTP 400)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Constraint violation [traceId={}]: {}", traceId, ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            errors.put(propertyPath, violation.getMessage());
        }

        ProblemDetail problemDetail = problemDetailFactory.createGenericProblem(
                HttpStatus.BAD_REQUEST,
                ProblemType.VALIDATION_ERROR,
                "Validation Error",
                "Constraint validation failed",
                request.getRequestURI()
        );
        problemDetail.setProperty("traceId", traceId);
        problemDetail.setProperty("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /**
     * Handles IllegalArgumentException (HTTP 400)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Illegal argument [traceId={}]: {}", traceId, ex.getMessage());

        ProblemDetail problemDetail = problemDetailFactory.createGenericProblem(
                HttpStatus.BAD_REQUEST,
                ProblemType.BAD_REQUEST,
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI()
        );
        problemDetail.setProperty("traceId", traceId);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /**
     * Handles type mismatch errors (HTTP 400)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Type mismatch [traceId={}]: {}", traceId, ex.getMessage());

        String detail = String.format(
                "Parameter '%s' should be of type '%s'",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );

        ProblemDetail problemDetail = problemDetailFactory.createGenericProblem(
                HttpStatus.BAD_REQUEST,
                ProblemType.BAD_REQUEST,
                "Bad Request",
                detail,
                request.getRequestURI()
        );
        problemDetail.setProperty("traceId", traceId);
        problemDetail.setProperty("parameter", ex.getName());
        problemDetail.setProperty("rejectedValue", ex.getValue());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /**
     * Handles NoResourceFoundException - Spring's built-in 404 for static resources (HTTP 404)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("No resource found [traceId={}]: {}", traceId, request.getRequestURI());

        ProblemDetail problemDetail = problemDetailFactory.createGenericProblem(
                HttpStatus.NOT_FOUND,
                ProblemType.RESOURCE_NOT_FOUND,
                "Resource Not Found",
                "The requested resource was not found",
                request.getRequestURI()
        );
        problemDetail.setProperty("traceId", traceId);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    /**
     * Handles Spring Security AuthenticationException (HTTP 401)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Authentication failed [traceId={}]: {}", traceId, ex.getMessage());

        ProblemDetail problemDetail = problemDetailFactory.createGenericProblem(
                HttpStatus.UNAUTHORIZED,
                ProblemType.UNAUTHORIZED,
                "Unauthorized",
                "Authentication is required to access this resource",
                request.getRequestURI()
        );
        problemDetail.setProperty("traceId", traceId);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

    /**
     * Handles Spring Security AccessDeniedException (HTTP 403)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Access denied [traceId={}]: {}", traceId, ex.getMessage());

        ProblemDetail problemDetail = problemDetailFactory.createGenericProblem(
                HttpStatus.FORBIDDEN,
                ProblemType.FORBIDDEN,
                "Forbidden",
                "You do not have permission to access this resource",
                request.getRequestURI()
        );
        problemDetail.setProperty("traceId", traceId);

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
    }

    /**
     * Catches all other exceptions (HTTP 500)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.error("Internal server error [traceId={}]: {}", traceId, ex.getMessage(), ex);

        ProblemDetail problemDetail = problemDetailFactory.createGenericProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ProblemType.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please contact support with the trace ID.",
                request.getRequestURI()
        );
        problemDetail.setProperty("traceId", traceId);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    /**
     * Handles HttpMessageNotReadableException - malformed JSON or invalid request body (HTTP 400)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Message not readable [traceId={}]: {}", traceId, ex.getMessage());

        ProblemDetail problemDetail = problemDetailFactory.createGenericProblem(
                HttpStatus.BAD_REQUEST,
                ProblemType.BAD_REQUEST,
                "Bad Request",
                "Invalid request content",
                request.getRequestURI()
        );
        problemDetail.setProperty("traceId", traceId);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /**
     * Handles MissingServletRequestParameterException (HTTP 400)
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Missing parameter [traceId={}]: {}", traceId, ex.getMessage());

        String detail = String.format("Required parameter '%s' of type '%s' is missing",
                ex.getParameterName(), ex.getParameterType());

        ProblemDetail problemDetail = problemDetailFactory.createGenericProblem(
                HttpStatus.BAD_REQUEST,
                ProblemType.BAD_REQUEST,
                "Bad Request",
                detail,
                request.getRequestURI()
        );
        problemDetail.setProperty("traceId", traceId);
        problemDetail.setProperty("parameter", ex.getParameterName());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /**
     * Handles HttpRequestMethodNotSupportedException (HTTP 405)
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Method not supported [traceId={}]: {}", traceId, ex.getMessage());

        ProblemDetail problemDetail = problemDetailFactory.createGenericProblem(
                HttpStatus.METHOD_NOT_ALLOWED,
                ProblemType.METHOD_NOT_ALLOWED,
                "Method Not Allowed",
                String.format("Method '%s' is not supported for this endpoint", ex.getMethod()),
                request.getRequestURI()
        );
        problemDetail.setProperty("traceId", traceId);

        if (ex.getSupportedMethods() != null) {
            problemDetail.setProperty("supportedMethods", ex.getSupportedMethods());
        }

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(problemDetail);
    }

    /**
     * Handles HttpMediaTypeNotSupportedException (HTTP 415)
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Media type not supported [traceId={}]: {}", traceId, ex.getMessage());

        ProblemDetail problemDetail = problemDetailFactory.createGenericProblem(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                ProblemType.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported Media Type",
                String.format("Content type '%s' is not supported", ex.getContentType()),
                request.getRequestURI()
        );
        problemDetail.setProperty("traceId", traceId);

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(problemDetail);
    }

    /**
     * Generates a unique trace ID for debugging
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }
}

