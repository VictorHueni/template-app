package com.example.demo.greeting;

import com.example.demo.greeting.repository.GreetingRepository;
import com.example.demo.greeting.service.GreetingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

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
