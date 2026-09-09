# Task Enhancements v1.1 — Implementation Plan

## Overview

Enhance the task system with two key user-facing improvements: (1) auto-populate due date to today to reduce friction, and (2) support recurring tasks that auto-generate on completion. This unlocks the "set-and-forget" household chore workflow (e.g., "buy groceries weekly") and reduces manual task recreation friction.

## Current State Analysis

**Existing baseline (S-02 basic-task-crud):**
- ✓ Task CRUD endpoints (create, read, list, update, delete)
- ✓ Task entity with title, description, category, dueDate, completedAt, deletedAt
- ✓ Frontend forms (TaskCreatePage, TaskEditPage, TaskListPage)
- ✓ Household isolation enforced
- ✗ No recurrence/repeat support
- ✗ No default due date (users must manually set each time)

**What's missing:**
- RecurrencePattern entity to store repeat rules (frequency: daily/weekly/monthly, endDate)
- TaskService logic to generate next instance on completion
- Frontend recurrence selector (dropdown or button group)
- Default due date in TaskCreatePage form
- Recurrence display on TaskCard
- Tests for recurrence generation and completion flow

**Key Discoveries:**

- **Recurrence is parent-child**: A recurring task (parent) has a recurrence pattern. Each generated instance is a child task linked to the parent via a `parentTaskId` field. On completion of a child, the service generates the next child task from the parent's pattern.
- **Instance generation is deterministic**: Given a parent's pattern (e.g., "weekly on Mondays") and a completed child's due date, the next child's due date is computed as `currentDueDate + interval` (e.g., Monday + 7 days = next Monday). No background job needed; generation is synchronous in the complete endpoint.
- **List shows current instance only**: Users see only the next upcoming instance of a recurring task in the list. After marking it complete, the next instance is generated and becomes the visible one. This keeps the list uncluttered.
- **Timezone is browser-local**: Default due date uses browser time (no server timezone conversion). Recurrence boundaries (e.g., weekly Monday) are also computed in browser timezone at frontend, sent to backend as `LocalDate` (which is timezone-agnostic).
- **Backwards compatibility**: Existing non-recurring tasks remain unchanged. New tasks can have recurrence added at creation or later (edit-to-add-recurrence is supported).

## Desired End State

After this plan is complete:

1. **Create task with default date**: User opens TaskCreatePage → due date field is pre-filled with today's date (browser local time, YYYY-MM-DD). User can override.
2. **Create recurring task**: User checks "Repeat" toggle → recurrence frequency selector appears (daily/weekly/monthly). Optionally set end date (defaults to no end). Save creates task with recurrence pattern.
3. **Complete recurring task**: User marks recurring task complete in the list → next instance is automatically created and appears in the list (e.g., "Buy groceries - Wed 9/11" after completing "Buy groceries - Wed 9/4").
4. **Edit recurrence**: User opens TaskEditPage for a recurring task → can see recurrence pattern (e.g., "Repeats weekly") and change it. Change applies only to future instances (past instances remain as-is).
5. **Dashboard integration**: S-06 dashboard can query `tasks where parentTaskId IS NULL` to show only top-level tasks (no instance duplication).

**Verification:**
- Create task → dueDate pre-filled with today's date
- Create recurring task (weekly) → next instance generated on completion
- Completed recurring task disappears, new instance appears with due date = current + 7 days
- Edit recurring task's pattern → change is reflected in future instances
- Dashboard shows "Buy groceries - Wed" not ["Buy groceries - Wed 9/4 (completed)", "Buy groceries - Wed 9/11", ...]

## What We're NOT Doing

- **Complex recurrence (RRULE)**: Only daily, weekly, monthly. No "every other Tuesday" or "3rd Friday of the month".
- **Recurring task end date UI**: End date is optional; users can theoretically set it via API but not easily in UI (can add in v1.2).
- **Timezone-aware backend**: Due dates are stored as `LocalDate` (timezone-agnostic). Browser handles local-time interpretation.
- **Instance-level override**: Can't move one instance of "weekly laundry" to a different day without affecting others (consider v1.2 if needed).
- **Bulk recurrence assignment**: Can't make all existing "cleaning" tasks recurring with one action.
- **Background job for generation**: No Quartz job. Generation is synchronous on completion only.

