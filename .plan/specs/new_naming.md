# Modular Data Modeling with Extensions, Profiles, and Templates

## Overview

This document describes a modular data modeling approach built on top of a relational backend (EMX2), combining:

* Strongly typed schema design
* Compositional extensions (subtables with shared primary key)
* Profile-driven constraints and UI behavior
* Deploy-time templates as compatibility contracts

The goal is to balance **flexibility**, **stability**, and **interoperability**, while avoiding overly sparse schemas or runtime schema mutation.

This document also defines clearer, more intuitive naming conventions, informed by how FHIR, openEHR, OMOP, and CDISC handle similar concepts.

---

## Core Principles

> Tables define everything that *can* exist.
> 
> Extensions add modular capabilities to tables — data modelers provide users with extension choice via columnType=EXTENSION(_ARRAY).
> 
> Profiles define what is *allowed* in a given context — controlling which extensions and columns are visible to users.
> 
> Templates define what is *deployed and active* — selecting tables, extensions, and profiles for a specific use case.

This separation ensures:

* Stable APIs
* Modular growth
* Controlled variability for specific use cases

---

## Naming Conventions

| Concept | Name | Backend | Notes |
|---|---|---|---|
| Base entity | **Table** | `TableType.DATA` | Foundational entity (e.g., Sample, Experiment) |
| Modular capability | **Extension** | `TableType.DATA`, has `extendsTables` | Subtable with shared PK (1:1). User-selectable. |
| Non-selectable extension | **Extension** with `internal: true` | `TableType.INTERNAL` | Groups shared columns across extensions. Not shown in selector. |
| Selector (pick-one) | `columnType: EXTENSION` | Extends STRING | Column on root table. Dropdown of available extensions filtered by profile. Root table is one of the options unless marked internal or hidden by profile. |
| Selector (pick-many) | `columnType: EXTENSION_ARRAY` | Extends STRING_ARRAY | Multi-select of extensions filtered by profile. Root table is one of the options unless marked internal or hidden by profile. |
| Constraint/UX layer | **Profile** | (metadata) | Controls which extensions/columns are visible to users. |
| UI grouping | **Section** | (metadata) | Groups columns for forms and presentation. |
| Deployment bundle | **Template** | (YAML file) | Selects tables, sets active profiles, configures instance. |
| CSV/metadata property | **`inherits`** | `inheritNames` (Java) | Replaces `inheritTable`. Points to parent table(s). |

### Profile identifiers

Profile names are identifiers: `[a-zA-Z][a-zA-Z0-9_]*`. Convention is camelCase (e.g., `wgs`, `genomicsBase`, `cranio_clinical`). No spaces allowed in profile names.

### Comparison with standards

| Our concept | FHIR equivalent | openEHR equivalent |
|---|---|---|
| Table | Resource | Reference Model class |
| Extension | Extension | Archetype |
| Profile | Profile (StructureDefinition) | Template (constrains archetypes) |
| Section | Section (in Questionnaire) | Cluster |
| Template | Implementation Guide | Operational Template (OPT) |
| `EXTENSION` column | `meta.profile` + `code` | `archetype_node_id` |

### Key insight from standards

Both FHIR and openEHR keep a **stable API regardless of profiles/templates**:
- FHIR: API always exposes full resource structure. Profiles restrict validity, not shape.
- openEHR: API is Reference Model-stable. Templates govern validation, not API shape.

Our approach follows this: expose full schema (all extensions), profiles only affect visibility and validation. The API does not change based on which profiles or templates are active.

### User-facing terminology research

When end users select "what kind of thing is this", standards use:

| System | Term |
|---|---|
| FHIR | **Category** (broad) + **Code** (specific) |
| openEHR | **Template** / **Form** (pre-selected, not per-row) |
| OMOP | **Type** (`_type_concept_id`) |
| REDCap | Custom label, usually **"Type"** |
| CDISC | **Category** (`--CAT`) |

In EMX2, the data modeler names the column itself (e.g., "experiment type", "sample kind") — that user-friendly label is independent of the column type name. The column type `EXTENSION` / `EXTENSION_ARRAY` is the technical term data modelers see.

---

## Architectural Layers

### 1. Tables

Tables are the foundational entities in the data model.

Examples: `Sample`, `Individual`, `Experiment`

These tables:
* Define identity and primary structure
* Serve as anchors for extensions
* Are created based on the active template

