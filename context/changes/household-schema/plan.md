# Household Schema & Data Model Implementation Plan

## Overview

Establish PostgreSQL schema with households, users, household_members, and tasks tables. Enforce user-household isolation and multi-member relationships (2-member partner model). This foundation unblocks all downstream task management slices (S-02: task CRUD, S-03: assignment, S-06: dashboard, S-07: AI).

## Current State Analysis

**Existing:**
- Spring Boot 4.1.1 with Spring Data JPA + Hibernate
- PostgreSQL with HikariCP (5 connection pool size)
- User entity: `id` (UUID), `email` (unique), `passwordHash`, `createdAt`, `updatedAt`
- UserSession entity: tracks device, IP, JWT state
- Schema auto-generation via Hibernate `create-drop` mode
- No migration framework (Flyway/Liquibase)

**Missing:**
- Household entity and table
- HouseholdMember join table (user-household relationship)
- Task entity with category enum, assignment, completion tracking
- HouseholdInvitation entity for email-based partner invites
- Soft-delete support (deletedAt column for tasks)
- Task query methods with household isolation and soft-delete filtering
- Repositories and indexes

## Desired End State

After this plan is complete:
- PostgreSQL schema has `households`, `household_members`, `tasks`, `household_invitations` tables
- Users can be members of exactly one household (via HouseholdMember join with 2-member limit)
- Tasks are always bound to a household (isolation enforced)
- Soft-deleted tasks are excluded from queries by default
- Repositories provide query methods for household-scoped operations
- Unit tests verify entity relationships, constraints, and isolation

**Verification:** Run `bun run build` and `bun test` — all tests pass; no DDL errors on app startup.

### Key Discoveries

- **Telegram bot ownership:** User chose system-wide bot model (one bot for all households) — schema should not include per-household or per-user bot token storage. Bot configuration handled in F-03.
- **Soft delete for tasks:** `deletedAt: LocalDateTime (nullable)` column added; all task queries exclude soft-deleted items by default.
- **Household creation:** Users auto-join household they create (not a separate acceptance step).
- **Categories:** Fixed enum only (CLEANING, SHOPPING, LAUNDRY, MAINTENANCE, BILLS) — no custom categories in MVP.
- **Household member count:** 2 members only (partner model) — enforced at schema level (unique constraint on household_id).

## What We're NOT Doing

