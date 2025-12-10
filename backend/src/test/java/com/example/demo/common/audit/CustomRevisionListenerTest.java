package com.example.demo.common.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("CustomRevisionListener")
class CustomRevisionListenerTest {

    private CustomRevisionListener listener;
    private CustomRevisionEntity revisionEntity;

    @BeforeEach
    void setUp() {
        listener = new CustomRevisionListener();
        revisionEntity = new CustomRevisionEntity();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Logic Tests")
    class LogicTests {

        @Test
        @DisplayName("should set 'system' when Context is empty (Batch Job)")
        void shouldSetSystemWhenNoAuthentication() {
            // Arrange
            SecurityContextHolder.clearContext();

            // Act
            listener.newRevision(revisionEntity);

            // Assert
            assertThat(revisionEntity.getUsername()).isEqualTo("system");
        }

        @Test
        @DisplayName("should set 'anonymous' when user is Anonymous (Public API)")
        void shouldSetAnonymousWhenAnonymousUser() {
            // Arrange
            setAnonymousUser();

            // Act
            listener.newRevision(revisionEntity);

            // Assert
            assertThat(revisionEntity.getUsername()).isEqualTo("anonymous");
        }

        @Test
        @DisplayName("sShould set username from Authenticated User")
        void shouldSetUsernameFromAuthentication() {
            // Arrange
            setAuthenticatedUser("alice");

            // Act
            listener.newRevision(revisionEntity);

            // Assert
            assertThat(revisionEntity.getUsername()).isEqualTo("alice");
        }

        @Test
        @DisplayName("Edge Case: Should set 'unknown' when Authenticated but Name is Null")
        void shouldSetUnknownWhenNameIsNull() {
            // Arrange
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(true);
            when(auth.getName()).thenReturn(null); // Null name scenario

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(securityContext);

            // Act
            listener.newRevision(revisionEntity);

            // Assert
            assertThat(revisionEntity.getUsername()).isEqualTo("unknown");
        }

        @Test
        @DisplayName("Edge Case: Should set 'unknown' when not authenticated and not AnonymousToken")
        void shouldSetUnknownWhenNotAuthenticatedAndNotAnonymous() {
            // Arrange: A generic Authentication token that is NOT authenticated
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(false);

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(securityContext);

            // Act
            listener.newRevision(revisionEntity);

            // Assert
            assertThat(revisionEntity.getUsername()).isEqualTo("unknown");
        }
    }

    @Nested
    @DisplayName("Integration Scenarios")
    class IntegrationScenarios {

        @Test
        @DisplayName("Batch Job: No Security Context -> system")
        void batchJobScenario() {
            SecurityContextHolder.clearContext();
            listener.newRevision(revisionEntity);
            assertThat(revisionEntity.getUsername()).isEqualTo("system");
        }

        @Test
        @DisplayName("Startup: Empty Context -> system")
        void startupScenario() {
            SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
            listener.newRevision(revisionEntity);
            assertThat(revisionEntity.getUsername()).isEqualTo("system");
        }
    }

    // --- Helpers ---

    private void setAuthenticatedUser(String username) {
        UserDetails userDetails = User.builder()
                .username(username)
                .password("password")
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    private void setAnonymousUser() {
        Authentication auth = new AnonymousAuthenticationToken(
                "key", "anonymousUser", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);
    }
}