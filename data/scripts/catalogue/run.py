import asyncio
import logging
import os
from pathlib import Path
from zipfile import ZipFile

from decouple import config
from molgenis_emx2_pyclient import Client

from catalogue_util.client import Session
from catalogue_util.zip_handling import Zip
from update.update_4_x import Transform

FILES_DIR = Path(__file__).parent.joinpath('files').resolve()

def old_run():
    if not os.path.isdir('./files'):
        os.mkdir('./files')

    os.chdir('./files')

    # Data model details
    DATA_MODEL_VERSION = config('MG_DATA_MODEL_VERSION')

    # Server details
    SERVER_URL = config('MG_SERVER_URL')
    SERVER_USERNAME = config('MG_SERVER_USERNAME')
    SERVER_PASSWORD = config('MG_SERVER_PASSWORD')
    SERVER_TYPE = config('MG_SERVER_TYPE')

    CATALOGUE_SCHEMA_NAME = config('MG_CATALOGUE_SCHEMA_NAME')
    ONTOLOGIES_SCHEMA_NAME = config('MG_ONTOLOGIES_SCHEMA_NAME')
    SHARED_STAGING_NAME = config('MG_SHARED_STAGING_NAME')

    if SERVER_TYPE == 'data_catalogue' or SERVER_TYPE == 'cohort_catalogue':
        COHORTS = config('MG_COHORTS', cast=lambda v: [s.strip() for s in v.split(',')])
        print(COHORTS)

    if SERVER_TYPE == 'data_catalogue':
        DATA_SOURCES = config('MG_DATA_SOURCES', cast=lambda v: [s.strip() for s in v.split(',')])
        NETWORKS = config('MG_NETWORKS', cast=lambda v: [s.strip() for s in v.split(',')])

    print('-----  Config variables loaded ----')

    print('SERVER_URL: ' + SERVER_URL)
    print('SERVER_USERNAME: ' + SERVER_USERNAME)
    print('SERVER_PASSWORD: ******')
    print('SERVER_TYPE: ' + SERVER_TYPE)
    print('CATALOGUE_SCHEMA_NAME: ' + CATALOGUE_SCHEMA_NAME)
    print('ONTOLOGIES_SCHEMA_NAME: ' + ONTOLOGIES_SCHEMA_NAME)
    print('SHARED_STAGING_NAME: ' + SHARED_STAGING_NAME)

    print('-----   ----')

    print('Updating catalogue data model to version ' + DATA_MODEL_VERSION)
    # TODO: rewrite to use py client

    # sign in to server
    print('Sign in to server: ' + SERVER_URL)
    session = Session(
        url=SERVER_URL,
        email=SERVER_USERNAME,
        password=SERVER_PASSWORD
    )

    # --------------------------------------------------------------
    # Catalogue schema update
    print('-----------------------')
    print('Catalogue schema update to data model ' + DATA_MODEL_VERSION)

    # extract data from catalogue schema
    print('Extract data from ' + CATALOGUE_SCHEMA_NAME + ': ' + CATALOGUE_SCHEMA_NAME + '_data.zip')
    session.download_zip(database_name=CATALOGUE_SCHEMA_NAME)

    # transform data from catalogue schema
    print('Transform data from ' + CATALOGUE_SCHEMA_NAME)
    # get instances of classes
    zip_handling = Zip(CATALOGUE_SCHEMA_NAME)
    update = Transform(database_name=CATALOGUE_SCHEMA_NAME, database_type='catalogue')

    # run zip and transform functions
    zip_handling.unzip_data()
    update.delete_data_model_file()  # delete molgenis.csv from data folder
    update.update_data_model_file()
    update.transform_data()
    zip_handling.zip_data()

    # --------------------------------------------------------------
    if SERVER_TYPE in ['data_catalogue', 'cohort_catalogue']:
        # Cohorts update
        print('-----------------------')
        print('Cohort staging schema update to data model ' + DATA_MODEL_VERSION)

        for cohort in COHORTS:
            print(cohort)
            # sign in to server
            print('Sign in to server: ' + SERVER_URL)
            session = Session(
                url=SERVER_URL,
                email=SERVER_USERNAME,
                password=SERVER_PASSWORD
            )
            # extract data
            print('Extract data for ' + cohort + ': ' + cohort + '_data.zip')
            session.download_zip(database_name=cohort)

            # transform data from cohorts
            print('Transform data from ' + cohort)
            zip_handling = Zip(cohort)
            if SERVER_TYPE == 'data_catalogue':
                update = Transform(cohort, 'cohort')
            elif SERVER_TYPE == 'cohort_catalogue':
                update = Transform(cohort, 'cohort_UMCG')

            zip_handling.remove_unzipped_data()
            zip_handling.unzip_data()
            update.delete_data_model_file()
            update.transform_data()
            update.update_data_model_file()
            zip_handling.zip_data()
            zip_handling.remove_unzipped_data()

            # # delete and create new cohort schema
            # schema_description = session.get_database_description(database_name=cohort)
            # session.drop_database(database_name=cohort)
            # session.create_database(database_name=cohort, database_description=schema_description)

    # --------------------------------------------------------------
    if SERVER_TYPE == 'data_catalogue':
        # Data sources update
        print('-----------------------')
        print('Data source update to data model ' + DATA_MODEL_VERSION)

        for data_source in DATA_SOURCES:
            # sign in to server
            print('Sign in to server: ' + SERVER_URL)
            session = Session(
                url=SERVER_URL,
                email=SERVER_USERNAME,
                password=SERVER_PASSWORD
            )
            # extract data
            print('Extract data for ' + data_source + ': ' + data_source + '_data.zip')
            session.download_zip(database_name=data_source)

            # transform data from data sources
            print('Transform data from ' + data_source)
            zip_handling = Zip(data_source)
            update = Transform(data_source, 'data_source')

            zip_handling.remove_unzipped_data()
            zip_handling.unzip_data()
            update.delete_data_model_file()
            update.transform_data()
            update.update_data_model_file()
            zip_handling.zip_data()
            zip_handling.remove_unzipped_data()

            # # delete and create new data source schema
            # schema_description = session.get_database_description(database_name=data_source)
            # session.drop_database(database_name=data_source)
            # session.create_database(database_name=data_source, database_description=schema_description)

        # Networks update
        print('-----------------------')
        print('Networks update to data model ' + DATA_MODEL_VERSION)

        for network in NETWORKS:
            # sign in to server
            print('Sign in to server: ' + SERVER_URL)
            session = Session(
                url=SERVER_URL,
                email=SERVER_USERNAME,
                password=SERVER_PASSWORD
            )
            # extract data
            print('Extract data for ' + network + ': ' + network + '_data.zip')
            session.download_zip(database_name=network)

            # transform data
            print('Transform data from ' + network)
            zip_handling = Zip(network)
            update = Transform(network, 'network')

            zip_handling.remove_unzipped_data()
            zip_handling.unzip_data()
            update.delete_data_model_file()
            update.transform_data()
            update.update_data_model_file()
            zip_handling.zip_data()
            zip_handling.remove_unzipped_data()

            # # delete and create new schema
            # schema_description = session.get_database_description(database_name=network)
            # session.drop_database(database_name=network)
            # session.create_database(database_name=network, database_description=schema_description)

    # ---------------------------------------------------------------

    # # delete and create schemas
    # print('------------------------')
    # print('Updating catalogue schema')
    # # delete and create new catalogue schema
    # schema_description = session.get_database_description(database_name=CATALOGUE_SCHEMA_NAME)
    # session.drop_database(database_name=CATALOGUE_SCHEMA_NAME)
    # session.create_database(database_name=CATALOGUE_SCHEMA_NAME, database_description=schema_description)
    #
    # # upload molgenis.csv to catalogue schema
    # update_general = Transform(CATALOGUE_SCHEMA_NAME, 'catalogue')
    # data_model_file = update_general.update_data_model_file()
    # session.upload_zip(database_name=CATALOGUE_SCHEMA_NAME, data_to_upload='catalogue_data_model')
    #
    # # upload transformed catalogue data to catalogue schema
    # session.upload_zip(database_name=CATALOGUE_SCHEMA_NAME, data_to_upload=CATALOGUE_SCHEMA_NAME)
    #
    # # ----------------------------------------------------------------------

    # # Cohorts upload data
    # print('-----------------------')
    #
    # if SERVER_TYPE in ['data_catalogue', 'cohort_catalogue']:
    #     print('Updating data for cohorts')
    #     for cohort in COHORTS:
    #         # sign in to server
    #         print('Sign in to server: ' + SERVER_URL)
    #         session = Session(
    #             url=SERVER_URL,
    #             email=SERVER_USERNAME,
    #             password=SERVER_PASSWORD
    #         )
    #         print('Upload transformed data for: ' + cohort)
    #         session.upload_zip(database_name=cohort, data_to_upload=cohort)
    #
    # if SERVER_TYPE == 'data_catalogue':
    #     # Data sources upload data
    #     print('-----------------------')
    #
    #     print('Updating data for data sources')
    #
    #     for data_source in DATA_SOURCES:
    #         # sign in to server
    #         print('Sign in to server: ' + SERVER_URL)
    #         session = Session(
    #             url=SERVER_URL,
    #             email=SERVER_USERNAME,
    #             password=SERVER_PASSWORD
    #         )
    #         print('Upload transformed data for: ' + data_source)
    #         session.upload_zip(database_name=data_source, data_to_upload=data_source)
    #
    #     # Networks upload data
    #     print('-----------------------')
    #
    #     print('Updating data for networks')
    #
    #     for network in NETWORKS:
    #         # sign in to server
    #         print('Sign in to server: ' + SERVER_URL)
    #         session = Session(
    #             url=SERVER_URL,
    #             email=SERVER_USERNAME,
    #             password=SERVER_PASSWORD
    #         )
    #         print('Upload transformed data for: ' + network)
    #         session.upload_zip(database_name=network, data_to_upload=network)


