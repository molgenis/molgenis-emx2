# YAML bundle format

A **bundle** is a YAML package that defines a MOLGENIS data model: tables, columns, profiles, variants, and semantic annotations. Bundles can be a single file or a directory.

**Profiles define subsets of the core data model. Multiple profiles can be combined to support specific research use cases.**

---

## Bundle formats

### Single-file bundle

The simplest form: one YAML file with inline `tables:`. From `profiles/petstore.yaml`:

```yaml
name: Pet store
description: Classic MOLGENIS pet store demo

tables:
  Category:
    columns:
      name:
        key: 1
        required: 'true'

  Pet:
    columns:
      name:
        key: 1
        required: 'true'
      category:
        type: radio
        refTable: Category
      photoUrls:
        type: string_array
```

That is a complete working bundle. Use this form for a handful of tables with no ontologies, demo data, or multiple authors.

### Directory bundle

When a bundle grows — many tables, shared ontologies, multiple authors — split into a directory. The entry point is `molgenis.yaml`; tables live in `tables/*.yaml` and are loaded via `imports:`. From `profiles/shared/molgenis.yaml`:

```yaml
name: MOLGENIS shared catalogue bundle
description: Shared tables for all catalogue variants, cohorts, patient registries, and related deployments
imports:
  - tables/
  - ontologies/tables/
profiles:
  catalogue_core:
    description: Core catalogue columns visible in all catalogue-family templates
    internal: true
  cohorts_staging:
    description: Cohort registry staging workspace
    includes: [cohort_core]
```

Directory layout:

```
<bundle>/
├── molgenis.yaml        # required entry point
├── tables/*.yaml        # one file per table
├── ontologies/          # optional ontology tables
├── demodata/            # optional seed data CSVs
└── settings/            # optional settings YAML
```

| Situation | Form |
|---|---|
| Few tables, one author, no auxiliaries | Single file |
| Many tables, ontologies, demodata, or multiple authors | Directory |

---

## Table definitions

A table definition has the same shape whether it is inline under `tables:` in the bundle file or in its own file under `tables/`.

```yaml
# tables/Pet.yaml
table: Pet
description: My pet store example table
columns:
  name:
    key: 1
    required: 'true'
  category:
    type: radio
    refTable: Category
  photoUrls:
    type: string_array
```

Every column is an entry in the `columns:` keyed map. The key is the column name.

### Column attributes

| Attribute | Description |
|---|---|
| `type` | Column type (see list below). Default: `string` |
| `key` | Primary key part: `1`, `2`, `3` for composite keys |
| `required` | `'true'`, `'false'`, or a JavaScript expression returning a message |
| `refTable` | Target table for ref/ontology types |
| `refBack` | Column on the other table for `refback` type |
| `refLabel` | Display expression for dropdowns, e.g. `${label}` |
| `defaultValue` | Value pre-filled in new rows |
| `description` | Human-readable description |
| `semantics` | List of URIs/CURIEs for RDF export |
| `computed` | JavaScript expression evaluated at read time |
| `visible` | JavaScript expression controlling conditional visibility |
| `validation` | JavaScript expression that must evaluate truthy |
| `profiles` | List of profile names that include this column |
| `variant` | Scopes this section to a named variant (see Variants) |

**Column name rules:** names must be unique table-wide across the root columns map and all sections and headings. The name `columns` is reserved.

### Column types

