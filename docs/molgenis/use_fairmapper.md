# What is the FAIR Mapper

The FAIR Mapper is a command-line tool that harvests RDF data from remote endpoints and loads it
into a MOLGENIS EMX2 schema as regular tabular data. It currently focuses on harvesting
**DCAT** (and Health-DCAT-AP) metadata, for example catalogues, datasets/collections and
organisations exposed by a [FAIR Data Point](https://www.fairdatapoint.org/) (FDP).

The goal is to let a catalogue-style EMX2 schema be populated automatically from external,
FAIR-published metadata, instead of having to import CSV/Excel files by hand.

## How does the pipeline work?

A harvest run is a pipeline of five steps, described in a [pipeline file](#the-pipeline-file).
Each step's output can optionally be dumped to disk so you can inspect (and debug) what happened at
every stage.

1. **Extract** - fetch all RDF from the source into a temporary RDF4J repository. Starting from the
   source URL, the extract step can follow links level by level (crawl steps). For an FDP this
   means: resolve the FDP's metadata catalog(s), then every dataset in those catalogs, and so on.
2. **Pre-processing** - enrich the extracted RDF with additional statements before it is queried.
   Pre-processors run SPARQL `CONSTRUCT` queries against the repository and add the results back
   into it. For example, deriving a plain `dcat:startDate`/`dcat:endDate` from a
   `dcterms:temporal` interval, or normalising abbreviated Health-DCAT-AP predicates
   (`minTypicalAge`/`maxTypicalAge`) to their canonical form.
3. **Transform** - generate a SPARQL `SELECT` query for each table you want to harvest, based on
   that table's EMX2 metadata (columns and their `semantics` annotations), run it against the
   enriched repository, and write the results into an in-memory `TableStore` (effectively a CSV
   per table).
4. **Post-processing** - the tabular data produced by the transform step is further cleaned up by
   post-processors, for example: deriving an `id` column from other columns, resolving ontology
   term URIs to their names in the target schema, resolving rows that are missing a primary key,
   and dropping rows that still have no usable primary key.
5. **Upload** - the final table store is packaged as a ZIP file and uploaded to the target
   EMX2 instance's regular ZIP import API (data only, existing schema structure is left
   untouched).

# How to use the FAIR Mapper

## Prerequisites: a target schema to harvest into

The FAIR Mapper only ever loads *data*, it never creates schemas, tables or columns for you. So
before you run a harvest, the target schema and its tables must already exist on the EMX2 instance
you point `harvest` at. It can be created by MOLGENIS EMX2 itself (e.g. through the UI's
[schema creation](use_database.md), by uploading a `molgenis.csv`/schema definition, or via the
GraphQL/REST admin API).

That target schema also needs to be *semantically annotated* so the FAIR Mapper knows how RDF
predicates map onto your columns:

* It is recommended, but not required, to annotate a table with a `semantics` value identifying
  the RDF class it represents, e.g. a `Collections` table annotated with `dcat:Dataset`. Without
  it, the query won't filter by RDF type explicitly, but any column marked `required` still
  becomes a mandatory where clause/triple pattern, so in practice a subject still has to carry that
  data to match. Annotating the table just makes the type filter explicit (and the query cheaper).
* Each column you want filled needs a `semantics` value identifying the RDF predicate that holds
  its value, e.g. a `title` column annotated with `dcterms:title`. Columns without a `semantics`
  value are skipped entirely, they're left out of the generated query and stay empty.

This is the same `semantics` mechanism used elsewhere in EMX2's RDF support (see
[Linked data](semantics.md) for the full reference), including the list of predefined namespace
prefixes (`dcat:`, `dcterms:`, `healthdcatap:`, ...) you can use instead of full IRIs.

## Prerequisites: an EMX2 endpoint and access token

`harvest` talks to the target EMX2 instance entirely over HTTP: it looks up the target schema's
metadata through its GraphQL API, and (when the pipeline file has an `upload` step) uploads the
harvested data through its regular ZIP import API. The `emx2` section of the pipeline file needs:

* `endpoint` - the base URL of the target EMX2 instance, e.g. `https://my-emx2.example.org`. This
  can point at a local or a remote instance.
