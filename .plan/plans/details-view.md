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

## Status: DONE — full review 2026-07-05 found open issues, see `.plan/notes/review-2026-07-05.md`

All phases complete. Stories sufficient. Visual testing sufficient for now.

### Review 2026-07-05 (vs origin/master, pre-merge)
- 4 frontend CRITICALs: DataList search TABLE-only (spec: all layouts), LOGO not excluded in getListColumns, MULTISELECT/CHECKBOX layout mismatch in DetailSection, useId() in template (ui [entity].vue:133)
- Backend: json/Table.setRole case-sensitive; TableRole roundtrip untested
- Docs: use_schema.md "Datasets"→"Tables"; tableRole documented but unused in model CSVs
- Spec drift on nested-datalist-roles.md: search-all-layouts, LOGO exclusion, TITLE-counts-as-1 cap unimplemented

### Decisions
- Backend JUnit test for column role/display persistence — wontfix (metadata persistence covered by existing integration tests)
- HEADING columns in more catalogue data models — future work, separate PR

## Phase 13: Review cleanup (2026-07-08) — PLANNED, awaiting go
Six owner-approved items from branch review (rationale in `.plan/decisions.md` 2026-07-08).

| # | Task | Files (primary) | Test |
|---|------|-----------------|------|
| 13a | `nestedLimit` → `DEFAULT_NESTED_LIMIT=5` constant; drop prop plumbing + 3 literals | fetchTableData.ts, fetchRowData.ts, IQueryMetaData.ts, DetailView.vue, 3 catalogue pages | fetchTableData.spec.ts (assert limit arg from constant; uncapped path unchanged) |
| 13b | `sourceTableId` → `tableId` (rename only — `table?`/`inherited?` KEPT: live readers in Picker.vue:167 + apps/schema) | metadata-utils/types.ts, getSubclassColumns.ts, Ref.story.vue | getSubclassColumns.spec.ts (rename asserts) |
| 13c | Add `ontologyValueObject` (+ isOntologyValueObject/Array guards) in metadata-utils; use in ContentTypeOntologyArray/RefBack; point IOntologyItem/IOntologyNode at it where cheap | metadata-utils/types.ts, content/type/*.vue | types guard test if present; typecheck |
| 13d | `DataCards.gridColumns` → `maxColumns`; update DataList LIST/CARDS dispatch | DataCards.vue, DataList.vue, DataCards.story.vue | DataCards.story checklist; DataList behavior |
| 13e | ~~Make DataCards dumb (split props)~~ → REVERSED 2026-07-20 (decisions.md): DataCards keeps `columns`+`rows` API (like DataTable), calls pure `classifyCardColumns(columns)` internally; per-row `isEmptyValue` skip. Kept: classifyCardColumns + 13 tests. | DataCards.vue, DataList.vue, displayUtils.ts | `displayUtils.spec.ts` classifyCardColumns (13) |
| 13f | Consistency check: DataTable/DataLinks stay dumb; optional pure-helper extraction of DetailView header selection (getDetailViewColumns) for testability | DetailView.vue (opt) | displayUtils.spec.ts |

**Decision (2): keep `display` + `role` as two metadata columns — orthogonal, co-occur (role=DETAIL + display=CARDS). No code change.**

Ordering: 13a/13b/13c independent (parallel). 13d before 13e (rename first, then behavior). 13e depends on 13d. 13f last (verification + optional extract). Backend untouched (all frontend).

## Deferred (separate PRs)
- HEADING columns in more catalogue data models
- DATA_NESTED table type (TableRole=DETAIL covers it for now)
- Catalogue detail pages demo (needs HEADINGs in all models)
- Generic list/search page framework
- Landing pages, harmonisation, shopping cart — out of scope
- **Phase 13 follow-ups (from 2026-07-08 review):**
  - LOGO in cards via DataList: `getListColumns` strips `role=LOGO`, so `classifyCardColumns` (fed the filtered set by DataList) never finds a logo → card logos don't render via the smart path. Pre-existing (old code had the same dead path); fix = classify logo from raw metadata columns, needs visual verification. Not a regression.
  - Alias catalogue `IOntologyItem` (+ `IOntologyNode` in cms.ts) to the new `ontologyValueObject` — deferred (20+ cross-app refs; cms.ts is generated + has extra fields).
