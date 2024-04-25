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
| 3 | Click 'All variables' button | Should be redirected to  variable explorer (project agnostic) Variable page should be shown with complete variable names, variable labels and a filters panel on the left hand side of the page.| | |
| 4 | Search 'ADHD' in search bar | Variables should be filtered on ADHD variables (14variables shown); topics should be filtered on ADHD; active filters should show 'ADHD' | | |
| 5 | Open topic 'Symptoms and signs' > 'Symptoms and signs involving cognition, perception, emotional state and behaviour (R40-R46)' > 'Behavioral problem domains' > select 'ADHD symptoms' | Variables should be filtered on ADHD variables (10); active filters should show topic name | | |
| 6 |Click on the name of variable 'testVarRepeats'| Should redirect to variable detailed view; Details should show: 1) description, 2) harmonization status per repeat per cohort (green fully harmonized, yellow partially harmonized, white no harmonization), 3) tabs for each cohorts with further details (variables used, syntax) | | |
| 7 | Hover 'about statuses' | Pop up should show 'completed: cohort was able to fully map to the harmonized variables'; 'partially: cohort was able to partially map to the harmonized variable'; 'No data: no harmonization  data is available'. | | |
| 8 | Click on second cohort tab | Tab should show information for second cohort | | |
| 9 | Click first variable used | Dialog should show information available for the source variable | | |
| 10 | Click back button to return to previous page | Should be redirected to network variable explorer with previously selected filters | | |
| 11 | Open 'Harmonizations' tab | Harmonizations tab should show all ADHD variables and cohorts | | |
| 12 | Hover 'about statuses' | Pop up should show 'Available: cohort has data available for the variable'; 'No data: cohort does not have data available for the variable'| | |
| 13 | Click first variable| Should show pop up/dialog with information about variable | | |
| 14 | Click 'more details' in dialog| Should redirect variable detailed view | | |
