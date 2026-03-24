# Plan: feat/tw-filters — Filter System in tailwind-components

## Current status

Phase 13 complete. Suggested next: Phase 11 (UseFilters object, large).

## Phase 14: Fix paging param loss on filter URL update [x]

### What was done
- Added `getNonFilterParams()` helper in `useFilters.ts` — preserves all query params except `mg_search` and column-ID-based filter params
- Updated `actualFilterStates.set()` and `updateUrl()` to use `getNonFilterParams` instead of only preserving `mg_*` params
- Sidebar.vue was already correct (spreads full `route.query`)
- Added test: non-filter params like `page` and `view` are preserved on filter URL update
- Updated existing reserved-params test to also verify non-mg params
- Stabilized `gqlFilter` ref with JSON comparison guard — prevents spurious updates when URL changes for non-filter reasons (paging, sorting), which was causing TableEMX2 to reset page to 1
- All 73 useFilters tests pass

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

## Phase 13: Test readability improvements [x]

### What was done
- Standardized fake timers: `beforeEach`/`afterEach` pattern in FilterPicker, Ref, Ontology specs
- Replaced raw `setTimeout(resolve, 600)` with `vi.advanceTimersByTime()` + `flushPromises()` in FilterPicker
- Removed inline `vi.useFakeTimers()`/`vi.useRealTimers()` from Ref and Ontology specs
- Removed "should" prefix from 30 test names in useFilters.spec.ts
- Fixed `toBeLessThanOrEqual(5)` → `toBe(5)` in Sidebar.spec.ts
- Moved `computeDefaultFilters` tests to own spec file (`utils/computeDefaultFilters.spec.ts`)
- Extracted shared column fixtures (`orderColumn`, `nameColumn`, `userColumn`) in buildFilter.spec.ts
- All 212 filter tests pass across 10 files

---

## Phase 11: UseFilters object pattern

### Status: TODO

### Problem
`useFilters` returns individual refs/functions that consumers destructure. Sidebar receives filter state via multiple v-models (`filterStates`, `searchTerms`). This differs from the `UseForm` pattern where a single rich object is passed as one prop.

### Goal
Align with `UseForm` pattern — `useFilters` returns a `UseFilters` object passed as single prop. Sidebar and ActiveFilters both accept the object directly, reducing consumer wiring.

```typescript
const filters = useFilters(columns, { urlSync: true, route, router });

<FilterSidebar :filters="filters" :schemaId="schemaId" :tableId="tableId" />
<ActiveFilters :filters="filters" />
```

### Consumers (3 files)
1. `apps/ui/app/pages/[schema]/[table]/index.vue` — production table view
2. `apps/tailwind-components/app/pages/table/EMX2.story.vue` — story
3. `apps/tailwind-components/app/pages/filter/Sidebar.story.vue` — story

### Resolved questions
1. `visibleFilterIds` and filter picker stay in Sidebar (UI concern, not state)
2. Two-way binding via direct ref mutation (same as UseForm) — no v-model needed
3. `activeFilters` stays as computed on UseFilters return object
4. ActiveFilters receives full `filters` object — calls `filters.removeFilter()` and `filters.clearFilters()` internally
5. Sidebar.story.vue should use `useFilters()` to demo the real integration pattern

### Steps
- [ ] 11.1 Define `UseFilters` interface in `types/filters.ts`
- [ ] 11.2 Refactor `useFilters` to return typed `UseFilters` object
- [ ] 11.3 Update Sidebar: replace v-model:filterStates/searchTerms with `filters: UseFilters` prop
- [ ] 11.4 Update ActiveFilters: accept `filters: UseFilters` prop, internalize remove/clearAll
- [ ] 11.5 Update consumers: `apps/ui/.../[table]/index.vue`, `EMX2.story.vue`, `Sidebar.story.vue`
- [ ] 11.6 Update tests (Sidebar.spec.ts, ActiveFilters.spec.ts, useFilters.spec.ts)
- [ ] 11.7 Run format + lint on tailwind-components and ui apps

### Done so far
- [x] Added `activeFilters` computed to `useFilters` return value
- [x] Removed duplicate `activeFiltersList` from ui/index.vue and EMX2.story.vue

### Future (out of scope)
- Compose entire table view (filters + table + pagination) as reusable component developers can embed anywhere

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
