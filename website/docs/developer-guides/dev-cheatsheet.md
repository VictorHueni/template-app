---
sidebar_position: 3
---

# Development Cheatsheet

A comprehensive guide to all local development, testing, security scanning, and Docker commands.

## Docker Compose

### Basic Operations

```powershell
# Start all services (PostgreSQL + Backend)
docker compose up -d

# Start with rebuild
docker compose up -d --build

# Stop all services
docker compose down

# Stop and remove volumes (clean database)
docker compose down -v

# Stop, remove volumes, remove images, and orphan containers
docker compose down -v --rmi local --remove-orphans

# View logs
docker compose logs -f
docker compose logs -f backend
docker compose logs -f db

# Check service status
docker compose ps

# Restart specific service
docker compose restart backend
```

### Database Operations

```powershell
# Connect to PostgreSQL container
docker exec -it demo-postgres psql -U demo -d demo

# Backup database
docker exec demo-postgres pg_dump -U demo demo > backup.sql

# Restore database
docker exec -i demo-postgres psql -U demo demo < backup.sql

docker run --name spring-postgres -p 5432:5432 -e POSTGRES_DB=spring_db -e POSTGRES_USER=spring_user -e POSTGRES_PASSWORD=secure_password -d postgres:18.1-alpine
```

## Backend (Java/Spring Boot)

### Build & Run

```powershell
cd backend

# Build project (skip tests)
./mvnw clean package -DskipTests

# Build with all checks
./mvnw clean install

# Run application (local profile)
./mvnw spring-boot:run

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run built JAR
java -jar target/demo-0.0.1-SNAPSHOT.jar

# Run with specific profile
java -jar target/demo-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### Testing

```powershell
# Run all tests (Unit + Integration)
./mvnw clean verify

# Run only unit tests
./mvnw test

# Run only integration tests (Profile)
./mvnw verify -Pintegration-tests

# Run integration tests in parallel (Profile)
./mvnw verify -Pparallel-its

# Run fast CI build (Unit tests only, skips quality checks & ITs)
./mvnw clean test -Pci-fast

# Run full build (Unit + IT + Quality + Coverage)
./mvnw clean verify -Pfull-build

# Verify Coverage Compliance (Fails if < 80%)
./mvnw verify -Pcoverage-enforce

# Skip all tests
./mvnw clean package -DskipTests
```

### Code Quality & Static Analysis

```powershell
# Run Checkstyle
./mvnw checkstyle:check

# Run PMD
./mvnw pmd:check

# Run SpotBugs + FindSecBugs
./mvnw spotbugs:check

# Run all static analysis (Fast - Uses quality-check profile)
./mvnw clean compile -Pquality-check

# Generate PMD report
./mvnw pmd:pmd
# View: target/site/pmd.html

# Generate SpotBugs report
./mvnw spotbugs:spotbugs
# View: target/spotbugsXml.xml
```

### Security Scanning

```powershell
# OWASP Dependency-Check (requires NVD API key)
./mvnw dependency-check:check -Dnvd.api.key=YOUR_API_KEY

# View report: target/dependency-check-report.html

# Generate SBOM (CycloneDX)
./mvnw cyclonedx:makeAggregateBom
# View: target/sbom/backend-sbom.json

# List dependency tree
./mvnw dependency:tree

# Check for updates
./mvnw versions:display-dependency-updates
```

### OpenAPI Code Generation

```powershell
# Generate server stubs from OpenAPI spec
./mvnw clean compile

# Generated code location: target/generated-sources/openapi/
```

## Frontend (React/TypeScript/Vite)

### Build & Run

```powershell
cd frontend

# Install dependencies
npm ci

# Start dev server (with OpenAPI client generation)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Preview on specific port
npm run preview -- --port 4173
```

### Testing

```powershell
# Run unit tests (Vitest)
npm test

# Run unit tests with coverage
npm run test:coverage
# View: coverage/index.html

# Run E2E tests (Playwright)
npm run test:e2e

