# Spec: Modular Data Modeling — Naming, Profiles, and Bundles

## Scope

Terse behavioral contract for the parser, API, and DDL layer. For authoring guidance and worked examples, see [`docs/molgenis/yaml_format.md`](../../docs/molgenis/yaml_format.md).

---

## Naming Conventions

| Thing | Convention | Example |
|---|---|---|
| Table name | Uppercase start (sentence case) | `Sample types` |
| Column name | Lowercase start (sentence case) | `sample type` |
| Section name | Uppercase start | `Common fields` |
| Variant name | Uppercase start | `Whole genome sequencing` |
| Internal variant | Lowercase start (convention) | `sampling`, `quality` |
| Profile identifier | `[a-z][a-z0-9_]*` (snake_case) | `wgs`, `catalogue_core` |

Sentence-case names serve as display labels by default. An explicit `label` is only needed when the display text must differ from the name. Profile identifiers are technical — not display labels.

---

## Architectural Layers

### 1. Tables

Foundational entities (`TableType.DATA`). Define identity and primary structure. Tables are created in PostgreSQL only when their profile tag (if any) is in the active set, or when they carry no `profiles:` tag (always-on).

### 2. Variants (Composable Capabilities)

Subtables using `extends` (YAML) / `extendNames` (Java). Share parent table's PK (1:1, FK with ON UPDATE CASCADE, ON DELETE CASCADE). Subject to the same profile-driven DDL rules as tables.

**Internal variants** (`internal: true`, `TableType.INTERNAL`) group shared columns across user-selectable variants. Not shown in the variant selector dropdown.

### 3. Profiles (DDL Gate + UX Layer)

Profiles determine which tables and columns are **physically created in PostgreSQL**. This is not a visibility filter — it is a DDL gate.

- Column with `profiles: [x]`: created only when `x` is in the active profile set.
- Column with no `profiles:`: always created (always-on).
- Table with `profiles: [x]`: entire table created only when `x` is active.

**Activating** a profile is additive and idempotent (`ADD COLUMN IF NOT EXISTS`).
**Deactivating** a profile is DDL-free. Columns stay in the database. The middleware stops exposing them in the active model. EMX2 constraints are middleware-enforced (except FKs); FK targets self-resolve when the target table is not in the active model. No `confirm:` flag, no data-check, no column drop.

**Ontology tables: not dropped on deactivate.** Columns are never dropped on deactivate, so ontology FK targets remain intact. On activate: add-if-missing (unchanged).

### 4. Sections (UI Grouping)

Purely organizational. No structural impact on DDL. Can be scoped to a variant via `variant: X` — all columns in that section then belong to variant X.

---

## API Contract

- The API shape depends on the active profile set. Columns and tables not in the active set are absent from PostgreSQL and therefore absent from all API responses.
- `activateProfile(name: String!): MolgenisResult` — additive, safe, no confirmation required. Writes DDL. Unknown name returns an error.
- `deactivateProfile(name: String!): MolgenisResult` — DDL-free. Middleware stops exposing the profile's columns/tables in the active model. No confirmation needed. Columns remain in the database. Idempotent if already inactive.
- `_schema { activeProfiles: [String], availableProfiles: [ProfileInfo], bundleName: String, bundleDescription: String }` — introspection. Non-bundle schemas return empty lists / null without error.
- `ProfileInfo { name: String, description: String, includes: [String], active: Boolean }` — each registry entry with current activation state.
- Bundle-lock feature flag: setting `bundleLock.enabled=true` on a bundle-backed schema causes `migrate()` to throw a `MolgenisException`. Default off. `activateProfile`/`deactivateProfile` are unaffected by the flag.

---

## Legacy Field Policy

- YAML bundles accept only `activeProfiles:` as the canonical key for the template root. Legacy YAML files using the old key `activeSubsets:` get a parse error naming `activeProfiles` as the replacement.
- CSV schema imports accept the `profiles` column directly — this has always been the CSV column name and is unchanged.

## Validation Rules (parser must enforce)

