# feat/tw-filters — Filter System for tailwind-components

## Summary

New filter system for the tailwind-components library: composable-driven, URL-synced, with faceted counts and smart defaults. Replaces the old catalogue-specific filter implementation with a reusable, schema-driven approach.

**61 files changed** (against origin/master): 16 new source files, 6 stories, 14 test files, 2 shared test fixtures, 8 modified existing components, 6 test quality fixes, plus config/CSS/types.

**418 vitest tests, all passing** (48 test files).

---

## Reviewer Checklist

### Core: useFilters composable (`app/composables/useFilters.ts`)
- [ ] Returns typed `UseFilters` object with all filter state + methods
- [ ] URL sync: bidirectional (filter↔URL), preserves non-filter params (page, view)
- [ ] Debounced `gqlFilter` computed with JSON comparison guard (prevents spurious updates)
- [ ] 5 operators: `equals`, `like`, `between`, `notNull`, `isNull`
- [ ] `like` always uses AND semantics for space-separated terms
- [ ] Serialization round-trip: filter state → URL string → parse → GraphQL filter
- [ ] Tests: `composables/useFilters.spec.ts` (~33 composable tests, incl. toggleFilter/removeFilter/resetFilters removal paths) + `utils/filterUrlCodec.spec.ts` (51 pure-function tests, incl. ontology URL round-trip tests)

### Filter components (`app/components/filter/`)
- [ ] **Sidebar.vue** (~30 lines) — pure display wrapper, receives `UseFilters` prop
- [ ] **FilterPicker.vue** — column toggle dropdown, excludes HEADING/SECTION/FILE/mg_* columns
- [ ] **Column.vue** — dispatches to correct input type (text/range/bool), operator derivation from column type
- [ ] **Range.vue** — min/max tuple input with slot-based API
- [ ] **ActiveFilters.vue** — chip display with remove/clearAll events
- [ ] Stories exist for all 5 components + useFilters composable

### Filter utilities (`app/utils/`)
- [ ] **buildFilter.ts** — converts filter state to GraphQL filter syntax (nested refs, ontology `_match_any_including_children`); accepts optional `columnTypeMap` to resolve leaf column types for nested paths
- [ ] **createCountFetcher.ts** — `ICountFetcher` factory: ref counts via `_groupBy`, ontology counts via `_groupBy` + `_agg`, base counts (no cross-filter) for hiding empty options; nested paths use per-item `_agg` via shared `_fetchAggCounts` helper
- [ ] **computeDefaultFilters.ts** — smart defaults: ontology first, then refs, max 5
- [ ] **formatFilterValue.ts** — display formatting for active filter chips
- [ ] **resolveFilterLabels.ts** — async label resolution for nested ref paths
- [ ] **filterConstants.ts** — shared constants

### Filter types (`types/filters.ts`) + modified types (`types/types.ts`)
- [ ] `IFilterValue`, `FilterOperator`, `IGraphQLFilter`, `UseFilters` interface, `ICountFetcher` interface
- [ ] `ITreeNodeState.hiddenByCount` — distinguishes base-count-hidden from search-hidden nodes

