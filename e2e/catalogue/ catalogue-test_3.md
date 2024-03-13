# Number

3

# Role

Naïve user of the data catalogue

# Goal

A naïve visitor to the data catalogue can click around in the catalogue and understand what s/he is seeing when viewing a cohort.

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| -----| -------| ----------------| -----------------| ----------------|
| 1 | Navigate to [https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/) | Landing page: European health research data and sample catalogue| | |
| 2 | Hover over the testNetwork1 row | Row should be highlighted | | |
| 3 | Click on the testNetwork1 logo (to be implemented in test data, now MOLGENIS logo) | Should be directed to the testNetwork1 home page with 'Welcome to the catalogue of testNetwork1 [etc]', Cohorts (4), Data sources (1) and Variables (3) buttons | | |
| 3a | | There should be 700 participants, 250 samples and 50% Longitudinal given. | | |
| 3b | | In the ribbon at the top of the page there should be: Left: testNetwork1 logo (to be implemented in test data, now MOLGENIS logo), Right: (L-R) Overview, Cohorts, Data sources, Variables, More | | |
| 4 |  Click on the 'Cohorts' button under 'Cohorts' | Should be directed to the list of cohorts for testNetwork1 with: Cohorts logo (to be implemented in test data), "Group of individuals sharing a defining demographic characteristic", Detailed/Compact toggle buttons (Default is selected), Filters on the LHS | | |
| 5 | Click on Compact | The list of cohorts turns into a list of cohort acronyms and cohort names-in-full, with an arrow after each cohort | | |
| 6 | Scroll down and click on the arrow next to testCohort1 | Should be directed to the cohort home page with: 'testNetwork1 > COHORTS', 'ACRONYM FOR TEST COHORT 1', 'Name for test cohort 1' | | |
| 6a || In the ribbon at the top of the page there should be: Left: testCohort1 logo (to be implemented in test data), Right: (L-R) Overview, Cohorts, Data source, Variables, More | | |
| 6b || The following elements are listed: name of cohort, website, Description, General design, Contact & contributors, Available data & samples, Subpopulations, Collection events, Networks, Access conditions, Funding & Citation requirements | | |
| 7 | Click on the Cohorts button | Should be directed back to the list of cohorts for testNetwork1 | | |