| Rule | Error behavior |
|---|---|
| Every name in `profiles:` on a column/table must exist in the bundle's profile registry | Hard error at load time, naming the unknown identifier |
| Names must be unique across the `profiles:` section | Hard error |
| `includes:` must be acyclic | Hard error, naming the cycle |
| `[a-z][a-z0-9_]*` for profile names | Hard error |
| Reference completeness (Rule 5 — Confirmed): a ref from column A (profile X) to table B (profile Y only) is valid if there exists at least one profile in the registry whose transitive closure covers both X and Y (co-reachability). Strict per-profile reachability is rejected — real bundles like `shared/` require cross-profile refs that only co-activate via includes. | Hard error at load time if no such co-activating entry exists |
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

`profiles:` is a **keyed map** at the bundle root — key is the identifier, value has `description:` and optional `includes:` and `internal:`. `includes:` and column `profiles:` tags may reference any profile name in this section.

Activating a profile activates `{profile_name} ∪ transitive(includes)`. A column is created iff at least one of its `profiles:` values is in the active set, or it has no `profiles:` tag.

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

Sections and headings may carry `variant:` and `profiles:` that are inherited by all nested columns. A nested column can override with its own declaration.

- `variant:` — **inherited**. Scopes all nested columns to the named child table.
- `profiles:` — **inherited**. Tags all nested columns with the listed profiles.
- `semantics:` — **never inherited**. Setting `semantics:` on a section or heading is a parse error. Must be set per column.
- All other column-level attributes (`type:`, `description:`, `required:`, `key:`, `refTable:`, etc.) are per-entry and do not inherit.

See [`yaml_format.md`](../../docs/molgenis/yaml_format.md) for the full key reference and worked examples.

---

## Variant Lifecycle

Variants follow the same activation/deactivation model as columns. On activate, the variant tables are created if missing (`ADD ... IF NOT EXISTS` or `CREATE TABLE IF NOT EXISTS`). On deactivate, variant tables remain in the database but are hidden from the active schema model by the middleware filter. No data is dropped.

- Diamond inheritance (multiple `extends:`) resolved via upsert.

---

## Schema Editor Rule

On a schema backed by a `molgenis.yaml` bundle:
- Structural edits can be blocked by enabling `bundleLock.enabled=true` (off by default — admins are allowed to break the bundle). When enabled, `migrate()` throws `MolgenisException`; `activateProfile`/`deactivateProfile` remain unaffected.
- Profile activate/deactivate goes through `activateProfile`/`deactivateProfile` mutations only.

On a schema created ad-hoc via the editor (no bundle backing):
- No profile concept. The editor is the authoring path.

Do not mix the two paths on the same schema.

---

## Custom Migrations Hook

`migrations/<profile_name>/` in the bundle directory. Scripts run once when the profile is first activated on a given schema. Not every profile needs this directory. Full framework details are out of scope here.

---

## Semantics

`semantics:` URIs travel with the column definition regardless of which profiles are active. They are stable across deployments that activate the same profile. Physical profiling does not break semantic harmonisation.

Semantic URIs (`http://...`, CURIEs) must NOT appear in `profiles:` arrays. That was a bug in the old format.

---

## Comparison with Standards

| Our concept | FHIR equivalent | openEHR equivalent |
|---|---|---|
| Table | Resource | Reference Model class |
| Variant | Extension | Archetype |
| Profile (DDL-backed) | Profile | Template (constrains archetypes) |

Note: unlike FHIR and openEHR, our profiles drive physical DDL — the API shape changes with the active profile set. This is a deliberate divergence from those standards' "stable API regardless of profiles" model.

---

## Mapping to Java

