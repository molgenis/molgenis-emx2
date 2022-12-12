# FAIR Data Point

FAIR Data Point (FDP, [fairdatapoint.org](https://www.fairdatapoint.org)) is implemented in MOLGENIS EMX2 in compliance with the latest v1.1 specification of FAIR Data Point available at [specs.fairdatapoint.org](https://specs.fairdatapoint.org/).
For MOLGENIS EMX1, the FDP implementation guide can be found [here](https://molgenis.gitbooks.io/molgenis/content/guide-fair.html).


The easiest way to enable FDP in MOLGENIS EMX2 is by choosing 'FAIR_DATA_HUB' as a template for your database. This will add two tables that will define the content of your FAIR Data Point: [FDP_Catalog](https://github.com/molgenis/molgenis-emx2/blob/master/data/fairdatahub/fairdatapoint/demodata/FDP_Catalog.csv) and [FDP_Dataset](https://github.com/molgenis/molgenis-emx2/blob/master/data/fairdatahub/fairdatapoint/demodata/FDP_Dataset.csv). Loading the 'FAIR_DATA_HUB' template example data will result in a fully operational FAIR Data Point and can be used as a reference.

Each row in FDP_Catalog contains a reference to one or multiple FDP_Datasets via the `dataset` field. Each FDP_Dataset refers to exactly one table name within the database schema via the `distribution` field. The data for that table is to be distributed via the FAIR Data Point.

Adding rows to these tables and setting the right permissions will result in a FAIR Data Point endpoint at `<server>/api/fdp` with the following layers:
* The root endpoint, containing FDP catalogs
* Catalog, containing FDP datasets (you can specify its contents via the FDP_Catalog table)
* Dataset, containing FDP distributions (you can specify its contents via the FDP_Dataset table)
* Distribution, containing download URL for a particular format (13 currently available)
* A download URL that points to a particular API (e.g. CSV, JSON, ZIP, RDF, etc.)
