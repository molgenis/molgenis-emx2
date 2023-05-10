from decouple import config
from util.client import Session
from update.update_3_x import Transform
from update.update_3_x import TransformDataCatalogue
from update.update_3_x import TransformShared
from update.update_3_x import TransformDataStaging
from update.spaces_for_3x import Spaces
from util.zip_handling import Zip

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

print('Updating catalogue data model to version ' + DATA_MODEL_VERSION)

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

# --------------------------------------------------------------

# extract data from UMCG schema
print('Extract data from ' + CATALOGUE_SCHEMA_NAME + ': ' + CATALOGUE_SCHEMA_NAME + '_data.zip')
session.download_zip(database_name=CATALOGUE_SCHEMA_NAME)

# unzip SharedStaging data
zip_handling = Zip(SHARED_STAGING_NAME)
zip_handling.unzip_data()

# transform data from UMCG schema
print('Transform data from ' + CATALOGUE_SCHEMA_NAME)
# get instances of classes
transform_data = TransformDataCatalogue(CATALOGUE_SCHEMA_NAME, 'UMCG')
zip_handling = Zip(CATALOGUE_SCHEMA_NAME)
update_general = Transform(CATALOGUE_SCHEMA_NAME, 'catalogue')
spaces = Spaces(CATALOGUE_SCHEMA_NAME)

# run zip and transform functions
zip_handling.unzip_data()
update_general.delete_data_model_file()  # delete molgenis.csv from data folder
transform_data.transform_data()
spaces.get_spaces()
zip_handling.zip_data()

# --------------------------------------------------------------

# extract data from CatalogueOntologies
print('Extract data from ' + ONTOLOGIES_SCHEMA_NAME + ': ' + ONTOLOGIES_SCHEMA_NAME + '_data.zip')
session.download_zip(database_name=ONTOLOGIES_SCHEMA_NAME)

# transform data from CatalogueOntologies
print('Transform data from ' + ONTOLOGIES_SCHEMA_NAME)
# call instances
transform_data = TransformDataCatalogue(ONTOLOGIES_SCHEMA_NAME, 'ontologies')
zip_handling = Zip(ONTOLOGIES_SCHEMA_NAME)
update_general = Transform(ONTOLOGIES_SCHEMA_NAME, 'ontologies')
spaces = Spaces(ONTOLOGIES_SCHEMA_NAME)

# run zip and transform functions
zip_handling.remove_unzipped_data()
zip_handling.unzip_data()
update_general.delete_data_model_file()
spaces.get_spaces()
zip_handling.zip_data()
zip_handling.remove_unzipped_data()

# ---------------------------------------------------------------

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
    transform_data = TransformDataStaging(cohort, 'cohort_UMCG')
    zip_handling = Zip(cohort)
    update_general = Transform(cohort, 'cohort_UMCG')
    spaces = Spaces(cohort)

    zip_handling.remove_unzipped_data()
    zip_handling.unzip_data()
    update_general.delete_data_model_file()
    transform_data.transform_data()
    spaces.get_spaces()
    update_general.update_data_model_file()
    zip_handling.zip_data()
    zip_handling.remove_unzipped_data()
    # delete and create new cohort schema
    schema_description = session.get_database_description(database_name=cohort)
    session.drop_database(database_name=cohort)
    session.create_database(database_name=cohort[5:], database_description=schema_description)

# --------------------------------------------------------------

# transform data from SharedStaging schema
print('Transform data from ' + SHARED_STAGING_NAME)
# get instances of classes
transform_data = TransformShared(SHARED_STAGING_NAME, 'shared')
zip_handling = Zip(SHARED_STAGING_NAME)
update_general = Transform(SHARED_STAGING_NAME, 'shared')
spaces = Spaces(SHARED_STAGING_NAME)

# run zip and transform functions
update_general.delete_data_model_file()  # delete molgenis.csv from data folder
transform_data.organisations()
spaces.get_spaces()
update_general.update_data_model_file()
zip_handling.zip_data()

# ---------------------------------------------------------------

# delete and create schemas
print('------------------------')
print('Updating catalogue and CatalogueOntologies schemas')
# delete and create new UMCG schema
schema_description = session.get_database_description(database_name=CATALOGUE_SCHEMA_NAME)
session.drop_database(database_name=CATALOGUE_SCHEMA_NAME)
session.create_database(database_name=CATALOGUE_SCHEMA_NAME, database_description=schema_description)

# delete and create new SharedStaging schema
schema_description = session.get_database_description(database_name=SHARED_STAGING_NAME)
session.drop_database(database_name=SHARED_STAGING_NAME)
session.create_database(database_name=SHARED_STAGING_NAME, database_description=schema_description)

# delete and create new CatalogueOntologies schema
schema_description = session.get_database_description(database_name=ONTOLOGIES_SCHEMA_NAME)
session.drop_database(database_name=ONTOLOGIES_SCHEMA_NAME)
session.create_database(database_name=ONTOLOGIES_SCHEMA_NAME, database_description=schema_description)

# upload molgenis.csv to UMCG schema
update_general = Transform(CATALOGUE_SCHEMA_NAME, 'catalogue')
data_model_file = update_general.update_data_model_file()
session.upload_zip(database_name=CATALOGUE_SCHEMA_NAME, data_to_upload='catalogue_data_model')

# upload transformed CatalogueOntologies data to CatalogueOntologies schema
session.upload_zip(database_name=ONTOLOGIES_SCHEMA_NAME, data_to_upload=ONTOLOGIES_SCHEMA_NAME)

# upload transformed SharedStaging data to SharedStaging schema
session.upload_zip(database_name=SHARED_STAGING_NAME, data_to_upload=SHARED_STAGING_NAME)

# upload transformed UMCG data to UMCG schema
session.upload_zip(database_name=CATALOGUE_SCHEMA_NAME, data_to_upload=CATALOGUE_SCHEMA_NAME)

# ----------------------------------------------------------------------

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
    session.upload_zip(database_name=cohort[5:], data_to_upload=cohort)
