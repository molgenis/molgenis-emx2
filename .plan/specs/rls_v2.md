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
| Many roles per (user, schema) via per-group assignment (Phase 7; was: exclusive in v2) | `MOLGENIS.group_membership_metadata` PK | `TestAsymmetricCollaboration` ✓ | — |
| System roles have NO `BYPASSRLS`; their authority comes from seeded immutable rows in `role_permission_metadata` (Phase 7) | `MetadataUtils.seedSystemRoles`, RLS policy emitter | `TestSeedSystemRoles`, `TestAccessFunctions.systemRoleViaPgHasRole` ✓ | — |
| System role rows in `role_permission_metadata` are immutable post-seed (UPDATE blocked at SQL trigger; DELETE + role-name validation at Java + GraphQL layers) | `mg_protect_system_roles` trigger (UPDATE only — DELETE intentionally untrapped at SQL layer to permit cascade-delete on schema drop), `SqlRoleManager.upsertPermissions` / `deleteRole`, `GraphqlSchemaFieldFactory.change` | `MetadataUtilsRolePermissionTest` ✓ | — |

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
| `changeOwner` boolean controls whether user may UPDATE `mg_owner` column (enforced via `mg_check_change_capability` BEFORE trigger; INSERT path also checked) | `mg_check_change_capability` trigger, `mg_can_write_all` p_changing_owner branch | `TestChangeOwner` ✓ | — |
| `changeGroup` boolean controls whether user may UPDATE `mg_groups` column (enforced via `mg_check_change_capability` BEFORE trigger) | `mg_check_change_capability` trigger, `mg_can_write_all` p_changing_group branch | `TestChangeGroup` ✓ | — |

## Storage

| Behaviour | Component | Test | Visual |
|---|---|---|---|
| Role + permission storage in normalized `MOLGENIS.role_permission_metadata` + `MOLGENIS.group_membership_metadata` (Phase 7) | `MetadataUtils`, `SqlRoleManager` | `MetadataUtilsRolePermissionTest`, `SqlRoleManagerTest` ✓ | — |
| Dropping a schema cascades to its role permission rows and memberships | FK ON DELETE CASCADE | `TestTableRoleManagement` ✓ | — |
| Dropping a table cascades to its per-role permission rows (application-layer hook, no DB FK) | `SqlSchema.dropTable` (Phase 7 slice 7.C) | `TestTableRoleManagement` ✓ | — |
| Custom roles are NOT PG roles; only rows in `role_permission_metadata` (Q2 decision) | `SqlRoleManager.createRole` (no DROP/CREATE ROLE for custom roles) | `SqlRoleManagerTest` ✓ | — |
| Verb-level GRANT for custom-role users routed via per-schema `MG_ROLE_<schema>_MEMBER` PG role (Q4 decision) | `SqlSchema.addMember`, `addGroupMembership`, slice 7.D RLS-enable hook | `TestMemberPgRoleLifecycle` ✓ | — |
| Tables flip to RLS-enabled on first non-NONE EXACT-table row in `role_permission_metadata`; wildcard rows do not trigger RLS (Q3 decision) | slice 7.D policy emitter | `TestRlsLifecycle` ✓ | — |
| Access functions UNION system-role branch (`pg_has_role` + wildcard rows) and custom-role branch (`group_membership_metadata` + exact-table rows) (Q1 decision) | `mg_can_read`/`mg_can_write`/`mg_can_write_all` | `TestAccessFunctions.systemRoleViaPgHasRole` ✓ | — |

## GraphQL surface

| Behaviour | Component | Test | Visual |
|---|---|---|---|
| `_schema.roles` returns merged system + custom roles in one shape | `GraphqlSchemaFieldFactory` | `TestGraphqlPermissions.rolesQuery_returnsMerged` ✓ | — |
| `change(roles: [...])` mutation upserts role permissions | `GraphqlSchemaFieldFactory.change` | `TablePermissionsGraphqlTest.*` ✓ | — |
| `change(groups: [...])` / `delete(groups: [...])` use central mutation pattern | `GraphqlSchemaFieldFactory` | `TestGraphqlGroups.*` ✓ | — |

## Phase 7 acceptance test (asymmetric collaboration)

This is the gate for Phase 7. v2 cannot pass it; v3-shape design must.

