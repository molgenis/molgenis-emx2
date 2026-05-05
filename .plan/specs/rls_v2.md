# RLS v2 — Behavioural Spec

Living guardrails for the RLS v2 feature. Each row is a behaviour the system
must satisfy. `Test` links to the validating test (file:method or test class).
`Visual` is "visual check" if there is no automated assertion. Open
requirements (no test yet) live in the **Open requirements** section below.

## Schema-level

| Behaviour | Component | Test | Visual |
|---|---|---|---|
| System roles (Owner/Manager/Editor/Viewer) behave identically to master | `SqlSchema`, `SqlRoleManager` | `TestSchemaPermissions` | — |
| Custom roles are schema-local (a role spans only one schema) | `SqlRoleManager.createRole` | `SqlRoleManagerTest.createRole_*` | — |
| Many roles per (user, schema) via per-group assignment (Phase 7; was: exclusive in v2) | `MOLGENIS.group_membership_metadata` PK | _(open — Phase 7, see asymmetric-collaboration table)_ | — |
| System roles have NO `BYPASSRLS`; their authority comes from seeded immutable rows in `role_permission_metadata` (Phase 7) | `MetadataUtils.seedSystemRoles`, RLS policy emitter | _(open — Phase 7)_ | — |
| System role rows in `role_permission_metadata` are immutable post-seed (UPDATE/DELETE rejected at trigger + Java + GraphQL layers) | `role_permission_metadata` trigger, `SqlRoleManager.upsertPermissions`, `GraphqlSchemaFieldFactory.change` | _(open — Phase 7)_ | — |

## Group-level

| Behaviour | Component | Test | Visual |
|---|---|---|---|
| User can belong to multiple groups in one schema | `MOLGENIS.groups_metadata` | `TestGraphqlGroups.userInMultipleGroups` | — |
| `GROUP` scope shows rows whose `mg_groups` intersects the user's groups | `current_user_groups()` SQL function | `TestTablePolicies.groupScopeFiltersByMembership` | — |
| Removing user from group revokes group-scoped row access immediately | `removeGroupMember` | `TestGraphqlGroups.removeMemberRevokesAccess` | — |

## Per-table per-verb

| Behaviour | Component | Test | Visual |
|---|---|---|---|
| `select_scope` ladder NONE/EXISTS/COUNT/RANGE/AGGREGATE/OWN/GROUP/ALL | `SqlQuery` (view modes) + RLS policies | `TestSelectScope.*` | — |
| `insert/update/delete_scope` ladder NONE/OWN/GROUP/ALL | RLS policies | `TestUpdateScope.*` | — |
| `OWN` scope enforced via `mg_owner = current_user` | RLS policy emission | `TestRowOwnership.ownScope` | — |

## Capability flags

| Behaviour | Component | Test | Visual |
|---|---|---|---|
| `changeOwner` boolean controls whether user may UPDATE `mg_owner` column | column GRANT + RLS | `TestChangeOwner` | — |
| `changeGroup` boolean controls whether user may UPDATE `mg_groups` column | column GRANT + RLS | `TestChangeGroup` | — |

## Storage

| Behaviour | Component | Test | Visual |
|---|---|---|---|
| Role + permission storage in normalized `MOLGENIS.role_permission_metadata` + `MOLGENIS.group_membership_metadata` (Phase 7) | `MetadataUtils`, `SqlRoleManager` | _(open — Phase 7)_ | — |
| Dropping a schema cascades to its role permission rows and memberships | FK ON DELETE CASCADE | _(open — Phase 7)_ | — |
| Dropping a table cascades to its per-role permission rows | FK ON DELETE CASCADE | _(open — Phase 7)_ | — |

## GraphQL surface

| Behaviour | Component | Test | Visual |
|---|---|---|---|
| `_schema.roles` returns merged system + custom roles in one shape | `GraphqlSchemaFieldFactory` | `TestGraphqlPermissions.rolesQuery_returnsMerged` | — |
| `change(roles: [...])` mutation upserts role permissions | `GraphqlSchemaFieldFactory.change` | `TablePermissionsGraphqlTest.*` | — |
| `change(groups: [...])` / `delete(groups: [...])` use central mutation pattern | `GraphqlSchemaFieldFactory` | `TestGraphqlGroups.*` | — |

## Phase 7 acceptance test (asymmetric collaboration)

This is the gate for Phase 7. v2 cannot pass it; v3-shape design must.

