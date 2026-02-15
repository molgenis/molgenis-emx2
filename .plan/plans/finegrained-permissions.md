# Fine-grained Permissions: Implementation Plan v5.2.0

## Overview

Add fine-grained permissions to MOLGENIS EMX2 so different groups can work in the same schema with controlled access over multiple schemas, per schema, per table, per row, and per column. Uses PostgreSQL native RLS policies with a Java orchestration layer (SqlRoleManager).

Two sources of truth:
- **PG catalog**: table-level grants (SELECT/INSERT/UPDATE/DELETE) via `GRANT`/`REVOKE`, queried with `has_table_privilege()`. Also role membership and inheritance.
- **`MOLGENIS.rls_permissions`**: select level (VARCHAR: EXISTS/RANGE/AGGREGATOR/COUNT/TABLE/ROW), RLS restriction flags (boolean: insert_rls/update_rls/delete_rls), column access overrides (VARCHAR[]: editable/readonly/hidden), and grant permission (boolean). This is the source of truth for custom roles.

`SqlRoleManager` combines both sources via inline SQL queries loaded from resource files (`permissions_query.sql`, `permissions_explain_query.sql`). `permissions_query.sql` uses INNER JOIN from rls_permissions (driving table) with pg_tables and has_table_privilege().

Column-level access (editable/readonly/hidden) is enforced at application layer (GraphQL field filtering + SqlTable mutation validation), not PG. RLS row filtering uses pre-computed session variable lists -- no per-row subqueries. 5 session variables: `active_role`, `rls_select_tables`, `rls_insert_tables`, `rls_update_tables`, `rls_delete_tables`. System roles have no entries in `rls_permissions`, so their lists are empty = unrestricted.

- Requirements: `.plan/specs/finegrained-scenarios.md`
- Technical spec: `.plan/specs/finegrained-spec.md`

## Architecture

```
                    +---------------------+
                    |   GraphQL API       |
                    |   (queries/mutations)|
                    +----------+----------+
                               |
                    +----------v----------+
                    |   SqlRoleManager    |
                    |   (Java orchestrator)|
                    +----------+----------+
                               |
              +----------------+----------------+
              |                |                |
    +---------v-----+ +-------v-------+ +------v------+
    | PG Roles      | | PG Grants     | | PG RLS      |
    | + COMMENT ON  | | (permissions) | | (row filter) |
    |   ROLE        | |               | |              |
    +---------------+ +-------+-------+ +------+------+
                              |                |
                    +---------v---------+      |
                    | rls_permissions    |------+
                    | (select level +   |
                    |  RLS flags +      |
                    |  column access)   |
                    +-------------------+
```

### Unified grant management

System roles and custom roles are managed via the same mechanism:
- When a table is created, grants are applied for ALL roles (system AND custom)
- Schema-wide intent is derived from PG catalog AND `rls_permissions` (schema-wide rows with `table_name = '*'`)
- `SqlTableMetadataExecutor.createTable()` calls `SqlRoleManager.applySchemaWideGrantsForNewTable()` after applying system role grants

### Key simplification (v5.0)

- `permission_metadata` renamed to `rls_permissions` -- clearer purpose
- Hybrid storage: VARCHAR `select_level` for SELECT (6 levels) + boolean `_rls` flags for INSERT/UPDATE/DELETE
- `MG_ROWLEVEL` marker role removed -- no longer needed; RLS restrictions are tracked in `rls_permissions` and pre-computed into session variables
- `bypass_schemas` session variable removed -- system roles simply have no `rls_permissions` entries
- Inline SQL queries in resource files (`permissions_query.sql`, `permissions_explain_query.sql`) combine PG grants with rls_permissions
- `syncPermissionMetadata()` renamed to `syncRlsPermissions()`

## Phase Status

### Phase 1: Tests First -- NEEDS UPDATE (v5.0 changes)
- TestSqlRoleManager: 10 tests for role CRUD (create, delete, members, grants, system protection)
- TestRowLevelSecurity: 8 end-to-end RLS tests (will be activated in Phase 4)
- **Grant system tests** (new, in TestSqlRoleManager):
  - `setPermission()` + verify PG grants via `has_table_privilege(role, table, 'SELECT'/'INSERT'/etc.)`
  - `setPermission()` + verify `rls_permissions` rows via direct SQL query
  - `setPermission()` + verify `getRoleInfo()` returns matching domain objects (round-trip through `permissions_query.sql`)
  - `revokePermission()` + verify both PG grants and `rls_permissions` rows are cleaned up
  - Test all SelectLevel values (EXISTS through ROW) and ModifyLevel values (TABLE, ROW)
  - Test `grant=true` stored as `grant_permission` and read back correctly
  - Test `table="*"` wildcard stored and expanded correctly
  - Test merge semantics: two grants on same (role, table) merge non-null fields
