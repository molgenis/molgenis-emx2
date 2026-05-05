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
- FK `schema_name` → `MOLGENIS.schema_metadata(name)` ON DELETE CASCADE
- FK (`schema_name`, `table_name`) → `MOLGENIS.table_metadata(schema_name, name)` ON DELETE CASCADE — **deferrable, only enforced when `table_name <> '*'`** (wildcard rows for system roles do not reference a specific table)
- CHECK `select_scope IN ('NONE','EXISTS','COUNT','RANGE','AGGREGATE','OWN','GROUP','ALL')`
- CHECK each write scope IN `('NONE','OWN','GROUP','ALL')`
- INDEX (`schema_name`, `table_name`) — policy access function lookup
- BEFORE UPDATE/DELETE TRIGGER `mg_protect_system_roles` rejecting writes where `role_name IN ('Owner','Manager','Editor','Viewer')`. INSERT permitted (seeder uses it on schema create).

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

LANGUAGE sql STABLE PARALLEL SAFE. All three join `group_membership_metadata`
to `role_permission_metadata` via `(role_name, schema_name)` and key off
`current_user`.

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
- **REQ-3** (column-grant vs predicate for `change_owner`/`change_group`).
  Default to predicate-in-`mg_can_write_all` unless benchmarked slower.
  Update spec when locked.
- Confirm no `role_permission_metadata` / `group_membership_metadata`
  symbol clashes with master.
- Confirm `MOLGENIS.users_metadata(username)` PK shape unchanged.

**7.B — Foundation: storage tables, seeder, access functions**
- Add `createRolePermissionMetadata` and `createGroupMembershipMetadata` to
  `MetadataUtils`. Wire from `Migrations.initOrMigrate`.
- Add `mg_protect_system_roles` BEFORE UPDATE/DELETE trigger on
  `role_permission_metadata` (rejects writes to system role names).
- Add `seedSystemRoles(schema)` helper in `MetadataUtils`; call from
  `SqlSchema.create` (and from a one-shot Phase 7 migration loop over
  existing schemas on this branch).
- **Drop `BYPASSRLS` and `NOBYPASSRLS` from system role definitions in
  `MG_ROLE_OWNER_<schema>` / `MG_ROLE_MANAGER_<schema>` / `MG_ROLE_EDITOR_<schema>` / `MG_ROLE_VIEWER_<schema>`.**
- Drop `users` array from `groups_metadata` schema definition.
- Emit `mg_can_read`, `mg_can_write`, `mg_can_write_all` functions —
  resolve `(schema, role, table='*')` first, fall back to exact match.
- Emit `mg_privacy_count` function.
- Update `current_user_groups(schema_name)` to read `group_membership_metadata`.
- Targeted tests: `MetadataUtilsTest` (tables/functions/seed present;
  trigger rejects system-role mutation), `TestAccessFunctions`
  (truth-table coverage including system-role wildcard branch).

**7.C — `SqlRoleManager` rewrite**
- All `pg_shdescription` / `COMMENT ON ROLE` reads/writes deleted.
- `serializePermissionSet` / `deserializePermissionSet` deleted.
- `setPermissions(schema, role, PermissionSet)` → idempotent diff +
  UPSERT/DELETE on `role_permission_metadata`.
- `getPermissions(schema, role)` → SELECT.
- `listRoles(schema)` → SELECT DISTINCT.
- `createRole(schema, name, description)` → INSERT initial NONE row(s)
  + create `MG_ROLE_<role>` PG role only if column-grant path is chosen
  in 7.A.
- `deleteRole(schema, role)` → DELETE rows (FK cascade) + DROP ROLE.
- New: `addGroupMembership(schema, group, user, role)` /
  `removeGroupMembership(...)` — write `group_membership_metadata`.
- v2's `grantRoleToUser` exclusivity check **deleted**. The PK
  on `group_membership_metadata` enforces uniqueness per (user, schema, group,
  role) — no schema-wide single-role rule.
- `getJooqAsAdmin` audit: only `pg_authid`-class operations need elevated
  access. `role_permission_metadata` reads are normal admin queries.
- Targeted tests: `SqlRoleManagerTest` rewritten end-to-end against new
  tables; `TestTableRoleManagement` adapted (revoke/drop semantics now
  via DELETE on rows).

**7.D — Per-table policy template**
- Replace v2's per-(role × table × verb) emitter in `SqlRoleManager` /
  `SqlSchemaMetadataExecutor` with a per-table 4-policy emitter.
- Policy lifecycle driver: when first non-NONE capability row appears
  for `(schema, table)`, ENABLE RLS + emit policies. When last non-NONE
  row disappears, drop policies + DISABLE RLS.
- Capability changes do NOT regenerate policies; they only INSERT /
  UPDATE / DELETE capability rows.
- Targeted tests: `TestTablePolicies` covering ALL/GROUP/OWN/NONE for
  each verb, both USING and WITH-CHECK paths.

**7.E — View-mode SQL function integration**
- Update `SqlQuery.getCountField` to emit `MOLGENIS.mg_privacy_count(...)`
  call when effective scope is COUNT or RANGE.
- Targeted test: `TestSelectScope` — direct-SQL count via a COUNT-scoped
  role returns the floored value through the function path.

**7.F — Asymmetric collaboration acceptance**
- New test class `TestAsymmetricCollaboration` implementing the success
  scenario from REQ-1 / v3 prompt §"What success looks like":
  alice editor in study A, viewer in study B; assert read visibility,
  per-row update authority, and `mg_groups` mutation boundaries.
- Both unit-level (SqlRoleManager) and end-to-end (GraphQL) coverage.

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

**7.I — Wipe + reinit**
- `./gradlew :backend:molgenis-emx2-sql:cleandb`.
- Re-run targeted suites; combined-suite by user.

Exit criteria:
- `TestAsymmetricCollaboration` green (the v2-failing scenario).
- Policy count is exactly 4 per RLS table regardless of roles/groups,
  verified by `pg_policies` query.
- No remaining `pg_shdescription`, `COMMENT ON ROLE`,
  `serializePermissionSet`, or `clearJsonPermission` references in
  production code.
- REQ-1 closed in spec; REQ-2 resolved (closed or explicitly accepted).
- Direct-SQL count via COUNT-scoped role hits the privacy floor.

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
4. Inheritance semantics — `mg_owner` and `mg_groups` columns on inheriting
   tables: child inherits via PG inheritance.
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

## Out of scope

- Cross-schema custom roles (a role spans schemas). Roles are schema-local.
- User-defined privacy floors (currently hardcoded to 10 for RANGE).
- Audit logs for permission changes — can be added later as separate table.
- Migration tooling from PR #6058's data layout — out of scope.