| Behaviour | Component | Test | Visual |
|---|---|---|---|
| Alice as editor in group A and viewer in group B sees rows from both | `mg_can_read` | `TestAsymmetricCollaboration.readsRowsFromBothGroups` | — |
| Alice can UPDATE row tagged `[A]` (editor authority via group A) | `mg_can_write` | `TestAsymmetricCollaboration.updatesAGroupRow` | — |
| Alice cannot UPDATE row tagged `[B]` (viewer-only in B) | `mg_can_write` | `TestAsymmetricCollaboration.rejectsUpdateOfBOnlyRow` | — |
| Alice can move row tagged `[A,B]` to `[A]` (pulling out of B) | `mg_can_write_all` | `TestAsymmetricCollaboration.allowsRemovingGroupShe`HasNoAuthorityIn | — |
| Alice cannot share row into group C (no membership in C) | `mg_can_write_all` | `TestAsymmetricCollaboration.rejectsShareIntoForeignGroup` | — |
| Policy count per RLS table is exactly 4 (not per-role) | per-table policy emitter | `TestPolicyCount.fourPoliciesPerTable` | — |
| Direct-SQL count via COUNT-scoped role returns floored value | `mg_privacy_count` | `TestSelectScope.directSqlCountIsFloored` | — |

## Open requirements

### REQ-1: Per-(user, group) role differentiation — IN SCOPE FOR PHASE 7

**Use case:** Alice is editor in study A and viewer in study B, same schema.

**Decision:** accepted, in scope for Phase 7. Storage uses
`MOLGENIS.group_membership_metadata(user_name, schema_name, group_name, role_name)`
join table. Custom-role exclusivity rule is dropped. See Phase 7 in
`.plan/plans/rls_v2.md` and the acceptance-test table above.

### REQ-2: BYPASSRLS on system roles — RESOLVED (Path A)

**Concern:** System roles (Owner/Manager/Editor/Viewer) inherited `BYPASSRLS`
from master. Implications:
- A user acting through any system role skipped RLS entirely.
- Mixed-role users `SET ROLE`d to the most permissive system role,
  bypassing custom-role RLS guarantees.
- Compromise of any system-role account exposed ALL rows regardless of
  `mg_owner` / `mg_groups` tagging.

**Decision (locked):** **Path A — drop `BYPASSRLS`; materialise system
roles as immutable rows in `role_permission_metadata`.**

On schema create (and on Phase 7 migration of pre-existing schemas), seed:
- `Owner`,   `*`, select=ALL, insert=ALL, update=ALL, delete=ALL, change_owner=true,  change_group=true
- `Manager`, `*`, select=ALL, insert=ALL, update=ALL, delete=ALL, change_owner=true,  change_group=true
- `Editor`,  `*`, select=ALL, insert=ALL, update=ALL, delete=ALL, change_owner=false, change_group=false
- `Viewer`,  `*`, select=ALL, insert=NONE, update=NONE, delete=NONE, change_owner=false, change_group=false

(`*` = wildcard sentinel applied at access-function lookup; one row per
system role per schema, not per-table.)

**Why Path A over the allow-all-policy-branch alternative (Path B):**
- single source of truth — one storage, one emitter, one access-function
  lookup; `_schema.roles` already returns merged shape.
- uniform enforcement — RLS becomes the actual authorization layer, not
  advisory. Custom-role test fixtures also exercise system-role paths.
- per-table revocability for free — future "freeze table X to OWN-only
  even for Editors" is a row update, not a structural change.
- compromise blast-radius is auditable in `pg_policy` and per-table
  revocable; a leaked system-role token still gets ALL rows but through a
  visible, overridable code path.

**System-role row immutability (enforced in three places):**
1. `BEFORE UPDATE` / `BEFORE DELETE` trigger on `role_permission_metadata`
   rejecting writes where `role_name IN ('Owner','Manager','Editor','Viewer')`.
   INSERT path is allowed only via the schema-create seeder (not surfaced
   to UI/GraphQL).
2. `SqlRoleManager.upsertPermissions` and `deleteRole` reject the system
   role list at the Java layer.
3. GraphQL `change(roles:)` filters out the system role names with a
   user-facing error.

**Performance cost:** one `pg_has_role`-equivalent or membership-join
per row in the policy predicate — short-circuited by the planner.
Benchmark in slice 7.C; fall back to materialised view if pathological.

**Status:** **closed**, locked into Phase 7 slice 7.A. Slice 7.B
(foundation) implements the seeder + trigger; slice 7.C (`SqlRoleManager`
rewrite) implements the Java guards.

### REQ-3: Column-level `change_owner` / `change_group` enforcement model — DECIDE IN 7.A

**Concern:** v2 uses `MG_ROLE_<role>` PG-role inheritance for column-level
INSERT/UPDATE GRANTs on `mg_owner` / `mg_groups`. Under per-group role
membership, this collapses to "any group is enough" — a user with
`change_owner=true` on role X in group A can edit `mg_owner` for ANY row,
including rows where their per-group role in B has `change_owner=false`.

**Options:**
1. Keep `MG_ROLE_<role>` GRANT path; document the "any-group-is-enough"
   semantic as intended.
2. Drop the column GRANT; enforce `mg_owner` / `mg_groups` mutability via
   a verb-aware predicate inside `mg_can_write_all` that checks the
   column being changed vs the user's per-group `change_*` flags.
3. Hybrid: column GRANT for ALL-scope writers; predicate enforcement for
   OWN/GROUP-scope writers.

**Status:** decide in slice 7.A. Default to option 2 unless it benchmarks
materially slower than option 1.
