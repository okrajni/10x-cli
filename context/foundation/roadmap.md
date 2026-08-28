---
project: "done yet?"
version: 1
status: draft
created: 2026-08-26
updated: 2026-08-26
prd_version: 1
main_goal: speed
top_blocker: time
milestone_id: first-coordination-proof
milestone_seq: 1
milestone_status: open
---

# Roadmap: done yet?

> Derived from context/foundation/prd.md (v1) + auto-researched codebase baseline.
> Edit-in-place; archive when superseded.
> Slices below are listed in dependency order. The "At a glance" table is the index.

## Milestone

**M-1: first-coordination-proof** — Status: open

- **Intent:** Prove that the household coordination model works — two users can share task management responsibility and receive timely reminders, reducing the mental load from one person to both.
- **Source materials:** `context/foundation/prd.md` (v1)
- **Done when:** every F-NN and S-NN below is `done`.
- **Scope anchors:**
  - FR-001 through FR-005: Registration, household creation, partner invitation (the on-ramp)
  - FR-006 through FR-008, FR-010 through FR-014: Task CRUD, assignment, dashboard (the coordination layer)
  - FR-015 through FR-018: Telegram integration (the reminder channel)
  - FR-019 through FR-020: AI task generation (secondary feature, may defer if time pressure peaks)

## Vision recap

The mental load of managing a household falls disproportionately on one person — they remember what needs to be done, decide when it should be done, assign tasks, and remind others. **done yet?** transfers the coordination and reminder role to software, so household responsibilities can be shared without one partner becoming the perpetual "manager" of the home. The system acts as an external brain: rather than one partner remembering everything, the app surfaces reminders (Telegram for immediacy) and task suggestions (AI for discovery), allowing both partners to share responsibility explicitly through assignment and visibility.

## North star

**S-04: User assigns task, partner receives Telegram reminder, marks complete from bot** — This is the smallest end-to-end validation of the core value prop: reminders catch users where they already are (Telegram). Everything before it (setup, task CRUD, assignment) is a prerequisite; everything after it (2-way sync, dashboard, AI) amplifies the core mechanism. Success here proves that the Telegram-as-coordination-channel model works.

> The **north star** is the smallest end-to-end user-visible outcome whose successful delivery proves the core product hypothesis — placed as early as Prerequisites allow because everything else only matters if this works.

## At a glance

| ID    | Change ID                | Outcome (user can …)                                          | Prerequisites      | PRD refs           | Status   |
|-------|--------------------------|---------------------------------------------------------------|--------------------|--------------------|---------:|
| F-01  | auth-scaffold            | (foundation) Email/password registration & login              | —                  | FR-001, FR-002     | proposed |
| F-02  | household-schema         | (foundation) Household data model & schema established        | F-01               | FR-003, FR-004     | proposed |
| F-04  | frontend-scaffold        | (foundation) React app, routing, component scaffolding, build setup | —                 | —                  | proposed |
| S-01  | new-user-setup           | Register, create household, invite partner, partner joins     | F-01, F-02, F-04   | US-01, FR-001–005  | proposed |
| S-02  | basic-task-crud          | Create, view, edit, delete task with title, description, category, due date | F-01, F-02, F-04 | US-02 partial, FR-006, FR-008, FR-010–013 | proposed |
| S-03  | task-assignment          | Assign task to self or partner; both see assignments          | S-02, F-04         | FR-007             | proposed |
| F-03  | telegram-bot-scaffold    | (foundation) Telegram bot scaffolding, token management, webhook setup | F-01, S-03 | FR-015 | proposed |
| S-04  | telegram-reminder        | (NORTH STAR) Receive Telegram reminder at configured time     | S-03, F-03         | US-02, FR-016      | proposed |
| S-05  | telegram-completion-sync | Mark task complete from Telegram; reflected immediately in web app | S-04, F-04 | FR-017, FR-018 | proposed |
| S-06  | today-dashboard          | View tasks due today, grouped by assignee                     | S-02, S-03, F-04   | US-02 partial, FR-014 | proposed |
| S-07  | ai-task-generation       | Describe household, receive AI task suggestions, accept/save  | S-02, F-04         | US-03, FR-019–020  | proposed |

## Streams

Navigation aid — groups items that share a Prerequisites chain. Canonical ordering still lives in the dependency graph below.

