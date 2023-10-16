# Data manager of a cohort or data source

## Data Catalogue

[MOLGENIS Data Catalogue](https://data-catalogue.molgeniscloud.org/catalogue/catalogue/#) provides a
framework to describe in detail: metadata of cohorts and of data sources; definitions of the variables collected in
cohorts and data sources; and mappings to common data models. Its purpose is to facilitate pooled data analysis of multiple cohorts
[Fortier et al, 2017](https://pubmed.ncbi.nlm.nih.gov/27272186/) and multi-data
source studies [Gini et al, 2020](https://pubmed.ncbi.nlm.nih.gov/32243569/).

- The metadata of cohorts include descriptive information such as contact details, name of the cohort, and high-level
  summary of contents and cohort design. The metadata of data sources, of the corresponding data banks and of the
  organisations that provide access to them, include descriptive information such as contact details, reason for
  existence of the data banks, the prompt for the records in the data bank, and lag time for updating and accessing data.
- The metadata of the source variables can be considered the codebook or data
  dictionary of a cohort (e.g. ALSPAC) and of the tables which make up a data source's data bank(s) (e.g. the Danish
  Healthcare Registries).
- Similarly, the common data models (or 'target variables') can be considered the codebook for a network of organisations
  with access to cohorts or data sources (e.g. LifeCycle or ConcePTION)
- The mappings describe how source variables have been converted into target variables as a basis for integrated analysis.

### Data harmonisation

Each organisation with access to data (which may be a cohort, or a data source composed of one or more data banks)
harmonises their data according to the consortium’s protocols into a common data model (CDM) format which has been
centrally agreed upon. In some projects, data may be made available via [DataSHIELD](https://www.datashield.org/). In these cases each resource stores
the data locally in a [MOLGENIS Armadillo](/#/armadillo/) DataSHIELD server.

### Staging areas for uploads

The metadata of the cohort or of the data source are first uploaded into what are called "staging areas" of the Data 
Catalogue. Later on the metadata are transferred to production; use of a staging area allows for review before the 
metadata are entered in the live catalogue. 

You will need credentials to log in and upload metadata. 

Cohorts in projects such as ATHLETE, IPEC and LongITools use [*data-catalogue-staging*](https://data-catalogue.molgeniscloud.org/apps/central/#/).  
ConcePTION uses [*conception-acc*](https://conception-acc.molgeniscloud.org).

When you log in, you will be able to see at least the following databases:

- <b>DataCatalogue</b>: The catalogue data, in which you can search for target variables to map to.
- <b>CatalogueOntologies</b>: This database contains the look-up list that you need for filling out some columns in the
  templates, e.g. format or unit. If you need to add anything to these look-up lists, contact us
  at [molgenis-support](mailto:molgenis-support@umcg.nl).
- <b>SharedStaging</b>: A communal staging area in which Organisations are added and edited.
- <b>Your own database</b>: use this to upload the templates once you have filled them out.
- <b>Your own database </b>: (here: testCohort and testNetwork) Use this to fill out rich metadata and to upload the templates once you have filled them out.

![MOLGENIS databases](../img/cat_databases.png)

<sup>*Figure 1. Databases in the Data Catalogue staging area.*</sup>

### Fill out cohort rich metadata

Open your staging area, navigate to 'Tables' and open the table 'Cohorts'. Your cohort id and name are already 
filled out. Click on the pencil sign next to this entry to start editing your cohort rich metadata by filling out 
the form. 'Subcohorts' and 'Collection events' should also be filled out through this route. You can fill them out 
in subsections inside the 'Cohorts' form.

### Define metadata of cohorts or data sources

This section explains how to submit the 'source variables' + 'mappings from source variables to target variables' into
the Data Catalogue. Expected users of this 'how to' are data managers within the organisations with access to cohorts or
data sources. This document assumes you have received login details for upload of your metadata. You can also watch
this [*instruction video*](https://www.youtube.com/watch?v=b_Ef_Uiw1gE&amp;ab_channel=MOLGENIS). Note that this video used dictionary model version 2.x, which was updated to 3.x.

#### Define source variable metadata / source data dictionary

We use the [*SourceDictionary template*](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/SourceDictionary.xlsx)
to define variable metadata. The [*SourceDictionary
template*](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/SourceDictionary.xlsx) consists of multiple
sheets. Each sheet corresponds to a table in the Data Catalogue (Figure 1). The columns in the sheet correspond to
columns in the table concerned. This document describes how to fill out each of the sheets and their columns. A column
with an asterisk (\*) after its name is mandatory, i.e., it should contain values for the system to accept a data
upload. You can download this 
[*filled out example*](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/SourceDictionary_testCohort.xlsx) 
as a reference for filling out the template. Note that there is no sheet for *AllVariables*. This table is a generic listing of all variables entered for
the cohort; it shows *Variables* and *RepeatedVariables* in one table.

It is good practice to try adding a few variables to the template first and see whether your upload succeeds. To
upload the metadata to the Data Catalogue see the section [Upload metadata](cat_cohort-data-manager.md#upload-metadata).

![MOLGENIS tables in cohort catalogue](../img/cat_tables-in-cohort-catalogue.png)

<sup>*Figure 2. Tables in a cohort's database in the Data Catalogue. Note that not all tables are filled out 
via the templates, some are filled via an online form, see section 
[Fill out cohort rich metadata](cat_cohort-data-manager.md#fill-out-cohort-rich-metadata).*</sup>

#### *Datasets* sheet

The datasets/tables in a cohort or in the data banks of a data source are defined in the *Datasets* sheet. Columns with an
asterisk (\*) after their name are mandatory.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| resource \* | Cohort or data source that this table belongs to | |
| name \* | Unique dataset or table name | |
| label | Dataset label | |
| description | Dataset description | |
| unit of observation | Defines what each record in this dataset describes | |
| number of rows | Count of the number of records in this dataset | |
| keywords<sup>1</sup> | Enables grouping of datasets into topics and helps to display variables in a tree | Find list to choose from in CatalogueOntologies [Keywords](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/Keywords) |
| since version | Version of the data model when this dataset was introduced | e.g. 1.0.0 or 2.1 |
| until version | Version of the data model when this dataset was deleted | e.g. 2.0.0 or 2.1 |

<sup>Table 1. Description of the columns that can be filled out for Datasets. * = mandatory; 1 = contact [*molgenis-support@umcg.nl*](mailto:molgenis-support@umcg.nl) to add Keywords</sup>


#### *Variables* sheet

The variables of the datasets specified in the *Datasets* sheet are defined in the *Variables* sheet.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| resource \* | Cohort or databank that this variable belongs to | Fill out your cohort or databank id |
| dataset \* | Dataset that contains the variable. | Datasets must be predefined in the _Datasets_ sheet |
| name \* | Variable name, unique within a dataset | |
| label | Human readable variable label | |
| format | The data type of the variable | Find list to choose from in CatalogueOntologies [Formats](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/Formats) |
| unit<sup>1</sup> | Unit in case of a continuous or integer format | Find list to choose from in CatalogueOntologies [Units](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/Units) |
| description | Description of the variable | |
| exampleValues | Examples of values in a comma separated list | Makes your data more insightful. E.g. 1,2,3 or TRUE,FALSE or 1.23,4.56,3.14 |
| vocabularies<sup>1</sup> | Refer to ontologies being used | Find list to choose from in CatalogueOntologies [Vocabularies](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/Vocabularies) |
| collection event | Refer to the name of a collection event | The collection event needs to be predefined in the Collection events table in the cohort or data source database; e.g. y1 or y2 |
| keywords<sup>1</sup> | Enables grouping of variables into topics and helps to display variables in a tree | Find list to choose from in Catalogue [Keywords](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/Keywords)|
| since version | Version of the data model when this variable was introduced | e.g. 1.0.0 or 2.1 |
| until version | Version of the data model when this variable was deleted | e.g. 2.0.0 or 2.1 |

<sup>Table 2. Description of the columns that can be filled out for Variables. * = mandatory; 
1 = contact [*molgenis-support@umcg.nl*](mailto:molgenis-support@umcg.nl) to add Vocabularies, Keywords or Units</sup>

#### *Variable values* sheet

The coding of categorical variables is defined in the *Variable values* sheet. This sheet is optional, but it is
highly recommended to fill out the codes and values for your categorical variables, so that your data becomes more
insightful for those that are interested.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| resource \* | Cohort or databank that the variable belongs to | Fill out your cohort or databank id |
| variable.dataset \* | Dataset that contains the variable | Datasets must be predefined in the _Datasets_ sheet |
| variable.name \* | Variable name | Variables must be predefined in the _Variables_ sheet |
| value \* | The code or value used | e.g. 1, 2 or -99 |
| label \* | The label corresponding to the value | e.g. 'yes', 'no' or 'NA' |
| order | The order in which the code list should appear | e.g. 1 |
| is missing | Whether this value indicates a missing field | TRUE or FALSE |
| since version | Version of the data model when this variable value was introduced | e.g. 1.0.0 or 2.1 |
| until version | Version of the data model when this variable value was deleted | e.g. 2.0.0 or 2.1 |

<sup>Table 3. Description of the columns that can be filled out for Variable values. * = mandatory</sup>

#### *Repeated variables* sheet

The *Repeated variables* sheet is optional, and is most often used by cohorts whose variables are observed
repeatedly. Variables that are repeats of a variable defined in the sheet *Variables* are defined in the *Repeated variables* sheet. 
Defining your repeated variables using this sheet will limit the amount of information
that has to be repeated when filling out repeated variables. This sheet is optional.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| resource \* | Cohort or databank that this variable belongs to | Fill out your cohort or databank id |
| dataset \* | Dataset name. | e.g. core |
| name \* | Variable name. | e.g. height\_1 |
| is repeat of.dataset \* | Dataset that contains the variable that is repeated | Tables must be predefined in the _Datasets_ sheet; e.g. core |
| is repeat of.name \* | Name of the variable that is repeated | Variables must be predefined in the _Variables_ sheet; e.g. height\_0 |
| collection event | Refer to the name of a collection event | The collection event needs to be predefined via forms; e.g. y1 or y2 |
| since version | Version of the data model when this variable was introduced | e.g. 1.0.0 or 2.1 |
| until version | Version of the data model when this variable was deleted | e.g. 2.0.0 or 2.1 |


<sup>Table 4. Description of the columns that can be filled out for Repeated variables. * = mandatory</sup>

#### Define harmonisations

We use the [*Mappings* template*](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/Mappings.xlsx) to
describe the harmonisations. The 
[*Mappings* template*](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/Mappings.xlsx) consists of two
sheets (Dataset mappings and Variable mappings). It is used to define the mappings from source variables to target
variables, or the Extraction, Transformation and Load (ETL) process from a data source to a common data model (CDM).
You can download this
[*filled out example*](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/Mappings_testCohort.xlsx)
as a reference for filling out the template.

#### *Dataset mappings* sheet

Harmonisation procedures at the table level are defined in the *Dataset mappings* sheet, irrespective of whether the table
is in a cohort or in a data bank.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| source \* | Databank or cohort id |  |
| source dataset \* | Source dataset name | Datasets must be predefined in the _Datasets_ sheet in the SourceDictionary template |
| target \* | Name of the target common data model  | e.g. LifeCycle_CDM, LongITools_CDM, see [variable explorer](https://data-catalogue.molgeniscloud.org/catalogue/catalogue/#/variable-explorer) |
| target dataset \* | Target dataset name | Map to a [dataset](https://data-catalogue.molgeniscloud.org/catalogue/tables/#/Datasets) that is defined in a common data model |
| description | Description of the harmonisation | |
| syntax | Syntax used for this harmonisation | |

<sup>Table 5. Description of the columns that can be filled out for Variable mappings. * = mandatory</sup>

#### *Variable mappings* sheet

Harmonisation procedures at the variable level are defined in the *Variable mappings* sheet.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| source \* | Databank or cohort id |  |
| source dataset \* | Source table name | Datasets must be predefined in the *Datasets* sheet in the SourceDictionary template |
| source variables | Source variable name(s) | Variables must be predefined in the _Variables_ sheet in the SourceDictionary template; When multiple variables are mapped together use a comma-separated list, e.g. v1,v2,v3 |
| source variables other datasets.dataset | Other source tables | When using variables from multiple other datasets, use a comma-separated list, e.g. dataset1,dataset2,dataset3<sup>1</sup> |
| source variables other datasets.name | Source variable(s) from other datasets than filled out under source dataset | When using variables from multiple other datasets, use a comma-separated list, the order corresponding to the order of the datasets they are derived from specified under source variables from other datasets.dataset<sup>1</sup> |
| target \* | Name of the target common data model  | e.g. LifeCycle_CDM, LongITools_CDM, see [variable explorer](https://data-ca`talogue.molgeniscloud.org/catalogue/catalogue/#/variable-explorer) |
| target dataset \* | Target dataset name. | Map to a [dataset](https://data-catalogue.molgeniscloud.org/catalogue/tables/#/Datasets) that is defined in a common data model |
| target variable \* | Target variable name | Map to a [variable](https://data-catalogue.molgeniscloud.org/catalogue/catalogue/#/variable-explorer) that is defined in a common data model |
| match \* | Whether the harmonisation is partial, complete or NA (non-existent) | Find list to choose from in CatalogueOntologies [StatusDetails] |
| description | Description of the harmonisation | |
| syntax | Syntax used for this harmonisation | |

<sup>*Table 6. Description of the columns that can be filled out for Variable mappings. * = mandatory; 1 = see sheet Variable mappings in the 
[*example template*](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/Mappings_testCohort.xlsx)
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
