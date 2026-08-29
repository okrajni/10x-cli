# Household Schema — One Household Per User Constraint

## Overview

Enforce the "one household per user" constraint across the backend and frontend. Currently, users can create unlimited households, but the system assumes each user has exactly one. This creates a silent bug in TaskController where `.findFirst()` picks the first household if multiple exist. Add validation to prevent multiple household creation, make error handling consistent, and update the UI to prevent users from encountering this constraint.

## Current State Analysis

**What exists:**
- Household entity with `id`, `name`, `createdBy`, `createdAt`, `updatedAt`
- HouseholdService.`createHousehold()` validates name and user existence but **does not check for duplicate households**
- HouseholdRepository.`findByCreatedById(userId)` returns **all** households for a user (can be multiple)
- TaskController.`getUserHouseholdId()` calls `.findFirst()` on the list, silently using the first household if multiple exist
- Tests validate name/access but **do not test the one-household constraint**
- Frontend allows unlimited household creation without feedback

**What's missing:**
- Validation in HouseholdService to prevent 2nd household creation
- Query method to check if user already has a household
- Test coverage for duplicate household prevention
- Frontend UI to disable create button after first household is created
- Clear error message when user attempts to create a 2nd household

**Root Cause:**
The existing plan designed a full HouseholdMember join table (Phases 1-3, marked complete), but the implementation only partially matches—the Household entity lacks the OneToMany relationship to HouseholdMembers. Rather than implement the full design, this plan adds a targeted service-level constraint to prevent the bug.

## Desired End State

After this plan is complete:
- HouseholdService.`createHousehold()` throws a 422 Unprocessable Entity error if user already has a household
- Error message clearly states: "User already has a household. Use the existing household to create tasks."
- HouseholdRepository has a new query method `existsByCreatedById(userId): boolean` for efficient checking
- Unit tests verify the constraint (attempt to create 2nd household throws expected exception)
- Frontend HouseholdCreatePage disables the create button and shows existing household after first creation
- TaskController behavior is unchanged (still works correctly with the constraint in place)
- All existing tests pass; new tests validate the constraint

**Verification:**
- `bun run typecheck` passes
- `bun run lint` passes
- `bun test` passes (including new constraint tests)
- Manual test: register, create household, attempt to create 2nd household → get 422 error with clear message
- Manual test: frontend shows existing household details instead of create button after creation

## What We're NOT Doing

