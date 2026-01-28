# Generic View - Plan

## Completed
- v6.3.0-6.3.6: Filter system with URL sync (see spec for details)
- v6.3.7: Type refactoring - unified IDisplayConfig
- v6.3.8: ISectionColumn → ISectionField rename
- v6.3.9: ActiveFilters component

## Current: Emx2DataView Layout Refinements

### Done (v6.3.10)
- [x] Theme-aware table colors (bg-table, border-black/10, hover:bg-black/5)
- [x] Card container with shadow
- [x] Title/description moved outside card (page header style)
- [x] CardList/CardListItem improved styling
- [x] ActiveFilters bar integrated
- [x] Default layout = "table"
- [x] Table full-width (no padding wrapper)

### Remaining Steps

| Step | Task | Files |
|------|------|-------|
| 1 | Move search into FilterSidebar when showFilters + sidebar | FilterSidebar.vue, Emx2DataView.vue |
| 2 | Keep search in content area when no sidebar | Emx2DataView.vue |
| 3 | Ensure sidebar aligns with content card top | Emx2DataView.vue |
| 4 | Update story to demo sidebar with search | Emx2DataView.story.vue |

### Layout Structure

```
┌─────────────────────────────────────────────────────┐
│ Title + Description (page header)                   │
├─────────────────────────────────────────────────────┤
│ ┌──────────┐  ┌────────────────────────────────────┐│
│ │ Sidebar  │  │ Content Card                       ││
│ │          │  │ ┌────────────────────────────────┐ ││
│ │ [Search] │  │ │ ActiveFilters (if any)         │ ││
│ │          │  │ ├────────────────────────────────┤ ││
│ │ Filter 1 │  │ │ Table (full width)             │ ││
│ │ Filter 2 │  │ │                                │ ││
│ │ ...      │  │ ├────────────────────────────────┤ ││
│ └──────────┘  │ │ Pagination                     │ ││
│               │ └────────────────────────────────┘ ││
│               └────────────────────────────────────┘│
└─────────────────────────────────────────────────────┘
```

### Theme Guidelines

| Use | Instead of |
|-----|------------|
| `bg-table` | `bg-white`, `bg-gray-50` |
| `border-black/10` | `border-gray-200` |
| `hover:bg-black/5` | `hover:bg-gray-50` |
| `text-body-base` | hardcoded text colors |
| `bg-content shadow-primary` | no container |

---

## Reference: IDisplayConfig

```typescript
export interface IDisplayConfig {
  // Display mode
  layout?: "table" | "list" | "cards";
  displayComponent?: string | Component;

  // Columns
  visibleColumns?: string[];
  columnConfig?: Record<string, IDisplayConfig>;  // recursive

  // Data
  pageSize?: number;
  showEmpty?: boolean;
  rowLabel?: string;

  // Interaction
  clickAction?: (col: IColumn, row: IRow) => void;
  getHref?: (col: IColumn, row: IRow) => string;

  // Filters
  showFilters?: boolean;
  filterPosition?: "sidebar" | "topbar";
  filterableColumns?: string[];
  showSearch?: boolean;
}
```

Usage:
- **IColumn.displayConfig**: IDisplayConfig (already named this way)
- **Component config prop**: IDisplayConfig
- **columnConfig["colId"]**: IDisplayConfig (recursive for refs)

### Implementation Steps

| Step | Task | Files |
|------|------|-------|
| 1 | Expand IDisplayConfig in metadata-utils/types.ts | types.ts |
| 2 | Remove refColumn prop from RecordTableView | RecordTableView.vue |
| 3 | Update Emx2DataView: use config prop, extract values | Emx2DataView.vue |
| 4 | Update Emx2ListView: use config prop | Emx2ListView.vue |
| 5 | Update Emx2RecordView: use config prop | Emx2RecordView.vue |
| 6 | Update EMX2.vue: align with IDisplayConfig | EMX2.vue |
| 7 | Move story filter/ → display/ | pages/ |
| 8 | Remove IColumnDisplayOptions (merged into IDisplayConfig) | Emx2DataView.vue |
| 9 | Fix package.json ^ versions | package.json |
| 10 | Update/fix tests | tests/ |

### Component Props After Refactor

**Emx2DataView:**
```typescript
{
  schemaId: string;
  tableId: string;
  config?: IDisplayConfig;
  urlSync?: boolean;
}
```

**Emx2ListView:**
```typescript
{
  schemaId: string;
  tableId: string;
  config?: IDisplayConfig;
  filter?: object;  // external filter (REFBACK)
}
```

**Emx2RecordView:**
```typescript
{
  schemaId: string;
  tableId: string;
  rowId: Record<string, any>;
  config?: IDisplayConfig;
}
```

### Maybe Future
- staticFilter/defaultFilter concept
- Custom column labels

## Test Coverage
234 passing tests (v6.3.7)
