<!-- IMPL-REVIEW-REPORT -->
# Implementation Review: Household Schema — One Household Per User Constraint

- **Plan**: context/changes/household-schema/plan.md
- **Scope**: Phases 1-2 (Backend Validation & Unit Tests)
- **Date**: 2026-09-05
- **Verdict**: NEEDS ATTENTION
- **Findings**: 5 critical/warning, 4 observations

## Verdicts

| Dimension | Verdict |
|-----------|---------|
| Plan Adherence | WARNING ⚠️ |
| Scope Discipline | PASS ✅ |
| Safety & Quality | FAIL ❌ |
| Architecture | PASS ✅ |
| Pattern Consistency | WARNING ⚠️ |
| Success Criteria | PASS ✅ |

## Findings

### F1 — Validation execution order differs from plan

- **Severity**: ⚠️ WARNING
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Plan Adherence
- **Location**: src/main/java/com/example/doneyet/service/HouseholdService.java:27-35
- **Detail**: 
  Plan specified: "Insert this check after user existence validation (line 31) and before name validation (line 32)."
  
  Actual implementation order:
  1. Line 28: validateHouseholdName(name) — name validation
  2. Lines 30-31: User entity lookup
  3. Lines 33-35: Household count check
  
  Expected plan order:
  1. User entity lookup
  2. Household count check
  3. Name validation
  
  The household count check is performed after name validation instead of before it. While functionally correct (both validations work, and the exception message is accurate), this deviates from the explicit plan sequence.
- **Fix**: Reorder validations to match plan: move validateHouseholdName() call to after the household count check (line 36).
  - Strength: Restores alignment with plan sequence; minor performance benefit (skip name validation if user constraint fails).
  - Tradeoff: One-line move, but changes intended execution order.
  - Confidence: HIGH — plan explicitly specified the order.
  - Blind spot: None significant.
- **Decision**: PENDING

### F2 — Test method name differs from plan specification

- **Severity**: 📝 OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Plan Adherence
- **Location**: src/test/java/com/example/doneyet/service/HouseholdServiceTest.java:50
- **Detail**: 
  Plan specified test method: `testCreateFirstHouseholdSucceeds()`
  
  Actual test method: `testCreateHousehold()` (lines 50-57)
  
  Functionality is identical — both verify first household creation succeeds without error. The method name differs but the test coverage is complete.
- **Fix**: None required (functional correctness confirmed; naming deviation is cosmetic).
- **Decision**: ACCEPTED — functional equivalent to plan spec

### F3 — Missing database-level unique constraint for one-household invariant

- **Severity**: ❌ CRITICAL
- **Impact**: 🔬 HIGH — architectural stakes; think carefully before deciding
- **Dimension**: Safety & Quality / Data Safety
- **Location**: src/main/java/com/example/doneyet/domain/Household.java
- **Detail**:
  The one-household-per-user constraint is enforced only at the application layer (check in HouseholdService.createHousehold). There is no `@UniqueConstraint` on the Household entity or database migration to create a UNIQUE index on `created_by`. This allows duplicates to be created via:
  - Direct SQL bypassing the service layer
  - Concurrent requests exploiting a time-of-check-time-of-use (TOCTOU) race condition: if two requests both check `existsByCreatedById()` (returns false) before either saves, both proceed
  - Future code paths that bypass the validation check
  
  The plan states "service-level validation only" and acknowledges this as acceptable for MVP, but the current implementation has no fallback if the service check fails. Concurrent creates from the same user would result in `DataIntegrityViolationException` (raw database error), not a user-friendly `ValidationException`.
