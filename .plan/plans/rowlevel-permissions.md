# Row-Level Permissions: Java-First Redesign

## Context

The current `feat/rowlevel_permissions` branch implements row-level security via 729 lines of PL/pgSQL triggers, materialized views, and metadata tables. Five independent reviewers unanimously found critical bugs (wrong PKs, missing policy checks, MV staleness = security hole) and rejected the trigger-based approach as overengineered and untestable.

**Decision**: Pivot to Java-first approach. New branch from master. PG catalog is the sole source of truth — no custom metadata tables. Java orchestrates everything.

## Design Principles

1. **PG catalog = sole source of truth** — roles, grants, membership, row-level marker, description all in PG
2. **No custom metadata tables** — eliminated `group_metadata`; `is_row_level` via marker role, description via `COMMENT ON ROLE`, schema from role name
3. **Java = orchestrator** — creates roles, executes GRANT/REVOKE, creates RLS policies
4. **No triggers, no materialized views** — eliminates staleness and debugging complexity
5. **Backwards compatible** — existing 8 default roles (EXISTS→OWNER) unchanged
6. **No double administration** — mg_group column presence = RLS enabled (no separate flag)
7. **System roles protected** — SqlRoleManager refuses to modify/delete system-created roles

## Architecture Overview

```
                    ┌─────────────────────┐
                    │   GraphQL API        │
                    │   (queries/mutations)│
                    └──────────┬──────────┘
                               │
                    ┌──────────▼──────────┐
                    │   SqlRoleManager     │
                    │   (Java orchestrator)│
                    │   (instance-scoped)  │
                    └──────────┬──────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
    ┌─────────▼─────┐ ┌───────▼───────┐ ┌──────▼──────┐
    │ PG Roles      │ │ PG Grants     │ │ PG RLS      │
    │ + MG_ROWLEVEL │ │ (permissions) │ │ (row filter) │
    │ + COMMENT ON  │ │               │ │              │
    │   ROLE        │ │               │ │              │
    └───────────────┘ └───────────────┘ └─────────────┘
```

Everything lives in PG catalog. Zero custom tables for permissions.

## How is_row_level Works (Marker Role)

A single PG marker role `MG_ROWLEVEL` (no login, no privileges) tags row-level roles:

```sql
CREATE ROLE "MG_ROWLEVEL" WITH NOLOGIN;
GRANT "MG_ROWLEVEL" TO "MG_ROLE_myschema/GroupViewer";

SELECT pg_has_role('MG_ROLE_myschema/GroupViewer', 'MG_ROWLEVEL', 'member');  -- true
SELECT pg_has_role('MG_ROLE_myschema/Viewer', 'MG_ROWLEVEL', 'member');       -- false
```

## Where Everything Lives (PG Catalog Only)

| Info | PG Storage | Query |
|------|-----------|-------|
| Role exists | `pg_roles` | `SELECT 1 FROM pg_roles WHERE rolname = ?` |
| is_row_level | `GRANT MG_ROWLEVEL TO role` | `pg_has_role(role, 'MG_ROWLEVEL', 'member')` |
| Description | `COMMENT ON ROLE` | `shobj_description(oid, 'pg_authid')` |
| Schema | Encoded in role name | Parse `MG_ROLE_<schema>/<name>` |
| Membership | `GRANT role TO user` | `pg_has_role(user, role, 'member')` |
| Table perms | `GRANT SELECT/INSERT/... ON table TO role` | `has_table_privilege(role, table, priv)` |

## mg_group Column (RLS Enablement)

Adding mg_group to a table = enabling RLS. No separate flag needed.

```sql
ALTER TABLE <schema>.<table>
  ADD COLUMN mg_group TEXT[] DEFAULT NULL;
CREATE INDEX idx_<table>_mg_group ON <schema>.<table> USING GIN(mg_group);
ALTER TABLE <schema>.<table> ENABLE ROW LEVEL SECURITY;
```

**Key decisions** (from review):
- `DEFAULT NULL` not `'{}'` — NULL means "no group assigned", handled explicitly in policies
- `ENABLE` not `FORCE` — table owner (molgenis superuser) bypasses RLS for DDL operations
- Removing mg_group = disabling RLS (policies dropped, column optionally preserved)

