# Remove Multi-User Scaffold — Plan Brief

> Full plan: `context/changes/remove-multi-user-scaffold/plan.md`
> Roadmap: `context/foundation/roadmap.md` (F-02 cleanup)

## What & Why

We're removing the multi-user invitation and membership infrastructure (HouseholdMembers, HouseholdInvitations, InvitePartnerPage, InvitationAcceptPage, and related services) that was scaffolded during multi-user design but is **not part of the single-user MVP**. Per the PRD, multi-user household sharing is explicitly deferred to v1.1. This cleanup removes dead code, reduces schema complexity, and establishes a lean foundation before shipping S-06 (dashboard) and S-07 (AI task generation).

## Starting Point

Today, the codebase has full multi-user infrastructure in place:
- **Backend**: HouseholdMember and HouseholdInvitation entities, repositories, services, and endpoints; household queries look up users via membership, not ownership.
- **Frontend**: Two full pages for inviting partners and accepting invitations; routes for `/invitation/accept/:token` and `/household/:id/invite`.
- **Tasks**: Task.assignee field and HouseholdMemberSelect component (unused in MVP).
- **Database**: Three extra tables (household_members, household_invitations) that are never populated in single-user flow.

This adds schema complexity, test surface, and code to maintain without adding user value in the MVP.

## Desired End State

After this change:
- **Household ownership is enforced via `createdBy == userId`**, not membership lookups.
- **Single-user data isolation**: A user's `GET /api/household` returns only households they created.
- **No invitation infrastructure**: No InvitePartnerPage, InvitationAcceptPage, sendInvitation endpoint, or invitation routes.
- **Simplified tasks**: No assignee field or member selection component.
- **Lean test suite**: Multi-user tests removed; single-user happy path verified.
- **Database schema**: Only users, households, and tasks tables (no household_members or household_invitations).

End-to-end user flow: register → create household → create task → view on dashboard. No invitation or multi-household complexity.

## Key Decisions Made

| Decision                              | Choice                                  | Why                                          | Source |
| ------------------------------------- | --------------------------------------- | -------------------------------------------- | ------ |
| **Task assignment scope**             | Remove entirely (not keep for v1.1)     | Keeps MVP clean; v1.1 will re-add with full multi-user context. | Plan   |
| **Test strategy**                     | Delete multi-user tests, keep single-user | Smaller test surface; single-user happy path is what matters now. | Plan   |
| **Registration flow**                 | Simple refactor — remove invitation logic | Clean MVP flow; no surprises for users; old invite links silently ignored. | Plan   |
| **Rollout approach**                  | Phased backend-first, then frontend     | Compile errors guide the work; backend consistency before UI changes. | Plan   |
| **Documentation**                     | Add SCAFFOLD_REMOVAL.md                 | Git log alone doesn't capture context; v1.1 will benefit from seeing what was removed and why. | Plan   |

## Scope

**In scope:**
- Delete HouseholdMember, HouseholdInvitation, HouseholdMemberRole entities and repositories
- Delete InvitationController; remove sendInvitation endpoint from HouseholdController
- Delete InvitePartnerPage, InvitationAcceptPage, HouseholdMemberSelect components
- Remove `/invitation/accept/:token` and `/household/:id/invite` routes
- Refactor HouseholdService to use single-user queries (createdBy)
- Refactor Household entity to remove members relationship
- Simplify RegisterPage, HouseholdCreatePage, DashboardPage
- Delete HouseholdMemberRepositoryTest, HouseholdInvitationRepositoryTest
- Add single-user household query tests
- Remove invitation API functions and DTOs

**Out of scope:**
- Multi-user support is not added in this change (v1.1 work)
- Data migration (greenfield MVP has no production data; Hibernate handles schema cleanup)
- Email service beyond invitation removal (used only for invitations in this codebase)
- Cascading deletion logic (already configured)

## Architecture / Approach

**Phased deletion, backend-first:**

1. **Phase 1 (Delete entities & repos)** → triggers compile errors
2. **Phase 2 (Refactor services & controllers)** → fix compile errors; refactor queries to createdBy model
3. **Phase 3 (Delete frontend pages & routes)** → clean up UI and API clients
4. **Phase 4 (Test & verify)** → delete multi-user tests, add single-user tests, smoke test flows

This order ensures backend consistency before UI changes, and compile errors act as a guide for what needs refactoring.

## Phases at a Glance

| Phase | What it delivers | Key risk |
| --- | --- | --- |
| 1. Delete Entities | Compile errors surface all usages | None (expected); drives Phase 2 work |
| 2. Refactor Services | Backend refactored to single-user semantics (createdBy queries, no membership) | If household access control is missed, users might see other users' data (mitigated by access denial tests in Phase 4) |
| 3. Remove Frontend | Pages, routes, API functions deleted; RegisterPage, DashboardPage refactored | If invitation logic not fully removed from RegisterPage, users might encounter 404s on shared links (low risk; UI logic is isolated) |
| 4. Test & Verify | Multi-user tests deleted; single-user happy path verified end-to-end | None (verification gates shipping) |

**Prerequisites:** None. This is independent work that unblocks S-06 and S-07.

**Estimated effort:** ~2 sessions (4–6 hours). Backend refactoring is straightforward (3–4 methods to rewrite); frontend deletion is mechanical.

## Open Risks & Assumptions

- **Assumption**: No production data exists (greenfield MVP). Verified: `ddl-auto=create-drop` is configured; Hibernate will drop old tables and create new schema on startup.
- **Assumption**: No other code outside HouseholdService, HouseholdController, and frontend pages depends on HouseholdMember/HouseholdInvitation. Verified: grep found only expected usages; Phase 1 compile errors will confirm.
- **Risk (low)**: Household access control refactoring could miss a case, allowing users to access other users' households. **Mitigation**: Phase 4 includes explicit "user cannot access another user's household" test.

## Success Criteria (Summary)

- ✅ All compile errors resolved (TypeScript, Java)
- ✅ All tests pass (unit, integration, single-user happy path)
- ✅ End-to-end flow works: register → household → task → dashboard (no regressions in task CRUD, auth, or persistence)
- ✅ Single-user data isolation verified: user1 cannot see user2's households (401 on access attempt)
- ✅ Multi-user pages and routes no longer exist (404 on old invitation URLs)