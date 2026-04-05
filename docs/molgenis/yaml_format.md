# Template bundle format (`molgenis.yaml`)

> **What's in this doc**: how to define a MOLGENIS data model in YAML.
>
> - **Layer 0 — Orientation**: directory layout and the simplest possible bundle (one file, under 20 lines).
> - **Layer 1 — Tables**: defining tables, columns, types, keys, refs. Enough for most simple data models.
> - **Layer 2 — Extensions** *(optional)*: child tables with a subtype discriminator — model "this experiment is WGS or Imaging".
> - **Layer 3 — Subsets** *(optional)*: tag columns with named subsets so one bundle can produce different DDL for different deployments.
> - **Layer 4 — Templates** *(optional)*: named presets of subset activations, shown in the admin picker.
> - **Layer 5 — Semantics** *(optional)*: attach vocabulary URIs for RDF export and cross-system integration.
> - **Reference**: full key reference, column types, gotchas, FAQ.
>
> You only need Layers 2–5 when you need them. A zero-config bundle skips straight from Layer 0 to Layer 1.

---

## Layer 0 — Orientation

A bundle groups one or more table definitions, optional ontologies, demo data, and settings into a single deployable unit. Bundles live under `data/templates/` and are loaded by the import pipeline. A bundle takes one of two forms:

```
Single file (simplest):
  data/templates/<name>.yaml    # the entire bundle is this one file

Directory (for bigger bundles):
  data/templates/<bundle>/
  ├── molgenis.yaml        # REQUIRED bundle entry point
  ├── tables/*.yaml        # one file per table (referenced via tables: - import: tables/)
  ├── ontologies/          # optional ontology tables
  ├── demodata/            # optional seed data CSVs
  ├── settings/            # optional settings YAML files
  └── migrations/          # optional per-subset migration hooks
```

**The simplest bundle** is a single YAML file with inline `tables:` — no directory, no subdirectories, no imports required:

```yaml
# data/templates/pets.yaml
name: Pets
description: Minimal pet-store example
tables:
  - table: Pet
    sections:
      - columns:
          - name: id
            type: string
            key: 1
          - name: name
            type: string
            required: true
          - name: category
            type: ref
            refTable: Category

  - table: Category
    sections:
      - columns:
          - name: name
            type: string
            key: 1
```

That is a complete working bundle in under 20 lines.

When your bundle grows — more tables, auxiliary files (ontologies, demodata, settings), or multiple authors — split it into the directory form. Each `import:` entry under `tables:` names a file or directory to load:

```yaml
# data/templates/petstore/molgenis.yaml
name: Pet store
description: Classic pet store demo
tables:
  - import: tables/
```

Tables are loaded from `tables/`, one YAML file per table. This is the right form when a single file becomes unwieldy or when auxiliary files are needed. Most data models need nothing more than this.

You can also mix inline definitions and imports in the same `tables:` list:

```yaml
tables:
  - table: Pet          # inline definition
    sections: ...
  - import: tables/     # directory reference (all files inside)
  - import: tables/Category.yaml   # single-file reference
```

The effective table set is the union of all entries.

### When to use which form

| Situation | Form |
|---|---|
| A handful of tables, no ontologies/demodata/settings, one author | Single file |
| Many tables, auxiliary files needed, or multiple authors touching the bundle | Directory |

Single-file bundles cannot declare `ontologies:`, `demodata:`, `settings:`, or `migrations:` — those require the directory form.

Layers 2–5 below are **optional** — each solves a specific problem. If you don't have that problem, skip that layer.

---

## Layer 1 — Defining tables

### Defining a table

A table definition has the same shape whether it lives inline under `tables:` in the bundle file or in its own file under `tables/`. The location differs; the structure is identical.

Inline in a single-file bundle:

```yaml
# data/templates/pets.yaml
name: Pets
tables:
  - table: Pet
    sections:
      - columns:
          - name: id
            type: string
            key: 1
          - name: name
            type: string
            required: true
          - name: category
            type: ref
            refTable: Category
```

As a standalone file in a directory bundle:

