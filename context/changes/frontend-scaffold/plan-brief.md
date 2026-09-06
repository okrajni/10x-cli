# Frontend Scaffold — Plan Brief

> Full plan: `context/changes/frontend-scaffold/plan.md`
> Roadmap: `context/foundation/roadmap.md` (F-04 details)

## What & Why

React frontend foundation for "done yet?" household task management app. Bootstrapping a **lightweight, fast-iterating stack** (Vite + React 18 + React Router + Context API + Tailwind CSS + Radix UI) optimized for solo development on a 5-week MVP timeline. This scaffold unblocks all 6 downstream UI slices (S-01 through S-07) and establishes patterns for component structure, state management, API integration, and styling that scale through the full MVP.

## Starting Point

- **Frontend:** Absent — no React app, only landing page placeholder
- **Backend:** Spring Boot initialized with Spring Security + JWT, no endpoints yet
- **Build pipeline:** GitHub Actions CI configured, ready for frontend integration
- **Conventions:** Proven patterns from 10x-CLI (auth state management, API error handling, TypeScript strict mode)

## Desired End State

After this plan completes, the React frontend is **ready to accept feature work** (S-01, S-02, etc.):
- Dev server runs with hot-module-reload (HMR) on `http://localhost:5173`
- Project structure is organized domain-by-feature (`src/features/auth/`, `src/features/tasks/`, etc.)
- Auth flow works end-to-end (login → dashboard → logout) with Context API + protected routes
- Component library ready (Button, Input, Dialog, TaskCard — all keyboard-navigable, accessible)
- API integration pattern established (custom fetch wrapper with error handling, token refresh, type safety)
- Production build validates (TypeScript strict mode, ESLint, Prettier, <5MB gzipped)

## Key Decisions Made

| Decision                 | Choice                                      | Why (1 sentence)                                | Source |
|--------------------------|---------------------------------------------|--------------------------------------------------|--------|
| Build tool               | Vite (not Webpack/Create React App)        | Instant HMR + fast rebuild = rapid iteration on 5-week timeline | Plan  |
| State management         | Context API + useReducer (not Redux)       | Zero dependencies, lightweight, sufficient for 3-4 context slices | Plan  |
| Styling                  | Tailwind CSS + Radix UI (not Material-UI)  | Headless components + utility CSS = fast iteration without design bloat | Plan  |
| API layer                | Custom fetch wrapper (not React Query)     | Full control, no dependency overhead, pattern proven in 10x-CLI   | Plan  |
| Folder structure         | Domain-driven by feature (features/auth/*) | Scales well, clear ownership, matches downstream slices           | Plan  |
| Real-time sync           | Polling (not WebSocket)                    | Sufficient for MVP guardrails, simpler infrastructure              | Plan  |
| Auth storage             | HttpOnly cookie + localStorage              | Secure by default, simplifies token refresh handling              | Plan  |
| Error handling           | User-friendly toast + automatic retry      | Recoverable errors are retried transparently, user sees clear messages | Plan  |
| Testing approach         | Unit tests for hooks only (no snapshots)   | Focus scaffold speed, integration tests defer to S-01+             | Plan  |

## Scope

**In scope:** Vite project setup, TypeScript strict mode, React Router with lazy loading, Context API + auth state, custom fetch wrapper, Tailwind + Radix component library, protected routes, dev server with HMR, production build validation.

**Out of scope:** Server-side rendering, WebSocket real-time, offline-first, mobile-responsive deep-dive, testing beyond unit tests for hooks, environment configuration (.env), design token customization, advanced component library polish.

## Architecture / Approach

**Incremental, layered foundation:** Each phase builds on the previous, enabling manual testing after each step.

1. **Phase 1: Build tooling** — Vite + TypeScript + ESLint + Prettier + pre-commit hooks
2. **Phase 2: Auth state** — Context API + useReducer, login/logout/token refresh, mock flow
3. **Phase 3: API layer** — Custom fetch wrapper, error handling, domain-specific modules (auth.ts, tasks.ts, household.ts)
4. **Phase 4: UI components** — Button, Input, Dialog, TaskCard (all Radix + Tailwind, keyboard-navigable, accessible)
5. **Phase 5: Router** — React Router v6, protected routes, layout, lazy loading, navigation

After Phase 5, the scaffold is production-ready. Downstream slices (S-01, S-02, etc.) reuse established patterns without rearchitecting.

## Phases at a Glance

| Phase | What it delivers | Key risk |
|-------|-----------------|----------|
| 1. Build Tooling | Dev server with HMR, project structure, TypeScript config | Vite config complexity (mitigated: use proven defaults) |
| 2. Auth State | Context API + login/logout + token refresh | Lifecycle hooks for token expiry (mitigated: pattern from 10x-CLI) |
| 3. API Layer | Custom fetch wrapper + error handling + domain modules | Type alignment with Spring Boot responses (mitigated: later, real backend provides schema) |
| 4. UI Components | Button, Input, Dialog, TaskCard + accessibility | Radix UI learning curve (mitigated: excellent docs, only 4 components for MVP) |
| 5. Router | Protected routes + layout + lazy loading | Navigation edge cases (mitigated: simple route structure, no nested routes yet) |

**Prerequisites:** None — F-04 is a pure foundation with no backend dependencies.
**Estimated effort:** 3–4 hours solo (includes dev server, auth scaffold, component library, integration testing, build validation).

## Open Risks & Assumptions

- **Spring Boot API contract:** Assumes backend endpoints follow REST conventions (POST /auth/login, POST /auth/refresh, etc.). If backend deviates, API module wiring in Phase 3 may need adjustment.
- **TypeScript strict mode:** Strict mode catches bugs early but requires thorough type annotations. Trade-off: more typing upfront, fewer runtime bugs later (acceptable for MVP).
- **Tailwind CSS polish:** MVP uses Tailwind defaults (gray, blue, red, green colors). Design customization defers to post-MVP. If branding is critical in week 1, add 1–2 hours for color token setup.

## Success Criteria (Summary)

✅ **Dev experience:** `npm run dev` starts Vite with HMR on `http://localhost:5173`, edits appear instantly  
✅ **Auth flow:** Login form → submit → Context updates → navigate to dashboard → logout → redirect to login (no backend calls needed)  
✅ **Component library:** Button (4 variants), Input (text/password/email with errors), Dialog (modal), TaskCard (preview) — all keyboard-navigable, screen-reader-accessible  
✅ **Type safety:** `npm run typecheck` passes with zero errors (strict mode enabled)  
✅ **Production build:** `npm run build` succeeds, output <5MB gzipped  
✅ **Accessibility:** All interactive elements have visible focus indicators, keyboard nav works everywhere

---

**Next move:** Read `context/changes/frontend-scaffold/plan.md` for the full 5-phase breakdown with detailed implementation steps, code samples, success criteria per phase, and testing strategy.
