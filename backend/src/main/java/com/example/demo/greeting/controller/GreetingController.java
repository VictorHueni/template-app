package com.example.demo.greeting.controller;

import com.example.demo.api.v1.controller.GreetingsApi;
import com.example.demo.api.v1.model.CreateGreetingRequest;
import com.example.demo.api.v1.model.GreetingPage;
import com.example.demo.api.v1.model.GreetingResponse;
import com.example.demo.api.v1.model.PageMeta;
import com.example.demo.greeting.model.Greeting;
import com.example.demo.greeting.service.GreetingService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RestController
public class GreetingController implements GreetingsApi {

    private final GreetingService service;

    public GreetingController(GreetingService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<GreetingResponse> createGreeting(CreateGreetingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CreateGreetingRequest cannot be null");
        }

        // Call service with simple parameters
        Greeting entity = service.createGreeting(
                request.getMessage(),
                request.getRecipient()
        );

        // Map JPA entity to OpenAPI DTO
        GreetingResponse response = toGreetingResponse(entity);

        return ResponseEntity.status(201).body(response);
    }

    @Override
    public ResponseEntity<GreetingPage> listGreetings(Integer page, Integer size) {
        // Call service - returns Spring Data Page
        Page<Greeting> entityPage = service.getGreetings(page, size);

        // Map JPA entities to OpenAPI DTOs
        List<GreetingResponse> dtos = entityPage.getContent().stream()
                .map(this::toGreetingResponse)
                .toList();

        // Construct API page metadata
        PageMeta meta = new PageMeta(
                entityPage.getNumber(),
                entityPage.getSize(),
                (int) entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );

        return ResponseEntity.ok(new GreetingPage(dtos, meta));
    }

    /**
     * Maps JPA entity to OpenAPI DTO
     */
    private GreetingResponse toGreetingResponse(Greeting entity) {
        GreetingResponse response = new GreetingResponse(
                entity.getId(),
                entity.getMessage()
        );
        response.setRecipient(entity.getRecipient());
        response.setCreatedAt(OffsetDateTime.ofInstant(entity.getCreatedAt(), ZoneOffset.UTC));
        return response;
    }
}
