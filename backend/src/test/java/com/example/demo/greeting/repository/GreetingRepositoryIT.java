package com.example.demo.greeting.repository;

import com.example.demo.greeting.model.Greeting;
import com.example.demo.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for GreetingRepository with real PostgreSQL database.
 * Uses the singleton Testcontainers PostgreSQL instance from {@link AbstractIntegrationTest}.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GreetingRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    GreetingRepository greetingRepository;

    @Test
    void savesAndLoadsGreetingFromPostgres() {
        Greeting toSave = new Greeting(
                "Bob",
                "Hello Bob",
                Instant.parse("2025-01-01T10:00:00Z")
        );

        toSave.setReference("GRE-2025-000001");

        Greeting saved = greetingRepository.save(toSave);

        var loaded = greetingRepository.findById(saved.getId());

        assertThat(loaded).isPresent();
        assertThat(loaded.get().getId()).isNotNull();
        assertThat(loaded.get().getReference()).isEqualTo("GRE-2025-000001");
        assertThat(loaded.get().getRecipient()).isEqualTo("Bob");
        assertThat(loaded.get().getMessage()).isEqualTo("Hello Bob");
    }

    @Test
    void findsGreetingsWithPaginationAndSorting() {
        // 1. Arrange: Clear DB and save 3 greetings with different times
        greetingRepository.deleteAll();

        Instant now = Instant.now();
        Greeting oldGreeting = new Greeting("Old", "First", now.minus(1, ChronoUnit.HOURS));
        Greeting midGreeting = new Greeting("Mid", "Second", now);
        Greeting newGreeting = new Greeting("New", "Third", now.plus(1, ChronoUnit.HOURS));

        oldGreeting.setReference("GRE-2025-000001");
        midGreeting.setReference("GRE-2025-000002");
        newGreeting.setReference("GRE-2025-000003");

        greetingRepository.save(oldGreeting);
        greetingRepository.save(midGreeting);
        greetingRepository.save(newGreeting);

        // 2. Act: Request Page 0 with Size 2
        // Since we sort by createdAt DESC, we expect "New" and "Mid" on page 0
        PageRequest pageRequest = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Greeting> result = greetingRepository.findAll(pageRequest);

        // 3. Assert
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);

        // Verify Sorting (Newest First)
        assertThat(result.getContent().get(0).getMessage()).isEqualTo("Third");
        assertThat(result.getContent().get(1).getMessage()).isEqualTo("Second");
        assertThat(result.getContent().get(0).getReference()).isEqualTo("GRE-2025-000003");
        assertThat(result.getContent().get(1).getReference()).isEqualTo("GRE-2025-000002");
    }

    // ============================================================
    // TDD Tests for CRUD operations (get, update, delete)
    // ============================================================

    @Test
    void findsGreetingById() {
        // Arrange
        Greeting toSave = new Greeting("Alice", "Hello Alice", Instant.parse("2025-01-01T10:00:00Z"));
        toSave.setReference("GRE-2025-000010");
        Greeting saved = greetingRepository.save(toSave);

        // Act
        var found = greetingRepository.findById(saved.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getReference()).isEqualTo("GRE-2025-000010");
        assertThat(found.get().getRecipient()).isEqualTo("Alice");
        assertThat(found.get().getMessage()).isEqualTo("Hello Alice");
    }

    @Test
    void returnsEmptyWhenGreetingNotFoundById() {
        // Act
        var found = greetingRepository.findById(999999999L);

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void deletesGreetingById() {
        // Arrange
        Greeting toSave = new Greeting("ToDelete", "Delete me", Instant.parse("2025-01-01T10:00:00Z"));
        toSave.setReference("GRE-2025-000020");
        Greeting saved = greetingRepository.save(toSave);
        Long id = saved.getId();

        // Verify it exists
        assertThat(greetingRepository.existsById(id)).isTrue();

        // Act
        greetingRepository.deleteById(id);

        // Assert
        assertThat(greetingRepository.existsById(id)).isFalse();
        assertThat(greetingRepository.findById(id)).isEmpty();
    }

    @Test
    void existsByIdReturnsTrueWhenExists() {
        // Arrange
        Greeting toSave = new Greeting("Exists", "I exist", Instant.parse("2025-01-01T10:00:00Z"));
        toSave.setReference("GRE-2025-000030");
        Greeting saved = greetingRepository.save(toSave);

        // Act & Assert
        assertThat(greetingRepository.existsById(saved.getId())).isTrue();
    }

    @Test
    void existsByIdReturnsFalseWhenNotExists() {
        // Act & Assert
        assertThat(greetingRepository.existsById(999999999L)).isFalse();
    }
}
