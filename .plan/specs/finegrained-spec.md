# Fine-grained Permissions: Technical Specification v4.0.0

**Design principle**: PG catalog for what PG can express (grants, role membership). `rls_permissions` table for RLS restriction flags and column access overrides. `SqlRoleManager` combines both sources via query. RLS policies use session variable lists (pre-computed at tx start) -- no per-row subqueries.

**Model separation** -- three layers, never mix:
1. **Domain model** (Java classes): `Permission`, `SelectLevel`, `ModifyLevel`, `ColumnAccess`, `RoleInfo`. Clean enums and typed fields. This is what application code works with.
2. **Storage model** (PostgreSQL): `rls_permissions` table with VARCHAR `select_level` + boolean `*_rls` flags. PG catalog for grants. This is what the database stores.
3. **API model** (GraphQL/CSV): String representations of the domain enums. This is what external clients see.

Mapping between layers happens in `SqlRoleManager` (domain ↔ storage) and inline in GraphQL factories (domain ↔ API via `enum.valueOf()`/`enum.name()`). Never leak storage types into domain or API types.

## 1. Permission Model

### Permission class (`org.molgenis.emx2.Permission`) -- DOMAIN MODEL

Single class for both schema-local and global roles.

Fields:
- `schema` (String) -- schema name, required
- `table` (String) -- table name, required. Use `"*"` for schema-wide default (applies to all RLS-enabled tables)
- `select` (SelectLevel) -- EXISTS, RANGE, AGGREGATOR, COUNT, TABLE, ROW, or null
- `insert` (ModifyLevel) -- TABLE, ROW, or null
- `update` (ModifyLevel) -- TABLE, ROW, or null
- `delete` (ModifyLevel) -- TABLE, ROW, or null
- `columnAccess` (ColumnAccess) -- per-column access overrides, nullable
- `grant` (Boolean) -- true = can manage roles, members, and permissions in this schema. Only meaningful on `table="*"` entries.

Helpers:
- `hasRowLevelPermissions()` -- true if select is ROW or any modify is ROW

Schema-wide access for custom roles is expressed via `table="*"` in Permission. System roles use PG role inheritance instead.

Usage patterns:

**Schema-local role — per-table grant**:
```java
schema.grant("Researcher", new Permission()
    .setTable("Patients").setSelect(SelectLevel.ROW).setInsert(ModifyLevel.ROW));
```

**Global role — inherit system role in a schema** (separate from Permission):
```java
database.addGlobalRoleInherits("HospitalA", "CohortA", "Editor");
```

**Global role — per-table RLS override** (via Schema.grant, same as schema-local):
```java
schema.grant("HospitalA", new Permission()
    .setTable("Patients").setSelect(SelectLevel.ROW).setInsert(ModifyLevel.ROW));
```

Used in:
- `Schema.grant(String roleName, Permission permission)` -- grant per-table permissions to a role
- `Schema.revoke(String roleName, Permission permission)` -- revoke specific permissions from a role
- `RoleInfo.getPermissions()` -- returns `Set<Permission>` for this role
- `SqlRoleManager.setPermission(schemaName, roleName, permission)` -- maps domain → storage
- `SqlRoleManager.revokePermission(schemaName, roleName, permission)` -- removes grants + rls_permissions

Note: `inherits` (role inheritance for global roles) is managed via `Database.addGlobalRoleInherits()`, NOT via Permission. See "Global role grant model" in section 3.

### SelectLevel enum (`org.molgenis.emx2.SelectLevel`) -- DOMAIN MODEL

Ordered from least to most access:

| Value | Meaning |
|-------|---------|
| `EXISTS` | schema existence only (app-enforced) |
| `RANGE` | min/max ranges only (app-enforced) |
| `AGGREGATOR` | aggregate functions only (app-enforced) |
| `COUNT` | row counts only (app-enforced) |
| `TABLE` | full SELECT, all rows |
| `ROW` | full SELECT, filtered by mg_roles |

- `null` = no access

Used in:
- `Permission.getSelect()` / `Permission.setSelect(SelectLevel)` -- the select field type
- `SelectLevel.valueOf(String)` -- parsing GraphQL/CSV string input to enum
- `SqlRoleManager.syncRlsPermissions()` -- maps to VARCHAR `select_level`
- `SqlRoleManager.getRoleInfo()` -- maps from VARCHAR back to SelectLevel

### ModifyLevel enum (`org.molgenis.emx2.ModifyLevel`) -- DOMAIN MODEL

| Value | Meaning |
|-------|---------|
| `TABLE` | all rows |
| `ROW` | filtered by mg_roles |

- `null` = no access

Used in:
- `Permission.getInsert/getUpdate/getDelete()` -- the insert/update/delete field type
- `ModifyLevel.valueOf(String)` -- parsing GraphQL/CSV string input to enum
- `SqlRoleManager.syncRlsPermissions()` -- maps ROW → `true`, TABLE → `false` for `*_rls` booleans
- `SqlRoleManager.getRoleInfo()` -- maps from boolean back to ModifyLevel

### Domain ↔ Storage mapping (in SqlRoleManager)

**Writing** (`setPermission()`):

For schema-local roles (direct per-operation grants):

| Domain | PG GRANT | rls_permissions |
|--------|----------|-----------------|
| `select = EXISTS..COUNT` | none | `select_level = '<value>'` |
| `select = TABLE` | GRANT SELECT | `select_level = 'TABLE'` (or no entry) |
| `select = ROW` | GRANT SELECT | `select_level = 'ROW'` |
| `insert = TABLE` | GRANT INSERT | `insert_rls = false` (or no entry) |
| `insert = ROW` | GRANT INSERT | `insert_rls = true` |
| (same pattern for update, delete) | | |
| `grant = true` | none | `grant_permission = true` |
| `grant = false/null` | none | `grant_permission = null` (or no entry) |

For global roles (inherit one role per schema, via `Database.addGlobalRoleInherits()`):

| Domain | PG action |
|--------|-----------|
| `addGlobalRoleInherits("HospA", "CohortA", "Editor")` | `GRANT MG_ROLE_CohortA/Editor TO MG_ROLE_*/HospA` |
| `addGlobalRoleInherits("HospA", "CohortA", "Researcher")` | `GRANT MG_ROLE_CohortA/Researcher TO MG_ROLE_*/HospA` |
| per-table RLS overrides (via `Schema.grant()`) | Same as schema-local (rls_permissions entries) |

**Reading** (`getRoleInfo()` query in SqlRoleManager):

| can_select | select_level | → Domain |
|------------|-------------|----------|
| true | NULL | `SelectLevel.TABLE` |
| true | 'TABLE' | `SelectLevel.TABLE` |
| true | 'ROW' | `SelectLevel.ROW` |
| false | 'COUNT' | `SelectLevel.COUNT` |
| false | NULL | `null` |

| can_insert | insert_rls | → Domain |
|------------|-----------|----------|
| true | false | `ModifyLevel.TABLE` |
| true | true | `ModifyLevel.ROW` |
| false | * | `null` |

| grant_permission | → Domain |
|-----------------|----------|
| true | `grant = true` |
| false/NULL | `grant = null` |


### ColumnAccess class (`org.molgenis.emx2.ColumnAccess`) -- DOMAIN MODEL

Fields:
- `editable` (List\<String\>) -- columns that are editable (visible + updatable)
- `readonly` (List\<String\>) -- columns that are read-only (visible but not updatable)
- `hidden` (List\<String\>) -- columns that are hidden (not visible in API responses)

