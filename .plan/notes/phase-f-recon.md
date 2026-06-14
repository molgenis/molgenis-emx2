# Phase F recon — MODULE / MODULE_ARRAY + diamond on the frontend

Consolidated from 6 Explore agents (2026-06-14). file:line verified by agents; lead to re-verify load-bearing details before coding.

## Frontend topology (two stacks + a shared metadata lib + the schema app)
- **bootstrap stack = `apps/molgenis-components`** (Vue component library). Hosts BOTH the record editor
  (RowEdit/EditModal/FormInput) AND the data explorer (TableExplorer→TableMolgenis→TableRow→DataDisplayCell).
  Consumed by `apps/tables` (ViewTable→RoutedTableExplorer) and others. Has its OWN client + metadata query +
  IColumn/ITableMetaData + column-type maps.
- **tailwind stack = `apps/ui` (Nuxt) + `tailwind-components`** (shared Nuxt components: Form/Fields/FormField/
  Field/Input + `useForm` composable; value/EMX2.vue display dispatch). Uses **`apps/metadata-utils`** for types +
  metadata fetch (`metadata.js` GraphQL query, `fetchTableData.ts` getColumnIds, `fieldHelpers.ts`).
- **`apps/schema`** (Vite) — schema editor. Its OWN `utils.ts` schemaQuery + `columnTypes.js` + edit modals + diagrams.
- **Domain/specialised apps** (catalogue, directory, beacon, cranio, ern-ithaca, reports, molgenis-viz, …): consume
  table DATA, not inheritance metadata → mostly unaffected (see blast radius).

## F0 — backend write-enable (the N2 carry; VERIFIED)
- Blocker: `GraphqlTableFieldFactory.rowInputType()` **~:1019** iterates
  `getColumnsIncludingSubclassesExcludingHeadings()` (module-free) → module content cols not in mutation input type.
- Fix: → `getColumnsIncludingSubclassesAndModulesExcludingHeadings()` (already exists, `TableMetadata.java:770`;
  output side already uses it at `createTableObjectType()` **:160** since Phase E).
- Also `getMutationDefinition()` **~:925** uses the module-free variant only as a "should I add an argument" guard;
  root tables always have base cols so it passes — real fix is :1019. Widen :925 too for consistency (low risk).
- Engine already routes: `fetcher()` :984 `TypeUtils.convertToRows(getColumns(),…)` → `table.save/insert/update` →
  `SqlTable.buildValidationUnion`/`activeModuleTables` reads the MODULE_ARRAY discriminator off the Row and writes
  module subtype rows (C3). Required-gating already active-module-aware (`requiredModuleColumnEnforcedOnlyWhenModuleActive`).
  ONLY blocker is the input type omitting the cols.
- Test home: mirror `TestModuleGraphqlDataQuery.java` (graphql module read test) with a MUTATION roundtrip
  (insert+update+deselect) per graphql-test-pattern (pre+post via GraphQL surface).

## Forms / record editor (owner's KEY concern: "select = extended visible expression")
- **bootstrap** `molgenis-components/src/components/forms/`:
  - `FormInput.vue:41-82` `typeToInputMap` — column-type→widget. **No ENUM / ENUM_ARRAY / MODULE_ARRAY**; unknown →
    `<div>UNSUPPORTED TYPE</div>` (:8-14). ENUM_ARRAY today would NOT render (only ONTOLOGY_ARRAY maps to InputOntology).
  - Visible-expr engine: `formUtils.ts:451-466 isColumnVisible` → `:327-372 executeExpression` = `new Function(...colIds, "return eval(`expr`)")`
    fed ALL row values. Reactive via RowEdit watch. `RowEdit.vue:130-143 showColumn` gates per column.
  - required/readonly: `RowEdit.vue:15-22`. Metadata via `EditModal.vue:265 fetchTableMetaData`. Submit:
    `EditModal.vue:298-337` sends all cols except REFBACK.
