# Remove Multi-User Scaffold Implementation Plan

## Overview

Remove the multi-user invitation and membership infrastructure that was scaffolded during the multi-user design phase but is not needed for the single-user MVP. This cleanup reduces schema complexity, removes dead code paths, and establishes a clean foundation before shipping S-06 and S-07.

## Current State Analysis

**What exists today:**
- Backend: HouseholdMember and HouseholdInvitation entities with full services and repositories; household queries use membership instead of ownership
- Frontend: InvitePartnerPage and InvitationAcceptPage components; routes for `/invitation/accept/:token` and `/household/:id/invite`
- Database: Three extra tables (household_members, household_invitations) that exist but are unused in the MVP
- Task schema: Includes assignee field and HouseholdMemberSelect component

**Key Discoveries:**
- Project uses Hibernate with `ddl-auto=create-drop` — no Flyway/Liquibase migrations. Schema is auto-managed.
- HouseholdService.createHousehold() creates a HouseholdMember entry with CREATOR role — must be removed.
- Household entity has `Set<HouseholdMember> members` — must be removed along with mapping.
- SecurityConfig has rule `/api/invitation/*/accept` — must be removed.
- RegisterPage accepts inviteToken from URL params — this entire path must be removed.

## Desired End State

**After this plan:**
- All multi-user infrastructure removed: entities, repositories, services, controllers, frontend pages, routes
- Household queries refactored to use `createdBy == userId` instead of membership
- Household creation simplified: no HouseholdMember entry created
- Tasks simplified: no assignee field or member selection
- Test suite reduced: multi-user tests deleted; single-user happy path verified

**How to verify:**
- All TypeScript and Java compilation passes
- All tests pass
- API endpoints work: `POST /api/household` (create), `GET /api/household` (list), `GET /api/household/{id}` (get one)
- Frontend flows work: household create → dashboard without invitation prompts
- Single-user data isolation: user only sees their own household

## What We're NOT Doing

- **Multi-user support is not added.** v1.1 will re-introduce shared households.
- **Data migration.** This is greenfield MVP; no production data exists.
- **Cascading deletion cleanup.** Already configured.
- **Email service beyond invitation removal.** EmailService used only for invitations.

## Implementation Approach

**Phased removal, backend-first:** Delete entities → refactor services → delete frontend → test. Compile errors guide the refactoring.

## Critical Implementation Details

**HouseholdService.createHousehold() change:** Currently creates a HouseholdMember entry. After refactoring, only creates Household. Ownership is enforced by Household.createdBy == userId.

**Household.getUserHouseholds() change:** Currently queries HouseholdMember by userId. After refactoring, queries Household directly where createdBy == userId.

**Task assignee handling:** The Task entity has an assignee field and HouseholdMemberSelect component. Both are removed entirely to keep MVP scope clean.

## Phase 1: Remove Multi-User Entities & Repositories

### Overview

Delete entity classes, repositories, and enums. Compile errors will surface all usages for Phase 2.

### Changes Required:

#### 1. Delete HouseholdMember, HouseholdInvitation, HouseholdMemberRole

**Files**: 
- `src/main/java/com/example/doneyet/domain/HouseholdMember.java`
- `src/main/java/com/example/doneyet/domain/HouseholdInvitation.java`
- `src/main/java/com/example/doneyet/domain/HouseholdMemberRole.java`

**Intent**: Entity definitions are unused in single-user MVP. Delete entirely.

**Contract**: Removes `@Entity` classes and table definitions.

#### 2. Delete Repositories

**Files**: 
- `src/main/java/com/example/doneyet/repository/HouseholdMemberRepository.java`
- `src/main/java/com/example/doneyet/repository/HouseholdInvitationRepository.java`

**Intent**: Repository interfaces are unused. Delete entirely.

**Contract**: Removes data access layer for multi-user entities.

### Success Criteria:

#### Automated Verification:

- Java compilation reports clear "cannot find symbol HouseholdMember" errors
- TypeScript compilation fails with entity reference errors

#### Manual Verification:

- Verify 5 entity/repository files are deleted from filesystem

---

## Phase 2: Refactor Services & Controllers

### Overview

Fix compile errors by refactoring HouseholdService, Household entity, and controllers to single-user semantics.

### Changes Required:

#### 1. Update Household Entity

**File**: `src/main/java/com/example/doneyet/domain/Household.java`

**Intent**: Remove the `members` relationship.

**Contract**: Remove the field and its getter/setter:
```
@OneToMany(mappedBy = "household", cascade = CascadeType.ALL, orphanRemoval = true)
private Set<HouseholdMember> members = new HashSet<>();
```

#### 2. Refactor HouseholdService

**File**: `src/main/java/com/example/doneyet/service/HouseholdService.java`

**Intent**: Remove all invitation-related methods; refactor queries to single-user semantics.

**Contract**: 

Delete methods:
- `sendInvitation()`
- `acceptInvitation()`
- `acceptInvitationForUser()`
- `joinHousehold()`

