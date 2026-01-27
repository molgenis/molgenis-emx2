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
| 8.1 Fix silent failures in jsonEquals | HIGH | Done |
| 8.2 Add null-check to RemotePipelineExecutor | HIGH | Pending |
| 8.3 Command error case tests | HIGH | Pending |
| 8.4 RemotePipelineExecutor tests | MEDIUM | Pending |

---

### 8.1 Fix Silent Failures in jsonEquals

**Problem:** `TestCommand.jsonEquals()` and `E2eCommand.jsonEquals()` catch all exceptions and return false, masking real errors (OOM, parse failures, etc).

**Files:**
| File | Lines | Issue |
|------|-------|-------|
| `commands/TestCommand.java` | 135-142 | `catch (Exception e) { return false; }` |
| `commands/E2eCommand.java` | 181-188 | Same pattern |

**Fix:** Log exception details, distinguish comparison failure from error:

```java
private boolean jsonEquals(JsonNode expected, JsonNode actual) {
  try {
    return expected.equals(actual);
  } catch (Exception e) {
    System.err.println(color("@|red Error comparing JSON: " + e.getMessage() + "|@"));
    return false;
  }
}
```

**Tests:**
- Verify error message shown when comparison throws
- Verify false returned on actual mismatch (no exception)

---

### 8.2 Add Null-Check to RemotePipelineExecutor

**Problem:** `RemotePipelineExecutor.execute()` doesn't check for null steps, unlike `PipelineExecutor`.

**File:** `RemotePipelineExecutor.java`

**Fix:** Add null-check at start of execute():

```java
public JsonNode execute(JsonNode input, Mapping mapping) throws IOException {
  if (mapping.steps() == null || mapping.steps().isEmpty()) {
    return input;
  }
  // ... rest of method
}
```

**Test:** Add test case for mapping with null/empty steps.

---

### 8.3 Command Error Case Tests

**File:** `RunFairMapperTest.java` (extend existing)

| Command | Error Case | Expected |
|---------|------------|----------|
| `validate` | Missing fairmapper.yaml | Exit 1, "not found" message |
| `validate` | Invalid YAML syntax | Exit 1, parse error message |
| `validate` | Missing transform file | Exit 1, file path in error |
| `test` | Malformed test input JSON | Exit 1, parse error |
| `test` | Transform throws exception | Exit 1, transform path in error |
| `dry-run` | Missing input file | Exit 1, file not found |
| `dry-run` | Invalid transform | Exit 1, JSLT error message |
| `run` | Missing --server | Exit 1, "required" message |
| `run` | Invalid server URL | Exit 1, connection error |
| `e2e` | Server unreachable | Exit 1, timeout/connection error |
| `fetch-rdf` | Invalid URL | Exit 1, URL validation error |
| `fetch-rdf` | 404 response | Exit 1, HTTP status in error |

**Implementation:** Create test bundles in `src/test/resources/bundles/`:
- `invalid-yaml/` - syntax error in fairmapper.yaml
- `missing-transform/` - references non-existent .jslt
- `bad-test-input/` - test with malformed JSON

---

### 8.4 RemotePipelineExecutor Tests

**File:** Create `RemotePipelineExecutorTest.java`

| Test | Setup | Assertion |
|------|-------|-----------|
| Query success | Mock GraphqlClient returns data | Result contains data |
| Query error | Mock returns GraphQL errors | IOException thrown with message |
| Mutate success | Mock returns mutation result | Result contains data |
| Transform chain | Multi-step pipeline | Each step executed in order |
| Fetch mapping | Mapping with fetch field | Fetch executed before steps |
| Network failure | Mock throws IOException | IOException propagated |

**Mock setup:**
```java
@Mock GraphqlClient client;

@BeforeEach
void setup() {
  executor = new RemotePipelineExecutor(bundlePath, client);
}
```

---

### Implementation Order

```
8.1 Fix jsonEquals ──> 8.3 Command tests (use fixed error reporting)
        │
        v
8.2 Null-check ──────> 8.4 RemotePipelineExecutor tests
```

**Estimated changes:**
- 8.1: ~10 lines in 2 files
- 8.2: ~5 lines in 1 file
- 8.3: ~200 lines new tests + test fixtures
- 8.4: ~150 lines new test file

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
