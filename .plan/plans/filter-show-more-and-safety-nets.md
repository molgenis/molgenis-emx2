# Filter show-more, zero-hiding, and count-query safety nets

## Goal
For long filter option lists (e.g. `unit` on catalogue-demo/Variables): truncate to a manageable size with a "show all" reveal, and hide zero-count options while collapsed. Add minimal safety nets to count queries so large databases can't hang or blow memory.

Repro: https://preview-emx2-pr-6147.dev.molgenis.org/apps/ui/catalogue-demo/Variables → filter `unit`.

## User-confirmed decisions
1. **Threshold: 25** (reuse existing Tree threshold — same number that triggers search + auto-expand). No second magic number.
2. **Zero-hiding tied to truncation state, NOT list size.** Collapsed → hide zeros. Expanded → show all. One mental model.
3. **Safety nets included:** frontend groupBy limit + per-column AbortController. Ontology parent-count fan-out cap deferred to a follow-up ticket.

## Behavior spec (goes into filter-sidebar-v2.md)

### Show more / truncation (Tree.vue + Column.vue)
- When a countable filter (ONTOLOGY, ONTOLOGY_ARRAY, BOOL, RADIO, CHECKBOX) has >25 total options: render first 25 + footer button `Show all (N more)` where N = hidden count.
- Clicking Show all reveals the full list (one-shot — no pagination). Button changes to `Show less`.
- Hierarchical trees: count is `countAllNodes(options)`. Truncation flattens the first 25 nodes in DFS order — does not break mid-subtree (round up to complete top-level sibling).
- Expand state of individual nodes is independent of show-more state.
- When user searches within the filter (client-side search input): show all matching terms regardless of collapsed state.

### Zero-count hiding (mergeWithBaseCounts)
- When collapsed (show-more NOT clicked): filter out options with `count === 0` from the rendered tree.
- When expanded (show-more clicked): show everything including zeros.
- Pruning respects hierarchy: a parent with all-zero descendants and count 0 is hidden. A parent with count 0 but non-zero descendants stays (its children carry signal).
- Search mode: always show matching terms, zero-count included.

### Count-query safety nets (fetchCounts.ts + fetchGraphql.ts)
- **GroupBy limit:** every `_groupBy` query passes `limit: 500`. If response size equals 500, flag the filter as "truncated" and show a hint in the filter section: "Too many distinct values to list — use search to find specific terms" (exact copy TBD).
- **AbortController per column:** `useFilters.ts` stores an AbortController per columnId. On filter-change-triggered refetch, abort the prior in-flight count request before firing a new one. `fetchGraphql` accepts an optional `signal` param and threads it to `$fetch`.
- **No backend change this PR** — purely client-side caps. If the existing backend endpoint ignores `limit` for groupBy, file a backend ticket; the client-side check on response length still catches it.

## Work order

### Phase 1 — safety nets (unit-testable, no UI regression risk)
1. `fetchGraphql.ts`: accept optional `signal` parameter, thread to `$fetch`.
2. `fetchCounts.ts`: add `limit: 500` to all `_groupBy` query strings. Detect response saturation (exact length === 500) and attach a flag to the return value.
3. `useFilters.ts`: create `abortControllers` Map<columnId, AbortController>. In `refetchCountsFor(columnId)`, abort the prior controller and pass `signal` into fetchCounts.
4. Unit tests: `fetchCounts.spec.ts` — assert limit is passed; `useFilters.spec.ts` — assert prior request aborted when new filter fires.

### Phase 2 — zero-hiding in mergeWithBaseCounts
5. `useFilters.ts:mergeWithBaseCounts` — add `hideZero` param (default false). Filter zero-count options (with hierarchy-aware parent pruning) when true.
6. Hook up `hideZero` based on Column.vue's showMore state — needs to flow down as a prop since merge happens in composable. Actually: do the filtering in Column.vue (post-merge), keep useFilters pure. Decide during implementation.
7. Unit tests: `useFilters.spec.ts` — assert zero-count pruning with hierarchy.

### Phase 3 — show-more UI
8. `Column.vue`: add `isExpanded` local ref. Compute `visibleOptions`:
   - If searching: all matching.
   - If !isExpanded and totalCount > 25: first 25 (DFS flatten, round up to sibling boundary) with zeros removed.
   - Else: full tree.
9. Footer button: `Show all (N more)` when collapsed, `Show less` when expanded. Hide button when totalCount ≤ 25 or searching.
10. Truncation hint: when groupBy response saturated (from phase 1), render "Too many distinct values — use search" note above the tree.
11. Unit tests: `Column.spec.ts` — covers all 6 states (≤25/expand/search × zeros).

### Phase 4 — verification
12. `pnpm run test-ci` — all green.
13. Visual browser check on catalogue-demo/Variables → filter `unit` (large list). Verify:
    - Initial render: 25 non-zero options + "Show all (...)" button
    - Click show all: all options including zeros
    - Click another filter: abort happens (devtools Network tab shows cancel)
    - Search within filter: full match list including zeros
14. Visual check on short filters (≤25 options): no show-more button, no behavior change.

## Files expected to change
- `apps/tailwind-components/app/composables/fetchGraphql.ts`
- `apps/tailwind-components/app/utils/fetchCounts.ts`
- `apps/tailwind-components/app/composables/useFilters.ts`
- `apps/tailwind-components/app/components/filter/Column.vue`
- `apps/tailwind-components/app/components/input/Tree.vue` (maybe — if truncation implemented at Tree level rather than Column)
- `apps/tailwind-components/tests/vitest/composables/useFilters.spec.ts`
- `apps/tailwind-components/tests/vitest/components/filter/Column.spec.ts`
- `apps/tailwind-components/tests/vitest/utils/fetchCounts.spec.ts` (if exists)
- `.plan/specs/filter-sidebar-v2.md` — new spec rows

## Deferred to follow-ups
- Ontology parent-count fan-out cap (ONTOLOGY_ARRAY 1-query-per-parent-node).
- Backend groupBy limit + cardinality cap.
- Ontology.spec.ts CI flakiness (shouldAdvanceTime under memory pressure) — separate concern, keeps recurring.
- Backend `_groupBy` does not accept `limit` argument (rejected as unknown). File backend ticket to add this; client-side saturation is only a UI warning, not a payload safeguard.

## Resolved decisions
- **Filtering location:** pure helper `applyCollapseView(options, { hideZero, limit })` lives in composable (testable without mount). `showMore` boolean ref lives in Column.vue. Component owns UI state, composable owns logic.
- **Hint copy:** `"too many options, please search"` (shown when groupBy response saturates at limit: 500).
- **Show-more in URL:** no. Ephemeral UI state, matches "expand state is NOT persisted" rule for Tree nodes.
