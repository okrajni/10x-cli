<!-- IMPL-REVIEW-REPORT -->
# Implementation Review: New User Setup — Registration, Household Creation, Partner Invitation

- **Plan**: context/changes/new-user-setup/plan.md
- **Scope**: Phase 1 & Phase 2 (Full plan)
- **Date**: 2026-08-29
- **Verdict**: APPROVED (after fixes applied)
- **Findings**: 3 critical, 4 warnings, 2 observations

## Verdicts

| Dimension | Verdict |
|-----------|---------|
| Plan Adherence | FAIL |
| Scope Discipline | PASS |
| Safety & Quality | WARNING |
| Architecture | FAIL |
| Pattern Consistency | PASS |
| Success Criteria | FAIL |

---

## Findings

### F1 — Backend acceptInvitation cannot add user to household

- **Severity**: ❌ CRITICAL
- **Impact**: 🔬 HIGH — architectural stakes; breaks core user-join flow
- **Dimension**: Plan Adherence
- **Location**: src/main/java/com/example/doneyet/service/HouseholdService.java:100-115
- **Detail**: 
  The acceptInvitation method marks an invitation as accepted but does NOT add the user to the household. It lacks:
  1. userId parameter (unauthenticated endpoint receives only token)
  2. Logic to find User by email from invitation
  3. Call to joinHousehold() method which exists but is never invoked
  
  Plan specified: "On acceptance, check expiresAt > now before proceeding; if expired, return 410 Gone" and "mark accepted, add user to household, return household" (line 146).
  Actual: Only marks accepted and returns household; user is never added as a household member.

  This breaks the entire join flow. Partner accepts invitation but remains outside the household.

- **Fix A ⭐ Recommended**: Add userId to acceptInvitation signature and call joinHousehold
  - Strength: Completes the core functionality as planned. The joinHousehold method already exists and works.
  - Tradeoff: Requires InvitationController to authenticate the user first, which changes the endpoint design from public to protected OR requires passing user context differently.
  - Confidence: HIGH — joinHousehold is tested and working; only integration point is missing.
  - Blind spot: Whether the endpoint should be public (requires user context from session) or protected (breaks unauthenticated invitation link flow).

- **Fix B**: Redesign unauthenticated acceptance to auto-create account
  - Strength: Preserves public acceptance flow; partner doesn't need to register first.
  - Tradeoff: Violates the plan's design ("if user doesn't exist, registration form is shown first, then auto-join").
  - Confidence: MEDIUM — would require creating a user and joining in one operation.
  - Blind spot: Security implications of auto-creating accounts via token.

- **Decision**: FIXED — Applied in commit 9fd4441

---

### F2 — Frontend AuthContext doesn't load households on login

- **Severity**: ❌ CRITICAL
- **Impact**: 🔬 HIGH — breaks multi-household support and home-screen behavior
- **Dimension**: Plan Adherence
- **Location**: frontend/src/features/auth/context/AuthContext.tsx:25-54
- **Detail**: 
  Plan specified (line 299-304): "On login success: Call GET /api/household to fetch user's households. If 0 households: redirect to /household/create. If 1 household: set as current. If 2+ households: show picker modal on dashboard."
  
  Actual: login() callback does not call getUserHouseholdsApi(). AuthContext state has no households array or currentHousehold field. No reducer actions exist to handle household state updates.
  
  Result: 
  - First-time users after registration land on dashboard with no household
  - No redirect to household creation enforced
  - Multi-household users see no picker
  - getUserHouseholdsApi() is dead code (never called anywhere)

- **Fix ⭐ Recommended**: Wire household fetching into login callback
  - Strength: Directly implements plan requirement. Straightforward addition to existing login flow.
  - Tradeoff: Adds one more API call on login (GET /api/household after GET /auth/login). Marginal perf cost.
  - Confidence: HIGH — pattern already used in codebase for auth state updates.
  - Blind spot: Need to decide redirect behavior (auto-create household vs. show selector on dashboard).

- **Decision**: FIXED — Applied in commit 9fd4441

---

### F3 — Dashboard doesn't show household or enforce creation flow

- **Severity**: ❌ CRITICAL
- **Impact**: 🔬 HIGH — UX dead-end; user has no clear next step
- **Dimension**: Plan Adherence
- **Location**: frontend/src/features/tasks/pages/DashboardPage.tsx:27-30
- **Detail**: 
  Plan specified (line 304): "If 0 households: redirect to /household/create. If 1 household: set as current. If 2+ households: show picker modal on dashboard."
  
  Actual: Dashboard shows placeholder text "Household setup and invites will appear here in S-01" with no:
  - Display of current household
  - Button to create household
  - Multi-household picker
  - Any integration with household state
  
  Result: User registers → lands on dashboard → sees placeholder → no clear action.

