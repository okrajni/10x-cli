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
> Last updated 2026-09-03: S-07 (AI task generation) moved from deferred to in-progress.
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
| S-06  | today-dashboard-enhanced   | View tasks due today/overdue + suggested household tasks, sorted by priority and due date | S-02, F-04 | US-02, FR-014 | in-progress |
| S-07  | ai-task-generation         | AI-powered task suggestions using Gemini API; accept/customize/dismiss UI | S-02, F-04 | FR-019–FR-020 | in-progress |
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

### S-07: AI Task Generation

- **Outcome:** AI-powered task suggestions using Google Gemini API. System generates contextual household task recommendations based on user's existing tasks, household state, and seasonal/frequency heuristics. User can accept, customize, or dismiss suggestions. Accept adds task to their list; dismiss trains the model's suggestion quality.
- **Change ID:** `ai-task-generation`
- **PRD refs:** FR-019, FR-020
- **Prerequisites:** S-02 (need task context), F-04 (suggestion UI)
- **Parallel with:** S-06, S-08
- **Blockers:** —
- **Unknowns:**
  - **Prompt engineering for household domain:** What context should be fed to Gemini? How to structure prompts for high-quality suggestions? — Owner: implementation. Block: no (iterate in-flight).
  - **Suggestion ranking strategy:** How to rank/order suggestions? By confidence, by urgency (seasonal vs. routine), by user preference history? — Owner: implementation. Block: no.
- **Risk:** Medium. LLM quality varies; suggestions may hallucinate or be off-domain. Mitigation: pair with domain heuristics (S-06) as fallback; user can always dismiss poor suggestions. Cost control via API rate limits and caching.
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
| S-06       | today-dashboard-enhanced | Today dashboard: enhanced sorting, domain-heuristic suggestions | yes (in-progress) | **NORTH STAR.** Core decision-support via smart sorting + heuristic suggestions. |
| S-07       | ai-task-generation     | AI task generation: Gemini API integration + suggest/accept/dismiss UI | yes (in-progress) | Running parallel to S-06. Enhances suggestions beyond domain heuristics. |
| S-08       | ui-styling-updates     | UI styling updates: colors, spacing, typography        | yes (in-progress) | Polish pass. Can ship in parallel with S-06/S-07. |

## Open Roadmap Questions

1. **Domain heuristics for task suggestions (S-06):** What household tasks should the system suggest, and by what logic? Examples:
   - Seasonal (HVAC filter, gutters, AC maintenance every 6-12 months)
   - Frequency-based (laundry every 2-3 days, dishes daily, weekly cleaning, etc.)
   - Category-based (cleaning, maintenance, shopping, repairs, etc.)
   - What weights / prioritization should these have?
   
   Owner: product / user research. Block: yes (S-06). Status: **In progress.**

2. **Task prioritization and sorting in the dashboard (S-06):** How should the today dashboard order tasks?
   - Strictly by due date (current plan)?
   - By due date + priority + category?
   - Should overdue tasks float to the very top?
   - How should suggested tasks (from heuristics and AI) integrate with user's own tasks?
   
   Owner: product / design. Block: yes (S-06). Status: **In progress.**

3. **Hybrid suggestion strategy:** How should domain heuristics (S-06) and AI suggestions (S-07) interact?
   - Show both, rank by confidence?
   - Use heuristics as fallback when AI is low-confidence?
   - De-duplicate overlapping suggestions?
   
   Owner: implementation. Block: no (both can iterate independently).

4. **Suggestion acceptance rate target:** What's the validation threshold for "suggestions are useful"? If users accept 50%+ of suggestions, is that enough, or do we need higher signal?
   
   Owner: product. Block: no (ships regardless; iteration lever post-MVP).

## Parked

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

**Initial pivot to domain-specific approach (v0 → v1):**
- Planned S-07 (ai-task-generation) removal in favor of domain-specific heuristics.
- North star shifted from "70% AI acceptance" to "enhanced dashboard with smart sorting."

**Current state (v1 in-progress):**
- **S-06 (Today's Dashboard)** in-progress: implementing smart sorting + domain-heuristic suggestions.
- **S-07 (AI task generation)** now in-progress: Gemini API integration for AI-powered task suggestions.
- **Hybrid approach:** Both paths running in parallel. Domain heuristics provide immediate baseline; AI suggestions layer on for enhancement.

**Investment areas:**
- **Frontend:** Dashboard logic (sorting, grouping, suggestion display) + AI suggestion UI (accept/customize/dismiss).
- **Backend:** Task-suggestion service combining both rule-based heuristics and Gemini API integration.
- **Data:** Task table extended to track suggestion acceptance/dismissal metrics.
- **Auth / Infra:** No changes.

---

## Current Status & Next Steps

**S-06, S-07, S-08 all in progress** as of 2026-09-03. Parallel development:

- **S-06 (Today's Dashboard)** — Implementing smart sorting + domain-heuristic suggestions. Unknowns: finalize heuristic rules, sorting/grouping strategy. Continue implementation or iterate if logic feels off.
- **S-07 (AI Task Generation)** — Gemini API integration + suggest/accept/dismiss UI. Risk: prompt engineering, cost control. Iterate in-flight based on suggestion quality.
- **S-08 (UI Styling)** — Visual polish in parallel with core features.

**Validation criteria:**
- Users return to dashboard daily (engagement signal).
- Users accept 50%+ of suggestions (heuristics are useful).
- System responds to suggestion actions (no lag on accept/dismiss).

**Unknowns to resolve (non-blocking iteration):**
1. Domain heuristics rule set (Open Roadmap Questions #1).
2. Dashboard sorting strategy (Open Roadmap Questions #2).
3. Hybrid suggestion ranking (Open Roadmap Questions #3).

**Post-MVP path:**
- If heuristics work, keep simple for stability. Layer AI only if needed.
- If AI suggestions outperform heuristics, prioritize Gemini output.
- Continue v1.1 roadmap based on user feedback and adoption metrics.

---

**Summary:** MVP path is running now: (1) complete today dashboard + AI integration in parallel, (2) measure suggestion adoption and user engagement, (3) iterate based on data. Validate the decision-support hypothesis with both heuristic and AI-backed suggestions.
