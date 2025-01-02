from decouple import config
from catalogue_util.client import Session
from update.update_5_x_old import Transform
from catalogue_util.zip_handling import Zip
import os

if not os.path.isdir('./files'):
    os.mkdir('./files')

os.chdir('./files')

# Data model details
DATA_MODEL_VERSION = config('MG_DATA_MODEL_VERSION')

# Source server details
SOURCE_SERVER_URL = config('MG_SOURCE_SERVER_URL')
SOURCE_SERVER_USERNAME = config('MG_SOURCE_SERVER_USERNAME')
SOURCE_SERVER_PASSWORD = config('MG_SOURCE_SERVER_PASSWORD')
SERVER_TYPE = config('MG_SERVER_TYPE')

# Target server details
TARGET_SERVER_URL = config('MG_TARGET_SERVER_URL')
TARGET_SERVER_USERNAME = config('MG_TARGET_SERVER_USERNAME')
TARGET_SERVER_PASSWORD = config('MG_TARGET_SERVER_PASSWORD')

CATALOGUES = config('MG_CATALOGUE_SCHEMA_NAME', cast=lambda v: [s.strip() for s in v.split(',')])
ONTOLOGIES_SCHEMA_NAME = config('MG_ONTOLOGIES_SCHEMA_NAME')
SHARED_STAGING_NAME = config('MG_SHARED_STAGING_NAME')

if SERVER_TYPE == 'data_catalogue' or SERVER_TYPE == 'cohort_catalogue':
    COHORTS = config('MG_COHORTS', cast=lambda v: [s.strip() for s in v.split(',')])
    print(COHORTS)

if SERVER_TYPE == 'data_catalogue':
    DATA_SOURCES = config('MG_DATA_SOURCES', cast=lambda v: [s.strip() for s in v.split(',')])
    NETWORKS = config('MG_NETWORKS', cast=lambda v: [s.strip() for s in v.split(',')])

print('-----  Config variables loaded ----')

print('SOURCE_SERVER_URL: ' + SOURCE_SERVER_URL)
print('SERVER_USERNAME: ' + SOURCE_SERVER_USERNAME)
print('SERVER_PASSWORD: ******')
print('SERVER_TYPE: ' + SERVER_TYPE)

print('TARGET_SERVER_URL: ' + TARGET_SERVER_URL)
print('TARGET_USERNAME: ' + TARGET_SERVER_USERNAME)
print('TARGET_PASSWORD: ******')

print('CATALOGUE_SCHEMA_NAME: ' + str(CATALOGUES))
print('ONTOLOGIES_SCHEMA_NAME: ' + ONTOLOGIES_SCHEMA_NAME)
print('SHARED_STAGING_NAME: ' + SHARED_STAGING_NAME)

print('-------------------')

print('Updating catalogue data model to version ' + DATA_MODEL_VERSION)

# sign in to source server
print('Sign in to source server: ' + SOURCE_SERVER_URL)
session = Session(
    url=SOURCE_SERVER_URL,
    email=SOURCE_SERVER_USERNAME,
    password=SOURCE_SERVER_PASSWORD
)

# --------------------------------------------------------------
# Catalogue schema update
print('-----------------------')
print('Catalogue schema update to data model ' + DATA_MODEL_VERSION)

# extract data from catalogue schema(s)
for catalogue in CATALOGUES:
    print(catalogue)
    # sign in to source server
    print('Sign in to source server: ' + SOURCE_SERVER_URL)
    session = Session(
        url=SOURCE_SERVER_URL,
        email=SOURCE_SERVER_USERNAME,
        password=SOURCE_SERVER_PASSWORD
    )
    # extract data
    print('Extract data from ' + catalogue + ': ' + catalogue + '_data.zip')
    session.download_zip(database_name=catalogue)

    # transform data from catalogue schema
    print('Transform data from ' + catalogue)
    # get instances of classes
    zip_handling = Zip(catalogue)
    update = Transform(database_name=catalogue, database_type='catalogue')

    # run zip and transform functions
    zip_handling.unzip_data()
    update.delete_data_model_file()  # delete molgenis.csv from data folder
    update.update_data_model_file()
    update.transform_data()
    zip_handling.zip_data()

# ---------------------------------------------------------------------------------------

if SERVER_TYPE in ['data_catalogue', 'cohort_catalogue']:
    # SharedStaging schema download
    print('-----------------------')

    # extract data from catalogue schema
    print('Extract data from ' + SHARED_STAGING_NAME + ': ' + SHARED_STAGING_NAME + '_data.zip')
    session.download_zip(database_name=SHARED_STAGING_NAME)

    # unzip data from shared staging schema
    print('Unzip data from ' + SHARED_STAGING_NAME)
    zip_handling = Zip(SHARED_STAGING_NAME)
    zip_handling.unzip_data()

