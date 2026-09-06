# ✅ Roadmap to GitHub Issues Migration Complete

**Date:** 2026-08-28  
**Milestone:** M-1: first-coordination-proof  
**Status:** LIVE on GitHub

---

## What Was Created

### 📋 Issues (14 total)

#### Parent Issue
- **[M-1] first-coordination-proof** — Milestone container with full roadmap overview, open questions, and timeline

#### Foundations (4)
- **[F-01] auth-scaffold** — Email/password registration & login endpoints
- **[F-02] household-schema** — PostgreSQL schema with household isolation
- **[F-03] telegram-bot-scaffold** — Telegram bot setup, token management, webhooks
- **[F-04] frontend-scaffold** — React app, routing, build tooling

#### Slices (7)
- **[S-01] new-user-setup** — Register, create household, invite partner
- **[S-02] basic-task-crud** — Create, view, edit, delete tasks
- **[S-03] task-assignment** — Assign task to self or partner
- **[S-04] telegram-reminder** — ⭐ NORTH STAR: Receive reminders at configured time
- **[S-05] telegram-completion-sync** — Mark complete from bot, sync to web app
- **[S-06] today-dashboard** — View tasks due today, grouped by assignee
- **[S-07] ai-task-generation** — AI-suggested tasks based on household description

#### Decisions (3)
- **[DECISION] Telegram bot ownership** — System-wide vs per-household? (Blocks F-03)
- **[DECISION] Telegram user linking** — QR code vs token vs OAuth? (Blocks F-03)
- **[DECISION] Email infrastructure provider** — SendGrid vs Mailgun vs SMTP? (Blocks S-01)

### 🏷️ Labels (~30)

| Category | Labels |
|----------|--------|
| **Type** | `foundation`, `slice`, `decision`, `blocker`, `milestone` |
| **Status** | `status:proposed`, `status:ready`, `status:blocked`, `status:in-progress`, `status:done` |
| **Category** | `backend`, `frontend`, `database`, `telegram`, `user-visible` |
| **Priority** | `priority:critical`, `priority:high`, `priority:medium` |
| **ID** | `f01`, `f02`, `f03`, `f04`, `s01`, `s02`, `s03`, `s04`, `s05`, `s06`, `s07` |
| **Source** | `roadmap` |

### 📊 Milestone
- **M-1: first-coordination-proof** — Groups all issues together, MVP deadline 2026-09-30

### 🎪 GitHub Projects Board
- **Roadmap Board: M-1** — Visual kanban with columns: Proposed → Ready → In Progress → Done

### 🔗 Dependencies
All prerequisites are linked via GitHub issue "depends on" relationships:
- S-01 ← F-01, F-02, F-04
- S-02 ← F-01, F-02, F-04
- S-03 ← S-02, F-04
- S-04 ← S-03, F-03 (⭐ North Star)
- S-05 ← S-04, F-04
- S-06 ← S-02, S-03, F-04
- S-07 ← S-02, F-04
- F-03 ← F-01, S-03

---

## 🔗 Quick Links

### View Issues
- **All roadmap issues:** https://github.com/okrajni/10x-cli/issues?label=roadmap
- **Foundations only:** https://github.com/okrajni/10x-cli/issues?label=foundation
- **Slices only:** https://github.com/okrajni/10x-cli/issues?label=slice
- **North Star (S-04):** https://github.com/okrajni/10x-cli/issues?label=s04
- **Decisions/Blockers:** https://github.com/okrajni/10x-cli/issues?label=decision
- **By Milestone:** https://github.com/okrajni/10x-cli/issues?milestone=M-1

### GitHub Projects
- **Roadmap Board:** https://github.com/okrajni/10x-cli/projects

---

## 📚 Documentation

| File | Purpose |
|------|---------|
| `context/foundation/roadmap.md` | Source-of-truth milestone structure (vertical slices, dependencies, unknowns) |
| `ROADMAP_MIGRATION_GUIDE.md` | Complete guide to using issues, filtering, updating status |
| `scripts/migrate-roadmap-to-github.sh` | Bash script that created all issues (can re-run to update) |
| `.claude/plans/i-would-like-to-unified-waterfall.md` | Implementation plan and decisions locked in |

---

## 🚀 Next Steps

### 1. View the Issues
Open https://github.com/okrajni/10x-cli/issues?label=roadmap to see all 14 issues.

Click on the parent issue (**[M-1]**) to see:
- Milestone intent
- All foundations and slices
- Open questions blocking work
- Timeline and context

### 2. Plan Your First Slice
Start with a Foundation that's ready to plan:
- **F-01 (auth-scaffold)** has no prerequisites → plan immediately
- **F-02 (household-schema)** depends on F-01 → can plan after/parallel
- **F-04 (frontend-scaffold)** has no prerequisites → plan immediately

```bash
# View F-01 issue
gh issue view <F-01-number> --repo okrajni/10x-cli

# Then plan it
/10x-plan auth-scaffold
```

Each issue body includes:
- ✅ What needs to be done (Outcome)
- 📋 Why it matters (PRD refs)
- 🎯 What must be done first (Prerequisites)
- ⚠️ Things to watch for (Risk)
- 📖 Link to detailed plan folder (Lesson Plan: context/changes/...)

