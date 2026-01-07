package com.example.gateway.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.c4_soft.springaddons.security.oidc.starter.properties.SpringAddonsOidcClientProperties;
import com.c4_soft.springaddons.security.oidc.starter.properties.SpringAddonsOidcProperties;

/**
 * Unit tests for LoginOptionsController.
 * 
 * Tests verify:
 * 1. Endpoint returns correct JSON structure
 * 2. Endpoint is accessible without authentication (public)
 * 3. Endpoint works for authenticated users too
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(LoginOptionsControllerTest.TestConfig.class)
@ActiveProfiles("test")
class LoginOptionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("GET /login-options")
    class GetLoginOptions {

        @Test
        @WithAnonymousUser
        @DisplayName("should return login options for anonymous users")
        void shouldReturnLoginOptionsForAnonymous() throws Exception {
            mockMvc.perform(get("/login-options")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].label").value("keycloak"))
                .andExpect(jsonPath("$[0].loginUri").value("http://localhost:8080/oauth2/authorization/keycloak"));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should return login options for authenticated users")
        void shouldReturnLoginOptionsForAuthenticated() throws Exception {
            mockMvc.perform(get("/login-options")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
        }
    }

    /**
     * Test configuration providing mock beans for OAuth2 properties.
     */
    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        OAuth2ClientProperties oauth2ClientProperties() {
            var props = new OAuth2ClientProperties();
            
            // Configure a test provider
            var provider = new OAuth2ClientProperties.Provider();
            provider.setIssuerUri("http://localhost:9000/realms/test");
            props.getProvider().put("keycloak", provider);
            
            // Configure a test registration
            var registration = new OAuth2ClientProperties.Registration();
            registration.setProvider("keycloak");
            registration.setClientId("test-client");
            registration.setClientSecret("test-secret");
            registration.setAuthorizationGrantType("authorization_code");
            props.getRegistration().put("keycloak", registration);
            
            return props;
        }

        @Bean
        @Primary
        SpringAddonsOidcProperties springAddonsOidcProperties() {
            var props = new SpringAddonsOidcProperties();
            
            // Configure client properties with client-uri
            var clientProps = new SpringAddonsOidcClientProperties();
            clientProps.setClientUri(Optional.of(URI.create("http://localhost:8080")));
            clientProps.setSecurityMatchers(java.util.List.of("/**"));
            props.setClient(clientProps);
            
            return props;
        }
    }
}
