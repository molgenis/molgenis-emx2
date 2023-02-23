from decouple import config
from client import Session
from update_2_9 import TransformGeneral
from update_2_9 import TransformShared
from update import TransformDataCatalogue
from update import TransformDataStaging
from spaces import Spaces
from zip_handling import Zip

# Data model details
DATA_MODEL_VERSION = config('MG_DATA_MODEL_VERSION')

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

print('-----   ----')
#
# print('Updating catalogue data model to version ' + DATA_MODEL_VERSION)
#
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

# extract data from UMCG schema
print('Extract data from ' + CATALOGUE_SCHEMA_NAME + ': ' + CATALOGUE_SCHEMA_NAME + '_data.zip')
session.download_zip(database_name=CATALOGUE_SCHEMA_NAME)

# transform data from catalogue
print('Transform data from ' + CATALOGUE_SCHEMA_NAME)
# get instances of classes
zip_handling = Zip(CATALOGUE_SCHEMA_NAME)
zip_handling_shared_staging = Zip(SHARED_STAGING_NAME)
update_general = TransformGeneral(CATALOGUE_SCHEMA_NAME, 'catalogue_2.9')

# run download and transform functions
zip_handling.unzip_data()
zip_handling_shared_staging.unzip_data()
update_general.delete_data_model_file()  # delete molgenis.csv from data folder
update_general.update_data_model_file()
zip_handling.zip_data()

print('----------------')

# extract data from SharedStaging
print('Extract data from ' + SHARED_STAGING_NAME + ': ' + SHARED_STAGING_NAME + '_data.zip')
session.download_zip(database_name=SHARED_STAGING_NAME)

# update datamodel for SharedStaging
print('Transform data from ' + SHARED_STAGING_NAME)
# call instances
zip_handling = Zip(SHARED_STAGING_NAME)
update_general = TransformGeneral(SHARED_STAGING_NAME, 'SharedStagingUMCG')
update_shared = TransformShared(SHARED_STAGING_NAME)

# run update functions
zip_handling.remove_unzipped_data()
zip_handling.unzip_data()
update_general.delete_data_model_file()
update_general.update_data_model_file()
update_shared.transform_gdpr()
zip_handling.zip_data()
zip_handling.remove_unzipped_data()

# Cohorts update
print('-----------------------')
print('Cohort data update to data model ' + DATA_MODEL_VERSION)
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

    # transform data from cohorts
    print('Transform data from ' + cohort)
    zip_handling = Zip(cohort)
    update_general = TransformGeneral(cohort, 'cohort_UMCG_2.9')

    zip_handling.remove_unzipped_data()
    zip_handling.unzip_data()
    update_general.delete_data_model_file()
    update_general.update_data_model_file()
    zip_handling.zip_data()
    zip_handling.remove_unzipped_data()
    # delete and create new cohort schema
    schema_description = session.get_database_description(database_name=cohort)
    session.drop_database(database_name=cohort)
    session.create_database(database_name=cohort, database_description=schema_description)

# delete schemas UMCG and SharedStaging
print('------------------------')
print('Updating catalogue and SharedStaging schemas')


# delete and create new UMCG schema
schema_description = session.get_database_description(database_name=CATALOGUE_SCHEMA_NAME)
session.drop_database(database_name=CATALOGUE_SCHEMA_NAME)
session.create_database(database_name=CATALOGUE_SCHEMA_NAME, database_description=schema_description)

# delete SharedStaging schema
schema_description = session.get_database_description(database_name=SHARED_STAGING_NAME)
session.drop_database(database_name=SHARED_STAGING_NAME)
session.create_database(database_name=SHARED_STAGING_NAME, database_description=schema_description)

# upload transformed SharedStaging data to SharedStaging schema
session.upload_zip(database_name=SHARED_STAGING_NAME, data_to_upload=SHARED_STAGING_NAME)

# upload transformed data to UMCG schema
session.upload_zip(database_name=CATALOGUE_SCHEMA_NAME, data_to_upload=CATALOGUE_SCHEMA_NAME)


# per cohort:
# Cohorts update
print('-----------------------')

print('Updating data for cohorts')
# sign in to server
print('Sign in to server: ' + SERVER_URL)
session = Session(
    url=SERVER_URL,
    email=SERVER_USERNAME,
    password=SERVER_PASSWORD
)

for cohort in COHORTS:
    print('Upload transformed data for: ' + cohort)
    session.upload_zip(database_name=cohort, data_to_upload=cohort)
