# New User Setup — Plan Brief

> Full plan: `context/changes/new-user-setup/plan.md`  
> Roadmap: `context/foundation/roadmap.md` (S-01)  
> PRD: `context/foundation/prd.md` (US-01, FR-001–005)

## What & Why

**What**: Implement the complete household onboarding flow: user registers, creates a household, invites a partner via email, partner accepts invitation. Both users see the same household on login.

**Why**: S-01 is the critical on-ramp for the done yet? MVP. After this lands, all downstream features (task CRUD, assignment, Telegram reminders, dashboard) are unblocked. The guardrail from the PRD is < 5 minutes for new user setup → first task; this plan covers registration → household creation → invitation acceptance.

## Starting Point

**Foundation is ready:** Auth API (`/auth/register`, `/auth/login`) is complete with JWT tokens. Household and HouseholdInvitation entities are defined with repositories. Frontend scaffolding (React Router, auth context, form components) is done. RegisterPage is a stub waiting for implementation.

**What's missing:** No household creation endpoint, no invitation sending endpoint, no email service. The on-ramp flow doesn't exist yet.

## Desired End State

After this plan is complete, two users can:

1. First user registers → logged in with JWT token
2. First user creates household (e.g., "Smith Household") → household is created, user is member
3. First user invites partner by email → invitation link is generated with 24-hour token expiry
4. Partner receives invitation email with link to `/invitation/accept/{token}`
5. Partner clicks link:
   - If partner already has account → joins household, sees "Smith Household" on next login
   - If partner doesn't have account → registration form with email pre-filled, auto-joins after signup
6. **North Star Verified**: Both users see the same household, can see each other's assignments, can complete tasks

Guardrail check: registration → household creation → sending invite = ~2 minutes. Accepting invite from email = ~1 minute. Total < 5 minutes. ✓

## Key Decisions Made

| Decision | Choice | Why | Source |
|----------|--------|-----|--------|
| Invitation tokens | URL-embedded token + 24h expiry | Matches JWT pattern in codebase; simple UX. Tokens expire in 24h for urgency. | User decision |
| Email provider | Mock/stub for MVP, real provider in v1.1 | Unblocks S-01 immediately without ops work. Design is testable; swappable. | User decision |
| Household creation requirement | Required after registration; user can't proceed without | Clear onboarding flow; forces explicit household setup. MVP scope is two-person per household. | User decision |
| Email failure handling | Return error to user; let them retry | Transparent; user knows what happened and can try again. No hidden queue. | User decision |
| Double-join prevention | Reject with 409 Conflict if user already in household | Data integrity; prevents duplicate membership records. | User decision |
| Multi-household support | Show picker if user is in 2+ households; default to most recent | Future-proof without over-engineering. Minimal picker UI for MVP. | User decision |

## Scope

**In scope:**
- POST `/api/household` — create household with name validation
- POST `/api/household/{id}/invite` — send invitation email
- POST `/api/invitation/{token}/accept` — accept invitation with token validation and expiry checks
- Frontend household creation form
- Frontend invitation sending form
- Frontend invitation acceptance flow (unauthenticated, with email pre-fill on register)
- Mock email service (logs to stdout)
- Multi-household picker on dashboard

**Out of scope:**
- Household deletion, renaming, or advanced management (v1.1+)
- Email verification requirement (users can log in immediately after register)
- Role-based permissions (flat model per PRD)
- Bulk invitations
- Telegram integration (S-04+)
- Real email service (SendGrid, Mailgun) — upgrade path planned for v1.1

## Architecture / Approach

**Happy Path Flow:**

```
Register (email, password)
    ↓ [POST /auth/register] → JWT token
Create Household (name)
    ↓ [POST /api/household] → householdId
Invite Partner (email)
    ↓ [POST /api/household/{id}/invite] → token, email sent
Partner accepts (clicks link)
    ↓ [POST /api/invitation/{token}/accept] → household joined
Both users see Household
```

**Key Technical Decisions:**
- **Invitation tokens:** 32-char alphanumeric, stored in `HouseholdInvitation.invitationToken` (unique constraint), expires 24 hours from generation
- **Email service:** Interface + mock implementation logging to stdout; real provider injected in v1.1 without API changes
- **Multi-household state:** If user is in 1 household, auto-select. If 2+, show picker modal; default to most recent by `joinedAt`
- **User account matching:** When partner accepts invitation, system ensures email matches the invitation (prevents hijacking by different user)

## Phases at a Glance

| Phase | What it delivers | Key risk |
|-------|------------------|----------|
| 1. Backend API | Household CRUD, invitation generation, token acceptance, mock email service | Invitation token uniqueness and expiry validation must be bulletproof; unclear email exception handling |
| 2. Frontend UI | Household creation form, invitation form, acceptance flow, multi-household picker | Navigation complexity between register → household creation → invite form; unauthenticated acceptance flow tricky |

**Prerequisites:** F-01 (auth API), F-02 (household schema), F-04 (frontend scaffold) — all ≥95% complete.

**Estimated effort:** ~2–3 sessions (each session = ~4 hours implementation + testing). Backend Phase 1 is 1–1.5 sessions; Frontend Phase 2 is 1.5–2 sessions.

**Blocker:** Email infrastructure. Using mock resolves this for MVP; mark v1.1 task to wire real provider.

## Open Risks & Assumptions

- **Assumption:** User will reliably receive and click invitation link within 24 hours. If not, they can ask inviter to send again. (Mitigated: resend link is v1.1 feature)
- **Assumption:** Two-person household is the MVP model. If multi-household or group households become core later, the picker logic expands but no breaking changes.
- **Risk:** Email service mock logs to stdout; in production, logs might be noisy or lost if log level is not INFO. (Mitigated: mark v1.1 to use proper logging; mock is acceptable for dev/demo)
- **Assumption:** Household name is not unique (multiple users can name their household "My Household"). If name collision becomes a problem, add uniqueness in v1.1.

## Success Criteria (Summary)

- **New user can complete registration → household creation → partner invitation → partner acceptance in < 5 minutes**
- **Invitation tokens are valid for exactly 24 hours; expired tokens rejected with 410 Gone**
- **Double-accepting same invitation is prevented with 409 Conflict**
- **Both users see the same household on login (household is visible, can see each other's assignments)**
- **Email failure returns error to user with clear message and retry option**
- **All automated tests pass: `mvn test`, `npm run build`, ESLint**
- **Manual testing verifies happy path + error cases (expired token, already accepted, email failure)**
