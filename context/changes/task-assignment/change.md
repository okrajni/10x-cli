---
change_id: task-assignment
title: Task Assignment
description: Users can assign tasks to themselves or their partner; both see the assignee on every task
type: slice
parent_roadmap: S-03
status: implemented
priority: must-have
created: 2026-08-29
updated: 2026-08-29
owner: development team
---

# Task Assignment (S-03)

## Summary

Implement task assignment so users can assign tasks to themselves or their partner. Both household members see the assignee on every task, enabling shared responsibility and coordination. Assignment is foundational for Telegram reminders (S-04) which need to know who to remind.

## User Story

**Given** a household member creates or edits a task,
**When** they assign it to themselves or their partner,
**Then** both users see the assignee on the task, and the assigned person is notified if Telegram reminders are enabled (S-04).

## Acceptance Criteria

- [x] User can assign a task to themselves or their partner at creation time
- [x] User can reassign a task to a different household member via edit
- [x] Both household members see the same assignee on the same task
- [x] Assignment is required (every task must have an assignee)
- [x] Backend validates assignee is a member of the household (422 error if invalid)
- [x] TaskCard displays assignee name on task list
- [x] Default assignee is the task creator (if not explicitly picked)

## Dependencies

- **Prerequisite**: S-02 (basic-task-crud) — tasks must exist before assigning
- **Prerequisite**: F-04 (frontend-scaffold) — assignment UI components
- **Unlocks**: S-04 (telegram-reminder) — reminders need to know who to remind
- **Unlocks**: S-06 (today-dashboard) — dashboard groups tasks by assignee

## Design Decisions

| Decision | Choice | Why | Source |
|----------|--------|-----|--------|
| Assignment requirement | Required (NOT NULL) | Core rule: tasks are always assigned to a specific person, never to the household | PRD / User questions |
| Reassignment rights | Any household member can reassign | Supports shared responsibility; any user can take a task from their partner | User questions |
| Default assignee | Task creator | Sensible UX; person who creates task usually intends to do it | User questions |
| Dropdown visibility | At task creation | One-step workflow; task immediately assigned | User questions |
| Display format | Name only ("Assigned to: Alex") | Clear and familiar; reinforces accountability | User questions |
| Grouping | Single sorted list | Keep MVP simple; S-06 dashboard will handle grouping | User questions |
| Reminder targeting | Assignee only (not both) | Direct accountability; reduces notification noise | User questions |
| Auto-reassign on member removal | Yes (auto-reassign to remaining member) | No orphaned tasks; cleaner data state | User questions |
| Invalid assignee handling | Strict validation + 422 error | Prevents bad data; explicit feedback to user | User questions |

## Testing Plan

- Unit tests: TaskService defaults and validation logic
- Integration tests: API endpoints with assigneeId field, validation errors
- Manual: Create task with assignment, reassign, cross-user visibility, error cases

## Open Risks & Assumptions

- **Race conditions on reassignment**: If both users reassign the same task simultaneously, last write wins (eventual consistency). Acceptable for MVP given low edit frequency. May need locking or version control in v1.1 if conflicts arise.
- **Member removal handling**: Auto-reassign logic is assumed to be handled by a separate user-management feature. If not, orphaned tasks will exist. Consider adding a safeguard (CASCADE DELETE or explicit migration) in database setup.
- **Household members context**: Frontend assumes household members are pre-loaded in React Context. If not available, add a fetch call to TaskCreatePage.

## Related Files

- Full plan: `plan.md`
- Related (prereq): `context/changes/basic-task-crud/plan.md`
- Related (downstream): `context/changes/telegram-reminder/plan.md` (TODO: will exist after S-03)
