package com.example.demo.user.controller;

import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.api.v1.controller.UserApi;
import com.example.demo.api.v1.model.UserInfoResponse;

/**
 * REST endpoint that returns current user information.
 * Implements the generated UserApi interface from OpenAPI spec.
 *
 * <p>This controller abstracts over IdP-specific JWT claim formats
 * to provide a stable API contract regardless of the identity provider used.
 *
 * @see api/specification/openapi.yaml - User tag endpoints
 */
@RestController
public class UserController implements UserApi {

    // Application-defined roles (mapped from IdP roles)
    private static final Set<String> KNOWN_ROLES = Set.of("USER", "ADMIN");

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserInfoResponse> getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            // Should not happen with proper security config
            return ResponseEntity.status(401).build();
        }

        // Extract claims from JWT
        final var claims = jwtAuth.getTokenAttributes();

        // Map 'sub' claim to id (IdP-agnostic identifier)
        final var id = (String) claims.get("sub");

        // Map 'preferred_username' to username
        final var username = (String) claims.getOrDefault(
                StandardClaimNames.PREFERRED_USERNAME,
                auth.getName()
        );

        // Email is optional (might not be provided by IdP)
        final var email = (String) claims.get(StandardClaimNames.EMAIL);

        // Map IdP roles to application roles (filter to known roles only)
        final var roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.replace("ROLE_", ""))  // Remove Spring Security prefix
                .filter(KNOWN_ROLES::contains)           // Only known app roles
                .map(UserInfoResponse.RolesEnum::fromValue)
                .toList();

        var response = new UserInfoResponse()
                .id(id)
                .username(username)
                .email(email)
                .roles(roles);

        return ResponseEntity.ok(response);
    }
}
