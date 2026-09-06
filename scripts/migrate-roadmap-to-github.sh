#!/bin/bash

# Migrate roadmap.md to GitHub Issues
# Usage: bash scripts/migrate-roadmap-to-github.sh
# Requirements: GitHub CLI (gh) installed and authenticated

set -e

REPO="okrajni/10x-cli"
MILESTONE="M-1: first-coordination-proof"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Roadmap to GitHub Issues Migration ===${NC}\n"

# Phase 1: Create Labels
echo -e "${YELLOW}Phase 1: Creating labels...${NC}"
LABELS=(
  "roadmap:Issue originated from roadmap"
  "milestone:Milestone container issue"
  "foundation:Infrastructure/enabler work"
  "slice:User-visible feature slice"
  "decision:Blocker requiring a decision"
  "blocker:Blocking other work"
  "status:proposed:Initial proposal"
  "status:ready:Ready to plan"
  "status:blocked:Blocked by unknown/external"
  "status:in-progress:Currently being worked"
  "status:done:Complete"
  "backend:Backend/API work"
  "frontend:React/UI work"
  "database:Data/schema work"
  "telegram:Telegram bot integration"
  "user-visible:Affects user-facing behavior"
  "priority:critical:Unblock everything"
  "priority:high:Needed for MVP"
  "priority:medium:Nice to have"
  "f01:Foundation F-01"
  "f02:Foundation F-02"
  "f03:Foundation F-03"
  "f04:Foundation F-04"
  "s01:Slice S-01"
  "s02:Slice S-02"
  "s03:Slice S-03"
  "s04:Slice S-04"
  "s05:Slice S-05"
  "s06:Slice S-06"
  "s07:Slice S-07"
)

for label in "${LABELS[@]}"; do
  IFS=':' read -r name desc <<< "$label"
  echo "  Creating label: $name"
  gh label create "$name" --repo "$REPO" --description "$desc" 2>/dev/null || echo "    (label already exists or error)"
done

echo -e "${GREEN}✓ Labels created${NC}\n"

# Phase 2: Create Milestone
echo -e "${YELLOW}Phase 2: Creating milestone...${NC}"
gh milestone create "$MILESTONE" \
  --repo "$REPO" \
  --description "M-1: Prove that the household coordination model works — two users can share task management responsibility and receive timely reminders." \
  2>/dev/null || echo "  (milestone already exists)"
echo -e "${GREEN}✓ Milestone created${NC}\n"

# Phase 3: Create GitHub Projects board
echo -e "${YELLOW}Phase 3: Creating GitHub Projects board...${NC}"
BOARD_ID=$(gh project create \
  --repo "$REPO" \
  --title "Roadmap Board: M-1" \
  --format json \
  --template basic \
  2>/dev/null | jq -r '.id' || echo "")

if [ -z "$BOARD_ID" ]; then
  echo "  Note: Could not create project (may already exist). You may need to create it manually."
  echo "  Visit: https://github.com/okrajni/10x-cli/projects"
else
  echo "  Project created with ID: $BOARD_ID"
fi
echo -e "${GREEN}✓ GitHub Projects setup complete${NC}\n"

# Phase 4: Create Issues
echo -e "${YELLOW}Phase 4: Creating issues...${NC}"

# Helper function to create an issue
create_issue() {
  local title="$1"
  local body="$2"
  local labels="$3"

  echo "  Creating: $title"
  issue_num=$(gh issue create \
    --repo "$REPO" \
    --title "$title" \
    --body "$body" \
    --label "$labels" \
    --milestone "$MILESTONE" \
    --json number \
    --jq '.number' 2>/dev/null || echo "")

  if [ -n "$issue_num" ]; then
    echo "    → Issue #$issue_num"
    echo "$issue_num"
  else
    echo "    (failed or issue already exists)"
  fi
}