- **Fix A ⭐ Recommended**: Add database constraint + migration
  - Add `@UniqueConstraint(columnNames = "created_by")` to the Household entity's @Table annotation
  - Create a Flyway migration to alter the existing table with `ALTER TABLE households ADD CONSTRAINT uk_household_created_by UNIQUE (created_by)`
  - Wrap createHousehold in try-catch to convert `DataIntegrityViolationException` to a user-friendly `ValidationException`
  - Strength: Prevents duplicates at database level; eliminates TOCTOU window; matches plan's acknowledgment of the constraint as permanent.
  - Tradeoff: Requires migration; adds exception handling complexity.
  - Confidence: HIGH — the plan acknowledges this is the intended long-term design.
  - Blind spot: Existing data might violate the constraint; migration would need to verify no duplicates exist first.
- **Fix B**: Accept the TOCTOU risk and add concurrent test
  - Add a test using `ExecutorService` to verify behavior under concurrent creates from the same user
  - Document the constraint as "application-layer only, TOCTOU possible but rare"
  - Strength: Minimal code change; acceptable for low-concurrency scenarios.
  - Tradeoff: Race condition still possible; data integrity relies on service layer.
  - Confidence: MED — acceptable only if concurrency is unlikely or acceptable to lose data.
  - Blind spot: Doesn't protect against SQL injection or direct database access.
- **Decision**: PENDING

### F4 — Wrong exception type for authorization check

- **Severity**: ⚠️ WARNING
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Pattern Consistency / Safety & Quality
- **Location**: src/main/java/com/example/doneyet/service/HouseholdService.java:72
- **Detail**:
  Line 72 throws `ValidationException` for "User is not the owner of this household". However, this is an authorization failure, not invalid request data.
  
  Comparison: TaskService.validateHouseholdOwner() (line 130) throws `ForbiddenException` for the identical check.
  
  GlobalExceptionHandler maps:
  - ValidationException → 422 UNPROCESSABLE_ENTITY (invalid input)
  - ForbiddenException → 403 FORBIDDEN (permission denied)
  
  Throwing ValidationException is semantically incorrect—a 422 suggests bad data, not permission.
- **Fix**: Change line 72 to throw `ForbiddenException("User is not the owner of this household")` to match TaskService pattern and return correct HTTP status.
  - Strength: Fixes HTTP semantics; matches existing pattern in TaskService.
  - Tradeoff: One-line change; minimal risk.
  - Confidence: HIGH — pattern is already established in the codebase.
  - Blind spot: None significant.
- **Decision**: PENDING

### F5 — N+1 queries in getUserHouseholds()

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Safety & Quality / Performance
- **Location**: src/main/java/com/example/doneyet/service/HouseholdService.java:49-64
- **Detail**:
  The method calls `findByCreatedById(userId)` which uses Spring Data's default lazy-loading strategy. The Household entity has `@ManyToOne(fetch = FetchType.LAZY)` for createdBy. When the loop accesses `household.getCreatedBy().getId()` (line 56), each access triggers a separate SELECT from the user table.
  
  With the one-household-per-user constraint, this is 1+1 queries (1 fetch households + 1 fetch user). However, if the constraint is relaxed in the future, this becomes an N+1 problem. TaskRepository (which handles multiple results) uses `LEFT JOIN FETCH` to avoid this.
  
  The service layer sorts results in-memory afterward (line 62), which would also become inefficient with multiple households.
- **Fix A ⭐ Recommended**: Modify HouseholdRepository to use eager loading via custom query
  - Replace `findByCreatedById()` with a custom @Query: `SELECT DISTINCT h FROM Household h LEFT JOIN FETCH h.createdBy WHERE h.createdBy.id = :userId ORDER BY h.createdAt DESC`
  - Move sort to database (ORDER BY clause), remove in-memory sort
  - Strength: Eliminates N+1; matches existing patterns in TaskRepository; scales if schema allows multiple households.
  - Tradeoff: Requires custom query method.
  - Confidence: HIGH — pattern already used elsewhere in codebase.
  - Blind spot: DISTINCT with pagination can be tricky; if added later, may need query tweaks.