# --------------------------------------------------------------
if SERVER_TYPE in ['data_catalogue', 'cohort_catalogue']:
    # Cohorts update
    print('-----------------------')
    print('Cohort staging schema update to data model ' + DATA_MODEL_VERSION)

    for cohort in COHORTS:
        print(cohort)
        # sign in to source server
        print('Sign in to source server: ' + SOURCE_SERVER_URL)
        session = Session(
            url=SOURCE_SERVER_URL,
            email=SOURCE_SERVER_USERNAME,
            password=SOURCE_SERVER_PASSWORD
        )
        # extract data
        print('Extract data for ' + cohort + ': ' + cohort + '_data.zip')
        session.download_zip(database_name=cohort)

        # transform data from cohorts
        print('Transform data from ' + cohort)
        zip_handling = Zip(cohort)
        if SERVER_TYPE == 'data_catalogue':
            update = Transform(database_name=cohort, database_type='cohort')
        elif SERVER_TYPE == 'cohort_catalogue':
            update = Transform(database_name=cohort, database_type='cohort_UMCU')

        zip_handling.remove_unzipped_data()
        zip_handling.unzip_data()
        update.delete_data_model_file()
        update.transform_data()
        update.update_data_model_file()
        zip_handling.zip_data()
        zip_handling.remove_unzipped_data()

        # get cohort database description from source server
        schema_description = session.get_database_description(database_name=cohort)

        # sign in to target server
        print('Sign in to target server: ' + TARGET_SERVER_URL)
        session = Session(
            url=TARGET_SERVER_URL,
            email=TARGET_SERVER_USERNAME,
            password=TARGET_SERVER_PASSWORD
        )
        # create cohort schema on target server
        session.create_database(database_name=cohort, database_description=schema_description)

# --------------------------------------------------------------
if SERVER_TYPE == 'data_catalogue':
    # Data sources update
    print('-----------------------')
    print('Data source update to data model ' + DATA_MODEL_VERSION)

    for data_source in DATA_SOURCES:
        # sign in to source server
        print('Sign in to source server: ' + SOURCE_SERVER_URL)
        session = Session(
            url=SOURCE_SERVER_URL,
            email=SOURCE_SERVER_USERNAME,
            password=SOURCE_SERVER_PASSWORD
        )
        # extract data
        print('Extract data for ' + data_source + ': ' + data_source + '_data.zip')
        session.download_zip(database_name=data_source)

        # transform data from data sources
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

        # get data source database description from source server
        schema_description = session.get_database_description(database_name=data_source)

        # sign in to target server
        print('Sign in to target server: ' + TARGET_SERVER_URL)
        session = Session(
            url=TARGET_SERVER_URL,
            email=TARGET_SERVER_USERNAME,
            password=TARGET_SERVER_PASSWORD
        )
        # create data source schema on target server
        session.create_database(database_name=data_source, database_description=schema_description)

    # Networks update
    print('-----------------------')
    print('Networks update to data model ' + DATA_MODEL_VERSION)

    for network in NETWORKS:
        # sign in to source server
        print('Sign in to source server: ' + SOURCE_SERVER_URL)
        session = Session(
            url=SOURCE_SERVER_URL,
            email=SOURCE_SERVER_USERNAME,
            password=SOURCE_SERVER_PASSWORD
        )
        # extract data
        print('Extract data for ' + network + ': ' + network + '_data.zip')
        session.download_zip(database_name=network)

        # transform data
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

        # get network database description from source server
        schema_description = session.get_database_description(database_name=network)

        # sign in to target server
        print('Sign in to target server: ' + TARGET_SERVER_URL)
        session = Session(
            url=TARGET_SERVER_URL,
            email=TARGET_SERVER_USERNAME,
            password=TARGET_SERVER_PASSWORD
        )
        # create network schema on target server
        session.create_database(database_name=network, database_description=schema_description)

# # ---------------------------------------------------------------
# # print('------------------------')
#
# # upload catalogue data to target server
# # sign in to target server
# for catalogue in CATALOGUES:
#     print('Sign in to target server: ' + TARGET_SERVER_URL)
#     session = Session(
#         url=TARGET_SERVER_URL,
#         email=TARGET_SERVER_USERNAME,
#         password=TARGET_SERVER_PASSWORD
#     )
#     session.upload_zip(database_name=catalogue, data_to_upload=catalogue)

# ----------------------------------------------------------------------

# Cohorts upload data
print('-----------------------')

if SERVER_TYPE in ['data_catalogue', 'cohort_catalogue']:
    print('Updating data for cohorts')
    for cohort in COHORTS:
        # sign in to target server
        print('Sign in to target server: ' + TARGET_SERVER_URL)
        session = Session(
            url=TARGET_SERVER_URL,
            email=TARGET_SERVER_USERNAME,
            password=TARGET_SERVER_PASSWORD
        )
        print('Upload transformed data for: ' + cohort)
        session.upload_zip(database_name=cohort, data_to_upload=cohort)

if SERVER_TYPE == 'data_catalogue':
    # Data sources upload data
    print('-----------------------')

    print('Updating data for data sources')

    for data_source in DATA_SOURCES:
        # sign in to target server
        print('Sign in to target server: ' + TARGET_SERVER_URL)
        session = Session(
            url=TARGET_SERVER_URL,
            email=TARGET_SERVER_USERNAME,
            password=TARGET_SERVER_PASSWORD
        )
        print('Upload transformed data for: ' + data_source)
        session.upload_zip(database_name=data_source, data_to_upload=data_source)

    # Networks upload data
    print('-----------------------')

    print('Updating data for networks')

    for network in NETWORKS:
        # sign in to target server
        print('Sign in to target server: ' + TARGET_SERVER_URL)
        session = Session(
            url=TARGET_SERVER_URL,
            email=TARGET_SERVER_USERNAME,
            password=TARGET_SERVER_PASSWORD
        )
        print('Upload transformed data for: ' + network)
        session.upload_zip(database_name=network, data_to_upload=network)
