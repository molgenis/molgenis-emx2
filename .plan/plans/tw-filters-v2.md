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
- [x] 1.5 Cherry-pick Date/DateTime CSS fixes

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
- **449 tests**, vitest all passing
- **~50 files changed** vs origin/master
- Key components: useFilters (composable), Column, Sidebar, Picker, ActiveFilters, Range
- Key utils: filterUrlCodec, buildFilter, fetchCounts, computeDefaultFilters, filterTreeUtils
- TableEMX2 now owns filter system (enableFilters prop)

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
- [x] 8.7.8 Sidebar narrowed from w-[34rem] (544px) to w-80 (320px); Range.vue inputs changed from horizontal side-by-side to vertical stacked rows (Min above Max).

### Phase 8.8: Tree expand UX improvements
- [x] 8.8.1 Tree.vue: auto-expand all nodes when total count ≤25 (count all nodes in `createNodeMap`, set `expanded: true` on parents)
- [x] 8.8.2 Tree.vue: preserve expand state across node rebuilds — when `props.nodes` watcher fires, carry over `expanded` from old nodeMap before replacing
- [x] 8.8.3 Tests: Tree.spec.ts — small tree auto-expands; expand state survives node prop change (3 tests, 449 total passing)
- [ ] 8.8.4 Verify in apps/ui with ontology filters (small + large trees)

### Phase 8.10: ActiveFilters chip labels mismatch sidebar labels
Repro: `/catalogue-demo/Collections?mg_filters=id,hricore&hricore=true|false|_null_` — sidebar Column shows "Yes"/"No"/"Not set" for BOOL, ActiveFilters chips show raw `true` / `false` / `_null_`. Same class of bug likely affects ONTOLOGY (shows `name` instead of fetched `label`) and RADIO/CHECKBOX with refLabel templates.

Root cause: `formatFilterValue(IFilterValue)` at `apps/tailwind-components/app/utils/formatFilterValue.ts` has no access to column type or the per-value label map. Labels are resolved once in `fetchCounts.ts` (e.g. BOOL Yes/No/Not set at lines 108-112; ontology label at 257; RADIO/CHECKBOX refLabel at 164-168) and passed to the sidebar Tree — but never flow to the ActiveFilters chip builder in `useFilters.ts:196-207`.

Approach: one source of truth for value→label resolution, applied everywhere chips/filters are displayed. Not shipped yet — change signatures cleanly, no back-compat shims.

Design:
- New required signature: `formatFilterValue(filterValue, column, optionLabels)` where `optionLabels: Record<string, string>` is value → user-facing label. No optional args.
- `useFilters.activeFilters` owns label-map construction per column and passes it in:
  - BOOL → `{ true: "Yes", false: "No", _null_: "Not set" }` constant (extract to `filterConstants.ts`, reuse in fetchCounts.ts BOOL strategy to kill the duplicated literal).
  - ONTOLOGY / ONTOLOGY_ARRAY / RADIO / CHECKBOX → flatten `counts.value[columnId]` tree (root + all descendants) into `{ name: label }`. Composite-key RADIO/CHECKBOX use the JSON-stringified keyObject as the map key to match how the filter value is stored.
  - STRING / TEXT / RANGE → empty map; formatter falls through to raw value.
- Helper: `buildLabelMap(column, countsForColumn)` in a new `utils/filterLabels.ts`, unit-tested in isolation. Step-down rule — keeps `useFilters.activeFilters` readable.

