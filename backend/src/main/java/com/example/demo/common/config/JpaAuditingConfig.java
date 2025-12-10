package com.example.demo.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.envers.repository.config.EnableEnversRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration for JPA auditing and Hibernate Envers.
 *
 * <p>Enables:</p>
 * <ul>
 *   <li>JPA Auditing - automatic population of @CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy</li>
 *   <li>Envers Repositories - RevisionRepository support for querying audit history</li>
 * </ul>
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableEnversRepositories(basePackages = "com.example.demo")
public class JpaAuditingConfig {

    // 1. REMOVED: UserRepository dependency (no longer needed)

    // 2. CHANGED: Return type is now AuditorAware<String>
    @Bean
    public AuditorAware<String> auditorProvider() {
        // 3. CHANGED: No arguments passed to the constructor
        return new AuditorAwareImpl();
    }
}
