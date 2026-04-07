# Filter Sidebar — UI Behavior Spec

Living spec. Updated when tasks complete. Single source of truth for expected behaviors.

## Story URLs (for visual verification)

| Component | Story URL | Live demo URL |
|-----------|-----------|---------------|
| Filter Sidebar | http://localhost:3000/filter/Sidebar | http://localhost:3000/catalogue-demo/Resources |
| Filter Column | http://localhost:3000/filter/Column | - |
| Active Filters | http://localhost:3000/filter/ActiveFilters | - |
| Filter Range | http://localhost:3000/filter/Range | - |
| useFilters | http://localhost:3000/composables/useFilters | - |
| CheckboxGroup | http://localhost:3000/input/CheckboxGroup | - |
| Ontology | http://localhost:3000/input/Ontology | - |
| Ref | http://localhost:3000/input/Ref | - |

## Collapsible Filter Sections

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Each filter column wrapped in collapsible section (chevron toggle) | Sidebar.vue | Sidebar.spec.ts | Check: section headers with filter labels |
| Label-to-options gap is compact (header uses `px-5 pt-4 pb-2`, not `p-5`) | Sidebar.vue | - | Check: tight spacing between filter label and first checkbox |
| Sections collapsed by default (all fullPaths in collapsed Set) | Sidebar.vue | - | Check: all sections closed on page load |
| Click header to expand/collapse | Sidebar.vue | Sidebar.spec.ts (expand tests) | Check: toggle works |
| `<hr>` dividers between sections | Sidebar.vue | - | Check: dividers between sections |
| Column.vue `showLabel` prop hides label when inside collapsible | Column.vue | Column.spec.ts | Check: no duplicate labels |
| Lazy mount: `v-if` on filter content (FilterColumn only mounts on expand) | Sidebar.vue | Sidebar.spec.ts (tests expand before asserting) | Check: no network requests until section opened |
| Sections with URL-hydrated filters start expanded | Sidebar.vue | Sidebar.spec.ts "initial expanded state for URL-hydrated filters" (3 tests) | Check: active-filter sections open on page load |

## Default Filter Visibility

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| All filterable columns shown by default (no max-5 cap) | computeDefaultFilters.ts | computeDefaultFilters.spec.ts | - |
| HEADING, SECTION, FILE, mg_* columns excluded from defaults | computeDefaultFilters.ts | computeDefaultFilters.spec.ts (3 exclusion tests) | - |
| STRING-like free-text columns excluded from defaults (STRING, TEXT, EMAIL, HYPERLINK, UUID, AUTO_ID, JSON, PERIOD and their _ARRAY variants) | computeDefaultFilters.ts | computeDefaultFilters.spec.ts "excludes STRING columns from defaults", "excludes all string-like types" | - |
| String-like columns still available in FilterPicker for manual addition | FilterPicker.vue | - | Check: string columns appear in FilterPicker dropdown |
| FilterPicker still allows toggling individual filters on/off | FilterPicker.vue | Sidebar.spec.ts "re-renders when filters.visibleFilterIds changes" | - |

## Probe: Hide Empty Filters on Init

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Non-ontology columns: batch `_agg(filter: {col: {_notNull: true}})` probe | probeFilterColumns.ts | probeFilterColumns.spec.ts | - |
| Ontology columns: probe via `fetchAllOntologyBaseCounts` (reuses count fetcher cache) | probeFilterColumns.ts | probeFilterColumns.spec.ts "ontology column probing via count fetcher" | - |
| Ontology probe uses `_groupBy` without name/crossFilter — returns all terms with records | createCountFetcher.ts | createCountFetcher.spec.ts | - |
| `_groupBy` result cached in count fetcher — Ontology component reuses on expand (no duplicate query) | createCountFetcher.ts | - | Check: network tab shows single `_groupBy` per column |
| Probe runs once on init (after all dependencies defined) | useFilters.ts | - | - |
| Empty columns removed from `visibleFilterIds` (unchecked in Customize dialog) | useFilters.ts | - | Check: empty columns not checked in Customize |
| Only auto-removes when user hasn't customized (`userHasCustomized` guard) | useFilters.ts | - | - |
| User can re-add empty column via Customize — filter section appears with "No matching options" | useFilters.ts, Ontology.vue | - | Check: re-added empty filter shows message |
| Fail-open: on probe error, columns remain visible | probeFilterColumns.ts | probeFilterColumns.spec.ts "returns all columns on GraphQL error" | - |
| Nested paths produce correct nested notNull filter | probeFilterColumns.ts | probeFilterColumns.spec.ts "builds correct notNull filter for nested paths" | - |

