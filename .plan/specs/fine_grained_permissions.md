# Spec: Fine-grained permission system (RLS via one role + policies)

Living guardrail for the fine-grained permission feature. Update when behaviour is added/changed.

## Vocabulary

- **Role** — named permission bundle. One PostgreSQL role per EMX2 role. Custom role naming: `MG_ROLE_<rolename>` (global).
- **User** — granted one or more roles via `GRANT <role> TO <user>`. Source of truth: `pg_auth_members`.
- **Permission** — tuple `(schema, table, select, insert, update, delete, change_owner, share)` belonging to a role.
- **Scope ladder** — `none | own | group | all`.
  - **own** — row where `mg_owner = session_user`.
  - **group** — row where `mg_roles && current_user_roles()` (array overlap on SELECT/UPDATE/DELETE); INSERT uses `<@` (subset — stricter).
  - **all** — every row in the table.
- **Built-in role** — system role (VIEWER, EDITOR, MANAGER, OWNER, AGGREGATOR, RANGE, EXISTS, COUNT); `isSystemRole()` flag on the `Role` record.
- **Custom role** — admin-created, mutable.
- **`SqlPermissionExecutor`** — static helpers under `org.molgenis.emx2.sql.rls`. Class is package-accessible within `.rls` (not truly package-private to `org.molgenis.emx2.sql`); `SqlRoleManager` in the parent package imports it. Modelled on `org.molgenis.emx2.sql.autoid.*` precedent. Not part of public API.

  Verb-to-PG-action naming convention:
  - Role lifecycle: `createPgRole`, `dropPgRole`, `grantRoleToUser`, `revokeRoleFromUser`
  - Privilege (table-level verb access): `grantTablePrivilege`, `revokeTablePrivilege`, `revokeAllTablePrivileges`
  - Policy (row filters): `createPolicy`, `dropPolicy`, `dropAllPolicies`, `readPolicies`
  - RLS toggle + trigger: `enableRowLevelSecurity`, `disableRowLevelSecurity`, `installGuardTrigger`, `dropGuardTrigger`
  - Bootstrap (once per DB): `installCurrentUserRolesFunction`

## Components (public surface)

- `org.molgenis.emx2.TablePermission` — Java `record`: `schema, table, select: Scope, insert: Scope, update: Scope, delete: Scope, changeOwner: boolean, share: boolean`. Wildcards allowed on schema/table (`"*"`). Record `equals`/`hashCode` cover all fields (true value-object semantics).
- `org.molgenis.emx2.PermissionSet` — composition, holds `LinkedHashMap<String, TablePermission>` keyed on `schema + ":" + table`. Methods: `put(TablePermission)` (replace-by-key), `remove(schema, table)`, `iterator/size/contains` + `validate(Function<TableRef, Boolean> isRlsEnabled): List<ValidationError>` + `resolveFor(schemaName, tableName): TablePermission` (applies union-most-permissive). The "Set" in the type name reflects replace-by-key semantics, not a `java.util.Set`.
- `org.molgenis.emx2.RoleManager` — interface in base module. Methods:
  - `createRole(name, description)` — global custom role
  - `deleteRole(name)`
  - `listRoles(): List<Role>`
  - `grantRoleToUser(role, user) / revokeRoleFromUser(role, user)`
  - `setPermissions(role, permissionSet)` (replace-all semantics, v1)
  - `getPermissions(role): PermissionSet`
  - `getPermissionsForActiveUser(): PermissionSet`
- `org.molgenis.emx2.sql.SqlRoleManager` — extends existing class, implements `RoleManager`. Legacy per-schema methods stay. `setPermissions` is used both by custom-role admin mutations AND by `SqlSchema.create` when installing built-ins (under schema-scoped role names). Internally it decides between GRANT and POLICY based on scope; OWN/GROUP scope triggers `ensureRlsInstalled` automatically. The emitter is a private concern.

## Data model

### Columns added to user tables (only when RLS enabled)

| Column | Type | Default | Populated by |
|---|---|---|---|
| `mg_owner` | text | `session_user` (PG column DEFAULT) | `session_user` survives `SECURITY DEFINER` wrappers (where `current_user` becomes the function definer). Java SqlTable may set explicitly. |
| `mg_roles` | text[] | `'{}'` (PG column DEFAULT) | App input; validated by policy WITH CHECK on INSERT |

