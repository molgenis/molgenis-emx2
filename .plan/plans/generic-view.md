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

### Done (v6.3.12) - Toolbar & Mobile Improvements
- [x] Desktop toolbar: Add button, Show/Hide Filters toggle, Columns button
- [x] Mobile toolbar: Add button, Filters button (modal), Columns button
- [x] FilterSidebar: Customize button (uses Columns component with custom label/icon)
- [x] Columns component: flexible props (label, icon, buttonType, size)
- [x] Responsive table/cards: table at md+ (768px), cards below
- [x] Responsive filters: sidebar at xl+ (1280px), mobile toolbar below
- [x] RecordCard component: mobile-friendly card using ValueEMX2
- [x] isEditable prop: shows Add button when true
- [x] Settings icon added
- [x] 15 vitest tests for Emx2DataView

### Done (v6.3.13) - Row Actions
- [x] Row edit button (per row, when isEditable) → opens EditModal
- [x] Row delete button (per row, when isEditable) → opens DeleteModal
- [x] Button placement: first column (sticky)
- [x] Mobile: actions in RecordCard via #actions slot

### Done (v6.3.14) - Filter Bug Fixes & Slot Consolidation
- [x] Filterable types: everything except HEADING and SECTION
- [x] FILE type: filters on `name` field (like operator)
- [x] REF/SELECT/ONTOLOGY types: use "equals" operator (not "like")
- [x] refLabel fallback: `column.refLabel || column.refLabelDefault`
- [x] URL roundtrip: REF filters stored with column ID (not `column.name`)
- [x] URL filters apply on initial load (watch columns.length)
- [x] Merged #card and #default slots into single #default slot

### Next: Polish & Integration

- [ ] Run lint/format
- [ ] Verify stories work in all themes
- [ ] Review Emx2ListView/Emx2RecordView - align with IDisplayConfig
- [ ] Clean up unused code/imports

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

### Future: Nested Sidebar Filters

**Concept:** Instead of inline nested expansion, use literal nested sidebars.

**Approach:**
- Click REF column → opens new FilterSidebar as slide-in panel
- Each nested sidebar is independent: just `schemaId` + `tableId`
- Reuses FilterSidebar with all features (Customize, mg_ hidden, etc.)
- Can stack multiple levels (breadcrumb-style navigation?)

**Benefits:**
- Simpler: no complex metadata passing through props
- Reusable: each sidebar is a standard FilterSidebar
- Consistent: same UX as main filter sidebar

**URL encoding TBD:**
- Prefix approach: `?Order.pet.name=Fluffy`
- Or nested params: `?_filter[pet][name]=Fluffy`

### Maybe Future
- staticFilter/defaultFilter concept
- Custom column labels

## Test Coverage
234 passing tests (v6.3.7)
