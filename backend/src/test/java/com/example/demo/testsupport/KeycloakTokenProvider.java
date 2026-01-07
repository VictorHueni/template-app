package com.example.demo.testsupport;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class for obtaining JWT access tokens from Keycloak Testcontainer.
 * <p>
 * This provider implements the OAuth2 Resource Owner Password Credentials grant
 * (password flow) to obtain real JWT tokens for integration testing. The tokens
 * are cached to avoid repeated token requests during test execution.
 *
 * <p><strong>Security Note:</strong></p>
 * <p>
 * The password grant is only used in tests. In production, the gateway handles
 * authentication via the authorization code flow.
 * </p>
 *
 * <p><strong>Test Users:</strong></p>
 * <ul>
 *   <li>{@code testuser / test123} - Has USER role</li>
 *   <li>{@code adminuser / admin123} - Has USER and ADMIN roles</li>
 * </ul>
 *
 * <p><strong>Token Caching:</strong></p>
 * <p>
 * Tokens are cached per username to avoid excessive calls to Keycloak.
 * The cache is thread-safe for parallel test execution.
 * </p>
 *
 * @see KeycloakTestcontainerConfiguration
 * @see AbstractSecuredRestAssuredIT
 */
public final class KeycloakTokenProvider {

    /**
     * Test user credentials - USER role only.
     */
    public static final String TEST_USER_USERNAME = "testuser";
    public static final String TEST_USER_PASSWORD = "test123";

    /**
     * Admin user credentials - USER + ADMIN roles.
     */
    public static final String ADMIN_USER_USERNAME = "adminuser";
    public static final String ADMIN_USER_PASSWORD = "admin123";

    /**
     * Public test client ID (directAccessGrantsEnabled=true).
     */
    private static final String CLIENT_ID = "test-client";

    /**
     * Token cache to avoid repeated Keycloak calls.
     * Thread-safe for parallel test execution.
     */
    private static final Map<String, String> TOKEN_CACHE = new ConcurrentHashMap<>();

    /**
     * HTTP client for token requests.
     */
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * JSON parser for token response.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private KeycloakTokenProvider() {
        // Utility class - prevent instantiation
    }

    /**
     * Obtains an access token for the test user (USER role).
     *
     * @return JWT access token string
     */
    public static String getUserToken() {
        return getToken(TEST_USER_USERNAME, TEST_USER_PASSWORD);
    }

    /**
     * Obtains an access token for the admin user (USER + ADMIN roles).
     *
     * @return JWT access token string
     */
    public static String getAdminToken() {
        return getToken(ADMIN_USER_USERNAME, ADMIN_USER_PASSWORD);
    }

    /**
     * Obtains an access token for a specific user.
     * <p>
     * Tokens are cached per username. If a cached token exists, it's returned
     * without making a new request to Keycloak.
     *
     * @param username the username
     * @param password the password
     * @return JWT access token string
     * @throws RuntimeException if token acquisition fails
     */
    public static String getToken(String username, String password) {
        return TOKEN_CACHE.computeIfAbsent(username, _ -> fetchToken(username, password));
    }

    /**
     * Clears the token cache.
     * <p>
     * Call this if you need fresh tokens (e.g., after role changes in tests).
     */
    public static void clearCache() {
        TOKEN_CACHE.clear();
    }

    /**
     * Fetches a new access token from Keycloak using the password grant.
     *
     * @param username the username
     * @param password the password
     * @return JWT access token string
     */
    private static String fetchToken(String username, String password) {
        try {
            String tokenEndpoint = KeycloakTestcontainerConfiguration.getAuthServerUrl()
                    + "/realms/template-realm/protocol/openid-connect/token";

            // Don't request specific scopes - let Keycloak assign default scopes
            String formData = buildFormData(Map.of(
                    "grant_type", "password",
                    "client_id", CLIENT_ID,
                    "username", username,
                    "password", password
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenEndpoint))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "Failed to get token: HTTP " + response.statusCode() + " - " + response.body());
            }

            JsonNode jsonNode = OBJECT_MAPPER.readTree(response.body());
            return jsonNode.get("access_token").asText();

        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain access token from Keycloak", e);
        }
    }

    /**
     * Builds URL-encoded form data from a map of parameters.
     *
     * @param params the parameters
     * @return URL-encoded form data string
     */
    private static String buildFormData(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((key, value) -> {
            if (!sb.isEmpty()) {
                sb.append("&");
            }
            sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
            sb.append("=");
            sb.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        });
        return sb.toString();
    }
}
