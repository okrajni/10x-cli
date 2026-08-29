# Basic Task CRUD — Implementation Plan

## Overview

Implement full task lifecycle (create, read, update, delete) in the done yet? household coordination app. Users can manage household tasks with title, description, category, and required due date. All tasks are persistent, visible to household members, and trackable for completion. This slice unblocks the assignment flow (S-03) and the dashboard (S-06).

## Current State Analysis

**Foundations ready:**
- ✓ **F-01 (auth-scaffold)**: Registration, login, rate limiting, JWT tokens, global exception handling
- ✓ **F-02 (household-schema)**: Task entity with title, description, category enum (CLEANING, SHOPPING, LAUNDRY, MAINTENANCE, BILLS), dueDate fields; soft-delete support via deletedAt
- ✓ **F-04 (frontend-scaffold)**: React + Vite, routing, API fetch wrapper, Radix UI components (Button, Input, Dialog, TaskCard), protected routes, TypeScript strict mode
- ✓ **S-01 (new-user-setup)**: Reference pattern showing how to integrate backend services, frontend forms, and API flows

**What's missing:**
- TaskController with CRUD endpoints
- TaskService with business logic
- TaskDTO for requests/responses
- Task repository query methods (with household isolation)
- Frontend: TaskCreatePage, TaskListPage, TaskEditPage
- Task deletion flow and confirmation UI

**Key Discoveries:**

- **Household isolation is non-negotiable**: F-02 repositories filter all queries by household_id. TaskService must use these patterns; backend MUST validate household membership on every edit/delete to prevent cross-household data leaks.
- **Category enum is fixed**: Five categories defined in schema (CLEANING, SHOPPING, LAUNDRY, MAINTENANCE, BILLS). Any deviation breaks S-06 dashboard grouping logic. Use the enum exactly as defined.
- **Soft-delete is foundational**: F-02 entity already has deletedAt column. Treat as "recycle bin" pattern — deleted tasks are hidden from queries but recoverable (useful for accidental delete recovery).
- **Completion tracking for downstream work**: Adding completed_at timestamp enables S-04 (Telegram reminders) and S-06 (dashboard) to distinguish active from done tasks. Critical dependency.
- **Pattern consistency**: Backend exceptions follow GlobalExceptionHandler → ValidationException (422), ConflictException (409), NotFoundException (404). Frontend API client already handles these via ApiResult<T> discriminated union. Reuse these patterns verbatim.

## Desired End State

After this plan is complete, a household member will be able to:

1. **Create a task** with title (required), description (optional), category (required, 5 choices), and due date (required) → form validates inline, success toast shown, form clears for next entry
2. **View all household tasks** in a list sorted by due date (ascending), with completed tasks hidden by default (toggleable)
3. **Edit a task** by clicking into it, changing any field, and saving → updates reflected immediately on the list
4. **Delete a task** with soft-delete (recoverable via 'show deleted' toggle later) → confirmation dialog shown before deletion
5. **Mark a task complete** by clicking a checkbox or button → task disappears from active list, reappears under completed (toggle to show)
6. **See only their household's tasks** — backend enforces household isolation; user cannot access other households' tasks even by URL hacking

**Verification:**
- Create a task with all fields → appears in list, sorted by due date
- Edit task title → list updates immediately (no page refresh needed for same-session changes)
- Complete a task → disappears from active list, shows in completed when toggled
- Soft-delete a task → gone from view, but can be recovered via "show deleted" (manual test only)
- Both users in the same household see the same task → cross-user visibility confirmed
- User A cannot edit user B's household tasks → 403 error on cross-household attempts (manual test)

## What We're NOT Doing

- **Recurring tasks (FR-009)** — Explicitly deferred to v1.1. Users create one-off tasks; if a task repeats, they recreate it manually.
- **Task attachment/file uploads** — Out of scope; focus on metadata only.
- **Task comments or collaboration notes** — No comments in MVP; S-01 establishes household membership as the collaboration unit.
- **Bulk task operations** — No select-all, bulk delete, or bulk reassign in MVP. Single-task operations only.
- **Advanced filtering** — No complex query builders or saved filters. Sort by due date only in MVP.
- **Soft-delete recovery UI** — Tasks are soft-deleted but recovery is not exposed in UI. Recoverable via backend query if needed; scope for v1.1.
- **Optional due date** — Contrary to PRD's "optional", we require due date for MVP to simplify sorting and Telegram reminder logic. Revisit in v1.1 if users push back.

