# FAIR Data Point
FAIR Data Point (FDP, [fairdatapoint.org](https://www.fairdatapoint.org)) is a metadata service that provides access to metadata following the FAIR principles via a REST API.
Owners/publishers can use it to expose metadata of their digital objects, while consumers can discover information about offered digital objects.
The FDP uses the W3C's [Data Catalog Vocabulaire (DCAT) version 2 model](https://www.w3.org/TR/vocab-dcat-2/) as the basis for its metadata content.

FDP is implemented in MOLGENIS EMX2 in compliance with the latest v1.1 specification of FAIR Data Point available at [specs.fairdatapoint.org](https://specs.fairdatapoint.org/).
For MOLGENIS EMX1, the FDP implementation guide can be found [here](https://molgenis.gitbooks.io/molgenis/content/guide-fair.html).

### Setup and configuration
The easiest way to enable FDP in MOLGENIS EMX2 is by choosing 'FAIR_DATA_HUB' as a template for your database.
This will add three tables that will define the content of your FAIR Data Point:
[Catalog](https://github.com/molgenis/molgenis-emx2/blob/master/data/fairdatahub/fairdatapoint/demodata/Catalog.csv), [Dataset](https://github.com/molgenis/molgenis-emx2/blob/master/data/fairdatahub/fairdatapoint/demodata/Dataset.csv) and [Distribution](https://github.com/molgenis/molgenis-emx2/blob/master/data/fairdatahub/fairdatapoint/demodata/Distribution.csv).
Loading the 'FAIR_DATA_HUB' template including the example data will result in a fully operational FAIR Data Point.
The example data can be used as a reference on how to enter data into the system.
It can be safely removed or replaced.

Before the FDP can be used, log in as the `admin` user and visit your FDP at `<server>/api/fdp`.
This will add a database setting called _FAIR Data Point root metadata_ to the schema that enables your FDP.
This setting contains a piece of RDF/TTL that is part of your FDP root endpoint which is fully customizable.
Here, you can change the information about your FDP such as title, publisher, contact point and language.
To make changes, go to your FDP-enabling schema, go to Settings, Advanced settings, and click the Edit button for  _FAIR Data Point root metadata_.

After setting up these tables, this is how its contents are translated to the FDP structure:
- Each Dataset contains a reference to one or multiple Catalogs via the `belongsToCatalog` field.
  - These references can also be viewed in the referred-to Catalogs via the `dataset` field.
- Each Distribution contains a reference to one or multiple Datasets via the `belongsToDataset` field.
  - These references can also be viewed in the referred-to Datasets via the `distribution` field.

### API structure and traversal
Adding rows to these tables and setting the right permissions will result in a FAIR Data Point endpoint at `<server>/api/fdp` with the following traversable layers:
- The root endpoint, containing FDP catalogs
- Catalog, containing FDP datasets (you can specify its contents via the Catalog table)
- Dataset, containing FDP distributions (you can specify its contents via the Dataset table)
- Distribution, containing either 
  - Download URL of a Table for a particular format (13 currently available)
  - File metadata including its format (selected from EDAM ontology)
- A download URL that points to a particular API (e.g. CSV, JSON, ZIP, RDF, etc.)

A full example of traversal from root to data:
- An FDP root `<server>/api/fdp`, containing
- a catalog `<server>/api/fdp/catalog/fairdemo/catalogId01`, containing
- a dataset `<server>/api/fdp/dataset/fairdemo/datasetId01`, available as 
- a distribution `<server>/api/fdp/distribution/fairdemo/Analyses/rdf-ttl`, downloadable at
- a particular location `<server>/api/rdf/Analyses?format=ttl`.
