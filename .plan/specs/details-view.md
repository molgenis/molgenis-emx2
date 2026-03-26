# Detail View Like Catalogue — Spec

## Core Types

### IColumnDisplay (extends IColumn)

Client-side extension of backend IColumn with display hints. RecordView (dumb) works exclusively with this type. Defined in `tailwind-components/types/types.ts`.

```typescript
export interface IColumnDisplay extends IColumn {
  // Custom renderer component (set directly, or resolved from tags via displayMap in Emx2RecordView)
  displayComponent?: Component;

  // Layout hint for how this column occupies space in a section
  // "inline" = definition list key/value (default for scalars)
  // "block" = full-width below definition list (default for REF_ARRAY/REFBACK)
  // "full" = full-width, no section container (hero/intro blocks)
  layout?: "inline" | "block" | "full";

  // Navigation: make the value a clickable link
  getHref?: (col: IColumn, row: IRow) => string;

  // Navigation: custom click handler
  clickAction?: (col: IColumn, row: IRow) => void;

  // For REF_ARRAY/REFBACK: how to render the related list
  listConfig?: IListConfig;

  // Override label from metadata
  displayLabel?: string;

  // Hide this column entirely
  hidden?: boolean;
}
```

### IListConfig

Configuration for rendering REF_ARRAY/REFBACK columns as embedded lists/tables.

```typescript
export interface IListConfig {
  layout?: "table" | "cards" | "list";
  component?: Component;
  visibleColumns?: string[];
  pageSize?: number;
  showSearch?: boolean;
  showFilters?: boolean;
  getHref?: (col: IColumn, row: IRow) => string;
  filter?: object;
  rowLabel?: string;
}
```

### IRecordViewConfig

Top-level config passed to Emx2RecordView (smart wrapper).

```typescript
export interface IRecordViewConfig {
  // Per-column display overrides, keyed by column.id
  columnConfig?: Record<string, Partial<IColumnDisplay>>;

  // Show columns with empty values (default: false)
  showEmpty?: boolean;

  // Show mg_ prefixed metadata columns (default: false)
  showMgColumns?: boolean;

  // Column IDs to show, in order. Omit = show all.
  visibleColumns?: string[];

  // Extra columns not in metadata (e.g., computed display-only columns)
  extraColumns?: IColumnDisplay[];

  // Show sidebar navigation (default: true when sections exist)
  showSideNav?: boolean;
}
```

## Component Interfaces

### RecordView (dumb)

Pure rendering component. No backend calls. No displayMap — columns already have displayComponent set.

```typescript
// Props
{
  columns: IColumnDisplay[];           // extended column definitions with displayComponent already resolved
  data: Record<string, any>;           // row data
  showEmpty?: boolean;                 // default: false
}

// Slots
#header    — above all sections (breadcrumbs, title, actions)
#footer    — below all sections (extra content, related tables)
```

**Behavior:**
1. Groups columns by SECTION/HEADING (using columnType)
2. Filters empty values (unless showEmpty)
3. Filters hidden columns
4. Generates SideNav sections from SECTION columns
5. Renders DetailPageLayout with SideNav + RecordSection per group

### Emx2RecordView (smart wrapper)

Fetches data, builds IColumnDisplay[], delegates to RecordView.

```typescript
// Props
{
  schemaId: string;
  tableId: string;
  rowId: Record<string, any>;
  config?: IRecordViewConfig;
  displayMap?: Record<string, Component>;  // tag → component mapping, resolved here, NOT passed down
}

// Slots — passed through to RecordView
#header, #footer
```

**Merge logic (IColumn → IColumnDisplay):**
```
1. Fetch metadata → IColumn[]
2. Filter by visibleColumns (if set), reorder
3. Filter mg_ columns (unless showMgColumns)
4. For each column:
   a. Apply columnConfig[column.id] overrides (spread)
   b. If no displayComponent yet, resolve tags against displayMap → set displayComponent
   c. Auto-detect layout if not set:
      - REFBACK/REF_ARRAY → "block"
      - Everything else → "inline"
   d. If REF_ARRAY/REFBACK and no listConfig, set default listConfig
5. Append extraColumns
6. Fetch row data
7. Pass columns + data to RecordView
```

### RecordSection

```typescript
// Props
{
  heading?: IColumn | null;
  isSection?: boolean;
  columns: { meta: IColumnDisplay; value: any }[];
  showEmpty?: boolean;
}
```

