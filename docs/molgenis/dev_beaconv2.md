# Beacon v2

The Beacon Project is developed under a Global Alliance for Genomics and Health ([GA4GH](https://www.ga4gh.org/))
Initiative for the federated discovery of genomic and phenoclinic data in biomedical research and clinical applications
by aggregating worldwide genomics dataset under one umbrella.
The version 2 (v2) of the Beacon protocol has been accepted as GA4GH standard in Spring 2022.
Beacon project home page can be found [here](https://beacon-project.io/) and the source
code [here](https://github.com/ga4gh-beacon/beacon-v2/).
Beacon v2 is available as an API in MOLGENIS EMX2.

Loading a database with a beacon profile will make the Beacon API available, coming in two flavours:

- [Beacon V2 specification](https://github.com/ga4gh-beacon/beacon-v2/) served at `<server>/<database>/api/beacon`
- [Beacon VP (EJPRD) specification](https://github.com/ejp-rd-vp/vp-api-specs/tree/v4.0_spec) served
  at `<server>/<database>/api/beacon_vp`

### Setup

#### Create a database

The easiest way to enable Beacon v2 in MOLGENIS EMX2 is by choosing a Beacon data template for your database. More
information about how to create a database is found [here](use_database.md)
This will add a number of tables that define the content of your Beacon v2, for
example [Analyses](https://github.com/molgenis/molgenis-emx2/blob/master/data/fairdatahub/beaconv2/demodata/Analyses.csv)
and [Biosamples](https://github.com/molgenis/molgenis-emx2/blob/master/data/fairdatahub/beaconv2/demodata/Biosamples.csv).
Using the 'FAIR_DATA_HUB' template including the example data will result in an instantly working Beacon v2 API.
The example data can be used as a reference on how to enter data into the system, but can be safely removed or replaced.

The following data templates include a Beacon profile:

- BeaconV2
- FAIRDataHub <More info about these profiles?>
- GDI

After setup, the API is available at `<server>/<database>/api/beacon`.
For instance, if your MOLGENIS runs at `https://emx2.test.molgenis.org` and your database name is `fdp` the Beacon v2
API is located at `https://emx2.test.molgenis.org/fdp/api/beacon`.

#### Endpoint metadata

All beacon-compliant templates include an *Endpoint table*. This table contains the organizational data for the
endpoints displayed in your beacon's informational endpoints. You can edit this data using
the [Table Explorer](use_quickstart.md#7-view-tables-data).

#### Load data

Lorem ipsum

### Endpoints

Starting from the root of the API, `<server>/<database>/api/beacon`, a number of endpoints are available.
Essentially, there are two types of endpoints:

- Endpoints part of the Beacon v2 [framework](https://docs.genomebeacons.org/framework/), such
  as `<server>/api/beacon/map`, and
- Endpoints part of the Beacon v2 [models](https://docs.genomebeacons.org/models/), such
  as `<server>/api/beacon/individuals`.

Framework endpoints for which GET requests are implemented are:

- `/`, `/info` and `/service-info` return Beacon metadata described in the GA4GH Service Info schema.
- `/configuration` returns configuration aspects and the definition of the entry types.
- `/map` returns a map (like a web sitemap) of the different endpoints implemented in this Beacon instance.
- `/entry_types` returns the section of the configuration that describes the entry types in this Beacon.

Model endpoints for which record-level GET and POST requests are implemented are:

- `/analyses` for entry type [Analyses](https://docs.genomebeacons.org/schemas-md/analyses_defaultSchema/). Data is
  retrieved from the _Analyses_ table.
- `/biosamples` for entry type [Biosamples](https://docs.genomebeacons.org/schemas-md/biosamples_defaultSchema/). Data
  is retrieved from the _Biosamples_ table.
- `/cohorts` for entry type [Cohorts](https://docs.genomebeacons.org/schemas-md/cohorts_defaultSchema/). Data is
  retrieved from the _Cohorts_ table.
- `/datasets` for entry type [Datasets](https://docs.genomebeacons.org/schemas-md/datasets_defaultSchema/). Returns the
  names and timestamps of available database schemas.
- `/g_variants` for entry
  type [Genomic Variations](https://docs.genomebeacons.org/schemas-md/genomicVariations_defaultSchema/). Data is
  retrieved from the _GenomicVariations_ table.
- `/runs` for entry type [Runs](https://docs.genomebeacons.org/schemas-md/runs_defaultSchema/). Data is retrieved from
  the _Runs_ table.
- `/individuals` for entry type [Individuals](https://docs.genomebeacons.org/schemas-md/individuals_defaultSchema/).
  Data is retrieved from the _Individuals_ table.
- `/filtering_terms` returns a list of the filtering terms accepted by that Beacon instance.

GET request are record requests by default and therefor viewer permission on the data is needed to perform the queries.
For post requests a

### Permissions

By default, a new database will have VIEWER permission for all users including anonymous requests. How to set up
permissions for a database is found [here](use_permissions.md).

Beacon offers 3 different response types

- Record (complete response with the complete beacon model)
- Count (count the numbers occurrences for a particular model endpoint)
- Boolean (true/false, does the data exist in a given endpoint, given certain filters)

|            | Record | Count | Boolean |
|------------|-----|-------|---------|
| Viewer     | ✅   | ✅     | ✅       |
| Aggregator | ❌   | ✅       | ✅       |
| Count      | ❌   | ✅       | ✅       |
| Range      | ❌   | ✅       | ✅       |
| Exists     | ❌   | ❌      | ✅         |

### Queries

The Beacon v2 framework allows for a range of potential queries and filters.
The following ones are currently implemented.

#### ID queries

Simple identifier-based queries are accepted via GET on the endpoints for Analyses, Biosamples, Cohorts, Individuals and
Runs.
The identifier can be supplied in the URL path as follows:
`<server>/<database>/api/beacon/analyses/<id>`
For instance:

- `<server>/<database>/api/beacon/analyses/A01`
- `<server>/<database>/api/beacon/biosamples/Sample0001`
- `<server>/<database>/api/beacon/cohorts/Cohort0001`
- `<server>/<database>/api/beacon/individuals/Ind001`
- `<server>/<database>/api/beacon/runs/SRR10903401`

It is also possible to search a model endpoint using an identifier of another entity type.
For example, to obtain all the runs for individual 'Ind001':
`<server>/<database>/api/beacon/individuals/Ind001/runs`

List of available links:

- `/analyses/<analysis-id>`
- `/analyses/<analysis-id>/g_variants`
- `/biosamples/<sample-id>`
- `/biosamples/<sample-id>/analyses`
- `/biosamples/<sample-id>/g_variants`
- `/biosamples/<sample-id>/runs`
- `/individuals/<individual-id>`
- `/individuals/<individual-id>/analyses`
- `/individuals/<individual-id>/biosamples`
- `/individuals/<individual-id>/g_variants`
- `/individuals/<individual-id>/runs`
- `/runs/<run-id>`
- `/runs/<run-id>/analyses`
- `/runs/<run-id>/g_variants`

#### Genomic queries

On genomic variation, a number of different genomic queries are accepted via GET on the `/g_variants` endpoint.

- Sequence
  query, [example](https://vkgl-emx2.molgeniscloud.org/api/beacon/g_variants?start=32936732&referenceName=13&referenceBases=G&alternateBases=C)
- Bracket
  query, [example](https://vkgl-emx2.molgeniscloud.org/api/beacon/g_variants?start=2347952&end=2547955&referenceName=20)
- Range
  query, [example](https://vkgl-emx2.molgeniscloud.org/api/beacon/g_variants?start=32953990,32953999&end=32954003,32954015&referenceName=13)
- Gene query, [example](https://vkgl-emx2.molgeniscloud.org/api/beacon/g_variants?geneId=TERC)

### Semantics

All tables and columns of the Beacon
v2 [EMX2 model](https://github.com/molgenis/molgenis-emx2/blob/master/data/fairdatahub/beaconv2/molgenis.csv) are coded
with ontologies.
References to predefined lookup lists, such as _platformModel_ in _Runs_, point to OntologyTables such
as [SequencingInstrumentModels](https://github.com/molgenis/molgenis-emx2/blob/master/data/fairdatahub/ontologies/SequencingInstrumentModels.csv).
The complete list of ontology lookups can be
found [here](https://github.com/molgenis/molgenis-emx2/tree/master/data/fairdatahub/ontologies).
These semantics help to disambiguate terms and facilitate interoperability, for instance by exporting the data via the
RDF API.

### Compliance

The MOLGENIS EMX2 Beacon v2 API was developed to be fully compliance
with [beacon-verifier](https://crates.io/crates/beacon-verifier) version 0.3.2.
Note that not all variables from the Beacon v2 models have been implemented.
As per Beacon design philosophy, the variables and filter options of this implementation will grow and adapt to
community specific needs.

The API has also been tested with the latest beacon-verifier release, version 0.3.3.
This version requires sub-levels for the base entry types to be implemented.
The following sub-levels are currently missing but are planned to be implemented:

# Beacon VP

#### Count queries

Count queries are accepted as POST requests on the `/individuals` endpoint.
These requests should conform to specifications available [here](https://github.com/ejp-rd-vp/vp-api-specs).
The response contains the number of individuals that match the supplied filters.

