# Recurrence Logic + State Safety — Plan Brief

> Full plan: `context/changes/recurrence-logic-state-safety/plan.md`  
> Research: `context/changes/recurrence-logic-state-safety/research.md`

## What & Why

We're fixing three critical bugs in recurring task logic (S-03) and adding comprehensive test coverage to prove the feature works end-to-end. The bugs prevent dashboard users from creating recurring task chains, break idempotency (duplicates on double-click), and allow invalid task state (complete without timestamp).

**Research finding:** "Test readiness: Phase 2 cannot ship without fixing the dashboard bug and adding state invariant tests."

## Starting Point

Recurrence logic is **implemented but untested for edge cases and unguarded against bugs**:
- RecurrenceService.java calculates next occurrence (DAILY/WEEKLY/MONTHLY with month-end handling)
- TaskService.completeTask() triggers recurrence on task completion
- **Bug #1:** Dashboard calls updateTask() instead of completeTask() → recurrence never runs
- **Bug #2:** No idempotency check → calling completeTask() twice generates duplicate instances
- **Bug #3:** No state invariant enforcement → task can be "complete but no timestamp"
- **Test gaps:** Leap year, month-end cascades, workflow chains, soft-delete orphans all untested

## Desired End State

After this plan ships:
1. Dashboard completion calls correct endpoint; recurring tasks auto-generate next instances
2. Idempotency guard prevents duplicate instances on rapid clicks
3. Task state invariant (`completed=true` ↔ `completedAt≠NULL`) enforced at application level
4. ~5 recurrence boundary cases validated via RFC 5545 oracle (leap year, month-end, etc.)
5. End-to-end workflow tested: create recurring → complete → next appears
6. Soft-delete orphan behavior locked in via test (current: instances orphaned, not cascaded)

## Key Decisions Made

| Decision | Choice | Why | Source |
|----------|--------|-----|--------|
| Bug remediation scope | Fix all 3 bugs in Phase 1 | Research says "cannot ship without fixing"; better to unblock Phase 2 completely | Research |
| Invariant enforcement | Application-level guard in TaskService | Quick to implement, no schema migration, catches 99% of violations | Plan |
| Soft-delete orphans | Test current behavior; document as known issue | Locks in current behavior, clear signal if should change later | Plan |
| Recurrence oracle | RFC 5545 library (Java `rrule` or equivalent) | Standard, battle-tested, validates against thousands of date scenarios | Plan |
| Edge case coverage | ~5 parameterized test cases | Moderate depth: leap year, month-end, daily wraparound, recurring end-date, timezone edge | Plan |
| Idempotency fix | Guard check in TaskService.completeTask() | Simplest fix, immediate safety net in all caller contexts | Plan |

## Scope

**In scope:**
- Fix dashboard bug (TodayDashboardContainer.tsx → call completeTask endpoint)
- Add idempotency guard to TaskService.completeTask()
- Add state invariant guard to TaskService.updateTask()
- Add RFC 5545 library dependency
- Parameterized unit tests for recurrence (5 boundary cases)
- Integration tests for state transitions (workflow, orphans, invariants)

**Out of scope:**
- Dashboard UI refactor (only the completion call changes)
- Database schema/migration (guards operate on new records)
- Soft-delete cascade (orphaned instances remain visible — documented in test-plan §7)
- Timezone/DST support (deferred to future phases)
- Authorization tests, performance benchmarks (Phase 3)

## Architecture / Approach

**Sequence of 4 phases:**

1. **Phase 1: Bug fixes** — Dashboard completion, idempotency guard, state invariant guard
2. **Phase 2: Test infrastructure** — Add RFC 5545 library, create shared test fixtures
3. **Phase 3: Unit tests** — Parameterized recurrence math tests (leap year, month-end, etc.) with RFC 5545 oracle
4. **Phase 4: Integration tests** — Workflow chains, orphan behavior, invariant enforcement end-to-end

**Why this order:**
- Bugs fixed first so tests exercise working code
- Library added before tests reduces setup complexity
- Unit tests before integration (cheaper layer first per test-plan §1)
- Each phase has clear deliverables and can ship independently

**Test oracle:** Use RFC 5545 library to compute expected recurrence dates; assert actual matches expected. Fallback: hardcoded reference table for simple cases.

## Phases at a Glance

| Phase | What it delivers | Key risk |
|-------|------------------|----------|
| 1. Bug fixes | Dashboard works; no duplicates; state invariant enforced | Invariant guard must catch all invalid transitions; guard placement in updateTask() |
| 2. Infrastructure | RFC 5545 library added; test fixtures ready | Library selection (which RFC 5545 impl?); fixture quality (are 5 cases representative?) |
| 3. Unit tests | Recurrence math validated for 5 boundary cases | Oracle accuracy (is RFC 5545 library correct?); parameterized test clarity |
| 4. Integration | Workflow chains, orphans, invariants proven end-to-end | Test database setup; complexity of soft-delete orphan verification |

**Prerequisites:** 
- Phase 1 requires read access to TaskService.java, TodayDashboardContainer.tsx
- Phase 2 requires Maven pom.xml access
- Phases 3–4 require test framework setup (JUnit 5 already present)

**Estimated effort:** 
- Phase 1: 1–2 hours (3 small fixes)
- Phase 2: 30 min (add dependency + fixture class)
- Phase 3: 2–3 hours (parameterized tests, oracle integration)
- Phase 4: 3–4 hours (integration test suite, workflow scenarios)
- **Total:** ~8–10 hours across 2–3 sessions

## Open Risks & Assumptions

- **RFC 5545 library availability:** Assuming `org.dmfs:rfc5545-datetime` or equivalent exists in Maven Central. If not, fallback to hardcoded reference table (slower oracle, but functional).
- **State invariant scope:** Guard in updateTask() must catch incomplete → complete without timestamp AND complete → incomplete with timestamp. Test must verify both directions.
- **Soft-delete orphans:** Current behavior is to leave instances orphaned (not cascaded). Plan locks this in via test. If desired behavior changes later, test will need update.
- **Idempotency detection:** Guard checks `task.isCompleted()` — assumes this method or equivalent exists in Task entity. If not, guard checks `completed == true`.

## Success Criteria (Summary)

- ✅ All 4 phases pass automated tests (unit, integration, type-checking, linting)
- ✅ Dashboard completion triggers recurrence (manual: complete weekly task → next instance appears within 5s)
- ✅ Idempotency guard prevents duplicates (manual or automated: call completeTask() twice → 1 instance, not 2)
- ✅ State invariant enforced (manual or automated: reject PATCH with invalid completedAt/completed combo)
- ✅ Test plan cookbook (§6 Phase 2) updated with reference tests and run commands for future developers
