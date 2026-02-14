# Row-Level Permissions: Technical Specification v1.9.0

## 1. Permission Model

### Permission class (`org.molgenis.emx2.Permission`)

Fields:
- `schema` (String) -- schema name, nullable
- `table` (String) -- table name, nullable (null = schema-wide)
- `select` (PermissionLevel) -- nullable enum: TABLE, ROW, or null
- `insert` (PermissionLevel) -- nullable enum: TABLE, ROW, or null
- `update` (PermissionLevel) -- nullable enum: TABLE, ROW, or null
- `delete` (PermissionLevel) -- nullable enum: TABLE, ROW, or null
- `columnAccess` (ColumnAccess) -- per-column access overrides, nullable

### ColumnAccess class (`org.molgenis.emx2.ColumnAccess`)

Fields:
- `editable` (List\<String\>) -- columns that are editable (visible + updatable)
- `readonly` (List\<String\>) -- columns that are read-only (visible but not updatable)
- `hidden` (List\<String\>) -- columns that are hidden (not visible in API responses)

All three lists are nullable (null = no overrides for that level).

Unlisted columns inherit the default from table-level permission:
- If table has `update = TABLE or ROW`: unlisted columns are EDITABLE
- If table has only `select`: unlisted columns are VIEW (read-only)
- Column EDITABLE with table `update = null`: implies targeted column-level UPDATE grant

Column access is bounded by table-level permission: EDITABLE requires at least `update = null` with column-level EDITABLE listed.

### PermissionLevel enum (`org.molgenis.emx2.PermissionLevel`)

- `TABLE` -- operation granted, bypasses RLS (sees/modifies all rows)
- `ROW` -- operation granted, filtered by RLS (only rows with user's role in mg_roles)

Null means no grant for that operation.

Scope key: `(schema, table)` -- one permission record per role per table (was `(schema, table, isRowLevel)` in v1.4).

Revocation signal: `isRevocation()` returns true when all four operation fields are null AND columnAccess is null.

A role is considered row-level (`hasRowLevelPermissions()`) if ANY of its permissions has at least one ROW-level operation. Row-level roles are tagged with MG_ROWLEVEL marker.

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

### One Role Per Schema

Each user has exactly one role per schema, assigned via `addMember(email, role)`. This role is either a system role (Viewer, Editor, Manager, Owner, etc.) or a custom role (HospitalA, Researcher, etc.).

Role determination per request:
- User accesses `/<schema>/api/...`
- Application looks up member record: `SELECT role FROM members WHERE user = ?`
- If system role: is_schema_level = true, bypasses RLS
- If custom role: is_schema_level = false, filtered by RLS policies

Session setup in SqlUserAwareConnectionProvider.acquire():
```sql
SET LOCAL molgenis.active_role = '<user_role_short_name>';
SET LOCAL molgenis.is_schema_level = '<true|false>';
SET LOCAL molgenis.bypass_select = '<comma_separated_table_names>';
SET LOCAL molgenis.bypass_modify = '<comma_separated_table_names>';
```

No role switching API needed. Role is deterministic from membership.

If a user needs both cross-group visibility (Viewer) and data entry as HospitalA: use two separate accounts. This is standard practice in healthcare data management.

Global roles: same role name (e.g., "HospitalA") created independently in multiple schemas via the database-level API. The global API is a bulk convenience -- it iterates schemas, not a separate role concept. A user's role in each schema is still determined by their per-schema membership.

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

Row-level tagging is NOT set during `createRole()`. Instead, `MG_ROWLEVEL` membership is automatically granted when a row-level permission is set via `setPermission()` (if the permission has any ROW-level operations).

### MG_ROWLEVEL marker role

Global PG role, no login, no privileges. Created by migration31.sql:
```sql
CREATE ROLE "MG_ROWLEVEL" WITH NOLOGIN;
```

Row-level custom roles are granted membership when their first ROW-level permission is set. Membership is revoked when all permissions for the role are revoked or changed to TABLE-only.

Query: `pg_has_role(role, 'MG_ROWLEVEL', 'member')` returns true for row-level roles.

Constant: `MG_ROWLEVEL` in `org.molgenis.emx2.Constants`.

### Permission storage: PG catalog as sole source of truth

**Design principle**: PostgreSQL catalog is the single source of truth for access control. The metadata table only holds what PG cannot express natively (column restrictions).

**Table-level permissions** (SELECT, INSERT, UPDATE, DELETE):
- Stored as native PG GRANTs via `GRANT <privilege> ON <table> TO <role>` / `REVOKE`
- Queried via `has_table_privilege(role, table, privilege)` -- reads directly from PG catalog
- No duplication in any metadata table

**Row-level flag** (derived from having any ROW-level operations):
- Stored as PG role membership: `GRANT MG_ROWLEVEL TO <role>`
- Queried via `pg_has_role(role, 'MG_ROWLEVEL', 'member')` -- reads directly from PG catalog

**Column restrictions** (what PG cannot express natively):
- Stored in `MOLGENIS.permission_metadata` table (created by migration31.sql)

| Column | Type | Notes |
|--------|------|-------|
| role_name | VARCHAR | NOT NULL, short name (not PG role name) |
| table_schema | VARCHAR | NOT NULL |
| table_name | VARCHAR | NOT NULL |
| editable_columns | VARCHAR[] | nullable, columns with EDITABLE access |
| readonly_columns | VARCHAR[] | nullable, columns with READ-ONLY access |
| hidden_columns | VARCHAR[] | nullable, columns that are HIDDEN |

Primary key: `(role_name, table_schema, table_name)`.

Upsert strategy: INSERT ... ON CONFLICT ... DO UPDATE (matching on PK).

**Permission assembly** (`getPermissions()`):
1. Reads table-level grants from PG catalog via `has_table_privilege()`
2. Reads row-level flag from PG catalog via `pg_has_role()` to determine TABLE vs ROW level
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
  select: PermissionLevel
  insert: PermissionLevel
  update: PermissionLevel
  delete: PermissionLevel
  columns: MolgenisColumnAccessType
}

