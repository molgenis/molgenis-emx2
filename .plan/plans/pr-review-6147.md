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
