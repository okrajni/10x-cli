---
change_id: auth-scaffold
title: Auth scaffold — email/password registration and login
status: implementing
created: 2026-08-28
updated: 2026-08-28
archived_at: null
---

## Notes

Foundation work from roadmap F-01. Build email/password registration and login endpoints so users have session tokens and can log in independently. This unblocks all downstream user-facing work — users must be authenticated before any coordination features work.

**Roadmap context:**
- Unlocks: S-01 (user registration), all downstream user-facing work
- Risk: Auth is critical path; if delayed, all downstream work blocks
- Status: proposed