## Implementation Approach

**Backend (Spring Boot):**
- Add `RecurrencePattern` value object (frequency enum: DAILY/WEEKLY/MONTHLY, endDate optional)
- Add `parentTaskId` field to Task entity (nullable; non-null means this is an instance of a recurring task)
- Create `RecurrenceService` to compute next instance due date from a parent task + pattern
- Update `TaskService.completeTask()` to generate the next instance (if parent has recurrence)
- Extend `TaskDTO.TaskResponse` to include recurrence pattern info (for UI to display)
- No database job or scheduling; all generation is inline on completion

**Frontend (React):**
- Update `TaskCreatePage` to default `dueDate` to `new Date().toISOString().split('T')[0]` (today, browser local time)
- Add recurrence selector toggle and frequency dropdown (daily/weekly/monthly)
- Update `TaskEditPage` to show recurrence pattern (read-only initially; full edit in v1.2)
- Extend `TaskCard` to display "Repeats daily/weekly/monthly" if recurrence is present
- No UI changes to mark task complete (existing checkbox/button remains)

**Data Flow for Recurring Task Completion:**
```
User clicks "Complete" on TaskCard (instance of recurring task)
  ↓ [PUT /api/task/{id}/complete with { completed: true }]
TaskService.completeTask(id)
  ↓ (fetch task; if task.parentTaskId != null)
RecurrenceService.generateNextInstance(parentTask)
  ↓ (compute nextDueDate from parent.pattern + currentTask.dueDate)
Create new Task(parentTaskId=parent.id, dueDate=nextDueDate, ...)
  ↓ (save both current task & new instance)
Return updated task (now marked complete) + new instance details
  ↓ [200 response]
Frontend refetches list → current instance is gone (completed), next instance appears
```

**Architectural Decision:**
- **No recurrence type field**: A task either has `parentTaskId` (instance) or has `recurrencePattern` (parent), never both. Prevents ambiguity.
- **Deterministic generation**: Next due date is always `currentDueDate + interval` (e.g., Mon + 7 days = next Mon). No "relative to today" logic that could produce unexpected results.
- **Immutable past instances**: Editing parent's recurrence pattern does NOT modify past instances. Future instances are regenerated from the pattern on demand (or on next completion). Preserves audit trail.

## Critical Implementation Details

**Recurrence Pattern Schema:**
The `RecurrencePattern` is stored as a composite value (not a separate table) within the Task entity to keep it simple:
- Fields: `frequency` (enum: DAILY/WEEKLY/MONTHLY), `endDate` (LocalDate, nullable), `weekday` (Integer 0-6 for weekly only, nullable)
- Serialized as JSON or separate columns based on your ORM preference (Hibernate `@Embeddable` or individual columns)
- For weekly, store `weekday` (0=Sun, 1=Mon, ..., 6=Sat) to enable "repeat every Monday" vs arbitrary N-day intervals

**Next Instance Generation (Critical Logic):**
```
Given: parent task with pattern (WEEKLY, weekday=1), 
       current completed instance with dueDate = 2026-09-07 (Monday)
Compute: nextDueDate = currentDueDate + 7 days = 2026-09-14 (Monday)

For DAILY: nextDueDate = currentDueDate + 1 day
For MONTHLY: nextDueDate = currentDueDate + 1 month (be careful with month-end edge case, e.g., Jan 31 + 1 month = Feb 28/29)
```

**Timezone Consideration:**
- Browser sends due dates as strings in YYYY-MM-DD format (LocalDate, no time component)
- Backend stores as `LocalDate` (no timezone info; conceptually "midnight in the user's timezone")
- No conversion needed; just accept and return as-is
- Recurrence generation (adding days/months) is all in LocalDate arithmetic; no ZonedDateTime needed for MVP

