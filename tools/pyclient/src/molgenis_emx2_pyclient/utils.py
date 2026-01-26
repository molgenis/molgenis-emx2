"""
Utility functions for the Molgenis EMX2 Pyclient package
"""
import csv
import json
import logging
import pathlib

import pandas as pd

from .constants import INT, DECIMAL, BOOL, LONG, STRING
from .exceptions import NoSuchSchemaException
from .metadata import Table, Schema

log = logging.getLogger("Molgenis EMX2 Pyclient")

def read_file(file_path: str) -> str:
    """Reads and imports data from a file.
    
    :param file_path: path to a data file
    :type file_path: str
    :returns: data in string format
    :rtype: str
    """
    with open(file_path, 'r') as stream:
        data = stream.read()
        stream.close()
    return data

def parse_nested_pkeys(pkeys: list) -> str:
    """Converts a list of primary keys and nested primary keys to a string
    suitable for inclusion in a GraphQL query.
    """
    converted_pkeys = []
    for pk in pkeys:
        if isinstance(pk, str):
            converted_pkeys.append(pk)
        elif isinstance(pk, dict):
            for nested_key, nested_values in pk.items():
                converted_pkeys.append(nested_key)
                converted_pkeys.append("{")
                if isinstance(nested_values, str):
                    converted_pkeys.append(nested_values)
                else:
                    converted_pkeys.append(parse_nested_pkeys(nested_values).strip())
                converted_pkeys.append("}")
        else:
             logging.warning(f"Unexpected data type encountered: {type(pk)!r}.")

    return " ".join(converted_pkeys)

def convert_dtypes(table_meta: Table) -> dict:
    """Parses column metadata of a table to a dictionary of column ids to pandas dtypes."""

    type_map = {
        STRING: 'string',
        INT: 'Int64',
        LONG: 'Int64',
        DECIMAL: 'Float64',
        BOOL: 'boolean'
    }

    dtypes = {}
    for col in table_meta.columns:
        dtypes[col.name] = type_map.get(col.get('columnType'), 'object')

    return dtypes

def prepare_filter(expr: str, _table: str, schema_meta: Schema) -> dict | None:
    """Prepares a GraphQL filter based on the expression passed into `get`."""
    if expr in [None, ""]:
        return None
    statements = expr.split(' and ')
    _filter = dict()
    for stmt in statements:
        if '==' in stmt:
            _filter.update(**prepare_equals_filter(stmt, _table, schema_meta))
        elif '>' in stmt:
            _filter.update(**prepare_greater_filter(stmt, _table, schema_meta))
        elif '<' in stmt:
            _filter.update(**prepare_smaller_filter(stmt, _table, schema_meta))
        elif '!=' in stmt:
            _filter.update(**prepare_not_equals_filter(stmt, _table, schema_meta))
        elif 'between' in stmt:
            _filter.update(**prepare_between_filter(stmt, _table, schema_meta))
        else:
            raise ValueError(f"Cannot process statement {stmt!r}, "
                             f"ensure specifying one of the operators '==', '>', '<', '!=', 'between' "
                             f"in your statement.")
    return _filter

def prepare_equals_filter(stmt: str, _table: str, schema_meta: Schema) -> dict:
    """Prepares the filter part if the statement filters on equality."""
    _col = stmt.split('==')[0].strip()
    _val = stmt.split('==')[1].strip()

    col_id = ''.join(_col.split('`'))

    if '.' in col_id:
        return prepare_nested_filter(col_id, _val, "equals")

    col = schema_meta.get_table(by='name', value=_table).get_column(by='id', value=col_id)
    val = None
    match col.get('columnType'):
        case 'BOOL':
            val = False
            if str(_val).lower() == 'true':
                val = True
        case _:
            try:
                val = json.loads(''.join(_val.split('`')).replace("'", '"'))
            except json.decoder.JSONDecodeError:
                val = ''.join(_val.split('`'))

    return {col.id: {'equals': val}}

def prepare_greater_filter(stmt: str, _table: str, schema_meta: Schema) -> dict:
    """Prepares the filter part if the statement filters on greater than."""
    exclusive = '=' not in stmt
    stmt = stmt.replace('=', '')

    _col = stmt.split('>')[0].strip()
    _val = stmt.split('>')[1].strip()

    col_id = ''.join(_col.split('`'))

    col = schema_meta.get_table(by='name', value=_table).get_column(by='id', value=col_id)

    val = None
    match col.get('columnType'):
        case 'INT':
            val = int(_val) + 1 * exclusive
        case 'LONG':
            val = int(_val) + 1 * exclusive
        case 'DECIMAL':
            val = float(_val) + 0.0000001 * exclusive
        case _:
            raise NotImplementedError(f"Cannot perform filter '>' on column with type {col.get('columnType')}.")

    return {col.id: {"between": [val, None]}}