type MolgenisColumnAccessType {
  editable: [String]
  readonly: [String]
  hidden: [String]
}

enum PermissionLevel { TABLE ROW }
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
  select: PermissionLevel
  insert: PermissionLevel
  update: PermissionLevel
  delete: PermissionLevel
  columns: MolgenisColumnAccessInput
}

input MolgenisColumnAccessInput {
  editable: [String]
  readonly: [String]
  hidden: [String]
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
3. If `permission.isRevocation()` is true: call `schema.revokePermission(roleName, table)`
4. Otherwise: call `schema.setPermission(roleName, permission)`

Processing logic in `changeMembers()`:
- For each member: call `schema.addMember(email, role)`

#### Mutation: `drop(roles, members, permissions)`

**Authorization:** Requires Manager or Owner role in the schema, or database admin.

```graphql
mutation {
  drop(
    roles: [String]
    members: [String]
    permissions: [MolgenisPermissionDropInput]
  ) { detail }
}
```

**MolgenisPermissionDropInput**:
```graphql
input MolgenisPermissionDropInput {
  role: String!
  table: String         # null = revoke all permissions for this role in this schema
}
```

Processing: `dropRoles()` calls `schema.deleteRole(roleName)` for each. `dropMembers()` calls `schema.removeMember(name)` for each. `dropPermissions()` calls `schema.revokePermission(roleName, table)` for each.

### CSV Import: roles endpoint

POST to `/<schema>/api/csv/roles` with CSV body:

```csv
role,description,table,select,insert,update,delete,editable,readonly,hidden
HospitalA,Hospital A staff,Patients,ROW,ROW,ROW,,,,ssn
HospitalA,Hospital A staff,Samples,ROW,ROW,,,,,
DataMonitor,Read-only monitor,Patients,TABLE,,,,,,
Researcher,Read-only researcher,Patients,ROW,,,,name;dob,,ssn
Publisher,,Patients,TABLE,,ROW,,,mg_status,,
```

Column lists use semicolon separator within each field. Empty = no overrides.

Processing:
1. Group rows by role name
2. Create roles (idempotent)
3. Set permissions per table
4. Description taken from first occurrence per role

Export: GET `/<schema>/api/csv/roles` returns same format.

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
  select: PermissionLevel
  insert: PermissionLevel
  update: PermissionLevel
  delete: PermissionLevel
  columns: MolgenisColumnAccessType
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
  select: PermissionLevel
  insert: PermissionLevel
  update: PermissionLevel
  delete: PermissionLevel
  columns: MolgenisColumnAccessInput
}
```

Processing in `changeRoles()`:
1. Groups permissions by `schemaName`
2. For each schema group: resolves schema, calls `schema.createRole(roleName, description)`
3. Applies each permission via `schema.setPermission()` or `schema.revokePermission()`
4. Throws if `schemaName` is null on any permission

## 3b. Introspection API

### Schema-level: `_schema { myPermissions }`

Returns effective permissions for the current user, with source role information.

**MolgenisEffectivePermissionType**:
```graphql
type MolgenisEffectivePermissionType {
  table: String
  select: PermissionLevel
  insert: PermissionLevel
  update: PermissionLevel
  delete: PermissionLevel
  columns: MolgenisColumnAccessType
  sourceRole: String
}
```

Available to any authenticated user (shows own permissions only). Manager+ can query for any user via `_schema { permissionsOf(email: "user@example.com") }`.

## 4. Permission Semantics

### Operation grants

Controlled via PG `GRANT`/`REVOKE` executed by `SqlRoleManager.syncPermissionGrants()`:

- `PermissionLevel.TABLE`: executes `GRANT <privilege> ON <table> TO <role>` (bypasses RLS for this operation)
- `PermissionLevel.ROW`: executes `GRANT <privilege> ON <table> TO <role>` (filtered by RLS for this operation)
- `null`: no change to existing grant (on revocation: executes `REVOKE <privilege>`)

Both TABLE and ROW result in the same PG GRANT. The difference is in session variable computation:
- TABLE operations add the table to the bypass list (molgenis.bypass_select or molgenis.bypass_modify)
- ROW operations do not add to bypass lists, so RLS policies filter rows

For schema-wide permissions (`table == null`): iterates all tables in the schema and applies grants to each.

### Column-level access (app-enforced)

Per-column access control via `columns` field on Permission, containing three string arrays:

- `editable: [String]` -- these columns are editable (overrides default to EDITABLE)
- `readonly: [String]` -- these columns are read-only (overrides default to VIEW)
- `hidden: [String]` -- these columns are hidden entirely

Unlisted columns inherit the default from table-level permission:
- Table has `update = TABLE/ROW`: unlisted columns default to EDITABLE
- Table has `update = null` (no update grant): unlisted columns default to VIEW
- Column in `editable` list with `update = null`: implies targeted column UPDATE grant

Examples:
| update | columns | Effect |
|--------|---------|--------|
| ROW | `{ hidden: ["ssn"] }` | All editable, SSN hidden |
| null | `{ editable: ["name", "dob"] }` | All view-only, name/dob editable |
| ROW | `{ readonly: ["address"], hidden: ["ssn"] }` | All editable, address read-only, SSN hidden |
| null | `{ editable: ["name", "dob"], hidden: ["ssn"] }` | Name/dob editable, SSN hidden, rest view-only |
| ROW | (null) | All editable (default) |
| null | (null) | All view-only (default) |

Enforced at GraphQL/Java layer. Documented residual risk: direct SQL users bypass column filtering.

### mg_roles auto-population on INSERT

In SqlTable.insertBatch(), before values are assembled:
- If table has RLS enabled AND row does not have mg_roles set:
  - Set mg_roles = ARRAY[user_role] (user's role from member record)
  - If user has a schema-level role: leave mg_roles as NULL
- If row has mg_roles explicitly set: validate that user is Manager+ (reject otherwise)

### mg_roles modification control on UPDATE

In SqlTable.updateBatch(), before UPDATE:
- If row includes mg_roles changes: reject unless user has Manager or Owner role
- Defense in depth: WITH CHECK clause in RLS policy also prevents row-level users from modifying mg_roles to invalid values

### Schema-wide vs table-specific

- `table == null` in Permission (stored as empty string `""` in DB): applies to all tables in schema
- `table` set: applies to that specific table only

### Merge semantics

- Multiple permissions per role accumulate (one per scope key: schema + table)
- Table-level grants: `GRANT`/`REVOKE` applied directly to PG catalog (no upsert needed)
- Column restrictions: upsert via ON CONFLICT on `(table_schema, role_name, table_name)` in permission_metadata (editable_columns, readonly_columns, hidden_columns VARCHAR[])
- Revocation: revokes PG grants for that scope AND deletes column restriction rows from permission_metadata if present

### Row-level role auto-tagging

When `setPermission()` is called and the permission has any ROW-level operations:
1. PG role is granted `MG_ROWLEVEL` membership (if not already)
2. `enableRowLevelSecurity()` is called on the table (adds mg_roles column, GIN index, policies)

When all permissions for a role are revoked or changed to TABLE-only:
- MG_ROWLEVEL membership is revoked from the role

## 5. RLS Implementation (Current)

### mg_roles column

Added by `SqlRoleManager.enableRowLevelSecurity()` when triggered by a row-level permission being set.

```sql
ALTER TABLE <schema>.<table> ADD COLUMN mg_roles TEXT[] DEFAULT NULL;
CREATE INDEX IF NOT EXISTS <table>_mg_roles_idx ON <schema>.<table> USING GIN(mg_roles);
ALTER TABLE <schema>.<table> ENABLE ROW LEVEL SECURITY;
```

Idempotent: checks `information_schema.columns` before adding column.

### RLS policies (v1.5 - per-operation bypass)

Two policies per table: one for SELECT, one for modifications (INSERT/UPDATE/DELETE).

Session variables set once per transaction in SqlUserAwareConnectionProvider.acquire():
- `molgenis.active_role` -- user's role short name in this schema
- `molgenis.is_schema_level` -- true if user has a system role (bypasses all RLS)
- `molgenis.bypass_select` -- comma-separated table names where user's role has select=TABLE
- `molgenis.bypass_modify` -- comma-separated table names where user's role has insert/update/delete=TABLE

**SELECT policy** (`<table>_rls_select`):
```sql
CREATE POLICY <table>_rls_select ON <schema>.<table> FOR SELECT
  USING (
    mg_roles IS NULL
    OR current_setting('molgenis.is_schema_level', true)::boolean
    OR '<table>' = ANY(string_to_array(current_setting('molgenis.bypass_select', true), ','))
    OR mg_roles @> ARRAY[current_setting('molgenis.active_role', true)]
  )
