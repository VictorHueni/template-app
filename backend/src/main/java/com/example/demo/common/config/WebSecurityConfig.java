package com.example.demo.common.config;

import java.util.Arrays;
import java.util.List;

import jakarta.servlet.DispatcherType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Production security configuration.
 * <p>
 * This configuration is NOT active during tests (profile "test" is excluded).
 * For test security configuration, see TestSecurityConfig in testsupport package.
 */
@Configuration
@EnableWebSecurity
@Profile("!test")
public class WebSecurityConfig {

    @Value("${cors.allowed.origins:http://localhost:5173}")
    private String corsAllowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests((requests) -> requests

                                // 1. Allow Error Dispatches (Fixes missing headers on 404 pages)
                                .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()

                                // 2. Allow access to the /error endpoint itself
                                .requestMatchers("/error").permitAll()

                                // 3. PUBLIC Actuator Endpoints
                                .requestMatchers(EndpointRequest.to("health")).permitAll()

                                // 4. DENY/AUTHENTICATE ALL OTHER Actuator Endpoints
                                .requestMatchers(EndpointRequest.toAnyEndpoint()).authenticated()

                                // 5. Application Endpoints
                                .requestMatchers("/", "/v1/greetings", "/v1/greetings/**").permitAll()
                                .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                // Enable HTTP Basic Auth for actuator endpoints (machine-to-machine access)
                .httpBasic(basic -> basic
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(LogoutConfigurer::permitAll)
                // Security headers configuration (fixes ZAP DAST scan warnings)
                .headers(headers -> {
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

        return http.build();
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

