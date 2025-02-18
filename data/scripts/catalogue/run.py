import asyncio
import logging
import shutil
from pathlib import Path
from zipfile import ZipFile

import tqdm
from decouple import config
from molgenis_emx2_pyclient import Client
from molgenis_emx2_pyclient.exceptions import NoSuchSchemaException
from molgenis_emx2_pyclient.metadata import Schema
from tqdm.contrib.logging import logging_redirect_tqdm

from update.update_5_x import Transform

FILES_DIR = Path(__file__).parent.joinpath('files').resolve()

SHAREDSTAGING = 'SharedStaging'
ORGANISATIONS = 'Organisations'

MIN_VERSION = 'v11.22.0'


class Runner:
    """
    Class that handles the running of the update.
    It uses a source and target Pyclient object for handling data
    on the source and target servers.
    """

    def __init__(self, source: Client, target: Client, pattern = None, _debug: bool = False):
        """Initializes the object."""

        # Set the source and target Clients
        self.source = source
        self.target = target

        # Set additional attributes
        self.server_type = config('MG_SERVER_TYPE')
        self.catalogue = config('MG_CATALOGUE_SCHEMA_NAME')

        if not _debug:
            # Set resource type names
            stagings_by_type = self.gather_staging_types()
            self.cohorts = stagings_by_type.get('cohorts')
            self.data_sources = stagings_by_type.get('datasources')
            self.networks = stagings_by_type.get('networks')
            self.catalogues = stagings_by_type.get('catalogues')
            self.shared_stagings = stagings_by_type.get('shared')
            logging.info("Number of schemas per schema type: ")
            for key, value in stagings_by_type.items():
                logging.info(f"{key}: {len(value)}")

        # Set the pattern
        if pattern is not None:
            self.pattern = pattern
        else:
            self.pattern = '_'

    def __repr__(self):
        return f"Runner(source={self.source!r}, target={self.target!r}, pattern={self.pattern!r})"

    def has_latest_ontologies(self) -> bool:
        """Checks if the target server has the latest CatalogueOntologies."""
        new_ontologies = ['Clinical study types', 'Cohort collection types', 'Inclusion Exclusion Criteria']
        try:
            server_ontologies = self.target.get_schema_metadata(name='CatalogueOntologies').tables
        except NoSuchSchemaException as e:
            logging.warning(e)
            return False

        ontologies_exist = all(new_ont in [table.name for table in server_ontologies] for new_ont in new_ontologies)
        if not ontologies_exist:
            return False
        criteria_term = 'Health status inclusion criterion'
        if criteria_term not in self.target.get(schema='CatalogueOntologies',
                                                table='Inclusion Exclusion Criteria',
                                                query_filter=f"name == {criteria_term}",
                                                as_df=True)['name'].values:
            return False
        return True

    def gather_staging_types(self) -> dict[str, list[str]]:
        """Gathers the names of staging areas with the 'CohortsStaging' data model."""
        schemas = self.source.schema_names
        cohort_stagings = []
        datasource_stagings = []
        networks_stagings = []
        catalogues = []
        shared_stagings = []

        with logging_redirect_tqdm():
            logging.info("Checking schema data models.")
            for schema in tqdm.tqdm(schemas):
                metadata: Schema = self.source.get_schema_metadata(schema)
                try:
                    table_names = [t.name for t in metadata.tables]
                except AttributeError:
                    logging.warning(f"Could not find tables in schema {schema!r}.")
                    continue
                if 'Cohorts' in table_names and 'Networks' not in table_names:
                    cohort_stagings.append(schema)
                elif ('Data sources' in table_names
                        and 'Networks' not in table_names
                        and 'Cohorts' not in table_names):
                    datasource_stagings.append(schema)
                elif ('Networks' in table_names
                        and 'Cohorts' not in table_names
                        and 'Data sources' not in table_names):
                    networks_stagings.append(schema)
                elif ('Networks' in table_names
                        and 'Cohorts' in table_names
                        and 'Data sources' in table_names):
                    catalogues.append(schema)
                elif (ORGANISATIONS in table_names
                      and 'Cohorts' not in table_names):
                    shared_stagings.append(schema)
                else:
                    logging.warning(f"Schema {schema!r} does not fit the models.")

        return {'cohorts': cohort_stagings, 'datasources': datasource_stagings, 'networks': networks_stagings,
                'catalogues': catalogues, 'shared': shared_stagings}


    async def update_catalogue(self):
        """
        Updates the data model and data in the catalogue schema.
        """
        await self._update_schema(name=self.catalogue, database_type='catalogue')

    async def unpack_catalogue(self):
        """
        Exports the catalogue schema zip and performs the updates without uploading the data.
        """
        logging.info(f"Unpacking {self.catalogue!r} data and performing data updates.")
        await self._update_schema(name=self.catalogue, database_type='catalogue', transform_only=False)



    async def unpack_shared_staging(self):
        """
        Exports the SharedStaging zip and performs the updates without uploading the data.
        """
        logging.info(f"Unpacking {SHAREDSTAGING!r} data and performing data updates.")
        await self._update_schema(name=SHAREDSTAGING, database_type='shared_staging', transform_only=True)


    async def update_cohorts(self):
        """Updates the cohort schemas on a server."""
        logging.info(f"Cohorts to update: {', '.join(self.cohorts)}")
        with logging_redirect_tqdm():
            for cohort in tqdm.tqdm(self.cohorts):
                logging.info(f"Updating cohort staging area {cohort!r}")
                database_type = 'cohort_UMCG' if self.server_type == 'cohort_catalogue' else 'cohort'
                await self._update_schema(cohort, database_type=database_type)
                await asyncio.sleep(1.5)

    async def update_data_sources(self):
        """Updates the data sources on a schema."""
        logging.info(f"Data sources to update: {', '.join(self.data_sources)}")
        with logging_redirect_tqdm():
            for ds in tqdm.tqdm(self.data_sources):
                logging.info(f"Updating data source staging area {ds!r}")
                await self._update_schema(name=ds, database_type='data_source')

    async def update_networks(self):
        """Updates the networks on a schema."""
        logging.info(f"Networks to update: {', '.join(self.networks)}")
        with logging_redirect_tqdm():
            for network in tqdm.tqdm(self.networks):
                logging.info(f"Updating networks staging area {network!r}")
                await self._update_schema(name=network, database_type='network')


    async def _update_schema(self, name: str, database_type: str, transform_only: bool = False):
        """Updates a resource staging area. Specify the name and the type of the database."""
        logging.info(f"Starting update on {name!r}")
        description = {s.id: s for s in self.source.get_schemas()}[name].get('description')

        # Export catalogue data to zip
        if not FILES_DIR.exists():
            FILES_DIR.mkdir()
        await self.source.export(schema=name, filename=str(FILES_DIR.joinpath(f"{name}_data.zip")))

        logging.info(f"Transforming data from schema {name}")
        schema_transform = Transform(database_name=name, database_type=database_type)

        # Extract the zip file
        with ZipFile(FILES_DIR.joinpath(f"{name}_data.zip"), 'r') as zf:
            zf.extractall(path=FILES_DIR.joinpath(f"{name}_data"))

        # Replace the data model file
        schema_transform.delete_data_model_file()
        schema_transform.update_data_model_file()

        # Transform the data
        schema_transform.transform_data()

        # Return the function if only the transformation is requested
        if transform_only:
            return

        if f"{name}{self.pattern}" in self.target.schema_names:
            create_schema = asyncio.create_task(self.target.recreate_schema(name=f"{name}{self.pattern}",
                                                                     description=description))
        else:
            create_schema = asyncio.create_task(self.target.create_schema(name=f"{name}{self.pattern}",
                                                                   description=description))

        # Archive the files
        with ZipFile(FILES_DIR.joinpath(f"{name}_upload.zip"), 'w') as zf:
            for file_path in FILES_DIR.joinpath(f"{name}_data").iterdir():
                zf.write(file_path, arcname=file_path.name)
            if FILES_DIR.joinpath(f"{name}_data", '_files').exists():
                for file_path in FILES_DIR.joinpath(f"{name}_data", '_files').iterdir():
                    zf.write(file_path, arcname=f"_files/{file_path.name}")
            if database_type == 'catalogue':
                zf.write(FILES_DIR.joinpath(f"{name}_data_model", 'molgenis.csv'), arcname='molgenis.csv')

        # Upload the updated data
        await create_schema
        await self.target.upload_file(file_path=FILES_DIR.joinpath(f"{name}_upload.zip"),
                               schema=f"{name}{self.pattern}")


