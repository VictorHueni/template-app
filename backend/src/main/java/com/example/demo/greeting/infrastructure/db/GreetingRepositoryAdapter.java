package com.example.demo.greeting.infrastructure.db;

import com.example.demo.greeting.domain.Greeting;
import com.example.demo.greeting.domain.GreetingId;
import com.example.demo.greeting.domain.GreetingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Infrastructure adapter implementing the domain {@link GreetingRepository} port.
 * <p>
 * Bridges the domain model with the persistence layer:
 * handles entity mapping and delegates to {@link SpringDataGreetingJpaRepo}.
 * <p>
 * Tested via integration tests using Testcontainers Postgres.
 */
@Repository
public class GreetingRepositoryAdapter implements GreetingRepository {

    private final SpringDataGreetingJpaRepo jpaRepo;

    public GreetingRepositoryAdapter(SpringDataGreetingJpaRepo jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public Greeting save(Greeting greeting) {
        GreetingJpaEntity entity = new GreetingJpaEntity(
                greeting.id().value(),
                greeting.recipient(),
                greeting.message(),
                greeting.createdAt()
        );
        GreetingJpaEntity saved = jpaRepo.save(entity);
        return new Greeting(
                new GreetingId(saved.getId()),
                saved.getRecipient(),
                saved.getMessage(),
                saved.getCreatedAt()
        );
    }

    @Override
    public Optional<Greeting> findById(GreetingId id) {
        return jpaRepo.findById(id.value())
                .map(e -> new Greeting(
                        new GreetingId(e.getId()),
                        e.getRecipient(),
                        e.getMessage(),
                        e.getCreatedAt()
                ));
    }
}
