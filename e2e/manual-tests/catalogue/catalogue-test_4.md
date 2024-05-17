# Number

4

# Role

Researcher searching for specific variables of a project.

# Goal

A researcher working in consortium X (*example: testNetwork1*) searches for specific variables (*example: all ADHD variables*) to check which cohorts have these available.

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| -----| -------| ----------------| -----------------| ----------------|
| 1 | Navigate to [https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/) | Landing page: European health research data and sample catalogue| | |
| 2 | Go to the catalogue 'testNetwork1', under 'Thematic catalogues' | From left to right the network logo, testNetwork1 name (link) and description, arrow right (link) | | |
| 3 | Hover over the testNetwork1 row | Row should be highlighted | | |
| 4 | Click on the testNetwork1 logo | Should be redirected to the testNetwork1 'Welcome to... Select one of the content categories listed below.' | | |
| 4a |  Go back | Return to the European Health Research Data and Sample Catalogue page | | |
| 5 | Click on the testNetwork1 name | Should be redirected to the testNetwork1 'Welcome to... Select one of the content categories listed below.'. Cohorts should show 4, variables should show 3. | | |
| 6 | Hover over yellow Variables button | Button should be highlighted | | |
| 7 | Click Variables button | Should be redirected to testNetwork1 variable explorer; Variable page should show complete variable names, variable labels and a filters panel on the left hand side of the page. | | |
| 8 | Search 'ADHD' in search bar | Variables should be filtered on ADHD variables ('testVarRepeats' and 'testVarNoRepeats'); topics should be filtered on ADHD; active filters should show 'ADHD'  | | |
| 9 | Open topic 'Symptoms and signs' > 'Symptoms and signs involving cognition, perception, emotional state and behaviour (R40-R46)' > 'Behavioral problem domains' > select 'ADHD symptoms' | Variables should be filtered on ADHD variables ('testVarRepeats_' and 'testVarNoRepeats'); active filters should show topic name | | |
| 10 | Click on the name of first variable 'testVarRepeats_' | Should redirect to variable detailed view; Details should show: 1) description, 2) harmonization status per cohort, 3) harmonization details per cohort. Table in 2: testCohort1 should show all repeats completely harmonized; testCohort2 should show all repeats NA; testCohort3 and testCohort4 should show partial, NA and complete. Example: testCohort3 should show repeat 1 completely, repeat 3 NA and repeat 4 partial. 4) tabs for each cohorts with further details (variables used, syntax) | | |
| 11 | Hover 'about statuses' | Pop up should show 'completed: cohort was able to fully map to the harmonized variables'; 'partially: cohort was able to partially map to the harmonized variable'; 'No data: no harmonization data is available'. | | |
| 12 | Click on second cohort tab | Tab should show information for second cohort | | |
| 13 | Click first variable used | Dialog should show information available for the source variable | | |
| 14 | Click back button to return to previous page | Should be redirected to network variable explorer with previously selected filters | | |
| 15 | Open 'Harmonizations' tab | Harmonizations tab should show only variables ('testVarRepeats' and 'testVarNoRepeats') and cohorts part of the network; Show correct statuses for harmonization (testCohort1: has info; testCohort2: no info; testCohort3: has info; testCohort4: no info)| | |
| 16 | Hover 'about statuses | Pop up should show 'Available: cohort has data available for the variable'; 'No data: cohort does not have data available for the variable' | | |
| 17 | Click first variable | Should show pop up/dialog with information about variable | | |
| 18 | Click 'more details' in dialog| Should redirect variable detailed view | | |
