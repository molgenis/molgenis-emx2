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
MOLGENIS.mg_can_read     (p_schema, p_table, p_groups, p_owner)            BOOLEAN
MOLGENIS.mg_can_write    (p_schema, p_table, p_groups, p_owner, p_verb)    BOOLEAN
MOLGENIS.mg_can_write_all(p_schema, p_table, p_groups, p_owner, p_verb)    BOOLEAN
MOLGENIS.mg_privacy_count(p_table TEXT, p_filter TEXT)                     BIGINT
```

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

### Phase G — perf, audit, docs

G.1 Benchmark: 1M rows, 5 custom roles, 100 groups, 10k users. Target
    < 2× non-RLS baseline.
G.2 If pathological, fall back to materialised view of
    `(user, schema, table, verb) → groups[]` updated by trigger.
G.3 Audit query patterns over `pg_policies`.
G.4 Docs: model overview, worked examples, operator runbook
    (debug "user can't see row X").

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

- Performance benchmark numbers (§G.1) and materialised-view fallback
  decision.
- Cross-schema FK semantics tests (§F.1) — `GeneratorTest.generateTypes`
  / `generateCrossSchemaTest` are the canary; deferred from Phase A
  combined-suite triage.
- Docs and runbook (§G.4).

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

## Out of scope

- Cross-schema custom roles.
- User-defined privacy floor.
- Audit log of permission changes.
- Audit history per row (`_history` siblings).
- Direct-SQL `SELECT count(*)` by a COUNT-scoped user — leaks unfloored
  count. Future enhancement: route counts through a `SECURITY DEFINER`
  function. Deferred.
