# Row-Level Permissions: Java-First Redesign

## Context

The current `feat/rowlevel_permissions` branch implements row-level security via 729 lines of PL/pgSQL triggers, materialized views, and metadata tables. Five independent reviewers unanimously found critical bugs (wrong PKs, missing policy checks, MV staleness = security hole) and rejected the trigger-based approach as overengineered and untestable.

**Decision**: Pivot to Java-first approach. New branch from master. PG catalog is the sole source of truth — no custom metadata tables. Java orchestrates everything.

**Requirements**: See `.plan/specs/rowlevel-scenarios.md` for full scenarios, personas, and requirements.

## Design Principles

1. **PG catalog = sole source of truth** — roles, grants, membership, row-level marker, description all in PG
2. **No custom metadata tables** — eliminated `group_metadata`; `is_row_level` via marker role, description via `COMMENT ON ROLE`, schema from role name
3. **Java = orchestrator** — creates roles, executes GRANT/REVOKE, creates RLS policies
4. **No triggers, no materialized views** — eliminates staleness and debugging complexity
5. **Backwards compatible** — existing 8 default roles (EXISTS→OWNER) unchanged
6. **Linux ACL-inspired naming** — mg_can_edit (owning group, rw) + mg_can_view (read-only)
7. **Two RLS patterns** — Pattern A (public read, group write) + Pattern B (group read+write), per-table
8. **System roles protected** — SqlRoleManager refuses to modify/delete system-created roles

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
GRANT "MG_ROWLEVEL" TO "MG_ROLE_myschema/HospitalA";

SELECT pg_has_role('MG_ROLE_myschema/HospitalA', 'MG_ROWLEVEL', 'member');  -- true
SELECT pg_has_role('MG_ROLE_myschema/Viewer', 'MG_ROWLEVEL', 'member');     -- false
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

## Row-Level ACL Columns

Adding mg_can_edit to a table = enabling RLS. mg_can_view is optional (only for Pattern B with read/write separation).

```sql
ALTER TABLE <schema>.<table>
  ADD COLUMN mg_can_edit TEXT[] DEFAULT NULL;
CREATE INDEX idx_<table>_mg_can_edit ON <schema>.<table> USING GIN(mg_can_edit);

ALTER TABLE <schema>.<table>
  ADD COLUMN mg_can_view TEXT[] DEFAULT NULL;
CREATE INDEX idx_<table>_mg_can_view ON <schema>.<table> USING GIN(mg_can_view);

ALTER TABLE <schema>.<table> ENABLE ROW LEVEL SECURITY;
```

| Column | Linux analogy | Controls |
|--------|---------------|----------|
| `mg_can_edit` | Owning group (rw) | Who can read+write this row |
| `mg_can_view` | Extended ACL / read group (r) | Who can read this row (in addition to mg_can_edit) |

**Key decisions** (from review):
- `DEFAULT NULL` not `'{}'` — NULL means "no group assigned", handled explicitly in policies
- `ENABLE` not `FORCE` — table owner (molgenis superuser) bypasses RLS for DDL operations
- Removing mg_can_edit = disabling RLS (policies dropped, column optionally preserved)
- mg_can_view only added when table uses Pattern B with read/write separation

## Default Roles Per Schema (Unchanged)

The existing 8 system roles per schema remain unchanged:

| Role | PG Role | PG Grants |
|------|---------|-----------|
| Exists | MG_ROLE_schema/Exists | USAGE on schema |
| Range | MG_ROLE_schema/Range | inherits Exists |
| Aggregator | MG_ROLE_schema/Aggregator | inherits Range |
| Count | MG_ROLE_schema/Count | inherits Aggregator |
| Viewer | MG_ROLE_schema/Viewer | SELECT on all tables |
| Editor | MG_ROLE_schema/Editor | INSERT/UPDATE/DELETE on all tables |
| Manager | MG_ROLE_schema/Manager | ALL + WITH ADMIN OPTION |
| Owner | MG_ROLE_schema/Owner | ALL + ADMIN |

All 8 system roles are protected from modification/deletion by SqlRoleManager.

## Custom Row-Level Roles

Admin creates custom roles per schema via SqlRoleManager. Each custom role has:
- Per-table permissions (SELECT, INSERT, UPDATE, DELETE) — managed via GRANT/REVOKE
- Per-column permissions (SELECT, UPDATE) — PG native, same pattern
- Optional MG_ROWLEVEL membership — makes it a row-level role
- Members (users assigned to the role)