## Default Roles Per Schema

When a schema is created, Java creates these roles (extending existing pattern):

| Role | PG Role | Row-Level | PG Grants |
|------|---------|-----------|-----------|
| Exists | MG_ROLE_schema/Exists | no | USAGE on schema |
| Range | MG_ROLE_schema/Range | no | inherits Exists |
| Aggregator | MG_ROLE_schema/Aggregator | no | inherits Range |
| Count | MG_ROLE_schema/Count | no | inherits Aggregator |
| Viewer | MG_ROLE_schema/Viewer | no | SELECT on all tables |
| Editor | MG_ROLE_schema/Editor | no | INSERT/UPDATE/DELETE on all tables |
| Manager | MG_ROLE_schema/Manager | no | ALL + WITH ADMIN OPTION |
| Owner | MG_ROLE_schema/Owner | no | ALL + ADMIN |
| GroupViewer | MG_ROLE_schema/GroupViewer | **yes** | SELECT on data tables, SELECT on ontology tables |
| GroupEditor | MG_ROLE_schema/GroupEditor | **yes** | INSERT/UPDATE/DELETE on data tables |

**Inheritance chain** (critical — prevents pg_has_role transitivity bug):
- GroupViewer inherits from Count (NOT from Viewer)
- GroupEditor inherits from Count (NOT from GroupViewer)
- This ensures `pg_has_role(GroupEditor, 'MG_ROWLEVEL', 'member')` = true only via direct grant

Row-level roles get: `GRANT MG_ROWLEVEL TO role`.
GroupViewer/GroupEditor are NOT in Privileges enum — represented as String constants in SqlRoleManager.
All system roles (8 enum + GroupViewer/GroupEditor) protected from modification/deletion.

Global roles use schema prefix `mg_global`:
- `MG_ROLE_mg_global/DataStewards` — cross-schema, accessed via `db.getSchema("mg_global")`

## RLS Policy Design

