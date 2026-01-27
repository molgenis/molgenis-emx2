# FAIRmapper Schema Reference

Complete reference for `fairmapper.yaml` configuration files.

## Top-Level Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | yes | Bundle identifier (used in logs and CLI) |
| `version` | string | no | Bundle version (recommended for tracking) |
| `mappings` | array | yes* | List of mapping pipelines (schema v2) |
| `endpoints` | array | deprecated | Use `mappings` instead |

*Either `mappings` or `endpoints` is required.

## Mappings

Each mapping defines a processing pipeline.

```yaml
mappings:
  - name: harvest-catalog
    steps:
      - fetch: ${SOURCE_URL}
        frame: src/frames/catalog.jsonld
      - transform: src/transforms/to-molgenis.jslt
      - mutate: src/mutations/upsert.gql
```

### Mapping Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | conditional | CLI identifier (required if no `endpoint`) |
| `endpoint` | string | conditional | HTTP path (required if no `name`) |
| `methods` | array | no | HTTP methods: `GET`, `POST` (default: `[GET]`) |
| `steps` | array | yes | Processing pipeline steps |
| `e2e` | object | no | End-to-end test configuration |

If both `name` and `endpoint` are provided, the mapping is accessible via both CLI and HTTP.

### Path Parameters

In `endpoint` paths, `{schema}` is replaced with the MOLGENIS database name:

```yaml
endpoint: /{schema}/api/beacon/individuals
# Becomes: /myDatabase/api/beacon/individuals
```

## Steps

Steps execute in sequence. The output of each step becomes the input to the next.

### Fetch Step

Fetches RDF data from a URL and applies a JSON-LD frame.

```yaml
- fetch: ${SOURCE_URL}
  frame: src/frames/catalog.jsonld
  maxDepth: 2
  maxCalls: 50
```

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `fetch` | string | yes | - | URL to fetch (supports `${VAR}` substitution) |
| `frame` | string | yes | - | Path to JSON-LD frame file |
| `maxDepth` | integer | no | 2 | Maximum depth for following links |
| `maxCalls` | integer | no | 50 | Maximum HTTP requests |

The fetch step:
1. Fetches RDF from the URL (supports Turtle, JSON-LD, RDF/XML)
2. Follows links in the RDF up to `maxDepth` levels
3. Applies the JSON-LD frame to structure the result
4. Outputs framed JSON-LD

### Transform Step

Transforms JSON using JSLT.

```yaml
- transform: src/transforms/to-molgenis.jslt
  tests:
    - input: test/input.json
      output: test/expected.json
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `transform` | string | yes | Path to JSLT file |
| `tests` | array | no | Unit test cases |

#### Test Cases

```yaml
tests:
  - input: test/case1-input.json
    output: test/case1-expected.json
  - input: test/edge-case.json
    output: test/edge-case-expected.json
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `input` | string | yes | Path to input JSON file |
| `output` | string | yes | Path to expected output JSON file |

### Query Step

Executes a GraphQL query against MOLGENIS.

```yaml
- query: src/queries/individuals.gql
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `query` | string | yes | Path to GraphQL query file |

The query receives variables from the previous step's output. Query results become the next step's input.

### Mutate Step

Executes a GraphQL mutation against MOLGENIS.

```yaml
- mutate: src/mutations/upsert-resources.gql
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `mutate` | string | yes | Path to GraphQL mutation file |

The mutation receives variables from the previous step's output. Typically used as the final step to write data.

### RDF Step

Serializes JSON-LD to RDF format. Used for publishing DCAT/FDP endpoints.

```yaml
- rdf: turtle
```

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `rdf` | string | yes | - | Output format: `turtle`, `jsonld`, or `ntriples` |
| `tests` | array | no | - | Unit test cases |

The input must be valid JSON-LD with `@context`. Output is RDF in the specified format.

```yaml
steps:
  - query: src/queries/get-catalog.gql
  - transform: src/transforms/to-dcat.jslt
  - rdf: turtle
```

## E2e Configuration

End-to-end tests run the full pipeline against a live MOLGENIS database.

