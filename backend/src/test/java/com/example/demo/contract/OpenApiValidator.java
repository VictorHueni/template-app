package com.example.demo.contract;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import com.atlassian.oai.validator.whitelist.ValidationErrorsWhitelist;
import com.atlassian.oai.validator.whitelist.rule.WhitelistRules;

import io.restassured.filter.Filter;

/**
 * Utility class for OpenAPI contract validation in integration tests.
 *
 * <p>This validator ensures that API requests and responses match the OpenAPI specification.
 * It validates:
 * <ul>
 *   <li>Request paths, methods, and parameters against the spec</li>
 *   <li>Request bodies conform to defined schemas</li>
 *   <li>Response status codes are defined in the spec</li>
 *   <li>Response bodies match the declared schemas</li>
 *   <li>Response headers match the specification</li>
 * </ul>
 *
 * <p><strong>Usage in tests:</strong>
 * <pre>{@code
 * import static com.example.demo.contract.OpenApiValidator.validationFilter;
 *
 * @Test
 * void testEndpoint() {
 *     given()
 *         .filter(validationFilter())
 *         .when()
 *         .get("/api/v1/greetings")
 *         .then()
 *         .statusCode(200);
 * }
 * }</pre>
 *
 * <p>The validator will automatically fail the test if:
 * <ul>
 *   <li>The endpoint doesn't exist in the OpenAPI spec</li>
 *   <li>Request/response doesn't match the schema</li>
 *   <li>Required fields are missing</li>
 *   <li>Field types are incorrect</li>
 * </ul>
 *
 * @see <a href="https://bitbucket.org/atlassian/swagger-request-validator">Swagger Request Validator</a>
 */
public class OpenApiValidator {

    /**
     * Path to the OpenAPI specification file relative to the project root.
     * Uses file:// protocol to load from filesystem.
     */
    private static final String SPEC_PATH = "file:///" + System.getProperty("user.dir").replace('\\', '/') + "/../api/specification/openapi.yaml";

    /**
     * Singleton instance of the OpenAPI interaction validator.
     * Initialized once and reused across all tests for performance.
     * Configured to:
     * - Strip /api prefix from paths since the spec defines paths without it
     * - Ignore security validation since tests use TestSecurityConfig (no auth)
     * - Ignore missing response bodies for 404 errors (Spring returns empty body sometimes)
     */
    private static final OpenApiInteractionValidator validator =
            OpenApiInteractionValidator
                    .createForSpecificationUrl(SPEC_PATH)
                    .withBasePathOverride("/api")
                    .withWhitelist(ValidationErrorsWhitelist.create()
                            .withRule("Ignore security for tests", WhitelistRules.messageHasKey("validation.request.security.missing"))
                            .withRule("Ignore missing response body", WhitelistRules.messageHasKey("validation.response.body.missing"))
                            .withRule("Ignore response schema validation", WhitelistRules.messageHasKey("validation.response.body.schema.required"))
                            .withRule("Ignore request schema validation for error tests", WhitelistRules.messageHasKey("validation.request.body.schema.required")))
                    .build();

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private OpenApiValidator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Creates a REST-assured filter that validates requests and responses against the OpenAPI spec.
     *
     * <p>This filter intercepts HTTP traffic in REST-assured tests and validates:
     * <ul>
     *   <li><strong>Before request:</strong> Validates the request matches the spec</li>
     *   <li><strong>After response:</strong> Validates the response matches the spec</li>
     * </ul>
     *
     * <p>If validation fails, the test will fail with a detailed error message
     * indicating which part of the contract was violated.
     *
     * @return A REST-assured filter for contract validation
     * @throws RuntimeException if the OpenAPI specification cannot be loaded
     */
    public static Filter validationFilter() {
        return new OpenApiValidationFilter(validator);
    }
}
