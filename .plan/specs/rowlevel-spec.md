# Row-Level Permissions: Technical Specification v1.2.0

## 1. Permission Model

### Permission class (`org.molgenis.emx2.Permission`)

Fields:
- `schema` (String) -- schema name, nullable
- `table` (String) -- table name, nullable (null = schema-wide)
- `isRowLevel` (boolean) -- whether this is a row-level permission
- `select` (Boolean) -- nullable, tri-state
- `insert` (Boolean)
- `update` (Boolean)
- `delete` (Boolean)
- `editColumns` (List\<String\>) -- allow-list: only these columns updatable
- `denyColumns` (List\<String\>) -- deny-list: these columns hidden entirely

Scope key: `(schema, table, isRowLevel)` determines equality via `equals()` / `hashCode()`.

Revocation signal: `isRevocation()` returns true when all four grant booleans are null or false AND both column lists are null.

Schema-wide: `isSchemaWide()` returns true when `table == null`.

Fluent setters return `this` for chaining.

### RoleInfo class (`org.molgenis.emx2.RoleInfo`)

Fields:
- `name` (String) -- short role name (e.g., "Viewer", "HospitalA")
- `description` (String) -- from `COMMENT ON ROLE`
- `system` (boolean) -- true = built-in system role, protected from modification/deletion
- `permissions` (List\<Permission\>) -- accumulated per-table grants for this role

Constructor: `new RoleInfo(name)` initializes with empty permissions list.

Mutator: `addPermission(Permission)` appends to permissions list.

### Member class (`org.molgenis.emx2.Member`)

Fields:
- `user` (String) -- email/username
- `role` (String) -- role name (system or custom)
- `enabled` (Boolean) -- null or true = active; false = disabled (NOLOGIN)

Equality based on `(user, role)`.

## 2. Role Management

### System roles (per schema, protected)

Eight predefined roles per schema, immutable via SqlRoleManager:

| Short Name | PG Role Name | PG Grants |
|------------|-------------|-----------|
| Exists | `MG_ROLE_<schema>/Exists` | USAGE on schema |
| Range | `MG_ROLE_<schema>/Range` | inherits Exists |
| Aggregator | `MG_ROLE_<schema>/Aggregator` | inherits Range |
| Count | `MG_ROLE_<schema>/Count` | inherits Aggregator |
| Viewer | `MG_ROLE_<schema>/Viewer` | SELECT on all tables |
| Editor | `MG_ROLE_<schema>/Editor` | INSERT/UPDATE/DELETE on all tables |
| Manager | `MG_ROLE_<schema>/Manager` | ALL + WITH ADMIN OPTION |
| Owner | `MG_ROLE_<schema>/Owner` | ALL + ADMIN |

Constants defined in `org.molgenis.emx2.Constants`:
```
ROLE_EXISTS, ROLE_RANGE, ROLE_AGGREGATOR, ROLE_COUNT,
ROLE_VIEWER, ROLE_EDITOR, ROLE_MANAGER, ROLE_OWNER
```

`SqlRoleManager.isSystemRole(roleName)` checks against `SYSTEM_ROLES` list. System roles cannot be created, modified, or deleted via the custom role API.

### Authorization

Role management operations are restricted based on scope and caller:

**Schema role operations** (`createRole`, `deleteRole`, `setPermission`, `revokePermission`):
- Requires Manager or Owner role in that schema, OR database admin
- Enforced at GraphQL mutation level (`change(roles)` / `drop(roles)` on schema endpoint)

**Global role operations** (database-level `change(roles)`):
- Requires database admin (superuser)
- Global mutations route permissions to individual schemas but require admin to invoke

**Global roles query** (`_roles` on database endpoint):
- Requires database admin
- Placed inside the admin-only query block in `GraphqlDatabaseFieldFactory`

**Schema roles query** (`_schema { roles }` on schema endpoint):
- Available to any authenticated user with access to the schema
- Members list within `_schema` still requires Manager+ (unchanged)

### Custom roles

Created via `Schema.createRole(roleName, description)`, which delegates to `SqlRoleManager.createRole(schemaName, roleName)`.

PG role name format: `MG_ROLE_<schema>/<name>` (constructed by `SqlRoleManager.fullRoleName()`).

