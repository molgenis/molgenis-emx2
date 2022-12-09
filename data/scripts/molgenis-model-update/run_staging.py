from decouple import config
from session import Session
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
print('SHARED_STAGING_NAME: ' + SHARED_STAGING_NAME)

print('-----   ----')

print('Updating catalogue data model to version ' + DATA_MODEL_VERSION)

# sign in to server
print('Sign in to server.')
session = Session(
    url=SERVER_URL,
    database=CATALOGUE_SCHEMA_NAME,
    email=SERVER_USERNAME,
    password=SERVER_PASSWORD
)
# extract data from catalogue
print('Extract data from ' + CATALOGUE_SCHEMA_NAME + ': ' + CATALOGUE_SCHEMA_NAME + '_data.zip')
session.download_zip()

# transform data from catalogue
print('Transform data from ' + CATALOGUE_SCHEMA_NAME)
# get instances of classes
transform_data = TransformDataCatalogue(CATALOGUE_SCHEMA_NAME)
zip_handling = Zip(CATALOGUE_SCHEMA_NAME)
update_general = TransformGeneral(CATALOGUE_SCHEMA_NAME, 'catalogue')
spaces = Spaces(CATALOGUE_SCHEMA_NAME)

# run download, transform and upload
zip_handling.remove_unzipped_data()
zip_handling.unzip_data()
update_general.delete_data_model_file()
transform_data.transform_data()
spaces.get_spaces()
# update_general.update_data_model_file()
zip_handling.zip_data()

# # upload data from catalogue
# print('Load data from ' + CATALOGUE_SCHEMA_NAME + ': ' + CATALOGUE_SCHEMA_NAME +  '_upload.zip')
# download_upload.upload_zip()

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
transform_data = TransformDataCatalogue(ONTOLOGIES_SCHEMA_NAME)
zip_handling = Zip(ONTOLOGIES_SCHEMA_NAME)
update_general = TransformGeneral(ONTOLOGIES_SCHEMA_NAME, 'ontologies')
spaces = Spaces(ONTOLOGIES_SCHEMA_NAME)

zip_handling.remove_unzipped_data()
zip_handling.unzip_data()
update_general.delete_data_model_file()
spaces.get_spaces()
zip_handling.zip_data()

# # upload data from CatalogueOntologies
# print('Load data from ' + ONTOLOGIES_SCHEMA_NAME + ': ' + ONTOLOGIES_SCHEMA_NAME +  '_upload.zip')
# download_upload.upload_zip()


# Networks update
print('-----------------------')
print('Network data update to data model ' + DATA_MODEL_VERSION)

for network in NETWORKS:
    # sign in to staging server
    print('Sign in to staging server for database: %s.' % network)
    session = Session(
        url=SERVER_URL,
        database=network,
        email=SERVER_USERNAME,
        password=SERVER_PASSWORD
    )
    # extract data
    print('Extract data for ' + network + ': ' + network + '_data.zip')
    session.download_zip()

# TODO: add network transform
########



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

    # transform data from CatalogueOntologies
    print('Transform data from ' + ONTOLOGIES_SCHEMA_NAME)
    transform_data = TransformDataStagingCohorts(cohort, 'staging')
    zip_handling = Zip(cohort)
    update_general = TransformGeneral(cohort, 'staging')
    spaces = Spaces(cohort)

    zip_handling.remove_unzipped_data()
    zip_handling.unzip_data()
    update_general.delete_data_model_file()
    transform_data.transform_data()
    spaces.get_spaces()
    # update_general.update_data_model_file()
    zip_handling.zip_data()


# delete schemas DataCatalogue and CatalogueOntologies
# create schemas catalogue and CatalogueOntologies
# upload molgenis.csv to catalogue schema
# upload transformed CatalogueOntologies data to CatalogueOntologies schema
# upload transformed data to catalogue schema
# per network:
    # delete schema
    # create schema
    # upload transformed data
# per cohort:
    # delete cohort schema
    # create cohort schema minus 'UMCG_' (cohort[5:])
    # upload transformed cohort data
