package com.example.demo.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration for JPA auditing.
 * Enables automatic population of @CreatedDate and @LastModifiedDate fields
 * in entities that extend AbstractBaseEntity.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfiguration {
}
