package com.example.demo.greeting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.greeting.model.Greeting;

/**
 * Repository for Greeting entities with audit history support.
 *
 * <p>Extends RevisionRepository to provide audit history queries:</p>
 * <ul>
 *   <li>{@code findRevisions(id)} - Get all revisions for an entity</li>
 *   <li>{@code findRevision(id, revisionNumber)} - Get specific revision</li>
 *   <li>{@code findLastChangeRevision(id)} - Get most recent revision</li>
 * </ul>
 */
@Repository
public interface GreetingRepository
        extends JpaRepository<Greeting, Long>,
                RevisionRepository<Greeting, Long, Integer> {

    boolean existsByReference(String reference);
}
