# CDM / Network data manager

The Network data manager is responsible for uploading the common data model (CDM) to the MOLGENIS catalogue.

## MOLGENIS catalogue

### Define CDM metadata

[MOLGENIS catalogue](https://data-catalogue.molgeniscloud.org/catalogue/catalogue/#) provides a
framework to describe in detail: metadata of different data resources (such as cohorts and data sources); definitions of 
the variables collected in these resources; and mappings to common data models. Its purpose is to provide a gateway to 
find and access health research data and to facilitate pooled data analysis of multiple cohorts 
[Fortier et al, 2017](https://pubmed.ncbi.nlm.nih.gov/27272186/) and multi-datasource studies 
[Gini et al, 2020](https://pubmed.ncbi.nlm.nih.gov/32243569/).

- The resource metadata provides descriptive information such as contact details, name of the cohort, and high-level
  summary of contents and cohort design.
- The 'source variables' can be considered as a codebook or data dictionary for a resource (e.g.
  ALSPAC).
- Similarly, the common data model metadata (or 'target variables') can be considered the codebook for a network of
  cohorts working together (e.g. LifeCycle)
- The mappings describe how source variables have been converted into target variables as basis for integrated analysis.

This section explains how to submit the 'target variables' (also called the harmonised model or common data model) into
the MOLGENIS catalogue. Expected users of this 'how to' are central data managers of networks such as LifeCycle or
LongITools. You will need login details to upload metadata to the MOLGENIS catalogue.

#### Define common data elements

We use the [*TargetDictionary* template](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/TargetDictionary.xlsx)
to describe common data model elements. The 
[*TargetDictionary* template](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/TargetDictionary.xlsx)
consists of multiple sheets. Each sheet corresponds to a table in the catalogue. The columns in the sheet
correspond to columns in the table concerned. This document describes how to fill out each of the sheets and their
columns. A column with an asterisk (\*) after its name is mandatory, i.e., it should contain values for the system to
accept a data upload. You can download this
[*filled out example*](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/TargetDictionary_testCDM.xlsx)
as a reference for filling out the template.

It is good practice to try adding a few variables to the template first and see whether your upload succeeds. To
upload the metadata to the catalogue see the section [Upload metadata](cat_network-data-manager.md#upload-metadata).

![Figure 1. Tables in a Network’s staging area in the MOLGENIS catalogue](../img/cat_tables-in-catalogue.png)

<sup>Figure 1. Tables in a Network’s staging area in the MOLGENIS catalogue.</sup>


### Fill out network rich metadata

Open your staging area, navigate to 'Tables' and open the table 'Resources'. Your network id and name are already 
filled out. Click on the pencil sign next to this entry to start editing your network rich metadata by filling out 
the form. 'Subpopulations' and 'Collection events' are filled out through the same route, by accessing the corresponding tables.


### Define the common data model

#### *Datasets* sheet

The network's datasets are defined in the *Datasets* sheet. Columns with an asterisk (\*) after their name are mandatory.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| resource \* | Id of the <b>resource</b>. | e.g LifeCycle, LongITools or ATHLETE |
| name \* | Unique dataset name | |
| label | Dataset label | |
| description | Dataset description | |
| dataset type<sup>1</sup> | Type of dataset | Find list to choose from in CatalogueOntologies [Dataset types](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/DatasetTypes) |
| unit of observation<sup>1</sup> | Defines what each record in this dataset describes | Find list to choose from in CatalogueOntologies [Observation targets](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/ObservationTargets) |
| number of rows | Count of the number of records in this dataset | |
| keywords<sup>1</sup> | Enables grouping of datasets into topics and helps to display variables in a tree | Find list to choose from in CatalogueOntologies [Keywords](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/Keywords) |
| since version | Version of the data model when this dataset was introduced | e.g. 1.0.0 or 2.1 |
| until version | Version of the data model when this dataset was deleted | e.g. 2.0.0 or 2.1 |

<sup>Table 1. Description of the columns that can be filled out for Datasets. * = mandatory; 1 = contact [*molgenis support*](mailto:support@molgenis.org) to add Keywords, Observation targets or Dataset types</sup>

#### *Variables* sheet

The network's variables are defined in the *Variables* sheet.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| resource \* | Resource (Network) that this variable belongs to. Fill out your resource id | The resource id is found in the table _Resources_ in the resource staging area |
| dataset \* | Dataset that contains the variable. | Datasets must be predefined in the _Datasets_ sheet |
| name \* | Variable name, unique within a dataset | |
| label | Human readable variable label | |
| format | The data type of the variable | Find list to choose from in CatalogueOntologies [Formats](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/Formats) |
| unit<sup>1</sup> | Unit in case of a continuous or integer format | Find list to choose from in CatalogueOntologies [Units](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/Units) |
| description | Description of the variable | |
| example values | Examples of values in a comma separated list | Makes your data structure more insightful. E.g. 1,2,3 or TRUE,FALSE or 1.23,4.56,3.14 |
| repeat unit<sup>1</sup> | In case of repeated variables, indicate the repeat period | Find list to choose from in CatalogueOntologies [Repeat units](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/RepeatUnits) |
| repeat min | The minimum repeat unit | E.g. 0 or 10 |
| repeat max | The maximum repeat unit | E.g. 10 or 60 |
| collection events | Refer to the names of collection events in a comma separated list | The collection events need to be predefined in the Collection events table in the resource staging area; e.g. y1, y2 |
| vocabularies<sup>1</sup> | Refer to ontologies being used | Find list to choose from in CatalogueOntologies [Vocabularies](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/Vocabularies) |
| keywords<sup>1</sup> | Enables grouping of variables into topics and helps to display variables in a tree | Find list to choose from in Catalogue [Keywords](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/Keywords)|
| since version | Version of the data model when this variable was introduced | e.g. 1.0.0 or 2.1 |
| until version | Version of the data model when this variable was deleted | e.g. 2.0.0 or 2.1 |
| useExternalDefinition.resource | Refer to the associated resource id | When using the definitions of a harmonised variable from another CDM |
| useExternalDefinition.dataset | Refer to the associated dataset name | When using the definitions of a harmonised variable from another CDM |
| use externaldefinition.name | Refer to the associated variable name | When using the definitions of a harmonised variable from another CDM |

<sup>Table 2. Description of the columns that can be filled out for Variables. * = mandatory; 
1 = contact [*molgenis support*](mailto:support@molgenis.org) to add Vocabularies, Keywords, Repeat units, or Units</sup>

#### *Variable values* sheet

The coding of categorical variables is defined in the *Variable values* sheet. This sheet is optional, but it is
highly recommended to fill out the codes and values for your categorical variables, so that your data becomes more
insightful for those that are interested.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| resource \* | Resource that the variable belongs to. Fill out your resource id | The resource id is found in the table _Resources_ in the resource staging area |
| dataset \* | Dataset that contains the variable | Datasets must be predefined in the _Datasets_ sheet |
| name \* | Variable name | Variables must be predefined in the _Variables_ sheet |
| value \* | The code or value used | e.g. 1, 2 or -99 |
| label \* | The label corresponding to the value | e.g. 'yes', 'no' or 'NA' |
| order | The order in which the code list should appear | e.g. 1 |
| is missing | Whether this value indicates a missing field | TRUE or FALSE |
| since version | Version of the data model when this variable value was introduced | e.g. 1.0.0 or 2.1 |
| until version | Version of the data model when this variable value was deleted | e.g. 2.0.0 or 2.1 |

<sup>Table 3. Description of the columns that can be filled out for Variable values. * = mandatory</sup>

### Request access

Send an email to [*molgenis support*](mailto:support@molgenis.org) to apply for an account to upload metadata to
the catalogue.

### Upload metadata

When you log in to the MOLGENIS catalogue you will see a listing of databases that are accessible to you. Click on your
network's database to access it. Go to 'Up/Download' in the menu. Use 'browse' to select a template and 'upload' to
start uploading your metadata. After uploading you can view your metadata under 'Tables'. When you are finished uploading, 
contact [*molgenis support*](mailto:support@molgenis.org) to synchronise your data to the catalogue.
