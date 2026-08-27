"""
Utility functions for the Molgenis EMX2 Pyclient package
"""
import csv
import io
import json
import logging
import pathlib
from io import BytesIO

import math
import pandas as pd
import requests
from requests import Response

from .constants import INT, DECIMAL, BOOL, LONG, STRING, CHECKBOX, MULTISELECT, DATE, DATETIME
from .exceptions import NoSuchSchemaException, NoSuchColumnException
from .exceptions import (ServiceUnavailableError, ServerNotFoundError, PyclientException, GraphQLException,
                         InvalidTokenException,
                         PermissionDeniedException, NonExistentTemplateException,
                         ReferenceException)
from .metadata import Table, Schema

log = logging.getLogger("Molgenis EMX2 Pyclient")

def read_file(file_path: str | pathlib.Path) -> str:
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

def prepare_filter(expr: str | None, _table: str, schema_meta: Schema) -> dict | None:
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

def prepare_value(value):
    value = str(value)
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

def prep_data_or_file(file_path: str | pathlib.Path | None = None, data: list | pd.DataFrame | None = None) -> str | None:
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
        return data_to_csv(data)

    message = "No data to import. Specify a file location or a dataset."
    log.error(message)
    raise FileNotFoundError(message)

def data_to_csv(data: list[dict] | pd.DataFrame, filename: str | pathlib.Path | None = None) -> str | None:
    """Converts Molgenis-format data (DataFrame or list of dicts) to Molgenis-format CSV
    
    :param data: input data, in the form of a Molgenis table
    :param filename: when supplied, output to specified file rather than returning a string

    :returns: a string containing CSV-formatted content, or nothing when exporting to file
    """

    if isinstance(data, pd.DataFrame):
        data_for_csv = data.copy() # Do not modify the original data
        object_columns = data_for_csv.select_dtypes(include=['object', 'string']).columns
        data_for_csv[object_columns] = data[object_columns].map(array_to_csv_string)
        if filename:
            data_for_csv.to_csv(path_or_buf=filename, index=False, quoting=csv.QUOTE_NONNUMERIC)
            return None
        else:
            return data_for_csv.to_csv(index=False, quoting=csv.QUOTE_NONNUMERIC)
    else:
        if filename:
            target = open(filename, mode='w', encoding='utf-8', newline='')
        else:
            target = io.StringIO('')
        with target:
            # Get column names and write header row
            columns = {column for row in data for column in row}
            writer = csv.DictWriter(target, fieldnames=columns, dialect=csv.excel)
            writer.writeheader()
            for row in data:
                if not isinstance(row, dict):
                    raise ValueError(f"Cannot prepare row {row!r}. Supply a list of dictionaries.")
                cleaned_row = {}
                for k, v in row.items():
                    # Replace 'nan' with 'None'
                    if isinstance(v, float) and math.isnan(v):
                        cleaned_row[k] = None
                    # Replace 'NaT' with 'None'
                    elif isinstance(v, pd.api.typing.NaTType):
                        cleaned_row[k] = None
                    # Convert lists to CSV-formatted strings
                    elif isinstance(v, list):
                        cleaned_row[k] = array_to_csv_string(v)
                    else:
                        cleaned_row[k] = v
                writer.writerow(cleaned_row)
            if isinstance(target, io.StringIO):
                return target.getvalue()
            return None

def check_schema(schema: str | None, default_schema: str | None, schema_names: list[str]):
    """Checks whether the schema used for this action exists."""
    if schema is not None:
        if schema in schema_names:
            return schema
        else:
            raise NoSuchSchemaException(f"Schema {schema!r} not available.")
    if default_schema is None:
        raise NoSuchSchemaException(f"Select an existing schema for this operation.")
    return default_schema

def csv_string_to_array(csv_string: str) -> list:
    """Convert EMX2 input of type *_ARRAY, from a string from the CSV API to a list"""
    if pd.notna(csv_string):
        with io.StringIO(csv_string) as in_string:
            reader = csv.reader(in_string, dialect=csv.excel)
            return next(reader)
    else:
        return []

def array_to_csv_string(array: list | str) -> str:
    """Convert a list to a string suitable for output to an EMX2 value of type *_ARRAY, 
    through the CSV API
    """
    if isinstance(array, list):
        with io.StringIO() as csv_string:
            writer = csv.writer(csv_string, dialect=csv.excel)
            writer.writerow(array)
            return csv_string.getvalue().strip()
    else:
        return array


