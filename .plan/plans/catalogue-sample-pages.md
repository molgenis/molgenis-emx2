# Catalogue Sample Pages — Plan

## Goal
Reproduce catalogue collection detail pages (collection, collection-events, subpopulations) as sample pages in tailwind-components using **real data from catalogue-demo**. Identify missing framework features vs page-specific config.

## Source Pages (catalogue app)
1. **Collection detail** (~600 lines) — `[resource]/index.vue`
   - 15+ content sections, side navigation, paginated tables, ontology trees, definition lists
2. **Collection event detail** (~200 lines) — `collection-events/[collectionevent].vue`
   - 7 sections: details, sample categories, age categories, core variables, data categories, areas of info, standardized tools
3. **Subpopulation detail** (~200 lines) — `subpopulations/[subpopulation].vue`
   - 4 sections: details, age categories, main medical condition, comorbidity

## Available Framework (tailwind-components)
Already built:
- `DetailView` — smart/dumb, sections via SECTION/HEADING, sidenav
- `DetailSection` / `DetailColumn` — type-aware rendering
- `DataList` — TABLE/CARDS/LIST/LINKS layouts, smart/dumb, search + pagination
- `OntologyTreeDisplay` — flat + hierarchical
- `DataTable` / `DataCards` / `DataLinks` — dumb renderers
- `SideNav` — scrollspy sidebar
- `DefinitionList` — key-value pairs
- `useRecordNavigation` — injection-based click handling
- Column roles (TITLE, SUBTITLE, DESCRIPTION, DETAIL, LOGO)
- `columnTransform` — rewrite columns (add/change/remove/reorder)
- `expandLevel` — backend supports auto-expand depth 1-3 for GraphQL (default=2)

## Approach
**Incremental**: Start with sample pages immediately using what framework has today. Enhance with F1-F3 as needed during implementation. User can test each step.

## Gap Analysis

### Framework changes (introduced incrementally during page work)

#### F1: Row transform — NEW PROP (add when first page needs computed values)
Add `rowTransform` prop to DetailView — `(row: IRow) => IRow`. Applied after data fetch, before rendering.
**Use cases**: Aggregated "Available Data & Samples" from collection events (P4), period formatting.

#### F2: Expand level — NEW PROP (add when first page needs deep data)
Expose `expandLevel` prop on DetailView (default 2). Backend already supports up to 3.

#### F3: Header actions slot — NEW SLOT (add when contact button needed)
Add `#header-actions` slot to DetailView's auto-generated header. Renders next to title/subtitle area.
```vue
<DetailView ...>
  <template #header-actions>
    <Button @click="showContactModal = true">Contact</Button>
  </template>
</DetailView>
```

### Page config decisions

#### P1: Breadcrumbs — PAGE CONFIG
Each custom page provides its own breadcrumbs above DetailView.

#### P2: Publications display — ROLE METADATA
Configure via column roles on the data model (or `columnTransform`):
- `doi` → role TITLE (hyperlink shown as title)
- `reference` → role DESCRIPTION (formatted citation as description)
No framework changes needed.

#### P3: removeChildIfParentSelected — WONTFIX
Not an issue for these pages. If age groups have redundant parent+child selections, that's a data quality issue to fix in the data, not display logic.

#### P4: Aggregated "Available Data & Samples" — PAGE CONFIG via rowTransform
Merges ontology from all collection events into one tree. Domain logic in `rowTransform` (F1).

#### P5: Sub-page routing — PAGE CONFIG
Custom pages define routes. Use `useRecordNavigation` to override click behavior for collection events / subpopulations navigation.

#### P6: Contact button modal — PAGE CONFIG
Button in `#header-actions` (F3), modal logic in page.

### Nested transforms — POSSIBLE FUTURE WORK
**Question**: Can we pass `columnTransform` / `rowTransform` / `expandLevel` to nested DataList/DetailView elements (e.g., collection events DataList rendered inside parent DetailView)?
**Current state**: These props only apply to the top-level DetailView. Nested DataList components rendered by DetailColumn for REF_ARRAY fields don't receive page-level transforms.
**Decision**: Not needed now — nested lists use their own metadata as-is. If we discover a need during implementation, we could allow `columnTransform` to set nested props on specific columns (e.g., `{ ...col, expandLevel: 3, rowTransform: fn }`). Deferred until proven necessary.

## Implementation Plan

### Phase 1: Collection detail sample page
Create `pages/samples/catalogue/[resource].vue`:
- Smart mode: `schemaId="catalogue-demo"`, `tableId="Resources"`
- Start with basic DetailView, see what renders
- Add `columnTransform` to select/reorder columns, set roles
- **Introduce F2** (`expandLevel=3`) when we hit shallow data
- **Introduce F1** (`rowTransform`) when we need aggregated data or computed values
- **Introduce F3** (`#header-actions`) for contact button
- Breadcrumbs above DetailView
- Target sections: Description, General Design, Population, Organisations, Contributors, Available Data & Samples, Datasets, Subpopulations, Collection Events, Networks, Publications, Access Conditions, Funding, Documentation

### Phase 2: Collection event detail sample page
Create `pages/samples/catalogue/[resource]/collection-events/[event].vue`:
- Smart mode with appropriate columnTransform
- Breadcrumbs back to parent collection
- Sections: details, sample categories, age categories, core variables, data categories, areas of info, standardized tools

### Phase 3: Subpopulation detail sample page
Create `pages/samples/catalogue/[resource]/subpopulations/[subpopulation].vue`:
- Smart mode with columnTransform
- Sections: details (participants, period, countries, criteria), age categories, main medical condition, comorbidity

### Phase 4: Review & gap closure
- Compare sample pages visually with catalogue originals
- Document remaining differences
- Update this plan with final state

## Status: APPROVED — starting Phase 1

## Decisions Made
- Incremental approach: pages first, framework enhancements as needed
- Publications: `doi` as TITLE role, `reference` as DESCRIPTION role
- removeChildIfParentSelected: wontfix (data quality)
- Contact button: `#header-actions` slot (F3)
- Nested transforms: deferred to future work unless needed
- Dataset variable filter: removed, use standard DataList search
- Breadcrumbs: page config
