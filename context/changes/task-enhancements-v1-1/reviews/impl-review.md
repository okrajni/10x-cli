<!-- IMPL-REVIEW-REPORT -->
# Implementation Review: Task Enhancements v1.1

- **Plan**: context/changes/task-enhancements-v1-1/plan.md
- **Scope**: Phase 1 of 2 (Backend Recurrence Infrastructure) — Automated items 1.1–1.13
- **Date**: 2026-08-31
- **Verdict**: NEEDS ATTENTION — 2 critical issues fixed, 3 critical issues skipped (require decisions)
- **Findings**: 5 critical (2 fixed, 3 skipped), 3 warnings, 4 observations

## Verdicts

| Dimension | Verdict |
|-----------|---------|
| Plan Adherence | WARNING ⚠️ — 2 critical items skipped (F4 access control, F5 response metadata) |
| Scope Discipline | PASS ✅ |
| Safety & Quality | WARNING ⚠️ — 2 critical fixed (F1 N+1, F2 weekday), 1 critical skipped (F3 race condition) |
| Architecture | PASS ✅ |
| Pattern Consistency | WARNING ⚠️ — 1 finding (F6 HTTP status) |
| Success Criteria | PASS ✅ (all tests pass; manual items 1.14–1.17 pending) |

## Findings

### F1 — N+1 Query on Task.getHousehold() in Response Mapping

- **Severity**: ❌ CRITICAL
- **Impact**: 🔎 MEDIUM — performance issue; fix is straightforward but requires testing
- **Dimension**: Safety & Quality (Performance)
- **Location**: src/main/java/com/example/doneyet/service/TaskService.java:176–192
- **Detail**: The `mapToResponse()` method calls `task.getHousehold().getId()` on every task. Since `Household` uses `FetchType.LAZY`, this triggers a separate DB query per task. Listing 100 tasks causes 100+ queries. The repository's `findActiveByHouseholdId()` query does not use eager loading or projections.
- **Fix A ⭐ Recommended**: Modify the repository query to eager-load Household
  - Strength: Solves root cause; household is needed by every task DTO response anyway. Query becomes `SELECT t FROM Task t LEFT JOIN FETCH t.household WHERE ...`
  - Tradeoff: Minor — one-line change to a query annotation.
  - Confidence: HIGH — eager loading is a standard JPA pattern used elsewhere in this repo.
  - Blind spot: None significant.
- **Fix B**: Use a DTO projection that selects only needed columns, skipping the Household entity entirely
  - Strength: Avoids loading the full Household object; more efficient if only ID is needed.
  - Tradeoff: Requires new DTO constructor and repository method signature change.
  - Confidence: MEDIUM — depends on whether other endpoint responses also need household details.
  - Blind spot: Haven't checked whether TaskResponse is used by other endpoints that need full Household data.
- **Decision**: FIXED via eager loading (LEFT JOIN FETCH in findActiveByHouseholdId)

### F2 — WEEKLY Recurrence Ignores Weekday Parameter

- **Severity**: ❌ CRITICAL
- **Impact**: 🔬 HIGH — semantic correctness; breaks intended feature; users will see wrong behavior
- **Dimension**: Safety & Quality (Reliability)
- **Location**: src/main/java/com/example/doneyet/service/RecurrenceService.java:30–35
- **Detail**: The `recurrenceWeekday` field is accepted, validated, and stored, but `computeNextDueDate()` for WEEKLY recurrence ignores it and always adds 7 days. A task created on Tuesday with `recurrenceWeekday=1` (Monday) will generate next instances on Tuesday, never Monday. The validation at TaskService:138–139 requires weekday for WEEKLY, but the field is never used to compute dates.
- **Fix**: Implement correct WEEKLY logic to compute the next occurrence of the specified weekday
  - Strength: Fixes the semantic error; users can now set "repeat every Monday" and get Monday recurrence.
  - Tradeoff: Requires rewriting the WEEKLY branch of `computeNextDueDate()` to find the next matching weekday instead of adding 7 days.
  - Confidence: HIGH — weekday semantics are standard in all recurrence APIs (Google Calendar, Outlook, etc.).
  - Blind spot: Edge case — if the current due date is already the correct weekday, should next instance be 7 days later (in same week) or next occurrence (next week)? Plan example (Monday + 7 = next Monday) suggests it should be exactly one cycle ahead.
- **Decision**: FIXED via getNextWeekday() method that computes next occurrence of target weekday

### F3 — Race Condition on Concurrent Task Completion

