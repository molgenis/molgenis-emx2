# FAIRmapper Specification

## Purpose

Enable data managers to create API adapters and ETL pipelines without Java code. Configure transformations declaratively using YAML + JSLT or SQL.

## Core Concepts

### Bundle
Directory containing `fairmapper.yaml` + transform/query files + tests. Located in `fair-mappings/`.

### Mapping
Named processing pipeline. Two types:
- **API mapping**: exposes HTTP endpoint (`endpoint` field)
- **Harvest mapping**: fetches from external RDF source (`fetch` + `frame` fields)

Fields:
- `name` - required identifier
- `endpoint` - HTTP path with `{schema}` placeholder (API mappings)
- `fetch` - RDF source URL (harvest mappings)
- `frame` - JSON-LD frame file (required with `fetch`)

Validation: `endpoint` and `fetch` are mutually exclusive.

### Step
Single processing unit (strategy pattern):
- `transform` - JSLT JSON transformation
- `query` - GraphQL query execution
- `mutate` - GraphQL mutation execution

### E2e Test
Full pipeline test against live database with JSON input/output validation.

## fairmapper.yaml Schema (v4)

```yaml
name: beacon-v2         # Required: bundle identifier
version: 2.0.0          # version of the mapping, user defined

mappings:
  # API mapping (HTTP endpoint)
  - name: beacon-individuals
    endpoint: /{schema}/api/beacon/individuals
    methods: [GET, POST]
    input: json              # default request format (when no Content-Type)
    output: json             # default response format (when no Accept)
    steps:
      - transform: src/request-to-variables.jslt
        tests:
          - input: test/basic.input.json
            output: test/basic.output.json
      - query: src/individuals.gql
      - transform: src/individuals-response.jslt
    e2e:
      schema: patientRegistry
      tests:
        - method: POST
          input: test/e2e/request.json
          output: test/e2e/expected.json

  # API mapping (outputs RDF via content negotiation)
  - name: fdp-catalog
    endpoint: /{schema}/api/fdp/catalog/{id}
    methods: [GET]
    output: turtle           # default output = Turtle RDF
    steps:
      - query: src/queries/get-catalog.gql
      - transform: src/transforms/to-dcat.jslt

  # Harvest mapping (fetch from external RDF source)
  - name: harvest-catalog
    fetch: ${SOURCE_URL}
    frame: src/frames/catalog.jsonld
    steps:
      - transform: src/transforms/to-molgenis.jslt
      - mutate: src/mutations/upsert.gql
```

### Mapping Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | Yes | Unique identifier for the mapping |
| `endpoint` | string | Either endpoint or fetch | HTTP path with `{schema}` placeholder |
| `fetch` | string | Either endpoint or fetch | RDF source URL (supports `${VAR}` placeholders) |
| `frame` | string | When fetch is set | JSON-LD frame file for RDF parsing |
| `methods` | array | No (default: `[GET]`) | HTTP methods to register (endpoint only) |
| `input` | string | No (default: `json`) | Default request body format |
| `output` | string | No (default: `json`) | Default response format |
| `steps` | array | Yes | Processing pipeline |
| `e2e` | object | No | End-to-end test configuration |

### Mapping Type Rules

| Type | Required Fields | Optional Fields |
|------|-----------------|-----------------|
| API | `name`, `endpoint`, `steps` | `methods`, `input`, `output`, `frame`, `e2e` |
| Harvest | `name`, `fetch`, `frame`, `steps` | - |

**Validation:**
- `name` always required
- `endpoint` and `fetch` are mutually exclusive
- `frame` required when `fetch` is set

### Supported Formats

| Format ID | MIME Type | Notes |
|-----------|-----------|-------|
| `json` | `application/json` | Default for most endpoints |
| `turtle` | `text/turtle` | RDF Turtle |
| `jsonld` | `application/ld+json` | JSON-LD |
| `ntriples` | `application/n-triples` | N-Triples RDF |
| `csv` | `text/csv` | Tabular data |

### Mapping Validation Rules

| Rule | Error Message |
|------|---------------|
| `name` missing | "Mapping requires 'name' field" |
| Both `endpoint` and `fetch` set | "Mapping cannot have both 'endpoint' and 'fetch'" |
| Neither `endpoint` nor `fetch` set | "Mapping requires either 'endpoint' or 'fetch'" |
| `fetch` without `frame` | "Mapping with 'fetch' requires 'frame' field" |

### Content Negotiation

HTTP endpoints support content negotiation via headers. The `input`/`output` fields specify defaults when headers are absent.

**Response format** (Accept header):