Example workflow:
```
1. Admin creates role:      rm.createRole("myschema", "HospitalA", isRowLevel=true)
2. Admin grants per-table:  rm.grantTablePermission("myschema", "HospitalA", "patients", "SELECT")
3. Admin grants per-column: rm.grantColumnPermission("myschema", "HospitalA", "patients", "age", "SELECT")
4. Admin assigns user:      rm.addMember("myschema", "HospitalA", "user1")
5. Rows tagged:             mg_can_edit = ['MG_ROLE_myschema/HospitalA']
6. user1 queries:           sees only rows with matching mg_can_edit
```

Global roles use schema prefix `mg_global`:
- `MG_ROLE_mg_global/DataStewards` — cross-schema, accessed via `db.getSchema("mg_global")`

## RLS Policy Design

Two patterns, configurable per table via `table.getMetadata().enableRowLevelSecurity(pattern)`:

### Pattern A: Public read, group write

```sql
CREATE POLICY select_all ON myschema.mytable FOR SELECT USING (true);
CREATE POLICY group_write ON myschema.mytable FOR INSERT
  WITH CHECK (is_schema_level_user() OR mg_can_edit && current_user_roles());
CREATE POLICY group_update ON myschema.mytable FOR UPDATE
  USING (is_schema_level_user() OR mg_can_edit && current_user_roles());
CREATE POLICY group_delete ON myschema.mytable FOR DELETE
  USING (is_schema_level_user() OR mg_can_edit && current_user_roles());
ALTER TABLE myschema.mytable ENABLE ROW LEVEL SECURITY;
```

Use for: catalogues, metadata tables, reference tables.

### Pattern B: Group read + group write

```sql
CREATE POLICY group_or_schema_read ON myschema.mytable FOR SELECT
  USING (is_schema_level_user()
    OR mg_can_edit && current_user_roles()
    OR mg_can_view && current_user_roles()
    OR mg_can_edit IS NULL);
CREATE POLICY group_write ON myschema.mytable FOR INSERT
  WITH CHECK (is_schema_level_user() OR mg_can_edit && current_user_roles());
CREATE POLICY group_update ON myschema.mytable FOR UPDATE
  USING (is_schema_level_user() OR mg_can_edit && current_user_roles());
CREATE POLICY group_delete ON myschema.mytable FOR DELETE
  USING (is_schema_level_user() OR mg_can_edit && current_user_roles());
ALTER TABLE myschema.mytable ENABLE ROW LEVEL SECURITY;
```

Use for: patient data, sensitive tables, embargoed data.

**Key policy decisions** (from review):
- `WITH CHECK` clause on write policies — required for INSERT/UPDATE correctness
- `mg_can_edit IS NULL` in Pattern B — unassigned rows visible to schema-level users only
- `ENABLE` not `FORCE` — molgenis superuser bypasses RLS for DDL
- `is_schema_level_user()` = helper function checking non-MG_ROWLEVEL roles (includes system roles AND custom roles with isRowLevel=false)
- `current_user_roles()` = helper function returning user's MG_ROWLEVEL role names as array

**Performance**:
- GIN index on mg_can_edit/mg_can_view handles array overlap (`&&`)
- `is_schema_level_user()` uses PG's internal role membership cache
- Pattern A SELECT policy has no row references → evaluated once per query
- 1000 users (`MG_USER_*`) don't affect scan — filtered by role type

## mg_can_edit Auto-Population

On INSERT, when user belongs to exactly one row-level role: auto-set `mg_can_edit = ARRAY[role_name]`.
When user belongs to multiple row-level roles: require explicit mg_can_edit value (error if missing).
Non-row-level users: mg_can_edit not required (can be set for ownership tracking).

**Performance** (from review): Cache user's row-level roles at transaction start, not per-batch query.
This logic lives in Java (SqlTable insert/update methods), not in triggers.

**Migration path for existing data**: When RLS is enabled on an existing table, all existing rows have `mg_can_edit = NULL`. These rows are visible to schema-level users only. Admin can bulk-assign groups via SQL: `UPDATE table SET mg_can_edit = ARRAY['MG_ROLE_schema/GroupName'] WHERE condition`.

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
   - System role protection (refuse modify/delete of Viewer, Editor, etc.)
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
- `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Constants.java` (add MG_CAN_EDIT, MG_CAN_VIEW)

