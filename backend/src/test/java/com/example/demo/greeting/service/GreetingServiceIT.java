package com.example.demo.greeting.service;


import com.example.demo.greeting.model.Greeting;
import com.example.demo.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class GreetingServiceIT extends AbstractIntegrationTest {
    @Autowired
    private GreetingService greetingService;

    @Test
    void createsGreetingWithGeneratedIds() {
        // When - Create greeting through service (which generates functional ID)
        Greeting greeting = greetingService.createGreeting("Hello Integration", "TestContainer");

        // Then - Verify both IDs were generated
        assertThat(greeting.getId()).isNotNull(); // TSID
        assertThat(greeting.getReference()).isNotNull(); // Functional ID from sequence
        assertThat(greeting.getReference()).matches("GRE-\\d{4}-\\d{6}");
        assertThat(greeting.getMessage()).isEqualTo("Hello Integration");
        assertThat(greeting.getRecipient()).isEqualTo("TestContainer");
    }

}
