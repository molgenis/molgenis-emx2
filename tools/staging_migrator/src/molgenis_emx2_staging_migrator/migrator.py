import logging
import time
import zipfile
from datetime import datetime
from io import BytesIO
from pathlib import Path

import numpy as np
import pandas as pd
from molgenis_emx2_pyclient import Client
from molgenis_emx2_pyclient.exceptions import NoSuchSchemaException, NoSuchTableException, NoSuchColumnException
from molgenis_emx2_pyclient.metadata import Table

from .constants import BASE_DIR, changelog_query, SchemaType
from .utils import prepare_primary_keys, process_statement, resource_ref_cols, load_table, \
    set_all_delete, check_hri_core

log = logging.getLogger('Molgenis EMX2 Migrator')

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

        self.source = None
        self.resource_ids = None
        if catalogue is not None:
            log.warning("Parameter 'catalogue' is deprecated, use 'target' instead.")
            self.target = catalogue
        else:
            self.target = target
        if staging_area is not None:
            log.warning("Parameter 'staging_area' is deprecated, use 'source' instead.")
            self.set_source(staging_area)
        elif source is not None:
            self.set_source(source)
        self._verify_schemas()
        self.warnings = []
        self.errors = []

    def __repr__(self):
        class_name = type(self).__name__
        args = [
            f"source={self.source!r}",
            f"target={self.target!r}"
        ]
        return f"{class_name}({', '.join(args)})"

    def set_staging_area(self, staging_area: str):
        log.warning("Method 'set_staging_area' is deprecated, use 'set_target' instead.")

        return self.set_source(staging_area)

    def set_source(self, source: str):
        """Sets the source schema and verifies its existence."""
        self.source = source
        self._verify_schemas()
        self.resource_ids = self.get_resource_ids()

    def set_catalogue(self, catalogue: str):
        log.warning("Method 'set_catalogue' is deprecated, use 'set_target' instead.")
        return self.set_target(catalogue)

    def set_target(self, target: str):
        """Sets the target schema and verifies its existence."""
        self.target = target
        self._verify_schemas()

    def get_resource_ids(self):
        """
        Fetches the identifiers of the resources in the source schema.
        """
        try:
            return self.get(table="Resources", schema=self.source, as_df=True)["id"].to_list()
        except KeyError:
            raise NoSuchColumnException(f"Table 'Resources' in schema {self.source!r} has no column 'id'.")

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

        source_profile = self._get_source_profile()
        source_metadata = self.get_schema_metadata(self.source)
        upload_stream = BytesIO()
        updated_tables = list()
        with (zipfile.ZipFile(source_file_path, 'r') as source_archive,
              zipfile.ZipFile(upload_stream, 'w', zipfile.ZIP_DEFLATED, False) as upload_archive):
            for file_name in sorted(source_archive.namelist()):

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

                if source_profile in ["CohortStaging", "UMCGCohortsStaging"]:
                    if table.id == "Organisations":
                        updated_table = self.process_organisations(updated_table)
                    if table.id == "Contacts":
                        updated_table = process_statement(updated_table)
                    if table.id in ["CollectionEvents", "Subpopulations"]:
                        updated_table = self._copy_resource_columns(updated_table)
                    if table.id == "Resources":
                        try:
                            check_hri_core(updated_table)
                        except ValueError as ve:
                            self.errors.append(ve)
                            raise ValueError(ve)

                if len(updated_table.index) != 0:
                    upload_archive.writestr(file_name, updated_table.to_csv(index=False))
                    updated_tables.append(Path(file_name).stem)

        # Return zip
        if len(updated_tables) == 0:
            log.info(f"No data to migrate.")
            upload_stream.flush()
            return upload_stream
        log.info(f"Migrating tables {', '.join(updated_tables)}.")

        filepath = BASE_DIR.joinpath(f"update.zip")
        if Path(filepath).exists():
            Path(filepath).unlink()
        Path(filepath).write_bytes(upload_stream.getbuffer())
        return upload_stream

    def delete_resource(self):
        """Deletes the contents of the source schema from the target schema."""

        # Check if software supports deletion through import
        if self.version < "13.8.0":
            raise NotImplementedError("The delete functionality is not implemented for EMX2 "
                                      "software running a version below 13.8.0")
        source_file_path = self.download_schema_zip(schema=self.source, schema_type='source',
                                                    include_system_columns=True)

        source_metadata = self.get_schema_metadata(self.source)
        upload_stream = BytesIO()
        updated_tables = list()
        with (zipfile.ZipFile(source_file_path, 'r') as source_archive,
              zipfile.ZipFile(upload_stream, 'w', zipfile.ZIP_DEFLATED, False) as upload_archive):
            for file_name in source_archive.namelist():
                try:
                    table: Table = source_metadata.get_table('name', Path(file_name).stem)
                except NoSuchTableException:
                    log.debug(f"Skipping file {file_name!r}.")
                    continue
                log.debug(f"Preparing table {table.name!r} for deletion.")
                updated_table: pd.DataFrame = set_all_delete(table)
                if len(updated_table.index) != 0:
                    upload_archive.writestr(file_name, updated_table.to_csv(index=False))
                    updated_tables.append(Path(file_name).stem)

        if len(updated_tables) == 0:
            upload_stream.flush()
            return

        self.upload_zip_stream(upload_stream)
        self.cleanup()

    def _get_filtered(self, table: Table) -> pd.DataFrame:
        """
        Filters the table for rows in present in the source schema
        that have not been updated or published yet in the target schema.
        """
        # Specify the primary keys
        primary_keys = prepare_primary_keys(self.get_schema_metadata(self.source), table.name)

        # Find columns that reference 'Resources'
        ref_cols = resource_ref_cols(self.get_schema_metadata(self.source), table.name)

        # Load the data for the table from the ZIP files
        source_df = load_table('source', table)
        target_df = load_table('target', table)

        # Filter the rows in the target table that reference the Resource identifiers
        target_df = target_df.loc[target_df[ref_cols].isin(self.resource_ids).any(axis=1)]

        # Return if both tables are empty
        if len(source_df.index) + len(target_df.index) == 0:
            return source_df

        # Skip drafts from the upload
        if "mg_draft" in source_df.columns:
            source_df = source_df.loc[~source_df["mg_draft"].replace({np.nan: False})]

        # Create mapping of indices from the source table to the target table
        merge_df = source_df.reset_index().merge(target_df.reset_index(), on=primary_keys)

        # Filter rows not present in the target's table
        new_df = source_df.loc[~source_df.index.isin(merge_df["index_x"])].copy()

        # Filter rows not present in the source's table
        missing_df = target_df.loc[~target_df.index.isin(merge_df["index_y"])].copy()
        missing_df["mg_delete"] = 'true'

        # Filter updated rows
        merge_df = merge_df.loc[merge_df["mg_updatedOn_x"] > merge_df["mg_updatedOn_y"]]
        updated_df = source_df.iloc[merge_df["index_x"]]

        # Combine the new, updated and missing rows
        filtered_df = pd.concat([new_df, updated_df, missing_df])
        filtered_df = filtered_df[[col for col in filtered_df.columns if (not col.startswith('mg_') or col == 'mg_delete')]]

        return filtered_df

    def process_organisations(self, source_orgs: pd.DataFrame) -> pd.DataFrame:
        """Processes the organisations table by combining information from CatalogueOntologies."""
        ontology_organisations = self.get("Organisations", schema="CatalogueOntologies", as_df=True)

        def pid_func(org: str):
            return ontology_organisations.set_index('name')["code"].to_dict().get(org, None)
        def website_func(org: str):
            return ontology_organisations.set_index('name')["website"].to_dict().get(org, None)

        missing_orgs = source_orgs.loc[source_orgs["organisation"].isna(), ["resource", "id"]]
        for row in missing_orgs.itertuples():
            msg = f"No organisation for (resource, id) = ({row.resource}, {row.id})"
            log.warning(msg)
            self.warnings.append(msg)

        target_orgs = source_orgs.copy()
        target_orgs["organisation name"] = target_orgs["organisation"].copy()
        target_orgs["organisation pid"] = target_orgs["organisation"].apply(pid_func)
        target_orgs["organisation website"] = target_orgs["organisation"].apply(website_func)

        return target_orgs


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
                error_msg = f"Migration failed, reason: {upload_description}."
                log.error(error_msg)
                self.errors.append(error_msg)
                log.debug(self.session.get(response_url).json())
            else:
                log.info("Upload completed successfully.")


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

    def _copy_resource_columns(self, table_df: pd.DataFrame) -> pd.DataFrame:
        """Inserts values for columns 'publisher', 'creator', 'contact point' from Resources into this table."""
        resources = load_table('source', self.get_schema_metadata(self.source).get_table('name', 'Resources'))
        cols = ["publisher", "creator", "contact point"]
        for rc in resources.columns:
            if any(map(lambda c: rc.startswith(c), cols)):
                table_df[rc] = table_df["resource"].apply(lambda r: resources.loc[resources['id'] == r, rc])

        return table_df

    def _get_source_profile(self) -> str | None:
        """Returns the profile(s) of the source, defaults to None."""
        source_meta = self.get_schema_metadata(self.source)
        if "Profiles" not in map(lambda t: t.id, source_meta.tables):
            return None
        try:
            return source_meta.get_table('id', "Profiles").descriptions[0].get('value')
        except AttributeError:
            return None
