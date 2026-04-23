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

## Status: Phase 4 in progress — performance done, next: F9 prefetch pagination or visual comparison

### Progress
- [x] Created `pages/samples/catalogue/[resource]/index.vue` with smart mode
- [x] Resource picker with clickable IDs from catalogue-demo
- [x] Menu link in sidebar under "Sample pages"
- [x] DetailSection filters TITLE/SUBTITLE/LOGO from section body (no duplication)
- [x] Removed custom columnTransform — uses backend metadata sections as-is
- [x] Visual review: page renders, sections show, side nav works
- [x] **F2: expandLevel=3** — exposed `expandLevel` prop on DetailView (default 2), passed to fetchRowData
- [x] **F1: rowTransform** — exposed `rowTransform` prop on DetailView, applied in `effectiveData` computed after fetch
- [x] **columnTransform** — `injectMergedColumns` injects two virtual `ONTOLOGY_ARRAY` columns into "availableDataAndSamples" HEADING
- [x] **Framework fix**: removed ONTOLOGY_ARRAY from non-root skip list in fetchTableData.ts
- [x] **Framework fix**: getRowFilter now handles REF columns in composite keys
- [x] **Framework fix**: useHead in DetailView — auto-sets page title and meta description in smart mode
- [x] Column IDs confirmed camelCase (backend `getIdentifier()` → `convertToCamelCase()`)
- [x] Collection event detail page (`[resource]/collection-events/[event].vue`)
- [x] Subpopulation detail page (`[resource]/subpopulations/[subpopulation].vue`)
- [x] `provideRecordNavigation` on resource page for sub-page routing
- [x] Sidebar nav links for all three pages
- [x] Fixed Nuxt routing: moved `[resource].vue` → `[resource]/index.vue` for nested routes

### Phase 4 Progress
- [x] **F4: Type label above title** — DetailView shows `metadata.value.label` as small uppercase text above title in smart mode. Header now shows when any of tableLabel/autoTitle/autoSubtitle is present.
- [x] **F5: Breadcrumbs** — center-aligned page-level breadcrumbs on all three sample pages
- [x] **getTitleText key fallback** — falls back to key=1 column values when no TITLE role columns exist
- [x] **Story cleanup** — removed custom `#header` slot overrides from DetailView.story and DetailPageLayout.story so they use auto-generated headers
- [x] **Sidebar cleanup** — removed collection event/subpopulation links (reachable via navigation from resource page)
- [ ] **F3: Header-actions slot** for contact button
- [x] **Height constraints** — maxItems truncation on List.vue, OntologyTreeDisplay, ValueEMX2, DetailColumn (default 5 for array/ontology types)
- [x] **Card description tooltip** — native title attribute on clamped card descriptions
- [x] **Date nowrap** — whitespace-nowrap on Date/DateTime values, List.vue wrapper changed to inline span
- [x] **Metadata fetch dedup** — inflight promise cache in fetchMetadata.ts prevents duplicate _schema queries
- [x] **F6: REFBACK prefetch** — use expandLevel data to render REFBACK DataLists without extra queries. See [spec](../specs/refback-prefetch.md).
- [x] **F7: SSR metadata caching** — `resolved` Map in fetchMetadata.ts caches across SSR calls. See [spec](../specs/ssr-query-optimization.md).
- [x] **F8: Empty REFBACK prefetch** — accept empty arrays as valid prefetched data, eliminating smart DataList fallback queries.
- [x] **expandLevel=2** — reduced from 3 (ontology parent chains are hardcoded, independent of expandLevel)
- [x] **Skip useTableData in dumb DataList** — prevents client-side re-fetches after hydration
- [x] **Simplify DetailColumn** — merged two DataList blocks into one; DataList decides smart/dumb via rows prop
- [ ] **F9: Prefetch pagination** — optional nestedLimit + always include _agg{count}, hybrid smart/dumb DataList. See [spec](../specs/prefetch-pagination.md).
- [ ] Visual comparison with catalogue originals
- [ ] Document remaining differences

### Aggregation strategy for "Available Data & Samples"
The catalogue aggregates ontology arrays across all collection events into a merged resource-level summary. Implementation:
1. `expandLevel=2` fetches collection events with nested ontology fields (required framework fix: ONTOLOGY_ARRAY no longer skipped at non-root level; ontology parent chains are hardcoded independent of expandLevel)
2. `rowTransform` merges them: flatMap → deduplicate by name → set on row
3. For `areasOfInformation` and `biospecimenCollected`: override existing Resource column values
4. For `_mergedDataCategories` and `_mergedSampleCategories`: virtual row values + columnTransform injects virtual column defs into "availableDataAndSamples" heading