Tasks:
- [x] 8.10.1 `BOOL_LABELS` constant extracted to `filterConstants.ts`; `fetchCounts.ts` BOOL strategy now imports it (both success + catch paths).
- [x] 8.10.2 `utils/filterLabels.ts` with `buildLabelMap(column, counted)`. BOOL → `BOOL_LABELS`; ontology types → tree flatten; RADIO/CHECKBOX → flat + composite-key via `JSON.stringify(keyObject)`; REF/STRING/RANGE → `{}`. 15 new unit tests in `filterLabels.spec.ts`.
- [x] 8.10.3 `formatFilterValue(filterValue, column, optionLabels)` — 3 required params. `resolveLabel()` helper: `optionLabels[JSON.stringify(v)]` for objects, `optionLabels[String(v)]` for primitives. 7 new BOOL + ONTOLOGY tests.
- [x] 8.10.4 `useFilters.activeFilters` builds labelMap per column via `buildLabelMap(column, counts.value[columnId])` and passes to `formatFilterValue`. `resolveColumn()` helper added for step-down.
- [x] 8.10.5 Migrated all `formatFilterValue` callers — `FilterSystem.story.vue` also had a duplicated `activeFilters` computed that was migrated. No old-signature call sites remain.
- [x] 8.10.6 `ActiveFilters.spec.ts` +3 tests: BOOL chip renders Yes/No/Not set, ONTOLOGY chip renders label (not name).
- [ ] 8.10.7 E2e `filter-sidebar.spec.ts` repro-URL test — not added yet; live Playwright verify covered it, but codifying as an automated e2e still pending.
- [x] 8.10.8 Spec row added under "Active Filter Bar".
- [x] 8.10.9 Playwright live verify on apps/ui:3000 with the repro URL: chip shows "hricore 3" + hover tooltip "Yes / No / Not set"; no raw "true" / "false" / "_null_" anywhere. Sidebar unchanged (Yes/No/Not set with counts). Screenshot `/tmp/bool_final.png`.
- [x] 8.10.10 `pnpm format && lint && test-ci`: 474/474 pass (was 449, +25 new), exit 0, no new unhandled rejections.

### Phase 8.9: Picker modal scroll bug
Repro: catalogue-demo/Collections → Customize → select a filter → viewport scrolls down, footer (Apply) goes off-screen, large white area visible. Must scroll up to continue.

Root cause: `apps/tailwind-components/app/components/Modal.vue:91` outer container has `h-[95vh] flex flex-col overflow-auto`. Inner `#modal-content` (line 132) also has `overflow-y-auto`. With two nested scroll owners, when tree auto-expand grows Picker content past 95vh the OUTER wrapper scrolls (not the inner content div), pushing the fixed-height footer below the viewport. Header (line 104) and footer (line 139) also carry stray `overflow-y-auto` they don't need.

Tasks:
- [x] 8.9.1 Modal.vue outer wrapper — dropped `overflow-auto`. Inner `#modal-content` is sole scroll owner.
- [x] 8.9.2 Modal.vue header/subtitle/footer — dropped stray `overflow-y-auto` (flex-none regions, layout-only).
- [x] 8.9.3 Playwright verify on /catalogue-demo/Collections via apps/ui:3000 — Customize opens Picker with correct footer (Select all/Clear/Reset · Cancel/Apply), `window.scrollY` stays 0, Apply button bottom 690 ≤ innerHeight 720 after interaction. Screenshot recorded.
- [x] 8.9.4 Regression scan of Modal consumers: EditModal.vue and DeleteModal.vue override the header slot with their own markup (each carries its own `overflow-y-auto` on `flex-none` header — unaffected by our change). All other consumers use short default headers/footers — safe.
- [x] 8.9.5 Spec row added under "Filter Picker (Modal)": "Long filter lists scroll inside modal content; footer stays pinned; page does not scroll".
- Tests: tailwind-components vitest 449/449 pass; format + lint clean.