## Ontology Filter: Base Counts & Smart Loading

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| `fetchAllOntologyBaseCounts`: single `_groupBy` discovers all terms with records | createCountFetcher.ts | createCountFetcher.spec.ts | - |
| In sidebar (`forceList`): load ONLY terms present in baseCounts from ontology | Ontology.vue | Ontology.spec.ts "forceList path loads only terms present in baseCounts" | Check: only terms with records shown |
| In sidebar: no page-by-page loading, no autoPageAfterPrune | Ontology.vue | - | - |
| Without countFetcher (standalone): load all terms page-by-page as before | Ontology.vue | Ontology.spec.ts (pagination tests) | - |
| When baseCounts empty + forceList: show "No matching options" | Ontology.vue | - | Check: message shown for empty ontology filter |
| Search still works (loads matching terms from ontology regardless of baseCounts) | Ontology.vue | Ontology.spec.ts (search tests) | Check: search finds terms |

## Filter Option Counts (Cross-Filter)

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Cross-filter counts shown per option as single number "(N)" | Ref.vue, Ontology.vue, CheckboxGroup.vue, TreeNode.vue | createCountFetcher.spec.ts | Check: counts like "(8)" shown |
| Counts update when other filters change (debounced 300ms) | Ontology.vue, Ref.vue | - | Check: counts refresh on filter change |
| Counts update in-place without full re-render | CheckboxGroup.vue, TreeNode.vue | - | Check: no flicker on count change |
| `:key="option.value"` on v-for for stable DOM tracking | CheckboxGroup.vue | - | Check: options don't jump on count update |
| Nested paths use per-item `_agg` (not `_groupBy`) | createCountFetcher.ts | createCountFetcher.spec.ts (3 nested tests) | - |
| Ontology nested counts use `_match_any_including_children` | createCountFetcher.ts | createCountFetcher.spec.ts | - |

## URL Sync

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Filter state serialized to URL params | useFilters.ts | filterUrlCodec.spec.ts (56+ tests) | Check: URL updates on filter change |
| Ontology values round-trip as plain strings (not `{name}` objects) | useFilters.ts | filterUrlCodec.spec.ts (ontology parsing tests) | Check: no empty filter wells after reload |
| ONTOLOGY_ARRAY filter URL hydration reflects checkbox checked state | typeUtils.ts (getOntologyArrayValues), Input.vue | typeUtils.spec.ts "passes through plain string items (filter path shape)" | Check: `?type.name=|Clinical+trial` shows Clinical trial checkbox checked in sidebar |
| REF filter URL round-trip preserves selection | useFilters.ts, Ref.vue | filterUrlCodec.spec.ts "REF filter URL round-trip" (4 tests), Ref.spec.ts "REF URL-hydrated modelValue" (2 tests) | Check: `?type.name=Cohort+study` shows correct checkbox checked after reload |
| REF checkbox reflects URL-hydrated selection (checked state) | Ref.vue | Ref.spec.ts "renders the checkbox as checked for URL-hydrated partial ref object" | Check: `?type.name=Catalogue` shows Catalogue checkbox visually checked in sidebar |
| REF emits consistent object shape after URL-hydrated + new selection | Ref.vue | Ref.spec.ts "emits full object shape when user selects an additional option after URL-hydration" | - |
| REF emits empty array when URL-hydrated selection is removed | Ref.vue | Ref.spec.ts "emits empty array when user removes the URL-hydrated selection" | - |
| REF modelValue watch calls lightweight `syncSelectionFromModel()` not full `init()` — no `isInitializing` guard, no tableMetadata re-fetch, no `loadOptions` | Ref.vue | Ref.spec.ts "selectionMap is never transiently empty when modelValue round-trips the same value" | - |
| REF round-trip of same modelValue is a no-op (signature match → early return, no fetch) | Ref.vue | Ref.spec.ts "selectionMap is never transiently empty when modelValue round-trips the same value" | Check: no flicker after user click round-trips through URL |
| REF concurrent modelValue updates don't leave stale state — staleness check after fetch, last value wins | Ref.vue | Ref.spec.ts "concurrent modelValue updates are not dropped — last value wins" | - |
| ONTOLOGY URL-hydrated modelValue reflected as checked checkbox after mount | Ontology.vue | Ontology.spec.ts "reflects URL-hydrated modelValue as checked checkbox after mount" | Check: `?type=Catalogue` shows Catalogue checkbox checked in sidebar |
| ONTOLOGY modelValue watch calls lightweight `syncSelectionFromModel()` — signature-deduped, no destructive wipe, staleness check after fetch | Ontology.vue | Ontology.spec.ts "round-trip of same modelValue is a no-op" | - |
| ONTOLOGY round-trip of same modelValue is a no-op (signature match → early return, no fetch) | Ontology.vue | Ontology.spec.ts "round-trip of same modelValue is a no-op — no extra fetch, valueLabels stay populated" | Check: no flicker after user click round-trips through URL |
| ONTOLOGY concurrent modelValue updates don't leave stale labels — staleness check after fetch, last value wins | Ontology.vue | Ontology.spec.ts "concurrent modelValue updates — last value wins, no stale labels" | - |
| Nested paths serialize with `.name` suffix | useFilters.ts | filterUrlCodec.spec.ts | - |
| Back/forward navigation restores filter state | useFilters.ts | useFilters.spec.ts "reactively updates when URL changes" | Check: browser back restores filters |

