# Jelastic (Infomaniak) Staging Deployment Plan

This document captures the step-by-step staging-first roadmap to move from local Docker Compose to a deployable setup on Jelastic / Virtuozzo Application Platform (VAP), for this monorepo:
- `gateway`: Spring Cloud Gateway (WebMVC) BFF (Java 25)
- `backend`: Spring Modulith resource server (Java 25)
- `frontend`: React 19 / Vite 7 (static build)
- `keycloak`: identity provider (container)
- `website`: documentation portal (optional)

---

## 1) Jelastic Compatibility Risks

### Must fix before staging
- `host.docker.internal` is hard-coded in Compose and will not work on Jelastic (and can break on Linux):
  - `docker-compose.yml:59`, `docker-compose.yml:133`, `docker-compose.yml:134`, `docker-compose.yml:135`, `docker-compose.yml:161`
- Keycloak uses `start-dev` (not production-grade) and has no external DB configured:
  - `docker-compose.yml:123`
- Backend staging logs default to `/var/log/...` (bad fit for containers/distroless; prefer stdout):
  - `backend/src/main/resources/application-staging.properties:93`
- Backend Compose overrides schema management with `SPRING_JPA_HIBERNATE_DDL_AUTO=update` (conflicts with the repository’s “Flyway + ddl-auto=validate” posture):
  - `docker-compose.yml:56`
- Port mismatch risk (Compose uses backend 8081/8082 while Dockerfile exposes 8080/8081 and comments mention 8080/8081):
  - `docker-compose.yml:48`, `backend/Dockerfile:47`
- Frontend container runs as non-root but listens on privileged port 80 → likely won’t start:
  - `frontend/Dockerfile:55`, `frontend/nginx.conf:12`
- OIDC issuer calculation can mismatch the real public URL (common behind LBs/SSL termination):
  - `gateway/src/main/resources/application.yaml:3`

### Must fix before production (also recommended for staging)
- `keycloak/import/template-realm.json` contains static user passwords and broad redirect URIs:
  - `keycloak/import/template-realm.json:70`, `keycloak/import/template-realm.json:88`, `keycloak/import/template-realm.json:18`

---

## 2) Dockerfile Optimization Notes (Registry + Runtime)

- Backend/Gateway are multi-stage + distroless (good).
  - Recommendation for Jelastic: publish final images to a registry (GHCR recommended) and deploy by pulling immutable tags.
- Frontend/Website are multi-stage builds (good), but the frontend nginx must be fixed for non-root/port binding.
- Align Node versions across `frontend` and `website` to reduce drift (optional, but recommended).

---

## 3) Network & SSL Plan (BFF-friendly)

### Target staging topology
- Public entrypoint: `frontend` (nginx serving static + reverse proxy to gateway)
- Private services: `gateway` → `backend`
- Auth: `keycloak` as separate public endpoint (recommended)

### SSL termination
- Jelastic terminates TLS at the edge. Ensure gateway trusts proxy headers in staging/prod:
  - `server.forward-headers-strategy: framework` is already present in staging/prod gateway config.

### OIDC issuer (critical)
- Use a single canonical `KEYCLOAK_ISSUER` per environment:
  - Example: `https://auth-staging.<your-domain>/realms/template-realm`
- Avoid `host + port` computed issuer for staging/prod.

---

## 4) Ready-to-Deploy Fixes (Staging-first)

### A. Frontend: fix non-root + port 80
Pick one:
- Option A (recommended): move nginx to an unprivileged port (e.g. 8080) and map LB/host 80/443 → container 8080.
- Option B: keep port 80 but do not force `USER nginx` in the image.

### B. Backend: remove schema auto-update override
- Remove `SPRING_JPA_HIBERNATE_DDL_AUTO=update` from `docker-compose.yml` to keep Flyway/validation behavior consistent.

### C. Keycloak: split local vs staging realm import
- Local dev realm: can contain test users and wide redirect URIs.
- Staging realm: no test users, no default passwords, client secrets managed via env/secrets.
- Staging/prod startup: replace `start-dev` with `start`, enable proxy headers, and use an external Postgres.

### D. Compose portability
- Ensure staging/prod config never uses `host.docker.internal`.
- If local dev still needs it on Linux, add `extra_hosts: ["host.docker.internal:host-gateway"]` (local-only).