* `token` - an API token for that instance with read access to the target schema (and write access
  too, if you're uploading data). See [Tokens](use_tokens.md) for how to generate one.

## Build instructions

The FAIR Mapper lives in the `:backend:molgenis-emx2-fairmapper` Gradle module. Build a runnable,
all-in-one JAR with:

```bash
./gradlew :backend:molgenis-emx2-fairmapper:shadowJar
```

The resulting JAR is written to the module's build directory as
`backend/molgenis-emx2-fairmapper/build/libs/fairmapper-<version>-cli.jar`

## Run instructions

The FAIR Mapper is a [picocli](https://picocli.info/)-based CLI with the main class
`org.molgenis.emx2.fairmapper.cli.FairMapper`. Run it with:

```bash
java -jar backend/molgenis-emx2-fairmapper/build/libs/fairmapper-<version>-cli.jar <command> [options]
```

`generate-query` connects directly to Postgres using the same environment variables as the rest of
MOLGENIS EMX2 (`MOLGENIS_POSTGRES_URI`, `MOLGENIS_POSTGRES_USER`, `MOLGENIS_POSTGRES_PASS`), so
make sure these point at the Postgres instance that holds the schema you're generating a query
for. `harvest` talks to the target EMX2 instance over HTTP/GraphQL instead (see the `emx2` section
of the [pipeline file](#the-pipeline-file)).

?>**Tip**: since the command gets long, it's convenient to define a shell alias, e.g.:

```bash
alias fairmapper='java -jar /path/to/fairmapper-<version>-cli.jar'
```

### `harvest`

Runs the harvest described by a [pipeline file](#the-pipeline-file): extract, pre-process,
transform, post-process and (optionally) upload.

```bash
fairmapper harvest -c <pipeline-file>
```

| Option           | Required | Description                                       |
|------------------|----------|---------------------------------------------------|
| `-c`, `--config` | yes      | The pipeline file (YAML) that describes the harvest. |

The whole pipeline file is checked before anything is fetched, so a typo in a step name or a
missing option fails straight away.

When `output` is set in the pipeline file, a subdirectory `fairmapper-output-<harvest-id>` is
created in that directory, containing:

* `extracted.ttl` - the raw RDF extracted from the source, before any pre-processing.
* `preprocessed.ttl` - the RDF after pre-processing (only written if pre-processors are configured).
* `transformed.zip` - a CSV-in-ZIP export of the table store right after the transform step.
* `postprocessed.zip` - the same, after post-processing has run. This is what would be uploaded to
  the schema when the pipeline file has an `upload` step.

#### The pipeline file

A pipeline file describes one harvest. This example harvests DCAT metadata from a FAIR Data Point
into a catalogue schema:

```yaml
schema: catalogue
tables: [ Catalogues, Collections, Organisations ]
# output: ./fairmapper-output   # uncomment to dump the result of every stage
emx2:
  endpoint: https://emx2.example.org
  token: your-token
steps:
  - extract:
      url: https://fdp.example.org
      crawl: fdp
  - preprocessing: [ temporal, typical-age, stage-csvw ]
  - transform
  - postprocessing:
      - coalesce-field: {
        table: Collections,
        field: id,
        derive-from: [ acronym, name ],
        strict: false
      }
      - coalesce-field: {
        table: Catalogues,
        field: id,
        derive-from: [ acronym, name ],
        strict: false
      }
      - coalesce-field: {
        table: Organisations,
        field: id,
        derive-from: [ organisation name ],
        strict: false
      }
      - resolve-static: {
        table: Collections,
        field: type,
        value: http://semanticscience.org/resource/SIO_001067
      }
      - resolve-static: {
        table: Catalogues,
        field: type,
        value: http://semanticscience.org/resource/SIO_001067
      }
      - resolve-ontologies
      - resolve-missing-pk
      - drop-missing-pk: {
        tables: [ Organisations ]
      }
  # - upload   # uncomment to upload; leave out for a dry run
```

The same file, with comments, is in the repository at
`backend/molgenis-emx2-fairmapper/examples/stage-fdp.yml`.

The whole file is checked before anything is fetched: an unknown field, step, pre-processor,
post-processor or option, a missing required option, or a value of the wrong type (e.g. a single
value where a list is expected) stops the harvest straight away with a message naming the problem.

| Field           | Required | Description                                                                                                                                                          |
|-----------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `schema`        | yes      | Name of the schema on the target EMX2 instance. Its metadata drives the transform step, and the data is uploaded into it.                                           |
| `tables`        | yes      | List of table names in that schema to harvest, at least one. Every table must exist in the schema; this is checked before extracting. Only these tables are transformed, dumped and uploaded. |
| `output`        | no       | Directory to write intermediate results to (see above). If omitted, nothing is dumped to disk.                                                                       |
| `emx2.endpoint` | yes      | Base URL of the target EMX2 instance, e.g. `https://my-emx2.example.org`.                                                                                            |
| `emx2.token`    | yes      | API token for the target EMX2 instance. See [Tokens](use_tokens.md).                                                                                                 |
| `steps`         | yes      | The steps of the harvest, see below.                                                                                                                                 |

#### Steps

Each entry under `steps` names one step. A step can be listed at most once, and the order in which
they are listed doesn't matter: they always run in the order extract, preprocessing, transform,
postprocessing, upload.

| Step             | Required | Options                                                                    |
|------------------|----------|----------------------------------------------------------------------------|
| `extract`        | yes      | See [extract](#extract-step).                                              |
| `preprocessing`  | no       | A list of [pre-processors](#pre-processors), run in the listed order.      |
| `transform`      | yes      | None.                                                                      |
| `postprocessing` | no       | A list of [post-processors](#post-processors), run in the listed order.    |
| `upload`         | no       | None. Leave it out for a dry run.                                          |

A step, pre-processor or post-processor without options is written as a bare name (`- transform`,
`- resolve-ontologies`); `transform: {}` means the same. Options are given as a map under the
name, either on one line (`drop-missing-pk: { tables: [ Organisations ] }`) or spread over several
lines as in the example above. Option names are written in lowercase with dashes
(`derive-from`, not `deriveFrom`).

##### Extract step

| Option   | Required | Default | Description                                                                                                                                                                                  |
|----------|----------|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `url`    | yes      |         | Absolute URL of the RDF to start from, e.g. the root of an FDP.                                                                                                                             |
| `crawl`  | no       | none    | Links to follow from `url`, level by level: a crawl preset name or a list of crawl steps (see below). Without `crawl`, only the document at `url` is fetched.                               |
| `strict` | no       | `true`  | When `true`, the harvest stops as soon as a resource can't be fetched. When `false`, the error is logged and the harvest carries on without that resource; useful for sources with a few broken links. |

`crawl` is either the name of a crawl preset or a list of crawl steps. The only preset is `fdp`,
which follows a FAIR Data Point from its root:

| Level          | Predicate followed      |
|----------------|-------------------------|
| `catalog`      | `fdp-o:metadataCatalog` |
| `dataset`      | `dcat:dataset`          |
| `distribution` | `dcat:distribution`     |
| `csvw`         | `dcat:downloadURL`      |

A list of crawl steps works the same way: each step takes the resources found by the previous step
(the first step starts from `url`), follows `predicate` from each of them, and fetches every
resource it points to. The order of the steps therefore has to match the structure of the source.

| Option      | Required | Description                                                                                                                                                                      |
|-------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `name`      | yes      | Name of the level, only used in logging.                                                                                                                                         |
| `predicate` | yes      | The predicate to follow: a full IRI (`http(s):...`, or any IRI between `<` and `>`) or a prefixed name using one of the predefined namespace prefixes (see [Linked data](semantics.md)). |

```yaml
crawl:
  - { name: catalog, predicate: fdp-o:metadataCatalog }
  - { name: dataset, predicate: dcat:dataset }
```

##### Pre-processors

Pre-processors add statements to the extracted RDF before the transform step queries it. None of
them take options.

| Name          | What it does                                                                                                                                                                                                       |
|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `temporal`    | For every `dcat:Dataset` and `dcat:Catalog` with a `dcterms:temporal` interval, adds the year of the interval's `dcat:startDate` and `dcat:endDate` as `dcat:startDate`/`dcat:endDate` directly on the resource. |
| `typical-age` | For every `dcat:Dataset`, copies the abbreviated Health-DCAT-AP `healthdcatap:minTypicalAge`/`maxTypicalAge` to `healthdcatap:minimumTypicalAge`/`maximumTypicalAge`, the predicates the EMX2 catalogue model uses. |
| `stage-csvw`  | For every `dcat:Dataset` whose distribution's `dcat:downloadURL` is a `csvw:TableGroup`, links the dataset to each `csvw:table` of that group with `healthdcatap:hasVariables`.                                   |

##### Post-processors

Post-processors change the rows produced by the transform step, before they are uploaded. They run
in the listed order, and the same post-processor can be listed more than once with different
options. `table` and `field` options refer to tables and columns of the target schema by name.

**`coalesce-field`** - sets a field to the first of several candidate fields that has a value, e.g.
to base an `id` on the acronym when there is one, and on the name otherwise.

| Option        | Required | Default | Description                                                                                                                                                         |
|---------------|----------|---------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `table`       | yes      |         | Table whose rows to change.                                                                                                                                         |
| `field`       | yes      |         | Field to set. An existing value is overwritten.                                                                                                                     |
| `derive-from` | yes      |         | List of candidate fields, in order of preference. The value of the first one that isn't empty is used.                                                             |
| `strict`      | no       | `true`  | What happens to a row where all candidate fields are empty: when `true`, the harvest stops with an error; when `false`, `field` is left empty for that row.        |

**`resolve-static`** - sets a field to the same value on every row, e.g. a fixed type.

| Option  | Required | Description                                                       |
|---------|----------|-------------------------------------------------------------------|
| `table` | yes      | Table whose rows to change.                                       |
| `field` | yes      | Field to set. An existing value is overwritten.                   |
| `value` | yes      | The value to set. Numbers and booleans keep their type.           |

**`resolve-ontologies`** - takes no options. After the transform step, ontology columns hold the
IRIs of ontology terms. For every ontology column of the harvested tables, this replaces each IRI
with the name of the term that has that IRI as its `ontologyTermURI`, looked up in the column's
ontology table on the target EMX2 instance. A value without a matching term is logged as a warning
and left empty.

**`resolve-missing-pk`** - takes no options. Right after the transform step, a reference only knows
the IRI of the row it points to, not yet that row's primary key. This fills in reference columns
with the primary key of the harvested row that has that IRI, across all tables of the schema. It
repeats until nothing changes (at most 10 passes), so references to rows whose own key depends on
another reference are resolved too.

**`drop-missing-pk`** - removes rows that still miss part of their primary key, e.g. organisations
that no other row referred to, so their key was never filled in. Each dropped row is logged as a
warning.

| Option   | Required | Description                                                     |
|----------|----------|-----------------------------------------------------------------|
| `tables` | yes      | List of tables to remove such rows from, at least one.          |

### `extract`

Runs just the extract step (step 1 in above pipeline) for a given endpoint and writes the
resulting RDF to a file, without running the rest of the pipeline. Useful for grabbing a snapshot
of the source data to inspect or to reuse while iterating on `generate-query` output, without
hitting the remote endpoint again.

```bash
fairmapper extract -r <fdp-endpoint> -o <output-file>
```

| Option           | Required | Description                                       |
|------------------|----------|----------------------------------------------------|
| `-r`, `--rdf`    | yes      | The FDP endpoint URI to extract RDF from.          |
| `-o`, `--output` | yes      | File to write the extracted RDF (Turtle) to.       |

### `generate-query`

Generates the SPARQL `SELECT` query that the transform step (step 3 in above pipeline) would use for a given table,
based on its EMX2 metadata and column `semantics`, without running a full harvest.

```bash
fairmapper generate-query <schema> <table> [-o <output-file>]
```

| Parameter / option | Required | Description                                                                          |
|--------------------|----------|--------------------------------------------------------------------------------------|
| `<schema>`         | yes      | Name of the MOLGENIS schema that contains the table.                                 |
| `<table>`          | yes      | Name of the table to generate the query for.                                         |
| `-o`, `--output`   | no       | File to write the generated query to. The query is always printed to stdout as well. |

### Suggested debugging workflow

1. Run `generate-query` for the table(s) you're working on and inspect the generated SPARQL. If a
   column isn't mapped the way you expect, check that column's `semantics` annotation in the
   schema.
2. Load `extracted.ttl` (or `preprocessed.ttl`) from a previous `harvest` run with `output` set - or the output of
   a standalone `extract` run - into a SPARQL tool (e.g. the
   [SPARQLbook](https://marketplace.visualstudio.com/items?itemName=Zazuko.sparql-notebook) VS
   Code extension) and try out the query from step 1 against it interactively. This lets you
   iterate on schema/semantics changes without re-running the extract step against the remote
   endpoint each time.
3. Run `harvest` with `output` set and without an `upload` step first, to inspect
   `transformed.zip` and `postprocessed.zip` and confirm the data looks correct before actually
   uploading it.
4. Once satisfied, add the `upload` step and re-run `harvest` to import the data into the schema.
