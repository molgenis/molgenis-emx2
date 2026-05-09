# PR #6147 Review Response Plan

## Phase 1: Quick fixes (low risk, no design decisions) — DONE 2026-05-06

Outcome:
- 1.1: keep plan/spec files (auto-resolved — `.plan/` in master `.gitignore`)
- 1.2: already done (Skeleton.vue extracted, uses `bg-disabled` theme token; InputSearch reused)
- 1.3: already done (Well component replaces type-badge)
- 1.4: already done (`console.error("Failed to load ref columns:", e)` in Picker.vue:87)
- 1.5: already done (`legend?: string`, no default empty-string)
- 1.6: reply-only — Sidebar two-script pattern is intentional (MG_COLLAPSED_PARAM export)
- 1.7: **fixed now** — `Picker.vue:22` `description?: string | null` → `description?: string`. NestedColumnMeta refTableId/refSchemaId keep `| null` (explicit `?? null` at line 216).
- 1.8: confirmed safe-as-is — `??` is semantically correct for `string | undefined`; reviewer's flagged `| null` not present in current code.


### 1.1 Remove plan/spec files from PR
- `.plan/plans/tw-filters-v2.md` — reviewer says remove
- `.plan/specs/filter-sidebar-v2.md` — reviewer says remove
- Author doesn't agree, keep for now.

### 1.2 Theme variables replacing hardcoded colors
- `Column.vue:100` — `bg-gray-200` should use theme variable
- `Column.vue:152` — hardcoded border/color, use theme variable
- Action: replace with existing theme classes

### 1.3 Type badge question (main.css:271)
- Already addressed: type-badge removed, replaced with `<Well>` component
- Action: reply on PR

### 1.4 Log exception (Picker.vue:97)
- Action: add `console.error(e)` in catch block

### 1.5 Range.vue:7 — empty legend
- Legend prop defaults to "" with `v-if="legend"` guard — it's optional
- Action: reply explaining, or change default to undefined if cleaner

### 1.6 Sidebar.vue:5 — two scripts
- This is the WELL_BASE export pattern (documented Vue SFC pattern)
- RESOLVED: actually Sidebar two-scripts is for MG_COLLAPSED_PARAM export
- Action: reply explaining purpose

### 1.7 Picker.vue:21 — optional and null
- Action: check if nullable is needed or if optional alone suffices

### 1.8 Column.vue:66 — null check
- Action: check if null is appropriate or should be undefined

## Phase 2: Refactors (moderate, code changes) — DONE 2026-05-06

### 2.1 NOT-APPLICABLE 2026-05-06
`max-w-48`, `max-w-32`, `ml-1.5` are standard Tailwind sizing utilities with no matching theme tokens. Left as-is (conservative).

### 2.2 DONE 2026-05-06
Replaced raw `<button>` "Clear all" with `<Button type="text" size="tiny">` in ActiveFilters.vue.

### 2.3 NOT-APPLICABLE 2026-05-06
`pruneZeros` at Column.vue:32 already guards `node.children && node.children.length > 0` at every access point. `hasNonZeroDescendant` also guards. No undefined path exists.

### 2.4 NOT-APPLICABLE 2026-05-06
`BOOL_ARRAY` is in `SELECTABLE_FILTER_TYPES` but not `COUNTABLE_TYPES`. The spec lists `BOOL_ARRAY` as neither countable nor range — text input fallback is intentional per current spec. No other ARRAY type falls through incorrectly post-H3.

### 2.5 NOT-APPLICABLE 2026-05-06
Picker.vue already has `applyAndClose`, `cancelAndClose` etc. as named functions. No inline emit in apply path.

### 2.7 NOT-APPLICABLE 2026-05-06
Already `watch(..., { immediate: true })` at Picker.vue line 62. Already refactored.

### 2.9 DONE 2026-05-06
Refactored `buildNestedMeta` in Picker.vue from imperative loop into `.map().filter()` functional chain.

