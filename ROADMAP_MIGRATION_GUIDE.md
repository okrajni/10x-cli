# Roadmap to GitHub Issues Migration Guide

## Overview

This guide walks you through migrating the `context/foundation/roadmap.md` milestone to GitHub Issues using the provided migration script.

**What will be created:**
- 30+ GitHub labels (for categorization, status, priority)
- 1 GitHub milestone: "M-1: first-coordination-proof"
- 1 GitHub Projects board with columns: Proposed → Ready → In Progress → Done
- 14 GitHub issues:
  - 1 parent issue (M-1 milestone container)
  - 4 Foundation issues (F-01, F-02, F-03, F-04)
  - 7 Slice issues (S-01 through S-07)
  - 3 Decision/Blocker issues (Telegram ownership, user linking, email provider)
- Full dependency linking between all issues

## Prerequisites

1. **GitHub CLI installed**: [Install gh](https://cli.github.com)
   ```bash
   # macOS
   brew install gh
   
   # Linux
   curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | sudo dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg
   sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-key 23F3D4EA75716059
   echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | sudo tee /etc/apt/sources.list.d/github-cli.list > /dev/null
   sudo apt update
   sudo apt install gh
   ```

2. **GitHub CLI authenticated**:
   ```bash
   gh auth login
   # Choose: GitHub.com
   # Choose: HTTPS
   # Choose: Y for credential helper
   # Choose: Y for git credential protocol
   # Authorize in browser
   ```

3. **Repository access**:
   ```bash
   gh repo set-default okrajni/10x-cli
   ```

## Running the Migration

### Step 1: Make the script executable

```bash
chmod +x scripts/migrate-roadmap-to-github.sh
```

### Step 2: Run the migration

```bash
bash scripts/migrate-roadmap-to-github.sh
```

The script will:
1. Create all labels (~30 total)
2. Create the M-1 milestone
3. Create a GitHub Projects board
4. Create all 14 issues
5. Link dependencies between issues
6. Create decision/blocker issues

**Expected output:**
```
=== Roadmap to GitHub Issues Migration ===

Phase 1: Creating labels...
  Creating label: roadmap
  Creating label: milestone
  ...
✓ Labels created

Phase 2: Creating milestone...
✓ Milestone created

...

Migration complete!

Next Steps:
1. View all issues: https://github.com/okrajni/10x-cli/issues?label=roadmap
2. See GitHub Projects board: https://github.com/okrajni/10x-cli/projects
...
```

### Step 3: Verify the migration

Check that all issues were created:
```bash
gh issue list --repo okrajni/10x-cli --label roadmap --state open --json number,title
```

Expected: 14 issues listed

### Step 4 (Optional): Set up GitHub Projects board automation

If the Projects board was created, add a workflow to auto-add labeled issues:
1. Go to: https://github.com/okrajni/10x-cli/projects
2. Click on the "Roadmap Board: M-1" project
3. Click Settings → Automation
4. Set: "When a pull request is labeled..." or similar automation rules

## What the Issues Look Like

### Parent Issue (#N)
```
Title: [M-1] first-coordination-proof: Prove household coordination model

Status: open
Milestone: M-1: first-coordination-proof
Labels: roadmap, milestone, m1, status:proposed

Body includes:
- Milestone intent
- Links to all foundations and slices
- Open questions
- Timeline (5 weeks MVP, deadline 2026-09-30)
```

### Foundation Issue (Example: F-01)
```
Title: [F-01] auth-scaffold: Email/password registration & login

Status: open
Milestone: M-1: first-coordination-proof
Labels: foundation, f01, backend, priority:critical, status:proposed

Body includes:
- Outcome: (foundation) Email/password registration and login endpoints...
- PRD Refs: FR-001, FR-002
- Unlocks: S-01, all downstream work
- Prerequisites: None
- Blockers: None
- Unknowns: None
- Risk: Auth is absolute first-week blocker...
- Lesson Plan: Plan: context/changes/auth-scaffold/plan.md

Linked Issues:
- Blocks: S-01, S-02, etc. (all downstream work)
```

### Slice Issue (Example: S-04 - North Star)
```
Title: [S-04] telegram-reminder: Receive Telegram reminder at configured time (NORTH STAR)

Status: open
Milestone: M-1: first-coordination-proof
Labels: slice, s04, user-visible, priority:critical, telegram, status:proposed

Body includes:
- Outcome: When task has due date and reminder time...
- PRD Refs: US-02, FR-016
- Prerequisites: S-03 (need to know who to remind), F-03 (bot scaffolding)
- Blockers: Telegram bot token
- Unknowns: (resolved in F-03)
- Risk: This is the NORTH STAR...
- Lesson Plan: Plan: context/changes/telegram-reminder/plan.md

Linked Issues:
- Depends on: S-03, F-03
- Blocked by: Decision issues for Telegram ownership, user linking
```

### Decision Issue (Example: Telegram Ownership)
```
Title: [DECISION] Telegram bot token ownership: system-wide vs per-household?

Status: open
Labels: decision, blocker, f03-blocking, status:proposed

Body includes:
- Question: Is the bot token managed by the system (one bot for all households) or per-household?
- Owner: product/ops
- Impact: Architectural choice affects F-03 and F-01
- Blocks: F-03: telegram-bot-scaffold
- Deadline: End of week 1

Linked Issues:
- Blocks: F-03
```

## Issue Labels Reference

| Category | Labels |
|----------|--------|
| **Type** | `foundation`, `slice`, `decision`, `blocker`, `milestone` |
| **Status** | `status:proposed`, `status:ready`, `status:blocked`, `status:in-progress`, `status:done` |
| **Category** | `backend`, `frontend`, `database`, `telegram`, `user-visible` |
| **Priority** | `priority:critical`, `priority:high`, `priority:medium` |
| **ID** | `f01`, `f02`, `f03`, `f04`, `s01`, `s02`, `s03`, `s04`, `s05`, `s06`, `s07` |
| **Source** | `roadmap` |

## Using the Issues

### Viewing & Filtering

```bash
# View all roadmap issues
gh issue list --repo okrajni/10x-cli --label roadmap

# View only ready-to-plan slices
gh issue list --repo okrajni/10x-cli --label "status:ready"

# View only blocked issues
gh issue list --repo okrajni/10x-cli --label "status:blocked"

# View north star (S-04)
gh issue list --repo okrajni/10x-cli --label "s04"

# View all foundations
gh issue list --repo okrajni/10x-cli --label "foundation"
```

### Reading an Issue

```bash
# Read issue #123
gh issue view 123 --repo okrajni/10x-cli

# Read with web browser
gh issue view 123 --repo okrajni/10x-cli --web
```

### Updating Issue Status

```bash
# Mark an issue as ready (when dependencies are met)
gh issue edit 123 --repo okrajni/10x-cli --remove-label "status:proposed" --add-label "status:ready"

# Mark as blocked (when a dependency fails)
gh issue edit 123 --repo okrajni/10x-cli --add-label "status:blocked"

# Mark as in-progress
gh issue edit 123 --repo okrajni/10x-cli --add-label "status:in-progress"

# Mark as done
gh issue edit 123 --repo okrajni/10x-cli --add-label "status:done"
```

### Planning a Slice

When you're ready to plan a slice (e.g., S-01), run:
```bash
# View the issue to understand prerequisites
gh issue view <issue-number> --repo okrajni/10x-cli --web

# Then run /10x-plan with the Change ID
# Example: /10x-plan new-user-setup
```

The issue body includes:
- What needs to be done (Outcome)
- Why it matters (PRD refs)
- What needs to be done first (Prerequisites)
- Risks to watch for (Risk)
- Link to detailed plan (Lesson Plan: context/changes/...)

## Integrating with Roadmap.md

After migration, update `context/foundation/roadmap.md` with issue links:

```markdown
## At a glance

| ID    | Change ID         | Outcome | Prerequisites | PRD refs | Status   | GitHub Issue |
|-------|-------------------|---------|---------------|----------|----------|--------------|
| F-01  | auth-scaffold     | ... | — | FR-001, FR-002 | proposed | #123 |
| S-01  | new-user-setup    | ... | F-01, F-02, F-04 | US-01, FR-001–005 | proposed | #124 |
...
```

This creates bidirectional traceability:
- GitHub Issues → `context/changes/` for detailed planning
- `context/changes/` → Roadmap.md for milestone context
- Roadmap.md → GitHub Issues for team tracking

## Troubleshooting

### Issue: "gh: command not found"
**Solution**: Install GitHub CLI first
```bash
brew install gh  # macOS
apt install gh   # Linux
```

### Issue: "authentication required"
**Solution**: Authenticate with GitHub
```bash
gh auth login
```

### Issue: "not authorized to perform this action"
**Solution**: Ensure you have write access to the repo
```bash
gh repo set-default okrajni/10x-cli
gh auth status
```

### Issue: "labels already exist" errors
**Solution**: This is normal and safe. The script continues and reuses existing labels.

### Issue: "milestone already exists"
**Solution**: Also normal. The script will use the existing milestone.

### Issue: "project creation failed"
**Solution**: Create the project manually:
1. Go to: https://github.com/okrajni/10x-cli/projects/new
2. Name: "Roadmap Board: M-1"
3. Choose "Table" template
4. Create columns: "Proposed", "Ready", "In Progress", "Done"
5. Then add issues manually via the board UI

## After Migration: Workflow

1. **Check milestone status**: View all issues at https://github.com/okrajni/10x-cli/issues?label=roadmap

2. **Plan a slice**:
   - Pick an issue with `status:ready`
   - Read the issue to understand what needs to be done
   - Run `/10x-plan <change-id>` to create a detailed plan

3. **Track progress**:
   - Update issue status as work progresses
   - Use GitHub Projects board for visual overview
   - Link pull requests to issues for traceability

4. **Update roadmap.md**:
   - When a slice is archived (via `/10x-archive`), that issue gets `status:done`
   - Roadmap.md is the source-of-truth for milestone structure
   - GitHub Issues are the external tracking for the team

## Questions?

Refer back to the plan file:
```bash
cat /Users/joannao@backbase.com/.claude/plans/i-would-like-to-unified-waterfall.md
```

Or check the roadmap:
```bash
cat context/foundation/roadmap.md
```