# Parent Issue: M-1
PARENT_BODY=$(cat <<'EOF'
# M-1: first-coordination-proof

Prove that the household coordination model works — two users can share task management responsibility and receive timely reminders, reducing the mental load from one person to both.

## Milestone Intent

This milestone delivers the minimum viable proof that the core hypothesis works: assignment + Telegram reminders can coordinate household task management between partners.

## Foundations Required
- F-01: Auth scaffold (email/password registration & login)
- F-02: Household schema & data model (PostgreSQL, user isolation)
- F-03: Telegram bot scaffold (token management, webhooks)
- F-04: Frontend scaffold (React, routing, build setup)

## Vertical Slices (User-Visible Features)
- S-01: New user setup (register, create household, invite partner)
- S-02: Basic task CRUD (create, view, edit, delete tasks)
- S-03: Task assignment (assign to self or partner)
- S-04: Telegram reminder (NORTH STAR — proves value prop)
- S-05: Telegram completion 2-way sync
- S-06: "Today" dashboard (grouped by assignee)
- S-07: AI task generation (secondary feature, deferrable)

## Open Questions (Blocking F-03)
1. **Telegram bot ownership:** System-wide bot vs. per-household? (Owner: product/ops)
2. **User Telegram linking:** QR code vs. token entry vs. OAuth? (Owner: product)
3. **Email infrastructure:** Which provider (SendGrid, Mailgun, SMTP)? (Owner: ops)

## Timeline
- Target: 5 weeks MVP
- Hard deadline: 2026-09-30
- After-hours work (evenings/weekends)

## Milestone Status: OPEN
Track progress in the GitHub Projects board linked to this milestone.
EOF
)

PARENT_NUM=$(create_issue "[M-1] first-coordination-proof: Prove household coordination model" "$PARENT_BODY" "roadmap,milestone,m1,status:proposed" | tail -1)

# Foundations
F01_BODY=$(cat <<'EOF'
## Outcome
(foundation) Email/password registration and login endpoints working; users have session tokens and can log in independently.

## PRD Refs
- FR-001: User can register with email and password
- FR-002: User can log in with email and password

## Unlocks
- S-01 (user registration)
- All downstream user-facing work

## Prerequisites
None

## Blockers
None

## Unknowns
None

## Risk
Auth is the absolute first-week blocker — if delayed, all downstream work is blocked. Spring Boot has no Spring Security config yet, so this is critical path.

## Lesson Plan
Plan: `context/changes/auth-scaffold/plan.md`
EOF
)

F01_NUM=$(create_issue "[F-01] auth-scaffold: Email/password registration & login" "$F01_BODY" "foundation,f01,backend,priority:critical,status:proposed" | tail -1)

F02_BODY=$(cat <<'EOF'
## Outcome
(foundation) PostgreSQL schema established with households, users, household_members, and tasks tables. User-household relationships are enforced (isolation, multi-member households).

## PRD Refs
- FR-003: User can create a household
- FR-004: User can invite a partner to a household

## Unlocks
- S-02 (task persistence)
- S-03 (assignment)
- S-06 (dashboard queries)

## Prerequisites
- F-01 (need user context for household isolation)

## Blockers
None

## Unknowns
- **Telegram bot token ownership:** System-wide bot vs. per-household? Architectural choice affects F-03.

## Risk
Schema design cascades into Telegram architecture (F-03) and task assignment visibility (S-03). Get household isolation model right in week 1; redesigning in week 3 wastes critical time.

## Lesson Plan
Plan: `context/changes/household-schema/plan.md`
EOF
)

F02_NUM=$(create_issue "[F-02] household-schema: Household data model & PostgreSQL schema" "$F02_BODY" "foundation,f02,database,priority:critical,status:proposed" | tail -1)

F04_BODY=$(cat <<'EOF'
## Outcome
(foundation) React app bootstrapped with routing (React Router), build tooling (Vite or Webpack), component scaffolding, and state management skeleton (Context API or Redux). Development server running with hot-reload.

## PRD Refs
None (foundational)

## Unlocks
- S-01 (registration form UI)
- S-02 (task CRUD forms/views)
- S-03 (assignment picker UI)
- S-05 (real-time sync UI updates)
- S-06 (dashboard layout)
- S-07 (AI form UI)

## Prerequisites
None

## Blockers
None

## Unknowns
- **Frontend state management:** Redux, Context API, Zustand, or Jotai? (Owner: development team)
- **UI framework:** Headless components (Radix UI) or full library (Material-UI)? (Owner: product/design)

## Risk
Frontend setup in week 1 is critical to avoid late-stage bloat. React Router, build tool, and state management decisions cascade into all UI slices. Use lightweight stack (React 18 + React Router + Vite + Context API) to minimize complexity during time crunch.

## Lesson Plan
Plan: `context/changes/frontend-scaffold/plan.md`
EOF
)

