# Number

1

# Role

Naïve user of the data catalogue

# Goal

A naïve visitor to the data catalogue can click around in the catalogue and understand what s/he is seeing when viewing a network of networks.

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| -----| -------| ----------------| -----------------| ----------------|
| 1 | Navigate to [https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/) | Landing page: European health research data and sample catalogue| | |
| 2 | In section 'Project catalogues' hover over the testNetworkofNetworks row | Row should be highlighted | | |
| 3 | Click on the testNetworkofNetworks row | Should be directed to the testNetworkofNetworks with 'Welcome to the catalogue of testNetworkofNetworks: name for test network of networks [etc]', and Cohorts (3), Variables (3) and Networks (2) buttons | | |
| 3a | | There should be 700 participants, 250 samples and 67% Longitudinal given.| | |
| 3b | | In the ribbon at the top of the page there should be: Left: MOLGENIS logo (test logo to be implemented in test data), Right: (L-R) Overview, Cohorts, Variables, Networks, More  | |
| 4 | Click on the MOLGENIS logo | The page doesn't change | | |
| 5| Click on the 'Overview' button| The page doesn't change | | |
| 6| Click on the 'Cohorts' button at the top | Should be directed to the list of cohorts for the testNetworkofNetworks | | |
| 7 | Click on the MOLGENIS logo | Should be directed back to the home page for testNetwork1 | | |
| 8 | Click on the 'Variables' button at the top |  Should be directed to the list of variables for the testNetworkofNetworks | | |
| 9| Click on the 'Overview' button at the top |Should be directed back to the home page for testNetworkofNetworks | | |
| 10 | Click on the 'Networks' button at the top | Should be directed to the list of networks with which testNetworkofNetworks is associated (testNetwork1, testNetwork2) | | |
| 11 | Click on testNetwork1 | Should be directed to the testNetwork1 detailed network page with description, website, list of cohorts and list of variables | | |
| 12 | Click on the 'Overview' button at the top | Should be directed back to the home page for testNetworkofNetworks
| 13 | Click on More --> About | Should be directed to the detailed network page for testNetworkofNetworks (THIS IS NOT THE SAME AS THE HOME PAGE!) with description, website, list of cohorts and list of variables | | |
| 14 | Click on More --> Other catalogues | Should be directed to the landing page showing all thematic and project catalogues, entitled "European Health Research Data and Sample Catalogue" | | |
