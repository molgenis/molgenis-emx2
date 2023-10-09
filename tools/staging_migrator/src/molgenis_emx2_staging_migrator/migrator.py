# from molgenis_emx2_pyclient import Client
import logging
import os
import pathlib
from typing import TypeAlias, Literal

from molgenis_emx2_pyclient.exceptions import NoSuchSchemaException, NoSuchTableException, PyclientException

from tools.pyclient.src.molgenis_emx2_pyclient import Client
from tools.staging_migrator.src.molgenis_emx2_staging_migrator.constants import BASE_DIR
from tools.staging_migrator.src.molgenis_emx2_staging_migrator.utils import get_cohort_ids, \
    find_cohort_references, construct_delete_query, construct_delete_variables, prepare_pkey

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
        # self._download_schema_zip(schema=self.catalogue, schema_type='target', include_system_columns=False)

        # Delete the source tables from the target database
        # self._delete_staging_from_catalogue()

        # Filter and download the staging area tables
        self._find_source_table_references()
        cohort_ids = get_cohort_ids(self.url, self.session, self.staging_area)
        _tables_to_sync = find_cohort_references(schema_schema=self._schema_schema(self.staging_area))
        tables_to_sync = dict(reversed(_tables_to_sync.items()))
        for table, ref_col in tables_to_sync.items():
            print(table)
            try:
                self._synchronize_table(table_name=table, ref_col=ref_col, cohort_ids=cohort_ids)
            except NoSuchTableException as e:
                print(e)

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

        cohort_ids = get_cohort_ids(self.url, self.session, self.staging_area)
        for t_name, t_type in tables_to_delete.items():
            # Iterate over the tables that reference the Cohorts table of the staging area
            # Check if any row matches this Cohorts table
            table_id = self._schema_schema(self.catalogue).get(t_name).get('id')

            delete_rows = self._query_delete_rows(t_name, t_type, cohort_ids)

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
                           cohort_ids: list) -> dict:
        """Queries the rows to be deleted from a table."""
        query = construct_delete_query(self._schema_schema(self.catalogue), t_name)
        variables = construct_delete_variables(self._schema_schema(self.catalogue), cohort_ids, t_name, t_type)

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

    def _find_source_table_references(self):
        tables_to_sync = find_cohort_references(self._schema_schema(self.staging_area))
        print("\n".join([f"{k:25}: {v}" for (k, v) in tables_to_sync.items()]))

    def _synchronize_table(self, table_name: str, ref_col: str, cohort_ids: list):
        """Synchronizes the table from the staging area to the catalogue by performing three tasks:
        1. Delete rows from the catalogue relating to the cohort that are not in the staging area table
        2. Update rows in the catalogue that are also in the staging area
        3. Insert rows from the staging area into the catalogue that were not yet present
        """
        # Query the related rows in the staging area
        staging_rows = self._get_staging_rows(table_name, ref_col, cohort_ids)

        # Query the related rows in the catalogue
        catalogue_rows = self._get_catalogue_rows(table_name, ref_col, cohort_ids)

        # Filter records that are in catalogue table but not in staging area table
        missing_rows = [row for row in catalogue_rows if row not in staging_rows]
        self._delete_rows(table_name, missing_rows)

        # Update rows to the catalogue table
        present_rows = [row for row in staging_rows if row in catalogue_rows]
        self._update_rows(table_name, present_rows, ref_col, cohort_ids)

        # Insert rows to the catalogue table
        new_rows = [row for row in staging_rows if row not in catalogue_rows]
        self._insert_rows(table_name, new_rows)

    def _get_staging_rows(self, table_name: str, ref_col: str, cohort_ids: list) -> list:
        """Retrieves the rows corresponding to the Cohorts in the staging area table."""
        return self._get_database_rows(self.staging_area, table_name, ref_col, cohort_ids)

    def _get_catalogue_rows(self, table_name: str, ref_col: str, cohort_ids: list) -> list:
        """Retrieves the rows corresponding to the Cohorts in the catalogue table."""
        return self._get_database_rows(self.catalogue, table_name, ref_col, cohort_ids)

    def _get_database_rows(self, schema, table_name: str, ref_col: str, cohort_ids: list) -> list:
        """Retrieves the rows corresponding to the Cohorts in a schema's table."""
        schema_schema = self._schema_schema(schema)
        _schema_query = construct_delete_query(db_schema=schema_schema, table=table_name)
        _schema_variables = construct_delete_variables(db_schema=schema_schema, cohort_ids=cohort_ids,
                                                       t_name=table_name, t_type=ref_col)
        _schema_response = self.session.post(url=f"{self.url}/{schema}/graphql",
                                             json={"query": _schema_query, "variables": _schema_variables})
        table_id = schema_schema[table_name].get('id')
        if _schema_response.status_code == 400:
            if 'Unknown type' in _schema_response.json().get('errors')[0].get('message'):
                raise NoSuchTableException(f"Table '{table_id}' not in schema tables.")
            else:
                raise PyclientException
        schema_rows = _schema_response.json().get('data').get(table_id, [])
        return schema_rows

    def _delete_rows(self, table_name: str, rows: list):
        """Deletes rows from the table within a schema."""
        self._delete_table_entries(table_name, rows)

    def _update_rows(self, table_name: str, rows: list, ref_col: str, cohort_ids: list):
        """Updates the already present rows in the catalogue table."""

        # Do not update if there are no rows to update
        if len(rows) < 1:
            return

        # Generate query that fetches all the table's columns for these rows
        staging_schema = self._schema_schema(self.staging_area)
        table_id = staging_schema[table_name].get('id')

        filter_query = construct_delete_query(staging_schema, table_name, all_columns=True)

        filter_variables = construct_delete_variables(staging_schema, cohort_ids, table_name, ref_col)

        filter_response = self.session.post(url=f"{self.url}/{self.staging_area}/graphql",
                                            json={"query": filter_query, "variables": filter_variables})
        # TODO: remove following debugging line
        if table_name == 'Contacts':
            query = construct_delete_query(staging_schema, table_name, all_columns=True)
            print("Does not work due to missing column.")
            return

        data_rows = filter_response.json().get('data').get(table_id)

        # Use the result as variables for the mutation query

        # Generate update query
        update_query = f"mutation update($value:[{table_id}Input]){{update({table_id}:$value){{message}}}}"
        update_variables = {"value": data_rows}

        # Perform update query on catalogue database
        update_response = self.session.post(url=f"{self.url}/{self.catalogue}/graphql",
                                            json={"query": update_query, "variable": update_variables})

        if update_response.status_code == 200:
            log.info(update_response.json().get('message')[0])
        else:
            log.error(f"Error {update_response.status_code}: {update_response.json().get('errors')}")

    def _insert_rows(self, table_name: str, rows: list):
        """Inserts the new rows from the staging area into the catalogue."""
        log.warning(f"Method '_insert_rows' not implemented yet.")
