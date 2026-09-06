---
date: 2026-09-05T18:45:00Z
researcher: Claude (ultrathink mode)
git_commit: 3f2db728bf8d5a73d92678e9a83d6a83d1fc6e80
branch: module3
repository: 10x-cli (done-yet)
topic: "Recurrence Logic Implementation & State Safety Unit Test Strategy"
tags: [research, recurrence, state-safety, unit-testing, integration-testing, edge-cases]
status: complete
last_updated: 2026-09-05
last_updated_by: Claude
---

# Research: Recurrence Logic Implementation & State Safety Unit Test Strategy

**Date:** 2026-09-05 18:45 UTC  
**Researcher:** Claude  
**Git Commit:** 3f2db728bf8d5a73d92678e9a83d6a83d1fc6e80  
**Branch:** module3  
**Repository:** done-yet (10x-cli)

---

## Research Question

**For Phase 2 of the test plan:** What recurrence logic exists in the codebase, how does it interact with task state transitions, what state invariants must hold, and what unit + integration tests are required to prove it works correctly and safely?

---

## Summary

The recurring task system is **fully implemented** but **missing test coverage for edge cases and state safety invariants**. Core findings:

1. **Recurrence calculation is sound** — DAILY/WEEKLY/MONTHLY logic implemented via `RecurrenceService`, with month-end edge case handling (Jan 31 → Feb 28)
2. **Data model uses parent-child hierarchy** — Recurring tasks have `parentTaskId` linking instances; next occurrence created synchronously on task completion
3. **Critical state safety bug in dashboard** — UI calls `updateTask()` directly, bypassing the `completeTask()` endpoint, so recurrence logic never triggers
4. **No idempotency enforcement** — Calling `completeTask()` twice overwrites timestamp and may re-generate instances
5. **Invariants not enforced** — No database or application-level constraints ensuring `completed=true ⟺ completedAt≠NULL`
6. **Large test gaps** — Leap year, month-end cascades, workflow chains, soft-delete behavior, timezone sensitivity all untested

**Test readiness:** Phase 2 cannot ship without fixing the dashboard bug and adding state invariant tests.

---

## Detailed Findings

### 1. Recurrence Calculation Logic

#### Implementation Location & Architecture
- **Primary service:** `src/main/java/com/example/doneyet/service/RecurrenceService.java` (lines 1-150)
- **Supported frequencies:** DAILY, WEEKLY, MONTHLY (via enum at `domain/RecurrenceFrequency.java`)
- **Data model:** `src/main/java/com/example/doneyet/domain/Task.java` stores:
  - `recurrenceFrequency: String` (DAILY/WEEKLY/MONTHLY)
  - `recurrenceWeekday: Integer` (0-6, Sun=0, required for WEEKLY)
  - `recurrenceEndDate: LocalDate` (optional, stops recurrence if reached)
  - `parentTaskId: UUID` (links instances to parent)

#### Calculation Methods (RecurrenceService.java)

| Method | Lines | Logic | Edge Cases |
|--------|-------|-------|-----------|
| `computeNextDueDate()` | 20-35 | Routes DAILY/WEEKLY/MONTHLY to frequency-specific logic | Only MONTHLY explicitly handles month-end |
| `getNextWeekday()` | 37-48 | Finds next occurrence of target weekday (0-based); minimum 7 days forward | Correctly handles week-wrap (Fri→Mon) |
| `addMonths()` | 80-86 | Adds N months, handles day-of-month clamping (e.g., Jan 31 → Feb 28) | Uses `Math.min(originalDay, maxDayOfMonth)` |
| `generateNextInstance()` | 50-61 | Creates new task with same title/category/description, sets `parentTaskId`, copies recurrence config | Preserves all properties except completion state |
| `shouldGenerateNext()` | 63-78 | Returns false if: (a) no frequency, (b) end date <= nextDueDate | Prevents over-generation |

#### Calculation Quality
✅ **Strengths:**
- DAILY is trivial (+1 day)
- WEEKLY correctly implements weekday targeting (not just 7-day fixed)
- MONTHLY handles month-end (Jan 31 stays on 31st for months with 31 days; falls back to 28 for Feb)

⚠️ **Known gaps:**
- **Leap year:** Only one test (Feb 29 → Mar 29 not explicitly tested, only Feb 28 edge case)
- **DST/Timezone:** No timezone handling; uses `LocalDate` (calendar-only, not datetime), so DST should not affect date arithmetic
- **Consecutive month-end:** No test for repeated monthly on day 31 across Jan→Feb→Mar transition
- **Annual/quarterly:** Not implemented

**Test Oracle:** Current tests use hardcoded expected dates, not RFC 5545 (RRULE spec) or external library validation.

---

### 2. Task State Model & Invariants

