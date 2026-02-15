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

### Phase 1: Tests First -- DONE
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

### Phase 2: Migration -- DONE
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

### Phase 3: SqlRoleManager -- DONE
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

### Phase 4: RLS Policies + Session Setup -- DONE

**Prerequisites** (from Phase 5 gaps):
- Create ColumnAccess.java (editable/readonly/hidden) -- DONE
- Permission.java uses PermissionLevel enum (TABLE/ROW) -- DONE
- Update all references: SqlRoleManager, GraphqlPermissionUtils, tests -- DONE

#### 4a: Session Variables + Connection Safety -- DONE
- 5 session variables set in `acquire()` (session-level SET) and `release()` resets to empty -- DONE
- Cached as String fields on SqlUserAwareConnectionProvider, skip recomputation when warm -- DONE
- Inside transactions: SET LOCAL used for proper isolation via buildRlsSessionVars() -- DONE
- Pre-compute all 4 rls lists via `buildRlsSessionVars()` (SqlDatabase.java) -- DONE
- Wildcard expansion: queries pg_class for RLS-enabled tables (SqlDatabase.java) -- DONE
- System roles have no rls_permissions entries -> empty lists -> unrestricted -- DONE
- Role determined from member record lookup -- DONE
- Cache invalidation: DatabaseListener.onSchemaChange() and onUserChange() call connectionProvider.clearRlsCache() -- DONE
- Immediate cache invalidation: SqlRoleManager.grant(), revoke(), deleteRole() call clearRlsContext() + schemaChanged() -- DONE
- Test: `testRlsCacheInvalidationOnPermissionChange` verifies cross-transaction cache invalidation (ROW→TABLE→ROW cycle) -- DONE
- Files:
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlUserAwareConnectionProvider.java`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlDatabase.java`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java`

#### 4b: RLS Policies -- DONE
- Four policies per table (SELECT, INSERT, UPDATE, DELETE) each checking its own session variable -- DONE
- Pattern uses `!= ALL(string_to_array(...))` with `mg_roles @> ARRAY[active_role]` -- DONE
- UPDATE policy has both USING and WITH CHECK -- DONE
- INSERT uses only WITH CHECK, DELETE and SELECT use only USING -- DONE
- Table/schema name embedded as literals in policy at creation time -- DONE
- enableRowLevelSecurity() creates all four policies -- DONE
- Files:
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java`

#### 4c: mg_roles Column + Auto-Population + Unified Table Creation Grants -- DONE
- mg_roles TEXT[] column with GIN index on opt-in tables -- DONE
- Auto-populate from user's role on INSERT via `getUserRoleForAutoPopulate()` (SqlTable.java) -- DONE
- `applySchemaWideGrantsForNewTable()` queries wildcard entries and applies grants (SqlRoleManager.java) -- DONE
- Schema-wide setPermission with `table='*'` stores wildcard row and applies to all existing tables -- DONE
- Wildcard expansion on enableRowLevelSecurity() -- DONE
- Files:
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTable.java`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTableMetadataExecutor.java`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java`

#### 4d: Security Fixes -- DONE
- GRANT SELECT (not ALL) on rls_permissions to PUBLIC in migration31.sql -- DONE
- Orphaned mg_roles cleanup in deleteRole() via `array_remove()` -- DONE
- Files:
  - `backend/molgenis-emx2-sql/src/main/resources/org/molgenis/emx2/sql/migration31.sql`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java`

#### 4e: Cleanup on Drop -- DONE
- `cleanupTablePermissions(schemaName, tableName)` in SqlRoleManager -- DONE
- `cleanupSchemaPermissions(schemaName)` in SqlRoleManager -- DONE
- `dropTable()` hook calls cleanup BEFORE dropping (SqlTableMetadataExecutor.java) -- DONE
- `dropSchema()` hook calls cleanup BEFORE DROP SCHEMA CASCADE (SqlSchemaMetadataExecutor.java) -- DONE
- Files:
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTableMetadataExecutor.java`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlSchemaMetadataExecutor.java`

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

### Phase 6: GraphQL API -- MOSTLY DONE (3 items remaining)
- Schema-level: `_schema { roles }` query with RoleInfo type, `change(roles)` / `drop(roles)` mutations -- DONE
- Schema-level: `change(permissions)` / `drop(permissions)` for per-table grants -- DONE
- Database-level: `_roles` query (cross-schema), `change(roles)` mutation with schemaName -- DONE
- GraphqlPermissionUtils: shared conversion logic between schema and database factories -- DONE
- Authorization added: schema role management requires Manager+ in that schema; global role management and `_roles` query require database admin -- DONE
- **v5.0 changes**: GraphQL types expose `select`/`insert`/`update`/`delete` as String ("TABLE", "ROW", or null) derived from combining PG grants with `rls_permissions` boolean flags. PermissionLevel enum is the domain/API model. -- DONE
- **v5.2 changes**: `inherits` field removed from RoleInfo GraphQL type. Custom roles have no inheritance -- entire access defined by permission matrix. -- DONE
- **`_schema { roles }` restricted to Manager+ or Owner only** -- same authorization as `members`. Non-managers use `_session { permissions }` to see their own effective permissions. -- DONE
- **`MolgenisPermissionType` has `schema` field** -- populated for global role permissions (to indicate which schema the table belongs to), null for schema-local permissions. -- DONE
- Introspection: `_session { permissions }` added for any authenticated user to see own effective permissions -- DONE
- Introspection: `permissionsOf(email)` removed (redundant, managers have members + roles) -- DONE
- Explicit revocation: `drop(permissions)` with MolgenisPermissionDropInput -- PARTIALLY DONE (mechanism works, but `grant` field not exposed in GraphQL output types or mapped in GraphqlPermissionUtils)
- CSV endpoint: POST/GET `/<schema>/api/csv/roles` for bulk role+permission import/export -- NOT STARTED
- Role cloning: `cloneFrom` parameter on role creation to copy permissions from existing role -- NOT STARTED
- Files:
  - `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlSchemaFieldFactory.java`
  - `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlDatabaseFieldFactory.java`
  - `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlPermissionUtils.java`
  - `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlConstants.java`
  - `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlApiFactory.java`
  - `backend/molgenis-emx2-webapi/src/main/java/org/molgenis/emx2/web/CsvApi.java` (or similar)

## Next Steps (priority order)

1. **Phase 6 finish**: expose `grant` field in GraphQL output types + map in GraphqlPermissionUtils (small fix)
2. **Phase 6 finish**: CSV endpoint `/api/csv/roles` for bulk role+permission import/export
3. **Phase 6 finish**: role cloning `cloneFrom` parameter
4. **Phase 7a**: schema import/export with RLS config
5. **Phase 7b**: Admin UI permission matrix
6. **Optional**: harden 4a — move session vars into connection provider if non-tx DB access paths exist

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
