# feat/tw-filters — Filter System for tailwind-components

## Summary

New filter system for the tailwind-components library: composable-driven, URL-synced, with faceted counts and smart defaults. Replaces the old catalogue-specific filter implementation with a reusable, schema-driven approach.

**61 files changed** (against origin/master): 16 new source files, 6 stories, 14 test files, 2 shared test fixtures, 8 modified existing components, 6 test quality fixes, plus config/CSS/types.

**382 vitest tests, all passing** (47 test files).

---

## Reviewer Checklist

### Core: useFilters composable (`app/composables/useFilters.ts`)
- [ ] Returns typed `UseFilters` object with all filter state + methods
- [ ] URL sync: bidirectional (filter↔URL), preserves non-filter params (page, view)
- [ ] Debounced `gqlFilter` computed with JSON comparison guard (prevents spurious updates)
- [ ] 5 operators: `equals`, `like`, `between`, `notNull`, `isNull`
- [ ] `like` always uses AND semantics for space-separated terms
- [ ] Serialization round-trip: filter state → URL string → parse → GraphQL filter
- [ ] Tests: `composables/useFilters.spec.ts` (~25 composable tests) + `utils/filterUrlCodec.spec.ts` (51 pure-function tests)

### Filter components (`app/components/filter/`)
- [ ] **Sidebar.vue** (~30 lines) — pure display wrapper, receives `UseFilters` prop
- [ ] **FilterPicker.vue** — column toggle dropdown, excludes HEADING/SECTION/FILE/mg_* columns
- [ ] **Column.vue** — dispatches to correct input type (text/range/bool), operator derivation from column type
- [ ] **Range.vue** — min/max tuple input with slot-based API
- [ ] **ActiveFilters.vue** — chip display with remove/clearAll events
- [ ] Stories exist for all 5 components + useFilters composable

### Filter utilities (`app/utils/`)
- [ ] **buildFilter.ts** — converts filter state to GraphQL filter syntax (nested refs, ontology `_match_any_including_children`)
- [ ] **createCountFetcher.ts** — `ICountFetcher` factory: ref counts via `_groupBy`, ontology counts via `_groupBy` + `_agg`, base counts (no cross-filter) for hiding empty options
- [ ] **computeDefaultFilters.ts** — smart defaults: ontology first, then refs, max 5
- [ ] **formatFilterValue.ts** — display formatting for active filter chips
- [ ] **resolveFilterLabels.ts** — async label resolution for nested ref paths
- [ ] **filterConstants.ts** — shared constants

### Filter types (`types/filters.ts`)
- [ ] `IFilterValue`, `FilterOperator`, `IGraphQLFilter`, `UseFilters` interface, `ICountFetcher` interface

### Modified existing components
- [ ] **Ref.vue** — facet counts, base count hiding (baseCount=0 hidden, crossFilter count=0 shown), search overrides hiding
- [ ] **Ontology.vue** — tree pruning by base counts, auto-paging after pruning, parent counts via `_agg`
- [ ] **TreeNode.vue** — simplified (just renders what it's given), loading spinner
- [ ] **CheckboxGroup.vue** — count display + loading spinner
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
- [ ] **Ref.spec.ts** — moved createCountFetcher test to proper utility spec
- [ ] **Ontology.spec.ts** — expanded test coverage for tree pruning + auto-paging

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

### Task 3: Prevent filter option flickering during count reloads
**Priority: highest (most visible UX issue)**

Root causes identified:
1. `listOptions` computed in Ref.vue (line 71-82) recreates full array on every count Map change — even though only counts changed, not the options
2. CheckboxGroup.vue (line 56) missing `:key` on v-for items — Vue can't track which DOM element maps to which option
3. Ontology `pruneByBaseCounts()` (line 695-721) mutates tree structure on EVERY count refresh — should only prune on initial base count load, not on cross-filter updates
4. `countsLoading` boolean is global — spinner shows on ALL items, not per-item
5. `createCountFetcher` always returns new Map instances — triggers reactive cascade

Implementation:
- **Fix 1** (quick): Add `:key="option.value"` to CheckboxGroup v-for (line 56)
- **Fix 2**: Separate Ref.vue `listOptions` into stable option list + reactive count overlay. Base count filtering should be a separate computed that only reruns when baseCounts change, not on cross-filter count changes.
- **Fix 3**: In Ontology, only prune on initial base count load. Cross-filter count updates should only change count numbers, never mutate tree structure.
- **Fix 4**: Per-item loading state (or no spinner at all — just update count in place)

Files: `Ref.vue`, `Ontology.vue`, `CheckboxGroup.vue`, `TreeNode.vue`, `createCountFetcher.ts`

### Task 1: Click "X options hidden" to reveal all
**Priority: medium (quick win after Task 3)**

- **Ref.vue**: Add `showAllOptions = ref(false)`. Modify `listOptions` filter (line 75) to: `if (searchTerms.value || showAllOptions.value) return true`. Make the "X options hidden" message a clickable button that toggles `showAllOptions`.
- **Ontology.vue**: Add `showAllHidden = ref(false)`. Skip `pruneByBaseCounts()` when true (line 689 condition). Make message clickable. When toggled back off, re-prune. Note: Ontology already has per-node `showingAll` via `showAllChildrenOfNode()` — the new toggle is a global override.

Files: `Ref.vue`, `Ontology.vue`

### Task 2: Removing filter from sidebar should clear filter state
**Priority: investigate — code appears correct**

Analysis shows `toggleFilter()` (useFilters.ts line 548) already calls `removeFilter()` when hiding. The full chain: toggleFilter → removeFilter → setFilter(null) → delete from Map → URL sync → gqlFilter recompute.

Need to verify:
- Write a red-green test reproducing the reported behavior
- Check alternative paths: FilterPicker toggle, ActiveFilters "clear all", direct URL manipulation
- If test passes, this may be a perceived issue (flicker makes it look like state persists) — would be fixed by Task 3

Files: `useFilters.ts`, `useFilters.spec.ts`

## Future Work

- **Type fetchGraphql responses** — `fetchGraphql` returns `Promise<any>`. Fix: add generic typing `fetchGraphql<T>()`, define response interfaces (`IGroupByResponse`, `IAggResponse`, `IOntologySizeProbe`), type test mock data. This catches code↔mock drift at compile time.
- **E2e smoke tests for API contracts** — 9 test files (101 tests) mock backend responses with no type safety. Add 3 e2e smoke tests (filter sidebar, ontology input, ref facet counts) to anchor response shapes against real backend.
- **Backend: add limit/offset to _groupBy** — `_groupBy` GraphQL field lacks `limit`/`offset` parameters. The backend dataFetcher already handles them — only the GraphQL field definition needs updating (`GraphqlTableFieldFactory.tableGroupByField()`, add `.argument()` calls). Enables paginating through "terms with records" for large flat ontologies (10,000+ terms).
- **Consolidate test mocks** — shared `$fetch` stub (duplicated in Sidebar/Column specs), standardise mockRoute/mockRouter patterns, simplify gqlFilter assertions in useFilters.spec.ts that overlap with buildFilter.spec.ts.
