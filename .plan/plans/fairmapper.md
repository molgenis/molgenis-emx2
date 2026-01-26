# FAIRmapper - Implementation Plan v1.6.0

**Spec**: `.plan/specs/fairmapper.md`
**Branch**: `spike/etl_pilot`
**Module**: `backend/molgenis-emx2-fairmapper`

## Context

FAIRmapper enables data managers to create API adapters without Java code. Configure via YAML + JSLT transforms. First use case: Beacon v2 API. Then DCAT

## Architecture

```
fair-mappings/beacon-v2/
  fairmapper.yaml              # Bundle config (name, version, endpoints)
  src/transforms/*.jslt        # JSLT transforms
  src/queries/*.gql            # GraphQL queries
  test/*/*.json                # Test fixtures

backend/molgenis-emx2-fairmapper/
  RunFairMapper.java           # Picocli CLI (validate, test, dry-run, e2e)
  BundleLoader.java            # YAML parsing + validation
  JsltTransformEngine.java     # JSLT execution (preserves empty arrays)
  PipelineExecutor.java        # Chains steps: transform → query → transform (local)
  GraphqlClient.java           # HTTP client for remote GraphQL
  RemotePipelineExecutor.java  # Chains steps using GraphqlClient (remote)
  model/
    MappingBundle.java         # record(name, version, endpoints)
    Endpoint.java              # record(path, methods, steps, e2e)
    Step.java                  # record(transform, query, tests)
```

## Completed Phases

### Phase 1-5 Summary
- Core Engine with JSLT transforms
- CLI (validate, test, dry-run, e2e, fetch-rdf commands)
- Schema v2 with `mappings` replacing `endpoints`
- Step strategy pattern: StepConfig sealed interface
- FetchStep with RDF fetch + JSON-LD framing
- LocalRdfSource for testing, dcat-fdp bundle

---

## Phase 6: DCAT Harvesting - Transform + Mutate (DETAILED)

### 6.1 JSLT Transform: DCAT to MOLGENIS

**Goal**: Transform framed JSON-LD to MOLGENIS mutation format

**Files to create**:
- `fair-mappings/dcat-fdp/src/transforms/to-molgenis.jslt`
- `fair-mappings/dcat-fdp/test/transform/dcat-input.json`
- `fair-mappings/dcat-fdp/test/transform/molgenis-output.json`

**Mapping** (from Resources.csv and dcat-harvesting.md spec):

| DCAT Field | MOLGENIS Field | Transform Notes |
|------------|----------------|-----------------|
| `@id` | `id` | Extract UUID from URL path (last segment) |
| `@type` dcat:Catalog | `type` | `[{name: "Catalogue"}]` |
| `@type` dcat:Dataset | `type` | `[{name: "Dataset"}]` |
| `dcterms:title` | `name` | Direct copy |
| `dcterms:description` | `description` | Direct copy |
| `dcterms:identifier` | `pid` | Direct copy (DOI) |
| `dcat:keyword` | `keywords` | Array or single value to array |
| `dcterms:publisher.foaf:name` | `organisations[].id` | Create org reference |
| `dcterms:publisher.@id` | (skip for now) | Future: match by ROR |

**Output format** (for upsert mutation):
```json
{
  "Resources": [
    {
      "id": "catalog-uuid",
      "name": "Example Catalog",
      "type": [{"name": "Catalogue"}],
      "description": "...",
      "keywords": ["genetics", "health"]
    }
  ],
  "Organisations": [
    {
      "resource": {"id": "catalog-uuid"},
      "id": "Example Organization"
    }
  ]
}
```

**Tests**:
- Transform test in `fairmapper test dcat-fdp`

---

### 6.2 MutateExecutor

**Goal**: Execute GraphQL mutations (insert/upsert) against MOLGENIS

**Files to modify**:
- `backend/molgenis-emx2-fairmapper-cli/src/main/java/org/molgenis/emx2/fairmapper/RemotePipelineExecutor.java`

