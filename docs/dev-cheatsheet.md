# Development Cheatsheet

## 📋 Quick Reference
A comprehensive guide to all local development, testing, security scanning, and Docker commands.

---

## 🐳 Docker Compose

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

---

## ☕ Backend (Java/Spring Boot)

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
# Run all tests
./mvnw clean verify

# Run only unit tests
./mvnw test

# Run only integration tests
./mvnw verify -DskipUTs=true

# Run with coverage
./mvnw clean test jacoco:report

# Skip all tests
./mvnw clean package -DskipTests

# Skip specific test types
./mvnw package -DskipUTs=true -DskipITs=true
```

### Code Quality & Static Analysis
```powershell
# Run Checkstyle
./mvnw checkstyle:check

# Run PMD
./mvnw pmd:check

# Run SpotBugs + FindSecBugs
./mvnw spotbugs:check

# Run all static analysis (Checkstyle + PMD + SpotBugs)
./mvnw verify -DskipUTs=true -DskipITs=true

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

---

## ⚛️ Frontend (React/TypeScript/Vite)

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

---

## 🔐 Security Scanning (Local)

### Trivy (Container Scanning)
```powershell
# Scan backend Docker image
trivy image demo-backend:latest

# Scan with severity filter
trivy image --severity HIGH,CRITICAL demo-backend:latest

# Generate SARIF report
trivy image --format sarif -o trivy-results.sarif demo-backend:latest

# Scan filesystem
trivy fs --security-checks vuln,config,secret ./backend
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

---

## 🌐 API Governance

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

---

## 🧪 Health Checks & Verification

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

---

## 📊 Reports & Artifacts

### Backend Reports Location
- **JaCoCo Coverage**: `backend/target/site/jacoco/index.html`
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

---

## 🔧 Troubleshooting

### Clear All Caches
```powershell
# Maven
cd backend
./mvnw clean
rm -rf ~/.m2/repository

# Node
cd frontend
rm -rf node_modules package-lock.json
npm install

# Docker
docker system prune -a --volumes
```

### Fix Common Issues
```powershell
# Backend won't start - Port already in use
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Frontend won't start - Port already in use
netstat -ano | findstr :5173
taskkill /PID <PID> /F

# Database connection refused
docker compose restart db

# Testcontainers timeout
# Increase timeout in pom.xml (already set to 60s)

# OpenAPI generation fails
cd backend
./mvnw clean compile -X  # Debug mode

# Docker build context too large
# Check .dockerignore file
```

### View Logs
```powershell
# Backend application logs (Docker)
docker compose logs -f backend

# Backend application logs (local)
tail -f backend/logs/application.log

# Frontend dev server logs
# Displayed in console where npm run dev is running

# PostgreSQL logs
docker compose logs -f db
```

---

## 🚀 CI/CD Pipeline (Local Simulation)

### Run Full Backend CI Locally
```powershell
cd backend

# 1. Unit tests
./mvnw clean test

# 2. Static analysis
./mvnw verify -DskipUTs=true -DskipITs=true

# 3. Dependency check (requires API key)
./mvnw dependency-check:check -Dnvd.api.key=YOUR_KEY

# 4. Generate SBOM
./mvnw package -DskipTests

# 5. Integration tests
./mvnw verify -DskipUTs=true
```

### Run Full Frontend CI Locally
```powershell
cd frontend

# 1. Install dependencies
npm ci

# 2. Lint
npm run lint

# 3. Format check
npm run format:check

# 4. Type check
npm run typecheck

# 5. Unit tests with coverage
npm run test:coverage

# 6. Build
npm run build

# 7. E2E tests
npm run test:e2e
```

### Run Security Scans Locally
```powershell
# SAST (Semgrep via Docker)
docker run --rm -v "${PWD}:/src" semgrep/semgrep semgrep --config "p/default" --config "p/owasp-top-ten" /src

# Secrets (Gitleaks)
gitleaks detect --source=. --no-git

# Container (Trivy) - requires Docker image built
docker compose build backend
trivy image demo-backend:latest --severity HIGH,CRITICAL
```

---

## 📝 Git Workflow

### Commit Message Format
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `ci`, `perf`

**Example**:
```powershell
git commit -m "feat(backend): add user authentication endpoint

- Implement JWT token generation
- Add password encryption with BCrypt
- Include integration tests

Closes #123"
```

### Useful Git Commands
```powershell
# Check status
git status

# Stage all changes
git add -A

# Review changes
git diff
git diff --staged

# Create feature branch
git checkout -b feature/my-feature

# Push to remote
git push origin feature/my-feature

# View commit history
git log --oneline --graph --all

# Amend last commit
git commit --amend

# Interactive rebase (last 3 commits)
git rebase -i HEAD~3
```

---

## 🔑 Environment Variables

### Backend (.env for Docker Compose)
```properties
# Database
DB_NAME=demo
DB_ADMIN_USER=demo
DB_ADMIN_PW=demo
DB_HOST=db
DB_PORT=5432
PG_DATA_PATH=./pgdata

