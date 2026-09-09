# Local PostgreSQL Docker Setup — Implementation Plan

## Overview

Create a `docker-compose.yml` to run PostgreSQL locally, replacing manual installation and Fly.io managed database dependency during development. This eliminates cloud infrastructure costs during MVP iteration while maintaining schema parity with production.

## Current State Analysis

**What exists today:**
- README documents three PostgreSQL setup options: Homebrew, raw Docker (`docker run`), or installer
- Spring Boot app expects `jdbc:postgresql://localhost:5432/done_yet` with credentials (defaults: postgres/postgres)
- Hibernate `create-drop` DDL auto-creates schema on startup from JPA entities
- Production: Fly.io managed PostgreSQL via GitHub Actions CI/CD
- Tests use H2 in-memory database (no Docker dependency)
- No `docker-compose.yml` exists yet
- Multi-stage production Dockerfile for Java app (separate concern)

**Constraints:**
- Must retain compatibility with existing Spring Boot configuration (application.properties)
- Must not interfere with CI/CD pipeline (tests still run via H2 in-memory)
- Must support the three-terminal development workflow (backend on 8080, frontend on 5173, CLI dev)

## Desired End State

Developers can start PostgreSQL with a single command:
```bash
docker-compose up -d
```

The database is ready for Spring Boot to connect immediately, with data persisting across container restarts. README is updated with a single setup instruction: "Run docker-compose up, then start backend."

**Verification:** Spring Boot connects successfully on first run, Hibernate creates tables automatically, developer can insert test data via API and see it survive a container restart.

## Key Discoveries

- Spring Boot PostgreSQL driver already included (v42.7.2 in pom.xml)
- Fly.io uses PostgreSQL (infrastructure.md recommends latest stable)
- No migration framework (Flyway/Liquibase) — purely Hibernate DDL auto
- Tests already isolated from PostgreSQL (use H2 in-memory)
- Infrastructure documentation exists but focuses on Fly.io deployment, not local dev

## What We're NOT Doing

- Containerizing the Spring Boot app (backend runs native with `mvn spring-boot:run` for faster iteration)
- Adding pgAdmin or other development tools (developers use `psql` CLI or IDE connections)
- Pinning PostgreSQL version to match production exactly (use latest stable; Hibernate DDL ensures schema compatibility)
- Creating database migration scripts (Hibernate DDL auto handles all schema creation)
- Adding TestContainers or modifying CI/CD (tests continue using H2 in-memory, unchanged)

## Implementation Approach

**Two phases, based on user decisions:**

1. **Create `docker-compose.yml`** with PostgreSQL service, persistent named volume, and environment variables matching Spring Boot defaults
2. **Update README documentation** with clear one-step setup instruction

**Why this approach:**
- Minimal scope (single YAML file + doc update) reduces risk
- Persistent volume enables real testing scenarios without seed scripts
- Environment defaults already match Spring Boot, no code changes needed
- Tests continue working unchanged (they use H2)

---

## Phase 1: Create Docker Compose Configuration

### Overview

Write `docker-compose.yml` at the project root with PostgreSQL service, persistent named volume, and environment variables matching Spring Boot's database expectations.

### Changes Required

#### 1. Docker Compose File

**File**: `docker-compose.yml` (create at project root, alongside fly.toml and pom.xml)

**Intent**: Define PostgreSQL service with configuration that aligns with Spring Boot's database expectations (localhost:5432, database name `done_yet`, credentials postgres/postgres). Use a named volume so data persists across container lifecycle.

**Contract**: 
- Service name: `postgres`
- Port mapping: 5432 (host) → 5432 (container)
- Environment variables: `POSTGRES_PASSWORD=postgres`, `POSTGRES_DB=done_yet`, `POSTGRES_USER=postgres`
- Volume: Named volume `postgres_data` mounted at `/var/lib/postgresql/data` (persists on disk)
- Image: `postgres:latest` (latest stable)
- No health checks required (Spring Boot retries on startup if DB not ready)

### Success Criteria

#### Automated Verification

- Migration applies cleanly: `docker-compose config` returns valid YAML
- Container starts: `docker-compose up -d` completes without errors
- Port mapping works: `docker ps` shows postgres service on port 5432
- Database connection: `psql -h localhost -U postgres -d done_yet -c "SELECT 1"` returns 1
- Volume exists: `docker volume ls` shows `postgres_data`

#### Manual Verification

