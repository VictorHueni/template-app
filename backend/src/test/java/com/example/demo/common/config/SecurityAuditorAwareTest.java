package com.example.demo.common.config;

import com.example.demo.testsupport.AbstractIntegrationTest;
import com.example.demo.user.domain.UserDetailsImpl;
import com.example.demo.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for AuditorAwareImpl.
 * Tests auditor resolution with real database and EntityManager.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class SecurityAuditorAwareTest extends AbstractIntegrationTest {

    @Autowired
    private AuditorAware<UserDetailsImpl> auditorAware;

    @Autowired
    private UserRepository userRepository;

    private UserDetailsImpl testUser;
    private UserDetailsImpl systemUser;

    @BeforeEach
    void setup() {
        // Ensure system user exists
        systemUser = userRepository.findByUsername("system")
                .orElseGet(() -> userRepository.saveAndFlush(new UserDetailsImpl("system", "encoded-pw")));

        // Create test user
        testUser = userRepository.findByUsername("alice")
                .orElseGet(() -> userRepository.saveAndFlush(new UserDetailsImpl("alice", "encoded-pw")));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsUserWhenAuthenticatedWithUserDetailsPrincipal() {
        // Arrange: Set up authentication with Spring Security UserDetails
        UserDetails springUser = User.withUsername("alice")
                .password("pw")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                springUser, springUser.getPassword(), springUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        Optional<UserDetailsImpl> current = auditorAware.getCurrentAuditor();

        // Assert
        assertThat(current).isPresent();
        assertThat(current.get().getUsername()).isEqualTo("alice");
    }

    @Test
    void returnsUserWhenAuthenticatedWithStringPrincipal() {
        // Arrange: Set up authentication with String principal (e.g., from JWT)
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "alice", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        Optional<UserDetailsImpl> current = auditorAware.getCurrentAuditor();

        // Assert
        assertThat(current).isPresent();
        assertThat(current.get().getUsername()).isEqualTo("alice");
    }

    @Test
    void returnsUserDirectlyWhenPrincipalIsDomainEntity() {
        // Arrange: Principal is already our domain UserDetailsImpl
        Authentication auth = new UsernamePasswordAuthenticationToken(
                testUser, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        Optional<UserDetailsImpl> current = auditorAware.getCurrentAuditor();

        // Assert: Should return the same instance (no DB lookup)
        assertThat(current).isPresent();
        assertThat(current.get()).isSameAs(testUser);
    }

    @Test
    void returnsSystemUserWhenNoAuthentication() {
        // Arrange: No authentication set (SecurityContext is empty)

        // Act
        Optional<UserDetailsImpl> current = auditorAware.getCurrentAuditor();

        // Assert
        assertThat(current).isPresent();
        assertThat(current.get().getUsername()).isEqualTo("system");
    }

    @Test
    void returnsSystemUserWhenAuthenticationIsNull() {
        // Arrange: Explicitly set null authentication
        SecurityContextHolder.getContext().setAuthentication(null);

        // Act
        Optional<UserDetailsImpl> current = auditorAware.getCurrentAuditor();

        // Assert
        assertThat(current).isPresent();
        assertThat(current.get().getUsername()).isEqualTo("system");
    }

    @Test
    void returnsSystemUserWhenAnonymousAuthentication() {
        // Arrange: Set up anonymous authentication
        Authentication auth = new org.springframework.security.authentication.AnonymousAuthenticationToken(
                "key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        Optional<UserDetailsImpl> current = auditorAware.getCurrentAuditor();

        // Assert
        assertThat(current).isPresent();
        assertThat(current.get().getUsername()).isEqualTo("system");
    }

    @Test
    void returnsSystemUserWhenNotAuthenticated() {
        // Arrange: Create authentication that is not authenticated
        Authentication auth = new UsernamePasswordAuthenticationToken("user", "pass");
        // Note: UsernamePasswordAuthenticationToken without authorities is not authenticated
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        Optional<UserDetailsImpl> current = auditorAware.getCurrentAuditor();

        // Assert
        assertThat(current).isPresent();
        assertThat(current.get().getUsername()).isEqualTo("system");
    }

    @Test
    void handlesUsernameExtractionFromUnknownPrincipalType() {
        // Arrange: Use an unusual principal type (neither UserDetails, String, nor UserDetailsImpl)
        Object customPrincipal = new Object() {
            @Override
            public String toString() {
                return "customPrincipal";
            }
        };
        Authentication auth = new UsernamePasswordAuthenticationToken(
                customPrincipal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))) {
            @Override
            public String getName() {
                return "alice";
            }
        };
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        Optional<UserDetailsImpl> current = auditorAware.getCurrentAuditor();

        // Assert: Should fall back to authentication.getName() and find user
        assertThat(current).isPresent();
        assertThat(current.get().getUsername()).isEqualTo("alice");
    }

    @Test
    void returnsSystemUserWhenUserNotFoundInRepository() {
        // Arrange: Authenticate with a user that doesn't exist in repository
        UserDetails springUser = User.withUsername("nonexistent")
                .password("pw")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                springUser, springUser.getPassword(), springUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        Optional<UserDetailsImpl> current = auditorAware.getCurrentAuditor();

        // Assert: Should fall back to system user when lookup fails
        assertThat(current).isPresent();
        assertThat(current.get().getUsername()).isEqualTo("system");
    }
}