def validate_graphql_response(response, mutation: str | None = None, fallback_error_message: str | None = None):
    """Validates a GraphQL response and prints the appropriate message.

    :param response: a graphql response from the server
    :type response: requests.Response
    :param mutation: the name of the graphql mutation executed, optional
    :type mutation: str
    :param fallback_error_message: a fallback error message, optional
    :type fallback_error_message: str

    :returns: a success or error message
    :rtype: string
    """

    if response.status_code == 503:
        raise ServiceUnavailableError(f"Server with url {response.url!r} (temporarily) unavailable.")
    if response.status_code == 404:
        raise ServerNotFoundError(f"Server with url {response.url!r} not found.")
    if response.status_code == 400:
        if 'Invalid token or token expired' in response.text:
            raise InvalidTokenException("Invalid token or token expired.")
        if 'permission denied' in response.text:
            raise PermissionDeniedException(f"Transaction failed: permission denied.")
        if 'Graphql API error' in response.text:
            msg = response.json().get("errors", [])[0].get('message')
            log.error(msg)
            raise GraphQLException(msg)
        if "violates foreign key constraint" in response.text:
            msg = response.json().get("errors", [])[0].get('message', '')
            log.error(msg)
            raise ReferenceException(msg)
        if "Cannot create schema from template" in response.text:
            msg = response.json().get("errors", [])[0].get('message', '')
            log.error(msg)
            raise NonExistentTemplateException("Selected template does not exist.")
        if "Field \'members\' in type \'MolgenisSchema\' is undefined" in response.text:
            msg = response.json().get("errors", [])[0].get('message')
            log.error(msg)
            raise PermissionDeniedException("Cannot access members on this schema.")

        msg = response.json().get("errors", [])[0].get('message', '')
        log.error(msg)
        raise PyclientException("An unknown error occurred when trying to reach this server.")

    if response.request.method == 'GET':
        return

    if response.status_code == 200:
        return

    response_json = response.json()
    response_keys = response_json.keys()
    if 'errors' not in response_keys and 'data' not in response_keys:
        message = fallback_error_message
        log.error(message)

    elif 'errors' in response_keys:
        message = response_json.get('errors')[0].get('message')
        if 'permission denied' in message:
            log.error("Insufficient permissions for this operations.")
            raise PermissionDeniedException("Insufficient permissions for this operations.")
        if 'AvailableDataModels' in message:
            log.error("Selected template does not exist.")
            raise NonExistentTemplateException("Selected template does not exist.")
        log.error(message)
        raise GraphQLException(message)

    elif mutation is not None:
        if response_json.get('data').get(mutation).get('status') == 'SUCCESS':
            message = response_json.get('data').get(mutation).get('message')
            log.info(message)
        else:
            message = f"Failed to validate response for {mutation!r}"
            log.error(message)


def response_to_dataframe(response: Response,
                          table: Table,
                          columns: list[str] | None = None,
                          parse_arrays: bool = False) -> pd.DataFrame:
    """Parses the response of a CSV query to pandas DataFrame format."""

    response_columns = pd.read_csv(BytesIO(response.content)).columns
    dtypes = {c: t for (c, t) in convert_dtypes(table).items() if c in response_columns}

    bool_columns = [c for (c, t) in dtypes.items() if t == 'boolean']
    date_columns = [c.name for c in table.columns
                    if c.get('columnType') in (DATE, DATETIME) and c.name in response_columns]
    response_data = pd.read_csv(BytesIO(response.content), keep_default_na=False, na_values=[''], dtype=dtypes,
                                parse_dates=date_columns, dialect=csv.excel())
    response_data[bool_columns] = response_data[bool_columns].replace({'true': True, 'false': False})
    if parse_arrays:
        array_columns = [c.name for c in table.columns
                         if (c.get('columnType').endswith('_ARRAY') or
                             c.get('columnType') in (CHECKBOX, MULTISELECT))
                         and c.name in response_columns]
        response_data[array_columns] = response_data[array_columns].map(csv_string_to_array)
    response_data = response_data.astype(dtypes)

    if columns:
        try:
            response_data = response_data[columns]
        except KeyError as e:
            if e.args[0].startswith("None of [Index(['"):
                missing_cols = e.args[0].split("None of [Index([")[1].split("]")[0]
                msg = f"Columns {missing_cols} not found."
            elif "not in index" in e.args[0]:
                msg = f"Columns {e.args[0]}"
            else:
                msg = f"Columns {e.args[0].split('Index(')[1].split(', dtype')} not in index."
            raise NoSuchColumnException(msg)
        response_data = response_data.drop_duplicates(keep='first').reset_index(drop=True)

    return response_data