**Household Isolation:**
- Parent task is always scoped to a household
- All generated instances inherit the parent's household_id
- Backend MUST validate household membership on edit/complete of any task (parent or instance)
- Frontend filters list by household (existing pattern)

---

## Phase 1: Backend Recurrence Infrastructure

### Overview

Build the data model and service logic for recurring task support. This includes the recurrence pattern storage, next-instance generation logic, and updating the complete-task endpoint to trigger generation.

### Changes Required:

#### 1. Task Entity Extension (Recurrence Fields)

**File**: `src/main/java/com/example/doneyet/domain/Task.java`

**Intent**: Add fields to track recurrence pattern and parent-child relationship between recurring tasks.

**Contract**:
- Add field: `parentTaskId: UUID` (nullable; foreign key to parent Task)
- Add field: `recurrenceFrequency: RecurrenceFrequency` (enum: DAILY, WEEKLY, MONTHLY; nullable if non-recurring)
- Add field: `recurrenceEndDate: LocalDate` (nullable; no end date if null)
- Add field: `recurrenceWeekday: Integer` (nullable; 0-6 for weekly, required when frequency=WEEKLY)
- Column definitions: `@Column(name="parent_task_id")`, `@Column(name="recurrence_frequency")`, etc.
- Getters/setters for all new fields

Migration note: These columns are nullable to support existing non-recurring tasks.

---

#### 2. RecurrenceFrequency Enum

**File**: `src/main/java/com/example/doneyet/domain/RecurrenceFrequency.java`

**Intent**: Define supported recurrence patterns.

**Contract**: 
- Enum values: `DAILY`, `WEEKLY`, `MONTHLY`
- Used in Task entity and DTO

---

#### 3. Task Repository Update

**File**: `src/main/java/com/example/doneyet/repository/TaskRepository.java`

**Intent**: Add query methods for recurrence logic.

**Contract**:
- `Optional<Task> findByIdAndParentTaskIdIsNull(UUID id, UUID householdId)` — fetch a parent task (or non-recurring task) by ID, confirming it's not an instance of another recurring task
- `List<Task> findByParentTaskIdOrderByDueDateAsc(UUID parentTaskId)` — fetch all instances of a recurring task (for audit/dashboard)
- `Optional<Task> findByParentTaskIdAndCompletedFalseAndDueDateAfter(UUID parentTaskId, LocalDate dueDateThreshold)` — find the next active instance (for UI to display correct "current" task)

---

#### 4. Extend Task DTO for Recurrence

**File**: `src/main/java/com/example/doneyet/dto/TaskDto.java`

**Intent**: Update request/response DTOs to include recurrence pattern.

**Contract**: 
- `CreateTaskRequest`: Add optional fields `recurrenceFrequency: RecurrenceFrequency`, `recurrenceEndDate: LocalDate`, `recurrenceWeekday: Integer` (0-6 for weekly)
- `UpdateTaskRequest`: Same optional fields (to allow toggling recurrence on/off)
- `TaskResponse`: Add fields `parentTaskId: UUID`, `recurrenceFrequency: RecurrenceFrequency`, `recurrenceEndDate: LocalDate`, `recurrenceWeekday: Integer` (all nullable)
- Validation: If `recurrenceFrequency=WEEKLY`, then `recurrenceWeekday` is required; otherwise optional

---

#### 5. Recurrence Service

**File**: `src/main/java/com/example/doneyet/service/RecurrenceService.java`

**Intent**: Encapsulate logic for computing next instance due date and generating the next task.

