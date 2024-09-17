import asyncio
import logging
import os
from pathlib import Path
from zipfile import ZipFile

from decouple import config

from molgenis_emx2_pyclient import Client
from update.update_4_x import Transform

FILES_DIR = Path(__file__).parent.joinpath('files').resolve()


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

        # Archive the files
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


    async def _update_data_source(self, name):
        """Updates a data source."""

    async def _update_network(self, name):
        """Updates a network."""


    async def run(self):
        """Executes the run of the update."""
        logging.info(f"Updating schemas on {self.url!r}")

        # Update the catalogue
        await self.update_catalogue()

        await self.update_cohorts()

        if self.server_type == 'data_catalogue':
            await self.update_data_sources()
            await self.update_networks()


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