### 2.10 DONE 2026-05-06
Extracted `NestedColumnMeta` interface to `types/filters.ts`. Updated `useFilters.ts` to import and use it. Updated `Picker.vue` to import from `types/filters.ts` (removes local duplicate interface).

### 2.11 NOT-APPLICABLE 2026-05-06
Already addressed with `<Well>` component (Phase 1).

### 2.12 NOT-APPLICABLE 2026-05-06
Already using `input/Search.vue` in Column.vue (Phase 3/H7).

## Phase 3: Architecture decisions (DECIDED)

### 3.1 Column.vue:22 — debounce
- DECISION: reuse `input/Search.vue` in Column.vue (option A)
- Search.vue already has debounce built in
- If 300ms vs 500ms matters, add `debounce` prop to Search.vue
- Removes need for TEXT_INPUT_DEBOUNCE_MS constant and manual setTimeout

### 3.2 Column.vue:43,47 — function wrapping
- DECISION: inline the logic (option B)
- Move `countedOptionToTreeNode` and `filterValueToTreeSelection` into Column.vue
- Remove from global utils if no other consumers

### 3.3 Column.vue:90 — reusable debounce
- DECISION: covered by 3.1 — reuse Search.vue, no manual debounce needed

### 3.4 Column.vue:104 — loading indicator
- DECISION: create small `Skeleton.vue` component (~10 lines)
- Fixes hardcoded `bg-gray-200`, sets precedent for consistent loading
- Use theme variable for skeleton bar color

### 3.5 Column.vue:138 — template slots over-engineering
- DECISION: remove slots from Range.vue, let Range own Input rendering internally
- Range.vue accepts column type + values, renders Input internally

### 3.6 Sidebar.vue:30 — route vs props
- DECISION: move collapsed URL sync into `useFilters`
- Remove route/router props from Sidebar
- Sidebar gets only `defaultCollapsed?: string[]` prop
- `useFilters` gains: `collapsedIds` (reactive), `toggleCollapse(id)`, mg_collapsed URL sync
- Route/router stay as useFilters options (required — tailwind-components has autoImport:false)
- MG_COLLAPSED_PARAM export moves from Sidebar to useFilters

## Phase 4: Functional review (hslh + Brenda) — triage 2026-05-06

Branch merged with origin/master (no conflicts). `.plan/` now in master's `.gitignore`.

### Will fix

