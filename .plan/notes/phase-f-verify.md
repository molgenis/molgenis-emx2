# Phase F verify-before-code findings (lead, 2026-06-14)

Re-verified the load-bearing details from the plan's "Open verify-before-code" list + F0 surface
before delegating F0/F1. All confirmed.

## F0 surface (GraphqlTableFieldFactory.java) — CONFIRMED
- `rowInputType()` :1012, loop at **:1019** uses `getColumnsIncludingSubclassesExcludingHeadings()`
  (module-free) → THE blocker. Change to `getColumnsIncludingSubclassesAndModulesExcludingHeadings()`.
- `getMutationDefinition()` :918, guard at **:925** uses the same module-free method → widen for
  consistency (root always has base cols so it passes today; low risk).
- Output side `createTableObjectType()` **:160** ALREADY uses
  `getColumnsIncludingSubclassesAndModulesExcludingHeadings()` (since Phase E2).
- Target method `getColumnsIncludingSubclassesAndModulesExcludingHeadings()` EXISTS at
  `TableMetadata.java:770` (→ :762 `...AndModules` → adds each `getModuleSubtypeTables()`'s
  `getLocalColumns()`).

## F1 verify-before-code items — ALL CONFIRMED
1. **Module-col output field name == module's LOCAL column id.** `createTableField` (:168) names every
   field `col.getIdentifier()` — no prefix/namespacing. Module cols enter via `getModuleSubtypeTables()`
   → `moduleTable.getLocalColumns()` (TableMetadata.java:756-757). So the data-query output field for a
   module col = that column's own identifier → a frontend selection of `<module local col id>` resolves.
2. **MODULE tables + their columns ARE in `_schema.tables`.** `json/Schema.java:32`
   `list.addAll(schema.getTables())` then `:37 tables.add(new Table(t, minimal))` — NO filter by
   tableType. Each MODULE table is serialized; `json/Table.java:73` builds its column list from
   `tableMetadata.getColumns()` (= the module's inherited root cols + its local cols).
3. **No per-column owning-module field; owning module derivable via `values`→table (D4).** A ROOT table's
   `json/Table` column list comes from `getColumns()` (module-free) → does NOT contain module content
   cols. The module's cols live ONLY under each MODULE table's own `_schema.tables` entry. There is no
   per-column `table`/owner field carrying the owning module on the root. So the frontend composable MUST
   resolve: root's MODULE_ARRAY col `values` (bare module names, same schema) → the named MODULE table in
   `_schema.tables` → that table's LOCAL cols (its `getColumns()` minus the root's columns) = the module
   group. "Active" = the discriminator value array contains that module name. Exactly D4.

## Corrected frontend file paths (recon said "tailwind-components/…"; real prefix is "apps/tailwind-components/…")
- gql metadata query: `apps/tailwind-components/app/gql/metadata.js`
- value display dispatch: `apps/tailwind-components/app/components/value/EMX2.vue`
- form input dispatch: `apps/tailwind-components/app/components/Input.vue` (directly under components/, NOT form/)
- metadata-utils: `apps/metadata-utils/src/{types.ts,fieldHelpers.ts}` (composable lands here or in
  tailwind-components per step-down / 2-consumer rule; src/ currently has IQueryMetaData, fieldHelpers,
  generic, index, tableQuery, toFormData, types)
