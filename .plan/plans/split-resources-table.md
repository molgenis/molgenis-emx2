# Plan: Split Resources into Resources, Collections, Networks, Catalogues

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

**CRUCIAL**: All tables stay in ONE file `Resources.csv` using `tableName` column to discriminate (like `Processes.csv` pattern). Row order preserved — only `tableName` column changes.

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

---

## Phase 2: Update referencing CSV model files [DONE]

### Changed:
- `Endpoint.csv`: `metadataCatalog` refTable → `Catalogues`

### Kept as Resources (inheritance handles it):
- `Subpopulations.csv`, `Collection events.csv` — kept refTable=Resources
  (Collections IS-A Resources, so refs work via inheritance; changing to Collections caused validation errors)
- `Datasets.csv`, `Samplesets.csv`, `Resource counts.csv`
- `Contacts.csv`, `Publications.csv`, `Documentation.csv`
- All mapping/identifier/variable CSVs
- `Processes.csv`, `Individuals.csv`, `Materials.csv`, `Agents.csv`

### Demo data split [DONE]:
- `datacatalogue/Resources.csv` → Collections (96), Networks (2), Catalogues (11)
- `patient_registry/Resources.csv` → Collections (6), Catalogues (1)
- `shared-examples/Resources.csv` → Collections (1)
- `cohortstaging/Resources.csv` → Collections.csv (header only)
- `networkstaging/Resources.csv` → Networks.csv (header only)
- `_ontologies/Resources.csv` → Networks.csv (24 networks)
- Removed `rdf type` column from all data files

### Test [DONE]:
- `ResourcesSplitTest.java` — verifies tables exist, inheritance correct, demo data loaded
- All tests pass

---

## Phase 3: Update DataCatalogueFlat.csv

Verify alignment with new shared model. The specific model already has Collections/Catalogues — may need adjustments to match new inheritance structure.

---

## Phase 4: Update catalogue app frontend

### Key files to modify:

**GraphQL queries** — change `Resources` → `Collections` or `Networks`:
- `apps/catalogue/app/pages/[catalogue]/[resourceType]/index.vue` — search page
- `apps/catalogue/app/pages/[catalogue]/[resourceType]/[resource]/index.vue` — detail page
- `apps/catalogue/app/gql/resource.js` — shared query
- `apps/catalogue/app/gql/variable.ts` — variable query
- `apps/catalogue/app/composables/useHeaderData.ts` — header counts

**Landing pages:**
- `apps/catalogue/app/components/landing/Central.vue`
- `apps/catalogue/app/components/landing/CohortsOnly.vue`

**Interfaces & stores:**
- `apps/catalogue/app/interfaces/catalogue.ts` — IResources interface
- `apps/catalogue/app/interfaces/types.ts` — IShoppingCart
- `apps/catalogue/app/stores/useDatasetStore.ts`

**Components:**
- `apps/catalogue/app/components/content/ContentBlockCatalogues.vue`
- `apps/catalogue/app/components/ResourceCard.vue`
- `apps/catalogue/app/components/store/ModalResourceList.vue`
- `apps/catalogue/app/components/store/ModalResourceListItem.vue`
- `apps/catalogue/app/pages/[catalogue]/variables/index.vue`
- `apps/catalogue/app/pages/[catalogue]/variables/[variable].vue`

### Approach:
- Collection search → query `Collections` table (no type filter needed)
- Network search → query `Networks` table (no type filter needed)
- Detail pages → route determines which table to query
- Remove type-based filtering logic (`type.tags === "collection"` etc.)

---

## Verification
1. Load model in dev, verify tables with correct inheritance
2. `./gradlew test` — backend
3. `pnpm test` — catalogue app
4. DCAT/RDF: dcat:Catalog for Catalogues, dcat:Dataset for Collections
5. Catalogue app: search, detail pages work

## Open questions
- Phase 3: Does DataCatalogueFlat.csv need changes or is it independent?
- Phase 4: How much frontend type-filtering logic needs removal vs adaptation?
- Should existing CatalogueTest table count assertions be updated (was 24, now more)?