| ID | Issue | Notes |
|----|-------|-------|
| H1 | Customising filters → Apply does nothing; only works after Clear+Apply | DONE 2026-05-06 — `useFilters.ts:updateUrl()` was preserving an existing `mg_filters` URL param but never writing a new one. Fix: when `userHasCustomized.value`, write `visibleFilterIds.value.join(",")` to the param. RED test in useFilters.spec.ts. 51/51 pass. |
| H2 | Filtering on REF / REFBACK columns broken (e.g. `collectionEvents.name`) | DONE 2026-05-06 — `buildFilter.ts` refField-wrap branch applied even when the path leaf segment equalled the refField, producing double-name nesting. Fix: when `leafSegment === refField`, emit `{ equals: refValues }` directly (skip wrap). Tests: "nested REF text path equals/like: collectionEvents.name produces no double-name". 42/42 pass. |
| H3 | Filtering broken for INT, STRING_ARRAY, DATETIME, NON_NEGATIVE_INT | DONE 2026-05-06 — STRING_ARRAY: re-routed to COUNTABLE_TYPES (Tree + equals), removed from STRING_FILTER_TYPES, added to SELECTABLE_FILTER_TYPES. INT: already working (RANGE_TYPES + buildFilter between {min,max}). NON_NEGATIVE_INT: already working. DATETIME: already working. Tests: Column.spec.ts +5 H3 tests, buildFilter.spec.ts +4 H3 tests. 127/127 pass. |
| H4 | Picker adds filters in reverse order (first-selected ends bottom) | DONE 2026-05-06 — `useFilters.ts:268` was prepending (`[columnId, ...rest]`); fixed to append (`[...rest, columnId]`). Test in useFilters.spec.ts. |
| H5 | Reset gives different default set than initial load | DONE 2026-05-06 — (a) Picker Reset emits `reset` (not `apply`) → Sidebar calls `resetFilters()` → unsets userHasCustomized + removes mg_filters from URL. (b) Empty `mg_filters=` watcher bug fixed. (c) Follow-up: `resetFilters()` now re-applies count>0 pruning via extracted `pruneVisibleByBaseCount()` helper — initial-state hides zero-count columns, Reset matches. Plan item 2.6 rolled in. |
| H7 | Removing linked text filter leaves text in input | DONE 2026-05-06 — debounce race: typed value in-flight on the old `onSearchInput` fn would re-emit after chip-X cleared the parent. Fixed in Column.vue: replaced `textValue` computed + `:model-value` prop-split with local `inputText` ref (`v-model`), a `watch(props.modelValue)` that syncs parent→local on clear, and a `watch(inputText)` driving `debouncedEmitText`. Test: Column.spec.ts "H7: input value clears when modelValue is removed". |
| H7b | Typing in text input for nested REF column (e.g. `collectionEvents.name`) makes active-filter chip flash and disappear | DONE 2026-05-06 — Codec round-trip regression: serializer appended `.name` suffix to nested like filters (`collectionEvents.name.name=Smith`); parser then peeled last segment as refField and returned `{operator:"equals", value:[{name:"Smith"}]}`; `textValueFromModelValue` only handles `like` → returned `""` → debouncedEmitText("") → cleared filter. Fix: `filterUrlCodec.ts` — serializer emits `{path}~like={value}` for nested `like` filters; parser detects `~like` suffix, strips it, returns `{operator:"like", value}` directly. RED tests: "nested REF like filter URL round-trip" (3 cases). 69/69 filterUrlCodec, 117/117 useFilters+Column pass. |
| H10 | "No matching values for this filter" wording confusing | DONE 2026-05-06 — reworded to "No options available given current filters" (Column.vue:247, 270). Auto-collapse path skipped (reactive complexity). |
| H11 | Pet store / Pet → cat filter returns empty (expected 3 rows) | DONE 2026-05-06 — `buildFilter.ts` RADIO/CHECKBOX composite-key branch emitted `_match_any` which backend rejects; replaced with `_or: [{key:{equals:val}},...]` per key object. RED→GREEN in buildFilter.spec.ts (3 new tests, 1 renamed). 48/48 pass. |
| H12 | Long filter list — selecting bottom filter requires scrolling up to see table changes | DONE 2026-05-06 — `sticky top-0 max-h-screen overflow-y-auto` on FilterSidebar in TableEMX2.vue:10. Spec row added under Filter Sidebar table. |
| H13 | Bad GraphQL requests in console while typing in filter text input | Reported by user during H7 verify (2026-05-06). Likely intermediate debounced emits send malformed/empty queries. Investigate buildFilter for empty `_like` operator + suppress debounce emit if input matches current modelValue. |

### Won't fix / out of scope

- **H6 / B1** — 0-result options not always hidden (BOOL `hricore` shows Yes(0)/No(0)). **By design for boolean** — Yes/No always shown so user can flip filter without clearing.
- **H8** — Total result count missing. Out of scope (general feature).
- **H9** — Collapse all / Expand all buttons. Out of scope (nice-to-have, follow-up).
- **B3** — Download / filtered download. Out of scope.

### Connor follow-ups

