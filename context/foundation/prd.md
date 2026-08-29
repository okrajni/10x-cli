---
project: "done yet?"
version: 1
status: draft
created: 2026-08-29
context_type: greenfield
product_type: web-app
target_scale:
  users: small
  qps: low
  data_volume: small
timeline_budget:
  mvp_weeks: 3
  hard_deadline: null
  after_hours_only: true
---

# done yet? – Product Requirements Document

## Vision & Problem Statement

Household management is invisible mental labor. A person has to remember what needs doing, when it's due, and stay on top of dozens of recurring tasks (cleaning, maintenance, bills, shopping, repairs). Without external structure, tasks slip through the cracks.

**done yet?** externalizes household task management into a simple, durable system powered by AI-generated task suggestions. Users get prompted with relevant household tasks based on their household characteristics, accept or reject suggestions, and mark tasks complete as they go. The app becomes the household's shared brain, freeing the user from the cognitive load of remembering what needs doing.

## User & Persona

**Primary persona:** A person who manages a household (alone or with others) and wants to externalize the mental load of remembering what needs to be done. They track tasks with due dates and check in daily on what's due today. Age 25–55, digitally literate, values simplicity over features.

## Success Criteria

### Primary
- A user can register, create a household, add a task with a due date, view today's tasks, and mark tasks complete — all within 5 minutes of first use.
- Users can ask the AI to generate household task suggestions, accept/customize them, and save them as tasks.
- Users return daily to check their task list and feel the app reduces their mental load of remembering household duties.
- At least 70% of AI-generated task suggestions are accepted by users (acceptance rate guardrail).

### Secondary
- Task creation, editing, and completion operations feel instant (<500ms perceived latency).

### Guardrails
- No data loss. All tasks and user data persist reliably across sessions.
- User household data is isolated per user. Each user only sees their own household's tasks.
- App is usable on desktop and mobile web browsers (responsive design).
- AI task suggestions are relevant and actionable (70% acceptance rate target).

## User Stories

### US-01: New User Setup & First Task
- **Given** a new user visits the app
- **When** they register with email and password and create a household
- **Then** they can immediately create their first task with a due date

### US-02: Daily Task Review
- **Given** a user logs into the app
- **When** they view the "Today's tasks" dashboard
- **Then** they see all tasks due today or overdue, sorted by due date, and can mark any complete

### US-03: Task Lifecycle
- **Given** a user has created a task
- **When** they need to change details (due date, description) or delete it
- **Then** they can edit or delete the task at any time

### US-04: AI Task Generation
- **Given** a user describes their household (e.g., "kids and a dog, 3-bedroom apartment")
- **When** they ask the AI for task suggestions
- **Then** they receive a curated list of relevant household tasks and can accept, reject, or customize them before saving

## Functional Requirements

### Authentication & Household Setup
- FR-001: User can register with email and password. Priority: must-have
  > Socrates: Core to MVP — users need accounts to return and see their tasks.
- FR-002: User can log in with email and password. Priority: must-have
  > Socrates: Essential for returning users.
- FR-003: User can create a household and assign a name. Priority: must-have
  > Socrates: Household is the organizational unit; naming it sets context for the task list.

### Task Management
- FR-006: User can create a task with a title and optional description. Priority: must-have
  > Socrates: Core action — creating tasks is how users externalize the mental load.
- FR-008: User can set a due date on a task. Priority: must-have
  > Socrates: Due dates are how you schedule the future; essential for task tracking.
- FR-011: User can edit a task (title, description, due date). Priority: must-have
  > Socrates: Users will need to change task details after creation.
- FR-012: User can mark a task as completed. Priority: must-have
  > Socrates: Marking complete provides relief and progress visibility.
- FR-013: User can delete a task. Priority: must-have
  > Socrates: Users need an escape hatch if they create something by mistake.

### Dashboard & Views
- FR-014: User can view "Today's tasks" — tasks due today or overdue, sorted by due date. Priority: must-have
  > Socrates: The "Today" dashboard is the daily entry point — it shows what matters right now.

### AI Task Generation
- FR-019: User can ask the AI to generate household task suggestions based on household characteristics (e.g., "We have kids, a dog, and a 3-bedroom apartment"). Priority: must-have
  > Socrates: AI task generation is the differentiator — it surfaces what people forget to do. This is where the mental-load reduction happens.
- FR-020: AI-generated suggestions are presented for the user to accept, reject, or customize before saving as tasks. Priority: must-have
  > Socrates: User control over AI suggestions is essential — acceptance rate is a success metric (70% target). Users must feel agency.

## Non-Functional Requirements

- **Reliability:** No data loss. Tasks persist reliably across sessions. User data is stored securely.
- **Response time:** Task creation, editing, and completion operations feel instant (<500ms user-perceived latency).
- **Data privacy:** User household data is isolated per user. Each user only sees their own household's tasks.
- **Accessibility:** App is usable on desktop and mobile web browsers (responsive design).

## Business Logic

**Core Rule:** The app is an external brain for household task management, powered by AI-generated task suggestions. Instead of remembering dozens of duties, the user externalizes them into a durable list. The AI surfaces relevant household tasks based on the user's household characteristics (kids, pets, house type), reducing the cognitive work of discovery. Users accept or reject suggestions, building their task list through guided discovery rather than blank-slate creation.

The system treats tasks as a daily priority list, not a backlog. The "Today" view shows only what's due today or overdue, creating a manageable scope and daily ritual. AI suggestions bridge the gap between intention and action — they surface what people forget, allowing users to stay on top of household duties without the mental load of remembering everything themselves.

In MVP, tasks are created and managed by a single user. AI task generation is the core differentiator. Future versions will support multi-user household sharing.

## Access Control

Single-user MVP. One account = one household. Each user authenticates with email and password, can create a household, and manages their own tasks. No multi-user sharing in MVP.

## Non-Goals

- **Multi-user household sharing** — single-user only (deferred to v1.1)
- **Reminders** (Telegram bot, push notifications, in-app notifications) — deferred to v1.1
- **Recurring tasks** — users manually create repeating tasks (deferred to v1.1)
- **Task categories or tags** — deferred to v1.1
- **Mobile applications** (native iOS/Android) — web-only for MVP
- **Advanced analytics** — no mental load scores or metrics
- **Gamification** — no points, leaderboards, or badges
- **External integrations** — no Google Calendar, Slack, or other services

## Open Questions

*(None at this time — input was well-shaped and complete.)*
