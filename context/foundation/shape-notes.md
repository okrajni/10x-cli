---
project: "Is it done?"
context_type: greenfield
checkpoint:
  current_phase: 8
  phases_completed: [1, 2, 3, 4, 5, 6, 7]
  frs_drafted: 20
  quality_check_status: accepted
updated: 2026-08-19 — SHAPING COMPLETE
---

# Is it done? – Shaping Notes

## Vision & Problem Statement

The mental load of managing a household falls disproportionately on one person — they remember what needs to be done, decide when it should be done, assign tasks, and remind others about unfinished chores. This invisible labor fractures shared responsibility.

**Is it done?** transfers the coordination and reminder role to software, so household responsibilities can be shared without one partner becoming the "manager" of the home.

## User & Persona

**Primary persona**: A person in a shared household (partner or cohabiting relationship) who carries the bulk of household task management and coordination.

## Access Control

**Auth model**: Email + password registration and login. No OAuth or external identity provider.

**User roles**: Flat model. All household members see all tasks and can complete any task. No role-based visibility or permission layers in the MVP.

**Household model**: A user creates a household and invites a partner via email. Both users log in independently and share the same household context.

## Current System

N/A (greenfield)

## Success Criteria

### Primary
A user creates a household, invites a partner, assigns tasks with due dates, and both users can complete tasks either through the web app or a Telegram bot. Task reminders are delivered on time, and the web app reflects Telegram completions immediately.

### Secondary
AI-assisted task generation helps users discover relevant household tasks based on their household characteristics (children, pets, house/apartment).

### Guardrails
- At least 80% of created tasks can be completed via the app or Telegram
- Telegram reminders deliver reliably for tasks with a configured reminder time
- New user setup (account + household + first task) takes < 5 minutes
- At least 70% of AI-generated tasks are accepted by users

### Timeline acknowledgment
Acknowledged on 2026-08-19: 4–5 week MVP requires sustained after-hours dedication (evenings/weekends over 4–5 weeks). User accepted and understands the commitment. Full feature set locked: auth, household management, task CRUD, assignment, due dates, recurring tasks, dashboard, Telegram bot (2-way sync), AI task generation, and task categories.

## Functional Requirements

### Authentication & Household Setup
- FR-001: User can register with email and password. Priority: must-have
  > Socrates: No counter-argument. Core to MVP — users need separate accounts to distinguish assignments and track contributions.
- FR-002: User can log in with email and password. Priority: must-have
  > Socrates: No counter-argument. Standard auth flow; essential for returning users.
- FR-003: User can create a household and assign a name. Priority: must-have
  > Socrates: No counter-argument. Household is the organizational unit; naming it is essential.
- FR-004: User can invite a partner to a household by email. Priority: must-have
  > Socrates: No counter-argument. Email invitation is the glue that connects both users.
- FR-005: Invited user receives email invitation and can accept/join the household. Priority: must-have
  > Socrates: No counter-argument. Completing the invite flow is non-negotiable.

### Task Management
- FR-006: Household member can create a task with a title and optional description. Priority: must-have
  > Socrates: No counter-argument. This is the core action — creating tasks is how people externalize the mental load.
- FR-007: Household member can assign a task to themselves or their partner. Priority: must-have
  > Socrates: No counter-argument. Assignment is how coordination happens — without it, both people are responsible for everything.
- FR-008: Household member can set a due date on a task. Priority: must-have
  > Socrates: No counter-argument. Due dates are how you schedule the future; essential for task tracking.
- FR-009: Household member can mark a task as recurring (daily, weekly, monthly, etc.). Priority: nice-to-have
  > Socrates: Deferred to v1.1. Reduces MVP complexity; one-off task creation is sufficient for initial launch. Users can manually recreate repeating tasks until the feature ships.
- FR-010: Household member can add a task to a category (cleaning, shopping, laundry, maintenance, bills). Priority: must-have
  > Socrates: No counter-argument. Categories organize the dashboard and make the task list readable at a glance.
- FR-011: Household member can edit a task (title, description, due date, category, assignee). Priority: must-have
  > Socrates: No counter-argument. Users will need to change task details after creation. Essential for usability.
- FR-012: Household member can mark a task as completed. Priority: must-have
  > Socrates: No counter-argument. Marking complete provides relief and shared visibility of progress.
- FR-013: Household member can delete a task. Priority: must-have
  > Socrates: No counter-argument. Users need an escape hatch if they create something by mistake.

### Dashboard & Views
- FR-014: Household member can view "Today's tasks" — tasks due today or overdue, grouped by assignee. Priority: must-have
  > Socrates: No counter-argument. The "Today" dashboard is the daily entry point — it shows what matters right now and who owns what.

