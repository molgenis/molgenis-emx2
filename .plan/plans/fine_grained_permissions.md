# Plan: Fine-grained permission system

Spec: [`fine_grained_permissions.md`](../specs/fine_grained_permissions.md) — all behavioural detail lives there.

Phases:
- **Phase 1** — Java + SQL layer (migration, model, policies, guard trigger, integration tests). No GraphQL. No UI.
- **Phase 2** — GraphQL (`_session.permissions`, admin query + mutations). Likely PR boundary.
- **Phase 3** — UI (global admin).

Per-story workflow (red-green TDD is mandatory):
1. Agent writes failing JUnit tests FIRST. Runs them. Confirms RED to lead.
2. Lead approves move to GREEN.
3. Agent writes minimum code to pass. Verifies GREEN.
4. Surgical changes only (see CLAUDE.md `# Surgical changes`).
5. Agent stages with `git add`; reports spec rows satisfied.
6. Review agent checks patterns, dead code, terminology drift.

---

## Phase 1 — Java + SQL layer [COMPLETE]

### Story 1.1 — Migration 32 + permission tables + RLS flag column [DONE]

Tasks:
- Red: `MigrationsTest#migration32AppliesIdempotently`
- Green: added `migration32.sql` at `backend/molgenis-emx2-sql/src/main/resources/org/molgenis/emx2/sql/migration32.sql`; bumped `SOFTWARE_DATABASE_VERSION` to 33 in `Migrations.java`.
- Migration creates `permission_attributes` (role_name, schema_name, table_name, change_owner, share), `role_wildcards` (role_name, schema_pattern, table_pattern, scope columns), adds `row_level_security BOOLEAN DEFAULT FALSE` on `table_metadata`, installs `current_user_roles()`, sets `BYPASSRLS` on admin, creates `mg_enforce_row_authorisation` trigger function.
- Note: `role_metadata` table was NOT implemented. Role descriptions stored as `COMMENT ON ROLE`. System-role flag derived from `isSystemRoleByName` in `SqlRoleManager`.

Spec: migration 32 row, row_level_security flag default, BYPASSRLS.

### Story 1.2 — `TablePermission` + `PermissionSet` [DONE]

Tasks (all green):
- `TablePermissionTest#wildcardAccepted`, `PermissionSetTest#{putReplacesByKey, validateReturnsAllErrors, deleteRequiresRead, updateRequiresRead, changeOwnerRequiresUpdate, shareRequiresUpdate, serverRejectsInsteadOfUpgrading, resolveForUnionPermissive, schemaScopeDeferred}`.
- `TablePermission` is a Java `record`; `PermissionSet` wraps `LinkedHashMap<String, TablePermission>` keyed on `schema + ":" + table`.

### Story 1.3 — `RoleManager` interface + `TableMetadata.rowLevelSecurity` field [DONE]

Tasks: `TableMetadataTest#rlsFlagDefault` green. `RoleManager` interface added to base module.

### Story 1.4 — `SqlRoleManager`: create / delete / grant / revoke / admin guard [DONE]

Tasks: `SqlRoleManagerTest#{roleNameLengthCap, rejectBuiltinNameCollision, createPersistsRoleAndPgRole, deleteCascadesPoliciesAndMembers, immutableBuiltinsRejected, grantMembership, revokeMembership, nonAdminRejected}` all green.
- `deleteRole` uses PG-native cleanup: `REASSIGN OWNED BY`, `DROP OWNED BY`, `DROP ROLE`.
- No tombstone/`role_metadata` semantics — name is immediately reusable after drop.

### Story 1.5 — `SqlPermissionExecutor` scope=all path [DONE]

Tasks: `SqlRoleManagerEmissionTest#{allScopeUsesGrantOnly, clearScoped}` green.

### Story 1.6 — `SqlPermissionExecutor` policy path [DONE]

Tasks: `SqlRoleManagerEmissionTest#{selectOwn, selectGroup, insertPolicies, updatePoliciesUsingAndWithCheck, deletePolicy, roundTrip, allScopeReplacesNarrower}` green.

### Story 1.7 — `SqlTableMetadata.setRowLevelSecurity` lifecycle + guard trigger [DONE]

Tasks: `SqlTableMetadataRlsTest#{enableBackfillsAndInstalls, disableBlockedWhenOwnGroupUsed, disableSafeDropsPolicies, idempotent}` green.

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
- GraphQL enum `MolgenisViewMode` → `MolgenisViewScope` (view modality: FULL/COUNT/AGGREGATE/EXISTS/RANGE).
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

## Phase 3 — Global admin UI

TBD after phase 2 green.

---

## Decisions log

- Components: public = `TablePermission`, `PermissionSet`, `RoleManager` (base module); `SqlRoleManager` public impl; `SqlPermissionExecutor` package-private static helpers (`org.molgenis.emx2.sql.rls`).
- Helper function names follow PG action verbs: `create/drop` for policies and PG roles, `grant/revoke` for privileges and role-to-user, `enable/disable` for RLS, `install` for bootstrap-once fixtures.
- `PermissionSet` uses composition (wraps `Set<TablePermission>`), not inheritance — keeps API narrow, avoids leaking `Set` contract.
- `setPermissions` is replace-all, transactional (diff-and-patch deferred to backlog).
- Trigger count reduced to one (`mg_enforce_row_authorisation`) per RLS-enabled table.
- Migration 32 follows existing scheme (`migration32.sql` resource + version bump).
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
- `selectScope` field (was `viewMode`) on permission input; `MolgenisViewScope` enum (was `MolgenisViewMode`).
- `MolgenisEditScope` enum (was `MolgenisScope`) for edit-verb scopes.
- `systemRole` GraphQL field (not `immutable`) in admin role output.
- `_session.permissions` (not `effectivePermissions`) — no collision found with existing `tablePermissions`.
