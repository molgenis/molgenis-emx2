# GraphQL row-level security reference

This page documents the GraphQL types, queries, and mutations added for row-level security (RLS) and custom roles.

## Enum types

### `MolgenisSelectScope`

Used for the `select` field of `MolgenisTablePermissionInput` and `MolgenisTablePermissionOutput`.

Values: `NONE`, `OWN`, `GROUP`, `OWN_OR_GROUP`, `EXISTS`, `COUNT`, `RANGE`, `AGGREGATE`, `ALL`

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

## Implementation notes

### Role storage

Custom roles are stored as PostgreSQL roles named `MG_ROLE_<schema>/<roleName>`. Their `PermissionSet` is serialized as JSON and stored in `COMMENT ON ROLE`.

### Policy emission

On every `setPermissions` call all existing RLS policies for the role are dropped and rebuilt from the current `PermissionSet`:

- `applySelectPolicy(SelectScope)` — emits a `SELECT` policy. View-mode scopes (`EXISTS`, `COUNT`, `RANGE`, `AGGREGATE`) emit a full-table `ALL` policy; the restriction happens at the application layer.
- `applyWritePolicy(UpdateScope, verb)` — emits INSERT, UPDATE, or DELETE policies using `NONE`/`OWN`/`GROUP`/`ALL` logic.
- Column-level grants for `mg_owner` and `mg_groups` are controlled by the `changeOwner` / `changeGroup` flags.

### `schemaName` validation

If `PermissionSet.schema` is set, `SqlRoleManager.setPermissions` throws `MolgenisException` when it does not match the target schema. This catches accidental cross-schema mutations at the API layer.
