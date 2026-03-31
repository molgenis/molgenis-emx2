# Detail View Like Catalogue — Plan

## Goal
Replace handcrafted catalogue detail pages (~1,950 lines across 5 pages) with a config-driven system using generic building blocks. Same visual quality, 90% less page-specific code.

## Completed

### Phase 1-2: Foundation + RecordView — DONE
RecordView/RecordSection/RecordColumn dumb components. Emx2RecordView smart wrapper. SideNav scrollspy. Catalogue styling. Empty section hiding.

### Phase 3: ListView + Emx2ListView — DONE
Table/cards/list layout modes. Paginated search. REFBACK via Emx2ListView. RecordTable clickable title column. Ontology tooltips. Hide empty columns.

### Phase 4: REF_ARRAY Display + Row Navigation — DONE
REF_ARRAY as Emx2ListView. REF/SELECT/RADIO auto-link. Row click to detail.

### Phase 5: Ontology Tree Display — DONE
OntologyTreeDisplay auto-detects flat vs hierarchical. buildOntologyTree with 14 tests.

### Phase 6: Column Display Properties — DONE
Backend: ColumnRole enum (TITLE/SUBTITLE/DESCRIPTION/LOGO/DETAIL/INTERNAL), DisplayType enum (TABLE/CARDS/LIST), migration32, GraphQL + CSV import/export. Frontend: ListCard with role annotations, displayUtils (16+ tests). Data: HEADING columns + role/display annotations in Resources, Collection Events, Subpopulations.

### Phase 7: Table Role — DONE
TableRole enum (MAIN/DETAIL). DETAIL tables hidden from landing page.

### Phase 9: Polish & Small Fixes — DONE
Logo on cards + sidebar. Comma spacing. List alignment. Double scrollbar fix. ref_array items as links. Title-column-only click in tables. Date/DateTime display. ValueFile as download link. Sidebar [object object] fix. Delete button removed. Subpopulations creator=cards. Datasets variables refback.

### Phase 10: Refactor — getListColumns + IListConfig cleanup — DONE
Single `getListColumns` pure function replaces duplicated filtering in Emx2ListView, ListView, RecordColumn. Removed `layout`/`visibleColumns`/`hideColumns`/`showFilters` from IListConfig — column metadata (`display`, `role`) is single source of truth. IListConfig now only has UI concerns (pageSize, component, rowLabel).

### Phase 10b: Type cleanup — IN PROGRESS
- [x] Emx2RecordView.rowId: `Record<string, any>` → `IRow`
- [x] ContentTypeOntologyArray: removed `Item` interface + double-cast, uses `columnValueObject[]`
- [x] ContentTypeRefBack: `Object[]` → `columnValueObject[]`, uses `isEmptyValue` from displayUtils

## Phase 11: Rename components + stories

### Naming convention
Nuxt auto-registers components as `Display{Name}` from `display/` folder. Current names are inconsistent and collide with pre-existing components.

**Pre-existing components (NOT ours, don't rename):**
- `display/Record.vue` — older detail view (takes tableMetadata + inputRowData)
- `display/List.vue` — simple `<ul>` wrapper
- `display/ListItem.vue` — simple `<li>` wrapper
- `display/CodeBlock.vue` — code display
- `table/TableEMX2.vue` — table explorer (editable, sortable)

**Dumb components (no backend calls, pure rendering):**

| Current | New | Nuxt auto-name | Why |
|---|---|---|---|
| `RecordView.vue` | `DetailView.vue` | `DisplayDetailView` | Main detail page layout with sections + sidebar |
| `RecordSection.vue` | `DetailSection.vue` | `DisplayDetailSection` | Section within detail view (heading + columns). Passes schemaId/parentRowId through but doesn't fetch — stays dumb |
| `RecordColumn.vue` | → split, see below | | Currently smart (fetches metadata). Needs splitting |
| `RecordTable.vue` | `DataTable.vue` | `DisplayDataTable` | Read-only table with clickable title column |
| `ListView.vue` | `DataList.vue` | `DisplayDataList` | Renders rows as TABLE/CARDS/bullets via layout prop |
| `ListCard.vue` | `DataCard.vue` | `DisplayDataCard` | Single card using role annotations |

**Smart components (fetch data from backend):**

| Current | New | Why |
|---|---|---|
| `Emx2RecordView.vue` | `Emx2DetailView.vue` | Smart wrapper for DetailView |
| `Emx2ListView.vue` | `Emx2DataList.vue` | Smart wrapper for DataList |

**RecordColumn → Emx2DetailColumn (no split):**
RecordColumn is smart — fetches ref table metadata, resolves primary keys, builds filters. The "dumb" leaf rendering is already the sub-components (ValueEMX2, DataTable, DataCard, OntologyTreeDisplay). No benefit to splitting; just rename to acknowledge it's an Emx2 component.

### Tasks
- [x] Rename all display components
- [ ] Clean up DataList: make truly dumb, move pagination/schema/getHref to Emx2DataList
- [ ] Explicit layout types: TABLE, CARDS, LIST, LINKS (remove v-else fallback + component prop)
- [ ] Update DisplayType enum backend to include LINKS
- [ ] Story: DataTable — table with clickable title, various column types
- [ ] Story: DataCard — card with role annotations (logo, title, description, detail)
- [ ] Story: DataList — switch between TABLE/CARDS/LIST/LINKS layouts
- [ ] Story: DetailView — full detail page with sections, sidebar, nested lists
- [ ] Deprecate old `Record.vue` (add note, don't delete yet)

#### DataList layout types
| Layout | Component | Description |
|---|---|---|
| TABLE | DataTable | Tabular rows with clickable title column |
| CARDS | DataCard grid | Card grid (2 col desktop, 1 col mobile) |
| LIST | DataCard list | Cards in single column |
| LINKS | inline bullets | Simple name-only bullet links |

#### DataList prop cleanup (make truly dumb)
DataList props: `rows`, `columns`, `layout`, `rowLabel`
Move to Emx2DataList: `schemaId`, `tableId`, `getHref`, `pagination`, `search`, `component`

## Open Items (this branch)

### Visual Testing (manual)
- [ ] Verify cards, logo, dates, links, variables refback with running app
- [ ] Theme testing (Light, Dark, Molgenis, UMCG, AUMC)
- [ ] Responsive testing (desktop, tablet, mobile)

### Minor Gaps
- [ ] Backend JUnit test for column role/display persistence
- [ ] HEADING columns needed in more catalogue data models (Datasets, Variables, Organisations, Contacts, Networks)

## Deferred (separate PRs)

### DATA_NESTED table type
Currently using TableRole=DETAIL (lighter touch, same UX). DATA_NESTED enum only needed if DETAIL tables require different DB/API behavior.

### Catalogue Detail Pages Demo
Wire Emx2RecordView for catalogue's 5 detail pages. Requires HEADING columns in all catalogue data model CSVs.

### Generic List/Search Page Framework
Emx2SearchPage with auto-generated filters, filter sidebar, URL-based filter state, result cards.

### Out of Scope
Landing pages, variable harmonisation, shopping cart, contact modals.
