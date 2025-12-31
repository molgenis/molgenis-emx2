# Test Plan 8

## Role

Naïve user of the data catalogue

## Goal

A naïve visitor to the data catalogue can click around through items in the menu ribbon and end up where they would expect

## Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| ---- | ------ | --------------- |----------------- | --------------- |
| 1 | Go to `https://data-catalogue-acc.molgeniscloud.org` | Landing page: European health research data and sample catalogue. The ribbon reads: `MOLGENIS` logo, `HOME`, `ALL COLLECTIONS`, `ALL VARIABLES`, `ALL NETWORKS`, `MORE` | | |
| 2a | Hover over the `MOLGENIS` logo in the top left | The logo slightly increases in size | | |
| 2b | Click on the `MOLGENIS` logo in the top left | The page does not reload and the user stays on the same page | | |
| 3 | Hover over the `HOME` button | `HOME` is underlined | | |
| 4 | Click on the `HOME` button | The page is reloaded | | |
| 5 | Hover over the `ALL COLLECTIONS` button | `ALL COLLECTIONS` is underlined | | |
| 6 | Click on the `ALL COLLECTIONS` button | The user goes to [/all/collections](https://data-catalogue-acc.molgeniscloud.org/all/collections) | | |
| 7 | Use the browser's ⬅️ button | The user goes back to [home](https://data-catalogue-acc.molgeniscloud.org/) | | |
| 8 | Hover over the `ALL VARIABLES` button | `ALL VARIABLES` is underlined | | |
| 9 | Click on the `ALL VARIABLES` button | The user goes to [/all/variables](https://data-catalogue-acc.molgeniscloud.org/all/variables) | | |
| 10 | Use the browser's ⬅️ button | The user goes back to [home](https://data-catalogue-acc.molgeniscloud.org/) | | |
| 11 | Navigate to [/all](https://data-catalogue-acc.molgeniscloud.org/all) | The ribbon reads: `MOLGENIS` logo, `COLLECTIONS`, `NETWORKS`, `VARIABLES`, `OTHER CATALOGUES`, `MORE` | | |
| 12 | Hover over the `MOLGENIS` logo in the top left | The logo slightly increases in size | | |
| 13 | Click on the `MOLGENIS` logo in the top left | The page does not reload and the user stays on the same page | | |
| 14 | Hover over the `COLLECTIONS` button | `COLLECTIONS` is underlined | | |
| 15 | Click on the `COLLECTIONS` button | The user goes to [/all/collections](https://data-catalogue-acc.molgeniscloud.org/all/collections) | | |
| 16 | Use the browser's ⬅️ button | The user goes back to [/all](https://data-catalogue-acc.molgeniscloud.org/all) | | |
| 17 | Hover over the `NETWORKS` button | `NETWORKS` is underlined | | |
| 18 | Click on the `NETWORKS` button | The user goes to [/all/networks](https://data-catalogue-acc.molgeniscloud.org/all/networks) | | |
| 19 | Use the browser's ⬅️ button | The user goes back to [/all](https://data-catalogue-acc.molgeniscloud.org/all) | | |
| 20 | Hover over the `VARIABLES` button | `VARIABLES` is underlined | | |
| 21 | Click on the `VARIABLES` button | The user goes to [/all/variables](https://data-catalogue-acc.molgeniscloud.org/all/variables) | | |
| 22 | Use the browser's ⬅️ button | The user goes back to [/all](https://data-catalogue-acc.molgeniscloud.org/all) | | |
| 23 | Hover over the `OTHER CATALOGUES` button | `OTHER CATALOGUES` is underlined | | |
| 24 | Click on the `OTHER CATALOGUES` button | The user goes to [home](https://data-catalogue-acc.molgeniscloud.org/) | | |
| 25 | Use the browser's ⬅️ button | The user goes back to [/all](https://data-catalogue-acc.molgeniscloud.org/all) | | |
| 26 | Hover over the `MORE` button | A drop-down menu is presented with `Upload data` and `Manuals` as the only options | | |
| | | | | |
| 31 | Navigate to [/all/variables](https://data-catalogue-acc.molgeniscloud.org/all/variables) | The ribbon reads: `MOLGENIS` logo, `OVERVIEW`, `COLLECTIONS`, `NETWORKS`, `VARIABLES`, `MORE` | | |
| 32 | Hover over the `MOLGENIS` logo in the top left | The logo slightly increases in size | | |
| 33 | Click on the `MOLGENIS` logo in the top left | The user goes to [/all](https://data-catalogue-acc.molgeniscloud.org/all) | | |
| 34 | Use the browser's ⬅️ button | The user goes back to [/all/variables](https://data-catalogue-acc.molgeniscloud.org/all/variables) | | |
| 35 | Hover over the `OVERVIEW` button | `OVERVIEW` underlined | | |
| 36 | Click on the `OVERVIEW` button | The user goes to [/all](https://data-catalogue-acc.molgeniscloud.org/all) | | |
| 37 | Use the browser's ⬅️ button | The user goes back to [/all/variables](https://data-catalogue-acc.molgeniscloud.org/all/variables) | | |
| 38 | Hover over the `COLLECTIONS` button | `COLLECTIONS` underlined | | |
| 39 | Click on the `COLLECTIONS` button | The user goes to [/all/collections](https://data-catalogue-acc.molgeniscloud.org/all/collections) | | |
| 40 | Use the browser's ⬅️ button | The user goes back to [/all/variables](https://data-catalogue-acc.molgeniscloud.org/all/variables) | | |
| 41 | Hover over the `NETWORKS` button | `NETWORKS` underlined | | |
| 42 | Click on the `NETWORKS` button | The user goes to [/all/networks](https://data-catalogue-acc.molgeniscloud.org/all/networks) | | |
| 43 | Use the browser's ⬅️ button | The user goes back to [/all/variables](https://data-catalogue-acc.molgeniscloud.org/all/variables) | | |
| 44 | Hover over the `VARIABLES` button | `VARIABLES` underlined | | |
| 45 | Click on the `VARIABLES` button | The page is reloaded | | |
| 46 | Hover over `MORE` | A drop-down menu with `Other catalogues`, `Upload data` and `Manuals` as options is presented  | | |
| 47 | Click on `Networks` | The user goes to [/all/networks](https://data-catalogue-acc.molgeniscloud.org/all/networks) | | |
| 48 | Use the browser's ⬅️ button | The user goes back to [/all/variables](https://data-catalogue-acc.molgeniscloud.org/all/variables) | | |
| 48b | Hover over `MORE` | A drop-down menu is presented with `Other catalogues` and `Upload data` as options | | |
| 49 | Click on `Other catalogues` | The user goes to [home](https://data-catalogue-acc.molgeniscloud.org/) | | |
| 50 | Use the browser's ⬅️ button | The user goes back to [/all/variables](https://data-catalogue-acc.molgeniscloud.org/all/variables) | | |
| | | | | |
| 51 | Navigate to [/all/collections](https://data-catalogue-acc.molgeniscloud.org/all/collections) | The ribbon reads: `MOLGENIS` logo, `OVERVIEW`, `COLLECTIONS`, `NETWORKS`, `VARIABLES`, `MORE` | | |
| 52 | Hover over the `MOLGENIS` logo in the top left | The logo slightly increases in size | | |
| 53 | Click on the `MOLGENIS` logo in the top left | The user goes to [/all](https://data-catalogue-acc.molgeniscloud.org/all) | | |
| 54 | Use the browser's ⬅️ button | The user goes back to [/all/collections](https://data-catalogue-acc.molgeniscloud.org/all/collections) | | |
| 55 | Hover over the `OVERVIEW` button | `OVERVIEW` underlined | | |
| 56 | Click on the `OVERVIEW` button | The user goes to [/all](https://data-catalogue-acc.molgeniscloud.org/all) | | |
| 57 | Use the browser's ⬅️ button | The user goes back to [/all/collections](https://data-catalogue-acc.molgeniscloud.org/all/collections) | | |
| 58 | Hover over the `COLLECTIONS` button | `COLLECTIONS` underlined | | |
| 59 | Click on the `COLLECTIONS` button | The page is reloaded | | |
| 60 | Hover over the `NETWORKS` button | `NETWORKS` underlined | | |
| 61 | Click on the `NETWORKS` button | The user goes to [/all/networks](https://data-catalogue-acc.molgeniscloud.org/all/networks) | | |
| 62 | Use the browser's ⬅️ button | The user goes back to [/all/collections](https://data-catalogue-acc.molgeniscloud.org/all/collections) | | |
| 63 | Hover over the `VARIABLES` button | `VARIABLES` underlined | | |
| 64 | Click on the `VARIABLES` button | The user goes to [/all/variables](https://data-catalogue-acc.molgeniscloud.org/all/variables) | | |
| 65 | Use the browser's ⬅️ button | The user goes back to [/all/collections](https://data-catalogue-acc.molgeniscloud.org/all/collections) | | |
| 66 | Hover over `MORE` | A drop-down menu is presented with `Other catalogues`, `Upload data` and `Manuals` as options | | |
| 67 | Click on `Other catalogues` | The user goes to [home](https://data-catalogue-acc.molgeniscloud.org/) | | |
| 68 | Use the browser's ⬅️ button | The user goes back to [/all/collections](https://data-catalogue-acc.molgeniscloud.org/all/collections) | | |
| 69 | Click on `More` --> `Upload data` | Should be directed to the 'old' interface apps/central for the user to be able to sign in and upload data | | |
| 70 | Use the browser's ⬅️ button | The user goes back to [/all/collections](https://data-catalogue-acc.molgeniscloud.org/all/collections) | | |
| | | | | |
| 71 | Navigate to [/all/networks](https://data-catalogue-acc.molgeniscloud.org/all/networks) | The ribbon reads: `MOLGENIS` logo, `OVERVIEW`, `COLLECTIONS`, `NETWORKS`, `VARIABLES`, `MORE` | | |
| 72 | Hover over the `MOLGENIS` logo in the top left | The logo slightly increases in size | | |
| 73 | Click on the `MOLGENIS` logo in the top left | The user goes to [all](https://data-catalogue-acc.molgeniscloud.org/all) | | |
| 74 | Use the browser's ⬅️ button | The user goes back to [/all/networks](https://data-catalogue-acc.molgeniscloud.org/all/networks) | | |
| 75 | Hover over the `OVERVIEW` button | `OVERVIEW` underlined | | |
| 76 | Click on the `OVERVIEW` button | The user goes to [/all](https://data-catalogue-acc.molgeniscloud.org/all) | | |
| 77 | Use the browser's ⬅️ button | The user goes back to [/all/networks](https://data-catalogue-acc.molgeniscloud.org/all/networks) | | |
| 78 | Hover over the `COLLECTIONS` button | `COLLECTIONS` underlined | | |
| 79 | Click on the `COLLECTIONS` button | The user goes to [/all/collections](https://data-catalogue-acc.molgeniscloud.org/all/collections) | | |
| 80 | Use the browser's ⬅️ button | The user goes back to [/all/networks](https://data-catalogue-acc.molgeniscloud.org/all/networks) | | |
| 81 | Hover over the `NETWORKS` button | `NETWORKS` underlined | | |
| 82 | Click on the `NETWORKS` button | The page is reloaded | | |
| 83 | Hover over the `VARIABLES` button | `VARIABLES` underlined | | |
| 84 | Click on the `VARIABLES` button | The user goes to [/all/variables](https://data-catalogue-acc.molgeniscloud.org/all/variables) | | |
| 85 | Use the browser's ⬅️ button | The user goes back to [/all/networks](https://data-catalogue-acc.molgeniscloud.org/all/networks) | | |
| 86 | Hover over `MORE` | A drop-down menu is presented with `Other catalogues`, `Upload data` and `Manuals` as options | | |
| 87 | Click on `Other catalogues` | The user goes to [home](https://data-catalogue-acc.molgeniscloud.org/) | | |
| | | | | |
| 88 | Click on **testnetwork1** | The user goes to [/testNetwork1](https://data-catalogue-acc.molgeniscloud.org/testNetwork1). | | |
| 89 | | The ribbon reads: test logo, `COLLECTIONS`, `VARIABLES`, `ABOUT`, `OTHER CATALOGUES`, `MORE` | | |
| 91 | Click on the test logo in the top left | The page remains the same | | |
| 93 | Click on `COLLECTIONS` | The user goes to [/testNetwork1/collections](https://data-catalogue-acc.molgeniscloud.org/testNetwork1/collections) | | |
| 94 | Click on the test logo | The user goes to [/testNetwork1](https://data-catalogue-acc.molgeniscloud.org/testNetwork1) | | |
| 97 | Click on `VARIABLES` | The user goes to [/testNetwork1/variables](https://data-catalogue-acc.molgeniscloud.org/testNetwork1/variables) | | |
| 98 | Click on `About` | The user goes to [/testNetwork1/about/testNetwork1](https://data-catalogue-acc.molgeniscloud.org/testNetwork1/about/testNetwork1) | | |
| 99 | Hover on `MORE` and then click `Other catalogues` | The user goes to [home](https://data-catalogue-acc.molgeniscloud.org/) | | |
| 99a | See that `Upload data` is a menu item under `MORE`. Click on it. | The user is directed to the 'old' interface apps/central to be able to sign in and upload data | | |
| 99b | Use the browser's ⬅️ button | The user goes back to [home](https://data-catalogue-acc.molgeniscloud.org/) | | |
| 100a | Click on **LongITools** under *Project Catalogues* | The user goes to [/LongITools](https://data-catalogue-acc.molgeniscloud.org/LongITools). | | |
| 100b | | The ribbon reads: `MOLGENIS` logo, `COLLECTIONS`, `VARIABLES`, `ABOUT`, `OTHER CATALOGUES`, `MORE`. | | |
| 101 | Hover on `MORE` | 3 options are provided: `Other catalogues`, `Upload data` and`Manuals` | | |
| 102 | Click on `Other Catalogues` | The user goes to [home](https://data-catalogue-acc.molgeniscloud.org/) | | |
