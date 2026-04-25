# Plan: Fine-grained permission system

Spec: [`fine_grained_permissions.md`](../specs/fine_grained_permissions.md) — all behavioural detail lives there.

Phases:
- **Phase 1** — Java + SQL layer (migration, model, policies, guard trigger, integration tests). No GraphQL. No UI.
- **Phase 2** — GraphQL (`_session.permissions`, admin query + mutations). Likely PR boundary.
- **Phase 3** — SelectScope view modes (COUNT, AGGREGATE, EXISTS, RANGE enforcement) + diff-and-patch `setPermissions`.
- **Phase 4** — UI (global admin).
- **Phase 5** — Performance & polish (retire legacy `Privileges.java`, trigger-lookup caching, role-name tombstones).

Per-story workflow (red-green TDD is mandatory):
1. Agent writes failing JUnit tests FIRST. Runs them. Confirms RED to lead.
2. Lead approves move to GREEN.
3. Agent writes minimum code to pass. Verifies GREEN.
4. Surgical changes only (see CLAUDE.md `# Surgical changes`).
5. Agent stages with `git add`; reports spec rows satisfied.
6. Review agent checks patterns, dead code, terminology drift.

---

## Phase 1 — Java + SQL layer [COMPLETE]

### Story 1.1 — Migration 32 + permission tables [DONE]

Tasks:
- Red: `MigrationsTest#migration32AppliesIdempotently`
- Green: added `migration32.sql` at `backend/molgenis-emx2-sql/src/main/resources/org/molgenis/emx2/sql/migration32.sql`; bumped `SOFTWARE_DATABASE_VERSION` to 33 in `Migrations.java`.
- Migration creates `permission_attributes` (role_name, schema_name, table_name, change_owner, share), `role_wildcards` (role_name, schema_pattern, table_pattern, scope columns), installs `current_user_roles()`, sets `BYPASSRLS` on admin, creates `mg_enforce_row_authorisation` trigger function.
- Note: `role_metadata` table was NOT implemented. Role descriptions stored as `COMMENT ON ROLE`. System-role flag derived from `isSystemRoleByName` in `SqlRoleManager`.
- Note: `row_level_security BOOLEAN DEFAULT FALSE` column on `table_metadata` was removed — RLS is now emergent from scoped permissions.

Spec: migration 32 row, row_level_security flag default, BYPASSRLS.

### Story 1.2 — `TablePermission` + `PermissionSet` [DONE]

Tasks (all green):
- `TablePermissionTest#wildcardAccepted`, `PermissionSetTest#{putReplacesByKey, validateReturnsAllErrors, deleteRequiresRead, updateRequiresRead, changeOwnerRequiresUpdate, shareRequiresUpdate, serverRejectsInsteadOfUpgrading, resolveForUnionPermissive, schemaScopeDeferred}`.
- `TablePermission` is a Java `record`; `PermissionSet` wraps `LinkedHashMap<String, TablePermission>` keyed on `schema + ":" + table`.

### Story 1.3 — `RoleManager` interface [DONE]

Tasks: `RoleManager` interface added to base module. `TableMetadata.rowLevelSecurity` field removed — RLS is emergent from scoped permissions.

### Story 1.4 — `SqlRoleManager`: create / delete / grant / revoke / admin guard [DONE]

Tasks: `SqlRoleManagerTest#{roleNameLengthCap, rejectBuiltinNameCollision, createPersistsRoleAndPgRole, deleteCascadesPoliciesAndMembers, immutableBuiltinsRejected, grantMembership, revokeMembership, nonAdminRejected}` all green.
- `deleteRole` uses PG-native cleanup: `REASSIGN OWNED BY`, `DROP OWNED BY`, `DROP ROLE`.
- No tombstone/`role_metadata` semantics — name is immediately reusable after drop.

### Story 1.5 — `SqlPermissionExecutor` scope=all path [DONE]

Tasks: `SqlRoleManagerEmissionTest#{allScopeUsesGrantOnly, clearScoped}` green.

### Story 1.6 — `SqlPermissionExecutor` policy path [DONE]