F04_NUM=$(create_issue "[F-04] frontend-scaffold: React app, routing, build setup" "$F04_BODY" "foundation,f04,frontend,priority:critical,status:proposed" | tail -1)

F03_BODY=$(cat <<'EOF'
## Outcome
(foundation) Telegram bot initialized with token, webhook configured, system can send messages to users. User-bot communication authenticated.

## PRD Refs
- FR-015: Household member can link their Telegram account

## Unlocks
- S-04 (reminders)
- S-05 (2-way sync)

## Prerequisites
- F-01 (need user context)
- S-03 (need assigned tasks to know who to remind)

## Blockers
- Telegram bot token must be provisioned (blocked by F-02 unknown: bot ownership decision)

## Unknowns
- **Telegram bot API behavior:** Long-polling vs. WebSocket for message delivery; retry semantics for missed reminders. (Owner: development team. Block: YES)
- **User linking flow:** QR code, manual token entry, or OAuth? (Owner: product. Block: YES)

## Risk
Telegram bot token provisioning and user linking are critical path items. If not resolved by end of week 2, S-04 cannot ship. Set up test bot and linking flow early.

## Lesson Plan
Plan: `context/changes/telegram-bot-scaffold/plan.md`
EOF
)

F03_NUM=$(create_issue "[F-03] telegram-bot-scaffold: Telegram bot scaffolding, token management, webhook setup" "$F03_BODY" "foundation,f03,telegram,priority:critical,status:proposed" | tail -1)

# Slices
S01_BODY=$(cat <<'EOF'
## Outcome
User registers with email/password, creates a household, invites a partner via email, and partner accepts invite and logs in independently. Both users see same household on login.

## PRD Refs
- US-01: New User Setup & Household Creation
- FR-001 through FR-005

## Prerequisites
- F-01 (auth API)
- F-02 (household schema)
- F-04 (registration form UI)

## Blockers
- Email infrastructure provisioning (SMTP, SendGrid, Mailgun, etc.)

## Unknowns
None

## Risk
Invitation email delivery is hard dependency (partner must receive joinable link). Email infrastructure must be in place by end of week 1. If email setup delays, entire on-ramp blocks.

## Lesson Plan
Plan: `context/changes/new-user-setup/plan.md`
EOF
)

S01_NUM=$(create_issue "[S-01] new-user-setup: Register, create household, invite partner" "$S01_BODY" "slice,s01,user-visible,priority:high,status:proposed" | tail -1)

S02_BODY=$(cat <<'EOF'
## Outcome
User can create task with title, description, category, due date. View, edit, delete all household tasks. All changes persistent and visible to household members.

## PRD Refs
- US-02 (partial)
- FR-006, FR-008, FR-010–013

## Prerequisites
- F-01 (auth API)
- F-02 (household schema, task table)
- F-04 (task form UI, list view, category selector)

## Blockers
None

## Unknowns
None

## Risk
Category enum must match PRD exactly (cleaning, shopping, laundry, maintenance, bills); any mismatch breaks downstream dashboard grouping (S-06). Lock categories in schema design (F-02). Form validation in React (F-04) must align with backend (F-01).

## Lesson Plan
Plan: `context/changes/basic-task-crud/plan.md`
EOF
)

S02_NUM=$(create_issue "[S-02] basic-task-crud: Create, view, edit, delete task with title, description, category, due date" "$S02_BODY" "slice,s02,user-visible,priority:high,status:proposed" | tail -1)

