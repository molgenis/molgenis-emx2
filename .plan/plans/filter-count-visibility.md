# Filter Count Visibility: Hide Empty Options

## Context
Scientific database filters should show stable options. When a REF/ONTOLOGY filter option has zero records in the dataset, it should be hidden entirely. When an option becomes zero due to cross-filtering (other active filters), it remains visible with count (0). No greying/disabled state — keep it simple: hide or show.

Search overrides hiding: when user types a search query, show all matches including zero-count.

URL reload case: even when filters are pre-set via URL, base counts (unfiltered) determine which options exist in the data.

## Done
- ICountFetcher interface: `fetchRefBaseCounts`, `fetchOntologyLeafBaseCounts`, `fetchOntologyParentBaseCounts`
- createCountFetcher: base count implementations (no cross-filter), parent uses `_match_any_including_children`
- Ref.vue: baseCounts state, hides baseCount=0 options via listOptions filter, search overrides hiding, hidden count message
- Ontology.vue: prunes tree DATA (not v-for filter) — removes baseCount=0 nodes from children arrays
- Ontology.vue: always fetch+merge base counts (no `baseCountsFetched` flag), works with paging/expand
- Ontology.vue: auto-page after pruning — loads more batches until `props.limit` visible items or no more pages (safety cap 10 iterations)
- Ontology.vue: auto-page also works for expanded child nodes
- Ontology.vue: accumulated pruned counter (`+=`) with "X options hidden (no matching records)" message, always shown when > 0
- Ontology.vue: `node.expanded = true` set BEFORE loadPage so pruning recurses into children correctly
- TreeNode.vue: simplified — no baseCounts prop, no isVisibleByBaseCount, just renders what it's given
- Small spinner (BaseIcon progress-activity animate-spin :width=12) next to count while loading, replacing opacity-50
- `await fetchCountsForVisibleNodes()` in loadPage to ensure pruning completes before auto-page checks
- Tests: 387 passing (including red-green tests for child pruning after expand)
- CI fixes: formatted filter-counts.spec.ts, scoped e2e ref-columns click to table element
- Refactored createCountFetcher: 6 functions → 3 shared helpers with optional crossFilter param (−177 lines)
- toggleFilter prepends new filters at beginning of list (not end)
- toggleFilter already cleared filter state on remove (confirmed, added regression test)

## TODO
(none)

## Future (backend feature request)
- `_groupBy` with `limit`/`offset` support — enables paginating directly through "terms that have records" without fetching all terms. Critical for large flat ontologies (10,000+ terms) where only a few have matching records. Without this, frontend must fetch all base counts or page through many batches.

## Key files
1. `app/utils/createCountFetcher.ts` - ICountFetcher interface + 3 shared helpers (cross-filtered + base via optional param)
2. `app/components/input/Ref.vue` - base counts + listOptions filtering + hidden count
3. `app/components/input/Ontology.vue` - base counts + tree pruning + auto-paging + hidden count
4. `app/components/input/TreeNode.vue` - rendering only, spinner for count loading
5. `app/components/input/CheckboxGroup.vue` - spinner for count loading

## Verification
1. `pnpm test` — 387 pass
2. Manual: open filter sidebar, verify baseCount=0 options hidden
3. Manual: apply cross-filters, verify count=0 options still visible (not hidden)
4. Manual: search in filter, verify all matches shown including zero-count
5. Manual: expand ontology node, verify children with baseCount=0 are pruned
6. Manual: "load more" auto-pages when most items pruned
7. Manual: "X options hidden" message shown and accumulates across pages
8. Manual: load page with URL filter params, verify base counts correct
