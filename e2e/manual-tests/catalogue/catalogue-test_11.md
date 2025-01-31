# Number

11

# Role

Naïve user of the data catalogue.

# Goal

A naïve visitor to the data catalogue can click around the 'browse all catalogue content'-screen and search within 'all resources'-screen.

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| ---- | ------ | --------------- | ---------------- | --------------- |
| 0 | Navigate to the [all catalogue contents screen](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all) | End up on the 'browse all catalogue contents'-screen | | |
| 1 | Click on `VARIABLES` in the menu | End up at [the variable explorer](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/variables) | | |
| 2 | Go back using the browser back button | End up at [the all catalogue contents screen](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all) | | |
| 3 | Click on the word `VARIABLES` underneath the logo | End up at [the variable explorer](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/variables) | | |
| 4 | Go back by clicking the Molgenis logo in the top left | End up at [the all catalogue contents screen](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all) | | |
| 5 | Click on the variables CTA-button | End up at [the variable explorer](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/variables) | | |
| 6 | Check the breadcrumb | It should be `ALL > VARIABLES` | | |
| 7 | Go back by clicking the `ALL` breadcrumb | End up at [the all catalogue contents screen](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all) | | |
| 8 | Check the CTA-buttons | There should be 3, names: `COLLECTIONS`, `NETWORKS`, and `VARIABLES` | | |
| 9 | Check the title above each CTA | They should match the text on their respective CTA's | | |
| 10 | Check the description of the `COLLECTIONS` CTA | It should read "Data and sample collections" | | |
| 11 | Check the description of the `NETWORKS` CTA | It should read "Networks & Consortia" | | |
| 12 | Check the description of the `VARIABLES` CTA | It should read "Harmonised variables" | | |
| 13 | Check the ribbon at the top of the screen | It should contain the options: `COLLECTIONS`, `NETWORKS`, `VARIABLES`, `OTHER CATALOGUES` and `MORE` | | |
| 14 | Mouse over `MORE` | It should reveal the options "Upload data" | | |
| 15 | Check the texts below the CTAs | It should contain the number of participants, the number of samples, the percentage of longitudinal collections, and the number of subpopulations. | | |
| 16 | Each card containing a CTA should be lined out consistently | Each should take up the same amount of space and the room between them should be consistent. | | |
| | | | | |
| 17 | Click on the `NETWORKS` CTA-button | End up at [the networks screen](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/networks) | | |
| 18 | Check the breadcrumb | It should be `ALL > NETWORKS` | | |
| 19 | Check that it shows the correct number of networks | There should be 9 | | |
| 20 | Check the "Search in networks"-filter | It should be expanded | | |
| 21 | Click the "Search in networks"-input and enter 'acronym' | There should be 3 networks shown | | |
| 22 | Click the `Search in networks: acronym 🗑️` button | The filter should be removed and 9 results should be shown again. | | |
| 23 | Click the `COMPACT` button | The list of networks should reduce in size and just show the name and description. | | |
| 24 | Click the title of the first network "acronym for test network of networks" | End up at [the overview page of the network](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/networks/testNetworkofNetworks) | | |
| 25 | Check the breadcrumb | It should be "ALL > NETWORKS" | | |
| 26 | Click `NETWORKS` in the breadcrumb | End up at [the networks screen](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/networks) again | | |
| 27 | Look at the first network "acronym for test network of networks" | It should list its name, description, `Type` Network, `Design`, `Participants`, and `Duration` 1975 until 2010. | | |
| | | | | |
| 28 | Click on `COLLECTIONS` in the ribbon | End up at [the collections screen](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/all/collections) | | |
| 29 | Check the breadcrumb | It should be `ALL > COLLECTIONS`. | | |
| 30 | Check the number of collections | There should be 96 collections. | | |
| 31 | Check the "Collection type"-filter | It should be expanded. | | |
| 32 | Click the `Cohort study` "Collection type"-filter | The list of collections should now contain 57 results. | | |
| 33 | Click the `Data source` "Collection type"-filter | The list of collections should now contain 61 results. | | |
| 34 | Click the `Collection type - 2 🗑️` button in the "Active filters"-"bar | All 96 collections should be displayed again. | | |
| 35 | Mouse over the ⓘ-icon after "Biobank" in the "Collection type"-filter | A tooltip should appear, reading: "Repositories of biological samples." | | |
| 36 | Click on the "Collection type"-filters | It should collapse, hiding its options. | | |
| 37 | Click the "Areas of information"-filter | It should expand its options. | | |
| 38 | Click the "Search for options"-button in the "Areas of information"-filter. | It should open a modal window with a list of all filter options. | | |
| 39 | Scroll the list all the way down using the scroll wheel on the mouse. Note: if the list is not long enough to scroll, expand some of its list structure to make it larger. | After it reaches the bottom of the list, nothing should happen. | <https://github.com/molgenis/molgenis-emx2/issues/4443> | |
| 40 | Scroll the list back up using the mouse wheel. | The list should scroll back up. | <https://github.com/molgenis/molgenis-emx2/issues/4443> | |
| 41 | Check the ribbon at the top of the screen. | It should contain the options: `OVERVIEW`, `COLLECTIONS`, `NETWORKS`, `VARIABLES`, and `MORE`. | | |