### Telegram Integration
- FR-015: Household member can link their Telegram account to their household profile. Priority: must-have
  > Socrates: No counter-argument. Linking Telegram is the bridge between app and reminder bot.
- FR-016: Telegram bot sends reminder notifications for tasks with a configured reminder time. Priority: must-have
  > Socrates: No counter-argument. This is the core value — getting reminders where people already are (Telegram).
- FR-017: Household member can mark a task as completed directly from Telegram (via bot command or button). Priority: must-have
  > Socrates: No counter-argument. 2-way sync is essential — completing from Telegram is a UX win that reinforces the tool's usefulness.
- FR-018: Task completion via Telegram is immediately reflected in the web app. Priority: must-have
  > Socrates: No counter-argument. Real-time sync prevents confusion; both users see the same state instantly.

### AI Task Generation
- FR-019: Household member can ask the AI to generate household task suggestions based on household characteristics (e.g., "We have kids, a dog, and a 3-bedroom apartment"). Priority: must-have
  > Socrates: No counter-argument. AI task generation is the differentiator — it surfaces what people forget to do. This is where the mental-load reduction happens.
- FR-020: AI-generated suggestions are presented for the user to accept, reject, or customize before saving as tasks. Priority: must-have
  > Socrates: No counter-argument. User control over AI suggestions is essential — acceptance rate is a success metric (70% target). Users must feel agency.

## User Stories

### US-01: New User Setup & Household Creation
```
Given a new user visits the app,
When they register with email and password, create a household, and send an invitation to their partner,
Then both users can log in, see the same household, and create their first task.
```

### US-02: Task Assignment & Telegram Reminder
```
Given a household member assigns a task to their partner with a due date,
When the reminder time arrives,
Then the partner receives a Telegram reminder and can mark it complete from the bot.
```

### US-03: AI Task Generation
```
Given a household member describes their household (e.g., "kids and a pet"),
When they ask for AI-generated task suggestions,
Then they receive a curated list of relevant tasks and can save any to the household.
```

## Business Logic

**Core Rule**: The app prompts users with AI-suggested tasks based on household context, reducing the cognitive load of remembering what needs doing.

The system acts as an external brain for household management. Rather than one partner remembering everything, the app surfaces timely task suggestions ("you might need to check the gutters" or "the dog needs a vet appointment"). Users accept or reject suggestions, building their task list through discovery rather than blank-slate creation. This distribution of cognitive labor — AI suggests, human confirms — allows both partners to share responsibility without one becoming the perpetual "household manager."

The app enforces a simple rule: **tasks are never assigned to "the household" — they're always assigned to a specific person**. This explicitness prevents the tragedy-of-the-commons where both partners assume the other will do it. Assignment forces commitment.

Reminders bridge the gap between intention and action. A task created and assigned is worthless if forgotten. Telegram reminders catch users where they already are, removing the friction of opening a separate app.

## Non-Functional Requirements

- **Reliability**: Telegram reminders deliver reliably for tasks with a configured reminder time. Task completion via Telegram is immediately reflected in the web app. No data loss or sync failures.
- **Response time**: Task creation, assignment, and completion operations feel instant (<500ms user-perceived latency). No spinners or perceptible delays.
- **Data privacy**: User household data is isolated per household. Members of a household can only see their own household's tasks and cannot view other households.

## Non-Goals

- **Mobile applications** (native iOS/Android) — web-only for MVP
- **Advanced analytics & statistics** — no "mental load scores" or relationship metrics
- **Gamification** — no points, leaderboards, or badges
- **External integrations** — no Google Calendar, WhatsApp, or Slack integration
- **Complex AI agents** — no autonomous task management or predictive scheduling
- **Advanced notification channels** — no SMS or push notifications (Telegram only)
- **Multi-family household sharing** — one household per account in MVP
- **Advanced permission and role management** — flat user model (no admin/member tiers)

## Product Framing

**Product Type**: Web app (browser-based, both partners access via the same URL)

**Target Scale**: Dozens of households (50–100 people) in the first 6 months. Initial audience: friends, family, close circles. Product Hunt launch or social sharing for broader reach would be post-MVP.

**Timeline**: Hard deadline: **End of September 2026** (~6 weeks from 2026-08-19). 
- Estimated MVP work: 4–5 weeks of after-hours dedication
- Timeline acknowledged: 2026-08-19 (user committed to sustained effort over 4–5 weeks)
- Delivery window: mid-September for testing/polish buffer before end-of-month launch

**Context**: After-hours work (evenings/weekends).

## Open Questions

*(to be filled during cross-check)*

## Forward: tech-stack

*(to be filled during stack selection phase)*
