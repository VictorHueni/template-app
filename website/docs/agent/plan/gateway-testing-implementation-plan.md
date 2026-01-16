
# Implementation Plan: Gateway Test Strategy (BFF Token Relay + Routing)

## 1. Understanding the Problem

### Why are we doing this?
The Gateway is the security and routing boundary of the BFF architecture. If it misroutes requests, fails to enforce auth, or fails to relay tokens correctly, the whole platform is insecure or unusable.

At the same time, the Backend integration test suite intentionally **mocks Gateway + Keycloak** to keep feedback fast and failures local to the backend.

Therefore we need a **separate Gateway test suite** that validates:
* Routing (predicates, path handling, forwarding)
* Security boundaries (public vs protected routes)
* Token relay semantics (when an OAuth2 client session exists, the Gateway forwards a bearer token)

### What is the current bottleneck/risk?
Today the Gateway module has only smoke/controller coverage:
* A context-load smoke test (`GatewayApplicationTests`)
* A unit-style controller test (`LoginOptionsControllerTest`)

What is missing:
* Automated proof that `/api/**` routing works end-to-end.
* Automated proof that the Gateway forwards `Authorization: Bearer …` to the backend.
* A safety net against Keycloak realm/client/issuer drift.

### Before vs After (high level)

| Area                          | Before          | After                                                 |
| ----------------------------- | --------------- | ----------------------------------------------------- |
| Routing correctness           | Implicit/manual | Automated forwarding tests against a stub backend     |
| Public vs protected endpoints | Implicit/manual | Automated security boundary tests                     |
| Token relay                   | Not verified    | Contract test proves `TokenRelay` forwards a real JWT |
| Keycloak drift detection      | Not covered     | Nightly/opt-in contract tests run with real Keycloak  |

### Architectural boundary (important)
* **Backend IT suite**: validates backend behavior given a JWT; does not validate Gateway/Keycloak.
* **Gateway test suite (this plan)**: validates the BFF boundary and the “session → token relay → backend” behavior.

---

## 2. Phase 0: Prerequisites & Configuration

### Step 2.1: Scan current Gateway module and existing tests
* **Why:** We must align the test strategy with the module’s actual stack (Spring Cloud Gateway **WebMVC**) and its current config.
* **What:** Review routing + security configuration and current tests to avoid proposing patterns that cannot work in this module.
* **Implementation Options & Design Decisions:**
	* Option A (Chosen): Keep the plan grounded in the existing Spring Addons + Spring Cloud Gateway configuration style.
	* Option B: Rewrite gateway security to make testing easier. (Not acceptable; tests must adapt to the product, not the opposite.)
* **Changes:** (No code changes)
	* Review:
		* `gateway/src/main/resources/application.yaml` (routes + TokenRelay + Spring Addons)
		* `gateway/src/test/java/com/example/gateway/GatewayApplicationTests.java`
		* `gateway/src/test/java/com/example/gateway/auth/LoginOptionsControllerTest.java`
* **Verification:**
	* `cd gateway ; ./mvnw test`
	* Expected output: `BUILD SUCCESS`

### Step 2.2: Introduce a clear split between fast tests and contract integration tests
* **Why:** Tests that boot Keycloak are slower and should not block the inner-loop. Also, a junior-friendly template needs predictable commands.
* **What:** Adopt naming + Maven execution conventions:
	* Fast tests: `*Test.java` (Surefire → `mvn test`)
	* Contract tests: `*IT.java` (Failsafe → `mvn verify`)
* **Implementation Options & Design Decisions:**
	* Option A (Chosen): Add `maven-failsafe-plugin` in `gateway/pom.xml` and run `*IT.java` during `verify`.
	* Option B: Keep everything in Surefire and gate using JUnit tags. (Works, but easier to misconfigure in CI and harder for juniors to reason about.)
