package com.example.demo.greeting.domain;

import java.time.Instant;

public class Greeting {
    private final GreetingId id;
    private final String recipient;
    private final String message;
    private final Instant createdAt;

    public Greeting(GreetingId id, String recipient, String message, Instant createdAt) {
        this.id = id;
        this.recipient = recipient;
        this.message = message;
        this.createdAt = createdAt;
    }

    public GreetingId id() { return id; }
    public String recipient() { return recipient; }
    public String message() { return message; }
    public Instant createdAt() { return createdAt; }
}