### Phase 10: Embed filters in TableEMX2
- [x] 10.1 Add `enableFilters` prop (default true) to TableEMX2.vue
- [x] 10.2 TableEMX2 internally creates useFilters, renders FilterSidebar + ActiveFilters + toggle button
- [x] 10.3 Simplified [table]/index.vue — removed all filter plumbing (useFilters, FilterSidebar, ActiveFilters, sidebarVisible)
- [x] 10.4 Story updated with `:enable-filters="false"`
- [x] 10.5 Template deduplication — single table block, conditional flex wrapper (no copy-paste)
- [x] 10.6 Fixed fetchCounts.ts nested filter 400 error (use setNestedValue for dot-path column IDs)
- [x] 10.7 Fixed Picker.vue icon (check-box → checklist)
- [x] 10.8 Fixed filter-sidebar.spec.ts timing race (waitForTimeout → waitForURL)
- [x] 10.9 Removed Date.vue/DateTime.vue scoped width CSS (consumer controls width)
- [x] 10.10 Removed getSubclassColumns (not needed for this PR)
- [x] 10.11 Red-green test: two e2e tests in filter-sidebar.spec.ts: "pagination count updates when filter applied" (hide-below-pageSize case) and "pagination OF count decreases but stays visible when filter leaves >pageSize results" (>pageSize case, catalogue-demo/Resources with Netherlands country filter: 109 records/11 pages → 15 records/2 pages)
- [x] 10.12 Diagnose + fix pagination count bug: backend GraphQL bug — `_agg(filter:$filter)` ignored filter when `$filter` was shared between `Table(filter:$filter)` and `Table_agg(filter:$filter)` in one query, specifically with ontology `_match_any_including_children`. **Fixed on this branch (commit 9fd9e133a):** `GraphqlTableFieldFactory.java:672` now defensive-copies the variable map (`new LinkedHashMap<>((Map) entry.getValue())`) before mutating, so `_match_any_including_children` no longer corrupts the shared `$filter`. Backend test `testAggFilterNotMutatedByMatchIncludingChildren` in `TestGraphqlSchemaFields.java` validates. Frontend `$aggFilter` workaround removed — `fetchTableData.ts` is back to single `$filter` variable.
- [x] 10.13 Clean e2e verification: full ui e2e suite — filter-sidebar (9 pass + 1 pre-existing known failure on "type test schema"), admin-page, crud, view-row-details, explorer-ref-columns all green. Cleaned up scratch debug files (debug-*.spec.ts, PNGs, check_page.mjs).

### Phase 13: Direct REF / REF_ARRAY / REFBACK filtering

**Goal:** allow users to select a REF / REF_ARRAY / REFBACK column itself (not just drill into its children) and filter on distinct FK target rows. Reverses the "no ref filtering in sidebar" decision from §3 of Architectural Decisions and the matching spec rows.

**User answers (2026-05-26):**
- Sidebar render: distinct FK target rows as checkboxes (categorical), same as RADIO/CHECKBOX REF.
- Picker row UX: checkbox and expand caret are independent — selecting the ref AND drilling into a child both work, ANDed in sidebar.
- High-cardinality: reuse the existing saturated-flag pattern (≥500 rows) + the in-tree search.
- Scope: REF + REF_ARRAY + REFBACK.

**Approach.** Treat REF / REF_ARRAY / REFBACK as countable types with the same composite-key machinery that Phase 9 added for RADIO/CHECKBOX REF. They already have `refTableId`, so they go through the existing `_or: [{key:{equals:val}},...]` / `_match_any` paths in `buildFilter`. fetchCounts already knows how to expand key fields via `getColumnIds` and build `keyObject` on `CountedOption`. The Picker's only structural change is making the ref row's checkbox active alongside the existing caret.

**Reuse principle (per user 2026-05-27):** ideally NO new strategy code. Wire REF/REF_ARRAY/REFBACK through the existing `fetchFlatGroupBy` strategy (fetchCounts.ts:187) — same `GROUP_BY_SATURATION_THRESHOLD = 500` (line 87), same "too many options, please search" hint (Tree.vue:368–373), same `refLabelDefault` template, same `keyObject` composite-key path. New code only allowed for: (a) the type-list constants (`COUNTABLE_TYPES` etc.) and (b) the Picker row UX (checkbox + caret on same row). Anything else = re-check whether it's already there.

**Picker row click semantics (per user 2026-05-26):** label click toggles the checkbox (matches non-ref rows); caret click only expands; ref + child filters AND together (consistent with all other filter combinations).

**REFBACK scope (per user 2026-05-26):** include if backend `_groupBy` works on REFBACK; otherwise split into Phase 13b and ship REF + REF_ARRAY now (task 13.0 below decides).

