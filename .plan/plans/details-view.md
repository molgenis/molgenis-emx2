# Detail View Like Catalogue — Plan

## Goal
Replace handcrafted catalogue detail pages (1000+ line bespoke Vue files) with a **config-driven system** using generic building blocks in tailwind-components. Same visual quality, 90% less page-specific code.

Two consumers:
1. **`apps/ui` entity page** — generic detail view for any EMX2 table
2. **`apps/catalogue` detail pages** — rich, customized views (datasets, resources, variables)

## Key Design Decisions

### Dumb/Smart Split
- **RecordView** (dumb) — takes `columns: IColumnDisplay[]` + `data`, does all rendering. No backend calls.
- **Emx2RecordView** (smart) — fetches metadata + row, merges IColumn → IColumnDisplay, passes to RecordView.
- **ListView** (dumb) — takes `rows` + `columns` + `config`, renders as table/cards/list.
- **Emx2ListView** (smart) — fetches paginated data, delegates to ListView.

### IColumnDisplay (client-side only)
Extends IColumn with: `displayComponent`, `listConfig`, `displayLabel`. Backend stays clean. REF/SELECT/RADIO navigation uses automatic NuxtLink wrapping via `buildRefHref` — not config-driven.

### Master/Detail via listConfig
REFBACK columns get `listConfig` auto-generated. RecordColumn renders Emx2ListView for REFBACK. REF_ARRAY stays inline for now (data already in parent row).

### Tags → Component Resolution
`displayMap` on Emx2RecordView resolves tags → `displayComponent` during merge. Dumb components just check `column.displayComponent`.

## Phases

### Phase 1: Foundation — DONE
- [x] Copy core display components from feat/generic-view
- [x] Add `tags?: string[]` to IColumn
- [x] Copy story files from feat/generic-view

### Phase 2: IColumnDisplay + Dumb RecordView Refactor — DONE
- [x] Define IColumnDisplay, IListConfig, IRecordViewConfig in types/types.ts
- [x] RecordView takes `columns: IColumnDisplay[]` + `data` (not metadata + row)
- [x] RecordSection/RecordColumn work with IColumnDisplay
- [x] Emx2RecordView merges IColumn + columnConfig + displayMap → IColumnDisplay[]
- [x] Remove displayMap prop drilling (only on Emx2RecordView, resolved to displayComponent)
- [x] RecordView wraps in DetailPageLayout + auto-generates SideNav from SECTION columns
- [x] SideNav title from key=1 columns, click scrolls to top
- [x] mg_top_of_form handling (render children, hide heading)
- [x] Update story files

### Phase 2b: Catalogue Styling — DONE
- [x] Remove colons from definition list labels
- [x] Section container: text-title-contrast, last:rounded-b-50px
- [x] Headings: text-heading-4xl font-display uppercase (both sections and headings)
- [x] Outer spacing: lg:px-[30px] px-0
- [x] Section gap: grid lg:gap-2.5 gap-0 (matching ContentBlocks)
- [x] Description: mb-5 prose max-w-none
- [x] Hide empty sections from both rendering and SideNav
- [x] Empty value detection: null, undefined, "", [], {}

### Phase 3b: Subclass Column Support — DONE
- [x] `fetchTableMetadata` composable: `includeSubclassColumns` option fetches inherited columns across class hierarchy
- [x] `getSubclassColumns` composable: traverses inheritance tree to collect all columns from subclasses

### Phase 3: ListView + Emx2ListView — DONE
- [x] ListView (dumb): table/cards/list modes, filters SECTION/HEADING/mg_ columns
- [x] Emx2ListView (smart): useTableData, search (shown when >1 page), pagination
- [x] RecordColumn: REFBACK → Emx2ListView with auto-built filter
- [x] Emx2RecordView: auto-generates listConfig for REFBACK (table, pageSize 10, search)
- [x] hideColumns in IListConfig: auto-hides refBackId column in nested tables
- [x] Hide columns where all rows are empty in nested tables
- [x] RecordTable: first column only sticky/linked when primary key
- [x] Ontology values: plain text with (!) definition tooltip (CustomTooltip)
- [x] fetchTableData: include `definition` in ontology GraphQL expansion

### Phase 4: REF_ARRAY Display + Row Navigation — DONE
Goal: Consistent table rendering for both REF_ARRAY and REFBACK, with clickable rows.

**4a. REF_ARRAY as Emx2ListView table** — DONE
- [x] `isRefArrayColumn()` in displayUtils.ts (REF_ARRAY, MULTISELECT, CHECKBOX)
- [x] RecordColumn.vue: async `refArrayFilter` — fetches ref table metadata, finds REFBACK column, builds filter
- [x] `showListView` triggers for both REFBACK and REF_ARRAY (when listConfig + filter ready)
- [x] Emx2RecordView.vue: auto-generates listConfig for REF_ARRAY (table, pageSize 10, search)
- [x] REF_ARRAY and REFBACK now render identically via Emx2ListView
- [x] Tests for `isRefArrayColumn` in displayUtils.spec.ts

**4b. Row click for REF values** — DONE
- [x] REF/SELECT/RADIO values: automatic NuxtLink to detail page (commit 0ce1c5c30)

