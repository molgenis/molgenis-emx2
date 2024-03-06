import logging
import time
import zipfile
from io import BytesIO
from pathlib import Path
from typing import TypeAlias, Literal

from molgenis_emx2_pyclient import Client
from molgenis_emx2_pyclient.exceptions import NoSuchSchemaException, NoSuchTableException

from tools.staging_migrator.src.molgenis_emx2_staging_migrator.constants import BASE_DIR
from tools.staging_migrator.src.molgenis_emx2_staging_migrator.graphql_queries import Queries
from tools.staging_migrator.src.molgenis_emx2_staging_migrator.utils import find_cohort_references, \
    construct_delete_variables, has_statement_of_consent, \
    process_statement, prepare_pkey, query_columns_string

log = logging.getLogger('Molgenis EMX2 Migrator')

SchemaType: TypeAlias = Literal['source', 'target']


class StagingMigrator(Client):
    """
    The StagingMigrator class is used to migrate updated data in a staging area to a catalogue.
    The class subclasses the Molgenis EMX2 Pyclient to access the API on the server
    """

    def __init__(self, url: str,
                 staging_area: str = None,
                 catalogue: str = 'catalogue',
                 table: str = 'Cohorts', token: str = None):
        """Sets up the StagingMigrator by logging in to the client."""
        super().__init__(url=url, token=token)
        self.staging_area = staging_area
        self.catalogue = catalogue
        self.table = table

    def __repr__(self):
        class_name = type(self).__name__
        args = [
            f"staging_area={self.staging_area!r}",
            f"catalogue={self.catalogue!r}",
            f"table={self.table!r}"
        ]
        return f"{class_name}({', '.join(args)})"

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

        # Synchronize the organisations with SharedStaging
        self.sync_shared_staging()

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
        if Path(filename).exists():
            Path(filename).unlink()

        api_zip_url = f"{self.url}/{schema}/api/zip"
        if include_system_columns:
            api_zip_url += '?includeSystemColumns=true'
        resp = self.session.get(api_zip_url,
                                headers={'x-molgenis-token': self.token},
                                allow_redirects=True)

        if resp.content:
            Path(filename).write_bytes(resp.content)
            log.debug(f"Downloaded {schema_type!r} schema to {filename!r}.")
        else:
            log.error("Error: download failed.")
        return filename

    def _delete_staging_from_catalogue(self):
        """
        Prepares the staging area by deleting data from tables
        that are later synchronized from the staging area.
        """

        # Gather the tables to delete from the target catalogue
        tables_to_delete = find_cohort_references(self._get_schema_metadata(self.catalogue),
                                                  self.catalogue, self.table)

        cohort_ids = self._get_table_pkey_values()
        metadata = self._get_schema_metadata(self.catalogue)
        for table_name, ref_col in tables_to_delete.items():
            # Iterate over the tables that reference the core table of the staging area
            # Check if any row matches this core table
            table_meta, = [_t for _t in metadata['tables'] if _t.get('name') == table_name]
            table_id = table_meta.get('id')

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

    def sync_shared_staging(self):
        """Synchronizes the records in the SharedStaging schema that are referenced
        from the staging area with the relevant records in the catalogue.
        """
        staging_schema = self._get_schema_metadata(self.staging_area)
        # Collect the tables in which a column references a table in the SharedStaging schema
        ss_ref_tables = [_t for _t in staging_schema['tables']
                         if any(map(lambda c: c.get('refSchemaName') == 'SharedStaging', _t['columns'])) and _t.get(
                'schemaName') == self.staging_area]
        if len(ss_ref_tables) == 0:
            return

        organisations = set()
        for table in ss_ref_tables:
            ref_cols = [col for col in table['columns'] if col.get('refSchemaName') == 'SharedStaging']
            ref_col_strs = [f"{rc['id']} {{\n      id\n    }}" for rc in ref_cols]
            query = """{{\n  {} {{\n    {}\n  }}\n}}""".format(
                table['id'], "    \n    ".join(ref_col_strs))
            response = self.session.post(url=f"{self.url}/{self.staging_area}/graphql",
                                         json={"query": query},
                                         headers={'x-molgenis-token': self.token})
            data_response = response.json().get('data')
            if len(data_response) > 0:
                for record in data_response.values():
                    for column in list(record[0].values())[0]:
                        for entry in column.values():
                            organisations.add(entry)
        organisations = list(organisations)
        if len(organisations) < 1:
            return
        # TODO combine the ids of Organisations in the responses
        shared_schema = self._get_schema_metadata('SharedStaging')
        search_query = self.__construct_pkey_query(shared_schema, 'Organisations', all_columns=True)
        search_variables = construct_delete_variables(shared_schema, organisations, 'Organisations', 'id')
        search_response = self.session.post(url=f"{self.url}/SharedStaging/graphql",
                                            json={"query": search_query, "variables": search_variables},
                                            headers={'x-molgenis-token': self.token})

        mutation_query = "mutation insert($value: [OrganisationsInput]){insert(Organisations:$value){message}}"
        mutation_variables = {"value": search_response.json().get('data').get('Organisations')}

        mutation_response = self.session.post(url=f"{self.url}/{self.catalogue}/graphql",
                                              json={"query": mutation_query, "variables": mutation_variables},
                                            headers={'x-molgenis-token': self.token})

        if mutation_response.status_code != 200:
            log.error("Unable to synchronize SharedStaging.")
        else:
            log.info(f"Successfully migrated organisations {', '.join(organisations)} to catalogue.")

    def _query_delete_rows(self, table_name: str, ref_col: str,
                           cohort_ids: list) -> dict:
        """Queries the rows to be deleted from a table."""
        schema_meta = self._get_schema_metadata(self.catalogue)
        query = self.__construct_pkey_query(schema_meta, table_name)
        variables = construct_delete_variables(self._get_schema_metadata(self.catalogue),
                                               cohort_ids, table_name, ref_col)

        response = self.session.post(url=f"{self.url}/{self.catalogue}/graphql",
                                     json={"query": query, "variables": variables},
                                     headers={'x-molgenis-token': self.token})
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
                json={"query": query, "variables": variables},
                headers={'x-molgenis-token': self.token}
            )
            if response.status_code != 200:
                log.error(response)
                log.error(f"Deleting entries from table {table_id} failed.")

    def _cohorts_in_ref_array(self, _table_name: str) -> bool:
        """Returns True if cohorts are referenced in a referenced array in any column in this table."""
        try:
            _table_schema, = [_t for _t in self._get_schema_metadata(self.catalogue)['tables'] if
                              _t.get('name') == _table_name]
        except ValueError:
            raise NoSuchTableException(f"No table {_table_name!r} in schema {self.catalogue!r}.")
        return (len([col for col in _table_schema['columns']
                     if ((col.get('columnType') == 'REF_ARRAY') & (col.get('refTable') == 'Cohorts'))]) > 0)

    def _verify_schemas(self):
        """Ensures the staging area and catalogue are available."""
        if self.staging_area is not None:
            if self.staging_area not in self.schema_names:
                raise NoSuchSchemaException(f"Schema '{self.staging_area}' not found on server."
                                            f" Available schemas: {', '.join(self.schema_names)}.")
        if self.catalogue not in self.schema_names:
            raise NoSuchSchemaException(f"Schema '{self.catalogue}' not found on server."
                                        f" Available schemas: {', '.join(self.schema_names)}.")

    def _create_upload_zip(self) -> BytesIO:
        """Combines the relevant tables of the staging area into a zipfile."""
        tables_to_sync = find_cohort_references(self._get_schema_metadata(self.staging_area),
                                                self.staging_area, self.table)

        source_file_path = self._download_schema_zip(schema=self.staging_area, schema_type='source',
                                                     include_system_columns=False)

        upload_stream = BytesIO()

        with (zipfile.ZipFile(source_file_path, 'r') as source_archive,
              zipfile.ZipFile(upload_stream, 'w', zipfile.ZIP_DEFLATED, False) as upload_archive):
            for file_name in source_archive.namelist():
                if '_files/' in file_name:
                    upload_archive.writestr(file_name, BytesIO(source_archive.read(file_name)).getvalue())
                    continue
                elif (table_name := Path(file_name).stem) not in tables_to_sync.keys():
                    continue

                _table = source_archive.read(file_name)
                if consent_val := has_statement_of_consent(table_name, self._get_schema_metadata(self.staging_area)):
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
            files={'file': (f'{BASE_DIR}/upload.zip', zip_stream.getvalue())},
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

        # Query server for cohort id
        query = """{{\n  {} {{\n    id\n    name\n  }}\n}}""".format(self.table)
        staging_url = f"{self.url}/{self.staging_area}/graphql"
        response = self.session.post(url=staging_url,
                                     headers={'x-molgenis-token': self.token},
                                     json={"query": query})
        response_data = response.json().get('data')
        if response_data is None:
            # Raise new error
            raise NoSuchTableException(f"Table '{self.table}' not found on schema '{self.staging_area}'.")

        # Return only if there is exactly one id/cohort in the Cohorts table
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

        return [cohort['id'] for cohort in response_data[self.table]]

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
    def __construct_pkey_query(db_schema: dict, table_name: str, all_columns: bool = False):
        """Constructs a GraphQL query for finding the primary key values in a table."""
        table_schema, = [_t for _t in db_schema['tables'] if _t.get('name') == table_name]
        if all_columns:
            pkeys = [prepare_pkey(db_schema, table_name, col['id']) for col in table_schema['columns']]
            pkeys = [pk for pk in pkeys if pk is not None]
        else:
            pkeys = [prepare_pkey(db_schema, table_name, col['id']) for col in table_schema['columns'] if
                     col.get('key') == 1]
        table_id = table_schema['id']
        pkeys_print = query_columns_string(pkeys, indent=4)
        _query = (f"query {table_id}($filter: {table_id}Filter) {{\n"
                  f"  {table_id}(filter: $filter) {{\n"
                  f"{pkeys_print}\n"
                  f"  }}\n"
                  f"}}")
        return _query
