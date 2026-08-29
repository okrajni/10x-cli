# Household Schema & Data Model — Plan Brief

> Full plan: `context/changes/household-schema/plan.md`  
> Roadmap: `context/foundation/roadmap.md` (F-02)  
> PRD: `context/foundation/prd.md` (FR-003, FR-004, NFR)

## What & Why

Build the PostgreSQL schema foundation that enables two-person households to manage shared tasks with clear ownership and visibility. The schema enforces user-household isolation so that tasks from one household never leak to another. This is the data layer that unblocks all downstream features: task CRUD (S-02), task assignment (S-03), dashboard (S-06), and AI task generation (S-07).

## Starting Point

Spring Boot 4.1.1 with Spring Data JPA + Hibernate is configured. User and UserSession entities exist. Hibernate auto-generates schema on startup via `create-drop` mode. No migration framework yet (Flyway/Liquibase deferred to post-MVP).

## Desired End State

PostgreSQL has six tables:
- `households`: household records with creation metadata
- `household_members`: user-household join (2-member partner model)
- `tasks`: all household tasks with assignment, completion tracking, soft-delete support
- `household_invitations`: email-based partner invitations with 24-hour token expiry
- Supporting enums: TaskCategory (CLEANING, SHOPPING, LAUNDRY, MAINTENANCE, BILLS), HouseholdMemberRole (CREATOR, PARTNER)

Every task is bound to a household (isolation enforced at query layer). All repository methods exclude soft-deleted tasks by default. Unit tests verify isolation, soft-delete behavior, and constraint enforcement.

## Key Decisions Made

| Decision | Choice | Why | Source |
|----------|--------|-----|--------|
| Task deletion strategy | Soft delete (`deletedAt` column) | Preserves audit history; queries exclude soft-deleted by default; small schema overhead | Plan |
| Telegram bot ownership | System-wide bot (one for all households) | Simpler deployment and user linking; schema doesn't store bot token | Plan |
| Invitation expiry | 24 hours from creation | Encourages timely acceptance; balances security (token exposure window) vs convenience | Plan |
| Edit tracking in MVP | None (omit `lastEditedBy`/`lastEditedAt`) | Saves schema complexity; can be added post-MVP if needed | Plan |
| Household creation flow | Forced household creation (auto-join creator) | Single-threaded onramp; creator becomes instant member, simplifies API | Plan |
| Task categories | Fixed enum (no custom per-household) | Predictable dashboard grouping; AI suggestions align to known categories; prevents sprawl | Plan |
| Household member limit | 2 members only (partner model) | Aligns with PRD; simpler assignment UX ("me" or "partner"); enforced at schema + service | Plan |
| Task assignment ownership | Always assigned to a specific user | Enforces PRD rule: "tasks are never assigned to the household"; prevents tragedy-of-the-commons | Plan |

## Scope

**In scope:**
- Household entity (name, createdBy, timestamps)
- HouseholdMember join table with role tracking (CREATOR, PARTNER)
- Task entity with category enum, assignment, completion, soft-delete
- HouseholdInvitation entity for email-based invites (24-hour expiry)
- Spring Data JPA repositories with household-isolation queries
- Soft-delete filtering (deletedAt IS NULL on default queries)
- Unit tests for entity relationships and isolation
- Indexes on performance-critical columns (householdId, task.deletedAt)

**Out of scope:**
- Flyway/Liquibase migration framework (deferred to post-MVP)
- Edit history tracking (omitted in MVP)
- Recurring tasks (FR-009 deferred to v1.1)
- Custom task categories per household (fixed enum only)
- Bot token storage in schema (handled in F-03)
- Household deletion flow (users can't delete household in MVP)
- Advanced permission roles (flat model per PRD)

## Architecture / Approach

**Entity-first design:** Define JPA entities with `@Entity`, `@Column`, `@ManyToOne`, `@OneToMany` annotations. Hibernate auto-generates schema on startup via `create-drop` mode — no manual SQL or migration files in this phase.

**Isolation pattern:** Every Task has `householdId` FK. Repository queries filter by householdId to prevent cross-household leaks. HouseholdMember join validates user membership before granting access.

**Soft delete:** Add `deletedAt: LocalDateTime (nullable)` to Task. Repository methods use `findByHouseholdIdAndDeletedAtIsNull()` as default; explicit `findByHouseholdIdAndDeletedAtIsNotNull()` for testing/recovery.

**Task assignment invariant:** `assigneeId` (FK to User) is never null. Every task belongs to a specific person, not "the household." This explicitness enforces shared responsibility.

## Phases at a Glance

| Phase | What it delivers | Key risk |
|-------|------------------|----------|
| 1. Entity Definitions | Household, HouseholdMember, Task, HouseholdInvitation JPA entities with enums; Hibernate generates schema | Schema mismatch with expected structure; missing indexes |
| 2. Data Access Layer | Repositories with household-isolation queries and soft-delete filtering | Soft-delete filtering accidentally removed from default queries; cross-household data leaks |
| 3. Verification & Testing | Unit tests for entity relationships, soft-delete, isolation, constraints | Tests pass but production queries fail due to schema generation issues |

**Prerequisites:** F-01 (auth-scaffold) must be complete so that User entity exists for FK relationships.

**Estimated effort:** ~2–3 sessions (1 per phase) — entity definitions are straightforward; repository queries and tests are the bulk of the work.

## Open Risks & Assumptions

- **Assumption: Hibernate `create-drop` mode is acceptable for MVP.** In production with multiple environments (dev, staging, prod), we'll need Flyway migration framework. Schema changes post-launch must be backwards-compatible.
- **Risk: Soft-delete filtering is accidentally bypassed.** If a service method queries Task without the `DeletedAtIsNull` filter, deleted tasks leak. Mitigation: strict repository-only access, code review on all Task queries.
- **Risk: Household member limit enforcement.** Schema-level check constraint `COUNT(*) <= 2` is complex in SQL. Service layer validation is easier but requires discipline (no bypassing via direct SQL). Mitigation: repository method `countByHouseholdId()` with service-layer validation.
- **Assumption: System-wide bot model (chosen by user) doesn't require schema changes.** F-03 (Telegram bot scaffold) will handle bot token provisioning separately.

## Success Criteria (Summary)

1. **Data Isolation:** Queries for Household A return only Household A's tasks; Household B cannot access them.
2. **Soft Delete:** Deleted tasks are excluded from default queries; existing code doesn't break when soft-delete is added.
3. **Schema Correctness:** PostgreSQL schema matches entity definitions; all indexes and constraints present.
4. **Test Coverage:** Entity relationships, soft-delete behavior, household isolation, and constraint enforcement are tested and passing.
