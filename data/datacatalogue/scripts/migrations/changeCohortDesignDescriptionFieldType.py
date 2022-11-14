from io import BytesIO, StringIO
import sys
import os
import logging
from tabnanny import verbose
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.append(os.path.dirname(SCRIPT_DIR))
import client
from decouple import config
from decouple import AutoConfig
import pandas as pd

logging.basicConfig(level=os.environ.get("LOGLEVEL", "INFO"))
log = logging.getLogger('migration')

def main():
    log.info('start migration; change cohort designDescription field type from STRING to TEXT')

    log.info('read environment variables')
    config = AutoConfig(search_path=os.path.dirname(SCRIPT_DIR))
    log.info(config.search_path)
    try:
      MIGRATION_URL = config('MG_MIGRATION_URL')
      MIGRATION_USERNAME = config('MG_MIGRATION_USERNAME')
      MIGRATION_PASSWORD = config('MG_MIGRATION_PASSWORD')
      MIGRATION_DATABASE = config('MG_MIGRATION_DATABASE')
    except:
        log.error('Make sure you filled in all variables in the .env file, script will exit now.')
        sys.exit()

    # set up Client for migration requests
    mClient = client.Client(
        url=MIGRATION_URL,
        database=MIGRATION_DATABASE,
        email=MIGRATION_USERNAME,
        password=MIGRATION_PASSWORD
    )

    # updateColType = "mutation{change(columns: {table: \"Cohorts\", name: \"designDescription\", columnType: \"TEXT\"}){message}}"
    # updateDescription = "mutation change($tables: [MolgenisTableInput]){change(tables: $tables){message}}"
    # variables = {'tables': [  {'name': 'Version', 'tableType': 'DATA', 'description': '2.7'}]}
    # toggleOn = "mutation change($settings:[MolgenisSettingsInput]){ change(settings:$settings){ message }}"
    # variables = { 'settings': { 'key': 'isChangelogEnabled', 'value' : 'true' } }

    addSetting = "mutation change($settings:[MolgenisSettingsInput]){ change(settings:$settings){ message }}"
    isChaptersEnabled = { 'settings': { 'key': 'isChaptersEnabled', 'value' : 'false' } }

    
    schemas = mClient.list_schemas()

    stream = read_csv_into_stream('/models/staging-2.8.csv')
    # log.info(stream)

    # run migration for each schema
    for schema in schemas:
      version = get_catalogue_model_version(mClient, schema)
      isStaging = is_umcg_staging(mClient, schema)
      # if isStaging == True:
      log.info('version: ' + schema + ' ' + str(version) + ' ' + str(isStaging))
      # mClient.uploadCSV(None, stream.getvalue().encode('utf-8'), schema)
      #mClient.post_gql_to_db(schema, toggleOn, variables)
      # mClient.post_gql_to_db(schema, updateDescription, variables, '/schema/graphql')
      mClient.post_gql_to_db(schema, addSetting, isChaptersEnabled)
      log.info('run for: ' + schema)
      

    log.info('migration complete')


def get_catalogue_model_version(client: client.Client, databaseName):
  query = "query { _schema {tables {name, description}} }"

  resp = client.query(query, {}, databaseName)

  version = -1

  if resp and resp["_schema"] and "tables" in resp["_schema"]:
    for table in resp["_schema"]["tables"]:
      if table["name"] == "Version":
        version = table["description"]
  
  return version

def is_umcg_staging(client: client.Client, databaseName):
  query = "query { _schema {tables {name, description}} }"

  resp = client.query(query, {}, databaseName)

  # Wanneer is een schema een umcg staging schema ..
  # Als het geen ‘AllSourceVariables’ tabla heeft en geen ‘Cohorts’ tabel

  hasAllSourceVariables = False
  hasCohorts = False

  if resp and resp["_schema"] and "tables" in resp["_schema"]:
    for table in resp["_schema"]["tables"]:
      if table["name"] == "AllSourceVariables":
        hasAllSourceVariables = True
      if table["name"] == "Cohorts":
        hasCohorts = True
  
  return not hasAllSourceVariables and hasCohorts

def read_csv_into_stream(filePath):
    df = pd.read_csv(os.path.dirname(
        SCRIPT_DIR) + filePath, dtype='str', na_filter=False)
    stream = StringIO()
    df.to_csv(stream, index=False)
    return stream
   


if __name__ == ("__main__"):
  main()