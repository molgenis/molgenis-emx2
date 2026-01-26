# DCAT Harvesting Specification

## Purpose

Harvest DCAT metadata from FAIR Data Points (FDP) and import into MOLGENIS catalogue schema.

## Use Case

```bash
# CLI: run a mapping
./fairmapper run dcat-fdp harvest-catalog --source https://fdp.example.org/catalog/123

# HTTP: same mapping exposed as endpoint (optional)
POST /{schema}/api/harvest
Body: { "SOURCE_URL": "https://fdp.example.org/catalog/123" }
```

## fairmapper.yaml Schema (v2)

### Breaking Changes from v1
- `endpoints` → `mappings` (backwards compatible, deprecated)
- `path` → `endpoint` (backwards compatible, deprecated)
- Added `name` field for CLI-addressable mappings
- Added `fetch` step type with frame support

### Full Schema

```yaml
name: string                    # bundle identifier (required)
version: string                 # semver (optional)

mappings:                       # list of mappings (was: endpoints)
  - name: string                # CLI identifier (required if no endpoint)
    endpoint: string            # HTTP path with {schema} placeholder (optional)
    methods: [GET, POST]        # HTTP methods (required if endpoint present)
    steps: Step[]               # processing pipeline
    e2e:                        # end-to-end tests (optional)
      schema: string
      tests: E2eTest[]
```

### Step Types (Strategy Pattern)

Each step has exactly ONE of these keys:

```yaml
# Fetch: HTTP GET RDF, follow links, apply frame
- fetch: string                 # URL or ${VARIABLE}
  accept: string                # content-type (default: text/turtle)
  frame: string                 # path to JSON-LD frame file (optional)
  maxDepth: integer             # max depth to follow links (default: 5)
  maxCalls: integer             # max HTTP requests per record (default: 50)
  tests: TestCase[]

# Transform: JSLT JSON transformation
- transform: string             # path to .jslt file
  tests: TestCase[]

# Query: GraphQL query
- query: string                 # path to .gql file
  tests: TestCase[]             # (limited - needs mock data)

# Mutate: GraphQL mutation (NEW)
- mutate: string                # path to .gql file
```

### TestCase Schema

```yaml
tests:
  - input: string               # path to input file
    output: string              # path to expected output file
```

| Step Type | Input File | Output File |
|-----------|------------|-------------|
| fetch | .ttl (Turtle RDF) | .json (framed JSON-LD) |
| transform | .json | .json |
| query | .json (variables) | .json (mock result) |

### Mapping Rules

| Has `name` | Has `endpoint` | Result |
|------------|----------------|--------|
| ✓ | ✓ | Both CLI + HTTP |
| ✓ | ✗ | CLI only |
| ✗ | ✓ | HTTP only (auto-generate name from path) |
| ✗ | ✗ | Invalid |

## Example: dcat-fdp Bundle

```
fair-mappings/dcat-fdp/
├── fairmapper.yaml
├── src/
│   ├── frames/
│   │   └── catalog-with-datasets.jsonld
│   ├── transforms/
│   │   └── to-molgenis.jslt
│   └── mutations/
│       └── upsert-resources.gql
└── test/
    ├── fetch/
    │   ├── catalog.ttl              # input: local Turtle
    │   └── catalog.json             # output: expected framed JSON
    └── transform/
        ├── input.json
        └── output.json
```

### fairmapper.yaml

```yaml
name: dcat-fdp
version: 1.0.0

mappings:
  - name: harvest-catalog
    endpoint: /{schema}/api/harvest
    methods: [POST]
    steps:
      - fetch: ${SOURCE_URL}
        frame: src/frames/catalog-with-datasets.jsonld
        maxCalls: 10
        tests:
          - input: test/fetch/catalog.ttl
            output: test/fetch/catalog.json

      - transform: src/transforms/to-molgenis.jslt
        tests:
          - input: test/transform/input.json
            output: test/transform/output.json

      - mutate: src/mutations/upsert-resources.gql
```

### catalog-with-datasets.jsonld (Frame)

```json
{
  "@context": {
    "dcat": "http://www.w3.org/ns/dcat#",
    "dcterms": "http://purl.org/dc/terms/"
  },
  "@type": "dcat:Catalog",
  "dcterms:title": {},
  "dcterms:description": {},
  "dcterms:publisher": {
    "@embed": "@always"
  },
  "dcat:dataset": {
    "@type": "dcat:Dataset",
    "@embed": "@always",
    "dcterms:title": {},
    "dcterms:description": {},
    "dcterms:identifier": {},
    "dcat:keyword": []
  }
}
```

## Implementation Architecture

### Package Structure