### 2. Extensions (Composable Capabilities)

Extensions are implemented as subtables using `extendsTables` (in Java) and `inherits` (in YAML). They share the parent table's primary key (1:1 relationship).

Examples that extend the `Sample` table:
* `WGS sample` — whole genome sequencing fields
* `RNA sample` — transcriptome fields

Characteristics:
* 1:1 relationship with root table (shared primary key)
* Represent optional, domain-specific capabilities
* Strongly typed and normalized
* Can extend other extensions (multiple inheritance, diamond resolved via upsert)
* All extensions in a tree share the same root table

**Internal extensions** (`internal: true`) group columns shared across multiple user-selectable extensions. They are not shown in the extension selector dropdown. Backend type: `TableType.INTERNAL`.

> An Extension adds capability to a table without modifying its core structure.

### 3. Profiles (Constraint + UX Layer)

Profiles define how tables and columns are used in a specific context.

They:
* Control which extensions are visible to users (hide/show in selector)
* Control column and section visibility
* Can require or hide columns/sections

Profiles do **not**:
* Change the database schema
* Affect API structure (only result in null values in queries, stricter validation in mutations)

Profiles can be:
* Set at deploy time (in a Template)
* Changed at runtime by administrators

**Default behavior**: everything is visible unless scoped by `profiles:`. When an element has `profiles: [x]`, it is only visible when profile `x` is active. When an element has `profiles: [-x]`, it is visible in all profiles EXCEPT `x`.

### 4. Sections (UI Grouping)

Sections group columns for presentation and usability. They can also ease profile/extension definition by enabling/disabling a section as a whole.

Characteristics:
* Purely organizational (no structural impact on database)
* Used by forms and UI rendering
* Can be scoped to an extension via `extension: X` — all columns in that section then belong to extension X

### 5. Templates (Deployment Bundles)

Templates define deployable instances of the full model. A template can configure one or more PostgreSQL schemas to be populated, optionally filtered by profile.

They include:
* Which tables to import
* Which profiles are active
* Settings (landing page, menu, theme)
* Permissions (role-based access defaults)
* Optional: additional fixed schemas (e.g., shared ontologies)

They act as:
> Compatibility contracts for APIs, exports (CSV/JSON), and integrations

---

## API Strategy

The API:
* Exposes the **full schema**, including all extensions
* Does not change based on profiles or templates
* All extensions are always deployed technically (tables exist in PostgreSQL)

Profiles:
* Restrict what is visible and valid at the user-facing layer
* Do not alter API structure

This ensures:
* Stable integrations
* Predictable queries
* Forward compatibility

---

## Extension Lifecycle

* All extensions are always present in the database (tables are created for all)
* Profiles control which extensions are **visible** to end users
* The extension selector dropdown only shows non-internal extensions allowed by the active profile
* Data in hidden extensions is preserved (not deleted when profile changes)

---

## Validation Strategy

Validation occurs at backend level:
* Profiles define constraints (which extensions/columns are required/allowed)
* API enforces validity based on active profile
* UI prevents invalid input

---

## YAML Format

All sequences use `- name:` syntax for consistency and to support `- import:` at any level.

### Table files — one per table

Each table is a YAML file with these top-level properties:

| Property | Type | Required | Description |
|---|---|---|---|
| `table` | string | yes | Table name (sentence case, uppercase start) |
| `description` | string | no | Human-readable description |
| `profiles` | string[] | no | Profiles this table is active in. `-` prefix excludes (e.g., `[-core]`). |
| `extensions` | sequence | no | Extension definitions for this table |
| `sections` | sequence | yes | Ordered list of sections containing columns |

### Extension definitions

Extensions are declared inline in the parent table file under `extensions:`. Each extension implicitly extends the main table unless `inherits:` specifies other extension(s).

```yaml
extensions:
  - name: WGS
    description: "Whole genome sequencing"
    inherits: [sampling, sequencing, quality]
    profiles: [wgs]
  - name: Imaging
    description: "Medical imaging"
    profiles: [imaging]
  - name: sampling
    internal: true
  - name: sequencing
    internal: true
```

Rules:
* Extensions without `inherits:` implicitly extend the main table
* `inherits: [sampling, sequencing]` = multiple inheritance (all must share same root)
* `internal: true` marks an extension as non-selectable (`TableType.INTERNAL`)
* `profiles: [x]` scopes this extension to profile `x` (only visible in selector when `x` is active)
* Diamond inheritance resolved via upsert (same as current implementation)

