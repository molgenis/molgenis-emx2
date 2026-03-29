# Catalogue Detail Views — Plan

## Goal
Replace catalogue's 5 bespoke detail pages (~1,950 lines) with config-driven Emx2RecordView, using column/table settings to control rendering. Focus: detail views only (list views, landing pages, harmonisation matrix out of scope).

## Current State
Phases 1-4 delivered generic detail view components:
- Emx2RecordView, RecordView, RecordSection, RecordColumn
- Emx2ListView for REFBACK and REF_ARRAY (paginated table, search, clickable rows)
- SideNav, DetailPageLayout, ontology tooltips, empty section hiding

## Decisions Made
1. **`displayAs` → column setting** (not first-class property). It's a display hint, settings are sufficient.
2. **Nested tables → TableType enum** (`DATA_NESTED`). Cleaner than settings.
3. **Ontology tree → auto-apply** for ONTOLOGY_ARRAY. Lift from catalogue into tailwind-components (separate PR if large).
4. **Card grid → generic component** configured via `cardFields` setting (which fields to show).

## What's Missing

### 1. Display modes for REF_ARRAY/REFBACK
Currently always renders as **table**. Catalogue uses 3 modes:

| Display Mode | Example | Current Support |
|-------------|---------|----------------|
| **Table** | Datasets, subpopulations, collection events | YES |
| **Card grid** | Organisations, networks, contributors | NO |
| **Bullet list** | Publications, core variables, age categories | NO |

### 2. Visible fields in nested views
Cards need configurable field subset (name, description, image, link). Bullet list uses refLabel.

### 3. Ontology tree display
ONTOLOGY_ARRAY renders as flat comma-separated text. Catalogue has collapsible tree display (ContentOntology 80 lines + TreeNode 83 lines). Used in 4/5 detail pages. No equivalent in tailwind-components yet.

### 4. Top-level vs nested tables
No metadata distinction. Collection Events, Subpopulations, Datasets are "nested" — shown inline, shouldn't appear as standalone pages in entity list.

## Metadata Changes

### A. Column setting: `displayAs`
Controls how REF_ARRAY/REFBACK renders in detail views.

