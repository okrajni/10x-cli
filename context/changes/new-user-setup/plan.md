# New User Setup — Registration, Household Creation, Partner Invitation

## Overview

Complete the household onboarding flow for the done yet? task management system. Users will register with email/password (reusing existing auth endpoints), create a household with a name, send invitation emails to a partner, and the partner can accept the invitation via a token link to join the household. Both users will then see the same household on login.

This plan fulfills the critical on-ramp from the PRD (US-01, FR-001–005) and unblocks S-02 (basic task CRUD) and all downstream slices.

## Current State Analysis

**Completed Prerequisites:**
- ✓ **F-01 (auth-scaffold):** `POST /auth/register` and `POST /auth/login` endpoints fully implemented with JWT tokens, password hashing (BCrypt), and rate limiting
- ✓ **F-02 (household-schema):** `Household`, `HouseholdMember`, `HouseholdInvitation` entities defined with repositories and soft-delete patterns
- ✓ **F-04 (frontend-scaffold):** React app with routing (React Router), context API auth state management, component library (`Button`, `Input`), and auth pages (login, register stubs)

**Existing Patterns & Conventions:**
- **API responses:** Standardized DTOs (e.g., `AuthDto.RegisterRequest`, `AuthDto.AuthResponse`); error handling via custom exceptions (`ValidationException` → 422, `ConflictException` → 409)
- **Auth tokens:** JWT with 24-hour expiry, claims include `userId` and `email`; frontend auto-injects token in `Authorization: Bearer` header
- **Frontend client:** Type-safe `ApiResult<T>` discriminated union; automatic retry logic for network/5xx errors
- **Error patterns:** Unified exception handling in `GlobalExceptionHandler.java`; all responses include `code` and `message`

**What's Missing:**
- ✗ Email service (no Spring Mail dependency; blocker for invitation emails)
- ✗ Household creation endpoint (`POST /api/household`)
- ✗ Invitation sending endpoint (`POST /api/household/{id}/invite`)
- ✗ Invitation acceptance endpoint (`POST /api/invitation/{token}/accept`)
- ✗ Household creation form on frontend
- ✗ Invitation acceptance page and link handling
- ✗ Household picker / selection logic (for multi-household support)

### Key Discoveries:

- **Invitation infrastructure ready:** `HouseholdInvitation` entity has all needed fields: `invitedEmail`, `invitationToken` (unique), `expiresAt`, `accepted`, `acceptedAt`, `acceptedByUser`
- **No email service yet:** Email is the blocker; we'll mock it for MVP
- **Auth is reusable:** RegisterPage will call existing `/auth/register` endpoint (already tested)
- **Frontend routing established:** Router supports lazy-loading pages; add new routes for household creation and invitation acceptance
- **Error handling standardized:** All errors return 422/409/503 with structured JSON; frontend client already handles retries and displays user-friendly messages

## Desired End State

After this plan is complete, a user will be able to:

1. **Register** with email + password → receive JWT token, logged in immediately
2. **Create a household** with a name → household is created, user is marked as creator/member
3. **Invite a partner** by entering their email → system generates a unique invitation token, stores it with 24-hour expiry
4. **Send invitation email** → email contains a link with the token (e.g., `/invitation/accept/<token>`)
5. **Accept invitation** by clicking the link → if user exists, join household; if user doesn't exist, create account and auto-join
6. **See the household** on next login → both users see the household on their dashboard; if user is in multiple households, picker shows most recent

**Verification:**
- New user can complete the flow in < 5 minutes (guardrail from PRD)
- Invitation link is valid for 24 hours
- Expired or already-accepted invitations are rejected with clear error messages
- Email failure returns error to user with retry option
- Both users see the same household on login
- Double-joining (accepting same invitation twice) is rejected with 409 Conflict

## What We're NOT Doing

