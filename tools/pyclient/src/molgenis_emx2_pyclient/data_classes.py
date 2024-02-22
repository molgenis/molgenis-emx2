"""
Classes for the data types Schema, Table and Column.
"""
from dataclasses import dataclass
from typing import Literal

from tools.pyclient.src.molgenis_emx2_pyclient.exceptions import NoSuchColumnException, NoSuchTableException


@dataclass(eq=True, kw_only=True)
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
            value = self.__parse_arg(key, kwargs[key])
            setattr(self, key, value)
        if not hasattr(self, 'name'):
            raise AttributeError("Supply 'name' value when creating 'Table' object.")

    def __repr__(self):
        class_name = type(self).__name__
        dict_items = [f"{k}={v!r}" for k, v in self.__dict__.items()]
        return f"{class_name}({', '.join(dict_items)})"

    def __str__(self):
        return self.name

    def to_dict(self) -> dict:
        """Returns a dictionary representation of the Column object."""
        return self.__dict__

    @staticmethod
    def __parse_arg(_k: str, _v: str | list | dict):
        if _k == 'inherited':
            if _v.lower() == 'true':
                return True
            return False
        if _k == 'required':
            if _v.lower() == 'true':
                return True
            elif _v.lower() == 'false':
                return False
            return _v
        return _v


@dataclass(kw_only=True)
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
            value = self.__parse_arg(key, kwargs[key])
            setattr(self, key, value)
        if not hasattr(self, 'name'):
            raise AttributeError("Supply 'name' value when creating 'Table' object.")

    def __repr__(self):
        class_name = type(self).__name__
        dict_items = [f"{k}={v!r}" for k, v in self.__dict__.items()]
        return f"{class_name}({', '.join(dict_items)})"

    def __str__(self):
        return self.name

    def get_column(self, by: Literal['id', 'name'] = 'id', *, value: str) -> Column:
        """Gets the unique column by either id or name value.
        Raises NoSuchColumnException if the column could not be retrieved from the table.
        """
        columns = []
        for col in self.columns:
            if col.__getattribute__(by) == value:
                columns.append(col)
        if len(columns) == 0:
            raise NoSuchColumnException(f"Column with {by} '{value}' not found in table '{self}'.")
        return columns[0]

    def get_columns(self, by: str, value: str) -> list[Column]:
        """Gets the columns by an attribute of the Column objects."""
        columns = []
        for col in self.columns:
            if hasattr(col, by):
                if col.__getattribute__(by) == value:
                    columns.append(col)
        return columns

    def to_dict(self) -> dict:
        """Returns a dictionary representation of the Table object."""
        _dict = self.__dict__
        if 'columns' in _dict.keys():
            _dict['columns'] = [_col.to_dict() for _col in self.columns]
        return _dict

    @staticmethod
    def __parse_arg(_k: str, _v: str | list | dict):
        if _k == 'columns':
            return [Column(**__v) for __v in _v]
        return _v


class Schema:
    """Class for representing a schema on the server."""
    id: str
    name: str
    label: str
    tables: list[Table]
    members: dict
    roles: dict
    settings: dict

    def __init__(self, **kwargs):
        for key in kwargs.keys():
            value = self.__parse_arg(key, kwargs[key])
            setattr(self, key, value)
        if not hasattr(self, 'name'):
            raise AttributeError("Supply 'name' value when creating 'Schema' object.")

    def __repr__(self):
        class_name = type(self).__name__
        dict_items = [f"{k}={v!r}" for k, v in self.__dict__.items()]
        return f"{class_name}({', '.join(dict_items)})"

    def __str__(self):
        return self.name

    def get_table(self, by: Literal['id', 'name'] = 'id', *, value: str) -> Table:
        """Gets the unique table by either id or name value.
        Raises NoSuchTableException if the table could not be retrieved from the schema.
        """
        tables = []
        for tab in self.tables:
            if tab.__getattribute__(by) == value:
                tables.append(tab)
        if len(tables) == 0:
            raise NoSuchTableException(f"Table with {by} '{value}' not found in schema '{self}'.")
        return tables[0]

    def get_tables(self, by: str, value: str) -> list[Table]:
        """Gets the tables by an attribute of the Table objects."""
        tables = []
        for tab in self.tables:
            if hasattr(tab, by):
                if tab.__getattribute__(by) == value:
                    tables.append(tab)
        return tables

    def to_dict(self) -> dict:
        """Returns a dictionary representation of the Table object."""
        _dict = self.__dict__
        if 'tables' in _dict.keys():
            _dict['tables'] = [_tab.to_dict() for _tab in self.tables]
        return _dict

    @staticmethod
    def __parse_arg(_k: str, _v: str | list | dict):
        if _k == 'tables':
            return [Table(**__v) for __v in _v]
        return _v


if __name__ == '__main__':
    tab_values = {
          "name": "Cohorts",
          "id": "Cohorts",
          "description": "Group of individuals sharing a defining demographic characteristic",
          "schemaName": "catalogue",
          "schemaId": "catalogue",
          "inheritName": "Data resources",
          "inheritId": "DataResources",
          "tableType": "DATA",
          "columns": [
            {
              "id": "overview",
              "name": "overview",
              "description": "General information",
              "columnType": "HEADING",
            },
            {
              "id": "id",
              "name": "id",
              "description": "Internal identifier",
              "key": 1,
              "columnType": "STRING",
              "required": "TRUE",
            },
            {
              "id": "acronym",
              "name": "acronym",
              "description": "Acronym if applicable",
              "columnType": "STRING",
            },
            {
              "table": "Extended resources",
              "id": "leadOrganisation",
              "name": "lead organisation",
              "description": "lead organisation (e.g. research department or group) for this resource",
              "columnType": "REF_ARRAY",
              "inherited": 'true',
              "refSchemaName": "catalogue",
              "refTableName": "Organisations",
              "refTableId": "Organisations",
              "descriptions": [
                {
                  "locale": "en",
                  "value": "lead organisation (e.g. research department or group) for this resource"
                }
              ],
              "position": 29
            }
          ]}

    schema_values = {
        'id': 'catalogue',
        'name': 'catalogue',
        'label': 'catalogue',
        'tables': [tab_values]
    }

    table = Table(**tab_values)
    # Get the column with name 'acronym'
    acronym = table.get_column(by='name', value='acronym')
    print(acronym)
    # Columns referencing the table 'Organisation'
    orgs_refs = table.get_columns(by='refTableName', value='Organisations')
    print(orgs_refs)
    # Represent the Table object as a dictionary
    table_dict = table.to_dict()
    print(table_dict)
    print(table.__repr__())
    print(table)

    schema = Schema(**schema_values)
    schema_dict = schema.to_dict()
    print(schema_dict)
    print(schema.__repr__())
    print(schema)