**Tests to update (no new test infrastructure):**
- `computeDefaultFilters.spec.ts:20–33` "returns BOOL, CHECKBOX, RADIO but not REF types" — flip: REF/REF_ARRAY now in result
- `Picker.spec.ts:115–122` "shows countable columns (ONTOLOGY, BOOL) with checkboxes" — rename + add REF assertion
- `Picker.spec.ts:320–328` "shows REF columns with arrow indicator" — keep arrow + add checkbox assertion (both present, independent)
- `Picker.spec.ts:356–380` "Select all button selects all selectable columns" — verify count includes REF
- `useFilters.spec.ts:233–244` "defaults to ontology/bool columns" — add REF column to fixture, verify it's now in defaults
- Spec line 117 ("No facet counting on REF/REF_ARRAY") — flip; line 29 navigable-types — update

**Tasks:**
- [x] 13.0 Probe: REFBACK `_groupBy` confirmed SUPPORTED. Evidence: `GraphqlTableFieldFactory.java:259-304` adds `_groupBy` field for REFBACK in same branch as REF_ARRAY; `SqlQuery.java:627-637` `column.isReference()` covers REFBACK; `TestRefBack.java:119-122` already exercises ref agg path. REFBACK stays in Phase 13 scope.
- [x] 13.1 RED: 25 new RED tests across 7 files, all failing for correct feature-not-implemented reasons (not structural errors). Files: computeDefaultFilters.spec.ts (1), fetchCounts.spec.ts (6), buildGqlFilter.spec.ts (4), filterUrlParams.spec.ts (4), Column.spec.ts (5), Picker.spec.ts (4), useFilters.spec.ts (1). Existing GREEN tests untouched in same files.
  - fetchCounts.spec.ts: REF column → `_groupBy` query with key field expansion, `keyObject` on CountedOption, refLabelDefault used for label
  - fetchCounts.spec.ts: REF_ARRAY column → same plus array-style aggregation if needed (verify against backend behavior — may use `_agg { count, <ref>_groupBy }` pattern from existing RADIO_ARRAY/CHECKBOX work)
  - buildFilter.spec.ts: REF single-key → `_match_any: [val]`; REF composite-key → `_or:[{...equals:val},...]` (mirror existing RADIO behavior)
  - Column.spec.ts: REF/REF_ARRAY/REFBACK route to FilterTree (not FilterText, not "navigable" no-op)
  - Picker.spec.ts: ref row exposes both a clickable checkbox (toggles selection in `selected` set) and a clickable caret (toggles expand); checkbox state is independent of expand state and independent of any child selection
  - filterUrlCodec.spec.ts: REF round-trips flat (single-key) or JSON (composite-key), same as RADIO/CHECKBOX
