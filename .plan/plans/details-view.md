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
Extends IColumn with: `displayComponent`, `layout`, `getHref`, `clickAction`, `listConfig`, `displayLabel`, `hidden`. Backend stays clean.

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

### Phase 4: Progressive Disclosure for References — NEXT
Goal: Consistent drill-down pattern for REF, REF_ARRAY, and REFBACK.

**Design concept — three levels of detail:**
1. **Compact**: Show primary/secondary key columns inline (scannable)
2. **Preview**: Side modal or expandable showing more fields (quick look)
3. **Full**: Navigate to detail page (deep dive)

**For REF (single reference):**
- Currently: shows refLabel as plain text
- Improve: make clickable, show side modal with record preview on click
- Or: show as compact card with key fields + expand button

**For REF_ARRAY:**
- Currently: inline comma-separated
- Improve: show as embedded table (like REFBACK) with proper filter
- Filter challenge: need to build `{id: {equals: [val1, val2, ...]}}` from parent row data
- Alternative: client-side pagination of parent row's array data

**For REFBACK:**
- Currently: embedded table via Emx2ListView (DONE)
- Improve: add row click → side modal preview → navigate to full detail

**Column importance metadata:**
- Could use `key` field (1 = primary, 2+ = secondary) for compact display
- Or introduce `visible` / `importance` hint on columns
- Determines which columns show in compact list/table views vs full detail

**Steps:**
- [ ] REF values: clickable, open side modal with Emx2RecordView of referenced record
- [ ] REFBACK table rows: clickable, open side modal with record preview
- [ ] REF_ARRAY: render as embedded table (build filter from parent row array values)
- [ ] Side modal component for record preview (reuse existing SideModal + RecordView)
- [ ] Column importance: decide on metadata approach for compact vs full display

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
| Custom links | Navigate to sub-pages | getHref in columnConfig | DONE (type ready) |
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
- `display/RecordColumn.vue` — displayComponent, Emx2ListView for REFBACK
- `display/RecordTable.vue` — conditional sticky first column (key only)
- `display/Emx2RecordView.vue` — smart wrapper, merge logic, auto listConfig for REFBACK
- `display/ListView.vue` — dumb list/table/cards, filters empty/hidden columns
- `display/Emx2ListView.vue` — smart list with useTableData, search, pagination
- `display/InlinePagination.vue` — prev/next
- `layout/DetailPageLayout.vue` — sidebar + main layout
- `SideNav.vue` — scrollspy navigation with title, scroll to top
- `value/Object.vue` — plain text + definition tooltip
- `composables/fetchTableData.ts` — ontology definition in expansion

### Next (Phase 4)
- Side modal component for record preview
- REF click handler → side modal
- REFBACK/REF_ARRAY row click → side modal
- REF_ARRAY filter building

### Future (Phase 5+)
- `apps/ui/pages/[schema]/[table]/[entity].vue` — simplify with Emx2RecordView
- `display/OntologyTreeDisplay.vue`
- `display/CardGridDisplay.vue`
- `display/FileListDisplay.vue`
- `display/IntroDisplay.vue`
