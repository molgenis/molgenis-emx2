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

## notNull Probe (hide empty filters)

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Batch `_agg(filter: {col: {_notNull: true}}) { count }` probe on load | probeFilterColumns.ts | probeFilterColumns.spec.ts (6 tests) | - |
| Filters with count=0 hidden from resolvedFilters | useFilters.ts | probeFilterColumns.spec.ts | Check: empty-data filters not shown |
| Probe runs once when columns first load (not per cross-filter update) | useFilters.ts | - | - |
| Fail-open: on GraphQL error, all filters shown | probeFilterColumns.ts | probeFilterColumns.spec.ts "returns all columns on GraphQL error" | - |
| Nested paths produce correct nested notNull filter | probeFilterColumns.ts | probeFilterColumns.spec.ts "builds correct notNull filter for nested paths" | - |

## Filter Option Counts

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Cross-filter counts shown per option as single number "(N)" | Ref.vue, Ontology.vue, CheckboxGroup.vue, TreeNode.vue | createCountFetcher.spec.ts | Check: counts like "(8)" shown, no dual "x/y" display |
| Counts update in-place without full re-render | CheckboxGroup.vue, TreeNode.vue | - | Check: no flicker on count change |
| `:key="option.value"` on v-for for stable DOM tracking | CheckboxGroup.vue | - | Check: options don't jump on count update |
| No global loading spinner — counts appear instantly | CheckboxGroup.vue, TreeNode.vue | - | Check: no spinner on count refresh |
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

## Active Filters Bar

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Shows chips for active filters with label + value | ActiveFilters.vue | - | Check: chips show column name + value |
| Remove button on each chip clears that filter | ActiveFilters.vue | - | Check: clicking X removes filter |
| "Remove all" clears everything | ActiveFilters.vue | - | Check: clears all filters |
