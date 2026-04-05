# Phase 9: Separate Profiles from Templates, unify in `molgenis.yaml`

**Status**: Phase 9a complete. Phase 9b (physical subsetting) design locked, Java work pending.
**Parent plan**: `yaml-profile-format-v8.md`
**Spec**: `.plan/specs/new_naming.md`

---

## Problem (summary)

577/817 columns (71%) in `data/templates/shared/` each repeated 2–9 template names in `subsets:`. The flat format conflated "which templates include this column" with "reusable subset grouping." Phase 9a introduced `molgenis.yaml` as the bundle entry point with a proper subset/template registry, collapsing most subset lists to one entry.

---

## Phase 9b: Physical subsetting semantics (pivot)

Design is locked. All 9 items below are decided. Java implementation is pending.

1. **Bundle + active subset set → physical DDL.** Only tables/columns belonging to active subsets are created in PostgreSQL. Columns without a `subsets:` tag are always-on.

2. **Activate = additive and idempotent.** Uses `ALTER TABLE ADD COLUMN IF NOT EXISTS`. Safe to call multiple times. No confirmation needed.

3. **Deactivate = columns stay in database.** Tables/columns belonging to a deactivated subset are NOT dropped. The middleware stops exposing them in the active model (EMX2 constraints are middleware-enforced except FKs; FK targets self-resolve when the target is no longer in the active model). No `confirm:` flag needed. No data-check. DDL-free on deactivate.

4. **Ontology tables: NOT dropped on deactivate.** No FK reachability walk required — columns are not dropped, so ontology FK targets remain intact. On activate: add-if-missing.

5. **Incomplete subset combinations fail at load time.** Two cases:
   - Column A in subset X has `refTable: B` where B belongs only to subset Y, Y is not active → parser rejects, naming the missing subset.
   - Subset Z `includes: [Y]`, Z is active, Y not resolvable → parser rejects.

6. **Mutation API is split** (to be implemented in Java):
   - `activateSubset(name)` — additive, safe, no confirmation.
   - `deactivateSubset(name, confirm: Boolean)` — destructive, requires `confirm: true` when data would be lost.
   - The `activeProfiles`/`applyProfileFilter` API from Phase 6 is superseded. It stays in place during transition and will be removed once all callers migrate.

7. **Schema editor rule.** On a schema backed by a `molgenis.yaml` bundle, the schema editor is read-only for structure — subset activate/deactivate goes through the split mutations. On a schema created ad-hoc via the editor (no bundle backing), there is no subset concept — the editor is the authoring path. Do not mix.

8. **Custom migrations hook.** The bundle layout supports an optional `migrations/<subset_name>/` directory. Scripts there run once when the subset is first activated on a given schema. This is a hook for data transforms, backfills, and index additions. Not every subset needs migrations.

9. **`semantics:` is unaffected.** Semantic URIs are stable across deployments that activate the same subset. Physical subsetting does not break semantic harmonisation.

---

## Completed (Phase 9a)

- `shared/` bundle rewritten with new `molgenis.yaml` format.
- Old 13 flat wrapper files removed.
- Simple bundles (`petstore/`, `pages/`) converted to zero-config `molgenis.yaml`.
- `Emx2Yaml.java` parser updated: subset registry, `includes:` resolution, unknown-name validation.
- GraphQL `activeProfiles` mutation wired; schema editor profile checkboxes populated from registry.
- User docs written: `docs/molgenis/yaml_format.md`.
- Review complete: `.plan/reviews/phase9-persona-review.md`.

---

## Remaining (Java work — Phase 9b)

