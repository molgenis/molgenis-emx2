from typing import List

from molgenis.bbmri_eric.bbmri_client import EricSession, ExternalServerSession
from molgenis.bbmri_eric.errors import EricError, EricWarning, requests_error_handler
from molgenis.bbmri_eric.model import ExternalServerNode, NodeData, TableType
from molgenis.bbmri_eric.printer import Printer


class Stager:
    """
    This class is responsible for copying data from a node with an external server to
    its staging area in the BBMRI ERIC directory.
    """

    def __init__(self, session: EricSession, printer: Printer):
        self.session = session
        self.printer = printer

        self.warnings: List[EricWarning] = list()

    @requests_error_handler
    def stage(self, node: ExternalServerNode):
        """
        Stages all data from the provided external node in the BBMRI-ERIC directory.
        """
        self.warnings = []
        source_data = self._get_source_data(node)
        self._clear_staging_area(node)
        self._import_node(source_data)

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
        package = f"eu_bbmri_eric_{session.node.code}"
        if not session.get("sys_md_Package", q=f"id=={package}"):
            raise EricError(
                "The session user has invalid permissions\n       Please check the "
                "token and permissions of this user"
            )

    def _check_tables(self, session: ExternalServerSession):
        """
        Check if all tables are available on the external server
        """
        for table_type in TableType.get_import_order():
            id_ = session.node.get_staging_id(table_type)
            if not session.get("sys_md_EntityType", q=f"id=={id_}"):
                warning = EricWarning(
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
            self.session.delete(node.get_staging_id(table_type))

    def _import_node(self, source_data: NodeData):
        """
        Imports an external node's data to its staging area.
        """
        self.printer.print(
            f"💾 Saving data to the staging area of {source_data.node.code}"
        )

        self.session.upload_data(source_data.convert_to_staging())
