"""Defines the Stager class, which implements all staging-related functionality"""
import csv
import os
from typing import List
from urllib.error import ContentTooShortError, HTTPError, URLError
from urllib.parse import urljoin
from urllib.request import urlretrieve

from molgenis_emx2_pyclient.metadata import Column

from .directory_client import (
    DirectorySession,
    ExternalServerSession,
    NoSuchTableException,
)
from .errors import DirectoryError, DirectoryWarning, requests_error_handler
from .model import (
    ExternalServerNode,
    FileIngestNode,
    NodeData,
    Source,
    Table,
    TableMeta,
    TableType,
)
from .printer import Printer


class Stager:
    """
    This class is responsible for copying data from a node with an external server to
    its staging area in the BBMRI Biobank Directory.
    """

    def __init__(self, session: DirectorySession, printer: Printer):
        self.session = session
        self.printer = printer

        self.warnings: List[DirectoryWarning] = list()

    @requests_error_handler
    async def stage(self, node: ExternalServerNode | FileIngestNode):
        """
        Stages all data from the provided external node in the Directory.
        """
        self.warnings = []
        if isinstance(node, ExternalServerNode):
            source_data = self._get_external_server_data(node)
        else:
            source_data = self._ingest_files(node)
        self._clear_staging_area(node)
        await self._import_node(source_data)

        return self.warnings

    def _get_external_server_data(self, node: ExternalServerNode) -> NodeData:
        """
        Gets a node's data from an external server.
        First check if:
        - the session has the right permissions
        - and if all tables are available
        """
        self.printer.print(f"📦 Retrieving node's data from {node.url}")
        source_session = ExternalServerSession(node=node)
        self._check_permissions(source_session)
        self._check_tables(source_session)
        return source_session.get_node_data()

    def _ingest_files(self, node: FileIngestNode) -> NodeData:
        """
        Gets a node's data from the files on an external file ingest server.

        :return a NodeData object
        """
        self.printer.print(f"📦 Retrieving node's data from {node.url}")
        tables = {}
        for table_type in TableType.get_import_order():
            file = self._download_file(node, table_type)
            if not file or os.path.getsize(file) == 0:
                # No file or empty file? Create dummy Table
                dummy_meta = [Column(name='id', key=1, table=table_type.base_id)]
                table = Table.of_empty(
                    table_type,
                    TableMeta(meta=dummy_meta, table_name=table_type.base_id),
                )
            else:
                table = self._file_to_table(file, table_type)
            tables[table_type.value] = table

        return NodeData.from_dict(
            node=node, source=Source.EXTERNAL_SERVER, tables=tables
        )

    @staticmethod
    def _check_permissions(session: ExternalServerSession):
        """
        Check if the session has the necessary permissions
        """
        # In stead of this method try except around source_session with schema parameter
        schemas = [schema.name for schema in session.schemas]
        if session.node.get_schema_id() not in schemas:
            raise DirectoryError(
                "The session user has invalid permissions\n       Please check the "
                "token and permissions of this user"
            )

    def _check_tables(self, session: ExternalServerSession):
        """
        Check if all tables are available on the external server
        """
        schema_meta = session.get_schema_metadata(session.node.get_schema_id())
        for table_type in TableType.get_import_order():
            id_ = session.node.get_staging_id(table_type)
            try:
                schema_meta.get_table(by="name", value=id_)
            except NoSuchTableException:
                warning = DirectoryWarning(
                    f"Node {session.node.code} has no {table_type.value} table"
                )
                self.printer.print_warning(warning, indent=1)
                self.warnings.append(warning)

    def _clear_staging_area(self, node: ExternalServerNode):
        """
        Deletes all data in the staging area of an external node.
        """
        self.printer.print(f"🔥 Clearing staging area of {node.code}")
        for table_type in reversed(TableType.get_import_order()):
            # If truncate table is available this workaround can be removed
            df_data = self.session.get(
                schema=node.get_schema_id(),
                table=node.get_staging_id(table_type),
                as_df=True,
            )
            if len(df_data) > 0:
                ids = df_data["id"].to_frame().to_dict("records")
                self.session.delete_records(
                    schema=node.get_schema_id(),
                    table=node.get_staging_id(table_type),
                    data=ids,
                )

    @staticmethod
    def _file_to_table(filename: str, table_type: TableType) -> Table:
        """
        Get csv file, transform into Table object
        """
        rows = []
        with open(filename, 'r', newline='', encoding='utf-8') as file:
            reader = csv.DictReader(file)
            for row in reader:
                rows.append(row)
        # Create table metadata
        metadata = []
        for column in rows[0]:
            if column == 'id':
                column_meta = Column(name=column, key=1, table=table_type.base_id)
            else:
                column_meta = Column(name=column, key=None, table=table_type.base_id)
            metadata.append(column_meta)
        return Table.of(
            table_type=table_type,
            meta=TableMeta(meta=metadata, table_name=table_type.base_id),
            rows=rows,
        )

    def _download_file(self, node: FileIngestNode, table_type: TableType) -> str | None:
        """
        Download the .csv-file from the file ingest server
        """
        file_path = urljoin(f"{node.url}/", f"{table_type.base_id}.csv")
        try:
            filename, headers = urlretrieve(file_path)
        except (URLError, HTTPError, ContentTooShortError) as e:
            raise DirectoryError(f"Failed at retrieving {file_path}") from e
        if headers['Content-Type'] != 'text/csv; charset=utf-8':
            warning = DirectoryWarning(
                f"Node {node.code} has no file at {file_path} "
                f"for table {table_type.base_id}"
            )
            self.printer.print_warning(warning, indent=1)
            self.warnings.append(warning)
            return None
        return filename

    async def _import_node(self, source_data: NodeData):
        """
        Imports an external node's data to its staging area.
        """
        self.printer.print(
            f"💾 Saving data to the staging area of {source_data.node.code}"
        )

        await self.session.upload_data(
            schema=source_data.node.get_schema_id(),
            data=source_data.convert_to_staging(),
        )
