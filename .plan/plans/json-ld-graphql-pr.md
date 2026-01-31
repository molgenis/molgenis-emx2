# JSON-LD/TTL over GraphQL PR Plan

## PR Summary
**PR #5495**: spike: json-ld and ttl over graphql to allow subselections

### Intent
Enable RDF subsetting through GraphQL by:
1. Generating JSON-LD `@context` schema from EMX2 schema metadata
2. Executing GraphQL queries to get data subsets
3. Converting the combined context+data to TTL/JSON-LD format

### Current State
- Rebased onto master (fe4bf5c7e) - merge conflicts resolved
- 5 commits: first json-ld, graphql expansion, rest-like table function, catalogue test, fix test
- API endpoint: `/{schema}/api/ttl2/`

## Architecture

### New Files
1. `JsonLdSchemaGenerator.java` - generates JSON-LD @context from SchemaMetadata
2. `RestOverGraphql.java` - converts GraphQL query results to TTL/JSON-LD
3. `JsonLdValidator.java` - validates JSON-LD prefix usage
4. `JsonldApi.java` (webapi) - Javalin routes for ttl2 API

### Modified Files
1. `GraphqlApiFactory.java` → `GraphqlApi.java` - renamed + added:
   - `queryAsMap()`, `queryAsString()` - execute queries
   - `getSelectAllQuery()` - generates fragment-based query for all tables
   - `getJsonLdSchema()` - returns JSON-LD context
   - Fragment expansion via `...AllXxxFields` syntax
2. `GraphqlTableFieldFactory.java` - added `getGraphqlFragments()` method

## Reviewer Feedback (from PR reviews)

### Code Quality (jhhaanstra)
- [x] Factory methods return GraphQL to make field final - FIXED: `graphql` field now final
- [ ] schema/database on GraphqlApi seems misplaced - only used in one method
- [x] Don't expose testing-only methods - FIXED: execute(query) now package-private
- [x] Unused parameter in recursive scanning - FIXED: removed inheritedContextPath from scanNode()
- [x] Throw exceptions instead of stderr logging - FIXED: MolgenisException in JsonLdValidator
- [ ] Leverage Jackson more for JSON node manipulation

### Semantic Issues (svandenhoek) - Critical
- [x] Missing `my` prefix definition - FIXED: now `my: <schemaUrl#>`
- [x] Missing `rdf:type` triples - FIXED: table semantics now generate @type
- [x] Using `mg_id` instead of composite keys - FIXED: mg_id now computed from pkeys via PrimaryKey class
- [x] Invalid IRI syntax (`<my:.>`) - FIXED: removed invalid root ID, @base now full URL
- [x] Semantic fields missing entirely - FIXED: ref columns now have @type:@id in context
- [x] Non-deterministic blank nodes - INVESTIGATED: only affects structural wrapper nodes, real data has proper IRIs via mg_id
- [x] Incorrect object handling - FIXED: ref columns + ontologyTermURI now output as IRIs via @type:@id
- [x] Data mapping errors - INVESTIGATED: no issue found, getStoredColumns() correctly returns table-specific columns

## Implementation Plan

### Phase 1: Fix Critical Semantic Issues
1. **Fix IRI generation**
   - Replace `my:.` with proper base IRI
   - Generate proper subject IRIs from composite keys
   - Handle blank nodes deterministically

2. **Add rdf:type triples**
   - Use table semantics for `@type`
   - Handle inheritance correctly

3. **Fix prefix handling**
   - Define `my` prefix properly in context
   - Validate all prefixes before conversion

### Phase 2: Fix Data Mapping
1. **Handle references correctly**
   - ontologyTermURI should be `@type: @id`
   - Reference arrays should link correctly

2. **Fix inheritance handling**
   - Don't mix columns from different table types
   - Respect table hierarchy in GraphQL fragments

### Phase 3: Code Quality
1. **GraphqlApi refactoring**
   - Make `g` field final
   - Move JSON-LD generation to separate service
   - Clean up testing-only methods

2. **Error handling**
   - Throw exceptions for validation errors
   - Better error messages with context

3. **General cleanup**
   - Remove unused parameters
   - Use Jackson consistently

### Phase 4: Testing
1. **Unit tests**
   - Test JSON-LD context generation
   - Test TTL conversion
   - Test fragment expansion

2. **Integration tests**
   - Test with PET_STORE model
   - Test with TYPE_TEST model
   - Test with semantic-rich models (e.g., catalogue)

## Phase 5: Unified REST API for Multiple Formats

### Current State (Already Implemented)

**JSON/JSON-LD is already unified!** The `stripJsonLdKeywords()` function:
- Removes `@` prefixed fields if present (JSON-LD)
- Passes through unchanged if none (plain JSON)

