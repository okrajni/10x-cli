---
project: "done yet?"
version: 1.0
created: 2026-09-05
status: active
rollout_strategy: risk-first, cost×signal, four phased handoffs
test_base_profile: none
---

# Test Plan: done yet?

A household task management app (single-user MVP). Users externalize the mental load of remembering chores. Core features: task CRUD, recurring tasks (S-03), intelligent dashboard (S-06A/S-06B). This plan de-risks the MVP by covering high-impact failures and validating that the critical-path features (recurrence + dashboard) work as intended.

## §1 Strategy

### Three load-bearing principles

1. **Cost × signal.** Every test answers one question: *what is the cheapest test that gives real signal for this risk?* We promote from unit → integration → e2e only when cheaper layers don't catch the failure. We do not add a visual-regression test if a deterministic diff already wins. We do not layer a vision model on top.

2. **User concerns are evidence.** Risks the team has lived through carry the same weight as PRD lines or hot-spot data. The interview (Phase 2) surfaces what documents never capture: past incidents, areas you change without confidence, explicit "don't spend here" boundaries.

3. **Risks are scenarios, not code locations.** A risk is a *failure mode in user/business terms*, not a file to cover. "Logged-out user reaches paid content via stale token" is a risk; "test the auth middleware" is not. This plan cites evidence (PRD lines, interview answers, hot-spot directories with churn counts) as the SOURCE of risks, never as ANCHORS. The specific file where a failure lives is `/10x-research`'s job, not this plan's. Consequence: you will never see `src/foo/bar.ts:42` in §2 below.

---

## §2 Risk Map & Response Guidance

