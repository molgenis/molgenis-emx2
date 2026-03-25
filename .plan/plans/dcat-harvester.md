# RDF Harvest Plan

## Goal
Generic, metadata-driven RDF import for MOLGENIS EMX2. Reverses existing semantic annotations on columns to map incoming RDF triples to table rows. Supports URL fetching (with lazy URI dereferencing) and file upload. Runs as an EMX2 Task with UI in the updownload app.

The engine is **fully generic** — it works for any schema with semantic annotations, not just DCAT/catalogue. DCAT harvesting is the first use case.

## Status: PHASE 1-6 IMPLEMENTED, ADDITIONAL TESTING NEEDED

## Decisions

### Approach: Reverse Semantic Annotations (Option C)

**Chosen over:**

#### Option A': Auto-generated JSON-LD Frame
Generate a JSON-LD frame from column semantic annotations, apply to input graph, walk resulting JSON tree to build rows.
- (+) Standard, gives nested tree structure matching the Resource-as-document model
- (-) Frames get complex with multi-table mapping; columns with multiple semantic URIs (e.g. `website` = `dcat:landingPage, foaf:homepage`) can only match one per frame
- (-) Composite keys and ontology code mapping still need post-processing in Java
- (-) Debugging frame issues is harder than debugging triple iteration
- (-) Essentially builds Option C with an extra intermediate representation

#### Option B': Auto-generated SPARQL CONSTRUCT
Generate SPARQL CONSTRUCT queries per table from column semantic annotations, execute against in-memory RDF4J repository.
- (+) Very expressive — OPTIONAL, FILTER, BIND for transformations
- (-) Auto-generating correct SPARQL for ref_array, ontology_array, composite keys, multi-semantic columns is building a query compiler
- (-) Needs in-memory SPARQL engine (adds complexity)
- (-) CONSTRUCT output still needs post-processing to group into rows
- (-) Harder to debug than direct triple iteration

#### Option D: RML (RDF Mapping Language)
Use RML processor to map between RDF and tabular format.
- (+) Community standard
- (-) Heavy external dependency, learning curve, overkill for this use case

#### Why Option C wins
1. **Simplest code path** — no intermediate representation to generate/maintain
2. **Multi-semantic columns handled naturally** — both `dcat:landingPage` and `foaf:homepage` resolve to `website` column
3. **Composite keys** — resolved directly in Java when building rows (group triples by subject)
4. **Ontology recoding** — lookup step slots in per-value during row building
5. **Ref resolution** — follow ref subjects recursively, simple pattern
6. **Zero duplication** — mapping IS the data model; adding semantic annotations to a column automatically makes it harvestable
7. **Easy to debug** — log which triple mapped to which column, warn on unmapped predicates

### Memory model: Streaming filter
**No in-memory RDF model.** We use RDF4J's streaming `RDFHandler` to parse triples one-by-one. Each triple is checked against the predicate map — matched triples accumulate into a `Map<Subject, Map<Column, List<Value>>>`. Unmatched triples are discarded immediately.

**Why this works:**
- The predicate map (from schema annotations) is small and static
- The accumulated result is proportional to *matched* triples (rows × annotated columns), not input size
- A 10M-triple DCAT dump with 50 annotated columns and 5000 resources produces ~250K entries — trivial
- `rdf:type` triples are also captured during streaming for type discrimination
- No triple count limit needed — memory is bounded by output size, not input size

**Dereferencing:** After the first streaming pass, collect unresolved ref target URIs. Dereference each (also streaming). Repeat until depth limit reached (max depth=2, max fetches=100).

### Data path: Row objects directly, not GraphQL
Import builds `Row` objects and calls `Table.save(List<Row>)` directly — same as CSV import pipeline. No GraphQL layer: it would add HTTP overhead, serialization, and auth complexity for no benefit. `Table.save()` is what GraphQL mutations call anyway.

### Value conversion: reuse TypeUtils
No separate `RdfValueConverter` class needed. The conversion is thin:
- RDF Literal → `literal.stringValue()` → `TypeUtils.getTypedValue(string, columnType)` (same as CSV import)
- RDF IRI (hyperlink) → `iri.stringValue()` → store as string
- RDF IRI (ref/ref_array) → look up subject in accumulated data → resolve to pid
- RDF IRI (ontology) → `OntologyMapper` lookup via alternativeIds

`ColumnTypeRdfMapper` already defines the XSD datatype ↔ ColumnType mapping for export. We reverse it implicitly: `TypeUtils` already handles the string forms that XSD literals produce.

