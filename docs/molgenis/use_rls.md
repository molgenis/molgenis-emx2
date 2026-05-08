# Row-Level Security (RLS)

## Overview

Master's permission model grants roles access to entire tables via PostgreSQL `GRANT` statements. Row-level security (RLS) adds a second, strictly additive layer that filters individual rows within those tables. A user who holds no PG GRANT on a table is turned away before any row filter fires. A user who holds a GRANT is then filtered by the RLS policy — they see only the rows that their role's scope allows.

RLS is opt-in per table. Non-RLS tables are governed solely by the existing GRANT model; enabling RLS on a table does not change the GRANTs on other tables.

Key concepts:

| Concept | What it is |
|---|---|
| `MG_ROLE_<schema>/<role>` | PostgreSQL role. Created by `Schema.createRole`. Grants give this role SELECT/INSERT/UPDATE/DELETE on individual tables. |
| `role_permission_metadata` | Per `(schema, role, table)` row storing `select_scope`, `insert_scope`, `update_scope`, `delete_scope`, `change_owner`, `change_group`. Custom roles only; system roles use hardcoded scopes. |
| `group_membership_metadata` | Per `(user, schema, group?, role)` row. `group = NULL` = schema-wide binding. `group = <name>` = group-scoped binding for GROUP-scope row evaluation. |
| `mg_owner` | Column materialised on RLS-enable. `REF → MOLGENIS.users_metadata`. Backfilled from `mg_insertedBy`. |
| `mg_groups` | Column materialised on RLS-enable. `REF_ARRAY → MOLGENIS.groups_metadata`. Controls GROUP-scope visibility. |
| `mg_can_read` | PL/pgSQL function. Evaluates SELECT scope per row from `role_permission_metadata` + `group_membership_metadata`. Called by the SELECT policy `USING` clause. |
| `mg_can_write` | Evaluates INSERT/UPDATE/DELETE scope per row for existing-row visibility. Called by INSERT `WITH CHECK`, UPDATE `USING`, and DELETE `USING` clauses. |
| `mg_can_write_all` | Extends `mg_can_write` with group-subset enforcement on new/updated row groups, plus `change_owner`/`change_group` capability checks. Called by INSERT and UPDATE `WITH CHECK` clauses. |


## Enabling RLS on a table

RLS can only be enabled on the **root** of an inheritance tree. Attempts on a subclass are rejected with `"enable on root '<X>' instead"`.

Enable via GraphQL:

```graphql
mutation {
  change(tables: [{name: "Patient", rlsEnabled: true}]) {
    message
  }
}
```

What happens on enable:

1. Columns `mg_owner` and `mg_groups` are added to the table (and all subclass tables in the inheritance tree).
2. `mg_owner` is backfilled from `mg_insertedBy` for existing rows.
3. `ALTER TABLE … ENABLE ROW LEVEL SECURITY` and `FORCE ROW LEVEL SECURITY` are applied.
4. Four policies are emitted (see `migration32.sql` for the exact template):

```sql
CREATE POLICY mg_p_<table>_select ON <schema>.<table> FOR SELECT
  USING ( MOLGENIS.mg_can_read(<schema>, <table>, mg_groups, mg_owner) );

CREATE POLICY mg_p_<table>_insert ON <schema>.<table> FOR INSERT
  WITH CHECK ( MOLGENIS.mg_can_write_all(<schema>, <table>, mg_groups, mg_owner, 'insert', ...) );

CREATE POLICY mg_p_<table>_update ON <schema>.<table> FOR UPDATE
  USING      ( MOLGENIS.mg_can_write    (<schema>, <table>, mg_groups, mg_owner, 'update') )
  WITH CHECK ( MOLGENIS.mg_can_write_all(<schema>, <table>, mg_groups, mg_owner, 'update', ...) );

CREATE POLICY mg_p_<table>_delete ON <schema>.<table> FOR DELETE
  USING ( MOLGENIS.mg_can_write(<schema>, <table>, mg_groups, mg_owner, 'delete') );
```

Enable cascades atomically through the entire inheritance tree. Subclass tables inherit the parent's flag at runtime — child rows in `tables_metadata` always store `false`; the code walks to the root to read the effective value.

Disable is the reverse, but is rejected if any `role_permission_metadata` rows exist for the table ("first remove permissions on '<schema>.<table>'").


## Scope model

### SELECT scope ladder

```
NONE | EXISTS | COUNT | RANGE | AGGREGATE | OWN | GROUP | ALL
```

Two enforcement layers share a single column:

**Privacy projection layer** (`EXISTS | COUNT | RANGE | AGGREGATE`) — applies to any table, RLS or not. Enforced by `SqlQuery` after the rows are fetched:

| Scope | Projection behaviour |
|---|---|
| `EXISTS` | Result clamped to presence/absence boolean (0 or 1). |
| `COUNT` | `GREATEST(COUNT(*), 10)` — real count when above floor; floor of 10 below. |
| `RANGE` | Histogram bucket sizes rounded up to the nearest 10 via `MOLGENIS.mg_privacy_count`. |
| `AGGREGATE` | SUM/AVG returned only when the underlying count is 10 or more; NULL otherwise. |