### Sections and columns

```yaml
sections:
  - name: Common fields
    columns:
      - name: experiment id
        type: string
        key: 1
      - name: experiment type
        type: extension
        required: true
        profiles: [-core]
        defaultValue: WGS

  - name: Sample collection
    extension: sampling
    columns:
      - name: sample type
        type: ontology
        refTable: Sample types
      - name: tissue type
        type: ontology
        refTable: Tissue types

  - name: Imaging details
    extension: Imaging
    columns:
      - name: modality
        type: ontology
        refTable: Imaging modalities
```

Rules:
* `extension: X` on a section scopes ALL its columns to extension X (backend table assignment)
* `extension: X` on a column overrides its section's scope
* `profiles: [x]` on a section or column controls profile-based visibility (independent of extension)
* Sections without `extension:` are always visible (shared columns on parent table)
* Column order within sections determines display order

### Visibility rules

Two independent visibility mechanisms, combined with AND:

1. **Extension-based** (`extension: X`): section/column is visible when the user's selected extension includes X in its effective set
2. **Profile-based** (`profiles: [x]`): section/column is visible only when profile `x` is active

| `extension:` | `profiles:` | Visible when |
|---|---|---|
| absent | absent | Always visible |
| `sampling` | absent | Extension effective set includes `sampling` |
| absent | `[wgs]` | Profile `wgs` is active |
| `sampling` | `[wgs]` | Extension includes `sampling` AND profile `wgs` is active |

The `-` prefix inverts: `profiles: [-core]` means visible in all profiles EXCEPT `core`.

### Column properties

| Property | Type | Description |
|---|---|---|
| `name` | string | Column name (sentence case, lowercase start) |
| `type` | string | Column type (string, int, decimal, bool, date, ref, ontology, extension, extension_array, etc.) |
| `key` | int | Key index (1 = primary key) |
| `required` | bool | Whether the column is required |
| `defaultValue` | string | Default value for new rows |
| `description` | string | Human-readable description |
| `refTable` | string | Referenced table (for ref, ontology types) |
| `semantics` | string | Semantic URI (e.g., OBI, UBERON) |
| `profiles` | string[] | Profile-based visibility. `-` prefix excludes. |
| `extension` | string | Scopes this column to a specific extension (overrides section scope) |
| `validation` | string | Validation expression |
| `visible` | string | Visibility expression |

### Extension selector column

`type: extension` renders as a single-select dropdown. `type: extension_array` renders as a multi-select. Options are the non-internal extensions declared in the table's `extensions:` list, filtered by the active profile.

Standard column features apply:

| Feature | Effect |
|---|---|
| `required: true` | Must pick an extension before saving |
| `defaultValue: WGS` | Pre-selects WGS for new rows |
| `validation: ...` | Conditional validation expressions |
| `visible: ...` | Conditionally show/hide the selector |

### Section/extension visibility rule

When a user selects extension T:
* **Effective set** = {T} ∪ all extensions T extends (transitive)
* A section is visible when the effective set intersects the section's `extension:` value (AND profile condition is met)
* Sections without `extension:` are always visible (profile condition still applies)

Example:
```
WGS extends [sampling, sequencing, quality]
effective set = {WGS, sampling, sequencing, quality}
-> section with extension: sampling -> visible (if profile allows)
-> section with extension: Imaging -> not visible
-> section without extension -> always visible (if profile allows)
```

### Imports

Large files can be split using imports at any level, effectively replacing YAML at that position, optionally with overrides.

```yaml
sections:
  - name: Common fields
    columns:
      - name: experiment id
        type: string
        key: 1

  - import: blocks/sampling.yaml
    extension: sampling

  - import: blocks/sequencing.yaml
    extension: sequencing
```

Imported files define a section (name + columns) or column sequence. The importing table can override properties. Override rules:
* Section properties: shallow merge (local wins)
* Columns matched by name: merged in place (local properties override)
* New columns: appended to end of section

### Backend table structure

Each extension becomes a backend table:
* Shares parent table's PK (FK with ON UPDATE CASCADE, ON DELETE CASCADE)
* Contains only extension-specific columns
* Shared columns (from sections without `extension:`) live on the parent table

