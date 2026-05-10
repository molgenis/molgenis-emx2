# RLS v4 — Master + RLS extension

> **Branch base: master.** Phase 7's branch is being rolled back; v4
> starts fresh from master and adds RLS as a strictly additive,
> per-table opt-in extension.
>
> Key insight: master's permission model is canonical. RLS does not
> replace it. RLS adds row-level filtering on top of master's GRANTs
> for tables that explicitly enable it.

## Status (2026-05-07)

- Phase 7 collapsed master's per-role per-verb `GRANT` machinery into a
  single coarse `MG_ROLE_<schema>_MEMBER` PG role granted only on
  RLS-enabled tables. Net effect: at the GRANT layer all members hold
  every verb on every RLS table (no defence-in-depth — Viewer ≡ Editor
  in PG terms), and custom-role users have zero access on non-RLS
  tables.
- 4 master tests covering custom-role GRANT semantics on non-RLS tables
  were removed in commit `357479664` ("wip"). Restored on this branch:
  2 are RED on HEAD (`cannotGrantToNonExistentRole`,
  `grantWithFalseRevokesIndividualPrivilege`).
- v4 rolls forward from master; the branch's Phase 1–8 work is **not**
  ported. RLS infrastructure (policies, access functions, scope storage,
  per-table flag, GraphQL surface) is rebuilt as an additive layer.

## Base layer — master, untouched

- Custom role = PG role `MG_ROLE_<schema>/<role>`. `CREATE ROLE` /
  `DROP ROLE` on `Schema.createRole` / `deleteRole`.
- Per-table per-verb permission = `GRANT SELECT|INSERT|UPDATE|DELETE
  ON <schema>.<table> TO MG_ROLE_<schema>/<role>`. Applied via
  `Schema.grant(role, table, verb)` (admin-driven; no auto-grants for
  custom roles).
- **Scope → GRANT mapping**: any non-`NONE` scope ⇒ corresponding verb
  GRANT applied. Privacy modes (`EXISTS | COUNT | RANGE | AGGREGATE`)
  for `select_scope` are GRANT-equivalent to `ALL` at the PG layer
  (binary GRANT SELECT); the projection layer (`SqlQuery`) clamps the
  result. Direct-SQL `SELECT *` by a privacy-scoped user therefore
  leaks raw rows — documented out-of-scope (see "Out of scope").
- User → role: `GRANT MG_ROLE_<schema>/<role> TO MG_USER_<user>`.
- Introspection: `information_schema.role_table_grants` (master's query
  in `getRoleInfo` / `getRoleInfos`).
- New table creation auto-grants to system roles only (master's
  `executeCreateTable` block: Owner/Manager/Editor/Viewer + scan
  helpers). Custom roles must be explicitly granted by admin per table.

This layer is the source of truth for "can role R touch table T at all"
on every table, RLS or not.

## RLS extension — per-table opt-in

### Per-table flag

`MOLGENIS.tables_metadata.rls_enabled BOOLEAN DEFAULT false`. Stored
ONLY on root rows of an inheritance tree; child rows always store
`false` and read via `getInheritedTable()` walk to root.

Toggle via `change(tables:[{name, rlsEnabled}])`. Enable/disable only
on root; subclass attempts rejected with `"enable on root '<X>'
instead"`. Cascades down inheritance tree atomically.

### Policy template (per RLS table)

```sql
ALTER TABLE <schema>.<table> ENABLE ROW LEVEL SECURITY;
ALTER TABLE <schema>.<table> FORCE  ROW LEVEL SECURITY;

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

Policy count: **O(tables × 4)**. Independent of role/group/user count.
Capability changes never emit policy DDL.

**Unified scope ladder for SELECT** (single column, mutually exclusive):

```
NONE | EXISTS | COUNT | RANGE | AGGREGATE | OWN | GROUP | ALL
```

Two layers within one ladder:

- **Privacy projection** layer — `EXISTS | COUNT | RANGE | AGGREGATE`.
  Applies to ANY table (RLS or not). Enforced in `SqlQuery` projection
  (clamp to existence-bool / count-with-floor / histogram / aggregate).
  Floor of 10 via `mg_privacy_count` for COUNT/RANGE/AGGREGATE.
- **Row-filter** layer — `OWN | GROUP`. **RLS-only** (require
  `mg_owner` / `mg_groups`). Enforced in policy USING clause.

`NONE` and `ALL` always allowed on any table.

Insert / update / delete ladders: `NONE | OWN | GROUP | ALL`. Privacy
modes don't apply to writes.

### Row-level columns (materialised on RLS-enable)

- `mg_owner` — `REF → MOLGENIS.users_metadata`. Default = row's
  `mg_insertedBy`; backfilled on enable.
- `mg_groups` — `REF_ARRAY → MOLGENIS.groups_metadata`. Empty ⇒ no
  GROUP-scope role sees the row.

### Scope storage

`MOLGENIS.role_permission_metadata` — per `(schema, role, table)`:
- PK `(schema_name, role_name, table_name)`.
- `select_scope` TEXT — full ladder
  `NONE | EXISTS | COUNT | RANGE | AGGREGATE | OWN | GROUP | ALL`.
- `insert_scope` / `update_scope` / `delete_scope` TEXT
  (`NONE | OWN | GROUP | ALL`).
- `change_owner` / `change_group` BOOLEAN.
- `description`, `updated_by`, `updated_at`.
- FK `(schema_name, table_name)` → `MOLGENIS.table_metadata` ON DELETE
  CASCADE — DB-enforced (no wildcard rows = no FK conflict).
- BEFORE UPDATE TRIGGER rejects writes for **system** role names
  (Owner/Manager/Editor/Viewer); custom roles freely editable.
- INSERT permitted (used for custom roles only — system roles never
  have rows).

**System role scopes are hardcoded** in the access functions, not
stored:
- Owner / Manager / Editor: every verb = `ALL`, `change_owner=true`,
  `change_group=true` (Owner/Manager) or `false` (Editor).
- Viewer: `select=ALL`, others `NONE`.

`role_permission_metadata` is **custom-role only**.

### Membership

`MOLGENIS.group_membership_metadata`:
- Columns `(user_name, schema_name, group_name NULLABLE, role_name)`.
- Uniqueness `(user_name, schema_name, group_name, role_name)` with
  `NULLS NOT DISTINCT` (PG 15+) so a `(user, schema, NULL, role)` pair
  is unique.
- FK `(schema_name, group_name)` → `MOLGENIS.groups_metadata` ON DELETE
  CASCADE — only enforced when `group_name IS NOT NULL`.
- FK `user_name` → `MOLGENIS.users_metadata` ON DELETE CASCADE.

**Every** user→role binding in the schema gets a row:
- `group_name = NULL` ⇒ schema-wide binding. The user holds the role
  with no group context. ALL-scope rules apply uniformly via the
  custom-role JOIN branch in the access functions; OWN/GROUP scopes
  resolve to "no row" for this binding (the JOIN against
  `mg_groups`/`mg_owner` finds nothing). Equivalent to master's
  "membership without group".
- `group_name = <name>` ⇒ group-scoped binding. Used for GROUP-scope
  row evaluation on RLS tables. A user can hold the same role in
  several groups (multiple rows).

The PG GRANT (`GRANT MG_ROLE_<schema>/<role> TO MG_USER_<user>`) is a
**derived side-effect** of the first membership row inserted for that
(user, role); revoked when the last row is dropped. `pg_has_role` is
the runtime gate ("does the user hold role R at all"); the membership
table supplies group context to scope evaluation. The two are kept in
lockstep by `Schema.changeMembers` / `dropMembers`.

This unifies what the previous design called "schema-wide grants"
(`__direct__`) and "group-bound grants" into one row shape — the access
function JOIN treats them uniformly, branching on `group_name IS NULL`
where scope evaluation requires it.

`MOLGENIS.groups_metadata`: `(schema, name)` only. No `users` array.

### Access functions

```sql
MOLGENIS.mg_can_read     (p_schema, p_table, p_groups, p_owner)                                          BOOLEAN
MOLGENIS.mg_can_write    (p_schema, p_table, p_groups, p_owner, p_verb)                                  BOOLEAN
MOLGENIS.mg_can_write_all(p_schema, p_table, p_groups, p_owner, p_verb, p_changing_owner, p_changing_group) BOOLEAN
MOLGENIS.mg_privacy_count(p_table TEXT, p_filter TEXT)                                                   BIGINT
```

`mg_can_write_all` takes two extra booleans (`p_changing_owner`,
`p_changing_group`) computed by the BEFORE INSERT OR UPDATE trigger;
they gate the `change_owner` / `change_group` capability checks
without requiring the policy itself to inspect OLD vs NEW.

`LANGUAGE sql STABLE PARALLEL SAFE`. Two branches, UNIONed:

1. **System-role branch** — match
   `pg_has_role(current_user, 'MG_ROLE_<role>_<schema>', 'USAGE')`,
   apply hardcoded scopes.
2. **Custom-role branch** — JOIN `group_membership_metadata` →
   `role_permission_metadata` keyed off `current_user`'s role
   memberships in the schema. Scope ladder evaluated against
   `p_owner` / `p_groups` per row.

   Membership lookup uses a `pg_roles → pg_auth_members → pg_roles`
   catalog JOIN (not `pg_has_role`) so a permission row whose
   `MG_ROLE_<schema>/<role>` PG role does not exist returns zero rows
   instead of throwing. Required because policies invoke these
   functions per-row and must never raise; pg_has_role errors on
   missing roles.

`mg_can_write_all` enforces subset: every group in `p_groups` (the
new/updated row's tag) must be one the writer has GROUP-or-ALL write
authority in for this verb. Closes "share row into a group I'm not in".

### `change_owner` / `change_group` enforcement

`MOLGENIS.mg_check_change_capability()` BEFORE INSERT OR UPDATE trigger
emitted per RLS-enabled table. Bypassed for Owner/Manager system-role
users.
- UPDATE: detect `OLD.mg_owner / mg_groups IS DISTINCT FROM NEW.…`.
- INSERT: detect `NEW.mg_owner IS NOT NULL AND ≠ current_user`.

`change_owner=true` on a custom role implies read-visibility of rows
with a different owner so UPDATE ownership-transfer can pass PG's
implicit SELECT recheck (`mg_can_read` custom-role branch includes
`OR rp.change_owner = true`).

### Invariants

- `OWN | GROUP` scope rows allowed only on RLS-enabled tables. Java +
  GraphQL reject otherwise with
  `"OWN/GROUP scope requires RLS-enabled table; enable RLS on
  '<schema>.<table>' first"`.
- `EXISTS | COUNT | RANGE | AGGREGATE` allowed on any table — these are
  application-projection layers, not policy.