When mg_group column is added to a table, Java creates two policies (PG OR's them):

```sql
-- Policy 1: Schema-level users see all rows (USING + WITH CHECK)
CREATE POLICY schema_access ON myschema.mytable FOR ALL
USING (
  EXISTS (
    SELECT 1 FROM pg_roles r
    WHERE (r.rolname LIKE 'MG_ROLE_myschema/%'
        OR r.rolname LIKE 'MG_ROLE_mg_global/%')
    AND pg_has_role(current_user, r.rolname, 'member')
    AND NOT pg_has_role(r.rolname, 'MG_ROWLEVEL', 'member')
    AND has_table_privilege(r.rolname, '"myschema"."mytable"', 'SELECT')
  )
)
WITH CHECK (
  EXISTS (
    SELECT 1 FROM pg_roles r
    WHERE (r.rolname LIKE 'MG_ROLE_myschema/%'
        OR r.rolname LIKE 'MG_ROLE_mg_global/%')
    AND pg_has_role(current_user, r.rolname, 'member')
    AND NOT pg_has_role(r.rolname, 'MG_ROWLEVEL', 'member')
    AND has_table_privilege(r.rolname, '"myschema"."mytable"', 'INSERT')
  )
);

-- Policy 2: Row-level users see only their group's rows (USING + WITH CHECK)
CREATE POLICY row_level_access ON myschema.mytable FOR ALL
USING (
  mg_group IS NULL
  OR EXISTS (
    SELECT 1 FROM pg_roles r
    WHERE (r.rolname LIKE 'MG_ROLE_myschema/%'
        OR r.rolname LIKE 'MG_ROLE_mg_global/%')
    AND pg_has_role(current_user, r.rolname, 'member')
    AND pg_has_role(r.rolname, 'MG_ROWLEVEL', 'member')
    AND r.rolname = ANY(mg_group)
  )
)
WITH CHECK (
  mg_group IS NOT NULL
  AND EXISTS (
    SELECT 1 FROM pg_roles r
    WHERE (r.rolname LIKE 'MG_ROLE_myschema/%'
        OR r.rolname LIKE 'MG_ROLE_mg_global/%')
    AND pg_has_role(current_user, r.rolname, 'member')
    AND pg_has_role(r.rolname, 'MG_ROWLEVEL', 'member')
    AND r.rolname = ANY(mg_group)
  )
);

ALTER TABLE myschema.mytable ENABLE ROW LEVEL SECURITY;
```

**Key policy decisions** (from review):
- `WITH CHECK` clause on both policies — required for INSERT/UPDATE correctness
- `mg_group IS NULL` in USING (Policy 2) — unassigned rows visible to row-level users
- `mg_group IS NOT NULL` in WITH CHECK (Policy 2) — row-level users MUST specify group on INSERT
- `ENABLE` not `FORCE` — molgenis superuser bypasses RLS for DDL

**Performance**:
- `LIKE 'MG_ROLE_myschema/%'` uses btree index on `pg_authid.rolname` (prefix match)
- `pg_has_role()` uses PG's internal role membership cache
- Policy 1 has no row references → likely evaluated once per query (verify with EXPLAIN ANALYZE in Phase 1 tests)
- GIN index on mg_group handles array containment
- 1000 users (`MG_USER_*`) don't affect scan — filtered out by LIKE prefix

## mg_group Auto-Population

On INSERT, when user belongs to exactly one row-level role: auto-set `mg_group = ARRAY[role_name]`.
When user belongs to multiple row-level roles: require explicit mg_group value (error if missing).
Non-row-level users: mg_group not required (can be set for ownership tracking).

**Performance** (from review): Cache user's row-level roles at transaction start, not per-batch query.
This logic lives in Java (SqlTable insert/update methods), not in triggers.

**Migration path for existing data**: When RLS is enabled on an existing table, all existing rows have `mg_group = NULL`. These rows are visible to both schema-level users (Policy 1) and row-level users (Policy 2, `mg_group IS NULL` clause). Admin can bulk-assign groups via SQL: `UPDATE table SET mg_group = ARRAY['MG_ROLE_schema/GroupName'] WHERE condition`.

## Implementation Phases

### Phase 1: Branch Setup + Tests First (TDD)
**Status**: COMPLETE (branch created, tests written, being refined)

**Goal**: New branch from master, write failing tests that define expected behavior.

**Test design principles** (refined during implementation):
- Tests call the **real API** — no private stub methods mixing test code with implementation
- `TestSqlRoleManager` tests `SqlRoleManager` directly (obtained via `((SqlDatabase) db).getRoleManager()`)
- `TestRowLevelSecurity` tests RLS through public Schema/Table API (`table.getMetadata().enableRowLevelSecurity()`, `schema.addMember()`)
- All test operations wrapped in `tx()` blocks for proper transaction isolation
- Each test creates a unique schema name (e.g., `TestRM_createRole`, `TestRLS_ownGroup`)
- User names prefixed per test class (`rm_user1`, `rls_user1`) to avoid collision
- A minimal stub `SqlRoleManager.java` class is created so tests compile (methods throw until Phase 3)

**Parallel test safety**: `TestDatabaseFactory.getTestDatabase()` returns a new `SqlDatabase` instance each time. Each instance has its own `SqlUserAwareConnectionProvider`. Within `tx()`, a dedicated transactional connection is created with `SET ROLE` scoping. PG roles are cluster-global but only visible after commit. Unique schema names + prefixed user names ensure cross-class parallelism is safe.

**Test classes**:

`TestSqlRoleManager.java` — tests SqlRoleManager CRUD operations:
   - Create custom role, verify PG role exists
   - Create row-level role, verify MG_ROWLEVEL membership
   - Add/remove member, verify via pg_auth_members
   - Set per-table GRANT, verify via has_table_privilege
   - System role protection (refuse modify/delete of Viewer, Editor, GroupViewer, GroupEditor, etc.)
   - Global role creation (mg_global schema prefix)
   - Role description via COMMENT ON ROLE
   - **Negative tests**: duplicate role name (idempotent), delete system role, grant to non-existent table

`TestRowLevelSecurity.java` — tests end-to-end RLS behavior through public API:
   - Replaces old `@Disabled` test that used the deprecated `MG_EDIT_ROLE` approach
   - `enableRowLevelSecurity()` → mg_group column + RLS enabled
   - Row-level user sees only own group's rows
   - Schema-level user sees all rows
   - User in both row-level and schema-level → sees all
   - mg_group auto-population on insert (single group → auto, multiple → error)
   - `disableRowLevelSecurity()` → policies dropped, column preserved
   - **NULL mg_group handling**: existing rows with NULL visible to all
   - **WITH CHECK test**: row-level user INSERT must specify mg_group
   - **DELETE with RLS**: row-level user deletes only own group's rows
   - **Admin bypass**: owner/superuser sees all rows
   - **Anonymous user**: sees nothing unless granted access

**Critical files**:
- `backend/molgenis-emx2-sql/src/test/java/org/molgenis/emx2/sql/TestSqlRoleManager.java` (NEW)
- `backend/molgenis-emx2-sql/src/test/java/org/molgenis/emx2/sql/TestRowLevelSecurity.java` (REPLACE old)
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java` (NEW, stub only)
- `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Constants.java` (add MG_GROUP)

### Phase 2: Migration
**Goal**: MG_ROWLEVEL marker role, new default roles

1. Create `migration31.sql` (next after current migration30):
   - Create `MG_ROWLEVEL` marker role (idempotent)
   - Create GroupViewer + GroupEditor per existing schema (same loop pattern as migration10.sql)
   - GroupViewer inherits from Count, GroupEditor inherits from Count
   - `GRANT MG_ROWLEVEL TO` all new row-level roles
   - Does NOT touch existing roles, grants, or table structure
2. Update `Migrations.java`: bump `SOFTWARE_DATABASE_VERSION` to 32, add migration31 step

**Critical files**:
- `backend/molgenis-emx2-sql/src/main/resources/org/molgenis/emx2/sql/migration31.sql` (NEW)
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/Migrations.java`

### Phase 3: SqlRoleManager (Java Layer)
**Goal**: Central class for role operations, accessible via `database.getRoleManager()`

**API access pattern**:
- `SqlRoleManager` constructed with `SqlDatabase` reference, uses `database.getJooq()` for current (possibly transactional) context
- Accessible via `((SqlDatabase) database).getRoleManager()` (internal API)
- Schema provides convenience helpers that delegate with schema name pre-filled (e.g., `schema.createRole(name, isRowLevel)`)
- Inside `tx()`: get RoleManager from the transactional `db` parameter to ensure correct jooq context

1. Create `SqlRoleManager.java`:
   - `createRole(schemaName, roleName, isRowLevel)` — idempotent CREATE ROLE + optional GRANT MG_ROWLEVEL
   - `deleteRole(schemaName, roleName)` — with system-role protection check
   - `addMember(schemaName, roleName, userName)` — GRANT role TO user
   - `removeMember(schemaName, roleName, userName)` — REVOKE role FROM user
   - `getMembers(schemaName, roleName)` — query pg_auth_members
   - `getRolesForUser(userName)` — query pg_auth_members
   - `getRowLevelRolesForUser(userName, schemaName)` — filtered for caching
   - `isRowLevel(schemaName, roleName)` — pg_has_role(role, MG_ROWLEVEL, member)
   - `roleExists(schemaName, roleName)` — query pg_roles
   - `setTablePermission(schemaName, roleName, tableName, privilege)` — per-table GRANT
   - `revokeTablePermission(schemaName, roleName, tableName, privilege)` — per-table REVOKE
   - `getRolesForSchema(schemaName)` — query pg_roles WHERE rolname LIKE prefix
   - `isSystemRole(roleName)` — checks 8 enum values + GroupViewer/GroupEditor
   - `setDescription(schemaName, roleName, description)` — COMMENT ON ROLE
   - `getDescription(schemaName, roleName)` — shobj_description()

   Role creation is idempotent (`DO $$ IF NOT EXISTS...$$` pattern from existing codebase).

2. System role constants:
   ```java
   static final String GROUPVIEWER = "GroupViewer";
   static final String GROUPEDITOR = "GroupEditor";
   static final List<String> SYSTEM_ROLES = List.of(
     "Exists", "Range", "Aggregator", "Count",
     "Viewer", "Editor", "Manager", "Owner",
     GROUPVIEWER, GROUPEDITOR);
   ```

3. Add `getRoleManager()` to `SqlDatabase`

4. Update `SqlSchemaMetadataExecutor.executeCreateSchema()`:
   - After creating existing 8 roles, also create GroupViewer + GroupEditor
   - GRANT MG_ROWLEVEL TO new row-level roles

5. Update `SqlSchemaMetadataExecutor.executeDropSchema()`:
   - Drop custom roles for schema (query pg_roles by prefix)

6. Update `SqlTableMetadataExecutor.executeCreateTable()`:
   - After existing grants, also GRANT to custom roles for this schema

**Critical files**:
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java` (implement stub from Phase 1)
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlDatabase.java`
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlSchemaMetadataExecutor.java`
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTableMetadataExecutor.java`

### Phase 4: RLS Policies + mg_group Column
**Goal**: Adding mg_group to a table enables RLS; Java creates policies

1. Extend `SqlTableMetadataExecutor`:
   - `enableRowLevelSecurity(jooq, schemaName, tableName)`: adds `mg_group TEXT[] DEFAULT NULL`, GIN index, creates 2 RLS policies with WITH CHECK, ENABLE RLS (not FORCE)
   - `disableRowLevelSecurity(jooq, schemaName, tableName)`: drops policies, DISABLE RLS (keep column for data preservation)

2. mg_group auto-population in `SqlTable.insert()`:
   - Cache user's row-level roles at transaction start (not per-batch)
   - If exactly 1 role: auto-set mg_group
   - If multiple roles: require explicit value (clear error message)
   - If 0 (schema-level user): mg_group optional

**Critical files**:
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTableMetadataExecutor.java`
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTable.java`

### Phase 5: Domain Model Updates
**Goal**: Java interfaces for role/permission management

1. Domain records:
   - `RoleMetadata`: record(roleName, description, schemaName, isRowLevel) — assembled from PG catalog queries
   - `RolePermission`: record(roleName, tableName, select, insert, update, delete)

2. Update `Schema` interface:
   - `getRoles()` — existing, extended with custom roles
   - `createRole(name, description, isRowLevel)`, `deleteRole(name)`
   - `setTablePermission(roleName, tableName, ...)`, `getTablePermissions(roleName)`

3. Global roles accessed via `db.getSchema("mg_global")` (virtual schema pattern)

**Critical files**:
- `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Schema.java`
- `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Database.java`

### Phase 6: GraphQL API
**Goal**: Expose role/permission management via GraphQL

1. Schema-level queries (in `_schema`):
   ```graphql
   roles { name, description, isRowLevel, members, permissions { tableName, select, insert, update, delete } }
   ```

2. Schema-level mutations:
   ```graphql
   createRole(name, description, isRowLevel)
   deleteRole(name)
   setRolePermission(roleName, tableName, select, insert, update, delete)
   addRoleMember(roleName, userName)
   removeRoleMember(roleName, userName)
   enableRowLevelSecurity(tableName)
   disableRowLevelSecurity(tableName)
   ```

3. Global roles via `_schema` on mg_global virtual schema

4. Backwards compat: existing `members` and `roles` queries unchanged; new `rolesDetailed` field for rich data

**Critical files**:
- `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlSchemaFieldFactory.java`
- `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlDatabaseFieldFactory.java`

### Phase 7: Import/Export + UI
**Goal**: Schema export/import includes RLS config; admin UI for role management

1. Export: mg_group column presence signals RLS
2. Import: if mg_group in column list → enable RLS; validate role names exist
3. Export/import custom role definitions (from pg_roles queries)
4. Export/import mg_group column values
5. UI: Role management in schema settings (defer detailed design)

**Critical files**:
- `backend/molgenis-emx2-io/src/main/java/org/molgenis/emx2/io/emx2/Emx2.java`

## Migration Strategy

Migration 31 (minimal, no custom tables):
1. Create `MG_ROWLEVEL` marker role (idempotent)
2. For each existing schema: create GroupViewer + GroupEditor roles, GRANT MG_ROWLEVEL
3. GroupViewer inherits Count, GroupEditor inherits Count (NOT from each other)
4. Does NOT touch existing roles, grants, or table structure
5. No custom metadata tables created
6. Idempotent — safe to re-run (uses EXCEPTION WHEN duplicate_object pattern from migration10.sql)

## Branch Strategy

Branch `mswertz/rowlevel-permissions-v2` created from master (current feat/rowlevel_permissions stays intact).

## Verification Plan

1. **TDD cycle**: Write tests first (Phase 1), then implement until green
2. **Unit tests**: `./gradlew :backend:molgenis-emx2-sql:test --tests "*.TestSqlRoleManager"`
3. **Integration tests**: `./gradlew :backend:molgenis-emx2-sql:test --tests "*.TestRowLevelSecurity"`
4. **GraphQL tests**: `./gradlew :backend:molgenis-emx2-graphql:test --tests "*.TestGraphqlPermissions"`
5. **Full test suite**: `./gradlew test`

## Design Decisions Log

### group_metadata eliminated in favor of MG_ROWLEVEL marker role

**Problem**: Needed to store `is_row_level` flag per role for RLS policies.

**Considered**:
1. ~~`group_metadata` table (group_name, is_row_level)~~ — works but adds a custom table, migration, save/load code, double administration risk
2. ~~Naming convention (`MG_ROLE_schema/~GroupName`)~~ — no storage, but ugly in mg_group values and fragile
3. ~~PG role comments (JSON)~~ — fragile parsing, can't use in SQL policies
4. **Marker PG role (`MG_ROWLEVEL`)** — pure PG catalog, `pg_has_role()` check, scoped LIKE filter keeps scan to ~10 roles per schema

**Chosen**: Option 4. Zero custom tables. PG catalog is sole source of truth.

### GroupViewer/GroupEditor NOT in Privileges enum

**Problem**: Adding to Privileges enum breaks `Privileges.values()` iteration in tests, GraphQL, etc.

**Chosen**: Keep enum as-is (8 values: EXISTS→OWNER). GroupViewer/GroupEditor are String constants in SqlRoleManager. System role protection checks both enum and constants.

### ENABLE vs FORCE ROW LEVEL SECURITY

**Problem**: `FORCE` makes even table owners comply with RLS, breaking admin DDL.

**Chosen**: `ENABLE` only. Table owner (molgenis superuser) bypasses RLS for schema operations.

### mg_group DEFAULT NULL vs '{}'

**Problem**: Empty array `'{}'` causes different behavior than NULL with `= ANY()`.

**Chosen**: `DEFAULT NULL`. Policies handle NULL explicitly: `mg_group IS NULL` in USING means unassigned rows are visible; `mg_group IS NOT NULL` in WITH CHECK means row-level users must specify group.

### GroupEditor inheritance

**Problem**: If GroupEditor inherits GroupViewer, `pg_has_role(GroupEditor, 'MG_ROWLEVEL', 'member')` becomes true transitively.

**Chosen**: GroupEditor inherits from Count only. No inheritance from GroupViewer. Both get direct `GRANT MG_ROWLEVEL`.

## Review Findings (Incorporated)

Reviews from: PG expert, Java architect, pragmatist data manager, test case designer.
All approved architecture. Critical findings integrated above:
- 6 must-fix items (all addressed in plan)
- 7 should-fix items (all addressed in plan)
- 65 test cases designed (P0: 40, P1: 16, P2: 9) — integrated into Phase 1
- UI workflow deferred to Phase 7+

## Open Questions (Resolved)

1. ~~Extend Privileges enum?~~ → **No**, use String constants in SqlRoleManager
2. ~~Global group schema name?~~ → **mg_global**, accessed via virtual schema pattern
3. ~~mg_group on all tables?~~ → **Only when RLS enabled** on that specific table
4. ~~Need group_metadata table?~~ → **No**, use MG_ROWLEVEL marker role instead
5. ~~FORCE or ENABLE RLS?~~ → **ENABLE** only (owner bypasses)
6. ~~mg_group DEFAULT?~~ → **NULL** (not '{}')
7. ~~GroupEditor inherits GroupViewer?~~ → **No**, both inherit Count independently
8. ~~Privileges enum?~~ → **Keep 8 values**, constants for GroupViewer/GroupEditor
