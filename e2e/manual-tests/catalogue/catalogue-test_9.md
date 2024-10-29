# Number

9

# Role

Naïve user of the data catalogue

# Goal

A naïve visitor to the data catalogue can click around in the catalogue and understand what they are seeing when viewing a resource in detail. Multiple values in fields are displayed correctly. Not all fields on the page are tested (See test plan 5 for full test of this page).

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| -----| -------| ----------------| -----------------| ----------------|
| 1 | Navigate to [MOLGENIS ACC test catalogue](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue) | Landing page: European health research data and sample catalogue | | |
| 2 | Navigate to Search All | Cohort studies, Data sources, Variables and Networks buttons are shown with numbers of each under each button | | |
| 3 | Click on the Cohort studies button | Cohort studies overview page with a list of all cohort studies in the system | | |
| 4 | Type "Name for test cohort" in the Search field top left | See that two cohorts are presented in the list: "acronym for test cohort 1" and "acronym for test cohort 2" | | |
| 5 | Click on "acronym for test cohort 1" | See that the resource information page for test cohort 1 is presented: "All > cohort studies", acronym for test cohort 1, Name for test cohort 1 | | |
| 6 | See that the fields are filled as follows:| | | |
| 7 | DESCRIPTION | This is the Test cohort 1. It has multiple entries where possible. Design paper and publications are random DOIs from the EUCAN project. Yes to data access fee. The population age group is all adult options, so the population age group should be shown as "Adult 18+ years". Cohort type has both 'normal' and 'other' fields filled in --> see which is displayed | | |
| | Click on the three dots next to the description text | the text is displayed fully, and clicking on 'read less' reduces the text again | | |
| | GENERAL DESIGN | | | |
| 8a | Type | Clinical trial, Cohort study | | |
| 8b | Cohort type | Clinical cohort, Case-control | | |
| 9a | Data collection type | Retrospective, Prospective | | |
| 9b | Keywords | test, cohort1, Molgenis, testing | | |
| 10 | Start/End data collection | 1900 until 1967 | | |
| 11 | Design paper | Cohort Profile: The French national cohort of children (ELFE): birth to 5 years | | |
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
| 22a | ORGANISATIONS | | | |
| | | Lead organisations | | |
| | | 1 card per organisation: | | |
| | | Name for lead organisation 1 (ACRONYM FOR LEADORG1) \| Botswana, Holy See (the) \| Data originator, Data holder, Data provider, Researcher, Surveillance, Other | | |
| | | name for test lead organisation 2 (acronym for test lead organisation 2) \| Bonaire, Sint Eustatius and Saba, Réunion \| Data provider, Other | | |
| | | Additional organisations | | |
| | | 1 card per organisation: | | |
| | | name for test additional organisation 2 (acronym for test additional organisation 2) \| Bosnia and Herzegovina, Mayotte \| Data provider, Surveillance | | |
| | | name of test additional organisation 1 (acronym for test additional organisation 1) \| Canada, Western Sahara* \| Researcher, Surveillance | | |
| 22b | CONTRIBUTORS | | | |
| | | 1 card per contributor: | | |
| | | dr. ir. P.I.P.M. (PIPM) surname prefix last name PIPM \| Name for lead organisation 1 \| <pipm@testdomain.nl> \| Principal Investigator, Project manager | | |
| | | dr. D.M. (datamgr first name) datamgr surname prefix datamgr last name \| Name for lead organisation 1 \| <test@email.nl> \| Data manager | | |
| 23 | SUBPOPULATIONS | List of subpopulations for this resource | | |
| | | table with the following columns: Name, Description, Number of participants | | |
| | | test subcohort 1A, description for test subcohort 1A, 956 --> | | |
| | | test subcohort 1B, description for test subcohort 1B, 23487 --> | | |
| 24 | COLLECTION EVENTS | List of collection events defined for this resource | | |
| | | table with the following columns: Name, Description, Participants, Start end year | | |
| | | test collection event 1A, test description for test collection event 1A, 3500, 1958-04-01 until 1994-04-30 --> | | |
| | | test collection event 1B, description for test collection event 1B, 286, 1992-03-01 until 2000-08-31 --> | | |
| 25 | DATASETS | List of datasets for this resource | | |
| | | Table with the following columns: Name, Description | | |
| | | test dataset for testCohort1, test description for dataset 1 --> | | |
| | | name of test dataset 2 for test cohort 1, description for test dataset 2 for test cohort 1 --> | | |
| 26 | NETWORKS | Part of networks | | |
| | | logo, Name of network, >Website, --> to navigate to details | | |
| | | name for test network2, >Website, --> | | |
| | | name for test network of networks, >Website, --> | | |
| | | name for test network1, >Website, --> | | |
| 27 | ACCESS CONDITIONS | Data access conditions description text cohort 1 - yes fee | | |
| | Data access conditions | general research use, health or medical or biomedical research | | |
| | Data use conditions | genetic studies only, publication required, ethics approval required | | |
| | Data access fee | true | | |
| | Release type | closed dataset | | |
| | Release description | Release description cohort 1 | | |
| | Prelinked | true | | |
| | Linkage options | Linkage options cohort 1 | | |
| 28 | DOCUMENTATION | Card per document, clickable so that you open the document: test doc 2 for test cohort 1 & test documentation for cohort 1 | | |
| 29 | Click on test documentation for cohort 1 | test documentation for cohort 1 is downloaded locally | | |
