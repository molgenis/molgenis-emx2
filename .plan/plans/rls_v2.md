# RLS v2 — Role + Group + Owner

## Status

Design locked. Worktree not yet cut. This plan lives on the current branch
`mswertz/poc/rls_using_one_role_and_policies` as the source of truth; when we
cut the new worktree from master, copy this file across as the starting point.

## Background

- Current branch (`rls_using_one_role_and_policies`) achieved a feature-rich RLS
  model but at high policy count (5+ per role × table) and architectural
  complexity (per-scope policies, sentinel `USING(false)` markers, multi-role
  union resolution).
- PR #6058 (`feat: row level permissions`) offered a simpler binary-RLS model
  with ~4 shared policies per RLS-enabled table and a single `mg_roles[]` row
  column. Too narrow for our requirements (no per-row owner, no per-verb
  scope variance, no view modes, no per-role variance on shared tables).
- This plan synthesizes the lessons: 6058's `mg_*` row-tag convention and
  one-role-per-user-per-schema constraint, plus our scope lattice and
  privacy-mode enforcement.

## Model

> **Phase 7 supersedes parts of this section.** Phases 1–6 were implemented
> against the v2-shape model below. Phase 7 redesigns Role assignment,
> Storage, Policy emission, and Custom role exclusivity. See Phase 7 for the
> canonical current intent. Sections marked **[v2 / superseded]** are kept
> as the as-built record of Phases 1–6.

- **Role** [v2 / superseded by Phase 7]: per-schema named permissionset, exactly
  one per user per schema, scope encoded in emitted policy DDL.
  Phase 7: many memberships per (user, schema) via per-group assignment;
  scopes stored in `MOLGENIS.role_permission_metadata`.
- **Group**: per-schema named user-list. **Many memberships per user**. Pure
  membership only — no permissions, no description (just a name; log table can
  come later if needed). Stored as a row in `MOLGENIS.groups_metadata` with
  `users REF_ARRAY` to `MOLGENIS.users_metadata`.
