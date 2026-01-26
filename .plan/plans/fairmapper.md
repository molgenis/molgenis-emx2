# FAIRmapper - Implementation Plan v1.6.1

**Spec**: `.plan/specs/fairmapper.md`
**Branch**: `spike/etl_pilot`
**Module**: `backend/molgenis-emx2-fairmapper`

## Completed Phases

| Phase | Summary |
|-------|---------|
| 1 | Core engine: JSLT transforms, bundle loading |
| 2 | CLI: validate, test, dry-run commands (Picocli) |
| 3 | E2e testing against remote MOLGENIS |
| 4 | Schema v2: `mappings` replacing `endpoints`, step strategy pattern |
| 5 | FetchStep: RDF fetch + JSON-LD framing, dcat-fdp bundle |

**Key learnings**:
- JSLT `get-key(., "@id")` required for JSON-LD keys (bracket notation fails)
- Sealed interface pattern works well for step types
- Local RDF source enables offline testing of fetch steps

---

## Phase 6 Progress

| Story | Status | Notes |
|-------|--------|-------|
| 6.1 to-molgenis.jslt | ✅ DONE | JSLT transform complete, tests pass |
| 6.2 MutateStep + CLI run | ✅ DONE | Full pipeline: fetch→transform→mutate |

### 6.1 Learnings
- JSLT bracket notation `.["@id"]` doesn't work for special keys
- Must use `get-key(., "@id")` for JSON-LD properties with `:` or `@`

### 6.2 Implementation
- MutateStep.java: record with path
- StepConfigDeserializer: parses `mutate:` key
- BundleLoader: validates .gql files
- RemotePipelineExecutor: handles MutateStep
- RunCommand in RunFairMapper.java
- upsert-resources.gql: MOLGENIS insert mutation
- fairmapper.yaml: complete pipeline

---

## NEXT: Manual Testing

### Dry run (no mutation)
```bash
./fairmapper run fair-mappings/dcat-fdp harvest-catalog \
  --source https://fdp.health-ri.nl/catalog/5d6f222e-ba32-4930-9e77-d266f5ee64d9 \
  --schema catalogue \
  --dry-run -v
```

### Full run (with mutation)
```bash
./fairmapper run fair-mappings/dcat-fdp harvest-catalog \
  --source https://fdp.health-ri.nl/catalog/5d6f222e-ba32-4930-9e77-d266f5ee64d9 \
  --server http://localhost:8080 \
  --schema catalogue \
  --token <token> -v
```

---

## Context (for fresh start)

FAIRmapper = YAML + JSLT config for data pipelines without Java code.

**Architecture**:
```
fair-mappings/dcat-fdp/
  fairmapper.yaml           # Bundle config
  src/frames/*.jsonld       # JSON-LD frames for RDF
  src/transforms/*.jslt     # JSLT transforms
  src/mutations/*.gql       # GraphQL mutations
  test/*/*.json             # Test fixtures

backend/molgenis-emx2-fairmapper-cli/
  RunFairMapper.java        # Picocli CLI (validate, test, run, e2e)
  BundleLoader.java         # YAML parsing + validation
  JsltTransformEngine.java  # JSLT execution
  GraphqlClient.java        # HTTP client for GraphQL
  executor/FetchExecutor    # RDF fetch + JSON-LD framing
  model/step/
    StepConfig.java         # Sealed interface
    FetchStep.java          # RDF fetch + frame
    TransformStep.java      # JSLT transform
    QueryStep.java          # GraphQL query
    MutateStep.java         # GraphQL mutation
```

**dcat-fdp bundle**:
```yaml
name: dcat-fdp
version: 1.0.0
mappings:
  - name: harvest-catalog
    steps:
      - fetch: ${SOURCE_URL}
        frame: src/frames/catalog-with-datasets.jsonld
      - transform: src/transforms/to-molgenis.jslt
      - mutate: src/mutations/upsert-resources.gql
```

---

## Running Tests

```bash
./gradlew :backend:molgenis-emx2-fairmapper-cli:test
./fairmapper test fair-mappings/dcat-fdp -v
```

---

## Future Phases

- Phase 7: Task framework + async execution
- Phase 8: SQL query support
- Phase 9: Chunking/pagination
- Phase 10: Complete Beacon migration
