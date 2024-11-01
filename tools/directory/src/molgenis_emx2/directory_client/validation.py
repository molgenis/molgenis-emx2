import re
from typing import List, Set

from .errors import DirectoryWarning
from .model import NodeData, Table, TableType
from .printer import Printer


class Validator:
    """
    This class is responsible for validating the data in a single node. Validation
    consists of:
    1. Checking the validity of all identifiers
    2. Checking if there are rows that reference rows with invalid identifiers
    3. Checking the validity of ages
    4. Checking the validity of hyperlinks
    """

    ALLOWS_EU_PREFIXES = {TableType.PERSONS, TableType.NETWORKS}
    HYPERLINK_REGEX = (
        r"^((https?):\/\/)(www.)?[-a-zA-Z0-9@:%._\\+~#?&//=]{2,256}\."
        r"[a-z]{2,6}\b([-a-zA-Z0-9@:%._\\+~#?&//=]*)\/?$|^$/"
    )

    def __init__(self, node_data: NodeData, printer: Printer):
        self.printer = printer
        self.node_data = node_data
        self.invalid_ids: Set[str] = set()
        self.warnings: List[DirectoryWarning] = list()

    def validate(self) -> List[DirectoryWarning]:
        for table in self.node_data.import_order:
            self._validate_ids(table)
            self._validate_hyperlinks(table)

        self._validate_networks()
        self._validate_biobanks()
        self._validate_collections()

        return self.warnings

    def _validate_ids(self, table: Table):
        for row in table.rows:
            id_ = row["id"]
            self._validate_id_prefix(id_, table)
            self._validate_id_chars(id_, table)

    def _validate_hyperlinks(self, table: Table):
        for row in table.rows:
            for column in list(set(table.meta.hyperlinks).intersection(row.keys())):
                if not re.match(self.HYPERLINK_REGEX, row[column]):
                    self._warn(
                        f"{table.type.value.capitalize()[:-1]} {row['id']} "
                        f"has an invalid {column}: {row[column]}"
                    )

    def _validate_networks(self):
        for network in self.node_data.networks.rows:
            self._validate_xref(network, "contact")
            self._validate_mref(network, "parent_network")

    def _validate_biobanks(self):
        for biobank in self.node_data.biobanks.rows:
            self._validate_xref(biobank, "contact")
            self._validate_mref(biobank, "network")
            self._validate_mref(biobank, "also_known_in")

    def _validate_collections(self):
        for collection in self.node_data.collections.rows:
            self._validate_xref(collection, "contact")
            self._validate_xref(collection, "biobank")
            self._validate_xref(collection, "parent_collection")
            self._validate_mref(collection, "networks")
            self._validate_mref(collection, "also_known_in")
            self._validate_ages(
                collection,
            )

    def _validate_ages(self, collection: dict):
        low = collection.get("age_low", None)
        high = collection.get("age_high", None)
        unit = collection.get("age_unit", None)

        if low == 0 and high == 0:
            self._warn(
                f"Collection {collection['id']} has invalid ages: age_low = 0 and "
                f"age_high = 0"
            )

        if (low is not None or high is not None) and not unit:
            self._warn(
                f"Collection {collection['id']} has age_low/age_high without age_unit"
            )

        if low is not None and high is not None and (low > high):
            self._warn(
                f"Collection {collection['id']} has invalid ages: age_low > age_high"
            )

    def _validate_xref(self, row: dict, ref_attr: str):
        if ref_attr in row:
            self._validate_ref(row, row[ref_attr])

    def _validate_mref(self, row: dict, mref_attr: str):
        if mref_attr in row:
            for ref_id in row[mref_attr]:
                self._validate_ref(row, ref_id)

    def _validate_ref(self, row: dict, ref_id: str):
        if ref_id in self.invalid_ids:
            warning = DirectoryWarning(f"{row['id']} references invalid id: {ref_id}")
            self.printer.print_warning(warning)
            self.warnings.append(warning)

    def _validate_id_prefix(self, id_: str, table: Table):
        node = self.node_data.node

        prefix = node.get_id_prefix(table.type)
        if table.type in self.ALLOWS_EU_PREFIXES:
            eu_prefix = node.get_eu_id_prefix(table.type)
            if not id_.startswith((prefix, eu_prefix)):
                self._warn(
                    f"{id_} in entity: {table.full_name} does not start with {prefix} "
                    f"or {eu_prefix}"
                )
                self.invalid_ids.add(id_)
        else:
            if not id_.startswith(prefix):
                self._warn(
                    f"{id_} in entity: {table.full_name} does not start with {prefix}"
                )
                self.invalid_ids.add(id_)

    def _validate_id_chars(self, id_: str, table: Table):
        if not re.search("^[A-Za-z0-9-_:]+$", id_):
            self._warn(
                f"{id_} in entity: {table.full_name} contains"
                f" invalid characters. Only alphanumerics and -_: are allowed."
            )
            self.invalid_ids.add(id_)

    def _warn(self, message: str):
        warning = DirectoryWarning(message)
        self.printer.print_warning(warning)
        self.warnings.append(warning)