So `POST /{schema}/api/ttl2/{table}` already accepts:
```json
// Plain JSON - works
{"name": "pooky", "category": "cat"}

// JSON-LD - works (@ fields stripped)
{"@type": "my:Pet", "name": "pooky", "category": {"name": "cat"}}
```

### Remaining Work

| Feature | Status |
|---------|--------|
| JSON input | ✅ Done |
| JSON-LD input | ✅ Done |
| TTL/RDF input | ❌ TODO - parse RDF → JSON-LD → strip → save |
| Row-level `/{table}/{id}` | ❌ TODO |
| Reference flattening | ❌ TODO (see separate plan) |

### Proposed Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/{schema}/api/rdf` | Export entire schema |
| POST | `/{schema}/api/rdf` | Import entire schema |
| GET | `/{schema}/api/rdf/{table}` | Export table data |
| POST | `/{schema}/api/rdf/{table}` | Import table data |
| PUT | `/{schema}/api/rdf/{table}` | Upsert table data |
| DELETE | `/{schema}/api/rdf/{table}` | Delete table data |
| GET | `/{schema}/api/rdf/{table}/{id}` | Export single row |
| PUT | `/{schema}/api/rdf/{table}/{id}` | Update single row |
| DELETE | `/{schema}/api/rdf/{table}/{id}` | Delete single row |

### Content Negotiation

Use `Accept` / `Content-Type` headers:
- `application/json` → plain JSON (GraphQL-like)
- `application/ld+json` → JSON-LD with @context
- `text/turtle` → TTL format
- `application/rdf+xml` → RDF/XML
- `application/n-triples` → N-Triples

---

## Comparison with Linked Data Platform (LDP) Specification

