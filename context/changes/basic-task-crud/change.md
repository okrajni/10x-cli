# Change: basic-task-crud

**Roadmap ID:** S-02  
**Change ID:** `basic-task-crud`  
**Status:** implementing  
**Updated:** 2026-08-29  

## Identity

Implement full task lifecycle (create, read, update, delete) for household task management. Users can manage household tasks with title, description, category, and required due date. All tasks are persistent, visible to household members, and trackable for completion.

## Goal

Users can create, view, edit, and delete tasks within a household. Tasks have title, description, category (5 fixed options), and due date (required). Tasks are soft-deleted, not hard-deleted. Completion is tracked via completed_at timestamp. All operations enforce household isolation.

## Outcomes

- POST /api/task — create new task
- GET /api/task — list all active household tasks (sorted by due date)
- PUT /api/task/{id} — edit task
- DELETE /api/task/{id} — soft-delete task
- Frontend: TaskCreatePage, TaskListPage, TaskEditPage, TaskDeleteDialog

## Prerequisites

- ✓ F-01 (auth-scaffold)
- ✓ F-02 (household-schema)
- ✓ F-04 (frontend-scaffold)

## Blockers

None. All prerequisites complete.

## Key Decision

**Due date is required** (not optional as PRD states). This simplifies sorting logic and Telegram reminder flow. Users cannot create undated tasks in MVP.

---

See: `context/changes/basic-task-crud/plan.md` for full implementation details.
