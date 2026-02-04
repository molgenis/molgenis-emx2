# Getting Started with FAIRmapper

This guide walks you through creating your first FAIRmapper bundle to harvest data from a FAIR Data Point (FDP) into MOLGENIS.

## Prerequisites

- Java 21 or later
- Git clone of the molgenis-emx2 repository
- A running MOLGENIS server (for the final import step)

## Running FAIRmapper

From the `fair-mappings/` directory:

```bash
./fairmapper --help
```

This auto-builds on first run. All examples in this guide assume you're in the `fair-mappings/` directory.

## Step 1: Create Your Bundle Directory

```bash
mkdir my-fdp-harvest
cd my-fdp-harvest
```

## Step 2: Create fairmapper.yaml

Create `fairmapper.yaml` with this minimal configuration:

```yaml
name: my-fdp-harvest
version: 1.0.0

mappings:
  - name: harvest
    steps:
      - fetch: ${SOURCE_URL}
        frame: frame.jsonld
      - transform: transform.jslt
```

The `${SOURCE_URL}` is replaced at runtime with the `--source` argument.

## Step 3: Create the JSON-LD Frame

A frame tells FAIRmapper how to structure the RDF data it fetches. Create `frame.jsonld`:

```json
{
  "@context": {
    "dcat": "http://www.w3.org/ns/dcat#",
    "dcterms": "http://purl.org/dc/terms/",
    "foaf": "http://xmlns.com/foaf/0.1/"
  },
  "@type": "dcat:Catalog",
  "@embed": "@always",
  "dcterms:title": {},
  "dcterms:description": {},
  "dcterms:publisher": {
    "@embed": "@always",
    "foaf:name": {}
  },
  "dcat:dataset": {
    "@embed": "@always",
    "dcterms:title": {},
    "dcterms:description": {}
  }
}
```

The frame:
- Filters to only `dcat:Catalog` resources
- Embeds related resources (publisher, datasets) inline
- Selects which properties to include

## Step 4: Create the JSLT Transform

Create `transform.jslt` to convert JSON-LD to MOLGENIS format:

```jslt
def extract-id(url)
  let parts = split($url, "/")
  $parts[size($parts) - 1]

{
  "Resources": [
    {
      "id": extract-id(get-key(., "@id")),
      "name": get-key(., "dcterms:title"),
      "description": get-key(., "dcterms:description")
    }
  ]
}
```

Important: Use `get-key(., "@id")` for JSON-LD keys containing `@` or `:`. Regular dot notation fails for these.

## Step 5: Validate Your Bundle

```bash
cd ..
./fairmapper validate my-fdp-harvest
```

Expected output:
```
✓ Bundle 'my-fdp-harvest' is valid
  1 mapping(s), 2 step(s)
```

## Step 6: Test the Fetch Step

Fetch from a real FDP and see the framed output:

```bash
./fairmapper fetch-rdf \
  https://fdp.health-ri.nl/catalog/5d6f222e-ba32-4930-9e77-d266f5ee64d9 \
  --frame my-fdp-harvest/frame.jsonld
```

This shows the JSON-LD after framing. Use this to understand the structure your transform receives.

## Step 7: Add Tests

Create test fixtures to verify your transform. Create `test/` directory:

```bash
mkdir -p my-fdp-harvest/test
```

Save the fetch output as `test/input.json`, then create `test/expected.json` with your expected MOLGENIS format.

Update `fairmapper.yaml`:

```yaml
name: my-fdp-harvest
version: 1.0.0

mappings:
  - name: harvest
    steps:
      - fetch: ${SOURCE_URL}
        frame: frame.jsonld
      - transform: transform.jslt
        tests:
          - input: test/input.json
            output: test/expected.json
```

Run tests:

```bash
./fairmapper test my-fdp-harvest -v
```

## Step 8: Dry Run

Test the full pipeline without writing to MOLGENIS:

```bash
./fairmapper run my-fdp-harvest harvest \
  --source https://fdp.health-ri.nl/catalog/5d6f222e-ba32-4930-9e77-d266f5ee64d9 \
  --dry-run -v
```

The `-v` flag shows intermediate results after each step.

## Step 9: Add Mutation Step

To write data to MOLGENIS, add a mutation step. Create `mutation.gql`:

```graphql
mutation($Resources: [ResourcesInput]) {
  insert(Resources: $Resources) {
    message
  }
}
```

Update `fairmapper.yaml`:

```yaml
mappings:
  - name: harvest
    steps:
      - fetch: ${SOURCE_URL}
        frame: frame.jsonld
      - transform: transform.jslt
      - mutate: mutation.gql
```

## Step 10: Run Against MOLGENIS

```bash
./fairmapper run my-fdp-harvest harvest \
  --source https://fdp.health-ri.nl/catalog/5d6f222e-ba32-4930-9e77-d266f5ee64d9 \
  --server http://localhost:8080 \
  --schema catalogue \
  --token <your-molgenis-token>
```

## Complete Bundle Structure

```
my-fdp-harvest/
├── fairmapper.yaml
├── frame.jsonld
├── transform.jslt
├── mutation.gql
└── test/
    ├── input.json
    └── expected.json
```

## Step 11: Add E2e Tests

End-to-end tests run the full pipeline against a live MOLGENIS server. Add to your mapping:

```yaml
mappings:
  - name: harvest
    steps:
      - fetch: ${SOURCE_URL}
        frame: frame.jsonld
      - transform: transform.jslt
      - mutate: mutation.gql
    e2e:
      schema: my-test-schema
      tests:
        - method: POST
          input: test/e2e-input.json
          output: test/e2e-expected.json
```

Run e2e tests:

```bash
./fairmapper e2e my-fdp-harvest \
  --server http://localhost:8080 \
  --token <your-token> \
  -v
```

Options:
- `--schema` overrides the schema in e2e config
- `-v` shows detailed output for failures

## Next Steps

- See [Schema Reference](schema_reference.md) for all configuration options
- See [Troubleshooting](troubleshooting.md) for common issues
- Study the `dcat-via-mapping` bundle in `fair-mappings/` for an example

## Tips

### Debugging Transforms

Use `--show-data` with `run` to see JSON after each step:

```bash
./fairmapper run my-fdp-harvest harvest --source URL --dry-run --show-data
```

### JSLT for JSON-LD

JSON-LD keys often contain special characters. Always use:
- `get-key(., "@id")` instead of `.["@id"]`
- `get-key(., "dcterms:title")` instead of `."dcterms:title"`

### Testing Without Network

Save fetched data to a file, then reference it locally in tests:
```yaml
- fetch: test/cached-catalog.json
  frame: frame.jsonld
```