```

**Modify policy** (`<table>_rls_modify`):
```sql
CREATE POLICY <table>_rls_modify ON <schema>.<table>
  USING (
    mg_roles IS NULL
    OR current_setting('molgenis.is_schema_level', true)::boolean
    OR '<table>' = ANY(string_to_array(current_setting('molgenis.bypass_modify', true), ','))
    OR mg_roles @> ARRAY[current_setting('molgenis.active_role', true)]
  )
  WITH CHECK (
    mg_roles IS NULL
    OR current_setting('molgenis.is_schema_level', true)::boolean
    OR '<table>' = ANY(string_to_array(current_setting('molgenis.bypass_modify', true), ','))
    OR mg_roles @> ARRAY[current_setting('molgenis.active_role', true)]
  )
```

Table name is embedded as a string literal at policy creation time (each table gets its own policies).

Performance: all session variables computed once per transaction. `string_to_array()` on a short comma-separated list is negligible. `@>` uses GIN index. Estimated overhead: <5%.

Policy drops are idempotent (`DROP POLICY IF EXISTS` before create).

### Session variable lifecycle

Set in SqlUserAwareConnectionProvider.acquire(), after SET ROLE:

For authenticated users:
```sql
RESET ROLE;
SET jit = 'off';
SET ROLE MG_USER_<username>;
SET LOCAL molgenis.active_role = '<user_role_short_name>';
SET LOCAL molgenis.is_schema_level = '<true|false>';
SET LOCAL molgenis.bypass_select = '<comma_separated_table_names>';
SET LOCAL molgenis.bypass_modify = '<comma_separated_table_names>';
```

Bypass lists computed from role's permissions:
- `bypass_select`: tables where the role has `select = TABLE`
- `bypass_modify`: tables where the role has ANY of `insert/update/delete = TABLE`
- Schema-wide TABLE permission (table=null): all tables added to bypass list

For unauthenticated / admin:
```sql
RESET ROLE;
SET jit = 'off';
SET LOCAL molgenis.active_role = '';
SET LOCAL molgenis.is_schema_level = 'true';
SET LOCAL molgenis.bypass_select = '';
SET LOCAL molgenis.bypass_modify = '';
```

SET LOCAL scope: transaction-only, auto-resets on COMMIT/ROLLBACK. Safe for HikariCP connection pooling.

Error handling: if SET ROLE fails, close connection (force HikariCP discard) and throw MolgenisException. Never return connection with unknown role state.

### ENABLE vs FORCE

Current: `ENABLE ROW LEVEL SECURITY` (table owner bypasses).

Implication: the MOLGENIS application superuser bypasses all RLS. Java always executes `SET ROLE` before user queries. FORCE would require a separate `MG_APPLICATION` role (architectural change, planned for future).

## 6. Schema Interface Methods

Defined in `org.molgenis.emx2.Schema`, implemented in `org.molgenis.emx2.sql.SqlSchema`:

```java
void createRole(String roleName, String description);
void deleteRole(String roleName);
void setPermission(String roleName, Permission permission);
void revokePermission(String roleName, String table);
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
| `deleteRole(schemaName, roleName)` | Revokes all table grants, deletes permission_metadata rows, cleans orphaned mg_roles entries, revokes all memberships, DROP ROLE. Rejects system roles and non-existent roles. |
| `roleExists(schemaName, roleName)` | Checks pg_roles for full PG role name. |
| `isSystemRole(roleName)` | Checks against SYSTEM_ROLES list. |