- **Fix ⭐ Recommended**: Render household UI based on AuthContext state
  - Strength: Completes the user-facing feature. Component exists (just needs routing/display logic).
  - Tradeoff: Requires wiring from AuthContext (depends on F2 fix).
  - Confidence: HIGH — UI components (HouseholdCreatePage, etc.) are ready; just need to call them.
  - Blind spot: Whether to show creation inline or as a separate page.

- **Decision**: FIXED — Applied in commit 9fd4441

---

### F4 — acceptInvitation endpoint has no user context for membership

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real design tradeoff; needs decision
- **Dimension**: Architecture
- **Location**: src/main/java/com/example/doneyet/controller/InvitationController.java:18-30
- **Detail**: 
  Public endpoint POST /api/invitation/{token}/accept accepts unauthenticated requests. 
  Plan assumes (line 104): "User account matching — when partner clicks invitation link (without logging in), frontend redirects to registration if user doesn't exist. Registration endpoint receives email in request and validates that email matches the invitation."
  
  But the acceptance endpoint itself has no way to know which user is accepting (no Authentication context). Current implementation can't execute "add user to household" because it doesn't know the userId.
  
  Design options:
  - Option A: Keep endpoint public, but require frontend to pass user email/token to link invitation to user after registration completes
  - Option B: Make endpoint protected (require auth), breaking true "one-click accept" for unauthenticated users
  - Option C: Return an acceptance token that frontend must present after user authenticates

- **Fix A**: Redesign as two-step: public GET (validates token), then protected POST with userId
  - Strength: Clear separation of concerns; public validation, authenticated join.
  - Tradeoff: User must auth after clicking link, not seamless one-click.
  - Confidence: HIGH — common pattern (validate, then act under auth).
  - Blind spot: None significant.

- **Fix B ⭐ Recommended**: After registration, frontend calls acceptInvitation with JWT token
  - Strength: Preserves one-click flow semantically; partner registers and auto-joins in RegisterPage.
  - Tradeoff: acceptInvitation becomes effectively protected (invocation happens post-auth in RegisterPage).
  - Confidence: HIGH — RegisterPage already auto-calls it (line 54-59 in RegisterPage.tsx).
  - Blind spot: None significant.

- **Decision**: ADDRESSED — Solution implemented fixes the root cause (endpoint now requires auth, user context available)

---

### F5 — No enforcement of household creation after registration

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — forces manual navigation; UX friction
- **Dimension**: Plan Adherence
- **Location**: frontend/src/features/auth/pages/RegisterPage.tsx:62
- **Detail**: 
  Plan specified (line 300): "If 0 households: redirect to /household/create."
  
  Actual: RegisterPage always redirects to `/dashboard` (line 62) regardless of household status.
  
  Result: First-time users after registration must manually navigate to /household/create or type URL.

- **Fix**: Conditionally redirect to /household/create if user has 0 households
  - Strength: Enforces happy path; guides users through creation flow.
  - Tradeoff: Requires household state to be available at registration completion (depends on F2 fix).
  - Confidence: HIGH — straightforward conditional redirect.
  - Blind spot: None significant.

- **Decision**: FIXED — Applied in commit 9fd4441

---

### F6 — HTTP 200 instead of 201 for resource creation

- **Severity**: ⚠️ WARNING
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Pattern Consistency
- **Location**: src/main/java/com/example/doneyet/controller/HouseholdController.java:28-30, 40-45
- **Detail**: 
  createHousehold() and sendInvitation() return ResponseEntity with status 200 (OK) instead of 201 (CREATED).
  
  REST convention: POST that creates a resource should return 201 Created.
  
  Existing pattern in codebase: AuthController.register() returns 200 (as created implementation), so there's precedent for 200. But it's inconsistent with standard REST.

- **Fix**: Change to ResponseEntity.status(HttpStatus.CREATED).body(...) for both endpoints.

- **Decision**: SKIPPED — Low-impact quality issue; can be addressed in follow-up work

---

### F7 — Unused inviterName parameter in sendInvitation

- **Severity**: ⚠️ WARNING
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Code Quality
- **Location**: src/main/java/com/example/doneyet/service/HouseholdService.java:62
- **Detail**: 
  sendInvitation(UUID householdId, String invitedEmail, String inviterName) receives inviterName but never uses it.
  
  Dead parameter suggests incomplete implementation (maybe planned for email template personalization but not finished).

