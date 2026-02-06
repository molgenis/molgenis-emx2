# DCAT FDP via SQL (Proof of Concept)

Alternative approach: produce JSON-LD directly from PostgreSQL instead of GraphQL + JSLT.

## Comparison

| Approach | Steps | Languages |
|----------|-------|-----------|
| dcat-fdp | query.gql → transform.jslt | GraphQL + JSLT |
| dcat-fdp-sql | query.sql | SQL only |

## Testing

### Via JUnit test

```bash
./gradlew :backend:molgenis-emx2-fairmapper-cli:test --tests SqlQueryIntegrationTest
```

### Via MOLGENIS API (when implemented)

```bash
curl "https://catalogue.molgenis.org/catalogue/api/sql" \
  -H "Content-Type: application/json" \
  -d '{"query": "...", "parameters": {"base_url": "...", "schema": "...", "id": "..."}}'
```

### Via psql (for debugging)

```sql
\c molgenis
SET search_path TO catalogue;

-- Parameters use ${name} syntax, replace manually for psql:
-- ${base_url} -> 'https://catalogue.molgenis.org'
-- ${schema} -> 'catalogue'
-- ${id} -> 'umcg-lifelines'
```

## Table name assumptions

The SQL assumes MOLGENIS catalogue schema with:
- `"Resources"` - main table (id, name, description, website, publisher, mg_insertedOn, mg_updatedOn)
- `"Organisations"` - publisher reference (id, name)
- `"Datasets"` - child datasets (id, resource)

Adjust table/column names to match actual schema.

## Implementation needed

To support `.sql` files in FAIRmapper:

1. Add `SqlQueryStep` to model/step/
2. Use existing `schema.retrieveSql(sql, params)` method
3. Pass parameters: `${base_url}`, `${schema}`, `${id}` (from URL path)
4. Return JSON result from query

## Security considerations

**Safe:** SQL comes from trusted bundle files, only parameters are user input.
Parameters are bound via `SqlRawQueryForSchema` which prevents injection.

**Concern:** If SQL API is exposed publicly, users could craft malicious queries.
Options:
- Only enable in dev mode
- Only allow from internal FAIRmapper pipelines
- Require admin role
- Whitelist specific queries from bundles

## Benefits over GraphQL + JSLT

- Single step instead of two
- SQL is more widely known
- Direct JSON construction (no intermediate format)
- Can use database timestamps (mg_insertedOn, mg_updatedOn)
