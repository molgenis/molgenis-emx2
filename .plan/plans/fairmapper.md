# FAIRmapper - Implementation Plan v1.9.0

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
| Declarative field mapping | HIGH | → Phase 10.1 |
| FDP publish endpoints | MEDIUM | ✅ Done (7.5) |
| Quick wins sprint | HIGH | 🔄 Current (7.6) |

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

**Goal**: Add FDP-compliant endpoints for DCAT publishing from MOLGENIS data.

### What Was Done

1. **Bundle Reorganization**
   - Renamed `fair-mappings/dcat-fdp` → `fair-mappings/dcat-harvester` (harvests external FDP)
   - Created new `fair-mappings/dcat-fdp` for publishing MOLGENIS as FDP

2. **Endpoints** (all at `/{schema}/api/fdp/*`)
   | Path | Output | Description |
   |------|--------|-------------|
   | `/{schema}/api/fdp` | Turtle/JSON-LD | FDP root with catalog links |
   | `/{schema}/api/fdp/catalog/{id}` | Turtle/JSON-LD | Catalog with dataset links |
   | `/{schema}/api/fdp/dataset/{id}` | Turtle/JSON-LD | Dataset metadata |

3. **Files Created**
   - `src/queries/get-schema-metadata.gql`, `get-catalog.gql`, `get-dataset.gql`
   - `src/transforms/publish/to-fdp-root.jslt`, `to-dcat-catalog.jslt`, `to-dcat-dataset.jslt`
   - Test fixtures in `test/publish/` for each endpoint

4. **Testing**
   - Unit tests for JSLT transforms
   - SHACL validation against `data/_shacl/fair_data_point/v1.2/` shapes
   - Integration tests in `FairMapperApiTest.java` using DATA_CATALOGUE profile with demo data

### Data Shape Notes

- `theme` is `ontology_array`: `[{name, code, codesystem, definition}]`
- `contactPoint` is ref object: `{id, firstName, lastName, email}`
- Dataset types: excludes Catalogue/Network, includes Cohort/Biobank/Registry/Databank/etc.

### Out of Scope

- `dcat:Distribution` layer (Phase 8+)
- FDP Index registration
- Authentication/write endpoints

---

## Phase 7.6: Content Negotiation & Simplification (CURRENT)

**Goal:** Replace `rdf` step with mapping-level `input`/`output` + content negotiation.

| Task | Priority | Status |
|------|----------|--------|
| Add `input`/`output`/`frame` fields to Mapping | HIGH | ✅ Done (7.6.1) |
| Content negotiation in API (output) | HIGH | ✅ Done (7.6.2) |
| Input format handling + frame validation | MEDIUM | ⏭ Deferred (7.6.3) |
| Migrate bundles (remove `rdf` steps) | MEDIUM | ✅ Done (7.6.4) |
| Remove `OutputRdfStep` | MEDIUM | ✅ Done (7.6.5) |
| Update documentation | MEDIUM | ✅ Done (7.6.6) |
| Simplified Mapping model | HIGH | 🔄 Current (7.6.7) |

---

### 7.6.1: Add `input`/`output`/`frame` fields to Mapping

**Files to modify:**

| File | Change |
|------|--------|
| `model/Mapping.java` | Add `input`, `output`, `frame` fields |
| `BundleLoader.java` | Validate frame required when input is RDF |

**Mapping.java changes:**
```java
public record Mapping(
    String name,
    String endpoint,
    List<String> methods,
    String input,    // NEW: default input format (json, turtle, jsonld, csv)
    String output,   // NEW: default output format (json, turtle, jsonld, ntriples, csv)
    String frame,    // NEW: JSON-LD frame file (required when input is RDF)
    @JsonDeserialize(using = StepConfigDeserializer.class) List<StepConfig> steps,
    E2e e2e) {

  public String input() { return input != null ? input : "json"; }
  public String output() { return output != null ? output : "json"; }
  // ... existing methods
}
```

**Validation rules (BundleLoader):**
- RDF input formats require frame file
- Frame path must pass PathValidator check
- Unknown format names produce error

**Supported formats:** `json`, `turtle`, `jsonld`, `ntriples`, `csv`

**Tests:**
- Valid: `input: json` (no frame needed)
- Valid: `input: turtle`, `frame: src/x.jsonld`
- Invalid: `input: turtle` without frame → error

---

### 7.6.2: Content Negotiation in API (output) ✅ DONE

**Files created:**
- `ContentNegotiator.java` - format resolution from Accept header
- `ContentNegotiatorTest.java` - 22 test cases

