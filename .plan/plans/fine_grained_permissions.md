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

## Phase 1 — Java + SQL layer

### Story 1.1 — Migration 32 + `role_metadata` + RLS flag column

Tasks:
- Red: `MigrationsTest#migration32AppliesIdempotently` (bumps version 32→33; creates empty `role_metadata` with PK `(role_name, schema_name)` both NOT NULL, `schema_name` DEFAULT `'*'`; adds `row_level_security BOOLEAN NOT NULL DEFAULT FALSE` column on `table_metadata`; installs `current_user_roles()` SQL function marked `STABLE`; sets `BYPASSRLS` on admin; installs guard trigger function from `resources/sql/rls/mg_reserved_column_guard.sql`).
- Green: add `migration32.sql` resource at `backend/molgenis-emx2-sql/src/main/resources/org/molgenis/emx2/sql/migration32.sql` with all DDL; bump `SOFTWARE_DATABASE_VERSION` to 33 in `Migrations.java`; guard becomes `if (version < 33)`. DDL lives **only** in the migration — do **not** add `role_metadata` to `MetadataUtils.init()`.
- Add Java CRUD helpers to `MetadataUtils`: `saveRole(RoleMetadata)`, `getRole(name)`, `listRoles()`, `deleteRole(name)` — JOOQ queries against `role_metadata`, using `MetadataUtils.MOLGENIS` schema constant.
- `ALTER ROLE … BYPASSRLS` is non-transactional (documented risk in spec); no compensating logic in v1.
- Built-ins are NOT seeded in this story; they stay in legacy code path via `Privileges.java` **as a deliberate handover to story 1.9**, which replaces that install site.

Spec: migration 32 row, role_metadata table shape, row_level_security flag default, BYPASSRLS.

### Story 1.2 — `Permission` + `PermissionSet` (base module, no SQL yet)

Tasks:
- Red: `PermissionTest#wildcardAccepted`, `PermissionSetTest#{addReplacesByKey, validateReturnsAllErrors, deleteRequiresRead, updateRequiresRead, changeOwnerRequiresUpdate, shareRequiresUpdate, serverRejectsInsteadOfUpgrading, resolveForUnionPermissive, schemaScopeDeferred}`.
- Green: `org.molgenis.emx2.Permission` immutable data class; `org.molgenis.emx2.PermissionSet` with composition (not inheritance); `validate()` collects all errors; `resolveFor(schema, table)` unions wildcard+specific with most-permissive wins.
- `ownGroupRequiresRlsFlag` test deferred to story 1.7 (needs TableMetadata lookup).

Spec: Permission/PermissionSet rows.

### Story 1.3 — `RoleManager` interface + `TableMetadata.rowLevelSecurity` field

Tasks:
- Red: `TableMetadataTest#rlsFlagDefault`.
- Green: `org.molgenis.emx2.RoleManager` interface with signatures per spec; add `rowLevelSecurity` getter/setter on `TableMetadata` (default false).
- No implementation yet; just the contract + field.

Spec: components section, `rowLevelSecurity` field default.

### Story 1.4 — `SqlRoleManager` extends to implement `RoleManager`: create / delete / grant / revoke / admin guard

Tasks:
- Red: `SqlRoleManagerTest#{roleNameLengthCap, rejectBuiltinNameCollision, createPersistsRoleAndPgRole, deleteCascadesPoliciesAndMembers, immutableBuiltinsRejected, grantMembership, revokeMembership, nonAdminRejected}`.
- Green: extend `SqlRoleManager` with new methods; `deleteRole` scans `pg_policies` + grants for cleanup (emission helpers stubbed this story, fully implemented next).

Spec: role-mutation rows.

### Story 1.5 — `SqlPermissionExecutor` scope=all path (`grantTablePrivilege` / `revokeAllTablePrivileges`)

Tasks:
- Red: `SqlRoleManagerEmissionTest#{allScopeUsesGrantOnly, clearScoped}`.
- Green: create `org.molgenis.emx2.sql.rls.SqlPermissionExecutor` with statics `grantTablePrivilege`, `revokeTablePrivilege`, `revokeAllTablePrivileges`. `SqlRoleManager.setPermissions` invokes per entry; replace-all first calls `revokeAllTablePrivileges` + `dropAllPolicies`.