```yaml
# tables/Pet.yaml
table: Pet
sections:
  - columns:
      - name: id
        type: string
        key: 1
      - name: name
        type: string
        required: true
      - name: category
        type: ref
        refTable: Category
```

This produces:

```sql
CREATE TABLE "Pet store"."Pet" (
  "id"        TEXT NOT NULL,
  "name"      TEXT NOT NULL,
  "category"  TEXT REFERENCES "Pet store"."Category"("id"),
  PRIMARY KEY ("id")
);
CREATE INDEX ON "Pet store"."Pet" ("category");
```

Every column has a `name` and a `type`. `key: 1` marks the primary key (use `key: 2`, `key: 3` for composite keys). `required: true` adds a NOT NULL constraint. `ref` columns become foreign keys; `refTable:` names the target.

### Column types

All supported column types:

| Type | Purpose |
|---|---|
| `bool` | Boolean (single value) |
| `bool_array` | Boolean array |
| `uuid` | UUID identifier (single) |
| `uuid_array` | UUID identifier array |
| `file` | Attached file (stored in files table) |
| `string` | Short text (≤255 chars) |
| `string_array` | Array of short text values |
| `text` | Long text (>255 chars) |
| `text_array` | Array of long text values |
| `json` | JSONB column |
| `int` | 32-bit integer |
| `int_array` | Array of 32-bit integers |
| `long` | 64-bit integer |
| `long_array` | Array of 64-bit integers |
| `decimal` | Floating-point number |
| `decimal_array` | Array of floating-point numbers |
| `date` | Calendar date |
| `date_array` | Array of calendar dates |
| `datetime` | Date and time |
| `datetime_array` | Array of date-time values |
| `period` | ISO 8601 duration (e.g. `P32Y6M1D`) |
| `period_array` | Array of ISO 8601 durations |
| `ref` | Foreign key to another table (single) |
| `ref_array` | Multiple foreign keys to another table |
| `refback` | Computed reverse relationship — no DDL column, derived from the `ref` on the other side |
| `auto_id` | Auto-generated identifier string |
| `ontology` | Foreign key to an ontology table (single term) |
| `ontology_array` | Multiple ontology terms |
| `email` | Validated email address |
| `email_array` | Array of validated email addresses |
| `hyperlink` | Validated URL |
| `hyperlink_array` | Array of validated URLs |
| `non_negative_int` | Integer ≥ 0 |
| `non_negative_int_array` | Array of integers ≥ 0 |
| `select` | UI variant of `ref` — same backend storage, dropdown widget |
| `radio` | UI variant of `ref` — same backend storage, radio-button widget |
| `multiselect` | UI variant of `ref_array` — same backend storage, multi-select dropdown widget |
| `checkbox` | UI variant of `ref_array` — same backend storage, checkbox-group widget |
| `extension` | Subtype discriminator — holds the child-table name this row belongs to (see Layer 2) |
| `extension_array` | Multi-valued subtype discriminator (see Layer 2) |
| `heading` | UI-only section header inside a sections list — no DDL |
| `section` | UI-only section grouping — no DDL |

For full semantics and validation rules see `docs/molgenis/use_tables.md` and `docs/molgenis/CSV.md`.

### Advanced column features (briefly)

The examples in `data/templates/shared/` use a few features beyond this guide's scope:

| Feature | What it does | Where to read more |
|---|---|---|
| `key: 1`, `key: 2`, `key: 3` | Composite primary key with ordered parts | `use_tables.md` |
| `computed: "..."` | JavaScript expression evaluated at read time | `use_tables.md` |
| `visible: "..."` | JavaScript expression controlling conditional visibility per row | `use_tables.md` |
| `refLabel: "..."` | Display-label expression for foreign-key dropdowns | `use_tables.md` |
| `validation: "..."` | JavaScript expression that must evaluate truthy | `use_tables.md` |

A new bundle can ignore all of them.

---

## Layer 2 — Extensions (optional)