# Run E2E tests in CI mode
npm run test:e2e:ci

# Run E2E tests with UI
npx playwright test --ui

# Run specific E2E test
npx playwright test e2e/hello.spec.ts
```

### Code Quality

```powershell
# Lint code
npm run lint

# Type check
npm run typecheck

# Format code
npm run format

# Check formatting (CI mode)
npm run format:check
```

### OpenAPI Client Generation

```powershell
# Generate TypeScript API client
npm run api:generate

# Generated code location: src/api/generated/
```

### Security & Dependencies

```powershell
# Audit dependencies
npm audit

# Fix vulnerabilities
npm audit fix

# Check for outdated packages
npm outdated

# Update dependencies
npm update
```

## Security Scanning (Local)

### Trivy (Container Scanning)

```powershell
# Run Trivy via Docker (recommended - no installation required)
# Note: Requires Docker socket access for image scanning

# Scan backend Docker image
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image template-app-backend:latest

# Scan with severity filter (HIGH and CRITICAL only)
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image template-app-backend:latest --severity HIGH,CRITICAL

# Scan frontend Docker image
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image template-app-frontend:latest --severity HIGH,CRITICAL

# Generate SARIF report
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v "${PWD}:/output" aquasec/trivy image template-app-backend:latest --format sarif -o /output/trivy-results.sarif

# Scan filesystem (source code vulnerabilities)
docker run --rm -v "${PWD}:/src" aquasec/trivy fs /src/backend --scanners vuln,secret,misconfig
```

### Semgrep (SAST)

```powershell
# Run Semgrep via Docker (recommended - no installation required)
docker run --rm -v "${PWD}:/src" semgrep/semgrep semgrep --config "p/default" --config "p/owasp-top-ten" /src

# Scan with rule exclusions (matches CI configuration)
docker run --rm -v "${PWD}:/src" semgrep/semgrep semgrep `
  --config "p/default" `
  --config "p/owasp-top-ten" `
  --exclude-rule "generic.secrets.security.detected-jwt-token.detected-jwt-token" `
  --exclude-rule "generic.nginx.security.possible-h2c-smuggling.possible-nginx-h2c-smuggling" `
  --exclude-rule "generic.nginx.security.request-host-used.request-host-used" `
  --exclude-rule "java.spring.security.audit.spring-actuator-non-health-enabled.spring-actuator-dangerous-endpoints-enabled" `
  /src

# Generate SARIF output
docker run --rm -v "${PWD}:/src" semgrep/semgrep semgrep --config "p/default" --sarif -o /src/semgrep.sarif /src

# Alternative: Install Semgrep locally via pip
pip install semgrep
semgrep --config "p/default" --config "p/owasp-top-ten" .
```

### Gitleaks (Secrets Scanning)

```powershell
# Install Gitleaks (Windows)
# Download from https://github.com/gitleaks/gitleaks/releases

# Scan repository
gitleaks detect --source=. --no-git --redact

# Scan and generate report
gitleaks detect --source=. --report-format sarif --report-path gitleaks.sarif

# Scan specific commit range
gitleaks detect --log-opts="HEAD~10..HEAD"
```

### ZAP (DAST - Dynamic Application Security Testing)

```powershell
# Start ZAP daemon
docker run -u zap -p 8090:8090 -i ghcr.io/zaproxy/zaproxy:stable zap.sh -daemon -host 0.0.0.0 -port 8090 -config api.addrs.addr.name=.* -config api.addrs.addr.regex=true -config api.key=changeMe

# Run baseline scan (requires running backend)
docker run --rm --network host `
  -v ${PWD}:/zap/wrk/:rw `
  ghcr.io/zaproxy/zaproxy:stable `
  zap-baseline.py `
  -t http://localhost:8080 `
  -r zap-report.html `
  -J zap-report.json `
  -c .zap/rules.tsv
```

## API Governance

### OpenAPI Validation

```powershell
cd api

# Install tools
npm ci

# Lint OpenAPI specification
npm run lint

