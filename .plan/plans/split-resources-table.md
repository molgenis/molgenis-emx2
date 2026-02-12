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

## Phase 3: Update DataCatalogueFlat.csv [DONE — NO CHANGES NEEDED]

`data/_models/specific/dev/DataCatalogueFlat.csv` is a completely separate standalone model (no inheritance, no profile tags). It already has Collections/Catalogues tables with its own `collectionType`-based visibility. It is independent from the shared model we changed — no updates required.

---

## Phase 4: Update catalogue app frontend [DONE]

### Files modified (11):

**GraphQL queries and composables:**
- `useHeaderData.ts` — queries Catalogues/Collections_agg/Networks_agg directly, removed type.tags filters, uses mg_tableclass for variable filtering
- `gql/resource.js` — queries Collections instead of Resources
- `gql/variable.ts` — queries Networks instead of Resources, uses NetworksFilter

**Pages:**
- `pages/index.vue` — queries Catalogues table directly (no type filter)
- `pages/[catalogue]/index.vue` — queries Catalogues/Collections/Networks with proper filter types, uses Collections_groupBy
- `pages/[catalogue]/[resourceType]/index.vue` — dynamic table name (Collections/Networks) based on route param
- `pages/[catalogue]/[resourceType]/[resource]/index.vue` — dynamic table name, simplified partOfNetworks, uses catalogueType
- `pages/[catalogue]/variables/index.vue` — queries Networks, uses mg_tableclass for unscoped filter

**Components:**
- `Central.vue` — Resources_agg→Collections_agg, Resources_groupBy→Collections_groupBy
- `CohortsOnly.vue` — same as Central.vue

**Interfaces:**
- `catalogue.ts` — removed rdfType from IResources

### Not changed (no changes needed):
- `types.ts`, `useDatasetStore.ts` — IResources interface still valid for all subtypes
- `ResourceCard.vue` — type display works for Collections, gracefully empty for Networks
- `ContentBlockCatalogues.vue` — no Resources references
- Central.vue/CohortsOnly.vue templates — pre-existing bugs with Cohorts_agg etc., not related to this split

### Key patterns used:
- Dynamic table name mapping: `{ collections: 'Collections', networks: 'Networks' }`
- `mg_tableclass` filter (format: `schema.TableName`) to identify Networks in variable queries
- Removed all `type.tags` filters — table separation handles type discrimination
- `partOfNetworks`/`parentNetworks` filters preserved, `type.name` checks removed

---

## Phase 5: Verification and UI testing

### TODO:
1. Load model in dev, verify tables with correct inheritance
2. `./gradlew test` — backend
3. `pnpm test` — catalogue app frontend tests
4. Manual testing: catalogue app at port 3000
5. DCAT/RDF: dcat:Catalog for Catalogues, dcat:Dataset for Collections

## Open questions
- Does mg_tableclass filtering work through ref fields in GraphQL? (needs runtime verification)
- Central.vue/CohortsOnly.vue have pre-existing broken template references — separate fix needed?
