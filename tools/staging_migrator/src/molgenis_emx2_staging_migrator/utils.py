"""
Utility functions for the StagingMigrator class.
"""
import logging
from io import BytesIO

import pandas as pd

from tools.pyclient.src.molgenis_emx2_pyclient.exceptions import IncorrectSchemaError
from tools.staging_migrator.src.molgenis_emx2_staging_migrator.graphql_queries import Queries

log = logging.getLogger(__name__)


def get_cohort_ids(server_url, session, staging_area) -> list | None:
    """Fetches the id associated with the staging area's cohort."""

    # Query server for cohort id
    query = Queries.Cohorts
    staging_url = f"{server_url}/{staging_area}/graphql"
    response = session.post(url=staging_url,
                            json={"query": query})
    response_data = response.json().get('data')
    if response_data is None:
        # Raise new error
        raise IncorrectSchemaError(f"Table 'Cohorts' not found on schema '{staging_area}'.")

    # Return only if there is exactly one id/cohort in the Cohorts table
    if "Cohorts" in response_data.keys():
        if len(response_data['Cohorts']) < 1:
            log.warning(
                f'Expected a cohort in staging area "{staging_area}"'
                f' but found {len(response_data["Cohorts"])}')
            return None
    else:
        log.warning(
            f'Expected a single cohort in staging area "{staging_area}"'
            f' but found none.')
        return None

    return [cohort['id'] for cohort in response_data['Cohorts']]


def prepare_pkey(schema: dict, table_name: str, col_id: str | list = None) -> str | list | dict | None:
    """Prepares a primary key by adding a reference to a table if necessary."""
    if not col_id:
        # Return the primary keys of a table if no column name is specified
        return [col['id'] for col in schema[table_name]['columns'] if col.get('key') == 1]
    col_data, = [col for col in schema[table_name]['columns'] if col['id'] == col_id]
    if col_id.startswith('mg_'):
        return None
    if col_data.get('columnType') == 'HEADING':
        return None
    elif col_data.get('columnType') == 'REFBACK':
        return None
    elif col_data.get('columnType') in ['ONTOLOGY', 'ONTOLOGY_ARRAY']:
        ont_keys = prepare_pkey(schema, col_data.get('refTable'))
        ont_cols = [prepare_pkey(schema, col_data['refTable'], ok) for ok in ont_keys]
        return {col_id: ont_cols}
    elif col_data.get('columnType') in ['REF', 'REF_ARRAY']:
        ref_keys = prepare_pkey(schema, col_data.get('refTable'))
        ref_cols = [prepare_pkey(schema, col_data['refTable'], rk) for rk in ref_keys]
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


def find_cohort_references(schema_schema: dict) -> dict:
    """Finds the references in the target catalogue to the Cohorts table.
    References may be direct or indirect, such as the 'Subcohort counts' table
    that references the 'Subcohorts' table, which references the 'Cohorts' table directly.
    """

    def find_table_columns(_t_name: str, _t_values: dict):
        _table_references = []
        for _column in _t_values['columns']:
            if _column.get('columnType') in ['REF', 'REF_ARRAY']:
                if _column.get('refTable') in ['Cohorts', *cohort_inheritance.values()]:
                    _table_references.append(_column['id'])
                elif _column.get('refTable') != _t_name:
                    _column_references = find_table_columns(_column['refTable'], schema_schema[_column['refTable']])
                    if len(_column_references) > 0:
                        _table_references.append(_column['id'])
        return _table_references

    cohort_inheritance = {'Cohorts': 'Data resources', 'Data resources': 'Extended resources',
                          'Extended resources': 'Resources'}
    cohort_references = dict()
    for t_name, t_values in schema_schema.items():
        if t_name in cohort_inheritance.keys():
            table_references = ['id']
        else:
            table_references = find_table_columns(t_name, t_values)
        if len(table_references) > 0:
            cohort_references.update({t_name: table_references[0]})

    # Gather all backwards references to the tables
    ref_backs = {
        tab: [_tab for _tab in cohort_references.keys()
              if len([_col for _col in schema_schema[_tab]['columns']
                      if (_col.get('columnType') in ['REF', 'REF_ARRAY']) & (_col.get('refTable') == tab)]) > 0]
        for tab in cohort_references.keys()
    }
    for coh, inh in cohort_inheritance.items():
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
            if all((ref in [*sequence, *other_tabs]) for ref in refs):
                sequence.append(pop_dict(tab))

    cohort_references = {s: cohort_references.copy()[s] for s in sequence}

    return cohort_references


def construct_delete_query(db_schema: dict, table_name: str, all_columns: bool = False):
    """Constructs a GraphQL query for deleting rows from a table."""
    if all_columns:
        pkeys = [prepare_pkey(db_schema, table_name, col['id']) for col in db_schema[table_name]['columns']]
        pkeys = [pk for pk in pkeys if pk is not None]
    else:
        pkeys = [prepare_pkey(db_schema, table_name, col['id']) for col in db_schema[table_name]['columns'] if
                 col.get('key') == 1]
    table_id = db_schema[table_name]['id']
    pkeys_print = query_columns_string(pkeys, indent=4)
    _query = (f"query {table_id}($filter: {table_id}Filter) {{\n"
              f"  {table_id}(filter: $filter) {{\n"
              f"{pkeys_print}\n"
              f"  }}\n"
              f"}}")
    return _query


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
    col_names = [_col['name'] for _col in schema[table_name]['columns']]

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