| Item | Slice | Notes |
|---|---|---|
| `namespaces:` parsing in single-file bundles | Roadmap | Currently silently ignored. Directory bundles parse it correctly via `parseNamespaces`. TODO comment added in `parseSingleFileBundle`. Pending. |
| Load-time completeness validation | Slice 3 — DONE | Ref and `includes:` completeness checks at parse time; named errors |
| Imports preprocessor (`imports:` anywhere in YAML) | Slice 3b — DONE | Pure `ImportExpander.expandImports`; cycle detection; key-collision errors; file-not-found errors |
| Profile name auto-normalize | Slice 3b — DONE | `ProfileNameNormalizer.normalize`; emit INFO log on rename; `toBundleDirectory` generates `all` template |
| DDL generation per active subset set | Slice 4 — DONE | Only create tables/columns for active subsets; always-on for untagged columns; **deactivate is DDL-free**. `activateSubset`/`deactivateSubset`/`getActiveSubsets` on `Schema` + `SqlSchema`. `SubsetActivator` + `MigrationRunner`. `BundleContext`/`SubsetEntry` in core. `attachBundle(BundleContext)` on `SqlSchema`. Rewired in Phase 9 cleanup: `activateSubset` now calls `SubsetActivator.projectSchemaMetadataToActiveSubsets` + `schema.migrate(projected)` instead of direct DDL. |
| Split mutations | Slice 5 — DONE | `activateSubset(name)` + `deactivateSubset(name)` GraphQL mutations; `_schema` introspection: `activeSubsets`, `availableSubsets`, `availableTemplates`, `bundleName`, `bundleDescription`; `SubsetInfo` GraphQL type; `BundleContext.getBundleName()/getBundleDescription()`; `Schema.getBundleContext()` default interface method; `TestSubsetMutations` (9 tests after cleanup). |
| Schema-editor rule enforcement | Slice 6 — DONE | `Schema.isBundleLocked()` flag; `BUNDLE_LOCK_FEATURE_FLAG` setting; dormant guard in `SqlSchema.migrate()`. Default off. |
| Custom-migrations directory hook | Slice 4 — DONE (unwired) | `MigrationRunner` exists but is not wired into `activateSubset`. Awaiting Slice 6d design. TODO comment added to `MigrationRunner.java`. |
| Retire `activeProfiles`/`applyProfileFilter` | Slice 6 — DONE | Removed `change(activeProfiles:)` mutation arg; removed `_schema.activeProfiles` field; removed `applyProfileFilter` and `profiles:` query args; deleted `TestProfileFiltering` legacy tests; `SchemaMetadata.getActiveProfiles` renamed to `getActiveSubsets`. |
| Typed beans refactor | Slice 6c — PENDING | See below |
| Bundle versioning + migration integration | Slice 6d — PENDING | See below |
| Frontend subset UI | Slice 7 — PENDING | Depends on Slice 6c typed contract |
| **Accept single-file bundles** | Parser must accept `data/templates/<name>.yaml` as a bundle entry (no directory required) when the file contains a top-level `name:` and either inline `tables:` keyed map or `imports:` list. Single-file bundles cannot have ontologies, demodata, settings, or migrations — only directory bundles can. |
| **Parse `namespaces:` at bundle root** | Merge with built-in prefix map when resolving short-form CURIEs in `semantics:`. Bundle-declared prefixes override built-in ones on conflict. |
| **Rename `EXTENSION` → `SUBTYPE` in Java (YAML side done)** | Data files, docs, and spec have been renamed to use `subtypes:` / `subtype:` / `type: subtype` / `type: subtype_array`. Java-side rename remaining: `ColumnType.EXTENSION` / `EXTENSION_ARRAY` enum values → `SUBTYPE` / `SUBTYPE_ARRAY`; any `extensions:` YAML parser key recognition in `Emx2Yaml.java` → `subtypes:`; migration script to update persisted metadata; tests. Recommended as one atomic backend pass. |
| **Accept keyed-map `subsets:`, `templates:`, `subtypes:`, `columns:` at all levels** | Parser must accept and emit keyed maps (not lists) for all four. |
| **Accept `subtype:` attribute on column entries and on section/heading entries** | Attribute scopes the column or all nested columns to the named child table. |
| **Implicit section detection** | Entry with nested `columns:` at level 0 under a table's `columns:` is a section — no `type: section` required. |
| **Implicit heading detection** | Entry with nested `columns:` at level 1 (inside a section's `columns:`) is a heading — no `type: heading` required. |
| **Enforce max nesting depth 2** | table → section → heading → columns. `columns:` key inside a heading is a parse error. |
| **Enforce table-wide column name uniqueness** | Walk the full nested `columns:` tree on load; reject on first duplicate, naming the duplicate key and both locations. |
| **Reject reserved name `columns`** | A column, section, or heading named literally `columns` is a parse error. |
| **Section/heading `subtype:` and `subsets:` inherit into nested columns** | Nested column's own `subtype:` or `subsets:` declaration overrides the container default. |
| **Reject `semantics:` on section/heading containers** | `semantics:` on an entry that resolves as a section or heading is a parse error; must be set per data column. |

---

## Slice 6c — Typed beans refactor (Pending, before Slice 7)

Current parser operates on `Map<String,Object>` throughout. User wants typed records:

- `Bundle` (name, description, version, namespaces, subsets, templates, tables)
- `TableDef` (name, description, inherits, subsets, semantics, subtypes, columns)
- `SubtypeDef` (inherits, description, internal)
- `ColumnDef` (name, type, subtype, subsets, semantics, refTable, ...)
- `SectionDef` / `HeadingDef` — inline, have nested `columns:`
- `SubsetDef` (id, description, includes)

Parser builds these beans from YAML; downstream (validator, emitter, SubsetActivator) consumes beans, not maps. Record classes preferred (Java 17+). Estimated 400–600 lines touched.

Rationale: compile-time safety, no stringly-typed map access, self-documenting grammar, easier to evolve. Must be done before Slice 7 so frontend has a typed contract.

---

## Slice 6d — Bundle versioning + migration integration (Pending, before Slice 7)

Open design question. User wants:

- Bundle version is the global `SOFTWARE_DATABASE_VERSION` (int, currently 36) that EMX2 already uses for platform migrations — not a parallel version number.
- Per-schema tracking of which version the bundle was loaded at (new column on schema metadata table).
- On EMX2 upgrade, `Migrations.initOrMigrate` picks up both platform migrations AND any bundle migrations shipped with the new version.
- Bundle `migrations/<name>/*.sql` files tagged with the global version they apply at (e.g., `v37_add_index.sql`).
- Platform-change safety: bundle SQL runs AFTER platform migrations, against post-migration state.
- Reuse the existing `MOLGENIS.version_metadata` tracking table pattern — do not invent a parallel `_migrations` table (the current `MigrationRunner` does this; it will be rewired or removed in Slice 6d).
- `MigrationRunner.java` stays in place but is unwired until Slice 6d decides how to integrate.

Open questions to resolve in Slice 6d design:
1. Does `initOrMigrate` iterate bundle-backed schemas and apply their migrations in the same tx as platform migrations, or in a follow-up pass?
2. How are bundle migrations ordered relative to platform migrations of the same version?
3. What's the rollback story on partial failure?
4. Does `_schema { bundleVersion }` reflect the schema's installed version or the bundle's shipped version?

---

## Decisions (resolved)

**Decision — legacy `profiles` field**: Removed entirely from YAML (both bundle and template formats).
In CSV, the `profiles` column is still accepted on READ as a one-way migration input; on write,
CSV emits the new `subsets` column name. Users migrating old CSV models get transparent upgrade
without code changes. Legacy `profiles:` key in YAML templates throws a clear error naming
`activeSubsets` as the replacement.

1. **`includes:` depth** — arbitrary depth; cycles are a hard error.
2. **Identifier style** — snake_case (`[a-z][a-z0-9_]*`). No camelCase, no spaces.
3. **Bundle entry** — either a single YAML file (`data/templates/<name>.yaml`) with `name:` + inline `tables:`, or a directory with `molgenis.yaml` where the simplest form is `name:` + `imports:`. Both forms MUST declare `name:`. A bundle needs at least one of `tables:` or `imports:`.
4. **Semantic URIs** — live in `semantics:` on the column, never in `subsets:`.
5. **Profiles + templates unified** — one namespace; `subsets:` = internal, `templates:` = user-facing picker.
6. **No back-compat for old flat format** — feature branch; nothing in production depends on it.
7. **Centralized bundle config** — ontologies, demodata, settings, permissions, fixedSchemas all in `molgenis.yaml`.
8. **Subsets are structural (physical DDL), not visibility filters.** The active subset set determines which tables and columns exist in PostgreSQL.
9. **Deactivate is DDL-free.** Columns stay in the database. The middleware stops exposing them in the active model. No confirmation needed. Ontology tables are never dropped on deactivate. Only `activate` writes DDL.
10. **`subtype` used consistently for all three contexts.** The word "subtype" is used for (a) the `subtypes:` declaration map (child table declarations), (b) the `subtype:` attribute on columns/sections (scoping to a child table), and (c) the `type: subtype` discriminator column type. YAML position disambiguates. Alternatives (`subtable`, `subclass`, `variant`) rejected — "subtype" matches biomedical vocabulary ("disease subtypes", "cell subtypes") and ER-modeling textbook terminology. Does not collide with FHIR Extension or openEHR CLUSTER extension — those are different concepts.

---

## Out of scope

- Changing anything about extensions / multiple inheritance.
- Frontend profile UI beyond existing checkbox wiring.
- RDF/semantic URI cleanup beyond `semantics:` separation.
- Profile-based validation (required/optional changes).
