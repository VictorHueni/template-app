# 003. Authentication & Identity Strategy

## Context and Problem Statement

We are building a modern, cloud-native template application composed of a **React Frontend** (SPA) and a **Spring Boot Backend** (API). To ensure the security and integrity of user data, we need a robust **Authentication and Authorization** strategy.

The critical challenges are:
1.  **Security by Design**: We must protect against common web vulnerabilities, specifically **Cross-Site Scripting (XSS)**. Storing sensitive tokens (Access/Refresh Tokens) in the browser's `localStorage` or `sessionStorage` is widely considered unsafe in modern security contexts (2025+).
2.  **Statelessness**: Our backend API should remain stateless to ensure scalability and adherence to REST principles. It should not manage user sessions.
3.  **Standardization**: We want to rely on proven industry standards (OpenID Connect) rather than custom security schemes.
4.  **Microservices Readiness**: The architecture must support adding future services without duplicating authentication logic or sharing session state across services.

## Considered Options

### A. Authentication Standard
*   **OAuth 2.0 & OpenID Connect (OIDC)**: The modern industry standard.
*   **SAML 2.0**: The legacy enterprise standard (XML-based).
*   **WebAuthn (Passkeys)**: The passwordless future (FIDO2).

### B. Identity Provider (IdP)
*   **Keycloak**: The Java/Enterprise standard (Open Source).
*   **Zitadel**: The Cloud-Native/Go challenger.
*   **Authentik**: The flexible Homelab option.

### C. System Architecture
*   **Public Client (SPA)**: Frontend handles tokens directly (Vulnerable to XSS).
*   **Unified Backend (Monolith)**: Backend handles UI + Auth + Logic (Coupled, Stateful).
*   **Gateway BFF (Backend for Frontend)**: A dedicated edge service handles Auth and proxies requests.

(For a detailed analysis of these options, refer to `../04-solution-strategy/authentication-options-analysis.md`.)

## Decision Outcome

We have made a compound decision to adopt the following stack:

1.  **Standard**: **OpenID Connect (OIDC)**.
2.  **Identity Provider**: **Keycloak**.
3.  **Architecture**: **Gateway BFF Pattern** (using **Spring Cloud Gateway**).

### Reasons
*   **OIDC**: Provides the widest compatibility and security features.
*   **Keycloak**: Chosen for its "Reference Architecture" status in the Java ecosystem and comprehensive feature set.
*   **Gateway BFF**: This architecture explicitly solves the conflict between "Browser Security" and "API Statelessness".
    *   **Browsers** trust only Cookies (HttpOnly, Secure).
    *   **APIs** trust only Tokens (JWT).
    *   The **Gateway** acts as the bridge, translating the secure Cookie into a stateless JWT for the backend.

### Consequences

*   **Good**:
    *   **Maximum Security**: Tokens never reach the browser, eliminating the risk of XSS token theft.
    *   **Stateless Backend**: The `backend` service remains purely stateless, validating JWTs on every request. It has no knowledge of user sessions.
    *   **DevX (Golden Path)**: Frontend developers do not need to write complex OAuth2 code; they simply handle `401` errors. Backend developers rely on standard Spring Security resource server configuration.
    *   **Future-Proof**: Adding a second service (e.g., `Billing`) is trivial; the Gateway handles auth for both.

*   **Bad**:
    *   **Infrastructure Complexity**: Requires deploying and managing an additional container (the Gateway).
    *   **Network Hops**: Adds one extra network hop (Browser -> Gateway -> Backend), though latency is negligible in local/cluster networks.

## References

1.  **Analysis Document**: [Authentication & Identity Analysis](../04-solution-strategy/authentication-options-analysis.md)
2.  **IETF BCP**: [OAuth 2.0 for Browser-Based Apps](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-browser-based-apps)
3.  **Concept**: [The Backend for Frontend (BFF) Pattern](https://oauth.net/2/bff/)
4.  **Implementation**: [Spring Cloud Gateway Token Relay](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway/developer-guide.html#the-tokenrelay-gatewayfilter-factory)
