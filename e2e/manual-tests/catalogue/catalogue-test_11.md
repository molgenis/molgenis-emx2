# Number

11

# Role

Naïve user of the data catalogue.

# Goal

A naïve visitor to the data catalogue can click around and search within 'all resources'.

# Steps

| Step | Action                                                                                                               | Expected result                                                                                                                 | Github bug/issue          | Playwright test |
| ---- | -------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------- | ------------------------- | --------------- |
| 0    | Navigate to the [all variables screen](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all) | End up on the overview page of the testNetwork1                                                                                 |                           |                 |
| 1    | Click on VARIABLES in the menu                                                                                       | End up at [the variable screen](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/variables)         |                           |                 |
| 2    | Go back using the browser back button                                                                                | End up at [the all variables](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all)                     |                           |                 |
| 3    | Click on the word VARIABLES underneath the logo                                                                      | End up at [the variable screen](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/variables)         |                           |                 |
| 4    | Go back by clicking the Molgenis logo in the top left                                                                | End up at [the all variables](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all)                     |                           |                 |
| 5    | Click on the variables CTA                                                                                           | End up at [the variable screen](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/variables)         |                           |                 |
| 6    | Check the breadcrumb                                                                                                 | It should be "ALL > VARIABLES"                                                                                                  |                           |
| 7    | Go back by clicking the ALL breadcrumb                                                                               | End up at [the all variables](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all)                     |                           |                 |
| 8    | Check the CTA's                                                                                                      | There should be 3, names: "Resources", "Networks", and "Variables"                                                              |                           |                 |
| 9    | Check the title above each CTA                                                                                       | They should match the text on their respective CTA's                                                                            |                           |                 |
| 10   | Check the description of the "Resources" CTA                                                                         | It should read "Collections of data available (cohort, studies, biobank...)"                                                    |                           |                 |
| 11   | Check the description of the "Networks" CTA                                                                          | It should read "Networks & Consortia"                                                                                           |                           |                 |
| 12   | Check the description of the "Variables" CTA                                                                         | It should read "Harmonised variables"                                                                                           |                           |                 |
| 13   | Check The ribbon at the top of the screen                                                                            | It should contain the options: "RESOURCES", "NETWORKS", "VARIABLES", and "MORE"                                                 |                           |                 |
| 14   | Mouse over "MORE"                                                                                                    | It should reveal the options: "Other catalogues", and "Upload Data"                                                             |                           |                 |
| 15   | Check the texts below the CTAs                                                                                       | It should contain the number of "Participants", "Samples". The percentage of "Longitudinal", and the number of "subpopulations" |                           |                 |
| 16   | Each card containing a CTA should be lined out correctly                                                             | Each should take up the same amount of space and the room between them should be consistent.                                    | Currently not the case    |                 |
| 17   | Click on the "Resources" CTA                                                                                         | End up at [the resources screen](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/resources)        |                           |                 |
| 18   | Click the filter for resource type                                                                                   | A list of possible resources to filter on should appear                                                                         | Currently not implemented |                 |
| 19   | ------                                                                                                               |                                                                                                                                 |                           |                 |
| 20   | ------                                                                                                               |                                                                                                                                 |                           |                 |
| 20   | ------                                                                                                               |                                                                                                                                 |                           |                 |
| 20   | ------                                                                                                               |                                                                                                                                 |                           |                 |
| 20   | ------                                                                                                               |                                                                                                                                 |                           |                 |
| 20   | ------                                                                                                               |                                                                                                                                 |                           |                 |
| 20   | ------                                                                                                               |                                                                                                                                 |                           |                 |

<!-- Test ook doorklikken naar resources en de pagina die je krijgt, extra filter in resources en moet werken -->
<!-- check breadcrumbs in doorklik pagina's -->
