# Detail View Like Catalogue — Plan

## Goal
Replace handcrafted catalogue detail pages (~1,950 lines across 5 pages) with a config-driven system using generic building blocks. Same visual quality, 90% less page-specific code.

## Completed

### Phase 1-7: Core Implementation — DONE
Foundation components, ListView, REF_ARRAY display, ontology trees, column display properties (ColumnRole, DisplayType), TableRole (MAIN/DETAIL), HEADING sections in data models.

### Phase 9: Polish & Small Fixes — DONE
Logo, dates, links, scrollbar, alignment, comma spacing, delete button, data model annotations.

### Phase 10: Refactor — DONE
getListColumns consolidation, type cleanup (IRow, columnValueObject).

### Phase 11: Rename + DataList cleanup — DONE
Renamed all display components for clarity. DataList made dumb with 4 explicit layouts (TABLE/CARDS/LIST/LINKS). Pagination moved to Emx2DataList.

### Phase 12: Merge Emx2 wrappers + simplify — DONE

Merged smart wrappers into their base components. Extracted collection renderers. Unified navigation. Removed unused types.

**What was done:**
- Merged Emx2DetailView → DetailView (smart/dumb dual API via schemaId+tableId+rowId OR columns+data)
- Merged Emx2DataList → DataList (smart/dumb dual API, explicit props: layout, pageSize, hideColumns, rowLabelTemplate)
- Renamed Emx2DetailColumn → DetailColumn
- Extracted DataCards (from DataCard) and DataLinks for consistent collection rendering
- Replaced config/displayMap with `columnTransform` function on DetailView
- Created `useRecordNavigation` composable (provide/inject) — unified click handling, fixed broken navigation in DataCards/DataLinks (missing ?keys= param)
- Removed IColumnDisplay, IListConfig, IRecordViewConfig — all unused by real consumers, use plain IColumn everywhere
- Guard useTableData against empty schemaId/tableId
- Created stories for DataList, DataCards, DetailColumn
- Simplified DetailPageLayout story to dynamic-only

**Current component tree:**
```
display/
  DetailView.vue          — fetches metadata + row OR renders provided data, sections + sidebar
  DetailSection.vue       — one section: heading + columns
  DetailColumn.vue        — one column value, fetches ref metadata when needed
  DataList.vue            — layout switcher (smart/dumb), search + pagination in smart mode
  DataTable.vue           — renders rows as table (dumb)
  DataCards.vue           — renders rows as card grid, 1 or 2 columns (dumb)
  DataLinks.vue           — renders rows as link list (dumb)
  OntologyTreeDisplay.vue — auto flat/hierarchical ontology
  InlinePagination.vue    — prev/next
  SideNav.vue             — scrollspy sidebar

composables/
  useRecordNavigation.ts  — provide/inject for record click handling
  useTableData.ts         — paginated data fetching
  fetchTableMetadata.ts   — metadata fetching
  fetchRowData.ts         — single row fetching
```

**Consumer API:**
```vue
<!-- Smart mode — fetches everything -->
<DetailView :schema-id="s" :table-id="t" :row-id="r">
  <template #header>...</template>
</DetailView>

<!-- With column customization -->
<DetailView :schema-id="s" :table-id="t" :row-id="r"
  :column-transform="(cols) => cols.filter(c => mySet.has(c.id))"
  :show-empty="true" :show-side-nav="false"
/>

<!-- Dumb mode — provide data directly -->
<DetailView :columns="cols" :data="row" />

<!-- Data list — smart mode -->
<DataList :schema-id="s" :table-id="t" layout="CARDS" :page-size="20" />

<!-- Data list — dumb mode -->
<DataList :rows="rows" :columns="cols" layout="TABLE" />

<!-- Dumb collection renderers -->
<DataTable :columns="cols" :rows="rows" :schema-id="s" :table-id="t" />
<DataCards :rows="rows" :columns="cols" :grid-columns="2" />
<DataLinks :rows="rows" :schema-id="s" :table-id="t" />

<!-- Override navigation behavior -->
provideRecordNavigation({ navigateToRecord: myHandler })
```

## Status: DONE

All phases complete. Stories sufficient. Visual testing sufficient for now.

### Decisions
- Backend JUnit test for column role/display persistence — wontfix (metadata persistence covered by existing integration tests)
- HEADING columns in more catalogue data models — future work, separate PR

## Deferred (separate PRs)
- HEADING columns in more catalogue data models
- DATA_NESTED table type (TableRole=DETAIL covers it for now)
- Catalogue detail pages demo (needs HEADINGs in all models)
- Generic list/search page framework
- Landing pages, harmonisation, shopping cart — out of scope
