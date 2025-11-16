package com.example.demo.greeting.domain;

import com.example.demo.greeting.application.CreateGreetingCommand;
import com.example.demo.greeting.application.GreetingService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GreetingServiceTest {

    @Test
    void createsGreetingWithMessageAndTimestamp() {
        GreetingRepository repo = mock(GreetingRepository.class);
        Clock fixed = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneOffset.UTC);

        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GreetingService service = new GreetingService(repo, fixed);

        var cmd = new CreateGreetingCommand("Alice");
        Greeting result = service.createGreeting(cmd);

        assertThat(result.name()).isEqualTo("Alice");
        assertThat(result.message()).isEqualTo("Hello Alice");
        assertThat(result.createdAt()).isEqualTo(Instant.parse("2025-01-01T12:00:00Z"));

        verify(repo).save(any(Greeting.class));
    }
}
