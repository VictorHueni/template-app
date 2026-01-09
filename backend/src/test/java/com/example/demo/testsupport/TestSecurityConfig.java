package com.example.demo.testsupport;

import java.util.List;

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.demo.common.config.WebSecurityConfig;

@Configuration
@EnableWebSecurity
@Profile("test & !integration")
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for easier REST API testing
                .csrf(AbstractHttpConfigurer::disable)

                // Disable form login (not needed for API testing)
                .formLogin(AbstractHttpConfigurer::disable)

                // Disable HTTP Basic (tests don't need authentication)
                .httpBasic(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests((requests) -> requests
                        // Allow health check endpoint
                        .requestMatchers(EndpointRequest.to("health")).permitAll()

                        // All API endpoints are public in test mode
                        .requestMatchers("/api/**").permitAll()

                        // Allow root path
                        .requestMatchers("/").permitAll()

                        // Allow all other requests (for Swagger UI, etc.)
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource testCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow all localhost origins for testing
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