- **Email verification requirement:** Users can log in immediately after registration without verifying email (S-01 is about speed; email verification is v1.1 feature)
- **Household deletion or renaming:** Only creation in scope; updates/deletion are later features
- **Role-based permissions:** Both household members have equal access; flat model per PRD
- **Bulk invitations:** One invitation at a time; bulk invites are v1.1
- **Telegram integration:** Telegram linking happens in S-04; this slice is web app + email only
- **Real email service:** MVP uses mock/stub; production email provider (SendGrid, Mailgun) is v1.1 migration
- **Advanced household discovery:** No "join public households" or "browse listings"; only invite-based joining
- **Household switching UI:** Bare picker only if in multiple households; full household management is later

## Implementation Approach

**Technology Stack:**
- **Backend:** Spring Boot REST controller, service layer, JPA repositories (reuse existing patterns)
- **Email service:** Mock implementation with in-memory queue (logs to stdout for dev/test)
- **Tokens:** JWT-style 24-hour tokens using existing `JwtTokenProvider` as inspiration; generate short alphanumeric tokens stored in `HouseholdInvitation.invitationToken`
- **Frontend:** React forms (registration → household creation → invitation form), routing for `/household/create` and `/invitation/accept/:token`, Context API for user state updates
- **Error handling:** Standardized exceptions, same response format as auth endpoints

**Data Flow:**

```
Register (existing)
  ↓
Create Household (new)
  ↓
Send Invitation (new email)
  ↓
Accept Invitation (new token validation)
  ↓
Both users see same Household
```

**Architectural Decision:**
- **Invitation tokens are temporary, one-time-use:** Each `HouseholdInvitation` record is created with a unique token and 24-hour expiry. After acceptance, `accepted = true` and `acceptedByUser = user_id`. If user tries to accept again, error is returned (idempotency rejected per user decision).
- **Household picker is minimal MVP:** If user is in multiple households, show a simple picker on dashboard; default to most recent. Full household switching UI (sidebar, header dropdown) deferred.
- **Email is mocked for MVP:** `EmailService` interface with mock implementation logs to stdout. Real provider swapped in v1.1 without changing controller logic.
- **User account required for acceptance:** Invitation acceptance checks if user exists by email. If yes, join household. If no, registration form is shown first, then auto-join on registration.

## Critical Implementation Details

**Invitation Token Generation & Expiry:** The `invitationToken` field in `HouseholdInvitation` must be a unique, unguessable string (e.g., 32-char hex or URL-safe Base64). Tokens are stored with `expiresAt = now + 24 hours`. On acceptance, check `expiresAt > now` before proceeding; if expired, return 410 Gone. Use database uniqueness constraint to prevent duplicate tokens.

**Email Service Contract:** `EmailService` interface will have method `sendInvitationEmail(String to, String link, String householdName)`. Mock implementation prints to `System.out` and stores in in-memory queue for testing. Real provider injected in pom.xml dependency + application.properties config (deferred to v1.1). No changes to controller needed when provider swaps.

**User Account Matching:** When partner clicks invitation link (without logging in), frontend redirects to registration if user doesn't exist. Registration endpoint receives email in request (not from form input) and validates that email matches the invitation. This prevents a different user from hijacking the invite by registering with a different email.

## Phase 1: Backend Household & Invitation API

### Overview

Implement REST endpoints for creating households and managing invitations: household creation, invitation sending, and acceptance. Wire the mock email service so invitations can be tested via logs.

### Changes Required:

#### 1. Create Email Service Interface & Mock Implementation

**File**: `src/main/java/com/example/doneyet/service/EmailService.java`

**Intent**: Define the contract for sending emails; mock implementation logs invitations to stdout for development.

**Contract**: Interface with single method:
- `void sendInvitationEmail(String toEmail, String invitationLink, String householdName)`

Mock implementation stores emails in `ConcurrentHashMap` for testing, prints to System.out.

#### 2. Create HouseholdController

**File**: `src/main/java/com/example/doneyet/controller/HouseholdController.java`

**Intent**: REST endpoints for household creation and invitation management.

**Contract**: REST controller with endpoints:
- `POST /api/household` — create household; body: `{ "name": "Smith Household" }`; response: `{ "householdId": "...", "name": "..." }`
- `POST /api/household/{householdId}/invite` — send invitation; body: `{ "invitedEmail": "partner@example.com" }`; response: `{ "invitationId": "...", "expiresAt": "..." }`
- `POST /api/invitation/{token}/accept` — accept invitation; no body; response: `{ "householdId": "...", "name": "..." }`
- `GET /api/household` — list households for current user; response: array of household objects