All three lists are nullable (null = no overrides for that level).

Unlisted columns inherit the default from table-level permission:
- If table has UPDATE grant: unlisted columns are EDITABLE
- If table has only SELECT: unlisted columns are VIEW (read-only)

Used in:
- `Permission.getColumnAccess()` / `Permission.setColumnAccess(ColumnAccess)` -- nested in Permission
- `SqlRoleManager.syncRlsPermissions()` -- maps to VARCHAR[] columns in rls_permissions
- `SqlRoleManager.getRoleInfo()` -- maps from VARCHAR[] back to ColumnAccess
- `SqlQuery` / `GraphqlTableFieldFactory` -- enforces hidden/readonly at query/mutation time

### RoleInfo class (`org.molgenis.emx2.RoleInfo`) -- DOMAIN MODEL

Fields:
- `name` (String) -- short role name (e.g., "Viewer", "HospitalA")
- `description` (String) -- from `COMMENT ON ROLE`
- `system` (boolean) -- true = built-in system role, protected from modification/deletion
- `permissions` (Set\<Permission\>) -- per-table grants for this role (on top of inherited roles)

Constructor: `new RoleInfo(name)` initializes with empty permissions set.

Mutator: `addPermission(Permission)` adds to permissions set.

Used in:
- `Schema.getRoleInfo(String roleName)` -- returns single RoleInfo
- `Schema.getRoleInfos()` -- returns `List<RoleInfo>` for all roles in schema
- `SqlRoleManager.getRoleInfo(schemaName, roleName)` -- builds single RoleInfo from pg_roles + rls_permissions query
- `SqlRoleManager.getRoleInfos(schemaName)` -- builds all RoleInfos
- `GraphqlSchemaFieldFactory` -- serialized into `_schema { roles }` query response
- `GraphqlDatabaseFieldFactory` -- serialized into `_roles` query response (global roles)

### One Role Per Schema

Each user has exactly one role per schema, assigned via `addMember(email, role)`. This role is either a system role (Viewer, Editor, Manager, Owner, etc.), a schema-local custom role (Researcher, Curator, etc.), or a global role (HospitalA, DataMonitor, etc.).

### Role types by naming convention

| Type | PG role name | Managed by | Visible to |
|------|-------------|------------|------------|
| System | `MG_ROLE_<schema>/Viewer` etc. | System (immutable) | All |
| Schema-local | `MG_ROLE_<schema>/<name>` | Schema Manager+ | Schema Manager+ |
| Global | `MG_ROLE_*/<name>` | Database admin only | Database admin only |

Global roles are a single PG role with `*` as the schema part. They inherit roles per schema for their grants (system or custom, multiple allowed). One membership per user (not N per schema). Schema managers cannot see or modify global roles.

Role determination per request:
- User accesses `/<schema>/api/...`
- Application looks up member record: `SELECT role FROM members WHERE user = ?`
- If system role: unrestricted (see section 3 "System roles")
- If schema-local or global custom role: active_role set to full PG role name. Session variables pre-computed from `rls_permissions`. Each RLS policy checks its own list.

Session setup in SqlUserAwareConnectionProvider.acquire():
```sql
SET LOCAL molgenis.active_role = '<full_pg_role_name>';
SET LOCAL molgenis.rls_select_tables = '<comma_separated_fq_table_names>';
SET LOCAL molgenis.rls_insert_tables = '<comma_separated_fq_table_names>';
SET LOCAL molgenis.rls_update_tables = '<comma_separated_fq_table_names>';
SET LOCAL molgenis.rls_delete_tables = '<comma_separated_fq_table_names>';
```

See section 6 for session variable details.

No role switching API needed. Role is deterministic from membership.

If a user needs both cross-group visibility (Viewer) and data entry as HospitalA: use two separate accounts. This is standard practice in healthcare data management.

### Member class (`org.molgenis.emx2.Member`) -- DOMAIN MODEL

Fields:
- `user` (String) -- email/username
- `role` (String) -- role name (system or custom)
- `enabled` (Boolean) -- null or true = active; false = disabled (NOLOGIN)
- `roleAdmin` (Boolean) -- can add/remove other members to this same role

PG mapping:
- `roleAdmin=true` → `GRANT role TO user WITH ADMIN OPTION`
- `roleAdmin=false/null` → `GRANT role TO user`

Equality based on `(user, role)`.

Used in:
- `Schema.addMember(String user, String role)` -- convenience, defaults enabled=null, roleAdmin=null
- `Schema.addMember(Member member)` -- full control over enabled and roleAdmin
- `Schema.getMembers()` -- returns `List<Member>` for the schema
- `Schema.removeMember(String user)` -- removes member from schema
- `Schema.removeMembers(List<Member> members)` -- bulk remove
- `GraphqlSchemaFieldFactory` -- `change(members)` mutation sets all fields including enabled/roleAdmin
- `SqlRoleManager.addMember(schemaName, member)` -- executes PG GRANT, applies NOLOGIN / WITH ADMIN OPTION

## 2. Permission Storage

Three sources of truth:

### PG catalog (grants, role membership, role info)

| Info | PG Storage | Query Method |
|------|-----------|-------------|
| Role exists | `pg_roles` | `SELECT 1 FROM pg_roles WHERE rolname = ?` |
| Description | `COMMENT ON ROLE` | `shobj_description(oid, 'pg_authid')` |
| Schema | Encoded in role name | Parse `MG_ROLE_<schema>/<name>` (`*` = global) |
| Membership | `GRANT role TO user` | `pg_has_role(user, role, 'member')` |
| Role inherits | `GRANT MG_ROLE_<schema>/<parent> TO MG_ROLE_<schema>/<child>` | `pg_has_role(child, parent, 'member')` |
| Global role inherits | `GRANT MG_ROLE_<schema>/<role> TO MG_ROLE_*/<name>` | `pg_has_role(global_role, schema_role, 'member')` |
| Table perms | `GRANT SELECT/INSERT/... ON table TO role` | `has_table_privilege(role, table, priv)` |

### `rls_permissions` table (select level, RLS flags + column access overrides)

Created by migration31.sql in the `MOLGENIS` schema:

No PG enum type — `select_level` is VARCHAR, validated by Java `SelectLevel.valueOf()`.

| Column | Type | Notes |
|--------|------|-------|
| role_name | VARCHAR | NOT NULL, full PG role name (e.g., `MG_ROLE_schema/HospitalA`) |
| table_schema | VARCHAR | NOT NULL |
| table_name | VARCHAR | NOT NULL. Specific table name or `'*'` for schema-wide default |
| select_level | VARCHAR | EXISTS, RANGE, AGGREGATOR, COUNT, TABLE, ROW (validated by Java) |
| insert_rls | BOOLEAN | true = row-filtered for INSERT |
| update_rls | BOOLEAN | true = row-filtered for UPDATE |
| delete_rls | BOOLEAN | true = row-filtered for DELETE |
| grant_permission | BOOLEAN | nullable, true = can manage roles and permissions |
| editable_columns | VARCHAR[] | nullable |
| readonly_columns | VARCHAR[] | nullable |
| hidden_columns | VARCHAR[] | nullable |

Primary key: `(table_schema, role_name, table_name)`.

