# Spec: Prefetched DataList pagination — limit + count + smart fallback

## Problem

The REFBACK prefetch optimization (F6) fetches ALL nested rows in the main query. For small datasets this is fine, but a REFBACK with 10,000 rows would bloat the main query response. We need:

1. An optional limit on prefetched rows to keep the main query lightweight
2. A count so the dumb DataList knows if there are more records beyond the initial pages
3. A smart fallback for search and pagination beyond the prefetched set

## Backend verification

Confirmed: backend supports both `(limit:N)` and `_agg{count}` on nested REFBACK fields:
```graphql
Resources(filter:{id:{equals:"testCohort1"}},limit:1) {
  collectionEvents(limit:2) { name }
  collectionEvents_agg { count }    # returns 2
  subpopulations(limit:1) { name }
  subpopulations_agg { count }      # returns 2 (1 row returned, 2 total)
}
```

## Current state

### Main query (expandLevel=2)
`getColumnIds` in `fetchTableData.ts` builds the GQL query. For REFBACK at root level, it expands nested fields but does NOT add `limit` or request `_agg { count }` on nested collections. The backend returns ALL matching rows.

### DetailColumn
When `usePrefetchedData` is true (value is an array), renders dumb DataList with `:rows="value"`. The dumb DataList does client-side pagination (slicing the array). No way to know if the data is truncated.

### DataList (dumb mode)
Receives `:rows` prop, paginates client-side. `useTableData` is now skipped in dumb mode. No search capability in dumb mode.

## Proposed solution

### Step 1: Add `_agg { count }` and optional `limit` to nested REFBACK/REF_ARRAY in GQL query

**File: `app/composables/fetchTableData.ts` → `getColumnIds`**

Currently, REFBACK/REF_ARRAY at root level generates:
```graphql
collectionEvents { name description ... }
```

Change to always include `_agg { count }` and optionally add `(limit: N)`:
```graphql
collectionEvents { name description ... }
collectionEvents_agg { count }
```

Or with limit:
```graphql
collectionEvents(limit: 100) { name description ... }
collectionEvents_agg { count }
```

**Limit is NOT added by default.** It's controlled via a new `nestedLimit` property on `IQueryMetaData`. When omitted, no limit is applied (all rows returned, current behavior). When set (e.g. `nestedLimit: 100`), adds `(limit: N)` to REFBACK/REF_ARRAY fields at root level.

`_agg { count }` is always added for REFBACK/REF_ARRAY at root level, regardless of whether a limit is set. This gives the DataList total count awareness for free.

**Changes to `getColumnIds`:**
- New parameter: `nestedLimit?: number`
- For REFBACK/REF_ARRAY/MULTISELECT/CHECKBOX at rootLevel: append `_agg { count }` as sibling field
- If `nestedLimit` is set: add `(limit: ${nestedLimit})` to the field arguments

**Changes to `IQueryMetaData`:**
- Add `nestedLimit?: number`

**Changes to `fetchTableData`:**
- Pass `nestedLimit` from properties to `getColumnIds`

### Step 2: Expose `nestedLimit` on DetailView

**File: `app/components/display/DetailView.vue`**

Add optional `nestedLimit?: number` prop. Pass to `fetchRowData` → `fetchTableData`.

**File: `app/composables/fetchRowData.ts`**

Accept and pass `nestedLimit` to `fetchTableData`.

### Step 3: Pass count alongside value to DetailColumn

**File: `app/components/display/DetailView.vue`**

The sections computed maps columns to `{ meta, value }`. Add count from `_agg` data:
```ts
value: effectiveData.value[col.id],
count: effectiveData.value[`${col.id}_agg`]?.count
```

**File: `types/types.ts` → `ISectionField`**
```ts
export interface ISectionField {
  meta: IColumn;
  value: any;
  count?: number;
}
```

**File: `app/components/display/DetailSection.vue`**

Pass `count` to RecordColumn:
```html
<RecordColumn :column="col.meta" :value="col.value" :count="col.count" ... />
```

### Step 4: DetailColumn passes count and filter to DataList

**File: `app/components/display/DetailColumn.vue`**

Add `count?: number` prop. Pass to dumb DataList:
```html
<DataList
  v-else-if="usePrefetchedData"
  :rows="value"
  :total-count="count"
  :columns="refTableColumns"
  :layout="column.display || 'TABLE'"
  :schema-id="column.refSchemaId || schemaId"
  :table-id="column.refTableId"
  :filter="listFilter"
  :hide-columns="column.refBackId ? [column.refBackId] : undefined"
  :row-label-template="column.refLabelDefault"
/>
```

The `filter` (REFBACK filter scoped to parent record) is passed so the smart fallback can query correctly. The `watchEffect` already computes this.

