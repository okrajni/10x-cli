# Recurrence Logic + State Safety — Implementation Plan

## Overview

This plan adds comprehensive test coverage for recurring task logic and fixes three critical bugs that prevent the feature from working correctly:
1. Dashboard UI bypasses recurrence logic entirely
2. No guard against double-completion (duplicate instances)
3. Task state invariant (`completed ↔ completedAt`) not enforced

We prove recurring tasks work correctly, ship Phase 2 unblocked, and establish the testing patterns in the cookbook for later phases.

## Current State Analysis

### Existing Implementation
- **RecurrenceService.java** (lines 1-150) implements DAILY/WEEKLY/MONTHLY calculation with month-end edge case handling
- **TaskService.completeTask()** (lines 143-174) triggers recurrence via `shouldGenerateNext()` + `generateNextInstance()`
- **Database schema** (V002 migration) stores parent-child hierarchy via `parent_task_id`; has weekday CHECK constraint but no state invariant enforcement
- **Existing tests** cover basic paths (DAILY, WEEKLY, MONTHLY); missing edge cases (leap year, month-end cascades, soft-delete orphans)

### Critical Bugs Found
1. **Dashboard completion bypass** — TodayDashboardContainer.tsx (lines 67–87) calls `updateTask({ completedAt: NOW })` instead of `completeTask()` endpoint; recurrence logic never runs
2. **No idempotency guard** — TaskService.completeTask() has no check for already-completed tasks; calling twice generates duplicate instances
3. **State invariant violated** — updateTask() allows setting `completedAt` independently without enforcing `completed=true`; task can exist in invalid state

### Test Gaps
- Leap year boundary (Feb 29 transitions)
- Consecutive month-end cascades (Jan 31 → Feb 28 → Mar 31)
- Soft-delete orphan behavior (parent deleted, instances remain orphaned)
- Workflow chains (create → complete → verify next → complete next)
- Idempotency (call completeTask() twice → no duplicates)

### Key Architecture Constraints
- **Synchronous recurrence** — next instance generated immediately on completion; no background jobs
- **Parent-child model** — recurring parent stores config; each instance is separate Task with `parentTaskId`
- **Soft-delete only** — deleted tasks set `deletedAt` timestamp; instances not cascaded on parent delete
- **No RFC 5545 library** — recurrence uses simple enum (DAILY/WEEKLY/MONTHLY); no RRULE spec compliance

## Desired End State

After this plan completes:
1. **Dashboard bug fixed** — UI calls correct `completeTask()` endpoint; recurrence triggers in all paths
2. **Idempotency guaranteed** — calling completeTask() twice returns same task, no duplicates
3. **State invariant enforced** — `completed=true` ⟺ `completedAt≠NULL` guaranteed in code (application-level guard)
4. **Recurrence math validated** — parameterized unit tests with RFC 5545 oracle covering ~5 boundary cases
5. **State transitions proven** — integration tests verify complete → next workflow, soft-delete orphans, invariant holds
6. **Test patterns established** — cookbook entries (§6 Phase 2) define how to write recurrence + state tests for this project

**Verification:** All tests pass (`mvn test`), dashboard completion triggers recurrence, recurring tasks create correct next-occurrence dates.

## What We're NOT Doing

- **Dashboard UI refactor** — only the completion call changes, no layout/styling work
- **Migration or backfill** — database schema unchanged; guards operate on new/updated records
- **RFC 5545 full compliance** — using RFC 5545 library as oracle for tests only; implementation stays with simple enum
- **Soft-delete cascade** — orphaned instances remain visible (documented as known issue in test-plan §7)
- **Timezone-aware testing** — DST/timezone support deferred to future phases; tests use UTC
- **Phase 3 work** — authorization tests, performance benchmarks, other validations in Phase 3

## Implementation Approach

**Core strategy:**
1. **Fix bugs first (Phase 1)** — unblock recurrence logic so tests actually exercise the feature end-to-end
2. **Establish test infrastructure (Phase 2)** — add RFC 5545 library, create shared fixtures
3. **Unit test the math (Phase 3)** — parameterized tests with oracle-based validation for edge cases
4. **Integration test the workflow (Phase 4)** — full state transitions, orphan behavior, invariants

**Why this order:**
- Bugs fixed first ensures tests aren't testing broken code
- Library added before tests reduces test setup complexity
- Unit tests before integration tests (cheaper layer first per test-plan §1 principle)
- Integration tests verify end-to-end after units establish confidence

