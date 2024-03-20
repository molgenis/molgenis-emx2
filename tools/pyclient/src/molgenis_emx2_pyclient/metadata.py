"""
Classes for the data types Schema, Table and Column.
"""
from itertools import starmap
from typing import Literal

from .exceptions import NoSuchColumnException, NoSuchTableException


class Column:
    """Class for representing a column in the database."""
    id: str
    name: str

    def __init__(self, **kwargs):
        for key in kwargs.keys():
            value = self.__parse_arg(key, kwargs[key])
            setattr(self, key, value)
        if not hasattr(self, 'name'):
            raise AttributeError("Supply 'name' value when creating 'Column' object.")

    def __repr__(self):
        class_name = type(self).__name__
        dict_items = [f"{k}={v!r}" for k, v in self.__dict__.items()]
        return f"{class_name}({', '.join(dict_items)})"

    def __str__(self):
        return self.name

    def get(self, attr: str, default: object = None):
        """Returns the value of an attribute. If the attribute does not exist, returns a default value.
        If this default value is not given, returns None.
        """
        if hasattr(self, attr):
            return self.__getattribute__(attr)
        return default

    def to_dict(self) -> dict:
        """Returns a dictionary representation of the Column object."""
        return self.__dict__

    @staticmethod
    def __parse_arg(k: str, v: str | list | dict):
        if k == 'inherited':
            if isinstance(v, bool):
                return v
            if v.lower() == 'true':
                return True
            return False
        if k == 'required':
            if isinstance(v, bool):
                return v
            if v.lower() == 'true':
                return True
            elif v.lower() == 'false':
                return False
            return v
        return v


class Table:
    """Class for representing a table in the database."""
    id: str
    name: str
    columns: list[Column]

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

    def get(self, attr: str, default: object = None):
        """Returns the value of an attribute. If the attribute does not exist, returns a default value.
        If this default value is not given, returns None.
        """
        if hasattr(self, attr):
            return self.__getattribute__(attr)
        return default

    def get_column(self, by: Literal['id', 'name'], value: str) -> Column:
        """Gets the unique column by either id or name value.
        Raises NoSuchColumnException if the column could not be retrieved from the table.
        """
        columns = []
        for col in self.columns:
            if col.__getattribute__(by) == value:
                columns.append(col)
        if len(columns) == 0:
            raise NoSuchColumnException(f"Column with {by} {value!r} not found in table {self.name!r}.")
        return columns[0]

    def get_columns(self, by: str | list, value: str | int | bool | list) -> list[Column]:
        """Gets the columns by one or multiple attributes of the Column objects."""
        columns = []
        if type(by) is list:
            assert len(by) == len(value), "'by' and 'value' should be of same length if supplied as lists."
            for col in self.columns:
                if all(starmap(lambda _by, _value: (hasattr(col, _by)
                                                    and (str(col.__getattribute__(_by)) == str(_value))),
                               zip(by, value))):
                    columns.append(col)
            return columns
        else:
            for col in self.columns:
                if hasattr(col, by):
                    if str(col.__getattribute__(by)) == str(value):
                        columns.append(col)
            return columns

    def to_dict(self) -> dict:
        """Returns a dictionary representation of the Table object."""
        _dict = self.__dict__
        if 'columns' in _dict.keys():
            _dict['columns'] = [_col.to_dict() for _col in self.columns]
        return _dict

    @staticmethod
    def __parse_arg(k: str, v: str | list | dict):
        if k == 'columns':
            return [c if isinstance(c, Column) else Column(**c) for c in v]
        return v


class Schema:
    """Class for representing a schema on the server."""
    id: str
    name: str
    tables: list[Table]

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

    def get(self, attr: str, default: object = None):
        """Returns the value of an attribute. If the attribute does not exist, returns a default value.
        If this default value is not given, returns None.
        """
        if hasattr(self, attr):
            return self.__getattribute__(attr)
        return default

    def get_table(self, by: Literal['id', 'name'], value: str) -> Table:
        """Gets the unique table by either id or name value.
        Raises NoSuchTableException if the table could not be retrieved from the schema.
        """
        tables = []
        for tab in self.tables:
            if tab.__getattribute__(by) == value:
                tables.append(tab)
        if len(tables) == 0:
            raise NoSuchTableException(f"Table with {by} {value!r} not found in schema {self.name!r}.")
        return tables[0]

    def get_tables(self, by: str, value: str) -> list[Table]:
        """Gets the tables by an attribute of the Table objects."""
        tables = []
        if type(by) is list:
            assert len(by) == len(value), "'by' and 'value' should be of same length if supplied as lists."
            for tab in self.tables:
                if all(starmap(lambda _by, _value: (hasattr(tab, _by)
                                                    and (str(tab.__getattribute__(_by)) == str(_value))),
                               zip(by, value))):
                    tables.append(tab)
            return tables
        else:
            for tab in self.tables:
                if hasattr(tab, by):
                    if str(tab.__getattribute__(by)) == str(value):
                        tables.append(tab)
            return tables

    def to_dict(self) -> dict:
        """Returns a dictionary representation of the Table object."""
        _dict = self.__dict__
        if 'tables' in _dict.keys():
            _dict['tables'] = [table.to_dict() for table in self.tables]
        return _dict

    @staticmethod
    def __parse_arg(k: str, v: str | list | dict):
        if k == 'tables':
            return [Table(**t) for t in v]
        return v
