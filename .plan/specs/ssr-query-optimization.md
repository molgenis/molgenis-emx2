# Spec: SSR query optimization — metadata caching + prefetch for empty REFBACKs

## Problem

Profiling the catalogue detail page SSR reveals two issues:

### Issue 1: ~80 duplicate metadata fetches on SSR

The `fetchMetadata` composable uses `useSessionStorage` for caching and an `inflight` Map for deduplication. Neither works properly on SSR:

- `useSessionStorage` → returns null on server (no sessionStorage in Node)
- `inflight` Map → the `.finally()` clause deletes the entry after the promise resolves. Subsequent calls (from components rendered after the first batch) find an empty Map and fetch again.

**Result**: every call to `fetchMetadata` during SSR triggers a new HTTP request. With `getColumnIds` calling it recursively for each ref column schema, this adds up to ~80 fetches of the same 2 schemas.

### Issue 2: Smart DataList queries for empty REFBACK columns

The `usePrefetchedData` computed in DetailColumn requires `props.value.length > 0`. REFBACK columns with no data (empty array `[]`) fail this check and fall through to the smart DataList path, which fires a separate query just to confirm there are 0 results.

The main query (with expandLevel=2) already returns `[]` for empty REFBACK columns. We should trust that result instead of re-querying.

## Profiling results (current state)

```
fetchMetadata catalogue-demo:        ~80 calls × 20-110ms each
fetchMetadata CatalogueOntologies:   ~3 calls × 23-31ms each
fetchTableData Resources (main):     807ms
fetchTableData InternalIdentifiers:  1320ms
fetchTableData ExternalIdentifiers:  1320ms
fetchTableData Publications:         1292ms
fetchTableData Documentation:        1293ms
fetchTableData Datasets:             936ms
fetchTableData Contacts:             684ms
fetchTableData Organisations (×2):   662ms
fetchTableData Subpopulations:       601ms
fetchTableData CollectionEvents:     601ms
fetchTableData Resources (×2):       1251ms
```

## Fix 1: SSR metadata caching

### File: `app/composables/fetchMetadata.ts`

Add a module-level `resolved` Map that caches the actual result (not just the in-flight promise). Check it before sessionStorage and before inflight.

**Before:**
```ts
const inflight = new Map<string, Promise<ISchemaMetaData>>();

export default async (schemaId: string): Promise<ISchemaMetaData> => {
  const cached = useSessionStorage(...);
  if (cached.value) return cached.value;

  if (!inflight.has(schemaId)) {
    const promise = $fetch(...)
      .then(({ data }) => { cached.value = data._schema; return data._schema; })
      .finally(() => { inflight.delete(schemaId); });
    inflight.set(schemaId, promise);
  }
  return inflight.get(schemaId)!;
};
```

**After:**
```ts
const inflight = new Map<string, Promise<ISchemaMetaData>>();
const resolved = new Map<string, ISchemaMetaData>();

export default async (schemaId: string): Promise<ISchemaMetaData> => {
  if (resolved.has(schemaId)) return resolved.get(schemaId)!;

  const cached = useSessionStorage(...);
  if (cached.value) return cached.value;

  if (!inflight.has(schemaId)) {
    const promise = $fetch(...)
      .then(({ data }) => {
        resolved.set(schemaId, data._schema);
        cached.value = data._schema;
        return data._schema as ISchemaMetaData;
      })
      .catch(...)
      .finally(() => { inflight.delete(schemaId); });
    inflight.set(schemaId, promise);
  }
  return inflight.get(schemaId)!;
};
```

**Why this works:**
- SSR: `resolved` Map is module-level, persists across calls within the same SSR render (and across requests — fine for schema metadata which is stable)
- Client: `sessionStorage` check still fires first (populated from SSR payload via Nuxt), `resolved` Map serves as fallback

**Expected impact:** ~80 metadata fetches → 2 (one per schema, first call only)

## Fix 2: Accept empty arrays as valid prefetched data

### File: `app/components/display/DetailColumn.vue`

Change `usePrefetchedData` to accept empty arrays:

**Before:**
```ts
const usePrefetchedData = computed(() => {
  const type = props.column.columnType;
  if (type !== "REFBACK" && !isRefArrayColumn(type)) return false;
  if (!Array.isArray(props.value) || props.value.length === 0) return false;
  if (!props.column.refTableId || !props.schemaId) return false;
  return true;
});
```

**After:**
```ts
const usePrefetchedData = computed(() => {
  const type = props.column.columnType;
  if (type !== "REFBACK" && !isRefArrayColumn(type)) return false;
  if (!Array.isArray(props.value)) return false;
  if (!props.column.refTableId || !props.schemaId) return false;
  return true;
});
```

**Why this is safe:**
- The main query with expandLevel=2 includes all REFBACK columns at root level
- Empty REFBACK = `[]` in the response (not `undefined`)
- `undefined`/`null` value → `Array.isArray` returns false → falls through to smart DataList (correct fallback)
- Empty array → dumb DataList with 0 rows → renders "No items" (same as smart DataList would)

**Expected impact:** ~13 data queries → 1 (only the main Resources query)

## Verification

### Test 1: Add temporary timing instrumentation

In `fetchMetadata.ts`, add before the `if (resolved.has...)`:
```ts
console.log(`[TIMING] fetchMetadata ${schemaId} (resolved: ${resolved.has(schemaId)}, cached: ${!!cached.value}, inflight: ${inflight.has(schemaId)})`);
```

In `fetchTableData.ts`, add around the `$fetch`:
```ts
const t0 = Date.now();
// ... existing fetch ...
console.log(`[TIMING] fetchTableData ${schemaId}/${tableId}: ${Date.now() - t0}ms`);
```

### Test 2: Load the page with Playwright, check console

```bash
playwright-cli open http://localhost:3001/samples/catalogue/testCohort1
playwright-cli console
```

**Expected after both fixes:**
- `fetchMetadata` logs: 2 with `resolved: false` (first fetch), rest with `resolved: true` (cache hit)
- `fetchTableData` logs: only 1 entry for `Resources` (main query) + 1 for resource picker
- Load time: ~2-3s (down from ~5s)

### Test 3: Verify page renders correctly

```bash
playwright-cli snapshot
# Check subpopulations and collection events are present with cursor=pointer
grep -i "subcohort\|collection event" <snapshot>.yml
# Click a subpopulation to verify navigation
playwright-cli click <ref>
# Verify URL changed to /subpopulations/...
```

### Test 4: Remove timing instrumentation

Revert the console.log lines added in Test 1.
