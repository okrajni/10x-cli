---
status: implementing
created: 2026-08-31
updated: 2026-08-31
---

# Local PostgreSQL Docker Setup

**ID:** local-postgres-docker  
**Roadmap item:** F-03 (Foundation)  
**PRD refs:** —

## Description

Create a `docker-compose.yml` to run PostgreSQL locally for development, replacing manual installation and eliminating Fly.io managed database dependency during MVP iteration. Developers start the database with a single command (`docker-compose up -d`), and data persists across container restarts. Update README to make docker-compose the recommended setup path.

## Timeline

Foundation-level work, can run in parallel with other foundational work. Unblocks all downstream slices (S-01 through S-07) for local development.

## Related Work

Complements household schema cleanup (`remove-multi-user-scaffold`) by keeping the codebase lean before launch. Reduces infrastructure costs during development phase as mentioned in the MVP planning roadmap.
