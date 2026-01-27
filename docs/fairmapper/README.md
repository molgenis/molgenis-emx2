# FAIRmapper

FAIRmapper enables data managers to create API adapters and ETL pipelines without writing Java code. Configure transformations declaratively using YAML and JSLT.

## What Can FAIRmapper Do?

- **Query adapters** - Expose MOLGENIS data via custom APIs (e.g., Beacon v2)
- **Data harvesting** - Fetch from external sources and import into MOLGENIS (e.g., DCAT/FDP)
- **Format conversion** - Transform between JSON formats using JSLT

## Quick Example

A FAIRmapper bundle harvests DCAT metadata from a FAIR Data Point:

```yaml
name: dcat-fdp
mappings:
  - name: harvest-catalog
    steps:
      - fetch: ${SOURCE_URL}
        frame: src/frames/catalog-with-datasets.jsonld
      - transform: src/transforms/to-molgenis.jslt
      - mutate: src/mutations/upsert-resources.gql
```

Run it:
```bash
./fairmapper run dcat-fdp harvest-catalog \
  --source https://fdp.example.org/catalog/123 \
  --server http://localhost:8080 \
  --schema catalogue \
  --token <your-token>
```

## Getting Started

New to FAIRmapper? Start with the [Getting Started guide](getting_started.md) for a step-by-step tutorial.

## Documentation

| Guide | Description |
|-------|-------------|
| [Getting Started](getting_started.md) | Create your first bundle |
| [Schema Reference](schema_reference.md) | Complete fairmapper.yaml specification |
| [Troubleshooting](troubleshooting.md) | Common errors and solutions |

## Key Concepts

### Bundles
A bundle is a directory containing `fairmapper.yaml` and your transform/query files. Bundles live in `fair-mappings/`.

### Mappings
Each mapping defines a pipeline with a name (for CLI) or endpoint path (for HTTP). A mapping has one or more steps.

### Steps
Steps execute in sequence. Each step type processes data differently:

| Step | Description |
|------|-------------|
| `fetch` | Fetch RDF data and apply JSON-LD frame |
| `transform` | Transform JSON using JSLT |
| `query` | Execute GraphQL query |
| `mutate` | Execute GraphQL mutation |

### Output Formats
Mappings can specify an `output` format for RDF serialization:

```yaml
mappings:
  - name: fdp-catalog
    endpoint: /{schema}/api/fdp/catalog/{id}
    output: turtle
    steps:
      - query: src/queries/get-catalog.gql
      - transform: src/transforms/to-dcat.jslt
```

Supported formats: `json`, `turtle`, `jsonld`, `ntriples`, `csv`

Clients can override via `Accept` header.

### JSLT
[JSLT](https://github.com/schibsted/jslt) is a JSON transformation language. See the [JSLT tutorial](https://github.com/schibsted/jslt/blob/master/tutorial.md).

## Resources

- [JSLT Tutorial](https://github.com/schibsted/jslt/blob/master/tutorial.md)
- [JSLT Functions](https://github.com/schibsted/jslt/blob/master/functions.md)
- [JSON-LD Framing](https://www.w3.org/TR/json-ld11-framing/)
