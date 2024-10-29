# Number

10

# Role

Technical tester

# Goal

This test plan checks various specific, unrelated elements of the catalogue making use of a dataset copied from production (eg ATHLETE project, or LIFECYCLE project), which is larger than the test dataset we currently have in ACC.  With the large dataset a technical tester can check (eg) that pagination is working correctly or that long variable names are correctly displayed.

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| ---- | ------ | --------------- | ---------------- | --------------- |
| 1 | Navigate to [MOLGENIS ACC catalogue](https://data-catalogue-acc.molgeniscloud.org/catalogue/ssr-catalogue) | Landing page: European health research data and sample catalogue | | |
| 2 | Click on "Project Catalogue" EUChildNetwork|EUChildNetwork. Following message is displayed: "Welcome to the catalogue of EUChildNetwork: The EU Child Cohort Network. Select one of the content categories listed below." | | |
| 3 | Click on "Variables"|Go to EUChildNetwork overview of variables. The page takes no longer than 1 second to open. | | |
| 4 | Scroll down to the bottom of the page and click page right|Page "2" is displayed at the bottom and the list of variables changes to reflect page 2 of the variables. The page takes no longer than 1 second to update. The user is returned to the top of the page. | | |
| 5 | Note which variables are first and last in the list on page 2. Click on the Harmonisations tab. | See that the same variables top and bottom are shown in the harmonisation matrix for page 2. | | |
| 6 | Return to the List of variables tab. | | | |
| 7 |In the search bar top left, type "food_all_sens"|The list of variables is filtered to all variables with the name starting "food_all_sens" (16). | | |
| 8| Click on the first variable in the list| Go to the detailed page for that variable (<https://data-catalogue-acc.molgeniscloud.org/catalogue/ssr-catalogue/EUChildNetwork/variables/food_all_sens_IgE_0-LifeCycle_CDM-core-LifeCycle_CDM?keys={%22name%22:%22food_all_sens_IgE_0%22,%22resource%22:{%22id%22:%22LifeCycle_CDM%22},%22dataset%22:{%22name%22:%22core%22,%22resource%22:{%22id%22:%22LifeCycle_CDM%22}}}>) | | |
| 9 |Press the "back "button in the browser | Return to <https://data-catalogue-acc.molgeniscloud.org/catalogue/ssr-catalogue/EUChildNetwork/variables?page=1&conditions=[{%22id%22:%22search%22,%22search%22:%22food_all_sens%22}>] and see that the filter on food_all_sens has been retained (16 variables are displayed). | | |
| 10 |Click on "Networks"| Go to <https://data-catalogue-acc.molgeniscloud.org/catalogue/ssr-catalogue/EUChildNetwork/networks> | | |
| 11 |Click on "Variables" | Go to <https://data-catalogue-acc.molgeniscloud.org/catalogue/ssr-catalogue/EUChildNetwork/variables>. See that all filters from previous steps have been cleared. | | |