| ID | Issue | Status |
|----|-------|--------|
| C1 | `route/router` as composable options — Connor disagrees with "needs to work without nuxt context" argument | **DONE 2026-05-06** — removed `route?`/`router?` from `UseFiltersOptions`; composable now calls `useRoute()`/`useRouter()` from `#imports` directly. Tests mock `"#app/composables/router"` with reactive query. TableEMX2.vue simplified. 141/141 tests pass. |
| C2 | `subclassColumns` injection into TableMetadata | ✅ **Already done** — `subclassColumns` no longer present in `fetchTableMetadata.ts` / `fetchMetadata.ts`. |
| C3 | Integrate FilterSidebar into TableEMX2 by default | ✅ **Already done** — `TableEMX2.vue` now imports `FilterSidebar` and uses `useFilters`. |
| C4 | Remove `.plan/*` from PR | ✅ **Auto-resolved** — `.plan/` is in master's `.gitignore` after merge. Decision: keep specs as living guardrails on this branch only; consider moving spec-style notes to a non-`.plan` location later (e.g. `docs/` or repo wiki) once the pattern stabilises. No action needed on this PR. |

### Phase 4 fix order

1. **H1** first (Apply-after-Clear) — likely a URL/state hydration timing bug that may also explain H2.
2. **H2** — verify if H1 fix already resolves nested ref filter URL hydration.
3. **H3** — independent type-coverage fix.
4. **H4, H5, H7, H10** — small UX fixes, batchable.
5. **C1** — refactor route/router access. Independent; can be parallelised.

User to provide private postgres for repro before any code work begins.

## Phase 5: Clean-code refactor (PR size concern raised 2026-05-06)

Total PR JS: 3051 lines. useFilters.ts = 724 (largest). Owner concerns: Column/Picker too smart; Sidebar should delegate to useFilters; verify Search debounce removal safe; justify fetchGraphql abort.

**Audit findings:**
- Search debounce removal — SAFE. Every API-firing caller self-debounces (Column text 500ms, Sidebar global 500ms, Ontology setTimeout, TableEMX2 useDebounceFn). Listbox/RefSelect already synchronous pre-PR.
- fetchGraphql abort — KEEP. Used by `useFilters.ts:499` `signalledFetcher` to cancel stale per-column count requests. Without abort, last-arriving stale request overwrites fresh counts. Other callers (Ontology, RefSelect) ignore the optional param.

### 5.1 Mechanical extractions (no behavior change) — pending approval

| # | Move | From → To | Δ lines |
|---|------|-----------|---------|
| 5.1.a | `buildLabelMap`, `flattenOntologyTree`, `flattenRefOptions` | useFilters.ts → new `utils/filterLabels.ts` | −55 useFilters |
| 5.1.b | `pathLabel` | Sidebar.vue → `filterTypes.ts` | −4 Sidebar |
| 5.1.c | `getFilterSelectionCount` | Sidebar.vue → useFilters return | −6 Sidebar |
| 5.1.d | tree utils (pruneZeros, filterNode, displayCount, countedOptionToTreeNode etc.) | Column.vue → new `utils/filterOptionsTree.ts` | −100 Column |
| 5.1.e | Housekeeping: drop `compare.ts:jsonEqual` if unused; dedupe `STRING_TYPES` (filterUrlCodec) with `STRING_FILTER_TYPES` (filterTypes) | — | small |

### 5.2 Behavioral extractions (need care) — pending approval

| # | Move | From → To | Δ lines | Risk |
|---|------|-----------|---------|------|
| 5.2.a | `buildNodes` + `buildNestedMeta` | Picker.vue → new `utils/pickerNodes.ts` | −75 Picker | Low (verify reactivity) |
| 5.2.b | `handlePickerApply` | Sidebar.vue → useFilters as `applyPickerSelection` | −22 Sidebar | Low |
| 5.2.c | count-fetching cluster | useFilters.ts → new `useFilterCounts` sub-composable | −128 useFilters | Medium (reactive wires) |

**Net target:** useFilters 724 → ~540, Column 324 → ~225, Picker 378 → ~300, Sidebar 202 → ~170.

**Status 2026-05-06:** awaiting owner decision — proceed with 5.1 mechanical batch, or ship PR as-is now that all functional review items are DONE and 551/551 tests pass.

