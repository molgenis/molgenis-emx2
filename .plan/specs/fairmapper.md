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

## fairmapper.yaml Schema (v2)

```yaml
name: beacon-v2         # Required: bundle identifier
version: 2.0.0          # version of the mapping, user defined

mappings:               # (was: endpoints)
  - name: individuals   # CLI identifier (required if no endpoint)
    endpoint: /{schema}/api/beacon/individuals  # HTTP path (optional)
    methods: [GET, POST]
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
```

Backwards compatibility: `endpoints` and `path` still work but are deprecated.

## Step Types

| Type        | Extension | Input | Output | Engine |
|-------------|-----------|-------|--------|--------|
| `fetch`     | URL | RDF (Turtle) | JSON-LD (framed) | RDF4J + Titanium |
| `transform` | `.jslt` | JSON | JSON | JSLT (schibsted) |
| `query`     | `.gql` | Variables JSON | Query result JSON | molgenis-emx2-graphql |
| `mutate`    | `.gql` | Variables JSON | Mutation result | molgenis-emx2-graphql |
| `query`     | `.sql` | Variables JSON | Query result JSON | PostgreSQL (planned) |

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
