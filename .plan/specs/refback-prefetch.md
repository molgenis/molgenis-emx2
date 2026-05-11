# Spec: REFBACK/REF_ARRAY prefetch — eliminate redundant DataList queries

## Problem

Loading a DetailView page (e.g. catalogue resource) makes **~19 separate GraphQL data queries**:
- 1 main row fetch (Resources with expandLevel=3)
- ~17 separate DataList queries, one per REFBACK/REF_ARRAY column
- 1 resource picker query (page-specific)

The main row fetch at expandLevel>=2 **already includes REFBACK data** at root level (`getColumnIds` only skips REFBACK at non-root levels). But DetailColumn ignores this prefetched data and renders a smart DataList that re-fetches the same data.

### Why it's slow

Each DataList query is sequential during SSR: metadata lookup → build GQL → fetch data → render. With ~17 REFBACK columns this creates a waterfall of ~17 round-trips to the backend, totaling ~13 seconds on a local dev server.

### Current flow (wasteful)

```
DetailView fetchRowData(expandLevel=3)
  → GQL query includes REFBACK data (e.g. subpopulations: [{name, description, ...}])
  → Row data stored in effectiveData

DetailView passes effectiveData[col.id] as `value` to DetailSection → DetailColumn

DetailColumn for REFBACK column:
  → watchEffect: fetchMetadata → find refbackCol → set refArrayFilter → showListView=true
  → Renders <DataList smart> which IGNORES the `value` prop
  → DataList fetches same data AGAIN via its own GraphQL query
```

## Proposed solution