- **v5.0 changes**: Update test assertions for SelectLevel/ModifyLevel enums. Remove MG_ROWLEVEL references. Update session variable names (`rls_select_tables`, `rls_insert_tables`, `rls_update_tables`, `rls_delete_tables`).
- Files:
  - `backend/molgenis-emx2-sql/src/test/java/org/molgenis/emx2/sql/TestSqlRoleManager.java`
  - `backend/molgenis-emx2-sql/src/test/java/org/molgenis/emx2/sql/TestRowLevelSecurity.java`

### Phase 2: Migration -- NEEDS UPDATE (v5.0 changes)
- `rls_permissions_create.sql`: separate SQL resource file with CREATE TABLE + PK (reusable from migration and SqlRoleManager)
- `migration31.sql`: references `rls_permissions_create.sql`, GRANT SELECT to PUBLIC, creates `MG_ROLE_*/Admin` global system role
- `permissions_query.sql`: joins rls_permissions + pg_tables + has_table_privilege() (rls_permissions is driving table, used by SqlRoleManager.getRoleInfo())
- `permissions_explain_query.sql`: reserved for future admin introspection
- **v5.0 changes**:
  - Rename table from `permission_metadata` to `rls_permissions`
  - Hybrid storage: VARCHAR `select_level` for SELECT (6 levels) + boolean `_rls` flags for INSERT/UPDATE/DELETE
  - Remove `MG_ROWLEVEL` marker role creation (no longer needed)
  - GRANT SELECT on rls_permissions to PUBLIC
  - Creates `MG_ROLE_*/Admin` global system role
- Migrations.java: bumped SOFTWARE_DATABASE_VERSION to 32
- Files:
  - `backend/molgenis-emx2-sql/src/main/resources/org/molgenis/emx2/sql/rls_permissions_create.sql`
  - `backend/molgenis-emx2-sql/src/main/resources/org/molgenis/emx2/sql/migration31.sql`
  - `backend/molgenis-emx2-sql/src/main/resources/org/molgenis/emx2/sql/permissions_query.sql`
  - `backend/molgenis-emx2-sql/src/main/resources/org/molgenis/emx2/sql/permissions_explain_query.sql`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/Migrations.java`

### Phase 3: SqlRoleManager -- NEEDS UPDATE (v5.0 changes)
- Central Java class for custom role management via PG catalog
- CRUD for roles, members, permissions (GRANT/REVOKE)
- System role protection (9 roles: 8 per-schema + Admin, checked via `SYSTEM_ROLES` constant)
- **No inheritance for schema-local custom roles**: schema-local custom roles have NO `addRoleInherits`/`removeRoleInherits`/`getRoleInherits`. Entire access defined by the permission matrix (grants + RLS flags). Global custom roles DO inherit roles per schema (system or custom) via PG GRANT (`GRANT MG_ROLE_<schema>/<role> TO MG_ROLE_*/<name>`).
- **v5.0 changes**:
  - Remove all MG_ROWLEVEL marker role logic (auto-tagging, membership checks, `isRowLevelRole()`)
  - `syncPermissionMetadata()` renamed to `syncRlsPermissions()` -- upserts VARCHAR select_level + boolean _rls flags
  - `getRoleInfo()` / `getRoleInfos()` use inline `permissions_query.sql` (no view)
  - `rls_permissions` table referenced instead of `permission_metadata`
  - `deleteRole()` deletes from `rls_permissions` instead of `permission_metadata`
  - `enableRowLevelSecurity()` creates 4 policies per table using per-operation session variable lists (`rls_select_tables`, `rls_insert_tables`, `rls_update_tables`, `rls_delete_tables`)
  - **Merge semantics**: grants on same (role, table) merge. Non-null fields overwrite, null fields preserved. Upsert uses `COALESCE(EXCLUDED.field, existing.field)` to preserve unset fields. PG grants are additive (no implicit REVOKE). Use explicit `revoke()` to remove.
- Files:
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java`

### Phase 4: RLS Policies + Session Setup -- IN PROGRESS