#### State Fields
- **`completed: boolean`** — Primary state flag (default: false)
- **`completedAt: LocalDateTime`** — When marked complete (nullable, null means incomplete)
- **`deletedAt: LocalDateTime`** — Soft-delete marker (null = active)
- **`parentTaskId: UUID`** — Recurring instance parent (null = parent/standalone task)
- **Recurrence config fields** — `recurrenceFrequency`, `recurrenceWeekday`, `recurrenceEndDate`

#### Valid State Transitions

```
[Created] completed=false, completedAt=null
    ↓ [updateTask - any field]
[Incomplete] completedAt=null (can update fields)
    ↓ [completeTask] 
[Complete] completed=true, completedAt=NOW
    └─ [IF recurring] → trigger RecurrenceService.generateNextInstance()
                       → create sibling with same parentTaskId
    ↓ [deleteTask]
[Deleted] deletedAt=NOW (soft-delete, hidden from queries)
```

**Transition enforcement:** Service layer only (TaskService.java:144-174); no state machine formalism.

#### State Invariants (Should Hold)

| Invariant | Enforced | Risk | Notes |
|-----------|----------|------|-------|
| Soft-deleted tasks hidden from queries | ✅ YES (filter `deletedAt IS NULL` in all repos) | Low | All queries properly filtered |
| `completed=true` ⟺ `completedAt≠NULL` | ❌ NO | **HIGH** | Can call updateTask() to set completedAt independently; can cause "complete but no timestamp" state |
| WEEKLY recurrence requires weekday in [0,6] | ✅ YES (DB CHECK + TaskService.validateRecurrenceFields) | Low | Enforced at creation/update |
| `recurrenceEndDate ≥ dueDate` | ❌ NO | Medium | No validation; end date can be before task due date |
| No circular parentTaskId | ❌ NO | Medium | Only FK exists; could theoretically create instance → parent → instance loop |
| Task completed at most once | ❌ NO | **CRITICAL** | No idempotency check; calling completeTask() twice will overwrite completedAt AND re-generate recurrence instance (duplicate) |

#### Overdue Logic
**Definition:** Frontend only (TaskCard.tsx:26)
```typescript
const isOverdue = dueDate < new Date() && !isCompleted
```

- Computed client-side in browser timezone
- Task is overdue if `dueDate < TODAY` AND `completedAt = NULL`
- Completed tasks never show as overdue
- **Risk:** DST/timezone mismatch — if user's local time shifts, overdue status flips unexpectedly

---

### 3. Recurring Task Integration Points

#### A. Task Creation (POST /api/task)
- **Entry:** `TaskService.createTask()` (lines 36-57)
- **Validation:** `validateRecurrenceFields()` (lines 134-141) ensures WEEKLY has weekday
- **Storage:** All recurrence fields saved on Task entity
- **Frontend:** `TaskCreatePage.tsx` (lines 212-290) provides frequency dropdown, optional weekday picker, end-date input

#### B. Task Completion (PUT /api/task/{id}/complete) ⭐ PRIMARY
- **Entry:** `TaskService.completeTask()` (lines 144-174)
- **Flow:**
  1. Validate ownership + user exists (line 145)
  2. Set `completed=true`, `completedAt=now()` (lines 149-151)
  3. Persist to database (line 152)
  4. Fetch parent task if this is an instance (lines 156-159)
  5. Call `recurrenceService.shouldGenerateNext()` (line 168) → checks frequency + end date
  6. IF true: `recurrenceService.generateNextInstance()` (lines 170-171) → creates new instance
  7. Return updated task
- **Return:** Single updated task (old completed instance)
- **Synchronous:** Entire operation blocks; no background queue

#### C. Dashboard Task Display (GET /api/task)
- **Query:** `TaskRepository.findByHousehold()` (lines 16-17) — returns all active tasks (parent + instances)
- **Includes:** All recurrence fields in response
- **Filtering:** Shows both parent recurring tasks AND all completed instances (not hidden)
- **Grouping:** Frontend groups by status:
  1. Overdue incomplete
  2. Today incomplete
  3. Today/overdue completed
  - Sorting: by dueDate (oldest first)

#### D. Recurrence Calculation Entry Points
1. **`computeNextDueDate()`** — Called on completion; returns next LocalDate
2. **`generateNextInstance()`** — Creates new Task entity, sets parentTaskId, copies recurrence config
3. **`shouldGenerateNext()`** — Guard before generation; checks frequency + end date

---

### 4. Critical Bugs Found 🚨

#### Bug #1: Dashboard Completion Doesn't Trigger Recurrence
**Severity:** CRITICAL (blocks S-03 shipping)

**Location:** `frontend/src/features/tasks/components/TodayDashboardContainer.tsx:75`

