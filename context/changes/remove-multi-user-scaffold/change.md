---
title: Remove Multi-User Scaffold
id: remove-multi-user-scaffold
status: impl_reviewed
created: 2026-08-29
updated: 2026-08-29
roadmap_item: F-02
---

# Remove Multi-User Scaffold

A foundation-level cleanup change that removes the multi-user invitation and membership infrastructure from the single-user MVP. This unblocks F-02 completion and keeps the codebase lean for shipping.

## Change Metadata

- **Change ID**: remove-multi-user-scaffold
- **Status**: planned
- **Type**: cleanup/refactor
- **Blocks**: S-06 (today-dashboard), S-07 (ai-task-generation)
- **Blocked by**: none
- **Roadmap**: F-02 cleanup
- **Phase**: module2

## Summary

The multi-user invitation flow (HouseholdMembers, HouseholdInvitations, InvitePartnerPage, InvitationAcceptPage) was scaffolded during the multi-user design phase but is explicitly out of scope for the single-user MVP (per PRD §Non-Goals). Removing this dead code reduces schema complexity, eliminates test surface area, and establishes a clean foundation before shipping S-06 and S-07.

**Scope**: Backend entities, repositories, services, controllers; frontend pages, routes, and API functions; test cleanup.

**Effort**: ~2 sessions (phased backend-first, then frontend, driven by compile errors).

**Dependencies**: None. This is independent work that unblocks downstream work.

See `plan.md` for full implementation details.