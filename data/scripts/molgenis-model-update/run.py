from posixpath import join
from decouple import config
from session import Session
from update import TransformData

# Data model details
DATA_MODEL_VERSION = config('MG_DATA_MODEL_VERSION')

# Server details
SERVER_URL = config('MG_SERVER_URL')
SERVER_USERNAME = config('MG_SERVER_USERNAME')
SERVER_PASSWORD = config('MG_SERVER_PASSWORD')
SCHEMA_NAME = config('MG_SCHEMA_NAME')

# COHORTS = config('MG_COHORTS', cast=lambda v: [s.strip() for s in v.split(',')])

print('-----  Config variables loaded ----')

print('SERVER_URL: ' + SERVER_URL)
print('SERVER_USERNAME: ' + SERVER_USERNAME)
print('SERVER_PASSWORD: ******')
print('SCHEMA_NAME: ' + SCHEMA_NAME)

print('-----   ----')

print('Updating catalogue data model to version' + DATA_MODEL_VERSION)

# sign in to server
print('Sign in to server.')
session = Session(
    url=SERVER_URL,
    database=SCHEMA_NAME,
    email=SERVER_USERNAME,
    password=SERVER_PASSWORD
)
# extract data from catalogue
print('Extract data from ' + SCHEMA_NAME + ': ' + SCHEMA_NAME + '_data.zip')
session.download_zip()

# transform data
print('Transform data from ' + SCHEMA_NAME)
transform_data = TransformData(SCHEMA_NAME)

# # upload data
# print('Load data' + SCHEMA_NAME + '(upload.zip)')
# session.upload_zip()

# # Cohorts update
# print()
# print('Cohorts ETL')
# for item in COHORTS:
#     # sign in to staging server
#     print('Sign in to staging server for database: %s.' % (item))
#     session_staging = Session(
#         url=URL_STAGING,
#         database=item,
#         email=USERNAME_STAGING,
#         password=PASSWORD_STAGING
#     )
#     # extract data
#     print('Extract data (data.zip)')
#     session_staging.download_zip()
#     # transform data
