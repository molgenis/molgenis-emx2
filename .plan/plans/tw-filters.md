# Plan: feat/tw-filters — Port Filter System to tailwind-components + TableEMX2

## Status: Phase 9 complete

## Phase 9: Refactor facet counts [x]

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

#### 9.1 Add `crossFilter` computed map in Sidebar [x]
- Compute `Map<columnId, IGraphQLFilter>` from current filterStates (reuse `buildCrossFilter` logic from useFilterCounts)
- Pass `crossFilter` prop to `FilterColumn` instead of `facetCounts`
- Remove `useFilterCounts` import from Sidebar

#### 9.2 Update Column.vue and Input.vue prop chain [x]
- Replace `facetCounts: Map<string, number>` prop with `crossFilter: IGraphQLFilter`
- Pass through Column → Input → InputRef / InputOntology

#### 9.3 Add count fetching to InputRef [x]
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

#### 9.4 Add count fetching to InputOntology [x]
- After `loadPage()` returns a batch of tree nodes, fire a `_groupBy` query for those nodes
- For parent nodes: use `_agg` with `_match_any_including_children` (existing pattern)
  but only for the parents currently visible, not all
- On expand/load-more: fetch counts for newly loaded nodes only
- On `crossFilter` change: re-fetch counts for all currently visible nodes (debounced)
  - Keep old counts visible until new ones arrive
- Store in local `Map<string, number>` ref
- Pass to TreeNode as `facetCounts`

#### 9.5 Add opacity transition to count badges [x]
- CheckboxGroup.vue and TreeNode.vue: wrap count `<span>` in a transition
- `opacity-50` while counts are loading/stale, `opacity-100` when fresh
- Use `transition-opacity duration-200` for smooth fade
- No spinners, no layout shift

#### 9.6 Remove useFilterCounts composable [x]
- Delete `app/composables/useFilterCounts.ts`
- Delete `tests/vitest/composables/useFilterCounts.spec.ts`
- Remove from Sidebar.vue imports

#### 9.7 Update tests [x]
- Add vitest tests for InputRef count fetching (mock _groupBy responses)
- Add vitest tests for InputOntology count fetching
- Update Sidebar.spec.ts to pass crossFilter instead of facetCounts
- Update Column.spec.ts prop expectations

#### 9.8 Verify
- [x] Counts appear next to filter options (fade in after options load)
- [x] Counts update when other filters change (crossFilter reactivity)
- [x] Old counts stay visible while new counts load (no flicker)
- [x] "Load more" fetches counts only for the new batch
- [x] Ontology expand fetches counts only for visible children
- [x] No N+1 query explosion — one _groupBy per load action
- [x] Small ontologies (< 25 items) still work
- [x] All vitest tests pass
- [x] E2e test passes

### Decisions
1. **Cross-filter excludes current column** — standard faceted search. Users see "what would I get if I also select this?" for multi-select filters.
2. **No spinners** — options show immediately, counts fade in async. Stale counts stay visible during re-fetch.
3. **Separate _groupBy from option query** — simpler code, optimize to single request later if needed.
4. **Debounce count re-fetch** on crossFilter change (~300ms).
5. **String/range filters** don't have option counts. No change needed for them.

### Implementation notes
- `useFilterCounts` composable is SHARED between InputRef and InputOntology (not deleted — repurposed as shared composable)
- Nested dotted column paths (e.g., `hospital.city`) are guarded: counts are skipped since backend `_groupBy` doesn't support nested ref field grouping
- `columnId` prop renamed to `columnPath` throughout the chain to carry the full dotted path
- Ontology parent counts use `_agg` with `_match_any_including_children` (batched N aliases in one GQL request)
- Step 9.6 was adjusted: composable was NOT deleted but refactored into shared composable with `fetchCounts` (leaves) and `fetchParentCounts` (ontology parents)

---

---

## Phase 10: Simplify FilterColumn

### Status: DRAFT

### Problem
Column.vue drills 6 props it doesn't use itself (`depth`, `labelPrefix`, `schemaId`, `tableId`, `columnPath`, `crossFilter`) through to Input → Ref/Ontology. The `:style` CSS var overrides are also unusual for our codebase.

### Current prop flow (too much drilling)
```
Sidebar
  → schemaId, tableId, columnPath, crossFilter, depth, labelPrefix
    → Column.vue (uses only column + labelPrefix, passes rest through)
      → Input.vue (routes by type, passes rest through)
        → Ref.vue / Ontology.vue (calls useFilterCounts with all 4 count-related props)
```

### Key insight: countFetcher object
Instead of drilling `schemaId`, `tableId`, `columnPath`, `crossFilter` separately, Sidebar creates a **countFetcher** object per column that bakes all of these in. Inputs receive one prop and just call a function.

### Design: ICountFetcher

