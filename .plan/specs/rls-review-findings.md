# RLS Review Findings — Consolidated Punch List

Branch: mswertz/poc/rls_using_one_role_and_policies
Reviewers: Java, JOOQ, SQL/Postgres, Security, Test-sufficiency
Date: 2026-04-25

## Verdict

3 BLOCKERs, 13 MAJORs, ~12 MINORs, 8 missing negative tests, 4 tests weakened vs master.
Most BLOCKERs are pre-existing on master (inherited via the AGGREGATE_COUNT_THRESHOLD port). Test weakening was introduced on this branch.
+ 2 pre-existing branch RLS bugs surfaced (carved out as Phase F).

## Convergent findings (multiple reviewers)

| # | Finding | Reviewers | Severity |
|---|---|---|---|
| C1 | AGGREGATE_COUNT_THRESHOLD broken three ways: default no-op, SUM bind dropped, no test, mutable global | Java, JOOQ, SQL, Security, Test | MAJOR (privacy) |
| C2 | EXISTS/COUNT/AGGREGATE/RANGE policies USING (true) — only Java enforces row visibility | SQL, Security | BLOCKER |
| C3 | GraphQL non-admin can escalate via setPermissions (becomeAdmin internally, no scope ceiling) | Java, Security | BLOCKER |
| C4 | Tests weakened vs master | Test-sufficiency | BLOCKER (regression of coverage) |

## Punch list

