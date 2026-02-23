# Streaming RDF Export

## Status: Future Enhancement (out of scope PR #5495)

## Problem
Current `getAllAsTurtle`/`getAllAsJsonLd` loads entire query result into memory before conversion. For large tables this could cause memory issues.

## Current Implementation Analysis

### SqlQuery.java
- `retrieveRows()` uses jOOQ `query.fetch()` → all rows in memory
- `retrieveJSON()` uses PostgreSQL `jsonb_agg` → single JSON string in memory

### Streaming options in jOOQ
```java
// Instead of:
Result<Record> fetch = query.fetch();

// Use cursor-based streaming:
try (Cursor<Record> cursor = query.fetchLazy()) {
    while (cursor.hasNext()) {
        Record record = cursor.fetchNext();
        // process one at a time
    }
}
// Or Java Stream:
query.fetchStream().forEach(record -> ...);
```

## GraphQL @stream Directive

GraphQL spec has `@stream` directive (Stage 2, experimental):
- Applies to list fields only
- Returns items incrementally via HTTP chunked transfer
- Facebook uses since 2017
- graphql-java discussion: https://github.com/graphql-java/graphql-java/discussions/3550

```graphql
query {
  Pet @stream(initialCount: 10) {
    name
    category { name }
  }
}
```

### graphql-java support
- Not yet in core, but can be implemented via custom execution strategy
- Would need Javalin to support chunked response

## Proposed Solution

### Option A: Dedicated streaming endpoints (simpler)
```
GET /{schema}/api/ttl/{table}?limit=&offset=
GET /{schema}/api/jsonld/{table}?limit=&offset=
```

No GraphQL involved, direct table streaming.

### Option B: GraphQL @stream support (more complex)
Requires:
1. Custom graphql-java execution strategy
2. Javalin chunked response support
3. Client support for incremental delivery

### Recommended: Option A first

#### Components needed:

**1. SqlQuery streaming method**
```java
public Stream<Map<String, Object>> retrieveJsonAsStream() {
    // Returns nested JSON objects (not flat rows)
    // Each Map includes resolved refs as nested Maps
    // Cannot use PostgreSQL jsonb_agg (aggregates all)
    // Must iterate main table, resolve refs per row
}
```

Note: Returns `Map<String, Object>` not `Row` because:
- Refs are nested objects, not flat foreign keys
- Output matches JSON-LD structure directly
- Can't flow through GraphQL (expects complete `data` wrapper)

**2. StreamingJsonLdWriter**
```java
public class StreamingJsonLdWriter {
    public void write(OutputStream out, TableMetadata table,
                      Stream<Map<String, Object>> items, String schemaUrl) {
        JsonGenerator gen = mapper.getFactory().createGenerator(out);
        gen.writeStartObject();
        writeContext(gen, table, schemaUrl);  // @context first
        gen.writeFieldName("data");
        gen.writeStartObject();
        gen.writeFieldName(table.getIdentifier());
        gen.writeStartArray();
        items.forEach(item -> mapper.writeValue(gen, item));
        gen.writeEndArray();
        gen.writeEndObject();
        gen.writeEndObject();
        gen.close();
    }
}
```

**3. StreamingTurtleWriter (direct triples from nested Maps)**
```java
public class StreamingTurtleWriter {
    public void write(OutputStream out, TableMetadata table,
                      Stream<Map<String, Object>> items, String schemaUrl) {
        RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, out);
        writer.startRDF();
        writePrefixes(writer, table);
        items.forEach(item -> writeItemAsTriples(writer, item, table));
        writer.endRDF();
    }

    private void writeItemAsTriples(RDFWriter writer, Map<String,Object> item, TableMetadata table) {
        IRI subject = createIRI(schemaUrl, table, item);
        writer.handleStatement(vf.createStatement(subject, RDF.TYPE, tableTypeIRI));
        for (Column col : table.getColumns()) {
            Object value = item.get(col.getIdentifier());
            if (value instanceof Map) {
                // nested ref - emit as IRI
            } else if (value instanceof List) {
                // array - emit multiple triples
            } else {
                // literal value
            }
        }
    }
}
```

**4. REST endpoints**
```java
app.get("/{schema}/api/ttl/{table}", ctx -> {
    Table table = getTable(ctx);
    Stream<Map<String,Object>> items = table.query().select(...).retrieveJsonAsStream();
    ctx.contentType("text/turtle");
    new StreamingTurtleWriter().write(ctx.outputStream(), table.getMetadata(), items, baseUrl);
});
```

## Constraints
- Streaming only works per-table (single entity type)
- Bypasses GraphQL layer (GraphQL expects complete `{data: {...}}` response)
- Filter/sort params could reuse GraphQL filter parsing logic
- Returns `Stream<Map>` with nested refs already resolved (not flat rows)

## Open Questions
- Pagination: client-side (limit/offset) vs server-side cursor?
- Should streaming endpoints support nested refs? (complex)
- Memory limit for non-streaming endpoints?
- Priority: JSON-LD streaming vs Turtle streaming?

## References
- [GraphQL @defer/@stream](https://graphql.org/blog/2020-12-08-defer-stream/)
- [graphql-java streaming discussion](https://github.com/graphql-java/graphql-java/discussions/3550)
- [jOOQ fetchStream](https://www.jooq.org/doc/latest/manual/sql-execution/fetching/lazy-fetching/)
- [RDF4J streaming](https://rdf4j.org/documentation/programming/rio/)
