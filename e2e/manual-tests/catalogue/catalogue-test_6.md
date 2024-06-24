# Number

6

# Role

Researcher searching for specific variables across all projects.

# Goal

A researcher searches for specific variables (*example: all ADHD variables*) across all projects to check which cohorts have these available.

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| -----| -------| ----------------| -----------------| ----------------|
| 1 | Navigate to [https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/) | Landing page: European health research data and sample catalogue| | |
| 2 | Hover over 'All variables' button in menu bar | Button should be underlined | | |
| 3 | Click 'All variables' button | Should be redirected to  variable explorer (project agnostic) Variable page should be shown with variable names not truncated, variable labels and a filters panel on the left hand side of the page. Text at the top: "A complete overview of harmonised variables."| | |
| 4 | Search 'ADHD' in search bar | Variables should be filtered on ADHD variables (14 variables shown); active filters should show 'ADHD' | | |
| 4a| Remove 'adhd' from the search bar | The list of variables returns to its initial form and no filters are shown above the list | | |
| 5 | Expand the Topic tree if necessary and then click on 'Search for options' above the topics on the left hand side | A pop-up with the list of topics is displayed | | |
| 5a| Type 'Symptoms' in the search field in the pop-up | Four sections of the topic tree with checkboxes are displayed | | |
| 5b| Open topic 'Symptoms and signs' > 'Symptoms and signs involving cognition, perception, emotional state and behaviour (R40-R46)' > 'Behavioral problem domains' > select 'ADHD symptoms' and then choose Show Results | Variables should be filtered on ADHD variables (10); active filters should show -1 | | |
| 6 |Click on the name of variable 'testVarRepeats_'| Should redirect to variable detailed view; Details should show: 1) description, 2) harmonisation status per repeat per cohort (green fully harmonised, yellow partially harmonised, white no harmonisation), 3) tabs for each cohorts with further details (variables used, syntax) | | |
| 7 | In "harmonisation details per cohort", hover 'about statuses' | Pop up should show 'completed: cohort was able to fully map to the harmonised variables'; 'partial: cohort was able to partially map to the harmonised variable'; 'No data: no harmonisation  data is available'. | | |
| 8 | Click on second cohort tab | Tab should show information for second cohort | | |
| 9 | Click first variable used | Dialog should show information available for the source variable | | |
| 10 | Click back button to return to previous page | Should be redirected to network variable explorer with previously selected filters | | |
| 11 | Open 'Harmonisations' tab | Harmonisations tab should show all ADHD variables and cohorts, scroll to the right to see that there are cohorts with information | | |
| 12 | Hover 'about statuses' | Pop up should show 'Available: cohort has data available for the variable'; 'No data: cohort does not have data available for the variable'| | |
| 13 | Click first variable| Should show pop up/dialog with information about variable | | |
| 14 | Click 'more details' in dialog| Should redirect variable detailed view | | |
