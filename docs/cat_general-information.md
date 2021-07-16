# General information
We facilitate federated analysis. Federated analysis allows researchers to carry out analysis over data in multiple cohorts a) without having access to individual data and b) without individual data needing to leave the cohort. Federated analysis allows GDPR-proof research.

![Figure 1: Federated analysis](img/cat-federated-analysis.png)

To **find** the data we developed a [catalogue](https://data-catalogue.molgeniscloud.org/catalogue/catalogue/#/explorer/details). Here you can find the metadata of the cohorts and the common data model. To **access** the data you need the MOLGENIS Armadillo. This is a platform which uses [DataSHIELD](https://datashield.org/) to perform the analysis.

We distinguish three roles within each federated network.

|     |     | [Researcher](cat_researcher.md) | [Cohort data manager](cat_cohort-data-manager.md) | [Network data manager](cat_network-data-manager.md) |
| --- | --- | --- | --- | --- |
| Catalogue | Data harmonisation | | [X](cat_cohort-data-manager.md#data-harmonisation) | |
| Catalogue | Cohort metadata description | | [X](cat_cohort-data-manager.md#) | |
| Catalogue | Describe and upload common data model | | | [X](cat_network-data-manager.md#describe-and-upload-common-data-model) |
| Catalogue | Find (harmonised) variables | [X](cat_researcher.md#find-harmonised-variables) | | |
| Catalogue | Find harmonisation specifications | [X](cat_researcher.md#find-harmonisation-specifications) | [X](cat_cohort-data-manager.md#find-harmonisation-specifications) | |
| Catalogue | Request access | | [X](cat_cohort-data-manager.md#) | X |
| Armadillo | Initial data upload | | [X](cat_cohort-data-manager.md#initial-data-upload) | |
| Armadillo | Create subsets of the data | | [X](cat_cohort-data-manager.md#create-subsets-of-the-data) | |
| Armadillo | Give permissions on the data | | [X](cat_cohort-data-manager.md#give-permissions-on-the-data) | |
| Armadillo | Quality control | | [X](cat_cohort-data-manager.md#quality-control) | |
| Armadillo | Use DataSHIELD | [X](cat_researcher.md#use-datashield) | | |
| Armadillo | Request permissions | [X](cat_researcher.md#request-permissions) | [X](cat_cohort-data-manager.md#request-permissions) | |
| Armadillo | Deploy an instance | | | [X](cat_network-data-manager.md#request-access) |
| Analysis environment | Request access | [X](cat_researcher.md#request-access) | | |