| Stream | Theme                      | Chain                                  | Note                                                           |
|--------|----------------------------|----------------------------------------|----------------------------------------------------------------|
| A      | Backend Foundations        | `F-01` → `F-02`                        | Auth & household data model; unblocks all backend work.        |
| B      | Frontend Foundation        | `F-04`                                 | React scaffolding, routing, build setup; runs parallel to A.   |
| C      | Household Onramp           | `S-01` → `S-02`                        | Registration, household setup, task CRUD; joins A & B.         |
| D      | Coordination (North Star)  | `S-03` → `F-03` → `S-04` → `S-05`      | Assignment & Telegram reminders; follows C; proves value prop. |
| E      | Visibility & Discovery     | `S-06` → `S-07`                        | Dashboard & AI; parallel to D; depends on C.                   |

## Baseline

What's already in place in the codebase as of 2026-08-26 (auto-researched + user-confirmed).

- **Frontend**: Absent — landing page only, no React framework or components yet
- **Backend / API**: Partial — Spring Boot initialized, pom.xml has web dependencies, but no controllers or endpoints
- **Data**: Partial — PostgreSQL + Hibernate + Spring Data JPA configured, but no entity classes or schema
- **Auth**: Minimal — 10x-cli has CLI auth scaffolding; Spring Boot has no Spring Security or auth endpoints
- **Deploy / Infra**: Present — Dockerfile, fly.toml, GitHub Actions auto-deploy workflows all in place
- **Observability**: Absent — no logging library, error tracking, or metrics configured

## Foundations

### F-01: Auth scaffold

- **Outcome:** (foundation) Email/password registration and login endpoints working; users have session tokens and can log in independently.
- **Change ID:** `auth-scaffold`
- **PRD refs:** FR-001, FR-002
- **Unlocks:** S-01 (user registration), all downstream user-facing work (users must be authenticated)
- **Prerequisites:** —
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Auth is the foundation of everything; if delayed, all downstream work is blocked. Spring Boot has no Spring Security config yet, so this is a first-week critical path item. Once in place, S-01 can start immediately.
- **Status:** proposed

### F-02: Household schema & data model

- **Outcome:** (foundation) PostgreSQL schema established with households, users, household_members, and tasks tables. User-household relationships are enforced (isolation, multi-member households).
- **Change ID:** `household-schema`
- **PRD refs:** FR-003, FR-004, NFR (data isolation)
- **Unlocks:** S-02 (task persistence), S-03 (assignment), S-06 (dashboard queries), all downstream slices that depend on data storage
- **Prerequisites:** F-01 (need user context for household isolation)
- **Parallel with:** F-04 (frontend work can start independently; both feed S-01, S-02)
- **Blockers:** —
- **Unknowns:**
  - **Telegram bot token ownership:** Is the bot token managed by the system (one bot for all households) or per-household? Architectural choice affects auth + F-03. — Owner: product/ops. Block: yes.
- **Risk:** Schema design choice here cascades into Telegram bot architecture (F-03) and task assignment visibility (S-03). Get the household isolation model right in week 1; redesigning the schema in week 3 wastes critical time.
- **Status:** proposed

### F-04: Frontend scaffold

- **Outcome:** (foundation) React app is bootstrapped with routing (React Router), build tooling (Vite or Webpack), component scaffolding, and state management skeleton (Context API or Redux setup). Development server is running and hot-reload is functional.
- **Change ID:** `frontend-scaffold`
- **PRD refs:** —
- **Unlocks:** S-01 (registration form UI), S-02 (task CRUD forms/views), S-03 (assignment picker UI), S-05 (real-time sync UI updates), S-06 (dashboard layout), S-07 (AI form UI)
- **Prerequisites:** —
- **Parallel with:** F-01, F-02 (backend and frontend can be built in parallel; no dependencies between them until integration in S-01)
- **Blockers:** —
- **Unknowns:**
  - **Frontend state management choice:** Redux, Context API, Zustand, or Jotai? Affects component complexity and testability downstream. — Owner: development team. Block: no (can iterate, but choice in week 1 saves refactoring later).
  - **UI framework & component library:** Use headless components (Radix UI, Headless UI) or a full component library (Material-UI, Chakra)? Affects development speed vs. customization. — Owner: product/design. Block: no (ship with basic HTML + CSS first, polish after north star).
- **Risk:** Frontend setup in week 1 is critical to avoid late-stage bloat. React Router, build tool, and state management decisions made here cascade into all UI slices. Use a proven lightweight stack (React 18 + React Router + Vite + Context API) to minimize complexity during time crunch.
- **Status:** proposed

