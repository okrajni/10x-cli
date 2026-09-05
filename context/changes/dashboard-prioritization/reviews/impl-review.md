<!-- IMPL-REVIEW-REPORT -->
# Implementation Review: Dashboard Prioritization & Sorting

- **Plan**: context/changes/dashboard-prioritization/plan.md
- **Scope**: Phases 1–3 (All phases, manual verification pending)
- **Date**: 2026-09-05
- **Verdict**: NEEDS ATTENTION
- **Findings**: 2 critical, 8 warnings, 3 observations (13 total)

## Verdicts

| Dimension | Verdict | Status |
|-----------|---------|--------|
| Plan Adherence | WARNING | ⚠️ Sorting logic drift; 11/12 requirements met |
| Scope Discipline | PASS | ✅ No extra scope except minor features (SuggestedTasksList as bonus) |
| Safety & Quality | FAIL | ❌ 2 critical issues (data safety, debug logs), 6 warnings |
| Architecture | PASS | ✅ Component boundaries clean, pattern-compliant |
| Pattern Consistency | WARNING | ⚠️ Date utilities duplicated, error handling patterns inconsistent |
| Success Criteria | WARNING | ⚠️ Automated checks pass; manual verification unchecked |

---

## CRITICAL FINDINGS ❌

### F1 — Stale Closure in Optimistic Updates

