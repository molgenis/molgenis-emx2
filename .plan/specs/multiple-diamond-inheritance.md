# Spec: Unified Discriminator-Driven Composition (Diamond Inheritance + Modules)

Living guardrail. `Test` links the validating test once written; `Visual` marks manual checks.
Status: Phase A + Phase B (diamond inheritance, EXCLUSIVE) DONE & green. Phases C–G PLANNED.

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| A table may declare multiple parents via `setInheritNames(String...)` (no `addInheritName`) | TableMetadata / SqlTableMetadata | TestTableMetadataDag, TestDiamondInheritance | - |
| Multi-parent DAG validated ONCE before any DDL — rejection leaves no partial state | SqlTableMetadata.setInheritNames | TestDiamondInheritance.twoRootsRejected | - |
| Inheritance DAG must have exactly one common root; >1 root throws | TableMetadata.getRootTable | TestDiamondInheritance.twoRootsRejected | - |
| Cyclic inheritance is rejected | TableMetadata DAG traversal | TestTableMetadataDag.getAllInheritNamesThrowsOnGenuineCycle | - |
| Columns reachable via two parents appear once, stable position | TableMetadata.getColumns/getLocalColumns | TestTableMetadataDag.getColumnsIncludingSubclassesDeduplicatesInDiamond, getSubclassTablesDeduplicatesInDiamond | - |
| Duplicate column name across parents/modules = validation error | SqlTableMetadata.validateInheritanceDag | TestDiamondInheritance.duplicateColumnAcrossParentsRejected, addColumnRejectedWhenCollidesViaSecondParentSubclass | - |
| Existing single `extends` schemas load & behave unchanged (desugar) | TableMetadata + SqlTableMetadata | TestInherits, TestExtends, TestCrossSchemaForeignKeysAndInheritance (green) | - |
| Generic `ENUM` / `ENUM_ARRAY` column types exist (string + `values` list) | ColumnType | TestTableMetadataDag | - |
| New column types `SUBCLASS` / `SUBCLASS_ARRAY` exist (enum family, NOT ref) | ColumnType | TestTableMetadataDag | - |
| `Column.values` (List<String>) holds the allowed choices; for discriminators these are `schema.Table` names validated to exist | Column / MetadataUtils | MetadataPersistenceRoundtripTest (persistence done; existence-validation = Phase C) | - |
| Discriminator chosen value(s) are schema-qualified `schema.Table` enum values from `Column.values` | SqlTable / discriminator | _Phase C_ | - |
| `extends` auto-derives its discriminator `values` from its subclasses (back-compat) | TableMetadata / SqlTableMetadata | _Phase C_ | - |
| `SUBCLASS` (single) = one type per row (EXCLUSIVE) | SqlTable / SqlQuery | _Phase C (scalar path proven by TestDiamondInheritance)_ | - |
| `SUBCLASS_ARRAY` (multi) = several active types per row (MULTIPLE) | SqlTable / SqlQuery | _Phase C_ | - |
| Diamond child has one PK, one FK per parent, all on single root PK | SqlTableMetadataExecutor.executeSetInherit | TestDiamondInheritance.diamondChildHasSingleRootPrimaryKey, diamondChildHasForeignKeyPerParent, mgTableclassLivesOnlyOnRoot | - |
| Insert into diamond writes shared root exactly once | SqlTable.insertBatch | TestDiamondInheritance.insertAndSelectRoundtripThroughDiamond | - |
| Query joins distinct ancestor set on single root PK | SqlQuery.tableWithInheritanceJoin | TestDiamondInheritance.insertAndSelectRoundtripThroughDiamond, diamondSurvivesSchemaReload | - |
| Module columns projected only when active for the row | SqlQuery.rowSelectFields | _Phase C_ | - |
| required/visible gated by active type set for subtype columns | SqlTypeUtils | _Phase C_ | - |
| Deactivating a type on update deletes its subtype row | SqlTable.updateBatch | _Phase C_ | - |
| `mg_tableclass` lives on single shared root, exactly once (scalar when EXCLUSIVE; array when MULTIPLE) | Constants / SqlTableMetadataExecutor | TestDiamondInheritance.mgTableclassLivesOnlyOnRoot, mgTableclassOnRootOnlyInSingleChain (array variant = Phase C) | - |
| `tableType=MODULE` = option-only, not a standalone top-level table | TableType / schema exposure | TestTableMetadataDag.tableTypeModuleHasIsModuleHelper (scaffolding; materialization = Phase D) | - |
| Module materialized per host as `<Host>.<Module>` on host root PK | SqlTableMetadataExecutor | _Phase D_ | - |
| Same module reused across hierarchies = independent physical tables | SqlTableMetadataExecutor | _Phase D_ | - |
| A module may extend other modules (columns flatten) | TableMetadata / materialization | _Phase D_ | - |
| A table may declare multiple discriminator columns (orthogonal axes) | TableMetadata.getDiscriminatorColumns | TestTableMetadataDag.getDiscriminatorColumnsFindsSubclassTypedColumns (accessor; multi-axis runtime = Phase G) | - |
| Discriminator allowed set lives in one `values` field (replaces `refTable`-base + `allowedSubclasses[]`) | Column / MetadataUtils | MetadataPersistenceRoundtripTest | - |
| DB migration: `table_inherits`→`VARCHAR[]` + `column_metadata."values"`; old DB upgrades | Migrations | migration32 (DB v33) + MetadataPersistenceRoundtripTest (TestMigration extension = TODO, deferred per nonparallel-module guidance) | - |
| EMX2 CSV roundtrips multi-parent, MODULE, ENUM/SUBCLASS(_ARRAY), `values` | Emx2 IO | _Phase E_ | - |
| Diamond schema survives `SqlSchema.merge` and JSON-model round-trip with ALL parents preserved (not just primary) | SqlSchema.merge, json/Table | TestDiamondInheritance.mergePreservesAllParentsOfDiamondChild, TableJsonRoundtripTest.jsonRoundtripPreservesAllParentsOfDiamondChild | - |
| GraphQL/JSON expose inheritNames, modules, `values` (back-compat inheritName) | GraphqlSchemaFieldFactory, json/Table | _Phase E_ | - |
| JSON `inheritId` carries ALL parents (currently scalar = parent[0] only; needs `inheritIds` list) | json/Table | _Phase E (inheritId scalar truncation, open)_ | - |
| RDF emits one rdf:type per active module + primary class type | Emx2RdfGenerator.dataRowToRdf | _todo_ | - |
| Subtype/variant picker writes to root with discriminator selection | frontend (Phase G) | _todo_ | visual check |
| Old extends write path stays automatic (type from target table) | SqlTable | _todo_ | visual check |