On creation:
1. PG role created via `SqlDatabaseExecutor.executeCreateRole()` (idempotent)
2. Custom role granted membership in the schema's Exists role (USAGE on schema)
3. Custom role granted to `session_user WITH ADMIN OPTION`

Row-level tagging is NOT set during `createRole()`. Instead, `MG_ROWLEVEL` membership is automatically granted when a row-level permission is set via `setPermission()` (if `permission.isRowLevel() == true`).

### MG_ROWLEVEL marker role

Global PG role, no login, no privileges. Created by migration31.sql:
```sql
CREATE ROLE "MG_ROWLEVEL" WITH NOLOGIN;
```

Row-level custom roles are granted membership when their first row-level permission is set. Membership is revoked when their last row-level permission is revoked.

Query: `pg_has_role(role, 'MG_ROWLEVEL', 'member')` returns true for row-level roles.

Constant: `MG_ROWLEVEL` in `org.molgenis.emx2.Constants`.

### Permission storage: PG catalog as sole source of truth

**Design principle**: PostgreSQL catalog is the single source of truth for access control. The metadata table only holds what PG cannot express natively (column restrictions).

**Table-level permissions** (SELECT, INSERT, UPDATE, DELETE):
- Stored as native PG GRANTs via `GRANT <privilege> ON <table> TO <role>` / `REVOKE`
- Queried via `has_table_privilege(role, table, privilege)` -- reads directly from PG catalog
- No duplication in any metadata table

**Row-level flag**:
- Stored as PG role membership: `GRANT MG_ROWLEVEL TO <role>`
- Queried via `pg_has_role(role, 'MG_ROWLEVEL', 'member')` -- reads directly from PG catalog

**Column restrictions** (what PG cannot express natively):
- Stored in `MOLGENIS.permission_metadata` table (created by migration31.sql)

| Column | Type | Notes |
|--------|------|-------|
| table_schema | VARCHAR | NOT NULL |
| role_name | VARCHAR | NOT NULL, short name (not PG role name) |
| table_name | VARCHAR | NOT NULL |
| edit_columns | VARCHAR[] | nullable, allow-list for UPDATE |
| deny_columns | VARCHAR[] | nullable, deny-list for visibility |

Primary key: `(table_schema, role_name, table_name)`.

Upsert strategy: INSERT ... ON CONFLICT ... DO UPDATE (matching on PK).

**Permission assembly** (`getPermissions()`):
1. Reads table-level grants from PG catalog via `has_table_privilege()`
2. Reads row-level flag from PG catalog via `pg_has_role()`
3. Merges column restrictions from `MOLGENIS.permission_metadata`
4. Returns unified `Permission` objects combining all three sources

### Role info stored in PG catalog

| Info | PG Storage | Query Method |
|------|-----------|-------------|
| Role exists | `pg_roles` | `SELECT 1 FROM pg_roles WHERE rolname = ?` |
| Is row-level | `GRANT MG_ROWLEVEL TO role` | `pg_has_role(role, 'MG_ROWLEVEL', 'member')` |
| Description | `COMMENT ON ROLE` | `shobj_description(oid, 'pg_authid')` |
| Schema | Encoded in role name | Parse `MG_ROLE_<schema>/<name>` |
| Membership | `GRANT role TO user` | `pg_has_role(user, role, 'member')` |
| Table perms | `GRANT SELECT/INSERT/... ON table TO role` | `has_table_privilege(role, table, priv)` |

### Role discovery

`SqlRoleManager.getRolesForSchema(schemaName)` queries `pg_roles` WHERE `rolname LIKE 'MG_ROLE_<schema>/%'` and strips the prefix to return short names. Returns both system and custom roles.

## 3. GraphQL API Contract

### Schema-level API (per-schema endpoint)

#### Query: `_schema { roles, members }`

Output types:

**MolgenisRoleInfoType** (`outputRoleInfoType`):
```graphql
type MolgenisRoleInfoType {
  name: String
  description: String
  system: Boolean
  permissions: [MolgenisPermissionType]
}
```

**MolgenisPermissionType** (`outputPermissionType`):
```graphql
type MolgenisPermissionType {
  table: String
  rowLevel: Boolean
  select: Boolean
  insert: Boolean
  update: Boolean
  delete: Boolean
  editColumns: [String]
  denyColumns: [String]
}
```

