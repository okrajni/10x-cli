# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

Early scaffold for the `@przeprogramowani/10x-cli`. Most commands are deliberate stubs that exit via `exitNotImplemented` and reference the phase in which they land. The full roadmap lives at `thoughts/shared/plans/2026-04-07-10x-cli-design.md` (in the sibling `10x-toolkit` repo, not in this one). When asked to implement something, check that plan first to understand which phase the work belongs to and what envelope/exit-code conventions apply.

## Commands

Runtime is **Bun** (≥ Node 20 declared in `package.json` for the published binary, but local dev uses Bun directly).

```bash
bun install
bun run dev -- <args>      # run CLI from source, e.g. `bun run dev -- --help`
bun run typecheck          # tsc --noEmit
bun run lint               # oxlint (config in .oxlintrc.json)
bun test                   # bun:test runner; tests live in tests/
bun test tests/smoke.test.ts   # single file
bun run build              # node-target ESM bundle → dist/index.mjs
bun run build:binary       # standalone compiled binary → dist/10x (~59MB)
bun run generate-types     # refetch /openapi.json → src/generated/api-types.ts
```

`generate-types` hits the production delivery API by default. To regenerate against a local backend: `API_BASE_URL=http://localhost:8787 bun run generate-types`. The same env var is read at CLI runtime by `resolveApiBase()` to point the CLI at a non-production API. **The allowlist is strict**: only the exact production host or `http://localhost` / `http://127.0.0.1` (any port) are accepted — any other URL throws and exits 2. If you need a staging host, add it explicitly to `PROD_HOSTNAME` / `DEV_HOSTNAMES` in `src/lib/api-client.ts`.

CI (`.github/workflows/ci.yml`) runs typecheck → lint → test → build → build:binary on every PR. Anything that breaks one of those steps will block merge.

## Architecture

The CLI is a thin **CAC**-based command dispatcher (`src/index.ts`) that wires command modules into a single `cac("10x")` instance and parses argv. Three concerns are factored into `src/lib/`:

- **`api-client.ts`** — typed `fetch` wrapper for the 10x-toolkit delivery API. Returns a discriminated `ApiResult<T>` (`{ ok: true, data }` | `{ ok: false, code, error }`) — callers **must** branch on `ok` and surface failures via `outputError`. Network errors collapse to `code: "network_error"`, status `0`. The HTTP surface is described by `src/generated/api-types.ts`, which is generated from `/openapi.json` and committed to git; never hand-edit it.
- **`config.ts`** — XDG-compliant local credential store at `$XDG_CONFIG_HOME/10x-cli/auth.json` (Windows: `%APPDATA%/10x-cli/auth.json`). `saveAuth` writes atomically via `tmp` + `renameSync` with mode `0o600`, and `AuthData` is versioned (`AUTH_FILE_VERSION = 1`) — bumping the schema means bumping the version and handling the older payload in `readAuth`.
- **`output.ts`** — the I/O contract every command must follow. Three rules to internalize:
  1. **Stdout is reserved for data; humans read stderr.** `output()` writes JSON to stdout *or* a human message to stderr — never both.
  2. **JSON mode is implied when stdout is not a TTY**, even without `--json`. `resolveContext()` handles this; commands should always go through it instead of checking flags directly.
  3. **Exit codes are semantic** (`ExitCodes`): `0` SUCCESS, `1` ERROR, `2` USAGE, `3` AUTH_REQUIRED, `4` FORBIDDEN, `5` NOT_FOUND. Use `outputError(ctx, code, message, exitCode, hint)` rather than `process.exit` ad-hoc, so the JSON envelope `{ status: "error", error: { code, message, hint } }` stays consistent.

- **`conflict-prompt.ts`** — interactive conflict resolution for user-edited files. `createConflictResolver(tty)` returns a `ConflictResolver` callback injected into `applyBundle()`. TTY mode shows a per-file `@clack/prompts` select with overwrite / save-as-.user / skip / apply-to-all options. Non-TTY mode returns `"skip"` unconditionally — user work is never silently destroyed in pipelines.

Each command in `src/commands/` exports a `register*Command(cli)` function that attaches itself to the shared CAC instance. Adding a new command means: create `src/commands/foo.ts` exporting `registerFooCommand`, import + call it in `src/index.ts`. Action callbacks receive their positional args followed by an options object that already includes the global `--json` / `--verbose` flags — pass that object straight into `resolveContext` / `outputError`.

