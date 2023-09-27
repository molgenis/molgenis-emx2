# from molgenis_emx2_pyclient import Client
import logging
import os
import pathlib
from typing import TypeAlias, Literal

from molgenis_emx2_pyclient.exceptions import NoSuchSchemaException

from tools.pyclient.src.molgenis_emx2_pyclient import Client
from tools.staging_migrator.src.molgenis_emx2_staging_migrator.constants import BASE_DIR
from tools.staging_migrator.src.molgenis_emx2_staging_migrator.utils import get_staging_cohort_id, prepare_pkey, \
    find_cohort_references, construct_delete_query, construct_delete_variables

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
        self._download_schema_zip(schema=catalogue, schema_type='target', include_system_columns=False)

        # Delete the source tables from the target database
        self._delete_staging_from_catalogue(staging_area, catalogue)

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

    def _delete_staging_from_catalogue(self, staging_area: str, catalogue: str):
        """
        Prepares the staging area by deleting data from tables
        that are later synchronized from the staging area.
        """

        # Gather the tables to delete from the target catalogue
        tables_to_delete = find_cohort_references(self._schema_schema(catalogue))

        staging_cohort_id = get_staging_cohort_id(self.url, self.session, staging_area)
        schema_schema = self._schema_schema(catalogue)
        for t_name, t_type in tables_to_delete.items():
            # Iterate over the tables that reference the Cohorts table of the staging area
            # Check if any row matches this Cohorts table
            table_schema = schema_schema[t_name]
            # TODO: put following in method and return response
            pkeys = [prepare_pkey(schema_schema, t_name, col['id']) for col in table_schema['columns'] if
                     col.get('key') == 1]
            query = construct_delete_query(schema_schema, t_name, pkeys)
            variables = construct_delete_variables(staging_cohort_id, t_name, t_type, schema_schema)

            response = self.session.post(url=f"{self.url}/{catalogue}/graphql",
                                         json={"query": query, "variables": variables})

            if len(response.json().get('data')) > 0:
                if not self._cohorts_in_ref_array(table_schema):
                    print(f"\nDeleting row with primary keys {response.json().get('data').get(table_schema['id'])}\n"
                          f" in table {t_name}.")

                    # Delete the matching rows from the target catalogue table
                    self._delete_table_entries(schema=catalogue, table_id=table_schema['id'],
                                               pkeys=response.json().get('data').get(table_schema['id']))
                else:
                    print(f"\nUpdating row with primary keys {response.json().get('data').get(table_schema['id'])}"
                          f"\n in table {t_name}. (Not yet implemented)")
                    # TODO: implement following
                    # self._delete_from_ref_array(schema=catalogue, table_id=table_schema['id'],
                    #                             pkeys=response.json().get('data').get(table_schema['id']))

    def _delete_table_entries(self, schema: str, table_id: str, pkeys: list):
        """Deletes the rows marked by the primary keys from the table."""
        query = (f"mutation delete($pkey:[{table_id}Input]) {{\n"
                 f"  delete({table_id}:$pkey) {{message}}\n"
                 f"}}")

        batch_size = 1000
        for _batch in range(0, len(pkeys), batch_size):
            variables = {'pkey': pkeys[_batch:_batch + batch_size]}
            response = self.session.post(
                url=f"{self.url}/{schema}/graphql",
                json={"query": query, "variables": variables}
            )
            if response.status_code != 200:
                log.error(response)
                log.error(f"Deleting entries from table {table_id} failed.")

    @staticmethod
    def _cohorts_in_ref_array(_table_schema: dict) -> bool:
        """Returns True if cohorts are referenced in a referenced array in any column in this table."""
        return (len([col for col in _table_schema['columns']
                     if ((col.get('columnType') == 'REF_ARRAY') & (col.get('refTable') == 'Cohorts'))]) > 0)
