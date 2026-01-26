# FAIRmapper - Implementation Plan v1.8.1

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
| 6.3 | Documentation (docs/fairmapper/) |
| 6.4 | Security: path traversal, SSRF, size limits, error handling, retry |

---

## Phase 6.4: Security Fixes ✅ COMPLETE

| Task | Priority | Status |
|------|----------|--------|
| Path traversal protection | CRITICAL | ✅ Done (6.4.1) |
| SSRF protection | CRITICAL | ✅ Done (6.4.2) |
| LocalRdfSource validation | CRITICAL | ✅ Done (6.4.3) |
| Size limits on fetch | HIGH | ✅ Done (6.4.4) |
| Error handling in FrameDrivenFetcher | HIGH | ✅ Done (6.4.5) |
| Retry logic for transient failures | MEDIUM | ✅ Done (6.4.6) |

---

## Phase 6.4.3: LocalRdfSource Validation (CRITICAL)

**Problem**: `LocalRdfSource.java:20` does `basePath.resolve(pathOrUrl)` without path traversal check. Attacker could use `../../../etc/passwd` to read arbitrary files.

**Solution**: Use existing `PathValidator.validateWithinBase()` before resolving.

### Files to Modify

| File | Change |
|------|--------|
| `LocalRdfSource.java` | Add PathValidator call before resolve |

### Implementation

```java
// LocalRdfSource.java:19-20
@Override
public Model fetch(String pathOrUrl) throws IOException {
  Path filePath = PathValidator.validateWithinBase(basePath, pathOrUrl);
  try (InputStream in = Files.newInputStream(filePath)) {
```

### Test Cases

| Test | Input | Expected |
|------|-------|----------|
| Valid path | `test/data.ttl` | Pass |
| Traversal attack | `../../../etc/passwd` | FairMapperException |
| Absolute path | `/etc/passwd` | FairMapperException |

### Verification
```bash
./gradlew :backend:molgenis-emx2-fairmapper-cli:test --tests "*LocalRdfSource*"
```

---

## Phase 6.4.4: Size Limits on Fetch (HIGH)

**Problem**: `RdfFetcher.java` uses `BodyHandlers.ofString()` which loads entire response into memory. No size limit → OOM risk with large/malicious responses.

**Solution**: Add 10MB default limit, configurable via constructor.

### Files to Modify

| File | Change |
|------|--------|
| `RdfFetcher.java` | Add maxBytes parameter, check Content-Length, use limited body handler |

### Implementation

```java
public class RdfFetcher implements RdfSource {
  private static final long DEFAULT_MAX_BYTES = 10 * 1024 * 1024; // 10MB
  private final long maxBytes;

  public RdfFetcher(UrlValidator urlValidator) {
    this(urlValidator, DEFAULT_MAX_BYTES);
  }

  public RdfFetcher(UrlValidator urlValidator, long maxBytes) {
    this.urlValidator = urlValidator;
    this.maxBytes = maxBytes;
    // ...
  }

  @Override
  public Model fetch(String url) throws IOException {
    // ... existing validation ...

    HttpResponse<String> response = httpClient.send(request,
        BodyHandlers.ofString());

    // Check Content-Length header
    response.headers().firstValueAsLong("Content-Length").ifPresent(len -> {
      if (len > maxBytes) {
        throw new FairMapperException("Response too large: " + len + " bytes (max: " + maxBytes + ")");
      }
    });

    String body = response.body();
    if (body.length() > maxBytes) {
      throw new FairMapperException("Response body too large: " + body.length() + " bytes");
    }

    return parseTurtle(body);
  }
}
```

### Test Cases

| Test | Expected |
|------|----------|
| Normal response (1KB) | Pass |
| Response > maxBytes | FairMapperException |
| Content-Length > maxBytes | FairMapperException (early fail) |

### Verification
```bash
./gradlew :backend:molgenis-emx2-fairmapper-cli:test --tests "*RdfFetcher*"
```

---

## Phase 6.4.5: Error Handling in FrameDrivenFetcher (HIGH)

**Problem**: `FrameDrivenFetcher.java:61-62` catches IOException and just prints to stderr. Silent failures are hard to debug.

**Solution**: Add configurable error handling via callback/enum.

### Files to Modify

| File | Change |
|------|--------|
| `FrameDrivenFetcher.java` | Add ErrorHandler enum, configurable behavior |

### Implementation

