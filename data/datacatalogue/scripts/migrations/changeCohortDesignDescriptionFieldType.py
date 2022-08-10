import sys
import os
import logging
from tabnanny import verbose
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.append(os.path.dirname(SCRIPT_DIR))
import client
from decouple import config
from decouple import AutoConfig

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

    gql = "mutation{change(columns: {table: \"Cohorts\", name: \"designDescription\", columnType: \"TEXT\"}){message}}"
    
    schemas = mClient.list_schemas()

    # run migration for each schema
    for schema in schemas:
      # mClient.post_gql_to_db(schema, gql)
      version = get_catalogue_model_version(mClient, schema)
      log.info(version)

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


if __name__ == ("__main__"):
  main()