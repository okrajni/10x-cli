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

## 10xDevs AI Toolkit — Module 1, Lesson 5

Pick a deployment platform and ship to production with the **infra chain**:

```
(/10x-init  →  /10x-shape  →  /10x-prd  →  /10x-tech-stack-selector  →  /10x-bootstrapper  →  /10x-agents-md  →  /10x-rule-review  →  /10x-lesson)  →  /10x-infra-research  →  Plan Mode deploy
```

The full Module 1 chain ships from Lessons 1–4 (re-included so you can fix any earlier contract mid-flight). `/10x-infra-research` is the lesson's main topic; the deploy step itself uses the host's built-in **Plan Mode** rather than a dedicated skill — the artifact (`context/deployment/deploy-plan.md`) is what carries forward.

### Task Router — Where to start

| Skill | Use it when |
| --- | --- |
| **Infrastructure (lesson focus)** | |
| `/10x-infra-research [path-to-tech-stack-or-prd]` | You have a `context/foundation/tech-stack.md` (and ideally a `prd.md`) and need to pick an MVP deployment platform. The skill loads the stack as a hard constraint, runs a 5-question developer interview (persistent connections, cost sensitivity, existing familiarity, global reach, co-location preference), spawns parallel subagent research across six candidate platforms, scores them Pass/Partial/Fail across the five agent-friendly criteria from `references/agent-friendly-criteria.md`, shortlists the top three, and runs a three-lens anti-bias cross-check on the leader (devil's advocate, pre-mortem, unknown unknowns) before writing `context/foundation/infrastructure.md`. Use AFTER `/10x-tech-stack-selector`, BEFORE `/10x-implement`. |
| **Deploy (host built-in, not a skill)** | |
| Plan Mode deploy | You have `infrastructure.md` + `tech-stack.md` and want a read-only plan reviewed before any mutation hits the platform. Activate the host's plan mode (Claude Code: `Shift+Tab` cycles default → auto-accept → plan; IDE: dedicated button) with the prompt "Wykonajmy pierwsze wdrożenie w oparciu o `@infrastructure.md`, zgodnie ze stackiem z `@tech-stack.md`". Read the plan, demand corrections, approve, then let the agent execute. The approved plan persists at `context/deployment/deploy-plan.md` so the next lesson's milestone planning can reference what's already deployed and which secrets are already wired. |
| **Re-run upstream if needed** | |
| `/10x-init` / `/10x-shape` / `/10x-prd` / `/10x-tech-stack-selector` / `/10x-bootstrapper` / `/10x-agents-md` / `/10x-rule-review` / `/10x-lesson` / `/10x-stack-assess` / `/10x-health-check` | Bundled so you can patch any earlier contract mid-flight. If the anti-bias cross-check forces a platform swap that pushes a stack-shaped decision (e.g. "this DB doesn't fit any platform we'd accept"), re-run `/10x-tech-stack-selector` to keep `tech-stack.md` and `infrastructure.md` aligned. |

### How the chain hands off

- `/10x-infra-research` reads `context/foundation/tech-stack.md` (language, framework, runtime, database) as **hard constraints** — platforms that can't run the stack are dropped before scoring. It also reads `context/foundation/prd.md` (scale, latency, uptime expectations) as **soft weights** when scoring. Both inputs are optional but strongly recommended; without them the skill proceeds but warns.
- The skill writes `context/foundation/infrastructure.md` as the third foundation contract: frontmatter (`project`, `researched_at`, `recommended_platform`, `runner_up`, `context_type`, `tech_stack`) plus a body covering recommendation, full platform comparison with scoring matrix, anti-bias findings, operational story (preview / secrets / rollback / approval / logs), and a risk register tying every entry back to the lens that surfaced it. On collision the skill prompts: overwrite, save as `infrastructure-v2.md`, or abort.
- Plan Mode reads `infrastructure.md` and `tech-stack.md` together. The agent emits a step-by-step plan covering automated steps it owns, manual setup gates (account creation, secret configuration), exact deploy commands (Pages vs Workers commands are NOT interchangeable on Cloudflare — the plan must specify), and verification steps. The plan is rejected/edited until it's right; only then does Plan Mode exit and execution begin. The approved plan lands at `context/deployment/deploy-plan.md` and is consumed downstream by milestone-planning skills as ground truth for "what's already deployed".

### What the lesson's skills capture (and what they do NOT)

- **`/10x-infra-research` captures**: platform shortlist scored against five agent-friendly criteria (CLI quality, managed/serverless degree, agent-readable docs, stable/scriptable deploy API, MCP or first-class agent integration), three anti-bias outputs on the leader (numbered weaknesses, 150–200-word failure narrative, 3–5 unknown-unknowns), an operational story with one concrete answer per axis (not categories), and a risk register where every row names its source lens (`Devil's advocate` / `Pre-mortem` / `Unknown unknowns` / `Research finding`). Status of every non-GA feature is captured inline (`beta` / `preview` / `region-limited` / `deprecated`) with the date the status was checked.
- **`/10x-infra-research` does NOT** build Docker images or write Dockerfiles, configure CI/CD pipelines, or plan beyond MVP scope (multi-region HA is explicitly out of scope). It does NOT decide for you — the user accepts, swaps to runner-up, or aborts after the cross-check, and that decision is recorded in the output.
- **Plan Mode** captures: an explicit human gate between "agent has a plan" and "agent mutates production". The artifact (`deploy-plan.md`) is the audit trail for "what was supposed to happen" when the live run goes sideways. Plan Mode does NOT replace `/10x-infra-research` (the platform decision must already be made — Plan Mode plans the deploy, it doesn't pick where to deploy).

### The five agent-friendly criteria (and why they're load-bearing)

The criteria that make `/10x-infra-research`'s scoring matrix are not generic "good platform" axes — they're the specific traits that determine whether an agent can operate this platform from a session without you holding its hand:

1. **CLI-first** — every routine operation has a documented command; the agent doesn't need to click in a panel.
2. **Managed / serverless** — fewer moving pieces means fewer ways the agent (or you) breaks something the platform was supposed to handle.
3. **Agent-readable docs** — markdown / `llms.txt` / GitHub-hosted docs the agent can fetch and parse, not JS-rendered marketing pages.
4. **Stable, scriptable deploy API** — predictable exit codes, structured output, no interactive prompts mid-deploy.
5. **MCP server or first-class agent integration** — bonus, not required. CLI alone is fine for MVP; MCP earns its keep when the agent makes dozens of structured queries against live state.

Hard filters apply before scoring (persistent-connection requirement drops Netlify/Vercel serverless-only; tech-stack runtime mismatch drops the platform entirely). Interview answers reweight criteria after — cost sensitivity penalizes expensive base tiers, familiarity breaks ties, global-reach preference favours edge-native platforms, co-location preference favours integrated databases.

### Anti-bias as a decision discipline (not theatre)

Every research conversation with an LLM has a built-in tilt toward whatever the user already signalled. `/10x-infra-research` runs three structured lenses against the leader BEFORE the file is written, not after:

- **Devil's advocate** — *find the weaknesses, hidden costs, and failure modes specific to deploying `<this stack>` on `<this platform>`*. Output is a numbered list of 3–5 specifics, not categories.
- **Pre-mortem** — *six months later, this decision turned out to be a complete disaster; walk through the assumptions and underestimated risks that led there*. Output is a 150–200-word narrative; narratives surface concrete failure shapes that abstract risk lists hide.
- **Unknown unknowns** — *what's true about this combination that the marketing page and docs don't make obvious?* Output is 3–5 non-obvious risks.

After the cross-check the user has three real options: **proceed with the leader and absorb the risks into the register**, **swap to runner-up** (and re-run the cross-check on the new leader), or **swap to third place**. The third option is rare; if it never happens across many runs, the cross-check has degraded into a ritual and should be rewritten.

Two additional techniques (no skill required, raw prompts) belong in the same toolbox: forcing the model to compare three alternatives in a markdown table (structure beats "the same answer in different words"), and role-rotation (the same decision through a frontend dev's, security person's, and cost owner's eyes — surface the cost each role pays and propose alternatives if any of them flinch).

### CLI vs MCP for live-infra operability

After deploy, the agent needs a way to talk to the running platform. Two paths, complementary not competing:

- **CLI** (`wrangler`, `flyctl`, `vercel`, `gh`) — explicit and auditable, output stays in the terminal, safer defaults for irreversible actions (e.g. `netlify deploy` is draft by default; `--prod` must be passed). Best for MVP: minimal setup, low context cost (no tool schemas pre-loaded), and the agent has to know the command (which is where a per-tool skill helps).
- **MCP** — a dedicated server exposing structured tools with schemas (`pages_deployments_list`, etc.). Each connected MCP server adds tool definitions to the context window, so cost compounds across servers. Earns its keep when the agent makes many discovery-style queries against live state (logs, deployment diffs) and structured JSON beats parsing CLI output.

Sensible default: start with CLI, add MCP when you notice a recurring pattern of `--help` traversal the agent has to do to answer a class of questions. Anthropic's own [building-agents-that-reach-production](https://claude.com/blog/building-agents-that-reach-production-systems-with-mcp) framing is "API, CLI, and MCP are three complementary paths" — pick by task, not by hype.

### Production-access boundary (minimal permissions, human-on-irreversibles)

Both CLI and MCP can give the agent direct access to production. The lesson sets a default posture:

- **Tokens are scoped, not master keys.** On Cloudflare: an API token limited to Pages or Workers for one project, no DNS, no Workers Secrets for unrelated projects, no billing. AWS / GCP equivalent: scoped IAM role with `console-only-user` or read-only on production, full access on staging.
- **Tokens live in env vars, not in `.mcp.json` committed to the repo.** The agent picks them up via the MCP server or CLI's env-discovery, not via plaintext in conversation.
- **Destructive actions are human-only.** Drop a database, rotate a primary secret, delete a project — those are panel-by-hand operations, even if the agent suggests them. Manual click costs 30 seconds; cleanup after an automated mistake costs hours.

This is the MVP posture. As the project matures, the natural evolution is staging gets full agent access, production becomes read-only — covered in later modules.

### Foundation paths used by this lesson

- `context/foundation/tech-stack.md` — input (Lesson 2 hand-off, hard constraints)
- `context/foundation/prd.md` — input (Lesson 1 hand-off, soft weights)
- `context/foundation/infrastructure.md` — output (the third foundation contract)
- `context/deployment/deploy-plan.md` — output of Plan Mode deploy (audit trail of "what was supposed to happen")
- `context/foundation/lessons.md` — recurring rules & pitfalls (use `/10x-lesson` from Lesson 4 if you spot a class of agent failure during research or deploy)
- `docs/reference/contract-surfaces.md` — load-bearing names registry

### Universal language

The shipped skill carries no 10xDevs / cohort / certification references. The candidate platform list (Cloudflare, Vercel, Netlify, Fly.io, Railway, Render) is the starting research lens, not a recommendation set — the scoring + interview + cross-check pipeline is what's load-bearing, and a platform absent from the default list can be added by extending the research step. The five agent-friendly criteria are the artifact's true core; `/10x-infra-research` re-reads them from `references/agent-friendly-criteria.md` so they evolve as platforms do.

Skills must not write to `context/archive/`. Archived changes are immutable; if a resolved target path starts with `context/archive/`, abort with: "This change is archived. Open a new change with `/10x-new` instead."

<!-- END @przeprogramowani/10x-cli -->
