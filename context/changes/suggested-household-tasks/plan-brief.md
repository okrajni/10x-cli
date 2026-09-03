# Suggested Household Tasks — Brief Plan

**Focus:** Define domain-specific heuristic tasks. Implement suggestion engine. Surface suggestions on dashboard.

## Unknowns Resolved

From roadmap Open Question #1:
- ✓ **Domain heuristics ruleset:** 20–30 common household tasks (seasonal, frequency-based, category-based)
- ✓ **Suggestion logic:** Simple rules: frequency (every N days since last completion), seasonal (current month/quarter), category defaults
- ✓ **Task suggestion display:** Show 3–5 suggestions below user's own tasks, user can accept/dismiss
- ✓ **Validation:** Track acceptance rate (target: ≥50% acceptance = heuristics work)

## Core Changes

1. **Backend: Domain Task Database + Heuristics Engine**
   - Define 20–30 household tasks in code (category, frequency, seasonal flags)
   - Categories: cleaning (weekly/bi-weekly), maintenance (6-month/yearly), shopping, seasonal (spring/fall), errands
   - Suggestion API endpoint: `GET /api/suggestion?householdId=...` → returns 3–5 tasks based on rules
   - Rules: "HVAC filter every 6 months", "laundry every 2 days", "deep clean every 3 months", etc.

2. **Frontend: Suggestions Component**
   - New `SuggestedTasksList` component to display 3–5 suggestions in dashboard
   - Each suggestion shows: task title, frequency (e.g., "every 2 days"), accept button
   - Accept action: calls `createTask()` with suggested task details + marks suggestion as accepted
   - Dismiss action: hides suggestion (logged for analytics)

3. **Dashboard Integration**
   - Add suggestions below user's own today/overdue tasks in container
   - Clear visual separation: "Suggested Tasks" section with distinct styling
   - No interleaving: user's tasks first (sorted by priority), then suggestions

4. **Analytics / Logging**
   - Backend: log when suggestion is accepted/dismissed
   - Frontend: track acceptance rate (accepted / (accepted + dismissed))
   - Use for post-MVP validation: if ≥50%, heuristics work; if <50%, iterate rules

## Phases

| Phase | Scope | Blockers |
|-------|-------|----------|
| 1 | Define domain tasks (20–30 items) + backend API | None (can stub frontend) |
| 2 | Implement suggestion engine + heuristic rules | None |
| 3 | Frontend component + accept/dismiss flow | None |
| 4 | Analytics logging + validation metrics | None |

## Success Criteria

- 20–30 household tasks defined with categories and frequencies
- Suggestion engine returns 3–5 tasks based on rules (not AI)
- Dashboard displays suggestions with accept/dismiss buttons
- Accepted suggestion becomes new task in user's list
- Acceptance rate tracked (post-MVP: ≥50% validates hypothesis)

## Timeline

4–5 days (4 phases, 1 day each + buffer)

## Dependencies

- Runs in parallel with dashboard-prioritization
- Needs suggestion component integrated into same dashboard container (agreed layout: user's tasks → suggestions)