Tasks: `SqlRoleManagerEmissionTest#{selectOwn, selectGroup, insertPolicies, updatePoliciesUsingAndWithCheck, deletePolicy, roundTrip, allScopeReplacesNarrower}` green.

### Story 1.7 — RLS emergent from scoped permissions [DONE]

RLS is no longer an explicit toggle. First OWN/GROUP grant on a table auto-installs RLS (mg_roles column, mg_owner column, trigger, ENABLE/FORCE RLS, GIN index) via `SqlPermissionExecutor.ensureRlsInstalled` called from `emitVerb`. Last OWN/GROUP policy removed drops only the policies; infrastructure stays.

Tasks: `SqlTableMetadataRlsTest#{firstOwnGrantInstallsRls, firstGroupGrantInstallsRls, rlsInstallIdempotent, lastOwnGroupRemovedDropsPoliciesKeepsRls, allScopeGrantDoesNotInstallRls}` green.

### Story 1.8 — `setPermissions` transactional [DONE]

Tasks: `SqlRoleManagerTest#{setPermissionsReplaceAll, setPermissionsRoundTrip, setPermissionsTransactional}` green.
- Replace-all semantics (not diff-and-patch in final implementation; diff-and-patch deferred to backlog).

### Story 1.9 — Wildcards + built-in install via new codepath [DONE]

Tasks: `SqlRoleManagerTest#{wildcardExistingAndFuture, schemaDropNoError, schemaDropCascades, customRoleOnAnySchema}` green.
- Built-ins still installed via legacy `Privileges.java` path; `SqlSchema.create` hook for built-in install via `SqlRoleManager.setPermissions` deferred.

### Story 1.10 — Phase 1 integration test [DONE]

`FineGrainedPermissionsIT` (or covered by phase 2 IT) exercises full CRUD matrix.

---

## Phase 2 — GraphQL [COMPLETE]

### Story 2.1 — `_session.permissions` field [DONE]

`GraphqlSessionFieldFactory` extended with `permissions` field backed by `SqlRoleManager.getPermissionsForActiveUser()`.
Field named `permissions` (not `effectivePermissions` as originally planned — no collision found).

### Story 2.2 — Admin `_admin.roles` query [DONE]

`GraphqlAdminFieldFactory` exposes `_admin { roles(name) { role description systemRole permissions {...} members } }`.
- Uses `roleOutputType` from `GraphqlPermissionFieldFactory`.
- `systemRole` field (not `immutable`) reflects `Role.isSystemRole()`.

### Story 2.3 — Admin `change` / `drop` mutations [DONE]

`GraphqlDatabaseFieldFactory` (or merged into existing field factories) exposes:
- `change(tables, roles, members)` — upsert roles with nested permissions, flip RLS, grant memberships.
- `drop(roles, members)` — delete roles, revoke memberships.
- Both admin-only, transactional via `database.tx(...)`.

### Story 2.4 — Phase 2 integration test [DONE]

`GraphqlPermissionsIT#fullScenario` (HTTP/GraphQL) and `GraphqlPermissionFieldFactoryTest` (unit) both green.

---

## Late-stage refactors [COMPLETE]

### API reshape [DONE]

Mutations merged into existing `change`/`drop` root fields (not separate `changePermissions`/`dropPermissions` pair as originally planned). Avoids root-field proliferation; roles arg reused for fine-grained role input.

### Scope + ViewMode unification (option B) [DONE]

- GraphQL enum `MolgenisScope` → `MolgenisEditScope` (edit verbs: select/insert/update/delete).
- GraphQL enum `MolgenisViewMode` → `MolgenisSelectScope` (view modality: FULL/COUNT/AGGREGATE/EXISTS/RANGE).
- GraphQL field `viewMode` → `selectScope` on `MolgenisPermissionInputFg`.
- Java enums `TablePermission.Scope` and `TablePermission.ViewMode` unchanged.

### Role merged with legacy, fluent builder, `isSystemRole` preserved [DONE]

