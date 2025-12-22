package com.example.demo.common.exception.handler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.example.demo.common.exception.BusinessValidationException;
import com.example.demo.common.exception.ConflictException;
import com.example.demo.common.exception.ProblemType;
import com.example.demo.common.exception.ResourceNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        webRequest = new ServletWebRequest(request);
    }

    @Nested
    @DisplayName("handleResourceNotFound")
    class HandleResourceNotFound {

        @Test
        @DisplayName("should return 404 with problem detail")
        void shouldReturn404WithProblemDetail() {
            // Arrange
            var exception = new ResourceNotFoundException("User", 123L);

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleResourceNotFound(exception, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(404);
            assertThat(response.getBody().getTitle()).isEqualTo("Resource Not Found");
            assertThat(response.getBody().getDetail()).contains("User", "123");
            assertThat(response.getBody().getType().toString()).isEqualTo(ProblemType.RESOURCE_NOT_FOUND);
            assertThat(response.getBody().getInstance().toString()).isEqualTo("/api/v1/test");
        }

        @Test
        @DisplayName("should include trace ID")
        void shouldIncludeTraceId() {
            // Arrange
            var exception = new ResourceNotFoundException("User", 123L);

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleResourceNotFound(exception, request);

            // Assert
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getProperties()).containsKey("traceId");
            assertThat(response.getBody().getProperties().get("traceId")).isInstanceOf(String.class);
        }

        @Test
        @DisplayName("should include timestamp")
        void shouldIncludeTimestamp() {
            // Arrange
            var exception = new ResourceNotFoundException("User", 123L);

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleResourceNotFound(exception, request);

            // Assert
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getProperties()).containsKey("timestamp");
        }

        @Test
        @DisplayName("should include resource type and ID when available")
        void shouldIncludeResourceTypeAndId() {
            // Arrange
            var exception = new ResourceNotFoundException("Product", 456L);

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleResourceNotFound(exception, request);

            // Assert
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getProperties()).containsKey("resourceType");
            assertThat(response.getBody().getProperties()).containsKey("resourceId");
            assertThat(response.getBody().getProperties().get("resourceType")).isEqualTo("Product");
            assertThat(response.getBody().getProperties().get("resourceId")).isEqualTo(456L);
        }

        @Test
        @DisplayName("should handle simple message constructor")
        void shouldHandleSimpleMessageConstructor() {
            // Arrange
            var exception = new ResourceNotFoundException("Custom not found message");

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleResourceNotFound(exception, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getDetail()).isEqualTo("Custom not found message");
        }
    }

    @Nested
    @DisplayName("handleBusinessValidation")
    class HandleBusinessValidation {

        @Test
        @DisplayName("should return 400 with validation errors")
        void shouldReturn400WithValidationErrors() {
            // Arrange
            Map<String, String> errors = new HashMap<>();
            errors.put("field1", "Error 1");
            errors.put("field2", "Error 2");
            var exception = new BusinessValidationException("Validation failed", errors);

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleBusinessValidation(exception, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getTitle()).isEqualTo("Validation Error");
            assertThat(response.getBody().getDetail()).isEqualTo("Validation failed");
            assertThat(response.getBody().getType().toString()).isEqualTo(ProblemType.VALIDATION_ERROR);
        }

        @Test
        @DisplayName("should include errors map")
        void shouldIncludeErrorsMap() {
            // Arrange
            Map<String, String> errors = new HashMap<>();
            errors.put("email", "Invalid email format");
            errors.put("age", "Must be at least 18");
            var exception = new BusinessValidationException("Validation failed", errors);

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleBusinessValidation(exception, request);

            // Assert
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getProperties()).containsKey("errors");
            @SuppressWarnings("unchecked")
            Map<String, String> responseErrors = (Map<String, String>) response.getBody().getProperties().get("errors");
            assertThat(responseErrors).containsEntry("email", "Invalid email format");
            assertThat(responseErrors).containsEntry("age", "Must be at least 18");
        }

        @Test
        @DisplayName("should handle empty errors map")
        void shouldHandleEmptyErrorsMap() {
            // Arrange
            var exception = new BusinessValidationException("Validation failed");

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleBusinessValidation(exception, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getDetail()).isEqualTo("Validation failed");
        }

        @Test
        @DisplayName("should handle single field error constructor")
        void shouldHandleSingleFieldErrorConstructor() {
            // Arrange
            var exception = new BusinessValidationException("username", "Username already exists");

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleBusinessValidation(exception, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            @SuppressWarnings("unchecked")
            Map<String, String> responseErrors = (Map<String, String>) response.getBody().getProperties().get("errors");
            assertThat(responseErrors).containsEntry("username", "Username already exists");
        }
    }

    @Nested
    @DisplayName("handleConflict")
    class HandleConflict {

        @Test
        @DisplayName("should return 409 with conflict details")
        void shouldReturn409WithConflictDetails() {
            // Arrange
            var exception = new ConflictException("Resource already exists");

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleConflict(exception, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(409);
            assertThat(response.getBody().getTitle()).isEqualTo("Conflict");
            assertThat(response.getBody().getDetail()).isEqualTo("Resource already exists");
            assertThat(response.getBody().getType().toString()).isEqualTo(ProblemType.CONFLICT);
        }

        @Test
        @DisplayName("should preserve original message")
        void shouldPreserveOriginalMessage() {
            // Arrange
            var exception = new ConflictException("Custom conflict message", "duplicate_key");

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleConflict(exception, request);

            // Assert
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getDetail()).isEqualTo("Custom conflict message");
        }

        @Test
        @DisplayName("should include conflict reason when provided")
        void shouldIncludeConflictReasonWhenProvided() {
            // Arrange
            var exception = new ConflictException("User already exists", "duplicate_username");

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleConflict(exception, request);

            // Assert
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getProperties()).containsKey("reason");
            assertThat(response.getBody().getProperties().get("reason")).isEqualTo("duplicate_username");
        }

        @Test
        @DisplayName("should handle null conflict reason")
        void shouldHandleNullConflictReason() {
            // Arrange
            var exception = new ConflictException("Resource conflict");

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleConflict(exception, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
        }
    }

    @Nested
    @DisplayName("handleMethodArgumentNotValid")
    class HandleMethodArgumentNotValid {

        @Test
        @DisplayName("should return 400 for invalid request body")
        void shouldReturn400ForInvalidRequestBody() {
            // Arrange
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError = new FieldError("user", "email", "must not be null");
            when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));
            when(bindingResult.getErrorCount()).thenReturn(1);
            var exception = new MethodArgumentNotValidException(null, bindingResult);

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleMethodArgumentNotValid(exception, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getTitle()).isEqualTo("Validation Error");
            assertThat(response.getBody().getDetail()).isEqualTo("Request validation failed");
            assertThat(response.getBody().getType().toString()).isEqualTo(ProblemType.VALIDATION_ERROR);
        }

        @Test
        @DisplayName("should include field errors")
        void shouldIncludeFieldErrors() {
            // Arrange
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError error1 = new FieldError("user", "email", "must be a valid email");
            FieldError error2 = new FieldError("user", "age", "must be at least 18");
            when(bindingResult.getAllErrors()).thenReturn(List.of(error1, error2));
            var exception = new MethodArgumentNotValidException(null, bindingResult);

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleMethodArgumentNotValid(exception, request);

            // Assert
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getProperties()).containsKey("errors");
            @SuppressWarnings("unchecked")
            Map<String, String> errors = (Map<String, String>) response.getBody().getProperties().get("errors");
            assertThat(errors).containsEntry("email", "must be a valid email");
            assertThat(errors).containsEntry("age", "must be at least 18");
        }

        @Test
        @DisplayName("should include global errors with object name")
        void shouldIncludeGlobalErrors() {
            // Arrange
            BindingResult bindingResult = mock(BindingResult.class);
            ObjectError globalError = new ObjectError("user", "passwords do not match");
            when(bindingResult.getAllErrors()).thenReturn(List.of(globalError));
            var exception = new MethodArgumentNotValidException(null, bindingResult);

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleMethodArgumentNotValid(exception, request);

            // Assert
            assertThat(response.getBody()).isNotNull();
            @SuppressWarnings("unchecked")
            Map<String, String> errors = (Map<String, String>) response.getBody().getProperties().get("errors");
            assertThat(errors).containsEntry("user", "passwords do not match");
        }
    }

    @Nested
    @DisplayName("handleConstraintViolation")
    class HandleConstraintViolation {

        @Test
        @DisplayName("should return 400 for constraint violations")
        void shouldReturn400ForConstraintViolations() {
            // Arrange
            Set<ConstraintViolation<?>> violations = new HashSet<>();
            ConstraintViolation<?> violation = createMockViolation("email", "must not be blank");
            violations.add(violation);
            var exception = new ConstraintViolationException(violations);

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleConstraintViolation(exception, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getTitle()).isEqualTo("Validation Error");
            assertThat(response.getBody().getDetail()).isEqualTo("Constraint validation failed");
            assertThat(response.getBody().getType().toString()).isEqualTo(ProblemType.VALIDATION_ERROR);
        }

        @Test
        @DisplayName("should map violations to errors")
        void shouldMapViolationsToErrors() {
            // Arrange
            Set<ConstraintViolation<?>> violations = new HashSet<>();
            violations.add(createMockViolation("name", "size must be between 1 and 100"));
            violations.add(createMockViolation("age", "must be greater than or equal to 0"));
            var exception = new ConstraintViolationException(violations);

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleConstraintViolation(exception, request);

            // Assert
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getProperties()).containsKey("errors");
            @SuppressWarnings("unchecked")
            Map<String, String> errors = (Map<String, String>) response.getBody().getProperties().get("errors");
            assertThat(errors).containsEntry("name", "size must be between 1 and 100");
            assertThat(errors).containsEntry("age", "must be greater than or equal to 0");
        }

        private ConstraintViolation<?> createMockViolation(String propertyPath, String message) {
            @SuppressWarnings("unchecked")
            ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
            Path path = mock(Path.class);
            when(path.toString()).thenReturn(propertyPath);
            when(violation.getPropertyPath()).thenReturn(path);
            when(violation.getMessage()).thenReturn(message);
            return violation;
        }
    }

    @Nested
    @DisplayName("handleIllegalArgument")
    class HandleIllegalArgument {

        @Test
        @DisplayName("should return 400 with error details")
        void shouldReturn400WithErrorDetails() {
            // Arrange
            var exception = new IllegalArgumentException("Invalid parameter value");

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleIllegalArgument(exception, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
            assertThat(response.getBody().getDetail()).isEqualTo("Invalid parameter value");
            assertThat(response.getBody().getType().toString()).isEqualTo(ProblemType.BAD_REQUEST);
            assertThat(response.getBody().getInstance().toString()).isEqualTo("/api/v1/test");
        }

        @Test
        @DisplayName("should include trace ID and timestamp")
        void shouldIncludeTraceIdAndTimestamp() {
            // Arrange
            var exception = new IllegalArgumentException("Bad argument");

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleIllegalArgument(exception, request);

            // Assert
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getProperties()).containsKey("traceId");
            assertThat(response.getBody().getProperties()).containsKey("timestamp");
        }
    }

    @Nested
    @DisplayName("handleTypeMismatch")
    class HandleTypeMismatch {

        @Test
        @DisplayName("should return 400 for type mismatch")
        void shouldReturn400ForTypeMismatch() {
            // Arrange
            var exception = new MethodArgumentTypeMismatchException(
                    "abc", Long.class, "id", null, null);

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleTypeMismatch(exception, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
            assertThat(response.getBody().getType().toString()).isEqualTo(ProblemType.BAD_REQUEST);
        }

        @Test
        @DisplayName("should describe expected type")
        void shouldDescribeExpectedType() {
            // Arrange
            var exception = new MethodArgumentTypeMismatchException(
                    "invalid", Integer.class, "count", null, null);

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleTypeMismatch(exception, request);

            // Assert
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getDetail()).contains("count", "Integer");
            assertThat(response.getBody().getProperties()).containsKey("parameter");
            assertThat(response.getBody().getProperties().get("parameter")).isEqualTo("count");
            assertThat(response.getBody().getProperties()).containsKey("rejectedValue");
            assertThat(response.getBody().getProperties().get("rejectedValue")).isEqualTo("invalid");
        }

        @Test
        @DisplayName("should handle null required type")
        void shouldHandleNullRequiredType() {
            // Arrange
            var exception = new MethodArgumentTypeMismatchException(
                    "value", null, "param", null, null);

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleTypeMismatch(exception, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getDetail()).contains("param", "unknown");
        }
    }

    @Nested
    @DisplayName("handleNoResourceFound")
    class HandleNoResourceFound {

        @Test
        @DisplayName("should return 404 for unmapped URLs")
        void shouldReturn404ForUnmappedUrls() {
            // Arrange
            var exception = new NoResourceFoundException(HttpMethod.GET, null, "/static/missing.js");

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleNoResourceFound(exception, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(404);
            assertThat(response.getBody().getTitle()).isEqualTo("Resource Not Found");
            assertThat(response.getBody().getDetail()).isEqualTo("The requested resource was not found");
            assertThat(response.getBody().getType().toString()).isEqualTo(ProblemType.RESOURCE_NOT_FOUND);
        }

        @Test
        @DisplayName("should include trace ID and timestamp")
        void shouldIncludeTraceIdAndTimestamp() {
            // Arrange
            var exception = new NoResourceFoundException(HttpMethod.GET, null, "/favicon.ico");

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleNoResourceFound(exception, request);

            // Assert
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getProperties()).containsKey("traceId");
            assertThat(response.getBody().getProperties()).containsKey("timestamp");
        }
    }

    @Nested
    @DisplayName("handleAuthentication")
    class HandleAuthentication {

        @Test
        @DisplayName("should return 401 for authentication failure")
        void shouldReturn401ForAuthenticationFailure() {
            // Arrange
            var exception = new AuthenticationException("Bad credentials") {};

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleAuthentication(exception, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(401);
            assertThat(response.getBody().getTitle()).isEqualTo("Unauthorized");
            assertThat(response.getBody().getDetail()).isEqualTo("Authentication is required to access this resource");
            assertThat(response.getBody().getType().toString()).isEqualTo(ProblemType.UNAUTHORIZED);
        }

        @Test
        @DisplayName("should include trace ID and timestamp")
        void shouldIncludeTraceIdAndTimestamp() {
            // Arrange
            var exception = new AuthenticationException("Invalid token") {};

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleAuthentication(exception, request);

            // Assert
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getProperties()).containsKey("traceId");
            assertThat(response.getBody().getProperties()).containsKey("timestamp");
        }
    }

    @Nested
    @DisplayName("handleAccessDenied")
    class HandleAccessDenied {

        @Test
        @DisplayName("should return 403 for access denied")
        void shouldReturn403ForAccessDenied() {
            // Arrange
            var exception = new AccessDeniedException("Insufficient permissions");

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleAccessDenied(exception, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(403);
            assertThat(response.getBody().getTitle()).isEqualTo("Forbidden");
            assertThat(response.getBody().getDetail()).isEqualTo("You do not have permission to access this resource");
            assertThat(response.getBody().getType().toString()).isEqualTo(ProblemType.FORBIDDEN);
        }

        @Test
        @DisplayName("should include trace ID and timestamp")
        void shouldIncludeTraceIdAndTimestamp() {
            // Arrange
            var exception = new AccessDeniedException("Access denied");

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleAccessDenied(exception, request);

            // Assert
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getProperties()).containsKey("traceId");
            assertThat(response.getBody().getProperties()).containsKey("timestamp");
        }
    }

    @Nested
    @DisplayName("handleGenericException")
    class HandleGenericException {

        @Test
        @DisplayName("should return 500 for unhandled exceptions")
        void shouldReturn500ForUnhandledExceptions() {
            // Arrange
            var exception = new RuntimeException("Unexpected error");

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleGenericException(exception, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(500);
            assertThat(response.getBody().getTitle()).isEqualTo("Internal Server Error");
            assertThat(response.getBody().getType().toString()).isEqualTo(ProblemType.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("should not expose internal details")
        void shouldNotExposeInternalDetails() {
            // Arrange
            var exception = new NullPointerException("Database connection failed");

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleGenericException(exception, request);

            // Assert
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getDetail()).isEqualTo("An unexpected error occurred. Please contact support with the trace ID.");
            assertThat(response.getBody().getDetail()).doesNotContain("NullPointerException");
            assertThat(response.getBody().getDetail()).doesNotContain("Database connection failed");
        }

        @Test
        @DisplayName("should include trace ID for debugging")
        void shouldIncludeTraceIdForDebugging() {
            // Arrange
            var exception = new RuntimeException("Something went wrong");

            // Act
            ResponseEntity<ProblemDetail> response = handler.handleGenericException(exception, request);

            // Assert
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getProperties()).containsKey("traceId");
            assertThat(response.getBody().getProperties()).containsKey("timestamp");
        }
    }
}
