# Phase F2b — server-side module cols on containing table: blast-radius recon (lead, 2026-06-14)

Goal of change: root/containing table's `_schema.columns` includes module cols (root-anchored, interleaved by `position`), client drops merge/sort + gates via visibility. See `.plan/decisions.md` 2026-06-14(b).

## Serialization hooks
- `json/Table.java:71-84` builds `columns` by iterating `tableMetadata.getColumns()` (module-free) with a STATEFUL section/heading tracker. Interleaving module cols would let a module SECTION/HEADING reset state for following root cols → must handle module section/heading independently (or assign module cols a fixed/own section).
- `json/Column.java:58` `table = column.getTableName()` ✓ (module col → table=module name). `:121-124` `inherited` = name present in an is-a parent → **module cols get `inherited=false`** (NOT marked inherited). So a module col is identifiable ONLY by `col.table !== self`, NOT by `inherited`.
- `TableMetadata.getColumnsIncludingModules():754` APPENDS module local cols, NO position sort, and is NOT root-anchored (uses `getModuleSubtypeTables()` = modules directly extending THIS table). → for a subclass form table it returns nothing; needs a root-anchored + position-sorted variant.

## GraphQL surface — already there
- `GraphqlSchemaFieldFactory.java:212` exposes `table`, `:259` exposes `inherited` on the `_schema` column type. No backend GraphQL schema change needed for those. Frontend `metadata.js` just doesn't REQUEST `table`/`inherited` yet.

## BLAST RADIUS — the catch
`_schema.tables[].columns` currently MEANS "this table's own + is-a-inherited cols." Consumers rely on it. Module cols have `inherited=false` + `table=module`, so the common `!c.inherited` filter does NOT exclude them — they'd leak in as if local:
- **Schema editor (CRITICAL, F5 surface):** `apps/schema/src/utils.ts:85`, `ProfileManager.vue`, `NomnomDiagram.vue` filter `!c.inherited` → module cols pass → shown as local/editable. `TableView.vue:160-187` / `ColumnView.vue:49` render all cols editable.
- **Data-query builder (needs check, not necessarily a break):** `fetchTableData.ts getColumnIds():68-133` builds the GraphQL selection from `table.columns`. For a module-bearing ROOT this is actually FINE — C4/E2 make module cols selectable-by-name on the root (null for non-activating rows). RISK is is-a SUBCLASS queries: unverified whether the C4 module LEFT-JOIN fires when querying a subclass; if not, selecting module cols on a subclass query fails. → verify, or scope/guard.
- **molgenis-components (MEDIUM):** TableExplorer/FilterSidebar would list module cols as filter/column options.
- **Tests:** `TestGraphqlSchemaFields` has hard-coded `_schema/.../columns/<index>` paths → shift/break. `SchemaMetadataGraphqlSurfaceTest` column lookups. Need updates.

## Principled fix (the real lesson)
"Local columns of table T" ⇔ `col.table === T.id` — NOT `!col.inherited`. The `!inherited` filter was a proxy that worked only for is-a; modules are the first case of `inherited=false && table!==self`. A single shared predicate (e.g. `isLocalColumn(col, table)`) applied across consumers is the robust fix vs whack-a-mole. This pulls F3 (data view) + F5 (schema editor) filter-correctness forward into the F2 change.

## Net assessment
Architecturally the owner is right (server is the consistent layer; mirrors is-a inherited cols). But it is NOT a small json/Table one-liner: it changes the SEMANTICS of `_schema.columns` for all consumers, requires the root-anchored+position-sorted+section-safe serialization, the `col.table===self` filter fix in data-query + schema-editor, frontend gql additions (`table`,`inherited`), and test updates. Bigger than the F2 form slice → confirm scope/timing with owner before fan-out.
