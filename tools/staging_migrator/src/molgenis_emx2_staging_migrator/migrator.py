import logging
import time
import zipfile
from datetime import datetime
from io import BytesIO
from pathlib import Path
from typing import TypeAlias, Literal

from molgenis_emx2_pyclient import Client
from molgenis_emx2_pyclient.exceptions import NoSuchSchemaException, NoSuchTableException
from molgenis_emx2_pyclient.metadata import Schema, Table

from .constants import BASE_DIR, changelog_query
from .utils import (find_cohort_references, construct_delete_variables, has_statement_of_consent,
                    process_statement, prepare_pkey, query_columns_string)

log = logging.getLogger('Molgenis EMX2 Migrator')

SchemaType: TypeAlias = Literal['source', 'target']

PUBLICATIONS = "Publications"
RESOURCES = "Resources"


class StagingMigrator(Client):
    """
    The StagingMigrator class is used to migrate updated data in a staging area to a catalogue.
    The class subclasses the Molgenis EMX2 Pyclient to access the API on the server
    """

    def __init__(self, url: str,
                 staging_area: str = None,
                 catalogue: str = 'catalogue',
                 table: str = 'Resources', token: str = None):
        """Sets up the StagingMigrator by logging in to the client."""
        super().__init__(url=url, token=token)
        self.staging_area = staging_area
        self.catalogue = catalogue
        self._verify_schemas()
        self.table = self.get_schema_metadata(catalogue).get_table('name', table).id
        self.extra_tables: list[str] = [PUBLICATIONS] if self.table == RESOURCES else []

    def __repr__(self):
        class_name = type(self).__name__
        args = [
            f"staging_area={self.staging_area!r}",
            f"catalogue={self.catalogue!r}",
            f"table={self.table!r}"
        ]
        return f"{class_name}({', '.join(args)})"

    def set_staging_area(self, staging_area: str):
        """Sets the staging area and verifies its existence."""
        self.staging_area = staging_area
        self._verify_schemas()

    def set_catalogue(self, catalogue: str):
        """Sets the catalogue and verifies its existence."""
        self.catalogue = catalogue
        self._verify_schemas()

    def migrate(self, keep_zips: bool = False):
        """Performs the migration of the staging area to the catalogue."""

        # Download the target catalogue for upload in case of an error during execution
        self._download_schema_zip(schema=self.catalogue, schema_type='target', include_system_columns=False)

        # Delete the source tables from the target database
        log.info("Deleting staging area resource from the catalogue.")
        self._delete_staging_from_catalogue()

        # Create zipfile for uploading
        zip_stream = self._create_upload_zip()

        # Upload the zip to the target schema
        self._upload_zip_stream(zip_stream)

        if not keep_zips:
            # Remove any downloaded files from disk
            self._cleanup()

    def _download_schema_zip(self, schema: str, schema_type: SchemaType,
                             include_system_columns: bool = True) -> str:
        """Download target schema as zip, save in case upload fails."""
        filepath = BASE_DIR.joinpath(f"{schema_type}.zip")
        if Path(filepath).exists():
            Path(filepath).unlink()

        api_zip_url = f"{self.url}/{schema}/api/zip"
        if include_system_columns:
            api_zip_url += '?includeSystemColumns=true'
        resp = self.session.get(api_zip_url,
                                headers={'x-molgenis-token': self.token},
                                allow_redirects=True)

        if resp.content:
            Path(filepath).write_bytes(resp.content)
            log.debug(f"Downloaded {schema_type!r} schema to {filepath!s}.")
        else:
            log.error("Error: download failed.")
        return filepath

    def _delete_staging_from_catalogue(self):
        """
        Prepares the staging area by deleting data from tables
        that are later synchronized from the staging area.
        """

        # Gather the tables to delete from the target catalogue
        tables_to_delete = find_cohort_references(self.get_schema_metadata(self.catalogue), self.table)

        cohort_ids = self._get_table_pkey_values()
        for table_id, ref_cols in tables_to_delete.items():
            # Iterate over the tables that reference the core table of the staging area
            # Check if any row matches this core table

            delete_rows = self._query_delete_rows(table_id, ref_cols, cohort_ids)

            if len(delete_rows) == 0:
                continue
            log.debug(f"Deleting in table {table_id!r} row(s) with primary keys {delete_rows.get(table_id)}.")

            # Delete the matching rows from the target catalogue table
            self._delete_table_entries(table_id=table_id,
                                       pkeys=delete_rows.get(table_id))

    def _query_delete_rows(self, table_id: str, ref_cols: str | list,
                           cohort_ids: list) -> dict:
        """Queries the rows to be deleted from a table."""
        schema_meta: Schema = self.get_schema_metadata(self.catalogue)
        query = self.__construct_pkey_query(schema_meta, table_id)
        if isinstance(ref_cols, str):
            ref_cols = [ref_cols]
        data = dict()
        for ref_col in ref_cols:
            variables = construct_delete_variables(self.get_schema_metadata(self.catalogue),
                                                   cohort_ids, table_id, ref_col)

            response = self.session.post(url=f"{self.url}/{self.catalogue}/graphql",
                                         json={"query": query, "variables": variables},
                                         headers={'x-molgenis-token': self.token})

            _data = response.json().get('data')
            for key, values in _data.items():
                if key in data.keys():
                    for row in values:
                        if row not in _data[key]:
                            data[key].append(row)
                else:
                    data[key] = values

        return data

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
                json={"query": query, "variables": variables},
                headers={'x-molgenis-token': self.token}
            )
            if response.status_code != 200:
                message = response.json().get('errors')[0].get('message')
                if 'is still referenced' in message:
                    log.error(f"Deleting entries from table {table_id} failed.")
                    log.error(message.split('Details: ')[-1])
                else:
                    log.error(response)
                    log.error(f"Deleting entries from table {table_id} failed.")
            else:
                log.info(response.json().get('data').get('delete').get('message'))

    def _cohorts_in_ref_array(self, _table_id: str) -> bool:
        """Returns True if cohorts are referenced in a referenced array in any column in this table."""
        try:
            _table_schema: Table = self.get_schema_metadata(self.catalogue).get_table(by='id', value=_table_id)
        except ValueError:
            raise NoSuchTableException(f"No table with id {_table_id!r} in schema {self.catalogue!r}.")
        return len(_table_schema.get_columns(by=['columnType', 'refTableId'], value=['REF_ARRAY', self.table])) > 0

    def _verify_schemas(self):
        """Ensures the staging area and catalogue are available."""
        if self.staging_area is not None:
            if self.staging_area not in self.schema_names:
                raise NoSuchSchemaException(f"Schema {self.staging_area!r} not found on server."
                                            f" Available schemas: {', '.join(self.schema_names)}.")
        if self.catalogue not in self.schema_names:
            raise NoSuchSchemaException(f"Schema {self.catalogue!r} not found on server."
                                        f" Available schemas: {', '.join(self.schema_names)}.")

        if self.staging_area == self.catalogue:
            raise NoSuchSchemaException(f"Catalogue schema must be different from staging area schema.")

    def _create_upload_zip(self) -> BytesIO:
        """Combines the relevant tables of the staging area into a zipfile."""
        tables_to_sync = find_cohort_references(self.get_schema_metadata(self.staging_area), self.table)

        source_file_path = self._download_schema_zip(schema=self.staging_area, schema_type='source',
                                                     include_system_columns=False)

        upload_stream = BytesIO()

        with (zipfile.ZipFile(source_file_path, 'r') as source_archive,
              zipfile.ZipFile(upload_stream, 'w', zipfile.ZIP_DEFLATED, False) as upload_archive):
            for file_name in source_archive.namelist():
                if '_files/' in file_name:
                    upload_archive.writestr(file_name, BytesIO(source_archive.read(file_name)).getvalue())
                    continue
                elif (table_name := Path(file_name).stem) not in [*tables_to_sync.keys(), *self.extra_tables]:
                    continue

                _table = source_archive.read(file_name)
                if consent_val := has_statement_of_consent(table_name, self.get_schema_metadata(self.staging_area)):
                    _table = process_statement(table=_table, consent_val=consent_val)
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
            files={'file': (f"{BASE_DIR}/upload.zip", zip_stream.getvalue())},
            headers={'x-molgenis-token': self.token}
        )

        response_status = response.status_code
        if response.status_code != 200:
            log.error(f"Migration failed with error {response_status}:\n{str(response.text)}")
            log.error("Uploading fallback zip.")
            self._upload_fallback_zip()
        else:
            response_url = f"{self.url}{response.json().get('url')}"
            upload_status = self.session.get(response_url,
                                             headers={'x-molgenis-token': self.token}).json().get('status')
            while upload_status == 'RUNNING':
                time.sleep(2)
                upload_status = self.session.get(response_url,
                                                 headers={'x-molgenis-token': self.token}).json().get('status')
            upload_description = self.session.get(response_url,
                                                  headers={'x-molgenis-token': self.token}).json().get('description')

            if upload_status == 'ERROR':
                log.error(f"Migration failed, reason: {upload_description}.")
                log.debug(self.session.get(response_url, headers={'x-molgenis-token': self.token}).json())
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

    def _upload_fallback_zip(self):
        """Restores the catalogue to the state before deletion by uploading the downloaded target zip."""
        target_filename = f"{BASE_DIR}/target.zip"
        upload_stream = BytesIO()
        # Load the target.zip file as a stream
        with (zipfile.ZipFile(target_filename, 'r') as target_archive,
              zipfile.ZipFile(upload_stream, 'w', zipfile.ZIP_DEFLATED) as upload_archive):
            for file_name in target_archive.namelist():
                if not file_name.startswith('molgenis'):
                    upload_archive.writestr(file_name, BytesIO(target_archive.read(file_name)).getvalue())
        # Upload the stream
        self._upload_zip_stream(upload_stream, is_fallback=True)

    def _get_table_pkey_values(self):
        """Fetches the primary key values associated with the staging area's table."""

        staging_schema = self.get_schema_metadata(self.staging_area)

        # Query server for resource id
        pkeys = [prepare_pkey(schema=staging_schema, table_id=self.table, col_id=col)
                 for col in prepare_pkey(schema=staging_schema, table_id=self.table)]
        pkey_print = query_columns_string(pkeys, indent=2)
        query = """{{\n  {} {{\n  {}\n  }}\n}}""".format(self.table, pkey_print)

        staging_url = f"{self.url}/{self.staging_area}/graphql"
        response = self.session.post(url=staging_url,
                                     headers={'x-molgenis-token': self.token},
                                     json={"query": query})
        response_data = response.json().get('data')
        if response_data is None:
            # Raise new error
            raise NoSuchTableException(f"Table {self.table!r} not found on schema {self.staging_area!r}.")

        # Return only if there is exactly one id/cohort in the Resources table
        if self.table in response_data.keys():
            if len(response_data[self.table]) < 1:
                raise ValueError(
                    f"Expected a value in table {self.table!r} in staging area {self.staging_area!r}"
                    f" but found {len(response_data[self.table])!r}"
                )
        else:
            raise ValueError(
                f"Expected a value in table {self.table!r} in staging area {self.staging_area!r}"
                f" but found none."
            )

        return [resource[pkeys[0]] for resource in response_data[self.table]]

    def last_change(self, staging_area: str = None) -> datetime | None:
        """Retrieves the datetime of the latest change made on the staging area.
        Returns None if the changelog is disabled or empty.
        """
        staging_area = staging_area or self.staging_area

        response = self.session.post(url=f"{self.url}/{staging_area}/settings/graphql",
                                         json={"query": changelog_query}, headers=self.session.headers)
        changelog = response.json().get('data').get('_changes')
        if len(changelog) == 0:
            return None
        change_date_str = changelog[0].get('stamp')
        change_datetime = datetime.strptime(change_date_str, '%Y-%m-%d %H:%M:%S.%f')

        return change_datetime

    @staticmethod
    def _cleanup():
        """Deletes the downloaded files after successful migration."""
        zip_files = ['target.zip', 'source.zip', 'upload.zip']
        for zp in zip_files:
            filename = f"{BASE_DIR}/{zp}"
            if Path(filename).exists():
                log.debug(f"Deleting file {zp!r}.")
                Path(filename).unlink()

    @staticmethod
    def __construct_pkey_query(db_schema: Schema, table_id: str, all_columns: bool = False):
        """Constructs a GraphQL query for finding the primary key values in a table."""
        table_schema: Table = db_schema.get_table(by='id', value=table_id)
        if all_columns:
            pkeys = [prepare_pkey(db_schema, table_id, col.id) for col in table_schema.columns]
            pkeys = [pk for pk in pkeys if pk is not None]
        else:
            pkeys = [prepare_pkey(db_schema, table_id, col.id) for col in table_schema.get_columns(by='key', value=1)]
        table_id = table_schema.id
        pkeys_print = query_columns_string(pkeys, indent=4)
        _query = (f"query {table_id}($filter: {table_id}Filter) {{\n"
                  f"  {table_id}(filter: $filter) {{\n"
                  f"{pkeys_print}\n"
                  f"  }}\n"
                  f"}}")
        return _query
