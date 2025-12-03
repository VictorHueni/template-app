package com.example.demo.greeting.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "greeting")
public class Greeting {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Greeting() {
        // JPA
    }

    public Greeting(UUID id, String recipient, String message, Instant createdAt) {
        this.id = id;
        this.recipient = recipient;
        this.message = message;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getRecipient() { return recipient; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}
