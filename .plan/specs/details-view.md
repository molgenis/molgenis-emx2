# Detail View Like Catalogue — Spec

## Core Types

### IColumnDisplay (extends IColumn)

Client-side extension of backend IColumn with display hints. RecordView (dumb) works exclusively with this type. Defined in `tailwind-components/types/types.ts`.

```typescript
export interface IColumnDisplay extends IColumn {
  displayComponent?: Component;
  listConfig?: IListConfig;
  displayLabel?: string;
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
  hideColumns?: string[];
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
3. Generates SideNav sections from SECTION columns
4. Renders DetailPageLayout with SideNav + RecordSection per group

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
   c. If REFBACK and no listConfig, set default listConfig (table, pageSize 10, search)
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
- Scalar/REF columns → DefinitionList (dt/dd grid)
- REFBACK/REF_ARRAY columns → full-width below definition list

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
1. If empty and !showEmpty → render nothing
2. If `column.displayComponent` → render that component with `{column, value, showEmpty}`
3. Fallback → ValueEMX2; REF/SELECT/RADIO values are automatically wrapped in NuxtLink using `buildRefHref` (no config needed)

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
      logo: { displayComponent: IntroDisplay },
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
