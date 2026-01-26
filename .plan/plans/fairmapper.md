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
| 6.4.1 | Path traversal protection (PathValidator) |

---

## Phase 6.4.2: SSRF Protection (CURRENT)

**Problem**: RdfFetcher.java allows fetching from any URL including internal network addresses (localhost, 10.x.x.x, 192.168.x.x, etc.). Attackers could use this to scan internal services or access metadata endpoints (AWS 169.254.169.254).

**Solution**: Create UrlValidator utility that:
1. Whitelist allowed schemes (https only, http optional for dev)
2. Block private/reserved IP ranges by default
3. Block localhost and loopback addresses by default
4. Resolve hostname to IP and validate before connecting
5. **Support configurable allowlist for dev/test environments**

### Allowlist Configuration

**Use Cases**:
- Local development with MOLGENIS at localhost:8080
- Integration tests against local services
- Docker Compose setups with internal service names

**Configuration Options** (in order of precedence):
1. **Constructor parameter**: `UrlValidator(Set<String> allowedHosts)` for programmatic use
2. **Environment variable**: `FAIRMAPPER_ALLOWED_HOSTS=localhost:8080,host.docker.internal:8080`
3. **System property**: `-Dfairmapper.allowed.hosts=localhost:8080`

**Behavior**:
- Allowlist entries match host:port exactly (e.g., `localhost:8080`)
- Allowlist bypasses SSRF checks for matching URLs only
- Empty allowlist = strict mode (production default)
- Allowlist is logged at startup for audit trail

### Files to Create

| File | Purpose |
|------|---------|
| `UrlValidator.java` | URL security validation with allowlist support |
| `UrlValidatorTest.java` | Unit tests for all validation rules + allowlist |

### Files to Modify

| File | Change |
|------|--------|
| `RdfFetcher.java` | Call UrlValidator.validate() before fetch |

### Implementation Details

**UrlValidator.java** (new):
```java
package org.molgenis.emx2.fairmapper;

public final class UrlValidator {
  private static final Set<String> ALLOWED_SCHEMES = Set.of("https", "http");
  private static final String ALLOWED_HOSTS_ENV = "FAIRMAPPER_ALLOWED_HOSTS";
  private static final String ALLOWED_HOSTS_PROP = "fairmapper.allowed.hosts";

  private final Set<String> allowedHosts;

  public UrlValidator() {
    this(loadAllowedHostsFromEnv());
  }

  public UrlValidator(Set<String> allowedHosts) {
    this.allowedHosts = allowedHosts != null ? Set.copyOf(allowedHosts) : Set.of();
    if (!this.allowedHosts.isEmpty()) {
      log.info("SSRF allowlist enabled for: {}", this.allowedHosts);
    }
  }

  private static Set<String> loadAllowedHostsFromEnv() {
    String hosts = System.getProperty(ALLOWED_HOSTS_PROP);
    if (hosts == null) {
      hosts = System.getenv(ALLOWED_HOSTS_ENV);
    }
    if (hosts == null || hosts.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(hosts.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toSet());
  }

  public void validate(String url) {
    URI uri = URI.create(url);
    validateScheme(uri);
    if (isAllowlisted(uri)) {
      return;
    }
    validateHost(uri);
  }

  private boolean isAllowlisted(URI uri) {
    String hostPort = uri.getHost() + ":" + (uri.getPort() != -1 ? uri.getPort() : getDefaultPort(uri));
    return allowedHosts.contains(hostPort) || allowedHosts.contains(uri.getHost());
  }

  private int getDefaultPort(URI uri) {
    return "https".equals(uri.getScheme()) ? 443 : 80;
  }

  private void validateScheme(URI uri) {
    if (!ALLOWED_SCHEMES.contains(uri.getScheme())) {
      throw new FairMapperException("Blocked URL scheme: " + uri.getScheme());
    }
  }

  private void validateHost(URI uri) {
    String host = uri.getHost();
    if ("localhost".equalsIgnoreCase(host)) {
      throw new FairMapperException("Blocked localhost URL");
    }
    InetAddress addr = InetAddress.getByName(host);
    if (isPrivateOrReserved(addr)) {
      throw new FairMapperException("Blocked private/reserved IP: " + host);
    }
  }

  private boolean isPrivateOrReserved(InetAddress addr) {
    return addr.isLoopbackAddress()
        || addr.isLinkLocalAddress()
        || addr.isSiteLocalAddress()
        || addr.isAnyLocalAddress()
        || isAwsMetadata(addr);
  }

  private boolean isAwsMetadata(InetAddress addr) {
    byte[] bytes = addr.getAddress();
    return bytes[0] == (byte)169 && bytes[1] == (byte)254
        && bytes[2] == (byte)169 && bytes[3] == (byte)254;
  }
}
```

### Test Cases

| Test | Expected |
|------|----------|
| `https://fdp.example.org/catalog` | Pass |
| `http://example.org/data` | Pass (http allowed) |
| `file:///etc/passwd` | Blocked (invalid scheme) |
| `ftp://server/file` | Blocked (invalid scheme) |
| `http://localhost:8080/api` | Blocked (no allowlist) |
| `http://localhost:8080/api` with allowlist `localhost:8080` | **Pass** |
| `http://localhost:9090/api` with allowlist `localhost:8080` | Blocked (wrong port) |
| `http://127.0.0.1/api` | Blocked (loopback) |
| `http://10.0.0.1/internal` | Blocked (private 10.x) |
| `http://192.168.1.1/router` | Blocked (private 192.168.x) |
| `http://172.16.0.1/service` | Blocked (private 172.16-31.x) |
| `http://169.254.169.254/meta` | Blocked (AWS metadata) |
| `http://[::1]/api` | Blocked (IPv6 loopback) |

### Usage Examples

**Local development**:
```bash
export FAIRMAPPER_ALLOWED_HOSTS=localhost:8080
./fairmapper run fair-mappings/dcat-fdp --target http://localhost:8080/api/graphql
```

**Integration tests**:
```java
UrlValidator validator = new UrlValidator(Set.of("localhost:8080"));
```

**Docker Compose**:
```bash
export FAIRMAPPER_ALLOWED_HOSTS=molgenis:8080,host.docker.internal:8080
```

### Verification Steps

1. Run unit tests: `./gradlew :backend:molgenis-emx2-fairmapper-cli:test --tests "*UrlValidator*"`
2. Run all fairmapper tests: `./gradlew :backend:molgenis-emx2-fairmapper-cli:test`
3. Test with allowlist: `FAIRMAPPER_ALLOWED_HOSTS=localhost:8080 ./fairmapper run ...`
4. Verify real FDP URL still works without allowlist

---

## Phase 6.4: Security Fixes (Remaining)

| Task | Priority | Status |
|------|----------|--------|
| Path traversal protection | CRITICAL | ✅ Done (6.4.1) |
| SSRF protection | CRITICAL | 🔄 In Progress (6.4.2) |
| LocalRdfSource validation | CRITICAL | Pending (6.4.3) |
| Size limits on fetch | HIGH | Pending (6.4.4) |
| Error handling in FrameDrivenFetcher | HIGH | Pending (6.4.5) |
| Retry logic for transient failures | MEDIUM | Pending (6.4.6) |

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
