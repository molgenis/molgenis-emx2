# Number

5

# Role

Naïve user of the data catalogue

# Goal

A naïve visitor to the data catalogue can click around in the catalogue and understand what s/he is seeing when viewing a cohort in detail.

# Steps

| Step | Action | Expected result | Github bug/issue | Playwright test |
| -----| -------| ----------------| -----------------| ----------------|
| 1 | Navigate to [MOLGENIS ACC test catalogue](https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue) | Landing page: European health research data and sample catalogue | | |
| 2 | Navigate to Search All | Cohorts, Data sources, Variables and Networks buttons are shown with numbers of each under each button | | |
| 3 | Click on the Cohorts button | Cohorts overview page with a list of all cohorts in the system | | |
| 4 | Type "test cohort" in the Search field top left | See that two cohorts are presented in the list: "acronym for test cohort 1" and "acronym for test cohort 2" | | |
| 5 | Click on "Acronym for test cohort 2" | See that the cohort detail page for Acronym for test cohort 2 is presented: "All > cohorts", ACRONYM FOR TEST COHORT 2, Name for test cohort 2 | | |
| 6 |Click on Contact| pop-up with "Name of test cohort 2", Contact, Name entry field, Email entry field, Message entry field, "or contact us at: molgenis-support@umcg.nl", Send button | | |
| 7 |Type in test message to your own email address and press 'Send'|an email is sent to the email address you filled in | | |
| 8 ||come back to main cohort detail view | | |
| 9 | click on website link|go to molgenis.org in a new window | | |
| 10 |close website and return to catalogue|return to cohort detail view page | | |
| 11 | See that the following is visible on the left hand side of the screen| (logo for cohort),Description,General design,Contact & contributors,Available data & samples,Subpopulations,Collection events,Datasets,Networks,Partners,Access conditions,Funding  & citation requirements,Attached files | | |
| 12 | See that the fields are filled as follows:| | | |
| 13 |First block: |small logo (test logo for cohort 2), www.molgenis.org, Contact button | | |
| 14 |DESCRIPTION|This is the Test cohort 2. It has "other" options where possible. No end year, so "ongoing". Design paper = Birth of a cohort — the first 20 years of the Raine study, publications = other papers. No to data access fee. Here we have some extra text to check the read less / read more functionality on the cohort detail page. | | |
| 15 |Click on the three dots next to the description text|the text is displayed fully, and clicking on 'read less' reduces the text again | | |
| 16 | |GENERAL DESIGN| | |
| 17 |Cohort type|Other type | | |
| 18 |Design|Longitudinal | | |
| 19 |Hover on the I next to 'Longitudinal'|hover text is 'repeated observations at different time-points' | | |
| 20 |Design description|Description of the design used for cohort 2 | | |
| 21 |Design schematic|Design schematic | | |
| 22 |Click on design schematic|an image (from FORCE NEN) is downloaded into the Download folder on the PC | | |
| 23 |Collection type|Retrospective | | |
| 24 |Start/End year|1955 - ongoing | | |
| 25 |Population|United Kingdom of Great Britain and Northern Ireland (the) | | |
| 26 |Regions|Bradford | | |
| 27 |Number of participants|100 | | |
| 28 |Number of participants with samples|50 | | |
| 29 |Population age groups|Prenatal, Child (2-12 years) | | |
| 30 |Inclusion criteria|Other inclusion criteria cohort 2 | | |
| 31 |Main medical condition|VIII Diseases of the ear and mastoid process | | |
| 32 |Population disease|F10 Mental and behavioural disorders due to use of alcohol, F11 Mental and behavioural disorders due to use of opioids | | |
| 33 |population oncology topology|KIDNEY, Kidney, NOS | | |
| 34 |population oncology morphology|Carcinomatosis | | |
| 35 |Marker paper|Birth of a cohort — the first 20 years of the Raine study | | |
| 36 |Click on the marker paper|goes to the article https://onlinelibrary.wiley.com/doi/10.5694/mja12.10698 in a new window | | |
| 37 | close the window or reselect the catalogue display window| | | |
| 38 |Publications| | | |
| 39 |PID|PID for test cohort 2 | | |
| 40 |CONTACT AND CONTRIBUTORS|1 card per contact person with: (titles) (initials) ((first name)) (surname prefix) (surname), (email address), (description of role) | | |
|||dr.  A.L.T.E.R. (cohort2alternativefirst) surname prefix cohort2alternativelast  --- testemailalternative@testdomain.nl  ---  test alternative contact for cohort 2 | | |
| 41 |click on the email address of the contact person|local email system is opened and an email addressed to testemailalternative@testdomain.nl is started | | |
| 42 |return to cohort detail view| | | |
| 43 |AVAILABLE DATA & SAMPLES|Data categories [all with explanatory hover text next to each category]   | | |
|||> Survey data | | |
|||Sample categories [no hover text] | | |
|||> Isolated Pathogen | | |
|||> Saliva | | |
|||Areas of information [no hover text] | | |
|||> Health and community care services utilization [consisting of the following when expanded:] | | |
|||>> Visits to health professionals | | |
|||>> Hospitalizations | | |
|||>> Community and social care | | |
|||>> Other health and community care | | |
|||> Cognition, personality and psychological measures and assessments [consisting of the following when expanded:] | | |
|||>> Cognitive functioning | | |
|||>> Personality | | |
|||>> Psychological distress and emotions | | |
|||>> Other psychological measures and assessments | | |
| 44 |SUBPOPULATIONS|List of subcohorts or subpopulations for this resource | | |
|||table with the following columns: Name, Description, Number of participants, with an arrow to navigate to details | | |
|||test subcohort 2A, description for test subcohort 2A, 3874, -->
| 45 |Click on test subcohort 2A|pop-up with information on the test subcohort 2A | | |
|||TEST SUBCOHORT 2A | | |
|||description for test subcohort 2A | | |
|||Number of participants  3874 | | |
|||Start/end year  1950 - ongoing | | |
|||Age categories  Adolescent (13-17 years) | | |
|||Main medical condition | | |
|||> H60-H62 Diseases of external ear | | |
|||> VIII Diseases of the ear and mastoid process | | |
|||> H65-H75 Diseases of middle ear and mastoid | | |
|||> H80-H83 Diseases of inner ear | | |
|||> H90-H95 Other disorders of ear | | |
|||Comorbidity | | |
|||> L00-L08 Infections of the skin and subcutaneous tissue | | |
|||> XII Diseases of the skin and subcutaneous tissue | | |
||| > L10-L14 Bullous disorders | | |
|||> L20-L30 Dermatitis and eczema | | |
|||> L40-L45 Papulosquamous disorders | | |
|||> L50-L54 Urticaria and erythema | | |
|||> L55-L59 Radiation-related disorders of the skin and subcutaneous tissue | | |
|||> L60-L75 Disorders of skin appendages | | |
|||> L80-L99 Other disorders of the skin and subcutaneous tissue | | |
|||Population  United Kingdom of Great Britain and Northern Ireland (the) | | |
|||Other inclusion criteria  test inclusion criteria for subcohort 2A | | |
| 46 |Click the cross top right to close the pop-up|Come back to cohort detail page | | |
| 47 |COLLECTION EVENTS|List of collection events defined for this resource | | |
|||table with the following columns: Name, Description, Participants, Start end year | | |
|||test collection event for cohort 2, description of test collection event for cohort 2, 15000, 1956-1999, --> | | |
| 48 |Click on test collection event for cohort 2|pop-up with information on the test collection event for cohort 2 | | |
|||TEST COLLECTION EVENT FOR COHORT 2 | | |
|||description of test collection event for cohort 2 | | |
|||Subcohorts  test subcohort 2A | | |
|||Number of participants 15000 | | |
|||Start/end year  1956 - 1999 | | |
|||Age categories  Prenatal  All ages | | |
|||Areas of information | | |
|||> Health and community care services utilization | | |
|||> Cognition, personality and psychological measures and assessments | | |
|||Data Categories  Survey data | | |
|||Sample categories  Isolated Pathogen  Saliva | | |
| 49 |Click the cross top right to close the pop-up|Come back to cohort detail page | | |
| 50 |DATASETS|DATASETS | | |
|||List of datasets for this resource | | |
|||Table with the following columns: Name, Description | | |
|||test dataset for testCohort2 description is empty (should be label for test dataset for cohort 2) | | |
| 51 |Click on name of dataset|pop-up appears with the following information | | |
|||TEST DATASET FOR TESTCOHORT2 | | |
|||test description of dataset for cohort 2 | | |
|||Label  label for test dataset for cohort 2 | | |
|||Keywords  <many, many keywords, starting with "distance (traffic)" and ending with "unhealthy_facilities_density_osm" | | |
|||Number of rows 83737 | | |
|||Since version  1959 | | |
|||Until version  1985 | | |
||Click the cross top right to close the pop-up|Come back to cohort detail page | | |
| 52 |PARTNERS|logo, Name of organisation, ">Read more" and arrow to navigate to details | | |
|||name of test additional organisation 2 | | |
| 53 |NETWORKS|List of networks to which this resource belongs
|||logo, Name of network, ">Read more?  And arrow to navigate to details | | |
|||name of test network2 | | |
|||> Read more | | |
|||name of test network of networks | | |
|||> Read more | | |
|||name of test network1 | | |
|||> Read more | | |
| 54 |Click on 'name of test network of networks'|go to molgenis.org in a new window | | |
| 55 |return to cohort detail view| | | |
| 56 |ACCESS CONDITIONS|Data access conditions description text cohort 2 - no fee | | |
||Conditions|general research use | | |
||Release  |Release description cohort 2 | | |
||Release type |Annually | | |
||Linkage options| Linkage options cohort 2 | | |
||Linkage possibility description|linkage possibility description cohort 2 | | |
||Data access fee|false | | |
||Prelinked|false | | |
||Data holder | | | |
||Data use conditions|publication required | | |
| 57 |FUNDING & CITATION REQUIREMENTS|Funding  This is the funding statement for cohort 1. It's not too long but covers more than one line, I think, particularly if I fill it up with blah blah blah (check for "until now" at the end) <lorem ipsum text repeated several times> UNTIL NOW | | |
|||Citation requirements  This is the acknowledgement statement for cohort 1. It's not too long but covers more than one line, I think, particularly if I fill it up with blah blah blah (check for "until now" at the end) <lorem ipsum text repeated several times> UNTIL NOW | | |
| 58 |ACKNOWLEDGEMENTS FIELD|Acknowledgements field to be filled in and tested when built | | |
| 59 |ATTACHED FILES|Documents | | |
|||Card per document, clickable so that you open the document - TEST SIGNED CODE OF CONDUCT FOR COHORT 2 opens in a new window when you click on it | | |