"""
Utility functions for the Molgenis EMX2 Pyclient package
"""
import logging

from .constants import INT, DECIMAL, DATETIME, BOOL, LONG, STRING
from .metadata import Table


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
