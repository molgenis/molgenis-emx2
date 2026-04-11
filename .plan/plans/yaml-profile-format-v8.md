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

The core backend feature is **multiple inheritance**. A table's `extendNames` is a String array — each entry is a direct parent. FK is created to each parent. All parents must share the same root table.

Everything else is layered on top:

| Concept | What it is | Backend impact |
|---|---|---|
| Multiple inheritance | `extendNames = ["sampling", "sequencing"]` | Core feature: FKs, column merging, recursive insert/update/delete |
| `mg_tableclass` | Old-style discriminator column (auto-managed) | Used when root has NO profile column (backward compat) |
| EXTENSION column | New-style discriminator (user-visible, pick-one) | Same role as `mg_tableclass` but explicit. Extends STRING. |
| EXTENSION_ARRAY column | Multi-value discriminator (pick-many) | Like EXTENSION but extends STRING_ARRAY — rows in multiple children |
| `TableType.INTERNAL` | Table type for non-selectable child tables | **UX-only** — backend never branches on it. Means "don't show in variant selector dropdown". Was called BLOCK. |

### Terminology

> **Note**: Phase 5.5 established new terminology. See `.plan/specs/yaml-profile-format-v8.md` for the full naming conventions.

- **Parent table** = any table listed in `extendNames`
- **Child table** = any table whose `extendNames` includes this table
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
| `TableMetadata.java` | Core model: `extendNames`, `getInheritedTables()`, `getAllInheritedTables()`, `getRootTable()`, `getSubclassTables()`, `getProfileColumn()`, `hasColumnInParent()`, `getQualifiedName()` |
| `SqlTableMetadataExecutor.java` | DDL: `executeSetInherit()` with `addMgTableclass` boolean |
| `SqlTableMetadata.java` | `setExtendNames()` validation: same root, no reroot, PK check |
| `SqlTable.java` | Row management: unified discriminator, `subclassRows` batching, per-row delete of non-matching children |
| `SqlQuery.java` | `tableWithInheritanceJoin()`: ancestor INNER JOIN + child LEFT JOIN (PK-only). `whereConditionSearch()`: self + ancestors |
| `MetadataUtils.java` | Persistence: `extendNames` as varchar[] |
| `Emx2.java` | CSV parser: `tableExtends` as comma-separated |
| `Emx2Yaml.java` | YAML parser: hierarchical format with variants, sections, profiles |
| `GraphqlSchemaFieldFactory.java` | GraphQL: `extendNames` as String[] list, `profiles`, `activeProfiles` |
| `ColumnTypeRdfMapper.java` | RDF: EXTENSION/EXTENSION_ARRAY mapped to STRING |

---

## Implementation Phases

### Phase 1: Java Metadata Model — COMPLETE

Extended core metadata: `TableType.INTERNAL` (was BLOCK), `ColumnType.EXTENSION/EXTENSION_ARRAY` (was PROFILE/PROFILES), `extendNames` as String[] (legacy `inheritNames` kept as `@Deprecated` delegator), helper methods on `TableMetadata`.

### Phase 2: SQL Layer — COMPLETE

Multiple inheritance in PostgreSQL. Single `executeSetInherit()` method. Unified row management (same `subclassRows` batching for both mg_tableclass and extension discriminator). Simplified joins and search. Migration for `table_inherits` varchar → varchar[].

### Phase 3: GraphQL API — COMPLETE

- `extendNames` exposed as String[] list
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

### Phase 7b: List-based columns + column-level imports — TODO

**Goal**: Change YAML format from keyed-maps to list-of-maps for **columns, profiles, and variants** (consistent pattern), and add `import:` support inside column lists for composable table definitions.

**Motivation**: List format (a) preserves explicit ordering, (b) enables `import:` entries at any position in the column sequence, (c) is more self-documenting for naive users (`- name: X` vs implicit map key), (d) one consistent pattern for all named collections.