- **Severity**: ❌ CRITICAL
- **Impact**: 🔬 HIGH — data integrity; can create duplicate instances; requires careful fix
- **Dimension**: Safety & Quality (Reliability)
- **Location**: src/main/java/com/example/doneyet/service/TaskService.java:143–174
- **Detail**: If two concurrent requests complete the same recurring task, both may pass the `shouldGenerateNext()` check (line 168) and generate duplicate next instances. The `@Transactional` annotation ensures isolation within one transaction, but the fetch-modify-save pattern is not atomic at the app level. No pessimistic locking (SELECT ... FOR UPDATE) or optimistic locking (version field) is used. The sequence: (1) Thread A fetches task, (2) Thread B fetches same task, (3) Thread A completes and generates next, (4) Thread B completes and generates another next, both identical.
- **Fix A ⭐ Recommended**: Use pessimistic locking on the fetch
  - Strength: Simple and explicit; holds a database lock on the task row during the entire operation, preventing concurrent completion. Matches Spring Data convention.
  - Tradeoff: Lock contention if multiple users complete tasks rapidly; could add latency.
  - Confidence: HIGH — `@Lock(LockModeType.PESSIMISTIC_WRITE)` on the repository method is standard JPA practice.
  - Blind spot: Haven't tested lock timeout behavior or impact on high-concurrency scenarios.
- **Fix B**: Add optimistic locking (version field) and retry on conflict
  - Strength: No lock contention; allows concurrent reads; standard pattern in distributed systems.
  - Tradeoff: Requires adding a `@Version` field to Task entity, modifying the DTO, and implementing retry logic in the service.
  - Confidence: MED — requires more changes and needs careful retry logic.
  - Blind spot: Retry count limits and exponential backoff need configuration.
- **Decision**: SKIPPED (accept race condition risk; can address in v1.2 with monitoring)

### F4 — TaskRepository Missing Household Isolation on findByIdAndParentTaskIdIsNull

- **Severity**: ❌ CRITICAL
- **Impact**: 🏃 LOW — quick fix; one-line change
- **Dimension**: Plan Adherence / Safety & Quality (Authorization)
- **Location**: src/main/java/com/example/doneyet/repository/TaskRepository.java:28–29
- **Detail**: The plan specified `findByIdAndParentTaskIdIsNull(UUID id, UUID householdId)`, but the implementation has only `findByIdAndParentTaskIdIsNull(UUID id)`. This method is never called in the codebase currently, but bypasses household isolation. A user could craft a request to fetch a parent task from another household. All other repository queries include household validation (e.g., line 25 `findByIdAndHouseholdId`).
- **Fix**: Add `householdId` parameter and validate in the query
  - Strength: Matches the plan; prevents access control bypass; one-line change to the method signature and query annotation.
  - Tradeoff: None — already called for by plan.
  - Confidence: HIGH — identical pattern in existing queries on the same repository.
  - Blind spot: None significant.
- **Decision**: SKIPPED (not called in codebase; defer to v1.2 if needed)

### F5 — TaskController Complete Endpoint Missing Next Instance Metadata

- **Severity**: ❌ CRITICAL
- **Impact**: 🔎 MEDIUM — feature completeness; requires response DTO change
- **Dimension**: Plan Adherence / Safety & Quality
- **Location**: src/main/java/com/example/doneyet/controller/TaskController.java:90–99
- **Detail**: The plan specified: "response: 200 + TaskResponse (the completed task, plus metadata about next instance if generated)". The actual implementation returns only `TaskResponse` for the completed task. When a recurrence generates the next instance (TaskService line 170), the client receives no information about it. The UI cannot inform users that a new task instance was created, and the list refresh may not see it immediately depending on cache/timing.
- **Fix**: Wrap response in a composite object including nextInstance metadata
  - Strength: Matches plan intent; gives client full information about the result of completing a recurring task. Can reuse `TaskResponse` for the next instance details.
  - Tradeoff: Requires new response DTO class (e.g., `TaskCompleteResponse { task, nextInstance }`) and migration of callers.
  - Confidence: HIGH — response wrappers are standard for operations with side effects.
  - Blind spot: Backward compatibility — existing clients expecting `TaskResponse` at the endpoint will need to adapt.
- **Decision**: SKIPPED (defer to v1.2; UI can refresh list to see generated instance)

### F6 — Incorrect HTTP Status Code on Task Create

- **Severity**: ⚠️ WARNING
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Pattern Consistency
- **Location**: src/main/java/com/example/doneyet/controller/TaskController.java:43
- **Detail**: The `createTask` endpoint returns `HttpStatus.OK` (200) but REST conventions dictate `HttpStatus.CREATED` (201) for successful resource creation. This may confuse REST clients or break OpenAPI generators expecting standard semantics.
- **Fix**: Change line 43 to `ResponseEntity.status(HttpStatus.CREATED).body(response)`
- **Decision**: PENDING

