# Plan: Split Resources into Resources, Collections, Networks, Catalogues (v2.0.0)

## Context
The `Resources` table in `data/_models/shared/Resources.csv` holds ALL resource types in one flat table. Problems:
1. Ugly computed `rdf type` field to distinguish dcat:Catalog vs dcat:Dataset
2. Catalogue-only and collection-only fields mixed, using `visible` expressions
3. One `type` ontology with `tags` to classify

By splitting into an inheritance hierarchy (all in one file), each table gets proper RDF semantics and cleaner column ownership.

## Inheritance Hierarchy

```
Resources (base - shared columns, NO rdf type)
├── Collections (extends Resources) → dcat:Dataset
└── Networks (extends Resources) → foaf:Organisation
    └── Catalogues (extends Networks) → dcat:Catalog
```

All tables stay in ONE file `Resources.csv` using `tableName` column to discriminate (like `Processes.csv` pattern). Row order preserved — only `tableName` column changes.

---

## Phase 1: Data Model — Edit Resources.csv [DONE]

Row order preserved from master. Only `tableName` column changed for moved rows. Table definition rows inserted before first column of each new table.

### Resources (base table) — shared columns kept:
- id, pid, name, local name, acronym
- overview heading, hricore
- website, description, keywords, contact email, logo
- start year, end year, time span description
- countries, continents, regions
- issued, modified
- internal/external identifiers (refbacks)
- contributors heading, organisations involved, publisher, creator, people involved, contact point
- part of networks (refback, refTable→Networks)
- datasets (refback), samplesets (refback), counts (refback)
- standards heading + mappings, common data models, ETL vocabularies
- information heading, publications, documentation
- funding sources, funding scheme, funding statement
- citation requirements, acknowledgements, provenance statement, supplementary information
- theme, applicable legislation
- status
- Simplified `required` expressions: removed type checks from keywords, creator, theme

### Collections (extends Resources) — tableName=Collections:
- type (ontology_array, Resource types)
- cohort type, clinical study type, registry or health record type
- design and structure section, subpopulations/collection events refbacks
- population section, available data and samples
- linkage, access conditions, updates, quality sections
- study details, study methods sections
- Simplified `access rights` required expression

### Networks (extends Resources) — tableName=Networks:
- network type (visible expression removed)
- data resources (ref_array → Resources, both profile variants)
- child networks (ref_array → Networks), parent networks (refback)
- networks other

### Catalogues (extends Networks) — tableName=Catalogues:
- catalogue type (visible expression removed), main catalogue (visible removed)
- fdp endpoint, ldp membership relation, conforms to, has member relation

### Deleted:
- `rdf type` computed field removed entirely

### File changed:
- `data/_models/shared/Resources.csv`

---

## Phase 2: Update referencing CSV model files and demo data [DONE]

### Model changes:
- `data/_models/shared/Endpoint.csv`: `metadataCatalog` refTable → `Catalogues`

### Kept as Resources (inheritance handles it):
- `Subpopulations.csv`, `Collection events.csv` — kept refTable=Resources
  (Collections IS-A Resources, so refs work via inheritance; changing to Collections caused validation errors)
- `Datasets.csv`, `Samplesets.csv`, `Resource counts.csv`
- `Contacts.csv`, `Publications.csv`, `Documentation.csv`
- All mapping/identifier/variable CSVs
- `Processes.csv`, `Individuals.csv`, `Materials.csv`, `Agents.csv`

### Demo data split:
- `data/_demodata/applications/datacatalogue/Resources.csv` → removed (replaced by split files below)
- `data/_demodata/applications/datacatalogue/Collections.csv` — 96 collections
- `data/_demodata/applications/datacatalogue/Networks.csv` — 2 networks
- `data/_demodata/applications/datacatalogue/Catalogues.csv` — 11 catalogues
- `data/_demodata/applications/patient_registry/Collections.csv` — 6 collections
- `data/_demodata/applications/patient_registry/Catalogues.csv` — 1 catalogue
- `data/_demodata/shared-examples/Collections.csv` — 1 collection
- `data/_demodata/applications/datacatalogue_cohortstaging/Collections.csv` — header only
- `data/_demodata/applications/datacatalogue_networkstaging/Networks.csv` — header only
- `data/_ontologies/Networks.csv` — 24 networks
- Removed `rdf type` column from all data files

