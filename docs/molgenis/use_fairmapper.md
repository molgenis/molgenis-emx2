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
tables: [Catalogues, Collections, Organisations]
# output: ./fairmapper-output   # uncomment to dump the result of every stage
emx2:
  endpoint: https://emx2.example.org
  token: your-token
steps:
  - extract:
      url: https://fdp.example.org
      crawl: fdp
  - preprocessing: [temporal, typical-age, stage-csvw]
  - transform
  - postprocessing:
      - coalesce-field: { table: Collections, field: id, derive-from: [acronym, name], strict: false }
      - coalesce-field: { table: Catalogues, field: id, derive-from: [acronym, name], strict: false }
      - coalesce-field: { table: Organisations, field: id, derive-from: [organisation name], strict: false }
      - resolve-static: { table: Collections, field: type, value: http://semanticscience.org/resource/SIO_001067 }
      - resolve-static: { table: Catalogues, field: type, value: http://semanticscience.org/resource/SIO_001067 }
      - resolve-ontologies
      - resolve-missing-pk
      - drop-missing-pk: { tables: [Organisations] }
  # - upload   # uncomment to upload; leave out for a dry run
```

The same file, with comments, is in the repository at
`backend/molgenis-emx2-fairmapper/examples/stage-fdp.yml`.

| Field            | Required | Description                                                                                                  |
|------------------|----------|--------------------------------------------------------------------------------------------------------------|
| `schema`         | yes      | Name of the schema (on the target EMX2 instance) that contains the target tables. Data is uploaded into it. |
| `tables`         | yes      | List of table names (in that schema) to harvest.                                                            |
| `output`         | no       | Directory to write intermediate results to. If omitted, nothing is dumped to disk.                          |
| `emx2.endpoint`  | yes      | Base URL of the target EMX2 instance.                                                                       |
| `emx2.token`     | yes      | API token for the target EMX2 instance.                                                                     |
| `steps`          | yes      | The steps of the harvest, see below.                                                                        |

Each entry under `steps` names one step. A step can be listed at most once, and the order in which
they are listed doesn't matter: they always run in the order extract, preprocessing, transform,
postprocessing, upload. Steps, pre-processors and post-processors without options can be written as
a bare name (`- transform`).

| Step             | Required | Options                                                                                                   |
|------------------|----------|-----------------------------------------------------------------------------------------------------------|
| `extract`        | yes      | `url` (required): where to fetch the RDF from. `crawl`: links to follow from there, see below. `strict` (default `true`): stop the harvest when a resource can't be fetched. |
| `preprocessing`  | no       | List of pre-processors, run in the listed order.                                                          |
| `transform`      | yes      | None.                                                                                                     |
| `postprocessing` | no       | List of post-processors, run in the listed order. The same post-processor can be listed more than once. |
| `upload`         | no       | None. Leave it out for a dry run.                                                                         |

`crawl` is either `fdp` (follow an FDP's catalogs, datasets, distributions and their download URLs)
or a list of crawl steps, each with a `name` (used in logging) and the `predicate` to follow. A
predicate is a full IRI or a prefixed name using the predefined namespace prefixes (see
[Linked data](semantics.md)):

```yaml
crawl:
  - { name: catalog, predicate: fdp-o:metadataCatalog }
  - { name: dataset, predicate: dcat:dataset }
```

Without `crawl`, only the document at `url` is fetched.

Pre-processors (none take options):

| Name          | What it does                                                                                       |
|---------------|----------------------------------------------------------------------------------------------------|
| `temporal`    | Adds `dcat:startDate`/`dcat:endDate` years to datasets and catalogs from their `dcterms:temporal` interval. |
| `typical-age` | Normalises abbreviated Health-DCAT-AP `minTypicalAge`/`maxTypicalAge` predicates.                   |
| `stage-csvw`  | Links the CSVW tables of a dataset's distributions to the dataset.                                  |

Post-processors:

| Name                 | Options                                                        | What it does                                                                                        |
|----------------------|----------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| `coalesce-field`     | `table`, `field`, `derive-from`, `strict` (default `true`)     | Sets `field` to the first non-empty of the `derive-from` fields. When `strict`, a row with none of them fails the harvest. |
| `resolve-static`     | `table`, `field`, `value`                                      | Sets `field` to `value` on every row of `table`.                                                    |
| `resolve-ontologies` | none                                                           | Replaces ontology term IRIs with their names in the target schema.                                  |
| `resolve-missing-pk` | none                                                           | Fills in references whose primary key wasn't known right after the transform step.                 |
| `drop-missing-pk`    | `tables`                                                       | Drops rows in `tables` that still have an incomplete primary key.                                   |

### `extract`

Runs just the extract step (step 1 in above pipeline) and writes the resulting RDF to a file,
without running the rest of the pipeline. Useful for grabbing a snapshot of the source data to
inspect or to reuse while iterating on `generate-query` output, without hitting the remote endpoint
again.

```bash
fairmapper extract -r <fdp-endpoint> -o <output-file>
fairmapper extract -c <pipeline-file> -o <output-file>
```

With `-r`, the FDP at the given endpoint is crawled. With `-c`, the `extract` step of a
[pipeline file](#the-pipeline-file) is run, with its `url`, `crawl` and `strict` options; this is
useful for trying out your own crawl steps. The whole pipeline file is checked, so it has to be a
valid pipeline file, not just an `extract` step.

| Option           | Required        | Description                                                  |
|------------------|-----------------|--------------------------------------------------------------|
| `-r`, `--rdf`    | one of `-r`/`-c` | The FDP endpoint URI to extract RDF from.                    |
| `-c`, `--config` | one of `-r`/`-c` | The pipeline file whose `extract` step to run.              |
| `-o`, `--output` | yes             | File to write the extracted RDF (Turtle) to.                 |

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
