# Number

6

# Role

Researcher searching for specific variables across all projects.

# Goal

A researcher searches for specific variables (*example: all ADHD variables*) across all projects to check which cohorts have these available.

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| -----| -------| ----------------| -----------------| ----------------|
| 1 | Navigate to `MOLGENIS ACC catalogue` | Landing page: European health research data and sample catalogue| | |
| 2 | Hover over 'All variables' button in menu bar | Button should be underlined | | |
| 3 | Click 'All variables' button | Should be redirected to  variable explorer (project agnostic) | | |
| 4 | Search 'ADHD' in search bar | Variables should be filtered on ADHD variables ('adhd_wave_1' and 'adhdR_wave_1'); topics should be filtered on ADHD; active filters should show 'ADHD' | | |
| 5 | Open topic 'Symptoms and signs' > 'Symptoms and signs involving cognition, perception, emotional state and behaviour (R40-R46)' > 'Behavioral problem domains' > select 'ADHD symptoms' | Variables should be filtered on ADHD variables ('adhd_wave_1' and 'adhdR_wave_1'); active filters should show topic name | | |
| 6 |Click on the name of first variable | Should redirect to variable detailed view; Details should show: 1) description, 2) table with all repeats and all cohorts that have information about this variable, 3) harmonization status per repeat per cohort (green fully harmonized, yellow partially harmonized, white no harmonization), 4) tabs for each cohorts with further details (variables used, syntax) | | |
| 7 | Click on second cohort tab | Tab should show information for second cohort | | |
| 8 | Click first variable used | Dialog should show information available for the source variable | | |
| 9 | Click back button to return to previous page | Should be redirected to network variable explorer with previously selected filters | #2419 | |
| 10 | Repeat filtering as in step 9 and open 'Harmonizations' tab | Harmonizations tab should show all ADHD variables and cohorts; Show correct statuses for harmonization | | |
| 11 | Click 'about statuses' | Should show pop up/dialog with information about statuses | #3423 | |
| 12 | Click first variable| Should show pop up/dialog with information about variable | | |
| 13 | Click 'more details' in dialog| Should redirect variable detailed view | | |
