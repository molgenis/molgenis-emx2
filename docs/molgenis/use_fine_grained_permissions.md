# Fine-grained permissions

Standard schema roles (viewer, editor, manager, etc.) grant uniform access to every table in a schema. Fine-grained roles give you table-level control: you define a named role, specify exactly which tables it can read, insert, update, or delete, how broadly (all rows, only the user's own rows, or rows tagged with a shared group), and then assign users to that role.

Fine-grained roles are global (not schema-scoped): one role can carry permissions across multiple schemas. Use `schema: "*"` in a permission entry to match all schemas. Scoped OWN and GROUP verbs automatically install row-level security (RLS) on the target table the first time such a scope is granted; ALL scope uses a plain PostgreSQL GRANT and does not require RLS.

Roles are stored as PostgreSQL roles named `MG_ROLE_<name>`; description lives in `COMMENT ON ROLE`.

All mutations described here require an admin session. Non-admin callers receive a `FAILED` result with the message `admin only`.

---

## Scope model

Every permission entry on a role carries four verbs (`select`, `insert`, `update`, `delete`) and two boolean flags (`changeOwner`, `share`). An optional `selectScope` field controls observability restrictions on `select`.

### Scope (all four verbs)

The same four values apply to every verb. All four are enforced server-side today.

| Value | Meaning |
|---|---|
| `NONE` | Operation not permitted |
| `OWN` | Rows where `mg_owner = session_user` |
| `GROUP` | Rows where `mg_roles` overlaps the caller's granted roles |
| `ALL` | Every row in the table |

### SelectScope (select only)

Controls what the caller can observe when `select` is not `NONE`. Only `FULL` is enforced today; the others are declared for future use and the server rejects them with an error if sent in a mutation.

| Value | Meaning |
|---|---|
| `FULL` | Full row data returned (default) |
| `COUNT` | Row count only — planned, not yet enforced |
| `AGGREGATE` | Statistical aggregates — planned, not yet enforced |
| `EXISTS` | Boolean existence check — planned, not yet enforced |
| `RANGE` | Min/max range — planned, not yet enforced |

### Boolean flags

- `changeOwner` — allows UPDATE mutations that change `mg_owner`. Requires `update >= OWN`.
- `share` — allows UPDATE mutations that change `mg_roles`. Requires `update >= OWN`.

---

## Mutations

All fine-grained permission operations use the existing `change` and `drop` mutations. Both run inside a single database transaction. A failure in any step rolls back the entire call.

### change (create/update roles, permissions, members)

Creates or updates roles, sets per-role per-table grants, and assigns users to roles. All argument lists are optional; include only the ones you need in a given call.

Execution order inside the transaction: **roles (including nested permissions) → members**.

When a role is granted OWN or GROUP scope on a table, RLS is installed automatically on that table if it is not already present (idempotent). See [How RLS turns on](#how-rls-turns-on) below.

**Input shape**

```graphql
mutation {
  change(
    roles: [{
      name: String!
      description: String
      permissions: [{
        schema: String!
        table: String!
        select: MolgenisEditScope
        insert: MolgenisEditScope
        update: MolgenisEditScope
        delete: MolgenisEditScope
        changeOwner: Boolean
        share: Boolean
        selectScope: MolgenisSelectScope
      }]
    }]
    members: [{ role: String!, user: String! }]
  ) {
    status
    message
  }
}
```

The `permissions` list inside a role is **replace-all**: it replaces the role's entire permission set for the listed (schema, table) pairs in one atomic operation. If `permissions` is absent, only the role definition (name, description) is created or updated.

**Example — create a role, set SELECT=ALL + UPDATE=OWN (RLS auto-installed), add a member**

```graphql
mutation {
  change(
    roles: [{
      name: "clinician"
      description: "Clinician read/own-update"
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

### drop (remove roles and/or revoke memberships)

Removes custom roles and/or revokes role memberships. Built-in roles (viewer, editor, manager, owner, aggregator, range, exists, count) cannot be dropped; the mutation returns `FAILED` if any entry targets a built-in.

When a role is dropped, all its RLS policies are removed, it is revoked from all users, and the PostgreSQL role is deleted. The name may be reused after deletion.

**Input shape**

```graphql
mutation {
  drop(
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
  drop(
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

### _admin.roles (admin-only)

Returns all roles with their permission sets and current members. Non-admin callers receive an empty result.

```graphql
{
  _admin {
    roles(name: String) {
      role
      description
      systemRole
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

The optional `name` argument filters to a single role by its base name.

### _session(schema).permissions

Returns the **caller's** resolved permissions — the union of permissions across all roles granted to that user, with wildcards expanded. Available to any authenticated user. The result reflects what the caller can actually do, not what any individual role declares.

If the `schema` argument is provided, only permissions matching that schema (plus global `"*"` permissions) are returned. If omitted, all permissions are returned.

```graphql
{
  _session(schema: "trial_data") {
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
  }
}
```

---

## How RLS turns on

When an admin grants an OWN or GROUP scoped permission on a table for the first time, RLS is installed automatically. The install sequence is:

1. Add `mg_owner text DEFAULT session_user` column (if absent).
2. Add `mg_roles text[] NOT NULL DEFAULT '{}'` column (if absent).
3. `ENABLE ROW LEVEL SECURITY` on the table.
4. `FORCE ROW LEVEL SECURITY` on the table.
5. Create a GIN index on `mg_roles` for `&&` / `<@` operator performance.
6. Install the `mg_enforce_row_authorisation` BEFORE UPDATE trigger.

The install is idempotent — running it a second time on an already-RLS-enabled table is safe.

Removing the last OWN or GROUP policy from a table (by clearing or replacing the role's permission set) drops the policies but keeps the infrastructure in place (`mg_owner`, `mg_roles`, trigger, RLS enabled, GIN index). The table remains in RLS mode until a full admin teardown. An admin who needs a full teardown must execute the DDL directly in PostgreSQL; there is no API for that in this version.

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

**Step 2 — create role, set permissions (RLS auto-installed by OWN scope), add member (one call)**

```graphql
mutation {
  change(
    roles: [{
      name: "clinician"
      description: "Clinician access"
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

Expected response: `{ "data": { "change": { "status": "SUCCESS", "message": "" } } }`

**Step 3 — sign in as alice**

```http
POST /api/graphql
{ "query": "mutation { signin(email: \"alice@example.com\", password: \"...\") { status } }" }
```

**Step 4 — alice checks her permissions**

```graphql
{
  _session(schema: "trial_data") {
    permissions {
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
  drop(roles: ["clinician"]) {
    status
    message
  }
}
```

**Step 8 — alice's permissions are now empty for this table**

```graphql
{ _session(schema: "trial_data") { permissions { schema table } } }
```

The `trial_data / Patients` entry is gone.

---

## Known gaps

- `MolgenisSelectScope` values `COUNT`, `AGGREGATE`, `EXISTS`, and `RANGE` are declared in the GraphQL schema and visible in introspection, but the server throws an error if any of them is sent in a mutation (`selectScope` field). They are planned for a future release.
- The `permissions` list inside `change` performs a replace-all for the affected (schema, table) pairs rather than a diff-and-patch. Unchanged tables still receive the same DDL as changed ones. This is correct but does more work than necessary; a diff-and-patch optimisation is on the backlog.
- When a fine-grained role is granted any permission in a schema, the schema-level `MG_ROLE_<schema>/Exists` role is automatically granted to it so the user can locate the schema. Revoking all permissions in that schema revokes the schema grant too.
