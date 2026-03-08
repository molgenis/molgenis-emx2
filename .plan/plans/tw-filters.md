# Plan: feat/tw-filters — Port Filter System to tailwind-components + TableEMX2

## Status: REVIEW FIXES DONE — CI green

## Phase 7: Review Fixes [x]

### 7.1 Remove fake/tautological tests [x]
- [x] `FilterPicker.spec.ts` — "sorts columns alphabetically": hardcoded expected order
- [x] `useFilters.spec.ts` — "handle empty objects": fixed to expect `""`
- [x] `useFilters.spec.ts` — "deeply nested objects": exact expected value
- [x] `FilterPicker.spec.ts` — "checked/unchecked checkboxes": specific column IDs

### 7.2 Remove unnecessary mocks [x]
- [x] `useFilterCounts.spec.ts` — removed dead `#imports` and `#app/composables/router` mocks
- [x] `FilterPicker.spec.ts` — simplified `vDropdownStub` to click-toggle

### 7.3 Fix copy-pasted test function [x]
- [x] Extracted `computeDefaultFilters` to `app/utils/computeDefaultFilters.ts`
- [x] `FilterPicker.spec.ts` imports real function, local copy removed
- [x] `Sidebar.vue` imports from utility

### 7.4 Add missing tests [x]
- [x] Create `getSubclassColumns.spec.ts` — 7 tests (empty, not found, direct, recursive, dedup, sourceTableId, empty array)
- [x] `FilterPicker.spec.ts` — FILE column exclusion test
- [x] `Column.spec.ts` — 4 removable/remove tests
- [x] `buildFilter.spec.ts` — notNull, isNull, nested notNull tests
- [x] `useFilterCounts.spec.ts` — SELECT/RADIO countable types test
- [x] Create `Sidebar.spec.ts` — 31 tests (rendering, smart defaults, toggle, reset, URL sync, nested filters, mobile, search)

### 7.5 Fix stale story documentation [x]
- [x] `Column.story.vue` — removed `collapsed` prop and collapse/caret items
- [x] `Sidebar.story.vue` — replaced "grouped by heading" with "sorted alphabetically"
- [x] `Range.story.vue` — removed "stacks vertically" mobile claim

### 7.6 Code fixes [x]
- [x] `extractStringKey` — returns `""` for undefined/empty objects instead of `"undefined"`
- [x] FILE exclusion already in `EXCLUDED_TYPES`, test now covers it

### Open questions (need product owner input)
1. Column.vue: Is collapse/expand planned? Story documents it but source doesn't have it => NO. we have flat selection of nested elemnts.
2. FilterPicker: Should columns be grouped by heading? Story documents it but not implemented => NO. Remove from story.
3. Range: Should it stack vertically on mobile? Story documents it but uses flex. NO => leave as is.
4. `like_or`/`like_and` operators: Are they used anywhere? Test or remove? YES: the string inputs should use this (except UUID)
5. `computeDefaultFilters`: Should it be exported from Sidebar.vue for testability? => YES, good idea.
6. `serializeFilterValue({operator:"in", value:{}})` returning `"undefined"` — bug or intentional? => BUG, can you interrogate?

## Phase 1: Create worktree [x]
- [x] Create worktree from master
- [x] Symlink .claude
- [x] Create .plan directories

## Phase 2: Copy new files from feat/generic-view [x]
Source: `/Users/m.a.swertz/git/molgenis-emx2/feat/generic-view/apps/tailwind-components/`

### New source files (12) — all copied
- [x] types/filters.ts
- [x] app/utils/filterConstants.ts
- [x] app/utils/buildFilter.ts
- [x] app/utils/extractPrimaryKey.ts
- [x] app/composables/useFilters.ts
- [x] app/composables/useFilterCounts.ts
- [x] app/composables/getSubclassColumns.ts
- [x] app/components/filter/Range.vue
- [x] app/components/filter/Column.vue
- [x] app/components/filter/FilterPicker.vue
- [x] app/components/filter/ActiveFilters.vue
- [x] app/components/filter/Sidebar.vue

### Story files (5) — all copied
- [x] app/pages/filter/Sidebar.story.vue
- [x] app/pages/filter/ActiveFilters.story.vue
- [x] app/pages/filter/Column.story.vue
- [x] app/pages/filter/Range.story.vue
- [x] app/pages/composables/useFilters.story.vue

### Test files (7) — all copied
- [x] tests/vitest/utils/buildFilter.spec.ts
- [x] tests/vitest/composables/useFilters.spec.ts
- [x] tests/vitest/composables/useFilterCounts.spec.ts
- [x] tests/vitest/components/filter/Range.spec.ts
- [x] tests/vitest/components/filter/Column.spec.ts
- [x] tests/vitest/components/filter/FilterPicker.spec.ts (removed stale grouped-by-type test)
- [x] tests/vitest/components/filter/ActiveFilters.spec.ts

