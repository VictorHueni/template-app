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
        // Clean up SecurityContext after each test
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("newRevision")
    class NewRevision {

        @Test
        @DisplayName("should set username from authentication")
        void shouldSetUsernameFromAuthentication() {
            // Arrange
            String username = "testuser";
            setAuthenticatedUser(username);

            // Act
            listener.newRevision(revisionEntity);

            // Assert
            assertThat(revisionEntity.getUsername()).isEqualTo(username);
        }

        @Test
        @DisplayName("should set system when no authentication")
        void shouldSetSystemWhenNoAuthentication() {
            // Arrange - no authentication set (SecurityContext is empty)
            SecurityContextHolder.clearContext();

            // Act
            listener.newRevision(revisionEntity);

            // Assert
            assertThat(revisionEntity.getUsername()).isEqualTo("system");
        }

        @Test
        @DisplayName("should set system when anonymous user")
        void shouldSetSystemWhenAnonymousUser() {
            // Arrange
            setAnonymousUser();

            // Act
            listener.newRevision(revisionEntity);

            // Assert
            assertThat(revisionEntity.getUsername()).isEqualTo("system");
        }

        @Test
        @DisplayName("should set system when authentication is not authenticated")
        void shouldSetSystemWhenAuthenticationIsNotAuthenticated() {
            // Arrange
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(false);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(securityContext);

            // Act
            listener.newRevision(revisionEntity);

            // Assert
            assertThat(revisionEntity.getUsername()).isEqualTo("system");
        }

        @Test
        @DisplayName("should handle null authentication name")
        void shouldHandleNullAuthenticationName() {
            // Arrange
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(true);
            when(auth.getName()).thenReturn(null);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(securityContext);

            // Act
            listener.newRevision(revisionEntity);

            // Assert
            assertThat(revisionEntity.getUsername()).isEqualTo("system");
        }
    }

    @Nested
    @DisplayName("getCurrentUsername")
    class GetCurrentUsername {

        @Test
        @DisplayName("should return username from security context")
        void shouldReturnUsernameFromSecurityContext() {
            // Arrange
            String expectedUsername = "john.doe";
            setAuthenticatedUser(expectedUsername);

            // Act
            listener.newRevision(revisionEntity);

            // Assert
            assertThat(revisionEntity.getUsername()).isEqualTo(expectedUsername);
        }

        @Test
        @DisplayName("should return system when no authentication")
        void shouldReturnSystemWhenNoAuthentication() {
            // Arrange
            SecurityContextHolder.clearContext();

            // Act
            listener.newRevision(revisionEntity);

            // Assert
            assertThat(revisionEntity.getUsername()).isEqualTo("system");
        }

        @Test
        @DisplayName("should return system when authentication is null")
        void shouldReturnSystemWhenAuthenticationIsNull() {
            // Arrange
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(null);
            SecurityContextHolder.setContext(securityContext);

            // Act
            listener.newRevision(revisionEntity);

            // Assert
            assertThat(revisionEntity.getUsername()).isEqualTo("system");
        }

        @Test
        @DisplayName("should return system when principal is anonymous")
        void shouldReturnSystemWhenPrincipalIsAnonymous() {
            // Arrange
            setAnonymousUser();

            // Act
            listener.newRevision(revisionEntity);

            // Assert
            assertThat(revisionEntity.getUsername()).isEqualTo("system");
        }

        @Test
        @DisplayName("should return system when authentication not authenticated")
        void shouldReturnSystemWhenAuthenticationNotAuthenticated() {
            // Arrange
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(false);
            when(auth.getName()).thenReturn("someuser");
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(securityContext);

            // Act
            listener.newRevision(revisionEntity);

            // Assert
            assertThat(revisionEntity.getUsername()).isEqualTo("system");
        }

        @Test
        @DisplayName("should handle different authenticated usernames")
        void shouldHandleDifferentAuthenticatedUsernames() {
            // Arrange & Act & Assert for first user
            setAuthenticatedUser("user1");
            listener.newRevision(revisionEntity);
            assertThat(revisionEntity.getUsername()).isEqualTo("user1");

            // Reset and test second user
            revisionEntity = new CustomRevisionEntity();
            setAuthenticatedUser("admin");
            listener.newRevision(revisionEntity);
            assertThat(revisionEntity.getUsername()).isEqualTo("admin");
        }

        @Test
        @DisplayName("should return system when getName returns null")
        void shouldReturnSystemWhenGetNameReturnsNull() {
            // Arrange
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(true);
            when(auth.getName()).thenReturn(null);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(securityContext);

            // Act
            listener.newRevision(revisionEntity);

            // Assert
            assertThat(revisionEntity.getUsername()).isEqualTo("system");
        }
    }

    @Nested
    @DisplayName("integration scenarios")
    class IntegrationScenarios {

        @Test
        @DisplayName("should handle batch job scenario with no authentication")
        void shouldHandleBatchJobScenario() {
            // Arrange - batch job has no security context
            SecurityContextHolder.clearContext();

            // Act
            listener.newRevision(revisionEntity);

            // Assert
            assertThat(revisionEntity.getUsername()).isEqualTo("system");
        }

        @Test
        @DisplayName("should handle application startup scenario")
        void shouldHandleApplicationStartupScenario() {
            // Arrange - application startup might have no authentication
            SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
            SecurityContextHolder.setContext(emptyContext);

            // Act
            listener.newRevision(revisionEntity);

            // Assert
            assertThat(revisionEntity.getUsername()).isEqualTo("system");
        }

        @Test
        @DisplayName("should handle authenticated user making changes")
        void shouldHandleAuthenticatedUserMakingChanges() {
            // Arrange
            String authenticatedUser = "authenticated.user";
            setAuthenticatedUser(authenticatedUser);

            // Act
            listener.newRevision(revisionEntity);

            // Assert
            assertThat(revisionEntity.getUsername()).isEqualTo(authenticatedUser);
        }
    }

    // Helper methods

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
                "key", "anonymous", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);
    }
}
