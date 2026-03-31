# Detail View Like Catalogue — Consolidated Plan

## Goal
Replace handcrafted catalogue detail pages (~1,950 lines across 5 pages) with a config-driven system using generic building blocks. Same visual quality, 90% less page-specific code.

Two consumers:
1. **apps/ui** — generic detail view for any EMX2 table
2. **apps/catalogue** — rich, customized views (future)

## Architecture

### Smart/Dumb Split
- **RecordView** (dumb) — takes `columns: IColumnDisplay[]` + `data`, renders sections/sidebar
- **Emx2RecordView** (smart) — fetches metadata + row, merges into IColumnDisplay[], passes to RecordView
- **ListView** (dumb) — takes `rows` + `columns`, renders as table/cards/list
- **Emx2ListView** (smart) — fetches paginated data, delegates to ListView
- **ListCard** — card component using role annotations (TITLE/DESCRIPTION/LOGO/DETAIL)

### Column Display Properties (first-class on Column)
- `role: ColumnRole` — TITLE, SUBTITLE, DESCRIPTION, LOGO, DETAIL, INTERNAL
- `display: DisplayType` — TABLE (default), CARDS, LIST (on REF_ARRAY/REFBACK)
- HEADING columns create sections with sidebar navigation

### Table Role
- `TableRole` — MAIN (default, shown on landing) or DETAIL (hidden, shown inline)

## Completed Phases

### Phase 1-2: Foundation + RecordView — DONE
- [x] IColumnDisplay, IListConfig, IRecordViewConfig types
- [x] RecordView/RecordSection/RecordColumn dumb components
- [x] Emx2RecordView smart wrapper with displayMap + columnConfig
- [x] SideNav scrollspy + DetailPageLayout
- [x] Catalogue styling (rounded sections, headings, spacing)
- [x] Empty section hiding, mg_top_of_form handling

### Phase 3: ListView + Emx2ListView — DONE
- [x] Table/cards/list layout modes
- [x] Paginated search via useTableData
- [x] REFBACK columns render via Emx2ListView with auto-built filter
- [x] RecordTable with clickable rows (schemaId/tableId navigation)
- [x] Ontology tooltips (CustomTooltip with definition)
- [x] Hide columns where all rows empty (filterNonEmptyColumns)

### Phase 4: REF_ARRAY Display + Row Navigation — DONE
- [x] REF_ARRAY renders as Emx2ListView (same as REFBACK)
- [x] REF/SELECT/RADIO values auto-link via buildRefHref
- [x] Row click navigates to detail page
- [x] Tests for isRefArrayColumn, isRefColumn

### Phase 5: Ontology Tree Display — DONE
- [x] OntologyTreeDisplay auto-detects flat vs hierarchical
- [x] OntologyTreeNode recursive collapsible tree with definition tooltips
- [x] buildOntologyTree.ts utility with 14 vitest tests
- [x] fetchTableData includes 4-level parent chain for ontology queries

### Phase 6: Column Display Properties — DONE
- [x] Backend: ColumnRole enum, DisplayType enum, migration32.sql
- [x] Backend: Column.java role/display fields, MetadataUtils persistence
- [x] Backend: GraphQL + CSV import/export for role/display
- [x] Frontend: IColumn.role/display, GraphQL metadata query
- [x] Frontend: ListCard renders using role annotations
- [x] Frontend: displayUtils pure functions (16+ vitest tests)
- [x] Data: HEADING columns in Collection Events, Subpopulations, Resources CSVs
- [x] Data: role annotations on Resources columns (title, subtitle, description, logo, detail)
- [x] Data: display=cards on organisations, people, networks refbacks
- [ ] Backend: JUnit test for column role/display persistence

### Phase 7: Table Role — DONE
- [x] TableRole enum (MAIN/DETAIL)
- [x] TableMetadata.role field with CSV import/export
- [x] Frontend: filter DETAIL tables from schema landing page
- [x] Data: CollectionEvents, Subpopulations, Datasets marked as DETAIL

### Phase 9: Polish & Small Fixes — DONE
- [x] 9a: Logo in ListCard + RecordView sidebar (getLogoColumn → image)
- [x] 9b: Empty columns already filtered in ListView
- [x] 9c: Comma spacing in ontology lists (`,&nbsp;` → `, `)
- [x] 9d: List view left alignment (ml-0 on dd)
- [x] 9e: Double scrollbar in TableEMX2 (removed duplicate overflow-auto)
- [x] 9f: ref_array items as links (getHref on inline ListView)
- [x] 9g: [object object] in sidebar (getRoleText for object key values)
- [x] 9h: Removed delete button, kept edit only
- [x] 9i: Subpopulations creator display=cards
- [x] 9j: DATE/DATETIME display (missing cases in EMX2.vue)
- [x] 9k: Datasets refback to Variables
- [x] 9l: RecordTable click on title column only (not whole row)
- [x] 9m: ValueFile renders as clickable link to file URL

## Open Items (this branch)

### Needs Visual Testing
- [ ] Verify card grid renders on resource detail page
- [ ] Verify logo appears in sidebar and on cards
- [ ] Verify date values display correctly
- [ ] Verify ref_array items link to correct detail pages
- [ ] Verify Datasets shows Variables refback
- [ ] Theme testing (Light, Dark, Molgenis, UMCG, AUMC)
- [ ] Responsive testing (desktop, tablet, mobile)

### Known Gaps
- [ ] HEADING columns needed in more catalogue models (Datasets, Variables, Organisations, Contacts, Networks)
- [ ] Backend JUnit test for role/display column persistence

## Deferred (separate PRs)

### DATA_NESTED table type (backend enum)
- Add DATA_NESTED to TableType.java (treat like DATA except hidden from landing)
- Update RDF switch statements, DATA checks, frontend type
- Currently using TableRole=DETAIL instead (lighter touch, same UX effect)

### Catalogue Detail Pages Demo
- Wire Emx2RecordView for catalogue's 5 detail pages
- Requires HEADING columns in all catalogue data model CSVs
- Visual comparison with handcrafted pages

### Generic List/Search Page Framework
- Emx2SearchPage with auto-generated filters from metadata
- Filter sidebar, filter well, URL-based filter state
- Result cards, view mode switching

### Out of Scope
- Landing pages with CTA cards and stats
- Variable harmonisation matrix
- Shopping cart integration
- Contact modal / side modal previews
