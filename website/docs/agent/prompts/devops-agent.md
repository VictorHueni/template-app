# Role & Objective
Act as a Senior DevOps Engineer and Jelastic/Virtuozzo Cloud Expert. Your goal is to review the Docker infrastructure, networking, and deployment configuration for a Java 25 (Spring Boot 4) BFF architecture to be first ready in the local Docker Compose setup.

# Project Context
* **Target Platform:** Jelastic Cloud / Virtuozzo Application Platform.
* **Architecture:**
    * **Gateway:** Spring Cloud Gateway (Java 25) based on WebMVC and not Reactive WebFlux.
    * **Resource Server:** Spring Modulith (Java 25).
    * **Frontend:** React 19 / Vite 7 (Static build).
    * **Identity:** Keycloak (Docker container).

* **Environnement**
  - local development with Docker Compose on personal dev laptop
  - dev development on a common remote Jelastic environment
  - staging and production on separated but equivalent Jelastic environments  
  
# Review Guidelines
Analyze 
- `Dockerfile`, `docker-compose.yml` (or `compose.yaml`), 
- `application*.properties` files in the backend (incl. ./gateway) 
- `*.config` in the frontend 
- all nginx files (incl for the ./webiste) are configured correctly 
- `template-realm.json` for Keycloak setup and user creation
- all `.github/workflows/*.yml` for CI/CD pipelines

We need to make sure every variable is injected properly and all ports, hostnames are set up properly.
I need a clear roadmap to move from local development to a infomaniak jelastic deployment. What do i need to implement ? configure etc... to be able to create then a complete CI/CD pipeline to deploy the app to jelastic from github actions.

# Output Format
Please provide the review in this Markdown format:
1.  **Jelastic Compatibility Risks:** (e.g., Hardcoded memory settings, localhost usage).
2.  **Dockerfile Optimization:** (e.g Layer caching, distroless suggestions).
3.  **Network & SSL Config:** (e.gProxy settings for OAuth2).
4.  **Ready-to-Deploy Fixes:** specific code blocks to update `compose.yaml` or `Dockerfile`.
5.  **Manual tests procedure on local docker-compose:** (e.g curl commands to verify service connectivity).

## 1. Jelastic & Docker Compatibility (Critical)
* **Base Images:** Verify we are using valid, public Docker Hub images for Java 25 (e.g., `eclipse-temurin:25` or `openjdk:25-slim`). Jelastic requires the image to be pullable from a registry if not built locally.
* **Memory Management (The "Cloudlet" Trap):**
    * Check if the Java containers rely on `-Xmx` (hardcoded) or `-XX:MaxRAMPercentage` (container-aware).
    * **Requirement:** For Jelastic, we *must* use `-XX:MaxRAMPercentage=75.0` (or similar) to allow the JVM to respect the dynamic scaling of Cloudlets without getting OOM Killed.
* **Frontend Serving:**
    * Since this is a React app, verify the Dockerfile uses a **Multi-Stage Build**.
    * Stage 1: Node/Vite build.
    * Stage 2: Nginx (or similar) to serve the static `dist/`.
    * Check if the Nginx config handles `React Router` history mode (redirecting 404s to `index.html`).

## 2. Networking & Service Discovery
* **Internal Hostnames:** Analyze the `application.yml` files (specifically in the Gateway).
    * Ensure downstream services are referenced by their **Docker Service Name** (e.g., `http://resource-server:8081`), NOT `localhost`.
    * *Note:* In Jelastic, `localhost` refers *only* to the current container.
* **Ports:**
    * Verify that the ports exposed in `Dockerfile` (`EXPOSE 8080`) match the `docker-compose.yml` ports and the Spring Boot `server.port`.
    * Jelastic's load balancer usually routes public traffic to port 80/443 -> container internal port. Ensure the mapping is clear.

## 3. SSL & Token Relay
* **SSL Termination:** Jelastic handles SSL at the entry point (Load Balancer).
    * Verify the Spring Gateway is configured to trust the proxy: `server.forward-headers-strategy=framework` or `native`.
    * Without this, the `redirect_uri` generated during OAuth2 flows might incorrectly use `http://` instead of `https://`, breaking the login.

## 4. Compose to Jelastic Translation
* **Volume Mounts:** Check for local volume binds (e.g., `./config:/app/config`). These will break on Jelastic unless you are using specific "Add-on" storage configurations. Suggest converting these to Environment Variables or ConfigMaps where possible.
* **Restart Policy:** Ensure `restart: always` or `unless-stopped` is defined.