- [x] 13.2 filterTypes.ts: added REF/REF_ARRAY/REFBACK to `COUNTABLE_TYPES`, `DEFAULT_FILTER_TYPES`, and `REF_FILTER_TYPES` (so `treeSelectionToFilterValue` handles composite keys).
- [x] 13.3 fetchCounts.ts: extended RADIO/CHECKBOX dispatcher branch to include REF/REF_ARRAY/REFBACK. Added `useGroupByField` param to `fetchFlatGroupBy` — REF types build field selection as `${col}_groupBy { count ${col} { ${keyLeaf} } }`. Reuses existing saturation (`GROUP_BY_SATURATION_THRESHOLD = 500`), refLabelDefault template, keyObject composite-key path. No new strategy added.
- [x] 13.4 buildGqlFilter.ts: extended RADIO/CHECKBOX branch to include REF/REF_ARRAY/REFBACK for direct (top-level) columns. Plain strings → `equals: [{ name: v }]`; composite-key objects → `_or:[{key:{equals:val}},...]`; single-key objects → `{ key: { equals: [...] } }` (preserves old unwrap behavior).
- [x] 13.5 Picker.vue: added `expandable` to `PickerNode`. `buildRefPickerNode` now sets `selectable: true` + `expandable: true`. New template branch for `node.selectable && node.expandable` renders checkbox (label-wrapped) AND a separate caret `<button>` — independent. Label-click toggles checkbox (matches non-ref rows); caret-click only expands. Confirmed: arrow "→ tableName" still shown.
- [x] 13.6 Column.vue: added `onTreeChange` handler that applies `treeSelectionToFilterValue` when Tree emits raw string arrays (covers single-key REF emit). FilterTree dispatch was automatic via COUNTABLE_TYPES inclusion (13.2).
- [x] 13.7 useFilters.ts: no changes needed. Existing `buildLabelMap` flatten for RADIO/CHECKBOX already covers REF via the same `keyObject` JSON-stringify key path. Verified by 40 existing useFilters tests staying GREEN.
- [x] 13.8 filterUrlParams.ts: REF/REF_ARRAY/REFBACK with `refTableId` + plain string values → flat URL key (same as RADIO/CHECKBOX, no `.name` suffix). `parseFilterValue` treats refTableId + no refField as MULTI_VALUE_TYPE → returns plain strings. Backward compat preserved for REF without `refTableId`.
- [x] 13.9 ActiveFilters: no changes needed — Phase 8.10 label-map flow handles REF automatically via the same RADIO/CHECKBOX path. No new spec row needed (existing line 187 already covers "refLabel" chip rendering).

**Result: 25/25 RED tests GREEN, 586/586 total tests pass, no regressions. Production files modified: filterTypes.ts, fetchCounts.ts, buildGqlFilter.ts, filterUrlParams.ts, Column.vue, Picker.vue. Sidebar.vue + nuxt.config.ts intentionally NOT staged.**
- [x] 13.10 Spec updates (lead, in `.plan/specs/filter-sidebar-v2.md`):
  - Flip "No facet counting on REF/REF_ARRAY" row (line 117) to: "REF/REF_ARRAY/REFBACK directly filterable via facet counts; saturation cap at ≥500 with search fallback"
  - Update "Navigable types" line 29 — REF/REF_ARRAY/REFBACK are now both countable AND navigable (selectable checkbox + expandable caret)
  - Add Picker row: "REF/REF_ARRAY/REFBACK rows expose checkbox AND caret independently; selecting ref + selecting children are AND'd in sidebar"
  - Add Column row: "REF/REF_ARRAY/REFBACK renders FilterTree with FK target rows as checkboxes; uses refLabel templates for option labels"
  - Add Sidebar row: "Selecting a REF column in Picker adds a sidebar section with checkboxes of distinct FK target rows + counts"
  - Add Facet Counting row mirroring lines 136–137 for REF types
- [x] 13.11 E2e (filter-sidebar.spec.ts): "direct REF column filtering exposes checkbox and caret in modal" — navigates to catalogue-demo/Resources on apps/ui:3001, opens Customize, finds "contact point" REF column, verifies checkbox + caret both render, selects checkbox, Applies, asserts URL `mg_filters` contains `contactPoint` and modal closes. 63 lines added. PASS.
- [x] 13.12 GREEN: 25/25 RED → GREEN. 586/586 total tests pass. Reuse principle held — no new strategy, no new saturation code. See 13.2-13.9 above for per-task summary.
- [x] 13.13 Review: 2 blockers raised. (a) `Well` columnType badge on Picker rows: FALSE positive — badge predates Phase 13 (spec rows 157, 160), Phase 13's third branch correctly mirrors the existing pattern. (b) REFBACK missing from `DEFAULT_FILTER_TYPES`: TRUE — fixed in follow-up: added `"REFBACK"` to `filterTypes.ts:59-67` + extended `computeDefaultFilters.spec.ts` fixture/assertion (renamed "returns BOOL, CHECKBOX, RADIO, REF, REF_ARRAY, REFBACK"). 586 tests still GREEN. Minor `isDirectColumn` dead-complexity flagged in buildGqlFilter.ts:94 → DEFERRED to next touch of that file (not a Phase 13 introduction).
- [x] 13.14 pnpm format + lint clean on both apps. nuxi typecheck exit 0. 14 files staged (6 production + 7 vitest + 1 e2e), Sidebar.vue + nuxt.config.ts intentionally unstaged. Final test run: 53 files / 586 tests GREEN.

