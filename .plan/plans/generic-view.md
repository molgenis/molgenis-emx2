# Generic View - Plan

## Completed
- v6.3.0-6.3.6: Filter system with URL sync (see spec for details)
- v6.3.7: Type refactoring - unified IDisplayConfig
- v6.3.8: ISectionColumn → ISectionField rename
- v6.3.9: ActiveFilters component

## Current: Component Architecture Refactor

### Done (v6.3.10)
- [x] Theme-aware table colors (bg-table, border-black/10, hover:bg-black/5)
- [x] Card container with shadow
- [x] CardList/CardListItem improved styling
- [x] ActiveFilters bar integrated
- [x] Default layout = "table"
- [x] Table full-width (no padding wrapper)

### Done (v6.3.11) - DetailPageLayout + Responsive Filters
- [x] Emx2DataView uses DetailPageLayout internally
- [x] #header slot (optional - compact when empty)
- [x] FilterSidebar in DetailPageLayout #sidebar slot
- [x] Mobile filter button + SideModal (xl:hidden)
- [x] showFilters=false: no sidebar, no mobile button (vanilla mode)
- [x] Stories updated (Full Page, Compact, Vanilla modes)

### Next: Consolidation & Polish

Potential tasks (pick based on priority):
- [ ] Run tests, fix any failures
- [ ] Run lint/format
- [ ] Verify stories work in all themes
- [ ] Review Emx2ListView/Emx2RecordView - align with IDisplayConfig if needed
- [ ] Clean up unused code/imports
- [ ] Update test coverage number

### Target Layout Structure

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