## Phase 3: Modify existing files [x]
- [x] types/types.ts — Added IOntologyItem interface
- [x] app/composables/fetchTableMetadata.ts — Added options param + getSubclassColumns
- [x] app/components/input/CheckboxGroup.vue — Added facetCounts prop + count badge
- [x] app/components/input/Ontology.vue — Added showClear, facetCounts, fetchParentCounts, forceList props
- [x] app/components/input/Ref.vue — Added showClear, facetCounts props, init() guards
- [x] app/components/Input.vue — Added showClear, facetCounts, fetchParentCounts, forceList props, pass-through

## Phase 4: Integrate into TableEMX2 [x]
- [x] Add showFilters + urlSync props (default false)
- [x] Add FilterSidebar two-column layout
- [x] Wire useFilters composable (filterColumns, gqlFilter, searchTerm)
- [x] Pass gqlFilter to fetchTableData
- [x] Watch gqlFilter/searchTerm to reset page + refresh
- [x] ActiveFilters chips above table

## Phase 5: Update TableEMX2 story [x]
- [x] Add showFilters toggle checkbox

## Phase 6: Enable filters in apps/ui [x]
- [x] Add show-filters="true" and url-sync="true" to table page
- [x] Remove search from handleSettingsUpdate (sidebar handles it)

## Verification [x]
- [x] All 327 vitest tests pass (tw-filters)
- [x] All 398 vitest tests pass (generic-view, 1 pre-existing network error unrelated)
- [x] Lint (nuxi typecheck) passes on both branches
- [ ] Story pages render correctly (manual verification needed)
- [ ] Manual test checklist below verified

## Files changed (34 total)
- 24 new files (12 source, 5 stories, 7 tests)
- 1 new type file (filters.ts)
- 1 plan file
- 8 modified files (Input.vue, CheckboxGroup.vue, Ontology.vue, Ref.vue, TableEMX2.vue, fetchTableMetadata.ts, types.ts, EMX2.story.vue, apps/ui index.vue)

---

## Manual Test Checklist

Test using story pages (`pnpm dev` in tailwind-components) and apps/ui with a running backend.
Check all 5 themes: Light, Dark, Molgenis, UMCG, AUMC.

### 1. Filter Sidebar Visibility
Story: `/table/EMX2.story` with showFilters toggle

- [ ] Sidebar appears when showFilters is enabled
- [ ] No sidebar when showFilters is disabled
- [ ] On mobile screens: a filter button appears instead of the sidebar
- [ ] On mobile: filter button is hidden when showFilters is disabled
- [ ] Search bar moves into the sidebar when filters are enabled

### 2. Add Filter Picker
Story: `/filter/Sidebar.story`

- [ ] "Add filter" button is visible in the sidebar footer
- [ ] Clicking it opens a dropdown listing available columns
- [ ] Dropdown is closed by default
- [ ] Filterable columns shown (STRING, INT, REF, ONTOLOGY, BOOL, DATE, etc.)
- [ ] HEADING and SECTION columns are excluded from the list
- [ ] Internal `mg_*` columns are excluded
- [ ] Columns are sorted alphabetically
- [ ] Typing in the search box narrows the column list (case-insensitive)
- [ ] Entering a non-matching search term shows "no results"
- [ ] Non-REF columns show a checkbox only
- [ ] REF columns show a checkbox AND an expand caret for nested columns
- [ ] Already-visible filters show a checked checkbox
- [ ] Hidden filters show an unchecked checkbox
- [ ] Clicking a column toggles the filter on/off in the sidebar
- [ ] "Reset to defaults" button restores the initial default filter set
- [ ] Pressing Escape closes the dropdown
- [ ] Default set: up to 5 ontology columns first, then REF columns

### 3. Nested REF Filters (3+ levels deep)
Story: `/filter/Sidebar.story` with a schema that has multi-level REFs

- [ ] REF columns in the picker show an expand caret
- [ ] Clicking the caret shows the REF table's own columns as sub-items
- [ ] Collapsing a parent also hides all its nested descendants
- [ ] Toggling a deeply nested column adds a filter with the full dot-path (e.g. `orders.product.category`)

### 4. Per-Column Filter Input
Story: `/filter/Column.story`

- [ ] Each filter shows the column label as a heading
- [ ] Custom labels from displayConfig are used when available
- [ ] Filter content is always expanded (no collapse toggle)
- [ ] A "Clear" link appears below the input when a filter value is set
- [ ] "Clear" link is hidden when the filter is empty
- [ ] Clicking "Clear" resets the filter value to empty
- [ ] "Remove" link appears for user-added (non-default) filters
- [ ] Clicking "Remove" hides the filter from the sidebar
- [ ] On mobile, mobile-specific styling classes are applied

### 5. Filter Type Behavior
Story: `/filter/Column.story` — try each column type