- **Severity**: ❌ CRITICAL
- **Impact**: 🔬 HIGH — architectural stakes; think carefully before deciding
- **Dimension**: Safety & Quality
- **Location**: [frontend/src/features/tasks/components/TodayDashboardContainer.tsx](frontend/src/features/tasks/components/TodayDashboardContainer.tsx#L71-L114)
- **Detail**: 
  Optimistic state updates capture `tasks` from closure at handler definition time. When rollback occurs on error, `setTasks(tasks)` restores stale state. Race condition: if user completes Task A (removed from state), then deletes Task B (fails), the rollback of Task B's delete restores the pre-delete `tasks` snapshot, losing Task A's completed state.
  
  Example sequence:
  1. User clicks complete on Task A → removed from state
  2. User clicks delete on Task B → removed from state  
  3. Task B delete fails → `setTasks(tasks)` restores tasks from line 90's closure (pre-delete)
  4. Task A's completed state lost (not in captured snapshot)

- **Fix**: Capture state snapshot at moment of action, not from closure:
  - Strength: Eliminates race condition entirely; every action has its own before/after pair.
  - Tradeoff: Requires updating all four handlers (handleComplete, handleDelete, handleMoveToTomorrow); minimal code change (~2 lines per handler).
  - Confidence: HIGH — this is a proven pattern for optimistic updates across React codebases.
  - Blind spot: Concurrent handlers could still race if not careful; but snapshots will prevent state loss.

- **Decision**: PENDING

---

### F2 — Unremoved Debug Console Logs in Production Code

- **Severity**: ❌ CRITICAL
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Safety & Quality
- **Location**: [frontend/src/features/tasks/components/SuggestedTasksList.tsx](frontend/src/features/tasks/components/SuggestedTasksList.tsx#L24,L54,L66,L69,L72,L88,L92)
- **Detail**: 
  Seven console.log/console.error statements remain in production code (lines 24, 54, 66, 69, 72, 88, 92). These expose internal logic, API responses, and state mutations to browser console, potentially leaking sensitive information to end users and debugging tools. Line 66 logs full API response including success/error payloads.

- **Fix**: Remove all console statements.
  - Strength: Eliminates information leakage; lines are unnecessary for production.
  - Tradeoff: None — pure cleanup.
  - Confidence: HIGH — console logs are development artifact, not production code.
  - Blind spot: None significant.

- **Decision**: FIXED

---

## WARNING FINDINGS ⚠️

### F3 — Unsafe Date Handling with Empty String Fallbacks

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Safety & Quality
- **Location**: [frontend/src/features/tasks/components/TodayDashboardContainer.tsx](frontend/src/features/tasks/components/TodayDashboardContainer.tsx#L37-L41,L48,L62)
- **Detail**: 
  Code creates Date objects from potentially empty strings, resulting in invalid dates (epoch: Jan 1, 1970) for sorting. Lines 48 and 62 use `new Date(a.dueDate ?? '').getTime()`. Empty string parses to epoch date, which sorts to the beginning of the list instead of the end, causing incorrect display order. Similar issue in SuggestedTasksList.tsx line 53.

- **Fix**: Use existing getTodayDate() utility and safe date comparison via string format:
  - Strength: Eliminates date parsing uncertainty; leverages existing utility; string comparison for YYYY-MM-DD is timezone-safe and deterministic.
  - Tradeoff: Requires importing recurrenceUtils; ~3 lines changed per file.
  - Confidence: HIGH — getTodayDate() already used in recurrenceUtils and tested.
  - Blind spot: Assumes dueDate field always contains valid ISO date per schema (true per Task interface).

- **Decision**: PENDING

---

### F4 — Unused Variable Suggests Incomplete Code

- **Severity**: ⚠️ WARNING
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Code Quality
- **Location**: [frontend/src/features/tasks/pages/TaskListPage.tsx](frontend/src/features/tasks/pages/TaskListPage.tsx#L40-L42)
- **Detail**: 
  Variable `task` is found but never used. Early return `if (!task) return` suggests developer intended to validate task existence but left implementation incomplete. Pattern incomplete:
  ```typescript
  const task = tasks.find((t) => t.id === taskId)
  if (!task) return  // Early return but nothing done
  ```

- **Fix A ⭐ Recommended**: Implement validation (catches edge cases):
  - Strength: Adds robustness; guards against missing task before API call.
  - Tradeoff: One extra conditional check per action.
  - Confidence: HIGH — validation pattern used throughout app.
  - Blind spot: None significant.

- **Fix B**: Remove unused variable (simplify):
  - Strength: Removes dead code; keeps logic simple if validation isn't needed.
  - Tradeoff: Loses defensive check; API call might fail obscurely if task not found locally.
  - Confidence: MEDIUM — depends whether local list is always in sync with backend.
  - Blind spot: Backend could have deleted a task that's still in local state.

- **Decision**: FIXED (Fix A — enhanced validation to prevent double-complete)

---

### F5 — Inefficient Date Parsing in Sort Operations

- **Severity**: ⚠️ WARNING
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Performance / Reliability
- **Location**: [frontend/src/features/tasks/pages/TaskListPage.tsx](frontend/src/features/tasks/pages/TaskListPage.tsx#L141)
- **Detail**: 
  `.sort()` creates new Date objects for every comparison. This is O(n log n) with overhead from Date object allocation. Also mutates original array instead of sorting a copy. Line creates new Date for each comparison during sort:
  ```typescript
  displayTasks.sort((a, b) => new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime())
  ```

- **Fix**: Parse dates once; use string comparison for YYYY-MM-DD format:
  - Strength: Eliminates allocation overhead; string comparison is deterministic and faster; avoids mutation.
  - Tradeoff: Minor refactor (~2 lines).
  - Confidence: HIGH — string comparison for ISO dates is standard pattern.
  - Blind spot: None significant.

- **Decision**: FIXED (string comparison via localeCompare, array copy to avoid mutation)

---

### F6 — Timezone-Unsafe Date Comparison

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Reliability
- **Location**: [frontend/src/shared/components/TaskCard.tsx](frontend/src/shared/components/TaskCard.tsx#L25-L26)
- **Detail**: 
  Date comparison doesn't account for timezone. Task due "2026-09-05" in UTC might display as overdue or not depending on user's local timezone and current time. Line 26:
  ```typescript
  const isOverdue = dueDate < new Date() && !isCompleted
  ```
  Compares ISO string (which is date-only) against current Date object (which includes time and timezone), leading to off-by-one errors.

- **Fix**: Use date-only string comparison (timezone-agnostic):
  - Strength: Timezone-independent; deterministic; aligns with backend date semantics (dueDate is LocalDate, not Instant).
  - Tradeoff: Minor refactor (~2 lines).
  - Confidence: HIGH — ISO date string comparison is standard for date-only fields.
  - Blind spot: Assumes dueDate field in Task is always date-only (true per schema).

- **Decision**: SKIPPED

---

### F7 — Fire-and-Forget Promise with Silent Failure

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Reliability
- **Location**: [frontend/src/features/tasks/components/SuggestedTasksList.tsx](frontend/src/features/tasks/components/SuggestedTasksList.tsx#L80-L85,L104-L109)
- **Detail**: 
  Analytics logging calls swallow errors silently with no logging or retry mechanism. While intentional per comments, this makes impossible to debug if endpoint fails. Lines:
  ```typescript
  logSuggestionInteraction({ suggestionId: suggestion.id, action: 'accepted' })
    .catch(() => { /* Silent failure */ })
  ```

- **Fix A ⭐ Recommended**: Add minimal observability:
  - Strength: Allows detection of logging failures without disrupting UX; uses conditional logging for dev environment.
  - Tradeoff: Minimal; a single console.warn line.
  - Confidence: HIGH — dev-mode logging is standard for observability.
  - Blind spot: Production failures still silent; Phase 3 task to integrate error tracking (Sentry).

- **Fix B**: Remove logging entirely until Phase 3:
  - Strength: Defers problem to explicit Phase 3 scope; keeps code simple.
  - Tradeoff: Loses data collection until Phase 3 implementation.
  - Confidence: MEDIUM — depends on whether analytics data is needed now.
  - Blind spot: If data should be collected now, this defers until later.

- **Decision**: SKIPPED

---

### F8 — Incomplete Loading State in DashboardPage

- **Severity**: ⚠️ WARNING
- **Impact**: 🔬 HIGH — architectural stakes; think carefully before deciding
- **Dimension**: Reliability
- **Location**: [frontend/src/features/tasks/pages/DashboardPage.tsx](frontend/src/features/tasks/pages/DashboardPage.tsx#L13-L22)
- **Detail**: 
  If `setCurrentHousehold` is never called, loading spinner shows indefinitely with no timeout or fallback. Lines 20–22:
  ```typescript
  if (households && households.length > 0 && !currentHousehold) {
    return <Spinner label="Loading household data" />  // ← No timeout
  }
  ```

- **Fix**: Auto-select first household or add timeout:
  - Strength: Prevents indefinite loading; auto-selection is UX best practice for single-household users.
  - Tradeoff: Requires adding useEffect hook to DashboardPage; ~5 lines.
  - Confidence: HIGH — auto-selection pattern standard across dashboards.
  - Blind spot: Multi-household users without explicit selection will get auto-selected; may need to validate if this is desired behavior.

- **Decision**: SKIPPED

---

### F9 — PII Display in UI Subtitle

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Security/Privacy
- **Location**: [frontend/src/features/tasks/pages/DashboardPage.tsx](frontend/src/features/tasks/pages/DashboardPage.tsx#L31)
- **Detail**: 
  User email is displayed as subtitle in page heading. Violates organization policy "Never include PII in the responses." Email visible in UI, error logs, screenshots, and analytics. Line 31:
  ```typescript
  <PageHeading title="Welcome back" subtitle={user?.email} />
  ```

- **Fix A ⭐ Recommended**: Use first name or display name instead of email:
  - Strength: Maintains friendly greeting without exposing PII; aligns with organization policy.
  - Tradeoff: Requires user profile data (firstName) which may need to be added to user schema if missing.
  - Confidence: HIGH — first-name greeting is standard UX pattern.
  - Blind spot: If firstName not available, fallback to generic greeting.

- **Fix B**: Remove subtitle entirely:
  - Strength: Eliminates PII exposure completely; simplifies UI.
  - Tradeoff: Loses personalization.
  - Confidence: HIGH — simple and safe.
  - Blind spot: None significant.

- **Decision**: SKIPPED

---

## OBSERVATION FINDINGS 📝

### F10 — Unused Component Prop

- **Severity**: 📝 OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Code Quality
- **Location**: [frontend/src/features/tasks/components/SuggestedTasksList.tsx](frontend/src/features/tasks/components/SuggestedTasksList.tsx#L14,L18)
- **Detail**: 
  `householdId?: string` prop declared in interface but never destructured or used. Suggests incomplete implementation or leftover prop from refactoring.

- **Fix**: Remove unused prop:
  ```typescript
  interface SuggestedTasksListProps {
    onSuggestionAccepted?: () => void
  }
  ```

- **Decision**: SKIPPED

---

### F11 — Redundant Null Coalescing Chain

- **Severity**: 📝 OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Code Quality
- **Location**: [frontend/src/features/tasks/components/TodayDashboardContainer.tsx](frontend/src/features/tasks/components/TodayDashboardContainer.tsx#L37)
- **Detail**: 
  Unnecessary null coalescing at end of chain: `new Date().toISOString().split('T')[0] ?? ''`. The split is redundant since `getTodayDate()` utility already exists and handles this.

- **Fix**: Replace with utility function call (also fixes F3):
  ```typescript
  import { getTodayDate } from '../utils/recurrenceUtils';
  const today = getTodayDate();
  ```

- **Decision**: SKIPPED

---

### F12 — Inconsistent Error Handling Patterns

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Pattern Consistency
- **Location**: Cross-file pattern issue (TodayDashboardContainer.tsx, TaskListPage.tsx use `error: string | null`; TaskCreatePage, TaskEditPage use `message: { type, text }`)
- **Detail**: 
  Two different error/message state patterns across codebase:
  - Pattern A: `const [error, setError] = useState<string | null>(null)` (TodayDashboardContainer, TaskListPage)
  - Pattern B: `const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null)` (TaskCreatePage, TaskEditPage)
  
  Inconsistency makes maintenance harder and reduces codebase cohesion.

- **Fix**: Standardize on Pattern B (used in TaskCreatePage/TaskEditPage as it's more extensible):
  - Strength: Single pattern for all pages; easier to maintain; supports success + error messages.
  - Tradeoff: Requires updating TodayDashboardContainer.tsx and TaskListPage.tsx error handling (~3-5 lines per file).
  - Confidence: HIGH — Pattern B is already used successfully in other pages.
  - Blind spot: None significant.

- **Decision**: SKIPPED

---

### F13 — Missing Shared Date Utility Functions

- **Severity**: 📝 OBSERVATION
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Pattern Consistency
- **Location**: Cross-file (TodayDashboardContainer.tsx, SuggestedTasksList.tsx, TaskCard.tsx, TaskListPage.tsx)
- **Detail**: 
  Date formatting and parsing logic duplicated across multiple files:
  - TodayDashboardContainer line 37: `new Date().toISOString().split('T')[0]`
  - SuggestedTasksList line 53: `new Date().toISOString().split('T')[0]`
  - TaskCard line 25: `new Date(task.dueDate) < new Date()`
  - TaskListPage line 141: `new Date(a.dueDate).getTime()`
  
  Existing utility `getTodayDate()` in `recurrenceUtils.ts` is underutilized. Creates maintenance burden when date logic changes.

- **Fix**: Expand `recurrenceUtils.ts` with additional utilities:
  - Strength: Centralizes date logic; single source of truth; easier to maintain; reduces duplication by ~10 lines across codebase.
  - Tradeoff: Requires creating new utility functions (~10 lines); updating 4 files to import and use them.
  - Confidence: HIGH — pattern already established with getTodayDate() utility.
  - Blind spot: None significant.

- **Decision**: SKIPPED

---

## Triage Summary

- **Fixed**: F2 (debug logs), F4 (validation), F5 (date parsing) — 3 findings
- **Pending**: F1 (stale closure), F3 (date handling) — 2 findings  
- **Skipped**: F6–F13 — 8 findings

---

## Summary of Changes

### All Plan Requirements Met (11/12)
- ✅ TodayDashboardContainer component created with filtering and sorting
- ✅ DashboardPage integration below household selector
- ✅ Task action callbacks (complete, delete, move-to-tomorrow)
- ✅ Refresh button implemented
- ✅ Empty state messaging
- ✅ Error recovery with retry
- ✅ Responsive mobile layout
- ✅ Overdue visual indicators (red styling, warning icon)
- ✅ Loading states managed
- ✅ API integration correct
- ⚠️ **DRIFT**: Sort today's tasks by priority (plan) vs. category (actual)

### Bonus Features (Beyond Plan)
- SuggestedTasksList (household task recommendations)
- Auto-move-to-tomorrow callback fully wired
- TaskListPage all-tasks list with filtering
- Retry buttons in error states

### Severity Breakdown
- **Critical**: 2 findings (data safety, debug logs)
- **Warnings**: 8 findings (date handling, logging, timezone, patterns, privacy)
- **Observations**: 3 findings (unused prop, utilities, duplication)

---

