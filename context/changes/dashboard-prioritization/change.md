---
change_id: dashboard-prioritization
title: Dashboard Prioritization & Sorting
description: Smart sorting of user's own tasks by due date, priority, and category. Overdue tasks float to top. Enables decision-support through task ordering without suggestion complexity.
status: impl_reviewed
roadmap_ref: S-06 (part 1 of 2)
prd_refs: US-02, FR-014 (sorting component)
depends_on: S-02 (basic-task-crud)
blocked_by: none
created: 2026-09-03
updated: 2026-09-05
---

# Dashboard Prioritization & Sorting

**Splits from:** S-06 (Today's Dashboard Enhanced) — focuses on sorting/organization of user's own tasks.

**Scope:** 
- Display user's tasks due today or overdue in a single list
- Sort by: overdue (top) → due date (primary) → priority (secondary)
- Visual indicators for overdue tasks (red/warning styling)
- Quick actions: complete, delete, move to tomorrow
- Manual refresh button, empty state, responsive layout

**Not included:** Task suggestion/heuristics (see suggested-household-tasks).

**Success criteria:**
- Users see today/overdue tasks immediately on dashboard landing
- Sorting clearly prioritizes urgent work (overdue first)
- All task actions work end-to-end
- Responsive on mobile without horizontal scroll