Additionally, `enableRowLevelSecurity` creates a GIN index on `mg_roles`: `CREATE INDEX <table>_mg_roles_gin ON <table> USING GIN (mg_roles)` (dropped by `disableRowLevelSecurity`). Required for `&&` / `<@` operator performance on large tables.

### `MOLGENIS.permission_attributes` table (migration 32)

| Column | Type | Notes |
|---|---|---|
| `role_name` | varchar NOT NULL | base name without `MG_ROLE_` prefix |
| `schema_name` | varchar NOT NULL | schema the grant applies to |
| `table_name` | varchar NOT NULL | table the grant applies to |
| `change_owner` | boolean NOT NULL DEFAULT false | allows `mg_owner` mutation |
| `share` | boolean NOT NULL DEFAULT false | allows `mg_roles` mutation |

Primary key `(role_name, schema_name, table_name)`. Stores `change_owner`/`share` flags per `(role, schema, table)` for the guard trigger.

### `MOLGENIS.role_wildcards` table (migration 32)

| Column | Type | Notes |
|---|---|---|
| `role_name` | varchar NOT NULL | |
| `schema_pattern` | varchar NOT NULL DEFAULT `'*'` | wildcard schema |
| `table_pattern` | varchar NOT NULL DEFAULT `'*'` | wildcard table |
| `select_scope` | varchar | |
| `insert_scope` | varchar | |
| `update_scope` | varchar | |
| `delete_scope` | varchar | |
| `change_owner` | boolean NOT NULL DEFAULT false | |
| `share` | boolean NOT NULL DEFAULT false | |

Primary key `(role_name, schema_pattern, table_pattern)`. Stores wildcard permission entries.

Note: `role_metadata` table was NOT implemented. Role descriptions are stored as `COMMENT ON ROLE` on the PG role. System-role flag is derived by `SqlRoleManager.isSystemRoleByName` from the built-in name list.

### Migration 32 (`migration32.sql` in resources, bumps `SOFTWARE_DATABASE_VERSION` to 33)

Resource path: `backend/molgenis-emx2-sql/src/main/resources/org/molgenis/emx2/sql/migration32.sql`. Guard: `if (version < 33)`.

DDL created by migration 32:
- `CREATE OR REPLACE FUNCTION MOLGENIS.current_user_roles()` — STABLE; reads `molgenis.current_roles` GUC, falls back to `pg_auth_members` scan.
- `ALTER ROLE "MG_USER_admin" BYPASSRLS` — non-transactional; idempotent on rerun.
- `CREATE TABLE IF NOT EXISTS MOLGENIS.permission_attributes` (see above)
- `CREATE TABLE IF NOT EXISTS MOLGENIS.role_wildcards` (see above)
- `CREATE OR REPLACE FUNCTION MOLGENIS.mg_enforce_row_authorisation()` — BEFORE UPDATE trigger function enforcing `change_owner`/`share` constraints.

Note: `table_metadata` has no `row_level_security` column — RLS state is derived from `pg_class.relrowsecurity` and is managed automatically by permission grants.

## Session role cache (`molgenis.current_roles` GUC)

