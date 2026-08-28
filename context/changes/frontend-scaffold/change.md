---
change_id: frontend-scaffold
title: Frontend Scaffold - React, Vite, Router Setup
roadmap_id: F-04
status: implementing
created: 2026-08-28
updated: 2026-08-28
team_member: joannao@backbase.com
---

# Frontend Scaffold (F-04)

## Summary

React frontend foundation for "done yet?" household task management app. This change implements:
- Vite + TypeScript build tooling with strict type checking
- React Router v6 with protected routes and lazy loading
- Context API + useReducer for auth state management
- Custom fetch wrapper for Spring Boot integration
- Tailwind CSS + Radix UI component foundation
- Domain-driven folder structure (features/*, lib/*, shared/*)

**Unlocks:** All 6 downstream UI slices (S-01, S-02, S-03, S-05, S-06, S-07)

**Critical path:** Week 1 foundation. No backend dependencies.

## Related Documents

- **Plan:** `context/changes/frontend-scaffold/plan.md`
- **Brief:** `context/changes/frontend-scaffold/plan-brief.md`
- **Roadmap:** `context/foundation/roadmap.md` (F-04 details)
- **PRD:** `context/foundation/prd.md` (feature requirements)

## Tech Stack (Confirmed)

- **Build tool:** Vite (fast HMR, instant feedback)
- **State management:** Context API + useReducer (lightweight, no dependencies)
- **Styling:** Tailwind CSS + Radix UI (composable, accessible, fast iteration)
- **API layer:** Custom fetch wrapper with error handling (zero dependencies, full control)
- **Folder structure:** Domain-driven by feature (features/auth, features/tasks, etc.)
- **Testing:** Unit tests for hooks/utilities only (focus on scaffold speed)
- **DevOps:** TypeScript strict mode, ESLint, Prettier, pre-commit hooks, HMR with state preservation

## Key Decisions

1. **Real-time sync:** Polling (refetch on interval/focus) — sufficient for MVP, simpler than WebSocket
2. **Auth storage:** HttpOnly cookie (from Spring) + refresh token flow — secure by default
3. **Error handling:** User-friendly toast + automatic retry for recoverable errors
4. **Component library:** Radix UI primitives styled with Tailwind (lightweight, accessible, customizable)
5. **Route loading:** Lazy loading with code-splitting for smaller initial bundle

## Success Criteria (from plan)

When this change completes:

✅ **Dev server running:** `npm run dev` starts Vite with HMR on `http://localhost:5173`
✅ **Project structure:** Folder hierarchy (features/*, lib/*, shared/*) ready for downstream slices
✅ **Auth flow:** Login → dashboard → logout wired with Context + protected routes
✅ **Component library:** Button, Input, Dialog, TaskCard ready for reuse
✅ **API integration:** Custom fetch wrapper + error handling + token refresh working
✅ **Build validated:** `npm run build` produces optimized production bundle
✅ **No type errors:** `npm run typecheck` passes with strict mode
✅ **Linting passes:** `npm run lint` finds zero issues

## Notes

- **Time estimate:** 3-4 hours solo (includes dev server, auth scaffold, basic component library, integration testing)
- **Dependency on:** None (F-04 is a pure foundation, parallel to F-01, F-02)
- **Blocks:** S-01 (registration UI), S-02 (task CRUD UI), S-03 (assignment UI), S-05 (real-time UI), S-06 (dashboard), S-07 (AI form)
- **Iteration point:** After Phase 3 (API layer), can add pages for S-01/S-02 without waiting for Phase 5 (Router) to complete
