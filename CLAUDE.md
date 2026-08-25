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

## 10xDevs AI Toolkit — Module 1, Lesson 4

Onboard the agent to the project you scaffolded in Lesson 3 with the **agent-context chain**:

```
(/10x-init  →  /10x-shape  →  /10x-prd  →  /10x-tech-stack-selector  →  /10x-bootstrapper)  →  /10x-agents-md  →  /10x-rule-review  →  /10x-lesson
```

The PRD → tech-stack → bootstrap chain ships from Lessons 1–3 (re-included so you can fix the project mid-flight). `/10x-agents-md`, `/10x-rule-review`, and `/10x-lesson` are the lesson's main topics. The chain extends in Lesson 5 to the infra/deploy step.

### Task Router — Where to start

| Skill | Use it when |
| --- | --- |
| **Agent context (lesson focus)** | |
| `/10x-agents-md` | The repo is scaffolded but the agent has no project-specific onboarding. Inspects the repo (package manifest, README, scripts, lint/test config, layout, commit history) and writes a concise, ordered "Repository Guidelines" to `AGENTS.md` (or, when invoked from a subdirectory, a directory-level `AGENTS.md` reframed around local conventions and the dominant unit). Use as an alternative to the host's built-in `/init` or as a fallback for tools without one. Repo-level body targets ~200 lines; directory-level guides target 120–250 words. |
| `/10x-rule-review <path>` | You have a rules-for-AI file (`AGENTS.md`, `CLAUDE.md`, `.cursor/rules/*.mdc`, `.github/copilot-instructions.md`, `.windsurfrules`, nested per-area files) and want a 5-axis scorecard: length, embedded code/config snippets, precision of language, redundancy with public knowledge, and rule ordering. Tool-agnostic — scores the artifact's condition, not the project. Default output is read-only; only Check 5 (reorder) may edit, and only with explicit approval. |
| `/10x-lesson [seed]` | You spotted a recurring rule worth surfacing for future runs of `/10x-frame`, `/10x-research`, `/10x-plan`, `/10x-plan-review`, `/10x-implement`, and `/10x-impl-review`. Appends a single entry (Context / Problem / Rule / Applies to) to `context/foundation/lessons.md`. Self-bootstraps the file with the canonical `# Lessons Learned` header on first use. Append-only — never reorders or rewrites prior entries. |
| **Re-run upstream if needed** | |
| `/10x-init` / `/10x-shape` / `/10x-prd` / `/10x-tech-stack-selector` / `/10x-bootstrapper` / `/10x-stack-assess` / `/10x-health-check` | Bundled so you can fix the PRD, swap the stack, or re-scaffold mid-flight. If `/10x-rule-review` flags a `FAIL` you can't shrink your way out of, that often points back to ambiguous PRD or stack decisions — re-run the upstream skill rather than padding `AGENTS.md` with corrections. |

### How the chain hands off

- `/10x-agents-md` writes (or surgically updates) `AGENTS.md` at the resolved scope. Repo-level scope = the file lives at the repo root and frames the project as a whole; directory-level scope = the file lives next to the code it governs and reframes around the local unit, dropping repo-wide framing entirely. The skill never silently overwrites — it switches to an update flow when the target exists.
- `/10x-rule-review` reads any rules-for-AI markdown file you point it at and prints a 5-check scorecard (`OK` / `WARN` / `FAIL`) with concrete fixes. It does not depend on `/10x-agents-md` having run; you can review `.cursor/rules/`, copilot instructions, or a hand-written `CLAUDE.md` the same way.
- `/10x-lesson` self-bootstraps `context/foundation/lessons.md` on first use, then appends one Context/Problem/Rule/Applies-to entry per invocation. The file is consumed as a prior by the planning- and review-phase skills introduced later in the workflow — `/10x-frame`, `/10x-research`, `/10x-plan`, `/10x-plan-review`, `/10x-implement`, `/10x-impl-review`.

### What the lesson's skills capture (and what they do NOT)