## Implementation Approach

**Backend (Spring Boot):**
- Create TaskService with CRUD logic; validate household membership on every mutation
- Create TaskController with 5 endpoints: POST (create), GET /id (read), GET (list), PUT /id (update), DELETE /id (soft-delete)
- Create TaskDTO for request/response shapes
- Reuse existing GlobalExceptionHandler + exception types
- Repository queries already filter by household (inherited from F-02 patterns)

**Frontend (React):**
- Create TaskCreatePage: form with title, description, category dropdown, date picker; inline validation; stay-on-form post-save
- Create TaskListPage: fetch all active tasks, sort by due date, show completed toggle, render TaskCard for each
- Create TaskEditPage: pre-fill form with task details, allow edits, update on save
- Create TaskDeleteDialog: confirmation, soft-delete on confirm
- Reuse Radix UI Button/Input/Dialog components from F-04
- API client methods: createTask, getTask, listTasks, updateTask, deleteTask
- Eventually consistent sync (no polling or WebSocket) — users manually refresh to see partner's changes

**Data Flow:**
```
TaskCreatePage (form)
  ↓ [POST /api/task]
TaskService.createTask()
  ↓ (validate, save)
TaskRepository.save()
  ↓ [200 + new task]
TaskListPage (refetch or append locally)
  ↓ (render in sorted list)
User sees task
```

**Architectural Decision:**
- **Household isolation is backend-enforced, not frontend-enforced**: Frontend trusts the server. Backend rejects any cross-household edit with 403. This prevents URL hacking (user changes ?householdId in URL).
- **Completion is a separate state, not deletion**: Tasks are "completed" (marked with completed_at timestamp), not deleted. This enables S-06 dashboard to query "tasks completed today" and S-04 to track Telegram reminder delivery.
- **Eventually consistent, not real-time**: Users must refresh to see partner's changes. Acceptable for MVP household scale (low edit frequency). Avoids WebSocket infrastructure burden.

## Critical Implementation Details

**Due Date Requirement**: Unlike the PRD (which says "optional due date"), this plan requires due date on every task. Why: sorting by due date is the default sort order; undated tasks would need special handling (sort to bottom, separate "no date" bucket). For MVP simplicity and to enable Telegram reminder logic (which needs a due date), we mandate it. If users request optional due dates in v1.1, add a nullable dueDate column and handle NULL values in sort logic.

**Household Membership Validation**: Before any edit/delete, TaskService must query the household and confirm the logged-in user is a member. Do NOT trust the task ID alone. This is non-negotiable security:
```java
// Pseudo-code
@PutMapping("/task/{id}")
public ResponseEntity<?> updateTask(@PathVariable UUID id, @RequestBody TaskUpdateRequest req, @AuthenticationPrincipal User user) {
    Task task = taskRepository.findById(id).orElseThrow(NotFoundException::new);
    Household household = task.getHousehold();
    
    // THIS CHECK IS CRITICAL
    if (!household.getMembers().stream().anyMatch(m -> m.getUserId().equals(user.getId()))) {
        throw new ForbiddenException("User not a member of this household");
    }
    // ... continue with update
}
```

**Soft-Delete Query Pattern**: All task queries must filter out soft-deleted rows. Repository methods should have variants (activeOnly() or includeDeleted()) to control this. Default: activeOnly.

---

## Phase 1: Backend Task CRUD API

### Overview

Implement REST endpoints and service layer for task creation, retrieval, updating, and soft-deletion with household isolation enforced at every step.

### Changes Required:

#### 1. Task Entity Extension & Repository

**File**: `src/main/java/com/example/doneyet/entity/Task.java`

**Intent**: Verify Task entity has all required fields and add completed_at timestamp for completion tracking.

**Contract**: 
- Fields: id (UUID), householdId (FK), title (String, required), description (String, nullable), category (Category enum), dueDate (LocalDate, required), completedAt (LocalDate, nullable), deletedAt (LocalDate, nullable)
- Annotations: @Entity, @Table(name="tasks"), @ManyToOne(fetch=LAZY) for household, soft-delete support via @SQLDelete and @Where

