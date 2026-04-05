# Plan: Multiple Inheritance & YAML Profile Format v8

**Spec**: `.plan/specs/yaml-profile-format-v8.md`

## Context

EMX2 data models are defined in flat CSV files (`molgenis.csv`). This branch adds:
1. **Multiple inheritance** — a table can extend multiple parents via `tableExtends`
2. **EXTENSION/EXTENSION_ARRAY column types** — discriminator columns that drive which child tables get rows (replaces `mg_tableclass` for new-style inheritance)
3. **Structured YAML format** — one file per table, sections, imports
4. **Profile system** — tags on tables/columns that control visibility per deployment
5. **Schema editor profile UI** — display, edit, and activate profiles

CSV with `tableExtends` remains fully supported.

---

## Core Concept: Multiple Inheritance

The core backend feature is **multiple inheritance**. A table's `inheritNames` is a String array — each entry is a direct parent. FK is created to each parent. All parents must share the same root table.

Everything else is layered on top:

| Concept | What it is | Backend impact |
|---|---|---|
| Multiple inheritance | `inheritNames = ["sampling", "sequencing"]` | Core feature: FKs, column merging, recursive insert/update/delete |
| `mg_tableclass` | Old-style discriminator column (auto-managed) | Used when root has NO profile column (backward compat) |
| EXTENSION column | New-style discriminator (user-visible, pick-one) | Same role as `mg_tableclass` but explicit. Extends STRING. |
| EXTENSION_ARRAY column | Multi-value discriminator (pick-many) | Like EXTENSION but extends STRING_ARRAY — rows in multiple children |
| `TableType.INTERNAL` | Table type for non-selectable child tables | **UX-only** — backend never branches on it. Means "don't show in profile selector dropdown". Was called BLOCK. |

### Terminology

> **Note**: Phase 5.5 established new terminology. See `.plan/specs/new_naming.md` for the full naming conventions.

- **Parent table** = any table listed in `inheritNames`
- **Child table** = any table whose `inheritNames` includes this table
- **Root table** = the top of the inheritance tree (no parents)
- **Internal table** = a child table marked `TableType.INTERNAL` (UX hint: not selectable by users). Previously called "block". Backend treats it identically to any other child table.
- **Extension column** = an EXTENSION or EXTENSION_ARRAY column on the root table that drives which child tables get rows on save
- **Profile** = a tag on tables/columns controlling visibility per deployment (e.g. "wgs", "imaging")
- **Active profiles** = schema-level setting determining which profile-tagged items are visible

### How it works

```
Experiments (root, PK: id, columns: name/date, EXTENSION: experiment type)
  ├── sampling (INTERNAL, extends Experiments, columns: sample type/tissue type)
  ├── sequencing (INTERNAL, extends Experiments, columns: library strategy/read length)  
  ├── WGS (extends sampling + sequencing, columns: coverage)
  └── Imaging (extends Experiments, columns: modality/body part)
```

- `sampling` and `sequencing` are **internal tables** — they group shared columns but users don't select them directly
- `WGS` and `Imaging` are **selectable child tables** — they appear in the extension dropdown
- When a user inserts with `experiment type = "WGS"`, `insertBatch` recursively creates rows in WGS → sampling → Experiments and WGS → sequencing → Experiments (with upsert for diamond)
- Querying Experiments auto-joins all children via LEFT JOIN (wide sparse result)

### Inheritance style decision

One check: `getRootTable().getProfileColumn() != null`
- **Yes** → new-style: no `mg_tableclass`, extension column is the discriminator
- **No** → old-style: `mg_tableclass` added to root (backward compat, single parent only)

---

## Key Files

