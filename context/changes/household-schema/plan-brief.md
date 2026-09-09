# Household Schema — One Household Per User Constraint — Plan Brief

> Full plan: `context/changes/household-schema/plan.md`
> PRD: `context/foundation/prd.md` (FR-003, FR-004, NFR)

## What & Why

Users can currently create unlimited households, but the system assumes each user has exactly one. This creates a silent bug in TaskController where `.findFirst()` picks the first household if multiple exist. We need to enforce "one household per user" by adding validation in the backend, testing the constraint, and updating the UI to prevent users from encountering the error.

## Starting Point

- Household entity exists with basic fields (id, name, createdBy, createdAt, updatedAt)
- HouseholdService.`createHousehold()` validates name and user, but does NOT check for duplicate households
- Users can create unlimited households with no error
- Tests exist but don't validate the one-household constraint
- Frontend allows unlimited household creation
- TaskController assumes one household via `.findFirst()` (works, but fragile)

## Desired End State

After this plan is complete:
- HouseholdService prevents users from creating a 2nd household (throws 422 Unprocessable Entity)
- Error message clearly explains the constraint: "User already has a household. Use the existing household to create tasks."
- Tests validate the constraint (attempt to create 2nd household → exception)
- Frontend disables create form and shows existing household after first creation
- Task creation flow works seamlessly

## Key Decisions Made

| Decision | Choice | Why | Source |
|----------|--------|-----|--------|
| Enforcement mechanism | Service-level check in HouseholdService | Simplest to implement, no schema changes, solves MVP bug immediately; future work can add database-level constraint via HouseholdMember | Plan |
| Error status | HTTP 422 Unprocessable Entity | Aligns with existing ValidationException mapping in GlobalExceptionHandler; allows frontend to distinguish from other errors | Plan |
| Household lifecycle | Permanent (no delete) | Simplest for MVP; prevents data mess; aligns with "household is isolation boundary" design | Plan |
| Frontend UX | Disable create button after first household | Prevents errors before they happen; shows existing household details instead of form | Plan |

## Scope

**In scope:**
- Add `existsByCreatedById()` query method to HouseholdRepository
- Validate in HouseholdService.`createHousehold()` before persisting
- Unit tests for constraint (duplicate prevention + first creation)
- Frontend conditional rendering (create form vs existing household details)
- Error message and HTTP status code

**Out of scope:**
- Full HouseholdMember join table (designed in Phase 1-3 of existing plan; this is a targeted MVP fix)
- Household delete/rename capability
- 2-member partner model implementation (future)
- Task entity changes (they work correctly already)

## Architecture / Approach

**Backend:** Add a single validation check in HouseholdService.`createHousehold()` before persisting. Query the repository to see if the user already has a household; if yes, throw ValidationException.

**Frontend:** Fetch household list on page mount. Conditionally render create form (if list empty) or existing household details (if list not empty). After creation, refetch and re-render.

**No schema changes:** Constraint is enforced at the Java service layer, not the database. Future HouseholdMember implementation will add database-level uniqueness constraint.

## Phases at a Glance

| Phase | What it delivers | Key risk |
|-------|------------------|----------|
| 1. Backend Validation | Service-level constraint + repository query | None — simple, proven approach |
| 2. Unit Tests | Test coverage for duplicate prevention | None — straightforward test cases |
| 3. Frontend UI | Disable form after creation, show existing household | Frontend state management (verify households list is fetched before rendering) |

**Prerequisites:** 
- HouseholdService exists and is tested
- GlobalExceptionHandler is configured (ValidationException → 422)
- Frontend can fetch household list via API

**Estimated effort:** ~2-3 hours (backend validation: 30 min, tests: 1 hour, frontend UI: 45 min, integration testing: 30 min)

## Open Risks & Assumptions

- **Risk:** Frontend doesn't refetch households after creation. **Mitigation:** Include refetch call in `onSuccess` callback after household creation.
- **Assumption:** Existing households in the database are well-formed (at most one per user). True for greenfield MVP; if duplicates exist, constraint only prevents creating more.
- **Assumption:** TaskController `.findFirst()` pattern is acceptable. True for MVP with constraint in place; future work should migrate to explicit household selection (HouseholdMember).

## Success Criteria (Summary)

- Backend: User cannot create 2nd household; receives 422 error with clear message
- Tests: New constraint tests pass; existing tests still pass
- Frontend: Create form hidden after first household; existing household details shown; task creation works
- Integration: E2E flow from registration → household creation → task creation works seamlessly