### Backend test:
- `ResourcesSplitTest.java` — removed (redundant with CatalogueTest)
- `CatalogueTest.java` — assertion updated: cohortStaging tables 20→21

---

## Phase 3: Update DataCatalogueFlat.csv [DONE — NO CHANGES NEEDED]

`data/_models/specific/dev/DataCatalogueFlat.csv` is a completely separate standalone model (no inheritance, no profile tags). It already has Collections/Catalogues tables with its own `collectionType`-based visibility. It is independent from the shared model we changed — no updates required.

---

## Phase 4: Update catalogue app frontend [DONE]

### Files changed (16):

**Interfaces (regenerated from schema):**
- `apps/catalogue/interfaces/catalogue.ts` — REGENERATED from schema; now has separate `IResources`, `ICollections`, `INetworks`, `ICatalogues` interfaces

**GraphQL queries and composables:**
- `apps/catalogue/app/composables/useHeaderData.ts` — `mg_tableclass` filter uses `_or` for both Networks and Catalogues
- `apps/catalogue/app/gql/resource.js` — queries Collections instead of Resources

**Pages:**
- `apps/catalogue/app/pages/index.vue` — queries `ICatalogues` table directly, uses `ICatalogues_agg` type
- `apps/catalogue/app/pages/[catalogue]/index.vue` — queries Catalogues/Collections/Networks with proper filter types; separate `subpopulationsCollectionFilter` (ResourcesFilter) variable; `mg_tableclass` uses `_or` for unscoped variables
- `apps/catalogue/app/pages/[catalogue]/[resourceType]/index.vue` — dynamic table name (Collections/Networks) based on route param
- `apps/catalogue/app/pages/[catalogue]/[resourceType]/[resource]/index.vue` — dynamic table name with `isCollection` guards; `IResourceQueryResponseValue` extends `Omit<ICollections, "type"> & Pick<INetworks, "networkType">`; `catalogueType` on `partOfNetworks`
- `apps/catalogue/app/pages/[catalogue]/variables/index.vue` — uses Resources/ResourcesFilter for the query (unchanged from master); `mg_tableclass` uses `_or` filter for unscoped

**Components:**
- `apps/catalogue/app/components/landing/Central.vue` — `Resources_agg` → `Collections_agg`, `Resources_groupBy` → `Collections_groupBy`
- `apps/catalogue/app/components/landing/CohortsOnly.vue` — same as Central.vue
- `apps/catalogue/app/components/content/ContentBlockCatalogues.vue` — `IResources[]` → `ICatalogues[]` prop type
- `apps/catalogue/app/components/content/cohort/GeneralDesign.vue` — `ICollections` → `ICollections & Pick<INetworks, "networkType">` prop type
- `apps/catalogue/app/components/ResourceCard.vue` — `IResources` → `ICollections` prop type

**Documentation:**
- `apps/catalogue/README.md` — fixed generateTypes command

### NOT changed (confirmed identical to master):
- `apps/catalogue/app/gql/variable.ts` — was temporarily changed but reverted, no diff vs master

---

## Phase 5: Verification and Testing [DONE]

- All CI tests passing (green)
- renderForm e2e test fixed: `table=Resources` → `table=Collections`
- Landing page fixed: added ScopedCollection query for Collections (e.g., "FORCE-NEN collections")
- Merge conflict in useHeaderData.ts resolved
- Frontend tests: catalogue (29/29), tailwind-components (125/125) all passed

---

## Phase 6: Simplify frontend types [DONE]

### Context
GraphQL flattens inheritance — querying `Resources` returns ALL fields from all subtables. The TypeScript generator used `table.getColumns()` (own+parent only), so `IResources` only had 49 fields. Fixed the generator to match GraphQL behavior, then reverted most frontend type changes back to master.

### Changes made:

