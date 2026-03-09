# Plan: feat/tw-filters — Port Filter System to tailwind-components + TableEMX2

## Status: Phase 9 — Refactor facet counts: per-input, batched with option loading

## Phase 9: Refactor facet counts

### Problem
Current `useFilterCounts` fetches counts centrally in Sidebar for ALL visible filter columns on every filter change. This means:
- Counts are fetched for ALL values of a column, not just the ones visible on screen
- Ontology trees may have hundreds of terms but only show 10 at a time
- Ref inputs paginate at 20 but counts are fetched for everything
- Every filter change triggers N separate `_groupBy` queries (one per visible column)

### Goal
Move count fetching into the input components (InputRef, InputOntology) so counts are fetched **only for the values currently being displayed**, batched with the option-loading query.

### Key insight
`_groupBy` accepts a `filter` parameter. We can add a filter constraint for the specific values being loaded (e.g., `{ species: { name: { equals: ["cat","dog",...20 names] } } }`). This gives us counts for exactly the options being displayed — one `_groupBy` call per "load more" action.

### Architecture change

**Before (centralized):**
```
Sidebar → useFilterCounts → N × _groupBy queries (all values)
       → passes Map<valueName, count> down to Column → Input
```

**After (per-input, batched):**
```
Sidebar → passes crossFilter down to Column → Input
InputRef/InputOntology → on loadOptions/loadPage:
  1. Fetch options (existing query)
  2. Fetch counts for THOSE specific options via _groupBy + value filter
  → display count next to each option
```

### Design: what gets passed down

Sidebar computes the **cross-filter** per column (all active filters EXCEPT that column). This is a plain object, cheap to compute. Pass it as a prop:

```
Sidebar → Column → Input → InputRef / InputOntology
  prop: crossFilter (IGraphQLFilter)
```

Each input component uses `crossFilter` when fetching counts for its loaded options.

### UX: async counts with graceful loading

Counts load async (separate `_groupBy` after options load). No spinners — too noisy.
- **Show options immediately** without counts
- **Fade counts in** when `_groupBy` response arrives (opacity transition)
- **On filter change**: keep showing old counts (stale) while re-fetching, then swap
- Use subtle `opacity-50 → opacity-100` transition to signal freshness

### Step-by-step plan

#### 9.1 Add `crossFilter` computed map in Sidebar
- Compute `Map<columnId, IGraphQLFilter>` from current filterStates (reuse `buildCrossFilter` logic from useFilterCounts)
- Pass `crossFilter` prop to `FilterColumn` instead of `facetCounts`
- Remove `useFilterCounts` import from Sidebar

#### 9.2 Update Column.vue and Input.vue prop chain
- Replace `facetCounts: Map<string, number>` prop with `crossFilter: IGraphQLFilter`
- Pass through Column → Input → InputRef / InputOntology