| Accept Header | Response Format |
|---------------|-----------------|
| `text/turtle` | Turtle RDF |
| `application/ld+json` | JSON-LD |
| `application/n-triples` | N-Triples |
| `text/csv` | CSV |
| `application/json` or absent | JSON (or mapping default) |

**Request format** (Content-Type header):

| Content-Type Header | Processing |
|---------------------|------------|
| `application/json` or absent | Pass-through to pipeline |
| `text/csv` | Parse to JSON array |
| `text/turtle` | Parse + apply frame → JSON-LD |
| `application/ld+json` | Apply frame → JSON-LD |

Backwards compatibility: `endpoints`, `path`, `name` on endpoint mappings still work but deprecated.

## Step Types

| Type        | Extension | Input | Output | Engine |
|-------------|-----------|-------|--------|--------|
| `transform` | `.jslt` | JSON | JSON | JSLT (schibsted) |
| `query`     | `.gql` | Variables JSON | Query result JSON | molgenis-emx2-graphql |
| `mutate`    | `.gql` | Variables JSON | Mutation result | molgenis-emx2-graphql |
| `sql`       | `.sql` | Variables JSON | Query result JSON | PostgreSQL via `schema.retrieveSql()` |
| `frame`     | `.jsonld` | JSON-LD | JSON-LD | Titanium JSON-LD |
| `sparql`    | `.sparql` | RDF Model | RDF Model | RDF4J in-memory (planned) |
| `mapping`   | `.yaml` | JSON-LD | JSON | YAML field mapping (planned) |

Notes:
- RDF output is handled via content negotiation (`output: turtle`), not as a step type
- RDF input (fetch) is handled at mapping level, not as a step type
- `sparql` and `mapping` steps are planned for Phase 11

### SQL Step (Alternative to GraphQL + JSLT)

SQL queries can produce JSON-LD directly using PostgreSQL JSON functions:

```yaml
steps:
  - sql: src/queries/get-catalog.sql
```

**SQL file format:**
```sql
SELECT json_build_object(
  '@context', json_build_object('dcat', 'http://www.w3.org/ns/dcat#'),
  '@id', ${base_url} || '/' || ${schema} || '/api/catalog/' || r.id,
  '@type', 'dcat:Catalog',
  'dct:title', r.name
) AS result
FROM "Resources" r
WHERE r.id = ${id}
```

**Parameters:** `${name}` syntax, bound via `schema.retrieveSql(sql, params)`

**Benefits over GraphQL + JSLT:**
- Single step instead of two
- SQL more widely known than JSLT
- Direct JSON-LD construction
- Access to database timestamps (`mg_insertedOn`, `mg_updatedOn`)

**Example bundle:** `fair-mappings/dcat-fdp-sql/`

### SPARQL CONSTRUCT Step (Planned - Phase 11)

Transforms RDF using SPARQL CONSTRUCT queries. Alternative to JSLT for RDF-native users.

```yaml
steps:
  - sparql: src/transforms/to-dcat.sparql
```

**SPARQL file format:**
```sparql
PREFIX dcat: <http://www.w3.org/ns/dcat#>
PREFIX dct: <http://purl.org/dc/terms/>
PREFIX my: <urn:molgenis:>

CONSTRUCT {
  ?dcat a dcat:Dataset ;
    dct:title ?name .
}
WHERE {
  ?s a my:Resources ;
    my:name ?name .
  BIND(IRI(CONCAT("https://example.org/dataset/", ?id)) AS ?dcat)
}
```

**Step fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `sparql` | string | yes | Path to SPARQL CONSTRUCT query file |

**Implementation:**
- Uses RDF4J in-memory repository (no external server)
- Input: RDF Model (from previous step or auto-converted from JSON-LD)
- Output: RDF Model (frame step converts to JSON-LD)

**Dependencies:**
```gradle
implementation 'org.eclipse.rdf4j:rdf4j-repository-sail:4.3.8'
implementation 'org.eclipse.rdf4j:rdf4j-sail-memory:4.3.8'
implementation 'org.eclipse.rdf4j:rdf4j-queryparser-sparql:4.3.8'
```

**Safety limits:**
- Max triples: 100,000
- Query timeout: 30 seconds

**Example bundle:** `fair-mappings/dcat-sparql/`

### Mapping Override Step (Planned - Phase 11)

Declarative YAML-based field mapping. Alternative to JSLT for simple mappings.

```yaml
steps:
  - frame: src/frames/input.jsonld
  - mapping: src/overrides.yaml
  - mutate: src/upsert.gql
```

