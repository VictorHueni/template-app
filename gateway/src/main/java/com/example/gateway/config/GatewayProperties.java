package com.example.gateway.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the BFF Gateway.
 * 
 * <p>Used to configure client-facing URI and other gateway-specific settings.</p>
 * 
 * @param clientUri The public-facing URI of this gateway (used for OAuth2 redirects)
 */
@ConfigurationProperties(prefix = "gateway")
public record GatewayProperties(
    URI clientUri
) {
    
    /**
     * Default constructor with sensible defaults for local development.
     */
    public GatewayProperties {
        if (clientUri == null) {
            clientUri = URI.create("http://localhost:8080");
        }
    }
}