### 5.3 Column.vue split into per-shape components — DONE 2026-05-06

Column.vue was "3 components in a trenchcoat" (countable/range/text). Extracted two new components mirroring the existing `Range.vue` pattern:

| File | Lines | Role |
|------|-------|------|
| `filter/Tree.vue` (`<FilterTree>`) | 181 | countable branch: all tree/show-more/search/skeleton/saturation logic |
| `filter/Text.vue` (`<FilterText>`) | 46 | text branch: debounced text input |
| `filter/Column.vue` (post-refactor) | 63 | thin dispatcher: `<FilterTree>` / `<FilterRange>` / `<FilterText>` |
| `filter/Range.vue` | 76 | unchanged |

Column.vue before: 324 lines. After: 63 lines (−261). Tree: 181 new. Text: 46 new.

Tests split: Column.spec.ts: 19 (dispatcher only). Tree.spec.ts: 46 (new). Text.spec.ts: 9 (new). Sidebar.spec.ts: 19 (unchanged). All 122 filter component tests pass.

### 5.4 Disentangle filter/Tree from input/Tree via TreeNode composition — proposed

**Motivation.** PR currently modifies `input/Tree.vue` in 8 places (selection rename, label clone, autoExpandSmallTree, expand-state preservation watch, showSearch computed, handleSearchInput type tweak, placeholder/size changes, ButtonText removal). Only one prod caller exists: `filter/Tree.vue`. Touching a shared input component to satisfy filter-only needs leaks filter concerns into the input layer and bloats the PR diff against `master`.

**Approach.** `input/TreeNode.vue` already exposes everything needed (recursive node renderer, label support, toggle/expand emits, multiselect, scrollContainer, applyFilter). Replace `<Tree>` usage inside `filter/Tree.vue` with direct `<TreeNode>` composition. `filter/Tree.vue` absorbs the state machinery (nodeMap, clone, parent/child selection propagation, expand-state preservation, virtual root). `input/Tree.vue` reverts ~100% to `master`.

**Scope.**

| File | Change | Δ vs current PR |
|------|--------|-----------------|
| `filter/Tree.vue` | absorb nodeMap + clone + selection propagation + expand-preservation + virtual root; render `<TreeNode>` recursively (no internal search — filter/Tree already has one) | +~110 LOC |
| `input/Tree.vue` | revert to `master` (drop `disableInternalSearch` prop, `autoExpandSmallTree`, label clone, `showSearch` computed, `handleSearchInput` type widening, `selection`→`selected` rename, expand-preservation watch, placeholder/size tweaks, ButtonText toggle restored) | diff vs master ~0 |
| `input/TreeNode.vue` | unchanged (master already has the API filter/Tree needs) | 0 |
| `pages/input/Tree.story.vue` | unchanged | 0 |

**What filter/Tree absorbs from input/Tree (pieces that are filter-specific anyway):**
- `nodeMap` keyed by `name` + recursive `clone(node)` — needed to track per-node expand/visibility independently of the props.
- `applyModelValueChangeToSelection` + `processSelectionChangeToParentAndChildNodes` — intermediate selection propagation.
- `toggleSelect` / `toggleExpand` handlers wired to TreeNode emits.
- `emitSelection` excluding selected children (the `emitSelectedChildren=false`-equivalent emit semantics).
- `nodes`-watch that preserves expand state across rebuild (counts refresh in filter context, not in master Tree usage).
- `autoExpandSmallTree` (≤25 nodes auto-expand) — filter-specific UX; master Tree doesn't need it.
- `virtualRootNode` + `<TreeNode :parentNode>` recursive render.

**What filter/Tree does NOT need:**
- input/Tree's internal `optionsSearch` watch + `applySearch` — filter/Tree has its own `localSearch` + `filterOptionsBySearch` already.
- `ButtonText` toggle button — filter/Tree shows search inline above the tree.

