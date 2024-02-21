"""
Classes for the data types Schema, Table and Column.
"""
from typing import Literal

from tools.pyclient.src.molgenis_emx2_pyclient.exceptions import NoSuchColumnException


class Column:
    """Class for representing a column in the database."""
    id: str
    name: str
    table: str
    label: str
    description: str
    labels: dict
    description: dict
    position: int
    columnType: str
    inherited: bool
    key: int
    required: str
    defaultValue: str
    refSchemaId: str
    refSchemaName: str
    refTableName: str
    refTableId: str
    refLinkName: str
    refLinkId: str
    refBackName: str
    refLabel: str
    refLabelDefault: str
    validation: str
    visible: str
    readonly: bool
    computed: str
    semantics: list[str]

    def __init__(self, **kwargs):
        for key in kwargs.keys():
            setattr(self, key, kwargs[key])
        if not hasattr(self, 'name'):
            raise AttributeError("Supply 'name' value when creating 'Table' object.")

    def __str__(self):
        return self.name


class Table:
    """Class for representing a table in the database."""
    id: str
    name: str
    label: str
    description: str
    labels: dict
    schemaName: str
    schemaId: str
    inheritName: str
    inheritId: str
    descriptions: dict
    columns: list[Column]
    settings: dict
    semantics: str
    tableType: str

    def __init__(self, **kwargs):
        for key in kwargs.keys():
            # TODO: need to parse 'columns' argument
            setattr(self, key, kwargs[key])
        if not hasattr(self, 'name'):
            raise AttributeError("Supply 'name' value when creating 'Table' object.")

    def __str__(self):
        return self.name

    def get_column(self, by: Literal['id', 'name'] = 'id', *, value: str) -> Column:
        """Gets the column by either id or name value.
        Raises NoSuchColumnException if the column could not be retrieved from the table.
        """
        columns = []
        for col in self.columns:
            if col.__getattribute__(by) == value:
                columns.append(col)
        if len(columns) == 0:
            raise NoSuchColumnException(f"Column with {by} '{value}' not found in table '{self}'.")
        return columns[0]


class Schema:
    """Class for representing a schema on the server."""


if __name__ == '__main__':
    col_values = {
              "name": "lead organisation",
              "id": "leadOrganisation",
              "columnType": "REF_ARRAY",
              "refSchemaName": "SharedStaging",
              "refTableName": "Organisations",
              "position": 97,
              "refSchemaId": "SharedStaging",
              "refLabelDefault": "${id}"
            }
    # tab_values = {'id': 'BigTable', 'label': 'the big table', 'schemaName': 'the schema name'}
    tab_values = {
          "name": "Cohorts",
          "id": "Cohorts",
          "description": "Group of individuals sharing a defining demographic characteristic",
          "schemaName": "ABCD",
          "tableType": "DATA",
          "columns": [
            {
              "name": "overview",
              "id": "overview"
            },
            {
              "name": "id",
              "id": "id"
            },
            {
              "name": "pid",
              "id": "pid"
            },
            {
              "name": "acronym",
              "id": "acronym"
            }]}

    table = Table(**tab_values)
    acronym = table.get_column(by='name', value='acronym')
    print(acronym)
    print(table.name)
