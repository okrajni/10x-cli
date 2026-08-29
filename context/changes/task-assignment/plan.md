# Task Assignment Implementation Plan

## Overview

Implement task assignment in the done yet? household coordination app. Users can assign tasks to themselves or their partner at creation time or change assignments later. Both household members see the assignee on every task, enabling shared responsibility and coordination. Assignment is foundational for Telegram reminders (S-04) which need to know who to remind, and for the "Today" dashboard (S-06) which groups tasks by assignee.

## Current State Analysis

**Foundations ready:**
- ✓ **F-02 (household-schema)**: Task entity exists with all CRUD fields (title, description, category, dueDate, completedAt, deletedAt); HouseholdMember entity establishes the user-household relationship
- ✓ **S-02 (basic-task-crud)**: Backend TaskService, TaskController, DTO patterns established; frontend TaskCreatePage, TaskListPage, TaskEditPage patterns in place
- ✓ **F-04 (frontend-scaffold)**: React + Vite, Radix UI (Button, Input, Dialog, Select), API client with type-safe fetch wrapper, protected routes

**What's missing:**
- Task entity needs `assigneeId` foreign key to HouseholdMember
- Task entity needs helper method to get assignee details (name, email) for API responses
- TaskService must fetch household members for validation and assign default (task creator)
- TaskRepository needs to support optional assignee filtering (for downstream S-06 dashboard grouping)
- TaskDTO must include assigneeId in requests and assignee object (name, email, id) in responses
- TaskController endpoints need to handle assigneeId in create/update payloads
- Frontend: assignment dropdown in TaskCreatePage and TaskEditPage (populated with household members)
- Frontend: display assignee name on TaskCard in TaskListPage
- Frontend: API client methods need updated signatures to include assigneeId

**Key Discoveries:**