Every entry is per-table, except `table_name = '*'` which is a schema-wide default. The `*` entry applies to all RLS-enabled tables in the schema. Per-table entries override the `*` default. Schema-wide access for system roles is still handled by system role inheritance.

Upsert strategy: INSERT ... ON CONFLICT ... DO UPDATE (matching on PK).

**Role name format**: `role_name` stores the full PG role name (e.g., `MG_ROLE_schema/HospitalA`). This aligns with `molgenis.active_role` session variable so the RLS policy can do a direct lookup without parsing.

**Key insight**: `select_level = 'ROW'` means ROW-level filtering for SELECT. `select_level = 'TABLE'` or no row means unrestricted SELECT (PG GRANT controls access). `select_level` in EXISTS..COUNT means app-enforced restrictions (no PG SELECT grant). System role users are unrestricted (see section 3). No special-casing needed.

```sql
GRANT SELECT ON "MOLGENIS"."rls_permissions" TO PUBLIC;
```

SELECT-only grant prevents metadata tampering by non-admin users.

### Permissions query (inline in SqlRoleManager)

No view — `SqlRoleManager.getRoleInfo()` / `getRoleInfos()` loads this query from `permissions_query.sql` resource file:

```sql
SELECT
  t.tablename as table_name,
  has_table_privilege(:role, :schema||'.'||t.tablename, 'SELECT') as can_select,
  has_table_privilege(:role, :schema||'.'||t.tablename, 'INSERT') as can_insert,
  has_table_privilege(:role, :schema||'.'||t.tablename, 'UPDATE') as can_update,
  has_table_privilege(:role, :schema||'.'||t.tablename, 'DELETE') as can_delete,
  rp.select_level,
  COALESCE(rp.insert_rls, false) as insert_rls,
  COALESCE(rp.update_rls, false) as update_rls,
  COALESCE(rp.delete_rls, false) as delete_rls,
  rp.grant_permission,
  rp.editable_columns, rp.readonly_columns, rp.hidden_columns
FROM pg_tables t
LEFT JOIN "MOLGENIS".rls_permissions rp
  ON rp.role_name = :role
  AND rp.table_schema = t.schemaname
  AND rp.table_name = t.tablename
WHERE t.schemaname = :schema
  AND (has_table_privilege(:role, :schema||'.'||t.tablename, 'SELECT')
    OR has_table_privilege(:role, :schema||'.'||t.tablename, 'INSERT')
    OR has_table_privilege(:role, :schema||'.'||t.tablename, 'UPDATE')
    OR has_table_privilege(:role, :schema||'.'||t.tablename, 'DELETE')
    OR rp.select_level IS NOT NULL)
```

Reading semantics (mapped to Java):
- `can_select=true, select_level=NULL` -> `SelectLevel.TABLE` (PG grant, no restriction)
- `can_select=true, select_level='ROW'` -> `SelectLevel.ROW` (filtered by mg_roles)
- `can_select=false, select_level='COUNT'` -> `SelectLevel.COUNT` (app-enforced)
- `can_select=false, select_level=NULL` -> `null` (no access)
- `can_insert=true, insert_rls=false` -> `ModifyLevel.TABLE`
- `can_insert=true, insert_rls=true` -> `ModifyLevel.ROW`
- `can_insert=false, *` -> `null`

### Permissions explain query (inline in SqlRoleManager)

The introspection API (`_schema { myPermissions }`) uses `permissions_explain_query.sql` to show which inherited role causes each permission per table. Powers the `sourceRole` field in `MolgenisEffectivePermissionType`.

```sql
WITH RECURSIVE role_tree AS (
  SELECT r.oid AS role_oid, r.rolname AS role_name, 0 AS depth
  FROM pg_roles r WHERE r.rolname = :role
  UNION ALL
  SELECT parent.oid, parent.rolname, rt.depth + 1
  FROM role_tree rt
  JOIN pg_auth_members m ON m.member = rt.role_oid
  JOIN pg_roles parent ON parent.oid = m.roleid
  WHERE parent.rolname LIKE 'MG_ROLE_%'
)
SELECT
  rt.role_name AS source_role,
  rt.depth,
  t.tablename AS table_name,
  acl.privilege_type AS grant_type,
  rp.select_level,
  rp.insert_rls,
  rp.update_rls,
  rp.delete_rls,
  rp.editable_columns, rp.readonly_columns, rp.hidden_columns
FROM role_tree rt
CROSS JOIN pg_tables t
LEFT JOIN LATERAL (
  SELECT a.privilege_type
  FROM pg_class c
  JOIN pg_namespace n ON c.relnamespace = n.oid
  CROSS JOIN LATERAL aclexplode(c.relacl) a
  WHERE n.nspname = t.schemaname
    AND c.relname = t.tablename
    AND a.grantee = rt.role_oid
    AND a.privilege_type IN ('SELECT', 'INSERT', 'UPDATE', 'DELETE')
) acl ON true
LEFT JOIN "MOLGENIS".rls_permissions rp
  ON rp.role_name = rt.role_name
  AND rp.table_schema = t.schemaname
  AND rp.table_name = t.tablename
WHERE t.schemaname = :schema
  AND (acl.privilege_type IS NOT NULL OR rp.role_name IS NOT NULL)
ORDER BY t.tablename, rt.depth, acl.privilege_type
```

How it works:
- Recursive CTE walks `pg_auth_members` from `:role` up to all inherited parents
- `aclexplode(pg_class.relacl)` returns *direct* ACL entries (not inherited), so we can see which specific role holds the grant
- `depth` shows inheritance distance: 0 = direct, 1 = immediate parent, etc.
- Joined with `rls_permissions` to include RLS flags and column restrictions per source role

Example result for `MG_ROLE_CohortA/Researcher` (inherits Viewer):
```
source_role                    | depth | table    | grant_type | select_level | insert_rls
MG_ROLE_CohortA/Researcher    | 0     | Patients | INSERT     | ROW          | true
MG_ROLE_CohortA/Viewer        | 1     | Patients | SELECT     | NULL         | NULL
MG_ROLE_CohortA/Viewer        | 1     | Samples  | SELECT     | NULL         | NULL
```

This tells the admin: "Researcher can SELECT Patients because it inherits Viewer (depth 1). It can INSERT Patients as a direct grant (depth 0) with row-level restriction."

## 3. Role Management

### System roles

#### Per-schema system roles (8, protected, immutable)

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

#### Global system role (1, protected, immutable)

| Short Name | PG Role Name | Capabilities |
|------------|-------------|-------------|
| Admin | `MG_ROLE_*/Admin` | Database admin: create/delete schemas, manage global roles, manage users. Does NOT automatically have data access — must explicitly inherit a role per schema like any global role. |

Created once at database initialization (migration). Protected from modification/deletion like per-schema system roles.

Constants defined in `org.molgenis.emx2.Constants`:
```
ROLE_EXISTS, ROLE_RANGE, ROLE_AGGREGATOR, ROLE_COUNT,
ROLE_VIEWER, ROLE_EDITOR, ROLE_MANAGER, ROLE_OWNER,
ROLE_ADMIN
```

`SqlRoleManager.isSystemRole(roleName)` checks against `SYSTEM_ROLES` list (includes Admin). System roles cannot be created, modified, or deleted via the custom role API.

System roles have NO entries in `rls_permissions`. Their session variable lists are always empty, so RLS policies never restrict them.

### Authorization

Role management operations are restricted based on scope and caller:

