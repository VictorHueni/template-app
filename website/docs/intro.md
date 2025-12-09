---
sidebar_position: 1
---

# Template App Documentation

Welcome to the Template App documentation. This monorepo contains a full-stack application with:

- **Backend**: Spring Boot (Java 25) with clean architecture
- **Frontend**: React 19 + Vite + TypeScript
- **API Contract**: OpenAPI 3.1 specification with governance
- **Infrastructure**: Docker Compose for local development

## Quick Start

```bash
# Start all services
docker compose up -d --build

# Access the application
# Frontend: http://localhost:80
# Backend API: http://localhost:8080/api
# API Docs: http://localhost:3001
```

## Documentation Sections

- [Architecture](./architecture) - High-level system design and backend clean architecture
- [Development Cheatsheet](./dev-cheatsheet) - Commands for development, testing, and debugging
- [References](./references) - AWS and external documentation links
- [API Reference](/api-reference) - Interactive OpenAPI documentation
