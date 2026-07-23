# YAML model format

The YAML model format is a new bundle format for describing a MOLGENIS [schema](use_schema.md): tables,
columns, inheritance, composition, reuse and reference data model, in a diff-friendly, file-based shape.
It is the authoring format, the wire format of the model API (`GET`/`PUT /{schema}/api/model`, with
dry-run migration plans), and the mental model of the schema editor. It supersedes `molgenis.csv` as the
primary authoring format; [CSV import/export](use_schema.md) remains supported indefinitely, and existing
plain JSON/YAML schema export/import (see [Up/download](use_updownload.md#download)) is a separate,
older mechanism.

A bundle's top-level content targets **whatever schema it is loaded into** — a bundle never hardcodes a
schema name (it is a template, see [Flat root and template semantics](#flat-root-and-template-semantics)
below); only its `additionalSchemas:` companions carry a fixed name.

If you already know the CSV/Excel `molgenis` format, start with [Migrating from CSV to YAML](#migrating-from-csv-to-yaml).

?> **Worked examples in this page cross-check against the shipped test fixtures** under
[`backend/molgenis-emx2-io/src/test/resources/yamlbundle/`](https://github.com/molgenis/molgenis-emx2/tree/master/backend/molgenis-emx2-io/src/test/resources/yamlbundle)
— `reuse/` (imports + heading splice), `woven/` (subclass/module weaving), `axes/` (module discriminator
axis), `diamond/` (multi-parent extends), `refs/` (dotted cross-schema `refTable`), `singlefile/`
(single-file/inline form), `fullsurface/` (every attribute, with a CSV twin proving parity),
`diamondshowcase/` (diamond inheritance combined with a companion schema). Sections
below quote these fixtures directly where practical; the converted production corpus lives at
[`data/_models/yaml/`](https://github.com/molgenis/molgenis-emx2/tree/master/data/_models/yaml) (see its
`SKIPPED.md` for the models that don't map cleanly yet — [Profiles](#profiles) below).

## Bundle anatomy

A **bundle** is the unit of model distribution and apply: either a folder containing a `molgenis.yaml`
root plus table files, or an equivalent **single-file form**. A bundle is versioned and applied in one
transaction.

### molgenis.yaml (bundle root)

The root file declares the format/model version, which shared files and table files make up this
bundle, and any companion schemas:

```yaml
# molgenis.yaml — catalogue template bundle
formatVersion: 1
version: 3.2.0
imports: [shared/common.yaml]
tables:
  - tables/resources.yaml     # -> current schema (flat root)
additionalSchemas:                            # fixed-name companions (acyclic)
  CatalogueOntologies:
    bundle: catalogue-ontologies/molgenis.yaml
    permissions: {Viewer: anonymous}  # role-DEFAULTS allowed; member accounts never
settings: {menu: ...}
namespaces:
  dcterms: http://purl.org/dc/terms/
```

File paths (bare-string `tables:` entries, `imports:` entries, companion `bundle:` references) resolve
relative to the file that references them.

### Table files

Each table file describes one **hierarchy**: a root table plus, woven into the same file, the identity
subclasses and composition modules built on top of it (see [Weaving and heading scope](#weaving-and-heading-scope)).
By default a hierarchy lives in one file; splitting a hierarchy across multiple files is allowed (merge
order is deterministic — see below).

```yaml
# tables/resources.yaml — one file per root hierarchy
name: Resources
label: Resources
label@nl: Bronnen                   # one form: flat label@locale keys
profiles: [datacatalogue]
subclasses:
  - name: Cohorts
    extends: Resources              # scalar or list; order never matters
modules:
  - name: QuantitativeInfo
columns:
  - name: id
    key: 1
  - name: familyName
    previousNames: [surname, last_name]
  - contactDetails                  # splices heading + email + phone + orcid (all columns in the heading's positional scope), flat
  - name: sex
    type: enum
    values: [male, female, unknown]
  - name: cohort type               # woven subclass column
    subclass: Cohorts
    type: ontology_array
    refTable: CatalogueOntologies.Cohort types   # dotted = THE cross-schema form
  - name: subgroups
    type: module_array              # discriminator axis
    values: [QuantitativeInfo]
```

Shared files (like `shared/common.yaml` below) hold only a `columns:` list — no `name:`, because they
describe no table of their own, just reusable definitions:

```yaml
# shared/common.yaml — table-less shared file; same flat list shape
columns:
  - heading: contactDetails         # named heading = reuse unit
    visible: hasContact             # cascades to scoped columns
  - name: email
    type: email
  - name: phone
  - name: orcid
    type: string
```

This `contactDetails` reuse pattern is exactly what the [`reuse`
fixture](https://github.com/molgenis/molgenis-emx2/tree/master/backend/molgenis-emx2-io/src/test/resources/yamlbundle/reuse)
exercises end to end (`Emx2YamlTest#headingSplice`).

### Single-file form

The whole bundle can also be written as one file: `tables:` entries carry the table object directly
(same keys as a table file — `name`, `label`, `columns`, its own `imports:`, …) instead of a bare-string
file path. Both forms parse identically, including a table-level `imports:` resolving inside the inlined
form (`Emx2YamlTest#singleFileEquivalence`, [`singlefile`
fixture](https://github.com/molgenis/molgenis-emx2/tree/master/backend/molgenis-emx2-io/src/test/resources/yamlbundle/singlefile)):

```yaml
# molgenis.yaml — single-file form
formatVersion: 1
version: 1.0.0
tables:
  - name: People
    imports: [shared/common.yaml]
    columns:
      - name: id
        key: 1
      - contactDetails
```

This authoring single-file form may still reference a shared file (`imports: [shared/common.yaml]`) or a
companion by `bundle:` pointer, because it is still resolved together with the files next to it on disk.
**The model API's wire form is stricter than this** — see [Model API](#model-api) below.

### Flat root and template semantics

The bundle's top-level content (its `tables:` outside of `additionalSchemas:`) is the **flat root**: it applies to
whatever schema it is loaded into. The bundle does not hardcode a schema name, so the same bundle is a
template — it can be applied to N differently-named schema instances (e.g. one catalogue template
producing many named catalogues).

### additionalSchemas: companions in the root

`additionalSchemas:` declares **companion** schemas: fixed-name, shared schemas that a bundle depends on but does
not own. See [Companion schemas](#companion-schemas) below for full semantics.

### namespaces

Bundle-root `namespaces:` declares CURIE prefixes (e.g. `dcterms: http://purl.org/dc/terms/`) available
to `semantics:` fields in this bundle, in addition to or overriding the [built-in defaults](semantics.md#defined-namespaces).

## Full attribute reference

Strict parsing rejects any key outside this enumerated surface, reporting the file and YAML path. This
surface is also published as a build-time-generated [JSON
Schema](https://github.com/molgenis/molgenis-emx2/blob/master/backend/molgenis-emx2-io/src/main/resources/molgenis-model.schema.json)
for editor/tooling validation, generated from the same validator rules as this table — the build fails if
the two ever drift.

### Bundle-level attributes

| Attribute | Meaning | Example |
|---|---|---|
| `formatVersion` | Format-spec revision this bundle is authored against; the parser refuses a bundle newer than it supports. | `formatVersion: 1` |
| `version` | The model's own version; stamped on the schema at apply. Must be strict numeric `MAJOR.MINOR.PATCH` — any other shape is a validation error. | `version: 3.2.0` |
| `imports` | Bundle-wide shared files whose named columns/headings become referenceable by bare string anywhere in the bundle (shadowed by a table-level `imports:` of the same name). | `imports: [shared/common.yaml]` |
| `tables` | This bundle's table files — one entry per hierarchy (root + its woven subclasses/modules), each a bare-string file path. Single-file form inlines the table object in place of the path string. | `tables: [tables/resources.yaml]` |
| `additionalSchemas` | Fixed-name companion schemas, inline or by `bundle:` reference, with optional role-default `permissions:`. | `additionalSchemas: {CatalogueOntologies: {bundle: catalogue-ontologies/molgenis.yaml}}` |
| `settings` | Free-form key/value settings map for the schema this bundle applies to (e.g. menu). | `settings: {menu: ...}` |
| `namespaces` | CURIE prefixes for `semantics:` fields, in addition to/overriding the built-in defaults. | `namespaces: {dcterms: http://purl.org/dc/terms/}` |

### Table-level attributes

Applies to a table file's top-level keys, and to each entry of `tables:`, `subclasses:`, `modules:`.

| Attribute | Meaning | Example |
|---|---|---|
| `name` | The table's identifier; unique per schema. | `name: Resources` |
| `extends` | Parent table name(s) as a scalar or list — one entry = single inheritance, several = diamond inheritance. The list is an unordered set (order never affects the result), so multi-parent parents always share one hierarchy root. A `subclasses:`/`modules:` entry may omit `extends:` when the parent is simply this hierarchy file's own root. | `extends: Resources` |
| `tableType` | Surface value `data` (default, omitted), `ontology` (reference-data table) or `module` (composition table), case-insensitive. A `subclasses:` entry defaults to `data`, a `modules:` entry to `module`, so `tableType:` is only spelled out to override. | `tableType: module` |
| `subclasses` | Identity subtables woven into this hierarchy file; their columns join via a `subclass:` marker (see [Weaving](#weaving-and-heading-scope)). | `subclasses: [{name: Cohorts, extends: [Resources]}]` |
| `modules` | Composition subtables (MODULE tables) woven into this hierarchy file; their columns join via a `module:` marker. | `modules: [{name: QuantitativeInfo}]` |
| `settings` | Free-form key/value settings map scoped to this table (mirrors bundle-level `settings:`; the format does not mandate specific keys). | `settings: {}` |
| `profiles` | Tags the whole table into one or more authoring-time [profile](#profiles) slices — legal at table level as well as column level, as in CSV. | `profiles: [datacatalogue]` |
| `columns` | The table's flat, ordered column list. | `columns: [{name: id, key: 1}]` |

### Column-level attributes

Entries in a `columns:` list are either a map (a column, or a `heading:`/`section:` layout entry) or a
bare string (a [reference](#reuse-and-imports) to a shared column/heading).

| Attribute | Meaning | Example |
|---|---|---|
| `name` | The column's identifier; unique per table. Not itself part of the enumerated attribute surface, but the map key that makes an entry "a column" (as opposed to `heading:`/`section:`, or a bare-string reference). | `name: familyName` |
| `type` | The column's data type (`string`, `int`, `ref`, `ontology_array`, `enum`, `module_array`, `heading`, `section`, … — same type vocabulary as CSV). | `type: enum` |
| `key` | Marks the column part of a key; the same key number on several columns forms one composite key. | `key: 1` |
| `required` | Must be filled; a boolean or a JS expression. | `required: true` |
| `readonly` | Not writable via forms/API once set (e.g. system-managed columns). | `readonly: true` |
| `refTable` | Target table of a reference column; dotted `Schema.Table` is the one cross-schema form, a bare name means same-schema. | `refTable: CatalogueOntologies.Cohort types` |
| `refLink` | For a reference into a table with a composite key, names the sibling reference column on *this* table that already covers part of that composite key, so the shared part need not be repeated. See the [worked example](#worked-example-reflink-with-composite-keys). | `refLink: patient` |
| `refLabel` | Expression used to render the reference in the UI instead of the bare key. | `refLabel: ${firstName} ${lastName}` |
| `refBack` | Pairs with `type: refback`; names the ref/ref_array column on `refTable` that points back to this table. | `refBack: owner` |
| `defaultValue` | Pre-filled/inserted value when none is given; a literal or an `=`-prefixed JS expression. | `defaultValue: "=new Date().toISOString()"` |
| `validation` | JS expression; must return `null`/`true` or the record is rejected. | `validation: price > 1` |
| `visible` | JS expression controlling whether the field is shown. On a heading/section, it cascades to every column it scopes. | `visible: hasContact` |
| `computed` | JS expression computed server-side before insert/update; read-only in the UI. | `computed: productNo + "_" + partNo` |
| `semantics` | RDF predicate(s) for this column (IRI or CURIE); also usable table-level, where it sets the table's RDF class/type. | `semantics: dcterms:title` |
| `formLabel` | Label shown only in the context of a data-entry form (distinct from the display `label`). | `formLabel: Family name` |
| `label`, `label@locale` | Display label; `label:` is the default-locale value, one flat `label@xx:` key per additional locale. | `label@nl: Achternaam` |
| `description`, `description@locale` | Longer explanatory text; same shape as `label`. | `description@nl: Achternaam van de deelnemer` |
| `profiles` | Tags this item into one or more authoring-time [profile](#profiles) slices. Legal at column level *and* table level (see [Profiles](#profiles) below). | `profiles: [datacatalogue]` |
| `previousNames` | Ordered list of former names, enabling rename inference across version gaps (see [previousNames](#previousnames-versioning-and-downgrade)). | `previousNames: [surname, last_name]` |
| `values` | Allowed literal set for `enum`/`enum_array`; or allowed MODULE table names for a `module_array` discriminator axis — always **bare, same-schema table names**, even when the module table lives in a schema referenced elsewhere via a dotted `refTable`. | `values: [male, female, unknown]` |
| `subclass` | Assigns this woven column to the named subclass table instead of the hierarchy root. | `subclass: Cohorts` |
| `module` | Assigns this woven column to the named MODULE table instead of the hierarchy root. | `module: QuantitativeInfo` |

## Reuse and imports

`imports:` (bundle-level or table-level) makes the named columns and headings of the listed shared files
referenceable, by bare string, anywhere the import is in scope:

- **Bundle-level `imports:`** (on `molgenis.yaml`) makes its shared files referenceable from any table
  file in the bundle.
- **Table-level `imports:`** (on a table file, or an inlined table entry in single-file form) scopes
  shared files to that one table, and **shadows** a bundle-level import of the same name — resolution is
  nearest-scope first.
- **Bare-string references**: a plain string entry in a `columns:` list (e.g. `- contactDetails`)
  resolves against the in-scope imports. Ambiguity between two same-scope candidates is an error;
  an unresolvable bare string is an error.
- **Named-heading splice**: referencing a shared heading's name splices that heading *and* the columns
  it scopes, flat, at the point of reference — its `visible:` cascade travels with it. In the reference
  example, `- contactDetails` in `tables/resources.yaml` splices in the `contactDetails` heading plus
  `email`, `phone` and `orcid` from `shared/common.yaml`.
- **Reuse-or-define, no refinement**: a reference always imports the shared definition verbatim. You
  cannot attach extra attributes at the use site to tweak it — that is a validation error. If you need a
  column with different attributes, don't reference the shared one; define your own column under its own
  name. A **local** definition whose name collides with an in-scope shared name is also an error — names
  must be unique within the resolving scope, whether local or imported.

## Weaving and heading scope

A hierarchy file weaves the root table together with its identity subclasses and composition modules
into **one** ordered flat `columns:` list.

- **`subclasses:` / `modules:` blocks** declare, per hierarchy file, which subtables participate: each
  `subclasses:` entry has its own `name`/`extends` (subclass = identity, `mg_tableclass`-discriminated);
  each `modules:` entry has its own `name` (module = composition, `tableType: module`, no
  `mg_tableclass`).
- **`subclass:` / `module:` column markers** assign a woven column to one of those declared names instead
  of the root. A column carrying neither marker belongs to the root.
- **File order = form order**: root columns, subclass-marked columns and module-marked columns can
  interleave freely in the same list — the position in the file *is* the compiled `Column.position`.
  This is what keeps the flat list diff-friendly: moving a column is a one-line change, never a
  restructuring.
- **Heading, two levels**: `- section:` starts a new top-level, page-like grouping; `- heading:` starts a
  subheader within the current section (or at the top level if no section is open). Both are layout-only
  entries — not stored as data columns — and scope every column positioned after them, up to the next
  heading/section entry of the same or higher level.
- **Positional scope**: a heading's scope is simply "the columns between this layout entry and the next
  one" — there is no separate nesting/grouping key. Moving a column in or out of a heading's scope means
  moving its line before or after the heading's line.
- **Visible cascade**: a `visible:` expression set on a heading/section applies to every column it
  currently scopes, in addition to any `visible:` a column sets for itself.
- **Cross-file / diamond merge order** (when a hierarchy is split across files, or a subclass has
  multiple parents): `extends:` is an unordered set — its order never affects the result. Ancestors are
  merged before their descendants (topological order), ties are broken by the bundle `tables:`
  declaration order, and each table's own woven list comes last. Multi-parent parents always share the
  same hierarchy root, so the inherited key comes from that shared root regardless of parent order —
  deterministic and stable across repeated parses and across any `extends:` permutation.

## previousNames, versioning and downgrade

- **`previousNames`** is an ordered list of a column's former names. The diff engine matches an older
  live name to infer a RENAME (instead of DROP+ADD) across version gaps — but only when that old name is
  absent from the desired state.
- **Live-name collision rule**: if a chain name is both still live in the current schema *and* still
  desired in the bundle, the dry-run refuses to guess and reports an error demanding explicit
  resolution — it never silently drops or mis-renames.
- **`version`** is stamped on the schema at apply time; a dry-run leaves it unchanged. It must be strict
  numeric `MAJOR.MINOR.PATCH` (e.g. `3.2.0`) — any other shape is a validation error; versions compare
  numerically, segment by segment.
- **Downgrade**: applying a bundle whose `version` is older than the schema's current one is a plain
  refusal (400) — there is no override flag. The only way forward is bumping `version:` in the bundle (or
  applying it to a fresh schema). A dry-run always shows the destructive diff before you decide.
- **`formatVersion` skew**: a bundle whose `formatVersion` is newer than the parser supports fails
  loudly before any side effects.

## Apply is additive

Applying a bundle is not "make the schema look exactly like this file" — it is **a series of changes**
layered on top of whatever the schema already has:

- **Absence means leave alone.** A column or table that simply isn't in the bundle is not touched — not
  dropped, not renamed, not altered. A bundle can describe just the tables/columns it cares about without
  re-declaring an entire schema's worth of unrelated structure.
- **Deletion is explicit.** The only way to remove a column or table is to mark its entry `drop: true`:

```yaml
columns:
  - name: legacyStatus
    drop: true
```

```yaml
tables:
  - name: LegacyLookup
    drop: true
```

- **Dry-run reflects exactly the marked drops.** `plan.columnDrops`/`plan.tableDrops` (see [Model
  API](#model-api) below) list only entries the bundle marks `drop: true` — never everything that happens
  to be missing from the bundle.

!> `drop: true` matches the schema's **current live name**, not any `previousNames` entry. If the live
column still carries an old name, either rename it first (a `previousNames:` entry migrates it forward,
without `drop:`) and drop it in a later apply, or mark that old name itself as the one to drop.

## Companion schemas

A companion is a **fixed-name**, shared schema a bundle depends on without owning:

- **Fixed-name**: the key under `additionalSchemas:` (e.g. `CatalogueOntologies`) is the companion's actual schema
  name — unlike the flat root, it is not templated per bundle instance.
- **Provision-if-absent**: the companion is created only if it does not already exist. A second apply of
  a bundle referencing an existing companion leaves that companion untouched; a dry-run warns if the
  existing companion is older than the one referenced.
- **Role-default permissions**: the optional `permissions:` map sets default grants for standard [roles](use_permissions.md)
  when the companion is first provisioned — the keys are exact-match role names, spelled and cased exactly
  as the UI shows them: `Exists`, `Range`, `Aggregator`, `Count`, `Viewer`, `Editor`, `Manager`, `Owner`
  (lowercase spellings like `view`/`edit`/`manage`/`own` are rejected as unknown keys). E.g.
  `{Viewer: anonymous}` grants default view access to the anonymous role — it can never carry a specific
  member or user account.
- **Acyclic**: companion references must not cycle; a cycle is a validation error.
- Declared either by `bundle:` reference (pointing at another bundle's `molgenis.yaml`) or inline, using
  the same shape as the bundle root:

```yaml
additionalSchemas:
  Lookups:
    tables:
      - tables/lookups.yaml
    permissions: {Viewer: anonymous}
```

Dotted `refTable: Schema.Table` references resolve companion-before-instance.

## Model API

`GET`/`PUT /{schema}/api/model` expose this same format as a schema's live model, using the same parser
and validator as file-based bundles.

### GET — export

`GET /{schema}/api/model` returns the schema's current model in the [single-file
form](#single-file-form) — one YAML document with `formatVersion`/`version` and inlined `tables:`. There
is no folder/zip response variant, and (per [Ontologies](#ontologies) below) ontology tables appear only
as metadata-only stubs — never their term rows.

### PUT — apply and dry-run

`PUT /{schema}/api/model` takes one YAML document as the request body and diffs it, server-side, against
the schema's live model (using `previousNames` chains to infer renames, as above).

- **The wire form is single-file, and companions must be INLINED.** The request body is one document
  with no accompanying file tree, so a companion under `additionalSchemas:` cannot use a `bundle:` reference on the
  wire — `bundle:` stays an authoring-only convenience for the folder form. On `PUT`, a companion carries
  its content inline, in the same shape as the bundle root (`tables:`, optional `version:`, optional
  `permissions:`):

```yaml
formatVersion: 1
version: 1.0.0
tables:
  - name: Cohorts
    columns:
      - name: id
        key: 1
      - name: cohortType
        type: ref
        refTable: CatalogueOntologies.Cohort types
additionalSchemas:
  CatalogueOntologies:
    version: 1.0.0
    permissions:
      Viewer: anonymous
    tables:
      - name: Cohort types
        columns:
          - name: id
            key: 1
```

- **`?dryRun=true`** computes and returns the migration plan without applying it: a JSON body with
  `plan.tableAdds`/`tableDrops`/`tableRenames`, `plan.columnAdds`/`columnDrops`/`columnRenames`,
  `plan.changes` (per-attribute before/after), and `plan.errors`/`plan.warnings` (e.g. a companion whose
  stored version is older than the one referenced). Nothing is written; the stored `version` is left
  unchanged. Because [apply is additive](#apply-is-additive), `plan.tableDrops`/`plan.columnDrops` list
  only entries the bundle marks `drop: true` — never everything merely absent from the bundle.
- **Downgrade refusal**: `PUT`-ing a bundle whose `version` is older than the schema's stored version
  fails (400) and nothing changes — there is no override flag. Bump `version:` in the bundle to proceed
  (see [previousNames, versioning and downgrade](#previousnames-versioning-and-downgrade)).
- **Version stamping**: a successful (non-dry-run) apply stores the bundle's `version:` on the schema —
  the next `PUT`'s downgrade check and previousNames-chain resolution compare against it, and the next
  `GET` reports it back.
- **Atomicity**: apply runs in one transaction across the target schema and any companions it provisions
  — on failure nothing persists, and the error names which schema failed.

## Ontologies

For `ontology`/`ontology_array` columns, `refTable` is auto-created if it does not already exist in the
target schema, exactly like the [CSV behavior](use_schema.md#ontologies). An ontology table is exported as
a **metadata-only stub** — its `name`, `tableType: ontology`, and any `description`, `label`,
`semantics` and `profiles` — but never its term rows and never its engine-defined columns, so ontology
*metadata* round-trips while terms stay data, not model (`Emx2YamlTest#ontologyTablesExportAsStubs`). On
import a declared stub merges cleanly with the auto-created table (no duplicate). Cross-schema ontologies
use the same dotted `refTable: Schema.Table` form as any other reference; same-schema ontologies use a bare
table name. From the shipped [`refs`
fixture](https://github.com/molgenis/molgenis-emx2/blob/master/backend/molgenis-emx2-io/src/test/resources/yamlbundle/refs/tables/Cohorts.yaml):

```yaml
# tables/Cohorts.yaml
name: Cohorts
columns:
  - name: id
    key: 1
  - name: cohortType
    type: ontology_array
    refTable: CatalogueOntologies.Cohort types   # cross-schema companion ontology
  - name: keywords
    type: ontology_array
    refTable: Keywords                            # same-schema, auto-created on import
```

Importing this bundle auto-creates a same-schema `Keywords` ontology table (empty — its terms are loaded
as data, e.g. via CSV upload); exporting the schema afterwards emits `Keywords` as a metadata-only stub
(`name`, `tableType: ontology`, plus any `description`/`semantics`/`profiles`) — never its term rows.

## Profiles

`profiles:` tags a table or column into one or more authoring-time slices (e.g. `profiles: [datacatalogue]`),
matching the `profileTags` a [profile YAML file](dev_profiles.md) (`data/_profiles/*.yaml`) uses to select
which parts of a shared model end up in a given deployment's schema.

- **Authoring-time, not runtime**: slicing happens when a profile builds a schema from the shared model —
  a schema built from a profile simply does not contain the untagged tables/columns. There is no
  per-request "profile mode" toggle at query time; once applied, a schema has no memory of `profiles:`.
- **Tag, don't gate**: `profiles:` only tags an item for the slicing step; it never changes a column's own
  type, requiredness or any other attribute.
- Table-level and column-level `profiles:` compose: a column only survives slicing if both its table and
  (when tagged) the column itself pass the profile's tag filter.

?> **v1 limitation — profile-conditional table structure.** Some CSV models encode *per-profile variants
of the same table* — multiple definition rows for one table name, e.g. a different `description` or
`extends` per tag. The YAML surface allows exactly one definition (one `extends`) per table name, so this
pattern isn't expressible yet; converting such a table would lose that variation. The [corpus
reconversion](https://github.com/molgenis/molgenis-emx2/tree/master/data/_models/yaml) skips these models
explicitly rather than convert them lossily (see its
[`SKIPPED.md`](https://github.com/molgenis/molgenis-emx2/blob/master/data/_models/yaml/SKIPPED.md) — e.g.
`Materials`, `Processes`, `Profiles`, `Collections`). A format-level answer is parked as its own line
(`2026-07-23-profile-conditional-tables`).

## Reserved keys

These names are reserved for future use — they are **not yet part of the parsed attribute surface**.
Using one today is rejected the same way any unrecognized key is (unknown-key error naming file and
path). The reservation is a promise: when a key does land, it will carry the meaning below (where one is
already decided); don't repurpose these names for anything else in the meantime.

| Key | Scope | Status |
|---|---|---|
| `migrations/` | bundle folder | Reserved for migration execution; not implemented this line. |
| `apps:` | bundle-level | Reserved; meaning defined by a follow-up line (app-package bundles). |
| `scripts:` | bundle-level | Reserved; same follow-up line. |
| `services:` | bundle-level | Reserved; same follow-up line. |
| `columnOrder:` | table-level | Reserved to let a (sub)class reorder its *effective* column set (inherited + own) subclass-side, so a superclass never has to know its subclasses — e.g. a diamond subclass wanting its own field order across two parents' columns without either parent file changing. Spec'd, not implemented this line. |
| `since:` | column/table-level | Reserved; documentation-only if it is ever introduced (not enforced by the engine). |

## Worked example: refLink with composite keys

`refLink` simplifies a design where a composite key indirectly refers to the same table through
multiple layers — the same `observations`/`samples`/`patients` case from the [CSV schema
docs](use_schema.md#reflink), expressed as a four-file bundle:

```yaml
# molgenis.yaml
formatVersion: 1
version: 1.0.0
tables:
  - tables/patients.yaml
  - tables/samples.yaml
  - tables/observations.yaml
```

```yaml
# tables/patients.yaml
name: patients
columns:
  - name: id
    key: 1
```

```yaml
# tables/samples.yaml
name: samples
columns:
  - name: patient
    key: 1
    type: ref
    refTable: patients
  - name: id
    key: 1
```

```yaml
# tables/observations.yaml
name: observations
columns:
  - name: patient
    key: 1
    type: ref
    refTable: patients
  - name: sample
    key: 1
    type: ref
    refTable: samples
    refLink: patient
```

`samples` has a composite key (`patient` + `id`) that itself references `patients`. Without `refLink`,
`observations` would indirectly reference `patients` twice — once via its own `patient` column, once via
`sample`'s composite key — and entering an observation would mean repeating the same patient reference
twice. `refLink: patient` on `observations.sample` tells the engine that the `patient` portion of
`sample`'s composite key is identical to `observations.patient` itself, so during data processing only
`sample`'s remaining key part (`id`) needs to be supplied, through the now-simplified `sample` column:

| column | without `refLink` | with `refLink` | explanation |
|---|---|---|---|
| `patient` | `patient` | `patient` | stays identical |
| `sample` | `sample.patient` | *(omitted)* | `refLink` makes this unnecessary |
| `sample` | `sample.id` | `sample` | column name simplifies — only one remaining column to define |

`samples` and `patients` are unaffected by `refLink`; it only simplifies `observations`.

An example data payload for `observations` makes this concrete. Without `refLink`, inserting an
observation repeats the patient reference across two data columns:

```csv
patient,sample.patient,sample.id
P1,P1,S1
```

With `refLink: patient` declared above, the same observation is entered with the patient supplied once:

```csv
patient,sample
P1,S1
```

## Migrating from CSV to YAML

| CSV (`molgenis.csv`) | YAML | Notes |
|---|---|---|
| Row order determines column position | `columns:` list order | Position is now literally the line order — moving a column is a one-line diff. |
| Comma-separated cell (`values=male,female,unknown`, `tableExtends=Parent1,Parent2`) | Native YAML list (`values: [male, female, unknown]`, `extends: [Parent1, Parent2]`) | No more escaping/quoting commas inside a metadata cell. |
| `label:nl`, `description:fr` column headers | Flat `label@nl:`, `description@fr:` keys next to the default `label:`/`description:` | Same one-value-per-locale idea, different key spelling. |
| Separate `refSchema` + `refTable` cells | One dotted `refTable: Schema.Table` | Same-schema references stay a bare table name; `refSchema` is retired from the YAML surface. |
| `tableExtends` | `extends:` (scalar or list) | An unordered set of parents; order never matters, so the export normalises to alphabetical. |
| A `tableType=MODULE` table row | A `modules:` block entry on the hierarchy file, its columns marked `module: <Name>` | The module's columns are woven into the same file instead of a separate table block. |
| A subclass table row (`tableExtends` naming a non-module parent) | A `subclasses:` block entry, its columns marked `subclass: <Name>` | The whole hierarchy — root, subclasses, modules — lives in one woven file. |
| A blank-`columnName` row setting table-level metadata | The table file's own top-level keys (`name:`, `description:`, `settings:`, …) | No more "blank row = table row" convention. |
| `heading`/`section` columnType rows | `- heading:` / `- section:` layout entries in `columns:` | Same two-level semantics. |
| `oldName` migration directive | `previousNames:` (list) | `oldName` was a one-shot instruction consumed by a single upload; `previousNames` is a persistent, versioned chain the diff engine consults on every apply, and supports rename **chains** across multiple versions. |
| `drop` migration flag | `drop: true` on the column/table entry | Explicit, not implied by absence — see [Apply is additive](#apply-is-additive). A column/table simply absent from the bundle is left untouched. |

### Step by step, on a real model

The [`fullsurface`
fixture](https://github.com/molgenis/molgenis-emx2/tree/master/backend/molgenis-emx2-io/src/test/resources/yamlbundle/fullsurface)
carries a `molgenis.csv` and its `molgenis.yaml`/`tables/*.yaml` twin, asserted to parse to the exact same
model (`Emx2YamlTest#fullSurfaceCsvParity`). Three of its tables — root `Resources`, its `Cohorts`
subclass, and a standalone `Contacts` table with a `refback` — walk through every migration-table row
above:

| tableName | tableExtends | tableType | columnName | key | label | label:nl |
|---|---|---|---|---|---|---|
| Resources | | | *(blank — table-meta row)* | | Resources | Bronnen |
| Resources | | | id | 1 | | |
| Resources | | | familyName | | Family name | Achternaam |
| Cohorts | Resources | DATA | *(blank — table-meta row)* | | | |
| Cohorts | | | cohortType | | | |
| Contacts | | DATA | *(blank — table-meta row)* | | Contacts | |
| Contacts | | | contactId | 1 | | |
| Contacts | | | contactName | | | |
| Contacts | | | linkedResources (`refback` → Resources.contact) | | | |

1. **One file per hierarchy, not per table.** `Resources` and its subclass `Cohorts` become one file
   (`tables/Resources.yaml`, `Cohorts` woven in via a `subclasses:` entry); `Contacts` has no
   subclasses/modules of its own, so it gets its own file.
2. **A blank-`columnName` table-meta row becomes the file's top-level keys** (`name:`, `label:`,
   `label@nl:`, …).
3. **Column rows become one flat, ordered `columns:` list**, in file order; a column belonging to a woven
   subclass (`cohortType`) gets a `subclass: <Name>` marker instead of moving to a separate table block.
4. **`label:nl` becomes `label@nl`** next to the default `label:`.

The real result (indentation preserved from the shipped fixture; `Resources.yaml` is trimmed to the
columns this walkthrough covers — the full file also carries `Networks`/`CohortNetwork` subclasses and a
`QuantitativeInfo`/`QualitativeInfo` module axis, see the fixture link above):

```yaml
# tables/Resources.yaml — trimmed
name: Resources
label: Resources
label@nl: Bronnen
subclasses:
- name: Cohorts
columns:
- section: identity
- name: id
  key: 1
- name: familyName
  label: Family name
  label@nl: Achternaam
- name: cohortType
  subclass: Cohorts
```

```yaml
# tables/Contacts.yaml — in full
name: Contacts
label: Contacts
description: People and organisations
columns:
- name: contactId
  key: 1
- name: contactName
- name: linkedResources
  type: refback
  refTable: Resources
  refBack: contact
```
