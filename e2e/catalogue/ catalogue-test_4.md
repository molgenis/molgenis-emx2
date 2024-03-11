# Number

4

# Role

Researcher searching for specific variables of a project.

# Goal

A researcher working in consortium X (*example: testNetwork1*) searches for specific variables (*example: all ADHD variables*) to check which cohorts have these available.

# Steps

| Step | Action | Expected result |  Playwright test |
| -----| -------| ----------------| -----------------| 
| 1 | Navigate to `[MOLGENIS ACC test catalogue](https://data-catalogue-acc.molgeniscloud.org/testCat2/ssr-catalogue/)` | Landing page: European health research data and sample catalogue| | 
| 2 | Go to the catalogue, under 'project catalogues' | From left to right the network logo, testNetwork1 name (link) and description, arrow right (link) | | 
| 3 | Hover over the testNetwork1 row | Row should be highlighted | | 
| 4 | Click on the testNetwork1 logo | Should be redirected to the Athlete 'browse all catalogue contents' | | 
| 5 | Click on the testNework1 name | Should be redirected to the testNetwork1 'browse all catalogue contents'. Cohorts should show 19, variables should show X. | | 
| 6 | Hover over yellow Variables button | Button should be highlighted | | 
| 7 | Click Variables button | Should be redirected to testNetwork1 variable explorer | | 
| 8 | Search 'repeats' in search bar | Variables should be filtered on repeats variables ('testVarrepeats' and 'testVarNoRepeats'); topics should be filtered on repeats; active filters should show 'repeats'  | | 
| 9 | Open topic 'Symptoms and signs' > 'Symptoms and signs involving cognition, perception, emotional state and behaviour (R40-R46)' > 'Behavioral problem domains' > select 'ADHD symptoms' | Variables should be filtered on ADHD variables ('testVarRepeats_' and 'testVarNoRepeats'); active filters should show topic name| | 
| 10 |Click on the name of first variable 'testVarRepeats_' | Should redirect to variable detailed view; Details should show: 1) description, 2) table with all repeats and cohorts (part of the network) that have information about this variable, 3) harmonization status per repeat per cohort. testCohort1 should show all repeats completely harmonized; testCohort2 should show all repeats NA; testCohort3 and testCohort4 should show partial, NA and complete. Example: testCohort3 should show repeat 1 completely, repeat 3 NA and repeat 4 partial. 4) tabs for each cohorts with further details (variables used, syntax) | | 
| 11 | Click on second cohort tab | Tab should show information for second cohort | | 
| 12 | Click first variable used | Dialog should show information available for the source variable | | 
| 13 | Click back button to return to previous page | Should be redirected to network variable explorer with previously selected filters | | 
| 14 | Open 'Harmonizations' tab | Harmonizations tab should show only ADHD variables and cohorts part of the network; Show correct statuses for harmonization (testCohort1: has info; testCohort2: no info; testCohort3: has info; testCohort4: no info)| | 
| 16 | Click first variable | Should show pop up/dialog with information about variable | | 
| 17 | Click 'more details' in dialog| Should redirect variable detailed view | | 