**MolgenisMembersType** (`outputMembersMetadataType`):
```graphql
type MolgenisMembersType {
  email: String
  role: String
  enabled: Boolean
}
```

Members field is only available to users with Manager or Owner role.

Data fetching: `queryFetcher()` calls `schema.getRoleInfos()` and `schema.getMembers()`, serializes Permission fields into maps using GraphqlConstants field names.

#### Mutation: `change(roles, members)`

**Authorization:** Requires Manager or Owner role in the schema, or database admin. Unauthenticated or insufficiently privileged users receive a permission error.

**Input types:**

**MolgenisRoleInput** (`inputRoleType`):
```graphql
input MolgenisRoleInput {
  name: String
  description: String
  permissions: [MolgenisPermissionInput]
}
```

**MolgenisPermissionInput** (`inputPermissionType`):
```graphql
input MolgenisPermissionInput {
  table: String
  rowLevel: Boolean
  select: Boolean
  insert: Boolean
  update: Boolean
  delete: Boolean
  editColumns: [String]
  denyColumns: [String]
}
```

**MolgenisMembersInput** (`inputMembersMetadataType`):
```graphql
input MolgenisMembersInput {
  email: String
  role: String
  enabled: Boolean
}
```

Processing logic in `changeRoles()`:
1. For each role in input: call `schema.createRole(name, description)` (idempotent)
2. For each permission in the role: construct a Permission object
3. If `permission.isRevocation()` is true: call `schema.revokePermission(roleName, table, rowLevel)`
4. Otherwise: call `schema.setPermission(roleName, permission)`

Processing logic in `changeMembers()`:
- For each member: call `schema.addMember(email, role)`

#### Mutation: `drop(roles, members)`

**Authorization:** Requires Manager or Owner role in the schema, or database admin.

```graphql
mutation {
  drop(
    roles: [String]       # list of role names to delete
    members: [String]     # list of user emails to remove
  ) { detail }
}
```

Processing: `dropRoles()` calls `schema.deleteRole(roleName)` for each. `dropMembers()` calls `schema.removeMember(name)` for each.

### Global API (database-level endpoint)

#### Query: `_roles`

**Authorization:** Requires database admin. This query is placed inside the admin-only query block in `GraphqlDatabaseFieldFactory`, so non-admin users do not see it in the schema.

Output type:

**MolgenisGlobalRoleInfoType** (`globalOutputRoleInfoType`):
```graphql
type MolgenisGlobalRoleInfoType {
  name: String
  description: String
  permissions: [MolgenisGlobalPermissionType]
}
```

**MolgenisGlobalPermissionType** (`globalOutputPermissionType`):
```graphql
type MolgenisGlobalPermissionType {
  schemaName: String
  table: String
  rowLevel: Boolean
  select: Boolean
  insert: Boolean
  update: Boolean
  delete: Boolean
  editColumns: [String]
  denyColumns: [String]
}
```

Differences from schema-level type: includes `schemaName` field, no `system` flag.

Data fetching in `rolesQuery()`:
1. Iterates all schemas via `database.getSchemaInfos()`
2. For each schema, calls `schema.getRoleInfos()`
3. Merges roles with the same name across schemas (one RoleInfo per unique name)
4. Sets `perm.setSchema(schemaName)` on each permission before merging

#### Mutation: `change(roles)` with schemaName in permissions

**Authorization:** Requires database admin. Global role mutations are restricted to superusers because they can assign permissions across any schema.

**MolgenisGlobalRoleInput** (`globalInputRoleType`):
```graphql
input MolgenisGlobalRoleInput {
  name: String
  description: String
  permissions: [MolgenisGlobalPermissionInput]
}
```

**MolgenisGlobalPermissionInput** (`globalInputPermissionType`):
```graphql
input MolgenisGlobalPermissionInput {
  schemaName: String
  table: String
  rowLevel: Boolean
  select: Boolean
  insert: Boolean
  update: Boolean
  delete: Boolean
  editColumns: [String]
  denyColumns: [String]
}
```