**Rendering:**
- "inline" columns → DefinitionList (dt/dd grid)
- "block" columns → full-width below definition list
- "full" columns → break out of section container

### RecordColumn

```typescript
// Props
{
  column: IColumnDisplay;
  value: any;
  showEmpty?: boolean;
}
```

**Resolution order:**
1. If `column.hidden` → render nothing
2. If empty and !showEmpty → render nothing
3. If `column.displayComponent` → render that component with `{column, value, showEmpty}`
4. Fallback → ValueEMX2

## Display Components (tag-driven, resolved in Emx2RecordView)

All display components receive standardized props:

```typescript
interface DisplayComponentProps {
  column: IColumnDisplay;
  value: any;
  showEmpty?: boolean;
}
```

### Built-in Display Map

| Tag | Component | When to Use |
|-----|-----------|-------------|
| `ontology-tree` | OntologyTreeDisplay | ONTOLOGY_ARRAY with hierarchy |
| `card-grid` | CardGridDisplay | REF_ARRAY shown as visual cards |
| `file-list` | FileListDisplay | FILE columns as download list |
| `intro` | IntroDisplay | Hero section (logo, website, contact) |

### Usage: displayMap on Emx2RecordView resolves tags → displayComponent during merge

```vue
<Emx2RecordView
  :schema-id="schema"
  table-id="Resources"
  :row-id="rowId"
  :display-map="{ ...defaultDisplayMap, 'harmonisation-grid': HarmonisationGrid }"
  :config="{ columnConfig: { logo: { displayComponent: IntroDisplay } } }"
/>
```

Note: `columnConfig` displayComponent takes precedence over tag-based resolution.

## Theme Compatibility

| Use | Class |
|-----|-------|
| Section background | `bg-content shadow-primary` |
| Section headings | `text-record-heading` |
| Sub-headings | `text-record-heading text-xl` |
| Definition labels | `text-record-label` |
| Definition values | `text-record-value` |
| Links | `text-link` |
| Cards | `bg-content shadow-primary` |
| Hover states | `hover:bg-black/5` |
| Borders | `border-black/10` |
| Gradient backgrounds | `text-title` |
| Content area text | `text-title-contrast` |

**Pitfall:** Never use `text-title` on `bg-content` — invisible in Molgenis/AUMC themes.

## Usage Examples

### Generic entity page (apps/ui)
```vue
<Emx2RecordView
  :schema-id="schemaId"
  :table-id="tableId"
  :row-id="rowId"
>
  <template #header>
    <BreadCrumbs :crumbs="crumbs" />
    <PageHeader :title="entityId" />
    <Button v-if="canEdit" @click="showEditModal = true">Edit</Button>
  </template>
</Emx2RecordView>
```

### Catalogue dataset page
```vue
<Emx2RecordView
  :schema-id="schema"
  table-id="Datasets"
  :row-id="rowId"
  :config="{
    columnConfig: {
      resource: {
        getHref: (col, row) => `/${catalogue}/collections/${row.id}`,
      },
    },
  }"
>
  <template #header>
    <BreadCrumbs :crumbs="crumbs" />
    <PageHeader :title="datasetName" />
  </template>

  <template #footer>
    <Emx2DataView
      :schema-id="schema"
      table-id="Variables"
      :config="variablesConfig"
      :static-filter="variablesFilter"
    />
  </template>
</Emx2RecordView>
```

### Catalogue resource page (replaces 1000+ line bespoke Vue)
```vue
<Emx2RecordView
  :schema-id="schema"
  table-id="Resources"
  :row-id="{ id: resourceId }"
  :display-map="{
    ...defaultDisplayMap,
    'harmonisation-grid': HarmonisationGrid,
  }"
  :config="{
    columnConfig: {
      logo: { displayComponent: IntroDisplay, layout: 'full' },
      dataCategories: { displayComponent: OntologyTreeDisplay },
      collectionEvents: {
        listConfig: {
          layout: 'table',
          visibleColumns: ['name', 'startYear', 'endYear'],
          getHref: (col, row) => `/${catalogue}/cohorts/${resourceId}/collection-events/${row.name}`,
        },
      },
      variables: {
        listConfig: {
          layout: 'table',
          showSearch: true,
          pageSize: 20,
          visibleColumns: ['name', 'label', 'format'],
        },
      },
    },
  }"
>
  <template #header>
    <BreadCrumbs :crumbs="crumbs" />
    <PageHeader :title="resourceName" />
  </template>
</Emx2RecordView>
```
