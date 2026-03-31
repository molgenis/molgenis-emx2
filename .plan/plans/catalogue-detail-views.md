# Catalogue Detail Views — Plan

## Goal
Replace catalogue's 5 bespoke detail pages (~1,950 lines) with config-driven Emx2RecordView, using column/table settings to control rendering. Focus: detail views only (list views, landing pages, harmonisation matrix out of scope).

## Current State
Phases 1-5 delivered generic detail view components:
- Emx2RecordView, RecordView, RecordSection, RecordColumn
- Emx2ListView for REFBACK and REF_ARRAY (paginated table, search, clickable rows)
- SideNav, DetailPageLayout, ontology tooltips, empty section hiding
- OntologyTreeDisplay for ONTOLOGY/ONTOLOGY_ARRAY (collapsible tree with definition tooltips)
- apps/ui already uses Emx2RecordView for its detail page

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
~~ONTOLOGY_ARRAY renders as flat comma-separated text.~~ (RESOLVED — Phase 5) OntologyTreeDisplay auto-detects flat vs tree, renders collapsible tree with definition tooltips.

### 5. HEADING columns missing from data models
Catalogue data model CSVs (Collection Events, Resources, Subpopulations, Datasets, Variables) have no HEADING type columns. Without these, Emx2RecordView renders all fields as a flat list without sidebar navigation or section grouping. The catalogue's handcrafted pages organize content into sections — this structure needs to be encoded as HEADING columns in the data model.

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

### Phase 6: Column Display Properties + Card Component

#### 6a. Backend: add `summary` (boolean) and `display` (string) to Column
New first-class Column properties (not settings). Controls how columns/refs render in detail views.

- `role: ColumnRole` — column's display role: TITLE, SUBTITLE, DESCRIPTION, LOGO, DETAIL
- `display: DisplayType` — on REF_ARRAY/REFBACK columns, controls layout: TABLE (default), CARDS, LIST

**Backend changes (migration 32 → 33):**
- [x] `Column.java`: add `summary` (Boolean, default false) + `display` (String) fields, getters, setters, copy constructor
- [x] `MetadataUtils.java`: add field definitions, init(), save (insert+conflict), load (recordToColumn)
- [x] `migration32.sql`: ALTER TABLE column_metadata ADD COLUMN summary/display
- [x] `Migrations.java`: version 32→33, execute migration32.sql
- [x] `GraphqlSchemaFieldFactory.java`: expose `summary` and `display` in GraphQL schema
- [x] CSV import/export: Column properties included in Emx2.java (constants, import, headers, export)
- [x] `json/Column.java`: fields, constructor mapping, getColumnMetadata, getters/setters
- [ ] JUnit test: create column with summary=true and display="card", verify persistence

#### 6b. Frontend: read `summary` + `display` from metadata
- [x] `IColumn` type (metadata-utils/src/types.ts): add `summary?: boolean`, `display?: string`
- [x] GraphQL metadata query (gql/metadata.js): add `summary`, `display` to columns query
- [x] `Emx2RecordView`: read `display` from column → set `listConfig.layout`
- [x] `Emx2RecordView`: filter ref table columns to `summary: true` → set `listConfig.visibleColumns`
- [x] Default when no summary columns: show primary key + first columns, max 5 total
- [x] Switched from `fetchTableMetadata` to `fetchMetadata` for full schema access

#### 6c. Generic card component for ListView
- [x] `display/ListCard.vue`: generic card showing summary fields as definition list, name as heading, link to detail
- [x] `ListView.vue`: when `layout === 'card'`, render ListCard in responsive grid (no custom component needed)
- [x] Responsive grid: 1 col mobile, 2 col desktop (`grid grid-cols-1 lg:grid-cols-2`)
- [x] Story file for ListCard + card grid layout
- [x] `IListConfig.layout` extended to accept `"card"` alongside `"cards"`

#### 6d. Test with catalogue data model
- [x] `data/_models/shared/Resources.csv`: set `display: card` on `people involved` (refback to Contacts)
- [x] `data/_models/shared/Contacts.csv`: set `summary: true` on `first name`, `last name`, `role`
- [ ] Verify card grid renders on resource detail page at `/catalogue/Resources/{id}`

### Phase 7: Nested Table Type (backend) — DEFERRED (separate PR)

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

### Phase 8: Catalogue Detail Pages Demo — DEFERRED (separate PR)
Wire everything together on real catalogue data model.

**Prerequisite:** HEADING columns must be added to catalogue data model CSVs (see What's Missing #5).

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
