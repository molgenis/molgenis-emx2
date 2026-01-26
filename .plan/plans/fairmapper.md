# FAIRmapper - Implementation Plan

**Spec**: `.plan/specs/fairmapper.md`
**Status**: CLI implemented, ready for full Beacon migration

## Goal
Eliminate Java coding for adapters. Data managers configure transformations via YAML + JSLT or SQL in directory structure with testable cases. Support remote development via CLI.

## Completed

### Phase 1: Core + Beacon Demo ✓
- [x] Example bundle (`fair-mappings/beacon-v2/`)
- [x] Gradle module + JUnit test runner
- [x] MappingBundle YAML parser (Jackson)
- [x] JsltTransformEngine (preserves empty arrays)
- [x] BundleLoader for fairmapper.yaml
- [x] PipelineExecutor (transform → query → transform)
- [x] FairMapperApi HTTP routes
- [x] Validation & error handling
- [x] E2e test framework
- [x] Beacon test migration (individuals)
- [x] JSLT imports (FileResourceResolver)
- [x] Filter parsing in request-to-variables.jslt

### Phase 2: CLI ✓
- [x] Picocli-based CLI with colored output
- [x] `validate` command - check bundle structure
- [x] `test` command - run step-level unit tests
- [x] `dry-run` command - transform input without queries
- [x] `e2e` command (stub - returns code 2)
- [x] Version from gradle
- [x] CLI command tests (RunFairMapperTest)

## Current Sprint

### Full Beacon Migration
- [ ] Additional entity types (biosamples, datasets, runs)
- [ ] More filter types (age, phenotype, diseases)
- [ ] Wire bundle to BeaconApi routes (parallel path)
- [ ] Compare outputs: old vs new path
- [ ] Deprecate Java FilterParser classes

## Next Phases

### Phase 3: CLI Remote Execution
- [ ] `--server` flag for MOLGENIS URL
- [ ] `--token` flag / `MOLGENIS_TOKEN` env var
- [ ] Execute GraphQL via `/{schema}/api/graphql`
- [ ] Implement `e2e` command against remote server

### Phase 4: PostgreSQL JSON Queries
- [ ] Add `/{schema}/api/sql` endpoint
- [ ] Add `sql:` step type to fairmapper.yaml
- [ ] Variable binding mechanism
- [ ] Benchmark vs GraphQL+JSLT

### Phase 5: Transaction Flow (DCAT)
- [ ] HttpClient input adapter
- [ ] RDF input adapter
- [ ] GraphQL mutation output adapter
- [ ] Task integration for async

## Key Files

```
fair-mappings/beacon-v2/
  fairmapper.yaml              # Bundle config
  src/transforms/*.jslt        # JSLT transforms
  src/queries/*.gql            # GraphQL queries
  test/*/*.json                # Test fixtures

backend/molgenis-emx2-fairmapper/
  RunFairMapper.java           # CLI entry point
  BundleLoader.java            # YAML parsing + validation
  JsltTransformEngine.java     # JSLT execution
  PipelineExecutor.java        # Step chaining
  RunFairMapperTest.java       # CLI tests
```

## Open Questions

1. Distribution: JAR with shell wrapper? GraalVM native?
2. Does `/api/graphql` work for remote CLI execution?
3. Variable binding for SQL: `:name` or `${name}`?
4. Hot reload: watch filesystem or explicit?