### Phase 2: Migration
**Goal**: Create MG_ROWLEVEL marker role

1. Create `migration31.sql`:
   - Create `MG_ROWLEVEL` marker role (idempotent, `CREATE ROLE IF NOT EXISTS` pattern)
   - No per-schema roles created — custom roles are created on demand by admin via SqlRoleManager
   - Does NOT touch existing roles, grants, or table structure
2. Update `Migrations.java`: bump `SOFTWARE_DATABASE_VERSION` to 32, add migration31 step

**Critical files**:
- `backend/molgenis-emx2-sql/src/main/resources/org/molgenis/emx2/sql/migration31.sql` (NEW)
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/Migrations.java`

### Phase 3: SqlRoleManager (Java Layer)
**Goal**: Central class for custom role operations, accessible via `database.getRoleManager()`

**API access pattern**:
- `SqlRoleManager` constructed with `SqlDatabase` reference, uses `database.getJooq()` for current (possibly transactional) context
- Accessible via `((SqlDatabase) database).getRoleManager()` (internal API)
- Schema provides convenience helpers that delegate with schema name pre-filled
- Inside `tx()`: get RoleManager from the transactional `db` parameter to ensure correct jooq context

1. Implement `SqlRoleManager.java` (stub exists from Phase 1):
   - `createRole(schemaName, roleName, isRowLevel)` — idempotent CREATE ROLE + optional GRANT MG_ROWLEVEL
   - `deleteRole(schemaName, roleName)` — with system-role protection check
   - `addMember(schemaName, roleName, userName)` — GRANT role TO user
   - `removeMember(schemaName, roleName, userName)` — REVOKE role FROM user
   - `getMembers(schemaName, roleName)` — query pg_auth_members
   - `getRolesForUser(userName)` — query pg_auth_members
   - `getRowLevelRolesForUser(userName, schemaName)` — filtered for caching
   - `isRowLevel(schemaName, roleName)` — pg_has_role(role, MG_ROWLEVEL, member)
   - `roleExists(schemaName, roleName)` — query pg_roles
   - `grantTablePermission(schemaName, roleName, tableName, privilege)` — per-table GRANT
   - `revokeTablePermission(schemaName, roleName, tableName, privilege)` — per-table REVOKE
   - `grantColumnPermission(schemaName, roleName, tableName, columnName, privilege)` — per-column GRANT
   - `revokeColumnPermission(schemaName, roleName, tableName, columnName, privilege)` — per-column REVOKE
   - `getRolesForSchema(schemaName)` — query pg_roles WHERE rolname LIKE prefix
   - `isSystemRole(roleName)` — checks 8 existing enum values
   - `setDescription(schemaName, roleName, description)` — COMMENT ON ROLE
   - `getDescription(schemaName, roleName)` — shobj_description()

   Role creation is idempotent (`DO $$ IF NOT EXISTS...$$` pattern from existing codebase).

2. System role constants (in Constants.java):
   ```java
   ROLE_EXISTS, ROLE_RANGE, ROLE_AGGREGATOR, ROLE_COUNT,
   ROLE_VIEWER, ROLE_EDITOR, ROLE_MANAGER, ROLE_OWNER
   ```

3. `getRoleManager()` already added to `SqlDatabase` (Phase 1)

4. Update `SqlSchemaMetadataExecutor.executeDropSchema()`:
   - Drop custom roles for schema (query pg_roles by prefix, skip system roles)

5. Update `SqlTableMetadataExecutor.executeCreateTable()`:
   - After existing grants, also GRANT to custom roles that have permissions on this schema

**Critical files**:
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java` (implement stub)
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlSchemaMetadataExecutor.java`
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTableMetadataExecutor.java`

### Phase 4: RLS Policies + ACL Columns
**Goal**: Adding mg_can_edit to a table enables RLS; Java creates pattern-specific policies

1. Extend `SqlTableMetadataExecutor`:
   - `enableRowLevelSecurity(jooq, schemaName, tableName, pattern)`: adds `mg_can_edit TEXT[] DEFAULT NULL` (+ `mg_can_view` for Pattern B), GIN indexes, creates pattern-specific RLS policies, ENABLE RLS (not FORCE)
   - `disableRowLevelSecurity(jooq, schemaName, tableName)`: drops policies, DISABLE RLS (keep columns for data preservation)

