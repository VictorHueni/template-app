/**
 * Greeting module - Domain module for greeting management.
 *
 * <p>This module handles the greeting domain and publishes events when
 * greetings are created, updated, or deleted.</p>
 *
 * <p>Dependencies:</p>
 * <ul>
 *   <li><strong>common</strong>: Shared infrastructure (AbstractBaseEntity, FunctionalIdGenerator)</li>
 *   <li><strong>api</strong>: OpenAPI generated controller interfaces and DTOs</li>
 * </ul>
 *
 * <p>Published Events:</p>
 * <ul>
 *   <li>{@link com.example.demo.greeting.event.GreetingCreatedEvent}</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"common", "api"}
)
package com.example.demo.greeting;