### Modified existing components
- [ ] **Ref.vue** — facet counts, base count hiding (baseCount=0 hidden via `visibleByBaseCount` computed, crossFilter count=0 shown), search overrides hiding, clickable "show/hide hidden options" toggle
- [ ] **Ontology.vue** — visibility-based hiding by base counts (`hiddenByCount` flag, no physical removal), auto-paging after hiding, parent counts via `_agg`, clickable "show/hide hidden options" toggle, `reload()` skips empty GraphQL query when counts cached
- [ ] **TreeNode.vue** — simplified (just renders what it's given), excludes `hiddenByCount` nodes from "hidden by search" message
- [ ] **CheckboxGroup.vue** — count display, `:key` on v-for for stable DOM tracking
- [ ] **RadioGroup.vue** — minor fix
- [ ] **TableEMX2.vue** — `below-toolbar` slot for ActiveFilters
- [ ] **fetchTableMetadata.ts** — `includeSubclassColumns` option

### UI app integration
- [ ] **apps/ui `[schema]/[table]/index.vue`** — filter sidebar + ActiveFilters bar
- [ ] **apps/ui `filter-counts.spec.ts`** — e2e Playwright test

### Test quality fixes (found during review, not filter-specific)
- [ ] **CheckboxGroup.spec.ts** — fixed broken assertions (bare `expect()` with no matcher), fixed event triggers (`focus`→`focusin`)
- [ ] **RadioGroup.spec.ts** — same fix
- [ ] **Listbox.spec.ts** — fixed `it.each([options])` → `it.each(options)` (was running once instead of per-option)
- [ ] **Form.spec.ts** — fixed incorrect mock path
- [ ] **Ref.spec.ts** — moved createCountFetcher test to proper utility spec, added show/re-hide toggle + search-override tests
- [ ] **Ontology.spec.ts** — expanded test coverage for tree pruning + auto-paging, added show/re-hide toggle + search-override + no-false-hidden-message tests

### Test infrastructure (new)
- [ ] **fixtures/columns.ts** — shared IColumn constants used across 6 test files
- [ ] **fixtures/mockFilters.ts** — unified UseFilters mock factory (reactive + stub modes)

---

## Architecture Decisions

1. **Composable-driven, not component-driven**: All filter state lives in `useFilters`. Components are thin display layers. This makes the filter system testable without DOM.

2. **Lazy facet counts**: Counts fetched per-input (not centralized), only for visible options. Cross-filter excludes current column. Debounced 300ms.

3. **Base count hiding**: Options with zero records in unfiltered dataset are hidden. Options that become zero due to cross-filtering remain visible (count shows 0). Search overrides hiding.

4. **URL as source of truth**: Filter state serialized to URL params. Supports back/forward navigation. Non-filter params preserved.

5. **5 operators only**: `equals`, `like`, `between`, `notNull`, `isNull`. Removed `like_or`, `like_and`, `in` for simplicity.

---

## Priority: Next Tasks

### ✅ Completed — Task 3: Prevent filter option flickering during count reloads

- CheckboxGroup: added `:key="option.value"` to v-for for stable DOM tracking
- Ref.vue: split `listOptions` into `visibleByBaseCount` (stable Set) + `listOptions` computed
- Ontology.vue: replaced physical node removal with `visible=false` + `hiddenByCount` flag — no tree structure mutation on count updates
- CheckboxGroup + TreeNode: removed global `countsLoading` spinner, counts update in-place
- TreeNode: excludes `hiddenByCount` nodes from "hidden by search" message
- Ontology `reload()`: skips empty GraphQL query when counts already cached

Files: `Ref.vue`, `Ontology.vue`, `CheckboxGroup.vue`, `TreeNode.vue`, `types/types.ts`

### ✅ Completed — Task 1: Click "X options hidden" to reveal all

- Ref.vue: added `showAllOptions` ref, bypass base count filter when true, clickable button toggles show/hide
- Ontology.vue: `toggleShowAllHidden()` toggles `visible` on count-hidden nodes (synchronous, no reload), `setHiddenNodesVisibility()` walks tree
- Both: "show hidden" button hidden during search (search already shows all matches)
- Tests: 4 Ontology tests (show, re-hide, search-override, no false message) + 3 Ref tests (show, re-hide, search-override)

Files: `Ref.vue`, `Ontology.vue`, `TreeNode.vue`, `types/types.ts`, `Ontology.spec.ts`, `Ref.spec.ts`

### ✅ Completed — not a bug — Task 2: Removing filter from sidebar should clear filter state

- 4 new tests added to `useFilters.spec.ts` covering `toggleFilter`/`removeFilter`/`resetFilters`/URL sync removal
- All pass green immediately — removal logic is correct
- Perceived issue likely caused by flicker (fixed in Task 3)

Files: `useFilters.ts`, `useFilters.spec.ts`

### ✅ Completed — Task 4: Enable nested filter counting + fix ontology URL round-trip

Three interleaved fixes for nested/path-based filters (e.g. `collectionEvents.ageGroups`):

**Ontology URL round-trip**: `parseFilterValue` returns plain strings for ONTOLOGY types (not `{ name: "..." }` objects). `serializeFiltersToUrl` appends `.name` for nested string values. Prevents empty filter wells after page reload.

**Nested counting**: Per-item `_agg` queries for nested paths via shared `_fetchAggCounts` helper. Ref counts use `equals`, ontology counts use `_match_any_including_children`. Non-nested paths still use efficient `_groupBy`.

**buildFilter columnTypeMap**: `buildGraphQLFilter` accepts optional `columnTypeMap` to resolve leaf column type for nested paths. Without it, `collectionEvents.ageGroups` resolved to REF_ARRAY (root column) instead of ONTOLOGY_ARRAY (leaf), producing wrong filter operator.

Backend verification: `_agg` through nested array refs returns correct distinct counts for single-value filters.

Files: `useFilters.ts`, `buildFilter.ts`, `createCountFetcher.ts`, `buildFilter.spec.ts`, `createCountFetcher.spec.ts`, `filterUrlCodec.spec.ts`

## Future Work

- **Type fetchGraphql responses** — `fetchGraphql` returns `Promise<any>`. Fix: add generic typing `fetchGraphql<T>()`, define response interfaces (`IGroupByResponse`, `IAggResponse`, `IOntologySizeProbe`), type test mock data. This catches code↔mock drift at compile time.
- **E2e smoke tests for API contracts** — 9 test files (101 tests) mock backend responses with no type safety. Add 3 e2e smoke tests (filter sidebar, ontology input, ref facet counts) to anchor response shapes against real backend.
- **Backend: add limit/offset to _groupBy** — `_groupBy` GraphQL field lacks `limit`/`offset` parameters. The backend dataFetcher already handles them — only the GraphQL field definition needs updating (`GraphqlTableFieldFactory.tableGroupByField()`, add `.argument()` calls). Enables paginating through "terms with records" for large flat ontologies (10,000+ terms).
- **Consolidate test mocks** — shared `$fetch` stub (duplicated in Sidebar/Column specs), standardise mockRoute/mockRouter patterns, simplify gqlFilter assertions in useFilters.spec.ts that overlap with buildFilter.spec.ts.
