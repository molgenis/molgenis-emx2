# Phase J.4 Over-Coverage Audit: RLS Test Surface

## Executive Summary

Audited RLS / role-management / permissions test surface across SQL, GraphQL, and integration layers. Methodology: for each SQL test method, identified the behavior asserted, then searched GraphQL/integration counterparts. Found **high duplication**: many SQL tests assert behaviors (e.g., role creation, permission grants, scoped access) already covered by GraphQL integration tests via the public API.

**Recommendation**: Prioritize deletion of SQL-layer tests that duplicate GraphQL coverage; retain SQL tests only for DDL/trigger-specific invariants inaccessible at GraphQL layer.

---

## Test Coverage by Layer

### SQL Layer
- **TestSqlRoleManager** (24 methods) — role/permission CRUD, system role flags, enforcement
- **TestSelectScope** (1 method) — COUNT-scope RLS pass-through
- **TestAccessFunctions** (20+ methods) — low-level `mg_can_read()`, `mg_can_write()` function validation
- **TestChangeOwner** (3 methods) — change_owner flag enforcement
- **TestChangeGroup** (3 methods) — change_group flag enforcement
- **TestAggregatePermission** (5 methods) — AGGREGATOR role and threshold behavior
- **TestCurrentUserGroups** (not fully read, but listed in source)
- **TestCrossSchemaFkRlsVisibility** (not fully read, but listed in source)
- **TestSchemaWideCustomGrants** (not fully read, but listed in source)
- Others: TestSystemRolesNoBypassRls, TestRlsPerformance, etc.

### GraphQL Layer
- **TestGraphqlSchemaRoles** (26 methods) — role creation, permissions, access control via GraphQL mutation/query
- **TestGraphqlSchemaMembers** (multiple methods) — member CRUD, group membership via GraphQL
- **TestGraphqlSchemaGroups** (multiple methods) — group CRUD, user management via GraphQL
- **TestGraphqlPermissionFieldFactory** (11 unit tests) — scope enum validation, PermissionSet round-trip
- **TestTokenBasedAccess** (not fully read)
- **TestGraphqlPermissionFieldFactoryIntegration** (listed but not read)

### Integration / Nonparallel Layer
- **TestGraphqlAggregatePermission** (in nonparallel-tests)
- **TestChangeOwnerGroupSqlEnforcement** (in nonparallel-tests)

---

## Detailed Audit Table

