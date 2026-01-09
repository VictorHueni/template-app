package com.example.demo.testsupport.auth;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import static org.assertj.core.api.Assertions.assertThat;

class TestJwtUtilsTest {

    @Test
    void getUserToken_shouldGenerateParsableJwtWithExpectedClaims() throws Exception {
        String token = TestJwtUtils.getUserToken();

        assertThat(token).isNotBlank();

        SignedJWT jwt = SignedJWT.parse(token);
        assertThat(jwt.verify(new MACVerifier(TestJwtUtils.TEST_HS256_SECRET))).isTrue();

        JWTClaimsSet claims = jwt.getJWTClaimsSet();
        assertThat(claims.getIssuer()).isEqualTo(TestJwtUtils.TEST_ISSUER);
        assertThat(claims.getStringClaim("preferred_username")).isEqualTo(TestJwtUtils.TEST_USER_USERNAME);
        assertThat(claims.getStringClaim("email")).isEqualTo(TestJwtUtils.TEST_USER_EMAIL);
        assertThat(claims.getSubject()).isNotBlank();

        Object realmAccessObj = claims.getClaim("realm_access");
        assertThat(realmAccessObj).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> realmAccess = (Map<String, Object>) realmAccessObj;
        assertThat(realmAccess.get("roles")).isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");
        assertThat(roles).containsExactly("USER");
    }

    @Test
    void getAdminToken_shouldIncludeUserAndAdminRoles() throws Exception {
        String token = TestJwtUtils.getAdminToken();

        assertThat(token).isNotBlank();

        SignedJWT jwt = SignedJWT.parse(token);
        assertThat(jwt.verify(new MACVerifier(TestJwtUtils.TEST_HS256_SECRET))).isTrue();

        JWTClaimsSet claims = jwt.getJWTClaimsSet();
        assertThat(claims.getStringClaim("preferred_username")).isEqualTo(TestJwtUtils.TEST_ADMIN_USERNAME);
        assertThat(claims.getStringClaim("email")).isEqualTo(TestJwtUtils.TEST_ADMIN_EMAIL);

        @SuppressWarnings("unchecked")
        Map<String, Object> realmAccess = (Map<String, Object>) claims.getClaim("realm_access");
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");

        assertThat(roles).containsExactly("USER", "ADMIN");
    }
}