### Phase 13.5: Fix REF query shape + add SELECT/MULTISELECT

**Bug discovered post-merge of Phase 13 (live test on apps/ui):** counting `contactPoint` (REF column on `Resources`) returns `Validation error (FieldUndefined@[Resources_groupBy/contactPoint_groupBy])`.

**Root cause:** Phase 13 GREEN added a new `useGroupByField` branch in `fetchCounts.ts:175-177` that wraps REF columns as `<col>_groupBy { count, <col> { key } }` inside `Resources_groupBy { ... }`. But `<col>_groupBy` is a ROOT field on `Resources` (per `GraphqlTableFieldFactory.java:295-303`), NOT a sub-field of `ResourcesGroupBy`. The RADIO/CHECKBOX REF path (which works) uses the simpler `Resources_groupBy { count, <col> { key } }` shape — no nested `_groupBy`. The Phase 13 RED test mocked the wrong shape, so GREEN passed against the mock but never matched real backend. E2e test only checked URL update, not table filtering — also missed it.

**Scope expansion (per user 2026-05-27):** add `SELECT` (extends REF) and `MULTISELECT` (extends REF_ARRAY) to the same direct-filtering treatment. After the fetchCounts fix, these are pure type-list additions.

**Tasks:**
- [x] 13.5.1 RED-flip: 4 fetchCounts REF tests flipped (wrong `<col>_groupBy` shape → correct `Resources_groupBy { count, <col> { key } }`). SELECT+MULTISELECT cases added across fetchCounts/buildGqlFilter/filterUrlParams/computeDefaultFilters/Column/Picker/useFilters specs.
- [x] 13.5.2 GREEN: `useGroupByField` parameter + branch deleted from `fetchFlatGroupBy`. `isRefType` callsite removed. SELECT + MULTISELECT added to `COUNTABLE_TYPES`, `DEFAULT_FILTER_TYPES`, `REF_FILTER_TYPES`, fetchCounts dispatcher, buildGqlFilter REF branch, filterUrlParams `isDirectRefType` + serialize branch.
- [x] 13.5.3 E2e tightened: "direct REF" test now asserts the new sidebar `<h3>` heading appears after Apply (proves the count query worked, not just URL update).
- [x] 13.5.4 Format + lint clean. 601/601 tests GREEN (+15 net from 586). 12 files staged (4 production + 7 vitest + 1 e2e).

### Phase 13.6: Picker row layout polish