## Decisions Made
- Incremental approach: pages first, framework enhancements as needed
- Use backend metadata sections as-is, only override with columnTransform where needed
- rowTransform overrides existing column values where possible, only adds virtual columns when no Resource-level column exists
- Publications: `doi` as TITLE role, `reference` as DESCRIPTION role
- removeChildIfParentSelected: wontfix (data quality)
- Contact button: `#header-actions` slot (F3)
- Nested transforms: deferred to future work unless needed
- Dataset variable filter: removed, use standard DataList search
- Breadcrumbs: page config

## Open Issues
### Height constraints for list/card values — PLAN

**Problem**: Some column types can produce unbounded/tall content on detail pages.

#### Complete column type audit

**Already truncated (no work needed):**
| Type | Rendering | Why it's fine |
|------|-----------|---------------|
| TEXT | ValueText → ContentReadMore | 250-char substring + "read more" toggle already implemented |
| REFBACK | DataList (smart, paginated) | Always paginated via DataList |
| REF_ARRAY (showListView) | DataList (smart, paginated) | Paginated when rendered as list/table |
| FILE | ValueFile | CSS `overflow-ellipsis whitespace-nowrap` |

**Needs maxItems truncation (array types that render inline):**
| Type | Rendering | Issue |
|------|-----------|-------|
| ONTOLOGY_ARRAY | OntologyTreeDisplay | Tree can have many nodes |
| ONTOLOGY (hierarchical) | OntologyTreeDisplay | Deep/wide trees |
| REF_ARRAY (showInlineListView) | DataList inline | Inline list can be long |
| STRING_ARRAY | List.vue → ValueString | Many items |
| TEXT_ARRAY | List.vue → ValueString | Many items (rendered as plain text, not markdown) |
| EMAIL_ARRAY | List.vue → ValueEmail | Many items |
| HYPERLINK_ARRAY | List.vue → ValueHyperlink | Many items |
| MULTISELECT | List.vue → ValueObject | Many selections |
| CHECKBOX | List.vue → ValueObject | Many selections |

**No truncation needed (bounded by nature):**
All scalar types: STRING, INT, LONG, DECIMAL, BOOL, DATE, DATETIME, PERIOD, UUID, AUTO_ID, EMAIL, HYPERLINK, NON_NEGATIVE_INT, REF, SELECT, RADIO, ONTOLOGY (single flat)

#### Approach

Per-component `maxItems` prop (not CSS-based ShowMore wrapping). ShowMore uses ResizeObserver + line-count — wrong granularity for array items.

#### Components to change

| Component | Handles | Change |
|-----------|---------|--------|
| `OntologyTreeDisplay` | ONTOLOGY, ONTOLOGY_ARRAY trees | Add `maxItems`, slice top-level nodes, toggle |
| `List.vue` (value) | All *_ARRAY, MULTISELECT, CHECKBOX | Add `maxItems`, slice items, toggle |
| `DetailColumn.vue` | Routes to value components | Pass `maxItems` to OntologyTreeDisplay and ValueEMX2 |
| `ValueEMX2.vue` | Delegates to List.vue etc. | Pass `maxItems` through to List.vue |

**Not in scope:**
- DataList (already paginated)
- Ref.vue, RefBack.vue (single item / already paginated)
- TEXT (already has ContentReadMore)
- ShowMore.vue (text/line-based, different use case)

#### Implementation steps

1. **List.vue**: Add `maxItems?: number` prop. Slice displayed items, show "Show N more" / "Show less" toggle button.

2. **OntologyTreeDisplay**: Add `maxItems?: number` prop. For flat lists: slice array. For hierarchical trees: count top-level nodes only.

3. **ValueEMX2.vue**: Accept and pass `maxItems` to List.vue.

4. **DetailColumn.vue**: Accept `maxItems` prop. Pass to OntologyTreeDisplay and ValueEMX2.

5. **Default**: Hardcode default maxItems=10 in DetailColumn for list-type columns. Overridable via columnTransform (`{ ...col, maxItems: 20 }`).

6. **Toggle styling**: Small text button: "Show N more" / "Show less". Reuse existing text-link styles.
