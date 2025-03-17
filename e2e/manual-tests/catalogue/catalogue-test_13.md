# Number

12

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
| 5 | Click on the 'Search all' button | The user goes to [/catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all) | | |
| 6 | | The ribbon reads: `MOLGENIS` logo, `COLLECTIONS`, `NETWORKS`, `VARIABLES`, `OTHER CATALOGUES` and `MORE`| | |
| 7 | Click on 'Collections' | The user goes to  [/catalogue/all/collections](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all/collections) | | |
| 8 | | The ribbon reads: `MOLGENIS` logo, `OVERVIEW`, `COLLECTIONS`, `NETWORKS`, `VARIABLES` and `MORE` | | |
| 26 | Scroll down 'Filters' | A search field is displayed at the top and filters below | | |    
| 11 | Click on the 'COMPACT' button | List of collections changes to compact view and the URL changes to [/view=compact](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all/collections?view=compact) | | || | |
| 13 | Click on 'acronym for test cohort 1' | The user goes to overview page for this collection and the ribbon reads : `MOLGENIS` logo, `OVERVIEW`, `COLLECTIONS`, `NETWORKS`, `VARIABLES` and `MORE` | | |
| 14 | See that the fields are filled as follows: | https://www.molgenis.org/, 'Contact' button, Description, General design, Population, Organisations, Contributors, Available Data & Samples, Subpopulations, Collection Events, Datasets, Networks, Publications, Access Conditions, Funding and acknowledgements, and Documentation | | |
| 16 | Click on `Overview` | The user returns to [/catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all) | | | 
| 17 | Click on 'Networks'| The user goes to [/catalogue/all/networks](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all/networks) | | |
| 18 | | The ribbon reads: `MOLGENIS` logo, `OVERVIEW`, `COLLECTIONS`, `NETWORKS`, `VARIABLES` and `MORE` | | |
| 26 | Scroll down 'Filters' | A search field is displayed at the top and filters below | | |
| 21 | Click on 'acronym for test network1' | The user goes to overview page for this network and the ribbon reads :`MOLGENIS` logo, `OVERVIEW`, `COLLECTIONS`, `NETWORKS`, `VARIABLES` and `MORE` | | |
| 22 | See that the fields are filled as follows: | Test logo for test network, (https://www.molgenis.org/), Description, General design, Population, Organisations, Datasets, Networks, Publications, Funding and acknowledgements | | |
| 24 | Click on `Overview` | The user goes to [/catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all) | | |
| 25 | Click on 'Variables` | The user goes to [/catalogue/all/variables](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all/variables) | | |
| 18 | | The ribbon reads: `MOLGENIS` logo, `OVERVIEW`, `COLLECTIONS`, `NETWORKS`, `VARIABLES` and `MORE` | | |
| 26 | Scroll down 'Filters' | A search field is displayed at the top and filters below | | |
| 28 | Click on 'Harmonisations' | Harmonisations matrix of all the variables are displayed | | |
| 29 | Press the back button on the page | The user goes to [/catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all) | | |
| 30 | Click on 'Variables' again | A list of all variables is displayed | | |
| 32 | Click on the first variable 'abd_circum_sdsWHO_t' | The user goes to [/variables/abd_circum_sdsWHO_t](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all/variables/abd_circum_sdsWHO_t-ATHLETE-outcome_ath-ATHLETE?keys={%22name%22:%22abd_circum_sdsWHO_t%22,%22resource%22:{%22id%22:%22ATHLETE%22},%22dataset%22:{%22name%22:%22outcome_ath%22,%22resource%22:{%22id%22:%22ATHLETE%22}}}) | | |
| 33 | See that the fields are filled as follows: | Definition, Harmonisation status per source and Harmonisation details per source | | |
| 34 | Click on `OTHER CATALOGUES` | | |
| 35 | Click on **OOM** under *Project Catalogues* | The user goes to [/catalogue/OOM](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/OOM). | | |
| 36 | | The ribbon reads: `MOLGENIS` logo, `COLLECTIONS`, `NETWORKS`, `VARIABLES`, `ABOUT` and `MORE` | | |
| 38 | Click on `MORE`->`Other Catalogues` | The user goes to [/catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/) | | |
| | [THIS EXTRA TEST STEP CAN BE RENDERED INVALID BY PRODUCTION DATA FOR LONGITOOLS BEING COPIED INTO ACC - SEE IT AS A BONUS STEP!] | | | |
| 39a | Click on **LongITools** under *Project Catalogues* | The user goes to [/catalogue/LongITools](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/LongITools). | | |
| 39b | | The ribbon reads: `MENU`, Molegenis logo at the top and below `COLLECTIONS`, `VARIABLES`, `ABOUT`, `OTHER CATALOGUES` | | |