| ID | File:line | Severity | Finding | Reviewer | Suggested fix |
|---|---|---|---|---|---|
| J1 | backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlPermissionFieldFactory.java:205-215 | MAJOR | non-admin applyRoles bypasses admin check | Java | Add admin ceiling check before delegating to applyRoles |
| J2 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java:114 | MAJOR | deleteRole LIKE prefix collision (data vs data manager) | Java | Use word boundary or exact match instead of LIKE prefix |
| J3 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/rls/SqlPermissionExecutor.java:120-136 | MAJOR | changeOwner/changeGroup as USING (false) SELECT policies | Java | Replace fragile USING (false) marker with proper policy semantics |
| J4 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java:843-854 | MAJOR | schema-scoped getPermissions loses OWN/GROUP scope | Java | Preserve OWN/GROUP scope when filtering by schema |
| J5 | backend/molgenis-emx2/src/main/java/org/molgenis/emx2/PermissionSet.java:69-72 | MAJOR | wildcardSchema/wildcardTable variable names inverted | Java | Swap variable names to match intent |
| J6 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java:319-330 | MINOR | O(n^2) List.contains in hot path | Java | Replace List with Set for membership checks |
| J7 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java:227-246 | MINOR | no role-exists check before grant | Java | Check role exists before attempting grant |
| J8 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/rls/SqlPermissionExecutor.java:273-302 | MINOR | manual quoting in DDL construction | Java | Use JOOQ DSL quoting instead of manual string concatenation |
| J9 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/rls/SqlPermissionExecutor.java:340-345 | MINOR | no exhaustive switch on ColumnType | Java | Add default/exhaustive branch with explicit error |
| J10 | backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlPermissionFieldFactory.java:204 | MINOR | unchecked cast | Java | Add instanceof check before cast |
| J11 | backend/molgenis-emx2/src/main/java/org/molgenis/emx2/TablePermission.java:85-94 | MINOR | aliased Set (caller mutation visible) | Java | Return defensive copy |
| J12 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java:570-608 | MINOR | N+1 queries in permission listing | Java | Batch query instead of per-row lookup |
| J13 | backend/molgenis-emx2/src/main/java/org/molgenis/emx2/PermissionSet.java | MINOR | validate error message misleading | Java | Fix error message to match actual validation logic |
| Q1 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlQuery.java:930-932 | MAJOR | SUM template "SUM({0})" silently drops AGGREGATE_COUNT_THRESHOLD bind | JOOQ | Change template to `GREATEST(SUM({0}),{1})` and bind threshold as second param |
| Q2 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlQuery.java:863-867 | MAJOR | RANGE count uses Integer.class but expression returns bigint, overflows >2.1B | JOOQ | Use Long.class for RANGE count binding |
| Q3 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/rls/SqlPermissionExecutor.java:42,47 | MINOR | verb concat into DDL string | JOOQ | Use DSL.keyword() for all DDL fragments |
| Q4 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java:159 | MINOR | keyword(privilegeType) missing — raw string in DDL | JOOQ | Wrap privilegeType with DSL.keyword() |
| Q5 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/rls/SqlPermissionExecutor.java:273-310 | MINOR | manual quoting in policy DDL (dup of J8) | JOOQ | Use JOOQ DSL quoting |
| S1 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/rls/SqlPermissionExecutor.java:110-118 + backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTableMetadataExecutor.java:131-143 | BLOCKER | USING (true) for EXISTS/COUNT/AGGREGATE/RANGE — only Java layer enforces row visibility | SQL, Security | Replace with policies that deny raw row enumeration at SQL layer or use SECURITY DEFINER aggregate functions |
| S2 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlQuery.java:930-932 | MAJOR | SUM threshold silently dropped (dup of Q1) | SQL | Fix SUM template to bind threshold |
| S3 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/rls/SqlPermissionExecutor.java:340-357 | MAJOR | UPDATE OWN/GROUP WITH CHECK (true), trigger is the only barrier | SQL | Add WITH CHECK (mg_owner = session_user) on UPDATE_OWN and equivalent for GROUP |
| S4 | migration32.sql:3-17 | MAJOR | molgenis.current_roles GUC user-settable, impersonation for direct DB users | SQL | Revoke SET privilege or restrict GUC to superuser only |
| S5 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlQuery.java:30 | MAJOR | public static mutable AGGREGATE_COUNT_THRESHOLD causes test pollution | SQL | Replace with per-database config injection |
| S6 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java:443-471,495-528 | MINOR | TOCTOU on CREATE POLICY — no duplicate_object handling | SQL | Wrap in try/catch for duplicate_object exception |
| S7 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/rls/SqlPermissionExecutor.java:120-136 | MINOR | USING (false) marker fragile — relies on Java convention not enforced by DB | SQL | Document clearly or replace with structural approach |
| S8 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java | MINOR | index name asymmetry between create and drop paths | SQL | Align index names across create/drop |
| S9 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java | MINOR | current_user_roles marked STABLE but reads GUC that can change mid-transaction | SQL | Mark as VOLATILE or document the constraint |
| S10 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/rls/SqlPermissionExecutor.java | MINOR | disableRowLevelSecurity leaves column-level policies behind | SQL | Ensure column policies are also removed on disable |
| S11 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java:159 | MINOR | revokeAllGrantsForRole uses raw keyword(privilegeType) (dup of Q4) | SQL | Use DSL.keyword() consistently |
| E1 | backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlDatabaseFieldFactory.java:260-272 + backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlPermissionFieldFactory.java:199-234 | BLOCKER | schema MANAGER can grant arbitrary scopes on global roles — no scope ceiling enforced | Security | Add ceiling check: non-admin callers cannot grant scopes they do not hold |
| E2 | backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlDatabaseFieldFactory.java:244-278 + backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlPermissionFieldFactory.java:249-253 | MAJOR | applyMembers admin check split across two layers — easy to bypass if one layer is omitted | Security | Consolidate admin check into single authoritative location |
| E3 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/rls/SqlPermissionExecutor.java:104,111-117 | MAJOR | PERMISSIVE OR semantics with USING (true) for EXISTS/COUNT/etc (dup of S1/C2) | Security | See S1 fix |
| E4 | backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlQuery.java:30 | MAJOR | AGGREGATE_COUNT_THRESHOLD = MIN_VALUE = no-op default (dup of S5/C1) | Security | See S5 fix |
| E5 | backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlSchemaFieldFactory.java:582-584 | MAJOR | _schema.roles enumerates full role catalog to any authenticated user | Security | Filter to roles the requesting user holds, or restrict to MANAGER/OWNER |
| T1 | backend/molgenis-emx2-webapi/src/test/java/org/molgenis/emx2/web/TablePermissionsGraphqlTest.java:449-453 | BLOCKER | test 13 weakened to body.contains("ALL") only — master asserted specific permission structure | Test-sufficiency | Revert to master's stricter assertion or replace with equivalent |
| T2 | backend/molgenis-emx2-nonparallel-tests/src/test/java/org/molgenis/emx2/sql/FineGrainedPermissionsIT.java:165-176 | BLOCKER | accessDenied OR afterReset==0 too weak — passes when either condition holds | Test-sufficiency | Require both conditions independently |
| T3 | backend/molgenis-emx2-sql/src/test/java/org/molgenis/emx2/sql/SqlQuerySelectModeEnforcementTest.java:275 | MAJOR | json.contains("0") trivially true due to weight values present in response | Test-sufficiency | Assert on a value that is only present when count is suppressed |
| T4 | backend/molgenis-emx2-sql/src/test/java/org/molgenis/emx2/sql/SelectScopeIT.java:169 | MAJOR | json.contains("0") trivially true (same issue as T3) | Test-sufficiency | Assert on a value that is only present when count is suppressed |
| T5 | backend/molgenis-emx2-sql/src/test/java/org/molgenis/emx2/sql/SqlQuery.java | MAJOR | no test sets AGGREGATE_COUNT_THRESHOLD to a positive value and verifies COUNT and SUM are clamped | Test-sufficiency | Add test with positive threshold asserting both COUNT and SUM are floored |
| T6 | backend/molgenis-emx2-sql/src/test/java/org/molgenis/emx2/sql/SqlQuerySelectModeEnforcementTest.java:220-231 | MAJOR | assertNotNull(json) only — does not verify response content | Test-sufficiency | Assert on specific fields in the JSON response |