**Prerequisites** (from Phase 5 gaps -- must complete first):
- Create ColumnAccess.java (editable/readonly/hidden) -- DONE
- Permission.java uses PermissionLevel enum (TABLE/ROW) -- DONE (enum is the domain model; boolean flags are storage-only in rls_permissions)
- Update all references: SqlRoleManager, GraphqlPermissionUtils, tests

#### 4a: Session Variables + Connection Safety (NOT STARTED)
- **Current state**: SqlUserAwareConnectionProvider only does RESET ROLE + SET ROLE, no session variables
- Add 5 session variables in acquire():
  - `SET LOCAL molgenis.active_role = '<full_pg_role_name>'`
  - `SET LOCAL molgenis.rls_select_tables = '<comma_separated_fq_tables>'`
  - `SET LOCAL molgenis.rls_insert_tables = '<comma_separated_fq_tables>'`
  - `SET LOCAL molgenis.rls_update_tables = '<comma_separated_fq_tables>'`
  - `SET LOCAL molgenis.rls_delete_tables = '<comma_separated_fq_tables>'`
- Pre-compute all 4 rls lists with 1 query against `rls_permissions`. **Wildcard expansion** (spec section 6): `table_name='*'` entries in `rls_permissions` must be expanded to all RLS-enabled tables in that schema when computing session variable lists
- System roles have no `rls_permissions` entries -> empty lists -> unrestricted (no special-casing)
- Transaction defaults in tx(): all five set to `''` (empty = nothing restricted = backward compatible)
- Role determined from member record lookup (one role per user per schema, no switching)
- Add SET ROLE failure handling: close connection on error, never return with unknown role state
- Files:
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlUserAwareConnectionProvider.java`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlDatabase.java` (tx() defaults)

#### 4b: RLS Policies (NOT STARTED)
- Four policies per table: SELECT, INSERT, UPDATE, DELETE — each checks its own session variable
- All use session variable lists (no EXISTS subquery)
- **Wildcard expansion**: session variable lists are pre-computed at connection time; `table_name='*'` entries are expanded to all RLS-enabled tables so policies only need simple list membership checks
- Pattern: `'<schema>.<table>' != ALL(string_to_array(rls_<op>_tables, ','))` -> if table NOT in list, allow all rows
- If table IS in list -> check `mg_roles @> ARRAY[active_role]`
- Table name and schema name embedded as literals in policy at creation time
- **UPDATE policy has both USING and WITH CHECK** — USING filters which rows can be seen for update, WITH CHECK prevents updating mg_roles to unauthorized values (privilege escalation prevention)
- INSERT uses only WITH CHECK (no existing rows). DELETE and SELECT use only USING.
- enableRowLevelSecurity() in SqlRoleManager creates all four policies
- Files:
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java`

#### 4c: mg_roles Column + Auto-Population + Unified Table Creation Grants (NOT STARTED)
- mg_roles TEXT[] column with GIN index on opt-in tables
- Auto-populate from user's role on INSERT (if table has RLS)
- Reject mg_roles modification on UPDATE unless Manager+
- **Derive-from-catalog on table creation**: when `SqlTableMetadataExecutor.createTable()` creates a new table, after applying system role grants, call `SqlRoleManager.applySchemaWideGrantsForNewTable()`. This method checks what grants each custom role has on ALL existing tables via `has_table_privilege()`. If a privilege is granted on ALL existing tables, it is granted on the new table. Checks `rls_permissions` for schema-wide RLS entries and applies to new table.
- **Schema-wide setPermission**: when `setPermission()` is called with `table == null`, iterate all current tables and apply grants, store schema-wide row with `table_name = '*'` in `rls_permissions`
- **`enableRowLevelSecurity()` + wildcard**: when enabling RLS on a table, check for `table_name='*'` entries in `rls_permissions` for all custom roles in this schema. If a role has a `*` entry with RLS flags set, apply corresponding grants and RLS restrictions to the new table.
- Files:
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTable.java`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTableMetadataExecutor.java`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java`

#### 4e: Cleanup on Drop (NOT STARTED)
- **NEW**: `cleanupTablePermissions(schemaName, tableName)` in SqlRoleManager -- deletes all `rls_permissions` rows for that table. PG grants auto-drop with table.
- **NEW**: `cleanupSchemaPermissions(schemaName)` in SqlRoleManager -- deletes all `rls_permissions` rows for that schema. PG `DROP SCHEMA CASCADE` handles roles/grants.
- `Schema.dropTable()` must call `cleanupTablePermissions()` BEFORE dropping the table
- `Database.dropSchema()` must call `cleanupSchemaPermissions()` BEFORE `DROP SCHEMA CASCADE`
- Without cleanup, orphaned `rls_permissions` rows would accumulate and could cause stale session variable lists
- Files:
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlSchema.java` (dropTable hook)
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlDatabase.java` (dropSchema hook)

