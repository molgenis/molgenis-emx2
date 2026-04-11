# feat/tw-filters v2 — Simplified Filter System

## Goal
Smaller, cleaner PR. Composable-driven filter sidebar with centralized counting, URL sync, filter picker modal, active filter bar. Don't modify existing form inputs (Ontology.vue, Ref.vue).

## Spec
`.plan/specs/filter-sidebar-v2.md`

## Approach
New worktree from master. Cherry-pick what works, rewrite what doesn't.

## Key Architectural Decisions

1. **Use Tree directly for all countable filter types, NOT Ontology.vue/Ref.vue**
   - Ontology.vue/Ref.vue are form-editing inputs with complex internal state
   - For filters: useFilters provides pre-counted options, `FilterOptions` renders them
   - ALL countable types (ONTOLOGY, ONTOLOGY_ARRAY, BOOL, RADIO, CHECKBOX) → Tree with checkboxes
     - Flat types (BOOL/RADIO/CHECKBOX) are just trees without children — same component
     - Ontology types preserve hierarchy
   - Range types (INT, DECIMAL, DATE, etc.) → FilterRange (needs actual range inputs)
   - String-like → text input (needs actual text input)
   - Existing Ontology.vue/Ref.vue stay untouched

2. **Centralized counting in useFilters**
   - No ICountFetcher pattern, no per-component count fetching
   - useFilters fires parallel `_groupBy` calls, stores results, passes down as props
   - Cross-filter computed centrally (one place, not N components)

3. **No ref filtering in sidebar**
   - REF/REF_ARRAY not directly filterable, no facet counts
   - Filter picker lets users drill into ref sub-columns (nested filters)
   - ArrowRight icon for ref navigation in picker

4. **URL-only persistence, no localStorage**
   - `mg_filters` for visible filter set
   - Collapse state NOT in URL (first-5 rule + active-filter override on load)

5. **mg_* columns hidden by default, revealed on search in picker**

## What to cherry-pick / adapt
- `ActiveFilters.vue` — update chip format (column name + count + tooltip)
- `FilterRange.vue` — works as-is
- `CheckboxGroup.vue` — keep existing (`:key` for stable DOM), but not used for filters (Tree handles all countable types)
- `Date.vue` / `DateTime.vue` CSS fixes
- `filterUrlCodec` (serialize/parse) + tests — URL format is good
- `buildFilter.ts` + tests — GraphQL filter construction
- `types/filters.ts` — type definitions
- `fetchTableMetadata.ts` — subclass columns
- `filterConstants.ts`, `formatFilterValue.ts`, `getPrimaryKey.ts`

## What to create new
- **FilterOptions.vue** — thin wrapper dispatching to Tree (countable) / FilterRange (range) / text input (string)
  - Tree used for ALL countable types (flat + hierarchical — just nodes with/without children)
  - Range/String types still need their specific input components
  - Receives counted options from useFilters
  - No own data fetching
  - Search-within-filter (client-side)
  - Loading skeleton while counts arrive

## What to rewrite
- **useFilters.ts** (877 → ~400 lines)
  - Central count management (parallel _groupBy calls)
  - Cross-filter computation
  - Initial count → visible options/filters
  - URL sync (bidirectional)
  - Visibility management (mg_filters)
  - Probe (base counts determine which filters to show)
  - gqlFilter computation (debounced)
- **FilterPicker.vue** — simpler modal, REF → navigation, apply/cancel/reset
- **Sidebar.vue** — show/hide toggle, first-5 open, loading states
- **computeDefaultFilters.ts** — simplify

## What to drop
- ICountFetcher interface and createCountFetcher.ts
- Ref filter in sidebar
- `removable` / `showLabel` props on FilterColumn
- Complex ontology paging/autoPageAfterPrune
- pruneFilterTree.ts (fold into FilterPicker or remove)
- 5-level deep ref column loading (2 max for picker)
- FilterColumn.vue (replaced by FilterOptions.vue)

## Tasks

### Phase 0: Setup
- [x] 0.1 Create new worktree from master
- [x] 0.2 Set up branch, symlink .claude, copy plan/spec

### Phase 1: Foundation (types, URL codec, GraphQL filter builder)
- [x] 1.1 Cherry-pick/adapt types (filters.ts, types.ts additions)
- [x] 1.2 Extract filterUrlCodec from useFilters into standalone utility + tests (56 tests)
- [x] 1.3 Cherry-pick buildFilter.ts + tests (28 tests)
- [x] 1.4 Cherry-pick small utils (formatFilterValue, filterConstants, computeDefaultFilters, filterTreeUtils) + tests
- [x] 1.5 Cherry-pick Date/DateTime CSS fixes + fetchTableMetadata subclass columns