**Schema-local role operations** (`createRole`, `deleteRole`, `grant`, `revoke`):
- Requires `grant=true` permission on `table='*'` for this schema, OR Owner system role, OR database admin
- Enforced at GraphQL mutation level and in `Schema.grant()` / `Schema.revoke()`

**Global role operations** (database-level `change(roles)`, `Database.addGlobalRoleInherits()`, etc.):
- Requires database admin (member of `MG_ROLE_*/Admin` or PG superuser)
- Creates single PG role (`MG_ROLE_*/<name>`), applies grants per schema

**Global roles query** (`_roles` on database endpoint):
- Requires database admin (member of `MG_ROLE_*/Admin` or PG superuser)
- Placed inside the admin-only query block in `GraphqlDatabaseFieldFactory`

**Schema roles query** (`_schema { roles }` on schema endpoint):
- Requires `grant=true` permission on `table='*'` for this schema, OR Owner system role, OR database admin
- Non-managers use `_schema { myPermissions }` to see their own effective permissions

### Schema-local custom roles

Created via `Schema.createRole(roleName, description)`, which delegates to `SqlRoleManager.createRole(schemaName, roleName)`.

PG role name format: `MG_ROLE_<schema>/<name>` (constructed by `SqlRoleManager.fullRoleName()`).

On creation:
1. PG role created via `SqlDatabaseExecutor.executeCreateRole()` (idempotent)
2. Custom role granted membership in the schema's Exists role (USAGE on schema)
3. Custom role granted to `session_user WITH ADMIN OPTION`

### Schema-wide permissions via wildcard

A custom role's data access is fully defined by the permission matrix. Use `table="*"` for a schema-wide default that applies to all RLS-enabled tables:

```java
schema.createRole("HospitalA", "Hospital A staff");
schema.grant("HospitalA", new Permission()
    .setTable("*").setSelect(SelectLevel.ROW)
    .setInsert(ModifyLevel.ROW).setUpdate(ModifyLevel.ROW));
```

This grants SELECT, INSERT, UPDATE on all RLS-enabled tables, restricted to own-group rows. When new tables get RLS enabled, HospitalA automatically gets row-filtered access.

Per-table entries override the wildcard default:
```java
schema.grant("HospitalA", new Permission()
    .setTable("Patients").setDelete(ModifyLevel.ROW));
schema.grant("HospitalA", new Permission()
    .setTable("Diseases").setSelect(SelectLevel.TABLE));
```

Result: HospitalA can also delete own Patients (override adds DELETE), and can see all Diseases (override to TABLE removes row filter for that table).

**Grant permission**: To give a custom role management capabilities (manage roles, members, permissions):
```java
schema.grant("HospitalA", new Permission()
    .setTable("*").setGrant(true));
```

This replaces the need to inherit Manager. Roles with `grant=true` can access `_schema { roles, members }` and call `change(roles)` / `drop(roles)` mutations.

### Global custom roles

Created via the database-level API (`change(roles)` mutation), which delegates to `SqlRoleManager.createGlobalRole(roleName)`.

PG role name format: `MG_ROLE_*/<name>` (constructed by `SqlRoleManager.fullRoleName("*", roleName)`).

On creation:
1. PG role created (idempotent)
2. Custom role granted to `session_user WITH ADMIN OPTION`

Authorization: see section 3 "Authorization". Database admin only.

Naming conflicts prevented by convention: `MG_ROLE_*/HospitalA` (global) is a different PG role from `MG_ROLE_CohortA/HospitalA` (local). No collision possible.

### Global role grant model

A global role gets **zero or more roles per schema** (system or custom). This is the only way global roles get data access — they do NOT get per-operation grants directly.

**Add role inheritance** (via `Database.addGlobalRoleInherits()`):
```java
database.addGlobalRoleInherits("HospitalA", "CohortA", "Editor");
database.addGlobalRoleInherits("HospitalA", "CohortA", "Researcher");
```

PG implementation: `GRANT MG_ROLE_CohortA/Editor TO MG_ROLE_*/HospitalA`

This gives HospitalA all the privileges of Editor in CohortA. Accepts any role name in that schema (system or custom). Multiple roles per schema allowed.

**Remove role inheritance**:
```java
database.removeGlobalRoleInherits("HospitalA", "CohortA", "Editor");
```

Revokes one inherited role. Does NOT delete per-table RLS overrides (those are managed separately via `Schema.revoke()`).

**Optional per-table RLS overrides** (via `Schema.grant()`, same as schema-local roles):
```java
Schema cohortA = db.getSchema("CohortA");
cohortA.grant("HospitalA", new Permission()
    .setTable("Patients").setSelect(SelectLevel.ROW).setInsert(ModifyLevel.ROW));
```

RLS overrides only **restrict** — they don't add grants beyond what the inherited system role provides. A global role with `inherits: "Editor"` gets full CRUD, but per-table RLS narrows that to own-group rows.

**Admin capability**: A global role with `inherits: "Manager"` or `"Owner"` in a schema gets management capability in that schema. No separate admin flag needed.

**Example**: Hospital A staff who can edit their own patients but view all samples in CohortA:
```java
database.addGlobalRoleInherits("HospitalA", "CohortA", "Editor");
Schema cohortA = db.getSchema("CohortA");
cohortA.grant("HospitalA", new Permission()
    .setTable("Patients").setSelect(SelectLevel.ROW)
    .setInsert(ModifyLevel.ROW).setUpdate(ModifyLevel.ROW).setDelete(ModifyLevel.ROW));
```

**Querying global role permissions**:
- `Database.getGlobalRoleInherits("HospitalA")` -- returns `Map<String, List<String>>` (schema → role names)
- `Schema.getRoleInfo("HospitalA").getPermissions()` -- returns per-table RLS overrides (same as schema-local roles)

### Role discovery

| Method | Scope |
|--------|-------|
| `getRolesForSchema(schemaName)` | Queries `MG_ROLE_<schema>/%`. Returns system + schema-local roles. |
| `getGlobalRoles()` | Queries `MG_ROLE_*/%`. Returns global roles only. Database admin only. |

## 4. GraphQL API Contract

### Schema-level API (per-schema endpoint)

#### Query: `_schema { roles, members }`

**Authorization:** Both `roles` and `members` require `grant=true` permission, Owner system role, or database admin (see section 3 "Authorization"). Non-managers use `_schema { myPermissions }` for their own effective permissions.

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

**MolgenisPermissionType** (`outputPermissionType`) -- for schema-local roles:
```graphql
type MolgenisPermissionType {
  schema: String   # populated for global roles, null for schema-local
  table: String
  select: String   # SelectLevel: "EXISTS".."ROW", or null
  insert: String   # ModifyLevel: "TABLE", "ROW", or null
  update: String   # ModifyLevel: "TABLE", "ROW", or null
  delete: String   # ModifyLevel: "TABLE", "ROW", or null
  grant: Boolean
  columns: MolgenisColumnAccessType
}

type MolgenisColumnAccessType {
  editable: [String]
  readonly: [String]
  hidden: [String]
}
```

**MolgenisMembersType** (`outputMembersMetadataType`):
```graphql
type MolgenisMembersType {
  email: String
  role: String
  enabled: Boolean
  roleAdmin: Boolean
}
```

Members field is only available to users with Manager or Owner role.

Data fetching: `queryFetcher()` calls `schema.getRoleInfos()` and `schema.getMembers()`, serializes Permission fields into maps using GraphqlConstants field names.

