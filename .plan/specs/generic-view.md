# Generic View - Specification

## Filter System

### Architecture

**One-way data flow** with URL as source of truth:

```
URL ──────► filterStates (computed) ──────► UI
 ▲                                          │
 │                                          │
 └─────────── router.replace() ◄────────────┘
                (on user action)
```

When `urlSync` is enabled:
- `filterStates` getter reads from URL
- `filterStates` setter calls `router.replace()` which updates URL
- URL change triggers computed to recalculate
- No bidirectional sync, no race conditions

When `urlSync` is disabled:
- `filterStates` is a simple ref
- Direct read/write, no URL involvement

### URL Format

```
?name=John                    # STRING: like filter (single term)
?name=aap+noot                # STRING: like OR ("aap noot", space=+)
?name=aap+and+noot            # STRING: like AND ("aap and noot", keyword preserved)
?age=18..65                   # INT/DECIMAL: between filter
?age=18..                     # >= 18
?age=..65                     # <= 65
?birth=2024-01-01..2024-12-31 # DATE: range
?category.name=Cat1|Cat2      # REF/ONTOLOGY: in filter (pipe = multi-select)
?name=null                    # is null
?name=!null                   # is not null
?mg_search=term               # Global search
```

#### String Filter Encoding

String (like) filters store the raw input string in the URL. Vue Router
encodes spaces as `+` (standard URL encoding). On read, `+` is decoded back
to space. Smart parsing (OR via spaces, AND via "and" keyword) only happens
in `buildGraphQLFilter()` at query build time, never during serialization.

This design ensures:
- Lossless round-trip: URL → filterState → URL produces same result
- No character eating during typing (no parsing in serialization path)
- AND intent preserved across page refresh (`aap+and+noot` → "aap and noot")
- Quoted phrases: single quotes preserve multi-word terms as one unit
  - `'aap noot' mies` → OR between "aap noot" and "mies"
  - `'aap noot' and mies` → AND between "aap noot" and "mies"

The `|` pipe separator is reserved exclusively for multi-value `in` filters
(REF/ONTOLOGY types where user selects discrete options from a list).

#### REF/ONTOLOGY Filter Encoding

REF types use dotted syntax (`column.field=value`) to:
- Be explicit about which field is filtered
- Enable nested queries (`parent.child.name=value`)
- Match GraphQL filter structure

Backward compatible: `?category=Cat1` defaults to `name` field.

### Components

| Component | Purpose |
|-----------|---------|
| `useFilters` | Composable: filter state, URL sync, GraphQL filter |
| `FilterColumn` | Single filter input (auto-selects input type) |
| `FilterSidebar` | Container for multiple FilterColumns |
| `FilterRange` | Min/max input for numeric/date types |
| `Emx2DataView` | Unified data view with integrated filters |

### useFilters API

```typescript
const {
  filterStates,  // Map<columnId, IFilterValue> - writable computed
  searchValue,   // string - search term
  gqlFilter,     // Record - debounced GraphQL filter object
  setFilter,     // (columnId, value) => void
  setSearch,     // (value) => void
  clearFilters,  // () => void
  removeFilter,  // (columnId) => void
} = useFilters(columns, {
  debounceMs: 300,    // gqlFilter debounce (default: 300)
  urlSync: true,      // sync to URL (default: false)
  route,              // vue-router route (optional, falls back to Nuxt)
  router,             // vue-router router (optional, falls back to Nuxt)
});
```

### IFilterValue

```typescript
interface IFilterValue {
  operator: "like" | "like_or" | "like_and" | "equals" | "in" | "between" | "isNull" | "notNull";
  value: any;
}
```

Operator semantics:
- `like` — raw string, parsed at query time by `parseFilterTerms()`
- `like_or` — pre-parsed array of terms, OR logic
- `like_and` — pre-parsed array of terms, AND logic (uses `_and` wrapper)
- `equals` — exact match (scalar or object)
- `in` — multi-select (array of values/objects)
- `between` — range [min, max]
- `isNull` / `notNull` — null checks

### Column Type Mapping

| Column Type | Input | Operator | URL Example |
|-------------|-------|----------|-------------|
| STRING, TEXT, EMAIL | text | like | `?name=John` or `?name=aap+noot` |
| INT, DECIMAL, LONG, DATE | range | between | `?age=18..65` |
| BOOL | toggle | equals | `?active=true` |
| REF, REF_ARRAY | dropdown | in | `?category.name=Cat1\|Cat2` |
| ONTOLOGY, ONTOLOGY_ARRAY | tree | in | `?country.name=NL\|BE` |

## Component Architecture

### Page Layout Pattern

