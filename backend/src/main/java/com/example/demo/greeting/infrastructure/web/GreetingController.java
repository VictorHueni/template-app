package com.example.demo.greeting.infrastructure.web;

import com.example.demo.api.v1.controller.GreetingsApi;
import com.example.demo.api.v1.model.CreateGreetingRequest;
import com.example.demo.api.v1.model.GreetingPage;
import com.example.demo.api.v1.model.GreetingResponse;
import com.example.demo.api.v1.model.PageMeta;
import com.example.demo.greeting.application.CreateGreetingCommand;
import com.example.demo.greeting.application.GreetingService;
import com.example.demo.greeting.domain.Greeting;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * HTTP adapter exposing greeting endpoints.
 * <p>
 * Responsible only for request handling, validation, and delegation to the domain layer.
 * Converts domain responses into HTTP responses.
 * <p>
 * Tested with Spring MVC slice tests or API integration tests (REST Assured).
 */
@RestController
public class GreetingController implements GreetingsApi {

    private final GreetingService service;

    public GreetingController(GreetingService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<GreetingResponse> createGreeting(CreateGreetingRequest createGreetingRequest) {
        if (createGreetingRequest == null) {
            throw new IllegalArgumentException("CreateGreetingRequest cannot be null");
        }

        // Map DTO to Command
        CreateGreetingCommand command = new CreateGreetingCommand(
                createGreetingRequest.getMessage(),
                createGreetingRequest.getRecipient()
        );

        // Execute domain logic
        Greeting greeting = service.createGreeting(command);

        // Map Domain to DTO
        GreetingResponse response = toGreetingResponse(greeting);

        return ResponseEntity.status(201).body(response);
    }

    @Override
    public ResponseEntity<GreetingPage> listGreetings(Integer page, Integer size) {
        // TODO: Implement pagination once repository supports it
        GreetingPage greetingPage = new GreetingPage(
                List.of(),
                new PageMeta(page, size, 0, 0)
        );
        return ResponseEntity.ok(greetingPage);
    }

    /**
     * Maps a domain Greeting to OpenAPI GreetingResponse DTO
     */
    private GreetingResponse toGreetingResponse(Greeting greeting) {
        GreetingResponse response = new GreetingResponse(
                greeting.id().value(),
                greeting.message()
        );
        response.setRecipient(greeting.recipient());
        response.setCreatedAt(OffsetDateTime.ofInstant(greeting.createdAt(), ZoneOffset.UTC));
        return response;
    }
}
