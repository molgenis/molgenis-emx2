# RLS v4 Spec — behavioural contract

> Self-contained successor to `rls_v3.md` spec. Master is the base;
> this document covers the RLS extension that opts in per table.

## Design overview (read this first)

The 10 things to understand before reviewing:

1. **RLS is per-table opt-in, not global.** Each table has a flag `rls_enabled`. Tables with the flag off behave exactly as in master (no policies, no `mg_owner`/`mg_groups` columns). Tables with the flag on enforce row-level visibility via PostgreSQL RLS policies.
2. **RLS lives only at the root of an inheritance tree.** If table `B` extends `A`, only `A` carries the `rls_enabled` setting; enabling on `A` cascades policies to `B` automatically. Toggling on a subclass is rejected with `"enable on root '<X>' instead"`. Same rule for disable.
3. **Two layers of access control compose**: (a) PostgreSQL `GRANT verb ON table TO role` from master — unchanged; gates whether the user can issue the SQL at all. (b) RLS policies — gate which rows the user sees within a granted verb. A user with `GRANT SELECT` but `select_scope=NONE` is rejected at app layer before reaching RLS.
4. **Custom roles are PG roles.** `Schema.createRole(R)` creates PG role `MG_ROLE_<schema>/<R>`. `grant(R, table, verb)` emits `GRANT verb ON table TO MG_ROLE_<schema>/<R>`. Membership: `GRANT MG_ROLE_<schema>/<R> TO MG_USER_<user>`.
5. **System roles (Owner/Manager/Editor/Viewer) have hardcoded scopes**, no rows in `role_permission_metadata`. Custom roles store one row per `(schema, role, table)` with five scopes.
6. **Five scopes per `(role, table)`**: `select_scope`, `insert_scope`, `update_scope`, `delete_scope`, `reference_scope`. SELECT supports privacy modes (EXISTS/COUNT/RANGE/AGGREGATE) plus row-access tiers (OWN/GROUP/ALL); INSERT/UPDATE/DELETE are NONE/OWN/GROUP/ALL; REFERENCE is NONE/ALL only (post-R.7).
7. **Effective permission across multiple roles = max scope per axis.** A user holding `Role1{select=GROUP}` + `Role2{reference=ALL}` sees group rows directly AND can FK-reference any row.
8. **Two row-tagging columns on RLS tables**: `mg_owner` (TEXT, the row's owner — defaults to inserting user; backfilled from `mg_insertedBy` on enable) and `mg_groups` (TEXT[], the groups the row belongs to). Indexed via GIN on `mg_groups`.
9. **REFERENCE is orthogonal to VIEW.** `VIEW(T)` gates direct query of T; `REFERENCE(T)` gates FK traversal into T from other tables. VIEW⊇REFERENCE at runtime — anyone with VIEW automatically gets REFERENCE-equivalent access. REFERENCE-only (`VIEW_NONE + REFERENCE_ALL`) emits a thin GraphQL type (PK + refLabel only) reachable only via FK from another table; T is absent from top-level Query/Mutation. Privacy modes (EXISTS/COUNT/RANGE/AGGREGATE) grant view but NOT reference — can't dereference an FK from privacy-only access.
10. **Child-row visibility rule (cross-table)**: a row in Child is hidden entirely if ANY of its FK targets falls outside `(view ∪ reference)` scope on the refTable. REF_ARRAY is all-or-nothing — one invisible element hides the row. At write time the server fail-fast rejects INSERT/UPDATE that would create a row pointing at a target outside the writing user's scope (defense in depth; DELETE has no such check). Ontology tables bypass these rules — they're always world-visible.

## Base layer (master, untouched)

| Behavior | Component | Test | Visual |
|---|---|---|---|
| `Schema.createRole(name)` creates PG role `MG_ROLE_<schema>/<role>` | SqlRoleManager | TestSqlRoleManager.createRoleCreatesPgRole | — |
| `Schema.deleteRole(name)` revokes all GRANTs and `DROP ROLE`s | SqlRoleManager | TestSqlRoleManager.deleteRoleDropsPgRole | — |
| `grant(role, table, verb)` emits `GRANT verb ON table TO MG_ROLE_<schema>/<role>` | SqlRoleManager | TestTableRoleManagement.grantWithFalseRevokesIndividualPrivilege | — |
| `revoke(role, table, verb)` REVOKEs the verb | SqlRoleManager | TestTableRoleManagement.revokeRemovesTableAccess | — |
| `grant` to non-existent role throws `MolgenisException` | SqlRoleManager | TestTableRoleManagement.cannotGrantToNonExistentRole | — |
| Table drop revokes role grants on that table | SqlRoleManager | TestTableRoleManagement.grantIsLostAfterTableDropAndRecreate | — |
| Anonymous Viewer + custom-role grants merge | SqlRoleManager | TestTableRoleManagement.anonymousViewerAndCustomRolePermissionsAreMerged | — |
| `getRoleInfo` reads `information_schema.role_table_grants` | SqlRoleManager | TestSqlRoleManager.getRoleInfoFromGrantsCatalog | — |
| New table auto-grants to system roles only (Owner/Manager/Editor/Viewer + scan helpers) | SqlTableMetadataExecutor | TestTableRoleManagement.systemRolesAutoGrantedOnTableCreate | — |
| Custom roles get no auto-grant on table create | SqlTableMetadataExecutor | TestTableRoleManagement.customRolesNotAutoGrantedOnTableCreate | — |

## Per-table RLS flag

Every table has an `rls_enabled` boolean (default false). Toggling it on is what installs the four PostgreSQL row-level-security policies, materializes the row-tagging columns (`mg_owner`, `mg_groups`), and creates the GIN index used by the group-overlap policy predicate. Toggling off removes them. The flag lives only on the root of an inheritance tree (cascades to subclasses); admins cannot disable a table while custom-role scope rows still reference it.

| Behavior | Component | Test | Visual |
|---|---|---|---|
| The flag column exists on `tables_metadata` and defaults to `false` (existing tables stay unaffected) | MetadataUtils | TestRlsToggle.flagDefaultsFalse | — |
| Admin enables RLS on a table via the standard schema-change mutation (`change(tables:[{name, rlsEnabled:true}])`) | GraphQL.tableSchema | TestRlsToggleGraphql.enableRlsRoot | rlsEnabled=true in admin UI |
| Disable rejected when any custom role still has a scope row for the table — protects against accidentally orphaning OWN/GROUP scopes (the `role_permission_metadata` table stores those rows) | SqlTableMetadata | TestRlsToggle.disableRejectedWithPermissions | error toast |
| Enabling on a subclass is rejected with `"enable on root '<X>' instead"` — RLS state is stored on the inheritance-tree root only | SqlTableMetadata | TestRlsToggle.enableSubclassRejected | — |
| Enabling on a root cascades policies + columns to all subclasses atomically (one transaction) | SqlTableMetadata | TestRlsToggle.enableCascadesToChildren | — |
| Disabling on a root cascades the removal to all subclasses | SqlTableMetadata | TestRlsToggle.disableCascadesToChildren | — |
| Internal storage: subclass rows always carry `rls_enabled=false`; consumers must walk up via `getInheritedTable()` to read the effective flag | TableMetadata.getRlsEnabled | TestRlsToggle.metadataStoredOnRootOnly | — |
| Enable adds two row-tagging columns to the physical table: `mg_owner TEXT` (the row's owner, defaults to inserting user) and `mg_groups TEXT[]` (groups the row belongs to) | SqlTableMetadata | TestRlsToggle.materialisesRowColumnsOnEnable | — |
| Enable backfills `mg_owner` from the existing `mg_insertedBy` audit column (so pre-RLS rows have a sensible owner) | SqlTableMetadata | TestRlsToggle.backfillsOwnerOnEnable | — |
| Enable creates a GIN index on `mg_groups` named `<table>_mg_groups_idx`; disable drops it. Index supports the `mg_groups && user_groups` overlap test in the SELECT policy | SqlRoleManager.enableRlsForTable | TestRlsEnableDisableLifecycle.enableRls_createsGinIndexOnMgGroups | — |

## Policy template (per RLS table)

| Behavior | Component | Test | Visual |
|---|---|---|---|
| 4 policies emitted: select / insert / update / delete | SqlTableMetadata | TestRlsPolicies.fourPoliciesPerTable | — |
| Policies call `mg_can_read` / `mg_can_write` / `mg_can_write_all` | SqlTableMetadata | TestRlsPolicies.policiesUseAccessFunctions | — |
| `ENABLE` + `FORCE ROW LEVEL SECURITY` applied | SqlTableMetadata | TestRlsPolicies.forceRlsApplied | — |
| Capability changes do not emit policy DDL | SqlRoleManager | TestRlsPolicies.scopeUpdateNoPolicyDdl | — |

## Scope evaluation (RLS tables)

| Behavior | Component | Test | Visual |
|---|---|---|---|
| `select_scope='ALL'` returns every row | mg_can_read | TestSqlRoleManager.selectScopeAllReturnsEveryRow | — |
| `select_scope='GROUP'` returns rows whose `mg_groups` overlap user's memberships | mg_can_read | TestSqlRoleManager.groupScopeSeesOnlyGroupRows / TestAsymmetricCollaboration | — |
| `select_scope='OWN'` returns rows whose `mg_owner = current_user` | mg_can_read | TestSqlRoleManager.ownScopeSeesOnlyOwnRows / TestSchemaWideCustomGrants.nullGroupGrant_ownScope_userReadsOwnRowsOnly | — |
| `select_scope='NONE'` rejected at app layer (PG SELECT revoked; query never reaches policy) | SqlQuery.checkHasViewPermission | TestTablePolicies.noneScopeIsRejectedBeforeRls | — |
| `insert_scope='GROUP'` requires every group in `NEW.mg_groups` to be one writer holds GROUP/ALL in | mg_can_write_all | TestUpdateScope.groupScopeCanUpdateGroupRow / TestSqlRoleManager.groupScopeUpdatesOnlyGroupRows | — |
| `update_scope='OWN'` rejects update of rows owned by another user | mg_can_write | TestUpdateScope.ownScopeCanUpdateOnlyOwnRow / TestSqlRoleManager.ownScopeUpdatesOnlyOwnRows | — |
| Custom-role scope row absent ⇒ user rejected at app layer (no per-table GRANT issued) | SqlQuery.checkHasViewPermission | TestSqlRoleManager.absentRpmRowMeansNoRowVisible | — |

## System roles (hardcoded scopes)

| Behavior | Component | Test | Visual |
|---|---|---|---|
| Owner/Manager/Editor: every verb = ALL | mg_can_read / mg_can_write | TestSqlRoleManager.editorCanReadAndWrite | — |
| Viewer: `select=ALL`, others NONE | mg_can_read / mg_can_write | TestSqlRoleManager.viewerCanReadRows / viewerCannotWriteRows | — |
| System roles never have rows in `role_permission_metadata` | trigger + Java | TestSqlRoleManager.deleteRoleRejectsSystemRoleNames + TestMetadataUtilsRolePermission.triggerRejectsUpdateOnSystemRoleRow | — |
| Granting custom role with system name rejected | Schema.createRole | TestSqlRoleManager.isSystemRole_rejectsSystemNames | — |

## Capability flags

| Behavior | Component | Test | Visual |
|---|---|---|---|
| `change_owner=false` rejects `OLD.mg_owner ≠ NEW.mg_owner` UPDATE | mg_check_change_capability | TestChangeOwner.updateBlocked | — |
| `change_owner=true` permits ownership transfer (read of new-owner row included) | mg_can_read + trigger | TestChangeOwner.transferAllowed | — |
| `change_group=false` rejects `OLD.mg_groups ≠ NEW.mg_groups` UPDATE | mg_check_change_capability | TestChangeGroup.updateBlocked | — |
| INSERT with `mg_owner ≠ current_user` requires `change_owner=true` | mg_check_change_capability | TestChangeOwner.insertNonSelfRequiresFlag | — |
| Owner / Manager bypass change-capability trigger | mg_check_change_capability | TestChangeOwner.systemRolesBypass | — |

## Storage

| Behavior | Component | Test | Visual |
|---|---|---|---|
| `role_permission_metadata` PK `(schema, role, table)` | MetadataUtils | TestMetadataSchema.rpmPrimaryKey | — |
| FK `(schema, table)` → `table_metadata` ON DELETE CASCADE | MetadataUtils | TestMetadataSchema.rpmTableFk | — |
| BEFORE UPDATE TRIGGER rejects writes to system-role names | MetadataUtils | TestMetadataSchema.rpmSystemRoleTriggerRejects | — |
| `group_membership_metadata` PK `(user, schema, group, role)` | MetadataUtils | TestMetadataSchema.gmmPrimaryKey | — |
| `groups_metadata` columns = `(schema, name)` only | MetadataUtils | TestMetadataSchema.groupsMetadataShape | — |

## Invariants (cross-cutting)

| Behavior | Component | Test | Visual |
|---|---|---|---|
| `OWN | GROUP` scope on non-RLS table rejected at Java + GraphQL | Schema.upsertRolePermissions | TestScopeInvariant.ownGroupRequiresRls | error toast |
| `EXISTS | COUNT | RANGE | AGGREGATE` scope accepted on non-RLS table | Schema.upsertRolePermissions | TestScopeInvariant.privacyAllowedNonRls | — |
| `change_owner=true` / `change_group=true` on non-RLS table rejected | Schema.upsertRolePermissions | TestScopeInvariant.changeFlagsRequireRls | error toast |
| Disable RLS rejected when scope rows exist for the table | SqlTableMetadata | TestRlsToggle.disableRejectedWithPermissions | error toast |
| System role + group binding rejected at Java layer | SqlRoleManager.addGroupMembership | TestSqlRoleManager.systemRoleWithGroup_rejected | — |
| System role + group binding rejected at GraphQL | GraphQL.changeMembers | TestMembersGraphql.systemRoleNoGroup | error toast |

## Privacy view modes (RLS or non-RLS table)

| Behavior | Component | Test | Visual |
|---|---|---|---|
| `select_scope='EXISTS'` projection: result clamped to {0, 1} | SqlQuery | TestPrivacy.existsProjection (gap D-test-1) | — |
| `select_scope='COUNT'` projection: floor of 10 via `mg_privacy_count` | SqlQuery + mg_privacy_count | TestSelectScope.countScopeRlsPassThroughSeesAllRows (partial) + TestPrivacy.countFloor10 (gap D-test-1) | — |
| `select_scope='RANGE'` projection: histogram bucket lower-bounded at 10 | SqlQuery | TestPrivacy.rangeFloor10 (gap D-test-1) | — |
| `select_scope='AGGREGATE'` projection: SUM/AVG only when count ≥ 10 | SqlQuery | TestPrivacy.aggregateFloor10 (gap D-test-1) | — |
| Policy predicate `true` for privacy scopes on RLS tables (pass-through) | mg_can_read | TestPrivacy.passThroughAtPolicyLayer (gap D-test-2) | — |
| Privacy scopes valid on non-RLS tables (projection only, no policy) | Schema.upsertRolePermissions | TestPrivacy.privacyOnNonRlsTable (gap D-test-2) | — |
| Any non-NONE `select_scope` ⇒ `GRANT SELECT` to role's PG role; `NONE` ⇒ REVOKE (RLS or not) | SqlRoleManager | TestPrivacy.nonNoneScopeImpliesSelectGrant | — |

## GraphQL surface

| Behavior | Component | Test | Visual |
|---|---|---|---|
| `_schema.roles` merges system + custom | GraphqlSchemaFieldFactory | TestGraphqlRoles.mergedList | role table |
| `change(roles:…)` upserts custom role + permissions | GraphqlSchemaMutations | TestGraphqlRoles.changeRolesUpserts | — |
| `change(roles:…)` rejects OWN/GROUP on non-RLS table | GraphqlSchemaMutations | TestGraphqlRoles.scopeInvariantEnforced | error toast |
| `_schema.tables[].rlsEnabled` returns flag | GraphqlSchemaFieldFactory | TestGraphqlTables.rlsEnabledField | — |
| `change(tables:[{name, rlsEnabled}])` toggles | GraphqlSchemaMutations | TestGraphqlTables.toggleRls | rlsEnabled toggle |
| `_schema.members` UNION (system + custom + custom-no-group) | GraphqlSchemaFieldFactory | TestGraphqlMembers.membersUnion | members table |
| `change(members:[{user, role, group?}])` — system + group rejected | GraphqlSchemaMutations | TestGraphqlMembers.systemRoleNoGroup | — |
| Custom role no group ⇒ schema-wide NULL-group row; supersedes any group-bound rows for same (user, role) | Schema.changeMembers / SqlRoleManager.grantRoleToUser | TestGraphqlSchemaMembers.changeMember_customRoleNoGroup_supersedesGroupScopedRows / TestSqlRoleManager.addMember_withoutGroup_supersedesExistingGroupBoundRows / TestSchemaWideCustomGrants.grantSchemaWide_supersedesExistingGroupScopedGrants | — |
| Custom role with group ⇒ PG GRANT + membership row; leaves other rows alone | Schema.changeMembers | TestGraphqlSchemaMembers.customRoleRows_groupSet | — |
| `drop(members)` no group: revokes PG role + clears ALL rows for (user, role, schema) | SqlRoleManager.revokeRoleFromUser | TestSqlRoleManager.removeMember_withoutGroup_clearsAllRowsAndRevokesPgRole_evenWhenGroupBoundRowsExist / TestSchemaWideCustomGrants.dropSchemaWideGrant_alsoRemovesGroupScopedGrant | — |
| `drop(members)` with group: removes only that row; PG REVOKE only if no rows remain | SqlRoleManager.removeGroupMembership | TestGraphqlSchemaMembers.dropMember_withGroup_leavesOtherGroupMembershipIntact | — |
| Escalation guard: only admin/Owner/Manager grants any custom role | GraphqlSchemaMutations | TestGraphqlMembers.escalationGuard | — |
| `_schema.groups` + `change/drop(groups)` central mutations | GraphqlSchemaMutations | TestGraphqlGroups.crud | groups admin |

## Cross-schema FK + RLS visibility

| Behavior | Component | Test | Visual |
|---|---|---|---|
| `retrieveJSON` subfield expansion filters invisible FK targets via auto-join + RLS | SqlQuery.refJoins | TestCrossSchemaFkRlsVisibility.joinResolvesOnlyVisibleParents | — |
| REF_ARRAY resolution drops invisible elements via auto-join + RLS | SqlQuery.refJoins | TestCrossSchemaFkRlsVisibility.refArrayDropsInvisibleElements | — |
| RLS-filtered Parent returns only visible rows; invisible parent unreachable via refback path | mg_can_read | TestCrossSchemaFkRlsVisibility.refbackEmptyForInvisibleParent | — |
| Inheritance composition: subclass rows owned by another user not visible through Parent query | mg_can_read + per-table policy | TestCrossSchemaFkRlsVisibility.inheritanceCompositionFiltersChildSubclass | — |
| `retrieveRows()` clamps FK scalar to null for invisible RLS-target via LEFT JOIN | SqlQuery.buildRlsClampAliases + addRlsClampJoins | TestCrossSchemaFkRlsVisibility.scalarRefProjectsNullForInvisibleParent | — |

## REFERENCE permission — model and persistence

| Behavior | Component | Test | Visual |
|---|---|---|---|
| `PermissionSet.ReferenceScope` enum (NONE/ALL) with `fromString` | PermissionSet | TestSqlRoleManager.referenceScope_roundTrip_all / referenceScope_roundTrip_none | — |
| `TablePermission.reference()` defaults to NONE; fluent setter rejects null | TablePermission | TestSqlRoleManager.fluentSetter_nullArg_throwsNPE | — |
| `role_permission_metadata.reference_scope TEXT NOT NULL DEFAULT 'NONE'` persisted | MetadataUtils / migration32.sql | TestSqlRoleManager.referenceScope_roundTrip_all | — |
| `setPermissions` round-trips REFERENCE | SqlRoleManager.setPermissions | TestSqlRoleManager.referenceScope_roundTrip_all | — |
| Additive `grant` merge preserves REFERENCE | SqlRoleManager.mergeWithExisting | TestSqlRoleManager.referenceOnlyPermission_survivesAggregationPath | — |
| `getPermissionsForActiveUser` aggregates REFERENCE across roles | SqlRoleManager.hasAnyPermission + mergePermissions | TestSqlRoleManager.referenceOnlyPermission_survivesAggregationPath | — |
| Wildcard role expansion propagates REFERENCE | SqlRoleManager.expandWildcard | TestSqlRoleManager.referenceOnlyWildcard_survivesExpandWildcard | — |
| GraphQL `change(roles:…)` accepts `reference` scope | GraphqlPermissionFieldFactory | TestGraphqlSchemaRoles.changeRoles_customRole_includesReferenceScope | role editor |

## REFERENCE permission — SQL predicate (`mg_can_reference`)

| Behavior | Component | Test | Visual |
|---|---|---|---|
| Returns true for REFERENCE_ALL | mg_can_reference | TestMgCanReference.mgCanReference_returnsTrue_whenReferenceScopeAll | — |
| Returns true for system roles (Owner/Manager/Editor/Viewer) | mg_can_reference | TestMgCanReference.mgCanReference_returnsTrue_forSystemRole | — |
| VIEW ⊇ REFERENCE implicit carry: row-access scope grants reference even with REFERENCE_NONE | mg_can_reference | TestMgCanReference.mgCanReference_returnsTrue_whenSelectScopeAllButReferenceNone | — |
| Privacy modes (EXISTS/COUNT/RANGE/AGGREGATE) do NOT grant reference | mg_can_reference | TestMgCanReference.mgCanReference_returnsFalse_whenPrivacyScopeOnly | — |
| Optional `p_user TEXT DEFAULT current_user` argument honored for delegated checks | mg_can_reference | TestMgCanReference.mgCanReference_withExplicitUser_honorsPassedUser | — |

## Read-path FK visibility (R.3b/c)

| Behavior | Component | Test | Visual |
|---|---|---|---|
| Single-valued FK: Child row hidden when FK target outside (view ∪ reference) scope on refTable | SqlQuery.fkRlsVisibilityConditions | TestCrossSchemaFkRlsVisibility.scalarRef_hidesChildRow_whenFkTargetInvisible | — |
| REF_ARRAY: Child row hidden if ANY element outside scope (all-or-nothing) | SqlQuery.refArrayRlsVisibilityCondition | TestCrossSchemaFkRlsVisibility.refArray_hidesChildRow_whenAnyElementInvisible | — |
| REF_ARRAY empty array passes (no elements to check) | SqlQuery.refArrayRlsVisibilityCondition | TestCrossSchemaFkRlsVisibility.refArray_emptyArray_keepsChildRow | — |
| REFERENCE_ALL on refTable keeps Child row visible even without VIEW | SqlQuery.fkRlsVisibilityConditions | TestCrossSchemaFkRlsVisibility.referenceAllOnRefTable_keepsChildRowVisible / refArray_referenceAllOnRefTable_keepsRow | — |
| User with zero refSchema membership: `Column.getRefTable()` throws (pre-existing) | Column.getRefTable | TestCrossSchemaFkRlsVisibility.crossSchema_throwsForUserWithNoMembershipInRefSchema | — |

## GraphQL schema reduction (R.4)

| Behavior | Component | Test | Visual |
|---|---|---|---|
| `hasReferencePermission(table)` plumbing populated from `getPermissionsForActiveUser` | GraphqlTableFieldFactory | TestGraphqlTableFieldFactoryReferencePermission | — |
| REFERENCE-only refTable in nested FK emits thin type (PK fields only) | GraphqlTableFieldFactory.createReferenceOnlyType | TestGraphqlReferenceOnlySchema.referenceOnlyTable_emitsThinTypeOnFkTraversal | — |
| Full type emitted when refTable has VIEW | GraphqlTableFieldFactory.createTableObjectType | TestGraphqlReferenceOnlySchema | — |
| FK field omitted when refTable has neither VIEW nor REFERENCE (ontology exception preserved) | GraphqlTableFieldFactory | TestGraphqlReferenceOnlySchema | — |
| REFERENCE-only table absent from top-level Query (no `query { T { … } }`) | GraphqlFactory.forSchema | TestGraphqlReferenceOnlyTopLevelSuppression.referenceOnlyTable_absentFromTopLevelQuery | — |
| REFERENCE-only table absent from top-level Mutation (insert/update/upsert/delete) | GraphqlTableFieldFactory.getMutationDefinition / deleteMutation | TestGraphqlReferenceOnlyTopLevelSuppression.referenceOnlyTable_absentFromTopLevelMutation | — |
| Aggregate/groupBy fields VIEW-gated (not REFERENCE) | GraphqlFactory.forSchema | TestGraphqlReferenceOnlyTopLevelSuppression | — |
| Cross-schema FK target: thin/full type decided by refSchema's permissions, not factory schema's | GraphqlTableFieldFactory.permissionsFor | TestGraphqlCrossSchemaReferencePermission.thinType_emittedFromCrossSchemaReferenceOnly / fullType_emittedFromCrossSchemaView | — |
| Cross-schema FK target absent when user has no perms in refSchema | GraphqlTableFieldFactory | TestGraphqlCrossSchemaReferencePermission.field_omitted_whenNoPermissionInRefSchema | — |
| Cross-schema FK thin type name pattern `<refSchemaIdentifier>_<refTableIdentifier>` (no collision with same-named table in factory schema) | GraphqlTableFieldFactory.createReferenceOnlyType | TestGraphqlCrossSchemaReferencePermission | — |
| `createRole` / `createGroup` / `deleteGroup` fire `database.getListener().onSchemaChange()` for cache invalidation | SqlRoleManager | TestSqlRoleManager.createRole_firesListener / createGroup_firesListener / deleteGroup_firesListener | — |

## Session permissions surface (R.4e)

| Behavior | Component | Test | Visual |
|---|---|---|---|
| `_session.tablePermissions[].canReference` boolean exposed | GraphqlSessionFieldFactory | TestGraphqlSession.sessionPermissions_canReference_reflectsScope | — |
| `canReference = select.allowsRowAccess() || reference != NONE` — privacy modes return false | GraphqlSessionFieldFactory.buildTablePermissions | TestGraphqlSession.sessionPermissions_privacyScopeCount_doesNotGrantCanReference | — |
| Ontology tables always appear in `tablePermissions` with `canView=true, canReference=true` regardless of explicit role grants | GraphqlSessionFieldFactory.buildTablePermissions | TestGraphqlSession.sessionPermissions_ontologyTable_alwaysVisibleWithCanReference | — |

## Write-time FK visibility guard (R.5)

| Behavior | Component | Test | Visual |
|---|---|---|---|
| INSERT throws `MolgenisException` fail-fast when FK target outside (view ∪ reference) scope | SqlTable.checkFkRlsWriteVisibility | TestFkRlsWriteGuard.insert_throws_whenFkTargetOutsideReferenceScope | — |
| INSERT succeeds with REFERENCE_ALL on refTable | SqlTable.checkFkRlsWriteVisibility | TestFkRlsWriteGuard.insert_succeeds_whenFkTargetWithinReferenceScope | — |
| INSERT succeeds with VIEW_ALL alone (implicit carry) | SqlTable.checkFkRlsWriteVisibility | TestFkRlsWriteGuard.insert_succeeds_whenViewScopeOnRefTable | — |
| UPDATE changing FK to invisible target throws | SqlTable.checkFkRlsWriteVisibility | TestFkRlsWriteGuard.update_throws_whenChangingFkToInvisibleTarget | — |
| UPDATE not touching FK column skips check (validates only changed FK columns) | SqlTable.checkFkRlsWriteVisibility | TestFkRlsWriteGuard.update_skipsCheck_whenFkColumnNotChanged | — |
| REF_ARRAY: any element outside scope throws | SqlTable.checkRefArrayColumnVisibility | TestFkRlsWriteGuard.refArray_throws_whenAnyElementInvisible | — |
| REF_ARRAY: all elements visible succeeds | SqlTable.checkRefArrayColumnVisibility | TestFkRlsWriteGuard.refArray_succeeds_whenAllElementsVisible | — |
| Cross-schema FK target validated against refSchema's permissions | SqlTable.assertAllReferencedKeysVisible | TestFkRlsWriteGuard.crossSchema_throws_whenFkTargetOutsideReferenceScope | — |
| Admin bypasses write-time check | SqlTable.checkFkRlsWriteVisibility | TestFkRlsWriteGuard.admin_bypassesCheck | — |
| Non-RLS refTable: no guard fires (write succeeds without REFERENCE permission) | SqlTable.checkRefColumnVisibility | TestFkRlsWriteGuard.rlsDisabledRefTable_noCheck | — |
| `mg_change_owner=true` alone (no REFERENCE / no VIEW row-access) does NOT grant FK target visibility for writes | SqlTable.assertAllReferencedKeysVisible | TestFkRlsWriteGuard.insert_throws_whenChangeOwnerTrueButNoReferenceOrViewScope | — |
| DELETE has no FK visibility check (no new FK refs introduced) | SqlTable.executeBatch | — (negative — not exercised) | — |

## Open requirements (to-do, become rows above as implemented)

- REQ-B: Performance benchmark < 2× non-RLS baseline at 1M / 5 / 100 / 10k.
- REQ-C: Operator runbook published in `docs/`.
- REQ-D: Materialised view fallback decision pending benchmark.
