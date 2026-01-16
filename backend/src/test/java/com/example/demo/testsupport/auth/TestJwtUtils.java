package com.example.demo.testsupport.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Test-only JWT factory for integration/unit tests.
 *
 * <p>Token shape matches what the backend expects in production:
 * <ul>
 *   <li>issuer: {@code iss}</li>
 *   <li>username: {@code preferred_username}</li>
 *   <li>roles: {@code realm_access.roles}</li>
 * </ul>
 */
public final class TestJwtUtils {



    /**
     * Matches the default local Keycloak issuer configured in application-local.properties.
     */
    public static final String TEST_ISSUER = "http://localhost:9000/realms/template-realm";

    /**
     * Fixed HS256 secret for tests.
     * <p>
     * Must be at least 256 bits for HS256.
     */

    // TODO: Make configurable via test properties if needed
    public static final byte[] TEST_HS256_SECRET =
            "test-hs256-secret-32-bytes-minimum!!".getBytes(StandardCharsets.UTF_8);

    public static final String TEST_USER_USERNAME = "testuser";
    public static final String TEST_USER_EMAIL = "test@template.com";

    public static final String TEST_ADMIN_USERNAME = "adminuser";
    public static final String TEST_ADMIN_EMAIL = "admin@template.com";

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    private TestJwtUtils() {
    }

    /**
     * Default audience matching the production Spring Addons configuration.
     */
    public static final String DEFAULT_AUDIENCE = "template-gateway";

    /**
     * Creates a signed JWT token with the default audience.
     *
     * @param username the preferred username
     * @param roles    the realm roles (e.g. USER, ADMIN)
     * @return signed JWT as compact string
     */
    public static String createToken(String username, List<String> roles) {
        return createTokenWithAudience(username, roles, List.of(DEFAULT_AUDIENCE));
    }

    public static String getUserToken() {
        return createToken(TEST_USER_USERNAME, List.of("USER"));
    }

    public static String getAdminToken() {
        return createToken(TEST_ADMIN_USERNAME, List.of("USER", "ADMIN"));
    }

    /**
     * Creates a signed JWT token with a specific audience claim.
     * Used for testing audience validation.
     *
     * @param username  the preferred username
     * @param roles     the realm roles (e.g. USER, ADMIN)
     * @param audiences list of audience values for aud claim
     * @return signed JWT as compact string
     */
    public static String createTokenWithAudience(String username, List<String> roles, List<String> audiences) {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(roles, "roles");
        Objects.requireNonNull(audiences, "audiences");

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(DEFAULT_TTL);

        String email = defaultEmailFor(username);
        String subject = UUID.nameUUIDFromBytes(username.getBytes(StandardCharsets.UTF_8)).toString();

        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", List.copyOf(roles));

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(TEST_ISSUER)
                .subject(subject)
                .issueTime(java.util.Date.from(issuedAt))
                .expirationTime(java.util.Date.from(expiresAt))
                .jwtID(UUID.randomUUID().toString())
                .claim("preferred_username", username)
                .claim("email", email)
                .claim("realm_access", realmAccess)
                .audience(audiences)
                .build();

        return signClaims(claims);
    }

    /**
     * Creates a signed JWT token without any audience claim.
     * Used for testing rejection of tokens missing audience.
     *
     * @param username the preferred username
     * @param roles    the realm roles (e.g. USER, ADMIN)
     * @return signed JWT as compact string without aud claim
     */
    public static String createTokenWithoutAudience(String username, List<String> roles) {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(roles, "roles");

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(DEFAULT_TTL);

        String email = defaultEmailFor(username);
        String subject = UUID.nameUUIDFromBytes(username.getBytes(StandardCharsets.UTF_8)).toString();

        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", List.copyOf(roles));

        // Note: No .audience() call - intentionally omitted
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(TEST_ISSUER)
                .subject(subject)
                .issueTime(java.util.Date.from(issuedAt))
                .expirationTime(java.util.Date.from(expiresAt))
                .jwtID(UUID.randomUUID().toString())
                .claim("preferred_username", username)
                .claim("email", email)
                .claim("realm_access", realmAccess)
                .build();

        return signClaims(claims);
    }

    /**
     * Signs JWT claims with the test HS256 secret.
     *
     * @param claims the JWT claims to sign
     * @return signed JWT as compact string
     */
    private static String signClaims(JWTClaimsSet claims) {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                .type(JOSEObjectType.JWT)
                .build();

        SignedJWT signedJwt = new SignedJWT(header, claims);
        try {
            signedJwt.sign(new MACSigner(TEST_HS256_SECRET));
        } catch (JOSEException ex) {
            throw new IllegalStateException("Failed to sign test JWT", ex);
        }

        return signedJwt.serialize();
    }

    private static String defaultEmailFor(String username) {
        if (TEST_USER_USERNAME.equals(username)) {
            return TEST_USER_EMAIL;
        }
        if (TEST_ADMIN_USERNAME.equals(username)) {
            return TEST_ADMIN_EMAIL;
        }
        return username + "@template.com";
    }
}
