package com.example.demo.greeting.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

public record GreetingResponseDto(
        UUID id,
        String name,
        String message,
        Instant createdAt
) {
}