**Scout verification 2026-05-07 (read-only):**
- TreeNode is unchanged on this branch vs master (zero diff). All props/emits filter/Tree needs are already on master: `id, parentNode, inverted, isRoot, multiselect, valid, invalid, disabled, isSearching, scrollContainer, enableAutoLoad` + emits `toggleSelect, toggleExpand, showOutsideResults, loadMore, showAllChildren, applyFilter`.
- **Zero prod callers of `input/Tree.vue` on master.** Only `pages/input/Tree.story.vue` uses it. Reverting is safe; no master consumer can regress.
- **Precedent for TreeNode-direct composition:** `input/Ontology.vue` on master already composes `<TreeNode>` directly without going through `input/Tree.vue`. Our filter/Tree refactor follows the same pattern.
- Story file (`pages/input/Tree.story.vue`) tests basic multiselect / inverted / expandSelected only — does NOT depend on any PR-added input/Tree behaviors (autoExpand, label clone, disableInternalSearch, expand-preservation watch).
- `disableInternalSearch` prop becomes unnecessary post-revert: filter/Tree has its own search above the tree, so input/Tree's internal search simply isn't needed by anyone.

**Risk:** Low. `filter/Tree.spec.ts` (46 tests) asserts external contract; should remain green. If any test reaches into input/Tree internals, rewrite to assert via TreeNode markup or filter/Tree behavior.

**Diff impact.**
- `apps/tailwind-components/app/components/input/Tree.vue` diff vs master: shrinks from ~50 lines changed → 0.
- `apps/tailwind-components/app/components/filter/Tree.vue`: 250 → ~360 LOC.
- Net PR LOC: roughly neutral. Net PR clarity: input layer untouched, filter logic localised to `filter/`.

**Steps.**
1. Scout: list all `master` callers of `input/Tree.vue` and `input/TreeNode.vue`; quote master TreeNode props/emits.
2. frontend agent: rewrite `filter/Tree.vue` to compose `<TreeNode>` directly (absorb logic above).
3. frontend agent: `git checkout master -- apps/tailwind-components/app/components/input/Tree.vue` (revert).
4. Run `Tree.spec.ts` + `Sidebar.spec.ts` + `Column.spec.ts` + `useFilters.spec.ts`. Fix any breakage in filter-side tests; do NOT modify input-side tests.
5. Run full `tailwind-components` vitest + lint + format.
6. Browser verify in story: countable filter still searches, multi-selects, preserves expand state on count refresh, auto-expands small trees, emits selected names without children-of-selected.
7. Stage all touched files.

**Status 2026-05-07:** DONE.

Outcome:
- `input/Tree.vue` reverted 100% to master (zero diff).
- `filter/Tree.vue` 250 → 405 LOC: absorbs nodeMap, clone, parent/child selection propagation, expand-state preservation watch, `autoExpandSmallTree`, virtualRootNode, recursive `<TreeNode>` composition. Internal `<InputSearch>` for in-filter search above the tree retained.
- `input/TreeNode.vue` unchanged.
- `input/Search.vue` minimal cleanup vs master: kept master's explicit `defineEmits` pattern, removed misplaced debounce, added explicit `string` typing on the emit. Diff vs master ~19 lines.
- `filter/Tree.spec.ts` grew to 824 lines: kept all §5.3 coverage, added two new describe blocks for behaviors that moved out of input/Tree (`auto-expand on small trees`, `expand state preserved across options updates`).
- `input/Tree.spec.ts` reduced to 133 lines: tests now match master Tree's actual behavior; branch-only tests for moved behaviors are now in filter/Tree.spec.ts.
- Final test count: 560 (was 559 pre-§5.4, +1 from added auto-expand/expand-preservation tests minus the 0 lost since coverage moved).
- Lint clean. Format clean.

