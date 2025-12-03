package com.example.demo.greeting.service;

import com.example.demo.greeting.model.Greeting;
import com.example.demo.greeting.repository.GreetingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class GreetingService {

    private final GreetingRepository repository;
    private final Clock clock;

    public GreetingService(GreetingRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Greeting createGreeting(String message, String recipient) {
        Instant now = Instant.now(clock);
        Greeting entity = new Greeting(
                UUID.randomUUID(),
                recipient,
                message,
                now
        );
        return repository.save(entity);
    }

    public Page<Greeting> getGreetings(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return repository.findAll(pageable);
    }
}