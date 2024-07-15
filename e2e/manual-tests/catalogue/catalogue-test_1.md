# Number

1

# Role

Naïve user of the data catalogue 

# Goal

A naïve visitor to the data catalogue can click around in the catalogue and understand what s/he is seeing when viewing a network of networks.

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| -----| -------| ----------------| -----------------| ----------------|
| 1 | Navigate to [https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/) | Landing page: European health research data and sample catalogue| | true |
| 2 | In section 'Project catalogues' hover over the testNetworkofNetworks row | Row should be highlighted | | true |
| 3 | Click on the testNetworkofNetworks row | Should be directed to the testNetworkofNetworks with 'Welcome to the catalogue of testNetworkofNetworks: name for test network of networks [etc]', and Cohorts (3), Variables (8) and Networks (2) buttons | | true |
| 3a | | There should be 700 participants, 250 samples and 67% Longitudinal given.| | true |
| 3b | | In the ribbon at the top of the page there should be: Left: MOLGENIS logo (test logo to be implemented in test data), Right: (L-R) Overview, Cohorts, Variables, Networks, More  | | true |
| 4 | Click on the MOLGENIS logo | The page doesn't change | | true |
| 5| Click on the 'Overview' button| The page doesn't change | | true |
| 6| Click on the 'Cohorts' button at the top | Should be directed to the list of cohorts for the testNetworkofNetworks | | true |
| 7 | Click on the MOLGENIS logo | Should be directed back to the home page for testNetworkofNetworks | | true |
| 8 | Click on the 'Variables' button at the top |  Should be directed to the list of variables for the testNetworkofNetworks | | true |
| 9| Click on the 'Overview' button at the top |Should be directed back to the home page for testNetworkofNetworks | | true |
| 10 | Click on the 'Networks' button at the top | Should be directed to the list of networks with which testNetworkofNetworks is associated (testNetwork1, testNetwork2) | | true |
| 11 | Click on testNetwork1 | Should be directed to the testNetwork1 detailed network page with website, description, partners, funding & citation requirements, list of cohorts, list of data sources and link to view the network's variables | | true |
| 12 | Click on the 'Overview' button at the top | Should be directed back to the home page for testNetworkofNetworks | | true |
| 13 | Click on More --> About | Should be directed to the detailed network page for testNetworkofNetworks (THIS IS NOT THE SAME AS THE HOME PAGE!) with website, description, partners, funding & citation requirements, list of cohorts and link to view the network's variables | | true |
| 14 | Click on More --> Other catalogues | Should be directed to the landing page showing all thematic and project catalogues, entitled "European Health Research Data and Sample Catalogue" | | true |
| 15 | Click on More --> Upload data | Should be directed to the 'old' interface apps/central for the user to be able to sign in and upload data
