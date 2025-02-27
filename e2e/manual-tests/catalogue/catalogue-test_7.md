# Number

7

# Role

Researcher searching for certain variables in the 'all variables' explorer and the network-specific explorer

# Goal

A researcher can filter on variables using the search filter, topics and cohorts in both the 'all variables' explorer and the network-specific explorer

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| ---- | ------ | --------------- | ---------------- | --------------- |
| 'ALL VARIABLES' VARIABLE EXPLORER | | | | |
| 0 | NB: Assumptions | This test plan assumes a 'clean' set of test data, otherwise counts for variables etc. might be off. Ensure that testCatalogue schema has been uploaded into the Acceptance server before you start testing| | |
| 1 | Go to <https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/all/variables> | The user goes to overview of all harmonised variables across all networks:         2249 variables | | |
| 2 | Type ‘adhd’ in search bar | 14 variables are listed | | |
| 3 | Click on Harmonisations button | Harmonisation matrix is opened | | |
| 4 | Scroll to the right to see Pelagie, testCohort1, testCohort2…| Pelagie has mappings for the first 7 variables, testCohort1, testCohort3 and testCohort4 have mappings on the ‘testVar…’ variables. | | |
| | | TestCohort2 has no mappings but is still displayed. | | |
| 5 | Delete ‘adhd’ in the search bar and instead type ‘asthma’| See that 15 variables are displayed. | | |
| 6 | Add the word ‘maternal’ after the word ‘asthma’ in the search bar --> ‘asthma maternal’| See that 3 variables are displayed | | |
| 7 | Change the search text to ‘maternal asthma’ | See that the list of variables  remains the same. | | |
| 8 | Change the search text back to ‘asthma’| See that 15 variables are displayed. | | |
| 9 | | On the left hand side, a long list of all the sources in the catalogue is displayed on the left hand side | | |
| 10 | Select all 4 testCohorts. | See that the message 'No variables found with current filters' is displayed. | | |
| 11 | Remove the filter on all 4 cohorts. | The list of variables remains at 15, and now all sources in the catalogue are displayed again, with their mappings. Sources without mappings are displayed. | | |
| 12 | Click on the first variable, ‘asthma_’| The overview page for the individual variable asthma_ is displayed. | | |
| | | Harmonisation information is given for cohort Pelagie alone. | | |
| | | The repeated variables are displayed in the correct numerical order. | | |
| 13 | Go back using the browser’s back button. | Asthma filter is still in place in the overview, and no sources have been selected. | | |
| 14 | Remove ‘asthma’ from the search bar at the top. | The original list of 2249 variables is displayed. | | |
| 15 | On the left hand side, expand Topics, press Search for options | Drop-down with topics is displayed | | |
| 16 | Type ‘lang’ in the search bar in the drop-down | List of topics is reduced to those highest-level topics including the letters ‘lang’ either in their name or in the hover text (whether in subtopics or highest-level topic itself)| | |
| 17 | Find ‘Language’ under ‘Socio-demographic and economic characteristics’ and select that. | Subtopic Language and topic Socio-demographic and economic characteristics are both selected. 12 variables are displayed  (8 existing vars + 4 test vars with keyword Language) | | |
| 18 | Toggle back to ‘List of Variables’| The same variables are displayed in a list. | | |
| 19 | Add ‘adhd’ in the search bar | 1 variable is displayed (only variables which match both criteria, i.e. testVarNoRepeats)| | |
| 20 | Delete ‘adhd’ in the search bar | List returns to 12 variables (just language now – 8 + 4 test vars)| | |
| 21 | | On the left hand side, a long list of all the sources in the catalogue is displayed on the left hand side | | |
| 22 | Select all 4 testCohorts. | See that the list of variables is reduced to: | | |
| | | testVarLang, | | |
| | | testVarLang3Vir, | | |
| | | testVarNoRepeats | | |
| | |(just the test vars with keyword Language AND with a mapping to one of the cohorts, so not testVarCategorical)| | |
| 23 | Click on Harmonisations | Harmonisation matrix is displayed. The following variables are listed, with the following mappings: | | |
| | | testVarLang mapped to cohorts 1 and 4, | | |
| | | testVarLang3Vir mapped to cohorts 3 and 4, | | |
| | | testVarNoRepeats mapped to cohorts 1,3 and 4. | | |
| | | See that testcohort2 is shown despite having no mappings, because it has been actively selected. | | |
| | | See that variables testVarCategorical is NOT shown. | | |
| 24 | Add the topic filter ‘Virology’ on the left hand side (under Laboratory measures)| See that 4 variables are displayed. | | |
| | | Variable testVarVir mapped to cohorts 3 and 4 is added to the list. No other changes to the list. | | |
| 25 | Remove the source filters | 13 variables are displayed. The following mappings to the test variables are shown (scroll to the right): | | |
| | | testVarCategorical with no mappings. | | |
| | | testVarLang mapped to cohorts 1 and 4. | | |
| | | testVarLang3Vir mapped to cohorts 3 and 4. | | |
| | | testVarNoRepeats mapped to cohorts 1,3 and 4. | | |
| | | testVarVir mapped to cohorts 3 and 4. | | |
| | | See that variables with no mappings ARE displayed, and that cohorts with no mappings are also displayed. | | |
| | | See that testVarRepeats_ is NOT shown (keyword adhd not selected). | | |
| 26 | Remove the ‘Language’ filter | 2 variables are displayed and all the sources are displayed. The following mappings are shown: | | |
| | | testVarLang3Vir mapped to cohorts 3 and 4. | | |
| | | testVarVir mapped to cohorts 3 and 4 | | |
| NETWORK-SPECIFIC VARIABLE EXPLORER | | | | |
| 27 | Go to More --> Other Catalogues | The user goes to <https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue> | | |
| 28 | Click on TestNetwork1 | The user goes to <https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/testNetwork1> | | |
| 29 | Choose Variables | The user goes to <https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/testNetwork1/variables>. 7 variables are displayed: | | |
| | | testVarCategorical_(repeated for year 0-21), | | |
| | | testVarLang, | | |
| | | testVarLang3Vir, | | |
| | | testVarNoRepeats, | | |
| | | testVarRep-with-looong-name_(repeated for trimester 0-3), | | |
| | | testVarRepeats_(repeated for year 0-10), | | |
| | | testVarVir | | |
| 30 | Type ‘adhd’ in search bar | 2 variables are listed. | | |
| | | testVarNoRepeats, | | |
| | | testVarRepeats_(repeated for year 0-10)| | |
| 31 | Click on Harmonisations button | Harmonisation matrix is opened. See that 2 variables are displayed and all 8 sources in the network are displayed, regardless of whether or not they have mappings. | | |
| 32 | On the left hand side, expand Topics, press Search for options | Drop-down with topics is displayed | | |
| 33 | Type ‘bio’ in the search bar | List of topics is reduced to those including the letters ‘bio’ either in their name or in the hover text (whether in subtopics or highest-level topic itself)| | |
| 34 | Having found ‘Laboratory measures’, expand that to find ‘Biochemistry’ and click on that. | See that no variables at all are displayed because there are no variables in testNetwork1 which match both criteria (search bar ‘adhd’ and topic ‘biochemistry’). | | |
| 35 | Click open the ‘Search for options’ under Topics. Remove the ‘biochemistry’ filter and add ‘Language’ as a filter (under ‘Socio-demographic and economic characteristics’)| 1 variable is displayed: only testVarNoRepeats because that is the only variable with both keywords ‘adhd’ and ‘language’. All 8 sources in the network are shown. | | |
| 36 | Delete ‘adhd’ in the search bar | List returns to 4 variables (just the filter on language now). | | |
| | | testVarCategorical_, | | |
| | | testVarLang, | | |
| | | testVarLang3Vir, | | |
| | | testVarNoRepeats. | | |
| | | All 8 sources in the network are shown because no source filter has been applied. | | |
| 37 | On the left hand side, expand Sources | List of all 8 sources in the network is displayed on the left hand side | | |
| 38 | Select testCohort2. | See that the list of variables is empty. testCohort2 has no mappings to a “Language” variable, so the message 'No variables found with current filters' is displayed. | | |
| 39 | Select the other 3 test cohorts alongside testCohort2. | 3 variables are displayed in the harmonisation matrix: | | |
| | | testVarLang mapped to cohorts1 and 4, | | |
| | | testVarLang3Vir mapped to cohorts 3 and 4, | | |
| | | testVarNoRepeats mapped to cohorts 1, 3 and 4. | | |
| | | See that testVarCategorical_ and testVarLang2 are not shown (because they have no mappings to the cohorts selected). | | |
| | | TestCohort2 is shown despite having no mappings to these variables, because it has been selected in the source filter. | | |
| 40 | Add the filter ‘Virology’ in the topics on the left hand side (under ‘Laboratory measures’)| See that 4 variables are displayed, with the following mappings: | | |
| | | testVarLang with mappings to cohorts 1 and 4. | | |
| | | testVarLang3Vir with mappings to cohorts 3 and 4. | | |
| | | testVarNoRepeats with mappings to cohorts 1,3,4. | | |
| | | testVarVir with mappings to cohorts 3 and 4. | | |
| | | Testcohort2 is shown with no mappings. | | |
| | | Not shown: testVarCategorical_ | | |
| 41 | Remove the filter ‘Language’ on the left hand side. | See that 2 variables are displayed, with the following mappings: | | |
| | | testVarLang3Vir with mappings to cohorts 3 and 4. | | |
| | | testVarVir with mappings to cohorts 3 and 4. | | |
| | | See that testcohort1 and testcohort2 are shown, despite having no mappings, because they have been actively selected. | | |
| 42 | Remove the filters for testcohort2 and testcohort3 so that only testcohort1 and testcohort4 are selected. | Testcohort1 and testcohort4 are shown and the list of variables remains the same. | | |
| | | testVarLang3Vir with mapping to cohort 4. | | |
| | | testVarVir with mapping to cohort 4. | | |
| | | Testcohort1 is shown, with no mappings. | | |
| | | See that testcohort 2 and 3 are NOT shown. | | |
| 43 | Remove all filters in the topics and remove all source filters by clicking on the rubbish bins at the top of the list of variables. | 7 variables are displayed (full list for testnetwork1) | | |
| | | testVarCategorical with no mappings. | | |
| | | testVarLang with mappings to cohorts 1 and 4. | | |
| | | testVarLang3Vir with mappings to cohorts 3 and 4. | | |
| | | testVarNoRepeats with mappings to cohorts 1,3,4. | | |
| | | testVarRepeats-with-a-very-looong-name_ with no mappings. | | |
| | | testVarRepeats_ with mappings to cohorts 1,3,4. | | |
| | | testVarVir with mappings to cohorts 3 and 4. | | |
| | | All 8 sources are displayed. | | |
| 44 | Select testcohort2. | Message 'No variables found with current filters' is displayed. | | |
| 45 | Remove all the source filters. | 7 variables are displayed (full list for testnetwork1)| | |
| 46 | Click on testVarRepeats_ | Pop-up with short information about the variable appears | | |
| 47 | Click on More Details | Individual page for variable testVarRepeats_ is displayed. | | |
| | | Under the ‘harmonisation status’ block only testcohort1, 3 and 4 are shown. | | |
| | | Testcohort2 has no mappings to this variable and so is not shown.   | | |