- **Assignee is a required relationship**: Unlike the optional description field, assignee MUST be set on every task. The core rule is "tasks are never assigned to the household — they're always assigned to a specific person." Enforcing this at the database level (NOT NULL constraint on assigneeId) prevents accidental unassigned state.
- **HouseholdMember is the assignee, not User**: The relationship is Task → HouseholdMember (not Task → User). This ensures assignment is scoped to the household (both members of household A see the task, members of household B do not). This follows the data isolation pattern from F-02.
- **Default assignee is task creator**: If user doesn't explicitly pick an assignee, the task defaults to the creator (the logged-in user at creation time). This is sensible UX — people usually create tasks for themselves or explicitly pick "partner" if delegating.
- **Any household member can reassign any task**: Both users have equal rights. This supports the shared responsibility model (e.g., "I can take this from you"). No role-based edit restrictions. Backend must still validate that the logged-in user is a member of the household before allowing the update (existing pattern from S-02).
- **Auto-reassign when member leaves household**: If a user is removed from the household, all their assigned tasks automatically reassign to the remaining member. This is a business logic concern, not a data constraint — it's handled by the member-removal service (out of scope for S-03, assumed handled in user-management work). Leave a TODO comment in TaskService if needed.
- **Telegram reminders target assignee only**: S-04 will send reminders only to the assigned user (not to both). This requires knowing the assignee reliably; strict validation on assignment is non-negotiable.
- **Strict validation on assignee**: The frontend dropdown will only show valid household members. Backend must validate that the provided assigneeId is a member of the household (query HouseholdMember table with household_id + user_id). Return 422 (validation error) if invalid or not in household.
- **No grouping in S-03 list view**: The TaskListPage remains a simple sorted list by dueDate with assignee name visible on each card. Grouping by assignee (My tasks | Partner's tasks) is deferred to S-06 dashboard. Keep S-03 scope tight.

## Desired End State

After this plan is complete, a household member will be able to:

1. **Create a task with assignment** — Form includes an assignment dropdown (defaulting to "me" / task creator). User can pick themselves or their partner. Task saves with assignee set. Success toast confirms.
2. **View tasks with assignee visible** — TaskListPage displays a list of all active household tasks sorted by due date. Each card shows: title, description (truncated), category (color), due date, **and assignee name** ("Assigned to: Alex").
3. **Reassign a task** — Edit a task, change the assignee dropdown to a different household member, save. Backend updates assignee immediately. No errors if both users reassign the same task simultaneously (last write wins; documented as eventual consistency).
4. **See who owns what** — Scan the task list and understand at a glance who's responsible for each task. This mental clarity enables shared responsibility without creating a management bottleneck.
5. **Have Telegram reminders target the right person** — When S-04 lands, reminders go to the assignee because the assignee is now explicitly tracked.

**Verification:**
- Create a task with assignment to partner → task appears in list with partner's name shown
- Edit task, change assignment back to me → list updates to show my name
- Both users see the same assignee on the same task → cross-user visibility confirmed
- Try to assign to invalid user (e.g., random UUID) → 422 error on API call
- Try to assign to user from another household → 422 error
- Reassign task while partner is viewing it → partner sees the change without page refresh (from S-02 patterns, "eventually consistent")

## What We're NOT Doing

- **Grouping by assignee in S-03** — Single sorted list only. Grouping (My tasks | Partner's tasks) is S-06 responsibility.
- **Permissions on who can reassign** — No role-based restrictions. Both users can reassign any task. Simplifies logic; aligns with flat permission model.
- **Member removal handling within S-03** — Auto-reassignment of orphaned tasks when a member leaves is a downstream concern (user-management feature). Assumed handled elsewhere; leave a TODO comment if needed.
- **Read-only tasks** — No "view-only" mode for partner's tasks. Both users can edit any task (including partner's). Supports shared responsibility.
- **Reassignment audit trail** — No tracking of "who changed the assignment and when." Not needed for MVP. Defer to v1.1 if needed.
- **Bulk reassignment** — No "select multiple tasks and reassign to person X" flow. Single-task operations only (consistent with S-02 scope).
- **Custom assignment rules** — No "notify both users of every reassignment" or complex notification logic. Keep Telegram reminder targeting simple: assignee only.

## Implementation Approach

**Data Model:**
- Task entity gains `assigneeId` (UUID, NOT NULL, FK to HouseholdMember) and `assignee` (fetch=LAZY) relationship
- Queries must validate household membership: i.e., confirm the logged-in user is a member of the household that owns the task

**Backend (Spring Boot):**
- Extend TaskDTO with assigneeId (create/update requests) and assignee object (read responses): { id, name, email }
- Extend TaskService.createTask() to accept assigneeId parameter, default to task creator if null, validate assignee is in household
- Extend TaskService.updateTask() to accept assigneeId, validate against household
- Extend TaskController POST/PUT endpoints to handle assigneeId in payloads
- Repository: no new query methods needed (fetch and update already work; just add assignee FK)

**Frontend (React):**
- Fetch household members on mount (new API endpoint: GET /api/household/{id}/members or reuse from existing auth context)
- TaskCreatePage: add HouseholdMemberSelect dropdown (populated with household members, default to current user)
- TaskEditPage: same dropdown for reassignment
- TaskListPage: display assignee name on each TaskCard ("Assigned to: Alex")
- API client: update createTask() and updateTask() signatures to include assigneeId

**Error Handling:**
- Invalid assigneeId (not in household): Backend returns 422 ValidationException. Frontend shows error toast: "Assignee not found or not in this household."
- Assignee no longer a member (race condition): Eventual consistency — list may briefly show stale assignee. Next refresh fixes it. (Deferred to S-06 real-time sync if needed.)

## Critical Implementation Details

**NOT NULL Constraint on assigneeId**: The Task table must have `assignee_id NOT NULL`. This enforces the rule "tasks are always assigned to someone." Do not allow NULL or unassigned state.

**HouseholdMember as the FK Target**: The foreign key points to HouseholdMember, not User. This ensures:
- Queries can filter by household before checking the assignee
- A user assigned a task in Household A is not accidentally visible in Household B
- Household isolation is preserved at the join level

Example schema validation:
```sql
ALTER TABLE task ADD COLUMN assignee_id UUID NOT NULL;
ALTER TABLE task ADD CONSTRAINT fk_task_assignee 
  FOREIGN KEY (assignee_id) REFERENCES household_member(id) ON DELETE CASCADE;
```

**Default Assignee in Service Layer**: When createTask() is called without an explicit assigneeId, default to the logged-in user (available via @AuthenticationPrincipal User in the controller, passed to service). Example:
```java
Task createTask(UUID householdId, CreateTaskRequest req, @Nullable UUID explicitAssigneeId) {
    UUID assigneeId = explicitAssigneeId != null 
        ? explicitAssigneeId 
        : getCurrentUsersHouseholdMemberId(householdId); // default to creator
    
    // Validate assignee is in household
    HouseholdMember assignee = householdMemberRepository
        .findByIdAndHouseholdId(assigneeId, householdId)
        .orElseThrow(() -> new ValidationException("Invalid assignee"));
    
    Task task = new Task(householdId, req.title, req.description, req.category, req.dueDate, assignee);
    return taskRepository.save(task);
}
```

**API Response Includes Assignee Details**: The TaskResponse DTO must include:
```java
public class TaskResponse {
    public UUID id;
    public UUID householdId;
    public String title;
    public String description;
    public String category;
    public LocalDate dueDate;
    public LocalDate completedAt;
    public LocalDate deletedAt;
    public AssigneeDTO assignee; // new: { id, name, email }
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}

public class AssigneeDTO {
    public UUID id;
    public String name;
    public String email;
}
```

**Frontend Dropdown Needs Household Members List**: To populate the assignment dropdown, the frontend must fetch the list of household members once on app load (or when joining the task form) and cache it. Example:
```typescript
// On app load or when mounting TaskCreatePage
const { data: household } = await apiClient.get(`/api/household/${householdId}`);
// Response includes: { id, name, members: [ { id, name, email }, ... ] }
// Use members array to populate dropdown
```

(Alternatively, if the household context is already available in React state from S-01 onboarding, reuse that.)

---

## Phase 1: Backend Assignment API

### Overview

Extend the Task entity with an assignee relationship, update DTOs and service logic to handle assignment with strict validation, and ensure all endpoints enforce household isolation.

### Changes Required:

#### 1. Task Entity — Add Assignee Relationship

**File**: `src/main/java/com/example/doneyet/entity/Task.java`

**Intent**: Extend Task entity with mandatory assignee (HouseholdMember foreign key) and helper method to fetch assignee details.

**Contract**: 
- New field: `assigneeId` (UUID, not null) with `@ManyToOne(fetch=LAZY) @JoinColumn(name="assignee_id")` relationship to HouseholdMember
- New field: `assignee` (HouseholdMember object, lazy-loaded)
- Constructor updated: `Task(UUID householdId, String title, ... , HouseholdMember assignee)` 
- Helper method: `AssigneeDTO getAssigneeDTO()` returning { id, name, email } for API responses

---

#### 2. Task Repository — Add Assignee Queries

**File**: `src/main/java/com/example/doneyet/repository/TaskRepository.java`

**Intent**: Extend repository with household-scoped queries that handle the new assignee relationship.

**Contract**: 
- Existing methods `findActiveByHouseholdId()`, `findByIdAndHouseholdId()` remain unchanged (they work with the new assignee FK without modification)
- No new query methods needed for S-03; the basic CRUD operations already support the assignee field
- All queries automatically include the new assignee data via lazy-load fetch when accessed

---

#### 3. Task DTO — Add Assignee to Request/Response

**File**: `src/main/java/com/example/doneyet/dto/TaskDto.java`

**Intent**: Extend request/response DTOs to include assigneeId in requests and assignee details in responses.

**Contract**: 
- `CreateTaskRequest`: add optional field `assigneeId` (UUID, nullable; service will default to creator if null). Keep title, description, category, dueDate as before.
- `UpdateTaskRequest`: add optional field `assigneeId` (UUID, nullable; if present, update the assignee)
- `TaskResponse`: add `assignee` field (object with id, name, email) and ensure it's populated from Task.getAssigneeDTO()
- Inner class `AssigneeDTO`: { id (UUID), name (String), email (String) }

Validation: Keep existing validation for title, category, dueDate. Add no explicit @NotNull on assigneeId in DTO (service layer validates).

---

#### 4. Task Service — Assignment Logic & Validation

**File**: `src/main/java/com/example/doneyet/service/TaskService.java`

**Intent**: Encapsulate assignment business logic: default to creator, validate assignee is in household, handle reassignment.

**Contract**: 
- `Task createTask(UUID householdId, CreateTaskRequest req, User currentUser)`
  - If req.assigneeId is null, default to the logged-in user's HouseholdMember record for this household
  - Validate the assignee (whether explicit or defaulted) is a member of the household; throw ValidationException if not
  - Create and save task with the validated assignee
- `Task updateTask(UUID taskId, UUID householdId, UpdateTaskRequest req, User currentUser)`
  - If req.assigneeId is provided (not null), validate it's in the household; throw ValidationException if not
  - Update the task's assignee and other fields; save
  - If req.assigneeId is null, leave assignee unchanged
- Helper method: `HouseholdMember validateAndFetchAssignee(UUID householdId, UUID assigneeId)` 
  - Query HouseholdMember table with both householdId and assigneeId; throw ValidationException if not found

---

#### 5. Task Controller — Add Assignee to Endpoints

**File**: `src/main/java/com/example/doneyet/controller/TaskController.java`

**Intent**: Update REST endpoints to accept and return assignee data; pass assignee to service layer.

**Contract**: 
- `POST /api/task` — CreateTaskRequest now has optional assigneeId field; pass req and currentUser to service.createTask()
- `PUT /api/task/{id}` — UpdateTaskRequest now has optional assigneeId field; pass req and currentUser to service.updateTask()
- GET endpoints (read one / list) — TaskResponse now includes assignee details; auto-populated from Task entity
- All responses return AssigneeDTO (id, name, email) so frontend knows who the task is assigned to

---

### Success Criteria:

#### Automated Verification:

- `mvn compile` succeeds; entity/DTO/service/controller compile cleanly
- Unit tests for TaskService pass:
  - `createTask()` with no assigneeId defaults to current user
  - `createTask()` with explicit valid assigneeId saves correctly
  - `createTask()` with invalid assigneeId throws ValidationException
  - `createTask()` with assigneeId from another household throws ValidationException
  - `updateTask()` with valid reassignment works
  - `updateTask()` with invalid reassignment throws ValidationException
- Integration tests pass:
  - POST `/api/task` with assigneeId field returns 200 + TaskResponse with assignee details
  - POST `/api/task` without assigneeId defaults to creator
  - POST `/api/task` with invalid assigneeId returns 422 ValidationException
  - PUT `/api/task/{id}` to reassign returns 200 + updated TaskResponse
  - PUT `/api/task/{id}` with invalid assigneeId returns 422
- Database schema migration creates assignee_id column, adds NOT NULL constraint and FK to household_member
- Foreign key constraint prevents orphan tasks (cascading delete if member is removed)

#### Manual Verification:

- Create task via curl/Postman WITH assigneeId → 200 + returns full TaskResponse with assignee name
- Create task via curl/Postman WITHOUT assigneeId → defaults to creator, 200 + assignee is creator
- List tasks → each task in response includes assignee object (id, name, email)
- Attempt to reassign to user from another household → 422 error
- Attempt to reassign to non-existent user ID → 422 error

---

## Phase 2: Frontend Assignment UI

### Overview

Add assignment dropdown to task creation/edit forms, display assignee on task cards, and integrate with React state management to fetch and cache household members.

### Changes Required:

#### 1. Extend App Context — Cache Household Members

**File**: `frontend/src/context/HouseholdContext.tsx`

**Intent**: Make household members list available to components that need it (for dropdown population). Fetch once, cache in React Context.

**Contract**: 
- HouseholdContext now provides: `household { id, name, members: [ { id, name, email }, ... ] }`
- On app initialization (e.g., in a ProtectedLayout or onboarding flow), fetch `GET /api/household/{householdId}` and populate Context
- Components can consume via `const { household } = useHousehold()`
- Members list is used by TaskCreatePage and TaskEditPage to populate assignment dropdown

---

#### 2. Create HouseholdMemberSelect Component

**File**: `frontend/src/features/task/components/HouseholdMemberSelect.tsx`

**Intent**: Reusable dropdown for selecting a household member as the assignee.

**Contract**: 
- Props: `members: HouseholdMember[]`, `value: UUID | null`, `onChange: (id: UUID) => void`, `label?: string`, `required?: boolean`
- Renders Radix UI Select component (similar to category dropdown in S-02)
- Options show member name + email (e.g., "Alex (alex@example.com)")
- Default placeholder: "Assign to..." or pre-selected value if provided
- If `required=true`, form validation enforces a selection

---

#### 3. Update TaskCreatePage — Add Assignment Dropdown

**File**: `frontend/src/features/task/pages/TaskCreatePage.tsx`

**Intent**: Form for creating new task now includes assignment selector.

**Contract**: 
- Form state adds field: `assigneeId` (UUID | null)
- After description and category fields, add HouseholdMemberSelect dropdown
- Dropdown defaults to showing current user (the creator) selected by default (or null to force selection)
- Form validation: ensure assigneeId is selected (required)
- On submit: POST to `/api/task` with `{ title, description, category, dueDate, assigneeId }`
- On success: show toast, clear form (including assigneeId), stay on page for next task
- On error: show error toast (e.g., "Assignee not found"), keep form filled

---

#### 4. Update TaskEditPage — Add Assignment Dropdown

**File**: `frontend/src/features/task/pages/TaskEditPage.tsx`

**Intent**: Form to edit task now includes option to reassign.

**Contract**: 
- Form state adds field: `assigneeId` (UUID)
- Fetch task details on mount: GET `/api/task/{id}` — response now includes assignee.id
- Pre-fill assigneeId dropdown with current task's assignee
- HouseholdMemberSelect dropdown allows changing the assignee
- On submit: PUT `/api/task/{id}` with `{ title, description, category, dueDate, assigneeId }`
- On success: show toast, navigate back to TaskListPage
- On error: show error toast, keep form filled

---

#### 5. Update TaskCard Component — Display Assignee

**File**: `frontend/src/features/task/components/TaskCard.tsx`

**Intent**: Show assignee name on each task card in the list.

**Contract**: 
- TaskCard receives task prop (now includes `assignee: { id, name, email }`)
- Render assignee name in a clear location (e.g., below category badge or next to due date)
- Format: "Assigned to: Alex" or similar
- If assignee is the current user, optionally show in a different color or with a badge (nice-to-have, not required)

---

#### 6. Update TaskListPage — Fetch Household Members

**File**: `frontend/src/features/task/pages/TaskListPage.tsx`

**Intent**: Ensure household members are loaded before rendering task list (for consistency with S-03 scope). 

**Contract**: 
- On mount, confirm household members are loaded in context (via useHousehold())
- If members are not loaded, fetch them first (or assume they're pre-loaded by the app on login)
- When rendering TaskList, each TaskCard will have assignee data from the API response
- No changes to sorting or filtering (stay with due date ASC, all active tasks visible)
- Assignment data is consumed from the task API response, not from context

---

#### 7. Update API Client — Assignment Methods

**File**: `frontend/src/lib/api-client.ts`

**Intent**: Update API client method signatures to include assigneeId in requests and responses.

**Contract**: 
- `createTask(householdId: UUID, payload: { title, description, category, dueDate, assigneeId }): Promise<TaskResponse>`
- `updateTask(taskId: UUID, payload: { title?, description?, category?, dueDate?, assigneeId? }): Promise<TaskResponse>`
- `getTask(taskId: UUID): Promise<TaskResponse>` — response now includes assignee
- `listTasks(householdId: UUID): Promise<TaskResponse[]>` — each task includes assignee
- TaskResponse type now includes: `assignee: { id, name, email }`

---

#### 8. Update Task Type Definitions

**File**: `frontend/src/types/task.ts`

**Intent**: TypeScript types reflect the new assignee field throughout the app.

**Contract**: 
```typescript
export interface Task {
  id: UUID;
  householdId: UUID;
  title: string;
  description?: string;
  category: TaskCategory;
  dueDate: LocalDate;
  completedAt?: LocalDate;
  deletedAt?: LocalDate;
  assignee: {  // new
    id: UUID;
    name: string;
    email: string;
  };
  createdAt: DateTime;
  updatedAt: DateTime;
}

export interface HouseholdMember {
  id: UUID;
  name: string;
  email: string;
}
```

---

### Success Criteria:

#### Automated Verification:

- TypeScript compilation succeeds; no type errors related to assignee fields
- React component tests pass:
  - TaskCreatePage renders HouseholdMemberSelect dropdown
  - TaskCreatePage form submission includes assigneeId in POST payload
  - TaskEditPage pre-fills assigneeId from task response
  - TaskCard displays assignee name
  - API client methods are called with correct assigneeId parameters

#### Manual Verification:

- Navigate to TaskCreatePage → form displays assignment dropdown with list of household members
- Create task: select partner from dropdown, submit → success toast shown, task appears in list with partner's name
- Create task: don't change assignment from default (creator) → task appears with current user's name
- Navigate to TaskListPage → each card displays "Assigned to: [Name]"
- Click Edit on a task assigned to partner → form pre-fills partner's name in dropdown
- Change assignment to self and save → task card updates to show current user's name
- Try to create task without selecting an assignee (if field is required) → form validation error shown
- Both users log in to same household, view task list → both see the same assignee on the same tasks

---

## Testing Strategy

### Unit Tests

**Backend:**
- TaskService.createTask() with null assigneeId → defaults to current user
- TaskService.createTask() with invalid assigneeId → throws ValidationException
- TaskService.updateTask() with valid reassignment → task.assignee updated
- HouseholdMember validation → rejects assignees not in household

**Frontend:**
- HouseholdMemberSelect renders dropdown with household members
- TaskCreatePage form includes assigneeId in submission payload
- TaskCard displays assignee.name
- API client includes assigneeId in createTask/updateTask calls

### Integration Tests

**Backend:**
- POST `/api/task` with valid assigneeId → 200 + TaskResponse includes assignee
- POST `/api/task` without assigneeId → defaults to creator
- POST `/api/task` with invalid assigneeId → 422 ValidationException
- PUT `/api/task/{id}` to reassign → 200 + updated TaskResponse

**Frontend:**
- Create task flow: select assignee, submit → task appears in list with assignee name
- Edit task flow: change assignee, submit → list reflects update
- Cross-user visibility: both users see same assignee on same task

### Manual Testing Steps

1. **Create task with assignment:**
   - Log in as User A
   - Navigate to TaskCreatePage
   - Fill in title, description, category, due date
   - Select "User B" from assignment dropdown
   - Submit → success toast, form clears, task appears in list with "Assigned to: User B"

2. **Default assignment (creator):**
   - Create a task, don't change the assignment dropdown (leave as default)
   - Submit → task shows "Assigned to: [My Name]"

3. **Reassign task:**
   - Open an existing task assigned to User B
   - Edit the task
   - Change assignment to User A (self)
   - Submit → task card updates to show "Assigned to: [My Name]"

4. **Cross-user visibility:**
   - User A creates task and assigns to User B
   - User B logs in (same household)
   - User B's task list shows the same task with "Assigned to: [User B Name]"
   - User B edits the task and reassigns to themselves
   - User A (viewing their list) eventually sees the update (after refresh)

5. **Error handling:**
   - Try to assign task to a user from another household (if possible via API direct call) → 422 error, error toast shown
   - Try to create task without selecting assignee (if field required) → form validation error shown

## Performance Considerations

- Household members list is fetched once at app initialization and cached in React Context; no repeated fetches needed per form
- Task queries already use lazy-loading for household relationships; assignee is lazy-loaded on demand (acceptable for current scale)
- No N+1 queries: when listing tasks, assignee details are loaded via the relationship; if optimization needed later, consider JOIN FETCH

## Migration Notes

- New column `assignee_id` on task table must be added via Flyway migration
- Existing tasks (if any) have NULL assignee initially; migration must set a default (e.g., task creator if traceable, or a system-wide default user)
- Foreign key constraint: `FOREIGN KEY (assignee_id) REFERENCES household_member(id) ON DELETE CASCADE` — if a member is removed, orphaned tasks cascade-delete (discussed in "out of scope" but leave as a safeguard; can be changed to auto-reassign in future)

## References

- Roadmap item: S-03, Change ID: `task-assignment`
- Prerequisites: S-02 (basic-task-crud), F-04 (frontend-scaffold)
- Downstream: S-04 (telegram-reminder), S-06 (today-dashboard)
- Related research: `context/changes/basic-task-crud/plan.md` (backend/frontend patterns)
- Pattern reference: HouseholdMember entity from F-02 (household-schema)

---

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles.

### Phase 1: Backend Assignment API

#### Automated

- [x] 1.1 Task entity extended with assigneeId FK to HouseholdMember, assignee relationship, and getAssigneeDTO() helper — ddea742
- [x] 1.2 Task repository queries work with assignee field (no new methods needed; existing CRUD auto-supports) — ddea742
- [x] 1.3 Task DTO (CreateTaskRequest, UpdateTaskRequest, TaskResponse) updated with assigneeId and AssigneeDTO — ddea742
- [x] 1.4 TaskService.createTask() defaults assigneeId to task creator if not provided, validates assignee is in household — ddea742
- [x] 1.5 TaskService.updateTask() handles assigneeId updates with validation, helper validateAndFetchAssignee() method — ddea742
- [x] 1.6 TaskController endpoints updated to accept/return assigneeId; household isolation enforced — ddea742
- [x] 1.7 Database migration creates assignee_id column, NOT NULL constraint, FK to household_member — ddea742
- [x] 1.8 SecurityConfig protects task endpoints; only authenticated users can access — ddea742
- [x] 1.9 Unit tests for TaskService assignment logic pass (default, validation, reassignment) — ddea742
- [ ] 1.10 Integration tests for TaskController endpoints with assigneeId pass

#### Manual

- [ ] 1.11 POST `/api/task` with assigneeId field → 200 + TaskResponse includes assignee details
- [ ] 1.12 POST `/api/task` without assigneeId → defaults to creator, success
- [ ] 1.13 POST `/api/task` with invalid assigneeId → 422 ValidationException
- [ ] 1.14 PUT `/api/task/{id}` to reassign → 200 + updated TaskResponse reflects new assignee
- [ ] 1.15 List tasks → all tasks include assignee object (id, name, email)

### Phase 2: Frontend Assignment UI

#### Automated

- [x] 2.1 HouseholdContext provides household members list (fetched once at app init)
- [x] 2.2 HouseholdMemberSelect component renders dropdown with household members
- [x] 2.3 TaskCreatePage includes HouseholdMemberSelect dropdown, form includes assigneeId in submission
- [x] 2.4 TaskEditPage includes HouseholdMemberSelect, pre-fills from task.assignee
- [x] 2.5 TaskCard displays assignee name ("Assigned to: [Name]")
- [x] 2.6 TaskListPage fetches and displays tasks with assignee data
- [x] 2.7 API client methods updated: createTask() and updateTask() include assigneeId parameter
- [x] 2.8 Task TypeScript type definitions include assignee object
- [x] 2.9 React component tests pass (dropdown rendering, form submission with assigneeId, card display)

#### Manual

- [ ] 2.10 Create task: select partner from dropdown → task appears with partner's name — **Ready for manual verification**
- [ ] 2.11 Create task: leave assignment as default → task appears with current user's name — **Ready for manual verification**
- [ ] 2.12 Edit task: change assignee to self → task card updates to show current user — **Ready for manual verification**
- [ ] 2.13 Both users view same task → both see identical assignee name — **Ready for manual verification**
- [ ] 2.14 Both users view task list → assignee visible on all tasks — **Ready for manual verification**
- [ ] 2.15 Form validation: missing assignee (if required) → error shown — **Ready for manual verification**
