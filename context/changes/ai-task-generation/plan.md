# AI Task Generation Implementation Plan

## Overview

Add AI-powered task generation to externalize household management mental load. Users will describe their household after registration and receive AI-suggested recurring tasks. A dashboard button enables on-demand suggestions. The system uses Google Gemini API to generate contextually relevant household tasks based on user household profile and existing task history.

## Current State Analysis

The application has a working household and task management system:
- Tasks support 5 categories (CLEANING, SHOPPING, LAUNDRY, MAINTENANCE, BILLS)
- Task creation form with title, description, category, due date, and recurrence settings
- Recurrence supports DAILY, WEEKLY, MONTHLY with weekday selection
- API client pattern with auth token injection and retry logic
- Auth context manages household state and token refresh
- No existing AI integration

### Key Discoveries:

- **API pattern established** [frontend/src/lib/api/client.ts:23-98]: Typed `ApiResult<T>` discriminated union, automatic retry on 5xx errors, 30s timeout. Follow this pattern for suggestions endpoint.
- **Task creation flow** [frontend/src/features/tasks/pages/TaskCreatePage.tsx:17-103]: Form validation, error handling via Notice component, loading states. Reuse this pattern for suggestion review.
- **Category system fixed** [frontend/src/features/tasks/utils/categoryUtils.ts:1-31]: 5 predefined categories with color/label mapping. Suggestions must map to these categories only.
- **Household context available** [frontend/src/features/auth/context/AuthContext.tsx]: Auth context provides currentHousehold and refetchHouseholds. Can store household description here.
- **Task API minimal** [frontend/src/features/tasks/api.ts:43-52]: POST /task expects CreateTaskRequest. Suggested tasks use the same shape.

## Desired End State

**User Registration → Onboarding:**
1. User completes registration
2. Sees modal: "Tell us about your household" (text input)
3. Clicks "Get Suggestions"
4. Receives 3-5 AI-generated recurring task suggestions
5. Reviews suggestions (sees flagged potential duplicates)
6. Checks boxes to select, clicks "Add Tasks"
7. Tasks are created and appear on dashboard

**Dashboard Usage:**
1. User clicks "Suggest Tasks" button on dashboard
2. Same review flow as above
3. New suggestions appear alongside existing tasks

**Success verification:**
- New households with AI suggestions complete registration faster (fewer manual task creations)
- Users find suggested tasks relevant and timely
- Duplicate detection prevents confusion
- Error handling gracefully shows retryable errors

## What We're NOT Doing