Refactor `createHousehold()` — do NOT create HouseholdMember.

Refactor `getUserHouseholds()` — query Household by createdBy, not via membership.

Refactor `getHouseholdDetails()` — remove member listing; enforce ownership via createdBy check.

#### 3. Remove InvitationController & Invite Endpoint

**Files**: 
- `src/main/java/com/example/doneyet/controller/InvitationController.java` — DELETE
- `src/main/java/com/example/doneyet/controller/HouseholdController.java` — remove `sendInvitation()` method

**Intent**: Invitation endpoints not used in single-user MVP.

**Contract**: Removes POST `/api/invitation/{token}/accept` and POST `/api/household/{id}/invite`.

#### 4. Update SecurityConfig

**File**: `src/main/java/com/example/doneyet/config/SecurityConfig.java`

**Intent**: Remove security rule for invitation endpoints.

**Contract**: Remove `.requestMatchers("/api/invitation/*/accept").permitAll()` if present.

#### 5. Add Single-User Query to HouseholdRepository

**File**: `src/main/java/com/example/doneyet/repository/HouseholdRepository.java`

**Intent**: Add method for single-user queries.

**Contract**: Add:
```java
List<Household> findByCreatedById(UUID userId);
```

#### 6. Simplify HouseholdDto

**File**: `src/main/java/com/example/doneyet/dto/HouseholdDto.java`

**Intent**: Remove multi-user DTOs.

**Contract**: Delete:
- `InvitationRequest`
- `InvitationResponse`
- `HouseholdMemberDto`

Remove `members` field from `HouseholdDetailsResponse`.

### Success Criteria:

#### Automated Verification:

- `mvn clean compile` passes
- `mvn test` passes
- `npm run typecheck` passes

#### Manual Verification:

- `POST /api/household` returns `{ householdId, name, createdBy, createdAt }`
- `GET /api/household` returns only current user's households
- `GET /api/household/{id}` returns 401 if not owner
- HouseholdService has exactly 3 public methods

---

## Phase 3: Remove Frontend Multi-User Pages & Routes

### Overview

Delete invitation pages, routes, and API functions. Refactor integration points.

### Changes Required:

#### 1. Delete Frontend Pages

**Files**:
- `frontend/src/features/household/pages/InvitePartnerPage.tsx` — DELETE
- `frontend/src/features/household/pages/InvitationAcceptPage.tsx` — DELETE
- `frontend/src/features/tasks/components/HouseholdMemberSelect.tsx` — DELETE

**Intent**: Invitation and task assignment components not used in single-user MVP.

**Contract**: Removes components and their exports.

#### 2. Update Router

**File**: `frontend/src/router.tsx`

**Intent**: Remove invitation routes.

**Contract**: 
- Delete imports for InvitePartnerPage and InvitationAcceptPage (lines 16-17)
- Delete route for `/invitation/accept/:token` (lines 83-89)
- Delete route for `/household/:householdId/invite` (lines 103-111)

#### 3. Update Household API

**File**: `frontend/src/features/household/api.ts`

**Intent**: Remove invitation API functions.

**Contract**: 
- Delete `sendInvitationApi()` and `acceptInvitationApi()` functions
- Delete types `SendInvitationRequest`, `SendInvitationResponse`, `HouseholdMember`
- Remove `members?` field from Household interface

#### 4. Update RegisterPage

**File**: `frontend/src/features/auth/pages/RegisterPage.tsx`

**Intent**: Remove invitation logic from registration.

**Contract**: 
- Remove inviteToken URL param extraction
- Remove acceptInvitationApi() call after registration
- Remove pre-filled email logic
- Keep straightforward register → dashboard flow

#### 5. Update HouseholdCreatePage

**File**: `frontend/src/features/household/pages/HouseholdCreatePage.tsx`

**Intent**: Redirect to dashboard after household creation.

**Contract**: After creation, redirect to `/dashboard` instead of `/household/{id}/invite`.

#### 6. Update DashboardPage

**File**: `frontend/src/features/tasks/pages/DashboardPage.tsx`

**Intent**: Remove "Invite Partner" button and multi-household UI.

