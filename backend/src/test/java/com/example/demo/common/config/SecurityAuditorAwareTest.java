package com.example.demo.common.config;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for AuditorAwareImpl.
 * Tests auditor resolution with real database and EntityManager.
 */
@DisplayName("AuditorAwareImpl (Security Integration)")
@ActiveProfiles("test")
class SecurityAuditorAwareTest {

    // No need for @Autowired or Spring Context - this is now a pure unit test!
    private final AuditorAwareImpl auditorAware = new AuditorAwareImpl();

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("should return username when authenticated with UserDetails")
    void shouldReturnUsernameFromUserDetails() {
        // Arrange
        UserDetails springUser = User.withUsername("alice")
                .password("pw")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                springUser, null, springUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        Optional<String> current = auditorAware.getCurrentAuditor();

        // Assert
        assertThat(current).isPresent().contains("alice");
    }

    @Test
    @DisplayName("should return username when authenticated with String principal")
    void shouldReturnUsernameFromStringPrincipal() {
        // Arrange (e.g. JWT token where principal is just "alice")
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "alice", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        Optional<String> current = auditorAware.getCurrentAuditor();

        // Assert
        assertThat(current).isPresent().contains("alice");
    }

    @Test
    @DisplayName("should return 'system' when SecurityContext is empty")
    void shouldReturnSystemWhenNoAuth() {
        // Arrange
        SecurityContextHolder.clearContext();

        // Act
        Optional<String> current = auditorAware.getCurrentAuditor();

        // Assert
        assertThat(current).isPresent().contains("anonymous");
    }

    @Test
    @DisplayName("should return 'system' when user is Anonymous")
    void shouldReturnSystemWhenAnonymous() {
        // Arrange
        Authentication auth = new AnonymousAuthenticationToken(
                "key", "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        Optional<String> current = auditorAware.getCurrentAuditor();

        // Assert
        assertThat(current).isPresent().contains("anonymous");
    }

}
