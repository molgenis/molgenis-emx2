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

### Step 5: Frontend — add `applyProfileFilter: true` to `_schema` queries — NEXT

Default all user-facing `_schema` queries to `applyProfileFilter: true`.
Schema editor app keeps unfiltered access (admin view).

#### 5a: Shared query definitions (high priority — affects most apps)
- `apps/tailwind-components/app/gql/metadata.js` — central shared query
  → add `applyProfileFilter: true` argument
- `apps/molgenis-components/src/client/client.ts` — core client `metadataQuery`
  → add `applyProfileFilter: true` argument

#### 5b: Inline `_schema` queries in page components
- `apps/ui/app/pages/[schema]/index.vue` — table listing
- `apps/tables/src/App.vue` — table view
- `apps/molgenis-viz/src/gql/schema.ts` — visualization
  → add `applyProfileFilter: true` to each

#### 5c: Schema editor — explicit NO filter (admin view)
- `apps/schema/src/utils.ts` — `schemaQuery` — DO NOT add filter
- Consider adding `applyProfileFilter: false` explicitly for clarity

#### 5d: Queries that DON'T need filtering
These fetch schema name/roles/settings only (no tables/columns), skip:
- `apps/ui/app/util/adminUtils.ts` (roles only)
- `apps/projectmanager/src/gql/schemaName.js` (name only)
- `apps/cranio-provider/src/utils/getSchemaName.ts` (name only)
- `apps/settings/src/components/Members.vue` (roles only)
- `_schemas` queries (schema list, no table metadata)

#### 5e: Verify + test
- Run `pnpm lint` and `pnpm format` on each touched app
- Manual verify: schema editor still shows all tables/columns
- Manual verify: data apps hide non-active-profile tables/columns

### Step 6: Wire YAML into web API

Replace or extend `/{schema}/api/yaml` endpoints in `JsonYamlApi.java`:
- GET: export in new hierarchical format
- POST: import from new hierarchical format (call `fromYamlFile`)

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