| # | Risk (failure scenario) | Impact | Likelihood | Source | Response Guidance |
|---|---|---|---|---|---|
| 1 | **Data loss / visibility bug** — tasks disappear from dashboard list despite being saved to database | High | Medium | Interview Q1 + Q3: "What worries you most?" (empty task list), "Which area lacks confidence?" (dashboard filter). Hot-spot: `frontend/src/features/tasks/pages/DashboardPage.tsx` (19 commits/30d). | **Prove:** Dashboard query returns all tasks due today or overdue, ordered by date, no missing tasks. Includes recurring tasks with correct next-occurrence. **Challenge:** "Filter works locally" ≠ "under concurrent edits." Assumption that task-update and dashboard-list endpoints stay in sync is brittle. **Context:** Task and recurrence schema; query implementation (JOIN or separate queries?); boundary conditions ("today" vs "overdue"). **Cheapest layer:** Integration test (database + query, not full API). Fixture-based with boundary cases (today's edge, overdue, recurring next-date). **Avoid:** Snapshot-asserting exact query results from live data; oracle must be independent ("task due 2026-09-05 is today on 2026-09-05"). |
| 2 | **Completion doesn't persist** — marked complete reverts on refresh | High | Medium-High | Interview Q1 (primary fear: "completion doesn't persist"). Interview Q2 (validation consistency gaps). | **Prove:** Mark complete → API succeeds → database persisted → refresh shows complete. Rollback to incomplete also persists. **Challenge:** "UI updated instantly" is optimistic update, not backend persistence. Frontend must not hide API failure. **Context:** Update endpoint behavior (validate state transitions?). API response contract. Database schema (complete: boolean or timestamp?). **Cheapest layer:** API integration test (POST /task/:id/complete; verify database state). Optional: frontend unit test of error handling (API 4xx; user sees error, not silent fail). **Avoid:** Only happy-path (complete works); must include: already-complete (idempotent?), overdue-task, error cases. |
| 3 | **Recurring task math wrong** — user misses weekly chores (e.g., "clean gutters every 7 days" due wrong date) | High | Medium | Interview Q1 (example resonated). Roadmap S-03 (critical path, complex unknowns: "How to store next occurrence?"). PRD success metric depends on task recurrence adoption. | **Prove:** Weekly/monthly/daily task recurs to correct next date. Dashboard shows right day. **Challenge:** "Works for simple cases" (Mon → next Mon) hides: end-of-month rolls (Jan 31 + 1 month = ?), leap years, timezone shifts, DST. Implementation copies date-fns incorrectly. **Context:** Recurrence schema (next_occurrence denormalized or calculated?). Calculation location (backend or frontend?). Timezone handling (UTC or user-local?). **Cheapest layer:** Unit test (math-only). Parameterized over 10–15 boundary cases: leap year, month-end, DST, timezone. Oracle: RRULE spec or reference table, NOT the implementation. **Avoid:** Implementation-mirror test ("assert the function matches itself"). Must test against independent oracle. |
| 4 | **Authorization bypass** — one user sees another's household tasks | High | Low-Medium | PRD NFR ("User household data isolated per user"). Auth code hot-spots: `AuthContext.tsx` (9 commits), `SecurityConfig.java` (6 commits). Single-user MVP: isolation boundary is explicit. | **Prove:** User A and User B: each sees only own household. User A cannot read/edit User B's tasks via API. **Challenge:** "Auth middleware checks JWT" ≠ "every endpoint checks resource ownership." Must verify resource belongs to user per-endpoint, not just per-user. **Context:** Auth verification logic. Household-task ownership model. Which endpoints modify tasks?. **Cheapest layer:** API integration test with two users, two households. For each endpoint: User A tries to read/write User B's task (expect 403 or 404). **Avoid:** Only happy-path (User A reads own); must test: User A reads/writes User B's task (expect 403). |
| 5 | **Validation mismatch** — invalid task saves to database (frontend allows, backend doesn't reject) | Medium | Medium | Interview Q2: "Where burned before?" (validation gaps across creation/editing/recurrence). Hot-spots: `TaskCreatePage.tsx`, `TaskEditPage.tsx`. | **Prove:** Frontend and backend validation match. Invalid data (empty title, invalid recurrence type) rejected consistently. **Challenge:** "Frontend rejects it" ≠ "backend does." Frontend can be bypassed (curl, Postman). Single-sided enforcement is incomplete. **Context:** Per-field validation rules. Recurrence constraints. **Cheapest layer:** API contract test (POST with invalid payloads; expect 400). Table-driven with valid + invalid examples. **Avoid:** Testing "form validates before sending" (client-side UX, not a protection). Must test: API rejects invalid directly. |
| 6 | **Query performance degrades** — dashboard loads >500ms, violating NFR | Medium | Low-Medium | PRD NFR (<500ms latency). Hot-spot: `DashboardPage.tsx` (19 commits). Roadmap S-06 ("must return in <500ms"). | **Prove:** Dashboard query loads <500ms on realistic dataset. Stays sub-500ms as task count grows to 1000+. **Challenge:** "Works on my laptop" with 10 tasks ≠ 1000 tasks. Simple filtering isn't fast if N+1 queries or missing index. **Context:** Realistic dataset size (assume 50–100 tasks per user in a month). Database indexes on task table. **Cheapest layer:** Benchmark test (fixture with realistic volume; measure query time). Optional: EXPLAIN query plan. **Avoid:** Measuring on empty database (meaningless). Must use realistic fixture. |
| 7 | **State inconsistency** — task is both "complete" and "overdue" simultaneously; recurring completion breaks invariants | Medium | Low-Medium | Interview Q3: "Change without confidence?" (recurring task complexity). Roadmap S-03 + S-06: new state transitions (marking recurring task complete, showing next). | **Prove:** Task cannot be complete and overdue simultaneously. Marking recurring task complete hides old, shows next. State transitions follow model invariants. **Challenge:** "Completion works" + "recurrence works" separately ≠ together. Bugs emerge at seams. **Context:** Task state model (valid states? complete + overdue coexist?). Recurring model (does marking complete create next?). **Cheapest layer:** Unit or integration test of state transition (mark complete; verify not overdue; next recurrence appears). **Avoid:** Only happy-path (incomplete → complete); must test: already-complete, overdue→complete, recurring completion. |

---

## §3 Phased Rollout

| # | Phase | Goal | Risks covered | Test types | Change folder | Status | Next action |
|---|---|---|---|---|---|---|---|
| 1 | Bootstrap + critical-path | Configure test runners (backend + frontend); prove data visibility + completion persistence work | #1, #2 | Backend integration (dashboard query, task update); frontend unit (state mgmt) | context/changes/testing-bootstrap-critical-path/ | complete | — |
| 2 | Recurrence logic + state safety | Validate recurring task math; prove state transitions don't introduce bugs. Unblock S-03 shipping. | #3, #7 | Unit tests (recurrence math); integration tests (state transitions + database) | context/changes/recurrence-logic-state-safety/ | complete | — |
| 3 | Performance + authorization | Verify dashboard query <500ms under load; authorization checks prevent cross-user access. | #4, #6 | API integration (auth boundaries, ownership checks); benchmark (query latency) | context/changes/testing-performance-authorization/ | change opened | — |
| 4 | Validation consistency | Ensure frontend and backend validation match across task creation, editing, recurrence. | #5 | API contract tests (invalid payloads); optional frontend unit tests | — | not started | (waits for Phase 3, can run parallel) |

---

## §4 Stack

### Detected test infrastructure

**Backend:**
- Maven project with JUnit 5, Spring Test, Spring Security Test configured.
- Test runner: `mvn test` (standard Maven Surefire).
- 0 test files present; bootstrap phase writes first tests.

**Frontend:**
- React 18 + TypeScript. Vite build tool. Radix UI + Tailwind CSS.
- No test runner configured (Vitest, Jest not present). Bootstrap phase chooses and configures.
- React Testing Library (or equivalent) not present; bootstrap phase adds.

**Deployment & CI:**
- GitHub Actions CI (`.github/workflows/ci.yml`) runs on every PR.
- Current gates: none (no test gate). Bootstrap phase or later phases will wire test gates.

### Stack grounding tools (current session)

- **Docs:** Context7 MCP available — will fetch Spring Boot, React, Vitest docs as needed. Checked: 2026-09-05.
- **Search:** Web search available. Checked: not needed for baseline.
- **Runtime/browser:** Playwright MCP not available in this session. Browser automation (e2e layer) deferred to later lessons.
- **Provider/platform:** GitHub MCP available. Quality-gate relevance: can wire test gate into CI Actions workflow (Lesson 3 territory).

### Non-functional constraints

- **Response latency (NFR):** <500ms for dashboard operations. Phase 3 (benchmark) validates.
- **Data isolation (NFR):** User household data isolated per user. Phase 3 (auth testing) validates.
- **Reliability (NFR):** No data loss. Phase 1 (persistence testing) validates.

---

## §5 Quality Gates

| Gate | Required? | When wired | Passes on | Notes |
|------|-----------|-----------|-----------|-------|
| Typecheck (TypeScript) | Yes | Before Phase 1 ships | Zero errors | Existing: frontend `tsc`. Backend: Maven compiler. Ensure both gates in CI. |
| Lint | Yes | Before Phase 1 ships | Zero errors | Existing: frontend (ESLint?), backend (Checkstyle?). Verify in CI. |
| Unit + integration tests | Yes | Phase 1 lands | All tests pass | New gate. Wired in Phase 1 or Lesson 3 (hook/CI config). |
| Backend: `mvn test` | Yes | Phase 1 | All tests pass | Standard Maven Surefire. |
| Frontend: unit test runner | Yes | Phase 1 | All tests pass | Phase 1 chooses (Vitest recommended for speed). |
| E2E tests (critical flows) | Optional after Phase 1 | Phase 4+ (Lesson 4) | Critical paths pass | Deferred: no Playwright/browser automation in Module 3 Lesson 1. |
| Query performance (Phase 3) | Yes | Phase 3 | Dashboard <500ms | Benchmark gate, not gated in CI (local tool). Alert if <500ms violated. |
| Authorization tests (Phase 3) | Yes | Phase 3 | No cross-user access | Integration test gate. Validates isolation NFR. |

---

## §6 Cookbook (Patterns by rollout phase)

Fills in over time as each phase ships. Each phase's final sub-phase updates the relevant entry.

### Phase 1: Bootstrap + critical-path

**Unit test: React component state**
- Location: `frontend/src/features/tasks/components/*.test.tsx`
- Reference test: (TBD — see Phase 1)
- Pattern: Test AuthContext state changes (login, logout, JWT token updates). Test dashboard state (filter toggle, task list updates).
- Run: `npm test -- <file>.test.tsx` (or Vitest equivalent)

**Integration test: Backend query + API**
- Location: `src/test/java/com/example/doneyet/api/TaskControllerIntegrationTest.java`
- Reference test: (TBD — see Phase 1)
- Pattern: Test dashboard endpoint (`GET /tasks?filter=today`). Fixture: create user, household, tasks. Assert response matches expected tasks.
- Run: `mvn test -Dtest=TaskControllerIntegrationTest`

**Integration test: Persistence (mark task complete)**
- Location: `src/test/java/com/example/doneyet/api/TaskControllerIntegrationTest.java`
- Reference test: (TBD — see Phase 1)
- Pattern: Test update endpoint (`PUT /task/:id`). Fixture: create task. POST mark complete. Query database; assert persisted.
- Run: `mvn test -Dtest=TaskControllerIntegrationTest`

### Phase 2: Recurrence logic + state safety

**Unit test: Recurrence math**
- Location: `src/test/java/com/example/doneyet/service/RecurrenceCalculatorTest.java`
- Reference test: (TBD — see Phase 2)
- Pattern: Parameterized test over 10–15 boundary cases (leap year, month-end, DST, timezone). Assert next-occurrence date matches oracle (RFC 5545 or table).
- Run: `mvn test -Dtest=RecurrenceCalculatorTest`

**Integration test: State transitions (mark recurring task complete)**
- Location: `src/test/java/com/example/doneyet/api/TaskRecurrenceIntegrationTest.java`
- Reference test: (TBD — see Phase 2)
- Pattern: Create recurring task. Mark complete. Assert: old occurrence marked done, next occurrence appears, dashboard shows only next. State invariants hold.
- Run: `mvn test -Dtest=TaskRecurrenceIntegrationTest`

### Phase 3: Performance + authorization

**Benchmark test: Dashboard query latency**
- Location: `src/test/java/com/example/doneyet/perf/DashboardQueryBenchmarkTest.java`
- Reference test: (TBD — see Phase 3)
- Pattern: Fixture: 1000+ tasks spread across users. Measure `GET /tasks?filter=today`. Assert <500ms.
- Run: `mvn test -Dtest=DashboardQueryBenchmarkTest`

**Integration test: Authorization (two-user isolation)**
- Location: `src/test/java/com/example/doneyet/security/AuthorizationIntegrationTest.java`
- Reference test: (TBD — see Phase 3)
- Pattern: Create User A + Household A + Task A. Create User B + Household B. User B tries to read Task A (expect 403). User B tries to update Task A (expect 403).
- Run: `mvn test -Dtest=AuthorizationIntegrationTest`

### Phase 4: Validation consistency

**API contract test: Invalid payloads**
- Location: `src/test/java/com/example/doneyet/api/ValidationContractTest.java`
- Reference test: (TBD — see Phase 4)
- Pattern: Table-driven: for each field (title, due_date, recurrence_type), POST invalid values. Assert 400 Bad Request.
- Run: `mvn test -Dtest=ValidationContractTest`

---

## §7 Negative Space (What We Deliberately Don't Test)

### Out of scope for this rollout

- **Internal admin endpoints** — low blast radius, low user surface; can ship without test coverage if endpoints are read-only and audit-logged.
- **Email notifications (post-MVP)** — explicitly deferred to v1.1 per roadmap; not tested in MVP.
- **Multi-user household sharing** — not in MVP scope; single-user only per PRD.
- **Task categories/tags** — deferred to v1.1; not in MVP scope.
- **Mobile-specific UI** — responsive design covers mobile browsers; native iOS/Android apps explicitly deferred.
- **Advanced analytics / mental-load scoring** — deferred; not in MVP.
- **UI snapshot tests** — brittle; prefer deterministic assertions (task text, order, state flags). Snapshot tests only for critical visual regressions (if approved post-Phase 1).
- **Gamification (points, badges, leaderboards)** — deferred; not in MVP scope.
- **External service integrations (Google Calendar, Slack)** — deferred to v1.1.

### Negative-space validation

These decisions are valid because:
1. Admin endpoints and deferred features have low user impact in MVP scope.
2. Responsive design verified via manual browser testing (not automated snapshot). Heavy automation added only if regressions surface.
3. Core risks (data loss, persistence, recurrence math, auth isolation) are covered in §2.

---

## Rollout Status

**Current step:** Awaiting Phase 1 kickoff.

- Phases 1–4: Not started. Change folders to be created per handoff.
- Estimated timeline: 2–3 days per phase (aggressive). Typical: 3–5 weeks total for all four phases, 1–2 per week if running in parallel.
- Downstream skills: `/10x-new` → `/10x-research` → `/10x-plan` → `/10x-implement` for each phase.

---

**Plan created:** 2026-09-05 by `/10x-test-plan`  
**Last updated:** 2026-09-05  
**Refresh cadence:** Re-run `/10x-test-plan --refresh` if a new top-3 risk surfaces, a tool's checked date is >3 months old, the tech stack changes, or §7 negative-space no longer matches team beliefs.