#### 3. Create HouseholdService

**File**: `src/main/java/com/example/doneyet/service/HouseholdService.java`

**Intent**: Business logic for household operations: creation, invitation generation, acceptance, and expiry validation.

**Contract**: Service class with methods:
- `Household createHousehold(UUID userId, String name)` — create and save household; link user as creator
- `HouseholdInvitation sendInvitation(UUID householdId, String invitedEmail, String inviterName)` — generate token, set 24h expiry, save, return invitation with generated link
- `Household acceptInvitation(String token)` — find invitation by token, validate expiry, check if already accepted, mark accepted, add user to household, return household
- `List<Household> getUserHouseholds(UUID userId)` — fetch all households user is member of, ordered by most recent first

#### 4. Create Household DTOs

**File**: `src/main/java/com/example/doneyet/dto/HouseholdDto.java`

**Intent**: Request/response shapes for household endpoints.

**Contract**: Inner classes:
- `CreateHouseholdRequest`: `name` (string, non-empty)
- `InvitationRequest`: `invitedEmail` (string, valid email format)
- `HouseholdResponse`: `householdId`, `name`, `createdBy`, `createdAt`
- `InvitationResponse`: `invitationId`, `token`, `expiresAt`, `invitedEmail`

#### 5. Add Email Service to ApplicationProperties

**File**: `src/main/resources/application.properties`

**Intent**: Configure email provider and sender address (stub for now).

**Contract**: Add properties:
- `app.email.enabled=true`
- `app.email.sender=noreply@doneyet.local`
- `app.email.provider=mock` (dev) or `sendgrid` (v1.1)

#### 6. Update SecurityConfig to Permit Invitation Acceptance

**File**: `src/main/java/com/example/doneyet/config/SecurityConfig.java`

**Intent**: Allow unauthenticated users to accept invitations (they may not have an account yet).

**Contract**: Add public endpoints:
- `/api/invitation/*/accept` (public)
- Keep `/api/household/*` protected (require auth)

### Success Criteria:

#### Automated Verification:

- [ ] `mvn compile` succeeds
- [ ] `mvn test` runs unit tests for `HouseholdService` (create, invite, accept, expiry validation)
- [ ] `mvn test` runs integration tests for `/api/household` endpoints
- [ ] Invalid household names (empty, null) are rejected with 422
- [ ] Invalid email format in invite request rejected with 422
- [ ] Duplicate household names allowed (no name uniqueness constraint)
- [ ] Expired invitation tokens rejected with 410 Gone
- [ ] Already-accepted invitation rejected with 409 Conflict
- [ ] Invitation acceptance requires email to match invitation

#### Manual Verification:

- [ ] Create household via curl/Postman → receive 200 with householdId
- [ ] Send invitation via curl/Postman → email appears in logs with correct link format
- [ ] Invitation link format is `/api/invitation/{token}/accept` (verifiable in logs)
- [ ] Accept valid token → 200, user is added to household
- [ ] Accept expired token → 410 Gone
- [ ] Accept already-accepted token → 409 Conflict
- [ ] List households for user → returns all households user is member of, ordered by most recent

---

## Phase 2: Frontend Household Creation & Invitation Forms

### Overview

Implement UI for household creation (post-registration) and invitation sending. Add routing for invitation acceptance with token from URL.

### Changes Required:

#### 1. Create HouseholdCreatePage Component

**File**: `frontend/src/features/household/pages/HouseholdCreatePage.tsx`

**Intent**: Form for user to create their first household after registration.

**Contract**: React component rendering:
- Form with `name` input field (required, non-empty validation)
- Submit button → calls POST `/api/household`
- Success → redirect to `/household/{householdId}/invite` (next step)
- Error → display error message, allow retry

#### 2. Create InvitePartnerPage Component

**File**: `frontend/src/features/household/pages/InvitePartnerPage.tsx`

**Intent**: Form to send invitation email to partner.

