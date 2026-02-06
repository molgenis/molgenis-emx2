# FAIRmapper - Implementation Plan v3.0.0

**Spec**: `.plan/specs/fairmapper.md`
**Branch**: `spike/etl_pilot`
**Module**: `backend/molgenis-emx2-fairmapper-cli`

## Current Status

### Completed
- Core engine: JSLT transforms, bundle loading, pipeline execution
- CLI: validate, test, dry-run, run, e2e commands (Picocli)
- Step types: QueryStep, TransformStep, MutateStep, FrameStep, SqlQueryStep, SparqlConstructStep, MappingStep
- Security: path traversal, SSRF, size limits, error handling
- RDF: fetch, frame, content negotiation (turtle/jsonld)
- MappingStep: declarative YAML mapping for export (MOLGENIS → RDF)
- E2E test for dcat-via-mapping (MappingStep)

### In Progress
- Fixing integration tests after bundle cleanup (backend agent running)

### Bundle Cleanup Done
Reduced from 8 to 4 example bundles:

| Bundle | Purpose | Approach |
|--------|---------|----------|
| beacon-v2 | Beacon API export | GraphQL + JSLT |
| dcat-fdp-sql | DCAT FDP export | SQL |
| dcat-harvester | DCAT import | Frame + JSLT |
| dcat-via-mapping | DCAT export | MappingStep (YAML) |

**Deleted:** dcat-fdp, dcat-harvester-auto, dcat-harvester-framed, dcat-sparql

### Folder Structure (Simplified)
```
fair-mappings/
├── {bundle}/
│   ├── fairmapper.yaml
│   ├── src/           # all source files flat
│   └── test/          # test fixtures
```

---

## Next Steps

### 1. Fix Integration Tests (In Progress)
Update tests referencing deleted bundles to use remaining bundles.

### 2. Endpoint Naming Convention
**Current:** Each bundle defines arbitrary paths like `/{schema}/api/beacon/individuals`
**Proposed:** Standard path `/api/fair/{bundle}/{mapping}?schema=X`

Benefits:
- No route conflicts
- Clear ownership
- Easy discovery
- Schema as query param

### 3. Framework Hardening
| Task | Priority |
|------|----------|
| Fix hardcoded paths in tests | Done |
| Update shadow plugin | Done |
| Fix JSLT for empty filters | Done |
| Update BeaconBundleTest to new model | Done |
| Route validation (detect duplicates) | Pending |
| Error handling improvements | Pending |

### 4. Deferred (Not Core Framework)
- Auto mode (needs MOLGENIS semantic endpoints)
- DCAT completeness (distribution layer, timestamps)
- SHACL validation step
- CSV zip output

---

## Architecture

```
backend/molgenis-emx2-fairmapper-cli/
├── RunFairMapper.java         # CLI entry
├── commands/                  # Picocli subcommands
├── model/
│   ├── Mapping.java          # Mapping record
│   ├── MappingBundle.java    # Bundle config
│   └── step/                 # Step types (sealed interface)
├── MappingEngine.java        # YAML mapping → JSON-LD
├── JsltTransformEngine.java  # JSLT execution
├── rdf/                      # RDF fetch, frame, SPARQL
└── executor/
    ├── PipelineExecutor.java
    └── RemotePipelineExecutor.java

backend/molgenis-emx2-fairmapper/
├── FairMapperApi.java        # Spark routes
└── PipelineExecutor.java     # Server-side execution
```

---

## Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Transform language | JSLT | JSON→JSON, well-documented |
| Mapping format | YAML | Declarative, readable |
| RDF library | RDF4J | Already in MOLGENIS |
| CLI framework | Picocli | Annotations, easy subcommands |
| Step model | Sealed interface | Type-safe, extensible |

---

## Running

```bash
# CLI
./gradlew :backend:molgenis-emx2-fairmapper-cli:shadowJar
java -jar backend/molgenis-emx2-fairmapper-cli/build/libs/fairmapper.jar validate fair-mappings/beacon-v2

# Tests
./gradlew :backend:molgenis-emx2-fairmapper-cli:test
./gradlew :backend:molgenis-emx2-fairmapper:test
```
