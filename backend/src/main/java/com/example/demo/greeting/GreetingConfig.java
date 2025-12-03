package com.example.demo.greeting;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class GreetingConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
