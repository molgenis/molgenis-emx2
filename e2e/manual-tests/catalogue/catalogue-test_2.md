# Test Plan 2

## Role

Naïve user of the data catalogue

## Goal

A naïve visitor to the data catalogue can click around in the catalogue and
understand what they are seeing when viewing a network, in both of the network overview pages.

## Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| ---- | ------ | --------------- | ----------------- | -----------------|
| 0 | NB: Assumptions | This test plan assumes a 'clean' set of test data, otherwise counts for variables etc. might be off. Ensure that testCatalogue scheme has been uploaded into the Acceptance server before you start testing | | |
| 1 | Navigate to [https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue/) | Landing page: European health research data and sample catalogue| | |
| 2 | In section 'Thematic catalogues' hover over the testNetwork1 row | Row should be highlighted | | |
| 3 | Click on the testNetwork1 logo | Should be directed to the testNetwork1 home page with 'Welcome to the catalogue of testNetwork1: [etc]', and Collections (8), Variables (7) buttons | | |
| 3a | | There should be 3,700 participants, 498 samples, Longitudinal 25%, 3 Subpopulations given. | | |
| 3b | | In the ribbon at the top of the page there should be: Left: testNetwork1 logo, Right: (L-R) Collections, Variables, About, Other catalogues, More (-> Upload data, Manuals) | | |
| 4 | Click on the testNetwork1 logo | The page doesn't change | | |
| 5 | Click on the 'Collections' button at the top | Should be directed to the list of collections for testNetwork1 | | |
| 6 | In the filters on the left hand side for 'Collection type' select 'Biobank' | Should find 0 collections and text: 'No resources found with current filters' | | |
| 7 | In the filters on the left hand side for 'Collection type' select 'Cohort study' | Should find 4 collections | | |
| 8 | In the filters on the left hand side for 'Collection type' select 'Databank' | Should find 3 collections | | |
| 9 | In the filters on the left hand side for 'Collection type' select 'Data source' and 'Cohort study' | Should find 5 collections | | |
| 10 | Click on the testNetwork1 logo | Should be directed back to the home page for testNetwork1 | | |
| 11 | Click on 'Variables' in the ribbon | Should be directed to the list of variables for testNetwork1 | | |
| 12 | Click on the 'Overview' button at the top | Should be directed back to the home page for testNetwork1 | | |
| 13 | Click on 'About' button | Should be directed to the detailed network page for testNetwork1 with logo, website, description, general design, population, organisations, datasets, networks, publications, funding & acknowledgements | | |
| 14 | Click on 'More' -> 'Other catalogues' | Should be directed to the landing page showing all thematic and project catalogues, entitled "European Health Research Data and Sample Catalogue" | | |
| 15 | Click on Upload data | Should be directed to the 'old' interface apps/central for the user to be able to sign in and upload data | | |
| 16 | Click on back in the browser | Should be directed back to the landing page showing all thematic and project catalogues | | |
| 17 | Click on testNetwork1 | Should be directed to the testNetwork1 home page with 'Welcome to the catalogue of testNetwork1: [etc]', and Collections (8), Variables (7) buttons | | |
| 18 | Click on the underlined name of the catalogue in the "Welcome to the catalogue of..." text | ACRONYM FOR TESTNETWORK 1, name for testnetwork1 | | |
| 19 | Menu on the left hand side | Logo, Description, General Design, Population, Organisations, Datasets, Networks, Publications, Funding & Acknowledgements | | |
| 20 | First block | Logo, <https://www.molgenis.org> | | |
| 21 | Click on URL | You are taken to the Molgenis website in a separate window | | |
| 22 | Go back to the catalogue window | | | |
| 23 | DESCRIPTION | test description for new test network | | |
| 24 | GENERAL DESIGN | Type Network, Network type EU4Health - Prevention, Start/End data collection 1975 until 2010, PID `https://pid-for-testnetwork1.org` | | |
| 25 | POPULATION | Central African Republic (the), Chad | | |
| 26 | ORGANISATIONS | Lead organisations, University Medical Center Groningen (UMCG), Netherlands (the) | | |
|    | | Additional organisations, Amsterdam Medical Center (AMC), Netherlands (the), Cynexo (CYN), Italy | | |
| 27 | DATASETS | Datasets, List of datasets for this resource | | |
|    | | Name, Description | | |
|    | | cdm_1 -> | | |
| 28 | NETWORKS | Part of networks | | |
|    | | name for test network of networks, > Website > Network details > Catalogue, -> | | |
| 29 | PUBLICATIONS | Two publications: 'Sustainability...phase 3 trials.' and 'Ten-year...randomized trial.' | | |
| 30 | FUNDING & ACKNOWLEDGEMENTS | lorem ipsum text, check that "until now" is shown at the end of both fields so that you know the full text has been displayed | | |
