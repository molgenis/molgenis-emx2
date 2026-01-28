# Spec: mg_id as Computed GraphQL Field

## Problem
RDF output needs meaningful subject IRIs based on primary keys, not opaque auto-generated IDs.

## Proposal
Make `mg_id` a computed field in GraphQL layer (not stored in DB).

## IRI Format

### Full IRI Pattern
```
{baseURL}/{schema}/{table}/{pkey}
```

### Examples (current - compatible with existing RDF)
```
# Single pkey
http://example.org/myschema/Pet/name=Spike

# Composite pkey (ampersand-separated, sorted alphabetically)
http://example.org/myschema/Pet/firstName=Donald&lastName=Duck

# Special chars (URL encoded)
http://example.org/myschema/Pet/name=Spike%20%26%20Friends
```

### mg_id Field Value (without base)
```
# Single pkey
Pet/name=Spike

# Composite pkey
Pet/firstName=Donald&lastName=Duck
```

### Encoding Rules (compatible with existing RDF API)
- Delimiter between key-value pairs: `&` (ampersand)
- Delimiter between key and value: `=`
- Keys sorted alphabetically for deterministic output
- Values URL-encoded (RFC 3986): spaces → `%20`, `&` → `%26`, `=` → `%3D`
- Always uses `key=value` format (including single pkey)

## Implementation Status

### Done
- [x] Move PrimaryKey to `molgenis-emx2` core module
- [x] Fix array ref bug in fromRow()
- [x] Add null validation in fromRow()
- [x] Add mg_id computed field to GraphQL schema

### Deferred (separate PR)
- [ ] Change delimiter from `&` to `,` (cleaner URLs)
- [ ] Single pkey: value only without `key=`
- [ ] Update JSON-LD @base pattern

## Benefits
1. Meaningful RDF subject IRIs
2. REST-like resource addressing
3. Client-side caching keys
4. Federation support (same entity → same IRI)
5. No storage overhead
6. Compatible with existing RDF API format

## Edge Cases
- Tables without pkey: mg_id is `null`
- Null in pkey column: throw exception
- Array pkey columns: not supported (throw exception)

## Code References
- `PrimaryKey.java`: `/backend/molgenis-emx2/src/main/java/org/molgenis/emx2/PrimaryKey.java`
- `GraphqlTableFieldFactory.java`: mg_id computed field
- `JsonLdSchemaGenerator.java`: JSON-LD context mapping