Querying the parent table auto-joins all extensions via LEFT JOIN -> wide sparse result where extension-specific columns are NULL for rows without that extension selected.

### Backend row management

When saving a row, the backend reads the extension selector column and ensures corresponding rows exist:
* **Insert**: create parent row, then create rows in each selected extension table (recursive for `inherits` chains, upsert for diamond)
* **Update** (extension changed): create rows in newly-selected extensions, delete rows from deselected extensions
* **Delete**: FK cascade handles cleanup
* All within a single database transaction

For `extension_array` (pick-many), a single parent row can have entries in multiple extension tables simultaneously.

---

## Profile definition files

Profiles are defined in their own YAML files in the `profiles/` directory. Each file defines one profile.

```yaml
name: wgs
description: "WGS genomics workflow"
includes: [genomics_base]
```

| Property | Type | Description |
|---|---|---|
| `name` | string | Profile identifier (`[a-zA-Z][a-zA-Z0-9_]*`) |
| `description` | string | Human-readable description |
| `includes` | string[] | Other profiles that are automatically activated when this profile is active |

Profile mechanics:
* `includes` reduces the number of profiles that need to be set — activating `wgs` also activates `genomics_base`
* Profiles only control visibility: which tables, extensions, sections, and columns are shown
* Everything is visible by default unless scoped by `profiles:` on the element
* Multiple profiles can be active simultaneously (union of visibility)
* Profiles are referenced by identifier on tables, extensions, sections, and columns via `profiles: [x, -y]`

Example profile hierarchy:
```yaml
# profiles/genomics_base.yaml
name: genomics_base
description: "Base genomics extensions"

# profiles/wgs.yaml
name: wgs
description: "WGS genomics workflow"
includes: [genomics_base]

# profiles/cranio.yaml
name: cranio
description: "Craniofacial registry"
includes: [genomics_base]
```

Activating `wgs` -> also activates `genomics_base`. Anything scoped to `genomics_base` is visible in both `wgs` and `cranio` deployments.

---

## Template files — one per deployment

Each template file configures a deployable instance. Templates can populate multiple PostgreSQL schemas.

```yaml
name: RD3
description: "Rare Disease Data for Discovery"

imports:
  - tables/*

profiles:
  - wgs
  - rna

settings:
  import: settings/rd3.yaml

permissions:
  view: anonymous
  edit: user

fixedSchemas:
  - schemaName: RD3 ontologies
    description: "Shared ontology tables"
    imports:
      - _ontologies/*
    permissions:
      view: anonymous
```

### Template properties

| Property | Type | Description |
|---|---|---|
| `name` | string | Deployment name (shown in admin UI) |
| `description` | string | What this deployment is for |
| `imports` | list | Table files to load (supports `tables/*` wildcards) |
| `profiles` | list | Active profile identifiers (omit -> all extensions visible) |
| `settings` | mapping or `import:` | Schema-level configuration (landing page, menu, theme) |
| `permissions` | mapping | Default role-based access (e.g., `view: anonymous`) |
| `fixedSchemas` | list | Additional schemas with fixed names |

### How templates work

The administrator selects a template file and provides a name for the main schema. The system:
1. Creates the main schema with the user-provided name
2. Imports all tables (expanding wildcards)
3. Creates all extensions (all extension tables exist in PostgreSQL)
4. Applies profile(s) — controls which extensions appear in selector dropdowns
5. Sets permissions and settings
6. Creates any fixed schemas (e.g., shared ontologies)

---

## Complete Example: Experiments table

