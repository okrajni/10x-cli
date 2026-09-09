# Basic Task CRUD — Plan Brief

> Full plan: `context/changes/basic-task-crud/plan.md`

## What & Why

Implement task creation, editing, deletion, and completion tracking in the done yet? app. Users need a way to externalize household coordination—creating and managing tasks is the first step toward shared responsibility. This slice unblocks task assignment (S-03) and the "Today" dashboard (S-06), which are essential for the north-star flow (Telegram reminders in S-04).

## Starting Point

- ✓ Auth API (registration & login) works with JWT tokens and global exception handling
- ✓ Household schema defined with Task entity (title, description, category enum, dueDate, soft-delete support)
- ✓ React app scaffolded with routing, Radix UI components, and API fetch wrapper
- ✓ New user setup demonstrates the full backend-to-frontend integration pattern

Missing: Task CRUD endpoints, service layer, frontend forms, and list views.

## Desired End State

Users (in the same household) can:
- Create tasks with title, description, category (5 fixed options), and required due date
- View all active household tasks in a list sorted by due date
- Edit any task's details
- Soft-delete tasks (recoverable)
- Mark tasks complete (tracked via completed_at timestamp)
- See ONLY their household's tasks (isolation enforced)

All changes are immediately reflected in the database and visible to household members after a page refresh (no real-time WebSocket sync).

## Key Decisions Made

| Decision                  | Choice                          | Why (1 sentence)             |
|---------------------------|---------------------------------|------------------------------|
| Soft-delete vs hard-delete | Soft-delete (recoverable)       | Aligns with F-02's soft-delete pattern already built in; prevents accidental data loss. |
| Completion tracking       | Separate completed_at timestamp | Enables S-06 dashboard to query "completed today" and S-04 Telegram to track delivery. |
| Concurrent edits conflict | Last-write-wins (optimistic)    | Simplest to implement; acceptable for MVP household scale (low edit frequency). |
| Task list pagination      | Load all at once (no pagination) | MVP simplicity; households typically have <50 active tasks. Scale in v1.1 if needed. |
| Sort order by default     | Due date ascending              | Urgency-driven; users see what's due first. Aligns with S-06 dashboard. |
| Completed tasks visibility | Hide by default, toggle to show  | Focuses on action items; completed tasks don't clutter the view. |
| Form validation           | Inline field-level errors       | Matches modern UX; users know exactly which field failed. |
| Real-time sync across users | Eventually consistent (refresh needed) | Avoids WebSocket infrastructure; acceptable for household use case. |
| Post-save behavior        | Success toast + stay on form    | Enables fast bulk entry of multiple tasks. |
| Due date requirement      | **Required** (not optional)      | Simplifies sorting logic and Telegram reminder scheduling. Contradicts PRD (which says optional); revisit in v1.1. |
| Household isolation       | Always validate on every edit/delete | Prevents URL hacking (user can't cross-household access). Non-negotiable security. |
| Testing depth             | Minimal (happy path only)       | Fast to implement; manual testing validates core flow. Covers happy path for core CRUD. |

## Scope

**In scope:**
- Create task (title, description, category, due date)
- Read tasks (list all active, sorted by due date)
- Update task (edit any field)
- Delete task (soft-delete, recoverable)
- Mark complete (set completed_at timestamp)
- Household isolation (user can only see/edit their household's tasks)
- Inline form validation
- Completion toggle (hide/show completed tasks)

**Out of scope:**
- Recurring tasks (deferred to v1.1)
- Task comments or collaboration notes
- Bulk operations (select-all, bulk delete)
- Advanced filtering or search
- Soft-delete recovery UI (recoverable but not exposed; for v1.1)
- Optional due date (MVP requires it; optional in v1.1)
- Real-time WebSocket sync
- Task attachments or file uploads

## Architecture / Approach

**Backend:**
- TaskService encapsulates CRUD logic; validates household membership on every mutation
- TaskController exposes 5 REST endpoints (POST, GET /id, GET list, PUT /id, DELETE /id)
- TaskRepository queries filter by household_id and deletedAt (soft-delete pattern)
- Reuse GlobalExceptionHandler + existing exception types (ValidationException → 422, ForbiddenException → 403)

**Frontend:**
- TaskCreatePage: form with inline validation, clears on success, stays on page for bulk entry
- TaskListPage: fetches all active tasks, sorts by due date, toggles completed visibility
- TaskEditPage: pre-fills form, updates on save, redirects back to list
- TaskDeleteDialog: confirmation before soft-delete
- TaskCard: reusable component displaying one task with edit/delete/complete actions
- API client: type-safe fetch wrapper with ApiResult<T> discriminated union

**Data flow**: Form submission → API call (POST/PUT/DELETE) → TaskService (validation) → TaskRepository (query) → DB. List fetches GET /api/task on mount, no polling or WebSocket. Changes visible after page refresh.

**Household isolation**: Backend-enforced, not frontend-enforced. Every TaskController method validates that the logged-in user is a member of the task's household; rejects with 403 if not. Prevents URL hacking.

## Phases at a Glance

| Phase           | What it delivers                                                        | Key risk                                                      |
|-----------------|-------------------------------------------------------------------------|---------------------------------------------------------------|
| 1. Backend API  | TaskController, TaskService, TaskDTO, household isolation validation   | Forgetting to validate household membership on edit/delete    |
| 2. Frontend UI  | TaskCreatePage, TaskListPage, TaskEditPage, TaskDeleteDialog, API client | Form state management complexity, list re-render performance  |

**Prerequisites:** All three foundations (F-01, F-02, F-04) complete and verified.  
**Estimated effort:** ~2-3 sessions across 2 phases (backend + frontend in parallel possible after API contract is locked).

## Open Risks & Assumptions

- **Due date as required field**: Contradicts PRD ("optional due date"). Chosen for MVP simplicity; revisit in v1.1 if users push back.
- **Eventually consistent sync**: Users must manually refresh to see partner's changes. Acceptable for MVP but may feel stale in real-time collaborative scenarios.
- **Minimal test coverage**: Happy path only; edge cases rely on manual testing. May miss validation bugs if automated tests don't exercise error cases.
- **No pagination**: MVP assumes <50 active tasks per household. If a household grows beyond that, list rendering may slow down; implement virtualizing/pagination in v1.1.

## Success Criteria (Summary)

- User can create task with title, description, category, and due date; form shows inline validation errors
- User can view all household tasks sorted by due date; completed tasks hidden by default
- User can edit task details and see changes reflected immediately (same session) or after refresh
- User can delete task and confirm soft-deletion
- User can mark task complete; task disappears from active list, reappears in completed toggle
- Cross-household access rejected with 403 error; household isolation enforced on every backend operation