### Orphaned mg_roles cleanup on deleteRole

On deleteRole, BEFORE dropping role:
- For each table with mg_roles column in schema:
  UPDATE <table> SET mg_roles = array_remove(mg_roles, '<fullRoleName>')
- This prevents role re-creation attack (old mg_roles entries reactivating)

### Permission management

| Method | Behavior |
|--------|----------|
| `setPermission(schemaName, roleName, permission)` | Syncs PG grants via `syncPermissionGrants()`, syncs column restrictions via `syncColumnRestrictions()`, auto-tags MG_ROWLEVEL if any ROW-level operations, auto-enables RLS on table if ROW-level + table specified. |
| `revokePermission(schemaName, roleName, tableName)` | Revokes all 4 PG grants, deletes column restriction rows from permission_metadata, revokes MG_ROWLEVEL if no ROW-level permissions remain. |
| `getPermissions(schemaName, roleName)` | Reads table-level grants from PG catalog via `hasTablePrivilege()`, reads row-level flag via `isRowLevelRole()`, merges column restrictions from permission_metadata via `mergeColumnRestrictions()`. |
| `getAllPermissions(schemaName)` | Same as `getPermissions` but for all roles in the schema. |
| `hasTablePrivilege(roleName, tableName, privilege)` | Queries `has_table_privilege(role, table, privilege)` from PG catalog. |
| `isRowLevelRole(roleName)` | Queries `pg_has_role(role, 'MG_ROWLEVEL', 'member')` from PG catalog. |
| `mergeColumnRestrictions(schemaName, roleName, permissions)` | Reads `editable_columns`, `readonly_columns`, `hidden_columns` from permission_metadata and builds ColumnAccess into Permission. |
| `syncColumnRestrictions(schemaName, roleName, permission)` | Upserts `editable_columns`, `readonly_columns`, `hidden_columns` VARCHAR[] to permission_metadata; deletes row if all three are null. |

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
| `enableRowLevelSecurity(schemaName, tableName)` | Idempotent: adds mg_roles column, GIN index, enables RLS, creates SELECT and modify policies. |

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
| `BYPASS_SELECT` | `"bypass_select"` |
| `BYPASS_MODIFY` | `"bypass_modify"` |
| `ACTIVE_ROLE` | `"active_role"` |
| `IS_SCHEMA_LEVEL` | `"is_schema_level"` |

