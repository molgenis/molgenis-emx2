# Fine-grained Permissions: Implementation Plan v5.0.0

## Overview

Add fine-grained permissions to MOLGENIS EMX2 so different groups can work in the same schema with controlled access over multiple schemas, per schema, per table, per row, and per column. Uses PostgreSQL native RLS policies with a Java orchestration layer (SqlRoleManager). PG catalog stores effective access control via GRANTs queried with `has_table_privilege()`. `MOLGENIS.rls_permissions` stores RLS restriction flags (boolean) and column access overrides. `MOLGENIS.permissions_all` view combines both sources for easy querying. RLS policies use pre-computed session variable lists -- no per-row subqueries. 5 session variables: `active_role`, `rls_select_tables`, `rls_insert_tables`, `rls_update_tables`, `rls_delete_tables`. System roles have no entries in `rls_permissions`, so their lists are empty = unrestricted.

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
                    | (RLS flags +      |
                    |  column access)   |
                    +-------------------+
                              |
                    +---------v---------+
                    | permissions_all   |
                    | (unified view)    |
                    +-------------------+
```

### Unified grant management

System roles and custom roles are managed via the same mechanism:
- When a table is created, grants are applied for ALL roles (system AND custom)
- Schema-wide intent is derived from PG catalog AND `rls_permissions` (schema-wide rows with `table_name = '*'`)
- `SqlTableMetadataExecutor.createTable()` calls `SqlRoleManager.applySchemaWideGrantsForNewTable()` after applying system role grants

### Key simplification (v5.0)

- `permission_metadata` renamed to `rls_permissions` -- clearer purpose
- VARCHAR level columns (`select_level`, etc.) replaced with BOOLEAN flags (`select_rls`, etc.)
- `MG_ROWLEVEL` marker role removed -- no longer needed; RLS restrictions are tracked in `rls_permissions` and pre-computed into session variables
- `bypass_schemas` session variable removed -- system roles simply have no `rls_permissions` entries
- `permissions_all` view added -- combines PG grants with RLS flags for easy querying
- `syncPermissionMetadata()` renamed to `syncRlsPermissions()`

## Phase Status

### Phase 1: Tests First -- NEEDS UPDATE (v5.0 changes)
- TestSqlRoleManager: 10 tests for role CRUD (create, delete, members, grants, system protection)
- TestRowLevelSecurity: 8 end-to-end RLS tests (will be activated in Phase 4)
- **v5.0 changes**: Update test assertions for PermissionLevel enum (TABLE/ROW). Remove MG_ROWLEVEL references. Update session variable names (`rls_select_tables`, `rls_insert_tables`, `rls_update_tables`, `rls_delete_tables`).
- Files:
  - `backend/molgenis-emx2-sql/src/test/java/org/molgenis/emx2/sql/TestSqlRoleManager.java`
  - `backend/molgenis-emx2-sql/src/test/java/org/molgenis/emx2/sql/TestRowLevelSecurity.java`

### Phase 2: Migration -- NEEDS UPDATE (v5.0 changes)
- migration31.sql: creates `rls_permissions` table + `permissions_all` view
- **v5.0 changes**:
  - Rename table from `permission_metadata` to `rls_permissions`
  - Replace VARCHAR level columns (`select_level`, etc.) with BOOLEAN (`select_rls`, etc.)
  - Remove `MG_ROWLEVEL` marker role creation (no longer needed)
  - Add `permissions_all` view combining PG grants with RLS restrictions
  - GRANT SELECT on both table and view to PUBLIC
- Migrations.java: bumped SOFTWARE_DATABASE_VERSION to 32
- Files:
  - `backend/molgenis-emx2-sql/src/main/resources/org/molgenis/emx2/sql/migration31.sql`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/Migrations.java`

### Phase 3: SqlRoleManager -- NEEDS UPDATE (v5.0 changes)
- Central Java class for custom role management via PG catalog
- CRUD for roles, members, permissions (GRANT/REVOKE)
- System role protection (8 default roles untouchable)
- **v5.0 changes**:
  - Remove all MG_ROWLEVEL marker role logic (auto-tagging, membership checks, `isRowLevelRole()`)
  - `syncPermissionMetadata()` renamed to `syncRlsPermissions()` -- upserts boolean flags instead of VARCHAR levels
  - `getPermissions()` / `getAllPermissions()` now read from `permissions_all` view
  - `getRoleInfos()` uses `permissions_all` view
  - `rls_permissions` table referenced instead of `permission_metadata`
  - `deleteRole()` deletes from `rls_permissions` instead of `permission_metadata`
  - `enableRowLevelSecurity()` creates 4 policies per table using per-operation session variable lists (`rls_select_tables`, `rls_insert_tables`, `rls_update_tables`, `rls_delete_tables`)
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
- Pre-compute all 4 rls lists with 1 query against `rls_permissions`
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
- Pattern: `'<schema>.<table>' != ALL(string_to_array(rls_<op>_tables, ','))` -> if table NOT in list, allow all rows
- If table IS in list -> check `mg_roles @> ARRAY[active_role]`
- Table name and schema name embedded as literals in policy at creation time
- enableRowLevelSecurity() in SqlRoleManager creates both policies
- Files:
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java`

#### 4c: mg_roles Column + Auto-Population + Unified Table Creation Grants (NOT STARTED)
- mg_roles TEXT[] column with GIN index on opt-in tables
- Auto-populate from user's role on INSERT (if table has RLS)
- Reject mg_roles modification on UPDATE unless Manager+
- **Derive-from-catalog on table creation**: when `SqlTableMetadataExecutor.createTable()` creates a new table, after applying system role grants, call `SqlRoleManager.applySchemaWideGrantsForNewTable()`. This method checks what grants each custom role has on ALL existing tables via `has_table_privilege()`. If a privilege is granted on ALL existing tables, it is granted on the new table. Checks `rls_permissions` for schema-wide RLS entries and applies to new table.
- **Schema-wide setPermission**: when `setPermission()` is called with `table == null`, iterate all current tables and apply grants, store schema-wide row with `table_name = '*'` in `rls_permissions`
- Files:
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTable.java`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTableMetadataExecutor.java`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java`

