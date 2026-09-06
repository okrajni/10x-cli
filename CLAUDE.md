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

## 10xDevs AI Toolkit - Module 3, Lesson 1

Open Module 3 by producing a **durable, risk-first quality contract** before any test is written — then drive each rollout phase through the standard change chain.

```
PRD + roadmap + archive
        │
        ▼
   /10x-test-plan  ──►  context/foundation/test-plan.md  (strategy §1–§5 frozen + cookbook §6 grows)
        │
        ▼  (one rollout phase at a time, /clear between handoffs)
   /10x-new ──► /10x-research ──► /10x-plan ──► /10x-implement
```

`/10x-test-plan` is a **stateful orchestrator**, not a one-shot generator. On first run it writes the phased rollout to `context/foundation/test-plan.md`. On every subsequent run it re-derives state from on-disk artifacts and presents the next handoff. The lesson focus is **strategy and rollout sequencing, not configuration**. Hooks, MCP servers, and CI YAML are configured in later lessons of this module.

### Task Router - Where to start

| Skill | Use it when |
| --- | --- |
| **Quality strategy as a rules-file (lesson focus)** | |
| `/10x-test-plan` | You have a PRD (and ideally a roadmap and a few archived slices) and you are about to write the project's first tests, or you noticed that AI-generated tests are landing on helpers while critical flows go uncovered. First invocation runs discovery (PRD + roadmap + archive + hot-spot scan), a 5-question user interview, and a synthesis pass with a mandatory challenger check, then writes `test-plan.md` in `context/foundation/` with a risk map (5–7 failure scenarios), a phased rollout table, a stack table, a quality-gates table, a cookbook section (`§6`, fills in as phases ship), and a negative-space section (what we deliberately don't test). Subsequent invocations advance the rollout one handoff at a time. |
| `/10x-test-plan --status` | A `test-plan.md` already exists and you want a compact snapshot of where the rollout stands — which phases are `not started`, `change opened`, `researched`, `planned`, `implementing`, or `complete`, and what the next action is. Does no work; safe to run any time. |
| `/10x-test-plan --refresh` | A `test-plan.md` already exists and one of: a new top-3 risk surfaced from the roadmap or archive, a tool's `checked:` date is older than three months, the project's tech stack changed, or §7 negative-space no longer matches what the team believes. Opens a new `test-plan-refresh-<YYYY-MM-DD>` change folder rather than editing the guide in place. |

### Rollout chain — what happens after the guide is written

The guide's §3 *Phased Rollout* table is the orchestrator's state. For each non-`complete` row the orchestrator selects the next handoff based on which artifacts exist in `context/changes/<change-id>/`:

| State on disk | Next handoff | Status transitions to |
| --- | --- | --- |
| change folder missing | `/10x-new <change-id>` | `change opened` |
| `change.md` only | `/10x-research` (with a risks-to-verify brief) | `researched` |
| `+ research.md` | `/10x-plan` (with cost × signal + cookbook-update constraints) | `planned` |
| `+ plan.md` with pending `## Progress` items | `/10x-implement <change-id> phase <N>` | `implementing` / `complete` |
| `+ plan.md` fully `[x]` | Mark §3 row `complete`; loop to next pending row | — |

Each handoff is a **STOP point**. The orchestrator copies the next command to the clipboard, asks the user to `/clear` and run it, then exits. Re-invoke `/10x-test-plan` (no arguments) to advance.

### Risk-first prioritization rules

- Risks are **failure scenarios in user / business terms**, not test names. "Logged-out user reaches paid content via stale token" is a risk; "test the login form" is not.
- 5 to 7 risks. Fewer is too coarse; more makes prioritization useless.
- Impact and likelihood are user/business ratings, not technical complexity.
- Every risk traces to a source: PRD section, archived slice, roadmap entry, Phase 2 interview question, hot-spot **directory** with churn count, or a tech-stack constraint. No invented risks.
- **Signal, not knowledge.** §2 cites *evidence that raised the risk*, never a file as "where the failure lives." File:line anchors, function names, schema names, and module names are forbidden in §2 — they belong in `/10x-research`'s output, produced per rollout phase against current code. The plan is a QA spec; it is not a code audit.
- Coverage is not the metric. **Risk coverage** is the metric.

### Dual-layer mapping rules

- Classic layer first: the cheapest test that gives a real signal wins. Promote to e2e only when no cheaper layer covers the risk.
- AI-native layer second, and only where it adds signal classic tests do not give cheaply.
- Every AI-native row has a **"When NOT to use"** line. If you cannot write one, drop the row.
- Every tool name carries a `checked: <YYYY-MM-DD>` date. Tool names are examples of the category, not endorsements.
- Both layers must be non-empty in the final guide if the project warrants them. Classic-only is a 2020 plan; AI-native-only is hype. AI-native phases are not mandatory — include them only when the brief justified them under cost × signal.

### Quality gates rules

- Required gates (lint, typecheck, unit+integration, e2e on critical flows) must map to actual CI steps. If a required gate is not yet wired, mark it as `required after §3 Phase <N>` and let the named rollout phase wire it.
- Post-edit hook is **recommended local**, not a CI substitute.
- Multimodal visual review is **selective**, applied to 1–3 critical screens, not to every page.
- Vision-driven fallback (Anthropic Computer Use or OpenAI CUA) is reserved for DOM-unreachable surfaces; expensive per action.

### Cookbook patterns (§6) — fills in over time

`test-plan.md` is both a phased strategy and a **growing cookbook**. §6 starts as placeholders (`TBD — see §3 Phase <N>`) and fills in incrementally — each rollout phase's plan ends with a sub-phase that updates the relevant §6 entry (location, naming, reference test, run command). After Module 3 completes, §6 becomes the canonical answer to "how do I add a test for X in this project?" — and is what `/10x-tdd` reads in Lesson 2.

### Lesson boundaries

- Do not write test code. That is Lesson 2 (`/10x-tdd` and unit-test authoring).
- Do not configure hooks, hook lifecycle, or debugging hooks. That is Lesson 3.
- Do not configure MCP servers, Playwright API, e2e code, or multimodal scenario code. That is Lesson 4.
- Do not run the bug-to-fix-to-regression-test workflow. That is Lesson 5.
- Do not author CI/CD pipelines from scratch or write GitHub Actions YAML. The guide names gates; configuration is owned by Module 1 Lesson 5 and Module 2 Lesson 5.
- Do not benchmark multimodal models. Cite criteria (cost, latency, agent-friendliness), never a ranking.
- Do not read the codebase for knowledge (call graphs, schemas, "which file owns this failure"). That is `/10x-research`'s job, per rollout phase.

### Paths used by this lesson

- `context/foundation/test-plan.md` — the quality contract produced and maintained by `/10x-test-plan`
- `context/foundation/prd.md` — primary risk source
- `context/foundation/roadmap.md` — likelihood weighting
- `context/foundation/tech-stack.md` — stack input (when present)
- `context/archive/<change-id>/plan.md` — implemented risk surface
- `context/changes/<change-id>/` — per-rollout-phase change folder (one per row in §3)

<!-- END @przeprogramowani/10x-cli -->