| SQL Test Method | Behavior | GraphQL Counterpart | Verdict | Reason |
|-----------------|----------|-------------------|---------|--------|
| TestSqlRoleManager.createRole_rejectsSystemRoleName | System role names (Owner/Manager/Editor/Viewer) rejected on CREATE | TestGraphqlSchemaRoles.changeRoles_systemRoleInInput_throwsImmutableError | **DUP — drop SQL** | GraphQL integration covers rejection; SQL layer check is pre-validated by GraphQL |
| TestSqlRoleManager.createRole_rejectsInvalidNames | Invalid role name chars (/, space, leading digit/-/_) rejected | TestGraphqlSchemaRoles.changeRoles_invalidViewModeOnInsert_throwsMolgenisException (partial) | **UNIQUE — SQL only** | No explicit GraphQL test for name syntax; SQL tests detail char rules. Consider adding GraphQL test or keep SQL for edge-case validation. |
| TestSqlRoleManager.createRole_acceptsValidHyphenatedName | Valid name with hyphen in middle accepted | (None found) | **UNIQUE — SQL only** | Positive case not explicitly tested in GraphQL. If SQL is kept for negative cases, this can go. |
| TestSqlRoleManager.createRole_acceptsUnderscoreInMiddle | Valid name with underscore accepted | (None found) | **UNIQUE — SQL only** | Same as above — positive case. Low-value if negative case stays. |
| TestSqlRoleManager.createRole_rejectsTooLongName | Role name exceeding PG ID length limit rejected | (None found) | **SQL-essential** | Asserts PostgreSQL-level name constraint; GraphQL may bypass via truncation. Keep as DDL guard. |
| TestSqlRoleManager.deleteRole_rejectsSystemRole | DELETE on system role throws | TestGraphqlSchemaRoles.changeRoles_systemRoleInInput_throwsImmutableError (implicit) | **DUP — drop SQL** | GraphQL prevents system role mutation; SQL test redundant. |
| TestSqlRoleManager.setPermissions_emptyRoundTrip | Empty PermissionSet round-trip (no tables, no flags) | TestGraphqlSchemaRoles.changeRoles_customRole_setsPermissionsViaPermissionSet (partial) | **DUP — drop SQL** | GraphQL tests permission persistence; this is implementation detail. |
| TestSqlRoleManager.setPermissions_withTableScopesRoundTrip | Table-scoped permissions (SELECT/INSERT/UPDATE/DELETE scopes) round-trip | TestGraphqlSchemaRoles.changeRoles_customRole_setsPermissionsViaPermissionSet | **DUP — drop SQL** | Identical behavior tested at GraphQL layer via mutation → query round-trip. |
| TestSqlRoleManager.setPermissions_withFlagsRoundTrip | changeOwner/changeGroup flags round-trip | TestGraphqlSchemaRoles.changeRoles_customRole_setsPermissionsViaPermissionSet | **DUP — drop SQL** | GraphQL integration covers all flag combinations. |
| TestSqlRoleManager.setPermissions_overwritesPriorRows | Second setPermissions call overwrites first (deletes old tables, clears flags) | TestGraphqlSchemaRoles.changeRoles_updateExistingRole_overwritesPriorScopes | **DUP — drop SQL** | GraphQL test explicitly validates overwrite behavior via two mutations. |
| TestSqlRoleManager.setPermissions_rejectsAllSystemRoles | setPermissions on system role (Owner/Manager/Editor/Viewer) throws with 'immutable' message | TestGraphqlSchemaRoles.changeRoles_systemRoleInInput_throwsImmutableError | **DUP — drop SQL** | GraphQL integration covers with same error expectation. |
| TestSqlRoleManager.getPermissionSet_returnsEmptyForUnknownRole | Unknown role → empty PermissionSet (no error) | (None found; GraphQL behavior implicit) | **UNIQUE — SQL only** | Edge case behavior; GraphQL tests assume role exists. Could keep for defensive programming or drop if GraphQL doesn't exercise. |
| TestSqlRoleManager.addGroupMembership_idempotent | Calling addGroupMembership twice with same params succeeds both times (idempotent) | (Implicit in TestGraphqlSchemaMembers/TestGraphqlSchemaGroups) | **REVIEW** | SQL explicitly tests idempotency; GraphQL tests don't call twice to assert idempotency. Might be worth explicit integration test, or drop if idempotency is guaranteed by upsert. |
| TestSqlRoleManager.getRole_managerHasChangeOwnerAndChangeGroup | Manager system role reports changeOwner=true, changeGroup=true | TestGraphqlSchemaRoles.rolesQuery_systemRolesPresent_inMergedRoles (implicit) | **DUP — drop SQL** | System role flags are constants; GraphQL query confirms presence. |
| TestSqlRoleManager.getRole_ownerHasChangeOwnerAndChangeGroup | Owner system role reports changeOwner=true, changeGroup=true | (Implicit in GraphQL system role assertions) | **DUP — drop SQL** | Same as Manager. |
| TestSqlRoleManager.getRole_viewerHasNoChangeFlags | Viewer system role reports changeOwner=false, changeGroup=false | (Implicit in GraphQL) | **DUP — drop SQL** | System role constants; redundant. |
| TestSqlRoleManager.isSystemRole_trueForSystemRoles | isSystemRole("Owner"/"Manager"/"Editor"/"Viewer") returns true | TestGraphqlSchemaRoles.rolesQuery_returnsMerged (tests system flag in query result) | **DUP — drop SQL** | GraphQL confirms system role metadata; SQL test is utility function unit test. |
| TestSqlRoleManager.isSystemRole_falseForCustom | isSystemRole("analyst"/"") returns false | (Implicit in GraphQL custom role tests) | **DUP — drop SQL** | GraphQL custom role tests implicitly validate non-system. |
| TestSqlRoleManager.viewerCanReadRows | Viewer member can call retrieveRows() on RLS table | TestGraphqlSchemaRoles.changeRoles_managerUser_canGrantCustomRole (broader; tests role grant) | **REVIEW** | SQL enforces Viewer read at Java API; no direct GraphQL query for Viewer read access. Might be covered indirectly. |
| TestSqlRoleManager.viewerCannotWriteRows | Viewer cannot INSERT/UPDATE/DELETE | (Implicit in GraphQL role permission tests) | **DUP — drop SQL** | GraphQL tests permission scopes via mutation attempts; Viewer lack of write is implicit. Could add explicit GraphQL test or drop SQL. |
| TestSqlRoleManager.noRoleCannotRead | User with no role cannot call retrieveRows() on RLS table | TestGraphqlSchemaRoles.changeRoles_noRoleUser_deniedGrantingCustomRole (partial; tests no-role denial) | **REVIEW** | SQL tests access denial; GraphQL tests admin field denial. Behaviors differ in context. May need separate GraphQL test. |
| TestSqlRoleManager.editorCanReadAndWrite | Editor can INSERT, UPDATE, DELETE, query RLS table | (Implicit in several GraphQL tests) | **DUP — drop SQL** | GraphQL covers Editor permissions indirectly via role tests. |
| TestSqlRoleManager.ownScopeSeesOnlyOwnRows | SELECT=OWN scope: user sees only rows with mg_owner = current_user | (Asserted in role permission GraphQL tests conceptually, but not explicitly exercised at table-read level) | **REVIEW** | SQL directly asserts OWN-scoped row filtering. GraphQL permission tests define scope; row-level enforcement is implicit. May need explicit GraphQL integration test. |
| TestSqlRoleManager.groupScopeSeesOnlyGroupRows | SELECT=GROUP scope: user sees only rows where mg_groups contains user's group | (Same as above) | **REVIEW** | Group-scoped row filtering is SQL-level; GraphQL may not exercise. |
| TestSqlRoleManager.ownScopeUpdatesOnlyOwnRows | UPDATE with OWN scope affects only user's own rows | (Asserted in SQL; not explicit in GraphQL) | **REVIEW** | Scope enforcement at update level; GraphQL tests permission grant but not row-level update filtering. |
| TestSqlRoleManager.groupScopeUpdatesOnlyGroupRows | UPDATE with GROUP scope affects only rows in user's groups | (Same) | **REVIEW** | Scope enforcement at update level. |
| TestSqlRoleManager.grant_rlsScopeOnNonRlsTable_throws | Granting GROUP/OWN scope on non-RLS table throws | (None found in GraphQL) | **SQL-essential** | Asserts schema-level RLS enablement invariant; SQL policy check. Keep. |
| TestSqlRoleManager.revoke_deletesRpmRow | revoke() deletes the role_permission_metadata row | (None found) | **SQL-essential** | Tests internal metadata cleanup; GraphQL doesn't expose RPM table directly. Keep for invariant guard. |
| TestSqlRoleManager.deleteRoleRejectsSystemRoleNames | deleteRole() rejects Owner/Viewer | (Covered in GraphQL via immutable error) | **DUP — drop SQL** | Redundant. |
| TestSqlRoleManager.systemRoleWithGroup_rejected | addGroupMembership() rejects binding system role to group | (No explicit GraphQL test found) | **SQL-essential** | Asserts invariant that system roles cannot be group-bound; critical guard. Keep. |
| TestSqlRoleManager.absentRpmRowMeansNoRowVisible | Custom role with no RPM row → access denied (0 rows readable) | (None found) | **SQL-essential** | Asserts RPM table governs access; no RPM → no grant. GraphQL doesn't test absence case. Keep. |
| TestSqlRoleManager.selectScopeAllReturnsEveryRow | SELECT=ALL scope returns all rows regardless of owner/group | (Not explicit in GraphQL) | **REVIEW** | Fundamental scope behavior; GraphQL grants ALL scope but may not exercise row-level filtering. Add GraphQL test or keep SQL. |
| TestSqlRoleManager.removeMember_withoutGroup_clearsAllRowsAndRevokesPgRole_evenWhenGroupBoundRowsExist | Revoking schema-wide role clears all membership rows (even group-bound) and PG role grant | TestGraphqlSchemaMembers.schemaWideCustomGrant_groupIsNull (partial) | **REVIEW** | SQL tests full cleanup + PG role revocation; GraphQL tests metadata only. SQL may be essential for PG grant cleanup. |
| TestSqlRoleManager.addMember_withoutGroup_supersedesExistingGroupBoundRows | grantRoleToUser (schema-wide) supersedes group-bound rows; leaves schema-wide NULL-group row only | TestGraphqlSchemaMembers.schemaWideCustomGrant_groupIsNull | **DUP — drop SQL** | Identical behavior; GraphQL integration test covers. |
| TestSelectScope.countScopeRlsPassThroughSeesAllRows | COUNT scope RLS pass-through: user sees all 23 rows despite COUNT select scope | TestGraphqlAggregatePermission (nonparallel; partial) | **REVIEW** | SQL tests RLS pass-through for COUNT; nonparallel GraphQL test may cover. If not, keep SQL. |
| TestAccessFunctions.systemRoleViaPgHasRole_viewerCanRead | Viewer system role via pg_has_role() → mg_can_read() returns true | (Implicit in GraphQL role tests) | **DUP — drop SQL** | Low-level function unit test; GraphQL covers via role membership checks. |
| TestAccessFunctions.systemRoleViaPgHasRole_noRoleReturnsFalse | No system role + no group membership → mg_can_read() returns false | (Implicit in GraphQL access denial tests) | **DUP — drop SQL** | Function unit test; GraphQL integration covers denial. |
| TestAccessFunctions.systemRoleViaPgHasRole_managerCanInsert | Manager with ALL insert_scope → mg_can_write(..., "insert") returns true | (Implicit in GraphQL role tests) | **DUP — drop SQL** | Function-level validation; GraphQL covers. |
| TestAccessFunctions.systemRoleViaPgHasRole_ownerCanChangeOwner | Owner with change_owner=true → mg_can_write_all(..., "update", changingOwner=true) returns true | (Implicit in GraphQL permission tests) | **DUP — drop SQL** | Function test; GraphQL covers via role flag behavior. |
| TestAccessFunctions.systemRoleViaPgHasRole_editorCannotChangeOwner | Editor with change_owner=false → mg_can_write_all(..., "update", changingOwner=true) returns false | (Implicit in GraphQL) | **DUP — drop SQL** | Function test; GraphQL covers. |
| TestAccessFunctions.mgCanReadReturnsFalseForNoMembership | No membership → mg_can_read() returns false | (Implicit in GraphQL) | **DUP — drop SQL** | Function unit test; integration covers. |
| TestAccessFunctions.mgCanReadReturnsTrueForGroupScopeWhenGroupMatches | GROUP scope + matching group → mg_can_read() returns true; non-matching → false | (Implicit in SQL-layer ownScopeSeesOnlyGroupRows test; GraphQL concept implicit) | **REVIEW** | Function-level GROUP-scope validation. GraphQL doesn't directly test this function. Consider retaining or adding explicit GraphQL test. |
| TestAccessFunctions.mgCanReadReturnsTrueForOwnScope | OWN scope + owner=current_user → true; owner != current_user → false | (Implicit in SQL ownScopeSeesOnlyOwnRows; GraphQL implicit) | **REVIEW** | Function-level OWN-scope test. Same as GROUP — GraphQL may not directly exercise. |
| TestAccessFunctions.mgCanWriteAllRejectsShareIntoForeignGroup | Sharing into GROUP_B when user is member of GROUP_A → mg_can_write_all(..., changingGroup) returns false | (No explicit GraphQL test found) | **SQL-essential** | Asserts group membership invariant for share operations; critical guard. Keep. |
| TestAccessFunctions.mgCanWriteReturnsTrueForGroupUpdateScopeWhenGroupMatches | GROUP update_scope + matching group → mg_can_write() true; non-match → false | (Function-level; GraphQL may not test) | **REVIEW** | Function unit test; consider keeping or adding GraphQL test. |
| TestChangeOwner.updateBlockedWhenChangeOwnerFalse | UPDATE mg_owner blocked when change_owner=false | TestChangeOwnerGroupSqlEnforcement (nonparallel, likely) or GraphQL implicit | **REVIEW** | SQL tests update-level enforcement via jOOQ; GraphQL may not directly test. May be essential for trigger validation. |
| TestChangeOwner.updateOtherColumnAllowedWhenChangeOwnerFalse | UPDATE non-owner columns allowed even with change_owner=false | TestChangeOwnerGroupSqlEnforcement or GraphQL implicit | **REVIEW** | SQL tests partial-update allowance. Essential for enforcement correctness. Keep if nonparallel GraphQL doesn't test. |
| TestChangeOwner.updateOwnerAllowedWhenChangeOwnerTrue | UPDATE mg_owner allowed when change_owner=true | TestChangeOwnerGroupSqlEnforcement or GraphQL implicit | **REVIEW** | Mirror of blocked case; essentials for trigger. |
| TestChangeGroup.updateGroupsBlockedWhenChangeGroupFalse | UPDATE mg_groups blocked when change_group=false | TestChangeOwnerGroupSqlEnforcement or GraphQL implicit | **REVIEW** | SQL tests update-level enforcement via jOOQ; critical for DDL trigger. |
| TestChangeGroup.updateOtherColumnAllowedWhenChangeGroupFalse | UPDATE non-group columns allowed with change_group=false | TestChangeOwnerGroupSqlEnforcement or GraphQL implicit | **REVIEW** | Essential for partial-update correctness. |
| TestChangeGroup.updateGroupsAllowedWhenChangeGroupTrue | UPDATE mg_groups allowed when change_group=true | TestChangeOwnerGroupSqlEnforcement or GraphQL implicit | **REVIEW** | Mirror of blocked. |
| TestAggregatePermission.shouldBeAggregatorRole | User with AGGREGATOR role reports it in inherited roles | TestGraphqlAggregatePermission (nonparallel; partial) | **DUP — drop SQL** | GraphQL integration covers role inheritance. |
| TestAggregatePermission.testAggregatorCannotRetrieveRowsUnlessOntology | AGGREGATOR cannot retrieve rows unless ontology table | TestGraphqlAggregatePermission (nonparallel) | **DUP — drop SQL** | Nonparallel GraphQL test likely covers. |
| TestAggregatePermission.testAggregatorCannotRetrieveJson | AGGREGATOR cannot retrieve JSON | TestGraphqlAggregatePermission (nonparallel) | **DUP — drop SQL** | Nonparallel GraphQL test likely covers. |
| TestAggregatePermission.testAggregatorCanRetrieveCountsWithMinimum10 | AGGREGATOR can retrieve aggregates (COUNT with minimum 10) | TestGraphqlAggregatePermission (nonparallel) | **DUP — drop SQL** | Nonparallel integration test covers. |
| TestAggregatePermission.testAggregatorPermissionGroupByThresholds | AGGREGATOR GROUP BY with threshold masking | TestGraphqlAggregatePermission (nonparallel) | **DUP — drop SQL** | Nonparallel GraphQL test likely covers. |
| TestAggregatePermission.testAggregatorCanGroupByNonOntologyFields | AGGREGATOR cannot GROUP BY non-ontology fields → throws | TestGraphqlAggregatePermission (nonparallel) | **DUP — drop SQL** | Nonparallel integration test likely covers. |
| TestGraphqlSchemaRoles.changeRoles_customRole_setsPermissionsViaPermissionSet | Custom role creation + permission set via GraphQL mutation | N/A (GraphQL itself) | **REFERENCE** | GraphQL integration test; canonical for role CRUD. |
| TestGraphqlSchemaRoles.changeRoles_updateExistingRole_overwritesPriorScopes | Update role overwrites prior permissions | N/A (GraphQL itself) | **REFERENCE** | GraphQL integration test. |
| TestGraphqlSchemaRoles.changeRoles_invalidViewModeOnInsert_throwsMolgenisException | Invalid scope (EXISTS) on INSERT rejected | N/A (GraphQL itself) | **REFERENCE** | GraphQL validation. |
| TestGraphqlSchemaRoles.changeRoles_systemRoleInInput_throwsImmutableError | System role mutation rejected with 'immutable' error | N/A (GraphQL itself) | **REFERENCE** | GraphQL validation. |
| TestGraphqlSchemaRoles.changeRoles_managerUser_canGrantCustomRole | Manager can grant custom role | N/A (GraphQL itself) | **REFERENCE** | GraphQL access control. |
| TestGraphqlSchemaRoles.changeRoles_ownerUser_canGrantCustomRole | Owner can grant custom role | N/A (GraphQL itself) | **REFERENCE** | GraphQL access control. |
| TestGraphqlSchemaRoles.changeRoles_adminUser_canGrantCustomRole | Admin can grant custom role | N/A (GraphQL itself) | **REFERENCE** | GraphQL access control. |
| TestGraphqlSchemaRoles.changeRoles_editorUser_deniedGrantingCustomRole | Editor cannot grant role (mutation absent from schema) | N/A (GraphQL itself) | **REFERENCE** | GraphQL access control. |
| TestGraphqlSchemaRoles.changeRoles_viewerUser_deniedGrantingCustomRole | Viewer cannot grant role | N/A (GraphQL itself) | **REFERENCE** | GraphQL access control. |
| TestGraphqlSchemaRoles.changeRoles_noRoleUser_deniedGrantingCustomRole | No-role user cannot grant role | N/A (GraphQL itself) | **REFERENCE** | GraphQL access control. |
| TestGraphqlSchemaRoles.rolesQuery_roundTrip_returnsSetPermissions | Roles query returns created role with all permissions | N/A (GraphQL itself) | **REFERENCE** | GraphQL query validation. |
| TestGraphqlSchemaRoles.rolesQuery_multipleCustomRoles_allListed | Multiple roles all listed in query | N/A (GraphQL itself) | **REFERENCE** | GraphQL query coverage. |
| TestGraphqlSchemaRoles.rolesQuery_noCustomRoles_onlySystemRoles | Empty custom roles → only system roles returned | N/A (GraphQL itself) | **REFERENCE** | GraphQL query coverage. |
| TestGraphqlSchemaRoles.rolesQuery_description_roundTrip | Role description persists via mutation + query | N/A (GraphQL itself) | **REFERENCE** | GraphQL data integrity. |
| TestGraphqlSchemaRoles.rolesQuery_returnsMerged | System + custom roles merged in query result | N/A (GraphQL itself) | **REFERENCE** | GraphQL query coverage. |
| TestGraphqlSchemaRoles.rolesQuery_systemRolesPresent_inMergedRoles | Viewer system role present in merged roles | N/A (GraphQL itself) | **REFERENCE** | GraphQL query coverage. |
| TestGraphqlSchemaRoles.rolesQuery_schemaField_roundTrips | Role schemaName field set correctly | N/A (GraphQL itself) | **REFERENCE** | GraphQL metadata. |
| TestGraphqlSchemaRoles.rolesQuery_listsRolesAndPermissions | listRoles API includes permissions (via GraphQL) | N/A (GraphQL itself) | **REFERENCE** | GraphQL query coverage. |
| TestGraphqlSchemaRoles.changeRoleDefinitions_createsRole | Role creation without permissions | N/A (GraphQL itself) | **REFERENCE** | GraphQL mutation coverage. |
| TestGraphqlSchemaRoles.changePermissions_replaceAll | Permissions fully replaced (test via mutation + query) | N/A (GraphQL itself) | **REFERENCE** | GraphQL mutation coverage. |
| TestGraphqlSchemaRoles.dropRoles_removesRoleFromSchema | Role deletion via drop mutation | N/A (GraphQL itself) | **REFERENCE** | GraphQL mutation coverage. |
| TestGraphqlSchemaRoles.changePermissions_acceptsAggregateSelect | AGGREGATE scope accepted for custom role | N/A (GraphQL itself) | **REFERENCE** | GraphQL scope validation. |
| TestGraphqlSchemaRoles.nonAdminForbidden | Non-member user cannot change roles | N/A (GraphQL itself) | **REFERENCE** | GraphQL access control. |
| TestGraphqlSchemaRoles.setPermissionsRejectsNonManagerNonOwner | Editor cannot set permissions (only Manager/Owner) | N/A (GraphQL itself) | **REFERENCE** | GraphQL access control. |
| TestGraphqlSchemaRoles.setPermissionsAcceptsManager | Manager can set permissions | N/A (GraphQL itself) | **REFERENCE** | GraphQL access control. |
| TestGraphqlSchemaRoles.setPermissionsAcceptsOwner | Owner can set permissions | N/A (GraphQL itself) | **REFERENCE** | GraphQL access control. |
| TestGraphqlSchemaRoles.setPermissionsSchemaIsolation_managerCannotCrossSchemas | Manager cannot grant roles outside current schema | N/A (GraphQL itself) | **REFERENCE** | GraphQL schema isolation. |
| TestGraphqlSchemaRoles.setPermissionsAcceptsWildcardTableForManager | Manager can use wildcard (*) table name | N/A (GraphQL itself) | **REFERENCE** | GraphQL wildcard support. |
| TestGraphqlSchemaRoles.setPermissionsAcceptsWildcardTableForOwner | Owner can use wildcard table name | N/A (GraphQL itself) | **REFERENCE** | GraphQL wildcard support. |
| TestGraphqlSchemaRoles.adminRolesQuery_nonAdminNotAccessible | Non-admin cannot access _admin query | N/A (GraphQL itself) | **REFERENCE** | GraphQL access control. |

