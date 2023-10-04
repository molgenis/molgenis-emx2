# from molgenis_emx2_pyclient import Client
import logging
import os
import pathlib
from typing import TypeAlias, Literal

from molgenis_emx2_pyclient.exceptions import NoSuchSchemaException

from tools.pyclient.src.molgenis_emx2_pyclient import Client
from tools.staging_migrator.src.molgenis_emx2_staging_migrator.constants import BASE_DIR
from tools.staging_migrator.src.molgenis_emx2_staging_migrator.utils import get_staging_cohort_id, \
    find_cohort_references, construct_delete_query, construct_delete_variables

log = logging.getLogger(__name__)

SchemaType: TypeAlias = Literal['source', 'target']


class StagingMigrator(Client):
    """
    The StagingMigrator class is used to migrate updated data in a staging area to a catalogue.
    The class subclasses the Molgenis EMX2 Pyclient to access the API on the server
    """

    def __init__(self, url: str, staging_area: str, catalogue: str = 'catalogue'):
        """Sets up the StagingMigrator by logging in to the client."""
        super().__init__(url)
        self.staging_area = staging_area
        self.catalogue = catalogue

    def signin(self, username: str, password: str):
        """Signs the user in to the server using the Client's signin method
        and verifies the source schema and catalogue schema are on the server.
        """
        super().signin(username, password)
        self._verify_schemas()

    def migrate(self):
        """Performs the migration of the staging area to the catalogue."""

        # Download the target catalogue for upload in case of an error during execution
        self._download_schema_zip(schema=self.catalogue, schema_type='target', include_system_columns=False)

        # Delete the source tables from the target database
        self._delete_staging_from_catalogue()

        # Filter and download the staging area tables

        # Upload the staging area tables to the catalogue

    def _download_schema_zip(self, schema: str, schema_type: SchemaType, include_system_columns: bool = True):
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

    def _delete_staging_from_catalogue(self):
        """
        Prepares the staging area by deleting data from tables
        that are later synchronized from the staging area.
        """

        # Gather the tables to delete from the target catalogue
        tables_to_delete = find_cohort_references(self._schema_schema(self.catalogue))
        print("\n".join([f"{k:25}: {v}" for (k, v) in tables_to_delete.items()]))

        staging_cohort_id = get_staging_cohort_id(self.url, self.session, self.staging_area)
        for t_name, t_type in tables_to_delete.items():
            # Iterate over the tables that reference the Cohorts table of the staging area
            # Check if any row matches this Cohorts table
            table_id = self._schema_schema(self.catalogue).get(t_name).get('id')

            delete_rows = self._query_delete_rows(t_name, t_type, staging_cohort_id)

            if len(delete_rows) > 0:
                if not self._cohorts_in_ref_array(t_name):
                    log.info(f"\nDeleting row with primary keys {delete_rows.get(table_id)}\n"
                             f" in table {t_name}.")

                    # Delete the matching rows from the target catalogue table
                    self._delete_table_entries(table_id=table_id,
                                               pkeys=delete_rows.get(table_id))
                else:
                    log.info(f"\nUpdating row with primary keys {delete_rows.get(table_id)}"
                             f"\n in table {t_name}. (Not yet implemented)")
                    # TODO: implement following
                    # self._delete_from_ref_array(schema=catalogue, table_id=table_schema['id'],
                    #                             pkeys=response.json().get('data').get(table_schema['id']))

    def _query_delete_rows(self, t_name: str, t_type: str,
                           staging_cohort_id: str) -> dict:
        """Queries the rows to be deleted from a table."""
        query = construct_delete_query(self._schema_schema(self.catalogue), t_name)
        variables = construct_delete_variables(self._schema_schema(self.catalogue), staging_cohort_id, t_name, t_type)

        response = self.session.post(url=f"{self.url}/{self.catalogue}/graphql",
                                     json={"query": query, "variables": variables})
        return response.json().get('data')

    def _delete_table_entries(self, table_id: str, pkeys: list):
        """Deletes the rows marked by the primary keys from the table."""
        query = (f"mutation delete($pkey:[{table_id}Input]) {{\n"
                 f"  delete({table_id}:$pkey) {{message}}\n"
                 f"}}")

        batch_size = 1000
        for _batch in range(0, len(pkeys), batch_size):
            variables = {'pkey': pkeys[_batch:_batch + batch_size]}
            response = self.session.post(
                url=f"{self.url}/{self.catalogue}/graphql",
                json={"query": query, "variables": variables}
            )
            if response.status_code != 200:
                log.error(response)
                log.error(f"Deleting entries from table {table_id} failed.")

    def _cohorts_in_ref_array(self, _table: str) -> bool:
        """Returns True if cohorts are referenced in a referenced array in any column in this table."""
        _table_schema = self._schema_schema(self.catalogue)[_table]
        return (len([col for col in _table_schema['columns']
                     if ((col.get('columnType') == 'REF_ARRAY') & (col.get('refTable') == 'Cohorts'))]) > 0)

    def _verify_schemas(self):
        """Ensures the staging area and catalogue are available."""
        if self.staging_area not in self.schemas:
            raise NoSuchSchemaException(f"Schema '{self.staging_area}' not found on server."
                                        f" Available schemas: {', '.join(self.schemas)}.")
        if self.catalogue not in self.schemas:
            raise NoSuchSchemaException(f"Schema '{self.catalogue}' not found on server."
                                        f" Available schemas: {', '.join(self.schemas)}.")
