package com.example.demo.greeting.service;

import com.example.demo.common.repository.FunctionalIdGenerator;
import com.example.demo.greeting.model.Greeting;
import com.example.demo.greeting.repository.GreetingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GreetingServiceTest {

    @Test
    void createsGreetingWithMessageAndTimestamp() {
        FunctionalIdGenerator idGenerator = mock(FunctionalIdGenerator.class);
        GreetingRepository repo = mock(GreetingRepository.class);
        Clock fixed = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneOffset.UTC);

        when(idGenerator.generate("greeting_sequence", "GRE")).thenReturn("GRE-2025-000042");
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GreetingService service = new GreetingService(idGenerator, repo, fixed);

        Greeting result = service.createGreeting("Hello, World!", "Alice");

        assertThat(result.getRecipient()).isEqualTo("Alice");
        assertThat(result.getMessage()).isEqualTo("Hello, World!");
        assertThat(result.getCreatedAt()).isEqualTo(Instant.parse("2025-01-01T12:00:00Z"));
        assertThat(result.getReference()).isEqualTo("GRE-2025-000042");

        verify(idGenerator).generate("greeting_sequence", "GRE");
        verify(repo).save(any(Greeting.class));
    }

    @Test
    void getsPagedGreetingsFromRepository() {
        FunctionalIdGenerator idGenerator = mock(FunctionalIdGenerator.class);
        GreetingRepository repo = mock(GreetingRepository.class);
        Clock fixed = Clock.systemUTC();
        GreetingService service = new GreetingService(idGenerator, repo, fixed);

        // Prepare mock data
        Greeting entity = new Greeting(
                "Bob", "Hi", Instant.now()
        );
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Greeting> expectedPage = new PageImpl<>(List.of(entity), pageRequest, 1);

        when(repo.findAll(pageRequest)).thenReturn(expectedPage);

        // Execute
        Page<Greeting> result = service.getGreetings(0, 10);

        // Verify
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(repo).findAll(pageRequest);
    }
}
