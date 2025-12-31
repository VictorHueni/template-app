package com.example.demo.greeting.controller;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.demo.api.v1.model.GreetingResponse;
import com.example.demo.api.v1.model.PatchGreetingRequest;
import com.example.demo.api.v1.model.UpdateGreetingRequest;
import com.example.demo.greeting.exception.GreetingNotFoundException;
import com.example.demo.greeting.mapper.GreetingMapper;
import com.example.demo.greeting.mapper.GreetingMapperImpl;
import com.example.demo.greeting.model.Greeting;
import com.example.demo.greeting.service.GreetingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GreetingController.
 * Tests follow TDD approach - written before implementation.
 * 
 * API Contract (from openapi.yaml):
 * - GET /v1/greetings/{id} -> 200 (GreetingResponse) or 404 (ProblemDetail)
 * - PUT /v1/greetings/{id} -> 200 (GreetingResponse) or 400/404/409
 * - PATCH /v1/greetings/{id} -> 200 (GreetingResponse) or 400/404/409
 * - DELETE /v1/greetings/{id} -> 204 or 404
 */
class GreetingControllerTest {

    private GreetingService service;
    private GreetingMapper mapper;
    private GreetingController controller;

    @BeforeEach
    void setUp() {
        service = mock(GreetingService.class);
        mapper = new GreetingMapperImpl(); // Use real mapper implementation
        controller = new GreetingController(service, mapper);
    }

    /**
     * Creates a mocked Greeting entity with all fields stubbed.
     * Uses Mockito mocks instead of reflection - cleaner for unit tests.
     */
    private Greeting mockGreeting(Long id, String reference, String message, String recipient) {
        Greeting greeting = mock(Greeting.class);
        when(greeting.getId()).thenReturn(id);
        when(greeting.getReference()).thenReturn(reference);
        when(greeting.getMessage()).thenReturn(message);
        when(greeting.getRecipient()).thenReturn(recipient);
        when(greeting.getCreatedAt()).thenReturn(Instant.parse("2025-01-01T12:00:00Z"));
        return greeting;
    }

    @Nested
    @DisplayName("GET /v1/greetings/{id}")
    class GetGreeting {

        @Test
        @DisplayName("returns 200 with GreetingResponse when greeting exists")
        void returnsGreetingWhenFound() {
            // Arrange
            Long id = 506979954615549952L;
            String idStr = String.valueOf(id);
            Greeting entity = mockGreeting(id, "GRE-2025-000042", "Hello, World!", "Alice");
            when(service.getGreeting(id)).thenReturn(Optional.of(entity));

            // Act
            ResponseEntity<GreetingResponse> response = controller.getGreeting(idStr);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(idStr);
            assertThat(response.getBody().getReference()).isEqualTo("GRE-2025-000042");
            assertThat(response.getBody().getMessage()).isEqualTo("Hello, World!");
            assertThat(response.getBody().getRecipient()).isEqualTo("Alice");
            assertThat(response.getBody().getCreatedAt()).isNotNull();

            verify(service).getGreeting(id);
        }

        @Test
        @DisplayName("returns 404 when greeting does not exist")
        void returnsNotFoundWhenGreetingDoesNotExist() {
            // Arrange
            Long id = 999L;
            String idStr = String.valueOf(id);
            when(service.getGreeting(id)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> controller.getGreeting(idStr))
                    .isInstanceOf(GreetingNotFoundException.class)
                    .hasMessageContaining("Greeting") // Optional: Check message content
                    .hasMessageContaining(idStr);

            verify(service).getGreeting(id);
        }
    }

    @Nested
    @DisplayName("DELETE /v1/greetings/{id}")
    class DeleteGreeting {

        @Test
        @DisplayName("returns 204 when greeting is successfully deleted")
        void returnsNoContentWhenDeleted() {
            // Arrange
            Long id = 506979954615549952L;
            String idStr = String.valueOf(id);
            when(service.deleteGreeting(id)).thenReturn(true);

            // Act
            ResponseEntity<Void> response = controller.deleteGreeting(idStr);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(response.getBody()).isNull();

            verify(service).deleteGreeting(id);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when greeting does not exist")
        void throwsResourceNotFoundExceptionWhenGreetingDoesNotExist() {
            // Arrange
            Long id = 999L;
            String idStr = String.valueOf(id);
            when(service.deleteGreeting(id)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> controller.deleteGreeting(idStr))
                    .isInstanceOf(GreetingNotFoundException.class)
                    .hasMessageContaining("Greeting")
                    .hasMessageContaining("999");

            verify(service).deleteGreeting(id);
        }
    }

    @Nested
    @DisplayName("PUT /v1/greetings/{id}")
    class UpdateGreeting {

