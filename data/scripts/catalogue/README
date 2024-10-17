# molgenis-py-model-update

Python library to update catalogue data model and migrate data on catalogue servers.
Some catalogue servers use staging where data managers fill out data for e.g. cohorts or networks.
These staging areas have references to the main catalogue schema. Therefore, data migration has to be
done in a particular order.

1. Data from the main catalogue schema data is downloaded and transformed.
2. Data from staging areas is downloaded and transformed, staging areas are deleted and new staging areas with the
same name are made.
3. The main catalogue schema is deleted
4. CatalogueOntologies schema is deleted
5. Updated data model is uploaded to catalogue schema
6. CatalogueOntologies data is uploaded from repository to CatalogueOntologies
7. Transformed data uploaded to catalogue schema
8. Transformed data is uploaded to staging areas.

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

6. Run script for update of servers after selecting the right update program at import,
   e.g. 'from update.update_4_x import Transform'

    'run.py'
