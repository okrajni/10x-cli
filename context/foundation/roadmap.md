---
project: "done yet?"
version: 1
status: draft
created: 2026-08-26
updated: 2026-09-03
prd_version: 1
main_goal: speed
top_blocker: decisions
milestone_id: single-user-decision-system-mvp
milestone_seq: 1
milestone_status: open
---

# Roadmap: done yet?

> Derived from user description + auto-researched codebase baseline.
> Last updated 2026-09-03: Refocused on domain-specific decision system. S-07 (AI) parked to v1.1 for 1-week MVP sprint.
> Edit-in-place; archive when superseded.
> Slices below are listed in dependency order. The "At a glance" table is the index.

## Milestone

**M-1: single-user-decision-system-mvp** — Status: open

- **Intent:** Validate that a domain-specific decision system (smart sorting, heuristic-based task suggestions, user-driven prioritization) reduces household management's mental load without requiring AI integration. Single user signs up, creates tasks, views an intelligent today dashboard, and can accept/customize suggested tasks. Core hypothesis: a simple, rule-based system that helps users decide what needs attention is more maintainable and faster to ship than AI-generated suggestions.
- **Source materials:** User description (scope anchors below)
- **Done when:** every F-NN and S-NN below is `done`.
- **Scope anchors:**
  - **MS-01:** Shift from AI-agent approach to domain-specific decision system
  - **MS-02:** Application actively helps user decide what needs attention (not just CRUD)
  - **MS-03:** Suggest useful household tasks based on domain heuristics and decision logic

## Vision recap

