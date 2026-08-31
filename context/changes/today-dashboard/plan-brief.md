# Today Dashboard Container — Plan Brief

> Full plan: `context/changes/today-dashboard/plan.md`

## What & Why

Build a dashboard container showing today's tasks and overdue tasks in a single merged list. Users currently must navigate away from the dashboard to see what's due today or overdue — this container brings urgent work front-and-center on landing, enabling quick access and faster task management without leaving the dashboard.

## Starting Point

The dashboard is a placeholder with household selector and navigation link to TaskListPage. Today/overdue tasks exist in the backend (dueDate field, soft delete pattern) and can already be filtered on the frontend. TaskCard component is reusable and ready to display tasks. No container yet aggregates today's urgent work.

## Desired End State

Users land on `/dashboard` and see a "Today & Overdue" container showing:
- All tasks due today + all overdue tasks in one scrollable list
- Overdue tasks sorted first with red highlighting
- Quick actions: mark complete, edit, delete, move to tomorrow, add new task
- Manual refresh button to pull latest data
- Friendly empty state when no urgent tasks
- Works responsively on mobile without horizontal scroll

## Key Decisions Made

| Decision | Choice | Why | Source |
|----------|--------|-----|--------|
| Date logic | Simple comparison (today = dueDate equals today, overdue = past) | Straightforward, matches common patterns. No grace period needed. | Plan |
| Display structure | Merged list with visual indicators | Compact, single scroll, overdue highlighted in red. Matches TaskListPage pattern. | Plan |
| Data scope | Show all, scroll if needed | Users see complete picture without limits or clicks. | Plan |
| User actions | Mark complete, Edit, Delete, Move to tomorrow, Quick-add | Full task control from dashboard. Move to tomorrow is key for deferring urgent work. | Plan |
| Data refresh | Load on mount + manual refresh button | User controls when to sync. No background polling overhead. Matches TaskListPage. | Plan |
| Empty state | Friendly message + quick-add CTA | Guides user action, celebrates progress. | Plan |
| Testing | Defer framework, build for testability | Move fast now. Component structure supports future Jest/RTL migration. | Plan |

## Scope

**In scope:**
- TodayDashboardContainer component with filtering and display logic
- Integration into DashboardPage (below household selector)
- Reuse of TaskCard component and existing API endpoints
- Task actions: complete, edit, delete, move to tomorrow, quick-add
- Manual refresh button
- Responsive layout
- Empty state and error handling

**Out of scope:**
- Dashboard layout redesign (container is addition only)
- Persistent filtering or search
- Calendar/date picker UI
- Test framework setup (component built for testability)
- Drag-drop or bulk actions
- Recurring task auto-generation (handled by backend)
- New backend endpoints (uses existing `/api/task` and updateTask)

## Architecture / Approach

```
DashboardPage
├── Household Selector
├── TodayDashboardContainer (NEW)
│   ├── Fetch tasks on mount (GET /api/task)
│   ├── Filter by date: today and overdue
│   ├── Sort: overdue first
│   ├── Display as list of TaskCard components
│   ├── Handle actions:
│   │   ├── Complete → API call → remove from list
│   │   ├── Delete → confirm dialog → API call → remove
│   │   ├── Move to tomorrow → updateTask with tomorrow's date → remove
│   │   ├── Quick-add → createTask → add to list
│   │   └── Refresh → re-fetch tasks
│   └── Empty state + error recovery
└── Task Navigation Link (existing)
```

**Key patterns reused:**
- useState/useEffect data fetching (from TaskListPage)
- TaskCard component for display
- Callback pattern for actions (onComplete, onDelete, etc.)
- Utility functions for category colors/labels (from categoryUtils)
- TailwindCSS + clsx for styling (existing pattern)

## Phases at a Glance

| Phase | What it delivers | Key risk |
|-------|------------------|----------|
| 1. Core Container & Data Fetching | TodayDashboardContainer component with filtering logic; integration into DashboardPage; task fetching on mount; loading/error/empty states | Filtering logic correctness; date comparison timezone handling |
| 2. User Actions & Interactions | Mark complete, delete, move to tomorrow, quick-add; refresh button; optimistic updates; error recovery | Complex state management if actions overlap; quick-add form scope creep |
| 3. Polish & Edge Cases | Mobile responsive layout; overdue visual indicators; error retry; tested edge cases | Mobile responsiveness edge cases; performance with many tasks |

**Prerequisites:** No blocking dependencies. Reuses existing components and APIs.

**Estimated effort:** ~2 sessions across 3 phases (Phase 1: 45min, Phase 2: 60min, Phase 3: 45min). Straightforward composition and state management; main complexity is action handling and mobile testing.

## Open Risks & Assumptions

- **Date filtering timezone:** Comparing ISO strings (from API) with JavaScript Date object. Assumption: backend always uses UTC; frontend uses `toISOString().split('T')[0]` for today. If backend uses local timezone, filtering may off by one day.
- **Quick-add scope:** Form implementation could expand into full inline edit — recommend MVP: title + category only, defer advanced fields.
- **No test coverage:** Manual testing only until test framework added. Risk of regressions on refactor.
- **TaskCard extension:** Adding `isOverdue` prop to TaskCard for styling — backward compatible, but verify TaskListPage still works.

## Success Criteria (Summary)

- Dashboard loads with TodayDashboardContainer showing today/overdue tasks correctly filtered and sorted
- All user actions (complete, delete, move, quick-add, refresh) work end-to-end with optimistic updates and error handling
- Mobile responsive layout works without horizontal scroll
- Empty state and error recovery tested and working as designed
