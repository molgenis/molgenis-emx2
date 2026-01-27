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
- [ ] Factory methods should return schema to make `g` field final
- [ ] schema/database on GraphqlApi seems misplaced - only used in one method
- [ ] Don't expose testing-only methods in public interfaces
- [ ] Unused `inheritedPrefixes` parameter in recursive scanning
- [ ] Throw exceptions instead of logging validation errors to stderr
- [ ] Leverage Jackson more for JSON node manipulation

### Semantic Issues (svandenhoek) - Critical
- [ ] Missing `my` prefix definition
- [ ] Missing `rdf:type` triples
- [ ] Using `mg_id` instead of composite keys for subjects
- [ ] Invalid IRI syntax (`<my:.>`)
- [ ] Semantic fields missing entirely (semantics from columns/tables not used)
- [ ] Non-deterministic blank nodes in output
- [ ] Incorrect object handling (ontologyTermURI should be IRI, not literal)
- [ ] Data mapping errors (Tags incorrectly inheriting Pet properties)

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

## Open Questions
1. How should composite keys be serialized in subject IRIs?
2. Should semantic annotations be required or optional?
3. What's the expected behavior for refback columns in JSON-LD?
4. How to handle FILE type columns in RDF?

## Build Issue
Gradle build currently failing due to nyx plugin Git issue - likely needs clean checkout or skip nyx during development.
