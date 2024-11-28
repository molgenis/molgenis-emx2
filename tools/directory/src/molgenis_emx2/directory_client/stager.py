from typing import List

from .directory_client import (
    DirectorySession,
    ExternalServerSession,
    NoSuchTableException,
)
from .errors import DirectoryError, DirectoryWarning, requests_error_handler
from .model import ExternalServerNode, NodeData, TableType
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
    async def stage(self, node: ExternalServerNode):
        """
        Stages all data from the provided external node in the Directory.
        """
        self.warnings = []
        source_data = self._get_source_data(node)
        self._clear_staging_area(node)
        await self._import_node(source_data)

        return self.warnings

    def _get_source_data(self, node: ExternalServerNode) -> NodeData:
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