S03_BODY=$(cat <<'EOF'
## Outcome
When task created or edited, user can assign to self or partner. Both household members see assignee on every task. Task list reflects who owns what.

## PRD Refs
- FR-007: Household member can assign a task

## Prerequisites
- S-02 (need tasks before assigning them)
- F-04 (assignment dropdown/selector UI)

## Blockers
None

## Unknowns
None

## Risk
Assignment is the core coordination mechanism (PRD: "tasks are never assigned to the household — always to a specific person"). Explicitness prevents tragedy-of-the-commons. Ensure assignment is required, not optional.

## Lesson Plan
Plan: `context/changes/task-assignment/plan.md`
EOF
)

S03_NUM=$(create_issue "[S-03] task-assignment: Assign task to self or partner" "$S03_BODY" "slice,s03,user-visible,priority:high,status:proposed" | tail -1)

S04_BODY=$(cat <<'EOF'
## Outcome
When task has due date and reminder time, Telegram bot sends notification at that time to assigned user. Reminder includes task title and due date. At least 80% of reminders deliver on time (per PRD guardrails).

## PRD Refs
- US-02
- FR-016: Telegram bot sends reminder notifications

## Prerequisites
- S-03 (need to know who to remind)
- F-03 (bot scaffolding)

## Blockers
- Telegram bot token (blocked by F-03 resolution)

## Unknowns
- (Resolved in F-03: polling vs. WebSocket behavior must be confirmed before this slice starts)

## Risk
This is the **NORTH STAR**: if reminders don't work reliably, core value prop fails. Test reminder delivery exhaustively in week 2–3 (send 100s of reminders, verify >80% on-time delivery). If Telegram retry semantics don't meet 80% threshold, implement polling fallback or escalate to queue-based delivery (Upstash).

## Lesson Plan
Plan: `context/changes/telegram-reminder/plan.md`
EOF
)

S04_NUM=$(create_issue "[S-04] telegram-reminder: Receive Telegram reminder at configured time (NORTH STAR)" "$S04_BODY" "slice,s04,user-visible,priority:critical,telegram,status:proposed" | tail -1)

S05_BODY=$(cat <<'EOF'
## Outcome
User can mark task complete directly from Telegram bot (via bot command or button on reminder message). Completion immediately reflected in web app — both users see task marked done without page reload or delay. No sync lag.

## PRD Refs
- FR-017, FR-018: Telegram task completion and real-time sync

## Prerequisites
- S-04 (reminder must exist before user can complete from bot)
- F-04 (React real-time UI updates)

## Blockers
None

## Unknowns
- Real-time sync mechanism (WebSocket vs. polling, latency target <500ms per NFR) must be proven in S-04 work

## Risk
2-way sync failure (bot completion not reflecting in web app) creates confusion and breaks trust. Test sync latency under load in week 3; if >500ms, add loading indicator or polling fallback. React component state updates must handle rapid bot completions without race conditions.

## Lesson Plan
Plan: `context/changes/telegram-completion-sync/plan.md`
EOF
)

S05_NUM=$(create_issue "[S-05] telegram-completion-sync: Mark task complete from Telegram, reflected immediately in web app" "$S05_BODY" "slice,s05,user-visible,priority:high,telegram,status:proposed" | tail -1)

S06_BODY=$(cat <<'EOF'
## Outcome
User sees dashboard of tasks due today or overdue, grouped by assignee (two columns: "Today's tasks for me" | "Today's tasks for partner"). View updates whenever task completed (either in app or bot). Provides at-a-glance visibility of who owns what.

## PRD Refs
- US-02 (partial)
- FR-014: Household member can view "Today's tasks"

## Prerequisites
- S-02 (need tasks)
- S-03 (need assignment to group by)
- F-04 (dashboard layout, grouping UI, real-time updates)

## Blockers
None

## Unknowns
None

## Risk
Dashboard is the daily entry point; it must be fast (query <500ms) and accurate (must reflect real-time updates from bot). Denormalize task counts/status in schema if needed to meet latency target. React component must efficiently re-render grouped task lists on updates.

## Lesson Plan
Plan: `context/changes/today-dashboard/plan.md`
EOF
)

S06_NUM=$(create_issue "[S-06] today-dashboard: View tasks due today, grouped by assignee" "$S06_BODY" "slice,s06,user-visible,priority:high,status:proposed" | tail -1)