**Files to create**:
- `backend/molgenis-emx2-fairmapper-cli/src/main/java/org/molgenis/emx2/fairmapper/executor/MutateExecutor.java`
- `fair-mappings/dcat-fdp/src/mutations/upsert-resources.gql`

**Design**:
```java
public class MutateExecutor {
  private final GraphqlClient client;
  private final Path bundlePath;
  private final String schema;

  public JsonNode execute(MutateStep step, JsonNode data) throws IOException {
    Path mutationPath = bundlePath.resolve(step.path());
    String mutation = Files.readString(mutationPath);
    // GraphQL mutation format: mutation { upsert(Resources: $resources) { message } }
    return client.execute(schema, mutation, data);
  }
}
```

**GraphQL mutation file** (`upsert-resources.gql`):
```graphql
mutation($Resources: [ResourcesInput], $Organisations: [OrganisationsInput]) {
  insert(Resources: $Resources, Organisations: $Organisations) {
    message
  }
}
```

Note: MOLGENIS uses `insert` for upsert behavior (updates if key exists).

**Update RemotePipelineExecutor**:
- Add `MutateStep` handling in `execute(mapping)` method
- Similar pattern to QueryStep

**Tests**:
- `MutateExecutorTest.java`: mock GraphqlClient, verify mutation sent

---

### 6.3 CLI `run` Command

**Goal**: Execute full pipeline: fetch → transform → mutate

**Files to modify**:
- `backend/molgenis-emx2-fairmapper-cli/src/main/java/org/molgenis/emx2/fairmapper/RunFairMapper.java`

**New command**:
```bash
./fairmapper run <bundle> <mapping-name> <source-url> \
  --server http://localhost:8080 \
  --schema catalogueTest \
  --token <token>
```

**Design**:
```java
@Command(name = "run", description = "Execute a mapping pipeline")
static class RunCommand implements Callable<Integer> {
  @Parameters(index = "0") Path bundlePath;
  @Parameters(index = "1") String mappingName;
  @Parameters(index = "2") String sourceUrl;  // Direct argument, no templating
  @Option(names = "--server") String server;
  @Option(names = "--schema") String schema;
  @Option(names = "--token") String token;
  @Option(names = "--dry-run") boolean dryRun;  // Skip mutation, print output

  @Override
  public Integer call() {
    // 1. Load bundle, find mapping
    // 2. Execute fetch step with sourceUrl
    // 3. Execute transform step(s)
    // 4. If !dryRun: execute mutate step with transform output as variables
    // 5. Print result summary
  }
}
```

**Tests**:
- Manual test against real FDP + local MOLGENIS

---

### 6.4 Update fairmapper.yaml

**Files to modify**:
- `fair-mappings/dcat-fdp/fairmapper.yaml`

**Add transform and mutate steps**:
```yaml
name: dcat-fdp
version: 1.0.0

mappings:
  - name: harvest-catalog
    steps:
      - fetch: ${SOURCE_URL}
        frame: src/frames/catalog-with-datasets.jsonld
        maxDepth: 5
        maxCalls: 50
        tests:
          - input: test/fetch/catalog.ttl
            output: test/fetch/catalog.json

      - transform: src/transforms/to-molgenis.jslt
        tests:
          - input: test/transform/dcat-input.json
            output: test/transform/molgenis-output.json

      - mutate: src/mutations/upsert-resources.gql
```

Note: `${SOURCE_URL}` is a placeholder - CLI `run` command provides actual URL as argument, overriding this value.

---

### 6.5 BundleLoader Validation Updates

**Files to modify**:
- `backend/molgenis-emx2-fairmapper-cli/src/main/java/org/molgenis/emx2/fairmapper/BundleLoader.java`

**Add validation**:
- Mutate step: file must exist, extension `.gql`
- Transform step: already validated
- Fetch step: already validated

---

## Phase 6 Implementation Order

| Story | Dependencies | Description |
|-------|--------------|-------------|
| 6.1 to-molgenis.jslt | None | Transform framed DCAT JSON-LD to MOLGENIS mutation format |
| 6.2 MutateExecutor | None | Execute GraphQL mutations with transform output as variables |
| 6.3 CLI run command | 6.1, 6.2 | Chain: fetch URL → transform → mutate |
| 6.4 Update fairmapper.yaml | 6.1 | Add transform + mutate steps to dcat-fdp bundle |
| 6.5 BundleLoader validation | None | Validate mutate step files exist |