**Files modified:**
- `FairMapperApi.java` - uses getMappings(), content negotiation, RDF conversion
- `PipelineExecutor.java` - added execute(Mapping) overload
- `FairMapperApiTest.java` - updated for Mapping

**Original plan (kept for reference):**

| File | Change |
|------|--------|
| `FairMapperApi.java` | Use `bundle.getMappings()`, add content negotiation |
| `ContentNegotiator.java` | NEW: content negotiation helper |

**New helper class - ContentNegotiator.java:**
```java
public class ContentNegotiator {
  private static final List<String> ACCEPT_PRIORITY = List.of(
      "text/turtle", "application/ld+json", "application/n-triples",
      "text/csv", "application/json"
  );

  private static final Map<String, String> MIME_TO_FORMAT = Map.of(
      "text/turtle", "turtle",
      "application/ld+json", "jsonld",
      "application/n-triples", "ntriples",
      "text/csv", "csv",
      "application/json", "json"
  );

  private static final Map<String, String> FORMAT_TO_MIME = Map.of(
      "turtle", "text/turtle",
      "jsonld", "application/ld+json",
      "ntriples", "application/n-triples",
      "csv", "text/csv",
      "json", "application/json"
  );

  public static String resolveOutputFormat(String acceptHeader, String defaultFormat) {
    if (acceptHeader == null || acceptHeader.isBlank()) return defaultFormat;
    String accept = acceptHeader.toLowerCase();
    for (String mime : ACCEPT_PRIORITY) {
      if (accept.contains(mime)) return MIME_TO_FORMAT.get(mime);
    }
    return defaultFormat;
  }

  public static String getMimeType(String format) {
    return FORMAT_TO_MIME.getOrDefault(format, "application/json");
  }
}
```

**FairMapperApi changes:**
- Use `bundle.getMappings()` instead of `bundle.endpoints()` for backwards compatibility
- Register routes for Mapping (new) not Endpoint (deprecated)
- Add content negotiation for output

**Tests:**
- GET with `Accept: text/turtle` → returns Turtle
- GET with `Accept: application/json` → returns JSON
- GET without Accept + `output: turtle` → returns Turtle
- GET without Accept + no output → returns JSON

---

### 7.6.3: Input Format Handling + Frame Validation ⏭ DEFERRED

**Status:** No current use case for RDF/CSV input via POST. Fetch step handles RDF from URLs. Revisit when needed.

**Existing classes:**
- `RdfToJsonLd.java` - converts Model → JSON-LD (expand mode)
- `JsonLdFramer.java` - applies frame to JSON-LD string

**Files to modify:**

| File | Change |
|------|--------|
| `FairMapperApi.java` | Parse input based on Content-Type |
| `ContentNegotiator.java` | Add `resolveInputFormat()` method |

**Files to create:**

| File | Purpose |
|------|---------|
| `CsvToJson.java` | CSV string → JSON array (Jackson) |

**Input parsing flow:**
1. Resolve format from Content-Type header (fallback to mapping.input())
2. JSON: pass-through
3. Turtle/N-Triples: parse to Model → RdfToJsonLd → JsonLdFramer with mapping.frame()
4. JSON-LD: JsonLdFramer with mapping.frame()
5. CSV: CsvToJson

**Frame path must use PathValidator:**
```java
Path framePath = PathValidator.validateWithinBase(bundlePath, mapping.frame());
```

**Tests:**
- POST JSON → pass-through
- POST Turtle + frame → framed JSON-LD
- POST Turtle + no frame → error
- POST CSV → JSON array

---

### 7.6.4: Migrate Bundles

**Bundles to migrate:**

| Bundle | Change |
|--------|--------|
| `dcat-fdp` | Add `output: turtle`, remove `- rdf: turtle` steps |
| `beacon-v2` | Migrate `endpoints:` → `mappings:`, `path:` → `endpoint:` |
| `dcat-harvester` | No change needed (uses fetch step with frame) |

**dcat-fdp migration (3 mappings):**

Before:
```yaml
- name: fdp-catalog
  endpoint: /{schema}/api/fdp/catalog/{id}
  methods: [GET]
  steps:
    - query: src/queries/get-catalog.gql
    - transform: src/transforms/publish/to-dcat-catalog.jslt
    - rdf: turtle
```

