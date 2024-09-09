"""
Script for developing the method that uploads a file to a server.

The script creates a new staging area, titled 'Upload Test' with the Pet store template.
Then the ZIP file containing 'pet store' demo data is uploaded.
Next, the new data from the tables on the schema are retrieved.
Finally, the schema is deleted from the EMX2 dev server.
"""
import logging
import os
from pathlib import Path

from dotenv import load_dotenv

from tools.pyclient.src.molgenis_emx2_pyclient import Client
from tools.pyclient.src.molgenis_emx2_pyclient.exceptions import (NoSuchSchemaException, GraphQLException,
                                                                  PermissionDeniedException, PyclientException)


def upload_zip(file_name: str):
    token = os.environ.get('MG_TOKEN')

    # Connect to server and create, update, and drop schemas
    with Client('https://emx2.dev.molgenis.org/', token=token) as client:

        # Download catalogue.zip
        client.export(schema='catalogue', fmt='csv')

        # Create a schema
        try:
            client.create_schema(name='Upload Test')
            print(client.schema_names)
        except (GraphQLException, PermissionDeniedException, PyclientException) as e:
            print(e)

        client.set_schema('Upload Test')

        file_path = Path(file_name).absolute()

        client.upload_file(file_path)

        try:
            print(client.get(table='Cohorts', schema='Upload Test', as_df=True).to_string())
        except PyclientException as e:
            print(e)

        # Delete the schema
        try:
            client.delete_schema(name='Upload Test')
            print(client.schema_names)
        except (GraphQLException, NoSuchSchemaException) as e:
            print(e)

        os.remove('catalogue.zip')


def upload_csv(file_name: str):
    token = os.environ.get('MG_TOKEN')

    # Connect to server and create, update, and drop schemas
    with Client('https://emx2.dev.molgenis.org/', token=token) as client:

        # Download catalogue.zip
        client.export(schema='catalogue', fmt='csv')

        # Download schema csv
        response = client.session.get(f"{client.url}/catalogue/api/csv")
        with open("molgenis.csv", "wb") as f:
            f.write(response.content)

        # Download catalogue Organisations
        client.export(schema='catalogue', table='Organisations', fmt='csv')

        # Create a schema
        try:
            client.create_schema(name='Upload Test')
            print(client.schema_names)
        except (GraphQLException, PermissionDeniedException, PyclientException) as e:
            print(e)

        client.set_schema('Upload Test')

        # Upload molgenis.csv
        file_path = Path("molgenis.csv").absolute()
        client.upload_file(file_path)

        # Upload data table
        file_path = Path("Organisations.csv").absolute()
        client.upload_file(file_path)

        try:
            print(client.get(table='Organisations', schema='Upload Test', as_df=True).to_string())
        except PyclientException as e:
            print(e)

        # Delete the schema
        try:
            client.delete_schema(name='Upload Test')
            print(client.schema_names)
        except (GraphQLException, NoSuchSchemaException) as e:
            print(e)

        os.remove('molgenis.csv')
        os.remove('Organisations.csv')


def main():
    """Function that launches the functions that test the Pyclient."""
    # Set up the logger
    logging.basicConfig(level='INFO')
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)

    # Load the login details into the environment
    load_dotenv()

    # Launch the functions
    # upload_zip(file_name='catalogue.zip')

    upload_csv("Organisations.csv")


if __name__ == '__main__':
    main()
