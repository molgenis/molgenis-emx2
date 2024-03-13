# Number

2

# Role

Naïve user of the data catalogue

# Goal

A naïve visitor to the data catalogue can click around in the catalogue and understand what she is seeing when viewing a network.

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| ---- | ------ | --------------- | -----------------| ----------------|
| 1 | Navigate to [https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/) | Landing page: European health research data and sample catalogue| | |
| 2 | In section 'Thematic catalogues' hover over the testNetwork1 row | Row should be highlighted | | |
| 3 | Click on the testNetwork1 logo (to be implemented, now MOLGENIS logo) | Should be directed to the testNetwork1 home page with 'Welcome to the catalogue of testNetwork1: [etc]', and Cohorts (4), Data sources (1), Variables (3) buttons | | |
| 3a | | There should be 304.995 participants, 114.210 samples and 89% Longitudinal given. | | |
| 3b | | In the ribbon at the top of the page there should be: Left: ATHLETE logo, Right: (L-R) Overview, Cohorts, Data sources, Variables, About, More | | |
| 4 | Click on the testNetwork1 logo (to be implemented, now MOLGENIS logo) | The page doesn't change | | |
| 5 | Click on the 'Overview' button| The page doesn't change | | |
| 6 | Click on the 'Cohorts' button at the top | Should be directed to the list of cohorts for testNetwork1 | | |
| 7 | Click on the testNetwork1 logo (to be implemented, now MOLGENIS logo) | Should be directed back to the home page for testNetwork1 | | |
| 8 | Click on the 'Variables' button at the top |  Should be directed to the list of variables for testNetwork1 | | |
| 9 | Click on the 'Overview' button at the top |Should be directed back to the home page for testNetwork1 | | |
| 10 | Click on the 'About' button at the top | Should be directed to the detailed network page for testNetwork1 with description, website, list of cohorts and list of variables | | |
| 11 | Click on More --> Other catalogues | Should be directed to the landing page showing all thematic and project catalogues, entitled "European Health Research Data and Sample Catalogue" | | |