Household management is invisible mental labor — a person carries the cognitive load of remembering what needs doing. **done yet?** externalizes this through a simple task list (capture what's due when) plus intelligent task prioritization and suggestion (surface what people forget to do via smart defaults, not AI). The system acts as an external brain, reducing mental load through guided discovery powered by domain knowledge rather than AI-agent complexity.

The **north star** — the smallest outcome that proves the hypothesis — is the today dashboard: when users open the app, they see their tasks smartly organized (by due date, priority, category, etc.), with optional task suggestions that surface common household needs they might have overlooked. If users find this simple heuristic-based system useful, the MVP validates without AI overhead.

## North star

**S-06: Today's Dashboard (Enhanced)** — User sees tasks due today or overdue, intelligently sorted and grouped. System surfaces suggested household tasks based on domain heuristics (common chores, seasonal maintenance, frequency-based patterns). This proves the decision-support value without requiring AI integration.

> A reader-facing one-liner explaining what "north star" means here: the smallest end-to-end slice whose successful delivery would prove the core product hypothesis — placed as early as Prerequisites allow because everything else only matters if this works.

## At a glance

| ID    | Change ID                  | Outcome (user can …)                                     | Prerequisites    | PRD refs       | Status   |
|-------|----------------------------|----------------------------------------------------------|------------------|-------|----------|
| F-01  | auth-scaffold              | (foundation) Register and log in with email/password     | —                | FR-001, FR-002 | done   |
| F-02  | household-schema           | (foundation) Single-user household data model (cleaned)  | F-01             | FR-003         | done   |
| F-03  | local-postgres-docker      | (foundation) Local postgres in Docker                    | —                | —              | done   |
| F-04  | frontend-scaffold          | (foundation) React app with routing, components, build   | —                | —              | done   |
| S-01  | new-user-setup             | Register, create household, create first task            | F-01, F-02, F-04 | US-01, FR-001–003 | done |
| S-02  | basic-task-crud            | Create, view, edit, delete task with title, description, due date | F-01, F-02, F-04 | US-02, FR-006, FR-008, FR-010–013 | done |
| S-06A | dashboard-prioritization   | User's tasks due today/overdue, intelligently sorted by due date and priority | S-02, F-04 | US-02, FR-014 (sorting) | ready-to-plan |
| S-06B | suggested-household-tasks  | Suggested household tasks via domain heuristics (3–5 suggestions); user can accept or dismiss | S-02, F-04 | US-02, FR-014 (suggestions) | ready-to-plan |
| S-08  | ui-styling-updates         | UI styling is updated and refined; polished appearance   | F-04, S-01, S-02 | —              | in-progress |

## Baseline

What's already in place in the codebase as of 2026-09-03 (auto-researched + user-confirmed).

- **Frontend**: Present — Vite + React 18 with TypeScript. Router, auth context, API client, component scaffolding.
- **Backend / API**: Present — Spring Boot with auth, household, and task endpoints. JWT auth via JwtAuthenticationFilter.
- **Data**: Partial — PostgreSQL with users, households, tasks tables (cleaned of multi-user schema).
- **Auth**: Present — Registration, login, JWT token management, rate limiting.
- **Deploy / Infra**: Present — Dockerfile, GitHub Actions CI/CD (Fly.io deploy), local Docker Compose.
- **Observability**: Minimal — Spring Boot defaults only, no structured logging.

## Foundations

### F-01: Auth scaffold

- **Outcome:** (foundation) Email/password registration and login working; users receive JWT tokens and can log in independently.
- **Change ID:** `auth-scaffold`
- **PRD refs:** FR-001, FR-002
- **Unlocks:** S-01 (user registration), all downstream user-facing work
- **Prerequisites:** —
- **Parallel with:** F-02, F-03, F-04
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Low risk — fully implemented and tested. Spring Boot auth is rock-solid.
- **Status:** done

### F-02: Household schema (cleaned)

- **Outcome:** (foundation) PostgreSQL schema with users and households tables. Single-user model: each user has one household, with full ownership and isolation enforced.
- **Change ID:** `household-schema`
- **PRD refs:** FR-003, NFR (data isolation)
- **Unlocks:** S-01 (household creation), S-02 (task persistence), S-06 (dashboard queries)
- **Prerequisites:** F-01
- **Parallel with:** F-04
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Low — schema cleanup complete, multi-user dead code removed.
- **Status:** done

### F-03: Local postgres docker setup

- **Outcome:** (foundation) PostgreSQL runs in Docker locally for development; enables free local iteration. Docker Compose configured with schema parity to production.
- **Change ID:** `local-postgres-docker`
- **PRD refs:** —
- **Unlocks:** S-01 (local task persistence), S-02 (local task operations), S-06 (dashboard queries), all downstream slices
- **Prerequisites:** —
- **Parallel with:** F-01, F-02, F-04
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Low — straightforward Docker Compose setup, schema parity verified.
- **Status:** done

### F-04: Frontend scaffold

- **Outcome:** (foundation) React app bootstrapped with routing (React Router), build tooling (Vite), component scaffolding, and auth context. Dev server runs with hot-reload.
- **Change ID:** `frontend-scaffold`
- **PRD refs:** —
- **Unlocks:** S-01 (registration form, household creation form), S-02 (task CRUD UI), S-06 (dashboard), S-08 (styling)
- **Prerequisites:** —
- **Parallel with:** F-01, F-02
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Low — fully scaffolded and running. Radix UI + Tailwind CSS proven.
- **Status:** done

## Slices

### S-01: New user setup

- **Outcome:** User registers with email/password, creates a household with a name, and immediately can create their first task. Single-user flow; no partner invitation.
- **Change ID:** `new-user-setup`
- **PRD refs:** US-01, FR-001–003
- **Prerequisites:** F-01 (auth API), F-02 (household schema), F-04 (registration form, household creation form)
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Implementation already complete and tested.
- **Status:** done

### S-02: Basic task CRUD

- **Outcome:** User can create a task with title, optional description, and due date. User can view all household tasks, edit details, and delete. All changes persist and are visible on next login.
- **Change ID:** `basic-task-crud`
- **PRD refs:** US-02, FR-006, FR-008, FR-010–013
- **Prerequisites:** F-01 (auth), F-02 (task schema), F-04 (task forms and list view)
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Implementation complete. Forms and API endpoints tested. Task schema supports dashboard queries in S-06.
- **Status:** done

### S-06: Today's Dashboard (Enhanced)

- **Outcome:** User sees a dashboard of tasks due today or overdue, intelligently sorted by due date and priority. System surfaces 3–5 suggested household tasks based on domain heuristics (common chores, seasonal maintenance, frequency-based patterns) that the user can accept, customize, or dismiss. Provides at-a-glance entry point and daily ritual. Updates when user completes a task (no page reload lag).
- **Change ID:** `today-dashboard-enhanced`
- **PRD refs:** US-02, FR-014, NFR <500ms latency
- **Prerequisites:** S-02 (need tasks), F-04 (dashboard layout and real-time updates)
- **Parallel with:** S-08
- **Blockers:** —
- **Unknowns:**
  - **Domain heuristics for task suggestions:** What household tasks should the system suggest? Seasonal (HVAC filter every 6 months), frequency-based (laundry every 2 days), category-based (cleaning, maintenance, errands)? How to weight them? — Owner: product/user research. Block: yes (this drives the dashboard logic).
  - **Task prioritization / sorting strategy:** Should dashboard sort by due date only, or by priority + due date + category? Should overdue tasks float to top? How should suggested tasks integrate with user's own tasks? — Owner: product. Block: yes.
- **Risk:** Medium complexity. Query performance matters (must return in <500ms per NFR). Decision-support logic (heuristics for suggestions, sorting/grouping) is the new critical path instead of prompt engineering. User testing on the heuristic suggestions is the validation lever. If domain heuristics feel off, iterate quickly; the system is fully under your control (unlike prompt engineering).
- **Status:** in-progress

### S-08: UI styling updates

- **Outcome:** UI styling is updated and refined (colors, spacing, typography, component polish). App has a cohesive, polished visual appearance.
- **Change ID:** `ui-styling-updates`
- **PRD refs:** —
- **Prerequisites:** F-04 (frontend scaffold), S-01 (UI flows exist to style), S-02 (core UI in place)
- **Parallel with:** S-06
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Low — styling refinement has no impact on core functionality. Can ship as polish pass post-north-star validation.
- **Status:** in-progress

## Backlog Handoff

| Roadmap ID | Change ID                  | Suggested issue title                                     | Ready for `/10x-plan` | Notes |
|------------|------------------------|-------------------------------------------------------|-----------------------|-------|
| F-01       | auth-scaffold          | Auth scaffold: email/password registration & login    | no (done)             | Complete; no additional planning needed. |
| F-02       | household-schema       | Household schema: single-user model (cleaned)          | no (done)             | Complete; multi-user tables removed. |
| F-03       | local-postgres-docker  | Local postgres in Docker for development               | no (done)             | Complete; Docker Compose running. |
| F-04       | frontend-scaffold      | Frontend scaffold: React, routing, build setup         | no (done)             | Complete; dev server running. |
| S-01       | new-user-setup         | New user setup: register, create household, first task | no (done)             | Complete. |
| S-02       | basic-task-crud        | Basic task CRUD: create, view, edit, delete            | no (done)             | Complete; tested. |
| S-06A      | dashboard-prioritization | Today dashboard: smart sorting (due date, priority, overdue first) | yes (ready-to-plan) | **NORTH STAR part 1.** Core decision-support via sorting. 3–4 days. |
| S-06B      | suggested-household-tasks | Domain-heuristic suggestions (3–5 tasks); user can accept/dismiss | yes (ready-to-plan) | **NORTH STAR part 2.** Task suggestions without AI. 4–5 days. Runs parallel with S-06A. |
| S-08       | ui-styling-updates     | UI styling updates: colors, spacing, typography        | yes (in-progress) | Polish pass. Run in parallel with S-06A & S-06B. |
| S-07       | ai-task-generation     | AI task generation (deferred to v1.1)                    | no (parked)       | Revisit post-launch based on heuristic validation. |

## Open Roadmap Questions

**Status:** S-06 has been split into S-06A (dashboard-prioritization) and S-06B (suggested-household-tasks) to simplify scope. Both tasks resolve the blocking questions below.

1. **Task prioritization and sorting in dashboard (S-06A):** ✓ **Resolved** — Sort by overdue (top) → due date (primary) → priority (secondary). Implementation plan: [dashboard-prioritization/plan.md](../changes/dashboard-prioritization/plan.md)

2. **Domain heuristics ruleset for task suggestions (S-06B):** ✓ **Resolved** — 20–30 common household tasks with rules (seasonal, frequency-based, category-based). Implementation plan: [suggested-household-tasks/plan.md](../changes/suggested-household-tasks/plan.md)

3. **Suggestion acceptance & metrics (post-MVP):** Target ≥50% acceptance rate to validate heuristics work. Block: no (ships regardless; iteration lever post-MVP).

## Parked

- **S-07 (AI task generation)** — Deferred to v1.1. MVP focuses on domain-specific heuristics (S-06) only. Reason: 1-week timeline, reduced complexity, keep system maintainable. AI enhancement revisit post-launch based on user feedback and heuristic effectiveness.
- **FR-007 (Task assignment to partner), S-03, S-04, S-05 (Telegram bot and reminders)** — Multi-user feature, explicitly deferred. Single-user MVP has no "partner" to assign tasks to.
- **Task categories/tags** — Deferred. Dashboard sorts by due date + priority only.
- **Advanced observability** — Deferred to post-MVP. Baseline logging (stdout/stderr) only.
- **Mobile native apps** — Web-only MVP per PRD. Responsive design covers mobile browsers.
- **External integrations** — Google Calendar, Slack, etc. deferred.

## Milestone History

(Empty on first generation. `/10x-archive` appends entries here as slices complete.)

## Done

(Empty on first generation. `/10x-archive` appends entries here as slices are archived.)

---

## Key Changes from v0 → v1

**Pivot to domain-specific decision system (v0 → v1):**
- **v0:** AI task generation (S-07) as north star. Challenge: complex prompt engineering, external dependencies, 70% acceptance threshold.
- **v1:** Enhanced dashboard with domain-specific heuristics (S-06) as north star. Benefit: simpler, faster to ship, fully under your control, easier to iterate.

**Current commitment (1-week MVP):**
- **S-06 (Today's Dashboard)** — Smart sorting + domain-heuristic task suggestions. Validate core value: users find heuristics useful enough to return daily.
- **S-07 (AI task generation)** — Parked to v1.1. Revisit only if domain heuristics work and you want to layer on AI.
- **S-08 (UI Styling)** — Polish pass in parallel.

**Investment areas (MVP scope):**
- **Frontend:** Dashboard logic (sorting, grouping, suggestion display). Keep UI simple: show suggested tasks, user can accept.
- **Backend:** Simple heuristics engine (rule-based task suggestions, no ML). ~10-20 common household tasks, hardcoded rules.
- **Data:** Task table tracks suggestion acceptance (optional; log first).
- **Auth / Infra:** No changes.

---

## Current Status & Next Steps

**1-week sprint to MVP** (S-06A + S-06B + S-08 focus, S-07 parked):

**Week 1 priorities:**
1. **S-06A (Dashboard Prioritization)** — CRITICAL PATH (3–4 days)
   - Create TodayDashboardContainer component
   - Implement smart sorting: overdue first → due date → priority
   - Wire task actions: complete, delete, move to tomorrow, refresh
   - Visual hierarchy: clear distinction between overdue and today's tasks
   
2. **S-06B (Suggested Household Tasks)** — CRITICAL PATH (4–5 days, runs parallel with S-06A)
   - Define 20–30 domain household tasks with frequencies/seasons
   - Implement heuristics scoring engine (backend)
   - Create suggestion API endpoint
   - Build SuggestedTasksList component + accept/dismiss flow
   - Analytics: log acceptance/dismissal for post-MVP validation
   
3. **Dashboard Integration** (both S-06A and S-06B together)
   - Show user's sorted tasks above suggested tasks
   - Accept suggestion → task added to user's list
   - Clear visual separation
   
4. **S-08 (UI Styling)** — PARALLEL
   - Polish dashboard appearance
   - Color, spacing, typography refinement
   
5. **Launch ready:**
   - Smart sorting working (S-06A)
   - Domain heuristics working (S-06B)
   - No AI complexity
   - Users see useful suggestions

**Validation criteria (post-launch):**
- Users return to dashboard daily (engagement signal).
- Users accept 50%+ of suggestions (heuristics are useful).
- System responds instantly to actions (no lag).

**Task dependencies:**
- S-06A and S-06B can run in parallel (different concerns)
- S-06B depends on S-06A's container for dashboard integration
- Both depend on S-02 (task CRUD + API) — already done

**Post-launch path (v1.1):**
- Measure heuristic effectiveness via user adoption & acceptance rate.
- If heuristics work well, keep simple; no AI needed.
- If users want more sophistication, revisit S-07 (AI suggestions) based on validated demand.

---

**Summary:** 1-week sprint: build S-06A (dashboard prioritization) + S-06B (domain heuristics) in parallel + S-08 (polish). Split S-06 into two focused tasks to reduce complexity per task and enable parallel work. Validate the decision-support hypothesis with simple, rule-based sorting + suggestions. Ship lean, iterate based on real user behavior. AI considered only post-launch if heuristics alone aren't enough.
