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

  - name: publish-catalog
    endpoint: /{schema}/api/fdp/catalog/{id}
    output: turtle
    steps:
      - query: src/queries/get-catalog.gql
      - transform: src/transforms/to-dcat.jslt
```

### Mapping Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | conditional | CLI identifier (required if no `endpoint`) |
| `endpoint` | string | conditional | HTTP path (required if no `name`) |
| `methods` | array | no | HTTP methods: `GET`, `POST` (default: `[GET]`) |
| `input` | string | no | Default input format: `json` (default), `turtle`, `jsonld`, `ntriples`, `csv` |
| `output` | string | no | Default output format: `json` (default), `turtle`, `jsonld`, `ntriples`, `csv` |
| `frame` | string | no | JSON-LD frame file for RDF serialization (used when output is RDF) |
| `steps` | array | yes | Processing pipeline steps |
| `e2e` | object | no | End-to-end test configuration |

If both `name` and `endpoint` are provided, the mapping is accessible via both CLI and HTTP.

#### Content Negotiation

When a mapping has an `endpoint`, clients can override the default `output` format using the `Accept` header:

| Accept Header | Output Format |
|---------------|---------------|
| `application/json` | JSON |
| `text/turtle` | Turtle |
| `application/ld+json` | JSON-LD |
| `application/n-triples` | N-Triples |
| `text/csv` | CSV |

Example:
```bash
curl -H "Accept: text/turtle" http://localhost:8080/mydb/api/fdp/catalog/123
```

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

### SQL Step

Executes a SQL query against PostgreSQL, returning JSON directly.

```yaml
- sql: src/queries/get-catalog.sql
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `sql` | string | yes | Path to SQL file |

The SQL query must return a `result` column containing JSON:

```sql
SELECT json_build_object(
  '@context', json_build_object('dcat', 'http://www.w3.org/ns/dcat#'),
  '@id', 'https://example.org/catalog/' || id,
  '@type', 'dcat:Catalog',
  'dct:title', name
) AS result
FROM "Resources"
WHERE id = ${id}
```

Variables from the previous step are available as `${name}` parameters.

**Benefits over GraphQL + JSLT:**
- Single step instead of two
- Direct JSON-LD construction
- Access to database timestamps (`mg_insertedOn`, `mg_updatedOn`)

### Frame Step

Applies a JSON-LD frame to reshape RDF/JSON-LD data.

```yaml
- frame: src/frames/output.jsonld
  unmapped: true
```

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `frame` | string | yes | - | Path to JSON-LD frame file |
| `unmapped` | boolean | no | false | Keep unmapped properties (sets `@explicit: false`) |

The frame step is useful for:
- Restructuring harvested RDF into a specific shape
- Filtering to specific types
- Two-frame pattern: first with `unmapped: true` to capture all, then strict frame to filter

```yaml
steps:
  - frame: src/frames/resources.jsonld
    unmapped: true
  - transform: src/transforms/fix-exceptions.jslt
  - frame: src/frames/resources.jsonld
```

### RDF Step (Deprecated)

**DEPRECATED in 7.6.4+**: Use the `output` field at mapping level instead.

The `rdf:` step has been removed. RDF serialization is now handled via content negotiation at the mapping level. See [Migration Guide](MIGRATION.md) for upgrade instructions.

Old approach (7.6.3 and earlier):
```yaml
mappings:
  - name: fdp-catalog
    endpoint: /{schema}/api/fdp/catalog/{id}
    steps:
      - query: src/queries/get-catalog.gql
      - transform: src/transforms/to-dcat.jslt
      - rdf: turtle
```

New approach (7.6.4+):
```yaml
mappings:
  - name: fdp-catalog
    endpoint: /{schema}/api/fdp/catalog/{id}
    output: turtle
    steps:
      - query: src/queries/get-catalog.gql
      - transform: src/transforms/to-dcat.jslt
```

The pipeline output must still be valid JSON-LD with `@context`. The `output` field controls serialization format.

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
    output: turtle
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

## HTTP API

FAIRmapper exposes HTTP endpoints for each bundle mapping. The URL pattern is:

```
/{schema}/api/fair/{bundleName}/{route}
```

### Examples

**Get DCAT catalog in Turtle:**
```bash
curl -H "Accept: text/turtle" \
  http://localhost:8080/catalogue/api/fair/dcat-via-mapping/dcat
```

**Get DCAT catalog in JSON-LD:**
```bash
curl -H "Accept: application/ld+json" \
  http://localhost:8080/catalogue/api/fair/dcat-via-mapping/dcat
```

**Get default format (uses mapping's output setting):**
```bash
curl http://localhost:8080/catalogue/api/fair/dcat-via-mapping/dcat
```

**Get with SHACL validation:**
```bash
curl -H "Accept: text/turtle" \
  "http://localhost:8080/catalogue/api/fair/dcat-fdp-sql/catalog/123?validate=dcat-ap"
```

**POST request with JSON body:**
```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"SOURCE_URL": "https://fdp.example.org/catalog/abc"}' \
  http://localhost:8080/catalogue/api/fair/dcat-harvester/harvest
```

**POST with path parameters and query variables:**
```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"filters": {"age": ">18"}}' \
  http://localhost:8080/mydb/api/fair/beacon/individuals
```

**Verbose output with response headers:**
```bash
curl -v -H "Accept: text/turtle" \
  http://localhost:8080/catalogue/api/fair/dcat-via-mapping/dcat
```

### Response Formats

Use the `Accept` header to request different output formats:

| Accept Header | Format |
|---------------|--------|
| `text/turtle` | Turtle |
| `application/ld+json` | JSON-LD |
| `application/n-triples` | N-Triples |
| `application/json` | JSON |

If no `Accept` header is provided, the mapping's default `output` format is used.

### Common HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 400 | Bad request (invalid input) |
| 404 | Mapping not found |
| 500 | Server error (check logs) |

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