| Type | Purpose |
|---|---|
| `string` | Short text (≤255 chars). Default type. |
| `string_array` | Array of short text values |
| `text` | Long text (>255 chars) |
| `text_array` | Array of long text values |
| `json` | JSONB column |
| `auto_id` | Auto-generated identifier string |
| `email` | Validated email address |
| `email_array` | Array of validated email addresses |
| `hyperlink` | Validated URL |
| `hyperlink_array` | Array of validated URLs |
| `int` | 32-bit integer |
| `int_array` | Array of 32-bit integers |
| `long` | 64-bit integer |
| `long_array` | Array of 64-bit integers |
| `decimal` | Floating-point number |
| `decimal_array` | Array of floating-point numbers |
| `non_negative_int` | Integer ≥ 0 |
| `non_negative_int_array` | Array of integers ≥ 0 |
| `bool` | Boolean |
| `bool_array` | Boolean array |
| `uuid` | UUID identifier |
| `uuid_array` | UUID identifier array |
| `date` | Calendar date |
| `date_array` | Array of calendar dates |
| `datetime` | Date and time |
| `datetime_array` | Array of date-time values |
| `period` | ISO 8601 duration (e.g. `P32Y6M1D`) |
| `period_array` | Array of ISO 8601 durations |
| `file` | Attached file |
| `ref` | Foreign key to another table (single) |
| `ref_array` | Multiple foreign keys to another table |
| `refback` | Computed reverse relationship — no DDL column |
| `select` | UI variant of `ref` — dropdown widget |
| `radio` | UI variant of `ref` — radio-button widget |
| `multiselect` | UI variant of `ref_array` — multi-select dropdown |
| `checkbox` | UI variant of `ref_array` — checkbox-group widget |
| `ontology` | Foreign key to an ontology table (single term) |
| `ontology_array` | Multiple ontology terms |
| `variant` | Discriminator — holds which variant(s) a row belongs to |
| `variant_array` | Multi-valued variant discriminator |
| `heading` | Decorative UI-only label row — no DDL column |
| `section` | UI section placeholder — no DDL column |

---

## Sections and headings

Columns can be organized into **sections** and **headings** for display in the form UI. Sections and headings do not affect the database schema.

### Sections

A `sections:` keyed map at table level groups columns under named headings. Each section has optional `description:` and a `columns:` map. From `profiles/petstore.yaml`:

```yaml
table: Pet
columns:
  name:
    key: 1
    required: 'true'
sections:
  details:
    description: Details
    columns:
      status: {}
      weight:
        type: decimal
        required: 'true'
```

### Headings

A section can contain `headings:` for a second level of grouping. Each heading has a `columns:` map. Max depth is section → heading → columns:

```yaml
sections:
  details:
    columns:
      status: {}
    headings:
      Heading2:
        columns:
          orders:
            type: refback
            refTable: Order
            refBack: pet
```

A section can have both direct `columns:` and `headings:`. Columns listed directly under the section appear before any headings.

**Maximum nesting depth is 2:** table → section → heading → columns. A heading cannot contain further sub-groupings.

---

## Variants

A **variant** is a composable, table-level configuration that adds columns for a specific use case. Multiple variants can be active simultaneously on the same row.

### Declaring variants

Variants are declared in a `variants:` keyed map at the top of a table file. Each entry supports:

| Attribute | Description |
|-----------|-------------|
| `description:` | Human-readable description |
| `extends:` | List of parent variants this variant extends (IS-A relationship, inherits all columns) |
| `internal:` | If `true`, the variant is not shown in the user-facing variant selector but can be used as an `extends:` target |

```yaml
table: Experiments
description: "Research experiments with composable protocol modules"

variants:
  sampling:
    description: "Sample collection protocol"
    internal: true
  sequencing:
    description: "Sequencing protocol"
    internal: true
  WGS:
    description: "Whole genome sequencing"
    extends: [sampling, sequencing]
  Imaging:
    description: "Medical imaging"
```

In this example, `sampling` and `sequencing` are internal variants — they group shared columns but users don't select them directly. `WGS` extends both, so enabling WGS automatically includes all columns from `sampling` and `sequencing`.

Simple variants without `extends:` or `internal:` flag:

```yaml
variants:
  Dermatology:
    description: "Skin examination findings"
  Neurology:
    description: "Neurological assessment"
  Questionnaire:
    description: "Validated instruments and scores"
```

### Discriminator columns

Add a discriminator column to the base table using `type: variant` (single selection) or `type: variant_array` (multi-select):

```yaml
columns:
  observation id:
    type: string
    key: 1
  date:
    type: date
  observation types:
    type: variant_array
    description: "Select one or more clinical domains"
```

### Scoping columns to a variant

Columns belonging to a variant are placed in a named entry under `columns:` with a `variant:` attribute and a nested `columns:` map:

