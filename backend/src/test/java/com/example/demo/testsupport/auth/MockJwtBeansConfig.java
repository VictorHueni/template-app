package com.example.demo.testsupport.auth;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@TestConfiguration
@Profile("integration")
public class MockJwtBeansConfig {

    /**
     * Expected audience for JWT tokens.
     * Matches the Spring Addons configuration in application-*.properties.
     */
    public static final String EXPECTED_AUDIENCE = "template-gateway";

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKeySpec key = new SecretKeySpec(TestJwtUtils.TEST_HS256_SECRET, "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(TestJwtUtils.TEST_ISSUER);
        OAuth2TokenValidator<Jwt> requiredClaimShape = new JwtClaimValidator<>(
                "preferred_username",
                value -> value instanceof String && !((String) value).isBlank()
        );

        // Audience validation - prevents cross-client token reuse attacks
        // JWT must contain our expected audience in its aud claim
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<>(
                "aud",
                aud -> {
                    if (aud instanceof List<?> audiences) {
                        return audiences.contains(EXPECTED_AUDIENCE);
                    }
                    if (aud instanceof String audience) {
                        return EXPECTED_AUDIENCE.equals(audience);
                    }
                    return false;
                }
        );

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                withIssuer,
                requiredClaimShape,
                audienceValidator
        ));
        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("preferred_username");
        converter.setJwtGrantedAuthoritiesConverter(realmAccessRolesConverter());
        return converter;
    }

    private Converter<Jwt, Collection<GrantedAuthority>> realmAccessRolesConverter() {
        return jwt -> {
            Object realmAccessObj = jwt.getClaim("realm_access");
            if (!(realmAccessObj instanceof Map<?, ?> realmAccess)) {
                return List.<GrantedAuthority>of();
            }

            Object rolesObj = realmAccess.get("roles");
            if (!(rolesObj instanceof List<?> roles)) {
                return List.<GrantedAuthority>of();
            }

            return roles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
        };
    }
}
