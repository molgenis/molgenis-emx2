# Spec: Modular Data Modeling — Naming, Subsets, and Templates

## Scope

Terse behavioral contract for the parser, API, and DDL layer. For authoring guidance and worked examples, see [`docs/molgenis/yaml_format.md`](../../docs/molgenis/yaml_format.md).

---

## Naming Conventions

| Thing | Convention | Example |
|---|---|---|
| Table name | Uppercase start (sentence case) | `Sample types` |
| Column name | Lowercase start (sentence case) | `sample type` |
| Section name | Uppercase start | `Common fields` |
| Subtype name | Uppercase start | `Whole genome sequencing` |
| Internal subtype | Lowercase start (convention) | `sampling`, `quality` |
| Subset / template identifier | `[a-z][a-z0-9_]*` (snake_case) | `wgs`, `catalogue_core` |

Sentence-case names serve as display labels by default. An explicit `label` is only needed when the display text must differ from the name. Subset identifiers are technical — not display labels.

---

## Architectural Layers

### 1. Tables

Foundational entities (`TableType.DATA`). Define identity and primary structure. Tables are created in PostgreSQL only when their subset tag (if any) is in the active set, or when they carry no `subsets:` tag (always-on).

### 2. Subtypes (Composable Capabilities)

Subtables using `extendsTables` (Java) / `inherits` (YAML). Share parent table's PK (1:1, FK with ON UPDATE CASCADE, ON DELETE CASCADE). Subject to the same subset-driven DDL rules as tables.

**Internal subtypes** (`internal: true`, `TableType.INTERNAL`) group shared columns across user-selectable subtypes. Not shown in the subtype selector dropdown.

### 3. Subsets (DDL Gate + UX Layer)

Subsets determine which tables and columns are **physically created in PostgreSQL**. This is not a visibility filter — it is a DDL gate.

- Column with `subsets: [x]`: created only when `x` is in the active subset set.
- Column with no `subsets:`: always created (always-on).
- Table with `subsets: [x]`: entire table created only when `x` is active.

**Activating** a subset is additive and idempotent (`ADD COLUMN IF NOT EXISTS`).
**Deactivating** a subset drops tables/columns exclusive to that subset. Requires `confirm: true` if any of those columns contain data. Never silent.

**Ontology tables: conditional drop on deactivate.** On deactivate, walk all FK references to the ontology table across all schemas (including cross-schema references on the same instance). Drop only if zero references remain. Skip silently if any FK still references it — no error. On activate: add-if-missing (unchanged).

### 4. Sections (UI Grouping)

Purely organizational. No structural impact on DDL. Can be scoped to a subtype via `subtype: X` — all columns in that section then belong to subtype X.

### 5. Templates (Deployment Bundles)

User-facing named presets of subset activations. Shown in the admin picker. Carry optional deployment config (`settings:`, `permissions:`, `fixedSchemas:`). A template activates `{itself} ∪ transitive(includes)`.

---

## API Contract

- The API shape depends on the active subset set. Columns and tables not in the active set are absent from PostgreSQL and therefore absent from all API responses.
- `activateSubset(name)` — additive, safe, no confirmation required.
- `deactivateSubset(name, confirm: Boolean)` — destructive. Fails if data exists in columns to be dropped unless `confirm: true`.
- The legacy `activeProfiles`/`applyProfileFilter` API is superseded by the split mutations above. It remains during transition and will be removed.

---

## Validation Rules (parser must enforce)

| Rule | Error behavior |
|---|---|
| Every name in `subsets:` on a column/table must exist in the bundle's subset or template registry | Hard error at load time, naming the unknown identifier |
| Names must be unique across `subsets:` and `templates:` sections | Hard error |
| `includes:` must be acyclic | Hard error, naming the cycle |
| `[a-z][a-z0-9_]*` for subset/template names | Hard error |
| Reference completeness: if column A (subset X) has `refTable: B` (subset Y only), Y must be active | Hard error at load time, naming the missing subset |
| `includes:` completeness: if Z is active and Z `includes: [Y]`, Y must be resolvable | Hard error at load time |
| Column names must be unique table-wide (walk full nested `columns:` tree) | Hard error, naming the duplicate |
| The key `columns` cannot be used as a column, section, or heading name | Hard error |
| Nesting depth > 2 (heading contains `columns:`) | Hard error |
| `semantics:` declared on a section or heading entry | Hard error |

---

## Bundle Contract

A bundle takes one of two forms:

- **Single-file bundle**: `data/templates/<name>.yaml` — one file with a top-level `name:` and inline `tables:`. No directory required.
- **Directory bundle**: `data/templates/<bundle>/molgenis.yaml` — directory with `molgenis.yaml` at root, supporting files alongside.

Both forms MUST declare `name:`. `description:` is recommended but not required. A bundle must have at least one table, either inline (`tables:` keyed map) or via `imports:` list of file/directory paths.

Single-file bundles MUST NOT declare `ontologies:`, `demodata:`, `settings:`, or `migrations:`. Those require the directory form.

