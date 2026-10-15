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
    catalog_id: str
    publisher_id: str
    deleted_ids: dict = field(default_factory=dict)
    data_to_publish: MixedData = field(init=False)

    def __post_init__(self):
        self.data_to_publish = self.existing_data.copy_empty()
        self.data_to_publish.source = Source.TRANSFORMED
        self.deleted_ids = {table_type: set() for table_type in TableType}


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
        2. Removed rows are deleted from non-persistent Directory tables and
        removed rows are set to 'withdrawn' for persistent Directory tables

        For certain tables (Biobanks, Collections, Networks), data is never deleted from
        the main schema, but rather the withdrawn attribute is set to 'True'
        """
        self.printer.print("💾 Saving new and updated data to Directory tables")
        with self.printer.indentation():
            await self._upsert_data(state)

        self.printer.print("🧼 Cleaning up and withdrawing deleted data in Directory tables")
        with self.printer.indentation():
            await self._delete_data(state)

    async def _upsert_data(self, state):
        try:
            await self.session.upload_data(
                schema=self.session.directory_schema, data=state.data_to_publish
            )
        except MolgenisRequestError as e:
            raise DirectoryError("Error importing data to combined tables") from e

    async def _delete_data(self, state):
        for table in reversed(state.data_to_publish.import_order):
            # Check whether deletion is allowed or 'withdrawn' should be used
            if "withdrawn" in table.meta.attributes:
                await self._withdraw_rows(table, state)
            else:
                try:
                    with self.printer.indentation():
                        self._delete_rows(
                            table, state.existing_data.table_by_type[table.type], state
                        )
                except MolgenisRequestError as e:
                    raise DirectoryError(f"Error deleting rows from {table.type.base_id}") from e

    def _delete_rows(self, table: Table, existing_table: Table, state: PublishingState):
        """
        Deletes rows from a combined table that are not present in the staging area's
        table. If a row is referenced from the quality info tables, it is not deleted
        but a warning will be raised.

        :param Table table: the staging area's table
        :param Table existing_table: the existing rows
        """

        # Remove ids that we are not allowed to delete
        deleted_ids = state.deleted_ids[table.type]
        undeletable_ids = state.quality_info.get_qualities(table.type).keys()
        deletable_ids = deleted_ids.difference(undeletable_ids)

        # Actually delete the rows in the combined table
        if deletable_ids:
            ids_dict = [{table.meta.id_attribute: id_} for id_ in deletable_ids]
            self.printer.print(f"Deleting {len(deletable_ids)} row(s) in {table.type.base_id}")
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

    async def _withdraw_rows(self, staging_table: Table, state: PublishingState):
        data_to_withdraw = []
        deleted_ids = state.deleted_ids[staging_table.type]
        # FIXME Ideally use CSV Update method, but do it ugly for now
        data_to_withdraw = []
        for deleted_id in deleted_ids:
            row_to_withdraw = self.session.get(
                schema=self.session.directory_schema,
                table=staging_table.full_name,
                query_filter=f"id == {deleted_id}",
            )[0]
            row_to_withdraw["withdrawn"] = True
            data_to_withdraw.append(row_to_withdraw)
        try:
            self.session.save_table(
                schema=self.session.directory_schema,
                table=staging_table.full_name,
                data=data_to_withdraw,
            )
            # Print warnings for deletions turned into withdrawals
            for deleted_id in deleted_ids:
                warning = DirectoryWarning(
                    f" Deleted {staging_table.full_name[:-1]} {deleted_id} has been 'withdrawn'."
                )
                self.printer.print_warning(warning)
            # Set PIDs for withdrawn biobanks to withdrawn
            if staging_table.type == TableType.BIOBANKS:
                self.pid_manager.withdraw_biobanks(deleted_ids)
        except MolgenisRequestError as e:
            raise DirectoryError("Error importing data to combined tables") from e
