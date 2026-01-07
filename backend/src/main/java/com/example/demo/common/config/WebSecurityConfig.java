package com.example.demo.common.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.c4_soft.springaddons.security.oidc.starter.synchronised.resourceserver.ResourceServerSynchronizedHttpSecurityPostProcessor;

/**
 * Security configuration for OAuth2 Resource Server.
 * <p>
 * Spring Addons handles:
 * - JWT validation against Keycloak (issuer, signature)
 * - Authorities extraction from realm_access.roles
 * - Stateless session management
 * - Public endpoint access (configured in application.properties)
 * <p>
 * This config adds:
 * - Security headers (CSP, Permissions-Policy, X-Frame-Options) - ZAP DAST fixes
 * - CORS configuration for frontend access
 * - Method-level security via @EnableMethodSecurity
 * <p>
 * For test security configuration, see TestSecurityConfig in testsupport package.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("!test")
public class WebSecurityConfig {

    @Value("${cors.allowed.origins:http://localhost:5173}")
    private String corsAllowedOrigins;

    /**
     * Customizes the Spring Addons auto-configured SecurityFilterChain.
     * Adds security headers that were configured for ZAP DAST compliance.
     */
    @Bean
    ResourceServerSynchronizedHttpSecurityPostProcessor securityHeadersPostProcessor() {
        return (HttpSecurity http) -> {
            try {
                http.headers(headers -> {
                    // Content-Security-Policy for API-only backend (fixes ZAP 10038)
                    headers.contentSecurityPolicy(csp -> csp
                            .policyDirectives("default-src 'none'; frame-ancestors 'none'")
                    );
                    // Permissions-Policy (fixes ZAP 10063)
                    headers.permissionsPolicyHeader(permissions -> permissions
                            .policy("camera=(), microphone=(), geolocation=(), payment=(), usb=()")
                    );
                    // Cache-Control to prevent caching sensitive responses (fixes ZAP 10049)
                    headers.cacheControl(Customizer.withDefaults());
                    // X-Frame-Options: DENY
                    headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny);
                    // X-Content-Type-Options: nosniff
                    headers.contentTypeOptions(Customizer.withDefaults());
                });
                return http;
            }
            catch (Exception e) {
                throw new RuntimeException("Failed to configure security headers", e);
            }
        };
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow frontend origins from configuration (comma-separated)
        configuration.setAllowedOrigins(Arrays.asList(corsAllowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