### Module location
Add to **existing `molgenis-emx2-rdf` module** — it already has RDF4J dependencies and namespace handling. Import is the natural inverse of export. Classes prefixed `RdfImport*` to distinguish from export classes.

### Computed columns
Computed columns (like `rdf type` on Resources) are **skipped during import**. The `rdf:type` triple is used only as a **table/type discriminator**, not mapped to a column value.

### Type discrimination
**Hardcoded for v1.** The mapping from `rdf:type` IRI to (table, type-value) is defined in code:
- `dcat:Catalog` → Resources, type=Catalogue
- `dcat:Dataset` → Resources, type=Cohort study (default)
- `foaf:Agent` / `org:Organization` → Agents
- `vcard:Individual` → Contacts

Goal is to make data model self-describing so this can be derived from metadata in v2.

---

## Architecture Overview

```
Input (URL or file)
    |
    v
[1. RdfFetcher] -- fetch URL with content negotiation, or accept file upload
    |               returns InputStream + format
    v
[2. Streaming RDF Parser (RDFHandler)]
    |   For each triple:
    |     - rdf:type? → record subject→type mapping
    |     - predicate in annotation map? → accumulate in subject→column→values map
    |     - else → count for "unmapped" warning summary
    v
[3. Accumulated Data: Map<Subject, Map<Column, List<Value>>>]
    |   (small — only matched triples)
    |
    v
[4. Ref Resolution Pass]
    |   Collect unresolved ref URIs → dereference (streaming) → merge results
    |   Repeat up to depth=2, max=100 fetches
    v
[5. Row Builder]
    |   For each subject:
    |     - Determine target table from rdf:type (TypeDiscriminator)
    |     - Convert values: literal.stringValue() → TypeUtils.getTypedValue() (reuses CSV import path)
    |     - Resolve ontology codes via OntologyMapper (alternativeIds lookup)
    |     - Resolve refs via pid lookup in accumulated data
    |     - Generate pid from URI local name if missing
    |     - Build Row objects
    v
[6. EMX2 upsert] -- save() rows, keyed on pid, wrapped in schema.tx()
    |
    v
[7. Task reporting] -- progress, warnings, unmapped predicates summary
```

---

## Implementation Plan (TDD — Red/Green cycles)

### Phase 1: Foundation

#### 1.1 Add `alternativeIds` column to ontology table format
- Extend ontology table base definition with `alternativeIds` (string_array)
- Allows ontology terms to carry mapped external URIs
- E.g., `Release type` term `Annually` gets `alternativeIds: ["http://publications.europa.eu/resource/authority/frequency/ANNUAL"]`
- Location: core metadata model where ontology columns are defined

#### 1.2 Sample test data
- Create `test-dcat-catalog.ttl` — small DCAT Turtle file covering:
  - 1 `dcat:Catalog` with title, description, publisher, contactPoint
  - 2 `dcat:Dataset` entries with keywords, themes, temporal coverage
  - 1 `foaf:Agent` (publisher)
  - 1 `vcard:Individual` (contact)
  - Ontology values using EU authority URIs (requiring alternativeIds mapping)
  - Multi-valued properties (keywords, themes)
  - A URI that would need dereferencing (dataset linked but not inline)

### Phase 2: Core Engine (TDD in `molgenis-emx2-rdf`)

Each step: write failing test FIRST, then implement to make it pass.

#### 2.1 ReverseAnnotationMapper — predicate-to-column mapping

**RED**: Test that given a SchemaMetadata with semantic annotations, the mapper builds a correct `Map<IRI, List<ColumnMapping>>`.
- Column with single semantic → 1 entry
- Column with multiple semantics (e.g. `dcat:landingPage, foaf:homepage`) → 2 entries pointing to same column
- Multiple columns with same semantic → multiple ColumnMappings per IRI
- Computed columns → excluded from map
- `rdf:type` → not in map (handled separately)

**GREEN**: Implement `ReverseAnnotationMapper.buildPredicateMap(SchemaMetadata)`.

#### 2.2 Streaming triple filter (FilteringRdfHandler)

**RED**: Test that given a predicate map and a Turtle input stream, the handler:
- Captures `rdf:type` triples into `Map<Resource, Set<IRI>>` (subject → types)
- Captures matched triples into `Map<Resource, Map<ColumnMapping, List<Value>>>` (subject → column → values)
- Discards non-matching triples
- Counts discarded triples for summary

**GREEN**: Implement `FilteringRdfHandler implements RDFHandler`.
- `handleStatement(Statement)` checks predicate against map, accumulates or discards
- After parsing: exposes accumulated data + type map + discard count

