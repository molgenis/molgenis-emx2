# FAIRmapper Specification

## Purpose

Enable data managers to create API adapters and ETL pipelines without Java code. Configure transformations declaratively using YAML + JSLT or SQL.

## Core Concepts

### Bundle
Directory containing `fairmapper.yaml` + transform/query files + tests. Located in `fair-mappings/`.

### Mapping
Named processing pipeline. Can expose HTTP endpoint or run via CLI.
- `name` - CLI identifier
- `endpoint` - HTTP path with `{schema}` placeholder (optional)

### Step
Single processing unit (strategy pattern):
- `fetch` - HTTP GET RDF, follow links, apply JSON-LD frame
- `transform` - JSLT JSON transformation
- `query` - GraphQL query execution
- `mutate` - GraphQL mutation execution (planned)

### E2e Test
Full pipeline test against live database with JSON input/output validation.

## fairmapper.yaml Schema (v3)

```yaml
name: beacon-v2         # Required: bundle identifier
version: 2.0.0          # version of the mapping, user defined

mappings:
  # Endpoint mapping (HTTP API)
  - endpoint: /{schema}/api/beacon/individuals  # HTTP path (endpoint is identifier)
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

  # FDP publish endpoint (outputs RDF)
  - endpoint: /{schema}/api/fdp/catalog/{id}
    methods: [GET]
    output: turtle           # default output = Turtle RDF
    steps:
      - query: src/queries/get-catalog.gql
      - transform: src/transforms/to-dcat.jslt

  # CLI-only mapping (fetch from external)
  - name: harvest-catalog   # CLI identifier (required when no endpoint)
    input: turtle            # expect Turtle input from SOURCE_URL
    frame: src/frames/catalog.jsonld  # required when input is RDF
    steps:
      - fetch: ${SOURCE_URL}
      - transform: src/transforms/to-molgenis.jslt
      - mutate: src/mutations/upsert.gql
```

### Mapping Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `endpoint` | string | - | HTTP path with `{schema}` placeholder |
| `name` | string | - | CLI identifier (required when no endpoint) |
| `methods` | array | `[GET]` | HTTP methods to register |
| `input` | string | `json` | Default request body format |
| `output` | string | `json` | Default response format |
| `frame` | string | - | JSON-LD frame for RDF request parsing |
| `steps` | array | - | Processing pipeline (required) |
| `e2e` | object | - | End-to-end test configuration |

**When to use mapping-level `frame`:**
- HTTP endpoints that accept RDF in request body (`input: turtle`)
- Frame is applied to incoming request before pipeline

**When to use fetch step `frame`:**
- CLI mappings that fetch RDF from external URL
- Frame is applied to fetched RDF data

### Supported Formats

| Format ID | MIME Type | Notes |
|-----------|-----------|-------|
| `json` | `application/json` | Default for most endpoints |
| `turtle` | `text/turtle` | RDF Turtle |
| `jsonld` | `application/ld+json` | JSON-LD |
| `ntriples` | `application/n-triples` | N-Triples RDF |
| `csv` | `text/csv` | Tabular data |

### Mapping Validation Rules

| Rule | Condition | Result |
|------|-----------|--------|
| Identifier required | No `endpoint` | `name` required |
| RDF input needs frame | `input: turtle\|jsonld\|ntriples` | `frame` required |
| No conflicting patterns | `endpoint` + `fetch` step | Error |

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
| `fetch`     | URL | RDF (Turtle) | JSON-LD (framed) | RDF4J + Titanium |
| `transform` | `.jslt` | JSON | JSON | JSLT (schibsted) |
| `query`     | `.gql` | Variables JSON | Query result JSON | molgenis-emx2-graphql |
| `mutate`    | `.gql` | Variables JSON | Mutation result | molgenis-emx2-graphql |
| `query`     | `.sql` | Variables JSON | Query result JSON | PostgreSQL (planned) |

Note: RDF output is handled via content negotiation (`output: turtle`), not as a step type.

### Fetch Step Options

```yaml
- fetch: ${SOURCE_URL}      # URL or variable
  accept: text/turtle       # Content-Type (default: text/turtle)
  frame: src/frames/x.jsonld  # JSON-LD frame file
  maxDepth: 5               # Max link-following depth (default: 5)
  maxCalls: 50              # Max HTTP requests per record (default: 50)
  tests:
    - input: test/catalog.ttl   # Local Turtle file
      output: test/catalog.json # Expected framed JSON
```

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

1. `fairmapper.yaml` must have: name, mappings (or endpoints for v1)
2. Each mapping must have: name OR endpoint (at least one)
3. Each step must have exactly one of: `fetch`, `transform`, `query`, `mutate`
4. Transform files must exist and have `.jslt` extension
5. Query files must exist and have `.gql` extension
6. Frame files must exist and have `.json` or `.jsonld` extension
7. E2e test method must be GET or POST
8. E2e input/output files must exist
9. Version field optional (warning logged if missing)

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

## Future Extensions

- SQL step type for PostgreSQL JSON queries
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

### SQL Queries
4. Variable binding: `:name` or `${name}`? Todo: check with how report module works
5. Result format: Must return single `json_build_object`? Currently yes.

### General
6. Mixed pipelines: Allow `transform → sql → transform`? Yes.
7. Performance: SQL faster than GraphQL+JSLT? To be decided
8. Scalability: how to deal with large payloads, do we need safety valve?