```java
public enum FetchErrorBehavior {
  WARN_AND_CONTINUE,  // Current behavior (default)
  FAIL_FAST           // Throw on first error
}

public class FrameDrivenFetcher {
  private final FetchErrorBehavior errorBehavior;
  private static final Logger log = LoggerFactory.getLogger(FrameDrivenFetcher.class);

  public FrameDrivenFetcher(RdfSource source, FrameAnalyzer analyzer) {
    this(source, analyzer, FetchErrorBehavior.WARN_AND_CONTINUE);
  }

  public FrameDrivenFetcher(RdfSource source, FrameAnalyzer analyzer,
                             FetchErrorBehavior errorBehavior) {
    this.errorBehavior = errorBehavior;
    // ...
  }

  // In fetch(), line 61-62:
  } catch (IOException e) {
    if (errorBehavior == FetchErrorBehavior.FAIL_FAST) {
      throw new FairMapperException("Failed to fetch " + uri, e);
    }
    log.warn("Failed to fetch {}: {}", uri, e.getMessage());
  }
}
```

### Test Cases

| Test | ErrorBehavior | Expected |
|------|---------------|----------|
| Fetch failure | WARN_AND_CONTINUE | Logged, continues |
| Fetch failure | FAIL_FAST | FairMapperException thrown |

### Verification
```bash
./gradlew :backend:molgenis-emx2-fairmapper-cli:test --tests "*FrameDrivenFetcher*"
```

---

## Phase 6.4.6: Retry Logic for Transient Failures (MEDIUM)

**Problem**: No retry on transient HTTP errors (5xx, timeouts). Single failure aborts entire fetch.

**Solution**: Add exponential backoff retry for transient errors.

### Files to Modify

| File | Change |
|------|--------|
| `RdfFetcher.java` | Add retry logic with exponential backoff |

### Implementation

```java
private static final int MAX_RETRIES = 3;
private static final int BASE_DELAY_MS = 1000;

@Override
public Model fetch(String url) throws IOException {
  urlValidator.validate(url);

  IOException lastException = null;
  for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
    try {
      return doFetch(url);
    } catch (IOException e) {
      if (!isTransientError(e) || attempt == MAX_RETRIES) {
        throw e;
      }
      lastException = e;
      log.warn("Fetch attempt {} failed for {}: {}, retrying...",
               attempt, url, e.getMessage());
      sleep(BASE_DELAY_MS * (1 << (attempt - 1))); // exponential backoff
    }
  }
  throw lastException;
}

private boolean isTransientError(IOException e) {
  String msg = e.getMessage();
  if (msg == null) return false;
  return msg.contains("status 5")
      || msg.contains("timed out")
      || msg.contains("Connection reset");
}

private Model doFetch(String url) throws IOException {
  // ... existing fetch logic ...
}
```

### Test Cases

| Test | Expected |
|------|----------|
| 500 error, then success | Retry succeeds |
| 500 error x3 | Fail after 3 attempts |
| 404 error | Fail immediately (not transient) |
| Timeout, then success | Retry succeeds |

### Verification
```bash
./gradlew :backend:molgenis-emx2-fairmapper-cli:test --tests "*RdfFetcher*"
```

---

## Implementation Order

1. **6.4.3 LocalRdfSource** (~5 lines change, trivial)
2. **6.4.4 Size limits** (~15 lines change)
3. **6.4.5 Error handling** (~20 lines change)
4. **6.4.6 Retry logic** (~30 lines change)

Total: ~70 lines of changes + tests

---

## Phase 6.5: Code Quality

| Task | Priority | Status |
|------|----------|--------|
| Split RunFairMapper.java (891 lines) | MEDIUM | ✅ Done (6.5.1) |
| Merge RemotePipelineExecutor + PipelineExecutor | MEDIUM | ⏭ Skipped (minimal duplication) |
| Share ObjectMapper instances | LOW | Pending (6.5.3) |
| Extract magic numbers to constants | LOW | Pending (6.5.4) |
| Add RunCommand integration test | MEDIUM | Pending (6.5.5) |

---

## Phase 6.5.1: Split RunFairMapper.java (CURRENT)

**Problem**: RunFairMapper.java is 891 lines with 6 nested command classes. Hard to navigate and maintain.

**Solution**: Extract each command to its own file in `commands/` subpackage.

### Current Structure (891 lines)

| Class | Lines | Purpose |
|-------|-------|---------|
| `RunFairMapper` | ~50 | Main entry, color helper |
| `ValidateCommand` | ~77 | Bundle validation |
| `TestCommand` | ~160 | Unit test runner |
| `DryRunCommand` | ~105 | Transform dry-run |
| `E2eCommand` | ~173 | E2e test runner |
| `FetchRdfCommand` | ~90 | RDF fetch utility |
| `RunCommand` | ~171 | Live pipeline execution |

