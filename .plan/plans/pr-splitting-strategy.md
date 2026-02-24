# PR Splitting Strategy for #5495

## Status: Phase 0 Complete

## Overview
Split large PR #5495 (71 files, ~5800 lines) into 4 smaller PRs after PR #5762 merges.

## Branch Strategy
- `feat/rest-json-ld-graphql` = full reference (this branch, kept intact)
- 4 new branches created from master, cherry-picking subsets

## Phases

### Phase 0: Prepare ✅
- [x] Add backward-compat route aliases
- [x] Commit: `30fa46de6`
- [x] Push branch as backup

### Phase 1: Wait for #5762
- [ ] Review/approve PR #5762 (graphql-fragments)
- [ ] Merge #5762 to master
- [ ] Rebase this branch against master
- [ ] Resolve conflicts

### Phase 2: PR-A Core Infrastructure (~300 lines)
Branch: `mswertz/pr-a-core-infrastructure`

Files:
- `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/PrimaryKey.java`
- `backend/molgenis-emx2/src/test/java/org/molgenis/emx2/PrimaryKeyTest.java`
- `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Constants.java`
- `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/MolgenisException.java`
- `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/utils/TypeUtils.java`
- Delete old PrimaryKey from RDF module
- Update imports

Test: `./gradlew :backend:molgenis-emx2:test :backend:molgenis-emx2-rdf:test`

### Phase 3: PR-B JSON-LD Layer (~800 lines)
Branch: `mswertz/pr-b-jsonld-layer`

Files:
- `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/jsonld/JsonLdValidator.java`
- `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/jsonld/RestOverGraphql.java`
- `backend/molgenis-emx2-rdf/src/main/java/org/molgenis/emx2/rdf/jsonld/JsonLdSchemaGenerator.java`
- `backend/molgenis-emx2-rdf/src/main/java/org/molgenis/emx2/rdf/RdfDataValidationService.java`
- Tests
- `backend/molgenis-emx2-graphql/build.gradle` (RDF dependency)

Test: `./gradlew :backend:molgenis-emx2-graphql:test`

Depends on: PR-A

### Phase 4: PR-C REST API (~2500 lines)
Branch: `mswertz/pr-c-rest-api`

Files:
- `backend/molgenis-emx2-webapi/src/main/java/org/molgenis/emx2/web/RestApi.java` (new)
- `backend/molgenis-emx2-webapi/src/main/java/org/molgenis/emx2/web/DataApi.java` (new)
- `backend/molgenis-emx2-webapi/src/main/java/org/molgenis/emx2/web/DownloadApiUtils.java` (new)
- `CsvApi.java`, `ExcelApi.java`, `ZipApi.java` (refactor + legacy aliases)
- `RDFApi.java` (route changes)
- `MolgenisWebservice.java`, `GraphqlApi.java`, `Constants.java`
- Delete `JsonYamlApi.java`
- `WebApiSmokeTests.java`
- `RdfApiLegacyTests.java` (new)

Test: `./gradlew :backend:molgenis-emx2-webapi:test`

Depends on: PR-A, PR-B

### Phase 5: PR-D Docs + Frontend (~600 lines)
Branch: `mswertz/pr-d-docs-frontend`

Files:
- `docs/molgenis/dev_batchapi.md`
- `docs/molgenis/dev_rdf.md`
- `docs/molgenis/semantics.md`
- `docs/molgenis/use_updownload.md`
- `apps/ui/app/pages/[schema]/shacl/index.vue`
- `apps/ui/app/util/shaclUtils.ts`
- `apps/updownload/src/components/Import.vue`
- `tools/pyclient/src/molgenis_emx2_pyclient/client.py`
- `tools/pyclient/tests/test_client.py`
- `data/scripts/` updates

Test: `cd tools/pyclient && pytest` and `cd apps/ui && pnpm test`

Depends on: PR-C

## Merge Order
```
#5762 (graphql-fragments)
    ↓
[rebase feat/rest-json-ld-graphql]
    ↓
PR-A (core) → PR-B (jsonld) → PR-C (rest-api) → PR-D (docs)
```

## After All PRs Merge
- This branch (`feat/rest-json-ld-graphql`) becomes obsolete
- Master will contain all changes
- Can delete this branch