#### Mutation: `change(roles, members)`

**Authorization:** Requires `grant=true` permission, Owner role, or database admin (see section 3).

**Input types:**

**MolgenisRoleInput** (`inputRoleType`):
```graphql
input MolgenisRoleInput {
  name: String
  description: String
  permissions: [MolgenisPermissionInput]
}
```

**MolgenisPermissionInput** (`inputPermissionType`) -- for schema-local roles:
```graphql
input MolgenisPermissionInput {
  table: String
  select: String   # SelectLevel: "EXISTS".."ROW", or omit
  insert: String   # ModifyLevel: "TABLE", "ROW", or omit
  update: String   # ModifyLevel: "TABLE", "ROW", or omit
  delete: String   # ModifyLevel: "TABLE", "ROW", or omit
  grant: Boolean
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
  roleAdmin: Boolean
}
```

Processing logic in `changeRoles()`:
1. For each role in input: call `schema.createRole(name, description)` (idempotent)
2. For each permission in the role: construct a Permission object, parse select string to SelectLevel enum
3. Call `schema.grant(roleName, permission)`

Processing logic in `changeMembers()`:
- For each member: call `schema.addMember(email, role)`

#### Mutation: `drop(roles, members, permissions)`

**Authorization:** Requires `grant=true` permission, Owner role, or database admin (see section 3).

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
  table: String
  select: String
  insert: String
  update: String
  delete: String
}
```

Processing: `dropRoles()` calls `schema.deleteRole(roleName)` for each. `dropMembers()` calls `schema.removeMember(name)` for each. `dropPermissions()` constructs a Permission from the input fields and calls `schema.revoke(roleName, permission)` for each. If only `role` and `table` are provided (no specific operations), all operations for that table are revoked.

### CSV Import: roles endpoint

POST to `/<schema>/api/csv/roles` with CSV body:

**Schema-local roles** (POST `/<schema>/api/csv/roles`):
```csv
role,description,table,select,insert,update,delete,grant,editable,readonly,hidden
HospitalA,Hospital A staff,*,ROW,ROW,ROW,,,,,,
HospitalA,,Patients,,,,ROW,,,,
Researcher,Read-only researcher,*,ROW,,,,,,,
Curator,Data curator,*,TABLE,TABLE,TABLE,TABLE,true,,,
```

`select` accepts SelectLevel (EXISTS/RANGE/AGGREGATOR/COUNT/TABLE/ROW). `insert`/`update`/`delete` accept ModifyLevel (TABLE/ROW). `grant` is true/false for management capabilities.

**Global roles** (POST `/api/csv/globalroles`, database admin only):
```csv
role,description,schema,systemRole,table,select,insert,update,delete,editable,readonly,hidden
HospitalA,Hospital A staff,CohortA,Editor,,,,,,,,
HospitalA,,CohortA,,Patients,ROW,ROW,ROW,ROW,,,ssn
HospitalA,,CohortA,,Samples,ROW,ROW,,,,,,
HospitalA,,CohortB,Viewer,,,,,,,,
DataMonitor,Read-only monitor,CohortA,Count,,,,,,,,
```

Rows with `systemRole` set (and no `table`) define the per-schema role grant (system or custom role). Rows with `table` set define per-table RLS overrides. `schema` is required on all rows.

Column lists use semicolon separator within each field. Empty = no overrides.

Processing:
1. Group rows by role name
2. Create roles (idempotent)
3. Set permissions per table
4. Description taken from first occurrence per role

Export: GET `/<schema>/api/csv/roles` returns same format.

### Global API (database-level endpoint)

#### Query: `_roles`

**Authorization:** See section 3 "Authorization". Database admin only. Placed inside the admin-only query block in `GraphqlDatabaseFieldFactory`.

Output type — separates role hierarchy (`schemas`) from per-table permissions:

**MolgenisGlobalRoleInfoType** (`globalOutputRoleInfoType`):
```graphql
type MolgenisGlobalRoleInfoType {
  name: String
  description: String
  schemas: [MolgenisSchemaGrantType]       # one role per schema
  permissions: [MolgenisPermissionType]    # per-table RLS overrides (reuses schema-level type)
}
```

**MolgenisSchemaGrantType** (`schemaGrantType`):
```graphql
type MolgenisSchemaGrantType {
  schema: String    # schema name
  roles: [String]   # inherited role names (system or custom, e.g. ["Editor", "Researcher"])
}
```

The `schemas` list has at most one entry per schema. The `permissions` list uses the same `MolgenisPermissionType` as schema-level (with `schema` field populated to indicate which schema).

Data fetching in `rolesQuery()`:
1. Queries `pg_roles` for `MG_ROLE_*/%` roles
2. For each global role, checks system role inheritance per schema via `pg_has_role()`
3. Reads per-table RLS overrides from `rls_permissions`

#### Mutation: `change(roles)` for global roles

**Authorization:** See section 3 "Authorization". Database admin only.

**MolgenisGlobalRoleInput** (`globalInputRoleType`):
```graphql
input MolgenisGlobalRoleInput {
  name: String
  description: String
  schemas: [MolgenisSchemaGrantInput]      # one role per schema
  permissions: [MolgenisPermissionInput]   # per-table RLS overrides
}
```

**MolgenisSchemaGrantInput** (`schemaGrantInput`):
```graphql
input MolgenisSchemaGrantInput {
  schema: String!      # schema name
  roles: [String!]!    # role names to inherit (system or custom)
}
```

Per-table permissions reuse `MolgenisPermissionInput` (with `schema` field required for global roles).

Processing in `changeRoles()`:
1. Creates global PG role `MG_ROLE_*/<name>` (idempotent)
2. For each schema grant: for each role in `roles`, `Database.addGlobalRoleInherits(name, schema, role)`
3. For each permission: `Schema.grant(name, permission)` with per-table RLS overrides
4. Validates each role exists in the target schema

## 4b. Introspection API

### Schema-level: `_schema { myPermissions }`

Returns effective permissions for the current user, with source role information.

**MolgenisEffectivePermissionType**:
```graphql
type MolgenisEffectivePermissionType {
  table: String
  select: String   # effective SelectLevel
  insert: String   # effective ModifyLevel
  update: String   # effective ModifyLevel
  delete: String   # effective ModifyLevel
  columns: MolgenisColumnAccessType
  sourceRole: String   # role name that provides this permission
}
```

Available to any authenticated user (shows own permissions only). Manager+ can query for any user via `_schema { permissionsOf(email: "user@example.com") }`.

## 5. Permission Semantics

### Operation grants (unified for system and custom roles)

System roles and custom roles use the same grant mechanism. Both get PG table grants via the same codepath.

Controlled via PG `GRANT`/`REVOKE` executed by `SqlRoleManager.syncPermissionGrants()`. See "Domain ↔ Storage mapping" in section 1 for full mapping tables.

All permissions are per-table. Schema-wide access is handled by system roles or `Database.addGlobalRoleInherits()`.

### Column-level access (app-enforced)

Per-column access control via `columns` field on Permission, containing three string arrays:

- `editable: [String]` -- these columns are editable (overrides default to EDITABLE)
- `readonly: [String]` -- these columns are read-only (overrides default to VIEW)
- `hidden: [String]` -- these columns are hidden entirely

Unlisted columns inherit the default from table-level permission:
- Table has UPDATE grant: unlisted columns default to EDITABLE
- Table has no UPDATE grant: unlisted columns default to VIEW

Examples:
| has UPDATE | columns | Effect |
|-----------|---------|--------|
| yes | `{ hidden: ["ssn"] }` | All editable, SSN hidden |
| no | `{ editable: ["name", "dob"] }` | All view-only, name/dob editable |
| yes | `{ readonly: ["address"], hidden: ["ssn"] }` | All editable, address read-only, SSN hidden |
| no | `{ editable: ["name", "dob"], hidden: ["ssn"] }` | Name/dob editable, SSN hidden, rest view-only |
| yes | (null) | All editable (default) |
| no | (null) | All view-only (default) |

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

### Table creation hook

When `SqlTableMetadataExecutor.createTable()` creates a new table:
1. Apply grants for the 8 system roles (Exists through Owner) -- existing behavior
2. Global roles that inherit a system role in this schema automatically get access via PG role inheritance -- no extra work needed

Custom roles do NOT automatically get access to new tables. Admin must explicitly `grant()` per table. This is intentional — custom roles are for fine-grained per-table control.

**Exception**: Custom roles with `table_name = '*'` entries in `rls_permissions` DO get automatic access to new RLS-enabled tables. When `enableRowLevelSecurity()` is called on a table, it checks for `*` entries and applies their PG grants to the new table.

**RLS enablement**: Not all tables need RLS. RLS is only enabled on a table when at least one role has an RLS restriction on it.

### Cleanup on drop

**Table drop** (`Schema.dropTable()`): before dropping, clean up all permission state:
```sql
DELETE FROM "MOLGENIS".rls_permissions WHERE table_schema = :schema AND table_name = :table;
```
PG grants are automatically dropped when the table is dropped.

**Schema drop** (`Database.dropSchema()`): before `DROP SCHEMA CASCADE`, clean up:
```sql
DELETE FROM "MOLGENIS".rls_permissions WHERE table_schema = :schema;
```
PG `DROP SCHEMA CASCADE` drops all `MG_ROLE_<schema>/*` roles, which automatically breaks any `GRANT ... TO MG_ROLE_*/<global>` inheritance chains. Global roles simply lose their inherited privileges for that schema.

### Merge semantics

Grants on the same (role, schema, table) **merge** — non-null fields in the new Permission overwrite, null fields are left unchanged. This allows incremental permission building:
```java
schema.grant("Researcher", new Permission().setTable("Patients").setSelect(SelectLevel.ROW));
schema.grant("Researcher", new Permission().setTable("Patients").setInsert(ModifyLevel.TABLE));
// Result: Patients has select=ROW AND insert=TABLE
```

- PG grants: additive (`GRANT` applied, no implicit `REVOKE`). Use explicit `revoke()` to remove.
- RLS flags + column restrictions: upsert via ON CONFLICT, using `COALESCE(EXCLUDED.field, existing.field)` to preserve unset fields
- Revocation: revokes PG grants for non-null fields AND clears corresponding `rls_permissions` flags. Deletes row if all fields become null/false.

## 6. RLS Implementation

### mg_roles column

Added by `SqlRoleManager.enableRowLevelSecurity()` when triggered by an RLS restriction being set.

```sql
ALTER TABLE <schema>.<table> ADD COLUMN mg_roles TEXT[] DEFAULT NULL;
CREATE INDEX IF NOT EXISTS <table>_mg_roles_idx ON <schema>.<table> USING GIN(mg_roles);
ALTER TABLE <schema>.<table> ENABLE ROW LEVEL SECURITY;
```

Idempotent: checks `information_schema.columns` before adding column.

### Session variables

5 session variables set once per transaction in SqlUserAwareConnectionProvider.acquire():
- `molgenis.active_role` -- user's full PG role name (e.g., `MG_ROLE_schema/HospitalA`)
- `molgenis.rls_select_tables` -- comma-separated fully-qualified table names where this user gets row-filtered for SELECT
- `molgenis.rls_insert_tables` -- comma-separated fully-qualified table names where this user gets row-filtered for INSERT
- `molgenis.rls_update_tables` -- comma-separated fully-qualified table names where this user gets row-filtered for UPDATE
- `molgenis.rls_delete_tables` -- comma-separated fully-qualified table names where this user gets row-filtered for DELETE

Pre-computed at tx start with 1 query:
```sql
SELECT table_schema || '.' || table_name, select_level, insert_rls, update_rls, delete_rls
FROM "MOLGENIS".rls_permissions
WHERE role_name = :active_role
  AND (select_level = 'ROW' OR insert_rls = true OR update_rls = true OR delete_rls = true)