### Target Structure

```
fairmapper/
  RunFairMapper.java          (~80 lines - main + shared helpers)
  commands/
    ValidateCommand.java      (~80 lines)
    TestCommand.java          (~165 lines)
    DryRunCommand.java        (~110 lines)
    E2eCommand.java           (~180 lines)
    FetchRdfCommand.java      (~95 lines)
    RunCommand.java           (~175 lines)
```

### Files to Create

| File | Content |
|------|---------|
| `commands/ValidateCommand.java` | Extract from lines 76-157 |
| `commands/TestCommand.java` | Extract from lines 159-323 |
| `commands/DryRunCommand.java` | Extract from lines 325-434 |
| `commands/E2eCommand.java` | Extract from lines 436-613 |
| `commands/FetchRdfCommand.java` | Extract from lines 615-709 |
| `commands/RunCommand.java` | Extract from lines 711-886 |

### Files to Modify

| File | Change |
|------|--------|
| `RunFairMapper.java` | Remove nested classes, update subcommands reference, add shared static helpers |

### Shared Helpers (stay in RunFairMapper)

```java
public static String color(String text) {
  return CommandLine.Help.Ansi.AUTO.string(text);
}

public static Path resolveConfigPath(Path bundlePath) {
  return bundlePath.resolve("fairmapper.yaml");
}
```

### Implementation Notes

1. Each command becomes a top-level class
2. Change `static class` to `public class`
3. Import `RunFairMapper.color()` and `RunFairMapper.resolveConfigPath()`
4. Keep `@Command` annotations as-is
5. Update `RunFairMapper.@Command.subcommands` to use full class paths

### Verification

```bash
./gradlew :backend:molgenis-emx2-fairmapper-cli:test
./fairmapper validate fair-mappings/dcat-fdp
./fairmapper test fair-mappings/dcat-fdp
```

---

## Phase 7: Transform Engine Enhancements

| Task | Priority | Status |
|------|----------|--------|
| JSONata transform engine | HIGH | ❌ Rejected (7.1) |
| CSV source step | HIGH | Pending (7.2) |
| RDF output step | MEDIUM | ✅ Done (7.3) |
| Declarative field mapping | HIGH | Pending (7.4) |
| FDP publish endpoints | MEDIUM | ✅ Done (7.5) |

---

## Phase 7.1: JSONata Transform Engine - REJECTED

### Design Decision: Why JSONata Was Not Added

