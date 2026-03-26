# Detail View Like Catalogue — Plan

## Goal
Replace handcrafted catalogue detail pages (1000+ line bespoke Vue files) with a **config-driven system** using generic building blocks in tailwind-components. Same visual quality, 90% less page-specific code.

Two consumers:
1. **`apps/ui` entity page** — generic detail view for any EMX2 table
2. **`apps/catalogue` detail pages** — rich, customized views (datasets, resources, variables)

## Key Design Decisions

### Dumb/Smart Split
- **RecordView** (dumb) — takes `columns: IColumnDisplay[]` + `data: Record<string, any>`, does all rendering. No backend calls. Testable with mock data.
- **Emx2RecordView** (smart) — fetches metadata + row from backend, transforms IColumn → IColumnDisplay, passes to RecordView.

### Extended Column Type for Layout
IColumn comes from backend metadata. We extend it **client-side only** with display hints into `IColumnDisplay`:

```
IColumn (from backend)     IColumnDisplay (client-side extended)
├── id                     ├── ...all IColumn fields
├── columnType             ├── component?: Component        // custom renderer
├── label                  ├── layout?: "inline"|"block"|"full"  // width hint
├── tags                   ├── getHref?: (col, row) => string
├── section/heading        ├── clickAction?: (col, row) => void
└── ...                    ├── listConfig?: IListConfig     // for REF_ARRAY/REFBACK
                           └── hidden?: boolean
```

This means:
- Backend stays clean (just IColumn + tags)
- `displayMap` maps tags → components (as before)
- `columnConfig` maps column ids → display overrides
- Emx2RecordView merges both into IColumnDisplay[] before passing to RecordView

### Master/Detail via ListConfig
REF_ARRAY/REFBACK columns need embedded list views. This is configured per-column:

```typescript
interface IListConfig {
  layout?: "table" | "cards" | "list";
  component?: Component;           // card component
  visibleColumns?: string[];
  pageSize?: number;
  showSearch?: boolean;
  showFilters?: boolean;
  getHref?: (col: IColumn, row: IRow) => string;
  filter?: object;                 // additional static filter
}
```

### Tags → Component Resolution (unchanged)
```
Column.tags = ["ontology-tree"]  →  displayMap["ontology-tree"]  →  OntologyTreeDisplay
No match  →  ValueEMX2 (default)
```

## Architecture

### Component Hierarchy
```
Emx2RecordView (smart wrapper)
  ├── fetches metadata + row data
  ├── merges IColumn + columnConfig + displayMap → IColumnDisplay[]
  └── passes to RecordView

RecordView (dumb, pure rendering)
  ├── groups columns by SECTION/HEADING
  ├── generates SideNav sections
  └── renders:
      DetailPageLayout
        ├── SideNav (from sections)
        └── RecordSection[] (one per section)
              └── RecordColumn[] (one per field)
                    ├── custom component (if column.component set)
                    ├── Emx2ListView (if REF_ARRAY/REFBACK + listConfig)
                    └── ValueEMX2 (default fallback)

Emx2ListView (smart list for related data)
  ├── fetches paginated data for ref table
  ├── search, pagination, filters
  └── renders as table/cards/list per config
```

### Page Anatomy
```
┌─────────────────────────────────────────────────────┐
│ #header slot (breadcrumbs, title, edit/delete)       │
├─────────────────────────────────────────────────────┤
│ ┌──────────┐  ┌────────────────────────────────────┐│
│ │ SideNav  │  │ Sections (from SECTION columns)    ││
│ │ (sticky) │  │                                    ││
│ │ - Sec 1  │  │ ┌────────────────────────────────┐ ││
│ │ - Sec 2  │  │ │ Section: def-list (key/value)  │ ││
│ │ - Sec 3  │  │ ├────────────────────────────────┤ ││
│ │ - Vars   │  │ │ Section: ontology-tree column  │ ││
│ │          │  │ ├────────────────────────────────┤ ││
│ │          │  │ │ Section: Emx2ListView (table)  │ ││
│ │          │  │ │ (paginated + search + links)   │ ││
│ │          │  │ ├────────────────────────────────┤ ││
│ │          │  │ │ Section: card-grid column      │ ││
│ └──────────┘  └────────────────────────────────────┘│
├─────────────────────────────────────────────────────┤
│ #footer slot (extra sections, related data)          │
└─────────────────────────────────────────────────────┘
```