**Mapping file format:**
```yaml
fields:
  dct:title: name
  dct:description: description
  dcat:keyword: keywords
  dct:publisher:
    target: organisation
    extract: foaf:name

types:
  dcat:Catalog: Resources
  dcat:Dataset: Resources

id:
  extract: last-segment
  prefix: ""
```

**Step fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `mapping` | string | yes | Path to YAML mapping file |

**Mapping file sections:**

| Section | Purpose |
|---------|---------|
| `fields` | Map RDF predicates to MOLGENIS columns |
| `types` | Map `@type` values to table names |
| `id` | Configure ID extraction from `@id` |

**ID extraction modes:**

| Mode | Config | Example |
|------|--------|---------|
| Last segment (default) | `extract: last-segment` | `/catalog/123` → `123` |
| Full IRI | `extract: full` | Keep entire IRI |
| Regex | `extract: ".*[/#]([^/#]+)$"` | Custom pattern |

**Benefits:**
- No code for simple field mappings
- Easier than JSLT for data managers
- Can override schema semantics without schema changes

### RDF Pipeline Mode (Planned - Phase 11)

When `pipeline: rdf`, steps pass RDF Model instead of JSON between them.

```yaml
- name: harvest-sparql
  fetch: ${SOURCE_URL}
  pipeline: rdf
  steps:
    - sparql: src/normalize.sparql
    - sparql: src/to-molgenis.sparql
    - frame: src/output.jsonld
    - mutate: src/upsert.gql
```

**Mapping field:**

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `pipeline` | string | no | `json` | Pipeline format: `json` or `rdf` |

**Format rules:**
- `transform` (JSLT) requires JSON format
- `sparql` requires RDF format
- `frame` converts RDF → JSON
- Auto-conversion: JSON-LD → RDF when next step is SPARQL

### Auto Mode (Planned - Phase 12)

Zero-config harvesting using MOLGENIS schema semantics.

```yaml
- name: harvest-auto
  fetch: ${SOURCE_URL}
  auto: true
  target: catalogue
```

**Mapping fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `auto` | boolean | no | Enable auto-mapping from schema semantics |
| `target` | string | when auto=true | Target schema name |

**Validation:**
- `auto: true` requires `target` field
- `auto: true` requires `fetch` field
- Target schema must exist and have semantic annotations

**Equivalent to:**
```yaml
steps:
  - frame: ${target}/api/jsonld/frame
    unmapped: true
  - import: ${target}/api/jsonld/import
```

### Fetch (Mapping-Level)

For harvest mappings, RDF is fetched and framed before the pipeline runs:

```yaml
- name: harvest-catalog
  fetch: ${SOURCE_URL}                    # URL with variable placeholders
  frame: src/frames/catalog.jsonld        # JSON-LD frame (required)
  steps:
    - transform: src/transforms/to-molgenis.jslt
    - mutate: src/mutations/upsert.gql
```

**Fetch behavior:**
- Fetches RDF from URL (Turtle format)
- Follows links based on frame structure (link-driven fetching)
- Applies JSON-LD frame to produce input for pipeline
- Max depth: 5 levels, max calls: 50 per record

## CLI Commands

```bash
# Show help
fairmapper --help

# Validate bundle structure
fairmapper validate <bundle-path>

# Run unit tests for transforms
fairmapper test <bundle-path> [-v]

# Dry-run: transform input without queries
fairmapper dry-run <bundle-path> <input.json>

# E2e tests against remote MOLGENIS server
fairmapper e2e <bundle-path> --server <url> [--token <token>] [--schema <name>] [-v]
```

### E2e Command Options
| Option | Env Var | Description |
|--------|---------|-------------|
| `--server` | `MOLGENIS_SERVER` | Base URL (required) |
| `--token` | `MOLGENIS_TOKEN` | API token for auth |
| `--schema` | - | Override e2e config schema |
| `-v` | - | Verbose output |

### Exit Codes
- 0: Success (all tests pass)
- 1: Error (validation failed, test failed, connection error)

## Processing Flow

### Standard (JSLT + GraphQL)
```
HTTP Request
     ↓
[transform] → request to variables
     ↓
[query] → GraphQL execution
     ↓
[transform] → result to response format
     ↓
HTTP Response (JSON)
```

## Validation Rules

1. `fairmapper.yaml` must have: `name`, `mappings`
2. Each mapping must have: `name`, either `endpoint` or `fetch`, `steps`
3. Harvest mappings (`fetch`) require `frame` field
4. Each step must have exactly one of: `transform`, `query`, `mutate`
5. Transform files must exist and have `.jslt` extension
6. Query files must exist and have `.gql` extension
7. Frame files must exist and have `.json` or `.jsonld` extension
8. E2e test method must be GET or POST
9. E2e input/output files must exist
10. Version field optional (warning logged if missing)