### 3. Update Issue Status as You Work
```bash
# When dependencies are met → mark as ready
gh issue edit #N --repo okrajni/10x-cli --remove-label "status:proposed" --add-label "status:ready"

# When starting work → mark as in-progress
gh issue edit #N --repo okrajni/10x-cli --add-label "status:in-progress"

# When complete → mark as done
gh issue edit #N --repo okrajni/10x-cli --add-label "status:done"
```

### 4. Track Progress
- **GitHub Projects board:** Visual view of milestone progress
- **Roadmap.md:** Source-of-truth for milestone structure
- **context/changes/** folders: Detailed plans per slice

### 5. Resolve Blocking Decisions (Critical Path)
Before F-03 (Telegram bot) can be planned, resolve:
1. ✅ **Telegram bot ownership model** → decision issue
2. ✅ **User Telegram linking flow** → decision issue

Before S-01 (New user setup) can ship, resolve:
1. ✅ **Email infrastructure provider** → decision issue

---

## 📊 Milestone Status at a Glance

| Component | Count | Ready? | Notes |
|-----------|-------|--------|-------|
| Foundations | 4 | No | F-01, F-04 ready to plan. F-02 ready after F-01. F-03 blocked on decisions. |
| Slices | 7 | No | None ready yet (all depend on foundations). S-04 is north star. |
| Decisions | 3 | No | 3 blockers must be resolved before F-03, S-01 can fully plan |
| Dependencies | 20+ | ✅ | All linked in GitHub issues |
| Timeline | 5 weeks | ⏰ | Hard deadline: 2026-09-30 (after-hours work) |

---

## 🎯 Recommended Sequence

### Week 1 (Foundation Week)
1. ✅ Resolve 3 decision issues (parallel)
2. ✅ Plan & implement F-01 (auth-scaffold)
3. ✅ Plan & implement F-04 (frontend-scaffold)
4. ✅ Plan & implement F-02 (household-schema)
5. ✅ Set up email infrastructure

### Week 2-3 (Core MVP)
1. ✅ Implement S-01 (new-user-setup)
2. ✅ Implement S-02 (basic-task-crud)
3. ✅ Implement S-03 (task-assignment)
4. ✅ Plan & implement F-03 (telegram-bot-scaffold)

### Week 3-4 (North Star)
1. ✅ Implement S-04 (telegram-reminder) — NORTH STAR
2. ✅ Implement S-05 (telegram-completion-sync)

### Week 4-5 (Features + Cleanup)
1. ✅ Implement S-06 (today-dashboard)
2. ✅ Implement S-07 (ai-task-generation) — or defer to v1.1
3. ✅ Testing, polish, launch prep

---

## 🔄 Integration with Lesson-Based Planning

**Bidirectional traceability:**

```
GitHub Issues (external tracking)
        ↕
Roadmap.md (milestone structure)
        ↕
context/changes/<change-id>/plan.md (detailed plans)
        ↕
context/changes/<change-id>/IMPLEMENTATION.md (code changes)
```

When you plan a slice:
```bash
/10x-plan new-user-setup
# Creates: context/changes/new-user-setup/plan.md
# Links to: GitHub issue S-01
# References: Roadmap M-1 in milestone context
```

When you archive a completed change:
```bash
/10x-archive new-user-setup
# Moves plan to: context/archive/2026-XX-XX-new-user-setup/
# Updates: GitHub issue S-01 → status:done
# Updates: Roadmap.md → S-01 in ## Done section
```

---

## 📝 Updating Roadmap.md (Optional but Recommended)

Add a "GitHub Issues" column to the roadmap's At a Glance table:

```markdown
| ID | Change ID | Outcome | Prerequisites | PRD refs | Status | GitHub Issue |
|---|---|---|---|---|---|---|
| F-01 | auth-scaffold | ... | — | FR-001, FR-002 | proposed | #123 |
| S-01 | new-user-setup | ... | F-01, F-02, F-04 | US-01, FR-001–005 | proposed | #124 |
| S-04 | telegram-reminder | ... | S-03, F-03 | US-02, FR-016 | proposed | #127 |
```

This creates a single reference point for both systems.

---

## ✅ Verification Checklist

- [ ] All 14 issues visible at https://github.com/okrajni/10x-cli/issues?label=roadmap
- [ ] Milestone M-1 visible at https://github.com/okrajni/10x-cli/issues?milestone=M-1
- [ ] Dependencies linked (view any issue to see "Depends on" section)
- [ ] 3 decision issues blocking F-03 and S-01
- [ ] GitHub Projects board accessible
- [ ] Each issue body includes lesson plan link (context/changes/...)
- [ ] Status labels set to `status:proposed` for all

---

## 🎉 You're Ready!

The roadmap is now live on GitHub. Start with:

```bash
# View parent milestone
gh issue view <parent-issue-number> --repo okrajni/10x-cli --web

# Or go directly to
https://github.com/okrajni/10x-cli/issues?label=roadmap
```

Then plan your first foundation:
```bash
/10x-plan auth-scaffold
```

Happy shipping! 🚀

---

**Migration completed by:** Roadmap to GitHub Issues migration script  
**Created:** 2026-08-28  
**Total issues:** 14  
**Total labels:** ~30  
**Dependencies tracked:** 20+  
**Status:** LIVE
