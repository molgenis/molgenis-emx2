# Number

8

# Role

Naïve user of the data catalogue

# Goal

A naïve visitor to the data catalogue can click around through items in the menu ribbon and end up where they would expect

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| ---- | ------ | --------------- |----------------- | --------------- |
| 1 | Navigate to [ssr-catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/) | Landing page: European health research data and sample catalogue. The ribbon reads: `MOLGENIS` logo, `HOME`, `ALL RESOURCES`, `ALL VARIABLES`, `UPLOAD DATA` | | |
| 2a | Hover over the `MOLGENIS` logo in the top left | The logo slightly increases in size | | |
| 2b | Click on the `MOLGENIS` logo in the top left | The page does not reload and the user stays on the same page | | |
| 3 | Hover over the `HOME` button | `HOME` is underlined | | |
| 4 | Click on the `HOME` button | The page is reloaded | | |
| 5 | Hover over the `ALL RESOURCES` button | `ALL RESOURCES` is underlined | | |
| 6 | Click on the `ALL RESOURCES` button | The user goes to [/ssr-catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all) | | |
| 7 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/) | | |
| 8 | Hover over the `ALL VARIABLES` button | `ALL VARIABLES` is underlined | | |
| 9 | Click on the `ALL VARIABLES` button | The user goes to [/ssr-catalogue/all/variables](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/variables) | | |
| 10 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/) | | |
| 11 | Navigate to [/ssr-catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all) | The ribbon reads: `MOLGENIS` logo, `RESOURCES`, `COHORT STUDIES`, `DATA SOURCES`, `DATABANKS`, `MORE` | | |
| 12 | Hover over the `MOLGENIS` logo in the top left | The logo slightly increases in size | | |
| 13 | Click on the `MOLGENIS` logo in the top left | The page does not reload and the user stays on the same page | | |
| 14 | Hover over the `OVERVIEW` button | `OVERVIEW` is underlined | | |
| 15 | Click on the `OVERVIEW` button | The page is reloaded | | |
| 16 | Hover over the `COHORTS` button | `COHORTS` is underlined | | |
| 17 | Click on the `COHORTS` button | The user goes to [/ssr-catalogue/all/cohorts](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/cohorts) | | |
| 18 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all) | | |
| 19 | Hover over the `DATA SOURCES` button | `DATA SOURCES` is underlined | | |
| 20 | Click on the `DATA SOURCES` button | The user goes to [/ssr-catalogue/all/datasources](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/datasources) | | |
| 21 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all) | | |
| 22 | Hover over the `VARIABLES` button | `VARIABLES` is underlined | | |
| 23 | Click on the `VARIABLES` button | The user goes to [/ssr-catalogue/all/variables](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/variables) | | |
| 24 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all) | | |
| 25 | Hover over the `MORE` button | A drop-down menu with `Networks`, `Other catalogues` and `Upload data` presented as options | | |
| 26 | Click on `Networks` | The user goes to [/ssr-catalogue/all/networks](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/networks) | | |
| 27 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all) | | |
| 28 | Hover over the `MORE` button | A drop-down menu with `Networks`, `Other catalogues` and `Upload data` presented as options | | |
| 29 | Click on `Other catalogues` | The user goes to [/ssr-catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/) | | |
| 30 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all) | | |
| | | | | |
| 31 | Navigate to [/ssr-catalogue/all/variables](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/variables) | The ribbon reads: `MOLGENIS` logo, `OVERVIEW`, `COHORTS`, `DATA SOURCES`, `VARIABLES`, `MORE` | | |
| 32 | Hover over the `MOLGENIS` logo in the top left | The logo slightly increases in size | | |
| 33 | Click on the `MOLGENIS` logo in the top left | The user goes to [/ssr-catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all) | | |
| 34 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/all/variables](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/variables) | | |
| 35 | Hover over the `OVERVIEW` button | `OVERVIEW` underlined | | |
| 36 | Click on the `OVERVIEW` button | The user goes to [/ssr-catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all) | | |
| 37 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/all/variables](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/variables) | | |
| 38 | Hover over the `COHORTS` button | `COHORTS` underlined | | |
| 39 | Click on the `COHORTS` button | The user goes to [/ssr-catalogue/all/cohorts](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/cohorts) | | |
| 40 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/all/variables](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/variables) | | |
| 41 | Hover over the `DATA SOURCES` button | `DATA SOURCES` underlined | | |
| 42 | Click on the `DATA SOURCES` button | The user goes to [/ssr-catalogue/all/datasources](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/datasources) | | |
| 43 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/all/variables](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/variables) | | |
| 44 | Hover over the `VARIABLES` button | `VARIABLES` underlined | | |
| 45 | Click on the `VARIABLES` button | The page is reloaded | | |
| 46 | Hover over `MORE` | A drop-down menu with `Networks`, `Other catalogues` and `Upload data` presented as options | | |
| 47 | Click on `Networks` | The user goes to [/ssr-catalogue/all/networks](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/networks) | | |
| 48 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/all/variables](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/variables) | | |
| 48b | Hover over `MORE` | A drop-down menu with `Networks`, `Other catalogues` and `Upload data` presented as options | | |
| 49 | Click on `Other catalogues` | The user goes to [/ssr-catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/) | | |
| 50 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/all/variables](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/variables) | | |
| | | | | |
| 51 | Navigate to [/ssr-catalogue/all/cohorts](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/cohorts) | The ribbon reads: `MOLGENIS` logo, `OVERVIEW`, `COHORTS`, `DATA SOURCES`, `VARIABLES`, `MORE` | | |
| 52 | Hover over the `MOLGENIS` logo in the top left | The logo slightly increases in size | | |
| 53 | Click on the `MOLGENIS` logo in the top left | The user goes to [/ssr-catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all) | | |
| 54 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/all/cohorts](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/cohorts) | | |
| 55 | Hover over the `OVERVIEW` button | `OVERVIEW` underlined | | |
| 56 | Click on the `OVERVIEW` button | The user goes to [/ssr-catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all) | | |
| 57 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/all/cohorts](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/cohorts) | | |
| 58 | Hover over the `COHORTS` button | `COHORTS` underlined | | |
| 59 | Click on the `COHORTS` button | The page is reloaded | | |
| 60 | Hover over the `DATA SOURCES` button | `DATA SOURCES` underlined | | |
| 61 | Click on the `DATA SOURCES` button | The user goes to [/ssr-catalogue/all/datasources](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/datasources) | | |
| 62 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/all/cohorts](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/cohorts) | | |
| 63 | Hover over the `VARIABLES` button | `VARIABLES` underlined | | |
| 64 | Click on the `VARIABLES` button | The user goes to [/ssr-catalogue/all/variables](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/variables) | | |
| 65 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/all/cohorts](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/cohorts) | | |
| 66 | Hover over `MORE` | A drop-down menu with `Networks`, `Other catalogues` and `Upload data` presented as options | | |
| 67 | Click on `Networks` | The user goes to [/ssr-catalogue/all/networks](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/networks) | | |
| 68 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/all/cohorts](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/cohorts) | | |
| 68b | Hover over `MORE` | A drop-down menu with `Networks`, `Other catalogues` and `Upload data` presented as options | | |
| 69 | Click on `Other catalogues` | The user goes to [/ssr-catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/) | | |
| 70 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/all/cohorts](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/cohorts) | | |
| | | | | |
| 70a | Click on `More` --> `Upload data` | Should be directed to the 'old' interface apps/central for the user to be able to sign in and upload data | | |
| 70b | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/all/cohorts](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/cohorts) | | |
| 71 | Navigate to [/ssr-catalogue/all/datasources](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/datasources) | The ribbon reads: `MOLGENIS` logo, `OVERVIEW`, `COHORTS`, `DATA SOURCES`, `VARIABLES`, `MORE` | | |
| 72 | Hover over the `MOLGENIS` logo in the top left | The logo slightly increases in size | | |
| 73 | Click on the `MOLGENIS` logo in the top left | The user goes to [/ssr-catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all) | | |
| 74 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/all/datasources](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/datasources) | | |
| 75 | Hover over the `OVERVIEW` button | `OVERVIEW` underlined | | |
| 76 | Click on the `OVERVIEW` button | The user goes to [/ssr-catalogue/all](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all) | | |
| 77 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/all/datasources](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/datasources) | | |
| 78 | Hover over the `COHORTS` button | `COHORTS` underlined | | |
| 79 | Click on the `COHORTS` button | The user goes to [/ssr-catalogue/all/cohorts](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/cohorts) | | |
| 80 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/all/datasources](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/datasources) | | |
| 81 | Hover over the `DATA SOURCES` button | `DATA SOURCES` underlined | | |
| 82 | Click on the `DATA SOURCES` button | The page is reloaded | | |
| 83 | Hover over the `VARIABLES` button | `VARIABLES` underlined | | |
| 84 | Click on the `VARIABLES` button | The user goes to [/ssr-catalogue/all/variables](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/variables) | | |
| 85 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/all/datasources](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/datasources) | | |
| 86 | Hover over `MORE` | A drop-down menu with `Networks`, `Other catalogues` and `Upload data` presented as options | | |
| 87 | Click on `Networks` | The user goes to [/ssr-catalogue/all/networks](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/networks) | | |
| 88 | Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/all/datasources](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/datasources) | | |
| 88b | Hover over `MORE` | A drop-down menu with `Networks`, `Other catalogues` and `Upload data` presented as options | | |
| 89 | Click on `Other catalogues` | The user goes to [/ssr-catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/) | | |
| 90 | Click on **testnetwork1** | The user goes to [/ssr-catalogue/testNetwork1](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/testNetwork1). | | |
| 90b | | The ribbon reads: `MOLGENIS` logo, `OVERVIEW`, `COHORTS`, `DATA SOURCES`, `VARIABLES`, `MORE` | | |
| 91 | Click on the `MOLGENIS` logo in the top left | The page remains the same | | |
| 92 | Click on `OVERVIEW` | The page reloads | | |
| 93 | Click on `COHORTS` | The user goes to [/ssr-catalogue/testNetwork1/cohorts](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/testNetwork1/cohorts) | | |
| 94 | Click on `MOLGENIS` logo | The user goes to [/ssr-catalogue/testNetwork1](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/testNetwork1) | | |
| 95 | Click on `DATA SOURCES` | The user goes to [/ssr-catalogue/testNetwork1/datasources](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/testNetwork1/datasources) | | |
| 96 | Click on `OVERVIEW` | The user goes to [/ssr-catalogue/testNetwork1](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/testNetwork1) | | |
| 97 | Click on `VARIABLES` | The user goes to [/ssr-catalogue/testNetwork1/variables](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/testNetwork1/variables) | | |
| 98 | Hover on `MORE` and then click `About` | The user goes to [/ssr-catalogue/testNetwork1/networks/testNetwork1](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/testNetwork1/networks/testNetwork1) | | |
| 99 | Hover on `MORE` and then click `Other catalogues` | The user goes to [/ssr-catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/) | | |
| 99a| See that `Upload data` is a separate menu item. Click on it. | Should be directed to the 'old' interface apps/central for the user to be able to sign in and upload data | | |
|99b| Use the browser's ⬅️ button | The user goes back to [/ssr-catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/) | | |
| 100 | Click on **OOM** under *Project Catalogues* | The user goes to [/ssr-catalogue/OOM](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/OOM). Menu = Molgenis logo, Overview, Cohorts, Data Sources, Variables, More | | |
| 101 | Hover on `MORE` | 4 options are provided: Networks, About, Other catalogues, Upload data | | |
| 102 | Hover on `MORE` and click on `Other Catalogues` | The user goes to [/ssr-catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/) | | |
| | [THIS EXTRA TEST STEP CAN BE RENDERED INVALID BY PRODUCTION DATA FOR LONGITOOLS BEING COPIED INTO ACC - SEE IT AS A BONUS STEP!] | | | |
| 103 | Click on **LongITools** under *Project Catalogues* | The user goes to [/ssr-catalogue/LongITools](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/LongITools). Menu = Molgenis logo, Overview, Cohorts, About, Other Catalogues, More | | |