```yaml
columns:
  observation id:
    type: string
    key: 1
  date:
    type: date
  observation types:
    type: variant_array
  Dermatology:
    variant: Dermatology
    columns:
      BSA percentage:
        type: decimal
        description: "Body surface area affected (%)"
      lesion type: {}
  Neurology:
    variant: Neurology
    columns:
      motor score:
        type: int
      cognitive score:
        type: int
  Questionnaire:
    variant: Questionnaire
    columns:
      instrument name: {}
      total score:
        type: decimal
```

All variant columns are created in the same physical table. The `variant:` attribute on a section scopes those columns to that named variant for display and filtering.

Variants from `profiles/pages/tables/Blocks.yaml` showing a section scoped to a variant:

```yaml
table: Blocks
variants:
  Headers:
    description: <header> elements

sections:
  Headers:
    variant: Headers
    columns:
      title:
        description: A title for the page
      background image:
        type: select
        refTable: Images
```

### Diamond inheritance

Variants can form a diamond when two variants extend the same parent:

```
Experiments (root table)
├── sampling (internal, columns: sample_type, tissue_type)
├── sequencing (internal, columns: library_strategy, read_length)
├── WGS (extends: [sampling, sequencing], columns: coverage)
└── Imaging (columns: modality, body_part)
```

When a user selects `WGS`, the row gets columns from `WGS` + `sampling` + `sequencing` — all merged into the same physical table row. If another variant also extends `sampling`, those shared columns are not duplicated.

Key rules:
- Variant columns are **additive** — enabling two variants adds all columns from both
- **Internal variants** (`internal: true`) group shared columns but don't appear in the user-facing selector
- A variant with `extends:` transitively inherits all columns from its parents
- All variants in a table must share the same root table — the `extends:` chain cannot cross table boundaries

---

## Profiles

A **profile** is a tagged subset of the core data model. Profiles are declared in `profiles:` at the bundle root. `internal: true` marks profiles that are implementation details, not shown in the user picker.

### Declaring profiles

From `profiles/shared/molgenis.yaml`:

```yaml
profiles:
  catalogue_core:
    description: Core catalogue columns visible in all catalogue-family templates
    internal: true
  cohort_core:
    description: Cohort-specific columns shared by CohortsStaging, UMCGCohortsStaging, and UMCUCohorts
    includes: [catalogue_core]
    internal: true
  cohorts_staging:
    description: Cohort registry staging workspace
    includes: [cohort_core]
  patient_registry:
    description: Patient registry (includes catalogue resource view)
    includes: [patient_core, catalogue_core]
  fair_genomes:
    description: FAIR Genomes patient registry
    includes: [patient_core]
```

`includes:` is transitive: activating `cohorts_staging` automatically activates `cohort_core` and then `catalogue_core`.

### Profile entry properties

| Key | Type | Description |
|---|---|---|
| `description` | string | Human-readable description |
| `includes` | string list | Profiles to activate transitively |
| `internal` | bool | If `true`, not shown in user picker |
| `settings` | list | YAML settings file paths applied when this profile is active |

### Tagging tables and columns

Tag tables or columns with `profiles:` to make them conditional on those profiles:

```yaml
table: Individuals
profiles: [patient_core]
columns:
  year of birth:
    type: int
    profiles: [patient_core]
  alternate ids:
    type: string_array
    profiles: [patient_registry]
```

A table or column with no `profiles:` tag is **always-on** — it is created in every deployment regardless of which profiles are active.

Reading the example: `year of birth` is created in any deployment with `patient_core` active (that includes both `patient_registry` and `fair_genomes`). `alternate ids` is created only when `patient_registry` is active.

---

## Enabling and disabling profiles

### Enabling a profile

Call the `enableProfile(name)` GraphQL mutation. This re-runs schema apply with the updated active profile set. New tables are created; new columns are added via `ALTER TABLE ADD COLUMN IF NOT EXISTS`. The operation is additive and idempotent — running it twice has no effect. No data is lost.

### Disabling a profile

Call the `disableProfile(name)` GraphQL mutation. This is DDL-free: disabling a profile does **not** drop tables or columns. Data is preserved. The profile is simply removed from the active set, and profile-filtered views are updated. Columns tagged only to the disabled profile are no longer returned by default queries but remain in the database.

This means enabling and disabling profiles is safe in production. Re-enabling a profile restores full access to the previously hidden columns and their data.

