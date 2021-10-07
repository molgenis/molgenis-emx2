# Information for system administrator of catalogue

## To initialize catalogue

1. Import the metadata schema. It is file molgenis.csv that can be found here:
   https://github.com/molgenis/molgenis-emx2/tree/master/data/datacatalogue
1. Optionally, also import ontologies etc from the data folders there. You can create zip file of one of the folders to
   upload in batch.
   (todo: define what data folder to use in what case)

## Updating exing schema

Since 27 sep 2021 we have started to give catalogue schema a seperate version number. This can be found on first line of
molgenis.csv file (using description of special table 'Version'). Below summary of the changes and what the general
procedure is to update.

| version | update procedure |
|---------|------------------|
| 0 => 1 | This change only adds column 'notes' to table 'variables'. Just upload molgenis.csv to update.|

