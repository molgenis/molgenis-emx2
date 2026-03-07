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

### Key patterns used:
- Dynamic table name mapping: `{ collections: 'Collections', networks: 'Networks' }`
- `mg_tableclass` filter for discriminating table types — format is just the table name (e.g., `"Networks"`, `"Catalogues"`)
- `_or` filter needed for unscoped queries: each row has its own mg_tableclass value, so need `_or: [{mg_tableclass: "Networks"}, {mg_tableclass: "Catalogues"}]`
- `Subpopulations.resource` expects `ResourcesFilter` not `CollectionsFilter` — requires separate GraphQL variable even though filter shape is identical
- Dynamic GraphQL queries with `${isCollection ? ... : ...}` template literals for table-specific fields
- Removed all `type.tags` filters — table separation handles type discrimination

### Verified counts (matching production):
- EUChildNetwork: Collections 20, Networks 7, Variables 1990
- All (unscoped): Collections 96, Variables 2249
- Network count difference accepted: 13 vs 10 because Catalogues now show separately in Networks query (3 extra: main catalogue, testUMC, UMCG)

---

## Phase 5: Verification and Testing [DONE]

- All CI tests passing (green)
- renderForm e2e test fixed: `table=Resources` → `table=Collections`
- Landing page fixed: added ScopedCollection query for Collections (e.g., "FORCE-NEN collections")
- Merge conflict in useHeaderData.ts resolved
- Frontend tests: catalogue (29/29), tailwind-components (125/125) all passed

---

## Phase 6: Simplify frontend types [PLANNED]

### Context
GraphQL flattens inheritance — querying `Resources` returns ALL fields from all subtables. But the TypeScript generator uses `table.getColumns()` (own+parent only), so `IResources` only has 49 fields. Fix the generator to match GraphQL behavior, then revert most frontend type changes back to master.

### Step 1: Fix TypeScript generator
**File:** `backend/molgenis-emx2-typescript/src/main/java/org/molgenis/emx2/typescript/Generator.java`
- Line 85: `table.getColumns()` → `table.getColumnsIncludingSubclasses()`
- Makes `IResources` include all 154+ fields (matching GraphQL and master)

### Step 2: Regenerate catalogue.ts
- Run generator → updated `apps/catalogue/interfaces/catalogue.ts`
- Verify `IResources` has all fields

### Step 3: Revert component types to IResources
- `apps/catalogue/app/components/ResourceCard.vue` — prop type back to `IResources`
- `apps/catalogue/app/components/content/ContentBlockCatalogues.vue` — prop type back to `IResources`
- `apps/catalogue/app/components/content/cohort/GeneralDesign.vue` — prop type back to `IResources`

### Step 4: Revert list page to query Resources
**File:** `apps/catalogue/app/pages/[catalogue]/[resourceType]/index.vue`
- Query `Resources` table (like master) instead of separate Collections/Networks
- Use `mg_tableclass` filter instead of `type.tags`

### Step 5: Revert detail page to query Resources
**File:** `apps/catalogue/app/pages/[catalogue]/[resourceType]/[resource]/index.vue`
- Query `Resources` with all fields (like master)
- Use `mg_tableclass` filter where needed

### Keep as-is (necessary structural changes):
- `apps/catalogue/app/pages/[catalogue]/index.vue` — landing page queries Catalogues + ScopedCollection
- `apps/catalogue/app/pages/index.vue` — root redirect page
- `apps/catalogue/app/composables/useHeaderData.ts` — merged from master
- `apps/catalogue/app/components/landing/Central.vue` — Collections_agg/Networks_agg
- `apps/catalogue/app/components/landing/CohortsOnly.vue` — Collections_agg
- Variable pages — keep current filters

### Verification
1. `pnpm --filter catalogue test` — vitest passes
2. `pnpm --filter catalogue lint` — typecheck passes
3. `pnpm --filter tailwind-components test` — vitest passes
4. Push and verify CI passes

---

## Resolved Questions
- **mg_tableclass filtering**: works correctly — verified by comparing with production; each row has its table name as mg_tableclass value
- **_or filter pattern**: needed for unscoped queries where results should include multiple table types (e.g., both Networks and Catalogues)
- **Network count difference (13 vs 10)**: accepted — Catalogues now appear separately in Networks queries because Catalogues extends Networks (3 extra: main catalogue, testUMC, UMCG)
- **Subpopulations.resource filter type**: must use `ResourcesFilter` (not `CollectionsFilter`) because the ref column targets the base `Resources` table
- **DataCatalogueFlat.csv**: independent model, no changes needed