```

Java groups result into 4 comma-separated lists (only ROW-level SELECT tables go into `rls_select_tables`).

**Wildcard expansion**: If the query returns a row with `table_name = '*'`, Java expands it by querying all RLS-enabled tables in the schema:
```sql
SELECT tablename FROM pg_tables t
JOIN pg_class c ON c.relname = t.tablename
JOIN pg_namespace n ON c.relnamespace = n.oid AND n.nspname = t.schemaname
WHERE t.schemaname = :schema AND c.relrowsecurity = true
```
Each RLS-enabled table inherits the `*` row's flags unless overridden by a per-table entry.

Transaction defaults (set in tx()):
```sql
SET LOCAL molgenis.active_role = '';
SET LOCAL molgenis.rls_select_tables = '';
SET LOCAL molgenis.rls_insert_tables = '';
SET LOCAL molgenis.rls_update_tables = '';
SET LOCAL molgenis.rls_delete_tables = '';
```

Empty lists = nothing restricted = backward compatible.

SET LOCAL scope: transaction-only, auto-resets on COMMIT/ROLLBACK. Safe for HikariCP connection pooling.

Error handling: if SET ROLE fails, close connection (force HikariCP discard) and throw MolgenisException. Never return connection with unknown role state.

### RLS policies

Four policies per table, one per operation. Each checks its own session variable.

**SELECT policy** (`<table>_rls_select`):
```sql
CREATE POLICY <table>_rls_select ON <schema>.<table> FOR SELECT
  USING (
    '<schema>.<table>' != ALL(string_to_array(
        COALESCE(current_setting('molgenis.rls_select_tables', true), ''), ','))
    OR mg_roles IS NULL
    OR (COALESCE(current_setting('molgenis.active_role', true), '') <> ''
        AND mg_roles @> ARRAY[current_setting('molgenis.active_role', true)])
  )
```

**INSERT policy** (`<table>_rls_insert`):
```sql
CREATE POLICY <table>_rls_insert ON <schema>.<table> FOR INSERT
  WITH CHECK (
    '<schema>.<table>' != ALL(string_to_array(
        COALESCE(current_setting('molgenis.rls_insert_tables', true), ''), ','))
    OR mg_roles IS NULL
    OR (COALESCE(current_setting('molgenis.active_role', true), '') <> ''
        AND mg_roles @> ARRAY[current_setting('molgenis.active_role', true)])
  )
