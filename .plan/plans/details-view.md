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

## Catalogue Gap Analysis (List + Detail views only)

### What We Have vs What Catalogue Needs

#### DETAIL VIEWS — Catalogue has 5 detail pages (~1,950 lines of bespoke code)

| Catalogue Page | Lines | Can Replace Now? | Gaps |
|----------------|-------|------------------|------|
| Resource/Collection detail | 1,014 | Partially | Ontology trees, card grids, file downloads, intro hero, publications |
| Collection Event detail | 257 | Mostly | Ontology trees |
| Subpopulation detail | 266 | Mostly | Ontology trees |
| Variable detail (resource) | 212 | Partially | Harmonisation grid (OUT OF SCOPE) |
| Variable detail (catalogue) | 203 | Partially | Harmonisation grid (OUT OF SCOPE) |

#### What already works for detail views (no changes needed):
- **Key/value definition lists** → RecordSection
- **Sidebar TOC with scrollspy** → SideNav (auto-generated from SECTION columns)
- **Conditional sections** → empty detection + showEmpty
- **REF navigation** → auto NuxtLink via buildRefHref
- **REFBACK nested tables** → Emx2ListView (paginated, searchable, clickable rows)
- **REF_ARRAY nested tables** → Emx2ListView (same as REFBACK)
- **Ontology tooltips** → CustomTooltip with definition
- **Hide empty columns** → filterNonEmptyColumns
- **Row click → detail page** → RecordTable with schemaId/tableId

#### Gaps for detail views (need new displayComponent or feature):

| Gap | Catalogue Components | Complexity | Priority |
|-----|---------------------|------------|----------|
| **Ontology trees** | ContentOntology + TreeNode (163 lines) | Medium | HIGH — used in 4/5 detail pages |
| **Card grids** | OrganisationCard, ContactCard, ReferenceCard (~180 lines) | Medium | MEDIUM — organisations, networks, contributors |
| **File downloads** | ContentBlockAttachedFiles (in tailwind-components already) | Low | LOW — just wire up via displayComponent |
| **Intro hero** | ContentBlockIntro (247 lines: logo, website, contact modal) | High | LOW — cosmetic, can add later |
| **Publications list** | ContentBlockPublications (custom link list) | Low | LOW — could be a simple displayComponent |
| **Side modal preview** | SideModal + *Display components (~480 lines) | High | SKIP — direct navigation via row click replaces this |

#### LIST VIEWS — Catalogue has 2 list pages (+ 2 landing pages, OUT OF SCOPE)

| Catalogue Page | Can Replace? | Gaps |
|----------------|-------------|------|
| Collections/Networks browse | No — needs filter sidebar, cards, view modes | Full list page framework needed |
| Variables browse | No — needs filter sidebar, variable cards | Full list page framework + harmonisation (OUT OF SCOPE) |

#### What we have for list views:
- **Emx2ListView** — paginated table with search (works for nested tables)
- **ListView** — table/cards/list layouts
- **RecordTable** — read-only table with clickable rows

#### What's missing for list views:
| Gap | Catalogue Components | Complexity | Priority |
|-----|---------------------|------------|----------|
| **Filter sidebar** | FilterSidebar + FilterContainer + FilterOntology + FilterList (~400 lines) | HIGH | HIGH — core list page feature |
| **Search page layout** | SearchPage.vue (sidebar + main two-column) | Low | HIGH — needed as container |
| **Filter well** | FilterWell (active filter chips with remove) | Medium | MEDIUM |
| **Result cards** | ResourceCard, VariableCard (~200 lines) | Medium | MEDIUM — or use generic card |
| **View mode switching** | SearchResultsViewTabs (detailed/compact) | Low | LOW |
| **Result count** | SearchResultsCount | Trivial | LOW |
| **URL-based filter state** | Query parameter serialization | Medium | HIGH — expected UX |

### Recommended Phases

### Phase 5: Ontology Tree Display Component — DONE
Highest-impact gap — used in 4/5 catalogue detail pages.
- [x] OntologyTreeDisplay.vue — smart component, auto-detects flat list vs hierarchical tree
- [x] OntologyTreeNode.vue — recursive collapsible tree node with definition tooltips
- [x] buildOntologyTree.ts utility with 14 vitest tests
- [x] RecordColumn: direct ONTOLOGY/ONTOLOGY_ARRAY columnType detection (not via displayMap)
- [x] fetchTableData: 4-level parent chain for ontology GQL queries
- [x] Story file

### Phase 6: Card Display Components
For organisations, networks, contributors sections.
- [ ] CardGridDisplay — renders REF_ARRAY/REFBACK as card grid instead of table
- [ ] Generic card template (name, description, link, optional image)
- [ ] Customizable via slot or card component prop in IListConfig
- [ ] Story file

### Phase 7: Upgrade [entity].vue (apps/ui) — DONE
- [x] apps/ui [entity].vue already imports Emx2RecordView directly
- [x] Edit/delete kept via modal overlay in the page itself

