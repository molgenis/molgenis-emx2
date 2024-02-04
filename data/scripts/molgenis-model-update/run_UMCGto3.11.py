import setuptools.discovery
from decouple import config
from util.client import Session
from update.update_3_11 import Transform
from util.zip_handling import Zip
import os

if not os.path.isdir('./files'):
    os.mkdir('./files')

os.chdir('./files')

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
DATA_SOURCES = config('MG_DATA_SOURCES', casst=lambda v: [s.strip() for s in v.split(',')])
NETWORKS = config('MG_NETWORKS', casst=lambda v: [s.strip() for s in v.split(',')])

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

# --------------------------------------------------------------

# extract data from catalogue schema
print('Extract data from ' + CATALOGUE_SCHEMA_NAME + ': ' + CATALOGUE_SCHEMA_NAME + '_data.zip')
session.download_zip(database_name=CATALOGUE_SCHEMA_NAME)

# transform data from catalogue schema
print('Transform data from ' + CATALOGUE_SCHEMA_NAME)
# get instances of classes
zip_handling = Zip(CATALOGUE_SCHEMA_NAME)
update = Transform(CATALOGUE_SCHEMA_NAME, 'catalogue')

# run zip and transform functions
zip_handling.unzip_data()
update.delete_data_model_file()  # delete molgenis.csv from data folder
update.update_data_model_file()
update.transform_data()
zip_handling.zip_data()

# --------------------------------------------------------------

# Cohorts update
print('-----------------------')
print('Cohort data update to data model ' + DATA_MODEL_VERSION)

for cohort in COHORTS:
    # sign in to server
    print('Sign in to server: ' + SERVER_URL)
    session = Session(
        url=SERVER_URL,
        email=SERVER_USERNAME,
        password=SERVER_PASSWORD
    )
    # extract data
    print('Extract data for ' + cohort + ': ' + cohort + '_data.zip')
    session.download_zip(database_name=cohort)

    # transform data from cohorts
    print('Transform data from ' + cohort)
    zip_handling = Zip(cohort)
    update = Transform(cohort, 'cohort_UMCG')

    zip_handling.remove_unzipped_data()
    zip_handling.unzip_data()
    update.delete_data_model_file()
    update.transform_data()
    update.update_data_model_file()
    zip_handling.zip_data()
    zip_handling.remove_unzipped_data()
    # delete and create new cohort schema
    schema_description = session.get_database_description(database_name=cohort)
    session.drop_database(database_name=cohort)
    session.create_database(database_name=cohort, database_description=schema_description)

# --------------------------------------------------------------

# Data sources update
print('-----------------------')
print('Data source update to data model ' + DATA_MODEL_VERSION)

for data_source in DATA_SOURCES:
    # sign in to server
    print('Sign in to server: ' + SERVER_URL)
    session = Session(
        url=SERVER_URL,
        email=SERVER_USERNAME,
        password=SERVER_PASSWORD
    )
    # extract data
    print('Extract data for ' + data_source + ': ' + data_source + '_data.zip')
    session.download_zip(database_name=data_source)

    # transform data from cohorts
    print('Transform data from ' + data_source)
    zip_handling = Zip(data_source)
    update = Transform(data_source, 'data_source')

    zip_handling.remove_unzipped_data()
    zip_handling.unzip_data()
    update.delete_data_model_file()
    update.transform_data()
    update.update_data_model_file()
    zip_handling.zip_data()
    zip_handling.remove_unzipped_data()
    # delete and create new cohort schema
    schema_description = session.get_database_description(database_name=data_source)
    session.drop_database(database_name=data_source)
    session.create_database(database_name=data_source, database_description=schema_description)

# Networks update
print('-----------------------')
print('Networks update to data model ' + DATA_MODEL_VERSION)

for network in NETWORKS:
    # sign in to server
    print('Sign in to server: ' + SERVER_URL)
    session = Session(
        url=SERVER_URL,
        email=SERVER_USERNAME,
        password=SERVER_PASSWORD
    )
    # extract data
    print('Extract data for ' + network + ': ' + network + '_data.zip')
    session.download_zip(database_name=network)

    # transform data from cohorts
    print('Transform data from ' + network)
    zip_handling = Zip(network)
    update = Transform(network, 'network')

    zip_handling.remove_unzipped_data()
    zip_handling.unzip_data()
    update.delete_data_model_file()
    update.transform_data()
    update.update_data_model_file()
    zip_handling.zip_data()
    zip_handling.remove_unzipped_data()
    # delete and create new cohort schema
    schema_description = session.get_database_description(database_name=network)
    session.drop_database(database_name=network)
    session.create_database(database_name=network, database_description=schema_description)

# ---------------------------------------------------------------

# delete and create schemas
print('------------------------')
print('Updating catalogue schema')
# delete and create new UMCG schema
schema_description = session.get_database_description(database_name=CATALOGUE_SCHEMA_NAME)
session.drop_database(database_name=CATALOGUE_SCHEMA_NAME)
session.create_database(database_name=CATALOGUE_SCHEMA_NAME, database_description=schema_description)

# upload molgenis.csv to catalogue schema
update_general = Transform(CATALOGUE_SCHEMA_NAME, 'catalogue')
data_model_file = update_general.update_data_model_file()
session.upload_zip(database_name=CATALOGUE_SCHEMA_NAME, data_to_upload='catalogue_data_model')

# upload transformed catalogue data to catalogue schema
session.upload_zip(database_name=CATALOGUE_SCHEMA_NAME, data_to_upload=CATALOGUE_SCHEMA_NAME)

# ----------------------------------------------------------------------

# Cohorts upload data
print('-----------------------')

print('Updating data for cohorts')

for cohort in COHORTS:
    # sign in to server
    print('Sign in to server: ' + SERVER_URL)
    session = Session(
        url=SERVER_URL,
        email=SERVER_USERNAME,
        password=SERVER_PASSWORD
    )
    print('Upload transformed data for: ' + cohort)
    session.upload_zip(database_name=cohort, data_to_upload=cohort)

# Data sources upload data
print('-----------------------')

print('Updating data for data sources')

for data_source in DATA_SOURCES:
    # sign in to server
    print('Sign in to server: ' + SERVER_URL)
    session = Session(
        url=SERVER_URL,
        email=SERVER_USERNAME,
        password=SERVER_PASSWORD
    )
    print('Upload transformed data for: ' + data_source)
    session.upload_zip(database_name=data_source, data_to_upload=data_source)

# Networks upload data
print('-----------------------')

print('Updating data for networks')

for network in NETWORKS:
    # sign in to server
    print('Sign in to server: ' + SERVER_URL)
    session = Session(
        url=SERVER_URL,
        email=SERVER_USERNAME,
        password=SERVER_PASSWORD
    )
    print('Upload transformed data for: ' + network)
    session.upload_zip(database_name=network, data_to_upload=network)
