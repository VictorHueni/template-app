package com.example.demo.audit;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;

import com.example.demo.common.config.AuditorAwareImpl;

import io.micrometer.tracing.Tracer;

/**
 * Test configuration for the Audit module tests.
 *
 * <p>Provides infrastructure beans that are normally provided by shared modules
 * but are not automatically loaded in module tests.</p>
 */
@TestConfiguration
public class AuditModuleTestConfiguration {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl();
    }

    @Bean
    public Tracer tracer() {
        return Tracer.NOOP;
    }
}