# Backend
BCK_SERVER_PORT=8080
BCK_LOCAL_PORT=8080
```

### Backend (application-local.properties)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/demo
spring.datasource.username=demo
spring.datasource.password=demo
cors.allowed.origins=http://localhost:5173,http://localhost:4173
admin.username=admin
admin.password=localpassword
```

### Frontend (.env.development)
```properties
VITE_API_URL=http://localhost:8080
```

### Frontend (.env.production)
```properties
VITE_API_URL=/api
```

---

## 🎯 Quick Commands Summary

| Task             | Command                                                                              |
| ---------------- | ------------------------------------------------------------------------------------ |
| Start everything | `docker compose up -d --build`                                                       |
| Stop everything  | `docker compose down -v`                                                             |
| Backend tests    | `cd backend && ./mvnw verify`                                                        |
| Frontend tests   | `cd frontend && npm test`                                                            |
| Backend run      | `cd backend && ./mvnw spring-boot:run`                                               |
| Frontend run     | `cd frontend && npm run dev`                                                         |
| Lint backend     | `cd backend && ./mvnw verify -DskipUTs=true -DskipITs=true`                          |
| Lint frontend    | `cd frontend && npm run lint`                                                        |
| Security scan    | `docker run --rm -v "${PWD}:/src" semgrep/semgrep semgrep --config "p/default" /src` |
| Generate SBOM    | `cd backend && ./mvnw package -DskipTests`                                           |
| API client gen   | `cd frontend && npm run api:generate`                                                |
| View coverage    | `cd backend && ./mvnw test jacoco:report`                                            |
| Clean all        | `docker compose down -v --rmi local --remove-orphans`                                |
| Health check     | `curl http://localhost:8081/management/health`                                       |

---

## 🐛 Common Development Workflows

### Starting Fresh Development Session
```powershell
# 1. Pull latest changes
git pull origin main

# 2. Clean and rebuild everything
docker compose down -v --rmi local
docker compose up -d --build

# 3. Start frontend
cd frontend
npm ci
npm run dev
```

### Before Committing Changes
```powershell
# 1. Run backend checks
cd backend
./mvnw clean verify

# 2. Run frontend checks
cd frontend
npm run lint
npm run typecheck
npm run test:coverage

# 3. Check for secrets
gitleaks detect --source=. --no-git

# 4. Stage and commit
git add -A
git status
git commit -m "type(scope): description"
```

### Testing API Changes
```powershell
# 1. Update OpenAPI spec
# Edit: api/specification/openapi.yaml

# 2. Validate spec
cd api
npm run lint

# 3. Regenerate backend code
cd ../backend
./mvnw clean compile

# 4. Regenerate frontend client
cd ../frontend
npm run api:generate

# 5. Run tests
cd ../backend && ./mvnw verify
cd ../frontend && npm test
```

### Debugging Integration Tests
```powershell
cd backend

# Run single test with debug logging
./mvnw test -Dtest=GreetingApiIT -X

# Run with Testcontainers logs
./mvnw verify -DskipUTs=true -Dlogging.level.org.testcontainers=DEBUG

# Keep containers running after test (for inspection)
# Set in test: container.withReuse(true)
```

---

## 📚 Additional Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [Vite Documentation](https://vitejs.dev/)
- [React Documentation](https://react.dev/)
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [Spectral Rulesets](https://docs.stoplight.io/docs/spectral/docs/reference/openapi-rules.md)
- [Trivy Documentation](https://aquasecurity.github.io/trivy/)
- [Semgrep Rules](https://semgrep.dev/explore)
- [OWASP Dependency-Check](https://owasp.org/www-project-dependency-check/)
- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
- [Playwright Documentation](https://playwright.dev/)
- [Vitest Documentation](https://vitest.dev/)

---

## 💡 Tips & Best Practices

### Performance Optimization
- Use `./mvnw -T 1C` for parallel Maven builds (1 thread per CPU core)
- Enable Maven daemon for faster builds: `./mvnw --daemon`
- Use `npm ci` instead of `npm install` in CI and fresh environments
- Cache Docker layers by copying dependency files first

### Security Best Practices
- Never commit `.env` files or secrets
- Rotate NVD API key regularly
- Run security scans before merging to main
- Keep dependencies updated weekly
- Review Dependabot/Renovate PRs promptly

### Testing Best Practices
- Write tests before fixing bugs (TDD)
- Aim for >80% code coverage
- Use Testcontainers for realistic integration tests
- Mock external services in unit tests
- Run E2E tests against production-like environment

### Code Quality
- Run linters before committing (consider pre-commit hooks)
- Follow conventional commit messages
- Keep PRs small and focused (<400 lines)
- Document complex business logic
- Refactor when you see code duplication