**Backend — TypeScript generator:**
- `Generator.java`: `getColumns()` → `getColumnsIncludingSubclasses()` + dedup via `LinkedHashSet`
- Added proper imports (`Set`, `LinkedHashSet`) to match file style
- `IResources` now includes all 154+ fields from all subtables (matching GraphQL)

**Frontend — simplified to match master:**
- `[resourceType]/index.vue` — queries `Resources` table (like master) with `mg_tableclass` filter replacing old `type.tags` filter
- `[resource]/index.vue` — queries `Resources` with all fields; added explicit `Resources` key to `IResponse` interface; removed dead `tableName` variable
- `[catalogue]/index.vue` — removed then restored `ScopedCollection` query (needed for Collections used as catalogue scopes, e.g., "FORCE-NEN collections")
- `variables/index.vue` — replaced `type.name = "Network"` with `mg_tableclass` filter for unscoped resource options
- `useHeaderData.ts` — removed `type.name = "Network"` filter; fixed `partOfNetworks` → `parentNetworks` bug in scoped variablesFilter

**E2e test updated:**
- `keep-filter-on-pagination-change.spec.ts` — "21 collections" → "18 collections" (stricter `mg_tableclass` filter vs old `type.tags`)

### Key architectural insights:
- Table inheritance auto-filters via `mg_tableclass` at SQL level: querying `Networks` returns Networks + Catalogues, querying `Collections` returns only Collections
- `mg_tableclass` values are schema-qualified (e.g., `datacatalogue.Networks`) — must use `${schema}.Networks`
- `mg_tableclass` filters only needed when querying `Resources` base table or filtering ref columns pointing to Resources
- `ScopedCollection` fallback needed because some catalogue route params refer to Collections (e.g., "FORCE-NEN collections") — consider reclassifying as Catalogues in demo data

### Verification:
- All vitest tests pass (29/29)
- Typecheck clean (`pnpm --filter catalogue lint`)
- All CI checks green (test, preview, e2e-test, testPyPI release, pr-preview)

---

## Resolved Questions
- **mg_tableclass filtering**: works correctly — verified by comparing with production; each row has its table name as mg_tableclass value
- **_or filter pattern**: needed for unscoped queries where results should include multiple table types (e.g., both Networks and Catalogues)
- **Network count difference (13 vs 10)**: accepted — Catalogues now appear separately in Networks queries because Catalogues extends Networks (3 extra: main catalogue, testUMC, UMCG)
- **Subpopulations.resource filter type**: must use `ResourcesFilter` (not `CollectionsFilter`) because the ref column targets the base `Resources` table
- **DataCatalogueFlat.csv**: independent model, no changes needed
- **Table inheritance behavior**: querying a table includes subtable rows automatically (LEFT JOIN); no explicit mg_tableclass filter needed when querying subtable endpoints
- **"FORCE-NEN collections"**: is a Collection used as a catalogue scope — ScopedCollection fallback handles this; consider reclassifying as Catalogue in future

## Phase 7: Reclassify FORCE-NEN collections as Catalogue [DONE]

### Context
"FORCE-NEN collections" was a Collection acting as catalogue scope — semantically it's a Catalogue. Required `ScopedCollection` hack in landing page.

### Changes made:
- `datacatalogue/Collections.csv` — removed "FORCE-NEN collections" row
- `datacatalogue/Catalogues.csv` — added "FORCE-NEN collections" with shared fields
- `datacatalogue/Subpopulations.csv` — changed 8 subpopulation refs from "FORCE-NEN collections" to "FORCE-NEN"
- `datacatalogue/Collections.csv` — set `part of networks` = "FORCE-NEN collections" on FORCE-NEN row
- `datacatalogue_aggregates/Collections.csv` — changed collection ref from "FORCE-NEN collections" to "FORCE-NEN"
- `[catalogue]/index.vue` — removed ScopedCollection query, collectionIdFilter variable, and Collections fallback

### Verification:
- vitest 29/29 pass
- typecheck clean

## Open items
- `mg_tableclass` strings duplicated across 3 files (useHeaderData, variables/index, resourceType/index) — could extract to shared constant