## Slices

### S-01: New user setup

- **Outcome:** User registers with email/password, creates a household, invites a partner via email, and the partner can accept the invite and log in independently. Both users see the same household on login.
- **Change ID:** `new-user-setup`
- **PRD refs:** US-01, FR-001–005
- **Prerequisites:** F-01 (auth API), F-02 (household schema), F-04 (registration form UI, email input validation, household creation form)
- **Parallel with:** S-02 (both depend on F-01, F-02, F-04; can run in parallel after foundations land)
- **Blockers:** Email infrastructure provisioning (SMTP, SendGrid, Mailgun, etc.)
- **Unknowns:** —
- **Risk:** Invitation email delivery is a hard dependency (partner must receive a joinable link). Email infrastructure (SMTP, SendGrid, etc.) must be in place by end of week 1. If email setup delays, the entire on-ramp blocks.
- **Status:** proposed

### S-02: Basic task CRUD

- **Outcome:** User can create a task with title, optional description, category (cleaning, shopping, laundry, maintenance, bills), and due date. User can view all household tasks, edit a task's details, and delete a task. All changes are persistent and visible to household members.
- **Change ID:** `basic-task-crud`
- **PRD refs:** US-02 (partial), FR-006, FR-008, FR-010–013
- **Prerequisites:** F-01 (auth API), F-02 (household schema, task table), F-04 (task form UI, task list view, category selector)
- **Parallel with:** S-01 (both depend on F-01, F-02, F-04; can run in parallel after foundations land)
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Category enum must match PRD exactly (cleaning, shopping, laundry, maintenance, bills); any mismatch breaks downstream dashboard grouping (S-06). Lock categories in schema design (F-02). Form validation and error handling in React (F-04) must align with backend (F-01).
- **Status:** proposed

### S-03: Task assignment

- **Outcome:** When a task is created or edited, the user can assign it to themselves or to their partner. Both household members see the assignee on every task. The task list reflects who owns what.
- **Change ID:** `task-assignment`
- **PRD refs:** FR-007
- **Prerequisites:** S-02 (need tasks before assigning them), F-04 (assignment dropdown/selector UI, display assignee on task cards)
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Assignment is the core coordination mechanism (PRD: "tasks are never assigned to the household — they're always assigned to a specific person"). A household member must always know who owns a task; this explicitness prevents the tragedy-of-the-commons. Ensure assignment is required, not optional.
- **Status:** proposed

### F-03: Telegram bot scaffold

- **Outcome:** (foundation) Telegram bot is initialized with a token, webhook is configured, and the system can send messages to users. User-bot communication is authenticated (bot can verify user identity).
- **Change ID:** `telegram-bot-scaffold`
- **PRD refs:** FR-015
- **Unlocks:** S-04 (reminders), S-05 (2-way sync)
- **Prerequisites:** F-01 (need user context), S-03 (need assigned tasks to know who to remind)
- **Parallel with:** —
- **Blockers:** Telegram bot token must be provisioned before work can start. Resolves the unknown from F-02 (bot ownership model).
- **Unknowns:**
  - **Telegram bot API polling vs. WebSocket behavior:** Long-polling vs. WebSocket for message delivery; retry semantics for missed reminders. Affects real-time sync reliability. — Owner: development team. Block: yes.
  - **User linking flow:** How do users link their Telegram account to the household profile? QR code, manual token entry, or OAuth? Affects UX and security. — Owner: product. Block: yes.
- **Risk:** Telegram bot token provisioning and user linking are critical path items. If not resolved by end of week 2, S-04 cannot ship. Set up a test bot and linking flow early.
- **Status:** proposed

### S-04: Telegram reminder (NORTH STAR)

- **Outcome:** When a task has a due date and a reminder time, the Telegram bot sends a notification at that time to the assigned user (via the linked Telegram account). The reminder includes the task title and due date. At least 80% of reminders deliver on time (per PRD guardrails).
- **Change ID:** `telegram-reminder`
- **PRD refs:** US-02, FR-016
- **Prerequisites:** S-03 (need to know who to remind), F-03 (bot scaffolding)
- **Parallel with:** —
- **Blockers:** Telegram bot token (blocked by F-03 resolution)
- **Unknowns:** Resolved in F-03 (polling vs. WebSocket behavior must be confirmed in F-03 before this slice starts)
- **Risk:** This is the **north star**: if reminders don't work reliably, the core value prop fails. Test reminder delivery exhaustively in week 2–3 (send 100s of reminders, verify >80% on-time delivery). If Telegram's retry semantics don't meet the 80% threshold, implement a polling fallback or escalate to queue-based delivery (e.g., Upstash).
- **Status:** proposed

