# Local PostgreSQL Docker Setup — Plan Brief

> Full plan: `context/changes/local-postgres-docker/plan.md`
> Research: comprehensive investigation of codebase Docker config, Spring Boot database setup, and development documentation

## What & Why

Set up PostgreSQL in Docker for local development, eliminating manual installation and Fly.io managed database dependency during MVP iteration. Developers start the database with a single command (`docker-compose up -d`). This reduces cloud infrastructure costs while maintaining schema parity with production.

## Starting Point

Spring Boot app expects PostgreSQL on localhost:5432 with Hibernate auto-creating schema on startup. README currently documents three setup options (Homebrew, raw Docker, installer), but no docker-compose.yml exists. Tests already use H2 in-memory (no Docker dependency). This task replaces the ad-hoc setup with a standard docker-compose workflow.

## Desired End State

Developers run `docker-compose up -d` to start PostgreSQL. Data persists across container restarts. Spring Boot connects automatically, Hibernate creates tables on first run. README is updated with a single primary setup instruction, alternatives kept as fallbacks.

## Key Decisions Made

| Decision | Choice | Why | Source |
|----------|--------|-----|--------|
| Containerization scope | PostgreSQL only (Spring Boot native) | Faster iteration, easier debugging; matches codebase pattern where frontend runs native in Vite | Plan |
| Data persistence | Named volume (persists across restarts) | Enables real testing scenarios without seed scripts | Plan |
| PostgreSQL version | Latest stable (not pinned to production) | Simpler maintenance; Hibernate DDL ensures schema compatibility | Plan |
| Development tools | None (skip pgAdmin, adminer) | Keep it minimal; developers use psql CLI or IDE connections | Plan |

## Scope

**In scope:**
- Create `docker-compose.yml` at project root with PostgreSQL service, persistent named volume, environment variables matching Spring Boot defaults
- Update README: promote docker-compose to primary setup option, keep Homebrew/installer as alternatives
- Add troubleshooting documentation (port conflicts, volume cleanup, credential overrides)

**Out of scope:**
- Spring Boot containerization (backend continues running native)
- Database migration framework setup (Hibernate DDL auto handles it)
- pgAdmin or other development tools
- CI/CD changes (tests still use H2 in-memory, workflows unchanged)
- TestContainers setup (outside MVP scope)

## Architecture / Approach

Simple two-phase approach:

1. **Phase 1: Create docker-compose.yml** — Single PostgreSQL service with persistent named volume (`postgres_data`), environment variables matching Spring Boot defaults (postgres/postgres, database: done_yet), port 5432. No health checks (Spring Boot retries on startup).

2. **Phase 2: Update README** — Replace three-step Homebrew/raw-Docker/installer instructions with single `docker-compose up -d` command as primary. Update "Running the Full Stack" section to show three-terminal workflow: Terminal 1 (database), Terminal 2 (backend), Terminal 3 (frontend).

**Why this approach:** Minimal scope (single YAML file + doc update) reduces risk. No code changes to Spring Boot. Tests unaffected. Environment defaults already match Spring Boot config.

## Phases at a Glance

| Phase | What it delivers | Key risk |
|-------|------------------|----------|
| 1. Docker Compose | `docker-compose.yml` with PostgreSQL, named volume, env vars | Port conflict if 5432 already in use; volume cleanup confusion |
| 2. Documentation | README updated with docker-compose as primary setup | Documentation maintenance if workflow changes later |

**Prerequisites:** Docker Desktop installed (assumed for local dev)  
**Estimated effort:** ~1-2 sessions (1-2 hours: write YAML, test connectivity, update README)

## Open Risks & Assumptions

- **Port conflict:** If 5432 already in use locally, docker-compose fails. Mitigated by documenting `netstat` check and optional port override.
- **Volume persistence:** Named volume persists after `docker-compose down`; developer may expect fresh data. Mitigated by troubleshooting section with explicit cleanup command.
- **Default credentials in version control:** postgres/postgres are acceptable for development (standard defaults); production overrides via environment variables handled by Spring Boot.
- **Assumption:** Docker Desktop available on all developer machines (reasonable for MVP team).

## Success Criteria (Summary)

- Fresh developer follows README and starts full stack successfully with docker-compose
- PostgreSQL data persists across container restart
- Existing tests continue to pass (H2 in-memory unchanged)
- No impact to CI/CD pipeline (GitHub Actions workflows unchanged)