```
┌─────────────────────────────────────────────────────┐
│ Header (slot) - PageHeader with breadcrumbs         │
├─────────────────────────────────────────────────────┤
│ ┌──────────┐  ┌────────────────────────────────────┐│
│ │ Sidebar  │  │ Main                               ││
│ │ (slot)   │  │ (slot)                             ││
│ │ optional │  │                                    ││
│ └──────────┘  └────────────────────────────────────┘│
└─────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Purpose |
|-----------|---------|
| `DetailPageLayout` | Page structure: header + optional sidebar + main |
| `PageHeader` | Title, description, prefix slot for breadcrumbs |
| `BreadCrumbs` | Navigation breadcrumbs |
| `Emx2DataView` | Data listing with integrated filter sidebar (headerless) |
| `Emx2RecordView` | Single record display |
| `FilterSidebar` | Filter inputs container |
| `SideNav` | Section navigation for detail pages |

### Emx2DataView (headerless)

```
┌──────────┐  ┌────────────────────────────────────┐
│ Filter   │  │ Content card                       │
│ Sidebar  │  │ ┌────────────────────────────────┐ │
│ (when    │  │ │ ActiveFilters                  │ │
│ config.  │  │ ├────────────────────────────────┤ │
│ show     │  │ │ Table/List/Cards               │ │
│ Filters) │  │ ├────────────────────────────────┤ │
│          │  │ │ Pagination                     │ │
└──────────┘  └────────────────────────────────────┘
```

- No title/description (parent provides via PageHeader)
- Keeps FilterSidebar internally (filter state tightly coupled)
- `config.showFilters` controls sidebar visibility

### Usage Patterns

**Data page:**
```vue
<DetailPageLayout>
  <template #header>
    <PageHeader title="Pets" description="All pets">
      <template #prefix>
        <BreadCrumbs :crumbs="[...]" />
      </template>
    </PageHeader>
  </template>
  <template #main>
    <Emx2DataView schema-id="..." table-id="..." :config="{showFilters: true}" />
  </template>
</DetailPageLayout>
```

**Detail page:**
```vue
<DetailPageLayout>
  <template #header>
    <PageHeader title="Spike">
      <template #prefix>
        <BreadCrumbs :crumbs="[...]" />
      </template>
    </PageHeader>
  </template>
  <template #sidebar>
    <SideNav :sections="[...]" />
  </template>
  <template #main>
    <Emx2RecordView ... />
  </template>
</DetailPageLayout>
```

## Acceptance Criteria: Emx2DataView

### View Modes
| Mode | Header Slot | Filters | Sidebar | Mobile Button |
|------|-------------|---------|---------|---------------|
| Full Page | provided | true | visible (xl+) | visible (<xl) |
| Compact | empty | true | visible (xl+) | visible (<xl) |
| Vanilla | empty | false | hidden | hidden |

### AC-1: DetailPageLayout Integration
- [x] Uses DetailPageLayout internally
- [x] #header slot passed through (optional)
- [x] #sidebar slot contains FilterSidebar when showFilters=true
- [x] Compact mode when no header provided

### AC-2: Responsive Filters
- [x] Desktop (xl+): FilterSidebar visible in sidebar
- [x] Mobile (<xl): "Filters" button shows SideModal with FilterSidebar
- [x] Mobile button hidden when showFilters=false

### AC-3: Vanilla Mode
- [x] showFilters=false: no sidebar rendered
- [x] showFilters=false: no mobile filter button
- [x] Pure data display, usable anywhere

### AC-4: Layout Modes
- [x] layout="table": renders HTML table
- [x] layout="list": renders ul/li list
- [x] layout="cards": renders CardList/CardListItem

### AC-5: Slots
- [x] #header - optional page header content
- [x] #default - custom list item (props: row, label)
- [x] #card - custom card content (props: row, label)

### AC-6: Toolbar Controls
- [x] Desktop (xl+): Add button (if editable), Show/Hide Filters toggle, Columns button
- [x] Mobile (<xl): Add button (if editable), Filters button, Columns button
- [x] isEditable prop controls Add button visibility
- [x] Show/Hide Filters toggles sidebar visibility at runtime

### AC-7: Responsive Table/Cards
- [x] Desktop (md+): Table layout with horizontal scroll
- [x] Mobile (<md): RecordCard layout with label:value pairs
- [x] RecordCard uses ValueEMX2 for proper type formatting
- [x] Sticky first column on table (desktop)

### AC-8: Filter Customization
- [x] FilterSidebar has Customize button (settings icon)
- [x] Opens Columns modal in filters mode
- [x] Can show/hide individual filters
- [x] Visible in both mobile and desktop views

### AC-9: Row Actions (when isEditable=true)
- [x] Edit button per row (icon="edit") in first column (sticky)
- [x] Delete button per row (icon="trash") in first column
- [x] Edit button opens EditModal with row data
- [x] Delete button opens DeleteModal with confirmation
- [x] Modal submit refreshes data
- [x] Mobile: actions in RecordCard via #actions slot

## Responsive Breakpoints

| Breakpoint | Width | Filters | Table/Cards |
|------------|-------|---------|-------------|
| < md | < 768px | mobile toolbar | cards |
| md - xl | 768px - 1280px | mobile toolbar | table |
| xl+ | 1280px+ | sidebar | table |

## Design Decisions

1. **One-way data flow**: URL is single source of truth, eliminates sync bugs
2. **Writable computed**: Convenient API while maintaining one-way flow internally
3. **Debounced gqlFilter**: Prevents excessive API calls (300ms default)
4. **Immediate URL updates**: User sees URL change instantly
5. **Explicit REF paths**: `category.name=value` not `category=value`
6. **Pipe separator**: `|` for REF/ONTOLOGY multi-select only (avoids comma conflicts)
12. **Raw string URL for text filters**: Spaces encoded as `+` by Vue Router, decoded on read. Parsing (OR/AND) only in `buildGraphQLFilter()`. Avoids character-eating during typing and ensures lossless round-trip. Pipe `|` NOT used for text filters.
7. **Reserved prefix**: `mg_*` params preserved across filter changes
8. **Graceful degradation**: Works without router (uses local refs)
9. **Composition over props**: DetailPageLayout uses slots, components compose
10. **Headerless data view**: Emx2DataView has no title, parent provides context
11. **Coupled filter sidebar**: FilterSidebar stays in Emx2DataView (state coupling)