## Missing negative tests

The following permission paths have no test that verifies an unauthorized caller is rejected:

1. Non-admin user calling setPermissions on a global role — should be rejected with authorization error.
2. Schema MANAGER granting a scope (e.g., ADMIN) they do not hold — should be rejected.
3. Direct database user setting molgenis.current_roles GUC to impersonate another role — should be rejected or ignored by policies.
4. SELECT via EXISTS policy by a user who holds no schema role — should return empty result, not error.
5. Aggregate query by a user with AGGREGATE scope returning raw row data via SUM — should be floored by threshold.
6. UPDATE on a row not owned by the caller under UPDATE_OWN policy — should be rejected at DB layer, not just application layer.
7. Caller with GROUP scope reading rows belonging to a different group — should be denied.
8. _schema.roles query by a viewer-level user — should not enumerate roles the user does not hold.

## Tests weakened vs master

The following tests were stricter on master and were weakened on this branch:

| ID | Master assertion | Branch assertion | Master:line | Branch:line |
|---|---|---|---|---|
| W1 | TablePermissionsGraphqlTest test 13: asserted specific permission structure in response body | Weakened to `body.contains("ALL")` only | TablePermissionsGraphqlTest.java (master) | TablePermissionsGraphqlTest.java:449-453 |
| W2 | FineGrainedPermissionsIT: asserted accessDenied AND afterReset==0 independently | Weakened to `accessDenied OR afterReset==0` | FineGrainedPermissionsIT.java (master) | FineGrainedPermissionsIT.java:165-176 |
| W3 | SqlQuerySelectModeEnforcementTest: asserted specific suppressed-count value | Weakened to `json.contains("0")` (trivially true) | SqlQuerySelectModeEnforcementTest.java (master) | SqlQuerySelectModeEnforcementTest.java:275 |
| W4 | SelectScopeIT: asserted specific suppressed-count value | Weakened to `json.contains("0")` (trivially true) | SelectScopeIT.java (master) | SelectScopeIT.java:169 |

## Recommendation

### PHASE A — Restore broken test coverage (must do; introduced on this branch)

- [x] W1 (TablePermissionsGraphqlTest Order 3): replaced OR-substring with per-field assertions `"insert":"NONE"`, `"update":"NONE"`, `"delete":"NONE"` adapted to branch `permissions { select insert update delete }` API.
- [x] T1 (TablePermissionsGraphqlTest Order 13 `revokeInsertShowsCorrectSessionPermission`): added second grant that drops insert, then asserts `"insert":"NONE"` and `"update":"ALL"` per-field.
- [x] W2 / T2 (FineGrainedPermissionsIT:165): changed `assertTrue(accessDenied || afterReset == 0, ...)` to `assertTrue(accessDenied, ...)` — permission-denied is now required, not optional.
- [x] A1.a `grantWithFalseIsDistinguishableFromNull` (TestTableRoleManagement): kept `revokeInsertLeavesSelectIntact` AND added new `explicitEmptyGrantDiffersFromNeverGranted` that proves insert is denied at DB level after explicit revoke.
- [x] A1.a `multipleGrantsOnSameTableAreMergedForActiveUser` → replaced by `permissionsFromMultipleRolesAreUnionedForActiveUser`: branch deliberately keeps REPLACE semantics for `Schema.grant()` (a second `grant()` call on the same role wholly replaces, not merges). The master test relied on two sequential `grant()` calls to the same role merging — invalid under REPLACE. The replacement test proves the still-valid behavior: when a user holds two **distinct** roles (`SelectRole` with select ALL, `InsertRole` with insert ALL), `getPermissionsForActiveUser()` returns the session-level union (both select and insert are present). This is enforced by PostgreSQL role inheritance, independent of grant semantics.
- [x] A1.a `anonymousViewerAndCustomRolePermissionsAreMerged`: restored insert-only grant for `InsertOnly` role (no select), relying on anonymous Viewer for select.
- [ ] W3 (SqlQuerySelectModeEnforcementTest.java:275): not in scope for this task — T3/T4 are MAJOR not BLOCKER and involve production code (AGGREGATE_COUNT_THRESHOLD), deferred to Phase B.
- [ ] W4 (SelectScopeIT.java:169): same as W3, deferred to Phase B.
- Estimated: 1 day. Low risk, additive to test files.