Spec rows added (commit alongside merge):
| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Small tree (≤25 total nodes): parent nodes start expanded | filter/Tree.vue | `Tree.spec.ts: "auto-expands all parent nodes when FilterTree has ≤25 total nodes"` | visual check |
| Large tree (>25 total nodes): parents start collapsed | filter/Tree.vue | `Tree.spec.ts: "does not auto-expand when FilterTree has >25 total nodes"` | visual check |
| Expand state preserved across options prop updates (count refresh) | filter/Tree.vue | `Tree.spec.ts: "preserves expand state when options prop updates with new counts"` | visual check |

## Phase 5.5: Cognitive-load reduction (audit-driven, 2026-05-09)

PR-owner principle: **reducing code & demystifying > splitting**. Slight duplication acceptable if it makes a file easier to follow. Splitting only when the test surface or the cohesion truly demands it.

Audit confirmed (scout): `fetchGraphql` abort is structural (not optional); `buildFilter.ts` keeps its public test API; `compare.ts` is specialised; `filterTypes.ts` is correctly placed in `utils/`; `formatFilterValue.ts` is clean; backend Java diffs are merge-from-master noise (1 branch-authored line in `GraphqlTableFieldFactory.java`).

### Slice A — cheap mechanical wins

| # | Change | Files |
|---|--------|-------|
| A1 | filter/Tree.vue: reorder so the script reads top-down (imports → constants → props/emits → nodeMap setup → main reactive flow → handlers → low-level helpers). No behaviour change. | `apps/tailwind-components/app/components/filter/Tree.vue` |
| A2 | `fetchCounts.ts`: rename `resolveOntologyAncestorChain` → `fetchOntologyAncestorsForLeaves`; `rollupOntologyParentCountsFromChildren` → `rollupParentCountsForSingleSelectOntology`. Add one-line "why" comment above each. | `apps/tailwind-components/app/utils/fetchCounts.ts` + spec |
| A3 | Merge `filterConstants.ts` (`BOOL_LABELS`) into `filterTypes.ts`; delete `filterConstants.ts`; update both importers (`useFilters.ts`, `fetchCounts.ts`). | `app/utils/filterTypes.ts`, `app/utils/filterConstants.ts` (delete), `useFilters.ts`, `fetchCounts.ts` |
| A4 | Rename `filterUrlCodec.ts` → `filterUrlParams.ts`. Update spec filename (`filterUrlCodec.spec.ts` → `filterUrlParams.spec.ts`) and all import paths. No behaviour change. | `app/utils/filterUrlCodec.ts` → `app/utils/filterUrlParams.ts`, spec, `useFilters.ts` |

### Slice B — Picker.vue inline refactor

| # | Change | Notes |
|---|--------|-------|
| B1 | Split the 50-line `buildNodes` and 25-line `buildNestedMeta` inside Picker.vue into smaller, named local helpers (no new file). Group sections in source-order: ref-loading → tree-building → search/filter → selection → modal handlers. Goal: each function fits on a screen. | `apps/tailwind-components/app/components/filter/Picker.vue` |

### Slice C — useFilters split

| # | Change | Notes |
|---|--------|-------|
| C1 | Extract the count-fetch cluster (~170 LOC: abort controller, signalledFetcher, fetchColumnCounts, mergeWithBaseCounts, base/updated count maps, saturation flags) into a sub-composable `composables/useFilterCounts.ts`. Take dependencies via params. Keep public API on useFilters unchanged. | new `composables/useFilterCounts.ts`; `useFilters.ts` slimmer |
| C2 | After C1, in-place readability pass on `useFilters.ts`: group sections in source-order, simplify trivial wrappers, ensure top-down flow. No new files. | `useFilters.ts` |

### Order

1. Slice A (one frontend agent pass, low risk; verify with full vitest + lint).
2. Slice B (Picker.vue inline refactor; verify Picker.spec.ts + Sidebar.spec.ts).
3. Slice C1 → C2 (sub-composable extraction first, then in-place cleanup).

**Skip Phase 5.1 / 5.2 batch** — covered or rejected by this slicing.