The optional `namespaces:` key at the bundle root declares custom prefix-to-URI mappings used when expanding short-form CURIEs in `semantics:` annotations. Bundle-declared prefixes override built-in ones on conflict.

`subsets:` and `templates:` are **keyed maps** — key is the identifier, value has `description:` and optional `includes:`. They share one identifier namespace. `includes:` and column `subsets:` tags may reference names from either section.

Activating a template activates `{template_name} ∪ transitive(includes)`. A column is created iff at least one of its `subsets:` values is in the active set, or it has no `subsets:` tag.

### Column map rules

`columns:` at table level is a **keyed map**. Keys are column names. Rules for inferring entry type from structure:

| Entry has `columns:` sub-key? | Depth | Resolved as |
|---|---|---|
| Yes | 0 (under table's `columns:`) | Section |
| Yes | 1 (inside a section's `columns:`) | Heading |
| No, and `type: heading` | any | Decorative heading (UI label only) |
| No, and no `type: heading` | any | Data column |

- **Max nesting depth is 2**: table → section → heading → columns. A heading cannot contain further nested `columns:`. Anything deeper is a parse error.
- **Table-wide column name uniqueness**: column names must be unique across the root `columns:` map and all nested section/heading `columns:` maps. The parser walks the full tree on load.
- **Reserved name**: the key `columns` cannot be used as a column, section, or heading name. Parse error if encountered.

### Section/heading attribute hoisting

Sections and headings may carry `subtype:` and `subsets:` that are inherited by all nested columns. A nested column can override with its own declaration.

- `subtype:` — **inherited**. Scopes all nested columns to the named child table.
- `subsets:` — **inherited**. Tags all nested columns with the listed subsets.
- `semantics:` — **never inherited**. Setting `semantics:` on a section or heading is a parse error. Must be set per column.
- All other column-level attributes (`type:`, `description:`, `required:`, `key:`, `refTable:`, etc.) are per-entry and do not inherit.

See [`yaml_format.md`](../../docs/molgenis/yaml_format.md) for the full key reference and worked examples.

---

## Subtype Lifecycle

- A subtype is created when its subset is activated (or on first deploy if no subset tag).
- A subtype is dropped when deactivated, subject to the same confirmation rules as columns.
- Diamond inheritance (multiple `inherits:`) resolved via upsert.

---

## Schema Editor Rule

On a schema backed by a `molgenis.yaml` bundle:
- The schema editor is **read-only for structure**.
- Subset activate/deactivate goes through `activateSubset`/`deactivateSubset` mutations only.

On a schema created ad-hoc via the editor (no bundle backing):
- No subset concept. The editor is the authoring path.

Do not mix the two paths on the same schema.

---

## Custom Migrations Hook

`migrations/<subset_name>/` in the bundle directory. Scripts run once when the subset is first activated on a given schema. Not every subset needs this directory. Full framework details are out of scope here.

---

## Semantics

`semantics:` URIs travel with the column definition regardless of which subsets are active. They are stable across deployments that activate the same subset. Physical subsetting does not break semantic harmonisation.

Semantic URIs (`http://...`, CURIEs) must NOT appear in `subsets:` arrays. That was a bug in the old format.

---

## Comparison with Standards

| Our concept | FHIR equivalent | openEHR equivalent |
|---|---|---|
| Table | Resource | Reference Model class |
| Subtype | Extension | Archetype |
| Subset | Profile (DDL-backed) | Template (constrains archetypes) |
| Template | Implementation Guide | Operational Template (OPT) |

Note: unlike FHIR and openEHR, our subsets drive physical DDL — the API shape changes with the active subset set. This is a deliberate divergence from those standards' "stable API regardless of profiles" model.

---

## Mapping to Java

| YAML / spec concept | Java / SQL | Status |
|---|---|---|
| `inherits` (YAML) | `inheritNames` (Java), `tableExtends` (CSV) | Parser uses new name; Java/CSV unchanged |
| `subtypes:` (YAML key, list of child-table definitions) | (parser) | YAML done; Java parser rename pending |
| `subtype:` (YAML key, section/column scoping) | (parser) | YAML done; Java parser rename pending |
| `subtype` / `subtype_array` (columnType) | `ColumnType.EXTENSION` / `ColumnType.EXTENSION_ARRAY` | YAML done; Java rename pending (`SUBTYPE` / `SUBTYPE_ARRAY`) |
| Internal subtype | `TableType.INTERNAL` | Implemented |
| Subset-driven DDL | (new) | Pending — Phase 9b |
| `activateSubset` mutation | (new) | Pending |
| `deactivateSubset` mutation | (new) | Pending |
| Load-time completeness validation | (new) | Pending |
| `namespaces:` at bundle root | (new) | Pending — merge with built-in prefix map for CURIE expansion |
| Ontology FK reachability on deactivate | (new) | Pending — walk cross-schema FKs before dropping ontology tables |