**When you need this:** you have a base table and some rows belong to structurally distinct subtypes with different fields. You want a single query surface ("every row is an Experiment") with a discriminator saying which subtype each row is.

### Why extensions?

Imagine you have experiments. Some are whole-genome sequencing (WGS) — they have `coverage`, `read_length`, `library_prep`. Others are imaging — they have `modality`, `resolution`, `stain`. You could make one wide sparse table with many NULLs, or two unrelated tables. Neither is good. Extensions give you a third option: a base `Experiment` table with a discriminator column, and separate child tables (`WGS`, `Imaging`) that share the parent's primary key.

Query the parent and you see all experiments. Join to the child table and you see the subtype-specific fields. The discriminator tells you which child applies.

### Mechanism

Declare child tables that `inherit:` from the parent. Add an `extension` column on the parent (a `extension_array` for multi-valued subtypes). Each child table shares the parent's primary key via a 1:1 FK with ON UPDATE/DELETE CASCADE.

```yaml
# tables/Experiment.yaml
table: Experiment
sections:
  - columns:
      - name: id
        type: string
        key: 1
      - name: date
        type: date
      - name: type
        type: extension

# tables/WGS.yaml
table: WGS
inherits: Experiment
sections:
  - columns:
      - name: coverage
        type: decimal
      - name: read_length
        type: int
      - name: library_prep
        type: string

# tables/Imaging.yaml
table: Imaging
inherits: Experiment
sections:
  - columns:
      - name: modality
        type: string
      - name: resolution
        type: decimal
      - name: stain
        type: string
```

A row in `Experiment` with `type = WGS` links to a row in the `WGS` table with the same primary key. The admin UI and import pipeline handle the join automatically.

### What rows look like

| Experiment.id | Experiment.date | Experiment.type |
|---|---|---|
| EXP001 | 2024-01-10 | WGS |
| EXP002 | 2024-02-15 | Imaging |

| WGS.id | WGS.coverage | WGS.read_length | WGS.library_prep |
|---|---|---|---|
| EXP001 | 30.0 | 150 | PCR-free |

| Imaging.id | Imaging.modality | Imaging.resolution | Imaging.stain |
|---|---|---|---|
| EXP002 | MRI | 1.5 | — |

### Internal extensions

A child table marked `internal: true` is not shown in the extension selector dropdown. Use this when a table groups shared columns across multiple user-selectable extensions rather than being a selectable extension itself.

```yaml
table: SequencingBase
inherits: Experiment
internal: true
sections:
  - columns:
      - name: sequencer_model
        type: string
```

Then `WGS` and `RNASeq` can both `inherit: [Experiment, SequencingBase]` — they gain `sequencer_model` without users seeing `SequencingBase` in the dropdown.

### Multiple inheritance

A child can inherit from multiple parents, provided all parents share the same root table:

```yaml
table: WGS
inherits: [Experiment, SequencingBase]
```

All parents must have the same root (they form a diamond, not a forest). Diamond inheritance is resolved via upsert.

### Relationship to subsets

Extensions define the **structural shape** of your data — which child tables exist, which discriminator columns they use, how they relate. Subsets (Layer 3) gate which tables and columns are physically created per deployment. The two are independent:

- Extensions without subsets: structural modeling, all child tables always created.
- Subsets without extensions: deployment filtering on a flat model.
- Both: structural modeling with per-deployment DDL filtering.

### About the word "extension"

> The `extension` / `extension_array` column type is a **subtype discriminator** — it tells the backend which child table a row belongs to. It has nothing to do with FHIR Extension (an additive resource field), openEHR CLUSTER extension (unmodelled content nodes), or any plugin/add-on concept. A future rename to `subtype` / `subtype_array` is under consideration; for now the YAML key is `extension`.

---

## Layer 3 — Adding subsets for modular data models (optional)

**When you need this:** you have one bundle but multiple deployment variants that share most tables yet differ in which columns should physically exist. You don't want to maintain two parallel bundles. Without subsets, you can't gate individual columns per deployment.

### Why subsets?