#### 4d: Security Fixes (NOT STARTED)
- Fix migration31.sql: GRANT SELECT (not ALL) on `rls_permissions` to PUBLIC
- Add orphaned mg_roles cleanup to deleteRole()
- Files:
  - `backend/molgenis-emx2-sql/src/main/resources/org/molgenis/emx2/sql/migration31.sql`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java`

### Phase 5: Domain Model -- DONE (updated v5.2)
- **DONE**: Permission.java with SelectLevel/ModifyLevel enums per operation + schema field for global role permissions + `grant` boolean field (for role management capability). TABLE = PG GRANT only. ROW = PG GRANT + rls_permissions entry.
- **DONE**: ColumnAccess.java (editable/readonly/hidden lists)
- **DONE**: SelectLevel.java enum (EXISTS, RANGE, AGGREGATOR, COUNT, TABLE, ROW)
- **DONE**: ModifyLevel.java enum (TABLE, ROW)
- **DONE**: RoleInfo.java: role metadata with name, description, system flag, permissions set. **No `inherits` field** -- custom roles have no inheritance; entire access defined by permission matrix.
- **DONE**: Member.java: added enabled and roleAdmin fields
- **DONE**: Schema interface: createRole, deleteRole, grant, revoke, getRoleInfo, getRoleInfos. **No addRoleInherits/removeRoleInherits** -- custom roles have no inheritance.
- **DONE**: Database interface: createGlobalRole, deleteGlobalRole, getGlobalRoleInfos
- **DONE**: SqlSchema: implements Schema role methods, delegates to SqlRoleManager
- **DONE**: Constants: SYSTEM_ROLES set, ROLE_ADMIN, GLOBAL_SCHEMA, session variable names
- Files:
  - `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Permission.java`
  - `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/SelectLevel.java`
  - `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/ModifyLevel.java`
  - `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/ColumnAccess.java`
  - `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/RoleInfo.java`
  - `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Member.java`
  - `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Schema.java`
  - `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Database.java`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlSchema.java`

### Phase 6: GraphQL API -- IN PROGRESS
- Schema-level: `_schema { roles }` query with RoleInfo type, `change(roles)` / `drop(roles)` mutations -- DONE
- Schema-level: `change(permissions)` / `drop(permissions)` for per-table grants -- DONE
- Database-level: `_roles` query (cross-schema), `change(roles)` mutation with schemaName -- DONE
- GraphqlPermissionUtils: shared conversion logic between schema and database factories -- DONE
- Authorization added: schema role management requires Manager+ in that schema; global role management and `_roles` query require database admin -- DONE
- **v5.0 changes**: GraphQL types expose `select`/`insert`/`update`/`delete` as String ("TABLE", "ROW", or null) derived from combining PG grants with `rls_permissions` boolean flags. PermissionLevel enum is the domain/API model. -- DONE
- **v5.2 changes**: `inherits` field removed from RoleInfo GraphQL type. `grant` boolean field added to Permission GraphQL type (for role management capability). Custom roles have no inheritance -- entire access defined by permission matrix. -- DONE
- **`_schema { roles }` restricted to Manager+ or Owner only** -- same authorization as `members`. Non-managers use `_session { permissions }` to see their own effective permissions. -- DONE
- **`MolgenisPermissionType` has `schema` field** -- populated for global role permissions (to indicate which schema the table belongs to), null for schema-local permissions. -- DONE
- Introspection: `_session { permissions }` added for any authenticated user to see own effective permissions -- DONE
- Introspection: `permissionsOf(email)` removed (redundant, managers have members + roles) -- DONE
- Explicit revocation: extend `drop(permissions)` mutation with MolgenisPermissionDropInput -- NOT STARTED
- CSV endpoint: POST/GET `/<schema>/api/csv/roles` for bulk role+permission import/export -- NOT STARTED
- Role cloning: `cloneFrom` parameter on role creation to copy permissions from existing role -- NOT STARTED
- Files:
  - `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlSchemaFieldFactory.java`
  - `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlDatabaseFieldFactory.java`
  - `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlPermissionUtils.java`
  - `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlConstants.java`
  - `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlApiFactory.java`
  - `backend/molgenis-emx2-webapi/src/main/java/org/molgenis/emx2/web/CsvApi.java` (or similar)

