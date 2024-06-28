from typing import List, Optional

from molgenis_emx2.directory_client.directory_client import (
    AttributesRequest,
    DirectorySession,
)
from molgenis_emx2.directory_client.errors import (
    DirectoryError,
    ErrorReport,
    requests_error_handler,
)
from molgenis_emx2.directory_client.model import ExternalServerNode, Node
from molgenis_emx2.directory_client.pid_manager import PidManagerFactory
from molgenis_emx2.directory_client.pid_service import BasePidService
from molgenis_emx2.directory_client.printer import Printer
from molgenis_emx2.directory_client.publication_preparer import PublicationPreparer
from molgenis_emx2.directory_client.publisher import Publisher, PublishingState
from molgenis_emx2.directory_client.stager import Stager


class Directory:
    """
    Main class for doing operations on the BBMRI Biobank Directory.
    """

    def __init__(
        self, session: DirectorySession, pid_service: Optional[BasePidService] = None
    ):
        """
        :param session: an authenticated session with a BBMRI Biobank Directory.
        :param pid_service: a configured PidService, required for publishing. When no
        PidService is provided, nodes can only be staged.
        """
        self.session = session
        self.printer = Printer()
        self.stager = Stager(self.session, self.printer)
        self.pid_service: Optional[BasePidService] = pid_service
        if pid_service:
            self.pid_manager = PidManagerFactory.create(self.pid_service, self.printer)
            self.preparator = PublicationPreparer(
                self.printer, self.pid_manager, self.session
            )
            self.publisher = Publisher(self.session, self.printer, self.pid_manager)

    def stage_external_nodes(self, nodes: List[ExternalServerNode]) -> ErrorReport:
        """
        Stages all data from the provided external nodes in the BBMRI Biobank Directory.

        Parameters:
            nodes (List[ExternalServerNode]): The list of external nodes to stage
        """
        report = ErrorReport(nodes)
        for node in nodes:
            self.printer.print_node_title(node)
            try:
                self._stage_node(node, report)
            except DirectoryError as e:
                self.printer.print_error(e)
                report.add_node_error(node, e)

        self.printer.print_summary(report)
        return report

    def publish_nodes(self, nodes: List[Node]) -> ErrorReport:
        """
        Publishes data from the provided nodes to the tables in the Directory.

        Parameters:
            nodes (List[Node]): The list of nodes to publish
        """
        if not self.pid_service:
            raise ValueError("A PID service is required to publish nodes")

        report = ErrorReport(nodes)
        try:
            state = self._init_state(nodes, report)
        except DirectoryError as e:
            self.printer.print_error(e)
            report.set_global_error(e)
        else:
            self._prepare_nodes(nodes, state)
            self._publish_nodes(state)

        self.printer.print_summary(report)
        return report

    @requests_error_handler
    def _init_state(self, nodes: List[Node], report: ErrorReport) -> PublishingState:
        self.printer.print_header("⚙️ Preparation")

        self.printer.print("📦 Retrieving existing published data")
        published_data = self.session.get_published_data(
            nodes,
            AttributesRequest(
                persons=["id", "national_node"],
                networks=["id", "national_node"],
                also_known_in=["id", "national_node"],
                biobanks=["id", "pid", "name", "national_node", "withdrawn"],
                collections=["id", "national_node"],
                facts=["id", "national_node"],
            ),
        )

        self.printer.print("📦 Retrieving quality information")
        quality_info = self.session.get_quality_info()

        self.printer.print("📦 Retrieving data of node EU")
        eu_node_data = self.session.get_staging_node_data(self.session.get_node("EU"))

        self.printer.print("📦 Retrieving disease ontologies")
        diseases = self.session.get_ontology(
            "DiseaseTypes",
            matching_attrs=["exact_mapping", "ntbt_mapping"],
        )

        return PublishingState(
            existing_data=published_data,
            quality_info=quality_info,
            eu_node_data=eu_node_data,
            diseases=diseases,
            nodes=nodes,
            report=report,
        )

    def _prepare_nodes(self, nodes, state):
        for node in nodes:
            self.printer.print_node_title(node)
            try:
                if isinstance(node, ExternalServerNode):
                    self._stage_node(node, state.report)
                node_data = self.preparator.prepare(node, state)
                state.data_to_publish.merge(node_data)
            except DirectoryError as e:
                self.printer.print_error(e)
                state.existing_data.remove_node_rows(node)
                state.report.add_node_error(node, e)

    def _publish_nodes(self, state: PublishingState):
        self.printer.print_header(
            f"🎁 Publishing node{'s' if len(state.nodes) > 1 else ''}"
        )
        try:
            self.publisher.publish(state)
        except DirectoryError as e:
            self.printer.print_error(e)
            state.report.set_global_error(e)

    @requests_error_handler
    def _stage_node(self, node: ExternalServerNode, report: ErrorReport):
        self.printer.print(f"📥 Staging data of node {node.code}")
        with self.printer.indentation():
            warnings = self.stager.stage(node)
            if warnings:
                report.add_node_warnings(node, warnings)