`SqlRoleManager` extends existing class (not a replacement). `Role` record carries `isSystemRole()` derived from built-in name check.

### Trigger renamed [DONE]

Guard trigger function renamed to `mg_enforce_row_authorisation` (was `mg_reserved_column_guard` in original spec).

### `role_metadata` table removed in favour of PG-native roles [DONE]

Role descriptions stored as `COMMENT ON ROLE`. System-role flag derived via `isSystemRoleByName`. No metadata tables — `permission_attributes` and `role_wildcards` removed during revert; all state encoded in `pg_policies` policy names.

### GraphQL naming [DONE]

- `systemRole` not `immutable` in admin role output type.
- `_session.permissions` not `_session.effectivePermissions`.
- Mutations on existing `change`/`drop` not separate `changePermissions`/`dropPermissions`.

---

## Post-Phase-2 cleanup [COMPLETE]

### `TablePermission` record migration [DONE]

- `TablePermission.java` introduced as Java `record` replacing legacy Boolean-based class
- Updated all callers: `Schema.java`, `SqlSchema.java`, `SqlRoleManager.java`, `SqlQuery.java`, `Role.java`, `GraphqlSchemaFieldFactory.java`, `GraphqlTableFieldFactory.java`, all test files
- Boolean `true` → `Scope.ALL`, `false/null` → `Scope.NONE` (bistate; `applyPgGrants` now always explicitly GRANTs or REVOKEs all verbs)
- `mergePermissions` uses `maxScope(Scope, Scope)` on each verb
- `TestTableRoleManagement` and `TestTablePermissionEnforcement` updated; `TestTableRoleManagement` all 26 tests pass

### `_session.tablePermissions` retirement [DONE]

- Removed `TABLE_PERMISSIONS` field and `MolgenisTablePermission` GraphQL type from `GraphqlSessionFieldFactory`
- Removed `CAN_VIEW`, `CAN_INSERT`, `CAN_UPDATE`, `CAN_DELETE`, `TABLE_PERMISSIONS` constants from `GraphqlConstants`
- `_session` now exposes `permissions` (list of `TablePermission` with `Scope` enum values) replacing the old Boolean-typed field
- `roleToMap` in `GraphqlSchemaFieldFactory` converts `Scope` back to boolean for `_schema.roles.permissions` (keeps that field Boolean-typed)
- `TablePermissionsGraphqlTest` (15 integration tests via HTTP) and `GraphqlPermissionFieldFactoryTest` (10 unit tests) all pass

---

## Phase 3 — SelectScope view modes (COUNT, AGGREGATE, EXISTS, RANGE) + MANAGER/OWNER gate + role-name validation + unified select

Phase 3 unifies select mode + row scope into single Select enum; validates role names (max 32 chars, charset [a-zA-Z0-9 ]); gates setPermissions on MANAGER/OWNER privilege; makes COUNT/AGGREGATE/EXISTS/RANGE view modes real.

### Storage

All permission attributes (change_owner, share, view mode) are encoded as PG policies in `pg_policies`. No auxiliary MOLGENIS tables. Source of truth = `pg_policies` scan only.

Policy naming (migration 32 is pre-ship — edit in place, no version bump):

- Edit-verb scopes: `MG_P_<role>_<VERB>_<SCOPE>` where VERB ∈ SELECT/INSERT/UPDATE/DELETE, SCOPE ∈ OWN/GROUP/ALL — already implemented.
- `change_owner` flag: `MG_P_<role>_CHANGEOWNER_<UPDATE_SCOPE>` (existence = flag is true; scope mirrors the update verb scope).
- `share` flag: `MG_P_<role>_SHARE_<UPDATE_SCOPE>` (existence = flag is true).
- View mode: `MG_P_<role>_VIEW_<MODE>` where MODE ∈ COUNT/AGGREGATE/EXISTS/RANGE. Default FULL = absence of any VIEW policy.