**Format change — columns:**
```yaml
# BEFORE (keyed map)
columns:
  id:
    key: 1
  status: {}

# AFTER (list of maps)
columns:
  - name: id
    key: 1
  - name: status

# NEW: column-level imports (inserted in order)
columns:
  - name: id
    key: 1
  - import: columns/demographics.yaml
  - name: status
  - import: columns/consent_section.yaml
```

Imported column files can define:
- A list of columns: `columns: [{ name: x, type: int }, ...]`
- A section: `section: Demographics` + `columns: [...]`

**Format change — profiles (bundle root):**
```yaml
# BEFORE
profiles:
  catalogue_core:
    description: Core catalogue columns
    internal: true
  cohort_core:
    includes: [catalogue_core]

# AFTER
profiles:
  - name: catalogue_core
    description: Core catalogue columns
    internal: true
  - name: cohort_core
    includes: [catalogue_core]
```

**Format change — variants (table level):**
```yaml
# BEFORE
variants:
  sampling:
    description: Sample collection
    internal: true
  WGS:
    extends: [sampling, sequencing]

# AFTER
variants:
  - name: sampling
    description: Sample collection
    internal: true
  - name: WGS
    extends: [sampling, sequencing]
```

**Steps:**

Red-green approach: update tests first (RED — they fail), then fix code to pass (GREEN).

#### Step 1: RED — Update test resources to list format (6 YAML files)
- Convert all test YAML files under `src/test/resources/yaml-model/` to list-of-maps format
- Convert `columns:`, `profiles:`, and `variants:` maps to lists with `name:` field
- Sections/headings become entries in the columns list (`section:` / `heading:` key)
- Add new test resource files for column-level imports
- Tests now fail because parser still expects map format

#### Step 2: RED — Update test code for list format (~58 tests)
- `Emx2YamlBundleTest.java` (50 tests): update inline YAML strings to list format (columns, profiles, variants)
- `Emx2YamlTest.java`, `Emx2ChangelogTest.java`, `Emx2CsvMigrationTest.java`: same
- Add new tests:
  - Column-level import: columns from file inserted at correct position
  - Multiple imports at different positions in one table
  - Section import (imported file defines `section:` + `columns:`)
  - Import collision detection (duplicate column name across imports)
  - Import inside section columns
- Verify all tests fail (RED)

#### Step 3: GREEN — Update bundle record classes
- `TableDef.java`: `Map<String, DataColumn> columns` → `List<DataColumn> columns` (add `name` field to DataColumn or use a new ColumnEntry type)
- `SectionDef.java`: same change
- `HeadingDef.java`: same change
- Decide on a union type for column list entries: `{name:}` = column, `{section:}` = section, `{import:}` = import directive

#### Step 4: GREEN — Update `Emx2Yaml.java` parser
- `parseColumnsAtDepth()`: change from iterating `Map.entrySet()` to iterating `List<Map>`, extracting `name:` from each entry
- Parse `profiles:` and `variants:` as lists with `name:` field (both at bundle root and table level)
- Handle `section:` entries (currently detected by presence of nested `columns:`, now explicit)
- Handle `heading:` entries within sections
- Preserve depth validation (max 2)
- Run tests — existing parsing tests should pass (GREEN)

#### Step 5: GREEN — Update `Emx2Yaml.java` serializer
- `buildDataColumnMap()` → `buildDataColumnList()`: emit list-of-maps with `name:` field
- Profiles/variants serialization: emit as lists with `name:` field
- Section/heading serialization: emit `section:` / `heading:` key instead of relying on map nesting
- Run tests — roundtrip tests should now pass (GREEN)

#### Step 6: GREEN — Update `ImportExpander.java` for column-level imports
- Detect `import:` entries inside column lists
- Expand imported file contents in-place (preserving position in list)
- Imported file resolution: reuse existing `resolveImportPaths()` with cycle/escape detection
- Collision detection: duplicate column names across imports + inline
- Run tests — column-level import tests should now pass (GREEN)

