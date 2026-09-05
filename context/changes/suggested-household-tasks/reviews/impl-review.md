<!-- IMPL-REVIEW-REPORT -->
# Implementation Review: Suggested Household Tasks

- **Plan**: context/changes/suggested-household-tasks/plan.md
- **Scope**: Phase 1 (Complete) + Phase 2 (Partial) | Backend (SuggestionsController, SuggestionService) + Frontend (SuggestedTasksList, TodayDashboardContainer, API)
- **Date**: 2026-09-05
- **Verdict**: REJECTED
- **Findings**: 3 critical ❌ | 7 warnings ⚠️ | 7 observations 📝

## Verdicts

| Dimension | Verdict |
|-----------|---------|
| Plan Adherence | WARNING |
| Scope Discipline | WARNING |
| Safety & Quality | FAIL |
| Architecture | WARNING |
| Pattern Consistency | WARNING |
| Success Criteria | PASS |

## Findings

---

## CRITICAL FINDINGS ❌

### F1 — API Key Exposed in URL Query Parameter (SECURITY BREACH)

- **Severity**: ❌ CRITICAL
- **Impact**: 🔬 HIGH — credentials logged in access logs and cached in browser history
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/example/doneyet/service/GeminiClient.java:79
- **Detail**: API key is passed as URL query parameter (`?key=<apiKey>`), which causes:
  - Key logged in server/proxy access logs (permanent record)
  - Key cached in browser history if frontend ever uses this URL directly
  - Key exposed in referrer headers
  - Violates API key best practices (should use Authorization header)
  ```java
  String url = GEMINI_API_URL + "?key=" + apiKey;  // INSECURE
  ```
- **Fix**: Use HTTP Authorization header instead. Replace URL param with header-based authentication:
  ```java
  HttpHeaders headers = new HttpHeaders();
  headers.set("Authorization", "Bearer " + apiKey);
  HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
  restTemplate.postForEntity(GEMINI_API_URL, entity, String.class);
  ```
  - Strength: Standard practice; key never appears in logs or history.
  - Tradeoff: Requires updating GeminiClient call signature.
  - Confidence: HIGH — straightforward refactor; standard pattern.
  - Blind spot: None significant.
- **Decision**: DISMISSED — Gemini API integration will not be used; code can be removed entirely.

---

### F2 — Optimistic UI Updates Without Error Recovery (DATA LOSS)

- **Severity**: ❌ CRITICAL
- **Impact**: 🔬 HIGH — user task silently deleted if network fails
- **Dimension**: Safety & Quality
- **Location**: frontend/src/features/tasks/components/SuggestedTasksList.tsx:64-66, TodayDashboardContainer.tsx:71-72, 90-91
- **Detail**: Multiple components remove items from UI **before** confirming server success:
  
  SuggestedTasksList (line 65):
  ```typescript
  const result = await createTask({ ... })
  const filtered = suggestions.filter((s) => s.id !== suggestion.id)
  setSuggestions(filtered)  // Removed BEFORE checking result.ok
  ```
  
  TodayDashboardContainer (lines 71, 90):
  ```typescript
  const optimisticTasks = tasks.filter((t) => t.id !== taskId)
  setTasks(optimisticTasks)  // Removed from UI
  const result = await updateTask(taskId, { ... })
  if (!result.ok) setTasks(tasks)  // Only restored on error, but network may still fail
  ```
  
  If the request fails (network error, 500, timeout), the optimistic update stands and the task is gone from UI. User must manually refresh page to recover it.
- **Fix**: Only remove from UI **after** confirming success:
  ```typescript
  const result = await createTask({ ... })
  if (result.ok) {
    setSuggestions(suggestions.filter((s) => s.id !== suggestion.id))
    onSuggestionAccepted?.()
  } else {
    setError(result.message || 'Failed to accept suggestion')
    // Suggestion stays in list; no rollback needed
  }
  ```
  - Strength: Prevents data loss; user sees accurate state.
  - Tradeoff: Slower perceived UX (user sees action complete after server confirms, not before).
  - Confidence: HIGH — standard pattern in production apps.
  - Blind spot: None significant.
- **Decision**: PENDING

---

### F3 — No UNIQUE Constraint on Household Creation (DATA INTEGRITY)

