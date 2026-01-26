# FAIRmapper

FAIRmapper enables data managers to create API adapters and ETL pipelines without writing Java code. Configure transformations declaratively using YAML and JSLT.

> **Status**: FAIRmapper is under active development. Currently you can define and test transforms. Runtime endpoint registration is coming soon.

## Why FAIRmapper?

- **No Java required** - Configure adapters via YAML + JSLT
- **Testable** - Each step can have input/output test cases
- **Reusable** - Share transforms across bundles
- **FAIR principles** - Standardized mappings for interoperability

## Bundle Structure

A FAIRmapper bundle is a directory with a `mapping.yaml` and your transform/query files. You're free to organize files as you see fit. Here's one suggested structure:

```
beacon-v2/
├── mapping.yaml                    # Required: main configuration
├── src/                            # Your transforms and queries
│   ├── request-to-variables.jslt
│   ├── individuals-response.jslt
│   └── individuals.gql
└── test/                           # Optional: test cases
    ├── basic.input.json
    └── basic.output.json
```

Or keep it flat:
```
my-adapter/
├── mapping.yaml
├── transform.jslt
└── query.gql
```

## mapping.yaml

The main configuration file defines endpoints and their processing steps:

```yaml
apiVersion: molgenis.org/v1
kind: FairMapperBundle
metadata:
  name: beacon-v2
  version: 2.0.0

endpoints:
  - path: /{schema}/api/beacon/individuals
    methods: [GET, POST]
    steps:
      # Step 1: Transform incoming request to GraphQL variables
      - transform: src/request-to-variables.jslt
        tests:
          - input: test/basic.input.json
            output: test/basic.output.json

      # Step 2: Execute GraphQL query
      - query: src/individuals.gql

      # Step 3: Transform result to API response format
      - transform: src/individuals-response.jslt
```

### Step Types

| Type | Description |
|------|-------------|
| `transform` | JSON-to-JSON transformation using JSLT |
| `query` | GraphQL query execution |

*More step types may be added in future versions (e.g., `mutation`, `fetch`, `validate`).*

### Path Parameters

`{schema}` in the path is replaced with the MOLGENIS database name. For example, if your database is called `patientRegistry`, the endpoint becomes:
```
/patientRegistry/api/beacon/individuals
```

## Writing GraphQL Queries

Query steps execute GraphQL against your MOLGENIS schema. The variables from the previous transform step are passed in.

```graphql
# individuals.gql
query Individuals($limit: Int, $offset: Int) {
  Individuals(limit: $limit, offset: $offset) {
    id
    genderAtBirth { code name }
    yearOfBirth
  }
}
```

The query fetches from tables defined in your MOLGENIS schema. Variable names must match what your transform outputs.

## Writing JSLT Transforms

[JSLT](https://github.com/schibsted/jslt) is a JSON query and transformation language. See the [JSLT tutorial](https://github.com/schibsted/jslt/blob/master/tutorial.md) and [function reference](https://github.com/schibsted/jslt/blob/master/functions.md).

### Basic Example

Transform a Beacon request to GraphQL variables:

```jslt
// request-to-variables.jslt
{
  "filters": [for (.query.filters) {"field": .id, "value": .value}],
  "limit": .query.pagination.limit,
  "offset": .query.pagination.skip
}
```

Input:
```json
{
  "query": {
    "filters": [{"id": "genderAtBirth", "value": "male"}],
    "pagination": {"limit": 10, "skip": 0}
  }
}
```

Output:
```json
{
  "filters": [{"field": "genderAtBirth", "value": "male"}],
  "limit": 10,
  "offset": 0
}
```

### JSLT Quick Reference

| Syntax | Description | Example |
|--------|-------------|---------|
| `.field` | Access field | `.query.filters` |
| `[for (array) expr]` | Transform array | `[for (.items) .name]` |
| `if (cond) a else b` | Conditional | `if (.x) .x else "default"` |
| `let $var = expr` | Variable binding | `let $count = size(.items)` |
| `size(array)` | Array length | `size(.filters)` |
| `fallback(a, b)` | Null coalescing | `fallback(.name, "unknown")` |

## Test Cases

You can add test cases to verify your transforms. Each test specifies an input file and expected output file.

```yaml
steps:
  - transform: my-transform.jslt
    tests:
      - input: tests/case1-input.json
        output: tests/case1-expected.json
      - input: tests/edge-case.json
        output: tests/edge-case-expected.json
```

Name your test files however makes sense to you. Some conventions:
- Flat: `basic.input.json` / `basic.output.json`
- By step: `request-to-variables/basic.input.json`
- By feature: `filter-tests/gender.input.json`

Run tests:
```bash
./gradlew :backend:molgenis-emx2-fairmapper:test
```

## Use Cases

### Query Flow (e.g., Beacon API)
External request → transform → GraphQL query → transform → response

### Transaction Flow (e.g., DCAT Harvesting)
Fetch external data → transform → GraphQL mutation → store in MOLGENIS

*Transaction flow support coming in future versions.*

## Bundle Location

Place bundles in the `fair-mappings/` directory:

```
fair-mappings/
├── beacon-v2/
├── my-custom-api/
└── _shared/              # Optional: shared transforms
```

## Tips

- **Empty arrays**: JSLT normally removes empty arrays from output. FAIRmapper preserves them.
- **Paths**: All file paths in `mapping.yaml` are relative to the bundle directory.
- **Debugging**: Run `./gradlew test --info` for detailed output when tests fail.

## Resources

- [JSLT Tutorial](https://github.com/schibsted/jslt/blob/master/tutorial.md)
- [JSLT Functions Reference](https://github.com/schibsted/jslt/blob/master/functions.md)
- [JSLT GitHub Repository](https://github.com/schibsted/jslt)