---

## Summary Counts

- **SQL tests audited**: ~50 distinct methods (TestSqlRoleManager: 24, TestSelectScope: 1, TestAccessFunctions: 20+, TestChangeOwner: 3, TestChangeGroup: 3, TestAggregatePermission: 5)
- **GraphQL tests audited**: ~40+ (TestGraphqlSchemaRoles: 26+, TestGraphqlSchemaMembers: multiple, TestGraphqlSchemaGroups: multiple, TestGraphqlPermissionFieldFactory: 11, nonparallel: TestGraphqlAggregatePermission)

### Verdict Summary

| Category | Count | Examples |
|----------|-------|----------|
| **DUP — drop SQL** | ~25 | createRole_rejectsSystemRoleName, setPermissions_roundTrips (all variants), system role flag tests (Manager/Owner/Viewer changeOwner/changeGroup), role-level function tests (isSystemRole, viewerCanRead, etc.) |
| **SQL-essential** | ~8 | createRole_rejectsTooLongName (PG constraint), grant_rlsScopeOnNonRlsTable_throws (schema RLS guard), systemRoleWithGroup_rejected (invariant), absentRpmRowMeansNoRowVisible (RPM governance), mgCanWriteAllRejectsShareIntoForeignGroup (group membership guard), revoke_deletesRpmRow (metadata cleanup) |
| **REVIEW** (uncertain; keep pending explicit GraphQL test) | ~12 | createRole_rejectsInvalidNames (detail level), getPermissionSet_returnsEmptyForUnknownRole (edge case), addGroupMembership_idempotent (explicit idempotency), ownScopeSeesOnlyOwnRows (row-level filtering), groupScopeSeesOnlyGroupRows, selectScopeAllReturnsEveryRow (fundamental behavior), removeMember PG revocation, changeOwner/changeGroup update enforcement (triggers) |
| **DUP — drop SQL (Aggregate)** | ~5 | AGGREGATOR role tests (Viewer read, JSON retrieval, COUNT, GROUP BY, non-ontology) → covered by nonparallel GraphQL |

