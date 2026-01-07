package com.example.demo.testsupport;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * Security configuration for Keycloak integration tests.
 * <p>
 * This configuration is ONLY active when BOTH "test" AND "keycloak-test" profiles are active.
 * It enables method-level security (PreAuthorize, PostAuthorize, etc.) which is required
 * for testing endpoint protection.
 * <p>
 * Spring Addons handles the actual OAuth2/OIDC configuration:
 * <ul>
 *   <li>JWT validation against Keycloak</li>
 *   <li>Authority extraction from token claims</li>
 *   <li>URL-based authorization via permit-all configuration</li>
 * </ul>
 * <p>
 * Security Model:
 * <ul>
 *   <li>permit-all in properties allows requests through the filter chain (enables anonymous access)</li>
 *   <li>@PreAuthorize("isAuthenticated()") on controller methods enforces authentication</li>
 *   <li>Public endpoints (without @PreAuthorize) are accessible by anonymous users</li>
 *   <li>Protected endpoints (with @PreAuthorize) return 401 for anonymous users</li>
 * </ul>
 * <p>
 * This class enables method security so that @PreAuthorize annotations on controllers work.
 * Without this, the WebSecurityConfig (with @EnableMethodSecurity) is excluded by @Profile("!test").
 *
 * @see AbstractSecuredRestAssuredIT
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("test & keycloak-test")
public class KeycloakTestSecurityConfig {
    // Spring Addons auto-configures the SecurityFilterChain for OAuth2 resource server.
    // We only need to enable method security here since WebSecurityConfig (with @EnableMethodSecurity)
    // is excluded by @Profile("!test").
    //
    // The authorization model works as follows:
    // 1. permit-all paths (from properties) allow anonymous requests through the filter chain
    // 2. @PreAuthorize("isAuthenticated()") on controller methods blocks anonymous access
    // 3. Methods without @PreAuthorize are publicly accessible
}
