# Dashboard Prioritization — Brief Plan

**Focus:** Sort user's own tasks by due date + priority. Display today/overdue with clear visual hierarchy.

## Unknowns Resolved

From roadmap Open Question #2:
- ✓ **Task prioritization strategy:** Sort by overdue (top) → due date (primary) → priority (secondary)
- ✓ **Sorting logic:** Overdue float to top, visually distinct (red styling)
- ✓ **Empty state & UX:** Show "All caught up!" when no tasks, link to create new task

## Core Changes

1. **TodayDashboardContainer component**
   - Fetch user's tasks for the household
   - Filter: dueDate ≤ today AND NOT completed
   - Sort: `[overdue] → [today] sorted by priority`
   - Display: TaskCard list + refresh button + empty state

2. **DashboardPage integration**
   - Add container between household selector and navigation

3. **Task actions** (Phase 2)
   - Mark complete (remove from list)
   - Delete with confirmation
   - Move to tomorrow (reschedule via updateTask)
   - Refresh button (re-fetch)

4. **Styling**
   - Overdue tasks: red border / red badge
   - Today tasks: default styling
   - Empty state: centered, friendly message

## Phases

| Phase | Scope | Blockers |
|-------|-------|----------|
| 1 | Container + data fetching + filtering | None |
| 2 | User actions (complete/delete/move/refresh) | None |
| 3 | Polish (mobile responsive, visual hierarchy) | None |

## Success Criteria

- Container renders on `/dashboard` immediately upon landing
- Overdue tasks appear above today's tasks
- All actions (complete, delete, move) work without page reload
- Responsive on mobile (375px)
- No horizontal scroll

## Timeline

3–4 days (3 phases, 1 day each + buffer)