After:
```yaml
- name: fdp-catalog
  endpoint: /{schema}/api/fdp/catalog/{id}
  methods: [GET]
  output: turtle
  steps:
    - query: src/queries/get-catalog.gql
    - transform: src/transforms/publish/to-dcat-catalog.jslt
```

**beacon-v2 migration (3 endpoints):**

Before:
```yaml
endpoints:
  - path: /{schema}/api/beacon/individuals
    methods: [GET, POST]
    steps: [...]
```

After:
```yaml
mappings:
  - endpoint: /{schema}/api/beacon/individuals
    methods: [GET, POST]
    steps: [...]
```

Note: `input`/`output` default to `json` so can be omitted for beacon-v2.

**Why dcat-harvester unchanged:**
- Uses `fetch` step (external URL) which has its own `frame:` field
- Mapping-level `input`/`frame` is for HTTP request body parsing
- Fetch step handles RDF input internally

---

### 7.6.5: Remove `OutputRdfStep`

**Files to delete:**

| File | Reason |
|------|--------|
| `model/step/OutputRdfStep.java` | Replaced by content negotiation |

**Files to modify:**

| File | Change |
|------|--------|
| `StepConfigDeserializer.java` | Remove `rdf:` step parsing, throw error if encountered |
| `RemotePipelineExecutor.java` | Remove OutputRdfStep case from switch |
| `PipelineExecutor.java` | Remove OutputRdfStep case from switch |
| `StepConfig.java` | Remove OutputRdfStep from sealed permits |

**Verification:**
- All bundles migrated in 7.6.4 (no `rdf:` steps remain)
- Tests still pass after removal

---

### 7.6.7: Simplified Mapping Model

**Goal:** Cleaner distinction between API endpoints and harvest pipelines.

**Design:**
```
Mapping = name + (endpoint | fetch) + steps
  - name: always required (identifier)
  - endpoint: API route (e.g. /{schema}/api/fdp)
  - fetch: RDF source URL (e.g. ${SOURCE_URL})
  - frame: JSON-LD frame file (required with fetch)
```

**Rules:**
1. `name` always required
2. Either `endpoint` OR `fetch` (not both, mutually exclusive)
3. `frame` required when `fetch` is set
4. `frame` optional when `endpoint` is set (for RDF POST input, future)

**API mapping (endpoint):**
```yaml
mappings:
  - name: fdp-catalog
    endpoint: /{schema}/api/fdp/catalog/{id}
    methods: [GET]
    output: turtle
    steps:
      - query: src/queries/get-catalog.gql
      - transform: src/transforms/publish/to-dcat-catalog.jslt
```

**Harvest mapping (fetch + frame):**
```yaml
mappings:
  - name: harvest-catalog
    fetch: ${SOURCE_URL}
    frame: src/frames/catalog.jsonld
    steps:
      - transform: src/transforms/to-molgenis.jslt
      - mutate: src/mutations/upsert.gql
```

**Validation errors:**
- Missing `name` → "Mapping requires 'name' field"
- Both `endpoint` + `fetch` → "Mapping cannot have both 'endpoint' and 'fetch'"
- Neither `endpoint` nor `fetch` → "Mapping requires either 'endpoint' or 'fetch'"
- `fetch` without `frame` → "Mapping with 'fetch' requires 'frame' field"

**Files to modify:**

| File | Change |
|------|--------|
| `Mapping.java` | Add `fetch` field, add validation methods |
| `BundleLoader.java` | Call Mapping.validate(), throw on error |
| `FetchStep.java` | Keep for backwards compat, log deprecation |
| `PipelineExecutor.java` | Handle mapping.fetch() before steps |
| `RemotePipelineExecutor.java` | Same change as PipelineExecutor |

**Files to migrate:**

| Bundle | Change |
|--------|--------|
| `dcat-harvester` | Move `fetch:` from step to mapping level |

**Implementation order:**
1. Add `fetch` field to Mapping.java
2. Add validation methods to Mapping.java
3. Update BundleLoader to call validation
4. Update executors to handle mapping-level fetch
5. Migrate dcat-harvester bundle
6. Add deprecation warning to FetchStep parsing

---

### 7.6.6: Update Documentation

**Files to modify:**

| File | Change |
|------|--------|
| `docs/fairmapper/schema_reference.md` | Add input/output/frame fields, deprecate rdf step |
| `docs/fairmapper/getting_started.md` | Update examples |
| `docs/fairmapper/README.md` | Update overview examples |
| `docs/fairmapper/MIGRATION.md` | NEW: migration guide for existing bundles |