2. Pattern A policies: public SELECT, group INSERT/UPDATE/DELETE
3. Pattern B policies: group+schema SELECT (using mg_can_edit + mg_can_view), group INSERT/UPDATE/DELETE

4. PG helper functions:
   - `is_schema_level_user()` — checks if current user has any non-MG_ROWLEVEL role for schema
   - `current_user_roles()` — returns array of user's role names (cached per transaction)

5. mg_can_edit auto-population in `SqlTable.insert()`:
   - Cache user's row-level roles at transaction start (not per-batch)
   - If exactly 1 role: auto-set mg_can_edit
   - If multiple roles: require explicit value (clear error message)
   - If 0 (schema-level user): mg_can_edit optional

**Critical files**:
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTableMetadataExecutor.java`
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTable.java`

### Phase 4b: mg_status + Provenance (to be discussed)
**Goal**: Status state machine and append-only audit table. Scope and design of mg_status needs product owner input.

1. `mg_status` column (TEXT, new feature — does NOT replace existing mg_draft):
   - Proposed states: Draft, Submitted, Published, Revision needed, Withdrawn
   - Transition control via column-level permissions: `GRANT UPDATE(mg_status)` to Publisher/Editor roles
   - App-level validation of allowed transitions per state
   - mg_draft continues to work independently for simple draft/final toggling

2. Provenance table (per schema):
   - Columns: id, timestamp, user, table_name, row_id, action, mg_can_edit, details(JSONB)
   - INSERT only — no UPDATE/DELETE grants for any role
   - Auto-entries on: record creation, status change, group change, deletion
   - Schema-level users SELECT all; row-level users SELECT own rows

**Critical files**:
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTableMetadataExecutor.java`
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTable.java`

### Phase 5: Domain Model Updates
**Goal**: Java interfaces for role/permission management

1. Domain records:
   - `RoleMetadata`: record(roleName, description, schemaName, isRowLevel) — assembled from PG catalog queries
   - `RolePermission`: record(roleName, tableName, select, insert, update, delete)
   - `ColumnPermission`: record(roleName, tableName, columnName, select, update)

2. Update `Schema` interface:
   - `getRoles()` — existing, extended with custom roles
   - `createRole(name, description, isRowLevel)`, `deleteRole(name)`
   - `setTablePermission(roleName, tableName, ...)`, `getTablePermissions(roleName)`
   - `setColumnPermission(roleName, tableName, columnName, ...)`, `getColumnPermissions(roleName)`

3. Update `TableMetadata`:
   - `enableRowLevelSecurity(pattern)` — Pattern A or B
   - `getRlsPattern()` — returns current pattern or null
   - `hasStatusColumn()` — checks if mg_status is enabled

4. Global roles accessed via `db.getSchema("mg_global")` (virtual schema pattern)

**Critical files**:
- `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Schema.java`
- `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Database.java`
- `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/TableMetadata.java`

### Phase 6: GraphQL API
**Goal**: Expose role/permission management via GraphQL

1. Schema-level queries (in `_schema`):
   ```graphql
   roles { name, description, isRowLevel, members,
     tablePermissions { tableName, select, insert, update, delete },
     columnPermissions { tableName, columnName, select, update } }
   ```

2. Schema-level mutations:
   ```graphql
   createRole(name, description, isRowLevel)
   deleteRole(name)
   setRolePermission(roleName, tableName, select, insert, update, delete)
   setColumnPermission(roleName, tableName, columnName, select, update)
   addRoleMember(roleName, userName)
   removeRoleMember(roleName, userName)
   enableRowLevelSecurity(tableName, pattern)
   disableRowLevelSecurity(tableName)
   ```

3. Global roles via `_schema` on mg_global virtual schema

4. Backwards compat: existing `members` and `roles` queries unchanged; new `rolesDetailed` field for rich data

**Critical files**:
- `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlSchemaFieldFactory.java`
- `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlDatabaseFieldFactory.java`

### Phase 7: Import/Export + UI
**Goal**: Schema export/import includes RLS config; admin UI for role management