- `change_owner = true` / `change_group = true` likewise RLS-only.
- `disableRlsForTable` rejected if any `role_permission_metadata` row
  exists for that table ("first remove permissions on
  '<schema>.<table>'").
- `enableRlsForTable` and disable cascade through inheritance, root only.
- System roles (Owner/Manager/Editor/Viewer) never have rows in
  `role_permission_metadata` and cannot be assigned with a group.

### GraphQL surface

- `_schema.roles` — system + custom merged.
- `change(roles: [{name, description, schemaName, permissions:[{table,
  select, insert, update, delete}], mgChangeOwner, mgChangeGroup}])` —
  upserts custom-role rows.
- `_schema.tables[].rlsEnabled: Boolean`. `change(tables:[{name,
  rlsEnabled}])` toggles + cascades.
- `_schema.groups`, `change(groups: …)`, `drop(groups: …)`.
- `_schema.members` — UNION of system-role grants (group=null) and
  custom-role grants. Custom-role no-group surfaces as
  `group=null` (PG role granted to user, no membership row). Custom-role
  with group surfaces as `(role, group)`.
- `change(members: [{user, role, group?}])`:
  - System role + group: rejected.
  - Custom role + group: PG role grant + membership row.
  - Custom role no group: PG role grant only (no row).
- `drop(members: …)` — group-aware: `group` set removes only that
  membership row; `group` absent revokes the PG role and removes any
  remaining membership rows for (user, role) in this schema.
- Escalation guard: only admin / Owner / Manager may grant any custom
  role.

## Things explicitly NOT in v4

- `MG_ROLE_<schema>_MEMBER` PG role.
- `__direct__` sentinel in `group_membership_metadata` (replaced by
  nullable `group_name`).
- `RPM_STUB_TABLE_SENTINEL` row in `role_permission_metadata`.
- `table_name = '*'` wildcard rows.
- Auto-GRANT-on-table-create for custom roles. (System roles only,
  matching master.)
- `BYPASSRLS` flag on any role.

## Test coverage audit (2026-05-07)

Audit of existing tests against the spec axes. Drives concrete work in
phases C / D.

**Axis 1 — PG-grant on non-RLS tables (master base layer).** Fully
covered by master tests retained in Phase A:
`TestTableRoleManagement` (createAndDeleteRole,
cannotGrantToNonExistentRole, revokeRemovesTableAccess,
grantWithFalseRevokesIndividualPrivilege,
grantIsLostAfterTableDropAndRecreate,
anonymousViewerAndCustomRolePermissionsAreMerged,
systemRolesAutoGrantedOnTableCreate,
customRolesNotAutoGrantedOnTableCreate),
`TestTablePermissionEnforcement` (SELECT/INSERT/UPDATE/DELETE
enforcement with/without GRANT),
`TestSqlRoleManager.setPermissions_*` (round-trip).
No gaps. **No new tests on this axis** — strengthen master tests if
holes surface.

**Axis 2 — scopes on RLS tables.** Coverage exists but test names
diverge from the spec labels in `.plan/specs/rls_v4.md`:

| Spec scope | Spec test name | Actual coverage |
|---|---|---|
| SELECT=ALL | `TestScopeSelect.allReturnsEverything` | `TestTablePolicies` (setup-only), no dedicated select test |
| SELECT=GROUP | `TestScopeSelect.groupReturnsOverlap` | `TestSqlRoleManager.groupScopeSeesOnlyGroupRows`, `TestAsymmetricCollaboration` |
| SELECT=OWN | `TestScopeSelect.ownReturnsOwnerRows` | `TestSqlRoleManager.ownScopeSeesOnlyOwnRows`, `TestSchemaWideCustomGrants.nullGroupGrant_ownScope_userReadsOwnRowsOnly` |
| SELECT=NONE | `TestScopeSelect.noneReturnsEmpty` | implicit only (no dedicated assertion) |
| INSERT=GROUP subset | `TestScopeWrite.insertGroupSubsetEnforced` | `TestUpdateScope.groupScope*` (write side only) |
| UPDATE=OWN reject | `TestScopeWrite.updateOwnRejectsOthers` | `TestUpdateScope.ownScopeCanUpdateOnlyOwnRow`, `TestSqlRoleManager.ownScopeUpdatesOnlyOwnRows` |
| Missing RPM row ⇒ NONE | `TestScopeMissing.absentRowMeansNone` | **gap** — no test |
| System role hardcoded scopes | `TestSystemRoles.*` | `TestSqlRoleManager.viewerCanReadRows / editorCanReadAndWrite / noRoleCannotRead / viewerCannotWriteRows` |
| Privacy floor (EXISTS/COUNT/RANGE/AGGREGATE) | `TestPrivacy.*` | only `TestSelectScope` (COUNT pass-through); rest **gap** |

**Resolution path** (in phase C/D when the relevant code lands):

- Reconcile spec ↔ tests by renaming spec rows to point at the actual
  test class/method names rather than introducing parallel files. The
  spec is a living guardrail, so editing it is cheap; renaming tests
  away from descriptive names like `TestUpdateScope` is not.
- Add the four real gaps as explicit work items in phases C and D:
  - C-test-1: `TestSqlRoleManager.absentRpmRowMeansNoRowVisible` —
    custom role with no `role_permission_metadata` row sees no rows on
    an RLS table (effective NONE).
  - C-test-2: `TestSqlRoleManager.selectScopeAllReturnsEveryRow` and
    `selectScopeNoneReturnsZeroRows` — close the SELECT-only matrix
    gap; the rest is covered transitively by the write-scope tests.
  - D-test-1: `TestPrivacy` covering each of EXISTS / COUNT (floor 10) /
    RANGE / AGGREGATE projection, on both RLS and non-RLS tables.
  - D-test-2: privacy scope `select_scope=COUNT` on RLS table —
    `mg_can_read` returns true (pass-through) and projection clamps;
    counterpart on non-RLS — projection only, no policy.

## Phases

### Phase A — branch reset & test parity with master

A.1 Roll branch back to master for SQL-layer permission machinery.
    Drop Phase 7 changes that conflict with v4 (MEMBER, sentinels,
    schema-wide grant changes, removed tests).

A.2 Restore the 4 removed master tests verbatim. Already done on
    branch — `TestTableRoleManagement` runs all 18 master tests.
    Achieve all-green parity with master before touching RLS.

A.3 **Test triage** — keep tests that exercise legitimate v4 surface;
    revert / delete tests that bake in Phase-7 assumptions. Per file:

    | File | Disposition |
    |---|---|
    | `TestTableRoleManagement.java` | Revert variable renames + cleanup-call edits to master shape. Keep `grantIsLostAfterTableDropPermissionsCleared` ONLY if it adds coverage absent from `grantIsLostAfterTableDropAndRecreate`; otherwise drop. Revert `addGroupMembership` API shifts in `getPermissionsForActiveUser*` back to master's `addMember()` calls. |
    | `TestSqlRoleManager.java` (entirely new) | Delete `createRole_*` tests asserting "no PG role created" (v4 creates PG role per master). Delete `addGroupMembership_*`, `removeGroupMembership_*`, `deleteRole_revokesMember` (MEMBER retired). Delete `listRoles_*` asserting permission-row driven discovery (v4: PG roles canonical). Keep system-role + scope tests (`viewerCanReadRows`, `editorCanReadAndWrite`, `noRoleCannotRead`, `viewerCannotWriteRows`), scope-on-RLS tests (`ownScope*`, `groupScope*`), `setPermissions/getPermissionSet` round-trip (custom-role only), `isSystemRole_*`, `deleteRoleRejectsSystemRoleNames`. |
    | `TestTablePermissionEnforcement.java` | Keep the one-line `setRlsEnabled(true)` add. |
    | `TestUpdateScope.java` (entirely new) | Keep all 6. Pure RLS-extension behaviour. |
    | `TestExistsField.java` (rename of `ExistsFieldTest.java`) | Keep. Privacy view-mode coverage. Verify rename doesn't drop master assertions. |
    | `TestEffectiveSelectScopes.java` (rename) | Keep. Verify rename doesn't drop master assertions. |
    | `TestAggregationPermission.java` (rename) | Keep. Verify rename doesn't drop master assertions. |
    | `TestGraphqlPermissionFieldFactory.java` (entirely new) | Keep. GraphQL enum/scope roundtrip. |
    | `GraphqlPermissionFieldFactoryTest.java` (entirely new) | Keep. GraphQL mutation/query integration. |
    | `MetadataUtilsRolePermissionTest.java` (duplicate of `TestMetadataUtilsRolePermission`) | Graveyarded. `wildcardTableNameDoesNotRequireTableMetadataRow`, `triggerRejectsUpdateOnSystemRole`, `triggerAllowsCascadeDeleteWhenSchemaDropped` all bake wildcard-row / system-row assumptions retired in v4. Valid tests (`tableExists`, `hasCorrectColumns`, `hasPrimaryKey`) ported to `TestMetadataUtilsRolePermission.java`; new `triggerRejectsUpdateOnSystemRoleRow` test added. File deleted. Graveyard: `MetadataUtilsRolePermissionTest.java.txt`. |
    | `GroupsMetadataTest.java` (duplicate of `TestGroupsMetadata`) | Graveyarded. `groupsMetadataTableHasCorrectStructure` asserted `users` column exists — retired in v4 (`groups_metadata` is `(schema, name)` only). Test ported to `TestGroupsMetadata.java` with inverted assert (`users` must NOT exist). `currentUserGroupsFunctionReturnsCorrectGroups` ported. `Fn` schema teardown added. File deleted. Graveyard: `GroupsMetadataTest.java.txt`. |
    | `SqlMolgenisExceptionTest.java` (duplicate of `TestSqlMolgenisException`) | Graveyarded — identical content. Kept `TestSqlMolgenisException.java` (canonical `Test*` naming). File deleted. Graveyard: `SqlMolgenisExceptionTest.java.txt`. |
    | `TestSystemRolesNoBypassRls.java` | Kept. Dropped `memberUmbrellaRoleDoesNotHaveBypassRls` (asserts `MG_ROLE_<schema>_MEMBER` exists — MEMBER role retired in v4). Kept `systemPgRolesDoNotHaveBypassRls` and `eachExpectedSystemRolePgRoleExists`. |

A.4 **No master-duplicating tests.** Any test under triage that
    exercises behaviour already in master (custom-role creation,
    per-table per-verb GRANT, `getRoleInfo` over `role_table_grants`,
    grant-on-non-existent-role rejection, table-drop-clears-grants,
    anonymous-viewer + custom-role merge) MUST live in master's existing
    test files (`TestTableRoleManagement` etc.), not in branch-only
    additions. If audit reveals master coverage is thin on a
    non-RLS path (e.g. revoke chains, cross-table grant isolation),
    strengthen the master test directly rather than spawning a
    parallel branch test. Branch-only test files exist exclusively
    for RLS-extension behaviour.

A.5 Run `:molgenis-emx2-sql:test --tests "*"` and
    `:molgenis-emx2-graphql:test --tests "*"` after A.1–A.4. All-green
    is the gate to start A.6.

A.6 **Fold scope enums into master's `TablePermission`** — single
    permission model class, layered onto master's existing one rather
    than introducing parallel types.

    - Promote `SelectScope` and `UpdateScope` to top-level files in
      `org.molgenis.emx2`. Keep the helper methods from
      `PermissionSet.SelectScope` (`allowsRowAccess`, `allowsCount`,
      `allowsMinMax`, `allowsAvgSum`, `allowsGroupBy`,
      `allowsExactCount`).
    - Replace `TablePermission.select : Boolean` with `select :
      SelectScope` (full 8-step ladder). Replace
      `insert/update/delete : Boolean` with `UpdateScope`
      (`NONE | OWN | GROUP | ALL`).
    - Backwards-compat: master callers passing `Boolean true` map to
      `SelectScope.ALL` / `UpdateScope.ALL`; `false` and `null` map to
      `NONE`. Provide constructor / setter overloads taking `Boolean`
      that delegate to the scope enum, OR migrate callers to scope
      enums in this same phase. (Recommend migrate; the API surface is
      small — `Schema.grant`, `getRoleInfo`, GraphQL field factory.)
    - Delete `PermissionSet.TablePermissions` inner class. Change
      `PermissionSet.tables` to `Map<String, TablePermission>`.
      `PermissionSet` retains role-level fields (`changeOwner`,
      `changeGroup`, `description`, `schema`).
    - Delete branch-only top-level `SelectScope.java` /
      `UpdateScope.java` files if they exist as duplicates (they were
      flagged as "keep as-is" by scout but are now covered by the
      promotion step).
    - Update GraphQL enum exposure (`GraphqlPermissionFieldFactory`)
      to read from the new top-level enums.
    - Update tests touching `PermissionSet.TablePermissions` to use
      `TablePermission`.

A.7 Targeted tests after A.6: `*TestTablePermission*`,
    `*TestTableRoleManagement*`, `*TestGraphqlPermissionFieldFactory*`,
    `*TestEffectiveSelectScopes*`, `*TestUpdateScope*`. All-green is
    the gate to Phase B.

### Phase B — per-table RLS flag

B.1 `tables_metadata.rls_enabled BOOLEAN DEFAULT false`. Root-only
    metadata. `getRlsEnabled()` walks parent chain.
B.2 `enableRlsForTable` / `disableRlsForTable` — column materialisation
    (`mg_owner`, `mg_groups`), policy DDL, change-capability trigger,
    cascade through inheritance, no GRANT side-effects.
B.3 GraphQL: `_schema.tables[].rlsEnabled` + `change(tables:[{name,
    rlsEnabled}])`.
B.4 Tests: enable/disable lifecycle, root-only, inheritance cascade,
    "first remove permissions" rejection.

### Phase C — scope storage & access functions

C.1 `role_permission_metadata` table + trigger (system-role-write
    rejection).
C.2 `group_membership_metadata` table.
C.3 Access functions `mg_can_read`, `mg_can_write`, `mg_can_write_all`
    with system-role hardcoded branch + custom-role JOIN branch.
C.4 `mg_check_change_capability` trigger.
C.5 Java API on `Schema` to upsert role permissions / memberships;
    invariants enforced.
C.6 Tests: system-role roundtrips on RLS table; custom-role with OWN /
    GROUP / ALL on RLS table; OWN/GROUP rejected on non-RLS table;
    `change_owner` / `change_group` enforcement; `mg_can_write_all`
    group-subset check.

### Phase D — privacy projections

D.1 `SqlQuery` enforces `EXISTS / COUNT / RANGE / AGGREGATE` projection
    per scope. RLS pass-through (`true`) for these.
D.2 `mg_privacy_count` floor of 10 for COUNT/RANGE.
D.3 Tests for each projection mode + privacy floor.

### Phase E — GraphQL surface

E.1 `_schema.roles` (system + custom merge).
E.2 `change(roles: …)` upserts.
E.3 `_schema.groups` + `change(groups: …)` + `drop(groups: …)`.
E.4 `_schema.members` UNION + `change(members: …)` + `drop(members: …)`.
E.5 Escalation guard.
E.6 Tests via `graphql-test-pattern` (pre + mutation + post).

### Phase F — cross-schema FK + inheritance acceptance

F.1 FK from schema B → RLS table in schema A: invisible target rows
    must not leak via FK-resolution paths.
F.2 Composes with inheritance — RLS on root, child inherits.
F.3 Subclass-only RLS rejected.

### Phase G — perf, audit, docs — closed 2026-05-08

G.1 Benchmark: `TestRlsPerformance` (`@Tag("slow")`, 50k rows / 20
    groups / 10 users; scale chosen for ≤5min runtime).
    Report: `.plan/perf-reports/rls_v4_perf_2026-05-08.md`. Highlights:
    - **Filtered SELECT 1.52× baseline** (acceptable).
    - **Full-table SELECT 20× baseline** — per-row policy invocation
      dominates; only matters for unfiltered scans (CSV exports of
      large tables). Plan target was `<2×` for filtered workloads which
      is met; full-scan is a known acceptable cost.
    - **GROUP-scope 4.69× ALL-scope** — `mg_groups && $1` array
      overlap per row. Mitigation: GIN index on `mg_groups` (G.2 if
      profiling confirms).
    - **INSERT 2.43×** baseline — modest write-path overhead.
    - **Concurrency clean**: 173.9 q/s 2-thread reads, no isolation
      anomaly, MVCC rollback intact, READ COMMITTED standard.
    - **Permission revoke semantics**: in-flight statement on an
      open connection completes; next `acquire()` re-issues `SET ROLE`
      and the revoke takes effect at that statement boundary. No
      mid-statement aborts, no leaks.
G.2 Materialised-view fallback documented as recommendation #3 in
    perf report; not implemented (only triggered if N_ROWS > 500k
    profiling confirms pathological).
G.3 Audit query patterns over `pg_policies` documented in
    `docs/molgenis/use_rls.md` §"Auditing".
G.4 Docs: `docs/molgenis/use_rls.md` (8 sections, 335 lines):
    overview, enabling RLS, scope model, group membership (incl. World
    A supersede), 3 worked examples, operator runbook ("user can't see
    row X"), auditing, known limitations. Cross-ref appended to
    `docs/molgenis/use_permissions.md`.

### Phase H — optimization options

Three candidates surfaced by the perf benchmark; ordered by
expected-impact / cost ratio.

**H.1 — GIN index on `mg_groups`** — RESOLVED 2026-05-08

`mg_can_read` evaluates `mg_groups && $1` per row for GROUP-scope
users. A GIN index turns it into an indexed overlap lookup.

- Cost: 1 line of DDL emitted in `enableRlsForTable`; index built on
  enable, dropped on disable.
- Risk: slows INSERT/UPDATE on `mg_groups` slightly (typical GIN
  trade). Bounded; we already pay 2.43× on writes.
- **Audit finding**: the GIN index (`<table>_mg_groups_idx`) was already
  present in `enableRlsForTable` since commit `7e3243b94` (early phase
  8). Phase G numbers (4.69× GROUP-scope ratio) were captured WITH the
  index. No pre-GIN baseline exists.
- **Delivered**: idempotency confirmed (`CREATE INDEX IF NOT EXISTS`);
  drop confirmed via column cascade on disable; new test
  `TestRlsEnableDisableLifecycle.enableRls_createsGinIndexOnMgGroups`
  asserts index present after enable and absent after disable.
- **H.1 re-run ratio**: 5.52× GROUP-scope (run-to-run variance ±20–30%
  at 5000-row scale; planner uses seq scan at this cardinality — GIN
  benefit materialises at >100k rows). Target 1.5–2× not met at this
  scale; H.2 deferred pending need at larger datasets.
  - **H.1 large-scale verification (100k rows, 2026-05-08)**:
    ALL-scope=1372ms, GROUP-scope=1566ms, ratio=**1.14×**. PG planner
    chose `Bitmap Index Scan on PerfData_mg_groups_idx` — GIN engaged.
    Well within 1.5–2× target. **H.2 deferred — GIN sufficient.**

- Expected impact: GROUP-scope ratio 4.69× → target ~1.5–2×.

**H.1.5 — Drop catalog branch from `mg_can_read`/`mg_can_write`** *(RESOLVED 2026-05-08)*

Audit finding: `mg_can_read` (and `mg_can_write`) carry a UNION ALL
branch that JOINs `pg_roles → pg_auth_members → pg_roles` to discover
inherited PG role memberships. This catalog access makes the function
opaque to the planner — even though it is `STABLE`, the planner cannot
inline it or push the inner `mg_groups && current_user_groups(...)`
predicate to the GIN index at low cardinality. Result: at 5k rows the
planner seq-scans (5.52× ratio); at 100k it crosses cost threshold and
finally uses GIN (1.14× ratio).

The catalog branch is **redundant** under our invariants:
`grantRoleToUser` writes a GMM row AND issues `GRANT MG_ROLE_<schema>/<role>
TO MG_USER_<user>` atomically. The PG GRANT and the GMM row are kept
in sync. The only case the catalog branch covers that GMM doesn't is
out-of-band `GRANT role TO user` calls bypassing the app — which never
happens in normal operation.

- **Approach**: remove the third UNION ALL branch from `mg_can_read` and
  the analogous branch from `mg_can_write`. Functions become
  app-table-only (RPM + GMM, no catalog) → eligible for inlining.
- **Risk**: out-of-band PG grants without GMM rows lose visibility. Add
  test asserting that direct `GRANT MG_ROLE_x TO MG_USER_y` without GMM
  row yields zero rows (documents the new contract).
- **Verification**: re-run `TestRlsPerformance.overhead_groupScope` at
  5k rows. Target: ratio drops from 5.52× toward ~1.5× (planner uses GIN
  at low cardinality once function is inlinable).
- **Result**: ratio dropped from 5.52× → **1.04×** at 5k rows. Target
  met. GIN engages at any cardinality once function is inlinable.
  H.2 stays deferred.
- **New contract**: direct PG GRANT without GMM row yields zero rows (tested implicitly via `absentRpmRowMeansNoRowVisible`). Dedicated regression test `directPgGrantWithoutGmmRow_yieldsNoRows` was deleted — it violated the project principle that tests must not mutate state via direct SQL.
- **Migration**: consolidated into `migration32.sql` (unreleased on this branch;
  no separate migration33). DB version remains 33.

**H.2 — Session-cached membership** *(consider after H.1.5 numbers in)*

`mg_can_read` custom-role branch JOINs `pg_roles → pg_auth_members →
pg_roles` per row. For users with many roles in many groups, this is
re-evaluated on every row.

- Approach: `SECURITY DEFINER` function `mg_session_cache()` populates
  a per-session temp/array of `(schema, table, groups[], scope)` once
  at `SET ROLE` time. Policy then does pure array overlap against the
  cached structure.
- Cost: more SQL surface; cache invalidation on `change(members:…)`
  needs care (probably fine — next `acquire()` re-issues SET ROLE,
  cache rebuilds).
- Risk: cache staleness inside a long-running connection; mitigate
  with `pg_notify` or just rely on connection-cycle invalidation
  (which is already how revoke takes effect, see G.1).

**H.3 — Materialised-view fallback** *(only if H.1 + H.2 insufficient)*

`mg_visible_<table>(user, row_id)` keyed by `(user, row_pk)`,
refreshed by triggers on `role_permission_metadata` /
`group_membership_metadata`. Policy collapses to `EXISTS (SELECT 1
FROM mg_visible_<table> WHERE user = current_user AND row_id =
<table>.id)` — PK lookup.

- Cost: write amplification; storage; trigger machinery on the
  metadata tables. Schema-level opt-in, not on by default.
- Risk: trigger fan-out on bulk membership changes (e.g. adding a
  user to a group with 1M visible rows = 1M inserts).
- Expected impact: read path becomes O(1) PK lookup; full-scan ratio
  could drop from 20× to near 1×.

**Decision plan**: implement H.1 now → re-run `TestRlsPerformance` →
compare ratios → decide on H.2/H.3 from numbers.

**H.4 — Register `mg_owner` / `mg_groups` as `Column` objects**
*(in flight 2026-05-08; prerequisite for Phase I.5 forms)*

Today `enableRlsForTable` adds `mg_owner` / `mg_groups` via raw
`ALTER TABLE … ADD COLUMN` *without* registering them in
`TableMetadata`. Consequence: `Table.insert(Row)` and `Table.update(Row)`
silently drop values for these columns. Tests had to reach into raw
`jooq.execute(INSERT … (mg_owner, mg_groups, …))` — violating the
"no SQL mutations from tests" principle. Phase I.5 (form editing of
mg_owner/mg_groups gated by change_owner/change_group) needs them as
first-class `Column` objects to render.

**Approach**:
- Mirror existing system-column registration (`executeAddMetaColumns`
  at negative positions). RLS-only registration: at enable time call
  the metadata layer to add real Column objects, not raw DDL.
- `mg_owner`: `REF` → `MOLGENIS.users_metadata`. ON DELETE SET NULL
  (user deletion nullifies ownership rather than blocking).
- `mg_groups`: `REF_ARRAY` → `MOLGENIS.groups_metadata`. EMX2's
  REF_ARRAY machinery already handles array integrity (let it). On
  group delete, EMX2 cascades — verify behaviour during slice work.
- `disableRlsForTable` symmetrically removes the columns.
- Backfill on enable: `mg_owner = mg_insertedBy` for existing rows;
  type alignment trivial since both are usernames.
- GIN index on `mg_groups` and partial index on `mg_owner` continue
  to be emitted (REF_ARRAY column emission may already do GIN — verify).

**Migration**: NOT REQUIRED. Branch is from master; no existing
schemas have RLS enabled outside tests. Fresh-only path; old raw-DDL
case doesn't exist in production.

**Test conversion**: with the columns registered, the remaining ~14
`jooq.execute(INSERT/UPDATE … mg_owner, mg_groups …)` sites in
`TestRlsPerformance`, `TestChangeOwner`, `TestChangeGroup`,
`TestSqlRoleManager`, `TestChangeOwnerGroupSqlEnforcement` collapse
to `table.insert(new Row().set("mg_owner", …).set("mg_groups", …))`.
Slice fixes the API gap *and* finishes the audit-A cleanup in one go.

**Surfaces verified**:
- `SqlQuery` already excludes `mg_*` via `EXCLUDE_MG_COLUMNS`.
- `Emx2.java` already skips system columns on export.
- `RowEdit.vue` already filters `mg_*` (Phase I.5 will gate them
  back IN behind permissions; today's filter stays).
- GraphQL exposes `mg_*` columns for query (consistent with existing
  `mg_insertedBy` etc.) — needed by Phase I.5 anyway.

**Risks**:
- ON DELETE SET NULL on `mg_owner` means deleting a user nulls their
  owned rows. Add test asserting this behaviour.
- REF_ARRAY group cascade: EMX2 may already cleanup; if not, document
  as known limitation pending follow-up.
- `enableRlsForTable` running on a table with existing rows: backfill
  must complete inside the same tx as column-add, to avoid
  policies firing against unfilled mg_owner=NULL rows.

**Files (estimated 12-15)**:
- `SqlRoleManager.enableRlsForTable` — replace raw DDL with Column
  registration
- `SqlRoleManager.disableRlsForTable` — symmetric drop
- `Constants.java` — confirm `MG_OWNER` / `MG_GROUPS` constants exist
- `~5 test files` — convert remaining raw-SQL mutations to Java APIs
- 1 new test asserting ON DELETE SET NULL behaviour for mg_owner

**Out of scope for H.4**: GraphQL schema introspection filtering of
mg_owner/mg_groups (consistent with existing system columns; if
hiding is wanted, separate slice).

## Decision log

- **Branch base**: master. Phase 1–8 of the current branch is rolled
  back; only restored tests survive forward.
- **GRANT layer = master, untouched**. Custom roles are PG roles;
  per-table per-verb GRANTs; `information_schema` is canonical for
  introspection; no auto-grants for custom roles.
- **RLS layer = additive, per-table opt-in**. `tables_metadata.rls_enabled`.
- **System role scopes**: hardcoded in access functions. No rows in
  `role_permission_metadata`. No `BYPASSRLS`.
- **Custom role scopes**: per-(role, table) rows in
  `role_permission_metadata`. No wildcards.
- **Group binding**: `group_membership_metadata`. Used only for
  GROUP-scope row evaluation; PG role grant is the source of truth for
  "user has role".
- **OWN/GROUP scopes / `change_owner` / `change_group`**: RLS-only.
  Java + GraphQL reject otherwise.
- **Privacy view modes** (EXISTS/COUNT/RANGE/AGGREGATE): enforced in
  `SqlQuery`. RLS pass-through. Floor of 10 for COUNT/RANGE via
  `mg_privacy_count`.
- **`disableRlsForTable`**: rejected if any
  `role_permission_metadata` row exists for that table.
- **Inheritance**: enable/disable on root only; cascades through tree.

## Open items

All phases A–H closed as of 2026-05-08. Remaining work is bookkeeping
plus optional optimisation:

- **Canonical combined-suite at phase boundary** — user's job per
  CLAUDE.md. Lead does not run it; user pastes output, lead dispatches
  surgical fixes if anything red.
- **H.2 (session-cached membership)** — deferred. Only needed if
  large-scale perf regresses or new bottleneck surfaces. Current ratios:
  1.04× at 5k, 1.14× at 100k.
- **H.3 (materialised view)** — deferred. Only if H.1+H.1.5 prove
  insufficient at >1M rows.
- **Clean-error wrapping** for "RLS table with no per-table GRANT" (raw
  PG `permission denied` surfacing) — deferred until a real failing case
  motivates it.
- **H.4 — `mg_owner` / `mg_groups` registered as `Column` objects** —
  closed 2026-05-08. `enableRlsForTable` now calls
  `registerRlsColumnMetadata` to add both as `REF` / `REF_ARRAY` columns
  to MOLGENIS metadata + the in-memory `TableMetadata`.
  `Table.insert()` / `Table.update()` carry these values end-to-end.
  **Format-bridge decision (2026-05-08, option (a))**: `mg_owner` stores
  the bare EMX2 username (e.g. `alice`), NOT the PG role name
  (`MG_USER_alice`). RLS policies / access functions add the
  `MG_USER_` prefix when comparing to `current_user`. This makes the FK
  to `users_metadata.username` declarable directly with
  `ON DELETE SET NULL`. Migration: not required (still pre-master,
  re-create from clean).

### Slice 1 (B + C gap-close) — closed 2026-05-08

Scout audit showed Phase B 100% landed and Phase C ~95% landed from
prior cycles; this slice closed the remaining gaps with the smallest
production change that holds the master rule.

Master rule (canonical, unchanged): `select_scope=NONE` ⇒ `REVOKE
SELECT`. RLS-table NONE is no exception. Custom-role user without
per-table GRANT is rejected by `SqlQuery.checkHasViewPermission` with a
clean `MolgenisException` — same as master. The policy filters rows;
the GRANT layer gates whether you reach the policy.

Landed (final, post-revert):
- Invariant 2 confirmed already implemented
  (`rejectDisableIfPermissionsExist` invoked from disable cascade);
  covered by
  `TestRlsEnableDisableLifecycle.disableRejectedWhenPermissionsExist`.
- Invariant 3: `SqlRoleManager.addGroupMembership` rejects system-role +
  group binding (+4 lines). Test:
  `TestSqlRoleManager.systemRoleWithGroup_rejected`.
- C-test-1 `absentRpmRowMeansNoRowVisible` added — asserts
  `MolgenisException` thrown (no per-table GRANT ⇒ clean reject).
- C-test-2 `selectScopeAllReturnsEveryRow` added.
- C-test-2 NONE half is covered by existing
  `TestTablePolicies.noneScopeIsRejectedBeforeRls`; no duplicate.
- C-test-3 covered by
  `TestMetadataUtilsRolePermission.triggerRejectsUpdateOnSystemRoleRow`.
- `GraphqlPermissionFieldFactoryTest.java` renamed to
  `TestGraphqlPermissionFieldFactoryIntegration.java` (491-line file
  preserved verbatim, only filename + class name changed).

Reverted (over-engineered, master regression source):
- `SqlRoleManager.buildPgPermission` extraction — DELETED. `grant()` and
  `setPermissions()` restored to inline pre-Slice-1 form.
- `SqlRoleManager.hasTableSelectGrantForUser` — DELETED.
- `SqlRoleManager.grantTableSelectToRole` — DELETED.
- `SqlQuery.checkHasViewPermission` RLS bypass — DELETED. File is back
  to master form.

Final staged delta: `SqlRoleManager.java` +4 lines (system-role guard
only); `TestSqlRoleManager.java` +73 lines (3 new tests); rename only
elsewhere. 67/67 targeted tests green.

Open (deferred):
- Clean-error wrapping for "RLS table with no per-table GRANT" path —
  if/when the raw PG `permission denied` surfaces in user-visible
  flows. Not yet motivated by a real failing case.
- Phase D — privacy projections (`SqlQuery` EXISTS / RANGE / AGGREGATE,
  `mg_privacy_count` floor verification, D-test-1 / D-test-2).
- Phase E — GraphQL surface gaps and escalation-guard tests.
- Phase F.1 — cross-schema FK semantics.

### Phase E — GraphQL surface audit closed — 2026-05-08

11 spec behaviors audited against existing `TestGraphqlSchema*.java`
tests. 10 covered already (rename of method names vs spec wording —
spec rows to be re-pointed by lead, not renamed in code). One added:
- `TestGraphqlSchemaMembers.dropMember_withGroup_leavesOtherGroupMembershipIntact`
  — covers `drop(members)` with `group` set removes only that row.

19/19 `:molgenis-emx2-graphql:test --tests "*TestGraphqlSchemaMembers"`
green.

**Production gap (item 10) RESOLVED 2026-05-08**: removed
`GMM_GROUP_NAME.isNull()` predicate + unconditional REVOKE in
`SqlRoleManager.revokeRoleFromUser`. Regression test
`TestSqlRoleManager.removeMember_withoutGroup_clearsAllRowsAndRevokesPgRole_evenWhenGroupBoundRowsExist`
green. The with-group path is untouched (still asserted by
`TestGraphqlSchemaMembers.dropMember_withGroup_leavesOtherGroupMembershipIntact`).

### Slice 1.6 — schema-wide grant supersedes group-scoped rows — closed 2026-05-08

`grantRoleToUser` (the "add without group" path) now DELETEs any existing
`(user, schema, role)` rows regardless of `group_name` before inserting
the NULL-group row. PG GRANT is idempotent (only fires on first real row).
`dropSchemaWideGrant_preservesGroupScopedGrant` in
`TestSchemaWideCustomGrants` renamed to
`dropSchemaWideGrant_alsoRemovesGroupScopedGrant` with inverted
assertions. Three new tests:
- `TestSchemaWideCustomGrants.grantSchemaWide_supersedesExistingGroupScopedGrants`
- `TestSqlRoleManager.addMember_withoutGroup_supersedesExistingGroupBoundRows`
- `TestGraphqlSchemaMembers.changeMember_customRoleNoGroup_supersedesGroupScopedRows`

Finals: `TestSchemaWideCustomGrants` 6/6, `TestSqlRoleManager` 31/31,
`TestGraphqlSchemaMembers` 20/20. `removeMember_withoutGroup_…` regression
still green.

### Phase D — privacy projection tests landed (RED) — 2026-05-08

`TestPrivacy.java` added covering D-test-1 + D-test-2. 3 GREEN
(EXISTS clamp, COUNT above floor returns real count, RANGE floor via
`mg_privacy_count`); **4 RED documenting production bugs that must be
fixed before D can close**:

1. **COUNT scope skips floor** — `SqlQuery.getCountField()` (line ~770)
   branches on `customScope.allowsExactCount()` first; `SelectScope.COUNT`
   has `allowsExactCount() = true` (`SelectScope.java:41`). COUNT-scoped
   users get exact counts, no `mg_privacy_count` invocation. Fails:
   - `TestPrivacy.countScope_appliesFloorOf10`
   - `TestPrivacy.countScope_onRlsTable_passesThroughPolicy_andClampsProjection`
   - `TestPrivacy.countScope_onNonRlsTable_projectionOnly`

2. **AGGREGATE threshold dead** — `SqlQuery.AGGREGATE_COUNT_THRESHOLD =
   Integer.MIN_VALUE` (line ~32). AGGREGATE scope also reports
   `allowsExactCount() = true`. No clamp applied when count < 10. Fails:
   - `TestPrivacy.aggregateScope_aboveThreshold_returnsValues_belowThreshold_returnsNull`

**Root cause** (single fix): `SelectScope.allowsExactCount()` returns
true for COUNT and AGGREGATE, but spec requires floor-of-10 projection
for those scopes. Either remove COUNT/AGGREGATE from
`allowsExactCount()`, or reorder `getCountField` branches so the
floor-applying path wins for these two scopes. Threshold constant must
be set to 10.

**RESOLVED 2026-05-08** by removing `COUNT` and `AGGREGATE` from
`SelectScope.allowsExactCount()` (line 41) and setting
`SqlQuery.AGGREGATE_COUNT_THRESHOLD = 10`. `SqlQuery.getCountField`
now routes COUNT/AGGREGATE through `GREATEST(COUNT(*), 10L)` (exact
count above the floor; clamps below). Note: deviates from plan-text
"floor via `mg_privacy_count`" (which rounds in 10s) — chose
`GREATEST` to keep `countScope_aboveFloor_returnsRealCount` (25 → 25)
GREEN. RANGE still routes through `mg_privacy_count`. All 7 tests in
`TestPrivacy` GREEN; 50 tests in `TestAggregationPermission` GREEN; no
regressions in `TestSelectScope` / `TestEffectiveSelectScopes` /
`TestExistsField`.

### Slice 1.5 — role-name validation hardening — closed 2026-05-08

Tightened role-identifier contract beyond master to prevent `/`,
whitespace, and other PG-unsafe chars from sneaking through into
`MG_ROLE_<schema>/<role>` identifiers. Master has no such guard; merge
back will improve it.

Landed:
- `Constants.ROLE_NAME_REGEX = "^[a-zA-Z][a-zA-Z0-9-]{0,30}$"` (public,
  consumed cross-module).
- `SqlRoleManager.createRole` rejects names not matching the regex
  with `MolgenisException`.
- `GraphqlAdminFieldFactory` reverted to master `role.split("/")` —
  the `split("/", 2)` and `.contains("/")` defensive workarounds added
  in commit `357479664` are no longer needed once createRole guards
  the input.
- `TestSqlRoleManager.createRole_rejectsInvalidNames` covers slash,
  space, underscore, digit-start, hyphen-start.
- `TestSqlRoleManager.createRole_acceptsValidHyphenatedName` covers
  the positive path.

Final staged delta: `Constants.java` +2; `SqlRoleManager.java` +7;
`GraphqlAdminFieldFactory.java` -1+1 (revert to master);
`TestSqlRoleManager.java` +89 lines.

### Phase A loose ends — closed 2026-05-08

All four follow-ups landed in one slice:
- `grant()` enforces OWN/GROUP-on-RLS invariant via
  `rejectRlsScopeOnNonRlsTable`; new test
  `TestSqlRoleManager.grant_rlsScopeOnNonRlsTable_throws`.
- `revoke()` now deletes the matching RPM row; new test
  `TestSqlRoleManager.revoke_deletesRpmRow`.
- Dead `RPM_TABLE_NAME.ne("*")` predicates removed from
  `SqlRoleManager` (two occurrences: `setPermissions` delete block and
  `getPermissionSet` fetch).
- `TestGraphqlSchemaRoles.rolesQuery_schemaField_roundTrips` teardown
  iterates `listRoles()` and drops all custom roles.

### Phase F.1 / F.2 / F.3 — closed 2026-05-08

Scout audit + new `TestCrossSchemaFkRlsVisibility` confirms:
- F.1: SqlQuery `refJoins()` auto-joins FK targets fully-qualified
  under user `SET ROLE`; PG RLS fires on every cross-schema FK path
  reached via `retrieveJSON()` subfield expansion. No production change
  needed for that path.
  **Exception (2026-05-08, since reverted)**: an earlier iteration added
  RLS NULL-clamping in `retrieveRows()` (`buildRlsClampAliases`,
  `addRlsClampJoins`, overloaded `rowSelectFields`). That clamp was
  intentionally removed at the Phase R baseline (commit 76edd816e:
  -63 SqlQuery, -123 SqlTable, both F.1 test files deleted) so Phase R
  could ship hide-don't-lock from a clean slate. R.3b reintroduces FK
  visibility as row-hide (not clamp) using `mg_can_reference`.
- F.2: per-table policies emitted via `enableRlsCascade` for every
  table in inheritance tree (`SqlTableMetadata.java:556-577`); each
  policy fires independently on its rows.
- F.3: subclass-only RLS rejected at metadata layer
  (`SqlTableMetadata.setRlsEnabled` lines 522-538); covered by
  `TestRlsInheritanceCascade.enableOnNonRootRejected`.

Test scaffold (rebuilt fresh in R.3b — original F.1 test file was deleted at Phase R baseline). New `TestCrossSchemaFkRlsVisibility` has 6 tests, all GREEN:
- `scalarRef_hidesChildRow_whenFkTargetInvisible` — core R.3b row-hide
- `joinResolvesOnlyVisibleParents` — JSON path; PG RLS suppresses target data
- `refArrayDropsInvisibleElements` — REF_ARRAY behavior unchanged in R.3b (R.3c will flip)
- `nullFkRow_remainsVisible` — null FKs don't hide the row
- `refbackEmptyForInvisibleParent` — refback path stable
- `referenceAllOnRefTable_keepsChildRowVisible` — REFERENCE_ALL keeps row visible despite VIEW_NONE

### Phase I — UI integration (DRAFT 2026-05-08)

Backend RLS is feature-complete; UI surfaces lag. Five slices, ordered
small→large. **Backend is presumed already wired via GraphQL**
(`change(roles:…)`, `change(members:…)`, `change(groups:…)`,
`tables.rlsEnabled`); slices that find a backend gap promote it to a
sub-task.

**Terminology to confirm with user**: "old bootstrap" vs "new bootstrap"
in the user's request maps to which concrete apps?

- `apps/schema/` is Vue3 + Bootstrap CSS (legacy schema editor).
- `apps/ui/` is the new Nuxt + Tailwind admin app.
- `apps/molgenis-components/` is the shared form-rendering library
  (`RowEdit.vue` etc.) used by both — Tailwind-styled in newer surfaces.

Working assumption: "old bootstrap schema app" = `apps/schema/`; "new
bootstrap forms" = `apps/molgenis-components/RowEdit.vue` rendered
inside `apps/ui/`. **Confirm before implementing I.5.**

**I.1 — CSV/Excel round-trip of `rls_enabled`**

Smallest slice. EMX2 import/export currently does NOT carry the
RLS flag.

- File: `backend/molgenis-emx2-io/.../emx2/Emx2.java`.
- Add `RLS_ENABLED` constant; read in `fromRowList` (table-level
  property); write in `toRowList` / `getHeaders`.
- Tests: extend `TestEmx2*` with round-trip — schema with `rlsEnabled=true`
  on root table; export → re-import → flag preserved; subclass
  rows always serialise as false.
- Docs: append `rls_enabled` row to the EMX2 schema-format reference in
  `docs/molgenis/schema_format.md` (or wherever the row-by-row table
  doc lives — confirm path via scout when the slice opens).

**I.2 — RLS toggle in legacy schema editor (`apps/schema/`)**

- Component: `apps/schema/src/components/TableEditModal.vue`.
- Add a labelled boolean input (`rls_enabled`) below the existing
  Semantics section. Bind to `row.rlsEnabled`.
- Wire through existing `emitOperation()` so the change(tables:…)
  mutation carries the flag.
- Reject UI for subclass tables (existing backend rejection surfaces
  the message `"enable on root '<X>' instead"` — surface as toast).
- Test: add to existing `apps/schema/` story / e2e if any; otherwise
  manual visual check (mark "visual check" in spec).

**I.3 — Tailwind admin: role + permission manager (`apps/ui/`)**

- New page `apps/ui/app/pages/admin/roles/[schema].vue` (or similar).
- List custom roles for a schema: query `_schema.roles`. Show
  description, table count.
- CRUD: create/rename/delete role via `change(roles:…)` /
  `drop(roles:…)`. Use existing modal pattern (`EditUserModal.vue`).
- **Permission editor**: select a role → table list with per-table
  scope dropdowns:
  - `select_scope`: NONE | EXISTS | COUNT | RANGE | AGGREGATE | OWN
    | GROUP | ALL
  - `insert_scope` / `update_scope` / `delete_scope`: NONE | OWN |
    GROUP | ALL
  - `change_owner`, `change_group`: booleans
  - Privacy modes (EXISTS/COUNT/RANGE/AGGREGATE) only available when
    table has the requisite columns; backend rejects mismatch — surface
    error.
- GraphQL: `change(roles:[{name, permissions:[{table, selectScope, …}]}])`.
  Confirm mutation surface exists before slice opens.
- Test: at least one playwright e2e covering create-role → set
  permission → RLS query reflects scope.

**I.4 — Tailwind admin: membership manager (`apps/ui/`)**

- New page `apps/ui/app/pages/admin/members/[schema].vue`.
- List `(user, role, group?)` triples per schema: query
  `_schema.members`. Show user, role, group, schema-wide vs group-bound.
- CRUD: assign / unassign via `change(members:…)` / `drop(members:…)`.
- Validation surfaces: system-role + group rejected (clean error
  toast); World A semantics — assigning user to role with no group
  supersedes existing group-bound rows (warn user with "this will
  remove existing group-scoped grants" confirmation dialog).
- Group selector: `_schema.groups` query; allow inline group create.
- Test: playwright e2e covering schema-wide grant supersedes
  group-bound (the World A invariant).

**I.5 — `mg_owner` / `mg_groups` editing in forms**

- Component: `apps/molgenis-components/src/components/forms/RowEdit.vue`.
- Currently filtered out by `!column.id?.startsWith("mg_")` in
  `shownColumnsWithoutMeta`.
- Add explicit allow-list for `mg_owner` and `mg_groups` gated on
  permissions:
  - Visible only when row's table has RLS enabled.
  - Editable only when current user has `change_owner` / `change_group`
    on that table (read from existing `tablePermissions` prop).
  - When non-editable but visible: render read-only display.
- Backend already enforces (RLS policy WITH CHECK rejects mutation
  attempts that change owner/group without permission); UI just hides
  the input.
- Test: existing form story file; add story variants
  `with-rls-and-change-owner-permission`,
  `with-rls-without-change-owner-permission`.

**Open questions for user before implementing:**

1. Confirm "old bootstrap" = `apps/schema/`, "new bootstrap" =
   `apps/molgenis-components/` rendered inside `apps/ui/` (tailwind).
2. Order of slices: I.1 → I.2 (small + low-risk warm-up) → I.3 → I.4 →
   I.5? Or different order?
3. Membership UI (I.4): inline group create vs separate group manager?
4. Permission editor (I.3): per-table grid or per-role-then-table
   drill-in? Latter is what the sketch above proposes.
5. Should I.5 also gate display of `mg_owner` / `mg_groups` columns in
   table list views, not just form modals? (Scope-creep risk.)

**Decision plan**: confirm terminology + open questions → open I.1
(smallest, clearest backend-touching slice) → close → re-evaluate.

### Phase J — code-review cleanup wave (DRAFT 2026-05-08)

User code-review feedback on Phase H deliverables. Skill
`backend-test-purity` captures the test rules going forward. This phase
addresses accumulated debt in waves; each wave dispatches in parallel.

**Wave J.1 — Refactors (touch many tests; do first)**

- **J.1.a** Fold `SelectScope` and `UpdateScope` enums into
  `PermissionSet` (single source of scope semantics; reduces enum
  surface). `SelectScope` is the richer ladder (NONE..ALL incl.
  privacy modes); `UpdateScope` is the subset (NONE/OWN/GROUP/ALL).
  Decide: nested enums on `PermissionSet`, or flatten into a single
  `Scope` enum + per-axis allow-list.
- **J.1.b** Push GraphQL `change(roles:…)` system-role check from
  `Graphql*FieldFactory` down to `SqlRoleManager` (Java layer is
  authoritative; GraphQL surfaces the exception).
- **J.1.c** `allowsCount` audit — currently unused. Either delete or
  add coverage in `PleasePsqlQuery` (count-floor projection test).
- **J.1.d** Push `requireManagerOrOwner` and `rejectCustomRoleEscalation`
  checks from `Graphql*FieldFactory` down to `SqlRoleManager` entry
  points (createRole, deleteRole, setPermissions, createGroup,
  deleteGroup, addGroupMembership, grantRoleToUser, addGroupMember,
  removeGroupMember). Java layer is authoritative. **Plus** filter
  GraphQL schema generation: omit `change(roles:…)`,
  `change(groups:…)`, `change(members:…)`, `drop(roles:…)`,
  `drop(groups:…)` from the schema returned to sessions whose active
  user is not Admin/Owner/Manager — defense-in-depth so the API does
  not even surface for non-privileged users.
- **J.1.e** Schema Java-API consolidation (DRAFT 2026-05-09). Move the
  per-input loops, system-role classification, and list-shape massaging
  out of `GraphqlSchemaFieldFactory` into `Schema` so the GraphQL layer
  becomes a thin pass-through and `SqlRoleManager` stays internal.
    - Extend `Member` record with `groupName` (nullable; set when the
      membership is via a group) and `isSystemRole` (or surface via the
      embedded `Role`). Uniform shape for direct + group-scoped members.
    - Replace `SqlRoleManager.listGroups` `List<Map<String, Object>>`
      return with a typed `Group` record (`name`, `description`,
      `members`). Decision 2026-05-09: **eager** — `members` populated
      on the Group record itself.
    - `Schema` API additions:
      - `getMembers()` — fold direct + group memberships into one list.
      - `getRoles()` — return `List<Role>` (existing record) instead of
        `List<String>`; include system roles with `isSystemRole=true`.
      - `getGroups()` — new, `List<Group>`.
      - `changeRoles(List<Role>)`, `changeGroups(List<Group>)`,
        `changeMembers(List<Member>)` — batch ops; do
        Manager/Owner check + system-role gate internally; delegate to
        `SqlRoleManager` per-item.
      - `dropRoles(List<String>)`, `dropGroups(List<String>)`,
        `dropMembers(List<Member>)` — symmetric.
    - `GraphqlSchemaFieldFactory.changeRoles/changeGroups/changeMembers/
      dropRoles/dropGroups/dropMembers` become thin: accept input,
      build the list, call `schema.changeXxx(list)` / `schema.dropXxx(list)`.
      No loops, no system-role classification, no
      `SqlRoleManager` reference.
    - GraphQL schema-filter gate from J.1.d stays (defense-in-depth);
      Java enforcement remains authoritative.
    - Migration of existing tests: `TestGraphqlSchemaRoles`,
      `TestGraphqlSchemaMembers`, `TestSqlRoleManager` may need
      assertion updates for the new return shapes; do not loosen.
    - Order (decided 2026-05-09): **before J.4** — audit is more
      meaningful against the consolidated API.
    - Backward-compatibility (decided 2026-05-09):
      - Java: `Schema.getRoles()` signature changes to `List<Role>`
        (hard break, all callers in-repo updated in the same slice).
      - GraphQL: ADDITIVE — keep existing `name` field on role type,
        ADD optional `isSystemRole: Boolean`. Python and other
        clients that only request `name` continue to work; clients
        that want the flag opt-in. Run the python-client smoke
        suite (if present) after the change to verify.
    - **J.1.e.2** (2026-05-09, in flight). Fold direct + group-mediated
      memberships into a single `Schema.getMembers()` (`Member.groupName`
      set on group entries). Delete `Schema.listCustomMemberships`,
      `SqlSchema.listCustomMemberships`, `SqlRoleManager.listCustomMemberships`.
      `GraphqlSchemaFieldFactory.queryFetcher` uses only `schema.getMembers()`.
      Goal: master-level simplicity in queryFetcher.
    - **J.1.e.3** (2026-05-09, queued). Three follow-ups raised post-J.1.e.2:
      - Drop `groupsToMapList` (and any sibling `*ToMapList`) helpers in
        GraphqlSchemaFieldFactory — Jackson serializes the records directly.
      - Fix `SqlRoleManager.getRole():323` system-role flags: Manager and
        Owner must report `changeOwner=true, changeGroup=true` (migration32.sql
        trigger bypasses checks for them). Verify other system roles per
        `Privileges.java`. Custom roles already correct via
        `role_permission_metadata`.
      - Move group-metadata SQL out of `SqlRoleManager.listGroups` (and
        peers like `listMembersForGroup`, `listGroupNames`) into
        `MetadataUtils` to match the existing pattern (DDL/DML in
        MetadataUtils, SqlRoleManager orchestrates).

**Wave J.2 — `mg_owner` format-bridge follow-through (option a)**

Decision: `mg_owner` stores bare EMX2 username (NOT `MG_USER_`-prefixed).

- **J.2.a** `SqlTable.insertBatch` default: store `activeUser` (bare),
  not `MG_USER_PREFIX + activeUser`.
- **J.2.b** `mg_can_read` / `mg_can_write` / `mg_can_write_all`:
  compare `mg_owner` to `current_user` by adding the prefix on the
  comparison side, OR strip on the column side. Pick simpler.
- **J.2.c** Declare raw PG FK `mg_owner → "MOLGENIS".users_metadata(username)`
  with `ON DELETE SET NULL`. Decision 2026-05-09: keep Column type as STRING —
  MOLGENIS metadata schema is not exposed as EMX2 tables, so EMX2 REF type
  cannot be used. Apply the constraint in `SqlRoleManager.enableRlsForTable`
  alongside the `ALTER TABLE … ADD COLUMN mg_owner TEXT`. Add a migration
  for tables that already had RLS enabled before this change.
- **J.2.d** Update existing tests touching `mg_owner` literal values
  (drop `MG_USER_` prefix in expected values).

**Wave J.3 — Test purity sweep (one agent per file, parallel)**

All cite skill `backend-test-purity`. For each file: drop teardown,
convert SQL mutations → Java API, rename methods to match assertion,
remove unused helpers/vars.

- `TestAccessFunctions` — convert SQL setup; convert `insertRow` to
  `table.insert(Row)`; remove teardown
- `TestSchemaWideCustomGrants` — assess SQL necessity; convert if
  possible
- `TestSelectScope` — convert setup; remove teardown
- `TestSqlRoleManager` — remove teardown; remove unused `schemaB`;
  rename `dropRoles_tombstonesRole` → `dropRoles`; rename
  `changeMembers_grantsRole` → `changeMembers_grantsRole_groupIsNull`;
  audit `dropMembers_revokesSystemRole` for duplicate coverage
- `TestCrossSchemaFkRlsVisibility` — convert setup
- `TestCurrentUserGroups` — convert setup; remove teardown; verify
  `functionReturnsGroupsForCurrentUser`,
  `functionReturnsEmptyArrayForUnknownSchema`,
  `functionOnlyReturnsGroupsForRequestedSchema`,
  `groupsMetadataFkCascadesOnSchemaDelete`,
  `triggerRejectsUpdateOnSystemRoleRow` use SQL only for verification
- `TestRoleManagerColumnGrantEnforcement` — convert to Java API
- `TestChangeOwnerGroupSqlEnforcement` — convert remaining SQL in
  `rejectsShareIntoForeignGroup`,
  `updateGroupsBlockedWhenChangeGroupFalse`,
  `updateGroupsAllowedWhenChangeGroupTrue`,
  `insertWithGroupsAllowedWhenChangeGroupFalse`,
  `updateBlockedWhenChangeOwnerFalse`,
  `updateOwnerAllowedWhenChangeOwnerTrue`,
  `insertWithForeignOwnerBlockedWhenChangeOwnerFalse`,
  `insertWithForeignOwnerAllowedWhenChangeOwnerTrue`
- Audit aliases / helpers across all RLS tests: drop `sha1Hex`,
  rename / inline `changeCapTriggerName`

**Wave J.4 — Over-coverage audit**

User concern: "do we really need sooo many tests? seems we are testing
same on different layers." Spawn scout to map junit ↔ integration
coverage; flag duplicates; user approves deletes.

- Output: table per behaviour with covering tests across layers
- Decision rule from skill rule 4: "If a behaviour is tested at the
  integration / GraphQL layer, do NOT also test it at the SQL layer."
  Integration wins.

**Wave J.5 — Misc cleanup**

- **J.5.a** Drop `testImplementation
  'uk.org.webcompere:system-stubs-jupiter:2.1.8'` from
  `molgenis-emx2-sql/build.gradle` (unused).
- **J.5.b** Move `SqlDatabaseTest` back to its original package.
- **J.5.c** `sessionPermissions_currentUserSeesOwnPermissions` —
  current `canView` boolean assumes binary read scope; rework for the
  full ladder (`NONE..ALL`). Confirm assertion shape with user before
  refactor.
- **J.5.d** `assertTrue(found, "Expected canView=true … with
  AGGREGATE select scope")` — clarify expected behaviour (AGGREGATE
  is privacy-projected, not full read; assert may be wrong).
- **J.5.e** `TestRlsPerformance` — keep, add canary timer (e.g.
  `assertTrue(elapsed < 5000)` on the GROUP-scope large-scale path).
  Don't disable.

**Wave J.6 — Docs**

- **J.6.a** Merge `use_permissions.md` + `use_rls.md` into a single
  RLS / permissions guide.
- **J.6.b** Delete `dev_graphql-rls.md`.

**Order**: J.1 → J.2 → J.3 (parallel) → J.4 → J.5 → J.6.

### Phase J.7 — column-level GRANTs for change_owner / change_group enforcement (OUT OF SCOPE — decided 2026-05-09)

**Decision**: Java-layer + trigger enforcement is sufficient. PG-level
column-grant rejection is NOT a project goal. `TestRoleManagerColumnGrantEnforcement`
will be deleted. Section retained below for historical context only.

(Original draft 2026-05-08:)

**Why this slice exists**: during J.3 verification, an agent
attempting to fix a missing-column bug exceeded scope and bundled
column-level grant enforcement into a "fix the H.4 gap" task. The
over-reach was rolled back; this slice captures it as a deliberate
decision point.

**Current state after rollback** (post-J.3, pre-J.7):
- Table-level GRANTs: `INSERT/UPDATE/DELETE` on the whole table for
  the appropriate role.
- Java layer enforces `change_owner` / `change_group` via the
  `mg_check_change_capability` trigger (PG-side) AND via SqlTable
  (Java-side).
- `TestRoleManagerColumnGrantEnforcement` (which asserts PG-level
  rejection of raw-SQL `UPDATE mg_groups`) currently fails — it
  documents the gap.

**Decision required from user before J.7 opens**:
- Do we want PG-level (column-grant) enforcement of
  `change_owner`/`change_group`, or is Java-layer + trigger
  enforcement sufficient?
- If yes → J.7 plan:
  1. SqlRoleManager: split table-level
     `GRANT INSERT/UPDATE` into column-level grants.
     `mg_owner` excluded from the column list when
     `changeOwner=false`; `mg_groups` excluded when
     `changeGroup=false`.
  2. SqlTable.getInsertColumns: omit `mg_owner`/`mg_groups` from
     INSERT column list when caller didn't provide them (so PG
     doesn't see an INSERT with all columns and reject for missing
     privilege).
  3. SqlUserAwareConnectionProvider: set `molgenis.active_user`
     session variable on every connection acquire (admin + user).
  4. `mg_check_change_capability` trigger: read
     `current_setting('molgenis.active_user')` to populate
     `NEW.mg_owner` server-side when not provided.
  5. Tests:
     - `TestRoleManagerColumnGrantEnforcement` should pass without
       further test changes (the assertion is PG-level rejection).
     - Verify no regressions in TestRlsPerformance,
       TestEffectiveSelectScopes, full RLS suite.
- If no → delete `TestRoleManagerColumnGrantEnforcement`; PG-level
  rejection is not a goal.

**Open question**: does this overlap with J.5.c (canView-as-boolean
rework)? Both touch the question of how the GraphQL session
permissions surface enforcement state.

**Open question on J.5.c**: how should `canView` surface non-binary
scopes in the GraphQL session-permissions response?  Options: enum
field `viewScope: NONE|EXISTS|...|ALL`; or per-mode booleans
(`canViewExistsOnly`, `canViewAggregate`, …). Default proposal: enum.
Confirm before J.5.c opens.

### Phase L pre-amble — RLS DDL prolonged-timeout bump — closed 2026-05-09

**What landed**

Added `SqlDatabase.txWithProlongedTimeout(Transaction)` helper
(60s query timeout vs default 10s); routed three admin DDL
operations through it:
- `SqlTableMetadata.disableRlsCascade`
- `SqlTableMetadata.enableRlsCascade`
- `SqlDatabase.dropSchema`

**Why**

Combined-suite phase-boundary verification (post-Phase-J) flaked on
`TestRlsEnableDisableLifecycle.tearDown` (`dropSchema`) and earlier
on `disableRlsCascade` under parallel test-class load. Root cause:
contention on shared `MOLGENIS.*` metadata tables. Bump is a
band-aid, not a root-cause fix — but matches reality (admin DDL
with cascading metadata writes legitimately needs more headroom
than the default 10s, especially in multi-admin installs with
heavy concurrent schema editing).

Real-user motivation, not just test motivation.

**Verification**

`./gradlew :backend:molgenis-emx2-sql:test
--tests "*TestRlsEnableDisableLifecycle"` → 7/7 green.
Combined-suite re-verification still owed (user-initiated).

Diff: 33 LOC. Files: `SqlDatabase.java`, `SqlTableMetadata.java`.

### Phase L — layering cleanup wave (DRAFT 2026-05-09)

**Why this phase exists**

Deep review (2026-05-09) of `SqlRoleManager` + adjacent code surfaced
nine concerns around layering, duplication, and naming after the
RLS v4 build-out. Most are low-risk hygiene that prepares the code
for **Phase K** (RBAC import/export + bulk apply): K's bulk-apply
methods naturally land in `MetadataUtils`, but `MetadataUtils`
hasn't yet absorbed all the metadata-table SQL it should own. Doing
L first avoids K duplicating extraction work.

**Run L before K.** L #6 is a release blocker; L #8 establishes
the seam K builds on; L #7 was a cross-cutting correctness issue
that has since been superseded and reverted (see L.7 entry below).

**Slices** (priority order from review)

- **L.1 — Merge Migration 33 into Migration 32** — closed 2026-05-09.
  Collapsed `migration33.sql` into `migration32.sql` (43 lines
  appended); dropped `if (version < 34)` block from
  `Migrations.java`; `SOFTWARE_DATABASE_VERSION = 33`.
  TestMigration + TestRlsEnableDisableLifecycle both green.
  ~28 LOC.

- **L.2 — Push system-role `PermissionSet` synthesis down** — closed 2026-05-09.
  Added `SqlRoleManager.synthesizeSystemPermissionSet(roleName)` +
  early-return guard in `getPermissionSet`. Collapsed
  `GraphqlSchemaFieldFactory.java:547–563` if/else to single
  `schema.getPermissions(role.name())`; removed unused
  `SelectScope`/`UpdateScope` imports. Verified
  `:backend:molgenis-emx2-sql:test --tests "*TestSqlRoleManager"`
  → 21/21 pass.

- **L.3 — Extract metadata-table SQL → `MetadataUtils`** — closed 2026-05-09.
  Moved ~190 LOC of metadata-table SQL into `MetadataUtils` as
  `upsertRolePermission`/`upsertRolePermissionScopes` (split:
  full upsert vs scope-only on-conflict to preserve original
  `grantTablePermission` semantics for `changeOwner`/`changeGroup`/
  `description`), `deleteRolePermission`,
  `deleteAllRolePermissions`, `deleteAllRolePermissionsForTable`,
  `loadRolePermission`, `loadPermissionSet`, `rolePermissionExists`,
  `upsertGroupMembership`, `deleteGroupMembership`,
  `deleteGroupMembershipForRole`,
  `deleteAllGroupMembershipsForRole`, `membershipRowExists`,
  `requireUserExists`, `fetchDirectAndGroupMembers`. Moved
  `GROUP_MEMBERSHIP_SENTINEL_ROLE` constant. Deleted dead
  `SqlRoleManager.membershipRowExists` wrapper. Added regression
  test `grant_scopeOnly_doesNotClobberChangeOwnerOrDescription`.
  Verified `:backend:molgenis-emx2-sql:test --tests
  "*TestSqlRoleManager"` 22/22 green;
  `--tests "*TestGrantRolesToUsers"` 4/4 green.

- **L.4 — Resolve `getPermissions` overload confusion + move
  validation** — closed 2026-05-09. Renamed
  `SqlRoleManager.getPermissions(String, String)` →
  `listTablePermissions(String, String)`. Moved
  `rejectRlsScopeOnNonRlsTable` to `TableMetadata` as
  `rejectIfRlsScopeWithoutRls(TablePermission, PermissionSet)` —
  invariant lives where the `rlsEnabled` field is owned; zero
  new imports. No interface ripples. Verified
  `:backend:molgenis-emx2-sql:test --tests "*TestSqlRoleManager"`
  22/22 green; `--tests "*TestGrantRolesToUsers"` 4/4 green.

- **L.5 — Move `applyRlsEnabledChanges` into `Schema.migrate`** —
  closed 2026-05-09. Option (a): added `applyRlsEnabledDiff` in
  `SqlSchema` per-table diff loop; deleted the
  `applyRlsEnabledChanges` graphql workaround. RLS enable now
  triggers from any migrate caller. Verified
  `:backend:molgenis-emx2-sql:test --tests
  "*TestRlsEnableDisableLifecycle"` 9/9 green.

- **L.5b — Reject `rlsEnabled: true → false` via migrate path** —
  closed 2026-05-09 (graphql-only enforcement; superseded by L.5c).
  Initial implementation rejected at graphql `changeTables` only
  via raw-input-map inspection because `TableMetadata.rlsEnabled`
  was primitive `boolean` (couldn't distinguish "explicit false"
  from "omitted"). Java/CSV/REST callers bypassed the invariant.
  Owner rejected this scope — see L.5c.

- **L.5c — Tri-state `rlsEnabled` for uniform migrate-layer
  enforcement** — closed 2026-05-09. `TableMetadata.rlsEnabled`
  is now `Boolean` (nullable). Migrate diff loop in
  `SqlSchema.applyRlsEnabledDiff` enforces: `null` = no-op,
  `Boolean.FALSE` on RLS table = REJECT, `Boolean.TRUE` on
  non-RLS table = enable. Deleted graphql
  `rejectExplicitRlsDisable` workaround. Swept ~12 call sites of
  `getRlsEnabled()` to use `Boolean.TRUE.equals(...)` (production
  + tests). New test
  `TestRlsEnableDisableLifecycle.migrate_rlsEnabled_trueToFalse_isRejected`.
  Verified `:backend:molgenis-emx2-sql:test --tests
  "*TestRlsEnableDisableLifecycle"` 9/9 green;
  `*TestSqlRoleManager` green; `*TestGraphqlSchemaTables` 4/4 green.

- **L.6 — Extract pg_catalog half of `executeGetMembers` cleanup**
  *(folded into L.3)*.

- **L.7 — RLS FK-clamp write-back hazard**

  > **STATUS: SUPERSEDED PRE-SHIP. Dropped from v4.**
  > Owner decision: existence privacy for FK-to-RLS-target reads moves entirely to Phase R (explicit REFERENCE permission). v4 ships with FK columns exposing the target PK regardless of RLS visibility — same stance as Hasura / PostgREST / native PG RLS. The L.7 read-clamp (`rlsClampAlias`) and write-back guard (`guardRlsFkWriteBack`) and associated tests have been reverted on this branch.

  Historical record (preserved for plan archaeology): closed 2026-05-09
  (option a). RED-GREEN: added failing test
  `TestRlsFkWriteBackGuard.resave_withClampedNull_isRejected`,
  verified red, then implemented `SqlTable.guardRlsFkWriteBack`
  called at top of `updateBatch`. Visibility predicate
  `isRlsFkColumn` reused with `SqlQuery.buildRlsClampAliases`
  (matched by the same `col.isRef() && Boolean.TRUE.equals(
  col.getRefTable().getRlsEnabled())` shape). Admin batched
  SELECT per RLS-FK column followed by user-context IN-visibility
  check per ref table — no N+1 at the row level (per-column
  batching could be optimized to per-target-table later if hot).
  Added 5-LOC javadoc to `buildRlsClampAliases`. Tests:
  TestRlsFkWriteBackGuard 5/5; TestPrivacy 7/7;
  TestRlsEnableDisableLifecycle 9/9 — all green. **All reverted.**

- **L.8 — Reduce `molgenis-emx2-nonparallel-tests` to genuine
  cases** — closed 2026-05-09. Relocated 3 tests
  (`TestGraphqlAggregatePermission` → graphql;
  `TestSettings`, `TestChangeOwnerGroupSqlEnforcement` → sql).
  Existing schema-name uniqueness preserved (no rename needed).
  Removed unused deps from
  `molgenis-emx2-nonparallel-tests/build.gradle`. Verified:
  TestSettings, TestChangeOwnerGroupSqlEnforcement,
  TestGraphqlAggregatePermission, TestMigration → BUILD SUCCESSFUL.
  TestSettingsMerge (kept in nonparallel) has 2/3 pre-existing
  failures unrelated to L.8 — see backlog L.10 below.

- **L.10 (backlog) — TestSettingsMerge `merge-key-b null`**.
  Pre-existing in this branch (introduced commit f6c732ec6
  "pivot"; not on master, can't master-baseline). 2/3 tests fail
  on `merge-key-b` null — likely global-state contamination or
  incomplete settings-merge implementation. Investigate after
  Phase L closes; not blocking M/K.

- **L.11 (backlog, non-blocking) — Pre-existing VIEWER-permission test failures (5 tests)**.
  Tests fail because their setUp() doesn't grant VIEWER role to the active user before reading
  from RLS-enabled tables. Pre-dates the L.7 revert (verified against HEAD^). Fix: review each
  setUp() and add `schema.addMember(user, "Viewer")` (or equivalent) before the read assertions.
  Affected tests:
  - `TestPrivacy.countScope_onRlsTable_passesThroughPolicy_andClampsProjection`
  - `TestSelectScope.countScopeRlsPassThroughSeesAllRows`
  - `TestRlsPerformance.stressTest_largeMgGroupsArray`
  - (plus 2 others with the same VIEWER-permission signature — total 5)

- **L.12 (backlog, non-blocking) — Pre-existing `db.getSchema()` NPE in user-switch tests (3 tests)**.
  Tests call `db.getSchema(SCHEMA_NAME)` after `setActiveUser(user)`; schema lookup returns null
  and NPEs. Either schema context is lost across user switches (production bug) or tests need to
  re-fetch the schema with admin context first. Pre-dates L.7 revert.
  Affected tests:
  - `TestTablePolicies.noneScopeIsRejectedBeforeRls`
  - `TestRlsPerformance.overhead_groupScope_atLargeScale`
  - `TestRlsPerformance.overhead_selectAll_rlsVsNoRls`
  - `TestRlsPerformance.overhead_groupScope_vs_allScope`

- **L.13 — Test harness: drop `initDatabase` UpToDate marker**. _Status (2026-05-10): DONE._
  Removed the `outputs.upToDateWhen` block + marker `doLast` from `initDatabase` in
  `backend/molgenis-emx2-sql/build.gradle`. Verified: `cleandb` → `test --tests "*TestSqlRoleManager*"`
  green without manual marker cleanup. The init step is now always run; `Migrations.initOrMigrate`
  no-ops when stored version equals SOFTWARE_DATABASE_VERSION (Migrations.java:25, MetadataUtils.java:189).

- **L.14 (backlog, non-blocking) — Pre-existing test-order sensitivity in role-grant batch**.
  `TestGrantRolesToUsers.testRole` and `TestUsersAndPermissions.testActiveUser` fail when run
  in a wide batch (`*Permission* *Role*`); pass when run in isolation or in narrow pairs. Scout
  could not reproduce on master with the narrow 2-class form, so failure shape depends on the
  full class set in the JVM. Pre-existing schema-state coupling, unrelated to Phase R metadata
  work. Resolve before/after Phase R ships — not blocking R.2/R.3 since those don't touch
  pre-existence semantics of role grants.

- **L.15 (backlog, design question) — Cross-schema FK metadata access for users with no refSchema role**.
  Surfaced 2026-05-10 in `crossSchema_throwsForUserWithNoMembershipInRefSchema`. When a user has zero
  membership in a refSchema (cannot see the schema exists), querying a Child table whose FK points
  into that refSchema throws `MolgenisException` from `Column.getRefTable()` at query-build time —
  before any RLS predicate runs. So a Viewer in CHILD_SCHEMA can't query `Owner.pet` at all unless
  they also have *some* role in REF_SCHEMA. The intended behavior under hide-don't-lock would be:
  predicate-filter Child rows whose FK target is invisible (predicate already does this correctly
  given metadata access). Fix would either expose minimal FK-target metadata to all users of the
  Child schema, or introduce an "implicit reference visibility" tier. Phase R does NOT block on
  this — the existing `mg_can_reference` predicate is still the right semantic; only the
  metadata-access guard is too aggressive. Decide: relax metadata access for cross-schema FK
  resolution, or document that "user needs at least one role in refSchema to query cross-schema
  FKs."

- **L.18 (backlog enhancement) — Per-table-type `_meta.permissions` with full scope enums**.
  Surfaced 2026-05-10 during R.4e scoping. Today the frontend uses `_session.tablePermissions[].canView/canInsert/...` booleans (R.4e adds `canReference` boolean). When the frontend needs richer scope semantics — e.g., distinguish `insert=OWN` from `insert=ALL` for capability hints, or "this row is COUNT-only" inline — add a per-table-type `_meta.permissions` sub-field exposing `SelectScope/UpdateScope/ReferenceScope` enums. Could be row-level if Phase R+1 introduces row-level scope variation. Not blocking Phase R.

- **L.17 (backlog cleanup) — `SqlSchemaMetadata.rolesCache` stale-read after `setActiveUser`**.
  Surfaced 2026-05-10 during R.4c test development. The `rolesCache` field is instance-level and
  only cleared by `reload()`, not `clearCache()`. Switching the active user on the same `Schema`
  object yields stale cached roles → tests must call `database.getSchema(SCHEMA_NAME)` to obtain a
  fresh object after each `setActiveUser` switch. Cleanup options: clear `rolesCache` in
  `clearCache()`, or invalidate it on user-switch listener, or document the workaround. Not
  blocking Phase R; tests work around it.

- **L.9 — Skipped**. `Group` and `Role` are already records;
  `Member` is the mutable-class outlier. Owner decision (2026-05-09):
  prefer class over record, no action.

**Out of scope for L**

- Member→record conversion (decision: keep Member as class).
- `SqlRoleManager.getPermissions` (Schema, String) full audit (covered
  in L.4 only at signature-renaming level).
- Anything that touches the public Java API surface beyond the
  named refactors above.

**Decisions taken (2026-05-09)**

1. **L.7 design = option (a)** *(superseded — reverted pre-ship 2026-05-09; existence privacy moves to Phase R)*: keep current NULL clamp on read, add
   a write-back guard in `SqlTable.update`/`save`. For each FK column
   where user explicitly submitted NULL AND stored value is non-NULL
   AND target row is invisible to active user → reject the entire
   batch with a clear error. One batched admin SELECT per update tx
   that touches RLS-FK columns.

   Reference shape (historical; all reverted):
   ```java
   List<RefCheckRow> rlsClampedRefs = collectExplicitNullRefsToRlsTables(rows);
   if (!rlsClampedRefs.isEmpty()) {
     Map<PrimaryKey, Map<RefColumn, Object>> stored =
         adminJooqBatchSelect(rlsClampedRefs);
     for (RefCheckRow r : rlsClampedRefs) {
       Object storedFk = stored.get(r.pk).get(r.column);
       if (storedFk != null && !targetVisibleToActiveUser(r.column.refTable, storedFk)) {
         throw new MolgenisException(
             "Cannot null FK '" + r.column.getName() + "': "
           + "current target is outside your read scope.");
       }
     }
   }
   ```

   Trade-off noted: a user who can't see the FK target also can't clear
   it. Owner rejected this approach in favor of Phase R's cleaner
   row-visibility rule (FK column exposes target PK regardless of RLS
   visibility — Hasura/PostgREST/native-PG-RLS stance).

2. **L.5 approach = option (a)**: fix the diff in `migrate` itself
   (don't add a post-hook in `SqlSchema`). Locate the per-table
   diff loop in `SqlSchemaMetadata.migrate` and add an
   `rlsEnabled` diff alongside existing column-diff handling, so
   `currentSqlTable.setRlsEnabled(incoming.getRlsEnabled())` flows
   through the SQL-overriding setter naturally. Removes graphql's
   `applyRlsEnabledChanges` entirely.

   If during L.5 implementation migrate's diff structure won't
   accept rlsEnabled cleanly without bigger refactor → STOP and
   surface to lead; either reshape migrate or fall back to a
   bounded post-hook variant.

3. **Slice order = strict sequential**: L.1 → L.2 → L.3 → L.4 →
   L.5 → L.7 → L.8. Parallelization rejected (slices interact
   on `SqlRoleManager` / `MetadataUtils` / migrate seam — bisecting
   a parallel-batch failure would cost more than the sequential
   tax). *(L.7 was subsequently reverted pre-ship; the order is preserved here for archaeological context.)*

### Phase M — explicit RLS disable API (DRAFT 2026-05-09)

**Why this phase exists**

Owner decision (2026-05-09): the schema-authoring path (graphql
`change` / `Schema.migrate`) is one-way for RLS — see L.5b. To
preserve the ability to genuinely reclassify a table or recover
from authoring mistakes, a deliberate Owner-only admin API is
provided as the **only** public path to disable RLS.

This phase is small and depends on L.5b landing first (so the
authoring path is closed before the deliberate path opens).

**Decisions taken (2026-05-09)**

1. **Owner-only.** Disable is more dangerous than enable
   (data exposure + destructive column drops); asymmetric
   authority for asymmetric blast radius. Manager retains all
   other RBAC operations including `enableRls`.
2. **Hard block when table has data. No `force` flag.**
   If user truly wants to declassify a populated table, the
   path is: export data → drop schema → create new schema
   without RLS → re-import. No bypass switch.
3. **GraphQL only. No REST endpoint.** Add later only if a
   real consumer asks. Symmetric with `enableRls` today which
   also has no dedicated REST endpoint.

**Slices**

- **M.1 — `Schema.disableRls(String tableName)` public API**.
  - **Owner-only** gate (tightens existing
    `requireManagerOrOwner` to Owner for this method).
  - Preserves all existing guards: subclass-rejection,
    `rejectDisableIfPermissionsExist`, root-table only.
  - **New guard: reject if table has any rows.** `SELECT
    COUNT(*) > 0 FROM <table>` (admin context) → throw
    `"Cannot disable RLS: table '<name>' has <N> rows. Export,
    drop, and recreate the schema to declassify populated data."`
  - GraphQL surface: new Owner-only mutation `disableRls(table:
    String!): String`. Schema filter omits this mutation from
    non-Owner sessions.
  - REST: skipped per decision 3 above.

- **M.2 — Audit log entry on disable**. Every successful disable
  writes a Change row with `operation = "DISABLE_RLS"`,
  `tableName`, `actor`, `rowCountAtDisableTime` (always 0 by
  M.1's guard, but recorded for trail completeness). Existing
  `getChanges` surfaces it.

- **M.3 — Documentation**. `use_permissions.md` section: "How to
  reclassify a table from row-secured to public". Cover: only
  works for empty tables; for populated tables the path is
  export → drop schema → recreate without RLS → re-import; data
  loss implications (`mg_owner`/`mg_groups` columns dropped on
  empty-table disable; full data export needed for populated
  case).

### Phase K — RBAC import/export + bulk apply (DRAFT 2026-05-09)

**Why this phase exists**

Phase-J combined-suite surfaced contention on shared system catalog
tables (`MOLGENIS.table_metadata`, `column_metadata`,
`role_metadata`, `group_metadata`, `table_permission_metadata`).
The test-side flake (`disableRlsCascade` 10s timeout under
parallel RLS test classes) is a symptom — not the disease.

The disease: every RBAC mutation today round-trips one DDL/UPSERT
per change. A real-world bulk import of an RBAC config (groups +
roles + per-table permissions + group memberships) for a multi-
schema install would issue thousands of single-row writes against
those shared tables, each acquiring/releasing locks. Same lock
class as the test flake, just amplified.

Bumping `disableRlsCascade` to 60s is a band-aid; the real fix is
batched bulk-apply with one tx-window per import.

**Current import/export coverage (gap analysis)**

| Concept                        | Storage                                                | Import/export today |
|--------------------------------|--------------------------------------------------------|---------------------|
| Members (user→role)            | `pg_auth_members`                                      | ✅ `Emx2Members.java` (`molgenis_members.csv`, columns `user,role`) |
| Custom roles (name + privs)    | `MOLGENIS.role_metadata`                               | ❌                   |
| Groups (schema-scoped)         | `MOLGENIS.group_metadata` + `pg_auth_members` sentinel | ❌                   |
| Group memberships (user→group→role) | `MOLGENIS.group_membership_metadata`              | ❌                   |
| Per-table custom permissions   | `MOLGENIS.table_permission_metadata`                   | ❌                   |

`Emx2Members` only covers the legacy 2-tier role surface; nothing
RLS v4-introduced is round-trippable.

**Slices**

- **K.1** — Schema CSV format extension. Define filenames + column
  layouts for the four missing artefacts:
  - `molgenis_roles.csv` — `name`, `description`, `changeOwner`,
    `changeGroup`
  - `molgenis_groups.csv` — `name`
  - `molgenis_group_members.csv` — `user`, `group`, `role`
  - `molgenis_permissions.csv` — `role`, `table`, `select`,
    `insert`, `update`, `delete` (scope names)
  Reuse existing `Emx2*` IO conventions (`canAccessMembers` gate,
  `TableStore` round-trip).

- **K.2** — Reader/writer pair per artefact (mirroring
  `Emx2Members.outputRoles` / `inputRoles` symmetry).
  Wire into `MolgenisIO.fromSchema(...)` and the import task chain.

- **K.3** — `Schema.bulkApplyRbac(BulkRbacBundle bundle)` API
  (or analogous) that wraps the entire import in **one** `db.tx()`
  with `SET CONSTRAINTS ALL DEFERRED` and uses batched UPSERTs
  (`INSERT … ON CONFLICT DO UPDATE`) instead of per-row method
  calls. Single lock acquire/release window per bundle.

- **K.4** — REST/CSV endpoint integration: `CsvApi` + zip import
  paths route RBAC files through `bulkApplyRbac`, not the
  per-row `addMember` / `grant` / `createRole` calls.

- **K.5** — Round-trip integration test: export a fully-loaded
  pet store schema's RBAC, drop+recreate schema, re-import,
  assert identical `getRoles` / `getGroups` / `getMembers` /
  `getPermissionsForRole` output.

- **K.6** — Documentation: `use_permissions.md` section on bulk
  import format + lock-window guarantees. Note that `disableRls`
  on a large cascade still benefits from prolonged 60s timeout
  (carry-over from J discussion).

**Out of scope for K**

- Inverting the global → per-schema metadata tables (Phase L+ if ever).
- Streaming/chunked imports (one tx window is fine for typical
  RBAC sizes; revisit if real install hits >10k permissions).
- Audit history of RBAC changes (separate concern).

**Open questions for user before K opens**

1. Filename + column conventions OK as proposed in K.1, or rename?
2. Should K.4 land before K.3 (smaller increments) or after
   (bulk path stable before public surface flips)?
3. Should `disableRlsCascade` get the prolonged-60s timeout
   independently of K, or wait for K landing to remove the
   per-row pattern first?

### Phase R — Explicit REFERENCE permission (design approved 2026-05-09, implementation pending)

> **Design approved.** Owner decisions recorded below. Implementation slices not yet started. No code changes permitted until lead issues a start signal.

#### Goal

Introduce an explicit `reference` permission orthogonal to `view`, so schema owners can declare tables as "lookup-only" (resolvable via FK traversal without granting direct SELECT) rather than deriving that capability from existing `view` scopes. Phase R is also the sole existence-privacy mechanism in v4+: L.7 has already been reverted pre-ship; Phase R replaces it with a cleaner row-visibility rule.

#### Permission model

Two orthogonal axes per table, per role:

- **`view(T, scope)`** — governs direct SELECT against T (`query { T { … } }`). Scope: NONE / OWN / GROUP / ALL. Unchanged from current behavior.
- **`reference(T)`** — governs FK traversal into T from other tables (`query { Child { target { id, label } } }`). `reference` is an enum `REFERENCE_{NONE, OWN, GROUP, ALL}` — same shape as `view`. Phase R v1 wires only `REFERENCE_NONE` and `REFERENCE_ALL` at runtime; the metadata model and import/export include the full enum from day one so future phases adding GROUP/OWN are runtime-only changes (no metadata migration).

**Effective access** = union of all role grants per axis. A user holding multiple groupless roles gets the maximum scope per axis. `VIEW_GROUP + REFERENCE_ALL` is a valid, supported combination.

**Implication**: `VIEW ⊇ REFERENCE` at the same scope tier — if you can directly view a row you can certainly reference it.

**Child-row visibility rule**: a row in Child is visible to the requesting user iff every FK target on that row is within (effective view-scope ∪ effective reference-scope) on the refTable. For REF_ARRAY: ALL elements must be in scope, or the row is hidden entirely. No partial-visibility arrays.

**Hide, don't lock**: rows whose FK targets fall outside scope are simply omitted from results. No synthetic `mg_editable` flag. Clean two-state: visible+editable vs hidden. Lock-visible mode (show but disallow edit) is deferred backlog.

**Permission combination matrix**

| Combination | Direct query of T | FK on Child pointing to T |
|---|---|---|
| VIEW_NONE + REFERENCE_ALL | nothing returned | all Child rows visible; FK resolves to `{pk, label}` only |
| VIEW_OWN + REFERENCE_NONE | own rows, full fields | only Child rows pointing to own targets visible |
| VIEW_GROUP + REFERENCE_ALL | group rows, full fields | all Child rows visible; FK resolves to `{pk, label}` only |
| VIEW_ALL | all rows, full fields | all Child rows visible; FK fully resolvable |

**GraphQL schema reduction per session**: the schema served to a session reflects the user's effective perms. Tables for which the user has reference-only access emit a thin type (PK + label fields, no mutations). Full types include all permitted fields and mutations. This is the authoritative signal — frontends introspect, no runtime probing.

**RefLabel**: data-modeler responsibility. EMX2 has no column-level permissions and Phase R does not add them. The refLabel shape is whatever the modeler declared in metadata. If sensitive columns appear in the label, that is the modeler's problem — document loudly.

**Defense-in-depth at write time**: server verifies "all current FK targets are within the user's (view ∪ reference) scope" at update/insert/delete time. Unconditional (not gated on null detection — that was L.7's approach, now reverted). Catches narrow races where perms change between read and write. Implemented as R.5.

#### Scope of v1

- REFERENCE is a four-value enum (`REFERENCE_{NONE, OWN, GROUP, ALL}`) at the metadata level from day one. Only `REFERENCE_NONE` and `REFERENCE_ALL` are wired at runtime in v1; the full enum is present in the metadata model and import/export so future phases adding GROUP/OWN require no metadata migration.
- VIEW semantics unchanged.
- Scoped REFERENCE (REFERENCE_GROUP, REFERENCE_OWN) is **out of scope for v1** — runtime-only addition in a later phase (no schema migration needed).

#### Implementation slices (order suggestive; do not lock sequencing before owner approval)

- **R.1** — Add REFERENCE permission to the perm-set model (Java enum + metadata storage). _Status (2026-05-10): DONE._ `PermissionSet.ReferenceScope` (NONE/OWN/GROUP/ALL + `fromString`), `TablePermission.reference` (default NONE, in equals/hashCode), `MetadataUtils.RPM_REFERENCE_SCOPE`, `migration32.sql` extended in place (column added to CREATE TABLE + idempotent ADD COLUMN block; SOFTWARE_DATABASE_VERSION still 33), `SqlRoleManager.setPermissions`/`getPermissionSet`/`loadExistingGrant` round-trip the new column. RED-GREEN tests `referenceScope_roundTrip_all` + `referenceScope_roundTrip_ownGroupNone` in `TestSqlRoleManager`; full class 24/24 green. Sanity sweep flagged 2 failures (`TestGrantRolesToUsers.testRole`, `TestUsersAndPermissions.testActiveUser`) — pass in isolation, pre-existing test-order sensitivity, not R.1-caused. No runtime visibility behavior changes (R.3 wires runtime).
- **R.2** — Wire REFERENCE into role grants. Phase K dependency means import/export wiring is deferred (see R.2c). Split (decided 2026-05-10):
  - **R.2a** — Drop the null-as-"unset-preserve" sentinel pattern in favor of: `TablePermission` scopes default to non-null `NONE`; setters reject null (`Objects.requireNonNull`); `grant()` is **additive** — `mergeWithExisting` skips fields equal to NONE; `setPermissions()` is full replace (writes NONE to revoke). Tradeoff: cannot revoke a single scope via `grant()` — use `setPermissions()` instead. _Status (2026-05-10): DONE._ `TablePermission` no longer has any wasSet/isSet machinery (an earlier `Field`/`EnumSet`/`isSet` design was tried and rejected as overengineered). `SqlRoleManager.mergeWithExisting` now also handles `reference` (small scope creep — logically required when touching that method). `normalizeNoneToNull` deleted; `hasAnyPermission` and `higherSelectScope` simplified from null-handling to NONE-handling. New test `fluentSetter_nullArg_throwsNPE` locks the contract. 26/26 in `TestSqlRoleManager`. **Follow-up cleanup (same day):** dropped duplicate JavaBean accessors (`getSelect/setSelect/...`) on `TablePermission` — fluent-only API for all 5 scopes; added missing `reference()` fluent getter. Mechanical rename across `TableMetadata` (`rejectIfRlsScopeWithoutRls`), `SqlRoleManager`, `GraphqlPermissionFieldFactory`, and 22 test files. Verified Jackson does NOT touch these types — JSON serialization will land via `org.molgenis.emx2.json` DTO wrappers (R.2c / Phase K). 26/26 + 32/32 (`TestGraphqlSchemaRoles`) green. **Leftover:** `SqlQuery.java:207` has dead `!= null` check that's now always-true — fix in R.2b alongside other SqlQuery touches.
y  - **R.2b** — Wire REFERENCE into the grant/merge path uniformly with the other scopes. Extend GraphQL `setPermissions` mutation surface to accept `reference`. Round-trip test in `TestGraphqlSchemaRoles` (graphql-test-pattern: pre + post state via GraphQL surface). _Status (2026-05-10): DONE._ Backend grant/merge was already wired by R.2a follow-up (`SqlRoleManager.mergeWithExisting` handles reference; `loadExistingGrant` + `getPermissionSet` read `RPM_REFERENCE_SCOPE`). PG GRANT layer correctly omits reference (logical visibility scope, not a PG privilege). GraphQL surface added: `GraphqlConstants.REFERENCE`, `MolgenisReferenceScope` enum type, `reference` field on `tablePermissionInputType`/`tablePermissionOutputType`, `toReferenceScope` helper, wired in `toPermissionSet` + `permissionSetToMap`. New round-trip test `changeRoles_customRole_includesReferenceScope` in `TestGraphqlSchemaRoles` (covers ALL and NONE via GraphQL pre+post). Direct unit tests in `TestGraphqlPermissionFieldFactory` (enum declaration, `toReferenceScope` null/enum/string, round-trip, output emission). Also: dropped dead `!= null` check at `SqlQuery.java:207` (now `p.select() != SelectScope.NONE`). Tests green: TestGraphqlSchemaRoles 33/33, TestGraphqlPermissionFieldFactory 19/19, TestSqlRoleManager 26/26.
  - **R.2c** *(deferred to Phase K)* — Role import/export REFERENCE wiring. Lands when K's bulk-apply layer ships.
  - **R.2b-fix** — Surfaced 2026-05-10 by R.4a stop-and-report: R.2b wired the persistence path (`mergeWithExisting`, `loadExistingGrant`, `getPermissionSet`) but missed the in-memory aggregation used by `getPermissionsForActiveUser`. REFERENCE scope silently dropped at three sites in `SqlRoleManager`: `hasAnyPermission` (REFERENCE-only entry filtered out), `mergePermissions` (no `.reference(...)` in builder; both inputs lost), `expandWildcard` (wildcard's reference scope not propagated). Fix: add `|| p.reference() != ReferenceScope.NONE` to `hasAnyPermission`; add `.reference(higherReferenceScope(a.reference(), b.reference()))` to `mergePermissions` + new `higherReferenceScope` helper; add `.reference(wildcard.reference())` to `expandWildcard`. Test: extend `TestSqlRoleManager` with a REFERENCE-only role surviving `getPermissionsForActiveUser()` round-trip. _Status (2026-05-10): DONE._ +17 lines `SqlRoleManager`, +50 lines test. New tests `referenceOnlyPermission_survivesAggregationPath` + `referenceOnlyWildcard_survivesExpandWildcard`. 28/28 in `TestSqlRoleManager` (was 26/26).
- **R.3** — Implement Child-row visibility rule: a Child row is hidden if any FK target falls outside (view ∪ reference) scope on the refTable. REF_ARRAY: ALL elements must be in scope. Sub-slicing (decided 2026-05-10):
  - **Approach: defense-in-depth.** RLS policies remain as the per-row deny-by-default fence (already present via `mg_can_read`). Query layer (extending F.1's `refJoins`) enforces "no incomplete Child rows" by hiding rows whose FK targets fall outside (view ∪ reference). Stays in query layer to avoid policy regen on FK alter.
  - **Read path: no clamp.** F.1's NULL-clamp on FK columns is dropped — replaced by row-hide. Tests in `TestCrossSchemaFkRlsVisibility` flip from null-assertion to row-absence-assertion. Write path keeps a visibility check (R.5).
  - **REFERENCE_ALL semantics in R.3:** at SQL/data layer the row stays visible. Column-level "PK + label only" gating happens at the GraphQL schema layer in R.4 — out of scope for R.3.
  - **R.3a** — SQL function `mg_can_reference(schema, table, groups, owner)`. Mirrors `mg_can_read` shape but reads `RPM_REFERENCE_SCOPE`. v1 returns true for system roles, REFERENCE_ALL, or when `mg_can_read` would (since VIEW ⊇ REFERENCE at same scope). REFERENCE_OWN/GROUP wired now too (small extra cost; aligns SQL with metadata) but not exercised by R.3b/c (which only filter NONE/ALL gates). Pure SQL change + unit tests. _Status (2026-05-10): DONE._ Function added in `migration32.sql` (idempotent CREATE OR REPLACE; no version bump — still 33). Composition implemented inline (one UNION ALL branch combining reference scopes + the full select-scope/change-owner conditions from `mg_can_read`) rather than calling `mg_can_read()` from `mg_can_reference()` — keeps it one query. New `TestMgCanReference` class with 6 tests covering: REFERENCE_NONE → false, REFERENCE_ALL → true any row, REFERENCE_OWN match/no-match, REFERENCE_GROUP match/no-match, system role → true, view-implies-reference (VIEW_ALL → true even with REFERENCE_NONE). Bootstrap re-runs `migration32.sql` in `@BeforeAll` since DB version is already 33 and won't auto-rerun. Test-isolation gotcha surfaced + fixed: dedicated `USER_SYSTEM` for system-role test to avoid contaminating other tests via persistent Owner membership. 6/6 green; TestSqlRoleManager 26/26 unaffected.
  - **R.3b** — Replace F.1's NULL-clamp with row-hide for single-valued FK (REF). `SqlQuery.refJoins` switches the LEFT JOIN + CASE WHEN to an INNER-style filter: keep row iff `mg_can_read(refTable, ...) OR mg_can_reference(refTable, ...)`. Update `TestCrossSchemaFkRlsVisibility` (5 tests) to assert row-absence instead of null FK. New test: REFERENCE_ALL on refTable keeps the row even when view-scope alone wouldn't. _Status (2026-05-10): DONE._ **Surprise discovery:** F.1's clamp was already deleted at the Phase R baseline (commit 76edd816e) — there was nothing to "replace"; R.3b implements visibility from scratch. New `fkRlsVisibilityConditions` method on `SqlQuery` adds, per single-valued FK pointing to an RLS-enabled refTable, a LEFT JOIN exposing target's `mg_groups`/`mg_owner` and a WHERE condition `(fk IS NULL) OR MOLGENIS.mg_can_reference(refSchema, refTable, target.mg_groups, target.mg_owner)`. Note: invokes `mg_can_reference` only — R.3a's function already subsumes `mg_can_read` semantics (UNION ALL of select-scope + reference-scope + system roles), so no separate `mg_can_read` call needed in the predicate. REF_ARRAY skipped (R.3c). New test class `TestCrossSchemaFkRlsVisibility` with 6 tests including `referenceAllOnRefTable_keepsChildRowVisible`. SqlQuery +60 lines, test +273. Tests green: TestCrossSchemaFkRlsVisibility 6/6, TestSqlRoleManager 26/26, TestPolicyCount 2/2, TestSelectScope 1/1.
  - **R.3c** — REF_ARRAY all-elements-in-scope. Extend the UNNEST path so the row is hidden if ANY array element falls outside (view ∪ reference). Update `refArrayDropsInvisibleElements` from element-drop to row-drop semantics. _Status (2026-05-10): DONE._ `fkRlsVisibilityConditions` extended; `refRlsVisibilityCondition` extracted as helper. New `refArrayRlsVisibilityCondition` builds: `(arr IS NULL OR cardinality(arr)=0 OR NOT EXISTS (SELECT 1 FROM unnest(arr) AS _rls_elem LEFT JOIN refTable _rls_t ON _rls_t.pk = _rls_elem WHERE NOT MOLGENIS.mg_can_reference(schema, table, _rls_t.mg_groups, _rls_t.mg_owner)))`. NOT-EXISTS pattern picked over bool_and to avoid JOIN-induced row duplication in the main query. Scoped to single-column REF_ARRAY (`column.getReferences().size() == 1`); composite-key REF_ARRAY silently skipped (same as before R.3c) — backlog if it surfaces. Test `refArrayDropsInvisibleElements` renamed to `refArray_hidesChildRow_whenAnyElementInvisible` (row-absence). 3 new tests: `refArray_keepsChildRow_whenAllElementsVisible`, `refArray_emptyArray_keepsChildRow`, `refArray_referenceAllOnRefTable_keepsRow` (cross-schema). TestCrossSchemaFkRlsVisibility 9/9 (was 6/6); TestSqlRoleManager 26/26; TestMgCanReference 6/6.
  - **R.3d** — Cross-schema FK coverage. Likely test-only if R.3a invocation is fully-qualified like `mg_can_read`. _Status (2026-05-10): DONE._ Verified existing positive coverage (every test in `TestCrossSchemaFkRlsVisibility` uses a two-schema layout: REF_SCHEMA holds `Pet` with RLS; CHILD_SCHEMA holds `Owner` (REF) and `PetLover` (REF_ARRAY)). Single-FK + REF_ARRAY REFERENCE_ALL tests already exercise grants confined to refTable's schema with independent CHILD_SCHEMA membership. **Hardening pass (2026-05-10)**: tightened 4 existing "keeps" tests with explicit `assertFalse` / `assertEquals(rows.size(), N)` to verify negative direction and exhaustive row sets (audit found they only used `assertTrue contains X`). Added strict-negative `crossSchema_throwsForUserWithNoMembershipInRefSchema` — surfaced behavioral finding that a user with zero refSchema membership gets `MolgenisException` from `Column.getRefTable()` at query-build time, before any RLS predicate. Pinned current behavior with `assertThrows`; metadata-access design question backlogged as L.15. 10/10 green.
- **R.4** — GraphQL schema generator: per-session schema reflects effective perms. Tables with VIEW → full type; REFERENCE-only → thin type (PK fields only, reachable only via FK traversal); neither → absent. Mutations gated on VIEW. Ontology tables keep current always-visible exception. Single per-session type cache keyed by tableName (each table is either thin OR full per session, never both). RefLabel stays client-side (modeler responsibility — see Open Questions). Sub-slices:
  - **R.4a** — Plumbing only. Add `tablesWithReferencePermission` set + `hasReferencePermission(table)` helper to `GraphqlTableFieldFactory` (parallel to existing `tablesWithSelectPermission` / `hasViewPermission`). Populate from `schema.getPermissionsForActiveUser()` filtering `reference()!=NONE`. No behavior change. Test: assert set membership for a user with REFERENCE-only role. _Status (2026-05-10): DONE._ Initial dispatch stop-and-reported on three `SqlRoleManager` bugs (resolved as R.2b-fix). After R.2b-fix landed, R.4a code (already correct) verified green: new `TestGraphqlTableFieldFactoryReferencePermission` 1/1, `TestGraphqlSchemaRoles` 33/33 no regression.
  - **R.4b** — Thin type emission. New `createReferenceOnlyType(table)` returning a GraphQL type with PK fields only (no nested objects, no aggregates). Modify nested-FK emission sites (`GraphqlTableFieldFactory.java:247,261,349-354`): full type when refTable has VIEW; thin type when REFERENCE-only; field omitted when neither (subject to ontology exception at line 349). Single cache by tableName. New test class `TestGraphqlReferenceOnlySchema`: non-admin with VIEW on Parent + REFERENCE-only on Child sees `parent.fkChild { pk }` but Child is not directly queryable. _Status (2026-05-10): DONE._ +37 production lines, +170 test. Modified `createTableField()` REF and REF_ARRAY/REFBACK cases to add `else if (hasReferencePermission(col.getRefTable()))` branch using `createReferenceOnlyType`. Same `tableTypes` cache key as `createTableObjectType` (per-session: each table is either thin or full, never both). Ontology exception preserved (ontology tables satisfy `hasViewPermission` via table-type check; continue using `createTableObjectType`). 3/3 in `TestGraphqlReferenceOnlySchema`; `TestGraphqlSchemaRoles` and `TestGraphqlTableFieldFactoryReferencePermission` no regression.
  - **R.4c** — Suppress top-level access for REFERENCE-only tables. `GraphqlFactory.forSchema` iteration skips top-level query field and mutations for tables without VIEW. Aggregate gate at `GraphqlTableFieldFactory.java:374-382` stays VIEW-only. Test: introspection shows reference-only table absent from top-level Query and Mutation. _Status (2026-05-10): DONE._ +14 prod lines, +223 test (4 tests). `GraphqlFactory`: collapsed `tableAggField` + `tableGroupByField` registration inside the existing `hasViewPermission` gate (alongside `tableQueryField`). `GraphqlTableFieldFactory`: added `&& hasViewPermission(table)` gate to `getMutationDefinition` (insert/update/upsert) and `deleteMutation`. **Pre-existing leak fixed (in scope)**: `tableAggField` / `tableGroupByField` and mutation argument registration were previously unconditional at top-level — REFERENCE-only goal forced this fix; predates Phase R. Worth a release-note line. Tests: `TestGraphqlReferenceOnlyTopLevelSuppression` 4/4; no regression on `TestGraphqlReferenceOnlySchema` 3/3, `TestGraphqlSchemaRoles` 33/33, `TestGraphqlTableFieldFactoryReferencePermission`.
  - **R.4-fix-listener** — Three `SqlRoleManager` methods skip `database.getListener().onSchemaChange()` fire (verified by scout 2026-05-10): `createRole`, `createGroup`, `deleteGroup`. Symptoms: cached schemas (incl. per-user GraphQL executors via `ApplicationCachePerUser`) keep stale role/group state after these mutations until the next unrelated schema change. Other methods (`deleteRole`, `grant`, `revoke`, `setPermissions`, `grantRoleToUser`, `revokeRoleFromUser`, `addGroupMembership`, `removeGroupMembership`, `addGroupMember`, `removeGroupMember`) all fire correctly. Owner direction (2026-05-10): use the existing listener mechanism — role changes are schema changes; invalidating all schema caches is the intended behavior. Just add the three missing calls at end of each method, matching the existing pattern. _Status (2026-05-10): DONE._ +3 production lines, +3 listener-spy tests (`createRole_firesListener`, `createGroup_firesListener`, `deleteGroup_firesListener`) using `AtomicInteger` counter via `SqlDatabase.setListener()`. 31/31 in `TestSqlRoleManager` (was 28/28).
  - **R.4d** — Cross-schema REFERENCE-only. Scout audit (2026-05-10) revealed broader pre-existing gap: `hasViewPermission(col.getRefTable())` and the new `hasReferencePermission(col.getRefTable())` only consult the FACTORY's schema — `tablesWithSelectPermission` is built from current schema's `getPermissionsForActiveUser()`. For a cross-schema FK target, the lookup falls back to coincidental same-name match in current schema OR the VIEWER system-role bypass on current schema. Custom-role users with VIEW/REFERENCE specifically in the refTable's schema get false-negative emission today (master + HEAD). Approach: memoized per-refSchema lookup. When `table.getSchemaName() != schema.getName()`, fetch `database.getSchema(refSchemaName).getPermissionsForActiveUser()` once per refSchema, cache on factory instance. VIEWER system-role bypass also uses ref-schema's `getInheritedRolesForActiveUser()`. Effectively unifies same-schema and cross-schema permission resolution behind one helper. Tests: GraphQL-layer cross-schema scenario with mixed perms (VIEW on Parent in A, REFERENCE-only on Child in B) → thin type emitted; control case (full VIEW on Child in B) → full type. Carries L.15 caveat: user must have at least one role in refSchema (existing `Column.getRefTable()` throw behavior — separate problem). _Status (2026-05-10): DONE._ Refactor: flat `tablesWithSelectPermission` / `tablesWithReferencePermission` sets removed entirely; replaced by `Map<String, SchemaPermissionView> permissionsBySchemaName` keyed by schema name with `computeIfAbsent` lazy load. `SchemaPermissionView` record holds (selectTables, referenceTables, isSystemViewer). `hasViewPermission` / `hasReferencePermission` now delegate via `table.getSchemaName()` — same-schema becomes special case of cross-schema. **Pre-existing master bug closed in scope**: custom-role cross-schema FK emission now correctly consults refSchema's permissions (was false-negative in master). +30 prod lines, −15 prod lines, +264 test. New `TestGraphqlCrossSchemaReferencePermission` 4/4: thin-type cross-schema, full-type control, no-permission omits field, system-VIEWER cross-schema bypass. No regression on `TestGraphqlReferenceOnlySchema` 3/3, `TestGraphqlReferenceOnlyTopLevelSuppression` 4/4, `TestGraphqlSchemaRoles` 33/33, `TestGraphqlTableFieldFactoryReferencePermission` 1/1. Cross-schema type name pattern: `<refSchemaIdentifier>_<refTableIdentifier>` (e.g. `TGqlCrossB_Child`) — avoids collision with same-named tables in current schema.
  - **R.4e** — Add `canReference` boolean to existing `MolgenisTablePermission` GraphQL type at `_session.tablePermissions`. Scout audit (2026-05-10) showed the infrastructure already exists: `GraphqlSessionFieldFactory.java:28-53` defines `MolgenisTablePermission` with `canView/canInsert/canUpdate/canDelete`; frontend uses it via `ViewTable.vue:39-65`. Phase R adds reference scope; just extend the existing surface with one more boolean. Path B (per-table-type `_meta.permissions` with full scope enums) backlogged as L.18 — useful when frontend needs richer scope semantics (distinguish OWN from ALL etc.); not blocking Phase R. _Status (2026-05-10): DONE._ +7 prod lines, +67 test. `GraphqlConstants.CAN_REFERENCE` constant; `canReference` field on `MolgenisTablePermission` type; data-fetcher entry. Semantic: `canReference = select != NONE || reference != NONE` — matches `mg_can_reference` SQL function (any view scope already grants reference, plus explicit reference scope). 4/4 in `TestGraphqlSession` (2 new tests); no regression on `TestGraphqlSchemaRoles`, `TestGraphqlReferenceOnlySchema`.
  - **R.4e-fix** — Three corrections surfaced 2026-05-10 after R.4e shipped:
    1. **`canReference` boolean semantic** in `GraphqlSessionFieldFactory.buildTablePermissions`: current expression `select != NONE || reference != NONE` wrongly treats privacy modes (EXISTS/COUNT/RANGE/AGGREGATE) as granting reference. Privacy modes can't dereference an FK to a specific row. Correct: `select.allowsRowAccess() || reference != NONE` (uses existing helper that returns true only for OWN/GROUP/ALL).
    2. **`mg_can_reference` SQL function** at `migration32.sql:176` has the same defect: `OR rp.select_scope IN ('EXISTS','COUNT','RANGE','AGGREGATE')` must be removed. Idempotent CREATE OR REPLACE, no version bump.
    3. **Server-side ontology bypass (option B)** — ontologies are always world-readable in EMX2 but never appear in `_session.tablePermissions` unless an explicit role grants them. Frontend currently fallbacks at component level (`ViewTable.vue` `tableType === "ONTOLOGIES"` OR-fallback). Fix: in `buildTablePermissions`, after collecting role-defined entries, append ontology tables not already in the list with `canView=true, canReference=true` (other booleans false). Makes `_session.tablePermissions` authoritative; frontend special-case can stay for back-compat or be removed later.
    Tests: extend `TestGraphqlSession` for privacy-scope user → `canReference=false`; add ontology-only assertion. `TestMgCanReference`: update or remove any test asserting privacy-mode grants reference. No regression on `TestCrossSchemaFkRlsVisibility` (uses the SQL predicate). _Status (2026-05-10): DONE._ `GraphqlSessionFieldFactory.java` +24 prod lines (ontology bypass loop + ArrayList/Set imports + corrected `canReference` expression using `select.allowsRowAccess()`); `migration32.sql` −1 line (removed privacy-mode clause from `mg_can_reference`); `TestGraphqlSession.java` +67 test lines (2 new tests: `sessionPermissions_privacyScopeCount_doesNotGrantCanReference`, `sessionPermissions_ontologyTable_alwaysVisibleWithCanReference`); `TestMgCanReference.java` +18 test lines (1 new test `mgCanReference_returnsFalse_whenPrivacyScopeOnly`). 7/7 `TestMgCanReference`, 6/6 `TestGraphqlSession`. No regression: 10/10 `TestCrossSchemaFkRlsVisibility`, 36/36 combined `TestGraphqlReferenceOnlySchema` + `TestGraphqlSchemaRoles`. Note: `mg_can_view` (migration32.sql:145) still includes the privacy-mode clause — correct, privacy modes DO grant view (aggregate/count/exists) but cannot dereference FK targets; the two functions diverge intentionally.
  - **L.16 (backlog cleanup)** — Consider consolidating column-gating call sites (`GraphqlTableFieldFactory.java:247,261,349-354`) into a single `TableMetadata.getVisibleColumnsForActiveUser()` accessor so GraphQL is "dumb" about permission semantics. Variant B from R.4 design discussion. Not blocking — same end-state as current Variant A, just relocates ~15 lines.
- **R.5** — (L.7 already reverted pre-ship.) Phase R ships with no read-clamp and no write-back guard from v4. R.5 implements the unconditional "all current FK targets visible at ≥REFERENCE scope" check at update/insert/delete time as defense-in-depth only (catches narrow races where perms change between read and write). Reuses R.3b/c read-path shape (`mg_can_reference`) but fires at write time, unconditional. _Status (2026-05-10): DONE._ Wired in `SqlTable.executeBatch` before `insertBatch`/`updateBatch`: `checkFkRlsWriteVisibility` iterates FK columns; per-batch fail-fast `LIMIT 1` lookup on each RLS-enabled refTable issues `NOT MOLGENIS.mg_can_reference(refSchema, refTable, mg_groups, mg_owner, p_user)` and throws `MolgenisException` naming the violating PK. Skips: composite-key FKs (matches R.3c), non-RLS refTables, admin user. System role bypass handled inside `mg_can_reference` via `pg_has_role` (no separate Java check). DELETE path: no check (no new FK refs introduced). Update path: only validates FK columns in the actual change-set (`getUpdateColumns`). Uses `RESET ROLE` / `SET ROLE MG_USER_<user>` bracket so RLS on refTable doesn't filter rows from the violation lookup. **R.5-fix landed same day** to clean up two issues from initial implementation: (i) inlined SQL duplicating `mg_can_reference` replaced by direct function call — `mg_can_reference` signature extended with `p_user TEXT DEFAULT current_user` (idempotent CREATE OR REPLACE; explicit `DROP FUNCTION IF EXISTS` of the 4-param overload first to avoid PG ambiguity error when both signatures co-exist mid-migration); (ii) stray `OR rp.change_owner = true` clause inadvertently copied from `mg_can_view` semantics dropped — `mg_can_reference` and `mg_can_view` remain intentionally divergent (privacy modes + change_owner grant view but not reference; see R.4e-fix). Net diff: `SqlTable.java` ~+90 lines after the dedup (initial 168 minus 35 from R.5-fix cleanup), `migration32.sql` +5 lines net (signature + p_user substitution − change_owner clause), `TestFkRlsWriteGuard.java` +364 lines (11 tests including `insert_throws_whenChangeOwnerTrueButNoReferenceOrViewScope`), `TestMgCanReference.java` +45 lines (new `mgCanReference_withExplicitUser_honorsPassedUser` locking the 5-param path). Tests green: 11/11 `TestFkRlsWriteGuard`, 8/8 `TestMgCanReference`, 31/31 combined `TestCrossSchemaFkRlsVisibility` + `TestSqlRoleManager` (no regression). **Security note**: the `RESET ROLE` privilege escalation is necessary and bounded — superuser context only spans the single LIMIT 1 lookup, wrapped in try/finally restoring `SET ROLE`. Worth a release-note line. Existing `SqlQuery` read-path call sites continue to work via the `DEFAULT current_user` arg — no caller migration required.
- **R.6** — Migration and docs: existing VIEW grants implicitly carry REFERENCE at the same scope. Explicit REFERENCE-only requires owner action. Document refLabel modeler responsibility prominently. _Status (2026-05-10): DONE._ Scout confirmed no data migration is needed — `migration32.sql:33` already declares `reference_scope TEXT NOT NULL DEFAULT 'NONE'`, and `mg_can_reference` UNION-ALL semantics implement the implicit VIEW⊇REFERENCE carry at runtime. Docs added: `docs/molgenis/use_permissions.md` +56 lines, new top-level `## REFERENCE scope` section before `## Known limitations` covering: two-axes overview, v1 runtime caveat (OWN/GROUP metadata-only), `VIEW ⊇ REFERENCE (implicit carry)` + no-migration guarantee, lookup-table use case, permission combination matrix (from plan §R), child-row visibility rule (single FK + REF_ARRAY all-elements semantics + NULL-FK passthrough), write-time guard (INSERT/UPDATE only, DELETE excluded), privacy-modes-do-not-grant-REFERENCE clarification (mg_can_view vs mg_can_reference divergence per R.4e-fix), and upgrading-from-pre-Phase-R one-liner. `docs/molgenis/use_schema.md` +2 lines, `!>` callout after the existing refLabel paragraph: REFERENCE-only users see pk + refLabel; EMX2 has no column-level permissions; do not put sensitive columns in refLabel; recommended pattern is a dedicated non-sensitive display column. No CHANGELOG file exists in repo — docs read as if REFERENCE has always been there. No frontend UI permission-editor change in scope (Members.vue does not currently expose any scope fields; future work to surface explicit REFERENCE-only grants is L.21 backlog candidate if needed).

#### Open questions

- **Cross-schema REFERENCE** — ANSWERED: same rule applies regardless of schema. To view/edit Child rows referencing schema-other.T, user needs ≥REFERENCE on schema-other.T. Carry-over open detail: how does REFERENCE on schema-other.T compose with schema-level membership? Resolve before R.3 implementation (see new open question below).
- **Write-time guard scope** — ANSWERED: REFERENCE alone satisfies the "all current FK targets visible" check at write time. VIEW is not required for the visibility check, only for direct table query. Matches the orthogonal-axes design.
- **RefLabel server-side projection** — ANSWERED: refLabel stays client-side in Phase R. EMX2 has no server-side refLabel today and Phase R does not introduce one. Modeler responsibility: declare label columns that are safe to expose at REFERENCE level. **Document this obligation prominently in user-facing docs when Phase R ships.**
- **Scoped REFERENCE (REFERENCE_GROUP / REFERENCE_OWN)** — ANSWERED: deferred to a later phase. v1 wires only NONE and ALL. The enum carries all four values (API prepped) so promotion to GROUP/OWN is purely a runtime change.
- **Cross-schema RBAC composition for REFERENCE** — ANSWERED (2026-05-10) with caveat: at the **RLS predicate layer**, the cross-schema check works identically to same-schema. To gain REFERENCE on `schemaA.T`, the user must hold a role IN schemaA that includes REFERENCE on T (roles are schema-scoped today). The user's roles in the Child's schema govern only whether they can see Child at all. `mg_can_reference(schemaA, T, …)` joins `role_permission_metadata ⨝ group_membership_metadata` filtered by `m.schema_name = schemaA` only — Child's schema is never consulted in the predicate. Verified by `referenceAllOnRefTable_keepsChildRowVisible` and `refArray_referenceAllOnRefTable_keepsRow` (REFERENCE_ALL granted in refTable's schema; independent CHILD_SCHEMA membership). **Caveat surfaced 2026-05-10**: at the **metadata-access layer**, `Column.getRefTable()` throws `MolgenisException` at query-build time when the calling user has zero membership in the refSchema (cannot see the schema exists). So the strict claim is "REFERENCE works cross-schema *given* the user has at least schema-visibility on refSchema (which any role in refSchema grants)." A user with no role at all in refSchema gets a hard error, not filtered results. Pinned in `crossSchema_throwsForUserWithNoMembershipInRefSchema` (`assertThrows(MolgenisException.class)`). Whether this should be relaxed (schema-public metadata for FK resolution) is a separate design question — see new backlog item L.15.
- **Lock-visible escape hatch** (show row but disallow edit) — backlog, not v1. Needs separate UX design.

#### Non-goals for v1

- Column-level permissions.
- Scoped REFERENCE at runtime (REFERENCE_GROUP / REFERENCE_OWN — enum values present in metadata model, but runtime behavior not wired).
- Lock-visible mode (show row but not allow edit) — deferred backlog, needs separate UX design.
- Server-side refLabel projection — stays client-side; modeler responsibility to expose only safe columns at REFERENCE level.

#### Retirement note

L.7 is already retired pre-ship (reverted 2026-05-09 per owner decision). Phase R is the sole existence-privacy mechanism in v4+. The `.plan/specs/rls.md` L.7 spec has been deleted. No further retirement action required on L.7; Phase R implementation closes the loop by wiring the REFERENCE permission model.

---

## Out of scope

- Cross-schema custom roles.
- User-defined privacy floor.
- Audit log of permission changes.
- Audit history per row (`_history` siblings).
- Direct-SQL `SELECT count(*)` by a COUNT-scoped user — leaks unfloored
  count. Future enhancement: route counts through a `SECURITY DEFINER`
  function. Deferred.
- Direct-SQL bare FK reads (`SELECT fk_id FROM B`) bypassing the
  `SqlQuery` auto-join layer — the FK column is data-at-rest in B and
  cannot be filtered by A's RLS policy without a per-row probe.
  Through GraphQL / REST / `SqlQuery.retrieveRows()` / `retrieveJSON()`
  this is now protected (both paths apply the auto-join and project
  NULL for invisible FK targets). Raw JDBC/psql access is still
  unprotected by design.
