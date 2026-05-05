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

## Model (locked)

- **Role**: per-schema named permissionset. **Exactly one per user per schema**.
  Declares per-table per-verb scope ∈ `{NONE, OWN, GROUP, ALL}` plus
  `changeOwner` / `changeGroup` boolean flags. Stored as PG role
  `MG_ROLE_<schema>/<name>`. Permission scopes encoded in emitted policy DDL
  (no central metadata table).
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

**Storage** (decided): the JSON document attached to the PG role via
`COMMENT ON ROLE` is the canonical PermissionSet — full per-table per-verb
scope record, plus the changeOwner/changeGroup booleans (which are
double-recorded as column grants for SQL-layer enforcement). Java reads the
COMMENT for view-mode lookups; policies and grants are derived projections.
No metadata table.

### Custom role exclusivity (one per user per schema)

Enforced at grant time in Java (not in PG). Before `GRANT MG_ROLE_<schema>/<x>
TO MG_USER_<u>`, check `pg_auth_members` for any other `MG_ROLE_<schema>/*`
already granted to `<u>` and reject. System roles are excluded from this
check.

### System role coexistence

Owner, Manager, Editor, Viewer at session level have `BYPASSRLS` set on the
PG role (master behaviour, untouched). Result: when a user is acting through a
system role, all custom-role policies are skipped. When the same user acts
through a custom role only, policies apply.

If a user has both a system role and a custom role granted in a schema, PG
session attribution is by `SET ROLE` (the active role determines BYPASSRLS).
Default: `SET ROLE` to the most-permissive system role available for ergonomic
parity with master. Override available via session config if needed.

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

Phase 6 exits when 6.A–6.F are green and combined-suite is still zero
new failures (existing 5 graphql JWT failures stay deferred per user).

### Phase 7 — Tests, performance, hardening

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

### Phase 8 — Migration & docs

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
- **Membership storage split**: roles via PG GRANT (exclusive,
  `pg_auth_members`), groups via `MOLGENIS.groups_metadata` ref_array. Roles
  and groups are conceptually different; mixing both in `pg_auth_members`
  blurs the line.
- **No central role-metadata table**: scope encoded in policy DDL (Option d).
  Trades constant-policy-count goal for richer per-verb / per-role scope
  expressivity.
- **`changeOwner` / `changeGroup` flag storage**: PG ROLE COMMENT JSON.
  Lightweight; revisitable.
- **No base role inheritance** (Viewer / NONE): role explicitly lists
  per-table scopes. Few roles per schema, so verbosity is acceptable.
- **System roles untouched**: Owner / Manager / Editor / Viewer hold
  BYPASSRLS as in master. Custom-role mechanism is purely additive.
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
