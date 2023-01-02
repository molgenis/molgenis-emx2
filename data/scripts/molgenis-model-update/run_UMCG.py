from decouple import config
from client import Session
from update import TransformGeneral
from update import TransformDataCatalogue
from update import TransformDataStagingCohorts
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

print('Updating catalogue data model to version ' + DATA_MODEL_VERSION)

# sign in to server for UMCG data
print('Sign in to server, schema: ' + CATALOGUE_SCHEMA_NAME)
session = Session(
    url=SERVER_URL,
    database=CATALOGUE_SCHEMA_NAME,
    email=SERVER_USERNAME,
    password=SERVER_PASSWORD
)
# extract data from UMCG schema
print('Extract data from ' + CATALOGUE_SCHEMA_NAME + ': ' + CATALOGUE_SCHEMA_NAME + '_data.zip')
session.download_zip()

# sign in to server for SharedStaging data
print('Sign in to server, schema: ' + SHARED_STAGING_NAME)
session = Session(
    url=SERVER_URL,
    database=SHARED_STAGING_NAME,
    email=SERVER_USERNAME,
    password=SERVER_PASSWORD
)
# extract data from SharedStaging
print('Extract data from ' + SHARED_STAGING_NAME + ': ' + SHARED_STAGING_NAME + '_data.zip')
session.download_zip()

# transform data from catalogue
print('Transform data from ' + CATALOGUE_SCHEMA_NAME)
# get instances of classes
transform_data = TransformDataCatalogue(CATALOGUE_SCHEMA_NAME, 'UMCG')
zip_handling = Zip(CATALOGUE_SCHEMA_NAME)
zip_handling_shared_staging = Zip(SHARED_STAGING_NAME)
update_general = TransformGeneral(CATALOGUE_SCHEMA_NAME, 'catalogue')
spaces = Spaces(CATALOGUE_SCHEMA_NAME)

# run download and transform
zip_handling.unzip_data()
zip_handling_shared_staging.unzip_data()
update_general.delete_data_model_file()
transform_data.transform_data()
spaces.get_spaces()
# update_general.update_data_model_file()
zip_handling.zip_data()
zip_handling.remove_unzipped_data()

print('----------------')

# sign in to server
print('Sign in to server.')
session = Session(
    url=SERVER_URL,
    database=ONTOLOGIES_SCHEMA_NAME,
    email=SERVER_USERNAME,
    password=SERVER_PASSWORD
)
# extract data from CatalogueOntologies
print('Extract data from ' + ONTOLOGIES_SCHEMA_NAME + ': ' + ONTOLOGIES_SCHEMA_NAME + '_data.zip')
session.download_zip()

# transform data from CatalogueOntologies
print('Transform data from ' + ONTOLOGIES_SCHEMA_NAME)
transform_data = TransformDataCatalogue(ONTOLOGIES_SCHEMA_NAME, 'ontologies')
zip_handling = Zip(ONTOLOGIES_SCHEMA_NAME)
update_general = TransformGeneral(ONTOLOGIES_SCHEMA_NAME, 'ontologies')
spaces = Spaces(ONTOLOGIES_SCHEMA_NAME)

zip_handling.remove_unzipped_data()
zip_handling.unzip_data()
update_general.delete_data_model_file()
spaces.get_spaces()
zip_handling.zip_data()
zip_handling.remove_unzipped_data()


# Cohorts update
print('-----------------------')
print('Cohort data update to data model ' + DATA_MODEL_VERSION)
for cohort in COHORTS:
    # sign in to staging server
    print('Sign in to staging server for database: %s.' % cohort)
    session = Session(
        url=SERVER_URL,
        database=cohort,
        email=SERVER_USERNAME,
        password=SERVER_PASSWORD
    )
    # extract data
    print('Extract data for ' + cohort + ': ' + cohort + '_data.zip')
    session.download_zip()

    # transform data from cohorts
    print('Transform data from ' + cohort)
    transform_data = TransformDataStagingCohorts(cohort, 'cohort_UMCG')
    zip_handling = Zip(cohort)
    update_general = TransformGeneral(cohort, 'cohort_UMCG')
    spaces = Spaces(cohort)

    zip_handling.unzip_data()
    update_general.delete_data_model_file()
    transform_data.transform_data()
    spaces.get_spaces()
    zip_handling.zip_data()
    zip_handling.remove_unzipped_data()

# delete schemas UMCG and CatalogueOntologies
# create schemas UMCG and CatalogueOntologies
# upload molgenis.csv to UMCG schema
# upload transformed CatalogueOntologies data to CatalogueOntologies schema
# upload transformed data to UMCG schema
# per cohort:
# Cohorts update
print('-----------------------')

print('Updating schemas for cohorts')
for cohort in COHORTS:
    # sign in to staging server
    print('Sign in to staging server for database: %s.' % cohort)
    session = Session(
        url=SERVER_URL,
        database=cohort,
        email=SERVER_USERNAME,
        password=SERVER_PASSWORD
    )
    print('Delete and create cohort schema:' + cohort)
    session.drop_database()
#     update_general = TransformGeneral(database=cohort, database_type='cohort_UMCG')
#     data_model = update_general.data_model_file()
#     print(data_model)
    session.create_database(database_name=cohort[5:])
# #     # create cohort schema minus 'UMCG_' (cohort[5:])
# #     # upload transformed cohort data
#
