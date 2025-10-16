# Test Plan 12

## Role

Naïve user of the data catalogue on a small screen

## Goal

A naïve visitor to the data catalogue on a small screen can click around through items in the menu ribbon
and end up where they would expect.
The catalogue (menu ribbon, blocks of information displayed, filters, etc) is displayed correctly
when viewed in 'small-screen' mode.

## Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| ---- | ------ | --------------- |----------------- | --------------- |
| 0 | NB: Assumptions | This test plan assumes the user is viewing the catalogue on a small screen. | | |
| 1 | Navigate to `https://data-catalogue-acc.molgeniscloud.org` | Landing page: European health research data and sample catalogue. The ribbon reads: `MENU` and `MOLGENIS` logo at the top and `HOME`, `ALL COLLECTIONS`, `ALL VARIABLES`, `ALL NETWORKS` below | | |
| 2 | Hover over the `MENU` button | It stays the same size | | |
| 3 | Click on the `MENU` button | A pop-up opens listing `HOME`, `ALL COLLECTIONS`, `ALL VARIABLES`, `ALL NETWORKS`, `UPLOAD DATA`, `MANUALS` and `ABOUT` | | |
| 4 | Close the pop-up | | | |
| 5 | Click on the 'Search all' button | The user goes to [/all](https://data-catalogue-acc.molgeniscloud.org/all) | | |
| 6 | | The ribbon reads: `MENU` and `MOLGENIS` logo at the top, Below: `COLLECTIONS`, `NETWORKS`, `VARIABLES`, `OTHER CATALOGUES` | | |
| 7 | Click on 'Collections' | The user goes to  [/all/collections](https://data-catalogue-acc.molgeniscloud.org/all/collections) | | |
| 8 | | The ribbon reads: `MENU` and `MOLGENIS` logo at the top, Below: `OVERVIEW`, `COLLECTIONS`, `NETWORKS`, `VARIABLES` | | |
| 9 | Click on 'Filters' and a pop-up opens. | A search field is displayed at the top and filters below | | |
| 10 | Close the pop-up | | | |
| 11 | Click on the 'View' button | List of collections changes to compact view and the URL changes to [/all/collections?view=compact](https://data-catalogue-acc.molgeniscloud.org/all/collections?view=compact) | | |
| 12 | Click on the 'View' button again | List of collections goes back to detailed view and the URL changes to [/all/collections?view=detailed](https://data-catalogue-acc.molgeniscloud.org/all/collections?view=detailed) | | |
| 13 | Click on 'acronym for test cohort 1' | The user goes to overview page for this collection and the ribbon reads :`MENU` and `MOLGENIS` logo at the top, Below: `OVERVIEW`, `COLLECTIONS`, `NETWORKS`, `VARIABLES` | | |
| 14 | See that the following blocks are shown: | logo, <https://www.molgenis.org/>, `CONTACT` button, Description, General Design, Population, Organisations, Contributors, Available Data & Samples, Dataset Variables, Subpopulations, Collection Events, Networks, Publications, Access Conditions, Funding & Acknowledgements, and Documentation | | |
| 15 | Click on the back arrow on the page| The user goes to  [/all/collections](https://data-catalogue-acc.molgeniscloud.org/all/collections) | | |
| 16 | Click the back arrow on the page again | The user returns to [/all](https://data-catalogue-acc.molgeniscloud.org/all) | | |
| 17 | Click on 'Networks'| The user goes to [/all/networks](https://data-catalogue-acc.molgeniscloud.org/all/networks) | | |
| 18 | | The ribbon reads: `MENU` and `MOLGENIS` logo at the top, Below: `OVERVIEW`, `COLLECTIONS` `NETWORKS`, `VARIABLES` | | |
| 19 | Click on 'Filters' and a pop-up opens. | A search field is displayed at the top and filters below | | |
| 20 | Close the pop-up | | | |
| 21 | Click on 'acronym for test network1' | The user goes to overview page for this network and the ribbon reads :`MENU` and `MOLGENIS` logo at the top, Below: `OVERVIEW`, `COLLECTIONS`, `NETWORKS`, `VARIABLES` | | |
| 22 | See that the fields are filled as follows: | Test logo for test network, <https://www.molgenis.org/>, Description, General design, Population, Organisations, Contributors Dataset variables, Networks, Publications, Funding & acknowledgements | | |
| 23 | Press the back button on the page | The user goes to [/all/networks](https://data-catalogue-acc.molgeniscloud.org/all/networks) | | |
| 24 | Press the back button again | The user goes to [/all](https://data-catalogue-acc.molgeniscloud.org/all) | | |
| 25 | Click on 'Variables` | The user goes to [/all/variables](https://data-catalogue-acc.molgeniscloud.org/all/variables) | | |
| 26 | Click on 'Filters' and a pop-up opens. | A search field is displayed at the top and filters below | | |
| 27 | Close the pop-up | | | |
| 28 | Click on 'Harmonisations' | Harmonisations matrix of all the variables are displayed | | |
| 29 | Press the back button on the page | The user goes to [/all](https://data-catalogue-acc.molgeniscloud.org/all) | | |
| 30 | Click on 'Variables' again | A list of all variables is displayed | | |
| 31 | | The ribbon reads: `MENU` and `MOLGENIS` logo at the top, Below: `OVERVIEW`, `COLLECTIONS`, `NETWORKS`,  and `VARIABLES` | | |
| 32 | Click on the first variable 'abd_circum_sdsWHO_t' | The user goes to [/all/variables/abd_circum_sdsWHO_t...](https://data-catalogue-acc.molgeniscloud.org/all/variables/abd_circum_sdsWHO_t-ATHLETE-outcome_ath-ATHLETE?keys={%22name%22:%22abd_circum_sdsWHO_t%22,%22resource%22:{%22id%22:%22ATHLETE%22},%22dataset%22:{%22name%22:%22outcome_ath%22,%22resource%22:{%22id%22:%22ATHLETE%22}}}) | | |
| 33 | See that the fields are filled as follows: | Definition, Harmonisation status per source and Harmonisation details per source | | |
| 34 | Click on `OTHER CATALOGUES` again | | | |
| 35 | Click on **OOM** under *Project Catalogues* | The user goes to [/OOM](https://data-catalogue-acc.molgeniscloud.org/OOM). | | |
| 36 | | The ribbon reads: `MENU`, test logo at the top (or Molgenis logo if no test logo is available) and below `COLLECTIONS`, `NETWORKS`, `VARIABLES`, and `ABOUT` | | |
| 37 | Click on `MENU` | A pop-up opens with options: `COLLECTIONS`, `NETWORKS`, `VARIABLES`, `ABOUT`, `OTHER CATALOGUES`, `UPLOAD DATA`, `MANUALS` | | |
| 38 | Click on `Other Catalogues` | The user goes to [/](https://data-catalogue-acc.molgeniscloud.org/) | | |
| | [THIS EXTRA TEST STEP CAN BE RENDERED INVALID BY PRODUCTION DATA FOR LONGITOOLS BEING COPIED INTO ACC - SEE IT AS A BONUS STEP!] | | | |
| 39a | Click on **LongITools** under *Project Catalogues* | The user goes to [/LongITools](https://data-catalogue-acc.molgeniscloud.org/LongITools). | | |
| 39b | | The ribbon reads: `MENU`, Molegenis logo at the top and below `COLLECTIONS`, `VARIABLES`, `ABOUT`, `OTHER CATALOGUES` | | |
