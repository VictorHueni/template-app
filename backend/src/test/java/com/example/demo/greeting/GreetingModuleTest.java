package com.example.demo.greeting;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.greeting.event.GreetingCreatedEvent;
import com.example.demo.greeting.service.GreetingService;
import com.example.demo.testsupport.AbstractIntegrationTest;
import com.example.demo.testsupport.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the Greeting module event publishing functionality.
 *
 * <p>This test verifies that the greeting module correctly publishes domain
 * events when business operations occur, specifically testing the
 * {@link GreetingCreatedEvent} is published when a greeting is created.</p>
 *
 * <p><strong>Implementation Note:</strong> This test uses {@code @SpringBootTest}
 * instead of {@code @ApplicationModuleTest} due to a known compatibility issue
 * between Spring Modulith 2.x's {@code @ApplicationModuleTest} and Spring Boot's
 * Testcontainers integration. When using Testcontainers with PostgreSQL and
 * Flyway migrations, {@code @ApplicationModuleTest} causes schema validation
 * failures due to initialization order issues.</p>
 *
 * <p><strong>Database Schema Management:</strong>
 * This test relies on Spring Boot's auto-configuration to initialize Flyway
 * migrations and Hibernate schema validation. The {@code @SpringBootTest} annotation
 * ensures that Spring Boot's auto-configuration classes are loaded, including
 * Flyway's auto-configuration. The {@code @ContextConfiguration} adds our custom
 * {@link TestcontainersConfiguration} which provides the PostgreSQL datasource
 * via {@code @ServiceConnection}.</p>
 *
 * <p><strong>Related Issues:</strong>
 * <ul>
 *   <li><a href="https://github.com/spring-projects/spring-modulith/issues/272">
 *       Spring Modulith Issue #272: ApplicationModuleTest with Flyway and Testcontainer doesn't work</a></li>
 *   <li><a href="https://github.com/spring-projects/spring-modulith/issues/1067">
 *       Spring Modulith Issue #1067: Module-aware Flyway migrations support</a></li>
 * </ul>
 * </p>
 *
 * <p><strong>Error Details:</strong> When using {@code @ApplicationModuleTest} with
 * {@code @ContextConfiguration(classes = TestcontainersConfiguration.class)},
 * Spring's initialization order causes Hibernate to validate the database schema
 * before Flyway migrations have executed. This results in:
 * <pre>
 * org.hibernate.tool.schema.spi.SchemaManagementException: Schema validation: missing table [event_publication]
 * </pre>
 * </p>
 *
 * <p><strong>Workarounds Attempted:</strong>
 * <ol>
 *   <li>Using {@code @Import(TestcontainersConfiguration.class)} instead of
 *       {@code @ContextConfiguration} - did not resolve the issue</li>
 *   <li>Enabling {@code spring.modulith.runtime.flyway-enabled=true} for
 *       module-aware Flyway migrations - did not fully resolve the issue</li>
 *   <li>Using {@code @SpringBootTest} with webEnvironment=RANDOM_PORT - had web server
 *       configuration issues</li>
 *   <li>Using {@code @SpringBootTest} with webEnvironment=NONE - allows Spring Boot
 *       auto-configuration to properly load Flyway</li>
 * </ol>
 * </p>
 *
 * <p><strong>Future Consideration:</strong> Once Spring Modulith 2.1+ or a
 * future version resolves the {@code @ApplicationModuleTest} initialization
 * ordering issue with Testcontainers, this test can be migrated back to
 * {@code @ApplicationModuleTest} for module-level testing benefits.</p>
 *
 * @see GreetingCreatedEvent
 * @see GreetingService
 * @see TestcontainersConfiguration
 * @see <a href="https://docs.spring.io/spring-modulith/reference/fundamentals/testing.html">
 *      Spring Modulith Testing Documentation</a>
 */
@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
@ActiveProfiles({"test", "integration"})
@Transactional
class GreetingModuleTest extends AbstractIntegrationTest {

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
