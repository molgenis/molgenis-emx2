# target server should be empty and updated to latest emx2 version

from decouple import config
from molgenis_emx2_pyclient import Client
from molgenis_emx2_pyclient.exceptions import NoSuchSchemaException
from molgenis_emx2_pyclient.metadata import Schema
from catalogue_util.zip_handling import Zip
from update.update_7_x import Transform
import os
import asyncio
from pathlib import Path


# Data model details
DATA_MODEL_VERSION = config('MG_DATA_MODEL_VERSION')

# Source server details
SOURCE_SERVER_URL = config('MG_SOURCE_SERVER_URL')
SOURCE_SERVER_TOKEN = config('MG_SOURCE_SERVER_TOKEN')

# Target server details
TARGET_SERVER_URL = config('MG_TARGET_SERVER_URL')
TARGET_SERVER_TOKEN = config('MG_TARGET_SERVER_TOKEN')

CATALOGUE_SCHEMA_NAME = config('MG_CATALOGUE_SCHEMA_NAME')

print('-----  Config variables loaded ----')

print('SOURCE_SERVER_URL: ' + SOURCE_SERVER_URL)
print('SOURCE_SERVER_TOKEN: *****')

print('TARGET_SERVER_URL: ' + TARGET_SERVER_URL)
print('TARGET_SERVER_TOKEN: *****')

print('CATALOGUE_SCHEMA_NAME: ' + str(CATALOGUE_SCHEMA_NAME))

print('-------------------')

if not os.path.isdir('./files'):
    os.mkdir('./files')

os.chdir('./files')

# instantiate Client for source server:
source = Client(SOURCE_SERVER_URL, schema=CATALOGUE_SCHEMA_NAME, token=SOURCE_SERVER_TOKEN)

# ETL for catalogue schema data:
print('Extract data from ' + CATALOGUE_SCHEMA_NAME + ': ' + CATALOGUE_SCHEMA_NAME + '_data.zip')
asyncio.run(source.export(filename=CATALOGUE_SCHEMA_NAME + '_data.zip'))
# transform data from schema
print('Transform data from ' + CATALOGUE_SCHEMA_NAME)
# unzip schema data
zip_handling = Zip(CATALOGUE_SCHEMA_NAME)
zip_handling.unzip_data()
# transform schema data:
update = Transform(schema_name=CATALOGUE_SCHEMA_NAME, profile='DataCatalogueFlat')
# update data model file
update.delete_data_model_file()
update.update_data_model_file()
update.transform_data()
zip_handling.zip_data()

# instantiate Client for target server:
# target = Client(TARGET_SERVER_URL, schema=CATALOGUE_SCHEMA_NAME, token=TARGET_SERVER_TOKEN)

# upload catalogue data to target server

# ETL for other schemas:
schema_names = source.schema_names
for schema_name in schema_names:
    if schema_name not in ['CatalogueOntologies', CATALOGUE_SCHEMA_NAME, '_SYSTEM_', 'Pet store']:
        # instantiate Client for source schema:
        source = Client(SOURCE_SERVER_URL, schema=schema_name, token=SOURCE_SERVER_TOKEN)

        # extract data
        print('Extract data from ' + schema_name + ': ' + schema_name + '_data.zip')
        asyncio.run(source.export(filename=schema_name + '_data.zip'))

        # transform data from schema
        print('Transform data from ' + schema_name)
        # unzip schema data
        zip_handling = Zip(schema_name)
        zip_handling.unzip_data()

        # determine schema profile:
        path_to_data = Path().cwd().joinpath(schema_name + '_data')
        tables = os.listdir(path_to_data)
        if 'Linkages.csv' in tables:
            profile = 'RWEStaging'
        elif all(x in tables for x in ['Collection events.csv', 'Variables.csv', 'Internal identifiers.csv']):
            profile = 'CohortsStaging'
        # else:
        #     profile = 'INTEGRATE'
        # else:
        #     profile = 'UMCUCohorts'
        else:
            profile = 'UMCGCohorts'
        print(schema_name, profile)

        # transform schema data:
        update = Transform(schema_name=schema_name, profile=profile)
        # update data model file
        update.delete_data_model_file()
        update.update_data_model_file()
        update.transform_data()
        zip_handling.zip_data()
        # # instantiate Client for target server:
        # target = Client(TARGET_SERVER_URL, schema=schema_name, token=TARGET_SERVER_TOKEN)
        # upload zipped data to target server


# data checks:
# move some sub-organisations to Organisations.department
