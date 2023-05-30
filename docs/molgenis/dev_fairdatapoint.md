# FAIR Data Point
FAIR Data Point (FDP, [fairdatapoint.org](https://www.fairdatapoint.org)) is a metadata service that provides access to metadata following the FAIR principles via a REST API.
Owners/publishers can use it to expose metadata of their digital objects, while consumers can discover information about offered digital objects.
The FDP uses the W3C's [Data Catalog Vocabulaire (DCAT) version 2 model](https://www.w3.org/TR/vocab-dcat-2/) as the basis for its metadata content.

FDP is implemented in MOLGENIS EMX2 in compliance with the latest v1.1 specification of FAIR Data Point available at [specs.fairdatapoint.org](https://specs.fairdatapoint.org/).
For MOLGENIS EMX1, the FDP implementation guide can be found [here](https://molgenis.gitbooks.io/molgenis/content/guide-fair.html).

### Setup and configuration
The easiest way to enable FDP in MOLGENIS EMX2 is by choosing 'FAIR_DATA_HUB' as a template for your database.
This will add two tables that will define the content of your FAIR Data Point:
[FDP_Catalog](https://github.com/molgenis/molgenis-emx2/blob/master/data/fairdatahub/fairdatapoint/demodata/FDP_Catalog.csv) and [FDP_Dataset](https://github.com/molgenis/molgenis-emx2/blob/master/data/fairdatahub/fairdatapoint/demodata/FDP_Dataset.csv).
Loading the 'FAIR_DATA_HUB' template including the example data will result in a fully operational FAIR Data Point.
The example data can be used as a reference on how to enter data into the system, but can be safely removed or replaced.

After setting up these tables, this is how its contents are translated to the FDP structure:
- Each row in FDP_Catalog contains a reference to one or multiple FDP_Datasets via the `dataset` field.
- Each FDP_Dataset refers to exactly one table name within the database schema via the `distribution` field.
- The data for that table is to be distributed via the FAIR Data Point.

### API structure and traversal
Adding rows to these tables and setting the right permissions will result in a FAIR Data Point endpoint at `<server>/api/fdp` with the following traversable layers:
- The root endpoint, containing FDP catalogs
- Catalog, containing FDP datasets (you can specify its contents via the FDP_Catalog table)
- Dataset, containing FDP distributions (you can specify its contents via the FDP_Dataset table)
- Distribution, containing download URL for a particular format (13 currently available)
- A download URL that points to a particular API (e.g. CSV, JSON, ZIP, RDF, etc.)

A full example of traversal from root to data:
- An FDP root `<server>/api/fdp`, containing
- a catalog `<server>/api/fdp/catalog/fairdemo/catalogId01`, containing
- a dataset `<server>/api/fdp/dataset/fairdemo/datasetId01`, available as 
- a distribution `<server>/api/fdp/distribution/fairdemo/Analyses/rdf-ttl`, downloadable at
- a particular location `<server>/api/rdf/Analyses?format=ttl`.
