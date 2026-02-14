# Fine-grained Permissions: Implementation Plan v2.6.0

## Overview

Add fine-grained permissions to MOLGENIS EMX2 so different groups can work in the same schema with controlled access over multiple schemas, per schema, per table, per row, and per column. Uses PostgreSQL native RLS policies with a Java orchestration layer (SqlRoleManager). PG catalog is the sole source of truth for access control -- table-level permissions are pure PG GRANTs queried via `has_table_privilege()`, row-level flag via `pg_has_role()`. Only column restrictions (`edit_columns`/`deny_columns`) are stored in a minimal `MOLGENIS.permission_metadata` table since PG cannot express these natively.

- Requirements: `.plan/specs/rowlevel-scenarios.md`
- Technical spec: `.plan/specs/rowlevel-spec.md`

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
    | + MG_ROWLEVEL | | (permissions) | | (row filter) |
    | + COMMENT ON  | |               | |              |
    |   ROLE        | |               | |              |
    +---------------+ +---------------+ +-------------+
```

## Phase Status

### Phase 1: Tests First -- COMPLETE
- TestSqlRoleManager: 12 tests for role CRUD (create, delete, members, grants, system protection)
- TestRowLevelSecurity: end-to-end RLS tests (will be activated in Phase 4)
- Files:
  - `backend/molgenis-emx2-sql/src/test/java/org/molgenis/emx2/sql/TestSqlRoleManager.java`
  - `backend/molgenis-emx2-sql/src/test/java/org/molgenis/emx2/sql/TestRowLevelSecurity.java`

### Phase 2: Migration -- COMPLETE
- migration31.sql: creates MG_ROWLEVEL marker role (idempotent)
- Migrations.java: bumped SOFTWARE_DATABASE_VERSION to 32
- Files:
  - `backend/molgenis-emx2-sql/src/main/resources/org/molgenis/emx2/sql/migration31.sql`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/Migrations.java`

### Phase 3: SqlRoleManager -- COMPLETE
- Central Java class for custom role management via PG catalog
- CRUD for roles, members, permissions (GRANT/REVOKE)
- System role protection (8 default roles untouchable)
- Row-level vs schema-level custom roles (MG_ROWLEVEL marker)
- **Simplification (v2.2)**: `permission_metadata` table stripped to column restrictions only (`edit_columns`, `deny_columns`). Table-level permissions (SELECT/INSERT/UPDATE/DELETE) are pure PG GRANTs, read from PG catalog via `has_table_privilege()`. Row-level flag read via `pg_has_role()`. `getPermissions()` assembles from PG catalog + merges column restrictions from metadata table. Key methods: `hasTablePrivilege()`, `isRowLevelRole()`, `mergeColumnRestrictions()`, `syncColumnRestrictions()`.
- Files:
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java`

### Phase 4: RLS Policies + Session Setup -- IN PROGRESS

#### 4a: Session Variables + Connection Safety (BLOCKING)
- Add SET LOCAL molgenis.active_role and molgenis.is_schema_level in SqlUserAwareConnectionProvider.acquire()
- SET LOCAL molgenis.bypass_select (tables where role has select=TABLE)
- SET LOCAL molgenis.bypass_modify (tables where role has insert/update/delete=TABLE)
- Role determined from member record lookup (one role per user per schema, no switching)
- Add SET ROLE failure handling: close connection on error, never return with unknown role state
- Files:
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlUserAwareConnectionProvider.java`

#### 4b: RLS Policies (BLOCKING)
- Two policies per table: SELECT policy + modify policy (INSERT/UPDATE/DELETE)
- Both have USING + WITH CHECK clauses
- Check session variables: bypass_select and bypass_modify (comma-separated table names)
- Table name embedded as literal in policy at creation time
- PermissionLevel enum: TABLE (bypass RLS), ROW (filtered), null (no grant)
- Files:
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java`
  - `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/PermissionLevel.java` (new enum)

#### 4c: mg_roles Column + Auto-Population
- mg_roles TEXT[] column with GIN index on opt-in tables
- Auto-populate from user's role on INSERT (if table has RLS)
- Reject mg_roles modification on UPDATE unless Manager+
- Files:
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTable.java`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTableMetadataExecutor.java`

#### 4d: Security Fixes
- Fix migration31.sql: GRANT SELECT (not ALL) on permission_metadata to PUBLIC
- Add orphaned mg_roles cleanup to deleteRole()
- Files:
  - `backend/molgenis-emx2-sql/src/main/resources/org/molgenis/emx2/sql/migration31.sql`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java`