```
org.molgenis.emx2.fairmapper/
├── RunFairMapper.java              # CLI entry point
├── BundleLoader.java               # YAML parsing + validation
├── model/
│   ├── MappingBundle.java          # record(name, version, mappings)
│   ├── Mapping.java                # record(name, endpoint, methods, steps, e2e)
│   └── step/                       # Strategy pattern for steps
│       ├── StepConfig.java         # sealed interface
│       ├── FetchStep.java          # record implements StepConfig
│       ├── TransformStep.java
│       ├── QueryStep.java
│       └── MutateStep.java
├── executor/
│   ├── StepExecutor.java           # interface
│   ├── FetchExecutor.java          # uses rdf package
│   ├── TransformExecutor.java      # uses JsltTransformEngine
│   ├── QueryExecutor.java          # uses GraphqlClient
│   └── MutateExecutor.java         # uses GraphqlClient
└── rdf/                            # RDF processing (DONE)
    ├── RdfSource.java              # interface for testability
    ├── RdfFetcher.java             # HTTP implementation
    ├── FrameAnalyzer.java          # extract @embed predicates
    ├── FrameDrivenFetcher.java     # recursive fetch
    ├── JsonLdFramer.java           # apply frame (Titanium)
    └── RdfToJsonLd.java            # Model → JSON-LD
```

### Dependencies

```gradle
// Already added
implementation 'org.eclipse.rdf4j:rdf4j-rio-turtle:4.3.8'
implementation 'org.eclipse.rdf4j:rdf4j-rio-jsonld:4.3.8'
implementation 'org.eclipse.rdf4j:rdf4j-model:4.3.8'
implementation 'com.apicatalog:titanium-json-ld:1.7.0'

// To add for JSON comparison in tests
testImplementation 'org.skyscreamer:jsonassert:1.5.1'
```

## CLI Commands

### Existing
```bash
./fairmapper validate <bundle>           # validate bundle structure
./fairmapper test <bundle> [-v]          # run step tests
./fairmapper dry-run <bundle> <input>    # transform without queries
./fairmapper e2e <bundle> --server ...   # end-to-end tests
```

### New
```bash
./fairmapper run <bundle> <mapping> [--var=value...]
# Example:
./fairmapper run dcat-fdp harvest-catalog --SOURCE_URL=https://fdp.example.org/...
```

## Implementation Phases

### Phase 1: RDF Fetch + JSON-LD ✅
- RdfFetcher, RdfToJsonLd
- fetch-rdf CLI command
- Basic tests

### Phase 2: Frame-Driven Link Resolution ✅
- FrameAnalyzer, FrameDrivenFetcher, JsonLdFramer
- RdfSource interface for testability
- Unit tests with mocks

### Phase 3: Schema v2 + Fetch Step Testing (NEXT)
- Update model classes (Mapping, StepConfig hierarchy)
- Update BundleLoader for new schema
- Add FetchExecutor
- Add JSONAssert for output comparison
- Create dcat-fdp bundle with test fixtures
- Backwards compatibility for v1 schema

### Phase 4: JSLT Transforms
- Create to-molgenis.jslt transform
- Map framed DCAT → MOLGENIS Resources format

### Phase 5: Mutations
- Add MutateExecutor
- GraphQL mutation support
- Transaction wrapping for batch imports

### Phase 6: CLI `run` Command
- Wire up full pipeline: fetch → transform → mutate
- Variable substitution (${SOURCE_URL})

## Target Schema Mapping (DCAT → MOLGENIS)

| DCAT Predicate | MOLGENIS Table.Column | Notes |
|----------------|----------------------|-------|
| Subject IRI | Resources.id | UUID from URL path |
| rdf:type | Resources.resourceType | Catalogue/Dataset |
| dcterms:title | Resources.name | Required |
| dcterms:description | Resources.description | |
| dcterms:identifier | Resources.pid | DOI or other PID |
| dcat:contactPoint | Resources.contacts | Ref to Contacts |
| dcterms:publisher | Resources.organisations | Ref to Organisations |
| dcat:keyword | Resources.keywords | String array |
| dcat:theme | Resources.themes | Ontology ref |
| dcat:landingPage | Resources.homepage | URL |
| dcterms:issued | Resources.dateCreated | Date |
| dcterms:modified | Resources.dateModified | Date |

## Open Questions

1. ~~JSON-LD framing deterministic?~~ Yes, with Titanium
2. ~~Link following automatic?~~ Yes, frame-driven
3. Organisation handling: create new or match existing by name/ROR?
4. Theme mapping: FDP EU URIs → local ontology codes?
5. Error handling: skip invalid records or fail entire harvest?

## Test Fixtures Location

```
fair-mappings/dcat-fdp/test/fetch/
├── catalog.ttl          # Sample catalog in Turtle
├── catalog.json         # Expected framed output
├── dataset.ttl          # Sample dataset
└── dataset.json         # Expected framed output
```

These are LOCAL files for offline testing, not real FDP URLs.
