---
project: "done yet?"
version: 1
status: draft
created: 2026-08-26
updated: 2026-08-31
prd_version: 1
main_goal: speed
top_blocker: time
milestone_id: single-user-ai-mvp
milestone_seq: 1
milestone_status: open
---

# Roadmap: done yet?

> Derived from context/foundation/prd.md (v1) + auto-researched codebase baseline.
> Edit-in-place; archive when superseded.
> Slices below are listed in dependency order. The "At a glance" table is the index.

## Milestone

**M-1: single-user-ai-mvp** — Status: open

- **Intent:** Validate that AI-generated task suggestions reduce household management's mental load. Single user signs up, creates tasks or accepts AI suggestions, checks in daily to mark complete. Core hypothesis: 70%+ acceptance of AI suggestions = users find this useful enough to return daily.
- **Source materials:** `context/foundation/prd.md` (v1)
- **Done when:** every F-NN and S-NN below is `done`, AND production database has been cleaned (see Cleanup change below).
- **Scope anchors:**
  - FR-001, FR-002, FR-003: Registration, login, household creation (single-user onramp)
  - FR-006, FR-008, FR-010–013: Task CRUD with title, description, due date
  - FR-014: Daily "Today's tasks" dashboard
  - FR-019, FR-020: AI task generation with accept/reject/customize flow

## Vision recap

