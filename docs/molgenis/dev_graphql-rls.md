# GraphQL row-level security reference

This page documents the GraphQL types, queries, and mutations added for row-level security (RLS) and custom roles.

## Enum types

### `MolgenisSelectScope`

Used for the `select` field of `MolgenisTablePermissionInput` and `MolgenisTablePermissionOutput`.

Values: `NONE`, `OWN`, `GROUP`, `EXISTS`, `COUNT`, `RANGE`, `AGGREGATE`, `ALL`

### `MolgenisUpdateScope`

Used for the `insert`, `update`, and `delete` fields of `MolgenisTablePermissionInput` and `MolgenisTablePermissionOutput`.

Values: `NONE`, `OWN`, `GROUP`, `ALL`

## Input types

### `MolgenisTablePermissionInput`

```graphql
input MolgenisTablePermissionInput {
  table: String!
  select: MolgenisSelectScope
  insert: MolgenisUpdateScope
  update: MolgenisUpdateScope
  delete: MolgenisUpdateScope
}
```

All scope fields default to `NONE` when omitted.

### `MolgenisRoleInput`

```graphql
input MolgenisRoleInput {
  name: String!
  description: String
  schemaName: String
  tables: [MolgenisTablePermissionInput]
  changeOwner: Boolean
  changeGroup: Boolean
}
```

`schemaName` is validated server-side: if set it must match the schema being mutated.

### `MolgenisGroupInput`

```graphql
input MolgenisGroupInput {
  name: String!
  users: [String]
}
```

## Output types

### `MolgenisTablePermissionOutput`

```graphql
type MolgenisTablePermissionOutput {
  table: String
  select: MolgenisSelectScope
  insert: MolgenisUpdateScope
  update: MolgenisUpdateScope
  delete: MolgenisUpdateScope
}
```

### `MolgenisRoleOutput`

```graphql
type MolgenisRoleOutput {
  name: String
  description: String
  schemaName: String
  tables: [MolgenisTablePermissionOutput]
  changeOwner: Boolean
  changeGroup: Boolean
}
```

### `MolgenisGroupOutput`

```graphql
type MolgenisGroupOutput {
  name: String
  users: [String]
}
```

## Schema query fields

These fields are exposed under `_schema`.

| Field | Type | Description |
|---|---|---|
| `roles` | `[MolgenisRoleOutput]` | All roles — system roles (empty tables list) and custom roles |
| `groups` | `[MolgenisGroupOutput]` | All groups defined in the schema |

## Mutations

### `change` (roles and groups)

```graphql
mutation {
  change(
    roles: [MolgenisRoleInput]
    groups: [MolgenisGroupInput]
  ) {
    message
  }
}
```

- `roles` — upserts each role. System roles are silently skipped. `setPermissions` is called on every custom role, replacing the stored `PermissionSet`.
- `groups` — idempotent upsert. Creates the group if absent, then replaces the user list with the provided `users` array. Omitting `users` leaves membership unchanged.

Requires Manager or Owner privilege.

### `drop` (roles and groups)

```graphql
mutation {
  drop(
    roles: [String]
    groups: [String]
  ) {
    message
  }
}
```

Deletes each named role or group. Requires Manager or Owner privilege.

## Groups and role assignment

### What a group is

A group is a per-schema named list of users. Groups carry no permissions of their own. Their purpose is to tag rows (`mg_groups` column) and to gate row-level access when a custom role uses `GROUP` scope. A user can belong to many groups in the same schema, and can hold a different custom role in each group.

### Assigning a user to a role

`MolgenisMembersInput` accepts `{user, role, group?}`. The `email` field is a deprecated alias for `user`. The back-end routes the call: if the role is a system role, `schema.addMember` is called (group must be absent); if it is a custom role, `SqlRoleManager.addGroupMembership` stores the membership.

System-role assignment (no group):

```graphql
mutation {
  change(members: [{ user: "alice@example.org", role: "Viewer" }]) {
    message
  }
}
```

Custom-role assignment with group:

```graphql
mutation {
  change(members: [
    { user: "alice@example.org", role: "staff", group: "science_dept" }
  ]) {
    message
  }
}
```

Custom-role assignment without group (schema-wide):

```graphql
mutation {
  change(members: [
    { user: "alice@example.org", role: "staff" }
  ]) {
    message
  }
}
```

Removing a schema-wide grant (leaves group-scoped grants for the same user+role intact):

```graphql
mutation {
  drop(members: [
    { user: "alice@example.org", role: "staff" }
  ]) {
    message
  }
}
```

Removing a specific group grant (leaves the schema-wide grant and other group grants intact):

```graphql
mutation {
  drop(members: [
    { user: "alice@example.org", role: "staff", group: "science_dept" }
  ]) {
    message
  }
}
```