- Storing suggestion history (stateless: each request generates new suggestions)
- Editable task titles/categories before creation (review modal shows AI's full output, then create as-is)
- Custom recurrence patterns from AI (AI suggests only DAILY/WEEKLY/MONTHLY + end dates)
- Suggestion quality scoring or analytics
- Rate limiting per user (covered by backend/Gemini quotas)
- AI-driven task categories (fixed to app's 5 categories)

## Implementation Approach

**Backend:** Create POST /api/suggestions endpoint that calls Google Gemini API with:
- Household description (from user input)
- Existing task history (titles, categories, recurrence)
- Category list (to constrain suggestions)
- Structured response parsing into Task-compatible objects

**Frontend:**
- Post-registration modal: Text input for household description, calls /api/suggestions
- Suggestion review list: Checkboxes, duplicate flags, create button
- Dashboard button: Same flow as post-registration

**Duplicate detection:** Client-side fuzzy string matching (Levenshtein distance) against existing tasks; flag tasks >75% similar.

**Error handling:** Gemini failures return empty suggestions + user-friendly error message with retry button.

## Critical Implementation Details

### Gemini API Setup

The backend must securely store GOOGLE_API_KEY (env var or secrets manager). Gemini rate limits are generous for free tier (~15 requests/min). Add per-request timeout (10s) to avoid blocking the app.

### Suggestion Output Format

Gemini will be instructed to return JSON-structured task suggestions. The response parser must validate that:
- Each suggestion has title, description, category (from the 5 allowed), dueDate, recurrenceFrequency (DAILY/WEEKLY/MONTHLY or undefined)
- DueDate is a valid YYYY-MM-DD
- RecurrenceWeekday (0-6) is only set if recurrenceFrequency is WEEKLY
- All required fields are present (title, category)

### Household Description Storage

The household description (from onboarding) should be stored in the Household entity on the backend (add optional `description: string` field). This allows future requests to Gemini to include the context. For now, store it but don't persist; pass it each request.

### Duplicate Detection Threshold

Fuzzy matching (Levenshtein distance normalized 0-1) will flag suggestions >0.75 similar to existing task titles. This reduces false positives while catching obvious duplicates like "Clean kitchen" vs "clean the kitchen". Flagged duplicates show inline: "Similar to: Clean Kitchen (due Fri)".

## Phase 1: Backend API Endpoint & Gemini Integration

### Overview

Build the suggestion endpoint that calls Gemini API, validates responses, and returns structured task objects. This is the foundation for both onboarding and dashboard flows.

### Changes Required:

#### 1. Create Gemini API Client Wrapper

**File**: `backend/src/lib/gemini-client.ts` (new)

**Intent**: Encapsulate Google Gemini API calls with error handling, timeout, and response validation. Separates AI integration from business logic.

**Contract**: Export async function `generateTaskSuggestions(householdDescription: string, existingTasks: Task[], apiKey: string): Promise<ApiResult<SuggestedTask[]>>`. Validates Gemini response against task schema and returns discriminated union (success with suggestions array, or error with code + message).

```typescript
interface SuggestedTask {
  title: string
  description?: string
  category: TaskCategory
  dueDate: string // YYYY-MM-DD
  recurrenceFrequency?: 'DAILY' | 'WEEKLY' | 'MONTHLY'
  recurrenceWeekday?: number // 0-6, only if WEEKLY
  recurrenceEndDate?: string // YYYY-MM-DD
}
```

The Gemini prompt will be:
```
You are a household task planning assistant. Given a household description and existing tasks, suggest 3-5 recurring tasks that would help keep the household running smoothly.

Household: {householdDescription}

Existing tasks (do not duplicate):
{existingTasksTitles}

Suggest tasks in the following categories: CLEANING, SHOPPING, LAUNDRY, MAINTENANCE, BILLS

Return a JSON array with this schema:
[
  {
    "title": "string",
    "description": "string (optional)",
    "category": "CLEANING | SHOPPING | LAUNDRY | MAINTENANCE | BILLS",
    "dueDate": "YYYY-MM-DD (14 days from now as baseline)",
    "recurrenceFrequency": "DAILY | WEEKLY | MONTHLY (optional)",
    "recurrenceWeekday": 0-6 (only if WEEKLY),
    "recurrenceEndDate": "YYYY-MM-DD (optional, 1 year from now)"
  }
]

Ensure suggestions are realistic, non-obvious, and diverse across categories.
```

#### 2. Create POST /api/suggestions Endpoint

**File**: `backend/src/routes/suggestions.ts` (new)

**Intent**: HTTP endpoint that validates household description, fetches user's existing tasks, calls Gemini, and returns suggestions. Handles auth and error cases.

**Contract**: POST /api/suggestions with body `{ householdDescription: string }`. Requires Bearer token auth. Returns `{ ok: true, data: SuggestedTask[] }` or `{ ok: false, code: string, message: string, status: number }`. Errors: 400 (empty description), 401 (unauthorized), 500 (Gemini failure).

#### 3. Environment & Secrets

**File**: `.env.example` and deployment config

**Intent**: Document GOOGLE_API_KEY requirement for deployment.

**Contract**: Add `GOOGLE_API_KEY` to secrets/env config. Endpoint reads via `process.env.GOOGLE_API_KEY`.

### Success Criteria:

#### Automated Verification:

- Endpoint accepts POST request with valid household description and returns 200 + suggestions array
- Suggestions array contains 3-5 objects with required fields (title, category, dueDate, recurrenceFrequency)
- All suggestions have valid categories from the 5-category set
- DueDate is a valid future date (YYYY-MM-DD format)
- RecurrenceWeekday is only set when recurrenceFrequency is WEEKLY
- Endpoint returns 400 if householdDescription is missing or empty
- Endpoint returns 401 if authorization header is missing
- Endpoint gracefully returns 500 + error message if Gemini API call fails
- Suggestion parsing rejects invalid JSON or missing required fields
- Unit tests cover: valid input, empty description, auth failure, Gemini timeout, malformed Gemini response

#### Manual Verification:

- Call endpoint with curl/Postman using valid auth token and household description; inspect returned suggestions for relevance
- Verify suggestions match categories used in task creation
- Test Gemini API timeout handling (should return error, not hang)
- Confirm suggestions avoid exact duplicates from household's existing tasks

---

## Phase 2: Post-Registration Onboarding Flow

### Overview

Add modal that appears after user completes registration, collects household description, calls suggestion endpoint, and shows review/selection UI.

### Changes Required:

#### 1. Store Household Description in Household Entity

**File**: `backend/src/db/households.ts` (schema changes) and `frontend/src/features/household/api.ts` (API contract)

**Intent**: Persist household description so it's available for future Gemini requests and shown in household settings.

**Contract**: 
- Backend: Add optional `description?: string` field to Household table. Add UPDATE query to save description.
- Frontend: Extend CreateHouseholdRequest with optional `description` field; update `createHouseholdApi()` to include it in request body.

#### 2. Create Post-Registration Modal Component

**File**: `frontend/src/features/auth/components/HouseholdDescriptionModal.tsx` (new)

**Intent**: Modal that asks "Tell us about your household" with text input and calls /api/suggestions endpoint.

**Contract**: 
- Props: `{ isOpen: boolean, householdName: string, onClose: () => void, onSuggestionsReady: (suggestions: SuggestedTask[]) => void }`
- Shows textarea for household description (placeholder: "e.g., 4 people, 2 kids, 1 dog, apartment")
- "Get Suggestions" button disabled until description is non-empty
- While loading, show spinner + "Generating suggestions..."
- On success, call onSuggestionsReady with suggestions array
- On error, show error message with retry button
- On cancel, close modal and skip suggestions

#### 3. Create Suggestion Review List Component

**File**: `frontend/src/features/tasks/components/SuggestionReviewList.tsx` (new)

**Intent**: Shows suggested tasks in a checkbox list with duplicate flags, allowing user to select which ones to create.

**Contract**:
- Props: `{ suggestions: SuggestedTask[], existingTasks: Task[], onConfirm: (selected: SuggestedTask[]) => void, onCancel: () => void }`
- Each suggestion row: checkbox + title + category badge + description + recurrence info
- Duplicate flag inline: "Similar to: {existing task title}" for >0.75 match
- "Add Selected" button (disabled if no checkboxes selected)
- "Skip" button to dismiss without creating
- Fuzzy matching logic (import levenshtein-distance or implement simple Levenshtein)

#### 4. Wire Into Registration Flow

**File**: `frontend/src/features/auth/pages/RegisterPage.tsx` (modify)

**Intent**: After successful registration, show HouseholdDescriptionModal instead of auto-redirecting to dashboard.

**Contract**:
- Post-registration success → show modal
- Modal onClose → redirect to dashboard
- Modal onSuggestionsReady → show SuggestionReviewList
- SuggestionReviewList onConfirm → batch create tasks, then redirect

### Success Criteria:

#### Automated Verification:

- HouseholdDescriptionModal renders when isOpen=true
- Text input accepts household description text
- "Get Suggestions" button calls /api/suggestions endpoint with householdDescription
- On success, onSuggestionsReady callback is called with suggestions
- On error, error message displays with retry option
- SuggestionReviewList renders suggestions as checkboxes
- Duplicate detection flags tasks >0.75 similar to existing ones
- "Add Selected" button calls onConfirm with only checked suggestions
- Fuzzy match logic correctly flags "clean kitchen" ≈ "Clean kitchen" but not "clean kitchen" ≈ "do laundry"
- Component integrates into RegisterPage flow (post-success shows modal)
- Type checking passes; all SuggestedTask fields are optional where required

#### Manual Verification:

- Register a new user, see modal immediately after
- Enter household description, click "Get Suggestions", receive relevant suggestions
- Review suggestions (verify no exact duplicates shown, fuzzy matches flagged)
- Select some suggestions, click "Add Selected", see tasks appear on dashboard
- Household description is saved (can check API response)
- Skip button dismisses modal without creating tasks
- Error case: enter household description with no internet, see error + retry, then succeed

---

## Phase 3: Dashboard "Suggest Tasks" Button

### Overview

Add on-demand task suggestion button to dashboard. Reuses suggestion review flow from Phase 2 but triggers on button click instead of post-registration.

### Changes Required:

#### 1. Add Suggestion Button to Dashboard

**File**: `frontend/src/features/tasks/components/TodayDashboardContainer.tsx` or `frontend/src/features/tasks/pages/DashboardPage.tsx` (modify)

**Intent**: Add "✨ Suggest Tasks" button that triggers on-demand suggestions using the user's stored household description.

**Contract**: 
- Button triggers modal/flow to request new suggestions
- Calls /api/suggestions with user's existing household description (from currentHousehold context if available, else ask again)
- Shows SuggestionReviewList component (reused from Phase 2)
- On confirm, batch-create suggested tasks and refresh task list
- Loading state: spinner, disabled button during request

#### 2. Update /api/suggestions to Handle Missing Description

**File**: `backend/src/routes/suggestions.ts` (modify)

**Intent**: If householdDescription is not provided (dashboard call), use stored description from household entity.

**Contract**: 
- Endpoint now accepts optional householdDescription in request body
- If missing, query Household.description
- If neither is available, return 400 (user must set one first)

### Success Criteria:

#### Automated Verification:

- Dashboard button exists and is clickable
- Button click calls /api/suggestions with stored household description
- Suggestions returned successfully
- SuggestionReviewList shows suggestions with duplicates flagged
- "Add Selected" creates tasks and refreshes dashboard task list
- Task list immediately shows newly created suggested tasks

#### Manual Verification:

- Log in to dashboard, see "Suggest Tasks" button
- Click button, wait for suggestions to load
- Review suggestions (check for duplicates vs existing tasks)
- Select some, click "Add Selected"
- See new tasks appear on dashboard immediately
- Click button again, get new suggestions (not the same ones)

---

## Testing Strategy

### Unit Tests:

- `gemini-client.test.ts`: Response parsing, validation, error cases (timeout, malformed JSON, missing required fields)
- `suggestions-endpoint.test.ts`: Auth checks, empty description validation, Gemini error handling, response format
- `SuggestionReviewList.test.tsx`: Render suggestions, checkbox selection, duplicate flag logic, fuzzy matching thresholds
- `HouseholdDescriptionModal.test.tsx`: Modal open/close, input handling, API call triggering

### Integration Tests:

- Register user → see modal → enter household description → receive suggestions → select and create tasks → verify on dashboard
- Dashboard button → click → enter household description (if not set) → receive suggestions → add tasks → refresh
- Duplicate handling: add task "Clean kitchen", trigger suggestions, verify fuzzy-match flag appears for similar suggestion

### Manual Testing Steps:

1. **Post-Registration Flow:**
   - Register new account
   - See household description modal
   - Enter "3-person household, 2 kids, 1 dog, apartment"
   - Get suggestions (should include kids-related tasks, pet care, apartment-specific)
   - Accept some, reject others
   - Verify tasks created on dashboard

2. **Dashboard On-Demand:**
   - Log in to existing account
   - Click "Suggest Tasks"
   - Get new suggestions (different from previous)
   - Accept and verify creation

3. **Duplicate Handling:**
   - Existing task: "Clean the kitchen"
   - Suggestion: "Clean kitchen"
   - Should show fuzzy flag; allow user to decide

4. **Error Cases:**
   - No internet: See error message + retry
   - Gemini timeout: See error after 10s + retry option
   - Empty household description: See validation error

5. **Recurrence Suggestions:**
   - Verify suggestions include daily (trash), weekly (bathroom), monthly (deep clean) tasks
   - Verify recurrence parameters (end date, weekday) are valid

## Performance Considerations

- Gemini API calls will add ~1-3 second latency; acceptable for on-demand feature but not for auto-load
- Suggestion review modal should not block other dashboard interactions (async flow)
- Fuzzy matching algorithm (Levenshtein) is O(n*m) per comparison; cache existing task titles for <100ms overhead on small household task lists
- Consider debouncing household description input if real-time validation is added later

## Migration Notes

Household schema change (add optional description field) is backwards-compatible. Existing households without description will skip suggestion feature gracefully (must set description first).

## References

- Gemini API docs: https://ai.google.dev/docs
- Fuzzy string matching: Levenshtein distance (consider npm package `string-similarity` or simple inline implementation)
- Existing task patterns: `frontend/src/features/tasks/pages/TaskCreatePage.tsx:17-103`
- API client pattern: `frontend/src/lib/api/client.ts:23-98`
- Similar onboarding modal pattern: Look for auth flow modals or user settings modals in the codebase

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Backend API Endpoint & Gemini Integration

#### Automated

- [x] 1.1 Create gemini-client.ts with generateTaskSuggestions() function
- [x] 1.2 POST /api/suggestions endpoint with auth and validation
- [x] 1.3 Environment variable setup for GOOGLE_API_KEY
- [x] 1.4 Unit tests for Gemini parsing and error cases
- [x] 1.5 Unit tests for /api/suggestions endpoint

#### Manual

- [ ] 1.6 Test endpoint with curl/Postman for valid and error cases
- [ ] 1.7 Verify suggestions are relevant and use only allowed categories

### Phase 2: Post-Registration Onboarding Flow

#### Automated

- [ ] 2.1 Add household.description field to schema
- [ ] 2.2 Create HouseholdDescriptionModal component
- [ ] 2.3 Create SuggestionReviewList component with fuzzy matching
- [ ] 2.4 Wire modal into RegisterPage post-registration flow
- [ ] 2.5 Unit tests for both components
- [ ] 2.6 Integration test for full registration → suggestions → create flow

#### Manual

- [ ] 2.7 Register new user and complete onboarding with suggestions
- [ ] 2.8 Verify household description is saved
- [ ] 2.9 Verify suggested tasks created and appear on dashboard
- [ ] 2.10 Test error handling (network failure, empty input, Gemini timeout)

### Phase 3: Dashboard "Suggest Tasks" Button

#### Automated

- [ ] 3.1 Add "Suggest Tasks" button to dashboard component
- [ ] 3.2 Update /api/suggestions to use stored household description
- [ ] 3.3 Wire suggestion review flow into dashboard
- [ ] 3.4 Unit tests for button and suggestion creation flow

#### Manual

- [ ] 3.5 Test "Suggest Tasks" button on dashboard
- [ ] 3.6 Verify new suggestions are generated (not duplicates of previous run)
- [ ] 3.7 Verify created tasks appear immediately on dashboard
- [ ] 3.8 Test duplicate detection with existing tasks
