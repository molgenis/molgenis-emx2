# Plan: feat/tw-filters — Filter System in tailwind-components

## Current status

Phase 12 complete. Suggested order: Phase 14 (bug fix, small), Phase 13 (test cleanup, medium), Phase 11 (UseFilters object, large).

## Phase 14: Fix paging param loss on filter URL update

### Status: TODO

### Problem
When user pages in table explorer, the `page` param gets dropped from the URL. The filter system's `useFilters.ts` and Sidebar only preserve `mg_*` prefixed params when calling `router.replace()`. The paging param (`page`) doesn't have that prefix, so it's wiped.

Reproduction: page to page 2 → Sidebar's `visibleFilterIds` watcher fires `router.replace()` → `page` param lost → snaps back to page 1.

### Root cause
In `useFilters.ts`, `serializeFiltersToUrl` builds params from scratch (filters + search). The callers (`updateUrl`, `actualFilterStates.set`) only preserve `mg_*` params from existing query. Non-`mg_*` params like `page` are dropped.

Same issue in `Sidebar.vue` — the `visibleFilterIds` watcher builds a new query with only `mg_filters` + existing `mg_*` params.

### Fix options
1. **Preserve all non-filter params**: Change the preserved-params logic to keep ALL existing query params except the ones being managed by the filter system (column filter keys + `mg_search` + `mg_filters`)
2. **Alternative**: Rename `page` to `mg_page` in consuming apps — but this couples paging to the `mg_` convention

Option 1 is better — the filter system shouldn't assume it owns all non-`mg_*` params.

### Steps
- [ ] 14.1 In `useFilters.ts`: change `updateUrl` and `actualFilterStates.set` to preserve all existing query params that aren't filter-managed keys
- [ ] 14.2 In `Sidebar.vue`: same fix for the `visibleFilterIds` watcher's `router.replace` call
- [ ] 14.3 Add test: setting a filter preserves unrelated query params (e.g., `page=2`)
- [ ] 14.4 Verify in catalogue app (manual) that paging survives filter changes

---

## Phase 12: Simplify FilterOperator [x]

### What was done
- Removed `like_or`, `like_and`, `in` from `FilterOperator` — only 5 operators remain: `equals`, `like`, `between`, `notNull`, `isNull`
- `like` now always uses AND semantics for multi-term strings (space-separated → `_and`)
- `parseFilterTerms` simplified: removed `mode` field from `ParsedTerms` type (always AND)
- `in` replaced with `equals` everywhere: `parseFilterValue`, `serializeFilterValue`, `formatFilterValue`
- Removed dead `AND_VALUE_SEPARATOR` constant
- Updated story file (`useFilters.story.vue`) to use `equals` instead of `in`
- All 150 tests updated and passing

## Phase 13: Test readability improvements

### Status: TODO

### Steps
- [ ] 13.1 Standardize fake timers: use `beforeEach`/`afterEach` across all filter test files
- [ ] 13.2 Remove raw `setTimeout(resolve, 600)` in FilterPicker.spec.ts → use fake timers
- [ ] 13.3 Fix inline `vi.useFakeTimers()`/`vi.useRealTimers()` in Ref.spec.ts
- [ ] 13.4 Fix test naming: remove `should` prefix inconsistency, fix misleading names
- [ ] 13.5 Sidebar.spec.ts: extract `waitForMetadataLoad()` helper, fix `toBeLessThanOrEqual(5)` → `toBe(5)`
- [ ] 13.6 Move `computeDefaultFilters` tests from FilterPicker.spec.ts to own spec file
- [ ] 13.7 Extract shared column fixtures in buildFilter.spec.ts

---

## Phase 11: UseFilters object pattern

### Status: TODO

### Problem
`useFilters` returns individual refs/functions that consumers destructure. Sidebar receives filter state via multiple v-models (`filterStates`, `searchTerms`). This differs from the `UseForm` pattern where a single rich object is passed as one prop.

Currently:
```typescript
const { filterStates, searchValue, gqlFilter, activeFilters, removeFilter, clearFilters } =
  useFilters(columns, { urlSync: true, route, router });

// Sidebar receives via multiple v-models:
<FilterSidebar v-model:filterStates="filterStates" v-model:searchTerms="searchValue" ... />
```

### Goal
Align with `UseForm` pattern — `useFilters` returns a `UseFilters` object that can be passed as a single prop:

```typescript
const filters = useFilters(columns, { urlSync: true, route, router });

<FilterSidebar :filters="filters" :schemaId="schemaId" :tableId="tableId" />
<ActiveFilters :filters="filters.activeFilters" @remove="filters.removeFilter" ... />
```

### Steps
- [ ] 11.1 Define `UseFilters` interface in `types/filters.ts`
- [ ] 11.2 Refactor `useFilters` to return typed `UseFilters` object (backward-compatible — destructuring still works)
- [ ] 11.3 Update Sidebar to accept `filters: UseFilters` prop instead of separate v-models
- [ ] 11.4 Update ui/index.vue and EMX2.story.vue to pass `filters` object
- [ ] 11.5 Update tests
- [ ] 11.6 Review: does ActiveFilters still need separate props or can it use `filters.activeFilters` directly?

### Open questions
1. Should Sidebar still own `visibleFilterIds` and filter picker, or should that move into `useFilters` too?
2. How does two-way binding work? `UseForm` uses `form.values.value[key]` directly. Filters could use `filters.filterStates` as a writable ref.
3. Should `activeFilters` be a computed on `UseFilters` or a separate composable?

### Done so far
- [x] Added `activeFilters` computed to `useFilters` return value
- [x] Removed duplicate `activeFiltersList` from ui/index.vue and EMX2.story.vue

---

## Phase 10: Simplify FilterColumn [x]

### What was done
- Created `createCountFetcher` factory (`app/utils/createCountFetcher.ts`) — stateless `ICountFetcher` with `fetchRefCounts`, `fetchOntologyLeafCounts`, `fetchOntologyParentCounts`
- Column.vue props reduced from 8 to 4: `column`, `label?`, `removable?`, `countFetcher?`
- Removed prop drilling: `schemaId`, `tableId`, `columnPath`, `crossFilter`, `depth`, `labelPrefix` all gone from Column.vue and Input.vue
- Replaced `ResolvedFilter` with `IFilter` interface (fullPath, column, label)
- CSS var overrides moved to `.filter-sidebar-context` scoped class in Sidebar
- Deleted `useFilterCounts` composable (replaced by `createCountFetcher`)
- `ICountFetcher.getCrossFilter()` exposed as plain getter for reactive watching
- Added tests for nested filter path resolution from URL
- Fixed bug: `loadRefColumnsForPath` ran before metadata loaded — added `filterableColumns` watcher

---

## Phase 9: Lazy per-input facet counts [x]

### What was done
- Moved count fetching from centralized Sidebar into InputRef and InputOntology
- Counts fetched only for currently-visible options (not all values)
- Cross-filter per column (all filters EXCEPT current) for faceted counts
- Ontology parent counts via `_agg` + `_match_any_including_children`
- Debounced refetch on crossFilter change (~300ms)
- Opacity transition for async count loading (no spinners)

---

## Phases 1–8 [x]

See git history. Key milestones:
- Phase 1–6: Port filter system to tailwind-components
- Phase 7: Review fixes, test improvements, dead code removal
- Phase 8: Simplification — self-contained Sidebar, removed mobileDisplay, extracted utilities

### Resolved from Phase 7
1. `like_or`/`like_and` → dropped, `like` always uses AND semantics (Phase 12)
2. `in` operator → dropped, use `equals` instead (Phase 12)