Spec: scope=all rows.

### Story 1.6 — `SqlPermissionExecutor` policy path (`createPolicy` / `dropPolicy` / `readPolicies` + `installCurrentUserRolesFunction`)

Tasks:
- Red: `SqlRoleManagerEmissionTest#{selectOwn, selectGroup, insertPolicies, updatePoliciesUsingAndWithCheck, deletePolicy, commentMetadata, roundTrip, allScopeAddsBypassWhenRlsEnabled}`.
- Green: implement `createPolicy(dsl, role, schema, table, verb, scope)` emitting `MG_P_<role>_<VERB>_<SCOPE>` with proper USING / WITH CHECK per matrix + COMMENT. Add `installCurrentUserRolesFunction` called once by migration 32. `readPolicies(dsl, role)` scans `pg_policies` + `information_schema.role_table_grants` + COMMENTs for round-trip.

Spec: own/group rows, COMMENT, round-trip.

### Story 1.7 — `SqlTableMetadata.setRowLevelSecurity` lifecycle + guard trigger

Tasks:
- Red: `SqlTableMetadataRlsTest#{enableBackfillsAndInstalls, disableBlockedWhenOwnGroupUsed, disableSafeDropsPolicies, idempotent}`.
- Red (from story 1.2 deferred): `PermissionSetTest#ownGroupRequiresRlsFlag`.
- Green: `SqlTableMetadata.setRowLevelSecurity(boolean, boolean dropColumns)` delegates to `SqlPermissionExecutor.enableRowLevelSecurity` / `disableRowLevelSecurity` / `installGuardTrigger` / `dropGuardTrigger`.
  - Enable: ADD `mg_owner` (DEFAULT `current_user`, backfill from `mg_insertedBy`), ADD `mg_roles` (DEFAULT `'{}'`), call `enableRowLevelSecurity`, call `installGuardTrigger`, re-emit pending own/group policies for any role whose permissions touched this table.
  - Disable: guard "no role uses own/group on this table", call `dropAllPolicies` per role on this table, call `dropGuardTrigger`, call `disableRowLevelSecurity`, optional column drop.

Spec: RLS flag lifecycle rows, ownGroupRequiresRlsFlag.

### Story 1.8 — `setPermissions` transactional + guard trigger body

Tasks:
- Red: `SqlRoleManagerTest#{setPermissionsReplaceAll, setPermissionsRoundTrip, setPermissionsTransactional, concurrentSaveLastWins}`.
- Red: `RowLifecycleTest#{insertDefaultsOwner, insertOwnBlocksForeignOwner, insertGroupValidatesRoles, insertAllDefaults, updateWithoutChangeOwnerRejected, updateWithoutShareRejected, changeOwnerOwnScope, changeOwnerGroupScope, changeOwnerAllScope, shareLimitedToGrantedRoles}`.
- Green: `setPermissions` wraps apply in one JOOQ transaction (replace-all semantics for this story; diff-and-patch in 1.8a). Guard trigger body consults `current_user_roles()` + stored permissions to decide change_owner/share authorisation per OLD→NEW row diff.

Spec: setPermissions rows + row-lifecycle rows.

### Story 1.8a — Diff-and-patch in `setPermissions`

Tasks:
- Red: `SqlRoleManagerTest#{setPermissionsDiffPatchOnlyTouchesChanged, setPermissionsNoOpForUnchangedWildcard}`.
- Green: before emitting DDL, call `getPermissions(role)` to load current state, compute `(added, removed, changed)` sets keyed on `(schema, table)`. Emit DROP+GRANT-revoke only for removed + changed; CREATE+GRANT only for added + changed. Unchanged entries produce zero DDL and zero locks.
- Integration: existing round-trip tests still pass (diff-and-patch is observationally equivalent to replace-all).

Spec: diff-and-patch rows.

### Story 1.9 — Wildcards + schema-create hook installs built-ins via new codepath