#### Step 7: CSV→YAML converter + regenerate all profiles
- Write a Java conversion utility (JUnit test or main class) that does: CSV → `SchemaMetadata` (via `Emx2.fromReader()`) → new YAML format (via updated `Emx2Yaml` serializer)
- This replaces the fragile text-level YAML transformations from steps 1-2 with programmatically correct output
- Handles all nesting correctly: columns, sections, headings, profiles, variants
- Regenerate all production YAML files under `profiles/` from their CSV source in `data/_models/`
- Reusable for future CSV→YAML migrations (replaces old Phase 8)
- Verify roundtrip: CSV → SchemaMetadata → YAML → SchemaMetadata → compare

**Model groupings (from old Phase 8 analysis):**

| Profile dir | CSV source |
|---|---|
| `petstore/` | `data/_models/specific/petstore.csv` |
| `typetest/` | `data/_models/specific/typetest.csv` |
| `pages/` | `data/_models/specific/Pages.csv` |
| `shared/` (12 profiles) | `data/_models/shared/*.csv` (31 files) |

Key insight: 12 profiles (DataCatalogue, CohortsStaging, PatientRegistry, FAIRGenomes, etc.) are different profile-tagged views of one shared model from the same 31 CSVs.

#### Step 8: Update documentation
- `docs/molgenis/yaml_format.md`: all examples to list format, document `import:` in columns, update profiles/variants format, update migration table

**Estimated effort**: Medium-high. Steps 1-2 (RED) establish the target. Steps 3-6 (GREEN) are the core work (~2-3 days). Step 7 is the reliable conversion path.

**Not in scope:**
- demodata, ontologies, settings (stay in `_demodata/`, `_ontologies/`, `_settings/`)
- Template import logic (creating schemas, loading demodata, setting permissions)
- `firstCreateSchemasIfMissing` cascading
- `ontologiesToFixedSchema` / `additionalFixedSchemaModel`

---

### Phase 7c: Review-driven improvements — IN PROGRESS

After multi-persona review (backend, frontend, data manager, naive researcher), the following issues need addressing. Grouped by track.

**Status**: Tracks 1 and 2 COMPLETE. Tracks 3-6 TODO.

#### Track 1: Correctness blockers — COMPLETE

**1.1 Fix `buildVariantDefList` extends suppression bug** — `Emx2Yaml.java:1111`
- Currently: `def.name().equals(defaultParent)` — wrong check, never fires
- Fix: `def.extendNames().get(0).equals(defaultParent)`
- Symptom: redundant `extends:` written to YAML, round-trip bloat

**1.2 Fix path-traversal in template import path** — `Emx2Yaml.java:1221-1241`
- `resolveImport` (template flow) has no `startsWith(baseReal)` escape check
- `imports: [../../../etc/passwd]` would pass through
- Fix: reuse `ImportExpander.resolveImportPaths()` or add same guards
- Add test for traversal attempt

**1.3 Safe type coercion for all String attributes** — `Emx2Yaml.java:772-848`
- `refTable`, `refBack`, `refLink`, `refLabel`, `refSchema`, `computed`, `label`, `oldName`, `validation`, `visible`, `description` all use `(String) attrs.get(...)` — ClassCastException on numeric values
- `defaultValue` already fixed
- Fix: replace casts with `.toString()` coercion, same pattern as defaultValue
- Add test for numeric value in refTable

#### Track 2: Architectural cleanup — COMPLETE

**2.1 `TableDef.columns` from `Object` to `List<>`** — `bundle/TableDef.java:26`
- Remove backward-compat map format entirely (nothing uses it now)
- Change type to `List<Map<String, Object>>` (or a proper ColumnEntry union type)
- Remove `getColumnsAsList()`, `hasColumns()`, `instanceof Map` branches
- Update callers