        @Test
        @DisplayName("returns 200 with updated GreetingResponse when greeting exists")
        void returnsUpdatedGreetingWhenFound() {
            // Arrange
            Long id = 506979954615549952L;
            String idStr = String.valueOf(id);
            UpdateGreetingRequest request = new UpdateGreetingRequest("Updated Message", "Bob");
            Greeting updatedEntity = mockGreeting(id, "GRE-2025-000042", "Updated Message", "Bob");
            when(service.updateGreeting(id, "Updated Message", "Bob")).thenReturn(Optional.of(updatedEntity));

            // Act
            ResponseEntity<GreetingResponse> response = controller.updateGreeting(idStr, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(idStr);
            assertThat(response.getBody().getMessage()).isEqualTo("Updated Message");
            assertThat(response.getBody().getRecipient()).isEqualTo("Bob");

            verify(service).updateGreeting(id, "Updated Message", "Bob");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when greeting does not exist")
        void throwsResourceNotFoundExceptionWhenGreetingDoesNotExist() {
            // Arrange
            Long id = 999L;
            String idStr = String.valueOf(id);
            UpdateGreetingRequest request = new UpdateGreetingRequest("Updated Message", "Bob");
            when(service.updateGreeting(id, "Updated Message", "Bob")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> controller.updateGreeting(idStr, request))
                    .isInstanceOf(GreetingNotFoundException.class)
                    .hasMessageContaining("Greeting")
                    .hasMessageContaining("999");

            verify(service).updateGreeting(id, "Updated Message", "Bob");
        }
    }

    @Nested
    @DisplayName("PATCH /v1/greetings/{id}")
    class PatchGreeting {

        @Test
        @DisplayName("returns 200 when patching only message")
        void returnsUpdatedGreetingWhenPatchingMessage() {
            // Arrange
            Long id = 506979954615549952L;
            String idStr = String.valueOf(id);
            PatchGreetingRequest request = new PatchGreetingRequest();
            request.setMessage("Patched Message");
            // recipient is null - not being patched

            Greeting patchedEntity = mockGreeting(id, "GRE-2025-000042", "Patched Message", "Alice");
            when(service.patchGreeting(id, "Patched Message", null)).thenReturn(Optional.of(patchedEntity));

            // Act
            ResponseEntity<GreetingResponse> response = controller.patchGreeting(idStr, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("Patched Message");
            assertThat(response.getBody().getRecipient()).isEqualTo("Alice");

            verify(service).patchGreeting(id, "Patched Message", null);
        }

        @Test
        @DisplayName("returns 200 when patching only recipient")
        void returnsUpdatedGreetingWhenPatchingRecipient() {
            // Arrange
            Long id = 506979954615549952L;
            String idStr = String.valueOf(id);
            PatchGreetingRequest request = new PatchGreetingRequest();
            request.setRecipient("Charlie");
            // message is null - not being patched

            Greeting patchedEntity = mockGreeting(id, "GRE-2025-000042", "Hello, World!", "Charlie");
            when(service.patchGreeting(id, null, "Charlie")).thenReturn(Optional.of(patchedEntity));

            // Act
            ResponseEntity<GreetingResponse> response = controller.patchGreeting(idStr, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("Hello, World!");
            assertThat(response.getBody().getRecipient()).isEqualTo("Charlie");

            verify(service).patchGreeting(id, null, "Charlie");
        }

        @Test
        @DisplayName("returns 200 when patching both message and recipient")
        void returnsUpdatedGreetingWhenPatchingBothFields() {
            // Arrange
            Long id = 506979954615549952L;
            String idStr = String.valueOf(id);
            PatchGreetingRequest request = new PatchGreetingRequest();
            request.setMessage("New Message");
            request.setRecipient("Dave");

            Greeting patchedEntity = mockGreeting(id, "GRE-2025-000042", "New Message", "Dave");
            when(service.patchGreeting(id, "New Message", "Dave")).thenReturn(Optional.of(patchedEntity));

            // Act
            ResponseEntity<GreetingResponse> response = controller.patchGreeting(idStr, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("New Message");
            assertThat(response.getBody().getRecipient()).isEqualTo("Dave");

            verify(service).patchGreeting(id, "New Message", "Dave");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when greeting does not exist")
        void throwsResourceNotFoundExceptionWhenGreetingDoesNotExist() {
            // Arrange
            Long id = 999L;
            String idStr = String.valueOf(id);
            PatchGreetingRequest request = new PatchGreetingRequest();
            request.setMessage("Patched Message");
            when(service.patchGreeting(id, "Patched Message", null)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> controller.patchGreeting(idStr, request))
                    .isInstanceOf(GreetingNotFoundException.class)
                    .hasMessageContaining("Greeting")
                    .hasMessageContaining("999");

            verify(service).patchGreeting(id, "Patched Message", null);
        }
    }
}