**Status 2026-05-09:** Slice A DONE.

Slice A outcome:
- A1 filter/Tree.vue reordered top-down: imports → constants → props/emits → main reactive flow (refs/computeds, lines 37–83) → nodeMap state machine (lines 86–236) → UI handlers (lines 240–255) → low-level pure helpers (lines 259–350). 49/49 Tree tests green. Caveat: `treeSelection`/`treeNodes` had to stay in main-flow before nodeMap setup because `buildNodeMap(treeNodes.value)` runs at top level (const-init order vs function hoisting).
- A2 fetchCounts: renamed `resolveOntologyAncestorChain` → `fetchOntologyAncestorsForLeaves`, `rollupOntologyParentCountsFromChildren` → `rollupParentCountsForSingleSelectOntology`. Two one-line "why" comments added.
- A3 `filterConstants.ts` deleted; `BOOL_LABELS` moved into `filterTypes.ts`. Two importers updated (`useFilters.ts`, `fetchCounts.ts`).
- A4 `filterUrlCodec.ts` → `filterUrlParams.ts` (file + spec, all imports). `git grep filterUrlCodec` empty.
- 560/560 tests, lint+format clean. Spec file references updated (8 rows in filter-sidebar-v2.md).

Slice B DONE 2026-05-09:
- Picker.vue script 252 → 278 LOC (+26). Five named helpers extracted in-place (buildRefPickerNode, buildLeafPickerNode, matchesSearchQuery, isInExpandedParent, buildNestedLabelForId).
- buildNodes 50→27, displayedNodes 19→10, buildNestedMeta 25→20. Each function ≤ ~25 lines.
- Dead code inlined (isMgCol wrapper).
- Top-down source order: imports → types → props/emits → state → ref-loading → tree-builders → search helpers → main computeds → selection → nested-meta → modal handlers.
- 18/18 Picker.spec.ts, 560/560 full vitest, lint+format clean. No new files, no comments.

Trade: +26 LOC for "every function fits on a screen with a name that explains intent". Acceptable per slice-B mandate.

Slice C DONE 2026-05-09:
- Extracted `composables/useFilterCounts.ts` (new, 233 LOC). Houses: per-column AbortControllers, signalledFetcher, fetchColumnCounts, mergeWithBaseCounts, baseCounts/updatedCounts maps, loading + saturation flags, debouncedRefetchCounts. Self-contained watchers on filterStates/searchValue/nestedColumnMeta live here.
- Public API of `useFilters` unchanged (all 21 names preserved). useFilterCounts public API: `baseCounts: Ref<Map<string, CountedOption[]>>`, `fetchColumnCounts(columnId, useBase?)`, `getCountedOptions`, `isCountLoading`, `isSaturated`, `debouncedRefetchCounts`.
- `useFilters.ts` slimmed 724 → 563 LOC (−161). Top-down structure: imports → module constants/types → pure helpers → useFilters fn (state refs → URL sync → useFilterCounts invocation → mutators → computeds → collapse state → nested hydration → watchers → return) → module-level label utilities.
- 56/56 useFilters.spec.ts, 560/560 full vitest, lint+format clean. No new spec for useFilterCounts (covered transitively via useFilters).
- columns watcher kept in useFilters because it must coordinate with pruneVisibleByBaseCount.
- Spec file (filter-sidebar-v2.md) Component column updated for behaviors that physically moved (rows 106, 107, 114, 116, 121, 125–130, 134, 142–143).

Phase 5.5 closed. Net cognitive-load delta across A+B+C:
- 1 file deleted (filterConstants.ts)
- 1 file renamed (filterUrlCodec → filterUrlParams)
- 2 helper renames in fetchCounts.ts
- Picker.vue: same file, 5 named helpers (+26 LOC, screen-fit functions)
- useFilters.ts: −161 LOC; new useFilterCounts.ts (+233) with focused responsibility
- filter/Tree.vue: top-down reorder, no functional change.