Imagine the Pet store bundle needs two deployment variants: a minimal one with just `id`, `name`, and `category`, and an extended one that also tracks `favorite toy`. Without subsets you'd need two separate bundles. With subsets, one bundle covers both: tag the optional column and let each deployment activate only the subset it needs.

### Adding subsets to the simplest bundle

Take the `Pet.yaml` from Layer 1 and add one optional column:

```yaml
# tables/Pet.yaml
table: Pet
sections:
  - columns:
      - name: id
        type: string
        key: 1
      - name: name
        type: string
        required: true
      - name: category
        type: ref
        refTable: Category
      - name: favorite toy
        type: string
        subsets: [extended]
```

And declare the subset in `molgenis.yaml`:

```yaml
name: Demo
tables:
  - import: tables/
subsets:
  - name: extended
templates:
  - name: minimal
  - name: full
    includes: [extended]
```

Activating template `minimal` (no extra subsets) produces:

```sql
CREATE TABLE "Demo"."Pet" (
  "id"            TEXT NOT NULL,
  "name"          TEXT NOT NULL,
  "category"      TEXT REFERENCES "Demo"."Category"("id"),
  PRIMARY KEY ("id")
);
CREATE INDEX ON "Demo"."Pet" ("category");
```

Activating template `full` (which includes `extended`) adds the extra column:

```sql
CREATE TABLE "Demo"."Pet" (
  "id"            TEXT NOT NULL,
  "name"          TEXT NOT NULL,
  "category"      TEXT REFERENCES "Demo"."Category"("id"),
  "favorite toy"  TEXT,
  PRIMARY KEY ("id")
);
CREATE INDEX ON "Demo"."Pet" ("category");
```

These are two different deployments with two different physical schemas.
Switching from `minimal` to `full` on the same schema calls
`ALTER TABLE "Demo"."Pet" ADD COLUMN IF NOT EXISTS "favorite toy" TEXT` — safe and idempotent.
Switching from `full` back to `minimal` would drop "favorite toy" — destructive, requires confirmation.

**Corollary**: the subset a column belongs to is a **per-deployment physical contract**. Clients that depend on subset `catalogue_core` commit to the columns tagged with that subset being present in PostgreSQL. Two different clients activating two different subsets commit to two different schemas — and those schemas can diverge freely, as long as both remain internally consistent.

### Activating and deactivating subsets

#### Activating a subset

Activating a subset (by changing the active template or calling `activateSubset(name)`) re-runs schema apply with the new subset set. New tables are created; new columns are added via `ALTER TABLE ADD COLUMN IF NOT EXISTS`. The operation is idempotent — running it twice has no effect. No confirmation needed.

#### Deactivating a subset

Deactivating a subset (by calling `deactivateSubset(name, confirm: Boolean)`) is destructive. Tables and columns that belong exclusively to the deactivated subset are dropped. The API checks whether any of those columns contain data before proceeding:

- If data exists: the call fails with a message listing the columns and row counts. You must pass `confirm: true` to override the check and proceed with the drops.
- If no data exists: the drop proceeds without a confirmation flag.

The old `activeProfiles`/`applyProfileFilter` API from Phase 6 is superseded by the split `activateSubset` / `deactivateSubset` mutations. That API remains in place during the transition but will be removed once all callers migrate.

#### Ontology tables on deactivate

Ontology tables may be dropped on deactivate, but only if no foreign keys from any schema (including other deployments on the same instance) still reference them.

- On activate: ontology tables are created if missing (same idempotent add).
- On deactivate: walk all FK references to the ontology table across all schemas. If any FK still points to it, the ontology table is **kept silently** — no error, no warning, just a skip. If no FKs reference it, it is dropped subject to the same data-loss confirmation rule as any other table.

This replaces the old blanket "never drop ontology" rule. The new rule respects actual FK topology and still avoids breaking shared usage.

#### Load-time completeness validation

When a bundle is loaded with a given active subset set, the parser validates that the combination is self-consistent:

- **Reference completeness**: if column A in subset X has `refTable: B` and B belongs only to subset Y, then Y must be in the active set. Otherwise the parser rejects with a named error identifying the missing subset.
- **`includes:` completeness**: if subset Z declares `includes: [Y]` and Z is active, Y must be resolvable in the registry. Otherwise the parser rejects.

#### Custom migrations per subset

The bundle directory may contain a `migrations/<subset_name>/` directory. Scripts in that directory run exactly once, when the subset is first activated on a given schema. This is a hook for data transforms, backfills, and index additions specific to a subset. Not every subset needs migrations. The migration framework details are out of scope here — treat this as a reserved directory layout.

### The subset IS the contract

**Subsets drive DDL.** A column tagged `subsets: [x]` is only created in PostgreSQL when subset `x` is in the active set. A column with no `subsets:` tag is always-on — it is created regardless of the active set.

The subset IS the contract, backed materially by DDL. Clients that depend on subset `catalogue_core` commit to the columns tagged with that subset being physically present. Two deployments with different active subsets have genuinely different schemas — data is not automatically portable between them without a migration if the column sets differ. There is no `overrides:` mechanism — the subset tag is the single source of truth.

---

## Layer 4 — Adding templates for named deployments (optional)

**When you need this:** once you have multiple subsets, you want named combinations that admins can pick from a dropdown ("data catalogue" vs "cohort staging") rather than toggling subsets individually. Without templates, the admin would need to know which subsets to activate — templates make the bundle self-describing.

### Adding templates

To make a bundle appear in the admin schema picker, add a `templates:` section:

```yaml
name: Pet store
description: Classic pet store demo

templates:
  - name: pet_store
    description: Standard pet store deployment
```

That entry is now visible to admins when they create a new schema and choose a starting template. The name is the identifier used internally; the description is shown in the picker.

A template is a named bundle of subset activations: "for this deployment, turn on this set of subsets." The admin picks a template when creating a schema; the same bundle can power a slim "data catalogue" deployment and a richer "UMCG cohort staging" deployment from one source of truth — and each gets a different physical schema.

Subsets and templates share **one identifier namespace** across `subsets:` and `templates:` in `molgenis.yaml`. Names must be unique across both sections. A template name can be used directly in `subsets:` on a column, and `includes:` in either section can reference names from either section.

Activating a template activates that template plus every subset or template named in its `includes:` list, transitively. A column tagged with any name in that active set is created.

### Worked example: the catalogue family

The `data/templates/shared/molgenis.yaml` bundle has eight subsets and thirteen user-facing templates. The key structure:

```yaml
name: MOLGENIS shared catalogue bundle
description: Shared tables for all catalogue variants, cohorts, patient registries, and related deployments

tables:
  - import: tables/
  - import: ontologies/tables/

subsets:
  - name: catalogue_core
    description: Core catalogue columns visible in all catalogue-family templates

  - name: cohort_core
    description: Cohort-specific columns shared by CohortsStaging, UMCGCohortsStaging, and UMCUCohorts
    includes: [catalogue_core]

  - name: patient_core
    description: Shared patient registry and FAIR Genomes columns

templates:
  - name: data_catalogue
    description: Standard MOLGENIS data catalogue (flat resource view)
    includes: [catalogue_core]

  - name: cohorts_staging
    description: Cohort registry staging workspace
    includes: [cohort_core]

  - name: umcg_cohorts_staging
    description: UMCG cohorts staging workspace
    includes: [cohort_core]        # transitively activates catalogue_core too

  - name: patient_registry
    description: Patient registry (includes catalogue resource view)
    includes: [patient_core, catalogue_core]

  - name: fair_genomes
    description: FAIR Genomes patient registry
    includes: [patient_core]
```

Now look at `tables/Datasets.yaml`. The table itself and most of its columns are tagged `subsets: [catalogue_core]`:

```yaml
table: Datasets
subsets: [catalogue_core]
sections:
- columns:
  - name: name
    key: 1
    required: true
    subsets: [catalogue_core]
    description: Unique dataset name in the model
  - name: mapped to
    type: refback
    refTable: Dataset mappings
    refBack: source dataset
    subsets: [cohorts_staging, rwe_staging, study_staging]
    description: Common dataset models this dataset has been mapped into
```