### Phase 7a: Import/Export -- NOT STARTED
- Schema export/import with RLS config (mg_roles column, role definitions, permissions)
- Files (to modify):
  - `backend/molgenis-emx2-io/src/main/java/org/molgenis/emx2/io/emx2/Emx2.java`

### Phase 7b: Admin UI -- NOT STARTED

Permission matrix UI for role management. Admin sees a grid per custom role:

| Table | Select | Insert | Update | Delete | Grant |
|---|--------|--------|--------|--------|-------|
| * | ROW | ROW | ROW | --- | --- |
| Patients | *(ROW)* | *(ROW)* | *(ROW)* | ROW | --- |
| Diseases | TABLE | --- | --- | --- | --- |
| Samples | *(ROW)* | *(ROW)* | *(ROW)* | --- | --- |

- `*` row = schema-wide default for all RLS-enabled tables
- Per-table rows override `*` default
- Italic/grey values = inherited from `*` (not explicitly set)
- Bold values = explicit per-table override
- Cells are dropdowns: SELECT has EXISTS/RANGE/AGGREGATOR/COUNT/TABLE/ROW; INSERT/UPDATE/DELETE have TABLE/ROW; Grant has YES/NO
- Empty cell = no access (or inherited from `*`)
- Column access (editable/readonly/hidden) shown as expandable detail per table row
- Grant=YES on `*` row = role can manage other roles and members (replaces Manager inheritance)
- System roles shown as read-only reference (not editable)

**Key simplification**: Custom roles have NO inheritance. Entire access defined by the permission matrix. No 'which system role to inherit' question.

- Files:
  - Frontend files TBD (Vue components for permission matrix grid)

## Open Questions (Product Owner)

All questions from v2.3 resolved. See "Resolved" section below.

### Resolved by v2.7 PO Decisions
- **mg_status**: Out of scope for RLS. Moved to future enhancements (finegrained-future.md).
- **Time-limited access**: Out of scope. Moved to future enhancements.
- **Embargo auto-lift**: Out of scope. Moved to future enhancements.
- **Role templates**: Simplest approach: `cloneFrom` parameter on role creation + CSV import for bulk. No dedicated template system needed. Moved to future enhancements for formal template API.
- **Provenance**: Out of scope for RLS. Moved to future enhancements (finegrained-future.md). Targeting 21 CFR Part 11 compliance as separate epic.

### Resolved by v2.3 Reviews
- One role per schema: deterministic from membership, no switching (resolved ambiguity in auto-population and multi-role queries)
- CSV import format: single denormalized file for roles+permissions
- Introspection API: `_session { permissions }` for own permissions; `permissionsOf` removed (managers use members + roles)
- Explicit revocation: drop(permissions) endpoint
- Performance: pre-computed session variable lists, no per-row subqueries

### Resolved by v5.0 Simplification
- **MG_ROWLEVEL removed**: no longer needed. RLS restrictions tracked in `rls_permissions` table, pre-computed into session variable lists.
- **bypass_schemas removed**: system roles have no `rls_permissions` entries -> empty session variable lists -> unrestricted. No special sentinel values needed.
- **Hybrid storage model**: VARCHAR `select_level` for the 6 SELECT levels (EXISTS, RANGE, AGGREGATOR, COUNT, TABLE, ROW) + boolean `_rls` flags for INSERT/UPDATE/DELETE. `select_level = 'ROW'` means "this role gets row-filtered for SELECT on this table". Inline SQL resource files (`permissions_query.sql`, `permissions_explain_query.sql`) combine PG grants with rls_permissions for querying.

## Future Enhancements

See `.plan/specs/finegrained-future.md` for comprehensive list of long-term ideas including:
- Provenance / audit trail (21 CFR Part 11 compliance)
- FORCE RLS with MG_APPLICATION role
- OpenFGA / ReBAC for role hierarchy
- SMART on FHIR scopes for healthcare interop
- SCIM for multi-tenant user provisioning
- Time-limited access with auto-expiration
- Permission delegation (WITH GRANT OPTION)
- Materialized views for COUNT-only access
- GDPR-compliant provenance

## Design Decisions

See `finegrained-spec.md` for all technical design decisions and their rationale.