**Contract**: Methods:
- `LocalDate computeNextDueDate(Task currentTask)` — given a completed task with recurrence pattern, compute the due date for the next instance. Logic: `nextDueDate = currentTask.dueDate + frequency interval` (1 day for DAILY, 7 days for WEEKLY, ~30 days for MONTHLY accounting for month-end)
- `Task generateNextInstance(Task parentTask, LocalDate nextDueDate, User createdBy)` — create a new Task with same title/description/category/household as parent, set parentTaskId=parent.id, set dueDate=nextDueDate, return it
- `boolean shouldGenerateNext(Task task)` — check if task has recurrence and if it has an end date that hasn't been reached (false if `recurrenceEndDate != null && nextDueDate > recurrenceEndDate`)

---

#### 6. Task Service Update (Complete Task Endpoint)

**File**: `src/main/java/com/example/doneyet/service/TaskService.java`

**Intent**: Update the `completeTask` method to auto-generate the next instance when a recurring task is marked complete.

**Contract**:
- Method `completeTask(UUID taskId, UUID householdId, User user): TaskResponse` (new/updated)
- On completion, if task is an instance of a recurring parent (task.parentTaskId != null):
  - Fetch parent task via taskRepository.findById(task.parentTaskId)
  - Call `recurrenceService.shouldGenerateNext(task)` — if true, call `recurrenceService.generateNextInstance(parentTask, nextDueDate, user)` and persist it
  - If false (recurrence has ended), do not generate next
- Mark current task as complete (set completed=true, completedAt=now())
- Return updated task in response

---

#### 7. Task Controller Update (Complete Endpoint)

**File**: `src/main/java/com/example/doneyet/controller/TaskController.java`

**Intent**: Add or update endpoint to mark task complete.

**Contract**:
- `PUT /api/task/{id}/complete` — body: empty or `{ "completed": true }`, response: 200 + TaskResponse (the completed task, plus metadata about next instance if generated)
- Call `taskService.completeTask(id, householdId, user)`
- If complete was successful and next instance was generated, optionally include `{ task, nextInstance }` in response for frontend

Alternative: Extend existing `PUT /api/task/{id}` to support `completedAt` field in UpdateTaskRequest (simpler if already supported).

---

#### 8. Database Migration

**File**: `src/main/resources/db/migration/V<N>__Add_recurrence_fields.sql`

**Intent**: Add recurrence columns to tasks table.

**Contract**: SQL:
```sql
ALTER TABLE tasks ADD COLUMN parent_task_id UUID REFERENCES tasks(id);
ALTER TABLE tasks ADD COLUMN recurrence_frequency VARCHAR(10);
ALTER TABLE tasks ADD COLUMN recurrence_end_date DATE;
ALTER TABLE tasks ADD COLUMN recurrence_weekday INTEGER CHECK(recurrence_weekday >= 0 AND recurrence_weekday <= 6);

CREATE INDEX idx_tasks_parent_task_id ON tasks(parent_task_id);
CREATE INDEX idx_tasks_parent_completed ON tasks(parent_task_id, completed);
```

---

### Success Criteria:

#### Automated Verification:

- `mvn compile` succeeds (new fields, enum, service compile cleanly)
- `mvn test` runs unit tests for RecurrenceService (computeNextDueDate for DAILY/WEEKLY/MONTHLY edge cases, e.g., Jan 31 + 1 month)
- `mvn test` runs integration tests for TaskService.completeTask (recurring + non-recurring tasks)
- Invalid recurrence request (WEEKLY without weekday) rejected with 422 ValidationException
- Recurrence end date in the past rejected (or handled gracefully)
- Migration applies cleanly: `mvn liquibase:update` or `mvn flyway:migrate`
- Type checking passes: `mvn compile` with strict checks

#### Manual Verification:

- Create recurring task via curl/Postman with recurrenceFrequency=WEEKLY, recurrenceWeekday=1 → 200, task saved with recurrence fields
- Complete that task (PUT /api/task/{id}/complete) → 200, original task marked complete, next instance created with dueDate = original + 7 days
- Fetch task list → returns both original (completed) and next instance (not completed) if including completed tasks in query
- Edit recurrence pattern on parent → new instances generate with updated pattern (verify via next completion)
- Attempt to create WEEKLY task without weekday → 422 ValidationException