1. Export: mg_can_edit/mg_can_view column presence signals RLS, pattern stored in metadata
2. Import: if mg_can_edit in column list → enable RLS; validate role names exist
3. Export/import custom role definitions (from pg_roles queries)
4. Export/import mg_can_edit/mg_can_view column values
5. Export/import provenance table (read-only)
6. UI: Role management in schema settings (defer detailed design)
7. UI: mg_status workflow visualization (defer detailed design)

**Critical files**:
- `backend/molgenis-emx2-io/src/main/java/org/molgenis/emx2/io/emx2/Emx2.java`

## Migration Strategy

Migration 31 (minimal):
1. Create `MG_ROWLEVEL` marker role (idempotent)
2. Does NOT touch existing roles, grants, or table structure
3. No custom metadata tables created
4. No per-schema roles created — custom roles are created on demand by admin
5. Idempotent — safe to re-run

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
**Chosen**: Marker PG role (`MG_ROWLEVEL`). Zero custom tables. PG catalog is sole source of truth.

### No predefined row-level roles (GroupViewer/GroupEditor removed)
**Chosen**: Admin creates custom roles (e.g., HospitalA, HospitalB) via SqlRoleManager with per-table permissions.

### ENABLE vs FORCE ROW LEVEL SECURITY
**Chosen**: `ENABLE` only. Table owner (molgenis superuser) bypasses RLS for schema operations.

### mg_can_edit DEFAULT NULL vs '{}'
**Chosen**: `DEFAULT NULL`. NULL = visible to schema-level users only (safe default).

### Custom roles have per-table AND per-column permissions
**Chosen**: Each custom role gets per-table + per-column GRANT/REVOKE via SqlRoleManager. Stored purely in PG catalog.

### Linux ACL-inspired naming: mg_can_edit + mg_can_view
**Problem**: Single mg_group column couldn't express separate read/write permissions.
**Chosen**: mg_can_edit (owning group, rw) + mg_can_view (read-only). Mirrors Linux ACL (owner group + extended ACL).

### Two RLS patterns per table (Pattern A + B)
**Chosen**: Pattern A (public read, group write) for catalogues/metadata. Pattern B (group read+write) for sensitive data. Configurable per table.

### mg_status is a new feature (to be discussed)
mg_status does NOT replace mg_draft. They coexist: mg_draft for simple boolean toggling, mg_status for richer workflows. Exact scope and design needs product owner discussion.

### Append-only provenance table
**Chosen**: INSERT-only table tracking all state changes, ownership transfers, deletions. No UPDATE/DELETE.

### Deletion by editor/admin only
**Chosen**: Row-level users cannot delete. Only schema-level Editor/Manager/Owner can delete.

## Review Findings (Incorporated)

Reviews from: PG expert, Java architect, pragmatist data manager, test case designer.
All approved architecture. Critical findings integrated above.
Scenario validation from 7 synthetic personas (see `.plan/specs/rowlevel-scenarios.md`).

## Resolved Questions

1. ~~Extend Privileges enum?~~ → **No**, keep 8 values unchanged
2. ~~Global group schema name?~~ → **mg_global**, accessed via virtual schema pattern
3. ~~mg_can_edit on all tables?~~ → **Only when RLS enabled** on that specific table
4. ~~Need group_metadata table?~~ → **No**, use MG_ROWLEVEL marker role instead
5. ~~FORCE or ENABLE RLS?~~ → **ENABLE** only (owner bypasses)
6. ~~mg_can_edit DEFAULT?~~ → **NULL** (not '{}')
7. ~~Need GroupViewer/GroupEditor per schema?~~ → **No**, custom roles only
8. ~~Separate read/write?~~ → **Yes**, mg_can_edit + mg_can_view
9. ~~mg_draft boolean?~~ → **mg_draft stays**; mg_status is a separate new feature (to be discussed)
10. ~~Column-level via same pattern?~~ → **Yes**, PG native GRANT/REVOKE

## Open Questions (Need Product Owner Input)

1. **mg_status scope and design**: is this in scope for RLS? If so: fixed states or configurable? Interaction with mg_draft?
2. **Time-limited access**: role description JSON + cron? Or first-class expiration field?
3. **Group inheritance**: auto-inherit from parent FK? Explicit per row? Configurable?
4. **"Import as" context**: how does a Manager import on behalf of an institute?
5. **Embargo auto-lift**: scheduled job? Trigger? Manual?
6. **Role templates**: predefined permission bundles? Or always explicit per-table?
7. **Provenance granularity**: every field change or only status/group/key fields?