**Where:** Column settings (`molgenis_settings` table)
**Key:** `displayAs` (on the column's row, keyed as `columnName.displayAs` or similar)
**Values:** `table` (default), `cards`, `list`

**Frontend:** Emx2RecordView reads column settings, sets `listConfig.layout`.

### B. Column setting: `cardFields`
Which fields to show when rendering as cards or list.

**Key:** `cardFields`
**Value:** Comma-separated column IDs from the ref table (e.g., `name,acronym,country,website`)

Falls back to refLabelDefault fields if not set.

**Frontend:** Passed as `visibleColumns` in IListConfig.

### C. TableType enum: add `DATA_NESTED`

**Impact analysis (15 Java files reference tableType):**

| Category | Files | Impact |
|----------|-------|--------|
| Switch statements | 2 (RdfRowsGenerator, OntologyIriMapper) | Need explicit case — treat like DATA |
| If-else `== ONTOLOGIES` | 8 (SqlQuery, GraphqlTableFieldFactory, MolgenisIO, Emx2, etc.) | No change — DATA_NESTED is not ONTOLOGIES |
| If-else `== DATA` | 3 (ImportProfileTask, Emx2RdfGenerator, RootRdfGenerator) | Needs review — should DATA_NESTED be imported/exported like DATA? |
| DB persistence | 1 (MetadataUtils) | Auto-works via valueOf() |
| CSV import | 1 (Emx2) | Auto-works via valueOf() |
| Frontend | 1 (apps/ui [schema]/index.vue) | Update TypeScript type, filter from main table list |
| Tests | ~8 files | May need new test cases |

**Behavior for DATA_NESTED (treat like DATA except):**
- Same DB schema generation as DATA
- Same permissions as DATA
- Same RDF generation as DATA
- Same import/export as DATA
- **UI difference:** filtered from main entity list, row click may expand inline

**Estimated effort:** ~2 hours backend changes. Most files need no change (they check `== ONTOLOGIES`). Only need explicit handling in:
- 2 switch statements (add case → same as DATA)
- 3 `== DATA` checks (include DATA_NESTED)
- Frontend type update + filter logic

## Implementation Phases

### Phase 5: Ontology Tree Display (separate PR candidate)
Lift catalogue's display-only tree into tailwind-components.

**Source components:**
- `apps/catalogue/app/components/content/Ontology.vue` (80 lines) — smart: detects list vs tree
- `apps/catalogue/app/components/content/TreeNode.vue` (83 lines) — recursive collapsible nodes

**Target:**
- [x] `tailwind-components/app/components/display/OntologyTreeDisplay.vue`
- [x] Recursive collapsible tree with definition tooltips (`OntologyTreeNode.vue`)
- [x] Auto-detects flat list vs hierarchical tree
- [x] Default rendering for ONTOLOGY_ARRAY columns (replace flat text)
- [x] RecordColumn: detect ONTOLOGY_ARRAY → render OntologyTreeDisplay instead of ValueEMX2
- [x] Story file (`OntologyTreeDisplay.story.vue`)
- [x] `buildOntologyTree.ts` utility with 14 vitest tests
- [x] `fetchTableData.ts` updated to include 4-level parent chain for ONTOLOGY/ONTOLOGY_ARRAY
- [ ] Test with catalogue data (data categories, medical conditions)

### Phase 6: Display Mode Settings + Components
Enable metadata-driven choice between table, cards, and bullet list.

**6a. Column settings infrastructure**
- [ ] Verify column settings are exposed in GraphQL metadata response
- [ ] Verify IColumn TypeScript type includes `settings` field
- [ ] If not, add to metadata GraphQL query and IColumn type

**6b. Frontend: read `displayAs` + `cardFields` settings**
- [ ] Emx2RecordView: in column processing, read `displayAs` from column settings
- [ ] Map to listConfig.layout: `table` / `cards` / `list`
- [ ] Read `cardFields` from column settings → `listConfig.visibleColumns`
- [ ] Fallback: `table` if no setting, refLabelDefault fields if no cardFields

**6c. Card grid in ListView**
- [ ] ListView `cards` layout: render generic card per row (currently requires custom component)
- [ ] Generic card: shows `visibleColumns` fields as definition list, name as heading, link to detail
- [ ] Responsive grid: 1 col mobile, 2 col tablet, 3 col desktop
- [ ] Story file

**6d. Bullet list in ListView**
- [ ] ListView `list` layout already works (bullet list with NuxtLink)
- [ ] Ensure `getRowLabel` uses `refLabel`/`refLabelDefault` template
- [ ] Verify Emx2ListView passes row href for link generation

### Phase 7: Nested Table Type (backend)

**7a. Backend enum**
- [ ] Add `DATA_NESTED` to `TableType.java`
- [ ] Update 2 switch statements in RDF (RdfRowsGenerator, OntologyIriMapper) — treat as DATA
- [ ] Update 3 `== DATA` checks to include DATA_NESTED (ImportProfileTask, Emx2RdfGenerator, RootRdfGenerator)
- [ ] Verify CSV import/export handles new value (should auto-work)
- [ ] Add basic test for DATA_NESTED table creation

**7b. Frontend**
- [ ] Update TypeScript type: `"DATA" | "ONTOLOGIES" | "DATA_NESTED"`
- [ ] apps/ui `[schema]/index.vue`: filter DATA_NESTED from main table list (or show with indicator)
- [ ] RecordTable row click: DATA_NESTED tables still navigate to detail page (no change needed)

### Phase 8: Catalogue Detail Pages Demo
Wire everything together on real catalogue data model.

- [ ] Set up catalogue data model with settings:
  - `organisations.displayAs = cards`, `organisations.cardFields = name,acronym,country,website`
  - `contributors.displayAs = cards`, `contributors.cardFields = name,role,email`
  - `networks.displayAs = cards`, `networks.cardFields = name,description`
  - `publications.displayAs = list`
  - `datasets.displayAs = table` (default)
  - Collection Events, Subpopulations, Datasets → `tableType: DATA_NESTED`
- [ ] Collection Event detail → Emx2RecordView + ontology tree
- [ ] Subpopulation detail → Emx2RecordView + ontology tree
- [ ] Resource detail → Emx2RecordView (organisations as cards, publications as list, etc.)
- [ ] Variable detail → Emx2RecordView (harmonisation OUT OF SCOPE)
- [ ] Visual comparison with handcrafted pages
- [ ] Theme testing (Light, Dark, Molgenis, UMCG, AUMC)

## NOT IN SCOPE
- Filter sidebar / list views / search pages (separate PR)
- Landing pages with CTA cards and stats
- Variable harmonisation matrix
- Shopping cart integration
- Contact modal / side modal previews (direct navigation replaces)
- IntroDisplay hero section (cosmetic, can add later)
- File download display (existing component, wire up later)