### Step 5: DataList hybrid mode — dumb with smart fallback

**File: `app/components/display/DataList.vue`**

Add `totalCount?: number` prop.

**Hybrid mode logic:**

The DataList starts in prefetch (dumb) mode when `:rows` is provided. It switches to smart mode permanently when the user interacts beyond the prefetched data. Once in smart mode, it stays there — the server handles all pagination and search from that point.

```ts
const isPrefetchMode = computed(
  () => !!props.rows && !needsSmartFallback.value
);

const needsSmartFallback = ref(false);

// Switch to smart mode permanently when user needs server data
watch(searchTerms, (val) => {
  if (val && hasTruncatedData.value) {
    needsSmartFallback.value = true;
  }
});

watch(page, (val) => {
  const prefetchedPages = Math.ceil((props.rows?.length || 0) / props.pageSize);
  if (val > prefetchedPages) {
    needsSmartFallback.value = true;
  }
});

const hasTruncatedData = computed(() =>
  props.totalCount !== undefined && props.rows && props.totalCount > props.rows.length
);
```

**Important: switching to smart mode is one-way.** Once the user searches or pages beyond prefetched data, we stay in smart mode. Clearing search does NOT return to dumb mode — the smart DataList handles page 1 with server data. This avoids jarring UX where data switches between local and server results.

**Exception**: if `totalCount <= rows.length` (all data is prefetched), smart fallback is never needed. Search stays client-side.

Pass effective mode to `useTableData`:
```ts
const effectiveSmartMode = computed(
  () => isSmartMode.value || needsSmartFallback.value
);

} = useTableData(
  effectiveSmartMode.value ? props.schemaId || "" : "",
  effectiveSmartMode.value ? props.tableId || "" : "",
  { ... }
);
```

Display rows and pagination:
```ts
const displayedRows = computed(() =>
  effectiveSmartMode.value ? fetchedRows.value : props.rows || []
);

const displayedCount = computed(() =>
  effectiveSmartMode.value
    ? count.value
    : (props.totalCount ?? props.rows?.length ?? 0)
);

const effectiveTotalPages = computed(() =>
  Math.ceil(displayedCount.value / props.pageSize)
);

const effectiveShowPagination = computed(() =>
  displayedCount.value > props.pageSize
);
```

**Search behavior:**
- All data prefetched (`totalCount === rows.length`): client-side filter on `searchTerms`, no smart fallback needed
- Truncated data (`totalCount > rows.length`): typing in search triggers smart fallback, server handles filtering

## Files to change

| File | Change |
|------|--------|
| `metadata-utils/src/IQueryMetaData.ts` | Add `nestedLimit?: number` |
| `app/composables/fetchTableData.ts` | `getColumnIds`: add `_agg{count}` for REFBACK/REF_ARRAY at root; optional `(limit:N)` |
| `app/composables/fetchRowData.ts` | Pass `nestedLimit` through to `fetchTableData` |
| `types/types.ts` | `ISectionField`: add `count?: number` |
| `app/components/display/DetailView.vue` | Add `nestedLimit` prop; pass `_agg` count in section mapping |
| `app/components/display/DetailSection.vue` | Pass `count` to RecordColumn |
| `app/components/display/DetailColumn.vue` | Add `count` prop, pass `totalCount` + `filter` to dumb DataList |
| `app/components/display/DataList.vue` | Add `totalCount` prop, hybrid mode, one-way smart fallback |

## Edge cases

- **REFBACK with < nestedLimit rows (or no limit set)**: count equals array length, no smart fallback ever needed. Pure dumb mode with client-side search.
- **REFBACK with > nestedLimit rows**: first N prefetched, count shows total. Initial pages use prefetched data. Search or page-beyond triggers permanent smart fallback.
- **Empty REFBACK**: count = 0, empty array. Dumb DataList shows "No items".
- **No nestedLimit set (default)**: all rows fetched, `_agg{count}` still included. DataList knows total = array length, never needs smart fallback.
- **Smart fallback filter**: The REFBACK filter from `watchEffect` scopes queries to the parent record. Available after first metadata fetch (cached, fast).

## Verification

1. **curl test**: ✅ backend supports `(limit:N)` and `_agg{count}` on nested REFBACK fields
2. **Playwright**: load page, check only 1 SSR data query, no client-side re-fetches
3. **Playwright**: verify DataList shows correct total count in pagination
4. **Playwright**: search in a truncated REFBACK DataList triggers smart mode query
5. **Playwright**: navigate beyond prefetched pages triggers smart mode query
6. **Playwright**: small REFBACK (all data prefetched) — search works client-side, no server query