### `org.molgenis.emx2.graphql.GraphqlConstants`

Permission-related constants added for this feature:

| Constant | Value | Used in |
|----------|-------|---------|
| `SELECT` | `"select"` | Permission input/output |
| `INSERT` | `"insert"` | Permission input/output |
| `UPDATE` | `"update"` | Permission input/output |
| `DELETE` | `"delete"` | Permission input/output |
| `COLUMNS` | `"columns"` | Permission field (ColumnAccess object) |
| `EDITABLE` | `"editable"` | ColumnAccess field |
| `READONLY_FIELD` | `"readonly"` | ColumnAccess field |
| `HIDDEN` | `"hidden"` | ColumnAccess field |
| `PERMISSIONS` | `"permissions"` | RoleInfo input/output |
| `SYSTEM` | `"system"` | RoleInfo output |
| `ROLES` | `"roles"` | Query/mutation argument name |
| `MEMBERS` | `"members"` | Query/mutation argument name |
| `NAME` | `"name"` | RoleInfo field name |
| `ENABLED` | `"enabled"` | Member field name |
| `EMAIL` | `"email"` | Member field name |
| `SCHEMA_NAME` | `"schemaName"` | Global permission field |
| `PERMISSION_LEVEL_TABLE` | `"TABLE"` | PermissionLevel enum value |
| `PERMISSION_LEVEL_ROW` | `"ROW"` | PermissionLevel enum value |

