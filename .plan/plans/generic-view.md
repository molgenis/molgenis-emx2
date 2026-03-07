# Generic View - Plan

## Completed

- v6.3.0-6.3.6: Filter system with URL sync (see spec for details)
- v6.3.7: Type refactoring - unified IDisplayConfig
- v6.3.8: ISectionColumn → ISectionField rename
- v6.3.9: ActiveFilters component
- v6.3.10: Theme-aware table, card styling, ActiveFilters bar
- v6.3.11: DetailPageLayout + responsive filters + SideModal
- v6.3.12: Toolbar, mobile improvements, RecordCard, Columns component
- v6.3.13: Row actions (edit/delete buttons, EditModal, DeleteModal)
- v6.3.14: Filter bug fixes, slot consolidation, URL roundtrip fixes
- v6.3.15: Polish & integration (format, dead code cleanup, story fixes, useFilters story)

- v6.3.16: Show filter options with zero counts (removed nodeHasMatches hiding)

## Architecture

### Layout Structure

**Page level (DetailPageLayout):**
```
┌─────────────────────────────────────────────────────┐
│ Header (slot) - PageHeader with breadcrumbs         │
├─────────────────────────────────────────────────────┤
│ ┌──────────┐  ┌────────────────────────────────────┐│
│ │ Sidebar  │  │ Main (slot)                        ││
│ │ (slot)   │  │                                    ││
│ │ optional │  │                                    ││
│ └──────────┘  └────────────────────────────────────┘│
└─────────────────────────────────────────────────────┘
```

**Emx2DataView (headerless, in main slot):**
```
┌──────────┐  ┌────────────────────────────────────┐
│ Filter   │  │ Content Card                       │
│ Sidebar  │  │ ┌────────────────────────────────┐ │
│ [Search] │  │ │ ActiveFilters (if any)         │ │
│ Filter 1 │  │ ├────────────────────────────────┤ │
│ Filter 2 │  │ │ Table (full width)             │ │
│ ...      │  │ ├────────────────────────────────┤ │
│          │  │ │ Pagination                     │ │
└──────────┘  └────────────────────────────────────┘
```

### Component Props

**Emx2DataView:**
```typescript
{ schemaId: string; tableId: string; config?: IDisplayConfig; isEditable?: boolean }
```

**Emx2ListView:**
```typescript
{ schemaId: string; tableId: string; config?: IDisplayConfig; filter?: object }
```

**Emx2RecordView:**
```typescript
{ schemaId: string; tableId: string; rowId: Record<string, any>; config?: IDisplayConfig }
```

### Theme Guidelines

| Use | Instead of |
|-----|------------|
| `bg-table` | `bg-white`, `bg-gray-50` |
| `border-black/10` | `border-gray-200` |
| `hover:bg-black/5` | `hover:bg-gray-50` |
| `text-body-base` | hardcoded text colors |
| `bg-content shadow-primary` | no container |

## v6.4.0: Subclass Columns & Filters (planned)

### Goal
When viewing a parent table, show columns from subclass tables and allow filtering on them.

### Context
- Backend GraphQL already supports querying subclass columns on parent tables (confirmed by `TestTableQueriesWithInheritance`)
- Backend `_schema` GraphQL exposes `inheritId` and `inheritName` on table type (lines 305-310 of `GraphqlSchemaFieldFactory.java`)
- Frontend doesn't query these fields → subclass columns invisible

### Approach: Frontend-only (no backend changes)

**Step 1: Add `inheritId` to metadata query**
- `apps/tailwind-components/app/gql/metadata.js` — add `inheritId` to tables fields
- `apps/metadata-utils/src/types.ts` — add `inheritId?: string` to `ITableMetaData`

**Step 2: Create `getSubclassColumns()` utility**
- New function in `apps/tailwind-components/app/composables/` or `metadata-utils`
- Input: `tableId`, `allTables` (from schema metadata)
- Algorithm:
  1. Find all tables where `inheritId === tableId` (direct subclasses)
  2. Recursively find their subclasses too
  3. For each subclass, collect columns NOT present in parent's column list
  4. Tag each with `sourceTableId` so UI can show provenance
  5. Return merged array: `[...parentColumns, ...subclassColumns]`
- Add `sourceTableId?: string` to `IColumn` type

**Step 3: Add `includeSubclassColumns` option to data layer**
- `fetchTableMetadata` — add option to merge subclass columns into returned metadata
  - Uses `getSubclassColumns()` + `fetchMetadata()` (has full schema with all tables)
  - Subclass columns added with `visible: "false"` (opt-in) and `sourceTableId` set
- `fetchTableData` / `getColumnIds` — same option, so GraphQL query includes subclass fields
- `useTableData` — pass option through to both fetchers
- Result: components receive merged columns automatically, no component-level logic needed

**Step 4: `Emx2DataView.vue` — enable the option**
- Pass `includeSubclassColumns: true` to `useTableData`
- No other changes needed — columns/filters/data all flow from metadata

**Step 5: UI polish in Columns selector**
- Group subclass columns by `sourceTableId` in the column/filter picker
- Show table label as section header (e.g., "Employee columns", "Manager columns")

### Files to modify
- [x] `apps/tailwind-components/app/gql/metadata.js` — add `inheritId`
- [x] `apps/metadata-utils/src/types.ts` — add `inheritId` to ITableMetaData, `sourceTableId` to IColumn
- [x] `apps/tailwind-components/app/composables/getSubclassColumns.ts` — NEW utility
- [x] `apps/tailwind-components/tests/vitest/getSubclassColumns.spec.ts` — 8 tests passing
- [ ] `apps/tailwind-components/app/composables/fetchTableMetadata.ts` — add includeSubclassColumns option
- [ ] `apps/tailwind-components/app/composables/fetchTableData.ts` / `getColumnIds` — add option
- [ ] `apps/tailwind-components/app/composables/useTableData.ts` — pass option through
- [ ] `apps/tailwind-components/app/components/display/Emx2DataView.vue` — enable option
- [ ] `apps/tailwind-components/app/components/table/control/Columns.vue` — group by sourceTableId
- [ ] Tests for fetchTableMetadata with subclass option

### Edge cases
- Multi-level inheritance (Manager → Employee → Person): recursive lookup
- Column name conflicts across subclasses: shouldn't happen (unique within schema)
- Null values: rows that aren't of the subclass type will have null for those columns — normal
- `fetchTableData` query builder already handles arbitrary column IDs → should just work

## Future

### Nested Sidebar Filters
- Click REF column → opens new FilterSidebar as slide-in panel
- Each nested sidebar is independent: just `schemaId` + `tableId`
- Reuses FilterSidebar with all features
- URL encoding TBD: `?Order.pet.name=Fluffy` or `?_filter[pet][name]=Fluffy`

### Other Ideas
- staticFilter/defaultFilter concept
- Custom column labels

## Test Coverage
385 passing tests (v6.3.15, 46 files)
