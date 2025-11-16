package com.example.demo.greeting.application;

import com.example.demo.greeting.domain.Greeting;
import com.example.demo.greeting.domain.GreetingId;
import com.example.demo.greeting.domain.GreetingRepository;

import java.time.Clock;
import java.time.Instant;

/**
 * Domain service responsible for all greeting business logic.
 * <p>
 * Framework-agnostic and free of I/O concerns. Depends only on domain abstractions,
 * such as {@link GreetingRepository} and {@link java.time.Clock}.
 * <p>
 * Tested via pure unit tests (Surefire).
 */
public class GreetingService {

    private final GreetingRepository repository;
    private final Clock clock;

    public GreetingService(GreetingRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Greeting createGreeting(CreateGreetingCommand cmd) {
        Instant now = Instant.now(clock);
        Greeting greeting = new Greeting(
                GreetingId.newId(),
                cmd.name(),
                "Hello " + cmd.name(),
                now
        );
        return repository.save(greeting);
    }
}