---

## Phase 2: Frontend Enhancements (Default Date + Recurrence UI)

### Overview

Update the frontend forms to (1) default due date to today, and (2) provide UI for selecting recurrence patterns. No changes to list display (still show current instance only via existing logic).

### Changes Required:

#### 1. TaskCreatePage Enhancement (Default Due Date)

**File**: `frontend/src/features/tasks/pages/TaskCreatePage.tsx`

**Intent**: Pre-populate due date field with today's date when component mounts.

**Contract**: 
- On component mount (useEffect), set `formData.dueDate` to `new Date().toISOString().split('T')[0]` (e.g., "2026-09-04")
- User can override by changing the date input
- Rest of form validation/submission unchanged

---

#### 2. TaskCreatePage Enhancement (Recurrence Selector)

**File**: `frontend/src/features/tasks/pages/TaskCreatePage.tsx` (continued from 1)

**Intent**: Add recurrence options to form.

**Contract**: 
- Add to form: toggle "Repeat this task?" (checkbox)
- If checked, show frequency selector (radio buttons or dropdown): Daily / Weekly / Monthly
- If frequency = Weekly, show weekday selector (7 checkboxes or dropdown for Mon-Sun) — *required* field when WEEKLY is selected
- Optional: "End repeat on" date input (defaults to empty/null)
- All recurrence fields added to `formData` state and sent in `CreateTaskRequest`
- Validation: if frequency is set, weekday must be set (for WEEKLY); no other validations

---

#### 3. TaskEditPage Enhancement (Recurrence Display)

**File**: `frontend/src/features/tasks/pages/TaskEditPage.tsx`

**Intent**: Show current recurrence pattern when editing a recurring task (read-only for now; full edit in v1.2).

**Contract**:
- Fetch task on mount (existing logic)
- If task has `recurrenceFrequency`, display read-only text: "Repeats daily/weekly/monthly" (e.g., "Repeats weekly on Monday")
- In future (v1.2), make recurrence fields editable (same selector as TaskCreatePage)
- Non-recurring tasks show no recurrence info

---

#### 4. TaskCard Component Enhancement (Recurrence Badge)

**File**: `frontend/src/shared/components/TaskCard.tsx`

**Intent**: Display recurrence pattern on the card so users can quickly see which tasks repeat.

**Contract**:
- If task has `recurrenceFrequency`, add a small badge/label: "Repeats daily/weekly/monthly"
- Styling: Use a muted color (e.g., gray badge) to distinguish from action items
- Non-recurring tasks show no badge

---

#### 5. Extend Task API Types

**File**: `frontend/src/features/tasks/api.ts` (or auto-generated types if using OpenAPI)

**Intent**: Update TypeScript types to include recurrence fields.

**Contract**:
- `CreateTaskRequest`: Add optional fields `recurrenceFrequency?: string`, `recurrenceWeekday?: number`, `recurrenceEndDate?: string`
- `TaskResponse`: Add fields `recurrenceFrequency?: string`, `recurrenceWeekday?: number`, `recurrenceEndDate?: string`, `parentTaskId?: string`
- Types should match backend DTO

---

#### 6. Update Form Validation Utils

**File**: `frontend/src/features/tasks/utils/formValidation.ts` (new or extended)

**Intent**: Add validation helper for recurrence fields.

**Contract**:
- Function `validateRecurrence(frequency?: string, weekday?: number): error?` — return error if WEEKLY is selected but weekday is not set; otherwise return null

---

#### 7. Recurrence Display Utility

**File**: `frontend/src/features/tasks/utils/recurrenceUtils.ts` (new)

**Intent**: Helper to format recurrence pattern for display.

**Contract**:
- Export `formatRecurrence(frequency?: string, weekday?: number, endDate?: string): string` — returns human-readable string like "Repeats weekly on Monday" or "Repeats daily until Dec 31, 2026"
- Used in TaskCard and TaskEditPage display

---