```

**UPDATE policy** (`<table>_rls_update`):
```sql
CREATE POLICY <table>_rls_update ON <schema>.<table> FOR UPDATE
  USING (
    '<schema>.<table>' != ALL(string_to_array(
        COALESCE(current_setting('molgenis.rls_update_tables', true), ''), ','))
    OR mg_roles IS NULL
    OR (COALESCE(current_setting('molgenis.active_role', true), '') <> ''
        AND mg_roles @> ARRAY[current_setting('molgenis.active_role', true)])
  )
  WITH CHECK (
    '<schema>.<table>' != ALL(string_to_array(
        COALESCE(current_setting('molgenis.rls_update_tables', true), ''), ','))
    OR mg_roles IS NULL
    OR (COALESCE(current_setting('molgenis.active_role', true), '') <> ''
        AND mg_roles @> ARRAY[current_setting('molgenis.active_role', true)])
  )
```

**DELETE policy** (`<table>_rls_delete`):
```sql
CREATE POLICY <table>_rls_delete ON <schema>.<table> FOR DELETE
  USING (
    '<schema>.<table>' != ALL(string_to_array(
        COALESCE(current_setting('molgenis.rls_delete_tables', true), ''), ','))
    OR mg_roles IS NULL
    OR (COALESCE(current_setting('molgenis.active_role', true), '') <> ''
        AND mg_roles @> ARRAY[current_setting('molgenis.active_role', true)])
  )
```

Note: INSERT uses only WITH CHECK (no existing rows to filter). UPDATE uses BOTH USING (which existing rows can be seen) and WITH CHECK (what the new row values must satisfy). DELETE and SELECT use only USING.

Logic: if table is NOT in that operation's rls list -> allow all rows (TABLE level). If table IS in rls list -> check mg_roles.

Table name and schema name are embedded as string literals at policy creation time (each table gets its own policies).

Performance: `string_to_array` on a short comma list + `!= ALL` is very efficient. `@>` uses GIN index. Estimated overhead: <1%.

Policy drops are idempotent (`DROP POLICY IF EXISTS` before create).

### ENABLE vs FORCE

Current: `ENABLE ROW LEVEL SECURITY` (table owner bypasses).

Implication: the MOLGENIS application superuser bypasses all RLS. Java always executes `SET ROLE` before user queries. FORCE would require a separate `MG_APPLICATION` role (architectural change, planned for future).

## 7. Interface Methods

### Database interface (`org.molgenis.emx2.Database`)

New methods for global role management:

```java
void createGlobalRole(String roleName, String description);
void deleteGlobalRole(String roleName);
void addGlobalRoleInherits(String globalRoleName, String schemaName, String roleName);
void removeGlobalRoleInherits(String globalRoleName, String schemaName, String roleName);
Map<String, List<String>> getGlobalRoleInherits(String globalRoleName);
List<RoleInfo> getGlobalRoleInfos();
```

Used in:
- `createGlobalRole()` -- creates `MG_ROLE_*/<name>` PG role. Admin only.
- `deleteGlobalRole()` -- drops global role and all its grants/rls_permissions. Admin only.
- `addGlobalRoleInherits()` -- `GRANT MG_ROLE_<schema>/<role> TO MG_ROLE_*/<name>`. Idempotent.
- `removeGlobalRoleInherits()` -- revokes one inherited role for that schema.
- `getGlobalRoleInherits()` -- returns map of schema → list of role names for a global role.
- `getGlobalRoleInfos()` -- returns all global roles with their schema grants and per-table permissions.

All delegate to `SqlRoleManager`.

### Schema interface (`org.molgenis.emx2.Schema`)

Per-table permission methods (work for both schema-local and global roles):

```java
void createRole(String roleName, String description);
void deleteRole(String roleName);
void grant(String roleName, Permission permission);
void revoke(String roleName, Permission permission);
RoleInfo getRoleInfo(String roleName);
List<RoleInfo> getRoleInfos();
```

All delegate to `SqlRoleManager` with `getName()` (schema name) prepended.

`grant()` and `revoke()` work identically for schema-local and global roles — the system determines the role type from the PG role name (`MG_ROLE_*/*` = global).

`revoke()` accepts a Permission to allow revoking specific operations (e.g., revoke only DELETE while keeping SELECT). Fields set to non-null in the Permission are revoked; null fields are left unchanged.

`createRole()` and `deleteRole()` are for schema-local roles only. Use `Database.createGlobalRole()` / `Database.deleteGlobalRole()` for global roles.

Member methods:
```java
void addMember(String user, String role);    // convenience, enabled=null, roleAdmin=null
void addMember(Member member);               // full control: enabled, roleAdmin
List<Member> getMembers();
void removeMember(String user);
void removeMembers(List<Member> members);
List<String> getRoles();
```

`addMember(Member)` maps to PG:
- `enabled=false` → `ALTER ROLE ... NOLOGIN`
- `roleAdmin=true` → `GRANT role TO user WITH ADMIN OPTION`

## 8. SqlRoleManager API

Class: `org.molgenis.emx2.sql.SqlRoleManager`

Constructor: `SqlRoleManager(SqlDatabase database)` -- uses `database.getJooq()` for current DSL context.

### Schema-local role lifecycle

| Method | Behavior |
|--------|----------|
| `createRole(schemaName, roleName)` | Idempotent CREATE ROLE, GRANT Exists, GRANT to session_user. Rejects system roles. Called by `Schema.createRole()`. |
| `deleteRole(schemaName, roleName)` | Revokes all table grants, deletes `rls_permissions` rows, cleans orphaned mg_roles entries, revokes all memberships, DROP ROLE. Rejects system roles and non-existent roles. Called by `Schema.deleteRole()`. |
| `roleExists(schemaName, roleName)` | Checks pg_roles for full PG role name. |
| `isSystemRole(roleName)` | Checks against SYSTEM_ROLES list (includes Admin). |

### Global role lifecycle

| Method | Behavior |
|--------|----------|
| `createGlobalRole(roleName)` | Idempotent CREATE ROLE `MG_ROLE_*/<name>`, GRANT to session_user. Rejects system roles. Called by `Database.createGlobalRole()`. |
| `deleteGlobalRole(roleName)` | Revokes all system role grants across schemas, deletes `rls_permissions` rows, cleans orphaned mg_roles, DROP ROLE. Called by `Database.deleteGlobalRole()`. |
| `addGlobalRoleInherits(globalRoleName, schemaName, roleName)` | `GRANT MG_ROLE_<schema>/<role> TO MG_ROLE_*/<name>`. Idempotent. Validates role exists. Called by `Database.addGlobalRoleInherits()`. |
| `removeGlobalRoleInherits(globalRoleName, schemaName, roleName)` | `REVOKE MG_ROLE_<schema>/<role> FROM MG_ROLE_*/<name>`. Called by `Database.removeGlobalRoleInherits()`. |
| `getGlobalRoleInherits(globalRoleName)` | Queries `pg_auth_members` for inherited roles per schema. Returns `Map<String, List<String>>`. Called by `Database.getGlobalRoleInherits()`. |
| `getGlobalRoleInfos()` | Queries `MG_ROLE_*/%` roles, builds RoleInfo with schema grants + per-table permissions. Called by `Database.getGlobalRoleInfos()`. |

### Orphaned mg_roles cleanup on deleteRole

On deleteRole, BEFORE dropping role:
- For each table with mg_roles column in schema:
  UPDATE <table> SET mg_roles = array_remove(mg_roles, '<fullRoleName>')
- This prevents role re-creation attack (old mg_roles entries reactivating)

### Permission management

| Method | Behavior |
|--------|----------|
| `setPermission(schemaName, roleName, permission)` | Maps domain model (SelectLevel/ModifyLevel) to storage. Syncs PG grants via `syncPermissionGrants()`, upserts to `rls_permissions` via `syncRlsPermissions()`. Always per-table. Enables RLS on table if select=ROW or any modify=ROW. If `table="*"`, stores schema-wide default. If `grant=true`, maps to Manager-level authorization check. Called by `Schema.grant()`. |
| `revokePermission(schemaName, roleName, permission)` | For each non-null field in permission: revokes that PG grant and clears corresponding rls_permissions flag. Null fields left unchanged. Deletes rls_permissions row if all fields become null/false. Called by `Schema.revoke()`. |
| `getRoleInfo(schemaName, roleName)` | Builds RoleInfo from pg_roles + inline permissions query. Called by `Schema.getRoleInfo()`. |
| `syncRlsPermissions(schemaName, roleName, permission)` | Upserts `select_level` (VARCHAR), `insert_rls`/`update_rls`/`delete_rls` (boolean), and column restrictions to `rls_permissions` using full PG role name. Maps domain enums to storage types. Always per-table. Deletes row if all fields are null/false. |

### Role discovery

| Method | Behavior |
|--------|----------|
| `getRolesForSchema(schemaName)` | Queries pg_roles with prefix `MG_ROLE_<schema>/`, returns short names. |
| `getRoleInfos(schemaName)` | For each role: builds RoleInfo with system flag, description, and permissions from inline permissions query. |

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

### Cleanup

| Method | Behavior |
|--------|----------|
| `cleanupTablePermissions(schemaName, tableName)` | Deletes all `rls_permissions` rows for that table. Called by `Schema.dropTable()` before table drop. |
| `cleanupSchemaPermissions(schemaName)` | Deletes all `rls_permissions` rows for that schema. Called by `Database.dropSchema()` before schema cascade. |

### RLS enablement

| Method | Behavior |
|--------|----------|
| `enableRowLevelSecurity(schemaName, tableName)` | Idempotent: adds mg_roles column, GIN index, enables RLS, creates SELECT and modify policies. |

### Static utility

```java
public static String fullRoleName(String schemaName, String roleName)
// Returns: MG_ROLE_PREFIX + schemaName + "/" + roleName
// schemaName = "*" for global roles