**2.2 Consolidate column-parsing paths** — `Emx2Yaml.java`
- `parseNewFormatTableIntoSchema` (line 390) and `materializeTableDef` (line 302) both call `parseColumnsAsList` but duplicate setup
- Merge: everything flows through `TableDef` as the canonical intermediate
- One entry point for parsing columns

**2.3 Consolidate `toBundle*` methods** — `Emx2Yaml.java:931-1001`
- 4 methods: `toBundleDirectory`, `toBundleDirectoryWithProfiles`, `toBundleSingleFile`, `toBundleSingleFileWithProfiles`
- Collapse to 2: `toBundleDirectory(schema, name, desc, dir, profileDefs)` and `toBundleSingleFile(schema, name, desc, file, profileDefs)`
- `profileDefs` is optional param (default `List.of()`)

#### Track 3: Design decisions — TODO

**3.1 Simple inline enum type** — new column type (`enum`)
- `- name: smoking status, type: enum, values: [never, former, current]`
- Avoids needing a separate ontology table for simple choice fields
- Backend: new `ColumnType.ENUM` or similar, stored as string with validation
- Separate phase — not in this refactor scope

**3.2 Profile precedence rule** — document and implement
- Inheritance order (most-specific wins on override): **column > heading > section > table**
- Union semantics already works via `includes:`
- Document clearly in `yaml_format.md`
- Add tests for precedence

#### Track 4: Server-side features — TODO

**4.1 Active-profile filtering done server-side** — GraphQL
- Backend should filter metadata responses based on active profiles
- Frontend shouldn't need to replicate `includes:` expansion
- Already partially done (`applyProfileFilter`) — verify and document

**4.2 Structured parse errors** — new error type
- Current: raw exception, line number only
- New: `{file, table, section/heading, column, message, line}` — structured
- Return via GraphQL mutation errors so UI can highlight the problematic entry
- Update error-throw sites in `Emx2Yaml.java` to include context

**4.3 Source provenance on imported columns** — debugging
- When `ImportExpander` splices an imported file's columns, tag them with a `_source` field
- Parser preserves it into the Column metadata as a non-persisted attribute
- API responses include it so UI can show "defined in columns/demographics.yaml"
- Strip before DDL apply

#### Track 5: Format + naming refinements — TODO

**5.1 Two-roundtrip equivalence rule**
- Roundtrip YAML → parse → serialize → YAML is ALLOWED to differ (comments lost, reformatting ok)
- But parse → serialize → parse → serialize must produce IDENTICAL output on the second pass
- Add test: idempotence of roundtrip after first normalisation pass

**5.2 Dual format support: CSV stays first-class**
- Data managers can use CSV or YAML — both are source-of-truth options
- New features added to YAML may also be backported to CSV format (keep old CSV naming)
- Long-term: YAML is the primary recommended format, CSV stays supported

**5.3 Profile naming standardisation** — rename with migration
- Current mix: `datacatalogueflat`, `cohortsstaging`, `umcgcohortsstaging`, `fair_genomes`
- Standardise: snake_case, `_core` suffix for internal base profiles
- Proposal: `datacatalogueflat` → `catalogue_core`, `cohortsstaging` → `cohort_staging`, etc.
- Breaking change — needs migration script for existing deployments
- Discuss scope before implementing

#### Track 6: Documentation overhaul — TODO

**6.1 Terminology section** — add glossary up-front in `yaml_format.md`
- Define: bundle, profile (was called subset), variant (vs extends/inheritance), section, heading, import, ontology vs ref, refTable, refback, computed, visible, semantics, CURIE, internal
- Short definitions, 1 line each

**6.2 Consolidate with `use_schema.md` / `schema.md`**
- Currently separate docs for schema model and YAML format
- Merge into one: YAML docs should fully replace the old schema docs
- Include: ref vs radio vs select vs ontology (all UI hints), ref_array vs checkbox vs multiselect

**6.3 Beginner-friendly type reference**
- Group the 50+ types: "text values", "numbers", "dates", "choices (single/multi)", "files", "references", "ontology"
- Explain which are UI hints for the same underlying type
- "How do I add a simple choice field?" worked example

