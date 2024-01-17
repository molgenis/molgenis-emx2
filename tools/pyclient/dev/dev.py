# ///////////////////////////////////////////////////////////////////////////////
# FILE: dev.py
# AUTHOR: David Ruvolo, Ype Zijlstra
# CREATED: 2023-05-22
# MODIFIED: 2023-11-28
# PURPOSE: development script for initial testing of the py-client
# STATUS: ongoing
# PACKAGES: pandas, python-dotenv
# COMMENTS: Designed to interact with the schema "pet store".
#           Create a file called '.env' that states the molgenis token, to get this token login into the server (UI) as admin.
#           Next click on 'Hi admin' and under Manage token give the new token a name and create. Copy this token into .env as described below.
#           MG_TOKEN = ....
# ///////////////////////////////////////////////////////////////////////////////
import asyncio
import logging
import os

import pandas as pd
from dotenv import load_dotenv

from tools.pyclient.src.molgenis_emx2_pyclient import Client
from tools.pyclient.src.molgenis_emx2_pyclient.exceptions import (NoSuchSchemaException, NoSuchTableException,
                                                                  GraphQLException, PermissionDeniedException)


async def main():
    # Set up the logger
    logging.basicConfig(level='INFO')
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)

    # Load the login details into the environment
    load_dotenv()
    token = os.environ.get('MG_TOKEN')
    # Connect to the server and sign in
    async with Client('https://emx2.dev.molgenis.org/', token=token) as client:
        # Check sign in status
        print(client.status)

        # Retrieve data from a table in a schema on the server using the 'get' method
        # Passing non-existing schema name yields a NoSuchSchemaException
        try:
            data = client.get(schema='', table='')  # run without specifying target
            print(data)
        except NoSuchSchemaException as e:
            print(e)

        # Passing table name not on the schema yields a NoSuchTableException
        try:
            data = client.get(schema='pet store', table='')  # run without specifying table
            print(data)
        except NoSuchTableException as e:
            print(e)

        # Export the entire 'pet store' schema to a .xlsx file
        # and export the 'Cohorts' table from schema 'catalogue' to a .csv file
        client.export(schema='pet store', fmt='xlsx')
        client.export(schema='catalogue-demo', table='Cohorts', fmt='csv')

    # Connect to server with a default schema specified
    with Client('https://emx2.dev.molgenis.org/', schema='pet store', token=token) as client:
        client.export(fmt='csv')
        client.export(table='Pet', fmt='csv')
        client.export(table='Pet', fmt='xlsx')

        # Retrieving data from table Pet as a list
        data = client.get(table='Pet')  # get Pets
        print(data)

        # Retrieving data from table Pet as a pandas DataFrame
        data = client.get(table='Pet', as_df=True)  # get Pets
        print(data)

        # ///////////////////////////////////////////////////////////////////////////////

        # ~ 1 ~
        # Check Import Methods

        # ~ 1a ~
        # Check import via the `data` parameters
        # Add new record to the pet store with new tags

        new_tags = [
            {'name': 'brown', 'parent': 'colors'},
            {'name': 'canis', 'parent': 'species'},
        ]

        new_pets = [{
            'name': 'Woofie',
            'category': 'dog',
            'status': 'available',
            'weight': 6.8,
            'tags': 'brown,canis'
        }]

        # Import new data
        try:
            client.save_schema(name='pet store', table='Tag', data=new_tags)
            client.save_schema(name='pet store', table='Pet', data=new_pets)

            # Retrieve records
            tags_data = client.get(schema='pet store', table='Tag', as_df=True)
            print(tags_data)
            pets_data = client.get(schema='pet store', table='Pet', as_df=True)
            print(pets_data)

            # Drop records
            tags_to_remove = [{'name': row['name']} for row in new_tags if row['name'] == 'canis']
            client.delete_records(schema='pet store', table='Pet', data=new_pets)
            client.delete_records(schema='pet store', table='Tag', data=tags_to_remove)
        except PermissionDeniedException:
            print(f"Permission denied for importing or deleting data. "
                  f"Ensure correct authorization.")

        # ///////////////////////////////////////

        # ~ 1b ~
        # Check import via the `file` parameter
        try:
            # Save datasets
            pd.DataFrame(new_tags).to_csv('demodata/Tag.csv', index=False)
            pd.DataFrame(new_pets).to_csv('demodata/Pet.csv', index=False)

            # Import files
            client.save_schema(name='pet store', table='Tag', file='demodata/Tag.csv')
            client.save_schema(name='pet store', table='Pet', file='demodata/Pet.csv')

            client.delete_records(schema='pet store', table='Pet', file='demodata/Pet.csv')
            client.delete_records(schema='pet store', table='Tag', file='demodata/Tag.csv')
        except PermissionDeniedException:
            print(f"Permission denied for importing or deleting data. "
                  f"Ensure correct authorization.")

    # Connect to server and create, update, and drop schemas
    with Client('https://emx2.dev.molgenis.org/', token=token) as client:
        # Create a schema
        try:
            client.create_schema(name='myNewSchema')
            print(client.schema_names)
        except (GraphQLException, PermissionDeniedException) as e:
            print(e)

        # Update the description
        try:
            client.update_schema(name='myNewSchema', description='I forgot the description')
            print(client.schema_names)
            print(client.schemas)
        except GraphQLException as e:
            print(e)

        # Recreate the schema: delete and create
        try:
            client.recreate_schema(name='myNewSchema')
            print(client.schema_names)
        except GraphQLException as e:
            print(e)

        # Delete the schema
        try:
            client.delete_schema(name='myNewSchema')
            print(client.schema_names)
        except GraphQLException as e:
            print(e)


if __name__ == '__main__':
    asyncio.run(main())