#### 9.3 Add count fetching to InputRef
- After `loadOptions()` returns a batch of options, fire a `_groupBy` query:
  ```graphql
  query($filter: {Table}Filter) {
    {Table}_groupBy(filter: $filter) {
      count
      {columnId} { {keyField} }
    }
  }
  ```
  Where `$filter` combines:
  - The `crossFilter` from props (excludes this column's own filter)
  - A constraint on the ref table: `{ {keyField}: { equals: [loaded option values] } }`
- Store counts in a local `Map<string, number>` ref
- On "load more": fetch counts for the NEW batch only, merge into existing map
- On `crossFilter` change: re-fetch counts for ALL currently loaded options (debounced ~300ms)
  - Keep old counts visible (stale) until new ones arrive
- Pass local counts to CheckboxGroup as `facetCounts`

#### 9.4 Add count fetching to InputOntology
- After `loadPage()` returns a batch of tree nodes, fire a `_groupBy` query for those nodes
- For parent nodes: use `_agg` with `_match_any_including_children` (existing pattern)
  but only for the parents currently visible, not all
- On expand/load-more: fetch counts for newly loaded nodes only
- On `crossFilter` change: re-fetch counts for all currently visible nodes (debounced)
  - Keep old counts visible until new ones arrive
- Store in local `Map<string, number>` ref
- Pass to TreeNode as `facetCounts`

#### 9.5 Add opacity transition to count badges
- CheckboxGroup.vue and TreeNode.vue: wrap count `<span>` in a transition
- `opacity-50` while counts are loading/stale, `opacity-100` when fresh
- Use `transition-opacity duration-200` for smooth fade
- No spinners, no layout shift

#### 9.6 Remove useFilterCounts composable
- Delete `app/composables/useFilterCounts.ts`
- Delete `tests/vitest/composables/useFilterCounts.spec.ts`
- Remove from Sidebar.vue imports

#### 9.7 Update tests
- Add vitest tests for InputRef count fetching (mock _groupBy responses)
- Add vitest tests for InputOntology count fetching
- Update Sidebar.spec.ts to pass crossFilter instead of facetCounts
- Update Column.spec.ts prop expectations

#### 9.8 Verify
- [ ] Counts appear next to filter options (fade in after options load)
- [ ] Counts update when other filters change (crossFilter reactivity)
- [ ] Old counts stay visible while new counts load (no flicker)
- [ ] "Load more" fetches counts only for the new batch
- [ ] Ontology expand fetches counts only for visible children
- [ ] No N+1 query explosion — one _groupBy per load action
- [ ] Small ontologies (< 25 items) still work
- [ ] All vitest tests pass
- [ ] E2e test passes

### Decisions
1. **Cross-filter excludes current column** — standard faceted search. Users see "what would I get if I also select this?" for multi-select filters.
2. **No spinners** — options show immediately, counts fade in async. Stale counts stay visible during re-fetch.
3. **Separate _groupBy from option query** — simpler code, optimize to single request later if needed.
4. **Debounce count re-fetch** on crossFilter change (~300ms).
5. **String/range filters** don't have option counts. No change needed for them.

---

## Previous phases

## Phase 8: Simplification (review round 2) [x]

### 8.1 Remove mobileDisplay [x]
### 8.2 Make Sidebar self-contained [x]
### 8.3 Remove schemaId prop [x]
### 8.4 Extract ActiveFilters watch [x] — new `resolveFilterLabels.ts` utility + 6 tests
### 8.5 Remove fetchParentCounts [x]
### 8.6 Fix getRefKeyField [x] — checks refLabel first, then refLabelDefault
### 8.7 Fix getSubclassColumns [x] — async, uses fetchMetadata, added inheritName to GQL
### 8.8 Clean up story files [x]

### Resolved questions
1. Sidebar expose columns? → NO. No caller reads them back.
2. getRefKeyField default? → Check `refLabel` first (overrides `refLabelDefault`). Keep `"name"` as defensive fallback.
3. fetchParentCounts in production? → NO. Only in this PR. Safe to remove.

## Phase 7: Review Fixes [x]

### 7.1 Remove fake/tautological tests [x]
- [x] `FilterPicker.spec.ts` — "sorts columns alphabetically": hardcoded expected order
- [x] `useFilters.spec.ts` — "handle empty objects": fixed to expect `""`
- [x] `useFilters.spec.ts` — "deeply nested objects": exact expected value
- [x] `FilterPicker.spec.ts` — "checked/unchecked checkboxes": specific column IDs

### 7.2 Remove unnecessary mocks [x]
- [x] `useFilterCounts.spec.ts` — removed dead `#imports` and `#app/composables/router` mocks
- [x] `FilterPicker.spec.ts` — simplified `vDropdownStub` to click-toggle

### 7.3 Fix copy-pasted test function [x]
- [x] Extracted `computeDefaultFilters` to `app/utils/computeDefaultFilters.ts`
- [x] `FilterPicker.spec.ts` imports real function, local copy removed
- [x] `Sidebar.vue` imports from utility

### 7.4 Add missing tests [x]
- [x] Create `getSubclassColumns.spec.ts` — 7 tests (empty, not found, direct, recursive, dedup, sourceTableId, empty array)
- [x] `FilterPicker.spec.ts` — FILE column exclusion test
- [x] `Column.spec.ts` — 4 removable/remove tests
- [x] `buildFilter.spec.ts` — notNull, isNull, nested notNull tests
- [x] `useFilterCounts.spec.ts` — SELECT/RADIO countable types test
- [x] Create `Sidebar.spec.ts` — 31 tests (rendering, smart defaults, toggle, reset, URL sync, nested filters, mobile, search)

### 7.5 Fix stale story documentation [x]
- [x] `Column.story.vue` — removed `collapsed` prop and collapse/caret items
- [x] `Sidebar.story.vue` — replaced "grouped by heading" with "sorted alphabetically"
- [x] `Range.story.vue` — removed "stale vertically" mobile claim

### 7.6 Code fixes [x]
- [x] `extractStringKey` — returns `""` for undefined/empty objects instead of `"undefined"`
- [x] FILE exclusion already in `EXCLUDED_TYPES`, test now covers it

### Open questions (need product owner input)
1. Column.vue: Is collapse/expand planned? Story documents it but source doesn't have it => NO. we have flat selection of nested elemnts.
2. FilterPicker: Should columns be grouped by heading? Story documents it but not implemented => NO. Remove from story.
3. Range: Should it stack vertically on mobile? Story documents it but uses flex. NO => leave as is.
4. `like_or`/`like_and` operators: Are they used anywhere? Test or remove? YES: the string inputs should use this (except UUID)
5. `computeDefaultFilters`: Should it be exported from Sidebar.vue for testability? => YES, good idea.
6. `serializeFilterValue({operator:"in", value:{}})` returning `"undefined"` — bug or intentional? => BUG, can you interrogate?

## Phase 1-6 [x]

See git history for details. All phases completed.