## 9. Migration

### migration31.sql

Creates two artifacts:
1. `MG_ROWLEVEL` marker role (idempotent, `DO $$ IF NOT EXISTS ... $$`)
2. `MOLGENIS.permission_metadata` table for column restrictions only, with PK on `(table_schema, role_name, table_name)` and columns `(editable_columns VARCHAR[], readonly_columns VARCHAR[], hidden_columns VARCHAR[])`. Table-level permissions (SELECT/INSERT/UPDATE/DELETE) are NOT stored here -- they live as native PG GRANTs queried via `has_table_privilege()`.

```sql
GRANT SELECT ON "MOLGENIS"."permission_metadata" TO PUBLIC;
```

SELECT-only grant (not GRANT ALL) prevents metadata tampering by non-admin users.

### Migrations.java

`SOFTWARE_DATABASE_VERSION` bumped to 32. Migration31 step executes when `version < 31`.

## 10. Security Model

### Threat model

RLS protects against authorized users exceeding their data scope (user in GroupA cannot see GroupB rows). It does NOT protect against SQL injection, superuser/DBA access, or application bugs that skip SET ROLE.

Trust boundaries: superuser and application owner role are trusted. User-level roles are untrusted and subject to RLS.

### Privilege escalation prevention

- Modify RLS policy includes `WITH CHECK` clause (prevents INSERT/UPDATE of rows with unauthorized mg_roles)
- Application-layer validation: `SqlTable.insertBatch()` / `SqlTable.updateBatch()` reject mg_roles changes unless user is Manager+
- System role protection: SqlRoleManager refuses to create/modify/delete system roles
- Orphaned mg_roles cleanup on role deletion prevents role re-creation attack

### Role name validation

mg_roles stores role names as TEXT[] without FK to pg_roles. Mitigations:
- Application validates role names on INSERT/import
- `deleteRole()` cleans orphaned mg_roles entries, revokes all memberships, and drops the role
- Residual orphaned role names in mg_roles are harmless (no matching role = no access granted)

### Column restriction bypass risk

Column access (ColumnAccess editable/readonly/hidden lists) is enforced at application layer only. Direct SQL access bypasses these restrictions. Same residual risk as COUNT-only access restrictions.

## 11. Design Decisions

See `rowlevel-scenarios.md` "Design decisions" section for all resolved design decisions.