At the policy layer, privacy scopes pass through as `true` — the policy does not filter rows; `SqlQuery` clamps the result.

**Row-filter layer** (`OWN | GROUP`) — RLS tables only. Enforced by the SELECT policy `USING` clause:

| Scope | Rows visible |
|---|---|
| `OWN` | Rows where `mg_owner = current_user`. |
| `GROUP` | Rows where `mg_groups` overlaps the user's group memberships in this schema. |
| `ALL` | All rows (subject to GRANT). |
| `NONE` | No rows — GRANT is also revoked, so `SqlQuery` rejects the query before reaching any policy. |

### Write scope ladder

INSERT, UPDATE, DELETE each use: `NONE | OWN | GROUP | ALL`.

`mg_can_write_all` adds a group-subset check for INSERT and UPDATE: every group in the new row's `mg_groups` must be one that the writer holds GROUP-or-ALL write authority in. This prevents tagging a row into a group the writer cannot access.

### System role scopes (hardcoded)

System roles have no rows in `role_permission_metadata`. Their scopes are evaluated in the access functions directly:

| Role | select | insert | update | delete | change_owner | change_group |
|---|---|---|---|---|---|---|
| Owner | ALL | ALL | ALL | ALL | true | true |
| Manager | ALL | ALL | ALL | ALL | true | true |
| Editor | ALL | ALL | ALL | ALL | false | false |
| Viewer | ALL | NONE | NONE | NONE | false | false |


## Group membership model

`MOLGENIS.group_membership_metadata` stores every user–role binding in a schema:

- `group_name = NULL` — schema-wide binding. The user holds the role without group context. ALL-scope rules apply; OWN-scope resolves against `mg_owner`; GROUP-scope finds no overlap (no group context, so no group-tagged rows are visible through this binding alone).
- `group_name = <name>` — group-scoped binding. The user holds the role within a specific group. GROUP-scope evaluates `mg_groups` overlap against this group.

A user can hold the same role in multiple groups (one row per group).

**Schema-wide supersedes group-scoped (World A semantics):** granting a role to a user *without* a group deletes any existing group-bound rows for that `(user, role, schema)` combination before inserting the NULL-group row. The PG GRANT is issued once (idempotent) and revoked only when the last membership row is removed.

The PG GRANT (`GRANT MG_ROLE_<schema>/<role> TO MG_USER_<user>`) is the runtime gate — `pg_has_role` determines whether the user holds the role at all. The membership table provides group context for scope evaluation. Both are kept in sync by `Schema.changeMembers` / `dropMembers`.


## Worked examples

### (a) Public read-only catalogue

No RLS needed. Grant the built-in Viewer role to the special `anonymous` user so that any unauthenticated visitor can read all rows.

```graphql
mutation {
  change(members: [{user: "anonymous", role: "Viewer"}]) {
    message
  }
}
```

The Viewer's hardcoded scope is `select=ALL`. No `role_permission_metadata` rows are involved.

### (b) Per-user privacy — patients see only their own records

Enable RLS on the `Patient` table, create a `clinician` custom role with OWN scope, and grant it to user U1 schema-wide.

```graphql
mutation {
  change(
    tables: [{name: "Patient", rlsEnabled: true}]
    roles: [{
      name: "clinician"
      permissions: [{table: "Patient", select: OWN, update: OWN}]
    }]
    members: [{user: "U1", role: "clinician"}]
  ) {
    message
  }
}
```

U1 sees only rows where `mg_owner = 'MG_USER_U1'`. Updates to rows owned by other users are blocked by the UPDATE policy.

### (c) Group-bounded research — each department sees its own samples

Enable RLS on the `Sample` table, create groups DEPT1 and DEPT2, create a `researcher` role with GROUP scope, and add U1 to both groups.

```graphql
mutation {
  change(
    tables: [{name: "Sample", rlsEnabled: true}]
    roles: [{
      name: "researcher"
      permissions: [{table: "Sample", select: GROUP, update: GROUP}]
    }]
    groups: [{name: "DEPT1"}, {name: "DEPT2"}]
    members: [
      {user: "U1", role: "researcher", group: "DEPT1"},
      {user: "U1", role: "researcher", group: "DEPT2"}
    ]
  ) {
    message
  }
}
```

U1 sees rows whose `mg_groups` array contains `DEPT1` or `DEPT2`. Rows tagged with other groups are invisible. A row with an empty `mg_groups` array is visible to no GROUP-scoped user.


## Operator runbook — "user can't see row X"

Follow these steps in order.

### Step 1 — does the user hold a PG GRANT on the table?

```sql
SELECT grantee, table_name, privilege_type
FROM information_schema.role_table_grants
WHERE grantee LIKE 'MG_ROLE_%'
  AND table_schema = '<schema>'
  AND table_name   = '<table>';
```

The user's PG role is `MG_USER_<username>`. Cross-check with:

```sql
SELECT rolname
FROM pg_roles
WHERE pg_has_role('MG_USER_<username>', oid, 'MEMBER');
```

