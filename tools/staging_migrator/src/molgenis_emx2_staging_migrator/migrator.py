import logging
import time
import zipfile
from datetime import datetime
from io import BytesIO
from pathlib import Path
from typing import TypeAlias, Literal

import pandas as pd
from molgenis_emx2_pyclient import Client
from molgenis_emx2_pyclient.constants import DATE, DATETIME
from molgenis_emx2_pyclient.exceptions import NoSuchSchemaException, NoSuchTableException
from molgenis_emx2_pyclient.metadata import Table
from molgenis_emx2_pyclient.utils import convert_dtypes

from .constants import BASE_DIR, changelog_query
from .utils import prepare_primary_keys, has_statement_of_consent, process_statement

log = logging.getLogger('Molgenis EMX2 Migrator')

SchemaType: TypeAlias = Literal['source', 'target']
CATALOGUE = "catalogue"


class StagingMigrator(Client):
    """
    The StagingMigrator class is used to migrate updated data from a source schema to a target.
    The class subclasses the Molgenis EMX2 Pyclient to access the API on the server
    """

    def __init__(self, url: str,
                 source: str = None,
                 target: str = CATALOGUE,
                 staging_area: str = None,
                 catalogue: str = None,
                 token: str = None):
        """Sets up the StagingMigrator by logging in to the client."""
        super().__init__(url=url, token=token)

        if staging_area is not None:
            DeprecationWarning("Parameter 'staging_area' is deprecated, use 'source' instead.")
            self.source = staging_area
        else:
            self.source = source
        if catalogue is not None:
            DeprecationWarning("Parameter 'catalogue' is deprecated, use 'target' instead.")
            self.target = catalogue
        else:
            self.target = target
        self._verify_schemas()

    def __repr__(self):
        class_name = type(self).__name__
        args = [
            f"source={self.source!r}",
            f"target={self.target!r}"
        ]
        return f"{class_name}({', '.join(args)})"

    def set_staging_area(self, staging_area: str):
        DeprecationWarning("Method 'set_staging_area' is deprecated, use 'set_target' instead.")
        return self.set_source(staging_area)

    def set_source(self, source: str):
        """Sets the source schema and verifies its existence."""
        self.source = source
        self._verify_schemas()

    def set_catalogue(self, catalogue: str):
        DeprecationWarning("Method 'set_catalogue' is deprecated, use 'set_target' instead.")
        return self.set_target(catalogue)

    def set_target(self, target: str):
        """Sets the target schema and verifies its existence."""
        self.target = target
        self._verify_schemas()

    def migrate(self, keep_zips: bool = False):
        """Performs the migration of the source schema to the target schema."""

        # Download data from the target schema for upload in case of an error during execution
        self.download_schema_zip(schema=self.target, schema_type='target', include_system_columns=True)

        # Create zipfile for uploading
        zip_stream = self.create_zip()

        # Upload the zip to the target schema
        self.upload_zip_stream(zip_stream)

        if not keep_zips:
            # Remove any downloaded files from disk
            self.cleanup()

    def create_zip(self):
        """
        Creates a ZIP file containing tables to be uploaded to the target schema.
        """
        source_file_path = self.download_schema_zip(schema=self.source, schema_type='source',
                                                    include_system_columns=True)

        source_metadata = self.get_schema_metadata(self.source)
        upload_stream = BytesIO()
        updated_tables = list()
        with (zipfile.ZipFile(source_file_path, 'r') as source_archive,
              zipfile.ZipFile(upload_stream, 'w', zipfile.ZIP_DEFLATED, False) as upload_archive):
            for file_name in source_archive.namelist():

                # Add files in '_files' folder
                if '_files/' in file_name:
                    upload_archive.writestr(file_name, BytesIO(source_archive.read(file_name)).getvalue())
                    continue

                try:
                    table: Table = source_metadata.get_table('name', Path(file_name).stem)
                except NoSuchTableException:
                    log.debug(f"Skipping file {file_name!r}.")
                    continue
                log.debug(f"Processing table {table.name!r}.")
                updated_table: pd.DataFrame = self._get_filtered(table)
                modified_table: pd.DataFrame = self._modify_table(updated_table, table)
                if len(modified_table.index) != 0:
                    upload_archive.writestr(file_name, modified_table.to_csv())
                    updated_tables.append(Path(file_name).stem)

        # Return zip
        if len(updated_tables) == 0:
            log.info(f"No data to migrate.")
            upload_stream.flush()
            return upload_stream
        log.info(f"Migrating tables {', '.join(updated_tables)}.")
        return upload_stream

    def _get_filtered(self, table: Table) -> pd.DataFrame:
        """
        Filters the table for rows in present in the source schema
        that have not been updated or published yet in the target schema.
        """
        # Specify the primary keys
        primary_keys = prepare_primary_keys(self.get_schema_metadata(self.source), table.name)

        # Load the data for the table from the ZIP files
        source_df = self._load_table('source', table)
        target_df = self._load_table('target', table)

        if len(source_df.index) == 0:
            return source_df

        # if "mg_draft" in source_df.columns:
        #     source_df = source_df.loc[~source_df["mg_draft"]]

        # Create mapping of indices from the source table to the target table
        merge_df = source_df.reset_index().merge(target_df.reset_index(), on=primary_keys)

        # Filter rows not present in the target's table
        new_df = source_df.loc[~source_df.index.isin(merge_df["index_x"])].copy()

        # Filter updated rows
        merge_df = merge_df.loc[merge_df["mg_updatedOn_x"] > merge_df["mg_updatedOn_y"]]


        target_table = self.get_schema_metadata(self.target).get_table("id", table.id)
        if "issued" in map(lambda c: c.name, target_table.columns):
            new_df["issued"] = new_df["mg_insertedOn"]
        if "modified" in map(lambda c: c.name, target_table.columns):
            new_df["modified"] = new_df["mg_updatedOn"]

        updated_df = source_df.iloc[merge_df["index_x"]]
        if "modified" in map(lambda c: c.name, target_table.columns):
            updated_df["modified"] = updated_df["mg_updatedOn"]

        filtered_df = pd.concat([new_df, updated_df])

        return filtered_df

    @staticmethod
    def _modify_table(df: pd.DataFrame, table: Table) -> pd.DataFrame:
        """
        Applies transformation on a table's data given its contents.
        """
        if (consent_val := has_statement_of_consent(table)) != 0:
            return process_statement(df, consent_val=consent_val)
        return df


    def download_schema_zip(self, schema: str, schema_type: SchemaType,
                            include_system_columns: bool = True) -> Path:
        """Download target schema as zip, save in case upload fails."""
        filepath = BASE_DIR.joinpath(f"{schema_type}.zip")
        if Path(filepath).exists():
            Path(filepath).unlink()

        api_zip_url = f"{self.url}/{schema}/api/zip"
        if include_system_columns:
            api_zip_url += '?includeSystemColumns=true'
        resp = self.session.get(api_zip_url, allow_redirects=True)

        if resp.content:
            Path(filepath).write_bytes(resp.content)
            log.debug(f"Downloaded {schema_type!r} schema to {filepath!s}.")
        else:
            log.error("Error: download failed.")
        return filepath

    @staticmethod
    def _load_table(schema_type: SchemaType, table: Table) -> pd.DataFrame:
        """Loads the table from a zip file into a DataFrame.
        Then parses the data by converting the columns' dtypes.
        """
        with zipfile.ZipFile(BASE_DIR / f"{schema_type}.zip", 'r') as archive:
            raw_df = pd.read_csv(BytesIO(archive.read(f"{table.name}.csv")), nrows=1)

        columns = raw_df.columns
        dtypes = {c: t for (c, t) in convert_dtypes(table).items() if c in columns}

        bool_columns = [c for (c, t) in dtypes.items() if t == 'boolean']
        date_columns = [c.name for c in table.columns
                        if c.get('columnType') in (DATE, DATETIME) and c.name in columns]

        with zipfile.ZipFile(BASE_DIR / f"{schema_type}.zip", 'r') as archive:
            df = pd.read_csv(filepath_or_buffer=BytesIO(archive.read(f"{table.name}.csv")),
                             dtype=dtypes,
                             na_values=[""],
                             keep_default_na=False,
                             parse_dates=date_columns)

        df[bool_columns] = df[bool_columns].replace({'true': True, 'false': False})
        df = df.astype(dtypes)

        return df


    def _verify_schemas(self):
        """Ensures the source and target are available."""
        if self.source is not None:
            if self.source not in self.schema_names:
                raise NoSuchSchemaException(f"Schema {self.source!r} not found on server."
                                            f" Available schemas: {', '.join(self.schema_names)}.")
        if self.target not in self.schema_names:
            raise NoSuchSchemaException(f"Schema {self.target!r} not found on server."
                                        f" Available schemas: {', '.join(self.schema_names)}.")

        if self.source == self.target:
            raise NoSuchSchemaException(f"Target schema must be different from source schema.")


    def upload_zip_stream(self, zip_stream: BytesIO):
        """Uploads the zip file containing the tables from the source schema
        to the target schema.
        """
        upload_url = f"{self.url}/{self.target}/api/zip?async=true"

        response = self.session.post(
            url=upload_url,
            files={'file': (f"{BASE_DIR}/upload.zip", zip_stream.getvalue())}
        )

        response_status = response.status_code
        if response.status_code != 200:
            log.error(f"Migration failed with error {response_status}:\n{str(response.text)}")
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
            else:
                log.info("Migration process completed successfully.")


    def last_change(self, source: str = None) -> datetime | None:
        """Retrieves the datetime of the latest change made on the source schema.
        Returns None if the changelog is disabled or empty.
        """
        source = source or self.source

        response = self.session.post(url=f"{self.url}/{source}/settings/graphql",
                                         json={"query": changelog_query}, headers=self.session.headers)
        changelog = response.json().get('data').get('_changes')
        if len(changelog) == 0:
            return None
        change_date_str = changelog[0].get('stamp')
        change_datetime = datetime.strptime(change_date_str, '%Y-%m-%d %H:%M:%S.%f')

        return change_datetime

    @staticmethod
    def cleanup():
        """Deletes the downloaded files after successful migration."""
        zip_files = ['target.zip', 'source.zip', 'upload.zip']
        for zp in zip_files:
            filename = f"{BASE_DIR}/{zp}"
            if Path(filename).exists():
                log.debug(f"Deleting file {zp!r}.")
                Path(filename).unlink()