**Test oracle approach:**
- Use Java `rrule` library (RFC 5545 compliant) to compute expected recurrence dates
- Parameterized test cases: {startDate, frequency, weekday?, expected_nextDate}
- Assert actual RecurrenceService output matches oracle output
- Fallback: hardcoded reference table for simple cases

## Critical Implementation Details

**State invariant timing:** The guard in `TaskService.updateTask()` must check if `completedAt` is being set without `completed=true`, or vice versa. Reject invalid transitions at method entry before database update. Throw `IllegalArgumentException` with clear message — tests will expect and catch this.

**Idempotency gate:** Check `task.isCompleted()` at line 1 of `completeTask()` method. If true, return the existing task DTO immediately. No database write, no recurrence generation. Tests will verify calling twice returns identical TaskResponse (same timestamps, no duplicate instances).

**Dashboard fix surface:** One-line change in TodayDashboardContainer.tsx — replace the `updateTask()` call with the existing `completeTask()` function already imported from `api.ts`. Verify both TaskListPage.tsx (which already uses it) and TodayDashboardContainer now call the same endpoint.

---

## Phase 1: Bug Fixes & Invariant Guards

### Overview

Fix the three critical bugs blocking S-03 shipping and establish application-level guarantees for task state consistency. After this phase, the feature works end-to-end from UI to database.

### Changes Required

#### 1. TodayDashboardContainer.tsx — Fix dashboard completion bypass

**File**: `frontend/src/features/tasks/components/TodayDashboardContainer.tsx`

**Intent**: Dashboard task completion currently bypasses the recurrence endpoint and goes directly to `updateTask()`. Change it to call the dedicated `completeTask()` endpoint so recurrence logic triggers.

**Contract**: Replace the existing `updateTask()` call (lines 67–87) with `completeTask()` which already exists in the imported API module. The function signature is: `completeTask(taskId: string): Promise<ApiResult<Task>>`. Update the optimistic state handling to match (the API returns the completed task DTO).

**Code reference (current lines 75–78):**
```typescript
const result = await updateTask(taskId, { completedAt: new Date().toISOString() })
```

Should become:
```typescript
const result = await completeTask(taskId)
```

#### 2. TaskService.java — Add idempotency guard

**File**: `src/main/java/com/example/doneyet/service/TaskService.java`

**Intent**: Prevent double-completion by checking if the task is already marked complete. If so, return it immediately without generating a duplicate recurrence instance.

**Contract**: At the start of `completeTask()` method (line 144), add an early-return guard:
```java
if (task.isCompleted()) {
  return mapToResponse(task);
}
```
After validation but before any state changes. Existing test `testCompleteTaskIdempotency()` will verify calling twice returns the same task with no duplicate instances.

#### 3. TaskService.java — Add state invariant guard

**File**: `src/main/java/com/example/doneyet/service/TaskService.java`

**Intent**: Enforce the invariant that `completed=true` ⟺ `completedAt≠NULL`. Prevent invalid state where one flag is set without the other.

**Contract**: In `updateTask()` method (line ~95), before persisting the updated task, add validation:
```java
if ((updateRequest.getCompleted() != null && updateRequest.getCompleted() == true && updateRequest.getCompletedAt() == null) ||
    (updateRequest.getCompleted() != null && updateRequest.getCompleted() == false && updateRequest.getCompletedAt() != null)) {
  throw new IllegalArgumentException("Task completed flag and completedAt timestamp must be consistent");
}
```
This rejects attempts to set `completed=true` without `completedAt`, or `completed=false` with a non-null `completedAt`.

### Success Criteria

#### Automated Verification

- Type checking passes: `npm run typecheck` (frontend), `mvn clean compile` (backend)
- Linting passes: `npm run lint` (frontend), `mvn checkstyle:check` (backend)
- Dashboard fix compiles and no import errors
- Guard methods compile correctly with new validation logic
- Unit tests for guards pass: `mvn test -Dtest=TaskServiceTest`
- Existing recurrence tests still pass: `mvn test -Dtest=RecurrenceServiceTest`

#### Manual Verification

- Complete a recurring task from dashboard → verify next instance appears (test case: weekly task, expect next week)
- Try to set `completedAt` without `completed=true` via API (expect 400 Bad Request with error message)
- Call completeTask() twice rapidly → verify only one instance created, no duplicates
- Dashboard task list updates immediately after completion (optimistic + server-driven state sync works)

