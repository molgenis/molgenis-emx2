# Number

9

# Role

Naïve user of the data catalogue

# Goal

A naïve visitor to the data catalogue can click around in the catalogue and understand what they are seeing when viewing a resource in detail. Multiple values in fields are displayed correctly. Not all fields on the page are tested (see test plan 5 for full test of this page).

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| -----| -------| ----------------| -----------------| ----------------|
| 1 | Navigate to [testCatalogue on the acceptance server](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/catalogue)| Landing page: European health research data and sample catalogue | | |
| 2 | Click the `SEARCH ALL` button | `COLLECTIONS`, `NETWORKS` and `VARIABLES` buttons are with the number of each under the button | | |
| 3 | Click on the `COLLECTIONS` button | Collections overview page with a list of all collections in the system | | |
| 4 | Type "Name for test cohort" in the Search field top left | See that two cohorts are presented in the list: "acronym for test cohort 1" and "acronym for test cohort 2" | | |
| 5 | Click on "acronym for test cohort 1" | See that the resource information page for test cohort 1 is presented: "All > Collections", acronym for test cohort 1, Name for test cohort 1 | | |
| 6 | See that the fields are filled as follows:| | | |
| 7 | DESCRIPTION | This is the Test cohort 1. It has multiple entries where possible. Design paper and publications are random DOIs from the EUCAN project. Yes to data access fee. The population age group is all adult options, so the population age group should be shown as "Adult 18+ years". Cohort type has both 'normal' and 'other' fields filled in --> see which is displayed | | |
| | Click on the three dots next to the description text | the text is displayed fully, and clicking on 'read less' reduces the text again | | |
| | GENERAL DESIGN | | | |
| 8a | Type | Cohort study | | |
| 8b | Cohort type | Clinical cohort, Case-control | | |
| 9 | Data collection type | Retrospective, Prospective | | |
| 10 | Start/End data collection | 1950 until 1967 | | |
| 11 | Design paper | Cohort Profile: The French national cohort of children (ELFE): birth to 5 years | | |
| 11a| PID | https://pid-for-testcohort1.org | | |
| 11b| External identifiers | Clinical Trials.gov: test external identifier for cohort 1 | | |
| 12 | Click on the design paper | goes to the article in a new window: <https://doi.org/10.1093/ije/dyz227> [Cohort Profile: The French national cohort of children (ELFE): birth to 5 years] | | |
| 13 | close the window or reselect the catalogue display window | | | |
| | POPULATION | | | |
| 14 | Countries | Armenia, Bermuda, Réunion | | |
| 15 | Regions | Bradford, Lapland, Gipuzkoa | | |
| 16 | Population age groups | Adolescent (13-17 years), Adult (18+ years) | | |
| 17 | Main medical condition | Displays two main items: VII Diseases of the eye and adnexa & VIII Diseases of the ear and mastoid process, with many subitems each | | |
| 19 | Population oncology topology | BASE OF TONGUE. Consists of the following when expanded: | | |
| | | Base of tongue, NOS | | |
| | | UTERUS, NOS. Consists of the following when expanded: | | |
| | | Uterus, NOS | | |
| 20 | Population oncology morphology | Epithelial neoplasms, NOS. Consists of the following when expanded: | | |
| | | Carcinoma, metastatic, NOS. | | |
| | | Carcinomatosis. | | |
| 21 | Inclusion criteria | Clinically relevant exposure inclusion criterion | | |
| | | Clinically relevant lifestyle inclusion criterion | | |
| | | Country of residence inclusion criteria | | |
| 22 | Other inclusion criteria | Other inclusion criteria cohort 1 | | |
| 23 | Exclusion criteria | Clinically relevant exposure inclusion criterion | | |
| | | Clinically relevant lifestyle inclusion criterion | | |
| | | Country of residence inclusion criteria | | |
| 24| Other exclusion criteria | Other exclusion criteria cohort 1 | | |
| 25a | ORGANISATIONS | | | |
| | | Lead organisations | | |
| | | 1 card per organisation: | | |
| | | Name for lead organisation 1 (ACRONYM FOR LEADORG1) \| Botswana, Holy See (the) \| Data originator, Data holder, Data provider, Researcher, Surveillance, Other | | |
| | | name for test lead organisation 2 (acronym for test lead organisation 2) \| Bonaire, Sint Eustatius and Saba, Réunion \| Data provider, Other | | |
| | | Additional organisations | | |
| | | 1 card per organisation: | | |
| | | name for test additional organisation 2 (acronym for test additional organisation 2) \| Bosnia and Herzegovina, Mayotte \| Data provider, Surveillance | | |
| | | name of test additional organisation 1 (acronym for test additional organisation 1) \| Canada, Western Sahara* \| Researcher, Surveillance | | |
| 25b | CONTRIBUTORS | | | |
| | | 1 card per contributor: | | |
| | | dr. ir. P.I.P.M. (PIPM) surname prefix last name PIPM \| Name for lead organisation 1 \| <pipm@testdomain.nl> \| Principal Investigator, Project manager | | |
| | | dr. D.M. (datamgr first name) datamgr surname prefix datamgr last name \| Name for lead organisation 1 \| <test@email.nl> \| Data manager | | | |
| 26 | Available Data & Samples | | |
| | Data categories | Imaging data, Medical records, National registries, Genealogical records | | |
| | Hover over 'ⓘ' | Details about each data category appears | | |
| | Sample categories | Blood, Genetic material | | |
| | Areas of information | Health and community care services utilization, Laboratory measures | | |
| 27 | SUBPOPULATIONS | List of subpopulations for this resource | | |
| | | table with the following columns: Name, Description, Number of participants | | |
| | | test subcohort 1A, description for test subcohort 1A, 956 --> | | |
| | | test subcohort 1B, description for test subcohort 1B, 23487 --> | | |
| 28 | COLLECTION EVENTS | List of collection events defined for this resource | | |
| | | table with the following columns: Name, Description, Participants, Start end year | | |
| | | test collection event 1A, test description for test collection event 1A, 3500, 1958-04-01 until 1994-04-30 --> | | |
| | | test collection event 1B, description for test collection event 1B, 286, 1992-03-01 until 2000-08-31 --> | | |
| 29 | DATASETS | List of datasets for this resource | | |
| | | Table with the following columns: Name, Description | | |
| | | test dataset for testCohort1, test description for dataset 1 --> | | |
| | | name of test dataset 2 for test cohort 1, description for test dataset 2 for test cohort 1 --> | | |
| 30 | NETWORKS | Part of networks | | |
| | | logo, Name of network, >Website, --> to navigate to details | | |
| | | name for test network2, >Website, --> | | |
| | | name for test network of networks, >Website, --> | | |
| | | name for test network1, >Website, --> | | |
| 31 | PUBLICATIONS | Three publications: 'Birth of a cohort--..Raine study.',  'Cohort Profile: The French...birth to 5 years', Effevtiveness of AS04-adjuvanted...community randomised trial' | | |
| 32 | ACCESS CONDITIONS | Data access conditions description text cohort 1 - yes fee | | |
| | Data access conditions | general research use, health or medical or biomedical research | | |
| | Data use conditions | genetic studies only, publication required, ethics approval required | | |
| | Data access fee | true | | |
| | Release type | closed dataset | | |
| | Release description | Release description cohort 1 | | |
| | Prelinked | true | | |
| | Linkage options | Linkage options cohort 1 | | |
| 33 | FUNDING & ACKNOWLEDGEMENTS | lorem ipsum text, check that "until now" is shown at the end of both fields so that you know the full text has been displayed | | |
| 34 | DOCUMENTATION | Card per document, clickable so that you open the document: test doc 2 for test cohort 1 & test documentation for cohort 1 | | |
| 29 | Click on test documentation for cohort 1 | test documentation for cohort 1 is downloaded locally | | |