| File | Role |
|---|---|
| `TableMetadata.java` | Core model: `inheritNames`, `getInheritedTables()`, `getAllInheritedTables()`, `getRootTable()`, `getSubclassTables()`, `getProfileColumn()`, `hasColumnInParent()`, `getQualifiedName()` |
| `SqlTableMetadataExecutor.java` | DDL: `executeSetInherit()` with `addMgTableclass` boolean |
| `SqlTableMetadata.java` | `setInheritNames()` validation: same root, no reroot, PK check |
| `SqlTable.java` | Row management: unified discriminator, `subclassRows` batching, per-row delete of non-matching children |
| `SqlQuery.java` | `tableWithInheritanceJoin()`: ancestor INNER JOIN + child LEFT JOIN (PK-only). `whereConditionSearch()`: self + ancestors |
| `MetadataUtils.java` | Persistence: `inheritNames` as varchar[] |
| `Emx2.java` | CSV parser: `tableExtends` as comma-separated |
| `Emx2Yaml.java` | YAML parser: hierarchical format with extensions, sections, profiles |
| `GraphqlSchemaFieldFactory.java` | GraphQL: `inheritNames` as String[] list, `profiles`, `activeProfiles` |
| `ColumnTypeRdfMapper.java` | RDF: EXTENSION/EXTENSION_ARRAY mapped to STRING |

---

## Implementation Phases

### Phase 1: Java Metadata Model — COMPLETE

Extended core metadata: `TableType.INTERNAL` (was BLOCK), `ColumnType.EXTENSION/EXTENSION_ARRAY` (was PROFILE/PROFILES), `inheritNames` as String[], helper methods on `TableMetadata`.

### Phase 2: SQL Layer — COMPLETE

Multiple inheritance in PostgreSQL. Single `executeSetInherit()` method. Unified row management (same `subclassRows` batching for both mg_tableclass and extension discriminator). Simplified joins and search. Migration for `table_inherits` varchar → varchar[].

### Phase 3: GraphQL API — COMPLETE

- `inheritNames` exposed as String[] list
- EXTENSION/EXTENSION_ARRAY columns queryable via GraphQL
- `TableType.INTERNAL` exposed in `_schema` metadata query
- Mutations with extension values create correct child table rows

### Phase 4: IO Pipeline — COMPLETE

- CSV `tableExtends` round-trips comma-separated multi-parent
- `TableType.INTERNAL` round-trips in CSV export/import

### Phase 5: Frontend — Schema & Explorer Apps — COMPLETE

EXTENSION/EXTENSION_ARRAY columns work in the UI. Schema editor supports multiple inheritance editing. Diagrams show multiple parent edges.

### Phase 5.5: Naming & Terminology — COMPLETE

Established naming conventions. `PROFILE` → `EXTENSION`, `PROFILES` → `EXTENSION_ARRAY`. "Profile" reserved for visibility tags.

### Phase 5.6: Rename PROFILE → EXTENSION column types — COMPLETE

Full codebase rename with migration34.sql.

### Phase 6: YAML Parser & Web API — COMPLETE

**See**: `.plan/plans/phase6-yaml-parser.md` for full details.

Steps completed:
- Step 1-2: Table file parser + YAML export with roundtrip tests
- Step 3: Template file parser with wildcard expansion
- Step 4: Profile filtering backend (migration35.sql, GraphQL `applyProfileFilter`, `activeProfiles`)
- Step 5: Frontend profile filtering (`applyProfileFilter: true` on user-facing queries, activeProfiles mutation)
- Step 6: Wired hierarchical YAML into web API (GET/POST/DELETE)
- Step 6b: YAML+ZIP download/upload with `molgenis.yaml` marker

### Phase 7: Schema Editor Profile Support — COMPLETE

**Goal**: Display, edit, and activate profiles in the schema editor UI.

**Summary:**
- `utils.ts`: `profiles` on tables/columns + `activeProfiles` on `_schema` in GraphQL query; `getAvailableProfiles()` utility function
- `TableView.vue` / `ColumnView.vue`: display profiles as `[wgs]` after semantics
- `TableEditModal.vue` / `ColumnEditModal.vue`: `InputCheckbox` multiselect from known profiles + add-new-profile input
- `Schema.vue`: activeProfiles `InputCheckbox` in header (select from existing only, no add-new)
- CSS: `.profiles-checkboxes :deep(.form-check-inline) { display: block }` for vertical stacking
- 5 Playwright e2e tests (display on tables, display on columns, table edit modal, column edit modal, active profiles header)

### Phase 8: Extract CSV Models to YAML Table Definitions — TODO

**Goal**: Convert existing CSV table definitions to YAML format under `data/templates/`. CSV originals untouched. This phase focuses on **table definitions only** — not demodata, ontologies, settings, or template import logic.

**Analysis of model groupings:**