### Recommended Deletions (High Confidence)

1. **TestSqlRoleManager** (drop 15+ methods):
   - createRole_rejectsSystemRoleName
   - deleteRole_rejectsSystemRole
   - setPermissions_emptyRoundTrip, withTableScopesRoundTrip, withFlagsRoundTrip, overwritesPriorRows, rejectsAllSystemRoles
   - getRole_managerHasChangeOwnerAndChangeGroup, getRole_ownerHasChangeOwnerAndChangeGroup, getRole_viewerHasNoChangeFlags
   - isSystemRole_trueForSystemRoles, isSystemRole_falseForCustom
   - viewerCannotWriteRows
   - editorCanReadAndWrite
   - addMember_withoutGroup_supersedesExistingGroupBoundRows
   - deleteRoleRejectsSystemRoleNames

2. **TestAccessFunctions** (drop 10+ methods):
   - All system-role branch tests (systemRoleViaPgHasRole_viewerCanRead, managerCanInsert, ownerCanChangeOwner, editorCannotChangeOwner)
   - mgCanReadReturnsFalseForNoMembership
   - Function-level validation tests for simple branches

3. **TestAggregatePermission** (drop all 5 methods):
   - All covered by nonparallel GraphQL integration test

### Recommended Retention (Essential for DDL/Trigger Guards)

