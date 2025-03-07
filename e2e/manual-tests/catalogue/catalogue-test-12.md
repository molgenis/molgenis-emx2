# Number

12

# Role

Naïve user of the data catalogue on a small screen

# Goal

A naïve visitor to the data catalogue on a small screen can click around through items in the menu ribbon and end up where they would expect. The catalogue (menu ribbon, blocks of information displayed, filters, etc) is displayed correctly when viewed in 'small-screen' mode.

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| ---- | ------ | --------------- |----------------- | --------------- |
| 0 | NB: Assumptions | This test plan assumes the user is viewing the catalogue on a small screen. Open the link on Google chrome and press the 'Restore' button to view it on a smaller screen | | |
| 1 | Navigate to [catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/) | Landing page: European health research data and sample catalogue. The ribbon reads: `MENU` and `MOLGENIS` logo at the top and `HOME`, `ALL COLLECTIONS`, `ALL VARIABLES`, `ALL NETWORKS` below | | |
| 2 | Hover over the `MENU` button | It stays the same size| | |
| 3 | Click on the `MENU` button | A pop-up opens listing `HOME`, `ALL COLLECTIONS`, `ALL VARIABLES`, `ALL NETWORKS`, `UPLOAD DATA`, and `MANUALS`| | |
| 4 | Close the pop-up | | |
| 5 | Click on the 'Search all' button | The user goes to [/catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all) | | |
| 6 | | The ribbon reads: `MENU` and `MOLGENIS` logo at the top, Below: `COLLECTIONS`, `NETWORKS`, `VARIABLES`, `OTHER CATALOGUES` | | |
| 7 | Click on 'Collections' | The user goes to  [/catalogue/all/collections](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all/collections) | | |
| 8 | | The ribbon reads: `MENU` and `MOLGENIS` logo at the top, Below: `OVERVIEW`, `COLLECTIONS`, `NETWORKS`, `VARIABLES` | | |
| 9 | Click on 'Filters' and a pop-up opens. | A search field is disaplyed at the top and filters below | | |
| 10 | Close the pop-up | | |
| 11 | Click on the 'View' button | List of collections changes to compact view and the URL changes to [/view=compact](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all/collections?view=compact) | | || | |
| 12 | Click on the 'View' button again | List of collections goes back to detailed view and the URL changes to [/view=detailed](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all/collections?view=detailed) | | |
| 13 | Click on 'acronym for test cohort 1' | The user goes to overview page for this collection and the ribbon reads :`MENU` and `MOLGENIS` logo at the top, Below: `OVERVIEW`, `COLLECTIONS`, `NETWORKS`, `VARIABLES` | | |
| 14 | See that the fields are filled as follows: | https://www.molgenis.org/, 'Contact' button, Description, General design, Population, Organisations, Contributors, Available Data & Samples, Subpopulations, Collection Events, Datasets, Networks, Publications, Access Conditions, Funding and acknowledgements, and Documentation | | |
| 15 | Click on the back arrow on the page| The user goes to  [/catalogue/all/collections](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all/collections) | | |
| 16 | Click the back arrow on the page again | The user returns to [/catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all) | | | 
| 17 | Click on 'Networks'| The user goes to [/catalogue/all/networks](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all/networks) | | |
| 18 | | The ribbon reads: `MENU` and `MOLGENIS` logo at the top, Below: `OVERVIEW`, `COLLECTIONS` `NETWORKS`, `VARIABLES` | | |
| 19 | Click on 'Filters' and a pop-up opens. | A search field is displayed at the top and filters below | | |
| 20 | Close the pop-up | | | |
| 21 | Click on 'acronym for test network1' | The user goes to overview page for this network and the ribbon reads :`MENU` and `MOLGENIS` logo at the top, Below: `OVERVIEW`, `COLLECTIONS`, `NETWORKS`, `VARIABLES` | | |
| 22 | See that the fields are filled as follows: | Test logo for test network,(https://www.molgenis.org/), Description, General design, Population, Organisations, Datasets, Networks, Publication, Funding and acknowledgements | | |
| 23 | Press the back button on the page | The user goes to [/catalogue/all/networks](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all/networks) | | |
| 24 | Press the back button again | The user goes to [/catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all) | | |
| 25 | Click on 'Variables` | The user goes to [/catalogue/all/variables](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all/variables) | | |
| 26 | Click on 'Filters' and a pop-up opens. | A search field is displayed at the top and filters below | | |
| 27 | Close the pop-up | | | |
| 28 | Click on 'Harmonisations' | Harmonisation matrix of all the variables are displayed | | |
| 29 | Press the back button on the page | The user goes to [/catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all) | | |
| 30 | Click on 'Variables' again | A list of all variables is displayed | | |
| 31 | | The ribbon reads: `MENU` and `MOLGENIS` logo at the top, Below: `COLLECTIONS`, `NETWORKS`, `VARIABLES` and `OTHER CATALOGUES` | | |
| 32 | Click on the first variable 'abd_circum_sdsWHO_t' | The user goes to [/variables/abd_circum_sdsWHO_t](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all/variables/abd_circum_sdsWHO_t-ATHLETE-outcome_ath-ATHLETE?keys={%22name%22:%22abd_circum_sdsWHO_t%22,%22resource%22:{%22id%22:%22ATHLETE%22},%22dataset%22:{%22name%22:%22outcome_ath%22,%22resource%22:{%22id%22:%22ATHLETE%22}}}) | | |
| 33 | See that the fields are filled as follows: | Definition, Harmonisation status per source and Harmonisation details per source | | |
| 34 | Click on `OTHER CATALOGUES` again | | |
| 35 | Click on **OOM** under *Project Catalogues* | The user goes to [/catalogue/OOM](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/OOM). | | |
| 36 | | The ribbon reads: `MENU`, test logo at the top (or Molgenis logo if no test logo is available) and below `COLLECTIONS`, `NETWORKS`, `VARIABLES`, and `ABOUT` | | |
| 37 | Click on `MENU` | A pop-up opens with options: `COLLECTIONS`, `NETWORKS`, `VARIABLES`, `ABOUT`, `OTHER CATALOGUES`, `UPLOAD DATA`, `MANUALS` | | |
| 38 | Click on `Other Catalogues` | The user goes to [/catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/) | | |
| | [THIS EXTRA TEST STEP CAN BE RENDERED INVALID BY PRODUCTION DATA FOR LONGITOOLS BEING COPIED INTO ACC - SEE IT AS A BONUS STEP!] | | | |
| 39a | Click on **LongITools** under *Project Catalogues* | The user goes to [/catalogue/LongITools](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/LongITools). | | |
| 39b | | The ribbon reads: `MENU`, Molegenis logo at the top and below `COLLECTIONS`, `VARIABLES`, `ABOUT`, `OTHER CATALOGUES` | | |