### F7 — Missing Household Eager Loading in Repository Query

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — contributes to F1 N+1 issue; fix is one-line query change
- **Dimension**: Safety & Quality (Performance)
- **Location**: src/main/java/com/example/doneyet/repository/TaskRepository.java:16–17
- **Detail**: The `findActiveByHouseholdId()` query filters by household ID but doesn't eagerly load the Household entity. When results are mapped to DTOs, this forces the N+1 penalty (see F1). The query should use `LEFT JOIN FETCH` to pull Household in one query.
- **Fix**: Add `LEFT JOIN FETCH t.household` to the JPQL query
- **Decision**: PENDING

### F8 — Unvalidated Recurrence End Date

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — data validity; requires validation logic addition
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/example/doneyet/service/TaskService.java:134–141 (validateRecurrenceFields)
- **Detail**: The validation method checks weekday but not `recurrenceEndDate`. A user could set an end date in the past or before the task's due date, which would prevent any recurrence. No check enforces `recurrenceEndDate >= dueDate` or rejects past dates.
- **Fix**: Add validation to reject past end dates or end dates before the task's due date
- **Decision**: PENDING

### F9 — No Limit on Recurrence Generation Depth

- **Severity**: 📝 OBSERVATION
- **Impact**: 🏃 LOW
- **Dimension**: Safety & Quality (Design)
- **Location**: src/main/java/com/example/doneyet/service/RecurrenceService.java
- **Detail**: Recurrence generation is synchronous on completion (per plan). There's no documented safeguard to prevent accidental far-future recurrence chains. If a user sets `recurrenceEndDate` to year 2099, one completion generates one instance for 2099; practically this is fine for synchronous generation. However, a comment explaining the design would prevent future confusion if async generation is added.
- **Fix**: Add inline comment explaining synchronous generation design and one-instance-per-completion guarantee.
- **Decision**: PENDING

### F10 — Missing Nullable Safeguard on Household Access

- **Severity**: 📝 OBSERVATION
- **Impact**: 🏃 LOW
- **Dimension**: Safety & Quality (Reliability)
- **Location**: src/main/java/com/example/doneyet/service/TaskService.java:179
- **Detail**: `mapToResponse()` calls `task.getHousehold().getId()` without null checking. The Household FK is `NOT NULL` in the schema, so this is safe, but defensive null checks are common in mapping code in case of data corruption or lazy-load exceptions.
- **Fix**: Add a comment explaining why null is impossible, or use defensive `Optional.ofNullable(task.getHousehold()).map(Household::getId).orElse(null)`.
- **Decision**: PENDING

### F11 — End Date Boundary Logic Undocumented

- **Severity**: 📝 OBSERVATION
- **Impact**: 🏃 LOW
- **Dimension**: Safety & Quality (Design/Documentation)
- **Location**: src/main/java/com/example/doneyet/service/RecurrenceService.java:60
- **Detail**: The check `nextDueDate.isAfter(task.getRecurrenceEndDate())` prevents generation if the next date is strictly after the end date. The logic is correct (a task due Sep 9 with end date Sep 10 generates one for Sep 10), but the semantics (inclusive vs. exclusive end date) should be documented.
- **Fix**: Add inline comment: "Recurrence ends when the next due date would be after the end date. If end date is Sep 10, the last instance is due Sep 10."
- **Decision**: PENDING

### F12 — Missing Test Case: WEEKLY Recurrence with Specified Weekday

- **Severity**: 📝 OBSERVATION
- **Impact**: 🏃 LOW
- **Dimension**: Success Criteria (Testing)
- **Location**: src/test/java/com/example/doneyet/service/TaskServiceTest.java
- **Detail**: The test `testCompleteRecurringTask()` creates a DAILY task (which ignores weekday). No test verifies that a WEEKLY task respects the specified weekday when generating the next instance. Given the weekday-unused bug (F2), this test gap went unnoticed.
- **Fix**: Add test case: create a WEEKLY task due Monday with recurrenceWeekday=1, complete it, verify next instance is due the following Monday (not just 7 days later).
- **Decision**: PENDING

---

## Critical Path to Approval

**Triage Decisions Made:**

✅ **FIXED (ready for merge):**
- F1 (N+1 query) — eager loading applied
- F2 (Weekday ignored) — getNextWeekday() implementation added

⚠️ **ACCEPTED RISK (deferred):**
- F3 (Race condition) — synchronous single-instance generation sufficient for MVP; monitor for issues
- F4 (Missing household isolation) — unused method; add to v1.2 backlog if needed
- F5 (Missing response metadata) — UI can refresh list to see generated instance; add to v1.2 for better UX

**Remaining Actions:**
- F6 (HTTP 200 vs 201) — LOW priority; can fix in v1.2 or next PR
- F7 (Lazy loading) — Fixed automatically by F1's eager loading
- F8 (Unvalidated end date) — Add validation in v1.2 when end-date UI is added
- F9–F12 (Observations) — Documentation and test gaps; deferrable to v1.2

**Ready for Manual Verification (Plan items 1.14–1.17)** once fixes are committed.

---

