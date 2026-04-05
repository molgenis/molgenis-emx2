# feat/tw-filters — Filter System for tailwind-components

## Summary

New filter system for the tailwind-components library: composable-driven, URL-synced, with faceted counts and smart defaults. Replaces the old catalogue-specific filter implementation with a reusable, schema-driven approach.

**61 files changed** (against origin/master): 16 new source files, 6 stories, 14 test files, 2 shared test fixtures, 8 modified existing components, 6 test quality fixes, plus config/CSS/types.

**421 vitest tests, all passing** (49 test files).

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

### ✅ Completed — Task 5: Collapsible filter sections

Replaced Accordion with lightweight catalogue-style collapsible. Chevron next to label, `<hr>` dividers, `p-5` padding, no "Remove" button, `v-show` keeps DOM mounted.

**Final approach:** Custom collapsible in Sidebar.vue matching catalogue's `FilterContainer` pattern (not Accordion component). Collapsed state tracked via reactive `Set<string>`.

Files: `Sidebar.vue`, `Column.vue` (added `showLabel` prop), `Sidebar.spec.ts`

### ✅ Completed — Task 6: Show all filters (collapsed, lazy, data-driven)

**6a: Show all filterable columns** — `computeDefaultFilters.ts` simplified to return ALL filterable column ids (excluding HEADING, SECTION, FILE, mg_*). No priority ordering or max-5 cap. FilterPicker still allows toggling.

**6b: Lazy load filter content on expand** — `Sidebar.vue` changed `v-show` to `v-if`. FilterColumn only mounts when section is expanded, so options/counts only fetch on expand. Fixed collapsed Set reactivity (reassign instead of mutate). Sidebar.spec.ts updated: `expandAllSections()` helper called before asserting on FilterColumn.

**6c: notNull probe — hide filters with no data** — New `probeFilterColumns.ts` utility batches `_agg(filter: {col: {_notNull: true}}) { count }` queries using GraphQL aliases. Returns `Set<string>` of columns with data. Wired into `useFilters.ts`: runs once when columns first load, filters `resolvedFilters` by result. Fails-open on error.

Files: `computeDefaultFilters.ts`, `Sidebar.vue`, `useFilters.ts`, `probeFilterColumns.ts` (new), `computeDefaultFilters.spec.ts`, `Sidebar.spec.ts`, `probeFilterColumns.spec.ts` (new)

### ✅ Completed — Regression fix: REF URL round-trip (partial key object)

Bug: URL `type.name=Cohort+study` loaded but Ref.vue `init()` called `extractPrimaryKey` on the partial `{name: "Cohort study"}` object, which resolved to `undefined` (primary key not present), causing `fetchTableData` to return all rows instead of the selected one — `selectionMap` ended up with all options selected.

Fix: `Ref.vue init()` detects single-field partial objects (URL-hydrated values) and builds a field-based filter `{name: {equals: [...]}}` directly, bypassing `extractPrimaryKey`. Full primary-key objects (multiple fields) still use the existing path.

Red-green: 2 failing component tests confirmed the bug (`selectionMap` had 6 items instead of 1). 4 codec tests verified the URL round-trip was already correct (codec was not the issue). Fix made all 6 tests green. Total: 436 tests passing.

Files: `Ref.vue`, `filterUrlCodec.spec.ts`, `Ref.spec.ts`, `filter-sidebar.md`

## ✅ Completed — Task 7: Auto-expand sections with URL-set filters

Sidebar `collapsed` Set initialization now filters out columns that have an active filter state on mount. Initial collapsed set excludes any `fullPath` present in `filterStates`, so those sections start expanded. User can still manually collapse them afterward — the set is only computed once at mount, no watch forces re-open on filter change.

`expandAllSections` test helper updated to only click collapsed sections (caret has `rotate-180` class), preventing it from accidentally re-collapsing already-expanded sections.

Red-green: 2 new tests failed before fix (disease section not visible), passed after. All 439 tests green.

Files: `Sidebar.vue`, `Sidebar.spec.ts`, `filter-sidebar.md`, `tw-filters.md`

## ✅ Completed — Regression fix: REF checkbox unchecked after URL hydration

Partial regression: `Ref.vue init()` correctly populated `selectionMap` with full rows from `fetchTableData`, but those rows were stored as-is (full row objects). When user later selected an additional option, `select()` stored the primary key object from `extractPrimaryKey` — causing `Object.values(selectionMap)` to emit inconsistent shapes (`[fullRow, pkeyObj]`).

Fix: `init()` now calls `extractPrimaryKey(row)` on each fetched row before storing in `selectionMap`, matching what `select()` already does. All selectionMap values are now primary key objects (`{name:"..."}` shape), matching the URL-codec contract.

Also updated `fetchRowPrimaryKey` test mock to return `{name: row.name}` (proper primary key object shape) instead of `row.id` (string) — aligns with real `fetchRowPrimaryKey` return type `Record<string,string>`.

Red-green: 3 new tests added to `Ref.spec.ts` — "renders the checkbox as checked for URL-hydrated partial ref object" passed immediately (existing init() fix from previous task was sufficient), "emits full object shape when user selects additional option after URL-hydration" failed (RED) due to inconsistent selectionMap values, "emits empty array when user removes the URL-hydrated selection" passed. Fix made all green. Total: 442 tests.

Files: `Ref.vue`, `Ref.spec.ts`, `filter-sidebar.md`, `tw-filters.md`

## Future Work

- **Type fetchGraphql responses** — `fetchGraphql` returns `Promise<any>`. Fix: add generic typing `fetchGraphql<T>()`, define response interfaces (`IGroupByResponse`, `IAggResponse`, `IOntologySizeProbe`), type test mock data. This catches code↔mock drift at compile time.
- **E2e smoke tests for API contracts** — 9 test files (101 tests) mock backend responses with no type safety. Add 3 e2e smoke tests (filter sidebar, ontology input, ref facet counts) to anchor response shapes against real backend.
- **Backend: add limit/offset to _groupBy** — `_groupBy` GraphQL field lacks `limit`/`offset` parameters. The backend dataFetcher already handles them — only the GraphQL field definition needs updating (`GraphqlTableFieldFactory.tableGroupByField()`, add `.argument()` calls). Enables paginating through "terms with records" for large flat ontologies (10,000+ terms).
- **Consolidate test mocks** — shared `$fetch` stub (duplicated in Sidebar/Column specs), standardise mockRoute/mockRouter patterns, simplify gqlFilter assertions in useFilters.spec.ts that overlap with buildFilter.spec.ts.