### Phase 5: Domain Model -- COMPLETE
- Permission.java: per-operation PermissionLevel enum (TABLE/ROW/null) replaces boolean select/insert/update/delete + isRowLevel
- PermissionLevel.java: new enum (TABLE, ROW)
- RoleInfo.java: role metadata with name, description, system flag, permissions list
- Member.java: added enabled field for user disable/enable
- Schema interface: createRole, deleteRole, setPermission, revokePermission, getRoleInfos
- SqlSchema: implements Schema role methods, delegates to SqlRoleManager
- Files:
  - `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Permission.java`
  - `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/RoleInfo.java`
  - `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Member.java`
  - `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Schema.java`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlSchema.java`

### Phase 6: GraphQL API -- COMPLETE
- Schema-level: `_schema { roles }` query with RoleInfo type, `change(roles)` / `drop(roles)` mutations
- Schema-level: `change(permissions)` / `drop(permissions)` for per-table grants
- Database-level: `_roles` query (cross-schema), `change(roles)` mutation with schemaName
- GraphqlPermissionUtils: shared conversion logic between schema and database factories
- Authorization added: schema role management requires Manager+ in that schema; global role management and `_roles` query require database admin
- **Note (v2.2)**: GraphQL query/mutation types still expose select/insert/update/delete fields on Permission, but under the hood these are read from PG catalog (not from permission_metadata). The API contract is unchanged -- only the storage layer was simplified.
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

### Phase 4b: Provenance / Audit Trail -- NOT STARTED
- Append-only provenance table per schema (targeting 21 CFR Part 11 compliance)
- See scenarios section 3.6 for provenance table schema
- Automatic entries via triggers or application layer on: record creation, field changes, deletion, mg_roles changes
- Permissions: all roles INSERT only. No UPDATE/DELETE. Schema-level users SELECT all. Row-level users SELECT own rows.
- mg_status moved to future enhancements (out of scope for RLS)

## Open Questions (Product Owner)

All questions from v2.3 resolved. See "Resolved" section below.

### Resolved by v2.5 PO Decisions
- **mg_status**: Out of scope for RLS. Moved to future enhancements (rowlevel-future.md).
- **Time-limited access**: Out of scope. Moved to future enhancements.
- **Embargo auto-lift**: Out of scope. Moved to future enhancements.
- **Role templates**: Simplest approach: `cloneFrom` parameter on role creation + CSV import for bulk. No dedicated template system needed. Moved to future enhancements for formal template API.
- **Provenance granularity**: Every field change must be logged (targeting 21 CFR Part 11 compliance). Provenance table records: who, what, when, old value, new value, reason. Non-alterable audit trail.

### Resolved by v2.3 Reviews
- One role per schema: deterministic from membership, no switching (resolved ambiguity in auto-population and multi-role queries)
- CSV import format: single denormalized file for roles+permissions
- Introspection API: myPermissions + permissionsOf queries
- Explicit revocation: drop(permissions) endpoint
- Performance: pre-computed session variables, unified RLS policy

## Future Enhancements

See `.plan/specs/rowlevel-future.md` for comprehensive list of long-term ideas including:
- FORCE RLS with MG_APPLICATION role
- OpenFGA / ReBAC for role hierarchy
- SMART on FHIR scopes for healthcare interop
- SCIM for multi-tenant user provisioning
- Time-limited access with auto-expiration
- Permission delegation (WITH GRANT OPTION)
- Materialized views for COUNT-only access
- GDPR-compliant provenance

## Design Decisions

See `rowlevel-spec.md` for all technical design decisions and their rationale.