Reading this: `name` is physically created in any deployment that activates `catalogue_core` — that is `data_catalogue`, `cohorts_staging`, `umcg_cohorts_staging`, `patient_registry`, and several others. The `mapped to` column is narrower: only the three staging templates create it. The table-level `subsets: [catalogue_core]` means the entire table is absent from deployments that do not activate `catalogue_core`.

Now look at `tables/Individuals.yaml`. That table is tagged `subsets: [patient_core]` and most columns also carry `subsets: [patient_core]`. A few narrower columns are tagged `subsets: [patient_registry]`:

```yaml
table: Individuals
subsets: [patient_core]
sections:
- columns:
  - name: year of birth
    type: int
    subsets: [patient_core]       # created in patient_registry AND fair_genomes
  - name: alternate ids
    type: string_array
    subsets: [patient_registry]   # created only in patient_registry, not fair_genomes
```

Both `patient_registry` and `fair_genomes` include `patient_core`, so both get `year of birth`. Only `patient_registry` includes itself, so `alternate ids` is absent in a `fair_genomes` deployment.

---

## Authoring columns: decision guide

| Situation | What to write |
|---|---|
| Column appears in every template | Omit `subsets:` entirely |
| Column belongs to one subset | `subsets: [x]` |
| Column belongs to several related subsets | `subsets: [x, y]` — or extract a shared subset if the combination recurs across many columns |

A table-level `subsets:` line gates the entire table. If every template should create the table, omit the table-level `subsets:`.

---

## Layer 5 — Adding semantic annotations (optional)

**When you need this:** you want RDF export, SPARQL federation, or cross-bundle data harmonisation. For single-system use you may not need this at all. Semantics become important when multiple deployments need to compare data, or when you export to RDF for FAIR federation.

### Adding semantics

Tag tables and columns with `semantics:` CURIEs to give them a machine-readable meaning:

```yaml
table: Datasets
semantics: [dcat:Dataset]
sections:
  - columns:
      - name: title
        type: string
        semantics: [dcterms:title]
      - name: publisher
        type: ref
        refTable: Organisations
        semantics: [dcterms:publisher, foaf:Organization]
```

- **CURIEs** (`dcterms:title`) are expanded via a built-in prefix map. The built-in map covers common biomedical and metadata vocabularies — see [semantics.md](semantics.md) for the full list.
- **Multiple URIs** per element mean "this element is semantically equivalent to ALL of these" (union semantics). Use this when the element maps onto several vocabularies (DCAT + schema.org, for example).
- `semantics:` is orthogonal to `subsets:`. Semantic URIs travel with the column definition regardless of which subsets are active.
- The RDF exporter uses `semantics:` to emit typed triples. See [dev_rdf.md](dev_rdf.md) and [semantics.md](semantics.md) for the full mapping rules.

**Common pitfall**: semantic URIs do NOT belong in `subsets:` arrays. Keep them in `semantics:`. Subset tags are internal identifiers (`[a-z][a-z0-9_]*`); semantic URIs are external vocabulary references.

### Declaring custom namespaces

Bundle authors can declare their own prefixes at the root level of `molgenis.yaml`:

```yaml
name: My consortium
description: ...
namespaces:
  myvocab: https://example.org/vocab/
  custom: https://example.com/custom#
tables:
  - import: tables/
```

Then columns and tables can use the short prefix in `semantics:`:

```yaml
semantics: [myvocab:patientId, custom:collectionSite]
```

Short-form CURIEs are expanded using (a) the built-in prefix map (DCAT, DCTERMS, FOAF, SNOMED, NCIT, HPO, etc. — see `semantics.md` for the full list), then (b) any `namespaces:` declared in `molgenis.yaml`. Bundle-declared prefixes override built-in ones on conflict.

### Semantics as harmonisation target

Across deployments, `semantics:` URIs are the **stable identifier** for a column or table. Column names can be renamed, tables reorganised, subsets restructured — clients that query by semantic URI (via the RDF export and SPARQL) keep working without coordination with the server.

