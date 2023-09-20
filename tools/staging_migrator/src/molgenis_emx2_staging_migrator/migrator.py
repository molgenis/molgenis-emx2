# from molgenis_emx2_pyclient import Client
import logging
import os
import pathlib
from typing import TypeAlias, Literal

from molgenis_emx2_pyclient.exceptions import NoSuchSchemaException
from tools.pyclient.src.molgenis_emx2_pyclient import Client
from tools.staging_migrator.src.molgenis_emx2_staging_migrator.SyncTables import TablesToDelete, TablesToSync
from tools.staging_migrator.src.molgenis_emx2_staging_migrator.constants import BASE_DIR

log = logging.getLogger(__name__)

SchemaType: TypeAlias = Literal['source', 'target']

class StagingMigrator(Client):
    """
    The StagingMigrator class is used to migrate updated data in a staging area to a catalogue.
    The class subclasses the Molgenis EMX2 Pyclient to access the API on the server
    """

    def __init__(self, url: str):
        super().__init__(url)

    def migrate(self, staging_area: str, catalogue: str = 'catalogue'):
        """Performs the migration of the staging area to the catalogue."""

        # Ensure the staging area is available, otherwise raise error
        if staging_area not in self.schemas:
            raise NoSuchSchemaException(f"Schema '{staging_area}' not found on server."
                                        f" Available schemas: {', '.join(self.schemas)}.")
        if catalogue not in self.schemas:
            raise NoSuchSchemaException(f"Schema '{catalogue}' not found on server."
                                        f" Available schemas: {', '.join(self.schemas)}.")

        # Download the target catalogue for upload in case of an error during execution
        self._download_schema(schema=catalogue, schema_type='target', include_system_columns=False)

        # Database name needs to be identical to cohort PID
        tables_to_delete = TablesToDelete.COHORT_STAGING_TO_DATA_CATALOGUE_ZIP
        tables_to_sync = TablesToSync.COHORT_STAGING_TO_DATA_CATALOGUE_ZIP

    def _download_schema(self, schema: str, schema_type: SchemaType, include_system_columns: bool = True):
        """Download target schema as zip, save in case upload fails."""
        filename = f'{BASE_DIR}/{schema_type}.zip'
        if os.path.exists(filename):
            os.remove(filename)
        api_zip_url = f"{self.url}/{schema}/api/zip"
        if include_system_columns:
            api_zip_url += '?includeSystemColumns=true'
        resp = self.session.get(api_zip_url, allow_redirects=True)

        if resp.content:
            pathlib.Path(filename).write_bytes(resp.content)
            log.info(f'Downloaded target schema to "{filename}".')
        else:
            log.error('Error: download failed')
