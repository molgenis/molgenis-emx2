# Number

3

# Role

Naïve user of the data catalogue

# Goal

A naïve visitor to the data catalogue can click around in the catalogue and understand what s/he is seeing when viewing a cohort study.

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| ---- | ------ | --------------- | ---------------- | --------------- |
| 1 | Navigate to [https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/) | Landing page: European health research data and sample catalogue | | |
| 2 | Hover over the testNetwork1 row | Row should be highlighted | | |
| 3 | Click on the testNetwork1 row | Should be directed to the testNetwork1 home page with 'Welcome to the catalogue of testNetwork1: name for test network1. Select one of the content categories listed below.', and Cohort studies (4), Data Sources (1), Databanks (3) and Variables (7) buttons | | |
| 3a | | There should be 3.700 participants, 498 samples, 25% Longitudinal and 3 Subpopulations given. | | |
| 3b | | In the ribbon at the top of the page there should be: Left: testNetwork1 logo, Right: (L-R) Cohort studies, Data Sources, Databanks, Variables, More (--> About, Other catalogues, Upload data)| | |
| 4 | Click on the 'Cohort studies' button under 'Cohort studies' | Should be directed to the list of cohorts for testNetwork1 with: Cohort studies logo, "COHORT STUDIES", "Cohorts & Biobanks", Detailed/Compact toggle buttons ('Detailed' is selected by default), "4 cohort studies", Filters on the LHS | | |
| 5 | Click on Compact | The list of cohort studies turns into a list of cohort study acronyms and cohort study names-in-full, with an arrow after each cohort study | | |
| 6 | Scroll down and click on the arrow next to 'acronym for test cohort 2' | Should be directed to the resource home page with: 'TESTNETWORK1 > COHORT STUDIES', 'ACRONYM FOR TEST COHORT 2', 'Name for test cohort 2' | | |
|6a || In the ribbon at the top of the page there should be: Left: testnetwork1 logo, Right: (L-R) Overview, Cohort studies, Data Sources, Databanks, More (--> Variables, About, Other catalogues, Upload data) | | |
| 6b || The following elements are listed: logo, website and Contact button, Description, General design, Population, Organisations, Contributors, Available data & samples, Subpopulations, Collection events, Datasets, Networks, Publications, Access conditions, Funding & Acknowledgements, Documentation | | |
| 7 | Click on the Cohort studies button in the ribbon | Should be directed back to the list of cohort studies for testnetwork1 | | |
