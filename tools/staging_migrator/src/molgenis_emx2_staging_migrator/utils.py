"""
Utility functions for the StagingMigrator class.
"""
import logging

from tools.staging_migrator.src.molgenis_emx2_staging_migrator.graphql_queries import Queries

log = logging.getLogger(__name__)


def get_staging_cohort_id(url, session, staging_area) -> str | None:
    """Fetches the id associated with the staging area's cohort."""

    # Query server for cohort id
    query = Queries.Cohorts
    staging_url = f"{url}/{staging_area}/graphql"
    response = session.post(url=staging_url, json={"query": query}).json().get('data')

    # Return only if there is exactly one id/cohort in the Cohorts table
    if "Cohorts" in response.keys():
        if len(response['Cohorts']) != 1:
            log.warning(
                f'Expected a single cohort in staging area "{staging_area}"'
                f' but found {len(response["Cohorts"])}')
            return None
    else:
        log.warning(
            f'Expected a single cohort in staging area "{staging_area}"'
            f' but found none.')
        return None

    return response['Cohorts'][0]['id']


def prepare_pkey(schema: dict, table_name: str, col_name: str | list = None) -> str | list | dict:
    """Prepares a primary key by adding a reference to a table if necessary."""
    if not col_name:
        # Return the primary keys of a table if no column name is specified
        return [col['id'] for col in schema[table_name]['columns'] if col.get('key') == 1]
    col_data, = [col for col in schema[table_name]['columns'] if col['id'] == col_name]
    if col_data.get('columnType') in ['ONTOLOGY', 'ONTOLOGY_ARRAY']:
        ont_keys = prepare_pkey(schema, col_data.get('refTable'))
        ont_cols = [prepare_pkey(schema, col_data['refTable'], ok) for ok in ont_keys]
        return {col_name: ont_cols}
    elif col_data.get('columnType') in ['REF', 'REF_ARRAY']:
        ref_keys = prepare_pkey(schema, col_data.get('refTable'))
        ref_cols = [prepare_pkey(schema, col_data['refTable'], rk) for rk in ref_keys]
        return {col_name: ref_cols}
    else:
        return col_name


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
    """Finds the references in the target catalogue to the Cohorts table."""

    def find_table_columns(_t_name: str, _t_values: dict):
        _table_references = []
        for _column in _t_values['columns']:
            if _column.get('columnType') in ['REF', 'REF_ARRAY']:
                if _column.get('refTable') in cohort_inheritance and _column['id'] != 'target':
                    _table_references.append(_column['id'])
                elif _column.get('refTable') != _t_name:
                    _column_references = find_table_columns(_column['refTable'], schema_schema[_column['refTable']])
                    if len(_column_references) > 0:
                        _table_references.append(_column['id'])
        return _table_references

    cohort_inheritance = ['Cohorts', 'Data resources', 'Extended resources']
    cohort_references = dict()
    for t_name, t_values in schema_schema.items():
        table_references = find_table_columns(t_name, t_values)
        if len(table_references) > 0:
            cohort_references.update({t_name: table_references[0]})

    # Put in sequence
    ref_backs = {tab: [_col.get('refTable') for _col in schema_schema[tab]['columns']
                       if _col.get('columnType') == 'REFBACK']
                 for tab, col in cohort_references.items()}

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


def construct_delete_query(db_schema: dict, table: str, pkeys: list):
    """Constructs a GraphQL query for deleting rows from a table."""
    table_id = db_schema[table]['id']
    pkeys_print = query_columns_string(pkeys, indent=4)
    _query = (f"query {table_id}($filter: {table_id}Filter) {{\n"
              f"  {table_id}(filter: $filter) {{\n"
              f"{pkeys_print}\n"
              f"  }}\n"
              f"}}")
    return _query


def construct_delete_variables(staging_cohort_id: str, t_name: str, t_type: str, schema: dict):
    """Constructs a variables filter for querying the GraphQL table on the desired column values."""
    pkeys = prepare_pkey(schema, t_name, t_type)

    def prepare_key_part(_pkey: str | dict):
        if isinstance(_pkey, str):
            return {"equals": [{_pkey: staging_cohort_id}]}
        if isinstance(_pkey, dict):
            _key, _val = list(_pkey.items())[0]
            return {_key: prepare_key_part(_val[0])}

    variables = {"filter": prepare_key_part(pkeys)}

    return variables
