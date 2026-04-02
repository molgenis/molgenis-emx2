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

## Filter Option Counts

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Cross-filter counts shown per option (excludes current column) | Ref.vue, Ontology.vue | createCountFetcher.spec.ts | - |
| Counts update in-place without full re-render | CheckboxGroup.vue, TreeNode.vue | - | Check: no flicker on count change |
| `:key="option.value"` on v-for for stable DOM tracking | CheckboxGroup.vue | - | Check: options don't jump on count update |
| No global loading spinner — counts appear instantly | CheckboxGroup.vue, TreeNode.vue | - | Check: no spinner on count refresh |
| Nested paths use per-item `_agg` (not `_groupBy`) | createCountFetcher.ts | createCountFetcher.spec.ts (3 nested tests) | - |
| Ontology nested counts use `_match_any_including_children` | createCountFetcher.ts | createCountFetcher.spec.ts | - |

## Base Count Hiding

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Options with baseCount=0 hidden (not removed from DOM) | Ref.vue, Ontology.vue | Ref.spec.ts "hides options where baseCount is 0" | - |
| Options with crossFilter count=0 but baseCount>0 remain visible | Ref.vue | Ref.spec.ts "shows options with baseCount>0 even when crossFilter count is 0" | - |
| Ontology uses `visible=false` + `hiddenByCount` flag (no tree mutation) | Ontology.vue | Ontology.spec.ts "should show pruned nodes when clicking show hidden button" | - |
| "Show X hidden options" button appears when options are hidden | Ref.vue, Ontology.vue | Ref.spec.ts "reveals hidden options", Ontology.spec.ts same | Check: button visible below options |
| Clicking show/hide toggles instantly (no reload for Ontology) | Ontology.vue | Ontology.spec.ts "should re-hide nodes" | Check: no flicker on toggle |
| "Show hidden" button hidden during search | Ref.vue, Ontology.vue | Ref.spec.ts "hides show-hidden button during search", Ontology.spec.ts same | - |
| Search shows all matching options regardless of baseCount | Ref.vue, Ontology.vue | - | Check: search results include zero-count options |
| TreeNode does NOT show "hidden by search" for count-hidden nodes | TreeNode.vue | Ontology.spec.ts "should not show hidden by search message" | - |

## URL Sync

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Filter state serialized to URL params | useFilters.ts | filterUrlCodec.spec.ts (51+ tests) | Check: URL updates on filter change |
| Ontology values round-trip as plain strings (not `{name}` objects) | useFilters.ts | filterUrlCodec.spec.ts (ontology parsing tests) | Check: no empty filter wells after reload |
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
