package com.example.demo.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BusinessValidationException")
class BusinessValidationExceptionTest {

    @Nested
    @DisplayName("constructor with message only")
    class ConstructorWithMessage {

        @Test
        @DisplayName("should create exception with message")
        void shouldCreateExceptionWithMessage() {
            // Arrange
            String message = "Validation failed";

            // Act
            BusinessValidationException exception = new BusinessValidationException(message);

            // Assert
            assertThat(exception).isInstanceOf(RuntimeException.class);
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getValidationErrors()).isEmpty();
        }

        @Test
        @DisplayName("should have empty validation errors map")
        void shouldHaveEmptyValidationErrorsMap() {
            // Arrange & Act
            BusinessValidationException exception = new BusinessValidationException("Test");

            // Assert
            assertThat(exception.getValidationErrors()).isNotNull();
            assertThat(exception.getValidationErrors()).isEmpty();
        }
    }

    @Nested
    @DisplayName("constructor with message and errors map")
    class ConstructorWithMessageAndErrors {

        @Test
        @DisplayName("should create exception with message and errors map")
        void shouldCreateExceptionWithMessageAndErrorsMap() {
            // Arrange
            String message = "Multiple validation errors";
            Map<String, String> errors = new HashMap<>();
            errors.put("email", "Invalid email format");
            errors.put("age", "Must be at least 18");

            // Act
            BusinessValidationException exception = new BusinessValidationException(message, errors);

            // Assert
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getValidationErrors()).hasSize(2);
            assertThat(exception.getValidationErrors()).containsEntry("email", "Invalid email format");
            assertThat(exception.getValidationErrors()).containsEntry("age", "Must be at least 18");
        }

        @Test
        @DisplayName("should handle null errors map")
        void shouldHandleNullErrorsMap() {
            // Arrange & Act
            BusinessValidationException exception = new BusinessValidationException("Test", (Map<String, String>) null);

            // Assert
            assertThat(exception.getMessage()).isEqualTo("Test");
            assertThat(exception.getValidationErrors()).isEmpty();
        }

        @Test
        @DisplayName("should handle empty errors map")
        void shouldHandleEmptyErrorsMap() {
            // Arrange
            Map<String, String> emptyErrors = new HashMap<>();

            // Act
            BusinessValidationException exception = new BusinessValidationException("Test", emptyErrors);

            // Assert
            assertThat(exception.getValidationErrors()).isEmpty();
        }

        @Test
        @DisplayName("should copy errors map to avoid external modification")
        void shouldCopyErrorsMapToAvoidExternalModification() {
            // Arrange
            Map<String, String> originalErrors = new HashMap<>();
            originalErrors.put("field1", "error1");
            BusinessValidationException exception = new BusinessValidationException("Test", originalErrors);

            // Act - modify original map after exception creation
            originalErrors.put("field2", "error2");

            // Assert - exception should not be affected by external modification
            assertThat(exception.getValidationErrors()).hasSize(1);
            assertThat(exception.getValidationErrors()).containsKey("field1");
            assertThat(exception.getValidationErrors()).doesNotContainKey("field2");
        }
    }

    @Nested
    @DisplayName("constructor with single field and error")
    class ConstructorWithFieldAndError {

        @Test
        @DisplayName("should create exception with field and error")
        void shouldCreateExceptionWithFieldAndError() {
            // Arrange
            String field = "username";
            String error = "Username already exists";

            // Act
            BusinessValidationException exception = new BusinessValidationException(field, error);

            // Assert
            assertThat(exception.getMessage()).isEqualTo("Validation failed");
            assertThat(exception.getValidationErrors()).hasSize(1);
            assertThat(exception.getValidationErrors()).containsEntry(field, error);
        }

        @Test
        @DisplayName("should have default message 'Validation failed'")
        void shouldHaveDefaultMessage() {
            // Arrange & Act
            BusinessValidationException exception = new BusinessValidationException("email", "Invalid");

            // Assert
            assertThat(exception.getMessage()).isEqualTo("Validation failed");
        }

        @Test
        @DisplayName("should store single field error correctly")
        void shouldStoreSingleFieldErrorCorrectly() {
            // Arrange & Act
            BusinessValidationException exception = new BusinessValidationException("password", "Too weak");

            // Assert
            Map<String, String> errors = exception.getValidationErrors();
            assertThat(errors).hasSize(1);
            assertThat(errors.get("password")).isEqualTo("Too weak");
        }
    }

    @Nested
    @DisplayName("getValidationErrors")
    class GetValidationErrors {

        @Test
        @DisplayName("should return validation errors map")
        void shouldReturnValidationErrorsMap() {
            // Arrange
            Map<String, String> errors = new HashMap<>();
            errors.put("field1", "error1");
            errors.put("field2", "error2");
            BusinessValidationException exception = new BusinessValidationException("Test", errors);

            // Act
            Map<String, String> actualErrors = exception.getValidationErrors();

            // Assert
            assertThat(actualErrors).hasSize(2);
            assertThat(actualErrors).containsEntry("field1", "error1");
            assertThat(actualErrors).containsEntry("field2", "error2");
        }

        @Test
        @DisplayName("should return empty map when no errors")
        void shouldReturnEmptyMapWhenNoErrors() {
            // Arrange
            BusinessValidationException exception = new BusinessValidationException("Test");

            // Act
            Map<String, String> actualErrors = exception.getValidationErrors();

            // Assert
            assertThat(actualErrors).isNotNull();
            assertThat(actualErrors).isEmpty();
        }

        @Test
        @DisplayName("should return map containing all validation errors")
        void shouldReturnMapContainingAllValidationErrors() {
            // Arrange
            Map<String, String> errors = new HashMap<>();
            errors.put("name", "Name is required");
            errors.put("email", "Email format invalid");
            errors.put("phone", "Phone number too short");
            BusinessValidationException exception = new BusinessValidationException("Multiple errors", errors);

            // Act
            Map<String, String> actualErrors = exception.getValidationErrors();

            // Assert
            assertThat(actualErrors).hasSize(3);
            assertThat(actualErrors).containsAllEntriesOf(errors);
        }
    }

    @Nested
    @DisplayName("inheritance")
    class Inheritance {

        @Test
        @DisplayName("should extend RuntimeException")
        void shouldExtendRuntimeException() {
            // Arrange
            BusinessValidationException exception = new BusinessValidationException("Test");

            // Assert
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("should be throwable")
        void shouldBeThrowable() {
            // Arrange
            BusinessValidationException exception = new BusinessValidationException("Test");

            // Assert
            assertThat(exception).isInstanceOf(Throwable.class);
        }
    }

    @Nested
    @DisplayName("usage scenarios")
    class UsageScenarios {

        @Test
        @DisplayName("should work in throw statements")
        void shouldWorkInThrowStatements() {
            // Arrange
            String field = "email";
            String error = "Required field";

            // Act & Assert
            assertThatThrownBy(() -> {
                throw new BusinessValidationException(field, error);
            })
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessage("Validation failed");
        }

        @Test
        @DisplayName("should preserve all errors when thrown and caught")
        void shouldPreserveAllErrorsWhenThrownAndCaught() {
            // Arrange
            Map<String, String> errors = new HashMap<>();
            errors.put("field1", "error1");
            errors.put("field2", "error2");

            // Act & Assert
            try {
                throw new BusinessValidationException("Validation failed", errors);
            } catch (BusinessValidationException e) {
                assertThat(e.getValidationErrors()).hasSize(2);
                assertThat(e.getValidationErrors()).containsAllEntriesOf(errors);
            }
        }
    }
}
