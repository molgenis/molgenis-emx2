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
| 8.2 Add null-check to RemotePipelineExecutor | HIGH | Done |
| 8.3 Command error case tests | HIGH | Done |
| 8.4 RemotePipelineExecutor tests | MEDIUM | Done |

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

**Already covered:**
| Command | Error Case | Test |
|---------|------------|------|
| `validate` | Missing fairmapper.yaml | `testValidate_missingBundle` |
| `test` | Missing bundle | `testTest_missingBundle` |
| `dry-run` | Missing input file | `testDryRun_missingInputFile` |
| `e2e` | Missing --server | `testE2e_missingServer` |

**Tests to add:**
| Command | Error Case | Expected | Test Name |
|---------|------------|----------|-----------|
| `validate` | Invalid YAML syntax | Exit 1, parse error | `testValidate_invalidYaml` |
| `validate` | Missing transform file | Exit 1, file path in error | `testValidate_missingTransformFile` |
| `test` | Malformed test input JSON | Exit 1, parse error | `testTest_malformedInputJson` |
| `test` | Invalid JSLT transform | Exit 1, JSLT error | `testTest_invalidTransform` |
| `dry-run` | Invalid JSLT transform | Exit 1, JSLT error | `testDryRun_invalidTransform` |
| `run` | Missing --source | Exit 1, "required" message | `testRun_missingSource` |
| `run` | Missing --schema | Exit 1, "required" message | `testRun_missingSchema` |
| `fetch-rdf` | Invalid URL | Exit 1, error message | `testFetchRdf_invalidUrl` |
| `fetch-rdf` | Missing frame file | Exit 1, "not found" | `testFetchRdf_missingFrameFile` |

**Implementation:** Use `@TempDir` to create test fixtures dynamically (existing pattern).

---

### 8.4 RemotePipelineExecutor Tests

**File:** `RemotePipelineExecutorTest.java` (extend existing)

**Existing tests:**
- `testMappingWithNullStepsReturnsInputUnchanged` ✅
- `testMappingWithEmptyStepsListReturnsInputUnchanged` ✅

**Tests to add:**

| Test | Setup | Assertion |
|------|-------|-----------|
| `testQueryStepSuccess` | Mock client returns `{"data":{"Users":[...]}}` | Result equals mocked response |
| `testQueryStepPropagatesIOException` | Mock client throws IOException | IOException propagated |
| `testMutateStepSuccess` | Mock client returns mutation result | Result equals mocked response |
| `testTransformStepSuccess` | Mock engine returns transformed JSON | Result equals mocked response |
| `testMultiStepPipeline` | Query → Transform → Mutate | Steps execute in order, verify call order |
| `testTransformStepInvalidPath` | Step path outside bundle | IOException (path traversal blocked) |
| `testPlaceholderResolution` | Mapping with `${SOURCE_URL}` + input with SOURCE_URL | URL correctly resolved |

**Deferred (requires refactoring):**
| Test | Reason |
|------|--------|
| Fetch mapping tests | RdfFetcher/JsonLdFramer instantiated inline, not mockable |

**File setup for tests:**
```java
@TempDir Path tempDir;

@BeforeEach
void setUp() throws IOException {
  Files.writeString(tempDir.resolve("query.gql"), "query { Users { name } }");
  Files.writeString(tempDir.resolve("mutate.gql"), "mutation { insert_User(...) }");
  Files.writeString(tempDir.resolve("transform.jslt"), ". | {\"wrapped\": .}");
}
```

**Mapping construction:**
```java
Mapping queryMapping = new Mapping(
    "test",           // name
    "/api/test",      // endpoint (required if no fetch)
    null,             // fetch
    List.of("GET"),   // methods
    null,             // input
    null,             // output
    null,             // frame
    List.of(new QueryStep("query.gql", null)),  // steps
    null              // e2e
);
```

