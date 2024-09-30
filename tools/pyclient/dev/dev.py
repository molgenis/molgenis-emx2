# ///////////////////////////////////////////////////////////////////////////////
# FILE: dev.py
# AUTHOR: David Ruvolo, Ype Zijlstra
# CREATED: 2023-05-22
# MODIFIED: 2024-09-11
# PURPOSE: development script for initial testing of the py-client
# STATUS: ongoing
# PACKAGES: pandas, python-dotenv
# COMMENTS: Designed to interact with the schema "pet store".
#           Create a file called '.env' that states the molgenis token, to get
#           this token login into the server (UI) as admin. Next, click on
#           'Hi admin' and under Manage token give the new token a name and
#           create. Copy this token into .env as described below.
#           MG_TOKEN = ....
# ///////////////////////////////////////////////////////////////////////////////
import asyncio
import logging
import os

import numpy
import openpyxl
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

    async with Client('https://emx2.dev.molgenis.org/', schema='catalogue') as client:

        participant_range = [10_000, 20_000.5]
        big_data = client.get(table='Collection subcohorts',
                              query_filter=f'`numberOfParticipants` between {participant_range}', as_df=True)
        print(big_data.head().to_string())

        excluded_countries = ["Denmark", "France"]
        collections = client.get(table='Collections',
                             query_filter=f'subcohorts.countries.name != {excluded_countries}',
                             as_df=True)
        print(collections.head().to_string())

        var_values = client.get(table='Variable values',
                                query_filter='label != No and value != 1', as_df=True)

        print(var_values.head().to_string())

    # Connect to the server and sign in
    async with Client('https://emx2.dev.molgenis.org/', token=token) as client:
        # Check sign in status
        print(client.__repr__())

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

        # Export the entire 'pet store' schema to memory in Excel format,
        # print its table names and the contents of the 'Pet' table.
        # Export the 'Collections' table from schema 'catalogue' to memory and print a sample of its contents
        pet_store_excel = await client.export(schema='pet store', as_excel=True)

        pet_store = openpyxl.load_workbook(pet_store_excel, data_only=True)
        print(pet_store.sheetnames)

        pet_sheet = pd.DataFrame((ps := pd.DataFrame(pet_store['Pet'].values)).values[1:], columns=ps.iloc[0].values)
        print(pet_sheet.to_string())

        raw_collections = await client.export(schema='catalogue', table='Collections')
        collections = pd.read_csv(raw_collections)
        print(collections.sample(5).to_string())

    # Connect to server with a default schema specified
    with Client('https://emx2.dev.molgenis.org/', schema='pet store', token=token) as client:
        print(client.__repr__())
        # Export all data from the 'pet store' schema to zipped CSVs
        await client.export(filename='pet store.zip')
        # Export the 'Pet' table from the 'pet store' schema to a CSV file
        await client.export(table='Pet', filename='Pet.csv')
        # Export the 'Pet' table from the 'pet store' schema to an Excel file
        await client.export(table='Pet', filename='Pet.xlsx')

        # Retrieving data from table Pet as a list
        data = client.get(table='Pet')  # get Pets
        print(data)

        # Retrieving data from table Pet as a pandas DataFrame
        data = client.get(table='Pet', as_df=True)  # get Pets
        print(data)

        # Retrieving filtered data from table Order as pandas DataFrame
        # Filter on 'complete == true' only
        data = client.get(table='Order', query_filter="price > 9", as_df=True)
        print(data)

        # Filter on 'complete == true' and 'status == delivered'
        data = client.get(table='Order', query_filter="complete == true and status == delivered", as_df=True)
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
        }, {
            'name': 'NA',
            'category': 'ant',
            'status': numpy.nan,
            'weight': 23.6,
            'tags': 'purple,insect'
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
        # Check import via the `data` parameters as pandas DataFrames
        # Add new record to the pet store with new tags

        new_tags = pd.DataFrame(data=[{'name': 'brown', 'parent': 'colors'},
                                      {'name': 'canis', 'parent': 'species'}])

        new_pets = pd.DataFrame(data=[{
            'name': 'Woofie',
            'category': 'dog',
            'status': 'available',
            'weight': 6.8,
            'tags': 'brown,canis'
        }, {
            'name': 'NA',
            'category': 'ant',
            'status': numpy.nan,
            'weight': 23.6,
            'tags': 'purple,insect'
        }])

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
            tags_to_remove = new_tags.loc[new_tags['name'] == 'canis']
            client.delete_records(schema='pet store', table='Pet', data=new_pets)
            client.delete_records(schema='pet store', table='Tag', data=tags_to_remove)
        except PermissionDeniedException:
            print(f"Permission denied for importing or deleting data. "
                  f"Ensure correct authorization.")

        # ///////////////////////////////////////

        # ~ 1c ~

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
            schema_create = asyncio.create_task(client.create_schema(name='myNewSchema'))
            print(client.schema_names)
        except (GraphQLException, PermissionDeniedException) as e:
            print(e)

        # Update the description
        try:
            await schema_create
            client.update_schema(name='myNewSchema', description='I forgot the description')
            print(client.schema_names)
            print(client.schemas)
        except (GraphQLException, NoSuchSchemaException) as e:
            print(e)

        # Recreate the schema: delete and create
        try:
            await client.recreate_schema(name='myNewSchema')
            print(client.schema_names)
        except (GraphQLException, NoSuchSchemaException) as e:
            print(e)

        # Delete the schema
        try:
            await schema_create
            await asyncio.create_task(client.delete_schema(name='myNewSchema'))
            print(client.schema_names)
        except (GraphQLException, NoSuchSchemaException) as e:
            print(e)

    print("\n\n")

    # Use the Schema, Table, and Column classes
    catalogue_schema = Client('https://emx2.dev.molgenis.org/').get_schema_metadata('catalogue')

    # Find the tables inheriting from the 'Collections' table
    resource_children = catalogue_schema.get_tables(by='inheritName', value='Collections')

    print("Tables in the schema inheriting from the 'Collections' table.")
    for res_chi in resource_children:
        print(f"{res_chi!s}\n{res_chi!r}")
    print("\n")

    # Find the  table
    collections_meta = catalogue_schema.get_table(by='name', value='Collections')
    print(collections_meta)

    # Find the columns in the Collections table referencing the Organisations table
    orgs_refs = collections_meta.get_columns(by='refTableName', value='Organisations')
    print(orgs_refs)

    # Find the columns in the Collections table referencing the Organisations table in a reference array
    orgs_array_refs = collections_meta.get_columns(by=['columnType', 'refTableName'],
                                                   value=['REF_ARRAY', 'Collection organisations'])
    print(orgs_array_refs)

    # Print the __str__ and __repr__ representations of these columns
    print("Columns in the Collections table referencing the Collection organisations table in an array.")
    for orgs_ref in orgs_array_refs:
        print(f"{orgs_ref!s}\n{orgs_ref!r}\n")

if __name__ == '__main__':
    asyncio.run(main())
