# Plan: Fine-grained permission system

Spec: [`fine_grained_permissions.md`](../specs/fine_grained_permissions.md) — all behavioural detail lives there.

Phases:
- **Phase 1** — Java + SQL layer (migration, model, policies, guard trigger, integration tests). No GraphQL. No UI.
- **Phase 2** — GraphQL (`_session.permissions`, admin query + mutations). Likely PR boundary.
- **Phase 3** — SelectScope view modes (COUNT, AGGREGATE, EXISTS, RANGE enforcement).
- **Phase 4** — UI (global admin).
- **Phase 5** — Performance & polish (diff-and-patch, built-in install via new codepath, trigger-lookup caching).

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

Role descriptions stored as `COMMENT ON ROLE`. System-role flag derived via `isSystemRoleByName`. `permission_attributes` and `role_wildcards` tables used instead of `role_metadata`.

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

## Phase 3 — SelectScope view modes (COUNT, AGGREGATE, EXISTS, RANGE)

Today only `FULL` is enforced server-side; the other `MolgenisSelectScope` values are declared but rejected on input. Phase 3 makes them real: a user assigned a role whose `selectScope` is non-FULL can perform only the allowed operations on matching tables.

### Storage

Two columns added (migration 32 is pre-ship — edit in place, no version bump):

```sql
ALTER TABLE "MOLGENIS"."permission_attributes" ADD COLUMN select_scope VARCHAR NOT NULL DEFAULT 'FULL';
ALTER TABLE "MOLGENIS"."role_wildcards"        ADD COLUMN select_scope VARCHAR NOT NULL DEFAULT 'FULL';
```

Plus a consistency rename in `role_wildcards`: rename `{select,insert,update,delete}_scope` → `{select,insert,update,delete}`. Those columns hold the **edit** Scope (NONE/OWN/GROUP/ALL), not the view mode; the new `select_scope` column is the view mode. Rename frees the name for the semantic it matches (same as the `selectScope` field on `TablePermission`). Internal-only rename — DB tables are private to `MOLGENIS` schema; no external API change.

Built-ins re-seed via `SqlSchema.create` hook: AGGREGATOR→AGGREGATE, COUNT→COUNT, RANGE→RANGE, EXISTS→EXISTS, VIEWER/EDITOR/MANAGER/OWNER→FULL. Replaces the hardcoded template currently at `SqlRoleManager:885-890` with actual rows in `permission_attributes`. This subsumes the deferred story 1.9 tail (built-in install via new codepath).

### Capability matrix

| Mode | rows / JSON | count | sum/avg | min/max | group by | exists |
|------|-------------|-------|---------|---------|----------|--------|
| FULL | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| AGGREGATE | ✗ | ✓ (≥ threshold) | ✓ | ✓ | ✓ | ✓ |
| COUNT | ✗ | ✓ (≥ threshold) | ✗ | ✗ | ✗ | ✓ |
| EXISTS | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ |
| RANGE | ✗ | ✗ | ✗ | ✓ | ✗ | ✓ |

Threshold for AGGREGATE/COUNT: single database-wide default (10), no per-role override in v1. `exists` check is the minimum affordance for all non-FULL modes (lets UI show "records present" without exposing data).

When a user holds multiple roles on the same table, union-most-permissive applies: the widest SelectScope wins. E.g. a user with AGGREGATOR (AGGREGATE) and VIEWER (FULL) sees FULL. Matches the scope-ladder union for edit verbs already in place.

### Stories

#### Story 3.1 — DB column add + rename (in-place migration edit)

- Edit `migration32.sql` to add `select_scope` column on both tables and rename the wildcard edit-scope columns. Update `MigrationsTest` if it asserts on column names.
- Update `MOLGENIS.mg_enforce_row_authorisation()` — not affected by this change, but confirm no reference to the renamed columns.

Red: `MigrationsTest#migration32CreatesSelectScopeColumn` (new). Green: edit migration, re-run idempotency test.

#### Story 3.2 — Read/write `selectScope` through SqlRoleManager

- `setPermissions` writes `select_scope` alongside `change_owner`/`share` in `permission_attributes` (and in `role_wildcards` for wildcard entries).
- `getPermissions` / `getTablePermissionsForActiveUser` hydrate `selectScope` back into the `TablePermission` record.
- Built-in roles returning `Scope.NONE + selectScope=ALL-as-none` placeholder gets replaced with actual rows + selectScope.