### PHASE B — Fix the AGGREGATE_COUNT_THRESHOLD design (DONE)

- [x] B1: Fixed JOOQ SUM template — SUM is now suppressed (NULL) when group COUNT < threshold. Semantic chosen: privacy suppression (not floor) — returning a fake sum would be meaningless and could mislead. `CASE WHEN COUNT(*) >= threshold THEN SUM(col) ELSE NULL END`.
- [x] B2: Replaced `public static int AGGREGATE_COUNT_THRESHOLD` with per-database config. `SqlQuery.getAggregateCountThreshold()` reads `database.getSetting(MOLGENIS_AGGREGATE_COUNT_THRESHOLD)`; returns Integer.MIN_VALUE (disabled) when not set. Constant `MOLGENIS_AGGREGATE_COUNT_THRESHOLD` added to Constants.java.
- [x] B3: Javadoc on `getAggregateCountThreshold()` in SqlQuery.java documents the privacy-floor semantic (≤100 chars).
- [x] Tests: `TestAggregatePermission` updated — removed global static mutation; added three new tests: `threshold_clamps_small_group_count_upward`, `threshold_suppresses_sum_for_groups_below_threshold`, `threshold_disabled_returns_real_sum`. All use `db.setSetting`/`db.removeSetting` with proper try/finally teardown.

### PHASE C — Defence-in-depth for RLS (BLOCKER on master, inherited)

- Replace `USING (true)` for EXISTS/COUNT/AGGREGATE/RANGE (SqlPermissionExecutor.java:110-118, SqlTableMetadataExecutor.java:131-143) with policies that deny raw row enumeration at the SQL layer (e.g., `USING (false)` and route aggregate queries through SECURITY DEFINER functions, or add RESTRICTIVE policies).
- Set `WITH CHECK (mg_owner = session_user)` on UPDATE_OWN and equivalent for GROUP (SqlPermissionExecutor.java:340-357).
- Document the `molgenis.current_roles` GUC (migration32.sql:3-17) and either revoke SET privilege or remove the override path.
- Estimated: 1 week. Coordinate with master maintainers since these are pre-existing bugs.

### PHASE D — Code quality (do as cleanup PRs)

- Fix LIKE prefix collision in deleteRole at SqlRoleManager.java:114 (use word boundary or exact match).
- Fix RANGE count Integer/bigint overflow at SqlQuery.java:863-867 (use Long.class).
- Add scope ceiling check to non-admin setPermissions path at GraphqlPermissionFieldFactory.java:205-215.
- Restrict _schema.roles visibility to MANAGER/OWNER or filter to roles the user holds (GraphqlSchemaFieldFactory.java:582-584).
- Add TOCTOU protection on CREATE POLICY at SqlRoleManager.java:443-471,495-528 (try/catch duplicate_object).
- Address minor JOOQ pattern violations (Q3, Q4, Q5).
- Estimated: 2-3 days, splittable across multiple PRs.

### PHASE E — Defer / discuss

- The 8 missing negative tests (section above), prioritized by which permission paths they cover.
- The variable-name inversion in PermissionSet.java:69-72 (cosmetic but risk-of-future-bug).

## Recommended scope for THIS branch

- PHASE A: required (we caused it).
- PHASE B: strongly recommended (we ported a broken feature without fixing it).
- PHASE C: out of scope (master bug, raise issue separately).
- PHASE D: opportunistic cleanup; can split off.

