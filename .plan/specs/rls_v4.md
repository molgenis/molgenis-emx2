# RLS v4 Spec — behavioural contract

> Self-contained successor to `rls_v3.md` spec. Master is the base;
> this document covers the RLS extension that opts in per table.

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

| Behavior | Component | Test | Visual |
|---|---|---|---|
| `tables_metadata.rls_enabled BOOLEAN DEFAULT false` | MetadataUtils | TestRlsToggle.flagDefaultsFalse | — |
| `change(tables:[{name, rlsEnabled:true}])` enables RLS on root | GraphQL.tableSchema | TestRlsToggleGraphql.enableRlsRoot | rlsEnabled=true in admin UI |
| Disable rejected when `role_permission_metadata` row exists | SqlTableMetadata | TestRlsToggle.disableRejectedWithPermissions | error toast |
| Enable on subclass rejected with `"enable on root '<X>' instead"` | SqlTableMetadata | TestRlsToggle.enableSubclassRejected | — |
| Enable cascades through inheritance tree | SqlTableMetadata | TestRlsToggle.enableCascadesToChildren | — |
| Disable cascades through inheritance tree | SqlTableMetadata | TestRlsToggle.disableCascadesToChildren | — |
| Root-only metadata; child rows always store `false` | TableMetadata.getRlsEnabled | TestRlsToggle.metadataStoredOnRootOnly | — |
| `mg_owner` + `mg_groups` columns materialised on enable | SqlTableMetadata | TestRlsToggle.materialisesRowColumnsOnEnable | — |
| `mg_owner` backfilled from `mg_insertedBy` on enable | SqlTableMetadata | TestRlsToggle.backfillsOwnerOnEnable | — |

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
| `select_scope='ALL'` returns every row | mg_can_read | TestSqlRoleManager.selectScopeAllReturnsEveryRow (gap C-test-2) | — |
| `select_scope='GROUP'` returns rows whose `mg_groups` overlap user's memberships | mg_can_read | TestSqlRoleManager.groupScopeSeesOnlyGroupRows / TestAsymmetricCollaboration | — |
| `select_scope='OWN'` returns rows whose `mg_owner = current_user` | mg_can_read | TestSqlRoleManager.ownScopeSeesOnlyOwnRows / TestSchemaWideCustomGrants.nullGroupGrant_ownScope_userReadsOwnRowsOnly | — |
| `select_scope='NONE'` returns zero rows | mg_can_read | TestSqlRoleManager.selectScopeNoneReturnsZeroRows (gap C-test-2) | — |
| `insert_scope='GROUP'` requires every group in `NEW.mg_groups` to be one writer holds GROUP/ALL in | mg_can_write_all | TestUpdateScope.groupScopeCanUpdateGroupRow / TestSqlRoleManager.groupScopeUpdatesOnlyGroupRows | — |
| `update_scope='OWN'` rejects update of rows owned by another user | mg_can_write | TestUpdateScope.ownScopeCanUpdateOnlyOwnRow / TestSqlRoleManager.ownScopeUpdatesOnlyOwnRows | — |
| Custom-role scope row absent ⇒ no row visible (effective NONE) | mg_can_read | TestSqlRoleManager.absentRpmRowMeansNoRowVisible (gap C-test-1) | — |

## System roles (hardcoded scopes)

| Behavior | Component | Test | Visual |
|---|---|---|---|
| Owner/Manager/Editor: every verb = ALL | mg_can_read / mg_can_write | TestSqlRoleManager.editorCanReadAndWrite | — |
| Viewer: `select=ALL`, others NONE | mg_can_read / mg_can_write | TestSqlRoleManager.viewerCanReadRows / viewerCannotWriteRows | — |
| System roles never have rows in `role_permission_metadata` | trigger + Java | TestSqlRoleManager.deleteRoleRejectsSystemRoleNames + RPM trigger test (gap C-test-3) | — |
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
| System role + group binding rejected | GraphQL.changeMembers | TestMembersGraphql.systemRoleNoGroup | error toast |

## Privacy view modes (RLS or non-RLS table)

| Behavior | Component | Test | Visual |
|---|---|---|---|
| `select_scope='EXISTS'` projection: result clamped to {0, 1} | SqlQuery | TestPrivacy.existsProjection (gap D-test-1) | — |
| `select_scope='COUNT'` projection: floor of 10 via `mg_privacy_count` | SqlQuery + mg_privacy_count | TestSelectScope.countScopeRlsPassThroughSeesAllRows (partial) + TestPrivacy.countFloor10 (gap D-test-1) | — |
| `select_scope='RANGE'` projection: histogram bucket lower-bounded at 10 | SqlQuery | TestPrivacy.rangeFloor10 (gap D-test-1) | — |
| `select_scope='AGGREGATE'` projection: SUM/AVG only when count ≥ 10 | SqlQuery | TestPrivacy.aggregateFloor10 (gap D-test-1) | — |
| Policy predicate `true` for privacy scopes on RLS tables (pass-through) | mg_can_read | TestPrivacy.passThroughAtPolicyLayer (gap D-test-2) | — |
| Privacy scopes valid on non-RLS tables (projection only, no policy) | Schema.upsertRolePermissions | TestPrivacy.privacyOnNonRlsTable (gap D-test-2) | — |
| Any non-NONE `select_scope` ⇒ `GRANT SELECT` to role's PG role | SqlRoleManager | TestPrivacy.nonNoneScopeImpliesSelectGrant | — |

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
| Custom role no group ⇒ PG GRANT only, no membership row | Schema.changeMembers | TestGraphqlMembers.customRoleNoGroupGrantOnly | — |
| Custom role with group ⇒ PG GRANT + membership row | Schema.changeMembers | TestGraphqlMembers.customRoleWithGroupBoth | — |
| `drop(members)` group-aware: present removes that row only; absent revokes role + clears all rows | GraphqlSchemaMutations | TestGraphqlMembers.dropGroupAware | — |
| Escalation guard: only admin/Owner/Manager grants any custom role | GraphqlSchemaMutations | TestGraphqlMembers.escalationGuard | — |
| `_schema.groups` + `change/drop(groups)` central mutations | GraphqlSchemaMutations | TestGraphqlGroups.crud | groups admin |

## Open requirements (to-do, become rows above as implemented)

- REQ-A: Cross-schema FK target visibility (Phase F.1).
- REQ-B: Performance benchmark < 2× non-RLS baseline at 1M / 5 / 100 / 10k.
- REQ-C: Operator runbook published in `docs/`.
- REQ-D: Materialised view fallback decision pending benchmark.