Validation: system role must not have a `group`. Custom roles accept an optional `group`; omit for a schema-wide grant. Non-Owner/non-Manager cannot grant Owner or Manager (escalation guard). The group name `__direct__` is reserved and rejected at the API level.

### Per-group multiplicity

A user can hold the same custom role in multiple groups, or different custom roles in different groups, or a schema-wide grant alongside group-scoped grants. System roles are schema-wide and do not accept a group parameter.

### Schema-wide custom-role grants

Omitting `group` from a custom-role `change(members:[...])` call creates a schema-wide grant rather than a group-scoped one. The effect depends on the verb scopes defined in the role for each table:

| Role verb scope | Effect of schema-wide grant |
|---|---|
| `ALL` | User can access all rows in that table |
| `OWN` | User can access their own rows in that table |
| `GROUP` | Grant is **inert** for that table — no group to overlap with `row.mg_groups` |

A schema-wide grant on a `GROUP`-scoped table produces no access. To get row access on a `GROUP`-scoped table the user must also hold a group-scoped grant for a group that appears in `mg_groups`.

Typical use case: a role that mixes scopes. Grant the role schema-wide and the `ALL`/`OWN`-scoped tables work immediately; for any `GROUP`-scoped tables in the same role, add explicit group grants as needed.

## Custom roles and per-table permissions

### What a custom role is

A custom role is a per-schema named permission set, distinct from the four system roles (Owner, Manager, Editor, Viewer). It defines, for each table, independent `select`, `insert`, `update`, and `delete` scopes, plus optional `changeOwner` and `changeGroup` flags. A table not listed in the role defaults to `NONE` on all verbs — the role holder cannot see or touch those rows.

### Defining a custom role

Use the `change` mutation with a `roles` argument:

```graphql
mutation {
  change(roles: [{
    name: "staff"
    description: "Department staff with full access to own-group rows"
    tables: [
      {
        table: "Books"
        select: GROUP
        insert: GROUP
        update: GROUP
        delete: GROUP
      }
    ]
    changeOwner: false
    changeGroup: false
  }]) {
    message
  }
}
```

Calling `change(roles:[...])` on an existing role replaces its full `PermissionSet` and rebuilds all RLS policies immediately. Requires Manager or Owner privilege.

### Scope semantics

| Scope | Rows visible / writable |
|---|---|
| `ALL` | Every row in the table |
| `GROUP` | Rows where `mg_groups` overlaps the user's group memberships |
| `OWN` | Rows where `mg_owner` equals the current user |
| `NONE` | No rows (policy not emitted; verb not granted) |

To grant a user access to both own rows and group rows, assign them two roles in different groups — one with `OWN` scope and one with `GROUP` scope. The `mg_can_read` and `mg_can_write` SQL functions union all matching role memberships.

The read-only privacy modes `EXISTS`, `COUNT`, `RANGE`, and `AGGREGATE` are `select`-only values. They grant visibility of the full table but the application layer restricts what is returned (presence check, count only, numeric range, or aggregate values).

`changeOwner: true` grants the role column-level UPDATE on `mg_owner`. `changeGroup: true` grants column-level INSERT/UPDATE on `mg_groups`.

## Worked example: schema-wide grant with mixed scopes

Role `staff` is defined with three tables at different scopes:

```graphql
mutation {
  change(roles: [{
    name: "staff"
    tables: [
      { table: "Books",   select: GROUP }
      { table: "Drafts",  select: OWN   }
      { table: "Catalog", select: ALL   }
    ]
  }]) { message }
}
```

Grant `alice` the `staff` role schema-wide (no group):

```graphql
mutation {
  change(members: [{ user: "alice@example.org", role: "staff" }]) {
    message
  }
}
```

What alice can read:

| Table | Scope | Result |
|---|---|---|
| `Books` | `GROUP` | Nothing — schema-wide grant has no group to match `mg_groups` |
| `Drafts` | `OWN` | Her own rows only |
| `Catalog` | `ALL` | All rows |

To give alice access to `Books` rows tagged `science_dept`, add a group-scoped grant:

```graphql
mutation {
  change(members: [
    { user: "alice@example.org", role: "staff", group: "science_dept" }
  ]) { message }
}
```

Now alice sees `Books` rows where `mg_groups` contains `science_dept`, in addition to all `Catalog` rows and her own `Drafts`.

## Worked example: asymmetric group access

This example shows a user who can write rows tagged with one group and only read rows tagged with another.

### Setup

Schema `Library` with table `Books`. The `mg_owner` and `mg_groups` columns are added automatically when the first custom role with `OWN` or `GROUP` scope is applied to `Books`.

Create two groups:

```graphql
mutation {
  change(groups: [
    { name: "science_dept" }
    { name: "humanities_dept" }
  ]) {
    message
  }
}
```

Create a `staff` role with full group-scoped access to `Books`:

```graphql
mutation {
  change(roles: [{
    name: "staff"
    tables: [{ table: "Books", select: GROUP, insert: GROUP, update: GROUP, delete: GROUP }]
  }]) {
    message
  }
}
```

