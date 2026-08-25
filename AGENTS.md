# Repository Guidelines

10x-cli distributes 10xDevs course content into workspaces. Core commands: `10x auth`, `10x list`, `10x get <lesson>`, `10x sync`.

## Exit Codes Are Semantic

Use `ExitCodes` enum constants: `0` SUCCESS, `1` ERROR, `2` USAGE, `3` AUTH_REQUIRED, `4` FORBIDDEN, `5` NOT_FOUND. Always call `outputError(ctx, code, message, exitCode, hint)` instead of `process.exit()` — this preserves the JSON error envelope. Commands that call `process.exit` directly will break the JSON contract for machine consumers.

## Commands Are CAC-Registered Functions

Each command exports `register*Command(cli)` from `src/commands/<name>.ts` and is imported + called in `src/index.ts`. The action callback receives positional args followed by an options object (already includes `--json` / `--verbose` flags); pass that directly into `resolveContext()` and `outputError()`.

## Testing: Bun Native Runner Only

Use `import { describe, it, expect, mock } from "bun:test"` — not vitest, not Jest. Module mocks use `mock.module()`, not `vi.mock`. Run `bun test`, not `npm test` or `vitest run`. This is a hard requirement enforced by CI.

## I/O Contract

Stdout is reserved for data (JSON or piped output). Humans read stderr. The `output()` function writes JSON to stdout *or* a message to stderr — never both. JSON mode is auto-detected when stdout is not a TTY (pipes, redirects).

## API Results Are Discriminated Unions

`api-client.ts` returns `ApiResult<T>`: either `{ ok: true, data }` or `{ ok: false, code, error }`. Every caller **must** branch on `ok` and never assume the shape. Surface failures via `outputError()` to maintain JSON consistency.

## Configuration Constraints

**API base URL:** `API_BASE_URL` env var is strictly validated. Only production hostname or `http://localhost` / `http://127.0.0.1` (any port) are allowed. Any other URL is rejected with exit code 2. This is a security boundary.

**Credentials storage:** Written to `$XDG_CONFIG_HOME/10x-cli/auth.json` (Windows: `%APPDATA%/10x-cli/auth.json`). Must be atomic (write to temp file, then `renameSync`) with mode `0o600` to prevent world-readable secrets.

**Manifest versioning:** The manifest is cumulative (never replaced). It stores per-file SHA-256 hashes for conflict detection; a three-way comparison identifies user edits. When the schema changes, bump `AUTH_FILE_VERSION` / manifest version and add migration logic in the read function.

---

See @CLAUDE.md for architecture depth (api-client, config, writer, conflict detection) and @README.md for usage (all commands, flags, quick start).
