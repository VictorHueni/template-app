package com.example.demo.common.exception.handler;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.example.demo.common.exception.ProblemType;
import com.example.demo.common.exception.ResourceNotFoundException;

/**
 * Global exception handler for the application.
 * Converts exceptions to RFC 7807 Problem Details responses.
 *
 * All responses include:
 * - type: URI identifying the problem type
 * - title: Short, human-readable summary
 * - status: HTTP status code
 * - detail: Human-readable explanation
 * - instance: URI identifying the specific occurrence
 * - timestamp: When the error occurred (ISO-8601)
 * - traceId: Unique identifier for debugging
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles ResourceNotFoundException (HTTP 404)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        LOGGER.warn("Resource not found [traceId={}]: {}", traceId, ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setType(URI.create(ProblemType.RESOURCE_NOT_FOUND));
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        enrichProblemDetail(problemDetail, traceId);

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
        LOGGER.warn("Business validation failed [traceId={}]: {}", traceId, ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setType(URI.create(ProblemType.VALIDATION_ERROR));
        problemDetail.setTitle("Validation Error");
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        enrichProblemDetail(problemDetail, traceId);

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
        LOGGER.warn("Conflict [traceId={}]: {}", traceId, ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problemDetail.setType(URI.create(ProblemType.CONFLICT));
        problemDetail.setTitle("Conflict");
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        enrichProblemDetail(problemDetail, traceId);

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
        LOGGER.warn("Validation failed [traceId={}]: {} error(s)", traceId, ex.getErrorCount());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError
                    ? ((FieldError) error).getField()
                    : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed"
        );
        problemDetail.setType(URI.create(ProblemType.VALIDATION_ERROR));
        problemDetail.setTitle("Validation Error");
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        enrichProblemDetail(problemDetail, traceId);
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
        LOGGER.warn("Constraint violation [traceId={}]: {}", traceId, ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            errors.put(propertyPath, violation.getMessage());
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Constraint validation failed"
        );
        problemDetail.setType(URI.create(ProblemType.VALIDATION_ERROR));
        problemDetail.setTitle("Validation Error");
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        enrichProblemDetail(problemDetail, traceId);
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
        LOGGER.warn("Illegal argument [traceId={}]: {}", traceId, ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setType(URI.create(ProblemType.BAD_REQUEST));
        problemDetail.setTitle("Bad Request");
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        enrichProblemDetail(problemDetail, traceId);

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
        LOGGER.warn("Type mismatch [traceId={}]: {}", traceId, ex.getMessage());

        String detail = String.format(
                "Parameter '%s' should be of type '%s'",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                detail
        );
        problemDetail.setType(URI.create(ProblemType.BAD_REQUEST));
        problemDetail.setTitle("Bad Request");
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        enrichProblemDetail(problemDetail, traceId);
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
        LOGGER.warn("No resource found [traceId={}]: {}", traceId, request.getRequestURI());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "The requested resource was not found"
        );
        problemDetail.setType(URI.create(ProblemType.RESOURCE_NOT_FOUND));
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        enrichProblemDetail(problemDetail, traceId);

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
        LOGGER.warn("Authentication failed [traceId={}]: {}", traceId, ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Authentication is required to access this resource"
        );
        problemDetail.setType(URI.create(ProblemType.UNAUTHORIZED));
        problemDetail.setTitle("Unauthorized");
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        enrichProblemDetail(problemDetail, traceId);

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
        LOGGER.warn("Access denied [traceId={}]: {}", traceId, ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource"
        );
        problemDetail.setType(URI.create(ProblemType.FORBIDDEN));
        problemDetail.setTitle("Forbidden");
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        enrichProblemDetail(problemDetail, traceId);

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
        LOGGER.error("Internal server error [traceId={}]: {}", traceId, ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support with the trace ID."
        );
        problemDetail.setType(URI.create(ProblemType.INTERNAL_SERVER_ERROR));
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        enrichProblemDetail(problemDetail, traceId);

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
        LOGGER.warn("Message not readable [traceId={}]: {}", traceId, ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid request content"
        );
        problemDetail.setType(URI.create(ProblemType.BAD_REQUEST));
        problemDetail.setTitle("Bad Request");
        enrichProblemDetail(problemDetail, traceId, request);

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
        LOGGER.warn("Missing parameter [traceId={}]: {}", traceId, ex.getMessage());

        String detail = String.format("Required parameter '%s' of type '%s' is missing",
                ex.getParameterName(), ex.getParameterType());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                detail
        );
        problemDetail.setType(URI.create(ProblemType.BAD_REQUEST));
        problemDetail.setTitle("Bad Request");
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        enrichProblemDetail(problemDetail, traceId);
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
        LOGGER.warn("Method not supported [traceId={}]: {}", traceId, ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.METHOD_NOT_ALLOWED,
                String.format("Method '%s' is not supported for this endpoint", ex.getMethod())
        );
        problemDetail.setType(URI.create(ProblemType.METHOD_NOT_ALLOWED));
        problemDetail.setTitle("Method Not Allowed");

        enrichProblemDetail(problemDetail, traceId, request);

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
        LOGGER.warn("Media type not supported [traceId={}]: {}", traceId, ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                String.format("Content type '%s' is not supported", ex.getContentType())
        );
        problemDetail.setType(URI.create(ProblemType.UNSUPPORTED_MEDIA_TYPE));
        problemDetail.setTitle("Unsupported Media Type");
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        enrichProblemDetail(problemDetail, traceId);

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(problemDetail);
    }

    /**
     * Adds common custom properties to all problem details
     */
    private void enrichProblemDetail(ProblemDetail problemDetail, String traceId) {
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", traceId);
    }

    private void enrichProblemDetail(ProblemDetail problemDetail, String traceId, HttpServletRequest request) {
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", traceId);
    }

    /**
     * Generates a unique trace ID for debugging
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }
}

