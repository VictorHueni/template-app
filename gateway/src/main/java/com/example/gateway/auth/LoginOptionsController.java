package com.example.gateway.auth;

import java.util.List;

import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.c4_soft.springaddons.security.oidc.starter.properties.SpringAddonsOidcProperties;

/**
 * REST endpoint that returns available OAuth2 login providers.
 *
 * <p>The frontend calls this to discover login URLs dynamically,
 * making it easy to add more providers (Google, GitHub, etc.) later.</p>
 */
@RestController
public class LoginOptionsController {

    private final List<LoginOptionDto> loginOptions;

    public LoginOptionsController(
            OAuth2ClientProperties clientProps,
            SpringAddonsOidcProperties addonsProperties) {

        // Get client URI from spring-addons config
        final var clientUri = addonsProperties.getClient().getClientUri().orElseThrow();

        // Build the login options from configured OAuth2 providers
        this.loginOptions = clientProps.getRegistration()
                .entrySet()
                .stream()
                .filter(e -> "authorization_code".equals(e.getValue()
                        .getAuthorizationGrantType()))
                .map(e -> {
                    final var label = e.getValue().getProvider();
                    final var loginUri = "%s/oauth2/authorization/%s".formatted(
                            clientUri,
                            e.getKey()
                    );
                    return new LoginOptionDto(label, loginUri);
                })
                .toList();
    }

    /**
     * GET /login-options
     *
     * @return List of available OAuth2 login providers with their login URIs
     */
    @GetMapping(path = "/login-options", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<LoginOptionDto> getLoginOptions() {
        return this.loginOptions;
    }

    /**
     * DTO representing a login option.
     *
     * @param label    Display name (e.g., "keycloak", "google")
     * @param loginUri URL to redirect browser for login
     */
    public record LoginOptionDto(String label, String loginUri) {}
}