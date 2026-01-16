package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test to verify the application context loads correctly.
 * Uses the 'test' profile to disable OAuth2/OIDC requirements.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayApplicationTests {

	@Test
	void contextLoads() {
		// Verifies Spring context initializes without Keycloak
	}

}