### S-05: Telegram completion (2-way sync)

- **Outcome:** User can mark a task as complete directly from the Telegram bot (via bot command or button on the reminder message). The completion is immediately reflected in the web app — both users see the task marked done without page reload or delay. No sync lag.
- **Change ID:** `telegram-completion-sync`
- **PRD refs:** FR-017, FR-018
- **Prerequisites:** S-04 (reminder must exist before user can complete from bot), F-04 (React real-time UI updates to reflect task completion from bot)
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** Real-time sync mechanism (WebSocket vs. polling, latency target <500ms per NFR) must be proven in S-04 work. Carry that knowledge forward.
- **Risk:** 2-way sync failure (bot completion not reflecting in web app) creates confusion and breaks trust. Test sync latency under load in week 3; if >500ms, add a loading indicator or polling fallback. React component state updates must handle rapid bot completions without race conditions.
- **Status:** proposed

### S-06: "Today" dashboard

- **Outcome:** User sees a dashboard of tasks due today or overdue, grouped by assignee (two columns: "Today's tasks for me" | "Today's tasks for partner"). The view updates whenever a task is completed (either in app or bot). Provides at-a-glance visibility of who owns what.
- **Change ID:** `today-dashboard`
- **PRD refs:** US-02 (partial), FR-014
- **Prerequisites:** S-02 (need tasks), S-03 (need assignment to group by), F-04 (dashboard layout, grouping UI, real-time updates)
- **Parallel with:** S-04, S-05, S-07 (dashboard doesn't depend on Telegram or AI; runs parallel to those streams)
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Dashboard is the daily entry point; it must be fast (query must return in <500ms) and accurate (must reflect real-time updates from bot). Denormalize task counts / status in schema if needed to meet latency target. React component must efficiently re-render grouped task lists on updates.
- **Status:** proposed

### S-07: AI task generation

- **Outcome:** User can describe their household characteristics ("we have kids and a dog and a 3-bedroom apartment"), and the AI generates 5–10 task suggestions (e.g., "check gutters", "vet appointment for dog"). User can accept, reject, or customize suggestions before saving as tasks. At least 70% of generated tasks are accepted by users (per PRD guardrails).
- **Change ID:** `ai-task-generation`
- **PRD refs:** US-03, FR-019–020
- **Prerequisites:** S-02 (tasks must exist in the system; AI suggestions integrate into the task list), F-04 (household description form UI, suggestion list display, accept/reject/customize buttons)
- **Parallel with:** S-04, S-05, S-06 (AI work is independent of Telegram + reminder work)
- **Blockers:** OpenAI API key must be provisioned
- **Unknowns:**
  - **Prompt engineering for 70% acceptance rate:** How to design the prompt so AI suggestions match user expectations? Accuracy validation requires real users and iteration. — Owner: development team (post-MVP). Block: no.
  - **Household description input:** Free-form text, structured form, or multi-choice questionnaire? Affects UX and prompt consistency. — Owner: product. Block: no.
- **Risk:** AI suggestions are a secondary feature (PRD marks it "Secondary" success criterion). If timeline pressure peaks, this slice can defer to v1.1 without breaking the north star (task assignment + reminders). Front-load the core 4 slices; AI can be an early post-MVP feature.
- **Status:** proposed

## Backlog Handoff

| Roadmap ID | Change ID                | Suggested issue title                                    | Ready for `/10x-plan` | Notes |
|------------|--------------------------|----------------------------------------------------------|-----------------------|-------|
| F-01       | auth-scaffold            | Auth scaffold: email/password registration & login       | no                    | Unblock all downstream work |
| F-02       | household-schema         | Household data model & PostgreSQL schema                 | no                    | Unblock S-01, S-02, S-03, S-06, S-07 |
| F-04       | frontend-scaffold        | Frontend scaffold: React, routing, build setup           | no                    | Unblock all UI slices; runs parallel to F-01, F-02 |
| S-01       | new-user-setup           | New user setup: register, create household, invite       | no                    | Depends on F-01, F-02, F-04 |
| S-02       | basic-task-crud          | Basic task CRUD: create, view, edit, delete              | no                    | Depends on F-01, F-02, F-04 |
| S-03       | task-assignment          | Task assignment to self or partner                       | no                    | Depends on S-02, F-04 |
| F-03       | telegram-bot-scaffold    | Telegram bot scaffold & webhook setup                    | no                    | Blocked on Telegram token provisioning & user linking design |
| S-04       | telegram-reminder        | Telegram reminder (north star)                           | no                    | Depends on S-03, F-03 |
| S-05       | telegram-completion-sync | Telegram 2-way sync: completion from bot                 | no                    | Depends on S-04, F-04 |
| S-06       | today-dashboard          | "Today" dashboard: tasks due today, grouped by assignee  | no                    | Depends on S-02, S-03, F-04 |
| S-07       | ai-task-generation       | AI task generation: suggestions, accept/reject/customize | no                    | Depends on S-02, F-04; deferrable if time pressure peaks |

## Open Roadmap Questions

1. **Telegram bot token ownership model:** Is the bot token managed by the system (one bot for all households) or per-household? This affects authentication and deployment architecture for F-03. — Owner: product/ops. Block: F-03.

2. **User Telegram linking flow:** How do users link their Telegram account to the household profile? QR code scan, manual token entry, or OAuth-style flow? Affects UX and security design for F-03. — Owner: product. Block: F-03.

3. **Email infrastructure provider:** Which service (SMTP, SendGrid, Mailgun) will handle invitation emails for S-01? Must be provisioned before week 1 ends. — Owner: ops/infrastructure. Block: S-01.

## Parked

- **FR-009 (recurring tasks)** — Explicitly deferred to v1.1 in PRD. Reduces MVP scope; one-off task creation is sufficient for launch. Users can manually recreate repeating tasks until v1.1 ships.
- **Advanced Telegram features** — Bot can complete tasks only in MVP. Task editing, assignment changes, and due-date updates remain in web app. Rich Telegram UX deferred to v1.1.
- **Advanced observability** — Error tracking (Sentry), metrics dashboards, and log aggregation deferred to post-MVP. Baseline logging only (stdout/stderr) in scope.
- **Mobile native apps** — Web-only MVP per PRD §Non-Goals.
- **External integrations** — Google Calendar, WhatsApp, Slack integrations deferred per PRD §Non-Goals.
- **Gamification, advanced analytics, complex AI agents** — All out of scope per PRD §Non-Goals.

## Milestone History

(Empty on first generation.)

## Done

(Empty on first generation. `/10x-archive` appends entries here as slices complete.)

---

## Your Next Move

**► Parallel launch:** Start `/10x-plan auth-scaffold` on **F-01** AND `/10x-plan frontend-scaffold` on **F-04** immediately.

  **Why these two first:** F-01 and F-04 have no prerequisites and must both land in week 1 to unblock S-01 and S-02. Backend and frontend can develop in parallel. Email infrastructure provisioning is also week-1 critical path.

  **Recommended sequence after F-01 & F-04:**
  1. `/10x-plan household-schema` on **F-02** → database foundation
  2. `/10x-plan new-user-setup` on **S-01** → registration & household setup (can start parallel to F-02)
  3. `/10x-plan basic-task-crud` on **S-02** → task forms & views (parallel to S-01)
  4. `/10x-plan task-assignment` on **S-03** → assignment UI
  5. **[Resolve Telegram unknowns]** → then `/10x-plan telegram-bot-scaffold` on **F-03**
  6. `/10x-plan telegram-reminder` on **S-04** → **NORTH STAR** (validation moment)
  7. `/10x-plan telegram-completion-sync` on **S-05** → 2-way sync
  8. `/10x-plan today-dashboard` on **S-06** → dashboard (parallel to Telegram work)
  9. `/10x-plan ai-task-generation` on **S-07** → AI suggestions (defer to post-MVP if time tight)

  **Blockers to resolve before planning F-03:**
  - Telegram bot ownership model (system-wide vs. per-household)
  - User Telegram linking flow (QR code, token, or OAuth)

  **Week-1 critical path:**
  - F-01 auth endpoints shipped
  - F-04 React scaffold shipped
  - Email provider provisioned (SendGrid, Mailgun, SMTP)

---

**Ready.** The roadmap is clean: 4 foundations, 7 vertical slices, clear north-star path. Parallelism opportunity: F-01+F-04+F-02 can overlap. Speed goal means ship the must-have path; parked items offer scope trim if weeks 4–5 get tight.