**Call verification pattern:**
```java
@Test
void testMultiStepPipeline() throws IOException {
  InOrder inOrder = inOrder(mockClient, mockTransformEngine);

  JsonNode queryResult = objectMapper.readTree("{\"data\":{}}");
  JsonNode transformResult = objectMapper.readTree("{\"wrapped\":{}}");
  JsonNode mutateResult = objectMapper.readTree("{\"result\":\"ok\"}");

  when(mockClient.execute(eq("testSchema"), contains("query"), any())).thenReturn(queryResult);
  when(mockTransformEngine.transform(any(Path.class), eq(queryResult))).thenReturn(transformResult);
  when(mockClient.execute(eq("testSchema"), contains("mutation"), any())).thenReturn(mutateResult);

  List<StepConfig> steps = List.of(
      new QueryStep("query.gql", null),
      new TransformStep("transform.jslt", null),
      new MutateStep("mutate.gql")
  );
  Mapping mapping = new Mapping("test", "/api/test", null, List.of("POST"), null, null, null, steps, null);

  JsonNode result = executor.execute(objectMapper.createObjectNode(), mapping);

  inOrder.verify(mockClient).execute(eq("testSchema"), contains("query"), any());
  inOrder.verify(mockTransformEngine).transform(any(), any());
  inOrder.verify(mockClient).execute(eq("testSchema"), contains("mutation"), any());
  assertEquals(mutateResult, result);
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

## Phase 9: SQL Query Step (Proof of Concept)

Exploring SQL as alternative to GraphQL + JSLT (per SQL expert review feedback).

| Task | Priority | Status |
|------|----------|--------|
| 9.1 Create dcat-fdp-sql mock bundle | HIGH | Done |
| 9.2 Test SQL against real catalogue DB | HIGH | Done |
| 9.3 Implement SqlQueryStep | MEDIUM | Done |

**Completed:**
- `fair-mappings/dcat-fdp-sql/` - Example bundle with SQL query
- `SqlQueryIntegrationTest.java` - Proves SQL produces valid JSON-LD
- Uses existing `schema.retrieveSql(sql, params)` with `${param}` binding

**Benefits:**
- Single step instead of GraphQL + JSLT (2 steps)
- SQL more widely known than JSLT
- Direct JSON-LD construction
- Can use database timestamps (mg_insertedOn, mg_updatedOn)

---

### 9.3 Implement SqlQueryStep

**Files to create/modify:**

1. `model/step/SqlQueryStep.java` - New step type implementing StepConfig
2. `model/step/StepConfigDeserializer.java` - Add `"sql"` key handling
3. `PipelineExecutor.java` - Handle SqlQueryStep via schema.retrieveSql()
4. `RemotePipelineExecutor.java` - Handle SqlQueryStep (needs schema access)
5. `BundleLoader.java` - Add validateSqlFile() for .sql extension

**Usage:**
```yaml
steps:
  - sql: src/queries/get-catalog.sql
```

**Implementation pattern:**
```java
} else if (step instanceof SqlQueryStep sqlStep) {
  Path sqlPath = bundlePath.resolve(sqlStep.path());
  String sql = Files.readString(sqlPath);
  List<Row> rows = schema.retrieveSql(sql, variablesAsMap(input));
  current = rows.isEmpty() ? null : parseJson(rows.get(0).get("result", String.class));
}
```

---

## Phase 10: Frame Step (Simplified Harvesting)

Enable two-frame pattern for cleaner RDF harvesting:
1. Frame with `@explicit: false` → capture all properties
2. Transform → handle exceptions (type, id)
3. Frame with `@explicit: true` → strip unmapped

| Task | Priority | Status |
|------|----------|--------|
| 10.1 Create FrameStep model class | HIGH | Done |
| 10.2 Add frame handling to PipelineExecutor | HIGH | Done |
| 10.3 Proof-of-concept example bundle | HIGH | Done |
| 10.4 Auto-frame endpoint in MOLGENIS | MEDIUM | Pending (separate PR) |
| 10.5 JSON-LD import endpoint | MEDIUM | Pending (separate PR) |

### 10.1 FrameStep

**Files:**
- `model/step/FrameStep.java` - record implementing StepConfig
- `model/step/StepConfigDeserializer.java` - add `"frame"` key handling

```java
public record FrameStep(String path, Boolean unmapped) implements StepConfig {
  @Override public String path() { return path; }
}
```

**Usage:**
```yaml
steps:
  - frame: src/frames/resources.jsonld
    unmapped: true    # flip @explicit to false → keep unmapped
  - transform: src/transforms/fix-exceptions.jslt
  - frame: src/frames/resources.jsonld
    # unmapped: false (default) → use @explicit: true as authored
```

**Frame files authored with `@explicit: true` at all levels (strict by default).**
Step only flips when `unmapped: true`.

### 10.2 PipelineExecutor handling

```java
} else if (step instanceof FrameStep frameStep) {
  Path framePath = bundlePath.resolve(frameStep.path());
  ObjectNode frameDoc = (ObjectNode) objectMapper.readTree(Files.readString(framePath));

  if (Boolean.TRUE.equals(frameStep.unmapped())) {
    setExplicitRecursive(frameDoc, false);
  }

  current = JsonLdFramer.frame(current, frameDoc);
}

