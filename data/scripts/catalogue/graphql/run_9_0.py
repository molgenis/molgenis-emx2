"""
Attempt to perform the migration through GraphQL API.
In this example the table Pet is renamed to 'Pet1' in a 'pet store' schema.
"""
import asyncio
import json

from decouple import config
from molgenis_emx2_pyclient import Client

from scripts.catalogue.graphql.constants import query

SERVER_URL = config("MG_URL")
TOKEN = config("MG_TOKEN")

PETSTORE2 = "pet store 2"
CATALOGUE2 = "catalogue 2"

async def run():
    with Client(url=SERVER_URL, token=TOKEN) as client:
        if PETSTORE2 in client.schema_names:
            print(f"Deleting schema {PETSTORE2}")
            await client.delete_schema(PETSTORE2)
        print(f"Creating schema {PETSTORE2}")
        await client.create_schema(PETSTORE2)

        update_schema(client, PETSTORE2)

def update_schema(client: Client, schema_name: str):
    url = f"{client.url}/{schema_name}/graphql"
    with open("pet store/variables.json", 'r') as f:
        variables = json.load(f)
    print("Updating schema metadata")
    result = client.session.post(url=url, json={
        "operationName": "change",
        "query": query,
        "variables": variables
    })
    if result.status_code != 200:
        print("ERROR: something went wrong.")
        print(result.content)
    else:
        print(result.json().get("data").get("change").get("message"))

    schema_tables = client.get_schema_metadata(name=PETSTORE2).tables
    print(f"Tables in {PETSTORE2}:")
    print(', '.join(map(lambda t: t.name, schema_tables)))



if __name__ == '__main__':
    asyncio.run(run())
