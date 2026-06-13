# Spec: Unified Discriminator-Driven Composition (Diamond Inheritance + Modules)

Living guardrail. `Test` links the validating test once written; `Visual` marks manual checks.
Status: Phase A + Phase B (diamond inheritance, EXCLUSIVE) DONE & green. Phase C REVISED (2026-06-12) =
unified column-based discriminator engine, staged C0–C6 (absorbs old Phase D MODULE + Phase G multi-axis).
C0 (discriminator abstraction + rename) DONE & green (2026-06-12, reviewed; trimmed to Design B — scalar
`SUBCLASS` type DROPPED, `mg_tableclass` stays master-style untyped, discriminator engine = `MODULE_ARRAY` only).
C1 (MODULE_ARRAY DDL + value validation) DONE & green (2026-06-12, reviewed PASS-WITH-FIXES, all addressed):
MODULE_ARRAY persists `varchar[]` + survives reload; `values` validated to reference existing `tableType=MODULE`
tables (non-existent/DATA/ONTOLOGY/malformed rejected); enum-family membership enforced on insert+update via core
`TypeUtils.checkEnumMembership` (only when `values` non-empty); mg_tableclass (is-a) + MODULE_ARRAY (composition)
coexist (mg_tableclass readonly/immutable, MODULE_ARRAY mutable, no trigger/name collision). NO materialization/
write-routing/query/gating/hard-delete yet (C2–C6). C2–C6 PLANNED.

**MODEL B PIVOT (2026-06-12, owner):** a MODULE is a **real, queryable table bound to ONE root** (NOT
materialized per consumer). One storage model = Phase B table-per-type on shared root PK; two discriminator
flavors: is-a/`mg_tableclass` (EXCLUSIVE, immutable) vs composition/`MODULE_ARRAY` (MULTIPLE, mutable, no
mg_tableclass). C2 = flag split (skipPk+skipMgTableclass), MODULE-extends-root → real subtype table w/o
mg_tableclass, exclude MODULE subtypes from is-a enumeration, tighten MODULE_ARRAY value validation to
"extends this root" + one-axis, reload round-trip. Materialization/reserved-naming DROPPED; cross-root reuse
intentionally LOST (reversible later). Binding model LOCKED = explicit: module `extends root` +
`tableType=MODULE` (storage binding) AND root's `MODULE_ARRAY.values` list the module per axis (axis
assignment); engine cross-checks each value extends this root + one-axis-per-module (O-5).