**Evaluated**: JSONata as alternative to JSLT using [dashjoin/jsonata-java](https://github.com/dashjoin/jsonata-java)

**Implementation**: Fully implemented and tested, then removed after comparison.

**Findings**:

| Aspect | JSLT | JSONata | Verdict |
|--------|------|---------|---------|
| Variable binding | `let x = ...` | `$x := ...` | JSLT clearer |
| Array iteration | `[for ($arr) {...}]` | `arr.{...}` | JSLT more explicit |
| Functions | `def name(x) ...` | `$name := function($x){...}` | JSLT cleaner |
| String concat | `+` | `&` | JSLT intuitive |
| Comments | `// supported` | Not supported | JSLT wins |

**Also considered**: Python/JavaScript scripting
- More readable for complex logic (`.get()` handles nulls, list comprehensions)
- But: security concerns, sandboxing complexity, not declarative

**Root cause analysis**: The DCAT `to-molgenis.jslt` transform is "ugly" not because of JSLT, but because:
1. JSON-LD has prefixed keys (`dcterms:title`) requiring `get-key()` everywhere
2. JSON-LD array ambiguity (single item vs array) needs normalization
3. Significant semantic gap between DCAT ontology and MOLGENIS schema

**Conclusion**: Another expression language doesn't solve the problem. The complexity should be in **declarative mapping configuration**, not transform code.

**Decision**: Keep JSLT only, focus on declarative field mapping (Phase 7.4) instead.

### Also Considered: Python and Rule Engines

**GraalPy (Python on GraalVM)**:
- Production ready since 2024 for pure Python code
- ~4x faster than CPython when JIT-compiled
- Native extensions (NumPy) still experimental
- Data managers like Python
- Concern: sandboxing complexity, cold start performance

**Rule Engines (Drools, Easy Rules)**:
- Drools: powerful but complex, enterprise-focused
- Easy Rules: lightweight but limited
- Both require learning new DSL
- Better for business rules than data transformation

**Conclusion**: Worth exploring GraalPy for complex transforms where data managers prefer Python. Rule engines less suitable for ETL use cases. Parked for future consideration.

---

## Phase 7.2: CSV Source Step

**Goal**: Enable CSV files as data source for schema migrations.

### Files to Create

| File | Purpose |
|------|---------|
| `CsvFetcher.java` | Load CSV, convert to JSON array |
| `CsvFetcherTest.java` | Unit tests |

### fairmapper.yaml syntax

```yaml
steps:
  - fetch-csv: data/input.csv
    options:
      delimiter: ","
      headers: true
  - transform: src/transforms/normalize.jslt
  - mutate: src/mutations/upsert.gql
```

---

## Phase 7.3: RDF Output Step ✅ COMPLETE

**Goal**: Generate RDF/JSON-LD output for DCAT publishing.

### Files Created

| File | Purpose |
|------|---------|
| `OutputRdfStep.java` | Step config record |
| `JsonLdToRdf.java` | JSON-LD to RDF conversion using RDF4J |
| `JsonLdToRdfTest.java` | Unit tests |

### fairmapper.yaml syntax

```yaml
steps:
  - query: src/queries/get-resources.gql
  - transform: src/transforms/to-dcat.jslt
  - output-rdf: turtle  # or jsonld, ntriples
```

Supported formats: turtle, jsonld, ntriples (case-insensitive)

---

## Phase 7.4: Declarative Field Mapping (Future Consideration)

**Goal**: Simple YAML-based field mapping for 80% of use cases without writing JSLT.

### Proposed Syntax

```yaml
steps:
  - mapping:
      source-type: "dcat:Catalog"
      target-table: Resources
      fields:
        - from: "dcterms:title"
          to: name
        - from: "dcterms:description"
          to: description
        - from: "@id"
          to: id
          transform: extract-url-suffix
          prefix: "catalog-"
        - from: "@type"
          to: type
          lookup:
            "dcat:Catalog": [{name: "Catalogue"}]
            "dcat:Dataset": [{name: "Databank"}]
        - from: "dcat:keyword"
          to: keywords
          ensure-array: true
```

### Benefits

- No programming required for simple field mapping
- Declarative = easier to validate and analyze
- Lookup tables for value mapping
- Built-in transforms (extract-url-suffix, ensure-array, prefix)
- Falls back to JSLT for complex cases

### Implementation Complexity

- Need to design complete mapping DSL
- Parse and execute mapping config
- Define built-in transforms
- Handle nested objects and arrays

### Status

**Not yet planned** - Needs requirements analysis and design. User considering options.

---

## Phase 7.5: FDP DCAT Publish Endpoints ✅ COMPLETE

**Goal**: Add FDP-compliant endpoints to dcat-fdp bundle for DCAT publishing from MOLGENIS data.

### Implementation

Renamed `fair-mappings/dcat-fdp` → `fair-mappings/dcat-harvester` (for harvesting external FDP).
Created new `fair-mappings/dcat-fdp` bundle for publishing MOLGENIS data as FDP.

**Spec**: [FDP Specification](https://specs.fairdatapoint.org/) | [GitHub](https://github.com/FAIRDataTeam/FAIRDataPoint-Spec)

### FDP Layer Structure

| Layer | RDF Type | Parent | Required Properties |
|-------|----------|--------|---------------------|
| FDP Root | `r3d:Repository` | - | title, publisher, metadataIdentifier, r3d:dataCatalog |
| Catalog | `dcat:Catalog` | FDP | title, publisher, dct:isPartOf, dcat:dataset |
| Dataset | `dcat:Dataset` | Catalog | title, publisher, dct:isPartOf, dcat:theme |
| Distribution | `dcat:Distribution` | Dataset | (not in scope) |

### New Endpoints

| Path | Method | Output | Description |
|------|--------|--------|-------------|
| `/{schema}/fdp` | GET | Turtle/JSON-LD | FDP root with catalog links |
| `/{schema}/fdp/catalog/{id}` | GET | Turtle/JSON-LD | Catalog with dataset links |
| `/{schema}/fdp/dataset/{id}` | GET | Turtle/JSON-LD | Dataset metadata |

### Implementation Approach

**Option A: FAIRmapper bundle** (preferred for flexibility)
- Add mappings to `fair-mappings/dcat-fdp/`
- Use JSLT transforms to convert MOLGENIS → DCAT RDF
- Requires RDF output step (Phase 7.3)

**Option B: Direct Java API** (faster, less flexible)
- Add endpoints to existing `RDFApi.java`
- Use existing `Emx2RdfGenerator` patterns

### fairmapper.yaml Structure

```yaml
name: dcat-fdp
version: 1.1.0

mappings:
  # Existing: harvest external FDP
  - name: harvest-catalog
    steps:
      - fetch: ${SOURCE_URL}
        frame: src/frames/catalog-with-datasets.jsonld
      - transform: src/transforms/to-molgenis.jslt
      - mutate: src/mutations/upsert-resources.gql

  # NEW: publish as FDP
  - name: fdp-root
    endpoint: /{schema}/fdp
    methods: [GET]
    steps:
      - query: src/queries/get-schema-metadata.gql
      - transform: src/transforms/publish/to-fdp-root.jslt

  - name: fdp-catalog
    endpoint: /{schema}/fdp/catalog/{id}
    methods: [GET]
    steps:
      - query: src/queries/get-catalog.gql
      - transform: src/transforms/publish/to-dcat-catalog.jslt

  - name: fdp-dataset
    endpoint: /{schema}/fdp/dataset/{id}
    methods: [GET]
    steps:
      - query: src/queries/get-dataset.gql
      - transform: src/transforms/publish/to-dcat-dataset.jslt
```

### Files to Create

```
fair-mappings/dcat-fdp/
  src/
    queries/
      get-schema-metadata.gql      # Schema info for FDP root
      get-catalog.gql              # Single catalog by ID
      get-dataset.gql              # Single dataset by ID
    transforms/publish/
      to-fdp-root.jslt             # → r3d:Repository
      to-dcat-catalog.jslt         # → dcat:Catalog
      to-dcat-dataset.jslt         # → dcat:Dataset
  test/publish/
    fdp-root/
    catalog/
    dataset/
```

### JSLT Transform: to-fdp-root.jslt

```jslt
let baseUrl = .baseUrl
let schema = .schema

{
  "@context": {
    "dcat": "http://www.w3.org/ns/dcat#",
    "dct": "http://purl.org/dc/terms/",
    "r3d": "http://www.re3data.org/schema/3-0#",
    "fdp": "https://w3id.org/fdp/fdp-o#"
  },
  "@id": $baseUrl + "/" + $schema.name + "/fdp",
  "@type": "r3d:Repository",
  "dct:title": $schema.name,
  "dct:description": $schema.description,
  "dct:publisher": {
    "@id": $baseUrl,
    "@type": "foaf:Organization"
  },
  "fdp:metadataIdentifier": {"@id": $baseUrl + "/" + $schema.name + "/fdp"},
  "fdp:metadataIssued": now(),
  "r3d:dataCatalog": [for (.catalogs) {
    "@id": $baseUrl + "/" + $schema.name + "/fdp/catalog/" + .id
  }]
}
```

### Prerequisites

1. **Phase 7.3 (RDF output)** - Need to serialize JSON-LD to Turtle
2. **Content negotiation** - Return Turtle or JSON-LD based on Accept header
3. **GraphQL queries** - Fetch Resources by type (Catalogue/Databank)

### Verification

```bash
./fairmapper test fair-mappings/dcat-fdp -v
curl -H "Accept: text/turtle" http://localhost:8080/catalogue/fdp
curl -H "Accept: text/turtle" http://localhost:8080/catalogue/fdp/catalog/cat-1
```

### Out of Scope

- `dcat:Distribution` layer (Phase 8+)
- FDP Index registration
- Authentication/write endpoints

---

## Phase 8+: Future Ideas

### Transform Simplification
- **Declarative field mapping** (Phase 7.4) - YAML-based, no-code for simple cases
- **JSON-LD context compaction** - Simplify prefixed keys before transform
- **SPARQL CONSTRUCT queries** - Alternative to JSON-LD framing + JSLT

### Data Sources
- **SQL query support** - Direct database queries as alternative to GraphQL
- **CSV source step** (Phase 7.2) - For schema migrations

### Output Formats
- **RDF output step** (Phase 7.3) - For DCAT publishing

### Scalability
- **Chunking/pagination** - For large datasets
- **Task framework + async** - Background execution

### Scripting (Future Exploration)
- **GraalPy (Python on GraalVM)** - Production ready 2024, data managers like Python
  - Pure Python: ready, ~4x faster than CPython
  - Native extensions: experimental
  - Consider for complex transforms where JSLT is insufficient
- **Rule engines** - Less suitable for ETL, better for business rules

### Rejected Ideas
- **JSONata** - Doesn't improve learnability over JSLT (see Phase 7.1)

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