public static boolean isGlobalRole(String fullRoleName)
// Returns: fullRoleName starts with "MG_ROLE_*/"
```

## 9. Constants

### `org.molgenis.emx2.Constants`

| Constant | Value |
|----------|-------|
| `MG_ROLES` | `"mg_roles"` |
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
| `ROLE_ADMIN` | `"Admin"` |
| `GLOBAL_SCHEMA` | `"*"` |
| `TABLE` | `"table"` |
| `DESCRIPTION` | `"description"` |
| `KEY` | `"key"` |
| `ACTIVE_ROLE` | `"active_role"` |
| `RLS_SELECT_TABLES` | `"rls_select_tables"` |
| `RLS_INSERT_TABLES` | `"rls_insert_tables"` |
| `RLS_UPDATE_TABLES` | `"rls_update_tables"` |
| `RLS_DELETE_TABLES` | `"rls_delete_tables"` |
| `SYSTEM_ROLES` | `Set.of(ROLE_EXISTS, ROLE_RANGE, ROLE_AGGREGATOR, ROLE_COUNT, ROLE_VIEWER, ROLE_EDITOR, ROLE_MANAGER, ROLE_OWNER, ROLE_ADMIN)` |

### `org.molgenis.emx2.graphql.GraphqlConstants`

Permission-related constants added for this feature:

| Constant | Value | Used in |
|----------|-------|---------|
| `SELECT` | `"select"` | Permission input/output (PermissionLevel string) |
| `INSERT` | `"insert"` | Permission input/output (PermissionLevel string) |
| `UPDATE` | `"update"` | Permission input/output (PermissionLevel string) |
| `DELETE` | `"delete"` | Permission input/output (PermissionLevel string) |
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
| `SCHEMA_NAME` | `"schema"` | Global permission field |
| `SCHEMAS` | `"schemas"` | Global role schema grants |
| `GRANT` | `"grant"` | Permission field (Boolean) |

## 10. Migration

### migration31.sql

migration31.sql references `rls_permissions_create.sql` and creates:
1. `MOLGENIS.rls_permissions` table (via `rls_permissions_create.sql`)
2. SELECT grant on rls_permissions to PUBLIC
3. `MG_ROLE_*/Admin` global system role (idempotent CREATE ROLE, GRANT to session_user)

### SQL resource files

Stored in `backend/molgenis-emx2-sql/src/main/resources/org/molgenis/emx2/sql/`:

| File | Used by | Content |
|------|---------|---------|
| `rls_permissions_create.sql` | migration31.sql, SqlRoleManager | CREATE TABLE with PK on `(table_schema, role_name, table_name)` |
| `permissions_query.sql` | SqlRoleManager.getRoleInfo() | Joins pg_tables + rls_permissions + has_table_privilege() |
| `permissions_explain_query.sql` | Introspection API (myPermissions) | Recursive role tree + aclexplode() for source attribution |

`rls_permissions` columns:
- `role_name` (VARCHAR) -- full PG role name (e.g., `MG_ROLE_schema/HospitalA`)
- `table_schema` (VARCHAR)
- `table_name` (VARCHAR) -- specific table name or `'*'` for schema-wide default
- `select_level` (VARCHAR) -- EXISTS, RANGE, AGGREGATOR, COUNT, TABLE, ROW (validated by Java)
- `insert_rls`, `update_rls`, `delete_rls` (BOOLEAN, nullable) -- RLS restriction flags
- `grant_permission` (BOOLEAN, nullable) -- true = can manage roles and permissions
- `editable_columns`, `readonly_columns`, `hidden_columns` (VARCHAR[], nullable) -- column-level restrictions

### Migrations.java

`SOFTWARE_DATABASE_VERSION` bumped to 32. Migration31 step executes when `version < 31`.

## 11. Security Model

### Threat model

RLS protects against authorized users exceeding their data scope (user in GroupA cannot see GroupB rows). It does NOT protect against SQL injection, superuser/DBA access, or application bugs that skip SET ROLE.

Trust boundaries: superuser and application owner role are trusted. User-level roles are untrusted and subject to RLS.

### Privilege escalation prevention

- Modify RLS policy includes `WITH CHECK` clause (prevents INSERT/UPDATE of rows with unauthorized mg_roles)
- Application-layer validation: `SqlTable.insertBatch()` / `SqlTable.updateBatch()` reject mg_roles changes unless user is Manager+
- System role protection: SqlRoleManager refuses to create/modify/delete system roles
- Orphaned mg_roles cleanup on role deletion prevents role re-creation attack
- Grant permission check: only roles with `grant=true` on `table='*'` can manage other roles' permissions. Enforced at application layer.

### Role name validation

mg_roles stores role names as TEXT[] without FK to pg_roles. Mitigations:
- Application validates role names on INSERT/import
- `deleteRole()` cleans orphaned mg_roles entries, revokes all memberships, and drops the role
- Residual orphaned role names in mg_roles are harmless (no matching role = no access granted)

### Column restriction bypass risk

Column access (ColumnAccess editable/readonly/hidden lists) is enforced at application layer only. Direct SQL access bypasses these restrictions. Same residual risk as COUNT-only access restrictions.

## 12. Design Decisions

See `finegrained-scenarios.md` "Design decisions" section for all resolved design decisions.
