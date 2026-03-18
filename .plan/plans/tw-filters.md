# Plan: feat/tw-filters — Filter System in tailwind-components

## Current status

Phase 10 complete. Working on Phase 11 (UseFilters object pattern).

## Phase 11: UseFilters object pattern

### Status: IN PROGRESS

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

### Open questions from Phase 7 (need product owner input)
1. `like_or`/`like_and` operators: string inputs should use this (except UUID) — not yet implemented
2. `serializeFilterValue({operator:"in", value:{}})` returning `"undefined"` — confirmed bug, not yet fixed
