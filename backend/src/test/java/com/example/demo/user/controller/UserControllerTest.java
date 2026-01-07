package com.example.demo.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.example.demo.api.v1.model.UserInfoResponse;
import com.example.demo.api.v1.model.UserInfoResponse.RolesEnum;

/**
 * Unit tests for UserController.
 *
 * <p><strong>Test Scope:</strong></p>
 * <ul>
 *   <li>Tests controller business logic in isolation</li>
 *   <li>Uses mocked SecurityContext to simulate JWT authentication</li>
 *   <li>Verifies correct mapping from JWT claims to UserInfoResponse</li>
 * </ul>
 *
 * <p><strong>Note:</strong></p>
 * <p>
 * Security enforcement (401/403 responses) is tested in {@link UserControllerSecuredIT}
 * using real Keycloak tokens. This unit test focuses on the controller's logic
 * when an authenticated user is present.
 * </p>
 *
 * @see UserController
 * @see UserControllerSecuredIT
 */
class UserControllerTest {

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GET /api/v1/me - Current User Info")
    class GetCurrentUser {

        @Test
        @DisplayName("should return user info with USER role")
        void shouldReturnUserInfoWithUserRole() {
            // Given
            mockSecurityContext(
                    "f47ac10b-58cc-4372-a567-0e02b2c3d479",
                    "testuser",
                    "test@example.com",
                    List.of("USER")
            );

            // When
            ResponseEntity<UserInfoResponse> response = controller.getCurrentUser();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            UserInfoResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getId()).isEqualTo("f47ac10b-58cc-4372-a567-0e02b2c3d479");
            assertThat(body.getUsername()).isEqualTo("testuser");
            assertThat(body.getEmail()).isEqualTo("test@example.com");
            assertThat(body.getRoles()).containsExactly(RolesEnum.USER);
        }

        @Test
        @DisplayName("should return user info with multiple roles")
        void shouldReturnUserInfoWithMultipleRoles() {
            // Given
            mockSecurityContext(
                    "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                    "adminuser",
                    "admin@example.com",
                    List.of("USER", "ADMIN")
            );

            // When
            ResponseEntity<UserInfoResponse> response = controller.getCurrentUser();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            UserInfoResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getId()).isEqualTo("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
            assertThat(body.getUsername()).isEqualTo("adminuser");
            assertThat(body.getEmail()).isEqualTo("admin@example.com");
            assertThat(body.getRoles()).containsExactlyInAnyOrder(RolesEnum.USER, RolesEnum.ADMIN);
        }

        @Test
        @DisplayName("should handle missing email gracefully")
        void shouldHandleMissingEmail() {
            // Given
            mockSecurityContext(
                    "user-without-email",
                    "noemaileruser",
                    null,
                    List.of("USER")
            );

            // When
            ResponseEntity<UserInfoResponse> response = controller.getCurrentUser();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            UserInfoResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getEmail()).isNull();
        }

        @Test
        @DisplayName("should handle unknown roles by filtering them out")
        void shouldHandleUnknownRoles() {
            // Given - MODERATOR is not a known AppRole, should be filtered out
            mockSecurityContext(
                    "user-with-unknown-role",
                    "moderator",
                    "mod@example.com",
                    List.of("USER", "MODERATOR", "UNKNOWN")
            );

            // When
            ResponseEntity<UserInfoResponse> response = controller.getCurrentUser();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            UserInfoResponse body = response.getBody();
            assertThat(body).isNotNull();
            // Only USER should be included (MODERATOR and UNKNOWN are filtered)
            assertThat(body.getRoles()).containsExactly(RolesEnum.USER);
        }
    }

    /**
     * Creates a mock SecurityContext with JWT authentication.
     *
     * @param sub           the subject claim (user ID)
     * @param username      the preferred_username claim
     * @param email         the email claim (can be null)
     * @param realmRoles    the roles from realm_access.roles (converted to ROLE_ authorities)
     */
    private void mockSecurityContext(String sub, String username, String email, List<String> realmRoles) {
        // Build claims map for token attributes
        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("sub", sub);
        claims.put("preferred_username", username);
        if (email != null) {
            claims.put("email", email);
        }
        claims.put("realm_access", Map.of("roles", realmRoles));

        // Build mock JWT with claims
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(sub);
        when(jwt.getClaimAsString("preferred_username")).thenReturn(username);
        when(jwt.getClaimAsString("email")).thenReturn(email);
        when(jwt.getClaims()).thenReturn(claims);

        // Create authorities from roles (with ROLE_ prefix)
        var authorities = realmRoles.stream()
                .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))
                .map(auth -> (org.springframework.security.core.GrantedAuthority) auth)
                .toList();

        // Create JWT authentication token with authorities
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities);

        // Set up SecurityContext
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
