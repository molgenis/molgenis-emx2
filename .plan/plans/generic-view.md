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
