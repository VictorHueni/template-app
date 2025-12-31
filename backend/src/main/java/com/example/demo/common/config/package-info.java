/**
 * Application-wide configuration.
 *
 * <p><strong>INTERNAL:</strong></p>
 * <ul>
 *   <li>SecurityConfig, WebSecurityConfig - Spring Security setup</li>
 *   <li>JpaAuditingConfig - JPA auditing configuration</li>
 *   <li>WebApiConfig - Web MVC configuration</li>
 *   <li>AuditorAwareImpl - Current user provider for auditing</li>
 * </ul>
 *
 * <p><strong>Used by:</strong> Spring Boot auto-configuration</p>
 *
 * <p>This package contains application-wide Spring configuration beans that are
 * automatically discovered by Spring Boot's component scanning. These configurations
 * set up cross-cutting concerns like security, web, and auditing.</p>
 *
 * <p><strong>Note:</strong> This is internal infrastructure - modules should not
 * directly interact with these classes. Spring discovers and applies them automatically.</p>
 *
 * @since 1.0.0
 */
package com.example.demo.common.config;