### Catalogue Usage Example
```vue
<!-- Dataset detail page: ~40 lines instead of 1000+ -->
<Emx2RecordView
  :schema-id="schema"
  table-id="Datasets"
  :row-id="rowId"
  :column-config="{
    resource: {
      getHref: (col, row) => `/${catalogue}/collections/${row.id}`,
    },
  }"
>
  <template #header>
    <BreadCrumbs :crumbs="crumbs" />
    <PageHeader :title="datasetName" />
  </template>

  <template #footer>
    <!-- Extra section for related Variables table -->
    <Emx2DataView
      :schema-id="schema"
      table-id="Variables"
      :config="variablesConfig"
      :static-filter="variablesFilter"
    />
  </template>
</Emx2RecordView>
```

## What Current Catalogue Pages Need

Analysis of master catalogue detail pages reveals these patterns:

| Pattern | Catalogue Example | Generic Solution |
|---------|------------------|-----------------|
| Key/value pairs | Name, acronym, dates | Default RecordSection (def-list) |
| Ontology trees | Data categories, conditions | `ontology-tree` tag → OntologyTreeDisplay |
| Related table with search | Variables, subpopulations | `listConfig` on REF_ARRAY column |
| Card grid | Networks, publications | `card-grid` tag → CardGridDisplay |
| File downloads | Documentation files | `file-list` tag → FileListDisplay |
| Hero intro | Logo, website, contact | `intro` tag → IntroDisplay |
| Conditional sections | Skip empty sections | Already supported (showEmpty=false) |
| Sidebar TOC | Section navigation | Auto-generated from SECTION columns |
| Custom links | Navigate to sub-pages | `getHref` in columnConfig |
| Harmonisation grid | Variable harmonisation status | Custom component via displayMap (catalogue-specific) |

## Phases

### Phase 1: Foundation (done)
- [x] Copy core display components from feat/generic-view
- [x] Add `tags?: string[]` to IColumn
- [x] Add `displayMap` prop chain (RecordView → RecordSection → RecordColumn)
- [x] Copy story files from feat/generic-view

### Phase 2: IColumnDisplay + Dumb RecordView Refactor
- [ ] Define IColumnDisplay interface extending IColumn with display hints
- [ ] RecordView takes `columns: IColumnDisplay[]` + `data` (not metadata + row)
- [ ] RecordSection/RecordColumn work with IColumnDisplay
- [ ] Emx2RecordView merges IColumn + columnConfig + displayMap → IColumnDisplay[]
- [ ] Update story files to test dumb RecordView with mock data

### Phase 3: List Components (Emx2ListView + Emx2DataView)
- [ ] Copy Emx2ListView from generic-view, adapt to IColumnDisplay
- [ ] Copy Emx2DataView from generic-view, adapt
- [ ] RecordColumn renders Emx2ListView for REF_ARRAY/REFBACK when listConfig set
- [ ] InlinePagination for compact lists
- [ ] Story files for list views

### Phase 4: Standard Display Components
- [ ] OntologyTreeDisplay — collapsible tree for ONTOLOGY columns
- [ ] CardGridDisplay — ref items as cards with links
- [ ] FileListDisplay — file download cards
- [ ] IntroDisplay — hero block (logo, website, contact)
- [ ] Export defaultDisplayMap from tailwind-components

### Phase 5: Upgrade [entity].vue
- [ ] Replace 220-line bespoke page with Emx2RecordView
- [ ] Keep edit/delete buttons via #header slot
- [ ] Keep field search filter
- [ ] Test with various table shapes

### Phase 6: Catalogue Demo
- [ ] Dataset detail page using Emx2RecordView + Emx2DataView
- [ ] Resource/collection detail page (the big one)
- [ ] Visual comparison with handcrafted pages
- [ ] Theme testing (Light, Dark, Molgenis, UMCG, AUMC)
- [ ] Responsive testing (desktop, tablet, mobile)

### Phase 7: Tests & Polish
- [ ] Unit tests for display components
- [ ] Story files for all components
- [ ] E2E smoke test

## Files

### New/Modified in tailwind-components
- `types/types.ts` — IColumnDisplay, IListConfig interfaces
- `display/RecordView.vue` — refactor to take IColumnDisplay[]
- `display/RecordSection.vue` — use IColumnDisplay
- `display/RecordColumn.vue` — use IColumnDisplay, render Emx2ListView
- `display/Emx2RecordView.vue` — merge logic, columnConfig prop
- `display/Emx2ListView.vue` — copy from generic-view
- `display/Emx2DataView.vue` — copy from generic-view
- `display/OntologyTreeDisplay.vue` — new
- `display/CardGridDisplay.vue` — new
- `display/FileListDisplay.vue` — new
- `display/IntroDisplay.vue` — new

### Modified in apps/ui
- `pages/[schema]/[table]/[entity].vue` — simplify to use Emx2RecordView

### New in apps/catalogue (demo)
- `pages/[catalogue]/datasets/[resource]/[name]/index.vue`
