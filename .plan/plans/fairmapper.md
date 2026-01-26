# FAIRmapper - Implementation Plan v1.7.0

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
| 6.1 | to-molgenis.jslt transform |
| 6.2 | MutateStep + CLI run command |

---

## Phase 6.3: Documentation (DONE)

Created top-level docs/fairmapper/ section.

| Task | Status | Notes |
|------|--------|-------|
| Create docs/fairmapper/ structure | ✅ | README, _sidebar |
| Getting Started guide | ✅ | Step-by-step tutorial |
| Schema Reference | ✅ | fairmapper.yaml complete reference |
| Troubleshooting guide | ✅ | Common errors + solutions |
| Update nav to include FAIRmapper | ✅ | _sidebar.md, _navbar.md |
| Redirect old use_fairmapper.md | ✅ | Points to new docs |

---

## Phase 6.4: Security Fixes

Critical issues from code review.

| Task | Priority | Notes |
|------|----------|-------|
| Path traversal protection | CRITICAL | Validate resolved paths stay within bundle |
| SSRF protection | CRITICAL | Whitelist schemes, block private IPs |
| LocalRdfSource validation | CRITICAL | Prevent arbitrary file reads |
| Size limits on fetch | HIGH | Prevent OOM from large RDF |
| Error handling in FrameDrivenFetcher | HIGH | Use logger, don't swallow errors |
| Retry logic for transient failures | MEDIUM | 429, 503, timeouts |

---

## Phase 7+: Future

- Phase 7: Task framework + async execution
- Phase 8: SQL query support
- Phase 9: Chunking/pagination
- Phase 10: Complete Beacon migration

---

## Code Review Findings (2024-01-26)

### Security Issues
1. **Path traversal** - `bundlePath.resolve("../../../etc/passwd")` escapes bundle
   - BundleLoader.java:115,128,140,178
   - RemotePipelineExecutor.java:62,67,73
   - JsltTransformEngine.java:51
   - PipelineExecutor.java:43,48

2. **SSRF** - RdfFetcher.java:27-34 allows internal network access

3. **LocalRdfSource** - Can read arbitrary files

### Error Handling
- FrameDrivenFetcher:56-63 swallows errors silently
- No retry logic anywhere
- No size limits (OOM risk)

### Code Quality
- RunFairMapper.java is 891 lines (should split)
- Code duplication: RemotePipelineExecutor vs PipelineExecutor
- Multiple ObjectMapper instances (should share)
- Magic numbers (maxCalls: 50)

### Test Gaps
- No security tests (path traversal, SSRF)
- No RunCommand integration test
- Limited JSLT edge case tests

---

## Documentation Review Findings (2024-01-26)

### Missing
- Getting Started tutorial
- fairmapper.yaml complete schema reference
- Troubleshooting guide
- JSON-LD frames explanation

### Outdated
- use_fairmapper.md says fetch/mutate "coming soon" (they exist!)
- CLI commands inconsistent across docs

### Confusing
- Three different ways to run commands
- Assumes JSON-LD/RDF knowledge

---

## Context (for fresh start)

FAIRmapper = YAML + JSLT config for data pipelines without Java code.

**Architecture**:
```
docs/fairmapper/           # NEW: Top-level docs
  README.md                # Overview + quick start
  getting_started.md       # Step-by-step tutorial
  schema_reference.md      # fairmapper.yaml spec
  troubleshooting.md       # Common errors
  _sidebar.md              # Navigation

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