**Contract**: 
- Delete or hide "Invite Partner" button
- Simplify household selection (use user's single household)
- Remove multi-household list UI

### Success Criteria:

#### Automated Verification:

- `npm run typecheck` passes
- `npm run build` passes

#### Manual Verification:

- `/invitation/accept/...` returns 404
- `/household/:id/invite` returns 404
- Register → dashboard (no invite prompt)
- Household create → dashboard (no invite redirect)
- Dashboard loads without "Invite Partner" button

---

## Phase 4: Testing & Verification

### Overview

Delete multi-user tests, add single-user tests, verify end-to-end flow.

### Changes Required:

#### 1. Delete Multi-User Tests

**Files to delete**:
- `src/test/java/com/example/doneyet/repository/HouseholdMemberRepositoryTest.java`
- `src/test/java/com/example/doneyet/repository/HouseholdInvitationRepositoryTest.java`

**Intent**: These tests verify multi-user behavior; in single-user MVP, ownership is via createdBy.

**Contract**: Remove files entirely.

#### 2. Refactor HouseholdServiceTest

**File**: `src/test/java/com/example/doneyet/service/HouseholdServiceTest.java`

**Intent**: Remove invitation/membership test methods.

**Contract**: 
- Delete all `*Invitation*` test methods
- Delete all `*Join*` test methods
- Keep tests for: createHousehold, getUserHouseholds, getHouseholdDetails
- Add test for access denial (user cannot access another user's household)

#### 3. Add Single-User Query Tests

**File**: `src/test/java/com/example/doneyet/repository/HouseholdRepositoryTest.java`

**Intent**: Add tests for single-user queries.

**Contract**: Add tests:
```java
@Test
void userCanOnlySeetheirOwnHouseholds() { ... }

@Test
void householdAccessEnforcedByCreatedBy() { ... }
```

### Success Criteria:

#### Automated Verification:

- `mvn test` passes (all tests)
- `bun test` passes (frontend tests)
- Code coverage for HouseholdService ≥ 80%

#### Manual Verification:

1. **Backend smoke test:**
   - Start app, register user1 and user2
   - User1 creates household, verifies `GET /api/household` shows only their household
   - User2 cannot access user1's household (401)

2. **Frontend smoke test:**
   - Register → create household → create task → verify dashboard loads
   - No "Invite Partner" button
   - Create task and verify it appears

3. **E2E flow:**
   - Full flow: register → household → task → complete → no regressions

---

## Testing Strategy

### Unit Tests:
- Single-user household queries (findByCreatedById)
- Household creation (no HouseholdMember entry)
- Access control (user cannot access other user's household)

### Integration Tests:
- End-to-end household creation and task creation
- Single-user household isolation

### Manual Testing Steps:
1. Register and create household
2. Create, edit, complete tasks
3. Logout and verify household isolation per user
4. Verify no multi-user endpoints accessible

## Performance Considerations

Removing multi-user code reduces complexity:
- One fewer join (HouseholdMember) in household queries
- Simpler ownership checks (direct createdBy comparison)
- Fewer indexes to maintain

## Migration Notes

**No data migration needed.** Project uses Hibernate with `ddl-auto=create-drop`:
1. Old tables automatically dropped when entities are deleted
2. New schema created fresh on app startup
3. Greenfield MVP has no production data

## References

- Roadmap: `context/foundation/roadmap.md`
- PRD: `context/foundation/prd.md`

## Progress

### Phase 1: Remove Multi-User Entities & Repositories

#### Automated

- [x] 1.1 Delete HouseholdMember.java, HouseholdInvitation.java, HouseholdMemberRole.java — 611df04
- [x] 1.2 Delete HouseholdMemberRepository.java, HouseholdInvitationRepository.java — 611df04
- [x] 1.3 Verify compilation fails with expected errors — 611df04

#### Manual

- [x] 1.4 Verify all 5 entity/repository files are deleted — 611df04

### Phase 2: Refactor Services & Controllers

#### Automated

- [x] 2.1 Update Household entity (remove members relationship) — 3409c45
- [x] 2.2 Refactor HouseholdService methods — 3409c45
- [x] 2.3 Delete InvitationController and sendInvitation endpoint — 3409c45
- [x] 2.4 Update SecurityConfig — 3409c45
- [x] 2.5 Add findByCreatedById() to HouseholdRepository — 3409c45
- [x] 2.6 Simplify HouseholdDto — 3409c45
- [x] 2.7 `mvn clean compile` passes — 3409c45
- [x] 2.8 `mvn test` passes — 3409c45

#### Manual

- [ ] 2.9 Test POST /api/household, GET /api/household, GET /api/household/{id}
- [ ] 2.10 Verify access control (401 for non-owner)

### Phase 3: Remove Frontend Multi-User Pages & Routes

#### Automated

- [x] 3.1 Delete InvitePartnerPage.tsx, InvitationAcceptPage.tsx, HouseholdMemberSelect.tsx
- [x] 3.2 Update router.tsx to remove routes and imports
- [x] 3.3 Update household/api.ts to remove invitation functions
- [x] 3.4 Update RegisterPage, HouseholdCreatePage, DashboardPage
- [x] 3.5 `npm run typecheck` passes
- [x] 3.6 `npm run build` passes

#### Manual

- [ ] 3.7 Verify routes return 404, registration flow works, no invite buttons

### Phase 4: Testing & Verification

#### Automated

- [ ] 4.1 Delete HouseholdMemberRepositoryTest.java, HouseholdInvitationRepositoryTest.java
- [ ] 4.2 Refactor HouseholdServiceTest, add single-user tests
- [ ] 4.3 `mvn test` passes
- [ ] 4.4 `bun test` passes

#### Manual

- [ ] 4.5 Backend smoke test: users see only their households
- [ ] 4.6 Frontend smoke test: register, create household, create task
- [ ] 4.7 E2E flow: no regressions