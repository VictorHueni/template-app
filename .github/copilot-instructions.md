# Copilot Instructions for Template-App

## Architecture Overview

Monorepo with **API-First** design: `api/` (OpenAPI spec) → `backend/` (Spring Boot) → `frontend/` (React/Vite). The OpenAPI specification is the single source of truth—never modify generated code directly.

**Key Modules:**

- `api/specification/openapi.yaml` - Contract definition (modify here first)
- `backend/` - Java 25 + Spring Boot 4, feature-based packages under `com.example.demo.{feature}/`
- `frontend/` - React 19 + TypeScript, feature-based structure under `src/features/`
- `website/docs/` - Arc42 documentation (Docusaurus)

## Critical Workflows

### Adding/Modifying API Endpoints

```bash
# 1. Edit api/specification/openapi.yaml first
# 2. Backend: regenerate interfaces
cd backend && ./mvnw clean install
# 3. Frontend: regenerate client
cd frontend && npm run api:generate
# 4. Implement the generated interface (never edit generated files)
```

### Running Development Environment

```bash
# Backend (requires PostgreSQL via docker-compose)
docker-compose up db -d
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Frontend with real backend
cd frontend && npm run dev

# Frontend with mock API (no backend needed)
cd frontend && npm run dev:mock
```

### Testing

```bash
# Backend: Unit tests (*Test.java) + Integration tests (*IT.java with Testcontainers)
cd backend && ./mvnw test                    # Unit tests only
cd backend && ./mvnw verify                  # All tests including IT

# Frontend
cd frontend && npm test                      # Vitest unit tests
cd frontend && npm run test:e2e:mock         # Playwright with Prism mock
```

## Project Conventions

### Backend (Java/Spring Boot)

- **Package by feature**: `com.example.demo.greeting.{controller,service,repository,mapper,model}`
- **Dependency injection**: Constructor injection with `@RequiredArgsConstructor` + `private final` fields. Field `@Autowired` is forbidden.
- **DTOs**: Are Generated from OpenAPI spec.
- **Entity mapping**: MapStruct is mandatory—no manual DTO↔Entity conversion
- **Controllers**: Implement generated `*Api` interfaces, delegate to services, never access repositories directly
- **Services**: Accept/return entities only, never DTOs. Define `@Transactional` boundaries here.
- **Test naming**: `*Test.java` (unit), `*IT.java` (integration with Testcontainers)

### Frontend (React/TypeScript)

- **Feature modules**: `src/features/{feature}/components/`, `hooks/`
- **API access**: Always through custom hooks (e.g., `useGreetings`), never call generated API functions directly in components
- **Generated code**: `src/api/generated/` is READ-ONLY—regenerate with `npm run api:generate`
- **No `any` types**: Use generated types (`GreetingResponse`, `CreateGreetingRequest`)

### Database & IDs

- **Dual-ID strategy**: TSID (technical, internal PK) + Functional ID (human-readable, e.g., `GRE-2025-000042`)
- **Naming**: `snake_case` for tables/columns, `{module}_{entity}` format (e.g., `auth_user`)
- **Constraints**: Explicit names—`pk_`, `fk_`, `uk_`, `ix_` prefixes

## Key Files Reference

| Purpose                    | Location                                                                                                |
| -------------------------- | ------------------------------------------------------------------------------------------------------- |
| API Contract               | [api/specification/openapi.yaml](../api/specification/openapi.yaml)                                     |
| Backend coding standards   | [backend/CODING_GUIDELINES.md](../backend/CODING_GUIDELINES.md)                                         |
| Full AI context            | [GEMINI.md](../GEMINI.md)                                                                               |
| Feature example (backend)  | [backend/src/main/java/com/example/demo/greeting/](../backend/src/main/java/com/example/demo/greeting/) |
| Feature example (frontend) | [frontend/src/features/greetings/](../frontend/src/features/greetings/)                                 |

## Documentation Protocol

**Docs as Code** - Documentation is versioned and reviewed in PRs. No feature is complete without docs.

### New Feature Workflow

1. **Check PRD**: Look in `website/docs/product/specs/` for existing requirements
2. **Create PRD if missing**: Use template at `website/docs/product/templates/prd-template.md`
3. **Plan before coding**: For API changes, draft YAML first and get approval
4. **Update docs on completion**:
   - Architecture changes → `website/docs/architecture/`
   - New configs/secrets → `website/docs/operations/`
   - API changes → Examples in `openapi.yaml`

### Architecture Decision Records (ADRs)

- Location: `website/docs/architecture/09-architecture-decisions/`
- When: Database choice, framework selection, major pattern changes
- Format: MADR template with "References" section linking to authoritative sources

### Diagrams

- **Mermaid.js only** - Binary images (PNG/JPG) are prohibited for architecture diagrams
- Embed diagrams directly in markdown files

## Quality Gates

- **Backend**: Checkstyle (Google style), SpotBugs, JaCoCo (>80% coverage)
- **Frontend**: ESLint, Prettier, TypeScript strict mode
- **Security**: Gitleaks, Semgrep scans in CI
- **Docs**: No feature is done without updating `website/docs/` (architecture, operations, or developer guides)
