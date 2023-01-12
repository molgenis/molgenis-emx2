# Data manager of a cohort or data source

## Data Catalogue

[MOLGENIS Data Catalogue](https://data-catalogue.molgeniscloud.org/catalogue/catalogue/#) provides a
framework to describe in detail: metadata of cohorts and of data sources; definitions of the variables collected in
cohorts and data sources; and mappings to common data models. Its purpose is to facilitate pooled data analysis of multiple cohorts
[Fortier et al, 2017](https://pubmed.ncbi.nlm.nih.gov/27272186/) and multi-data
source studies [Gini et al, 2020](https://pubmed.ncbi.nlm.nih.gov/32243569/).

- The metadata of cohorts include descriptive information such as contact details, name of the cohort, and high-level
  summary of contents and cohort design. The metadata of data sources, of the corresponding data banks and of the
  institutions that provide access to them, include descriptive information such as contact details, reason for
  existence of the data banks, the prompt for the records in the data bank, and lag time for updating and accessing data.
- The metadata of the source variables (also known as 'data dictionary') can be considered as a codebook or data
  dictionary of a cohort (e.g. ALSPAC) and of the tables which make up a data source's data bank(s) (e.g. the Danish
  Healthcare Registries).
- Similarly, the common data models (or 'target variables') can be considered the codebook for a network of institutions
  with access to cohorts or data sources (e.g. LifeCycle or ConcePTION)
- The mappings describe how source variables have been converted into target variables as a basis for integrated analysis.

### Data harmonisation

Each organisation with access to data (which may be a cohort, or a data source composed of one or more data banks)
harmonises their data according to the consortium’s protocols into a common data model (CDM) format which has been
centrally agreed upon. In some projects, data may be made available via DataSHIELD. In these cases each resource stores
the data locally in a [MOLGENIS Armadillo](/#/armadillo/) DataSHIELD server.

### Staging areas for uploads

The metadata of the cohort or of the data source are first uploaded into what are called "staging areas" of the Data 
Catalogue. Later on the metadata are transferred to production; use of a staging area allows for review before the 
metadata are entered in the live catalogue. 

You will need credentials to log in and upload metadata. 

LifeCycle, ATHLETE and LongITools use [*data-catalogue-staging*](https://data-catalogue-staging.molgeniscloud.org).  
ConcePTION uses [*conception-acc*](https://conception-acc.molgeniscloud.org).

When you log in, you will be able to see at least the following databases:

- <b> DataCatalogue</b>: The catalogue data, in which you can search for target variables to map to.
- <b>CatalogueOntologies</b>: This database contains the look-up list that you need for filling out some columns in the
  templates, e.g. format or unit. If you need to add anything to these look-up lists, contact us
  at [molgenis-support](mailto:molgenis-support@umcg.nl).
- <b>SharedStaging</b>: A communal staging area in which e.g. Contacts and Institutions are added and edited.
- <b>Your own database</b>: Use this to fill out cohort rich metadata and to upload the templates once you have filled them out.

![MOLGENIS databases](../img/cat_databases.png)

<sup>*Figure 1. Databases in the Data Catalogue staging area.*</sup>

### Fill out cohort rich metadata

Open your staging area, navigate to 'Tables' and open the table 'Cohorts'. Your cohort pid and name are already 
filled out. Click on the pencil sign next to this entry to start editing your cohort rich metadata by filling out 
the form. 'Subcohorts' and 'Collection events' should also be filled out through this route. You can fill them out 
in subsections inside the 'Cohorts' table.

### Define metadata of cohorts or data sources

This section explains how to submit the 'source variables' + 'mappings from source variables to target variables' into
the Data Catalogue. Expected users of this 'how to' are data managers within the institutions with access to cohorts or
data sources. This document assumes you have received login details for upload of your metadata. You can also watch
this [*instruction video*](https://www.youtube.com/watch?v=b_Ef_Uiw1gE&amp;ab_channel=MOLGENIS).

#### Define source variable metadata / source data dictionary

We use the [*SourceDictionary template*](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/SourceDictionary.xlsx)
to define variable metadata. The [*SourceDictionary
template*](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/SourceDictionary.xlsx) consists of multiple
sheets. Each sheet corresponds to a table in the Data Catalogue (Figure 1). The columns in the sheet correspond to
columns in the table concerned. This document describes how to fill out each of the sheets and their columns. A column
with an asterisk (\*) after its name is mandatory, i.e., it should contain values for the system to accept a data
upload. You can download this 
[*filled out example*](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/SourceDictionary_testCohort.xlsx) 
as a reference for filling out the template.

<sup>Note that there is no sheet for *AllVariables*. This table is a generic listing of all variables entered for
the cohort; it shows *Variables* and *RepeatedVariables* in one table. </sup>

It is good practice to try adding a few variables to the template first and see whether your upload succeeds. To
upload the metadata to the Data Catalogue see the section [Upload metadata](cat_cohort-data-manager.md#upload-metadata).

![MOLGENIS tables in cohort catalogue](../img/cat_tables-in-cohort-catalogue.png)

<sup>*Figure 2. Tables in a cohort's or DAP's database in the Data Catalogue. Note that not all tables are filled out 
via the templates, some are filled via an online form, see section 
[Fill out cohort rich metadata](cat_cohort-data-manager.md#fill-out-cohort-rich-metadata).*</sup>

#### *SourceDataDictionary* sheet

This sheet is used to fill out versions of your codebook.

| *Column name* | *Description* |
| --- | --- |
| resource \* | Fill out your cohort id |
| version \* | Version of the source data dictionary |

<sup>Table 1. Description of the columns that can be filled out for SourceDataDictionaries. * = mandatory</sup>

#### *SourceTables* sheet

The tables in a cohort or in the data banks of a data source are defined in the *SourceTables* sheet. Columns with an
asterisk (\*) after their name are mandatory.

| *Column name* | *Description* |
| --- | --- |
| dataDictionary.resource \* | Source data dictionary that this table belongs to |
| dataDictionary.version \* |  Version of the source data dictionary that this table belongs to |
| name \* | Unique table name |
| label | Table label |
| description | Table description |
| unitOfObservation | Defines what each record in this table describes |
| numberOfRows | Count of the number of records in this table |

<sup>Table 2. Description of the columns that can be filled out for SourceTables. * = mandatory</sup>

#### *SourceVariables* sheet

The variables of the tables specified in the *SourceTables* sheet are defined in the *SourceVariables* sheet.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| dataDictionary.resource \* | Source data dictionary that this variable belongs to | Fill out your cohort pid |
| dataDictionary.version \* |  Version of the data dictionary that this variable belongs to | Source data dictionaries must be predefined in the _SourceDataDictionaries_ sheet  |
| table \* | Table that contains the variable. | Tables must be predefined in the SourceTables sheet |
| name \* | Variable name, unique within a table | |
| label | Human readable variable label | |
| format | The data type of the variable | Find list to choose from in CatalogueOntologies |
| unit<sup>1</sup> | Unit in case of a continuous or integer format | Find list to choose from in CatalogueOntologies Units |
| description | Description of the variable | |
| exampleValues | Examples of values in a comma separated list | Makes your data more insightful. E.g. 1,2,3 or TRUE,FALSE or 1.23,4.56,3.14 |
| vocabularies<sup>1</sup> | Refer to ontologies being used | Find list to choose from in CatalogueOntologies Vocabularies |
| collectionEvent.resource | Your cohort pid | The collectionEvent needs to be predefined in the _CollectionEvents_ sheet; e.g. y1 or y2 |
| collectionEvent.name | Refer to the name of a collection event | The collectionEvent needs to be predefined in the _CollectionEvents_ sheet; e.g. y1 or y2 |
| keywords<sup>1</sup> | Enables grouping of variables into topics and helps to display variables in a tree | Find list to choose from in Catalogue |

<sup>Table 3. Description of the columns that can be filled out for SourceVariables. * = mandatory; 
<sup>1</sup>contact [*molgenis-support@umcg.nl*](mailto:molgenis-support@umcg.nl) to add Vocabularies, Keywords or Units</sup>

#### *SourceVariableValues* sheet

The coding of categorical variables is defined in the *SourceVariableValues* sheet. This sheet is optional, but it is
highly recommended to fill out the codes and values for your categorical variables, so that your data becomes more
insightful for those that are interested.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| dataDictionary.resource \* | Source data dictionary that the variable belongs to | Fill out your cohort pid |
| dataDictionary.version \* |  Version of the data dictionary that the variable belongs to | Source data dictionaries must be predefined in the _SourceDataDictionaries_ sheet |
| variable.table \* | Table that contains the variable | Tables must be predefined in the _SourceTables_ sheet |
| variable.name \* | Variable name | Variables must be predefined in the _SourceVariables_ sheet |
| value \* | The code or value used | e.g. 1, 2 or -99 |
| label \* | The label corresponding to the value | e.g. 'yes', 'no' or 'NA' |
| order | The order in which the code list should appear | e.g. 1 |
| isMissing | Whether this value indicates a missing field | TRUE or FALSE |

<sup>Table 4. Description of the columns that can be filled out for SourceVariableValues. * = mandatory</sup>

#### *RepeatedSourceVariables* sheet

The *RepeatedSourceVariables* sheet is optional, and is most often used by cohorts whose variables are observed
repeatedly. Variables that are repeats of a variable defined in the sheet *SourceVariables* are defined in the *
RepeatedSourceVariables* sheet. Defining your repeated variables using this sheet will limit the amount of information
that has to be repeated when filling out repeated variables. This sheet is optional.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| dataDictionary.resource \* | Source data dictionary that this variable belongs to | Fill out your cohort pid |
| dataDictionary.version \* |  Version of the source data dictionary that this variable belongs to | Source data dictionaries must be predefined in the _SourceDataDictionaries_ sheet |
| table \* | Table name. | e.g. core |
| name \* | Variable name. | e.g. height\_1 |
| isRepeatOf.table \* | Table that contains the variable that is repeated | Tables must be predefined in the _SourceTables_ sheet; e.g. core |
| isRepeatOf.name \* | Name of the variable that is repeated | Variables must be predefined in the _SourceVariables_ sheet; e.g. height\_0 |
| collectionEvent.resource | Your cohort pid | The collectionEvent needs to be predefined in the _CollectionEvents_ sheet; e.g. y1 or y2 |
| collectionEvent.name | Refer to the name of a collection event | The collectionEvent needs to be predefined in the _CollectionEvents_ sheet; e.g. y1 or y2 |

<sup>Table 5. Description of the columns that can be filled out for RepeatedSourceVariables. * = mandatory</sup>

#### *CollectionEvents* sheet

The *CollectionEvents* sheet is optional, and is most often used by cohorts. The timing of data collection in events is
defined in the *CollectionEvents* sheet. It can be used to describe time periods within which the data for variables are
collected. The events are defined here and referred to from the sheets *SourceVariables* and/or *
RepeatedSourceVariables*. Collection events can also be filled out when filling out the 
[cohort rich metadata](cat_cohort-data-manager.md#fill-out-cohort-rich-metadata).

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| resource \* | Your cohort pid | |
| name \* | Name of the collection event | e.g. y9 |
| description | Event description | e.g. Between 9 and 10 years |
| ageGroups | The age groups that were sampled within this collection event | Find list to choose from in CatalogueOntologies  (AgeGroups) |
| subcohorts | (sub)populations that are targeted with this collection event | e.g. women or children |

<sup>Table 6. Description of the columns that can be filled out for SourceCollectionEvents. * = mandatory</sup>

#### *Subcohorts* sheet

The sheet *Subcohorts* is optional, and is most often used by cohorts. Here you may describe populations that can be
linked to collection events. Subcohorts can also be filled out when filling out the
[cohort rich metadata](cat_cohort-data-manager.md#fill-out-cohort-rich-metadata).

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| resource \* | Your cohort pid | |
| name \* | Name of the subpopulation | e.g. mothers or children |
| description | Subpopulation description | |

<sup>Table 7. Description of the columns that can be filled out for Subpopulations. * = mandatory</sup>

#### Define harmonisations

We use the [*Mappings* template*](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/SourceMappings.xlsx) to
describe the harmonisations. The 
[*Mappings* template*](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/SourceMappings.xlsx) consists of two
sheets (TableMappings and VariableMappings). It is used to define the mappings from source variables to target
variables, or the Extraction, Transformation and Load (ETL) process from a data source to a common data model (CDM).
You can download this
[*filled out example*](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/SourceMappings_testCohort.xlsx)
as a reference for filling out the template.

#### *TableMappings* sheet

Harmonisation procedures at the table level are defined in the *TableMappings* sheet, irrespective of whether the table
is in a cohort or in a data bank.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| fromDataDictionary.resource \* | Source cohort pid |  |
| fromDataDictionary.version \* | Source data dictionary version | |
| fromTable \* | Source table name. | Tables must be predefined in the *SourceTables* sheet |
| toDataDictionary.resource \* | Name of the target common data model  | e.g. LifeCycle_CDM, LongITools_CDM |
| toDataDictionary.version \* | Target data dictionary version | Look up in [variable explorer](https://data-catalogue.molgeniscloud.org/catalogue/catalogue/#/variable-explorer/)|
| toTable \* | Target table name | Map to a table that is defined in a common data model |
| description | Description of the harmonisation | |
| syntax | Syntax used for this harmonisation | |

<sup>Table 8. Description of the columns that can be filled out for VariableMappings. * = mandatory</sup>

#### *VariableMappings* sheet

Harmonisation procedures at the variable level are defined in the *VariableMappings* sheet.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| fromDataDictionary.resource \* | Source cohort pid |  |
| fromDataDictionary.version \* | Source data dictionary version | |
| fromTable \* | Source table name | Tables must be predefined in the *SourceTables* sheet |
| fromVariable | Source variable name(s) | Variables must be predefined in the _SourceVariables_ sheet; When multiple variables are mapped together use a comma-separated list, e.g. v1,v2,v3 |
| fromVariablesOtherTables.table | Other source tables | When using variables from multiple other tables, use a comma-separated list, e.g. table1,table2,table3<sup>1</sup> |
| fromVariablesOtherTables.name | Source variable(s) from other tables than filled out under fromTable | When using variables from multiple other tables, use a comma-separated list, the order corresponding to the order of the tables they are derived from specified under fromVariablesOtherTables.table<sup>1</sup> |
| toDataDictionary.resource \* | Name of the target common data model  | e.g. LifeCycle_CDM, LongITools_CDM |
| toDataDictionary.version \* | Source data dictionary version | Look up in [variable explorer](https://data-catalogue.molgeniscloud.org/catalogue/catalogue/#/variable-explorer/) |
| toTable \* | Target table name. | Map to a table that is defined in a common data model |
| toVariable \* | Target variable name | Map to a variable that is defined in a common data model |
| match | Whether the harmonisation is partial, complete or NA (non-existent) | Find list to choose from in CatalogueOntologies (StatusDetails) |
| description | Description of the harmonisation | |
| syntax | Syntax used for this harmonisation | |

<sup>*Table 9. Description of the columns that can be filled out for VariableMappings. * = mandatory <sup>1</sup>see sheet VariableMappings in the 
[*example template*](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/SourceMappings_testCohort.xlsx)
for an example on how to fill this out (last line) </sup> 

### Upload metadata

When you have filled out the template(s) you can start uploading the metadata. When you log in to MOLGENIS Data 
Catalogue you will see a listing of databases that are accessible to you. Click on your cohort's database to access it. 
Go to 'Up/Download' in the menu. Use 'browse' to select a template and 'upload' to start uploading your metadata. After 
uploading, you can view your metadata under 'Tables'.

Please report any bugs or difficulties to [molgenis-support](mailto:molgenis-support@umcg.nl).

#### Find harmonisations

When your data is uploaded to the Data Catalogue you can find your own harmonised variables in variable details in the 
[Variable Explorer] (https://data-catalogue.molgeniscloud.org/catalogue/catalogue/#/variable-explorer/) 
once they have been transferred there. 
Use the search bar to find your variable(s) of interest.

![Variable search](../img/cat_variable-open.png)

Click on "Details"

![Variable detail](../img/cat_variable-detail.png)

Click on "Harmonization"

![Variable mapping](../img/cat_variable-mapping.png)

#### Request access (catalogue)

If you do not have an account to upload data to the Data Catalogue yet, please
email [molgenis-support](mailto:molgenis-support@umcg.nl) to apply for an account.
