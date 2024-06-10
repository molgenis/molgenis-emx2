# Changelog
## Version 1.19.0 (development)

## Version 1.18.1
- Paediatric categories are combined and infectious now includes covid19

## Version 1.18.0
- Derivation of new categories

## Version 1.17.0
- Get staging data instead of published data from the external servers

## Version 1.16.1
- Fix error when deleting biobanks with no PID (issue #68)

## Version 1.16.0
- Ability to add/remove a WITHDRAWN status to PIDs

## Version 1.15.2
- Fix a bug in the derivation of paediatric and paediatric included categories

## Version 1.15.0
- Columns used for mapping ICD-10 to Orphanet changed from matching_high/medium to
  exact_mapping and ntbt_mapping.

## Version 1.14.0
- Besides adding the RD category to collections having "ORPHA" diagnosis_available, now
  also collection having collection type = RD (and no ORPHA or other (than ORPHA)
  diagnosis_available) get the RD-category assigned.

## Version 1.13.1
- Fix error in staging: Also known in should be emptied after networks and not before

## Version 1.13.0
- Map matching disease codes to categories
- Remove check on multiple age_units as datatype has changed to categorical

## Version 1.12.0
- Include publishing of Facts tables

## Version 1.11.0
- Add withdrawn column for withdrawn national nodes

## Version 1.10.0
- Validation of hyperlinks

## Version 1.9.0
BBMRI-model changes as described in this [document](https://docs.google.com/document/d/1h-xkU6uR8jytDnMm7wT38bk0ZdmXzywZ/edit?usp=sharing&ouid=103886117563984453369&rtpof=true&sd=true).
Main changes are:
- Add also_known_in tables and column to staging areas
- Move head info from biobanks and collections to persons

## Version 1.8.0
- The 'combined quality' field in collections is automatically filled during publishing
- The 'biobank_label' field in collections is automatically filled during publishing

## Version 1.7.0
- The 'categories' field in collections is automatically filled during publishing
- The 'covid19biobank' column is merged into the 'capabilities' column during publishing

## Version 1.6.0
- Improves indexing performance:
    - by publishing all nodes at once
    - by using the Import API

## Version 1.5.0
- Adds step to fill combined_network field
- Fix issue #61, import of NL data fails due to self-referencing columns

## Version 1.4.0
- PIDs are prefixed with `1.` (example: `1.6ed7-328b-2793`)

## Version 1.3.0
- The PID feature can be turned off completely by supplying a NoOpPidService

## Version 1.2.1
- PID URLs are based on a config parameter instead of the session

## Version 1.2.0
- Change PID format to use a random 12 digit hexadecimal number (example: `6ed7-328b-2793`)
- Introduced a DummyPidService to test publishing without interacting with a Handle server
- Fixed issue #35 References to EU networks and persons don't pass validation
- Fixed issue #41 Rows of node EU are overwritten by other nodes that reference them

## Version 1.1.0
- Persistent Identifiers (PIDs) for biobanks
    - New biobanks are automatically assigned a PID
    - Biobank name changes are reflected in its PID record
    - Removal of a biobank is reflected in its PID record
- Removed command-line "eric" command
- Fixed issue #42 ID validation should not allow @ and .
- Fixed issue #43 Current ID validation results in false-positive invalid IDs

## Version 1.0.4
- Fixed issue #36 Deleted rows from a staging area are not deleted from the published ERIC tables
- Fixed issue #38 Parent and sub collections are empty in the ERIC collections table
- Fixed an issue where references were not validated correctly

## Version 1.0.3
- Fixed critical error when running on Python 3.6 or lower

## Version 1.0.2
- Fixed support for Python 3.6 by including the [dataclasses backport](https://pypi.org/project/dataclasses/)

## Version 1.0.1
- Adds library and command line tool for staging, validating, enriching and publishing national nodes of BBMRI-ERIC
