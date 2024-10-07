# CDM / Network data manager

The Network data manager is responsible for uploading the common data model (CDM) to the Data Catalogue.

## Catalogue

### Define CDM metadata

[MOLGENIS catalogue](https://data-catalogue.molgeniscloud.org/catalogue/catalogue/#) provides a
framework to describe in detail: metadata of different data resources, such as cohorts and data sources; definitions of 
the variables collected in these resources; and mappings to common data models. Its purpose is to provide a gateway to 
find and access health research data and to facilitate pooled data analysis of multiple cohorts 
[Fortier et al, 2017](https://pubmed.ncbi.nlm.nih.gov/27272186/) and multi-data source studies 
[Gini et al, 2020](https://pubmed.ncbi.nlm.nih.gov/32243569/).

- The resource metadata provides descriptive information such as contact details, name of the cohort, and high-level
  summary of contents and cohort design.
- The 'source variables' can be considered as a codebook or data dictionary for a resource (e.g.
  ALSPAC).
- Similarly, the common data model metadata (or 'target variables') can be considered the codebook for a network of
  cohorts working together (e.g. LifeCycle)
- The mappings describe how source variables have been converted into target variables as basis for integrated analysis.

This section explains how to submit the 'target variables' (also called the harmonised model or common data model) into
the catalogue. Expected users of this 'how to' are central data managers of networks such as LifeCycle or
LongITools. You will need login details to upload metadata to MOLGENIS catalogue.

#### Define common data elements

We use the [*TargetDictionary* template](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/TargetDictionary.xlsx)
to describe the common data model elements. The 
[*TargetDictionary* template](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/TargetDictionary.xlsx)
consists of multiple sheets. Each sheet corresponds to a table in catalogue. The columns in the sheet
correspond to columns in the table concerned. This document describes how to fill out each of the sheets and their
columns. A column with an asterisk (\*) after its name is mandatory, i.e., it should contain values for the system to
accept a data upload. You can download this
[*filled out example*](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/TargetDictionary_testCDM.xlsx)
as a reference for filling out the template.

It is good practice to try adding a few variables to the template first and see whether your upload succeeds. To
upload the metadata to the Data Catalogue see the section To upload the metadata to the catalogue see the
section [Upload metadata](cat_network-data-manager.md#upload-metadata).

![Figure 1. Tables in a Network’s staging area in the MOLGENIS catalogue](../img/cat_tables-in-catalogue.png)

<sup>Figure 1. Tables in a Network’s staging area in the MOLGENIS catalogue.</sup>


### Fill out network rich metadata

Open your staging area, navigate to 'Tables' and open the table 'Networks'. Your network id and name are already 
filled out. Click on the pencil sign next to this entry to start editing your network rich metadata by filling out 
the form. The network's common data model should also be filled out in the same manner under 'Models'.'Subcohorts' 
and 'Collection events' are filled out through the same route, by accessing the corresponding tables. Make sure to 
choose the right <b>model</b> id under resource when defining your 'Subcohorts' and 'Collection events', e.g. LongITools
should select LongITools_CDM.


### Define the common data model

#### *Datasets* sheet

The network's datasets are defined in the *Datasets* sheet. Columns with an asterisk (\*) after their name are mandatory.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| resource \* | Id of the <b>model</b>. | e.g LifeCycle_CDM, LongITools_CDM or ATHLETE_CDM |
| name \* | Unique dataset name | |
| label | Dataset label | |
| description | Dataset description | |
| unit of observation | Defines what each record in this dataset describes | |
| number of rows | Count of the number of records in this dataset | |
| keywords<sup>1</sup> | Enables grouping of datasets into topics and helps to display variables in a tree | Find list to choose from in CatalogueOntologies [Keywords](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/Keywords) |
| since version | Version of the data model when this dataset was introduced | e.g. 1.0.0 or 2.1 |
| until version | Version of the data model when this dataset was deleted | e.g. 2.0.0 or 2.1 |

<sup>Table 1. Description of the columns that can be filled out for Datasets. * = mandatory</sup>

#### *Variables* sheet

The network's variables are defined in the *Variables* sheet.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| resource \* | Id of the <b>model</b> that contains this variable | e.g LifeCycle_CDM, LongITools_CDM or ATHLETE_CDM |
| dataset \* | Dataset that contains the variable | Datasets must be predefined in the _Datasets_ sheet |
| name \* | Variable name, unique within a dataset | |
| label | Human readable variable label | |
| format | The data type of the variable | Find list to choose from in CatalogueOntologies [Formats](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/Formats) |
| unit<sup>1</sup> | Unit in case of a continuous or integer format | Find list to choose from in CatalogueOntologies [Units](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/Units) |
| description | Description of the variable | |
| keywords<sup>1</sup> | Enables grouping of variables into topics and displaying in a tree | Find list to choose from in CatalogueOntologies [Keywords](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/Keywords)  |
| example values | Examples of values in a comma separated list | Makes your data more insightful; e.g. 1,2,3 or TRUE,FALSE or 1.23,4.56,3.14 |
| mandatory | Whether this variable is required within this collection | |
| vocabularies<sup>1</sup> | Refer to ontologies being used | Find list to choose from in CatalogueOntologies [Vocabularies](https://data-catalogue.molgeniscloud.org/CatalogueOntologies/tables/#/Vocabularies) |
| collection event.resource | Refer to the resource that contains the collection event | e.g. LifeCycle |
| collection event.name | Refer to a collection event; The collection event needs to be predefined in the Collection events table in the network staging area | e.g. y1 or y2 |
| since version | Version of the data model when this variable was introduced | e.g. 1.0.0 or 2.1 |
| until version | Version of the data model when this variable was deleted | e.g. 2.0.0 or 2.1 |

<sup>Table 2. Description of the columns that can be filled out for Variables. * = mandatory; 1 = contact [*molgenis support*](mailto:support@molgenis.org) to add Vocabularies, Keywords or Units</sup>

#### *Variable values* sheet

The coding of categorical variables is defined in the *Variable values* sheet. This sheet is optional, but it is
highly recommended to fill out the codes and values for your categorical variables, so that your data becomes more
insightful for those that are interested.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| resource \* | Id of the <b>model</b> that contains this variable | e.g LifeCycle_CDM |
| variable.dataset \* | Dataset that contains the variable | Datasets must be predefined in the _Datasets_ sheet |
| variable.name \* | Variable name | Variables must be predefined in the _Variables_ sheet |
| value \* | The code or value used | e.g. 1, 2 or -99 |
| label \* | The label corresponding to the value | e.g. 'yes', 'no' or 'NA' |
| order | The order in which the code list should appear | e.g. 1 |
| is missing | Whether this value indicates a missing field | TRUE or FALSE |
| ontology term URI | Reference to an ontology term that defines this categorical value | e.g. [http://purl.obolibrary.org/obo/DOID\_1094](http://purl.obolibrary.org/obo/DOID\_1094) |
| since version | Version of the data model when this variable value was introduced | e.g. 1.0.0 or 2.1 |
| until version | Version of the data model when this variable value was deleted | e.g. 2.0.0 or 2.1 |


<sup>Table 3. Description of the columns that can be filled out for Variable values. * = mandatory</sup>

#### *Repeated variables* sheet

The *Repeated variables* sheet is optional. Variables that are repeats of a variable defined in the sheet *Variables* 
are defined in the *Repeated variables* sheet. Defining your repeated variables using this sheet
will limit the amount of information that has to be repeated when filling out repeated variables. This sheet is
optional.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| resource \* | Id of the <b>model</b> that contains this variable | e.g LifeCycle_CDM or ATHLETE_CDM |
| dataset \* | Dataset name | e.g. core |
| name \* | Variable name | e.g. height\_1 |
| label | Human readable variable label | |
| is repeat of.dataset \* | Dataset that contains the variable that is repeated | Datasets must be predefined in the _Datasets_ sheet |
| is repeat of.name \* | Name of the variable that is repeated | Variables must be predefined in the _Variables_ sheet |
| collection event.resource | Refer to the network that contains the collection event | e.g. LifeCycle |
| collection event.name | Refer to a collection event; The collection event needs to be predefined in the Collection events table in the network staging area | e.g. y1 or y2 |
| since version | Version of the data model when this variable was introduced | e.g. 1.0.0 or 2.1 |
| until version | Version of the data model when this variable was deleted | e.g. 2.0.0 or 2.1 |

<sup>Table 4. Description of the columns that can be filled out for Repeated variables. * = mandatory</sup>

### Request access

Send an email to [*molgenis support*](mailto:support@molgenis.org) to apply for an account to upload metadata to
the Data Catalogue.

### Upload metadata

When you log in to MOLGENIS Data Catalogue you will see a listing of databases that are accessible to you. Click on your
network's database to access it. Go to 'Up/Download' in the menu. Use 'browse' to select a template and 'upload' to
start uploading your metadata. After uploading you can view your metadata under 'Tables'.
