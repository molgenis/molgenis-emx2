# Detail View Like Catalogue — Plan

## Goal
Replace handcrafted catalogue detail pages (1000+ line bespoke Vue files) with a **config-driven system** using generic building blocks in tailwind-components. Same visual quality, 90% less page-specific code.

Two consumers:
1. **`apps/ui` entity page** — generic detail view for any EMX2 table
2. **`apps/catalogue` detail pages** — rich, customized views (datasets, resources, variables)

## Key Design Decisions

### Dumb/Smart Split
- **RecordView** (dumb) — takes `columns: IColumnDisplay[]` + `data: Record<string, any>`, does all rendering. No backend calls.
- **Emx2RecordView** (smart) — fetches metadata + row from backend, merges IColumn → IColumnDisplay, passes to RecordView.

### IColumnDisplay (client-side only)
Extends IColumn with: `displayComponent`, `layout`, `getHref`, `clickAction`, `listConfig`, `displayLabel`, `hidden`. Backend stays clean.

### Master/Detail via listConfig
REF_ARRAY/REFBACK columns get `listConfig` with layout/visibleColumns/pageSize/showSearch/getHref. RecordColumn renders Emx2ListView when listConfig is present.

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

### Phase 3: Emx2ListView for REFBACK/REF_ARRAY — NEXT
Goal: REF_ARRAY and REFBACK columns render as embedded searchable/paginated lists instead of comma-separated inline text.

**Architecture:**
```
RecordColumn (detects REFBACK/REF_ARRAY)
  └── Emx2ListView (self-contained, fetches own data)
        ├── useTableData(refSchemaId, refTableId, filter)
        ├── optional search bar
        ├── InlinePagination
        └── renders as table / cards / list per listConfig
```

**Key: Emx2ListView is self-contained.** It takes schemaId + tableId + filter and fetches its own paginated data. This avoids loading all related rows upfront in the parent record fetch.

**Steps:**
- [ ] Copy Emx2ListView from generic-view, adapt to our IListConfig type
- [ ] RecordColumn: when column is REF_ARRAY/REFBACK, render Emx2ListView instead of ValueEMX2
  - Build filter from parent row's key columns (REFBACK: `{refBackId: {equals: rowId}}`)
  - Pass listConfig (layout, visibleColumns, pageSize, etc.)
  - Need access to parent row data and schema context → pass via props or provide/inject
- [ ] Emx2RecordView: auto-generate listConfig defaults for REF_ARRAY/REFBACK columns
  - refSchemaId from column.refSchemaId
  - refTableId from column.refTableId
  - default pageSize: 10
  - default layout: "table"
- [ ] RecordSection: keep separating "inline" vs "block" columns (REF_ARRAY/REFBACK = block)
- [ ] Story file for Emx2ListView
- [ ] Test with catalogue-demo Resources table (which has REFBACK columns)

**Data flow for REFBACK:**
```
Parent record: Resources { id: "ACBB" }
REFBACK column: datasets (refTableId: "Datasets", refBackId: "resource")
  → Emx2ListView fetches: Datasets where resource.id = "ACBB"
  → Renders as paginated table with search
```

**Data flow for REF_ARRAY:**
```
Parent record already has the array data in row[columnId]
But for large arrays, better to fetch paginated from backend
  → Emx2ListView fetches: refTable where id in [array values]
  → Or: use column filter from parent context
```

### Phase 4: Standard Display Components
- [ ] OntologyTreeDisplay — collapsible tree for ONTOLOGY/ONTOLOGY_ARRAY columns
- [ ] CardGridDisplay — ref items as cards with links
- [ ] FileListDisplay — file download cards
- [ ] IntroDisplay — hero block (logo, website, contact)
- [ ] Export defaultDisplayMap from tailwind-components

### Phase 5: Upgrade [entity].vue
- [ ] Replace 220-line bespoke page with Emx2RecordView
- [ ] Keep edit/delete buttons via #header slot
- [ ] Keep field search filter
- [ ] Nested row click → navigate to detail page (`/schema/table/entityId`)
  - ListView/RecordTable rows get auto-generated getHref from key columns
  - Clicking a row in a REFBACK table opens that record's detail view
  - URL pattern: `/${schemaId}/${refTableId}/${encodedRowId}`
- [ ] Test with various table shapes

### Phase 6: Catalogue Demo
- [ ] Dataset detail page using Emx2RecordView + Emx2DataView
- [ ] Resource/collection detail page (the big one)
- [ ] Visual comparison with handcrafted pages
- [ ] Theme testing (Light, Dark, Molgenis, UMCG, AUMC)
- [ ] Responsive testing (desktop, tablet, mobile)

### Phase 7: Tests & Polish
- [ ] Unit tests for display components
- [ ] Story files for all components
- [ ] E2E smoke test

## What Current Catalogue Pages Need

| Pattern | Catalogue Example | Generic Solution | Status |
|---------|------------------|-----------------|--------|
| Key/value pairs | Name, acronym, dates | RecordSection def-list | DONE |
| Sidebar TOC | Section navigation | Auto-generated SideNav | DONE |
| Conditional sections | Skip empty sections | showEmpty=false + empty detection | DONE |
| Custom links | Navigate to sub-pages | getHref in columnConfig | DONE (type ready) |
| Related table with search | Variables, subpopulations | listConfig → Emx2ListView | Phase 3 |
| Ontology trees | Data categories, conditions | displayComponent tag resolution | Phase 4 |
| Card grid | Networks, publications | CardGridDisplay | Phase 4 |
| File downloads | Documentation files | FileListDisplay | Phase 4 |
| Hero intro | Logo, website, contact | IntroDisplay | Phase 4 |
| Harmonisation grid | Variable harmonisation status | Custom component via displayMap | Phase 6 |

## Files

### Existing (modified)
- `types/types.ts` — IColumnDisplay, IListConfig, IRecordViewConfig
- `display/RecordView.vue` — dumb, IColumnDisplay[], DetailPageLayout + SideNav
- `display/RecordSection.vue` — catalogue styling, IColumnDisplay
- `display/RecordColumn.vue` — uses displayComponent directly
- `display/Emx2RecordView.vue` — smart wrapper, merge logic
- `layout/DetailPageLayout.vue` — sidebar + main layout
- `SideNav.vue` — scrollspy navigation with title

### Next (Phase 3)
- `display/Emx2ListView.vue` — copy from generic-view, adapt to IListConfig
- `display/RecordColumn.vue` — add Emx2ListView rendering for REFBACK/REF_ARRAY

### Future (Phase 4+)
- `display/OntologyTreeDisplay.vue`
- `display/CardGridDisplay.vue`
- `display/FileListDisplay.vue`
- `display/IntroDisplay.vue`
- `apps/ui/pages/[schema]/[table]/[entity].vue` — simplify
