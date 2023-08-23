# molgenis-py-model-update

Python library to update catalogue data model from version 2.8 to 3.0.

## system requirements

- Python 3 (3.8.10)
- Git

## Initial one-time setup

Use virtual env to get a consistent python environment.

1. Clone the github repository

    'git clone git@github.com:molgenis/molgenis-emx2'

2. Create a virtual python environment at the location of the script

    'cd data\scripts\molgenis-model-update'

    `python -m venv venv`

3. Activate the virtual python environment

    `source venv/bin/activate`

4. Install the script dependencies from requirements.txt file

    `pip install -r requirements.txt`

    More info see:

    mac: [https://www.youtube.com/watch?v=Kg1Yvry_Ydk](https://www.youtube.com/watch?v=Kg1Yvry_Ydk)

    windows: [https://www.youtube.com/watch?v=APOPm01BVrk](https://www.youtube.com/watch?v=APOPm01BVrk)

5. Define environment variables in .env, see .env_example

6. Run script for update of data catalogue staging from 2.8 to 3.x

    'runDataCatalogueStaging.py'

   Run script for update of UMCG server from 2.9 to 3.x

    'run_UMCG.py'

   Run script for update of UMCG server to 2.9

    'run_UMCG_2.8to2.9.py'