| YAML / spec concept | Java / SQL | Status |
|---|---|---|
| `extends` (YAML) | `extendNames` (Java), `tableExtends` (CSV) | Done |
| `variants:` (YAML key, list of child-table definitions) | (parser) | Done — YAML parser handles `variants:` key |
| `variant:` (YAML key, section/column scoping) | (parser) | Done — YAML parser handles section/column variant scoping |
| `variant` / `variant_array` (columnType string in YAML) | `ColumnType.EXTENSION` / `ColumnType.EXTENSION_ARRAY` | Done — YAML strings mapped to Java types |
| Internal variant | `TableType.INTERNAL` | Done |
| Profile-driven DDL (activate only) | `ProfileActivator.projectSchemaMetadataToActiveProfiles` + `schema.migrate(projected)` | Done — Slice 4, rewired in Phase 9 cleanup. `ProfileActivator` exposes a pure projection function; `SqlSchema.activateProfile` calls `migrate` with the projected schema. Deactivate is DDL-free. `BundleContext`/`ProfileEntry` in core. `attachBundle(BundleContext)` on `SqlSchema`. |
| `activateProfile` / `deactivateProfile` / `getActiveProfiles` Java API | `Schema` interface + `SqlSchema` | Done — Slice 4. |
| `Schema.getBundleContext()` default method | `Schema` interface | Done — Slice 5. Default returns `null`; `SqlSchema` overrides to return attached `BundleContext`. |
| `Schema.isBundleLocked()` | `SqlSchema` | Done — Slice 6. Returns `false` by default; reads `bundleLock.enabled` setting when bundle-backed. |
| `BundleContext.getBundleName()` / `getBundleDescription()` | `BundleContext` (core) | Done — Slice 5. |
| `activateProfile(name: String!): MessageResponse` GraphQL mutation | `GraphqlSchemaFieldFactory` | Done — Slice 5. Validates known name; calls `schema.activateProfile(name)`. Unknown name → clear error. |
| `deactivateProfile(name: String!): MessageResponse` GraphQL mutation | `GraphqlSchemaFieldFactory` | Done — Slice 5. Middleware-only; no DDL. Idempotent if already inactive. |
| `_schema { activeProfiles, availableProfiles, bundleName, bundleDescription }` | `GraphqlSchemaFieldFactory` | Done — Slice 5. `ProfileInfo` GraphQL type: `name`, `description`, `includes`, `active`. Non-bundle schemas return empty lists / null. |
| Custom migrations hook | `MigrationRunner` | Exists but not wired (Phase 9 cleanup). Awaiting Slice 6d design. `migrations/<profile_name>/*.sql`; `_migrations` tracking table; idempotent logic in place. |
| Load-time completeness validation | `BundleValidator.java` | Done — Phase 9b Slice 3 |
| `imports:` preprocessor | `ImportExpander.java` | Done — Slice 3b |
| Profile name auto-normalize | `ProfileNameNormalizer.java` | Done — Slice 3b |
| `namespaces:` at bundle root | (new) | Pending — merge with built-in prefix map for CURIE expansion |
| Bundle source path | `SchemaMetadata.bundleSourcePath` field | Done — Slice 4. Set via `setBundleSourcePath(String)`. |
| Typed beans refactor | Slice 6c — parser beans | Pending. See plan for details. `VariantDef`, `ProfileDef` already exist in `bundle/` package. |
| Bundle versioning + migration integration | Slice 6d — `Migrations.initOrMigrate` | Pending. `MigrationRunner` stays in place; full design TBD. |

---

## Deferred

### Slice 6c — Typed beans (before Slice 7)
Parser currently uses `Map<String,Object>`. Replace with typed Java record classes: `Bundle`, `TableDef`, `VariantDef`, `ColumnDef`, `SectionDef`, `HeadingDef`, `ProfileDef`. `VariantDef` and `ProfileDef` already exist in `backend/molgenis-emx2-io/src/main/java/org/molgenis/emx2/io/emx2/bundle/`. Enables compile-time safety and provides a typed contract for frontend integration.

### Slice 6d — Bundle versioning + migration integration
`MigrationRunner` exists with logic for running per-profile SQL scripts, but is not wired into the activation path. Integration with `Migrations.initOrMigrate` and `SOFTWARE_DATABASE_VERSION` is a design-open item. Key questions: ordering of bundle vs platform migrations, rollback story, per-schema version tracking.
