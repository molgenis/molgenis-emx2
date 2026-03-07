# molgenis-emx2-fairmapper

Harvests external RDF sources and imports the data as rows into an EMX2 schema.
The pipeline is generic; DCAT is the first concrete mapping type.

## Architecture overview

```
Catalog URL
    |
    v
[RdfFetcher]           -- HTTP GET, parse Turtle to RDF4J Model
    |
    v
[FrameDrivenFetcher]   -- follow linked URIs based on @embed in frame
    |
    v  RDF4J Model (all fetched statements)
    |
[RdfToJsonLd]          -- serialize Model to expanded JSON-LD
    |
    v  expanded JSON-LD string
    |
[JsonLdFramer]         -- apply JSON-LD frame, shape the tree
    |
    v  framed JSON tree (nested, prefixed)
    |
[JsltTransformer]      -- JSLT template maps to {Resources:[...], Organisations:[...]}
    |
    v  MOLGENIS row format
    |
[DcatHarvestService]   -- Table.save(rows) for each target table
```

## Package layout

```
fairmapper/
  rdf/           -- generic: fetch, convert, frame any RDF source
  transform/     -- generic: apply JSLT transformation to any JSON
  dcat/          -- specific: DCAT mapping, service, task, exception, report
```

The `rdf/` and `transform/` packages contain no DCAT-specific logic.
They are the reusable infrastructure for any future mapping type.

## How it works — step by step

### 1. URL validation

Before any HTTP call, the URL is validated for scheme (`http`/`https`).

### 2. Frame-driven fetch (`FrameDrivenFetcher`)

`FrameAnalyzer` inspects the JSON-LD frame for properties marked `"@embed":
"@always"`. These represent linked resources that must be fetched separately.
`FrameDrivenFetcher` starts with the catalog URL, then follows the IRI values of
those predicates (e.g. `dcat:dataset`) up to `maxDepth` levels and `maxCalls`
total HTTP requests. All fetched statements are merged into a single RDF4J
`Model`. Fetch failures are logged as warnings by default (`WARN_AND_CONTINUE`),
or rethrown (`FAIL_FAST`).

### 3. RDF to expanded JSON-LD (`RdfToJsonLd`)

The merged `Model` is serialized to JSON-LD in EXPAND mode using the RDF4J Rio
writer. Arrays are compacted but property IRIs remain fully qualified.

### 4. JSON-LD framing (`JsonLdFramer`)

The expanded JSON-LD is framed using the Titanium JSON-LD library. The frame
selects nodes by `@type`, determines which linked nodes are embedded inline, and
produces a predictable tree with the context from the frame attached.

### 5. JSLT transformation (`JsltTransformer`)

A JSLT template loaded from the classpath maps the framed JSON tree to the
target EMX2 row format. The DCAT template produces:

```json
{
  "Resources": [ { "id": "...", "name": "...", "type": [...], ... } ],
  "Organisations": [ { "id": "...", "resource": {"id": "..."} } ]
}
```

### 6. Import (`DcatHarvestService`)

Each array in the JSLT output is saved as rows into the matching EMX2 table.
The service reads `Resources` and `Organisations` tables from the target schema.
Missing tables produce warnings; a missing `Resources` table is a hard error.

## JSON-LD Frame generation

`JsonLdFrameGenerator` auto-generates a JSON-LD frame from an EMX2
`SchemaMetadata` object by reading its semantic annotations:

- Each table's `semantics` value maps to `@type` in the context entry for that
  table.
- Each column with `semantics` gets a context entry mapping the column name to
  the semantic IRI.
- Reference and ontology columns additionally get `"@type": "@id"` and a root-
  level `"@embed": "@always"` entry so linked resources are resolved inline.
- Columns without semantics are ignored.
- The schema identifier is added as a namespace prefix so unannotated refs still
  resolve.

This means a well-annotated EMX2 schema can produce a working frame without
writing any JSON by hand.

For the DCAT mapping the frame is generated at runtime from the target schema's
semantic annotations. The auto-generated frame is useful for prototyping new
mappings or when the schema is the authoritative source of truth.

## Adding a new mapping type

1. Create a subpackage, e.g. `fairmapper/myformat/`.
2. Write a JSON-LD frame (`myformat-frame.jsonld`) selecting the root type and
   embedded relations.
3. Write a JSLT template (`to-molgenis.jslt`) mapping framed output to
   `{ "TableA": [...], "TableB": [...] }`.
4. Create a service class that wires the generic pipeline:

```java
JsonNode frame = JsltTransformer.loadJsonResource(FRAME_RESOURCE);
RdfFetcher fetcher = new RdfFetcher();
FrameDrivenFetcher frameFetcher = new FrameDrivenFetcher(fetcher, new FrameAnalyzer());
Model model = frameFetcher.fetch(sourceUrl, frame, MAX_DEPTH, MAX_CALLS);
JsonNode framed = new JsonLdFramer().frame(model, frame);
JsonNode rows = new JsltTransformer(JSLT_RESOURCE).transform(framed);
```

5. Create a `Task` subclass (optional, for async progress tracking).
6. Register a REST endpoint in `molgenis-emx2-webapi` following the DCAT
   pattern.
7. Add unit tests with fixture files under `src/test/resources/`.

## DCAT harvester

The DCAT harvester fetches a `dcat:Catalog` from a remote URL, resolves linked
`dcat:Dataset` resources, and imports them into the target schema's `Resources`
table. The publisher organisation is imported into `Organisations`.

Field mapping:

| DCAT predicate       | EMX2 column  |
|----------------------|--------------|
| `@id` (last segment) | `id`         |
| `dcterms:title`      | `name`       |
| `dcterms:description`| `description`|
| `dcterms:identifier` | `pid`        |
| `dcat:keyword`       | `keywords`   |
| `@type`              | `type`       |
| `foaf:name` (publisher) | `id` in Organisations |

`dcat:Catalog` maps to type `Catalogue`; `dcat:Dataset` maps to `Databank`.

Resources:
- `src/main/resources/org/molgenis/emx2/fairmapper/dcat/to-molgenis.jslt` — JSLT template

## REST API

Registered by `DcatHarvestApi` in `molgenis-emx2-webapi`.

```
POST /{schema}/api/harvest/dcat
Content-Type: application/json

{ "url": "https://example.org/catalog/1" }
```

Returns `202 Accepted` with a task reference:

```json
{ "id": "<taskId>", "status": "RUNNING", ... }
```

Poll `/{schema}/api/tasks/{taskId}` for progress and completion status.
The task tracks four sub-steps: fetch RDF, frame JSON-LD, transform, import.

## Testing

Tests are in `src/test/java/` and use JUnit 5. Fixture files live in
`src/test/resources/org/molgenis/emx2/fairmapper/dcat/`.

Test layers:

| Test class              | What it covers                                      |
|-------------------------|-----------------------------------------------------|
| `RdfFetcherTest`        | Turtle parsing, size limits, scheme validation      |
| `FrameAnalyzerTest`     | Predicate extraction, depth limiting                |
| `JsonLdFrameGeneratorTest` | Frame generation from schema semantics           |
| `JsltTransformerTest`   | End-to-end JSLT mapping with fixture files          |

Pattern for a new mapping test:

1. Add `myformat-input.json` (framed JSON-LD fixture) and
   `myformat-expected-output.json` (expected MOLGENIS rows) under
   `src/test/resources/`.
2. Load both with `JsltTransformer.loadJsonResource(...)`.
3. Assert `transformer.transform(input).equals(expected)`.

For fetch tests, pass a lambda or stub as `RdfSource` rather than making real
HTTP calls.
