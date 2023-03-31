from decouple import config
from util.client import Session
from ETL.etl import TransformGeneral
from util.zip_handling import Zip

# Server details
SERVER_URL = config('MG_SERVER_URL')
SERVER_USERNAME = config('MG_SERVER_USERNAME')
SERVER_PASSWORD = config('MG_SERVER_PASSWORD')
CATALOGUE_SCHEMA_NAME = config('MG_CATALOGUE_SCHEMA_NAME')
ONTOLOGIES_SCHEMA_NAME = config('MG_ONTOLOGIES_SCHEMA_NAME')
SHARED_STAGING_NAME = config('MG_SHARED_STAGING_NAME')

COHORTS = config('MG_COHORTS', cast=lambda v: [s.strip() for s in v.split(',')])
NETWORKS = config('MG_NETWORKS', cast=lambda v: [s.strip() for s in v.split(',')])

print('-----  Config variables loaded ----')

print('SERVER_URL: ' + SERVER_URL)
print('SERVER_USERNAME: ' + SERVER_USERNAME)
print('SERVER_PASSWORD: ******')
print('CATALOGUE_SCHEMA_NAME: ' + CATALOGUE_SCHEMA_NAME)

print('---------')

# sign in to server
print('Sign in to server: ' + SERVER_URL)
session = Session(
    url=SERVER_URL,
    email=SERVER_USERNAME,
    password=SERVER_PASSWORD
)

# Cohorts update
print('-----------------------')
# sign in to server
print('Sign in to server: ' + SERVER_URL)
session = Session(
    url=SERVER_URL,
    email=SERVER_USERNAME,
    password=SERVER_PASSWORD
)
for cohort in COHORTS:
    # extract data
    print('Extract data for ' + cohort + ': ' + cohort + '_data.zip')
    session.download_zip(database_name=cohort)

    # transform data
    print('Transform data from ' + cohort)
    zip_handling = Zip(cohort)
    update_general = TransformGeneral(cohort)

    zip_handling.unzip_data()
    update_general.delete_molgenis_files()
    zip_handling.zip_data()

    # load data
    session.upload_zip(database_name=CATALOGUE_SCHEMA_NAME, data_to_upload=cohort)

# Networks update
print('-----------------------')
# sign in to server
print('Sign in to server: ' + SERVER_URL)
session = Session(
    url=SERVER_URL,
    email=SERVER_USERNAME,
    password=SERVER_PASSWORD
)
for network in NETWORKS:
    # extract data
    print('Extract data for ' + network + ': ' + network + '_data.zip')
    session.download_zip(database_name=network)

    # transform data
    print('Transform data from ' + network)
    zip_handling = Zip(network)
    update_general = TransformGeneral(network)

    zip_handling.unzip_data()
    update_general.delete_molgenis_files()
    zip_handling.zip_data()

    # load data
    session.upload_zip(database_name=CATALOGUE_SCHEMA_NAME, data_to_upload=network)