Stub commands intentionally call `exitNotImplemented(name, phase, options)` so machine consumers still get a parseable error envelope. When implementing a phase, replace that call rather than working around it.

## Testing

- Test runner: `bun test` (not vitest, not Jest)
- Imports: `import { describe, it, expect, mock } from "bun:test"`
- Module mocks: use `mock.module()` from `bun:test`, not `vi.mock`
- Prefer dependency injection over module mocking where possible
- Run tests with `bun test`, not `vitest run` or `npx jest`

## Writer & conflict detection

`applyBundle()` in `writer.ts` is **async** and accepts an optional `onConflict: ConflictResolver` callback via `ApplyOptions`. The writer uses three-way hash comparison to detect user-edited files:

- The manifest (v3) stores per-file SHA-256 content hashes (`contentHashes` for skills, `promptHashes` for prompts).
- On re-apply, if local content differs from both the stored hash and the new bundle content, it's a user edit → the `onConflict` callback is invoked.
- If local content differs from the stored hash but matches the new content → `"unchanged"` (no conflict).
- If local content matches the stored hash → clean upstream update, no conflict.
- When no `onConflict` callback is provided, conflicts default to `"skip"` (safe).
- Manifest v2 (no hashes) is accepted at read time; any content difference on first apply triggers a conflict prompt (one-time calibration). After resolution, v3 hashes are stored.

`WriteResult` includes a `removals` field tracking files deleted during lesson-scoped cleanup. These render as `[removed]` lines in human output and appear in the JSON envelope under `writes.removals` with `counts.removals`.

Conflict actions: `"conflict_overwritten"` | `"conflict_saved_user"` (creates `.user.<ext>` backup) | `"conflict_skipped"` (preserves local, does NOT update manifest hash so conflict re-triggers on next apply).

`applyBundle` takes an `applyCourseRules?: boolean` (default `true`). When `false`, the course rules block (the `@przeprogramowani/10x-cli` sentinel section) is not written and any existing one is **stripped** from the rules file (surrounding content preserved, `rules.action: "removed"`). The CLI exposes this via `--no-course-rules` / `--course-rules` on `get`, persists the choice as `courseRules` in `config.json` (merge-safe via `updateToolConfig`), and resolves it tri-state — argv flag > persisted config > default-on (CAC can't distinguish default-on from explicit `--course-rules`, hence the argv peek in `resolveCourseRulesFlag`). An explicit `--type rules` request forces apply regardless of the setting. Rules are sentinel-based, not manifest-tracked, so opting out needs no manifest changes.

## Cumulative manifest & lesson-scoped removal

The manifest is **cumulative** — each `10x get` accumulates artifacts across lessons instead of replacing them. The manifest's `lessons` field (`Record<string, LessonFilesEntry>`) tracks per-lesson file ownership:

- Each lesson entry records its skills, prompts, configs, and an `appliedAt` timestamp.
- The `files` field is a **union** of all lesson entries, rebuilt on each apply via `buildUnionFiles()`. Content hashes in `files` reflect what's on disk (current bundle's hashes win, others preserved from previous manifest).
- `computeRemovals()` is **scoped to the current lesson**: it only removes files that (a) were in this lesson's previous entry, (b) are absent from the new bundle, and (c) are not claimed by any other lesson (the "protected set").
- `lessonId` is the last-applied lesson (for display/backward compat). `Object.keys(manifest.lessons)` gives all applied lesson IDs.
- Upgrading from v2 or v3-without-`lessons` seeds the `lessons` record from the previous manifest's `lessonId` + `files` data so existing artifacts aren't orphaned.

## `10x sync` & change detection

`commands/sync.ts` is the bulk download + update command. It enumerates unlocked lessons in one `fetchCatalog` call, applies each via `applyBundle`, and emits one aggregate report — it **never `process.exit`s mid-loop** (per-lesson failures accumulate; exit code is worst-outcome, `1` only if a lesson errored) and **never prompts** (default conflict resolver skips, `--force` overwrites).

