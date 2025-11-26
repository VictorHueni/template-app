
## 🎯 Goals & Scope
- [ ] Add a short **Purpose** section explaining what the project does, who it’s for, and what it’s *not*.
- [ ] Add a **Prerequisites** block listing versions (AWS CLI v2, Copilot v1.34+, Node 20+, Java 25, Docker, jq).
- [ ] Add a concise **“What You’ll Deploy”** summary (ECS/Fargate API + S3/CloudFront SPA + optional RDS).

## 🧭 Structure & Navigation
- [ ] Add a top-level **Quickstart** (5–10 essential commands).
- [ ] Move detailed content into `/docs/` (CI/CD, naming, teardown, security).
- [ ] Ensure a clean **Table of Contents** with stable anchors.
- [ ] Add a one-page **Architecture Overview** with a simplified diagram + legend.

## 🔒 Security & Privacy Hygiene
- [ ] Replace all real IDs, ARNs, and URLs with `<PLACEHOLDER>` consistently.
- [ ] Highlight “**Do not commit**” list early (IDs, ARNs, `.env*`, `cloudfront.json`, ALB URLs).
- [ ] Add a `.env.example` file with safe defaults.
- [ ] Clarify that ACM certificates for CloudFront **must be in us-east-1**.

## ⚙️ Copilot-Specific Clarifications
- [ ] Note that `http.public: false` does *not* make an LBWS private — recommend using a **Backend Service**.
- [ ] Provide example of `allowed_source_ips` to restrict public LBWS access.
- [ ] Include actual `healthcheck` path, port, and ALB behavior details.
- [ ] Document CLI commands to get **ALB DNS**, **DIST_ID**, and **OAC_ID**.

## 💻 Frontend (Vite) Clarity
- [ ] Show full `vite.config.ts` with `base: '/'` and dev `proxy` setup.
- [ ] Explain `.env.development` vs `.env.production` usage.
- [ ] Clarify `VITE_` prefix requirement for environment variables.
- [ ] Describe SPA routing fix via CloudFront `CustomErrorResponses` (403/404 → `/index.html`).

## 🌐 CloudFront + S3 Configuration
- [ ] Emphasize S3 **REST** endpoint (`bucket.s3.<region>.amazonaws.com`) with OAC.
- [ ] Include validated **cloudfront.json** that matches AWS CLI schema.
- [ ] Add caching strategy: immutable assets vs `index.html` no-cache.
- [ ] Include **cache invalidation** command and when to use it.

## 🧱 Backend Containerization
- [ ] Keep minimal, secure **Dockerfile** (non-root, healthcheck, BuildKit caching).
- [ ] Add a **Makefile** or scripts for `build`, `run`, and `deploy`.
- [ ] Include test examples: `curl /actuator/health` and `curl /api/hello`.

## 🗄️ Optional RDS Integration
- [ ] Clearly mark RDS as optional in the README.
- [ ] Document Flyway migration flow (local dry-run + ECS task).
- [ ] Note use of private subnets + SG rules (no public access).

## 🚀 CI/CD Workflows
- [ ] Provide **frontend workflow** example (OIDC auth + S3 sync + CF invalidate).
- [ ] Provide **backend workflow** example (ECR push + ECS deploy by digest).
- [ ] Add environments and manual approval for production deployment.
- [ ] Add **release workflow** example (tag + release notes).
- [ ] Add **rollback workflow** example (rollback to previous version).
- [ ] Add **cleanup workflow** example (delete all resources).
- [ ] Add **security workflow** example (security scan).
- [ ] Add **test workflow** example (unit tests).
- [ ] Add **deploy workflow** example (ECS deploy by digest/tag).
- [ ] Add **promote workflow** example (promote to staging).
- [ ] Add **Secret management** 

- [ ] Implement Path Filters in ci.yml: This is the most important change for efficiency and cost control.
- [ ] Add Architectural Testing: Include a Maven plugin (like ArchUnit) or a dedicated test in the backend project and ensure it runs during the backend_unit_tests job.
- [ ] Local Config	- Implement Husky and lint-staged.- Add the Husky and lint-staged NPM packages and configure them to run npm run lint and npm run format on staged files. This prevents hygiene issues from being committed, shifting quality left and saving CI time.

## 📈 Security
- [ ] When SPring Security is compeletely set up, finalize the DAST cofniguration and run a full scan
- [ ] use the JDK 25 distroless image when ever it is ready and bump the java version
- [ ] implement .env in front end for all sensitive data


## 🧩 Troubleshooting & Teardown
- [ ] Add a **Common Errors** section (SSO expired, InvalidIfMatch, OAC in use, SPA 403/404).
- [ ] Include **Teardown Checklist** (Copilot delete → CF disable → S3 delete → OAC delete).
- [ ] Include AWS cost notes for each resource (ALB, CF, S3, ECR, logs).

## 🧾 Consistency & Readability
- [ ] Use consistent placeholders (`<ACCOUNT_ID>`, `<DIST_ID>`, `<ALB_DNS>`, `<REGION>`).
- [ ] Use `bash` syntax in fenced code blocks.
- [ ] Keep commands copy-pasteable; store long JSON in `file://` references.
- [ ] Remove emojis or excessive decoration for enterprise readability.

## 📚 Documentation Hygiene
- [ ] Move detailed content to `/docs/`:
    - `/docs/cheatsheet.md`
    - `/docs/teardown.md`
    - `/docs/troubleshooting.md`
    - `/docs/naming.md`
- [ ] Add `CHANGELOG.md`, `VERSIONING.md`, and `LICENSE`.
- [ ] Add minimal `CONTRIBUTING.md` with pull request and security disclosure info.

## ✅ Verification Steps
- [ ] Add short **verify commands** after each section (e.g., `aws … --query …`, `curl …`, `copilot svc status`).
- [ ] Add a final **end-to-end test checklist**:
    - SPA loads via CloudFront
    - `/api/hello` returns JSON
    - ALB healthchecks return 200

## 💡 Optional Enhancements
- [ ] Add cleaned-up **Mermaid** diagrams (no real resource names).
- [ ] Provide `scripts/` folder with helper scripts:
    - `deploy-frontend.sh`
    - `update-cloudfront.sh`
    - `teardown.sh`
- [ ] Add **Glossary** for AWS/Copilot terms (OAC, LBWS, ECR, etc.).
- [ ] Add **FAQ** for recurring issues (ACM region, OAC vs OAI, ECS vs App Runner).