```typescript
interface ICountFetcher {
  fetchRefCounts(options: Map<string, Record<string, unknown>>): Promise<Map<string, number>>;
  fetchOntologyLeafCounts(names: string[]): Promise<Map<string, number>>;
  fetchOntologyParentCounts(names: string[]): Promise<Map<string, number>>;
}
```

**`fetchRefCounts(options: Map<string, Record<string, unknown>>)`**
- Takes a map of `label → keyObject` (e.g. `Map{ "cat" → {name:"cat"}, "dog" → {name:"dog"} }`)
- Internally: extracts key field from objects, builds `_groupBy` query using baked-in `schemaId`, `tableId`, `columnPath`
- Applies current `crossFilter` at call time (reactive — always uses latest)
- Returns `Map<label, count>` — same keys as input map

**`fetchOntologyLeafCounts(names)`**
- Takes leaf ontology term names
- Internally: builds `_groupBy` query (same as fetchRefCounts but keyed on `name`)
- Applies current `crossFilter` at call time
- Returns `Map<name, count>`

**`fetchOntologyParentCounts(names)`**
- Takes parent ontology term names
- Internally: builds `_agg` query with `_match_any_including_children` per name
- Applies current `crossFilter` at call time
- Returns `Map<name, count>`
- Future: replace with `_groupByIncludingChildren` when backend supports it

**No state in the fetcher** — no refs, no loading booleans, no cached maps. Pure async functions. The input component manages its own display state (show options immediately, update counts when promise resolves).

### New prop flow
```
Sidebar
  → creates ICountFetcher per column (bakes in schemaId, tableId, columnPath, crossFilter)
  → passes countFetcher + label to Column
    → Column.vue (uses column, label, removable — passes countFetcher to Input)
      → Input.vue (passes countFetcher by type)
        → Ref.vue: awaits countFetcher.fetchRefCounts(loadedOptions)
        → Ontology.vue: awaits countFetcher.fetchOntologyLeafCounts(leaves)
                       awaits countFetcher.fetchOntologyParentCounts(parents)
```

### Step-by-step plan

#### 10.1 Create `createCountFetcher` factory
- New file: `app/utils/createCountFetcher.ts`
- Takes `{ schemaId, tableId, columnPath, crossFilter: () => IGraphQLFilter }`
- Returns `ICountFetcher` with two async methods
- `fetchRefCounts`: reuses `_groupBy` query logic from current `useFilterCounts.fetchCounts`
- `fetchOntologyLeafCounts`: reuses `_groupBy` logic, keyed on `name`
- `fetchOntologyParentCounts`: reuses `_agg` + `_match_any_including_children` logic from current `useFilterCounts.fetchParentCounts` (future: replace with `_groupByIncludingChildren`)
- crossFilter is read at call time via getter, so always current

#### 10.2 Remove `depth` from Column.vue
Unused prop, just delete.

#### 10.3 Replace `labelPrefix` with `label` prop
Parent already knows the full label. Pass directly.

#### 10.4 Replace 5 drilling props with `countFetcher`
- Sidebar creates `ICountFetcher` per column in computed/function
- Column.vue: remove `schemaId`, `tableId`, `columnPath`, `crossFilter` — add `countFetcher`
- Input.vue: same replacement
- Ref.vue: replace `useFilterCounts` call with `props.countFetcher.fetchRefCounts()`
- Ontology.vue: replace `useFilterCounts` calls with `countFetcher.fetchOntologyLeafCounts()` + `countFetcher.fetchOntologyParentCounts()`

#### 10.5 Replace `:style` hacks with CSS cascade
Add `.filter-sidebar-context` class on Sidebar wrapper:
```css
.filter-sidebar-context {
  --text-color-title-contrast: var(--text-color-search-filter-group-title);
  --text-color-input-description: var(--text-color-search-filter-group-title);
  --text-color-input: var(--text-color-search-filter-group-title);
}
```
Remove per-Column `:style` bindings.

#### 10.6 Delete `useFilterCounts` composable
Logic moved into `createCountFetcher`. Delete:
- `app/composables/useFilterCounts.ts`
- `tests/vitest/composables/useFilterCounts.spec.ts`

#### 10.7 Write tests for `createCountFetcher`
- Test `fetchRefCounts` with mock GraphQL responses
- Test `fetchOntologyLeafCounts` with mock GraphQL responses
- Test `fetchOntologyParentCounts` with mock GraphQL responses
- Test that crossFilter getter is called at invocation time (not creation time)

#### 10.8 Resulting Column.vue props
```typescript
defineProps<{
  column: IColumn;
  label?: string;
  removable?: boolean;
  countFetcher?: ICountFetcher;
}>()
```

### Risks
- Ontology.vue currently debounces count refetch on crossFilter change — with pure async the caller must handle debouncing itself (already does via watch)

### Not in scope
- Changing IGraphQLFilter type (already handles nesting)
- Changing how Sidebar computes crossFilter
- Changing Ref/Ontology option loading logic

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
- [x] `FilterPicker.spec.ts` — simplified `vDropdownStub` to click-toggle1

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
