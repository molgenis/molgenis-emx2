# General information
We facilitate federated analysis. Federated analysis allows researchers to carry out analysis over data in multiple cohorts a) without having access to individual data and b) without individual data needing to leave the cohort. Federated analysis allows GDPR-proof research.

![Figure 1: Federated analysis](https://github.com/molgenis/molgenis-emx2/blob/feat/add-docs/docs/img/cat-federated-analysis.png)

To **find** the data we developed a [catalogue](https://data-catalogue.molgeniscloud.org/catalogue/catalogue/#/explorer/details). Here you can find the metadata of the cohorts and the common data model. To **access** the data you need the MOLGENIS Armadillo. This is a platform which uses [DataSHIELD](https://datashield.org/) to perform the analysis.

*TODO: FAIR principles*

We distinguish 3 roles within each federated network.

|     |     | Researcher | Cohort data manager | Network data manager |
| --- | --- | --- | --- | --- |
| Catalogue | Data harmonisation | | X | |
| Catalogue | Cohort metadata description | | X | |
| Catalogue | Describe common data model | | | X |
| Catalogue | Find (harmonised) variables | X | | |
| Catalogue | Find harmonisation specifications | X | X | |
| Catalogue | Request access | | X | X |
| Armadillo | Initial data upload | | X | |
| Armadillo | Create subsets of the data | | X | |
| Armadillo | Give permissions on the data | | X | |
| Armadillo | Quality control | | X | |
| Armadillo | Use DataSHIELD | X | | |
| Armadillo | Request permissions | X | X | |
| Analysis environment | Request access | X | | |