S07_BODY=$(cat <<'EOF'
## Outcome
User can describe household characteristics ("kids and dog and 3-bedroom apartment"), AI generates 5–10 task suggestions (e.g., "check gutters", "vet appointment"). User can accept, reject, or customize suggestions before saving as tasks. At least 70% of generated tasks accepted by users (per PRD guardrails).

## PRD Refs
- US-03: AI Task Generation
- FR-019–020

## Prerequisites
- S-02 (tasks must exist; AI suggestions integrate into task list)
- F-04 (household description form UI, suggestion list display, accept/reject/customize buttons)

## Blockers
- OpenAI API key must be provisioned

## Unknowns
- **Prompt engineering for 70% acceptance rate:** How to design prompt so AI suggestions match expectations? (Owner: development team, post-MVP. Block: NO)
- **Household description input:** Free-form text, structured form, or multi-choice? (Owner: product. Block: NO)

## Risk
AI is a secondary feature (PRD marks it "Secondary" success criterion). If timeline pressure peaks, this slice can defer to v1.1 without breaking north star (task assignment + reminders). Front-load core 4 slices; AI can be early post-MVP feature.

## Lesson Plan
Plan: `context/changes/ai-task-generation/plan.md`
EOF
)

S07_NUM=$(create_issue "[S-07] ai-task-generation: Describe household, receive AI task suggestions, accept/save" "$S07_BODY" "slice,s07,user-visible,priority:medium,status:proposed" | tail -1)

echo -e "${GREEN}✓ Issues created${NC}\n"

# Phase 5: Link Dependencies
echo -e "${YELLOW}Phase 5: Linking issue dependencies...${NC}"

link_issues() {
  local blocker_num="$1"
  local dependent_num="$2"
  local relation="$3"  # "blocks" or "depends-on"

  gh issue edit "$dependent_num" \
    --repo "$REPO" \
    --add-"$relation" "$blocker_num" \
    2>/dev/null || echo "    (link may already exist)"
}

# S-01 depends on F-01, F-02, F-04
[ -n "$F01_NUM" ] && link_issues "$F01_NUM" "$S01_NUM" "depends-on"
[ -n "$F02_NUM" ] && link_issues "$F02_NUM" "$S01_NUM" "depends-on"
[ -n "$F04_NUM" ] && link_issues "$F04_NUM" "$S01_NUM" "depends-on"

# S-02 depends on F-01, F-02, F-04
[ -n "$F01_NUM" ] && link_issues "$F01_NUM" "$S02_NUM" "depends-on"
[ -n "$F02_NUM" ] && link_issues "$F02_NUM" "$S02_NUM" "depends-on"
[ -n "$F04_NUM" ] && link_issues "$F04_NUM" "$S02_NUM" "depends-on"

# S-03 depends on S-02, F-04
[ -n "$S02_NUM" ] && link_issues "$S02_NUM" "$S03_NUM" "depends-on"
[ -n "$F04_NUM" ] && link_issues "$F04_NUM" "$S03_NUM" "depends-on"

# F-03 depends on F-01, S-03
[ -n "$F01_NUM" ] && link_issues "$F01_NUM" "$F03_NUM" "depends-on"
[ -n "$S03_NUM" ] && link_issues "$S03_NUM" "$F03_NUM" "depends-on"

# S-04 depends on S-03, F-03
[ -n "$S03_NUM" ] && link_issues "$S03_NUM" "$S04_NUM" "depends-on"
[ -n "$F03_NUM" ] && link_issues "$F03_NUM" "$S04_NUM" "depends-on"

# S-05 depends on S-04, F-04
[ -n "$S04_NUM" ] && link_issues "$S04_NUM" "$S05_NUM" "depends-on"
[ -n "$F04_NUM" ] && link_issues "$F04_NUM" "$S05_NUM" "depends-on"

# S-06 depends on S-02, S-03, F-04
[ -n "$S02_NUM" ] && link_issues "$S02_NUM" "$S06_NUM" "depends-on"
[ -n "$S03_NUM" ] && link_issues "$S03_NUM" "$S06_NUM" "depends-on"
[ -n "$F04_NUM" ] && link_issues "$F04_NUM" "$S06_NUM" "depends-on"

