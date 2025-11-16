package com.example.demo.greeting.domain;

import com.example.demo.greeting.application.GreetingService;

import java.util.Optional;

/**
 * Domain port defining the persistence operations required by the greeting use case.
 * <p>
 * Implemented by an infrastructure adapter and injected into {@link GreetingService}.
 * <p>
 * Declared in the domain so business logic does not depend on JPA or any DB technology.
 */
public interface GreetingRepository {
    Greeting save(Greeting greeting);
    Optional<Greeting> findById(GreetingId id);
}