**User request (2026-05-27):** caret on LEFT of REF row (before checkbox); Well badge inline after column label (not floated to row's far right via `justify-between`). Per user diff, caret was deleted from the `selectable && expandable` branch — needs to be re-added on the left. Apply layout consistently across all 3 branches (selectable+expandable, !selectable, plain selectable).

- [x] 13.6.1 Picker.vue: caret re-added as sibling before `<label>` in `selectable && expandable` branch (with `@click.stop` so it doesn't toggle the checkbox). Well moved inline (last child) inside `<label>` in branches 1+3, and inside the `<button>` in branch 2. `justify-between` dropped. Caret stays independent of checkbox (toggleExpand on caret click only).
- [x] 13.6.2 Picker.spec.ts: 21/21 GREEN, no selector changes needed.
- [x] 13.6.3 Format + lint clean. 601/601 tests still pass.

### Phase 13.7: Picker row element order

User-specified order: `checkbox, label, optional → entity, optional caret, Well`. Caret moves from leftmost (13.6) to between `→ entity` and Well.

- [x] 13.7.1 Picker.vue: order is now `checkbox, label, → entity, caret, Well` in branch 1; `label, → entity, caret, Well` in branch 2; `checkbox, label, [description block], Well` in branch 3. Caret kept as `@click.stop` sibling in branch 1.
- [x] 13.7.2 Picker.spec.ts 21/21 GREEN; no selector adjustments needed.
- [x] 13.7.3 Format + lint clean. 601/601 tests pass.
- [x] 13.7.4 Left-align follow-up: removed `flex-1`/`min-w-0`/`w-full` so everything packs left, no flex-1 gap pushing Well right. 21/21 Picker tests still GREEN.

### Phase 13.8: Tree show-more label + search visibility

**Bug repro (apps/ui:3001 catalogue-demo/Resources):** "people involved" filter shows 1 option, click "Show more (+50)" → reveals 7 (1 non-zero + 6 zero-count); but label said +50 when only 6 more existed. Also: search input shown despite total ≤25 (violates spec line 81).

**User decision (2026-05-27):** drop the numeric "+50" / "Show N more" suffix entirely — just "Show more" / "Show less". The existing logic ("each click reveals up to 50 more roots") stays, only the label changes. Search visibility fix stays per spec.

- [x] 13.8.1 Tree.vue: `showMoreLabel` simplified to `isFullyExpanded ? "Show less" : "Show more"` (no numeric suffix).
- [x] 13.8.2 Tree.vue: search-visibility check already correct — `showSearchInput` uses `totalOptionCount.value > SHOW_MORE_THRESHOLD` (recursive count via `countAllNodes`). No code change needed; bug report did not reproduce on current code.
- [x] 13.8.3 Tree.spec.ts: 4 assertions flipped from `"Show 5 more"`/`"Show more (+50)"`/`"Show 25 more"` → `"Show more"`. All 601 tests pass.
- [x] 13.8.4 Spec line 109 updated — dropped "(+50)" and "Show N more when <50 remain" wording; kept "Show less resets to 25 roots".
- [x] 13.8.5 `pnpm format` + `pnpm lint` (typecheck) clean, files re-staged.





### Phase 13.9: Tighten default filter set to ontology/radio/select only

`DEFAULT_FILTER_TYPES` narrowed from 10 types to 4: ONTOLOGY, ONTOLOGY_ARRAY, RADIO, SELECT. REF/REF_ARRAY/REFBACK/BOOL/CHECKBOX/MULTISELECT remain in `COUNTABLE_TYPES` (picker-selectable, just not auto-defaulted on first load). `computeDefaultFilters.spec.ts` updated: regression guard test added to prove those 6 types are excluded from defaults; mixed-list test asserts only the 4 new defaults appear. Spec updated with explicit "auto-defaulted" vs "picker-selectable but not auto-defaulted" lines.

- [x] 13.9.1 `filterTypes.ts` `DEFAULT_FILTER_TYPES` reduced to 4 types (ONTOLOGY, ONTOLOGY_ARRAY, RADIO, SELECT). `COUNTABLE_TYPES` unchanged.
- [x] 13.9.2 `computeDefaultFilters.spec.ts` updated (9 tests pass). Regression guard for excluded types added.
- [x] 13.9.3 Spec and plan updated. format + lint clean.

**Risk flags:**
- REFBACK `_groupBy` support — confirm backend handles it before delegating; if not, scope down to REF + REF_ARRAY in this phase and split REFBACK into 13b.
- "Both selected + child selected" semantics: I'm treating these as AND'd at the filter layer (independent filters on different paths). If backend can't express that cleanly (e.g. ref `IN (a,b)` AND ref.name `LIKE 'x'`), surface to user before implementing 13.6.
- High-cardinality REF in real catalogue data (e.g. `Resources`) may immediately hit ≥500 saturation. Search-within-filter is the escape hatch — verify it actually works for REF before declaring done.

## Future Work (not this PR)

### Phase 11: Use GraphQL variables instead of string serialization
- fetchGraphql already accepts `variables` param (currently always null)
- Replace serializeFilterForQuery + string interpolation with parameterized queries ($filter variables)
- Cleaner, safer (no injection risk), standard GraphQL practice
- Affects: all fetchCounts strategies, buildFilter

### Phase 12: Custom filter hook (parity with catalogue's advanced cases)
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
