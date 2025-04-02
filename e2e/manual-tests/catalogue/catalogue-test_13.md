# Number

13

# Role

Naïve user of the data catalogue on different browsers: Mozilla Firefox, Microsoft Edge and Safari.

# Goal

A naïve visitor to the data catalogue on a different browsers like Mozilla Firefox, Microsoft Edge and Safari can click around through items in the menu ribbon and end up where they would expect. The catalogue (menu ribbon, blocks of information displayed, filters, etc) is displayed correctly.

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| ---- | ------ | --------------- |----------------- | --------------- |
| 0 | NB: Assumptions | This test plan assumes the user is viewing the catalogue on Mozilla Firefox. | | |
| 1 | Navigate to [catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/) | Landing page: European health research data and sample catalogue. The ribbon reads: `MOLGENIS` logo, `HOME`, `ALL COLLECTIONS`, `ALL VARIABLES`, `ALL NETWORKS` and `MORE` | | |
| 2 | Hover over the `MORE` button | It shows a dropdown with `UPLOAD DATA`, and `MANUALS` | | |
| 3 | Click on the `SEARCH ALL` button | The user goes to [/catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all) | | |
| 4 | | The ribbon reads: `MOLGENIS` logo, `COLLECTIONS`, `NETWORKS`, `VARIABLES`, `OTHER CATALOGUES` and `MORE`| | |
| 5 | Click on `COLLECTIONS` | The user goes to  [/catalogue/all/collections](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all/collections) | | |
| 6 | | The ribbon reads: `MOLGENIS` logo, `OVERVIEW`, `COLLECTIONS`, `NETWORKS`, `VARIABLES` and `MORE` | | |
| 7 | Scroll down the filters on the left | A search field is displayed at the top and filters below | | |    
| 8 | Click on the `COMPACT` button | List of collections changes to compact view and the URL changes to [?view=compact](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all/collections?view=compact) | | || | |
| 9 | Click on 'acronym for test cohort 1' | The user goes to overview page for this collection and the ribbon reads : `MOLGENIS` logo, `OVERVIEW`, `COLLECTIONS`, `NETWORKS`, `VARIABLES` and `MORE` | | |
| 10 | See that the fields are filled as follows: | https://www.molgenis.org/, `CONTACT` button, Description, General design, Population, Organisations, Contributors, Available Data & Samples, Subpopulations, Collection Events, Datasets, Networks, Publications, Access Conditions, Funding and acknowledgements, and Documentation | | |
| 11 | Click on `OVERVIEW` | The user returns to [/catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all) | | | 
| 12 | Click on `NETWORKS`| The user goes to [/catalogue/all/networks](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all/networks) | | |
| 13 | | The ribbon reads: `MOLGENIS` logo, `OVERVIEW`, `COLLECTIONS`, `NETWORKS`, `VARIABLES` and `MORE` | | |
| 14 | Scroll down the filters on the left | A search field is displayed at the top and filters below | | |
| 15 | Click on 'acronym for test network1' | The user goes to overview page for this network and the ribbon reads :`MOLGENIS` logo, `OVERVIEW`, `COLLECTIONS`, `NETWORKS`, `VARIABLES` and `MORE` | | |
| 16 | See that the fields are filled as follows: | Test logo for test network, (https://www.molgenis.org/), Description, General design, Population, Organisations, Datasets, Networks, Publications, Funding and acknowledgements | | |
| 17 | Click on `OVERVIEW` | The user goes to [/catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all) | | |
| 18 | Click on `VARIABLES` | The user goes to [/catalogue/all/variables](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all/variables) | | |
| 19 | | The ribbon reads: `MOLGENIS` logo, `OVERVIEW`, `COLLECTIONS`, `NETWORKS`, `VARIABLES` and `MORE` | | |
| 20 | Scroll down the filters on the left | A search field is displayed at the top and filters below | | |
| 21 | Click on `HARMONISATIONS` | Harmonisations matrix of all the variables are displayed | | |
| 22 | Press the back button on the browser | The user goes to [/all/variables](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all/variables) | | |
| 23 | Click on `VARIABLES` again | A list of all variables is displayed | | |
| 24 | Click on the first variable 'abd_circum_sdsWHO_t' | The user goes to [/variables/abd_circum_sdsWHO_t](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all/variables/abd_circum_sdsWHO_t-ATHLETE-outcome_ath-ATHLETE?keys={%22name%22:%22abd_circum_sdsWHO_t%22,%22resource%22:{%22id%22:%22ATHLETE%22},%22dataset%22:{%22name%22:%22outcome_ath%22,%22resource%22:{%22id%22:%22ATHLETE%22}}}) | | |
| 25 | See that the fields are filled as follows: | Definition, Harmonisation status per source and Harmonisation details per source | | |
| 26 | Click on `OTHER CATALOGUES` | | |
| 27 | Click on **OOM** under *Project Catalogues* | The user goes to [/catalogue/OOM](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/OOM). | | |
| 28 | | The ribbon reads: `MOLGENIS` logo, `COLLECTIONS`, `NETWORKS`, `VARIABLES`, `ABOUT` and `MORE` | | |
| 29 | Click on `MORE`->`Other Catalogues` | The user goes to [/catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/) | | |
| | [THIS EXTRA TEST STEP CAN BE RENDERED INVALID BY PRODUCTION DATA FOR LONGITOOLS BEING COPIED INTO ACC - SEE IT AS A BONUS STEP!] | | | |
| 30a | Click on **LongITools** under *Project Catalogues* | The user goes to [/catalogue/LongITools](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/LongITools). | | |
| 30b | | The ribbon reads: `MOLGENIS` logo, `COLLECTIONS`, `VARIABLES`, `ABOUT`, `OTHER CATALOGUES` and `MORE` | | |
| 31 | Navigate to [catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/) using Safari | Landing page: European health research data and sample catalogue. The ribbon reads: `MOLGENIS` logo, `HOME`, `ALL COLLECTIONS`, `ALL VARIABLES`, `ALL NETWORKS` and `MORE` | | |
| 32 | Repeat steps from 2 to 30b | | |
| 31 | Navigate to [catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/) using Microsoft Edge | Landing page: European health research data and sample catalogue. The ribbon reads: `MOLGENIS` logo, `HOME`, `ALL COLLECTIONS`, `ALL VARIABLES`, `ALL NETWORKS` and `MORE` | | |
| 32 | Repeat steps from 2 to 30b | | |