Tasks:
- Red: `SqlRoleManagerTest#{wildcardExistingAndFuture, schemaDropNoError, schemaDropCascades, customRoleOnAnySchema}`.
- Red: `BuiltinInstallTest#{uniformAcrossSchemas, listBuiltinsUniform}`.
- Green:
  - `setPermissions` enumerates existing matching schemas/tables for wildcard entries.
  - `SqlSchema.create` hook iterates `Privileges.java` and calls `setPermissions(<schema/builtin>, preset)` for each built-in. **Delete legacy built-in install call in `SqlSchemaMetadataExecutor.executeCreateSchema`** — do not leave dual code paths. Because built-in presets are scope=all, emission produces identical `GRANT`s to legacy; integration test asserts `role_table_grants` is identical between a pre-1.9 schema and a post-1.9 schema.
  - Pre-existing schemas stay untouched (no backfill); `listRoles` joins `Privileges.java` with `pg_roles` so built-ins appear uniformly.
  - Schema-drop silently no-ops for `*`-scoped custom roles.

Spec: wildcard + built-in rows.

### Story 1.10 — Phase 1 integration test

Tasks:
- Write `FineGrainedPermissionsIT`: admin creates custom role, saves wildcard+specific permissions mixing all/own/group, flips RLS on a table, grants role to a user, then asserts user read/insert/update/delete outcomes across all scopes, including change_owner/share trigger rejections and admin BYPASSRLS.
- Must pass GREEN to close phase 1.

---

## Phase 2 — GraphQL

Mutation pattern: follow existing EMX2 unified `change` / `drop` (see `GraphqlSchemaFieldFactory`). No per-operation mutation methods.

### Story 2.1 — `_session.permissions` field

Tasks:
- Red: `GraphqlSessionTest#permissionsForCurrentUser`.
- Green: extend `GraphqlSessionFieldFactory` with `permissions` field backed by `SqlRoleManager.getPermissionsForActiveUser()`.

### Story 2.2 — Admin `permission { roles }` query

Tasks:
- Red: `PermissionQueryTest#{listsAllRolesAndPermissions, nonAdminForbidden}`.
- Green: new `GraphqlPermissionFieldFactory` providing the query; lists roles + per-role permission set + members. Built-ins surfaced by joining `Privileges.java` with `pg_roles`.

### Story 2.3 — Admin `change` / `drop` mutations

Tasks:
- Red: `PermissionMutationTest#{changeRoles, changePermissions, changeMembers, changeTableRls, changeBatchTransactional, dropRoles, dropMembers, rejectInvariantViolations, nonAdminForbidden, validationErrorShape}`.
- Green: add `change(roles, permissions, members, tables)` and `drop(roles, members)` resolvers on `GraphqlPermissionFieldFactory`. Each resolver iterates its list and delegates to `SqlRoleManager.*` / `SqlTableMetadata.setRowLevelSecurity`. Whole call wrapped in one JOOQ transaction. Return `GraphqlApiMutationResult`. Validation errors collected into a single `MolgenisException` with multi-line message (EMX2 convention).

### Story 2.4 — Phase 2 integration test

Tasks:
- `GraphqlPermissionsIT#fullScenario` — full HTTP/GraphQL scenario per spec: single `change` call with roles + permissions + tables + members → user session → `_session.permissions` assertion → CRUD matrix → `drop` cleanup.

---

## Phase 3 — Global admin UI

TBD after phase 2 green.

---

## Decisions log