| Behaviour | Component | Test | Visual |
|---|---|---|---|
| Alice as editor in group A and viewer in group B sees rows from both | `mg_can_read` | `TestAsymmetricCollaboration.readsRowsFromBothGroups` ✓ | — |
| Alice can UPDATE row tagged `[A]` (editor authority via group A) | `mg_can_write` | `TestAsymmetricCollaboration.updatesAGroupRow` ✓ | — |
| Alice cannot UPDATE row tagged `[B]` (viewer-only in B) | `mg_can_write` | `TestAsymmetricCollaboration.rejectsUpdateOfBOnlyRow` ✓ | — |
| Alice can move row tagged `[A,B]` to `[A]` (pulling out of B) | `mg_can_write_all` | `TestAsymmetricCollaboration.allowsRemovingGroupSheHasNoAuthorityIn` ✓ | — |
| Alice cannot share row into group C (no membership in C) | `mg_can_write_all` | `TestAsymmetricCollaboration.rejectsShareIntoForeignGroup` ✓ | — |
| Policy count per RLS table is exactly 4 (not per-role) | per-table policy emitter | `TestPolicyCount.fourPoliciesPerTable` ✓ | — |
| Direct-SQL count via COUNT-scoped role returns floored value | `mg_privacy_count` | `TestSelectScope.directSqlCountIsFloored` ✓ | — |

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

### REQ-3: Column-level `change_owner` / `change_group` enforcement model — RESOLVED (option 2)

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

**Decision (locked 2026-05-06):** **option 2 — predicate path.** The
`change_owner` / `change_group` BOOLEAN columns on
`role_permission_metadata` already carry the data; the predicate inside
`mg_can_write_all` reads them via the same join used for scope
resolution. No extra columns or denormalisation needed (scout confirmed
2026-05-06). v2's column-level GRANT path in
`SqlRoleManager.java:679-723` is deleted in slice 7.C.

**Why option 2 over the hybrid:** uniform enforcement layer beats
split-path complexity. Performance cost is one extra boolean comparison
per row in the WITH-CHECK predicate when the verb is UPDATE and the
diff touches `mg_owner` / `mg_groups` — negligible. Benchmark anyway
in slice 7.D; promote to option 3 (hybrid) only if the predicate path
is materially slower under realistic workloads.

**Status:** **closed** — implemented in slice 7.J.

**Implementation (7.J):** `MOLGENIS.mg_check_change_capability()` generic trigger
function emitted in `MetadataUtils.emitAccessFunctions`. Per-table `BEFORE INSERT OR UPDATE`
trigger `mg_check_change_cap_<table>` emitted by `SqlRoleManager.enableRlsForTable` and
dropped by `disableRlsForTable`. Trigger bypasses for Manager/Owner system-role users
(`pg_has_role` check). On UPDATE: `p_changing_owner := (OLD.mg_owner IS DISTINCT FROM NEW.mg_owner)`,
`p_changing_group := (OLD.mg_groups IS DISTINCT FROM NEW.mg_groups)`. On INSERT:
`p_changing_owner := (NEW.mg_owner IS NOT NULL AND NEW.mg_owner IS DISTINCT FROM current_user)`,
`p_changing_group := FALSE` (group-subset check in `mg_can_write_all` handles INSERT-time
group authorization, and Editor — seeded with `change_group=false` — must still be able
to create group-tagged rows; semantic: `change_group=false` blocks MODIFICATION on
existing rows, not initial tagging on insert). Trigger function is emitted by
`emitAccessFunctions` so the single migration v34 step (which calls that method) covers it.
Tests: `TestChangeOwner` (5 tests), `TestChangeGroup` (4 tests) — all green.

### REQ-4: Privacy-view-mode RLS pass-through — RESOLVED (Path A, deferred)

**Concern:** `mg_can_read` for the custom-role branch only passes RLS for
`select_scope ∈ {ALL, GROUP, OWN}`. For `{EXISTS, COUNT, RANGE, AGGREGATE}`
RLS returns FALSE, which makes `SELECT COUNT(*)` return 0 and breaks the
entire privacy-view-mode mechanism.

**Options:**
- **A)** `mg_can_read` passes RLS for COUNT/RANGE/AGGREGATE/EXISTS too
  (read-through). Privacy enforced at the GraphQL projection layer plus
  `mg_privacy_count` floor. Direct-SQL `SELECT *` by such a user leaks
  rows.
- **B)** RLS denies; route COUNT/aggregate through a `SECURITY DEFINER`
  function that bypasses RLS but floors via `mg_privacy_count`. Direct
  SQL truly only sees floored counts. Requires app-side rewrites of all
  count queries.

**Decision (locked 2026-05-06):** **Path A** for Phase 7. Closer to v2
master's behaviour; small slice. Path B logged as a future enhancement
(separate phase) when stricter direct-SQL privacy is required by a
real-world deployment.

**Implementation note:** in `mg_can_read` custom-role branch, extend the
predicate from `IN ('ALL','GROUP','OWN')` to also pass when
`select_scope IN ('EXISTS','COUNT','RANGE','AGGREGATE')` — these scopes
do not depend on `m.group_name` or `p_owner` because the view-mode
projection (not the row predicate) enforces privacy. The system-role
branch is unaffected (system roles already get full read pass-through
via their `select_scope='ALL'` seed row).

**Status:** **closed (Path A)**; Path B deferred.