---

## Semantics

Tag tables and columns with `semantics:` URIs or CURIEs to give them machine-readable meaning for RDF export and cross-system harmonisation:

```yaml
table: Pet
semantics: ['foaf:Person']
columns:
  username:
    key: 1
    semantics: ['foaf:accountName']
  email:
    type: email
    semantics: ['foaf:mbox']
  tags:
    type: ontology_array
    refTable: Tag
    semantics: ['http://example.com/petstore#hasTags']
```

- CURIEs (`foaf:accountName`) are expanded via a built-in prefix map covering common biomedical and metadata vocabularies — see [semantics.md](semantics.md).
- Multiple URIs per element mean "semantically equivalent to all of these."
- `semantics:` on sections or headings is a parse error — set it per column only.
- `semantics:` is independent of `profiles:` — URIs travel with the column regardless of which profiles are active.

### Custom namespaces

Declare custom prefixes at the bundle root:

```yaml
name: My consortium
namespaces:
  myvocab: https://example.org/vocab/
imports:
  - tables/
```

Then use the short prefix in `semantics:`:

```yaml
semantics: [myvocab:patientId]
```

Bundle-declared prefixes override built-in ones on conflict.

---

## Migration from CSV format

The CSV format uses separate spreadsheet tabs or files (`molgenis.csv`, `columns.csv`, etc.). The YAML format replaces these with a single hierarchical file per table.

| CSV concept | YAML equivalent |
|---|---|
| `molgenis.csv` row (table) | Top-level entry under `tables:` or a `tables/*.yaml` file |
| `columns.csv` row | Entry under `columns:` in the table definition |
| `tableName` column | `table:` key at file top (directory) or map key (inline) |
| `columnName` column | Key in `columns:` map |
| `columnType` column | `type:` attribute |
| `required` column | `required:` attribute |
| `refTable` column | `refTable:` attribute |
| `key` column | `key:` attribute (integer) |
| `profiles` column | `profiles:` attribute (list) |
| `semantics` column | `semantics:` attribute (list) |
| `description` column | `description:` attribute |
| Tab/section grouping | `sections:` and `headings:` keys |
| Subtype discriminator | `type: variant` / `type: variant_array` |
| Subtype column scoping | `variant:` attribute on a section entry |

Key differences:

- **Structure is hierarchical**, not flat rows. Column grouping (sections, headings) is expressed via nesting, not separate columns.
- **Variants replace subtypes.** The old `subtype`/`subtype_array` discriminator types become `variant`/`variant_array`. The old `subtype:` scoping attribute on sections becomes `variant:`.
- **Profiles replace templates/subsets.** The old `subsets:` / `templates:` split is unified into `profiles:`. The `internal: true` flag replaces the old `subsets:` vs `templates:` distinction.
- **Single file per table** in directory bundles keeps large models readable.

---

## `molgenis.yaml` key reference

| Key | Type | Description |
|---|---|---|
| `name` | string | Bundle display name |
| `description` | string | Shown in admin UI |
| `imports` | list | File or directory paths to load (e.g. `- tables/`) |
| `tables` | mapping | Inline table definitions keyed by table name |
| `profiles` | mapping | Profile declarations (see Profiles section) |
| `namespaces` | mapping | Custom prefix-to-URI mappings for CURIE expansion |
| `ontologies` | list | Ontology table file paths (e.g. `[_demodata/applications/petstore]`) |
| `demodata` | list | Seed data CSV paths (e.g. `[_demodata/applications/petstore]`) |
| `settings` | list | YAML settings file paths (e.g. `[_demodata/applications/petstore]`) |
| `permissions` | mapping | Role-based access: `view: anonymous`, `edit: user` |
| `additionalSchemas` | mapping | Additional schemas to create (e.g., shared ontology schemas). Each entry has `model`, `permissions`, `demodata`, etc. |

---

## See also

- [semantics.md](semantics.md) — semantic annotation, prefix map, RDF export rules
- [use_tables.md](use_tables.md) — full column-type reference, advanced features
- [use_schema.md](use_schema.md) — creating schemas from templates via the admin UI
- [dev_rdf.md](dev_rdf.md) — RDF serializations, SHACL validation
