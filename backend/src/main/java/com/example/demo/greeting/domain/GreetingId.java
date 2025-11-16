package com.example.demo.greeting.domain;

import java.util.UUID;

public record GreetingId(UUID value) {
    public static GreetingId newId() {
        return new GreetingId(UUID.randomUUID());
    }
}