---

## Phase 2: Test Infrastructure & Dependencies

### Overview

Add the RFC 5545 library dependency and establish shared test fixtures for recurrence boundary cases. This prepares for unit + integration tests in Phases 3–4.

### Changes Required

#### 1. pom.xml — Add RFC 5545 library

**File**: `pom.xml`

**Intent**: Add a Java library that implements RFC 5545 (RRULE spec) to validate recurrence calculations. This serves as the oracle for unit tests.

**Contract**: Add the `rrule` library (or equivalent, e.g., `net.objectlab.kit:objectlab-kit-datecalc`) to the `<dependencies>` section. Example:
```xml
<dependency>
  <groupId>org.dmfs</groupId>
  <artifactId>rfc5545-datetime</artifactId>
  <version>0.2.5</version>
  <scope>test</scope>
</dependency>
```
Verify build succeeds: `mvn clean install`. Select library based on availability and ease of use for boundary-case validation.

#### 2. TestRecurrenceFixtures.java — Create shared test fixtures

**File**: `src/test/java/com/example/doneyet/TestRecurrenceFixtures.java`

**Intent**: Centralize boundary-case test data (leap year, month-end, etc.) so unit + integration tests reference the same oracle.

**Contract**: Create a public class with static method `getRecurrenceBoundaryCases()` returning a list of test records:
```java
public record RecurrenceTestCase(
    LocalDate startDate,
    RecurrenceFrequency frequency,
    Integer weekday,  // null if not WEEKLY
    LocalDate expectedNextDate
) {}

public static List<RecurrenceTestCase> getRecurrenceBoundaryCases() {
  return List.of(
    // ~5 boundary cases: leap year, month-end, daily wraparound, etc.
  );
}
```
Populate with cases discovered in research (section 5.1, table of test gaps).

### Success Criteria

#### Automated Verification

- Maven build succeeds: `mvn clean install`
- RFC 5545 library resolves from Maven Central (no version conflicts)
- No compilation errors in TestRecurrenceFixtures.java
- Fixtures are accessible from test classes: `import static com.example.doneyet.TestRecurrenceFixtures.*`

#### Manual Verification

- Manually inspect TestRecurrenceFixtures to verify boundary cases are representative (leap year, month-end, etc.)
- RFC 5545 library is added to classpath: `mvn dependency:tree | grep rfc5545`

---

## Phase 3: Recurrence Math Unit Tests

### Overview

Expand `RecurrenceServiceTest.java` with parameterized boundary-case tests validated against the RFC 5545 oracle. This proves recurrence math is correct for edge cases.

### Changes Required

#### 1. RecurrenceServiceTest.java — Parameterized boundary tests

**File**: `src/test/java/com/example/doneyet/service/RecurrenceServiceTest.java`

**Intent**: Add `@ParameterizedTest` cases covering ~5 recurrence boundary scenarios (leap year, month-end, daily wraparound, etc.). Use RFC 5545 library to compute expected next date, assert RecurrenceService matches.

**Contract**: Add method:
```java
@ParameterizedTest(name = "Recurrence {0}: {1} from {2} => {3}")
@MethodSource("getRecurrenceBoundaryCases")
void testComputeNextDueDateMatchesRfc5545Oracle(
    String description,
    RecurrenceTestCase testCase
) {
  Task task = new Task();
  task.setDueDate(testCase.startDate());
  task.setRecurrenceFrequency(testCase.frequency());
  if (testCase.weekday() != null) {
    task.setRecurrenceWeekday(testCase.weekday());
  }
  
  LocalDate actual = recurrenceService.computeNextDueDate(task);
  LocalDate expected = testCase.expectedNextDate();
  
  assertEquals(expected, actual, "Mismatch for " + description);
}

private static Stream<Arguments> getRecurrenceBoundaryCases() {
  return TestRecurrenceFixtures.getRecurrenceBoundaryCases()
    .stream()
    .map(tc -> Arguments.of(tc.toString(), tc));
}
```

Populate with ~5 cases from TestRecurrenceFixtures (leap year, Jan 31→Feb, daily month-wrap, etc.).

### Success Criteria

#### Automated Verification

- Unit tests compile: `mvn clean compile`
- All recurrence tests pass: `mvn test -Dtest=RecurrenceServiceTest`
- Parameterized tests run with correct count (~5 cases shown in test output)
- No assertion failures; all boundary cases produce expected dates
- Existing tests still pass (no regression)