- Components: public = `Permission`, `PermissionSet`, `RoleManager` (base module); `SqlRoleManager` public impl; `SqlPermissionExecutor` package-private static helpers (`org.molgenis.emx2.sql.rls`).
- Helper function names follow PG action verbs: `create/drop` for policies and PG roles, `grant/revoke` for privileges and role-to-user, `enable/disable` for RLS, `install` for bootstrap-once fixtures.
- `PermissionSet` uses composition (wraps `Set<Permission>`), not inheritance — keeps API narrow, avoids leaking `Set` contract.
- `setPermissions` is replace-all, transactional v1 (no diff-and-patch, no ETag).
- Trigger count reduced to one (`mg_reserved_column_guard`) per RLS-enabled table. Other enforcement via PG column DEFAULTs + policy WITH CHECK. Defense-in-depth principle: PG layer is the enforcement boundary, SqlTable is ergonomics.
- Migration 32 follows existing scheme (`migration32.sql` resource + version bump + `MetadataUtils.init()` extension).
- Built-in install unified under `SqlRoleManager.setPermissions` (invoked by `SqlSchema.create`). Because built-in presets are scope=all, emission produces the same `GRANT`s as legacy — no observable change. Legacy install call is replaced, not preserved.
- Schema-scoped roles in v1: internal infrastructure supports them for built-ins; user-facing custom-role mutation still global-only.
- Terminology: Option 1 (`mg_roles` column, `group` scope, `share` permission).
- Policy naming: Proposal A (`MG_P_<role>_<VERB>_<SCOPE>`), role name ≤ 40 chars.
- Concurrency: last-write-wins.
- Admin authority: Java `database.isAdmin()` pre-check on every mutation (PG DDL failure is defense-in-depth only). `BYPASSRLS` is a separate concern (row visibility for admin sessions).
- `setPermissions` uses diff-and-patch (not blunt replace-all) in v1 — changed table → DDL, unchanged table → zero DDL. Required to keep wildcard lock blast proportional to actual changes rather than full set size.
- `deleteRole` uses **tombstone semantics**: PG role + policies + grants are dropped, but `role_metadata` row is flipped to `status='deleted'` (not removed) to reserve the name against reuse. Orphaned `mg_roles` values are benign because `current_user_roles()` cannot contain a non-existent name. Hard-purge of tombstones + `mg_roles` arrays is a backlog item.
- Session role cache via `SET LOCAL molgenis.current_roles = '<csv>'` issued by `SqlUserAwareConnectionProvider` alongside `SET ROLE`. `current_user_roles()` reads the GUC as fast path, falls back to `pg_auth_members` scan when unset. One catalog scan per request → zero catalog scans per policy evaluation.
- `role_metadata.schema_name` uses sentinel `'*'` (NOT NULL) for global custom roles — avoids NULL-in-PK and reuses the same wildcard token as `Permission.schema`.
- All feature DDL lives in `migration32.sql`. `MetadataUtils` gains Java CRUD helpers only; `init()` is not extended with `role_metadata` DDL.
- GraphQL mutations use EMX2's unified `change` / `drop` pattern, but on a **separate root pair** `changePermissions` / `dropPermissions` (admin-only, database-scoped) to avoid colliding with the existing `change(roles: ...)` arg that manages per-schema members. Input types carry `Molgenis` prefix.
- Session field named `effectivePermissions` (not `permissions`) to avoid collision with existing `tablePermissions` on `MolgenisSession`, which stays unchanged.
- Policy metadata is **not** stored in `COMMENT ON POLICY`. All policy attributes are derivable from the policy name + `pg_policies` columns + `role_metadata`.
- RLS columns and predicates use `session_user`, not `current_user`, so `SECURITY DEFINER` wrappers do not leak the definer into ownership/role checks.
- `ALTER TABLE … FORCE ROW LEVEL SECURITY` is issued alongside `ENABLE` so table-owner sessions do not silently bypass policies.
- `current_user_roles()` is declared `STABLE` to enable planner caching across rows in a policy evaluation.
- `enableRowLevelSecurity` creates a GIN index on `mg_roles`; `disableRowLevelSecurity` drops it.
- `deleteRole` pattern: revoke + `REASSIGN OWNED BY` + `DROP OWNED BY` + `DROP ROLE` + delete metadata, to avoid "role has default privileges" failures.
- `scope=all` replaces (drops) any narrower OWN/GROUP policy the same role held for the same verb on the same table — permissive policies OR together in PG, so coexistence would make narrower policies dead code.
- `Permission` is a Java `record`; `PermissionSet` wraps a `LinkedHashMap<String, Permission>` keyed on `schema + ":" + table`. "Set" in the name reflects replace-by-key semantics, not `java.util.Set`.
- Role name cap is **40 UTF-8 bytes** (`getBytes(UTF_8).length`), not Java chars.
- `PermissionSet.validate` takes a `Function<TableRef, Boolean> isRlsEnabled` so the base module stays free of `SqlDatabase` dependencies.
- Guard trigger body lives at `backend/molgenis-emx2-sql/src/main/resources/sql/rls/mg_reserved_column_guard.sql`, loaded at `SqlPermissionExecutor` class-init.
- Aggregate validation errors use `MolgenisException` populated with `List<MolgenisExceptionDetail>` (one detail per violation) — structured, not string-concatenated.
- GraphQL resolvers use `database.tx(...)`, not `dsl.transaction(...)`, to honour EMX2's `inTx` re-entry guard.