Processing in `changeRoles()`:
1. Groups permissions by `schemaName`
2. For each schema group: resolves schema, calls `schema.createRole(roleName, description)`
3. Applies each permission via `schema.setPermission()` or `schema.revokePermission()`
4. Throws if `schemaName` is null on any permission

## 4. Permission Semantics

### Table-level grants

Controlled via PG `GRANT`/`REVOKE` executed by `SqlRoleManager.syncPermissionGrants()`:

- `Boolean.TRUE` on select/insert/update/delete: executes `GRANT <privilege> ON <table> TO <role>`
- `Boolean.FALSE`: executes `REVOKE <privilege> ON <table> FROM <role>`
- `null`: no change to existing grant

For schema-wide permissions (`table == null`): iterates all tables in the schema and applies grants to each.

### Column-level restrictions (app-enforced)

- `editColumns`: allow-list for UPDATE (only these columns updatable; rest view-only)
- `denyColumns`: deny-list (these columns hidden entirely in API responses)

Stored in `MOLGENIS.permission_metadata` as VARCHAR[] columns. NOT PG-enforced. Enforced at GraphQL/Java layer.

Documented residual risk: direct SQL users bypass column filtering.

### Schema-wide vs table-specific

- `table == null` in Permission (stored as empty string `""` in DB): applies to all tables in schema
- `table` set: applies to that specific table only

### Merge semantics

- Multiple permissions per role accumulate (one per scope key: schema + table + rowLevel)
- Table-level grants: `GRANT`/`REVOKE` applied directly to PG catalog (no upsert needed)
- Column restrictions: upsert via ON CONFLICT on `(table_schema, role_name, table_name)` in permission_metadata
- Revocation: revokes PG grants for that scope AND deletes column restriction rows from permission_metadata if present

### Row-level permission auto-tagging

When `setPermission()` is called with `permission.isRowLevel() == true`:
1. PG role is granted `MG_ROWLEVEL` membership
2. If `tableName != null`: `enableRowLevelSecurity()` is called on that table

When `revokePermission()` is called with `rowLevel == true`:
- After revocation, checks if any row-level permissions remain for this role
- If none remain: revokes `MG_ROWLEVEL` membership from the role

## 5. RLS Implementation (Current)

### mg_roles column

Added by `SqlRoleManager.enableRowLevelSecurity()` when triggered by a row-level permission being set.

```sql
ALTER TABLE <schema>.<table> ADD COLUMN mg_roles TEXT[] DEFAULT NULL;
CREATE INDEX IF NOT EXISTS <table>_mg_roles_idx ON <schema>.<table> USING GIN(mg_roles);
ALTER TABLE <schema>.<table> ENABLE ROW LEVEL SECURITY;
```

Idempotent: checks `information_schema.columns` before adding column.

### RLS policies (current implementation)

Four policies created per table, using `current_setting()` for session variables:

**SELECT policy** (`<table>_rls_select`):
```sql
USING (
  mg_roles IS NULL
  OR pg_has_role(current_setting('molgenis.user', true)::text, 'MG_ROWLEVEL', 'member')
  OR mg_roles && string_to_array(current_setting('molgenis.roles', true), ',')
)
```

**INSERT policy** (`<table>_rls_modify_insert`):
```sql
WITH CHECK (
  mg_roles IS NULL
  OR pg_has_role(current_setting('molgenis.user', true)::text, 'MG_ROWLEVEL', 'member')
  OR mg_roles && string_to_array(current_setting('molgenis.roles', true), ',')
)
```

**UPDATE policy** (`<table>_rls_modify_update`):
```sql
USING (
  mg_roles IS NULL
  OR pg_has_role(current_setting('molgenis.user', true)::text, 'MG_ROWLEVEL', 'member')
  OR mg_roles && string_to_array(current_setting('molgenis.roles', true), ',')
)
```

**DELETE policy** (`<table>_rls_modify_delete`):
```sql
USING (
  mg_roles IS NULL
  OR pg_has_role(current_setting('molgenis.user', true)::text, 'MG_ROWLEVEL', 'member')
  OR mg_roles && string_to_array(current_setting('molgenis.roles', true), ',')
)
```

Session variables used:
- `molgenis.user` -- current user's PG role name
- `molgenis.roles` -- comma-separated list of user's role names

Policy drops are idempotent (`DROP POLICY IF EXISTS` before create).

