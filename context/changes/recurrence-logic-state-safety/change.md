---
id: recurrence-logic-state-safety
title: Recurrence Logic + State Safety Unit Tests
phase: Phase 3 (Rollout §3)
status: implementing
updated: 2026-09-06
last_phase_completed: 2
risks_covered:
  - Risk #3 (recurring task math)
  - Risk #7 (state inconsistency & transitions)
created: 2026-09-05
updated: 2026-09-05
research_completed: 2026-09-05T18:45Z
---

# Change: Recurrence Logic + State Safety Unit Tests

## Goal

Validate recurring task calculation logic and prove that state transitions don't introduce invariant violations. Unblock S-03 recurring task shipping.

## Risks Addressed

**Risk #3:** Recurring task math wrong — user misses weekly chores (e.g., "clean gutters every 7 days" due wrong date)
- **Impact:** High | **Likelihood:** Medium
- **Cheapest layer:** Unit test (math-only, parameterized boundary cases)

**Risk #7:** State inconsistency — task is both "complete" and "overdue" simultaneously; recurring completion breaks invariants
- **Impact:** Medium | **Likelihood:** Low-Medium
- **Cheapest layer:** Unit + integration tests (state transitions, database constraints)

## Scope

This change covers:
1. **Recurrence calculation unit tests** — boundary cases (leap year, month-end, DST, timezone)
2. **State invariant enforcement** — proving completed/completedAt consistency, no double-completion, parent-instance integrity
3. **Integration tests** — recurring task workflows (create → complete → verify next)
4. **NOT included:** Dashboard UI routing, endpoint-level authorization, persistence stress tests (those are Phase 3)

## Key Findings Summary

See `research.md` for detailed findings. Critical blockers:

1. **Dashboard recurrence bug** — completion doesn't trigger next instance generation
2. **No idempotency check** — calling completeTask() twice re-generates instances
3. **Invariant not enforced** — completed flag can diverge from completedAt timestamp
4. **Missing test coverage** — leap year, month-end cascades, workflow chains, soft-delete cascades

## Next Steps

1. `/10x-research` → read `research.md` (done)
2. `/10x-plan` → design unit test approach, state model constraints, remediation (blocks dashboard bug)
3. `/10x-implement` → write tests per plan, fix invariant gaps
