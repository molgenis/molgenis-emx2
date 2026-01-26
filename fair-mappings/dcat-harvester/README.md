# DCAT-FDP Harvester

Harvest DCAT metadata from FAIR Data Points and import into MOLGENIS catalogue.

## Quick Start

```bash
# From project root (etl_pilot/)

# Validate bundle structure
../fairmapper validate .

# Run step tests (uses local .ttl fixtures)
../fairmapper test . -v

# Fetch from real FDP
../fairmapper fetch-rdf \
  https://fdp.health-ri.nl/catalog/5d6f222e-ba32-4930-9e77-d266f5ee64d9 \
  --frame src/frames/catalog-with-datasets.jsonld
```

## Bundle Structure

```
dcat-fdp/
├── fairmapper.yaml                    # Bundle config
├── src/frames/
│   └── catalog-with-datasets.jsonld   # JSON-LD frame for shaping output
└── test/fetch/
    ├── catalog.ttl                    # Sample input (Turtle RDF)
    └── catalog.json                   # Expected output (framed JSON)
```

## How It Works

1. **fetch** - HTTP GET RDF from source URL
2. **frame** - Apply JSON-LD frame to shape the output
3. Link following - Automatically fetches linked resources (datasets, publishers)

## Configuration

```yaml
mappings:
  - name: harvest-catalog
    steps:
      - fetch: ${SOURCE_URL}           # Variable from CLI --SOURCE_URL=...
        frame: src/frames/...          # Path relative to bundle root
        maxDepth: 5                     # Levels to follow links (default: 5)
        maxCalls: 50                    # Max HTTP requests (default: 50)
```

## Fetch Limits

| Scenario | Typical calls |
|----------|---------------|
| 1 catalog | 1 |
| + 10 datasets (depth=1) | 11 |
| + publishers per dataset (depth=2) | ~20 |

## Status

- [x] Fetch step with JSON-LD framing
- [ ] Transform step (DCAT → MOLGENIS Resources)
- [ ] Mutate step (GraphQL import)
