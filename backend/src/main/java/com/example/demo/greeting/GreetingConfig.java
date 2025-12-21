package com.example.demo.greeting;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GreetingConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