Household management is invisible mental labor — a person carries the cognitive load of remembering what needs doing. **done yet?** externalizes this through a simple task list (capture what's due when) plus AI-generated suggestions (surface what people forget to do). The system acts as an external brain, reducing mental load through guided discovery rather than blank-slate creation.

The **north star** — the smallest outcome that proves the hypothesis — is AI task generation: if users accept 70%+ of AI-suggested tasks, they find this useful enough to adopt the app.

## North star

**S-07: AI task generation** — User describes household, receives AI task suggestions, accepts/rejects/customizes them. If 70%+ acceptance rate, the core value prop works and the MVP validates.

> The **north star** is the smallest end-to-end user-visible outcome whose successful delivery proves the core product hypothesis — placed as early as Prerequisites allow because everything else only matters if this works.

## At a glance

| ID    | Change ID                  | Outcome (user can …)                                     | Prerequisites    | PRD refs       | Status   |
|-------|----------------------------|----------------------------------------------------------|------------------|-------|----------|
| F-01  | auth-scaffold              | (foundation) Register and log in with email/password     | —                | FR-001, FR-002 | done   |
| F-02  | household-schema           | (foundation) Single-user household data model (cleaned)  | F-01             | FR-003         | done   |
| F-03  | local-postgres-docker      | (foundation) Local postgres in Docker; eliminates Fly.io costs | —           | —              | done   |
| F-04  | frontend-scaffold          | (foundation) React app with routing, components, build   | —                | —              | done   |
| S-01  | new-user-setup             | Register, create household, create first task            | F-01, F-02, F-04 | US-01, FR-001–003 | done |
| S-02  | basic-task-crud            | Create, view, edit, delete task with title, description, due date | F-01, F-02, F-04 | US-02, FR-006, FR-008, FR-010–013 | done |
| S-06  | today-dashboard            | View tasks due today or overdue, sorted by due date     | S-02, F-04       | US-02, FR-014  | in-progress  |
| S-07  | ai-task-generation         | (NORTH STAR) Describe household, receive AI suggestions, accept/customize | S-02, F-04 | US-03, FR-019–020 | proposed |
| S-08  | ui-styling-updates         | UI styling is updated and refined; polished appearance   | F-04, S-01, S-02 | —              | proposed |

## Baseline

What's already in place in the codebase as of 2026-08-29 (auto-researched + user-confirmed).

- **Frontend**: Present — Vite + React 18 with TypeScript. Router, auth context, API client, component scaffolding.
- **Backend / API**: Present — Spring Boot with auth, household, and task endpoints. JWT auth via JwtAuthenticationFilter.
- **Data**: Partial — PostgreSQL with users, households, tasks tables (but also includes unnecessary HouseholdMembers and HouseholdInvitations tables from multi-user design).
- **Auth**: Present — Registration, login, JWT token management, rate limiting.
- **Deploy / Infra**: Present — Dockerfile, GitHub Actions CI/CD (Fly.io deploy).
- **Observability**: Minimal — Spring Boot defaults only, no structured logging.

## Foundations

### F-01: Auth scaffold

- **Outcome:** (foundation) Email/password registration and login working; users receive JWT tokens and can log in independently.
- **Change ID:** `auth-scaffold`
- **PRD refs:** FR-001, FR-002
- **Unlocks:** S-01 (user registration), all downstream user-facing work
- **Prerequisites:** —
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Low risk — fully implemented and tested. Spring Boot auth is rock-solid; no critical path concern.
- **Status:** done

### F-02: Household schema (cleaned)

- **Outcome:** (foundation) PostgreSQL schema with users and households tables. Single-user model: each user has one household, with full ownership and isolation enforced. Multi-user tables (HouseholdMembers, HouseholdInvitations) removed.
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

- **Outcome:** (foundation) PostgreSQL runs in Docker locally for development; eliminates Fly.io hosting costs and enables free local iteration. Docker Compose configured with schema parity to production.
- **Change ID:** `local-postgres-docker`
- **PRD refs:** —
- **Unlocks:** S-01 (local task persistence), S-02 (local task operations), S-06 (local dashboard queries), all downstream slices
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
- **Unlocks:** S-01 (registration form, household creation form), S-02 (task CRUD UI), S-06 (dashboard), S-07 (AI form)
- **Prerequisites:** —
- **Parallel with:** F-01, F-02
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Low risk — fully scaffolded and running. Radix UI + Tailwind CSS are proven lightweight choices.
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
- **Risk:** Implementation already complete. Invitation flow was multi-user feature — removed in this redesign. Registration and household creation are solid.
- **Status:** done

### S-02: Basic task CRUD

- **Outcome:** User can create a task with title, optional description, and due date. User can view all household tasks, edit details, and delete. All changes persist and are visible to the user on next login.
- **Change ID:** `basic-task-crud`
- **PRD refs:** US-02, FR-006, FR-008, FR-010–013
- **Prerequisites:** F-01 (auth), F-02 (task schema), F-04 (task forms and list view)
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Implementation complete. Forms and API endpoints are tested. Task schema (soft-delete, due-date indexing) supports dashboard queries in S-06.
- **Status:** done

### S-06: "Today" dashboard

- **Outcome:** User sees a dashboard of tasks due today or overdue, sorted by due date. Provides at-a-glance entry point and daily ritual. Updates when user completes a task (no page reload lag).
- **Change ID:** `today-dashboard`
- **PRD refs:** US-02, FR-014, NFR <500ms latency
- **Prerequisites:** S-02 (need tasks), F-04 (dashboard layout and real-time updates)
- **Parallel with:** S-07 (independent; both depend on S-02)
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Low complexity. Query performance matters (must return in <500ms per NFR). Use task table's due-date index; no denormalization needed for single user. React re-render on task completion must be efficient.
- **Status:** in-progress

### S-07: AI task generation (NORTH STAR)

- **Outcome:** User can describe their household ("we have kids and a dog and a 3-bedroom apartment"), and the AI generates 5–10 task suggestions (e.g., "schedule pest control", "replace HVAC filter"). User can accept, reject, or customize suggestions before saving as tasks. Acceptance rate >= 70% = MVP validates.
- **Change ID:** `ai-task-generation`
- **PRD refs:** US-03 (partially), FR-019–020
- **Prerequisites:** S-02 (tasks must exist; AI suggestions integrate into the task list), F-04 (household description form, suggestion display, accept/reject/customize UI)
- **Parallel with:** S-06
- **Blockers:** OpenAI API key provisioning
- **Unknowns:**
  - **Prompt engineering for 70% acceptance:** How to design the prompt so AI suggestions match user expectations? Requires iteration with real users. — Owner: dev team. Block: no (ship with a reasonable prompt; iterate post-MVP).
  - **Household description input format:** Free-form text input, structured form (checkboxes: kids, pets, house/apartment), or multi-turn Q&A? Affects AI input consistency and UX. — Owner: product. Block: no (start with free-form; simplify if time pressure peaks).
- **Risk:** **This is the north star.** If AI suggestions don't feel relevant (>30% rejection), users won't adopt the app. Prompt engineering is the critical unknown. Allocate week 3 to prompt iteration and user testing. If the API rate limit or latency becomes a blocker late in the sprint, fall back to a simpler rule-based suggestion system (e.g., checklist of common household tasks) that doesn't require a real AI call.
- **Status:** proposed

### S-08: UI styling updates

- **Outcome:** UI styling is updated and refined (colors, spacing, typography, component polish). App has a cohesive, polished visual appearance.
- **Change ID:** `ui-styling-updates`
- **PRD refs:** —
- **Prerequisites:** F-04 (frontend scaffold), S-01 (UI flows exist to style), S-02 (core UI in place)
- **Parallel with:** S-06, S-07
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Low risk — styling refinement has no impact on core functionality. Can ship as polish pass post-north-star validation.
- **Status:** proposed

## Backlog Handoff

| Roadmap ID | Change ID              | Suggested issue title                                 | Ready for `/10x-plan` | Notes |
|------------|------------------------|-------------------------------------------------------|-----------------------|-------|
| F-01       | auth-scaffold          | Auth scaffold: email/password registration & login    | no (done)             | Complete; no additional planning needed. |
| F-02       | household-schema       | Household schema cleanup: remove multi-user tables    | yes                   | Remove HouseholdMembers and HouseholdInvitations tables. Update foreign keys. Plan before S-06. |
| F-03       | local-postgres-docker  | Local postgres in Docker: eliminate Fly.io costs      | yes                   | Set up Docker Compose with postgres. Retain schema parity with production. |
| F-04       | frontend-scaffold      | Frontend scaffold: React, routing, build setup        | no (done)             | Complete; no additional planning needed. |
| S-01       | new-user-setup         | New user setup: register, create household, first task | no (done)             | Complete; removed multi-user partner invitation flow. |
| S-02       | basic-task-crud        | Basic task CRUD: create, view, edit, delete           | no (done)             | Complete. Task schema supports dashboard queries. |
| S-06       | today-dashboard        | Today dashboard: tasks due today, sorted by due date  | yes                   | Ready to plan. Depends on F-02 cleanup for clean schema. |
| S-07       | ai-task-generation     | AI task generation: suggestions, accept/reject/save   | yes (after S-06)      | North star. Plan after dashboard UI is in place; iteration expected week 3. |
| S-08       | ui-styling-updates     | UI styling updates: colors, spacing, typography       | yes (post-north-star) | Polish pass. Can run parallel to S-06/S-07; ship after core validation. |

## Open Roadmap Questions

1. **Prompt engineering for AI task suggestions:** What household characteristics (kids, pets, house type, climate, etc.) should the prompt ask for? How to phrase suggestions so 70%+ are accepted? — Owner: dev team (post-MVP iteration). Block: no.

2. **OpenAI API provisioning:** API key, rate limits, cost model. Must be in place before S-07 starts. — Owner: ops. Block: yes (S-07).

3. **Dashboard grouping strategy (deferred):** If time allows post-north-star, should dashboard group tasks by category (cleaning, shopping, etc.)? Currently just sorted by due date. — Owner: product. Block: no.

## Parked / v1.1 Work

### In Progress

- **✅ FR-009 (Recurring tasks) / Task Enhancements v1.1** — Backend phase complete; frontend enhancements (default due date, recurrence UI) complete. Implements daily/weekly/monthly task recurrence with auto-generation on completion. See `context/changes/task-enhancements-v1-1/plan.md`.
  - Phase 1 (Backend): Complete — RecurrenceFrequency enum, Task entity extension, RecurrenceService, complete-task auto-generation
  - Phase 2 (Frontend): Complete — Default due date to today, recurrence toggle/frequency selector, weekday picker, recurrence badge on cards, read-only display in edit

### Deferred

- **FR-007, S-03 (Task assignment to partner)** — Multi-user feature, explicitly deferred. Single-user MVP has no "partner" to assign tasks to. Can re-enable once multi-user sharing lands.
- **F-03, S-04, S-05 (Telegram bot and reminders)** — Explicitly deferred per PRD §Non-Goals. Web-based daily check-in is the reminder mechanism for MVP. Telegram integration follows once core product is validated.
- **Task categories/tags** — Deferred. Dashboard sorts by due date only.
- **Advanced observability** — Deferred to post-MVP. Baseline logging (stdout/stderr) only.
- **Mobile native apps** — Web-only MVP per PRD. Responsive design covers mobile browsers.
- **External integrations** — Google Calendar, Slack, etc. deferred per PRD §Non-Goals.

## Milestone History

(Empty on first generation.)

## Done

(Empty on first generation. `/10x-archive` appends entries here as slices complete.)

---

## CLEANUP WORK REQUIRED

**Before shipping**, you must resolve one critical change that removes multi-user infrastructure from the codebase:

### Cleanup change: `remove-multi-user-scaffold`

**What to remove:**
1. **Database tables:** HouseholdMembers, HouseholdInvitations (created during multi-user design phase).
2. **Entity classes:** HouseholdMember.java, HouseholdInvitation.java (Hibernate entities).
3. **API endpoints:** POST `/api/households/{id}/invite`, POST `/api/households/{id}/accept-invite`, GET `/api/households/invitations` (in HouseholdController).
4. **Service logic:** HouseholdInvitationService.java, any invitation email-sending code.
5. **Frontend components:** InvitationFlow, PartnerInvite, AcceptInvite components in React.
6. **Frontend routes:** `/household/invite`, `/invite/:token`.

**What to keep:**
- Household creation (single user creates one household per account).
- Task CRUD (no changes needed; task table doesn't reference multi-user features).
- Auth (no changes; already single-user).

**Why critical:** The multi-user tables and endpoints are dead code in this MVP; they consume schema complexity and increase maintenance burden. Removing them first keeps the codebase lean and deployment clean.

**Plan:** This is a foundation-level cleanup, not a user-facing slice. Run it as a blocking change before S-06 and S-07. Mark in the roadmap as `F-02 cleanup` or absorb into F-02 before marking F-02 `done`.

---

## Your Next Move

**► Start now:** Plan the cleanup change (`remove-multi-user-scaffold`) to remove HouseholdMembers, HouseholdInvitations, and invitation endpoints/UI.

  **Why this one first:** The cleanup unblocks F-02 completion and keeps the database schema clean before you ship. It's low complexity (removal, not addition) but critical for launch readiness. After cleanup lands, F-02 is truly `done`, and S-06/S-07 can plan with confidence.

  **Sequence after cleanup:**
  1. **Cleanup:** `remove-multi-user-scaffold` — Remove HouseholdMembers, HouseholdInvitations, invitation flows.
  2. **S-06:** `/10x-plan today-dashboard` → "Today's tasks" view (ready to plan now).
  3. **S-07:** `/10x-plan ai-task-generation` → North star validation (parallel to S-06, or after for focused iteration).

  **Why cleanup is worth a day:**
  - Reduces schema complexity and test surface.
  - Removes dead code paths (no invitation logic to maintain).
  - Gives you a clean foundation before shipping.
  - Single-user MVP is simpler to debug and scale (no multi-tenant logic).

  **Status summary:**
  - ✅ Auth scaffold (F-01): Done
  - ✅ Frontend scaffold (F-04): Done
  - ✅ Basic task CRUD (S-01, S-02): Done
  - ✅ Household schema cleanup (F-02): Done
  - ✅ Local postgres docker (F-03): Done
  - ⏳ Today dashboard (S-06): Ready — plan and implement
  - ⏳ AI task generation (S-07): Proposed — plan after dashboard; north star validation
  - ✅ Task enhancements v1.1 (Recurring tasks): Done — Phase 1 & 2 complete, pending manual verification

---

**Summary:** All foundations complete (auth, frontend, task CRUD, household schema, local postgres). Remaining MVP path: (1) build today dashboard (S-06), (2) implement AI task generation (S-07) and iterate on prompt engineering. Everything else is parked until v1.1. Ship the core 4 slices and measure the 70% AI acceptance rate.

**V1.1 in progress:** Task enhancements (recurring tasks, default due date) Phase 1-2 code-complete; pending manual verification.