```yaml
mappings:
  - name: get-individuals
    endpoint: /{schema}/api/beacon/individuals
    steps:
      - transform: src/request-to-variables.jslt
      - query: src/individuals.gql
      - transform: src/response.jslt
    e2e:
      schema: fairmapperTest
      tests:
        - method: POST
          input: test/e2e/request.json
          output: test/e2e/expected.json
```

### E2e Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `schema` | string | yes | MOLGENIS database name for testing |
| `tests` | array | yes | List of e2e test cases |

### E2e Test Case Fields

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `method` | string | no | GET | HTTP method |
| `input` | string | yes | - | Path to request body JSON |
| `output` | string | yes | - | Path to expected response JSON |

## Variable Substitution

Variables in step configurations are replaced at runtime:

| Variable | Source | Example |
|----------|--------|---------|
| `${SOURCE_URL}` | `--source` CLI argument | `https://fdp.example.org/catalog/123` |
| `${VAR_NAME}` | Environment variable | `${API_KEY}` |

```yaml
- fetch: ${SOURCE_URL}
```

## File Paths

All paths in `fairmapper.yaml` are relative to the bundle directory:

```
my-bundle/
├── fairmapper.yaml          # Bundle root
├── src/
│   ├── frames/
│   │   └── catalog.jsonld   # Referenced as: src/frames/catalog.jsonld
│   └── transforms/
│       └── to-molgenis.jslt # Referenced as: src/transforms/to-molgenis.jslt
```

## Complete Example

```yaml
name: dcat-fdp
version: 1.0.0

mappings:
  - name: harvest-catalog
    steps:
      - fetch: ${SOURCE_URL}
        frame: src/frames/catalog-with-datasets.jsonld
        maxDepth: 2
        maxCalls: 100
      - transform: src/transforms/to-molgenis.jslt
        tests:
          - input: test/transform/catalog-input.json
            output: test/transform/molgenis-output.json
      - mutate: src/mutations/upsert-resources.gql

  - name: list-catalogs
    endpoint: /{schema}/api/fdp/catalogs
    methods: [GET]
    steps:
      - query: src/queries/list-resources.gql
      - transform: src/transforms/to-dcat.jslt
    e2e:
      schema: catalogue
      tests:
        - method: GET
          input: test/e2e/empty-request.json
          output: test/e2e/catalogs-response.json
```

## JSON-LD Frames

Frames control how RDF is structured as JSON. Key concepts:

| Directive | Description |
|-----------|-------------|
| `@type` | Filter to resources of this type |
| `@embed` | `@always` embeds related resources inline |
| `{}` | Include this property in output |

Example frame for DCAT catalog:

```json
{
  "@context": {
    "dcat": "http://www.w3.org/ns/dcat#",
    "dcterms": "http://purl.org/dc/terms/"
  },
  "@type": "dcat:Catalog",
  "@embed": "@always",
  "dcterms:title": {},
  "dcat:dataset": {
    "@embed": "@always",
    "dcterms:title": {},
    "dcterms:description": {}
  }
}
```

See [JSON-LD Framing spec](https://www.w3.org/TR/json-ld11-framing/) for details.

## JSLT Tips

### Handling JSON-LD Keys

JSON-LD keys contain special characters. Use `get-key()`:

```jslt
get-key(., "@id")           // Not: .["@id"]
get-key(., "dcterms:title") // Not: ."dcterms:title"
```

### Defining Functions

```jslt
def extract-id(url)
  let parts = split($url, "/")
  $parts[size($parts) - 1]

{
  "id": extract-id(get-key(., "@id"))
}
```

### Handling Arrays vs Single Values

RDF may return arrays or single values. Normalize:

```jslt
def to-array(val)
  if (is-array($val)) $val
  else if ($val) [$val]
  else []

{
  "keywords": to-array(get-key(., "dcat:keyword"))
}
```

### Imports

Share transforms across files:

```jslt
import "shared/helpers.jslt" as helpers

{
  "id": helpers:extract-id(get-key(., "@id"))
}
```

Paths are relative to the importing file.