* **Changes:**
	* File: `gateway/pom.xml`
	* Add plugin:
		```xml
		<plugin>
			<groupId>org.apache.maven.plugins</groupId>
			<artifactId>maven-failsafe-plugin</artifactId>
			<version>3.5.2</version>
			<executions>
				<execution>
					<goals>
						<goal>integration-test</goal>
						<goal>verify</goal>
					</goals>
				</execution>
			</executions>
		</plugin>
		```
* **Verification:**
	* `cd gateway ; ./mvnw test` runs only `*Test` → `BUILD SUCCESS`
	* `cd gateway ; ./mvnw verify` runs `*Test` + `*IT` → `BUILD SUCCESS`

### Step 2.3: Add a backend stub dependency for routing/token-relay assertions
* **Why:** We want to prove forwarding behavior without booting the real backend.
* **What:** Use a local HTTP stub server inside tests so we can assert what the Gateway forwarded.
* **Implementation Options & Design Decisions:**
	* Option A (Chosen): WireMock (expressive request assertions; great for gateway forwarding tests).
	* Option B: OkHttp `MockWebServer` (lighter but less expressive request matching).
	* Option C: Testcontainers-based stub backend (overkill).
* **Changes:**
	* File: `gateway/pom.xml`
	* Add test dependency:
		```xml
		<dependency>
			<groupId>org.wiremock</groupId>
			<artifactId>wiremock-standalone</artifactId>
			<version>3.9.1</version>
			<scope>test</scope>
		</dependency>
		```
* **Verification:**
	* `cd gateway ; ./mvnw test` → `BUILD SUCCESS`

---

## 3. Phase 1: Fast Gateway Behavior Tests (No Keycloak)

### Step 3.1: Add a routing-forwarding test (Gateway → stub backend)
* **Why:** Routing mistakes are common and high-impact. We need automated proof that `/api/**` is forwarded to the configured backend URI and that responses are relayed.
* **What:** Boot the Gateway, route `/api/**` to WireMock, then assert:
	* `GET /api/v1/greetings` returns the stubbed backend response.
	* WireMock received the request with the expected path.
* **Implementation Options & Design Decisions:**
	* Option A (Chosen): `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`.
	* Option B: `MockMvc`. (Good for controller-only tests, but often bypasses gateway routing internals.)
* **Changes:**
	* Create: `gateway/src/test/java/com/example/gateway/routing/GatewayRoutingForwardingTest.java`
	* Recommended skeleton:
		```java
		@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
		@ActiveProfiles("routing-test")
		class GatewayRoutingForwardingTest {
			@LocalServerPort int port;

			@Test
			void forwardsApiRequestsToBackendStub() {
				// Arrange: WireMock stub for /api/v1/greetings
				// Act: GET http://localhost:{port}/api/v1/greetings
				// Assert: response matches stub; WireMock received expected request
			}
		}
		```
	* Create: `gateway/src/test/resources/application-routing-test.yaml`
		* Enable gateway routing for this profile.
		* Point the backend route URI to the WireMock port (via `@DynamicPropertySource`).
* **Verification (Checklist for Success):**
	* Running `cd gateway ; ./mvnw test` executes the test and prints `BUILD SUCCESS`.
	* WireMock confirms the Gateway forwarded the request to `/api/v1/greetings`.

### Step 3.2: Add security boundary tests (permit-all vs protected)
* **Why:** The Gateway is the policy boundary. We must prove public endpoints remain public and protected ones are not accidentally opened.
* **What:** Add tests that verify:
	* `/login-options` is accessible anonymously.
	* Public backend endpoints (as configured in `application.yaml`) remain accessible anonymously.
	* A deliberately protected route is not accessible anonymously.
* **Implementation Options & Design Decisions:**
	* Option A (Chosen): Add one test-only protected route under a test profile to ensure the test suite actually proves a negative case.
	* Option B: Only test public endpoints. (Too weak; doesn’t catch accidental `permitAll` drift.)
* **Changes:**
	* Create: `gateway/src/test/java/com/example/gateway/security/GatewaySecurityBoundaryTest.java`
	* Create or extend: `gateway/src/test/resources/application-security-test.yaml`
		* Define one route that is *not* in permit-all.
