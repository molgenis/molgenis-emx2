# Number

9

# Role

Naïve user of the data catalogue

# Goal

A naïve visitor to the data catalogue can click around in the catalogue and understand what s/he is seeing when viewing a cohort in detail. Multiple values in fields are displayed correctly. Not all fields on the page are tested (See test plan 5 for full test of this page).

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| -----| -------| ----------------| -----------------| ----------------|
| 1 | Navigate to [MOLGENIS ACC test catalogue](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue) | Landing page: European health research data and sample catalogue | | |
| 2 | Navigate to Search All | Cohorts, Data sources, Variables and Networks buttons are shown with numbers of each under each button | | |
| 3 | Click on the Cohorts button | Cohorts overview page with a list of all cohorts in the system | | |
| 4 | Type "test cohort" in the Search field top left | See that two cohorts are presented in the list: "acronym for test cohort 1" and "acronym for test cohort 2" | | |
| 5 | Click on "Acronym for test cohort 1" | See that the cohort detail page for Acronym for test cohort 1 is presented: "All > cohorts", ACRONYM FOR TEST COHORT 1, Name for test cohort 1 | | |
| 6 | See that the fields are filled as follows:| | | |
| 7 |DESCRIPTION|This is the Test cohort 1. It has multiple entries where possible.  Design paper and publications are random DOIs from the EUCAN project.  Yes to data access fee. The population age group is all adult options, so the population age group should be shown as "Adult 18+ years". Cohort type has both 'normal' and 'other' fields filled in --> see which is displayed | | |
||Click on the three dots next to the description text|the text is displayed fully, and clicking on 'read less' reduces the text again | | |
| 8 |Cohort type|Clinical cohort, Clinical trial | | |
||NB not all fields are tested in this test plan| | | |
| 9 |Collection type|Retrospective, Prospective | | |
| 10 |Start/End year|1900 - 1967 | | |
| 11|Design paper|Cohort Profile: The French national cohort of children (ELFE): birth to 5 years | | |
| 12|Click on the design paper|goes to the article in a new window: https://doi.org/10.1093/ije/dyz227 [Cohort Profile: The French national cohort of children (ELFE): birth to 5 years] | | |
| 13| close the window or reselect the catalogue display window| | | |
|  | POPULATION
| 14 |Countries|Armenia, Bermuda, Réunion | | |
| 15 |Regions|Bradford, Lapland, Gipuzkoa | | |
| 16 |Population age groups|Adult (18+ years) | | |
| 17 |Main medical condition|VII Diseases of the eye and adnexa, VIII Diseases of the ear and mastoid process | | |
| 18 |Population disease|V Mental and behavioural disorders, IV Endocrine, nutritional and metabolic diseases | | |
| 19 |ICDO topology|BASE OF TONGUE, UTERUS, NOS | | |
| 20 |ICDO morphology|Carcinoma, metastatic, NOS Carcinomatosis | | |
| 21 |Inclusion criteria|Clinically relevant exposure inclusion criterion | | |
|||Clinically relevant lifestyle inclusion criterion | | |
|||Country of residence inclusion criteria | | |
| 22|CONTACT AND CONTRIBUTORS|1 card per contact person with: (titles) (initials) ((first name)) (surname prefix) (surname), (email address), (description of role) | | |
|||dr. ir.  P.I.P.M. (PIPM) surname prefix last name PIPM ---- pipm@testdomain.nl -----  Test description of PI/PM role | | |
|||dr.  D.M. (datamgr first name) datamgr surname prefix datamgr last name --- test@email.nl --- test description for data manager role | | |
||NB not all blocks are tested in this test plan| | | |
| 23|SUBPOPULATIONS|List of subcohorts or subpopulations for this resource | | |
|||table with the following columns: Name, Description, Number of participants | | |
|||test subcohort 1A, description for test subcohort 1A, 956 --> | | |
|||test subcohort 1B, description for test subcohort 1B, 23487 --> | | |
| 24|COLLECTION EVENTS|List of collection events defined for this resource | | |
|||table with the following columns: Name, Description, Participants, Start end year | | |
|||test collection event 1A, test description for test collection event 1A, 3500, 1958-1994 --> | | |
|||test collection event 1B, description for test collection event 1B, 286, 1992-2000 --> | | |
| 25 |DATASETS|List of datasets for this resource | | |
|||Table with the following columns: Name, Description | | |
|||test dataset for testCohort1,   test description for dataset 1  --> | | |
|||name of test dataset 2 for test cohort 1,  description for test dataset 2 for test cohort 1 --> | | |
| 26 |PARTNERS|logo, Name of organisation, ">Read more" and arrow to navigate to details | | |
|||name of test additional organisation 1 | | |
|||name of test additional organisation 2 | | |
| 27 |NETWORKS|List of networks which this cohort is involved in | | |
|||logo, Name of network, ">Read more?  And arrow to navigate to details | | |
|||name of test network2 | | |
|||> Read more | | |
|||name of test network of networks | | |
|||> Read more | | |
|||name of test network1 | | |
|||> Read more | | |
| 28|ACCESS CONDITIONS|Data access conditions description text cohort 1 - yes fee | | |
||Conditions|general research use, health or medical or biomedical research | | |
||Release  |Release description cohort 1 | | |
||Release type |closed dataset | | |
||Linkage options|Linkage options cohort 1 | | |
||Linkage possibility description|linkage possibility description cohort 1 | | |
||Data access fee|true | | |
||Prelinked|true | | |
||Data holder |test lead organisation 1 | | |
||Data use conditions|genetic studies only, publication required, ethics approval required | | |
| 29|ATTACHED FILES|Documents | | |
|||Card per document, clickable so that you open the document - test doc2 for test cohort 1 and test documentation for cohort 1 | | |
| 30|Click on test documentation for cohort 1|test documentation for cohort 1 opens in a separate window | | |
