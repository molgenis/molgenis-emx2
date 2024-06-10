from collections import OrderedDict

from unidecode import unidecode

from molgenis.bbmri_eric.errors import EricWarning
from molgenis.bbmri_eric.model import NodeData, Table, TableType
from molgenis.bbmri_eric.printer import Printer
from molgenis.bbmri_eric.utils import to_ordered_dict


class ModelFitter:
    """
    Sometimes, model changes are implemented in the published tables, but can't be
    implemented yet in all staging areas because action (adjustment of local databases,
    API calls, etc) from the national nodes is needed.
    This class contains temporary solutions to transform the staging areas data
    according to the published model. If the models of all staging areas are equal to
    the published model this class shouldn't contain any methods.
    """

    def __init__(
        self,
        node_data: NodeData,
        printer: Printer,
    ):
        self.node_data = node_data
        self.printer = printer

        self.warnings = []

    def fit_model(self):
        """
        Transforms the data of a node according to the published model:
        1. Merges biobank 'covid19biobank' values into 'capabilities'
        2. Moves biobank and collection head information to persons
        """
        self._merge_covid19_capabilities()
        self._move_heads_to_persons()
        return self.warnings

    def _add_warning(self, message: str):
        self.printer.print_warning(EricWarning(message), indent=1)
        self.warnings.append(message)

    def _merge_covid19_capabilities(self):
        """
        Merges each biobank's 'covid19biobank' column into its 'capabilities' column and
        then removes it.
        """
        self.printer.print("Merging 'covid19biobank' into 'capabilities'")

        covid = "covid19biobank"
        caps = "capabilities"
        for biobank in self.node_data.biobanks.rows:
            if covid in biobank and biobank[covid]:
                self._add_warning(
                    f"Biobank {biobank['id']} uses deprecated {covid}' "
                    f"column. Use '{caps}' instead."
                )

                if not biobank[caps]:
                    biobank[caps] = []

                biobank[caps] = list(
                    OrderedDict.fromkeys(biobank[caps] + biobank[covid])
                )

            biobank.pop(covid, None)

    def _move_heads_to_persons(self):
        """
        Moves the head information in the biobank and collection tables to persons and
        adds the person ID to the biobank and collection tables.
        """
        self.printer.print("Moving heads to persons")
        self._move_heads_for_table(self.node_data.biobanks)
        self._move_heads_for_table(self.node_data.collections)

    def _move_heads_for_table(self, table: Table):
        """
        1. Checks if the head already exists as a person
        2a. If not, creates a new person
        2b. If so, updates person information (f.e. add the role)
        3. Fills the 'head' column with person ID
        4. Removes the redundant 'head' columns.
        """
        head_columns = [
            "head_title_before_name",
            "head_firstname",
            "head_lastname",
            "head_title_after_name",
            "head_role",
        ]

        for row in table.rows:
            if set(row.keys()).isdisjoint(set(head_columns)):
                continue

            if "head" in row.keys():
                self._add_warning(
                    f"{table.type.value.capitalize()[:-1]} has a head ID. "
                    "But still includes deprecated 'head' columns."
                )

            else:
                self._add_warning(
                    f"{table.type.value.capitalize()[:-1]} {row['id']} uses "
                    "deprecated 'head' columns. Move head info to persons instead."
                )

                # Add head id to the table
                row["head"] = self._add_head(row, table)

            for column in head_columns:
                row.pop(column, None)

    def _add_head(self, data, table):
        if {"head_lastname", "head_firstname"}.issubset(set(data.keys())):
            person_id = self._check_person(data)
            if not person_id:
                self._add_warning(
                    f"Add {data['head_firstname']} {data['head_lastname']} to persons "
                )
                person_id = self._create_person(data)
            return person_id

        self._add_warning(
            f"{table.type.value.capitalize()[:-1]} {data['id']} has head"
            " info without first and/or last name"
        )

        return None

    def _check_person(self, data):
        # A head exists if the combination of first- and last name exists in persons
        for person in self.node_data.persons.rows:
            if (
                person.get("last_name", "NN").lower().replace(" ", "")
                == data["head_lastname"].lower().replace(" ", "")
                and person.get("first_name", "NN").lower().strip()
                == data["head_firstname"].lower().strip()
            ):
                if "role" in person and person["role"] and data.get("head_role"):
                    roles = person["role"].split(" and ")
                    roles.append(data.get("head_role").strip())
                    person["role"] = " and ".join(set(roles))
                else:
                    person["role"] = data.get("head_role")
                return person["id"]

        return None

    def _create_person(self, data):
        prefix = self.node_data.node.get_id_prefix(TableType.PERSONS)
        person = dict()
        person["id"] = prefix + data.get("head_lastname").lower().replace(" ", "")
        person["id"] = unidecode(person["id"])
        person["title_before_name"] = data.get("head_title_before_name")
        person["first_name"] = data.get("head_firstname")
        person["last_name"] = data.get("head_lastname")
        person["title_after_name"] = data.get("head_title_after_name")
        person["role"] = data.get("head_role")
        person["email"] = "UNKNOWN@" + self.node_data.node.code
        person["country"] = data.get("country")
        person["national_node"] = self.node_data.node.code
        self.node_data.persons.rows_by_id.update(to_ordered_dict([person]))

        return person["id"]