**4c. REFBACK table row click → detail page** — DONE
- [x] RecordTable.vue: `schemaId`/`tableId` props, async `handleRowClick` with getPrimaryKey + buildRefHref + navigateTo
- [x] Row gets `cursor-pointer` + `@click` when schemaId/tableId provided
- [x] ListView.vue: threads `schemaId`/`tableId` to RecordTable
- [x] Emx2ListView.vue: passes `schemaId`/`tableId` to ListView

**4d. REF_ARRAY row click → detail page** — DONE
- [x] Comes for free with 4c (Emx2ListView tables have clickable rows)

**Deferred:**
- Side modal preview (nice-to-have, not priority — direct navigation is sufficient)
- Column importance metadata (can revisit when needed)

### Phase 5: Upgrade [entity].vue
- [ ] Replace 220-line bespoke page with Emx2RecordView
- [ ] Keep edit/delete buttons via #header slot
- [ ] Keep field search filter
- [ ] Nested row click → navigate to detail page (`/schema/table/entityId`)
  - ListView/RecordTable rows get auto-generated getHref from key columns
  - Clicking a row in a REFBACK table opens that record's detail view
  - URL pattern: `/${schemaId}/${refTableId}/${encodedRowId}`
- [ ] Test with various table shapes

### Phase 6: Standard Display Components
- [ ] OntologyTreeDisplay — collapsible tree for ONTOLOGY/ONTOLOGY_ARRAY columns
- [ ] CardGridDisplay — ref items as cards with links
- [ ] FileListDisplay — file download cards
- [ ] IntroDisplay — hero block (logo, website, contact)
- [ ] Export defaultDisplayMap from tailwind-components

### Phase 7: Catalogue Demo
- [ ] Dataset detail page using Emx2RecordView + Emx2DataView
- [ ] Resource/collection detail page (the big one)
- [ ] Visual comparison with handcrafted pages
- [ ] Theme testing (Light, Dark, Molgenis, UMCG, AUMC)
- [ ] Responsive testing (desktop, tablet, mobile)

### Phase 8: Tests & Polish
- [ ] Unit tests for display components
- [ ] Story files for all components
- [ ] E2E smoke test

## What Current Catalogue Pages Need

| Pattern | Catalogue Example | Generic Solution | Status |
|---------|------------------|-----------------|--------|
| Key/value pairs | Name, acronym, dates | RecordSection def-list | DONE |
| Sidebar TOC | Section navigation | Auto-generated SideNav | DONE |
| Conditional sections | Skip empty sections | showEmpty + empty detection | DONE |
| REF navigation | Navigate to referenced record | Auto NuxtLink via buildRefHref | DONE |
| REFBACK tables | Subpopulations, datasets | Emx2ListView nested table | DONE |
| Ontology tooltips | Design type (!) icon | CustomTooltip + definition | DONE |
| Hide empty columns | Nested tables | Auto-filter in ListView | DONE |
| REF drill-down | Click resource → details | Side modal preview | Phase 4 |
| REF_ARRAY tables | Networks, publications | Embedded table + filter | Phase 4 |
| Ontology trees | Data categories, conditions | OntologyTreeDisplay | Phase 6 |
| Card grid | Networks as cards | CardGridDisplay | Phase 6 |
| File downloads | Documentation files | FileListDisplay | Phase 6 |
| Hero intro | Logo, website, contact | IntroDisplay | Phase 6 |
| Harmonisation grid | Variable harmonisation | Custom displayMap component | Phase 7 |

## Files

### Done
- `types/types.ts` — IColumnDisplay, IListConfig, IRecordViewConfig, hideColumns
- `display/RecordView.vue` — dumb, IColumnDisplay[], DetailPageLayout + SideNav
- `display/RecordSection.vue` — catalogue styling, IColumnDisplay, schemaId/parentRowId passthrough
- `display/RecordColumn.vue` — displayComponent, Emx2ListView for REFBACK, NuxtLink for REF/SELECT/RADIO
- `display/RecordTable.vue` — conditional sticky first column (key only)
- `display/Emx2RecordView.vue` — smart wrapper, merge logic, auto listConfig for REFBACK
- `display/ListView.vue` — dumb list/table/cards, filters empty/hidden columns
- `display/Emx2ListView.vue` — smart list with useTableData, search, pagination
- `display/InlinePagination.vue` — prev/next
- `layout/DetailPageLayout.vue` — sidebar + main layout
- `SideNav.vue` — scrollspy navigation with title, scroll to top
- `value/Object.vue` — plain text + definition tooltip
- `composables/fetchTableData.ts` — ontology definition in expansion
- `composables/fetchTableMetadata.ts` — metadata fetch with includeSubclassColumns option
- `composables/getSubclassColumns.ts` — inheritance tree column collector
- `composables/displayUtils.ts` — pure utility functions (buildRefHref, getPrimaryKey, etc.)

### Next (Phase 4)
- RecordColumn.vue / ValueList.vue — REF_ARRAY as bulleted list of NuxtLinks
- RecordTable.vue — full-row click navigation for REFBACK tables

### Future (Phase 5+)
- `apps/ui/pages/[schema]/[table]/[entity].vue` — simplify with Emx2RecordView
- `display/OntologyTreeDisplay.vue`
- `display/CardGridDisplay.vue`
- `display/FileListDisplay.vue`
- `display/IntroDisplay.vue`
