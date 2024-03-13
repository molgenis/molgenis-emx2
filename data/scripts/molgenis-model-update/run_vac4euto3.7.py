from decouple import config
from util.client import Session
from update.update_3_7 import Transform
from update.update_3_7 import TransformDataCatalogue
from update.update_3_7 import TransformDataStaging
from util.zip_handling import Zip

# Data model details
DATA_MODEL_VERSION = config('MG_DATA_MODEL_VERSION')

# Server details
SERVER_URL = config('MG_SERVER_URL')
SERVER_USERNAME = config('MG_SERVER_USERNAME')
SERVER_PASSWORD = config('MG_SERVER_PASSWORD')
ONTOLOGIES_SCHEMA_NAME = config('MG_ONTOLOGIES_SCHEMA_NAME')

CATALOGUES = config('MG_CATALOGUE_SCHEMA_NAME', cast=lambda v: [s.strip() for s in v.split(',')])

print('-----  Config variables loaded ----')

print('SERVER_URL: ' + SERVER_URL)
print('SERVER_USERNAME: ' + SERVER_USERNAME)
print('SERVER_PASSWORD: ******')

print('-----   ----')

print('Updating catalogue data model to version ' + DATA_MODEL_VERSION)

# sign in to server
print('Sign in to server: ' + SERVER_URL)
session = Session(
    url=SERVER_URL,
    email=SERVER_USERNAME,
    password=SERVER_PASSWORD
)

for catalogue in CATALOGUES:
    # extract data from catalogue schema
    print('Extract data from ' + catalogue + ': ' + catalogue + '_data.zip')
    session.download_zip(database_name=catalogue)

    # transform data from catalogue schema
    print('Transform data from ' + catalogue)
    # get instances of classes
    transform_data = TransformDataCatalogue(catalogue, 'catalogue')
    zip_handling = Zip(catalogue)
    update_general = Transform(catalogue, 'catalogue')

    # run zip and transform functions
    zip_handling.unzip_data()
    update_general.delete_data_model_file()  # delete molgenis.csv from data folder
    transform_data.transform_data()
    zip_handling.zip_data()

    # --------------------------------------------------------------

# extract data from CatalogueOntologies
print('Extract data from ' + ONTOLOGIES_SCHEMA_NAME + ': ' + ONTOLOGIES_SCHEMA_NAME + '_data.zip')
session.download_zip(database_name=ONTOLOGIES_SCHEMA_NAME)

# transform data from CatalogueOntologies
print('Transform data from ' + ONTOLOGIES_SCHEMA_NAME)
# call instances
zip_handling = Zip(ONTOLOGIES_SCHEMA_NAME)
#
# run zip and transform functions
zip_handling.unzip_data()
zip_handling.zip_data()
zip_handling.remove_unzipped_data()
#
# ---------------------------------------------------------------


# delete and create schemas for catalogues
print('------------------------')
print('Updating catalogue and CatalogueOntologies schemas')

# delete and create new catalogue schemas
for catalogue in CATALOGUES:
    schema_description = session.get_database_description(database_name=catalogue)
    session.drop_database(database_name=catalogue)
    session.create_database(database_name=catalogue, database_description=schema_description)

# delete and create new CatalogueOntologies schema
schema_description = session.get_database_description(database_name=ONTOLOGIES_SCHEMA_NAME)
session.drop_database(database_name=ONTOLOGIES_SCHEMA_NAME)
session.create_database(database_name=ONTOLOGIES_SCHEMA_NAME, database_description=schema_description)

for catalogue in CATALOGUES:
    # upload molgenis.csv to catalogue schema
    update_general = Transform(catalogue, 'catalogue')
    data_model_file = update_general.update_data_model_file()
    session.upload_zip(database_name=catalogue, data_to_upload='catalogue_data_model')

# upload transformed CatalogueOntologies data to CatalogueOntologies schema
session.upload_zip(database_name=ONTOLOGIES_SCHEMA_NAME, data_to_upload=ONTOLOGIES_SCHEMA_NAME)

# upload transformed catalogue data to catalogue schemas
for catalogue in CATALOGUES:
    session.upload_zip(database_name=catalogue, data_to_upload=catalogue)
