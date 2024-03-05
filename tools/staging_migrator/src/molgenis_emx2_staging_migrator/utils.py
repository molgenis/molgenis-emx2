"""
Utility functions for the StagingMigrator class.
"""
import logging
from io import BytesIO

import pandas as pd

from molgenis_emx2_pyclient.exceptions import NoSuchTableException

log = logging.getLogger(__name__)


def prepare_pkey(schema: dict, table_name: str, col_id: str | list = None) -> str | list | dict | None:
    """Prepares a primary key by adding a reference to a table if necessary."""
    try:
        table_schema, = [_t for _t in schema['tables'] if _t.get('name') == table_name]
    except ValueError:
        raise NoSuchTableException(f"{table_name!r} not found in schema.")
    if not col_id:
        # Return the primary keys of a table if no column name is specified
        return [col['id'] for col in table_schema['columns'] if col.get('key') == 1]
    col_data, = [col for col in table_schema['columns'] if col['id'] == col_id]
    if col_id.startswith('mg_'):
        return None
    if col_data.get('columnType') == 'HEADING':
        return None
    elif col_data.get('columnType') == 'REFBACK':
        return None
    elif col_data.get('columnType') in ['ONTOLOGY', 'ONTOLOGY_ARRAY']:
        return {col_id: 'name'}
    #     ont_keys = prepare_pkey(schema, col_data.get('refTableName'))
    #     ont_cols = [prepare_pkey(schema, col_data['refTableName'], ok) for ok in ont_keys]
    #     return {col_id: ont_cols}
    elif col_data.get('columnType') in ['REF', 'REF_ARRAY']:
        ref_keys = prepare_pkey(schema, col_data.get('refTableName'))
        ref_cols = [prepare_pkey(schema, col_data['refTableName'], rk) for rk in ref_keys]
        return {col_id: ref_cols}
    elif col_data.get('columnType') == 'FILE':
        return {col_id: 'id'}
    else:
        return col_id


def query_columns_string(column: str | list | dict, indent: int) -> str:
    """Constructs a query string based on the level of indentation requested."""
    if isinstance(column, str):
        return_val = f"{indent * ' '}{column}"
        return return_val
    if isinstance(column, list):
        return_val = "\n".join(query_columns_string(item, indent=indent) for item in column)
        return return_val
    if isinstance(column, dict):
        col = list(column.keys())[0]
        vals = list(column.values())[0]
        return_val = (f"{indent * ' '}{col} {{\n"
                      f"{query_columns_string(vals, indent=indent + 2)}\n"
                      f"{indent * ' '}}}")
        return return_val


def find_cohort_references(schema_schema: dict, schema_name: str, base_table: str) -> dict:
    """Finds the references in the target catalogue to the Cohorts table.
    References may be direct or indirect, such as the 'Subcohort counts' table
    that references the 'Subcohorts' table, which references the 'Cohorts' table directly.
    """
    schema_schema['tables'] = [
        t for t in schema_schema['tables'] if t['schemaName'] in [schema_name, 'SharedStaging']
    ]

    def find_table_columns(_table: dict):
        _table_references = []
        for _column in _table['columns']:
            if _column.get('columnType') in ['REF', 'REF_ARRAY']:
                if _column.get('refTableName') in [base_table, *inheritance.values()]:
                    _table_references.append(_column['id'])
                elif _column.get('refTableName') != _table['name']:
                    ref_table, = [_t for _t in schema_schema['tables'] if _t['name'] == _column['refTableName']]
                    _column_references = find_table_columns(ref_table)
                    if len(_column_references) > 0:
                        _table_references.append(_column['id'])
            elif _column.get('columnType') == 'REFBACK':
                if _column.get('refTableName') in [base_table, *inheritance.values()]:
                    _table_references.append(_column['id'])

        return _table_references

    inheritance = {}
    table = base_table
    inherit = [t for t in schema_schema['tables'] if t['name'] == table][0].get('inheritName')
    inheritance.update({table: inherit})
    while inherit is not None:
        table = inherit
        inherit = [t for t in schema_schema['tables'] if t['name'] == table][0].get('inheritName')
        inheritance.update({table: inherit})

    cohort_references = dict()
    for table in schema_schema['tables']:
        if table['name'] in inheritance.keys():
            table_references = ['id']
        else:
            table_references = find_table_columns(table)
        if len(table_references) > 0:
            cohort_references.update({table['name']: table_references[0]})

    # Gather all backwards references to the tables
    ref_backs = {}
    for tab in cohort_references.keys():
        ref_backs[tab] = []
        for _tab in cohort_references.keys():
            _cols = [_col for _col in [t for t in schema_schema['tables'] if t['name'] == _tab][0].get('columns')
                     if (_col.get('columnType') in ['REF', 'REF_ARRAY']) & (_col.get('refTableName') == tab) > 0]
            if len(_cols) > 0:
                ref_backs[tab].append(_tab)

    for coh, inh in inheritance.items():
        if coh in ref_backs.keys():
            ref_backs[coh].append(inh)
        else:
            ref_backs[coh] = [inh]

    def pop_dict(key):
        ref_backs.pop(key)
        return key

    other_tabs = [key for key in schema_schema.keys() if key not in ref_backs.keys()]
    sequence = [pop_dict(tab) for (tab, refs) in ref_backs.copy().items() if len(refs) == 0]
    while len(ref_backs) > 0:
        for tab, refs in ref_backs.copy().items():
            if all((ref in [*sequence, *other_tabs, ref]) for ref in refs):
                sequence.append(pop_dict(tab))

    cohort_references = {s: cohort_references.copy()[s] for s in sequence}

    return cohort_references


def construct_delete_variables(db_schema: dict, cohort_ids: list, table_name: str, ref_col: str):
    """Constructs a variables filter for querying the GraphQL table on the desired column values."""
    pkeys = prepare_pkey(db_schema, table_name, ref_col)

    def prepare_key_part(_pkey: str | dict):
        if isinstance(_pkey, str):
            return {"equals": [{_pkey: _id} for _id in cohort_ids]}
        if isinstance(_pkey, dict):
            _key, _val = list(_pkey.items())[0]
            return {_key: prepare_key_part(_val[0])}

    variables = {"filter": prepare_key_part(pkeys)}

    return variables


def has_statement_of_consent(table_name: str, schema: dict) -> int:
    """Checks whether this table has a column that asks for a statement of consent."""
    consent_cols = ['statement of consent personal data',
                    'statement of consent email']
    try:
        table_schema, = [t for t in schema['tables'] if (t['name'] == table_name) & (t['schemaName'] == schema['name'])]
    except ValueError:
        raise NoSuchTableException(f"Table {table_name!r} not in schema.")
    col_names = [_col['name'] for _col in table_schema['columns']]

    return 1*(consent_cols[0] in col_names) + 2*(consent_cols[1] in col_names)


def process_statement(table: bytes, consent_val: int) -> bytes:
    """Processes any statement of consent by modifying the rows in the table for which no consent is given."""
    df = pd.read_csv(BytesIO(table))

    # Remove rows without any data consent
    if consent_val % 2 == 1:
        df = df.loc[df['statement of consent personal data']]
    # Replace email values for rows without email consent
    if consent_val > 1:
        df.loc[~df['statement of consent email'], 'email'] = ''

    _table = df.to_csv(index=False).encode()

    log.info("Implemented statement of consent in Contacts table.")

    return _table
