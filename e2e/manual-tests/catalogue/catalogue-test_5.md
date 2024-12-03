# Number

5

# Role

Naïve user of the data catalogue

# Goal

A naïve visitor to the data catalogue can click around in the catalogue and understand what they are seeing when viewing a resource in detail.

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| ---- | ------ | --------------- | ---------------- | --------------- |
| 0 | NB: Assumptions | This test plan assumes that the settings are configured to display the contact form rather than just an e-mail address. | | |
| 1 | Navigate to [testCatalogue on the acceptance server](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue) | Landing page: European health research data and sample catalogue | | |
| 2 | Navigate to Search All |  `COLLECTIONS`, `NETWORKS`, and `VARIABLES` buttons are shown with the number of each under each button | | |
| 3a | Click on the `COLLECTIONS` button | Should be directed to the list of collections with: collection logo, "COLLECTIONS", "Data & sample collections", `DETAILED`/`COMPACT` toggle buttons (`DETAILED` is selected by default), Filters on the LHS. | | |
| 3b | Click on `COMPACT` | The list of collections turns into a list of collection acronyms and names (in full), with an arrow after each collection | | |
| 4 | Type "name for test cohort" in the Search field top left | See that two collections are presented in the list: "acronym for test cohort 1" and "acronym for test cohort 2" | | |
| 5a | Click on "Acronym for test cohort 2" | See that the resource detail page for Acronym for test cohort 2 is presented: "All > Collections", ACRONYM FOR TEST COHORT 2, Name for test cohort 2 | | |
| 5b | | In the ribbon at the top of the page there should be: Left: Molgenis logo, Right: (L-R) `OVERVIEW`, `COLLECTIONS`, `NETWORKS`, `VARIABLES`, `MORE` (-> `Other catalogues`, `Upload data`) | | |
| 6 | Click on the `CONTACT` button | pop-up with "Name for test cohort 2", Contact, Name entry field, Email entry field, Organisation entry field, Topic dropdown, Message entry field, "or contact us at: <support@molgenis.org>", `SEND` button | | |
| 7 | Type in test message, fill in your own email address and press `SEND` | An e-mail is sent to Molgenis Support. | | |
| 8 | Close the notification, if any pops up | Return to the detailed page for "acronym for test cohort 2". | | |
| 9 | Click on website link | Go to molgenis.org in a new window. | | |
| 10 | Close website and return to catalogue | Return to resource detail view page. | | |
| 11 | See that the following is visible on the left hand side of the screen | (logo for resource), Description, General design, Population, Organisations, Contributors, Available Data & Samples, Subpopulations, Collection events, Datasets, Networks, Publications, Access Conditions, Funding & Acknowledgements, Documentation | | |
| 12 | See that the fields are filled as follows: | | | |
| 13 | First block: | small logo (test logo for cohort 2), <https://www.molgenis.org>, Contact button | | |
| 14 | DESCRIPTION | This is the Test cohort 2. It has "other" options where possible. No end year, so "ongoing". Design paper = Birth of a cohort — the first 20 years of the Raine study, publications = other papers. No to data access fee. Here we have some extra text to... | | |
| 15 | Click on the three dots next to the description text | the text is displayed fully ("check the read less / read more functionality on the cohort detail page" is added) , and clicking on 'read less' reduces the text again | | |
| 16 | GENERAL DESIGN | | | |
| 17 | Type | Cohort study | | |
| 17b | Cohort type | Other type | | |
| 18 | Data collection type | Retrospective | | |
| 19 | Design | Longitudinal ⓘ | | |
| 20 | Hover on the ⓘ next to 'Longitudinal' | The hover text is 'repeated observations at different time-points'. | | |
| 21 | Design description | Description of the design used for cohort 2 | | |
| 22 | Design schematic | Design schematic | | |
| 23 | Click on Design schematic | An image (from FORCE NEN) is downloaded. | | |
| 24 | Start/End data collection | 1955 (ongoing) | | |
| 25 | Design paper | Birth of a cohort — the first 20 years of the Raine study. | | |
| 26a | Click on the design paper | Opens [the article](https://onlinelibrary.wiley.com/doi/10.5694/mja12.10698) in a new window. | | |
| 26b | Return to the catalogue display window. | | | |
| 27 | PID | `https://pid-for-testcohort2.org` | | |
| 27b| External identifiers| EUDRACT number: test external identifier for cohort 2 | | |
| 28 | POPULATION | | | |
| 29 | Countries | United Kingdom of Great Britain and Northern Ireland (the) | | |
| 30 | Regions | Bradford | | |
| 31 | Number of participants | 100 | | |
| 32 | Number of participants with samples | 50 | | |
| 33 | Population age groups | Prenatal, Child (2-12 years) | | |
| 34 | Population oncology topology | KIDNEY. Consists of the following when expanded: | | |
| | | Kidney, NOS | | |
| 35 | Population oncology morphology | Epithelial Neoplasms, NOS. Consists of the following when expanded: | | |
| | | Carcinomatosis | | |
| 36 | Main medical condition | VIII Diseases of the ear and mastoid process | | |
| 37 | Inclusion criteria | Age of majority inclusion criterion ⓘ | | |
| 38 | Other inclusion criteria | Other inclusion criteria cohort 2 | | |
| 39 | ORGANISATIONS | | | |
| | | Lead organisations | | |
| | | 1 card with organisation: name for test lead organisation 2 (acronym for test lead organisation 2) \| Bonaire, Sint Eustatius and Saba \| Data provider | | |
| | | Additional organisations | | |
| | | 1 card with organisation: name for test additional organisation 2 (acronym for test additional organisation 2) \| Bosnia and Herzegovina \| Data provider, Surveillance | | |
| 40 | CONTRIBUTORS | | | |
| | | 1 card with contributor: dr.  A.L.T.E.R. (cohort2alternativefirst) surname prefix cohort2alternativelast \| name for test lead organisation 2 \| <testemailalternative@testdomain.nl> \| Alternative contact | | |
| 41 | Click on the email address of the contact person | The local email system opens and an email addressed to <testemailalternative@testdomain.nl> is opened. | | |
| 42 | Return to the resource information page | | | |
| 43 | AVAILABLE DATA & SAMPLES | Data categories | | |
| | | > Survey data ⓘ [hover text] | | |
| | | Sample categories | | |
| | | > Fluids and Secretions [consisting of the following when expanded:] | | |
| | | >> Saliva | | |
| | | Areas of information | | |
| | | > Health and community care services utilization [consisting of the following when expanded:] | | |
| | | >> Visits to health professionals | | |
| | | >> Hospitalizations | | |
| | | >> Community and social care | | |
| | | >> Other health and community care | | |
| | | > Cognition, personality and psychological measures and assessments [consisting of the following when expanded:] | | |
| | | >> Cognitive functioning | | |
| | | >> Personality | | |
| | | >> Psychological distress and emotions | | |
| | | >> Other psychological measures and assessments | | |
| 44 | SUBPOPULATIONS | List of subpopulations for this resource | | |
| | | table with the following columns: Name, Description, Number of participants | | |
| | | test subcohort 2A, description for test subcohort 2A, 3874, -> | | |
| 45 | Click on test subcohort 2A | Pop-up with information on the test subcohort 2A appears | | |
| | | TEST SUBCOHORT 2A | | |
| | | description for test subcohort 2A | | |
| | | Number of participants 3874 | | |
| | | Start/end year 1950 (ongoing) | | |
| | | Age categories Adolescent (13-17 years) | | |
| | | Main medical condition | | |
| | | > H60-H62 Diseases of external ear | | |
| | | > H65-H75 Diseases of middle ear and mastoid | | |
| | | > H80-H83 Diseases of inner ear | | |
| | | > H90-H95 Other disorders of ear | | |
| | | > VIII Diseases of the ear and mastoid process | | |
| | | Comorbidity | | |
| | | > L00-L08 Infections of the skin and subcutaneous tissue | | |
| | | > L10-L14 Bullous disorders | | |
| | | > L20-L30 Dermatitis and eczema | | |
| | | > L40-L45 Papulosquamous disorders | | |
| | | > L50-L54 Urticaria and erythema | | |
| | | > L55-L59 Radiation-related disorders of the skin and subcutaneous tissue | | |
| | | > L60-L75 Disorders of skin appendages | | |
| | | > L80-L99 Other disorders of the skin and subcutaneous tissue | | |
| | | > XII Diseases of the skin and subcutaneous tissue | | |
| | | Countries United Kingdom of Great Britain and Northern Ireland (the) | | |
| | | Other inclusion criteria test inclusion criteria for subcohort 2A | | |
| 46 | Click the cross top right to close the pop-up | Come back to resource detail page | | |
| 47 | COLLECTION EVENTS | List of collection events defined for this resource | | |
| | | Table with the following columns: Name, Description, Participants, Start end year | | |
| | | test collection event for cohort 2, description of test collection event for cohort 2, 15000, 1956-06-01 until 1999-12-31, -> | | |
| 48 | Click on test collection event for cohort 2 | Pop-up with information on the test collection event for cohort 2 appears | | |
| | | TEST COLLECTION EVENT FOR COHORT 2 | | |
| | | description of test collection event for cohort 2 | | |
| | | Subpopulations test subcohort 2A | | |
| | | Number of participants 15000 | | |
| | | Start/end year 1956-06-01 until 1999-12-31 | | |
| | | Age categories Prenatal All ages | | |
| | | Areas of information | | |
| | | > Health and community care services utilization | | |
| | | > Cognition, personality and psychological measures and assessments | | |
| | | Data Categories Survey data ⓘ [hover text] | | |
| | | Sample categories | | |
| | | > Fluids and Secretions | | |
| 49 | Click the cross top right to close the pop-up | Return to the resource information page. | | |
| 50 | DATASETS | DATASETS | | |
| | | List of datasets for this resource | | |
| | | Table with the following columns: Name, Description | | |
| | | test dataset for testCohort2 test description of dataset for cohort 2 | | |
| 51 | Click on the name of the dataset | A pop-up appears with the following information: | | |
| | | TEST DATASET FOR TESTCOHORT2 | | |
| | | test description of dataset for cohort 2 | | |
| | | Label label for test dataset for cohort 2 | | |
| | | Keywords <many, many keywords> | | |
| | | Number of rows 83737 | | |
| | | Since version 1959 | | |
| | | Until version 1985 | | |
| 52 | Click the cross top right to close the pop-up | Return to resource information page. | | |
| 53 | NETWORKS, subtitle: Part of networks | List of networks in which this resource is involved. For each network: logo, name, > Website, and an arrow to navigate to details. | | |
| | | name for test network2 | | |
| | | > Website | | |
| | | name for test network of networks | | |
| | | > Website | | |
| | | name for test network1 | | |
| | | > Website | | |
| 54 | Click on 'name for test network of networks' | Molgenis.org opens in a new window. | | |
| 55 | Return to the resource information page | | | |
| 56a | PUBLICATIONS | | | |
| | | Clickable block per publication with the name of the publication as a clickable link which opens the link in a new window: "Birth of a cohort--the first 20 years of the Raine study", "test publication 2", "test publication 3" | | |
| | Click on test publication 2 | The paper "Maternal Dietary Glycemic Index and Glycemic Load in Pregnancy and Offspring Cord Blood DNA Methylation" opens in a new window. | | |
| 56b | Return to the resource information page | | | |
| 57 | ACCESS CONDITIONS | Data access conditions description text cohort 2 - no fee | | |
| | Data access conditions | general research use | | |
| | Data use conditions | publication required ⓘ | | |
| | Data access fee | false | | |
| | Release type | Annually | | |
| | Release description | Release description cohort 2 | | |
| | Prelinked | false | | |
| | Linkage options | Linkage options cohort 2 | | |
| 58 | FUNDING & ACKNOWLEDGEMENTS | Funding This is the funding statement for cohort 2. It's not too long but covers more than one line, I think, particularly if I fill it up with blah blah blah (check for "until now" at the end to make sure all the text has been displayed) lorem ipsum text repeated several times UNTIL NOW | | |
| | | Acknowledgements This is the acknowledgement statement for cohort 2. It's not too long but covers more than one line, I think, particularly if I fill it up with blah blah blah (check for "until now" at the end to make sure all the text has been displayed) lorem ipsum text repeated several times UNTIL NOW | | |
| 59 | DOCUMENTATION | | | |
| | | Card per document, clickable so that the document is opened in a new window. | | |
| | | test signed code of conduct for cohort 2 | | |
