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
└── Networks (extends Resources) → foaf:Organisation (or similar)
    └── Catalogues (extends Networks) → dcat:Catalog
```

**CRUCIAL**: All tables stay in ONE file `Resources.csv` using `tableName` column to discriminate (like `Processes.csv` pattern). This preserves column order.

---

## Phase 1: Data Model — Edit Resources.csv

### Resources (base table) — keep shared columns:
- id, pid, name, local name, acronym
- overview heading, hricore
- website, description, keywords, contact email, logo
- start year, end year, time span description
- countries, continents, regions
- issued, modified
- internal/external identifiers (refbacks)
- contributors heading, organisations involved, publisher, creator, people involved, contact point
- part of networks (refback) — stays in Resources (all resources can be part of networks)
- datasets (refback) — stays in Resources (networks define harmonized variable sets)
- samplesets (refback)
- counts (refback) — stays in Resources (counts applicable to networks too)
- standards heading + mappings, common data models, ETL vocabularies — stays
- information heading, publications, documentation
- funding sources, funding scheme, funding statement
- citation requirements, acknowledgements, provenance statement, supplementary information
- theme, applicable legislation
- status
- NO `rdf type` computed field (removed entirely)
- NO `fdp endpoint`, `ldp membership relation`, `conforms to`, `has member relation`
- NO `catalogue type`, `network type`
- NO `type` (Resource types) — moves to Collections
- NO `data resources`, `child networks`, `parent networks`

### Collections (extends Resources) — add rows with tableName=Collections:
- Table row: `Collections,Resources,,,,,,,,,,dcat:Dataset,...`
- type (ontology_array, Resource types — collection types)
- cohort type, clinical study type, registry or health record type
- design and structure section (all fields: design, design description, design schematic, data collection type, data collection description, reason sustained, record trigger, unit of observation)
- subpopulations (refback), collection events (refback)
- population section (all fields: number of participants, number of participants with samples, underlying population, population description, population of interest, population age groups, age min/max, inclusion/exclusion criteria, population entry/exit, population disease, oncology fields, population coverage/not covered)
- available data and samples heading + areas of information, biospecimen, languages, multiple entries, data dictionary, disease details
- linkage section (has identifier, identifier description, prelinked, linkage options/description/possibility, linked resources)
- access conditions section (informed consent, access rights, data access/use conditions, access fees, access identifiable data, access third party, biospecimen access, governance, approval for publication)
- updates section (release type/description, number of records, release frequency, refresh time, lag time, refresh period, date last refresh, preservation)
- quality section (standard operating procedures, qualification, audit, completeness, quality description, validation, correction methods)
- study details section (all EMA study fields)
- study methods section

### Networks (extends Resources) — add rows with tableName=Networks:
- Table row: `Networks,Resources,,,,,,,,,,foaf:Organisation,...` (or appropriate semantic)
- network type (ontology_array, CatalogueOntologies, Network types)
- data resources (ref_array → Resources)
- child networks (ref_array → Networks)
- parent networks (refback from child networks)

### Catalogues (extends Networks) — add rows with tableName=Catalogues:
- Table row: `Catalogues,Networks,,,,,,,,,,dcat:Catalog,...`
- catalogue type (ontology, CatalogueOntologies, CatalogueTypes)
- fdp endpoint (ref → Endpoint)
- ldp membership relation (hyperlink)
- conforms to (hyperlink)
- has member relation (hyperlink)

---

## Phase 2: Update referencing CSV model files

### Change refTable to Collections:
- `Subpopulations.csv`: refTable Resources → Collections
- `Collection events.csv`: refTable Resources → Collections
- `Subpopulation counts.csv`: references Subpopulations (no change needed)

### Keep refTable as Resources:
- `Datasets.csv` — networks can define harmonized variables
- `Samplesets.csv` — keep at Resources level
- `Resource counts.csv` — counts applicable to networks too
- `Contacts.csv`, `Publications.csv`, `Documentation.csv`
- `Variable mappings.csv`, `Dataset mappings.csv`, `Resource mappings.csv`
- `External identifiers.csv`, `Internal identifiers.csv`
- `Linkages.csv`, `Variables.csv`, `Variable values.csv`, `Reused variables.csv`
- `Processes.csv`, `Individuals.csv`, `Materials.csv`, `Agents.csv`

### Special case:
- `Endpoint.csv`: `metadataCatalog` refTable → Catalogues

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