def prepare_smaller_filter(stmt: str, _table: str, schema_meta: Schema) -> dict:
    """Prepares the filter part if the statement filters on greater than."""
    exclusive = '=' not in stmt
    stmt = stmt.replace('=', '')

    _col = stmt.split('<')[0].strip()
    _val = stmt.split('<')[1].strip()

    col_id = ''.join(_col.split('`'))

    col = schema_meta.get_table(by='name', value=_table).get_column(by='id', value=col_id)

    val = None
    match col.get('columnType'):
        case 'INT':
            val = int(_val) - 1 * exclusive
        case 'LONG':
            val = int(_val) - 1 * exclusive
        case 'DECIMAL':
            val = float(_val) - 0.0000001 * exclusive
        case _:
            raise NotImplementedError(f"Cannot perform filter '<' on column with type {col.get('columnType')}.")

    return {col.id: {"between": [None, val]}}

def prepare_not_equals_filter(stmt: str, _table: str, schema_meta: Schema) -> dict:
    """Prepares the filter part if the statement filters on greater than."""
    _col = stmt.split('!=')[0].strip()
    _val = stmt.split('!=')[1].strip()

    col_id = ''.join(_col.split('`'))

    if '.' in col_id:
        return prepare_nested_filter(col_id, _val, "not_equals")

    col = schema_meta.get_table(by='name', value=_table).get_column(by='id', value=col_id)

    val = None
    match col_type := col.get('columnType'):
        case 'BOOL':
            val = False
            if str(_val).lower() == 'true':
                val = True
        case 'RADIO' | 'REF' | 'REF_ARRAY' | 'ONTOLOGY' | 'ONTOLOGY_ARRAY':
            raise NotImplementedError(f"The filter '!=' is not implemented for columns of type {col_type!r}.")
        case _:
            try:
                val = json.loads(''.join(_val.split('`')).replace("'", '"'))
            except json.decoder.JSONDecodeError:
                val = ''.join(_val.split('`'))

    return {col.id: {"not_equals": val}}

def prepare_between_filter(stmt: str, _table: str, schema_meta: Schema) -> dict:
    """Prepares the filter part if values between a certain range are requested."""
    stmt.replace('=', '')
    _col = stmt.split('between')[0].strip()
    _val = stmt.split('between')[1].strip()

    try:
        val = json.loads(_val)
    except json.decoder.JSONDecodeError:
        msg = ("To filter on values between a and b, supply them as a list, [a, b]. "
               "Ensure the values for a and b are numeric.")
        raise ValueError(msg)
    col_id = ''.join(_col.split('`'))

    col = schema_meta.get_table(by='name', value=_table).get_column(by='id', value=col_id)
    if (col_type := col.get('columnType')) not in ['LONG', 'INT', 'DECIMAL']:
        raise NotImplementedError(f"The filter 'between' is not implemented for columns of type {col_type!r}.")

    return {col.id: {'between': val}}

def prepare_nested_filter(columns: str, value: str | int | float | list, comparison: str):
    _filter = {}
    current = _filter
    for (i, segment) in enumerate(columns.split('.')[:-1]):
        current[segment] = {}
        current = current[segment]
    last_segment = columns.split('.')[-1]
    current[last_segment] = {comparison: prepare_value(value)}
    return _filter

def prepare_value(value: str):
    if value.startswith('[') and value.endswith(']'):
        return json.loads(value.replace('\'', '"'))
    return value

def format_optional_params(**kwargs):
    """Parses optional keyword arguments to a format suitable for GraphQL queries."""
    args = {key: kwargs[key] for key in kwargs.keys() if key not in ('self', None)}
    if 'name' in args.keys():
        args['name'] = args.pop('name')
    if 'include_demo_data' in args.keys():
        args['includeDemoData'] = args.pop('include_demo_data')
    if 'parent_job' in args.keys():
        args['parentJob'] = args.pop('parent_job')
    return args

def prep_data_or_file(file_path: str | pathlib.Path = None, data: list | pd.DataFrame = None) -> str | None:
    """Prepares the data from memory or loaded from disk for addition or deletion action.

    :param file_path: path to the file to be prepared
    :type file_path: str
    :param data: data to be prepared
    :type data: list

    :returns: prepared data in dataframe format
    :rtype: pd.DataFrame
    """

    if file_path is not None:
        return read_file(file_path=file_path)

    if data is not None:
        if isinstance(data, pd.DataFrame):
            return data.to_csv(index=False, quoting=csv.QUOTE_NONNUMERIC, encoding='UTF-8')
        else:
            return pd.DataFrame(data, dtype=str).to_csv(index=False, quoting=csv.QUOTE_NONNUMERIC, encoding='UTF-8')

    message = "No data to import. Specify a file location or a dataset."
    log.error(message)
    raise FileNotFoundError(message)

def check_schema(schema: str, default_schema: str, schema_names: list[str]):
    """Checks whether the schema used for this action exists."""
    if schema is not None:
        if schema in schema_names:
            return schema
        else:
            raise NoSuchSchemaException(f"Schema {schema!r} not available.")
    if default_schema is None:
        raise NoSuchSchemaException(f"Select an existing schema for this operation.")
    return default_schema