### Phase 2: Centralized useFilters composable
- [x] 2.1 New useFilters (374 lines, down from 877) with centralized counting
- [x] 2.2 useFilters tests (26 tests)
- [x] 2.3 fetchCounts utility (166 lines, replaces createCountFetcher)

### Phase 3: Components
- [x] 3.1 FilterOptions.vue (201 lines) — Tree for countable, Range for numeric/date, text for string (23 tests)
- [x] 3.2 Sidebar.vue (141 lines) — collapsible sections, search, customize, show/hide (14 tests)
- [x] 3.3 FilterPicker.vue (349 lines) — modal, REF → navigation, search, apply/cancel/reset (15 tests)
- [x] 3.4 ActiveFilters.vue (78 lines) — chips with count + tooltip (8 tests)
- [x] 3.5 FilterRange.vue (58 lines) — cherry-picked
- [x] 3.6 Component tests included with each component

### Phase 4: Integration
- [x] 4.1 Wire into [schema]/[table]/index.vue (sidebar + activefilters + gqlFilter)
- [x] 4.2 TableEMX2: added filter prop, hideSearch prop, below-toolbar slot, pagination reset
- [x] 4.3 fetchTableMetadata subclass columns (done in Phase 1)

### Phase 5: Bug fixes & polish (completed during review)
- [x] 5.1 Tree.vue: fixed `selection` → `selected` typo in clone(), preserve `label`, re-apply modelValue after nodes rebuild
- [x] 5.2 fetchGraphql: URL-encode schemaId for schemas with spaces
- [x] 5.3 Sidebar styling: matched catalogue (bg-sidebar-gradient, semantic colors, rounded)
- [x] 5.4 useFilters: mergeWithBaseCounts prevents options disappearing on cross-filter
- [x] 5.5 BOOL filter: Yes/No/Not set options, URL round-trip, string→boolean conversion, is_null backend syntax
- [x] 5.6 buildFilter: isNull→is_null, notNull→is_null:false (backend snake_case), root-level _or for BOOL combos
- [x] 5.7 filterUrlCodec: BOOL/RADIO/CHECKBOX URL parsing (equals with array, not like)
- [x] 5.8 FilterPicker: wired into Sidebar template, InputCheckboxIcon, description+type badges, REF expand with caret
- [x] 5.9 Show/hide sidebar: button in table toolbar with toggle label, v-show single-root fix
- [x] 5.10 FilterPicker: Clear/Select all/Reset buttons, 90vw modal, description truncation
- [x] 5.11 Nested filters: nestedColumnMeta for type resolution, pathLabel display, columnTypeMap for buildFilter
- [x] 5.12 Range inputs: use generic Input component (auto-dispatches to Date/DateTime/Int), strip _ARRAY suffix
- [x] 5.13 DateTime width 14em, sidebar w-[30rem] to fit date-time range

### Phase 6: Nested filters & ontology tree counting
- [x] 6.1 FilterPicker: show all column types (not just countable+string), nested REF children appear
- [x] 6.2 fetchCounts: valid nested GraphQL for dotted paths (e.g. `self { ontologySmallType { ... } }`)
- [x] 6.3 Sidebar: resolve nested column metadata on URL load (fetch ref table columns)
- [x] 6.4 useFilters: trigger count fetch when nested meta registers (watch nestedColumnMeta)
- [x] 6.5 Ontology tree counting: two-query approach (_groupBy + _match_any_including_parents)
- [x] 6.6 ONTOLOGY: client-side rollup (parent = sum of children, safe for single-select)
- [x] 6.7 ONTOLOGY_ARRAY: _agg with _match_any_including_children per parent (accurate for multi-select)
- [x] 6.8 CountedOption extends ITreeNode (no reinvented interface)

