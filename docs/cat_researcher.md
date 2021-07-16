# Researcher

## Catalogue
When you define a research question it is time to find the relevant variables to answer it. You can use the [MOLGENIS Data Catalogue](https://data-catalogue.molgeniscloud.org/catalogue/catalogue/#/explorer/details) to compose your dataset. The Data Catalogue only describes the variables, it does **not** contain the actual data values.
### Find variables
You can use the filters and search bar to subset variables. In the example below four filters are used, one network filter and three topic filters.

![Variable explorer filters](img/cat-var-explorer-1.png)

You can search for variables (for example: `agebirth`) with filters in place.

![Variable explorer search](img/cat_search-variables.png)

*In the future you will be able to use the shopping card to select your variables of interest and create an overview.*


### Find harmonisation details
The harmonisation view allows you to see which cohorts have (partially) harmonised your variable of interest and thus have that variable available for analysis.

![Variable explorer harmonisation specifications](img/cat_harmonisation-specifications.png)

You can view how a specific cohort has harmonised a specific variable.

![Variable explorer cohort harmonisation specification](img/cat_cohort-harmonised-variable.png)

## Armadillo
[MOLGENIS Armadillo](https://github.com/molgenis/molgenis-r-armadillo) allows you to analyse data uploaded in the Armadillo in a federated way. This means that you send the analysis to the Armadillo and results come back from the Armadillo to the researcher.

Harmonized data uploaded by a cohort data manager into a local Armadillo/Opal instance to make it available for the researcher to run analysis.

### Use DataSHIELD
> DataSHIELD is an infrastructure and series of R packages that enables the remote and non-disclosive analyses of sensitive research data. Users are note required to have prior knowledge of R ([datashield.org](https://www.datashield.org/)).

In order to access and analyse data you need to log into a central login server as depicted in the example below.
![Armadillo LifeCycle Authentication](img/cat_armadillo-lifecycle-login.png)

We encourage users to move towards central authentication used by different consortia. Which means that we try to use as much of the institution accounts to login to all the cohort federated platforms. [ELIXIR](https://elixir-europe.org/) is a European platform which has contracts with most of the research institutes which allows you to login with your own account. This is used for all Armadillo instances, the MOLGENIS catalogue, and for the central analysis server as well. Visit: [https://elixir-europe.org/](https://elixir-europe.org/) for more information.

After successful authentication you need [DSMolgenisArmadillo](https://molgenis.github.io/molgenis-r-datashield/) installed locally on your machine in order to analyse data shared on [MOLGENIS Armadillo](https://github.com/molgenis/molgenis-service-armadillo) servers using DataSHIELD.

    install.packages("DSI")
    install.packages("DSMolgenisArmadillo")

### Request permissions
You need to request access by sending an email to the cohort data managers. You will need to specify which variables you want to use. Be specific about this. The cohort data manager can then grant you access to the data that you requested.

After the correct authentication and authorization steps have been set up you will be able to analyse the cohort’s data via DataSHIELD.

## Analysis environment

Some networks use the analysis environment instead or next to the locally installed MOLGENIS Armadillo. The analysis environment is centralized. You will be using a web based RStudio which is available here: [https://analysis.gcc.rug.nl](https://analysis.gcc.rug.nl).
![Rstudio analysis envirioment login](img/cat_rstudio-login.png)
### Request access
Access to the analysis environment is centralised, which means that you have to request access. To request access send an email to [molgenis-support@umcg.nl](mailto:molgenis-support@umcg.nl). Please specify for which cohort you work and the responsible PI.
### Use the analysis environment
You need at least 3 packages to work with DataSHIELD on the central analysis server:
- [dsBaseClient](https://github.com/datashield/dsBaseClient) → is the DataSHIELD analysis packages
- [DSOpal](https://github.com/datashield/DSOpal) → allowes you to connect to the Opals
- [DSMolgenisArmadillo](https://cran.r-project.org/web/packages/DSMolgenisArmadillo/index.html) → allows you to connect to the Armadilllo’s

These packages are pre-installed. You will not have to install them again. In the future we will support profiles which you can choose when you login.
