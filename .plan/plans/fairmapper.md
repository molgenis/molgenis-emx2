# FAIRmapper - Implementation Plan v2.0.0

**Spec**: `.plan/specs/fairmapper.md`
**Branch**: `spike/etl_pilot`
**Module**: `backend/molgenis-emx2-fairmapper-cli`

## Completed Phases

| Phase | Summary |
|-------|---------|
| 1 | Core engine: JSLT transforms, bundle loading |
| 2 | CLI: validate, test, dry-run commands (Picocli) |
| 3 | E2e testing against remote MOLGENIS |
| 4 | Schema v2: `mappings` replacing `endpoints`, step strategy pattern |
| 5 | RDF fetch + JSON-LD framing, dcat bundles |
| 6.1 | to-molgenis.jslt transform |
| 6.2 | MutateStep + CLI run command |
| 6.3 | Documentation (docs/fairmapper/) |
| 6.4 | Security: path traversal, SSRF, size limits, error handling, retry |
| 6.5 | Split RunFairMapper.java into commands/ subpackage |
| 7.3 | RDF output via content negotiation |
| 7.5 | FDP DCAT publish endpoints |
| 7.6 | Schema v4: simplified Mapping model |

---

## Current State (v4 Schema)

### Mapping Types

**API mapping** (exposes HTTP endpoint):
```yaml
- name: fdp-catalog
  endpoint: /{schema}/api/fdp/catalog/{id}
  methods: [GET]
  output: turtle
  steps:
    - query: src/queries/get-catalog.gql
    - transform: src/transforms/to-dcat-catalog.jslt
```

**Harvest mapping** (fetches from external RDF):
```yaml
- name: harvest-catalog
  fetch: ${SOURCE_URL}
  frame: src/frames/catalog.jsonld
  steps:
    - transform: src/transforms/to-molgenis.jslt
    - mutate: src/mutations/upsert.gql
```

### Validation Rules
- `name` always required
- `endpoint` OR `fetch` (mutually exclusive)
- `frame` required when `fetch` is set

---

## Phase 8: Test Coverage & Error Handling

| Task | Priority | Status |
|------|----------|--------|
| Command integration tests | HIGH | Pending |
| Fix exception hierarchy | HIGH | Pending |
| Fix silent partial failures | HIGH | Pending |
| RemotePipelineExecutor tests | MEDIUM | Pending |

---

## Phase 9: DCAT Completeness

| Task | Priority | Status |
|------|----------|--------|
| Add dcat:distribution layer | HIGH | Pending |
| Fix hardcoded timestamps | HIGH | Pending |
| Add dct:accessRights | MEDIUM | Pending |
| Extract shared FDP context | MEDIUM | Pending |
| SHACL validation step | MEDIUM | Pending |

---

## Phase 10: Transform Simplification

| Task | Priority | Status |
|------|----------|--------|
| Declarative field mapping | HIGH | Pending |
| JSON-LD compaction step | MEDIUM | Pending |
| JSLT-Python cheatsheet | LOW | Pending |

---

## Future Ideas

### Developer Experience
- **Dev server mode** - `fairmapper serve` with hot-reload

### Data Sources
- **SQL query support** - Direct PostgreSQL queries
- **CSV source** - For schema migrations

### Scalability
- **Chunking/pagination** - For large datasets
- **Task framework + async** - Background execution

### Scripting
- **GraalPy** - Python transforms (data managers prefer Python)
- **SPARQL CONSTRUCT** - Alternative to JSON-LD framing

### Rejected
- **JSONata** - No improvement over JSLT (see design decisions in spec)

---

## Architecture

```
fair-mappings/
  dcat-fdp/              # FDP publish bundle (API mappings)
  dcat-harvester/        # DCAT harvest bundle (harvest mapping)
  beacon-v2/             # Beacon API bundle

backend/molgenis-emx2-fairmapper-cli/
  RunFairMapper.java     # CLI entry point
  commands/              # Picocli subcommands
  model/
    Mapping.java         # Mapping record with validation
    MappingBundle.java   # Bundle config
    step/
      StepConfig.java    # Sealed interface
      TransformStep.java
      QueryStep.java
      MutateStep.java
  executor/
    PipelineExecutor.java
    RemotePipelineExecutor.java

backend/molgenis-emx2-fairmapper/
  FairMapperApi.java     # Spark routes + content negotiation
  ContentNegotiator.java # Accept/Content-Type handling
```

---

## Running Tests

```bash
./gradlew :backend:molgenis-emx2-fairmapper-cli:test
./fairmapper validate fair-mappings/dcat-fdp
./fairmapper test fair-mappings/dcat-fdp -v
```
