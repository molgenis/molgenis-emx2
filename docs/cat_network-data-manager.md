# Network data manager
Each cohort needs to organise together with their IT department all the necessary technical server requirements needed for federated analysis if this is a requirement of the consortium.
## Catalogue

### Describe and upload common data model
MOLGENIS Data Catalogue (sometimes also called 'EMX2 catalogue') provides a framework to describe in detail cohort metadata, descriptions of the data variables collected (aka 'source variables'), and mappings to common data models (aka 'target variables). Its purpose is to facilitate pooled data analysis of multiple cohorts together.

- The cohort metadata provides descriptive information such as contact details, name of the cohort, and high level summary of contents and cohort design.
- The variable metadata (or 'source variables') can be considered as a codebook or data dictionary of a cohort (e.g. ALSPAC).
- Similarly, the common data model metadata (or 'target variables') can be considered the codebook of a network of cohorts working together (e.g. LifeCycle)
- The mappings describe how source variables have been converted into target variables as basis for integrated analysis.

This document explains how to submit the 'target variables' (also called the harmonized model) into the Data Catalogue. Expected users of this 'how to' are central data managers of networks such as LifeCycle or LongITools. This document assumes you have received login details to access MOLGENIS Data Catalogue.
### Define variable metadata using the *NetworkDictionary* template
The _NetworkDictionary_ template consists of multiple sheets. Each sheet corresponds to a table in the Data Catalogue.The columns in the sheet correspond to columns in the table concerned. This document describes how to fill out each of the sheets and their columns. A column with an asterisk (\*) after its name is mandatory, i.e., it should contain values for the system to accept a data upload. Note that there is no sheet for *AllTargetVariables*. This table is a generic listing of all variables entered for the cohort; it shows *TargetVariables* and *RepeatedTargetVariables* in one table.

It is good practice to try out adding a few variables to the template first and see whether your upload succeeds. To upload the metadata to the Data Catalogue see the section [Upload template](https://github.com/molgenis/molgenis-emx2/blob/master/docs/resources/NetworkDictionary.xlsx) to the [Data Catalogue](https://data-catalogue-staging.molgeniscloud.org).

![](img/cat_tables-in-catalogue.png)

<sub><sup>*Figure 1. Tables in a Network’s staging area in the Data Catalogue.*</sup></sub>
#### *Releases* sheet
Versioning of the network's data model is defined in the *Releases* sheet. Columns with an asterisk (\*) after their name are mandatory.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| resource \* | Name of the network. | e.g LifeCycle, LONGITOOLS or ATHLETE |
| version \* | Version of the data release. | e.g. 1.1 |

<sub><sup>*Table 1. Description of the columns that can be filled out for Releases. \* = mandatory*</sup></sub>

![Figure 2. Example of Releases filled out in Excel.](img/cat_target-release-sheet.png ':size=150')

<sub><sup>*Figure 2. Example of Releases filled out in Excel.*</sup></sub>
#### *TargetTables* sheet
The cohort tables are defined in the *SourceTables* sheet. Columns with an asterisk (\*) after their name are mandatory.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| release.resource \* | Name of the network. | e.g LifeCycle, LONGITOOLS or ATHLETE |
| release.version \* | Release version this table is part of. | e.g. 1.0.1 |
| name \* | Unique table name. | |
| label | Table label. | |
| description | Table description. | |

<sub><sup>*Table 2. Description of the columns that can be filled out for TargetTables. \* = mandatory*</sup></sub>

![Figure 3. Example of TargetTables filled out in Excel.](img/cat_target-tables-sheet.png ':size=600')

<sub><sup>*Figure 3. Example of TargetTables filled out in Excel.*</sup></sub>
#### *TargetVariables* sheet
The cohort variables are defined in the *TargetVariables* sheet.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| release.resource \* | Name of the network that contains this variable. | e.g LifeCycle, LONGITOOLS or ATHLETE |
| release.version \* | Release version this variable is part of. | e.g. 1.0.1 |
| table \* | Table that contains the variable. | Tables must be predefined in the TargetTables sheet. |
| name \* | Variable name, unique within a table. | |
| label | Human readable variable label. | |
| format | The data type of the variable. | Find list to choose from in CatalogueOntologies \&gt; Formats |
| unit | Unit in case of a continuous or integer format. | Find list to choose from in CatalogueOntologies \&gt; Units1 |
| description | Description of the variable. | |
| keywords | Enables grouping of variables into topics and displaying in a tree. | Find list to choose from in CatalogueOntologies \&gt; Keywords1 |
| exampleValues | Examples of values in a comma separated list. | Makes your data more insightful. E.g. 1,2,3 or TRUE,FALSE or 1.23,4.56,3.14 |
| mandatory | Whether this variable is required within this collection. | |
| vocabularies | Refer to ontologies being used. | Find list to choose from in CatalogueOntologies \&gt; Vocabularies1, e.g. ICD10 |
| collectionEvent.resource | Refer to the resource that contains the collectionEvent. | e.g. LifeCycle |
| collectionEvent.name | Refer to a collection event. | e.g. y1 or y2 |

<sub><sup>*Table 3. Description of the columns that can be filled out for TargetVariables. \* = mandatory;* *1* *contact* [*molgenis-support@umcg.nl*](mailto:molgenis-support@umcg.nl) *to add Vocabularies, Keywords or Units*</sup></sub>

![](img/cat_target-variables-sheet.png ':size=950')

![](img/cat_keywords-sheet.png ':size=650')

<sub><sup>*Figure 4. Example of TargetVariables filled out in Excel.*</sup></sub>
#### *TargetVariableValues* sheet
The coding of categorical variables is defined in the *TargetVariableValues* sheet. This sheet is optional, but it is highly recommended to fill out the codes and values for your categorical variables, so that your data becomes more insightful for those that are interested.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| release.resource \* | Name of the network that contains this variable. | e.g LifeCycle, LONGITOOLS or ATHLETE |
| release.version \* | Release version this variable is part of. | e.g. 1.0.1 |
| variable.table \* | Table that contains the variable. | Tables must be predefined in the TargetTables sheet. |
| variable.name \* | Variable name. | Variables must be predefined in the TargetVariables sheet. |
| value \* | The code or value used. | e.g. 1, 2 or -99 |
| label \* | The label corresponding to the value. | e.g. 'yes', 'no' or 'NA' |
| order | The order in which the code list should appear. | e.g. 1 |
| isMissing | Whether this value indicates a missing field. | TRUE or FALSE |
| ontologyTermIRI | Reference to an ontology term that defines this categorical value. | e.g. http://purl.obolibrary.org/obo/DOID\_1094 |

<sub><sup>*Table 4. Description of the columns that can be filled out for TargetVariableValues. \* = mandatory*</sup></sub>

![](img/cat_target-variable-sheet.png ':size=650')

<sub><sup>*Figure 5. Example of TargetVariableValues filled out in Excel.*</sup></sub>
#### *RepeatedTargetVariables* sheet
The *RepeatedTargetVariables* sheet is optional.Variables that are repeats of a variable defined in the sheet _TargetVariables_ are defined in the *RepeatedTargetVariables* sheet. Defining your repeated variables using this sheet will limit the amount of information that has to be repeated when filling out repeated variables. This sheet is optional.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| release.resource \* | Name of the network that contains this variable. | e.g LifeCycle, LONGITOOLS or ATHLETE |
| release.version \* | Release version this variable is part of. | e.g. 1.0.1 |
| table \* | Table name. | e.g. core |
| name \* | Variable name. | e.g. height\_1 |
| isRepeatOf.table \* | Table that contains the variable that is repeated. | Tables must be predefined in the _TargetTables_ sheet; e.g. core |
| isRepeatOf.name \* | Name of the variable that is repeated. | Variables must be predefined in the _TargetVariables_ sheet; e.g. height\_0 |
| collectionEvent.resource | Refer to the network that contains the collection event. | e.g. LifeCycle |
| collectionEvent.name | Refer to the name of a collection event. | The collectionEvent needs to be predefined in the *CollectionEvents* sheet; e.g. y1 or y2 |

<sub><sup>*Table 5. Description of the columns that can be filled out for RepeatedTargetVariables. \* = mandatory*</sup></sub>

![](img/cat_target-repeated-variable-sheet.png ':size=650')

<sub><sup>*Figure 6. Example of RepeatedTargetVariables filled out in Excel.*</sup></sub>

#### *CollectionEvents* sheet

The *CollectionEvents* sheet is optional. The timing of data collection in events is defined in the *CollectionEvents* sheet. It can be used to describe time periods within which the data for variables are collected. The events are defined here and referred to from the sheets *TargetVariables* and/or *RepeatedTargetVariables*.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| resource \* | Name of the network that contains this collection event. | e.g LifeCycle, LONGITOOLS or ATHLETE |
| name \* | Name of the collection event. | e.g. y9 |
| description | Event description. | e.g. Between 9 and 10 years. |
| ageMin | The minimum age for this collection event. | Find list to choose from in CatalogueOntologies \&gt; AgeCategories1 |
| ageMax | The maximum age for this collection event. | Find list to choose from in CatalogueOntologies \&gt; AgeCategories1 |
| subcohorts | Subcohorts or subpopulations that are targeted with this variable. | Subcohorts need to be predefined in the _Subcohorts_ sheet. |

<sub><sup>*Table 6. Description of the columns that can be filled out for CollectionEvents. \* = mandatory;* *1* *contact* [*molgenis-support@umcg.nl*](mailto:molgenis-support@umcg.nl) *to add AgeCategories*</sup></sub>

![](img/cat_target-collection-events-sheet.png ':size=500')

<sub><sup>*Figure 7. Example of CollectionEvents filled out in Excel.*

#### *Subcohorts* sheet
The sheet *Subcohorts* is optional. Here you may describe populations that can be linked to collection events.

| *Column name* | *Description* | *Remarks* |
| --- | --- | --- |
| resource | Name of the network that contains this subcohort. | e.g LifeCycle, LONGITOOLS or ATHLETE |
| name \* | Name of the subpopulation or subcohort. | e.g. mothers or children |
| description | Subpopulation description | |

<sub><sup>*Table 7. Description of the columns that can be filled out for Subpopulations. \* = mandatory*</sup></sub>

![](img/cat_target-subcohorts-sheet.png ':size=300')

<sub><sup>*Figure 8. Example of Subcohorts filled out in Excel.*</sup></sub>
### Request access to the catalogue
Send an email to [*molgenis-support@umcg.nl*](mailto:molgenis-support@umcg.nl) to apply for an account on the catalogue server.

### Upload to the catalogue
When you log in to MOLGENIS Data Catalogue you will see a listing of databases that are accessible to you. Click on your network's database to access it. Move to 'Up/Download' via the menu. Use 'browse' to select a template and 'upload' to start uploading your data. You can now view your data under 'Tables'.
## Armadillo
The [Armadillo](https://github.com/molgenis/molgenis-service-armadillo/blob/master/README.md) is an application which allows you to share your data in a federated way.

### Deploy an instance
To get the Armadillo installed at your institute you need to contact your IT-department. The installation manual you can find here: https://galaxy.ansible.com/molgenis/armadillo. 

The system administrator needs to have specific information to setup the Armadillo. Each Armadillo is bound to a central authentication server. There needs to be an entry in this central authentication server for the Armadillo. You can email [*molgenis-support@umcg.nl*](mailto:molgenis-support@umcg.nl) to get the specific information that applies to your Armadillo instance.
