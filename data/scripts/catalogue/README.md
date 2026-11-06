# molgenis-py-model-update

Python library to update catalogue and rd3 data model and migrate data on catalogue servers.
Most catalogue servers use staging areas where data managers fill out data for e.g. cohorts or networks/catalogues. These staging areas have references to the main catalogue schema. Therefore, data migration has to be performed in a particular order.

1. The target server (e.g. the acceptance server or another test server) is updated to the correct software using awx. The target server needs to be an empty emx2 server.
2. A catalogue schema is made on the target server with template (DATA_CATALOGUE) or a RD3 schema (PATIENT_REGISTRY), which also instantiates the CatalogueOntologies schema and imports the appropriate ontology data
3. Migration program is run, see steps 4-7.
4. Data from the main catalogue schema data is downloaded from a source server (usually the production server) and transformed. The molgenis.csv is deleted and an updated version is added.
5. The transformed data is uploaded to the catalogue schema on the target server.
6. Data from staging areas is downloaded and transformed, staging areas are deleted and new staging areas with the same name are made. The molgenis.csv is deleted and an updated version is added.
7. Transformed data is uploaded to staging areas on the target server.
8. Other schemas that have no Profiles table have to still be moved 'as is' by hand.


## system requirements

- Python 3 (3.8.10)
- Git

## Initial one-time setup

Use virtual env to get a consistent python environment.

1. Clone the github repository

    'git clone git@github.com:molgenis/molgenis-emx2'

2. Create a virtual python environment at the location of the scripts

    'cd data\scripts\catalogue'

    `python -m venv venv`

3. Activate the virtual python environment

    `source venv/bin/activate`
    for windows: 'venv\Scripts\activate.bat'

4. Install the catalogue migration script dependencies from requirements.txt file

    `pip install -r requirements.txt`

    More info see:

    mac: [https://www.youtube.com/watch?v=Kg1Yvry_Ydk](https://www.youtube.com/watch?v=Kg1Yvry_Ydk)

    windows: [https://www.youtube.com/watch?v=APOPm01BVrk](https://www.youtube.com/watch?v=APOPm01BVrk)

5. Define environment variables in .env, see .env_example

6. Run script for update of servers after selecting the right run.py, e.g.

    'run_8.0.py'

## Update to 10.0: contact email moves to Contacts

The column `contact email` of Resources is removed. Each email becomes a row in Contacts:

- An email that belongs to an existing contact of the resource uses that contact.
- Another email gives a new contact with role 'Primary contact'. The name comes from the email:
  `tiina.eriksson@tuni.fi` gives Tiina Eriksson, `info@nkcx.se` gives 'Contact info'.
- The first email becomes the `contact point`, unless the resource already has one.
- No email is lost. A field with more than one email gives more than one contact.

The script prints a line for each change. Check the lines with 'NEW contact', and correct the names on the
target server. Check also the setting `contactRecipientsQuery`: it must not use `contactEmail`.

Run the checks for this update with `python test_update_10_0.py`.
