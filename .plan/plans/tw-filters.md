# Plan: feat/tw-filters — Filter System in tailwind-components

## Current status

Phases 11–16 done. useFilters is a complete filter controller. Sidebar and FilterPicker are pure display.

## Phase 16: Ontology fix + e2e tests + cleanup [x]

### What was done
- Fixed `buildGraphQLFilter` for ONTOLOGY columns: uses `_match_any_including_children` instead of `{ name: { equals: [...] } }`
- Added `FilterValue` type to replace `any` in `IFilterValue.value`
- Added Playwright e2e tests (`apps/ui/tests/e2e/filter-counts.spec.ts`): counts visible, filter click works, URL filters, ontology parent matching
- Memoized `getCountFetcher` to prevent infinite re-render loop (template called it per render, creating new watchers)
- Made `refColumnsCache` internal to useFilters, exposed `getRefColumns(path)` instead
- FilterPicker receives `filters: UseFilters` prop directly (no more individual props/emits)
- Removed `schemaId` prop from FilterPicker (useFilters owns it)
- Made `flattenColumns` a computed (`flatRows`) instead of a function called per render
- Removed `defaultFilterIds`, `findColumnForPath`, `crossFilterMap` from public `UseFilters` interface (internal only)

## Phase 15: Move filter visibility into useFilters [x]

### What was done
- All filter state/logic moved from Sidebar into useFilters: visibility, column resolution, cross-filtering, counts, ref pkey stripping
- useFilters takes optional `schemaId`/`tableId` for ref resolution and count fetching
- Sidebar is now ~30 lines of pure display (template + scoped CSS)
- FilterPicker receives `filters` object, calls `filters.toggleFilter()` / `filters.resetFilters()` directly
- Eliminated duplicate `refColumnsCache` (was in both Sidebar and FilterPicker)
- All consumers pass `schemaId`/`tableId` to useFilters options

---

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

## Phase 11: UseFilters object pattern [x]

### What was done
- `useFilters` returns a typed `UseFilters` object instead of individual refs/functions
- Sidebar accepts `filters: UseFilters` prop instead of separate v-models
- All consumers pass single `filters` object
- `FilterValue` union type replaces `any` in `IFilterValue.value`

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