#### Manual Verification

- Review test output to verify each parameterized case executed
- Spot-check one boundary case manually (e.g., Jan 31 + 1 month → Feb 28 on non-leap year)
- RFC 5545 oracle results make intuitive sense for each case

---

## Phase 4: State Transition & Integration Tests

### Overview

Create `TaskRecurrenceIntegrationTest.java` to prove end-to-end recurrence workflows work correctly and state invariants hold. Test dashboard completion, soft-delete orphans, and idempotency guard.

### Changes Required

#### 1. TaskRecurrenceIntegrationTest.java — Integration test suite

**File**: `src/test/java/com/example/doneyet/api/TaskRecurrenceIntegrationTest.java`

**Intent**: Test the complete recurrence workflow (create recurring → mark complete → verify next instance appears) at the integration level. Verify state invariants hold and soft-delete orphans are handled.

**Contract**: Create test class with:
- **Setup:** User, household, test database fixture
- **Test: Recurring task completion workflow**
  ```
  1. Create recurring task (weekly, due Mon)
  2. Verify completed=false, completedAt=null
  3. Call completeTask()
  4. Verify old instance: completed=true, completedAt=NOW, hidden from dashboard
  5. Verify new instance: created, due next Monday, incomplete
  6. Verify state invariants: all tasks satisfy completed ↔ completedAt
  ```
- **Test: Idempotency guard**
  ```
  1. Create task, call completeTask()
  2. Call completeTask() again with same taskId
  3. Verify no duplicate instances created (only 1 next instance)
  ```
- **Test: Soft-delete orphans**
  ```
  1. Create recurring parent task with 3 instances
  2. Delete parent task (soft-delete via DELETE /task/:id)
  3. Verify instances remain in database (orphaned)
  4. Verify instances not returned by dashboard query (filtered by deletedAt IS NULL on parent)
  ```
- **Test: State invariant violation rejected**
  ```
  1. Attempt to PATCH task with completedAt=NOW but completed=false
  2. Expect 400 Bad Request
  3. Verify task state unchanged in database
  ```

### Success Criteria

#### Automated Verification

- Integration tests compile: `mvn clean compile -DskipTests=false`
- All tests pass: `mvn test -Dtest=TaskRecurrenceIntegrationTest`
- Test database setup succeeds (fixtures created)
- No assertion failures in workflow, idempotency, orphan, or invariant tests
- Existing integration tests still pass (no regression): `mvn test -Dtest=TaskControllerIntegrationTest`

#### Manual Verification

