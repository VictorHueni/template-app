---
id: overview
title: Local Dev - Level 1
sidebar_label: Overview (Level 1)
sidebar_position: 1
---

# Local Deployment (Level 1)

This section describes the **Local Development Environment**, which uses Docker Compose to orchestrate the entire system on a developer's workstation.

## Context

The goal of this deployment is to provide a high-fidelity replica of the production architecture while maintaining ease of use for developers. It uses a bridge network to isolate internal traffic and exposes specific ports for host access.

## Deployment Diagram

```plantuml
@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Deployment.puml

LAYOUT_WITH_LEGEND()

title Deployment View - Local Docker Environment

Deployment_Node(computer, "Developer Workstation", "Windows/Linux/Mac OS", "The host machine running Docker Desktop/Engine") {
    
    Deployment_Node(docker, "Docker Host", "Docker Engine") {
        
        Deployment_Node(network, "backend-network", "Docker Bridge Network", "Internal isolation") {
            
            Container(frontend, "Frontend", "Nginx + React (Vite)", "Serves the SPA on port 80. Proxies /api to Gateway.")
            
            Container(gateway, "Gateway (BFF)", "Spring Cloud Gateway", "Entry point (8080). Handles OAuth2, Token Relay, and Routing.")
            
            Container(backend, "Backend API", "Spring Boot 4", "Resource Server (8081). Business logic.")
            
            Container(keycloak, "Keycloak", "Keycloak 26", "Identity Provider (8080 int / 9000 ext).")
            
            ContainerDb(db, "PostgreSQL", "Postgres 18", "Primary Database (5432).")
            
            Container(docs, "Documentation", "Docusaurus", "Project Documentation (3001).")
        }
        
        Deployment_Node(volume, "Volumes", "Docker Volume") {
            Container(vol_db, "db_data", "File System", "Persists PostgreSQL data")
        }
    }
}

' Relationships
Rel(frontend, gateway, "Proxies API requests", "HTTP/JSON")
Rel(gateway, backend, "Forwards with JWT", "HTTP/8081")
Rel(gateway, keycloak, "Auth flows (OIDC)", "HTTP/8080")
Rel(backend, db, "Reads/Writes", "JDBC/5432")
Rel(keycloak, db, "Persists User Data", "JDBC")

' Volume mapping
Rel(db, vol_db, "Mounts", "/var/lib/postgresql/data")

@enduml
```

## Motivation

*   **Isolation:** The `backend-network` ensures that containers communicate via their internal DNS names (`backend`, `keycloak`) rather than relying on `localhost`, mimicking a real distributed network.
*   **Security:** The **BFF (Backend for Frontend)** pattern is enforced even locally. The Frontend container cannot "skip" the Gateway to reach the Backend for authenticated requests; it must follow the proper routing path.
*   **Persistence:** The `db_data` volume ensures that database state (users, greetings) survives container restarts, allowing for long-running development sessions.

## Quality Features

*   **Reproducibility:** A single `docker-compose up` command spins up the entire stack.
*   **Parity:** Configuration injection via `.env` files mimics the production secret injection mechanism.
*   **Observability:** All containers log to `stdout/stderr`, accessible via `docker-compose logs`.