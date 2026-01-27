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
| `query`     | `.sql` | Variables JSON | Query result JSON | PostgreSQL (planned) |

Notes:
- RDF output is handled via content negotiation (`output: turtle`), not as a step type
- RDF input (fetch) is handled at mapping level, not as a step type

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
