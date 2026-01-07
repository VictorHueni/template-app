---
title: Local Dev - Level 2
sidebar_label: Details (Level 2)
sidebar_position: 2
---

# Local Deployment Details (Level 2)

This section provides the technical specifications for the containers running in the local environment.

## Container Mapping

| Service Name | Container Name | Host Port | Internal Port | Image / Build Context | Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **gateway** | `template-gateway` | `8080` | `8080` | `gateway/Dockerfile` | Main entry point (BFF). Routing & Auth. |
| **backend** | `demo-backend` | `8081` | `8081` | `backend/Dockerfile` | Resource Server. Business Logic. |
| **keycloak** | `template-keycloak` | `9000` | `8080` | `quay.io/keycloak/keycloak:26.0` | Identity Provider. |
| **frontend** | `demo-frontend` | `80` | `80` | `frontend/Dockerfile` | SPA serving static assets via Nginx. |
| **db** | `demo-postgres` | `5432` | `5432` | `postgres:18.1-alpine` | Primary data store. |
| **docs** | `demo-docs` | `3001` | `3001` | `website/Dockerfile` | Project documentation portal. |

## Network Configuration

All services are connected to the `backend-network` (Bridge Driver).

*   **Internal DNS:** Containers address each other by service name (e.g., Gateway connects to `http://backend:8081`).
*   **Host Access:** Developers access the system via `http://localhost:8080` (Gateway). Direct access to Backend (`:8081`) or Keycloak (`:9000`) is possible for debugging but bypassed in standard flows.

## Persistence & Volumes

| Volume Name | Target Container | Mount Path | Purpose |
| :--- | :--- | :--- | :--- |
| `db_data` | `db` | `/var/lib/postgresql/data` | Persists PostgreSQL data files so user accounts and data remain after restart. |
| `keycloak/import` | `keycloak` | `/opt/keycloak/data/import` | (Bind Mount) Loads the `template-realm.json` configuration on startup. |

## Configuration Injection

Configuration is managed via the root `.env` file and injected as Environment Variables.

| Variable | Target Service | Purpose |
| :--- | :--- | :--- |
| `KC_CLIENT_SECRET` | `gateway` | OAuth2 Client Secret for authentication. |
| `SPRING_DATASOURCE_URL` | `backend` | Database connection string. |
| `KEYCLOAK_ADMIN` | `keycloak` | Admin credentials for initial setup. |