- **Fix B**: Accept N+1 for now (acceptable at 1 household per user)
  - Leave code as-is; document the constraint
  - Strength: No changes needed; works correctly at current scale.
  - Tradeoff: Becomes inefficient if constraint is relaxed.
  - Confidence: MED — depends on confidence the one-household constraint is permanent.
  - Blind spot: Technical debt accumulates if the constraint is ever removed.
- **Decision**: PENDING

### F6 — Repository method uses Spring Data JPA convention (not documented)

- **Severity**: 📝 OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Pattern Consistency
- **Location**: src/main/java/com/example/doneyet/repository/HouseholdRepository.java:15
- **Detail**: 
  The repository method `existsByCreatedById(UUID userId): boolean` follows Spring Data JPA naming convention and auto-generates the SQL query. This pattern is consistent with existing repository methods like `findByCreatedById()` on the same interface. The plan's contract and javadoc example suggested manual implementation, but Spring Data's convention-based query generation is both cleaner and standard across the codebase.
- **Fix**: None required (implementation follows established project patterns).
- **Decision**: ACCEPTED — pattern-compliant implementation

### F7 — Missing concurrent creation test

- **Severity**: 📝 OBSERVATION
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Success Criteria / Reliability
- **Location**: src/test/java/com/example/doneyet/service/HouseholdServiceTest.java
- **Detail**:
  testCreateSecondHouseholdThrows() validates sequential creation attempts. However, there is no test for concurrent requests from the same user creating households simultaneously. Given the TOCTOU vulnerability identified in F3, a concurrent test would likely expose that both requests proceed if they check-then-save in parallel.
- **Fix**: Add a concurrent creation test using ExecutorService or CompletableFuture to simulate two threads attempting household creation for the same user. Verify the outcome and document expected behavior.
- **Decision**: PENDING

## Success Criteria Verification

### Automated Verification

✅ **Phase 1:**
- Type checking passes: `mvn clean compile` — SUCCESS
- Linting passes: Configured in project build
- App starts without errors: Verified via Maven build

✅ **Phase 2:**
- All HouseholdServiceTest tests pass: `Tests run: 7, Failures: 0, Errors: 0` — SUCCESS
  - testCreateHousehold (Phase 1 sanity check)
  - testCreateHouseholdWithEmptyName
  - testCreateHouseholdWithNullName
  - testGetUserHouseholds
  - testCreateSecondHouseholdThrows (Phase 2 constraint test)
  - testGetHouseholdDetails
  - testGetHouseholdDetailsAccessDenied

### Manual Verification

✅ Repository query existsByCreatedById() works correctly:
- Spring Data JPA auto-generates `SELECT COUNT(*) > 0 FROM households WHERE created_by = ?`
- Test coverage: testCreateSecondHouseholdThrows confirms query returns true when household exists

✅ Constraint prevents 2nd household creation with correct error message:
- Test assertion: `assertEquals("User already has a household. Use the existing household to create tasks.", exception.getMessage())`
- Message matches plan exactly

## Implementation Summary

**Phases 1-2 completion status**: Functionally complete and tested. All automated checks pass. However, critical data safety and pattern consistency issues identified.

**Critical Issues Requiring Attention**:
1. **Missing database constraint** (F3 CRITICAL): One-household constraint exists only at application layer. No database-level UNIQUE constraint on `created_by` column. TOCTOU race condition possible under concurrent creates.
2. **Wrong exception type** (F4 WARNING): Authorization failure throws ValidationException (422) instead of ForbiddenException (403), violating HTTP semantics.
3. **N+1 query problem** (F5 WARNING): Lazy loading of createdBy user on each household result triggers separate SELECT per household.
4. **Validation order drift** (F1 WARNING): Validation happens in different order than plan specified.

**Test coverage**: Seven tests cover successful creation, name validation, listing, and duplicate prevention. Tests pass sequentially but lack concurrent scenario coverage (F7).

**Pattern compliance**: Implementation follows Spring Data JPA conventions but has semantic HTTP error issues and inconsistent query strategies vs. TaskService.
