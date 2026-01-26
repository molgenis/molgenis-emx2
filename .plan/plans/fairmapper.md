# FAIRmapper - Implementation Plan

**Spec**: `.plan/specs/fairmapper.md`
**Branch**: `spike/etl_pilot`
**Module**: `backend/molgenis-emx2-fairmapper`

## Context

FAIRmapper enables data managers to create API adapters without Java code. Configure via YAML + JSLT transforms. First use case: Beacon v2 API.

## Architecture

```
fair-mappings/beacon-v2/
  fairmapper.yaml              # Bundle config (name, version, endpoints)
  src/transforms/*.jslt        # JSLT transforms
  src/queries/*.gql            # GraphQL queries
  test/*/*.json                # Test fixtures

backend/molgenis-emx2-fairmapper/
  RunFairMapper.java           # Picocli CLI (validate, test, dry-run, e2e)
  BundleLoader.java            # YAML parsing + validation
  JsltTransformEngine.java     # JSLT execution (preserves empty arrays)
  PipelineExecutor.java        # Chains steps: transform → query → transform (local)
  GraphqlClient.java           # HTTP client for remote GraphQL
  RemotePipelineExecutor.java  # Chains steps using GraphqlClient (remote)
  model/
    MappingBundle.java         # record(name, version, endpoints)
    Endpoint.java              # record(path, methods, steps, e2e)
    Step.java                  # record(transform, query, tests)
```

## Completed ✓

### Phase 1: Core Engine
- [x] MappingBundle YAML parser (Jackson)
- [x] JsltTransformEngine with FileResourceResolver for imports
- [x] BundleLoader with validation (files exist, extensions correct)
- [x] PipelineExecutor chains transform → query → transform
- [x] FairMapperApi registers HTTP routes from bundles
- [x] E2eTestRunner for integration tests

### Phase 2: CLI
- [x] Picocli-based CLI with colored output
- [x] `validate` - check bundle structure
- [x] `test` - run step-level unit tests
- [x] `dry-run` - transform input without queries
- [x] `e2e` - run e2e tests against remote MOLGENIS server
- [x] Version from `Version.getVersion()`
- [x] RunFairMapperTest (10 tests)

### Phase 2.5: Schemca Simplification
- [x] Rename mapping.yaml → fairmapper.yaml
- [x] Remove apiVersion, kind, metadata wrapper
- [x] Flat schema: name, version, endpoints
- [x] Update all tests and docs

## Current Sprint: Full Beacon Migration

### Completed
1. [x] Inventory existing Beacon endpoints in `molgenis-emx2-beacon-v2`
2. [x] Add individuals-minimal endpoint to fairmapper bundle
3. [x] Wire FAIRmapper routes parallel to existing BeaconApi
4. [x] Compare outputs: old Java path vs new FAIRmapper path

### Comparison Results (2026-01-25)
Both APIs now produce **identical** output:
- `numTotalResults`: 23 = 23 ✓
- Fields: `[id, sex]`
- Sex ontology: `{"id": "GSSO:124", "label": "assigned male at birth"}`

Key: Added `Individuals_agg { count }` to same GraphQL query for total count.

Test: `BeaconFairMapperComparisonTest.java` in molgenis-emx2-webapi

### Next Steps
5. [ ] Add more entity types (biosamples, datasets)
6. [ ] Add more filter types (age, phenotype, diseases)

### Key Files
- `backend/molgenis-emx2-beacon-v2/src/.../BeaconApi.java` - existing routes
- `backend/molgenis-emx2-beacon-v2/src/.../QueryEntryType.java` - query orchestration
- `backend/molgenis-emx2-webapi/src/.../FairMapperApi.java` - FAIRmapper routes
- `fair-mappings/beacon-v2/` - FAIRmapper bundle

## Future Phases

### Phase 3: CLI Remote Execution ✓
- [x] `GraphqlClient.java` - HTTP client for remote GraphQL
- [x] `RemotePipelineExecutor.java` - pipeline using GraphqlClient
- [x] `e2e` command: `--server`, `--token`, `--schema`, `-v` flags
- [x] Execute GraphQL via `{baseUrl}/{schema}/api/graphql`
- [x] Unit tests: `GraphqlClientTest.java`

Usage:
```bash
./gradlew :backend:molgenis-emx2-fairmapper:run \
  --args="e2e fair-mappings/beacon-v2 --server http://localhost:8080 -v"
```

### Phase 4: SQL Query Support
- [ ] Add `.sql` file extension support
- [ ] Variable binding (check reports module: `${param}` syntax)
- [ ] PostgreSQL `json_build_object` for direct JSON responses

### Phase 5: Transaction Flow (DCAT)
- [ ] Fetch external data → transform → mutation
- [ ] Task integration for async

## Key Decisions Made

1. **Config file**: `fairmapper.yaml` (not mapping.yaml)
2. **Schema**: Flat structure (name, version, endpoints) - no apiVersion/kind
3. **Transform engine**: JSLT with imports, preserves empty arrays
4. **Query engine**: GraphQL via molgenis-emx2-graphql
5. **CLI framework**: Picocli with colored output
6. **Distribution**: JAR with shell wrapper
7. **Multi-tenancy**: `{schema}` placeholder in paths

## Running Tests

```bash
# All fairmapper tests
./gradlew :backend:molgenis-emx2-fairmapper:test

# CLI commands
./gradlew :backend:molgenis-emx2-fairmapper:run --args="validate fair-mappings/beacon-v2"
./gradlew :backend:molgenis-emx2-fairmapper:run --args="test fair-mappings/beacon-v2 -v"

# E2e against remote server (use absolute path for bundle)
./gradlew :backend:molgenis-emx2-fairmapper:run \
  --args="e2e $(pwd)/fair-mappings/beacon-v2 --server http://localhost:8080 --schema fairmapperTest -v"
```

## Open Questions

1. SQL variable binding: check reports module for `${param}` syntax
2. Large payload handling: need safety valve for streaming?
3. Hot reload: watch filesystem or explicit reload endpoint?