**Contract**: React component rendering:
- Household name display (context or URL param)
- Email input for invitee
- Submit button → calls POST `/api/household/{householdId}/invite`
- Success → show success message "Invitation sent to {email}"
- Option to send another invite or proceed to dashboard
- Error handling for invalid email or service failure

#### 3. Create InvitationAcceptPage Component

**File**: `frontend/src/features/household/pages/InvitationAcceptPage.tsx`

**Intent**: Handle invitation acceptance flow; token comes from URL (`:token` route param).

**Contract**: React component:
- Extract token from URL
- If user is authenticated: call `POST /api/invitation/{token}/accept` → redirect to dashboard
- If user is not authenticated: redirect to `/register?email={invitedEmail}&inviteToken={token}` (registration form receives context)
- Show loading state while validating token
- Handle errors: expired token (410) → show "Invitation expired", already accepted (409) → show "Already joined", other errors → show generic error

#### 4. Update RegisterPage for Invitation Context

**File**: `frontend/src/features/auth/pages/RegisterPage.tsx`

**Intent**: When user is registering via invitation link, pre-fill email and auto-join household after registration.

**Contract**: Read query params `?email=...&inviteToken=...`:
- Pre-fill email input (read-only or disabled)
- On successful registration, automatically call `POST /api/invitation/{token}/accept`
- Auto-redirect to dashboard after acceptance
- If token is missing/invalid, show warning but allow registration to proceed (user can join later via manual invite link)

#### 5. Update Router with New Routes

**File**: `frontend/src/router.tsx`

**Intent**: Add routes for household creation, invitation sending, and acceptance.

**Contract**: Add routes:
- `POST /household/create` → `HouseholdCreatePage` (protected)
- `POST /household/:householdId/invite` → `InvitePartnerPage` (protected)
- `GET /invitation/accept/:token` → `InvitationAcceptPage` (public)

#### 6. Update Household API Client

**File**: `frontend/src/features/household/api.ts`

**Intent**: Type-safe API client for household endpoints (already partially defined; complete implementation).

**Contract**: Functions:
- `createHousehold(name: string): Promise<ApiResult<HouseholdResponse>>`
- `sendInvitation(householdId: string, email: string): Promise<ApiResult<InvitationResponse>>`
- `acceptInvitation(token: string): Promise<ApiResult<HouseholdResponse>>`

#### 7. Update AuthContext to Handle Household State

**File**: `frontend/src/features/auth/context/AuthContext.tsx`

**Intent**: When user logs in, fetch their household list and handle multi-household selection.

**Contract**: Add to AuthContext:
- `households: Household[]` — list of households user is member of
- `currentHousehold: Household` — selected/current household (default to most recent)
- `setCurrentHousehold(householdId: string)` — switch households if user is in multiple

On login success:
- Call `GET /api/household` to fetch user's households
- If 0 households: redirect to `/household/create`
- If 1 household: set as current
- If 2+ households: show picker modal on dashboard

### Success Criteria:

#### Automated Verification:

- [ ] TypeScript compilation passes (`npm run build` succeeds)
- [ ] ESLint passes (`npm run lint`)
- [ ] App starts without console errors (`npm run dev`)

#### Manual Verification:

- [ ] Register new user → after successful auth, redirect to household creation form
- [ ] Create household with valid name → success message, redirect to invite partner form
- [ ] Send invitation with valid email → success message, can proceed to dashboard
- [ ] Click invitation link from browser → if logged out, redirect to register page with email pre-filled
- [ ] Register via invitation link → auto-join household, redirect to dashboard
- [ ] Accept expired invitation → show "Invitation expired" message
- [ ] Try to accept same invitation twice → show "Already joined" message
- [ ] Dashboard shows current household name/tasks
- [ ] If user is in multiple households, picker modal appears; selecting household updates `currentHousehold`

---

## Testing Strategy

### Unit Tests:

- **HouseholdService:**
  - Create household with valid/invalid names
  - Send invitation generates unique tokens, sets 24h expiry
  - Accept invitation validates token, expiry, and acceptance status
  - Accept already-accepted invitation returns error

