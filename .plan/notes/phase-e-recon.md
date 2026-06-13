# Phase E recon notes (2026-06-13)

Consolidated from 5 Explore agents. file:line verified by agents; lead to re-verify load-bearing
ones before any code. Owner steer (2026-06-13): hope Phase E is METADATA-ONLY for CSV; GraphQL/JSON
"implicit" (data query should already surface module cols of the hierarchy); RDF "only retrieve what
is there", maybe just emit all module columns too. Reconciliation below.

## 1. CSV IO (molgenis-emx2-io / Emx2.java) — METADATA-ONLY, owner hope HOLDS
- `extends` multi-parent: ALREADY round-trips. Import `Emx2.java:66` `setInheritNames(row.getStringArray(TABLE_EXTENDS))` (getStringArray splits on comma); export `:248-250` `String.join(",", getInheritNames())`. NO change.
- `columnType`: ALREADY round-trips MODULE_ARRAY/ENUM/ENUM_ARRAY via generic `ColumnType.valueOf(...)` import `:118-119` / `toString().toLowerCase()` export `:294-295`. NO change.
- `tableType=MODULE`: import OK (`:70-74` `TableType.valueOf`); **EXPORT BROKEN** `:251-252` emits only "ONTOLOGIES", else null → MODULE round-trips as DATA. **FIX (metadata, ~1 line): emit non-DATA tableType.**
- `Column.values`: NOT handled in CSV at all. Core `Column.values` exists (`Column.java:46`). **ADD (metadata): header const `VALUES="values"`, import parse `setValues(row.getStringArray(VALUES))`, export emit, add to `getHeaders()`.** Header naming: lowercase camel like `semantics`/`defaultValue`.
- `position` round-trip: read on import (`:149` auto-assign global counter; explicit `COLUMN_POSITION` honored) but NOT emitted on export. Relevant to cross-module order (item 6).
- Tests live in: TestExtends, TestImportExportEmx2DataAndMetadata, TestOntologyMetadataExport, TestTypesImport (molgenis-emx2-io/src/test).
- **Data import** for a root activating modules is NOT an Emx2.java concern — module col values are extra columns on the root data row; C3 write routing handles them. So owner's "metadata only" holds.

## 6. Cross-module column ORDER (Phase H prereq)
- Import: single GLOBAL position counter across the whole file (`Emx2.java:48` `columnPosition=0`, `:149` `setPosition(columnPosition++)`); explicit `position` column honored per-row if present.
- Export: columns flattened from all tables, sorted by `Comparator.comparing(getRootTableName).thenComparing(getPosition)` (`:231`). So order is grouped BY ROOT TABLE (alphabetical), then position within root. **Position value NOT exported.**
- Conclusion: within one root, row order / explicit position controls column order INCLUDING module-owned columns IF module cols carry positions in the same root's space — BUT export groups by getRootTableName so a module subtype table (its own table name) may sort as its own group, NOT interleaved with root. NEEDS verification: does a module subtype col report the ROOT's name via getRootTableName (→ interleaves) or its own (→ separate group)? If root, cross-module order already works via position; if own table, Phase H needs file-global ordering. **OPEN — verify getRootTableName on a module col + whether export should emit position.**

## 2. GraphQL (_schema METADATA surface) — NOT implicit, explicit field wiring
- molgenis-emx2-graphql/GraphqlSchemaFieldFactory.java. Output table type `:339-394`: exposes `inheritName` `:368`, `inheritId` `:371-373`, NO `inheritNames`/`modules`. Input table type `:497-538`: `inheritName` `:515-517`, no plural. Column output `:208-338`: `semantics` `:336`, NO `values`. Column input `:404-496`: no `values`.
- Path: query = `JsonUtil.schemaToJson(schema.getMetadata())` `:569`; mutation = args→JSON→`JsonUtil.jsonToSchema` `:947-948`. Jackson reflects json/Table+json/Column POJOs, BUT GraphQL only surfaces fields explicitly declared in the field factory.
- Constants `GraphqlConstants.java:30` INHERIT_NAME, `:31` INHERIT_ID. Add INHERIT_NAMES/MODULES/VALUES.
- To expose inheritNames/modules/values on `_schema`: add field defs (output+input) + json model fields. ~8 small edits. **This is the METADATA introspection surface — separate from the DATA query the owner means by "implicit".**
- Tests: TableJsonRoundtripTest, TestGraphqlSchemaFields.testTableAlterDropOperations (graphql-test-pattern).