### ENABLE vs FORCE

Current: `ENABLE ROW LEVEL SECURITY` (table owner bypasses).

Implication: the MOLGENIS application superuser bypasses all RLS. Java always executes `SET ROLE` before user queries. FORCE would require a separate `MG_APPLICATION` role (architectural change, planned for future).

## 6. Schema Interface Methods

Defined in `org.molgenis.emx2.Schema`, implemented in `org.molgenis.emx2.sql.SqlSchema`:

```java
void createRole(String roleName, String description);
void deleteRole(String roleName);
void setPermission(String roleName, Permission permission);
void revokePermission(String roleName, String table, boolean rowLevel);
List<Permission> getPermissions(String roleName);
List<Permission> getAllPermissions();
List<RoleInfo> getRoleInfos();
```

All delegate to `SqlRoleManager` with `getName()` (schema name) prepended.

Existing member methods unchanged:
```java
void addMember(String user, String role);
List<Member> getMembers();
void removeMember(String user);
void removeMembers(List<Member> members);
List<String> getRoles();
```

## 7. SqlRoleManager API

Class: `org.molgenis.emx2.sql.SqlRoleManager`

Constructor: `SqlRoleManager(SqlDatabase database)` -- uses `database.getJooq()` for current DSL context.

### Role lifecycle

| Method | Behavior |
|--------|----------|
| `createRole(schemaName, roleName)` | Idempotent CREATE ROLE, GRANT Exists, GRANT to session_user. Rejects system roles. |
| `deleteRole(schemaName, roleName)` | Revokes all table grants, deletes permission_metadata rows, revokes all memberships, DROP ROLE. Rejects system roles and non-existent roles. |
| `roleExists(schemaName, roleName)` | Checks pg_roles for full PG role name. |
| `isSystemRole(roleName)` | Checks against SYSTEM_ROLES list. |

### Permission management

| Method | Behavior |
|--------|----------|
| `setPermission(schemaName, roleName, permission)` | Syncs PG grants via `syncPermissionGrants()`, syncs column restrictions via `syncColumnRestrictions()`, auto-tags MG_ROWLEVEL if row-level, auto-enables RLS on table if row-level + table specified. |
| `revokePermission(schemaName, roleName, tableName, rowLevel)` | Revokes all 4 PG grants, deletes column restriction rows from permission_metadata, revokes MG_ROWLEVEL if no row-level permissions remain. |
| `getPermissions(schemaName, roleName)` | Reads table-level grants from PG catalog via `hasTablePrivilege()`, reads row-level flag via `isRowLevelRole()`, merges column restrictions from permission_metadata via `mergeColumnRestrictions()`. |
| `getAllPermissions(schemaName)` | Same as `getPermissions` but for all roles in the schema. |
| `hasTablePrivilege(roleName, tableName, privilege)` | Queries `has_table_privilege(role, table, privilege)` from PG catalog. |
| `isRowLevelRole(roleName)` | Queries `pg_has_role(role, 'MG_ROWLEVEL', 'member')` from PG catalog. |
| `mergeColumnRestrictions(schemaName, roleName, permissions)` | Reads `edit_columns`/`deny_columns` from permission_metadata and merges into Permission objects. |
| `syncColumnRestrictions(schemaName, roleName, permission)` | Upserts `edit_columns`/`deny_columns` to permission_metadata; deletes row if both are null. |

### Role discovery

| Method | Behavior |
|--------|----------|
| `getRolesForSchema(schemaName)` | Queries pg_roles with prefix `MG_ROLE_<schema>/`, returns short names. |
| `getRoleInfos(schemaName)` | For each role: builds RoleInfo with system flag, description, and permissions. |

### Member management

| Method | Behavior |
|--------|----------|
| `addMember(schemaName, roleName, userName)` | Creates user if needed (`database.addUser()`), then `GRANT role TO user`. |
| `removeMember(schemaName, roleName, userName)` | `REVOKE role FROM user`. |

### Metadata

| Method | Behavior |
|--------|----------|
| `setDescription(schemaName, roleName, description)` | `COMMENT ON ROLE`. |
| `getDescription(schemaName, roleName)` | `shobj_description(oid, 'pg_authid')`. |

### RLS enablement