## Error Handling

All validation errors throw `MolgenisException` with clear message indicating:
- Which file is missing/invalid
- What the expected format is

## Multi-tenancy

Bundles are global. Schema is extracted from URL path (`{schema}` placeholder) at runtime.

## Test Modes

| Mode | Runs | Data |
|------|------|------|
| Unit tests | Per-step transform validation | Mock JSON files |
| E2e tests | Full pipeline | Live database schema |

## JSLT Behavior

- Empty arrays preserved in output
- Imports supported for shared transforms
- Paths relative to transform file

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Config format | YAML (`fairmapper.yaml`) | Human-readable, familiar |
| Schema structure | Flat (name, version, mappings) | Simple, no apiVersion/kind overhead |
| Transform engine | JSLT | Powerful, imports supported, Java native |
| Query engine | GraphQL first, SQL later | GraphQL already in MOLGENIS |
| CLI framework | Picocli | Colored output, subcommands, well-documented |
| Distribution | JAR with shell wrapper | Simple deployment |
| Multi-tenancy | `{schema}` in paths | Runtime extraction from URL |
| Mutations | MOLGENIS `insert` | Upsert behavior (updates if key exists) |
| RDF parsing | RDF4J + Titanium JSON-LD | Standards-compliant, frame support |
| JSLT special keys | `get-key(., "@id")` | Bracket notation fails for `@`/`:` chars |

## Security

### SSRF Protection (fetch step)
| Decision | Choice |
|----------|--------|
| Default policy | Same-origin: fetches only allowed to source URL domain |
| Subdomain handling | Allowed (e.g., `api.fdp.example.org` when source is `fdp.example.org`) |
| Bypass option | `--allow-external` CLI flag for trusted scenarios |
| Scheme validation | Only `http://` and `https://` allowed |

### Path Traversal Protection
| Decision | Choice |
|----------|--------|
| Validation method | `PathValidator.validateWithinBase()` for all file path resolution |
| Canonical path check | Prevents `../` escape attempts |

## Frame Step

Applies JSON-LD framing to reshape RDF data. Enables two-frame pattern for harvesting.

```yaml
steps:
  - frame: src/frames/resources.jsonld
    unmapped: true    # keep unmapped properties
  - transform: src/transforms/fix-exceptions.jslt
  - frame: src/frames/resources.jsonld
    # unmapped: false (default) - strip to @explicit: true
```

### Frame Step Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `frame` | string | Yes | Path to JSON-LD frame file (.json or .jsonld) |
| `unmapped` | boolean | No (default: false) | If true, sets `@explicit: false` recursively to keep unmapped properties |

### Frame File Requirements

Frame files should be authored with `@explicit: true` at all nested levels:

```json
{
  "@context": { ... },
  "@type": ["dcat:Catalog", "dcat:Dataset"],
  "@explicit": true,
  "name": {},
  "dcat:dataset": {
    "@type": "dcat:Dataset",
    "@embed": "@always",
    "@explicit": true
  }
}
```

When `unmapped: true`, the step recursively flips all `@explicit: true` to `false`.

---

## MOLGENIS JSON-LD Endpoints (Separate PR)

Two endpoints to simplify RDF harvesting by leveraging existing schema semantics.

### Auto-Frame Endpoint

```
GET /{schema}/api/jsonld/frame
```

Generates JSON-LD frame from schema metadata (tables with `semantics` annotations).

**Generation rules:**
| Schema metadata | Frame output |
|-----------------|--------------|
| `table.semantics` | `@type` array |
| `column.semantics` | Property key in context |
| `column.isReference()` | `@embed: @always` |
| Schema namespaces | `@context` prefixes |

**Output includes `@explicit: true` at all levels** (strict by default).

### JSON-LD Import Endpoint (OTHER PR)

```
POST /{schema}/api/jsonld/import
Content-Type: application/ld+json
```

Accepts framed JSON-LD, imports to MOLGENIS tables.

#### Design Decisions