Create a `reader` role with read-only group-scoped access to `Books`:

```graphql
mutation {
  change(roles: [{
    name: "reader"
    tables: [{ table: "Books", select: GROUP, insert: NONE, update: NONE, delete: NONE }]
  }]) {
    message
  }
}
```

Add `alice` to `science_dept` with the `staff` role, and to `humanities_dept` with the `reader` role:

```graphql
mutation {
  change(members: [
    { user: "alice@example.org", role: "staff",  group: "science_dept"   }
    { user: "alice@example.org", role: "reader", group: "humanities_dept" }
  ]) {
    message
  }
}
```

### Row tagging

Insert rows and tag them with groups:

```graphql
mutation {
  Books {
    insert(value: [
      { title: "The Selfish Gene",         mg_groups: ["science_dept"]   }
      { title: "Hamlet",                   mg_groups: ["humanities_dept"] }
      { title: "An Untagged Internal Doc", mg_groups: []                 }
    ]) { message }
  }
}
```

### What alice sees

Query as alice:

```graphql
{
  Books { title }
}
```

| Row | alice can read | alice can write |
|---|---|---|
| `mg_groups = ["science_dept"]` | yes (`staff` has `select: GROUP`) | yes (`staff` has `insert/update/delete: GROUP`) |
| `mg_groups = ["humanities_dept"]` | yes (`reader` has `select: GROUP`) | no (`reader` has `insert/update/delete: NONE`) |
| `mg_groups = []` | no (no role matches) | no |

### What just happened

MOLGENIS evaluated RLS policies at the PostgreSQL layer for each row alice touched. For each row it intersected `mg_groups` with the groups alice belongs to and matched the row against her active roles. The `science_dept` row matched alice's `staff` role — a `GROUP` scope match — granting all four verbs. The `humanities_dept` row matched her `reader` role, which has `select: GROUP` but all write verbs at `NONE`, so PostgreSQL allowed the SELECT policy and denied the INSERT/UPDATE/DELETE policies. The untagged row matched no policy and was invisible, as if it did not exist.

## What is not covered here

- **Cross-schema custom roles**: custom roles are scoped to a single schema. Cross-schema access is not supported.
- **System role override**: users with a system role (Owner, Manager, Editor, Viewer) are matched inside the RLS policy via `pg_has_role(current_user, 'MG_ROLE_<schema>/<role>', 'MEMBER')`. The `mg_can_read` and `mg_can_write` SQL functions check this membership and grant unconditional access for system role holders. No `BYPASSRLS` attribute is used; the policy itself evaluates to `true` for system role members.

## Implementation notes

### Role storage

Permission data is stored in three `MOLGENIS` catalog tables:

- `MOLGENIS.groups_metadata` — per-schema group definitions (`schema`, `name`, `users`).
- `MOLGENIS.group_membership_metadata` — per-user per-group per-role assignments (`user_name`, `schema_name`, `group_name`, `role_name`). Schema-wide custom-role grants are stored here using a reserved sentinel value for `group_name`; this sentinel is hidden from `_schema.groups` query results and is not a real group.
- `MOLGENIS.role_permission_metadata` — per-role per-table scope rows (`schema_name`, `role_name`, `table_name`, `select_scope`, `insert_scope`, `update_scope`, `delete_scope`, `change_owner`, `change_group`). System roles (Owner, Manager, Editor, Viewer) have a wildcard row `table_name = '*'` seeded by migration 32.

PostgreSQL roles still exist for system roles (`MG_ROLE_<schema>/Owner`, etc.) and one umbrella `MG_ROLE_<schema>_MEMBER` role per schema. Custom roles are no longer represented as PG roles and carry no `COMMENT ON ROLE` JSON.

### Policy emission

When `setPermissions` is called all existing non-sentinel rows for the role are deleted and replaced. When the first non-`NONE` scope appears on a table, `enableRlsForTable` fires:

- `ENABLE ROW LEVEL SECURITY` + `FORCE ROW LEVEL SECURITY` on the table.
- `mg_groups TEXT[]` and `mg_owner TEXT` columns are added.
- Four policies are created using the `mg_can_read` and `mg_can_write` SQL functions as their USING/WITH CHECK expressions.
- A `mg_check_change_capability` trigger enforces `changeOwner` / `changeGroup` constraints at write time.

When the last non-`NONE` scope is removed from a table, `disableRlsForTable` tears down the policies and reverts `DISABLE ROW LEVEL SECURITY`.

Column-level grants for `mg_owner` and `mg_groups` are controlled by the `changeOwner` / `changeGroup` flags.

### `schemaName` validation

If `PermissionSet.schema` is set, `SqlRoleManager.setPermissions` throws `MolgenisException` when it does not match the target schema. This catches accidental cross-schema mutations at the API layer.
