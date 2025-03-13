# Data manager

The [MOLGENIS Data Catalogue](https://data-catalogue.molgeniscloud.org/) provides a
framework for detailed descriptions of the following:
metadata of different types of resources, such as cohort studies and biobanks;
definitions of the variables collected in these resources;
and mappings of these variables to common data models.
Its purpose is to provide a gateway for finding and accessing health research data
and to facilitate pooled data analysis of multiple cohorts ([Fortier et al., 2017](https://pubmed.ncbi.nlm.nih.gov/27272186/))
and multi-data source studies ([Gini et al., 2020](https://pubmed.ncbi.nlm.nih.gov/32243569/)).

The metadata of data sources consists of high-level descriptive information, such as contact details,
contents, design, and access and usage conditions.
The definitions of the source variables of a resource
(e.g. a cohort study like [ALSPAC](https://data-catalogue.molgeniscloud.org/catalogue/ssr-catalogue/all/collections/ALSPAC))
can be considered its codebook or data dictionary.
Similarly, the common data models (or 'target variables') can be considered the codebook for a network of organisations
with access to multiple data sources
(e.g. [LifeCycle](https://data-catalogue.molgeniscloud.org/catalogue/ssr-catalogue/LifeCycle/variables)).
Combining these two, the variable mappings describe how source variables have been converted into target variables,
which can be used as a basis for integrated analysis. In some projects, data may be made available via [DataSHIELD](https://www.datashield.org/).
In these cases each resource stores the data locally in a [MOLGENIS Armadillo](../armadillo/) DataSHIELD server.

This page describes the process of filling out metadata and variable information for a resource
and uploading this to the catalogue. Users of this guide are expected to be data managers within the organisations
with access to the resources to be included in the catalogue, who can submit metadata, source variables
(also called collected variables) and variable mappings. In addition, it can be used by central data managers
of networks with a common data model such as LifeCycle or LongITools, who can submit metadata and target variables
(also called the harmonised variables or common data model).
This document assumes you have received login details for upload of your variables and metadata.
If this is not the case, see the section on [requesting access](#request-access).

## Staging areas

The metadata of the resource are first uploaded into what are called the 'staging areas' of the catalogue.
Later on the metadata are transferred to the main catalogue
Use of a staging area allows for review before the information becomes available through the live catalogue.
If the data in your staging area is ready for transfer you can let us know by emailing us through [MOLGENIS support](mailto:support@molgenis.org).

When you log in, you will be able to see at least the following databases:

- **Aggregates**: A database where aggregate data are stored.
- **Your own database**: The staging area of the resource you are working on.
Use this to fill out rich metadata and to upload the variable templates once you have filled them out.
- **catalogue**: The main catalogue, where your data will eventually be transferred to.
This is also where you can search for target variables to map to.
- **CatalogueOntologies**: This database contains the ontologies (or look-up lists)
that you need for filling out some columns in the templates, e.g. format or unit.
These are centrally managed by us. If you need something added to these look-up lists, [let us know](mailto:support@molgenis.org).
- **Test databases** (here: testCatalogue, testCohort, testDatasource, testNetwork and testStaging):
You can see filled out example metadata in these databases.

![MOLGENIS databases](../img/cat_databases.png)

*Figure 1. Databases in the MOLGENIS catalogue.*

## Fill out rich metadata

Open your staging area and open the table **Resources**. The **id** and **name** columns are already filled in.
Click on the pencil sign next to this entry to start editing your metadata by filling out
the form. If you are planning on entering source variables,
make sure to fill in tables **Collection events** and **Subpopulations** as well.
You can later refer to these from columns in the dictionary templates to indicate
which variables were collected during which collection event and for which subpopulation.

## Define data dictionaries and common data models

The next sections explain how to define variable metadata, which can be used to create data dictionaries (also known as codebooks),
in the case of source variables, and common data models, in the case of target variables, in the MOLGENIS catalogue.
For the creation of data dictionaries, you can also watch this
[instruction video](https://www.youtube.com/watch?v=b_Ef_Uiw1gE&amp;ab_channel=MOLGENIS).
Note that this video uses an older version of the dictionary model (2.x).
However, the basic principles remain the same, although column names have changed between this version and the current version.
There is no specific instruction video for the creation of common data models, but network data managers might still find
the video a useful introduction to definining variable metadata in the MOLGENIS catalogue.

## Define variable metadata

We use the [Dictionary template](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/Dictionary.xlsx)
to define metadata on variables, creating data dictionaries (with source variables) or common data models (with target variables).
The [Dictionary template](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/Dictionary.xlsx)
consists of multiple sheets. Each sheet corresponds to a table in the catalogue (Figure 2 and 3).
The columns in each sheet correspond to columns in the table of the same name.
Here, we describe how to fill out each of the sheets and their columns.
Columns marked with an asterisk (\*) are mandatory,
i.e., they need to be filled in for the system to accept the data as valid. Depending on your needs, you can download a
[filled-in example for source variables](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/Dictionary_testCohort.xlsx)
and a [filled-in example for target variables](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/Dictionary_testNetwork.xlsx),
as a reference for filling out the template.

It is good practice to try adding a few variables to the template first and see whether your upload succeeds.
See the section [Upload variable metadata](cat_resource-data-manager.md#upload-variable-metadata) for details on how to upload.

![MOLGENIS tables in cohort staging area](../img/cat_tables-in-cohort-catalogue.png)

*Figure 2. Tables in a cohort staging area. Note that not all tables are filled out
via the templates, some are filled via an online form, see section
[Fill out rich metadata](cat_resource-data-manager.md#fill-out-rich-metadata).*

![MOLGENIS tables in network staging area](../img/cat_tables-in-catalogue.png)

*Figure 3. Tables in a network staging area. Note that not all tables are filled out
via the templates, some are filled via an online form, see section
[Fill out rich metadata](cat_resource-data-manager.md#fill-out-rich-metadata).*

### *Datasets* sheet

The datasets (sets of variables) that make up a resource are defined in the *Datasets* sheet.

| Column name                               | Description                                                                       | Remarks                                                                                                                                                         |
|-------------------------------------------|-----------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| resource\*                                | Resource that this dataset belongs to. Fill in your resource id                   | The resource id is found in the table *Resources* in the resource staging area                                                                                  |
| name\*                                    | Unique dataset name                                                               |                                                                                                                                                                 |
| label                                     | Dataset label                                                                     |                                                                                                                                                                 |
| dataset type<sup id="ds1">[1](#ds1)</sup> | Type of dataset                                                                   | Find list to choose from in CatalogueOntologies [Dataset types](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/DatasetTypes)             |
| description                               | Dataset description                                                               |                                                                                                                                                                 |
| unit of observation<sup>[1](#ds1)</sup>   | Defines what each record in this dataset describes                                | Find list to choose from in CatalogueOntologies [Observation targets](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/ObservationTargets) |
| number of rows                            | Count of the number of records in this dataset                                    |                                                                                                                                                                 |
| keywords<sup>[1](#ds1)</sup>              | Enables grouping of datasets into topics and helps to display variables in a tree | Find list to choose from in CatalogueOntologies [Keywords](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/Keywords)                      |
| since version                             | Version of the data model when this dataset was introduced                        | e.g. 1.0.0 or 2.1                                                                                                                                               |
| until version                             | Version of the data model when this dataset was deleted                           | e.g. 2.0.0 or 2.1                                                                                                                                               |

_Table 1. Description of the columns that can be filled out for Datasets.
\*: mandatory;
<a id="ds1">1</a>: contact [*molgenis support*](mailto:support@molgenis.org)
to add Keywords, Observation targets or Dataset types._

### *Variables* sheet

The variables making up the datasets specified in the *Datasets* sheet are defined in the *Variables* sheet.

| Column name                             | Description                                                                        | Remarks                                                                                                                                            |
|-----------------------------------------|------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| resource\*                              | Resource that this variable belongs to. Fill out your resource id                  | The resource id is found in the table *Resources* in the resource staging area                                                                     |
| dataset\*                               | Dataset that contains the variable.                                                | Datasets must be predefined in the *Datasets* sheet                                                                                                |
| name\*                                  | Variable name, unique within a dataset                                             |                                                                                                                                                    |
| label                                   | Human readable variable label                                                      |                                                                                                                                                    |
| format                                  | The data type of the variable                                                      | Find list to choose from in CatalogueOntologies [Formats](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/Formats)           |
| unit<sup id="v1">[1](#v1)</sup>         | Unit in case of a continuous or integer format                                     | Find list to choose from in CatalogueOntologies [Units](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/Units)               |
| description                             | Description of the variable                                                        |                                                                                                                                                    |
| example values                          | Examples of values in a comma separated list                                       | Makes your data structure more insightful. E.g. 1,2,3 or TRUE,FALSE or 1.23,4.56,3.14                                                              |
| repeat unit<sup id="v1">[1](#v1)</sup>  | In case of repeated variables, indicate the repeat period                          | Find list to choose from in CatalogueOntologies [Repeat units](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/RepeatUnits)  |
| repeat min                              | The minimum repeat unit                                                            | E.g. 0 or 10                                                                                                                                       |
| repeat max                              | The maximum repeat unit                                                            | E.g. 10 or 60                                                                                                                                      |
| collection event                        | Refer to the names of collection events in a comma separated list                  | The collection events need to be predefined in the Collection events table in the resource staging area; e.g. y1, y2                               |
| vocabularies<sup id="v1">[1](#v1)</sup> | Refer to ontologies being used                                                     | Find list to choose from in CatalogueOntologies [Vocabularies](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/Vocabularies) |
| keywords<sup id="v1">[1](#v1)</sup>     | Enables grouping of variables into topics and helps to display variables in a tree | Find list to choose from in Catalogue [Keywords](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/Keywords)                   |
| since version                           | Version of the data model when this variable was introduced                        | e.g. 1.0.0 or 2.1                                                                                                                                  |
| until version                           | Version of the data model when this variable was deleted                           | e.g. 2.0.0 or 2.1                                                                                                                                  |
| useExternaldefinition.resource          | Refer to the associated resource id                                                | When using the definitions of a harmonised variable from another CDM                                                                               |
| useExternaldefinition.dataset           | Refer to the associated dataset name                                               | When using the definitions of a harmonised variable from another CDM                                                                               |
| useExternaldefinition.name              | Refer to the associated variable name                                              | When using the definitions of a harmonised variable from another CDM                                                                               |

_Table 2. Description of the columns that can be filled out for Variables. \*: mandatory;
<a id="v1">1</a>: contact [*molgenis support*](mailto:support@molgenis.org) to add Vocabularies, Keywords, Repeat units, or Units._

### *Variable values* sheet

The coding of categorical variables is defined in the *Variable values* sheet. This sheet is optional, but it is
highly recommended to fill out the codes and values for your categorical variables, so that your data becomes more
insightful for those that are interested.

| Column name       | Description                                                       | Remarks                                                                        |
|-------------------|-------------------------------------------------------------------|--------------------------------------------------------------------------------|
| resource\*        | Resource that the variable belongs to. Fill out your resource id  | The resource id is found in the table *Resources* in the resource staging area |
| dataset\*         | Dataset that contains the variable                                | Datasets must be predefined in the *Datasets* sheet                            |
| variable\*        | Variable name                                                     | Variables must be predefined in the *Variables* sheet                          |
| value\*           | The code or value used                                            | e.g. 1, 2 or -99                                                               |
| label\*           | The label corresponding to the value                              | e.g. 'yes', 'no' or 'NA'                                                       |
| order             | The order in which the code list should appear                    | e.g. 1                                                                         |
| is missing        | Whether this value indicates a missing field                      | TRUE or FALSE                                                                  |
| ontology term URI | Reference to ontology term that defines this categorical value    | e.g. <http://purl.obolibrary.org/obo/NCIT_C48660>                              |
| since version     | Version of the data model when this variable value was introduced | e.g. 1.0.0 or 2.1                                                              |
| until version     | Version of the data model when this variable value was deleted    | e.g. 2.0.0 or 2.1                                                              |

_Table 3. Description of the columns that can be filled out for Variable values. \*:mandatory._

## Define harmonisations

We use the [*Mappings template*](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/Mappings.xlsx) to
describe the harmonisations,
i.e. mappings from a resource's source variables to the target variables of a common data model (CDM).
The [*Mappings template*](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/Mappings.xlsx)
consists of two sheets: Dataset mappings and Variable mappings.
Dataset mappings defines mappings from a dataset containing source variables to a dataset of the CDM.
Variable mappings is used to define the mappings from source variables to target
variables, which corresponds to the Extraction, Transformation and Load (ETL) process of a data source
to a common data model (CDM). You can download this
[*filled out example*](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/Mappings_testCohort.xlsx)
as a reference for filling out the template.

### *Dataset mappings* sheet

Harmonisation procedures at the dataset level are defined in the *Dataset mappings* sheet.

| Column name      | Description                                                                     | Remarks                                                                              |
|------------------|---------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| source\*         | Resource id. Fill out your resource id                                          | The resource id is found in the table *Resources* in the resource staging area       |
| source dataset\* | Source dataset name                                                             | Datasets must be predefined in the *Datasets* sheet in the SourceDictionary template |
| target\*         | Name of the target CDM                                                          | e.g. LifeCycle or LongITools                                                         |
| target dataset\* | Target dataset name                                                             | Map to a dataset that is defined in a CDM                                            |
| order            | Order in which table ETLs should be executed for this source-target combination |                                                                                      |
| description      | Description of the harmonisation                                                |                                                                                      |
| syntax           | Syntax used for this harmonisation                                              |                                                                                      |

_Table 4. Description of the columns that can be filled out for Variable mappings. \*: mandatory._

### *Variable mappings* sheet

Harmonisation procedures at the variable level are defined in the *Variable mappings* sheet.

| Column name                             | Description                                                                                    | Remarks                                                                                                                                                                                                                                             |
|-----------------------------------------|------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| source\*                                | Resource id. Fill out your resource id                                                         | The resource id is found in the table *Resources* in the resource staging area                                                                                                                                                                      |
| source dataset\*                        | Source table name                                                                              | Datasets must be predefined in the *Datasets* sheet in the SourceDictionary template                                                                                                                                                                |
| source variables                        | Source variable name(s)                                                                        | Variables must be predefined in the *Variables* sheet in the SourceDictionary template; When multiple variables are mapped together use a comma-separated list, e.g. v1,v2,v3                                                                       |
| source variables other datasets.dataset | Other source tables                                                                            | When using variables from multiple other datasets, use a comma-separated list, e.g. dataset1,dataset2,dataset3<sup id="vm1">[1](#vm1)</sup>                                                                                                         |
| source variables other datasets.name    | Source variable(s) from other datasets than filled out under source dataset                    | When using variables from multiple other datasets, use a comma-separated list, the order corresponding to the order of the datasets they are derived from specified under source variables from other datasets.dataset<sup id="vm1">[1](#vm1)</sup> |
| target\*                                | Name of the target CDM                                                                         | e.g. LifeCycle or LongITools                                                                                                                                                                                                                        |
| target dataset\*                        | Target dataset name.                                                                           | Map to a dataset that is defined in a CDM                                                                                                                                                                                                           |
| target variable\*                       | Target variable name                                                                           | Map to a variable that is defined in a CDM                                                                                                                                                                                                          |
| match\*                                 | Whether the harmonisation is partial, complete or na (non-existent)                            | Find list to choose from in CatalogueOntologies [StatusDetails](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/StatusDetails)                                                                                                |
| repeats                                 | In the case of a repeated target variable, comma separated list of repeats that were mapped to | e.g. 0,1,2,6,12                                                                                                                                                                                                                                     |
| description                             | Description of the harmonisation                                                               |                                                                                                                                                                                                                                                     |
| syntax                                  | Syntax used for this harmonisation                                                             |                                                                                                                                                                                                                                                     |

_Table 5. Description of the columns that can be filled out for Variable mappings. \*: mandatory;
<a id="vm1">1</a>: see sheet Variable mappings in the
[example template](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/Mappings_testCohort.xlsx)
for an example on how to fill this out (last line)._

## Upload variable metadata

When you have filled out the template(s) you can start uploading variable metadata. To do this, log in to the
[MOLGENIS catalogue backend](https://data-catalogue.molgeniscloud.org/apps/central/#/), where
you will see a list of databases that are accessible to you. Click on the database for your resource and
go to **Up/Download** in the menu. Use **Browse** to select the file containing the filled-in template and
click **Import** to start uploading. A progress report is displayed and if your upload completes without
errors, your variable metadata is available for viewing under **Tables**.

If your variable metadata is correctly uploaded and ready for transfer from the staging area
to the main catalogue, contact us at [MOLGENIS support](mailto:support@molgenis.org).
Once the data is transferred, you can find your own harmonised variables and variable details in the
[harmonised variable explorer](https://data-catalogue.molgeniscloud.org/catalogue/ssr-catalogue/all/variables) ([manual](cat_researcher)).

If you encounter any difficulties during this process, feel free to [contact us](mailto:support@molgenis.org).

## Request access

If you do not have an account yet, you will need to set one up:
Go to the catalogue at [data-catalogue.molgeniscloud.org](https://data-catalogue.molgeniscloud.org).
Click on **More** and select **Upload data**. Click on **Sign in**. Click on **LS Login**.
Search for and select your institution and follow the instructions.
You can now log in using your institutional e-mail account.
If your institution is not listed, set up an [ORCID](https://orcid.org/register),
then log in via LS login and choose the ORCID login option.
In addition, your account needs to be linked to the resource(s) you will be working on.
Contact us at [support@molgenis.org](mailto:support@molgenis.org) with your account e-mail address and
the resource(s) you will be working on to get the necessary access permissions.
