package com.example.demo.greeting.infrastructure.web;

import com.example.demo.greeting.application.CreateGreetingCommand;
import com.example.demo.greeting.application.GreetingService;
import com.example.demo.greeting.domain.Greeting;
import org.springframework.web.bind.annotation.*;

/**
 * HTTP adapter exposing greeting endpoints.
 * <p>
 * Responsible only for request handling, validation, and delegation to the domain layer.
 * Converts domain responses into HTTP responses.
 * <p>
 * Tested with Spring MVC slice tests or API integration tests (REST Assured).
 */
@RestController
@RequestMapping("/api/greetings")
public class GreetingController {

    private final GreetingService service;

    public GreetingController(GreetingService service) {
        this.service = service;
    }

    @PostMapping
    public GreetingResponseDto create(@RequestBody GreetingRequestDto request) {
        Greeting created = service.createGreeting(new CreateGreetingCommand(request.name()));
        return new GreetingResponseDto(
                created.id().value(),
                created.name(),
                created.message(),
                created.createdAt()
        );
    }
}