### Phase 7b: HEADING columns in catalogue data models — DEFERRED (separate PR)
Prerequisite for catalogue demo — without HEADINGs, detail pages render as flat lists without sidebar navigation.
- [ ] Add HEADING columns to Collection Events model CSV
- [ ] Add HEADING columns to Resources model CSV
- [ ] Add HEADING columns to Subpopulations model CSV
- [ ] Add HEADING columns to Datasets model CSV
- [ ] Add HEADING columns to Variables model CSV
- [ ] Group fields into logical sections matching catalogue's current detail page layout

### Phase 7c: DATA_NESTED table type (backend + frontend) — DEFERRED (separate PR)
See catalogue-detail-views.md Phase 7 for full details.
- [ ] Add DATA_NESTED to TableType.java (backend)
- [ ] Update switch statements and DATA checks in RDF + import/export
- [ ] Update TypeScript type in frontend
- [ ] Filter DATA_NESTED from main table list in apps/ui

### Phase 8: Catalogue Detail Pages Demo — DEFERRED (separate PR)
**Prerequisite:** HEADING columns must be added to catalogue data model CSVs (Phase 7b) and DATA_NESTED implemented (Phase 7c).
At this point we can replace collection event, subpopulation, and most of the resource detail page.
- [ ] Collection event detail → Emx2RecordView + ontology tree displayMap
- [ ] Subpopulation detail → Emx2RecordView + ontology tree displayMap
- [ ] Resource detail → Emx2RecordView + displayMap (ontology tree, card grid)
- [ ] Visual comparison with handcrafted pages
- [ ] File download — wire up existing ContentBlockAttachedFiles
- [ ] Publications — simple displayComponent or just use default rendering

### Phase 9: Generic List Page Framework — DEFERRED (separate PR)
Build the equivalent of catalogue's SearchPage for any EMX2 table.
- [ ] Emx2SearchPage (smart) — schema/table driven, auto-generates filters from metadata
- [ ] SearchPageLayout — two-column layout (filter sidebar + results)
- [ ] Filter components — auto-generate from column types:
  - STRING/TEXT → search input
  - ONTOLOGY/ONTOLOGY_ARRAY → tree filter
  - REF/REF_ARRAY → list filter
  - BOOL → checkbox
  - INT/DECIMAL/DATE → range filter
- [ ] FilterWell — active filter chips
- [ ] URL-based filter state (query params)
- [ ] Result cards — generic card component (configurable via IListConfig)
- [ ] Pagination, result count, view mode switching

### Phase 10: Catalogue List Pages Demo — DEFERRED (separate PR)
- [ ] Collections/Networks browse → Emx2SearchPage with config
- [ ] Compare with handcrafted browse pages
- [ ] Theme + responsive testing

### NOT IN SCOPE
- Landing pages with CTA cards and stats
- Variable harmonisation matrix
- Shopping cart integration
- Contact modal / side modal previews (direct navigation replaces these)

## Files

### Done (Phases 1-5)
- `types/types.ts` — IColumnDisplay, IListConfig, IRecordViewConfig
- `display/RecordView.vue` — dumb, IColumnDisplay[], DetailPageLayout + SideNav
- `display/RecordSection.vue` — catalogue styling, schemaId/parentRowId passthrough
- `display/RecordColumn.vue` — displayComponent, Emx2ListView for REFBACK + REF_ARRAY, NuxtLink for REF, OntologyTreeDisplay for ONTOLOGY/ONTOLOGY_ARRAY
- `display/RecordTable.vue` — clickable rows with schemaId/tableId navigation
- `display/Emx2RecordView.vue` — smart wrapper, auto listConfig for REFBACK + REF_ARRAY
- `display/ListView.vue` — dumb list/table/cards, filters empty/hidden columns
- `display/Emx2ListView.vue` — smart list with useTableData, search, pagination
- `display/InlinePagination.vue` — prev/next
- `display/OntologyTreeDisplay.vue` — auto-detects flat list vs hierarchical tree
- `display/OntologyTreeNode.vue` — recursive collapsible tree node with definition tooltips
- `layout/DetailPageLayout.vue` — sidebar + main layout
- `SideNav.vue` — scrollspy navigation with title
- `value/Object.vue` — plain text + definition tooltip
- `composables/fetchTableData.ts` — ontology definition + 4-level parent chain in expansion
- `composables/fetchTableMetadata.ts` — with includeSubclassColumns
- `composables/getSubclassColumns.ts` — inheritance tree column collector
- `utils/displayUtils.ts` — buildRefHref, isRefColumn, isRefArrayColumn, etc.
- `utils/buildOntologyTree.ts` — builds tree structure from flat ontology rows (14 vitest tests)

### Future
- `display/CardGridDisplay.vue` — card layout for refs
- `display/Emx2SearchPage.vue` — smart list page with filters
- `layout/SearchPageLayout.vue` — filter sidebar + results
- `filter/*.vue` — auto-generated filter components
