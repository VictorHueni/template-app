package com.example.demo.greeting.service;

import com.example.demo.common.audit.CustomRevisionEntity;
import com.example.demo.common.repository.FunctionalIdGenerator;
import com.example.demo.greeting.dto.GreetingRevisionDTO;
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
import org.springframework.data.history.Revision;
import org.springframework.data.history.RevisionMetadata;
import org.springframework.data.history.Revisions;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GreetingService.
 * Tests follow TDD approach - some tests are written before implementation.
 */
class GreetingServiceTest {

    private FunctionalIdGenerator idGenerator;
    private GreetingRepository repo;
    private GreetingService service;

    @BeforeEach
    void setUp() {
        idGenerator = mock(FunctionalIdGenerator.class);
        repo = mock(GreetingRepository.class);
        service = new GreetingService(idGenerator, repo);
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
     * Creates a real Greeting entity for tests that need mutable state.
     * Uses reflection to set the ID field since it has no setter.
     */
    private Greeting createTestGreeting(Long id, String reference, String message, String recipient) {
        Greeting greeting = new Greeting(recipient, message);
        greeting.setReference(reference);
        setIdViaReflection(greeting, id);
        return greeting;
    }

    private void setIdViaReflection(Greeting greeting, Long id) {
        try {
            Field idField = greeting.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(greeting, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set ID via reflection", e);
        }
    }

    @Nested
    @DisplayName("createGreeting")
    class CreateGreeting {
        @Test
        @DisplayName("creates greeting with message, recipient and reference")
        void createsGreetingWithMessageAndTimestamp() {
            when(idGenerator.generate("greeting_sequence", "GRE")).thenReturn("GRE-2025-000042");
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Greeting result = service.createGreeting("Hello, World!", "Alice");

            assertThat(result.getRecipient()).isEqualTo("Alice");
            assertThat(result.getMessage()).isEqualTo("Hello, World!");
            assertThat(result.getReference()).isEqualTo("GRE-2025-000042");
            // Note: createdAt is populated by JPA Auditing, not available in unit tests

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
            Greeting entity = new Greeting("Bob", "Hi");
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
            Greeting existing = createTestGreeting(id, "GRE-2025-000042", "Old Message", "Alice");
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
            Greeting existing = createTestGreeting(id, "GRE-2025-000042", "Old Message", "Alice");
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
            Greeting existing = createTestGreeting(id, "GRE-2025-000042", "Hello", "Alice");
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
            Greeting existing = createTestGreeting(id, "GRE-2025-000042", "Old Message", "Alice");
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

    // ============================================================
    // Audit History Tests
    // ============================================================

    @Nested
    @DisplayName("getGreetingHistory")
    class GetGreetingHistory {

        @Test
        @DisplayName("returns all revisions for a greeting")
        void returnsAllRevisionsForGreeting() {
            // Arrange
            Long id = 506979954615549952L;
            Instant revTime = Instant.parse("2025-01-15T10:00:00Z");

            Greeting entity = mockGreeting(id, "GRE-2025-000042", "Hello", "Alice");

            CustomRevisionEntity customRev = new CustomRevisionEntity();
            customRev.setUsername("testuser");


            RevisionMetadata<Integer> metadata = mock(RevisionMetadata.class);
            when(metadata.getRequiredRevisionNumber()).thenReturn(1);
            when(metadata.getRequiredRevisionInstant()).thenReturn(revTime);
            when(metadata.getRevisionType()).thenReturn(RevisionMetadata.RevisionType.INSERT);
            when(metadata.getDelegate()).thenReturn(customRev);

            @SuppressWarnings("unchecked")
            Revision<Integer, Greeting> revision = mock(Revision.class);
            when(revision.getEntity()).thenReturn(entity);
            when(revision.getMetadata()).thenReturn(metadata);

            Revisions<Integer, Greeting> revisions = Revisions.of(List.of(revision));
            when(repo.findRevisions(id)).thenReturn(revisions);

            // Act
            List<GreetingRevisionDTO> result = service.getGreetingHistory(id);

            // Assert
            assertThat(result).hasSize(1);
            GreetingRevisionDTO dto = result.get(0);
            assertThat(dto.revisionNumber()).isEqualTo(1);
            assertThat(dto.revisionDate()).isEqualTo(revTime);
            assertThat(dto.revisionType()).isEqualTo("INSERT");
            assertThat(dto.modifiedBy()).isEqualTo("testuser");
            assertThat(dto.id()).isEqualTo(id);
            assertThat(dto.reference()).isEqualTo("GRE-2025-000042");
            assertThat(dto.message()).isEqualTo("Hello");
            assertThat(dto.recipient()).isEqualTo("Alice");

            verify(repo).findRevisions(id);
        }

        @Test
        @DisplayName("returns empty list when no revisions exist")
        void returnsEmptyListWhenNoRevisions() {
            // Arrange
            Long id = 506979954615549952L;
            when(repo.findRevisions(id)).thenReturn(Revisions.none());

            // Act
            List<GreetingRevisionDTO> result = service.getGreetingHistory(id);

            // Assert
            assertThat(result).isEmpty();
            verify(repo).findRevisions(id);
        }
    }

    @Nested
    @DisplayName("getGreetingAtRevision")
    class GetGreetingAtRevision {

        @Test
        @DisplayName("returns revision when it exists")
        void returnsRevisionWhenExists() {
            // Arrange
            Long id = 506979954615549952L;
            Integer revisionNumber = 5;
            Instant revTime = Instant.parse("2025-01-15T10:00:00Z");

            Greeting entity = mockGreeting(id, "GRE-2025-000042", "Updated", "Bob");

            CustomRevisionEntity customRev = new CustomRevisionEntity();
            customRev.setUsername("admin");

            @SuppressWarnings("unchecked")
            RevisionMetadata<Integer> metadata = mock(RevisionMetadata.class);
            when(metadata.getRequiredRevisionNumber()).thenReturn(revisionNumber);
            when(metadata.getRequiredRevisionInstant()).thenReturn(revTime);
            when(metadata.getRevisionType()).thenReturn(RevisionMetadata.RevisionType.UPDATE);
            when(metadata.getDelegate()).thenReturn(customRev);

            @SuppressWarnings("unchecked")
            Revision<Integer, Greeting> revision = mock(Revision.class);
            when(revision.getEntity()).thenReturn(entity);
            when(revision.getMetadata()).thenReturn(metadata);

            when(repo.findRevision(id, revisionNumber)).thenReturn(Optional.of(revision));

            // Act
            Optional<GreetingRevisionDTO> result = service.getGreetingAtRevision(id, revisionNumber);

            // Assert
            assertThat(result).isPresent();
            GreetingRevisionDTO dto = result.get();
            assertThat(dto.revisionNumber()).isEqualTo(5);
            assertThat(dto.revisionType()).isEqualTo("UPDATE");
            assertThat(dto.modifiedBy()).isEqualTo("admin");
            assertThat(dto.message()).isEqualTo("Updated");

            verify(repo).findRevision(id, revisionNumber);
        }

        @Test
        @DisplayName("returns empty when revision not found")
        void returnsEmptyWhenRevisionNotFound() {
            // Arrange
            Long id = 506979954615549952L;
            Integer revisionNumber = 999;
            when(repo.findRevision(id, revisionNumber)).thenReturn(Optional.empty());

            // Act
            Optional<GreetingRevisionDTO> result = service.getGreetingAtRevision(id, revisionNumber);

            // Assert
            assertThat(result).isEmpty();
            verify(repo).findRevision(id, revisionNumber);
        }
    }

    @Nested
    @DisplayName("getLastGreetingRevision")
    class GetLastGreetingRevision {

        @Test
        @DisplayName("returns latest revision when exists")
        void returnsLatestRevisionWhenExists() {
            // Arrange
            Long id = 506979954615549952L;
            Instant revTime = Instant.parse("2025-01-20T15:30:00Z");

            Greeting entity = mockGreeting(id, "GRE-2025-000042", "Final", "Charlie");

            CustomRevisionEntity customRev = new CustomRevisionEntity();
            customRev.setUsername("system");

            @SuppressWarnings("unchecked")
            RevisionMetadata<Integer> metadata = mock(RevisionMetadata.class);
            when(metadata.getRequiredRevisionNumber()).thenReturn(10);
            when(metadata.getRequiredRevisionInstant()).thenReturn(revTime);
            when(metadata.getRevisionType()).thenReturn(RevisionMetadata.RevisionType.UPDATE);
            when(metadata.getDelegate()).thenReturn(customRev);

            @SuppressWarnings("unchecked")
            Revision<Integer, Greeting> revision = mock(Revision.class);
            when(revision.getEntity()).thenReturn(entity);
            when(revision.getMetadata()).thenReturn(metadata);

            when(repo.findLastChangeRevision(id)).thenReturn(Optional.of(revision));

            // Act
            Optional<GreetingRevisionDTO> result = service.getLastGreetingRevision(id);

            // Assert
            assertThat(result).isPresent();
            GreetingRevisionDTO dto = result.get();
            assertThat(dto.revisionNumber()).isEqualTo(10);
            assertThat(dto.revisionDate()).isEqualTo(revTime);
            assertThat(dto.modifiedBy()).isEqualTo("system");

            verify(repo).findLastChangeRevision(id);
        }

        @Test
        @DisplayName("returns empty when entity has no revisions")
        void returnsEmptyWhenNoRevisions() {
            // Arrange
            Long id = 506979954615549952L;
            when(repo.findLastChangeRevision(id)).thenReturn(Optional.empty());

            // Act
            Optional<GreetingRevisionDTO> result = service.getLastGreetingRevision(id);

            // Assert
            assertThat(result).isEmpty();
            verify(repo).findLastChangeRevision(id);
        }
    }
}
