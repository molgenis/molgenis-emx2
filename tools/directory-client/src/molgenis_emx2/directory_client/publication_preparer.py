from molgenis.bbmri_eric.bbmri_client import EricSession
from molgenis.bbmri_eric.errors import ErrorReport, requests_error_handler
from molgenis.bbmri_eric.model import Node, NodeData
from molgenis.bbmri_eric.model_fitting import ModelFitter
from molgenis.bbmri_eric.pid_manager import BasePidManager
from molgenis.bbmri_eric.printer import Printer
from molgenis.bbmri_eric.publisher import PublishingState
from molgenis.bbmri_eric.transformer import Transformer
from molgenis.bbmri_eric.validation import Validator


class PublicationPreparer:
    """Prepares nodes for publishing."""

    def __init__(
        self, printer: Printer, pid_manager: BasePidManager, session: EricSession
    ):
        self.printer = printer
        self.pid_manager = pid_manager
        self.session = session

    @requests_error_handler
    def prepare(self, node: Node, state: PublishingState) -> NodeData:
        node_data = self._get_node_data(node)
        self._validate_node(node_data, state.report)
        self._fit_node_model(node_data, state.report)
        self._transform_node(node_data, state)
        self._manage_node_pids(node_data, state)
        return node_data

    def _validate_node(self, node_data: NodeData, report: ErrorReport):
        self.printer.print(f"🔎 Validating staged data of node {node_data.node.code}")
        with self.printer.indentation():
            warnings = Validator(node_data, self.printer).validate()
            if warnings:
                report.add_node_warnings(node_data.node, warnings)

    def _fit_node_model(self, node_data: NodeData, report: ErrorReport):
        self.printer.print(
            f"⟺ Align staged data of node {node_data.node.code} "
            f"with the published model"
        )
        with self.printer.indentation():
            warnings = ModelFitter(node_data, self.printer).fit_model()
            if warnings:
                report.add_node_warnings(node_data.node, warnings)

    def _transform_node(self, node_data: NodeData, state: PublishingState):
        self.printer.print("✏️ Preparing staged data for publishing")
        with self.printer.indentation():
            warnings = Transformer(
                node_data=node_data,
                quality=state.quality_info,
                printer=self.printer,
                existing_biobanks=state.existing_data.biobanks,
                eu_node_data=state.eu_node_data,
                diseases=state.diseases,
            ).transform()
            if warnings:
                state.report.add_node_warnings(node_data.node, warnings)

    def _manage_node_pids(self, node_data: NodeData, state: PublishingState):
        self.printer.print("🆔 Managing PIDs")
        with self.printer.indentation():
            warnings = self.pid_manager.assign_biobank_pids(node_data.biobanks)
            self.pid_manager.update_biobank_pids(
                node_data.biobanks, state.existing_data.biobanks
            )
            if warnings:
                state.report.add_node_warnings(node_data.node, warnings)

    def _get_node_data(self, node: Node) -> NodeData:
        self.printer.print(f"📦 Retrieving staged data of node {node.code}")
        return self.session.get_staging_node_data(node)
