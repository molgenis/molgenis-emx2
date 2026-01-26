# Catalogue Datasets Pages - Plan v1

## Goal
Create dedicated Datasets browse and detail pages in the catalogue app using the new generic record/list view components.

## URLs
```
/[catalogue]/datasets                      # Datasets table (filtered by catalogue)
/[catalogue]/datasets/[resource]/[name]    # Dataset detail with variables table
```

Note: `/[catalogue]/datasets/[resource]` shows datasets filtered by that resource (breadcrumb consistency).

## Data Model
```typescript
// Dataset has composite PK: resource (ref) + name
interface IDatasets {
  resource: IResources;     // FK to Resources table (part of PK)
  name: string;             // PK field
  label?: string;
  datasetType?: IOntologyNode[];
  unitOfObservation?: IOntologyNode;
  keywords?: IOntologyNode[];
  description?: string;
  numberOfRows?: number;
  mappedTo?: IDatasetMappings[];
  mappedFrom?: IDatasetMappings[];
  sinceVersion?: string;
  untilVersion?: string;
}

// Variables have refback to Dataset
interface IVariables {
  dataset: IDatasets;       // FK to Datasets
  name: string;             // PK
  // ... other fields
}
```

## Architecture

### Use New Generic Components
- `TableEMX2` for datasets table (list view)
- `Emx2RecordView` for dataset detail
- `displayConfig` with `component: 'table'` for variables refback

### Catalogue Filtering
```typescript
// Filter datasets by catalogue (via resource's network associations)
function getCatalogueFilter(catalogueId: string) {
  if (catalogueId === 'all') return {};
  return {
    resource: {
      _or: [
        { id: { equals: catalogueId } },
        { partOfNetworks: { id: { equals: catalogueId } } },
        { partOfNetworks: { parentNetworks: { id: { equals: catalogueId } } } },
      ]
    }
  };
}
```

### URL → PK Mapping
```
/[catalogue]/datasets/[resource]/[name]
           ↓
{ resource: { name: resourceParam }, name: nameParam }
           ↓
?resource.name=MyResource&name=MyDataset
```

---

## Phase 1: Datasets List Page

### Story 1.1: Create datasets list page
**File**: `apps/catalogue/app/pages/[catalogue]/datasets/index.vue`

**Features**:
- Table view of datasets using TableEMX2 or custom table
- Columns: name, label, resource, datasetType, numberOfRows
- Filter by catalogue (via resource associations)
- Search functionality
- Pagination
- Click row → navigate to detail page

**Layout**: Use `LayoutsSearchPage.vue` pattern (sidebar + main)

**Filters** (sidebar):
- Search (text)
- datasetType (ontology)
- resource (ref filter)
- unitOfObservation (ontology)

### Story 1.2: Datasets filtered by resource
**File**: `apps/catalogue/app/pages/[catalogue]/datasets/[resource]/index.vue`

**Features**:
- Same as 1.1 but pre-filtered by resource
- Breadcrumb: `[catalogue] > Datasets > [resource]`
- Used for navigation consistency

---

## Phase 2: Dataset Detail Page

### Story 2.1: Create dataset detail page
**File**: `apps/catalogue/app/pages/[catalogue]/datasets/[resource]/[name]/index.vue`

**Features**:
- Use `Emx2RecordView` for dataset fields
- Breadcrumb: `[catalogue] > Datasets > [resource] > [name]`
- SideNav for sections if dataset has many fields

**Display sections**:
1. Basic info: label, description, datasetType, unitOfObservation
2. Statistics: numberOfRows
3. Keywords
4. Versioning: sinceVersion, untilVersion
5. Mappings: mappedTo, mappedFrom (if any)
6. Variables: refback table (Phase 2.2)

### Story 2.2: Variables as table refback
**Goal**: Show variables belonging to this dataset as a table

**Implementation**:
```typescript
const displayConfig = new Map([
  ['variables', {
    component: 'table',
    visibleColumns: ['name', 'label', 'format', 'unit'],
    pageSize: 20,
    getHref: (col, row) => `/${catalogue}/variables/${row.name}`,
  }]
]);
```

**Note**: Variables table should be clickable → navigate to variable detail.

---

## Phase 3: Integration

### Story 3.1: Update landing page
- Add "Datasets" card/link to catalogue landing
- Show dataset count

### Story 3.2: Navigation integration
- Add "Datasets" to catalogue navigation menu
- Update existing dataset links in resource pages to point to new detail pages

### Story 3.3: Shopping cart integration
- Reuse existing `useDatasetStore` for cart functionality
- Add "Add to cart" button on list and detail pages

---

## Files to Create

### Pages
```
apps/catalogue/app/pages/[catalogue]/datasets/
  index.vue                              # Datasets list
  [resource]/
    index.vue                            # Datasets filtered by resource
    [name]/
      index.vue                          # Dataset detail
```

### Components (if needed)
```
apps/catalogue/app/components/
  DatasetCard.vue                        # Card for list view (optional)
```

### GraphQL (extend if needed)
```
apps/catalogue/app/gql/
  datasetsSearch.js                      # Extended query for search/filter
```

---

## Key Decisions

1. **Use TableEMX2 vs custom table**: Start with TableEMX2, customize if needed
2. **Composite PK URL encoding**: Use dot notation (`resource.name=X&name=Y`)
3. **Variables display**: Use displayConfig with `component: 'table'`
4. **Filter sidebar**: Reuse existing filter components from catalogue
5. **Layout**: Match existing catalogue pages (LayoutsSearchPage)

---

## Dependencies

- Phase 5 complete: query param URLs, getHref, extractPrimaryKey ✅
- TableEMX2 component (exists)
- Emx2RecordView component (exists)
- Catalogue filter utilities (exist)

---

## Open Questions

1. Should dataset detail show embedded resource info or link to resource?
2. Variables table: include all variable fields or subset?
3. Mappings display: table or list view?
4. Need DatasetCard component or use generic table row?

---

## Progress

| Story | Status | Notes |
|-------|--------|-------|
| 1.1 Datasets list page | ✅ DONE | `/[catalogue]/datasets/index.vue` |
| 1.2 Datasets by resource | ✅ DONE | `/[catalogue]/datasets/[resource]/index.vue` |
| 2.1 Dataset detail page | ✅ DONE | `/[catalogue]/datasets/[resource]/[name]/index.vue` using Emx2RecordView |
| 2.2 Variables table | ✅ DONE | displayConfig with `component: 'table'` |
| 3.1 Landing page | ✅ DONE | Added Datasets card with count |
| 3.2 Navigation | ✅ DONE | Added to header via useHeaderData |
| 3.3 Shopping cart | 📦 FUTURE | Deferred - reuse existing useDatasetStore when needed |
