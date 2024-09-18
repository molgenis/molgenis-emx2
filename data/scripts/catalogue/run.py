import asyncio
import logging
import os
import shutil
from pathlib import Path
from zipfile import ZipFile

from decouple import config

from molgenis_emx2_pyclient import Client
from update.update_4_x import Transform

FILES_DIR = Path(__file__).parent.joinpath('files').resolve()


class Runner:
    """
    Class that handles the running of the update.
    It uses a source and target Pyclient object for handling data
    on the source and target servers.
    """

    def __init__(self, source: Client, target: Client, pattern = None):
        """Initializes the object."""

        # Set the source and target Clients
        self.source = source
        self.target = target

        # Set additional attributes
        self.server_type = config('MG_SERVER_TYPE')
        self.catalogue = config('MG_CATALOGUE_SCHEMA_NAME')

        # Set resource type names
        self.cohorts, self.data_sources, self.networks = self._prepare_resource_names(self.server_type)

        # Set the pattern
        if pattern is not None:
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

    def has_latest_ontologies(self) -> bool:
        """Checks if the target server has the latest CatalogueOntologies."""
        new_ontologies = ['Clinical study types', 'Cohort collection types']
        server_ontologies = self.target.get_schema_metadata(name='CatalogueOntologies').tables

        return all(new_ont in [table.name for table in server_ontologies] for new_ont in new_ontologies)

    async def update_catalogue(self):
        """
        Updates the data model and data in the catalogue schema.
        """
        logging.info(f"Starting update on {self.catalogue!r}")

        description = {s.id: s for s in self.source.get_schemas()}[self.catalogue].get('description')

        if f"{self.catalogue}{self.pattern}" in self.target.schema_names:
            create_schema = asyncio.create_task(self.target.recreate_schema(name=f"{self.catalogue}{self.pattern}",
                                                                     description=description))
        else:
            create_schema = asyncio.create_task(self.target.create_schema(name=f"{self.catalogue}{self.pattern}",
                                                                   description=description))

        # Export catalogue data to zip
        if not FILES_DIR.exists():
            FILES_DIR.mkdir()
        await self.source.export(schema=self.catalogue, filename=str(FILES_DIR.joinpath(f"{self.catalogue}_data.zip")))

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
        await self.target.upload_file(file_path=FILES_DIR.joinpath(f"{self.catalogue}_upload.zip"),
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
        logging.info(f"Starting update on {name!r}")
        description = {s.id: s for s in self.source.get_schemas()}[name].get('description')

        if f"{name}{self.pattern}" in self.target.schema_names:
            create_schema = asyncio.create_task(self.target.recreate_schema(name=f"{name}{self.pattern}",
                                                                     description=description))
        else:
            create_schema = asyncio.create_task(self.target.create_schema(name=f"{name}{self.pattern}",
                                                                   description=description))

        # Export catalogue data to zip
        if not FILES_DIR.exists():
            FILES_DIR.mkdir()
        await self.source.export(schema=name, filename=str(FILES_DIR.joinpath(f"{name}_data.zip")))

        logging.info(f"Transforming data from schema {name}")
        schema_transform = Transform(database_name=name, database_type='cohort_UMCG')

        # Extract the zip file
        with ZipFile(FILES_DIR.joinpath(f"{name}_data.zip"), 'r') as zf:
            zf.extractall(path=FILES_DIR.joinpath(f"{name}_data"))

        # Replace the data model file
        schema_transform.delete_data_model_file()
        schema_transform.update_data_model_file()

        # Transform the data
        schema_transform.transform_data()

        # Archive the files
        with ZipFile(FILES_DIR.joinpath(f"{name}_upload.zip"), 'w') as zf:
            for file_path in FILES_DIR.joinpath(f"{name}_data").iterdir():
                zf.write(file_path, arcname=file_path.name)
            if FILES_DIR.joinpath(f"{name}_data", '_files').exists():
                for file_path in FILES_DIR.joinpath(f"{name}_data", '_files').iterdir():
                    zf.write(file_path, arcname=f"_files/{file_path.name}")
            # zf.write(FILES_DIR.joinpath(f"{name}_data_model", 'molgenis.csv'), arcname='molgenis.csv')

        # Upload the updated data
        await create_schema
        await self.target.upload_file(file_path=FILES_DIR.joinpath(f"{name}_upload.zip"),
                               schema=f"{name}{self.pattern}")


    async def _update_data_source(self, name):
        """Updates a data source."""

    async def _update_network(self, name):
        """Updates a network."""


async def main(pattern = None):
    # Initialize the client with URL and token
    source_server = config('MG_SOURCE_SERVER_URL')
    source_token = config('MG_SOURCE_SERVER_TOKEN')

    target_server = config('MG_TARGET_SERVER_URL')
    target_token = config('MG_TARGET_SERVER_TOKEN')

    with (Client(url=source_server, token=source_token) as source,
          Client(url=target_server, token=target_token) as target):
        runner = Runner(source, target, pattern=pattern)
        logging.info(f"Updating schemas on {runner.target.url!r}")

        if not runner.has_latest_ontologies():
            # Trigger CatalogueOntologies update by creating a dummy catalogue
            if not 'dummy' in target.schema_names:
                create_dummy = asyncio.create_task(runner.target.create_schema(name='_dummy',
                                                                                  template='DATA_CATALOGUE',
                                                                                  include_demo_data=False))
                await create_dummy
            delete_dummy = asyncio.create_task(runner.target.delete_schema('_dummy'))

            await delete_dummy

        # Update the catalogue
        await runner.update_catalogue()

        await runner.update_cohorts()

        if runner.server_type == 'data_catalogue':
            await runner.update_data_sources()
            await runner.update_networks()

    # Clean up
    shutil.rmtree(FILES_DIR)


if __name__ == '__main__':
    logging.basicConfig(level='DEBUG', format = '%(filename)s:%(lineno)s %(levelname)s:%(message)s')
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)

    asyncio.run(main(pattern=''))