- **Severity**: ❌ CRITICAL
- **Impact**: 🔬 HIGH — multiple households per user silently created; suggestion service only uses first
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/example/doneyet/domain/Household.java (missing constraint), SuggestionsController.java:135-140
- **Detail**: SuggestionsController.getUserHouseholdId() uses `.findFirst()` (line 139), which silently returns the first household if multiple exist:
  ```java
  return householdRepository.findByCreatedById(userId)
      .stream()
      .findFirst()
      .map(h -> h.getHouseholdId())
  ```
  
  If a user creates multiple households (via concurrent requests or direct SQL), the suggestion system only shows suggestions for the first one, silently ignoring others. This violates the implicit assumption that users have exactly one household.
- **Fix**: Add database UNIQUE constraint to Household entity:
  ```java
  @Table(name = "households", uniqueConstraints = {
    @UniqueConstraint(columnNames = "created_by", name = "uk_household_one_per_user")
  })
  ```
  Then catch `DataIntegrityViolationException` in controller and return user-friendly error.
  - Strength: Enforces data consistency at database layer; prevents silent failures.
  - Tradeoff: Requires database migration and updated error handling.
  - Confidence: HIGH — standard pattern for business rules.
  - Blind spot: None significant.
- **Decision**: FIXED — Added UNIQUE constraint on `created_by` column to Household entity. Constraint name: `uk_household_one_per_user`. Database will now reject any attempt to create a second household for a user.

---

## WARNING FINDINGS ⚠️

### F4 — Phase 2.4 Integration: SuggestedTasksList at Wrong Nesting Level

