# Filter Count Visibility: Hide Empty Options

## Context
Scientific database filters should show stable options. When a REF/ONTOLOGY filter option has zero records in the dataset, it should be hidden entirely. When an option becomes zero due to cross-filtering (other active filters), it remains visible with count (0). No greying/disabled state — keep it simple: hide or show.

Search overrides hiding: when user types a search query, show all matches including zero-count.

URL reload case: even when filters are pre-set via URL, base counts (unfiltered) determine which options exist in the data.

## Current Status

### Done
- ICountFetcher interface: `fetchRefBaseCounts`, `fetchOntologyLeafBaseCounts`, `fetchOntologyParentBaseCounts`
- createCountFetcher: base count implementations (no cross-filter)
- Ref.vue: baseCounts state, hides baseCount=0 options via listOptions filter, search overrides hiding
- Ontology.vue: fetches leaf + parent base counts, merges into baseCounts (always fetch+merge, no fetched flag)
- Small spinner (BaseIcon progress-activity animate-spin :width=12) next to count while loading
- Tests: 384 passing

### In Progress
- Prune tree approach: remove baseCount=0 nodes from tree DATA instead of hiding via v-for filter in TreeNode
- "X options hidden (no matching records)" message

### Design: Prune Tree Approach
Instead of passing baseCounts to TreeNode and filtering in v-for, prune the tree in Ontology.vue:

**Ontology.vue** (after base counts arrive):
1. Walk the tree recursively
2. Remove leaf nodes where baseCounts.get(name) === 0
3. Remove parent nodes where ALL descendants have baseCount=0
4. Track count of removed nodes for display message
5. Show "X options hidden (no matching records)"
6. When searching, skip pruning — show all matches including zero-count

**TreeNode.vue** (simplify):
- Remove `baseCounts` prop entirely
- Remove `isVisibleByBaseCount` function
- Remove `isSearching` prop (if only used for base count override)
- TreeNode becomes simpler — just renders what it's given

**Benefits**:24 got 4 
- "Load more" and pagination work correctly (based on actual tree data)
- No ghost nodes confusing UI
- Cleaner separation: Ontology.vue owns visibility logic, TreeNode just renders

### TODO
- Refactor: reduce duplication between base-count and cross-filter-count functions in createCountFetcher
- New filter column should be added at BEGINNING of filter list, not end
- Removing a filter should also clear its filter state (selected values)

### Future (backend feature request)
- `_groupBy` with `limit`/`offset` support — enables paginating directly through "terms that have records" without fetching all terms. Critical for large flat ontologies (10,000+ terms) where only a few have matching records.

## Key files
1. `app/utils/createCountFetcher.ts` - ICountFetcher interface + base count implementations
2. `app/components/input/Ref.vue` - base counts + visibility
3. `app/components/input/Ontology.vue` - base counts + tree pruning
4. `app/components/input/TreeNode.vue` - rendering only (no baseCounts logic)
5. `app/components/input/CheckboxGroup.vue` - display (no baseCounts logic)

## Verification
1. `pnpm test` - all pass
2. Manual: open filter sidebar, verify baseCount=0 options hidden
3. Manual: apply cross-filters, verify count=0 options still visible (not hidden)
4. Manual: search in filter, verify all matches shown including zero-count
5. Manual: expand ontology node, verify children appear correctly
6. Manual: "load more" works correctly — no empty results, correct counts
7. Manual: "X options hidden" message shown when options pruned
8. Manual: load page with URL filter params, verify base counts correct
