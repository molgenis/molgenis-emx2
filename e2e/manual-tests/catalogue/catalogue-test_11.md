# Number

11

# Role

Naïve user of the data catalogue.

# Goal

A naïve visitor to the data catalogue can click around and search within 'all resources'.

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| 0 | Navigate to [the home screen](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/testNetwork1) | End up on the overview page of the testNetwork1 | | |
| 0.5 | Check numbers at the bottom, they come straight from the db, not sure how to verify | numbers are as expected (can I asssume this testset always?) | | |
| 1 | Click on VARIABLES in the menu | End up at[the variable screen](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/testNetwork1/variables)| | |
| 2 | Go back using the browser back button| End up at[the home screen](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/testNetwork1) | | |
| 3 | Click on the word VARIABLES underneath the logo | End up at [the variable screen](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/testNetwork1/variables) | | |
| 4 | Go back by clicking the Molgenis logo in the top left | End up at [the home screen](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/testNetwork1) | | |
| 4.5 | Go back by clicking the TESTNETWORK1 breadcrumb | End up at [the home screen](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/testNetwork1) | | |
| 5 | Click on the variables button | End up at [the variable screen](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/testNetwork1/variables) | | |
| 6 | Enter 'lang' into the search box (why is there a button if it does auto search?) | I'd expect 2 variables to show: 'testVarLang' and 'textVarLang3Vir' | I also find 'textVarCategorical\_' and 'textVarNoRepeats' and I don't see why | |
| 7 | ------ | | | |
| 8 | ------ | | | |
