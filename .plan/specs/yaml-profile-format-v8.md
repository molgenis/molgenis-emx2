# Profile Syntax Proposal for EMX2 YAML (v8)

Proof of concept for the profile mechanism in EMX2 YAML. Uses Approach A's table design as the running example.

## Design decisions

1. **One file, one table.** Each YAML file defines exactly one table. Top-level properties are `tableName`, `description`, `tableType`, `profiles`, `sections`. No wrapping key, no nesting of tables.

2. **EMX2 naming conventions.** Table names use sentence case starting uppercase (e.g. `Experiments`, `Sample types`). Column names use sentence case starting lowercase (e.g. `experiment id`, `sample type`). EMX2 auto-derives camelCase/PascalCase identifiers from these names in code. This means the `name` property doubles as the display label in most cases — an explicit `label` is only needed when the display text should differ from the name.

3. **Sections are structural YAML.** The `sections:` property is a YAML sequence of section objects, each with `name`, optional `profiles`, and a nested `columns:` sequence. This gives IDEs real folding — collapse a section and you see one line per section, giving an instant overview of the table.

4. **Profile selector is a column.** `columnType: profile` (pick-one) or `columnType: profiles` (pick-many). These are new base column types (not REF-based) — their valid values are derived from the table's declared subtables in `table_metadata`, not from a separate data table. Being a normal column it inherits `required`, `defaultValue`, `visible`, `validation`. Position in column order is controlled by placing it in the appropriate section.

5. **Section-scoped profiles.** A section tagged `profiles: [sampling]` scopes all its columns to that profile. The `profiles:` tag can reference any name in the table's `profiles:` header — whether block or subtable. A column can override with its own `profiles:` tag for additive membership. Sections without `profiles:` are always visible.

6. **Import at section level.** `- import: blocks/sampling.yaml` in the `sections:` list splices a block file (which is a section definition) at that position. The importing table can override section-level properties (`name`, `description`, `profiles`) and column-level properties. Overrides are merged onto the imported definition: section properties are shallow-merged, column overrides match by `name` and are applied at their original position, new columns are appended to the section. The imported section's `profiles:` list is filtered to only names declared in the importing table's `profiles:` header — unknown names are silently stripped.

7. **Import at column level.** `- import: blocks/sampling.yaml` can also appear inside a section's `columns:` list. The imported file is then a column sequence (no section wrapper). Same override rules: later columns with the same `name` override in place; new columns are appended.

8. **Subtables and blocks.** Subtables are user-selectable profiles (shown in the profile selector dropdown) and each subtable becomes a backend table. Blocks group column definitions and compose into subtables via `includes:` — they also become backend tables (to avoid column duplication when shared across subtables). Blocks are marked `tableType = BLOCK` in `table_metadata`; subtables are regular DATA tables — their "subtable" status is derived from being a non-BLOCK child of a table with a PROFILE column. Profile options come from non-BLOCK children of the parent table.

9. **Main schema at top level, fixed schemas alongside.** Each schema file defines the main schema (user-named at deploy time) at the top level: `imports`, `activeProfiles`, `settings`, `permissions`. Additional schemas with fixed names (e.g. a shared ontology schema) are declared in `fixedSchemas:`. This keeps the primary thing front and centre.

10. **`activeProfiles` is a flat list.** Table and subtable names are globally unique within the schema (see §17), so a flat list (e.g. `[WGS, WES, Imaging, Dermatology]`) is sufficient. `activeProfiles` only lists subtables (not blocks — blocks have no backend presence). The system matches each name against all imported tables. Purely subtractive: only listed subtables appear in the per-row profile selector. Omitting `activeProfiles` deploys all subtables.

11. **Wildcard imports.** `imports:` supports glob patterns: `tables/*` imports all YAML files in `tables/`, `_ontologies/*` imports all ontology tables. Explicit file paths are also supported for fine-grained control. Wildcards reduce maintenance — adding a new table file automatically includes it.

12. **Settings.** Each schema can declare `settings:` as inline key-value pairs or import them from a file. Settings configure schema-level behaviour (e.g. landing page, menu structure, theme).

13. **Default permissions.** Each schema can declare `permissions:` with role-based defaults (e.g. `view: anonymous`, `edit: user`). This controls who can see and modify data in the schema.

14. **Standard column features on profile selector.** `defaultValue` pre-selects, `required` forces selection, `validation` expressions handle profile-conditional required fields.

