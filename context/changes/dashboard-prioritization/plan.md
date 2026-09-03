# Dashboard Prioritization & Sorting — Implementation Plan

**Split from:** S-06 (Today's Dashboard Enhanced) — part 1 of 2

**Relationship:** This plan extracts the sorting/prioritization logic from [today-dashboard/plan.md](../today-dashboard/plan.md). Both plans implement the same `TodayDashboardContainer` component but with distinct responsibilities:
- **dashboard-prioritization (this plan):** Sorting algorithm, container structure, user's own tasks, empty state, mobile responsiveness
- **suggested-household-tasks:** Domain heuristics engine, suggestion component, accept/dismiss flow, integration into container

**Implementation:** Start with dashboard-prioritization Phase 1–3, then integrate suggested-household-tasks as a new section within the container (Phase 4 of this plan becomes coordination with S-06B).

## Overview

Build a dashboard container displaying today's tasks (due today) and overdue tasks in a single merged list with visual indicators and smart sorting, enabling quick access to urgent/due work from the dashboard landing page.

## Current State Analysis

The DoneYet task management app has:
- Placeholder DashboardPage component showing household selector and task navigation links
- TaskCard reusable component for displaying individual tasks with complete/edit/delete actions
- TaskListPage showing full task list with filtering by completion status and sorting by dueDate
- API endpoint `GET /api/task` returning all active tasks sorted by dueDate, supporting filtering on the frontend
- Task model with `dueDate` (LocalDate), `completed` (boolean), `completedAt` (LocalDateTime), and soft-delete via `deletedAt`

**Gap:** No container on the dashboard displays urgent/upcoming work — users must navigate away to TaskListPage to see what's due today or overdue.

## Desired End State

Users land on the dashboard and immediately see:
- A dedicated container labeled "Today & Overdue" showing all tasks due today and all past-due tasks
- Tasks sorted with overdue items first (visually highlighted in red)
- Quick actions: mark complete, edit, delete, reschedule to tomorrow, add new task
- Manual refresh button to pull latest data
- Friendly empty state when no today/overdue tasks exist
- Responsive layout on mobile

### Verification

- Container renders on `/dashboard` page without breaking existing household selector/navigation
- Today/overdue filtering works correctly (tasks with dueDate = today or < today shown)
- All actions (complete, edit, delete, move to tomorrow, quick-add) function end-to-end
- Empty state displays when no tasks match filter
- Refresh button updates task list without full page reload
- TaskCard component properly re-renders on state changes
- Mobile responsive (container scrolls without horizontal scroll)

## What We're NOT Doing

- Implementing task suggestion/heuristics (see suggested-household-tasks change)
- Changing the dashboard layout or top-level structure (container is an addition, not a redesign)
- Adding persistent filtering or search to this container (today/overdue is fixed scope)
- Implementing recurring task auto-generation on completion (handled by backend RecurrenceService)
- Adding a test framework/test suite (deferred; component built for testability)
- Building calendar or date picker UI (today + overdue only, no date selection)
- Adding drag-drop or bulk actions (single-task actions only)
- Creating a "move to tomorrow" backend endpoint (uses existing updateTask endpoint with dueDate adjustment)

## Implementation Approach

1. **Create TodayDashboardContainer component** — a new presentational container that fetches and displays today/overdue tasks
2. **Integrate with DashboardPage** — add container to dashboard between household selector and existing task navigation link
3. **Reuse TaskCard** — leverage existing component for consistent task display
4. **Data fetching in container** — useState/useEffect pattern following TaskListPage conventions
5. **Action callbacks** — wire complete/delete to backend via existing API, implement move-to-tomorrow as updateTask call
6. **Date filtering logic** — simple comparison: today = dueDate equals today (ISO format), overdue = dueDate < today
7. **Sorting strategy** — overdue first (visually top), then by due date ascending, then by priority descending

## Key Discoveries

- TaskListPage (frontend/src/features/tasks/pages/TaskListPage.tsx:1-176) shows the established pattern for task lists: useState for loading/error/tasks, toggle filtering logic, TaskCard composition
- TaskCard (frontend/src/shared/components/TaskCard.tsx:7-13) accepts optional onComplete/onEdit/onDelete callbacks — extend with onMoveToTomorrow callback
- API response format (frontend/src/features/tasks/api.ts:5-20) includes dueDate as ISO string; filtering requires no backend changes
- Utility functions (frontend/src/features/tasks/utils/categoryUtils.ts) provide category label/color mappings — reuse for consistent styling
- No existing filter/sort utilities in codebase — implement date comparisons inline in component
- Frontend has no test framework installed (no Jest, Vitest, or RTL) — defer testing infrastructure, structure component for future tests

## Critical Implementation Details

**Date Comparison in JavaScript:** Task.dueDate comes from backend as ISO string (e.g., "2026-08-31"). Convert to Date object only for comparison; store original string for display. Use `new Date(dueDate).toDateString() === new Date().toDateString()` for "today" check to avoid timezone issues.

**Sorting Strategy:**
1. Partition tasks into overdue and today buckets
2. Sort overdue by dueDate ascending (oldest first)
3. Sort today's tasks by priority descending (high → medium → low)
4. Combine: [overdue sorted] + [today sorted]

```javascript
const today = new Date().toISOString().split('T')[0]  // YYYY-MM-DD
const overdue = tasks.filter(t => t.dueDate < today && !t.completed)
  .sort((a, b) => new Date(a.dueDate) - new Date(b.dueDate))
const todayOnly = tasks.filter(t => t.dueDate === today && !t.completed)
  .sort((a, b) => (b.priority || 0) - (a.priority || 0))
const todayAndOverdue = [...overdue, ...todayOnly]
```

**Action: Move to Tomorrow:** Reschedule task by calling `updateTask(taskId, { dueDate: tomorrow })`. Tomorrow is calculated as `new Date(new Date().getTime() + 86400000)` (one day in ms). Call existing updateTask API endpoint (no new backend changes needed).

**Empty State Conditional:** Show only when `tasks.length === 0 && !isLoading && !error`. Include inline link or button to navigate to TaskCreatePage (use React Router navigate).

---

## Phase 1: Core Container & Data Fetching

### Overview

Create the TodayDashboardContainer component with task fetching logic, date filtering, and initial task display using TaskCard. Wire up to DashboardPage.

### Changes Required

#### 1. Create TodayDashboardContainer Component

**File**: `frontend/src/features/tasks/components/TodayDashboardContainer.tsx`

**Intent**: New container component that manages today/overdue task fetching and state, handles loading/error/empty states, and composes TaskCard components with proper callbacks. This is the main display logic for today's and overdue tasks.

**Contract**: 
- Export default component: `TodayDashboardContainer` (no props required, uses useAuth for householdId)
- Internal state: `tasks[]`, `isLoading`, `error`, `lastRefresh`
- Side effect: Fetch on mount via useEffect (household context from useAuth)
- Returns JSX with: container div, header "Today & Overdue", refresh button, loading spinner, error message, task list, empty state

**Filtering and Sorting logic:**
```
const today = new Date()
const todayStr = today.toISOString().split('T')[0]  // YYYY-MM-DD
const overdue = tasks.filter(t => {
  const dueStr = t.dueDate.split('T')[0]
  return dueStr < todayStr && !t.completed
}).sort((a, b) => new Date(a.dueDate) - new Date(b.dueDate))

const todayOnly = tasks.filter(t => {
  const dueStr = t.dueDate.split('T')[0]
  return dueStr === todayStr && !t.completed
}).sort((a, b) => (b.priority || 0) - (a.priority || 0))

const todayAndOverdue = [...overdue, ...todayOnly]
```

#### 2. Integrate Container into DashboardPage

**File**: `frontend/src/features/tasks/pages/DashboardPage.tsx`

**Intent**: Add the TodayDashboardContainer to the dashboard so users see today/overdue tasks immediately upon landing.

**Contract**: Import TodayDashboardContainer and place it in the grid below the household selector, above or alongside existing task navigation link. Maintain responsive layout (grid-cols-1 md:grid-cols-2).

#### 3. Wire Task Actions (Callback Stubs)

**File**: `frontend/src/features/tasks/components/TodayDashboardContainer.tsx`

**Intent**: Define onComplete and onDelete callbacks that will be implemented in Phase 2. Stub them for now so TaskCard doesn't error.

**Contract**: Pass `onComplete={() => { /* Phase 2 */ }}` and `onDelete={() => { /* Phase 2 */ }}` to each TaskCard. These will call the API methods in Phase 2.

### Success Criteria

#### Automated Verification

- Component compiles without errors: `npm run typecheck` passes
- No linting issues: `npm run lint` passes
- DashboardPage still renders without breaking existing layout
- TaskCard component receives expected props (task, onComplete, onDelete, isLoading)

#### Manual Verification

- TodayDashboardContainer displays on `/dashboard` page
- Today's tasks appear in the list (tasks with dueDate = today)
- Overdue tasks appear (tasks with dueDate < today)
- Overdue tasks appear ABOVE today's tasks (visual ordering correct)
- Loading spinner shows briefly during fetch
- Error state displays if API fails (can test by offline mode)
- Empty state shows if no tasks match filter
- Household isolation works: switching household updates task list
- Refresh button exists and is clickable (doesn't action anything yet — Phase 2)

---

## Phase 2: User Actions & Interactions

### Overview

Implement all task actions: mark complete, edit (navigate), delete, move to tomorrow, and quick-add task. Wire callbacks to API endpoints and handle state updates.

### Changes Required

#### 1. Implement Mark Complete Action

**File**: `frontend/src/features/tasks/components/TodayDashboardContainer.tsx`

**Intent**: When user clicks complete on a TaskCard, call the backend complete endpoint and update local state to remove the task from the list.

**Contract**: 
- Implement `handleComplete(taskId)` function that calls `completeTask(taskId)` from API
- Update local state: remove completed task from list (optimistic update)
- Handle error: restore task if API fails, show error message
- Pass handler to TaskCard: `onComplete={handleComplete}`

#### 2. Implement Delete Action

**File**: `frontend/src/features/tasks/components/TodayDashboardContainer.tsx`

**Intent**: Delete task with confirmation. Reuse TaskDeleteDialog from TaskListPage or create inline confirmation.

**Contract**:
- Implement `handleDelete(taskId)` function that calls `deleteTask(taskId)` from API
- Show confirmation dialog before delete (can reuse TaskDeleteDialog component)
- Remove task from list on success, restore on error
- Pass handler to TaskCard: `onDelete={handleDelete}`

#### 3. Implement Move to Tomorrow Action

**File**: `frontend/src/features/tasks/components/TodayDashboardContainer.tsx`

**Intent**: Reschedule task to tomorrow by updating dueDate. Add button/option to TaskCard or context menu.

**Contract**:
- New callback: `onMoveToTomorrow(taskId)` — passed to TaskCard
- Calculate tomorrow: `new Date(new Date().getTime() + 86400000)` (or use day utilities if they exist)
- Call `updateTask(taskId, { dueDate: tomorrow })`
- Remove task from list (it's no longer due today/overdue)
- Handle error: restore task in list, show error message
- Button placement: TaskCard action buttons or inline options menu

#### 4. Implement Refresh Button

**File**: `frontend/src/features/tasks/components/TodayDashboardContainer.tsx`

**Intent**: Allow user to manually refresh the task list without reloading page.

**Contract**:
- Add refresh button to container header (near "Today & Overdue" title)
- onClick handler: re-fetch tasks via `fetchTasks()` function
- Show loading spinner during refresh
- Disable button while loading

### Success Criteria

#### Automated Verification

- No type errors: `npm run typecheck` passes
- Linting passes: `npm run lint` passes
- All callbacks integrate with existing API functions (no new backend code)

#### Manual Verification

- Mark complete: Click complete button → task disappears from list immediately (optimistic) → success message appears
- Mark complete undo: If error, task reappears in list with error message
- Delete: Click delete → confirmation dialog → click confirm → task removed and success message
- Delete cancel: Dialog cancel button keeps task in list
- Move to tomorrow: Task no longer shows in today/overdue after action (moved to future)
- Refresh button: Clicking refresh fetches fresh data and updates list
- All actions work while handling isLoading state (buttons disabled during request)

---

## Phase 3: Polish & Edge Cases

### Overview

Add responsive mobile layout, visual indicators for overdue tasks (styling), improve empty state UX, and error recovery.

### Changes Required

#### 1. Responsive Mobile Layout

**File**: `frontend/src/features/tasks/components/TodayDashboardContainer.tsx`

**Intent**: Ensure container doesn't overflow on mobile and scrolls cleanly within dashboard layout.

**Contract**:
- Use Tailwind responsive classes: `h-auto md:max-h-96` for height constraints
- Internal scroll: `overflow-y-auto` on task list div
- Grid/flex layout adjusts from 1 column to 2 as viewport widens (already in DashboardPage, just ensure container respects)
- Test on mobile viewport (iPhone SE, 375px width)

#### 2. Visual Indicators for Overdue Tasks

**File**: `frontend/src/features/tasks/components/TodayDashboardContainer.tsx` (or TaskCard variant)

**Intent**: Highlight overdue tasks with red styling so they visually stand out from today's tasks.

**Contract**:
- Pass additional prop to TaskCard: `isOverdue={dueDate < today}` (boolean)
- TaskCard applies conditional styling: overdue gets red border or red badge/text
- Modify TaskCard to accept `isOverdue` prop and add classes: `border-red-500 bg-red-50` or similar

#### 3. Enhanced Empty State

**File**: `frontend/src/features/tasks/components/TodayDashboardContainer.tsx`

**Intent**: Provide friendly messaging and guidance when no today/overdue tasks exist.

**Contract**:
- Message text: "All caught up! No tasks for today."
- Add button/link: "Create a new task" that navigates to TaskCreatePage
- Icon: Checkmark or thumbs-up emoji for positive reinforcement
- Styling: Center aligned, padding, subtle background color (gray-50)

#### 4. Error Recovery & Retry

**File**: `frontend/src/features/tasks/components/TodayDashboardContainer.tsx`

**Intent**: Handle API errors gracefully and provide retry option.

**Contract**:
- Show error message with context (e.g., "Failed to load tasks")
- Add "Retry" button next to error message
- onClick: re-fetch tasks
- For network errors: optionally show offline message
- Error dismissal: X button to clear message (error persists in state but isn't displayed)

### Success Criteria

#### Automated Verification

- No regressions: `npm run lint` and `npm run typecheck` pass
- TaskCard prop changes are compatible with existing TaskListPage usage

#### Manual Verification

- Mobile layout: Container scrolls within dashboard on 375px viewport (no horizontal scroll)
- Overdue visual: Overdue tasks have red styling distinct from today's tasks
- Empty state: Shows when no tasks, "Create a new task" button works (navigates to TaskCreatePage)
- Error recovery: Intentionally break API (use DevTools) → see error message → click Retry → see tasks reload
- Responsiveness: Responsive between mobile/tablet/desktop without breaking TaskCard rendering

---

## Testing Strategy

### Manual Testing Steps

1. **Setup & Navigation**
   - Navigate to `/dashboard`
   - Verify TodayDashboardContainer appears below household selector
   - Confirm household selector still works (switch household → task list updates)

2. **Today vs Overdue Filtering & Sorting**
   - Create a task with dueDate = today (via TaskCreatePage)
   - Verify it appears in TodayDashboardContainer
   - Create a task with dueDate = yesterday
   - Verify it appears with overdue styling and ABOVE today's tasks
   - Create a task with dueDate = tomorrow
   - Verify it does NOT appear in container

3. **Mark Complete Flow**
   - Click complete button on a task in the container
   - Verify task disappears immediately (optimistic update)
   - Verify "✓ Task marked complete" message appears
   - Refresh page → task should not reappear (verify backend persisted)
   - (Optional: test failure case by going offline, attempting complete, see error + restore)

4. **Delete Flow**
   - Click delete button on a task
   - Confirm deletion dialog appears
   - Click cancel → task remains, dialog closes
   - Click delete again, click confirm → task disappears with success message
   - Refresh page → task should not reappear

5. **Move to Tomorrow**
   - Click move to tomorrow (button/option on task)
   - Task disappears from container
   - Create a new task for today to verify container still shows new tasks
   - Verify moved task is gone (was with dueDate = today, now dueDate = tomorrow)

6. **Refresh Button**
   - Create a new task in another browser tab for same household
   - Container doesn't show it yet
   - Click refresh button in container
   - Verify new task appears in list

7. **Empty State**
   - Mark all today/overdue tasks as complete
   - Verify container shows "All caught up!" empty state
   - Click "Create a new task" link
   - Verify navigated to TaskCreatePage

8. **Responsive & Styling**
   - Resize browser to mobile width (375px)
   - Verify no horizontal scroll, container fits viewport
   - Verify overdue tasks have red styling distinct from today's tasks

9. **Error Scenarios**
   - Go offline, click refresh
   - Verify error message appears with retry option
   - Click retry, come back online
   - Verify tasks reload successfully

### Edge Cases to Test

- No tasks at all (empty state)
- Many tasks (50+) — verify scroll works, no performance lag
- Task completion during refresh (quick-add one task, refresh simultaneously)
- Household switch while loading (switch household mid-fetch)
- Browser back/forward navigation (should preserve state on return)
- Tasks with no priority field (default to 0 in sort)

### Notes

- No automated test suite in place yet (defer test framework)
- Build component with clean props/callbacks to ease future test migration
- Structure: TodayDashboardContainer (container) → TaskCard (presentational), TaskDeleteDialog (composed), plus utility functions (filterTodayAndOverdue, etc.)

---

## Performance Considerations

- Task list fetch on component mount: one request per household, cached in state
- Manual refresh: user-triggered, no background polling
- List rendering: TaskCard component memoization not required yet (list expected < 50 tasks typically)
- Optimistic updates: update local state before API response for perceived speed (complete, delete, move to tomorrow)

---

## References

- DashboardPage: `frontend/src/features/tasks/pages/DashboardPage.tsx:1-79`
- TaskListPage (patterns): `frontend/src/features/tasks/pages/TaskListPage.tsx:1-176`
- TaskCard component: `frontend/src/shared/components/TaskCard.tsx:7-98`
- Task API: `frontend/src/features/tasks/api.ts:43-72`
- Task type: `frontend/src/features/tasks/api.ts:5-20`
- Utility functions: `frontend/src/features/tasks/utils/categoryUtils.ts`

---

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles.

### Phase 1: Core Container & Data Fetching

#### Automated

- [ ] 1.1 Component compiles without errors (npm run typecheck)
- [ ] 1.2 No linting issues (npm run lint)
- [ ] 1.3 DashboardPage layout unchanged

#### Manual

- [ ] 1.4 TodayDashboardContainer displays on dashboard
- [ ] 1.5 Today's tasks filter and display correctly
- [ ] 1.6 Overdue tasks filter and display correctly
- [ ] 1.7 Overdue tasks appear above today's tasks
- [ ] 1.8 Loading spinner shows during fetch
- [ ] 1.9 Error state displays on API failure
- [ ] 1.10 Empty state shows when no tasks match filter
- [ ] 1.11 Household switching updates task list
- [ ] 1.12 Refresh button exists and is clickable

### Phase 2: User Actions & Interactions

#### Automated

- [ ] 2.1 No type errors (npm run typecheck)
- [ ] 2.2 Linting passes (npm run lint)

#### Manual

- [ ] 2.3 Mark complete: task disappears with success message
- [ ] 2.4 Complete undo: task reappears on error
- [ ] 2.5 Delete: confirmation dialog works, task removed on confirm
- [ ] 2.6 Move to tomorrow: task no longer in today/overdue list
- [ ] 2.7 Refresh button: fetches and updates list
- [ ] 2.8 All actions respect loading state (buttons disabled during request)

### Phase 3: Polish & Edge Cases

#### Automated

- [ ] 3.1 No regressions in lint/typecheck
- [ ] 3.2 TaskCard prop changes backward compatible with TaskListPage

#### Manual

- [ ] 3.3 Mobile layout: no horizontal scroll on 375px viewport
- [ ] 3.4 Overdue tasks visually distinct (red styling)
- [ ] 3.5 Empty state message and "Create a new task" link work
- [ ] 3.6 Error recovery: error message + retry button functional
- [ ] 3.7 Responsive design: works across mobile/tablet/desktop
- [ ] 3.8 Edge cases tested (many tasks, household switch during load, etc.)
