---
change_id: suggested-household-tasks
title: Suggested Household Tasks (Domain Heuristics)
description: Domain-specific heuristic engine suggesting 3–5 household tasks based on seasonal patterns, frequency rules, and category analysis. User can accept, customize, or dismiss suggestions.
status: ready-to-plan
roadmap_ref: S-06 (part 2 of 2)
prd_refs: US-02, FR-014 (suggestion component)
depends_on: S-02 (basic-task-crud), dashboard-prioritization (for shared container/UI)
blocked_by: none
created: 2026-09-03
---

# Suggested Household Tasks — Domain Heuristics

**Splits from:** S-06 (Today's Dashboard Enhanced) — focuses on task suggestion logic via domain heuristics (no AI).

**Scope:**
- Define 20–30 common household tasks organized by category (cleaning, maintenance, shopping, seasonal, errands)
- Implement heuristics: frequency-based (every N days), seasonal (spring/fall), manual overrides
- Backend suggestion engine: receives household context, returns candidate tasks
- Frontend display: show 3–5 suggestions below user's own tasks in dashboard
- User interaction: accept (adds to task list), dismiss (remember choice), customize (edit before accepting)
- Track user choices: log acceptance rate to validate heuristics work

**Not included:** Smart sorting of user's own tasks (see dashboard-prioritization).

**Success criteria:**
- 20–30 domain tasks hardcoded in backend with rules
- Suggestion engine returns 3–5 relevant tasks per household state
- Users see suggestions on dashboard without breaking prioritization layout
- Acceptance rate ≥50% signals heuristics are working
