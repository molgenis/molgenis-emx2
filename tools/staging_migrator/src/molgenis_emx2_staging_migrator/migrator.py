import logging
import os
import pathlib
import time
import zipfile
from io import BytesIO
from typing import TypeAlias, Literal

import pandas as pd
from molgenis_emx2_pyclient.exceptions import NoSuchSchemaException

from tools.pyclient.src.molgenis_emx2_pyclient import Client
from tools.staging_migrator.src.molgenis_emx2_staging_migrator.constants import BASE_DIR
from tools.staging_migrator.src.molgenis_emx2_staging_migrator.utils import get_cohort_ids, \
    find_cohort_references, construct_delete_query, construct_delete_variables, has_statement_of_consent, \
    process_statement, prepare_pkey

log = logging.getLogger('Molgenis EMX2 Migrator')

SchemaType: TypeAlias = Literal['source', 'target']


class StagingMigrator(Client):
    """
    The StagingMigrator class is used to migrate updated data in a staging area to a catalogue.
    The class subclasses the Molgenis EMX2 Pyclient to access the API on the server
    """

    def __init__(self, url: str, staging_area: str = None, catalogue: str = 'catalogue', table: str = 'Cohorts'):
        """Sets up the StagingMigrator by logging in to the client."""
        super().__init__(url)
        self.staging_area = staging_area
        self.catalogue = catalogue
        self.table = table

    def signin(self, username: str, password: str):
        """Signs the user in to the server using the Client's signin method
        and verifies the source schema and catalogue schema are on the server.
        """
        super().signin(username, password)
        self._verify_schemas()

    def set_staging_area(self, staging_area: str):
        """Sets the staging area and verifies its existence."""
        old_staging_area = self.staging_area

        self.staging_area = staging_area
        try:
            self._verify_schemas()
        except NoSuchSchemaException:
            self.staging_area = old_staging_area

    def set_catalogue(self, catalogue: str):
        """Sets the catalogue and verifies its existence."""
        old_catalogue = self.catalogue

        self.catalogue = catalogue
        try:
            self._verify_schemas()
        except NoSuchSchemaException:
            self.catalogue = old_catalogue

    def migrate(self):
        """Performs the migration of the staging area to the catalogue."""

        # Download the target catalogue for upload in case of an error during execution
        self._download_schema_zip(schema=self.catalogue, schema_type='target', include_system_columns=False)

        # Delete the source tables from the target database
        log.info("Deleting staging area cohorts from the catalogue.")
        self._delete_staging_from_catalogue()

        # Create zipfile for uploading
        zip_stream = self._create_upload_zip()

        # Upload the zip to the target schema
        self._upload_zip_stream(zip_stream)

        # Remove any downloaded files from disk
        self._cleanup()

    def _download_schema_zip(self, schema: str, schema_type: SchemaType,
                             include_system_columns: bool = True) -> str:
        """Download target schema as zip, save in case upload fails."""
        filename = f"{BASE_DIR}/{schema_type}.zip"
        if os.path.exists(filename):
            os.remove(filename)
        api_zip_url = f"{self.url}/{schema}/api/zip"
        if include_system_columns:
            api_zip_url += '?includeSystemColumns=true'
        resp = self.session.get(api_zip_url, allow_redirects=True)

        if resp.content:
            pathlib.Path(filename).write_bytes(resp.content)
            log.debug(f"Downloaded '{schema_type}' schema to '{filename}'.")
        else:
            log.error("Error: download failed.")
        return filename

    def _delete_staging_from_catalogue(self):
        """
        Prepares the staging area by deleting data from tables
        that are later synchronized from the staging area.
        """

        # Gather the tables to delete from the target catalogue
        tables_to_delete = find_cohort_references(self._schema_schema(self.catalogue))

        cohort_ids = get_cohort_ids(self.url, self.session, self.staging_area)
        for table_name, ref_col in tables_to_delete.items():
            # Iterate over the tables that reference the core table of the staging area
            # Check if any row matches this core table
            table_id = self._schema_schema(self.catalogue).get(table_name).get('id')

            delete_rows = self._query_delete_rows(table_name, ref_col, cohort_ids)

            if len(delete_rows) == 0:
                continue
            if not self._cohorts_in_ref_array(table_name):
                log.debug(f"\nDeleting in table '{table_name}' row(s) with primary keys {delete_rows.get(table_id)}.")

                # Delete the matching rows from the target catalogue table
                self._delete_table_entries(table_id=table_id,
                                           pkeys=delete_rows.get(table_id))
            else:
                log.debug(f"\nUpdating row(s) with primary keys {delete_rows.get(table_id)}"
                          f"\n in table {table_name}. (Not yet implemented)")
                # TODO: implement following
                # self._delete_from_ref_array(schema=catalogue, table_id=table_schema['id'],
                #                             pkeys=response.json().get('data').get(table_schema['id']))

    def _query_delete_rows(self, table_name: str, ref_col: str,
                           cohort_ids: list) -> dict:
        """Queries the rows to be deleted from a table."""
        query = construct_delete_query(self._schema_schema(self.catalogue), table_name)
        variables = construct_delete_variables(self._schema_schema(self.catalogue), cohort_ids, table_name, ref_col)

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
        if self.staging_area is not None:
            if self.staging_area not in self.schemas:
                raise NoSuchSchemaException(f"Schema '{self.staging_area}' not found on server."
                                            f" Available schemas: {', '.join(self.schemas)}.")
        if self.catalogue not in self.schemas:
            raise NoSuchSchemaException(f"Schema '{self.catalogue}' not found on server."
                                        f" Available schemas: {', '.join(self.schemas)}.")

    def _create_upload_zip(self) -> BytesIO:
        """Combines the relevant tables of the staging area into a zipfile."""
        tables_to_sync = find_cohort_references(self._schema_schema(self.staging_area))

        source_file_path = self._download_schema_zip(schema=self.staging_area, schema_type='source',
                                                     include_system_columns=False)

        upload_stream = BytesIO()

        with (zipfile.ZipFile(source_file_path, 'r') as source_archive,
              zipfile.ZipFile(upload_stream, 'w', zipfile.ZIP_DEFLATED, False) as upload_archive):
            for file_name in source_archive.namelist():
                if '_files/' in file_name:
                    upload_archive.writestr(file_name, BytesIO(source_archive.read(file_name)).getvalue())
                    continue
                elif (table_name := os.path.splitext(file_name)[0]) not in tables_to_sync.keys():
                    continue

                _table = source_archive.read(file_name)
                if consent_val := has_statement_of_consent(table_name, self._schema_schema(self.staging_area)):
                    _table = process_statement(table=_table, consent_val=consent_val)
                # self._check_diff(file_name, _table)
                upload_archive.writestr(file_name, BytesIO(_table).getvalue())

        log.info(f"Migrating tables {', '.join(tables_to_sync.keys())}.")
        return upload_stream

    def _upload_zip_stream(self, zip_stream: BytesIO, is_fallback: bool = False):
        """Uploads the zip file containing the tables from the staging area
        to the catalogue.
        """
        upload_url = f"{self.url}/{self.catalogue}/api/zip?async=true"

        response = self.session.post(
            url=upload_url,
            files={'file': (f'{BASE_DIR}/upload.zip', zip_stream.getvalue())}
        )

        response_status = response.status_code
        if response.status_code != 200:
            log.error(f"Migration failed with error {response_status}:\n{str(response.text)}")
            log.error("Uploading fallback zip.")
            self._upload_fallback_zip()
        else:
            response_url = f"{self.url}{response.json().get('url')}"
            upload_status = self.session.get(response_url).json().get('status')
            while upload_status == 'RUNNING':
                time.sleep(2)
                upload_status = self.session.get(response_url).json().get('status')
            upload_description = self.session.get(response_url).json().get('description')

            if upload_status == 'ERROR':
                log.error(f"Migration failed, reason: {upload_description}.")
                log.debug(self.session.get(response_url).json())
                if not is_fallback:
                    log.info("Uploading fallback zip.")
                    self._upload_fallback_zip()
                else:
                    log.error("Restoring fallback zip failed.")
            else:
                if is_fallback:
                    log.info("Fallback zip restored successfully.")
                else:
                    log.info("Migrated successfully.")

    @staticmethod
    def _cleanup():
        """Deletes the downloaded files after successful migration."""
        zip_files = ['target.zip', 'source.zip', 'upload.zip']
        for zp in zip_files:
            filename = f"{BASE_DIR}/{zp}"
            if os.path.exists(filename):
                log.debug(f"Deleting file '{zp}'.")
                os.remove(filename)

    def _check_diff(self, file_name: str, source_table: bytes):
        """Logs an overview of differences on this table compared to the table on the server."""
        with zipfile.ZipFile(f"{BASE_DIR}/target.zip", 'r') as target_archive:
            try:
                target_table = target_archive.read(file_name)
            except Exception as e:
                print(e)

        # Convert csv files to pandas DataFrame
        source_df = pd.read_csv(BytesIO(source_table))
        target_df = pd.read_csv(BytesIO(target_table))

        if not (len(source_df) + len(target_df)):
            return

        # Get primary keys for these tables
        p_keys = prepare_pkey(schema=self._schema_schema(self.staging_area), table_name=os.path.splitext(file_name)[0])

        source_df[p_keys] = source_df[p_keys].astype(str)
        target_df[p_keys] = target_df[p_keys].astype(str)

        # Filter target_table on primary keys that are the same as the ones in the source_table
        # target_df = target_df.loc[target_df == source_df]
        merge = pd.merge(source_df, target_df, on=p_keys)
        if len(merge):
            print(merge)
        # Output in case row exists in source but not target

        # Output in case row exists in target but not source
        # Output in case all rows are the same

    def _upload_fallback_zip(self):
        """Restores the catalogue to the state before deletion by uploading the downloaded target zip."""
        target_filename = f"{BASE_DIR}/target.zip"
        upload_stream = BytesIO()
        # Load the target.zip file as a stream
        with (zipfile.ZipFile(target_filename, 'r') as target_archive,
              zipfile.ZipFile(upload_stream, 'w', zipfile.ZIP_DEFLATED) as upload_archive):
            for file_name in target_archive.namelist():
                upload_archive.writestr(file_name, BytesIO(target_archive.read(file_name)).getvalue())
        # Upload the stream
        self._upload_zip_stream(upload_stream, is_fallback=True)