15. **One-repo governance.** Single repository, tagged releases, PR-based collaboration.

16. **`tableExtends` translated to profiles in v8.** The v8 YAML format achieves column composition through profiles/blocks/imports instead of `tableExtends`. The CSV format with `tableExtends` remains fully supported for backward compatibility. Migration handled separately.

17. **Only subtables become backend tables; blocks do not.** Each subtable declared in a table's `profiles:` header resolves to its own backend table, scoped to that parent table. Foreign keys (`ref`, `ref_array`, `refLink`) can point to any table or subtable — they are all backend tables. Blocks are parse-time constructs only: they group columns and compose into subtables, but produce no backend table. This means block files (e.g. `blocks/sampling.yaml`) can be freely imported into multiple tables without conflict. Consequence: table names and subtable names must be **globally unique within the schema** — they share a single namespace. Block names only need to be unique within their declaring table's `profiles:` header.

18. **Backend manages subtable table rows.** When a user saves a row with a profile selector value, the backend ensures corresponding rows exist in the selected subtable tables (linked by the parent PK). For pick-many (`columnType: profiles`), the parent row can have entries in multiple subtable tables simultaneously. The profile selector column is the single source of truth — no system-level discriminator column (like `mg_tableclass`) is needed.

---

## Schema files — one per deployment

Each deployment has its own schema file. The main schema — the one the deploying user names — is defined at the top level. Additional schemas with fixed names (e.g. a shared ontology schema) are declared in `fixedSchemas:`.

### schema-rd3.yaml

```yaml
name: RD3
description: "Rare Disease Data for Discovery"

imports:
  - tables/*

activeProfiles:
  - WGS
  - WES
  - RNA-seq
  - Variant calling
  - Alignment
  - Expression quantification
  - Neurology
  - Questionnaire
  - Metabolic panel

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

When a user deploys this and names their schema "Groningen RD3", the system creates two schemas: "Groningen RD3" (the main data schema, user-named) and "RD3 ontologies" (fixed).

### schema-cranio.yaml

```yaml
name: Cranio
description: "Craniofacial registry — imaging + dermatology focus"

imports:
  - tables/Experiments.yaml
  - tables/Observations.yaml
  - tables/Samples.yaml
  - tables/Individuals.yaml
  - tables/Files.yaml

activeProfiles:
  - WGS
  - Imaging
  - Array
  - Dermatology
  - Questionnaire

settings:
  import: settings/cranio.yaml

permissions:
  view: anonymous
  edit: user