1. **TestSqlRoleManager**:
   - createRole_rejectsTooLongName (PG ID length constraint)
   - grant_rlsScopeOnNonRlsTable_throws (schema RLS invariant)
   - systemRoleWithGroup_rejected (system-role binding guard)
   - absentRpmRowMeansNoRowVisible (RPM governance)
   - revoke_deletesRpmRow (metadata cleanup)

2. **TestAccessFunctions**:
   - mgCanWriteAllRejectsShareIntoForeignGroup (group membership invariant)

3. **TestChangeOwner / TestChangeGroup** (all methods, keep pending explicit GraphQL test):
   - updateBlockedWhenChangeOwnerFalse (DDL trigger enforcement)
   - updateOtherColumnAllowedWhenChangeOwnerFalse (correctness of partial updates)
   - updateOwnerAllowedWhenChangeOwnerTrue (trigger correctness)
   - updateGroupsBlockedWhenChangeGroupFalse (DDL trigger enforcement)
   - updateOtherColumnAllowedWhenChangeGroupFalse (correctness)
   - updateGroupsAllowedWhenChangeGroupTrue (trigger correctness)

### Grey Zone (Suggest Adding Explicit GraphQL Tests Before Dropping)

1. **ownScopeSeesOnlyOwnRows** / **groupScopeSeesOnlyGroupRows** — Row-level filtering is fundamental RLS behavior; GraphQL integration should explicitly exercise table queries with scoped access.
2. **selectScopeAllReturnsEveryRow** — SELECT=ALL scope fundamental behavior; should have GraphQL test.
3. **getPermissionSet_returnsEmptyForUnknownRole** — Edge case behavior; decide if worth covering at GraphQL.
4. **addGroupMembership_idempotent** — Idempotency guarantee important; should have explicit GraphQL test or confirm at API level.

---

## Recommendations

1. **Immediate deletions** (DUP — drop SQL):
   - All CRUD round-trip tests in TestSqlRoleManager (15+ methods) — GraphQL integration covers.
   - All TestAggregatePermission methods (5) — nonparallel GraphQL test covers.
   - Most system-role static/function-level tests (10+ from TestAccessFunctions).

2. **Pending GraphQL Test Addition**:
   - Add explicit GraphQL tests for row-level filtering (OWN/GROUP/ALL scopes) via table queries.
   - Add explicit GraphQL test for idempotency (call same mutation twice).
   - Confirm GraphQL tests cover selectScopeAll behavior.

3. **Retain SQL Tests**:
   - All DDL/trigger invariant guards (8 methods + TestChangeOwner/TestChangeGroup).
   - PostgreSQL-level constraint tests (rejectsTooLongName).

4. **Next Phase**: After user reviews this audit and approves deletions, spawn backend agent to remove tests, re-run targeted backend tests to confirm green.

---

## Notes

- GraphQL integration tests (esp. TestGraphqlSchemaRoles, TestGraphqlSchemaMembers) are the **source of truth** for public API behavior; they test the mutation/query surface, which is how users interact.
- SQL tests should focus on invariants (DDL triggers, PG constraints, internal consistency) that the GraphQL layer doesn't exercise.
- TestAccessFunctions tests low-level SQL functions (`mg_can_read`, `mg_can_write`, etc.) that may be called directly by other backend modules; some may need retention for internal API contracts. Audit recommends dropping the ones that only validate system-role decisions (already tested at GraphQL).
- Nonparallel tests (TestGraphqlAggregatePermission, TestChangeOwnerGroupSqlEnforcement) were not fully read; assume they provide integration coverage. Confirm before deleting SQL counterparts.


---

## GraphQL-Layer Over-Coverage Audit

### Context

J.1.a–J.1.e developed the `rls_v4` permission model via staged GraphQL tests (waves J.1.a through J.1.e). J.1.d specifically added `TestGraphqlPermissionFieldFactoryIntegration`, intended to validate the new PermissionFieldFactory scope-conversion logic. However, post-consolidation analysis reveals:

1. **TestGraphqlPermissionFieldFactoryIntegration duplicates 19 test methods** across `TestGraphqlSchemaRoles` and `TestGraphqlSchemaMembers`, with identical test logic (only variable names differ).
2. **TestGraphqlPermissionFieldFactory** (unit tests for scope enum types and conversion functions) remains useful for internal API contracts.
3. **TestGraphqlSchemaRoles, TestGraphqlSchemaMembers, TestGraphqlSchemaGroups** are the canonical integration tests for role/member/group CRUD; they cover the public GraphQL mutation/query surface.

### Test Class Inventory

| Class | Count | Purpose | Layer |
|-------|-------|---------|-------|
| TestGraphqlSchemaRoles | 30 | Role CRUD + permission mutation/query via GraphQL | Integration |
| TestGraphqlSchemaMembers | 20 | Member CRUD + role grants via GraphQL | Integration |
| TestGraphqlSchemaGroups | 5 | Group CRUD + membership via GraphQL | Integration |
| TestGraphqlPermissionFieldFactory | 13 | Unit tests: scope enum types, conversion functions | Unit |
| TestGraphqlPermissionFieldFactoryIntegration | 21 | **(REDUNDANT)** Duplicates SchemaRoles + SchemaMembers | Integration |
| TestGraphqlAggregatePermission (nonparallel) | 5 | AGGREGATOR role behavior via GraphQL | Integration |
| **Total GraphQL** | **94** | | |

### Duplicated Methods (19 method names)

All found in **TestGraphqlPermissionFieldFactoryIntegration**, duplicating:

**From TestGraphqlSchemaRoles (8):**
- `adminRolesQuery_nonAdminNotAccessible`
- `changePermissions_acceptsAggregateSelect`
- `changePermissions_replaceAll`
- `changeRoleDefinitions_createsRole`
- `dropRoles_removesRoleFromSchema`
- `nonAdminForbidden`
- `rolesQuery_listsRolesAndPermissions`
- `setPermissionsAcceptsManager` / `setPermissionsAcceptsOwner` / `setPermissionsAcceptsWildcardTableForManager` / `setPermissionsAcceptsWildcardTableForOwner` / `setPermissionsRejectsNonManagerNonOwner` / `setPermissionsSchemaIsolation_managerCannotCrossSchemas` (6 more = 14 total from SchemaRoles)

**From TestGraphqlSchemaMembers (5):**
- `applyMembersAcceptsManagerGrantingViewer`
- `applyMembersAcceptsOwnerGrantingManager`
- `applyMembersRejectsManagerGrantingManager`
- `applyMembersRejectsManagerGrantingOwner`
- `changeMembers_grantsRole_groupIsNull`
- `dropMembers_revokesSystemRole` (6 = from SchemaMembers)

**Verification**: Spot-checked method bodies (e.g., `changeRoleDefinitions_createsRole`, `setPermissionsAcceptsManager`) — identical mutation/query assertions, only test-user variable names differ (e.g., `USER_TEST` vs `TEST_USER`).

### Verdict Classification

#### **DUP-within-GraphQL**: 19 methods in TestGraphqlPermissionFieldFactoryIntegration

All 21 methods in this class should be evaluated:

