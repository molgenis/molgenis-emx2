# Number

4

# Role

Researcher searching for specific variables of a project.

# Goal

A researcher working in consortium X (*example: testNetwork1*) searches for specific variables (*example: all ADHD variables*) to check which cohorts have these available.

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| ---- | ------ | --------------- | ---------------- | --------------- |
| 0 | NB: Assumptions | This test plan assumes a 'clean' set of test data, otherwise counts for variables etc. might be off. | | |
| 1 | Navigate to [https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/) | Landing page: European health research data and sample catalogue | | |
| 2 | Scroll down to the catalogue 'testNetwork1', under 'Thematic catalogues' | From left to right the network logo, testNetwork1 name (link) and description, arrow right (link) | | |
| 3 | Hover over the testNetwork1 row | Row should be highlighted | | |
| 4 | Click on the testNetwork1 logo | Should be redirected to the testNetwork1 'Welcome to... Select one of the content categories listed below.' | | |
| 4a | Go back | Return to the European Health Research Data and Sample Catalogue page | | |
| 5 | Click on the testNetwork1 name | Should be redirected to the testNetwork1 'Welcome to... Select one of the content categories listed below.'. Cohorts should show 4, data sources should show 1, databanks should show 3, variables should show 7. | | |
| 6 | Hover over yellow Variables button | Button should be highlighted | | |
| 7 | Click Variables button | Should be redirected to testNetwork1 variable explorer; Variable page should show complete variable names, variable labels and a filters panel on the left hand side of the page. Variable "testVarRepeats_" should have "repeated for year 0-10" under its name. Variable "testVarRep-with-looong-name_" should have "repeated for trimester 0-3" under its name. Text above the list of variables reads '7 variables'. | | |
| 8 | Search 'ADHD' in search bar | Variables should be filtered on ADHD variables ('testVarRepeats_' and 'testVarNoRepeats'); variable "testVarRepeats_" should have "repeated for year 0-10" under its name; active filters should show 'Search in variables: ADHD' | | |
| 8a | Remove 'ADHD' from the search bar | The list of variables returns to its initial form and no filters are shown above the list | | |
| 9 | Expand the Topic tree if necessary and then click on 'Search for options' above the topics on the left hand side | A pop-up with the list of topics is displayed | | |
| 9a | Type 'Symptoms' in the search field in the pop-up | Four sections of the topic tree with checkboxes are displayed | | |
| 9b | Open topic 'Symptoms and signs' > 'Symptoms and signs involving cognition, perception, emotional state and behaviour (R40-R46)' > 'Behavioral problem domains' > select 'ADHD symptoms' and then choose Show Results | Variables should be filtered on ADHD variables ('testVarNoRepeats' and 'testVarRepeats_'); variable "testVarRepeats_" should have "repeated for year 0-10" under its name; active filters should show 'Topics - 1' | | |
| 10 | Click on the name of variable 'testVarRepeats_' | Should redirect to variable detailed view; Details should show: 1) definition, 2) harmonisation status per cohort, 3) harmonisation details per cohort. Table in 2): testCohort1 should show all repeats completely harmonised; testCohort3 and testCohort4 should show partial, NA and complete. Example: testCohort3 should show repeat 1 completely, repeat 3 NA and repeat 4 partial. 4) tabs for each cohorts with further details (variables used, syntax) | | |
| 11 | In "Harmonisation status per cohort", hover over 'about statuses' | Pop up should show 'completed: source was able to fully map to the harmonised variables'; 'partially: source was able to partially map to the harmonised variable'; 'No data: no harmonisation data is available'. | | |
| 12 | In "Harmonisation details per cohort", click on tab for 'testcohort3' | Tab should show information for testcohort3 | | |
| 13 | Click first variable used | Dialog should show information available for the source variable | | |
| 14 | Click back button to return to previous page | Should be redirected to network variable explorer with previously selected filters | | |
| 15 | Open 'Harmonisations' tab | Harmonisations tab should show only variables ('testVarNoRepeats' and 'testVarRepeats_') and cohorts/databanks/data sources/etc which are part of the network; Show correct statuses for harmonisation (testCohort1: has info; testCohort2: no info; testCohort3: has info; testCohort4: has info) | | |
| 16 | Hover over 'about statuses' | Pop up should show 'Available: source has data available for the variable'; 'No data: source does not have data available for the variable' | | |
| 17 | Click first variable | Should show pop up/dialog with information about variable | | |
| 18 | Click 'more details' in dialog | Should redirect to variable detailed view | | |
| 19 | |See that the variable 'ga_lmp' (a LifeCycle variable mapped by testcohort2) is NOT shown in the list | | |