async def main(pattern = None, debug: bool = False):
    # Initialize the client with URL and token
    source_server = config('MG_SOURCE_SERVER_URL')
    source_token = config('MG_SOURCE_SERVER_TOKEN')

    target_server = config('MG_TARGET_SERVER_URL')
    target_token = config('MG_TARGET_SERVER_TOKEN')

    with (Client(url=source_server, token=source_token) as source,
          Client(url=target_server, token=target_token) as target):

        # Set up the Runner
        runner = Runner(source, target, pattern=pattern, _debug=debug)
        logging.info(f"Updating schemas on {runner.target.url!r}")

        if debug:
            runner.cohorts = ['ABCD']
            runner.data_sources = []
            runner.networks = []

        if not runner.has_latest_ontologies():
            # Trigger CatalogueOntologies update by creating a dummy catalogue
            if runner.target.version < MIN_VERSION:
                raise FileNotFoundError(f"Update software to >={MIN_VERSION} "
                                        f"to ensure the installation of the correct ontologies.")
            dummy = '_dummy'
            if not dummy in target.schema_names:
                create_dummy = asyncio.create_task(runner.target.create_schema(name=dummy,
                                                                                  template='DATA_CATALOGUE',
                                                                                  include_demo_data=False))
                await create_dummy
            delete_dummy = asyncio.create_task(runner.target.delete_schema(dummy))

            await delete_dummy


        # Update the catalogue
        await runner.update_catalogue()

        # Unpack and transform SharedStaging data without uploading
        await runner.unpack_shared_staging()

        # Update the cohorts
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

    asyncio.run(main(pattern='', debug=True))
