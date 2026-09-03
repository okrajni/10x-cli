# Suggested Household Tasks — Implementation Plan

**Split from:** S-06 (Today's Dashboard Enhanced) — part 2 of 2

**Relationship:** This plan extracts the suggestion heuristics logic from [today-dashboard/plan.md](../today-dashboard/plan.md). Both plans contribute to the north-star dashboard but with distinct responsibilities:
- **dashboard-prioritization:** Sorting algorithm, container structure, user's own tasks, mobile UI
- **suggested-household-tasks (this plan):** Domain task definitions, heuristics scoring engine, suggestion API, frontend component, accept/dismiss, analytics logging

**Implementation:** Develop in parallel with dashboard-prioritization. Phases 1–3 are independent (backend heuristics). Phase 4 integrates the `SuggestedTasksList` component into the container built by dashboard-prioritization (S-06A).

## Overview

Implement a domain-specific heuristic engine that suggests 3–5 common household tasks based on seasonal patterns, frequency rules, and category analysis. Users can accept (adds to their task list), dismiss, or customize suggested tasks. This validates the core hypothesis: simple, rule-based suggestions are more useful and maintainable than AI-generated ones.

## Current State Analysis

The DoneYet app has:
- Backend Task API with create, update, delete endpoints
- Frontend TaskCard component for display
- Task model with category, dueDate, completed fields, soft-delete via deletedAt
- No suggestion logic or domain task definitions yet

**Gap:** No system suggests tasks to users. Users must remember what household chores are due (HVAC filters, laundry, deep cleaning, etc.). Mental load is high; MVP must surface these.

## Desired End State

Users open the dashboard and see:
- Their own today/overdue tasks (from dashboard-prioritization, sorted by due date + priority)
- Below that: "Suggested Tasks" section with 3–5 domain-heuristic suggestions
- Each suggestion: title (e.g., "Change HVAC filter"), frequency label (e.g., "Every 6 months"), accept/dismiss buttons
- Accept action: task added to user's list with calculated due date (today by default, or based on frequency logic)
- Dismiss action: suggestion hidden for now (logged for validation)
- Post-MVP validation: track acceptance rate (target ≥50% to validate heuristics work)

### Verification

- Backend returns 3–5 suggestions via new API endpoint
- Frontend displays suggestions in dashboard below user's tasks
- User can accept suggestions (task created, appears in list immediately)
- User can dismiss suggestions (suggestion removed from view, logged)
- Acceptance rate ≥50% post-launch confirms heuristics are working
- No AI integration required

## Domain Tasks Reference

Below are suggested household tasks organized by category, frequency, and seasonal factors. Implement as backend constants; adjust based on user feedback.

### Cleaning (Weekly / Bi-Weekly)

| Task | Frequency | Category | Notes |
|------|-----------|----------|-------|
| Vacuum living room | Every 1 week | cleaning | High-traffic area |
| Clean bathrooms | Every 2 weeks | cleaning | Toilets, sinks, tubs |
| Mop kitchen floor | Every 2 weeks | cleaning | High-traffic area |
| Change bed sheets | Every 2 weeks | cleaning | Bedroom |
| Dust surfaces | Every 1 week | cleaning | Bedroom, living room, kitchen |
| Laundry | Every 2 days | cleaning | Variable by household |
| Wash windows | Every 3 months | cleaning | Interior/exterior |
| Deep clean kitchen | Every 1 month | cleaning | Ovens, appliances |
| Deep clean bathroom | Every 1 month | cleaning | Grout, tile, mirrors |

### Maintenance (Monthly / Seasonal / Yearly)

| Task | Frequency | Category | Seasonal | Notes |
|------|-----------|----------|----------|-------|
| Check air filter | Every 1 month | maintenance | N/A | HVAC system check |
| Change HVAC filter | Every 3 months (90 days) | maintenance | N/A | Furnace/AC filter |
| Lawn mowing | Every 2 weeks | maintenance | Spring–Fall | Outdoor |
| Garden watering | Every 3 days | maintenance | Summer | Hot months only |
| Gutter cleaning | Once per year | maintenance | Fall | Prep for winter |
| AC maintenance | Once per year | maintenance | Spring | Before summer |
| Furnace inspection | Once per year | maintenance | Fall | Before winter |
| Chimney sweep | Once per year | maintenance | Fall | If applicable |
| Pest control | Every 3 months | maintenance | N/A | Quarterly |
| Water softener recharge | Every 6 weeks | maintenance | N/A | If applicable |

### Shopping & Errands (Variable)

| Task | Frequency | Category | Notes |
|--------|-----------|----------|-------|
| Grocery shopping | Every 1 week | shopping | Variable by household |
| Pharmacy run | As needed | errands | Prescription refills |
| Gas up car | Every 2 weeks | errands | Vehicle maintenance |
| Car wash | Every 2 weeks | errands | Exterior cleaning |
| Post office | As needed | errands | Mail, packages |

### Seasonal & Special

| Task | Frequency | Category | Seasonal | Notes |
|------|-----------|----------|----------|-------|
| Spring cleaning | Once per year | seasonal | Spring | Deep clean/organize |
| Fall cleanup | Once per year | seasonal | Fall | Leaves, prepare for winter |
| Holiday prep | Once per year | seasonal | Q4 | Decorations, planning |
| Yard winterization | Once per year | seasonal | Fall | Drain hoses, prep |
| Yard spring prep | Once per year | seasonal | Spring | Mulch, plant, rake |

## Heuristics Engine Logic

### Core Algorithm

```
getSuggestions(householdId) {
  1. Fetch all tasks for household (completed and active)
  2. Get current date, month, season
  3. For each domain task:
     - Calculate days since last completion (or 999 if never done)
     - Check if due: days_since >= frequency_days
     - Check if seasonal: current_season matches task.season or task.season == "N/A"
     - Calculate relevance score: (days_since / frequency_days) + season_bonus
  4. Sort domain tasks by relevance (highest first)
  5. Return top 3–5 that score > 0.8 (due or overdue-ish)
}
```

### Key Rules

1. **Frequency-based:** Task is suggested if `daysInce last completion >= frequency_days`
   - Example: "HVAC filter every 90 days" → suggest if last change was ≥90 days ago
   - If task never completed, suggest immediately (days_since = 999)

2. **Seasonal:** Task is suggested only during relevant season
   - "Spring cleaning" only March–May
   - "Gutter cleaning" only Sept–Nov
   - Non-seasonal tasks ("N/A") always available

3. **Category weighting:** Boost cleaning tasks in spring/summer, maintenance in fall/spring
   - Optional: slightly higher relevance for cleaning (frequent, visible)

4. **Limit to 3–5 suggestions:** Prevents overwhelm. Show most relevant only.

5. **User household context:** Different households may need different suggestions
   - Future: "Do you have a pool?" → add pool maintenance tasks
   - Future: "Do you have a furnace?" → add furnace maintenance
   - MVP: static task list for all households (simplest)

### Pseudo-code: Suggestion Scoring

```typescript
interface DomainTask {
  id: string;
  title: string;
  category: "cleaning" | "maintenance" | "shopping" | "seasonal" | "errands";
  frequency_days: number;
  seasonal?: "spring" | "summer" | "fall" | "winter";
}

function scoreTask(task: DomainTask, household: Task[]): number {
  // Find last completion of this task in household
  const lastCompletion = household
    .filter(t => t.title.toLowerCase().includes(task.title.toLowerCase()) && t.completed)
    .map(t => t.completedAt)
    .sort()
    .pop();

  // Calculate days since last completion
  const daysSince = lastCompletion
    ? Math.floor((Date.now() - lastCompletion.getTime()) / (1000 * 60 * 60 * 24))
    : 999; // Never done = high score

  // Check if in season
  const currentMonth = new Date().getMonth() + 1; // 1–12
  const isInSeason = !task.seasonal || isInSeasonRange(task.seasonal, currentMonth);

  // Score: how overdue is the task?
  const overdueFactor = Math.max(0, daysSince / task.frequency_days);
  const seasonalBonus = isInSeason ? 1.0 : 0.2; // Lower score out of season

  return overdueFactor * seasonalBonus;
}

function getSuggestions(household: Household): DomainTask[] {
  const allDomainTasks = DOMAIN_TASKS; // Backend constant
  const householdTasks = fetchTasks(household.id);

  const scored = allDomainTasks.map(task => ({
    task,
    score: scoreTask(task, householdTasks)
  }));

  // Filter to scored ≥ 0.8 (due/overdue-ish), sort by score, return top 5
  return scored
    .filter(s => s.score >= 0.8)
    .sort((a, b) => b.score - a.score)
    .slice(0, 5)
    .map(s => s.task);
}
```

---

## What We're NOT Doing

- AI-generated suggestions (using LLMs like Gemini)
- Personalization based on user habits (MVP uses generic rules for all households)
- Machine learning or ML-based ranking (domain heuristics only)
- Recurring task automation (user accepts suggestion → one-time task added, not recurring)
- Custom household configuration (MVP: static domain tasks)
- Suggestion persistence or history (dismissed suggestions don't persist across sessions, MVP)
- Advanced analytics or A/B testing (log acceptance/dismiss rate only)
- Mobile-specific UX (responsive design covers mobile)

---

## Implementation Approach

1. **Backend: Define Domain Tasks** — Create constants file with 20–30 household tasks
2. **Backend: Heuristics Engine** — Implement scoring algorithm in service class
3. **Backend: Suggestion Endpoint** — New REST endpoint: `GET /api/household/{id}/suggestions`
4. **Frontend: Suggestions Component** — New React component to display suggestions
5. **Frontend: Accept/Dismiss Flow** — Wire buttons to create task or log dismissal
6. **Analytics: Logging** — Track acceptance/dismissal in backend logs or DB
7. **Dashboard Integration** — Add suggestions component to today dashboard container

---

## Phase 1: Backend Domain Tasks & Heuristics Engine

### Overview

Define all household domain tasks and implement the heuristics scoring engine. No frontend yet; backend can be tested via API directly.

### Changes Required

#### 1. Create Domain Tasks Constant

**File**: `backend/src/main/java/com/doneyet/household/domain/DomainTasks.java` (or `domain-tasks.constant.ts` if backend is TypeScript)

**Intent**: Define all 20–30 common household tasks with metadata (title, category, frequency, seasonal).

**Contract**:
- Public static final or const list: `DOMAIN_TASKS`
- Each entry: `{ id, title, category, frequency_days, seasonal? }`
- Example: `{ id: "hvac-filter", title: "Change HVAC filter", category: "maintenance", frequency_days: 90, seasonal: null }`
- Organized into categories (cleaning, maintenance, shopping, seasonal)
- 20–30 total tasks

#### 2. Create Heuristics Scoring Service

**File**: `backend/src/main/java/com/doneyet/task/service/SuggestionService.java` (or equivalent)

**Intent**: Implement the suggestion scoring algorithm that ranks domain tasks by relevance.

**Contract**:
- Method: `List<DomainTask> suggestTasks(String householdId)`
- Logic: 
  1. Fetch all tasks for household
  2. Score each domain task based on frequency + seasonal + last completion
  3. Filter score ≥ 0.8
  4. Sort by score descending
  5. Return top 5
- Returns domain task objects (not yet persisted as user tasks)

#### 3. Create Suggestion API Endpoint

**File**: `backend/src/main/java/com/doneyet/task/controller/SuggestionController.java` (or in TaskController)

**Intent**: Expose suggestions via REST endpoint.

**Contract**:
- Endpoint: `GET /api/household/{householdId}/suggestions` (or `/api/suggestions?householdId=...`)
- Auth: JWT required; user must own the household
- Response: JSON array of domain task objects + scores
- Example response:
  ```json
  [
    { "id": "hvac-filter", "title": "Change HVAC filter", "category": "maintenance", "frequency_days": 90, "score": 1.2 },
    { "id": "laundry", "title": "Laundry", "category": "cleaning", "frequency_days": 2, "score": 0.95 }
  ]
  ```
- Error handling: Return 404 if household not found, 403 if not authorized

### Success Criteria

#### Automated Verification

- Backend compiles without errors
- No type errors (TypeScript) or compilation errors (Java)
- Unit tests for SuggestionService (test scoring logic with mock tasks)

#### Manual Verification

- Call `GET /api/household/{id}/suggestions` via curl/Postman
- Verify 3–5 domain tasks returned (scores visible, high scores first)
- Verify tasks have correct fields (id, title, category, frequency_days, score)
- Verify seasonal filtering works (e.g., spring cleaning only returns March–May)
- Verify frequency scoring works (e.g., HVAC filter scores high if 90+ days since last change)

---

## Phase 2: Frontend Suggestions Component & Accept Flow

### Overview

Build the SuggestedTasksList component to display suggestions and wire up accept/dismiss actions.

### Changes Required

#### 1. Create SuggestedTasksList Component

**File**: `frontend/src/features/tasks/components/SuggestedTasksList.tsx`

**Intent**: Display 3–5 domain-heuristic suggestions with accept and dismiss buttons.

**Contract**:
- Props: `householdId: string`, `onSuggestionAccepted?: () => void` (callback to refresh parent list)
- Internal state: `suggestions[]`, `isLoading`, `error`
- Side effect: Fetch suggestions on mount via `GET /api/household/{id}/suggestions`
- Returns JSX with: header "Suggested Tasks", list of suggestions, each with accept/dismiss buttons
- Styling: distinct section below user's own tasks, gray background or subtle border

#### 2. Implement Accept Action

**File**: `frontend/src/features/tasks/components/SuggestedTasksList.tsx`

**Intent**: User clicks accept → suggestion becomes a task in the task list.

**Contract**:
- Button: "Accept" on each suggestion
- On click: call `createTask()` with:
  - title: suggestion.title
  - category: suggestion.category
  - dueDate: today (or configurable; MVP is today)
  - householdId: from props
- Optimistic update: remove suggestion from list immediately
- Handle error: restore suggestion, show error message
- Success: show "Task added" message, call onSuggestionAccepted callback to refresh parent

#### 3. Implement Dismiss Action

**File**: `frontend/src/features/tasks/components/SuggestedTasksList.tsx`

**Intent**: User can dismiss a suggestion (hide it, don't create task).

**Contract**:
- Button: "Dismiss" or "✕" on each suggestion
- On click: remove suggestion from local state (optimistic)
- Backend logging: POST `/api/household/{id}/suggestion-interaction` with `{ suggestionId, action: "dismiss" }`
- No error handling needed (fire-and-forget logging)
- Visual: fade out or slide away animation (optional, MVP: just remove)

#### 4. Integration with Dashboard Container

**File**: `frontend/src/features/tasks/components/TodayDashboardContainer.tsx`

**Intent**: Add SuggestedTasksList to the dashboard container below user's tasks.

**Contract**:
- Add `<SuggestedTasksList householdId={householdId} onSuggestionAccepted={handleRefresh} />` below task list
- Separate section with distinct styling (gray background, "Suggested Tasks" header)
- Pass handleRefresh as callback so accepting a suggestion triggers re-fetch of user's tasks (task list updates immediately)

### Success Criteria

#### Automated Verification

- Component compiles: `npm run typecheck` passes
- No linting issues: `npm run lint` passes

#### Manual Verification

- SuggestedTasksList renders on dashboard below user's tasks
- Suggestions load and display correctly (3–5 tasks visible)
- Accept button: click → task added to user's list → suggestion disappears → refresh shows new task in today/overdue section
- Dismiss button: click → suggestion disappears → task NOT added
- Error handling: intentionally break API → see error message + option to retry
- Multiple households: switch household → suggestions update (different tasks for different households)

---

## Phase 3: Analytics & Validation Logging

### Overview

Log user interactions with suggestions (accept/dismiss) to track effectiveness of heuristics post-MVP.

### Changes Required

#### 1. Create Suggestion Interaction Logging

**File**: `backend/src/main/java/com/doneyet/task/repository/SuggestionInteractionRepository.java`

**Intent**: Persist user's accept/dismiss choices for analytics.

**Contract**:
- New table: `suggestion_interactions` with fields:
  - `id` (UUID)
  - `household_id` (FK to households)
  - `suggestion_id` (domain task ID)
  - `action` (enum: "accepted" | "dismissed")
  - `created_at` (timestamp)
- JPA repository for save/query

#### 2. Add Logging Endpoint

**File**: `backend/src/main/java/com/doneyet/task/controller/SuggestionController.java`

**Intent**: Accept POST requests from frontend to log suggestion interactions.

**Contract**:
- Endpoint: `POST /api/household/{id}/suggestion-interaction`
- Request body: `{ suggestionId: string, action: "accepted" | "dismissed" }`
- Auth: JWT required
- Response: `{ status: "ok" }`
- Backend: save interaction to DB, return immediately (fire-and-forget from frontend)

#### 3. Create Acceptance Rate Query

**File**: `backend/src/main/java/com/doneyet/task/controller/AnalyticsController.java`

**Intent**: Expose acceptance rate for validation (MVP: admin/testing only, not user-facing).

**Contract**:
- Endpoint: `GET /api/analytics/suggestion-acceptance` (admin only, optional for MVP)
- Response: `{ accepted: 150, dismissed: 100, rate: 0.60 }` (60% acceptance rate)
- Usage: post-launch, user/admin can check if heuristics are working

### Success Criteria

#### Automated Verification

- Backend compiles and migration runs
- No compilation errors

#### Manual Verification

- Accept a suggestion → check DB, interaction logged with action="accepted"
- Dismiss a suggestion → check DB, interaction logged with action="dismissed"
- Query analytics endpoint → see aggregated acceptance rate
- Acceptance rate ≥50% post-MVP = hypothesis validated (heuristics work)

---

## Phase 4: Dashboard Container Integration & Polish

### Overview

Integrate suggestions into the today dashboard container and polish the overall UX.

### Changes Required

#### 1. Dashboard Layout & Styling

**File**: `frontend/src/features/tasks/components/TodayDashboardContainer.tsx`

**Intent**: Ensure user's tasks and suggestions display in clear visual hierarchy.

**Contract**:
- Structure:
  ```
  [Dashboard Header: "Today & Overdue"]
  [Refresh button]
  [Loading spinner or error state]
  [User's own tasks - sorted by due date + priority]
  [------- Divider -------]
  [Suggested Tasks section]
  [3–5 suggested tasks with accept/dismiss buttons]
  [Empty state if no suggestions]
  ```
- Styling: user's tasks in default color, suggestions in light gray background or distinct container
- Responsive: works on mobile (no horizontal scroll), suggestions stack below user's tasks

#### 2. Acceptance Callback Flow

**File**: `frontend/src/features/tasks/components/TodayDashboardContainer.tsx`

**Intent**: When user accepts a suggestion, immediately show it in the user's task list.

**Contract**:
- Pass `onSuggestionAccepted={handleRefresh}` callback to SuggestedTasksList
- handleRefresh: re-fetch user's tasks and suggestions, re-render both sections
- UX: accepted task immediately appears in user's sorted list, suggestion disappears

#### 3. Edge Case: Many Suggestions

**File**: `frontend/src/features/tasks/components/SuggestedTasksList.tsx`

**Intent**: If more than 5 suggestions exist, scroll or paginate gracefully.

**Contract**:
- Limit display to 5 suggestions (backend already returns top 5)
- If 5+ suggestions: add "See more suggestions" link (optional, MVP: top 5 only)
- Scroll within suggestions section if needed (overflow-y-auto)

### Success Criteria

#### Automated Verification

- No regressions: `npm run lint` and `npm run typecheck` pass

#### Manual Verification

- Dashboard shows user's tasks above suggestions (clear separation)
- Accept a suggestion → task appears in user's list immediately (optimistic update)
- Dismiss a suggestion → removed from suggestions, can accept others
- Refresh page → suggestions reset (fetch fresh from API)
- Mobile (375px): container scrolls within viewport, no horizontal scroll
- Many tasks (50+ user + 5 suggestions): all visible, scrollable, no lag

---

## Testing Strategy

### Backend Testing (Unit + Integration)

1. **SuggestionService Scoring Logic**
   - Test case: task due in 2 days (frequency: 3 days) → score ≈ 0.67 (not due yet, lower score)
   - Test case: task due 95 days ago (frequency: 90 days) → score ≈ 1.06 (overdue, higher score)
   - Test case: seasonal task in-season vs out-of-season → score boost in-season
   - Test case: never-completed task → very high score (days_since = 999)

2. **Suggestion API Endpoint**
   - Test: fetch suggestions for valid household → returns 3–5 tasks sorted by score
   - Test: fetch suggestions for household with no tasks → returns full domain task list (all high scoring)
   - Test: fetch suggestions for invalid household → returns 404
   - Test: unauthenticated request → returns 401

3. **Suggestion Interaction Logging**
   - Test: POST accept interaction → persisted to DB
   - Test: POST dismiss interaction → persisted to DB
   - Test: query acceptance rate → calculates correctly

### Frontend Testing (Manual)

1. **Suggestions Display**
   - Navigate to `/dashboard`
   - Verify "Suggested Tasks" section visible below user's tasks
   - Verify 3–5 suggestions displayed
   - Verify each suggestion has title, category, accept/dismiss buttons

2. **Accept Flow**
   - Click accept on a suggestion
   - Verify task added to user's list immediately (optimistic)
   - Verify suggestion removed from suggestions list
   - Verify "Task added" success message
   - Refresh page → suggestion gone, task remains

3. **Dismiss Flow**
   - Click dismiss on a suggestion
   - Verify suggestion removed from list
   - Verify task NOT added
   - Refresh page → suggestion may reappear (fresh fetch) or stay gone (depends on implementation)

4. **Household Switching**
   - Create household A and B
   - Add different tasks to each
   - Switch households → suggestions update (different tasks)

5. **Error Scenarios**
   - Go offline, load dashboard
   - Verify error message appears for suggestions section
   - Retry button works, suggestions load when back online

### Edge Cases

- Household with zero tasks → all domain tasks eligible for suggestion
- Household with all tasks completed recently → low-scoring suggestions (not due yet)
- Many suggestions (future: >5) → scrolling works, no overflow
- User accepts all 5 suggestions → suggestions section is empty (all dismissed/accepted)
- Race condition: accept suggestion while refresh is in flight → graceful handling (optimistic updates)

### Manual Validation (Post-MVP)

- Track acceptance rate for 1–2 weeks of real usage
- Target: ≥50% acceptance = heuristics are working
- If <50%: adjust domain tasks or rules
- If >70%: heuristics are very useful, consider adding more suggestions

### Notes

- No automated frontend test suite (no Jest/Vitest yet)
- Build components with clean props/callbacks for future testing
- Acceptance rate is the key validation metric (not user satisfaction surveys)

---

## Performance Considerations

- Suggestion fetch on component mount: one request per household, cached in state
- Scoring algorithm: O(N * M) where N = domain tasks (30), M = user tasks (typically <100) → fast
- No database indexing needed yet (MVP scale)
- Frontend: no memoization needed yet (suggestions list < 10 items)
- Backend: cache domain tasks in memory (never changes, only changes code)

---

## References

- Task API: `backend/src/main/java/com/doneyet/task/controller/TaskController.java`
- Task model: `backend/src/main/java/com/doneyet/task/domain/Task.java`
- Household model: `backend/src/main/java/com/doneyet/household/domain/Household.java`
- Frontend TaskCard: `frontend/src/shared/components/TaskCard.tsx`
- Frontend TodayDashboardContainer: `frontend/src/features/tasks/components/TodayDashboardContainer.tsx` (from dashboard-prioritization change)

---

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles.

### Phase 1: Backend Domain Tasks & Heuristics Engine

#### Automated

- [ ] 1.1 Backend compiles without errors
- [ ] 1.2 Unit tests for SuggestionService pass

#### Manual

- [ ] 1.3 Domain tasks constant defined (20–30 tasks)
- [ ] 1.4 SuggestionService scoring algorithm works correctly
- [ ] 1.5 Suggestion API endpoint returns 3–5 tasks
- [ ] 1.6 Seasonal filtering works
- [ ] 1.7 Frequency-based scoring works
- [ ] 1.8 Multiple households return different suggestions

### Phase 2: Frontend Suggestions Component & Accept Flow

#### Automated

- [ ] 2.1 Component compiles (npm run typecheck)
- [ ] 2.2 No linting issues (npm run lint)

#### Manual

- [ ] 2.3 SuggestedTasksList displays on dashboard
- [ ] 2.4 Accept button: task added to list
- [ ] 2.5 Dismiss button: suggestion removed, task not added
- [ ] 2.6 Error handling: offline → see error + retry
- [ ] 2.7 Household switching: suggestions update

### Phase 3: Analytics & Validation Logging

#### Automated

- [ ] 3.1 Backend compiles, migrations run
- [ ] 3.2 Suggestion interaction logging works

#### Manual

- [ ] 3.3 Accept logged to DB with action="accepted"
- [ ] 3.4 Dismiss logged to DB with action="dismissed"
- [ ] 3.5 Acceptance rate endpoint calculates correctly

### Phase 4: Dashboard Container Integration & Polish

#### Automated

- [ ] 4.1 No regressions (npm run lint, npm run typecheck)

#### Manual

- [ ] 4.2 Dashboard layout: user tasks above suggestions, clear separation
- [ ] 4.3 Accept flow: task appears in user list immediately
- [ ] 4.4 Mobile responsive (375px): no horizontal scroll
- [ ] 4.5 Edge cases: many tasks, household switch, error recovery all work
