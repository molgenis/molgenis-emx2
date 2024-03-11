# Number

3

# Role

Naïve user of the data catalogue

# Goal

A naïve visitor to the data catalogue can click around in the catalogue and understand what s/he is seeing when viewing a cohort.

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| -----| -------| ----------------| -----------------| ----------------|
| 1 | Navigate to `MOLGENIS ACC catalogue` | Landing page: European health research data and sample catalogue| | |
| 2 | Hover over the ATHLETE row | Row should be highlighted | | |
| 3 | Click on the ATHLETE logo | Should be directed to the ATHLETE home page with 'Welcome to the catalogue of ATHLETE [etc]', and Cohorts (19) and Variables (1377) buttons | | |
| 3a | | There should be 304.995 participants, 114.210 samples and 89% Longitudinal given. | | |
| 3b | | In the ribbon at the top of the page there should be: Left: ATHLETE logo, Right: (L-R) Overview, Cohorts, Variables, About, More | | |
| 4 |  Click on the 'Cohorts' button under 'Cohorts' | Should be directed to the list of cohorts for ATHLETE with: Cohorts logo, "Group of individuals sharing a defining demographic characteristic", Detailed/Compact toggle buttons (Default is selected), Filters on the LHS | | |
| 5 | Click on Compact | The list of cohorts turns into a list of cohort acronyms and cohort names-in-full, with an arrow after each cohort | | |
| 6 | Scroll down and click on the arrow next to EDEN | Should be directed to the cohort home page with: 'ATHLETE > COHORTS', 'EDEN', "Study on the pre and early postnatal determinants of child health and development' | | |
|6a ||In the ribbon at the top of the page there should be: Left: ATHLETE logo, Right: (L-R) Overview, Cohorts, Variables, About, More | | |
| 6b ||The following elements are listed: name of cohort, website, Description, General design, Contact & contributors, Available data & samples, Subpopulations, Collection events, Networks, Access conditions, Funding & Citation requirements | | |
| 7 | Click on the Cohorts button | Should be directed back to the list of cohorts for ATHLETE | | |
