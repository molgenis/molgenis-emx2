# FAIRmapper Specification

## Purpose

Enable data managers to create API adapters and ETL pipelines without Java code. Configure transformations declaratively using YAML + JSLT or SQL.

## Core Concepts

### Bundle
Directory containing `fairmapper.yaml` + transform/query files + tests. Located in `fair-mappings/`.

### Endpoint
HTTP route with processing steps. Path supports `{schema}` placeholder for multi-tenant access.

### Step
Single processing unit:
- `transform` - JSLT JSON transformation. Later we will add other engines (based on file extension currently .jslt)
- `query` - GraphQL query execution. Later we will also allow postgresql sql (based on file extension, .gql vs .sql)

### E2e Test
Full pipeline test against live database with JSON input/output validation.

## fairmapper.yaml Schema

```yaml
name: beacon-v2         # Required: bundle identifier
version: 2.0.0          # version of the mapping, user defined

endpoints:
  - path: /{schema}/api/beacon/individuals
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

## Step Types

| Type        | Extension | Input | Output | Engine |
|-------------|-----------|-------|--------|--------|
| `transform` | `.jslt` | JSON | JSON | JSLT (schibsted) |
| `query`     | `.gql` | Variables JSON | Query result JSON | molgenis-emx2-graphql |
| `query`     | `.sql` | Variables JSON | Query result JSON | PostgreSQL (planned) |

N.b we might later have other edge formats. 

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

# E2e tests against remote (not yet implemented)
fairmapper e2e <bundle-path> --server <url> --token <token>
```

### Exit Codes
- 0: Success
- 1: Error (validation failed, test failed, etc.)
- 2: Feature not implemented

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

1. `fairmapper.yaml` must have: name, endpoints
2. Each step must have exactly one of: `transform`, `query`, or `sql`
3. Transform files must exist and have `.jslt` extension
4. Query files must exist and have `.gql` extension
5. E2e test method must be GET or POST
6. E2e input/output files must exist
7. Version field optional (warning logged if missing)

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

## Future Extensions

- SQL step type for PostgreSQL JSON queries
- Additional transform engines (JSONata, jq)
- Mutation support for ETL ingest
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
