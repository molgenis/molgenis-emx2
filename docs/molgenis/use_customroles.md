# Custom roles and row-level security

Custom roles let a Manager or Owner define fine-grained, per-table access rules that go beyond the built-in system roles (Viewer, Editor, etc.). Each custom role carries a set of per-table verb scopes that PostgreSQL enforces through row-level security (RLS) policies.

## Concepts

### Scopes

**Select scope** — controls which rows a role can read:

| Scope | Rows visible |
|---|---|
| `NONE` | No rows |
| `OWN` | Rows where `mg_owner` matches the current user |
| `GROUP` | Rows where `mg_groups` overlaps the current user's groups |
| `OWN_OR_GROUP` | Union of OWN and GROUP |
| `EXISTS` | Row exists (no data, only presence) |
| `COUNT` | Row count only |
| `RANGE` | Row count rounded to nearest 10 |
| `AGGREGATE` | Aggregates only (no individual rows) |
| `ALL` | All rows |

**Update scope** — controls which rows a role can insert, update, or delete:

| Scope | Rows affected |
|---|---|
| `NONE` | No rows |
| `OWN` | Rows where `mg_owner` matches the current user |
| `GROUP` | Rows where `mg_groups` overlaps the current user's groups |
| `ALL` | All rows |

### Ownership columns

Two special columns carry row-level identity:

- `mg_owner` — a single username string. Set at insert time to track who created the row.
- `mg_groups` — a text array of group names. Rows can belong to one or more groups.

By default a role cannot write to these columns even if it has insert or update scope. Two flags unlock this:

- `changeOwner: true` — grants column-level INSERT and UPDATE privilege on `mg_owner`.
- `changeGroup: true` — grants column-level INSERT and UPDATE privilege on `mg_groups`.

### Groups

Groups are named sets of users. A user belongs to a group by being added as a group member. Row-level GROUP policies evaluate the current user's group membership at query time via the `current_user_groups()` SQL function.

## Managing custom roles

Custom roles are managed by Managers and Owners through the `change` mutation on `_schema`.

### Create or update a role

```graphql
mutation {
  change(roles: [{
    name: "researcher"
    description: "Can read own data and insert new records"
    tables: [
      { table: "Sample", select: OWN, insert: ALL, update: OWN, delete: NONE }
      { table: "Result", select: GROUP, insert: NONE, update: NONE, delete: NONE }
    ]
    changeOwner: false
    changeGroup: false
  }]) {
    message
  }
}
```

Calling `change` on an existing role is idempotent — it replaces the full permission set.

### Query all roles

```graphql
{
  _schema {
    roles {
      name
      description
      schemaName
      tables {
        table
        select
        insert
        update
        delete
      }
      changeOwner
      changeGroup
    }
  }
}
```

Both system roles and custom roles are returned in the same list. System roles carry an empty `tables` list and their name as `description`.

### Delete a role

```graphql
mutation {
  drop(roles: ["researcher"]) {
    message
  }
}
```

## Managing groups

### Create or update a group and its members

```graphql
mutation {
  change(groups: [{
    name: "cohort-a"
    users: ["alice", "bob"]
  }]) {
    message
  }
}
```

Providing `users` replaces the full member list. Omitting `users` leaves the current membership unchanged. Creating a group that already exists is a no-op.

### Delete groups

```graphql
mutation {
  drop(groups: ["cohort-a"]) {
    message
  }
}
```

### Query groups

```graphql
{
  _schema {
    groups {
      name
      users
    }
  }
}
```

## Assigning roles to users

Custom roles are assigned the same way as system roles, via the `members` argument:

```graphql
mutation {
  change(members: [{ email: "alice", role: "researcher" }]) {
    message
  }
}
```