**6.4 Rename "diamond inheritance" in variants docs**
- Current: "Diamond inheritance" — compiler-writer jargon
- New: "Multiple protocols on one row" (diamond inheritance)
- Keep technical term in parens for experts

**6.5 Document `required:` semantics**
- Allowed values: `true`, `false`, or a JavaScript expression returning a message
- Quoting is not needed (boolean is fine); quote only if writing an expression that starts with ambiguous characters
- Add examples of each

**6.6 Example bundle with all features** — new reference file
- Small RD3-inspired model (patient registry) with heavily reduced column count
- Demonstrates: sections, headings, variants, profiles, includes, column-level imports, semantics, ontology refs, computed fields, validation
- Goal: "one place to see everything"
- Location: `profiles/example/` or `profiles/reference/`

**6.7 Target audience for docs**
- YAML format docs target data modelers and developers, NOT naive researchers
- Naive researchers keep using Excel/CSV path (no YAML exposure needed)
- State this explicitly in docs introduction

---

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

### Phase 9b: Variant rendering architecture — TODO (discussion track)

**Goal**: Decide how variants are represented in YAML, GraphQL, and frontend rendering. This is a design discussion, not a ready-to-implement task.

**The problem** — raised by frontend review:
- Current YAML uses `section: X` + `variant: X` together to scope a section to a variant. This overloads the `section:` discriminator.
- In the frontend, detecting "is this a variant scope?" requires checking two keys: `entry.section && entry.variant`.
- But making `variant:` its own top-level discriminator raises architectural questions (see below).

**Complications to resolve**:

1. **Multi-variant selection**: A row can have multiple variants active (e.g. `[sampling, sequencing, WGS]` all at once via diamond inheritance). When rendering a form, do we:
   - Concatenate columns from all active variants into one flat list?
   - Show separate column groups per variant?
   - Let the data modeler decide via explicit grouping?

2. **Plain grouping inside variant scope**: A variant's columns may themselves need sections/headings. E.g. WGS variant could have a "Quality metrics" heading and a "Pipeline info" heading. Does `variant:` as a top-level discriminator allow nested sections?

3. **GraphQL representation**:
   - Should GraphQL schema metadata expose variants as a separate field (`table.variants[].columns`) or keep them flat with a `variantScope` attribute per column?
   - Should the same shape serve both authoring (YAML roundtrip) and rendering (form building)?
   - Or is it OK for GraphQL to flatten/denormalize for frontend convenience while YAML keeps the structural form?

4. **"Dumb frontend" principle**: Frontend devs want simple iteration. The backend should precompute the rendering tree for each active variant set, vs forcing the frontend to reimplement the inheritance logic.

**Options to evaluate**:

- **Option A**: Keep current `section: + variant:` overload, add frontend utility to detect variant scope.
- **Option B**: Promote `variant:` to top-level discriminator (rejected in discussion — loses composability with sections).
- **Option C**: Keep `section: + variant:` in YAML as authoring format; GraphQL exposes a computed rendering tree per active variant set (denormalized).
- **Option D**: Add `variantColumns` or similar explicit column grouping in GraphQL while YAML stays as-is.

**Decision needed**:
- Should YAML and GraphQL converge on the same shape, or can they diverge (YAML = authoring, GraphQL = rendering)?
- Who owns variant expansion: parser at load-time, or GraphQL resolver at query-time?

**Related files**:
- `profiles/shared/tables/Processes.yaml` — extensive variants (Analyses, Observations, Experiments, NGS sequencing...)
- `profiles/shared/tables/Observations.yaml` — variant-scoped sections
- `backend/molgenis-emx2-graphql/src/main/java/.../GraphqlSchemaFieldFactory.java` — where GraphQL metadata is exposed

**Not in scope for this phase**:
- Implementation. This track is for reaching architectural alignment before coding.

---

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