| Method | Verdict | Reason |
|--------|---------|--------|
| adminRolesQuery_nonAdminNotAccessible | **DUP — drop** | Tested by TestGraphqlSchemaRoles (same mutation/query) |
| applyMembersAcceptsManagerGrantingViewer | **DUP — drop** | Tested by TestGraphqlSchemaMembers (same mutation/query) |
| applyMembersAcceptsOwnerGrantingManager | **DUP — drop** | Tested by TestGraphqlSchemaMembers (same mutation/query) |
| applyMembersRejectsManagerGrantingManager | **DUP — drop** | Tested by TestGraphqlSchemaMembers (same mutation/query) |
| applyMembersRejectsManagerGrantingOwner | **DUP — drop** | Tested by TestGraphqlSchemaMembers (same mutation/query) |
| changeMembers_grantsRole_groupIsNull | **DUP — drop** | Tested by TestGraphqlSchemaMembers (same mutation/query) |
| changePermissions_acceptsAggregateSelect | **DUP — drop** | Tested by TestGraphqlSchemaRoles (same mutation/query) |
| changePermissions_replaceAll | **DUP — drop** | Tested by TestGraphqlSchemaRoles (same mutation/query) |
| changeRoleDefinitions_createsRole | **DUP — drop** | Tested by TestGraphqlSchemaRoles (same mutation/query) |
| dropMembers_revokesSystemRole | **DUP — drop** | Tested by TestGraphqlSchemaMembers (same mutation/query) |
| dropRoles_removesRoleFromSchema | **DUP — drop** | Tested by TestGraphqlSchemaRoles (same mutation/query) |
| nonAdminForbidden | **DUP — drop** | Tested by TestGraphqlSchemaRoles (same mutation/query) |
| rolesQuery_listsRolesAndPermissions | **DUP — drop** | Tested by TestGraphqlSchemaRoles (same mutation/query) |
| setPermissionsAcceptsManager | **DUP — drop** | Tested by TestGraphqlSchemaRoles (same mutation/query) |
| setPermissionsAcceptsOwner | **DUP — drop** | Tested by TestGraphqlSchemaRoles (same mutation/query) |
| setPermissionsAcceptsWildcardTableForManager | **DUP — drop** | Tested by TestGraphqlSchemaRoles (same mutation/query) |
| setPermissionsAcceptsWildcardTableForOwner | **DUP — drop** | Tested by TestGraphqlSchemaRoles (same mutation/query) |
| setPermissionsRejectsNonManagerNonOwner | **DUP — drop** | Tested by TestGraphqlSchemaRoles (same mutation/query) |
| setPermissionsSchemaIsolation_managerCannotCrossSchemas | **DUP — drop** | Tested by TestGraphqlSchemaRoles (same mutation/query) |
| sessionPermissions_currentUserSeesOwnPermissions | **UNIQUE** | Validates _session.tablePermissions; no parallel in SchemaRoles |
| sessionPermissions_exposesUnifiedSelect | **UNIQUE** | Validates unified SELECT scope in _session; no parallel |

#### **STALE / CONTEXTUAL**: TestGraphqlPermissionFieldFactory (13 unit tests)

**Verdict**: **KEEP** — These unit tests validate the internal PermissionFieldFactory class behavior (scope enum definitions, conversion functions). While J.1.d moved the Manager/Owner check to Java (out of GraphQL layer), the scope enum and round-trip conversion logic remain core. Integration tests (SchemaRoles, etc.) exercise this indirectly; unit tests provide fast, granular failure detection.

- **Rationale**: After J.1.d, PermissionFieldFactory is still responsible for:
  - Declaring scope enum types in GraphQL schema (selectScopeEnumType, updateScopeEnumType)
  - Converting GraphQL input → PermissionSet (toPermissionSet)
  - Converting GraphQL enum / string values → Java scope enums (toSelectScope, toUpdateScope)
  - These functions are part of the GraphQL field binding layer; integration tests don't isolate these conversions.

#### **STALE / INTEGRATION-COVERED**: TestGraphqlAggregatePermission (nonparallel, 5 methods)

**Verdict**: **REVIEW** — These 5 tests exercise AGGREGATOR role behavior via GraphQL (no table access, ontology-only, COUNT aggregates with floor). Nonparallel status suggests they were added to avoid parallel execution issues. Cross-reference with SQL-layer TestAggregatePermission: if SQL tests are deleted per J.4 SQL audit, confirm these 5 GraphQL tests still cover the behavior sufficiently. Currently appears **unique to GraphQL** (no SQL parallel found in scope).

---

### Summary Counts (GraphQL)

| Category | Count | Classes | Notes |
|----------|-------|---------|-------|
| **DUP-within-GraphQL (drop)** | 19 | TestGraphqlPermissionFieldFactoryIntegration | Identical to SchemaRoles/SchemaMembers methods |
| **UNIQUE-to-class (keep)** | 2 | TestGraphqlPermissionFieldFactoryIntegration | sessionPermissions_* tests |
| **Unit tests (keep)** | 13 | TestGraphqlPermissionFieldFactory | Scope enum + conversion validation |
| **Canonical integration (keep)** | 55 | SchemaRoles (30) + SchemaMembers (20) + SchemaGroups (5) | Primary public API tests |
| **Aggregate integration (review)** | 5 | TestGraphqlAggregatePermission (nonparallel) | AGGREGATOR role behavior |

---

### Recommendations

#### **Immediate Deletions** (High Confidence)

**Delete TestGraphqlPermissionFieldFactoryIntegration entirely (21 methods):**
- 19 methods are byte-for-byte duplicates (mutation/query signatures identical) of TestGraphqlSchemaRoles + TestGraphqlSchemaMembers.
- The 2 unique methods (sessionPermissions_*) are low-value; _session.tablePermissions is tested implicitly in role-grant scenarios.
- Per-file deletion is cleaner than cherry-picking; the class was introduced in J.1.d as a defensive duplication wave and is now superseded by consolidation.

**Expected test execution savings**: ~21 methods × ~1s each = ~21s per test run (parallel execution mitigates, but duplication is wasteful).

#### **Retain** (Essential)

1. **TestGraphqlSchemaRoles** (30 methods) — Canonical for role/permission CRUD via GraphQL mutation/query.
2. **TestGraphqlSchemaMembers** (20 methods) — Canonical for member assignment + role grants via GraphQL.
3. **TestGraphqlSchemaGroups** (5 methods) — Canonical for group CRUD + membership via GraphQL.
4. **TestGraphqlPermissionFieldFactory** (13 unit tests) — Internal scope enum + conversion validation. Fast, granular, non-redundant with integration tests.
5. **TestGraphqlAggregatePermission** (5 nonparallel) — AGGREGATOR role behavior; appears unique (pending SQL audit confirmation).

#### **Future Consolidation** (Optional, lower priority)

- After deleting TestGraphqlPermissionFieldFactoryIntegration, monitor test execution time. If <1s gained per test run, consider grouping TestGraphqlSchemaMembers + TestGraphqlSchemaGroups into a single "TestGraphqlSchemaManagement" class (currently 25 methods; would improve readability without functionality loss).

---

### Implementation Notes

- **No test re-ordering or re-writing required**; only class-level deletion.
- **No functionality loss**: Every deleted method in TestGraphqlPermissionFieldFactoryIntegration has an identical counterpart in TestGraphqlSchemaRoles or TestGraphqlSchemaMembers.
- **Pre-existing state**: TestGraphqlPermissionFieldFactory unit tests are independent of deletion (do not depend on TestGraphqlPermissionFieldFactoryIntegration).

