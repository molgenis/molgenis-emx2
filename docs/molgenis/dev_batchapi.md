# REST API

MOLGENIS EMX2 provides REST APIs for data import/export in multiple formats.

## Format-specific endpoints

Each format has its own base path with consistent endpoint structure:

| Format | Base path | Content-Type |
|--------|-----------|--------------|
| CSV | `/api/csv/` | `text/csv` |
| JSON | `/api/json/` | `application/json` |
| YAML | `/api/yaml/` | `text/yaml` |
| JSON-LD | `/api/jsonld/` | `application/ld+json` |
| Turtle | `/api/ttl/` | `text/turtle` |
| Excel | `/api/excel/` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| ZIP | `/api/zip/` | `application/zip` |

### Endpoint structure

All format-specific APIs follow the same pattern:

| Endpoint | GET | POST | PUT | DELETE | Description |
|----------|-----|------|-----|--------|-------------|
| `/_schema` | Export schema | Import schema | - | Delete tables/columns | Schema metadata |
| `/_data` | Export all data | Import all data | - | - | All table rows |
| `/_all` | Complete export | Complete import | - | - | Schema + data |
| `/_members` | List members | - | - | - | Schema permissions |
| `/_settings` | List settings | - | - | - | Schema settings |
| `/_changelog` | View changelog | - | - | - | Audit log |
| `/{table}` | Export table | Insert rows | Upsert rows | Delete rows | Table data |
| `/{table}/{id}` | Get row | - | Update row | Delete row | Single row (JSON/YAML/JSON-LD only) |

### Examples

```bash
# Export Pet table as CSV
curl https://example.org/pet%20store/api/csv/Pet

# Export Pet table as JSON
curl https://example.org/pet%20store/api/json/Pet

# Export Pet table as JSON-LD (with semantic context)
curl https://example.org/pet%20store/api/jsonld/Pet

# Import data via CSV
curl -X POST -H "x-molgenis-token: $TOKEN" \
  -d "name,category\npooky,cat" \
  https://example.org/pet%20store/api/csv/Pet

# Get single row by primary key
curl https://example.org/pet%20store/api/json/Pet/pooky

# Delete rows matching filter
curl -X DELETE "https://example.org/pet%20store/api/json/Pet?filter={\"status\":{\"equals\":\"sold\"}}"
```

## Content-negotiated endpoint

The `/api/data/` endpoint provides format selection via HTTP `Accept` header:

```bash
# Request CSV format
curl -H "Accept: text/csv" https://example.org/pet%20store/api/data/Pet

# Request JSON-LD format
curl -H "Accept: application/ld+json" https://example.org/pet%20store/api/data/Pet

# Request Turtle/RDF format
curl -H "Accept: text/turtle" https://example.org/pet%20store/api/data/Pet
```

### Supported Accept headers

| Accept Header | Format |
|---------------|--------|
| `text/csv` | CSV |
| `application/json` | JSON (default) |
| `application/ld+json` | JSON-LD |
| `text/turtle` | Turtle/RDF |
| `text/yaml` | YAML |
| `application/vnd.ms-excel` | Excel |
| `application/zip` | ZIP |

The `/api/data/` endpoint supports the same structure as format-specific endpoints (`/_schema`, `/_data`, `/_all`, `/{table}`, etc.).

## GraphQL-LD endpoint

For semantic queries with JSON-LD context, use the GraphQL-LD endpoint:

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"query": "{ Pet { name category { name } } }"}' \
  https://example.org/pet%20store/api/graphql-ld
```

Response includes `@context` for RDF compatibility:

```json
{
  "@context": {
    "my": "https://example.org/pet%20store#",
    "mg_id": "@id",
    ...
  },
  "data": {
    "Pet": [{"name": "pooky", "category": {"name": "cat"}}]
  }
}
```

## Query parameters

Table endpoints (`/{table}`) support filtering and pagination:

| Parameter | Description | Example |
|-----------|-------------|---------|
| `filter` | JSON filter object (GraphQL syntax) | `?filter={"status":{"equals":"available"}}` |
| `limit` | Maximum rows to return | `?limit=100` |
| `offset` | Skip first N rows | `?offset=50` |

```bash
# Get first 10 available pets
curl "https://example.org/pet%20store/api/json/Pet?filter={\"status\":{\"equals\":\"available\"}}&limit=10"
```

## System columns

Include system columns (audit fields) in downloads:

```bash
curl "https://example.org/pet%20store/api/csv/Pet?includeSystemColumns=true"
```

Returns additional columns: `mg_draft`, `mg_insertedBy`, `mg_insertedOn`, `mg_updatedBy`, `mg_updatedOn`.

## Authentication

For protected operations, include a token header:

```bash
curl -H "x-molgenis-token: YOUR_TOKEN" https://example.org/pet%20store/api/json/Pet
```

See [API tokens](use_tokens.md) for token management.
