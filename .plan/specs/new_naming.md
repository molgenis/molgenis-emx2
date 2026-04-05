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
**Deactivating** a subset is DDL-free. Columns stay in the database. The middleware stops exposing them in the active model. EMX2 constraints are middleware-enforced (except FKs); FK targets self-resolve when the target table is not in the active model. No `confirm:` flag, no data-check, no column drop.

**Ontology tables: not dropped on deactivate.** Columns are never dropped on deactivate, so ontology FK targets remain intact. On activate: add-if-missing (unchanged).

### 4. Sections (UI Grouping)

Purely organizational. No structural impact on DDL. Can be scoped to a subtype via `subtype: X` — all columns in that section then belong to subtype X.

### 5. Templates (Deployment Bundles)

User-facing named presets of subset activations. Shown in the admin picker. Carry optional deployment config (`settings:`, `permissions:`, `fixedSchemas:`). A template activates `{itself} ∪ transitive(includes)`.

---

## API Contract

- The API shape depends on the active subset set. Columns and tables not in the active set are absent from PostgreSQL and therefore absent from all API responses.
- `activateSubset(name: String!): MolgenisResult` — additive, safe, no confirmation required. Writes DDL. Unknown name returns an error.
- `deactivateSubset(name: String!): MolgenisResult` — DDL-free. Middleware stops exposing the subset's columns/tables in the active model. No confirmation needed. Columns remain in the database. Idempotent if already inactive.
- `_schema { activeSubsets: [String], availableSubsets: [SubsetInfo], availableTemplates: [SubsetInfo], bundleName: String, bundleDescription: String }` — introspection. Non-bundle schemas return empty lists / null without error.
- `SubsetInfo { name: String, description: String, includes: [String], active: Boolean }` — each registry entry with current activation state.
- Bundle-lock feature flag: setting `bundleLock.enabled=true` on a bundle-backed schema causes `migrate()` to throw a `MolgenisException`. Default off. `activateSubset`/`deactivateSubset` are unaffected by the flag.

---

## Legacy Field Policy

- YAML bundles and templates do NOT accept `profiles:` at any level. Use `subsets:` (column/table scope) or `activeSubsets:` (template root). A `profiles:` key in a YAML template throws a `MolgenisException` naming `activeSubsets` as the replacement.
- CSV schema imports accept `profiles` column as a one-way migration input mapped to `subsets` internally. CSV export emits `subsets`, never `profiles`.

## Validation Rules (parser must enforce)

| Rule | Error behavior |
|---|---|
| Every name in `subsets:` on a column/table must exist in the bundle's subset or template registry | Hard error at load time, naming the unknown identifier |
| Names must be unique across `subsets:` and `templates:` sections | Hard error |
| `includes:` must be acyclic | Hard error, naming the cycle |
| `[a-z][a-z0-9_]*` for subset/template names | Hard error |
| Reference completeness (Rule 5 — Confirmed): a ref from column A (subset X) to table B (subset Y only) is valid if there exists at least one template or subset in the registry whose transitive closure covers both X and Y (co-reachability). Strict per-subset reachability is rejected — real bundles like `shared/` require cross-subset refs that only co-activate via templates. | Hard error at load time if no such co-activating entry exists |
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

Subtypes follow the same activation/deactivation model as columns. On activate, the subtype tables are created if missing (`ADD ... IF NOT EXISTS` or `CREATE TABLE IF NOT EXISTS`). On deactivate, subtype tables remain in the database but are hidden from the active schema model by the middleware filter. No data is dropped.

- Diamond inheritance (multiple `inherits:`) resolved via upsert.

---

## Schema Editor Rule

On a schema backed by a `molgenis.yaml` bundle:
- Structural edits can be blocked by enabling `bundleLock.enabled=true` (off by default — admins are allowed to break the bundle). When enabled, `migrate()` throws `MolgenisException`; `activateSubset`/`deactivateSubset` remain unaffected.
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
| Subset-driven DDL (activate only) | `SubsetActivator.projectSchemaMetadataToActiveSubsets` + `schema.migrate(projected)` | Done — Slice 4, rewired in Phase 9 cleanup. `SubsetActivator` now exposes a pure projection function; `SqlSchema.activateSubset` calls `migrate` with the projected schema. Deactivate is DDL-free. `BundleContext`/`SubsetEntry` in core. `attachBundle(BundleContext)` on `SqlSchema`. |
| `activateSubset` / `deactivateSubset` / `getActiveSubsets` Java API | `Schema` interface + `SqlSchema` | Done — Slice 4. |
| `Schema.getBundleContext()` default method | `Schema` interface | Done — Slice 5. Default returns `null`; `SqlSchema` overrides to return attached `BundleContext`. |
| `Schema.isBundleLocked()` | `SqlSchema` | Done — Slice 6. Returns `false` by default; reads `bundleLock.enabled` setting when bundle-backed. |
| `BundleContext.getBundleName()` / `getBundleDescription()` | `BundleContext` (core) | Done — Slice 5. New constructor overload; `Emx2Yaml.toBundleContext()` passes name+description. |
| `activateSubset(name: String!): MessageResponse` GraphQL mutation | `GraphqlSchemaFieldFactory` | Done — Slice 5. Validates known name; calls `schema.activateSubset(name)`. Unknown name → clear error. |
| `deactivateSubset(name: String!): MessageResponse` GraphQL mutation | `GraphqlSchemaFieldFactory` | Done — Slice 5. Middleware-only; no DDL. Idempotent if already inactive. |
| `_schema { activeSubsets, availableSubsets, availableTemplates, bundleName, bundleDescription }` | `GraphqlSchemaFieldFactory` | Done — Slice 5. `SubsetInfo` GraphQL type: `name`, `description`, `includes`, `active`. Non-bundle schemas return empty lists / null. |
| Custom migrations hook | `MigrationRunner` | Exists but not wired (Phase 9 cleanup). Awaiting Slice 6d design. TODO comment in source. `migrations/<subset_name>/*.sql`; `_migrations` tracking table; idempotent logic in place. |
| Load-time completeness validation | `BundleValidator.java` | Done — Phase 9b Slice 3 |
| `imports:` preprocessor | `ImportExpander.java` | Done — Slice 3b |
| Profile name auto-normalize | `ProfileNameNormalizer.java` | Done — Slice 3b |
| `namespaces:` at bundle root | (new) | Pending — merge with built-in prefix map for CURIE expansion |
| Bundle source path | `SchemaMetadata.bundleSourcePath` field | Done — Slice 4. Set via `setBundleSourcePath(String)`. No longer used by `activateSubset` (migrations unwired). |
| Typed beans refactor | Slice 6c — parser beans | Pending. See plan for details. |
| Bundle versioning + migration integration | Slice 6d — `Migrations.initOrMigrate` | Pending. `MigrationRunner` stays in place; full design TBD. |

---

## Deferred

### Slice 6c — Typed beans (before Slice 7)
Parser currently uses `Map<String,Object>`. Replace with typed Java record classes: `Bundle`, `TableDef`, `SubtypeDef`, `ColumnDef`, `SectionDef`, `HeadingDef`, `SubsetDef`. Enables compile-time safety and provides a typed contract for frontend integration.

### Slice 6d — Bundle versioning + migration integration
`MigrationRunner` exists with logic for running per-subset SQL scripts, but is not wired into the activation path. Integration with `Migrations.initOrMigrate` and `SOFTWARE_DATABASE_VERSION` is a design-open item. Key questions: ordering of bundle vs platform migrations, rollback story, per-schema version tracking.