# S-07 depends on S-02, F-04
[ -n "$S02_NUM" ] && link_issues "$S02_NUM" "$S07_NUM" "depends-on"
[ -n "$F04_NUM" ] && link_issues "$F04_NUM" "$S07_NUM" "depends-on"

echo -e "${GREEN}✓ Dependencies linked${NC}\n"

# Decision Issues
echo -e "${YELLOW}Phase 6: Creating decision/blocker issues...${NC}"

DECISION01_BODY=$(cat <<'EOF'
## Question
Is the Telegram bot token managed by the system (one bot for all households) or per-household?

## Owner
product/ops

## Impact
Architectural choice affects F-03 (bot deployment, user association) and F-01 (auth + bot linking).

## Blocks
- F-03: telegram-bot-scaffold

## Deadline
End of week 1 (needed before F-03 planning can start)
EOF
)

DECISION01_NUM=$(create_issue "[DECISION] Telegram bot token ownership: system-wide vs per-household?" "$DECISION01_BODY" "decision,blocker,f03-blocking,status:proposed" | tail -1)
[ -n "$F03_NUM" ] && [ -n "$DECISION01_NUM" ] && link_issues "$DECISION01_NUM" "$F03_NUM" "blocks"

DECISION02_BODY=$(cat <<'EOF'
## Question
How do users link their Telegram account to the household profile? QR code scan, manual token entry, or OAuth-style flow?

## Owner
product

## Impact
Affects UX design and security model for F-03 bot linking. Influences user onboarding flow.

## Blocks
- F-03: telegram-bot-scaffold

## Deadline
End of week 1 (needed before F-03 planning can start)
EOF
)

DECISION02_NUM=$(create_issue "[DECISION] Telegram user linking flow: QR code vs token vs OAuth?" "$DECISION02_BODY" "decision,blocker,f03-blocking,status:proposed" | tail -1)
[ -n "$F03_NUM" ] && [ -n "$DECISION02_NUM" ] && link_issues "$DECISION02_NUM" "$F03_NUM" "blocks"

DECISION03_BODY=$(cat <<'EOF'
## Question
Which email infrastructure provider (SMTP, SendGrid, Mailgun, etc.) will handle invitation emails for S-01?

## Owner
ops/infrastructure

## Impact
Blocks S-01 (New User Setup) from shipping. Must be provisioned before week 1 ends.

## Blocks
- S-01: new-user-setup

## Deadline
Day 1 of development (needed before S-01 planning can start)
EOF
)

DECISION03_NUM=$(create_issue "[DECISION] Email infrastructure provider for invitations" "$DECISION03_BODY" "decision,blocker,s01-blocking,status:proposed" | tail -1)
[ -n "$S01_NUM" ] && [ -n "$DECISION03_NUM" ] && link_issues "$DECISION03_NUM" "$S01_NUM" "blocks"

echo -e "${GREEN}✓ Decision issues created${NC}\n"

# Summary
echo -e "${BLUE}=== Migration Summary ===${NC}"
echo -e "${GREEN}✓ Phase 1: Labels created"
echo "✓ Phase 2: Milestone '$MILESTONE' created"
echo "✓ Phase 3: GitHub Projects board setup"
echo "✓ Phase 4: 11 issues created (1 parent + 4 foundations + 7 slices)"
echo "✓ Phase 5: Dependencies linked"
echo "✓ Phase 6: 3 decision/blocker issues created${NC}"

echo -e "\n${BLUE}Next Steps:${NC}"
echo "1. View all issues: https://github.com/okrajni/10x-cli/issues?label=roadmap"
echo "2. See GitHub Projects board: https://github.com/okrajni/10x-cli/projects"
echo "3. Update roadmap.md with GitHub issue links for reference"
echo "4. Start planning with: gh issue view #N (replace N with issue number)"

echo -e "\n${YELLOW}Manual Steps (if needed):${NC}"
echo "- Add issues to GitHub Projects board columns manually if automation didn't work"
echo "- Set priority labels on individual issues as needed"
echo "- Pin the parent issue (#$PARENT_NUM) for visibility"

echo -e "\n${GREEN}Migration complete!${NC}\n"
