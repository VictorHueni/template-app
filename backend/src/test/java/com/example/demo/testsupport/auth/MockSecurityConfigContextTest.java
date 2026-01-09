package com.example.demo.testsupport.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;

class MockSecurityConfigContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(MockJwtBeansConfig.class)
        .withPropertyValues("spring.profiles.active=integration");

    @Test
    void jwtDecoderBean_shouldBeRegisteredAndValidateTokens() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(JwtDecoder.class);

            JwtDecoder jwtDecoder = context.getBean(JwtDecoder.class);
            Jwt jwt = jwtDecoder.decode(TestJwtUtils.getUserToken());

            assertThat(jwt.getIssuer().toString()).isEqualTo(TestJwtUtils.TEST_ISSUER);
            assertThat(jwt.getClaimAsString("preferred_username")).isEqualTo(TestJwtUtils.TEST_USER_USERNAME);
        });
    }
}