| Question | Decision | Rationale |
|----------|----------|-----------|
| **Target table** | From `@type` → table with matching `semantics` | Natural RDF mapping; if multiple types, first match wins |
| **Nested objects** | Recursively import, create refs | Handles `@embed` structure naturally |
| **`@graph` array** | Import all items in single transaction | Deferred FK checks make order irrelevant |
| **Multiple types** | Single request can contain Catalog + Dataset | Common in DCAT; single tx with deferred FK |
| **`@context` usage** | Ignored (assume already resolved) | Framing already expanded prefixes |
| **ID extraction** | Configurable, default = last path segment | See ID Configuration below |
| **Unknown properties** | Ignore, warning in response (one per type) | Flexibility; schema defines what matters |
| **Conflict handling** | Merge (like CSV import) | Uses existing MOLGENIS merge behavior |
| **Missing required fields** | Fail entire request | Atomic; all or nothing |
| **HTTP response** | 200 + summary (not 201) | Bulk import, not single resource creation |
| **Type column mapping** | `@type` → type table lookup | e.g., `dcat:Catalog` → `[{name: "Catalogue"}]` |

#### ID Configuration

| Mode | Config | Example |
|------|--------|---------|
| Last segment (default) | - | `https://fdp.org/catalog/123` → `123` |
| Full IRI | `idExtract: full` | `https://fdp.org/catalog/123` → full string |
| Regex pattern | `idExtract: ".*[/#]([^/#]+)$"` | Custom extraction |

Schema setting: `jsonld.idExtract` (applies to all tables in schema).

#### Standards Compliance

**LDP-inspired but not LDP-compliant:**

| LDP says | We do | Rationale |
|----------|-------|-----------|
| POST returns 201 + Location | 200 + summary | Bulk import, not single resource |
| One resource per POST | Multiple in @graph | Practical for harvesting |
| Server assigns URI | Client provides @id | We extract ID from existing URIs |

References:
- [W3C LDP 1.0](https://www.w3.org/TR/ldp/)
- [REST HTTP Methods](https://restfulapi.net/http-methods/)

#### Import Algorithm

```
1. Parse JSON-LD input
2. If @graph present, iterate items; else treat root as single item
3. Begin transaction (deferred FK checks)
4. For each item:
   a. Lookup table from @type → table.semantics match
   b. Extract id from @id using idExtract setting
   c. Map @type to type table (if type column exists)
   d. For each property:
      - If column exists with matching name → include
      - If column.semantics matches property IRI → include
      - Else → ignore (add to warnings)
   e. For nested objects (refs):
      - Extract id, replace with {id: extractedId} reference
5. Merge all records (uses MOLGENIS CSV-import merge behavior)
6. Commit transaction
7. Return summary response
```

**Note:** Deferred FK checks mean import order doesn't matter. Catalog can reference Dataset before Dataset is inserted.

#### Example Request

```json
{
  "@context": { "dcat": "http://www.w3.org/ns/dcat#", "dct": "http://purl.org/dc/terms/" },
  "@graph": [
    {
      "@id": "https://fdp.example.org/catalog/health",
      "@type": "dcat:Catalog",
      "dct:title": "Health Data",
      "dcat:dataset": {
        "@id": "https://fdp.example.org/dataset/covid",
        "@type": "dcat:Dataset",
        "dct:title": "COVID Stats"
      }
    }
  ]
}
```

#### Example Response

```json
{
  "imported": 2,
  "skipped": 0,
  "tables": {
    "Resources": {"inserted": 2, "updated": 0}
  },
  "errors": []
}
```

---

## Simplified Harvesting Pipeline

With auto-frame and import endpoints, standard DCAT harvesting becomes:

```yaml
mappings:
  - name: harvest
    fetch: ${SOURCE_URL}
    steps:
      - frame: ${TARGET_SCHEMA}/api/jsonld/frame
        unmapped: true
      - import: ${TARGET_SCHEMA}/api/jsonld/import
```

No JSLT transforms needed for standard mappings.

---

## Future Extensions

- Task framework integration (async, progress, scheduling)
- Additional transform engines (JSONata, jq)
- Hot reload of bundles
- Web UI for bundle management

---

## Open Questions

### CLI
1. Distribution: JAR with shell wrapper? Yes
2. GraphQL execution: We should move to fully qualified paths which may include {schema}. Yes
3. Offline dev: Support mocking query results? We already have this with the step test cases.

### SQL Queries (Resolved)
4. Variable binding: `${name}` syntax (matches MOLGENIS `SqlRawQueryForSchema`)
5. Result format: SELECT with `json_build_object(...) AS result` column
6. Security: SQL from bundle files is trusted; parameters bound safely via JDBC

### General
6. Mixed pipelines: Allow `transform → sql → transform`? Yes.
7. Performance: SQL faster than GraphQL+JSLT? To be decided
8. Scalability: how to deal with large payloads, do we need safety valve?