**Current code:**
```typescript
const result = await updateTask(taskId, { completedAt: new Date().toISOString() })
```

**Should be:**
```typescript
const result = await completeTask(taskId)  // Calls PUT /api/task/{id}/complete
```

**Impact:** When user marks task complete from dashboard, only `completedAt` is set. The `PUT /api/task/{id}/complete` endpoint (which triggers recurrence logic) is never called. **Next recurring instance is NOT created.**

**Evidence:** 
- `TaskService.completeTask()` (lines 159-171) contains all recurrence logic
- Dashboard UI bypasses this and calls `updateTask()` instead
- No test validates dashboard → complete → recurrence chain

---

#### Bug #2: No Idempotency Check on Task Completion
**Severity:** HIGH (duplicate recurrence instances possible)

**Location:** `TaskService.completeTask()` (lines 144-174)

**Issue:** No check for `if (task.isAlreadyCompleted()) return`. Calling `completeTask()` twice with same taskId will:
1. First call: Set `completedAt=T1`, generate next instance
2. Second call: Overwrite `completedAt=T2`, re-generate next instance (duplicate!)

**Evidence:** No guard condition in method body

---

#### Bug #3: State Invariant Not Enforced
**Severity:** HIGH (data corruption possible)

**Invariant:** `completed=true` ⟺ `completedAt≠NULL` (completed iff timestamp exists)

**Problem:** `UpdateTaskRequest` (line 95 in TaskDto.java) allows setting `completedAt` independently. Can call:
```
PATCH /task/{id} { "completedAt": "2026-09-05T12:00Z" }
```
Without setting `completed=true`. **Task has timestamp but `completed=false`.**

**Evidence:** No validation in `updateTask()` to enforce invariant

---

### 5. Test Coverage Gaps

#### Missing Recurrence Edge Cases

| Scenario | Current Test | Gap | Impact |
|----------|--------------|-----|--------|
| Leap year (Feb 29 → Mar 29) | ❌ NO | Can't verify correct day preservation in leap year | High |
| Consecutive month-end | ❌ NO | Jan 31 → Feb (28/29) → Mar 31 chain | High |
| Daily recurrence to month-end | ❌ NO | Sep 28 → 29 → 30 → 1st (month wrap) | Medium |
| Recurrence end date = next due date | ❌ NO | Edge between "should generate" and "end reached" | Medium |
| Recurrence end date in past | ❌ NO | Creating task with past end date | Low |
| Recurrence weekday 0-6 boundaries | ✅ YES (7-day wrap tested) | But not boundary values (-1, 7) | Low |
| Monthly on day 31 repeated (all months) | ❌ NO | Cascading behavior across Feb/Apr/Jun/Sep/Nov | High |

#### Missing State Transition Tests

| Scenario | Current Test | Gap |
|----------|--------------|-----|
| Overdue task detection | ❌ NO | Can't verify isOverdue=true when dueDate < today |
| Mark complete then complete again (idempotency) | ❌ NO | Duplicate recurrence generation not detected |
| Soft-delete parent task | ❌ NO | What happens to instances? (orphaned?) |
| Update parent recurrence config after instances exist | ❌ NO | Does config change propagate? Should it? |
| Cascade from completed to next instance | ❌ NO | Full workflow (complete A → verify B exists & correct) |
| Mark completed task incomplete (rollback) | ❌ NO | No endpoint to toggle incomplete |

#### Missing Integration/Workflow Tests