**Key doc changes:**
- Add Mapping Fields table with input/output/frame
- Add Content Negotiation section with examples
- Deprecate RDF step section
- Add migration guide for bundle authors

---

## Implementation Order

```
7.6.1 Mapping fields ─┬─> 7.6.2 Content negotiation (output) ─┬─> 7.6.4 Migrate bundles
                      │                                       │
                      └─> 7.6.3 Input parsing (CsvToJson) ────┘
                                                              │
                                                              v
                                              7.6.5 Remove OutputRdfStep
                                                              │
                                                              v
                                              7.6.6 Update docs
```

| Step | Task | Creates | Modifies |
|------|------|---------|----------|
| 7.6.1 | Add fields to Mapping | - | `Mapping.java`, `BundleLoader.java` |
| 7.6.2 | Content negotiation | `ContentNegotiator.java` | `FairMapperApi.java` |
| 7.6.3 | Input parsing | `CsvToJson.java` | `FairMapperApi.java`, `ContentNegotiator.java` |
| 7.6.4 | Migrate bundles | - | `dcat-fdp/fairmapper.yaml`, `beacon-v2/fairmapper.yaml` |
| 7.6.5 | Remove rdf step | - | Delete `OutputRdfStep.java`, modify executors |
| 7.6.6 | Update docs | `MIGRATION.md` | `schema_reference.md`, `getting_started.md` |

---

## Out of Scope (Future)

- CSV format configuration (delimiter, headers)
- Accept header q-weight parsing
- Frame syntax validation at bundle load
- GET request variables: path params (`{id}`) and query params need to be extracted and passed to pipeline as input JSON
- RDF input via POST (7.6.3) - no current use case, fetch step handles RDF from URLs
- CSV input via POST (7.6.3) - deferred

---

## Phase 8: Test Coverage & Error Handling

**Source:** Java expert review

| Task | Priority | Status |
|------|----------|--------|
| Command integration tests | HIGH | Pending (8.1) |
| Fix exception hierarchy | HIGH | Pending (8.2) |
| Fix silent partial failures | HIGH | Pending (8.3) |
| RemotePipelineExecutor tests | MEDIUM | Pending (8.4) |

---

## Phase 9: DCAT Completeness

**Source:** Bioinformatician/FAIR expert review

| Task | Priority | Status |
|------|----------|--------|
| Add dcat:distribution layer | HIGH | Pending (9.1) |
| Fix hardcoded timestamps | HIGH | Pending (9.2) |
| Add dct:accessRights | MEDIUM | Pending (9.3) |
| Extract shared FDP context | MEDIUM | Pending (9.4) |
| SHACL validation step | MEDIUM | Pending (9.5) |

---

## Phase 10: Transform Simplification

**Source:** All data manager reviews

| Task | Priority | Status |
|------|----------|--------|
| Declarative field mapping (7.4) | HIGH | Pending (10.1) |
| JSON-LD compaction step | MEDIUM | Pending (10.2) |
| JSLT-Python cheatsheet | LOW | Pending (10.3) |

---

## Phase 11+: Future Ideas

### Developer Experience
- **Dev server mode** - `fairmapper serve` runs local server on localhost:4000
  - Hot-reload on bundle changes
  - Continuous test runner
  - Interactive endpoint testing
  - Live RDF validation

### Data Sources
- **SQL query support** - Direct database queries as alternative to GraphQL
- **CSV source step** (Phase 7.2) - For schema migrations

### Scalability
- **Chunking/pagination** - For large datasets
- **Task framework + async** - Background execution

### Scripting (Future Exploration)
- **GraalPy (Python on GraalVM)** - Production ready 2024, data managers like Python
- **SPARQL CONSTRUCT queries** - Alternative to JSON-LD framing + JSLT

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

**Mapping types (7.6.7):**
```yaml
# API mapping (endpoint)
mappings:
  - name: fdp-catalog
    endpoint: /{schema}/api/fdp/catalog/{id}
    output: turtle
    steps:
      - query: src/queries/get-catalog.gql
      - transform: src/transforms/to-dcat-catalog.jslt

# Harvest mapping (fetch + frame)
mappings:
  - name: harvest-catalog
    fetch: ${SOURCE_URL}
    frame: src/frames/catalog.jsonld
    steps:
      - transform: src/transforms/to-molgenis.jslt
      - mutate: src/mutations/upsert.gql
```

---

## Running Tests

```bash
./gradlew :backend:molgenis-emx2-fairmapper-cli:test
./fairmapper test fair-mappings/dcat-fdp -v
```