**Parallel work**: 6.1 and 6.2 can be done simultaneously.

---

## Test Plan

### Unit Tests
1. `MutateExecutorTest` - mutation execution with mock
2. Transform test via `fairmapper test dcat-fdp` (JSLT transform)

### Integration Test
1. Manual: Run against real Health-RI FDP
   ```bash
   ./fairmapper run dcat-fdp harvest-catalog \
     https://fdp.health-ri.nl/catalog/5d6f222e-ba32-4930-9e77-d266f5ee64d9 \
     --server http://localhost:8080 \
     --schema catalogue
   ```

2. Verify in MOLGENIS:
   - Resources table has new catalog entry
   - Organisations table has publisher

---

## Open Questions for Phase 6

1. **Transaction handling**: Should all mutations in a run be in one transaction?
   - Current plan: No, each mutation is separate
   - Future Phase 7: Add transaction wrapping

2. **Organisation matching**: Create new or match existing by name?
   - Current plan: Create new with name as ID
   - Future: Match by ROR ID if available

3. **Error handling on mutate failure**: Skip record or fail entire run?
   - Current plan: Fail entire run, log error

4. **Dry-run for mutations**: Should `run --dry-run` show what would be mutated?
   - Current plan: Not in Phase 6, add later

---

## Future Phases (unchanged)

### Phase 7: Task Framework Integration
- Wrap pipeline in Task for progress tracking
- Async execution (avoid HTTP timeouts)
- DB persistence for audit trail
- Quartz scheduling for cron harvests

### Phase 8: SQL Query Support
- Add `.sql` file extension support
- Variable binding (check reports module: `${param}` syntax)
- PostgreSQL `json_build_object` for direct JSON responses

### Phase 9: Scaling / Chunking
- Split large inputs into chunks
- Process chunks with same pipeline
- Transaction wrapper for mutation batches
- Pagination support for queries

### Phase 10: Complete Beacon Migration
- Add more entity types (biosamples, datasets)
- Add more filter types (age, phenotype, diseases)

---

## Key Decisions Made

1. **Config file**: `fairmapper.yaml`
2. **Schema**: Flat structure (name, version, endpoints) - no apiVersion/kind
3. **Transform engine**: JSLT with imports, preserves empty arrays. Add more later
4. **Query engine**: GraphQL via molgenis-emx2-graphql. Add sql later
5. **CLI framework**: Picocli with colored output
6. **Distribution**: JAR with shell wrapper
7. **Multi-tenancy**: `{schema}` placeholder in paths
8. **Mutations**: Use MOLGENIS `insert` (upsert behavior) not separate `upsert`
9. **Variables**: No templating engine - source URL passed as CLI argument directly

---

## Running Tests

```bash
# All fairmapper-cli tests (includes RDF, framing, bundle loading)
./gradlew :backend:molgenis-emx2-fairmapper-cli:test

# CLI: validate bundle structure
fair-mappings/fairmapper validate fair-mappings/beacon-v2
fair-mappings/fairmapper validate fair-mappings/dcat-fdp

# CLI: run step tests (transforms + fetch steps)
fair-mappings/fairmapper test fair-mappings/beacon-v2 -v
fair-mappings/fairmapper test fair-mappings/dcat-fdp -v

# CLI: fetch RDF from URL with frame
fair-mappings/fairmapper fetch-rdf https://fdp.health-ri.nl/catalog/5d6f222e-ba32-4930-9e77-d266f5ee64d9 \
  --frame fair-mappings/dcat-fdp/src/frames/catalog-with-datasets.jsonld

# E2e against remote server
fair-mappings/fairmapper e2e fair-mappings/beacon-v2 \
  --server http://localhost:8080 --schema fairmapperTest -v
```

### Build CLI first
```bash
./gradlew :backend:molgenis-emx2-fairmapper-cli:shadowJar
```