# Spectral generates errors in: api/specification/openapi.yaml

# Validate specific rules
npx @stoplight/spectral-cli lint specification/openapi.yaml --ruleset .spectral.yaml
```

### Breaking Change Detection

```powershell
# Install openapi-diff
npm install -g openapi-diff

# Compare with main branch
git show main:api/specification/openapi.yaml > /tmp/main-spec.yaml
openapi-diff /tmp/main-spec.yaml api/specification/openapi.yaml
```

## Health Checks & Verification

### Backend Health

```powershell
# Check application health (actuator runs on separate port 8081)
curl http://localhost:8081/management/health

# Detailed health (when authorized)
curl http://localhost:8081/management/health -u admin:localpassword

# Check info endpoint
curl http://localhost:8081/management/info

# Test greeting endpoint (GET with pagination)
curl "http://localhost:8080/api/v1/greetings?page=0&size=10"

# Create greeting (POST)
curl -X POST http://localhost:8080/api/v1/greetings `
  -H "Content-Type: application/json" `
  -d '{"message":"Hello World"}'
```

### Database Connection

```powershell
# From Docker Compose
docker exec demo-postgres psql -U demo -d demo -c "SELECT version();"

# Check connectivity
docker exec demo-postgres pg_isready -U demo -d demo

# List all tables
docker exec demo-postgres psql -U demo -d demo -c "\dt"

# Query greetings table
docker exec demo-postgres psql -U demo -d demo -c "SELECT * FROM greetings;"
```

### Frontend

```powershell
# Check if dev server is running
curl http://localhost:5173

# Check production build
curl http://localhost:4173
```

## Reports & Artifacts

### Backend Reports Location

- **JaCoCo Coverage**: `backend/target/site/jacoco/index.html`
- **Spring Modulith Docs**: `backend/target/spring-modulith-docs/`
- **Surefire (Unit Tests)**: `backend/target/surefire-reports/`
- **Failsafe (Integration Tests)**: `backend/target/failsafe-reports/`
- **Checkstyle**: `backend/target/checkstyle-result.xml`
- **PMD**: `backend/target/pmd.xml` & `backend/target/reports/pmd.html`
- **SpotBugs**: `backend/target/spotbugsXml.xml`
- **OWASP Dependency-Check**: `backend/target/dependency-check-report.html`
- **SBOM**: `backend/target/sbom/backend-sbom.json`
- **OpenAPI Generated Code**: `backend/target/generated-sources/openapi/`

### Frontend Reports Location

- **Vitest Coverage**: `frontend/coverage/index.html`
- **Playwright Reports**: `frontend/playwright-report/index.html`
- **ESLint Output**: Console
- **TypeScript Errors**: Console
- **OpenAPI Generated Client**: `frontend/src/api/generated/`

## Quick Commands Summary

| Task             | Command                                                                              |
| ---------------- | ------------------------------------------------------------------------------------ |
| Start everything | `docker compose up -d --build`                                                       |
| Stop everything  | `docker compose down -v`                                                             |
| Backend tests    | `cd backend && ./mvnw verify`                                                        |
| Frontend tests   | `cd frontend && npm test`                                                            |
| Backend run      | `cd backend && ./mvnw spring-boot:run`                                               |
| Frontend run     | `cd frontend && npm run dev`                                                         |
| Lint backend     | `cd backend && ./mvnw clean compile -Pquality-check`                                 |
| Lint frontend    | `cd frontend && npm run lint`                                                        |
| Security scan    | `docker run --rm -v "${PWD}:/src" semgrep/semgrep semgrep --config "p/default" /src` |
| Generate SBOM    | `cd backend && ./mvnw package -DskipTests`                                           |
| API client gen   | `cd frontend && npm run api:generate`                                                |
| View coverage    | `cd backend && ./mvnw test jacoco:report`                                            |
| Clean all        | `docker compose down -v --rmi local --remove-orphans`                                |
| Health check     | `curl http://localhost:8081/management/health`                                       |