Use `semantics:` when you want two different bundles — or two different deployments of the same bundle — to be **comparable**: tag the equivalent columns with the same URI from DCAT, DCTERMS, NCIT, SNOMED, HPO, schema.org, or any vocabulary your community already agrees on. The RDF exporter emits triples that external tools can federate without caring about the underlying table names.

Two deployments that activate the same subset get the same physical columns, and those columns carry the same `semantics:` URIs. The semantic URIs are therefore stable across deployments that share a subset — they do not break under physical subsetting.

**When to use which identifier:**

| Use case | Query by |
|---|---|
| Single deployment, single client app | GraphQL field name (fast, ergonomic) |
| Multiple deployments, federating clients | Semantic URI via RDF / SPARQL (stable across renames) |
| Cross-bundle data comparison / harmonisation | Semantic URI |
| Internal admin tooling, one schema at a time | GraphQL field name |

---

## `molgenis.yaml` key reference

| Key | Type | Required | When to use |
|---|---|---|---|
| `name` | string | yes | Bundle display name |
| `description` | string | no | What this bundle deploys; shown in the admin UI |
| `namespaces` | mapping | no | Custom prefix-to-URI mappings for CURIE expansion in `semantics:` annotations. Bundle-declared prefixes override built-in ones on conflict. |
| `tables` | list | no* | Inline table definitions (`- table: X`) and/or file/directory imports (`- import: path/`). The effective table set is the union of all entries. *At least one of `tables:` or a populated import is required. |
| `subsets` | list | no | Internal reusable DDL groupings — not shown in admin picker. Share one identifier namespace with `templates:`. |
| `templates` | list | no | User-facing deployment presets — shown in admin picker. Share one identifier namespace with `subsets:`. |
| `ontologies` | path | no | Directory of ontology table files loaded as read-only lookup tables |
| `demodata` | path | no | Directory of seed data CSVs loaded on first deploy |
| `settings` | path | no | YAML file of schema settings (e.g. menu, theme) |
| `permissions` | mapping | no | Default role-to-permission assignments applied on schema creation |
| `fixedSchemas` | list | no | Additional schemas created with fixed names alongside the main schema |

---

## Subset and template entry properties

Both `subsets:` entries and `templates:` entries support the same properties:

| Key | Type | Description |
|---|---|---|
| `name` | string | Identifier. Must be `[a-z][a-z0-9_]*`. Must be unique across both `subsets:` and `templates:`. Referenced by `includes:` and by `subsets:` on columns. |
| `description` | string | Human-readable description shown in admin UI (for templates) or used as documentation |
| `includes` | string list | Subsets or templates to activate transitively when this entry is activated. Can reference names from either list. |
| `settings` | path | Template-specific settings file, merged with bundle-level settings |
| `permissions` | mapping | Template-specific default permissions, merged with bundle-level permissions |
| `fixedSchemas` | list | Additional fixed-name schemas created when this template is activated |

---

## Common patterns

**Shared core with variants** — `catalogue_core` is a subset included by many templates. Columns tagged `subsets: [catalogue_core]` write their DDL gate once and automatically apply to all those templates.

**Tight pair** — `patient_core` is included by both `patient_registry` and `fair_genomes`. Columns shared by both carry `subsets: [patient_core]`; columns specific to the full registry carry `subsets: [patient_registry]`.

**Singleton template** — `fair_data_point` has its own tables tagged `subsets: [fair_data_point]`. When a template has no shared columns with other templates, no separate subset is needed — the template name itself can be used in `subsets:` on columns.

**Zero-subsets bundle** — either a single-file bundle with inline `tables:` or a directory bundle with `name:` and `tables: [- import: tables/]`. No subsets, no templates, all columns always created. Suitable for any bundle where every deployment looks the same.

---

## Gotchas

