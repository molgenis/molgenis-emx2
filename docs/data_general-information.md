# General information

We facilitate federated analysis. Federated analysis allows researchers to carry out analysis over data in multiple cohorts a) without having access to individual data and b) without individual data needing to leave the cohort. Federated analysis allows GDPR-proof research.

![Figure 1: Federated analysis](img/cat_federated-analysis.png)

To **access** the data you need the MOLGENIS Armadillo. This is a platform which uses [DataSHIELD](https://datashield.org/) to perform the analysis.

The Armadillo suite can be used by data stewards to share datasets on a server. Researchers can then analyse these datasets and datasets shared on other servers using the DataSHIELD analysis tools. Researchers will only be able to access aggregate information and cannot see individual rows.

 Learn more about [Armadillo suite](https://github.com/molgenis/molgenis-service-armadillo) and the [R client (MolgenisArmadillo)](https://github.com/molgenis/molgenis-r-armadillo).

We distinguish three roles within each federated network.

|     |     | [Researcher](data_researcher.md) | [Cohort data manager](data_cohort-data-manager.md) | [Network data manager](cat_network-data-manager.md) |
| --- | --- | --- | --- | --- |
| Armadillo | Initial data upload | | [X](data_cohort-data-manager.md#initial-upload) | |
| Armadillo | Request access | [X](data_researcher.md#request-federated-access) | [X](data_cohort-data-manager.md#request-access-armadillo) | |
| Armadillo | Request permissions | [X](data_researcher.md#request-permissions) | [X](data_cohort-data-manager.md#assign-permissions) | |
| Armadillo | Create subsets of the data | | [X](data_cohort-data-manager.md#create-subsets) | |
| Armadillo | Give permissions on the data | | [X](data_cohort-data-manager.md#assign-permissions) | |
| Armadillo | Quality control | | [X](data_cohort-data-manager.md#quality-control) | |
| Armadillo | Use DataSHIELD | [X](data_researcher.md#use-datashield) | | |
| Armadillo | Deploy an instance | | | [X](data_cohort-data-manager.md#deploy-an-instance) |
| Analysis environment | Request access | [X](data_researcher.md#request-analysis-access) | | |