- Soft delete for households or users (hard delete only; users can't delete account in MVP)
- Edit history tracking (no `lastEditedBy` / `lastEditedAt` in MVP; can add in future)
- Recurring task support (FR-009 deferred to v1.1)
- Custom task categories per household (fixed enum only)
- Household roles beyond CREATOR/PARTNER (flat model per PRD)
- Bot token storage in schema (system-wide bot config in F-03)
- More than 2 household members (partner model only)

## Implementation Approach

**Entity-first design:** Define JPA entities with annotations; Hibernate auto-generates schema on startup via `create-drop` mode. No manual DDL or migrations in this phase (MVP trade-off for speed; Flyway migration layer can be added later).

**Isolation pattern:** Every Task has `householdId` foreign key. Queries filter by `householdId` to prevent cross-household data leaks. HouseholdMember acts as the authorization matrix (is User X a member of Household Y?).

**Soft delete:** Add `deletedAt: LocalDateTime` column to Task; create repository method `findAllByHouseholdIdAndDeletedAtIsNull()` as the default query. Hard-delete methods exist for testing cleanup only.

## Critical Implementation Details

**Task assignment invariant:** Every task has an `assigneeId` (FK to User). Assignment is never null or to "the household"; it's always to a specific person. This explicitness enforces the PRD rule: "tasks are never assigned to the household — they're always assigned to a specific person."

**HouseholdMember uniqueness:** Unique constraint on `(householdId, userId)` prevents duplicate membership. Combined with a check constraint `(householdId IN SELECT COUNT(userId) FROM household_members GROUP BY householdId HAVING COUNT(*) <= 2)` — enforces 2-member limit, though easier to enforce at service layer initially.

## Phase 1: Core Entity Definitions

### Overview

Define and annotate all entities (Household, HouseholdMember, Task, HouseholdInvitation) with JPA annotations. Add category enum. Relationships are configured; Hibernate will generate schema on app startup.

### Changes Required

#### 1. Household Entity

**File:** `src/main/java/com/example/doneyet/domain/Household.java`

**Intent:** Represent a household unit. Store name, creation metadata, and track who created it (for audit). The household is the organizational boundary; all tasks and members belong to exactly one household.

**Contract:** JPA Entity mapped to `households` table with columns: `id` (UUID PK), `name` (not null, String), `createdBy` (FK to User, not null), `createdAt`, `updatedAt`. Relationships: OneToMany with HouseholdMember and Task.

#### 2. HouseholdMember Entity

**File:** `src/main/java/com/example/doneyet/domain/HouseholdMember.java`

**Intent:** Join table for user-household membership. Tracks which users are members of which households, with role information (CREATOR vs PARTNER). Enforces the 2-member partner model.

**Contract:** JPA Entity mapped to `household_members` table. Columns: `id` (UUID PK), `householdId` (FK, not null), `userId` (FK to User, not null), `role` (enum: CREATOR, PARTNER), `joinedAt`. Composite unique constraint: `UNIQUE(householdId, userId)`. ManyToOne to Household and User.

#### 3. Task Entity

**File:** `src/main/java/com/example/doneyet/domain/Task.java`

**Intent:** Represent a household task (chore, errand, bill payment, etc.). Store title, optional description, category, due date, and tracking for assignment and completion. Soft-delete support via `deletedAt`.

**Contract:** JPA Entity mapped to `tasks` table. Columns: `id` (UUID PK), `householdId` (FK, not null — for isolation), `title` (String, not null), `description` (String, nullable), `category` (enum: CLEANING, SHOPPING, LAUNDRY, MAINTENANCE, BILLS), `assigneeId` (FK to User, not null — who owns this task), `dueDate` (LocalDate, nullable), `reminderTime` (LocalTime, nullable), `completed` (boolean, default false), `completedBy` (FK to User, nullable), `completedAt` (LocalDateTime, nullable), `deletedAt` (LocalDateTime, nullable), `createdBy` (FK to User, not null), `createdAt`, `updatedAt`. ManyToOne relationships to Household and User (for assignee, completedBy, createdBy).

Index on `(householdId, deletedAt)` for soft-delete filtering performance.

#### 4. TaskCategory Enum

**File:** `src/main/java/com/example/doneyet/domain/TaskCategory.java`

**Intent:** Define the fixed set of allowed task categories per PRD.

**Contract:** Java enum with five values: `CLEANING`, `SHOPPING`, `LAUNDRY`, `MAINTENANCE`, `BILLS`. Maps to database as VARCHAR via Hibernate `@Enumerated(EnumType.STRING)`.

#### 5. HouseholdMemberRole Enum

**File:** `src/main/java/com/example/doneyet/domain/HouseholdMemberRole.java`

**Intent:** Track role in household (who created it vs who joined). Enables future permission logic; flat in MVP.

**Contract:** Java enum with two values: `CREATOR`, `PARTNER`. Maps to database as VARCHAR.

#### 6. HouseholdInvitation Entity

**File:** `src/main/java/com/example/doneyet/domain/HouseholdInvitation.java`

**Intent:** Represent an email-based invitation to join a household. Partner receives email with a token link; accepting the link creates a HouseholdMember record and marks the invitation accepted.

**Contract:** JPA Entity mapped to `household_invitations` table. Columns: `id` (UUID PK), `householdId` (FK, not null), `invitedEmail` (String, not null), `invitationToken` (String, unique, not null), `expiresAt` (LocalDateTime, not null — 24 hours from creation), `accepted` (boolean, default false), `acceptedAt` (LocalDateTime, nullable), `acceptedByUserId` (FK to User, nullable), `createdAt`. ManyToOne to Household. Unique constraint on `invitationToken`.

### Success Criteria

#### Automated Verification

- Type checking passes: `bun run typecheck`
- Linting passes: `bun run lint`
- App starts without errors: entities are scanned, Hibernate generates schema
- Unit tests verify entity relationships (ManyToOne, OneToMany cardinality) pass
- Entity constraint tests pass (unique constraint on HouseholdMember, Task soft-delete)
- Repositories can be instantiated (Spring auto-wires them)

#### Manual Verification

- PostgreSQL schema contains all tables: `households`, `household_members`, `tasks`, `household_invitations`
- Column names and types match entity definitions (e.g., `categoryId` is VARCHAR for enum)
- Foreign key relationships are in place
- Unique constraints exist on intended columns
- Indexes exist on performance-critical columns (householdId, task.deletedAt)

**Implementation Note:** After completing this phase and all automated verification passes, pause here for manual confirmation that the PostgreSQL schema is correct before proceeding to Phase 2.

---

## Phase 2: Data Access Layer & Query Methods

### Overview

Create Spring Data JPA repositories with custom query methods for household-scoped operations, soft-delete filtering, and task lifecycle queries. Add repository tests.

### Changes Required

#### 1. HouseholdRepository

**File:** `src/main/java/com/example/doneyet/repository/HouseholdRepository.java`

**Intent:** Access Household entities by ID or by membership. Support queries like "find all households a user is a member of" for login/dashboard flows.

**Contract:** Extends `JpaRepository<Household, UUID>`. Custom methods:
- `findById(UUID id): Optional<Household>` (inherited)
- `findByCreatedBy(UUID userId): List<Household>` — households created by this user
- `@Query("SELECT DISTINCT h FROM Household h JOIN HouseholdMember hm ON h.id = hm.householdId WHERE hm.userId = ?1") findHouseholdsByMemberId(UUID userId): List<Household>` — all households this user is a member of

#### 2. HouseholdMemberRepository

**File:** `src/main/java/com/example/doneyet/repository/HouseholdMemberRepository.java`

**Intent:** Query membership records. Answer: "Is User X a member of Household Y?" and "How many members does Household H have?"

**Contract:** Extends `JpaRepository<HouseholdMember, UUID>`. Custom methods:
- `findByHouseholdIdAndUserId(UUID householdId, UUID userId): Optional<HouseholdMember>` — check membership
- `findByHouseholdId(UUID householdId): List<HouseholdMember>` — all members of a household
- `countByHouseholdId(UUID householdId): long` — member count (for 2-member validation)

#### 3. TaskRepository

**File:** `src/main/java/com/example/doneyet/repository/TaskRepository.java`

**Intent:** Query tasks with household isolation and soft-delete filtering. Default queries exclude deleted tasks; explicit methods exist for testing and cleanup.

**Contract:** Extends `JpaRepository<Task, UUID>`. Custom methods:
- `findByHouseholdIdAndDeletedAtIsNull(UUID householdId): List<Task>` — all active tasks in a household
- `findByHouseholdIdAndAssigneeIdAndDeletedAtIsNull(UUID householdId, UUID assigneeId): List<Task>` — tasks assigned to a user
- `findByHouseholdIdAndDueDateAndDeletedAtIsNull(UUID householdId, LocalDate dueDate): List<Task>` — tasks due on a specific date
- `findByHouseholdIdAndCompletedAndDeletedAtIsNull(UUID householdId, boolean completed): List<Task>` — filter by completion status
- `findByIdAndHouseholdId(UUID id, UUID householdId): Optional<Task>` — isolation check (returns Task only if it belongs to this household)
- `save(Task task): Task` — persists task; ensure all queries exclude soft-deleted by default via `@Query` annotation or service-layer filtering

#### 4. HouseholdInvitationRepository

**File:** `src/main/java/com/example/doneyet/repository/HouseholdInvitationRepository.java`

**Intent:** Query invitations by token, email, or household. Support acceptance flows and expiry checks.

**Contract:** Extends `JpaRepository<HouseholdInvitation, UUID>`. Custom methods:
- `findByInvitationToken(String token): Optional<HouseholdInvitation>` — look up by token from email link
- `findByHouseholdIdAndInvitedEmail(UUID householdId, String email): Optional<HouseholdInvitation>` — check if email already invited
- `findByHouseholdIdAndAcceptedFalse(UUID householdId): List<HouseholdInvitation>` — pending invitations for a household
- `findByExpiresAtBeforeAndAcceptedFalse(LocalDateTime now): List<HouseholdInvitation>` — expired invitations (for cleanup job)

### Success Criteria

#### Automated Verification

- Type checking passes: `bun run typecheck`
- Linting passes: `bun run lint`
- Unit tests for repositories pass: each repository method is tested with sample data
- Isolation test passes: query a task from Household A and verify it's not accessible via Household B's queries
- Soft-delete test passes: create a task, soft-delete it, verify it's excluded from default queries
- Spring wires all repositories at startup (no instantiation errors)

#### Manual Verification

- Queries return expected results against PostgreSQL
- Soft-deleted tasks are not returned by default query methods
- Household isolation is enforced (cross-household queries return empty)
- Transaction handling is correct (no uncommitted reads)

**Implementation Note:** After completing this phase and all automated verification passes, pause here for manual confirmation that repository queries work correctly with sample data before proceeding to Phase 3.

---

## Phase 3: Schema Verification & Testing

### Overview

Write comprehensive tests to verify entity relationships, soft-delete behavior, household isolation, and constraint enforcement. Run full test suite.

### Changes Required

#### 1. Entity Relationship Tests

**File:** `src/test/java/com/example/doneyet/domain/EntityRelationshipTest.java`

**Intent:** Verify JPA annotations generate correct schema and relationships work as expected.

**Contract:** Test class with `@DataJpaTest` annotation. Tests:
- Household can have multiple Tasks (OneToMany relationship works)
- Household can have multiple HouseholdMembers (OneToMany)
- HouseholdMember links correct User and Household (ManyToOne)
- Task.assigneeId references a User
- Task.householdId references a Household
- Cascade delete rules (e.g., deleting Household cascades to Tasks)

#### 2. Soft Delete Behavior Tests

**File:** `src/test/java/com/example/doneyet/repository/TaskSoftDeleteTest.java`

**Intent:** Verify soft-deleted tasks are excluded from default queries and included only when explicitly requested.

**Contract:** Test class that:
- Creates a task, saves it, queries it — present in default query
- Soft-deletes the task (set `deletedAt` to now, save)
- Queries default list — deleted task not included
- Queries with explicit `findByHouseholdIdAndDeletedAtIsNotNull()` — deleted task is included
- Cascade behavior: deleting a Task does not delete the User or Household

#### 3. Household Isolation Tests

**File:** `src/test/java/com/example/doneyet/repository/HouseholdIsolationTest.java`

**Intent:** Verify tasks from one household cannot leak to queries for another household.

**Contract:** Test class that:
- Creates Household A and B with different users
- Creates Task T1 in Household A (assigned to user in A)
- Creates Task T2 in Household B (assigned to user in B)
- Queries Household A's tasks — only T1 is returned
- Queries Household B's tasks — only T2 is returned
- Cross-household query attempt returns empty (no "T1 in B" or "T2 in A")

#### 4. Constraint Enforcement Tests

**File:** `src/test/java/com/example/doneyet/domain/ConstraintTest.java`

**Intent:** Verify database constraints are enforced (unique, not-null, foreign keys).

**Contract:** Test class that:
- Duplicate email in User throws (inherited from F-01, included for completeness)
- Null household_id on Task throws
- Null assignee_id on Task throws
- Duplicate HouseholdMember (same household + user) throws or is prevented
- Foreign key violation on invalid householdId throws

#### 5. HouseholdInvitation Lifecycle Tests

**File:** `src/test/java/com/example/doneyet/domain/HouseholdInvitationTest.java`

**Intent:** Verify invitation creation, expiry, and acceptance flow.

**Contract:** Test class that:
- New invitation has `accepted = false`, `acceptedAt = null`
- Invitation token is unique (can't create two invitations with same token)
- Invitation.expiresAt is 24 hours from creation
- Expired invitations are correctly identified by `expiresAt < now`
- Accepting an invitation sets `accepted = true`, `acceptedAt = now`, `acceptedByUserId`

### Success Criteria

#### Automated Verification

- Unit tests pass: `bun test` (all tests in src/test/java)
- Coverage for repositories and entities >80%
- No SQL errors on test startup (H2 in-memory database auto-schema works)
- Soft-delete tests pass
- Isolation tests pass
- Constraint tests pass (or document expected exceptions)

#### Manual Verification

- Test output shows all entity relationships correctly established
- Schema dump (via PostgreSQL console) matches expected structure
- Indexes are present on performance-critical columns
- No warnings in Hibernate DDL generation logs

**Implementation Note:** After completing this phase and all automated verification passes, pause here for manual confirmation that all tests pass and schema is correct before declaring the plan complete.

---

## Testing Strategy

### Unit Tests

**Entity tests** (`src/test/java/com/example/doneyet/domain/`):
- Entity creation, getters/setters, equality
- Relationship navigation (Household → Tasks, etc.)
- Enum values and mapping

**Repository tests** (`src/test/java/com/example/doneyet/repository/`):
- CRUD operations (create, read, update, delete)
- Custom query methods return correct data
- Soft-delete filtering works
- Household isolation is enforced
- Constraint violations throw expected exceptions

**Constraint & lifecycle tests**:
- Unique constraints work
- Not-null constraints work
- Foreign key relationships work
- Cascade delete behavior
- Invitation expiry logic

### Integration Tests

- Test full flow: Create Household → Add HouseholdMember → Create Task → Query by Household
- Verify soft-delete doesn't break task assignment queries
- Verify permission checks (user membership) block cross-household access

### Manual Testing Steps

1. Start app: `bun run dev --backend` (Spring Boot starts, Hibernate generates schema)
2. Open PostgreSQL console: `psql -U postgres -d done_yet`
3. Verify tables: `\dt` — see households, household_members, tasks, household_invitations
4. Verify columns: `\d households` — see id, name, created_by, created_at, updated_at
5. Verify indexes: `SELECT * FROM pg_indexes WHERE tablename = 'tasks'` — see index on (household_id, deleted_at)
6. Manual insertion: insert a household, members, tasks; verify queries return correct isolation

## Performance Considerations

- Index on `(householdId, deletedAt)` for fast soft-delete filtering
- Index on `(householdId, assigneeId)` for task-assignment queries (added if S-02 queries are slow)
- Connection pool size 5 (HikariCP) — sufficient for MVP; increase if connection timeouts occur
- No N+1 query issues (use `@ManyToOne(fetch = FetchType.EAGER)` carefully; prefer LAZY + explicit fetch if needed)

## Migration Notes

**Schema evolution:** Hibernate `create-drop` mode is for MVP speed only. Before production or scaling to multiple environments:
1. Add Flyway migration framework (dependency + config in pom.xml, application.properties)
2. Convert entities to migrations: `V1__initial_schema.sql` with CREATE TABLE statements
3. Switch `spring.jpa.hibernate.ddl-auto` to `validate` (Hibernate validates but doesn't modify schema)

**Data backfill:** No existing household or task data; schema is greenfield. When migrating from other systems, add data migration scripts in a `V2__backfill_*.sql` phase.

## References

- Spring Data JPA docs: https://spring.io/projects/spring-data-jpa
- Hibernate annotations: https://hibernate.org/orm/documentation/
- PostgreSQL JDBC driver: https://jdbc.postgresql.org/
- PRD: `context/foundation/prd.md` (FR-003, FR-004, NFR data isolation)
- Roadmap: `context/foundation/roadmap.md` (F-02 dependencies and unknowns)

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Core Entity Definitions

#### Automated

- [x] 1.1 Type checking passes
- [x] 1.2 Linting passes
- [x] 1.3 App starts without errors
- [x] 1.4 Entity relationship tests pass

#### Manual

- [x] 1.5 PostgreSQL schema verified (tables exist with correct columns)
- [x] 1.6 Unique constraints exist
- [x] 1.7 Foreign key relationships verified

### Phase 2: Data Access Layer & Query Methods

#### Automated

- [ ] 2.1 Type checking passes
- [ ] 2.2 Linting passes
- [ ] 2.3 Repository tests pass
- [ ] 2.4 Soft-delete filtering tests pass

#### Manual

- [ ] 2.5 Repository queries return expected results
- [ ] 2.6 Household isolation verified
- [ ] 2.7 Cross-household queries blocked

### Phase 3: Schema Verification & Testing

#### Automated

- [ ] 3.1 Full test suite passes
- [ ] 3.2 Coverage >80%
- [ ] 3.3 Constraint tests pass

#### Manual

- [ ] 3.4 Schema dump verified against spec
- [ ] 3.5 Indexes present on performance columns
- [ ] 3.6 No Hibernate DDL generation warnings