If no matching GRANT row exists, the GRANT layer itself is blocking access. Assign the role or grant the appropriate permission before investigating RLS.

### Step 2 — is the table RLS-enabled?

```sql
SELECT relname, relrowsecurity
FROM pg_class
WHERE relname = '<table>'
  AND relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = '<schema>');
```

If `relrowsecurity = false`, only the GRANT layer applies — no RLS policies fire. Check `role_permission_metadata` only if `relrowsecurity = true`.

### Step 3 — what is the user's effective scope?

```sql
SELECT rp.role_name, rp.select_scope, rp.insert_scope, rp.update_scope, rp.delete_scope,
       rp.change_owner, rp.change_group,
       m.group_name
FROM MOLGENIS.role_permission_metadata rp
JOIN MOLGENIS.group_membership_metadata m
  ON m.schema_name = rp.schema_name
 AND m.role_name   = rp.role_name
WHERE rp.schema_name = '<schema>'
  AND rp.table_name  = '<table>'
  AND m.user_name    = '<username>';
```

A user with no rows here has effective scope `NONE` for this table — `SqlQuery` will reject the query before any policy fires.

### Step 4 — does the row qualify the user?

Probe as that user:

```sql
SET ROLE "MG_USER_<username>";

SELECT id,
       MOLGENIS.mg_can_read('<schema>', '<table>', mg_groups, mg_owner) AS can_read
FROM "<schema>"."<table>"
WHERE id = '<row-id>';

RESET ROLE;
```

If `can_read = false`, the row's `mg_owner` and `mg_groups` do not match the user's scope or group memberships. Compare the row values with step 3 results.

To verify write access:

```sql
SET ROLE "MG_USER_<username>";

SELECT MOLGENIS.mg_can_write('<schema>', '<table>', mg_groups, mg_owner, 'update') AS can_update
FROM "<schema>"."<table>"
WHERE id = '<row-id>';

RESET ROLE;
```

### Step 5 — inheritance: check the root table's flag, not the child's

`rls_enabled` is stored only on the root row in `MOLGENIS.table_metadata`. Children always show `false`. To confirm:

```sql
SELECT table_name, rls_enabled
FROM MOLGENIS.table_metadata
WHERE table_schema = '<schema>'
  AND rls_enabled = true;
```

If the root's flag is `true` but policies appear missing, re-run `change(tables:[{name:"<root>", rlsEnabled:true}])` to trigger policy creation.


## Auditing

### Inventory of RLS policies

```sql
SELECT schemaname, tablename, policyname, cmd, qual
FROM pg_policies
WHERE schemaname = '<schema>'
ORDER BY tablename, policyname;
```

Each RLS-enabled table should have exactly four policies: `mg_p_<table>_select`, `mg_p_<table>_insert`, `mg_p_<table>_update`, `mg_p_<table>_delete`.

### Confirm the change-capability trigger is in place

```sql
SELECT trigger_name, event_manipulation, event_object_table
FROM information_schema.triggers
WHERE trigger_schema = '<schema>'
  AND trigger_name LIKE 'mg_check_change%'
ORDER BY event_object_table;
```

Each RLS-enabled table should have one trigger per INSERT and UPDATE event. The trigger calls `MOLGENIS.mg_check_change_capability()` and is created alongside the policies when RLS is enabled.

### Quick row-visibility check

Compare what different users see:

```sql
SET ROLE "MG_USER_U1";
SELECT count(*) FROM "<schema>"."<table>";
RESET ROLE;

SET ROLE "MG_USER_U2";
SELECT count(*) FROM "<schema>"."<table>";
RESET ROLE;
```

Note: this raw count is not subject to the `mg_privacy_count` floor — see Known limitations below.


## Known limitations

The following are explicitly out of scope for RLS v4:

- **Direct-SQL COUNT leaks unfloored count.** A user with `select_scope=COUNT` who runs `SELECT count(*) FROM <table>` directly (psql, JDBC) receives the exact row count without the floor of 10. The floor applies only through the GraphQL/REST/SqlQuery layer. Future enhancement: route counts through a `SECURITY DEFINER` function.
- **Direct-SQL bare FK reads are not filtered.** `SELECT fk_id FROM B` returns the raw FK column value even when the referenced row in A is invisible to the user. Through GraphQL, REST, or `SqlQuery.retrieveRows()` / `retrieveJSON()`, FK columns are protected via a LEFT JOIN that projects `NULL` for invisible targets. Raw JDBC or psql access is unprotected by design.
- **Cross-schema custom roles are not supported.** A custom role is scoped to a single schema. There is no mechanism to share a role definition across schemas.
- **User-defined privacy floor.** The floor of 10 for COUNT/RANGE/AGGREGATE is hardcoded in `mg_privacy_count` and `SqlQuery`. Operators cannot configure a different threshold per table or role.
- **Audit log of permission changes.** Changes to `role_permission_metadata` and `group_membership_metadata` are not logged to an audit trail.
- **Audit history per row.** The `_history` sibling tables are not covered by per-row ownership or group filtering.
