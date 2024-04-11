"""
Utility functions for the StagingMigrator class.
"""
import logging
from io import BytesIO

import pandas as pd

from molgenis_emx2_pyclient.exceptions import NoSuchTableException
from molgenis_emx2_pyclient.metadata import Schema, Table, Column

log = logging.getLogger(__name__)


def prepare_pkey(schema: Schema, table_name: str, col_id: str | list = None) -> str | list | dict | None:
    """Prepares a primary key by adding a reference to a table if necessary."""
    try:
        table_schema: Table = schema.get_table(by='name', value=table_name)
    except ValueError:
        raise NoSuchTableException(f"{table_name!r} not found in schema.")
    if not col_id:
        # Return the primary keys of a table if no column name is specified
        return list(map(lambda col: col.id, table_schema.get_columns(by='key', value=1)))
    if isinstance(col_id, list):
        return {_col_id: prepare_pkey(schema, table_name, _col_id)[_col_id] for _col_id in col_id}
    if isinstance(col_id, dict):
        return [*prepare_pkey(schema, table_name),
                *[{key: prepare_pkey(schema, value)} for (key, value) in col_id.items()]]

    col_data: Column = table_schema.get_column(by='id', value=col_id)
    if col_id.startswith('mg_'):
        return None
    if col_data.get('columnType') == 'HEADING':
        return None
    elif col_data.get('columnType') == 'REFBACK':
        return None
    elif col_data.get('columnType') in ['ONTOLOGY', 'ONTOLOGY_ARRAY']:
        return {col_id: 'name'}
    elif col_data.get('columnType') in ['REF', 'REF_ARRAY']:
        ref_keys = prepare_pkey(schema, col_data.get('refTableName'))
        ref_cols = [prepare_pkey(schema, col_data.get('refTableName'), rk) for rk in ref_keys]
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


def find_cohort_references(schema_schema: Schema, schema_name: str, base_table: str) -> dict:
    """Finds the references in the target catalogue to the Cohorts table.
    References may be direct or indirect, such as the 'Subcohort counts' table
    that references the 'Subcohorts' table, which references the 'Cohorts' table directly.
    """

    inheritance = {}
    table_name = base_table
    inherit = schema_schema.get_table(by='name', value=table_name).get('inheritName')
    inheritance.update({table_name: inherit})
    while inherit is not None:
        table_name = inherit
        inherit = schema_schema.get_table(by='name', value=str(table_name)).get('inheritName')
        inheritance.update({table_name: inherit})

    backward_refs = {
        tab.name: [c.id for c in tab.get_columns(by='refSchemaName', value=schema_name)
                   if c.get('refTableName') in inheritance.keys() and c.get('columnType') != 'REFBACK']
        for tab in schema_schema.get_tables(by='schemaName', value=schema_name)
    }
    backward_refs[base_table] = 'id'

    table_references = {
        tab.name: {c.id: c.get('refTableName')
                   for c in [*tab.get_columns(by=['columnType', 'refSchemaName'], value=['REF', schema_name]),
                             *tab.get_columns(by=['columnType', 'refSchemaName'], value=['REF_ARRAY', schema_name])]}
        for tab in schema_schema.get_tables(by='schemaName', value=schema_name)
    }

    # Add columns referencing the base table indirectly to the backward_refs dictionary
    for k, v in table_references.items():
        if k == base_table:
            continue
        for c, t in v.items():
            if t == k:
                continue
            if len(backward_refs.get(t)) and c not in backward_refs.get(k, []):
                backward_refs.get(k).append(c)

    # Gather all backwards references to the tables
    ref_backs = {}
    for tab in table_references.keys():
        ref_backs[tab] = []
        for _tab in table_references.keys():
            if tab == _tab:
                continue
            if tab in table_references[_tab].values():
                ref_backs[tab].append(_tab)

    for coh, inh in inheritance.items():
        if coh in ref_backs.keys():
            ref_backs[coh].append(inh)
        else:
            ref_backs[coh] = [inh]

    def pop_dict(key):
        ref_backs.pop(key)
        return key

    table_names = [_t.name for _t in schema_schema.tables]
    other_tabs = [key for key in table_names if key not in ref_backs.keys()]
    sequence = [pop_dict(tab) for (tab, refs) in ref_backs.copy().items() if len(refs) == 0]
    while len(ref_backs) > 0:
        for tab, refs in ref_backs.copy().items():
            if all(map(lambda ref: ref in [*sequence, *other_tabs, None], refs)):
                sequence.append(pop_dict(tab))

    backward_refs = {s: backward_refs.copy()[s] for s in sequence if len(backward_refs.copy()[s])}

    return backward_refs


def construct_delete_variables(db_schema: Schema, cohort_ids: list, table_name: str, ref_col: str):
    """Constructs a variables filter for querying the GraphQL table on the desired column values."""
    pkeys = prepare_pkey(db_schema, table_name, ref_col)
    # pkeys = [prepare_pkey(db_schema, table_name, col.id) for col in table_schema.get_columns(by='key', value=1)]

    def prepare_key_part(_pkey: str | dict | list):
        if isinstance(_pkey, str):
            return {"equals": [{_pkey: _id} for _id in cohort_ids]}
        if isinstance(_pkey, dict):
            _key, _val = list(_pkey.items())[0]
            for _key, _val in _pkey.items():
                pass
            return {_key: prepare_key_part(_val[0])}
        if isinstance(_pkey, list):
            pass

    variables = {"filter": prepare_key_part(pkeys)}

    return variables


def has_statement_of_consent(table_name: str, schema: Schema) -> int:
    """Checks whether this table has a column that asks for a statement of consent."""
    consent_cols = ['statement of consent personal data',
                    'statement of consent email']
    try:
        table_schema = schema.get_table(by='name', value=table_name)
    except ValueError:
        raise NoSuchTableException(f"Table {table_name!r} not in schema.")
    col_names = map(str, table_schema.columns)

    return 1 * (consent_cols[0] in col_names) + 2 * (consent_cols[1] in col_names)


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