`SqlUserAwareConnectionProvider.setActiveUser` issues `SET LOCAL molgenis.current_roles = '<csv>'` right after `SET ROLE <user>`, where `<csv>` is the comma-joined list of roles the user is a member of (derived from `pg_auth_members`, cached per request). Connection return to pool → GUC resets automatically (it's `SET LOCAL`-scoped, or cleared by `RESET ALL` on pool return). Rationale: `current_user_roles()` is called once per row in `group`-scope policies; a memory-read GUC replaces a catalog scan per evaluation (two-order-of-magnitude win on large result sets).

Role names in EMX2 cannot contain commas (sanitized at creation), so `,` is safe as the delimiter. If the GUC is absent (raw psql admin session, or connection not managed by EMX2), the function falls back to a live `pg_auth_members` scan — correctness preserved, performance is the same as having no cache at all.

## Source of truth for permissions

`pg_policies` scan, filtered by `policyname LIKE 'MG_P_%'`. Role name, verb, and scope are encoded directly in the policy name (`MG_P_<role>_<VERB>_<SCOPE>`), so parsing is positional and unambiguous — no `COMMENT ON POLICY` is used. Schema and table come from `pg_policies` columns. `change_owner`/`share` flags live in `MOLGENIS.permission_attributes`. Wildcard entries live in `MOLGENIS.role_wildcards`. Table-level `GRANT`s (scope=all without RLS) read from `information_schema.role_table_grants`.

## Policy naming

`MG_P_<role>_<VERB>_<SCOPE>`

- `VERB` ∈ `SELECT | INSERT | UPDATE | DELETE` in v1. Reserved for future: `EXISTS | COUNT | GROUPBY`.
- `SCOPE` ∈ `OWN | GROUP | ALL`.
- Fixed overhead is 18 chars max (`MG_P_` + `_` + longest VERB (`DELETE`, 6) + `_` + longest SCOPE (`GROUP`, 5) = 18). Role name capped at 40 UTF-8 bytes → worst-case identifier = 58 bytes, within PG's 63-byte `NAMEDATALEN` limit.
- No `COMMENT ON POLICY` — metadata is fully derivable from name + `pg_policies` columns + `permission_attributes` / `role_wildcards` tables.

## Trigger & default policy strategy (defense in depth)

- **FORCE ROW LEVEL SECURITY**: `enableRowLevelSecurity` issues `ALTER TABLE <t> ENABLE ROW LEVEL SECURITY; ALTER TABLE <t> FORCE ROW LEVEL SECURITY`. `FORCE` is required because the app's JDBC connection may coincide with the table owner; without `FORCE`, the table owner session bypasses all policies regardless of `BYPASSRLS`. Admin session relies on its `BYPASSRLS` attribute (explicitly granted in migration 32).
- **INSERT**: native PG `mg_owner DEFAULT session_user`, `mg_roles DEFAULT '{}'`, plus policy `WITH CHECK`:
  - `insert:own` → `mg_owner = session_user`.
  - `insert:group` → `mg_roles <@ current_user_roles() AND cardinality(mg_roles) >= 1`. **Note asymmetry**: INSERT uses `<@` (subset — every value in `mg_roles` must be a role the inserter holds) while SELECT/UPDATE USING uses `&&` (overlap — row is visible if the caller shares *any* role with it). This is intentional: to *place* a row in a group, the inserter must be a member of every group they tag; to *read* a row, sharing one group suffices.
  - `insert:all` → `WITH CHECK (true)`.
- **UPDATE**: policy `USING` (row visibility) + policy `WITH CHECK` (post-update row still in scope). For `own` scope, both `USING` and `WITH CHECK` are `mg_owner = session_user` — the `WITH CHECK` is mandatory so that a policy-permitted UPDATE cannot move the row out of the caller's ownership undetected.
- **UPDATE — change_owner / share enforcement**: **one** BEFORE UPDATE trigger per RLS-enabled table (`mg_enforce_row_authorisation`), checks `OLD.mg_owner IS DISTINCT FROM NEW.mg_owner` and `OLD.mg_roles IS DISTINCT FROM NEW.mg_roles`, raises unless current user has `change_owner` / `share` in `MOLGENIS.permission_attributes`. Trigger function defined in `migration32.sql`. Uses `session_user`, not `current_user` (see `SECURITY DEFINER` consideration below).
- **DELETE**: policy `USING` matching scope; no trigger needed.
- **SECURITY DEFINER awareness**: EMX2 has existing `SECURITY DEFINER` helpers (e.g. `mg_generate_autoid`). Inside them `current_user` is the definer, not the caller. All RLS-sensitive predicates (`mg_owner` column DEFAULT, policy `USING`/`WITH CHECK`, guard trigger) use `session_user` to follow the caller, not the definer.
- **Principle**: policies + DEFAULTs + one guard trigger + `FORCE` means direct `psql` access is equally protected as Java access. Java layer adds nicer error messages and pre-flight validation but is not the enforcement boundary.

## Behaviours

| Behavior | Component | Test | Visual |
|---|---|---|---|
| Role name ≤ 40 chars accepted; 41+ rejected | `SqlRoleManager.createRole` (validate) | `SqlRoleManagerTest#roleNameLengthCap` | n/a |
| Role name collision with built-in rejected | `SqlRoleManager.createRole` | `SqlRoleManagerTest#rejectBuiltinNameCollision` | n/a |
| `createRole` creates PG role `MG_ROLE_<name>` and persists `role_metadata` row | `SqlRoleManager.createRole` | `SqlRoleManagerTest#createPersistsRoleAndPgRole` | n/a |
| `deleteRole` (tombstone semantics): drops all `MG_P_<role>_%` policies across schemas, revokes role from users, `REASSIGN OWNED BY <role> TO <admin>; DROP OWNED BY <role>; DROP ROLE <role>`, then sets `role_metadata.status = 'deleted'` + `deleted_on = now()` (row is kept, not deleted). Tombstone reserves the name against reuse. Orphaned `mg_roles` entries referencing the dead name are benign: `current_user_roles()` cannot contain a non-existent role, so `&&` never matches; and the tombstone blocks anyone creating a new role with the same name that would resurrect visibility. | `SqlRoleManager.deleteRole` → SqlPermissionExecutor `clearPolicies` + ownership cleanup | `SqlRoleManagerTest#deleteCascadesPoliciesAndMembers` | n/a |
| `createRole` rejects any name already present in `role_metadata` regardless of `status` (case-insensitive) — tombstones block reuse | `SqlRoleManager.createRole` | `SqlRoleManagerTest#createRejectsTombstonedName` | n/a |
| `listRoles()` returns only `status='active'` rows by default; an optional `includeDeleted` flag surfaces tombstones for admin audit | `SqlRoleManager.listRoles` | `SqlRoleManagerTest#listRolesExcludesTombstonesByDefault` | n/a |
| `deleteRole` on built-in rejected | `SqlRoleManager.deleteRole` | `SqlRoleManagerTest#immutableBuiltinsRejected` | n/a |
| `grantRoleToUser` = `GRANT MG_ROLE_<name> TO <user>` | `SqlRoleManager.grantRoleToUser` | `SqlRoleManagerTest#grantMembership` | n/a |
| `revokeRoleFromUser` = `REVOKE MG_ROLE_<name> FROM <user>` | `SqlRoleManager.revokeRoleFromUser` | `SqlRoleManagerTest#revokeMembership` | n/a |
| Only admin may invoke role mutation methods (v1) | `SqlRoleManager` admin guard | `SqlRoleManagerTest#nonAdminRejected` | n/a |
| Migration 32 bumps `SOFTWARE_DATABASE_VERSION` to 33, creates `permission_attributes` and `role_wildcards`, installs `current_user_roles()`, sets `BYPASSRLS` on admin, creates `mg_enforce_row_authorisation` trigger function | `Migrations.executeMigrationFile("migration32.sql", …)` | `MigrationsTest#migration32AppliesIdempotently` | n/a |

### Permission CRUD + invariants

| Behavior | Component | Test | Visual |
|---|---|---|---|
| `TablePermission` immutable; `schema`/`table` accept `"*"` | `TablePermission` | `TablePermissionTest#wildcardAccepted` | n/a |
| `PermissionSet.put` replaces on `(schema, table)` key (Map-semantics via internal `LinkedHashMap`) | `PermissionSet` | `PermissionSetTest#putReplacesByKey` | n/a |
| `PermissionSet.validate()` collects all invariant violations (no short-circuit) | `PermissionSet` | `PermissionSetTest#validateReturnsAllErrors` | n/a |
| `delete.scope > select.scope` → invariant error | `PermissionSet.validate` | `PermissionSetTest#deleteRequiresRead` | n/a |
| `update.scope > select.scope` → invariant error | `PermissionSet.validate` | `PermissionSetTest#updateRequiresRead` | n/a |
| `changeOwner=true` requires `update ≥ own` | `PermissionSet.validate` | `PermissionSetTest#changeOwnerRequiresUpdate` | n/a |
| `share=true` requires `update ≥ own` | `PermissionSet.validate` | `PermissionSetTest#shareRequiresUpdate` | n/a |
| `own`/`group` scope on any verb auto-installs RLS on the target table via `ensureRlsInstalled` | `SqlRoleManager.setPermissions` → `SqlPermissionExecutor.ensureRlsInstalled` | `SqlTableMetadataRlsTest#firstOwnGrantInstallsRls` | n/a |
| Validation is server-side rejection; client-side auto-upgrade deferred to UI | `PermissionSet.validate` | `PermissionSetTest#serverRejectsInsteadOfUpgrading` | n/a |
| `SqlRoleManager.setPermissions` uses **diff-and-patch**: reads current `PermissionSet` via `getPermissions(role)`, compares against payload, emits DROP/CREATE only for `(schema, table)` keys that changed. Unchanged entries → no DDL, no lock. Removed entries → drop only. Added entries → create only. | `SqlRoleManager.setPermissions` → SqlPermissionExecutor | `SqlRoleManagerTest#setPermissionsDiffPatchOnlyTouchesChanged` | n/a |
| `SqlRoleManager.setPermissions` wraps the diff application in one JOOQ transaction; mid-save failure rolls back so role never observed in half-saved state | `SqlRoleManager.setPermissions` | `SqlRoleManagerTest#setPermissionsTransactional` | n/a |
| Wildcard permission that shrinks from N tables to M unchanged tables takes ACCESS EXCLUSIVE on **0** tables (diff is empty for unchanged). Only changed tables are locked. | `SqlRoleManager.setPermissions` | `SqlRoleManagerTest#setPermissionsNoOpForUnchangedWildcard` | n/a |
| `SqlRoleManager.getPermissions(role)` scans `pg_policies` + comments → `PermissionSet` equal to what was saved | `SqlRoleManager.getPermissions` ← SqlPermissionExecutor `readPolicies` | `SqlRoleManagerTest#setPermissionsRoundTrip` | n/a |
| `PermissionSet.resolveFor(schema, table)` returns union-most-permissive of matching wildcard + specific entries | `PermissionSet.resolveFor` | `PermissionSetTest#resolveForUnionPermissive` | n/a |
| Wildcard `schema="*"` materialised on all existing matching schemas and on future ones (hook into schema-create) | `SqlRoleManager.setPermissions` + `SqlSchema.create` hook | `SqlRoleManagerTest#wildcardExistingAndFuture` | n/a |
| Last-write-wins concurrency (no ETag); two concurrent `setPermissions(role=R)` calls serialize on PG `ACCESS EXCLUSIVE` locks taken by `DROP POLICY`/`CREATE POLICY` on changed tables, so the final `pg_policies` state matches the last committer. Test asserts final state equals last-writer's payload, not merely "no error" | `SqlRoleManager.setPermissions` | `SqlRoleManagerTest#concurrentSaveLastWins` | n/a |
| Wildcard permission that genuinely changes N tables still takes `ACCESS EXCLUSIVE` on all N tables for the transaction duration; diff-and-patch only helps when tables are unchanged. Documented as a v1 limitation. | `SqlRoleManager.setPermissions` | n/a | n/a |
| `scope='schema'` rejected in v1 | `PermissionSet.validate` or `SqlRoleManager.createRole` | `PermissionSetTest#schemaScopeDeferred` | n/a |

### RLS emergent lifecycle

| Behavior | Component | Test | Visual |
|---|---|---|---|
| First OWN grant on a table auto-installs RLS: adds `mg_owner`, `mg_roles`, `ENABLE ROW LEVEL SECURITY`, `FORCE ROW LEVEL SECURITY`, GIN index, guard trigger `mg_enforce_row_authorisation` | `SqlRoleManager.setPermissions` → `SqlPermissionExecutor.ensureRlsInstalled` | `SqlTableMetadataRlsTest#firstOwnGrantInstallsRls` | n/a |
| First GROUP grant on a table auto-installs RLS (same as OWN) | `SqlRoleManager.setPermissions` → `SqlPermissionExecutor.ensureRlsInstalled` | `SqlTableMetadataRlsTest#firstGroupGrantInstallsRls` | n/a |
| `ensureRlsInstalled` is idempotent — calling twice does not error | `SqlPermissionExecutor.ensureRlsInstalled` | `SqlTableMetadataRlsTest#rlsInstallIdempotent` | n/a |
| Removing last OWN/GROUP policy (by clearing role permissions) drops only the policies; `mg_owner`, `mg_roles`, trigger, RLS enabled, GIN index remain | `SqlRoleManager.setPermissions` (clear) | `SqlTableMetadataRlsTest#lastOwnGroupRemovedDropsPoliciesKeepsRls` | n/a |
| ALL-scope grant does not trigger RLS install | `SqlRoleManager.setPermissions` | `SqlTableMetadataRlsTest#allScopeGrantDoesNotInstallRls` | n/a |

### GRANT / POLICY emission semantics (private to `SqlRoleManager`)

These behaviours are tested through `SqlRoleManager.setPermissions` + `getPermissions`; the emitter is not publicly addressable.

| Behavior | Component | Test | Visual |
|---|---|---|---|
| `scope=all` without RLS: native `GRANT <verb> ON <table> TO <role>`; no policy | `SqlRoleManager.setPermissions` | `SqlRoleManagerEmissionTest#allScopeUsesGrantOnly` | n/a |
| `scope=all` with RLS enabled: installs permissive policy `MG_P_<role>_<verb>_ALL USING (true)` **and drops any narrower OWN/GROUP policy the same role held for the same verb** (permissive policies are OR-ed by PG; the ALL policy would make narrower policies dead code, so setPermissions enforces one-scope-per-(role,verb,table)) | `SqlRoleManager.setPermissions` | `SqlRoleManagerEmissionTest#allScopeReplacesNarrower` | n/a |
| `scope=own` SELECT: policy `USING (mg_owner = session_user)` | `SqlRoleManager.setPermissions` | `SqlRoleManagerEmissionTest#selectOwn` | n/a |
| `scope=group` SELECT: policy `USING (mg_roles && current_user_roles())` (helper SQL function) | `SqlRoleManager.setPermissions` + `current_user_roles()` | `SqlRoleManagerEmissionTest#selectGroup` | n/a |
| INSERT policy uses `WITH CHECK`: own requires `mg_owner = session_user`; group requires `mg_roles <@ current_user_roles() AND cardinality(mg_roles) >= 1` (stricter than SELECT's `&&` — see trigger section for rationale); all = `(true)` | `SqlRoleManager.setPermissions` | `SqlRoleManagerEmissionTest#insertPolicies` | n/a |
| UPDATE policy uses both `USING` and `WITH CHECK` (post-update still in scope); for `own` both clauses are `mg_owner = session_user` | `SqlRoleManager.setPermissions` | `SqlRoleManagerEmissionTest#updatePoliciesUsingAndWithCheck` | n/a |
| DELETE policy: `USING` only | `SqlRoleManager.setPermissions` | `SqlRoleManagerEmissionTest#deletePolicy` | n/a |
| `getPermissions(role)` round-trips with `setPermissions` by parsing policy names and joining `role_table_grants` — no COMMENT parsing | `SqlRoleManager.getPermissions` | `SqlRoleManagerEmissionTest#roundTrip` | n/a |
| Clearing permissions for one `(role, schema, table)` drops only that role's policies + GRANTs on that table | `SqlRoleManager.setPermissions` (via replace-all with zeroed entry) | `SqlRoleManagerEmissionTest#clearScoped` | n/a |

### Row lifecycle (insert / update data semantics)

| Behavior | Component | Test | Visual |
|---|---|---|---|
| INSERT on RLS-enabled table: `mg_owner` defaults to `session_user` when user omits it (PG column DEFAULT) | PG column DEFAULT | `RowLifecycleTest#insertDefaultsOwner` | n/a |
| `insert:own` — WITH CHECK rejects INSERT if `mg_owner != session_user` | policy | `RowLifecycleTest#insertOwnBlocksForeignOwner` | n/a |
| `insert:group` — user must set `mg_roles` length ≥ 1 and all ∈ user's granted roles | policy | `RowLifecycleTest#insertGroupValidatesRoles` | n/a |
| `insert:all` — user may set `mg_owner`/`mg_roles` freely; omitted fields get DEFAULTs | policy + DEFAULTs | `RowLifecycleTest#insertAllDefaults` | n/a |
| UPDATE without `change_owner` — guard trigger raises if `NEW.mg_owner != OLD.mg_owner` | `mg_enforce_row_authorisation` | `RowLifecycleTest#updateWithoutChangeOwnerRejected` | n/a |
| UPDATE without `share` — guard trigger raises if `NEW.mg_roles != OLD.mg_roles` | `mg_enforce_row_authorisation` | `RowLifecycleTest#updateWithoutShareRejected` | n/a |
| `change_owner:own` — may transfer rows user owned; destination unrestricted | `mg_enforce_row_authorisation` | `RowLifecycleTest#changeOwnerOwnScope` | n/a |
| `change_owner:group` — may change owner on rows in user's group; destination must also be member of user's group | `mg_enforce_row_authorisation` | `RowLifecycleTest#changeOwnerGroupScope` | n/a |
| `change_owner:all` — unrestricted | `mg_enforce_row_authorisation` | `RowLifecycleTest#changeOwnerAllScope` | n/a |
| `share` (any scope) — new `mg_roles` values must all be roles user is member of | `mg_enforce_row_authorisation` | `RowLifecycleTest#shareLimitedToGrantedRoles` | n/a |

### Built-in roles + coexistence

| Behavior | Component | Test | Visual |
|---|---|---|---|
| `SqlSchema.create` installs built-ins (VIEWER/EDITOR/MANAGER/OWNER/AGGREGATOR/RANGE/EXISTS/COUNT) by iterating `Privileges.java` and calling `SqlRoleManager.setPermissions(<schema/role>, preset)` for each; result is identical PG `GRANT`s to the legacy code path since built-ins are scope=all | `SqlSchema.create` + `SqlRoleManager.setPermissions` | `BuiltinInstallTest#uniformAcrossSchemas` | n/a |
| Pre-existing schemas have legacy-installed PG built-in roles; no backfill of `role_metadata`; `listRoles` reconstructs built-ins by joining `Privileges.java` with `pg_roles` for both pre-feature and post-feature schemas | `SqlRoleManager.listRoles` | `BuiltinInstallTest#listBuiltinsUniform` | n/a |
| Custom role name colliding with any built-in privilege (case-insensitive) rejected | `SqlRoleManager.createRole` | `SqlRoleManagerTest#rejectBuiltinNameCollision` | n/a |
| Custom role operates on any schema regardless of age; own/group scope always requires the target table's `row_level_security=true` | `SqlRoleManager.setPermissions` + `PermissionSet.validate` | `SqlRoleManagerTest#customRoleOnAnySchema` | n/a |
| Deleting a built-in via `deleteRole` rejected (system roles cannot be dropped) | `SqlRoleManager.deleteRole` | `SqlRoleManagerTest#immutableBuiltinsRejected` | n/a |
| Drop schema / table cascades `MG_P_%` policies and built-in PG roles for that schema automatically (PG cascade) | PG | `SqlRoleManagerTest#schemaDropCascades` | n/a |
| Drop schema while a `schema:*` role exists silently shrinks effective permissions (no error) | `SqlRoleManager` | `SqlRoleManagerTest#schemaDropNoError` | n/a |

### Authorisation

Admin guard is **Java-first with PG defense-in-depth**: `SqlRoleManager` checks `database.getActiveUser()`/`isAdmin()` before any mutation and throws `MolgenisException` fast on a non-admin. PG DDL would also fail without privilege, but the Java guard gives a clean error. `BYPASSRLS` (migration 32) is a separate concern — it governs row visibility for admin sessions on RLS-enabled tables, not mutation authority.

| Behavior | Component | Test | Visual |
|---|---|---|---|
| Admin PG role carries `BYPASSRLS` attribute (via migration 32) | migration | `AdminBypassTest#adminBypassesRls` | n/a |
| Non-admin caller of role mutations rejected by Java `database.isAdmin()` pre-check (fast-fail before any DDL) | `SqlRoleManager` admin guard | `SqlRoleManagerTest#nonAdminRejected` | n/a |

## GraphQL surface (phase 2)

Follows the existing EMX2 pattern: **existing `change` and `drop` root mutations** extended with new argument lists for fine-grained permission management. No separate `changePermissions`/`dropPermissions` root fields. Both return `GraphqlApiMutationResult { status, message }`.

GraphQL enum types:
- `MolgenisEditScope` — values: `NONE | OWN | GROUP | ALL` (edit-verb scopes for select/insert/update/delete)
- `MolgenisSelectScope` — values: `FULL | COUNT | AGGREGATE | EXISTS | RANGE` (view modality; only `FULL` enforced in v1)

Input types:
- `MolgenisRoleInput { name: String!, description: String, permissions: [MolgenisPermissionInputFg!] }`
- `MolgenisPermissionInputFg { schema, table, select, insert, update, delete: MolgenisEditScope, changeOwner, share: Boolean, selectScope: MolgenisSelectScope }`
- `MolgenisRoleMemberInput { role: String!, user: String! }`

Output types:
- `MolgenisRoleOutput { role, description, systemRole: Boolean, permissions: [MolgenisRolePermissionsOutput], members: [String] }`
- `MolgenisEffectivePermission { schema, table, select, insert, update, delete: MolgenisEditScope, changeOwner, share: Boolean }`

Tests live in `org.molgenis.emx2.graphql.*`.

### Queries

| Behavior | Component | Test | Visual |
|---|---|---|---|
| `_session(schema) { permissions { schema table select insert update delete changeOwner share } }` returns current user's resolved permissions. Field named `permissions` (no collision found with existing `tablePermissions`). | `GraphqlSessionFieldFactory` + `SqlRoleManager.getPermissionsForActiveUser()` | `GraphqlPermissionFieldFactoryTest#sessionPermissions_currentUserSeesOwnPermissions` | n/a |
| `_admin { roles(name) { role description systemRole permissions {...} members } }` (admin-only) lists all roles with permission sets and members | `GraphqlAdminFieldFactory` using `roleOutputType` from `GraphqlPermissionFieldFactory` | `GraphqlPermissionFieldFactoryTest#adminRolesQuery_listsRolesAndPermissions` | n/a |
| `systemRole` field in role output is `true` for built-in roles, `false` for custom | `GraphqlAdminFieldFactory` → `Role.isSystemRole()` | `GraphqlPermissionFieldFactoryTest#adminRolesQuery_listsRolesAndPermissions` | n/a |
| Non-admin querying `_admin { roles }` receives exception | `GraphqlAdminFieldFactory` auth check | `GraphqlPermissionFieldFactoryTest#adminRolesQuery_nonAdminNotAccessible` | n/a |

### Mutation behaviours

| Behavior | Component | Test | Visual |
|---|---|---|---|
| `change(roles: [{name, description, permissions:[...]}])` creates or updates custom role(s) with nested permission set; admin-only | `GraphqlDatabaseFieldFactory` → `GraphqlPermissionFieldFactory.applyRoles` → `SqlRoleManager.createOrUpdateRole` + `setPermissions` | `GraphqlPermissionFieldFactoryTest#changeRoleDefinitions_createsRole` | n/a |
| `change(roles: [{name, permissions:[{schema, table, select, ...}]}])` replaces permission set for role | `GraphqlPermissionFieldFactory.applyPermissionsForRole` → `SqlRoleManager.setPermissions` | `GraphqlPermissionFieldFactoryTest#changePermissions_replaceAll` | n/a |
| `change(members: [{role, user}])` grants role→user | `GraphqlPermissionFieldFactory.applyMembers` → `SqlRoleManager.grantRoleToUser` | `GraphqlPermissionFieldFactoryTest#changeMembers_grantsRole` | n/a |
| `change` with roles + members in one call executes transactionally; order: roles → members; OWN/GROUP scopes in permissions auto-install RLS | `GraphqlDatabaseFieldFactory` wraps in `database.tx(...)` | `GraphqlPermissionsIT#fullScenario` | n/a |
| `drop(roles: [String!])` deletes custom role(s); system roles rejected | `GraphqlDatabaseFieldFactory` → `SqlRoleManager.deleteRole` | `GraphqlPermissionFieldFactoryTest#dropRoles_tombstonesRole` | n/a |
| `drop(members: [{role, user}])` revokes role→user | `GraphqlDatabaseFieldFactory` → `SqlRoleManager.revokeRoleFromUser` | `GraphqlPermissionFieldFactoryTest#dropMembers_revokesRole` | n/a |
| Non-admin invoking `change` or `drop` with permission arguments receives `FAILED` result with "admin" message | `GraphqlDatabaseFieldFactory` admin guard | `GraphqlPermissionFieldFactoryTest#nonAdminForbidden` | n/a |
| `selectScope` field in permission input accepts `MolgenisSelectScope` values; only `FULL` is passed through in v1 | `GraphqlPermissionFieldFactory.applyPermissionsForRole` | implicit in mutation tests | n/a |
| End-to-end: single `change` call (tables + roles + members) → non-admin session → `_session.permissions` correct → SELECT/INSERT/UPDATE enforced per scope → `drop` cleans up | `GraphqlPermissionsIT` | `GraphqlPermissionsIT#fullScenario` | n/a |

## Out of scope (v1 — phases 1+2)

- `agg` sub-ladder (`exists_*`, `count_*`, `groupBy_*`) — enum reserved, no policy code.
- Per-schema admin UI (phase 3 global admin only).
- Migration of legacy GRANT-based built-ins on existing schemas.
- Optimistic locking / etags on role mutations.
- Upgrade-old-schema action.
- Admin group membership model (today: PG superuser shortcut via `BYPASSRLS`).
- Schema-scoped custom roles (`scope='schema'` reserved in model, rejected at write-time).

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
- Guard-trigger permission lookup: cache caller's `change_owner`/`share` scope in a session GUC at request start (mirrors `molgenis.current_roles`) so the slow path avoids a `permission_attributes` scan per changed row.
- Tombstone/name-reservation semantics for deleted roles (v1 does not reserve names after drop).
