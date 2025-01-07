from dataclasses import dataclass, field
from typing import List

from .directory_client import DirectorySession
from .errors import DirectoryError, DirectoryWarning, ErrorReport, MolgenisRequestError
from .model import (
    MixedData,
    Node,
    NodeData,
    OntologyTable,
    QualityInfo,
    Source,
    Table,
    TableType,
)
from .pid_manager import BasePidManager
from .printer import Printer


@dataclass
class PublishingState:
    existing_data: MixedData
    eu_node_data: NodeData
    quality_info: QualityInfo
    nodes: List[Node]
    report: ErrorReport
    diseases: OntologyTable
    data_to_publish: MixedData = field(init=False)

    def __post_init__(self):
        self.data_to_publish = self.existing_data.copy_empty()
        self.data_to_publish.source = Source.TRANSFORMED


class Publisher:
    """
    This class is responsible for copying data from the staging areas to the combined
    public tables.
    """

    def __init__(
        self,
        session: DirectorySession,
        printer: Printer,
        pid_manager: BasePidManager,
    ):
        self.session = session
        self.printer = printer
        self.pid_manager = pid_manager

    async def publish(self, state: PublishingState):
        """
        Copies staging data to the combined tables. This happens in two phases:
        1. New/existing rows are upserted in the Directory tables
        2. Removed rows are deleted from the Directory tables
        """
        self.printer.print("💾 Saving new and updated data to Directory tables")
        with self.printer.indentation():
            await self._upsert_data(state)

        self.printer.print("🧼 Cleaning up removed data in Directory tables")
        with self.printer.indentation():
            self._delete_data(state)

    async def _upsert_data(self, state):
        try:
            await self.session.upload_data(
                schema=self.session.directory_schema, data=state.data_to_publish
            )
        except MolgenisRequestError as e:
            raise DirectoryError("Error importing data to combined tables") from e

    def _delete_data(self, state):
        for table in reversed(state.data_to_publish.import_order):
            try:
                with self.printer.indentation():
                    self._delete_rows(
                        table, state.existing_data.table_by_type[table.type], state
                    )
            except MolgenisRequestError as e:
                raise DirectoryError(
                    f"Error deleting rows from {table.type.base_id}"
                ) from e

    def _delete_rows(self, table: Table, existing_table: Table, state: PublishingState):
        """
        Deletes rows from a combined table that are not present in the staging area's
        table. If a row is referenced from the quality info tables, it is not deleted
        but a warning will be raised.

        :param Table table: the staging area's table
        :param Table existing_table: the existing rows
        """
        # Compare the ids from staging and production to see what was deleted
        staging_ids = table.rows_by_id.keys()
        production_ids = set(existing_table.rows_by_id.keys())
        deleted_ids = production_ids.difference(staging_ids)

        # Remove ids that we are not allowed to delete
        undeletable_ids = state.quality_info.get_qualities(table.type).keys()
        deletable_ids = deleted_ids.difference(undeletable_ids)

        # For deleted biobanks, update the handle
        if table.type == TableType.BIOBANKS:
            self.pid_manager.terminate_biobanks(
                [existing_table.rows_by_id[id_].get("pid") for id_ in deletable_ids]
            )

        # Actually delete the rows in the combined table
        if deletable_ids:
            ids_dict = [{table.meta.id_attribute: id_} for id_ in deletable_ids]
            self.printer.print(
                f"Deleting {len(deletable_ids)} row(s) in {table.type.base_id}"
            )
            self.session.delete_records(table=table.type.base_id, data=ids_dict)
            for id_ in deletable_ids:
                with self.printer.indentation():
                    code = existing_table.rows_by_id[id_]["national_node"]
                    warning = DirectoryWarning(f"ID {id_} is deleted")
                    self.printer.print_warning(warning)
                    state.report.add_node_warnings(Node.of(code), [warning])

        # Show warning for every id that we prevented deletion of
        if deleted_ids != deletable_ids:
            for id_ in undeletable_ids:
                if id_ in deleted_ids:
                    warning = DirectoryWarning(
                        f"Prevented the deletion of a row that is referenced from "
                        f"the quality info: {table.type.value} {id_}."
                    )
                    self.printer.print_warning(warning)

                    code = existing_table.rows_by_id[id_]["national_node"]
                    state.report.add_node_warnings(Node.of(code), [warning])