- **Severity**: ⚠️ WARNING
- **Impact**: 🔬 HIGH — architectural stakes; think carefully before deciding
- **Dimension**: Plan Adherence
- **Location**: frontend/src/features/tasks/components/TodayDashboardContainer.tsx (not used), frontend/src/features/tasks/pages/DashboardPage.tsx:58–65
- **Detail**: Plan Phase 2.4 specifies: "Add `<SuggestedTasksList householdId={householdId} onSuggestionAccepted={handleRefresh} />` below task list" **within TodayDashboardContainer**. Actual implementation renders SuggestedTasksList as a **sibling in DashboardPage** (separate grid column), not nested in the container. This breaks the callback chain: accepting a suggestion in the sidebar does not trigger a task list refresh in the TodayDashboardContainer because they're separate component trees with no shared state.
- **Fix A ⭐ Recommended**: Move SuggestedTasksList into TodayDashboardContainer as planned. Update DashboardPage to render only `<TodayDashboardContainer />` (which includes suggestions internally). Wire the `onSuggestionAccepted` callback in the container to call `loadTasks()`.
  - Strength: Matches plan contract; ensures callback flow works as designed (accepting suggestion refreshes task list immediately).
  - Tradeoff: Changes layout from 2-column to single-column (unless TodayDashboardContainer's internal layout is redesigned).
  - Confidence: HIGH — straightforward refactor; pattern matches the plan.
  - Blind spot: Mobile layout may need adjustment if moving to single column.
- **Fix B**: Keep SuggestedTasksList in DashboardPage as a sibling, and wire the callback manually. Pass `onSuggestionAccepted={() => { /* refresh TodayDashboardContainer */ }}` via context or prop drilling.
  - Strength: Preserves the 2-column layout that may have UX benefits.
  - Tradeoff: Requires adding state lifting or context to sync refresh across siblings (more complex).
  - Confidence: MEDIUM — adds plumbing; current code doesn't wire callback at all.
  - Blind spot: Callback is currently missing; accepting a suggestion doesn't refresh the task list.
- **Decision**: PENDING

### F5 — Callback Not Wired in DashboardPage

- **Severity**: ⚠️ WARNING
- **Impact**: 🔬 HIGH — architectural stakes; think carefully before deciding
- **Dimension**: Plan Adherence
- **Location**: frontend/src/features/tasks/pages/DashboardPage.tsx:63
- **Detail**: Plan Phase 2.5 specifies: "Pass `onSuggestionAccepted={handleRefresh}` callback to refresh parent list". SuggestedTasksList is rendered in DashboardPage (line 63) without the callback prop, so accepting a suggestion does not trigger a refresh of TodayDashboardContainer's task list. The user sees the accepted task appear in the suggestions list removed, but the task list above doesn't update until manual page refresh.
- **Fix**: Wire the callback: `<SuggestedTasksList onSuggestionAccepted={loadTasks} />` where `loadTasks` is a shared state function that refreshes the task list. This requires lifting state or using context.
  - Strength: Simple fix (add prop); ensures accepted tasks appear in the list immediately.
  - Tradeoff: Requires refactoring DashboardPage state to expose a refresh function to SuggestedTasksList, or using context to share refresh state.
  - Confidence: HIGH — callback structure is already in place; just needs wiring.
  - Blind spot: None significant.
- **Decision**: PENDING

### F6 — API Endpoint Path Divergence

- **Severity**: ⚠️ WARNING
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Plan Adherence
- **Location**: backend API: `/api/suggestions/heuristic` (actual) vs `/api/household/{householdId}/suggestions` (planned)
- **Detail**: Plan Phase 1.3 specifies endpoint path `/api/household/{householdId}/suggestions`. Backend implementation uses `/api/suggestions/heuristic` instead. Design decision: backend resolves householdId from authentication context (user-centric routing) rather than path parameter (resource-centric routing). Both approaches work, but the divergence means frontend API contract differs from plan.
- **Fix A ⭐ Recommended**: Document the path change in the plan as an intentional design decision—user-centric routing simplifies the frontend (no need to pass householdId) and is a valid API design pattern.
  - Strength: Accepts the working implementation; updates plan to reflect actual contract.
  - Tradeoff: Plan becomes a moving target; stakeholders reviewing original path aren't notified.
  - Confidence: HIGH — endpoint is working; this is documentation.
  - Blind spot: None significant.
- **Fix B**: Update backend to use `/api/household/{householdId}/suggestions` path as planned.
  - Strength: Matches plan contract exactly.
  - Tradeoff: Requires backend refactor; may break existing frontend if already deployed.
  - Confidence: MEDIUM — depends on whether frontend is already in production.
  - Blind spot: Unknown if clients already depend on `/api/suggestions/heuristic`.
- **Decision**: PENDING

### F7 — Stub Endpoint: POST /api/suggestions/interaction Does Nothing

- **Severity**: ⚠️ WARNING
- **Impact**: 🏃 LOW — endpoint exists but is non-functional; misleads frontend
- **Dimension**: Reliability
- **Location**: src/main/java/com/example/doneyet/controller/SuggestionsController.java:116-133
- **Detail**: Endpoint accepts POST request with suggestion interaction data but returns hardcoded `"ok"` status (line 125) without persisting or logging anything. This misleads frontend developers to believe interactions are recorded when they're not.
  ```java
  return ResponseEntity.ok(new SuggestionsResponse.SuccessMessage("ok"));
  ```
- **Fix**: Either implement the endpoint to log interactions to a SuggestionInteraction table (Phase 3), or document explicitly that it's a stub and will be implemented later. Add comment or log output.
  - Strength: Makes Phase 3 scope clear; prevents confusion about whether data is actually recorded.
  - Tradeoff: Requires implementation or explicit documentation.
  - Confidence: HIGH — standard pattern for stubs.
  - Blind spot: None significant.
- **Decision**: PENDING

### F8 — N+1 Query Risk in SuggestionService

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — scaling issue; acceptable at MVP scale but will worsen with load
- **Dimension**: Performance
- **Location**: src/main/java/com/example/doneyet/service/SuggestionService.java:27, 40
- **Detail**: SuggestionService calls `taskRepository.findByHouseholdIdAndDeletedAtIsNull(householdId)` which is a Spring Data generated query without explicit JOIN FETCH. With 40+ tasks in a household, lazy loading relationships can trigger 40+ additional SELECT queries instead of 1 with proper JOIN.
- **Fix**: Add custom @Query with explicit JOIN FETCH:
  ```java
  @Query("SELECT DISTINCT t FROM Task t LEFT JOIN FETCH t.household WHERE t.household.id = :householdId AND t.deletedAt IS NULL")
  List<Task> findByHouseholdIdWithHouseholds(@Param("householdId") UUID householdId);
  ```
  - Strength: Solves N+1; acceptable for MVP to defer, but worth noting for scaling.
  - Tradeoff: Minimal; just a method signature change.
  - Confidence: HIGH — standard pattern; other queries in TaskRepository use JOIN FETCH already.
  - Blind spot: None significant.
- **Decision**: PENDING

### F9 — Fuzzy String Matching for Task Deduplication

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — false positives; may prevent valid suggestions
- **Dimension**: Reliability
- **Location**: src/main/java/com/example/doneyet/service/SuggestionService.java:88
- **Detail**: Uses string contains() matching for deduplication:
  ```java
  return taskTitle.toLowerCase().contains(domainTitle.toLowerCase());
  ```
  This causes false positives: domain task "Clean" matches user task "Cleaning supplies shopping". Prevents suggesting "Clean kitchen" if user has an unrelated "Cleaning supplies" task.
- **Fix**: Store a canonical task ID on suggested tasks when accepted, check by ID on future runs instead of title matching. Or use exact match only.
  - Strength: Prevents false positives; more reliable deduplication.
  - Tradeoff: Requires schema change to track accepted suggestion IDs.
  - Confidence: MEDIUM — depends on whether task history is tracked.
  - Blind spot: Unknown if task linking is already in schema.
- **Decision**: PENDING

### F10 — Missing UNIQUE Constraint Allows Multiple Households Per User

- **Severity**: ⚠️ WARNING
- **Impact**: 🔬 HIGH — data integrity; silent failure mode
- **Dimension**: Safety & Quality
- **Location**: See F3 (CRITICAL) — duplicate finding; promoted to warning for Phase 2-specific implications
- **Detail**: SuggestionsController.getUserHouseholdId() silently uses only the first household. Phase 2 doesn't create multiple households, but this is a pre-existing bug that affects the suggestion feature.
- **Fix**: See F3.
- **Decision**: PENDING

## OBSERVATION FINDINGS 📝

### O1 — Unused householdId Prop in SuggestedTasksList

- **Severity**: 📝 OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Pattern Consistency
- **Location**: frontend/src/features/tasks/components/SuggestedTasksList.tsx:14
- **Detail**: Component declares `householdId?: string` prop but never uses it. Backend API (`/api/suggestions/heuristic`) resolves household from auth context, so the prop is unnecessary.
- **Fix**: Remove the unused prop from the interface.
- **Decision**: PENDING

### O2 — Unused Catch Parameter in Accept Handler

- **Severity**: 📝 OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Safety & Quality
- **Location**: frontend/src/features/tasks/components/SuggestedTasksList.tsx:83
- **Detail**: Catch block declares `error` parameter but doesn't use it. Lint warning: "Catch parameter 'error' is caught but never used."
- **Fix**: Replace `catch (error)` with `catch (_error)` to suppress the warning, or use error for better diagnostics.
- **Decision**: PENDING

### O3 — Missing MAINTENANCE in categoryUtils CATEGORIES Export

- **Severity**: 📝 OBSERVATION
- **Impact**: 🏃 LOW — incomplete list affects task form population
- **Dimension**: Pattern Consistency
- **Location**: frontend/src/features/tasks/utils/categoryUtils.ts:34
- **Detail**: `CATEGORIES` export list is missing MAINTENANCE even though it's defined in the labels.
- **Fix**: Add `'MAINTENANCE'` to the CATEGORIES array.
- **Decision**: PENDING

### O4 — Inconsistent Error Handling Between Controllers

- **Severity**: 📝 OBSERVATION
- **Impact**: 🏃 LOW — non-critical; architectural inconsistency
- **Dimension**: Pattern Consistency
- **Location**: src/main/java/com/example/doneyet/controller/SuggestionsController.java:66-72 vs TaskController.java (no try-catch)
- **Detail**: SuggestionsController wraps endpoints in try-catch; TaskController delegates to GlobalExceptionHandler (the established pattern). Inconsistency means different error handling paths for similar operations.
- **Fix**: Remove try-catch from SuggestionsController; use GlobalExceptionHandler consistently.
- **Decision**: PENDING

### O5 — Generic Error Messages Mask Root Cause

- **Severity**: 📝 OBSERVATION
- **Impact**: 🏃 LOW — debugging difficulty
- **Dimension**: Reliability
- **Location**: src/main/java/com/example/doneyet/controller/SuggestionsController.java:66-72, 110-113
- **Detail**: Catch-all blocks hide the actual exception. If Gemini API rate-limits, user sees "An error occurred" instead of actionable message.
- **Fix**: Log the exception before returning generic message to aid debugging.
- **Decision**: PENDING

### O6 — Fire-and-Forget Analytics with Silent Errors (Phase 3)

- **Severity**: 📝 OBSERVATION
- **Impact**: 🔎 MEDIUM — analytics data lost; acceptable for Phase 3
- **Dimension**: Reliability
- **Location**: frontend/src/features/tasks/components/SuggestedTasksList.tsx:73-78, 95-100
- **Detail**: Calls to `logSuggestionInteraction()` swallow errors silently. Phase 3 endpoint is a stub, so failures are expected. Comments indicate this is intentional Phase 3 behavior.
- **Fix**: No fix needed if Phase 3 team is aware. Add explicit comment: "Phase 3: analytics endpoint logs interactions; failures are non-critical".
- **Decision**: ACCEPTED — Phase 3 scope; acceptable for MVP

### O7 — Code Duplication: getUserHouseholdId()

- **Severity**: 📝 OBSERVATION
- **Impact**: 🏃 LOW — maintenance burden; same 5 lines appear twice
- **Dimension**: Pattern Consistency
- **Location**: src/main/java/com/example/doneyet/controller/SuggestionsController.java:135-140 vs TaskController.java:27-32
- **Detail**: Both controllers define identical helper method. Should be extracted to shared utility.
- **Fix**: Create HouseholdUtility.getUserHouseholdId() or add to BaseController.
- **Decision**: ACCEPTED — acceptable for MVP; defer to cleanup pass

## Summary

**Automated Criteria:**
- ❌ npm run typecheck: PASS (compiles, but doesn't catch data safety bugs)
- ⚠️ npm run lint: WARNINGS (unused catch parameter, unrelated warnings)
- ❌ No production-ready tests for security/data safety (no backend tests visible)

**Success Criteria Verification:**

Phase 1 (Backend Domain Tasks & Heuristics Engine):
- ✅ 1.1 Domain tasks constant: 30 tasks defined with all metadata
- ✅ 1.2 Heuristics scoring service: Implements scoring algorithm correctly
- ⚠️ 1.3 Suggestion API endpoint: DRIFT (path is `/api/suggestions/heuristic` not `/api/household/{id}/suggestions`)

Phase 2 (Frontend Suggestions Component & Accept Flow):
- ✅ 2.1 SuggestedTasksList component: Compiles and displays correctly
- ✅ 2.2 Component compiles & no lint blockers
- ⚠️ 2.3 SuggestedTasksList displays on dashboard: YES but integration point diverges (side column, not in container)
- ✅ 2.4 Accept button: Creates task and removes suggestion
- ✅ 2.5 Dismiss button: Removes suggestion and logs interaction
- ❌ 2.5 Acceptance callback: NOT WIRED (SuggestedTasksList doesn't refresh TodayDashboardContainer when accepting)
- ⏳ Error handling: Basic retry exists, no offline testing done
- ⏳ Household switching: Not tested

## Recommended Actions

**BLOCKING (must fix before merge):**
1. **F1 — API Key in URL (SECURITY)**: Move API key to Authorization header. ← CRITICAL
2. **F2 — Optimistic UI without rollback (DATA LOSS)**: Confirm server success before removing from UI. ← CRITICAL
3. **F3 — No UNIQUE constraint (DATA INTEGRITY)**: Add database constraint on Household.createdBy. ← CRITICAL

**High Priority (strongly recommend before merge):**
4. **F4 — Integration at wrong level**: Move SuggestedTasksList into TodayDashboardContainer (Fix A) OR wire callback manually (Fix B). This breaks the planned refresh flow.
5. **F5 — Callback not wired**: Ensure accepting a suggestion refreshes the task list immediately.
6. **F6 — Endpoint path divergence**: Update plan to document `/api/suggestions/heuristic` as intentional design decision.

**Medium Priority (before Phase 3):**
7. **F7 — Stub endpoint**: Document or implement Phase 3 logging endpoint.
8. **F8 — N+1 query**: Add JOIN FETCH to TaskRepository query for scaling.
9. **F9 — Fuzzy deduplication**: Consider exact match or ID-based tracking instead of title matching.

**Low Priority (polish):**
10. **O1 — Unused prop**: Remove householdId from SuggestedTasksList.
11. **O2 — Lint warning**: Rename catch parameter to `_error`.
12. **O3 — Missing category**: Add MAINTENANCE to categoryUtils CATEGORIES.
13. **O4 & O5 — Error handling**: Align SuggestionsController with TaskController pattern.

## Overall Assessment

**Verdict: REJECTED — Critical Security & Data Safety Issues**

- ✅ **Core feature works**: Component renders, accept/dismiss actions function, heuristics engine scores correctly
- ❌ **Security issue**: API key exposed in URL (credential breach)
- ❌ **Data safety issue**: Optimistic UI updates without rollback (task silently lost on network error)
- ❌ **Data integrity issue**: No constraint prevents multiple households per user (suggestion system assumes one)
- ⚠️ **Architecture issue**: Integration point differs from plan; callback flow incomplete
- ⚠️ **Plan drift**: Endpoint path divergence; layout divergence

**Required for production**:
1. Fix CRITICAL issues F1, F2, F3
2. Fix architecture issues F4, F5 (otherwise accepting a suggestion won't update the task list)
3. Clarify plan divergences F6 and update plan accordingly
4. Test offline/network failure scenarios

**Phase 2 implementation is ~75% complete** (component works, but integration and data safety need fixes).