```yaml
table: Experiments
description: "Wet-lab and dry-lab experiments, ISA-like"

extensions:
  - name: WGS
    description: "Short-read whole genome sequencing"
    inherits: [sampling, sequencing, quality]
    profiles: [wgs]
  - name: WES
    description: "Exome capture followed by short-read sequencing"
    inherits: [sampling, sequencing, quality]
    profiles: [wgs]
  - name: RNA-seq
    description: "Transcriptome sequencing"
    inherits: [sampling, sequencing]
    profiles: [rna]
  - name: Optical genome mapping
    inherits: [sampling]
    profiles: [wgs]
  - name: Array
    description: "SNP array / genotyping chip"
    inherits: [sampling]
    profiles: [wgs]
  - name: Imaging
    description: "Medical imaging (MRI, CT, PET, X-ray)"
    profiles: [imaging]
  - name: Variant calling
    description: "Computational variant detection pipeline"
    profiles: [wgs, rna]
  - name: Expression quantification
    profiles: [rna]
  - name: Alignment
    profiles: [wgs, rna]
  - name: sampling
    internal: true
  - name: sequencing
    internal: true
  - name: quality
    internal: true

sections:
  - name: Common fields
    columns:
      - name: experiment id
        type: string
        key: 1
        description: "Unique experiment identifier"
      - name: date
        type: date
      - name: protocol
        type: string
        description: "Protocol name or reference"
      - name: performer
        type: string
      - name: experiment type
        type: extension
        description: "Select the type of experiment to show relevant fields"
        required: true
        profiles: [-core]
        defaultValue: WGS

  - name: Linked entities
    columns:
      - name: sample
        type: ref
        refTable: Samples
      - name: individual
        type: refLink
        refLink: sample.individual
      - name: parent experiments
        type: ref_array
        refTable: Experiments
        description: "Upstream experiments this one depends on (DAG)"

  - name: Sample collection
    extension: sampling
    columns:
      - name: sample type
        type: ontology
        refTable: Sample types
        semantics: http://purl.obolibrary.org/obo/OBI_0000747
      - name: tissue type
        type: ontology
        refTable: Tissue types
        semantics: http://purl.obolibrary.org/obo/UBERON_0000479
      - name: collection method
        type: ontology
        refTable: Collection methods
      - name: pathological state
        type: ontology
        refTable: Pathological states

  - name: Sequencing
    extension: sequencing
    columns:
      - name: library strategy
        type: ontology
        refTable: Library strategies
        semantics: http://www.ebi.ac.uk/efo/EFO_0004184
      - name: read length
        type: int
      - name: platform
        type: ontology
        refTable: Platforms

  - name: Quality control
    extension: quality
    columns:
      - name: QC pass fail
        type: bool
      - name: coverage depth
        type: decimal
        description: "Mean coverage depth (x)"

  - name: Optical genome mapping
    extension: Optical genome mapping
    columns:
      - name: OGM enzyme
        type: string
      - name: OGM labelling
        type: string

  - name: Array / chip
    extension: Array
    columns:
      - name: array platform
        type: string
      - name: chip version
        type: string

  - name: Imaging
    extension: Imaging
    columns:
      - name: modality
        type: ontology
        refTable: Imaging modalities
      - name: body part examined
        type: string

  - name: Variant calling
    extension: Variant calling
    columns:
      - name: variant caller
        type: string
      - name: aligner
        type: string
      - name: reference genome
        type: string

  - name: Expression quantification
    extension: Expression quantification
    columns:
      - name: quantification tool
        type: string
      - name: normalisation method
        type: string

  - name: Alignment
    extension: Alignment
    columns:
      - name: alignment tool
        type: string
      - name: alignment reference genome
        type: string
```

## Complete Example: Observations table (pick-many)

```yaml
table: Observations
description: "Clinical observations, additive extensions"

extensions:
  - name: Dermatology
    description: "Skin examination findings"
    profiles: [cranio]
  - name: Neurology
    description: "Neurological assessment scores"
    profiles: [neuro]
  - name: Questionnaire
    description: "Validated instruments and scores"
  - name: Metabolic panel
    description: "Basic metabolic panel results"

sections:
  - name: Common fields
    columns:
      - name: observation id
        type: string
        key: 1
      - name: date
        type: date
      - name: performer
        type: string
      - name: individual
        type: ref
        refTable: Individuals
      - name: observation types
        type: extension_array
        description: "Select one or more clinical domains for this observation"

  - name: Dermatology
    extension: Dermatology
    columns:
      - name: BSA percentage
        type: decimal
        description: "Body surface area affected (%)"
      - name: lesion type
        type: string

  - name: Neurology
    extension: Neurology
    columns:
      - name: motor score
        type: int
      - name: cognitive score
        type: int

  - name: Questionnaire
    extension: Questionnaire
    columns:
      - name: instrument name
        type: string
      - name: total score
        type: decimal

  - name: Metabolic panel
    extension: Metabolic panel
    columns:
      - name: glucose
        type: decimal
        description: "Fasting glucose (mmol/L)"
      - name: creatinine
        type: decimal
        description: "Serum creatinine (umol/L)"
```

## Complete Example: Template files

