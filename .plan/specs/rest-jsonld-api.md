# Semantic API Specification (GraphQL-LD + REST-LD)

## Overview

Two complementary APIs sharing a semantic JSON-LD layer:

- **GraphQL-LD**: GraphQL queries/mutations with JSON-LD context
- **REST-LD**: REST verbs with content negotiation, built on top of GraphQL

Both accept plain JSON or JSON-LD input, and output JSON-LD (convertible to TTL/RDF).

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      Clients                            │
└─────────────────────────────────────────────────────────┘
                    │                    │
                    ▼                    ▼
┌──────────────────────────┐  ┌──────────────────────────┐
│        REST-LD API       │  │      GraphQL-LD API      │
│  (JsonldApi.java)        │  │  (GraphqlApi.java)       │
│  - Content negotiation   │  │  - Query/mutation        │
│  - REST verbs            │  │  - Fragments             │
└──────────────────────────┘  └──────────────────────────┘
                    │                    │
                    └────────┬───────────┘
                             ▼
┌─────────────────────────────────────────────────────────┐
│              Semantic Layer (shared)                    │
│  - JsonLdSchemaGenerator: @context from schema          │
│  - stripJsonLdKeywords(): remove @ fields on input      │
│  - @embed/@type: framing info always included           │
└─────────────────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────┐
│                   GraphQL Engine                        │
│  - TypeUtils.convertToRows(): handle nested refs        │
│  - table.save(): persist to database                    │
└─────────────────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────┐
│                      Database                           │
└─────────────────────────────────────────────────────────┘
```

### Data Flow

**REST-LD Import:**
```
REST POST → parse JSON-LD → stripJsonLdKeywords() → TypeUtils.convertToRows() → table.save()
```

**REST-LD Export:**
```
REST GET → GraphQL query → wrap with @context → content negotiation → response
```

**GraphQL-LD:**
```
GraphQL query/mutation → TypeUtils.convertToRows() → table.save()
```

### Why Direct Path (not via GraphQL mutation)?

All import paths share `TypeUtils.convertToRows() → table.save()`:
- CSV import: direct
- Excel import: direct
- GraphQL mutation: direct
- REST-LD import: direct

GraphQL layer adds no value for import:
- No extra security (permissions at table level)
- No extra hooks (triggered by table.save())
- No extra transactions (same SqlTable.tx())
- Only adds parsing overhead (+20-40ms)

GraphQL is for **clients wanting GraphQL syntax**, not an internal abstraction.

## Design Decisions

### Why JSON-LD Lite?

| Alternative | Why Not |
|-------------|---------|
| **Full LDP** | Too complex (containment triples, Link headers, ETags) |
| **JSON:API** | Rigid wrapper structure, no semantics |
| **OData** | Proprietary query syntax, no RDF |
| **Full Hydra** | Complex operation vocabulary |

### What We Support

| Feature | Status | Notes |
|---------|--------|-------|
| JSON input/output | ✅ | Plain JSON without `@` fields |
| JSON-LD input/output | ✅ | With `@context`, `@type`, `@id` |
| TTL/Turtle output | ✅ | Via content negotiation |
| TTL/Turtle input | 🔜 | Parse RDF → JSON-LD → save |
| Content negotiation | ✅ | Accept/Content-Type headers |
| GraphQL queries | ✅ | For subsetting/filtering |
| Framing (`@embed`) | ✅ | Always included, ignored if unused |

### What We Skip (Intentionally)

| Feature | Reason |
|---------|--------|
| `ldp:contains` triples | Table structure implies containment |
| `Link` type headers | `@type` in body sufficient |
| `ETag` / `If-Match` | Database transactions handle concurrency |
| `Slug` header | Use `mg_id` from primary keys |
| `hydra:operation` | GraphQL introspection for API discovery |
| Strict JSON-LD validation | Accept plain JSON too |

## API Contract

### Format-Specific Endpoints

Three parallel APIs with identical structure, differing only in serialization:

```
# JSON API (plain JSON, no @context)
/{schema}/api/json/{table}              GET, POST, PUT, DELETE
/{schema}/api/json/{table}/{id}         GET, PUT, DELETE

# JSON-LD API (JSON with @context, semantic URIs)
/{schema}/api/jsonld/{table}            GET, POST, PUT, DELETE
/{schema}/api/jsonld/{table}/{id}       GET, PUT, DELETE

