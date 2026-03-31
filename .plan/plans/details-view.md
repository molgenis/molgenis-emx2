# Detail View Like Catalogue — Plan

## Goal
Replace handcrafted catalogue detail pages (~1,950 lines across 5 pages) with a config-driven system using generic building blocks. Same visual quality, 90% less page-specific code.

## Completed

### Phase 1-7: Core Implementation — DONE
Foundation components, ListView, REF_ARRAY display, ontology trees, column display properties (ColumnRole, DisplayType), TableRole (MAIN/DETAIL), HEADING sections in data models.

### Phase 9: Polish & Small Fixes — DONE
Logo, dates, links, scrollbar, alignment, comma spacing, delete button, data model annotations.

### Phase 10: Refactor — DONE
getListColumns consolidation, IListConfig stripped to UI-only, type cleanup (IRow, columnValueObject).

### Phase 11: Rename + DataList cleanup — DONE
Renamed all display components for clarity. DataList made dumb with 4 explicit layouts (TABLE/CARDS/LIST/LINKS). Pagination moved to Emx2DataList.

**Current component tree:**
```
display/
  DetailView.vue          — layout: sections + sidebar (needs schema context)
  DetailSection.vue       — one section: heading + columns (passes schema context)
  Emx2DetailColumn.vue    — one column value (fetches ref metadata, smart)
  DataList.vue            — renders rows as TABLE/CARDS/LIST/LINKS (dumb-ish, takes getHref)
  DataTable.vue           — read-only table, clickable title column (dumb)
  DataCard.vue            — single card with role annotations (dumb)
  Emx2DetailView.vue      — fetches metadata + row, delegates to DetailView
  Emx2DataList.vue        — fetches paginated data, delegates to DataList
  OntologyTreeDisplay.vue — auto flat/hierarchical ontology
  InlinePagination.vue    — prev/next
  SideNav.vue             — scrollspy sidebar
```

## Phase 12: Merge Emx2 wrappers — simplify component tree

### Problem
The Emx2* prefix was meant to separate "smart" (fetches data) from "dumb" (pure rendering). But the "dumb" components (DetailView, DetailSection) need schema context anyway for navigation, so the split is artificial. Nobody uses DetailView without Emx2DetailView.

### Solution
Merge smart wrappers into their corresponding components. Drop the Emx2 prefix. Components that fetch data just... fetch data. No pretense of a dumb/smart split where the split doesn't exist.

### Merges

| Merge into | Absorbs | What moves |
|---|---|---|
| `DetailView.vue` | `Emx2DetailView.vue` | Data fetching (fetchTableMetadata, fetchRowData, useAsyncData), column processing (filter INTERNAL/mg_, resolve displayComponent from tags) |
| `DetailColumn.vue` | `Emx2DetailColumn.vue` | Just rename — it was already smart, prefix was acknowledging that |
| `DataList.vue` | `Emx2DataList.vue` | Data fetching (useTableData), pagination (InlinePagination), search (InputSearch), getHref building |

### Result: 3 fewer files

```
display/
  DetailView.vue       — fetches metadata + row, renders sections + sidebar
  DetailSection.vue    — one section: heading + columns
  DetailColumn.vue     — one column value, fetches ref metadata when needed
  DataList.vue         — fetches paginated data, renders as TABLE/CARDS/LIST/LINKS
  DataTable.vue        — truly dumb: renders rows as table
  DataCard.vue         — truly dumb: renders one card
  OntologyTreeDisplay.vue
  InlinePagination.vue
  SideNav.vue
```

Only DataTable and DataCard are truly dumb (no fetching, no schema awareness). Everything else is part of the EMX2 display system — and that's fine.

### Tasks
- [ ] Merge Emx2DetailView into DetailView (move fetching + column processing)
- [ ] Merge Emx2DataList into DataList (move pagination, search, data fetching)
- [ ] Rename Emx2DetailColumn → DetailColumn
- [ ] Delete Emx2DetailView.vue, Emx2DataList.vue, Emx2DetailColumn.vue
- [ ] Update all imports (apps/ui, stories, tests, internal cross-refs)
- [ ] Update story files
- [ ] Run tests
- [ ] Story: DataTable — table with various column types
- [ ] Story: DataCard — card with role annotations
- [ ] Story: DataList — switch between TABLE/CARDS/LIST/LINKS
- [ ] Story: DetailView — full detail page with sections, sidebar, nested lists

### Consumer API after merge

```vue
<!-- apps/ui entity page — same as before, just no Emx2 prefix -->
<DetailView
  :schema-id="schemaId"
  :table-id="tableId"
  :row-id="rowId"
>
  <template #header>
    <PageHeader :title="entityId" />
    <Button v-if="canEdit" @click="showEditModal = true">Edit</Button>
  </template>
</DetailView>

<!-- Nested data list (used internally by DetailColumn for REFBACK/REF_ARRAY) -->
<DataList
  :schema-id="schemaId"
  :table-id="tableId"
  :filter="refbackFilter"
  :column="column"
/>

<!-- Truly dumb — usable anywhere -->
<DataTable :columns="columns" :rows="rows" />
<DataCard :title="name" :data="row" :columns="columns" />
```

## Open Items

### Visual Testing (manual)
- [ ] Verify cards, logo, dates, links, variables refback with running app
- [ ] Theme testing (Light, Dark, Molgenis, UMCG, AUMC)
- [ ] Responsive testing (desktop, tablet, mobile)

### Minor Gaps
- [ ] Backend JUnit test for column role/display persistence
- [ ] HEADING columns needed in more catalogue data models

## Deferred (separate PRs)
- DATA_NESTED table type (TableRole=DETAIL covers it for now)
- Catalogue detail pages demo (needs HEADINGs in all models)
- Generic list/search page framework
- Landing pages, harmonisation, shopping cart — out of scope