- Spring Boot connects: `mvn spring-boot:run` shows Hibernate creating tables
- Test data insertion: Create task via API, verify it's in the database
- Persistence test: `docker-compose restart postgres` and verify data survives
- No regressions: Existing tests still pass with H2 in-memory database

---

## Phase 2: Update Documentation

### Overview

Update README to make docker-compose the recommended setup path, replacing the raw `docker run` command as the default.

### Changes Required

#### 1. README Database Setup Section

**File**: `README.md` (section: "Database Setup")

**Intent**: Replace three separate installation options with docker-compose as the primary recommendation. Keep alternatives but clearly mark docker-compose as preferred.

**Contract**: New structure:
- **Recommended: Docker Compose** — `docker-compose up -d` (new primary path)
- **Alternatives** — Homebrew and installer remain as fallbacks
- Add quick test: `psql -h localhost -U postgres -d done_yet -c "SELECT 1"` to verify connection

#### 2. Running the Full Stack Section

**File**: `README.md` (section: "Running the Full Stack")

**Intent**: Clarify the three-terminal development workflow with docker-compose as the database layer.

**Contract**: Update to show:
- Terminal 1: `docker-compose up -d` (database in background)
- Terminal 2: `mvn spring-boot:run` (backend)
- Terminal 3: `cd frontend && npm run dev` (frontend)

### Success Criteria

#### Automated Verification

- README markdown validates: No syntax errors
- No broken links: All references resolve correctly
- Commands syntactically correct: `docker-compose up -d` appears correctly

#### Manual Verification

- Fresh developer follows README: Successfully starts full stack from instructions
- Documentation renders cleanly: No formatting artifacts in markdown viewer

---

## Testing Strategy

### Integration Test

1. **Fresh start scenario**:
   - Clean checkout
   - `docker-compose up -d` → verify PostgreSQL starts
   - `mvn spring-boot:run` → verify app connects, schema created
   - Hit a test API endpoint (e.g., `/health`) → verify response

2. **Persistence test**:
   - Create a task via API
   - `docker-compose restart postgres`
   - Verify task still exists in database

3. **Cleanup test**:
   - `docker-compose down`
   - `docker volume rm postgres_data` (optional explicit cleanup)
   - Next `docker-compose up` starts fresh

### CI/CD Verification

- Unit tests: `bun test` — pass unchanged (still use H2)
- E2E tests: `bun test tests/e2e/` — pass unchanged (don't depend on docker-compose)
- GitHub Actions workflows — no changes needed (CI still uses H2, not docker-compose)

---

## Edge Cases & Risks

**Port conflict:** If 5432 already in use locally (another PostgreSQL instance running), docker-compose fails to start.
- **Mitigation**: Document in README troubleshooting: `netstat -an | grep 5432` to check; optionally use different port (e.g., `5433:5432` in docker-compose)

**Volume persistence on cleanup:** Named volume persists after `docker-compose down`. Developer may expect fresh data on next startup.
- **Mitigation**: Add README troubleshooting section: "To start fresh, run `docker volume rm postgres_data` before `docker-compose up`"

**Default credentials in version control:** postgres/postgres are not secrets for development (standard defaults).
- **Acceptable**: These are development-only; environment variables in Spring Boot can override for production scenarios

---

## References

- **Spring Boot config**: `src/main/resources/application.properties` (database URL: localhost:5432, DDL auto mode)
- **pom.xml**: PostgreSQL driver (v42.7.2), Spring Data JPA, H2 for tests
- **README.md**: Current setup instructions, three-terminal dev workflow
- **Infrastructure doc**: `context/foundation/infrastructure.md` (deployment reference)
- **Related task**: Household schema cleanup (`remove-multi-user-scaffold`) — complements this by keeping schema simple

---

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Create Docker Compose Configuration

#### Automated

- [x] 1.1 docker-compose config validates without errors — b538b86
- [ ] 1.2 PostgreSQL container starts and listens on port 5432
- [ ] 1.3 Database credentials work (psql connection succeeds)
- [ ] 1.4 Named volume created (docker volume ls shows postgres_data)
- [ ] 1.5 Volume mounted correctly

#### Manual

- [ ] 1.6 Spring Boot connects and creates schema on startup
- [ ] 1.7 Test data persists across container restart

### Phase 2: Update Documentation

#### Automated

- [x] 2.1 README markdown validates (no syntax errors) — b538b86
- [x] 2.2 Links are correct — b538b86

#### Manual

- [ ] 2.3 Fresh developer follows README and starts full stack successfully
- [ ] 2.4 Documentation rendering is clean (tested in markdown viewer)