- **`mg_owner`**: row-level column, `REF → MOLGENIS.users_metadata`. Required
  when any role on this table has `OWN` scope. Default value is the row's
  `mg_insertedBy` (emx2's existing createdBy field) — set via column DEFAULT
  or via the existing emx2 row-lifecycle trigger.
- **`mg_groups`**: row-level column, `REF_ARRAY → MOLGENIS.groups_metadata`.
  Required when any role on this table has `GROUP` scope. emx2's standard
  ref_array FK trigger already enforces that all referenced names exist;
  caller-side validation (caller can only reference groups they belong to,
  unless `changeGroup`) is enforced via column-level INSERT/UPDATE GRANTs
  (see below).
- **System roles** (Owner / Manager / Editor / Viewer): unchanged from master.
  Hold `BYPASSRLS` so they ignore custom-role policies entirely. The system
  role layer gates table-level access; the custom role layer gates row-level
  access, independently.
- **Defaults**: a role that does not list a table → scope `NONE` on that table
  → invisible. Empty `mg_groups[]` on a row → only OWN-scope role-holders or
  ALL-scope role-holders see it (system roles see it via BYPASSRLS regardless).

## Architecture

### Policy emission

For each `(custom_role, RLS_table)` pair we emit up to 4 policies — one per
verb (SELECT, INSERT, UPDATE, DELETE) — only if the role's scope on that verb
is non-`NONE`. Scope is encoded into the policy `USING` (and `WITH CHECK` for
write verbs):

| Scope | USING / WITH CHECK |
|---|---|
| `ALL` | `true` |
| `GROUP` | `mg_groups && current_user_groups(current_schema)` |
| `OWN` | `mg_owner = current_user` |
| `NONE` | policy not emitted; verb not granted |

Policy naming: `MG_P_<role>_<table>_<verb>`. Match current branch convention.
Validated against PG's 63-byte object name limit at emission time.

**Table-level GRANTs paired with policies**: SELECT and DELETE policies are
paired with `GRANT SELECT/DELETE ON TABLE <schema>.<table> TO <role>`.
INSERT and UPDATE intentionally do NOT receive a table-level GRANT — column-
level INSERT/UPDATE grants from `applyColumnGrants` (Phase 2) satisfy PG's
"some privilege required for RLS" prerequisite while preserving the column-
level restriction on `mg_owner`/`mg_groups` that drives `changeOwner` /
`changeGroup` enforcement. A table-level INSERT/UPDATE grant would void the
column restriction.

**Scaling**: 4 × #custom-roles × #RLS-tables. With 5 roles × 10 tables × 1k
schemas = 200k policies. PG handles this; auditing remains tractable via the
naming convention. Constant policy count was a stretch goal we have explicitly
relaxed in favour of richer per-verb scope expressivity (see decision log
below).

### Group membership lookup

Group membership lives in `MOLGENIS.groups_metadata`:

| Column | Type |
|---|---|
| `schema` | TEXT |
| `name` | TEXT |
| `users` | REF_ARRAY → `MOLGENIS.users_metadata` |

PK = (`schema`, `name`). When the MOLGENIS schema is later exposed via the
emx2 standard API, group CRUD comes for free; until then we drive it via
GraphQL surfaces in Phase 6.

A `STABLE` SQL function exposes the current user's groups for the current
schema (one function in MOLGENIS, called from any schema's policies):

```sql
CREATE FUNCTION MOLGENIS.current_user_groups(schema_name TEXT) RETURNS TEXT[]
LANGUAGE SQL STABLE AS $$
  SELECT COALESCE(array_agg(name), ARRAY[]::TEXT[])
  FROM MOLGENIS.groups_metadata gm
  WHERE gm.schema = schema_name
    AND current_user = ANY (gm.users);
$$;
```

`STABLE` allows planner caching within a query. One catalog-style lookup per
query, not per row.

### `changeOwner` / `changeGroup` enforcement — column-level GRANTs

We do **not** use triggers. Postgres column-level UPDATE/INSERT grants give
us the enforcement directly:

- Default emit per role: `GRANT INSERT (col1, col2, …)` and
  `GRANT UPDATE (col1, col2, …)` over the role's permitted columns
  **excluding** `mg_owner` and `mg_groups`.
- If role flag `changeOwner` is true, additionally
  `GRANT INSERT (mg_owner)` and `GRANT UPDATE (mg_owner)`.
- If role flag `changeGroup` is true, additionally
  `GRANT INSERT (mg_groups)` and `GRANT UPDATE (mg_groups)`.

If a role lacks the grant, the SQL UPDATE/INSERT fails at the SQL layer with
a column-permission error. No trigger, no flag lookup at runtime.

`mg_owner` is set automatically on INSERT via column DEFAULT
(`DEFAULT current_user`) or via the existing emx2 row-lifecycle trigger that
already populates `mg_insertedBy`. Roles without `changeOwner` cannot
override the default because they lack INSERT on the column. Roles with
`changeOwner` can override at insert time (e.g. an admin-style assignment).

The flag values themselves (`changeOwner`, `changeGroup`) are not stored
separately — they are derivable from the column-level grants. The
`getPermissions(role)` API recovers them by inspecting
`information_schema.column_privileges`.

### View modes (EXISTS / COUNT / RANGE / AGGREGATE)

Java-side enforcement in `SqlQuery`, unchanged from current branch — port
verbatim. Policies do not filter rows for view-mode scopes — `applyPolicies`
emits `USING(true)` for SELECT when scope is EXISTS/COUNT/RANGE/AGGREGATE
(pass-through at the PG layer). INSERT/UPDATE/DELETE policies are not emitted
for view-mode scopes (those scopes only constrain the SELECT verb's result
shape, not write verbs). Privacy floor for `RANGE`: `CEIL(COUNT(*)::numeric /
10) * 10`. View modes are computed against the already-filtered row set
produced by the row-level policies.

**Encoding** (decided): a single `SelectScope` enum per (role, table) with
values `NONE | EXISTS | COUNT | RANGE | AGGREGATE | OWN | GROUP | ALL` —
same shape as the current branch, conflating row-scope and view-mode in one
ordered ladder. This serves us today and the test surface already exists.

**Storage** [v2 / superseded by Phase 7]: v2 used JSON in `COMMENT ON ROLE` as
the canonical PermissionSet. Phase 7 replaces this with normalized
`MOLGENIS.role_permission_metadata` (per-table per-verb scopes +
changeOwner/changeGroup booleans + description) and
`MOLGENIS.group_membership_metadata` (per-(user, schema, group, role)
assignment). PG GRANTs and RLS policies become derived projections from
those tables; column-level GRANTs for changeOwner/changeGroup are
re-evaluated in 7.A (REQ-3).

### Custom role exclusivity [v2 / superseded by Phase 7]

v2 enforced "exactly one custom role per user per schema" at grant time in
Java by inspecting `pg_auth_members`. Phase 7 drops this rule entirely; a
user can hold multiple roles in one schema, one per group, via rows in
`MOLGENIS.group_membership_metadata`. The PK
`(user, schema, group, role)` enforces uniqueness only at the assignment
tuple level. This unlocks REQ-1 (asymmetric collaboration).

### System role coexistence

> **Phase 7 (2026-05-06): BYPASSRLS dropped — Path A locked.** v2 sections
> below describe the legacy mechanic; Phase 7 unifies system and custom
> roles through `role_permission_metadata`.

[Phase 7] Owner, Manager, Editor, Viewer have **no** `BYPASSRLS`. Their
authority is materialised as immutable seeded rows in
`role_permission_metadata` with `table_name='*'` (Owner/Manager/Editor =
ALL on every verb; Viewer = ALL select, NONE writes). Access functions
resolve the wildcard row first then fall back to exact-table custom-role
rows, so a user holding both a system and a custom role in a schema sees
the union of both authorities through one uniform RLS path.

[v2 / superseded] Owner, Manager, Editor, Viewer at session level had
`BYPASSRLS` set on the PG role (master behaviour). Mixed-role users
`SET ROLE`d to the most-permissive system role for ergonomic parity,
bypassing custom-role policies — the structural defect REQ-2 records.

## Phases

Acceptance criteria for each phase are captured in the exit-criteria line at
the end of that phase. The decision log below covers cross-cutting choices.
No separate spec files.

### Phase 0 — Plan freeze, scout

- Confirm `MOLGENIS.users_metadata` schema and primary key (likely
  `name TEXT PK`). Validate that a `REF_ARRAY` to it works for our access
  patterns.
- Verify there is no existing `MOLGENIS.role_metadata` table to extend (we
  expect none; if one exists we revisit (d) vs other storage choices).
- Capture exact JOOQ DSL utilities available in master for policy emission
  and DSL-safe role naming.
- Cut worktree: `git worktree add /Users/m.a.swertz/git/molgenis-emx2/poc/rls_v2 -b mswertz/poc/rls_v2 master`.
- Symlink `.claude` from master.
- Copy this plan into the new worktree.

Exit criteria: worktree exists, plan present, scout report appended to plan.

#### Phase 0 scout findings

1. **`MOLGENIS.users_metadata`** — exists in master and current branch. PK
   `username TEXT`, plus `password VARCHAR`, `enabled BOOLEAN NOT NULL`,
   inherited `SETTINGS JSON`. Defined in `MetadataUtils.java:28, 264-267`.
   Suitable for REF / REF_ARRAY targets. No divergence risk.
2. **No competing role/group metadata tables** — clean slate. Only existing
   MOLGENIS tables: `schema_metadata`, `table_metadata`, `column_metadata`,
   `users_metadata`, `settings_metadata`, deprecated `version_metadata`.
   System roles hardcoded in `SqlRoleManager.systemPermissions()`.
3. **Policy emission helpers** — port from
   `SqlPermissionExecutor.java`: `createPolicy`, `createSelectScopePolicy`,
   `dropAllPolicies`, `grantTablePrivilege`, `revokeTablePrivilege`,
   `enableRowLevelSecurity`. Current naming: `MG_P_<role>_<verb>_<scope>`.
   v2 renames to `MG_P_<role>_<table>_<verb>` and drops scope from the name
   (scope lives in USING).
4. **DSL-safe role naming** — `SqlRoleManager.fullRoleName(schema, name)`
   line 981 returns `MG_ROLE_<schema>/<name>`, validates UTF-8 length
   against PG's 63-byte limit. JOOQ `name()` wrapper is the canonical safe
   quoter; `stripRolePrefix` in `SqlPermissionExecutor.java:394` for the
   inverse.
5. **BYPASSRLS inheritance** — unproven in codebase. Only `MG_USER_admin`
   carries BYPASSRLS directly. No test demonstrates the flag propagating
   via `GRANT role TO role`. Decision: add a Phase 1 micro-spike test
   before relying on this for the future `MG_BASE_*` extension; not a
   blocker for v2 phases 1–6 (we don't depend on base-role BYPASSRLS in
   the locked design).
6. **ref_array FK trigger** — reusable. `SqlColumnRefArrayExecutor.java:25-44,
   88-120` provides `createRefArrayConstraints` (reference-exists +
   referred checks) and `removeRefArrayConstraints`. GIN index automatic
   on ref_array columns. `mg_groups REF_ARRAY → groups_metadata.name`
   needs no new code.
7. **`mg_insertedBy`** — set by emx2 row-lifecycle trigger, not column
   DEFAULT. `Constants.java:41`. `SqlPermissionExecutor.java:280-293`
   already copies `mg_insertedBy` into `mg_owner` on RLS enablement.
   Decision: `mg_owner` defaults via the same trigger path (preferred
   over `DEFAULT current_user` so the column tracks the audit column).
8. **No existing `MG_BASE_*` roles** — no BYPASSRLS-bearing reusable base
   roles. The future-extension (shared-policy via base role) is greenfield.

### Phase 0.5 — Post-merge baseline (2026-05-01)

After merging origin/master (13 new commits, no rls_v2-scope overlap), the
test floor was established with a SINGLE combined gradle invocation. Running
the suites as separate `./gradlew` invocations causes 9 spurious failures
(DB-state leak between JVM processes). Always use the combined form.

Canonical baseline command (after `./gradlew cleandb`):

```
./gradlew :backend:molgenis-emx2-sql:test \
          :backend:molgenis-emx2-graphql:test \
          :backend:molgenis-emx2-io:test \
          :backend:molgenis-emx2-webapi:test \
          :backend:molgenis-emx2-nonparallel-tests:test \
          --no-parallel \
          -x :apps:collectDist -x :apps:pnpm_build
```

Baseline floor (all green — Phase 1 must keep this):

| Suite | Pass | Fail | Skip |
|---|---|---|---|
| sql | 279 | 0 | 1 (`TestRowLevelSecurity` disabled — old RLS) |
| graphql | 53 | 0 | 0 |
| io | 67 | 0 | 0 |
| webapi | 146 | 0 | 3 (`testScriptExecution`, `testExecuteSubtaskInScriptTask`, `PerformanceTest`) |
| nonparallel-tests | 6 | 0 | 0 |

### Phase 1 — Foundation: groups table + function (slice A)

Phase 1 is scoped to the foundation pieces that are independently testable
right now, without needing a custom role to exist (custom roles arrive in
Phase 2). The column-lifecycle plumbing (originally tasks 3, 4, 8 in the
locked design) moves into Phase 2 because those columns only get added/
removed when a role with OWN/GROUP scope is granted — implementing them
without a caller produces dead code waiting for Phase 2.

1. **Add `MOLGENIS.groups_metadata`** table with columns
   `(schema TEXT, name TEXT, users REF_ARRAY → users_metadata)`, PK
   `(schema, name)`. FK to schema metadata so deletion cascades.
2. **Emit `MOLGENIS.current_user_groups(schema_name)`** STABLE function —
   single source, called from any user schema's policies.
3. **Verify the existing emx2 ref_array FK trigger** rejects `mg_groups`-style
   references to non-existent group names — write a unit test against
   `groups_metadata`-shaped columns. Reuse, do not re-implement.
4. **Verify GIN index** on the ref_array column (auto-created by emx2
   ref_array machinery) — assertion test, no new code.

Exit criteria: groups table + function present in MOLGENIS schema after
DB init; ref_array FK trigger rejects bad group references; GIN index
verified; full combined-suite test floor still green
(sql/graphql/io/webapi/nonparallel = 0 failures).

#### Phase 1 status (2026-05-01)

- Implementation in place: `migration33.sql` (table DDL + FK + indexes),
  `utility-sql/current_user_groups.sql` (function), `Migrations.java` runs
  both at `version < 33`. `MetadataUtils.init()` no longer contains
  `groups_metadata` DDL — table creation is exclusively migration-driven.
  `GroupsMetadataTest` covers structure / FK cascade / function / GIN index.
- Targeted test: `GroupsMetadataTest` 4/4 pass (~17s).
- Combined-suite floor verified by user before commit `181e6db44`.

**Fork base**: v2 was cut fresh from master. The prior branch
`mswertz/poc/rls_using_one_role_and_policies` (still live locally + remote)
holds the reference implementation for `SqlRoleManager` / per-verb policy
emission / column-grant logic — 13 commits ahead of v2's fork point.
Phases 2–6 port from there with naming aligned to the locked v2 design.

Deferred from the original Phase 1 design (now Phase 2):

- `mg_owner` column lifecycle (was task 3) — drives off OWN-scope role grant
- `mg_groups` column lifecycle (was task 4) — drives off GROUP-scope role grant
- B-tree index on `mg_owner` (was task 6) — driven by the column add
- Column-level INSERT/UPDATE GRANTs for changeOwner/changeGroup (was task 8) —
  driven by role grant

### Phase 2 — Custom role definition + column lifecycle

1. **`SqlRoleManager.createRole(schema, name, description)`** creates PG role
   `MG_ROLE_<schema>/<name>`. Reject duplicates and reserved name patterns.
2. **`setPermissions(role, PermissionSet)`** writes the role's per-table
   per-verb scopes by emitting policies (Phase 3 detail). Replaces all prior
   policies for this role atomically.
3. **`grantRoleToUser(role, user)`** with one-role-per-user-per-schema
   exclusivity check (rejects if user already holds a different custom role
   in this schema).
4. **`revokeRoleFromUser`**, **`deleteRole`**, **`listRoles`** — port from
   current branch with naming aligned.
5. **`changeOwner` / `changeGroup`** — emit column-level INSERT/UPDATE
   GRANTs as described in the architecture section. The PG ROLE COMMENT
   JSON also records the boolean for round-tripping via `getPermissions`.
6. **Column lifecycle** (lifted in from old Phase 1):
   - Add `mg_owner` column (REF → `MOLGENIS.users_metadata`, defaulted via
     existing emx2 row-lifecycle trigger) when the first role with OWN scope
     on the table is granted; drop when the last is removed. Verify
     B-tree index comes for free with the REF column.
   - Add `mg_groups` column (REF_ARRAY → `MOLGENIS.groups_metadata`) when
     the first role with GROUP scope is granted; drop when the last is
     removed.

Exit criteria: roles can be created, listed, granted, revoked, deleted via
GraphQL; one-role-per-schema constraint enforced; lifecycle round-trip tests
green.

#### Phase 2 port plan (2026-05-02)

Source-branch reference: `mswertz/poc/rls_using_one_role_and_policies`.
The port is **not verbatim** — five concrete divergences require adaptation:

1. **Role naming**: source uses global `MG_ROLE_<name>`; v2 needs schema-scoped
   `MG_ROLE_<schema>/<name>` (already noted Phase 0 scout #4).
2. **Policy naming**: source emits `MG_P_<role>_<verb>_<scope>`; v2 emits
   `MG_P_<role>_<table>_<verb>` (per-table, scope in USING).
3. **Sentinel `USING(false)` policies for changeOwner/changeGroup** are
   rejected in v2 in favour of column-level INSERT/UPDATE GRANTs. Do not port.
4. **One-role-per-user-per-schema exclusivity** is new in v2 — source has no
   such check. Add it in `grantRoleToUser`.
5. **Column lifecycle (mg_owner/mg_groups add+drop)** is new in v2 — source
   only adds on first RLS enable, never drops. Drive lifecycle from
   `setPermissions` diff in v2.

Source files to mine (read-only, do not check out branch):
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java` — createRole 73, deleteRole 102, listRoles 163, grantRoleToUser 188, revokeRoleFromUser 222, setPermissions 241, emitFlagPolicies 558.
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/rls/SqlPermissionExecutor.java` — createChangeOwnerPolicy 121, createChangeGroupPolicy 130, enableRowLevelSecurity 268 (mg_insertedBy → mg_owner migration is reusable, lines 280–293).
- Tests: `SqlRoleManagerTest`, `rls/SqlRoleManagerEmissionTest`.

Slice plan (each slice independently testable, RED-GREEN per slice):

- **2.A — Create / list / delete role.** `SqlRoleManager.createRole(schema,
  name, description)` emits `CREATE ROLE MG_ROLE_<schema>/<name>` with empty
  COMMENT JSON `{}`. `deleteRole(schema, name)` drops the PG role.
  `listRoles(schema)` queries `pg_authid` + `pg_shdescription`, filtered on
  `MG_ROLE_<schema>/` prefix. Tests: round-trip; duplicate rejection; reserved
  name patterns. **No grants, no policies, no columns** in this slice.
  **Status (2026-05-02): GREEN — 9/9 tests pass.** Implementation note: emx2
  system roles (Owner / Manager / Editor / Viewer / Count / Range / Exists /
  Aggregator) share the `MG_ROLE_<schema>/` prefix, so `listRoles` filters
  these out by name to return only custom roles. Any future consumer that
  enumerates custom roles must apply the same filter. `deleteRole` calls
  `DROP OWNED BY` before `DROP ROLE` so an existing-grants/objects state
  doesn't block deletion. `deleteRole` of a missing role throws
  `MolgenisException` (not idempotent).
- **2.B — Grant / revoke with exclusivity.**
  `grantRoleToUser(schema, role, user)` checks `pg_auth_members` for any
  existing `MG_ROLE_<schema>/*` granted to user → reject if found (system
  roles exempt). `revokeRoleFromUser(schema, role, user)` straight DROP.
  Tests: round-trip; second-role rejection; system role exempt.
  **Status (2026-05-02): GREEN — 17/17 tests pass (9 from 2.A + 8 new).**
  Implementation note: exclusivity scans `pg_auth_members` for any
  `MG_ROLE_<schema>/*` membership held by `MG_USER_<u>`, strips the prefix,
  applies the same `isSystemRole` filter from 2.A, and rejects only when a
  non-system custom-role membership remains. Missing-user / missing-role
  checks query `pg_roles` directly (no Database context plumbing required).
- **2.C — `setPermissions` API surface (no policies yet).** Define
  `PermissionSet` Java type (per-table per-verb scope + `changeOwner` /
  `changeGroup` booleans). `setPermissions(role, PermissionSet)` writes the
  full document into `COMMENT ON ROLE`. `getPermissions(role)` reads it back.
  Tests: round-trip; reject scopes outside enum.
  **Status (2026-05-02): GREEN — 23/23 tests pass (17 from 2.B + 6 new).**
  `SelectScope` and `PermissionSet` (with inner `TablePermissions`) placed in
  `org.molgenis.emx2` (model module). JSON codec lives in `SqlRoleManager`
  using Jackson. Single `SelectScope` enum: `NONE|EXISTS|COUNT|RANGE|AGGREGATE|OWN|GROUP|ALL`.
  JSON shape: `{"tables":{"<table>":{"select":"OWN","insert":"GROUP","update":"OWN","delete":"NONE"}},"changeOwner":false,"changeGroup":false}`.
- **2.D — Column lifecycle (mg_owner / mg_groups).** On `setPermissions`,
  diff old vs new doc per table: first OWN appears → add `mg_owner` REF
  (reuse the `enableRowLevelSecurity` migration logic); last OWN goes →
  drop column. Same for `mg_groups` REF_ARRAY. Existing emx2 row-lifecycle
  trigger sets `mg_owner = mg_insertedBy` on insert. B-tree index auto via
  REF column (verify). Tests: column appears/disappears via setPermissions;
  default value populated by trigger; B-tree index present.
  **Status (2026-05-01): GREEN — 31/31 tests pass (23 from 2.C + 8 new).**
  **Concurrency fix (2026-05-01): GREEN — 32/32 tests pass (+1 new).**
  Column lifecycle lives in `SqlRoleManager.applyColumnLifecycle` (private),
  called from `setPermissions` inside the tx. `pg_advisory_xact_lock` on the
  schema oid serializes concurrent `setPermissions` calls; snapshot is built
  via `txJooq` after the COMMENT is written so the just-written role's
  permissions are visible without any "inject" hack. `mg_owner` added as TEXT
  + explicit btree index `<table>_mg_owner_btree`; `mg_groups` added as TEXT[]
  + explicit GIN index `<table>_mg_groups_gin`. Both dropped via `CASCADE` when
  last scope is removed. Teardown fix: `dropCustomRolesForSchema` now filters
  out system roles to avoid `DROP OWNED BY Manager` wiping tables.
- **2.E — Column-level GRANTs for changeOwner / changeGroup.** On
  `setPermissions` diff: emit `GRANT INSERT/UPDATE (cols…)` over role's
  permitted columns excluding `mg_owner` / `mg_groups`; if `changeOwner=true`
  add grant on `mg_owner`; if `changeGroup=true` add grant on `mg_groups`.
  Revoke when flag flips false. Tests: SQL-level rejection of `mg_owner`
  update without flag; allow with flag.
  **Status (2026-05-02): GREEN — 38/39 tests pass (32 from 2.D + 6 new), 1
  skipped.** Grant logic in `SqlRoleManager.applyColumnGrants` (private),
  called from `setPermissions` after `applyColumnLifecycle`. Diff driven by
  `information_schema.column_privileges`. Regular columns = all non-`mg_*`
  cols via `information_schema.columns`. Test #7 (`setPermissions_sqlLevelRejectsMgOwnerUpdateWithoutFlag`)
  is `@Disabled` — requires table-level INSERT grant (Phase 3) before
  column-level enforcement is observable via emx2 table API.

Phase 2 exits when 2.A–2.E are green. **Policy DDL itself stays Phase 3** —
`setPermissions` in Phase 2 only writes COMMENT JSON + manages columns/grants;
policy CREATE/DROP slots into the same diff loop in Phase 3.

### Phase 3 — Per-verb scope policies

1. For each `(role, table, verb)` with non-`NONE` scope, emit policy
   `MG_P_<role>_<table>_<verb>` with the USING/WITH CHECK from the table
   above.
2. `setPermissions` diffs old vs new and emits `CREATE`/`DROP POLICY`
   surgically (port `applyDiff` from current branch but adapt to per-verb
   per-(role × table) shape).
3. **GRANT/REVOKE table-level privilege** for the verb in step with policy
   creation/deletion (PG requires both the GRANT and the matching policy for
   filtered access).
4. **`mg_owner` / `mg_groups[]` column lifecycle** — driven by aggregate of
   all roles' scopes for this table:
   - Any role has `OWN` on any verb → `mg_owner` column present.
   - Any role has `GROUP` on any verb → `mg_groups[]` column present.
   - When the last such role flag goes away, drop column.

Exit criteria: policies appear/disappear on grant/revoke; SQL-level
enforcement matches expectation for each (scope, verb) cell;
`SqlPermissionExecutor`-equivalent tests green.

#### Phase 3 status (2026-05-03) — GREEN

- Implementation: `SqlRoleManager.applyPolicies` (private), called from
  `setPermissions` after `applyColumnGrants`. Per-verb helpers: `applyVerbPolicy`,
  `ensureRlsEnabled`, `emitPolicy`, `grantOrRevokeTableVerb`, `buildPolicyName`,
  `isViewModeScope`, `buildUsingExpr`, `buildWithCheckExpr`, `groupUsingExpr`,
  `buildGroupWithCheckExpr`.
- Policy naming: `MG_P_<fullRole>_<table>_<verb>`.
- DROP-then-CREATE on every `setPermissions` call (idempotent via `DROP POLICY IF EXISTS`).
- INSERT/UPDATE verbs: no table-level GRANT emitted — column-level grants from
  `applyColumnGrants` are sufficient for PG policy evaluation AND preserve
  `mg_owner`/`mg_groups` column restrictions (table-level grant would override them).
  SELECT/DELETE: table-level GRANTs emitted (no column-level grant concept).
- View-mode scopes (EXISTS/COUNT/RANGE/AGGREGATE): mapped to `USING(true)` row policy;
  Java enforces aggregate-only access in Phase 4.
- `setPermissions_sqlLevelRejectsMgOwnerUpdateWithoutFlag` remains `@Disabled`:
  test uses Alice-as-Manager (BYPASSRLS), which is not column-restricted; needs
  `SET ROLE` to custom role session — Phase 4/5 fixture work.
- Targeted test: `SqlRoleManagerTest` 56/56 pass, 1 skipped (the disabled
  Alice-as-Manager BYPASSRLS test), 0 failed.
- Hygiene pass (review-driven): added `buildPolicyName` UTF-8 length check
  (PG 63-byte limit; throws `MolgenisException` on overflow); added INSERT/OWN
  + INSERT/GROUP policy-shape tests; added parameterized view-mode SELECT test
  (4 invocations: EXISTS/COUNT/RANGE/AGGREGATE → `USING(true)`, no
  INSERT/UPDATE/DELETE policies); switched `grantColumnPrivileges` /
  `revokeColumnGrants` to jOOQ `name(col)` quoting. Concurrency test renamed
  table `ConcurrentTable` → `ConcTable` to fit under the new 63-byte cap.

##### Deferred hygiene (rolled into Phase 7)

- `buildGroupWithCheckExpr` strips outer parens via `substring` from
  `groupUsingExpr` — fragile coupling. Refactor: have `groupUsingExpr` return
  the raw predicate without wrapping parens; let callers wrap as needed.
- `MOLGENIS_SCHEMA` literal hardcoded in `groupUsingExpr` — should reference
  `Constants.MG_SYSTEM_SCHEMA` (or whichever existing constant names the
  MOLGENIS schema) so a future rename doesn't silently break policies.
- `grantOrRevokeTableVerb` uses string concatenation for the verb (verbs are
  constants, not user input — minor stylistic cleanup).
- `fetchRegularColumns`'s `NOT LIKE 'mg_%'` filter silently excludes any
  user-defined column starting with `mg_`. Document the convention or harden
  to an explicit allow-list of system columns (`mg_owner`, `mg_groups`,
  `mg_insertedBy`, etc.).

### Phase 4 — View modes (Java side)

1. Port `SelectScope` enum (NONE / EXISTS / COUNT / RANGE / AGGREGATE / OWN /
   GROUP / ALL) and capability methods (`allowsCount`, `allowsMinMax`,
   `allowsAvgSum`, `allowsGroupBy`, `allowsExactCount`, `allowsRowAccess`,
   etc.) from current branch.
2. Port the privacy-floor implementation in `SqlQuery.getCountField()`
   (`CEIL(COUNT(*)::numeric / 10) * 10` as `Long`).
3. Port the `SqlQuery` enforcement helper introduced in Phase 6.C
   (`requireSelectCapability`).
4. View-mode scopes (EXISTS / COUNT / RANGE / AGGREGATE) extend the row-level
   model — they layer on top of `OWN` / `GROUP` / `ALL` row-filtering. The
   role declares e.g. `select=AGGREGATE+ALL` meaning aggregate-only access
   to all rows. Encoding TBD: bitset on the policy verb, or two-axis scope
   field.

Exit criteria: privacy modes work as in current branch; aggregate-only and
range-only queries are correctly enforced; view-mode tests pass.

#### Phase 4 port plan (2026-05-03)

Source-branch reference: `mswertz/poc/rls_using_one_role_and_policies` —
`SqlQuery.java` `getCountField`, `requireSelectCapability`,
`getEffectiveSelectScopes`, plus call sites at MIN/MAX/AVG/SUM/GROUP BY.

Pre-state (scout 2026-05-03):
- `SelectScope` enum + all 6 capability methods already in v2
  (`backend/molgenis-emx2/src/main/java/org/molgenis/emx2/SelectScope.java`).
- v2 `SqlQuery.getCountField()` (lines 744–757) currently dispatches on
  **system roles** via `schema.hasActiveUserRole(COUNT/AGGREGATOR/RANGE)` —
  legacy code from master, not custom-role aware.
- v2 `SqlQuery` EXISTS path (lines 709–712) also gated on system role.
- No `getEffectiveSelectScopes` / `requireSelectCapability` in v2 yet.

Divergences from source-branch port:
1. **Source enum lives at `TablePermission.SelectScope`**; v2's lives at
   `org.molgenis.emx2.SelectScope` (already moved in Phase 2.C). Imports differ.
2. **`getEffectiveSelectScopes`** in v2 must read from `SqlRoleManager`
   COMMENT-JSON PermissionSet (Phase 2.C storage), not from a metadata table.
3. **System-role view-mode dispatch** stays for backward compat with master
   (existing tests rely on it). Custom-role scope is layered on top: if
   a custom role is granted, its scope wins; else fall back to system-role
   dispatch (master behaviour).
4. **Privacy-floor return type**: source `Long`, v2 currently `Integer` at
   line 754 — align to `Long` to match source semantics and fix overflow on
   billion-row tables.

Slice plan (each slice independently testable, RED-GREEN per slice):

- **4.A — `getEffectiveSelectScopes(table)` helper.** New private method on
  `SqlQuery` (or its companion). Returns `Set<SelectScope>` for current user
  on this table: union of (a) custom-role scope from PermissionSet for the
  current user's role on this schema/table, plus (b) system-role view-mode
  scopes (COUNT/AGGREGATOR/RANGE/EXISTS) translated from existing
  `hasActiveUserRole` checks. Empty set = no access. Tests: per-(role,table)
  scope returns expected set; user with no role returns empty; system role
  fallback works when no custom role granted.
  **Status (2026-05-03): GREEN — 8/8 tests pass.** Implementation landed on
  `SqlRoleManager` rather than `SqlQuery` — `pg_auth_members` lookups require
  admin jOOQ access (`database.getJooqAsAdmin`), which the role manager
  already plumbs. Public method
  `getEffectiveSelectScopes(Schema, SqlTableMetadata)` returns the union of
  the user's custom-role `select` scope (read from PermissionSet COMMENT
  JSON) and any direct system view-mode role grants (Count/Range/Aggregator/
  Exists, queried from `pg_auth_members` with the system-role hierarchy
  unflattened — direct grants only). Test class
  `EffectiveSelectScopesTest`. Pre-existing interaction noted:
  `executeAddMembers` revokes a user's custom PG role when assigning a new
  system role — Phase 6 GraphQL work must account for this.
- **4.B — `requireSelectCapability` + `getCountField` refactor.** Add private
  helper `requireSelectCapability(table, predicate, message)` that throws
  `MolgenisException` when no scope in `getEffectiveSelectScopes` satisfies
  the predicate. Refactor `getCountField` to: if any scope `allowsExactCount`
  → `count()`; else if `RANGE` present → privacy floor (`CEIL(COUNT(*)::
  numeric / 10) * 10` as `Long`); else throw. Tests: COUNT returns exact;
  RANGE returns floored; AGGREGATE returns exact (allowsExactCount=true);
  EXISTS-only throws; NONE throws.
  **Status (2026-05-03): GREEN — 9/9 tests pass; 23-test smoke (incl.
  `TestAggregatePermission`, `EffectiveSelectScopesTest`) all green.**
  `getCountField` return type changed `Integer` → `Long`; only caller
  (`jsonAggregateSelect`) takes `List<Field<?>>` so no cascade. Dispatch
  rule: custom-role scope drives the new exact/floor/throw path; **if no
  custom role**, falls back to the existing system-role dispatch which
  preserves master's `GREATEST(COUNT(*),10)` for the AGGREGATOR system role
  (distinct from custom-scope `AGGREGATE` which routes through
  `allowsExactCount=true`). New helpers: `SqlQuery.effectiveSelectScopes`,
  `SqlQuery.requireSelectCapability`; `SqlRoleManager.getEffectiveSelectScopes`
  gained a `(String schemaName, SqlTableMetadata)` overload to handle
  cases where the active user lacks schema VIEW (the original `(Schema,
  ...)` overload may now be unused — review in 4.A cleanup).
- **4.C — MIN/MAX/AVG/SUM/GROUP BY enforcement.** Identify call sites in
  v2 `SqlQuery` for these aggregations. Wrap each with the appropriate
  `requireSelectCapability` predicate (`allowsMinMax`, `allowsAvgSum`,
  `allowsGroupBy`). Tests: per-aggregation, per-scope allow/deny matrix.
  **Status (2026-05-03): GREEN — 45/45 parameterized tests pass; 79-test
  smoke (4.A+4.B+4.C+4.D) all green.** Wraps applied: `jsonAggregateSelect`
  split the single MAX/MIN/AVG/SUM guard into two `requireSelectCapability`
  calls (`allowsMinMax` for MAX/MIN, `allowsAvgSum` for AVG/SUM); switch-case
  refactored to if/else to drop dead `default` arm. `jsonGroupBySelect` adds
  `allowsGroupBy` check after the existing COUNT/SUM guard. Denial throws
  `MolgenisException` (not silent omission). Confirmed: RANGE → MIN/MAX
  allowed, AVG/SUM denied; AGGREGATE → all four allowed; COUNT/EXISTS/NONE
  → all denied; system Viewer → ALL via 4.D mapping → all allowed.
- **4.D — EXISTS field refactor.** Replace `schema.hasActiveUserRole(EXISTS)`
  in `jsonFieldSelect()` (lines 709–712) with effective-scope check. Test:
  EXISTS field returns boolean for any scope where `allowsRowAccess=true OR
  EXISTS in set`; absent otherwise.
  **Status (2026-05-03): GREEN — 10/10 tests pass; 28-test smoke green
  (ExistsFieldTest + GetCountFieldTest + EffectiveSelectScopesTest).**
  `SqlQuery.existsFieldAllowed(table)`: custom-role scope wins (NONE / non-
  row-access non-EXISTS → field omitted); falls back to
  `schema.hasActiveUserRole(EXISTS)` when no custom role granted.
  `SqlRoleManager.addSystemRoleScopes` extended to map
  Viewer/Editor/Manager/Owner → `SelectScope.ALL` so capability checks
  pass-through for full-access system roles (consistent with their
  BYPASSRLS at PG layer). New: `SqlRoleManager.hasCustomRoleForUser(
  schemaName)` to disambiguate "custom-role-with-NONE" from "no custom
  role at all".

Phase 4 exits when 4.A–4.D are green and the combined-suite floor is still
zero failures.

**Status (2026-05-03): all four slices GREEN via targeted tests
(9 + 9 + 50 + 10 = 78 new tests; 86-test cross-module smoke green).
Awaiting user-run combined-suite at phase boundary (canonical command in §0.5).**

**4.C master-parity fix (2026-05-03)**: combined-suite revealed 14 regressions
(`TestCompositeForeignKeys`, `TestQueryJsonGraph`, `TestGraphqlSchemaFields`,
`TestSumQuery`) — `requireSelectCapability` threw for admin/superuser users
who have neither a custom role nor a view-mode system role. Fix: short-circuit
`requireSelectCapability` to no-op when `effectiveSelectScopes` is empty AND
`hasCustomRoleForUser==false`. Preserves master parity. 5 regression tests
added to `AggregationPermissionTest`. Error-message format also fixed (table
name was missing from the thrown exception).

**Suspected ordering glitch**: combined-suite also reported
`TestGraphqlSchemaFields.testMatchInParentsAndChildren` (`Field 'Tag/name'
undefined`). PASSES on master AND in targeted v2 run. Likely cross-test
schema pollution; tracked as #6, will re-evaluate at next combined-suite run.

### Phase 5 — `changeOwner` / `changeGroup` capabilities

Already enforced by column-level INSERT/UPDATE GRANTs emitted in Phase 3.
This phase is reduced to integration testing:

1. Round-trip test: setting `changeOwner=true` adds the column grants;
   setting `false` revokes them.
2. Validation: SQL error wrapping in `SqlPermissionExecutor` translates the
   PG `permission denied for column mg_owner` into the user-facing
   `"role X cannot change row owner"`.
3. WITH CHECK on UPDATE policy already prevents broader category violations
   (e.g. updating to a row that would no longer be visible to the role).

Exit criteria: change-capability flags enforced via column grants; SQL
errors translated; tests cover allow/deny per flag.

#### Phase 5 port plan (2026-05-03)

Phase 5 is mostly integration-test work — column-level INSERT/UPDATE GRANTs
were already emitted in Phase 2.E and policies in Phase 3, so the SQL layer
already enforces `changeOwner` / `changeGroup`. What's missing: end-to-end
verification with an actual custom-role SET ROLE session, plus user-facing
error translation.

Pre-state references:
- `SqlRoleManager.applyColumnGrants` (Phase 2.E) emits/revokes
  `GRANT INSERT/UPDATE (mg_owner)` and `(mg_groups)` on flag flips.
- `SqlRoleManagerTest.setPermissions_sqlLevelRejectsMgOwnerUpdateWithoutFlag`
  (Phase 2.E + Phase 3) is `@Disabled` because the test ran as Manager
  (BYPASSRLS) — needs a custom-role SET ROLE fixture.

Scout findings (2026-05-03) simplified the slicing:
- `SqlUserAwareConnectionProvider.acquire()` already issues `RESET ROLE; SET
  ROLE MG_USER_<u>` for non-admin active users, so `database.setActiveUser(u)`
  + a custom-role grant on `u` is the fixture — no new helper needed.
- `SqlMolgenisException.getTitle()` already switches on
  `PSQLException.getSQLState()` (currently `57014` timeout only). Adding the
  `42501` arm with a small lookup on column name produces the friendly message.
  Result is `MolgenisException` (the parent class) — no new exception type.
- Source branch has NO prior translation for `mg_owner` / `mg_groups` —
  Phase 5 work is net-new.

Slice plan (two slices):

- **5.A — Re-enable + extend `setPermissions_sqlLevelRejectsMgOwnerUpdateWithoutFlag`.**
  Remove `@Disabled`. Fix the existing test setup: grant Alice the **custom
  role** (not Manager) and call `setActiveUser(alice)` so PG enforces the
  column-level restriction. Expand into a matrix:
  - `changeOwner=false` → UPDATE `mg_owner` rejected by PG (SQLSTATE 42501).
  - `changeOwner=true` → UPDATE `mg_owner` succeeds.
  - `changeGroup=false` → UPDATE `mg_groups` rejected.
  - `changeGroup=true` → UPDATE `mg_groups` succeeds.
  - `changeOwner=false` → INSERT row with explicit `mg_owner` rejected.
  - `changeOwner=true` → INSERT row with explicit `mg_owner` succeeds.
  - Round-trip: flip flag false→true→false; verify column grant added then
    revoked at each transition (read `information_schema.column_privileges`).

  Tests at this slice run via raw jOOQ on the user's connection — they
  assert PG's behaviour, not the friendly translation.

  **Status (2026-05-03): GREEN — 62/62 tests in SqlRoleManagerTest (was 56/56 + 1
  skipped); 140/140 smoke (SqlRoleManagerTest + EffectiveSelectScopesTest +
  GetCountFieldTest + ExistsFieldTest + AggregationPermissionTest).**
  Implementation: 7 new test methods in a new `ColumnGrantEnforcement` nested
  class. Fixture: schema USAGE explicitly granted to the custom role
  (`GRANT USAGE ON SCHEMA ... TO MG_ROLE_.../enforcer`), custom role with
  `select=OWN, insert=ALL, update=ALL` (OWN scope triggers `mg_owner` column;
  ALL insert/update scope gives WITH CHECK(true) so policy doesn't interfere
  with column-level enforcement). Alice uses `db.setActiveUser(alice)` → jOOQ
  issues `SET ROLE MG_USER_alice` → PG enforces column-level grants inherited
  from the custom role. SQLSTATE 42501 asserted via PSQLException.getSQLState().
  No production code changes required — Phase 2.E column-grant logic already
  enforced correctly; only needed the right test fixture.

- **5.B — User-facing error translation.** Extend `SqlMolgenisException.getTitle()`
  (`backend/molgenis-emx2-sql/src/main/java/.../SqlMolgenisException.java`
  lines ~27–37) with a `42501` arm. Structure: a small static
  `Map<String, String>` (column name → friendly message)
  with two entries — `mg_owner` → "cannot change row owner", `mg_groups` →
  "cannot change row groups". Lookup keyed by parsing the column name out
  of PG's "permission denied for column <col>" message text. If the column
  isn't in the map, fall through to the original PG message (don't swallow).
  Add a SQLSTATE constant (`SQLSTATE_INSUFFICIENT_PRIVILEGE = "42501"`) —
  the codebase has no constants file yet, so add it as a private constant
  in `SqlMolgenisException` to avoid scope creep. Tests: the same matrix
  as 5.A but going through the emx2 table API (`SqlTable.update` /
  `SqlTable.insert`) — assert the message text.

  **Status (2026-05-03): GREEN — 6/6 unit tests pass; 68-test smoke
  (SqlMolgenisExceptionTest + SqlRoleManagerTest) all green. pmdMain +
  pmdTest clean.** `SqlMolgenisException.java` adds three private statics:
  `SQLSTATE_INSUFFICIENT_PRIVILEGE = "42501"`, `COLUMN_DENIED_PREFIX`,
  `COLUMN_FRIENDLY_MESSAGES` map (`mg_owner` → "cannot change row owner",
  `mg_groups` → "cannot change row groups"); the 42501 arm in `getTitle()`
  parses the column name out of PG's "permission denied for column <col>"
  message and substitutes the friendly text — falls through to the original
  PG message when the column isn't in the map (so unrelated 42501 errors
  aren't swallowed). Tests use synthetic `PSQLException` via PG wire format
  (no DB), keeping the unit fast and isolated.

Phase 5 exits when 5.A and 5.B are green and combined-suite is still zero
failures.

**Status (2026-05-03): both slices GREEN via targeted tests
(7 + 6 = 13 new tests; 68-test cross-class smoke green). Awaiting user-run
combined-suite at phase boundary (canonical command in §0.5).**

**Phase 5 boundary follow-up (2026-05-03)**: first combined-suite run reported
5 unrelated graphql/webapi failures (`MG_USER_pietje does not exist` etc.).
Both reproducible-in-isolation tests pass on master AND v2 in full-class runs
— diagnosis was cross-suite Postgres state leakage from the new
`@Nested ColumnGrantEnforcement` tests. Fix: moved the 7 column-grant tests
out of `SqlRoleManagerTest` (sql module) into a new top-level test class
`TestRoleManagerColumnGrantEnforcement` in the `nonparallel-tests` module,
which runs LAST in the canonical combined-suite so any state leakage cannot
affect downstream modules. SQLException supertype used in place of
PSQLException to avoid pulling the postgres driver into nonparallel-tests'
classpath. Fixture schema name shortened (`RmColGrantEnfA`) to keep policy
names under PG's 63-byte object limit. Result counts: sql `SqlRoleManagerTest`
55/55, nonparallel-tests `TestRoleManagerColumnGrantEnforcement` 7/7, 139-test
smoke green. `SqlMolgenisExceptionTest` (5.B, pure unit) stays in sql module.

**Phase 5 boundary follow-up (2026-05-04)** — additional fixes landed this turn:

1. **NPE null-guard at `SqlRoleManager.java:236`** — `getTablePermissionsForActiveUser`
   was dereferencing `database.getSchema(schemaName)` without a null-check.
   Returns null when the active user has no schema access (e.g., system-role-only
   user under a view-mode test). Cascade hid this: NPE in
   `GetCountFieldTest.systemRoleRange_noCustomRole_returnsPrivacyFloor` aborted
   the transaction and downstream tests reported "current transaction is aborted".
   Fix: `if (schema == null) return List.of();`. Targeted tests green
   (GetCountFieldTest 9/9, EffectiveSelectScopesTest 9/9, AggregationPermissionTest
   50/50, ExistsFieldTest 10/10, SqlMolgenisExceptionTest 6/6).

2. **`cleandb` `DROP OWNED BY` patch** — `clean-molgenis-database.sql` did bare
   `DROP ROLE` without first severing role-to-role grants. RLS v2's role grants
   in migrations 10/21 (Aggregator→Viewer, Exists→Range, Range→Aggregator)
   stress this hole; pre-existing on master, but exercised harder by v2.
   Inserted `DROP OWNED BY %I CASCADE;` before each `DROP ROLE`. Verified:
   `pg_roles MG_*` empty after cleandb (only freshly-recreated system roles
   remain). Targeted tests still green; TestGraphqlDatabaseFields 8/8.

**Known follow-up — defer to post-PR investigation**: combined-suite still
reports the same 5 graphql failures (JWT secret null) plus
`TestColumnTypeIsFile.initializationError` (transaction-aborted cascade).
Verified root cause: `MOLGENIS.database_metadata.settings` JSONB is being
truncated mid-suite — after sql:test it shrinks from the full init-key set
to `{"isOidcEnabled":"false","it-db-setting-key":"it-db-setting-value"}`,
losing `MOLGENIS_JWT_SHARED_SECRET`. Mechanism is in `SqlDatabase.tx`/`sync()`
asymmetry: tx-copy and parent settings maps go out of sync, then a subsequent
`setSettings(map)` saves the truncated map back. Likely culprit is `TestSettings`
in the sql module. **NEW to v2 per user (baseline test was clean)**, but
mechanism may be a pre-existing latent bug uncovered by a v2 ordering shift.
Postpone fix until after Phase 5 PR closes; track as separate workstream
under "test-isolation hardening" or as Phase 5.C if scope allows.

### Phase 6 — GraphQL surfaces

We adopt the **current branch's existing API design** (already in use in our
repo's master after merge of our prior work, or directly portable from this
branch's source):

1. **`GraphqlPermissionFieldFactory`** — port; extend with the new
   `selectScope` / `updateScope` ladder values used by v2.
2. **`change(roles, members)` mutation** — keep the signature; adapt the
   PermissionSet input type to carry per-table per-verb scopes plus
   `changeOwner` / `changeGroup` booleans.
3. **Group CRUD** added as new mutations: `createGroup`, `deleteGroup`,
   `addGroupMember`, `removeGroupMember`. Naming convention matches existing
   schema-management mutations (no `v2` prefix).
4. **Authorization scope ceiling** (port from current branch Phase 6.B):
   only schema MANAGER / OWNER can grant custom roles or modify groups in
   their schema; admin retains override.
5. **Permission inspection queries**: extend existing `_schema.roles` (if
   present) with `_schema.groups`; per-role permission listing reads from
   the role's COMMENT JSON.

Exit criteria: GraphQL surface complete; permission UI in `apps/schema`
drives the model; e2e tests green.

#### Phase 6 port plan (2026-05-05)

Source-branch reference: `mswertz/poc/rls_using_one_role_and_policies` —
`GraphqlPermissionFieldFactory.java` (~360 lines) holds the canonical port
target; do NOT check it out, read via `git show`.

Pre-state (scout 2026-05-05):
- v2 has NO `GraphqlPermissionFieldFactory` yet.
- v2 `GraphqlSchemaFieldFactory.java` lines 115–189 declares Boolean-based
  permission types (`outputPermissionType`, `inputPermissionType`,
  `inputRoleType`); `changeRoles()` (lines 686–710) calls `schema.grant(...)`
  with a Boolean→TablePermission mapping. No SqlRoleManager wiring.
- v2 `_schema.roles` (lines 138–153, 544–581) returns Boolean perms via
  `roleToMap()`. No `_schema.groups` query.
- No group CRUD mutations anywhere in v2 GraphQL.
- v2 backend already has the right model: `PermissionSet` (per-table
  TablePermissions × per-verb SelectScope + changeOwner/changeGroup),
  `SqlRoleManager.{set,get}Permissions(role, ps)` round-trip.
- Existing GraphQL test surface: `TestGraphqlSchemaFields` (integration).
  No dedicated permission test class yet.

Divergences from source-branch port:
1. **Source's `inputPermissionFgType` carries SELECT as `[SelectScope]` list**
   (legacy multi-scope shape). v2's `PermissionSet.TablePermissions.select`
   is a single `SelectScope`. Port as single-value, not list.
2. **Source has no group CRUD** — net-new in v2 (slice 6.D) reading/writing
   `MOLGENIS.groups_metadata` (Phase 1 table).
3. **Source's `_schema.roles`** returns Boolean perms; v2 must expose the
   per-verb scope + booleans from PG ROLE COMMENT JSON.
4. **Storage**: source reads from SqlRoleManager too — keep alignment, no
   metadata-table fallback.

Slice plan (each slice independently testable, RED-GREEN per slice):

- **6.A — Port `GraphqlPermissionFieldFactory` (types + helpers).** New file
  `backend/molgenis-emx2-graphql/.../GraphqlPermissionFieldFactory.java`
  with: `selectScopeEnumType`, `updateScopeEnumType` (DELETE/INSERT/UPDATE
  use single SelectScope value, restricted via Java-side validation to
  NONE/OWN/GROUP/ALL), `effectivePermissionType`, `rolePermissionsOutputType`,
  `inputPermissionType`, `inputRoleType` matching v2's PermissionSet shape
  (single SelectScope per verb + changeOwner/changeGroup booleans). Helper
  coercions (`toSelectScope`, `toPermissionSet`). No mutation wiring yet.
  Tests: factory unit test asserting GraphQL type names + field shapes.
  **Status (2026-05-05): GREEN — 11/11 tests pass.** Per-table type renamed
  `tablePermissionInputType` / `tablePermissionOutputType` (clearer than
  `inputPermissionType` once `inputRoleType` exists alongside). Single
  `SelectScope` enum reused for all verbs (no separate `UpdateScope`);
  write-verb validation rejects view-mode scopes (EXISTS/COUNT/RANGE/
  AGGREGATE). `changeOwner` / `changeGroup` live on outer `inputRoleType`,
  matching `PermissionSet` shape. Private constructor — all static.
- **6.B — Replace `change(roles)` mutation with PermissionSet path.**
  Replace existing `inputRoleType`/`changeRoles` in `GraphqlSchemaFieldFactory`
  with the 6.A types. `changeRoles` calls `SqlRoleManager.setPermissions(role,
  ps)` with full per-verb scopes + change flags. Tests in
  `TestGraphqlPermissions` (new): set per-verb scopes via mutation; round-trip
  via `_schema.roles`.
  **Status (2026-05-05): GREEN — 4/4 tests pass.** `inputPermissionType` and
  old `inputRoleType` (Boolean-based) removed from `GraphqlSchemaFieldFactory`;
  `change(roles)` arg now uses `GraphqlPermissionFieldFactory.inputRoleType`.
  `changeRoles` refactored: skips system roles (continue), creates custom role
  via `SqlRoleManager.createRole(schema, name, desc)` if not present, calls
  `toPermissionSet` + `setPermissions`. `mapToTablePermission` deleted (no
  callers). `SqlDatabase`/`SqlRoleManager` imports added. `TestGraphqlPermissions`
  covers: round-trip via PermissionSet, idempotent overwrite, invalid view-mode
  scope throws, mixed system+custom input succeeds. Schema name `TGraphqlPermSchema`
  (short) to keep policy names under 63-byte PG limit. Regression: 11/11
  `TestGraphqlPermissionFieldFactory` + 25/25 `TestGraphqlSchemaFields` green.
- **6.C — Scope-ceiling authorization on role grant/edit.** Port
  `requireManagerOrOwner()` from source into 6.A. Wire into mutation entry
  points. Tests: Manager/Owner allowed; Editor/Viewer denied; admin allowed.
  **Status (2026-05-05): GREEN — 10/10 tests in `TestGraphqlPermissions` (4 from
  6.B + 6 new); 11/11 `TestGraphqlPermissionFieldFactory`; 25/25
  `TestGraphqlSchemaFields`; total 46→51 tests, 0 failed, 0 skipped.**
  `requireManagerOrOwner(Database db, Schema schema)` added as public static to
  `GraphqlPermissionFieldFactory`: admin short-circuits; null schema or schema
  where active user lacks Manager/Owner throws `MolgenisException("Only Manager
  or Owner can grant custom roles on schema <name>")`. `changeRoles` in
  `GraphqlSchemaFieldFactory` checks for any custom role in the input list first;
  if present, calls `requireManagerOrOwner` before any `SqlRoleManager` work
  (system-role-only inputs bypass the gate, preserving master-parity for system
  role assignment). No-role user test exercises `requireManagerOrOwner` directly
  (user has no schema access so `database.getSchema()` returns null — guard
  handles the null case correctly). `GraphqlConstants.java` also staged (was
  unstaged from 6.A/6.B).
- **6.D — Group CRUD mutations.** Add to schema mutation surface:
  `createGroup(schema, name)`, `deleteGroup(schema, name)`,
  `addGroupMember(schema, group, user)`, `removeGroupMember(schema, group,
  user)`. All write `MOLGENIS.groups_metadata` (Phase 1 table). Apply
  `requireManagerOrOwner` ceiling. Tests: round-trip create→add→remove→
  delete; ref_array FK rejects non-existent users; non-Manager denied.
  **Status (2026-05-05): GREEN — 62/62 tests pass (51 from 6.A–6.C/6.F + 11
  new in TestGraphqlGroups). 0 failures, 0 skipped.** Implementation:
  four new `public` methods on `SqlRoleManager` (`createGroup`, `deleteGroup`,
  `addGroupMember`, `removeGroupMember`) use `getJooqAsAdmin` for admin-level
  writes to `MOLGENIS.groups_metadata`; four matching `GraphQLFieldDefinition`
  builders in `GraphqlSchemaFieldFactory` delegate to `SqlRoleManager` and call
  `requireManagerOrOwner` at entry. Registered in `GraphqlFactory.forSchema`.
  `addGroupMember` is idempotent via `array(SELECT DISTINCT unnest(users ||
  ARRAY[user]))`. `removeGroupMember` is idempotent via `array_remove`. Mutation
  arguments: `createGroup(name)`, `deleteGroup(name)`, `addGroupMember(group,
  user)`, `removeGroupMember(group, user)`. Non-existent user validated via
  `MOLGENIS.users_metadata` lookup before array append.
- **6.E — `_schema.groups` query.** Add `groups: [GroupOutput]` field to
  `_schema` output. Each entry: `name`, `members: [User]`. Tests: query
  returns expected groups; visibility: any user can read groups (membership
  is not sensitive in v2 design — confirm with user if a ceiling is needed).
  **Status (2026-05-05): GREEN — 67/67 tests pass (TestGraphqlGroups 16/16
  including 5 new 6.E query tests, TestGraphqlPermissions 19/19,
  TestGraphqlPermissionFieldFactory 11/11, TestGraphqlSchemaFields 25/25). 0
  failures, 0 skipped.** Visibility decision: no ceiling — groups are readable
  by all users with schema access. Rationale: `current_user_groups()` already
  exposes group names as part of row filtering; gating the listing would be
  inconsistent without adding privacy. Implementation: `SqlRoleManager.listGroups
  (Schema)` queries `MOLGENIS.groups_metadata` via `getJooqAsAdmin` and returns
  `List<Map<String,Object>>` with `name`/`users` entries. `GraphqlConstants.GROUPS`
  constant added. `GraphqlPermissionFieldFactory.groupOutputType` (`MolgenisGroupOutput`)
  declared with `name: String` + `users: [String]`. Field `groups:
  [MolgenisGroupOutput]` added to `MolgenisSchema` unconditionally (parallel to
  `customRoles`). `queryFetcher` populates `GROUPS` key via `listGroups`. Phase 6
  complete — all slices 6.A–6.F are green.
- **6.F — `_schema.customRoles` per-verb scope output.** Decision: parallel
  field approach. `_schema.roles` unchanged (Boolean shape, system + custom
  roles, backward compat). New `_schema.customRoles: [MolgenisRoleOutput]`
  lists only custom roles with full PermissionSet output (per-table per-verb
  scopes + changeOwner/changeGroup booleans). Implementation:
  `GraphqlConstants.CUSTOM_ROLES` constant; `GraphqlPermissionFieldFactory
  .permissionSetToMap(roleName, ps)` converts PermissionSet → GraphQL map;
  `queryFetcher` in `GraphqlSchemaFieldFactory` calls `roleManager.listRoles(
  schema)` + `getPermissions(schema, role)` for each custom role and puts the
  result under `customRoles`. Schema output type gains the new field referencing
  `roleOutputType` from 6.A. Tests in `TestGraphqlPermissions`: round-trip
  (set via mutation, query customRoles, assert per-verb scopes + flags);
  multiple custom roles all listed; schema with no custom roles → empty array;
  system roles absent from customRoles; legacy `roles` field still contains
  system roles. Verified grep: no existing test queries `_schema { roles { … }
  }` → extend-vs-parallel verdict: parallel field is the safe choice (legacy
  `roles` output type uses Boolean per-verb shape; customRoles uses scope enum
  — two incompatible shapes, no single-field extension possible without a
  breaking change to the Boolean shape).
  **Status (2026-05-05): GREEN — 51/51 tests pass (TestGraphqlPermissions
  19/19 including 5 new 6.F tests, TestGraphqlPermissionFieldFactory 11/11,
  TestGraphqlSchemaFields 25/25). 0 failures, 0 skipped.**

#### Phase 6 review-fix pass (2026-05-05)
Four findings from post-phase review addressed:
- **Fix 1 (CRITICAL) — `description` round-trip.** `PermissionSet` gains `description: String`
  (getter/setter, default `""`). `serializePermissionSet` emits `"description"` key; `deserializePermissionSet`
  reads it. `createRole(Schema, String, String)` builds initial COMMENT JSON via
  `serializePermissionSet` instead of writing literal `{}`. `toPermissionSet` reads `description`
  from GraphQL input map (null → `""`). `permissionSetToMap` emits `description` in `customRoles`
  output. Semantics: null input → overwrite to `""` (explicit field; no implicit preservation).
  New test `customRolesQuery_description_roundTrip` verifies set→query echo and null-overwrites-to-empty.
- **Fix 2 (CRITICAL) — inline comments removed.** Two `// add custom roles …` / `// add groups …`
  lines deleted from `GraphqlSchemaFieldFactory.java:queryFetcher`.
- **Fix 3 (NIT) — `ARG_GROUP` extracted.** `private static final String ARG_GROUP = "group"` removed
  from `GraphqlSchemaFieldFactory`; `GROUP = "group"` added to `GraphqlConstants`; all usages updated.
- **Fix 4 (NIT) — `deleteGroup` check atomicity.** "not found" `MolgenisException` moved inside
  `getJooqAsAdmin` lambda, matching `createGroup`'s duplicate-check pattern.

#### Phase 6 user-review-comments pass (2026-05-05)
Seven user review comments applied:
- **Comment 1 — `UpdateScope` enum.** New `UpdateScope {NONE, OWN, GROUP, ALL}` enum in
  `org.molgenis.emx2`. `PermissionSet.TablePermissions.insert/update/delete` changed from
  `SelectScope` to `UpdateScope`. `SqlRoleManager` split `applyVerbPolicy` into
  `applySelectPolicy(SelectScope)` + `applyWritePolicy(UpdateScope, verb)`. Runtime
  view-mode rejection in `toPermissionSet` dropped — type system enforces it.
  `TestRoleManagerColumnGrantEnforcement` updated to `UpdateScope.ALL` for write verbs.
- **Comment 2 — `PermissionSet.schema` field.** Optional `schema: String` on `PermissionSet`.
  `SqlRoleManager.setPermissions` throws `MolgenisException` on schema mismatch.
  `changeRoles` calls `ps.setSchema(schema.getName())`. GraphQL `inputRoleType`/`roleOutputType`
  gain `schemaName` field. `permissionSetToMap` 3-arg overload includes `schemaName`.
  New test `rolesQuery_schemaField_roundTrips`.
- **Comment 3 — Docs.** Two new files: `docs/molgenis/use_customroles.md` (user-facing)
  and `docs/molgenis/dev_graphql-rls.md` (developer GraphQL reference).
- **Comment 4 — Constants.** `MG_CHANGE_OWNER = "changeOwner"` and `MG_CHANGE_GROUP = "changeGroup"`
  added to `Constants.java`. All literal string occurrences replaced.
- **Comment 5 — Merged `roles` field; `customRoles` fully dropped.** `ROLES` returns system roles
  (description=roleName, tables=[]) plus custom roles in a single unified list, built via a single
  pass over `getRoleInfos()` using `role.isSystemRole()` to choose the mapping. `CUSTOM_ROLES`
  constant, `customRoles` GraphQL field, and the parallel `result.put(CUSTOM_ROLES, ...)` line are
  all removed. `TestGraphqlPermissions` tests renamed from `customRolesQuery_*` to `rolesQuery_*`;
  assertions updated to query `_schema { roles { ... } }`. Pre-existing factory bug (duplicate
  entries for custom roles) fixed as a side-effect. 68/68 targeted tests green.
- **Comment 6 — Central group management.** `createGroup`/`deleteGroup`/`addGroupMember`/
  `removeGroupMember` standalone mutations removed from `GraphqlSchemaFieldFactory` and
  unregistered from `GraphqlFactory`. Group management now via `change(groups: [MolgenisGroupInput])`
  (idempotent upsert + member replacement) and `drop(groups: [String!])`. `TestGraphqlGroups`
  fully rewritten to use new syntax.
- **Comment 7 — No `@SuppressWarnings`.** All unchecked-cast suppressions removed.
  `toPermissionSet` uses `Map<?,?>` + `extractString()` helper. `changeRoles` uses
  `instanceof String roleName` pattern matching. `permissionSetToMap` passes enum
  constants (not `.name()` strings) so GraphQL-Java coerces enum output correctly.

Targeted tests: `TestGraphqlPermissions` 17/17, `TestGraphqlPermissionFieldFactory` 13/13,
`TestGraphqlGroups` 8/8, `TestGraphqlSchemaFields` 14/14, `SqlRoleManagerTest` 55/55. 0 failed.

Phase 6 exits when 6.A–6.F are green and combined-suite is still zero
new failures (existing 5 graphql JWT failures stay deferred per user).

#### TestSettings relocate (2026-05-05)
Addresses the Phase 5 deferred follow-up (documented in "Phase 5 boundary follow-up 2026-05-03"):
`TestSettings` (sql module) was identified as the likely culprit for `MOLGENIS_JWT_SHARED_SECRET`
truncation — its `testDatabaseSetting`/`testDeleteDatabaseSetting` tests call `db.setSetting()`
inside `SqlDatabase.tx()`, which writes a truncated settings map back to `MOLGENIS.database_metadata.settings`,
losing the JWT secret installed by `InitDatabase`. `TestColumnTypeIsFile` (io module) then fails
with an init error because the secret is gone.

Fix: `TestSettings.java` moved from `:backend:molgenis-emx2-sql:test` to
`:backend:molgenis-emx2-nonparallel-tests:test` (same package `org.molgenis.emx2.sql`, same class name).
`nonparallel-tests` runs last in the canonical combined-suite, after `io`. No code changes — pure
relocation. `TestDatabaseFactory` is in `molgenis-emx2-sql:main` and already reachable via
`testImplementation project(':backend:molgenis-emx2-sql')` in `nonparallel-tests/build.gradle`.
Same precedent as `TestRoleManagerColumnGrantEnforcement` (Phase 5).

User should verify the combined-suite floor at the next phase boundary — specifically confirm
`TestColumnTypeIsFile` no longer reports `initializationError` and the JWT-secret-null failures
in graphql tests are reduced (or eliminated if this was their sole cause).

#### SqlDatabaseTest relocate (2026-05-05)
Addresses the confirmed cascade: `SqlDatabaseTest.OIDCFlagDefaultsFalse` / `enableOIDCFlagViaSettings`
call `sqlDatabase.setSettings(Maps.newHashMap())` (not `setSetting`, but the bulk-replace overload),
which truncates `MOLGENIS.database_metadata.settings` and wipes `MOLGENIS_JWT_SHARED_SECRET`. Downstream
graphql tests (`TestGraphqlAdminFields.testSetUserAdmin`, `TestGraphqlDatabaseFields.testRegisterAndLoginUsers`,
`TestGraphqlSchemaFields.testSession`, `TestTokenBasedAccess.testJWTgenerator`) then NPE in `JWTgenerator`.

Fix: same pattern as `TestSettings` — moved `SqlDatabaseTest.java` from `:backend:molgenis-emx2-sql:test`
to `:backend:molgenis-emx2-nonparallel-tests:test`. Added `'uk.org.webcompere:system-stubs-jupiter:2.1.8'`
to `nonparallel-tests/build.gradle` (was only in `molgenis-emx2-sql/build.gradle`). No code changes.

These repeated relocations point to a real underlying bug in `SqlDatabase.setSettings` / `tx` / `sync`:
the tx-copy/sync asymmetry means any test calling the bulk `setSettings(Map)` overload at module-test
scope permanently overwrites the database-level settings row, including keys the test didn't touch.
Any test in any module that calls `setSettings` will keep finding new ways to leak into later modules.
Tracked as Phase 7 hardening scope.

### Phase 7 — settings-merge fix (2026-05-05)

Root cause of the repeated settings-truncation failures fixed properly.

**Two-part fix in `SqlDatabase`:**

1. `setSettings(Map)` — MERGE semantics: loads current DB state via `MetadataUtils.loadDatabaseSettings(jooq)`, overlays the caller-supplied keys, persists the merged map. Partial callers (e.g., a test that only supplies `IS_OIDC_ENABLED`) no longer truncate keys they don't know about (e.g., `MOLGENIS_JWT_SHARED_SECRET`).

2. `removeSetting(String)` — new override: loads DB state, removes the key, persists, updates in-memory. Required because the base class `HasSettings.removeSetting` delegates to `setSettings(fullMapMinusKey)`, which under merge semantics would NOT remove the key (the DB-loaded existing state still contains it). The override bypasses that indirection.

3. `tx()` fresh-load: after `db.setJooq(ctx)`, calls `db.setSettingsWithoutReload(MetadataUtils.loadDatabaseSettings(ctx))` to ensure the tx-copy's in-memory state reflects committed DB state, not potentially stale parent in-memory.

**Test changes:**

- `SqlDatabaseTest.OIDCFlagDefaultsFalse` — replaced `setSettings(emptyMap())` (now a no-op under merge) with `removeSetting(IS_OIDC_ENABLED)`.
- `SqlDatabaseTest.enableOIDCFlagViaSettings` — replaced `setSettings({key:value})` calls with `setSetting(key, value)`.
- `TestSettingsMerge` — new test class in `nonparallel-tests` covering: merge-on-top-of-existing-keys, merge verified after `clearCache()`, explicit `removeSetting` deletes key from DB.

**Callers of `setSettings(emptyMap())` found:** `SqlDatabaseTest.OIDCFlagDefaultsFalse` — now fixed. No other production callers found.

**TODO:** evaluate whether `TestSettings` and `SqlDatabaseTest` can move back to the `molgenis-emx2-sql` module (they were relocated as a band-aid). With merge semantics, any test calling `setSettings(partial)` no longer truncates keys — the relocation is no longer strictly necessary for test isolation. A follow-up can confirm at the next combined-suite run.

**Bootstrap-tolerance follow-up (2026-05-05):** `MetadataUtils.loadDatabaseSettings` now guards against missing MOLGENIS schema/table during bootstrap — returns empty `LinkedHashMap` if schema absent or `database_metadata` row missing. Same guard pattern as `getVersion()`. Fixes `AToolToCleanDatabase` / fresh-DB init path that called `tx()` before MOLGENIS schema existed. `nonparallel-tests:test` 12/12 green.

#### Phase 6 `TablePermissionsGraphqlTest` green (2026-05-05)
`TablePermissionsGraphqlTest` (15 tests in `molgenis-emx2-webapi`) fully green. Five production bugs
found and fixed:

1. **Policy name overflow → SHA-1 hash fallback.** `buildPolicyName` in `SqlRoleManager` now truncates
   overlong names (> 63 bytes) to `MG_P_` + 12-char SHA-1 hex instead of throwing. Fixes tests with
   long schema names like `TablePermissionsGqlTest`.

2. **`pg_authid` permission denied.** `getPermissions(Schema, String)` used user-context jooq to query
   `pg_authid` (superuser-only). Fixed by using `database.getJooqAsAdmin(...)`.

3. **`createRole(Schema, String, String)` missing grants.** New role-creation path (Phase 6) was missing
   `GRANT existsRole TO fullRole` (schema visibility) and `GRANT fullRole TO ownerRole WITH ADMIN OPTION`
   (owner delegation). Added both, matching the old `createRole(String, String)` path.

4. **`grant(String, String, TablePermission)` doesn't update JSON.** The old `SqlSchema.grant()` API only
   applied PG grants; `resolvePermissionsForRole` reads from the JSON comment in `pg_authid`. Fixed by
   adding `updateJsonPermission(...)` after `applyPgGrants`. Now `schema.grant()` keeps JSON + PG in sync.
   Tests 10 and 13 use this path.

5. **Custom role member add/remove requires admin.** Three separate paths now use admin elevation:
   - `changeMembers` in `GraphqlSchemaFieldFactory`: routes custom-role grants through `SqlRoleManager.grantRoleToUser` (which already calls `becomeAdmin()`) instead of `schema.addMember()`.
   - `executeRemoveMembers` in `SqlSchemaMetadataExecutor`: `REVOKE role FROM user` now uses `db.getJooqAsAdmin(...)`.
   - `dropRoles` in `GraphqlSchemaFieldFactory`: uses `roleManager.deleteRole(schema, roleName)` (new `DROP OWNED BY` path) instead of `schema.deleteRole()` (old path that doesn't remove column-level grants, causing `DROP ROLE` to fail).

Targeted tests: `TablePermissionsGraphqlTest` 15/15, `SqlRoleManagerTest` 55/55,
`TestGraphqlPermissions` 16/16, `TestGraphqlPermissionFieldFactory` 13/13. 0 failed.

### Phase 7 — pg_authid → pg_roles + revoke/drop JSON sync (2026-05-05)

All `pg_authid` table references in `SqlRoleManager` replaced with `pg_roles` (publicly-readable view; same `rolname`/`oid` columns). The `pg_shdescription` join clause retains `classoid = 'pg_authid'::regclass` (catalog class identifier, not a table read). Removes superuser dependency on all comment-read paths; `getJooqAsAdmin` retained only for `COMMENT ON ROLE` writes.

Two additional bugs fixed discovered by this change:

1. **`revoke()` did not update JSON comment.** `REVOKE ALL ON table FROM role` removed PG privilege but left the role's JSON comment intact; `resolvePermissionsForRole` reads that comment, so the user still appeared to have select access. Fixed: `revoke()` now calls `clearJsonPermission(fullRole, tableName)` which removes the table key from the JSON and writes `COMMENT ON ROLE`.

2. **`dropTable()` did not clear JSON.** Dropping and recreating a table cleared PG ACLs (fresh table) but left all custom role comments unchanged; any role that previously had a grant on the table still appeared to have access. Fixed: `SqlSchema.dropTable()` now calls `roleManager().clearTableFromAllRoles(this, tableName)` before the drop.

Targeted tests: `TestTableRoleManagement` 26/26, `SqlRoleManagerTest` 38/38, `TablePermissionsGraphqlTest` 15/15, `TestGraphqlPermissions`+`TestGraphqlPermissionFieldFactory` 29/29. 0 failed.

### Phase 7 — Per-group role model + normalized storage + shared policies

**Goal**: redesign the storage and assignment model in place on this branch
to fix two structural defects of v2:

1. **v2 cannot express asymmetric collaboration** (REQ-1 in spec). One
   custom role per user per schema is an invariant baked into the policy
   generator, not an extension point.
2. **PG ROLE COMMENT JSON is the wrong storage** for permissions that are
   themselves a regulated artifact: not queryable in SQL, not indexable,
   not validatable, no audit trail, no transactional link to the GRANTs
   it drives.

**Process**: clean-sheet replacement on this branch. **No fresh worktree**,
**no migration script** — the branch is unreleased. Rewrite v2 Phases 2–3
in place; Phases 1, 4, 5, 6 mostly port verbatim with the storage swap.

**REQ-2 (BYPASSRLS) — RESOLVED 2026-05-06 (Path A).** Drop `BYPASSRLS`
from system roles; seed Owner/Manager/Editor/Viewer as immutable rows
in `role_permission_metadata` with `table_name='*'` (wildcard). Access
functions resolve `*` first then exact-table match. RLS becomes the
uniform authorization layer. See `.plan/specs/rls_v2.md` REQ-2.

#### What survives v2 verbatim

- `mg_owner` column + emx2 row-lifecycle trigger.
- `mg_groups` REF_ARRAY column shape.
- ref_array FK trigger for `mg_groups` integrity.
- GIN index on `mg_groups`, B-tree on `mg_owner`.
- `current_user_groups(schema)` STABLE function (driver changes — reads
  the new membership table — but signature and call sites are unchanged).
- View modes (EXISTS / COUNT / RANGE / AGGREGATE) Java enforcement —
  AGGREGATE / EXISTS stay Java-only.
- `SqlMolgenisException` 42501 → friendly message translation.
- GraphQL surface shape from Phase 6 — `PermissionSet`, `inputRoleType`,
  `roleOutputType`, `groupOutputType`, central change/delete mutations.
  The types map onto the new tables instead of COMMENT JSON.

#### What changes

##### Storage tables (replace v2 COMMENT JSON)

`MOLGENIS.role_permission_metadata` — canonical permission storage
- `schema_name` TEXT NOT NULL
- `role_name` TEXT NOT NULL
- `table_name` TEXT NOT NULL
- `select_scope` TEXT NOT NULL DEFAULT 'NONE'
- `insert_scope` TEXT NOT NULL DEFAULT 'NONE'
- `update_scope` TEXT NOT NULL DEFAULT 'NONE'
- `delete_scope` TEXT NOT NULL DEFAULT 'NONE'
- `change_owner` BOOLEAN NOT NULL DEFAULT FALSE
- `change_group` BOOLEAN NOT NULL DEFAULT FALSE
- `description` TEXT
- `updated_by` TEXT NOT NULL  (set to `current_user` on write)
- `updated_at` TIMESTAMPTZ NOT NULL DEFAULT now()
- PK (`schema_name`, `role_name`, `table_name`)
- FK `schema_name` → `MOLGENIS.schema_metadata(table_schema)` ON DELETE CASCADE
- (schema_name, table_name) → `MOLGENIS.table_metadata`: **no DB-level FK** because PG does not support conditional FKs and wildcard rows (`table_name='*'`) would violate it. Cascade behaviour on table drop is enforced in the application layer via a hook in `SqlSchema.dropTable` added in slice 7.C.
- CHECK `select_scope IN ('NONE','EXISTS','COUNT','RANGE','AGGREGATE','OWN','GROUP','ALL')`
- CHECK each write scope IN `('NONE','OWN','GROUP','ALL')`
- INDEX (`schema_name`, `table_name`) — policy access function lookup
- BEFORE UPDATE TRIGGER `mg_protect_system_roles` rejecting writes where `role_name IN ('Owner','Manager','Editor','Viewer')`. **DELETE is intentionally NOT trapped** at the SQL trigger layer (cascade-delete on schema drop is indistinguishable from explicit delete inside a trigger; would block legitimate cascade). DELETE protection is enforced at the Java layer in slice 7.C (`SqlRoleManager.deleteRole` rejects system role names; explicit DELETE on `role_permission_metadata` rows for system roles is not surfaced via any public API). INSERT permitted (seeder uses it on schema create).

**System-role seeder** (Path A — REQ-2 resolution): on schema create, insert four rows with `table_name='*'`:
- `Owner`,   `*`, select=ALL, insert=ALL,  update=ALL,  delete=ALL,  change_owner=true,  change_group=true
- `Manager`, `*`, select=ALL, insert=ALL,  update=ALL,  delete=ALL,  change_owner=true,  change_group=true
- `Editor`,  `*`, select=ALL, insert=ALL,  update=ALL,  delete=ALL,  change_owner=false, change_group=false
- `Viewer`,  `*`, select=ALL, insert=NONE, update=NONE, delete=NONE, change_owner=false, change_group=false

Access functions resolve `(schema, role, table='*')` first; if absent fall back to `(schema, role, table=<exact>)`. System roles never have exact-table rows; custom roles never have `*` rows.

`MOLGENIS.group_membership_metadata` — replaces v2's `groups_metadata.users` array
- `user_name` TEXT NOT NULL
- `schema_name` TEXT NOT NULL
- `group_name` TEXT NOT NULL
- `role_name` TEXT NOT NULL
- `granted_by` TEXT NOT NULL  (`current_user` at insert time)
- `granted_at` TIMESTAMPTZ NOT NULL DEFAULT now()
- PK (`user_name`, `schema_name`, `group_name`, `role_name`)
- FK (`schema_name`, `group_name`) → `MOLGENIS.groups_metadata(schema, name)` ON DELETE CASCADE
- FK `user_name` → `MOLGENIS.users_metadata(username)` ON DELETE CASCADE
- INDEX (`user_name`, `schema_name`)

`MOLGENIS.groups_metadata` — drop the `users` REF_ARRAY column. Membership
moves to `group_membership_metadata` entirely. (Clean sheet — no backfill.)

**Audit history is deferred** — out of scope for Phase 7. Per-table
`_history` siblings + AFTER triggers can be added later without changing
the core storage shape. Tracked as a Phase 8/9 hardening item.

##### Access functions (drive RLS policies)

```sql
MOLGENIS.mg_can_read(p_schema, p_table, p_groups, p_owner) RETURNS BOOLEAN
  -- ALL: any membership of the user in this schema whose role has
  --      select_scope='ALL' on (p_schema, p_table).
  -- GROUP: membership where role.select_scope='GROUP'
  --        AND m.group_name = ANY(p_groups).
  -- OWN: p_owner = current_user AND any membership where role.select_scope='OWN'.

MOLGENIS.mg_can_write(p_schema, p_table, p_groups, p_owner, p_verb) RETURNS BOOLEAN
  -- USING-side. Same three branches keyed off insert/update/delete_scope.

MOLGENIS.mg_can_write_all(p_schema, p_table, p_groups, p_owner, p_verb) RETURNS BOOLEAN
  -- WITH-CHECK side. Subset semantics: every group in p_groups must be
  -- one the user has GROUP-or-ALL write authority in for this verb.
  -- Closes "share row into a group I'm not in" hole by construction.
```

LANGUAGE sql STABLE PARALLEL SAFE. The three functions UNION two
sources of "user's effective role authority" (Q1 decision, 2026-05-06):

1. **System-role branch** — for each of `Owner`/`Manager`/`Editor`/`Viewer`,
   if `pg_has_role(current_user, 'MG_ROLE_<role>_<schema>', 'USAGE')`,
   pull the wildcard row `(schema, role, table='*')` from
   `role_permission_metadata`.
2. **Custom-role branch** — JOIN `group_membership_metadata` to
   `role_permission_metadata` keyed off `current_user` (the existing
   slice 7.B path).

System roles have only wildcard rows; custom roles have only exact-table
rows. The branches compose cleanly: a user holding both a system role
and a custom role in a schema gets the union of both authorities.

##### Verb-level GRANT model (Q2/Q4 decision, 2026-05-06)

**Custom roles are NOT PG roles.** They exist only as rows in
`role_permission_metadata`; `group_membership_metadata.role_name` is a
string identifier, no `MG_ROLE_<custom>_<schema>` PG role created.

**System roles keep their existing PG roles** (`MG_ROLE_OWNER_<schema>`
etc.) with their existing table-level GRANTs from master. Membership in
a system role is granted via `addMember(schema, user, systemRole)`.

**Verb-level GRANT vehicle for custom-role users**: a new per-schema
PG role **`MG_ROLE_<schema>_MEMBER`** (NOBYPASSRLS) exists per schema.
Lifecycle:
- Created in `MetadataUtils.executeCreateSchema` alongside the four
  system roles.
- Granted to `MG_USER_<user>` whenever the user gets their first
  `group_membership_metadata` row in this schema; revoked when the
  user's last `group_membership_metadata` row in this schema disappears.
- Receives `GRANT SELECT, INSERT, UPDATE, DELETE ON <schema>.<table>`
  on every RLS-enabled table. The GRANT is added by slice 7.D's policy
  emitter when RLS is enabled on a table; revoked when RLS is disabled.
- Tables WITHOUT RLS (i.e. no custom-role activity yet — Q3 decision)
  remain accessible only to system roles via their existing GRANTs.

This keeps non-RLS tables on the master GRANT path (Q3), keeps custom
roles purely declarative (no PG role inventory to manage), and keeps
the verb-level access path uniform (one MEMBER role per schema instead
of one PG role per custom role).

**RLS table lifecycle** (Q3 decision):
- A table flips to RLS-enabled on the first non-NONE EXACT-table row in
  `role_permission_metadata` for `(schema, table_name=<exact>)`.
  Wildcard rows do NOT trigger RLS-enable — they belong to system roles
  whose authority flows through the existing GRANT path on non-RLS
  tables and through policy predicates on RLS tables.
- A table flips back to RLS-disabled when the last non-NONE EXACT-table
  row disappears.

##### Per-table policy template (4 policies, never per-role)

```sql
ALTER TABLE <schema>.<table> ENABLE ROW LEVEL SECURITY;

CREATE POLICY mg_p_<table>_select ON <schema>.<table> FOR SELECT
  USING ( MOLGENIS.mg_can_read(...) );

CREATE POLICY mg_p_<table>_insert ON <schema>.<table> FOR INSERT
  WITH CHECK ( MOLGENIS.mg_can_write_all(..., 'insert') );

CREATE POLICY mg_p_<table>_update ON <schema>.<table> FOR UPDATE
  USING      ( MOLGENIS.mg_can_write    (..., 'update') )
  WITH CHECK ( MOLGENIS.mg_can_write_all(..., 'update') );

CREATE POLICY mg_p_<table>_delete ON <schema>.<table> FOR DELETE
  USING ( MOLGENIS.mg_can_write(..., 'delete') );
```

Policy count: O(tables × 4). Independent of roles, groups, users.
Capability changes never emit policy DDL — policies call functions that
read current capability state. v2's per-(role × table × verb) emitter is
deleted.

##### Privacy floor in SQL

`MOLGENIS.mg_privacy_count(p_table TEXT, p_filter TEXT) RETURNS BIGINT`
returns `CEIL(COUNT(*)/10)*10` over the post-RLS-filtered table.
`SqlQuery.getCountField` calls this when the user's effective view-mode
scope is COUNT or RANGE. Direct-SQL `SELECT count(*)` by a COUNT-scoped
user remains a documented residual gap (out of scope for Phase 7;
requires column-level approach or wrapping view).

#### Refinements over the raw v3 prompt

1. **REQ-2 BYPASSRLS resolved before 7.B.** Pick option 1, 2, or 3 from
   the spec. The redesign is incomplete if RLS remains advisory for
   system roles.
2. **Column-level `change_owner` / `change_group` GRANTs**: v3 prompt
   keeps `MG_ROLE_<role>` PG-role inheritance, which makes the column
   write authority "any group is enough" — a known soft spot v3 itself
   defers. **Alternative**: drop `MG_ROLE_<role>` GRANT; enforce
   `mg_owner` / `mg_groups` mutability via a verb-aware predicate inside
   `mg_can_write_all` that checks the column being changed. Decide in
   slice 7.A scout; default to the trigger/predicate path unless it
   benchmarks materially slower than column GRANTs.
3. **`mg_can_write_all` performance**: `p_groups <@ array_agg(...)`
   evaluates per-row. Acceptable in spec but BENCHMARK before slice 7.C
   exits. If the EXPLAIN shows pathological plans, fall back to a
   precomputed materialized view of `(user, schema, table, verb) →
   groups[]` updated by trigger on capability/membership changes.
4. **Schema-wide ALL via capability row**: a user with role X in any
   group, where X has `select_scope='ALL'` on T, sees ALL rows in T
   regardless of the row's `mg_groups`. This is intended; write an
   explicit assertion test so it doesn't read as a bug to a future
   maintainer.
5. **`updated_by` / `granted_by` audit columns**: set to `current_user`
   at write time. Document that this is the SET-ROLE'd identity, not the
   service account. If MOLGENIS introduces an "actor" concept distinct
   from `current_user` later, revisit.
6. **No `role_permission_metadata` row implies role NONE on that table**:
   a role exists if it has at least one membership row OR at least one
   capability row in the schema. `listRoles(schema)` → SELECT DISTINCT
   role across both tables.

#### Implementation slices

**7.A — Lock-in decisions; design scout**
- **REQ-2 — RESOLVED (Path A)**: drop `BYPASSRLS` from system roles;
  materialise Owner/Manager/Editor/Viewer as immutable rows in
  `role_permission_metadata` with `table_name='*'` (wildcard). Access
  functions resolve `*` first, then exact `table_name` match. System-role
  authority flows through the same policy predicates as custom roles —
  no allow-all branch in the policy template. See spec REQ-2 for seed
  values and immutability enforcement.
- **System-role-on-RLS-transition behaviour**: when a normal table
  becomes an RLS table, pre-existing PG GRANTs to system roles persist
  but are no longer sufficient on their own; the seeded `*`-wildcard
  rows in `role_permission_metadata` carry the access through the
  policy predicates. No silent loss of access because the seeder runs
  on schema create (and on Phase 7 migration of existing schemas).
- **REQ-3 — RESOLVED 2026-05-06 (option 2: predicate)**: `mg_can_write_all`
  receives `change_owner` / `change_group` booleans from
  `role_permission_metadata` and emits the predicate branch directly. No
  column-level GRANT path; the v2 column-GRANT logic in
  `SqlRoleManager.java:679-723` is deleted in slice 7.C. Scout
  (2026-05-06) confirmed no extra columns needed beyond the booleans
  already in the spec.
- **Symbol-clash scout (2026-05-06)**: clean. None of the new names
  (`role_permission_metadata`, `group_membership_metadata`,
  `mg_can_read`/`write`/`write_all`/`privacy_count`,
  `mg_protect_system_roles`, `seedSystemRoles`) collide.
- **users_metadata PK column = `username`** (literal, lowercase,
  one word). `group_membership_metadata.user_name` (snake_case for
  Phase 7 column convention) FK references `users_metadata(username)`.
- **BYPASSRLS removal scout (2026-05-06)**: no production code sets
  `BYPASSRLS` on system roles in master or v2; only `MG_USER_admin`
  carries it. The "drop BYPASSRLS" step in slice 7.B is a verify-only
  no-op. Path A is therefore purely additive (seed wildcard rows +
  uniform policy emission); no rollback risk on this axis.

**7.B — Foundation: storage tables, seeder, access functions**
- Add `createRolePermissionMetadata` and `createGroupMembershipMetadata` to
  `MetadataUtils`. Wire from `Migrations.initOrMigrate`.
- Add `mg_protect_system_roles` BEFORE UPDATE/DELETE trigger on
  `role_permission_metadata` (rejects writes to system role names).
- Add `seedSystemRoles(schema)` helper in `MetadataUtils`; call from
  `SqlSchema.create` (and from a one-shot Phase 7 migration loop over
  existing schemas on this branch).
- **Verify no `BYPASSRLS` on system roles** (scout 2026-05-06 confirms
  none exists in master or v2; only `MG_USER_admin` carries it). If a
  rogue setter is found during implementation, remove it; otherwise
  this is a no-op step.
- Drop `users` array from `groups_metadata` schema definition.
- Emit `mg_can_read`, `mg_can_write`, `mg_can_write_all` functions —
  resolve `(schema, role, table='*')` first, fall back to exact match.
- Emit `mg_privacy_count` function.
- Update `current_user_groups(schema_name)` to read `group_membership_metadata`.
- Targeted tests: `MetadataUtilsTest` (tables/functions/seed present;
  trigger rejects system-role mutation), `TestAccessFunctions`
  (truth-table coverage including system-role wildcard branch).
**Status (2026-05-06): GREEN.** `migration34.sql` adds both metadata tables,
  triggers, indexes, and access functions. `MetadataUtils.emitAccessFunctions`
  emits the SQL functions + `mg_privacy_count`. `seedSystemRoles` called from
  `SqlSchema.create`. `TestAccessFunctions`, `MetadataUtilsRolePermissionTest`,
  `TestSeedSystemRoles`, `GroupsMetadataTest`, `TestCurrentUserGroups` all green.

**7.C — `SqlRoleManager` rewrite + access-function fix-up**
- **Access function fix-up (carried over from 7.B)**: rewrite
  `mg_can_read` / `mg_can_write` / `mg_can_write_all` to UNION the
  system-role branch (pg_has_role for `MG_ROLE_<system>_<schema>` +
  wildcard row from `role_permission_metadata`) with the existing
  custom-role branch (JOIN `group_membership_metadata` → exact-table
  row). Add `TestAccessFunctions.systemRoleViaPgHasRole` covering the
  system-role path.
- **`MG_ROLE_<schema>_MEMBER` PG role** (Q4 decision):
  - Created in `MetadataUtils.executeCreateSchema` alongside the four
    system roles. NOBYPASSRLS. No GRANTs at creation — verb GRANTs are
    added by slice 7.D when tables go RLS-enabled.
  - `SqlSchema.addMember(...)` / `addGroupMembership(...)` grant
    MG_ROLE_<schema>_MEMBER to MG_USER_<user> if not already granted.
  - `removeGroupMembership` / removal of last group_membership row →
    revoke MEMBER from MG_USER_<user>.
- **`SqlRoleManager` rewrite**:
  - All `pg_shdescription` / `COMMENT ON ROLE` reads/writes deleted.
  - `serializePermissionSet` / `deserializePermissionSet` deleted.
  - v2's column-GRANT path for `mg_owner` / `mg_groups`
    (`SqlRoleManager.java:679-723`) deleted (REQ-3 option 2 locked).
  - `setPermissions(schema, role, PermissionSet)` → idempotent diff +
    UPSERT/DELETE on `role_permission_metadata`.
  - `getPermissions(schema, role)` → SELECT.
  - `listRoles(schema)` → SELECT DISTINCT.
  - `createRole(schema, name, description)` → INSERT initial NONE row(s)
    only. **No `MG_ROLE_<role>` PG role created** (Q2 decision).
  - `deleteRole(schema, role)` → DELETE rows (FK cascade). Reject system
    role names (Owner/Manager/Editor/Viewer) at Java layer; this is the
    second pillar of the immutability guarantee (alongside the SQL
    trigger that blocks UPDATE). No `DROP ROLE` for custom roles.
  - New `SqlSchema.dropTable` hook: before dropping the table, DELETE
    from `role_permission_metadata` WHERE schema_name=this AND
    table_name=<dropped table>. Replaces the missing DB-level FK on
    `(schema_name, table_name) → table_metadata`.
  - New: `addGroupMembership(schema, group, user, role)` /
    `removeGroupMembership(...)` — write `group_membership_metadata`
    AND wire the MEMBER PG-role lifecycle.
  - v2's `grantRoleToUser` exclusivity check **deleted**. The PK
    on `group_membership_metadata` enforces uniqueness per
    `(user, schema, group, role)` — no schema-wide single-role rule.
  - `getJooqAsAdmin` audit: only `pg_authid`-class operations need
    elevated access. `role_permission_metadata` reads are normal admin
    queries.
- **Targeted tests**:
  - `TestAccessFunctions` — add system-role-via-pg_has_role coverage.
  - `SqlRoleManagerTest` rewritten end-to-end against new tables.
  - `TestTableRoleManagement` adapted (revoke/drop semantics now via
    DELETE on rows; no more PG role per custom role).
  - New `TestMemberPgRoleLifecycle` — adding a user to a group grants
    MEMBER; removing their last group_membership revokes MEMBER.
**Status (2026-05-06): GREEN.** `SqlRoleManager` fully rewritten: all COMMENT
  JSON / `pg_shdescription` / v2 column-GRANT paths removed; storage is
  `role_permission_metadata` and `group_membership_metadata`. Access functions
  UNION system-role branch (pg_has_role) with custom-role branch.
  `MG_ROLE_<schema>_MEMBER` created per schema; lifecycle wired in
  `addGroupMembership` / `removeGroupMembership`. Sentinel row
  (`RPM_STUB_TABLE_SENTINEL = ""`) marks role existence before permissions set.
  `SqlRoleManagerTest` 23/23, `TestTableRoleManagement` 12/12,
  `TestMemberPgRoleLifecycle` 4/4, `TestAccessFunctions` green. 0 failed.

**7.D — Per-table policy template + RLS-enable lifecycle**
- Replace v2's per-(role × table × verb) emitter in `SqlRoleManager` /
  `SqlSchemaMetadataExecutor` with a per-table 4-policy emitter.
- Policy lifecycle driver (Q3 decision): RLS-enable triggers on the
  first non-NONE EXACT-table row in `role_permission_metadata` for
  `(schema, table_name=<exact>)`. Wildcard rows (`table_name='*'`) do
  NOT trigger RLS-enable. RLS-disable when the last non-NONE
  exact-table row disappears.
- On RLS-enable: ENABLE ROW LEVEL SECURITY + emit the 4 policies +
  `GRANT SELECT, INSERT, UPDATE, DELETE ON <schema>.<table> TO
  MG_ROLE_<schema>_MEMBER` (Q4 decision — verb-level GRANT vehicle for
  custom-role users).
- On RLS-disable: drop policies + DISABLE RLS + `REVOKE ... FROM
  MG_ROLE_<schema>_MEMBER`. Tables go back to system-role-only access
  via the master GRANT path.
- Capability changes within RLS-on state do NOT regenerate policies;
  they only INSERT / UPDATE / DELETE capability rows.
- Targeted tests:
  - `TestTablePolicies` covering ALL/GROUP/OWN/NONE for each verb,
    both USING and WITH-CHECK paths.
  - `TestRlsLifecycle` — table goes RLS-on when first custom-role row
    appears, RLS-off when last disappears; MEMBER GRANT toggles in
    sync.
  - `TestPolicyCount.fourPoliciesPerTable` (from spec acceptance set).
**Status (2026-05-06): GREEN.** `SqlRoleManager.enableRlsForTable` emits 4
  per-table policies calling `MOLGENIS.mg_can_read`/`mg_can_write`/
  `mg_can_write_all`. Policy count is exactly 4 per RLS table, independent
  of role/group count. `TestTablePolicies`, `TestRlsLifecycle`,
  `TestPolicyCount` all green.

**7.E — View-mode SQL function integration**
- Update `SqlQuery.getCountField` to emit `MOLGENIS.mg_privacy_count(...)`
  call when effective scope is COUNT or RANGE.
- Targeted test: `TestSelectScope` — direct-SQL count via a COUNT-scoped
  role returns the floored value through the function path.
**Status (2026-05-06): GREEN.** `SqlQuery.getCountField` routes through
  `mg_privacy_count` for COUNT/RANGE scopes. `TestSelectScope.directSqlCountIsFloored`
  green.

**7.F — Asymmetric collaboration acceptance**
- New test class `TestAsymmetricCollaboration` implementing the success
  scenario from REQ-1 / v3 prompt §"What success looks like":
  alice editor in study A, viewer in study B; assert read visibility,
  per-row update authority, and `mg_groups` mutation boundaries.
- Both unit-level (SqlRoleManager) and end-to-end (GraphQL) coverage.
**Status (2026-05-06): GREEN.** `TestAsymmetricCollaboration` 5/5 green
  (reads from both groups, updates A-tagged row, rejects B-only update,
  allows removing group, rejects share into foreign group C).

**7.G — GraphQL surface adaptation**
- `PermissionSet` GraphQL types map to `role_permission_metadata` rows.
- `change(groups: ...)` mutation writes `group_membership_metadata` instead
  of `groups_metadata.users` array.
- New mutation: `addRoleToGroup(schema, group, user, role)` /
  `removeRoleFromGroup(...)` — surface for per-group role assignment.
- `_schema.roles` reads `role_permission_metadata`; `_schema.groups` joins
  `group_membership_metadata` for member listings.
- Targeted tests: `TestGraphqlPermissions`, `TestGraphqlGroups`,
  `TestAsymmetricCollaboration` (e2e).
**Status (2026-05-06): GREEN.** GraphQL types and mutations fully adapted to
  normalized storage. `change(groups: [...])` writes `group_membership_metadata`;
  `_schema.roles` reads merged system + custom roles from `role_permission_metadata`.
  `GraphqlSchemaFieldFactory` null-schema guard + `rejectEscalation` for Manager
  privilege escalation. `GraphqlAdminFieldFactory` filters `MG_ROLE_<schema>_MEMBER`
  roles without `/` separator to prevent ArrayIndexOutOfBoundsException.
  `SqlRoleManager.setPermissions` re-inserts sentinel when empty PermissionSet
  preserves role existence. `TestGraphqlPermissions` 17/17, `TestGraphqlGroups`
  11/11, `GraphqlPermissionFieldFactoryTest` 14/14, `TestGraphqlAdminFields`
  passes (testUsers). `TablePermissionsGraphqlTest` 15/15. 0 failed.

**7.H — Cleanup**
- Delete `serializePermissionSet`, `deserializePermissionSet`, Jackson
  imports if unused.
- Delete `pg_roles` / `pg_shdescription` SQL fragments from
  `SqlRoleManager` (kept only for system role display, if anywhere).
- Delete `updateJsonPermission`, `clearJsonPermission`,
  `clearTableFromAllRoles` — replaced by row-level CRUD on
  `role_permission_metadata`.
- Update Decision log (replace COMMENT-JSON entries with normalized-table
  + per-group-role entries).
- Update spec table to reference new tests; flip REQ-1 from open to
  closed; flip REQ-2 according to 7.A decision.
**Status (2026-05-06): GREEN.** All COMMENT JSON serialization code deleted.
  `pg_shdescription` references removed. `updateJsonPermission`, `clearJsonPermission`,
  `clearTableFromAllRoles` deleted. `TestChangeOwner` 5/5, `TestChangeGroup` 4/4 green.

**7.I — Wipe + reinit**
- `./gradlew :backend:molgenis-emx2-sql:cleandb`.
- Re-run targeted suites; combined-suite by user.
**Status (2026-05-06): GREEN.** All targeted test classes pass. Combined-suite
  pending user-run at phase boundary (canonical command in §0.5).

Exit criteria:
- `TestAsymmetricCollaboration` green (the v2-failing scenario). ✓
- Policy count is exactly 4 per RLS table regardless of roles/groups,
  verified by `pg_policies` query. ✓ (`TestPolicyCount.fourPoliciesPerTable`)
- No remaining `pg_shdescription`, `COMMENT ON ROLE`,
  `serializePermissionSet`, or `clearJsonPermission` references in
  production code. ✓
- REQ-1 closed in spec; REQ-2 resolved (closed or explicitly accepted). ✓
- Direct-SQL count via COUNT-scoped role hits the privacy floor. ✓ (`TestSelectScope.directSqlCountIsFloored`)

**Phase 7 status (2026-05-06): ALL EXIT CRITERIA MET.** All slices 7.A–7.I
green via targeted tests. Combined-suite awaiting user-run at phase boundary.

**7.L — Bug fixes: MEMBER role admin option + grantSchemaUsage removal**

Two bugs fixed together as a single slice.

**Bug 1 — Manager/Owner lack ADMIN OPTION on `_MEMBER` role**: `executeCreateMemberRole` created
the `MG_ROLE_<schema>_MEMBER` PG role but did not grant it to Manager/Owner WITH ADMIN OPTION.
This caused `TestGraphqlGroups.changeGroups_asManager_succeed` to fail with "must have admin
option on role". Fix: add two GRANT statements at the end of `executeCreateMemberRole`, and
matching EXECUTE format statements in the `migration32.sql` DO loop for existing schemas.

**Bug 2 — `grantSchemaUsage` helper used non-existent PG role**: `TestChangeOwnerGroupSqlEnforcement`
had a helper that called `GRANT USAGE ON SCHEMA ... TO MG_ROLE_<schema>/enforcer`, but in RLS v2
custom roles are NOT PG roles. Fix: delete the helper and all callsites; `grantRoleToUser` already
grants the `_MEMBER` umbrella role (which carries schema USAGE) via `addGroupMembership`.

**Additional fixes found during implementation**:
- `mg_can_read`: added `OR rp.change_owner = true` to the custom-role branch so that users with
  `change_owner=true` can see rows after an ownership-transfer UPDATE (PostgreSQL implicitly applies
  SELECT USING to the new row's state during UPDATE WITH CHECK).
- `mg_privacy_count`: fixed implementation from `GREATEST(p_count, 10)` to
  `CEIL(p_count::numeric / 10) * 10` to match the intended round-up-to-nearest-10 semantics.
- `TestSqlRoleManagerEnforcement.java` renamed to `TestAccessFunctions.java` to match the public
  class name inside (Java requires file == class name).

Acceptance:
- `TestGraphqlGroups.changeGroups_asManager_succeed` passes — confirmed via targeted tests.
- `TestChangeOwnerGroupSqlEnforcement` (7 tests, all green).
- `TestAccessFunctions` (16 tests, all green).

**Status: GREEN** — all targeted tests pass.

**7.K — Unified role-assignment GraphQL mutation**

Data managers need a single `change(members:[...])` mutation that handles both
system roles (schema-wide) and custom roles (per-group). Currently `changeMembers`
in `GraphqlSchemaFieldFactory` reads `{email, role}` and routes by `isSystemRole`;
it cannot accept a `group` field, and `drop(members:[...])` takes plain strings,
not objects. `addGroupMembership` in `SqlRoleManager` is the correct back-end API
for custom-role assignment but is not exposed via GraphQL.

Acceptance:
- `change(members:[{user, role}])` accepts system role (Owner/Manager/Editor/Viewer)
  without group.
- `change(members:[{user, role, group}])` accepts custom role within a named group.
- Validation rejects custom role without group with a domain-level error (not a
  GraphQL schema-type error).
- Validation rejects system role supplied with a group (or normalises group to null)
  with a domain-level error.
- `drop(members:[{user, role, group}])` symmetric removal for custom-role assignments.
- Escalation guard: non-Owner/Manager cannot grant Owner or Manager.

Implementation notes:
- Rename/extend `MolgenisMembersInput` to add `user` (alias of current `email`) and
  optional `group` field; keep `email` as deprecated alias for back-compat.
- `changeMembers` routes: isSystemRole → `schema.addMember(user, role)` (existing);
  custom role → `roleManager.addGroupMembership(schema, group, user, role)`.
- `dropMembers` changes from `List<String>` to `List<Map<String,String>>` taking
  the same `{user, role, group?}` shape; system role → `schema.removeMember(user)`;
  custom role → `roleManager.removeGroupMembership(schema, group, user, role)`.
- Escalation guard already in `SqlSchemaMetadataExecutor.executeAddMembers`; verify
  it fires for the custom-role path too.

RED test: `TestGraphqlUnifiedRoleAssignment` (6 tests, all failing until implemented).

**Status: GREEN** — implemented. All 6 tests in `TestGraphqlUnifiedRoleAssignment` pass.

**7.M — Allow null group on custom-role grants (schema-wide custom membership)**

Data managers need a way to grant a custom role schema-wide, not only per-group.
Today `change(members:[{user, role}])` rejects custom roles without a group, and
the underlying `addGroupMembership` requires a non-null group. The model:
`(user, role, group?)` where `group = null` means schema-wide. Combinations with
`select: GROUP` are silently inert — the grant simply does not match GROUP-scoped
tables. OWN and ALL scopes work as expected.

Acceptance:
- `change(members:[{user, role}])` (no group, custom role) is accepted; row
  written with null `group_name`.
- `change(members:[{user, role, group}])` continues to work (existing behavior).
- `(alice, role, null)` where role has `Books select: ALL` → alice reads all rows.
- `(alice, role, null)` where role has `Books select: OWN` → alice reads own rows.
- `(alice, role, null)` where role has `Books select: GROUP` → alice sees nothing
  from this grant. Other grants she holds still apply.
- `drop(members:[{user, role}])` (no group) drops only the schema-wide row;
  group-scoped rows for the same `(user, role)` survive.
- `drop(members:[{user, role, group: "dept1"}])` drops only that group row;
  schema-wide row survives.

Implementation notes:
- Storage already supports schema-wide custom grants via the
  `DIRECT_GRANT_GROUP = "__direct__"` sentinel that `SqlRoleManager.grantRoleToUser`
  inserts. `addGroupMembership` requires non-null group; the schema-wide path
  goes through `grantRoleToUser`. No migration; reuse existing mechanism.
- `GraphqlSchemaFieldFactory.changeMembers`: drop the "custom role requires group"
  validation. When custom role + null/empty group → call
  `roleManager.grantRoleToUser(schema, role, user)` instead of
  `addGroupMembership`. Custom role + group → keep current `addGroupMembership` path.
- `GraphqlSchemaFieldFactory.dropMembers`: route custom role + null group →
  `roleManager.revokeRoleFromUser(schema, role, user)`. Custom role + group →
  keep `removeGroupMembership` path.
- `mg_can_read` / `mg_can_write`: existing GROUP-scope branch checks
  `m.group_name = ANY(p_groups)` where `p_groups` is the row's `mg_groups`.
  The sentinel `__direct__` will not appear in any row's `mg_groups`, so a
  schema-wide grant is automatically inert for GROUP scope. No SQL change needed.
- Sentinel hiding: ensure `_schema.groups` query and `Schema.getMembers` do not
  surface `__direct__` as a real group/membership row. Filter in GraphQL read path
  if not already filtered.
- Reserve `__direct__` as a forbidden group name in `addGroup` validation to
  prevent users from creating a real group that collides with the sentinel.

Tests (`TestSqlRoleManager` extension, Java API level, no mocks):
- `addGroupMembership_nullGroup_persistsRow`
- `nullGroupGrant_allScope_userReadsAllRows`
- `nullGroupGrant_ownScope_userReadsOwnRowsOnly`
- `nullGroupGrant_groupScope_userSeesNothing`
- `dropMembership_nullGroup_preservesGroupedRows`
- `dropMembership_specificGroup_preservesNullRow`
- One smoke test via GraphQL in `TestGraphqlUnifiedRoleAssignment` for the
  null-group accept path.

Docs: rewrite §"Groups and role assignment" in `docs/molgenis/dev_graphql-rls.md`
to explain schema-wide custom grants, GROUP-scope inertness, and partial-drop
semantics. Add a worked example showing `(alice, staff, null)` on a mixed-scope
role.

**Status: GREEN** — implemented and reviewed. 38 sql tests + 8 graphql tests pass. Sentinel reservation enforced at public API boundary in `SqlRoleManager.addGroupMembership` / `removeGroupMembership` / `deleteGroup`; internal sentinel path uses private `insertGroupMembership` / `deleteGroupMembership` helpers. `dropMembers` now calls `rejectCustomRoleEscalation` symmetrically with `changeMembers`. Custom-role escalation guard tested for both change and drop paths.

**7.N — Harmonize members & groups GraphQL output (read-side unification)**

After 7.K/7.M the WRITE side is unified — `change(members:[{user, role, group?}])`
covers system roles, custom roles per group, and schema-wide custom grants. The
READ side is still split: `_schema.members` returns only system-role grants
(from `pg_auth_members`) while `_schema.groups[].users` exposes custom-role
grants in a different shape. Tests cannot positively verify schema-wide custom
grants, and per-group per-user role information is missing.

Acceptance:
- `_schema.members` returns the union of system-role grants and custom-role
  grants from `MOLGENIS.group_membership_metadata`.
- `MolgenisMembersType` gains a nullable `group: String` field. `null` for
  system-role rows and for schema-wide custom grants (sentinel `__direct__`
  mapped to null on output). Set for group-scoped custom-role rows.
- `_schema.groups[].users` shape changes from `[String]` to
  `[{ name: String, role: String }]`. Each row is one `(user, role)` pair
  for that group, since a user can hold multiple custom roles in a group.
- `TestGraphqlGroups` and `TestGraphqlUnifiedRoleAssignment` updated to use
  the new shapes for both pre- and post-condition verification.
- Schema-wide custom grants (slice 7.M) become positively verifiable through
  the harmonized members endpoint (`group: null` row with custom role).

Implementation notes:
- Resolver `executeGetMembers` (or its GraphQL counterpart) UNIONs:
  - existing pg_auth_members source for system roles (group=null)
  - SELECT user, role, NULLIF(group_name, '__direct__') FROM
    MOLGENIS.group_membership_metadata WHERE schema = current schema
- `MolgenisGroupOutput.users` projection: change from raw `listGroups`
  user-name array to `(user, role)` pairs by querying
  `group_membership_metadata` grouped by `group_name`.
- Add new GraphQL output type `MolgenisGroupUserOutput { name, role }`.
- Backwards-compat sweep: only `apps/settings/src/components/Members.vue`
  consumes `_schema.members` (queries `{email, role}` — additive `group`
  field is safe). No frontend consumer of `groups[].users` found; the
  shape change is internal-test-only.
- Sentinel `__direct__` continues to be filtered from `_schema.groups`
  output (already in 7.M).

Tests:
- Update `TestGraphqlGroups` assertions for new `users { name, role }` shape.
- Update `TestGraphqlUnifiedRoleAssignment` to verify `members{email,role,group}`
  for both system and custom grants. Schema-wide custom grants become
  positively assertable (`group: null` rows).
- New tests: `members_returnsSystemAndCustomRoles_unionedShape`,
  `members_schemaWideCustomGrant_groupIsNull`,
  `groups_users_includeRolePerUser`.

**Status: GREEN** — implemented and reviewed.
- `MolgenisMembersType` gained nullable `group: String`; resolver UNIONs
  system-role grants (group=null) with `listCustomMemberships` (custom-role
  rows; `__direct__` sentinel mapped to null on output).
- `MolgenisGroupOutput.users` changed from `[String]` to
  `[MolgenisGroupUserOutput { name, role }]`; new helper
  `findGroupUserRolePairs` projects `(user, role)` pairs per group.
- Sentinel-leak guard: `listCustomMemberships` filters
  `role_name != 'member'` so the bare-membership sentinel role does not
  surface in the harmonized `_schema.members` output.
- `findGroupUserRolePairs` keeps sentinel rows so the legacy bare-membership
  flow (`change(groups: [{users: ["alice"]}])`) continues to display users
  in `groups[].users`.
- Frontend `Members.vue` (query `{email, role}`) unaffected (additive `group`).
- Tests: `TestGraphqlMembersHarmonized` (new, 4 tests including
  `addGroupMember_sentinelRoleNotInMembers`), `TestGraphqlGroups` (updated
  shape + new `users_includesRolePerUser`), `TestGraphqlUnifiedRoleAssignment`,
  `TestGraphqlSchemaFields` — all green.

### Phase 7.O — GraphQL test reorganization (one class per endpoint)

Principle: one `Test*` class per GraphQL endpoint. Class name reflects the
full GraphQL path: `_schema.X` family → `TestGraphqlSchema<X>`; top-level
endpoints (`_session`, `_admin`, `_database`) keep bare names.

Done as part of post-7.N sweep:
- `TestGraphqlSchemaGroups` — `_schema.groups` endpoint. 5/5 green.
- `TestGraphqlSchemaMembers` — `_schema.members` endpoint. Merged from
  `TestGraphqlMembersHarmonized` (READ) + `TestGraphqlUnifiedRoleAssignment`
  (WRITE) + 6 grant-related tests pulled from
  `TestGraphqlPermissionFieldFactoryIntegration`. 18/18 green, single fixture.
- `TestGraphqlSchemaRoles` — `_schema.roles` endpoint. 13 tests pulled from
  `TestGraphqlPermissionFieldFactoryIntegration` into `TestGraphqlPermissions`,
  then renamed. 30/30 green.
- `TestGraphqlSession` — new, 2 sessionPermissions tests extracted from the
  same integration class. 2/2 green.
- Deleted: `TestGraphqlPermissionFieldFactoryIntegration`,
  `TestGraphqlPermissions` (renamed to Roles), `TestGraphqlMembersHarmonized`,
  `TestGraphqlUnifiedRoleAssignment`.

Pending (later slices):
- Split `TestGraphqlSchemaFields` into `TestGraphqlSchemaTables`,
  `TestGraphqlSchemaColumns`, `TestGraphqlSchemaSettings` — the file currently
  mixes table DDL, column DDL, and schema-level settings.
- Rename `TestGraphqlAdminFields` → `TestGraphqlAdmin` (`_admin` endpoint).
- Rename `TestGraphqlDatabaseFields` → `TestGraphqlDatabase` (`_database` /
  `_schemas` endpoint).
- Keep as-is: `TestGraphqlObjectFilters`, `TestGraphqlCrossSchemaRefs` (query DSL,
  not endpoint-shape); `TestGraphqlPermissionFieldFactory` (pure unit tests on
  static helpers).

Status: PARTIAL — `_schema.*` family (groups, members, roles) +
`_session` complete and Schema-prefixed. `_admin` / `_database` /
`_schema` (tables/columns/settings) splits + renames deferred.

### Phase 7.P — `roles.tables` → `roles.permissions` rename

**Current shape** (`MolgenisRoleOutput` / `MolgenisRoleInput` in
`GraphqlPermissionFieldFactory.java`):

```graphql
roles { name tables { table select insert update delete } changeOwner changeGroup }
```

**Target shape**:

```graphql
roles { name permissions { table select insert update delete } changeOwner changeGroup }
```

**Why**: `tables` reads as "list of tables" but the rows are scoped permissions
per (table, verb). `permissions` is the accurate noun.

**Touch list**:
- `GraphqlPermissionFieldFactory.java`:
  - `roleOutputType.tables` (line ~114) → `permissions`.
  - `inputRoleType.tables` (line ~75) → `permissions` (mutation symmetry).
  - Optionally rename type aliases `tablePermissionOutputType` /
    `tablePermissionInputType` → `permissionOutputType` / `permissionInputType`.
- Any data-fetcher map keyed `"tables"` for the roles output (search for
  `Map.of("tables"` in role result assembly).
- `TestGraphqlSchemaRoles.java` — update query strings.
- `TablePermissionsGraphqlTest.java` (`molgenis-emx2-webapi` test) — update query strings.

**Future work — global-level roles endpoint**: when the same endpoint is
raised to global (`_database.roles` or `_admin.roles`), the `permissions`
block should accept a `schema` field so a single role object can declare
permissions across multiple schemas:

```graphql
roles { name permissions { schema table select insert update delete } ... }
```

At schema-scope the `schema` field would be omitted (defaulted to the
enclosing schema). Plan the rename so that adding `schema` later is
additive — do NOT bake the assumption "permissions are always within
current schema" into the data-fetcher signature; keep the row shape
compatible with future addition of a `schema` key.

### Slice 8.0 — Explicit RLS-enable per table

Implements the design locked in §8.4a. Decouples RLS column/policy
lifecycle from permission-grant lifecycle.

Order of sub-slices (each independently RED-GREEN):

**8.0.A — Schema column + migration** — **Status: GREEN (2026-05-07)**
- Added `rls_enabled BOOLEAN NOT NULL DEFAULT false` to
  `MOLGENIS.table_metadata`.
- Migration: folded the `ALTER TABLE … ADD COLUMN rls_enabled` into
  `migration32.sql` (canonical bulk RLS migration). Deleted leftover
  `migration33.sql` and `migration34.sql` (subsets of 32). Reset
  `SOFTWARE_DATABASE_VERSION` to 32 and dropped the version<33/34/35
  steps in `Migrations.java`. Branch is unshipped, so amending is safe.
- `TableMetadata.rlsEnabled` field + getter/setter; sync()'d.
- `MetadataUtils.saveTableMetadata` writes; `recordToTable` reads.
- Test: `TestRlsEnabledMetadataRoundtrip` (2/2 green) — defaults false +
  round-trips true after save/reload.

**8.0.B — Java API + scope-validation guard** — **Status: GREEN (2026-05-07)**
- Guard added in `SqlRoleManager.setPermissions` per-table loop: rejects
  `OWN`/`GROUP` on any of select/insert/update/delete scopes, and
  `changeOwner`/`changeGroup=true`, when the target table has
  `rls_enabled = false`.
- Error: `"OWN/GROUP scope requires RLS-enabled table; enable RLS on
  '<schema>.<table>' first"`.
- Pass-through: tables absent from schema metadata are not guarded
  (consistent with rest of `setPermissions`). `changeOwner`/`changeGroup`
  validated per-table in the loop — if the PermissionSet covers multiple
  tables and either flag is true, every non-RLS table triggers rejection.
- Test: `TestRlsEnabledScopeGuard` (4/4 green) — RED→GREEN cycle
  verified (3 rejection + 1 accept; rejection tests failed before guard).
- Regression: `TestRlsEnabledMetadataRoundtrip` 2/2, `SqlRoleManagerTest`
  23/23 — both green.

**8.0.C — DDL emitter: column + policy lifecycle tied to flag** — **Status: GREEN (2026-05-07)**
- `SqlTableMetadata.setRlsEnabled(true)`:
  - Adds `mg_owner` + `mg_groups` columns + indexes.
  - `ENABLE` + `FORCE ROW LEVEL SECURITY`.
  - Applies 4-policy template (verbatim from prior `enableRlsForTable`).
  - Backfills `mg_owner` from `mg_insertedBy` (quoted identifiers).
  - GRANTs SELECT/INSERT/UPDATE/DELETE to MEMBER role.
- `SqlTableMetadata.setRlsEnabled(false)`:
  - Pre-check (`rejectDisableIfPermissionsExist`): any row in
    `role_permission_metadata` for schema+table → MolgenisException
    "first remove permissions on '<schema>.<table>'".
  - Drops 4 policies + `mg_check_change_cap_*` trigger.
  - `DISABLE ROW LEVEL SECURITY`. REVOKEs grants.
  - Drops `mg_owner` + `mg_groups` columns (NEW behavior).
- Implicit triggering REMOVED:
  - `setPermissions`, `deleteRole`, `grant`, `revoke` no longer call
    `applyRlsTransition`.
  - Deleted: `applyRlsTransition`, `hasNonNoneExactRow`,
    `findTablesForRole`.
- Test: `TestRlsEnableDisableLifecycle` (5/5 GREEN) — RED→GREEN cycle
  verified.
- Regression: `TestRlsEnabledMetadataRoundtrip` 2/2,
  `TestRlsEnabledScopeGuard` 4/4 — green.
- **Expected breakage** (to fix in 8.0.F): `TestRlsLifecycle`,
  `TestPolicyCount`, `TestSelectScope`, `TestChangeGroup`,
  `TestChangeOwner`, `TestAsymmetricCollaboration`,
  `TestSchemaWideCustomGrants`, `TablePermissionsGraphqlTest`,
  `TestGraphqlSchemaRoles`, `TestTablePolicies`.

**8.0.D — Inheritance: cascade enable/disable to root + reject non-root** — **Status: GREEN (2026-05-07)**
- `SqlTableMetadata.setRlsEnabled` now cascades from root via
  `enableRlsCascade` / `disableRlsCascade` (walks `getSubclassTables()`
  recursively, all in one transaction).
- Non-root call rejected with `"Cannot enable/disable RLS on subclass
  '<name>' — call on root '<root>' instead"`.
- New child auto-inherits parent's flag: hook in
  `SqlTableMetadataExecutor.executeCreateTable` (after `executeSetInherit`)
  + second hook in `SqlTableMetadata.setInheritTransaction` (for
  inheritance set after table creation).
- Test: `TestRlsInheritanceCascade` (4/4 GREEN) — RED→GREEN cycle
  verified.
- Regression: `TestRlsEnableDisableLifecycle` 5/5,
  `TestRlsEnabledMetadataRoundtrip` 2/2, `TestRlsEnabledScopeGuard` 4/4,
  `TestInherits` green.

**Known limitation** (acceptable for POC; ticket-worthy for follow-up):
mg_insertedBy is a meta-column on the ROOT table only (per EMX2's
manual-FK inheritance design — see `SqlTableMetadataExecutor` line
107-109). On enable, mg_owner backfill from mg_insertedBy works for
root rows but child rows lack mg_insertedBy on the same physical row —
their mg_owner falls back to DEFAULT current_user instead of the
original creator. For a tree that has existing child rows at the
moment RLS is flipped on, mg_owner attribution on those child rows is
"user-who-flipped" rather than "original-creator". A correct fix would
JOIN child to root via FK during backfill; deferred.

**8.0.E — GraphQL surface**
- `_schema.tables[]` exposes `rlsEnabled: Boolean`.
- `change(tables:[{name, rlsEnabled}])` mutation accepts the flag.
- `change(roles:[{...}])` server-side validation: same error messages as
  the Java guard.
- RED: `TestGraphqlSchemaTables.rlsEnabled_*` — read + write +
  rejection tests (3-4 tests). New file (also satisfies §7.O split:
  `TestGraphqlSchemaFields` → `TestGraphqlSchemaTables`).
- GREEN: GraphQL field + fetcher + mutation handler.

**8.0.F — Migrate existing tests from implicit to explicit RLS-enable**
- Most likely the largest sub-slice by line count, smallest by risk.
- Test files that previously relied on first-grant-implicitly-enables-RLS
  must now call `setRlsEnabled(true)` (or the GraphQL mutation) before
  granting `OWN`/`GROUP` permissions.
- Touch list (preliminary, expand during implementation):
  `TestSelectScope`, `TestChangeGroup`, `TestChangeOwner`,
  `TestRlsLifecycle`, `TestTablePolicies`, `TestPolicyCount`,
  `TestAsymmetricCollaboration`, `TestSchemaWideCustomGrants`,
  `TablePermissionsGraphqlTest`, `TestGraphqlSchemaRoles`.
- RED: any test that previously implicitly enabled RLS now fails with
  the new "OWN/GROUP requires RLS-enabled table" error → confirms the
  guard works.
- GREEN: add explicit `setRlsEnabled(true)` calls to test setup; tests
  pass again.

**Order constraint**: 8.0.A → 8.0.B → 8.0.C → 8.0.D → 8.0.E → 8.0.F.
A and B are pure additive; C is the behavioral pivot; D extends C; E is
GraphQL on top of A-D; F is mass test migration after the guard exists.

**Phase-boundary verification**: at the end of slice 8.0, run the
combined-suite invocation (user-driven, per CLAUDE.md). Expect only the
already-known pre-existing 5 failures in `TablePermissionsGraphqlTest`;
no new failures introduced.

### Phase 8 — Tests, performance, hardening

1. **Continuous master compatibility**: existing master tests must keep
   passing throughout development — Owner / Manager / Editor / Viewer /
   None all behave identically to master at every phase boundary. Run the
   full suite at the end of each phase. No separate "system role
   integration" phase.
2. Port test classes from current branch where applicable. Drop ones that
   tested the OLD scope/policy structure that no longer applies.
3. Cross-schema FK semantics — verify FKs to RLS tables behave correctly
   when row is invisible to current user.
4. **Inheritance semantics — RLS scope across an inheritance tree.**
   Open question: when an RLS-enabled table has subclasses (or vice versa,
   a subclass becomes RLS-enabled), does RLS apply to the full inheritance
   tree, or only from the enabled node downward?

   **Working position** (to verify with tests): RLS must apply to the full
   tree rooted at the highest RLS-enabled ancestor.

   **Why** — three cases to test:

   - **Parent has RLS, child does not declare RLS**:
     Child inherits the row-tag columns (`mg_owner`, `mg_groups`) via PG
     inheritance, and child rows are stored in the child table but visible
     through `SELECT ... FROM parent` via PG inheritance. If child rows
     bypass parent's RLS policies, a custom-role user reading the parent
     can see child rows they shouldn't. Therefore: applying RLS at the
     parent level MUST also enforce on the child table — the policies
     have to live on each child too, not only on the parent. Need to
     verify whether PG inheritance carries policies (it does NOT — RLS is
     per-table; `ENABLE ROW LEVEL SECURITY` and policies must be issued
     on every node).

   - **Subclass declares RLS, parent does not**:
     A user querying via the parent can SELECT from the union of all
     children. If the parent has no policies, child rows are visible
     through the parent regardless of the child's policies. So
     "subclass-only RLS" is semantically incoherent for any client that
     queries through the parent — and emx2 always exposes both.
     Therefore: enabling RLS on a subclass should require / propagate
     RLS upward to the root, not "downward only".

   - **`mg_groups` divergence between parent and child**:
     A child row's `mg_groups` could in principle differ from any parent
     row's `mg_groups`, because they are independent rows. But there is
     no understandable UX for "this row is more restricted because it's
     a subclass instance" without it also being expressible at the
     parent. The simplification we should validate: a row's
     `mg_groups` is a property of THE ROW, not of the leaf-most type;
     the column is declared once at the root and inherited unchanged.

   **Acceptance tests to write in Phase 8**:
   1. Parent table RLS-enabled, child table inherits — custom-role user
      with no group membership SELECTs through parent → must NOT see
      child rows whose `mg_groups` excludes them. (This is the trap test:
      verify that PG does NOT auto-propagate RLS through inheritance,
      and that emx2 issues policies on every child.)
   2. Same scenario but SELECT directly from child — same result.
   3. Subclass-only RLS attempt: declaring RLS on a child whose parent
      has none should either (a) propagate upward to the root, or
      (b) be rejected with a clear error. Decision needed in Phase 8;
      working position is (a).
   4. Insert/update on the child as a custom-role user — `mg_groups`
      WITH-CHECK enforcement comes from the child's policies; verify
      it's symmetric with parent.
   5. Cross-table-tree FK: a ref column from another schema points at
      the root parent — invisible-to-user rows in any subclass must not
      leak through the FK. (Composes with §8.3.)

   **Implementation implications**:
   - emx2's RLS-enable trigger / DDL emitter must walk the inheritance
     tree and apply `ENABLE ROW LEVEL SECURITY` + the 4-policy template
     to every node.
   - Adding a child to an existing RLS-enabled parent must apply the
     same policies on the new child at creation time.
   - `mg_owner` / `mg_groups` columns are declared on the root and
     inherit by PG semantics — no per-subclass copy needed.

4a. **RLS as an explicit table-level setting (raised 2026-05-07).**

   **Current behavior**: RLS is implicit — a table becomes RLS-managed
   the first time a custom role with non-`NONE` scope references it.
   `mg_owner` / `mg_groups` columns + the 4-policy template + child-tree
   propagation all happen as a side effect of granting the first
   permission.

   **Proposal**: make RLS an explicit table flag, set at table-create or
   alter time (`rlsEnabled: true` in EMX2 metadata, `ENABLE ROW LEVEL
   SECURITY` in PG). Permissions are granted independently and only
   take effect on tables that have RLS enabled.

   **Why this is the right cut-point**:
   - **Inheritance becomes well-defined.** RLS-enable is a DDL act on
     the root of an inheritance tree; child tables inherit the flag at
     creation. No surprise propagation at grant time. Trying to enable
     RLS on a child whose parent isn't RLS-enabled is rejected with a
     clear error (or implicitly raised to the root, decided in Phase 8).
   - **DBA / data-manager control is explicit.** "Is this table
     row-secured?" is answerable from metadata, not from "did anyone
     happen to grant a non-NONE custom-role permission on it yet?"
   - **Policy/column lifecycle is decoupled from permission lifecycle.**
     Adding/removing a custom role permission is a pure
     `role_permission_metadata` write — never triggers DDL.
   - **PG-native semantics.** `ENABLE ROW LEVEL SECURITY` in Postgres
     is itself an explicit ALTER TABLE; we'd be aligning with PG, not
     diverging.
   - **Migration story improves.** Existing schemas remain unchanged
     until the operator explicitly opts a table in. No "first grant"
     trap.

   **Cost**:
   - One extra step in the table-creation UI/API.
   - Small change to the DDL emitter: split "enable RLS on table" from
     "grant role X permission Y on table".
   - Existing implicit-enable code path is removed; tests that rely on
     the implicit path need to flip an explicit flag instead.

   **Decisions (locked 2026-05-07)**:
   1. **Flag location**: `MOLGENIS.tables_metadata.rls_enabled BOOLEAN
      DEFAULT false`. New tables default `false`. Migration: every
      existing table flips to `false` (no v2-RLS in prod yet).
   2. **Disable with existing permission rows**: **reject** with "first
      remove permissions" error. Operator does the cleanup explicitly.
      Once disable succeeds, drop `mg_owner` / `mg_groups` columns and
      the policies. Re-enable later starts from scratch (mg_owner
      defaults to `mg_insertedBy`, `mg_groups` defaults to empty).
      We can revisit if cascade-style "disable + cleanup in one op"
      becomes annoying in practice.
   3. **Inheritance**: enable/disable only on the root of an inheritance
      tree.
      - New subclass added under an RLS-enabled root: auto-inherits RLS.
      - Direct enable/disable on a non-root node: **reject** with
        "enable on root <X> instead".
      - Re-parenting an existing RLS-enabled subtree: not applicable —
        emx2 does not allow re-parenting. (Confirmed by user.)
      - Disable on a root with N children cascades atomically in one
        transaction (drops policies + columns on every node).
      - Existing rows at enable-time: `mg_owner` defaults to
        `mg_insertedBy`, `mg_groups` defaults to `{}`. **Empty
        `mg_groups` ⇒ no GROUP-scope role sees the row** until the
        operator populates it. No automatic "default group" backfill.
   4. **System-role interaction**: no change to authority resolution.
      - RLS off ⇒ no policies on the table; system roles use existing
        table-level GRANTs (master behavior).
      - RLS on ⇒ same wildcard system-role rows in
        `role_permission_metadata` drive the access functions called by
        the 4-policy template. Custom-role users still need a
        `group_membership_metadata` row to get authority through the
        `MG_ROLE_<schema>_MEMBER` GRANT.
      - Pathological state (RLS on, seed wildcard rows hand-deleted):
        treated as "operator broke things on purpose" — not defended
        against.
   5. **Scope availability gated by RLS-enable**:
      - `ALL` and `NONE` scopes are always allowed on any table.
      - `OWN` and `GROUP` scopes are only allowed on tables with
        `rls_enabled = true`.
      - **Java API**: throw `MolgenisException` when a permission row
        with `OWN` or `GROUP` scope is set on a table where
        `rls_enabled = false`. Symmetric for select / insert / update /
        delete scopes and for `changeOwner` / `changeGroup` capabilities.
      - **GraphQL API**: enums are static (cannot vary per table at
        introspection time). The single `SelectScope` / `UpdateScope`
        enum is exposed; server-side validation rejects `OWN`/`GROUP`
        on non-RLS tables with the same error as the Java API.
      - **UI**: reads `tables_metadata.rls_enabled` and greys out the
        `OWN` / `GROUP` options in the scope dropdown when RLS is off,
        with a tooltip "enable RLS on this table to use OWN/GROUP scope".
        Single source of truth = the flag.
   6. **`mg_owner` / `mg_groups` column lifecycle**: created at
      RLS-enable time regardless of which scopes are used. Dropped at
      RLS-disable time. Decoupled from per-role permission grants.
      Trivial storage cost on tables that only use `ALL`/`NONE` scopes;
      lifecycle is one-shot.

   **Status**: design locked. Implement as **slice 8.0** before the
   inheritance acceptance tests in §8.4 — those tests need a way to
   enable RLS on a parent without granting any permission first.

5. **Benchmark** target: 1M-row table, 5 custom roles, 100 groups, 10k
   users. Query latency < 2× non-RLS baseline. GIN index on `mg_groups`
   verified non-degraded.
6. Audit: `pg_policies` query patterns to list all policies for a (schema,
   role) pair — verify naming convention supports this.

Exit criteria: benchmark target met; full test suite green;
nonparallel-tests covers wildcard / heavy-DDL cases.

### Phase 9 — Migration & docs

1. Document the model in `docs/` with worked examples.
2. Upgrade path from master: existing schemas continue to work unchanged
   until a custom role with non-`NONE` scope is created on a table; at that
   point the foundation columns/triggers/policies are added on demand. No
   rewrite of existing data required.
3. Operator runbook: how to add a role, add a group, debug a "user can't
   see row X" issue (`pg_policies`, function output, role/group lookup).

Exit criteria: docs published; runbook reviewed by data managers.

## Decision log

- **Branch base**: master (not PR #6058). 6058 architecture diverges too far;
  rebase risk + adversarial PR-back outweighs alignment value. We borrow
  conventions, not code.
- **Membership storage** [v2 / superseded by Phase 7]: v2 split roles
  (PG GRANT, exclusive) from groups (`groups_metadata.users` REF_ARRAY).
  Phase 7 unifies into `MOLGENIS.group_membership_metadata` carrying per-group
  role; drops the `users` array from `groups_metadata` and the exclusivity
  rule.
- **Role + permission storage** [Phase 7]: `MOLGENIS.role_permission_metadata`
  (one row per `(schema, role, table)`) is canonical. PG roles
  `MG_ROLE_<role>` and the policy DDL are derived projections. Replaces v2's
  PG ROLE COMMENT JSON storage.
- **Policy count** [Phase 7]: 4 per RLS table (USING/WITH-CHECK over shared
  access functions), independent of role/group count. Replaces v2's
  per-(role × table × verb) emission. Capability changes never emit policy
  DDL after the table is RLS-enabled.
- **`changeOwner` / `changeGroup` flag storage** [Phase 7]: stored as
  BOOLEAN columns on `role_permission_metadata`. Column-level GRANT vs
  trigger-predicate enforcement decided in slice 7.A (REQ-3).
- **No base role inheritance** (Viewer / NONE): role explicitly lists
  per-table scopes. Few roles per schema, so verbosity is acceptable.
- **System roles** [v2 / superseded by Phase 7]: held BYPASSRLS as in master;
  custom-role mechanism was purely additive.
- **System roles** [Phase 7 — Path A, locked 2026-05-06]: NO BYPASSRLS.
  Owner / Manager / Editor / Viewer materialised as immutable seeded
  rows in `role_permission_metadata` with `table_name='*'` (wildcard).
  Authority flows through the same access functions / 4-policy template
  as custom roles. RLS is the uniform enforcement layer. Immutability
  enforced at trigger + Java + GraphQL layers. See spec REQ-2.
- **View modes Java-side**: privacy modes (EXISTS / COUNT / RANGE / AGGREGATE)
  enforced in `SqlQuery`, not in policies. Same as current branch.
- **Subgroups dropped as a separate concept**: replaced by flat per-schema
  groups. A user in a hierarchical structure simply joins multiple groups.
- **Disabled vs deleted users**: users are disabled, never deleted; orphan
  ownership is therefore not a concern.

## Open items

- Phase 0 scout findings on `MOLGENIS.users_metadata` PK shape — confirms or
  forces small adjustment to ref/ref_array column definitions.
- Benchmark numbers (Phase 7) once workload data is available — current
  target is < 2× non-RLS baseline; revisit if measured.

### Phase 7 — Post-merge review queue (2026-05-06)

User review of post-merge state. Action items collected; awaiting
go-ahead before dispatching cleanup agent.

**Definite cleanups (no decision needed):**
1. Merge `migration33.sql` into `migration34.sql`; drop the `version < 33`
   step. Single migration since branch ships from clean master.
2. Fold `SelectScope.java` (53 LOC) and `UpdateScope.java` (20 LOC) into
   `PermissionSet` as inner enums; delete the two files.
3. `GraphqlAdminFieldFactory.java:157` `.filter(role.contains("/"))` —
   band-aid for `MG_ROLE_<schema>_MEMBER` rows. Real fix: filter at the
   source (`executeGetMembers` SQL) so MEMBER PG role never appears in
   the user-facing list.
4. Move `rejectEscalation` from `GraphqlSchemaFieldFactory` into
   `SqlRoleManager` / `SqlSchema.addMember`. `MolgenisException`
   propagates up; one enforcement point.
5. Drop unused constants `PG_ROLES`, `ROLNAME` in `SqlRoleManager.java:28-29`.
6. Drop `system-stubs-jupiter` from `molgenis-emx2-sql/build.gradle:9`
   (unused there; legitimately used in `nonparallel-tests` and `webapi`).

**Outcomes (2026-05-06 cleanup pass):**
1–6. Applied. Files: `migration33.sql` (deleted, folded into 34),
   `SelectScope.java`/`UpdateScope.java` (deleted, moved into `PermissionSet`),
   `SqlSchemaMetadataExecutor.executeGetMembers` + `SqlDatabase.loadUserRoles`
   filter `%_MEMBER` at source (band-aid removed from
   `GraphqlAdminFieldFactory`), `rejectEscalation` moved to
   `SqlSchemaMetadataExecutor.executeAddMembers`, `PG_ROLES`/`ROLNAME`
   constants deleted, unused `system-stubs-jupiter` dep dropped from
   `molgenis-emx2-sql/build.gradle`.
7. Reverted. Both `SqlRoleManager.grantMemberRoleToUser` /
   `revokeMemberRoleFromUser` now use `database.getJooqAsAdmin(...)`;
   `runAsAdmin` method deleted from `SqlDatabase`. Tests green
   (`SqlRoleManagerTest`, `TestMemberPgRoleLifecycle`,
   `TestTablePermissionEnforcement`).
8. **Real fix — keep.** `setSettings` read-merge-write + `removeSetting`
   + tx-start reload are exercised by `TestSettingsMerge` (3 tests).
   Master's overwrite-only `setSettings` would lose unrelated keys when
   updating one — that test would fail on master.
9. **Applied.** `updateMembershipForUser` now wraps GRANT/REVOKE in
   `getJooqAsAdmin`, mirroring `executeRemoveMembers`. Dead `jooq`
   parameter removed from helper and call site.
10. Kept. App-layer hook stays.

## Out of scope

- Cross-schema custom roles (a role spans schemas). Roles are schema-local.
- User-defined privacy floors (currently hardcoded to 10 for RANGE).
- Audit logs for permission changes — can be added later as separate table.
- Migration tooling from PR #6058's data layout — out of scope.