Reference: [W3C LDP 1.0](https://www.w3.org/TR/ldp/) | [LDP Primer](https://www.w3.org/TR/ldp-primer/)

### Conceptual Mapping

| LDP Concept | MOLGENIS Equivalent |
|-------------|---------------------|
| LDP Container (LDPC) | Table |
| LDP Resource (LDPR) | Row |
| Container URI | `/{schema}/api/rdf/{table}` |
| Resource URI | `/{schema}/api/rdf/{table}/{mg_id}` |
| `ldp:contains` | Foreign key relationships |
| Resource creation (POST) | `POST /{table}` with JSON/JSON-LD |

### LDP Compliance Assessment

| LDP Requirement | Our Status | Notes |
|-----------------|------------|-------|
| **HTTP Methods** | | |
| GET resources | ✅ Compliant | Export as JSON-LD/TTL |
| POST to container | ✅ Compliant | Create rows in table |
| PUT resource | ⚠️ Partial | Need row-level endpoint |
| PATCH resource | ❌ Missing | Could add later |
| DELETE resource | ⚠️ Partial | Need row-level endpoint |
| OPTIONS | ❌ Missing | Should return allowed methods |
| HEAD | ✅ Exists | In RDFApi |
| **Content Negotiation** | | |
| Turtle (MUST) | ✅ Compliant | `text/turtle` supported |
| JSON-LD (SHOULD) | ✅ Compliant | `application/ld+json` |
| **Headers** | | |
| `Link: rel="type"` | ❌ Missing | Should indicate LDPC/LDPR |
| `Slug` hint | ❌ Missing | Client-suggested URI |
| `ETag` | ❌ Missing | For concurrency control |
| `If-Match` | ❌ Missing | Prevent lost updates |
| `Location` on 201 | ⚠️ Partial | Should return created URI |
| `Accept-Post` | ❌ Missing | Advertise accepted types |
| **Container Types** | | |
| BasicContainer | ✅ Similar | Table as container |
| DirectContainer | ❌ N/A | Custom membership predicates |
| IndirectContainer | ❌ N/A | Indirect membership |

### Key Differences

1. **Container model**: LDP uses explicit `ldp:contains` triples; we use table/row structure
2. **Resource URIs**: LDP allows any URI; we use `{table}/{mg_id}` pattern
3. **Concurrency**: LDP requires ETag/If-Match; we rely on database transactions
4. **Type discovery**: LDP uses `Link` header; we use `@type` in JSON-LD body

### Recommendations for LDP-like Compliance

**Minimal (recommended):**
1. Add `Link: <http://www.w3.org/ns/ldp#BasicContainer>; rel="type"` header for table endpoints
2. Add `Link: <http://www.w3.org/ns/ldp#Resource>; rel="type"` header for row endpoints
3. Return `Location` header with created resource URI on POST 201

**Optional enhancements:**
4. Support `Slug` header for client-suggested IDs
5. Add `ETag` and `If-Match` for optimistic concurrency
6. Implement `OPTIONS` returning allowed methods
7. Add `Accept-Post` header on containers

### Decision

We are **LDP-inspired but not LDP-compliant**. Full compliance adds complexity (containment triples, Link headers, ETags) that may not be needed. Our approach:
- Same REST semantics (GET/POST/PUT/DELETE)
- Same content negotiation
- Simpler resource model (table=container, row=resource)
- JSON-LD context provides semantic mapping without explicit LDP vocabulary

---

## Comparison with Other REST Standards

References: [JSON:API](https://jsonapi.org/) | [Hydra](http://www.markus-lanthaler.com/hydra/) | [OData](https://www.odata.org/) | [HAL](https://stateless.group/hal_specification.html) | [Choosing a Hypermedia Format](https://sookocheff.com/post/api/on-choosing-a-hypermedia-format/)

### Standards Overview

| Standard | Focus | Complexity | Semantic Support |
|----------|-------|------------|------------------|
| **HAL** | Hypermedia links | Low | ❌ None |
| **JSON:API** | Full CRUD, relationships | Medium | ❌ None |
| **OData** | Query language, metadata | High | ⚠️ EDM schema |
| **Hydra+JSON-LD** | Semantic web, hypermedia | High | ✅ Full RDF |
| **OpenAPI** | Documentation/contract | Medium | ❌ None |

### Detailed Comparison

#### HAL (Hypertext Application Language)
```json
{
  "_links": { "self": { "href": "/pets/1" } },
  "name": "pooky",
  "_embedded": { "category": { "name": "cat" } }
}
```
- ✅ Simple, widely adopted
- ✅ Good for hypermedia navigation
- ❌ No semantic meaning
- ❌ No standard for actions/mutations

#### JSON:API
```json
{
  "data": {
    "type": "pets", "id": "1",
    "attributes": { "name": "pooky" },
    "relationships": { "category": { "data": { "type": "categories", "id": "cat" } } }
  }
}
```
- ✅ Standardized CRUD, errors, pagination
- ✅ Broad tooling support
- ✅ Compound documents (include related)
- ❌ No semantic/RDF support
- ❌ Rigid structure (type/id/attributes/relationships)

#### OData
```
GET /pets?$filter=status eq 'available'&$expand=category&$select=name
```
- ✅ Powerful query language ($filter, $expand, $orderby)
- ✅ Standardized metadata (EDM)
- ✅ Enterprise adoption (Microsoft, SAP)
- ❌ Complex, heavyweight
- ❌ Not RDF-compatible

#### Hydra + JSON-LD (closest to our approach)
```json
{
  "@context": "http://schema.org/",
  "@type": "Pet",
  "@id": "/pets/1",
  "name": "pooky",
  "category": { "@id": "/categories/cat" }
}
```
- ✅ Full semantic web support
- ✅ Self-describing API (operations in response)
- ✅ RDF-compatible
- ❌ Complex to implement fully
- ❌ Less tooling than JSON:API

### Our Position

| Feature | JSON:API | Hydra | Our Approach |
|---------|----------|-------|--------------|
| Resource wrapper | `{data: {type, id, attributes}}` | `{@type, @id, ...}` | `{@type, @id, ...}` ✅ |
| Relationships | Separate `relationships` object | Inline with `@id` | Inline ✅ |
| Semantic URIs | ❌ | ✅ via @context | ✅ via @context |
| Query subsetting | Sparse fieldsets | SPARQL/GraphQL | GraphQL ✅ |
| Content types | `application/vnd.api+json` | `application/ld+json` | Multiple ✅ |
| Hypermedia ops | ❌ | `hydra:operation` | ❌ (use GraphQL introspection) |

### Recommendation

We are closest to **Hydra + JSON-LD** but simpler:
- Use JSON-LD `@context` for semantics (like Hydra)
- Skip Hydra vocabulary for operations (use GraphQL instead)
- Accept plain JSON too (unlike strict Hydra)

**Why not JSON:API?**
- Requires wrapper structure (`data.attributes`) - more verbose
- No semantic web support
- We already have GraphQL for typed queries

**Why not OData?**
- Heavy, enterprise-focused
- Proprietary query syntax (we have GraphQL)
- No RDF compatibility

**Conclusion:** Our "super REST" = **JSON-LD lite** + **content negotiation** + **GraphQL for queries**

## Open Questions
1. How should composite keys be serialized in subject IRIs?
2. Should semantic annotations be required or optional?
3. What's the expected behavior for refback columns in JSON-LD?
4. How to handle FILE type columns in RDF?
5. How to handle import order for tables with circular references?
6. Should DELETE accept filter parameters or only work on specific IDs?

## Build Issue
Gradle build currently failing due to nyx plugin Git issue - likely needs clean checkout or skip nyx during development.
