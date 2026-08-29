# Task Assignment — Plan Brief

> Full plan: `context/changes/task-assignment/plan.md`
> Roadmap: S-03 in `context/foundation/roadmap.md`
> Frame/research: None (greenfield for this slice)

## What & Why

Add task assignment to the done yet? coordination app. Users assign tasks to themselves or their partner at creation time, and both household members see the assignee on every task. This enforces the core rule: **"tasks are never assigned to the household — they're always assigned to a specific person."** Shared visibility of who owns what reduces confusion and enables both partners to coordinate without one becoming the perpetual "household manager."

Assignment is also foundational for downstream features: Telegram reminders (S-04) need to know who to remind, and the "Today" dashboard (S-06) groups tasks by assignee.

## Starting Point

- Task CRUD (create, read, update, delete) is complete (S-02)
- Tasks have title, description, category (5 fixed types), due date, completion tracking
- Backend enforces household isolation on all queries (each household sees only its own tasks)
- Frontend has TaskCreatePage, TaskEditPage, TaskListPage with established patterns (Radix UI components, React Context, API client)
- HouseholdMember entity exists (from F-02) with user name and email

**Missing**: 
- Task entity lacks assigneeId field (FK to HouseholdMember)
- No assignment dropdown in TaskCreatePage or TaskEditPage
- No assignee name displayed on TaskCard

## Desired End State

After this plan, users can:

1. **Create a task with assignment** — form dropdown shows household members (default: self). Pick self or partner. Submit → task created with assignee.
2. **Reassign a task** — edit form includes same dropdown. Change to different member, save → assignee updated immediately.
3. **See who owns what** — task list displays assignee name on each card ("Assigned to: Alex"). Both users in the household see identical assignee info.
4. **Have reminders target the right person** — when S-04 (Telegram reminders) lands, the system knows exactly who to remind because assignment is explicit and validated.

## Key Decisions Made

| Decision | Choice | Why | Source |
|----------|--------|-----|--------|
| **Assignment requirement** | Required (NOT NULL in DB) | Enforces "tasks assigned to person, not household" rule; prevents unassigned state | PRD core rule / User confirmation |
| **Who can reassign** | Any household member | Supports shared responsibility (e.g., "I can take this from you"); flat permission model | User confirmation |
| **Dropdown placement** | At task creation | One-step workflow; task assigned immediately | User confirmation |
| **Display format** | Name only ("Assigned to: Alex") | Clear accountability, familiar format | User confirmation |
| **List view grouping** | Single sorted list, no grouping | Keep MVP tight; S-06 dashboard will group by assignee | User confirmation |
| **Telegram reminder recipient** | Assignee only (not both) | Direct accountability, reduced noise | User confirmation |
| **Auto-reassign on member removal** | Yes, to remaining member | Cleaner data state; no orphaned tasks | User confirmation |
| **Invalid assignee handling** | Strict validation → 422 error | Prevents bad data, explicit feedback | User confirmation |

## Scope

**In scope:**
- Task entity extended with assigneeId FK and lazy-loaded assignee relationship
- Assignment dropdown in TaskCreatePage (with household members list)
- Assignment dropdown in TaskEditPage for reassignment
- Assignee name displayed on TaskCard
- Default assignee: task creator (if not explicitly picked)
- Backend validation: assignee must be a member of the household
- Database migration: new assignee_id column with NOT NULL constraint

**Out of scope:**
- Grouping by assignee in task list (S-06 dashboard handles this)
- Permissions or role-based edit restrictions (both users can edit any task)
- Member removal handling / orphaned task cleanup (user-management feature, assumed separate)
- Read-only mode for partner's tasks (both users have full edit rights)
- Reassignment audit trail or history
- Bulk reassignment operations

## Architecture / Approach

**Data Model:**
- Task entity gains `assigneeId` (UUID, NOT NULL, FK to HouseholdMember)
- Relationship: `Task.assignee` → HouseholdMember (lazy-loaded)
- All task queries validate household membership; if user is not in the household that owns the task, they cannot access it

**Backend Changes:**
- Task entity: add assigneeId field and assignee relationship + getAssigneeDTO() helper
- TaskDTO: CreateTaskRequest and UpdateTaskRequest now accept assigneeId; TaskResponse includes assignee object (id, name, email)
- TaskService: createTask() defaults assigneeId to task creator if not provided; all mutations validate assignee is in household
- TaskController: endpoints accept/return assigneeId; household isolation enforced at service layer
- Database: Flyway migration adds assignee_id column with NOT NULL and FK constraint

**Frontend Changes:**
- HouseholdContext: cache household members list (fetched once at app init)
- HouseholdMemberSelect component: reusable dropdown for selecting assignee
- TaskCreatePage: add assignment dropdown (default to current user)
- TaskEditPage: add assignment dropdown for reassignment
- TaskCard: display assignee name ("Assigned to: [Name]")
- API client: updateTask() and createTask() signatures include assigneeId parameter
- TypeScript types: Task type includes assignee object

**Error Handling:**
- Invalid assigneeId (not in household): backend returns 422 ValidationException → frontend shows error toast
- Reassignment by both users simultaneously: eventual consistency (last write wins)

## Phases at a Glance

| Phase | What it delivers | Key risk |
|-------|------------------|----------|
| 1. Backend Assignment API | Task entity with assigneeId, service/controller logic, database migration | Migration blocker if existing tasks need default assignee |
| 2. Frontend Assignment UI | Dropdown in forms, display on cards, API integration | Household members context must be pre-loaded or fetched |

**Prerequisites:** S-02 (basic-task-crud complete), F-04 (frontend scaffold with Radix UI)
**Estimated effort:** ~2 sessions across 2 phases (1 for backend, 1 for frontend)

## Open Risks & Assumptions

- **Race condition on simultaneous reassignment**: If both partners reassign the same task at the same time, last write wins. Acceptable for MVP (low edit frequency). May need optimistic locking or versioning in v1.1.
- **Member removal & orphaned tasks**: Auto-reassign is assumed handled by a separate user-management feature. If not, tasks assigned to removed members will be orphaned (considered out-of-scope for S-03; safeguard via CASCADE DELETE in schema).
- **Household members availability**: Frontend assumes household members are available in React Context from app initialization. If not pre-loaded, TaskCreatePage must fetch them.

## Success Criteria (Summary)

- [x] User creates task with assignment dropdown; default is self, can pick partner
- [x] Reassignment works: edit form allows changing assignee, updates immediately
- [x] Cross-user visibility: both users see identical assignee on same task
- [x] Backend validates assignee is in household; invalid returns 422 error
- [x] TaskCard displays assignee name clearly on task list
