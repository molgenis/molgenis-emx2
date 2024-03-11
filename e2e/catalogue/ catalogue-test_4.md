# Number

4

# Role

Researcher searching for specific variables of a project.

# Goal

A researcher working in consortium X (*example: ATHLETE*) searches for specific variables (*example: all ADHD variables*) to check which cohorts have these available.

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| -----| -------| ----------------| -----------------| ----------------|
| 1 | Navigate to `MOLGENIS ACC catalogue` | Landing page: European health research data and sample catalogue| | |
| 2 | Go to the catalogue, under 'project catalogues' | From left to right the Athlete logo, athlete name (link) and description, arrow right (link) | | |
| 3 | Hover over the Athlete row | Row should be highlighted | | |
| 4 | Click on the Athlete logo | Should be redirected to the Athlete 'browse all catalogue contents' | | |
| 5 | Click on the Athlete name | Should be redirected to the Athlete 'browse all catalogue contents'. Cohorts should show 19, variables should show X. | | |
| 6 | Hover over Variables button | Button should be highlighted | | |
| 7 | Click Variables button | Should be redirected to ATHLETE variable explorer | | |
| 8 | Search 'ADHD' in search bar | Variables should be filtered on ADHD variables ('adhd_wave_1' and 'adhdR_wave_1'); topics should be filtered on ADHD; active filters should show 'ADHD'  | | |
| 9 | Open topic 'Symptoms and signs' > 'Symptoms and signs involving cognition, perception, emotional state and behaviour (R40-R46)' > 'Behavioral problem domains' > select 'ADHD symptoms' | Variables should be filtered on ADHD variables ('adhd_wave_1' and 'adhdR_wave_1'); active filters should show topic name| | | |
| 10 |Click on the name of first variable | Should redirect to variable detailed view; Details should show: 1) description, 2) table with all repeats and cohorts (part of the network) that have information about this variable, 3) harmonization status per repeat per cohort (green fully harmonized, yellow partially harmonized, white no harmonization), 4) tabs for each cohorts with further details (variables used, syntax) | | |
| 11 | Click on second cohort tab | Tab should show information for second cohort | | |
| 12 | Click first variable used | Dialog should show information available for the source variable | | |
| 13 | Click back button to return to previous page | Should be redirected to network variable explorer with previously selected filters | | |
| 14 | Open 'Harmonizations' tab (after filtering) | Harmonizations tab should show only ADHD variables and cohorts part of the network; Show correct statuses for harmonization | | |
| 15 | Click 'about statuses' | Should show pop up/dialog with information about statuses | | |
| 16 | Click first variable| Should show pop up/dialog with information about variable | | |
| 17 | Click 'more details' in dialog| Should redirect variable detailed view | | |
