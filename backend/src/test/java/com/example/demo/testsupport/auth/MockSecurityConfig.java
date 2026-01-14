package com.example.demo.testsupport.auth;

import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Integration-test-only security configuration.
 *
 * <p>Replaces Keycloak by validating locally minted HS256 JWTs and mapping claims exactly like production:
 * <ul>
 *   <li>username: {@code preferred_username}</li>
 *   <li>roles: {@code realm_access.roles}</li>
 * </ul>
 *
 * <p>Also configures CORS to match production behavior for security integration tests.
 */
@TestConfiguration
@Profile("integration")
@Import({MockJwtBeansConfig.class, MockMethodSecurityConfig.class})
public class MockSecurityConfig {

    /**
     * Default allowed origin for CORS tests.
     * Matches the frontend development server.
     */
    public static final String TEST_ALLOWED_ORIGIN = "http://localhost:5173";

    @Bean
    public SecurityFilterChain integrationSecurityFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests((requests) -> requests
                        // Public endpoints (matches OpenAPI security: [])
                        .requestMatchers(HttpMethod.GET, "/api/v1/greetings", "/api/v1/greetings/**").permitAll()

                        // Protected API endpoints: enforce authentication at filter-chain level.
                        // This ensures missing/invalid tokens yield 401 (not 403).
                        .requestMatchers("/api/**").authenticated()

                        // Default: allow other non-API requests (e.g., error pages) during tests
                        .anyRequest().permitAll()
                )
                .oauth2ResourceServer((oauth2) -> oauth2
                        .jwt((jwt) -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)
                        )
                );

        return http.build();
    }

    /**
     * CORS configuration source for integration tests.
     * Mirrors production WebSecurityConfig CORS settings.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(TEST_ALLOWED_ORIGIN));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Explicitly list required headers to prevent header injection attacks
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-XSRF-TOKEN",
                "Accept",
                "Origin"
        ));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