#### 2.3 Type discrimination

**RED**: Test that given accumulated type map:
- Subject with `rdf:type dcat:Catalog` → assigned to Resources table, type=Catalogue
- Subject with `rdf:type dcat:Dataset` → assigned to Resources table, type=Cohort study
- Subject with `rdf:type foaf:Agent` → assigned to Agents table
- Subject with unknown rdf:type → logged warning, skipped
- Subject with no rdf:type → logged warning, skipped

**GREEN**: Implement `TypeDiscriminator.assignTable(subjectTypes)`.
- Hardcoded v1 mapping; returns (tableName, optionalTypeValue) or null

#### 2.4 OntologyMapper

**RED**: Test ontology term resolution:
- Match by `alternativeIds` (external URI found in alternativeIds array) → returns term name
- Match by `ontologyTermURI` → returns term name
- Match by `name` (case-insensitive) → returns term name
- No match → returns null, logs warning
- Cache: second lookup for same table doesn't re-query

**GREEN**: Implement `OntologyMapper`.
- Takes schema, loads ontology table rows on first access per table
- Builds lookup indexes: alternativeIds→term, ontologyTermURI→term, lowerName→term

#### 2.5 Ref resolution

**RED**: Test that ref/ref_array columns resolve correctly:
- Object is a URI with data in accumulated map → resolve to referenced entity's pid
- Object URI not in accumulated map → added to "needs dereferencing" set
- Circular ref → detect and break cycle
- ref_array → multiple objects collected

**GREEN**: Implement ref resolution in row builder.

#### 2.6 Row building — end-to-end per subject

**RED**: Test full row building for a `dcat:Dataset` subject:
- All literal columns populated correctly (via `TypeUtils.getTypedValue()`)
- Ontology columns mapped via OntologyMapper
- Ref columns resolved to referenced entity pids
- pid generated from URI local name when dcterms:identifier missing
- Unmapped predicates counted in warning summary

**GREEN**: Implement `RowBuilder.buildRows(accumulatedData, typeAssignments, schema)`.
- Value conversion: `literal.stringValue()` → `TypeUtils.getTypedValue(string, columnType)` (reuses existing CSV import path)
- IRI values: `iri.stringValue()` for hyperlinks, OntologyMapper for ontology types, pid lookup for refs
- Returns `Map<String, List<Row>>` (tableName → rows)
- Plus list of warnings

### Phase 3: RDF Fetching

#### 3.1 RdfFetcher — parse from InputStream

**RED**: Test parsing Turtle, JSON-LD, RDF/XML from InputStream through FilteringRdfHandler.
- Auto-detect format from content type or file extension
- Invalid RDF → clear error

**GREEN**: Implement `RdfFetcher.parse(InputStream, formatHint, RDFHandler)`.

#### 3.2 RdfFetcher — URL fetching with content negotiation

**RED**: Test (with mock HTTP) that fetcher sends proper Accept headers and parses response.
- Content negotiation: `text/turtle, application/ld+json, application/rdf+xml`
- Follow redirects
- HTTP errors → clear error message

**GREEN**: Implement `RdfFetcher.fetch(URL, RDFHandler)`.

#### 3.3 Dereferencing pass

**RED**: Test that after initial parse, unresolved ref URIs are dereferenced:
- Fetches URI, streams through same FilteringRdfHandler (merges into accumulated data)
- Respect max depth (2) and max fetches (100)
- Already-fetched URIs not re-fetched
- Unreachable URLs → warning, not fatal
- Stops when no new unresolved URIs or limits reached

**GREEN**: Implement `RdfFetcher.resolveReferences(accumulatedData, unresolvedUris, depth)`.

### Phase 4: Task + API

#### 4.1 RdfImportTask

**RED**: Test task lifecycle:
- Subtask progression: Fetching → Mapping → Processing → Importing → Summary
- Task status transitions: WAITING → RUNNING → COMPLETED
- Error during fetch → task ERROR with message
- Summary includes: records per table, warnings count, unmapped predicate count

**GREEN**: Implement `RdfImportTask extends Task`.
- Constructor: schema, source (URL or InputStream), format hint, options
- Orchestrates: build predicate map → stream-parse → resolve refs → build rows → upsert
- Wraps import in schema.tx() for atomicity
- Merge strategy: upsert on `pid`

#### 4.2 API endpoint

**RED**: Integration test — POST URL to endpoint → task created → poll → rows in database.

**GREEN**: Add routes:
- `POST /{schema}/api/rdf/import` — harvest from URL (body: `{"url": "..."}`)
- `POST /{schema}/api/rdf/import/file` — harvest from file upload
- Returns task ID for progress polling