**C2 DONE & green (2026-06-13, independently re-verified; STAGED, not committed).** C2.A–C2.E landed + the
`executeSetInherit` param-elimination simplification (single derived 3-arg form). MODULE-extends-root = real subtype
table (PK+FK+KEY1, no mg_tableclass); MODULE excluded from is-a enumeration (catalog-visible); MODULE_ARRAY value must
extend this root + one-axis (O-5); reload round-trip. Verified exit 0 across diamond/module/cross-schema/reload suites;
spotless+PMD clean. NO write-routing/query/gating/hard-delete yet — C3–C6 PLANNED; C4 GENERALIZES
`tableWithInheritanceJoin` into ONE discriminator-driven join (not a parallel composition method).

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
| Column type `MODULE_ARRAY` (composition; renamed from `SUBCLASS_ARRAY` 2026-06-12) exists (enum family, NOT ref). Scalar `SUBCLASS` type DROPPED (Design B) — is-a uses untyped `mg_tableclass` | ColumnType | TestTableMetadataDag | - |
| `Column.values` (List<String>) holds the allowed choices; for discriminators these are `schema.Table` names validated to exist | Column / MetadataUtils | MetadataPersistenceRoundtripTest (persistence); **existence-validation DONE C1** — TestModuleArrayDiscriminator.moduleArrayValuesRejectNonExistentTable / RejectDataTable / RejectOntologyTable / RejectMalformedQualifiedNames | - |
| Composition discriminator is a first-class `MODULE_ARRAY` column; engine loops `getDiscriminatorColumns()` (= MODULE_ARRAY cols). is-a (`mg_tableclass`) is separate, NOT in this set | TableMetadata.getDiscriminatorColumns / Column.isDiscriminator | TestTableMetadataDag.getDiscriminatorColumnsFindsModuleArrayColumns | - |
| Discriminator chosen value(s) are schema-qualified `schema.Table` enum values from `Column.values` | SqlTable / discriminator | _C3_ | - |
| built-in `mg_tableclass` stays the master-style untyped, engine-managed, immutable system column (Design B: NOT typed `SUBCLASS`; executor unchanged from Phase B) | SqlTableMetadataExecutor | _C0 DONE — TestDiamondInheritance / TestInherits / MetadataPersistenceRoundtripTest green_ | - |
| `Column.values` applies to `MODULE_ARRAY` columns (the allowed MODULE `schema.Table` set, validated to exist); `mg_tableclass`/is-a needs no `values` | Column / MetadataUtils / SqlColumnExecutor.validateModuleArrayValues | **C1 DONE** — TestModuleArrayDiscriminator.moduleArrayColumnPersistsAndSurvivesReload + RejectNonExistentTable/RejectDataTable/RejectOntologyTable | - |
| Enum-family values constrained to `Column.values` (membership) on INSERT and UPDATE, only when `values` non-empty; logic in core `TypeUtils.checkEnumMembership`, invoked from SQL write path | TypeUtils.checkEnumMembership / SqlTypeUtils.checkValidation | **C1 DONE** — TestTypeUtils.checkEnumMembership* (5 unit) + TestModuleArrayDiscriminator.insertRejectsOutOfSetValueAndAcceptsInSetValue / updateRejectsOutOfSetValue / enumScalarWithValuesEnforcesAllowedSet / enumWithNoValuesFreeStringAccepted | - |
| is-a identity (`extends` → `mg_tableclass`, single/EXCLUSIVE) references TABLE/DATA subclasses incl. cross-schema | SqlTable / SqlQuery | TestDiamondInheritance; cross-schema = TestCrossSchemaForeignKeysAndInheritance | - |
| `MODULE_ARRAY` (multi, MULTIPLE) references MODULE classes ALWAYS; stored `varchar[]`; COEXISTS with scalar `mg_tableclass` (does NOT replace it) | SqlTable / SqlQuery / SqlTableMetadataExecutor | **DDL+storage DONE C1** — TestModuleArrayDiscriminator.moduleArrayColumnPersistsAndSurvivesReload, mgTableclassAndModuleArrayCoexist; _write/query routing C3–C4_ | - |
| Inheritance (`extends`/`mg_tableclass`) and composition (`MODULE_ARRAY`) may coexist on one table | SqlTable / SqlQuery / SqlTableMetadataExecutor | **DDL/metadata coexistence DONE C1** — TestModuleArrayDiscriminator.mgTableclassAndModuleArrayCoexist (mg_tableclass present + readonly; 2 MODULE_ARRAY independently validated; no collision); _write/query mix smoke C6_ | - |
| Multiple `MODULE_ARRAY` columns = independent modular choice-sets (orthogonal axes) | TableMetadata.getDiscriminatorColumns | **C0 accessor + C1 independent validation** — TestModuleArrayDiscriminator.mgTableclassAndModuleArrayCoexist (2 discriminators); _runtime C3–C6_ | - |
| Diamond child has one PK, one FK per parent, all on single root PK | SqlTableMetadataExecutor.executeSetInherit | TestDiamondInheritance.diamondChildHasSingleRootPrimaryKey, diamondChildHasForeignKeyPerParent, mgTableclassLivesOnlyOnRoot | - |
| Insert into diamond writes shared root exactly once | SqlTable.insertBatch | TestDiamondInheritance.insertAndSelectRoundtripThroughDiamond | - |
| Query joins distinct ancestor set on single root PK | SqlQuery.tableWithInheritanceJoin | TestDiamondInheritance.insertAndSelectRoundtripThroughDiamond, diamondSurvivesSchemaReload | - |
| Module columns projected only when active for the row (membership CASE-WHEN) | SqlQuery.rowSelectFields | _C4_ | - |
| required/visible gated by active set for module columns (`__active_<d>` in JS graph) | SqlTypeUtils | _C5_ | - |
| Deactivating a module on update HARD-DELETEs its materialized row (root row intact) | SqlTable.updateBatch | _C6_ | - |
| Discriminator named column: `mg_tableclass` is the reserved auto-name from `extends`; `MODULE_ARRAY` columns are modeler-named | SqlTableMetadataExecutor | TestDiamondInheritance.mgTableclassLivesOnlyOnRoot, mgTableclassOnRootOnlyInSingleChain | - |
| `tableType=MODULE` (MODEL B) = a real, queryable composition-subtype table bound to ONE root; extends the root with NO mg_tableclass | TableType / SqlTableMetadataExecutor | **C2 DONE** — TestModuleArrayDiscriminator.moduleExtendsRootIsRealTableWithPkFkKey1NoMgTableclass | - |
| MODULE-extends-root → real subtype table keyed on root PK (PK+FK+KEY1), NO mg_tableclass; via param-free derived executeSetInherit (primary-parent ⇒ PK/KEY1; tableType==MODULE ⇒ skip mg_tableclass) | SqlTableMetadataExecutor.executeSetInherit | **C2 DONE** — TestModuleArrayDiscriminator.moduleExtendsRootIsRealTableWithPkFkKey1NoMgTableclass; diamond unchanged: TestDiamondInheritance (13) | - |
| MODULE composition subtypes excluded from is-a enumeration (getSubclassTables / is-a query joins) but stay catalog-visible real tables | TableMetadata.collectSubclassTablesDeduped / SqlQuery | **C2 DONE** — TestModuleArrayDiscriminator.moduleSubtypesExcludedFromIsAEnumeration | - |
| A module may extend other modules (columns flatten via normal inheritance, all rooted at the one root — NO copy/materialize) | TableMetadata.getColumns | **C2 DONE** (transitive root-match) — TestModuleArrayDiscriminator.moduleArrayValueMustExtendThisRoot | - |
| MODULE_ARRAY value must reference a MODULE that EXTENDS this root (shares root) + at most one axis per module (O-5) | SqlColumnExecutor.validateModuleArrayValues / validateNoModuleInMultipleAxes | **C2 DONE** — TestModuleArrayDiscriminator.moduleArrayValueMustExtendThisRoot / moduleArrayValueOneAxisPerModule | - |
| Module subtype tables + MODULE_ARRAY survive schema reload (no new metadata column / migration) | MetadataUtils (existing persistence) | **C2 DONE** — TestModuleArrayDiscriminator.moduleSubtypesAndModuleArraySurviveReload | - |
| Binding is EXPLICIT: module declares `extends root` + tableType=MODULE (storage) AND root's MODULE_ARRAY.values list it (axis); a column edit never mutates the module table (no adoption side-effect) | SqlColumnExecutor / SqlTableMetadataExecutor | **C2 DONE** — TestModuleArrayDiscriminator.moduleArrayValueMustExtendThisRoot | - |
| `MODULE_ARRAY` values are MODULE classes only; `extends`/`mg_tableclass` values are TABLE subclasses | SqlColumnExecutor.validateModuleArrayValues / validation | **C1 DONE (array)** — TestModuleArrayDiscriminator.moduleArrayValuesRejectDataTable / RejectOntologyTable / RejectNonExistentTable; is-a = done | - |
| Discriminator allowed set lives in one `values` field (replaces `refTable`-base + `allowedSubclasses[]`) | Column / MetadataUtils | MetadataPersistenceRoundtripTest | - |
| DB migration: `table_inherits`→`VARCHAR[]` + `column_metadata."values"`; old DB upgrades | Migrations | migration32 (DB v33) + MetadataPersistenceRoundtripTest (TestMigration extension = TODO, deferred per nonparallel-module guidance) | - |
| EMX2 CSV roundtrips multi-parent, MODULE, ENUM/MODULE_ARRAY, `values` | Emx2 IO | _Phase E_ | - |
| Diamond schema survives `SqlSchema.merge` and JSON-model round-trip with ALL parents preserved (not just primary) | SqlSchema.merge, json/Table | TestDiamondInheritance.mergePreservesAllParentsOfDiamondChild, TableJsonRoundtripTest.jsonRoundtripPreservesAllParentsOfDiamondChild | - |
| GraphQL/JSON expose inheritNames, modules, `values` (back-compat inheritName) | GraphqlSchemaFieldFactory, json/Table | _Phase E_ | - |
| JSON `inheritId` carries ALL parents (currently scalar = parent[0] only; needs `inheritIds` list) | json/Table | _Phase E (inheritId scalar truncation, open)_ | - |
| RDF emits one rdf:type per active module + primary class type | Emx2RdfGenerator.dataRowToRdf | _todo_ | - |
| Subtype/variant picker writes to root with discriminator selection | frontend (Phase G) | _todo_ | visual check |
| Old extends write path stays automatic (type from target table) | SqlTable | _todo_ | visual check |