private void setExplicitRecursive(ObjectNode node, boolean explicit) {
  if (node.has("@explicit")) {
    node.put("@explicit", explicit);
  }
  node.fields().forEachRemaining(entry -> {
    if (entry.getValue().isObject()) {
      setExplicitRecursive((ObjectNode) entry.getValue(), explicit);
    }
  });
}
```

### 10.3 Proof-of-concept bundle

`fair-mappings/dcat-harvester-framed/` demonstrating:
- Fetch RDF (no frame in fetch)
- Frame step 1: capture all
- Transform: handle @type → type, @id → id
- Frame step 2: strip unmapped
- Mutate

### 10.4 Auto-frame endpoint

Separate PR (`feat/rest-json-ld-graphql`):
```
GET /{schema}/api/jsonld/frame
```

Generates frame from **whole schema** metadata (all tables with `semantics` annotations).

**Why whole schema:**
- RDF is a graph with relationships (Catalog → Dataset → Distribution)
- Frame once, shape entire connected structure
- References between tables become `@embed: @always`

**Generation logic:**
```java
for (TableMetadata table : schema.getTables()) {
  if (table.getSemantics() != null) {
    // Add @type from table.semantics
    // Add properties from column.semantics
    // Add @embed for reference columns
  }
}
```

**Output structure:**
```json
{
  "@context": { /* from schema namespaces */ },
  "@type": ["dcat:Catalog", "dcat:Dataset", ...],
  "@explicit": true,
  "name": {},
  "description": {},
  "dcat:dataset": {
    "@type": "dcat:Dataset",
    "@embed": "@always",
    "@explicit": true
  }
}
```

Extends existing `JsonLdSchemaGenerator.java` which already generates context.

### 10.5 JSON-LD Import Endpoint

Separate PR (`feat/rest-json-ld-graphql`):
```
POST /{schema}/api/jsonld/import
```

Accepts framed JSON-LD, imports to MOLGENIS.

**Key decisions:**

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Target table | `@type` → table with matching `semantics` | Natural RDF mapping |
| ID extraction | Configurable via `jsonld.idExtract` schema setting | Default: last segment |
| Multiple types | Single transaction, deferred FK | Order-independent |
| Unknown properties | Ignore, warn in response | Schema defines what matters |
| Conflict | Merge (like CSV import) | Existing MOLGENIS behavior |
| Missing required | Fail entire request | Atomic |
| Response | 200 + summary (not 201) | Bulk import |
| Type column | `@type` → type table lookup | e.g., `dcat:Catalog` → `Catalogue` |

**ID extraction options:**
- Default: last path segment (`/catalog/123` → `123`)
- `idExtract: full` - keep full IRI
- `idExtract: "regex"` - custom pattern

**LDP-inspired, not LDP-compliant** (bulk import vs single resource creation).

**Full spec:** See `.plan/specs/fairmapper.md` section "JSON-LD Import Endpoint"

**Simplified pipeline:**
```yaml
steps:
  - frame: ${TARGET_SCHEMA}/api/jsonld/frame
    unmapped: true
  - import: ${TARGET_SCHEMA}/api/jsonld/import
```

---

## Phase 11: DCAT Completeness (Deferred)

| Task | Priority | Status |
|------|----------|--------|
| Add dcat:distribution layer | HIGH | Pending |
| Fix hardcoded timestamps | HIGH | Pending |
| Add dct:accessRights | MEDIUM | Pending |
| Extract shared FDP context | MEDIUM | Pending |
| SHACL validation step | MEDIUM | Pending |

---

## Phase 12: Transform Simplification

| Task | Priority | Status |
|------|----------|--------|
| Declarative field mapping | HIGH | Pending |
| JSON-LD compaction step | MEDIUM | Pending |
| JSLT-Python cheatsheet | LOW | Pending |

---

## Phase 12.5: Route Validation (TODO)

| Task | Priority | Status |
|------|----------|--------|
| 11.5.1 Detect duplicate paths | HIGH | Pending |
| 11.5.2 Validate path patterns | MEDIUM | Pending |

**Problem:** Multiple bundles can register the same endpoint path, causing conflicts.

**Fix:** Add validation in `FairMapperApi.registerRoutes()`:
- Track registered paths
- Warn/error on duplicate
- Consider bundle priority or first-wins

---

## Phase 13: Output Targets

| Task | Priority | Status |
|------|----------|--------|
| 13.1 MOLGENIS CSV zip export | HIGH | Pending |
| 13.2 Self-harvest demo | HIGH | Pending |

### 13.1 MOLGENIS CSV Zip Export

**Goal:** Transform harvested RDF into MOLGENIS-compatible CSV zip (reverse of fetch)

**Use case:** Harvest external FDP → transform → download as `schema.zip` → import into MOLGENIS

**Implementation:**
- New output format: `output: molgenis-zip`
- After transform, convert JSON to CSV files per table
- Package as zip matching MOLGENIS import format

**Format:**
```
schema.zip/
  Resources.csv
  Organisations.csv
  Contacts.csv
  molgenis.csv (metadata)
```

### 13.2 Self-Harvest Demo

**Goal:** Demo harvesting our own FDP endpoints back into MOLGENIS

**Flow:**
1. Publish: `GET /catalogue/api/fdp` → JSON-LD
2. Harvest: FAIRmapper fetches from own endpoint
3. Transform: JSON-LD → MOLGENIS format
4. Output: Either mutate or CSV zip

---

## Future Ideas

### Developer Experience
- **Dev server mode** - `fairmapper serve` with hot-reload

### Data Sources
- **SQL query support** - Direct PostgreSQL queries ✅ Done (SqlQueryStep)
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
