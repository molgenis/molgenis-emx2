# Number

2

# Role

Naïve user of the data catalogue

# Goal

A naïve visitor to the data catalogue can click around in the catalogue and understand what she is seeing when viewing a network, in both of the network overview pages.

# Steps

| Step | Action | Expected result | Playwright test |
| ---- | ------ | --------------- | -----------------|
| 1 | Navigate to [https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/) | Landing page: European health research data and sample catalogue| | |
| 2 | In section 'Thematic catalogues' hover over the testNetwork1 row | Row should be highlighted | | |
| 3 | Click on the testNetwork1 logo (to be implemented, click on 'testNetwork1') | Should be directed to the testNetwork1 home page with 'Welcome to the catalogue of testNetwork1: [etc]', and Cohorts (4), Data sources (1), Variables (3) buttons | | |
| 3a | | There should be 700 participants, 250 samples and 50% Longitudinal given. | | |
| 3b | | In the ribbon at the top of the page there should be: Left: testnetwerk1 logo, Right: (L-R) Overview, Cohorts, Data sources, Variables, More | | |
| 4 | Click on the testNetwork1 logo (to be implemented, now MOLGENIS logo) | The page doesn't change | | |
| 5 | Click on the 'Overview' button| The page doesn't change | | |
| 6 | Click on the 'Cohorts' button at the top | Should be directed to the list of cohorts for testNetwork1 | | |
| 7 | Click on the testNetwork1 logo (to be implemented, now MOLGENIS logo) | Should be directed back to the home page for testNetwork1 | | |
| 8 | Click on the 'Data Sources' button at the top | Should be directed to the list of data sources for testNetwork1 | | |
| 9 | Click on the 'Variables' button at the top |  Should be directed to the list of variables for testNetwork1 | | |
| 10 | Click on the 'Overview' button at the top |Should be directed back to the home page for testNetwork1 | | |
| 11 | Click on More --> 'About' button | Should be directed to the detailed network page for testNetwork1 with website, description, partners, funding & citation requirements, list of cohorts, list of data sources and list of variables | | |
| 12 | Click on More --> Other catalogues | Should be directed to the landing page showing all thematic and project catalogues, entitled "European Health Research Data and Sample Catalogue" | | |
| 13 | Click on testNetwork1 | Should be directed to the testNetwork1 home page with 'Welcome to the catalogue of testNetwork1: [etc]', and Cohorts (4), Data sources (1), Variables (3) buttons | | |
| 14 | Click on the underlined name of the catalogue in the "Welcome to the catalogue of..." text | ACRONYM FOR TESTNETWORK 1, name for testnetwork1 | | |
| 15 | Menu on the left hand side | ACRONYM FOR TESTNETWORK1, Description, Partners, Funding & Citation requirements, Cohorts, Data Sources, Variables | | |
| 16 | First block | logo, https://www.molgenis.org | | |
| 17 | Click on URL | You are taken to the Molgenis website in a separate window | | |
| 18 | Go back to the catalogue window | | | |
| 19 | DESCRIPTION | test description for new test network | | |
| 20 | PARTNERS | Amsterdam Medical Centre, Cynexo | | |
| 21 | FUNDING & CITATION REQUIREMENTS | lorem ipsum text, check that "until now" is shown at the end of both fields so that you know the full text has been displayed | | |
| 22 | COHORTS | A list of cohorts you can explore. | | |
|    |  | Name, Design, Number of participants, --> | | |
|    |  | Name for test cohort 1   Longitudinal  600   --> | | |
|    |  | Name for test cohort 2   Longitudinal  100   --> | | |
|    |  | testCohort3                                  --> | | |
|    |  | testCohort4                                  --> | | |
| 23 | Click on Name for test cohort 1 | A side pop-up is shown with the following information: NAME FOR TEST COHORT 1, This is the Test cohort 1. It has multiple entries ... see which is displayed.  Website https://www.molgenis.org, Number of participants 600, Number of participants with samples 200 | | |
| 24 | Click on detail page | Go to the cohort overview page for ACRONYM FOR TEST COHORT 1 | | |
| 25 | Go back | Back to network overview page | | |
| 26 | DATA SOURCES | Data sources connected in this network| | |
| | | Name   Type    Number of participants | | |
| | | TESTDATASOURCE Participants  3000| | |
| 27 | Click on TESTDATASOURCE | Get taken to the overview page for the data source TESTDATASOURCE | | |
| 28 | Go back | Come back to network overview page for testNetwork1 | | |
| 29 | VARIABLES | Variables in this network | | |
|   |   | Name  Label   Model    | | |
|  |    | testVarCategorical_ categorical test variable testNetwork1_CDM  --> | | |
|  |    | testVarNoRepeats test variable without repeats testNetwork1_CDM --> | | |
|  |    | testVarRepeats_ test variable with repeats testNetwork1_CDM  -->  | | |
| 30 | Click on testVarNoRepeats | Variable information in pop-up UNDER CONSTRUCTION | | |