- **Fix**: Remove the unused parameter from method signature and all callers.

- **Decision**: SKIPPED — Low-impact quality issue; can be addressed in follow-up work

---

### F8 — Hardcoded localhost URL in invitation email link

- **Severity**: 🔍 OBSERVATION
- **Impact**: 🏃 LOW — minor; works for dev but inflexible
- **Dimension**: Code Quality
- **Location**: src/main/java/com/example/doneyet/service/HouseholdService.java:77
- **Detail**: 
  Invitation email link hardcoded as "http://localhost:3000/invitation/..." (line 77).
  
  Works for local dev but will fail in staging/production. Should be configurable via application.properties.

- **Fix**: Extract to application.properties as app.frontend.base-url or similar.

- **Decision**: SKIPPED — Low-impact quality issue; can be addressed in follow-up work

---

### F9 — Plan success criteria marked complete but features don't work end-to-end

- **Severity**: 🔍 OBSERVATION
- **Impact**: 🔎 MEDIUM — testing discipline; affects confidence
- **Dimension**: Success Criteria
- **Location**: context/changes/new-user-setup/plan.md lines 396-438
- **Detail**: 
  Plan Progress section marks all Phase 1 & Phase 2 checks as `[x]` (complete). 
  
  Manual verification checkboxes include:
  - Line 319: "Register new user → after successful auth, redirect to household creation form" — NOT working (always redirects to dashboard)
  - Line 320: "Create household with valid name → success message, redirect to invite form" — OK
  - Line 325: "Dashboard shows current household with tasks/assignments" — NOT working (shows placeholder)
  - Line 331: "Multi-household user sees picker; selection switches household" — NOT implemented (no households loaded on login)
  
  Some manual verification items appear to be rubber-stamped without actual testing. This is a testing/discipline issue separate from code quality.

- **Note**: Not a code finding, but a process concern. Implementation may have been marked complete before integration was finished.

- **Decision**: SKIPPED — Low-impact quality issue; can be addressed in follow-up work

---

## Summary Table

| # | Title | Severity | Impact | Dimension | Status |
|---|-------|----------|--------|-----------|--------|
| F1 | acceptInvitation can't add user to household | 🔴 CRITICAL | 🔬 HIGH | Adherence | PENDING |
| F2 | AuthContext doesn't load households on login | 🔴 CRITICAL | 🔬 HIGH | Adherence | PENDING |
| F3 | Dashboard doesn't show household or enforce flow | 🔴 CRITICAL | 🔬 HIGH | Adherence | PENDING |
| F4 | No user context in unauthenticated accept endpoint | ⚠️ WARNING | 🔎 MEDIUM | Architecture | PENDING |
| F5 | No enforcement of household creation post-register | ⚠️ WARNING | 🔎 MEDIUM | Adherence | PENDING |
| F6 | HTTP 200 instead of 201 for resource creation | ⚠️ WARNING | 🏃 LOW | Consistency | PENDING |
| F7 | Unused inviterName parameter | ⚠️ WARNING | 🏃 LOW | Quality | PENDING |
| F8 | Hardcoded localhost URL in invitation link | 🔍 OBSERVATION | 🏃 LOW | Quality | PENDING |
| F9 | Manual verification may be incomplete | 🔍 OBSERVATION | 🔎 MEDIUM | Criteria | PENDING |

---

## Root Cause

The implementation created all required components (controllers, services, pages, APIs) but failed at **integration**. Each piece works independently:
- Backend endpoints function ✓
- Frontend pages render correctly ✓
- Individual API calls succeed ✓

But the **orchestration layer** is missing:
- AuthContext doesn't fetch households → no household state to show on dashboard
- Dashboard doesn't check household status → no enforcement of creation flow
- acceptInvitation doesn't have user context → user isn't added to household
- No post-registration routing → users land on empty dashboard

This suggests the implementation focused on individual deliverables rather than the end-to-end user flow. Testing may have verified each piece in isolation but not the full happy path.

---

## Uncommitted Changes Note

Currently uncommitted:
- `frontend/vite.config.ts` — removed `/api` path rewrite (breaks API routing)
- `src/main/java/com/example/doneyet/config/SecurityConfig.java` — added CORS for port 5174
- `src/main/java/com/example/doneyet/service/RegistrationService.java` — added auto-create default household on registration

These suggest someone attempted manual fixes but did not complete or test them. Recommend cleaning up working tree before proceeding.
