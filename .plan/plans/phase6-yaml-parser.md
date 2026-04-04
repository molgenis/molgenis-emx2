# Phase 6: YAML Parser

## Decision: Drop profile definitions entirely

Profiles are just tags referenced inline on tables/extensions/sections/columns.
No `profiles/` directory needed. No profile definition files.
Templates are the single place that controls which profile tags are active.
Rationale: simpler, more explicit, avoids hidden transitive dependencies.

## Steps

### Step 1: Table file parser + tests (FIRST) — DONE (commit f6bb94233)

Created `Emx2Yaml.java` in `backend/molgenis-emx2-io/src/main/java/org/molgenis/emx2/io/emx2/`

Core methods implemented:
- `fromYamlFile(InputStream)` -> `SchemaMetadata`
- `fromYamlDirectory(Path)` -> `SchemaMetadata` (reads all .yaml files from tables/ dir)

Full metadata model support: tables, extensions, sections, columns, all column props, profiles.

Test fixtures in `src/test/resources/yaml-model/` with roundtrip test in `Emx2YamlTest.java`.

### Step 2: YAML export (SchemaMetadata -> YAML) — DONE (commit f6bb94233)

Implemented `toYamlFile(SchemaMetadata)` and `toYamlDirectory(SchemaMetadata, Path)`.
Roundtrip test (parse → export → parse → compare) passes.

### Step 3: Template file parser — DONE (commit f6bb94233)

Implemented `fromYamlTemplate(Path)` and `toYamlTemplate(TemplateResult)`.
TemplateResult holds: name, description, schema, profiles, settings, permissions, fixedSchemas.
Wildcard expansion (`tables/*`), role mapping (view→Viewer, edit→Editor).
3 new tests: testTemplateRd3, testTemplateFull, testTemplateRoundtrip. All 9 tests pass.

### Step 4: Profile filtering — NEXT

**Principle**: Profiles control visibility, not existence. All tables/columns always exist in PostgreSQL.
GraphQL data schema is UNCHANGED — all fields always available. Filtering is metadata-level only.

#### Design decisions
- Profiles are a **first-class metadata field** (like settings), not stuffed into settings map
- `Column.profiles` — already exists, persisted via COLUMN_PROFILES
- `TableMetadata.profiles` — exists in Java, needs DB persistence (migration)
- `SchemaMetadata.profiles` — NEW, the "active profiles" for this schema (migration)
- Use same persistence mechanism as column profiles (String[] in metadata tables)

#### Profile matching logic
- No profiles on column/table → always visible
- `profiles: [wgs]` → visible only when `wgs` is in schema's active profiles
- `profiles: [-core]` → visible when `core` is NOT in schema's active profiles
- No active profiles on schema (empty/null) → all columns/tables visible (no filtering)

#### 4a: Migration — persist table + schema profiles
- Add `TABLE_PROFILES` column to `table_metadata` table
- Add `SCHEMA_PROFILES` column to `schema_metadata` table (active profiles)
- Update MetadataUtils to read/write table profiles and schema profiles

#### 4b: Metadata layer — profile filter methods
- `SchemaMetadata.getActiveProfiles()` / `setActiveProfiles(String...)`
- `TableMetadata.getColumnsForProfiles(String[] activeProfiles)` → filtered column list
- `TableMetadata.getVisibleExtensions(String[] activeProfiles)` → filtered extension list
- Shared `ProfileUtils.matchesProfiles(String[] itemProfiles, String[] activeProfiles)` helper

#### 4c: GraphQL metadata API — filtered metadata response
- `_schema` query gets two new optional filter parameters:
  - `applyProfileFilter: Boolean` — when true, filter using schema's active profiles
  - `profiles: [String]` — explicit profile list to filter by (overrides schema's active profiles)
  - No parameters / both null → return full unfiltered metadata (backward compatible)
- Client receives only profile-matching columns/tables → no profile logic needed in client
- Standard API calls use `applyProfileFilter: true` for normal UI rendering
- Admin/schema editor can call without filter to see everything
- GraphQL **data** schema stays unchanged (all fields always queryable)

#### 4d: SqlQuery + SqlTable — CSV import/export awareness
- CSV export: SqlQuery uses profile-filtered columns (omit non-active columns from output)
- CSV import: SqlTable silently skips non-active-profile columns on write
- Ensures CSV roundtrip respects profile boundaries

#### Future (not this step)
- Store template reference on schema (for migration/upgrade tracking)
- Profile write mode setting (strict=error vs lenient=skip)

### Step 5: Wire YAML into web API

Replace or extend `/{schema}/api/yaml` endpoints in `JsonYamlApi.java`:
- GET: export in new hierarchical format
- POST: import from new hierarchical format (call `fromYamlFile`)

### NOT in scope
- `- import:` fragment inclusion with overrides (future)
- i18n labels in YAML (future — current CSV format handles this)

## Verification

After each step:
1. `./gradlew :backend:molgenis-emx2-io:test`
2. `./gradlew :backend:molgenis-emx2-sql:test` (after step 4)
3. `./gradlew :backend:molgenis-emx2-webapi:test` (after step 5)