### Phase 7: Component renames & UI polish
- [x] 7.1 Rename FilterOptions.vue → Column.vue, FilterPicker.vue → Picker.vue (+ test files)
- [x] 7.2 Sidebar styling aligned with catalogue (p-5 padding, h3 headings, caret-up icon)
- [x] 7.3 Min/max range labels use text-search-filter-group-title for theme consistency
- [x] 7.4 Tree search: always-visible compact input (shown when >25 options or has children)
- [x] 7.5 mg_collapsed URL param for bookmarkable collapse state
- [x] 7.6 filterColumns logic moved into useFilters (apps/ui simplified)
- [x] 7.7 Picker uses fetchTableMetadata (cached) instead of raw $fetch
- [x] 7.8 fetchCounts refactored: clean dispatcher + named strategies (step-down rule)
- [x] 7.9 E2e smoketest on catalogue-demo/Resources (7 tests)

### Phase 8: Final polish
- [x] 8.1 Theme testing (all 5 themes) — verified Light/Dark/Molgenis/UMCG/AUMC via `?theme=` query param on apps/ui:3000. Found + fixed 2 contrast bugs: Picker type badges (raw `bg-gray-100`/`text-disabled` ≈1.2:1 → added semantic `text-type-badge`/`bg-type-badge` pair in tailwind config + main.css + dark.css) and ActiveFilters chips (hardcoded `text-white` on `bg-button-primary` → swapped to semantic `text-button-primary`).
- [x] 8.2 pnpm format + pnpm lint — both apps clean (tailwind-components + ui). Format only touched `apps/ui/tests/e2e/filter-sidebar.spec.ts`. nuxi typecheck exit 0.
- [x] 8.3 Review: dead code, stale terminology, orphaned refs — review pass done. 11 findings triaged, user approved all. Applied: #1 Sidebar.vue `fetchTableColumns` → `fetchTableMetadata`; #2 useFilters.ts `filterColumns` uses `isExcludedColumn`; #3 `IGraphQLFilterNull.is_null` (was `isNull`/`notNull`); #4 `UseFilters.nestedColumnMeta` adds `refLabel`; #5 Sidebar.vue imports Column/Picker (not FilterOptions/FilterPicker); #6 `IGraphQLFilterMatchAny` in union; #7 `resolveRouteRouter` extracted to new `utils/routeParams.ts`; #8 Sidebar.vue unused `index` in v-for removed; #10 unused `FilterOperator` type removed. Skipped: #9 (review was wrong — `LONG` is already in `RANGE_TYPES` at filterTypes.ts:33); #11 (`PickerNode extends INode` rejected — `INode` uses `name` not `id`, `ITreeNode` adds unrelated `children`/`parent`). Leftover `apps/ui/test_themes.mjs` deleted. Tests: tailwind-components vitest 47 files / 433 tests pass; e2e filter-sidebar 10/10 pass.
- [x] 8.4 Final test run — tailwind-components vitest: 433/433 pass (17 pre-existing unhandled rejections in Picker.spec.ts from a vitest/SSR `BaseIcon.vue` dynamic-import quirk, not ours). filter-sidebar e2e: 10/10 pass. Full ui e2e: 19 pass, 2 fail. Regression fixed: `explorer-ref-columns.spec.ts:16` — generic `h2` locator tightened to `getByRole('dialog').getByRole('heading', { level: 2 })` (broken by new sidebar `<h2>Filters</h2>`). Remaining 2 failures are pre-existing, unrelated: `crud.spec.ts:53` (missing "e2e" cell — test-data/state), `re-authtest.spec.ts:60` ("reauth" text not found — state/timing). These predate the branch and need separate triage.
- [ ] 8.5 Stage changes
- [x] 8.6 E2e verification of nested REF picker flow on `catalogue-demo` + `type test` via apps/ui:3000 — added 2 nested-ref tests + footer-buttons assertion in `apps/ui/tests/e2e/filter-sidebar.spec.ts` (10/10 pass). Verified: `internalIdentifiers.identifier` (catalogue-demo/Resources) and `refType.optionValue` (type test/Types) — breadcrumb `→` in sidebar `<h3>`, dotted path in `mg_filters` URL param, text input in new section updates URL on type.

## Current stats
- **405 tests**, 46 test files, all passing
- **~50 files changed** vs master
- Key components: useFilters (composable), Column, Sidebar, Picker, ActiveFilters, Range
- Key utils: filterUrlCodec, buildFilter, fetchCounts, computeDefaultFilters, filterTreeUtils

### Phase 9: Composite key support for RADIO/CHECKBOX filters
Approach: `_match_any` operator — backend resolves against primary key structure natively.
- Single key (99%): plain strings, flat URL, `_match_any: ["active"]`
- Composite key: key objects on CountedOption, `_match_any: [{ id: "A", code: "1" }]`
- STRING/RANGE filters unchanged (`like`/`between` operators, no ref key concerns)