Red: `SqlRoleManagerTest#{setPermissionsPersistsSelectScope, getPermissionsReadsSelectScope}`. Green: plumb through.

#### Story 3.3 — GraphQL accepts non-FULL values

- Remove server-side rejection of COUNT/AGGREGATE/EXISTS/RANGE in `GraphqlPermissionFieldFactory.applyPermissionsForRole`.
- `_session.permissions` output includes the effective `selectScope`.

Red: `GraphqlPermissionFieldFactoryTest#changePermissions_acceptsSelectScope`. Green: drop the rejection + wire the field.

#### Story 3.4 — Enforcement per mode in `SqlQuery`

Introduce a per-query `effectiveSelectScope(table)` helper that resolves the user's strongest view mode for the target table.

- **3.4a — AGGREGATE** — already partial. Today enforces row-retrieve block + count threshold. Add explicit allow-lists for sum/avg/min/max/group-by (these already work by virtue of row-retrieve being off, but add guards so dropping a check elsewhere does not accidentally widen).
- **3.4b — COUNT** — block sum/avg/min/max/group-by; allow count only (with threshold).
- **3.4c — EXISTS** — block all aggregates except `SELECT EXISTS(...)`; allow exists only.
- **3.4d — RANGE** — allow min/max only; block sum/avg/count/group-by.

Each story writes red tests first (`TestSelectScope{Aggregate,Count,Exists,Range}Enforcement#...`), then green.

#### Story 3.5 — Built-in roles re-seeded via SqlSchema.create

- On schema create, call `SqlRoleManager.setPermissions(<built-in>, preset)` for each built-in including its `selectScope`.
- Pre-existing schemas: backfill once on migration (or rely on a follow-up "upgrade schema" admin action — see Phase 5).
- Legacy `Privileges.java` install path retained for compatibility during rollout; removal in Phase 5.

Red: `BuiltinInstallTest#selectScopeMatchesBuiltin`. Green: hook + seed.

#### Story 3.6 — Phase 3 integration test

`SelectScopeIT#fullMatrix` — admin creates 5 roles (one per mode), grants each to a distinct test user, runs each allowed and disallowed operation per row of the capability matrix. Asserts HTTP 200 for allowed, `MolgenisException`-mapped error for disallowed.

### Open design questions (resolve before starting Phase 3)

- Threshold configurable per-database or hardcoded 10? (Recommend hardcoded; revisit if bioinformatics users complain.)
- Does EXISTS mode allow the user to discover table/column names via GraphQL introspection? (Schema metadata is a separate concern from row data; recommend: yes, keep introspection open — matching COUNT/AGGREGATE behavior.)
- Is the `exists` affordance part of all non-FULL modes, or a mode of its own that stacks? (Matrix above treats it as a baseline floor for all non-FULL.)

---

## Phase 4 — Global admin UI

TBD after Phase 3 green. Depends on: SelectScope must round-trip through `_session.permissions` and `_admin.roles` before UI can render/edit it.

---

## Phase 5 — Performance & polish (backlog → phased)

### Story 5.1 — Diff-and-patch `setPermissions`

Currently replace-all: drops all policies for the role, re-creates them. ACCESS EXCLUSIVE lock per touched table for the transaction. Wildcard-permission updates that touch N tables lock all N, even when only one changed.

Goal (per spec rows 154-156): read current `PermissionSet` via `getPermissions(role)`, compute diff against payload, emit `DROP POLICY`/`CREATE POLICY` only for `(schema, table)` keys that changed. Unchanged entries → no DDL, no lock.

Red: `SqlRoleManagerTest#{setPermissionsDiffPatchOnlyTouchesChanged, setPermissionsNoOpForUnchangedWildcard}` (already on spec as unimplemented). Green: rewrite setPermissions around diff computation.

### Story 5.2 — Retire legacy `Privileges.java` install path

Once built-ins install exclusively via `SqlRoleManager.setPermissions` (Phase 3 Story 3.5) and all pre-existing schemas are backfilled, remove the legacy install code. Requires an admin-visible "upgrade schema" action for pre-feature DBs.

### Story 5.3 — Guard-trigger permission lookup cache

Per spec backlog line 289: cache caller's `change_owner`/`share` scope in a session GUC at request start (mirrors `molgenis.current_roles`) so the trigger avoids a `permission_attributes` scan per changed row. Profile first.

### Story 5.4 — Tombstone for deleted roles

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
