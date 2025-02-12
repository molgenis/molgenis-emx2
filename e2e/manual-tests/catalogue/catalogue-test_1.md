# Number

1

# Role

Naïve user of the data catalogue

# Goal

A naïve visitor to the data catalogue can click around in the catalogue and understand what they are seeing when viewing a network of networks.

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| -----| -------| ----------------| -----------------| ----------------|
| 0 | NB: Assumptions | This test plan assumes a 'clean' set of test data, otherwise counts for variables etc. might be off. | | |
| 1 | Navigate to [https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/) | Landing page: European health research data and sample catalogue| | true |
| 2 | In section 'Project catalogues' hover over the testNetworkofNetworks row | Row should be highlighted | | true |
| 3 | Click on the testNetworkofNetworks row | Should be directed to the testNetworkofNetworks with 'Welcome to the catalogue of testNetworkofNetworks: name for test network of networks [etc]', and Collections (8), Networks (2) and Variables (7) buttons | | true |
| 3a | | The text under each button is as follows: Data & sample collections, Networks & Consortia, Harmonised variables | | |
| 3b | | There should be 3,700 participants, 498 samples, Longitudinal 25% and 3 Subpopulations given.| | true |
| 3c | | In the ribbon at the top of the page there should be: Left: MOLGENIS logo, Right: (L-R) Collections, Networks, Variables, About, More  (More --> Other catalogues, Upload data)  | | true |
| 4 | Click on the MOLGENIS logo | The page doesn't change | | true |
| 5 | Click on the 'Collections' button at the top | Should be directed to the list of collections for the testNetworkofNetworks | | true |
| 6 | Click on the MOLGENIS logo | Should be directed back to the home page for testNetworkofNetworks | | true |
| 7 | Click on the 'Variables' button at the top | Should be directed to the list of variables for the testNetworkofNetworks | | true |
| 8 | Click on the 'Overview' button at the top |Should be directed back to the home page for testNetworkofNetworks | | true |
| 9 | Click on the 'Networks' button at the top | Should be directed to the list of networks with which testNetworkofNetworks is associated (testNetwork1, testNetwork2) | | true |
| 10 | Click on testNetwork1 | Should be directed to the testNetwork1 detailed network page with information about the network organised into tiles with a matching menu on the left hand side | | true |
| 11 | Click on the 'Overview' button at the top | Should be directed back to the home page for testNetworkofNetworks | | true |
| 12 | Click on About | Should be directed to the detailed network page for testNetworkofNetworks (THIS IS NOT THE SAME AS THE HOME PAGE!) with website, description, general design, population, partner organisations, publications, funding & acknowledgements, list of cohorts and link to view the network's variables | | true |
| 13 | Click on More --> Other catalogues | Should be directed to the landing page showing all thematic and project catalogues, entitled "European Health Research Data and Sample Catalogue" | | true |
| 14 | Click on More --> Upload data | Should be directed to the 'old' interface apps/central for the user to be able to sign in and upload data | | |