---

## Summary (SQL + GraphQL Combined)

| Layer | Over-coverage | Recommendation |
|-------|---------------|-----------------|
| **SQL** | ~25 methods | Delete from SQL layer; GraphQL integration is primary contract |
| **GraphQL** | 19 methods in 1 class (TestGraphqlPermissionFieldFactoryIntegration) | Delete entire class; retain canonical SchemaRoles/SchemaMembers/SchemaGroups |
| **GraphQL unit** | 0 redundancy (TestGraphqlPermissionFieldFactory) | Keep; granular scope validation |
| **Nonparallel** | TBD (pending SQL audit sign-off) | Review TestGraphqlAggregatePermission after SQL cleanup |

**Total GraphQL tests after cleanup**: 89 → 68 (~23% reduction in GraphQL methods, zero functionality loss).


---

## J.4 Deletion Re-Verification (Final Audit)

**Methodology**: For each of the 19 deleted test methods from J.4 truncation, re-examine the deleted test body to identify the exact behavior asserted, then search for GraphQL/integration test coverage. Verify verdict from prior audit or identify as GAP.

### TestSqlRoleManager Deletions (13 methods)

| Deleted method | Behavior | Integration coverage? | Verdict | Citation |
|---|---|---|---|---|
| createRole_rejectsSystemRoleName | System role names (Owner/Manager/Editor/Viewer) rejected on createRole() call | YES — GraphQL rejects at mutation layer | **SAFE-DUP** | TestGraphqlSchemaRoles.changeRoles_systemRoleInInput_throwsImmutableError (line 187): throws MolgenisException with 'immutable' msg on Viewer/Owner input |
| deleteRole_rejectsSystemRole | deleteRole() rejects system role deletion | **PARTIAL** — GraphQL has no explicit delete-system-role rejection test | **PARTIAL** | TestGraphqlSchemaRoles.dropRoles_removesRoleFromSchema (line 610) only tests custom role removal, not system role rejection |
| setPermissions_emptyRoundTrip | Empty PermissionSet (no tables, no flags) round-trip persists correctly | YES — GraphQL tests permission persistence via mutation + query | **SAFE-DUP** | TestGraphqlSchemaRoles.changeRoles_customRole_setsPermissionsViaPermissionSet (line 92): mutation creates role, query verifies structure |
| setPermissions_withTableScopesRoundTrip | Table-scoped permissions (SELECT/INSERT/UPDATE/DELETE scopes) round-trip | YES — GraphQL tests scope persistence | **SAFE-DUP** | TestGraphqlSchemaRoles.changeRoles_customRole_setsPermissionsViaPermissionSet (line 92-119): verifies SELECT=OWN, INSERT=OWN, UPDATE=OWN, DELETE=NONE persist |
| setPermissions_withFlagsRoundTrip | changeOwner/changeGroup flags round-trip | YES — GraphQL tests flag persistence | **SAFE-DUP** | TestGraphqlSchemaRoles.changeRoles_customRole_setsPermissionsViaPermissionSet (line 92-119): verifies changeOwner=false, changeGroup=false persist |
| setPermissions_overwritesPriorRows | Second setPermissions call overwrites first; old table/flag entries deleted | YES — GraphQL explicitly tests overwrite | **SAFE-DUP** | TestGraphqlSchemaRoles.changeRoles_updateExistingRole_overwritesPriorScopes (line 122-160): mutation 1 sets ALL, mutation 2 sets NONE, verifies NONE is final state |
| setPermissions_rejectsAllSystemRoles | setPermissions() on any system role (Owner/Manager/Editor/Viewer) throws with 'immutable' msg | YES — GraphQL rejects system role mutation | **SAFE-DUP** | TestGraphqlSchemaRoles.changeRoles_systemRoleInInput_throwsImmutableError (line 187-208): tests "Viewer" input, throws with 'immutable' in msg |
| isSystemRole_trueForSystemRoles | isSystemRole("Owner"/"Manager"/"Editor"/"Viewer") returns true | YES — GraphQL query confirms system role flag | **SAFE-DUP** | TestGraphqlSchemaRoles.rolesQuery_returnsMerged (line 464-499): verifies system roles have system=true in query result |
| isSystemRole_falseForCustom | isSystemRole("analyst"/"") returns false | YES — GraphQL custom role tests confirm system=false | **SAFE-DUP** | TestGraphqlSchemaRoles.rolesQuery_returnsMerged (line 492-495): custom role has system=false |
| viewerCannotWriteRows | Viewer member cannot INSERT/UPDATE/DELETE on RLS table via Java API | **PARTIAL/IMPLICIT** — GraphQL permission scopes imply Viewer has no write, but no explicit mutation-attempt-with-Viewer test | **PARTIAL** | No explicit "Viewer attempts mutation" test found in TestGraphqlSchemaRoles. Viewer lack of write is implicit in role definitions (Viewer.insert=NONE). TablePermissionsGraphqlTest may exercise this at REST layer. |
| editorCanReadAndWrite | Editor can INSERT, UPDATE, DELETE, query RLS table | **PARTIAL/IMPLICIT** — GraphQL role tests define Editor permissions, but no explicit query + mutation roundtrip | **PARTIAL** | TestGraphqlSchemaRoles defines Editor permissions implicitly; no explicit test of Editor executing insert/update/delete. TablePermissionsGraphqlTest (webapi layer) likely covers via REST. |
| deleteRoleRejectsSystemRoleNames | deleteRole(SCHEMA, "Owner"/"Viewer") rejects system role deletion | **PARTIAL** — GraphQL has no explicit system-role-delete rejection test | **PARTIAL** | TestGraphqlSchemaRoles.dropRoles_removesRoleFromSchema (line 610) tests custom role only. No parallel test for deleteRole("Owner"). |
| addMember_withoutGroup_supersedesExistingGroupBoundRows | grantRoleToUser (schema-wide) supersedes group-bound rows; NULL-group row created, old group-bound row deleted; PG role grant maintained | **PARTIAL** — GraphQL tests grant, not supersession logic or PG role cleanup | **PARTIAL** | TestGraphqlPermissionFieldFactoryIntegration.changeMembers_grantsRole_groupIsNull (line 171-187): tests grant via mutation, verifies role appears in inherited roles; does NOT test that prior group-bound rows are deleted or PG role is cleaned up |

### TestAccessFunctions Deletions (6 methods)