- **Cheap-skip is digest-vs-digest.** The catalog advertises a per-lesson `contentHash` (`LessonSummary.contentHash`, optional). On apply, sync stores that exact value into `manifest.lessons[id].catalogContentHash` (via `ApplyOptions.catalogContentHash`). Next sync compares the *new catalog digest* against the *stored* one and skips the fetch entirely when equal. Never compare the catalog digest against the writer's per-file hashes — they live in a different hash space. Absent digest (older backend) or absent stored value → always-fetch fallback. `--force` bypasses the gate so it can overwrite local edits even when upstream is unchanged.
- **`planBundle()` is the pure planner.** It classifies per-file `{ action, isConflict, upstreamChanged }` without writing or prompting; `applyBundle` consumes it so classification and application can't diverge. `sync --dry-run` reports off `planBundle`; the real path reports off `applyBundle`'s `WriteResult`. The parity is locked by `tests/writer-plan.test.ts`.
- **Change visibility is skills + prompts only.** Configs are create-only (never overwritten, so nothing to report) and rules are sentinel-managed, not manifest-hash-tracked.
- A plain `10x get` neither refreshes nor erases a stored `catalogContentHash` (it's carried forward in `applyBundle`), so at worst one redundant fetch never happens.

## Conventions worth knowing

- TypeScript is `strict` + `noUncheckedIndexedAccess` + `noImplicitOverride`. Index access on arrays/records returns `T | undefined` — handle it.
- Generated code lives under `src/generated/` and is excluded from oxlint via `.oxlintrc.json`.
- The CAC parser throws on unknown options; `src/index.ts` catches that and exits `2` (USAGE) with an `ERROR usage:` prefix on stderr. Preserve this behavior — it's how scripts detect bad invocations.
- The CLI's user-agent is hard-coded to `"10x-cli"` in `api-client.ts`.

<!-- BEGIN @przeprogramowani/10x-cli -->

## 10xDevs AI Toolkit - Module 3, Lesson 2

Lesson 2 is about **writing tests that actually protect code** — not just maximise coverage. The oracle problem and vibe-testing anti-patterns explain why LLM-generated tests fail on real code; the risk-first quality contract from Lesson 1 is the fix.

```
context/foundation/test-plan.md (§3 Phased Rollout)
        │
        ▼  (one rollout phase at a time)
   /10x-research  ──►  research.md  (oracle source: what code should do, not what it does)
        │
        ▼
   /10x-plan  ──►  plan.md  (cost × signal, two-layer strategy, ordered phases)
        │
        ▼
   /10x-implement  or  /10x-tdd   ──►  working tests + §6 cookbook update
```

`/10x-tdd` is an **optional test-first mode**, not a replacement for the chain. It reads the same `plan.md`, writes to the same `## Progress` section, and covers the same phases as `/10x-implement`. Use it only when you can name the first failing assertion before writing any code.

### Task Router — Where to start

| Skill / Prompt | Use it when |
| --- | --- |
| `/10x-research` | Before writing any test for a risk. Research produces the oracle — what behaviour a test must prove — from sources (PRD, tech-stack, docs), not from the implementation shape. Also reveals whether a risk is already covered or has two separate faces (one safe, one real). |
| `/10x-plan` | Research is done. Plan decomposes the risk into ordered phases: environment setup first, then rules that depend on it, then hermetic stubs for failures that real infra cannot trigger, then cookbook update. Each phase names the behaviour it asserts and the regression it catches. |
| `/10x-implement` | Default executor for plan phases. Use for environment setup, existing code, scaffolding, and any phase where you cannot define a red test before writing code. |
| `/10x-tdd` | Optional. Use instead of `/10x-implement` for a phase where you can name the first red test in one sentence. Agent writes the failing test first, then the minimal code to green it, then refactors. Stops at the assertion before touching the implementation — that pause is the point. |
| `m3l2-ad-hoc-testing` prompt | You have a single file and want tests now, without the full research→plan→implement cycle. The prompt forces oracle-from-sources (reads PRD + TECH_STACK before asserting), behavioural assertions, edge cases from risk, and a regression table. Use it knowing you are trading depth for speed. |

### When to use `/10x-tdd` vs `/10x-implement`

The deciding question: *Can you name the first red test in one sentence?*

Good conditions for `/10x-tdd`:
- "promuje wyłącznie drafty w stanie `accepted`, a `pending`/`rejected` nigdy nie trafiają do talii"
- "zwraca `ok: true` i loguje `orphan_review_state`, gdy upsert stanu powtórek padnie w trakcie zapisu"
- "zwraca 401, gdy użytkownik nie ma dostępu do kursu"
- "resetuje interwał powtórki do jednego dnia, gdy ocena wynosi 0"

Each of these names an observable outcome, not an internal detail. If you cannot produce a sentence like this, stay on `/10x-implement` or return to `/10x-research`.

`/10x-tdd` is **not suited** for: environment setup, CI/CD config, documentation, thin wiring where the test would just rewrite the implementation, or a spike where you are still discovering the contract.

You can mix both modes in one plan:

```
/10x-implement <change-id> phase 1   # environment
/10x-tdd       <change-id> phase 2   # contract (new code)
/10x-tdd       <change-id> phase 3   # contract (API endpoint)
/10x-implement <change-id> phase 4   # cookbook + plan sync
```

Both write progress to the same `## Progress` section in `plan.md`.

### Two-layer test strategy (cost × signal)

For each risk, pick the **cheapest test that gives a real signal**. Do not default to e2e "because it's safest", and do not chase coverage percentage.

| Layer | When to use | When NOT to use |
| --- | --- | --- |
| Integration (real DB / real infra) | The rule involves DB constraints, cascades, real SQL, or unique constraints that a mock would lie about. | Auth flows gated by RLS that belong to a separate phase; anything where setup cost exceeds signal value. |
| Hermetic (stub client) | Partial failures that real infra cannot trigger easily (e.g. second operation in a sequence fails). | Rules that depend on actual DB state — a stub will lie about constraint violations and cascades. |

A non-atomic save sequence (multiple independent operations without a transaction) means: write hermetic tests for partial-failure branches, not integration tests that force a mid-sequence error.

### Oracle rules

- The oracle — what the code *should* do — must come from sources: PRD, docs, tech-stack constraints, domain knowledge. It must **not** come from reading the implementation.
- If the implementation has a bug, copying its output as the expected value produces a mirror test that passes against the bug.
- When sources do not resolve the expected behaviour unambiguously, **stop and ask** rather than guessing.
- Research's job is to surface the oracle before any test is written.

### Vibe-testing anti-patterns to avoid

| Anti-pattern | How it looks | What to do instead |
| --- | --- | --- |
| Mirror implementation | Assertion computes the expected value with the same logic as the tested code. | Assert against a value derived from the oracle (PRD / domain rule), not from the implementation. |
| Happy paths only | Tests only pass valid inputs; edge cases absent. | Add at least one edge case per risk: `null`, empty, dependency error, invalid input. |
| Redundant copies | Six nearly identical tests checking the same absence of a sentinel. | One parameterised test (`it.each`) per property; each test catches a different regression. |

### Mutation testing (Stryker) — selective quality gate

Coverage says "this line was executed". Mutation score says "would a test fail if I broke this line?" Use Stryker as a **selective gate** after a risk phase, not as a CI gate on every commit.

Workflow:
1. Tests pass for the risk phase.
2. Run `npx stryker run --mutate "path/to/file.ts"` (narrow scope to the changed module).
3. Open the HTML report; find survived mutants.
4. For each survived mutant ask: "Would this change hurt a user or the business?"
   - Yes → add an assertion that kills the mutant.
   - No (equivalent mutant or cosmetic change) → ignore consciously.
5. Do not chase 100% mutation score. A test that pins implementation details to kill a cosmetic mutant is itself a vibe test.

The integration gate can stay **ad hoc** (not on every commit) when running local infra is expensive. Mark it accordingly in `test-plan.md §4`.

### Lesson boundaries

- Do not configure hooks, hook lifecycle, or debugging hooks. That is Lesson 3.
- Do not configure MCP servers, Playwright API, e2e code, or multimodal scenario code. That is Lesson 4.
- Do not run the bug-to-fix-to-regression-test workflow. That is Lesson 5.
- Do not author CI/CD pipelines from scratch. That is Module 1 Lesson 5 / Module 2 Lesson 5.
- Do not run `/10x-test-plan` to change the risk strategy. That is Lesson 1. Use `/10x-test-plan --status` to read current state.
- Do not write tests without a research step unless using the ad-hoc prompt with full awareness of its trade-offs.

### Paths used by this lesson

- `context/foundation/test-plan.md` — §3 rollout state; §6 cookbook (filled in as phases ship)
- `context/changes/<change-id>/research.md` — oracle source per rollout phase
- `context/changes/<change-id>/plan.md` — ordered phases with `## Progress` as execution state
- `.claude/prompts/m3l2-ad-hoc-testing.md` — ad-hoc file-level testing prompt

<!-- END @przeprogramowani/10x-cli -->