---

#### 2. Task Repository with Household Isolation

**File**: `src/main/java/com/example/doneyet/repository/TaskRepository.java`

**Intent**: Define query methods for task operations that enforce household isolation via filtering.

**Contract**:
- `List<Task> findActiveByHouseholdId(UUID householdId)` — return active tasks (deletedAt IS NULL), ordered by dueDate ASC
- `Optional<Task> findByIdAndHouseholdId(UUID taskId, UUID householdId)` — fetch one task, confirming it belongs to the household
- `Task save(Task task)` — persist new or updated task
- `void deleteById(UUID taskId)` — soft-delete via @SQLDelete (no hard delete in Spring)

All queries must use @Where(clause = "deleted_at IS NULL") to automatically exclude soft-deleted rows. Use the same pattern as household-schema repositories.

---

#### 3. Task DTO (Request/Response)

**File**: `src/main/java/com/example/doneyet/dto/TaskDto.java`

**Intent**: Define request/response shapes for task API endpoints.

**Contract**: Inner classes:
- `CreateTaskRequest`: title (required, non-empty), description (optional), category (required, enum from list), dueDate (required, LocalDate)
- `UpdateTaskRequest`: title (optional), description (optional), category (optional), dueDate (optional), completedAt (optional)
- `TaskResponse`: id, householdId, title, description, category, dueDate, completedAt, deletedAt, createdAt, updatedAt

Validation: Use @NotEmpty on title, @NotNull on category and dueDate (create only).

---

#### 4. Task Service with Business Logic

**File**: `src/main/java/com/example/doneyet/service/TaskService.java`

**Intent**: Encapsulate task CRUD logic and household isolation enforcement.

**Contract**: Methods:
- `Task createTask(UUID householdId, CreateTaskRequest req)` — create and save task; validate household exists
- `Task getTask(UUID taskId, UUID householdId)` — fetch by ID, confirming household membership; throw 404 if not found or wrong household
- `List<Task> listTasks(UUID householdId)` — fetch all active tasks for household, sorted by dueDate ASC
- `Task updateTask(UUID taskId, UUID householdId, UpdateTaskRequest req)` — validate membership, update fields, save
- `void deleteTask(UUID taskId, UUID householdId)` — validate membership, soft-delete via deletedAt = now()

Each method validates household membership before proceeding. Throw ForbiddenException (403) if user is not a member.

---

#### 5. Task Controller

**File**: `src/main/java/com/example/doneyet/controller/TaskController.java`

**Intent**: REST endpoints for task operations.

**Contract**: 
- `POST /api/task` — Create task; body: CreateTaskRequest; response: 200 + TaskResponse
- `GET /api/task/{id}` — Read one task; response: 200 + TaskResponse or 404
- `GET /api/task?householdId=X` — List all active tasks for household; response: 200 + [TaskResponse]
- `PUT /api/task/{id}` — Update task; body: UpdateTaskRequest; response: 200 + TaskResponse or 404/403
- `DELETE /api/task/{id}` — Soft-delete task; response: 204 (no content) or 404/403

Extract householdId and userId from auth context (JWT token + @AuthenticationPrincipal). Pass to service for validation.

---

#### 6. Update SecurityConfig for Task Endpoints

**File**: `src/main/java/com/example/doneyet/config/SecurityConfig.java`

**Intent**: Protect task endpoints; only authenticated users can access.

**Contract**: Add `/api/task/**` to protected routes (require Spring Security AUTHENTICATED). Task endpoints are not public (unlike `/api/invitation/*/accept` which is public).

### Success Criteria:

#### Automated Verification:

- `mvn compile` succeeds
- `mvn test` runs unit tests for TaskService (create, list, update, delete with household isolation)
- `mvn test` runs integration tests for TaskController endpoints
- Invalid input (empty title, missing category/dueDate) rejected with 422 ValidationException
- Task from another household rejected with 403 ForbiddenException
- Soft-deleted tasks filtered from list queries
- Task completion (completedAt set) preserves task record (not deleted)
- Foreign key constraint on householdId prevents orphan tasks

#### Manual Verification:

- Create task via curl/Postman with valid payload → 200 + task returned
- List tasks → returns only tasks from the logged-in user's household
- Edit task → 200 + updated fields reflected
- Delete task → 204, task gone from list but recoverable (manual SQL query)
- Attempt to edit task from another household (invalid householdId in URL) → 403 Forbidden
- Mark task complete (set completedAt) → task stays in DB but no longer in active list

---

## Phase 2: Frontend Task CRUD UI

### Overview

Implement forms and list views for task creation, editing, deletion, and completion with inline validation and eventually consistent sync.

### Changes Required:

#### 1. Create TaskCreatePage Component

**File**: `frontend/src/features/task/pages/TaskCreatePage.tsx`

**Intent**: Form for creating new task with title, description, category selector, and date picker.

**Contract**: React component rendering:
- Form with fields: title (required, text input), description (optional, textarea), category (required, dropdown of 5 options), dueDate (required, date input)
- Inline validation: show error text below each field if validation fails (required, format, etc.)
- Submit button disabled until form is valid
- On submit: POST to `/api/task`, show success toast, clear form, stay on page for next task
- On error (422, 409, 500): display error toast, keep form filled in (don't clear)
- Loading state while request in flight

---

#### 2. Create TaskListPage Component

**File**: `frontend/src/features/task/pages/TaskListPage.tsx`

**Intent**: Display all active household tasks sorted by due date, with toggle to show completed tasks.

**Contract**: React component rendering:
- Fetch all tasks from GET /api/task on mount
- Display tasks in a list (TaskCard component for each), sorted by dueDate ASC
- Toggle button at top: "Show completed (X)" that reveals/hides completed tasks
- Each TaskCard shows: title, description (truncated), category (color-coded), dueDate, assignee (when S-03 lands)
- Actions on each card: Edit button → navigate to TaskEditPage, Delete button → show DeleteDialog, Checkbox to mark complete
- Empty state message if no active tasks
- Loading skeleton while fetching

---

#### 3. Create TaskEditPage Component

**File**: `frontend/src/features/task/pages/TaskEditPage.tsx`

**Intent**: Form to edit an existing task's details.

**Contract**: React component rendering:
- Fetch task details on mount: GET `/api/task/{id}`
- Pre-fill form with current values (title, description, category, dueDate)
- Same form layout as TaskCreatePage (reuse form validation logic)
- Submit button sends PUT `/api/task/{id}` with UpdateTaskRequest
- On success: show success toast, navigate back to TaskListPage
- On error (404, 403, 422): show error toast, keep form filled
- Loading state while fetching task or submitting update

---

#### 4. Create TaskDeleteDialog Component

**File**: `frontend/src/features/task/components/TaskDeleteDialog.tsx`

**Intent**: Confirmation dialog before soft-deleting a task.

**Contract**: React component (Radix Dialog):
- Display task title in confirmation message: "Delete '{taskTitle}'? This can be recovered later."
- Two buttons: Cancel (close dialog), Delete (call DELETE /api/task/{id}, close dialog, refetch list)
- On success: show "Task deleted" toast, remove from list
- On error (403, 404, 500): show error toast, leave dialog open

---

#### 5. Update TaskListPage with TaskDeleteDialog Integration

**File**: `frontend/src/features/task/pages/TaskListPage.tsx` (continued from 2)

**Intent**: Wire the delete dialog to the TaskCard delete action.

**Contract**:
- When user clicks "Delete" on a TaskCard, open TaskDeleteDialog with task ID
- Pass task title to dialog for confirmation message
- On delete confirm, call TaskService.deleteTask(id), then refetch list

---

#### 6. Create TaskCard Component (if not already in F-04)

**File**: `frontend/src/features/task/components/TaskCard.tsx`

**Intent**: Reusable card displaying one task's details with action buttons.

**Contract**: React component:
- Props: task (Task), onEdit (callback), onDelete (callback), onComplete (callback)
- Display: title, category (badge), dueDate (formatted), assignee (when S-03 lands), description (truncated)
- Actions: Edit button, Delete button, Checkbox (mark complete), truncation with "..." if description long
- Styling: Radix UI + Tailwind; use consistent design from F-04 components

---

#### 7. Task API Client Methods

**File**: `frontend/src/features/task/api.ts`

**Intent**: Type-safe API client functions for task endpoints.

**Contract**: Functions (using existing ApiResult<T> pattern from F-04):
- `createTask(req: CreateTaskRequest): Promise<ApiResult<TaskResponse>>`
- `getTask(id: string): Promise<ApiResult<TaskResponse>>`
- `listTasks(): Promise<ApiResult<TaskResponse[]>>`
- `updateTask(id: string, req: UpdateTaskRequest): Promise<ApiResult<TaskResponse>>`
- `deleteTask(id: string): Promise<ApiResult<void>>`

All use existing fetch wrapper with bearer token injection. Handle errors via discriminated union (ok: true/false).

---

#### 8. Update AuthContext to Fetch & Cache Household Tasks

**File**: `frontend/src/features/auth/context/AuthContext.tsx`

**Intent**: On login, fetch the user's household tasks and cache them for the session. Allow manual refresh.

**Contract**:
- Add to AuthContext state: `tasks: Task[]`, `tasksLoading: boolean`
- Add method: `refreshTasks()` — fetch GET /api/task, update state
- On successful login, call `refreshTasks()` to populate task list
- Export `useTasks()` hook for TaskListPage to subscribe to task list

---

#### 9. Update Router with Task Routes

**File**: `frontend/src/router.tsx`

**Intent**: Add routes for task CRUD pages.

**Contract**: Add routes:
- `GET /task/create` → TaskCreatePage (protected)
- `GET /task/:id` → TaskEditPage (protected)
- `GET /task` → TaskListPage (protected, homepage after login)

---

#### 10. Add Category Label Utils

**File**: `frontend/src/features/task/utils/categoryUtils.ts`

**Intent**: Map category enum to human-readable labels and colors.

**Contract**: Export:
- `categoryLabel(category: Category): string` — e.g., CLEANING → "Cleaning"
- `categoryColor(category: Category): string` — e.g., CLEANING → "bg-blue-100" (Tailwind class)

Use for TaskCard display and category dropdown.

### Success Criteria:

#### Automated Verification:

- TypeScript compilation passes (`npm run build` succeeds)
- ESLint passes all checks (`npm run lint`)
- App starts without console errors (`npm run dev`)
- TaskCreatePage renders without errors
- TaskListPage fetches and displays tasks without errors
- Form validation triggers on invalid input

#### Manual Verification:

- Create task with all fields → appears in list sorted by due date
- Create task with invalid input (empty title) → validation error shown inline, form not submitted
- Edit task → navigate to edit page, change a field, save → changes reflected in list
- Delete task → confirmation dialog appears, confirm → task removed from list
- Mark task complete → task disappears from active list, reappears in completed (toggle)
- Both users in same household see the same task list (manual refresh)
- Attempt to access task from another household (URL hack) → 403 error shown
- Empty state message displayed when no active tasks

---

## Testing Strategy

### Unit Tests:

**Backend (TaskService):**
- Create task with valid/invalid input (missing required field, invalid dueDate format)
- List tasks filters by household and excludes soft-deleted
- Update task only succeeds if user is member of task's household
- Delete task soft-deletes (sets deletedAt, doesn't remove row)
- Complete task sets completedAt timestamp

**Frontend (React components):**
- TaskCreatePage: form validation triggers on invalid input, submit calls API with correct payload
- TaskListPage: renders task list, sort by due date verified, completed toggle shows/hides
- TaskEditPage: pre-fills form on load, submit calls update API with changed fields

### Integration Tests:

**Backend:**
- POST /api/task with valid payload → 200 + task in DB
- POST /api/task with missing title → 422 ValidationException
- GET /api/task for household → returns only that household's active tasks (not other household's tasks)
- PUT /api/task/{id} from different household → 403 ForbiddenException
- DELETE /api/task/{id} → soft-delete (deletedAt set, row still in DB)

**Frontend:**
- User creates task → form clears, success toast shows, new task appears in list (after manual refresh)
- User edits task → navigates to edit page, changes title, saves → list shows updated title (after refresh)
- User deletes task → confirmation dialog, confirm → task removed from list (after refresh)
- Completed task toggle shows/hides completed tasks from list

### Manual Testing Steps:

1. Start dev server: `npm run dev` (frontend) + `mvn spring-boot:run` (backend)
2. Register user A (alice@example.com), create household, log in
3. Create 3 tasks with different categories and due dates (past, today, future)
4. Verify tasks sorted by due date on list
5. Edit one task (change title) → verify updated on list
6. Mark one task complete → verify disappears from active list, shows in completed toggle
7. Delete one task → confirm dialog, verify removed
8. Register user B (bob@example.com), invite to household (S-01 flow)
9. User B logs in, creates a task → User A refreshes and sees it
10. User B tries to access User A's other household via URL hack → 403 error
11. Test edge cases: create task with missing due date → validation error, create task with very long description → truncated on card

## Performance Considerations

- **Query optimization**: Task list fetch (GET /api/task) should return active tasks only. Add index on (household_id, deleted_at) to speed query.
- **Frontend rendering**: TaskListPage renders all active tasks in a single list (no pagination per MVP decision). For households with >100 tasks, consider virtualizing the list in React (react-window) or adding pagination in v1.1.
- **API response time**: TaskService.listTasks() must complete in <500ms for household with 50 tasks. If slower, add database index on (household_id, deleted_at, due_date).

## Migration Notes

None for MVP. On v1.1 upgrade to support optional due dates:
- Add nullable column: `ALTER TABLE tasks ADD COLUMN due_date TIMESTAMP NULL;` (existing NOT NULL constraint allows one-time dual writing)
- Update sort logic in TaskRepository to handle NULL dueDate (e.g., sort NULL to bottom)
- Update frontend form to make dueDate optional
- No API contract changes needed; UpdateTaskRequest.dueDate can already be optional

## References

- Foundation: `context/foundation/roadmap.md` (S-02)
- PRD: `context/foundation/prd.md` (FR-006, FR-008, FR-010–013)
- Auth scaffold: `context/changes/auth-scaffold/plan.md`
- Household schema: `context/changes/household-schema/plan.md`
- Frontend scaffold: `context/changes/frontend-scaffold/plan.md`
- New user setup (reference pattern): `context/changes/new-user-setup/plan.md`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Backend Task CRUD API

#### Automated

- [x] 1.1 Task entity with completedAt timestamp for completion tracking — a03414b
- [x] 1.2 TaskRepository with household isolation queries (findActiveByHouseholdId, findByIdAndHouseholdId) — a03414b
- [x] 1.3 TaskDTO (CreateTaskRequest, UpdateTaskRequest, TaskResponse) with validation — a03414b
- [x] 1.4 TaskService with CRUD logic and household membership validation — a03414b
- [x] 1.5 TaskController with 5 REST endpoints (create, read, list, update, delete) — a03414b
- [x] 1.6 Update SecurityConfig to protect task endpoints — a03414b
- [x] 1.7 Unit tests pass for TaskService (CRUD, isolation, soft-delete) — a03414b
- [x] 1.8 Integration tests pass for TaskController endpoints — a03414b
- [x] 1.9 Compilation and type checking pass — a03414b

#### Manual

- [x] 1.10 Create task via curl/Postman → 200 with task returned
- [x] 1.11 List tasks → returns only current household's tasks
- [x] 1.12 Edit task → 200 with updated fields
- [x] 1.13 Soft-delete task → 204, task gone from active queries but recoverable
- [x] 1.14 Cross-household access rejected → 403 Forbidden
- [x] 1.15 Mark task complete (completedAt set) → task stays in DB, excluded from active list

### Phase 2: Frontend Task CRUD UI

#### Automated

- [x] 2.1 TypeScript compilation passes
- [x] 2.2 ESLint passes all checks
- [x] 2.3 App starts without console errors
- [x] 2.4 TaskCreatePage renders without errors
- [x] 2.5 Form validation works (required field, invalid input rejected)

#### Manual

- [x] 2.6 Create task with all fields → appears in list sorted by due date
- [x] 2.7 Create task with invalid input (empty title) → inline error shown, not submitted
- [x] 2.8 Edit task → form pre-filled, changes saved and reflected in list (after refresh)
- [x] 2.9 Delete task → confirmation dialog, confirm → task removed
- [x] 2.10 Mark task complete → disappears from active list, reappears in completed toggle
- [x] 2.11 Single user sees their household's tasks (multi-user scaffold removed, now single-user model)
- [x] 2.12 Cross-household access (URL hack) → 403 error shown
- [x] 2.13 Empty state message when no active tasks