## Phase F — Pre-existing branch RLS bugs surfaced during review (CLOSED)

Two test failures observed during Phase A+B verification turned out to be pre-existing on this branch (not master), and not caused by our Phase A/B changes. Both fixed.

### F1. `FineGrainedPermissionsIT.fullScenario` — INSERT_OWN policy rejects own-row insert (FIXED)

- Root cause: `SqlPermissionExecutor.java` constants `USING_OWN` and `WITH_CHECK_INSERT_OWN` used `session_user` instead of `current_user`. After `SET ROLE`, PostgreSQL keeps `session_user` as the login role (admin), while `current_user` reflects the active role. Same bug in `mg_enforce_row_authorisation.sql` trigger for OWN-scope ownership checks.
- Fix: Changed `session_user` → `current_user` in:
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/rls/SqlPermissionExecutor.java` (lines 26, 31)
  - `backend/molgenis-emx2-sql/src/main/resources/org/molgenis/emx2/sql/utility-sql/mg_enforce_row_authorisation.sql` (trigger CHANGEOWNER_OWN and CHANGEGROUP_OWN conditions)
  - Updated 4 assertions in `SqlRoleManagerEmissionTest` that were testing for the buggy `session_user` behavior.
- Verified: `FineGrainedPermissionsIT.fullScenario()` PASSED; `RowLifecycleTest` all PASSED.

### F2. `TablePermissionsGraphqlTest._session(schema:)` — schema-scoped permissions missing (FIXED)

- Root cause A: `GraphqlSessionFieldFactory.buildPermissions` called `getPermissionsForActiveUser()` (which excludes schema-scoped roles filtered by `NOT LIKE 'MG_ROLE_%/%'`) even when `schemaFilter != null`. Fixed by routing to `getTablePermissionsForActiveUser(schemaFilter)`.
- Root cause B: `SqlRoleManager.getTablePermissionsForActiveUser` called `database.getSchema(schemaName)` as the active (non-admin) user, which returned null due to schema metadata RLS. Fixed with `getSchemaAsAdmin` helper that temporarily escalates to admin.
- Root cause C: Custom schema roles inherit from `Exists` (GRANT existsRole TO customRole in createSchemaRole). This caused `getInheritedRolesForUser` to include `Exists`, whose wildcard `*` permissions were expanded to all tables. Fixed by filtering `EXISTS`-only permissions out of `_session.permissions` display in `buildPermissions` (isExistsOnly predicate).
- Fix files:
  - `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/RoleManager.java`: added `getTablePermissionsForActiveUser(String schemaName)` to interface.
  - `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlSessionFieldFactory.java`: route to `getTablePermissionsForActiveUser` when schemaFilter set.
  - `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java`: `getTablePermissionsForActiveUser` implementation, returns `PermissionSet`.
- Verified: `TablePermissionsGraphqlTest` all 15 tests PASSED.

### F2 — Future work

- Dedicated METADATA scope (separate from EXISTS) to allow seeing table existence/columns without row-level access; would simplify `getTablePermissionsForActiveUser` schema lookup.
- Ontology table auto-grant convention: when a ref points to an ontology table, auto-grant SELECT on that ontology table. Defer to admin scripts for now; revisit later.
- "Ref" / refLabel-only permission: allow seeing a ref column (and only the fields used in its refLabel) based on visibility of the referenced row. Future scope.

### F3 — Test isolation audit (follow-up, not in this branch)

- Global PG roles (created via `roleManager.createRole(name)`, no schema scope) live database-wide, so identical role names across test classes collide on parallel runs and reruns. Schema isolation alone does not fix this.
- Audit all test classes that create global roles. Either:
  - Prefix global role names with the test class simple name (e.g. `SqlRoleManagerTest_nodrop_role`), or
  - Switch to schema-scoped roles (`schema.createRole(...)` → auto-removed when the schema is dropped) where the test does not specifically need global semantics.
- For tests that genuinely need global-role semantics across schemas (e.g. `schemaDropNoError`), move them into the existing `molgenis-emx2-nonparallel-tests` module so they cannot interleave with other suites.
- Convention: clean at start (`@BeforeEach`), not at teardown — eases debugging by leaving state inspectable after a failure. Already followed in `SqlRoleManagerTest` after the LIKE-pattern fix.
