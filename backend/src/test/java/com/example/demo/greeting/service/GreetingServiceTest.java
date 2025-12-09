package com.example.demo.greeting.service;

import com.example.demo.common.repository.FunctionalIdGenerator;
import com.example.demo.greeting.model.Greeting;
import com.example.demo.greeting.repository.GreetingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.AdditionalAnswers.*;

/**
 * Unit tests for GreetingService.
 * Tests follow TDD approach - some tests are written before implementation.
 */
class GreetingServiceTest {

    private FunctionalIdGenerator idGenerator;
    private GreetingRepository repo;
    private Clock fixed;
    private GreetingService service;

    @BeforeEach
    void setUp() {
        idGenerator = mock(FunctionalIdGenerator.class);
        repo = mock(GreetingRepository.class);
        fixed = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneOffset.UTC);
        service = new GreetingService(idGenerator, repo, fixed);
    }

    /**
     * Creates a mocked Greeting entity with all fields stubbed.
     * Uses Mockito mocks - ideal for read-only operations in unit tests.
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

    /**
     * Creates a mocked Greeting entity that supports mutation (setMessage, setRecipient).
     * The mock tracks the "current" values and returns them on subsequent getter calls.
     */
    private Greeting mockMutableGreeting(Long id, String reference, String initialMessage, String initialRecipient) {
        Greeting greeting = mock(Greeting.class);
        when(greeting.getId()).thenReturn(id);
        when(greeting.getReference()).thenReturn(reference);
        when(greeting.getCreatedAt()).thenReturn(Instant.parse("2025-01-01T12:00:00Z"));
        
        // Use Answer to track mutable state
        final String[] message = {initialMessage};
        final String[] recipient = {initialRecipient};
        
        when(greeting.getMessage()).thenAnswer(inv -> message[0]);
        when(greeting.getRecipient()).thenAnswer(inv -> recipient[0]);
        doAnswer(inv -> { message[0] = inv.getArgument(0); return null; }).when(greeting).setMessage(any());
        doAnswer(inv -> { recipient[0] = inv.getArgument(0); return null; }).when(greeting).setRecipient(any());
        
        return greeting;
    }

    @Nested
    @DisplayName("createGreeting")
    class CreateGreeting {
        @Test
        @DisplayName("creates greeting with message, recipient and timestamp")
        void createsGreetingWithMessageAndTimestamp() {
            when(idGenerator.generate("greeting_sequence", "GRE")).thenReturn("GRE-2025-000042");
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Greeting result = service.createGreeting("Hello, World!", "Alice");

            assertThat(result.getRecipient()).isEqualTo("Alice");
            assertThat(result.getMessage()).isEqualTo("Hello, World!");
            assertThat(result.getCreatedAt()).isEqualTo(Instant.parse("2025-01-01T12:00:00Z"));
            assertThat(result.getReference()).isEqualTo("GRE-2025-000042");

            verify(idGenerator).generate("greeting_sequence", "GRE");
            verify(repo).save(any(Greeting.class));
        }
    }

    @Nested
    @DisplayName("getGreetings (paginated)")
    class GetGreetings {
        @Test
        @DisplayName("returns paged greetings from repository")
        void getsPagedGreetingsFromRepository() {
            Greeting entity = new Greeting("Bob", "Hi", Instant.now());
            PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Greeting> expectedPage = new PageImpl<>(List.of(entity), pageRequest, 1);

            when(repo.findAll(pageRequest)).thenReturn(expectedPage);

            Page<Greeting> result = service.getGreetings(0, 10);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(repo).findAll(pageRequest);
        }
    }

    // ============================================================
    // TDD Tests - Methods not yet implemented
    // ============================================================

    @Nested
    @DisplayName("getGreeting (by id)")
    class GetGreeting {

        @Test
        @DisplayName("returns greeting when found by id")
        void returnsGreetingWhenFound() {
            // Arrange
            Long id = 506979954615549952L;
            Greeting entity = mockGreeting(id, "GRE-2025-000042", "Hello", "Alice");
            when(repo.findById(id)).thenReturn(Optional.of(entity));

            // Act
            Optional<Greeting> result = service.getGreeting(id);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(id);
            assertThat(result.get().getRecipient()).isEqualTo("Alice");
            assertThat(result.get().getMessage()).isEqualTo("Hello");
            verify(repo).findById(id);
        }

        @Test
        @DisplayName("returns empty when greeting not found")
        void returnsEmptyWhenNotFound() {
            // Arrange
            Long id = 999L;
            when(repo.findById(id)).thenReturn(Optional.empty());

            // Act
            Optional<Greeting> result = service.getGreeting(id);

            // Assert
            assertThat(result).isEmpty();
            verify(repo).findById(id);
        }
    }

    @Nested
    @DisplayName("updateGreeting (full replacement)")
    class UpdateGreeting {

        @Test
        @DisplayName("updates all fields when greeting exists")
        void updatesGreetingWhenFound() {
            // Arrange
            Long id = 506979954615549952L;
            Greeting existing = mockMutableGreeting(id, "GRE-2025-000042", "Old Message", "Alice");
            when(repo.findById(id)).thenReturn(Optional.of(existing));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Optional<Greeting> result = service.updateGreeting(id, "New Message", "Bob");

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getMessage()).isEqualTo("New Message");
            assertThat(result.get().getRecipient()).isEqualTo("Bob");
            verify(repo).findById(id);
            verify(repo).save(any(Greeting.class));
        }

        @Test
        @DisplayName("returns empty when greeting not found")
        void returnsEmptyWhenNotFound() {
            // Arrange
            Long id = 999L;
            when(repo.findById(id)).thenReturn(Optional.empty());

            // Act
            Optional<Greeting> result = service.updateGreeting(id, "New Message", "Bob");

            // Assert
            assertThat(result).isEmpty();
            verify(repo).findById(id);
            verify(repo, never()).save(any());
        }
    }

    @Nested
    @DisplayName("patchGreeting (partial update)")
    class PatchGreeting {

        @Test
        @DisplayName("patches only message when recipient is null")
        void patchesOnlyMessageWhenRecipientIsNull() {
            // Arrange
            Long id = 506979954615549952L;
            Greeting existing = mockMutableGreeting(id, "GRE-2025-000042", "Old Message", "Alice");
            when(repo.findById(id)).thenReturn(Optional.of(existing));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Optional<Greeting> result = service.patchGreeting(id, "Patched Message", null);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getMessage()).isEqualTo("Patched Message");
            assertThat(result.get().getRecipient()).isEqualTo("Alice"); // unchanged
            verify(repo).save(any(Greeting.class));
        }

        @Test
        @DisplayName("patches only recipient when message is null")
        void patchesOnlyRecipientWhenMessageIsNull() {
            // Arrange
            Long id = 506979954615549952L;
            Greeting existing = mockMutableGreeting(id, "GRE-2025-000042", "Hello", "Alice");
            when(repo.findById(id)).thenReturn(Optional.of(existing));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Optional<Greeting> result = service.patchGreeting(id, null, "Charlie");

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getMessage()).isEqualTo("Hello"); // unchanged
            assertThat(result.get().getRecipient()).isEqualTo("Charlie");
            verify(repo).save(any(Greeting.class));
        }

        @Test
        @DisplayName("patches both fields when both are provided")
        void patchesBothFieldsWhenBothProvided() {
            // Arrange
            Long id = 506979954615549952L;
            Greeting existing = mockMutableGreeting(id, "GRE-2025-000042", "Old Message", "Alice");
            when(repo.findById(id)).thenReturn(Optional.of(existing));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Optional<Greeting> result = service.patchGreeting(id, "New Message", "Dave");

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getMessage()).isEqualTo("New Message");
            assertThat(result.get().getRecipient()).isEqualTo("Dave");
            verify(repo).save(any(Greeting.class));
        }

        @Test
        @DisplayName("returns empty when greeting not found")
        void returnsEmptyWhenNotFound() {
            // Arrange
            Long id = 999L;
            when(repo.findById(id)).thenReturn(Optional.empty());

            // Act
            Optional<Greeting> result = service.patchGreeting(id, "Patched", null);

            // Assert
            assertThat(result).isEmpty();
            verify(repo).findById(id);
            verify(repo, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteGreeting")
    class DeleteGreeting {

        @Test
        @DisplayName("returns true when greeting is deleted")
        void returnsTrueWhenDeleted() {
            // Arrange
            Long id = 506979954615549952L;
            when(repo.existsById(id)).thenReturn(true);

            // Act
            boolean result = service.deleteGreeting(id);

            // Assert
            assertThat(result).isTrue();
            verify(repo).existsById(id);
            verify(repo).deleteById(id);
        }

        @Test
        @DisplayName("returns false when greeting not found")
        void returnsFalseWhenNotFound() {
            // Arrange
            Long id = 999L;
            when(repo.existsById(id)).thenReturn(false);

            // Act
            boolean result = service.deleteGreeting(id);

            // Assert
            assertThat(result).isFalse();
            verify(repo).existsById(id);
            verify(repo, never()).deleteById(any());
        }
    }
}