- **`/10x-agents-md` captures**: project structure, build/test/lint commands actually present in scripts, commit conventions inferred from history, repo-specific tripwires the agent would otherwise miss, references to canonical files via `@`-paths instead of pasting their content. Directory-level scope additionally captures: local naming/layout patterns inferred from siblings, allowed/forbidden imports, the test pattern used by neighbours, and tripwires visible in the immediate area.
- **`/10x-agents-md` does NOT** paste in the contents of `tsconfig.json` / `eslint.config` / framework docs the agent already knows; it does NOT generate generic "write clean code" intentions; it does NOT replace the host's built-in `/init` when one exists — it's positioned as an alternative or fallback, not a default.
- **`/10x-rule-review` captures**: a length verdict (OK ≤ 200 non-empty lines, WARN 201–500, FAIL 501+), code/config blocks that should be `@`-references instead, vague-intention language, redundancy with framework docs the agent already has from training, and a Check 5 reorder proposal that surfaces critical rules to the top.
- **`/10x-rule-review` does NOT** edit the file by default; it does NOT score project content (architecture, stack choices) — it scores the rule artifact's condition; it does NOT generate a "fixed version" of the file (Check 5 may move sections with explicit approval, never rewrite rule wording).
- **`/10x-lesson` captures**: one entry per invocation with a short imperative H2 title (the title IS the rule), Context (subsystem / phase / file pattern, specific enough to pattern-match), Problem (what concretely breaks without the rule, ideally with a past incident), Rule (1–2 imperative sentences pasteable verbatim into a future review finding), Applies to (subset of `frame`, `research`, `plan`, `plan-review`, `implement`, `impl-review`, or `all`).
- **`/10x-lesson` does NOT** edit or remove existing lessons — the file is append-only by design (rewriting recurring rules without thought is the failure mode this convention prevents); it does NOT batch multiple rules per invocation; it does NOT pre-fill fields proactively (the user does the writing — that's the price of capturing rules outside a structured review).

### The inclusion test (the filter for AGENTS.md / CLAUDE.md)

Before you add a rule to any rules-for-AI file, ask: *could the agent know this without this file? Could public training data — books, blogs, repos in this stack — have prepared it for this?* If yes, drop it. If no, keep it. The file is onboarding for an agent that already knows TypeScript / Python / your framework but does NOT know your local conventions.

Belongs:
- non-obvious project conventions (error-response shape, file naming, allowed import paths)
- project-specific traps and "embarrassing" workarounds tied to history or dependency bugs
- referenced canonical files via `@`-paths (e.g. `@src/features/users/user.service.ts` as a pattern reference, not pasted code)

Does NOT belong:
- mainstream framework documentation
- README content the agent will read anyway (link with `@README.md`)
- popular generic advice ("use TypeScript strict mode") that's already enforced by config
- intention statements ("write clean code", "follow good practices") — convert to a checkable behaviour or drop

### U-shaped attention and granular rules

LLMs attend most strongly to the start and end of context (Lost-in-the-Middle / U-shaped attention). A long monolithic `CLAUDE.md` puts its middle rules in the weakest attention zone. Two practical consequences:

1. **Most important rules go to the top** of any rule file.
2. **Per-area rules belong next to their code** — nested `AGENTS.md` / `CLAUDE.md` inside `src/api/`, `.cursor/rules/*.mdc` with file globs, etc. Granular files are loaded selectively and arrive whole near the start of their own section, instead of being buried at line 400 of one big file.

`/10x-rule-review` Check 5 (reorder) operationalizes consequence (1); the inclusion test plus directory-level `/10x-agents-md` operationalizes consequence (2).

### The five-pattern calibration drill

Before writing a rule, validate that the agent actually breaks the convention without it. Pick one pattern from your project (error-response shape, file naming, import style, module structure, date handling). Then:

1. Ask the agent to implement against the pattern 3–5 times from a clean state, no rule.
2. Note where it broke the convention; capture run time, files explored, and visible cost/tokens if the host surfaces them.
3. Add a 1–3-sentence rule to the appropriate scope (root or area-level).
4. Re-run the same task in a fresh session and compare convention adherence, time, files, and iterations.

If the agent already trends toward the convention without the rule, you don't need the rule. If it systematically picks the wrong pattern, you've found a high-leverage rule to add. This drill is what "earning a rule from a recurring failure" actually looks like.

### Hierarchy and tool interop

- **Claude Code** loads `CLAUDE.md` from the user dir (`~/.claude/CLAUDE.md`), the repo root, and any subdirectory the agent works under. Deeper files override or supplement higher ones.
- **Codex** and **GitHub Copilot** load `AGENTS.md` from the current directory upward — closest file wins.
- One canonical file is preferable to three duplicates. A common pattern: `AGENTS.md` as source of truth, `CLAUDE.md` as a thin Claude-Code shim with `@AGENTS.md` import, `.github/copilot-instructions.md` only if Copilot needs its own additions. Symlink (`ln -s AGENTS.md CLAUDE.md`) is the simplest deduplication when tools require both names.
- Auto-memory (e.g. Claude Code's `~/.claude/projects/<dir-with-slashes-as-dashes>/memory/MEMORY.md`) is local to the machine and not a substitute for `AGENTS.md`. Team-binding rules live in the repo; auto-memory is a personal cache, periodically reviewable.

### Inner-loop hooks (deterministic feedback without prompting)

Mechanical, non-pickable checks belong in hooks (e.g. Claude Code's `PostToolUse`), not in the rule file. The agent finishes an edit; a formatter or fast lint runs; the result feeds back without you reminding it. Settings template (`settings.json.template`) ships in the lesson pack as the wiring entry point. Keep procedural workflows (deeper review, release checklist, deploy on sandbox) in skills, and reserve hooks for deterministic tool signals.

### Foundation paths used by this lesson

- `AGENTS.md` / `CLAUDE.md` (and per-area variants) — `/10x-agents-md` output
- `context/foundation/lessons.md` — `/10x-lesson` output (append-only register, consumed by future planning/review skills)
- `context/foundation/prd.md`, `context/foundation/tech-stack.md` — inputs from earlier lessons, still present
- `docs/reference/contract-surfaces.md` — load-bearing names registry (scaffolded by `/10x-init`)

### Universal language

The shipped skills carry no 10xDevs / cohort / certification references. `/10x-agents-md` discovers from the repo it's invoked in; `/10x-rule-review` is tool-agnostic and treats every file as "a rules-for-AI artifact"; `/10x-lesson` writes one entry shape regardless of project domain. The 5-pattern calibration drill is illustrative — substitute patterns from your own stack.

Skills must not write to `context/archive/`. Archived changes are immutable; if a resolved target path starts with `context/archive/`, abort with: "This change is archived. Open a new change with `/10x-new` instead."

<!-- END @przeprogramowani/10x-cli -->
