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
information about how to create a database is found [here](use_database.md).
This will add a number of tables that define the content of your Beacon v2, for
example [Analyses](https://github.com/molgenis/molgenis-emx2/blob/master/data/fairdatahub/beaconv2/demodata/Analyses.csv)
and [Biosamples](https://github.com/molgenis/molgenis-emx2/blob/master/data/fairdatahub/beaconv2/demodata/Biosamples.csv).
Using the 'PATIENT_REGISTRY' template including the example data will result in an instantly working Beacon v2 API.
The example data can be used as a reference on how to enter data into the system, but can be safely removed or replaced.

After setup, the API is available at `<server>/<database>/api/beacon`.
For instance, if your MOLGENIS runs at `https://emx2.test.molgenis.org` and your database name is `fdp` the Beacon v2
API is located at `https://emx2.test.molgenis.org/fdp/api/beacon`.

#### Endpoint metadata

All beacon-compliant templates include an **Endpoint table**. This table contains the organizational data for the
endpoints displayed in your beacon's informational endpoints. You can edit this data using
the [Table Explorer](use_quickstart.md#7-view-tables-data).

#### Load data

There are several ways to [load data](use_quickstart?id=_8-enter-data) in MOLGENIS EMX2. You can download the database
model in csv files (inside a zip container), or xls format. Edit these files and upload them again as
explained [here](http://localhost:8080/apps/docs/#/molgenis/use_quickstart?id=_8-enter-data)

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

### Permissions

By default, a new database with a Beacon profile will have **VIEWER** permission for all users including anonymous
requests. How to set up
permissions for a database is found [here](use_permissions.md).

Request are **Record** requests by default and therefor **VIEWER** permission on the data is needed to perform the
queries.
For GET request this can be altered via de requestedGranularity parameter:

`<server>/<database>/api/beacon/individuals/requestedsGranulariy=count`

For post request via query.requestedGranularity:

```{
"query": {
  "requestedGranularity": "count"
}
```

Beacon offers 3 different response types

- **Record** (complete response with the complete beacon model)
- **Count** (count the numbers occurrences for a particular model endpoint)
- **Boolean** (true/false, does the data exist in a given endpoint, given certain filters)

The following table shows how the MOLGENIS permissions relate to the Beacon response types:

|                | Record | Count                 | Boolean |
|----------------|--------|-----------------------|---------|
| **Viewer**     | ✅      | ✅                     | ✅       |
| **Count**      | ❌      | ✅ (exact counts)      | ✅       |
| **Aggregator** | ❌      | ✅ (exact counts > 10) | ✅       |
| **Range**      | ❌      | ✅ (step-size 10)      | ✅       |
| **Exists**     | ❌      | ❌                     | ✅       |

We have added the **Range** permission to handle inexact counts with a step size of 10 (e.g., 10, 20, 30, ..., 120, 130,
etc.). The **Aggregator** permission wil hide counts lower than 10, while the **Count** permission will always return
the exact count of records of a given request.

For example, if the exact count for a given request is **74**, a user with **Range** permission will see a count of
**80**, along with an additional description explaining within what range the exact count falls.

```"response": {
  "response": {
     "resultSets": [
      {
         "id": ...,
         "type": "dataset", 
         "exists": true,
         "resultCount": 80,
         "info": {
            "resultCountDescription": {
               "minRange": 71,
               "maxRange": 80
            }
         }      
      }
    ]
  }
```

### Queries

The Beacon v2 framework allows for a range of potential queries and filters. the `/filtering_terms` endpoint will
return a list of filtering terms accepted by your Beacon instance with the scope for which entry type the filtering term
is applicable. How these filter can be used is
explained [here](https://docs.genomebeacons.org/filters/#using-filters-in-queries)

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

On genomic variation, a number of different genomic queries are accepted via GET and POST requests on the `/g_variants`
endpoint.

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
with [beacon-verifier](https://github.com/EGA-archive/beacon-verifier-v2) version v2. After setting up your beacon
api, this verifier can be used to check the validation of your beacon endpoint.

Note that not all variables from the Beacon v2 models have been implemented.
As per Beacon design philosophy, the variables and filter options of this implementation will grow and adapt to
community specific needs.

### Beacon VP

The Beacon VP spec is exactly modeled after the specification Virtual Platform EJP-RD specification v4
found [here](https://github.com/ejp-rd-vp/vp-api-specs/tree/v4.0_spec)

## Performance

To evaluate the real-world performance of our Beacon endpoint, we used the publicly available VKGL dataset, which
contains over 200K genomic variants and loaded this on a MOLGENIS instance running on an Azure virtual machine. We
expanded the dataset to a total of 5.4 million records by duplicating the variants. This allowed us to test various
queries and assess how performance scales as the dataset size increases.

### Azure VM
#### Dataset
[VKGL_public_consensus_apr](https://vkgl-emx2.molgeniscloud.org/)

#### Software
MOLGENIS version: v11.2.1.\
Database version: v21.\
PostgreSQL version: v14.10.

#### Hardware
[Azure B2ms](https://learn.microsoft.com/nl-nl/azure/virtual-machines/sizes-b-series-burstable) (2vCPU, 8GB memory)

#### Query performance
Median value of 9 request
- No parameters, `/g_variants`
- Gene id query, `/g_variants?geneId=COL3A1`
- Range query, `/g_variants?start=32953990,32953999&end=32954003,32954015&referenceName=13`


| nRecords | No params | geneId | range |
|----------|-----------|--------|-------|
| 200K     | 31ms      | 34ms   | 35ms  |
| 1M       | 36ms      | 30ms   | 38ms  |
| 2.0M     | 89ms      | 78ms   | 83ms  |
| 5.4M     | 1871ms    | 830ms  | 794ms |

#### Total request time
Median value of 9 request

| nRecords | No params | geneId | range |
|----------|-----------|--------|-------|
| 200K     | 129ms     | 152ms  | 138ms |
| 1M       | 143ms     | 131ms  | 154ms |
| 2.0M     | 170ms     | 166ms  | 200ms |
| 5.4M     | 2012ms    | 1023ms | 918ms |

### Local
To gain more insight on the influence of hardware, we also benchmarked performance on a local machine.
#### Hardware
MacBook PRO (M1 PRO, 16GB memory)
#### Query performance (median value of 9 request)

| nRecords | No params | geneId | range |
|----------|-----------|--------|-------|
| 200K     | 27ms      | 39ms   | 35ms  |
| 1M       | 54ms      | 89ms   | 75ms  |
| 2.0M     | 148ms     | 200ms  | 188ms |
| 5.4M     | 341ms     | 424ms  | 400ms |

#### Total request time (median value of 9 request)

| nRecords | No params | geneId | range |
|----------|-----------|--------|-------|
| 200K     | 141ms     | 143ms  | 176ms |
| 1M       | 164ms     | 181ms  | 160ms |
| 2.0M     | 218ms     | 220ms  | 229ms |
| 5.4M     | 472ms     | 527ms  | 504ms |