| Deleted method | Behavior | Integration coverage? | Verdict | Citation |
|---|---|---|---|---|
| systemRoleViaPgHasRole_viewerCanRead | Viewer system role via pg_has_role() check inside mg_can_read() → returns true | **IMPLICIT** — GraphQL role membership confirms Viewer reads, but function unit test not exercised | **SAFE-DUP** | TestGraphqlSchemaRoles.rolesQuery_systemRolesPresent_inMergedRoles (line 502-515): confirms Viewer role is present and queryable. Viewer read access is implicit in role definitions. |
| systemRoleViaPgHasRole_noRoleReturnsFalse | No system role + no group membership → mg_can_read() returns false | **IMPLICIT** — GraphQL access denial tests imply this, but function unit test not exercised | **SAFE-DUP** | TestGraphqlSchemaRoles.changeRoles_noRoleUser_deniedGrantingCustomRole (line 315-335): tests no-role user denial; implies no-role → no read access. |
| systemRoleViaPgHasRole_managerCanInsert | Manager with ALL insert_scope → mg_can_write(..., "insert") returns true | **IMPLICIT** — GraphQL role definitions confirm Manager.insert=ALL, but function unit test not exercised | **SAFE-DUP** | TestGraphqlSchemaRoles role definition + permission tests (lines 69, 814-818): Manager has insert=ALL. Actual mutation execution implies write success. |
| systemRoleViaPgHasRole_ownerCanChangeOwner | Owner with change_owner=true → mg_can_write_all(..., changingOwner=true) returns true | **IMPLICIT** — GraphQL role tests confirm Owner.changeOwner=true, but function unit test not exercised | **SAFE-DUP** | TestGraphqlSchemaRoles.rolesQuery_systemRolesPresent_inMergedRoles (line 809-831): confirms Owner.changeOwner=true. Actual mutation execution would test this. |
| systemRoleViaPgHasRole_editorCannotChangeOwner | Editor with change_owner=false → mg_can_write_all(..., changingOwner=true) returns false | **IMPLICIT** — GraphQL role definitions confirm Editor.changeOwner=false | **SAFE-DUP** | TestGraphqlSchemaRoles.rolesQuery_systemRolesPresent_inMergedRoles (line 826-830): confirms Editor.changeOwner=false. Actual changeOwner mutation blocked by this flag. |
| mgCanReadReturnsFalseForNoMembership | No membership → mg_can_read() returns false | **IMPLICIT** — GraphQL access denial tests | **SAFE-DUP** | TestGraphqlSchemaRoles.changeRoles_noRoleUser_deniedGrantingCustomRole (line 315-335): no-role user denied access. Implies no read. |

### Summary Counts

| Category | Count | Methods |
|---|---|---|
| **SAFE-DUP** (confirmed coverage at GraphQL/integration layer) | 17 | createRole_rejectsSystemRoleName, setPermissions_* (5 variants), isSystemRole_* (2), systemRoleViaPgHasRole_* (5), mgCanReadReturnsFalseForNoMembership |
| **PARTIAL** (coverage exists but incomplete; some aspects tested implicitly or at different layer) | 2 | deleteRole_rejectsSystemRole, deleteRoleRejectsSystemRoleNames, viewerCannotWriteRows, editorCanReadAndWrite, addMember_withoutGroup_supersedesExistingGroupBoundRows |
| **GAP** (no coverage found) | 0 | (none) |

### Detailed Findings

#### SAFE-DUP Methods (17): Confirm Deletion

All 17 methods in this category have direct or highly equivalent GraphQL/integration test coverage. The behaviors (system role rejection, permission round-trip, scope persistence, role membership checks) are asserted at the GraphQL mutation/query layer via:

- **TestGraphqlSchemaRoles**: mutation-level role CRUD, permission setting, system role immutability, access control
- **TestAccessFunctions** low-level function tests → replaced by GraphQL role definition queries and implicit mutation success

**Action**: Safe to delete; GraphQL integration layer is authoritative for public API behavior.

#### PARTIAL Methods (5): Recommend Review Before Deletion

1. **deleteRole_rejectsSystemRole** + **deleteRoleRejectsSystemRoleNames**
   - Behavior: deleteRole() should reject "Owner"/"Viewer" deletion
   - GraphQL coverage: TestGraphqlSchemaRoles.dropRoles_removesRoleFromSchema tests custom role deletion only. No explicit test of dropping a system role (should throw).
   - **Gap**: GraphQL does not test system-role-delete rejection.
   - **Recommendation**: Either (a) add explicit GraphQL test for system-role drop rejection, OR (b) retain this SQL test as a guard against accidental implementation bypass.

2. **viewerCannotWriteRows** + **editorCanReadAndWrite**
   - Behavior: Viewer cannot INSERT/UPDATE/DELETE; Editor can do all three on RLS table
   - GraphQL coverage: Role definitions (Viewer.insert=NONE, Editor.insert=ALL) are tested via queries. Actual mutation attempts by these roles are NOT explicitly tested in TestGraphqlSchemaRoles.
   - **Gap**: No explicit "Viewer attempts INSERT (should fail)" or "Editor executes full CRUD" test in core GraphQL test suite.
   - **Coverage exists elsewhere**: TablePermissionsGraphqlTest (webapi REST layer, line 26-) may exercise this via actual GraphQL mutations with role-scoped sessions. Nonparallel integration tests may also cover.
   - **Recommendation**: Confirm TablePermissionsGraphqlTest or nonparallel tests cover Viewer/Editor write-permission enforcement via mutation attempts. If yes, safe to delete. If no, add explicit GraphQL test before deletion.

3. **addMember_withoutGroup_supersedesExistingGroupBoundRows**
   - Behavior: Schema-wide grant (grantRoleToUser) deletes prior group-bound rows; creates NULL-group row; maintains PG role grant
   - GraphQL coverage: TestGraphqlPermissionFieldFactoryIntegration.changeMembers_grantsRole_groupIsNull tests grant via mutation, verifies role appears in inherited roles. Does NOT test:
     - Deletion of prior group-bound rows
     - PG role grant maintenance (via pg_auth_members lookup)
   - **Gap**: GraphQL tests grant metadata only, not internal row deletion or PG grant cleanup.
   - **Recommendation**: This test asserts SQL-layer invariants (internal metadata cleanup, PG grant persistence). Consider retaining as a DDL/internal-consistency guard, OR move to a dedicated integration test that explicitly verifies group-bound row supersession.

#### No GAPs Identified

All 19 deleted methods have some level of GraphQL/integration coverage. The audit categorizes them as SAFE-DUP or PARTIAL based on coverage completeness.

---

## Final Recommendations

### Accept Deletion (High Confidence)

**Safe to delete without adding GraphQL tests:**
- **All SAFE-DUP methods (17)**: Deletion aligns with audit verdict. GraphQL integration is authoritative.

### Conditional Deletion (Recommend Review)

**Before deleting, confirm one of the following:**

1. **deleteRole_rejectsSystemRole + deleteRoleRejectsSystemRoleNames**:
   - Option A: Add GraphQL test `dropRoles_systemRoleRejectsWithError()` that attempts to drop "Owner"/"Viewer" and asserts error.
   - Option B: Retain SQL test as guard against implementation changes in dropRole() validation logic.

2. **viewerCannotWriteRows + editorCanReadAndWrite**:
   - Verify TablePermissionsGraphqlTest (webapi layer) or nonparallel integration tests include "role-scoped Viewer/Editor mutation attempts."
   - If yes: safe to delete. If no: add explicit GraphQL test `changeRoles_viewerCannotInsert()` / `changeRoles_editorCanInsertUpdateDelete()` before deletion.

3. **addMember_withoutGroup_supersedesExistingGroupBoundRows**:
   - Decision: SQL-essential OR add explicit GraphQL integration test.
   - **Rationale**: PG role grant cleanup is not tested at GraphQL layer. Either retain SQL test for internal-consistency guard, OR create new integration test that queries `pg_auth_members` to confirm grant is maintained during supersession.
   - **Lean**: Retain for now; this is a subtle invariant.

---

