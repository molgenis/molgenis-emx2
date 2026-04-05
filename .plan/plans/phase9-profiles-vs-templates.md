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

3. **Deactivate = destructive, fail loud by default.** Tables/columns belonging exclusively to the deactivated subset are dropped. The API must report "this will drop N columns with data; confirm?" and only proceed with an explicit `confirm: true`. Never silent.

4. **Ontology tables: conditional drop on deactivate.** On activate: add-if-missing. On deactivate: walk all FK references across all schemas (including cross-schema). Drop only if zero references remain. Skip silently if any FK still references the ontology table — no error. Rationale: the old blanket "never drop" was over-protective; FK reachability check avoids breaking shared usage while still allowing cleanup.

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

- `shared/` bundle rewritten with new `molgenis.yaml` format (formerly `shared2/`, promoted).
- Old 13 flat wrapper files removed.
- Simple bundles (`petstore/`, `pages/`) converted to zero-config `molgenis.yaml`.
- `Emx2Yaml.java` parser updated: subset registry, `includes:` resolution, unknown-name validation.
- GraphQL `activeProfiles` mutation wired; schema editor profile checkboxes populated from registry.
- User docs written: `docs/molgenis/yaml_format.md`.
- Review complete: `.plan/reviews/phase9-persona-review.md`.

---

## Remaining (Java work — Phase 9b)

| Item | Notes |
|---|---|
| Load-time completeness validation | Ref and `includes:` completeness checks at parse time; named errors |
| DDL generation per active subset set | Only create tables/columns for active subsets; always-on for untagged columns |
| Drop-on-deactivate with confirmation | Check for data before dropping; `confirm: true` override |
| Split mutations | `activateSubset(name)` and `deactivateSubset(name, confirm)` in GraphQL API |
| Ontology drop-on-deactivate with FK reachability check | On deactivate, walk all FKs (including cross-schema references) pointing to the ontology table. Drop only if zero references remain. Skip silently if any reference exists — do not error. |
| Schema-editor rule enforcement | Lock structural edits on bundle-backed schemas; route through mutations |
| Custom-migrations directory hook | Detect `migrations/<subset_name>/` on activate; run once per schema |
| Retire `activeProfiles`/`applyProfileFilter` | Remove after split mutations are in place and callers migrated |
| **Accept single-file bundles** | Parser must accept `data/templates/<name>.yaml` as a bundle entry (no directory required) when the file contains a top-level `name:` and either inline `tables:` entries or `- import:` entries under `tables:`. Single-file bundles cannot have ontologies, demodata, settings, or migrations — only directory bundles can. |
| **Parse `namespaces:` at bundle root** | Merge with built-in prefix map when resolving short-form CURIEs in `semantics:`. Bundle-declared prefixes override built-in ones on conflict. |
| **Rename `extension` → `subtype` (deferred, one cohesive pass)** | Rationale: `extension` collides with FHIR Extension (additive field) and openEHR CLUSTER extension (unmodelled content). `subtype` pairs naturally with the stable `inherit:` / `inheritNames` / `tableInherit` vocabulary already in master — `inherit:` is the relationship, `subtype` is the discriminator + the child-table terminology. Full scope when picked up: (1) `ColumnType.EXTENSION` / `EXTENSION_ARRAY` → `SUBTYPE` / `SUBTYPE_ARRAY` across parser, SQL, tests, plus a migration; (2) `extensions:` YAML key (the list of child-table definitions inside a table file) → `subtypes:`; (3) docs prose "extension" → "subtype"; (4) `TableType.INTERNAL` stays — it remains a flag on a subtype meaning "not user-selectable". Do NOT split across phases — rename all four together or not at all. Defer until after Phase 9b Java work stabilizes. |

---

## Decisions (resolved)

1. **`includes:` depth** — arbitrary depth; cycles are a hard error.
2. **Identifier style** — snake_case (`[a-z][a-z0-9_]*`). No camelCase, no spaces.
3. **Bundle entry** — either a single YAML file (`data/templates/<name>.yaml`) with `name:` + inline `tables:`, or a directory with `molgenis.yaml` where the simplest form is `name:` + `imports:`. Both forms MUST declare `name:`. A bundle needs at least one of `tables:` or `imports:`.
4. **Semantic URIs** — live in `semantics:` on the column, never in `subsets:`.
5. **Profiles + templates unified** — one namespace; `subsets:` = internal, `templates:` = user-facing picker.
6. **No back-compat for old flat format** — feature branch; nothing in production depends on it.
7. **Centralized bundle config** — ontologies, demodata, settings, permissions, fixedSchemas all in `molgenis.yaml`.
8. **Subsets are structural (physical DDL), not visibility filters.** The active subset set determines which tables and columns exist in PostgreSQL.
9. **Deactivate fails loud by default; ontology tables are dropped on deactivate only when no FK references remain across all schemas.**

---

## Out of scope

- Changing anything about extensions / multiple inheritance.
- Frontend profile UI beyond existing checkbox wiring.
- RDF/semantic URI cleanup beyond `semantics:` separation.
- Profile-based validation (required/optional changes).