---

## 5) Manual Tests (Local Docker Compose)

Assuming `.env` is configured:

- Start stack: `docker compose up --build`
- Verify Keycloak well-known:
  - `curl -sSf http://localhost:$KC_PORT/realms/template-realm/.well-known/openid-configuration`
- Verify backend actuator:
  - `curl -sSf http://localhost:$BCK_MGMT_PORT/management/health`
- Verify frontend static:
  - `curl -sSf http://localhost:$FE_PORT/`
- Verify frontend → gateway proxy:
  - `curl -i http://localhost:$FE_PORT/login-options`
- Browser OAuth smoke test:
  - Open `http://localhost:$FE_PORT/` → login → redirect to Keycloak → redirect back to app.

---

## 6) Step-by-Step Roadmap (Staging-first, junior-friendly)

### Phase 0 — Decide staging URLs (1–2 hours)
- Choose DNS names, e.g.:
  - App: `https://staging.<app>.jcloud.ik-server.com`
  - Auth: `https://auth-staging.<app>.jcloud.ik-server.com`
  - (Optional docs): `https://docs-staging.<app>.jcloud.ik-server.com`
- Choose image registry:
  - Recommendation: GHCR (`ghcr.io/<org>/<image>`)

### Phase 1 — Make local Compose “cloud-shaped” (half day)
- **Frontend**
  - Fix the nginx non-root/port binding issue.
  - Keep proxying `/api|/oauth2|/login|/logout` to gateway (BFF pattern).
- **Gateway**
  - In staging, set `KEYCLOAK_ISSUER` explicitly to match the public Keycloak URL.
  - Keep `server.forward-headers-strategy: framework` in staging/prod.
- **Backend**
  - Remove schema auto-update override in Compose.
  - Keep Flyway + `ddl-auto=validate` for staging.
  - Log to stdout (avoid file logs in containers).
- **Keycloak**
  - Keep `start-dev` only for local.
  - For staging, use `start`, proxy headers, external DB.
  - Use a staging realm without seeded users/passwords.

### Phase 2 — Define staging topology in Jelastic (half day)
- Create Jelastic environment: `staging`.
- Nodes (simple start):
  - `frontend` (public endpoint on 80/443)
  - `gateway` (private)
  - `backend` (private)
  - `postgres` (managed add-on preferred) for backend
  - `keycloak` (public endpoint) + its own Postgres
- Scaling:
  - Start 1 instance each.
  - If using server-side sessions, ensure sticky sessions at LB (or add Redis later).

### Phase 3 — Build & publish images from GitHub Actions (1 day)
- Add a release workflow to build and push images for:
  - `backend`, `gateway`, `frontend` (+ optional `website`)
- Tag strategy:
  - Immutable: `:<git-sha>`
  - Channel: `:staging`
- Store secrets in GitHub:
  - Registry auth (GHCR)
  - Jelastic API credentials (next phase)

### Phase 4 — First manual staging deployment (1 day)
- In Jelastic UI:
  - Create containers from pushed images.
  - Set env vars per node:
    - `SPRING_PROFILES_ACTIVE=staging`
    - `KEYCLOAK_ISSUER=https://auth-staging.../realms/template-realm`
    - DB settings from managed Postgres
    - `KC_CLIENT_SECRET` for gateway
  - Configure Keycloak hostname/proxy mode, connect external Postgres, import/create realm + client.
- Validate:
  - Health endpoints
  - Full OAuth login/logout flow via public URLs

### Phase 5 — Automate “deploy to staging” from GitHub Actions (1–2 days)
- Add `deploy-staging.yml`:
  1) Build+push images
  2) Call Jelastic API to update image tags to the new SHA
  3) Apply env var updates (if needed)
  4) Run smoke tests (curl + optional Playwright against staging URL)
- Use GitHub Environments (`staging`) for approvals + secret isolation.

### Phase 6 — Prepare IaC (JPS-ready) (half day)
- Add folder: `infrastructure/jelastic/`
  - `infrastructure/jelastic/README.md` (topology + env var contracts)
  - `infrastructure/jelastic/scripts/` (Jelastic API helpers)
  - `infrastructure/jelastic/jps/` (future JPS manifests)
  - `infrastructure/jelastic/env/` (non-secret templates)

