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
| 0 => 1 | This change only adds column 'notes' to table 'Variables'. Just upload molgenis.csv to update.|  
| 1 => 1.1 | Download data in either Excel or zip and change molgenis sheet or molgenis.csv to version 1.1. |
| |In table 'Contacts' the key column 'name' was changed to two key columns: 'firstName' and 'surname'. |
| | In table 'Contributions' the column 'contact' has to be split into 'contact.firstname' and 'contact.surname'. |
| | In table 'Resources' column 'contacts' was deleted. Contacts are now refered to via contributions. |
| | In table 'Contacts' columns 'title', 'prefix', 'intials' were added. |
| | In table 'Resources' change column names: 'homepage' => 'website', 'publication' => 'designPaper', 'otherPublications' => 'publications' |
| | In table 'Cohorts' change column names: 'noParticipants' => 'numberOfParticipants', 'noParticipantsWithSamples' => 'numberOfParticipantsWithSamples' |
| | In table 'CollectionEvents' columns 'startMonth' and 'endMonth' were added. The columnType of columns 'startYear' and 'startMonth' were changed from int to ref. Add a table that extend OntologyTerms named 'Years' including relevant years. |
| 1.1 => 1.2 | This change adds a column 'type' of columnType ref to table 'Datasources'. Just upload molgenis.csv to update.| 