## Nested Path Filters

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| `buildFilter` resolves leaf column type via `columnTypeMap` | buildFilter.ts | buildFilter.spec.ts "nested ontology with plain string values" | - |
| Nested ontology produces `_match_any_including_children` (not `equals`) | buildFilter.ts, useFilters.ts | buildFilter.spec.ts | Check: correct row count for nested ontology filter |
| `getSubclassColumns` matches `inheritName` against table name (not id) | getSubclassColumns.ts | getSubclassColumns.spec.ts "matches inheritName against table name, not id" | - |

## Filter Removal

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| `toggleFilter` removes filter and clears state + gqlFilter | useFilters.ts | useFilters.spec.ts (4 removal tests) | - |
| `resetFilters` clears all non-default filter state | useFilters.ts | useFilters.spec.ts "resetFilters clears all" | - |
| URL updates after filter removal | useFilters.ts | useFilters.spec.ts "toggleFilter with URL sync" | - |

## Customize Filter Dialog

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| `isSelectableFilterType(ct)` returns true for ONTOLOGY, ONTOLOGY_ARRAY, BOOLEAN, RADIO, CHECKBOX, DATE, DATE_ARRAY, INT, INT_ARRAY, DECIMAL, DECIMAL_ARRAY, LONG, NON_NEGATIVE_INT, NON_NEGATIVE_INT_ARRAY | filterTreeUtils.ts | filterTreeUtils.spec.ts | - |
| `isStringFilterType(ct)` returns true for STRING, STRING_ARRAY, TEXT, EMAIL, HYPERLINK, UUID, AUTO_ID and variants | filterTreeUtils.ts | filterTreeUtils.spec.ts | - |
| `isRefExpandable(ct)` returns true for REF, REF_ARRAY, REFBACK, SELECT, MULTISELECT | filterTreeUtils.ts | filterTreeUtils.spec.ts | - |
| `navDepth(ct)` returns 2 for REF/SELECT, 1 for REF_ARRAY/REFBACK/MULTISELECT | filterTreeUtils.ts | filterTreeUtils.spec.ts | - |
| `shouldExcludeSelfRef(col, parentTableId)` returns true when col.refTableId === parentTableId | filterTreeUtils.ts | filterTreeUtils.spec.ts | - |
| `filterTreeNodes(nodes, query)` returns all nodes for empty query | filterTreeUtils.ts | filterTreeUtils.spec.ts "returns all nodes when query is empty" | - |
| `filterTreeNodes` matches nodes by label or description (case-insensitive) | filterTreeUtils.ts | filterTreeUtils.spec.ts "returns node whose label matches" / "matches by description" / "match is case-insensitive" | - |
| `filterTreeNodes` includes parent with only matching children when parent doesn't match | filterTreeUtils.ts | filterTreeUtils.spec.ts "includes parent when a child label matches" | - |
| `filterTreeNodes` excludes parent when neither it nor descendants match | filterTreeUtils.ts | filterTreeUtils.spec.ts "excludes parent when neither it nor any descendant matches" | - |
| `filterTreeNodes` recursively filters deeply nested children | filterTreeUtils.ts | filterTreeUtils.spec.ts "recursively filters deeply nested children" | - |
| `buildFilterColumnTree` returns selectable nodes for selectable types, expandable nodes for ref types, hidden string nodes | buildFilterColumnTree.ts | buildFilterColumnTree.spec.ts | - |
| `buildFilterColumnTree` respects navDepth: REF 2 levels, REF_ARRAY/REFBACK 1 level | buildFilterColumnTree.ts | buildFilterColumnTree.spec.ts | - |
| `buildFilterColumnTree` excludes self-referencing sub-columns | buildFilterColumnTree.ts | buildFilterColumnTree.spec.ts | - |
| `buildFilterColumnTree` excludes HEADING, SECTION, FILE, mg_* columns | buildFilterColumnTree.ts | buildFilterColumnTree.spec.ts | - |
| Customize button inline-right of Filters h2, uses filter icon + "Customize" label, label styling | Sidebar.vue | - | Check: button appears on same line as Filters heading |
| Customize button opens large Dialog (not dropdown) | FilterPicker.vue | - | Check: dialog large enough to navigate tree |
| Dialog opens with localSelection initialized from current visibleFilterIds | FilterPicker.vue | FilterPicker.spec.ts "initializes localSelection from visibleFilterIds on open" | Check: pre-checked filters match sidebar |
| Tree v-model bound to localSelection (not live visibleFilterIds) | FilterPicker.vue | FilterPicker.spec.ts "does not call toggleFilter when Tree emits changes" | - |
| Save button applies localSelection diff to visibleFilterIds and closes dialog | FilterPicker.vue | FilterPicker.spec.ts "calls toggleFilter for added/removed ids on Save" | Check: sidebar updates after Save |
| Cancel button discards localSelection without calling toggleFilter | FilterPicker.vue | FilterPicker.spec.ts "does not call toggleFilter on Cancel" | Check: sidebar unchanged after Cancel |
| Clear button sets localSelection to [] without applying immediately | FilterPicker.vue | FilterPicker.spec.ts "sets local selection to empty array" / "does not apply clear until Save" | Check: all unchecked after Clear; Save required to apply |
| Reset to defaults sets localSelection to computeDefaultFilters result without applying immediately | FilterPicker.vue | FilterPicker.spec.ts "sets localSelection to computeDefaultFilters result" / "does not apply reset until Save" | Check: defaults restored in tree; Save required to apply |
| Footer layout: [Clear] [Reset to defaults] on left, [Cancel] [Save] on right | FilterPicker.vue | FilterPicker.spec.ts "shows Save and Cancel" / "shows Clear and Reset" | Check: button layout correct |
| String columns hidden by default (showAll off, no search); revealed by search or showAll toggle | FilterPicker.vue | FilterPicker.spec.ts "hides STRING columns" / "shows STRING columns when showAll toggle is enabled" / "includes string columns when search matches" | Check: search and toggle reveal string columns |
| "Show all filter types" checkbox when checked reveals all column types including strings | FilterPicker.vue | FilterPicker.spec.ts "shows STRING columns when showAll toggle is enabled" | Check: toggle shows more columns |
| Search uses filterTreeNodes — recursive, matches label/description, hides non-matching parent when only child matches | FilterPicker.vue | FilterPicker.spec.ts "filters tree nodes to matching columns" | Check: search narrows tree correctly |
| Tree scrollable only when content overflows (max-h-[60vh] on tree wrapper, not dialog) | FilterPicker.vue | - | Check: no unnecessary scrollbar when tree is short |
| REF/REF_ARRAY/REFBACK/SELECT/MULTISELECT shown as expandable (+) node, not directly checkable | FilterPicker.vue | - | Check: ref columns show + expand button |
| Expanding ref node loads sub-columns; self-refs excluded | FilterPicker.vue | - | Check: no circular refs in tree |
| Sub-columns filtered by navDepth (REF 2 levels, REF_ARRAY/REFBACK 1 level) | FilterPicker.vue | - | Check: cannot navigate deeper than allowed |

## Active Filters Bar

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Shows chips for active filters with label + value | ActiveFilters.vue | - | Check: chips show column name + value |
| Remove button on each chip clears that filter | ActiveFilters.vue | - | Check: clicking X removes filter |
| "Remove all" clears everything | ActiveFilters.vue | - | Check: clears all filters |