| Workflow | Test | Gap |
|----------|------|-----|
| Create recurring → Complete → Verify next → Complete next → Verify chain | ❌ NO | Full recurrence chain never tested end-to-end |
| Dashboard completion (bug #1) | ❌ NO | Dashboard flow with recurrence |
| Rapid successive completions | ❌ NO | Race condition or duplicate check |
| Persistence verification | ✅ YES (basic) | But not with recurrence state (parent links, next-occurrence accuracy) |

#### Existing Tests (Sufficient Coverage)

- **RecurrenceServiceTest.java** (lines 1-151) — DAILY, WEEKLY, MONTHLY basic cases + month-end
- **TaskServiceTest.java** (lines 1-251) — Create/update/delete/complete, WEEKLY weekday validation, completion triggers next
- **GeminiClientTest.java** — Parsing recurrence JSON, rejecting invalid weekday

---

## Code References

### Backend - Core Recurrence Logic
- `src/main/java/com/example/doneyet/service/RecurrenceService.java` — Calculation engine
- `src/main/java/com/example/doneyet/service/TaskService.java:144-174` — Task completion + recurrence trigger
- `src/main/java/com/example/doneyet/domain/Task.java:60-71` — Recurrence fields on entity
- `src/main/java/com/example/doneyet/repository/TaskRepository.java:16-17` — Query for dashboard

### Frontend - UI & Integration
- `frontend/src/features/tasks/components/TodayDashboardContainer.tsx:75` — **DASHBOARD BUG HERE**
- `frontend/src/features/tasks/pages/TaskCreatePage.tsx:212-290` — Recurrence form
- `frontend/src/features/tasks/pages/TaskEditPage.tsx:231-331` — Recurrence editing
- `frontend/src/shared/components/TaskCard.tsx:70-78` — Display recurrence info
- `frontend/src/features/tasks/utils/recurrenceUtils.ts:3-23` — Format recurrence text

### Database Schema
- `src/main/resources/db/migration/V002__Add_recurrence_fields.sql` — Columns: parent_task_id, recurrence_frequency, recurrence_weekday, recurrence_end_date

### Tests
- `src/test/java/com/example/doneyet/service/RecurrenceServiceTest.java` — Calculation tests
- `src/test/java/com/example/doneyet/service/TaskServiceTest.java` — Completion & recurrence flow tests

---

## Architecture Insights

### Recurrence Is Synchronous
- Next instance generated **immediately** on task completion (blocking operation)
- No background jobs (`@Scheduled` not found anywhere)
- Completes endpoint caller waits for instance creation

**Implication:** If instance creation fails, entire completeTask() call fails (no partial state).

### Parent-Child Hierarchy Model
- Recurring parent stores configuration (frequency, weekday, end-date)
- Each instance is a separate Task with `parentTaskId` pointing to parent
- When parent completed: new instance created as sibling (same parent)
- All instances visible in task list (not hidden by completion)

**Implication:** Dashboard shows full history; user sees both old completed tasks and upcoming instances.

### No Soft Cascades
- When parent deleted, instances remain orphaned (parentTaskId points to deleted task)
- No automatic cleanup of instances when parent deleted
- No "recalculate next date" job if schedule missed

---

## Historical Context

### Recent Implementation (Commit 3f2db72)
- **Commit:** "chore(module2): complete implementation reviews and add S-03 task recurrence"
- **Date:** 2026-09-05 (today)
- **Scope:** S-03 (recurring tasks) marked as implemented; module2 reviews completed
- **Implication:** Phase 2 research is validating fresh implementation; no prior test history

### Test Plan Alignment
The test-plan.md (created 2026-09-05) defines:
- **Phase 2 goal:** Validate recurring task math; prove state transitions don't introduce bugs
- **Risks:** #3 (recurring math), #7 (state inconsistency)
- **Cookbook location:** Phase 2 should fill `test-plan.md §6 Phase 2` with unit test patterns

---

## Related Research

None yet (this is Phase 2 kickoff).

---

## Open Questions & Blockers

### For Planning Phase

1. **Dashboard bug remediation:** Should we fix bug #1 in Phase 2 implementation, or defer to Phase 3 (frontend) as a blocker?

2. **Idempotency approach:** 
   - Add `if (task.completed) return task;` guard?
   - Use database unique constraint on (parentTaskId, completedAt)?
   - Explicit transaction handling?

3. **Invariant enforcement:**
   - Add database CHECK constraint for `(completed AND completedAt NOT NULL) OR (NOT completed)`?
   - Or enforce in application layer only?

4. **Test boundaries:**
   - Should unit tests mock RecurrenceService, or use real calculations (integration)?
   - How many recurrence edge cases to test (leap year cascades, DST, etc.)?

5. **Soft-delete orphans:**
   - What's desired behavior when parent recurring task is deleted?
   - Should instances be automatically deleted, or remain visible?

### For Future Investigation

- Timezone handling strategy (if non-UTC users are added)
- Performance of parent-instance queries under large recurrence chains (100+ instances)
- Multi-user household support (do recurrence dates stay aligned if household users are in different timezones?)

---

## Recommendations for Phase 2 Planning

### Must-Fix Before Shipping
1. **Dashboard bug #1** — Recurrence logic never triggered from UI
2. **Idempotency check** — Prevent double-completion duplicate instances
3. **Invariant enforcement** — At least application-level guard for `completed ↔ completedAt`

### Must-Test Before Shipping
1. **Leap year** — Feb 29 edge case (minimum for #3 risk)
2. **Month-end cascade** — Jan 31 chain across Feb/Mar (high impact)
3. **Workflow chain** — Create → Complete → Verify next → Complete next (end-to-end #7 risk)
4. **Soft-delete orphan** — What happens to instances when parent deleted?

### Nice-to-Have (Later)
- Boundary validation tests (weekday -1/7, past end-date)
- Timezone-aware tests (if TZ support planned)
- Race condition tests (rapid successive completions)

---

## Change Status

**Research complete.** Ready for `/10x-plan` handoff.

**Blockers:** Dashboard bug #1 is blocking S-03 shipping; mark as dependency in plan.
