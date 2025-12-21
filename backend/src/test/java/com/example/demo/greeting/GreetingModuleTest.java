package com.example.demo.greeting;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.greeting.event.GreetingCreatedEvent;
import com.example.demo.greeting.service.GreetingService;
import com.example.demo.testsupport.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module test for the Greeting module.
 *
 * <p>This test verifies that the greeting module correctly publishes domain
 * events when business operations occur.</p>
 */
@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
@ContextConfiguration(classes = TestcontainersConfiguration.class)
@Transactional
class GreetingModuleTest {

    @Autowired
    private GreetingService greetingService;

    @Test
    @DisplayName("publishes GreetingCreatedEvent when greeting is created")
    @WithMockUser(username = "testuser")
    void publishesGreetingCreatedEventWhenGreetingIsCreated(AssertablePublishedEvents events) {
        // When: a greeting is created
        var greeting = greetingService.createGreeting("Hello, World!", "Alice");

        // Then: a GreetingCreatedEvent is published
        assertThat(events.ofType(GreetingCreatedEvent.class))
                .hasSize(1)
                .element(0)
                .satisfies(event -> {
                    assertThat(event.greetingId()).isEqualTo(greeting.getId());
                    assertThat(event.reference()).isEqualTo(greeting.getReference());
                    assertThat(event.recipient()).isEqualTo("Alice");
                    assertThat(event.message()).isEqualTo("Hello, World!");
                    assertThat(event.createdBy()).isEqualTo("testuser");
                    assertThat(event.createdAt()).isNotNull();
                });
    }
}