- Subset and template names share one namespace. A name used in `subsets:` on a column or table must exist in either `subsets:` or `templates:` in `molgenis.yaml`. An unknown name is a hard error at load time.
- Names must be unique across both sections. Defining the same name in both `subsets:` and `templates:` is a hard error.
- `includes:` must be acyclic. A cycle (e.g. `a` includes `b` includes `a`) is a hard error.
- Semantic URIs (`http://...`) belong in `semantics:` on the column, not in `subsets:`. Mixing them into `subsets:` was a bug in the old format.
- Template names appear verbatim in the admin UI picker. Choose names that are readable to the people creating schemas (`UMCG cohorts staging workspace`, not `umcg_cohorts_staging` — note: the `description` field is what the UI shows; the `name` is the internal identifier).
- Subset and template `name` values are `snake_case` only: lowercase letters, digits, underscores. No spaces, no camelCase, no hyphens.
- Table-level `subsets:` gates the whole table. If you only want to gate individual columns, put `subsets:` on the columns, not on the table.
- `includes:` is **additive union**, not specialization. Including a subset activates ITS columns too — readers familiar with OOP inheritance or openEHR archetype specialization often expect the opposite (narrowing). It doesn't narrow, it adds.
- Deactivating a subset drops tables and columns in PostgreSQL. This is not reversible without re-activating the subset (and restoring any lost data separately). Never deactivate a subset on a production schema without a backup.
- The schema editor and bundle YAML are mutually exclusive authoring paths — see the next gotcha.
- `extension` / `extension_array` is a subtype discriminator column type — it tells the backend which child table a row belongs to. It is unrelated to FHIR Extension, openEHR CLUSTER extension, or plugins. A future rename to `subtype` / `subtype_array` is under consideration; for now the YAML key is `extension`.

**Schema editor / bundle YAML tension**: the browser-based schema editor edits a PostgreSQL schema directly. If you use it on a schema backed by a `molgenis.yaml` bundle, your edits diverge from the YAML source of truth — the next activate/deactivate operation will not know about them. Either treat the editor as authoring-only (new bundles, before a bundle YAML exists), or as a live-edit tool on schemas without a bundle. Don't mix. On a schema whose source is a bundle YAML, subset activate/deactivate goes through the `activateSubset`/`deactivateSubset` mutations; the schema editor is read-only for structure.

---

## See also

- [semantics.md](semantics.md) — semantic annotation, prefix map, RDF export rules
- [dev_rdf.md](dev_rdf.md) — content-negotiated RDF serializations, SHACL validation
- [dev_beaconv2.md](dev_beaconv2.md) — Beacon v2 genomics federation
- [use_tables.md](use_tables.md) — full column-type reference, advanced features
- [use_schema.md](use_schema.md) — creating schemas from templates via the admin UI

---

## FAQ

**Why are subsets and templates under two separate top-level keys?**
Both are named DDL groupings — they share one identifier namespace so `includes:` and column `subsets:` tags work uniformly across both. The split into `subsets:` (internal, not shown in picker) and `templates:` (user-facing, shown in picker) makes the distinction clear at a glance without requiring a per-entry flag.

**Can a template include another template?**
Yes. `includes:` does not care whether the listed name is in `subsets:` or `templates:`. Templates can include templates.

**Can a bundle have zero subsets?**
Yes. If the `subsets:` and `templates:` sections are omitted entirely, every column is always created and there is one implicit template (the bundle name). This is the recommended starting point — and the single-file bundle form naturally starts here.

**Does a bundle require a directory?**
No. A bundle can be a single YAML file (`data/templates/<name>.yaml`) with a top-level `name:` and inline `tables:`. The directory form is only needed when the bundle requires ontologies, demodata, settings, migrations, or simply has too many tables to read comfortably in one file.

**Can I have a subset that no template includes?**
Technically yes, but it is pointless — columns tagged with it would never be created. Treat it as a dead-code warning and either add an `includes:` in a template or remove the subset.

**Is data portable between two deployments of the same bundle with different active templates?**
Only if the column sets overlap. Columns present in both deployments can be migrated directly. Columns that exist in one deployment but not the other require either activating the missing subset first, or dropping those columns from the export.
