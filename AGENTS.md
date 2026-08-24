# Repository Guidelines

Fullstack: TypeScript CLI + Spring Boot backend. Phase-keyed stubs; check `thoughts/shared/plans/2026-04-07-10x-cli-design.md` in sibling `10x-toolkit` repo for roadmap.

## Critical Rules

**API result handling.** `src/lib/api-client.ts` returns discriminated `ApiResult<T>`. Always branch on `ok` field. Surface failures via `outputError()`, never `process.exit()`. Exit codes: `0` SUCCESS, `1` ERROR, `2` USAGE, `3` AUTH_REQUIRED, `4` FORBIDDEN, `5` NOT_FOUND.

**JSON mode is implicit.** When stdout is not a TTY, JSON output is automatic—no `--json` flag needed. Commands never check flags directly; use `resolveContext()`.

**Never hand-edit** `src/generated/api-types.ts` — regenerated from `/openapi.json`. Index access returns `T | undefined` (handle it).

**API allowlist is strict.** `resolveApiBase()` accepts only: production host or `http://localhost` / `http://127.0.0.1` (any port). To add staging: edit `PROD_HOSTNAME` / `DEV_HOSTNAMES` in `src/lib/api-client.ts`.

## Where Code Lives

| Component | Location |
|-----------|----------|
| CLI commands | `src/commands/<name>.ts` exporting `register*Command(cli)`, wired in `src/index.ts` |
| CLI libraries | `src/lib/` (api-client, config, output, conflict-prompt, writer) |
| Frontend | `src/App.tsx`, `src/main.tsx`, `src/*.css` |
| Backend API | `backend/src/main/java/com/example/is_it_done_backend/` |
| CLI tests | `tests/*.test.ts`, `tests/smoke/` (binary) |
| Backend tests | `backend/src/test/java/.../*Tests.java` |

## Naming

**Backend Java:** Package `com.example.is_it_done_backend` (underscores, not hyphens). Controllers/Services/Models follow `*Controller`, `*Service`, `*` (DTO) pattern. Tests mirror source with `*Tests.java` suffix.

## Commands & CI

**Build/test:**
```
bun run dev -- <args>          # CLI from source
bun test                        # CLI tests (bun:test)
bun run generate-types          # Refetch OpenAPI (API_BASE_URL=http://localhost:8787 for local)
cd backend && ./mvnw spring-boot:run    # Backend API (port 8080)
./mvnw test                     # Backend tests
```

**CI gate** (`.github/workflows/ci.yml`): typecheck → lint → test → build (node) → build (binary) → smoke tests. Breakage blocks merge.

---

See @CLAUDE.md for CLI writer architecture, manifest schema, lesson tracking, conflict detection.
