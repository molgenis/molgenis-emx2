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
| Merge RemotePipelineExecutor + PipelineExecutor | MEDIUM | Pending (6.5.2) |
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
