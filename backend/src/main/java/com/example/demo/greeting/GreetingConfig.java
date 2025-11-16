package com.example.demo.greeting;

import com.example.demo.greeting.application.GreetingService;
import com.example.demo.greeting.domain.GreetingRepository;
import com.example.demo.greeting.infrastructure.db.GreetingRepositoryAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Spring configuration class wiring together the greeting feature.
 * <p>
 * Exposes beans for:
 * <ul>
 *     <li>{@link GreetingService}</li>
 *     <li>{@link GreetingRepositoryAdapter}</li>
 *     <li>{@link java.time.Clock}</li>
 * </ul>
 * All domain objects are constructed here to keep the domain independent of Spring.
 */
@Configuration
public class GreetingConfig {

    @Bean
    public Clock clock() {
        // single source of time for the application
        return Clock.systemUTC();
    }

    @Bean
    public GreetingService greetingService(GreetingRepository greetingRepository, Clock clock) {
        // Spring injects GreetingRepositoryAdapter (your @Repository) here
        return new GreetingService(greetingRepository, clock);
    }
}
