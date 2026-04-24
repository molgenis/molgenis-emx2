# Fine-grained permissions

Standard schema roles (viewer, editor, manager, etc.) grant uniform access to every table in a schema. Fine-grained roles give you table-level control: you define a named role, specify exactly which tables it can read, insert, update, or delete, how broadly (all rows, only the user's own rows, or rows tagged with a shared group), and then assign users to that role.

Fine-grained roles are global (not schema-scoped): one role can carry permissions across multiple schemas. Use `schema: "*"` in a permission entry to match all schemas. Scoped OWN and GROUP verbs require row-level security (RLS) to be enabled on the target table; ALL scope uses a plain PostgreSQL GRANT and does not require RLS.

All mutations described here require an admin session. Non-admin callers receive a `FAILED` result with the message `admin only`.

---

## Scope model

Every permission entry on a role carries four verbs (`select`, `insert`, `update`, `delete`) and two boolean flags (`changeOwner`, `share`).

### ViewScope (select)

The four values below are enforced server-side today. The remaining twelve are declared in the GraphQL schema for future use; the server rejects them with an error if sent in a mutation.

**Supported**

| Value | Meaning |
|---|---|
| `NONE` | No read access |
| `OWN` | Rows where `mg_owner = session_user` |
| `GROUP` | Rows where `mg_roles` overlaps the caller's granted roles |
| `ALL` | Every row in the table |

**Planned placeholders (server rejects these today)**

| Family | Values |
|---|---|
| count | `COUNT_OWN`, `COUNT_GROUP`, `COUNT_ALL` |
| aggregate | `AGGREGATE_OWN`, `AGGREGATE_GROUP`, `AGGREGATE_ALL` |
| exists | `EXISTS_OWN`, `EXISTS_GROUP`, `EXISTS_ALL` |
| range | `RANGE_OWN`, `RANGE_GROUP`, `RANGE_ALL` |

### EditScope (insert, update, delete)

| Value | Meaning |
|---|---|
| `NONE` | Operation not permitted |
| `OWN` | Rows where `mg_owner = session_user` |
| `GROUP` | Rows where `mg_roles` overlaps the caller's granted roles (INSERT uses stricter subset check) |
| `ALL` | Every row in the table |

### Boolean flags

- `changeOwner` — allows UPDATE mutations that change `mg_owner`. Requires `update >= OWN`.
- `share` — allows UPDATE mutations that change `mg_roles`. Requires `update >= OWN`.

---

## Mutations

Both mutations run inside a single database transaction. A failure in any step rolls back the entire call.

### changePermissions

Creates or updates roles, enables RLS on tables, sets per-role per-table grants, and assigns users to roles. All four argument lists are optional; include only the ones you need in a given call.

Execution order inside the transaction: **roles → tables → permissions → members**.

**Input shape**

```graphql
mutation {
  changePermissions(
    roles: [{ name: String!, description: String }]
    tables: [{ schema: String!, table: String!, rowLevelSecurity: Boolean! }]
    permissions: [{
      role: String!
      permissions: [{
        schema: String!
        table: String!
        select: MolgenisViewScope
        insert: MolgenisEditScope
        update: MolgenisEditScope
        delete: MolgenisEditScope
        changeOwner: Boolean
        share: Boolean
      }]
    }]
    members: [{ role: String!, user: String! }]
  ) {
    status
    message
  }
}
```

The `permissions` list for a role is **replace-all**: it replaces the role's entire permission set for the listed (schema, table) pairs in one atomic operation.

**Example — create a role, enable RLS, set SELECT=ALL + UPDATE=OWN, add a member**

```graphql
mutation {
  changePermissions(
    roles: [{ name: "clinician", description: "Clinician read/own-update" }]
    tables: [{ schema: "trial_data", table: "Patients", rowLevelSecurity: true }]
    permissions: [{
      role: "clinician"
      permissions: [{
        schema: "trial_data"
        table: "Patients"
        select: ALL
        update: OWN
      }]
    }]
    members: [{ role: "clinician", user: "alice@example.com" }]
  ) {
    status
    message
  }
}
```

### dropPermissions

Removes custom roles and/or revokes role memberships. Built-in roles (viewer, editor, manager, owner, aggregator, range, exists, count) cannot be dropped; the mutation returns `FAILED` if any entry targets a built-in.

When a role is dropped, all its RLS policies are removed, it is revoked from all users, and its name is tombstoned to prevent accidental reuse.

**Input shape**

```graphql
mutation {
  dropPermissions(
    roles: [String!]
    members: [{ role: String!, user: String! }]
  ) {
    status
    message
  }
}
```

**Example — revoke a membership and drop a role**

```graphql
mutation {
  dropPermissions(
    members: [{ role: "clinician", user: "alice@example.com" }]
    roles: ["clinician"]
  ) {
    status
    message
  }
}
```

---

## Queries

### permission (admin-only)

Returns all roles with their permission sets and current members. Non-admin callers receive an empty `roles` list.

```graphql
{
  permission {
    roles {
      role
      description
      immutable
      permissions {
        schema
        table
        select
        insert
        update
        delete
        changeOwner
        share
      }
      members
    }
  }
}
```

### _session.effectivePermissions

Returns the **caller's** resolved permissions — the union of permissions across all roles granted to that user, with wildcards expanded. Available to any authenticated user. The result reflects what the caller can actually do, not what any individual role declares.

```graphql
{
  _session {
    effectivePermissions {
      schema
      table
      select
      insert
      update
      delete
      changeOwner
      share
    }
  }
}
```

---

## RLS prerequisite

Scopes `OWN` and `GROUP` on any verb require `rowLevelSecurity: true` on the target table. Setting those scopes without RLS causes the mutation to return `FAILED`. You can enable RLS in the same `changePermissions` call via the `tables` argument — the `tables` step runs before the `permissions` step, so a single call is sufficient.

Enabling RLS adds two columns to the table if they are not already present:

- `mg_owner text DEFAULT session_user` — populated automatically on INSERT; identifies the row's owner.
- `mg_roles text[] DEFAULT '{}'` — used for GROUP-scoped access; set explicitly by the inserting user.

The admin role carries the PostgreSQL `BYPASSRLS` attribute, so admin sessions always see all rows regardless of RLS policies.

---

## changeOwner and share

Both flags gate UPDATE operations that modify the reserved columns:

- Without `changeOwner`, any UPDATE that changes `mg_owner` is rejected by a database trigger.
- Without `share`, any UPDATE that changes `mg_roles` is rejected by a database trigger.

Both flags require `update >= OWN`. The trigger runs server-side and is enforced for direct database connections as well as API calls.

---

## End-to-end example

The following steps show a complete admin session setting up a role and a user session verifying it.

**Step 1 — sign in as admin**

```http
POST /api/graphql
{ "query": "mutation { signin(email: \"admin\", password: \"admin\") { status } }" }
```

**Step 2 — create role, enable RLS, set permissions, add member (one call)**

```graphql
mutation {
  changePermissions(
    roles: [{ name: "clinician", description: "Clinician access" }]
    tables: [{ schema: "trial_data", table: "Patients", rowLevelSecurity: true }]
    permissions: [{
      role: "clinician"
      permissions: [{
        schema: "trial_data"
        table: "Patients"
        select: ALL
        update: OWN
      }]
    }]
    members: [{ role: "clinician", user: "alice@example.com" }]
  ) {
    status
    message
  }
}
```

Expected response: `{ "data": { "changePermissions": { "status": "SUCCESS", "message": "Permissions updated" } } }`

**Step 3 — sign in as alice**

```http
POST /api/graphql
{ "query": "mutation { signin(email: \"alice@example.com\", password: \"...\") { status } }" }
```

**Step 4 — alice checks her effective permissions**

```graphql
{
  _session {
    effectivePermissions {
      schema
      table
      select
      update
    }
  }
}
```

Expected: an entry with `schema: "trial_data"`, `table: "Patients"`, `select: "ALL"`, `update: "OWN"`.

**Step 5 — alice reads all rows (SELECT=ALL)**

```graphql
{ Patients { id name } }
```

All rows are returned regardless of `mg_owner`.

**Step 6 — alice tries to update a row she does not own**

```graphql
mutation { update(Patients: [{ id: "r1", name: "changed" }]) { message } }
```

The RLS USING clause (`mg_owner = session_user`) hides rows owned by other users from UPDATE. The mutation returns without error but affects 0 rows.

**Step 7 — admin drops the role**

```graphql
mutation {
  dropPermissions(roles: ["clinician"]) {
    status
    message
  }
}
```

**Step 8 — alice's effective permissions are now empty for this table**

```graphql
{ _session { effectivePermissions { schema table } } }
```

The `trial_data / Patients` entry is gone.

---

## Known gaps

- `ViewScope` placeholder families `count`, `aggregate`, `exists`, and `range` (twelve values total) are declared in the GraphQL schema and visible in introspection, but the server throws an error if any of them is sent in a mutation. They are planned for a future release.
- The `permissions` list inside `changePermissions` performs a replace-all for the affected (schema, table) pairs rather than a diff-and-patch. Unchanged tables still receive the same DDL as changed ones. This is correct but does more work than necessary; a diff-and-patch optimisation is on the backlog.
- When a fine-grained role is granted any permission in a schema, the schema-level `MG_ROLE_<schema>/Exists` role is automatically granted to it so the user can locate the schema. Revoking all permissions in that schema revokes the schema grant too.