- Run test suite and inspect output to verify all 4 test scenarios executed
- Manually complete a recurring task from TaskListPage (not dashboard) and verify next instance appears within 5 seconds
- Manually check that soft-deleted parent's instances still exist in database but don't appear in task list
- Review test code for clarity (future developers should understand each test's intent)

---

## Testing Strategy

### Unit Tests (Phase 3)

**What:** Recurrence calculation logic (RecurrenceService)

**How:** Parameterized tests over 5 boundary cases (leap year, month-end, etc.). Each case provides start date, frequency, expected next date. Assert actual matches expected using RFC 5545 library as oracle.

**Coverage:** DAILY, WEEKLY, MONTHLY basic cases + edge cases. Does NOT test UI, does NOT test database persistence, does NOT test complete → next workflow (that's integration).

**Run:** `mvn test -Dtest=RecurrenceServiceTest`

### Integration Tests (Phase 4)

**What:** State transitions, workflow chains, invariant enforcement, soft-delete behavior

**How:** Test at API level (POST /task, PUT /task/{id}/complete, DELETE /task/{id}). Create fixtures (user, household, tasks), call endpoints, verify database state after each call.

**Coverage:** Complete recurring task → next instance appears. Idempotency guard (call twice → 1 instance, not 2). State invariant enforcement (reject invalid completedAt without completed). Soft-delete orphans. Does NOT test UI performance, does NOT test authorization boundaries (Phase 3 work).

**Run:** `mvn test -Dtest=TaskRecurrenceIntegrationTest`

### Manual Testing Steps

1. **Dashboard completion triggers recurrence:**
   - Open dashboard, create weekly task "Clean gutters" due Monday
   - Click "Complete" on task card
   - Verify task disappears from dashboard
   - Verify new task appears "Clean gutters" due next Monday

2. **State invariants hold:**
   - Use Postman/curl to PATCH `/task/{id}` with `{ "completedAt": "2026-09-05T12:00Z" }` (no `completed` flag)
   - Expect 400 Bad Request with error message
   - Verify task state unchanged in database

3. **Soft-delete orphans:**
   - Create recurring task with 3 instances visible
   - Delete parent task via API (DELETE /task/{id})
   - Query database directly: `SELECT * FROM task WHERE parent_task_id = <parent_id>`
   - Verify instances still in database (orphaned)
   - Refresh dashboard: verify instances don't appear (dashboard filters by `deletedAt IS NULL`)

---

## Performance Considerations

- **Recurrence calculation:** Happens synchronously in `completeTask()`. With the idempotency guard, no duplicate queries/inserts. Should add <10ms to completion latency.
- **Integration tests:** Use in-memory/test database; should run in <5 seconds total
- **Unit tests:** Parameterized with 5 cases; should run <1 second
- **No performance gate in Phase 2** — latency testing in Phase 3 (dashboard <500ms benchmark)

## Migration Notes

**Database schema:** No migration needed. The V002 schema already supports the changes:
- `parent_task_id` foreign key exists
- `recurrence_*` fields exist
- Weekday CHECK constraint exists

**Existing data:** No data transformation. Invariant guard operates on new/updated records. Existing tasks with null `completedAt` remain valid (not complete).

## References

- Research: `context/changes/recurrence-logic-state-safety/research.md` — detailed bug analysis, test gaps, architecture
- Test plan strategy: `context/foundation/test-plan.md` §3 Phase 2, §6 Cookbook Phase 2
- Code locations: RecurrenceService.java:1–150, TaskService.java:143–174, TodayDashboardContainer.tsx:67–87
- RFC 5545 spec: https://datatracker.ietf.org/doc/html/rfc5545 (informational, not needed for implementation)

---

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles.

### Phase 1: Bug Fixes & Invariant Guards

#### Automated

- [x] 1.1 Type checking passes (frontend + backend)
- [x] 1.2 Linting passes (frontend + backend)
- [x] 1.3 Dashboard fix compiles with no import errors
- [x] 1.4 Guard methods compile correctly
- [x] 1.5 TaskServiceTest passes with new guard logic
- [x] 1.6 Existing recurrence tests still pass: mvn test -Dtest=RecurrenceServiceTest

#### Manual

- [x] 1.6 Dashboard completion triggers recurrence (verify next instance appears)
- [x] 1.7 Idempotency guard works (call completeTask() twice → no duplicates)
- [x] 1.8 State invariant guard works (reject invalid completedAt without completed)

### Phase 2: Test Infrastructure & Dependencies

#### Automated

- [ ] 2.1 Maven build succeeds with RFC 5545 library added
- [ ] 2.2 RFC 5545 library resolves from Maven Central
- [ ] 2.3 TestRecurrenceFixtures compiles with no errors
- [ ] 2.4 Fixtures accessible from test classes

#### Manual

- [ ] 2.5 Review TestRecurrenceFixtures for representative boundary cases
- [ ] 2.6 Verify RFC 5545 library appears in dependency tree

### Phase 3: Recurrence Math Unit Tests

#### Automated

- [ ] 3.1 RecurrenceServiceTest compiles
- [ ] 3.2 Parameterized tests run (5 boundary cases)
- [ ] 3.3 All recurrence boundary tests pass
- [ ] 3.4 No regression in existing RecurrenceServiceTest tests

#### Manual

- [ ] 3.5 Review parameterized test output to verify all 5 cases executed
- [ ] 3.6 Spot-check one boundary case manually (e.g., Jan 31 + 1 month)

### Phase 4: State Transition & Integration Tests

#### Automated

- [ ] 4.1 TaskRecurrenceIntegrationTest compiles
- [ ] 4.2 Workflow test (create recurring → complete → verify next) passes
- [ ] 4.3 Idempotency test (call twice → 1 instance) passes
- [ ] 4.4 Soft-delete orphan test passes
- [ ] 4.5 State invariant violation test (reject invalid) passes
- [ ] 4.6 No regression in existing TaskControllerIntegrationTest tests

#### Manual

- [ ] 4.7 Dashboard completion triggers recurrence end-to-end
- [ ] 4.8 State invariant guard rejects invalid via API
- [ ] 4.9 Soft-delete orphans remain in DB but hidden from dashboard
