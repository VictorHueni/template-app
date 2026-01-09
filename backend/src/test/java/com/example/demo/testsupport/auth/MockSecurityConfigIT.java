package com.example.demo.testsupport.auth;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Replaced by MockSecurityConfigContextTest; full @SpringBootTest with integration profile requires Phase 2 programmatic Flyway migrations")
@SpringBootTest
@ActiveProfiles({"test", "integration"})
class MockSecurityConfigIT {

    @Autowired
    JwtDecoder jwtDecoder;

    @Test
    void jwtDecoder_shouldValidateTokenSignedByTestJwtUtils() {
        String token = TestJwtUtils.getUserToken();

        Jwt jwt = jwtDecoder.decode(token);

        assertThat(jwt.getIssuer().toString()).isEqualTo(TestJwtUtils.TEST_ISSUER);
        assertThat(jwt.getClaimAsString("preferred_username")).isEqualTo(TestJwtUtils.TEST_USER_USERNAME);
    }
}