- **tailwind** `tailwind-components/app/components/form/` + `composables/useForm.ts`:
  - `Input.vue` v-if chain (`NON_REF_ARRAY_TYPES :353-367`). **No ENUM / MODULE_ARRAY** → silent blank (no else).
  - `useForm.ts:88-110 visibilityMap` = per-col computed `new Function(params, "return "+expr)` w/ reactive
    `formValues`; `:112-153 requiredMap` (expression-capable); `:499-512 visibleColumns`.
  - Submit: `:424-454 insert/updateInto` builds `mutation($value:[<T>Input])` → `toFormData`. **Needs F0** so module
    cols are accepted by `<T>Input`.
- **KEY INSIGHT (confirms owner):** the visible-expr machinery is ALREADY capable of show/hide keyed on a
  MODULE_ARRAY value (it's in formValues). Phase F forms = render MODULE_ARRAY as a multi-select of `values`
  (module names) + GROUP module content cols by owning module + reveal a group when its module is selected
  (`discriminator.includes(moduleName)` — an "extended visible expression") + gate required to active modules.
  **Load-bearing unknown to verify:** does column metadata carry the OWNING module table id for flattened module
  cols (IColumn has `table?`/`inherited?`)? Needed to group cols → module. VERIFY before F2.

## Table / data explorer (apps/tables via molgenis-components)
- Render: `DataDisplayCell.vue:20-33 typeMap` — MODULE_ARRAY not mapped → falls to `ListDisplay` (`.includes("ARRAY")`)
  or `StringDisplay` (degrades, raw). Columns listed by default (all non-`mg_`) → module cols WOULD show (null for
  non-activating rows per C4).
- **MUST GATE filter:** `FilterInput.vue:101-103` validator THROWS for any type not in `filterTypeMap` (no ENUM/
  MODULE_ARRAY) → opening a MODULE_ARRAY filter ERRORS. Exclude from filterable set (`TableExplorer.vue:14` ShowHide
  `:exclude` + `FilterSidebar.vue:49-54`). Backend has no filter-by-module support (C4 deferred).
- **MUST GATE sort:** `TableExplorer.vue:713-730 onColumnClick` no type guard → backend rejects order-by module col.
- Query: `queryBuilder.ts getColumnIds` MODULE_ARRAY → default scalar field (included, OK).

## Data client / app shell (apps/ui + tailwind-components display)
- `apps/metadata-utils/src/types.ts`: `IColumn` (:56-83) has `columnType`,`table?`,`inherited?` but **NO `values`**;
  `ITableMetaData` (:93-103) has `tableType` but **NO inheritName/Id/inheritNames/inheritIds**; `CellValueType` union
  (:13-53) **lacks ENUM/ENUM_ARRAY/MODULE/MODULE_ARRAY**.
- `tailwind-components/app/gql/metadata.js:2-41` `_schema` query does NOT request `values`/`inheritNames`/`inheritIds`
  (backend exposes them since E3). Widen here.
- Display: `value/EMX2.vue:42-147` v-else-if chain — ENUM/MODULE_ARRAY → default `{{ columnType }}` text;
  ENUM_ARRAY → ValueList (endsWith ARRAY). Add arms.
- Detail/record: `apps/ui/.../[table]/[entity].vue:65-104` groups by HEADING (reduce). Module-group rendering hooks here.
- Nav: `apps/ui/.../[schema]/index.vue:57-69` + `apps/tables ListTables.vue:89-95` filter `tableType==="DATA"`/`"ONTOLOGIES"`
  → MODULE tables already excluded from top-level (neither). Verify no leak; optional "modules" catalog later.
- `fetchTableData.ts getColumnIds` MODULE_ARRAY → default scalar (included if in metadata col list).

## Schema editor (apps/schema) — biggest gap, most independent
- tableType: `TableEditModal.vue` accepts tableType as PROP, no selector; ontology special-cased (SchemaView.vue:98).
  Backend `TableType{DATA,ONTOLOGIES,MODULE}`. Add a DATA/ONTOLOGIES/MODULE selector.
- columnType list: `apps/schema/src/columnTypes.js:1-41` — **no ENUM/ENUM_ARRAY/MODULE/MODULE_ARRAY**. Add.
- values editor: **ABSENT** in `ColumnEditModal.vue`. Add a values[] editor shown for ENUM/ENUM_ARRAY/MODULE_ARRAY.
- multi-parent extends: `TableEditModal.vue:28-36,130-144` forces single scalar `inheritName` (auto-selects result[0]).
  No diamond representation. `utils.ts:206-211 addTableIdsLabelsDescription` derives scalar `inheritId` from
  `inheritName`; `utils.ts:22-66 schemaQuery` requests scalar `inheritName` only (NOT inheritNames/inheritIds/values).
- diagrams: `SchemaDiagram.vue:93-119` (mermaid) + `NomnomDiagram.vue:89-127` loop scalar `inheritName` → single edge;
  diamond needs loop over inheritNames[].
- read/save: `Schema.vue:242-246` load via schemaQuery; `:164-220` save via `change(tables:[MolgenisTableInput])`
  sending scalar inheritName.

## Blast radius (confirms owner: only inheritance-aware apps materially impacted)
- **inheritName/inheritId/tableExtends read ONLY by `apps/schema`** (8 files) → diamond display work is schema-app-local.
- columnType switches elsewhere mostly have SAFE defaults (tailwind EMX2.vue/CellEMX2.vue default text; molgenis-components
  DataDisplayCell default StringDisplay; metadata-utils tableQuery default col.id) → MODULE_ARRAY degrades, doesn't crash —
  **EXCEPT** `molgenis-components FilterInput.vue` (throws) → the one hard break to gate.
- `metadata-utils/fieldHelpers.ts`: `isArrayType` already TRUE for MODULE_ARRAY (endsWith _ARRAY); `isRefType` FALSE
  (good — module cols aren't refs). Add MODULE/MODULE_ARRAY to CellValueType union.
- `molgenis-viz/utils/defaults.ts emxTypes` incomplete (low priority; charts).

## CRITICAL metadata fact (lead-verified 2026-06-14) — shapes F1/F2
- `_schema` metadata serializes each table's columns via `json/Table.java:73 = tableMetadata.getColumns()`
  (**module-free**). So a ROOT table's metadata column list does **NOT** include module content columns.
  Module columns live under **each MODULE table's own** metadata (`getColumns()` = its inherited root cols + local).
  MODULE tables ARE in `_schema.tables` (catalog-visible real tables, C2).
- Consequence: the data-query OUTPUT TYPE exposes module fields on the root object (E2), but the frontend's
  `getColumnIds` builds its selection FROM the root's metadata cols (module-free) → it won't even SELECT module
  cols today. So both READ and EDIT of module cols require the frontend to EXPAND them.
- **Chosen design (no extra backend metadata change; matches Model B):** a frontend composable expands a root's
  MODULE_ARRAY columns into module groups by resolving each `values` entry (bare module name, same schema) to its
  MODULE table in the already-fetched schema metadata, taking that table's LOCAL columns (its cols minus the root's)
  as the group. "Active" = the discriminator's value array contains that module name. This drives: read-selection
  expansion, form group render+show/hide, and detail-view grouping. Backend stays as-is except F0 (write input).
F0 backend write-enable · F1 metadata plumbing (all stacks) · F2 forms (the minimum + owner's concern) ·
F3 tables (gate filter/sort + render) · F4 ui app shell display/detail/nav · F5 schema editor authoring ·
F6 e2e/visual. MINIMUM PR = F0+F1+F2+F3; FOLLOW-UP PR = F4+F5. F6 woven per surface.
Open decisions for owner: (1) both stacks or tailwind-first? (2) PR cut/order. (3) forms module-group UX.
