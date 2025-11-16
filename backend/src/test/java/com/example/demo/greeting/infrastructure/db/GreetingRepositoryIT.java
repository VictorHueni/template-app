package com.example.demo.greeting.infrastructure.db;

import com.example.demo.greeting.domain.Greeting;
import com.example.demo.greeting.domain.GreetingId;
import com.example.demo.greeting.domain.GreetingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GreetingRepositoryIT extends AbstractPostgresIT {

    @Autowired
    GreetingRepository greetingRepository;

    @Test
    void savesAndLoadsGreetingFromPostgres() {
        Greeting toSave = new Greeting(
                GreetingId.newId(),
                "Bob",
                "Hello Bob",
                Instant.parse("2025-01-01T10:00:00Z")
        );

        Greeting saved = greetingRepository.save(toSave);

        var loaded = greetingRepository.findById(saved.id());

        assertThat(loaded).isPresent();
        assertThat(loaded.get().message()).isEqualTo("Hello Bob");
    }
}