* **Verification:**
	* Anonymous call to `/login-options` returns 200.
	* Anonymous call to the protected test route returns 401 or a redirect (depending on auth entrypoint).
	* `cd gateway ; ./mvnw test` → `BUILD SUCCESS`

---

## 4. Phase 2: Token Relay Contract Tests (Real Keycloak)

### Step 4.1: Add a real Keycloak container with realm import
* **Why:** Mock-based tests can drift. A periodic proof against a real Keycloak realm catches issuer/client configuration breaks.
* **What:** Use Testcontainers to start Keycloak with the repository realm import (`keycloak/import`).
* **Implementation Options & Design Decisions:**
	* Option A (Chosen): `GenericContainer` with the official Keycloak image and `--import-realm`, mounting the import directory.
	* Option B: `dasniko/testcontainers-keycloak`. (Convenient, but adds a specialized dependency; `GenericContainer` is easier to reason about.)
* **Changes:**
	* File: `gateway/pom.xml` add Testcontainers:
		```xml
		<dependency>
			<groupId>org.testcontainers</groupId>
			<artifactId>junit-jupiter</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.testcontainers</groupId>
			<artifactId>testcontainers</artifactId>
			<scope>test</scope>
		</dependency>
		```
	* Create: `gateway/src/test/java/com/example/gateway/contract/KeycloakContainerSupport.java`
		* Starts Keycloak and exposes issuer URL + ports.
* **Verification:**
	* `cd gateway ; ./mvnw verify` shows Keycloak container logs and ends with `BUILD SUCCESS`.

### Step 4.2: Add a Token Relay contract test (Gateway → stub backend, Authorization forwarded)
* **Why:** The core BFF guarantee is: if the user is authenticated (session contains an authorized client), the Gateway forwards a bearer token to the backend.
* **What:** Write `*IT.java` that:
	1) Obtains a real access token from Keycloak (non-browser flow).
	2) Injects it into the Gateway’s authorized-client/session storage for the request.
	3) Calls the Gateway `/api/**` route.
	4) Asserts WireMock received `Authorization: Bearer <jwt>`.
* **Implementation Options & Design Decisions:**
	* Option A (Chosen): Fetch a token from Keycloak token endpoint (client credentials or password grant—whatever the realm supports) and attach it via a test-only hook.
	* Option B: Full browser automation (Playwright) to run the authorization-code flow. (More realistic but significantly more complex and slow; better as system-level e2e tests owned by the frontend module.)
* **Changes:**
	* Create: `gateway/src/test/java/com/example/gateway/contract/GatewayTokenRelayContractIT.java`
	* Create: `gateway/src/test/resources/application-contract.yaml`
		* Point `issuer` to the container.
		* Enable routing.
		* Route backend URI to WireMock.
* **Verification (Checklist for Success):**
	* Test starts Keycloak container.
	* Test starts WireMock backend.
	* Calling the Gateway route causes WireMock to receive `Authorization: Bearer ...`.
	* `cd gateway ; ./mvnw verify` → `BUILD SUCCESS`

---

## 5. Phase 3: Developer Workflow & Documentation

### Step 5.1: Document how to run fast vs contract tests
* **Why:** This is a template for small/medium projects; developers need a predictable inner-loop command.
* **What:** Document:
	* Fast loop: `./mvnw test`
	* Contract suite: `./mvnw verify`
* **Implementation Options & Design Decisions:**
	* Option A (Chosen): Use Maven lifecycle separation (Surefire vs Failsafe) so commands remain standard.
	* Option B: Custom scripts. (Adds maintenance burden.)
* **Changes:**
	* Update: `gateway/HELP.md` (or `gateway/README.md` if preferred)
* **Verification:**
	* Docs match actual behavior.

---

### Notes for maintainability (quality-first template)
* Keep contract tests few (1–3) and high-signal.
* Prefer asserting invariants:
	* which routes are public
	* which routes are protected
	* whether `Authorization` is forwarded when authenticated
* When auth configuration changes (realm/client/issuer), update contract tests first to detect drift early.