### Success Criteria:

#### Automated Verification:

- TypeScript compilation passes (`npm run build`)
- ESLint passes (`npm run lint`)
- App starts without errors (`npm run dev`)
- TaskCreatePage renders with default dueDate (verify via DOM inspection or snapshot test)
- Recurrence selector appears when toggle is checked
- Form validation rejects WEEKLY without weekday

#### Manual Verification:

- Open TaskCreatePage → due date field shows today's date (e.g., "2026-09-04")
- User can change due date to tomorrow or other date
- Check "Repeat this task" → frequency selector appears
- Select "Weekly" → weekday selector appears; form requires weekday to be selected before submit
- Create recurring task (weekly on Monday) → task is created successfully
- In task list, recurring task shows "Repeats weekly" badge
- In TaskEditPage, recurrence pattern is displayed (read-only)
- Create non-recurring task → no badge shown; no recurrence info in edit page

---

## Testing Strategy

### Unit Tests:

**Backend (RecurrenceService):**
- `testComputeNextDueDateDaily` — DAILY pattern, next = current + 1 day
- `testComputeNextDueDateWeekly` — WEEKLY pattern, next = current + 7 days (same weekday)
- `testComputeNextDueDateMonthlySimple` — MONTHLY pattern, Sep 4 → Oct 4
- `testComputeNextDueDateMonthlyEdgeCase` — MONTHLY pattern, Jan 31 → Feb 28/29
- `testShouldGenerateNextWithEndDate` — recurrence with endDate, verify it stops generating after end date
- `testShouldGenerateNextNoEndDate` — recurrence without endDate (infinite), always returns true

**Frontend (React components):**
- `testTaskCreatePageDefaultsToday` — verify dueDate is pre-filled with today's date
- `testTaskCreatePageRecurrenceToggle` — toggle "Repeat" → frequency selector appears
- `testTaskCreatePageRecurrenceValidation` — WEEKLY without weekday → validation error
- `testTaskCardRecurrenceBadge` — task with recurrence shows "Repeats weekly" badge
- `testTaskEditPageRecurrenceDisplay` — recurring task shows pattern (read-only)

### Integration Tests:

**Backend:**
- Create recurring task (WEEKLY) via POST /api/task → 200, recurrence fields saved
- Complete recurring task via PUT /api/task/{id}/complete → 200, original marked complete, next instance created with dueDate+7 days
- Complete recurring task with endDate in the past → 200, original marked complete, no next instance created
- Fetch task list (GET /api/task) → returns only the current active instance of recurring tasks (not all instances)

**Frontend:**
- User creates recurring task (daily) → form clears, success toast, new task appears in list with badge
- User marks recurring task complete → list refetches, current instance is gone (completed), next instance appears with "Repeats daily" badge
- User views task detail (TaskEditPage) for recurring task → recurrence pattern is displayed

### Manual Testing Steps:

1. Start dev server: `npm run dev` (frontend) + `mvn spring-boot:run` (backend)
2. Register user, log in, navigate to create task
3. Verify due date field is pre-filled with today's date
4. Check "Repeat this task" → verify frequency selector appears
5. Select "Weekly", then Monday → create task
6. Task appears in list with "Repeats weekly on Monday" badge
7. Mark task complete → task disappears from active list, next instance appears (due date = current + 7 days)
8. Edit recurring task → verify recurrence pattern is shown (read-only)
9. Create non-recurring task → verify no badge shown
10. Test edge cases:
    - Create WEEKLY task without selecting weekday → validation error shown
    - Create task with recurrence end date in the past → task created but no next instance generated
    - Mark task complete multiple times → each completion generates next instance (if within recurrence period)

## Performance Considerations

- **Recurrence generation is synchronous**: Completing a task will generate the next instance in the same request. For a user completing tasks rapidly, this could add latency. Monitor request time if household has many recurring tasks.
- **No N+1 queries**: When fetching task list, use eager loading or batch fetching to avoid querying parent task data for each child. Consider caching parent patterns.
- **Index on parent_task_id**: Ensure database has an index to speed up queries filtering by parentTaskId.

