from decouple import config
from util.client import Session
from ETL.etl import TransformGeneral
from ETL.etl import CohortsETL
from util.zip_handling import Zip

# Server details
SERVER_URL = config('MG_SERVER_URL')
SERVER_USERNAME = config('MG_SERVER_USERNAME')
SERVER_PASSWORD = config('MG_SERVER_PASSWORD')
CATALOGUE_SCHEMA_NAME = config('MG_CATALOGUE_SCHEMA_NAME')
ONTOLOGIES_SCHEMA_NAME = config('MG_ONTOLOGIES_SCHEMA_NAME')
SHARED_STAGING_NAME = config('MG_SHARED_STAGING_NAME')

COHORTS = config('MG_COHORTS', cast=lambda v: [s.strip() for s in v.split(',')])

print('-----  Config variables loaded ----')

print('SERVER_URL: ' + SERVER_URL)
print('SERVER_USERNAME: ' + SERVER_USERNAME)
print('SERVER_PASSWORD: ******')
print('CATALOGUE_SCHEMA_NAME: ' + CATALOGUE_SCHEMA_NAME)
print('ONTOLOGIES_SCHEMA_NAME: ' + ONTOLOGIES_SCHEMA_NAME)
print('SHARED_STAGING_NAME: ' + SHARED_STAGING_NAME)

print('---------')

# sign in to server
print('Sign in to server: ' + SERVER_URL)
session = Session(
    url=SERVER_URL,
    email=SERVER_USERNAME,
    password=SERVER_PASSWORD
)
# extract data from SharedStaging
print('Extract data from ' + SHARED_STAGING_NAME + ': ' + SHARED_STAGING_NAME + '_data.zip')
session.download_zip(database_name=SHARED_STAGING_NAME)

# delete molgenis files from SharedStaging
print('Transform data from ' + SHARED_STAGING_NAME)
# initiate instances to transform SharedStaging
zip_handling = Zip(SHARED_STAGING_NAME)
update_general = TransformGeneral(SHARED_STAGING_NAME)

# run transform functions
zip_handling.unzip_data()
update_general.delete_molgenis_files()
zip_handling.zip_data()

# load SharedStaging to UMCG
session.upload_zip(database_name=CATALOGUE_SCHEMA_NAME, data_to_upload=SHARED_STAGING_NAME)

# load SharedStaging to CatalogueOntologies
session.upload_zip(database_name=ONTOLOGIES_SCHEMA_NAME, data_to_upload=SHARED_STAGING_NAME)

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
    update_cohorts = CohortsETL(cohort, CATALOGUE_SCHEMA_NAME)

    zip_handling.unzip_data()
    update_general.delete_molgenis_files()
    update_cohorts.cohorts()
    zip_handling.zip_data()

    # load data
    session.upload_zip(database_name=CATALOGUE_SCHEMA_NAME, data_to_upload=cohort)

# MANUALLY REMOVE CONTACTS AND CONTRIBUTORS FROM UMCG SCHEMA WHEN NO CONSENT FOR CONTACT DETAILS DISPLAY