### RD3 template

```yaml
name: RD3
description: "Rare Disease Data for Discovery"

imports:
  - tables/*

profiles:
  - wgs
  - rna

settings:
  import: settings/rd3.yaml

permissions:
  view: anonymous
  edit: user

fixedSchemas:
  - schemaName: RD3 ontologies
    description: "Shared ontology tables"
    imports:
      - _ontologies/*
    permissions:
      view: anonymous
```

### Cranio template

```yaml
name: Cranio
description: "Craniofacial registry — imaging + dermatology focus"

imports:
  - tables/Experiments.yaml
  - tables/Observations.yaml
  - tables/Samples.yaml
  - tables/Individuals.yaml
  - tables/Files.yaml

profiles:
  - cranio

settings:
  import: settings/cranio.yaml

permissions:
  view: anonymous
  edit: user

fixedSchemas:
  - schemaName: Cranio ontologies
    imports:
      - _ontologies/*
    permissions:
      view: anonymous
```

### Full template (dev/testing)

```yaml
name: Full
description: "All tables, all profiles — development / testing"

imports:
  - tables/*
  - _ontologies/*

settings:
  import: settings/dev.yaml

permissions:
  view: anonymous
  edit: user

# no profiles -> all extensions visible
# no fixedSchemas -> just the main schema
```

## Repository layout

```
molgenis/rd3-data-model/
  templates/
    rd3.yaml
    cranio.yaml
    full.yaml
  tables/
    Experiments.yaml
    Observations.yaml
    Samples.yaml
    Individuals.yaml
    Files.yaml
  blocks/                        # importable section fragments
    sampling.yaml
    sequencing.yaml
    quality.yaml
  profiles/
    genomics_base.yaml
    wgs.yaml
    rna.yaml
    cranio.yaml
    neuro.yaml
  _ontologies/
    Sample types.yaml
    Tissue types.yaml
    Library strategies.yaml
    Platforms.yaml
    Imaging modalities.yaml
  settings/
    rd3.yaml
    cranio.yaml
    dev.yaml
  CHANGELOG.md
```

---

## Naming Conventions (EMX2 sentence case)

| Thing | Convention | Example | Auto-derived ID |
|---|---|---|---|
| Table name | Uppercase start | `Sample types` | `SampleTypes` |
| Column name | Lowercase start | `sample type` | `sampleType` |
| Section name | Uppercase start | `Common fields` | `CommonFields` |
| Extension name | Uppercase start | `Whole genome sequencing` | `WholeGenomeSequencing` |
| Internal extension | Lowercase start (convention) | `sampling`, `quality` | `sampling`, `quality` |
| Profile identifier | `[a-zA-Z][a-zA-Z0-9_]*` | `wgs`, `genomics_base` | (used as-is) |

Because names are sentence case, they serve as display labels by default. An explicit `label` is only needed when the display text should differ from the name. Profile identifiers are the exception — they are technical identifiers, not display labels.

---

## Design Considerations

### Strengths

* Modular and scalable
* Strong typing preserved
* Stable API surface (all extensions always deployed)
* Flexible UI behavior via profiles
* Clear separation of concerns
* Aligned with FHIR/openEHR patterns

### Risks and mitigations

**Profile combinatorics** — multiple profiles may conflict.
Mitigation: profile `includes` for hierarchy, union semantics (visibility is additive across profiles).

**Data drift** — profiles only affect UI unless enforced.
Mitigation: backend validation enforces profile constraints. SqlQuery can be made profile-aware to exclude out-of-profile columns from results.

**Migration complexity** — schema evolves over time.
Mitigation: all extensions always exist in database, so migration is just data migration, not schema changes.

---

## Mapping to current implementation

| New name | Current Java/SQL | Change needed |
|---|---|---|
| `inherits` (YAML) | `inheritNames` (Java), `tableExtends` (CSV) | YAML parser uses new name; Java/CSV unchanged for now |
| `EXTENSION` (columnType) | `ColumnType.PROFILE` | Rename enum value |
| `EXTENSION_ARRAY` (columnType) | `ColumnType.PROFILES` | Rename enum value |
| Extension | Subtable (child with profile column on root) | Terminology only |
| Internal extension | `TableType.INTERNAL` | Already implemented |
| Template | Schema file (v8 spec) | New YAML structure |
| Profile | (new concept) | New metadata + parser |