#### 4d: Security Fixes (NOT STARTED)
- Fix migration31.sql: GRANT SELECT (not ALL) on `rls_permissions` and `permissions_all` to PUBLIC
- Add orphaned mg_roles cleanup to deleteRole()
- Files:
  - `backend/molgenis-emx2-sql/src/main/resources/org/molgenis/emx2/sql/migration31.sql`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java`

### Phase 5: Domain Model -- DONE
- **DONE**: Permission.java uses PermissionLevel enum (TABLE/ROW) per operation. TABLE = PG GRANT only. ROW = PG GRANT + rls_permissions entry.
- **DONE**: ColumnAccess.java (editable/readonly/hidden lists)
- **DONE**: PermissionLevel.java enum (TABLE, ROW)
- **DONE**: RoleInfo.java: role metadata with name, description, system flag, permissions list
- **DONE**: Member.java: added enabled field for user disable/enable
- **DONE**: Schema interface: createRole, deleteRole, setPermission, revokePermission, getRoleInfos
- **DONE**: SqlSchema: implements Schema role methods, delegates to SqlRoleManager
- Files:
  - `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Permission.java` (needs v5 upgrade to boolean flags)
  - `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/ColumnAccess.java` (TO CREATE)
  - `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/RoleInfo.java`
  - `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Member.java`
  - `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Schema.java`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlSchema.java`

### Phase 6: GraphQL API -- NEEDS UPDATE (v5.0 changes)
- Schema-level: `_schema { roles }` query with RoleInfo type, `change(roles)` / `drop(roles)` mutations
- Schema-level: `change(permissions)` / `drop(permissions)` for per-table grants
- Database-level: `_roles` query (cross-schema), `change(roles)` mutation with schemaName
- GraphqlPermissionUtils: shared conversion logic between schema and database factories
- Authorization added: schema role management requires Manager+ in that schema; global role management and `_roles` query require database admin
- **v5.0 changes**: GraphQL types expose `select`/`insert`/`update`/`delete` as String ("TABLE", "ROW", or null) derived from combining PG grants with `rls_permissions` boolean flags. PermissionLevel enum is the domain/API model.
- Introspection: `_schema { myPermissions }` query returning effective permissions with source role
- Introspection: `_schema { permissionsOf(email: "...") }` for Manager+ to check any user
- Explicit revocation: extend `drop(permissions)` mutation with MolgenisPermissionDropInput
- CSV endpoint: POST/GET `/<schema>/api/csv/roles` for bulk role+permission import/export
- Role cloning: `cloneFrom` parameter on role creation to copy permissions from existing role
- Files:
  - `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlSchemaFieldFactory.java`
  - `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlDatabaseFieldFactory.java`
  - `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlPermissionUtils.java`
  - `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlConstants.java`
  - `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlApiFactory.java`
  - `backend/molgenis-emx2-webapi/src/main/java/org/molgenis/emx2/web/CsvApi.java` (or similar)

### Phase 7: Import/Export + UI -- NOT STARTED
- Schema export/import with RLS config (mg_roles column, role definitions, permissions)
- Admin UI for role management
- Files (to modify):
  - `backend/molgenis-emx2-io/src/main/java/org/molgenis/emx2/io/emx2/Emx2.java`

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
- Introspection API: myPermissions + permissionsOf queries
- Explicit revocation: drop(permissions) endpoint
- Performance: pre-computed session variable lists, no per-row subqueries

### Resolved by v5.0 Simplification
- **MG_ROWLEVEL removed**: no longer needed. RLS restrictions tracked in `rls_permissions` table, pre-computed into session variable lists.
- **bypass_schemas removed**: system roles have no `rls_permissions` entries -> empty session variable lists -> unrestricted. No special sentinel values needed.
- **Boolean flags over VARCHAR levels**: simpler mental model. `select_rls = true` means "this role gets row-filtered for SELECT on this table". No need for PermissionLevel enum.
- **permissions_all view**: single source for querying effective permissions (combines PG grants + RLS flags). Powers `getPermissions()` and admin API.

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
