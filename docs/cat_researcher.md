# Researcher

## Catalogue
When you define your research question it is time to find the relevant variables to answer this question. You can use the [MOLGENIS catalogue](https://data-catalogue.molgeniscloud.org/catalogue/catalogue/#/explorer/details) to compose your dataset of variables. The catalogue only describes the variables. It does not contain the actual data.
### Find (harmonised) variables
You can use the filters and search bar to subset the variables.

![Variable explorer filters](https://github.com/molgenis/molgenis-emx2/blob/feat/add-docs/docs/img/cat-var-explorer-1.png)

You can search on the variables.

*screenshot 2*

In the future you will be able to use the shopping card to select all variables of interest and create an overview.

*screenshot 3*

### Find harmonisation specifications
The harmonisation view allows you to see for which cohorts the variable of your interest is (partially) harmonized and available for analysis.

*screenshot 4*

You can view how a specific cohort has harmonised a specific variable.

*screenshot 5*
## Armadillo
The Armadillo allows you to analyse data uploaded in the Armadillo in a federated way. This means that you send the analysis to the Armadillo and results come back from the Armadillo to the researcher.

Harmonized data uploaded by a cohort data manager into a local Armadillo/Opal instance to make it available for the researcher to run analysis.
### Request permissions
You need to request access by sending an email to the cohort data managers. You will need to specify which variables you want to use. Be specific about this. The cohort data manager can then grant you access to the data that you requested.

After the correct authentication and authorization steps have been set up you will be able to analyse the cohort’s data via DataSHIELD.
### Use DataSHIELD
Login on the

[https://www.datashield.org/](https://www.datashield.org/)

[https://molgenis.github.io/molgenis-r-datashield](https://molgenis.github.io/molgenis-r-datashield)
## Analysis environment
The analysis environment is centralized. You will be using a web based RStudio which is available here: [https://analysis.gcc.rug.nl](https://analysis.gcc.rug.nl).
### Request access
The access to the analysis environment is centralized. Which means that you have to request access. To request access you can send an email to [molgenis-support@umcg.nl](mailto:molgenis-support@umcg.nl). Please specify for which cohort you work and the responsible PI.
### Use the analysis environment
You need at least 3 packages to work with DataSHIELD on the central analysis server:
- [dsBaseClient](https://github.com/datashield/dsBaseClient) → is the DataSHIELD analysis packages
- [DSOpal](https://github.com/datashield/DSOpal) → allowes you to connect to the Opals
- [DSMolgenisArmadillo](https://cran.r-project.org/web/packages/DSMolgenisArmadillo/index.html) → allowes you to connect to the Armadilllo’s

These packages are preinstalled. You won’t have to install them again. In the future we are going to support profiles which you can choose when you logon.