### Phase 5: Integration Tests

**Status: Basic tests implemented. See Phase 7 for additional smoke tests and roundtrip verification.**

#### 5.1 Full round-trip test
- Load catalogue schema with demo data model
- Add alternativeIds to relevant ontology terms
- Harvest `test-dcat-catalog.ttl`
- Verify:
  - Catalog resource created with type=Catalogue
  - Dataset resources created with type=Cohort study
  - Agent created and linked as publisher
  - Contact created and linked as contactPoint
  - Ontology values resolved via alternativeIds
  - Keywords populated as string_array
  - Warnings logged for unmapped predicates

#### 5.2 Merge/update test
- Harvest same file twice
- Verify: no duplicates, values updated, row count unchanged

#### 5.3 Edge cases
- Missing pid → generated from URI local name
- Empty/malformed RDF → clear error
- Dereference target returns 404 → warning, continues
- Subject with predicates for multiple tables → warning, best-effort assignment

### Phase 6: UI (updownload app)

#### 6.1 Add RDF import route
- New route/section in apps/updownload
- Simple form:
  - Text input for URL
  - File upload input (accept: .ttl, .jsonld, .rdf, .nt)
  - "Import" button
- Task progress display (poll task status endpoint)
- Show warnings/errors from task
- Show summary (records imported/updated per table)

### Phase 7: Smoke Tests & Roundtrip Verification

#### 7.1 WebApi smoke test for RDF import endpoints
Add RDF import tests to the existing `WebApiSmokeTests` pattern:
- Extend `ApiTestBase` (starts Javalin on port 8081, RestAssured)
- Create schema with semantic annotations + ontology terms with alternativeIds
- **File upload test**: POST multipart to `/{schema}/api/rdf/import/file` with test TTL, poll task, verify rows
- **URL harvest test**: POST JSON to `/{schema}/api/rdf/import` — requires a URL that the server can fetch. Options:
  - Self-serve: export TTL from one schema via `/{schema}/api/ttl`, then POST that URL to the import endpoint of another schema
  - This naturally becomes the roundtrip test (7.2)
- Verify task status polling returns COMPLETED
- Verify imported rows match expectations

#### 7.2 RDF export → import roundtrip test
True end-to-end validation that the RDF we export can be imported back:
1. Load a schema with semantic annotations + data (e.g., the test inline schema with demo rows)
2. Export as TTL via `GET /{schema}/api/ttl`
3. Create a second empty schema with the same structure + ontology terms
4. Import the exported TTL into schema2 via `POST /{schema2}/api/rdf/import` with URL `http://localhost:8081/{schema1}/api/ttl`
5. Verify schema2 has the same data as schema1
6. This tests: content negotiation, RDF export format compatibility, streaming parse, annotation mapping, ontology resolution, ref resolution, upsert

#### 7.3 Strengthen ontology term mapping assertions
Current `importedDatasetsHaveThemes` test only checks themes array is non-empty.
Strengthen to verify:
- HEAL URI → resolved to "Health" (via ontologyTermURI match)
- SOCI URI → resolved to "Society" (via alternativeIds match)
- Both mapping paths exercised and verified by name

#### 7.4 Edge case tests
- Import RDF with no matching semantic annotations → empty result, no error
- Import empty/malformed RDF → clear error message
- Import RDF where ontology term is not found → warning logged, field null

### Phase 8: Composite Primary Key Resolution (v2)

**Problem:** The catalogue data model uses composite primary keys for child tables. For example, `Agents` (parent of `Organisations`) has `resource` (REF to Resources, key=1) + `id` (key=1). The `resource` column has no semantic annotation — it represents the parent-child scoping relationship. When importing from RDF, the flat triple model loses this scoping: we see `<resource> dcterms:publisher <org>` and `<org> foaf:name "UMCG"` as independent subjects, but can't automatically set `resource` on the Organisation row.

**Would JSON-LD frames solve this?** Partially. Frames nest child objects inside parents, preserving the scoping. But:
- Frames need schema-specific configuration to know which refs embed children
- The catalogue model expects one Agents row per Resource (same org appears N times), while RDF deduplicates by subject URI
- Composite keys are a data-model choice, not an RDF concept — any approach needs to understand the schema

**Proposed solution: Reverse ref detection in RowBuilder**

For tables with composite primary keys where one key column is a REF without semantic annotation:

1. **Detect the pattern:** During row building, identify target tables where pkey includes a REF column that has no semantic annotation (e.g., `Agents.resource`)
2. **Build a reverse index:** From the accumulated data, build a map of `<child-subject> → List<(parent-subject, predicate)>` — which parent subjects reference this child via which predicate
3. **Match the predicate to the REF column:** The predicate `dcterms:publisher` on Resources maps to the `publisher` column. The `publisher` column targets `Organisations`. The `Organisations.resource` column targets `Resources`. So `resource` = the parent Resource's primary key.
4. **Set the composite key:** For each Organisation row, set `resource` = the referencing Resource's id, `id` = the Organisation's extracted identifier

**Algorithm:**
```
For each child subject assigned to a table with composite pkey:
  For each pkey column that is a REF without semantic annotation:
    Find which parent subjects reference this child
    Resolve the parent's primary key
    Set the REF pkey column to that value
    If multiple parents reference the same child:
      Create one row per parent (duplicate with different resource values)
```

**Edge cases:**
- Multiple parents referencing same child → one row per parent (matches catalogue model)
- Circular refs → detect and break
- No parent found → warning, skip row (can't satisfy required pkey)

**Impact:** This would make the roundtrip test pass for Organisations and Contacts with exact count matching.

#### 8.1 Reverse ref detection
- Identify composite pkey patterns in target schema
- Build reverse index from accumulated matched data

#### 8.2 Row duplication for multi-parent refs
- When same Organisation is publisher of 3 Resources, create 3 rows

#### 8.3 Integration with RowBuilder
- Extend `buildRows()` to handle composite pkeys
- Add tests with catalogue-like schema structure

---

## Key Files to Create/Modify

### New files (in existing molgenis-emx2-rdf module)
- `src/main/java/org/molgenis/emx2/rdf/ReverseAnnotationMapper.java` — predicate map builder
- `src/main/java/org/molgenis/emx2/rdf/FilteringRdfHandler.java` — streaming triple filter
- `src/main/java/org/molgenis/emx2/rdf/TypeDiscriminator.java` — rdf:type → table mapping
- `src/main/java/org/molgenis/emx2/rdf/OntologyMapper.java` — ontology code resolution
- `src/main/java/org/molgenis/emx2/rdf/RowBuilder.java` — accumulated data → Row objects (uses TypeUtils for value conversion)
- `src/main/java/org/molgenis/emx2/rdf/RdfFetcher.java` — URL fetch + content negotiation
- `src/main/java/org/molgenis/emx2/rdf/RdfImportTask.java` — Task orchestrator
- `src/test/java/org/molgenis/emx2/rdf/ReverseAnnotationMapperTest.java`
- `src/test/java/org/molgenis/emx2/rdf/FilteringRdfHandlerTest.java`
- `src/test/java/org/molgenis/emx2/rdf/TypeDiscriminatorTest.java`
- `src/test/java/org/molgenis/emx2/rdf/OntologyMapperTest.java`
- `src/test/java/org/molgenis/emx2/rdf/RowBuilderTest.java`
- `src/test/java/org/molgenis/emx2/rdf/RdfFetcherTest.java`
- `src/test/java/org/molgenis/emx2/rdf/RdfImportTaskTest.java`
- `src/test/resources/test-dcat-catalog.ttl`
- UI components in `apps/updownload/`

### Modified files
- Ontology table definition — add `alternativeIds` column
- `backend/molgenis-emx2-webapi/` — register import API routes
- `apps/updownload/` — add import UI route

---

## Open Questions
- [x] pid when missing → generate from URI local name
- [x] Max dereference depth=2, max fetches=100
- [x] Memory → streaming filter, accumulate only matched triples
- [x] Module location → existing molgenis-emx2-rdf
- [x] Type discrimination → hardcoded v1, data-model-driven v2
- [ ] Should composite pkey resolution use REFBACK columns to detect parent→child relationships? (REFBACK columns explicitly define the inverse)
- [ ] Performance impact of reverse index on large graphs?
- [ ] Should we support authentication for protected RDF endpoints? (defer to v2)
- [ ] Should we support incremental harvesting / change detection? (defer to v2)
- [ ] Scheduled/recurring harvesting via TaskScheduler cron? (defer to v2)
- [ ] How to handle `dcat:Distribution` and `dcat:DataService` classes? (no matching table yet, defer)

---

## Out of Scope (v1)
- SPARQL endpoint harvesting (only document-based RDF)
- Bi-directional sync
- Scheduled/recurring harvesting
- SHACL validation of input
- Distribution/DataService DCAT classes
- Authentication for protected endpoints
- Data-model-driven type discrimination (v2)
