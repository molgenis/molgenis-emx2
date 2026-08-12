# What is the FAIR Mapper

The FAIR Mapper is a command-line tool that harvests RDF data from remote endpoints and loads it
into a MOLGENIS EMX2 schema as regular tabular data. It currently focuses on harvesting
**DCAT** (and Health-DCAT-AP) metadata, for example catalogues, datasets/collections and
organisations exposed by a [FAIR Data Point](https://www.fairdatapoint.org/) (FDP).

The goal is to let a catalogue-style EMX2 schema be populated automatically from external,
FAIR-published metadata, instead of having to import CSV/Excel files by hand.

## How does the pipeline work?

A harvest run is a pipeline of five steps. Each step's output can optionally be dumped to disk so
you can inspect (and debug) what happened at every stage.

1. **Extract** - fetch all RDF from the source. For an FDP endpoint this means: resolve the FDP's
   metadata catalog(s), then resolve every dataset in those catalogs, and load all resulting RDF
   into a temporary, in-memory RDF4J repository.
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
   post-processing steps implemented in Java, for example: deriving an `id` column from other
   columns, resolving ontology term URIs to their names in the target schema, resolving rows that
   are missing a primary key, and dropping rows that still have no usable primary key.
5. **Load** - the final table store is imported into the target schema and its tables using
   MOLGENIS EMX2's regular import task infrastructure (data only, existing schema structure is
   left untouched).

# How to use the FAIR Mapper

## Prerequisites: a target schema to harvest into

The FAIR Mapper only ever loads *data*, it never creates schemas, tables or columns for you. So
before you run a harvest, the target schema and its tables must already exist in the database
you're connecting to. It can be created by MOLGENIS EMX2 itself (e.g. through the UI's
[schema creation](use_database.md), by uploading a `molgenis.csv`/schema definition, or via the
GraphQL/REST admin API).

That target schema also needs to be *semantically annotated* so the FAIR Mapper knows how RDF
predicates map onto your columns:

* It is recommended, but not required, to annotate a table with a `semantics` value identifying
  the RDF class it represents, e.g. a `Collections` table annotated with `dcat:Dataset`. Without
  it, the query won't filter by RDF type explicitly, but any column marked `required` still
  becomes a mandatory triple pattern, so in practice a subject still has to carry that data to
  match. Annotating the table just makes the type filter explicit (and the query cheaper).
* Each column you want filled needs a `semantics` value identifying the RDF predicate that holds
  its value, e.g. a `title` column annotated with `dcterms:title`. Columns without a `semantics`
  value are skipped entirely, they're left out of the generated query and stay empty.

This is the same `semantics` mechanism used elsewhere in EMX2's RDF support (see
[Linked data](semantics.md) for the full reference), including the list of predefined namespace
prefixes (`dcat:`, `dcterms:`, `healthdcatap:`, ...) you can use instead of full IRIs.

## Build instructions

The FAIR Mapper lives in the `:backend:molgenis-emx2-fairmapper` Gradle module. Build a runnable,
all-in-one JAR with:

```bash
./gradlew :backend:molgenis-emx2-fairmapper:shadowJar
```

The resulting JAR is written to the module's `build/libs` directory, named
`fairmapper-<version>-cli.jar`.

## Run instructions

The FAIR Mapper is a [picocli](https://picocli.info/)-based CLI with the main class
`org.molgenis.emx2.fairmapper.cli.FairMapper`. Run it with:

```bash
java -jar backend/molgenis-emx2-fairmapper/build/libs/fairmapper-<version>-cli.jar <command> [options]
```

It connects to the database using the same environment variables as the rest of MOLGENIS EMX2
(`MOLGENIS_POSTGRES_URI`, `MOLGENIS_POSTGRES_USER`, `MOLGENIS_POSTGRES_PASS`), so make sure these
point at the Postgres instance that holds the target schema, and that the schema/tables you want
to harvest into already exist.

?>**Tip**: since the command gets long, it's convenient to define a shell alias, e.g.:
    ```bash
    alias fairmapper='java -jar /path/to/fairmapper-<version>-cli.jar'
    ```

### `harvest`

Runs the full harvesting pipeline described above: extract, pre-process, transform,
post-process and (optionally) load.

```bash
fairmapper harvest -r <fdp-endpoint> -s <schema> -t <table1,table2,...> [-o <output-dir>] [-l]
```

| Option           | Required | Description                                                                                                                                      |
|------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| `-r`, `--rdf`    | yes      | The FDP endpoint URI to harvest from.                                                                                                            |
| `-s`, `--schema` | yes      | Name of the MOLGENIS schema that contains the target tables.                                                                                     |
| `-t`, `--tables` | yes      | Comma-separated list of table names (in that schema) to harvest.                                                                                 |
| `-o`, `--output` | no       | Directory to write intermediate results to. If omitted, nothing is dumped to disk.                                                               |
| `-l`, `--load`   | no       | Flag. If set, the harvested data is actually imported into the schema. If omitted, the pipeline runs but nothing is loaded, useful for dry runs. |

When `-o` is given, a subdirectory `fairmapper-output-<harvest-id>` is created containing:

* `extracted.ttl` - the raw RDF extracted from the source, before any pre-processing.
* `preprocessed.ttl` - the RDF after pre-processing (only written if pre-processors are configured).
* `transformed.zip` - a CSV-in-ZIP export of the table store right after the transform step.
* `postprocessed.zip` - the same, after post-processing has run. This is what would be loaded into
  the schema when `-l` is set.

### `generate-query`

Generates the SPARQL `SELECT` query that the transform step would use for a given table, based on
its EMX2 metadata and column `semantics`, without running a full harvest.

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
2. Load `extracted.ttl` (or `preprocessed.ttl`) from a previous `harvest -o` run into a SPARQL
   tool (e.g. the [SPARQLbook](https://marketplace.visualstudio.com/items?itemName=Zazuko.sparql-notebook)
   VS Code extension) and try out the query from step 1 against it interactively. This lets you
   iterate on schema/semantics changes without re-running the extract step against the remote
   endpoint each time.
3. Run `harvest` with `-o` and without `-l` first, to inspect `transformed.zip` and
   `postprocessed.zip` and confirm the data looks correct before actually loading it.
4. Once satisfied, re-run `harvest` with `-l` to import the data into the schema.
