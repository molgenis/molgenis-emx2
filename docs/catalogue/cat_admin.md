# Information for system administrator of catalogue

## To initialize catalogue

1. Import the metadata schema. It is file molgenis.csv that can be found here: [https://github.com/molgenis/molgenis-emx2/tree/master/data/datacatalogue](https://github.com/molgenis/molgenis-emx2/tree/master/data/datacatalogue)
1. Optionally, also import ontologies etc from the data folders there. You can create zip file of one of the folders to
   upload in batch.
   (todo: define what data folder to use in what case)

## Updating existing schema

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
| 1.2 => 1.3 | This change fixes a few variable descriptions. Just upload molgenis.csv to update.|
| 1.3 => 1.4 | This change adds Models.releases refback. Just upload molgenis.csv to update.|
| 1.4 => 1.5 | Added Counts table, and linked via RWEresources.counts. Just upload molgenis.csv to update.|
| 1.5 => 1.6 | The following data items were added: Databanks.dateEstablished, Databanks.refresh, Datasources.studies, Studies.type, Studies.dataExtractionDate, Studies.CDM, Studies.contactName
| | Deleted Models.datasources, Models.databanks. Moved models.releases to heading 'contents'. Just upload molgenis.csv to update.|
| 1.6 => 1.7 | Added CollectionEvents.standardizedTools, CollectionEvents.standardizedToolsOther, Cohorts.contactEmail, StandardizedTools (OntologyTerm table), Datasources.qualityOfLifeOther. Moved Datasources.studies to RWEresources.studies. Deleted RWEresources.standardVocabularies. Download datamodel and data and replace molgenis.csv or molgenis sheet (in xlsx) with the newest version and reupload data in freshly made schema. |
| 1.7 => 1.8 | ColumnType of CollectionEvents.standardizedTools changed to ref_array. Unless bugfix #711 Download datamodel and data and replace molgenis.csv or molgenis sheet (in xlsx) with the newest version and reupload data in freshly made schema. |
| 1.8 => 1.9 | fix: Institution.typeOther should be text. Just upload molgenis.csv to update. |
| 1.9=> 1.10 | feat: add Database.populationSize(captured,active), RWEresources.approvalForPublication, Dispensing subterms. Just upload molgenis.csv to update. |
| 1.10=> 1.11 | Add VariableMappings.fromVariablesInOtherTables. Just upload molgenis.csv to update. |
| 1.11 => 2.0 | Delete AllVariables and split tables Variables, RepeatedVariables and VariableValues in SourceVariables and TargetVariables, RepeatedSourceVariables and RepeatedTargetVariables, SourceVariableValues and TargetVariableValues. |
| | Split table Tables in SourceTables and TargetTables. |
| | Split table Releases in SourceDataDictionaries and TargetDataDictionaries and adapt references from tables TableMappings and VariableMappings accordingly. |
| 2.0 => 2.1 | Add Cohorts.designDescription and Cohorts.designSchematic. Change ColumnType of Resources.designPaper to ref, referring to table Publications. |
| | Split Cohorts.dataAccessConditions into dataAccessConditions and dataUseConditions, add Cohorts.dataAccessFee. |
| 2.1 => 2.2 | ColumnType ontology and ontology_array now automatically instantiate tables of ontology type with tablename that is under refTable. These tables are not otherwise explicitly defined in the datamodel anymore. Download whole database, replace molgenis.csv and reupload in new Schema. |
| 2.2 => 2.3 | Alter division of AllVariables, Variables and RepeatedVariables (for Sources and Targets). Download whole database, replace molgenis.csv and reupload in new Schema. |
| 2.3 => 2.4 | Ontologies are placed in schema 'CatalogueOntologies' next to the catalogue itself. molgenis.csv is updated with an extra column 'refSchema' and 'columnType' ontology and ontology_array are refering to schema 'CatalogueOntologies'. It is important that 'refSchema' refers to CatalogueOntologies and this schema needs to exist before uploading molgenis.csv (or alter the ontology schema on the server and within molgenis.csv) |
| 2.4 => 2.5 | CollectionEvents.coreVariables, Subcohorts.comorbidity, Subcohorts.counts. Add table SubcohortCounts. Download whole database, replace molgenis.csv and reupload in new Schema. | |
| 2.5 => 2.6 | Edits to table SubcohortCounts: delete SubcohortCounts.year, SubcohortCounts.ageband, SubcohortCounts.gender, SubcohortCounts.N. Add SubcohortCounts.ageGroup, SubcohortCounts.N total, SubcohortCounts.N female, SubcohortCounts.N male. Download whole database, replace molgenis.csv and reupload in new Schema. | |
