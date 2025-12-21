package com.example.demo.greeting.controller;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.api.v1.controller.GreetingsApi;
import com.example.demo.api.v1.model.CreateGreetingRequest;
import com.example.demo.api.v1.model.GreetingPage;
import com.example.demo.api.v1.model.GreetingResponse;
import com.example.demo.api.v1.model.PageMeta;
import com.example.demo.api.v1.model.PatchGreetingRequest;
import com.example.demo.api.v1.model.UpdateGreetingRequest;
import com.example.demo.common.exception.ResourceNotFoundException;
import com.example.demo.greeting.model.Greeting;
import com.example.demo.greeting.service.GreetingService;

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
                String.valueOf(entity.getId()),
                entity.getReference(),
                entity.getMessage()
        );
        response.setRecipient(entity.getRecipient());
        response.setCreatedAt(OffsetDateTime.ofInstant(entity.getCreatedAt(), ZoneOffset.UTC));
        return response;
    }

    @Override
    public ResponseEntity<Void> deleteGreeting(String id) {
        Long idLong = Long.parseLong(id);
        if (service.deleteGreeting(idLong)) {
            return ResponseEntity.noContent().build();
        }
        else {
            throw new ResourceNotFoundException("Greeting", idLong);
        }
    }

    @Override
    public ResponseEntity<GreetingResponse> getGreeting(String id) {
        Long idLong = Long.parseLong(id);
        return service.getGreeting(idLong)
                .map(this::toGreetingResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<GreetingResponse> patchGreeting(String id, PatchGreetingRequest patchGreetingRequest) {
        Long idLong = Long.parseLong(id);
        Greeting greeting = service.patchGreeting(idLong,
                        patchGreetingRequest.getMessage(),
                        patchGreetingRequest.getRecipient())
                .orElseThrow(() -> new ResourceNotFoundException("Greeting", idLong));
        return ResponseEntity.ok(toGreetingResponse(greeting));
    }

    @Override
    public ResponseEntity<GreetingResponse> updateGreeting(String id, UpdateGreetingRequest updateGreetingRequest) {
        Long idLong = Long.parseLong(id);
        Greeting greeting = service.updateGreeting(idLong,
                        updateGreetingRequest.getMessage(),
                        updateGreetingRequest.getRecipient())
                .orElseThrow(() -> new ResourceNotFoundException("Greeting", idLong));
        return ResponseEntity.ok(toGreetingResponse(greeting));
    }
}