fixedSchemas:
  - schemaName: Cranio ontologies
    description: "Shared ontology tables for Cranio"
    imports:
      - _ontologies/*
    permissions:
      view: anonymous
```

### schema-full.yaml

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

# no activeProfiles → all subtables
# no fixedSchemas → just the main schema
```

### Settings files

Settings are key-value pairs that configure schema-level behaviour. They can be declared inline or imported from a file.

#### settings/rd3.yaml

```yaml
landingPage: "Experiments"
menuLayout: [Experiments, Observations, Samples, Individuals, Files]
theme: "rd3-blue"
showProfileSelector: true
```

#### Inline settings (alternative)

```yaml
settings:
  landingPage: "Experiments"
  menuLayout: [Experiments, Observations, Samples, Individuals, Files]
```

**How the schema file works:**

- **Top-level properties** define the main schema. The deploying user provides the name. `imports`, `activeProfiles`, `settings`, `permissions` all apply to this main schema.
- **`activeProfiles`** — a flat list of subtable names (not blocks). Purely subtractive: only listed subtables appear in the per-row profile selector across all imported tables. Since table and subtable names are globally unique within the schema, a flat list works. Omitting `activeProfiles` deploys all subtables.
- **`fixedSchemas`** — additional schemas created alongside the main one. Each has a fixed `schemaName`, its own `imports`, `settings`, `permissions`. Typically used for a shared read-only ontology schema.
- **`settings`** — either inline key-value pairs or `import: path` to load from a file. Settings configure UI behaviour, not data structure.
- **`permissions`** — role-based defaults. `view: anonymous` means anyone can read; `edit: user` means authenticated users can write. These are schema-wide defaults; table-level and row-level permissions can override.

---

## Experiments table (pick-one profiles)

```yaml
tableName: Experiments
tableType: data
description: "Wet-lab and dry-lab experiments, ISA-like"

profiles:
  # ── subtables (user-selectable) ──────────────────
  - name: WGS
    description: "Short-read whole genome sequencing: sample → library → sequencing → QC"
    includes: [sampling, sequencing, quality]
  - name: WES
    description: "Exome capture followed by short-read sequencing"
    includes: [sampling, sequencing, quality]
  - name: RNA-seq
    description: "Transcriptome sequencing (mRNA, total RNA, small RNA)"
    includes: [sampling, sequencing]
  - name: Optical genome mapping
    includes: [sampling, OGM block]
  - name: Array
    description: "SNP array / genotyping chip"
    includes: [sampling, Array block]
  - name: Imaging
    description: "Medical imaging (MRI, CT, PET, X-ray)"
  - name: Variant calling
    description: "Computational variant detection pipeline"
  - name: Expression quantification
  - name: Alignment
  # ── blocks (internal, not user-selectable) ───────
  - name: sampling
    type: block
  - name: sequencing
    type: block
  - name: quality
    type: block
  - name: OGM block
    type: block
  - name: Array block
    type: block

sections:
  # ════════════════════════════════════════════════════
  # SHARED (always visible)
  # ════════════════════════════════════════════════════
  - name: Common fields
    columns:
      - name: experiment id
        columnType: string
        key: 1
        description: "Unique experiment identifier"
      - name: date
        columnType: date
      - name: protocol
        columnType: string
        description: "Protocol name or reference"
      - name: performer
        columnType: string
      - name: experiment type
        columnType: profile
        description: "Select the type of experiment to show relevant fields"
        required: true
        defaultValue: WGS

  - name: Linked entities
    columns:
      - name: sample
        columnType: ref
        refTable: Samples
      - name: individual
        columnType: refLink
        refLink: sample.individual
      - name: sample set
        columnType: ref
        refTable: Sample sets
      - name: subpopulation
        columnType: ref
        refTable: Subpopulations
      - name: parent experiments
        columnType: ref_array
        refTable: Experiments
        description: "Upstream experiments this one depends on (DAG)"

  # ════════════════════════════════════════════════════
  # PROFILE-SPECIFIC (blocks + subtables)
  # ════════════════════════════════════════════════════

  # ── imported blocks (reusable across tables) ───────
  - import: blocks/sampling.yaml

  - import: blocks/sequencing.yaml

  - import: blocks/quality.yaml

  # ── table-specific blocks (inline) ─────────────────
  - name: Optical genome mapping
    profiles: [OGM block]
    columns:
      - name: OGM enzyme
        columnType: string
      - name: OGM labelling
        columnType: string

  - name: Array / chip
    profiles: [Array block]
    columns:
      - name: array platform
        columnType: string
      - name: chip version
        columnType: string

  # ── subtable-owned sections (no block) ─────────────
  - name: Imaging
    profiles: [Imaging]
    columns:
      - name: modality
        columnType: ontology
        refTable: Imaging modalities
      - name: body part examined
        columnType: string

  - name: Variant calling
    profiles: [Variant calling]
    columns:
      - name: variant caller
        columnType: string
      - name: aligner
        columnType: string
      - name: reference genome
        columnType: string

  - name: Expression quantification
    profiles: [Expression quantification]
    columns:
      - name: quantification tool
        columnType: string
      - name: normalisation method
        columnType: string

  - name: Alignment
    profiles: [Alignment]
    columns:
      - name: alignment tool
        columnType: string
      - name: alignment reference genome
        columnType: string
```

---

## Block files

A block file defines a single section. It is structurally identical to an inline section. Because section names use sentence case, the `name` doubles as the display label — no separate `label` needed.

Block files may carry a `profiles:` tag listing the block names they are designed for. This tag is used for subtable composition (`includes:` resolution) — it does not create a backend table. On import, the parser **filters the `profiles:` list to only names declared in the importing table's `profiles:` header**. Unknown profile names are silently stripped. This allows the same block file to list multiple profile names (e.g. `profiles: [sampling, sample prep]`) and work across different tables that declare different subsets. The importing table can also override the `profiles:` tag entirely.

### blocks/sampling.yaml

```yaml
name: Sample collection
profiles: [sampling]
columns:
  - name: sample type
    columnType: ontology
    refTable: Sample types
    semantics: http://purl.obolibrary.org/obo/OBI_0000747
  - name: tissue type
    columnType: ontology
    refTable: Tissue types
    semantics: http://purl.obolibrary.org/obo/UBERON_0000479
  - name: collection method
    columnType: ontology
    refTable: Collection methods
  - name: pathological state
    columnType: ontology
    refTable: Pathological states
```

### blocks/sequencing.yaml

```yaml
name: Sequencing
profiles: [sequencing]
columns:
  - name: library strategy
    columnType: ontology
    refTable: Library strategies
    semantics: http://www.ebi.ac.uk/efo/EFO_0004184
  - name: read length
    columnType: int
  - name: platform
    columnType: ontology
    refTable: Platforms
```

### blocks/quality.yaml

```yaml
name: Quality control
profiles: [quality]
columns:
  - name: QC pass fail
    columnType: bool
  - name: coverage depth
    columnType: decimal
    description: "Mean coverage depth (×)"
```

---

## Override mechanics

Importing is merging. When you write `- import: blocks/sampling.yaml` in a `sections:` list, the imported section is the base. Any properties you add alongside the `import:` are merged on top.

### Section-level override

Override section properties and/or columns:

```yaml
sections:
  # Import sampling block, rename and customise
  - import: blocks/sampling.yaml
    name: "Specimen collection"
    description: "Adapted for biospecimen processing"
    columns:
      - name: sample type
        required: true                     # merged onto imported column in place
      - name: storage condition            # new column, appended to section
        columnType: string
        description: "Storage temperature and medium"
```

**Merge rules for section-level import:**

| What | Rule | Example |
|------|------|---------|
| Section properties (`name`, `description`) | Local overrides imported. Omitted keeps imported. | `name: "Specimen collection"` replaces `name: "Sample collection"` |
| `profiles` | Local overrides imported. If not overridden, imported value is filtered to only names in the importing table's `profiles:` header. | Block with `profiles: [sampling, sample prep]` imported into table declaring only `sampling` → `profiles: [sampling]` |
| Existing column (same `name`) | Local properties merged onto imported column at its original position. | `sample type` stays in position 1, gains `required: true` |
| New column (name not in import) | Appended to end of section. | `storage condition` added after `pathological state` |

### Column-level import

A block file can also be imported inside a section's `columns:` list. In this case the file contains a column sequence (no section wrapper):

#### blocks/address columns.yaml

```yaml
- name: street
  columnType: string
- name: city
  columnType: string
- name: postal code
  columnType: string
- name: country
  columnType: ontology
  refTable: Countries
```

#### Usage inside a section

```yaml
sections:
  - name: Contact information
    columns:
      - name: email
        columnType: string
      - import: blocks/address columns.yaml
      - name: postal code
        required: true     # overrides imported column in place
      - name: phone
        columnType: string
```

**Same rules at both levels: import is the base, local is the overlay, matching is by `name`, originals keep their position, new items append.**

---

## Observations table (pick-many profiles)

```yaml
tableName: Observations
tableType: data
description: "Clinical observations, additive profiles"

profiles:
  - name: Dermatology
    description: "Skin examination findings"
  - name: Neurology
    description: "Neurological assessment scores"
  - name: Questionnaire
    description: "Validated instruments and scores"
  - name: Metabolic panel
    description: "Basic metabolic panel results"

sections:
  - name: Common fields
    columns:
      - name: observation id
        columnType: string
        key: 1
      - name: date
        columnType: date
      - name: performer
        columnType: string
      - name: individual
        columnType: ref
        refTable: Individuals
      - name: observation types
        columnType: profiles
        description: "Select one or more clinical domains for this observation"

  - name: Dermatology
    profiles: [Dermatology]
    columns:
      - name: BSA percentage
        columnType: decimal
        description: "Body surface area affected (%)"
      - name: lesion type
        columnType: string

  - name: Neurology
    profiles: [Neurology]
    columns:
      - name: motor score
        columnType: int
      - name: cognitive score
        columnType: int

  - name: Questionnaire
    profiles: [Questionnaire]
    columns:
      - name: instrument name
        columnType: string
      - name: total score
        columnType: decimal

  - name: Metabolic panel
    profiles: [Metabolic panel]
    columns:
      - name: glucose
        columnType: decimal
        description: "Fasting glucose (mmol/L)"
      - name: creatinine
        columnType: decimal
        description: "Serum creatinine (µmol/L)"
```

---

## How it works

### Naming conventions

EMX2 uses sentence case for human-readable names:

| Thing | Convention | Example | Auto-derived ID |
|-------|-----------|---------|-----------------|
| Table name | Uppercase start | `Sample types` | `SampleTypes` |
| Column name | Lowercase start | `sample type` | `sampleType` |
| Section name | Uppercase start | `Common fields` | `CommonFields` |
| Subtable name | Uppercase start | `Whole Genome Sequencing` | `WholeGenomeSequencing` |
| Block name | Free-form (convention: lowercase start) | `sampling`, `OGM block` | `sampling`, `OGMBlock` |

Subtable names share the global namespace with table names (both are backend tables). Block names only need to be unique within their declaring table.

Because names are sentence case, they serve as display labels by default. An explicit `label` is only needed when the display text should differ from the name (rare).

### File structure

Each table is a YAML file with these top-level properties:

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `tableName` | string | yes | Table name in sentence case |
| `tableType` | string | yes | `data`, `ontologies`, etc. |
| `description` | string | no | Human-readable description |
| `profiles` | sequence | no | Subtable and block definitions |
| `sections` | sequence | yes | Ordered list of sections containing columns |

### Schema files

Each schema file defines the main schema at the top level, with optional `fixedSchemas:` alongside:

**Main schema (top-level properties):**

| Property | Type | Description |
|----------|------|-------------|
| `name` | string | Deployment name (shown in admin UI) |
| `description` | string | What this deployment is for |
| `imports` | list | Table files to load (supports `tables/*` wildcards) |
| `activeProfiles` | list | Subtable names to activate (flat list; omit → all) |
| `settings` | mapping or `import:` | Schema-level configuration |
| `permissions` | mapping | Default role-based access (e.g. `view: anonymous`) |

**Fixed schemas (`fixedSchemas:` list):**

| Property | Type | Description |
|----------|------|-------------|
| `schemaName` | string | Fixed schema name (always created with this name) |
| `description` | string | What this schema is for |
| `imports` | list | Table files to load |
| `settings` | mapping or `import:` | Schema-level configuration |
| `permissions` | mapping | Default role-based access |

The administrator selects a schema file and provides a name for the main schema. The system creates the main schema with the user-provided name, plus any fixed schemas. Tables are imported, `activeProfiles` applied, permissions set.

### Profile selector column

`columnType: profile` renders as a single-select dropdown; `columnType: profiles` renders as a multi-select. Options are the subtable names from `profiles:`, filtered by `activeProfiles` from the schema file. Standard column features apply:

| Feature | Effect |
|---------|--------|
| `required: true` | Must pick a profile before saving |
| `defaultValue: WGS` | Pre-selects WGS for new rows |
| `validation: if(experiment type == 'WGS') required(library strategy)` | Profile-conditional required fields |
| `visible: ...` | Conditionally show/hide the selector |

### Section visibility rule

The `profiles:` tag on a section or column can reference **any name** in the table's `profiles:` header — whether block or subtable. The names are resolved within the declaring table only.

When a user selects a subtable T, the **effective profile set** is `{T} ∪ T.includes` (block inclusion is not recursive — blocks cannot include other blocks). A section is visible when the effective profile set intersects the section's `profiles:` list, or when the section has no `profiles:` tag (always visible).

### Subtable composition

Subtables may optionally compose blocks via `includes:`. Blocks are resolved at parse time — they determine which sections/columns belong to a subtable but do not produce backend tables themselves. Only the subtable becomes a backend table.

Example — subtable with `includes:` (Experiments):
```
WGS.includes = [sampling, sequencing, quality]
effective set = {WGS, sampling, sequencing, quality}
→ sections tagged profiles: [sampling] → visible
→ sections tagged profiles: [WGS] → visible
→ sections tagged profiles: [Imaging] → not visible
```

Example — subtable without `includes:` (Observations):
```
Dermatology.includes = [] (none)
effective set = {Dermatology}
→ sections tagged profiles: [Dermatology] → visible
→ sections tagged profiles: [Neurology] → not visible
```

A subtable may have both `includes:` and sections directly tagged with its own name. Shared sections (no `profiles:`) are always visible regardless of selection.

### Subtable backend table structure

Each subtable declared in a table's `profiles:` header becomes a backend table that extends the parent:
- The subtable table contains the **parent PK column(s)** as its own PK + FK to the parent table (ON UPDATE CASCADE, ON DELETE CASCADE)
- The subtable table contains only **profile-specific columns** — columns from sections/blocks scoped to that subtable
- Shared columns (from sections without `profiles:`) live on the parent table only
- This is the same FK mechanism as `tableExtends`, but WITHOUT the `mg_tableclass` discriminator column

Querying a subtable table directly returns only rows for that subtable. Querying the parent table **auto-joins all active subtable tables** (LEFT JOIN), producing a wide sparse result where subtable-specific columns are NULL for rows that don't have that subtable selected. This is the same behavior as current `tableExtends` / `tableWithInheritanceJoin()`, minus `mg_tableclass`.

### Backend row management

When saving a row, the backend reads the profile selector column value and ensures corresponding rows exist in the selected subtable tables (linked by parent PK). Specifically:
- **On insert**: create the parent row, then create rows in each selected subtable table with the same PK
- **On update** (profile value changed): create rows in newly-selected subtable tables, delete rows from deselected subtable tables
- **On delete**: cascade via FK handles subtable table cleanup automatically
- All operations within the same database transaction

For `columnType: profiles` (pick-many), a single parent row can have entries in multiple subtable tables simultaneously. The profile selector column is the single source of truth — no system-level discriminator is needed.

### Parser behaviour

1. Read the selected schema file. Resolve imports for the main schema (expand wildcards) → full table list. Do the same for each fixed schema.
2. Apply `activeProfiles` (flat list) to the main schema: filter subtables across all tables. Apply `settings` and `permissions`.
3. For each table file: walk `sections:` in order. For each `- import:`, read the block file, merge overrides. Filter imported `profiles:` to names in this table's `profiles:` header.
4. Within each section, walk `columns:`. For each `- import:`, splice. Resolve overrides (later same-name wins in place).
5. Resolve subtable composition: for each subtable T, collect all columns from sections where `T ∈ section.profiles` OR any block B in `T.includes` has `B ∈ section.profiles`. These columns form T's backend table. Also collect column-level `profiles:` overrides.
6. Result: parent tables with shared columns + PROFILE/PROFILES column, subtable tables (extending parent via FK, regular DATA type) with profile-specific columns, and block tables (`tableType = BLOCK`, also PG tables with FK to parent). Profile selector options = non-BLOCK children of the parent table.

### Additive column scope

A column can override its section's profile scope with an explicit `profiles:` tag:

```yaml
  - name: Common fields
    # no profiles → always visible
    columns:
      - name: experiment id
        columnType: string
        key: 1
        # inherits: always visible
      - name: special note
        columnType: string
        profiles: [WGS, WES]
        # override: only visible in WGS and WES
```

---

## Repository layout

```
molgenis/rd3-data-model/
  schema-rd3.yaml                # RD3 deployment (ontologies + data)
  schema-cranio.yaml             # Cranio deployment
  schema-full.yaml               # Full (dev/test)
  tables/
    Experiments.yaml
    Observations.yaml
    Samples.yaml
    Sample sets.yaml
    Individuals.yaml
    Subpopulations.yaml
    Files.yaml
  blocks/                        # reusable sections
    sampling.yaml
    sequencing.yaml
    quality.yaml
    address columns.yaml         # column-level block
  _ontologies/
    Sample types.yaml
    Tissue types.yaml
    Library strategies.yaml
    Platforms.yaml
    Imaging modalities.yaml
    Collection methods.yaml
    Pathological states.yaml
  settings/
    rd3.yaml                     # RD3 settings (landing page, menu, theme)
    cranio.yaml
    dev.yaml
  CHANGELOG.md
```

External collaborators contribute via pull requests. Releases are tagged (`v3.2.0`). Downstream deployments pin to a version and select a schema file (e.g. `schema-cranio.yaml`).

---

## Summary of syntax elements

| Element | Where | Meaning |
|---------|-------|---------|
| `tableName` | file top-level | Table name (sentence case, uppercase start) |
| `profiles:` | file top-level | Subtable + block declarations |
| `sections:` | file top-level | Ordered list of sections |
| `columns:` | inside a section | Ordered list of columns (sentence case, lowercase start) |
| `columnType: profile` | on a column | Pick-one profile selector |
| `columnType: profiles` | on a column | Pick-many profile selector |
| `profiles: [x, y]` | on a section | Scopes section to profiles x, y (composition tag, not table creation) |
| `profiles: [x, y]` | on a column | Overrides section scope for this column |
| `includes: [a, b]` | on a subtable | Subtable includes blocks a, b (columns merged into subtable table) |
| `type: block` | on a profile entry | Internal block, not user-selectable, no backend table |
| `- import: path` | in `sections:` | Import a block file (section-level) |
| `- import: path` | in `columns:` | Import a column file (column-level) |
| `defaultValue`, `required`, `validation` | on profile column | Standard column features |
| `ref`, `ref_array`, `refLink` | on a column | Can target tables or subtables (both are backend tables; blocks are not) |
| `name` | in schema file | Deployment name |
| `imports:` | in schema file (top-level) | Table files to load (supports `tables/*` wildcards) |
| `activeProfiles:` | in schema file (top-level) | Flat list of subtable names to activate (omit → all) |
| `settings:` | in schema file | Key-value config or `import: path` to load settings file |
| `permissions:` | in schema file | Default access control (e.g. `view: anonymous`, `edit: user`) |
| `fixedSchemas:` | in schema file | Additional schemas with fixed names (e.g. ontologies) |

---

## Phase 1+2 Behavior Spec (Java model + SQL layer)

### Core Model

| Behavior | Component | Test | Status |
|---|---|---|---|
| `TableType.BLOCK` enum exists | TableType.java | ProfileMetadataTest | implemented |
| `ColumnType.PROFILE` extends STRING, `isReference()==false` | ColumnType.java | ProfileMetadataTest | implemented |
| `ColumnType.PROFILES` extends STRING_ARRAY, `isArray()==true` | ColumnType.java | ProfileMetadataTest | implemented |
| `getInheritNames()` returns String[] (not String — compiler catches callers) | TableMetadata.java | ProfileMetadataTest | implemented |
| `setInheritNames(String)` wraps as single-item array (backward compat) | TableMetadata.java | ProfileMetadataTest | implemented |
| `setInheritNames(String...)` stores multiple entries | TableMetadata.java | ProfileMetadataTest | implemented |
| `getInheritedTables()` returns `List<TableMetadata>` of ALL direct parents (no BLOCK-walking) | TableMetadata.java | ProfileMetadataTest | implemented |
| `getRootTable()` follows first parent chain to top | TableMetadata.java | — | implemented |
| `getIncludedBlockTables()` returns BLOCK entries from inheritNames (UX helper only) | TableMetadata.java | ProfileMetadataTest | implemented |
| `getSubclassTables()` finds transitive children (deduped via LinkedHashSet) | TableMetadata.java | ProfileMetadataTest | implemented |
| `isBlock()` checks `tableType == BLOCK` (UX hint only — backend NEVER branches on this for structural decisions) | TableMetadata.java | ProfileMetadataTest | implemented |
| `hasProfileColumnInAncestors()` on `TableMetadata` — single source of truth (not duplicated in Executor) | TableMetadata.java | ProfileMetadataTest | **needs fix** |
| Blocks behave identically to subtables at DB level | — | — | by design |
| `getProfileColumn()` finds PROFILE/PROFILES column | TableMetadata.java | ProfileMetadataTest | implemented |
| `setInheritNames("Person")` wraps as ["Person"] (backward compat) | TableMetadata.java | TestInherits | implemented |
| Multi-parent: `getColumns()` merges columns from ALL parents | TableMetadata.java | ProfileMetadataTest | implemented |
| Multi-parent: `insertBatch` recursively inserts into ALL parents (upsert for diamond) | SqlTable.java | TestSubtables | implemented |
| Multi-parent: FK created to EACH direct parent | SqlTableMetadataExecutor | TestSubtables | implemented |

### SQL Layer — Table Creation

| Behavior | Component | Test | Status |
|---|---|---|---|
| Inheritance style decision: `hasProfileColumnInAncestors(parent)` → no mg_tableclass; else → mg_tableclass | SqlTableMetadata, SqlTableMetadataExecutor | TestSubtables, TestInherits | **needs fix** (currently also checks `isBlock()`) |
| Subtable/block: PK + FK to parent (CASCADE), NO mg_tableclass (when parent has profile column) | SqlTableMetadataExecutor | TestSubtables | implemented |
| No profile column in ancestors → old-style extends WITH mg_tableclass (backward compat) | SqlTableMetadataExecutor | TestInherits | **needs fix** (fallback not explicit) |
| `executeSetInherit()` — single method with `addMgTableclass` parameter (no separate `executeSetSubtableInherit`) | SqlTableMetadataExecutor | TestSubtables, TestInherits | **needs fix** (currently two separate methods) |
| PROFILE column stored as varchar | SqlColumnExecutor | TestSubtables | implemented |
| PROFILES column stored as varchar[] | SqlColumnExecutor | TestSubtables | implemented |

### SQL Layer — Metadata Helpers

| Behavior | Component | Test | Status |
|---|---|---|---|
| `existsInAnyParent()` is instance method (not static with `tm` param) | SqlTableMetadata | — | **needs fix** |
| `setInheritNames` error identifies specific table not found (not whole list) | SqlTableMetadata | — | **needs fix** |
| `setInheritTransaction` documents why `inheritNames[0]` for old-style | SqlTableMetadata | — | **needs fix** |
| `SqlTable.getInheritedTables()` documents why SqlTable cast is needed | SqlTable | — | **needs fix** (add comment) |

### SQL Layer — Row Management

| Behavior | Component | Test | Status |
|---|---|---|---|
| Insert with profile="WGS" → rows in parent + WGS + included blocks | SqlTable | TestSubtables | implemented |
| Insert with profile=null → row in parent only | SqlTable | TestSubtables | implemented |
| Update profile WGS→Imaging → old subtable/block rows deleted, new created | SqlTable | TestSubtables | implemented |
| Delete parent → CASCADE deletes all child rows | SqlTable | TestSubtables | implemented |
| Invalid profile name → error | SqlTable | TestSubtables | implemented |
| PROFILES pick-many: rows in multiple subtables | SqlTable | TestSubtables | implemented |
| PROFILES remove entry → subtable row deleted | SqlTable | TestSubtables | implemented |
| Shared block: independent rows per parent row across subtables | SqlTable | TestSubtables | implemented |
| `handleSubtableRows` reuses existing insert path where possible | SqlTable | TestSubtables | **needs review** (currently manual upsert) |

### SQL Layer — Querying

| Behavior | Component | Test | Status |
|---|---|---|---|
| Query parent → LEFT JOINs all subtable+block child tables | SqlQuery | TestSubtables | implemented |
| Wide sparse result: subtable columns NULL for non-matching profiles | SqlQuery | TestSubtables | implemented |
| Filter on subtable column via parent query → works | SqlQuery | TestSubtables | implemented |
| Query subtable/block directly → subtable-specific + PK columns | SqlQuery | TestSubtables | implemented |
| `whereConditionSearch` uses `TableMetadata.getAllInheritedTables()` for ancestors | SqlQuery | TestSubtables | **needs fix** (currently custom recursive collector) |
| `tableWithInheritanceJoin` — keep minimal; simplify recursive helper if single-parent old-style | SqlQuery | TestSubtables | **needs review** |

### Metadata Persistence

| Behavior | Component | Test | Status |
|---|---|---|---|
| `inheritNames` persisted as varchar[] and round-trips | MetadataUtils | TestSubtables | implemented |
| Block stored with `table_type = BLOCK` in table_metadata | MetadataUtils | TestSubtables | implemented |
| Migration: `table_inherits` varchar → varchar[] | Migrations | TestSubtables | implemented |

### Backward Compatibility

| Behavior | Component | Test | Status |
|---|---|---|---|
| Existing TestInherits tests pass unchanged | all | TestInherits | implemented (needs test run) |
| Single-entry inheritName behaves like old inheritName | TableMetadata | TestInherits | implemented |
| mg_tableclass still works for regular extends (no profile column in ancestors) | SqlTableMetadataExecutor | TestInherits | implemented |
| No profile columns anywhere → exact same behavior as before (mg_tableclass fallback) | all | TestInherits | **needs explicit test** |

---

## Evolution from v1 to v8

| Aspect | v1–v3 | v4 | v5 | v6 | v7 | **v8** |
|--------|-------|-----|-----|-----|-----|--------|
| Column list | mapping / pseudo-columns | sequence | sequence in sections | sequence in sections | same | same |
| Naming | camelCase | camelCase | camelCase | sentence case | same | same |
| File scope | multi-table | multi-table | one table per file | same | same | same |
| Deploy config | — | — | — | single schema.yaml | separate schema files | **multi-schema + settings + permissions** |
| Imports | explicit | explicit | explicit | explicit | wildcard | wildcard |
| Sections | pseudo-column | pseudo-column | structural YAML | same | same | same |
| Block reuse | none | `- import:` | section/column level | same | same | same |
| Settings | — | — | — | — | — | **`settings:` inline or imported** |
| Permissions | — | — | — | — | — | **`permissions:` per schema** |