class Runner(Client):
    """
    Class that handles the running of the update.
    Inherits methods from the Pyclient class.
    """

    def __init__(self, pattern = None):
        """Initializes the object by loading information from the .env file."""

        # Initialize the client with URL and token
        server = config('MG_SERVER_URL')
        token = config('MG_SERVER_TOKEN')
        super().__init__(url=server, token=token)

        # Set additional attributes
        self.server_type = config('MG_SERVER_TYPE')
        self.catalogue = config('MG_CATALOGUE_SCHEMA_NAME')

        # Set resource type names
        self.cohorts, self.data_sources, self.networks = self._prepare_resource_names(self.server_type)

        # Set the pattern
        if pattern:
            self.pattern = pattern
        else:
            self.pattern = '_'

    @staticmethod
    def _prepare_resource_names(server_type):
        match server_type:
            case 'data_catalogue':
                cohorts = config('MG_COHORTS', cast=lambda v: [s.strip() for s in v.split(',')])
                data_sources = config('MG_DATA_SOURCES', cast=lambda v: [s.strip() for s in v.split(',')])
                networks = config('MG_NETWORKS', cast=lambda v: [s.strip() for s in v.split(',')])
            case 'cohort_catalogue':
                cohorts = config('MG_COHORTS', cast=lambda v: [s.strip() for s in v.split(',')])
                data_sources = None
                networks = None
            case _:
                cohorts = None
                data_sources = None
                networks = None
        return cohorts, data_sources, networks

    async def update_catalogue(self):
        """
        Updates the data model and data in the catalogue schema.
        """
        logging.info(f"Starting update on {self.catalogue!r}")

        if f"{self.catalogue}{self.pattern}" in self.schema_names:
            create_schema = asyncio.create_task(self.recreate_schema(name=f"{self.catalogue}{self.pattern}",
                                                                     description="Catalogue update"))
        else:
            create_schema = asyncio.create_task(self.create_schema(name=f"{self.catalogue}{self.pattern}",
                                                                   description="Catalogue update"))

        # Export catalogue data to zip
        await self.export(schema=self.catalogue, fmt='csv')
        if not FILES_DIR.exists():
            FILES_DIR.mkdir()
        os.rename(f"{self.catalogue}.zip", FILES_DIR.joinpath(f"{self.catalogue}_data.zip"))

        logging.info(f"Transforming data from schema {self.catalogue}")
        catalogue_transform = Transform(database_name=self.catalogue, database_type='catalogue')

        # Extract the zip file
        with ZipFile(FILES_DIR.joinpath(f"{self.catalogue}_data.zip"), 'r') as zf:
            zf.extractall(path=FILES_DIR.joinpath(f"{self.catalogue}_data"))

        # Replace the data model file
        catalogue_transform.delete_data_model_file()
        catalogue_transform.update_data_model_file()

        # Transform the data
        catalogue_transform.transform_data()

        # Compress the files
        with ZipFile(FILES_DIR.joinpath(f"{self.catalogue}_upload.zip"), 'w') as zf:
            for file_path in FILES_DIR.joinpath(f"{self.catalogue}_data").iterdir():
                zf.write(file_path, arcname=file_path.name)
            for file_path in FILES_DIR.joinpath(f"{self.catalogue}_data", '_files').iterdir():
                zf.write(file_path, arcname=f"_files/{file_path.name}")
            zf.write(FILES_DIR.joinpath(f"{self.catalogue}_data_model", 'molgenis.csv'), arcname='molgenis.csv')

        # Upload the updated data
        await create_schema
        await self.upload_file(file_path=FILES_DIR.joinpath(f"{self.catalogue}_upload.zip"),
                               schema=f"{self.catalogue}{self.pattern}")


    async def update_cohorts(self):
        """Updates the cohort schemas on a server."""
        for cohort in self.cohorts:
            logging.info(f"Updating cohort staging area {cohort!r}")
            await self._update_cohort(cohort)

    async def update_data_sources(self):
        """Updates the data sources on a schema."""
        for ds in self.data_sources:
            logging.info(f"Updating data source staging area {ds!r}")
            await self._update_data_source(ds)

    async def update_networks(self):
        """Updates the networks on a schema."""
        for network in self.networks:
            logging.info(f"Updating networks staging area {network!r}")
            await self._update_network(network)

    async def _update_cohort(self, name):
        """Updates a cohort."""

        # Get the description from the existing schema
        description = {s.name: s for s in self.get_schemas()}.get(name).get('description')

        # Create or re-create an empty schema with the name and description of the existing schema
        if f"{self.catalogue}{self.pattern}" in self.schema_names:
            create_schema = asyncio.create_task(self.recreate_schema(name=f"{name}{self.pattern}",
                                                                     description=description))
        else:
            create_schema = asyncio.create_task(self.create_schema(name=f"{name}{self.pattern}",
                                                                   description=description))

        # Export the staging area data to zip
        await self.export(schema=name, fmt='csv')
        if not FILES_DIR.exists():
            FILES_DIR.mkdir()
        os.rename(f"{name}.zip", FILES_DIR.joinpath(f"{name}_data.zip"))

        logging.info(f"Transforming data from schema {name}.")
        schema_transform = Transform(database_name=name, database_type='cohort_UMCG')

        # Extract the data
        with ZipFile(FILES_DIR.joinpath(f"{name}_data.zip"), 'r') as zf:
            zf.extractall(path=FILES_DIR.joinpath(f"{name}_data"))

        # Replace the data model file
        schema_transform.delete_data_model_file()
        schema_transform.update_data_model_file()

        # Apply the transformation of data tables
        schema_transform.transform_data()

        # Compress the data files
        with ZipFile(FILES_DIR.joinpath(f"{name}_upload.zip"), 'w') as zf:
            for file_path in FILES_DIR.joinpath(f"{name}_data").iterdir():
                zf.write(file_path, arcname=file_path.name)
            for file_path in FILES_DIR.joinpath(f"{name}_data", '_files').iterdir():
                zf.write(file_path, arcname=f"_files/{file_path.name}")
            zf.write(FILES_DIR.joinpath(f"{name}_data_model", 'molgenis.csv'), arcname='molgenis.csv')

        # Import the data into the new schema
        await create_schema
        await self.upload_file(file_path=FILES_DIR.joinpath(f"{name}_upload.zip"),
                               schema=f"{name}{self.pattern}")



    async def _update_data_source(self, name):
        """Updates a data source."""

    async def _update_network(self, name):
        """Updates a network."""


    async def run(self):
        """Executes the run of the update."""
        logging.info(f"Updating schemas on {self.url!r}")

        # Creating a catalogue schema to ensure the ontologies are loaded
        create_pseudo_catalogue = asyncio.create_task(self.create_schema(name='pseudo catalogue',
                                                                         description='Catalogue created to ensure correct configuration of ontologies',
                                                                         template='DATA_CATALOGUE'))

        # Update the catalogue
        # await self.update_catalogue()

        await self.update_cohorts()

        if self.server_type == 'data_catalogue':
            await self.update_data_sources()
            await self.update_networks()

        # Wait for the pseudo catalogue to be created, then delete it
        await create_pseudo_catalogue
        await self.delete_schema('pseudo catalogue')



async def main():
    with Runner() as runner:
        await runner.run()

    # Clean up
    FILES_DIR.rmdir()


if __name__ == '__main__':
    logging.basicConfig(level='DEBUG', format = '%(filename)s:%(lineno)s %(levelname)s:%(message)s')
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)

    asyncio.run(main())
