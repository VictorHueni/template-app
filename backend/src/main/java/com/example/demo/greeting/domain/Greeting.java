package com.example.demo.greeting.domain;

import java.time.Instant;

public class Greeting {
    private final GreetingId id;
    private final String name;
    private final String message;
    private final Instant createdAt;

    public Greeting(GreetingId id, String name, String message, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.message = message;
        this.createdAt = createdAt;
    }

    public GreetingId id() { return id; }
    public String name() { return name; }
    public String message() { return message; }
    public Instant createdAt() { return createdAt; }
}
