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

### Step 4: Profile filtering — DONE (commits f6bb94233, 346590656)

Backend complete:
- migration35.sql: `tableProfiles` + `schemaProfiles` columns
- MetadataUtils: persists table/schema profiles
- SchemaMetadata: `getActiveProfiles()` / `setActiveProfiles()`
- ProfileUtils: `matchesActiveProfiles()` with positive/negative matching
- TableMetadata: `getColumnsForProfiles()` / `getNonInheritedColumnsForProfiles()`
- GraphQL `_schema`: `applyProfileFilter` + `profiles` filter params
- `profiles` field on outputTableType and outputColumnMetadataType
- 8 GraphQL integration tests + 7 ProfileUtils unit tests, all green

### Step 5: Frontend profile filtering — DONE (commits 45f9c976c, next)

5a-5e: Added `applyProfileFilter: true` to 5 user-facing `_schema` queries.
Schema editor left unfiltered. Added `profiles` to GraphQL mutation input types.

5f: GraphQL `activeProfiles` mutation + output:
- `change(activeProfiles: [...])` mutation to set active profiles on schema
- `activeProfiles` exposed on `_schema` output type
- Fixed table profiles lost in JSON→SchemaMetadata and migrate paths
- `saveActiveProfiles()` on SqlSchemaMetadata, `sync()` includes profiles
- 3 JUnit tests + 5 e2e tests (API filtering, UI listing, form columns) — all green
- Full `./gradlew test` passes, all apps formatted+linted

### Step 6: Wire YAML into web API — DONE

Replaced flat Jackson-based YAML with hierarchical Emx2Yaml format:
- Added `toYamlSchema()` / `fromYamlSchema()` — multi-document YAML (root tables separated by `---`)
- `JsonYamlApi.java`: GET/POST/DELETE now use `Emx2Yaml` instead of `JsonUtil`
- Updated `WebApiSmokeTests.testJsonYamlApi()` for new format
- Updownload app works unchanged (already links to `/{schema}/api/yaml`)
- IO + WebAPI tests pass (pre-existing failures in `testScriptScheduling` and `TablePermissionsGraphqlTest` unrelated)

### Step 6b: YAML+ZIP download and ZIP import detection — DONE

#### Decisions
- Separate endpoint: `/{schema}/api/yamlzip` (not query param)
- ZIP with YAML uses `molgenis.yaml` as marker file (vs `molgenis.csv` for CSV format)
- `tables/*.yaml` for metadata, data CSVs alongside
- Modify existing classes, no new TableStore

#### Download: `GET /{schema}/api/yamlzip`
- New endpoint in `ZipApi.java`
- Export metadata as `tables/*.yaml` (via `Emx2Yaml.toYamlDirectory`)
- Export table data as CSV files (same as current)
- Include `molgenis.yaml` marker file in root (can be empty or contain schema-level metadata)

#### Upload: ZIP detection in `postZip()`
- Check ZIP contents: if `molgenis.yaml` exists → YAML path
- YAML path: parse `tables/*.yaml` via `Emx2Yaml.fromYamlDirectory`, import data CSVs normally
- CSV path: existing `ImportCsvZipTask` (no change)
- Max 1 template file in root

#### Frontend
- Add `yaml.zip` link in `apps/updownload/src/components/Import.vue`
- Upload unchanged — ZIP upload already goes to `/{schema}/api/zip`

### Step 7: CSV import/export profile awareness (deferred)
- CSV export: filter columns by active profiles
- CSV import: silently skip non-active-profile columns
- Lower priority — can be done after frontend is working

### NOT in scope
- `- import:` fragment inclusion with overrides (future)
- i18n labels in YAML (future — current CSV format handles this)
- Template reference on schema (for migration/upgrade tracking)

## Verification

After each step:
1. `./gradlew :backend:molgenis-emx2-io:test`
2. `./gradlew :backend:molgenis-emx2-sql:test` (after step 4)
3. `./gradlew :backend:molgenis-emx2-webapi:test` (after step 6)
4. `pnpm lint && pnpm format` on touched apps (after step 5)
