# Number

10

# Role

Technical tester

# Goal

This test plan checks various specific, unrelated elements of the catalogue making use of a dataset copied from production (eg ATHLETE project, or LIFECYCLE project), which is larger than the test dataset we currently have in ACC.  With the large dataset a technical tester can check (eg) that pagination is working correctly or that long variable names are correctly displayed.

# Steps

| Step | Action | Expected result | Playwright test |
| -----| -------| ----------------| -----------------|
| 1 | Navigate to [MOLGENIS ACC catalogue](https://data-catalogue-acc.molgeniscloud.org/catalogue/ssr-catalogue) | Landing page: European health research data and sample catalogue | | |
| 2 |Click on "Thematic Catalogue" EUChildNetwork|EUChildNetwork. Following message is displayed: "Welcome to the catalogue of EUChildNetwork: The EU Child Cohort Network. Select one of the content categories listed below." | | |
| 3 |Click on "Variables"|Go to EUChildNetwork overview of variables. The page takes no longer than 1 second to open. | | |
| 4 |Scroll down to the bottom of the page and click page right|Page "2" is displayed at the bottom and the list of variables changes to reflect page 2 of the variables. The page takes no longer than 1 second to update. | | |
| 5 |In the search bar top left, type "food_all_sens"|The list of variables is filtered to all variables with the name starting "food_all_sens". Check that the variable names are visible in full. | | |
