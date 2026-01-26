# DCAT Harvesting Specification

## Purpose

Harvest DCAT metadata from FAIR Data Points (FDP) and import into MOLGENIS catalogue schema.

## Use Case

```bash
# Phase 1: Fetch and explore
./fairmapper fetch-rdf https://fdp.some.nl/catalog/d7522c39-...

# Later: Full harvest
./fairmapper harvest dcat-fdp \
  --source https://fdp.some.nl/catalog/... \
  --server http://localhost:8080 \
  --schema catalogue \
  --token xyz
```

## Data Flow (Target)

```
FDP (Turtle RDF)                    MOLGENIS DataCatalogue
─────────────────                   ─────────────────────
dcat:Catalog ──────────────────────→ Resources (type=Catalogue)
  dcterms:title                        name
  dcterms:description                  description
  dcterms:publisher ───────────────→ Organisations → publisher ref
  dcat:contactPoint ───────────────→ Contacts → contact point ref
  dcat:dataset ────────────────────→ Resources (type=Dataset)
```

## Design Goal: Frame-Driven Fetching

**Core idea**: The JSON-LD frame defines what data you WANT. The system figures out what to FETCH.

Like GraphQL - you specify the shape, system resolves it automatically.

### Example Frame (catalog-with-datasets.jsonld)

```json
{
  "@context": {"dcat": "http://www.w3.org/ns/dcat#", "dcterms": "http://purl.org/dc/terms/"},
  "@type": "dcat:Catalog",
  "dcterms:title": {},
  "dcterms:description": {},
  "dcat:dataset": {
    "@type": "dcat:Dataset",
    "@embed": "@always",
    "dcterms:title": {},
    "dcterms:identifier": {},
    "dcat:keyword": []
  }
}
```

### System Behavior

1. Fetch entry URL (catalog)
2. See `dcat:dataset` with `@embed: @always`
3. Current data only has URI reference → fetch that URI
4. Combine results, apply frame
5. Output: deterministic JSON matching frame shape

### FAIRmapper Config

```yaml
steps:
  - fetch: ${SOURCE_URL}
    accept: text/turtle
    frame: src/frames/catalog-with-datasets.jsonld  # frame drives fetching
    maxDepth: 2                                      # safety limit
  - transform: src/transforms/to-molgenis.jslt
  - mutate: src/mutations/upsert.gql
```

No separate "follow these predicates" config needed - the frame IS the specification.

## Implementation Decision: Recursive Pre-framing Analyzer (Option 2)

Analyze frame structure upfront, recursively fetch linked resources at each depth level.

### Algorithm

```
analyzeFrame(frame, depth=0):
  for each predicate with @embed:
    add to fetchList[depth]
    analyzeFrame(nested frame, depth+1)

fetch(url, maxDepth):
  model = fetchUrl(url)
  for depth 0..maxDepth:
    for predicate in fetchList[depth]:
      uris = extractUris(model, predicate)
      for uri in uris:
        model.merge(fetchUrl(uri))
  return frame(model)
```

### Why Option 2
- Simpler than hooking into framing internals (Option 1)
- No gap detection needed (Option 3)
- Frame structure drives fetching declaratively
- maxDepth provides safety limit

### Package Structure

```
org.molgenis.emx2.fairmapper/
  RunFairMapper.java
  ...existing...

org.molgenis.emx2.fairmapper.rdf/     # NEW subpackage
  RdfFetcher.java                      # existing, move here
  RdfToJsonLd.java                     # existing, move here
  FrameAnalyzer.java                   # NEW: analyze frame for predicates
  FrameDrivenFetcher.java              # NEW: recursive fetch based on frame
  JsonLdFramer.java                    # NEW: apply frame to model
```

Later: publish as standalone library `molgenis-rdf-harvester`

## Phase 1: RDF → JSON-LD (POC Complete)

### Goal
Basic RDF fetch and JSON-LD conversion working. Link resolution not yet implemented.

### CLI Command

```bash
./fairmapper fetch-rdf <url>

# Options
--format jsonld|turtle  # Output format (default: jsonld)
```

### Status
- [x] RdfFetcher.java - HTTP GET with Accept: text/turtle
- [x] RdfToJsonLd.java - RDF4J Model → JSON-LD
- [x] fetch-rdf CLI command
- [x] Tests with real FDP
- [ ] Frame-driven link resolution (Phase 2)

### Dependencies

```gradle
implementation 'org.eclipse.rdf4j:rdf4j-rio-turtle:4.3.8'
implementation 'org.eclipse.rdf4j:rdf4j-rio-jsonld:4.3.8'
implementation 'org.eclipse.rdf4j:rdf4j-model:4.3.8'
```

## Phase 2: Frame-Driven Link Resolution

Implement one of the three options above to enable automatic link following based on frame.

## Phase 3: JSLT Transforms

Transform framed JSON-LD to MOLGENIS mutation format.

```
fair-mappings/dcat-fdp/
  fairmapper.yaml
  src/frames/
    catalog-with-datasets.jsonld
  src/transforms/
    to-molgenis.jslt
```

## Phase 4: Mutations

New step type for GraphQL mutations:

```yaml
steps:
  - fetch: ${SOURCE_URL}
    frame: src/frames/catalog-with-datasets.jsonld
  - transform: src/transforms/to-molgenis.jslt
  - mutate: src/mutations/upsert.gql
```

## Target Schema Mapping

| FDP Predicate | MOLGENIS Table.Column | Notes |
|--------------|----------------------|-------|
| Subject IRI | Resources.id | UUID from URL path |
| rdf:type | Resources.type | Catalogue/Dataset |
| dcterms:title | Resources.name | Required |
| dcterms:description | Resources.description | |
| dcterms:identifier | Resources.pid | DOI or other PID |
| dcat:contactPoint | Resources.contact point | Ref to Contacts |
| dcterms:publisher | Resources.publisher | Ref to Organisations |
| dcat:keyword | Resources.keywords | String array |
| dcat:theme | Resources.theme | Ontology (Themes) |
| dcat:landingPage | Resources.website | Hyperlink |
| dcterms:issued | Resources.issued | Datetime |
| dcterms:modified | Resources.modified | Datetime |

## Open Questions

1. JSON-LD framing: does it give deterministic shapes?
2. Link following: fetch linked datasets automatically?
3. Organisation handling: create new or match existing?
4. Theme mapping: FDP EU URIs → CatalogueOntologies?
5. Distributions: skip for now (no table)?
