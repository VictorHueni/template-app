package com.example.demo.greeting.infrastructure.db;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing the greeting record stored in the database.
 * <p>
 * Maps relational data to Java objects. Contains no business logic.
 * <p>
 * Validated through integration tests with Testcontainers + JPA/Hibernate.
 */
@Entity
@Table(name = "greeting")
public class GreetingJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GreetingJpaEntity() {
        // JPA
    }

    public GreetingJpaEntity(UUID id, String recipient, String message, Instant createdAt) {
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