Sentinel policies (CHANGEOWNER/SHARE/VIEW) are created as `FOR SELECT USING (false)` — they mark capability without granting data access (OR'd with real SELECT policies → no change to effective access).

Wildcards (`schema='*'`, `table='*'`) are materialised at `setPermissions` time as concrete per-(schema,table) policies. `getPermissions(role)` returns concrete entries, not wildcard templates — acceptable per design.

### Capability matrix

| Mode | rows / JSON | count | sum/avg | min/max | group by | exists |
|------|-------------|-------|---------|---------|----------|--------|
| FULL/ALL | ✓ | ✓ exact | ✓ | ✓ | ✓ | ✓ |
| AGGREGATE | ✗ | ✓ exact | ✓ | ✓ | ✓ | ✓ |
| COUNT | ✗ | ✓ exact | ✗ | ✗ | ✗ | ✓ |
| EXISTS | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ |
| RANGE | ✗ | ✓ truncated | ✗ | ✓ | ✗ | ✓ |

RANGE count truncation rule: keep only the first significant digit, zero the rest. Counts 0–9 → 0. `floor(n / 10^(digits-1)) * 10^(digits-1)` for n ≥ 10. `exists` check is the minimum affordance for all non-FULL modes.

When a user holds multiple roles on the same table, union-most-permissive applies: the widest SelectScope wins. E.g. a user with AGGREGATOR (AGGREGATE) and VIEWER (FULL) sees FULL. Matches the scope-ladder union for edit verbs already in place.

### Stories

#### Story 3.1 — Policy-based encoding (in-place migration edit) [DONE]

- `migration32.sql` updated in place: removed `permission_attributes` and `role_wildcards` tables; rewrote `mg_enforce_row_authorisation()` to scan `pg_policies` for CHANGEOWNER/SHARE sentinel policies instead of reading from auxiliary tables.
- Sentinel policies created as `FOR SELECT USING (false)` — visible in `pg_policies`, OR'd with real policies → no access change.
- Tests green: `MigrationsTest#migration32AppliesIdempotently`. `MigrationsTest#migration32CreatesSelectScopeColumn` deleted (tables no longer exist).

#### Story 3.2 — Read/write `selectScope` through SqlRoleManager [DONE]

- `setPermissions` emits `MG_P_<role>_VIEW_<MODE>` sentinel policy when viewMode != FULL.
- `getPermissions` parses VIEW/CHANGEOWNER/SHARE policy names from `pg_policies` scan.
- `SqlRoleManagerTest#{setPermissionsPersistsSelectScope, getPermissionsReadsSelectScope}` rewritten to assert policy existence/parsing.

#### Story 3.3 — GraphQL accepts non-FULL values [DONE]

Removed server-side rejection of COUNT/AGGREGATE/EXISTS/RANGE; exposed `selectScope` in `_session.permissions` output.

#### Story 3.4 — Collapse select+selectScope into single Select enum

Replace `select: Scope` + `selectScope: ViewMode` with unified `select: Select` enum carrying NONE/EXISTS/COUNT/AGGREGATE/RANGE/OWN/GROUP/ALL. Policy emission: `MG_P_<role>_SELECT_<MEMBER>` uniform. GraphQL: drop `MolgenisSelectScope`; new `MolgenisSelect` enum. Java: rename `TablePermission.viewMode` → `TablePermission.select`, type `Select`.

Diff-and-patch `setPermissions` only touches changed `(schema, table)` keys.

#### Story 3.5 — Validate role names (length 32, charset [a-zA-Z0-9 ])

Add constant `MAX_ROLE_NAME_LENGTH = 32` and validation regex in `SqlRoleManager.createRole`. Reject names >32 chars, containing underscore/special chars, or with leading/trailing space. Throw `MolgenisException` with clear message.

#### Story 3.6 — Gate setPermissions on MANAGER or OWNER privilege

GraphQL mutation `setPermissions` checks caller holds `MANAGER` or `OWNER` privilege on target schema. Lower-privileged calls get permission-denied error.

#### Story 3.7 — Enforcement per mode in `SqlQuery`

Introduce a per-query `effectiveSelectScope(table)` helper that resolves the user's strongest view mode for the target table.

- **3.7a — AGGREGATE** — enforces row-retrieve block; count/sum/avg/min/max/group-by exact (no threshold).
- **3.7b — COUNT** — block sum/avg/min/max/group-by; allow exact count only.
- **3.7c — EXISTS** — block all aggregates except `SELECT EXISTS(...)`; allow exists only.
- **3.7d — RANGE** — allow min/max and truncated count; block sum/avg/group-by. Count truncation: 0–9→0, 10→keep first digit zero rest (`floor(n / 10^(digits-1)) * 10^(digits-1)`).

Each story writes red tests first (`TestSelectScope{Aggregate,Count,Exists,Range}Enforcement#...`), then green.

#### Story 3.8 — Built-in roles re-seeded via SqlSchema.create

- On schema create, call `SqlRoleManager.setPermissions(<built-in>, preset)` for each built-in including its `selectScope`.
- Pre-existing schemas: backfill once on migration (or rely on a follow-up "upgrade schema" admin action — see Phase 5).
- Legacy `Privileges.java` install path retained for compatibility during rollout; removal in Phase 5.

Red: `BuiltinInstallTest#selectScopeMatchesBuiltin`. Green: hook + seed.

#### Story 3.9 — Diff-and-patch `setPermissions`

Currently replace-all: drops all policies for the role, re-creates them. ACCESS EXCLUSIVE lock per touched table for the transaction. Wildcard-permission updates that touch N tables lock all N, even when only one changed.

Goal (per spec rows 154-156): read current `PermissionSet` via `getPermissions(role)`, compute diff against payload, emit `DROP POLICY`/`CREATE POLICY` only for `(schema, table)` keys that changed. Unchanged entries → no DDL, no lock. Same treatment for the new `select_scope` column — unchanged rows in `permission_attributes` are skipped.

Pulled into Phase 3 (was backlog) because 3.1/3.2 touch the same emit path; folding in now avoids a second pass on the same code.

Red: `SqlRoleManagerTest#{setPermissionsDiffPatchOnlyTouchesChanged, setPermissionsNoOpForUnchangedWildcard}` (already on spec as unimplemented). Green: rewrite setPermissions around diff computation.

#### Story 3.10 — Phase 3 integration test

`SelectScopeIT#fullMatrix` — admin creates 5 roles (one per mode), grants each to a distinct test user, runs each allowed and disallowed operation per row of the capability matrix. Asserts HTTP 200 for allowed, `MolgenisException`-mapped error for disallowed.

### Design decisions (locked)

- Unified Select enum subsumes view modes + row scopes (Decisions A, I).
- Threshold hardcoded at 10 (per spec, no config in v1).
- EXISTS mode allows GraphQL introspection (schema metadata separate from row data).
- `exists` affordance is a baseline floor for all non-FULL modes.
- Role name max 32 chars, charset [a-zA-Z0-9 ], no leading/trailing space (Decision C).
- `setPermissions` gates on MANAGER/OWNER privilege (Decision E).
- Description sanitization via jOOQ inline() (Decision B).

---

## Phase 4 — Global admin UI

TBD after Phase 3 green. Depends on: SelectScope must round-trip through `_session.permissions` and `_admin.roles` before UI can render/edit it.

---

## Phase 4 — UI integration (planned)

### Story 4.1 — `mg_roles` cell editor with role filtering

`mg_roles` cell editor offers ONLY roles that hold a SELECT grant on the current table. If exactly one qualifying role exists, auto-select it. Client-side validation; backend enforces per-policy.

---

## Phase 5 — Performance & polish (backlog → phased)

### Story 5.1 — Retire legacy `Privileges.java` install path

Once built-ins install exclusively via `SqlRoleManager.setPermissions` (Phase 3 Story 3.5) and all pre-existing schemas are backfilled, remove the legacy install code. Requires an admin-visible "upgrade schema" action for pre-feature DBs.

### Story 5.2 — Guard-trigger permission lookup cache

Cache caller's `change_owner`/`share` scope in a session GUC at request start (mirrors `molgenis.current_roles`) so the trigger avoids a `pg_policies` scan per changed row. Profile first.

### Story 5.3 — Tombstone for deleted roles

Per spec backlog line 290: reserve role names after `deleteRole` so a future `createRole` with the same name cannot resurrect visibility via orphaned `mg_roles` entries. v1 does not reserve.

---

## Decisions log

- Components: public = `TablePermission`, `PermissionSet`, `RoleManager` (base module); `SqlRoleManager` public impl; `SqlPermissionExecutor` package-private static helpers (`org.molgenis.emx2.sql.rls`).
- Helper function names follow PG action verbs: `create/drop` for policies and PG roles, `grant/revoke` for privileges and role-to-user, `enable/disable` for RLS, `install` for bootstrap-once fixtures.
- `PermissionSet` uses composition (wraps `Set<TablePermission>`), not inheritance — keeps API narrow, avoids leaking `Set` contract.
- `setPermissions` is replace-all, transactional (diff-and-patch deferred to backlog).
- Trigger count reduced to one (`mg_enforce_row_authorisation`) per RLS-enabled table.
- Migration 32 follows existing scheme (`migration32.sql` resource + version bump). No `row_level_security` column on `table_metadata` — RLS is emergent.
- `role_metadata` table NOT implemented; PG-native `COMMENT ON ROLE` for descriptions, `isSystemRoleByName` for system-role flag.
- Schema-scoped roles in v1: internal infrastructure supports them for built-ins; user-facing custom-role mutation still global-only.
- Terminology: `mg_roles` column, `group` scope, `share` permission.
- Policy naming: `MG_P_<role>_<VERB>_<SCOPE>`, role name ≤ 40 chars.
- Concurrency: last-write-wins.
- Admin authority: Java `database.isAdmin()` pre-check on every mutation.
- `deleteRole` pattern: `REASSIGN OWNED BY` + `DROP OWNED BY` + `DROP ROLE`; no tombstone in v1.
- Session role cache via `SET LOCAL molgenis.current_roles = '<csv>'`.
- `FORCE ROW LEVEL SECURITY` issued alongside `ENABLE`.
- `current_user_roles()` declared `STABLE`.
- GIN index on `mg_roles` created on RLS enable, dropped on disable.
- GraphQL mutations use EMX2's unified `change` / `drop` pattern on existing root fields.
- `selectScope` field (was `viewMode`) on permission input; `MolgenisSelectScope` enum (was `MolgenisViewMode`).
- `MolgenisEditScope` enum (was `MolgenisScope`) for edit-verb scopes.
- `systemRole` GraphQL field (not `immutable`) in admin role output.
- `_session.permissions` (not `effectivePermissions`) — no collision found with existing `tablePermissions`.


## Follow-ups (backlog)

- Upgrade-schema admin action (migrate legacy built-ins to policy form).
- Schema-scoped custom roles.
- Agg sub-levels for read ladder.
- Formal admin group model (replace superuser shortcut).
- Schema-manager UI for user-to-role assignment per schema.
- Guard-trigger performance: shared function is called per UPDATE on every RLS-enabled table; profile under load and add per-transaction role-cache (e.g. `SET LOCAL`) if it becomes a hot path.
- Java-side cache for `getPermissionsForActiveUser` — add a request-scoped cache analogous to `SchemaMetadata` caching only if profiling shows repetitive calls are a bottleneck. No cache in v1.
- Fate of existing user-created per-schema custom roles from legacy `SqlRoleManager.createRole(schemaName, roleName)` — decide whether to deprecate the API, auto-migrate to global custom roles, or keep indefinitely.
- `setPermissions` wildcard lock blast (when the diff is genuinely wide): batch-by-table in separate transactions — loses atomicity, add `MOLGENIS.permission_sync_log` for resume-on-crash.
- Guard-trigger permission lookup: cache caller's CHANGEOWNER/SHARE policy presence in a session GUC at request start (mirrors `molgenis.current_roles`) so the slow path avoids a `pg_policies` scan per changed row.
- Tombstone/name-reservation semantics for deleted roles (v1 does not reserve names after drop).