- [ ] STRING/TEXT/EMAIL columns render a text input; typing produces a "like" filter
- [ ] INT/DECIMAL/LONG columns render two inputs (From / To); filling produces a "between" range filter
- [ ] DATE/DATETIME columns render two date inputs for range filtering
- [ ] BOOL columns render a single checkbox/radio; selecting produces an "equals" filter
- [ ] REF/REF_ARRAY columns render a reference picker with checkboxes
- [ ] ONTOLOGY columns render a tree picker with checkboxes
- [ ] Clearing both range inputs (From and To) removes the filter entirely

### 6. Range Filter
Story: `/filter/Range.story`

- [ ] Shows "From" and "To" labels by default
- [ ] Changing the min value preserves the existing max value
- [ ] Changing the max value preserves the existing min value
- [ ] Input fields have matching `id`/`for` attributes for accessibility
- [ ] The range is wrapped in a `<fieldset>` for screen readers

### 7. Active Filter Chips
Story: `/filter/ActiveFilters.story`

- [ ] No chips shown when no filters are active
- [ ] A single active filter shows one chip with the column name and value
- [ ] Multiple active filters show multiple chips
- [ ] Chip label for "like" filter: shows the search text (e.g. `Name: test`)
- [ ] Chip label for "equals" filter: shows `= value`
- [ ] Chip label for "in" with 1 item: shows the value directly
- [ ] Chip label for "in" with multiple items: shows count (e.g. `3 selected`)
- [ ] Chip label for "between" with min and max: shows `min – max`
- [ ] Chip label for "between" with min only: shows `>= min`
- [ ] Chip label for "between" with max only: shows `<= max`
- [ ] Chip label for "isNull": shows `is empty`
- [ ] Chip label for "notNull": shows `is not empty`
- [ ] Clicking a chip's X removes that specific filter
- [ ] "Clear all" button appears when 2 or more filters are active
- [ ] "Clear all" is hidden with only 1 active filter
- [ ] Clicking "Clear all" removes all filters at once
- [ ] Hovering a multi-value chip shows a tooltip listing all selected values
- [ ] Chips have ARIA labels for screen reader accessibility

### 8. URL Sync (browser address bar)
Story: `/table/EMX2.story` with showFilters enabled, or apps/ui table page

- [ ] With no filters active, no filter params appear in the URL
- [ ] Setting a filter immediately updates the URL
- [ ] Search text is stored as `mg_search=...` in the URL
- [ ] Filter values appear as `columnName=value` in the URL
- [ ] REF filters use dotted key syntax (e.g. `?orders.name=Widget`)
- [ ] Nested 3-level REF filters serialize correctly (e.g. `?orders.product.name=X`)
- [ ] Existing `mg_*` reserved params (page, orderby) are preserved when filters change
- [ ] Using browser Back/Forward updates the filters reactively
- [ ] Copy-pasting a URL with filter params restores the filters on page load
- [ ] Rapid filter changes don't corrupt the URL

### 9. Facet Counts
Story: `/filter/Sidebar.story` with a backend connection

- [ ] Ontology filter options show a count badge (e.g. "Species (42)")
- [ ] REF filter options show a count badge
- [ ] Counts update when other filters change (cross-filtering)
- [ ] The current column's own filter is excluded from its count calculation
- [ ] Parent ontology nodes show aggregated child counts
- [ ] Search text is included in count calculation
- [ ] No crash when the backend returns empty or error responses

### 10. Ontology Tree Search
Story: `/filter/Sidebar.story` with an ontology column

- [ ] Typing in the ontology search box filters the tree to matching terms
- [ ] When expanding a node whose children are hidden by search, "(show filtered)" link appears
- [ ] A message appears when all children of a node are hidden by the search filter
- [ ] Clicking "(show filtered)" reveals all children regardless of search
- [ ] Clicking "(apply filter)" re-applies the search filter after showing all
- [ ] No "(show filtered)" link when all children already match the search
- [ ] The count of filtered vs total terms is shown correctly
- [ ] Selecting a parent does NOT auto-select when search filter hides some children

### 11. Theme Compatibility
Story: `/filter/Sidebar.story` and `/Form.story`

- [ ] **All 5 themes** (Light, Dark, Molgenis, UMCG, AUMC): sidebar text is readable
- [ ] Filter labels, option labels, and "Clear"/"Remove"/"Add filter" links are visible
- [ ] "N more terms" and "(load more)" links are visible on dark sidebar backgrounds
- [ ] Filter separator lines are visible
- [ ] Ontology tree labels are readable in both the filter sidebar AND the form
- [ ] Bool/Ref checkbox labels are readable in both the filter sidebar AND the form
- [ ] Form section headings (e.g. "STRING TYPES") are readable on AUMC theme

### 12. Integration with TableEMX2
Story: `/table/EMX2.story` with showFilters enabled + backend

- [ ] Table data updates when a filter is applied
- [ ] Table resets to page 1 when a filter changes
- [ ] Active filter chips appear above the table
- [ ] Removing a chip re-fetches data without that filter
- [ ] "Clear all" re-fetches unfiltered data
- [ ] Search in the sidebar filters table data
- [ ] Combined filters + search work correctly together