- **Frontend:**
  - HouseholdCreatePage: form submission calls correct API
  - InvitePartnerPage: invitation sending with error handling
  - InvitationAcceptPage: token extraction, authenticated vs unauthenticated flows

### Integration Tests:

- **End-to-end happy path:**
  1. Register new user via `/auth/register`
  2. Create household via `POST /api/household`
  3. Send invitation via `POST /api/household/{id}/invite`
  4. Accept invitation via `POST /api/invitation/{token}/accept` with new user account
  5. Verify both users see same household on login

- **Edge cases:**
  - Expired token rejection
  - Already-accepted token rejection
  - Duplicate email in household rejection
  - Email service failure (mock logs but no exception)

### Manual Testing Steps:

1. Start dev server: `npm run dev` (frontend) + `mvn spring-boot:run` (backend)
2. Register new user (email: alice@example.com)
3. Create household (name: "Smith Household")
4. Send invitation (to: bob@example.com) — check logs for invitation link
5. Copy invitation link from logs, open in new incognito window
6. Register bob@example.com via invitation link
7. Verify bob is logged in and sees "Smith Household"
8. Log out bob, log in as alice
9. Verify alice sees same household
10. Test error cases: expired token, duplicate acceptance

## Performance Considerations

- **Household queries:** Add index on `household_members(user_id)` to speed up user household list fetch
- **Invitation token lookup:** `HouseholdInvitation.invitationToken` is already unique; querying by token is O(1)
- **Email sending:** Mock implementation is in-memory; real provider (SendGrid) will handle async sending via v1.1 upgrade

## Migration Notes

None for MVP. On v1.1 upgrade to real email provider:
- Swap mock `EmailService` with real implementation (e.g., `SendGridEmailService`)
- Update pom.xml to add SendGrid dependency
- Update application.properties with API key
- No data model or API contract changes needed (interface is stable)

## References

- Related research: `context/changes/new-user-setup/research.md` (if created)
- Foundation: `context/foundation/roadmap.md` (S-01)
- PRD: `context/foundation/prd.md` (US-01, FR-001–005)
- Auth scaffold: `context/changes/auth-scaffold/plan.md`
- Household schema: `context/changes/household-schema/plan.md`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Backend Household & Invitation API

#### Automated

- [x] 1.1 Create Email Service interface and mock implementation
- [x] 1.2 Create HouseholdController with create, invite, accept, and list endpoints
- [x] 1.3 Create HouseholdService with business logic for household and invitation operations
- [x] 1.4 Create Household DTOs (request/response shapes)
- [x] 1.5 Update application.properties with email service configuration
- [x] 1.6 Update SecurityConfig to permit public invitation acceptance
- [x] 1.7 Unit tests pass for HouseholdService
- [x] 1.8 Integration tests pass for all endpoints
- [x] 1.9 Compilation and type checking pass

#### Manual

- [x] 1.10 Create household via curl/Postman → receive 200 with householdId
- [x] 1.11 Send invitation via curl/Postman → email appears in logs with correct link
- [x] 1.12 Accept valid invitation token → 200, user added to household
- [x] 1.13 Accept expired token → 410 Gone with clear error
- [x] 1.14 Accept already-accepted token → 409 Conflict
- [x] 1.15 List households returns all user's households ordered by most recent

### Phase 2: Frontend Household Creation & Invitation Forms

#### Automated

- [ ] 2.1 TypeScript compilation passes
- [ ] 2.2 ESLint passes all checks
- [ ] 2.3 App starts without console errors
- [ ] 2.4 Frontend components render without errors

#### Manual

- [ ] 2.5 Register user → redirected to household creation form
- [ ] 2.6 Create household → success message, redirect to invite form
- [ ] 2.7 Send invitation → success message, see "Invitation sent"
- [ ] 2.8 Accept invitation via link (unauthenticated) → register form with email pre-filled
- [ ] 2.9 Register via invitation → auto-join household, redirect to dashboard
- [ ] 2.10 Dashboard shows current household with tasks/assignments
- [ ] 2.11 Multi-household user sees picker; selection switches household
- [ ] 2.12 Error scenarios: expired invite shows "Invitation expired", already-accepted shows "Already joined"
