import os

from decouple import config
from util.client import Session
from update.update_3_10 import Transform
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
NETWORKS = config('MG_NETWORKS', cast=lambda v: [s.strip() for s in v.split(',')])
DATA_SOURCES = config('MG_DATA_SOURCES', cast=lambda v: [s.strip() for s in v.split(',')])


print('-----  Config variables loaded ----')

print('SERVER_URL: ' + SERVER_URL)
print('SERVER_USERNAME: ' + SERVER_USERNAME)
print('SERVER_PASSWORD: ******')
print('CATALOGUE_SCHEMA_NAME: ' + CATALOGUE_SCHEMA_NAME)
print('ONTOLOGIES_SCHEMA_NAME: ' + ONTOLOGIES_SCHEMA_NAME)
print('SHARED_STAGING_NAME: ' + SHARED_STAGING_NAME)

print('-----   ----')

print('Updating catalogue data model to version ' + DATA_MODEL_VERSION)

os.chdir('./files')

# ---------------------------------------------------------------

# updating schemas
print('------------------------')
print('Updating catalogue, CatalogueOntologies and SharedStaging schemas')
print('Sign in to server: ' + SERVER_URL)
session = Session(
    url=SERVER_URL,
    email=SERVER_USERNAME,
    password=SERVER_PASSWORD
    )

# create instances for catalogue
zip_handling = Zip(database=CATALOGUE_SCHEMA_NAME)
transform = Transform(database_type='catalogue')

# move molgenis.csv to catalogue folder
transform.update_data_model_file()
# zip catalogue folder
zip_handling.zip_data()
# upload molgenis.csv to catalogue schema
session.upload_zip(database_name=CATALOGUE_SCHEMA_NAME, data_to_upload='catalogue')

# ----------------------------------------------------------------------

# Networks, Data sources and Cohorts upload data models
print('-----------------------')

print('Updating data model for cohorts, networks and data sources')

# create instances for cohorts
zip_handling = Zip(database='cohort')
transform = Transform(database_type='cohort')

# move molgenis.csv to cohort folder
transform.update_data_model_file()
# zip catalogue folder
zip_handling.zip_data()

for cohort in COHORTS:
    # sign in to server
    print('Sign in to server: ' + SERVER_URL)
    session = Session(
        url=SERVER_URL,
        email=SERVER_USERNAME,
        password=SERVER_PASSWORD
    )
    print('Upload transformed data for: ' + cohort)
    session.upload_zip(database_name=cohort, data_to_upload='cohort')

# -------------------------------------------------------------------

# create instances for data sources
zip_handling = Zip(database='data_source')
transform = Transform(database_type='data_source')

# move molgenis.csv to data source folder
transform.update_data_model_file()
# zip catalogue folder
zip_handling.zip_data()

for data_source in DATA_SOURCES:
    print('Upload data for: ' + data_source)
    session.upload_zip(database_name=data_source, data_to_upload='data_source')

# -------------------------------------------------------------------

# create instances for networks
zip_handling = Zip(database='network')
transform = Transform(database_type='network')

# move molgenis.csv to data source folder
transform.update_data_model_file()
# zip catalogue folder
zip_handling.zip_data()

for network in NETWORKS:
    print('Upload data for: ' + network)
    session.upload_zip(database_name=network, data_to_upload='network')
