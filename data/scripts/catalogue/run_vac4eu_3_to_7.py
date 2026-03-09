# Target server requirements:
# empty and updated to latest emx2 version
# 'conception' and 'vac4eu' schemas should be made with DATA_CATALOGUE template, without demo data
from decouple import config
from molgenis_emx2_pyclient import Client
from catalogue_util.zip_handling import Zip
from update.update_7_3 import Transform
import os
import asyncio


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
update = Transform(schema_name=CATALOGUE_SCHEMA_NAME)
# update data model file
update.delete_data_model_file()
update.update_data_model_file()
update.transform_data()
zip_handling.zip_data()

# instantiate Client for target server:
target = Client(TARGET_SERVER_URL, schema=CATALOGUE_SCHEMA_NAME, token=TARGET_SERVER_TOKEN)
# upload catalogue data to target server
asyncio.run(target.upload_file(file_path=CATALOGUE_SCHEMA_NAME + '_upload.zip', schema=CATALOGUE_SCHEMA_NAME))
