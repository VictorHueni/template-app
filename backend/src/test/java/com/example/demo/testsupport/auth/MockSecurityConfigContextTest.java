package com.example.demo.testsupport.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class MockSecurityConfigContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(MockJwtBeansConfig.class)
        .withPropertyValues("spring.profiles.active=integration");

    @Test
    void jwtDecoderBean_shouldBeRegisteredAndValidateTokens() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(JwtDecoder.class);
            assertThat(context).hasSingleBean(JwtAuthenticationConverter.class);

            JwtDecoder jwtDecoder = context.getBean(JwtDecoder.class);
            JwtAuthenticationConverter jwtAuthenticationConverter = context.getBean(JwtAuthenticationConverter.class);
            Jwt jwt = jwtDecoder.decode(TestJwtUtils.getUserToken());

            Authentication authentication = jwtAuthenticationConverter.convert(jwt);

            assertThat(jwt.getIssuer().toString()).isEqualTo(TestJwtUtils.TEST_ISSUER);
            assertThat(jwt.getClaimAsString("preferred_username")).isEqualTo(TestJwtUtils.TEST_USER_USERNAME);

            assertThat(authentication).isNotNull();
            assertThat(authentication.getName()).isEqualTo(TestJwtUtils.TEST_USER_USERNAME);

            Set<String> authorities = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());
            assertThat(authorities).contains("ROLE_USER");
        });
    }
}
