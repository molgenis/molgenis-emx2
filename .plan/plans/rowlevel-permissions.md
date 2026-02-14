# Row-Level Permissions: Implementation Plan v2.2.0

## Overview

Add row-level permissions to MOLGENIS EMX2 so different groups can work in the same schema with controlled access per row, per table, and per column. Uses PostgreSQL native RLS policies with a Java orchestration layer (SqlRoleManager). PG catalog is the sole source of truth for access control -- table-level permissions are pure PG GRANTs queried via `has_table_privilege()`, row-level flag via `pg_has_role()`. Only column restrictions (`edit_columns`/`deny_columns`) are stored in a minimal `MOLGENIS.permission_metadata` table since PG cannot express these natively.

- Requirements: `.plan/specs/rowlevel-scenarios.md`
- Technical spec: `.plan/specs/rowlevel-spec.md` (to be created)

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

### Phase 4: RLS Policies + mg_roles -- NOT STARTED
- mg_roles TEXT[] column with GIN index on opt-in tables
- Pattern A (public read, group write) and Pattern B (group read+write) RLS policies
- SET LOCAL helper variables (mg.is_schema_level, mg.user_roles) for per-transaction evaluation
- App-layer validation in SqlTable: mg_roles auto-population, modification control
- Files (to modify):
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTableMetadataExecutor.java`
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTable.java`

### Phase 5: Domain Model -- COMPLETE
- Permission.java: table/column permission with select/insert/update/delete + editColumns/denyColumns
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
- Files:
  - `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlSchemaFieldFactory.java`
  - `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlDatabaseFieldFactory.java`
  - `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlPermissionUtils.java`
  - `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlConstants.java`
  - `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlApiFactory.java`

### Phase 7: Import/Export + UI -- NOT STARTED
- Schema export/import with RLS config (mg_roles column, role definitions, permissions)
- Admin UI for role management
- Files (to modify):
  - `backend/molgenis-emx2-io/src/main/java/org/molgenis/emx2/io/emx2/Emx2.java`

### Phase 4b: mg_status + Provenance -- NOT STARTED (needs PO input)
- mg_status state machine (does NOT replace mg_draft)
- Append-only provenance table per schema

## Open Questions (Product Owner)

1. **mg_status scope and design**: in scope for RLS? Fixed states or configurable? Interaction with mg_draft?
2. **Time-limited access**: role description JSON + cron? Or first-class expiration field?
3. **"Import as" context**: how does a Manager import on behalf of an institute?
4. **Embargo auto-lift**: scheduled job? Trigger? Manual?
5. **Role templates**: predefined permission bundles? Or always explicit per-table?
6. **Provenance granularity**: every field change or only status/group/key fields?

## Future Enhancements

- **Permission delegation (WITH GRANT OPTION)**: allow roles to delegate their own permissions to others without Manager involvement. PG-native via `GRANT ... WITH GRANT OPTION`. Useful for large deployments (200+ groups) where Manager becomes a bottleneck. Deferred: delegation chains harder to audit/revoke, adds UI complexity.
- **FORCE ROW LEVEL SECURITY**: replace ENABLE with FORCE (requires MG_APPLICATION role architecture change)
- **User-defined role inheritance**: role A inherits role B's permissions
- **Time-limited access**: first-class expiration field on role membership

## Design Decisions

See `rowlevel-spec.md` for all technical design decisions and their rationale.
