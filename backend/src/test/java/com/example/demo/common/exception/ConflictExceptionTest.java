package com.example.demo.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConflictException")
class ConflictExceptionTest {

    @Nested
    @DisplayName("constructor with message")
    class ConstructorWithMessage {

        @Test
        @DisplayName("should create exception with message")
        void shouldCreateExceptionWithMessage() {
            // Arrange
            String message = "Resource already exists";

            // Act
            ConflictException exception = new ConflictException(message);

            // Assert
            assertThat(exception).isInstanceOf(RuntimeException.class);
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getConflictReason()).isNull();
        }

        @Test
        @DisplayName("should handle null conflict reason")
        void shouldHandleNullConflictReason() {
            // Arrange & Act
            ConflictException exception = new ConflictException("Test message");

            // Assert
            assertThat(exception.getConflictReason()).isNull();
        }
    }

    @Nested
    @DisplayName("constructor with message and conflict reason")
    class ConstructorWithMessageAndConflictReason {

        @Test
        @DisplayName("should create exception with message and conflict reason")
        void shouldCreateExceptionWithMessageAndConflictReason() {
            // Arrange
            String message = "User already exists";
            String conflictReason = "duplicate_username";

            // Act
            ConflictException exception = new ConflictException(message, conflictReason);

            // Assert
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getConflictReason()).isEqualTo(conflictReason);
        }

        @Test
        @DisplayName("should preserve both message and reason")
        void shouldPreserveBothMessageAndReason() {
            // Arrange
            String message = "Email conflict";
            String reason = "email_already_registered";

            // Act
            ConflictException exception = new ConflictException(message, reason);

            // Assert
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getConflictReason()).isEqualTo(reason);
        }

        @Test
        @DisplayName("should allow null conflict reason in two-arg constructor")
        void shouldAllowNullConflictReason() {
            // Arrange & Act
            ConflictException exception = new ConflictException("Message", null);

            // Assert
            assertThat(exception.getMessage()).isEqualTo("Message");
            assertThat(exception.getConflictReason()).isNull();
        }
    }

    @Nested
    @DisplayName("getConflictReason")
    class GetConflictReason {

        @Test
        @DisplayName("should return conflict reason when provided")
        void shouldReturnConflictReasonWhenProvided() {
            // Arrange
            String reason = "version_mismatch";
            ConflictException exception = new ConflictException("Conflict", reason);

            // Act
            String actualReason = exception.getConflictReason();

            // Assert
            assertThat(actualReason).isEqualTo(reason);
        }

        @Test
        @DisplayName("should return null when conflict reason not provided")
        void shouldReturnNullWhenConflictReasonNotProvided() {
            // Arrange
            ConflictException exception = new ConflictException("Conflict");

            // Act
            String actualReason = exception.getConflictReason();

            // Assert
            assertThat(actualReason).isNull();
        }
    }

    @Nested
    @DisplayName("inheritance")
    class Inheritance {

        @Test
        @DisplayName("should extend RuntimeException")
        void shouldExtendRuntimeException() {
            // Arrange
            ConflictException exception = new ConflictException("Test");

            // Assert
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("should be throwable")
        void shouldBeThrowable() {
            // Arrange
            ConflictException exception = new ConflictException("Test");

            // Assert
            assertThat(exception).isInstanceOf(Throwable.class);
        }
    }
}