# Turtle API (RDF Turtle format)
/{schema}/api/ttl/{table}               GET, POST, PUT, DELETE
/{schema}/api/ttl/{table}/{id}          GET, PUT, DELETE
```

### System Endpoints

Use `_` prefix to distinguish from user tables:

```
/{schema}/api/json/_schema              # Schema metadata (table/column definitions)
/{schema}/api/json/_data                # All table rows (no metadata)
/{schema}/api/json/_all                 # Complete export (schema + data)
/{schema}/api/json/_members             # Schema members/permissions
/{schema}/api/json/_settings            # Schema settings
/{schema}/api/json/_changelog           # Schema audit log
```

Same pattern for `/jsonld/` and `/ttl/`.

**Why `_` prefix is safe:** Table names must start with a letter (`[a-zA-Z]` per `TABLE_NAME_REGEX`), so `_` prefixed endpoints can never conflict with user tables.

**TODO:** Fix existing CSV API to use same pattern (`/_members`, `/_settings`, `/_changelog` instead of `/members`, `/settings`, `/changelog`).

### Content-Negotiated Endpoint (Canonical REST)

```
/{schema}/api/data/_schema              # Schema metadata (table/column definitions)
/{schema}/api/data/_data                # All table rows (no metadata)
/{schema}/api/data/_all                 # Complete export (schema + data)
/{schema}/api/data/{table}              # Table data
/{schema}/api/data/{table}/{id}         # Row data
```

Format determined by `Accept` header:

| Accept Header | Format |
|---------------|--------|
| `application/json` | Plain JSON (default) |
| `application/ld+json` | JSON-LD |
| `text/turtle` | Turtle/TTL |
| `text/csv` | CSV |

**Why both format-specific and content-negotiated?**
- Format-specific (`/api/json/`) = discoverable, browser-friendly, documentation
- Content-negotiated (`/api/data/`) = canonical URL, RESTful, Linked Data compatible

### GraphQL-LD Endpoint

```
/{schema}/api/graphql-ld                # GraphQL with @context in response
```

- Accepts standard GraphQL queries/mutations
- Response wrapped with JSON-LD @context
- Enables semantic subsetting via GraphQL

### Deprecated Endpoints

```
/{schema}/api/rdf                       # OLD: will be replaced
/{schema}/api/ttl2                      # TEMP: rename to /api/ttl
```

### Content Types

| Media Type | Format | Direction |
|------------|--------|-----------|
| `application/json` | Plain JSON | In/Out |
| `application/ld+json` | JSON-LD | In/Out |
| `text/turtle` | Turtle/TTL | In/Out |
| `application/n-triples` | N-Triples | Out |
| `application/rdf+xml` | RDF/XML | Out |

### Request/Response Format

**Input (any of these accepted):**
```json
// Plain JSON
{"name": "pooky", "category": "cat"}

// JSON-LD
{"@type": "my:Pet", "name": "pooky", "category": {"name": "cat"}}

// Full JSON-LD with context
{
  "@context": {"@vocab": "http://example.org/"},
  "@type": "Pet",
  "name": "pooky"
}
```

**Output (JSON-LD with context):**
```json
{
  "@context": {
    "my": "http://localhost/schema#",
    "mg_id": "@id",
    "data": "@graph",
    "@embed": "@always"
  },
  "data": {
    "Pet": [
      {"mg_id": "pooky", "name": "pooky", "category": {"name": "cat"}}
    ]
  }
}
```

### Query Parameters

| Parameter | Purpose                                | Example |
|-----------|----------------------------------------|---------|
| `query` | GraphQL query for subsetting           | `?query={Pet{name}}` |
| `filter` | JSON filter object conform GraphQL api | `?filter={"status":"available"}` |

### Status Codes

| Code | Meaning |
|------|---------|
| 200 | Success (GET, PUT, DELETE) |
| 201 | Created (POST) |
| 400 | Parse/validation error |
| 404 | Schema/table/row not found |
| 409 | Conflict (FK violation, unique constraint) |
| 415 | Unsupported media type |

### Error Response

```json
{
  "error": "Description of what went wrong",
  "details": "Optional additional context"
}
```

## Processing Pipeline

### Import (any format → database)

```
Input
  ↓
Content-Type detection
  ↓
If RDF (TTL/N3/XML): parse → JSON-LD
  ↓
stripJsonLdKeywords() — remove @type, @context, @id, etc.
  ↓
Extract table data from "data" or "@graph" key
  ↓
TypeUtils.convertToRows() — shared with CSV/Excel/GraphQL imports
  ↓
table.save(rows) — permissions, hooks, transactions all here
```

**Design notes:**
- No custom reference flattening needed — `TypeUtils.convertToRows()` handles nested refs
- Same path as CSV/Excel import — consistent behavior
- Direct to table.save(), not via GraphQL mutation — no overhead, same security

### Export (database → any format)

```
GraphQL query (with fragments for all fields)
  ↓
JSON result
  ↓
Wrap with @context (always includes @embed for framing)
  ↓
If Accept: text/turtle → convert JSON-LD to TTL
  ↓
Output
```

## Semantic Mapping

The `@context` provides bidirectional mapping:

| JSON Field | RDF Predicate | Notes |
|------------|---------------|-------|
| `mg_id` | `@id` | Row URI = `{base}/{table}/{mg_id}` |
| `{column}` | `my:{column}` or semantic URI | From column semantics |
| `{table}` | `my:{table}` or semantic URI | From table semantics |
| `@type` | `rdf:type` | From table semantics |

## Future Considerations

1. **RDF input parsing** — accept TTL/N-Triples, convert to JSON-LD before processing
2. **Batch operations** — import multiple tables in dependency order
3. **PATCH support** — partial updates via JSON-LD
4. **Streaming** — large dataset export/import
5. **Validation** — optional strict JSON-LD validation mode