## 3. JSON model (json/Table.java, json/Column.java, json/Schema.java)
- `inheritNames` ALREADY round-trips: Table.java field `:21`, ctor `:53-57`, Schema.java reverse `:49` `setInheritNames`. Locked by TableJsonRoundtripTest.jsonRoundtripPreservesAllParentsOfDiamondChild.
- **inheritId truncation**: Table.java `:56` `inheritId = getInheritedTables().get(0).getIdentifier()` = parent[0] only. CONSUMERS of inheritId: ONLY json/Table + the GraphQL field def `:372` + frontend apps/schema/src/utils.ts:211 which **COMPUTES inheritId locally from inheritName (does NOT read it from API)**. So dropping the scalar would break no real consumer; adding `inheritIds` list is the safer symmetric fix. **DECISION NEEDED: add inheritIds vs drop inheritId.**
- json/Column.java: NO `values` field. Core Column has it. Add for round-trip (ties to GraphQL values).
- No `modules` field in json/Table.

## 4. RDF (molgenis-emx2-rdf / Emx2RdfGenerator.java)
- `dataRowToRdf` `:257`. rdf:type emitted: table IRI `:268`, LD_OBSERVATION `:269`, per-semantics `:270-273`. Rows fetched per concrete subtype filtered by `mg_tableclass='schema.T'` (RdfGenerator.java:51-66).
- Active modules ARE derivable per row: `table.getMetadata().getDiscriminatorColumns()` → `row.getStringArray(discCol)` = bare module names. Module table IRI via `schema.getTable(name)` + `tableIRI`. ColumnTypeRdfMapper.java:86 maps MODULE_ARRAY→SKIP (emits nothing for the array itself).
- Spec wanted: one rdf:type per active module. Owner steer: "only retrieve what is there, maybe no change except using all columns of the modules too" → emphasis on emitting module COLUMN VALUES, rdf:type-per-module is softer. **DECISION NEEDED: keep rdf:type-per-active-module (FHIR meta.profile analog) or just emit module col values?**
- **OPEN (data-path): does the RDF VALUE emission loop iterate a column set that already includes module cols, or getColumns()-excluding? = same linchpin as GraphQL data query.** Recon covered rdf:type, not the value-emission column loop.

## 5. Migration / TestMigration
- Migrations.java:25 `SOFTWARE_DATABASE_VERSION=33`; `:198-203` `if(version<33) executeMigrationFile("migration32.sql", ...)`.
- migration32.sql: `ALTER COLUMN table_inherits TYPE VARCHAR[] USING ARRAY[...]` + `column_metadata ADD COLUMN IF NOT EXISTS "values" VARCHAR[]`.
- TestMigration in SEPARATE module `backend/molgenis-emx2-nonparallel-tests` (runs LAST, exclusive DB; build.gradle:194-195). Pattern: pre-state assert → run migrationN.sql → post-state assert (existing migration2 role test `:48-91`).
- **ADD migration33 assertion**: after migrate, assert `table_inherits` is VARCHAR[] + `column_metadata."values"` VARCHAR[] exists. Deferred originally per nonparallel guidance; safe to add here.

## LINCHPIN to verify (owner's "implicit" hope): the DATA-path column enumeration
C4 DELIBERATELY left `getColumnsIncludingSubclasses` UNTOUCHED and resolved module cols ONLY in SqlQuery
(getColumnsIncludingActiveModules used only there). So:
- Does the GraphQL DATA query object type for a root table include module-subtype columns? (which getColumns* feeds the GraphQL type builder?)
- Does RDF data-value emission iterate a module-inclusive column set?
If both use getColumns()/getColumnsIncludingSubclasses() → module cols are NOT implicit yet; Phase E
must point them at a module-inclusive surface (small but real). VERIFY before answering owner.
