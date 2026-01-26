# Beacon v2 API

Expose MOLGENIS data via GA4GH Beacon v2 protocol.

## Quick Start

```bash
# Run from this directory (beacon-v2/)

# Validate bundle structure
../fairmapper validate .

# Run step tests (transform tests)
../fairmapper test . -v

# Run e2e tests against server
../fairmapper e2e . \
  --server http://localhost:8080 \
  --schema patientRegistry \
  -v
```

## Bundle Structure

```
beacon-v2/
├── fairmapper.yaml                    # Bundle config
├── src/
│   ├── transforms/                    # JSLT transforms
│   │   ├── request-to-variables.jslt  # Beacon request → GraphQL vars
│   │   ├── individuals-response.jslt  # GraphQL result → Beacon response
│   │   └── shared/*.jslt              # Reusable transform functions
│   └── queries/
│       ├── individuals.gql            # GraphQL query
│       └── individuals-minimal.gql
└── test/
    ├── request-to-variables/          # Transform tests
    ├── individuals-response/
    └── e2e/                           # End-to-end tests
```

## How It Works

1. **transform** - Convert Beacon request to GraphQL variables
2. **query** - Execute GraphQL against MOLGENIS
3. **transform** - Convert GraphQL response to Beacon format

## Endpoints

| Path | Description |
|------|-------------|
| `/{schema}/api/beacon/individuals` | Query individuals |
| `/{schema}/api/beacon/individuals-full` | Full individual details |
| `/{schema}/api/beacon/individuals-minimal` | Minimal response |

## Test Data

E2e tests expect `patientRegistry` schema with sample individuals.

## Adding New Entity Types

1. Create `src/queries/<entity>.gql`
2. Create `src/transforms/<entity>-response.jslt`
3. Add endpoint to `fairmapper.yaml`
4. Add tests in `test/`
