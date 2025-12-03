package com.example.demo.greeting.infrastructure.db;

import com.example.demo.greeting.model.Greeting;
import com.example.demo.greeting.repository.GreetingRepository;
import com.example.demo.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for GreetingRepository with real PostgreSQL database.
 * Uses the singleton Testcontainers PostgreSQL instance from {@link AbstractIntegrationTest}.
 */
@SpringBootTest
class GreetingRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    GreetingRepository greetingRepository;

    @Test
    void savesAndLoadsGreetingFromPostgres() {
        Greeting toSave = new Greeting(
                UUID.randomUUID(),
                "Bob",
                "Hello Bob",
                Instant.parse("2025-01-01T10:00:00Z")
        );

        Greeting saved = greetingRepository.save(toSave);

        var loaded = greetingRepository.findById(saved.getId());

        assertThat(loaded).isPresent();
        assertThat(loaded.get().getRecipient()).isEqualTo("Bob");
        assertThat(loaded.get().getMessage()).isEqualTo("Hello Bob");
    }

    @Test
    void findsGreetingsWithPaginationAndSorting() {
        // 1. Arrange: Clear DB and save 3 greetings with different times
        greetingRepository.deleteAll();

        Instant now = Instant.now();
        Greeting oldGreeting = new Greeting(UUID.randomUUID(), "Old", "First", now.minus(1, ChronoUnit.HOURS));
        Greeting midGreeting = new Greeting(UUID.randomUUID(), "Mid", "Second", now);
        Greeting newGreeting = new Greeting(UUID.randomUUID(), "New", "Third", now.plus(1, ChronoUnit.HOURS));

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
    }
}
