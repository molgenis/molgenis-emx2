# Fine-Grained Table, Column, and Row-Level Permissions

## Context

MOLGENIS EMX2 currently has 8 fixed system roles (EXISTS through OWNER) applied uniformly at schema level. There's no way to give a user different access per table, restrict which columns they see, or filter rows by ownership. The existing `enableRowLevelSecurity()` method is a stub (`@Disabled` test).

This plan re-implements the architecture from `mswertz/rowlevel-permissions-v2` cleanly on `feat/table-row-column-perm`. Backend only (domain model, SQL, GraphQL API). No frontend.

## Architecture

Two sources of truth combined by `SqlRoleManager`:
1. **PG catalog** — table-level GRANT/REVOKE, queried via `has_table_privilege()`
2. **`MOLGENIS.rls_permissions`** — RLS flags, select level, column overrides

RLS enforcement uses 5 PostgreSQL session variables set per connection, and 4 RLS policies per table. System roles bypass RLS entirely (backward compatible).

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

---

## Phase 1: Domain Model

**New files to create:**

- `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/SelectLevel.java`
  - Enum: `EXISTS, RANGE, AGGREGATOR, COUNT, TABLE, ROW`

- `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/ModifyLevel.java`
  - Enum: `TABLE, ROW`

- `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/ColumnAccess.java`
  - Fields: `List<String> editable, readonly, hidden` with fluent setters

- `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Permission.java`
  - Fields: `String schema, String table`, `SelectLevel select`, `ModifyLevel insert/update/delete`, `ColumnAccess columnAccess`, `Boolean grant`
  - Methods: `isRevocation()`, `hasRowLevelPermissions()`, `isSchemaWide()`
  - equals/hashCode on (schema, table)

- `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/RoleInfo.java`
  - Fields: `String name, String description, boolean system, List<Permission> permissions`

**Files to modify:**

- `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Constants.java` — Add:
  - `MG_ROLES = "mg_roles"`
  - `GLOBAL_SCHEMA = "*"`
  - Session var names: `ACTIVE_ROLE`, `RLS_SELECT_TABLES`, `RLS_INSERT_TABLES`, `RLS_UPDATE_TABLES`, `RLS_DELETE_TABLES`
  - `SYSTEM_ROLES = Set.of("Exists", "Range", "Aggregator", "Count", "Viewer", "Editor", "Manager", "Owner", "Admin")`

**Verification:** Unit tests for Permission methods (isRevocation, hasRowLevelPermissions, isSchemaWide)

---

## Phase 2: Migration + SqlRoleManager

**New SQL resource files** (in `backend/molgenis-emx2-sql/src/main/resources/org/molgenis/emx2/sql/`):

- `rls_permissions_create.sql` — CREATE TABLE with PK (table_schema, role_name, table_name), columns: select_level VARCHAR, insert_rls/update_rls/delete_rls BOOLEAN, grant_permission BOOLEAN, editable_columns/readonly_columns/hidden_columns VARCHAR[]

- `migration31.sql` — Calls rls_permissions_create.sql logic, GRANT SELECT to PUBLIC, creates `MG_ROLE_*/Admin` global role

- `permissions_query.sql` — JOIN rls_permissions + pg_tables + has_table_privilege() to read effective permissions for a role in a schema

**New Java file:**

- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java` (~800 lines)
  - **Role CRUD**: `createRole()`, `deleteRole()`, `roleExists()` — PG role lifecycle with system role protection
  - **Members**: `addMember()`, `removeMember()` — PG GRANT/REVOKE role TO user
  - **Grants**: `grant(schema, role, Permission)` — PG GRANT + rls_permissions upsert with COALESCE merge semantics. Wildcard `table="*"` applies to all existing tables.
  - **Revoke**: `revoke(schema, role, Permission)` — PG REVOKE + rls_permissions cleanup
  - **RLS**: `enableRowLevelSecurity(schema, table)` — adds `mg_roles TEXT[]` + GIN index, 4 RLS policies checking session variables
  - **Cleanup**: `cleanupTablePermissions()`, `cleanupSchemaPermissions()`
  - **Query**: `getRoleInfo()`, `getRoleInfos()`, `getPermissions()` — reads from permissions_query.sql

**Files to modify:**

- `Migrations.java` — Bump `SOFTWARE_DATABASE_VERSION` to 32, add migration block for version < 32
- `SqlDatabase.java` — Add `getRoleManager()` returning `new SqlRoleManager(this)`
- `SqlSchemaMetadataExecutor.java` — In `executeDropSchema()`, call `cleanupSchemaPermissions()` before drop
- `SqlTableMetadataExecutor.java` — In `executeDropTable()`, call `cleanupTablePermissions()` before drop

**Verification:** New `TestSqlRoleManager.java` — test role CRUD, grant/revoke, RLS enable, wildcard expansion, cleanup, system role protection

---

## Phase 3: Session Variables + RLS Enforcement

**Files to modify:**

- `SqlUserAwareConnectionProvider.java` — Add 5 cached session var fields (`rlsActiveRole`, `rlsSelectTables`, etc.). In `acquire()`: execute `SET molgenis.active_role`, `SET molgenis.rls_select_tables`, etc. alongside existing `SET ROLE`. In `release()`: reset all 5 vars. Add `setRlsSessionVars()` and `clearRlsCache()`.

- `SqlDatabase.java` — Add `setRlsContext()`: resolves user's role, queries rls_permissions for RLS flags, expands wildcards via pg_class, builds comma-separated table lists, calls `connectionProvider.setRlsSessionVars()`. Add cache key (`rlsContextCacheKey`) to skip recomputation. Add `clearRlsContext()`. Wire into `DatabaseListener.onUserChange()` and `onSchemaChange()` for cache invalidation.

- `SqlTable.java` — In `insertBatch()`: if table has `mg_roles` column and row doesn't provide it, auto-populate with user's custom role. In insert/delete transaction blocks: call `setRlsContextForSchema()`.

**RLS policy pattern** (created by SqlRoleManager.enableRowLevelSecurity):
```sql
CREATE POLICY {table}_rls_select ON {schema}.{table} FOR SELECT
USING (
  '{schema}.{table}' != ALL(string_to_array(current_setting('molgenis.rls_select_tables', true), ','))
  OR mg_roles IS NULL
  OR mg_roles @> string_to_array(current_setting('molgenis.active_role', true), ',')
)
```
Logic: pass if table not in restricted list OR row is public (null) OR user's role matches.

**Verification:** Re-enable and rewrite `TestRowLevelSecurity.java` — create custom role with ROW-level access, insert rows with different mg_roles, verify filtering works. Verify system roles bypass RLS. Verify auto-population.

---

## Phase 4: Schema/Database Interface Extensions

**Files to modify:**

- `Schema.java` — Add 7 methods:
  ```java
  void createRole(String roleName, String description);
  void deleteRole(String roleName);
  void grant(String roleName, Permission permission);
  void revoke(String roleName, Permission permission);
  RoleInfo getRoleInfo(String roleName);
  List<RoleInfo> getRoleInfos();
  List<Permission> getPermissionsForActiveUser();
  ```

- `SqlSchema.java` — Implement all 7 methods. Each checks Manager+ authorization, then delegates to `SqlRoleManager`.

- `Database.java` — Add global role stubs (6 methods, for future use)

- `SqlDatabase.java` — Stub implementations throwing `UnsupportedOperationException`

**Verification:** Integration tests via Schema interface — createRole, grant, getRoleInfos, getPermissionsForActiveUser. Test authorization (non-Manager denied).

---

## Phase 5: GraphQL API

**New file:**

- `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlPermissionUtils.java`
  - `mapToPermission(Map)` — GraphQL input to Permission domain object
  - `permissionToMap(Permission)` — Permission to GraphQL output map
  - `roleInfoToMap(RoleInfo)` — RoleInfo to GraphQL output map

**Files to modify:**

- `GraphqlConstants.java` — Add: `PERMISSIONS`, `SELECT`, `INSERT`, `UPDATE`, `DELETE`, `COLUMN_ACCESS`, `EDITABLE`, `HIDDEN`, `SYSTEM`, `GRANT`

- `GraphqlSchemaFieldFactory.java`:
  - New output types: `MolgenisPermissionType` (table, select, insert, update, delete, grant, columns), `MolgenisRoleInfoType` (name, description, system, permissions)
  - Modify `_schema { roles }` to return `RoleInfo` objects (not just name strings)
  - Add `change(roles: [MolgenisRoleInput])` mutation — creates roles + applies permissions
  - Add `drop(roles: [String])` and `drop(permissions: [...])` mutations

- `GraphqlSessionFieldFactory.java`:
  - Add `permissions` field to `_session` query — calls `schema.getPermissionsForActiveUser()`

- `GraphqlDatabaseFieldFactory.java`:
  - Add `_roles` query (admin only) — cross-schema role listing

**Verification:** New `TestGraphqlRolePermissions.java`:
- Mutation: create role with permissions via GraphQL
- Query: `_schema { roles { name permissions { table select } } }`
- Query: `_session { permissions { table select } }`
- Drop: `drop(roles: ["Researcher"])`
- Authorization: non-Manager gets denied

---

## Phase Dependencies

```
Phase 1 (Domain Model)
    |
    v
Phase 2 (Migration + SqlRoleManager)
    |
    v
   +----------+
Phase 3     Phase 4  (independent, can parallelize)
   +----+-----+
        |
        v
    Phase 5 (GraphQL API)
```

## Key Design Decisions

1. **SqlRoleManager is stateless** — created fresh via `getRoleManager()`, delegates all state to SqlDatabase
2. **System roles bypass RLS** — empty session var lists = unrestricted, fully backward compatible
3. **Merge semantics** — UPSERT with COALESCE preserves existing values when granting incremental permissions
4. **mg_roles auto-populated** on INSERT for custom role users who don't provide it
5. **Wildcard `*`** in rls_permissions means "all tables in schema" — expanded when new tables are RLS-enabled