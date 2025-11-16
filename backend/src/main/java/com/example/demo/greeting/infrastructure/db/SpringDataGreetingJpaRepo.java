package com.example.demo.greeting.infrastructure.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository providing CRUD operations for {@link GreetingJpaEntity}.
 * <p>
 * Defines low-level DB access used internally by the persistence adapter.
 * Not referenced by the domain layer directly.
 */
public interface SpringDataGreetingJpaRepo extends JpaRepository<GreetingJpaEntity, UUID> {
}
