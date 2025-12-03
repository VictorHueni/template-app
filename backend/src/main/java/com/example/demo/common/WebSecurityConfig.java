package com.example.demo.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

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

    @Value("${admin.username:user}")
    private String adminUsername;

    @Value("${admin.password:password}")
    private String adminPassword;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests((requests) -> requests

                        // 1. PUBLIC Actuator Endpoints (Explicitly allow 'health' and 'info')
                        .requestMatchers(EndpointRequest.to("health")).permitAll()

                        // 2. DENY/AUTHENTICATE ALL OTHER Actuator Endpoints
                        .requestMatchers(EndpointRequest.toAnyEndpoint()).authenticated()

                        // 3. Application Endpoints
                        .requestMatchers("/", "/v1/greetings", "/v1/greetings/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .formLogin(AbstractHttpConfigurer::disable /*(form) -> form
                        .loginPage("/login")
                        .permitAll()*/
                )
                .logout((logout) -> logout.permitAll());

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

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails user = User.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .roles("USER", "ADMIN")
                .build();

        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Use BCrypt for production
        return new BCryptPasswordEncoder();
    }
}