- Implementing the full HouseholdMember join table from the existing plan (that's Phase 1-3; this is a targeted fix for MVP)
- Adding hard-delete or soft-delete of households (permanent model per requirements)
- Changing the 2-member partner model design (that's future work)
- Modifying Task entity relationships (they already work correctly)
- Adding household rename/update capability (out of scope)
- Migrating existing households from other systems (greenfield MVP)

## Implementation Approach

**Constraint enforcement:** Add validation in HouseholdService.`createHousehold()` before persisting. Use the existing `findByCreatedById()` query to check if user already has a household. Throw `ValidationException` (existing exception type used throughout the codebase) with HTTP 422 status (already configured in GlobalExceptionHandler).

**UI feedback:** After user creates their first household, disable the create form and show the existing household details. Frontend queries household list on mount and shows conditional UI based on count.

**No schema changes:** This constraint is enforced at the service layer, not the database level. If future work adds HouseholdMember, this constraint becomes redundant with the database-level unique constraint.

## Critical Implementation Details

**Service-level validation only:** Unlike HouseholdMember (which would enforce at DB via UNIQUE constraint), this uses Java logic in HouseholdService.`createHousehold()`. If a future refactor moves to stored procedures or introduces a bug, multiple households could theoretically exist in the database. The existing TaskController `.findFirst()` pattern will still work (picks the first), but ideally future work should migrate to HouseholdMember as designed in the existing plan.

**Error status code:** ValidationException is mapped to HTTP 422 by GlobalExceptionHandler (lines 14-18). Ensure frontend handles this status code specifically for the "already has household" message; other 422s (null name, invalid user) will have different messages.

**Household retrieval:** TaskController.`getUserHouseholdId()` will never need to `.findFirst()` after this constraint is in place (always exactly one household per user), but don't change TaskController in this plan—the `.findFirst()` is harmless and changing it risks breaking task operations.

## Phase 1: Backend Validation

### Overview

Add validation in HouseholdService to prevent users from creating a second household. Update repository with an efficient query method. Add unit tests for the constraint.

### Changes Required

#### 1. HouseholdService.`createHousehold()` Method

**File:** `src/main/java/com/example/doneyet/service/HouseholdService.java`

**Intent:** Before creating a household, check if the user already has one. If they do, throw an exception instead of persisting.

**Contract:** Modify the `createHousehold(UUID userId, String name)` method (currently lines 27-42) to add a household-count check before persisting. Call a new repository method `existsByCreatedById(userId)` (see below). If true, throw `new ValidationException("User already has a household. Use the existing household to create tasks.")`. The exact line to add is:

```java
if (householdRepository.existsByCreatedById(userId)) {
    throw new ValidationException("User already has a household. Use the existing household to create tasks.");
}
```

Insert this check after user existence validation (line 31) and before name validation (line 32).

#### 2. HouseholdRepository.`existsByCreatedById()` Query Method

**File:** `src/main/java/com/example/doneyet/repository/HouseholdRepository.java`

**Intent:** Provide an efficient query to check if a user already has a household, without fetching all households into memory.

**Contract:** Add a new method to HouseholdRepository:

```java
boolean existsByCreatedById(UUID userId);
```

Spring Data JPA will auto-generate the SQL: `SELECT COUNT(*) > 0 FROM households WHERE created_by = ?`. This is more efficient than fetching all households.

### Success Criteria

#### Automated Verification

- Type checking passes: `bun run typecheck`
- Linting passes: `bun run lint`
- App starts without errors (Spring wires the new repository method)
- HouseholdServiceTest compiles and runs

#### Manual Verification

- Verify repository query works: call `existsByCreatedById(userId)` in tests, confirm returns true/false correctly
- Verify exception is thrown: create a user, create a household, attempt to create a 2nd household, confirm 422 Unprocessable Entity is returned with the correct message

---

## Phase 2: Unit Tests for the Constraint

### Overview

Write tests to verify the constraint is enforced: users cannot create a second household, and the error message is clear.

### Changes Required

#### 1. Test for Duplicate Household Prevention

**File:** `src/test/java/com/example/doneyet/service/HouseholdServiceTest.java`

**Intent:** Verify that attempting to create a second household throws the expected exception.

**Contract:** Add a test method to HouseholdServiceTest:

```java
@Test
public void testCreateSecondHouseholdThrows() {
    // Given: a user with one household already created
    User user = new User("user@example.com", "hashedPassword");
    userRepository.save(user);
    Household first = new Household("First Household", user);
    householdRepository.save(first);
    
    // When: attempting to create a second household
    // Then: ValidationException is thrown with the correct message
    ValidationException exception = assertThrows(
        ValidationException.class,
        () -> householdService.createHousehold(user.getId(), "Second Household")
    );
    assertEquals("User already has a household. Use the existing household to create tasks.", exception.getMessage());
}
```

#### 2. Test for First Household Creation (Sanity Check)

**File:** `src/test/java/com/example/doneyet/service/HouseholdServiceTest.java`

**Intent:** Ensure the constraint doesn't block the first household creation.

**Contract:** Add or update an existing test:

```java
@Test
public void testCreateFirstHouseholdSucceeds() {
    // Given: a user with no households
    User user = new User("user@example.com", "hashedPassword");
    userRepository.save(user);
    
    // When: creating the first household
    HouseholdDto.HouseholdResponse response = householdService.createHousehold(user.getId(), "My Household");
    
    // Then: the household is created successfully
    assertNotNull(response.getHouseholdId());
    assertEquals("My Household", response.getName());
}
```

### Success Criteria

#### Automated Verification

- Type checking passes: `bun run typecheck`
- Linting passes: `bun run lint`
- All HouseholdServiceTest tests pass: `bun test src/test/java/com/example/doneyet/service/HouseholdServiceTest.java`
- New constraint tests pass
- Existing tests still pass (first household creation, name validation, access control)

#### Manual Verification

- Run full test suite: `bun test` — all tests pass
- Verify constraint test output shows the expected exception is thrown
- Verify no regressions in existing household tests

---

## Phase 3: Frontend UI Update

### Overview

Update the frontend to disable the household creation form after the first household is created. Show the existing household details instead of the create form when a household already exists.

### Changes Required

#### 1. HouseholdCreatePage Component Conditional Rendering

**File:** `frontend/src/features/household/pages/HouseholdCreatePage.tsx`

**Intent:** After a user creates their first household, prevent them from seeing the create form. Instead, show the existing household details and a message explaining they can only have one household.

**Contract:** Modify the component to:
- On mount, fetch the user's households via `getUserHouseholdsApi()`
- If households list is not empty (household already exists):
  - Hide the create form
  - Show the existing household name, ID, and a message: "You have one household. Create tasks to get started."
  - Show a button to go to the task dashboard or show household details
- If households list is empty:
  - Show the create form as currently implemented
- After successful creation, refetch the list and re-render to show the existing household view

Pseudo-code outline:
```typescript
const [households, setHouseholds] = useState([]);
const [loading, setLoading] = useState(true);

useEffect(() => {
  const fetchHouseholds = async () => {
    const result = await getUserHouseholdsApi();
    if (result.ok) {
      setHouseholds(result.data);
    }
    setLoading(false);
  };
  fetchHouseholds();
}, []);

return (
  <div>
    {households.length > 0 ? (
      <div>
        <h2>Your Household</h2>
        <p>{households[0].name}</p>
        <p>You have one household. Create tasks to get started.</p>
      </div>
    ) : (
      <HouseholdCreateForm onSuccess={() => refetch()} />
    )}
  </div>
);
```

### Success Criteria

#### Automated Verification

- TypeScript compilation passes: `bun run typecheck`
- Linting passes: `bun run lint`
- Component renders without errors (no null pointer exceptions on empty households)

#### Manual Verification

- Register a new user and navigate to the household creation page — see the create form
- Create a household — form successfully creates household
- After creation, page re-renders and shows the existing household details instead of the create form
- Refresh the page — still shows existing household details, not the create form
- The "create task" flow works seamlessly from the household page

---

## Testing Strategy

### Unit Tests

**Backend:**
- Constraint validation: attempt to create 2nd household → ValidationException thrown
- First household creation: succeeds without error
- Repository query: `existsByCreatedById(userId)` returns true when household exists, false otherwise
- Error message: verify exact wording of exception message (frontend depends on this)

**Frontend:**
- Component renders create form when households list is empty
- Component renders existing household details when households list has items
- Button to navigate to tasks works

### Integration Tests

- E2E flow: register → create household → try to create 2nd household → get 422 error → frontend shows error or refetch block
- E2E flow: register → create household → navigate to tasks → task creation works

### Manual Testing Steps

1. **Backend constraint:**
   - Start app: `bun run dev --backend`
   - Register a user (e.g., `POST /api/auth/register` with email, password)
   - Create first household: `POST /api/household` with `{"name": "My Home"}` → 200 OK
   - Create second household: `POST /api/household` with `{"name": "Second Home"}` → 422 Unprocessable Entity with message "User already has a household. Use the existing household to create tasks."

2. **Frontend flow:**
   - Register a user via web UI
   - Navigate to "Create Household" page — see the form
   - Fill in household name and submit
   - After creation, page shows household details and "You have one household. Create tasks to get started."
   - Refresh page — still shows existing household, not create form
   - Verify "Go to Tasks" button navigates to task creation page

3. **Task creation still works:**
   - After creating a household, create a task via `POST /api/task`
   - Verify task is created in the user's household
   - List tasks via `GET /api/task` — task appears in the list

## Performance Considerations

- `existsByCreatedById()` uses a database COUNT query, efficient even with many households (though users should never have more than one)
- No N+1 query issues (repository method is a single COUNT, not a fetch-all)
- Frontend fetch of households on mount is a single GET call (already implemented in existing code)

## Migration Notes

**No data migration needed:** This is a service-level constraint on new household creation. Existing households in the database are unaffected. If somehow multiple households already exist in the database, the constraint only prevents creating more; existing multiples remain. Future work migrating to HouseholdMember can enforce database-level uniqueness and clean up any existing duplicates.

**Backward compatibility:** All existing API contracts remain unchanged. The new constraint only changes the behavior: users who attempt to create a 2nd household will now get a 422 error instead of succeeding. This is the intended fix.

## References

- Existing plan (Phase 1-3, complete): `context/changes/household-schema/plan.md` (the full HouseholdMember model design)
- GlobalExceptionHandler config: `src/main/java/com/example/doneyet/exception/GlobalExceptionHandler.java` (ValidationException → 422 mapping at line 14-18)
- Spring Data JPA query methods: https://spring.io/projects/spring-data-jpa
- PRD household model: `context/foundation/prd.md`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Backend Validation

#### Automated

- [x] 1.1 Type checking passes — 59fa28c
- [x] 1.2 Linting passes — 59fa28c
- [x] 1.3 App starts without errors — 59fa28c
- [x] 1.4 HouseholdServiceTest compiles — 59fa28c

#### Manual

- [x] 1.5 Repository query existsByCreatedById() works correctly — 59fa28c
- [x] 1.6 Constraint prevents 2nd household creation with correct error message — 59fa28c

### Phase 2: Unit Tests for the Constraint

#### Automated

- [x] 2.1 Type checking passes — 59fa28c
- [x] 2.2 Linting passes — 59fa28c
- [x] 2.3 Duplicate household prevention test passes — 59fa28c
- [x] 2.4 First household creation test passes — 59fa28c
- [x] 2.5 All HouseholdServiceTest tests pass — 59fa28c

#### Manual

- [x] 2.6 Full test suite passes — 59fa28c

### Phase 3: Frontend UI Update

#### Automated

- [x] 3.1 TypeScript compilation passes — 46dd71a
- [x] 3.2 Linting passes — 46dd71a
- [x] 3.3 Component renders without errors — 46dd71a

#### Manual

- [ ] 3.4 Create form shown when no households exist
- [ ] 3.5 Existing household details shown after creation
- [ ] 3.6 Page refresh still shows existing household (not create form)
- [ ] 3.7 Task creation flow works after household exists