| Template dir | Source CSVs | Profile YAML files (12 templates share datacatalogue) |
|---|---|---|
| `petstore/` | `specific/petstore.csv` | PetStore.yaml |
| `typetest/` | `specific/typetest.csv` | TypeTest.yaml |
| `pages/` | `specific/Pages.csv` | Pages.yaml |
| `datacatalogue/` | All 31 `shared/*.csv` + `specific/CatalogueOntologies/` + `specific/Catalogue aggregates.csv` | DataCatalogue, CohortsStaging, INTEGRATECohorts, NetworksStaging, RWEStaging, SharedStaging, StudiesStaging, UMCGCohorts, UMCUCohorts, DataCatalogueAggregates, PatientRegistry, FAIRGenomes, ImageTest |

Key insight: 12 profiles are different **views** of one shared model (different profileTags select different subsets of tables/columns from the same 31 CSVs).

**Output structure:**
```
data/templates/
├── petstore/
│   ├── petstore.yaml              # template file
│   └── tables/*.yaml              # from specific/petstore.csv
├── typetest/
│   ├── typetest.yaml
│   └── tables/*.yaml
├── pages/
│   ├── pages.yaml
│   └── tables/*.yaml
└── shared/
    ├── tables/*.yaml              # from 31 shared/*.csv → YAML
    ├── datacatalogue.yaml         # template: profileTags: DataCatalogueFlat
    ├── cohortstaging.yaml         # template: profileTags: CohortsStaging
    ├── patientregistry.yaml       # template: profileTags: Patient registry, DataCatalogueFlat
    ├── fairgenomes.yaml           # template: profileTags: FAIR Genomes
    └── ...                        # remaining template files
```

**Approach:**
1. Write a conversion utility (Java or script) that reads CSV → SchemaMetadata → YAML via `Emx2Yaml.toYamlDirectory()`
2. Start with PetStore (simplest, standalone)
3. Convert datacatalogue shared models
4. Create template YAML files (just name, description, profileTags for now)
5. Verify roundtrip: YAML → SchemaMetadata → compare with CSV → SchemaMetadata

**Not in scope for Phase 8:**
- demodata, ontologies, settings (these stay in `_demodata/`, `_ontologies/`, `_settings/`)
- Template import logic (creating schemas, loading demodata, setting permissions)
- `firstCreateSchemasIfMissing` cascading
- `ontologiesToFixedSchema` / `additionalFixedSchemaModel`

### Phase 9: Template Import Process — TODO

**Goal**: Make `ImportProfileTask` (or a new task) understand the YAML template format, so `data/templates/` directories can fully replace `data/_profiles/` + `data/_models/`.

**Features to support in YAML templates:**
- `demodata: ./demodata/` — relative path to demo data CSVs within template dir
- `ontologies: ./ontologies/` — ontology tables within template dir
- `settings: ./settings/` — settings files within template dir
- `permissions:` — role assignments (setViewPermission, setEditPermission)
- `fixedSchemas:` — ontologiesToFixedSchema, additionalFixedSchemaModel
- `dependencies:` — replaces `firstCreateSchemasIfMissing` (create other schemas first)
- Resolution of relative paths within template directory

### Phase 10: Documentation — TODO

**Goal**: Update user-facing docs in `docs/molgenis/`.

**Files to update**:
- `docs/molgenis/use_schema.md` — multiple inheritance, INTERNAL table type
- `docs/molgenis/CSV.md` — `tableExtends` with comma-separated parents
- `docs/molgenis/dev_profiles.md` — EXTENSION/EXTENSION_ARRAY column types, profiles, templates
- `docs/molgenis/dev_architecture.md` — architecture overview
- New: YAML format documentation with examples
- New: Template migration guide (CSV → YAML)

---

## Verification

After each phase:
1. `./gradlew :backend:molgenis-emx2:test` — core model
2. `./gradlew :backend:molgenis-emx2-sql:test` — SQL (290 tests)
3. `./gradlew :backend:molgenis-emx2-io:test` — IO (68 tests)
4. `./gradlew :backend:molgenis-emx2-graphql:test` — GraphQL
5. `./gradlew :backend:molgenis-emx2-rdf:test` — RDF (103 tests)
6. Full suite for backward compat
7. `cd apps/schema && npx playwright test` — schema editor e2e (5 tests)
