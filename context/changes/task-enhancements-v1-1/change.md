# Change: task-enhancements-v1-1

**Roadmap ID:** S-02 (v1.1)  
**Change ID:** `task-enhancements-v1-1`  
**Status:** impl_reviewed  
**Updated:** 2026-08-31  

## Identity

Enhance basic task CRUD with two key features: (1) auto-populate due date to today in the form, reducing friction for rapid task creation, and (2) support recurring tasks with daily/weekly/monthly patterns, enabling household chores to repeat automatically without manual recreation.

## Goal

Users can:
1. Create tasks faster with due date defaulting to today
2. Set up recurring tasks (daily, weekly, monthly) that auto-generate the next instance on completion
3. Edit existing tasks to add/modify recurrence patterns
4. View only the current instance of recurring tasks in the list (next instance appears after completion)

## Outcomes

- Frontend: TaskCreatePage default dueDate to today (browser local time)
- Backend: New RecurrencePattern entity + repository
- Backend: TaskService logic to generate next recurring instance on completion
- Backend: API contract update to handle recurrencePattern field
- Frontend: TaskCreatePage/TaskEditPage UI for recurrence selector (daily/weekly/monthly)
- Frontend: Recurrence pattern display on TaskCard
- Tests: Unit tests for recurrence generation; integration tests for complete-and-generate flow

## Prerequisites

- ✓ S-02 (basic-task-crud) — already fully implemented

## Blockers

None. Basic task CRUD is complete.

## Key Decisions

**Default due date**: Frontend-only (JavaScript, no backend change). Browser local timezone assumed.

**Recurrence scope**: Daily, weekly, monthly patterns only. Optional end date (infinite by default).

**Instance generation**: Synchronous on task completion (generate next instance in same request).

**List visibility**: Show only current instance of recurring task (next appears after completion).

**Edit behavior**: Edit changes parent recurrence pattern; past instances remain unchanged; future instances unaffected (deterministic re-generation from pattern).

---

See: `context/changes/task-enhancements-v1-1/plan.md` for full implementation details.
