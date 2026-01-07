package com.example.demo.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import dasniko.testcontainers.keycloak.KeycloakContainer;

/**
 * Testcontainers configuration providing a singleton Keycloak container for OAuth2 integration tests.
 * <p>
 * This configuration enables real JWT token validation in integration tests:
 * <ul>
 *   <li>Starts Keycloak with the template-realm pre-configured</li>
 *   <li>Provides real JWT tokens from actual Keycloak instance</li>
 *   <li>Validates full OAuth2 resource server flow (JWKS, token validation)</li>
 *   <li>Container reuse enabled for optimal test performance</li>
 * </ul>
 *
 * <p><strong>Why Keycloak Testcontainer?</strong></p>
 * <p>
 * Spring Security's {@code @WithJwt} annotation sets SecurityContext in ThreadLocal,
 * which doesn't propagate to server threads during RestAssured HTTP calls. This means
 * we need real JWT tokens for true integration tests that exercise the full security stack.
 * </p>
 *
 * <p><strong>Test Users (from template-realm.json):</strong></p>
 * <ul>
 *   <li>testuser / test123 - USER role</li>
 *   <li>adminuser / admin123 - USER + ADMIN roles</li>
 * </ul>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * @SpringBootTest(webEnvironment = RANDOM_PORT)
 * @ActiveProfiles({"test", "keycloak-test"})
 * class MySecuredIT extends AbstractSecuredRestAssuredIT {
 *
 *     @Test
 *     void shouldRequireAuthentication() {
 *         given()
 *             .header("Authorization", "Bearer " + getUserToken())
 *             .when()
 *             .get("/api/v1/me")
 *             .then()
 *             .statusCode(200);
 *     }
 * }
 * }</pre>
 *
 * @see KeycloakTokenProvider
 * @see AbstractSecuredRestAssuredIT
 */
@TestConfiguration(proxyBeanMethods = false)
public class KeycloakTestcontainerConfiguration {

    /**
     * Keycloak container version - matches docker-compose.yml for consistency.
     */
    private static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:26.1.2";

    /**
     * Path to realm import file within test resources.
     */
    private static final String REALM_IMPORT_PATH = "/keycloak/template-realm.json";

    /**
     * Singleton Keycloak container instance shared across all tests.
     * Static to ensure single instance across test classes.
     */
    private static final KeycloakContainer KEYCLOAK_CONTAINER;

    static {
        KEYCLOAK_CONTAINER = new KeycloakContainer(KEYCLOAK_IMAGE)
                .withRealmImportFile(REALM_IMPORT_PATH)
                .withReuse(true)
                .withLabel("reuse.container.id", "template-app-keycloak-test");

        KEYCLOAK_CONTAINER.start();
    }

    /**
     * Provides the Keycloak container as a Spring bean for injection.
     *
     * @return the singleton Keycloak container
     */
    @Bean
    public KeycloakContainer keycloakContainer() {
        return KEYCLOAK_CONTAINER;
    }

    /**
     * Returns the OIDC issuer URL for the template realm.
     * <p>
     * This URL is used to configure Spring Security's OAuth2 Resource Server
     * to validate JWT tokens against this Keycloak instance.
     *
     * @return the issuer URL (e.g., http://localhost:32768/realms/template-realm)
     */
    public static String getIssuerUrl() {
        return KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/template-realm";
    }

    /**
     * Returns the auth server base URL.
     *
     * @return the Keycloak auth server URL (e.g., http://localhost:32768)
     */
    public static String getAuthServerUrl() {
        return KEYCLOAK_CONTAINER.getAuthServerUrl();
    }

    /**
     * Registers Spring properties dynamically from the Keycloak container.
     * <p>
     * This method is called by Spring's TestContext framework to inject
     * the actual Keycloak URLs into the application configuration.
     *
     * @param registry the dynamic property registry
     */
    @DynamicPropertySource
    static void keycloakProperties(DynamicPropertyRegistry registry) {
        registry.add("com.c4-soft.springaddons.oidc.ops[0].iss", 
                KeycloakTestcontainerConfiguration::getIssuerUrl);
    }
}
