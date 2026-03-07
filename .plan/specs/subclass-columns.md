# Spec: Subclass Columns

## Feature

Parent table views show columns from subclass (child) tables and allow filtering on them. Implemented frontend-only — no backend changes required.

## Invariants

Rules that must always hold. Do not break these.

| # | Rule |
|---|------|
| 1 | `_schema` GQL query (`metadata.js`) must include `inheritId` on tables |
| 2 | `ITableMetaData` must have `inheritId?: string` |
| 3 | `IColumn` must have `sourceTableId?: string` |
| 4 | `getSubclassColumns()` must recursively find all descendant subclass columns not already in parent |
| 5 | Each subclass column must be tagged with `sourceTableId` set to the subclass table id it came from |
| 6 | Columns that appear in multiple sibling subclasses must be deduplicated (first-wins) |
| 7 | Subclass columns added to parent metadata must default to `visible: "false"` (user opts in) |

## Architecture

```
_schema GQL query (metadata.js)
  → includes inheritId on tables
  → ITableMetaData.inheritId available in allTables

getSubclassColumns(tableId, allTables)
  → finds direct subclasses (t.inheritId === tableId)
  → recurses into their subclasses
  → collects columns not in parent
  → tags each with sourceTableId
  → returns IColumn[] (subclass-only, deduplicated)

fetchTableMetadata(schemaId, tableId, { includeSubclassColumns: true })
  → calls fetchMetadata(schemaId) to get full schema
  → calls getSubclassColumns() with full allTables
  → merges subclass columns (with visible: "false") into returned metadata

useTableData(IQueryMetaData)
  → IQueryMetaData.includeSubclassColumns threads option to fetchTableMetadata and getColumnIds
  → IQueryMetaData.columns carries visible column list for GQL query construction
  → GraphQL query then includes subclass fields automatically

Emx2DataView.vue
  → passes includeSubclassColumns: true to useTableData
  → no column-level logic needed — all flows from metadata

Columns.vue (filter/column picker)
  → groups subclass columns by sourceTableId
  → shows table label as section header (e.g. "Employee columns")
```

## Test Locations

| Layer | File |
|-------|------|
| Unit: `getSubclassColumns` | `apps/tailwind-components/tests/vitest/getSubclassColumns.spec.ts` |
| Unit: `fetchTableMetadata` with subclass option | `apps/tailwind-components/tests/vitest/fetchTableMetadata.spec.ts` |

### Test cases covered (getSubclassColumns)

- Returns `[]` when table has no subclasses
- Returns `[]` when tableId not found
- Returns subclass-only columns tagged with correct `sourceTableId`
- Does not return columns already present in parent
- Collects columns from multi-level hierarchy (grandchild)
- Tags deep subclass columns with their own `sourceTableId` (not grandparent)
- Collects columns from multiple sibling subclasses
- Deduplicates columns when same id appears in multiple siblings

### Test cases covered (fetchTableMetadata)

- Returns table metadata for valid tableId
- Rejects when tableId not found
- Without `includeSubclassColumns`, returns only the table's own columns
- With `includeSubclassColumns: true`, returns parent columns + subclass columns
- Subclass columns have `visible: "false"` set
- Subclass columns have `sourceTableId` set

## Files Involved

| Status | File | Role |
|--------|------|------|
| done | `apps/tailwind-components/app/gql/metadata.js` | Adds `inheritId` to GQL `_schema` tables query |
| done | `apps/metadata-utils/src/types.ts` | `ITableMetaData.inheritId?: string`, `IColumn.sourceTableId?: string` |
| done | `apps/metadata-utils/src/IQueryMetaData.ts` | `IQueryMetaData` interface — `includeSubclassColumns`, `columns`, threading options |
| done | `apps/tailwind-components/app/composables/getSubclassColumns.ts` | Core utility — recursive subclass column lookup |
| done | `apps/tailwind-components/tests/vitest/getSubclassColumns.spec.ts` | 8 unit tests |
| done | `apps/tailwind-components/app/composables/fetchTableMetadata.ts` | `includeSubclassColumns` option, merges + sets `visible: "false"` |
| done | `apps/tailwind-components/tests/vitest/fetchTableMetadata.spec.ts` | 6 unit tests |
| done | `apps/tailwind-components/app/composables/fetchTableData.ts` | Pass subclass column ids into GQL query |
| done | `apps/tailwind-components/app/composables/useTableData.ts` | Thread option to fetchers |
| done | `apps/tailwind-components/app/components/display/Emx2DataView.vue` | Enable `includeSubclassColumns: true` |
| done | `apps/tailwind-components/app/components/table/control/Columns.vue` | Group columns by `sourceTableId` in picker |

## Edge Cases

- **Multi-level hierarchy**: Manager → Employee → Person — `getSubclassColumns("Person")` returns both `salary` (Employee) and `department` (Manager) with correct `sourceTableId` on each.
- **Column name conflicts across siblings**: first subclass wins; duplicate id skipped.
- **Null values in data**: rows not of the subclass type will have null for subclass columns — expected, no special handling needed.
- **`fetchTableData` query builder**: already handles arbitrary column ids — subclass columns work without changes.