When DetailColumn already has prefetched array data (from the parent's expandLevel fetch), use it directly instead of launching a separate smart DataList query.

### New flow

```
DetailColumn for REFBACK column:
  → IF value is a non-empty array AND schemaId is available:
    → Render <DataList dumb> with :rows="value" (prefetched data)
    → No additional query needed
  → ELSE (value is empty/missing but showListView would be true):
    → Fall back to current smart DataList behavior
```

### Components to change

#### 1. DetailColumn.vue — use prefetched data when available

Current logic:
```
showListView → DataList smart (always re-fetches)
```

New logic for REFBACK columns:
```
hasPrefetchedData && !showListView → DataList dumb (uses existing data)
showListView                       → DataList smart (fallback, fetches)
```

Key insight: `showListView` requires the async `watchEffect` to resolve (find refbackCol, set refArrayFilter). During SSR, the `value` prop is available immediately from the parent. We can render with prefetched data without waiting for the watchEffect.

The computed `hasPrefetchedData`:
```ts
const hasPrefetchedData = computed(() => {
  if (showListView.value) return false; // watchEffect resolved, use smart mode
  const type = props.column.columnType;
  if (type !== "REFBACK" && !isRefArrayColumn(type)) return false;
  if (!Array.isArray(props.value) || props.value.length === 0) return false;
  if (!props.column.refTableId || !props.schemaId) return false;
  return true;
});
```

Template addition (before the smart DataList):
```vue
<DataList
  v-else-if="hasPrefetchedData"
  :rows="value"
  :columns="prefetchedColumns"
  :layout="column.display || 'TABLE'"
  :schema-id="column.refSchemaId || schemaId"
  :table-id="column.refTableId"
  :row-label-template="column.refLabelDefault"
/>
```

For `prefetchedColumns`, we need the ref table's column metadata to determine which columns to show. We can use `getListColumns` from displayUtils, but we need the ref table columns. These come from `fetchMetadata` (cached by inflight dedup). We already have `refTableColumns` from the existing watchEffect — but it's empty when showListView is true. We need an alternative source.

Option A: compute `prefetchedColumns` from the refTable metadata independently.
Option B: reuse the existing `refTableColumns` ref — populate it in all cases, not just the non-refback path.

**Option B is simpler**: in the watchEffect, always populate `refTableColumns` regardless of whether refbackCol was found. Then `hasPrefetchedData` can use them.

#### 2. DetailColumn.vue watchEffect change

Current:
```ts
if (refbackCol && props.parentRowId) {
  refArrayFilter.value = { [refbackCol.id]: keyFilter };
  refTableColumns.value = []; // ← throws away column info
} else {
  refArrayFilter.value = undefined;
  refTableColumns.value = getListColumns(refTable.columns, {...});
}
```

New:
```ts
// Always populate refTableColumns for dumb DataList rendering
refTableColumns.value = getListColumns(refTable.columns, {
  layout: props.column.display as "TABLE" | "CARDS" | "LIST" | undefined,
});

if (refbackCol && props.parentRowId) {
  refArrayFilter.value = { [refbackCol.id]: keyFilter };
} else {
  refArrayFilter.value = undefined;
}
```

### Edge cases

#### Large REFBACK datasets
The main query returns ALL REFBACK rows (no pagination in nested GraphQL results). For most catalogue columns this is fine (few subpopulations, collection events, etc.). But if a REFBACK has hundreds of rows:

- The prefetched DataList is dumb mode — no server-side pagination
- Client-side search/pagination still works in dumb DataList
- The backend's GraphQL already returns all nested rows anyway (no limit), so the data transfer cost is the same
- If a REFBACK has 1000+ rows, the main query might be slow — but that's already the case with expandLevel>=2

**Safety valve**: the backend GraphQL has configurable limits on nested queries. If a REFBACK exceeds the backend limit, the prefetched array will be truncated and we should fall back to smart DataList. We can detect this: if `value.length` equals the backend limit (typically 100 or 1000), assume truncation and use smart mode.

Actually, simpler: the current behavior with `showListView` is the fallback. The `hasPrefetchedData` computed only activates when `showListView` is false (watchEffect hasn't resolved yet or didn't find a refbackCol). Once the watchEffect resolves and sets `refArrayFilter`, `showListView` becomes true and the smart DataList takes over.

This means:
- **SSR**: value is available, watchEffect hasn't resolved → prefetched dumb DataList renders
- **Client hydration**: watchEffect resolves → showListView becomes true → Vue reactivity switches to smart DataList

Wait — this means we'd render dumb DataList during SSR, then switch to smart DataList on hydration (causing a hydration mismatch and double render). That's worse.

#### Revised approach: don't race — check if prefetched data is sufficient

Instead of racing with watchEffect, make the decision based on the data:

```ts
const usePrefetchedData = computed(() => {
  const type = props.column.columnType;
  if (type !== "REFBACK" && !isRefArrayColumn(type)) return false;
  if (!Array.isArray(props.value) || props.value.length === 0) return false;
  if (!props.column.refTableId || !props.schemaId) return false;
  // Use prefetched data — DataList in dumb mode
  return true;
});
```

And **remove the smart DataList path for REFBACK entirely** when prefetched data exists. The watchEffect still populates `refTableColumns` (needed for column display), but `refArrayFilter`/`showListView` are no longer used when `usePrefetchedData` is true.

Template order:
```
1. empty check
2. usePrefetchedData → DataList dumb (REFBACK/REF_ARRAY with inline data)
3. showListView → DataList smart (fallback when no prefetched data)
4. isClickableRef → link + ValueEMX2
5. isHierarchicalOntology → OntologyTreeDisplay
6. showInlineListView → DataList dumb (existing, for REF_ARRAY without refback)
7. fallback → ValueEMX2
```

This avoids hydration mismatch: both SSR and client render the same dumb DataList.

**Caveat**: dumb DataList doesn't support server-side pagination. For REFBACK columns with many rows (e.g. 500+ publications), the user won't see paginated results from the server. But:
- DataList dumb mode has client-side search and pagination
- The data is already fetched in the main query regardless
- Most REFBACK columns have <100 rows in practice

For truly large REFBACK datasets, the page-level `columnTransform` can set a flag to opt specific columns out of prefetch mode (future work if needed).

#### hideColumns for REFBACK

The current smart DataList uses `:hide-columns="column.refBackId ? [column.refBackId] : undefined"` to hide the circular reference column. The dumb DataList should do the same. Since `refTableColumns` are computed via `getListColumns`, we can filter out the refBackId column there, or pass `hideColumns` to the dumb DataList.

### Expected impact

- **Before**: ~2 metadata + ~17 data queries = ~19 GraphQL requests, ~13s SSR
- **After**: ~2 metadata + ~2 data queries (main row + resource picker) = ~4 GraphQL requests
- Estimated SSR time: ~2-3s (single main query with expandLevel=3)

### What stays as smart DataList

- REFBACK columns where the parent row doesn't include prefetched data (e.g. value is empty/null)
- Any future columns explicitly opted out of prefetch
- REFBACK columns in contexts without expandLevel (e.g. nested DataList within DataList)

### Not in scope

- Pagination for large REFBACK datasets (client-side pagination is sufficient)
- Changing expandLevel behavior in fetchTableData
- Nested DataList transforms (separate spec)