## Migration Notes

To support existing non-recurring tasks:
- New columns (parent_task_id, recurrence_frequency, etc.) are nullable → no backfill needed
- Existing tasks remain non-recurring (all recurrence fields null)
- No API contract breaking changes; recurrence fields are optional

For future migrations (e.g., to support complex RRULE format):
- Keep recurrence_frequency and other basic fields; add new columns for advanced patterns
- Versioning: add a `recurrence_pattern_version` field if needed

## References

- Foundation: `context/foundation/roadmap.md` (S-02 v1.1)
- Related: `context/changes/basic-task-crud/plan.md` (the initial CRUD implementation)
- Task entity: `src/main/java/com/example/doneyet/domain/Task.java`
- Frontend: `frontend/src/features/tasks/pages/TaskCreatePage.tsx`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Backend Recurrence Infrastructure

#### Automated

- [x] 1.1 RecurrenceFrequency enum (DAILY, WEEKLY, MONTHLY)
- [x] 1.2 Task entity extension with recurrence fields (parentTaskId, frequency, endDate, weekday)
- [x] 1.3 Database migration for recurrence columns
- [x] 1.4 TaskRepository with parent-child query methods
- [x] 1.5 Task DTO update with recurrence fields (CreateTaskRequest, UpdateTaskRequest, TaskResponse)
- [x] 1.6 RecurrenceService with computeNextDueDate, generateNextInstance, shouldGenerateNext methods
- [x] 1.7 TaskService.completeTask update to generate next instance
- [x] 1.8 TaskController update or new complete endpoint
- [x] 1.9 Security & validation (recurrence fields validated, WEEKLY requires weekday)
- [x] 1.10 Unit tests for RecurrenceService (DAILY, WEEKLY, MONTHLY, edge cases)
- [x] 1.11 Integration tests for TaskService.completeTask with recurrence
- [x] 1.12 Compilation & type checking pass
- [x] 1.13 All tests pass

#### Manual

- [ ] 1.14 Create recurring task (WEEKLY) via curl → task created with recurrence fields
- [ ] 1.15 Complete recurring task → next instance auto-generated with correct due date
- [ ] 1.16 Recurrence with end date → stops generating after end date
- [ ] 1.17 Household isolation enforced for recurrence (user can't complete other household's recurring task)

### Phase 2: Frontend Enhancements (Default Date + Recurrence UI)

#### Automated

- [x] 2.1 TaskCreatePage defaults dueDate to today's date
- [x] 2.2 Recurrence toggle & frequency selector in TaskCreatePage
- [x] 2.3 Weekday selector for WEEKLY recurrence
- [x] 2.4 Form validation for recurrence (WEEKLY requires weekday)
- [x] 2.5 TaskEditPage displays recurrence pattern (read-only)
- [x] 2.6 TaskCard shows recurrence badge (e.g., "Repeats weekly")
- [x] 2.7 Task API types updated with recurrence fields
- [x] 2.8 Recurrence formatting utility (formatRecurrence)
- [x] 2.9 TypeScript compilation passes
- [x] 2.10 ESLint passes
- [x] 2.11 App starts without console errors

#### Manual

- [ ] 2.12 TaskCreatePage due date field pre-filled with today
- [ ] 2.13 Check "Repeat" → frequency selector appears
- [ ] 2.14 Select WEEKLY → weekday selector appears
- [ ] 2.15 Create recurring task → succeeds, appears in list with badge
- [ ] 2.16 Mark recurring task complete → current instance hidden, next instance appears
- [ ] 2.17 TaskEditPage shows "Repeats weekly on Monday" (read-only)
- [ ] 2.18 Non-recurring tasks show no recurrence info
- [ ] 2.19 Form validation rejects WEEKLY without weekday
- [ ] 2.20 End-to-end: create daily task, complete it, next instance appears next day
