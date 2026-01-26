# FAIRmapper - Implementation Plan

**Spec**: `.plan/specs/fairmapper.md`
**Branch**: `spike/etl_pilot`
**Module**: `backend/molgenis-emx2-fairmapper`

## Context

FAIRmapper enables data managers to create API adapters without Java code. Configure via YAML + JSLT transforms. First use case: Beacon v2 API. Then DCAT

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

## Completed Phases

### Phase 3: CLI Remote Execution ✓ (2026-01-25)
- [x] `GraphqlClient.java` - HTTP client for remote GraphQL
- [x] `RemotePipelineExecutor.java` - pipeline using GraphqlClient
- [x] `e2e` command: `--server`, `--token`, `--schema`, `-v` flags
- [x] Execute GraphQL via `{baseUrl}/{schema}/api/graphql`
- [x] Unit tests: `GraphqlClientTest.java`
- [x] Fixed `individuals-response.jslt`: resultsCount, diseases field
- [x] Updated expected.json for GSSO codesystem

## Next Priorities

### Phase 4: Shell Wrapper ✓ (2026-01-25)
- [x] Split into two modules: `fairmapper-cli` (standalone) + `fairmapper` (server)
- [x] CLI module: 53MB fat JAR (vs 125MB with all deps)
- [x] Create `fairmapper` shell script in fair-mappings/
- [x] Script calls `java -jar` on built JAR
- [ ] Later: add JAR to release artifacts
- [ ] Later: consider GraalVM native image for faster startup

Module structure:
```
fairmapper-cli/    # Standalone CLI (53MB) - models, transforms, remote execution
fairmapper/        # Server integration - depends on cli, adds local GraphQL
```

Usage:
```bash
./gradlew :backend:molgenis-emx2-fairmapper-cli:shadowJar  # build once
fair-mappings/fairmapper --help
fair-mappings/fairmapper e2e beacon-v2 --server http://localhost:8080
```

### Phase 5: DCAT Harvesting - Schema v2 + Fetch Step ✓ (2026-01-26)
- [x] Schema v2: `mappings` replaces `endpoints`, `name` field, backwards compat
- [x] Step strategy pattern: `StepConfig` sealed interface
- [x] `FetchStep`: RDF fetch with JSON-LD framing
- [x] `TransformStep`, `QueryStep`, `MutateStep` records
- [x] `FetchExecutor`: uses FrameDrivenFetcher + JsonLdFramer
- [x] `LocalRdfSource`: reads .ttl files for testing
- [x] `maxDepth` (default 5) + `maxCalls` per record (default 50)
- [x] Deduplication: tracks fetched URIs, avoids re-fetch
- [x] `test` command: runs fetch step tests with .ttl → .json comparison
- [x] `dcat-fdp` bundle with test fixtures

### Phase 6: DCAT Harvesting - Transform + Mutate
- [ ] Create `to-molgenis.jslt` transform (DCAT → Resources schema)
- [ ] Add MutateExecutor for GraphQL mutations
- [ ] Transaction wrapping for batch imports
- [ ] CLI `run` command: full pipeline execution
- [ ] Variable substitution (${SOURCE_URL})

### Phase 7: Task Framework Integration
- [ ] Wrap pipeline in Task for progress tracking
- [ ] Async execution (avoid HTTP timeouts)
- [ ] DB persistence for audit trail
- [ ] Quartz scheduling for cron harvests

### Phase 8: SQL Query Support
- [ ] Add `.sql` file extension support
- [ ] Variable binding (check reports module: `${param}` syntax)
- [ ] PostgreSQL `json_build_object` for direct JSON responses

### Phase 9: Scaling / Chunking
- [ ] Split large inputs into chunks
- [ ] Process chunks with same pipeline
- [ ] Transaction wrapper for mutation batches
- [ ] Pagination support for queries

### Phase 10: Complete Beacon Migration
- [ ] Add more entity types (biosamples, datasets)
- [ ] Add more filter types (age, phenotype, diseases)

## Key Decisions Made

1. **Config file**: `fairmapper.yaml`
2. **Schema**: Flat structure (name, version, endpoints) - no apiVersion/kind
3. **Transform engine**: JSLT with imports, preserves empty arrays. Add more later
4. **Query engine**: GraphQL via molgenis-emx2-graphql. Add sql later
5. **CLI framework**: Picocli with colored output
6. **Distribution**: JAR with shell wrapper
7. **Multi-tenancy**: `{schema}` placeholder in paths

## Running Tests

```bash
# All fairmapper-cli tests (includes RDF, framing, bundle loading)
./gradlew :backend:molgenis-emx2-fairmapper-cli:test

# CLI: validate bundle structure
fair-mappings/fairmapper validate fair-mappings/beacon-v2
fair-mappings/fairmapper validate fair-mappings/dcat-fdp

# CLI: run step tests (transforms + fetch steps)
fair-mappings/fairmapper test fair-mappings/beacon-v2 -v
fair-mappings/fairmapper test fair-mappings/dcat-fdp -v

# CLI: fetch RDF from URL with frame
fair-mappings/fairmapper fetch-rdf https://fdp.health-ri.nl/catalog/5d6f222e-ba32-4930-9e77-d266f5ee64d9 \
  --frame fair-mappings/dcat-fdp/src/frames/catalog-with-datasets.jsonld

# E2e against remote server
fair-mappings/fairmapper e2e fair-mappings/beacon-v2 \
  --server http://localhost:8080 --schema fairmapperTest -v
```

### Build CLI first
```bash
./gradlew :backend:molgenis-emx2-fairmapper-cli:shadowJar
```

## Open Questions

1. SQL variable binding: check reports module for `${param}` syntax
2. Large payload handling: need safety valve for streaming?
3. Hot reload: watch filesystem or explicit reload endpoint?