| Method | Behavior |
|--------|----------|
| `enableRowLevelSecurity(schemaName, tableName)` | Idempotent: adds mg_roles column, GIN index, enables RLS, creates 4 policies. |

### Static utility

```java
public static String fullRoleName(String schemaName, String roleName)
// Returns: MG_ROLE_PREFIX + schemaName + "/" + roleName
```

## 8. Constants

### `org.molgenis.emx2.Constants`

| Constant | Value |
|----------|-------|
| `MG_ROLES` | `"mg_roles"` |
| `MG_ROWLEVEL` | `"MG_ROWLEVEL"` |
| `MG_ROLE_PREFIX` | `"MG_ROLE_"` |
| `MG_USER_PREFIX` | `"MG_USER_"` |
| `ROLE_EXISTS` | `"Exists"` |
| `ROLE_RANGE` | `"Range"` |
| `ROLE_AGGREGATOR` | `"Aggregator"` |
| `ROLE_COUNT` | `"Count"` |
| `ROLE_VIEWER` | `"Viewer"` |
| `ROLE_EDITOR` | `"Editor"` |
| `ROLE_MANAGER` | `"Manager"` |
| `ROLE_OWNER` | `"Owner"` |
| `TABLE` | `"table"` |
| `DESCRIPTION` | `"description"` |
| `KEY` | `"key"` |

### `org.molgenis.emx2.graphql.GraphqlConstants`

Permission-related constants added for this feature:

| Constant | Value | Used in |
|----------|-------|---------|
| `ROW_LEVEL` | `"rowLevel"` | Permission input/output |
| `SELECT` | `"select"` | Permission input/output |
| `INSERT` | `"insert"` | Permission input/output |
| `UPDATE` | `"update"` | Permission input/output |
| `DELETE` | `"delete"` | Permission input/output |
| `EDIT_COLUMNS` | `"editColumns"` | Permission input/output |
| `DENY_COLUMNS` | `"denyColumns"` | Permission input/output |
| `PERMISSIONS` | `"permissions"` | RoleInfo input/output |
| `SYSTEM` | `"system"` | RoleInfo output |
| `ROLES` | `"roles"` | Query/mutation argument name |
| `MEMBERS` | `"members"` | Query/mutation argument name |
| `NAME` | `"name"` | RoleInfo field name |
| `ENABLED` | `"enabled"` | Member field name |
| `EMAIL` | `"email"` | Member field name |
| `SCHEMA_NAME` | `"schemaName"` | Global permission field |

## 9. Migration

### migration31.sql

Creates two artifacts:
1. `MG_ROWLEVEL` marker role (idempotent, `DO $$ IF NOT EXISTS ... $$`)
2. `MOLGENIS.permission_metadata` table for column restrictions only, with PK on `(table_schema, role_name, table_name)` and columns `(edit_columns VARCHAR[], deny_columns VARCHAR[])`, granted to PUBLIC. Table-level permissions (SELECT/INSERT/UPDATE/DELETE) are NOT stored here -- they live as native PG GRANTs queried via `has_table_privilege()`.

### Migrations.java

`SOFTWARE_DATABASE_VERSION` bumped to 32. Migration31 step executes when `version < 31`.

## 10. Security Model

### Threat model

RLS protects against authorized users exceeding their data scope (user in GroupA cannot see GroupB rows). It does NOT protect against SQL injection, superuser/DBA access, or application bugs that skip SET ROLE.

Trust boundaries: superuser and application owner role are trusted. User-level roles are untrusted and subject to RLS.

### Privilege escalation prevention

- UPDATE policies include `WITH CHECK` (planned for Pattern A/B; current implementation uses USING only)
- Application-layer validation: `SqlTable.update()` rejects mg_roles changes unless user is Manager+ (planned)
- System role protection: SqlRoleManager refuses to create/modify/delete system roles

### Role name validation

mg_roles stores role names as TEXT[] without FK to pg_roles. Mitigations:
- Application validates role names on INSERT/import
- `deleteRole()` revokes all memberships and drops the role
- Orphaned role names in mg_roles are harmless (no matching role = no access granted)

### Column restriction bypass risk

editColumns and denyColumns are enforced at application layer only. Direct SQL access bypasses these restrictions. Same residual risk as COUNT-only access restrictions.
