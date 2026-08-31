# Task Enhancements v1.1 — Plan Brief

> Full plan: `context/changes/task-enhancements-v1-1/plan.md`
> Related: `context/changes/basic-task-crud/plan.md` (S-02, the base implementation)

## What & Why

Add two QoL improvements to task management: (1) auto-populate due date to today (reduce friction for rapid task creation), and (2) support recurring tasks that auto-generate on completion (enable "set-and-forget" household chores like "weekly laundry").

The recurring task feature unblocks household workflows where users create the same task repeatedly each week/month. Instead of recreating manually, they set it once and it repeats automatically.

## Starting Point

- ✓ Full task CRUD exists (S-02 complete): create, read, edit, delete, mark complete
- ✓ Due date is already required but not defaulted
- ✗ No recurrence support (explicitly deferred from S-02)
- ✗ No default due date in form

## Desired End State

**User flows:**
1. Open task form → due date is pre-filled with today (can override)
2. Check "Repeat this task?" → select frequency (daily/weekly/monthly)
3. Create task → recurring pattern is saved
4. Complete task → next instance is automatically created and appears in the list
5. Edit recurring task → can see recurrence pattern (read-only for now)

**Verification:**
- Create weekly task (Monday) → mark complete → next instance appears with due date = current + 7 days
- Task list shows "Repeats weekly" badge on recurring tasks
- Backwards compatible: existing non-recurring tasks unchanged

## Key Decisions Made

| Decision                         | Choice                          | Why (1 sentence)             | Source |
|----------------------------------|---------------------------------|------------------------------|--------|
| Default due date location        | Frontend (JavaScript)           | Simplest; no backend change; browser local time. | Plan |
| Recurrence scope                 | Daily, weekly, monthly only     | Covers 80% of household chores; simple schema. | Plan |
| Recurrence end date              | Optional (infinite by default)   | Flexible for open-ended chores; users can set if needed. | Plan |
| Instance generation timing       | Synchronous on completion       | Guarantees next instance exists immediately; no background job. | Plan |
| List display                     | Show current instance only      | Uncluttered; next instance appears after completion. | Plan |
| Timezone handling                | Browser local time              | Simplest for users; no server timezone conversion. | Plan |
| Edit recurring task behavior     | Edit parent pattern only        | Simple schema; past instances immutable; future generated from pattern. | Plan |
| Instance link to parent          | `parentTaskId` field + recurrence pattern | Clean parent-child relationship; instances are independent once created. | Plan |

## Scope

**In scope:**
- Default due date to today in TaskCreatePage
- Recurrence selector (Daily / Weekly / Monthly)
- Optional end date for recurrence
- Weekday selector for Weekly recurrence
- Auto-generate next instance on task completion
- Display recurrence pattern on task cards and edit page
- Household isolation for recurrence (existing pattern extended)

**Out of scope:**
- Complex recurrence (RFC 5545 RRULE, "every other Tuesday")
- Recurring task end date UI (optional field exists in schema but not exposed in UI; can add in v1.2)
- Per-instance override (can't move one "Monday laundry" to Wednesday without affecting others; v1.2)
- Background job for generation (sync-only; suffices for MVP)
- Bulk recurrence assignment
- Timezone-aware backend (browser local time only)

## Architecture / Approach

**Data model:**
- Task entity gains 4 nullable fields: `parentTaskId`, `recurrenceFrequency`, `recurrenceEndDate`, `recurrenceWeekday`
- Non-recurring tasks: all recurrence fields null (backwards compatible)
- Recurring tasks: either a **parent** (has recurrence pattern) or an **instance** (has parentTaskId)
- No task is both; prevents ambiguity

**Service logic:**
- `RecurrenceService` computes next due date deterministically: `nextDueDate = currentDueDate + interval` (e.g., Mon + 7 = next Mon)
- `TaskService.completeTask()` calls RecurrenceService to generate next instance (if recurrence exists and end date not reached)
- No background job; generation is inline on completion

**Frontend:**
- TaskCreatePage: default `dueDate` to `new Date().toISOString().split('T')[0]` on mount
- Recurrence toggle + frequency selector (radio buttons or dropdown)
- TaskCard: show "Repeats daily/weekly/monthly" badge
- TaskEditPage: display recurrence pattern (read-only; full edit in v1.2)

## Phases at a Glance

| Phase           | What it delivers                                          | Key risk                              |
|-----------------|-----------------------------------------------------------|---------------------------------------|
| 1. Backend      | RecurrenceService, Task entity extension, DB migration, complete-task endpoint update | Month-end edge case (Jan 31 + 1 month); timezone confusion if not careful |
| 2. Frontend     | Default date, recurrence selector UI, recurrence badges   | Form state management for recurrence toggle + weekday selector |

**Prerequisites:** S-02 (basic-task-crud) complete.  
**Estimated effort:** ~2-3 sessions across 2 phases; can parallelize frontend once API contract is finalized.

## Open Risks & Assumptions

- **Synchronous generation adds latency**: Completing a task with recurrence generates the next instance in the same request. Could add seconds if household has many recurring tasks; monitor in beta.
- **Timezone edge cases**: Using browser local time means household members in different timezones might see different due dates. Acceptable for MVP (most households are local); revisit if needed.
- **End date is optional but powerful**: Users can set recurrence to end at a future date, but there's no UI for it yet (v1.2). API supports it; document for power users.
- **No per-instance override**: Users can't move one occurrence of "Monday laundry" to Tuesday without affecting the whole pattern. May need v1.2 enhancement if requested.

## Success Criteria (Summary)

- ✓ Due date field is pre-filled with today's date in TaskCreatePage
- ✓ User can create recurring task (daily/weekly/monthly) with optional end date
- ✓ Completing a recurring task auto-generates the next instance with correct due date
- ✓ Task list shows only current instance of recurring task (next appears after completion)
- ✓ TaskCard displays "Repeats X" badge for recurring tasks
- ✓ TaskEditPage shows recurrence pattern (read-only)
- ✓ All household isolation rules enforced for parent + instance tasks
- ✓ Backwards compatible: existing non-recurring tasks unchanged
