---
status: implementing
created: 2026-08-28
updated: 2026-08-29
---

# Household Schema — One Household Per User Constraint

**ID:** household-schema  
**Roadmap item:** F-02 (Foundation)  
**PRD refs:** FR-003, FR-004, NFR (data isolation)  

## Description

Enforce "one household per user" constraint to fix a silent bug where users can create unlimited households but the system assumes one per user. Add service-level validation in HouseholdService, unit tests, and frontend UI to prevent the constraint violation. This unblocks reliable task creation since TaskController can depend on exactly one household per user.

## Timeline

Critical for MVP stability. Implementation is straightforward (3 phases: backend validation, tests, frontend UI).

## Related Work

The original schema plan (Phase 1-3) designed a full HouseholdMember join table for 2-member households. This update adds targeted constraint enforcement at the service level to prevent the bug immediately, without requiring the full HouseholdMember refactor.