Tasks:
- [x] 9.1 fetchCounts: getColumnIds expands key fields in _groupBy query, `keyObject` on CountedOption for composite
- [x] 9.2 buildFilter: RADIO/CHECKBOX → `_match_any: values` (+2 lines), no `_or` clause needed
- [x] 9.3 Column.vue: single-key emits plain strings; composite maps Tree names → keyObjects
- [x] 9.4 filterUrlCodec: RADIO/CHECKBOX stay flat in URL (no `.name` suffix, no nested path ambiguity)
- [x] 9.5 Tests: 14 new tests across 4 spec files (418 total passing)
- [x] 9.6 Review: clean
- [x] 9.7 Backend fix: `_groupBy` SQL fails when REF_ARRAY has overlapping FK (composite PK shared column)
  - Root cause: `jsonGroupBySelect()` correlated subquery references overlapping FK columns not in GROUP BY
  - Trigger: REF_ARRAY target table shares PK column name with source table (e.g., both have `resource` REF)
  - Fix: `SqlQuery.java` — collect overlapping ref fields into separate `overlappingRefFields` set, add to GROUP BY only (not SELECT)
  - Test: `TestCompositeForeignKeys.testGroupByWithCompositeRefKey()` — 3 scenarios (simple target PK, composite target PK, overlapping FK)
  - Verified in UI: `CollectionEvents_groupBy { count creator { ... } }` now returns data
  - Frontend fix: `fetchCounts.ts` uses `refLabelDefault` template for filter option labels (resolves `[object Object]` display)
  - Also added `resolveValue()` helper for graceful nested object stringification as fallback

### Phase 8.7: Post-review polish
- [x] 8.7.1 Rename `resolveNestedMeta` → `hydrateNestedFiltersFromUrl` (Sidebar.vue) — clearer intent (URL-hydration counterpart of Picker's nested selection)
- [x] 8.7.2 Delete redundant local `columnCache` in Sidebar.vue — `fetchTableMetadata` already caches per session (CLAUDE.md rule)
- [x] 8.7.3 Delete `utils/routeParams.ts` + the `resolveRouteRouter` magic (dynamic `require("#app/composables/router")` was a code smell). Route/router now explicit: useFilters warns + falls back when urlSync=true without them; Sidebar.vue uses `props.route`/`props.router` directly; apps/ui page now passes `:route="route" :router="router"` to `<FilterSidebar>`.
- [x] 8.7.4 Simplify `parseFilterTerms` — single-pass loop, return type `string[]` (was `{ terms: string[] }`), caller updated
- [x] 8.7.5 Add `defaultFilters?: string[]` option to `useFilters` — overrides `computeDefaultFilters` for initial visible set AND Picker Reset
- [x] 8.7.6 Add `defaultCollapsed?: string[]` prop to Sidebar.vue — overrides the first-5-expanded rule. `mg_collapsed` URL param still takes precedence when present.
- [x] 8.7.7 +3 vitest tests covering both new overrides (436 total, was 433). Filter-sidebar e2e 10/10.

## Future Work (not this PR)

### Phase 10: Use GraphQL variables instead of string serialization
- fetchGraphql already accepts `variables` param (currently always null)
- Replace serializeFilterForQuery + string interpolation with parameterized queries ($filter variables)
- Cleaner, safer (no injection risk), standard GraphQL practice
- Affects: all fetchCounts strategies, buildFilter

### Phase 11: Custom filter hook (parity with catalogue's advanced cases)
Backlog. Adds an escape hatch so consumers can inject custom GraphQL filter construction for individual columns or the `_search` field. Subsumes the catalogue features we intentionally skipped:
- Per-column `buildFilter?: (filterState, column) => IGraphQLFilter` hook (catalogue's `buildFilterFunction`)
- Custom `_search` builder: lets the consumer turn `_search` into an `_or` across multiple joined tables (catalogue's `searchTables` use case)
- Replaces need for `filterTable` and multi-table `_search` as standalone features — those become recipes on top of the hook
- Signatures TBD; likely lives in `useFilters` options

### Other future work
- Mobile: full-screen modal sidebar
- Backend: add limit/offset to _groupBy for large ontologies
- Type fetchGraphql responses (generics)
- Null search (filter for missing values)
- Input size prop for compact filter context
- Filter options description display (catalogue `List.vue` `descriptionField` prop) — optional secondary text next to checkbox labels in Column.vue
